package com.cnpm.flightbooking.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BookingResponse(
        String bookingCode,
        String status,
        BigDecimal chargedAmount,
        String message
) {}