package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 신청 엑셀 내보내기(DESIGN.md §2.4) — 빌더 자체 로직은 ApplicationExportExcelBuilderTest에서 이미
// 커버하므로, 여기서는 Service의 조회·검증·조합(어떤 Application/Applicant/UploadFile을 골라 빌더에
// 넘기는지)만 검증한다.
@SpringBootTest
class ApplicationServiceExportTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private CardType cardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        uploadFileRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("export-admin@example.com", "oauth-export-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-export", null, BigDecimal.valueOf(30000)));
    }

    private Long saveOwner(String suffix) {
        return userRepository.save(
                User.createOAuthUser("export-owner-" + suffix + "@example.com", "oauth-export-owner-" + suffix,
                        "google", "Owner")).getId();
    }

    @Test
    void individualExportBuildsOneRowPerApplicationWithApplicantEmailPhone() throws Exception {
        Long ownerId = saveOwner("1");
        Application app = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-950001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                app.getId(), "홍길동", "hong@example.com", "010-1111-2222"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                app.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "KR", null, "Seoul",
                Gender.MALE, null, null, null, "photos/a.jpg"));

        byte[] bytes = applicationService.exportExcel(adminId, List.of(app.getId()), ApplicationType.INDIVIDUAL);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(3);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Hong Gildong");
            assertThat(row.getCell(8).getStringCellValue()).isEqualTo("hong@example.com");
            assertThat(row.getCell(9).getStringCellValue()).isEqualTo("010-1111-2222");
        }
    }

    @Test
    void groupExportRejectsMultipleApplicationIds() {
        assertThatThrownBy(() -> applicationService.exportExcel(adminId, List.of(1L, 2L), ApplicationType.GROUP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void groupExportAppendsConfirmedNamesFromOriginalFile() throws Exception {
        Long ownerId = saveOwner("2");
        byte[] zip = buildGroupOriginalZip(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222|Seoul");
        UploadFile uploadFile = uploadFileRepository.save(UploadFile.create(
                "members.zip", "stored-members.zip", "group/stored-members.zip",
                UploadFileType.ZIP, "application/zip", zip.length));
        when(storageService.download(anyString())).thenReturn(zip);

        Application app = applicationRepository.save(Application.createGroup(
                ownerId, "APP-2026-950002", cardType.getId(), IssueType.MOBILE, false, 1,
                null, null, uploadFile.getId()));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                app.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US", null, "Chicago",
                Gender.MALE, null, "john@example.com", "010-1111-2222", "Seoul", null, null, null));
        member.assignKoreanName("지호", "智毫");
        applicationMemberRepository.saveAndFlush(member);

        byte[] bytes = applicationService.exportExcel(adminId, List.of(app.getId()), ApplicationType.GROUP);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(3);
            assertThat(row.getCell(11).getStringCellValue()).isEqualTo("지호");
            assertThat(row.getCell(12).getStringCellValue()).isEqualTo("智毫");
        }
    }

    @Test
    void rejectsForNonAdminCaller() throws Exception {
        Long ownerId = saveOwner("3");
        User user = userRepository.save(
                User.createOAuthUser("export-plain-user@example.com", "oauth-export-plain", "google", "User"));
        Application app = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-950003", cardType.getId(), IssueType.MOBILE, true, null, null));

        assertThatThrownBy(() -> applicationService.exportExcel(user.getId(), List.of(app.getId()), ApplicationType.INDIVIDUAL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsEmptyApplicationIds() {
        assertThatThrownBy(() -> applicationService.exportExcel(adminId, List.of(), ApplicationType.INDIVIDUAL))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    private static final String[] GROUP_HEADERS = {
            "사진 번호", "영문명", "생년월일", "출생국가", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소",
    };

    private byte[] buildGroupOriginalZip(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(0).createCell(0).setCellValue("공통 입국날짜");
            sheet.createRow(1).createCell(0).setCellValue("1.1");
            Row header = sheet.createRow(2);
            for (int i = 0; i < GROUP_HEADERS.length; i++) {
                header.createCell(i).setCellValue(GROUP_HEADERS[i]);
            }
            int rowIndex = 3;
            for (String rowCsv : rows) {
                String[] cols = rowCsv.split("\\|", -1);
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < cols.length; i++) {
                    if (!cols[i].isEmpty()) {
                        row.createCell(i).setCellValue(cols[i]);
                    }
                }
            }
            ByteArrayOutputStream xlsxOut = new ByteArrayOutputStream();
            workbook.write(xlsxOut);

            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
                zos.putNextEntry(new ZipEntry("members.xlsx"));
                zos.write(xlsxOut.toByteArray());
                zos.closeEntry();
            }
            return zipOut.toByteArray();
        }
    }
}
