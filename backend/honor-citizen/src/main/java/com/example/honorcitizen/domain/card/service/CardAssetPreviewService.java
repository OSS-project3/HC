package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.dto.CardDesignTemplatePreviewResponse;
import com.example.honorcitizen.domain.card.dto.ZodiacAnimalPreview;
import com.example.honorcitizen.domain.card.dto.ZodiacDesignPreviewResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.service.AdminAuthorizationService;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

// 십이간지 대표 4종 + 카드 디자인 앞뒤 원본 템플릿을 관리자에게 읽기 전용으로 보여준다
// (docs/collab/TODO.md "십이간지·카드 디자인 선택 이미지 미리보기" 2026-09-22 확정 정책).
// CardImageCompositor는 손대지 않는다 — 합성이 아니라 원본 파일 바이트만 base64로 돌려주면
// 되므로, 그 클래스의 AWT 팔레트 PNG 워크어라운드(loadImage)는 필요 없다. 후보 파일명 목록과
// VISITOR/1 미검수 가드만 동일 규칙으로 복제한다.
@Service
@RequiredArgsConstructor
public class CardAssetPreviewService {

    private static final String TEMPLATE_ROOT = "card-templates/";
    private static final List<String> FRONT_CANDIDATES = List.of("앞면.png", "대지 1.png");
    private static final List<String> BACK_CANDIDATES = List.of("뒷면.png", "대지 1 사본.png");

    private record ZodiacAnimal(String code, String name) {
    }

    private static final List<ZodiacAnimal> REPRESENTATIVE_ANIMALS = List.of(
            new ZodiacAnimal("RAT", "쥐"),
            new ZodiacAnimal("TIGER", "호랑이"),
            new ZodiacAnimal("DRAGON", "용"),
            new ZodiacAnimal("PIG", "돼지"));

    private final AdminAuthorizationService adminAuthorizationService;
    private final CardDesignRepository cardDesignRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UploadFileRepository uploadFileRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public ZodiacDesignPreviewResponse previewZodiacDesignSet(Long adminId, int designSet) {
        adminAuthorizationService.requireAdmin(adminId);
        if (designSet < 1 || designSet > 5) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<ZodiacAnimalPreview> animals = REPRESENTATIVE_ANIMALS.stream()
                .map(animal -> new ZodiacAnimalPreview(
                        animal.code(), animal.name(),
                        readClasspathImageAsBase64(
                                TEMPLATE_ROOT + "zodiac/" + designSet + "/" + animal.name() + ".png")))
                .toList();

        return new ZodiacDesignPreviewResponse(designSet, animals);
    }

    @Transactional(readOnly = true)
    public CardDesignTemplatePreviewResponse previewCardDesign(Long adminId, Long cardDesignId) {
        adminAuthorizationService.requireAdmin(adminId);

        CardDesign design = cardDesignRepository.findById(cardDesignId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND));
        if (!design.isActive()) {
            throw new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND);
        }
        CardType cardType = cardTypeRepository.findById(design.getCardTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND));

        String frontBase64;
        String backBase64;
        if (cardType.isStudentCard()) {
            frontBase64 = readUploadFileAsBase64(design.getTemplateFrontId());
            backBase64 = readUploadFileAsBase64(design.getTemplateBackId());
        } else {
            if (isUnverifiedDesign(cardType.getCode(), design.getDesignNumber())) {
                throw new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND);
            }
            String dir = TEMPLATE_ROOT + cardType.getCode().name() + "/" + design.getDesignNumber() + "/";
            frontBase64 = resolveAndReadCandidate(dir, FRONT_CANDIDATES);
            backBase64 = resolveAndReadCandidate(dir, BACK_CANDIDATES);
        }

        return new CardDesignTemplatePreviewResponse(cardDesignId, frontBase64, backBase64);
    }

    private boolean isUnverifiedDesign(CardTypeCode cardType, int designNumber) {
        return cardType == CardTypeCode.VISITOR && designNumber == 1;
    }

    private String resolveAndReadCandidate(String dir, List<String> candidates) {
        for (String candidate : candidates) {
            ClassPathResource resource = new ClassPathResource(dir + candidate);
            if (resource.exists()) {
                return encodeToBase64(resource);
            }
        }
        throw new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND);
    }

    private String readUploadFileAsBase64(Long uploadFileId) {
        if (uploadFileId == null) {
            throw new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND);
        }
        UploadFile uploadFile = uploadFileRepository.findById(uploadFileId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND));
        return Base64.getEncoder().encodeToString(storageService.download(uploadFile.getFilePath()));
    }

    private String readClasspathImageAsBase64(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return encodeToBase64(resource);
    }

    private String encodeToBase64(ClassPathResource resource) {
        try {
            return Base64.getEncoder().encodeToString(resource.getInputStream().readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
