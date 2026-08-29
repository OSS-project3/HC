package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.card.dto.CardGenerateResponse;
import com.example.honorcitizen.domain.card.dto.CardPreviewRequest;
import com.example.honorcitizen.domain.log.entity.AdminActivityLog;
import com.example.honorcitizen.domain.log.repository.AdminActivityLogRepository;
import com.example.honorcitizen.infra.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

// 3. 카드 생성·저장 — 최소 버전(2026-08-30) 오케스트레이션 서비스. 이 클래스 자체는 비-transactional —
// 렌더링·S3 업로드처럼 오래 걸리거나 외부 I/O가 섞인 작업을 DB 트랜잭션 밖에서 수행하고, DB 반영은
// CardGenerationPersistenceService의 짧은 @Transactional에만 맡긴다(3-A "서비스 구조 분리").
// FRONT/BACK은 하나의 결과 세트로 취급 — 렌더링·업로드·DB반영 중 어디서 실패해도 이번 요청에서
// 새로 올라간 S3 key를 역순으로 보상 삭제한다.
@Service
@RequiredArgsConstructor
public class CardGenerationService {

    // Generate 전용 상태 게이트(2026-08-30): PRODUCTION_READY, 또는 PRODUCING이면서 아직
    // cardReadyAt이 찍히지 않은 경우만 허용한다 — cardReadyAt이 찍힌 이후(카드가 이미 "준비 완료"로
    // 선언되어 사용자가 조회 가능한 상태)에는 COMPLETED와 마찬가지로 재생성도 거절한다.
    private static final Predicate<Application> GENERATE_STATUS_GATE = application ->
            application.getStatus() == ApplicationStatus.PRODUCTION_READY
                    || (application.getStatus() == ApplicationStatus.PRODUCING && application.getCardReadyAt() == null);

    private final CardRenderPreparation preparation;
    private final CardGenerationPersistenceService persistenceService;
    private final StorageService storageService;
    private final AdminActivityLogRepository adminActivityLogRepository;

    // 동일 요청을 다시 호출해도 멱등 처리(스킵)하지 않는다 — 상태 게이트를 통과하는 한 매번
    // 새로 렌더링·업로드·재확정한다(2026-08-30 정책, "동일 요청 재호출 = 재생성").
    public CardGenerateResponse generate(Long adminId, Long applicationId, Long memberId, CardPreviewRequest request) {
        CardRenderPreparation.CardRenderResult result =
                preparation.prepare(adminId, applicationId, memberId, request, GENERATE_STATUS_GATE);

        String applicationNumber = result.application().getApplicationNumber();
        String frontKey = buildKey(applicationNumber, memberId, "front");
        String backKey = buildKey(applicationNumber, memberId, "back");
        List<String> uploadedKeys = new ArrayList<>();
        try {
            storageService.uploadBytes(frontKey, result.front(), "image/png");
            uploadedKeys.add(frontKey);
            storageService.uploadBytes(backKey, result.back(), "image/png");
            uploadedKeys.add(backKey);

            CardGenerationPersistenceService.PersistResult persisted = persistenceService.persist(
                    applicationId, memberId, request.getCardDesignId(), request.getIssueDate(),
                    frontKey, backKey, GENERATE_STATUS_GATE);

            // 재생성이면 기존 파일은 DB commit이 성공한 지금부터만 삭제한다(신규 파일 선저장→commit→
            // 기존 파일 후삭제). 삭제 실패는 조용히 무시 — 고아 파일 1건은 예외적으로 허용하고
            // 수동 정리 대상으로 남긴다(TODO.md 3 완료조건).
            if (persisted.regenerated()) {
                deleteQuietly(persisted.oldFrontPath());
                deleteQuietly(persisted.oldBackPath());
            }
            adminActivityLogRepository.save(AdminActivityLog.create(adminId, AdminActivityLog.CARD_IMAGE_GENERATED,
                    memberId, (persisted.regenerated() ? "카드 이미지 재생성 성공: " : "카드 이미지 생성 성공: ") + applicationNumber));
            return new CardGenerateResponse(frontKey, backKey, request.getIssueDate());
        } catch (RuntimeException e) {
            // 이번 요청에서 새로 올라간 key만 역순으로 보상 삭제 — 렌더링/업로드/DB반영 중 어느 단계가
            // 실패해도 실패 이전 상태(기존 카드 유무 포함)가 그대로 보존된다.
            deleteUploadedKeysReversed(uploadedKeys);
            // 검증 실패(CustomException)든 S3/DB 등 예상 못한 RuntimeException이든 실패는 전부
            // 감사로그에 남긴다 — 실제 S3 장애 같은 흔한 실패 경로가 기록에서 빠지지 않도록.
            String failureLabel = (e instanceof CustomException ce) ? ce.getErrorCode().name() : e.getClass().getSimpleName();
            adminActivityLogRepository.save(AdminActivityLog.create(adminId, AdminActivityLog.CARD_IMAGE_GENERATED,
                    memberId, "카드 이미지 생성 실패(" + failureLabel + "): " + applicationNumber));
            throw e;
        }
    }

    private String buildKey(String applicationNumber, Long memberId, String side) {
        return "applications/" + applicationNumber + "/members/" + memberId + "/card/" + side + "-"
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
            // 보상 삭제 실패는 원 예외를 덮지 않는다 — 이 파일이 고아로 남을 수 있음을 감수한다
            // (TODO.md 3 완료조건 — 자동 정리 재시도 큐는 이번 스코프에 없음).
        }
    }
}
