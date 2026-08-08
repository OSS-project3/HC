# Application Pending Decisions

> `docs/specs/application/APPLICATION.md`에 명시된 TBD만 집약한다.

- 신청 취소 시 파일 30일 보관 후 삭제 정책의 현재 적용 여부
- 향후 Cleanup Scheduler 도입 여부
- 취소·반려·회원 탈퇴·출력 완료 후 파일 처리와 보존 기간
- ZIP 최대 크기
- Excel 최대 행 수
- 최대 신청 인원
- 일일 신청 횟수에 취소·반려 신청을 포함할지 여부
- 신청번호의 날짜별 번호 정책
- 운영 전 Idempotency-Key 도입 여부
- 학생증 공백 문자열 정책
- 학교명 필드 추가 여부
- quantity 정책
- 학생증 `department`(학과) 필드를 계속 유지할지, 제외할지 (2026-08-07 추가 — `APPLICATION.md`는 "현재 제외"로 명시했지만 근거가 없고, 사람이 아직 미결정으로 확인. 결정 전까지는 기존대로 필수 필드 유지)
- 일일 KST 3회 제한 DB 원자 처리 미구현 (2026-08-07 추가 — `checklist.md` §4·§5 항목 중 유일하게 남은 미구현. `APPLICATION.md` §7에 정책은 명시돼 있음: 하루 3회, KST 00:00~23:59 기준, 개인/단체 합산, 동시 요청은 DB 수준에서 원자적으로 처리, `APPLICATION_LIMIT_EXCEEDED` ErrorCode 추가 — 단 "현재 리팩터링 범위에서는 구현하지 않는다"고 명시돼 있어 저우선순위. 구현 착수 전 위 "취소·반려 포함 여부" 결정이 먼저 필요)
- 국제 전화번호 형식 정책 (2026-08-08 추가 — `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `phone` 필드에 `UserUpdateRequest`와 동일한 국내향 정규식(`^[0-9\-]{9,20}$`)을 재사용하려 했으나, 외국인 신청자를 받는 서비스 특성상 국제번호(`+`, 국가코드 등)를 고려한 별도 정책 확정이 먼저 필요하다는 판단으로 보류함. 현재는 `@NotBlank`(필수 여부)만 적용되고 형식 검증은 없음)
- ~~`GlobalExceptionHandler`의 Bean Validation 다중 필드 오류 응답~~ (2026-08-08 해결 — `errors[]`에 위반된 모든 필드를 field 기준 정렬로 반환하도록 구현 완료. `docs/collab/CHANGELOG.md` 2026-08-08 항목 참고)
- 공통 오류 모델 정리 (2026-08-08 추가 — `ValidationErrorDetail(row, field, code, message)`는 원래 Bulk 단체 신청 전용으로 설계된 타입인데, `GlobalExceptionHandler`의 일반 Bean Validation 오류 응답에도 그대로 재사용하면서 `row`가 Bulk 경로에서만 의미 있고 일반 검증 경로에서는 항상 `null`인 상태가 됐다. 또한 두 경로의 `code` 값 어휘도 다르다 — Bulk는 `REQUIRED`/`INVALID_FORMAT`/`PHOTO_NOT_FOUND` 같은 의미 카테고리, 일반 Bean Validation은 `NotBlank`/`Size`/`ValidNationality` 같은 애노테이션 이름. 현재는 최소 변경 원칙에 따라 그대로 재사용했으나, 두 경로가 늘어나면 `row`를 Bulk 전용 하위 타입으로 분리하거나 공통 `code` 어휘를 정하는 등 별도 정리가 필요할 수 있음)
