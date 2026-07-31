package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.LookupMethod;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupRequest;
import com.example.honorcitizen.domain.application.dto.ApplicationLookupResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ApplicationServiceLookupTest {

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
    @Autowired
    private ObjectMapper objectMapper;

    private Application individualApplication;
    private ApplicationMember individualMember;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();

        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-lookup", null, BigDecimal.valueOf(30000)));

        individualApplication = applicationRepository.save(Application.createIndividual(
                1L, "APP-2026-100001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                individualApplication.getId(), "이순신", "lee@example.com", "010-1111-2222"));
        individualMember = applicationMemberRepository.save(ApplicationMember.createIndividual(
                individualApplication.getId(), "Lee Sunsin", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        individualMember = withCardNumber(individualMember, "ROK-00001-0001");
    }

    private ApplicationMember withCardNumber(ApplicationMember member, String cardNumber) {
        // 엔티티에 채번 메서드가 없어 리플렉션 대신 리포지토리 재조회 후 네이티브 업데이트 대용으로 신규 저장
        // (Admin 카드발급 도메인은 이번 범위 밖이라 테스트 전용으로 직접 필드값을 세팅)
        try {
            var field = ApplicationMember.class.getDeclaredField("cardNumber");
            field.setAccessible(true);
            field.set(member, cardNumber);
            return applicationMemberRepository.save(member);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private ApplicationLookupRequest request(String json) {
        try {
            return objectMapper.readValue(json, ApplicationLookupRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void lookupByApplicationNumberWithMatchingPhoneSucceeds() {
        ApplicationLookupResponse response = applicationService.lookup(request("""
                { "method": "application", "keyValue": "APP-2026-100001", "phone": "010-1111-2222" }
                """));

        assertThat(response.getApplicationId()).isEqualTo(individualApplication.getId());
        assertThat(response.getApplicantNameMasked()).isEqualTo("이*신");
        assertThat(response.getCardType()).isEqualTo("명예한국인증-lookup");
        assertThat(response.getStatus().name()).isEqualTo("PAYMENT_PENDING");
    }

    @Test
    void lookupByApplicationNumberWithMatchingEmailSucceeds() {
        ApplicationLookupResponse response = applicationService.lookup(request("""
                { "method": "application", "keyValue": "APP-2026-100001", "email": "lee@example.com" }
                """));

        assertThat(response.getApplicationId()).isEqualTo(individualApplication.getId());
    }

    @Test
    void lookupByApplicationNumberWithWrongContactFailsWithNotFound() {
        assertThatThrownBy(() -> applicationService.lookup(request("""
                { "method": "application", "keyValue": "APP-2026-100001", "phone": "010-0000-0000" }
                """)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void lookupWithoutPhoneOrEmailIsRejected() {
        assertThatThrownBy(() -> applicationService.lookup(request("""
                { "method": "application", "keyValue": "APP-2026-100001" }
                """)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void lookupByCardFallsBackToApplicantContactForIndividualApplication() {
        ApplicationLookupResponse response = applicationService.lookup(request("""
                { "method": "card", "keyValue": "ROK-00001-0001", "phone": "010-1111-2222" }
                """));

        assertThat(response.getApplicationId()).isEqualTo(individualApplication.getId());
    }

    @Test
    void lookupByCardUsesMemberContactForGroupApplication() {
        CardType cardType = cardTypeRepository.findAll().get(0);
        Application groupApplication = applicationRepository.save(Application.createGroup(
                2L, "APP-2026-100002", cardType.getId(), IssueType.MOBILE, true, 1, null, null, 999L));
        applicantRepository.save(Applicant.createGroup(
                groupApplication.getId(), "인사담당", "hr@example.com", "010-9999-9999", "OO기업", "인사팀"));
        ApplicationMember groupMember = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                groupApplication.getId(), "John Doe", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "010-1234-5678", "Seoul", null, null, "photos/b.jpg"));
        withCardNumber(groupMember, "ROK-00002-0002");

        ApplicationLookupResponse response = applicationService.lookup(request("""
                { "method": "card", "keyValue": "ROK-00002-0002", "phone": "010-1234-5678" }
                """));
        assertThat(response.getApplicationId()).isEqualTo(groupApplication.getId());

        assertThatThrownBy(() -> applicationService.lookup(request("""
                { "method": "card", "keyValue": "ROK-00002-0002", "phone": "010-9999-9999" }
                """)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void lookupExposesPhotoRejectReasonOnlyWhenPhotoRejected() {
        individualApplication.confirmPayment();
        individualApplication.startReview();
        individualApplication.rejectPhoto("사진이 흐립니다.");
        applicationRepository.save(individualApplication);

        ApplicationLookupResponse response = applicationService.lookup(request("""
                { "method": "application", "keyValue": "APP-2026-100001", "phone": "010-1111-2222" }
                """));

        assertThat(response.getStatus().name()).isEqualTo("PHOTO_REJECTED");
        assertThat(response.getPhotoRejectReason()).isEqualTo("사진이 흐립니다.");
    }

    @Test
    void lookupThrowsNotFoundForUnknownApplicationNumber() {
        assertThatThrownBy(() -> applicationService.lookup(request("""
                { "method": "application", "keyValue": "APP-2026-999999", "phone": "010-1111-2222" }
                """)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }
}
