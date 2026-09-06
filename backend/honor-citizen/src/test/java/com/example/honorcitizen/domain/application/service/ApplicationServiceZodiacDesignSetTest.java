package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 십이간지 캐릭터 디자인 세트 지정/변경(2026-09-06 신규) — 엔티티 자체의 검증(1~3, 잠금 없음)은
// ApplicationStateTransitionTest가 이미 커버하므로, 여기서는 Service가 관리자 인가·신청 조회를
// 정확히 수행하는지만 검증한다.
@SpringBootTest
class ApplicationServiceZodiacDesignSetTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long userId;
    private Long applicationId;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("zodiac-admin@example.com", "oauth-zodiac-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("zodiac-owner@example.com", "oauth-zodiac-owner", "google", "Owner"));
        userId = owner.getId();

        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-zodiac", null, BigDecimal.valueOf(30000)));
        Application application = applicationRepository.save(Application.createIndividual(
                userId, "APP-2026-ZODIAC01", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicationId = application.getId();
    }

    @Test
    void adminAssignsZodiacDesignSet() {
        applicationService.assignZodiacDesignSet(adminId, applicationId, 2);

        Application saved = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(saved.getZodiacDesignSet()).isEqualTo(2);
    }

    @Test
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> applicationService.assignZodiacDesignSet(userId, applicationId, 1))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsOutOfRangeValue() {
        assertThatThrownBy(() -> applicationService.assignZodiacDesignSet(adminId, applicationId, 0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> applicationService.assignZodiacDesignSet(adminId, applicationId, 4))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsUnknownApplication() {
        assertThatThrownBy(() -> applicationService.assignZodiacDesignSet(adminId, 999999L, 1))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void canChangeSelectionRepeatedly() {
        applicationService.assignZodiacDesignSet(adminId, applicationId, 1);
        applicationService.assignZodiacDesignSet(adminId, applicationId, 3);

        Application saved = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(saved.getZodiacDesignSet()).isEqualTo(3);
    }
}
