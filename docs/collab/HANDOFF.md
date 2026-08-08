# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-09
- 작성자: Codex
- 작성 브랜치: main

## 지금 어디까지 됐는가

- Application failure-path/contract 검증 작업은 논리 단위별 커밋으로 분리 완료됐다.
- Redis 미기동 상태에서 전체 테스트 159개 중 3개가 실패했으나, Redis 기동 후 실패 원인을 다시 분리했다.
- Redis 기동 후 `UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken`, `UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall`은 통과했다.
- `UserApplicationFlowTest.fullUserApplicationFlow`는 Redis 연결 실패는 해소됐지만, 개인 신청 생성 단계에서 `TERMS_NOT_AGREED` 403으로 실패한다.
- 위 남은 실패는 Redis 환경 문제가 아니라 테스트 플로우가 현재 신청 정책(신청 전 필수 약관 동의)을 반영하지 못한 상태로 분류했다.
- 재검증 결과는 `docs/specs/application/checklist.md`의 `2026-08-09 Redis Retry Verification` 섹션과 `docs/collab/CHANGELOG.md` 최신 항목에 기록했다.

## 다음에 할 일

- `UserApplicationFlowTest.fullUserApplicationFlow`에 약관 동의 단계를 추가할지 결정 후 수정한다.
  - 현재 정책상 Application 신청 전 `UserService.findEligibleApplicationUser()`가 필수 약관 동의를 요구한다.
  - 테스트 사용자는 `User.createNewUser(...)` 직후 `userService.issueLoginTokens(user)`만 수행하고 약관 동의를 하지 않아 `TERMS_NOT_AGREED`가 발생한다.
- 테스트 수정 시에는 정책 변경이 아니라 현재 정책에 맞춘 플로우 보정으로 처리한다.
- Redis가 필요한 테스트는 로컬 Redis 기동 상태를 전제로 실행하거나, 별도 test profile/mock 전략을 후속으로 검토한다.

## ❓ 확인 필요

- `UserApplicationFlowTest`에서 실제 OAuth 신규 사용자 플로우처럼 `/api/auth/terms` 또는 `User.agreeTerms(...)`에 해당하는 단계를 HTTP/Service 중 어떤 방식으로 재현할지 결정 필요.
- Redis 의존 테스트를 장기적으로 실제 Redis 전제로 둘지, 테스트용 mock/embedded/testcontainer 전략으로 바꿀지 결정 필요.

## 참고

- 관련 테스트 실행:
  - `./gradlew.bat test --tests "com.example.honorcitizen.api.UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken" --tests "com.example.honorcitizen.api.UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall" --tests "com.example.honorcitizen.flow.UserApplicationFlowTest.fullUserApplicationFlow"`
- 결과: 3개 중 2개 통과, 1개 실패(`UserApplicationFlowTest.fullUserApplicationFlow` — `TERMS_NOT_AGREED` 403)
- 관련 문서: `docs/specs/application/checklist.md`, `docs/collab/CHANGELOG.md`
