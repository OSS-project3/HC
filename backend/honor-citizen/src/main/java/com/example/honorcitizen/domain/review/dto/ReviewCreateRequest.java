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
public class ReviewCreateRequest {

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
}
