package com.cnpm.flightbooking.payment.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerType customerType; // STANDARD, VIP, CHILDREN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod; // VNPAY, CREDIT_CARD, MOMO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status; // SUCCESS, FAILED, REFUNDED

    private String transactionRef;

    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @PrePersist
    protected void onCreate() {
        this.transactionDate = LocalDateTime.now();
    }
}