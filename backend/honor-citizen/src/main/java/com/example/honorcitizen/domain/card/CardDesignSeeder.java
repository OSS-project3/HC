package com.example.honorcitizen.domain.card;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

// 2-A: card-templates/{cardType}/{1..6}/ 클래스패스 리소스와 1:1 대응하는 DB 디자인 행을 시드한다.
// VISITOR/1은 CardImageCompositor.isUnverifiedDesign()이 렌더링을 거절하는 미검수 디자인이라
// active=false로 시드한다(2-B 검증 매트릭스 완료 전까지 신규 미리보기·생성에서 제외).
//
// count()>0이면 전체를 건너뛰던 방식은 폐기한다(2026-09-20 확정, QA 체크리스트 9번) — 중간 실패로
// 일부 행만 있는 상태에서는 다음 기동에도 나머지가 영영 채워지지 않았기 때문이다. 이제는
// (cardTypeId, designNumber) 조합별로 존재 여부를 확인해 누락분만 추가하고, 이미 있는 행은 절대
// 덮어쓰지 않는다. designNumber 중복(데이터 손상) 방어는 별도 코드가 필요 없다 —
// card_designs 테이블에 이미 (card_type_id, design_number) UNIQUE 제약이 있어(CardDesign.java)
// 중복 INSERT 자체가 DB 수준에서 거절되고, 이 예외를 여기서 삼키지 않으므로 애플리케이션 기동이
// 그대로 실패한다(임의로 하나를 골라 넘어가지 않음).
@Component
@Order(2) // CardTypeSeeder(@Order(1)) 뒤에 실행돼 카드종류가 존재하는 상태에서 디자인을 시드한다.
@RequiredArgsConstructor
public class CardDesignSeeder implements CommandLineRunner {

    private static final int DESIGN_COUNT_PER_CARD_TYPE = 6;

    private final CardDesignRepository cardDesignRepository;
    private final CardTypeRepository cardTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedForCardType(CardTypeCode.HONOR_KOREAN, "명예한국인증", CardDesignOrientation.LANDSCAPE);
        seedForCardType(CardTypeCode.HONOR_CITIZEN, "명예시민증", CardDesignOrientation.LANDSCAPE);
        seedForCardType(CardTypeCode.VISITOR, "방문증", CardDesignOrientation.PORTRAIT);
    }

    private void seedForCardType(CardTypeCode code, String label, CardDesignOrientation orientation) {
        Long cardTypeId = cardTypeRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("CardType 시드 누락: " + code))
                .getId();

        List<CardDesign> existing = cardDesignRepository.findByCardTypeIdOrderByDesignNumber(cardTypeId);
        Set<Integer> existingDesignNumbers = new HashSet<>();
        for (CardDesign design : existing) {
            existingDesignNumbers.add(design.getDesignNumber());
        }

        for (int designNumber = 1; designNumber <= DESIGN_COUNT_PER_CARD_TYPE; designNumber++) {
            if (existingDesignNumbers.contains(designNumber)) {
                continue; // 이미 정상 시드된 행은 덮어쓰지 않는다.
            }
            CardDesign design = CardDesign.create(cardTypeId, label + " 디자인" + designNumber, designNumber,
                    orientation, null, null, designNumber == 1);
            if (code == CardTypeCode.VISITOR && designNumber == 1) {
                design.deactivate();
            }
            cardDesignRepository.save(design);
        }
    }
}
