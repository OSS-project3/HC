package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.NameSelectionStat;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.NameSelectionStatRepository;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 관리자 대시보드 인앱 작명 확정(assignMemberName) — 성씨 저장, 형식 검증, 선택 이력 집계.
@SpringBootTest
class ApplicationServiceNameAssignTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private NameSelectionStatRepository nameSelectionStatRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long applicationId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        nameSelectionStatRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("name-assign-admin@example.com", "oauth-name-assign-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("name-assign-owner@example.com", "oauth-name-assign-owner", "google", "Owner"));
        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-nameassign", null, BigDecimal.valueOf(30000)));
        Application application = applicationRepository.save(Application.createIndividual(
                owner.getId(), "APP-2026-950001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicationId = application.getId();

        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                applicationId, "Hong Gildong", LocalDate.of(1990, 5, 15), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        memberId = member.getId();
    }

    @Test
    void assignsSurnameNameHanjaAndMeaning() {
        applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "길동", "吉童", "길할 길, 아이 동", "복을 비는 이름");

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getSurname()).isEqualTo("홍");
        assertThat(reloaded.getName()).isEqualTo("길동");
        assertThat(reloaded.getChineseName()).isEqualTo("吉童");
        // nameMeaning=짧은 훈음(카드 뒷면 "한자뜻음" 위치), nameInterpretation=긴 풀이 문단("풀이" 위치).
        // assignMemberName의 reading 인자(짧은 훈음)가 nameMeaning으로, meaning 인자(긴 풀이)가
        // nameInterpretation으로 들어간다 — 예전엔 반대로 들어가는 버그가 있었다(실제 카드 렌더링으로 발견).
        assertThat(reloaded.getNameMeaning()).isEqualTo("길할 길, 아이 동");
        assertThat(reloaded.getNameInterpretation()).isEqualTo("복을 비는 이름");
    }

    @Test
    void assignsNameWithoutSurnameDuringNameEditing() {
        applicationService.assignMemberName(adminId, applicationId, memberId, null, "길동", null, "뜻", null);

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getSurname()).isNull();
        assertThat(reloaded.getName()).isEqualTo("길동");
    }

    @Test
    void incrementsNameSelectionStatOnEachAssignment() {
        applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "길동", "吉童", "뜻", null);
        applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "길동", "吉童", "뜻", null);

        NameSelectionStat stat = nameSelectionStatRepository.findByNameAndHanja("길동", "吉童").orElseThrow();
        assertThat(stat.getSelectedCount()).isEqualTo(2);
    }

    @Test
    void rejectsNameOutsideTwoToThreeKoreanCharacters() {
        assertThatThrownBy(() -> applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "가", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsSurnameOutsideOneToTwoKoreanCharacters() {
        assertThatThrownBy(() -> applicationService.assignMemberName(adminId, applicationId, memberId, "황보김", "길동", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsWhenMemberDoesNotBelongToApplication() {
        Application otherApplication = applicationRepository.save(Application.createIndividual(
                applicationRepository.findById(applicationId).orElseThrow().getUserId(),
                "APP-2026-950002", cardTypeRepository.findAll().get(0).getId(), IssueType.MOBILE, true, null, null));

        assertThatThrownBy(() -> applicationService.assignMemberName(
                adminId, otherApplication.getId(), memberId, "홍", "길동", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsForNonAdminCaller() {
        User user = userRepository.save(
                User.createOAuthUser("name-assign-plain@example.com", "oauth-name-assign-plain", "google", "User"));

        assertThatThrownBy(() -> applicationService.assignMemberName(
                user.getId(), applicationId, memberId, "홍", "길동", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
