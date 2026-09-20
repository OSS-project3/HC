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

    // 공개 카드 다운로드(/cards/download/public)를 이 조회 건에 한해 허용하는 1회용 단기 토큰
    // (2026-09-20, CardLookupTokenService). 카드 준비 여부(status)와 무관하게 조회가 성공하면
    // 항상 발급한다 — 아직 준비 안 됐으면 다운로드 시도 시 CARD_NOT_READY로 거절된다.
    private final String cardDownloadToken;

    public ApplicationLookupResponse(Long applicationId, ApplicationType applicationType,
            String applicationNumber, String applicantNameMasked,
            String cardType, ApplicationStatus status, String photoRejectReason, LocalDateTime submittedAt,
            String cardDownloadToken) {
        this.applicationId = applicationId;
        this.applicationType = applicationType;
        this.applicationNumber = applicationNumber;
        this.applicantNameMasked = applicantNameMasked;
        this.cardType = cardType;
        this.status = status;
        this.photoRejectReason = photoRejectReason;
        this.submittedAt = submittedAt;
        this.cardDownloadToken = cardDownloadToken;
    }

    // 영어 응답용 사본 — 자유 텍스트인 photoRejectReason만 번역한다(cardType·status는 그대로).
    public ApplicationLookupResponse withTranslated(String photoRejectReason) {
        return new ApplicationLookupResponse(applicationId, applicationType, applicationNumber,
                applicantNameMasked, cardType, status, photoRejectReason, submittedAt, cardDownloadToken);
    }
}
