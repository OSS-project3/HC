package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.application.dto.AdminApplicationMemberResponse;
import com.example.honorcitizen.domain.application.dto.ApplicationExportRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationStatusResponse;
import com.example.honorcitizen.domain.application.dto.CardNumberAssignRequest;
import com.example.honorcitizen.domain.application.dto.CardNumberBatchAssignRequest;
import com.example.honorcitizen.domain.application.dto.CardNumberBatchAssignResponse;
import com.example.honorcitizen.domain.application.dto.DispatchRequest;
import com.example.honorcitizen.domain.application.dto.MyApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationListItemResponse;
import com.example.honorcitizen.domain.application.dto.NameAssignRequest;
import com.example.honorcitizen.domain.application.dto.NamingResultApplyResponse;
import com.example.honorcitizen.domain.application.dto.RejectPhotoRequest;
import com.example.honorcitizen.domain.application.service.ApplicationService;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokActiveResultResponse;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokConfirmRequest;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokResolveRequest;
import com.example.honorcitizen.domain.manseryeok.dto.ManseryeokResolveResponse;
import com.example.honorcitizen.domain.manseryeok.service.ManseryeokService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final ManseryeokService manseryeokService;

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

    // 인앱 작명 확정 — 선택한 추천 이름을 구성원에 저장하고 선택 이력(+1)을 DB에 남긴다(프론트 localStorage 미사용).
    @PostMapping("/{applicationId}/members/{memberId}/name")
    public ResponseEntity<ApiResponse<Void>> assignName(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @PathVariable Long memberId,
            @Valid @RequestBody NameAssignRequest request) {
        applicationService.assignMemberName(adminId, applicationId, memberId,
                request.getSurname(), request.getName(), request.getHanja(), request.getReading(), request.getMeaning());
        return ResponseEntity.ok(ApiResponse.success());
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

    // 개인/단일 Member 카드번호 확정 — 관리자가 직접 입력한다(서버 채번 없음).
    @PutMapping("/{applicationId}/members/{memberId}/card-number")
    public ResponseEntity<ApiResponse<Void>> assignCardNumber(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @PathVariable Long memberId,
            @Valid @RequestBody CardNumberAssignRequest request) {
        applicationService.assignCardNumber(adminId, applicationId, memberId, request.getCardNumber());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 단체 신청 카드번호 일괄 확정 — 사진 번호 기준 매칭, all-or-nothing.
    @PutMapping("/{applicationId}/card-numbers")
    public ResponseEntity<ApiResponse<CardNumberBatchAssignResponse>> assignCardNumbersBatch(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody CardNumberBatchAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.assignCardNumbersBatch(adminId, applicationId, request)));
    }

    // 만세력 timezone/DST 판정 — 미리보기 전용, DB에 저장하지 않는다(1-D).
    @PostMapping("/{applicationId}/members/{memberId}/manseryeok/resolve")
    public ResponseEntity<ApiResponse<ManseryeokResolveResponse>> resolveManseryeokBirthTime(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @PathVariable Long memberId,
            @Valid @RequestBody ManseryeokResolveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                manseryeokService.resolveBirthTime(adminId, applicationId, memberId, request)));
    }

    // 만세력 확정 결과 저장 — 프론트가 계산한 사주 결과를 이력 보존 방식으로 저장한다(1-D).
    @PostMapping("/{applicationId}/members/{memberId}/manseryeok")
    public ResponseEntity<ApiResponse<Void>> confirmManseryeokResult(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @PathVariable Long memberId,
            @Valid @RequestBody ManseryeokConfirmRequest request) {
        manseryeokService.confirmManseryeokResult(adminId, applicationId, memberId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 현재 활성 만세력 결과 조회 — 카드 띠 이미지 등에서 확정 연주를 읽어올 때 쓴다.
    @GetMapping("/{applicationId}/members/{memberId}/manseryeok")
    public ResponseEntity<ApiResponse<ManseryeokActiveResultResponse>> getActiveManseryeokResult(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(
                manseryeokService.getActiveManseryeokResult(adminId, applicationId, memberId)));
    }

    // 상태 전이 — 전이 규칙 자체는 Application 엔티티에 있고, 여기·Service는 호출·인가·감사로그만 담당한다.
    // 앞단 3종(결제확인·검토시작·작명승인)은 신청 접수 직후 관리자가 진행하는 흐름이다.
    @PostMapping("/{applicationId}/confirm-payment")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> confirmPayment(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.confirmPaymentByAdmin(adminId, applicationId)));
    }

    @PostMapping("/{applicationId}/start-review")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> startReview(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.startReview(adminId, applicationId)));
    }

    @PostMapping("/{applicationId}/approve-naming")
    public ResponseEntity<ApiResponse<ApplicationStatusResponse>> approveNaming(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.approveToNaming(adminId, applicationId)));
    }

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

    // 신청 명단 엑셀 내보내기(DESIGN.md §2.4). GROUP은 원본 서식 보존을 위해 정확히 1건만 허용
    // (Service에서 검증) — 2건 이상 보내면 INVALID_INPUT.
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody ApplicationExportRequest request) {
        byte[] bytes = applicationService.exportExcel(adminId, request.getApplicationIds(), request.getType());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"applications-export.xlsx\"")
                .body(bytes);
    }
}
