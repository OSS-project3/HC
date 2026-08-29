package com.example.honorcitizen.domain.card.service;

import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.entity.ApplicationMember;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.function.Predicate;

// 3-A(서비스 구조 분리)·3-B 영속 전담 서비스(2026-08-30) — CardGenerationService(오케스트레이션,
// 비-transactional: 준비→렌더링→S3 업로드)가 두 파일 업로드에 성공한 뒤에만 호출한다. 여기서
// Application/Member를 다시 조회해 상태 게이트·소속·작명·카드번호·디자인/발급일자 확정값을
// 재검증한다(준비 시점 스냅샷 이후 다른 요청이 상태를 바꿨을 수 있음 — 3-A "준비 시점 스냅샷과
// 최종 재검증"). ApplicationMember.version(낙관적 락)이 동시 요청 중 하나만 commit되게 보장한다.
@Service
@RequiredArgsConstructor
class CardGenerationPersistenceService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMemberRepository applicationMemberRepository;

    record PersistResult(String oldFrontPath, String oldBackPath, boolean regenerated) {
    }

    @Transactional
    PersistResult persist(Long applicationId, Long memberId, Long cardDesignId, LocalDate issueDate,
            String frontKey, String backKey, Predicate<Application> statusGate) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        if (!statusGate.test(application)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        ApplicationMember member = applicationMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!member.getApplicationId().equals(applicationId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (!StringUtils.hasText(member.getSurname()) || !StringUtils.hasText(member.getName())
                || !StringUtils.hasText(member.getNameMeaning())) {
            throw new CustomException(ErrorCode.NAMING_INCOMPLETE);
        }
        if (!StringUtils.hasText(member.getCardNumber())) {
            throw new CustomException(ErrorCode.CARD_NOT_READY);
        }
        if (application.getCardDesignId() != null && !application.getCardDesignId().equals(cardDesignId)) {
            throw new CustomException(ErrorCode.CARD_DESIGN_MISMATCH);
        }
        if (application.getCardIssueDate() != null && !application.getCardIssueDate().equals(issueDate)) {
            throw new CustomException(ErrorCode.CARD_ISSUE_DATE_MISMATCH);
        }

        String oldFrontPath = member.getCardFrontPath();
        String oldBackPath = member.getCardBackPath();
        boolean regenerated = member.isCardGenerated();

        application.confirmCardGeneration(cardDesignId, issueDate);
        member.assignCardImages(frontKey, backKey, issueDate);

        return new PersistResult(oldFrontPath, oldBackPath, regenerated);
    }
}
