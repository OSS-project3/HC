package com.example.honorcitizen.common.response;

import com.example.honorcitizen.common.exception.ValidationErrorDetail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String errorCode;
    private final String errorMessage;
    private final List<ValidationErrorDetail> errors;

    private ApiResponse(boolean success, T data, String errorCode, String errorMessage, List<ValidationErrorDetail> errors) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null, null, null);
    }

    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, null, errorCode, errorMessage, null);
    }

    public static <T> ApiResponse<T> fail(String errorCode, String errorMessage, List<ValidationErrorDetail> errors) {
        return new ApiResponse<>(false, null, errorCode, errorMessage, errors);
    }
}
