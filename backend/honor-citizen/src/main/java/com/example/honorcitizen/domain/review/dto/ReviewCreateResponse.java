package com.example.honorcitizen.domain.review.dto;

import com.example.honorcitizen.domain.review.entity.Review;
import lombok.Getter;

@Getter
public class ReviewCreateResponse {

    private final Long id;

    private ReviewCreateResponse(Long id) {
        this.id = id;
    }

    public static ReviewCreateResponse from(Review review) {
        return new ReviewCreateResponse(review.getId());
    }
}
