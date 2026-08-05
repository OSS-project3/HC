package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

@Getter
public class ApplicationPhotoReuploadResponse {

    private final Long applicationId;
    private final ApplicationStatus status;

    private ApplicationPhotoReuploadResponse(Long applicationId, ApplicationStatus status) {
        this.applicationId = applicationId;
        this.status = status;
    }

    public static ApplicationPhotoReuploadResponse from(Application application) {
        return new ApplicationPhotoReuploadResponse(application.getId(), application.getStatus());
    }
}
