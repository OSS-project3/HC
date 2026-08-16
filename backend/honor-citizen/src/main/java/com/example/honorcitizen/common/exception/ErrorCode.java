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
    EVENT_NOT_FOUND(404, "존재하지 않는 행사입니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
