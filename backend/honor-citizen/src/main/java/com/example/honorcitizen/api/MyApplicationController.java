package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationListItemResponse;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 마이페이지 신청 목록/상세(api.md API 6, 7) — 로그인 사용자 본인 신청만 노출한다.
@RestController
@RequestMapping("/api/my/applications")
@RequiredArgsConstructor
public class MyApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MyApplicationListItemResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listMyApplications(userId, status, page, size)));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<MyApplicationDetailResponse>> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getMyApplicationDetail(userId, applicationId)));
    }
}
