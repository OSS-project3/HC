package com.example.honorcitizen.domain.card;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// QA 체크리스트 9번: CardTypeSeeder/CardDesignSeeder가 count()>0이면 전체를 건너뛰던 방식이라,
// 중간 실패로 일부 행만 있는 상태에서는 다음 기동에도 나머지가 영영 채워지지 않았다(확정 정책,
// 2026-09-20). 매 테스트 전에 완전히 초기화한 뒤 필요한 상태를 직접 만들어 다른 테스트와
// 독립적으로 검증한다. "designNumber 중복 시 기동 실패" 요구사항은 card_designs 테이블에 이미
// (card_type_id, design_number) UNIQUE 제약이 걸려 있어(CardDesign.java) 중복 INSERT 자체가
// DataIntegrityViolationException으로 막힌다 — 시더가 스스로 막지 않아도 이미 DB 스키마 수준에서
// 보장되므로, ORM을 우회하지 않는 한 재현 불가능한 시나리오라 별도 테스트를 두지 않는다.
@SpringBootTest
class CardTypeCardDesignSeederAtomicityTest {

    @Autowired
    private CardTypeSeeder cardTypeSeeder;
    @Autowired
    private CardDesignSeeder cardDesignSeeder;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private CardDesignRepository cardDesignRepository;

    @BeforeEach
    void setUp() {
        cardDesignRepository.deleteAll();
        cardTypeRepository.deleteAll();
    }

    @Test
    void reseedingWhenFullyPopulatedDoesNotDuplicateOrChangeIds() {
        cardTypeSeeder.run();
        cardDesignSeeder.run();
        assertThat(cardTypeRepository.count()).isEqualTo(4);
        assertThat(cardDesignRepository.count()).isEqualTo(18);
        Long honorKoreanId = cardTypeRepository.findByCode(CardTypeCode.HONOR_KOREAN).orElseThrow().getId();

        cardTypeSeeder.run();
        cardDesignSeeder.run();

        assertThat(cardTypeRepository.count()).isEqualTo(4);
        assertThat(cardDesignRepository.count()).isEqualTo(18);
        assertThat(cardTypeRepository.findByCode(CardTypeCode.HONOR_KOREAN).orElseThrow().getId())
                .isEqualTo(honorKoreanId);
    }

    @Test
    void recoversMissingCardTypesWithoutTouchingExistingOnes() {
        // 중간 실패를 흉내내 앞 2개(HONOR_KOREAN, HONOR_CITIZEN)만 이미 존재하는 상태를 만든다.
        CardType existingHonorKorean = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증", null, BigDecimal.ZERO));
        Long existingId = existingHonorKorean.getId();
        cardTypeRepository.save(CardType.create(CardTypeCode.HONOR_CITIZEN, "명예시민증", null, BigDecimal.ZERO));

        cardTypeSeeder.run();

        assertThat(cardTypeRepository.count()).isEqualTo(4);
        assertThat(cardTypeRepository.findByCode(CardTypeCode.HONOR_KOREAN).orElseThrow().getId())
                .isEqualTo(existingId); // 기존 행은 그대로 — 새로 만들지 않는다.
        assertThat(cardTypeRepository.findByCode(CardTypeCode.VISITOR)).isPresent();
        assertThat(cardTypeRepository.findByCode(CardTypeCode.STUDENT)).isPresent();
    }

    @Test
    void recoversMissingCardDesignsForOneCardTypeWithoutTouchingOtherCardTypes() {
        cardTypeSeeder.run();
        cardDesignSeeder.run();
        Long honorKoreanId = cardTypeRepository.findByCode(CardTypeCode.HONOR_KOREAN).orElseThrow().getId();
        Long honorCitizenId = cardTypeRepository.findByCode(CardTypeCode.HONOR_CITIZEN).orElseThrow().getId();

        // HONOR_KOREAN의 디자인만 중간 실패를 흉내내 일부(1~3번)만 남기고 지운다.
        List<CardDesign> honorKoreanDesigns = cardDesignRepository.findByCardTypeIdOrderByDesignNumber(honorKoreanId);
        for (CardDesign design : honorKoreanDesigns) {
            if (design.getDesignNumber() > 3) {
                cardDesignRepository.delete(design);
            }
        }
        Long survivingDesignId = cardDesignRepository.findByCardTypeIdOrderByDesignNumber(honorKoreanId).get(0).getId();
        long honorCitizenDesignCountBefore = cardDesignRepository.findByCardTypeIdOrderByDesignNumber(honorCitizenId).size();

        cardDesignSeeder.run();

        assertThat(cardDesignRepository.findByCardTypeIdOrderByDesignNumber(honorKoreanId)).hasSize(6);
        assertThat(cardDesignRepository.findByCardTypeIdOrderByDesignNumber(honorKoreanId).get(0).getId())
                .isEqualTo(survivingDesignId); // 살아남은 기존 행은 그대로 — 새로 만들지 않는다.
        // 손대지 않은 다른 카드종류(HONOR_CITIZEN)는 영향받지 않는다.
        assertThat(cardDesignRepository.findByCardTypeIdOrderByDesignNumber(honorCitizenId))
                .hasSize((int) honorCitizenDesignCountBefore);
    }
}
