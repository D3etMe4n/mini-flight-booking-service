package com.cnpm.flightbooking.payment.internal.strategy;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.internal.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class VipPricingStrategy implements PaymentStrategy {

    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.30");

    @Override
    public CustomerType getCustomerType() {
        return CustomerType.VIP;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice, int rewardPoint, boolean isHoliday) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price must be non-negative");
        }

        BigDecimal discountRate = new BigDecimal("0.15");

        if (rewardPoint >= 1000) {
            discountRate = discountRate.add(new BigDecimal("0.10"));
        } else if (rewardPoint >= 500) {
            discountRate = discountRate.add(new BigDecimal("0.05"));
        }

        if (discountRate.compareTo(MAX_DISCOUNT_RATE) > 0) {
            discountRate = MAX_DISCOUNT_RATE;
        }

        BigDecimal multiplier = BigDecimal.ONE
                .subtract(discountRate);

        return basePrice.multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
