package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.SchoolType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
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

// QA 체크리스트 7·8번: 템플릿 DB 변경(CardDesign/UploadFile)과 감사로그 저장이 같은 트랜잭션인지
// 검증한다(확정 정책). AdminActivityLogRepository.save()가 실패하면 DB 변경 전체가 롤백되고,
// 이번 요청에서 새로 올린 S3 파일도 보상 삭제돼야 한다 — 별도 Spring context에서
// AdminActivityLogRepository만 실패하는 Mock으로 교체해 재현한다(TokenSessionStoreSessionValidationTest와
// 동일한 방식).
@SpringBootTest
class SchoolCardTemplateServiceAuditLogFailureTest {

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

    @MockitoBean
    private StorageService storageService;
    @MockitoBean
    private AdminActivityLogRepository adminActivityLogRepository;

    private Long adminId;
    private Long schoolId;

    @BeforeEach
    void setUp() {
        cardDesignRepository.deleteAll();
        uploadFileRepository.deleteAll();
        schoolRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("template-audit-fail-admin@example.com", "oauth-template-audit-fail-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        cardTypeRepository.save(CardType.create(CardTypeCode.STUDENT, "학생증-audit-fail", null, BigDecimal.ZERO));

        School school = schoolRepository.save(School.create("감사로그실패고등학교", SchoolType.HIGH_SCHOOL));
        schoolId = school.getId();

        when(storageService.upload(anyString(), any())).thenReturn("stored-url");
        when(storageService.generatePresignedUrl(anyString(), anyLong())).thenReturn("https://presigned.example/x");
        when(adminActivityLogRepository.save(any(AdminActivityLog.class)))
                .thenThrow(new RuntimeException("DB 장애(감사로그 저장)"));
    }

    private MockMultipartFile landscapePng(String part) {
        try {
            BufferedImage image = new BufferedImage(980, 650, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new MockMultipartFile(part, part + ".png", "image/png", out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rollsBackDesignAndDeletesNewFilesWhenAuditLogSaveFails() {
        assertThatThrownBy(() -> schoolCardTemplateService.upload(
                adminId, schoolId, CardDesignOrientation.LANDSCAPE, landscapePng("front"), landscapePng("back")))
                .isInstanceOf(RuntimeException.class);

        assertThat(cardDesignRepository.count()).isEqualTo(0);
        assertThat(uploadFileRepository.count()).isEqualTo(0);
        verify(storageService, times(2)).delete(anyString());
    }
}
