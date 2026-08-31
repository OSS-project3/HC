package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationLookupResponse {

    private final Long applicationId;
    private final ApplicationType applicationType;
    private final String applicationNumber;
    private final String applicantNameMasked;
    private final String cardType;
    private final ApplicationStatus status;
    private final String photoRejectReason;
    private final LocalDateTime submittedAt;

    public ApplicationLookupResponse(Long applicationId, ApplicationType applicationType,
            String applicationNumber, String applicantNameMasked,
            String cardType, ApplicationStatus status, String photoRejectReason, LocalDateTime submittedAt) {
        this.applicationId = applicationId;
        this.applicationType = applicationType;
        this.applicationNumber = applicationNumber;
        this.applicantNameMasked = applicantNameMasked;
        this.cardType = cardType;
        this.status = status;
        this.photoRejectReason = photoRejectReason;
        this.submittedAt = submittedAt;
    }

    // 영어 응답용 사본 — 자유 텍스트인 photoRejectReason만 번역한다(cardType·status는 그대로).
    public ApplicationLookupResponse withTranslated(String photoRejectReason) {
        return new ApplicationLookupResponse(applicationId, applicationType, applicationNumber,
                applicantNameMasked, cardType, status, photoRejectReason, submittedAt);
    }
}
