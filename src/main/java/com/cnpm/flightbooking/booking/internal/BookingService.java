package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.dto.BookingDetailResponse;
import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.booking.dto.BookingSummaryResponse;
import com.cnpm.flightbooking.common.exception.BookingNotFoundException;
import com.cnpm.flightbooking.common.exception.InvalidBookingStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;

    public Booking initiateBooking(BookingRequest request) {
        if (request == null || request.flightId() == null || request.flightId() <= 0) {
            throw new IllegalArgumentException("Invalid booking request or flight ID");
        }

        // Generate PNR code (e.g., PNR-A9E2F1)
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

    public void confirmBooking(Long bookingId, BigDecimal finalAmount) {
        Booking booking = findByIdOrThrow(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(finalAmount);
        bookingRepository.save(booking);
    }

    public Booking cancelBookingByCustomer(Long bookingId, String reason) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be greater than zero");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("Only CONFIRMED bookings can be cancelled by customer");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .map(this::mapToDetailResponse)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with code: " + bookingCode));
    }

    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getBookingsByEmail(String email) {
        return bookingRepository.findByCustomerEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(b -> BookingSummaryResponse.builder()
                        .bookingCode(b.getBookingCode())
                        .flightId(b.getFlightId())
                        .seatNumber(b.getSeatNumber())
                        .totalAmount(b.getTotalAmount())
                        .status(b.getStatus().name())
                        .createdAt(b.getCreatedAt())
                        .build())
                .toList();
    }

    private Booking findByIdOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));
    }

    private BookingDetailResponse mapToDetailResponse(Booking booking) {
        return BookingDetailResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .flightId(booking.getFlightId())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .customerPhone(booking.getCustomerPhone())
                .seatNumber(booking.getSeatNumber())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    public void cancelBookingBySystem(Long bookingId, String reason) {
        if (bookingId == null || bookingId<= 0) {
            throw new IllegalArgumentException("Booking ID must be greater than zero");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("Only CONFIRMED bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
}