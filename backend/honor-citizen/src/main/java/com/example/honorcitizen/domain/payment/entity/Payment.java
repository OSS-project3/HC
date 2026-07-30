package com.example.honorcitizen.domain.payment.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.PaymentMethod;
import com.example.honorcitizen.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 200)
    private String paymentKey;

    @Column(length = 100)
    private String depositorName;

    @Column(nullable = false)
    private long serviceAmount;

    @Column(nullable = false)
    private long shippingFee;

    @Column(nullable = false)
    private long totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(length = 500)
    private String receiptUrl;

    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;

    public static Payment create(Long applicationId, String orderId, PaymentMethod paymentMethod,
            String paymentKey, String depositorName,
            long serviceAmount, long shippingFee, LocalDateTime expiredAt) {
        Payment p = new Payment();
        p.applicationId = applicationId;
        p.orderId = orderId;
        p.paymentMethod = paymentMethod;
        p.paymentKey = paymentKey;
        p.depositorName = depositorName;
        p.serviceAmount = serviceAmount;
        p.shippingFee = shippingFee;
        p.totalAmount = serviceAmount + shippingFee;
        p.currency = "KRW";
        p.paymentStatus = PaymentStatus.WAITING_DEPOSIT;
        p.expiredAt = expiredAt;
        return p;
    }

    public void reissue(String orderId, String paymentKey, String depositorName, LocalDateTime expiredAt) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.depositorName = depositorName;
        this.expiredAt = expiredAt;
        this.paymentStatus = PaymentStatus.WAITING_DEPOSIT;
        this.paidAt = null;
    }

    public void complete(String receiptUrl) {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.receiptUrl = receiptUrl;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        this.paymentStatus = PaymentStatus.FAILED;
    }
}
