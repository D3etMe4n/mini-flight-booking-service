package com.cnpm.flightbooking.flight.internal;

import com.cnpm.flightbooking.common.exception.FlightNotFoundException;
import com.cnpm.flightbooking.common.exception.SeatNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private FlightSeatRepository flightSeatRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FlightServiceImpl flightService;

    private Flight sampleFlight;
    private FlightSeat sampleSeat;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        sampleFlight = Flight.builder()
                .id(1L)
                .flightNumber("VN123")
                .availableSeats(50)
                .basePrice(BigDecimal.valueOf(1500000))
                .build();

        sampleSeat = FlightSeat.builder()
                .id(10L)
                .flightId(sampleFlight.getId())
                .seatNumber("12A")
                .isReserved(false)
                .build();
    }

    @Nested
    @DisplayName("reserveSeat() - Input Validation")
    class ValidateInputTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when flightId is null or non-positive")
        void shouldThrowExceptionWhenFlightIdIsInvalid() {
            assertThatThrownBy(() -> flightService.reserveSeat(null, "12A"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Illegal ID format");

            assertThatThrownBy(() -> flightService.reserveSeat(-5L, "12A"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Illegal ID format");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "12Z", "123A", "A12", "12a"})
        @DisplayName("Should throw IllegalArgumentException when seatNumber does not match format")
        void shouldThrowExceptionWhenSeatFormatIsInvalid(String invalidSeat) {
            assertThatThrownBy(() -> flightService.reserveSeat(1L, invalidSeat))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid seat number format");
        }
    }

    @Nested
    @DisplayName("reserveSeat() - Redis Distributed Lock Flow")
    class RedisLockTests {

        @Test
        @DisplayName("Should return false immediately when Redis lock cannot be acquired")
        void shouldReturnFalseWhenRedisLockCannotBeAcquired() {
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(false);

            boolean result = flightService.reserveSeat(1L, "12A");

            assertThat(result).isFalse();
            verifyNoInteractions(flightRepository);
            verifyNoInteractions(flightSeatRepository);
        }
    }

    @Nested
    @DisplayName("reserveSeat() - Database Execution & Optimistic Locking")
    class DatabaseReservationTests {

        @BeforeEach
        void initLockSuccess() {
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(true);
        }

        @Test
        @DisplayName("Should delete lock and throw FlightNotFoundException when flight does not exist")
        void shouldDeleteLockAndThrowWhenFlightNotFound() {
            given(flightRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> flightService.reserveSeat(1L, "12A"))
                    .isInstanceOf(FlightNotFoundException.class);

            verify(redisTemplate).delete(eq("flight:1:seat:12A:lock"));
        }

        @Test
        @DisplayName("Should delete lock and throw SeatNotFoundException when seat does not exist")
        void shouldDeleteLockAndThrowWhenSeatNotFound() {
            given(flightRepository.findById(1L)).willReturn(Optional.of(sampleFlight));
            given(flightSeatRepository.findByFlightIdAndSeatNumber(1L, "12A")).willReturn(Optional.empty());

            assertThatThrownBy(() -> flightService.reserveSeat(1L, "12A"))
                    .isInstanceOf(SeatNotFoundException.class);

            verify(redisTemplate).delete(eq("flight:1:seat:12A:lock"));
        }

        @Test
        @DisplayName("Should delete lock and return false when seat is already reserved")
        void shouldReturnFalseAndCleanRedisWhenSeatAlreadyReserved() {
            sampleSeat.setIsReserved(true);

            given(flightRepository.findById(1L)).willReturn(Optional.of(sampleFlight));
            given(flightSeatRepository.findByFlightIdAndSeatNumber(1L, "12A")).willReturn(Optional.of(sampleSeat));

            boolean result = flightService.reserveSeat(1L, "12A");

            assertThat(result).isFalse();
            verify(redisTemplate).delete(eq("flight:1:seat:12A:lock"));
            verify(flightSeatRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should release lock and return false when OptimisticLockingFailureException occurs")
        void shouldCatchOptimisticLockExceptionAndReturnFalse() {
            given(flightRepository.findById(1L)).willReturn(Optional.of(sampleFlight));
            given(flightSeatRepository.findByFlightIdAndSeatNumber(1L, "12A")).willReturn(Optional.of(sampleSeat));

            given(flightRepository.save(any(Flight.class)))
                    .willThrow(new ObjectOptimisticLockingFailureException(Flight.class, 1L));

            boolean result = flightService.reserveSeat(1L, "12A");

            assertThat(result).isFalse();
            verify(redisTemplate).delete(eq("flight:1:seat:12A:lock"));
        }

        @Test
        @DisplayName("Should decrement available seats, set isReserved to true, and keep Redis lock on success")
        void shouldReserveSeatSuccessfully() {
            given(flightRepository.findById(1L)).willReturn(Optional.of(sampleFlight));
            given(flightSeatRepository.findByFlightIdAndSeatNumber(1L, "12A")).willReturn(Optional.of(sampleSeat));

            boolean result = flightService.reserveSeat(1L, "12A");

            assertThat(result).isTrue();
            assertThat(sampleSeat.getIsReserved()).isTrue();
            assertThat(sampleFlight.getAvailableSeats()).isEqualTo(49);

            verify(flightSeatRepository).save(sampleSeat);
            verify(flightRepository).save(sampleFlight);
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("releaseSeat() - Seat Reversion & Lock Eviction")
    class ReleaseSeatTests {

        @Test
        @DisplayName("Should update database entities and evict Redis lock successfully")
        void shouldReleaseSeatAndCleanRedisLock() {
            sampleSeat.setIsReserved(true);

            given(flightSeatRepository.findByFlightIdAndSeatNumber(1L, "12A")).willReturn(Optional.of(sampleSeat));
            given(flightRepository.findById(1L)).willReturn(Optional.of(sampleFlight));

            flightService.releaseSeat(1L, "12A");

            assertThat(sampleSeat.getIsReserved()).isFalse();
            assertThat(sampleFlight.getAvailableSeats()).isEqualTo(51);

            verify(flightSeatRepository).save(sampleSeat);
            verify(flightRepository).save(sampleFlight);
            verify(redisTemplate).delete(eq("flight:1:seat:12A:lock"));
        }
    }
}