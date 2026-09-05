package com.example.honorcitizen.domain.school;

import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.school.entity.School;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class HighSchoolSeederTest {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    // parse()는 학교명(3)·학교종류명(5)·시도명(6)까지만 읽으므로 테스트 CSV는 7개 컬럼(인덱스 0~6)만
    // 채운다 — 실제 원본 25개 컬럼 중 앞부분과 순서·의미가 동일하다.
    private static final String HEADER = "시도교육청코드,시도교육청명,행정표준코드,학교명,영문학교명,학교종류명,시도명";

    @Test
    void uniqueNameKeepsOriginalNameWithoutRegionPrefix() throws Exception {
        String csv = HEADER + "\n"
                + "B10,서울특별시교육청,1000001,유일고등학교,Yuil High School,고등학교,서울특별시\n";

        List<School> schools = HighSchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName).containsExactly("유일고등학교");
        assertThat(schools).extracting(School::getSchoolType).containsExactly(SchoolType.HIGH_SCHOOL);
        assertThat(schools).extracting(School::getAdminStandardCode).containsExactly("1000001");
    }

    @Test
    void duplicateNameAcrossRegionsGetsRegionPrefix() throws Exception {
        String csv = HEADER + "\n"
                + "B10,서울특별시교육청,1000002,중복고등학교,Jungbok High,고등학교,서울특별시\n"
                + "C10,부산광역시교육청,2000002,중복고등학교,Jungbok High,고등학교,부산광역시\n";

        List<School> schools = HighSchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName)
                .containsExactlyInAnyOrder("서울 중복고등학교", "부산 중복고등학교");
    }

    @Test
    void excludesNonHighSchoolRows() throws Exception {
        String csv = HEADER + "\n"
                + "B10,서울특별시교육청,1000003,포함안됨중학교,Included Middle,중학교,서울특별시\n"
                + "B10,서울특별시교육청,1000004,포함됨고등학교,Included High,고등학교,서울특별시\n";

        List<School> schools = HighSchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName).containsExactly("포함됨고등학교");
    }

    // 정책 결정(2026-09-05): 행정표준코드가 비어있는 행(개교예정/가칭 학교)은 아직 운영 중이 아닌
    // 것으로 보고 제외한다.
    @Test
    void excludesRowsWithBlankAdminStandardCode() throws Exception {
        String csv = HEADER + "\n"
                + "B10,서울특별시교육청,,개교예정고등학교,Not Yet Open,고등학교,서울특별시\n"
                + "B10,서울특별시교육청,1000005,이미개교고등학교,Already Open,고등학교,서울특별시\n";

        List<School> schools = HighSchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName).containsExactly("이미개교고등학교");
    }

    // 정책 결정(2026-09-05): 이 CSV 데이터셋 전용 특이 케이스(전남광주통합특별시)만 flat 매핑,
    // 나머지 시도는 RegionAbbreviations의 정식 축약표를 그대로 쓴다.
    @Test
    void mapsGwangjuAndJeonnamSpecialCaseSidoNames() throws Exception {
        String csv = HEADER + "\n"
                + "B10,전남광주통합특별시교육청(광주),3000001,광특고등학교,Gwangteuk High,고등학교,전남광주통합특별시(광주)\n"
                + "B10,대전광역시교육청,3000002,광특고등학교,Gwangteuk High,고등학교,대전광역시\n"
                + "B10,전남광주통합특별시교육청(전남),3000003,전특고등학교,Jeonteuk High,고등학교,전남광주통합특별시(전남)\n"
                + "B10,경기도교육청,3000004,전특고등학교,Jeonteuk High,고등학교,경기도\n";

        List<School> schools = HighSchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName)
                .containsExactlyInAnyOrder("광주 광특고등학교", "대전 광특고등학교", "전남 전특고등학교", "경기 전특고등학교");
    }

    @Test
    void koreanSchoolNameSurvivesCp949RoundTrip() throws Exception {
        String csv = HEADER + "\n"
                + "B10,서울특별시교육청,1000006,한글학교명확인고등학교,Encoding Check High,고등학교,서울특별시\n";

        List<School> schools = HighSchoolSeeder.parse(toStream(csv));

        assertThat(schools).extracting(School::getName).containsExactly("한글학교명확인고등학교");
    }

    // 실제 번들 CSV(전국 2026-08-31 기준) 전체를 파싱해 사용자가 요청한 검증 수치를 확인한다
    // (SchoolSeederTest의 bundledPublicDatasetProducesFourHundredEighteenUniqueSchools와 동일 패턴).
    @Test
    void bundledDatasetProducesExpectedHighSchoolCounts() throws Exception {
        List<School> schools;
        try (InputStream input = new ClassPathResource(HighSchoolSeeder.RESOURCE_PATH).getInputStream()) {
            schools = HighSchoolSeeder.parse(input);
        }

        // CSV 전체(12,673행) 중 학교종류명==고등학교가 2,409행, 그중 행정표준코드가 빈 개교예정/가칭
        // 6건을 제외한 최종 등록 대상은 2,403행이다(2026-09-05 실측, TODO.md 참고).
        assertThat(schools).hasSize(2403);

        assertThat(schools).extracting(School::getAdminStandardCode).doesNotHaveDuplicates();

        Map<String, Long> displayNameCounts = schools.stream()
                .collect(Collectors.groupingBy(School::getName, Collectors.counting()));
        List<String> duplicatedDisplayNames = displayNameCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        assertThat(duplicatedDisplayNames).isEmpty();

        assertThat(schools).extracting(School::getSchoolType).containsOnly(SchoolType.HIGH_SCHOOL);
        // 전국 유일한 학교명은 지역명 접두어 없이 원본 그대로 저장된다.
        assertThat(schools).extracting(School::getName).contains("가락고등학교");
        assertThat(schools).extracting(School::getName).contains("서울 강동고등학교", "대구 강동고등학교", "울산 강동고등학교");
        assertThat(schools).extracting(School::getName).contains("광주 광주고등학교", "경기 광주고등학교");
        assertThat(schools).extracting(School::getName)
                .doesNotContain("(가칭)명지3고등학교", "검단3고등학교(설립예정)", "AIDT고등학교");
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(CSV_CHARSET));
    }
}
