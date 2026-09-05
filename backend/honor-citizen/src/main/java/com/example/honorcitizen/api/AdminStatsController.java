package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.stats.dto.AdminStatsResponse;
import com.example.honorcitizen.domain.stats.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 통계 대시보드: SecurityConfig의 라우트 검증 후 Service가 관리자 권한을 다시 확인한다.
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatsResponse>> stats(
            @AuthenticationPrincipal Long adminId) {
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getStats(adminId)));
    }
}
