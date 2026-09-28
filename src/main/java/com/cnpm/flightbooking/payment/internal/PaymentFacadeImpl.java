package com.cnpm.flightbooking.payment.internal;

import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentFacade;
import com.cnpm.flightbooking.payment.dto.PaymentQuoteResponse;
import com.cnpm.flightbooking.payment.dto.PaymentRequest;
import com.cnpm.flightbooking.payment.dto.PaymentResultResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;


@Service
@AllArgsConstructor
@Transactional(readOnly = true)
class PaymentFacadeImpl implements PaymentFacade {

    private static final BigDecimal PAYMENT_LIMIT = new BigDecimal("100000000");

    private final PaymentRepository paymentRepository;
    private final PricingStrategyFactory strategyFactory;

    @Override
    @Transactional
    public PaymentResultResponse processPayment(PaymentRequest request) {
        if (request == null
                || request.bookingId() == null
                || request.basePrice() == null
                || request.basePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Booking ID cannot be null and Base Price must be greater than zero");
        }

        // Strategy Pattern
        PaymentStrategy paymentStrategy = strategyFactory.getStrategy(request.customerType());
        BigDecimal finalAmount = paymentStrategy.calculatePrice(request.basePrice(), request.rewardPoints(), request.isHoliday());
        BigDecimal discountAmount = request.basePrice().subtract(finalAmount);

        // Mock Gateway Check ( if the amount is larger than PAYMENT_LIMIT then decline )
        PaymentStatus status;
        String transactionRef = null;
        String failureReason = null;
        boolean isSuccess;

        if (finalAmount.compareTo(PAYMENT_LIMIT) > 0) {
            status = PaymentStatus.FAILED;
            failureReason = "Payment limit exceeded or invalid card.";
            isSuccess = false;
        } else {
            status = PaymentStatus.SUCCESS;
            transactionRef = "TXN-" + UUID.randomUUID();
            isSuccess = true;
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .bookingId(request.bookingId())
                .originalPrice(request.basePrice())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .customerType(request.customerType())
                .paymentMethod(request.paymentMethod())
                .status(status)
                .transactionRef(transactionRef)
                .failureReason(failureReason)
                .build();

        paymentRepository.save(transaction);

        return PaymentResultResponse.builder()
                .isSuccess(isSuccess)
                .amountPaid(finalAmount)
                .transactionRef(transactionRef)
                .failureReason(failureReason)
                .build();
    }

    @Override
    public PaymentQuoteResponse quotePrice(BigDecimal basePrice, CustomerType type, int points, boolean isHoliday) {
        if(basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Base price must be greater than zero and cannot be null");
        }

        PaymentStrategy strategy = strategyFactory.getStrategy(type);
        BigDecimal finalAmount = strategy.calculatePrice(basePrice, points, isHoliday);
        BigDecimal discountAmount = basePrice.subtract(finalAmount);

        return PaymentQuoteResponse.builder()
                .originalPrice(basePrice)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }
}
