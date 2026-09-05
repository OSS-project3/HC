package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.common.enums.UserRole;
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
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 관리자 신청 목록/상세 — 소유자 무관 전체 조회, ADMIN만 허용.
@SpringBootTest
@AutoConfigureMockMvc
class AdminApplicationControllerTest {

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

    private String adminToken;
    private String userToken;
    private CardType cardType;
    private Application otherUsersApplication;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = User.createOAuthUser("admin-app-admin@example.com", "oauth-admin-app-admin", "google", "Admin");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User owner = userRepository.save(User.createOAuthUser("admin-app-owner@example.com", "oauth-admin-app-owner", "google", "Owner"));
        owner.agreeTerms(true, true, true);
        userRepository.save(owner);
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(owner.getId(), UserRole.USER);

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-adminctrl", null, BigDecimal.valueOf(30000)));

        // 관리자 API는 "본인 소유가 아닌" 신청도 보여야 한다는 걸 증명하기 위해 owner 소유로 만든다.
        otherUsersApplication = applicationRepository.save(Application.createIndividual(
                owner.getId(), "APP-2026-910001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                otherUsersApplication.getId(), "홍길동", "hong-admin-app@example.com", "010-1111-2222"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                otherUsersApplication.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
    }

    @Test
    void listReturnsApplicationsRegardlessOfOwner() throws Exception {
        mockMvc.perform(get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(otherUsersApplication.getId()));
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listWithInvalidSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailReturnsApplicationNotOwnedByAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(otherUsersApplication.getId()));
    }

    @Test
    void detailForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId())
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailForMissingApplicationReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/applications/999999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    // saju "사주이름 포함" 엑셀 반영 — HTTP/multipart/인가 배선만 검증(비즈니스 로직은
    // ApplicationServiceNamingResultTest에서 이미 커버).
    private static final String[] NAMING_HEADERS = {
            "사진 번호", "영문명", "생년월일", "출생국가", "출생시간", "출생지역", "성별",
            "개별입국날짜", "이메일", "전화번호", "주소", "사주이름",
    };

    private byte[] buildNamingResultExcel(String... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("신청자명단");
            sheet.createRow(0).createCell(0).setCellValue("공통 입국날짜");
            sheet.createRow(1).createCell(0).setCellValue("1.1");
            Row header = sheet.createRow(2);
            for (int i = 0; i < NAMING_HEADERS.length; i++) {
                header.createCell(i).setCellValue(NAMING_HEADERS[i]);
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

    @Test
    void applyNamingResultUpdatesMatchingGroupMember() throws Exception {
        User groupOwner = userRepository.save(
                User.createOAuthUser("naming-group-owner@example.com", "oauth-naming-group-owner", "google", "Owner"));
        Application groupApplication = applicationRepository.save(Application.createGroup(
                groupOwner.getId(), "APP-2026-930001", cardType.getId(), IssueType.MOBILE, true, 1, null, null, null));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createGroupRow(
                groupApplication.getId(), "Jane Park", LocalDate.of(1995, 5, 5), "US", null, "Chicago",
                Gender.FEMALE, null, "jane@example.com", "010-5555-6666", "Seoul", null, null, null));

        MockMultipartFile file = new MockMultipartFile("file", "result.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildNamingResultExcel("1|Jane Park|1995-05-05|US||Chicago|FEMALE||jane@example.com|010-5555-6666||지은"));

        mockMvc.perform(multipart("/api/admin/applications/" + groupApplication.getId() + "/naming-result")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(1));

        ApplicationMember reloaded = applicationMemberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("지은");
    }

    @Test
    void applyNamingResultForNonAdminReturnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "result.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildNamingResultExcel("1|Jane Park|1995-05-05|US||Chicago|FEMALE||jane@example.com|010-5555-6666||지은"));

        mockMvc.perform(multipart("/api/admin/applications/" + otherUsersApplication.getId() + "/naming-result")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyNamingResultWithoutTokenReturnsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "result.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildNamingResultExcel("1|Jane Park|1995-05-05|US||Chicago|FEMALE||jane@example.com|010-5555-6666||지은"));

        mockMvc.perform(multipart("/api/admin/applications/" + otherUsersApplication.getId() + "/naming-result")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    // 상태 전이 5종 — 비즈니스 로직·감사로그는 ApplicationServiceAdminTransitionTest에서 이미 커버,
    // 여기서는 HTTP/JSON 배선만 검증한다.
    @Test
    void rejectPhotoEndpointTransitionsStatus() throws Exception {
        otherUsersApplication.confirmPayment();
        otherUsersApplication.startReview();
        applicationRepository.saveAndFlush(otherUsersApplication);

        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/reject-photo")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"reason\":\"사진이 흐립니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PHOTO_REJECTED"));
    }

    @Test
    void startProducingEndpointTransitionsStatus() throws Exception {
        otherUsersApplication.confirmPayment();
        otherUsersApplication.startReview();
        otherUsersApplication.approveToNaming();
        otherUsersApplication.completeNaming();
        applicationRepository.saveAndFlush(otherUsersApplication);

        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/start-producing")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PRODUCING"));
    }

    @Test
    void cardReadyEndpointCompletesMobileApplication() throws Exception {
        otherUsersApplication.confirmPayment();
        otherUsersApplication.startReview();
        otherUsersApplication.approveToNaming();
        otherUsersApplication.completeNaming();
        otherUsersApplication.startProducing();
        applicationRepository.saveAndFlush(otherUsersApplication);

        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/card-ready")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void completeNamingEndpointTransitionsStatus() throws Exception {
        otherUsersApplication.confirmPayment();
        otherUsersApplication.startReview();
        otherUsersApplication.approveToNaming();
        applicationRepository.saveAndFlush(otherUsersApplication);
        ApplicationMember member = applicationMemberRepository
                .findByApplicationId(otherUsersApplication.getId()).get(0);
        member.assignKoreanName("홍", "길동", null, "길할 길, 아이 동", "길이 복되기를 바라는 뜻");
        applicationMemberRepository.saveAndFlush(member);

        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/complete-naming")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PRODUCTION_READY"));
    }

    @Test
    void dispatchEndpointStoresTrackingNumberAndCompletesPhysicalApplication() throws Exception {
        Application physicalApplication = applicationRepository.save(Application.createIndividual(
                otherUsersApplication.getUserId(), "APP-2026-910002", cardType.getId(),
                IssueType.MOBILE_AND_PHYSICAL, false, null, null));
        physicalApplication.confirmPayment();
        physicalApplication.startReview();
        physicalApplication.approveToNaming();
        physicalApplication.completeNaming();
        physicalApplication.startProducing();
        physicalApplication.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.saveAndFlush(physicalApplication);

        mockMvc.perform(post("/api/admin/applications/" + physicalApplication.getId() + "/dispatch")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"trackingNumber\":\"1234567890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void startProducingForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/start-producing")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void startProducingWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/start-producing"))
                .andExpect(status().isUnauthorized());
    }

    // 엑셀 내보내기 — 비즈니스 로직은 ApplicationServiceExportTest에서 이미 커버, HTTP 배선만 검증.
    @Test
    void exportEndpointReturnsXlsxForIndividualApplication() throws Exception {
        mockMvc.perform(post("/api/admin/applications/export")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"applicationIds\":[" + otherUsersApplication.getId() + "],\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/applications/export")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType("application/json")
                        .content("{\"applicationIds\":[" + otherUsersApplication.getId() + "],\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/applications/export")
                        .contentType("application/json")
                        .content("{\"applicationIds\":[" + otherUsersApplication.getId() + "],\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isUnauthorized());
    }

    // 관리자 카드 다운로드 — 비즈니스 로직(상태 게이트·누락 멤버 식별·개별 다운로드)은
    // ApplicationServiceAdminCardDownloadTest에서 이미 커버, 여기선 HTTP 배선만 검증한다.
    @Test
    void cardsDownloadRejectsWhenMemberCardImagesNotReady() throws Exception {
        // setUp()의 otherUsersApplication은 카드 이미지가 아직 없는 멤버 1명뿐이다.
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId() + "/cards/download")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cardsDownloadForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId() + "/cards/download")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cardsDownloadWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId() + "/cards/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCardDownloadRejectsWhenNotReady() throws Exception {
        Long memberId = applicationMemberRepository.findByApplicationId(otherUsersApplication.getId()).get(0).getId();

        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId()
                        + "/members/" + memberId + "/cards/download")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void memberCardDownloadForNonAdminReturnsForbidden() throws Exception {
        Long memberId = applicationMemberRepository.findByApplicationId(otherUsersApplication.getId()).get(0).getId();

        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId()
                        + "/members/" + memberId + "/cards/download")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }
}
