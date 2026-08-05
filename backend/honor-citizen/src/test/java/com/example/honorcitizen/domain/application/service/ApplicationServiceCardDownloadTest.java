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
        Application application = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-400001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "owner@example.com", "010-1234-5678"));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
        setCardPaths(member, "cards/front.png", "cards/back.png");

        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.startProducing();
        application.complete();
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
        application.startProducing();
        application.complete();
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
}
