package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 4-D: 관리자 학생증 카드 템플릿 업로드 API — HTTP 계층(인증/권한/multipart 바인딩)만 검증한다.
// 비즈니스 로직 분기는 SchoolCardTemplateServiceTest의 역할이라 여기서 중복 검증하지 않는다(RULES.md §8).
@SpringBootTest
@AutoConfigureMockMvc
class AdminSchoolCardTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;

    @MockitoBean
    private StorageService storageService;

    private String adminToken;
    private String userToken;
    private Long schoolId;

    @BeforeEach
    void setUp() {
        cardDesignRepository.deleteAll();
        uploadFileRepository.deleteAll();
        schoolRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.createOAuthUser("template-ctl-admin@example.com", "oauth-template-ctl-admin", "google", "관리자"));
        // SecurityConfig의 hasRole("ADMIN")은 JWT 클레임만 보지만, SchoolCardTemplateService.validateAdmin()이
        // DB의 실제 User.role도 재확인한다(CardDesignService와 동일 패턴) — 둘 다 맞춰야 한다.
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(admin.getId(), UserRole.ADMIN);

        User user = userRepository.save(User.createOAuthUser("template-ctl-user@example.com", "oauth-template-ctl-user", "google", "일반사용자"));
        userToken = "Bearer " + jwtTokenProvider.generateAccessToken(user.getId(), UserRole.USER);

        cardTypeRepository.save(CardType.create(CardTypeCode.STUDENT, "학생증-template-ctl", null, BigDecimal.ZERO));
        School school = schoolRepository.save(School.create("전주고등학교", SchoolType.HIGH_SCHOOL));
        schoolId = school.getId();

        when(storageService.upload(anyString(), any())).thenReturn("stored-url");
        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://presigned.example/x");
    }

    private MockMultipartFile landscapePng(String part) throws Exception {
        BufferedImage image = new BufferedImage(980, 650, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile(part, part + ".png", "image/png", out.toByteArray());
    }

    @Test
    void getReturnsNullDataForAdminWhenUnregistered() throws Exception {
        mockMvc.perform(get("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .param("orientation", "LANDSCAPE")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .param("orientation", "LANDSCAPE")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .param("orientation", "LANDSCAPE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadSucceedsForAdminAndGetReflectsIt() throws Exception {
        mockMvc.perform(multipart("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .file(landscapePng("front"))
                        .file(landscapePng("back"))
                        .param("orientation", "LANDSCAPE")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cardDesignId").exists())
                .andExpect(jsonPath("$.data.frontPreviewUrl").exists())
                .andExpect(jsonPath("$.data.backPreviewUrl").exists());

        mockMvc.perform(get("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .param("orientation", "LANDSCAPE")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardDesignId").exists());
    }

    @Test
    void uploadReturnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(multipart("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .file(landscapePng("front"))
                        .file(landscapePng("back"))
                        .param("orientation", "LANDSCAPE")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadReturnsBadRequestWhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/admin/schools/{schoolId}/card-template", schoolId)
                        .file(landscapePng("front"))
                        .param("orientation", "LANDSCAPE")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());
    }
}
