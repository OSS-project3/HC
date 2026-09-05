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
import java.util.stream.IntStream;

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
        // 검색어를 포함하지만 그걸로 시작하지는 않는 케이스 — 관련도 우선순위(정확히 일치 > 시작 >
        // 포함) 검증용. 이름만 보면 "인문..."이 "전북..."보다 가나다순으로 앞서므로, 단순 이름순
        // 정렬이었다면 이 학교가 먼저 나왔을 것이다.
        schoolRepository.save(School.create("인문전북대학교사회과학고등학교", SchoolType.HIGH_SCHOOL));
    }

    @Test
    void searchOrdersExactMatchThenPrefixThenContains() {
        List<SchoolSearchResponse> result = schoolService.search("전북대학교");

        assertThat(result).extracting(SchoolSearchResponse::name)
                .containsExactly("전북대학교", "전북대학교사범대학부설고등학교", "인문전북대학교사회과학고등학교");
    }

    @Test
    void searchReturnsSchoolTypeForEachCandidate() {
        List<SchoolSearchResponse> result = schoolService.search("전주대학교");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).schoolType()).isEqualTo(SchoolType.UNIVERSITY);
    }

    // [2026-09-04 변경] 대학교+고등학교를 합치면 학교 수가 약 2,800개까지 늘어나 "검색어 없음 = 전체
    // 반환"을 더 이상 쓰지 않는다 — autocomplete이므로 검색어가 없으면 빈 목록을 반환한다.
    @Test
    void searchReturnsEmptyListForBlankOrNullQuery() {
        assertThat(schoolService.search(" ")).isEmpty();
        assertThat(schoolService.search(null)).isEmpty();
    }

    @Test
    void searchReturnsEmptyListWhenNoMatch() {
        assertThat(schoolService.search("존재하지않는학교이름")).isEmpty();
    }

    // 결과 개수가 MAX_RESULTS(20)를 넘는 경우 상한이 걸리는지 확인 — 이 숫자는 SchoolService의
    // private 상수와 값이 같아야 한다(상수 자체를 바꾸면 이 테스트도 같이 바꿀 것).
    @Test
    void searchCapsResultsAtMaxResults() {
        IntStream.rangeClosed(1, 25).forEach(i ->
                schoolRepository.save(School.create(String.format("한도테스트고등학교%02d", i), SchoolType.HIGH_SCHOOL)));

        assertThat(schoolService.search("한도테스트고등학교")).hasSize(20);
    }
}
