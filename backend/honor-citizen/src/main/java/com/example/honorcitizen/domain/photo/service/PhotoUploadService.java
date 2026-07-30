package com.example.honorcitizen.domain.photo.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.photo.dto.PhotoUploadResponse;
import com.example.honorcitizen.domain.photo.entity.PhotoUpload;
import com.example.honorcitizen.domain.photo.repository.PhotoUploadRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final PhotoUploadRepository photoUploadRepository;
    private final StorageService storageService;

    @Transactional
    public PhotoUploadResponse upload(Long userId, MultipartFile file) {
        validateFile(file);

        String photoId = "photo_" + UUID.randomUUID().toString().replace("-", "");
        String extension = extractExtension(file.getOriginalFilename());
        String photoPath = "photos/uploads/" + photoId + "." + extension;

        String photoUrl = storageService.upload(photoPath, file);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        PhotoUpload photoUpload = PhotoUpload.create(userId, photoId, photoPath, photoUrl, expiresAt);
        photoUploadRepository.save(photoUpload);

        return PhotoUploadResponse.from(photoUpload);
    }

    @Transactional
    public String resolvePhotoPath(String photoId, Long userId) {
        PhotoUpload photoUpload = photoUploadRepository.findByPhotoId(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        photoUpload.validateOwnership(userId);

        if (photoUpload.isExpired()) {
            throw new CustomException(ErrorCode.PHOTO_EXPIRED);
        }

        photoUpload.markUsed();
        return photoUpload.getPhotoPath();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
