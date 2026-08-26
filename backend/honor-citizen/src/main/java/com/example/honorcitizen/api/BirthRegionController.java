package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.manseryeok.dto.BirthRegionCandidateResponse;
import com.example.honorcitizen.domain.manseryeok.service.ManseryeokService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 관리자 만세력 화면에서 출생지역(도시명)을 검색해 timezoneId 조회에 쓸 좌표 후보를 받는다(1-D).
// 특정 신청/구성원과 무관한 순수 조회라 AdminApplicationController와 별도 경로로 둔다.
@RestController
@RequestMapping("/api/admin/birth-region")
@RequiredArgsConstructor
public class BirthRegionController {

    private final ManseryeokService manseryeokService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BirthRegionCandidateResponse>>> search(
            @AuthenticationPrincipal Long adminId,
            @RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(manseryeokService.searchBirthRegion(adminId, query)));
    }
}
