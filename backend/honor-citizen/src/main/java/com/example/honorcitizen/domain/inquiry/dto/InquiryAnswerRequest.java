package com.example.honorcitizen.domain.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryAnswerRequest {

    @NotBlank
    @Size(max = 5000)
    private String answer;
}
