package com.cnpm.flightbooking.common.exception;

import org.springframework.http.HttpStatus;

public class FlightNotFoundException extends BusinessException {

    public FlightNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public FlightNotFoundException(Long flightId) {
        super("Flight not found with ID: " + flightId, HttpStatus.NOT_FOUND);
    }
}