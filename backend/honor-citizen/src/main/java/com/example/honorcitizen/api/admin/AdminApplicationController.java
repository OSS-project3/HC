package com.example.honorcitizen.api.admin;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.citizencard.dto.CitizenCardIssueResponse;
import com.example.honorcitizen.domain.citizencard.service.CitizenCardService;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameRegisterRequest;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameRegisterResponse;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameUpdateRequest;
import com.example.honorcitizen.domain.koreanname.dto.KoreanNameUpdateResponse;
import com.example.honorcitizen.domain.koreanname.service.KoreanNameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {

    private final KoreanNameService koreanNameService;
    private final CitizenCardService citizenCardService;

    @PostMapping("/{applicationId}/korean-name")
    public ResponseEntity<ApiResponse<KoreanNameRegisterResponse>> registerKoreanName(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody KoreanNameRegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                koreanNameService.registerKoreanName(adminId, applicationId, request)));
    }

    @PatchMapping("/{applicationId}/korean-name")
    public ResponseEntity<ApiResponse<KoreanNameUpdateResponse>> updateKoreanName(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody KoreanNameUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                koreanNameService.updateKoreanName(adminId, applicationId, request)));
    }

    @PostMapping("/{applicationId}/issue-card")
    public ResponseEntity<ApiResponse<CitizenCardIssueResponse>> issueCard(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                citizenCardService.issueCard(adminId, applicationId)));
    }
}
