package com.example.honorcitizen.domain.school;

import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 나이스 "학교기본정보" CSV(원본: 공공데이터포털, 2026-08-31 기준)에서 고등학교만 골라 학교
 * 마스터로 적재한다.
 *
 * <p>원본 CSV는 CP949이며 초·중·고·특수학교 등 전 학교급을 담고 있다 — 학교급 판별은 학교명에
 * "고등학교"가 들어있는지가 아니라 공식 컬럼 {@code 학교종류명 == "고등학교"}로만 한다(정책 결정,
 * 2026-09-05 — 일반고/특성화고/특목고/자율고 4개 구분이 전부 이 필터 하나로 커버됨을 실측 확인).
 * 개교예정/가칭 학교(행정표준코드가 비어있음)는 아직 운영 중이 아닌 것으로 보고 제외한다(폐교 여부
 * 컬럼 자체가 없는 데이터셋이라, 이게 "운영 중" 취지에 대응하는 유일한 신호다).</p>
 *
 * <p>같은 학교명이 전국에 2개 이상이면(동명이교) 사용자를 위해 학교명 앞에 지역명을 붙인다 —
 * 지역명은 {@link RegionAbbreviations}가 시도명을 짧게 바꾼 값이다. 이미 운영 DB에 있는 학교는
 * 보존하고, 행정표준코드가 아직 등록 안 된 학교만 추가한다(표시용 {@code name}은 지역명이 붙어
 * 있을 수 있어 idempotency 키로 못 쓴다 — 행정표준코드가 진짜 식별자다, {@link School#getAdminStandardCode()}).</p>
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class HighSchoolSeeder implements CommandLineRunner {

    static final String RESOURCE_PATH = "seed/high-schools.csv";
    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final int ADMIN_STANDARD_CODE_INDEX = 2;
    private static final int SCHOOL_NAME_INDEX = 3;
    private static final int SCHOOL_TYPE_INDEX = 5;
    private static final int SIDO_NAME_INDEX = 6;
    private static final String HIGH_SCHOOL_TYPE = "고등학교";

    private final SchoolRepository schoolRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<School> sourceSchools;
        try (InputStream input = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            sourceSchools = parse(input);
        } catch (IOException e) {
            throw new IllegalStateException("고등학교 마스터 시드 파일을 읽을 수 없습니다: " + RESOURCE_PATH, e);
        }

        Set<String> existingCodes = new HashSet<>();
        schoolRepository.findAll().forEach(school -> {
            if (school.getAdminStandardCode() != null) {
                existingCodes.add(school.getAdminStandardCode());
            }
        });

        List<School> missingSchools = sourceSchools.stream()
                .filter(school -> existingCodes.add(school.getAdminStandardCode()))
                .toList();
        if (!missingSchools.isEmpty()) {
            schoolRepository.saveAll(missingSchools);
        }
    }

    private record HighSchoolRow(String adminStandardCode, String name, String sidoName) {
    }

    static List<School> parse(InputStream csv) throws IOException {
        List<HighSchoolRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, CSV_CHARSET))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                List<String> columns = CsvLineParser.split(line);
                if (columns.size() <= SIDO_NAME_INDEX) {
                    continue;
                }
                if (!HIGH_SCHOOL_TYPE.equals(columns.get(SCHOOL_TYPE_INDEX).trim())) {
                    continue;
                }

                String adminStandardCode = columns.get(ADMIN_STANDARD_CODE_INDEX).trim();
                if (adminStandardCode.isEmpty()) {
                    // 행정표준코드가 없는 행은 아직 개교하지 않은(가칭/설립예정) 학교다 — 제외한다.
                    continue;
                }

                String name = columns.get(SCHOOL_NAME_INDEX).trim();
                if (name.isEmpty()) {
                    continue;
                }

                rows.add(new HighSchoolRow(adminStandardCode, name, columns.get(SIDO_NAME_INDEX).trim()));
            }
        }

        Map<String, Long> nameOccurrences = rows.stream()
                .collect(Collectors.groupingBy(HighSchoolRow::name, Collectors.counting()));

        return rows.stream()
                .map(row -> {
                    boolean hasSameNameElsewhere = nameOccurrences.get(row.name()) > 1;
                    String displayName = hasSameNameElsewhere
                            ? RegionAbbreviations.abbreviate(row.sidoName()) + " " + row.name()
                            : row.name();
                    return School.createWithCode(displayName, SchoolType.HIGH_SCHOOL, row.adminStandardCode());
                })
                .toList();
    }
}
