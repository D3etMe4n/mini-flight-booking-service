package com.cnpm.flightbooking.payment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {
}
