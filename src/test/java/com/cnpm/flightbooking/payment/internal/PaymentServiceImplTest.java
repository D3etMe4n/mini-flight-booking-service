package com.cnpm.flightbooking.payment.internal;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentMethod;
import com.cnpm.flightbooking.payment.dto.PaymentQuoteResponse;
import com.cnpm.flightbooking.payment.dto.PaymentRequest;
import com.cnpm.flightbooking.payment.dto.PaymentResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PricingStrategyFactory strategyFactory;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private PaymentFacadeImpl paymentService;

    @Nested
    @DisplayName("processPayment() - Input Validation")
    class ProcessPaymentValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when request object is null")
        void shouldThrowExceptionWhenRequestIsNull() {
            assertThatThrownBy(() -> paymentService.processPayment(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Booking ID cannot be null and Base Price must be greater than zero");

            verifyNoInteractions(strategyFactory);
            verifyNoInteractions(paymentRepository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when bookingId is null")
        void shouldThrowExceptionWhenBookingIdIsNull() {
            assertThatThrownBy(() -> PaymentRequest.builder()
                    .bookingId(null)
                    .basePrice(new BigDecimal("1000000"))
                    .customerType(CustomerType.STANDARD)
                    .paymentMethod(PaymentMethod.VNPAY)
                    .rewardPoints(0)
                    .isHoliday(false)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid booking ID");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"0", "-100000"})
        @DisplayName("Should throw IllegalArgumentException when basePrice is null, zero, or negative")
        void shouldThrowExceptionWhenBasePriceIsInvalid(String priceValue) {
            BigDecimal basePrice = priceValue == null ? null : new BigDecimal(priceValue);

            assertThatThrownBy(() -> PaymentRequest.builder()
                    .bookingId(1L)
                    .basePrice(basePrice)
                    .customerType(CustomerType.STANDARD)
                    .paymentMethod(PaymentMethod.VNPAY)
                    .rewardPoints(0)
                    .isHoliday(false)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Base price must be greater than zero");
        }

        @Nested
        @DisplayName("processPayment() - Business & Gateway Execution")
        class ProcessPaymentExecutionTests {

            @Test
            @DisplayName("Should succeed and generate transactionRef when final amount is within payment limit")
            void shouldProcessPaymentSuccessfullyWhenWithinLimit() {
                BigDecimal basePrice = new BigDecimal("5000000");
                BigDecimal finalAmount = new BigDecimal("4500000");
                BigDecimal expectedDiscount = new BigDecimal("500000");

                PaymentRequest request = PaymentRequest.builder()
                        .bookingId(101L)
                        .basePrice(basePrice)
                        .customerType(CustomerType.STANDARD)
                        .paymentMethod(PaymentMethod.VNPAY)
                        .rewardPoints(100)
                        .isHoliday(false)
                        .build();

                given(strategyFactory.getStrategy(CustomerType.STANDARD)).willReturn(paymentStrategy);
                given(paymentStrategy.calculatePrice(basePrice, 100, false)).willReturn(finalAmount);

                PaymentResultResponse response = paymentService.processPayment(request);

                // Verify response payload
                assertThat(response).isNotNull();
                assertThat(response.isSuccess()).isTrue();
                assertThat(response.amountPaid()).isEqualByComparingTo(finalAmount);
                assertThat(response.transactionRef()).startsWith("TXN-");
                assertThat(response.failureReason()).isNull();

                // Verify and capture saved database entity
                ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
                verify(paymentRepository).save(captor.capture());

                PaymentTransaction savedEntity = captor.getValue();
                assertThat(savedEntity.getBookingId()).isEqualTo(101L);
                assertThat(savedEntity.getOriginalPrice()).isEqualByComparingTo(basePrice);
                assertThat(savedEntity.getDiscountAmount()).isEqualByComparingTo(expectedDiscount);
                assertThat(savedEntity.getFinalAmount()).isEqualByComparingTo(finalAmount);
                assertThat(savedEntity.getCustomerType()).isEqualTo(CustomerType.STANDARD);
                assertThat(savedEntity.getPaymentMethod()).isEqualTo(PaymentMethod.VNPAY);
                assertThat(savedEntity.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                assertThat(savedEntity.getTransactionRef()).isEqualTo(response.transactionRef());
                assertThat(savedEntity.getFailureReason()).isNull();
            }

            @Test
            @DisplayName("Should fail and save FAILED status when final amount exceeds 100,000,000")
            void shouldFailPaymentWhenAmountExceedsPaymentLimit() {
                BigDecimal basePrice = new BigDecimal("120000000");
                BigDecimal finalAmount = new BigDecimal("110000000"); // Greater than 100,000,000 limit

                PaymentRequest request = PaymentRequest.builder()
                        .bookingId(102L)
                        .basePrice(basePrice)
                        .customerType(CustomerType.VIP)
                        .paymentMethod(PaymentMethod.CREDIT_CARD)
                        .rewardPoints(0)
                        .isHoliday(true)
                        .build();

                given(strategyFactory.getStrategy(CustomerType.VIP)).willReturn(paymentStrategy);
                given(paymentStrategy.calculatePrice(basePrice, 0, true)).willReturn(finalAmount);

                PaymentResultResponse response = paymentService.processPayment(request);

                // Verify response payload
                assertThat(response).isNotNull();
                assertThat(response.isSuccess()).isFalse();
                assertThat(response.amountPaid()).isEqualByComparingTo(finalAmount);
                assertThat(response.transactionRef()).isNull();
                assertThat(response.failureReason()).isEqualTo("Payment limit exceeded or invalid card.");

                // Verify saved transaction in audit log
                ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
                verify(paymentRepository).save(captor.capture());

                PaymentTransaction savedEntity = captor.getValue();
                assertThat(savedEntity.getStatus()).isEqualTo(PaymentStatus.FAILED);
                assertThat(savedEntity.getTransactionRef()).isNull();
                assertThat(savedEntity.getFailureReason()).isEqualTo("Payment limit exceeded or invalid card.");
            }

            @Test
            @DisplayName("Should succeed when final amount is exactly equal to the 100,000,000 threshold")
            void shouldSucceedWhenAmountIsExactlyAtThreshold() {
                BigDecimal thresholdAmount = new BigDecimal("100000000");

                PaymentRequest request = PaymentRequest.builder()
                        .bookingId(103L)
                        .basePrice(thresholdAmount)
                        .customerType(CustomerType.STANDARD)
                        .paymentMethod(PaymentMethod.MOMO)
                        .rewardPoints(0)
                        .isHoliday(false)
                        .build();

                given(strategyFactory.getStrategy(CustomerType.STANDARD)).willReturn(paymentStrategy);
                given(paymentStrategy.calculatePrice(thresholdAmount, 0, false)).willReturn(thresholdAmount);

                PaymentResultResponse response = paymentService.processPayment(request);

                assertThat(response.isSuccess()).isTrue();
                assertThat(response.transactionRef()).startsWith("TXN-");
                assertThat(response.failureReason()).isNull();

                verify(paymentRepository).save(any(PaymentTransaction.class));
            }
        }

        @Nested
        @DisplayName("quotePrice() - Price Estimation Tests")
        class QuotePriceTests {

            @ParameterizedTest
            @NullSource
            @ValueSource(strings = {"0", "-500000"})
            @DisplayName("Should throw IllegalArgumentException when base price is null, zero, or negative")
            void shouldThrowExceptionWhenQuoteBasePriceIsInvalid(String invalidPriceStr) {
                BigDecimal invalidPrice = invalidPriceStr == null ? null : new BigDecimal(invalidPriceStr);

                assertThatThrownBy(() -> paymentService.quotePrice(invalidPrice, CustomerType.STANDARD, 0, false))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Base price must be greater than zero and cannot be null");

                verifyNoInteractions(strategyFactory);
                verifyNoInteractions(paymentRepository);
            }

            @Test
            @DisplayName("Should correctly calculate and return quoted prices without persisting audit logs")
            void shouldReturnQuoteResponseSuccessfullyWithoutDatabaseInteraction() {
                BigDecimal basePrice = new BigDecimal("2000000");
                BigDecimal finalAmount = new BigDecimal("1800000");
                BigDecimal expectedDiscount = new BigDecimal("200000");

                given(strategyFactory.getStrategy(CustomerType.VIP)).willReturn(paymentStrategy);
                given(paymentStrategy.calculatePrice(basePrice, 50, false)).willReturn(finalAmount);

                PaymentQuoteResponse response = paymentService.quotePrice(basePrice, CustomerType.VIP, 50, false);

                assertThat(response).isNotNull();
                assertThat(response.originalPrice()).isEqualByComparingTo(basePrice);
                assertThat(response.discountAmount()).isEqualByComparingTo(expectedDiscount);
                assertThat(response.finalAmount()).isEqualByComparingTo(finalAmount);

                // Ensure quotePrice has zero database side-effects
                verifyNoInteractions(paymentRepository);
            }
        }
    }
}