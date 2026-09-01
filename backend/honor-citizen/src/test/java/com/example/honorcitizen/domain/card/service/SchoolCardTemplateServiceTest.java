package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.dto.SchoolCardTemplateResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.school.entity.School;
import com.example.honorcitizen.domain.school.repository.SchoolRepository;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 4-D: 관리자 학생증 카드 템플릿 업로드 API — StorageService만 Mock(S3 실접근 없이), 나머지는
// 실제 DB(CardGenerationServiceTest/CardDesignServiceTest와 동일 스타일).
@SpringBootTest
class SchoolCardTemplateServiceTest {

    @Autowired
    private SchoolCardTemplateService schoolCardTemplateService;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private Long userId;
    private Long schoolId;

    @BeforeEach
    void setUp() {
        adminActivityLogRepository.deleteAll();
        cardDesignRepository.deleteAll();
        uploadFileRepository.deleteAll();
        schoolRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("template-admin@example.com", "oauth-template-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("template-user@example.com", "oauth-template-user", "google", "User"));
        userId = user.getId();

        cardTypeRepository.save(CardType.create(CardTypeCode.STUDENT, "학생증-template", null, BigDecimal.ZERO));

        School school = schoolRepository.save(School.create("경기외국어고등학교", SchoolType.HIGH_SCHOOL));
        schoolId = school.getId();

        when(storageService.upload(anyString(), any())).thenReturn("stored-url");
        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://presigned.example/x");
    }

    private MockMultipartFile landscapePng(String part) {
        return pngFile(part, 980, 650);
    }

    private MockMultipartFile portraitPng(String part) {
        return pngFile(part, 650, 980);
    }

    private MockMultipartFile pngFile(String part, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new MockMultipartFile(part, part + ".png", "image/png", out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getReturnsNullWhenNoTemplateRegistered() {
        SchoolCardTemplateResponse result = schoolCardTemplateService.get(adminId, schoolId, CardDesignOrientation.LANDSCAPE);

        assertThat(result).isNull();
    }

    @Test
    void uploadCreatesNewCardDesignWhenNoneExists() {
        SchoolCardTemplateResponse result = schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, landscapePng("front"), landscapePng("back"));

        assertThat(result.cardDesignId()).isNotNull();
        assertThat(result.frontPreviewUrl()).isEqualTo("https://presigned.example/x");
        assertThat(result.backPreviewUrl()).isEqualTo("https://presigned.example/x");

        CardDesign saved = cardDesignRepository.findById(result.cardDesignId()).orElseThrow();
        assertThat(saved.getSchoolId()).isEqualTo(schoolId);
        assertThat(saved.getOrientation()).isEqualTo(CardDesignOrientation.LANDSCAPE);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isDefault()).isFalse();
        assertThat(saved.getName()).isEqualTo("경기외국어고등학교 학생증(가로형)");
        assertThat(saved.getTemplateFrontId()).isNotNull();
        assertThat(saved.getTemplateBackId()).isNotNull();
        assertThat(uploadFileRepository.count()).isEqualTo(2);

        assertThat(adminActivityLogRepository.findAll()).hasSize(1);
    }

    @Test
    void getReturnsRegisteredTemplateAfterUpload() {
        schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.PORTRAIT, portraitPng("front"), portraitPng("back"));

        SchoolCardTemplateResponse result = schoolCardTemplateService.get(adminId, schoolId, CardDesignOrientation.PORTRAIT);

        assertThat(result).isNotNull();
        assertThat(result.frontPreviewUrl()).isNotBlank();
        assertThat(result.backPreviewUrl()).isNotBlank();
    }

    @Test
    void uploadReplacesExistingDesignKeepingSameId() {
        SchoolCardTemplateResponse first = schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, landscapePng("front"), landscapePng("back"));
        CardDesign firstDesign = cardDesignRepository.findById(first.cardDesignId()).orElseThrow();
        Long oldFrontId = firstDesign.getTemplateFrontId();
        Long oldBackId = firstDesign.getTemplateBackId();

        SchoolCardTemplateResponse second = schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, landscapePng("front"), landscapePng("back"));

        assertThat(second.cardDesignId()).isEqualTo(first.cardDesignId());
        CardDesign updated = cardDesignRepository.findById(second.cardDesignId()).orElseThrow();
        assertThat(updated.getTemplateFrontId()).isNotEqualTo(oldFrontId);
        assertThat(updated.getTemplateBackId()).isNotEqualTo(oldBackId);
        // 기존 UploadFile row는 삭제됐어야 한다(고아 row 방지, 4-D 정정 사항).
        assertThat(uploadFileRepository.findById(oldFrontId)).isEmpty();
        assertThat(uploadFileRepository.findById(oldBackId)).isEmpty();
        assertThat(uploadFileRepository.count()).isEqualTo(2);
        // 활성 디자인이 여전히 schoolId+orientation당 1개뿐이다.
        assertThat(cardDesignRepository.findBySchoolIdAndOrientationAndActiveOrderByDesignNumber(
                schoolId, CardDesignOrientation.LANDSCAPE, true)).hasSize(1);
        // 기존 S3 오브젝트도 커밋 후 정리 시도했어야 한다.
        verify(storageService, times(2)).delete(anyString());
    }

    @Test
    void uploadRejectsNonAdmin() {
        assertThatThrownBy(() -> schoolCardTemplateService.upload(
                userId, schoolId, CardDesignOrientation.LANDSCAPE, landscapePng("front"), landscapePng("back")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void uploadRejectsUnknownSchool() {
        assertThatThrownBy(() -> schoolCardTemplateService.upload(
                adminId, 999999L, CardDesignOrientation.LANDSCAPE, landscapePng("front"), landscapePng("back")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.SCHOOL_NOT_FOUND));
    }

    @Test
    void uploadRejectsNonPngFile() {
        MockMultipartFile jpg = new MockMultipartFile("front", "front.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, jpg, landscapePng("back")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE));

        // 검증은 업로드 전에 끝나야 한다 — 실패 시 아무것도 안 만들어져 있어야 한다.
        assertThat(cardDesignRepository.count()).isEqualTo(0);
        assertThat(uploadFileRepository.count()).isEqualTo(0);
    }

    @Test
    void uploadRejectsWrongOrientationRatio() {
        // LANDSCAPE 방향인데 세로가 더 긴 이미지를 보냄.
        MockMultipartFile portraitShapedFile = pngFile("front", 650, 980);

        assertThatThrownBy(() -> schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, portraitShapedFile, landscapePng("back")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_TEMPLATE_INVALID_RESOLUTION));
    }

    @Test
    void uploadRejectsTooSmallResolution() {
        MockMultipartFile tiny = pngFile("front", 300, 200);

        assertThatThrownBy(() -> schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, tiny, landscapePng("back")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_TEMPLATE_INVALID_RESOLUTION));
    }
}
