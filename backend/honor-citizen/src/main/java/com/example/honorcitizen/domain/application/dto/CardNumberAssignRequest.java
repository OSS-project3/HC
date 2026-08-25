package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자가 개인/단일 Member 카드번호를 직접 입력·확정한다(서버 채번 없음). 형식 검증은
// ApplicationMember.assignCardNumber(엔티티)에서 강제한다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CardNumberAssignRequest {

    @NotBlank
    @Size(max = 30)
    private String cardNumber;
}
