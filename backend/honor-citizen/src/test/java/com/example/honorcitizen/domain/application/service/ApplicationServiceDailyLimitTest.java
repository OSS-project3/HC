package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 일일 신청 생성 3회 제한(APPLICATION.md §7) — 개인/단체 합산 카운트를 실제 createIndividual/createGroup
// 경로로 검증한다. 동시성 안전성 자체는 ApplicationDailyLimitServiceTest에서 별도로 검증한다.
@SpringBootTest
class ApplicationServiceDailyLimitTest {

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
    private ApplicationDailyLimitRepository applicationDailyLimitRepository;
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

    @BeforeEach
    void setUp() {
        applicationDailyLimitRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User newUser = User.createNewUser("daily-limit@example.com", "oauth-daily-limit", "google", "Member");
        newUser.agreeTerms(true, true, true);
        user = userRepository.save(newUser);
        honorKoreanCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-일일제한", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenReturn("http://mock-storage/uploaded");
    }

    private ApplicationCreateRequest individualRequest() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "member": {
                    "englishName": "Hong Gildong",
                    "birthDate": "1990-05-15",
                    "nationality": "US",
                    "gender": "MALE"
                  }
                }
                """.formatted(honorKoreanCardType.getId());
        return parseIndividual(json);
    }

    private ApplicationCreateRequest parseIndividual(String json) {
        try {
            return objectMapper.readValue(json, ApplicationCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BulkApplicationCreateRequest groupRequest() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "applicant": { "organizationName": "OO기업", "name": "홍길동", "phone": "010-1234-5678" }
                }
                """.formatted(honorKoreanCardType.getId());
        try {
            return objectMapper.readValue(json, BulkApplicationCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] imageBytes() {
        try {
            BufferedImage image = new BufferedImage(300, 400, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes());
    }

    private byte[] buildExcel(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
            Row commonRow = sheet.createRow(0);
            commonRow.createCell(0).setCellValue("공통 입국날짜");
            commonRow.createCell(1).setCellValue("2026-08-15");
            sheet.createRow(2).createCell(0).setCellValue("ID");
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
                zip.putNextEntry(new ZipEntry(photoId + ".jpg"));
                zip.write(("photo-" + photoId).getBytes());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private void createGroupApplication() throws Exception {
        byte[] excel = buildExcel("1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul");
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        applicationService.createGroup(user.getId(), groupRequest(), logo, seal, submitFile);
    }

    @Test
    void fourthIndividualApplicationSameDayIsRejected() {
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_LIMIT_EXCEEDED);

        assertThat(applicationRepository.count()).isEqualTo(3);
    }

    @Test
    void individualAndGroupApplicationsShareTheSameDailyLimit() throws Exception {
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
        createGroupApplication();

        assertThatThrownBy(() -> {
            applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
            applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
        }).isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_LIMIT_EXCEEDED);

        // 개인 1건 + 단체 1건 + 개인 1건(성공) = 3건, 그다음 한 건은 거절되어 총 3건만 저장돼야 한다.
        assertThat(applicationRepository.count()).isEqualTo(3);
    }

    @Test
    void otherUserIsNotAffectedByFirstUsersLimit() {
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);
        applicationService.createIndividual(user.getId(), individualRequest(), photo(), null, null);

        User anotherUser = userRepository.save(
                User.createNewUser("daily-limit-2@example.com", "oauth-daily-limit-2", "google", "Member2"));
        anotherUser.agreeTerms(true, true, true);
        userRepository.save(anotherUser);

        applicationService.createIndividual(anotherUser.getId(), individualRequest(), photo(), null, null);

        assertThat(applicationRepository.count()).isEqualTo(4);
    }
}
