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
        @Pattern(regexp = "^[1-9][0-9]?[A-F]$", message = "Invalid seat format (e.g., 1A, 12A, max 99F)")
        String seatNumber,

        @NotBlank(message = "Customer name must not be blank")
        String customerName,

        @NotBlank(message = "Customer email must not be blank")
        @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
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