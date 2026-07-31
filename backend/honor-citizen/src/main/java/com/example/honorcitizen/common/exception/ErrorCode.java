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
    DUPLICATE_APPLICATION(409, "진행 중인 신청이 이미 존재합니다."),
    APPLICATION_NOT_FOUND(404, "존재하지 않는 신청입니다."),
    INVALID_STATUS_TRANSITION(400, "허용되지 않는 상태 전이입니다."),

    // Upload
    FILE_TOO_LARGE(413, "파일 크기는 10MB를 초과할 수 없습니다."),
    UNSUPPORTED_FILE_TYPE(415, "허용되지 않는 파일 형식입니다."),
    INVALID_IMAGE(400, "얼굴을 식별할 수 없습니다."),
    INAPPROPRIATE_IMAGE(400, "부적절한 이미지가 감지되었습니다."),
    PHOTO_NOT_FOUND(404, "유효하지 않은 사진입니다."),
    PHOTO_EXPIRED(400, "사진 업로드가 만료되었습니다."),
    PHOTO_OWNER_MISMATCH(403, "다른 사용자가 업로드한 사진입니다."),

    // Bulk Application
    ZIP_TOO_LARGE(413, "ZIP 파일은 500MB를 초과할 수 없습니다."),
    INVALID_ZIP(400, "ZIP 형식이 올바르지 않습니다."),
    EXCEL_NOT_FOUND(400, "ZIP 안에 엑셀 파일이 없습니다."),
    EXCEL_PARSE_ERROR(400, "엑셀 형식이 올바르지 않습니다."),
    ALL_FAILED(400, "전체 행 처리에 실패했습니다."),

    // Shipping
    SHIPPING_ALREADY_EXISTS(409, "이미 배송지가 등록되어 있습니다."),
    SHIPPING_NOT_FOUND(404, "배송지 정보가 없습니다."),
    SHIPPING_LOCKED(400, "발송 이후 배송지는 수정할 수 없습니다."),
    EDIT_PERIOD_EXPIRED(400, "수정 가능 기간이 만료되었습니다."),
    UNAUTHORIZED_ACCESS(403, "본인 신청이 아닙니다."),

    // KoreanName
    KOREAN_NAME_ALREADY_EXISTS(409, "이미 한국 이름이 등록되어 있습니다."),
    KOREAN_NAME_NOT_FOUND(404, "등록된 한국 이름이 없습니다."),
    INVALID_APPLICATION_STATUS(400, "현재 신청 상태에서 허용되지 않는 작업입니다."),

    // CitizenCard
    CARD_ALREADY_ISSUED(409, "이미 발급된 시민증이 있습니다."),
    CARD_NOT_READY(400, "아직 시민증이 발급되지 않았습니다."),
    CARD_NOT_FOUND(404, "발급된 시민증을 찾을 수 없습니다."),

    // Bulk Order
    BULK_ORDER_NOT_FOUND(404, "단체 신청 정보를 찾을 수 없습니다."),
    NO_CARDS_ISSUED(400, "아직 발급된 카드가 없습니다."),

    // Payment
    ALREADY_PAID(409, "이미 결제 완료된 신청입니다."),
    PAYMENT_FAILED(402, "결제에 실패했습니다."),
    AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않습니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
