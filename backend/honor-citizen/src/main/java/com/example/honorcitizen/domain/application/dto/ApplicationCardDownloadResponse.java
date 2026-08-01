package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationCardDownloadResponse {

    private final Long applicationId;
    private final ApplicationType applicationType;
    private final String cardFrontUrl;
    private final String cardBackUrl;
    private final String downloadUrl;
    private final LocalDateTime expiresAt;

    private ApplicationCardDownloadResponse(Long applicationId, ApplicationType applicationType,
            String cardFrontUrl, String cardBackUrl, String downloadUrl, LocalDateTime expiresAt) {
        this.applicationId = applicationId;
        this.applicationType = applicationType;
        this.cardFrontUrl = cardFrontUrl;
        this.cardBackUrl = cardBackUrl;
        this.downloadUrl = downloadUrl;
        this.expiresAt = expiresAt;
    }

    public static ApplicationCardDownloadResponse forIndividual(Long applicationId, String cardFrontUrl,
            String cardBackUrl, LocalDateTime expiresAt) {
        return new ApplicationCardDownloadResponse(
                applicationId, ApplicationType.INDIVIDUAL, cardFrontUrl, cardBackUrl, null, expiresAt);
    }

    public static ApplicationCardDownloadResponse forGroup(Long applicationId, String downloadUrl, LocalDateTime expiresAt) {
        return new ApplicationCardDownloadResponse(
                applicationId, ApplicationType.GROUP, null, null, downloadUrl, expiresAt);
    }
}
