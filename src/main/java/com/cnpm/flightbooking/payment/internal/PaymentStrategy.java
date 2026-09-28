package com.cnpm.flightbooking.payment.internal;

import com.cnpm.flightbooking.payment.CustomerType;

import java.math.BigDecimal;

public interface PaymentStrategy {
    CustomerType getCustomerType();
    BigDecimal calculatePrice(BigDecimal basePrice, int rewardPoint, boolean isHoliday);
}
