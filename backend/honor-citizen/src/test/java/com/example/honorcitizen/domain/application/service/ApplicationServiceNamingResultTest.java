package com.example.honorcitizen.domain.application.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.BulkValidationException;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.dto.NamingResultApplyResponse;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// saju가 돌려준 "사주이름 포함" 엑셀 반영(applyNamingResult) — 이메일+전화번호 매칭, 전체 실패 정책.
@SpringBootTest
class ApplicationServiceNamingResultTest {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    private Long adminId;
    private Long applicationId;
    private ApplicationMember memberA;
    private ApplicationMember memberB;

    private static final String[] HEADERS = {
            "사진 번호", "영문명", "생년월일", "출생국가", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소", "사주이름",
    };

    @BeforeEach
    void setUp() {
        adminActivityLogRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("naming-admin@example.com", "oauth-naming-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User owner = userRepository.save(
                User.createOAuthUser("naming-owner@example.com", "oauth-naming-owner", "google", "Owner"));

        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-naming", null, BigDecimal.valueOf(30000)));

        Application application = applicationRepository.save(Application.createGroup(
                owner.getId(), "APP-2026-920001", cardType.getId(), IssueType.MOBILE, true, 2, null, null, null));
        applicationId = application.getId();

        memberA = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                applicationId, "John Doe", LocalDate.of(1988, 1, 1), "US", null, "Chicago",
                Gender.MALE, null, "john@example.com", "010-1111-2222", "Seoul", null, null, null));
        memberB = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                applicationId, "Mike Kim", LocalDate.of(1992, 3, 3), "US", null, "Chicago",
                Gender.MALE, null, "mike@example.com", "010-3333-4444", "Busan", null, null, null));
    }

    private byte[] buildExcel(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(0).createCell(0).setCellValue("공통 입국날짜");
            sheet.createRow(1).createCell(0).setCellValue("1.1");
            Row header = sheet.createRow(2);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
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
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile toMultipart(byte[] bytes) {
        return new MockMultipartFile("file", "result.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }

    @Test
    void assignsNamesToMatchingMembersByEmailAndPhone() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호(智毫)",
                "2|Mike Kim|1992-03-03|US||Chicago|MALE||mike@example.com|010-3333-4444||수민");

        NamingResultApplyResponse response = applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel));

        assertThat(response.getUpdatedCount()).isEqualTo(2);
        ApplicationMember reloadedA = applicationMemberRepository.findById(memberA.getId()).orElseThrow();
        assertThat(reloadedA.getName()).isEqualTo("지호");
        assertThat(reloadedA.getChineseName()).isEqualTo("智毫");
        ApplicationMember reloadedB = applicationMemberRepository.findById(memberB.getId()).orElseThrow();
        assertThat(reloadedB.getName()).isEqualTo("수민");
    }

    @Test
    void logsKoreanNameRegisterForFirstAssignment() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호(智毫)");

        applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel));

        assertThat(adminActivityLogRepository.findAll())
                .hasSize(1)
                .anySatisfy(log -> {
                    assertThat(log.getActionType()).isEqualTo(AdminActivityLog.KOREAN_NAME_REGISTER);
                    assertThat(log.getTargetId()).isEqualTo(applicationId);
                });
    }

    @Test
    void logsKoreanNameUpdateWhenOverwritingExistingName() throws Exception {
        memberA.assignKoreanName("구명", "旧名");
        applicationMemberRepository.saveAndFlush(memberA);

        byte[] excel = buildExcel("1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||새이름");
        applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel));

        assertThat(adminActivityLogRepository.findAll())
                .hasSize(1)
                .anySatisfy(log -> assertThat(log.getActionType()).isEqualTo(AdminActivityLog.KOREAN_NAME_UPDATE));
    }

    @Test
    void overwritesAlreadyAssignedName() throws Exception {
        memberA.assignKoreanName("구명", "旧名");
        applicationMemberRepository.saveAndFlush(memberA);

        byte[] excel = buildExcel("1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||새이름");
        applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel));

        ApplicationMember reloaded = applicationMemberRepository.findById(memberA.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("새이름");
        assertThat(reloaded.getChineseName()).isNull();
    }

    @Test
    void rejectsEntireFileWhenOneRowDoesNotMatchAnyMember() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호",
                "2|Unknown|1999-01-01|US||Chicago|MALE||nobody@example.com|010-9999-9999||엉뚱");

        assertThatThrownBy(() -> applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel)))
                .isInstanceOf(BulkValidationException.class);

        // 전체 실패이므로 매칭됐던 첫 행도 반영되지 않아야 한다.
        ApplicationMember reloadedA = applicationMemberRepository.findById(memberA.getId()).orElseThrow();
        assertThat(reloadedA.getName()).isNull();
    }

    @Test
    void rejectsWhenEmailAndPhoneMatchMultipleMembers() throws Exception {
        applicationMemberRepository.save(ApplicationMember.createGroupRow(
                applicationId, "John Duplicate", LocalDate.of(1988, 1, 1), "US", null, "Chicago",
                Gender.MALE, null, "john@example.com", "010-1111-2222", "Seoul", null, null, null));

        byte[] excel = buildExcel("1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호");

        assertThatThrownBy(() -> applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel)))
                .isInstanceOf(BulkValidationException.class);
    }

    @Test
    void rejectsForNonAdminCaller() throws Exception {
        User user = userRepository.save(
                User.createOAuthUser("naming-plain-user@example.com", "oauth-naming-plain", "google", "User"));
        byte[] excel = buildExcel("1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호");

        assertThatThrownBy(() -> applicationService.applyNamingResult(user.getId(), applicationId, toMultipart(excel)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void rejectsForMissingApplication() throws Exception {
        byte[] excel = buildExcel("1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호");

        assertThatThrownBy(() -> applicationService.applyNamingResult(adminId, 999999L, toMultipart(excel)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void rejectsSajuNameOutsideAllowedFormat() throws Exception {
        byte[] excel = buildExcel(
                "1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||가");

        assertThatThrownBy(() -> applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void doesNotChangeApplicationStatus() throws Exception {
        byte[] excel = buildExcel("1|John Doe|1988-01-01|US||Chicago|MALE||john@example.com|010-1111-2222||지호");
        applicationService.applyNamingResult(adminId, applicationId, toMultipart(excel));

        Application reloaded = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(reloaded.getStatus().name()).isEqualTo("SUBMITTED");
    }
}
