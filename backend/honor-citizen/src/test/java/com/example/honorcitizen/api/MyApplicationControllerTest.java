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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MyApplicationControllerTest {

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

    private String ownerToken;
    private String otherToken;
    private CardType cardType;
    private Application application;

    @BeforeEach
    void setUp() {
        applicationMemberRepository.deleteAll();
        receiverRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(User.createNewUser("myapp-owner@example.com", "oauth-myapp-owner", "google", "Owner"));
        owner.agreeTerms(true, true, true);
        userRepository.save(owner);
        ownerToken = "Bearer " + jwtTokenProvider.generateAccessToken(owner.getId(), owner.getRole());

        User other = userRepository.save(User.createNewUser("myapp-other@example.com", "oauth-myapp-other", "google", "Other"));
        other.agreeTerms(true, true, true);
        userRepository.save(other);
        otherToken = "Bearer " + jwtTokenProvider.generateAccessToken(other.getId(), other.getRole());

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-myctrl", null, BigDecimal.valueOf(30000)));

        application = applicationRepository.save(Application.createIndividual(
                owner.getId(), "APP-2026-900001", cardType.getId(), IssueType.MOBILE, true, null, null));
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "홍길동", "hong@example.com", "010-1111-2222"));
        applicationMemberRepository.save(ApplicationMember.createIndividual(
                application.getId(), "Hong Gildong", LocalDate.of(1990, 1, 1), "KR",
                null, null, Gender.MALE, null, null, null, "photos/a.jpg"));
    }

    @Test
    void listReturnsOnlyOwnApplications() throws Exception {
        mockMvc.perform(get("/api/my/applications")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(application.getId()));
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/my/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listWithInvalidSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/my/applications")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailReturnsOwnApplication() throws Exception {
        mockMvc.perform(get("/api/my/applications/" + application.getId())
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(application.getId()))
                .andExpect(jsonPath("$.data.receiver").value(nullValue()));
    }

    @Test
    void detailForNonOwnerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/my/applications/" + application.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailForMissingApplicationReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/my/applications/999999")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNotFound());
    }
}
