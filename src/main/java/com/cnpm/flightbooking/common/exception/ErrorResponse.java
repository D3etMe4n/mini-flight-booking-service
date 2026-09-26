package com.cnpm.flightbooking.common.exception;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(LocalDateTime timestamp,
                            Integer status,
                            String error,
                            String message) {
}
