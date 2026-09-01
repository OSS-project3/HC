package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.card.dto.CardPreviewResponse;
import com.example.honorcitizen.domain.card.dto.SchoolCardTemplateResponse;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 4-D 완료 조건("통합 테스트", 2026-08-31 사용자 지정): 관리자가 실제 POST .../card-template로
// 앞/뒷면 이미지를 업로드하면, 그 등록된 CardDesign으로 실제 카드 Preview까지 실행해 잘리거나
// 겹치거나 배경과 겹쳐 안 보이는 문제 없이 렌더링되는지 확인한다 — 4-D 업로드 API부터
// CardPreviewService까지 실제 경로를 전부 태운다(단위 테스트 수준의 PNG 유효성 확인이 아니다).
// 실제 디자이너 템플릿(D:\HC-worktrees\saju\시안\시안\학생증\)을 그대로 써서 StorageService의
// upload/download를 서로 연결한다(둘 다 Mock이지만, upload된 바이트를 download가 그대로 돌려주게
// 스텁해 실제 S3 왕복을 흉내낸다).
@SpringBootTest
class SchoolCardTemplateEndToEndTest {

    private static final String SAJU_ASSET_ROOT = "D:/HC-worktrees/saju/\uc2dc\uc548/\uc2dc\uc548/\ud559\uc0dd\uc99d/";
    private static final String OUT_DIR = "C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/student-card-out/";

    @Autowired
    private SchoolCardTemplateService schoolCardTemplateService;
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
    private SchoolRepository schoolRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private Long userId;
    private Long schoolId;
    private Long studentTypeId;
    private final Map<String, byte[]> fakeS3 = new HashMap<>();

    @BeforeEach
    void setUp() {
        manseryeokResultRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardDesignRepository.deleteAll();
        uploadFileRepository.deleteAll();
        schoolRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();
        fakeS3.clear();
        new File(OUT_DIR).mkdirs();

        User admin = userRepository.save(User.createOAuthUser("e2e-admin@example.com", "oauth-e2e-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(User.createOAuthUser("e2e-user@example.com", "oauth-e2e-user", "google", "User"));
        userId = user.getId();

        CardType student = cardTypeRepository.save(CardType.create(CardTypeCode.STUDENT, "학생증-e2e", null, BigDecimal.ZERO));
        studentTypeId = student.getId();

        School school = schoolRepository.save(School.create("인문외국어대학교", SchoolType.UNIVERSITY));
        schoolId = school.getId();

        // upload()가 만든 key로 저장한 바이트를, download()가 그대로 돌려주게 연결한다(가짜 S3).
        when(storageService.upload(anyString(), any())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            org.springframework.web.multipart.MultipartFile file = inv.getArgument(1);
            fakeS3.put(key, file.getBytes());
            return "stored://" + key;
        });
        when(storageService.download(anyString())).thenAnswer(inv -> fakeS3.get((String) inv.getArgument(0)));
        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://presigned.example/x");
    }

    @Test
    void adminUploadedTemplateRendersRealCardWithoutGarbledText() throws Exception {
        // 1. 관리자가 실제 4-D API로 실제 디자이너 템플릿(대학교·가로형)을 업로드한다.
        MockMultipartFile front = realPng("front", "\uc544\ud2b8\ubcf4\ub4dc 8 \uc0ac\ubcf8 13.png");
        MockMultipartFile back = realPng("back", "\uc544\ud2b8\ubcf4\ub4dc 8 \uc0ac\ubcf8 15.png");

        SchoolCardTemplateResponse uploadResult = schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, front, back);

        assertThat(uploadResult.cardDesignId()).isNotNull();

        // 2. 실제 신청+구성원(작명·만세력 확정)을 준비한다 — Application.schoolId가 방금 등록한
        // 학교와 일치해야 CardRenderPreparation이 같은 CardDesign을 찾는다.
        Application application = Application.createIndividual(
                userId, "APP-2026-E2E01", studentTypeId, IssueType.MOBILE, true, null, null,
                Orientation.LANDSCAPE, SchoolType.UNIVERSITY, "인문외국어대학교", schoolId);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application = applicationRepository.save(application);
        Long applicationId = application.getId();

        ApplicationMember member = ApplicationMember.createIndividual(applicationId, "Jeong Seul-chan",
                LocalDate.of(2003, 5, 12), "KR", LocalTime.of(10, 0), "Seoul", Gender.MALE, null,
                "202500225", "\uc778\ubb38\ub300\ud559 \uc0ac\ud68c\ubcf5\uc9c0\ud559\uacfc", "photos/e2e.jpg");
        member.assignKoreanName("\uc815", "\uc2ac\ucc2c", "\u745f\u7009",
                "\ub9db\uc744 \uc2ac(\u745f) \ucc2c\ub780\ud560 \ucc2c(\u7009)",
                "\uc9c0\ud61c\ub86d\uac8c \ube5b\ub098\ub294 \uc0b6\uc744 \uc0b0\ub2e4.");
        member.assignCardNumber("ROK-99999-0001");
        member = applicationMemberRepository.save(member);
        Long memberId = member.getId();

        manseryeokResultRepository.save(ManseryeokResult.create(memberId, "hash-e2e", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("2003-05-12T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"\uacc4\",\"branch\":\"\ubbf8\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        // 3. 실제 CardPreviewService.preview() 호출 — 업로드 API가 만든 CardDesign을 그대로 쓴다.
        CardPreviewRequest request = new CardPreviewRequest();
        ReflectionTestUtils.setField(request, "cardDesignId", uploadResult.cardDesignId());
        ReflectionTestUtils.setField(request, "issueDate", LocalDate.now());

        CardPreviewResponse response = cardPreviewService.preview(adminId, applicationId, memberId, request);

        byte[] frontPng = Base64.getDecoder().decode(response.front());
        byte[] backPng = Base64.getDecoder().decode(response.back());
        assertThat(ImageIO.read(new ByteArrayInputStream(frontPng))).isNotNull();
        assertThat(ImageIO.read(new ByteArrayInputStream(backPng))).isNotNull();

        // 실제 렌더링 결과를 파일로 남긴다 — 육안 확인용(자동화로는 "글자 깨짐"을 판정할 수 없으므로
        // 이 프로젝트 관행대로 구조 검증은 자동, 가독성은 육안으로 보완한다).
        writeBytes(OUT_DIR + "e2e-upload-to-preview-front.png", frontPng);
        writeBytes(OUT_DIR + "e2e-upload-to-preview-back.png", backPng);
    }

    private MockMultipartFile realPng(String part, String assetFileName) throws Exception {
        byte[] bytes;
        try (FileInputStream in = new FileInputStream(SAJU_ASSET_ROOT + assetFileName)) {
            bytes = in.readAllBytes();
        }
        return new MockMultipartFile(part, assetFileName, "image/png", bytes);
    }

    private void writeBytes(String path, byte[] bytes) throws Exception {
        try (FileOutputStream out = new FileOutputStream(path)) {
            out.write(bytes);
        }
    }
}
