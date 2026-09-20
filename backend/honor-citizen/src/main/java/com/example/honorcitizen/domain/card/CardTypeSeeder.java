package com.example.honorcitizen.domain.card;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(1) // DemoDataSeeder(@Order(2))가 카드종류를 참조하므로 반드시 먼저 실행되어야 한다.
@RequiredArgsConstructor
public class CardTypeSeeder implements CommandLineRunner {

    // 프론트가 cardTypeId를 1~4로 하드코딩하므로 이 순서(코드 순서)를 절대 바꾸면 안 됨 — 부분 시드
    // 복구 시에도 항상 이 순서대로 누락분만 채워야 새로 생기는 ID가 프론트 기대와 어긋나지 않는다.
    // count()>0이면 전체를 건너뛰던 방식은 폐기한다(2026-09-20 확정, QA 체크리스트 9번) — 중간 실패로
    // 일부 행만 있는 상태에서는 다음 기동에도 나머지가 영영 채워지지 않았기 때문이다. 이제는 코드별로
    // 존재 여부를 확인해 누락된 것만 추가하고, 이미 있는 행은 절대 덮어쓰지 않는다.
    private static final Map<CardTypeCode, String> SEED_ROWS = new LinkedHashMap<>();

    static {
        SEED_ROWS.put(CardTypeCode.HONOR_KOREAN, "명예한국인증");
        SEED_ROWS.put(CardTypeCode.HONOR_CITIZEN, "명예시민증");
        SEED_ROWS.put(CardTypeCode.VISITOR, "방문증");
        SEED_ROWS.put(CardTypeCode.STUDENT, "학생증");
    }

    private final CardTypeRepository cardTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 가격은 관리자 설정 전까지 0으로 둠.
        SEED_ROWS.forEach((code, name) -> {
            if (cardTypeRepository.findByCode(code).isEmpty()) {
                cardTypeRepository.save(CardType.create(code, name, null, BigDecimal.ZERO));
            }
        });
    }
}
