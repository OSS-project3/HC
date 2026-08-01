package com.example.honorcitizen.flow;

import com.example.honorcitizen.common.enums.CardTypeCode;
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
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.security.AuthTokens;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.storage.StorageService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실사용자 시나리오 기준 End-to-End 플로우 테스트.
 *
 * 실제로 존재하지 않는 두 지점은 다음과 같이 대체한다:
 * - "Google 로그인": 테스트에서 실제 구글 서버를 칠 수 없으므로,
 *   {@code OAuth2SuccessHandler}와 동일한 코드 경로(User.createNewUser → UserService.issueLoginTokens)로 재현한다.
 * - "신청 수정": 신청 내용을 사용자가 직접 수정하는 API는 아직 없다.
 *   대신 사진 재업로드가 가능한 PHOTO_REJECTED 상태로 가려면 관리자 검토가 필요한데
 *   Admin API도 아직 없으므로, 엔티티 상태 전이 메서드를 리포지토리 레벨에서 직접 호출해
 *   "관리자가 검토 후 사진을 반려했다"는 상황을 재현한다(HTTP 호출 아님).
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserApplicationFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
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

    private CardType cardType;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-flow", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void fullUserApplicationFlow() throws Exception {
        // ── 1) Google 로그인 (OAuth2SuccessHandler와 동일한 코드 경로로 재현) ──────────────
        User user = userRepository.save(User.createNewUser(
                "flow-user@example.com", "google-oauth-sub-12345", "google", "홍길동"));
        AuthTokens tokens = userService.issueLoginTokens(user);

        assertThat(jwtTokenProvider.validateToken(tokens.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(tokens.accessToken())).isTrue();
        assertThat(jwtTokenProvider.getUserId(tokens.accessToken())).isEqualTo(user.getId());

        Cookie accessTokenCookie = new Cookie("accessToken", tokens.accessToken());

        // ── 2) 인증 유지 (Cookie/JWT) — 발급받은 쿠키로 /api/users/me 호출해 실제로 인증되는지 확인 ──
        mockMvc.perform(get("/api/users/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value("flow-user@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        // ── 3) 개인 신청 생성 ────────────────────────────────────────────────────────
        String createRequestJson = """
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
                """.formatted(cardType.getId());
        MockMultipartFile createRequestPart = new MockMultipartFile(
                "request", "", "application/json", createRequestJson.getBytes());
        MockMultipartFile originalPhoto = new MockMultipartFile(
                "photo", "face.jpg", "image/jpeg", "original-photo-bytes".getBytes());

        String createResponseJson = mockMvc.perform(multipart("/api/applications")
                        .file(createRequestPart)
                        .file(originalPhoto)
                        .cookie(accessTokenCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationNumber").value(org.hamcrest.Matchers.startsWith("APP-")))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("WAITING"))
                .andReturn().getResponse().getContentAsString();

        Long applicationId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponseJson, "$.data.applicationId")).longValue();
        String applicationNumber = com.jayway.jsonpath.JsonPath.read(createResponseJson, "$.data.applicationNumber");

        // DB 상태 검증
        Application application = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(application.isOwnedBy(user.getId())).isTrue();
        assertThat(application.isIndividual()).isTrue();
        assertThat(application.getTotalQuantity()).isEqualTo(1);
        assertThat(application.getCardDesignId()).isNull();

        Applicant applicant = applicantRepository.findByApplicationId(applicationId).orElseThrow();
        assertThat(applicant.getName()).isEqualTo("홍길동");
        assertThat(applicant.getEmail()).isEqualTo("flow-user@example.com");

        ApplicationMember member = applicationMemberRepository.findByApplicationId(applicationId).get(0);
        assertThat(member.getEnglishName()).isEqualTo("Hong Gildong");
        String originalPhotoPath = member.getPhotoPath();
        assertThat(originalPhotoPath).contains("member-photos").contains("face.jpg");

        // 파일 상태 검증 — 원본 사진이 실제로 storage.upload()에 전달됐는지
        verify(storageService).upload(eq(originalPhotoPath), eq(originalPhoto));

        // ── 4) 신청 조회 (전용 "내 신청 조회" API가 없어 공개 lookup으로 대체) ──────────────
        String lookupAfterCreateJson = """
                { "method": "application", "keyValue": "%s", "phone": "010-1234-5678" }
                """.formatted(applicationNumber);

        mockMvc.perform(post("/api/applications/lookup")
                        .contentType("application/json")
                        .content(lookupAfterCreateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.data.applicantNameMasked").value("홍*동"));

        // ── 5) 신청 수정 (수정 API 없음 → 관리자 검토·반려를 엔티티 레벨로 재현) ────────────
        application.confirmPayment();
        application.startReview();
        application.rejectPhoto("사진이 흐려서 식별이 어렵습니다.");
        applicationRepository.save(application);

        Application afterReject = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(afterReject.getStatus().name()).isEqualTo("PHOTO_REJECTED");
        assertThat(afterReject.getPhotoRejectReason()).isEqualTo("사진이 흐려서 식별이 어렵습니다.");

        // ── 6) Lookup 조회 (반려 상태가 실제로 노출되는지) ──────────────────────────────
        mockMvc.perform(post("/api/applications/lookup")
                        .contentType("application/json")
                        .content(lookupAfterCreateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PHOTO_REJECTED"))
                .andExpect(jsonPath("$.data.photoRejectReason").value("사진이 흐려서 식별이 어렵습니다."));

        // ── 7) 사진 재업로드 ────────────────────────────────────────────────────────
        MockMultipartFile newPhoto = new MockMultipartFile(
                "photo", "face-retake.jpg", "image/jpeg", "retaken-photo-bytes".getBytes());
        var reuploadBuilder = multipart("/api/applications/" + applicationId + "/photo")
                .file(newPhoto)
                .cookie(accessTokenCookie);
        reuploadBuilder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(reuploadBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REVIEWING"));

        // 파일 상태 검증 — 새 사진이 원본과 다른 키로 storage에 올라갔는지
        verify(storageService, times(1)).upload(eq(originalPhotoPath), eq(originalPhoto));
        verify(storageService).upload(
                argThat(key -> key.contains("member-photos") && key.contains("face-retake.jpg") && !key.equals(originalPhotoPath)),
                eq(newPhoto));

        // ── 8) DB 최종 상태 검증 ───────────────────────────────────────────────────
        Application finalApplication = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(finalApplication.getStatus().name()).isEqualTo("REVIEWING");
        assertThat(finalApplication.getPhotoRejectReason()).isNull();

        ApplicationMember finalMember = applicationMemberRepository.findByApplicationId(applicationId).get(0);
        assertThat(finalMember.getPhotoPath()).isNotEqualTo(originalPhotoPath);
        assertThat(finalMember.getPhotoPath()).contains("face-retake.jpg");

        Applicant finalApplicant = applicantRepository.findByApplicationId(applicationId).orElseThrow();
        assertThat(finalApplicant.getName()).isEqualTo("홍길동");
        assertThat(finalApplicant.getEmail()).isEqualTo("flow-user@example.com");

        // HTTP 레벨로도 최종 상태가 일치하는지 마지막으로 한 번 더 확인
        mockMvc.perform(post("/api/applications/lookup")
                        .contentType("application/json")
                        .content(lookupAfterCreateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEWING"))
                .andExpect(jsonPath("$.data.photoRejectReason").doesNotExist());
    }
}
