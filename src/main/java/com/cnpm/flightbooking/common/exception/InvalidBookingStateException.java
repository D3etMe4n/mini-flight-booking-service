package com.cnpm.flightbooking.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidBookingStateException extends BusinessException {
    public InvalidBookingStateException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}