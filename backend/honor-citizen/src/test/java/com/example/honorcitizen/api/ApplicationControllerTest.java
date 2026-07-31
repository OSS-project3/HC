package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.Gender;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.photo.entity.PhotoUpload;
import com.example.honorcitizen.domain.photo.repository.PhotoUploadRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private ApplicationRepository applicationRepository;

    @Autowired
    private PhotoUploadRepository photoUploadRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User agreedUser;
    private String agreedUserToken;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        photoUploadRepository.deleteAll();
        userRepository.deleteAll();

        agreedUser = User.createNewUser("john@example.com", "oauth-john", "google", "John");
        agreedUser.agreeTerms(true, true, true);
        agreedUser = userRepository.save(agreedUser);
        agreedUserToken = bearerToken(agreedUser);

        photoUploadRepository.save(PhotoUpload.create(
                agreedUser.getId(),
                "photo_abc123",
                "photos/preview/photo_abc123.jpg",
                "https://test.s3.amazonaws.com/photos/preview/photo_abc123.jpg",
                LocalDateTime.now().plusHours(1)));
    }

    @Test
    void createSingleReturnsCreatedResponse() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", agreedUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nameEn": "John Smith",
                                  "nationality": "US",
                                  "birthDate": "1990-05-15",
                                  "birthTime": "14:30",
                                  "birthRegion": "New York, USA",
                                  "gender": "MALE",
                                  "photoId": "photo_abc123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").isNumber())
                .andExpect(jsonPath("$.data.initialStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void createSingleReturnsTermsNotAgreedWhenUserHasNotAgreedTerms() throws Exception {
        User notAgreedUser = userRepository.save(
                User.createNewUser("not-agreed@example.com", "oauth-not-agreed", "google", "No Terms"));

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearerToken(notAgreedUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errorCode").value("TERMS_NOT_AGREED"))
                .andExpect(jsonPath("$.errorMessage").value("약관에 동의해주세요."));
    }

    @Test
    void createSingleReturnsDuplicateApplicationWhenProgressApplicationExists() throws Exception {
        saveApplication(agreedUser.getId(), ApplicationStatus.PENDING);

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", agreedUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_APPLICATION"))
                .andExpect(jsonPath("$.errorMessage").value("진행 중인 신청이 이미 존재합니다."));
    }

    @Test
    void getMyApplicationsReturnsApplicationList() throws Exception {
        saveApplication(agreedUser.getId(), ApplicationStatus.PENDING);

        mockMvc.perform(get("/api/my/applications")
                        .header("Authorization", agreedUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applications[0].applicationId").isNumber())
                .andExpect(jsonPath("$.data.applications[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.applications[0].koreanName").doesNotExist())
                .andExpect(jsonPath("$.data.applications[0].cardNumber").doesNotExist());
    }

    @Test
    void getMyApplicationDetailReturnsNotFoundForOtherUsersApplication() throws Exception {
        User otherUser = userRepository.save(
                User.createNewUser("other@example.com", "oauth-other", "google", "Other"));
        Application otherApplication = saveApplication(otherUser.getId(), ApplicationStatus.PENDING);

        mockMvc.perform(get("/api/my/applications/{applicationId}", otherApplication.getId())
                        .header("Authorization", agreedUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void reuploadPhotoReturnsPendingWhenApplicationIsPhotoRejected() throws Exception {
        Application application = savePhotoRejectedApplication(agreedUser.getId());
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "new-photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image".getBytes());

        mockMvc.perform(patchMultipart("/api/my/applications/" + application.getId() + "/photo")
                        .file(photo)
                        .header("Authorization", agreedUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").value(application.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.photoUrl").value(org.hamcrest.Matchers.containsString("photos/preview/photo_")));
    }

    @Test
    void reuploadPhotoReturnsInvalidStatusTransitionWhenStatusIsNotPhotoRejected() throws Exception {
        Application application = saveApplication(agreedUser.getId(), ApplicationStatus.PENDING);
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "new-photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image".getBytes());

        mockMvc.perform(patchMultipart("/api/my/applications/" + application.getId() + "/photo")
                        .file(photo)
                        .header("Authorization", agreedUserToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.errorMessage").value("허용되지 않는 상태 전이입니다."));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    }

    private String validCreateRequest() {
        return """
                {
                  "nameEn": "John Smith",
                  "nationality": "US",
                  "birthDate": "1990-05-15",
                  "birthTime": "14:30",
                  "birthRegion": "New York, USA",
                  "gender": "MALE",
                  "photoId": "photo_abc123"
                }
                """;
    }

    private Application saveApplication(Long userId, ApplicationStatus status) {
        // TODO(2026-07-31): entryDate/address/cardType 임시 null — Application 도메인 재설계 대상이라 우선 컴파일만 통과시킴 (ApplicationService.java와 동일 조치)
        Application application = Application.createSingle(
                userId,
                "John Smith",
                "US",
                LocalDate.of(1990, 5, 15),
                LocalTime.of(14, 30),
                "New York, USA",
                Gender.MALE,
                "photo_abc123",
                "photos/preview/photo_abc123",
                null,
                null,
                null);

        if (status == ApplicationStatus.REVIEWING) {
            application.startReview();
        } else if (status == ApplicationStatus.CANCELLED) {
            application.cancel();
        }

        return applicationRepository.save(application);
    }

    private Application savePhotoRejectedApplication(Long userId) {
        // TODO(2026-07-31): entryDate/address/cardType 임시 null — Application 도메인 재설계 대상이라 우선 컴파일만 통과시킴 (ApplicationService.java와 동일 조치)
        Application application = Application.createSingle(
                userId,
                "John Smith",
                "US",
                LocalDate.of(1990, 5, 15),
                LocalTime.of(14, 30),
                "New York, USA",
                Gender.MALE,
                "photo_abc123",
                "photos/preview/photo_abc123",
                null,
                null,
                null);
        application.startReview();
        application.rejectPhoto();
        return applicationRepository.save(application);
    }

    private MockMultipartHttpServletRequestBuilder patchMultipart(String url) {
        MockMultipartHttpServletRequestBuilder builder = multipart(url);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });
        return builder;
    }
}
