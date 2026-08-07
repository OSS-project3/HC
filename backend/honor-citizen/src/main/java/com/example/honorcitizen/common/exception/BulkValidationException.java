package com.example.honorcitizen.common.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class BulkValidationException extends CustomException {

    private final List<ValidationErrorDetail> errors;

    public BulkValidationException(List<ValidationErrorDetail> errors) {
        super(ErrorCode.BULK_APPLICATION_VALIDATION_FAILED);
        this.errors = errors;
    }
}
