package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationDetailResponse;
import com.example.honorcitizen.domain.application.dto.MyApplicationListItemResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.entity.Receiver;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ApplicationServiceMyApplicationsTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ReceiverRepository receiverRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private CardType cardType;
    private final AtomicInteger applicationNumberSeq = new AtomicInteger();

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-my", null, BigDecimal.valueOf(30000)));
    }

    private Application saveApplication(Long userId, IssueType issueType) {
        Application application = applicationRepository.save(Application.createIndividual(
                userId, "APP-2026-" + String.format("%06d", applicationNumberSeq.incrementAndGet()),
                cardType.getId(), issueType, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "홍길동", "hong@example.com", "010-1111-2222"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        if (issueType == IssueType.MOBILE_AND_PHYSICAL) {
            receiverRepository.save(Receiver.create(application.getId(), "홍길동", "010-1111-2222",
                    "12345", "서울시", "101호", null, null, null));
        }
        return application;
    }

    @Test
    void listMyApplicationsReturnsOnlyOwnedApplicationsSortedByCreatedAtDesc() {
        saveApplication(OWNER_ID, IssueType.MOBILE);
        saveApplication(OWNER_ID, IssueType.MOBILE);
        saveApplication(OTHER_USER_ID, IssueType.MOBILE);

        PageResponse<MyApplicationListItemResponse> result =
                applicationService.listMyApplications(OWNER_ID, null, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(item -> assertThat(item.getCardTypeName()).isEqualTo(cardType.getName()));
    }

    @Test
    void listMyApplicationsFiltersByStatus() {
        Application submitted = saveApplication(OWNER_ID, IssueType.MOBILE);
        Application cancelled = saveApplication(OWNER_ID, IssueType.MOBILE);
        cancelled.cancelByUser(LocalDateTime.now());
        applicationRepository.save(cancelled);

        PageResponse<MyApplicationListItemResponse> result =
                applicationService.listMyApplications(OWNER_ID, ApplicationStatus.CANCELLED, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getApplicationId()).isEqualTo(cancelled.getId());
    }

    @Test
    void listMyApplicationsRejectsInvalidPaging() {
        assertThatThrownBy(() -> applicationService.listMyApplications(OWNER_ID, null, -1, 20))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> applicationService.listMyApplications(OWNER_ID, null, 0, 101))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> applicationService.listMyApplications(OWNER_ID, null, 0, 0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void getMyApplicationDetailReturnsReceiverOnlyForMobileAndPhysical() {
        Application application = saveApplication(OWNER_ID, IssueType.MOBILE_AND_PHYSICAL);

        MyApplicationDetailResponse detail = applicationService.getMyApplicationDetail(OWNER_ID, application.getId());

        assertThat(detail.getApplicationId()).isEqualTo(application.getId());
        assertThat(detail.getReceiver()).isNotNull();
        assertThat(detail.getMemberCount()).isEqualTo(1);
    }

    @Test
    void getMyApplicationDetailReturnsNullReceiverForMobileOnly() {
        Application application = saveApplication(OWNER_ID, IssueType.MOBILE);

        MyApplicationDetailResponse detail = applicationService.getMyApplicationDetail(OWNER_ID, application.getId());

        assertThat(detail.getReceiver()).isNull();
    }

    @Test
    void getMyApplicationDetailRejectsNonOwner() {
        Application application = saveApplication(OWNER_ID, IssueType.MOBILE);

        assertThatThrownBy(() -> applicationService.getMyApplicationDetail(OTHER_USER_ID, application.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void getMyApplicationDetailRejectsMissingApplication() {
        assertThatThrownBy(() -> applicationService.getMyApplicationDetail(OWNER_ID, 999_999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }
}
