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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 마이페이지 문의 목록/상세(requirements.md §④ API 2·3) — 로그인 사용자 본인 문의만 노출한다.
// Accept-Language: en이면 title/content/answer를 영어로 번역한다(category·연락처는 그대로).
@RestController
@RequestMapping("/api/my/inquiries")
@RequiredArgsConstructor
public class MyInquiryController {

    private final InquiryService inquiryService;
    private final EnglishResponseTranslator englishResponseTranslator;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InquiryListItemResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(
                englishResponseTranslator.translateInquiries(inquiryService.listMine(userId), acceptLanguage)));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(englishResponseTranslator.translateInquiry(
                inquiryService.getMineDetail(userId, inquiryId), acceptLanguage)));
    }
}
