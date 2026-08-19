package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryDetailResponse;
import com.example.honorcitizen.domain.inquiry.dto.InquiryListItemResponse;
import com.example.honorcitizen.domain.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 마이페이지 문의 목록/상세(requirements.md §④ API 2·3) — 로그인 사용자 본인 문의만 노출한다.
@RestController
@RequestMapping("/api/my/inquiries")
@RequiredArgsConstructor
public class MyInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryListItemResponse>>> list(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.listMine(userId)));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getMineDetail(userId, inquiryId)));
    }
}
