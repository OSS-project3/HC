package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.AdminMemberCardDownloadResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 관리자 카드 다운로드(2026-09-05 정책, ApplicationServiceCardDownloadTest의 사용자용과 대비되는 계약):
// ApplicationStatus가 아니라 각 멤버의 렌더링 결과물(cardFrontPath/cardBackPath) 존재 여부로만 판단한다.
@SpringBootTest
class ApplicationServiceAdminCardDownloadTest {

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
    private UserRepository userRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @MockitoBean
    private StorageService storageService;

    private CardType cardType;
    private Long adminId;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-admin-download", null, BigDecimal.valueOf(30000)));

        User admin = userRepository.save(User.createOAuthUser("admin-card-download@example.com", "oauth-admin-cd", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("http://mock-storage/presigned");
        when(storageService.generatePresignedDownloadUrl(anyString(), anyLong(), anyString()))
                .thenReturn("http://mock-storage/presigned-download");
        when(storageService.download(anyString())).thenAnswer(inv -> ("bytes-of-" + inv.getArgument(0)).getBytes());
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

    private int applicationSequence = 0;

    // PRODUCING까지만 진행하고 COMPLETED로는 넘기지 않는다 — 사용자용 getCardDownload라면 거절될
    // 상태에서도 관리자 다운로드는 허용돼야 한다는 게 이번 정책의 핵심이다.
    private Application groupApplicationInProducing(int totalQuantity) {
        String applicationNumber = "APP-2026-50" + String.format("%04d", ++applicationSequence);
        Application application = applicationRepository.save(Application.createGroup(
                1L, applicationNumber, cardType.getId(), IssueType.MOBILE, true, totalQuantity, 10L, 11L, 12L));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        return applicationRepository.save(application);
    }

    private ApplicationMember addMember(Long applicationId, String englishName) {
        return applicationMemberRepository.save(ApplicationMember.createGroupRow(
                applicationId, englishName, LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, englishName + "@example.com", "010-2222-2222", "Seoul",
                null, null, "photos/" + englishName + ".jpg"));
    }

    // 개인 신청 — 관리자 카드 개별 다운로드 파일명 규칙(2026-09-24 확정) 검증용.
    private Application individualApplicationInProducing() {
        String applicationNumber = "APP-2026-60" + String.format("%04d", ++applicationSequence);
        Application application = applicationRepository.save(Application.createIndividual(
                1L, applicationNumber, cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "홍길동", "individual-admin-dl@example.com", "010-3333-3333"));
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        return applicationRepository.save(application);
    }

    @Test
    void adminZipDownloadSucceedsWhileApplicationIsStillProducingNotCompleted() throws Exception {
        Application application = groupApplicationInProducing(2);
        ApplicationMember first = addMember(application.getId(), "JohnDoe");
        setCardPaths(first, "cards/john-front.png", "cards/john-back.png");
        ApplicationMember second = addMember(application.getId(), "JaneDoe");
        setCardPaths(second, "cards/jane-front.png", "cards/jane-back.png");

        assertThat(application.getStatus().name()).isEqualTo("PRODUCING");

        byte[] zipBytes = applicationService.getAdminCardsZip(adminId, application.getId());

        assertThat(zipEntryNames(zipBytes)).containsExactlyInAnyOrder(
                "JohnDoe-front.png", "JohnDoe-back.png", "JaneDoe-front.png", "JaneDoe-back.png");
    }

    @Test
    void adminZipDownloadRejectsWhenAnyMemberMissingCardImagesAndIdentifiesWhichOne() {
        Application application = groupApplicationInProducing(2);
        ApplicationMember ready = addMember(application.getId(), "ReadyMember");
        setCardPaths(ready, "cards/ready-front.png", "cards/ready-back.png");
        ApplicationMember notReady = addMember(application.getId(), "NotReadyMember");
        // cardFrontPath/cardBackPath를 설정하지 않는다 — 아직 카드가 생성되지 않은 멤버.

        assertThatThrownBy(() -> applicationService.getAdminCardsZip(adminId, application.getId()))
                .isInstanceOf(BulkValidationException.class)
                .satisfies(ex -> {
                    BulkValidationException e = (BulkValidationException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CARD_NOT_READY);
                    assertThat(e.getErrors()).hasSize(1);
                    assertThat(e.getErrors().get(0).row()).isEqualTo(notReady.getId().intValue());
                });
    }

    // 2026-09-13: 관리자 화면 새로고침 후 카드 제작 진행 상태 복원 검증 후속 조치 — issueDate/
    // cardFrontPath/cardBackPath는 카드 생성 시 이미 저장되지만 이 응답에 없어 재조회로 확인할 수
    // 없었다. 신규 저장 로직은 없고 응답 매핑만 추가한 것이라, 이미 저장된 값이 그대로 노출되는지만
    // 검증한다.
    @Test
    void getApplicationMembersForAdminExposesCardProductionFields() {
        Application application = groupApplicationInProducing(1);
        ApplicationMember generated = addMember(application.getId(), "GeneratedMember");
        generated.assignCardImages("cards/generated-front.png", "cards/generated-back.png", LocalDate.of(2026, 9, 13));
        applicationMemberRepository.save(generated);
        ApplicationMember notGenerated = addMember(application.getId(), "NotGeneratedMember");

        var members = applicationService.getApplicationMembersForAdmin(adminId, application.getId());

        var generatedResponse = members.stream().filter(m -> m.getMemberId().equals(generated.getId())).findFirst().orElseThrow();
        assertThat(generatedResponse.getIssueDate()).isEqualTo(LocalDate.of(2026, 9, 13));
        assertThat(generatedResponse.getCardFrontPath()).isEqualTo("cards/generated-front.png");
        assertThat(generatedResponse.getCardBackPath()).isEqualTo("cards/generated-back.png");

        var notGeneratedResponse = members.stream().filter(m -> m.getMemberId().equals(notGenerated.getId())).findFirst().orElseThrow();
        assertThat(notGeneratedResponse.getIssueDate()).isNull();
        assertThat(notGeneratedResponse.getCardFrontPath()).isNull();
        assertThat(notGeneratedResponse.getCardBackPath()).isNull();
    }

    @Test
    void adminMemberDownloadSucceedsEvenWhenOtherMembersAreNotReady() {
        Application application = groupApplicationInProducing(2);
        ApplicationMember ready = addMember(application.getId(), "ReadyMember");
        setCardPaths(ready, "cards/ready-front.png", "cards/ready-back.png");
        addMember(application.getId(), "NotReadyMember"); // 이 멤버는 준비 안 됨 — 전체 ZIP이면 거절 대상.

        AdminMemberCardDownloadResponse response =
                applicationService.getAdminMemberCardDownload(adminId, application.getId(), ready.getId());

        assertThat(response.getMemberId()).isEqualTo(ready.getId());
        assertThat(response.getCardFrontUrl()).isEqualTo("http://mock-storage/presigned-download");
        assertThat(response.getCardBackUrl()).isEqualTo("http://mock-storage/presigned-download");
        // 2026-09-06: presigned URL 만료를 30일에서 7일로 낮춘 버그 수정(SigV4 최대 7일 하드리밋)에
        // 맞춰 갱신 — 사용자용과 같은 7일이다(ApplicationService.ADMIN_CARD_DOWNLOAD_URL_EXPIRY_SECONDS).
        assertThat(response.getExpiresAt()).isAfter(java.time.LocalDateTime.now().plusDays(6));
    }

    @Test
    void adminMemberDownloadRejectsWhenThatMemberNotReady() {
        Application application = groupApplicationInProducing(1);
        ApplicationMember notReady = addMember(application.getId(), "NotReadyMember");

        assertThatThrownBy(() -> applicationService.getAdminMemberCardDownload(adminId, application.getId(), notReady.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_READY);
    }

    @Test
    void adminMemberDownloadRejectsWhenMemberBelongsToDifferentApplication() {
        Application application = groupApplicationInProducing(1);
        Application other = groupApplicationInProducing(1);
        ApplicationMember memberOfOther = addMember(other.getId(), "OtherAppMember");
        setCardPaths(memberOfOther, "cards/x-front.png", "cards/x-back.png");

        assertThatThrownBy(() -> applicationService.getAdminMemberCardDownload(adminId, application.getId(), memberOfOther.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    // --- 파일명 규칙(2026-09-24 확정) — 새 탭 대신 파일로 저장되도록 presigned URL에 실어보내는
    // 파일명이 정책과 일치하는지 검증한다. 실제 다운로드(Content-Disposition 헤더 적용) 자체는
    // S3StorageService가 맡고 여기선 Mock으로 어떤 파일명이 전달됐는지만 확인한다. ---

    @Test
    void adminMemberDownloadUsesApplicationNumberOnlyFileNameForIndividual() {
        Application application = individualApplicationInProducing();
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/hong.jpg"));
        setCardPaths(member, "cards/hong-front.png", "cards/hong-back.png");

        applicationService.getAdminMemberCardDownload(adminId, application.getId(), member.getId());

        ArgumentCaptor<String> fileNames = ArgumentCaptor.forClass(String.class);
        verify(storageService, org.mockito.Mockito.times(2))
                .generatePresignedDownloadUrl(anyString(), anyLong(), fileNames.capture());
        assertThat(fileNames.getAllValues()).containsExactlyInAnyOrder(
                application.getApplicationNumber() + "-front.png",
                application.getApplicationNumber() + "-back.png");
    }

    @Test
    void adminMemberDownloadIncludesNameAndMemberIdFileNameForGroup() {
        Application application = groupApplicationInProducing(1);
        ApplicationMember member = addMember(application.getId(), "GilDongHong");
        member.assignKoreanName("홍", "길동", null);
        setCardPaths(member, "cards/gildong-front.png", "cards/gildong-back.png");

        applicationService.getAdminMemberCardDownload(adminId, application.getId(), member.getId());

        verify(storageService).generatePresignedDownloadUrl(eq("cards/gildong-front.png"), anyLong(),
                eq(application.getApplicationNumber() + "-홍길동-" + member.getId() + "-front.png"));
        verify(storageService).generatePresignedDownloadUrl(eq("cards/gildong-back.png"), anyLong(),
                eq(application.getApplicationNumber() + "-홍길동-" + member.getId() + "-back.png"));
    }

    @Test
    void adminMemberDownloadFallsBackToMemberIdOnlyWhenNameMissingForGroup() {
        Application application = groupApplicationInProducing(1);
        ApplicationMember member = addMember(application.getId(), "NoNameYet");
        // surname/name을 설정하지 않는다 — 이례적이지만(보통 카드 생성 전 작명이 끝나 있음) 방어적으로 확인.
        setCardPaths(member, "cards/noname-front.png", "cards/noname-back.png");

        applicationService.getAdminMemberCardDownload(adminId, application.getId(), member.getId());

        verify(storageService).generatePresignedDownloadUrl(eq("cards/noname-front.png"), anyLong(),
                eq(application.getApplicationNumber() + "-" + member.getId() + "-front.png"));
    }

    @Test
    void adminMemberDownloadFileNamesDoNotCollideForSameNameMembersInSameGroup() {
        Application application = groupApplicationInProducing(2);
        ApplicationMember first = addMember(application.getId(), "First");
        first.assignKoreanName("홍", "길동", null);
        setCardPaths(first, "cards/first-front.png", "cards/first-back.png");
        ApplicationMember second = addMember(application.getId(), "Second");
        second.assignKoreanName("홍", "길동", null); // 동명이인 — memberId로만 구분 가능해야 한다.
        setCardPaths(second, "cards/second-front.png", "cards/second-back.png");

        applicationService.getAdminMemberCardDownload(adminId, application.getId(), first.getId());
        applicationService.getAdminMemberCardDownload(adminId, application.getId(), second.getId());

        ArgumentCaptor<String> fileNames = ArgumentCaptor.forClass(String.class);
        verify(storageService, org.mockito.Mockito.times(4))
                .generatePresignedDownloadUrl(anyString(), anyLong(), fileNames.capture());
        assertThat(fileNames.getAllValues()).doesNotHaveDuplicates();
        assertThat(fileNames.getAllValues()).allSatisfy(name -> assertThat(name).contains("홍길동"));
    }

    @Test
    void adminDownloadsRejectNonAdminCaller() {
        Application application = groupApplicationInProducing(1);
        User nonAdmin = userRepository.save(User.createOAuthUser("not-admin@example.com", "oauth-not-admin", "google", "User"));

        assertThatThrownBy(() -> applicationService.getAdminCardsZip(nonAdmin.getId(), application.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void adminZipDownloadLogsActivity() {
        Application application = groupApplicationInProducing(1);
        ApplicationMember member = addMember(application.getId(), "LoggedMember");
        setCardPaths(member, "cards/logged-front.png", "cards/logged-back.png");

        applicationService.getAdminCardsZip(adminId, application.getId());

        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(logEntry -> {
                    assertThat(logEntry.getActionType()).isEqualTo("CARD_DOWNLOAD");
                    assertThat(logEntry.getAdminId()).isEqualTo(adminId);
                    assertThat(logEntry.getTargetId()).isEqualTo(application.getId());
                });
    }

    private java.util.List<String> zipEntryNames(byte[] zipBytes) throws Exception {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
