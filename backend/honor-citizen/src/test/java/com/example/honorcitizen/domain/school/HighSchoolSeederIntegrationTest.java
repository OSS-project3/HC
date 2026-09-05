package com.example.honorcitizen.domain.school;

import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// HighSchoolSeeder(CommandLineRunner)는 컨텍스트 기동 시 이미 한 번 실행된 상태다 — 여기서는 그 위에
// run()을 한 번 더 수동으로 호출해 재시딩이 중복 row를 만들지 않는지만 확인한다(DB 반영 여부는
// Set<String> 필터가 실제 SchoolRepository 상태를 조회해야 검증되므로 parse() 단위 테스트로는
// 못 잡는다).
@SpringBootTest
class HighSchoolSeederIntegrationTest {

    @Autowired
    private HighSchoolSeeder highSchoolSeeder;
    @Autowired
    private SchoolRepository schoolRepository;

    @Test
    void reseedingTheSameCsvDoesNotDuplicateRows() {
        long countAfterStartupSeeding = schoolRepository.count();
        assertThat(countAfterStartupSeeding).isGreaterThan(0);

        highSchoolSeeder.run();

        assertThat(schoolRepository.count()).isEqualTo(countAfterStartupSeeding);
    }
}
