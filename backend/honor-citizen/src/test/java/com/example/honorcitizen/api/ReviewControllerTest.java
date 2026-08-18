package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.IssueType;
import com.example.honorcitizen.domain.application.entity.Applicant;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.review.entity.Review;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

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
    private ApplicationMemberRepository applicationMemberRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    @MockitoBean
    private StorageService storageService;

    private String token;
    private String strangerToken;
    private User owner;
    private CardType cardType;

    private static final String REQUEST_JSON = """
            {
              "title": "제목",
              "applicationType": "INDIVIDUAL",
              "cardTypeId": %d,
              "authorName": "홍길동",
              "content": "내용입니다."
            }
            """;

    private static final String UPDATE_REQUEST_JSON = """
            {
              "title": "수정된 제목",
              "applicationType": "INDIVIDUAL",
              "cardTypeId": %d,
              "authorName": "수정된 작성자",
              "content": "수정된 내용",
              "removeImage": %s
            }
            """;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        applicationMemberRepository.deleteAll();
        applicantRepository.deleteAll();
        applicationRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.createNewUser("review-ctrl@example.com", "oauth-review-ctrl", "google", "리뷰어");
        user.agreeTerms(true, true, true);
        owner = userRepository.save(user);
        token = "Bearer " + jwtTokenProvider.generateAccessToken(owner.getId(), owner.getRole());

        User stranger = User.createNewUser("stranger-ctrl@example.com", "oauth-stranger-ctrl", "google", "타인");
        stranger.agreeTerms(true, true, true);
        stranger = userRepository.save(stranger);
        strangerToken = "Bearer " + jwtTokenProvider.generateAccessToken(stranger.getId(), stranger.getRole());

        cardType = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-review-ctrl", null, BigDecimal.valueOf(30000)));

        Application application = applicationRepository.save(Application.createIndividual(
                999L, "APP-2026-800001", cardType.getId(), IssueType.MOBILE, true, null, null));
        application.confirmPayment();
        application.startReview();
        application.approveToNaming();
        application.completeNaming();
        application.startProducing();
        application.markCardReady(java.time.LocalDateTime.now());
        applicationRepository.save(application);
        applicantRepository.save(Applicant.createIndividual(
                application.getId(), "리뷰어", "review-ctrl@example.com", "010-1111-2222"));

        when(storageService.upload(anyString(), any())).thenReturn("http://mock-storage/uploaded");
    }

    @Test
    void createReturnsCreatedWithReviewId() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());

        mockMvc.perform(multipart("/api/reviews")
                        .file(requestPart)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createWithImageUploadsToStorage() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile imagePart = new MockMultipartFile("image", "photo.jpg", "image/jpeg", jpegBytes());

        mockMvc.perform(multipart("/api/reviews")
                        .file(requestPart)
                        .file(imagePart)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createReturnsUnauthorizedWithoutToken() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());

        mockMvc.perform(multipart("/api/reviews")
                        .file(requestPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsBadRequestForMultipleImageParts() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", REQUEST_JSON.formatted(cardType.getId()).getBytes());
        MockMultipartFile first = new MockMultipartFile("image", "a.jpg", "image/jpeg", jpegBytes());
        MockMultipartFile second = new MockMultipartFile("image", "b.jpg", "image/jpeg", jpegBytes());

        mockMvc.perform(multipart("/api/reviews")
                        .file(requestPart)
                        .file(first)
                        .file(second)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void listSucceedsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(9));
    }

    @Test
    void listReturnsNotFoundForUnknownCardTypeFilter() throws Exception {
        mockMvc.perform(get("/api/reviews").param("cardTypeId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void listReturnsBadRequestForInvalidSearchType() throws Exception {
        mockMvc.perform(get("/api/reviews").param("searchType", "NOT_A_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void listReturnsBadRequestForNegativePage() throws Exception {
        mockMvc.perform(get("/api/reviews").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void detailSucceedsWithoutAuthenticationAndHidesEditDeletePermissions() throws Exception {
        Review review = reviewRepository.save(Review.create(999L, "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        mockMvc.perform(get("/api/reviews/{id}", review.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(review.getId()))
                .andExpect(jsonPath("$.data.canEdit").value(false))
                .andExpect(jsonPath("$.data.canDelete").value(false));
    }

    @Test
    void detailReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/reviews/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void deleteSucceedsForOwner() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        mockMvc.perform(delete("/api/reviews/{id}", review.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturnsForbiddenForNonOwnerNonAdmin() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        mockMvc.perform(delete("/api/reviews/{id}", review.getId())
                        .header("Authorization", strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void deleteReturnsUnauthorizedWithoutToken() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));

        mockMvc.perform(delete("/api/reviews/{id}", review.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateSucceedsForOwner() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", UPDATE_REQUEST_JSON.formatted(cardType.getId(), false).getBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/reviews/{id}", review.getId())
                        .file(requestPart)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateReturnsForbiddenForNonOwnerNonAdmin() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", UPDATE_REQUEST_JSON.formatted(cardType.getId(), false).getBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/reviews/{id}", review.getId())
                        .file(requestPart)
                        .header("Authorization", strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void updateReturnsUnauthorizedWithoutToken() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", null));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", UPDATE_REQUEST_JSON.formatted(cardType.getId(), false).getBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/reviews/{id}", review.getId())
                        .file(requestPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReturnsBadRequestWhenImageAndRemoveImageBothProvided() throws Exception {
        Review review = reviewRepository.save(Review.create(owner.getId(), "홍길동", "제목",
                ApplicationType.INDIVIDUAL, cardType.getId(), "내용", "reviews/old.jpg"));
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", "application/json", UPDATE_REQUEST_JSON.formatted(cardType.getId(), true).getBytes());
        MockMultipartFile imagePart = new MockMultipartFile("image", "new.jpg", "image/jpeg", jpegBytes());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/reviews/{id}", review.getId())
                        .file(requestPart)
                        .file(imagePart)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    private byte[] jpegBytes() {
        try {
            BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
