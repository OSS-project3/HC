package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationCreateResponse {

    private final Long applicationId;
    private final String applicationNumber;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final LocalDateTime createdAt;

    private ApplicationCreateResponse(Long applicationId, String applicationNumber,
            ApplicationStatus status, PaymentStatus paymentStatus, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public static ApplicationCreateResponse from(Application application) {
        return new ApplicationCreateResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getStatus(),
                application.getPaymentStatus(),
                application.getCreatedAt());
    }
}
