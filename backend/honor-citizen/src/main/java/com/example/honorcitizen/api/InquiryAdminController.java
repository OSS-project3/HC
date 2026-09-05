package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryAnswerRequest;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryStatusUpdateRequest;
import com.example.honorcitizen.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 관리자 문의 관리: SecurityConfig의 라우트 검증 후 Service가 관리자 권한을 다시 확인한다.
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryListItemResponse>>> list(
            @AuthenticationPrincipal Long adminId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.listAdmin(adminId)));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> detail(
            @AuthenticationPrincipal Long adminId, @PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getAdminDetail(adminId, inquiryId)));
    }

    @PatchMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<Void>> answer(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        inquiryService.answer(adminId, inquiryId, request.getAnswer());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{inquiryId}/status")
    public ResponseEntity<ApiResponse<Void>> status(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryStatusUpdateRequest request) {
        inquiryService.changeStatus(adminId, inquiryId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
