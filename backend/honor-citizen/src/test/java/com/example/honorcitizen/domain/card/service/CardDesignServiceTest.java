package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
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

// 2-A/4-B: CardDesign 조회 API — 신규 카드 종류별 디자인 목록 + active 필터, 학생증은
// applicationId의 schoolId+orientation으로 자동 확정.
@SpringBootTest
class CardDesignServiceTest {

    @Autowired
    private CardDesignService cardDesignService;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long userId;
    private Long honorKoreanTypeId;
    private Long studentTypeId;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
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

    private int applicationSeq = 0;

    private Long studentApplication(Long schoolId, Orientation orientation) {
        Application application = Application.createIndividual(
                userId, "APP-2026-CD" + (++applicationSeq), studentTypeId, IssueType.MOBILE, true, null, null,
                orientation, SchoolType.UNIVERSITY, "테스트대학교", schoolId);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.SUBMITTED);
        return applicationRepository.save(application).getId();
    }

    @Test
    void listsAllDesignsForCardTypeWhenActiveFilterOmitted() {
        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, honorKoreanTypeId, null, null);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(CardDesignResponse::designNumber).containsExactly(1, 2, 3);
    }

    @Test
    void filtersToActiveOnlyWhenRequested() {
        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, honorKoreanTypeId, true, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CardDesignResponse::designNumber).containsExactly(1, 2);
    }

    @Test
    void filtersToInactiveOnlyWhenRequested() {
        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, honorKoreanTypeId, false, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).designNumber()).isEqualTo(3);
    }

    @Test
    void rejectsUnknownCardTypeId() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(adminId, 999999L, null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_TYPE_NOT_FOUND);
    }

    @Test
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(userId, honorKoreanTypeId, null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // --- 4-B: 학생증 schoolId+orientation 자동 확정 ---

    @Test
    void rejectsStudentCardTypeWithoutApplicationId() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(adminId, studentTypeId, null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsStudentCardTypeWithUnknownApplicationId() {
        assertThatThrownBy(() -> cardDesignService.listCardDesigns(adminId, studentTypeId, null, 999999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void returnsEmptyListWhenApplicationHasNoSchoolIdYet() {
        Long applicationId = studentApplication(null, Orientation.LANDSCAPE);

        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, studentTypeId, null, applicationId);

        assertThat(result).isEmpty();
    }

    @Test
    void resolvesStudentDesignBySchoolIdAndOrientation() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                studentTypeId, "테스트대학교-가로", 10, CardDesignOrientation.LANDSCAPE, null, null, true, 5L));
        // 다른 학교(schoolId=6)나 다른 방향(PORTRAIT)의 디자인은 매칭되면 안 된다.
        cardDesignRepository.save(CardDesign.create(
                studentTypeId, "다른학교-가로", 11, CardDesignOrientation.LANDSCAPE, null, null, true, 6L));
        cardDesignRepository.save(CardDesign.create(
                studentTypeId, "테스트대학교-세로", 12, CardDesignOrientation.PORTRAIT, null, null, true, 5L));
        Long applicationId = studentApplication(5L, Orientation.LANDSCAPE);

        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, studentTypeId, null, applicationId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(design.getId());
    }

    @Test
    void returnsEmptyListWhenNoDesignRegisteredForSchoolYet() {
        Long applicationId = studentApplication(999L, Orientation.LANDSCAPE);

        List<CardDesignResponse> result = cardDesignService.listCardDesigns(adminId, studentTypeId, null, applicationId);

        assertThat(result).isEmpty();
    }

    @Test
    void filtersStudentDesignsByActiveToo() {
        CardDesign inactiveDesign = CardDesign.create(
                studentTypeId, "테스트대학교-비활성", 13, CardDesignOrientation.LANDSCAPE, null, null, false, 5L);
        inactiveDesign.deactivate();
        cardDesignRepository.save(inactiveDesign);
        Long applicationId = studentApplication(5L, Orientation.LANDSCAPE);

        assertThat(cardDesignService.listCardDesigns(adminId, studentTypeId, true, applicationId)).isEmpty();
        assertThat(cardDesignService.listCardDesigns(adminId, studentTypeId, false, applicationId)).hasSize(1);
    }
}
