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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationPhotoControllerTest {

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
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.createOAuthUser("photo-ctrl@example.com", "oauth-photo-ctrl", "google", "Photo"));
        token = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        CardType cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-photo-ctrl", null, BigDecimal.valueOf(30000)));

        application = applicationRepository.save(Application.createIndividual(
                user.getId(), "APP-2026-300001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(application.getId(), "홍길동", "photo-ctrl@example.com", "010-1234-5678"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "US",
                null, null, Gender.MALE, null, null, null, "photos/old.jpg"));
        application.confirmPayment();
        application.startReview();
        application.rejectPhoto("사진이 흐립니다.");
        applicationRepository.save(application);

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void reuploadPhotoReturnsReviewingStatus() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "new-bytes".getBytes());
        var builder = multipart("/api/applications/" + application.getId() + "/photo").file(photo);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(builder.header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REVIEWING"));
    }

    @Test
    void reuploadPhotoReturnsUnauthorizedWithoutToken() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "new.jpg", "image/jpeg", "new-bytes".getBytes());
        var builder = multipart("/api/applications/" + application.getId() + "/photo").file(photo);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });

        mockMvc.perform(builder)
                .andExpect(status().isUnauthorized());
    }
}
