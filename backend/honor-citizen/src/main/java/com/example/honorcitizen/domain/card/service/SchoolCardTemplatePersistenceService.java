package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.CardTypeCode;
import com.example.honorcitizen.common.enums.UploadFileType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.entity.CardType;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.card.repository.CardTypeRepository;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 4-D: SchoolCardTemplateService(비-transactional 오케스트레이션)가 S3 업로드까지 끝낸 뒤 이
// 짧은 트랜잭션에만 DB 반영을 맡긴다(CardGenerationService/CardGenerationPersistenceService와
// 완전히 같은 2-서비스 분리 패턴 — Card 모듈 안의 기존 선례를 그대로 재사용, ApplicationService의
// registerS3CleanupAfterTransaction과 목적은 같지만 이 프로젝트 안에 이미 있는 더 가까운 선례를
// 골랐다). 이 메서드가 예외 없이 반환하면(=트랜잭션이 실제로 커밋된 뒤에만) 호출부가 old 파일들을
// S3에서 지운다 — 스프링 프록시 경계 덕분에 별도 TransactionSynchronizationManager 없이도
// "커밋된 뒤에만" 이 보장이 성립한다(CardGenerationService의 같은 패턴 참고).
@Service
@RequiredArgsConstructor
class SchoolCardTemplatePersistenceService {

    private final CardDesignRepository cardDesignRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UploadFileRepository uploadFileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    record UploadedTemplate(String originalName, String storedName, String filePath, String mimeType, long fileSize) {
    }

    record PersistResult(Long cardDesignId, String oldFrontPath, String oldBackPath) {
    }

    @Transactional
    PersistResult upsert(Long schoolId, String schoolName, CardDesignOrientation orientation,
            UploadedTemplate front, UploadedTemplate back) {
        UploadFile newFront = uploadFileRepository.save(toUploadFile(front));
        UploadFile newBack = uploadFileRepository.save(toUploadFile(back));

        List<CardDesign> existing = cardDesignRepository.findBySchoolIdAndOrientationAndActiveOrderByDesignNumber(
                schoolId, orientation, true);

        if (!existing.isEmpty()) {
            CardDesign design = existing.get(0);
            Long oldFrontId = design.getTemplateFrontId();
            Long oldBackId = design.getTemplateBackId();
            String oldFrontPath = pathOf(oldFrontId);
            String oldBackPath = pathOf(oldBackId);

            design.replaceTemplates(newFront.getId(), newBack.getId());
            deleteIfPresent(oldFrontId);
            deleteIfPresent(oldBackId);

            return new PersistResult(design.getId(), oldFrontPath, oldBackPath);
        }

        Long cardTypeId = cardTypeRepository.findByCode(CardTypeCode.STUDENT)
                .map(CardType::getId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_TYPE_NOT_FOUND));
        String name = schoolName + " 학생증(" + orientationLabel(orientation) + ")";
        int designNumber = nextDesignNumber();
        CardDesign design = CardDesign.create(cardTypeId, name, designNumber, orientation,
                newFront.getId(), newBack.getId(), false, schoolId);
        cardDesignRepository.save(design);

        return new PersistResult(design.getId(), null, null);
    }

    private UploadFile toUploadFile(UploadedTemplate metadata) {
        return UploadFile.create(metadata.originalName(), metadata.storedName(), metadata.filePath(),
                UploadFileType.CARD_IMAGE, metadata.mimeType(), metadata.fileSize());
    }

    private String pathOf(Long uploadFileId) {
        if (uploadFileId == null) {
            return null;
        }
        return uploadFileRepository.findById(uploadFileId).map(UploadFile::getFilePath).orElse(null);
    }

    private void deleteIfPresent(Long uploadFileId) {
        if (uploadFileId != null) {
            uploadFileRepository.deleteById(uploadFileId);
        }
    }

    private String orientationLabel(CardDesignOrientation orientation) {
        return orientation == CardDesignOrientation.LANDSCAPE ? "가로형" : "세로형";
    }

    // application_seq(ApplicationService.nextApplicationSequence)와 같은 이유로 시퀀스를 쓴다 —
    // 서로 다른 두 학교가 동시에 신규 등록해도 중복 없는 번호를 보장한다.
    private int nextDesignNumber() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('student_card_design_seq')")
                .getSingleResult()).intValue();
    }
}
