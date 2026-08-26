package com.example.honorcitizen.domain.card.dto;

import com.example.honorcitizen.common.enums.CardSide;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CardPreviewRequest {

    @NotNull
    private Long cardDesignId;

    @NotNull
    private LocalDate issueDate;

    @NotNull
    private CardSide side;
}
