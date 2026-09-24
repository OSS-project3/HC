package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
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
        Application application = Application.createIndividual(
                owner.getId(), "APP-2026-950001", cardType.getId(), IssueType.MOBILE, true, null, null);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application = applicationRepository.save(application);
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

    // 2026-09-13: 관리자 화면 새로고침 후 카드 제작 진행 상태 복원 검증 후속 조치 — 확정 이름의
    // 훈음(nameMeaning)·의미(nameInterpretation)는 assignMemberName 저장 시 이미 채워지지만
    // getApplicationMembersForAdmin 응답에 없어 재조회로 확인할 수 없었다. 응답 매핑만 추가한
    // 것이라, 이미 저장된 값이 그대로 노출되는지만 검증한다.
    @Test
    void getApplicationMembersForAdminExposesNameMeaningAndInterpretation() {
        applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "길동", "吉童", "길할 길, 아이 동", "복을 비는 이름");

        var members = applicationService.getApplicationMembersForAdmin(adminId, applicationId);

        var response = members.stream().filter(m -> m.getMemberId().equals(memberId)).findFirst().orElseThrow();
        assertThat(response.getNameMeaning()).isEqualTo("길할 길, 아이 동");
        assertThat(response.getNameInterpretation()).isEqualTo("복을 비는 이름");
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
        Application otherApplicationDraft = Application.createIndividual(
                applicationRepository.findById(applicationId).orElseThrow().getUserId(),
                "APP-2026-950002", cardTypeRepository.findAll().get(0).getId(), IssueType.MOBILE, true, null, null);
        otherApplicationDraft.confirmPayment();
        otherApplicationDraft.startReview();
        otherApplicationDraft.approveToNaming();
        Long otherApplicationId = applicationRepository.save(otherApplicationDraft).getId();

        assertThatThrownBy(() -> applicationService.assignMemberName(
                adminId, otherApplicationId, memberId, "홍", "길동", null, "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    // 2026-09-24 정책 변경: 마지막 멤버 이름 확정과 동시에 PRODUCTION_READY로 자동 전이되면(관리자
    // 상태 전이 자동화), 그 직후 "다른 추천 이름으로 다시 고르고 싶다"는 정정이 막혀버린다
    // (PRODUCTION_READY→NAME_EDITING으로 되돌아가는 전이가 없어서). 그래서 카드가 아직 생성되기
    // 전이라면 PRODUCTION_READY 상태에서도 이름을 다시 고를 수 있어야 한다 — requireNamingEditable
    // (NAME_EDITING 전용)보다 완화된 requireMemberNameEditable로 이 케이스만 허용한다.
    @Test
    void allowsRenamingInProductionReadyWhenMemberCardNotYetGenerated() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        application.completeNaming();
        applicationRepository.save(application);

        applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "길동", "吉童", "뜻", null);

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("길동");
        Application reloadedApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(reloadedApplication.getStatus()).isEqualTo(ApplicationStatus.PRODUCTION_READY);
    }

    // 카드가 이미 생성된 뒤에는 이름만 바꾸면 이미지에 박힌 이름과 데이터가 어긋나므로(재생성 없이는),
    // PRODUCTION_READY라도 그 멤버의 카드가 이미 생성됐으면 여전히 거절한다.
    @Test
    void rejectsRenamingWhenMemberCardAlreadyGenerated() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        application.completeNaming();
        applicationRepository.save(application);
        ApplicationMember member = applicationMemberRepository.findById(memberId).orElseThrow();
        member.assignCardImages("cards/front.png", "cards/back.png", LocalDate.now());
        applicationMemberRepository.save(member);

        assertThatThrownBy(() -> applicationService.assignMemberName(
                adminId, applicationId, memberId, "홍", "길동", "吉童", "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getName()).isNull();
    }

    // REVIEWING·PRODUCING 등 NAME_EDITING/PRODUCTION_READY가 아닌 상태는 여전히 무조건 거절한다.
    @Test
    void rejectsWhenApplicationIsInUnrelatedStatus() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.REVIEWING);
        applicationRepository.save(application);

        assertThatThrownBy(() -> applicationService.assignMemberName(
                adminId, applicationId, memberId, "홍", "길동", "吉童", "뜻", null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);

        ApplicationMember reloaded = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(reloaded.getName()).isNull();
        assertThat(nameSelectionStatRepository.findByNameAndHanja("길동", "吉童")).isEmpty();
    }

    // 관리자 상태 전이 자동화(2026-09-24 확정) — 이 신청은 멤버가 1명뿐이라 이름 확정이 곧 마지막
    // 멤버 완료. "작명 완료 처리" 버튼을 따로 안 눌러도 자동으로 PRODUCTION_READY까지 전이된다.
    @Test
    void autoCompletesNamingWhenLastMemberNameConfirmed() {
        applicationService.assignMemberName(adminId, applicationId, memberId, "홍", "길동", "吉童", "뜻", "풀이");

        Application application = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PRODUCTION_READY);
    }

    // 단체 신청은 한 명만 확정해서는 자동 전이되지 않는다 — 아직 남은 멤버가 있으면 조용히
    // NAME_EDITING을 유지한다(에러도 없음, 이름 저장 자체는 성공해야 함).
    @Test
    void doesNotAutoCompleteNamingWhileOtherGroupMembersStillIncomplete() {
        Application group = Application.createGroup(
                applicationRepository.findById(applicationId).orElseThrow().getUserId(),
                "APP-2026-950010", cardTypeRepository.findAll().get(0).getId(), IssueType.MOBILE, true,
                2, null, null, null);
        group.confirmPayment();
        group.startReview();
        group.approveToNaming();
        group = applicationRepository.save(group);
        Long groupId = group.getId();
        ApplicationMember first = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                groupId, "First Member", LocalDate.of(1990, 1, 1), "US", null, null, Gender.MALE, null,
                "first@example.com", "010-1111-1111", "Seoul", null, null, "photos/first.jpg"));
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                groupId, "Second Member", LocalDate.of(1990, 1, 1), "US", null, null, Gender.MALE, null,
                "second@example.com", "010-2222-2222", "Seoul", null, null, "photos/second.jpg"));

        applicationService.assignMemberName(adminId, groupId, first.getId(), "홍", "길동", "吉童", "뜻", "풀이");

        Application reloaded = applicationRepository.findById(groupId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.NAME_EDITING);
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
