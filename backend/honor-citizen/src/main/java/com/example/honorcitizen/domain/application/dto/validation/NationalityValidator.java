package com.example.honorcitizen.domain.application.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NationalityValidator implements ConstraintValidator<ValidNationality, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return ApplicationFieldFormats.isValidNationality(value);
    }
}
