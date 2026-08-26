package com.example.honorcitizen.domain.school.service;

import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.school.dto.SchoolSearchResponse;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchoolServiceTest {

    @Autowired
    private SchoolService schoolService;
    @Autowired
    private SchoolRepository schoolRepository;

    @BeforeEach
    void setUp() {
        schoolRepository.deleteAll();
        schoolRepository.save(School.create("전북대학교", SchoolType.UNIVERSITY));
        schoolRepository.save(School.create("전주대학교", SchoolType.UNIVERSITY));
        schoolRepository.save(School.create("전북대학교사범대학부설고등학교", SchoolType.HIGH_SCHOOL));
    }

    @Test
    void searchReturnsCaseInsensitivePartialMatchesOrderedByName() {
        List<SchoolSearchResponse> result = schoolService.search("전북대학교");

        assertThat(result).extracting(SchoolSearchResponse::name)
                .containsExactly("전북대학교", "전북대학교사범대학부설고등학교");
    }

    @Test
    void searchReturnsSchoolTypeForEachCandidate() {
        List<SchoolSearchResponse> result = schoolService.search("전주대학교");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).schoolType()).isEqualTo(SchoolType.UNIVERSITY);
    }

    // 검색어가 비어있으면 전체 학교를 이름순으로 반환한다 — 프론트가 최초 1회 전체 목록을 받아
    // SearchableSelectField로 클라이언트 필터링하는 방식을 쓰기 때문(등록 학교 수가 적을 것으로 예상).
    @Test
    void searchReturnsAllSchoolsOrderedByNameForBlankQuery() {
        assertThat(schoolService.search(" ")).extracting(SchoolSearchResponse::name)
                .containsExactly("전북대학교", "전북대학교사범대학부설고등학교", "전주대학교");
        assertThat(schoolService.search(null)).hasSize(3);
    }

    @Test
    void searchReturnsEmptyListWhenNoMatch() {
        assertThat(schoolService.search("존재하지않는학교이름")).isEmpty();
    }
}
