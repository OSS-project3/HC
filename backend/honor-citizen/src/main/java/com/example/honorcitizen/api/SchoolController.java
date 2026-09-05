package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.domain.school.dto.SchoolSearchResponse;
import com.example.honorcitizen.domain.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 학생증 신청서(개인·단체) 작성 화면의 학교 검색select — 신청자는 로그인 여부와 무관하므로
// 비로그인 공개 API다(SecurityConfig permitAll, /api/applications/lookup과 동일 성격).
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    // 서버 검색 autocomplete. query가 비어있으면 빈 목록을 반환한다(SchoolService.search 참고 —
    // 학교 수가 많아 전체 목록을 한 번에 내려주지 않는다). 결과는 최대 20건으로 제한된다.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SchoolSearchResponse>>> search(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(schoolService.search(query)));
    }
}
