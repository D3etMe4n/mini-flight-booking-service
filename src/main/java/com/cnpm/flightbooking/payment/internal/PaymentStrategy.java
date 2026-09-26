package com.cnpm.flightbooking.payment.internal;

import java.math.BigDecimal;

public interface PaymentStrategy {
    CustomerType getCustomerType();
    BigDecimal calculatePrice(BigDecimal basePrice, int rewardPoint, boolean isHoliday);
}
