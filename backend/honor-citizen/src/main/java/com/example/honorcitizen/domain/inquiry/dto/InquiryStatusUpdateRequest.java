package com.example.honorcitizen.domain.inquiry.dto;

import com.example.honorcitizen.common.enums.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryStatusUpdateRequest {

    @NotNull
    private InquiryStatus status;
}
