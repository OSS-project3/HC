package com.example.honorcitizen.domain.payment.repository;

import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByApplicationId(Long applicationId);
    Optional<Payment> findByOrderId(String orderId);
    boolean existsByApplicationIdAndPaymentStatus(Long applicationId, PaymentStatus status);
}
