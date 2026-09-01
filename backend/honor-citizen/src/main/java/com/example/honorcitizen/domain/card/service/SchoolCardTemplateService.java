package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.CardDesignOrientation;
import com.example.honorcitizen.common.enums.UserRole;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.card.dto.SchoolCardTemplateResponse;
import com.example.honorcitizen.domain.card.entity.CardDesign;
import com.example.honorcitizen.domain.card.repository.CardDesignRepository;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.domain.school.service.SchoolService;
import com.example.honorcitizen.domain.uploadfile.entity.UploadFile;
import com.example.honorcitizen.domain.uploadfile.repository.UploadFileRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.service.UserService;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 4-D: 관리자가 학교별 학생증 카드 템플릿(앞/뒤)을 배포 없이 등록·교체하는 API의 오케스트레이션.
// CardGenerationService와 같은 이유로 이 클래스 자체는 비-transactional이다 — S3 업로드(외부 I/O,
// 느림)를 DB 트랜잭션 밖에서 하고, DB 반영은 SchoolCardTemplatePersistenceService의 짧은
// @Transactional에만 맡긴다.
@Service
@RequiredArgsConstructor
public class SchoolCardTemplateService {

    private static final long PREVIEW_URL_EXPIRY_SECONDS = 60 * 60L;

    private final CardDesignRepository cardDesignRepository;
    private final SchoolService schoolService;
    private final UserService userService;
    private final StorageService storageService;
    private final SchoolCardTemplatePersistenceService persistenceService;
    private final AdminActivityLogRepository adminActivityLogRepository;
    private final SchoolCardTemplateValidator validator;
    private final UploadFileRepository uploadFileRepository;

    @Transactional(readOnly = true)
    public SchoolCardTemplateResponse get(Long adminId, Long schoolId, CardDesignOrientation orientation) {
        validateAdmin(adminId);
        schoolService.getSchoolNameOrThrow(schoolId); // 존재 확인(4-A~4-D 공통 School 존재 검증 패턴)

        List<CardDesign> designs = cardDesignRepository.findBySchoolIdAndOrientationAndActiveOrderByDesignNumber(
                schoolId, orientation, true);
        if (designs.isEmpty()) {
            return null; // 미등록 — Controller가 data: null로 응답한다(관리자 화면이 항상 마주치는 정상 상태).
        }
        CardDesign design = designs.get(0);
        return toResponse(design);
    }

    public SchoolCardTemplateResponse upload(Long adminId, Long schoolId, CardDesignOrientation orientation,
            MultipartFile front, MultipartFile back) {
        validateAdmin(adminId);
        String schoolName = schoolService.getSchoolNameOrThrow(schoolId);
        if (orientation == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validator.validate(front, orientation);
        validator.validate(back, orientation);

        List<String> uploadedKeys = new ArrayList<>();
        try {
            SchoolCardTemplatePersistenceService.UploadedTemplate frontMeta = uploadToStorage(
                    schoolId, orientation, "front", front, uploadedKeys);
            SchoolCardTemplatePersistenceService.UploadedTemplate backMeta = uploadToStorage(
                    schoolId, orientation, "back", back, uploadedKeys);

            SchoolCardTemplatePersistenceService.PersistResult persisted =
                    persistenceService.upsert(schoolId, schoolName, orientation, frontMeta, backMeta);

            // DB 반영이 실제로 커밋된 뒤에만(persist()가 예외 없이 반환한 시점) 기존 파일을 지운다 —
            // 신규 선업로드 -> commit -> 기존 후삭제(SchoolCardTemplatePersistenceService 클래스 주석 참고).
            deleteQuietly(persisted.oldFrontPath());
            deleteQuietly(persisted.oldBackPath());

            adminActivityLogRepository.save(AdminActivityLog.create(adminId, AdminActivityLog.CARD_TEMPLATE_UPLOADED,
                    persisted.cardDesignId(), "학생증 카드 템플릿 등록: schoolId=" + schoolId + ", orientation=" + orientation));

            CardDesign design = cardDesignRepository.findById(persisted.cardDesignId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CARD_DESIGN_NOT_FOUND));
            return toResponse(design);
        } catch (RuntimeException e) {
            // 이번 요청에서 새로 올라간 key만 역순으로 보상 삭제 — 실패 이전 상태(기존 템플릿 유무 포함)가
            // 그대로 보존된다(CardGenerationService와 동일 패턴).
            deleteUploadedKeysReversed(uploadedKeys);
            throw e;
        }
    }

    // key는 서버가 직접 생성한다(원본 파일명을 저장 경로로 쓰지 않는다 — arch.md 15). originalName만
    // UploadFile 메타데이터의 표시용 필드로 남긴다.
    private SchoolCardTemplatePersistenceService.UploadedTemplate uploadToStorage(Long schoolId,
            CardDesignOrientation orientation, String side, MultipartFile file, List<String> uploadedKeys) {
        String key = buildKey(schoolId, orientation, side);
        storageService.upload(key, file);
        uploadedKeys.add(key);
        return new SchoolCardTemplatePersistenceService.UploadedTemplate(
                file.getOriginalFilename(), key, key, file.getContentType(), file.getSize());
    }

    private String buildKey(Long schoolId, CardDesignOrientation orientation, String side) {
        return "card-templates/STUDENT/" + schoolId + "/" + orientation.name() + "/" + side + "-"
                + UUID.randomUUID() + ".png";
    }

    private void deleteUploadedKeysReversed(List<String> keys) {
        for (int i = keys.size() - 1; i >= 0; i--) {
            deleteQuietly(keys.get(i));
        }
    }

    private void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            storageService.delete(key);
        } catch (RuntimeException ignored) {
            // 보상/정리 삭제 실패는 원 예외를 덮지 않는다 — 파일 1건이 고아로 남을 수 있음을 감수한다
            // (TODO.md 4-D 완료조건 — 자동 정리 재시도 큐는 이번 스코프에 없음).
        }
    }

    private SchoolCardTemplateResponse toResponse(CardDesign design) {
        String frontUrl = storageService.generatePresignedUrl(
                filePathOf(design.getTemplateFrontId()), PREVIEW_URL_EXPIRY_SECONDS);
        String backUrl = storageService.generatePresignedUrl(
                filePathOf(design.getTemplateBackId()), PREVIEW_URL_EXPIRY_SECONDS);
        return new SchoolCardTemplateResponse(design.getId(), frontUrl, backUrl);
    }

    private String filePathOf(Long uploadFileId) {
        // upsert()가 항상 두 UploadFile을 함께 저장/연결하므로 이 시점엔 항상 존재한다.
        return uploadFileRepository.findById(uploadFileId)
                .map(UploadFile::getFilePath)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private void validateAdmin(Long adminId) {
        User admin = userService.findById(adminId);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
