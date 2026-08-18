package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReviewEligibilityServiceTest {

    // applicationNumber는 DB 컬럼이 VARCHAR(20)라 System.nanoTime()을 그대로 쓰면 넘친다.
    private static final AtomicInteger APPLICATION_NUMBER_SEQ = new AtomicInteger(1);

    @Autowired
    private ReviewEligibilityService eligibilityService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    private User applicantUser;
    private User memberUser;
    private CardType cardType;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-eligibility", null, BigDecimal.valueOf(30000)));

        applicantUser = userRepository.save(
                User.createOAuthUser("applicant@example.com", "oauth-applicant", "google", "신청자"));
        memberUser = userRepository.save(
                User.createOAuthUser("member@example.com", "oauth-member", "google", "구성원"));
    }

    private Application completedApplication(Long submitterUserId, ApplicationType type) {
        Application application = applicationType(submitterUserId, type);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        return applicationRepository.save(application);
    }

    private Application applicationType(Long submitterUserId, ApplicationType type) {
        String applicationNumber = "APP-2026-" + String.format("%06d", APPLICATION_NUMBER_SEQ.getAndIncrement());
        if (type == ApplicationType.INDIVIDUAL) {
            return applicationRepository.save(Application.createIndividual(
                    submitterUserId, applicationNumber, cardType.getId(), IssueType.MOBILE, true, null, null));
        }
        return applicationRepository.save(Application.createGroup(
                submitterUserId, applicationNumber, cardType.getId(), IssueType.MOBILE, true, 1,
                null, null, null));
    }

    @Test
    void allowsCreateWhenApplicantEmailMatchesCompletedApplication() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));

        assertThatCode(() -> eligibilityService.validateForCreate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsCreateWhenGroupMemberEmailMatchesCompletedApplication() {
        Application application = completedApplication(999L, ApplicationType.GROUP);
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "담당자", "hr@example.com", "010-9999-9999", "OO기업", "인사팀"));
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "Member Name", LocalDate.of(1990, 1, 1), "US", null, null,
                Gender.MALE, null, "member@example.com", "010-1234-5678", "Seoul", null, null, null));

        assertThatCode(() -> eligibilityService.validateForCreate(
                memberUser.getId(), ApplicationType.GROUP, cardType.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsCreateWhenUserIsWithdrawn() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));
        applicantUser.withdraw();
        userRepository.save(applicantUser);

        assertThatThrownBy(() -> eligibilityService.validateForCreate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAWN);
    }

    @Test
    void allowsUpdateEvenWhenOriginalAuthorIsNowWithdrawn() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));
        Review review = reviewRepository.save(Review.create(applicantUser.getId(), "신청자", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));
        applicantUser.withdraw();
        userRepository.save(applicantUser);

        assertThatCode(() -> eligibilityService.validateForUpdate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId(), review.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenNoApplicationHistoryMatchesEmail() {
        assertThatThrownBy(() -> eligibilityService.validateForCreate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void rejectsWhenMatchingApplicationIsNotCompleted() {
        Application application = applicationType(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));

        assertThatThrownBy(() -> eligibilityService.validateForCreate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void rejectsWhenCombinationDoesNotMatchActualApplicationType() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));

        assertThatThrownBy(() -> eligibilityService.validateForCreate(
                applicantUser.getId(), ApplicationType.GROUP, cardType.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_ELIGIBLE);
    }

    @Test
    void rejectsCreateWhenReviewForSameCombinationAlreadyExists() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));
        reviewRepository.save(Review.create(applicantUser.getId(), "신청자", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        assertThatThrownBy(() -> eligibilityService.validateForCreate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void updateAllowsKeepingTheSameCombinationOnTheSameReview() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));
        Review review = reviewRepository.save(Review.create(applicantUser.getId(), "신청자", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        assertThatCode(() -> eligibilityService.validateForUpdate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId(), review.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void updateRejectsWhenAnotherReviewByTheSameUserAlreadyUsesTargetCombination() {
        Application application = completedApplication(999L, ApplicationType.INDIVIDUAL);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "신청자", "applicant@example.com", "010-1111-2222"));
        reviewRepository.save(Review.create(applicantUser.getId(), "신청자", "기존 후기",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));
        Review otherReview = reviewRepository.save(Review.create(applicantUser.getId(), "신청자", "다른 후기",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용2", null));
        // otherReview는 같은 조합을 이미 갖고 있는 상태에서 시작하므로, 실제로 검증할 시나리오는
        // "이 review가 아닌 다른 review가 그 조합을 쓰고 있을 때"이다 — otherReview 자신 기준으로 검증하면
        // 자기 자신은 제외되고 첫 번째 review와 충돌해야 한다.

        assertThatThrownBy(() -> eligibilityService.validateForUpdate(
                applicantUser.getId(), ApplicationType.INDIVIDUAL, cardType.getId(), otherReview.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_ALREADY_EXISTS);
    }
}
