package com.example.honorcitizen.domain.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 신청 필수값 Bean Validation과 이 프로젝트의 커스텀 검증(ISO 국적 코드)을 검증한다.
// birthDate는 @NotNull + @Past(표준)로만 검증하며, 별도 근거 없는 최소연도 제한은 두지 않는다.
class ApplicationCreateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApplicationCreateRequest parse(String birthDate, String nationality) {
        return parse(birthDate, nationality, "Chicago");
    }

    private ApplicationCreateRequest parse(String birthDate, String nationality, String birthRegion) {
        String json = """
                {
                  "cardTypeId": 1,
                  "issueType": "MOBILE",
                  "applicant": { "name": "홍길동", "phone": "010-1234-5678" },
                  "member": {
                    "englishName": "Hong Gildong",
                    "birthDate": "%s",
                    "nationality": "%s",
                    "birthRegion": "%s",
                    "gender": "MALE"
                  }
                }
                """.formatted(birthDate, nationality, birthRegion);
        try {
            return objectMapper.readValue(json, ApplicationCreateRequest.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<ConstraintViolation<ApplicationCreateRequest>> violationsFor(String birthDate, String nationality) {
        return validator.validate(parse(birthDate, nationality));
    }

    private boolean hasViolationOn(Set<ConstraintViolation<ApplicationCreateRequest>> violations, String pathSuffix) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().endsWith(pathSuffix));
    }

    @Test
    void validBirthDateAndNationalityHaveNoViolations() {
        var violations = violationsFor("1990-05-15", "US");

        assertThat(hasViolationOn(violations, "member.birthDate")).isFalse();
        assertThat(hasViolationOn(violations, "member.nationality")).isFalse();
    }

    @Test
    void threeLetterOrLowercaseNationalityCodeIsRejected() {
        assertThat(hasViolationOn(violationsFor("1990-05-15", "USA"), "member.nationality")).isTrue();
        assertThat(hasViolationOn(violationsFor("1990-05-15", "us"), "member.nationality")).isTrue();
    }

    @Test
    void nonExistentIsoCodeIsRejected() {
        assertThat(hasViolationOn(violationsFor("1990-05-15", "XX"), "member.nationality")).isTrue();
    }

    @Test
    void validIsoAlpha2CodeIsAccepted() {
        assertThat(hasViolationOn(violationsFor("1990-05-15", "KR"), "member.nationality")).isFalse();
    }

    @Test
    void blankBirthRegionIsRejected() {
        var violations = validator.validate(parse("1990-05-15", "US", "   "));

        assertThat(hasViolationOn(violations, "member.birthRegion")).isTrue();
    }
}
