package com.cnpm.flightbooking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatNotFoundException extends BusinessException {

    public SeatNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public SeatNotFoundException(String seatNumber, Long flightId) {
        super(String.format("Seat number %s not found  on flight %d", seatNumber, flightId), HttpStatus.NOT_FOUND);
    }
}