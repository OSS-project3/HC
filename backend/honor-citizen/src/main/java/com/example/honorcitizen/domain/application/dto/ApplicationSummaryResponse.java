package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationSummaryResponse {

    private final Long applicationId;
    private final String status;
    private final String koreanName;
    private final LocalDateTime createdAt;
    private final String cardNumber;

    private ApplicationSummaryResponse(
            Long applicationId,
            String status,
            String koreanName,
            LocalDateTime createdAt,
            String cardNumber) {
        this.applicationId = applicationId;
        this.status = status;
        this.koreanName = koreanName;
        this.createdAt = createdAt;
        this.cardNumber = cardNumber;
    }

    public static ApplicationSummaryResponse from(Application application) {
        return new ApplicationSummaryResponse(
                application.getId(),
                application.getStatus().name(),
                null,
                application.getCreatedAt(),
                null);
    }
}
