package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// QA 체크리스트 14번: 카드 경로 DB 반영(CardGenerationPersistenceService.persist())과 성공
// 감사로그(CARD_IMAGE_GENERATED) 저장이 같은 트랜잭션인지 검증한다(확정 정책, 7번 학교 템플릿과
// 동일). 감사로그 저장이 실패하면 DB 변경 전체가 롤백되고, 이번 요청에서 새로 올린 S3 이미지도
// 보상 삭제돼야 한다 — 재생성이었다면 기존 카드 이미지·기존 DB 경로는 그대로 유지돼야 한다.
// AdminActivityLogRepository만 실패하는 Mock으로 교체해 재현한다(SchoolCardTemplateServiceAuditLogFailureTest와
// 동일한 방식) — 단, 실패 경로(catch 블록)가 남기는 "생성 실패" 로그까지 막아버리면 원 예외가
// 감사로그 저장 실패로 가려지므로, "성공" 로그일 때만 실패하도록 detail 내용으로 구분한다.
@SpringBootTest
class CardGenerationServiceAuditLogFailureTest {

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

    @MockitoBean
    private StorageService storageService;
    @MockitoBean
    private AdminActivityLogRepository adminActivityLogRepository;

    private Long adminId;
    private Long applicationId;
    private Long memberId;
    private Long cardDesignId;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardDesignRepository.deleteAll();
        cardTypeRepository.deleteAll();
        uploadFileRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("generate-audit-fail-admin@example.com", "oauth-generate-audit-fail-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("generate-audit-fail-user@example.com", "oauth-generate-audit-fail-user", "google", "User"));
        Long userId = user.getId();

        CardType honorKorean = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-audit-fail", null, BigDecimal.ZERO));
        Long honorKoreanTypeId = honorKorean.getId();

        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인1", 1, CardDesignOrientation.LANDSCAPE, null, null, true));
        cardDesignId = design.getId();

        Application application = Application.createIndividual(
                userId, "APP-2026-AUDITFAIL01", honorKoreanTypeId, IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application.assignZodiacDesignSet(1);
        application = applicationRepository.save(application);
        applicationId = application.getId();

        ApplicationMember member = ApplicationMember.createIndividual(applicationId, "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null, null, null,
                "photos/generate.jpg", "대한민국 전라북도 전주시");
        member.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        member.assignCardNumber("ROK-33333-9999");
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

    private void makeSuccessLogSaveFail() {
        when(adminActivityLogRepository.save(any(AdminActivityLog.class))).thenAnswer(invocation -> {
            AdminActivityLog log = invocation.getArgument(0);
            if (log.getDetail() != null && log.getDetail().contains("성공")) {
                throw new RuntimeException("DB 장애(감사로그 저장)");
            }
            return log;
        });
    }

    @Test
    void firstGenerationRollsBackAndDeletesNewFilesWhenAuditLogSaveFails() {
        makeSuccessLogSaveFail();

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId, request()))
                .isInstanceOf(RuntimeException.class);

        ApplicationMember saved = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(saved.isCardGenerated()).isFalse();
        Application savedApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(savedApplication.getCardDesignId()).isNull();

        verify(storageService, times(2)).delete(anyString());
    }

    @Test
    void regenerationKeepsOldFilesAndDbPathsAndDeletesOnlyNewFilesWhenAuditLogSaveFails() {
        // 최초 생성은 감사로그가 정상 저장되는 상태로 먼저 성공시킨다.
        var first = cardGenerationService.generate(adminId, applicationId, memberId, request());

        makeSuccessLogSaveFail();

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, applicationId, memberId, request()))
                .isInstanceOf(RuntimeException.class);

        // 재생성 트랜잭션이 롤백돼 기존(첫 번째) 카드 경로가 그대로 유지돼야 한다.
        ApplicationMember saved = applicationMemberRepository.findById(memberId).orElseThrow();
        assertThat(saved.getCardFrontPath()).isEqualTo(first.cardFrontPath());
        assertThat(saved.getCardBackPath()).isEqualTo(first.cardBackPath());

        // 기존 파일은 삭제되면 안 되고, 이번에 새로 올라간 파일만 보상 삭제돼야 한다.
        verify(storageService, times(0)).delete(first.cardFrontPath());
        verify(storageService, times(0)).delete(first.cardBackPath());
        verify(storageService, times(2)).delete(anyString());
    }
}
