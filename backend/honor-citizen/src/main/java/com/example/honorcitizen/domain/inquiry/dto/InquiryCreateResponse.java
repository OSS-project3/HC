package com.example.honorcitizen.domain.inquiry.dto;

import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import lombok.Getter;

@Getter
public class InquiryCreateResponse {

    private final Long id;

    private InquiryCreateResponse(Long id) {
        this.id = id;
    }

    public static InquiryCreateResponse from(Inquiry inquiry) {
        return new InquiryCreateResponse(inquiry.getId());
    }
}
