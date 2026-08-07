package com.example.honorcitizen.common.exception;

public record ValidationErrorDetail(Integer row, String field, String code, String message) {
}
