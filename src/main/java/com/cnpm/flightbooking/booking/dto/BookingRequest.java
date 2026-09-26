package com.cnpm.flightbooking.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record BookingRequest(
        @NotNull(message = "Flight ID cannot be null")
        Long flightId,

        @NotBlank(message = "Seat number cannot be blank")
        @Pattern(regexp = "^[0-9]{1,2}[A-F]$", message = "Invalid seat number format (e.g., 12A)")
        String seatNumber,

        @NotBlank(message = "Customer name cannot be blank")
        String customerName,

        @NotBlank(message = "Customer email cannot be blank")
        @Email(message = "Invalid email format")
        String customerEmail,

        @NotBlank(message = "Customer phone cannot be blank")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid phone number format")
        String customerPhone
) {}