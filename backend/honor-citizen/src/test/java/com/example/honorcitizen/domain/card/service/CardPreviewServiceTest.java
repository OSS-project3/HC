package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.card.dto.CardPreviewResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

// 2-C: 저장 없는 카드 미리보기 — 실제 신청 데이터(이름/만세력 확정 결과)로 CardImageCompositor까지
// 이어지는지 검증한다. StorageService만 Mock(S3 실접근 없이), 나머지는 실제 DB.
@SpringBootTest
class CardPreviewServiceTest {

    @Autowired
    private CardPreviewService cardPreviewService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private ManseryeokResultRepository manseryeokResultRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private Long userId;
    private Long applicationId;
    private Long memberId;
    private Long cardDesignId;
    private Long honorKoreanTypeId;

    @BeforeEach
    void setUp() {
        manseryeokResultRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardDesignRepository.deleteAll();
        cardTypeRepository.deleteAll();
        uploadFileRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("preview-admin@example.com", "oauth-preview-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("preview-user@example.com", "oauth-preview-user", "google", "User"));
        userId = user.getId();

        CardType honorKorean = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-preview", null, BigDecimal.ZERO));
        honorKoreanTypeId = honorKorean.getId();

        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인1", 1, CardDesignOrientation.LANDSCAPE, null, null, true));
        cardDesignId = design.getId();

        Application application = Application.createIndividual(
                userId, "APP-2026-PREVIEW01", honorKoreanTypeId, IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application.assignZodiacDesignSet(1);
        application = applicationRepository.save(application);
        applicationId = application.getId();

        ApplicationMember member = ApplicationMember.createIndividual(applicationId, "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null, null, null,
                "photos/preview.jpg", "대한민국 전라북도 전주시");
        member.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        member.assignCardNumber("ROK-12345-6789");
        member = applicationMemberRepository.save(member);
        memberId = member.getId();

        manseryeokResultRepository.save(ManseryeokResult.create(memberId, "hash", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("1995-02-07T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"갑\",\"branch\":\"술\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        when(storageService.download(anyString())).thenReturn(samplePng());
    }

    private CardPreviewRequest request() {
        CardPreviewRequest req = new CardPreviewRequest();
        ReflectionTestUtils.setField(req, "cardDesignId", cardDesignId);
        ReflectionTestUtils.setField(req, "issueDate", LocalDate.now());
        return req;
    }

    private byte[] samplePng() {
        try {
            BufferedImage img = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedImage decode(String base64Png) throws Exception {
        return ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(base64Png)));
    }

    // 2026-08-27 계약 변경 — 앞/뒤를 각각 요청하던 previewsFrontSuccessfully/previewsBackSuccessfully를
    // 한 번의 호출로 둘 다 검증하는 테스트로 합쳤다(요청 자체가 이제 side를 구분하지 않음).
    @Test
    void previewsFrontAndBackInOneCall() throws Exception {
        CardPreviewResponse response = cardPreviewService.preview(adminId, applicationId, memberId, request());

        assertThat(response.front()).isNotBlank();
        assertThat(response.back()).isNotBlank();
        assertThat(decode(response.front())).isNotNull();
        assertThat(decode(response.back())).isNotNull();
    }

    @Test
    void doesNotMutateDbOrUploadToStorage() {
        long applicationCountBefore = applicationRepository.count();
        long memberCountBefore = applicationMemberRepository.count();
        long manseryeokCountBefore = manseryeokResultRepository.count();

        cardPreviewService.preview(adminId, applicationId, memberId, request());

        assertThat(applicationRepository.count()).isEqualTo(applicationCountBefore);
        assertThat(applicationMemberRepository.count()).isEqualTo(memberCountBefore);
        assertThat(manseryeokResultRepository.count()).isEqualTo(manseryeokCountBefore);
        // 앞/뒤를 한 번의 호출에서 같이 만들므로, 공통 S3 다운로드(사진)는 여전히 한 번만 일어나야 한다
        // (예전에 side별로 나눠 호출할 때는 두 번 호출해야 두 번 다운로드됐던 것과 대조).
        verify(storageService).download(anyString());
        verifyNoMoreInteractions(storageService);
    }

    @Test
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> cardPreviewService.preview(userId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsMemberFromDifferentApplication() {
        Application otherApplication = Application.createIndividual(
                userId, "APP-2026-PREVIEW02", honorKoreanTypeId, IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(otherApplication, "status", ApplicationStatus.PRODUCTION_READY);
        Long otherApplicationId = applicationRepository.save(otherApplication).getId();

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, otherApplicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsWhenApplicationNotProductionReady() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.NAME_EDITING);
        applicationRepository.save(application);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void rejectsUnknownCardDesign() {
        CardPreviewRequest req = request();
        ReflectionTestUtils.setField(req, "cardDesignId", 999999L);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    @Test
    void rejectsDesignForDifferentCardType() {
        CardType visitor = cardTypeRepository.save(
                CardType.create(CardTypeCode.VISITOR, "방문증-preview", null, BigDecimal.ZERO));
        CardDesign mismatched = cardDesignRepository.save(CardDesign.create(
                visitor.getId(), "방문증 디자인1", 1, CardDesignOrientation.PORTRAIT, null, null, true));
        CardPreviewRequest req = request();
        ReflectionTestUtils.setField(req, "cardDesignId", mismatched.getId());

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_MISMATCH);
    }

    @Test
    void rejectsInactiveDesign() {
        CardDesign inactive = CardDesign.create(
                honorKoreanTypeId, "디자인2-미검수", 2, CardDesignOrientation.LANDSCAPE, null, null, false);
        inactive.deactivate();
        Long inactiveId = cardDesignRepository.save(inactive).getId();
        CardPreviewRequest req = request();
        ReflectionTestUtils.setField(req, "cardDesignId", inactiveId);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_MISMATCH);
    }

    @Test
    void rejectsIssueDateBeforeSubmission() {
        CardPreviewRequest req = request();
        ReflectionTestUtils.setField(req, "issueDate", LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_ISSUE_DATE_OUT_OF_RANGE);
    }

    @Test
    void rejectsIssueDateBeyondThreeMonths() {
        CardPreviewRequest req = request();
        ReflectionTestUtils.setField(req, "issueDate", LocalDate.now().plusMonths(3).plusDays(1));

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_ISSUE_DATE_OUT_OF_RANGE);
    }

    @Test
    void rejectsWhenNamingIncomplete() {
        ApplicationMember member = applicationMemberRepository.findById(memberId).orElseThrow();
        ReflectionTestUtils.setField(member, "surname", null);
        applicationMemberRepository.save(member);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NAMING_INCOMPLETE);
    }

    @Test
    void rejectsWhenCardNumberMissing() {
        ApplicationMember member = applicationMemberRepository.findById(memberId).orElseThrow();
        ReflectionTestUtils.setField(member, "cardNumber", null);
        applicationMemberRepository.save(member);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_READY);
    }

    @Test
    void rejectsGroupApplicationMissingIssuerAssets() {
        Application group = Application.createGroup(userId, "APP-2026-PVGRP1", honorKoreanTypeId,
                IssueType.MOBILE, true, 1, null, null, null);
        ReflectionTestUtils.setField(group, "status", ApplicationStatus.PRODUCTION_READY);
        group = applicationRepository.save(group);
        ApplicationMember groupMember = ApplicationMember.createGroupRow(group.getId(), "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null,
                "member@example.com", "010-0000-0000", "대한민국 전라북도 전주시", null, null,
                "photos/preview-group.jpg", "001");
        groupMember.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        groupMember.assignCardNumber("ROK-11111-2222");
        Long groupMemberId = applicationMemberRepository.save(groupMember).getId();
        manseryeokResultRepository.save(ManseryeokResult.create(groupMemberId, "hash2", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("1995-02-07T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"갑\",\"branch\":\"술\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        Long groupId = group.getId();
        CardPreviewRequest req = request();
        assertThatThrownBy(() -> cardPreviewService.preview(adminId, groupId, groupMemberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_ISSUER_ASSETS_MISSING);
    }

    // 2026-09-06 신규: 십이간지 캐릭터 디자인 세트가 아직 지정되지 않았으면(기본값 자동 적용 안 함,
    // 정책 확정) 렌더링 자체를 거절한다 — 만세력 확정 이후·설계상 마지막 단계에서 걸린다.
    @Test
    void rejectsWhenZodiacDesignNotSelected() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        ReflectionTestUtils.setField(application, "zodiacDesignSet", null);
        applicationRepository.save(application);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ZODIAC_DESIGN_NOT_SELECTED);
    }

    @Test
    void rejectsWhenNoActiveManseryeokResult() {
        manseryeokResultRepository.deleteAll();

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MANSERYEOK_NOT_CONFIRMED);
    }

    @Test
    void rejectsWhenYearPillarUncertain() {
        manseryeokResultRepository.deleteAll();
        manseryeokResultRepository.save(ManseryeokResult.create(memberId, "hash3", "Asia/Seoul", 127.0,
                null, null, TimeAccuracy.PARTIAL,
                "{}", "[\"year\",\"hour\"]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MANSERYEOK_NOT_CONFIRMED);
    }

    @Test
    void resolvesLogoAndSealFromUploadFile() throws Exception {
        UploadFile logo = uploadFileRepository.save(
                UploadFile.create("logo.png", "stored-logo.png", "uploads/logo.png", UploadFileType.PHOTO,
                        "image/png", 100));
        UploadFile seal = uploadFileRepository.save(
                UploadFile.create("seal.png", "stored-seal.png", "uploads/seal.png", UploadFileType.PHOTO,
                        "image/png", 100));
        Application group = Application.createGroup(userId, "APP-2026-PVGRP2", honorKoreanTypeId,
                IssueType.MOBILE, true, 1, logo.getId(), seal.getId(), null);
        ReflectionTestUtils.setField(group, "status", ApplicationStatus.PRODUCTION_READY);
        group.assignZodiacDesignSet(1);
        group = applicationRepository.save(group);
        ApplicationMember groupMember = ApplicationMember.createGroupRow(group.getId(), "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null,
                "member2@example.com", "010-0000-0001", "대한민국 전라북도 전주시", null, null,
                "photos/preview-group2.jpg", "002");
        groupMember.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        groupMember.assignCardNumber("ROK-22222-3333");
        groupMember = applicationMemberRepository.save(groupMember);
        manseryeokResultRepository.save(ManseryeokResult.create(groupMember.getId(), "hash4", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("1995-02-07T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"갑\",\"branch\":\"술\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        CardPreviewResponse response = cardPreviewService.preview(adminId, group.getId(), groupMember.getId(), request());

        assertThat(decode(response.front())).isNotNull();
        assertThat(decode(response.back())).isNotNull();
        verify(storageService).download("photos/preview-group2.jpg");
        verify(storageService).download("uploads/logo.png");
        verify(storageService).download("uploads/seal.png");
    }

    // 2026-08-30: 카드 생성(3.)에서 Application.cardDesignId/cardIssueDate가 확정된 이후에는
    // Preview도 다른 값을 거절한다(CardRenderPreparation 공유 검증) — 확정 전(null)에는 기존처럼
    // 어떤 값이든 미리볼 수 있다(위 previewsFrontAndBackInOneCall이 이미 그 경우를 검증한다).
    @Test
    void rejectsMismatchedDesignAfterApplicationDesignConfirmed() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        CardDesign otherDesign = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인2", 2, CardDesignOrientation.LANDSCAPE, null, null, true));
        application.confirmCardGeneration(otherDesign.getId(), LocalDate.now());
        applicationRepository.save(application);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_MISMATCH);
    }

    @Test
    void rejectsMismatchedIssueDateAfterApplicationIssueDateConfirmed() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        application.confirmCardGeneration(cardDesignId, LocalDate.now().minusDays(1));
        applicationRepository.save(application);

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_ISSUE_DATE_MISMATCH);
    }

    @Test
    void allowsSameDesignAndIssueDateAfterConfirmed() throws Exception {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        application.confirmCardGeneration(cardDesignId, LocalDate.now());
        applicationRepository.save(application);

        CardPreviewResponse response = cardPreviewService.preview(adminId, applicationId, memberId, request());

        assertThat(decode(response.front())).isNotNull();
        assertThat(decode(response.back())).isNotNull();
    }
}
