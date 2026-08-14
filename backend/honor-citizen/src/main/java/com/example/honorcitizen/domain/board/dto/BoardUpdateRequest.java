package com.example.honorcitizen.domain.board.dto;

import com.example.honorcitizen.common.enums.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Review PATCH와 동일 원칙 — 전체 재제출(부분수정 아님).
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequest {

    @NotNull
    private BoardType boardType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
