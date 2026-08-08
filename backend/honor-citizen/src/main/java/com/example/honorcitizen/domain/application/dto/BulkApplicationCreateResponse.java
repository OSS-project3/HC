package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
public class BulkApplicationCreateResponse {

    private final Long applicationId;
    private final String applicationNumber;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final int totalQuantity;
    private final LocalDateTime createdAt;

    private BulkApplicationCreateResponse(Long applicationId, String applicationNumber, ApplicationStatus status,
            PaymentStatus paymentStatus, int totalQuantity, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.totalQuantity = totalQuantity;
        this.createdAt = createdAt;
    }

    public static BulkApplicationCreateResponse from(Application application) {
        return new BulkApplicationCreateResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getStatus(),
                application.getPaymentStatus(),
                application.getTotalQuantity(),
                application.getCreatedAt());
    }
}
