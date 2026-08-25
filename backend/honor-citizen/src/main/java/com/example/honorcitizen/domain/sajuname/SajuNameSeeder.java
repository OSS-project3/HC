package com.example.honorcitizen.domain.sajuname;

import com.example.honorcitizen.domain.sajuname.entity.SajuName;
import com.example.honorcitizen.domain.sajuname.repository.SajuNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 사주 작명용 이름 사전(700개, `frontend/src/data/sajuNames.json`과 동일 내용을
 * `resources/seed/saju-names.json`으로 복사해둔 것)을 DB로 옮긴다(DATA-1, BACKEND_TODO.md).
 *
 * {@code CardTypeSeeder}와 동일한 패턴 — 데모 데이터가 아니라 실제 참조 데이터라서
 * {@code app.seed-demo-data} 플래그와 무관하게 무조건 실행하고, count()>0이면 건너뛴다(idempotent).
 * 프론트는 아직 이 데이터를 DB가 아니라 자체 번들(`sajuNames.json`)로 쓰고 있다 — 이번 범위는
 * DB 이관까지만이고, 프론트를 이 데이터로 전환하는 건 추천 API(API-1)를 만들 때 함께 처리한다.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class SajuNameSeeder implements CommandLineRunner {

    private static final String RESOURCE_PATH = "seed/saju-names.json";

    private final SajuNameRepository sajuNameRepository;

    @Override
    public void run(String... args) {
        if (sajuNameRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            sajuNameRepository.saveAll(parse(in));
        } catch (IOException e) {
            throw new IllegalStateException("이름 사전 시드 파일을 읽을 수 없습니다: " + RESOURCE_PATH, e);
        }
    }

    // jawon/eum 배열 길이가 이름 글자 수만큼 가변(외자 1글자~4글자)이라 콤마로 이어 저장한다.
    static List<SajuName> parse(InputStream json) {
        ObjectMapper mapper = new ObjectMapper();
        List<SajuNameJson> rows = mapper.readValue(json, new TypeReference<List<SajuNameJson>>() {
        });
        return rows.stream()
                .map(row -> SajuName.create(row.name(), row.hanja(), row.roman(), row.reading(),
                        row.meaning(), String.join(",", row.jawon()), String.join(",", row.eum())))
                .toList();
    }

    private record SajuNameJson(String name, String hanja, String roman,
            List<String> jawon, List<String> eum, String reading, String meaning) {
    }
}
