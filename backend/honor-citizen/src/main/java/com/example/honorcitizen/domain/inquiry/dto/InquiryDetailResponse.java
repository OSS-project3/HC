package com.example.honorcitizen.domain.inquiry.dto;

import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import lombok.Getter;

import java.time.LocalDateTime;

// 내 문의 상세(GET /api/my/inquiries/{id})와 관리자 상세(GET /api/admin/inquiries/{id}) 공용.
@Getter
public class InquiryDetailResponse {

    private final Long id;
    private final InquiryCategory category;
    private final String name;
    private final String email;
    private final String phone;
    private final String title;
    private final String content;
    private final InquiryStatus status;
    private final String answer;
    private final LocalDateTime answeredAt;
    private final LocalDateTime createdAt;

    private InquiryDetailResponse(Long id, InquiryCategory category, String name, String email, String phone,
            String title, String content, InquiryStatus status, String answer, LocalDateTime answeredAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.title = title;
        this.content = content;
        this.status = status;
        this.answer = answer;
        this.answeredAt = answeredAt;
        this.createdAt = createdAt;
    }

    public static InquiryDetailResponse from(Inquiry inquiry) {
        return new InquiryDetailResponse(inquiry.getId(), inquiry.getCategory(), inquiry.getName(),
                inquiry.getEmail(), inquiry.getPhone(), inquiry.getTitle(), inquiry.getContent(),
                inquiry.getStatus(), inquiry.getAnswer(), inquiry.getAnsweredAt(), inquiry.getCreatedAt());
    }
}
