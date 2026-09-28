package com.cnpm.flightbooking.notification.event;

import java.math.BigDecimal;

public record BookingCancelledEvent(
        Long bookingId,
        String bookingCode,
        String customerEmail,
        Long flightId,
        String seatNumber,
        BigDecimal refundedAmount,
        String reason
) {}