package com.example.honorcitizen.api;

import com.example.honorcitizen.common.enums.ReviewSearchType;
import com.example.honorcitizen.common.response.ApiResponse;
import com.example.honorcitizen.common.response.PageResponse;
import com.example.honorcitizen.domain.review.dto.ReviewCreateRequest;
import com.example.honorcitizen.domain.review.dto.ReviewCreateResponse;
import com.example.honorcitizen.domain.review.dto.ReviewDetailResponse;
import com.example.honorcitizen.domain.review.dto.ReviewListItemResponse;
import com.example.honorcitizen.domain.review.dto.ReviewUpdateRequest;
import com.example.honorcitizen.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewCreateResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestPart("request") ReviewCreateRequest request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(reviewService.create(userId, request, images)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewListItemResponse>>> list(
            @RequestParam(required = false) Long cardTypeId,
            @RequestParam(required = false) Boolean hasPhoto,
            @RequestParam(required = false) ReviewSearchType searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.list(cardTypeId, hasPhoto, searchType, keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> detail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.detail(id, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        reviewService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestPart("request") ReviewUpdateRequest request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images) {
        reviewService.update(id, userId, request, images);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
