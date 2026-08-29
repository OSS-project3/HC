package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.card.dto.CardDesignResponse;
import com.example.honorcitizen.domain.card.service.CardDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 관리자가 신청의 카드 종류에 맞는 디자인을 조회한다(2-A). 특정 신청과 무관한 순수 카탈로그 조회라
// AdminApplicationController와 별도 경로로 둔다.
@RestController
@RequestMapping("/api/admin/card-designs")
@RequiredArgsConstructor
public class CardDesignController {

    private final CardDesignService cardDesignService;

    // applicationId는 학생증(cardTypeId가 STUDENT)일 때만 필수 — 그 신청의 schoolId+orientation으로
    // 디자인을 자동 확정하는 데 쓰인다(4-B). 비학생증 조회에는 영향 없음(무시된다).
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardDesignResponse>>> list(
            @AuthenticationPrincipal Long adminId,
            @RequestParam Long cardTypeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                cardDesignService.listCardDesigns(adminId, cardTypeId, active, applicationId)));
    }
}
