package com.cnpm.flightbooking.payment.dto;

import com.cnpm.flightbooking.payment.internal.CustomerType;
import com.cnpm.flightbooking.payment.internal.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentRequest(
        @NotNull(message = "Booking ID must not be null")
        Long bookingId,

        @NotNull(message = "Base price must not be null")
        @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
        BigDecimal basePrice,

        @NotNull(message = "Customer type must not be null")
        CustomerType customerType,

        @NotNull(message = "Payment method must not be null")
        PaymentMethod paymentMethod,

        @Min(value = 0, message = "Reward points cannot be negative")
        int rewardPoints,

        boolean isHoliday
) {
    public PaymentRequest {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Invalid booking ID");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base price must be greater than zero");
        }
        if (customerType == null) {
            throw new IllegalArgumentException("Customer type cannot be null");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
        if (rewardPoints < 0) {
            throw new IllegalArgumentException("Reward points cannot be negative");
        }
    }
}