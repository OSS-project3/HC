package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.ApplicationPhotoReuploadResponse;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.application.repository.ReceiverRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ApplicationServicePhotoReuploadTest {

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
    private UploadFileRepository uploadFileRepository;

    @MockitoBean
    private com.example.honorcitizen.infra.storage.StorageService storageService;

    private CardType cardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-reupload", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
        when(storageService.uploadBytes(anyString(), any(), anyString())).thenReturn("http://mock-storage/uploaded");
    }

    private Application photoRejectedIndividualApplication(Long ownerId) {
        Application application = applicationRepository.save(Application.createIndividual(
                ownerId, "APP-2026-200001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "owner@example.com", "010-1234-5678"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/old.jpg"));
        application.confirmPayment();
        application.startReview();
        application.rejectPhoto("사진이 흐립니다.");
        return applicationRepository.save(application);
    }

    private Application photoRejectedGroupApplication(Long ownerId) {
        Application application = applicationRepository.save(Application.createGroup(
                ownerId, "APP-2026-200002", cardType.getId(), IssueType.MOBILE, true, 1, 10L, 11L, 12L));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "010-2222-2222", "Seoul", null, null, "photos/old.jpg"));
        application.confirmPayment();
        application.startReview();
        application.rejectPhoto("사진이 흐립니다.");
        return applicationRepository.save(application);
    }

    private byte[] buildZip(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("members");
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
            ByteArrayOutputStream excelOut = new ByteArrayOutputStream();
            workbook.write(excelOut);

            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(zipOut)) {
                zip.putNextEntry(new ZipEntry("members.xlsx"));
                zip.write(excelOut.toByteArray());
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("9.jpg"));
                zip.write("photo-9".getBytes());
                zip.closeEntry();
            }
            return zipOut.toByteArray();
        }
    }

    @Test
    void reuploadPhotoForIndividualUpdatesMemberPhotoAndReturnsToReviewing() {
        Application application = photoRejectedIndividualApplication(1L);
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "new-bytes".getBytes());

        ApplicationPhotoReuploadResponse response = applicationService.reuploadPhoto(
                1L, application.getId(), photo, null);

        assertThat(response.getStatus().name()).isEqualTo("REVIEWING");

        Application saved = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(saved.getPhotoRejectReason()).isNull();

        ApplicationMember member = applicationMemberRepository.findByApplicationId(application.getId()).get(0);
        assertThat(member.getPhotoPath()).contains("new.jpg");
    }

    @Test
    void reuploadPhotoForIndividualDeletesOldPhotoFile() {
        Application application = photoRejectedIndividualApplication(1L);
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "new-bytes".getBytes());

        applicationService.reuploadPhoto(1L, application.getId(), photo, null);

        verify(storageService).delete("photos/old.jpg");
    }

    @Test
    void reuploadPhotoRejectsWhenStatusIsNotPhotoRejected() {
        Application application = applicationRepository.save(Application.createIndividual(
                1L, "APP-2026-200003", cardType.getId(), IssueType.MOBILE, true, null, null));
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "new-bytes".getBytes());

        assertThatThrownBy(() -> applicationService.reuploadPhoto(1L, application.getId(), photo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    void reuploadPhotoRejectsWhenNotOwner() {
        Application application = photoRejectedIndividualApplication(1L);
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "new-bytes".getBytes());

        assertThatThrownBy(() -> applicationService.reuploadPhoto(2L, application.getId(), photo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void reuploadPhotoRejectsSubmitFileForIndividualApplication() {
        Application application = photoRejectedIndividualApplication(1L);
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", "zip".getBytes());

        assertThatThrownBy(() -> applicationService.reuploadPhoto(1L, application.getId(), null, submitFile))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void reuploadPhotoForGroupReplacesMembersAndUpdatesQuantity() throws Exception {
        Application application = photoRejectedGroupApplication(1L);
        byte[] zip = buildZip("9|Jane Doe|1991-02-02|US|||FEMALE||jane@example.com|010-3333-3333|Busan");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);

        ApplicationPhotoReuploadResponse response = applicationService.reuploadPhoto(
                1L, application.getId(), null, submitFile);

        assertThat(response.getStatus().name()).isEqualTo("REVIEWING");

        Application saved = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(saved.getTotalQuantity()).isEqualTo(1);
        assertThat(saved.getPhotoRejectReason()).isNull();

        List<ApplicationMember> members = applicationMemberRepository.findByApplicationId(application.getId());
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getEnglishName()).isEqualTo("Jane Doe");
    }

    @Test
    void reuploadPhotoForGroupDeletesOldMemberPhotosAndOldSubmitFile() throws Exception {
        UploadFile oldSubmitFile = uploadFileRepository.save(UploadFile.create(
                "old.zip", "old-stored.zip", "applications/uploads/old-submit.zip",
                UploadFileType.ZIP, "application/zip", 100L));
        Application application = applicationRepository.save(Application.createGroup(
                1L, "APP-2026-200099", cardType.getId(), IssueType.MOBILE, true, 1, 10L, 11L, oldSubmitFile.getId()));
        applicantRepository.save(Applicant.createGroup(
                application.getId(), "인사담당", "hr@example.com", "010-1111-1111", "OO기업", "인사팀"));
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                application.getId(), "John Doe", LocalDate.of(1988, 1, 1), "US",
                null, null, Gender.MALE, null, "john@example.com", "010-2222-2222", "Seoul", null, null, "photos/old-member.jpg"));
        application.confirmPayment();
        application.startReview();
        application.rejectPhoto("사진이 흐립니다.");
        applicationRepository.save(application);

        byte[] zip = buildZip("9|Jane Doe|1991-02-02|US|||FEMALE||jane@example.com|010-3333-3333|Busan");
        MockMultipartFile submitFile = new MockMultipartFile("submitFile", "bulk.zip", "application/zip", zip);

        applicationService.reuploadPhoto(1L, application.getId(), null, submitFile);

        verify(storageService).delete("photos/old-member.jpg");
        verify(storageService).delete("applications/uploads/old-submit.zip");
    }

    @Test
    void reuploadPhotoRejectsPhotoPartForGroupApplication() {
        Application application = photoRejectedGroupApplication(1L);
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "bytes".getBytes());

        assertThatThrownBy(() -> applicationService.reuploadPhoto(1L, application.getId(), photo, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
