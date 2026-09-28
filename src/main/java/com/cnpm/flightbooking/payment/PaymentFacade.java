package com.cnpm.flightbooking.payment;

import com.cnpm.flightbooking.payment.dto.PaymentQuoteResponse;
import com.cnpm.flightbooking.payment.dto.PaymentRequest;
import com.cnpm.flightbooking.payment.dto.PaymentResultResponse;

import java.math.BigDecimal;

public interface PaymentFacade {
    PaymentResultResponse processPayment(PaymentRequest request);
    PaymentQuoteResponse quotePrice(BigDecimal basePrice, CustomerType type, int points, boolean isHoliday);
}
