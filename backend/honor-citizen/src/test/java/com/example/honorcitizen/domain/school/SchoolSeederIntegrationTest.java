package com.example.honorcitizen.domain.school;

import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// QA 체크리스트 10번: SchoolSeederTest는 CSV 파싱 결과만 검사할 뿐, 실제 SchoolSeeder.run()을
// 호출해 DB 적재·기존 학교 보존·재실행 시 중복 없음까지 확인하는 통합 테스트가 없었다
// (HighSchoolSeederIntegrationTest에는 있음, 정책 결정 불필요 — 동일 패턴으로 테스트만 추가).
//
// HighSchoolSeederIntegrationTest처럼 "컨텍스트 기동 시 이미 시드된 상태"에 기대지 않는다 —
// `schools` 테이블은 이 스위트의 다른 여러 테스트 클래스가 각자 @BeforeEach에서
// schoolRepository.deleteAll()로 공유해서 쓰기 때문에(전체 스위트 실행 시 어느 시점에 이
// 테스트가 도는지에 따라 시드 데이터가 이미 지워져 있을 수 있음 — HighSchoolSeederIntegrationTest가
// 겪는 것과 동일한 플레이키 원인), 매 테스트가 스스로 초기화한 뒤 필요한 상태를 직접 만들어 전체
// 스위트 실행 순서와 무관하게 항상 결정적으로 통과하게 한다. SchoolSeeder는 UNIVERSITY만 다루므로
// HIGH_SCHOOL 행(HighSchoolSeederIntegrationTest 등 다른 테스트가 의존할 수 있음)은 건드리지
// 않도록 UNIVERSITY 타입만 골라 지우고, 개수 검증도 UNIVERSITY로만 필터링한다 —
// schoolRepository.deleteAll()/count()를 그대로 쓰면 이 테스트가 HighSchoolSeederIntegrationTest를
// 새로운 플레이키로 만들 위험이 있다.
@SpringBootTest
class SchoolSeederIntegrationTest {

    @Autowired
    private SchoolSeeder schoolSeeder;
    @Autowired
    private SchoolRepository schoolRepository;

    @BeforeEach
    void setUp() {
        schoolRepository.deleteAll(universities());
    }

    private List<School> universities() {
        return schoolRepository.findAll().stream()
                .filter(school -> school.getSchoolType() == SchoolType.UNIVERSITY)
                .toList();
    }

    @Test
    void reseedingTheSameCsvDoesNotDuplicateRows() {
        schoolSeeder.run();
        long universityCountAfterFirstRun = universities().size();
        assertThat(universityCountAfterFirstRun).isEqualTo(418); // SchoolSeederTest의 CSV 기준 개수와 동일해야 함
        assertThat(universities())
                .filteredOn(school -> "전북대학교".equals(school.getName()))
                .hasSize(1);

        schoolSeeder.run();

        assertThat(universities()).hasSize((int) universityCountAfterFirstRun);
        assertThat(universities())
                .filteredOn(school -> "전북대학교".equals(school.getName()))
                .hasSize(1);
    }

    @Test
    void preservesExistingSchoolAndFillsInOnlyTheMissingOnes() {
        // 중간 실패나 이관 과정을 흉내내 CSV에도 있는 학교 하나만 이미 등록돼 있는 상태를 만든다 —
        // 이 행은 시더가 절대 덮어쓰거나 중복시키면 안 된다.
        School existing = schoolRepository.save(School.create("전북대학교", SchoolType.UNIVERSITY));
        Long existingId = existing.getId();

        schoolSeeder.run();

        assertThat(universities())
                .filteredOn(school -> "전북대학교".equals(school.getName()))
                .hasSize(1)
                .extracting(School::getId)
                .containsExactly(existingId); // 기존 행 그대로 — 새로 만들지 않는다.
        // CSV 기준 전체 대학 수(SchoolSeederTest.bundledPublicDatasetProducesFourHundredEighteenUniqueSchools)와
        // 같아야 한다 — 미리 있던 1건은 그대로 세고, 나머지 누락분만 새로 채워졌다는 뜻이다.
        assertThat(universities()).hasSize(418);
    }
}
