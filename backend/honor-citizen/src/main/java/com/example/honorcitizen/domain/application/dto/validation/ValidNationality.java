package com.example.honorcitizen.domain.application.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// ISO 3166-1 alpha-2 국가 코드인지 검증한다. 표준 Bean Validation 애노테이션으로는
// 표현할 수 없는 계약(고정된 코드 목록과의 일치)이라 커스텀 제약으로 둔다.
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NationalityValidator.class)
public @interface ValidNationality {

    String message() default "국적은 ISO 3166-1 alpha-2 국가 코드여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
