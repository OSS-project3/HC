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
import com.example.honorcitizen.common.enums.Orientation;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired
    private SchoolRepository schoolRepository;

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
        schoolRepository.deleteAll();
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

    // 작명 업무 진행중/완료/캔슬 조회(2026-09-24) — HTTP 배선만 검증(분류 판정 로직 자체는
    // ApplicationNamingProgressListTest에서 이미 커버). setUp의 otherUsersApplication은
    // SUBMITTED라 IN_PROGRESS에 속한다.
    @Test
    void listFiltersByNamingProgressInProgress() throws Exception {
        mockMvc.perform(get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("namingProgress", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(otherUsersApplication.getId()));
    }

    @Test
    void listFiltersByNamingProgressDoneExcludesInProgressApplication() throws Exception {
        mockMvc.perform(get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("namingProgress", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void listExposesCompletedMemberCountForGroupApplication() throws Exception {
        Application group = applicationRepository.save(Application.createGroup(
                otherUsersApplication.getUserId(), "APP-2026-910100", cardType.getId(),
                IssueType.MOBILE, true, 2, null, null, null));
        ApplicationMember done = ApplicationMember.createIndividual(group.getId(), "Member One",
                LocalDate.of(1990, 1, 1), "KR", null, null, Gender.MALE, null, null, null, "photos/b.jpg");
        var frontField = ApplicationMember.class.getDeclaredField("cardFrontPath");
        frontField.setAccessible(true);
        frontField.set(done, "cards/front.png");
        var backField = ApplicationMember.class.getDeclaredField("cardBackPath");
        backField.setAccessible(true);
        backField.set(done, "cards/back.png");
        applicationMemberRepository.save(done);
        applicationMemberRepository.save(ApplicationMember.createIndividual(group.getId(), "Member Two",
                LocalDate.of(1990, 1, 1), "KR", null, null, Gender.MALE, null, null, null, "photos/c.jpg"));

        mockMvc.perform(get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .param("namingProgress", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].applicationId").value(group.getId()))
                .andExpect(jsonPath("$.data.content[0].completedMemberCount").value(1))
                .andExpect(jsonPath("$.data.content[0].totalQuantity").value(2));
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
        Application groupApplicationDraft = Application.createGroup(
                groupOwner.getId(), "APP-2026-930001", cardType.getId(), IssueType.MOBILE, true, 1, null, null, null);
        groupApplicationDraft.confirmPayment();
        groupApplicationDraft.startReview();
        groupApplicationDraft.approveToNaming();
        Application groupApplication = applicationRepository.save(groupApplicationDraft);
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

    // 관리자 강제 취소(2026-09-25 확정) — 비즈니스 로직(허용 상태·멱등·파일 정리 등)은
    // ApplicationServiceAdminCancelTest에서 이미 커버, 여기서는 HTTP/JSON 배선만 검증한다.
    @Test
    void cancelEndpointCancelsApplicationAsAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"중복 신청으로 관리자 취소\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationType").value("ADMIN"))
                .andExpect(jsonPath("$.data.cancellationReason").value("ADMIN_DECISION"))
                .andExpect(jsonPath("$.data.cancellationMemo").value("중복 신청으로 관리자 취소"))
                .andExpect(jsonPath("$.data.firstCancellation").value(true));
    }

    @Test
    void cancelEndpointIsIdempotentOnSecondCall() throws Exception {
        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"최초 사유\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"다시 보낸 사유\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationMemo").value("최초 사유"))
                .andExpect(jsonPath("$.data.firstCancellation").value(false));
    }

    @Test
    void cancelEndpointForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"사유\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelEndpointWithBlankMemoReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelEndpointForMissingApplicationReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/admin/applications/999999/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"사유\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelEndpointRejectsCompletedApplication() throws Exception {
        otherUsersApplication.confirmPayment();
        otherUsersApplication.startReview();
        otherUsersApplication.approveToNaming();
        otherUsersApplication.completeNaming();
        applicationRepository.saveAndFlush(otherUsersApplication);
        completeCardGenerationFor(otherUsersApplication, LocalDate.of(2026, 9, 14));
        Application reloaded = applicationRepository.findById(otherUsersApplication.getId()).orElseThrow();
        reloaded.startProducing();
        reloaded.markCardReady(java.time.LocalDateTime.of(2026, 9, 14, 10, 0));
        applicationRepository.saveAndFlush(reloaded);

        mockMvc.perform(post("/api/admin/applications/" + otherUsersApplication.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"cancellationMemo\":\"사유\"}"))
                .andExpect(status().isBadRequest());
    }

    // 카드 생성 완료 집계 검증(3-F) — start-producing/card-ready 둘 다 Member의 카드번호·앞뒤 이미지·
    // 발급일자와 Application의 cardDesignId/cardIssueDate가 전부 확정돼 있어야 통과한다.
    private void completeCardGenerationFor(Application application, LocalDate issueDate) throws Exception {
        ApplicationMember member = applicationMemberRepository.findByApplicationId(application.getId()).get(0);
        member.assignCardNumber("ROK-00001-0001");
        member.assignCardImages("cards/front.png", "cards/back.png", issueDate);
        applicationMemberRepository.saveAndFlush(member);
        // application 파라미터를 그대로 재사용하면 이전 saveAndFlush로 detach된 stale 인스턴스라
        // ObjectOptimisticLockingFailureException이 난다 — 최신 상태로 다시 조회해서 반영한다.
        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        reloaded.confirmCardGeneration(1L, issueDate);
        applicationRepository.saveAndFlush(reloaded);
    }

    @Test
    void startProducingEndpointTransitionsStatus() throws Exception {
        otherUsersApplication.confirmPayment();
        otherUsersApplication.startReview();
        otherUsersApplication.approveToNaming();
        otherUsersApplication.completeNaming();
        applicationRepository.saveAndFlush(otherUsersApplication);
        completeCardGenerationFor(otherUsersApplication, LocalDate.of(2026, 9, 14));

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
        completeCardGenerationFor(otherUsersApplication, LocalDate.of(2026, 9, 14));

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

    // 만세력 재진입 일괄 복원(1-E-3) — 비즈니스 로직(활성만·소속 검증·불변조건)은
    // ManseryeokServiceTest에서 이미 커버, 여기선 HTTP/JSON 배선만 검증한다.
    @Test
    void manseryeokResultsReturnsEmptyListWhenNothingConfirmed() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId() + "/manseryeok-results")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void manseryeokResultsForMissingApplicationReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/applications/999999/manseryeok-results")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void manseryeokResultsForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId() + "/manseryeok-results")
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void manseryeokResultsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/applications/" + otherUsersApplication.getId() + "/manseryeok-results"))
                .andExpect(status().isUnauthorized());
    }

    // 십이간지 캐릭터 디자인 세트 — 비즈니스 로직(1~5 검증·잠금 없음)은
    // ApplicationServiceZodiacDesignSetTest/ApplicationStateTransitionTest가 이미 커버, 여기선
    // HTTP 배선만 검증한다.
    @Test
    void assignZodiacDesignSetSucceeds() throws Exception {
        mockMvc.perform(put("/api/admin/applications/" + otherUsersApplication.getId() + "/zodiac-design")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"zodiacDesignSet\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    void assignZodiacDesignSetRejectsOutOfRangeValue() throws Exception {
        mockMvc.perform(put("/api/admin/applications/" + otherUsersApplication.getId() + "/zodiac-design")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"zodiacDesignSet\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignZodiacDesignSetForNonAdminReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/applications/" + otherUsersApplication.getId() + "/zodiac-design")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType("application/json")
                        .content("{\"zodiacDesignSet\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignZodiacDesignSetWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/admin/applications/" + otherUsersApplication.getId() + "/zodiac-design")
                        .contentType("application/json")
                        .content("{\"zodiacDesignSet\":1}"))
                .andExpect(status().isUnauthorized());
    }

    // 관리자 학교 연결(4-A-1) — 직접입력(schoolId=null) STUDENT 신청을 이미 등록된 School에 연결.
    private Application directInputStudentApplication(String applicationNumber, Long ownerId, Long cardTypeId) {
        Application application = applicationRepository.save(Application.createIndividual(
                ownerId, applicationNumber, cardTypeId, IssueType.MOBILE, true, null, null,
                Orientation.LANDSCAPE, SchoolType.HIGH_SCHOOL, "직접입력고등학교", null));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Kim Student", LocalDate.of(2008, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/student.jpg"));
        return application;
    }

    @Test
    void linkSchoolSucceeds() throws Exception {
        CardType studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-adminctrl-link1", null, BigDecimal.valueOf(20000)));
        School highSchool = schoolRepository.save(School.create("전주고등학교-link1", SchoolType.HIGH_SCHOOL));
        Application application = directInputStudentApplication(
                "APP-2026-920001", otherUsersApplication.getUserId(), studentCardType.getId());

        mockMvc.perform(put("/api/admin/applications/" + application.getId() + "/school")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"schoolId\":" + highSchool.getId() + ",\"applicationVersion\":" + application.getVersion() + "}"))
                .andExpect(status().isOk());

        Application reloaded = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(reloaded.getSchoolId()).isEqualTo(highSchool.getId());
    }

    @Test
    void linkSchoolRejectsMissingSchoolIdAsBadRequest() throws Exception {
        CardType studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-adminctrl-link2", null, BigDecimal.valueOf(20000)));
        Application application = directInputStudentApplication(
                "APP-2026-920002", otherUsersApplication.getUserId(), studentCardType.getId());

        mockMvc.perform(put("/api/admin/applications/" + application.getId() + "/school")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"applicationVersion\":" + application.getVersion() + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkSchoolForNonAdminReturnsForbidden() throws Exception {
        CardType studentCardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-adminctrl-link3", null, BigDecimal.valueOf(20000)));
        School highSchool = schoolRepository.save(School.create("전주고등학교-link3", SchoolType.HIGH_SCHOOL));
        Application application = directInputStudentApplication(
                "APP-2026-920003", otherUsersApplication.getUserId(), studentCardType.getId());

        mockMvc.perform(put("/api/admin/applications/" + application.getId() + "/school")
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType("application/json")
                        .content("{\"schoolId\":" + highSchool.getId() + ",\"applicationVersion\":" + application.getVersion() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void linkSchoolWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/admin/applications/" + otherUsersApplication.getId() + "/school")
                        .contentType("application/json")
                        .content("{\"schoolId\":1,\"applicationVersion\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void linkSchoolForMissingApplicationReturnsNotFound() throws Exception {
        School highSchool = schoolRepository.save(School.create("전주고등학교-link5", SchoolType.HIGH_SCHOOL));

        mockMvc.perform(put("/api/admin/applications/999999/school")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType("application/json")
                        .content("{\"schoolId\":" + highSchool.getId() + ",\"applicationVersion\":0}"))
                .andExpect(status().isNotFound());
    }
}
