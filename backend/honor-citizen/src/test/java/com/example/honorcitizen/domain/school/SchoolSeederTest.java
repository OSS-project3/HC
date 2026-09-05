package com.example.honorcitizen.domain.school;

import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.school.entity.School;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolSeederTest {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");

    @Test
    void parsesOnlyUniversitiesAndCollegesAndDeduplicatesSchoolNames() throws Exception {
        String csv = """
                학교명,학교 영문명,본분교구분명,대학구분명
                한세종대학교,"Hansejong University, Seoul",본교,대학
                한세종대학교,Hansejong University,제2캠퍼,대학
                한세종전문대학,Hansejong College,본교,전문대학
                한세종대학교 일반대학원,Hansejong Graduate School,본교,대학원
                """;

        List<School> schools = SchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName)
                .containsExactly("한세종대학교", "한세종전문대학");
        assertThat(schools).extracting(School::getSchoolType)
                .containsOnly(SchoolType.UNIVERSITY);
    }

    @Test
    void bundledPublicDatasetProducesFourHundredEighteenUniqueSchools() throws Exception {
        try (InputStream input = new ClassPathResource(SchoolSeeder.RESOURCE_PATH).getInputStream()) {
            List<School> schools = SchoolSeeder.parse(input);

            assertThat(schools).hasSize(418);
            assertThat(schools).extracting(School::getName)
                    .contains("전북대학교")
                    .doesNotContain("충남대학교 평화안보대학원")
                    .doesNotHaveDuplicates();
            assertThat(schools).allSatisfy(school -> {
                assertThat(school.getName()).isNotBlank();
                assertThat(school.getSchoolType()).isEqualTo(SchoolType.UNIVERSITY);
            });
        }
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(CSV_CHARSET));
    }
}
