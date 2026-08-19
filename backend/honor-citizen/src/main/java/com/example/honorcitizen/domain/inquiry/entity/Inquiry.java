package com.example.honorcitizen.domain.inquiry.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.InquiryCategory;
import com.example.honorcitizen.common.enums.InquiryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 1:1 문의. name/email/phone은 User 스냅샷이 아니라 이 문의 건에 한정된 연락처로, 요청 바디 값을
// 그대로 저장한다(requirements.md §⑤ 근거). userId는 arch.md §5.1 원칙에 따라 User와 JPA 연관관계
// 없이 Long으로만 참조한다(Application.userId와 동일 방식).
@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InquiryCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(length = 5000)
    private String answer;

    private LocalDateTime answeredAt;

    public static Inquiry create(Long userId, InquiryCategory category, String name, String email,
            String phone, String title, String content) {
        Inquiry inquiry = new Inquiry();
        inquiry.userId = userId;
        inquiry.category = category;
        inquiry.name = name;
        inquiry.email = email;
        inquiry.phone = phone;
        inquiry.title = title;
        inquiry.content = content;
        inquiry.status = InquiryStatus.PENDING;
        return inquiry;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    // 답변 등록/수정(신규·수정 동일 처리) — 저장과 동시에 COMPLETED로 전이한다(requirements.md §④).
    public void answer(String answer) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
        this.status = InquiryStatus.COMPLETED;
    }

    // 답변 내용과 무관하게 상태만 독립적으로 바꾸는 관리자 동작(AdminPage.tsx의 별도 드롭다운).
    public void changeStatus(InquiryStatus status) {
        this.status = status;
    }
}
