package com.example.honorcitizen.domain.log.entity;

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
@Table(name = "email_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false, length = 50)
    private String emailType;

    @Column(nullable = false, length = 200)
    private String recipient;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 200)
    private String failureReason;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static EmailLog create(Long applicationId, String emailType, String recipient) {
        EmailLog log = new EmailLog();
        log.applicationId = applicationId;
        log.emailType = emailType;
        log.recipient = recipient;
        log.status = "PENDING";
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public void markSent() {
        this.status = "SENT";
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = "FAILED";
        this.failureReason = reason;
    }

    // emailType 상수
    public static final String PAYMENT_COMPLETE  = "PAYMENT_COMPLETE";
    public static final String PHOTO_REJECTED    = "PHOTO_REJECTED";
    public static final String CARD_READY        = "CARD_READY";
    public static final String SHIPPING_STARTED  = "SHIPPING_STARTED";
    public static final String DELIVERED         = "DELIVERED";
}
