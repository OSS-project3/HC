package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.entity.Application;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

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
    private User user;
    private CardType cardType;

    private static final String REQUEST_JSON = """
            {
              "cardTypeId": %d,
              "issueType": "MOBILE",
              "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
              "member": {
                "englishName": "Hong Gildong",
                "birthDate": "1990-05-15",
                "nationality": "US",
                "birthRegion": "Chicago",
                "gender": "MALE"
              }
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

        user = User.createOAuthUser("app@example.com", "oauth-app-ctrl", "google", "Applicant");
        user.agreeTerms(true, true, true);
        user = userRepository.save(user);
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-ctrl", null, BigDecimal.valueOf(30000)));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void createIndividualReturnsCreatedWithApplicationNumber() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile photoPart = new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes());

        mockMvc.perform(multipart("/api/applications")
                        .file(requestPart)
                        .file(photoPart)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationNumber").value(org.hamcrest.Matchers.startsWith("APP-")))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("WAITING"));
    }

    @Test
    void createIndividualReturnsUnauthorizedWithoutToken() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile photoPart = new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes());

        mockMvc.perform(multipart("/api/applications")
                        .file(requestPart)
                        .file(photoPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createIndividualReturnsNotFoundForUnknownCardType() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(999999L).getBytes());
        MockMultipartFile photoPart = new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes());

        mockMvc.perform(multipart("/api/applications")
                        .file(requestPart)
                        .file(photoPart)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void cancelReturnsConfirmedContractAndIsIdempotent() throws Exception {
        Application application = applicationRepository.save(Application.createIndividual(
                user.getId(), "APP-2026-CANCEL-1", cardType.getId(), IssueType.MOBILE, true, null, null));

        mockMvc.perform(post("/api/applications/{applicationId}/cancel", application.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").value(application.getId()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("WAITING"))
                .andExpect(jsonPath("$.data.refundRequired").value(false))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

        mockMvc.perform(post("/api/applications/{applicationId}/cancel", application.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancelRequiresAuthentication() throws Exception {
        Application application = applicationRepository.save(Application.createIndividual(
                user.getId(), "APP-2026-CANCEL-2", cardType.getId(), IssueType.MOBILE, true, null, null));

        mockMvc.perform(post("/api/applications/{applicationId}/cancel", application.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelReturnsRefundRequiredWhenPaymentWasConfirmed() throws Exception {
        Application application = Application.createIndividual(
                user.getId(), "APP-2026-CANCEL-PAID", cardType.getId(), IssueType.MOBILE, true, null, null);
        application.confirmPayment();
        application = applicationRepository.save(application);

        mockMvc.perform(post("/api/applications/{applicationId}/cancel", application.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.refundRequired").value(true));
    }

    @Test
    void cancelRejectsOtherOwner() throws Exception {
        Application application = applicationRepository.save(Application.createIndividual(
                user.getId(), "APP-2026-CANCEL-3", cardType.getId(), IssueType.MOBILE, true, null, null));
        User other = User.createOAuthUser("other-owner@example.com", "oauth-other-owner", "google", "Other");
        other.agreeTerms(true, true, true);
        other = userRepository.save(other);
        String otherToken = "Bearer " + jwtTokenProvider.generateAccessToken(other.getId(), other.getRole());

        mockMvc.perform(post("/api/applications/{applicationId}/cancel", application.getId())
                        .header("Authorization", otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void cancelReturnsNotFoundForUnknownApplication() throws Exception {
        mockMvc.perform(post("/api/applications/{applicationId}/cancel", 999999L)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void cancelRejectsApplicationAfterNamingStarted() throws Exception {
        Application application = Application.createIndividual(
                user.getId(), "APP-2026-CANCEL-4", cardType.getId(), IssueType.MOBILE, true, null, null);
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application = applicationRepository.save(application);

        mockMvc.perform(post("/api/applications/{applicationId}/cancel", application.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void createIndividualReturnsInvalidInputWhenReceiverZipCodeMissing() throws Exception {
        String json = """
                {
                  "cardTypeId": %d,
                  "issueType": "MOBILE_AND_PHYSICAL",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "receiver": { "sameAsApplicant": false, "name": "김수령", "phone": "010-9999-8888", "address": "서울특별시 강남구" },
                  "member": { "englishName": "Hong Gildong", "birthDate": "1990-05-15", "nationality": "US", "birthRegion": "Chicago", "gender": "MALE" }
                }
                """.formatted(cardType.getId());
        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", json.getBytes());
        MockMultipartFile photoPart = new MockMultipartFile("photo", "face.jpg", "image/jpeg", imageBytes());

        mockMvc.perform(multipart("/api/applications")
                        .file(requestPart)
                        .file(photoPart)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void createIndividualReturnsInvalidInputWhenPhotoMissing() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());

        mockMvc.perform(multipart("/api/applications")
                        .file(requestPart)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
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
}
