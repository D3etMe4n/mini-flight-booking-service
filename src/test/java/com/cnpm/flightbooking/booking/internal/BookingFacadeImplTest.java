package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.booking.dto.BookingResponse;
import com.cnpm.flightbooking.common.exception.PaymentFailedException;
import com.cnpm.flightbooking.common.exception.SeatUnavailableException;
import com.cnpm.flightbooking.flight.FlightFacade;
import com.cnpm.flightbooking.flight.dto.FlightResponse;
import com.cnpm.flightbooking.notification.event.BookingCancelledEvent;
import com.cnpm.flightbooking.notification.event.BookingSuccessEvent;
import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentFacade;
import com.cnpm.flightbooking.payment.PaymentMethod;
import com.cnpm.flightbooking.payment.dto.PaymentRequest;
import com.cnpm.flightbooking.payment.dto.PaymentResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingFacadeImplTest {

    @Mock
    private FlightFacade flightFacade;

    @Mock
    private PaymentFacade paymentFacade;

    @Mock
    private BookingService bookingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingFacadeImpl bookingFacade;

    private BookingRequest validRequest;
    private Booking pendingBooking;
    private FlightResponse flightResponse;

    @BeforeEach
    void setUp() {
        validRequest = BookingRequest.builder()
                .flightId(101L)
                .seatNumber("12A")
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .customerPhone("0987654321")
                .customerType(CustomerType.STANDARD)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .rewardPoints(0)
                .isHoliday(false)
                .build();

        pendingBooking = Booking.builder()
                .id(1L)
                .bookingCode("PNR-XYZ789")
                .flightId(101L)
                .customerEmail("john@example.com")
                .seatNumber("12A")
                .status(BookingStatus.PENDING)
                .build();

        flightResponse = new FlightResponse(101L, "VN123", 100 ,new BigDecimal("1500000"));
    }

    @Test
    @DisplayName("bookFlight() - Happy Path: Should reserve seat, process payment, confirm, and publish event")
    void shouldCompleteBookingSuccessfully() {
        when(flightFacade.reserveSeat(101L, "12A")).thenReturn(true);
        when(bookingService.initiateBooking(validRequest)).thenReturn(pendingBooking);
        when(flightFacade.getFlightById(101L)).thenReturn(flightResponse);

        PaymentResultResponse successPayment = PaymentResultResponse.builder()
                .isSuccess(true)
                .amountPaid(new BigDecimal("1500000"))
                .transactionRef("TXN-12345")
                .build();
        when(paymentFacade.processPayment(any(PaymentRequest.class))).thenReturn(successPayment);

        BookingResponse response = bookingFacade.bookFlight(validRequest);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.bookingCode()).isEqualTo("PNR-XYZ789");
        assertThat(response.chargedAmount()).isEqualByComparingTo("1500000");

        verify(bookingService).confirmBooking(1L, new BigDecimal("1500000"));
        verify(eventPublisher).publishEvent(any(BookingSuccessEvent.class));
        verify(flightFacade, never()).releaseSeat(anyLong(), anyString());
    }

    @Test
    @DisplayName("bookFlight() - Seat locked: Should throw SeatUnavailableException immediately")
    void shouldThrowWhenSeatCannotBeReserved() {
        when(flightFacade.reserveSeat(101L, "12A")).thenReturn(false);

        assertThatThrownBy(() -> bookingFacade.bookFlight(validRequest))
                .isInstanceOf(SeatUnavailableException.class);

        verifyNoInteractions(bookingService, paymentFacade, eventPublisher);
    }

    @Test
    @DisplayName("bookFlight() - Payment failed: Should trigger compensating actions and throw PaymentFailedException")
    void shouldCompensateWhenPaymentFails() {
        when(flightFacade.reserveSeat(101L, "12A")).thenReturn(true);
        when(bookingService.initiateBooking(validRequest)).thenReturn(pendingBooking);
        when(flightFacade.getFlightById(101L)).thenReturn(flightResponse);

        PaymentResultResponse failedPayment = PaymentResultResponse.builder()
                .isSuccess(false)
                .failureReason("Insufficient balance")
                .build();
        when(paymentFacade.processPayment(any(PaymentRequest.class))).thenReturn(failedPayment);

        assertThatThrownBy(() -> bookingFacade.bookFlight(validRequest))
                .isInstanceOf(PaymentFailedException.class)
                .hasMessageContaining("Insufficient balance");

        verify(bookingService).cancelBookingBySystem(1L, "Payment failed");
        verify(flightFacade).releaseSeat(101L, "12A");

        verify(bookingService, never()).confirmBooking(anyLong(), any());
        verify(eventPublisher, never()).publishEvent(any(BookingSuccessEvent.class));
    }

    @Test
    @DisplayName("cancelBookingByCustomer() - Should cancel booking, release seat, and publish cancelled event")
    void shouldCancelBookingByCustomer() {
        Booking confirmedBooking = Booking.builder()
                .id(1L)
                .bookingCode("PNR-XYZ789")
                .flightId(101L)
                .customerEmail("john@example.com")
                .seatNumber("12A")
                .totalAmount(new BigDecimal("1500000"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.cancelBookingByCustomer(1L, "Change plan")).thenReturn(confirmedBooking);

        bookingFacade.cancelBookingByCustomer(1L, "Change plan");

        verify(flightFacade).releaseSeat(101L, "12A");

        ArgumentCaptor<BookingCancelledEvent> captor = ArgumentCaptor.forClass(BookingCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BookingCancelledEvent capturedEvent = captor.getValue();
        assertThat(capturedEvent.bookingId()).isEqualTo(1L);
        assertThat(capturedEvent.reason()).isEqualTo("Change plan");
        assertThat(capturedEvent.refundedAmount()).isEqualByComparingTo("1500000");
    }
}