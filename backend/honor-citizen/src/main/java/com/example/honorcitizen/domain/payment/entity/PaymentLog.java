package com.example.honorcitizen.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(length = 20)
    private String paymentMethod;

    private Long amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 200)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String pgResponse;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PaymentLog create(Long paymentId, String eventType, String paymentMethod,
            Long amount, String status, String failureReason, String pgResponse) {
        PaymentLog log = new PaymentLog();
        log.paymentId = paymentId;
        log.eventType = eventType;
        log.paymentMethod = paymentMethod;
        log.amount = amount;
        log.status = status;
        log.failureReason = failureReason;
        log.pgResponse = pgResponse;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
