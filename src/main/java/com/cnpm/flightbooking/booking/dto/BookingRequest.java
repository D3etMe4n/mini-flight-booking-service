package com.cnpm.flightbooking.booking.dto;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record BookingRequest(
        @NotNull(message = "Flight ID must not be null")
        Long flightId,

        @NotBlank(message = "Seat number must not be blank")
        @Pattern(regexp = "^[0-9]{1,2}[A-F]$", message = "Invalid seat format (e.g., 12A, 1B)")
        String seatNumber,

        @NotBlank(message = "Customer name must not be blank")
        String customerName,

        @NotBlank(message = "Customer email must not be blank")
        @Email(message = "Invalid email format")
        String customerEmail,

        @NotBlank(message = "Customer phone must not be blank")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid phone number format")
        String customerPhone,

        @NotNull(message = "Customer type must not be null")
        CustomerType customerType,

        @NotNull(message = "Payment method must not be null")
        PaymentMethod paymentMethod,

        @Min(value = 0, message = "Reward points cannot be negative")
        int rewardPoints,

        boolean isHoliday
) {}