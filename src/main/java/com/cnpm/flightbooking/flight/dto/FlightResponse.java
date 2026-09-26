package com.cnpm.flightbooking.flight.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FlightResponse(Long flightId,
                             String flightNumber,
                             Integer availableSeats,
                             BigDecimal basePrice) {
}
