package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.common.exception.BookingNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
class BookingService {

    private final BookingRepository bookingRepository;

    public Booking initiateBooking(BookingRequest request) {
        if (request == null || request.flightId() == null || request.flightId() <= 0) {
            throw new IllegalArgumentException("Invalid booking request or flight ID");
        }

        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String bookingCode = "PNR-" + randomSuffix;

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .flightId(request.flightId())
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .seatNumber(request.seatNumber())
                .totalAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .build();

        return bookingRepository.save(booking);
    }

    void confirmBooking(Long bookingId, BigDecimal finalAmount){
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(finalAmount);
        bookingRepository.save(booking);
    }

    void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

}
