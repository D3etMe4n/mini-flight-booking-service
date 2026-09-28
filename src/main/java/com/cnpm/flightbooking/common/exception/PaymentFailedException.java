package com.cnpm.flightbooking.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "PAYMENT_DECLINED";

    public PaymentFailedException(String reason) {
        super(
                String.format("Payment processing failed: %s", reason != null ? reason : "Unknown reason"),
                HttpStatus.BAD_REQUEST
        );
    }

    public PaymentFailedException(String reason, Throwable cause) {
        super(
                String.format("Payment processing failed: %s", reason != null ? reason : "Unknown reason"),
                HttpStatus.BAD_REQUEST
        );
        initCause(cause);
    }
}