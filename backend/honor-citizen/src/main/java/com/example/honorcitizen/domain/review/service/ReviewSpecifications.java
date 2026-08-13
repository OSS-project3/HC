package com.example.honorcitizen.domain.review.service;

import com.example.honorcitizen.common.enums.ReviewSearchType;
import com.example.honorcitizen.domain.review.entity.Review;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

// GET /api/reviews의 동적 필터(cardTypeId/hasPhoto/searchType+keyword)를 JpaSpecificationExecutor로 구현
// (api.md §API 2 구현 메모). 각 메서드는 조건이 없으면 cb.conjunction()(항상 참, 무조건)을 반환해 "이 조건 없음"을
// 표현한다 — 이 프로젝트가 쓰는 Spring Data JPA 버전은 Specification.where(null)/.and(null)을 더 이상 허용하지
// 않고 IllegalArgumentException을 던지므로(과거 버전의 null 허용 동작과 다름), null을 반환하면 안 된다.
final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    static Specification<Review> cardTypeId(Long cardTypeId) {
        return (root, query, cb) ->
                cardTypeId == null ? cb.conjunction() : cb.equal(root.get("cardTypeId"), cardTypeId);
    }

    // 단일 컬럼(image_path) 존재 여부만 확인하면 되므로 배치조회 없이 단순 WHERE 조건이다.
    static Specification<Review> hasPhoto(Boolean hasPhoto) {
        return (root, query, cb) -> {
            if (hasPhoto == null) {
                return cb.conjunction();
            }
            return hasPhoto ? cb.isNotNull(root.get("imagePath")) : cb.isNull(root.get("imagePath"));
        };
    }

    // 검색 계약(api.md §API 2): keyword 없으면 searchType과 무관하게 조건 자체를 걸지 않는다.
    // keyword 있는데 searchType 없으면 ALL로 처리한다.
    static Specification<Review> keyword(ReviewSearchType searchType, String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            ReviewSearchType type = searchType == null ? ReviewSearchType.ALL : searchType;
            String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";

            return switch (type) {
                case TITLE -> cb.like(cb.lower(root.get("title")), pattern);
                case CONTENT -> cb.like(cb.lower(root.get("content")), pattern);
                case AUTHOR -> cb.like(cb.lower(root.get("authorDisplayName")), pattern);
                case ALL -> cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern),
                        cb.like(cb.lower(root.get("authorDisplayName")), pattern));
            };
        };
    }
}
