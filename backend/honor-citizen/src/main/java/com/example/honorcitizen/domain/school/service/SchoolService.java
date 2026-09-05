package com.example.honorcitizen.domain.school.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.school.dto.SchoolSearchResponse;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// 학생증 신청서(개인·단체 공통)의 학교 검색select용 — 신청 접수 단계에서 호출되므로 로그인 여부와
// 무관한 공개 API다(SecurityConfig permitAll). 등록된 학교가 없으면 프론트가 직접입력 폴백으로 넘어간다.
//
// [서버 검색, 2026-09-04 변경] 대학교(약 418) + 고등학교(약 2,400)를 합치면 "학교 수가 적다"는 예전
// 전제가 깨져 전체 목록을 한 번에 내려주지 않는다. 검색어가 없으면 빈 목록을 반환하고(autocomplete —
// 사용자가 입력을 시작해야 결과가 뜬다), 검색어가 있으면 이름 부분일치 결과를 최대 MAX_RESULTS개만
// 반환한다. 무한스크롤/Page<T> 기반 페이지네이션은 이번 범위가 아니라서 도입하지 않았다 — 응답은
// 여전히 List<SchoolSearchResponse>(기존 계약 그대로).
@Service
@RequiredArgsConstructor
public class SchoolService {

    private static final int MAX_RESULTS = 20;

    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<SchoolSearchResponse> search(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<School> schools = schoolRepository.searchByName(query.trim(), PageRequest.of(0, MAX_RESULTS));
        return schools.stream().map(SchoolSearchResponse::from).toList();
    }

    // 4-D: Card 모듈(SchoolCardTemplateService)이 School 존재 확인 + 이름 조회를 하려고 SchoolRepository를
    // 직접 주입하는 대신 이 메서드를 쓴다(arch.md 5.4 — 단순 존재 확인도 공개 서비스 또는 조회 Port를
    // 사용한다). School Entity 자체를 도메인 밖으로 노출하지 않고 필요한 값(이름)만 반환한다.
    @Transactional(readOnly = true)
    public String getSchoolNameOrThrow(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHOOL_NOT_FOUND))
                .getName();
    }
}
