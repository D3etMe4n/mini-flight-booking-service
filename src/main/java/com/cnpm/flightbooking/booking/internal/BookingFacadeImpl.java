package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.BookingFacade;
import com.cnpm.flightbooking.booking.dto.BookingDetailResponse;
import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.booking.dto.BookingResponse;
import com.cnpm.flightbooking.booking.dto.BookingSummaryResponse;
import com.cnpm.flightbooking.common.exception.PaymentFailedException;
import com.cnpm.flightbooking.common.exception.SeatUnavailableException;
import com.cnpm.flightbooking.flight.FlightFacade;
import com.cnpm.flightbooking.flight.dto.FlightResponse;
import com.cnpm.flightbooking.notification.event.BookingCancelledEvent;
import com.cnpm.flightbooking.notification.event.BookingSuccessEvent;
import com.cnpm.flightbooking.payment.PaymentFacade;
import com.cnpm.flightbooking.payment.dto.PaymentRequest;
import com.cnpm.flightbooking.payment.dto.PaymentResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class BookingFacadeImpl implements BookingFacade {

    private final FlightFacade flightFacade;
    private final PaymentFacade paymentFacade;
    private final BookingService bookingService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(noRollbackFor = PaymentFailedException.class)
    public BookingResponse bookFlight(BookingRequest request) {
        Long flightId = request.flightId();
        String seatNumber = request.seatNumber();

        boolean seatReserved = flightFacade.reserveSeat(flightId, seatNumber);
        if (!seatReserved) {
            throw new SeatUnavailableException(seatNumber, flightId);
        }

        Booking booking = bookingService.initiateBooking(request);

        FlightResponse flight = flightFacade.getFlightById(flightId);
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .bookingId(booking.getId())
                .basePrice(flight.basePrice())
                .customerType(request.customerType())
                .paymentMethod(request.paymentMethod())
                .rewardPoints(request.rewardPoints())
                .isHoliday(request.isHoliday())
                .build();

        PaymentResultResponse paymentResult = paymentFacade.processPayment(paymentRequest);

        if (!paymentResult.isSuccess()) {
            log.warn("Payment failed for booking code: {}. Reason: {}", booking.getBookingCode(), paymentResult.failureReason());

            // So if in the future, cancel reason have to be stored, the reason argument here will be utilized.
            bookingService.cancelBookingBySystem(booking.getId(), "Payment failed");
            flightFacade.releaseSeat(flightId, seatNumber);

            throw new PaymentFailedException(paymentResult.failureReason());
        }

        BigDecimal chargedAmount = paymentResult.amountPaid();
        bookingService.confirmBooking(booking.getId(), chargedAmount);

        BookingSuccessEvent successEvent = new BookingSuccessEvent(
                booking.getId(),
                booking.getCustomerEmail(),
                flight.flightNumber(),
                seatNumber,
                chargedAmount
        );
        eventPublisher.publishEvent(successEvent);

        return BookingResponse.builder()
                .bookingCode(booking.getBookingCode())
                .status(BookingStatus.CONFIRMED.name())
                .chargedAmount(chargedAmount)
                .message("Booking and payment processed successfully.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingByCode(String bookingCode) {
        return bookingService.getBookingByCode(bookingCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getBookingsByCustomerEmail(String email) {
        return bookingService.getBookingsByEmail(email);
    }

    @Override
    @Transactional
    public void cancelBookingByCustomer(Long bookingId, String reason) {
        log.info("Processing customer cancellation for booking ID: {}, Reason: {}", bookingId, reason);

        Booking cancelledBooking = bookingService.cancelBookingByCustomer(bookingId, reason);

        flightFacade.releaseSeat(cancelledBooking.getFlightId(), cancelledBooking.getSeatNumber());
        log.info("Released seat {} for flight ID {}", cancelledBooking.getSeatNumber(), cancelledBooking.getFlightId());

        BigDecimal refundAmount = cancelledBooking.getTotalAmount();
        // paymentFacade.refundPayment(cancelledBooking.getId(), refundAmount); // If payment have this method

        BookingCancelledEvent event = new BookingCancelledEvent(
                cancelledBooking.getId(),
                cancelledBooking.getBookingCode(),
                cancelledBooking.getCustomerEmail(),
                cancelledBooking.getFlightId(),
                cancelledBooking.getSeatNumber(),
                refundAmount,
                reason
        );
        eventPublisher.publishEvent(event);

        log.info("Booking cancellation completed successfully for booking code: {}", cancelledBooking.getBookingCode());
    }
}