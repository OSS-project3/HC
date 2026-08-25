package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 단체 신청 카드번호 일괄 저장(admin-saju.md "관리자 카드번호 입력 정책"). 관리자 화면의
// 사진번호+카드번호 탭 구분 붙여넣기를 프론트가 JSON items로 변환해 보낸다. applicationVersion은
// Application.version과 대조해 동시 수정을 감지한다(불일치 시 APPLICATION_VERSION_CONFLICT).
@Getter
@NoArgsConstructor
public class CardNumberBatchAssignRequest {

    @NotNull
    private Long applicationVersion;

    @NotEmpty
    @Valid
    private List<CardNumberItem> items;

    @Getter
    @NoArgsConstructor
    public static class CardNumberItem {

        // 신청 Excel의 사진 번호(ApplicationMember.photoNumber) — Member ID나 화면 순서로 매칭하지 않는다.
        @NotBlank
        @Size(max = 10)
        private String photoNumber;

        @NotBlank
        @Size(max = 30)
        private String cardNumber;
    }
}
