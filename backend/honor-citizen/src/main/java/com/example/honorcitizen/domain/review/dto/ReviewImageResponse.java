package com.example.honorcitizen.domain.review.dto;

import lombok.Getter;

// 후기 상세의 이미지 한 장. id는 수정 화면에서 유지/삭제할 이미지를 지정(keepImageIds)할 때 쓰인다.
@Getter
public class ReviewImageResponse {

    private final Long id;
    private final String imageUrl;

    private ReviewImageResponse(Long id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    public static ReviewImageResponse of(Long id, String imageUrl) {
        return new ReviewImageResponse(id, imageUrl);
    }
}
