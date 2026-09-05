package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.stats.dto.AdminStatsResponse;
import com.example.honorcitizen.domain.stats.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 통계 대시보드 — SecurityConfig의 /api/admin/**가 ADMIN 역할만 통과시키므로 여기서는
// 별도로 권한을 재확인하지 않는다(InquiryAdminController와 동일 원칙).
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getStats()));
    }
}
