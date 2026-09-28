package com.cnpm.flightbooking.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record BookingSummaryResponse(
        String bookingCode,
        Long flightId,
        String seatNumber,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt
) {}