package com.example.honorcitizen.api;

import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.review.dto.ReviewListItemResponse;
import com.example.honorcitizen.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my/reviews")
@RequiredArgsConstructor
public class MyReviewController {

    private final ReviewService reviewService;
    private final EnglishResponseTranslator englishResponseTranslator;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewListItemResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return ResponseEntity.ok(ApiResponse.success(
                englishResponseTranslator.translateReviews(reviewService.listMine(userId, page, size), acceptLanguage)));
    }
}
