package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCardDownloadResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCancelResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupRequest;
import com.example.honorcitizen.domain.application.dto.DepositorNameUpdateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationPhotoReuploadResponse;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.application.service.ApplicationService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final EnglishResponseTranslator englishResponseTranslator;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationCreateResponse>> createIndividual(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") ApplicationCreateRequest request,
            @RequestPart("photo") MultipartFile photo,
            @RequestPart(value = "schoolLogo", required = false) MultipartFile schoolLogo,
            @RequestPart(value = "schoolSeal", required = false) MultipartFile schoolSeal) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        applicationService.createIndividual(userId, request, photo, schoolLogo, schoolSeal)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkApplicationCreateResponse>> createGroup(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") BulkApplicationCreateRequest request,
            @RequestPart("logo") MultipartFile logo,
            @RequestPart(value = "seal", required = false) MultipartFile seal,
            @RequestPart("submitFile") MultipartFile submitFile) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        applicationService.createGroup(userId, request, logo, seal, submitFile)));
    }

    // Accept-Language: en이면 photoRejectReason(자유 텍스트)만 영어로 번역한다(cardType·status는 그대로).
    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<ApplicationLookupResponse>> lookup(
            @Valid @RequestBody ApplicationLookupRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(
                englishResponseTranslator.translateLookup(applicationService.lookup(request), acceptLanguage)));
    }

    @PostMapping("/{applicationId}/cancel")
    public ResponseEntity<ApiResponse<ApplicationCancelResponse>> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.cancelByUser(userId, applicationId)));
    }

    // 입금자명 등록/수정 — 완료 화면에서 신청자 본인이 호출. 결제 확인 전(SUBMITTED·WAITING)에만 허용.
    @PatchMapping("/{applicationId}/depositor")
    public ResponseEntity<ApiResponse<Void>> updateDepositorName(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId,
            @Valid @RequestBody DepositorNameUpdateRequest request) {
        applicationService.updateDepositorName(userId, applicationId, request.getDepositorName());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{applicationId}/photo")
    public ResponseEntity<ApiResponse<ApplicationPhotoReuploadResponse>> reuploadPhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "submitFile", required = false) MultipartFile submitFile) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.reuploadPhoto(userId, applicationId, photo, submitFile)));
    }

    @GetMapping("/{applicationId}/cards/download")
    public ResponseEntity<ApiResponse<ApplicationCardDownloadResponse>> getCardDownload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getCardDownload(userId, applicationId)));
    }
}
