package com.example.honorcitizen.domain.card.dto;

import com.example.honorcitizen.common.enums.StudentTextColor;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 앞/뒤를 한 번에 반환하므로(CardPreviewResponse) side 구분 없음 — 2026-08-27 계약 변경.
// 이전엔 CardSide(FRONT/BACK)를 받아 한쪽만 반환했으나, 공통 DB조회·검증·만세력조회·S3다운로드가
// side와 무관하게 매번 중복 실행되는 낭비가 있어 한 번에 둘 다 만들도록 바꿨다.
@Getter
@NoArgsConstructor
public class CardPreviewRequest {

    @NotNull
    private Long cardDesignId;

    @NotNull
    private LocalDate issueDate;

    // 학생증(STUDENT) 전용, 선택(checklist.md §6) — 비학생증 카드가 값을 보내면 거절한다.
    // 생략하거나 이미 확정된 값과 같으면 그대로 통과, 이미 확정된 값과 다르면 거절한다.
    private StudentTextColor studentFrontTextColor;
    private StudentTextColor studentBackTextColor;
}
