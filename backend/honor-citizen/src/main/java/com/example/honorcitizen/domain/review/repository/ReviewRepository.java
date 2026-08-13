package com.example.honorcitizen.domain.review.repository;

import com.example.honorcitizen.common.enums.ApplicationType;
import com.example.honorcitizen.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    // 단건조회의 "다음글"(더 오래된 글) 조회 — id 순서가 createdAt 순서와 항상 일치하므로
    // (IDENTITY 자동증가 + 즉시 저장) id 기준 PK 범위 조회만으로 결정론적으로 처리 가능하다.
    Optional<Review> findFirstByIdLessThanOrderByIdDesc(Long id);

    // 후기 작성 개수 제한("한 신청당 한 개", data-model.md §2) — 등록 시 사용.
    boolean existsByUserIdAndApplicationTypeAndCardTypeId(Long userId, ApplicationType applicationType, Long cardTypeId);

    // 위와 동일하되 수정 대상 후기 자신은 제외 — 수정 시 사용.
    boolean existsByUserIdAndApplicationTypeAndCardTypeIdAndIdNot(Long userId, ApplicationType applicationType,
            Long cardTypeId, Long id);
}
