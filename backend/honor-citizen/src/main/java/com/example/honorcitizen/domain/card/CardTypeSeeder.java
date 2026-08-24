package com.example.honorcitizen.domain.card;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1) // DemoDataSeeder(@Order(2))가 카드종류를 참조하므로 반드시 먼저 실행되어야 한다.
@RequiredArgsConstructor
public class CardTypeSeeder implements CommandLineRunner {

    private final CardTypeRepository cardTypeRepository;

    @Override
    public void run(String... args) {
        if (cardTypeRepository.count() > 0) {
            return;
        }
        // 프론트가 cardTypeId를 1~4로 하드코딩하므로 이 순서(코드 순서)를 절대 바꾸면 안 됨. 가격은 관리자 설정 전까지 0으로 둠.
        cardTypeRepository.save(CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증", null, BigDecimal.ZERO));
        cardTypeRepository.save(CardType.create(CardTypeCode.HONOR_CITIZEN, "명예시민증", null, BigDecimal.ZERO));
        cardTypeRepository.save(CardType.create(CardTypeCode.VISITOR, "방문증", null, BigDecimal.ZERO));
        cardTypeRepository.save(CardType.create(CardTypeCode.STUDENT, "학생증", null, BigDecimal.ZERO));
    }
}
