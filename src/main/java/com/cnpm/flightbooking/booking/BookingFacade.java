package com.cnpm.flightbooking.booking;

import com.cnpm.flightbooking.booking.internal.Booking;
import com.cnpm.flightbooking.booking.dto.BookingRequest;

import java.math.BigDecimal;

public interface BookingFacade {
    Booking initiateBooking(BookingRequest request);

    void confirmBooking(Long bookingId, BigDecimal finalAmount);

    void cancelBooking(Long bookingID) ;
}
