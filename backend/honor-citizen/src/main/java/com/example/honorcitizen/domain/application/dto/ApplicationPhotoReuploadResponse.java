package com.example.honorcitizen.domain.application.dto;

import com.example.honorcitizen.domain.application.entity.Application;
import lombok.Getter;

@Getter
public class ApplicationPhotoReuploadResponse {

    private final Long applicationId;
    private final String status;
    private final String photoUrl;

    private ApplicationPhotoReuploadResponse(Long applicationId, String status, String photoUrl) {
        this.applicationId = applicationId;
        this.status = status;
        this.photoUrl = photoUrl;
    }

    public static ApplicationPhotoReuploadResponse from(Application application) {
        return new ApplicationPhotoReuploadResponse(
                application.getId(),
                application.getStatus().name(),
                application.getPhotoPath());
    }
}
