package com.cnpm.flightbooking.notification.event;

import java.math.BigDecimal;

public record BookingSuccessEvent(
        Long bookingId,
        String customerEmail,
        String flightNumber,
        String seatNumber,
        BigDecimal totalAmount
) {}