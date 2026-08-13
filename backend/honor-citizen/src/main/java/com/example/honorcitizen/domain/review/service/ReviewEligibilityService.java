package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.entity.Application;
import com.example.honorcitizen.domain.application.repository.ApplicantRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationMemberRepository;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.review.repository.ReviewRepository;
import com.example.honorcitizen.domain.user.entity.User;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

// 후기 등록/수정 시 (applicationType, cardTypeId) 조합의 유효성을 검증한다 — data-model.md §2·2026-08-13 확정 참고.
// 1) (등록만) 탈퇴한 계정이 아닌가(ALREADY_WITHDRAWN) 2) 실제 카드 발급 이력이 있는가(REVIEW_NOT_ELIGIBLE)
// 3) 같은 조합으로 이미 작성한 후기가 없는가(REVIEW_ALREADY_EXISTS).
// 항상 후기 작성자(userId) 기준으로 판단한다 — 관리자가 대신 수정하더라도 원 작성자 기준.
@Component
@RequiredArgsConstructor
class ReviewEligibilityService {

    private final UserRepository userRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final ApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;

    void validateForCreate(Long userId, ApplicationType applicationType, Long cardTypeId) {
        User user = getUser(userId);
        // 탈퇴 처리된 계정은 새 후기를 작성할 수 없다 — 기존 토큰이 아직 블랙리스트에 없어 인증은
        // 통과하더라도(탈퇴 시 현재 토큰만 블랙리스트에 올림) 새 콘텐츠 생성은 여기서 막는다.
        // 수정(validateForUpdate)에는 적용하지 않는다 — 원작성자가 나중에 탈퇴해도 기존 후기 관리(관리자 수정 등)는 계속 가능해야 한다.
        if (user.isWithdrawn()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
        }
        validateHasCompletedApplication(user, applicationType, cardTypeId);
        if (reviewRepository.existsByUserIdAndApplicationTypeAndCardTypeId(userId, applicationType, cardTypeId)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    void validateForUpdate(Long userId, ApplicationType applicationType, Long cardTypeId, Long reviewId) {
        User user = getUser(userId);
        validateHasCompletedApplication(user, applicationType, cardTypeId);
        if (reviewRepository.existsByUserIdAndApplicationTypeAndCardTypeIdAndIdNot(
                userId, applicationType, cardTypeId, reviewId)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // "신청서를 낸 계정"이 아니라 "이메일로 자기 카드를 조회할 수 있는 사람" 기준(단체 신청 구성원 포함).
    // data-model.md §2: Applicant.email(대표 제출자) 또는 ApplicationMember.email(단체 구성원 개인) 매칭.
    private void validateHasCompletedApplication(User user, ApplicationType applicationType, Long cardTypeId) {
        Set<Long> matchedApplicationIds = new HashSet<>();
        applicantRepository.findByEmail(user.getEmail())
                .forEach(applicant -> matchedApplicationIds.add(applicant.getApplicationId()));
        applicationMemberRepository.findByEmail(user.getEmail())
                .forEach(member -> matchedApplicationIds.add(member.getApplicationId()));

        boolean eligible = applicationRepository.findAllById(matchedApplicationIds).stream()
                .filter(application -> application.getStatus() == ApplicationStatus.COMPLETED)
                .anyMatch(application -> matchesCombination(application, applicationType, cardTypeId));

        if (!eligible) {
            throw new CustomException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
    }

    private boolean matchesCombination(Application application, ApplicationType applicationType, Long cardTypeId) {
        return application.getApplicationType() == applicationType && application.getCardTypeId().equals(cardTypeId);
    }
}
