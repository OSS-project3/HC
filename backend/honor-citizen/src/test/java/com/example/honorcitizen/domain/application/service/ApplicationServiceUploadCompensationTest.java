package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.domain.application.dto.ApplicationCreateRequest;
import com.example.honorcitizen.domain.application.dto.BulkApplicationCreateRequest;
import com.example.honorcitizen.domain.application.repository.ApplicationDailyLimitRepository;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ApplicationServiceUploadCompensationTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationDailyLimitRepository applicationDailyLimitRepository;

    @MockitoBean
    private StorageService storageService;
    @MockitoBean
    private ApplicationPersistenceService applicationPersistenceService;

    private User user;
    private CardType studentCardType;
    private CardType honorKoreanCardType;

    @BeforeEach
    void setUp() {
        applicationDailyLimitRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User newUser = User.createOAuthUser("member@example.com", "oauth-app", "google", "Member");
        newUser.agreeTerms(true, true, true);
        user = userRepository.save(newUser);
        studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증", null, BigDecimal.valueOf(20000)));
        honorKoreanCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenReturn("http://mock-storage/uploaded");
        when(applicationPersistenceService.saveIndividual(
                any(), any(), any(), any(), anyBoolean(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB failure"));
        when(applicationPersistenceService.saveGroup(
                any(), any(), any(), any(), anyBoolean(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB failure"));
    }

    private ApplicationCreateRequest individualStudentRequest() {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE",
                  "orientation": "LANDSCAPE",
                  "schoolType": "UNIVERSITY",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "member": {
                    "englishName": "Hong Gildong",
                    "birthDate": "1990-05-15",
                    "nationality": "US",
                    "gender": "MALE",
                    "studentId": "20261234",
                    "department": "컴퓨터공학과"
                  }
                }
                """.formatted(studentCardType.getId());
        try {
            return objectMapper.readValue(json, ApplicationCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] imageBytes(int width, int height, String format) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes(300, 400, "jpg"));
    }

    @Test
    void createIndividualDeletesUploadedFilesInReverseOrderWhenPersistenceFails() {
        ApplicationCreateRequest request = individualStudentRequest();
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, seal))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<String> uploadKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(3)).upload(uploadKeyCaptor.capture(), any());
        List<String> uploadedKeys = uploadKeyCaptor.getAllValues();

        ArgumentCaptor<String> deleteKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(3)).delete(deleteKeyCaptor.capture());
        List<String> deletedKeys = deleteKeyCaptor.getAllValues();

        List<String> expectedReversed = new ArrayList<>(uploadedKeys);
        Collections.reverse(expectedReversed);
        assertThat(deletedKeys).isEqualTo(expectedReversed);
    }

    @Test
    void createIndividualDeletesAlreadyUploadedFilesWhenLaterUploadFails() {
        ApplicationCreateRequest request = individualStudentRequest();
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));
        when(storageService.upload(anyString(), any()))
                .thenReturn("http://mock-storage/logo")
                .thenThrow(new RuntimeException("S3 seal failure"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, seal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 seal failure");

        ArgumentCaptor<String> uploadKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(2)).upload(uploadKeyCaptor.capture(), any());

        ArgumentCaptor<String> deleteKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(1)).delete(deleteKeyCaptor.capture());
        assertThat(deleteKeyCaptor.getValue()).isEqualTo(uploadKeyCaptor.getAllValues().get(0));
    }

    @Test
    void createIndividualKeepsOriginalExceptionWhenCompensationDeleteFails() {
        ApplicationCreateRequest request = individualStudentRequest();
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));
        doThrow(new RuntimeException("S3 delete failure")).when(storageService).delete(anyString());

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, seal))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB failure");

        verify(storageService, times(3)).delete(anyString());
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

    @Test
    void createGroupDeletesUploadedFilesInReverseOrderWhenPersistenceFails() throws Exception {
        byte[] excel = buildExcel("1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul");
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        assertThatThrownBy(() -> applicationService.createGroup(user.getId(), groupRequest(), logo, seal, submitFile))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<String> uploadKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(3)).upload(uploadKeyCaptor.capture(), any());
        List<String> uploadedKeys = new ArrayList<>(uploadKeyCaptor.getAllValues());

        ArgumentCaptor<String> uploadBytesKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(1)).uploadBytes(uploadBytesKeyCaptor.capture(), any(), anyString());
        uploadedKeys.addAll(uploadBytesKeyCaptor.getAllValues());

        ArgumentCaptor<String> deleteKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(4)).delete(deleteKeyCaptor.capture());
        List<String> deletedKeys = deleteKeyCaptor.getAllValues();

        List<String> expectedReversed = new ArrayList<>(uploadedKeys);
        Collections.reverse(expectedReversed);
        assertThat(deletedKeys).isEqualTo(expectedReversed);
    }

    @Test
    void createGroupDeletesAlreadyUploadedFilesWhenMemberPhotoUploadFails() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul",
                "2|Jane Doe|1990-02-02|US|||FEMALE||jane@example.com|010-3333-4444|Seoul");
        byte[] zip = buildZip(excel, "1", "2");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());
        when(storageService.uploadBytes(anyString(), any(), anyString()))
                .thenReturn("http://mock-storage/member-1")
                .thenThrow(new RuntimeException("S3 member failure"));

        assertThatThrownBy(() -> applicationService.createGroup(user.getId(), groupRequest(), logo, seal, submitFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 member failure");

        ArgumentCaptor<String> uploadKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(3)).upload(uploadKeyCaptor.capture(), any());
        List<String> uploadedKeys = new ArrayList<>(uploadKeyCaptor.getAllValues());

        ArgumentCaptor<String> uploadBytesKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(2)).uploadBytes(uploadBytesKeyCaptor.capture(), any(), anyString());
        uploadedKeys.add(uploadBytesKeyCaptor.getAllValues().get(0));

        ArgumentCaptor<String> deleteKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(4)).delete(deleteKeyCaptor.capture());
        List<String> expectedReversed = new ArrayList<>(uploadedKeys);
        Collections.reverse(expectedReversed);
        assertThat(deleteKeyCaptor.getAllValues()).isEqualTo(expectedReversed);
    }

    // 이 파일의 모든 테스트는 applicationPersistenceService가 항상 실패하도록 스텁돼 있다(setUp 참고) —
    // 일일 3회 제한 슬롯이 실패 시 정상적으로 반환되는지 검증하기에 적합한 픽스처다.
    @Test
    void createIndividualReleasesDailyLimitSlotWhenPersistenceFails() {
        ApplicationCreateRequest request = individualStudentRequest();
        MockMultipartFile logo = new MockMultipartFile("schoolLogo", "logo.png", "image/png", imageBytes(50, 50, "png"));
        MockMultipartFile seal = new MockMultipartFile("schoolSeal", "seal.png", "image/png", imageBytes(50, 50, "png"));

        assertThatThrownBy(() -> applicationService.createIndividual(user.getId(), request, photo(), logo, seal))
                .isInstanceOf(RuntimeException.class);

        assertThat(applicationDailyLimitRepository
                .findByUserIdAndCountDate(user.getId(), ApplicationDailyLimitService.today()))
                .hasValueSatisfying(limit -> assertThat(limit.getCount()).isZero());
    }

    @Test
    void createGroupReleasesDailyLimitSlotWhenPersistenceFails() throws Exception {
        byte[] excel = buildExcel("1|John Doe|1988-01-01|US|||MALE||john@example.com|010-1111-2222|Seoul");
        byte[] zip = buildZip(excel, "1");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", "logo".getBytes());
        MockMultipartFile seal = new MockMultipartFile("seal", "seal.png", "image/png", "seal".getBytes());

        assertThatThrownBy(() -> applicationService.createGroup(user.getId(), groupRequest(), logo, seal, submitFile))
                .isInstanceOf(RuntimeException.class);

        assertThat(applicationDailyLimitRepository
                .findByUserIdAndCountDate(user.getId(), ApplicationDailyLimitService.today()))
                .hasValueSatisfying(limit -> assertThat(limit.getCount()).isZero());
    }
}
