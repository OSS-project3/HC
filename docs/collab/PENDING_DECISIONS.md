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
