package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.dto.CardDesignTemplatePreviewResponse;
import com.example.honorcitizen.domain.card.dto.ZodiacDesignPreviewResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 십이간지·카드 디자인 선택 이미지 미리보기(2026-09-22 확정) — CardImageCompositor는 건드리지
// 않고, 원본 classpath/S3 파일 바이트를 base64로 그대로 돌려주는지만 검증한다.
@SpringBootTest
class CardAssetPreviewServiceTest {

    @Autowired
    private CardAssetPreviewService cardAssetPreviewService;
    @Autowired
    private CardDesignRepository cardDesignRepository;
    @Autowired
    private CardTypeRepository cardTypeRepository;
    @Autowired
    private UploadFileRepository uploadFileRepository;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StorageService storageService;

    private Long adminId;
    private Long userId;
    private Long honorKoreanTypeId;
    private Long visitorTypeId;
    private Long studentTypeId;

    @BeforeEach
    void setUp() {
        cardDesignRepository.deleteAll();
        uploadFileRepository.deleteAll();
        cardTypeRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(
                User.createOAuthUser("preview-admin@example.com", "oauth-preview-admin", "google", "Admin"));
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.save(admin);
        adminId = admin.getId();

        User user = userRepository.save(
                User.createOAuthUser("preview-user@example.com", "oauth-preview-user", "google", "User"));
        userId = user.getId();

        honorKoreanTypeId = cardTypeRepository.save(
                CardType.create(CardTypeCode.HONOR_KOREAN, "명예한국인증-preview", null, BigDecimal.ZERO)).getId();
        visitorTypeId = cardTypeRepository.save(
                CardType.create(CardTypeCode.VISITOR, "방문증-preview", null, BigDecimal.ZERO)).getId();
        studentTypeId = cardTypeRepository.save(
                CardType.create(CardTypeCode.STUDENT, "학생증-preview", null, BigDecimal.ZERO)).getId();
    }

    // --- 십이간지 미리보기 ---

    @Test
    void previewsFixedFourAnimalsInOrder() {
        ZodiacDesignPreviewResponse result = cardAssetPreviewService.previewZodiacDesignSet(adminId, 1);

        assertThat(result.designSet()).isEqualTo(1);
        assertThat(result.animals()).extracting("code").containsExactly("RAT", "TIGER", "DRAGON", "PIG");
        assertThat(result.animals()).extracting("name").containsExactly("쥐", "호랑이", "용", "돼지");
        assertThat(result.animals()).allSatisfy(animal ->
                assertThat(animal.imageBase64()).isNotBlank());
    }

    @Test
    void previewsAllFiveWhiteDesignSetsToo() {
        for (int set = 1; set <= 5; set++) {
            ZodiacDesignPreviewResponse result = cardAssetPreviewService.previewZodiacDesignSet(adminId, set);
            assertThat(result.animals()).hasSize(4);
        }
    }

    @Test
    void decodesEachAnimalImageAsValidPng() {
        ZodiacDesignPreviewResponse result = cardAssetPreviewService.previewZodiacDesignSet(adminId, 2);

        result.animals().forEach(animal -> {
            byte[] bytes = java.util.Base64.getDecoder().decode(animal.imageBase64());
            assertThat(bytes.length).isGreaterThan(8);
            assertThat(bytes[0]).isEqualTo((byte) 0x89);
            assertThat(bytes[1]).isEqualTo((byte) 'P');
        });
    }

    @Test
    void rejectsDesignSetOutOfRange() {
        assertThatThrownBy(() -> cardAssetPreviewService.previewZodiacDesignSet(adminId, 0))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> cardAssetPreviewService.previewZodiacDesignSet(adminId, 6))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsNonAdminForZodiacPreview() {
        assertThatThrownBy(() -> cardAssetPreviewService.previewZodiacDesignSet(userId, 1))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // --- 카드 디자인(일반) 미리보기 ---

    @Test
    void previewsBothSidesOfRegularCardDesign() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인1-preview", 1, CardDesignOrientation.LANDSCAPE, null, null, true));

        CardDesignTemplatePreviewResponse result = cardAssetPreviewService.previewCardDesign(adminId, design.getId());

        assertThat(result.cardDesignId()).isEqualTo(design.getId());
        assertThat(result.frontImageBase64()).isNotBlank();
        assertThat(result.backImageBase64()).isNotBlank();
    }

    @Test
    void rejectsUnverifiedVisitorDesignOne() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                visitorTypeId, "방문증-미검수", 1, CardDesignOrientation.LANDSCAPE, null, null, true));

        assertThatThrownBy(() -> cardAssetPreviewService.previewCardDesign(adminId, design.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    @Test
    void previewsVisitorDesignTwoNormally() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                visitorTypeId, "방문증-정상", 2, CardDesignOrientation.LANDSCAPE, null, null, true));

        CardDesignTemplatePreviewResponse result = cardAssetPreviewService.previewCardDesign(adminId, design.getId());

        assertThat(result.frontImageBase64()).isNotBlank();
        assertThat(result.backImageBase64()).isNotBlank();
    }

    @Test
    void rejectsUnknownCardDesignId() {
        assertThatThrownBy(() -> cardAssetPreviewService.previewCardDesign(adminId, 999999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    @Test
    void rejectsInactiveCardDesign() {
        CardDesign inactive = CardDesign.create(
                honorKoreanTypeId, "디자인-비활성", 5, CardDesignOrientation.LANDSCAPE, null, null, false);
        inactive.deactivate();
        CardDesign saved = cardDesignRepository.save(inactive);

        assertThatThrownBy(() -> cardAssetPreviewService.previewCardDesign(adminId, saved.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    @Test
    void rejectsNonAdminForCardDesignPreview() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                honorKoreanTypeId, "디자인-권한테스트", 1, CardDesignOrientation.LANDSCAPE, null, null, true));

        assertThatThrownBy(() -> cardAssetPreviewService.previewCardDesign(userId, design.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // --- 학생증(S3) 카드 디자인 미리보기 ---

    @Test
    void previewsStudentCardDesignFromUploadedTemplates() {
        UploadFile front = uploadFileRepository.save(UploadFile.create(
                "front.png", "stored-front.png", "school-templates/5/front.png",
                UploadFileType.CARD_IMAGE, "image/png", 100L));
        UploadFile back = uploadFileRepository.save(UploadFile.create(
                "back.png", "stored-back.png", "school-templates/5/back.png",
                UploadFileType.CARD_IMAGE, "image/png", 100L));
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                studentTypeId, "테스트대학교-가로-preview", 10, CardDesignOrientation.LANDSCAPE,
                front.getId(), back.getId(), true, 5L));

        byte[] frontBytes = {1, 2, 3};
        byte[] backBytes = {4, 5, 6};
        when(storageService.download("school-templates/5/front.png")).thenReturn(frontBytes);
        when(storageService.download("school-templates/5/back.png")).thenReturn(backBytes);

        CardDesignTemplatePreviewResponse result = cardAssetPreviewService.previewCardDesign(adminId, design.getId());

        assertThat(java.util.Base64.getDecoder().decode(result.frontImageBase64())).isEqualTo(frontBytes);
        assertThat(java.util.Base64.getDecoder().decode(result.backImageBase64())).isEqualTo(backBytes);
    }

    @Test
    void rejectsStudentCardDesignWithMissingTemplateReference() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                studentTypeId, "테스트대학교-템플릿없음", 11, CardDesignOrientation.LANDSCAPE,
                null, null, true, 5L));

        assertThatThrownBy(() -> cardAssetPreviewService.previewCardDesign(adminId, design.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    @Test
    void rejectsStudentCardDesignWhenUploadFileRowMissing() {
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                studentTypeId, "테스트대학교-UploadFile유실", 12, CardDesignOrientation.LANDSCAPE,
                999999L, 999999L, true, 5L));

        assertThatThrownBy(() -> cardAssetPreviewService.previewCardDesign(adminId, design.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    @Test
    void previewDoesNotCallStorageUploadOrDelete() {
        UploadFile front = uploadFileRepository.save(UploadFile.create(
                "front.png", "stored-front.png", "school-templates/6/front.png",
                UploadFileType.CARD_IMAGE, "image/png", 100L));
        UploadFile back = uploadFileRepository.save(UploadFile.create(
                "back.png", "stored-back.png", "school-templates/6/back.png",
                UploadFileType.CARD_IMAGE, "image/png", 100L));
        CardDesign design = cardDesignRepository.save(CardDesign.create(
                studentTypeId, "테스트대학교-감사로그없음", 13, CardDesignOrientation.LANDSCAPE,
                front.getId(), back.getId(), true, 6L));
        when(storageService.download(anyString())).thenReturn(new byte[]{9});

        cardAssetPreviewService.previewCardDesign(adminId, design.getId());

        org.mockito.Mockito.verify(storageService, org.mockito.Mockito.never())
                .upload(anyString(), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(storageService, org.mockito.Mockito.never()).delete(anyString());
    }
}
