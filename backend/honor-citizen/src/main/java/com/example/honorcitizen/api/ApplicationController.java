package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupResponse;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

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
            @RequestPart("seal") MultipartFile seal,
            @RequestPart("submitFile") MultipartFile submitFile) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        applicationService.createGroup(userId, request, logo, seal, submitFile)));
    }

    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<ApplicationLookupResponse>> lookup(
            @Valid @RequestBody ApplicationLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.lookup(request)));
    }
}
