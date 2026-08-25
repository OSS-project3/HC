package com.example.honorcitizen.domain.sajuname;

import com.example.honorcitizen.domain.sajuname.repository.SajuNameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 번들 리소스(resources/seed/saju-names.json)가 애플리케이션 기동 시(CommandLineRunner) 그대로
// DB에 시드되는지 확인한다. 파싱 로직 자체는 SajuNameSeederTest에서 이미 커버.
@SpringBootTest
class SajuNameSeederIntegrationTest {

    @Autowired
    private SajuNameRepository sajuNameRepository;

    @Test
    void seedsAllSevenHundredNamesOnStartup() {
        assertThat(sajuNameRepository.count()).isEqualTo(700);
    }

    @Test
    void seededNamesHaveNoBlankRequiredFields() {
        sajuNameRepository.findAll().forEach(name -> {
            assertThat(name.getName()).isNotBlank();
            assertThat(name.getHanja()).isNotBlank();
            assertThat(name.getJawon()).isNotBlank();
            assertThat(name.getEum()).isNotBlank();
        });
    }
}
