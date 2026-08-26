package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.dto.CardDesignResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 2-A: CardDesign 조회 API — 신규 카드 종류별 디자인 목록 + active 필터 + 학생증 거절.
@SpringBootTest
class CardDesignServiceTest {

    @Autowired
    private CardDesignService cardDesignService;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long userId;
    private Long honorKoreanTypeId;
    private Long studentTypeId;

    @BeforeEach
    void setUp() {
        cardDesignRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("carddesign-admin@example.com", "oauth-carddesign-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("carddesign-user@example.com", "oauth-carddesign-user", "google", "User"));
        userId = user.getId();

        CardType honorKorean = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-carddesign", null, BigDecimal.ZERO));
        honorKoreanTypeId = honorKorean.getId();
        CardType student = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-carddesign", null, BigDecimal.ZERO));
        studentTypeId = student.getId();

        cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인1", 1, CardDesignOrientation.LANDSCAPE, null, null, true));
        cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인2", 2, CardDesignOrientation.LANDSCAPE, null, null, false));
        CardDesign inactive = CardDesign.create(
                honorKoreanTypeId, "디자인3-미검수", 3, CardDesignOrientation.LANDSCAPE, null, null, false);
        inactive.deactivate();
        cardDesignRepository.save(inactive);
    }

    @Test
    void listsAllDesignsForCardTypeWhenActiveFilterOmitted() {
        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, honorKoreanTypeId, null);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(CardDesignResponse::designNumber).containsExactly(1, 2, 3);
    }

    @Test
    void filtersToActiveOnlyWhenRequested() {
        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, honorKoreanTypeId, true);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CardDesignResponse::designNumber).containsExactly(1, 2);
    }

    @Test
    void filtersToInactiveOnlyWhenRequested() {
        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, honorKoreanTypeId, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).designNumber()).isEqualTo(3);
    }

    @Test
    void rejectsStudentCardType() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(adminId, studentTypeId, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_CARD_TYPE);
    }

    @Test
    void rejectsUnknownCardTypeId() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(adminId, 999999L, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_TYPE_NOT_FOUND);
    }

    @Test
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(userId, honorKoreanTypeId, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
