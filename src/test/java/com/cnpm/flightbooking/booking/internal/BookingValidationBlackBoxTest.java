package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional Black-Box Testing Suite.
 * Validates specifications using Boundary Value Analysis (BVA) and Equivalence Partitioning (EP)
 * without relying on internal implementation details.
 */
class BookingValidationBlackBoxTest {

    private Validator validator;

    @BeforeEach
    void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    private BookingRequest.BookingRequestBuilder createDefaultValidBuilder() {
        return BookingRequest.builder()
                .flightId(1L)
                .seatNumber("1A")
                .customerName("Nguyen Van A")
                .customerEmail("valid.user@example.com")
                .customerPhone("0987654321")
                .customerType(CustomerType.STANDARD)
                .paymentMethod(PaymentMethod.VNPAY)
                .rewardPoints(0)
                .isHoliday(false);
    }

    // =========================================================================
    // BLACK-BOX TEST: SEAT NUMBER BOUNDARY VALUE ANALYSIS (BVA)
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"1A", "9A", "10F", "99F"})
    @DisplayName("BVA - Valid seat number boundaries should pass validation")
    void validSeatNumberBoundariesShouldPass(String seat) {
        BookingRequest request = createDefaultValidBuilder().seatNumber(seat).build();
        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0A", "100A", "1G", "12", "A1", "", " "})
    @DisplayName("BVA - Invalid seat number boundaries should be rejected")
    void invalidSeatNumberBoundariesShouldFail(String seat) {
        BookingRequest request = createDefaultValidBuilder().seatNumber(seat).build();
        assertThat(validator.validate(request)).isNotEmpty();
    }

    // =========================================================================
    // BLACK-BOX TEST: EMAIL EQUIVALENCE PARTITIONING (EP)
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"user@gmail.com", "customer.vip@company.vn", "a@b.co"})
    @DisplayName("EP - Valid email partitions should pass validation")
    void validEmailPartitionsShouldPass(String email) {
        BookingRequest request = createDefaultValidBuilder().customerEmail(email).build();
        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "missing@domain", "@missinguser.com", "user@.com"})
    @DisplayName("EP - Invalid email partitions should be rejected")
    void invalidEmailPartitionsShouldFail(String email) {
        BookingRequest request = createDefaultValidBuilder().customerEmail(email).build();
        assertThat(validator.validate(request)).isNotEmpty();
    }

    // =========================================================================
    // BLACK-BOX TEST: REWARD POINTS BOUNDARY VALUE ANALYSIS (BVA)
    // =========================================================================

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1000})
    @DisplayName("BVA - Non-negative reward points should pass validation")
    void validRewardPointsShouldPass(int points) {
        BookingRequest request = createDefaultValidBuilder().rewardPoints(points).build();
        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100})
    @DisplayName("BVA - Negative reward points boundary should be rejected")
    void negativeRewardPointsShouldFail(int points) {
        BookingRequest request = createDefaultValidBuilder().rewardPoints(points).build();
        assertThat(validator.validate(request)).isNotEmpty();
    }
}