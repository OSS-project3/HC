package com.example.honorcitizen.domain.photo.entity;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo_uploads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PhotoUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String photoId;

    @Column(nullable = false, length = 500)
    private String photoPath;

    @Column(length = 1000)
    private String photoUrl;

    @Column(name = "is_used", nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PhotoUpload create(Long userId, String photoId, String photoPath, String photoUrl, LocalDateTime expiresAt) {
        PhotoUpload photo = new PhotoUpload();
        photo.userId = userId;
        photo.photoId = photoId;
        photo.photoPath = photoPath;
        photo.photoUrl = photoUrl;
        photo.used = false;
        photo.expiresAt = expiresAt;
        return photo;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void validateOwnership(Long requestUserId) {
        if (!this.userId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.PHOTO_OWNER_MISMATCH);
        }
    }

    public void markUsed() {
        this.used = true;
    }
}
