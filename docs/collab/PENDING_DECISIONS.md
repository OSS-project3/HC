# Application Pending Decisions

> `docs/specs/application/APPLICATION.md`에 명시된 TBD만 집약한다.

## 신청과 결제의 선후 관계 (2026-08-15 추가)

### 현재 상태

- 신청과 결제 중 무엇을 먼저 처리할지 아직 확정되지 않았다.
- 현재 백엔드는 신청서를 먼저 생성하고 초기 상태를 `PAYMENT_PENDING`, 결제 상태를 `WAITING`으로 저장하며 신청 생성 API에서 결제를 처리하지 않는다.
- 기존 요구사항에는 `상담 → 신청 → 계좌이체`로 기재되어 있으나, 아래 두 흐름 중 어느 것을 적용할지 논의가 끝나지 않았으므로 확정 정책으로 간주하지 않는다.

### 결정이 필요한 이유

- 선택 결과에 따라 신청 생성 API, 초기 상태, 결제 API와 트랜잭션 경계가 달라진다.
- 결제 전 이탈, 결제 실패, 미입금 자동 취소, 사용자 취소 및 환불 기준이 달라진다.
- 프론트 신청 단계와 완료 화면, 관리자 상담·입금 확인 업무 순서를 함께 변경해야 한다.

### 선택 가능한 대안

- **A안 — 신청 먼저**: `신청서 제출 → 상담/검토 → 결제 안내 → 입금 → 제작 → 발급`
- **B안 — 결제 먼저**: `신청서 작성 + 결제 → 접수 → 검토 → 제작 → 발급`

### 결정 후 영향

- `Application` 초기 상태와 상태 전이, `PaymentStatus` 정의
- 신청 생성 및 결제 요청·확인 API 계약
- 상담 결과에 따른 금액 결정 및 결제 기한
- `PAYMENT_PENDING`·`PHOTO_REJECTED` 사용자 취소, 미입금 자동 취소 및 환불 처리
- 결제 성공 후 신청 저장 실패 또는 신청 저장 후 결제 실패에 대한 보상 처리
- 프론트 신청 단계, 관리자 처리 화면, 관련 테스트와 Application·Payment 문서

---

- 신청 취소 시 파일 30일 보관 후 삭제 정책의 현재 적용 여부
- 향후 Cleanup Scheduler 도입 여부
- 취소·반려·회원 탈퇴·출력 완료 후 파일 처리와 보존 기간
- ZIP 최대 크기
- Excel 최대 행 수
- 최대 신청 인원
- ~~일일 신청 횟수에 취소·반려 신청을 포함할지 여부~~ (2026-08-16 해결 — 취소된 신청은 카운트에서 제외한다(자리가 다시 빔). 반려는 재시도가 update성 사진 재업로드라 새 `create()` 자체가 없어 별도 결정이 필요 없음. `APPLICATION.md` §7, 아래 "일일 KST 3회 제한" 항목 참고)
- 신청번호의 날짜별 번호 정책
- 운영 전 Idempotency-Key 도입 여부
- 학생증 공백 문자열 정책
- 학교명 필드 추가 여부
- quantity 정책
- 학생증 `department`(학과) 필드를 계속 유지할지, 제외할지 (2026-08-07 추가 — `APPLICATION.md`는 "현재 제외"로 명시했지만 근거가 없고, 사람이 아직 미결정으로 확인. 결정 전까지는 기존대로 필수 필드 유지)
- ~~일일 KST 3회 제한 DB 원자 처리 미구현~~ (2026-08-07 추가, 2026-08-16 정책 확정+구현 완료 — `checklist.md` §4·§5 항목 중 유일하게 남았던 미구현. `ApplicationDailyLimit`(사용자별·일자별 카운터) + `ApplicationDailyLimitService.reserveSlot/releaseSlot`(비관적 락 + 유니크 제약 충돌 재시도)로 구현. 상세는 `docs/collab/CHANGELOG.md` 2026-08-16 항목 참고)
- 국제 전화번호 형식 정책 (2026-08-08 추가 — `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `phone` 필드에 `UserUpdateRequest`와 동일한 국내향 정규식(`^[0-9\-]{9,20}$`)을 재사용하려 했으나, 외국인 신청자를 받는 서비스 특성상 국제번호(`+`, 국가코드 등)를 고려한 별도 정책 확정이 먼저 필요하다는 판단으로 보류함. 현재는 `@NotBlank`(필수 여부)만 적용되고 형식 검증은 없음)
- ~~`GlobalExceptionHandler`의 Bean Validation 다중 필드 오류 응답~~ (2026-08-08 해결 — `errors[]`에 위반된 모든 필드를 field 기준 정렬로 반환하도록 구현 완료. `docs/collab/CHANGELOG.md` 2026-08-08 항목 참고)
- 공통 오류 모델 정리 (2026-08-08 추가 — `ValidationErrorDetail(row, field, code, message)`는 원래 Bulk 단체 신청 전용으로 설계된 타입인데, `GlobalExceptionHandler`의 일반 Bean Validation 오류 응답에도 그대로 재사용하면서 `row`가 Bulk 경로에서만 의미 있고 일반 검증 경로에서는 항상 `null`인 상태가 됐다. 또한 두 경로의 `code` 값 어휘도 다르다 — Bulk는 `REQUIRED`/`INVALID_FORMAT`/`PHOTO_NOT_FOUND` 같은 의미 카테고리, 일반 Bean Validation은 `NotBlank`/`Size`/`ValidNationality` 같은 애노테이션 이름. 현재는 최소 변경 원칙에 따라 그대로 재사용했으나, 두 경로가 늘어나면 `row`를 Bulk 전용 하위 타입으로 분리하거나 공통 `code` 어휘를 정하는 등 별도 정리가 필요할 수 있음)
