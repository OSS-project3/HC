package com.example.honorcitizen.domain.log.entity;

import com.example.honorcitizen.common.enums.ApplicationStatus;
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
@Table(name = "application_status_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus toStatus;

    private Long changedBy;

    @Column(length = 200)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ApplicationStatusLog create(Long applicationId,
            ApplicationStatus fromStatus, ApplicationStatus toStatus,
            Long changedBy, String reason) {
        ApplicationStatusLog log = new ApplicationStatusLog();
        log.applicationId = applicationId;
        log.fromStatus = fromStatus;
        log.toStatus = toStatus;
        log.changedBy = changedBy;
        log.reason = reason;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
