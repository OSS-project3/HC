package com.example.honorcitizen.domain.school.service;

import com.example.honorcitizen.domain.school.dto.SchoolSearchResponse;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// 학생증 신청서(개인·단체 공통)의 학교 검색select용 — 신청 접수 단계에서 호출되므로 로그인 여부와
// 무관한 공개 API다(SecurityConfig permitAll). 등록된 학교가 없으면 프론트가 직접입력 폴백으로 넘어간다.
//
// [빈 검색어 = 전체 반환] 등록 학교 수가 적을 것으로 예상돼(페이지네이션 없음), 프론트가 최초 1회
// 빈 검색어로 전체 목록을 받아 SearchableSelectField로 클라이언트 필터링하는 방식을 쓴다. 나중에
// 학교 수가 크게 늘면 이 메서드 내부만 서버 검색/페이지네이션으로 바꾸면 되고, API 계약(쿼리 파라미터
// 이름·응답 형태)은 그대로 유지할 수 있도록 지금은 단순하게 둔다.
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<SchoolSearchResponse> search(String query) {
        List<School> schools = StringUtils.hasText(query)
                ? schoolRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim())
                : schoolRepository.findAllByOrderByNameAsc();
        return schools.stream().map(SchoolSearchResponse::from).toList();
    }
}
