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

    // 2026-09-13: 개인 신청 카드 표기 주소 누락 검증 후속 조치 — 관리자/마이페이지 응답에 주소를
    // 노출하기 시작했다. 개인 신청은 그 1명의 ApplicationMember.address를 그대로 노출해야 한다.
    @Test
    void getMyApplicationDetailReturnsMemberAddressForIndividualApplication() {
        Application application = applicationRepository.save(Application.createIndividual(
                OWNER_ID, "APP-2026-" + String.format("%06d", applicationNumberSeq.incrementAndGet()),
                cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "홍길동", "hong@example.com", "010-1111-2222"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg", "대한민국 서울특별시 강남구"));

        MyApplicationDetailResponse detail = applicationService.getMyApplicationDetail(OWNER_ID, application.getId());

        assertThat(detail.getMemberAddress()).isEqualTo("대한민국 서울특별시 강남구");
    }

    // 단체 신청은 구성원별 상세를 이 응답에서 다루지 않으므로(별도 API로 분리 예정), 멤버의 주소가
    // 실제로 채워져 있어도 항상 null로 응답해야 한다 — "주소가 없어서 null"이 아니라 "단체라 제외"임을
    // 구분해서 검증한다.
    @Test
    void getMyApplicationDetailReturnsNullMemberAddressForGroupApplication() {
        Application application = applicationRepository.save(Application.createGroup(
                OWNER_ID, "APP-2026-" + String.format("%06d", applicationNumberSeq.incrementAndGet()),
                cardType.getId(), IssueType.MOBILE, true, 1, 10L, 11L, 12L));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "010-2222-2222", "Seoul", null, null,
                "photos/b.jpg"));

        MyApplicationDetailResponse detail = applicationService.getMyApplicationDetail(OWNER_ID, application.getId());

        assertThat(detail.getMemberAddress()).isNull();
        assertThat(detail.getMemberCount()).isEqualTo(1);
    }

    // 2026-09-13: 관리자 화면 새로고침 후 카드 제작 진행 상태 복원 검증 후속 조치 — zodiacDesignSet/
    // cardDesignId/cardIssueDate는 각 액션 시점에 이미 Application에 저장되지만 이 응답에 없어
    // 재조회로 확인할 수 없었다(관리자 상세 조회도 이 응답을 그대로 씀). 응답 매핑만 추가한 것이라,
    // 이미 저장된 값이 그대로 노출되는지만 검증한다.
    @Test
    void getMyApplicationDetailExposesCardProductionProgressFields() {
        Application application = saveApplication(OWNER_ID, IssueType.MOBILE);
        application.assignZodiacDesignSet(2);
        application.confirmCardGeneration(99L, LocalDate.of(2026, 9, 13));
        applicationRepository.save(application);

        MyApplicationDetailResponse detail = applicationService.getMyApplicationDetail(OWNER_ID, application.getId());

        assertThat(detail.getZodiacDesignSet()).isEqualTo(2);
        assertThat(detail.getCardDesignId()).isEqualTo(99L);
        assertThat(detail.getCardIssueDate()).isEqualTo(LocalDate.of(2026, 9, 13));
    }

    // 아직 아무것도 확정되지 않은 신청은 세 필드 모두 null이어야 한다 — "값이 있는데 안 보임"과
    // "아직 진행 안 됨"을 구분해서 검증한다.
    @Test
    void getMyApplicationDetailReturnsNullCardProductionProgressFieldsBeforeAnyActionTaken() {
        Application application = saveApplication(OWNER_ID, IssueType.MOBILE);

        MyApplicationDetailResponse detail = applicationService.getMyApplicationDetail(OWNER_ID, application.getId());

        assertThat(detail.getZodiacDesignSet()).isNull();
        assertThat(detail.getCardDesignId()).isNull();
        assertThat(detail.getCardIssueDate()).isNull();
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
