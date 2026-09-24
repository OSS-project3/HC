package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.NamingProgress;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.application.dto.MyApplicationListItemResponse;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 작명 업무 진행중/완료/캔슬 조회(2026-09-24 정책 확정) — 개인은 status 그대로, 단체는 status와
// 무관하게 멤버 전원 카드 생성 완료(cardFrontPath/cardBackPath) 여부로 DONE을 판정한다. 상태 전이
// 로직(completeNaming/markCardReady 등)은 건드리지 않는 순수 조회 기능이라 그쪽은 검증하지 않는다.
@SpringBootTest
class ApplicationNamingProgressListTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long userId;
    private Long cardTypeId;
    private int seq = 0;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("naming-progress-admin@example.com", "oauth-naming-progress-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("naming-progress-user@example.com", "oauth-naming-progress-user", "google", "User"));
        userId = user.getId();

        cardTypeId = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-naming-progress", null, BigDecimal.ZERO)).getId();
    }

    private Application individual(ApplicationStatus status) {
        Application application = Application.createIndividual(
                userId, "APP-2026-NP" + (++seq), cardTypeId, IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(application, "status", status);
        return applicationRepository.save(application);
    }

    private Application group(ApplicationStatus status, int totalQuantity) {
        Application application = Application.createGroup(
                userId, "APP-2026-NP" + (++seq), cardTypeId, IssueType.MOBILE, true, totalQuantity, null, null, null);
        ReflectionTestUtils.setField(application, "status", status);
        return applicationRepository.save(application);
    }

    private ApplicationMember member(Long applicationId, boolean cardGenerated) {
        ApplicationMember member = ApplicationMember.createIndividual(
                applicationId, "Member", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg");
        if (cardGenerated) {
            setPrivateField(member, "cardFrontPath", "cards/front.png");
            setPrivateField(member, "cardBackPath", "cards/back.png");
        }
        return applicationMemberRepository.save(member);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = ApplicationMember.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Long> idsOf(PageResponse<MyApplicationListItemResponse> page) {
        return page.getContent().stream().map(MyApplicationListItemResponse::getApplicationId).toList();
    }

    // --- 개인 신청 ---

    @Test
    void individualCompletedIsDone() {
        Application app = individual(ApplicationStatus.COMPLETED);

        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);
        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);

        assertThat(idsOf(done)).contains(app.getId());
        assertThat(idsOf(inProgress)).doesNotContain(app.getId());
    }

    @Test
    void individualNonCompletedNonCancelledIsInProgress() {
        Application app = individual(ApplicationStatus.PRODUCING);

        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);
        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);

        assertThat(idsOf(inProgress)).contains(app.getId());
        assertThat(idsOf(done)).doesNotContain(app.getId());
    }

    @Test
    void individualCancelledIsCancelledOnly() {
        Application app = individual(ApplicationStatus.CANCELLED);

        var cancelled = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.CANCELLED, 0, 20);
        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);
        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);

        assertThat(idsOf(cancelled)).contains(app.getId());
        assertThat(idsOf(inProgress)).doesNotContain(app.getId());
        assertThat(idsOf(done)).doesNotContain(app.getId());
    }

    // --- 단체 신청 ---

    @Test
    void groupWithAllMembersCardGeneratedIsDoneEvenIfStatusNotCompleted() {
        Application app = group(ApplicationStatus.PRODUCING, 2);
        member(app.getId(), true);
        member(app.getId(), true);

        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);
        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);

        assertThat(idsOf(done)).contains(app.getId());
        assertThat(idsOf(inProgress)).doesNotContain(app.getId());
    }

    @Test
    void groupWithSomeMembersNotCardGeneratedIsInProgress() {
        Application app = group(ApplicationStatus.PRODUCING, 3);
        member(app.getId(), true);
        member(app.getId(), true);
        member(app.getId(), false);

        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);
        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);

        assertThat(idsOf(inProgress)).contains(app.getId());
        assertThat(idsOf(done)).doesNotContain(app.getId());
    }

    @Test
    void groupCancelledIsCancelledEvenIfAllMembersCardGenerated() {
        Application app = group(ApplicationStatus.CANCELLED, 2);
        member(app.getId(), true);
        member(app.getId(), true);

        var cancelled = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.CANCELLED, 0, 20);
        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);
        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);

        assertThat(idsOf(cancelled)).contains(app.getId());
        assertThat(idsOf(done)).doesNotContain(app.getId());
        assertThat(idsOf(inProgress)).doesNotContain(app.getId());
    }

    @Test
    void groupWithNoMembersYetIsInProgressNotDone() {
        Application app = group(ApplicationStatus.NAME_EDITING, 5);

        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);
        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);

        assertThat(idsOf(inProgress)).contains(app.getId());
        assertThat(idsOf(done)).doesNotContain(app.getId());
    }

    // --- completedMemberCount 진행률 필드 ---

    @Test
    void completedMemberCountReflectsCardGeneratedMembersForGroup() {
        Application app = group(ApplicationStatus.PRODUCING, 3);
        member(app.getId(), true);
        member(app.getId(), true);
        member(app.getId(), false);

        var page = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);
        var row = page.getContent().stream().filter(r -> r.getApplicationId().equals(app.getId())).findFirst().orElseThrow();

        assertThat(row.getCompletedMemberCount()).isEqualTo(2);
        assertThat(row.getTotalQuantity()).isEqualTo(3);
    }

    @Test
    void completedMemberCountIsNullForIndividual() {
        Application app = individual(ApplicationStatus.COMPLETED);

        var page = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);
        var row = page.getContent().stream().filter(r -> r.getApplicationId().equals(app.getId())).findFirst().orElseThrow();

        assertThat(row.getCompletedMemberCount()).isNull();
    }

    // --- 3분류 상호 배타성 ---

    @Test
    void threeBucketsPartitionAllApplicationsWithoutOverlap() {
        individual(ApplicationStatus.SUBMITTED);
        individual(ApplicationStatus.COMPLETED);
        individual(ApplicationStatus.CANCELLED);
        Application doneGroup = group(ApplicationStatus.PRODUCTION_READY, 2);
        member(doneGroup.getId(), true);
        member(doneGroup.getId(), true);
        Application inProgressGroup = group(ApplicationStatus.NAME_EDITING, 2);
        member(inProgressGroup.getId(), true);
        member(inProgressGroup.getId(), false);

        long total = applicationRepository.count();
        var inProgress = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.IN_PROGRESS, 0, 20);
        var done = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.DONE, 0, 20);
        var cancelled = applicationService.listApplicationsForAdmin(adminId, null, NamingProgress.CANCELLED, 0, 20);

        assertThat(inProgress.getTotalElements() + done.getTotalElements() + cancelled.getTotalElements())
                .isEqualTo(total);
    }
}
