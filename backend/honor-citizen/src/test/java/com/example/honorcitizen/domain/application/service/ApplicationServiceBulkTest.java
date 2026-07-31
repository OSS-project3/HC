package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class ApplicationServiceBulkTest {

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
    private ObjectMapper objectMapper;

    @MockitoBean
    private StorageService storageService;

    private User user;
    private CardType honorKoreanCardType;
    private CardType studentCardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.createNewUser("group@example.com", "oauth-group", "google", "Group"));
        honorKoreanCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-bulk", null, BigDecimal.valueOf(30000)));
        studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-bulk", null, BigDecimal.valueOf(20000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenReturn("http://mock-storage/uploaded");
    }

    private BulkApplicationCreateRequest request(Long cardTypeId) {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "organizationName": "OO기업", "department": "인사팀", "name": "홍길동", "phone": "010-1234-5678" }
                }
                """.formatted(cardTypeId);
        try {
            return objectMapper.readValue(json, BulkApplicationCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] buildExcel(boolean isStudent, String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
            Row commonRow = sheet.createRow(0);
            commonRow.createCell(0).setCellValue("공통 입국날짜");
            commonRow.createCell(1).setCellValue("2026-08-15");

            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("ID");

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

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] buildZip(byte[] excelBytes, String... photoIds) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("members.xlsx"));
            zip.write(excelBytes);
            zip.closeEntry();

            for (String photoId : photoIds) {
                zip.putNextEntry(new ZipEntry("photos/" + photoId + ".jpg"));
                zip.write(("photo-" + photoId).getBytes());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    // 컬럼 순서: ID|영문명|생년월일|국적|출생시간|출생지역|성별|개별입국날짜|이메일|전화번호|주소|학번|학과
    private static final String ROW_1 = "1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul";
    private static final String ROW_2 = "2|Mike Kim|1992-03-03|US|||MALE|2026-08-18|mike@example.com|010-3333-4444|Busan";

    @Test
    void createGroupSavesApplicationWithMemberPerRowAndResolvesEntryDate() throws Exception {
        byte[] excel = buildExcel(false, ROW_1, ROW_2);
        byte[] zip = buildZip(excel, "1", "2");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        BulkApplicationCreateResponse response = applicationService.createGroup(
                user.getId(), request(honorKoreanCardType.getId()), logo, seal, submitFile);

        Application application = applicationRepository.findById(response.getApplicationId()).orElseThrow();
        assertThat(application.getApplicationType().name()).isEqualTo("GROUP");
        assertThat(application.getTotalQuantity()).isEqualTo(2);
        assertThat(application.getLogoFileId()).isNotNull();
        assertThat(application.getSealFileId()).isNotNull();
        assertThat(application.getSubmitFileId()).isNotNull();

        Applicant applicant = applicantRepository.findByApplicationId(application.getId()).orElseThrow();
        assertThat(applicant.getEmail()).isEqualTo("group@example.com");
        assertThat(applicant.getOrganizationName()).isEqualTo("OO기업");

        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(application.getId());
        assertThat(members).hasSize(2);

        ApplicationMember john = members.stream().filter(m -> "John Doe".equals(m.getEnglishName())).findFirst().orElseThrow();
        assertThat(john.getEntryDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 15));
        assertThat(john.getEmail()).isEqualTo("john@example.com");
        assertThat(john.getPhone()).isEqualTo("010-1111-2222");

        ApplicationMember mike = members.stream().filter(m -> "Mike Kim".equals(m.getEnglishName())).findFirst().orElseThrow();
        assertThat(mike.getEntryDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 18));
    }

    @Test
    void createGroupRejectsWholeBatchWhenAnyRowMissingRequiredField() throws Exception {
        String invalidRow = "3||1988-01-01|US|||MALE||missing-name@example.com|010-0000-0000|Seoul";
        byte[] excel = buildExcel(false, ROW_1, invalidRow);
        byte[] zip = buildZip(excel, "1", "3");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        assertThatThrownBy(() -> applicationService.createGroup(
                user.getId(), request(honorKoreanCardType.getId()), logo, seal, submitFile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_PARSE_ERROR);

        assertThat(applicationRepository.count()).isZero();
    }

    @Test
    void createGroupRejectsWholeBatchWhenPhotoMissingForRow() throws Exception {
        byte[] excel = buildExcel(false, ROW_1);
        byte[] zip = buildZip(excel); // no photos at all
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        assertThatThrownBy(() -> applicationService.createGroup(
                user.getId(), request(honorKoreanCardType.getId()), logo, seal, submitFile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_PARSE_ERROR);
    }

    @Test
    void createGroupRequiresStudentIdAndDepartmentForStudentCardType() throws Exception {
        byte[] excel = buildExcel(true, ROW_1); // missing studentId/department columns
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        assertThatThrownBy(() -> applicationService.createGroup(
                user.getId(), request(studentCardType.getId()), logo, seal, submitFile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_PARSE_ERROR);
    }

    @Test
    void createGroupSucceedsForStudentCardWithStudentIdAndDepartment() throws Exception {
        String studentRow = "1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul|20261234|컴퓨터공학과";
        byte[] excel = buildExcel(true, studentRow);
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        BulkApplicationCreateResponse response = applicationService.createGroup(
                user.getId(), request(studentCardType.getId()), logo, seal, submitFile);

        ApplicationMember member = applicationMemberRepository.findByApplicationId(response.getApplicationId()).get(0);
        assertThat(member.getStudentId()).isEqualTo("20261234");
        assertThat(member.getDepartment()).isEqualTo("컴퓨터공학과");
    }

    @Test
    void createGroupThrowsInvalidZipForCorruptFile() {
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", "not-a-zip".getBytes());
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        assertThatThrownBy(() -> applicationService.createGroup(
                user.getId(), request(honorKoreanCardType.getId()), logo, seal, submitFile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_NOT_FOUND);
    }

    @Test
    void createGroupRequiresLogoAndSeal() throws Exception {
        byte[] excel = buildExcel(false, ROW_1);
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);

        assertThatThrownBy(() -> applicationService.createGroup(
                user.getId(), request(honorKoreanCardType.getId()), null, null, submitFile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
