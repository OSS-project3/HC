package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import lombok.Getter;

@Getter
public class ApplicationStatusResponse {

    private final Long applicationId;
    private final ApplicationStatus status;

    private ApplicationStatusResponse(Long applicationId, ApplicationStatus status) {
        this.applicationId = applicationId;
        this.status = status;
    }

    public static ApplicationStatusResponse of(Long applicationId, ApplicationStatus status) {
        return new ApplicationStatusResponse(applicationId, status);
    }
}
