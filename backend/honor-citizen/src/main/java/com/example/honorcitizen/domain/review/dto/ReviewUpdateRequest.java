package com.example.honorcitizen.domain.review.dto;

import com.example.honorcitizen.common.enums.ApplicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotNull
    private ApplicationType applicationType;

    @NotNull
    private Long cardTypeId;

    @NotBlank
    @Size(max = 50)
    private String authorName;

    @NotBlank
    private String content;

    // 프론트에는 없는 필드 — 멀티파트에서 "새 파일 파트 없음"만으로는 "기존 사진 유지"와 "삭제"를
    // 구분할 수 없어 API에서 별도로 받는다(api.md §API 5).
    private boolean removeImage;
}
