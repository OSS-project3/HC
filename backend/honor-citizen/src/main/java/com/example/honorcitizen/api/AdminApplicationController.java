package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.application.dto.AdminApplicationMemberResponse;
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

import java.util.List;

// 관리자 신청 목록/상세 — 소유자 무관 전체 조회. 인가는 SecurityConfig의 /api/admin/** → ADMIN
// 규칙으로 라우트 레벨에서 걸리고, ApplicationService.validateAdmin이 한 번 더 확인한다.
// 상태 전이(사진반려/카드발급/배송추적 등)와 통계는 이번 범위 밖 — 순수 조회 2개만 다룬다.
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MyApplicationListItemResponse>>> list(
            @AuthenticationPrincipal Long adminId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listApplicationsForAdmin(adminId, status, page, size)));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<MyApplicationDetailResponse>> detail(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getApplicationDetailForAdmin(adminId, applicationId)));
    }

    // 작명 화면용 구성원 목록(개인=1명, 단체=엑셀 행 N명) — 이름·출신국가·성별·생년월일 등.
    @GetMapping("/{applicationId}/members")
    public ResponseEntity<ApiResponse<List<AdminApplicationMemberResponse>>> members(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getApplicationMembersForAdmin(adminId, applicationId)));
    }
}
