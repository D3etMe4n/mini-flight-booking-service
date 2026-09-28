package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.dto.BookingDetailResponse;
import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.booking.dto.BookingSummaryResponse;
import com.cnpm.flightbooking.common.exception.BookingNotFoundException;
import com.cnpm.flightbooking.common.exception.InvalidBookingStateException;
import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private Booking sampleBooking;
    private BookingRequest validRequest;

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

        sampleBooking = Booking.builder()
                .id(1L)
                .bookingCode("PNR-ABC123")
                .flightId(101L)
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .customerPhone("0987654321")
                .seatNumber("12A")
                .totalAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("initiateBooking()")
    class InitiateBookingTests {

        @Test
        @DisplayName("Should generate PNR code, set PENDING status, and save booking")
        void shouldInitiateBookingSuccessfully() {
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Booking result = bookingService.initiateBooking(validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getBookingCode()).startsWith("PNR-");
            assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getFlightId()).isEqualTo(101L);

            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when request or flightId is invalid")
        void shouldThrowExceptionWhenRequestInvalid() {
            BookingRequest invalidRequest = BookingRequest.builder().flightId(null).build();

            assertThatThrownBy(() -> bookingService.initiateBooking(invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> bookingService.initiateBooking(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(bookingRepository);
        }
    }

    @Nested
    @DisplayName("confirmBooking()")
    class ConfirmBookingTests {

        @Test
        @DisplayName("Should update status to CONFIRMED and update final amount")
        void shouldConfirmBookingSuccessfully() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

            BigDecimal finalAmount = new BigDecimal("1500000");
            bookingService.confirmBooking(1L, finalAmount);

            assertThat(sampleBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(sampleBooking.getTotalAmount()).isEqualTo(finalAmount);
            verify(bookingRepository).save(sampleBooking);
        }

        @Test
        @DisplayName("Should throw BookingNotFoundException when booking not found")
        void shouldThrowWhenBookingNotFound() {
            when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.confirmBooking(99L, BigDecimal.TEN))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelBookingByCustomer()")
    class CancelBookingByCustomerTests {

        @Test
        @DisplayName("Should cancel confirmed booking successfully")
        void shouldCancelConfirmedBooking() {
            sampleBooking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

            Booking result = bookingService.cancelBookingByCustomer(1L, "Schedule conflict");

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository).save(sampleBooking);
        }

        @Test
        @DisplayName("Should throw InvalidBookingStateException if booking is already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            sampleBooking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

            assertThatThrownBy(() -> bookingService.cancelBookingByCustomer(1L, "Reason"))
                    .isInstanceOf(InvalidBookingStateException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("Should throw InvalidBookingStateException if booking is still PENDING")
        void shouldThrowWhenNotConfirmed() {
            sampleBooking.setStatus(BookingStatus.PENDING);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

            assertThatThrownBy(() -> bookingService.cancelBookingByCustomer(1L, "Reason"))
                    .isInstanceOf(InvalidBookingStateException.class)
                    .hasMessageContaining("Only CONFIRMED bookings can be cancelled");
        }
    }

    @Nested
    @DisplayName("getBookingByCode() and getBookingsByEmail()")
    class QueryTests {

        @Test
        @DisplayName("Should return BookingDetailResponse when booking code exists")
        void shouldReturnDetailResponse() {
            when(bookingRepository.findByBookingCode("PNR-ABC123")).thenReturn(Optional.of(sampleBooking));

            BookingDetailResponse response = bookingService.getBookingByCode("PNR-ABC123");

            assertThat(response.bookingCode()).isEqualTo("PNR-ABC123");
            assertThat(response.customerEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should return list of summary responses by email")
        void shouldReturnSummaryList() {
            when(bookingRepository.findByCustomerEmailOrderByCreatedAtDesc("john@example.com"))
                    .thenReturn(List.of(sampleBooking));

            List<BookingSummaryResponse> list = bookingService.getBookingsByEmail("john@example.com");

            assertThat(list).hasSize(1);
            assertThat(list.get(0).bookingCode()).isEqualTo("PNR-ABC123");
        }
    }
}