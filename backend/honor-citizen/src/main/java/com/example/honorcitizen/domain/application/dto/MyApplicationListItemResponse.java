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
    // 단체 신청 작명 업무 진행률(2026-09-24)에서만 쓰인다 — 카드 앞·뒷면이 모두 생성된 멤버 수.
    // 개인 신청은 항상 null(완료 판정에 status만으로 충분해 불필요).
    private final Integer completedMemberCount;

    private MyApplicationListItemResponse(Long applicationId, String applicationNumber, ApplicationType applicationType,
            Long cardTypeId, String cardTypeName, int totalQuantity, ApplicationStatus status,
            PaymentStatus paymentStatus, LocalDateTime createdAt, Integer completedMemberCount) {
        this.applicationId = applicationId;
        this.applicationNumber = applicationNumber;
        this.applicationType = applicationType;
        this.cardTypeId = cardTypeId;
        this.cardTypeName = cardTypeName;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.completedMemberCount = completedMemberCount;
    }

    // cardTypeName은 배치 조회 결과라 Application 엔티티 자체에서 얻을 수 없어 인자로 받는다(Review 패턴과 동일).
    public static MyApplicationListItemResponse of(Application application, String cardTypeName) {
        return of(application, cardTypeName, null);
    }

    public static MyApplicationListItemResponse of(Application application, String cardTypeName,
            Integer completedMemberCount) {
        return new MyApplicationListItemResponse(application.getId(), application.getApplicationNumber(),
                application.getApplicationType(), application.getCardTypeId(), cardTypeName,
                application.getTotalQuantity(), application.getStatus(), application.getPaymentStatus(),
                application.getCreatedAt(), completedMemberCount);
    }
}
