package com.example.honorcitizen.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT(400, "입력값 검증에 실패했습니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "권한이 없습니다."),
    NOT_FOUND(404, "데이터를 찾을 수 없습니다."),
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다."),

    // Auth
    TERMS_NOT_AGREED(403, "약관에 동의해주세요."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_REUSE_DETECTED(401, "리프레시 토큰 재사용이 감지되어 모든 세션이 만료되었습니다."),
    TERMS_ALREADY_AGREED(409, "이미 약관에 동의하셨습니다."),
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    ALREADY_WITHDRAWN(409, "이미 탈퇴 처리된 계정입니다."),
    EMAIL_ALREADY_EXISTS(409, "이미 가입된 이메일입니다."),
    EMAIL_DELIVERY_FAILED(503, "이메일 발송에 실패했습니다."),
    TOO_MANY_REQUESTS(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    // 불일치/만료/이미 사용됨/시도 초과를 전부 동일하게 취급한다(공격자가 원인을 구분하지 못하도록 — SIGNUP-2 정책).
    INVALID_VERIFICATION_CODE(400, "인증 코드가 올바르지 않거나 만료되었습니다."),
    // 토큰 없음/만료와 토큰-이메일 불일치를 동일하게 취급한다(이메일 존재 여부 비노출 — AUTH-4 정책).
    INVALID_SIGNUP_TOKEN(400, "가입 인증이 만료되었거나 유효하지 않습니다. 이메일 인증을 다시 진행해주세요."),
    // 15분 내 5회 로그인 실패 시 15분 잠금 — 잠금 여부는 계정 존재 여부와 무관하게 동일하게 카운트되므로
    // 이 오류 자체가 "이 이메일이 가입돼 있다"는 뜻은 아니다(RATE-1 정책).
    ACCOUNT_LOCKED(429, "로그인 시도가 너무 많아 일시적으로 잠겼습니다. 잠시 후 다시 시도해주세요."),
    // 계정없음/비밀번호불일치/OAuth전용계정/탈퇴유예기간경과를 전부 동일하게 취급한다(이메일 존재 여부 비노출 — AUTH-5 정책).
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    // AUTH-6: 이미 로그인된 사용자의 자기 서비스 요청이라 이메일 존재 여부 비노출과 무관 — 원인을 구체적으로 알려준다.
    CURRENT_PASSWORD_MISMATCH(400, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_CHANGE_NOT_ALLOWED(403, "OAuth 계정은 비밀번호를 변경할 수 없습니다."),

    // Application
    APPLICATION_NOT_FOUND(404, "존재하지 않는 신청입니다."),
    INVALID_STATUS_TRANSITION(400, "허용되지 않는 상태 전이입니다."),
    APPLICATION_LIMIT_EXCEEDED(429, "일일 신청 가능 횟수(3회)를 초과했습니다."),

    // Upload
    FILE_TOO_LARGE(413, "파일 크기는 10MB를 초과할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(415, "허용되지 않는 파일 형식입니다."),
    INVALID_IMAGE(400, "얼굴을 식별할 수 없습니다."),
    INVALID_IMAGE_FILE(400, "이미지 파일이 손상되었거나 형식이 올바르지 않습니다."),

    // Bulk Application
    INVALID_ZIP(400, "ZIP 형식이 올바르지 않습니다."),
    BULK_APPLICATION_VALIDATION_FAILED(400, "단체 신청 검증에 실패했습니다."),

    // Card
    CARD_NOT_READY(400, "아직 카드가 발급되지 않았습니다."),

    // Review
    REVIEW_NOT_FOUND(404, "존재하지 않는 후기입니다."),
    REVIEW_NOT_ELIGIBLE(403, "선택한 신청유형·카드종류에 대한 신청 이력이 없습니다."),
    REVIEW_ALREADY_EXISTS(409, "이미 해당 신청유형·카드종류로 작성한 후기가 있습니다."),

    // Board
    BOARD_NOT_FOUND(404, "존재하지 않는 게시글입니다."),

    // Event
    EVENT_NOT_FOUND(404, "존재하지 않는 행사입니다."),

    // Inquiry
    INQUIRY_NOT_FOUND(404, "존재하지 않는 문의입니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
