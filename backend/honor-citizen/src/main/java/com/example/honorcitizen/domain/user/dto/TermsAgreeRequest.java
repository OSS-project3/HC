package com.example.honorcitizen.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TermsAgreeRequest {

    @NotNull(message = "개인정보 동의 여부를 입력해주세요.")
    private Boolean privacyAgreed;

    @NotNull(message = "이미지 업로드 동의 여부를 입력해주세요.")
    private Boolean imageUploadAgreed;

    @NotNull(message = "배송 안내 동의 여부를 입력해주세요.")
    private Boolean shippingAgreed;
}
