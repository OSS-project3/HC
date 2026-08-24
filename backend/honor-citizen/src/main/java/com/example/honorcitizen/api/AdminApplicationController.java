package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.application.dto.AdminApplicationMemberResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationStatusResponse;
import com.example.honorcitizen.domain.application.dto.DispatchRequest;
import com.example.honorcitizen.domain.application.dto.MyApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationListItemResponse;
import com.example.honorcitizen.domain.application.dto.NamingResultApplyResponse;
import com.example.honorcitizen.domain.application.dto.RejectPhotoRequest;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    // saju 프로그램이 돌려준 "사주이름 포함" 엑셀을 업로드해 구성원 한글이름을 반영한다.
    @PostMapping("/{applicationId}/naming-result")
    public ResponseEntity<ApiResponse<NamingResultApplyResponse>> applyNamingResult(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.applyNamingResult(adminId, applicationId, file)));
    }

    // 상태 전이 5종 — 전이 규칙 자체는 Application 엔티티에 있고, 여기·Service는 호출·인가·감사로그만 담당한다.
    @PostMapping("/{applicationId}/reject-photo")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> rejectPhoto(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody RejectPhotoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.rejectPhoto(adminId, applicationId, request.getReason())));
    }

    @PostMapping("/{applicationId}/start-producing")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> startProducing(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.startProducing(adminId, applicationId)));
    }

    @PostMapping("/{applicationId}/card-ready")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> markCardReady(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.markCardReady(adminId, applicationId)));
    }

    @PostMapping("/{applicationId}/dispatch")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> dispatchPhysical(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody DispatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.dispatchPhysical(adminId, applicationId, request.getTrackingNumber())));
    }

    @PostMapping("/{applicationId}/complete-naming")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> completeNaming(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.completeNaming(adminId, applicationId)));
    }
}
