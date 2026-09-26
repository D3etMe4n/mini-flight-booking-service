package com.cnpm.flightbooking.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentResultResponse(boolean isSuccess,
                                    BigDecimal amountPaid,
                                    String transactionRef,
                                    String failureReason) {
}
