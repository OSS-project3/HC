package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationCardDownloadResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ApplicationServiceCardDownloadTest {

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
    private CardLookupTokenService cardLookupTokenService;

    @MockitoBean
    private StorageService storageService;

    private CardType cardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-download", null, BigDecimal.valueOf(30000)));

        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("http://mock-storage/presigned");
        when(storageService.download(anyString())).thenReturn("card-bytes".getBytes());
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenReturn("http://mock-storage/uploaded");
    }

    private void setCardPaths(ApplicationMember member, String front, String back) {
        try {
            var frontField = ApplicationMember.class.getDeclaredField("cardFrontPath");
            frontField.setAccessible(true);
            frontField.set(member, front);
            var backField = ApplicationMember.class.getDeclaredField("cardBackPath");
            backField.setAccessible(true);
            backField.set(member, back);
            applicationMemberRepository.save(member);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Application completedIndividualApplication(Long ownerId) {
        // ownerId를 번호에 반영해, 한 테스트 안에서 서로 다른 소유자로 두 번 호출해도 번호가 겹치지 않게 한다.
        Application application = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-40000" + ownerId, cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "owner@example.com", "010-1234-5678"));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        setCardPaths(member, "cards/front.png", "cards/back.png");

        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        return applicationRepository.save(application);
    }

    @Test
    void getCardDownloadReturnsFrontAndBackUrlsForIndividual() {
        Application application = completedIndividualApplication(1L);

        ApplicationCardDownloadResponse response = applicationService.getCardDownload(1L, application.getId());

        assertThat(response.getApplicationType().name()).isEqualTo("INDIVIDUAL");
        assertThat(response.getCardFrontUrl()).isEqualTo("http://mock-storage/presigned");
        assertThat(response.getCardBackUrl()).isEqualTo("http://mock-storage/presigned");
        assertThat(response.getDownloadUrl()).isNull();
        assertThat(response.getExpiresAt()).isNotNull();
    }

    @Test
    void getCardDownloadReturnsZipUrlForGroup() {
        Application application = applicationRepository.save(Application.createGroup(
                1L, "APP-2026-400002", cardType.getId(), IssueType.MOBILE, true, 1, 10L, 11L, 12L));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "010-2222-2222", "Seoul", null, null, "photos/b.jpg"));
        setCardPaths(member, "cards/front2.png", "cards/back2.png");

        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);

        ApplicationCardDownloadResponse response = applicationService.getCardDownload(1L, application.getId());

        assertThat(response.getApplicationType().name()).isEqualTo("GROUP");
        assertThat(response.getDownloadUrl()).isEqualTo("http://mock-storage/presigned");
        assertThat(response.getCardFrontUrl()).isNull();
        assertThat(response.getCardBackUrl()).isNull();
    }

    @Test
    void getCardDownloadRejectsWhenApplicationNotCompleted() {
        Application application = applicationRepository.save(Application.createIndividual(
                1L, "APP-2026-400003", cardType.getId(), IssueType.MOBILE, true, null, null));

        assertThatThrownBy(() -> applicationService.getCardDownload(1L, application.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_READY);
    }

    @Test
    void getCardDownloadRejectsWhenNotOwner() {
        Application application = completedIndividualApplication(1L);

        assertThatThrownBy(() -> applicationService.getCardDownload(2L, application.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void getCardDownloadThrowsNotFoundForUnknownApplication() {
        assertThatThrownBy(() -> applicationService.getCardDownload(1L, 999999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void getCardDownloadByTokenReturnsUrlsForValidToken() {
        Application application = completedIndividualApplication(1L);
        String token = cardLookupTokenService.issue(application.getId());

        ApplicationCardDownloadResponse response =
                applicationService.getCardDownloadByToken(application.getId(), token);

        assertThat(response.getCardFrontUrl()).isEqualTo("http://mock-storage/presigned");
        assertThat(response.getCardBackUrl()).isEqualTo("http://mock-storage/presigned");
    }

    @Test
    void getCardDownloadByTokenIsSingleUse() {
        Application application = completedIndividualApplication(1L);
        String token = cardLookupTokenService.issue(application.getId());

        applicationService.getCardDownloadByToken(application.getId(), token);

        assertThatThrownBy(() -> applicationService.getCardDownloadByToken(application.getId(), token))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOOKUP_TOKEN);
    }

    @Test
    void getCardDownloadByTokenRejectsTokenIssuedForDifferentApplication() {
        Application application = completedIndividualApplication(1L);
        Application other = completedIndividualApplication(2L);
        String tokenForOther = cardLookupTokenService.issue(other.getId());

        assertThatThrownBy(() -> applicationService.getCardDownloadByToken(application.getId(), tokenForOther))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOOKUP_TOKEN);
    }

    @Test
    void getCardDownloadByTokenRejectsUnknownToken() {
        Application application = completedIndividualApplication(1L);

        assertThatThrownBy(() -> applicationService.getCardDownloadByToken(application.getId(), "not-a-real-token"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOOKUP_TOKEN);
    }

    @Test
    void getCardDownloadByTokenAllowsPhysicalApplicationOnceCardIsReadyEvenIfStillProducing() {
        Application application = applicationRepository.save(Application.createIndividual(
                1L, "APP-2026-400005", cardType.getId(), IssueType.MOBILE_AND_PHYSICAL, true, null, null));
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "owner@example.com", "010-1234-5678"));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        setCardPaths(member, "cards/front.png", "cards/back.png");
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);
        String token = cardLookupTokenService.issue(application.getId());

        ApplicationCardDownloadResponse response =
                applicationService.getCardDownloadByToken(application.getId(), token);

        assertThat(response.getCardFrontUrl()).isEqualTo("http://mock-storage/presigned");
    }

    @Test
    void getCardDownloadByTokenRejectsWhenApplicationNotCompletedEvenWithValidToken() {
        Application application = applicationRepository.save(Application.createIndividual(
                1L, "APP-2026-400004", cardType.getId(), IssueType.MOBILE, true, null, null));
        String token = cardLookupTokenService.issue(application.getId());

        assertThatThrownBy(() -> applicationService.getCardDownloadByToken(application.getId(), token))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_READY);
    }

    // 구성원 범위 토큰: 단체 신청이어도 그 구성원의 앞·뒷면만 내려준다(ZIP 없음).
    @Test
    void getCardDownloadByTokenWithMemberScopeReturnsOnlyThatMemberCard() {
        Application application = applicationRepository.save(Application.createGroup(
                1L, "APP-2026-400010", cardType.getId(), IssueType.MOBILE, true, 2, 10L, 11L, 12L));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "+821022221111", "Seoul", null, null, "photos/b.jpg"));
        ApplicationMember target = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "Mike Kim", LocalDate.of(1992, 3, 3), "US",
                null, null, Gender.MALE, null, "mike@example.com", "+821033334444", "Busan", null, null, "photos/c.jpg"));
        setCardPaths(target, "cards/front3.png", "cards/back3.png");

        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);

        String token = cardLookupTokenService.issue(application.getId(), target.getId());
        ApplicationCardDownloadResponse response =
                applicationService.getCardDownloadByToken(application.getId(), token);

        assertThat(response.getDownloadUrl()).isNull();
        assertThat(response.getCardFrontUrl()).isEqualTo("http://mock-storage/presigned");
        assertThat(response.getCardBackUrl()).isEqualTo("http://mock-storage/presigned");
        verify(storageService).generatePresignedUrl("cards/front3.png", 604800L);
        verify(storageService).generatePresignedUrl("cards/back3.png", 604800L);
    }

    // 신청 범위 토큰: 단체 신청은 기존처럼 ZIP을 준다(회귀 방지).
    @Test
    void getCardDownloadByTokenWithApplicationScopeStillReturnsZipForGroup() {
        Application application = applicationRepository.save(Application.createGroup(
                1L, "APP-2026-400011", cardType.getId(), IssueType.MOBILE, true, 1, 10L, 11L, 12L));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "+821022221111", "Seoul", null, null, "photos/b.jpg"));
        setCardPaths(member, "cards/front2.png", "cards/back2.png");

        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);

        String token = cardLookupTokenService.issue(application.getId());
        ApplicationCardDownloadResponse response =
                applicationService.getCardDownloadByToken(application.getId(), token);

        assertThat(response.getDownloadUrl()).isNotNull();
        assertThat(response.getCardFrontUrl()).isNull();
    }

    // 다른 신청에 속한 구성원 ID로 만든 토큰은 거절한다(토큰의 신청 ID와 구성원 소속이 맞아야 한다).
    @Test
    void getCardDownloadByTokenRejectsMemberOfAnotherApplication() {
        Application application = completedIndividualApplication(1L);
        Application other = completedIndividualApplication(2L);
        ApplicationMember otherMember = applicationMemberRepository.findByApplicationId(other.getId()).get(0);

        String token = cardLookupTokenService.issue(application.getId(), otherMember.getId());

        assertThatThrownBy(() -> applicationService.getCardDownloadByToken(application.getId(), token))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }
}
