package com.example.honorcitizen.domain.log.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminId;

    @Column(nullable = false, length = 50)
    private String actionType;

    private Long targetId;

    @Column(length = 200)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static AdminActivityLog create(Long adminId, String actionType,
            Long targetId, String detail) {
        AdminActivityLog log = new AdminActivityLog();
        log.adminId = adminId;
        log.actionType = actionType;
        log.targetId = targetId;
        log.detail = detail;
        log.createdAt = LocalDateTime.now();
        return log;
    }

    // actionType 상수
    public static final String KOREAN_NAME_REGISTER = "KOREAN_NAME_REGISTER";
    public static final String KOREAN_NAME_UPDATE    = "KOREAN_NAME_UPDATE";
    public static final String CARD_ISSUE            = "CARD_ISSUE";
    public static final String PHOTO_REJECT          = "PHOTO_REJECT";
    public static final String TRACKING_REGISTER     = "TRACKING_REGISTER";
    public static final String PAYMENT_CONFIRMED     = "PAYMENT_CONFIRMED";
    public static final String REVIEW_START          = "REVIEW_START";
    public static final String NAMING_APPROVE        = "NAMING_APPROVE";
    public static final String PRODUCTION_START      = "PRODUCTION_START";
    // NAME_EDITING→PRODUCTION_READY 상태 전이 자체를 기록한다 — 이름 값 저장은
    // KOREAN_NAME_REGISTER/KOREAN_NAME_UPDATE(applyNamingResult)가 별도로 남기므로 중복 기록하지 않는다.
    public static final String NAMING_COMPLETE       = "NAMING_COMPLETE";
    public static final String MANSERYEOK_CONFIRMED  = "MANSERYEOK_CONFIRMED";
    // Member 1명 단위 카드 이미지 렌더링+S3 저장(카드 생성·재생성 최소 버전) — CARD_ISSUE(=markCardReady,
    // Application 전체의 "카드 준비 완료" 선언)와는 의미가 달라 별도 상수로 둔다.
    public static final String CARD_IMAGE_GENERATED  = "CARD_IMAGE_GENERATED";
    // 4-D: 관리자가 학교별 학생증 카드 템플릿(앞/뒤)을 등록·교체 — CARD_IMAGE_GENERATED(멤버별 카드
    // 렌더링 결과물)와는 대상이 다르다(이쪽은 CardDesign이 가리키는 렌더링용 원본 템플릿).
    public static final String CARD_TEMPLATE_UPLOADED = "CARD_TEMPLATE_UPLOADED";
}
