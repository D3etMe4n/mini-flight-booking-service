package com.cnpm.flightbooking.payment.internal;

import com.cnpm.flightbooking.payment.CustomerType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


// Oh Factory Pattern muhahahaha
@Component
public class PricingStrategyFactory {

    private final Map<CustomerType, PaymentStrategy> strategyMap;

    //  Spring will automatically inherit strategies and inject to this
    public PricingStrategyFactory(List<PaymentStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentStrategy::getCustomerType,
                        Function.identity()
                ));
    }

    public PaymentStrategy getStrategy(CustomerType type) {
        return Optional.ofNullable(strategyMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Payment strategy not found for customer type: " + type));
    }
}