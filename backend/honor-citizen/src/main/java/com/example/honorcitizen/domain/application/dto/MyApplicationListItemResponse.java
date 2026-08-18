package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.PaymentStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyApplicationListItemResponse {

    private final Long applicationId;
    private final String applicationNumber;
    private final ApplicationType applicationType;
    private final Long cardTypeId;
    private final String cardTypeName;
    private final int totalQuantity;
    private final ApplicationStatus status;
    private final PaymentStatus paymentStatus;
    private final LocalDateTime createdAt;

    private MyApplicationListItemResponse(Long applicationId, String applicationNumber, ApplicationType applicationType,
            Long cardTypeId, String cardTypeName, int totalQuantity, ApplicationStatus status,
            PaymentStatus paymentStatus, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.applicationType = applicationType;
        this.cardTypeId = cardTypeId;
        this.cardTypeName = cardTypeName;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    // cardTypeName은 배치 조회 결과라 Application 엔티티 자체에서 얻을 수 없어 인자로 받는다(Review 패턴과 동일).
    public static MyApplicationListItemResponse of(Application application, String cardTypeName) {
        return new MyApplicationListItemResponse(application.getId(), application.getApplicationNumber(),
                application.getApplicationType(), application.getCardTypeId(), cardTypeName,
                application.getTotalQuantity(), application.getStatus(), application.getPaymentStatus(),
                application.getCreatedAt());
    }
}
