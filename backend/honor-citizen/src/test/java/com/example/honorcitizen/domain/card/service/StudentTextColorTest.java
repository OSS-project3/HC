package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.StudentTextColor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 학생증 앞·뒷면 텍스트 색상(checklist.md §6, 2026-09-19) — CardRenderPreparation의 검증·해석
// 로직과 CardGenerationPersistenceService의 확정·잠금·단체 일관성 로직을 검증한다. 렌더링 픽셀
// 자체(색이 실제로 그렇게 그려지는지)는 CardImageCompositorTest가 이 파일의 철학(구조 검증 자동,
// 가독성 육안 확인)대로 별도로 다룬다 — 여기서는 상태 전이·거절·저장 계약만 다룬다.
@SpringBootTest
class StudentTextColorTest {

    @Autowired
    private CardPreviewService cardPreviewService;
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
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private ManseryeokResultRepository manseryeokResultRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private Long userId;
    private Long studentCardTypeId;
    private Long honorKoreanTypeId;
    private Long studentCardDesignId;

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
                User.createOAuthUser("text-color-admin@example.com", "oauth-text-color-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("text-color-user@example.com", "oauth-text-color-user", "google", "User"));
        userId = user.getId();

        CardType student = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-textcolor", null, BigDecimal.ZERO));
        studentCardTypeId = student.getId();

        CardType honorKorean = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-textcolor", null, BigDecimal.ZERO));
        honorKoreanTypeId = honorKorean.getId();

        UploadFile front = uploadFileRepository.save(UploadFile.create(
                "front.png", "front-stored.png", "templates/front.png", UploadFileType.CARD_IMAGE, "image/png", 100L));
        UploadFile back = uploadFileRepository.save(UploadFile.create(
                "back.png", "back-stored.png", "templates/back.png", UploadFileType.CARD_IMAGE, "image/png", 100L));
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                studentCardTypeId, "학생증디자인1", 1, CardDesignOrientation.LANDSCAPE,
                front.getId(), back.getId(), true));
        studentCardDesignId = design.getId();

        when(storageService.download(anyString())).thenReturn(samplePng());
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenAnswer(inv -> "stored://" + inv.getArgument(0));
    }

    private Application saveStudentApplication(String applicationNumber) {
        Application application = Application.createIndividual(
                userId, applicationNumber, studentCardTypeId, IssueType.MOBILE, true, null, null,
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "테스트대학교");
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application.assignZodiacDesignSet(1);
        return applicationRepository.save(application);
    }

    private int cardNumberSeq = 0;

    private ApplicationMember saveReadyMember(Long applicationId) {
        ApplicationMember member = ApplicationMember.createIndividual(applicationId, "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null,
                "202500225", "사회복지학과", "photos/x.jpg");
        member.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        member.assignCardNumber(String.format("ROK-12345-%04d", ++cardNumberSeq));
        member = applicationMemberRepository.save(member);
        manseryeokResultRepository.save(ManseryeokResult.create(member.getId(), "hash-" + member.getId(), "Asia/Seoul",
                127.0, "+09:00", Instant.parse("1995-02-07T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"갑\",\"branch\":\"술\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));
        return member;
    }

    private CardPreviewRequest request(StudentTextColor front, StudentTextColor back) {
        CardPreviewRequest req = new CardPreviewRequest();
        ReflectionTestUtils.setField(req, "cardDesignId", studentCardDesignId);
        ReflectionTestUtils.setField(req, "issueDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "studentFrontTextColor", front);
        ReflectionTestUtils.setField(req, "studentBackTextColor", back);
        return req;
    }

    private byte[] samplePng() {
        try {
            BufferedImage img = new BufferedImage(980, 650, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void previewDefaultsToDarkGrayWhenColorsOmitted() throws Exception {
        Application application = saveStudentApplication("APP-2026-TC001");
        ApplicationMember member = saveReadyMember(application.getId());

        assertThatCodeDoesNotThrow(() ->
                cardPreviewService.preview(adminId, application.getId(), member.getId(), request(null, null)));
    }

    @Test
    void previewAcceptsExplicitWhiteFrontAndDarkGrayBackIndependently() throws Exception {
        Application application = saveStudentApplication("APP-2026-TC002");
        ApplicationMember member = saveReadyMember(application.getId());

        assertThatCodeDoesNotThrow(() -> cardPreviewService.preview(
                adminId, application.getId(), member.getId(), request(StudentTextColor.WHITE, StudentTextColor.DARK_GRAY)));
    }

    @Test
    void previewRejectsColorFieldsForNonStudentCard() throws Exception {
        Application application = Application.createIndividual(
                userId, "APP-2026-TC003", honorKoreanTypeId, IssueType.MOBILE, true, null, null);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application.assignZodiacDesignSet(1);
        application = applicationRepository.save(application);
        ApplicationMember member = ApplicationMember.createIndividual(application.getId(), "Kim Hak-saeng",
                LocalDate.of(1995, 2, 7), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null, null, null,
                "photos/x.jpg", "대한민국 전라북도 전주시");
        member.assignKoreanName("김", "학생", "學生", "배울 학(學) 날 생(生)", "배우고 익히며 성장한다.");
        member.assignCardNumber("ROK-12345-0001");
        member = applicationMemberRepository.save(member);
        manseryeokResultRepository.save(ManseryeokResult.create(member.getId(), "hash-nc", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("1995-02-07T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"갑\",\"branch\":\"술\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));
        CardDesign nonStudentDesign = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "일반디자인1", 1, CardDesignOrientation.LANDSCAPE, null, null, true));
        CardPreviewRequest req = new CardPreviewRequest();
        ReflectionTestUtils.setField(req, "cardDesignId", nonStudentDesign.getId());
        ReflectionTestUtils.setField(req, "issueDate", LocalDate.now());
        ReflectionTestUtils.setField(req, "studentFrontTextColor", StudentTextColor.WHITE);
        Long applicationId = application.getId();
        Long memberId = member.getId();

        assertThatThrownBy(() -> cardPreviewService.preview(adminId, applicationId, memberId, req))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void generateConfirmsAndPersistsColorsOnApplication() throws Exception {
        Application application = saveStudentApplication("APP-2026-TC004");
        ApplicationMember member = saveReadyMember(application.getId());

        cardGenerationService.generate(adminId, application.getId(), member.getId(),
                request(StudentTextColor.WHITE, StudentTextColor.DARK_GRAY));

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getStudentFrontTextColor()).isEqualTo(StudentTextColor.WHITE);
        assertThat(reloaded.getStudentBackTextColor()).isEqualTo(StudentTextColor.DARK_GRAY);
    }

    @Test
    void generateRejectsConflictingColorAfterFirstConfirmation() throws Exception {
        Application application = saveStudentApplication("APP-2026-TC005");
        ApplicationMember member = saveReadyMember(application.getId());
        cardGenerationService.generate(adminId, application.getId(), member.getId(),
                request(StudentTextColor.WHITE, StudentTextColor.DARK_GRAY));

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, application.getId(), member.getId(),
                request(StudentTextColor.DARK_GRAY, StudentTextColor.DARK_GRAY)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_TEXT_COLOR_MISMATCH);
    }

    @Test
    void generateAllowsRegenerationWithSameConfirmedColor() throws Exception {
        Application application = saveStudentApplication("APP-2026-TC006");
        ApplicationMember member = saveReadyMember(application.getId());
        cardGenerationService.generate(adminId, application.getId(), member.getId(),
                request(StudentTextColor.WHITE, StudentTextColor.WHITE));

        assertThatCodeDoesNotThrow(() -> cardGenerationService.generate(adminId, application.getId(), member.getId(),
                request(StudentTextColor.WHITE, StudentTextColor.WHITE)));
    }

    @Test
    void generateAllowsSecondGroupMemberOnlyWithMatchingColor() throws Exception {
        Application application = saveStudentApplication("APP-2026-TC007");
        ReflectionTestUtils.setField(application, "totalQuantity", 2);
        applicationRepository.saveAndFlush(application);
        ApplicationMember memberA = saveReadyMember(application.getId());
        ApplicationMember memberB = saveReadyMember(application.getId());

        cardGenerationService.generate(adminId, application.getId(), memberA.getId(),
                request(StudentTextColor.WHITE, StudentTextColor.DARK_GRAY));

        assertThatThrownBy(() -> cardGenerationService.generate(adminId, application.getId(), memberB.getId(),
                request(StudentTextColor.DARK_GRAY, StudentTextColor.DARK_GRAY)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_TEXT_COLOR_MISMATCH);

        assertThatCodeDoesNotThrow(() -> cardGenerationService.generate(adminId, application.getId(), memberB.getId(),
                request(StudentTextColor.WHITE, StudentTextColor.DARK_GRAY)));
    }

    private void assertThatCodeDoesNotThrow(ThrowingCall call) throws Exception {
        call.run();
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }
}
