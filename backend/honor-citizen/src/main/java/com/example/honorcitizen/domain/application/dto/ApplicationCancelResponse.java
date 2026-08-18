package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationCancelResponse {

    private final Long applicationId;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final boolean refundRequired;
    private final LocalDateTime cancelledAt;

    private ApplicationCancelResponse(Long applicationId, ApplicationStatus status,
            PaymentStatus paymentStatus, boolean refundRequired, LocalDateTime cancelledAt) {
        this.applicationId = applicationId;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.refundRequired = refundRequired;
        this.cancelledAt = cancelledAt;
    }

    public static ApplicationCancelResponse from(Application application) {
        boolean refundRequired = application.getPaymentStatus() == PaymentStatus.CONFIRMED
                && application.getRefundedAt() == null;
        return new ApplicationCancelResponse(
                application.getId(), application.getStatus(), application.getPaymentStatus(),
                refundRequired, application.getCancelledAt());
    }
}
