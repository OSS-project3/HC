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

    @Test
    void searchReturnsEmptyListForBlankQuery() {
        assertThat(schoolService.search(" ")).isEmpty();
        assertThat(schoolService.search(null)).isEmpty();
    }

    @Test
    void searchReturnsEmptyListWhenNoMatch() {
        assertThat(schoolService.search("존재하지않는학교이름")).isEmpty();
    }
}
