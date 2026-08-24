package com.example.honorcitizen.domain.review.repository;

import com.example.honorcitizen.domain.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(Long reviewId);

    void deleteByReviewId(Long reviewId);

    // keepImageIds 소유권 검증 — 반환된 개수가 요청한 id 개수보다 적으면 타 Review 소유이거나
    // 존재하지 않는 id가 섞여 있다는 뜻이다(EventImageRepository와 동일 패턴).
    List<ReviewImage> findByIdInAndReviewId(List<Long> ids, Long reviewId);
}
