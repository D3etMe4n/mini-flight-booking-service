package com.cnpm.flightbooking.common.exception;

import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends BusinessException {

    public BookingNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public BookingNotFoundException(Long bookingId) {
        super("Booking not found with ID: " + bookingId, HttpStatus.NOT_FOUND);
    }
}