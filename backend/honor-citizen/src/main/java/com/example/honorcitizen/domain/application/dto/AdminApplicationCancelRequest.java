package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자 강제 취소(2026-09-25 확정) 요청 — RejectPhotoRequest와 동일한 패턴. 여기서의 @Size(max=500)는
// trim 전 원본 길이 상한이고, trim 결과 1~500자 검증의 최종 기준은 Application.cancelByAdmin()이 담당한다.
@Getter
@NoArgsConstructor
public class AdminApplicationCancelRequest {

    @NotBlank
    @Size(max = 500)
    private String cancellationMemo;
}
