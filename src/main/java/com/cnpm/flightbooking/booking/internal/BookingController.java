package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.BookingFacade;
import com.cnpm.flightbooking.booking.dto.BookingDetailResponse;
import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.booking.dto.BookingResponse;
import com.cnpm.flightbooking.booking.dto.BookingSummaryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingFacade bookingFacade;

    @PostMapping
    public ResponseEntity<BookingResponse> bookFlight(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingFacade.bookFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingCode}")
    public ResponseEntity<BookingDetailResponse> getBookingByCode(@PathVariable String bookingCode) {
        BookingDetailResponse response = bookingFacade.getBookingByCode(bookingCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingSummaryResponse>> getBookingsByEmail(
            @RequestParam @Email(message = "Invalid email format") String email) {
        List<BookingSummaryResponse> responses = bookingFacade.getBookingsByCustomerEmail(email);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @NotBlank(message = "Cancellation reason cannot be blank")
            String reason) {
        bookingFacade.cancelBookingByCustomer(bookingId, reason);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content
    }
}