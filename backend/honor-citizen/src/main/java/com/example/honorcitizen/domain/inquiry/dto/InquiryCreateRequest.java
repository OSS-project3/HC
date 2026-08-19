package com.example.honorcitizen.domain.inquiry.dto;

import com.example.honorcitizen.common.enums.InquiryCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryCreateRequest {

    @NotNull
    private InquiryCategory category;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

    @NotBlank
    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;

    // 개인정보 수집·이용 동의 서버 검증(requirements.md §⑤ "개인정보 동의 서버 검증" 근거) —
    // 컬럼으로 저장하지 않고 true 여부만 게이트로 쓴다.
    @AssertTrue
    private boolean privacyConsent;
}
