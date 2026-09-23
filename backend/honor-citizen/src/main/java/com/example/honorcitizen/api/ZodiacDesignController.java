package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.card.dto.ZodiacDesignPreviewResponse;
import com.example.honorcitizen.domain.card.service.CardAssetPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/zodiac-designs")
@RequiredArgsConstructor
public class ZodiacDesignController {

    private final CardAssetPreviewService cardAssetPreviewService;

    @GetMapping("/{designSet}/preview")
    public ResponseEntity<ApiResponse<ZodiacDesignPreviewResponse>> preview(
            @AuthenticationPrincipal Long adminId,
            @PathVariable int designSet) {
        return ResponseEntity.ok(ApiResponse.success(
                cardAssetPreviewService.previewZodiacDesignSet(adminId, designSet)));
    }
}
