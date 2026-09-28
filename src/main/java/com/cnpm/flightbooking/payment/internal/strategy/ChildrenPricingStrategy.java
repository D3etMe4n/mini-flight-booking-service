package com.cnpm.flightbooking.payment.internal.strategy;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.internal.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ChildrenPricingStrategy implements PaymentStrategy {
    @Override
    public CustomerType getCustomerType() {
        return CustomerType.CHILDREN;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice, int rewardPoint, boolean isHoliday) {
        return basePrice.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
    }
}
