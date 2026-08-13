package com.example.honorcitizen.common.exception;

import com.example.honorcitizen.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BulkValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBulkValidationException(BulkValidationException ex) {
        log.warn("BulkValidationException: {} errors", ex.getErrors().size());
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(ApiResponse.fail(ex.getErrorCode().name(), ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("CustomException: {} - {}", errorCode.name(), ex.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        // 최상위 errorMessage는 하위 호환을 위해 기존과 동일하게 BindingResult가 반환한
        // 원래 첫 번째 오류의 메시지를 그대로 쓴다 — 아래 errors[] 정렬과는 무관하다.
        FieldError firstFieldError = fieldErrors.stream().findFirst().orElse(null);
        String message = firstFieldError != null ? firstFieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";

        // errors[]는 여러 필드가 동시에 실패해도 전부 확인할 수 있도록 전체 목록을 담는다.
        // BindingResult 내부 순서는 스펙상 보장되지 않으므로 field 기준으로 정렬해 결정론적으로 만든다.
        // row는 Bulk 신청 전용 개념(엑셀 행 번호)이라 여기서는 항상 null — ValidationErrorDetail은
        // 원래 Bulk 전용으로 설계된 타입을 그대로 재사용한 것이라, 두 경로의 의미가 완전히 갈리면
        // 공통 오류 모델을 별도로 정리할 필요가 있다(PENDING_DECISIONS.md 참고).
        List<ValidationErrorDetail> errors = fieldErrors.stream()
                .map(fe -> new ValidationErrorDetail(null, fe.getField(), fe.getCode(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(ValidationErrorDetail::field))
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), message, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), "요청 본문이 올바르지 않습니다."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        log.warn("MissingServletRequestPartException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), "필수 요청 파트가 누락되었습니다."));
    }

    // 쿼리 파라미터가 enum 등 기대한 타입으로 변환 불가능할 때(예: ?searchType=WRONG) 발생.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.name(), "요청 파라미터 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail(ErrorCode.FILE_TOO_LARGE.name(), ErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
