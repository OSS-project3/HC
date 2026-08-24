package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.storage.StorageService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationBulkControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicantRepository applicantRepository;
    @Autowired
    private ReceiverRepository receiverRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;

    @MockitoBean
    private StorageService storageService;

    private String token;
    private CardType cardType;

    private static final String REQUEST_JSON = """
            {
              "cardTypeId": %d,
              "issueType": "MOBILE",
              "applicant": { "organizationName": "OO기업", "department": "인사팀", "name": "홍길동", "phone": "010-1234-5678" }
            }
            """;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.createOAuthUser("bulk-ctrl@example.com", "oauth-bulk-ctrl", "google", "Bulk");
        // createGroup()이 findUser()(약관 동의 필수)를 거치므로 기본 픽스처 사용자도 동의 상태여야 한다.
        user.agreeTerms(true, true, true);
        user = userRepository.save(user);
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-bulk-ctrl", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenReturn("http://mock-storage/uploaded");
    }

    private byte[] buildZip() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("ID");

            Row dataRow = sheet.createRow(3);
            String[] values = {"1", "John Doe", "1988-01-01", "US", "", "Chicago", "MALE", "", "john@example.com", "010-1111-2222", "Seoul"};
            for (int i = 0; i < values.length; i++) {
                if (!values[i].isEmpty()) {
                    dataRow.createCell(i).setCellValue(values[i]);
                }
            }

            ByteArrayOutputStream excelOut = new ByteArrayOutputStream();
            workbook.write(excelOut);

            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(zipOut)) {
                zip.putNextEntry(new ZipEntry("members.xlsx"));
                zip.write(excelOut.toByteArray());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("1.jpg"));
                zip.write("photo-1".getBytes());
                zip.closeEntry();
            }
            return zipOut.toByteArray();
        }
    }

    @Test
    void createGroupReturnsCreatedWithTotalQuantity() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", buildZip());

        mockMvc.perform(multipart("/api/applications/bulk")
                        .file(requestPart)
                        .file(logo)
                        .file(seal)
                        .file(submitFile)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(1))
                .andExpect(jsonPath("$.data.applicationNumber").value(org.hamcrest.Matchers.startsWith("APP-")));
    }

    @Test
    void createGroupReturnsUnauthorizedWithoutToken() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", buildZip());

        mockMvc.perform(multipart("/api/applications/bulk")
                        .file(requestPart)
                        .file(logo)
                        .file(seal)
                        .file(submitFile))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGroupReturnsInvalidInputWhenReceiverZipCodeMissing() throws Exception {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE_AND_PHYSICAL",
                  "applicant": { "organizationName": "OO기업", "department": "인사팀", "name": "홍길동", "phone": "010-1234-5678" },
                  "receiver": { "sameAsApplicant": false, "name": "김수령", "phone": "010-9999-8888", "address": "서울특별시 강남구" }
                }
                """.formatted(cardType.getId());
        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", json.getBytes());
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", buildZip());

        mockMvc.perform(multipart("/api/applications/bulk")
                        .file(requestPart)
                        .file(logo)
                        .file(seal)
                        .file(submitFile)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void createGroupReturnsExcelNotFoundWhenZipHasNoExcel() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", "not-a-zip".getBytes());

        mockMvc.perform(multipart("/api/applications/bulk")
                        .file(requestPart)
                        .file(logo)
                        .file(seal)
                        .file(submitFile)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BULK_APPLICATION_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].code").value("EXCEL_NOT_FOUND"));
    }
}
