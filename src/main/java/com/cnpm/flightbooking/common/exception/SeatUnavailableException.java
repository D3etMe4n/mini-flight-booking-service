package com.cnpm.flightbooking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatUnavailableException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "SEAT_ALREADY_RESERVED";

    public SeatUnavailableException(String seatNumber, Long flightId) {
        super(
                String.format("Seat %s on flight ID %d is already reserved or currently locked by another transaction", seatNumber, flightId),
                HttpStatus.CONFLICT);
    }

    public SeatUnavailableException(String message) {
        super(
                message,
                HttpStatus.CONFLICT
        );
    }
}