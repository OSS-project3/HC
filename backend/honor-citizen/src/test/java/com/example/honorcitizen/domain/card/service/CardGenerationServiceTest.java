package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.dto.CardGenerateResponse;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 3. 카드 생성·저장 — 최소 버전(2026-08-30). 공유 검증·렌더링(CardRenderPreparation)은
// CardPreviewServiceTest가 이미 폭넓게 검증하므로, 여기서는 Generate 고유 동작만 검증한다:
// S3 업로드·보상삭제, 재생성 순서(신규 선업로드->commit->기존 후삭제), 멱등성 없음, 디자인/발급일자
// 확정+불일치 거절, 상태 게이트(cardReadyAt 포함), 상태 자동전환 없음, 감사로그.
// StorageService만 Mock(S3 실접근 없이), 나머지는 실제 DB(CardPreviewServiceTest와 동일 스타일).
@SpringBootTest
class CardGenerationServiceTest {

    @Autowired
    private CardGenerationService cardGenerationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private ManseryeokResultRepository manseryeokResultRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

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
        adminActivityLogRepository.deleteAll();
        manseryeokResultRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardDesignRepository.deleteAll();
        cardTypeRepository.deleteAll();
        uploadFileRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("generate-admin@example.com", "oauth-generate-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("generate-user@example.com", "oauth-generate-user", "google", "User"));
        userId = user.getId();

        CardType honorKorean = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-generate", null, BigDecimal.ZERO));
        honorKoreanTypeId = honorKorean.getId();

        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인1", 1, CardDesignOrientation.LANDSCAPE, null, null, true));
        cardDesignId = design.getId();

        Application application = Application.createIndividual(
                userId, "APP-2026-GEN0001", honorKoreanTypeId, IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application = applicationRepository.save(application);
        applicationId = application.getId();

        ApplicationMember member = ApplicationMember.createIndividual(applicationId, "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null, null, null,
                "photos/generate.jpg", "대한민국 전라북도 전주시");
        member.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        member.assignCardNumber("ROK-33333-4444");
        member = applicationMemberRepository.save(member);
        memberId = member.getId();

        manseryeokResultRepository.save(ManseryeokResult.create(memberId, "hash", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("1995-02-07T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"갑\",\"branch\":\"술\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        when(storageService.download(anyString())).thenReturn(samplePng());
        when(storageService.uploadBytes(anyString(), any(byte[].class), anyString())).thenReturn("stored");
    }

    private CardPreviewRequest request() {
        return request(cardDesignId, LocalDate.now());
    }

    private CardPreviewRequest request(Long designId, LocalDate issueDate) {
        CardPreviewRequest req = new CardPreviewRequest();
        ReflectionTestUtils.setField(req, "cardDesignId", designId);
        ReflectionTestUtils.setField(req, "issueDate", issueDate);
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

    @Test
    void generatesNewCardAndPersists() {
        CardGenerateResponse response = cardGenerationService.generate(adminId, applicationId, memberId, request());

        assertThat(response.cardFrontPath()).contains("/members/" + memberId + "/card/front-");
        assertThat(response.cardBackPath()).contains("/members/" + memberId + "/card/back-");
        assertThat(response.issueDate()).isEqualTo(LocalDate.now());

        ApplicationMember saved = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(saved.getCardFrontPath()).isEqualTo(response.cardFrontPath());
        assertThat(saved.getCardBackPath()).isEqualTo(response.cardBackPath());
        assertThat(saved.getIssueDate()).isEqualTo(LocalDate.now());
        assertThat(saved.isCardGenerated()).isTrue();

        Application savedApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(savedApplication.getCardDesignId()).isEqualTo(cardDesignId);
        assertThat(savedApplication.getCardIssueDate()).isEqualTo(LocalDate.now());
        // 카드 생성 성공이 ApplicationStatus를 자동으로 바꾸지 않는다.
        assertThat(savedApplication.getStatus()).isEqualTo(ApplicationStatus.PRODUCTION_READY);

        verify(storageService).uploadBytes(eq(response.cardFrontPath()), any(byte[].class), eq("image/png"));
        verify(storageService).uploadBytes(eq(response.cardBackPath()), any(byte[].class), eq("image/png"));
        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getActionType()).isEqualTo(AdminActivityLog.CARD_IMAGE_GENERATED);
                    assertThat(log.getTargetId()).isEqualTo(memberId);
                });
    }

    @Test
    void regeneratesNewFilesFirstThenDeletesOldFilesAfterCommit() {
        CardGenerateResponse first = cardGenerationService.generate(adminId, applicationId, memberId, request());

        CardGenerateResponse second = cardGenerationService.generate(adminId, applicationId, memberId, request());

        assertThat(second.cardFrontPath()).isNotEqualTo(first.cardFrontPath());
        assertThat(second.cardBackPath()).isNotEqualTo(first.cardBackPath());

        ApplicationMember saved = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(saved.getCardFrontPath()).isEqualTo(second.cardFrontPath());
        assertThat(saved.getCardBackPath()).isEqualTo(second.cardBackPath());

        // 신규 파일 선업로드 -> DB commit -> 기존 파일 후삭제: old 경로만 delete 대상이어야 한다.
        verify(storageService).delete(first.cardFrontPath());
        verify(storageService).delete(first.cardBackPath());
        verify(storageService, never()).delete(second.cardFrontPath());
        verify(storageService, never()).delete(second.cardBackPath());
    }

    // 멱등성 없음(2026-08-30 정책) — 동일 요청을 다시 호출해도 스킵하지 않고 매번 새로 렌더링·업로드한다.
    @Test
    void rerendersEveryCallEvenForIdenticalRequest() {
        cardGenerationService.generate(adminId, applicationId, memberId, request());
        cardGenerationService.generate(adminId, applicationId, memberId, request());

        verify(storageService, times(4)).uploadBytes(anyString(), any(byte[].class), eq("image/png"));
    }

    @Test
    void rejectsBeforeAnyUploadWhenPreparationFails() {
        manseryeokResultRepository.deleteAll();

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MANSERYEOK_NOT_CONFIRMED);

        // 준비 단계(CardRenderPreparation)에서 이미 실패해 업로드 시도 자체가 없었어야 한다.
        verifyNoInteractions(storageService);
        assertThat(adminActivityLogRepository.findAll()).isEmpty();
        ApplicationMember saved = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(saved.isCardGenerated()).isFalse();
    }

    @Test
    void compensatesFrontUploadWhenBackUploadFails() {
        when(storageService.uploadBytes(contains("/card/back-"), any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("S3 down"));

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId, request()))
                .isInstanceOf(RuntimeException.class);

        verify(storageService).uploadBytes(contains("/card/front-"), any(byte[].class), anyString());
        verify(storageService).delete(contains("/card/front-"));
        verify(storageService, never()).delete(contains("/card/back-"));

        ApplicationMember saved = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(saved.isCardGenerated()).isFalse();
        Application savedApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(savedApplication.getCardDesignId()).isNull();

        // 예상 못한 RuntimeException도 실패로 감사로그에 남는다(2026-08-30 수정).
        assertThat(adminActivityLogRepository.findAll())
                .anySatisfy(log -> assertThat(log.getActionType()).isEqualTo(AdminActivityLog.CARD_IMAGE_GENERATED));
    }

    @Test
    void rejectsDifferentDesignAfterApplicationDesignConfirmed() {
        cardGenerationService.generate(adminId, applicationId, memberId, request());
        CardDesign otherDesign = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인2", 2, CardDesignOrientation.LANDSCAPE, null, null, true));

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId,
                request(otherDesign.getId(), LocalDate.now())))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_MISMATCH);
    }

    @Test
    void rejectsDifferentIssueDateAfterApplicationIssueDateConfirmed() {
        cardGenerationService.generate(adminId, applicationId, memberId, request());

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId,
                request(cardDesignId, LocalDate.now().plusDays(1))))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_ISSUE_DATE_MISMATCH);
    }

    @Test
    void allowsGenerateWhenProducingAndCardNotReady() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCING);
        applicationRepository.save(application);

        CardGenerateResponse response = cardGenerationService.generate(adminId, applicationId, memberId, request());

        assertThat(response.cardFrontPath()).isNotBlank();
    }

    @Test
    void rejectsWhenProducingAndCardAlreadyReady() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCING);
        ReflectionTestUtils.setField(application, "cardReadyAt", LocalDateTime.now());
        applicationRepository.save(application);

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void rejectsWhenCompleted() {
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.COMPLETED);
        applicationRepository.save(application);

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> cardGenerationService.generate(userId, applicationId, memberId, request()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        verifyNoInteractions(storageService);
    }
}
