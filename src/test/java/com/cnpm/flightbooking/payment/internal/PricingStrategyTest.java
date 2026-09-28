package com.cnpm.flightbooking.payment.internal;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.internal.strategy.ChildrenPricingStrategy;
import com.cnpm.flightbooking.payment.internal.strategy.StandardPricingStrategy;
import com.cnpm.flightbooking.payment.internal.strategy.VipPricingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingStrategyTest {

    @Nested
    @DisplayName("StandardPricingStrategy Tests")
    class StandardPricingStrategyTests {

        private final StandardPricingStrategy strategy = new StandardPricingStrategy();

        @Test
        @DisplayName("CustomerType should be STANDARD")
        void shouldReturnStandardCustomerType() {
            assertThat(strategy.getCustomerType()).isEqualTo(CustomerType.STANDARD);
        }

        @Test
        @DisplayName("C1/C2: Normal day, points < 200 -> No surcharge, no discount")
        void normalDayPointsUnderThreshold() {
            BigDecimal base = new BigDecimal("1000000");
            BigDecimal price = strategy.calculatePrice(base, 150, false);
            assertThat(price).isEqualByComparingTo("1000000.00");
        }

        @Test
        @DisplayName("C1/C2: Holiday = true, points < 200 -> 10% surcharge, no discount")
        void holidayPointsUnderThreshold() {
            BigDecimal base = new BigDecimal("1000000");
            // 1,000,000 * 1.10 = 1,100,000.00
            BigDecimal price = strategy.calculatePrice(base, 0, true);
            assertThat(price).isEqualByComparingTo("1100000.00");
        }

        @Test
        @DisplayName("C1/C2: Normal day, points >= 200, price above 80% floor -> Apply points discount")
        void normalDayPointsDeduction() {
            BigDecimal base = new BigDecimal("1000000");
            BigDecimal price = strategy.calculatePrice(base, 200, false);
            assertThat(price).isEqualByComparingTo("800000.00");
        }

        @Test
        @DisplayName("C1/C2: Points deduction exceeds floor -> Cap at floor price (80% basePrice)")
        void pointsDeductionExceedsFloor() {
            BigDecimal base = new BigDecimal("1000000");
            BigDecimal price = strategy.calculatePrice(base, 500, false);
            assertThat(price).isEqualByComparingTo("800000.00");
        }
    }

    @Nested
    @DisplayName("VipPricingStrategy Tests")
    class VipPricingStrategyTests {

        private final VipPricingStrategy strategy = new VipPricingStrategy();

        @Test
        @DisplayName("CustomerType should be VIP")
        void shouldReturnVipCustomerType() {
            assertThat(strategy.getCustomerType()).isEqualTo(CustomerType.VIP);
        }

        @Test
        @DisplayName("C1/C2: points < 500 -> 15% discount")
        void pointsUnder500() {
            BigDecimal base = new BigDecimal("1000000");
            // 1,000,000 * (1 - 0.15) = 850,000.00
            BigDecimal price = strategy.calculatePrice(base, 400, false);
            assertThat(price).isEqualByComparingTo("850000.00");
        }

        @Test
        @DisplayName("C1/C2: 500 <= points < 1000 -> 15% + 5% = 20% discount")
        void pointsBetween500And1000() {
            BigDecimal base = new BigDecimal("1000000");
            // 1,000,000 * (1 - 0.20) = 800,000.00
            BigDecimal price = strategy.calculatePrice(base, 500, true);
            assertThat(price).isEqualByComparingTo("800000.00");
        }

        @Test
        @DisplayName("C1/C2: points >= 1000 -> 15% + 10% = 25% discount")
        void pointsOver1000() {
            BigDecimal base = new BigDecimal("1000000");
            // 1,000,000 * (1 - 0.25) = 750,000.00
            BigDecimal price = strategy.calculatePrice(base, 1000, false);
            assertThat(price).isEqualByComparingTo("750000.00");
        }

        @Test
        @DisplayName("C1/C2: Defensive check - throw IllegalArgumentException when basePrice is null or negative")
        void invalidBasePriceThrowsException() {
            assertThatThrownBy(() -> strategy.calculatePrice(null, 0, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Base price must be non-negative");

            assertThatThrownBy(() -> strategy.calculatePrice(new BigDecimal("-100"), 0, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Base price must be non-negative");
        }
    }

    @Nested
    @DisplayName("ChildrenPricingStrategy Tests")
    class ChildrenPricingStrategyTests {

        private final ChildrenPricingStrategy strategy = new ChildrenPricingStrategy();

        @Test
        @DisplayName("CustomerType should be CHILDREN")
        void shouldReturnChildrenCustomerType() {
            assertThat(strategy.getCustomerType()).isEqualTo(CustomerType.CHILDREN);
        }

        @Test
        @DisplayName("C1: Always exactly 50% of basePrice")
        void alwaysFiftyPercent() {
            BigDecimal base = new BigDecimal("1500000");
            // 1,500,000 * 0.50 = 750,000.00
            BigDecimal price = strategy.calculatePrice(base, 999, true);
            assertThat(price).isEqualByComparingTo("750000.00");
        }
    }
}