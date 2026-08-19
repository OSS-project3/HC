package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryAnswerRequest;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 관리자 문의 목록/상세(requirements.md §④ API 4·5) — SecurityConfig의 /api/admin/**가 ADMIN 역할만
// 통과시키므로 여기서는 별도로 권한을 재확인하지 않는다(Board의 관리자 컨트롤러와 동일 원칙).
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryListItemResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.listAdmin()));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> detail(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getAdminDetail(inquiryId)));
    }

    @PatchMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<Void>> answer(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request) {
        inquiryService.answer(inquiryId, request.getAnswer());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
