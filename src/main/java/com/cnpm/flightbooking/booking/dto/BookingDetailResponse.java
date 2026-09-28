package com.cnpm.flightbooking.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record BookingDetailResponse(
        Long id,
        String bookingCode,
        Long flightId,
        String customerName,
        String customerEmail,
        String customerPhone,
        String seatNumber,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}