package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.TimeAccuracy;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.card.dto.CardPreviewResponse;
import com.example.honorcitizen.domain.card.dto.SchoolCardTemplateResponse;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.card.service.CardPreviewService;
import com.example.honorcitizen.domain.card.service.SchoolCardTemplateService;
import com.example.honorcitizen.domain.manseryeok.entity.ManseryeokResult;
import com.example.honorcitizen.domain.manseryeok.repository.ManseryeokResultRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 사용자 요청(2026-09-05): "엑셀 값 실제로 넣어서 신청된게 이후 관리자 화면 렌더링에도 잘 반영되는지
// 일단 한 값만 넣어서 확인해줘" — 단체 신청 ZIP(엑셀 1행 실값)을 실제 BulkExcelParser로 파싱해
// ApplicationService.createGroup()으로 접수하고, 그 신청 건을 그대로 실제 CardPreviewService.preview()
// 까지 태워 렌더링 결과에 엑셀에서 넣은 값(영문명·학번·학과)이 실제로 반영되는지 확인한다.
// ApplicationServiceBulkTest(엑셀 파싱 검증)와 SchoolCardTemplateEndToEndTest(4-D 업로드→렌더링 검증)
// 둘을 잇는 구간 — "엑셀 → DB → 카드 렌더링" 전체 경로를 한 번에 태우는 게 이 테스트의 목적이다.
@SpringBootTest
class BulkExcelToCardRenderingEndToEndTest {

    private static final String SAJU_ASSET_ROOT = "D:/HC-worktrees/saju/\uc2dc\uc548/\uc2dc\uc548/\ud559\uc0dd\uc99d/";
    private static final String OUT_DIR = "C:/TEMPFO~1/claude/d--HC-worktrees/c4a01a9d-4c65-474a-b64a-e0d1ec48f9d4/scratchpad/bulk-excel-render-out/";

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ManseryeokResultRepository manseryeokResultRepository;
    @Autowired
    private SchoolCardTemplateService schoolCardTemplateService;
    @Autowired
    private CardPreviewService cardPreviewService;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StorageService storageService;

    private final Map<String, byte[]> fakeS3 = new HashMap<>();

    private User applicant;
    private Long adminId;
    private Long studentCardTypeId;
    private Long schoolId;

    @BeforeEach
    void setUp() {
        manseryeokResultRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        schoolRepository.deleteAll();
        userRepository.deleteAll();
        fakeS3.clear();
        new File(OUT_DIR).mkdirs();

        applicant = User.createOAuthUser("bulk-e2e-user@example.com", "oauth-bulk-e2e-user", "google", "신청자");
        applicant.agreeTerms(true, true, true);
        applicant = userRepository.save(applicant);

        User admin = userRepository.save(User.createOAuthUser("bulk-e2e-admin@example.com", "oauth-bulk-e2e-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        studentCardTypeId = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-bulk-e2e", null, BigDecimal.ZERO)).getId();
        schoolId = schoolRepository.save(School.create("한세종합대학교", SchoolType.UNIVERSITY)).getId();

        // upload()/uploadBytes()가 만든 key로 저장한 바이트를 download()가 그대로 돌려주게 연결한다(가짜 S3) —
        // logo/seal/제출 zip/멤버 사진/4-D 템플릿까지 이 테스트 안의 모든 업로드가 같은 가짜 저장소를 공유한다.
        when(storageService.upload(anyString(), any())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            org.springframework.web.multipart.MultipartFile file = inv.getArgument(1);
            fakeS3.put(key, file.getBytes());
            return "stored://" + key;
        });
        when(storageService.uploadBytes(anyString(), any(byte[].class), anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            fakeS3.put(key, (byte[]) inv.getArgument(1));
            return "stored://" + key;
        });
        when(storageService.download(anyString())).thenAnswer(inv -> fakeS3.get((String) inv.getArgument(0)));
        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://presigned.example/x");
    }

    @Test
    void bulkExcelRowFlowsThroughToCardPreviewRendering() throws Exception {
        // 1. 엑셀 1행에 실제 값을 채워 ZIP으로 만든다 — 실제 사용자가 올릴 파일과 동일한 구조.
        // 컬럼: 사진번호|영문명|생년월일|국적|출생시간|출생지역|성별|개별입국날짜|이메일|전화번호|주소|학번|학과
        // 학생증은 카드에 주소를 표시하지 않으므로 주소 칸은 비운다(2026-09-13 정책 통일).
        byte[] excel = buildExcel(
                "1|Kim Testperson|2003-05-12|KR||Seoul|MALE||kim.testperson@example.com|010-1234-5678||202512345|컴퓨터공학과");
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        // 2. 실제 ApplicationController가 받는 것과 동일한 요청 — schoolId를 주면 resolveSchool()이
        // School 엔티티의 실제 schoolType(UNIVERSITY)을 그대로 쓰므로 request에 schoolType은 안 넣어도 된다.
        BulkApplicationCreateRequest request = request(studentCardTypeId, Orientation.LANDSCAPE, schoolId);

        // 3. 실제 ApplicationService.createGroup() — 내부에서 실제 BulkExcelParser가 엑셀을 파싱한다.
        BulkApplicationCreateResponse response = applicationService.createGroup(
                applicant.getId(), request, logo, seal, submitFile);

        // 4. 엑셀 값이 실제로 DB에 반영됐는지 자동 검증(파싱·저장 경로).
        Application application = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(application.getId());
        assertThat(members).hasSize(1);
        ApplicationMember member = members.get(0);
        assertThat(member.getEnglishName()).isEqualTo("Kim Testperson");
        assertThat(member.getBirthDate()).isEqualTo(LocalDate.of(2003, 5, 12));
        assertThat(member.getStudentId()).isEqualTo("202512345");
        assertThat(member.getDepartment()).isEqualTo("컴퓨터공학과");

        // 5. 관리자 화면(카드 렌더링)까지 가려면 필요한 나머지 선행 데이터를 채운다 — 실제 운영에서는
        // 관리자가 검토·만세력 확정·작명 단계를 거치며 채우는 값들이다(SchoolCardTemplateEndToEndTest와 동일 패턴).
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.PRODUCTION_READY);
        application.assignZodiacDesignSet(1);
        applicationRepository.save(application);

        manseryeokResultRepository.save(ManseryeokResult.create(member.getId(), "hash-bulk-e2e", "Asia/Seoul", 127.0,
                "+09:00", Instant.parse("2003-05-12T01:00:00Z"), TimeAccuracy.EXACT,
                "{\"year\":{\"stem\":\"\uacc4\",\"branch\":\"\ubbf8\"}}", "[]", "{}",
                "2026b", "test-v1", LocalDateTime.now(), adminId));

        // \uc774\ub984\uc740 \uc131\uc528 \uc81c\uc678 \ud55c\uae00 2~3\uc790, \ud55c\uc790\uac00 \uc788\uc73c\uba74 \uc774\ub984\uacfc \uac19\uc740 \uae00\uc790 \uc218\uc5ec\uc57c \ud55c\ub2e4(ApplicationMember.validateNameFormat).
        member.assignKoreanName("\uae40", "\ud61c\ube48",
                "\u6167\u5f6c", "\uc9c0\ud61c\ub85c\uc6b8 \ud61c(\u6167) \ube5b\ub0a0 \ube48(\u5f6c)", "\uc9c0\ud61c\ub86d\uac8c \ube5b\ub098\ub294 \uc0b6\uc744 \uc0b0\ub2e4.");
        member.assignCardNumber("ROK-99999-0002");
        applicationMemberRepository.save(member);

        // 6. 4-D 실제 업로드 API로 실제 디자이너 템플릿을 이 학교(schoolId)·orientation에 등록한다.
        MockMultipartFile front = realPng("front", "\uc544\ud2b8\ubcf4\ub4dc 8 \uc0ac\ubcf8 13.png");
        MockMultipartFile back = realPng("back", "\uc544\ud2b8\ubcf4\ub4dc 8 \uc0ac\ubcf8 15.png");
        SchoolCardTemplateResponse templateResult = schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, front, back);

        // 7. 실제 CardPreviewService.preview() — 엑셀에서 들어온 값이 반영된 이 member로 실제 렌더링.
        CardPreviewRequest previewRequest = new CardPreviewRequest();
        ReflectionTestUtils.setField(previewRequest, "cardDesignId", templateResult.cardDesignId());
        ReflectionTestUtils.setField(previewRequest, "issueDate", LocalDate.now());

        CardPreviewResponse preview = cardPreviewService.preview(adminId, application.getId(), member.getId(), previewRequest);

        byte[] frontPng = Base64.getDecoder().decode(preview.front());
        byte[] backPng = Base64.getDecoder().decode(preview.back());
        assertThat(ImageIO.read(new ByteArrayInputStream(frontPng))).isNotNull();
        assertThat(ImageIO.read(new ByteArrayInputStream(backPng))).isNotNull();

        // 자동화로는 "학번/학과 글자가 카드 위 올바른 위치에 정확히 보이는지"까지는 판정할 수 없으므로
        // 이 프로젝트 관행대로(HANDOFF.md) 구조 검증은 자동, 가독성은 렌더링 결과를 파일로 남겨 육안 확인한다.
        writeBytes(OUT_DIR + "bulk-e2e-front.png", frontPng);
        writeBytes(OUT_DIR + "bulk-e2e-back.png", backPng);
    }

    private BulkApplicationCreateRequest request(Long cardTypeId, Orientation orientation, Long schoolId) throws Exception {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "orientation": "%s",
                  "schoolId": %d,
                  "applicant": { "organizationName": "\ud55c\uc138\uc885\ud569\ub300\ud559\uad50", "department": "\ud559\uc0dd\ud68c", "name": "\ub2e8\uc7a5", "phone": "010-1234-5678" }
                }
                """.formatted(cardTypeId, orientation, schoolId);
        return objectMapper.readValue(json, BulkApplicationCreateRequest.class);
    }

    // 컬럼 순서: 사진번호|영문명|생년월일|국적|출생시간|출생지역|성별|개별입국날짜|이메일|전화번호|주소|학번|학과
    // (ApplicationServiceBulkTest.buildExcel와 동일한 형식 — 파이프로 구분한 문자열 1개 = 엑셀 1행)
    private byte[] buildExcel(String rowCsv) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
            Row commonRow = sheet.createRow(0);
            commonRow.createCell(0).setCellValue("\uacf5\ud1b5 \uc785\uad6d\ub0a0\uc9dc");

            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("ID");

            String[] cols = rowCsv.split("\\|", -1);
            Row row = sheet.createRow(3);
            for (int i = 0; i < cols.length; i++) {
                if (!cols[i].isEmpty()) {
                    row.createCell(i).setCellValue(cols[i]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildZip(byte[] excelBytes, String photoId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("members.xlsx"));
            zip.write(excelBytes);
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry(photoId + ".png"));
            zip.write(fakePhotoPng());
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    // 실제 디코딩 가능한 PNG가 필요하다(사진 검증·카드 합성이 진짜 이미지를 읽으므로) — 단색 사각형이면 충분.
    private byte[] fakePhotoPng() throws Exception {
        BufferedImage image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 300, 400);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private MockMultipartFile realPng(String part, String assetFileName) throws Exception {
        // 디자이너 원본 자산은 별도 saju 리포에만 있다 — 없는 머신에서는 실패 대신 건너뛴다.
        Assumptions.assumeTrue(new File(SAJU_ASSET_ROOT + assetFileName).exists(),
                "디자이너 학생증 자산 없음: " + SAJU_ASSET_ROOT + assetFileName);
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
