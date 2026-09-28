package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.BookingFacade;
import com.cnpm.flightbooking.booking.dto.BookingDetailResponse;
import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.booking.dto.BookingResponse;
import com.cnpm.flightbooking.booking.dto.BookingSummaryResponse;
import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BookingFacade bookingFacade;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Should return 201 Created when booking is successful")
    void shouldReturn201WhenBookingSucceeds() throws Exception {
        BookingRequest validRequest = BookingRequest.builder()
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

        BookingResponse mockResponse = BookingResponse.builder()
                .bookingCode("PNR-ABC123")
                .status("CONFIRMED")
                .chargedAmount(new BigDecimal("1500000"))
                .message("Booking completed successfully.")
                .build();

        when(bookingFacade.bookFlight(any(BookingRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingCode").value("PNR-ABC123"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.chargedAmount").value(1500000));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{bookingCode} - Should return 200 OK")
    void shouldReturnBookingByCode() throws Exception {
        BookingDetailResponse detailResponse = BookingDetailResponse.builder()
                .bookingCode("PNR-ABC123")
                .customerEmail("john@example.com")
                .status("CONFIRMED")
                .totalAmount(new BigDecimal("1500000"))
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingFacade.getBookingByCode("PNR-ABC123")).thenReturn(detailResponse);

        mockMvc.perform(get("/api/v1/bookings/{bookingCode}", "PNR-ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingCode").value("PNR-ABC123"))
                .andExpect(jsonPath("$.customerEmail").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings?email=... - Should return list of bookings")
    void shouldReturnBookingsByEmail() throws Exception {
        BookingSummaryResponse summary = BookingSummaryResponse.builder()
                .bookingCode("PNR-ABC123")
                .flightId(101L)
                .seatNumber("12A")
                .status("CONFIRMED")
                .build();

        when(bookingFacade.getBookingsByCustomerEmail("john@example.com")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/bookings")
                        .param("email", "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingCode").value("PNR-ABC123"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{bookingId}/cancel - Should return 204 No Content")
    void shouldReturn204WhenCancelled() throws Exception {
        doNothing().when(bookingFacade).cancelBookingByCustomer(1L, "Change travel dates");

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", 1L)
                        .param("reason", "Change travel dates"))
                .andExpect(status().isNoContent());

        verify(bookingFacade).cancelBookingByCustomer(1L, "Change travel dates");
    }
}