package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationListResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationPhotoReuploadResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationSummaryResponse;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.log.entity.ApplicationStatusLog;
import com.example.honorcitizen.domain.log.repository.ApplicationStatusLogRepository;
import com.example.honorcitizen.domain.photo.service.PhotoUploadService;
import com.example.honorcitizen.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final List<ApplicationStatus> DUPLICATE_TARGET_STATUSES = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.REVIEWING,
            ApplicationStatus.PHOTO_REJECTED);

    private final ApplicationRepository applicationRepository;
    private final UserService userService;
    private final PhotoUploadService photoUploadService;
    private final ApplicationStatusLogRepository statusLogRepository;

    @Transactional
    public ApplicationResponse createSingle(Long userId, ApplicationCreateRequest request) {
        userService.validateTermsAgreed(userId);
        validateDuplicate(userId);

        String photoPath = photoUploadService.resolvePhotoPath(request.getPhotoId(), userId);

        Application application = Application.createSingle(
                userId,
                request.getNameEn(),
                request.getNationality(),
                request.getBirthDate(),
                request.getBirthTime(),
                request.getBirthRegion(),
                request.getGender(),
                request.getPhotoId(),
                photoPath);

        Application saved = applicationRepository.save(application);
        statusLogRepository.save(ApplicationStatusLog.create(
                saved.getId(), null, ApplicationStatus.PENDING, userId, "단건 신청 생성"));
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationListResponse getMyApplications(Long userId) {
        List<ApplicationSummaryResponse> applications = applicationRepository.findAllByUserId(userId).stream()
                .map(ApplicationSummaryResponse::from)
                .toList();
        return new ApplicationListResponse(applications);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getMyApplicationDetail(Long userId, Long applicationId) {
        Application application = findOwnedApplication(userId, applicationId);
        return ApplicationDetailResponse.from(application);
    }

    @Transactional
    public ApplicationPhotoReuploadResponse reuploadPhoto(Long userId, Long applicationId, MultipartFile photo) {
        Application application = findOwnedApplication(userId, applicationId);
        validatePhotoReuploadAllowed(application);
        validatePhotoFile(photo);

        String photoId = "photo_" + UUID.randomUUID();
        String photoPath = "photos/preview/" + photoId + "-" + sanitizeFilename(photo.getOriginalFilename());
        application.resubmitPhoto(photoPath, photoId);

        return ApplicationPhotoReuploadResponse.from(application);
    }

    private void validateDuplicate(Long userId) {
        if (applicationRepository.existsByUserIdAndStatusIn(userId, DUPLICATE_TARGET_STATUSES)) {
            throw new CustomException(ErrorCode.DUPLICATE_APPLICATION);
        }
    }

    private Application findOwnedApplication(Long userId, Long applicationId) {
        Application application = applicationRepository.findByIdWithUser(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private void validatePhotoReuploadAllowed(Application application) {
        if (application.getStatus() != ApplicationStatus.PHOTO_REJECTED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void validatePhotoFile(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo";
        }
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
