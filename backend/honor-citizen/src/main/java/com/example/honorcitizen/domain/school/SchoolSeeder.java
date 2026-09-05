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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 공공데이터포털의 전국대학및전문대학정보 CSV에서 학부 과정 학교를 학교 마스터로 적재한다.
 *
 * <p>원본 CSV는 CP949이며 대학원 행도 포함한다. 사용자 학교 검색에는 {@code 대학}과
 * {@code 전문대학}만 필요하므로 두 대학구분만 선택하고, 캠퍼스 때문에 같은 학교명이 반복되는
 * 경우에는 학교명 하나로 합친다. 이미 운영 DB에 등록된 학교는 보존하고 누락된 학교만 추가한다.</p>
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class SchoolSeeder implements CommandLineRunner {

    static final String RESOURCE_PATH = "seed/universities.csv";
    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final int SCHOOL_NAME_INDEX = 0;
    private static final int UNIVERSITY_CATEGORY_INDEX = 3;

    private final SchoolRepository schoolRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<School> sourceSchools;
        try (InputStream input = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            sourceSchools = parse(input);
        } catch (IOException e) {
            throw new IllegalStateException("대학교 마스터 시드 파일을 읽을 수 없습니다: " + RESOURCE_PATH, e);
        }

        Set<String> existingNames = new HashSet<>();
        schoolRepository.findAll().forEach(school -> existingNames.add(school.getName()));

        List<School> missingSchools = sourceSchools.stream()
                .filter(school -> existingNames.add(school.getName()))
                .toList();
        if (!missingSchools.isEmpty()) {
            schoolRepository.saveAll(missingSchools);
        }
    }

    static List<School> parse(InputStream csv) throws IOException {
        Set<String> schoolNames = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, CSV_CHARSET))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                List<String> columns = CsvLineParser.split(line);
                if (columns.size() <= UNIVERSITY_CATEGORY_INDEX) {
                    continue;
                }

                String category = columns.get(UNIVERSITY_CATEGORY_INDEX).trim();
                if (!category.equals("대학") && !category.equals("전문대학")) {
                    continue;
                }

                String schoolName = columns.get(SCHOOL_NAME_INDEX).trim();
                if (!schoolName.isEmpty()) {
                    schoolNames.add(schoolName);
                }
            }
        }
        return schoolNames.stream()
                .map(name -> School.create(name, SchoolType.UNIVERSITY))
                .toList();
    }
}
