package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApplicationResponse {

    private final Long applicationId;
    private final String initialStatus;
    private final LocalDateTime createdAt;

    private ApplicationResponse(Long applicationId, String initialStatus, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.initialStatus = initialStatus;
        this.createdAt = createdAt;
    }

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getStatus().name(),
                application.getCreatedAt());
    }
}
