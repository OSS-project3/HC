package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.application.dto.NameSelectionStatResponse;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 작명 부가 조회 — 이름별 선택 이력 카운트. 인가는 SecurityConfig의 /api/admin/**로 걸린다.
@RestController
@RequestMapping("/api/admin/name-selection-stats")
@RequiredArgsConstructor
public class AdminNamingController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NameSelectionStatResponse>>> stats(
            @AuthenticationPrincipal Long adminId) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getNameSelectionStats(adminId)));
    }
}
