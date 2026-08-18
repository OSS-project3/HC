package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.common.enums.IssueType;
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
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationCardDownloadControllerTest {

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
    private Application application;

    @BeforeEach
    void setUp() throws Exception {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.createNewUser("card-ctrl@example.com", "oauth-card-ctrl", "google", "Card"));
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-card-ctrl", null, BigDecimal.valueOf(30000)));

        application = applicationRepository.save(Application.createIndividual(
                user.getId(), "APP-2026-500001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "card-ctrl@example.com", "010-1234-5678"));
        ApplicationMember member = applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));

        var frontField = ApplicationMember.class.getDeclaredField("cardFrontPath");
        frontField.setAccessible(true);
        frontField.set(member, "cards/front.png");
        var backField = ApplicationMember.class.getDeclaredField("cardBackPath");
        backField.setAccessible(true);
        backField.set(member, "cards/back.png");
        applicationMemberRepository.save(member);

        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);

        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("http://mock-storage/presigned");
    }

    @Test
    void getCardDownloadReturnsCardUrls() throws Exception {
        mockMvc.perform(get("/api/applications/" + application.getId() + "/cards/download")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.data.cardFrontUrl").value("http://mock-storage/presigned"))
                .andExpect(jsonPath("$.data.cardBackUrl").value("http://mock-storage/presigned"));
    }

    @Test
    void getCardDownloadReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/applications/" + application.getId() + "/cards/download"))
                .andExpect(status().isUnauthorized());
    }
}
