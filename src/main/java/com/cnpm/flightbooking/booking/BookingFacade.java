package com.cnpm.flightbooking.booking;

import com.cnpm.flightbooking.booking.dto.*;

import java.util.List;

public interface BookingFacade {
    BookingResponse bookFlight(BookingRequest request);

    BookingDetailResponse getBookingByCode(String bookingCode);

    List<BookingSummaryResponse> getBookingsByCustomerEmail(String email);

    void cancelBookingByCustomer(Long bookingId, String reason);
}
