package com.example.honorcitizen.domain.application.dto.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;

import java.util.Locale;
import java.util.Set;

// 개인 신청(Bean Validation DTO)과 단체 신청(BulkExcelParser의 수동 엑셀 행 파싱)이
// 같은 의미의 값(국적/이메일)에 대해 서로 다른 규칙을 쓰지 않도록,
// 판정 로직을 이 클래스에만 두고 양쪽에서 재사용한다.
// 전화번호 형식 검증은 포함하지 않는다 — 외국인 신청자를 고려한 국제 전화번호 정책이
// 아직 확정되지 않아 국내향 정규식을 임의로 적용하지 않기로 했다(PENDING_DECISIONS.md 참고).
public final class ApplicationFieldFormats {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public static boolean isValidNationality(String nationality) {
        return nationality == null || ISO_COUNTRIES.contains(nationality);
    }

    // DTO 쪽 @Email과 동일한 판정을 수동 파싱 경로(BulkExcelParser)에서도 그대로 쓰기 위해
    // 별도 정규식을 만들지 않고 Hibernate Validator의 @Email 구현을 그대로 재사용한다.
    public static boolean isValidEmail(String email) {
        return email == null || email.isBlank() || VALIDATOR.validate(new EmailHolder(email)).isEmpty();
    }

    private record EmailHolder(@Email String email) {
    }

    private ApplicationFieldFormats() {
    }
}
