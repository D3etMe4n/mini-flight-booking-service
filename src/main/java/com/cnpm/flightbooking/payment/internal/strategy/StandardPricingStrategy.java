package com.cnpm.flightbooking.payment.internal.strategy;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.internal.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StandardPricingStrategy implements PaymentStrategy {
    @Override
    public CustomerType getCustomerType() {
        return CustomerType.STANDARD;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice, int rewardPoint, boolean isHoliday) {
        BigDecimal price = basePrice;
        if (isHoliday) {
            price = price.multiply(BigDecimal.valueOf(1.10)).setScale(2, RoundingMode.HALF_UP);
        }

        if(rewardPoint >= 200) {
            price = price.subtract(BigDecimal.valueOf(rewardPoint * 1000L));
        }

        BigDecimal floorPrice = basePrice.multiply(new BigDecimal("0.80"));
        price = price.max(floorPrice);

        return price;
    }
}
