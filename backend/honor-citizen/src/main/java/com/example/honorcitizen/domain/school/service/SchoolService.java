package com.example.honorcitizen.domain.school.service;

import com.example.honorcitizen.domain.school.dto.SchoolSearchResponse;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// 학생증 신청서(개인·단체 공통)의 학교 검색select용 — 신청 접수 단계에서 호출되므로 로그인 여부와
// 무관한 공개 API다(SecurityConfig permitAll). 등록된 학교가 없으면 프론트가 직접입력 폴백으로 넘어간다.
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public List<SchoolSearchResponse> search(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return schoolRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim()).stream()
                .map(SchoolSearchResponse::from)
                .toList();
    }
}
