package com.example.honorcitizen.domain.inquiry.dto;

import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.InquiryStatus;
import com.example.honorcitizen.domain.inquiry.entity.Inquiry;
import lombok.Getter;

import java.time.LocalDateTime;

// 내 문의 목록(GET /api/my/inquiries)과 관리자 목록(GET /api/admin/inquiries) 공용 — 본인 데이터를
// 본인이 다시 받는 것이라 관리자용 필드(name/email/phone)가 섞여 있어도 노출 문제가 없다.
@Getter
public class InquiryListItemResponse {

    private final Long id;
    private final InquiryCategory category;
    private final String title;
    private final String name;
    private final String email;
    private final String phone;
    private final InquiryStatus status;
    private final LocalDateTime createdAt;

    private InquiryListItemResponse(Long id, InquiryCategory category, String title, String name,
            String email, String phone, InquiryStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static InquiryListItemResponse from(Inquiry inquiry) {
        return new InquiryListItemResponse(inquiry.getId(), inquiry.getCategory(), inquiry.getTitle(),
                inquiry.getName(), inquiry.getEmail(), inquiry.getPhone(), inquiry.getStatus(), inquiry.getCreatedAt());
    }
}
