package com.example.honorcitizen.domain.photo.dto;

import com.example.honorcitizen.domain.photo.entity.PhotoUpload;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PhotoUploadResponse {

    private final String photoId;
    private final String photoUrl;
    private final LocalDateTime expiresAt;

    private PhotoUploadResponse(String photoId, String photoUrl, LocalDateTime expiresAt) {
        this.photoId = photoId;
        this.photoUrl = photoUrl;
        this.expiresAt = expiresAt;
    }

    public static PhotoUploadResponse from(PhotoUpload photoUpload) {
        return new PhotoUploadResponse(
                photoUpload.getPhotoId(),
                photoUpload.getPhotoUrl(),
                photoUpload.getExpiresAt()
        );
    }
}
