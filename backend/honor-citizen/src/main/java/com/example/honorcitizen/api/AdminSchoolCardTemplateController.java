package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.card.dto.SchoolCardTemplateResponse;
import com.example.honorcitizen.domain.card.service.SchoolCardTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 4-D: 관리자가 학교별 학생증 카드 템플릿(앞/뒤)을 배포 없이 등록·교체한다. 다른 3종 카드처럼
// classpath 리소스로 커밋·배포하지 않고 S3 기반으로 관리한다(TODO.md "4-D" 참고). 이 세션은 백엔드
// API 계약까지만 — 실제 관리자 업로드 화면(미리보기+파일변경 버튼)은 프론트 스코프.
@RestController
@RequestMapping("/api/admin/schools/{schoolId}/card-template")
@RequiredArgsConstructor
public class AdminSchoolCardTemplateController {

    private final SchoolCardTemplateService schoolCardTemplateService;

    // 등록된 게 없으면 에러가 아니라 data:null — 관리자 화면이 "미등록" 상태를 정상적으로 매번
    // 마주친다(신규 학교는 항상 이 상태로 시작).
    @GetMapping
    public ResponseEntity<ApiResponse<SchoolCardTemplateResponse>> get(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long schoolId,
            @RequestParam CardDesignOrientation orientation) {
        return ResponseEntity.ok(ApiResponse.success(schoolCardTemplateService.get(adminId, schoolId, orientation)));
    }

    // 앞/뒤를 한 번에 같이 받는다(한쪽만 교체하는 흐름은 없음). 성공 응답은 GET과 동일한 DTO —
    // 관리자 화면이 등록 직후 별도 재조회 없이 미리보기를 갱신할 수 있다.
    @PostMapping
    public ResponseEntity<ApiResponse<SchoolCardTemplateResponse>> upload(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long schoolId,
            @RequestParam CardDesignOrientation orientation,
            @RequestPart("front") MultipartFile front,
            @RequestPart("back") MultipartFile back) {
        return ResponseEntity.ok(ApiResponse.success(
                schoolCardTemplateService.upload(adminId, schoolId, orientation, front, back)));
    }
}
