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

    // Upload
    FILE_TOO_LARGE(413, "파일 크기는 10MB를 초과할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(415, "허용되지 않는 파일 형식입니다."),
    INVALID_IMAGE(400, "얼굴을 식별할 수 없습니다."),

    // Bulk Application
    ZIP_TOO_LARGE(413, "ZIP 파일은 500MB를 초과할 수 없습니다."),
    INVALID_ZIP(400, "ZIP 형식이 올바르지 않습니다."),
    EXCEL_NOT_FOUND(400, "ZIP 안에 엑셀 파일이 없습니다."),
    EXCEL_PARSE_ERROR(400, "엑셀 형식이 올바르지 않습니다."),

    // Card
    CARD_NOT_READY(400, "아직 카드가 발급되지 않았습니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
