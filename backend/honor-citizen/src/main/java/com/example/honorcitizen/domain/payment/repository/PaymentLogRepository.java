package com.example.honorcitizen.domain.payment.repository;

import com.example.honorcitizen.domain.payment.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {}
