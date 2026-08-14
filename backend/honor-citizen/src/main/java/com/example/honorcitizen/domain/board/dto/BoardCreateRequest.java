package com.example.honorcitizen.domain.board.dto;

import com.example.honorcitizen.common.enums.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateRequest {

    @NotNull
    private BoardType boardType;

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
