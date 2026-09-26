package com.cnpm.flightbooking.flight.internal;

import com.cnpm.flightbooking.common.exception.FlightNotFoundException;
import com.cnpm.flightbooking.common.exception.SeatNotFoundException;
import com.cnpm.flightbooking.flight.FlightFacade;
import com.cnpm.flightbooking.flight.dto.FlightResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Transactional(readOnly = true)
@AllArgsConstructor
@Service
class FlightServiceImpl implements FlightFacade {

    private static final Predicate<String> SEAT_ID_PREDICATE = Pattern.compile("^[0-9]{1,2}[A-F]$").asMatchPredicate();
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);
    private static final String LOCK_VALUE = "LOCKED";

    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final StringRedisTemplate redisTemplate;


    @Override
    @Transactional
    public boolean reserveSeat(Long flightId, String seatNumber) {
        checkFlightId(flightId);
        checkSeatNumber(seatNumber);

        String lockKey = buildLockKey(flightId, seatNumber);

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, LOCK_VALUE, LOCK_TIMEOUT);
        if(Boolean.FALSE.equals(acquired)){
            // Cannot acquire lock -> seat is being acquired or being locked before
            return false;
        }

        try{
            Flight flight = flightRepository.findById(flightId).orElseThrow(() -> {
                return new FlightNotFoundException("Flight not found with id: " + flightId);
            });

            FlightSeat seat = flightSeatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber).orElseThrow(() -> {
                return new SeatNotFoundException("Seat " + seatNumber + " not found on flight: " + flightId);
            });

            if(Boolean.TRUE.equals(seat.getIsReserved()) || flight.getAvailableSeats() <= 0){
                cleanUpRedisKey(lockKey);
                return false;
            }

            seat.setIsReserved(true);
            flight.setAvailableSeats(flight.getAvailableSeats() - 1);

            flightSeatRepository.save(seat);
            flightRepository.save(flight);

            return true;
        } catch (ObjectOptimisticLockingFailureException ex) {
            cleanUpRedisKey(lockKey);
            return false;
        } catch (Exception ex) {
            cleanUpRedisKey(lockKey);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void releaseSeat(Long flightId, String seatNumber) {
        checkFlightId(flightId);
        checkSeatNumber(seatNumber);

        flightSeatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber).ifPresent(seat -> {
            if(Boolean.TRUE.equals(seat.getIsReserved())){
                seat.setIsReserved(false);
                flightSeatRepository.save(seat);

                flightRepository.findById(flightId).ifPresent(flight -> {
                    flight.setAvailableSeats(flight.getAvailableSeats() + 1);
                    flightRepository.save(flight);
                });
            }
        });
        String lockKey = buildLockKey(flightId, seatNumber);
        cleanUpRedisKey(lockKey);
    }

    @Override
    public FlightResponse getFlightById(Long flightId) {
        checkFlightId(flightId);
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with id: " + flightId));
        return FlightResponse.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .availableSeats(flight.getAvailableSeats())
                .basePrice(flight.getBasePrice())
                .build();
    }



    private void checkFlightId(Long flightId){
        if(flightId == null || flightId <= 0){
            throw new IllegalArgumentException("Illegal ID format");
        }
    }

    private void checkSeatNumber(String seatNumber) {
        if (seatNumber == null || !SEAT_ID_PREDICATE.test(seatNumber)) {
            throw new IllegalArgumentException("Invalid seat number format");
        }
    }

    private String buildLockKey(Long flightId, String seatNumber) {
        return "flight:" + flightId + ":seat:" + seatNumber + ":lock";
    }

    private void cleanUpRedisKey(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Maybe some logs here if needed, but im LAZY.
        }
    }
}
