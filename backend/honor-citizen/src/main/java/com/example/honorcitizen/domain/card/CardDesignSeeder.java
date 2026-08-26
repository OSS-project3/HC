package com.example.honorcitizen.domain.card;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

// 2-A: card-templates/{cardType}/{1..6}/ 클래스패스 리소스와 1:1 대응하는 DB 디자인 행을 시드한다.
// VISITOR/1은 CardImageCompositor.isUnverifiedDesign()이 렌더링을 거절하는 미검수 디자인이라
// active=false로 시드한다(2-B 검증 매트릭스 완료 전까지 신규 미리보기·생성에서 제외).
@Component
@Order(2) // CardTypeSeeder(@Order(1)) 뒤에 실행돼 카드종류가 존재하는 상태에서 디자인을 시드한다.
@RequiredArgsConstructor
public class CardDesignSeeder implements CommandLineRunner {

    private static final int DESIGN_COUNT_PER_CARD_TYPE = 6;

    private final CardDesignRepository cardDesignRepository;
    private final CardTypeRepository cardTypeRepository;

    @Override
    public void run(String... args) {
        if (cardDesignRepository.count() > 0) {
            return;
        }
        seedForCardType(CardTypeCode.HONOR_KOREAN, "명예한국인증", CardDesignOrientation.LANDSCAPE);
        seedForCardType(CardTypeCode.HONOR_CITIZEN, "명예시민증", CardDesignOrientation.LANDSCAPE);
        seedForCardType(CardTypeCode.VISITOR, "방문증", CardDesignOrientation.PORTRAIT);
    }

    private void seedForCardType(CardTypeCode code, String label, CardDesignOrientation orientation) {
        Long cardTypeId = cardTypeRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("CardType 시드 누락: " + code))
                .getId();
        for (int designNumber = 1; designNumber <= DESIGN_COUNT_PER_CARD_TYPE; designNumber++) {
            CardDesign design = CardDesign.create(cardTypeId, label + " 디자인" + designNumber, designNumber,
                    orientation, null, null, designNumber == 1);
            if (code == CardTypeCode.VISITOR && designNumber == 1) {
                design.deactivate();
            }
            cardDesignRepository.save(design);
        }
    }
}
