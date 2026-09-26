package com.cnpm.flightbooking.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentQuoteResponse(BigDecimal originalPrice,
                                   BigDecimal discountAmount,
                                   BigDecimal finalAmount) {
}
