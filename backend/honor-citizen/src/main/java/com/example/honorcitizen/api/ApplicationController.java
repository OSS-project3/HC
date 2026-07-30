package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationListResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationPhotoReuploadResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationResponse;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import com.example.honorcitizen.domain.citizencard.dto.CitizenCardDownloadResponse;
import com.example.honorcitizen.domain.citizencard.service.CitizenCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CitizenCardService citizenCardService;

    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createSingle(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ApplicationCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(applicationService.createSingle(userId, request)));
    }

    @GetMapping("/my/applications")
    public ResponseEntity<ApiResponse<ApplicationListResponse>> getMyApplications(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getMyApplications(userId)));
    }

    @GetMapping("/my/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getMyApplicationDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getMyApplicationDetail(userId, applicationId)));
    }

    @PatchMapping("/my/applications/{applicationId}/photo")
    public ResponseEntity<ApiResponse<ApplicationPhotoReuploadResponse>> reuploadPhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId,
            @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.reuploadPhoto(userId, applicationId, photo)));
    }

    @GetMapping("/my/applications/{applicationId}/card/download")
    public ResponseEntity<ApiResponse<CitizenCardDownloadResponse>> getCardDownload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(citizenCardService.getDownloadUrl(userId, applicationId)));
    }
}
