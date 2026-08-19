# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **일반 이메일 인증·로그인 전체 흐름 완료**: 회원가입 4단계(MAIL-1→SIGNUP-1→SIGNUP-2→AUTH-4)에 이어 로그인까지 끝났다.
  - **MAIL-1**(`a79c08c`)·**SIGNUP-1**(`4c0a534`)·**SIGNUP-2**(`bc37d8a`)·**AUTH-4**(`bc7d7ce`+후속 `f19830e`): 회원가입(이메일 인증 코드 요청/확인→가입 완료, `phone` 포함·비밀번호 8~72자 정책 확정)까지 완료·커밋됨.
  - **RATE-1**(`2cfcb74`): 로그인 실패 횟수 제한 — `infra/security/LoginAttemptLimiter`(정규화 이메일 SHA-256 해시 키, 15분 내 5회 실패→15분 잠금).
  - **AUTH-5**(이번 커밋): `POST /api/auth/login` — `LoginAttemptLimiter.checkNotLocked` → 계정없음/OAuth전용계정/비밀번호불일치를 전부 `INVALID_CREDENTIALS`(신규)로 동일 응답 → 탈퇴 계정은 `withdrawalRequestedAt` 날짜를 직접 비교해 7일 이내면 자동복구(`restored:true`), 지났으면 동일하게 거절 → 성공 시 카운터 리셋+로그인 토큰 발급.
  - **AUTH-3**(이번 커밋): `POST /api/auth/email/check` — 정규화 이메일로 `existsByEmail` 조회 후 boolean만 반환(OAuth 계정도 같은 UNIQUE 제약 공유해 함께 판정됨). AUTH-5와 의존관계 없어 병렬로 진행.
  - AUTH-1/AUTH-2/PW-1도 이미 완료·커밋됨(`dba810d`/`c5b1b14`/`e167c19`).
  - **AUTH-1~5·PW-1·MAIL-1·RATE-1 전부 완료 — 이 그룹에서 남은 건 AUTH-6(비밀번호 변경)뿐이다.**
  - 전체 스위트 431개 중 아래 "발견된 기존 결함" 1건만 실패(회귀 없음). 상세는 `TODO.md` 백엔드 API 연동 체크리스트 절, `CHANGELOG.md` 2026-08-19 항목들 참고.
- **✅ 프론트 연동 준비 상태 확인·문서화 완료**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13)·`docs/FRONTEND_API_GAPS.md`(§1.1)를 회원가입=완료/로그인·복구=일부 진행으로 최신화했고, 프론트 UX 결정 2건(회원가입 인증코드는 `SignupPage.tsx`에 인라인, 비밀번호 재설정은 코드확인+새비번을 한 화면에 통합 → 향후 `recovery/password/confirm` API도 단일요청으로 설계)을 기록해뒀다. **다만 로그인이 막 완료됐으므로 이 문서들의 "로그인·복구 BLOCKED" 표기와 §1.1(b)는 AUTH-5 완료를 아직 반영 못 한 상태 — 다음 세션에서 갱신 필요.**
- **⚠️ AUTH-4 때 발견·수정한 순환 의존 버그(유지 확정)**: `PasswordEncoder` Bean을 `SecurityConfig`에서 `infra/security/PasswordEncoderConfig.java`로 분리(순환 의존 해결). 사용자 확인 완료, 계속 유지.
- **⚠️ AUTH-5 설계 메모**: `User.isRestorable()`(OAuth 로그인도 사용 중)은 `anonymizedAt==null`만 확인해 7일 경과 여부가 스케줄러 실행 여부에 좌우된다 — AUTH-5는 이 문제를 피하려고 `withdrawalRequestedAt` 날짜를 직접 비교하는 로직을 `login()`에 별도로 추가했다(User/OAuth2SuccessHandler는 안 건드림, 그쪽엔 같은 잠재적 오차가 여전히 남아있음 — 필요시 별도 단위로 통일 검토).
- **⚠️ 로컬 테스트 환경 참고**: Redis 의존 테스트를 실제 Redis로 검증 중. Docker 전용 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**)를 띄워뒀다. 테스트 실행 시 `REDIS_PORT=6400 ./gradlew.bat test`로 지정, `build.gradle`엔 커밋 안 함(로컬 전용). Docker Desktop이 꺼져 있으면 `docker start honor-citizen-redis-test`로 재기동.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패한다 — `POST /api/applications`가 `TERMS_NOT_AGREED`를 던지는데, 이 테스트가 약관동의 단계를 안 거치고 로그인만 재현해서 발생. 클린 `main` HEAD에서도 재현되는 기존 결함(회귀 아님), User/Application 도메인 테스트라 이번 작업 범위 밖. 상세는 `TODO.md` "발견된 기존 결함" 절 참고.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 건드리지 않았다.

## 다음에 할 일

- **문서 갱신**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`/`docs/FRONTEND_API_GAPS.md`의 "로그인 BLOCKED" 표기를 AUTH-5/AUTH-3 완료로 갱신(위 참고, 아직 미반영).
- **AUTH-6 착수**: `PATCH /api/users/me/password` — 로그인 사용자의 현재 비밀번호 확인 후 새 비밀번호로 교체. OAuth 전용 계정(`passwordHash=null`)은 이 API 자체를 차단. AUTH-1~5 전부 끝나 선행조건 충족 — 이 그룹의 마지막 단위.
- **프론트 실 연동**: 백엔드가 준비된 회원가입/로그인 API를 `SignupPage.tsx`/`LoginPage.tsx`가 아직 mock으로만 처리 중 — 실 API 연동은 이번 세션 범위 밖(프론트 작업).
- **UserApplicationFlowTest 403 수정**: 위 "발견된 기존 결함" 참고 — 담당자 미정, User/Application 도메인 작업자가 처리.
- 그 외 미착수 항목(Inquiry 도메인, 관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고 — 이번 세션과 무관.

## ❓ 확인 필요

- 없음(이번 세션 기준 — RATE-1이 남겼던 `ACCOUNT_LOCKED` 노출 여부는 AUTH-5 구현으로 확정 반영됨).

## 참고

- MAIL-1/SIGNUP-1/SIGNUP-2/AUTH-4/RATE-1/AUTH-5/AUTH-3 전부 신규 테스트(집중) 실행 후 전체 스위트(394/400/406/411/416/427/431개, 회귀 테스트)를 돌려 확인했다 — 실패 1건(`UserApplicationFlowTest`)은 위에 기록한 대로 기존 결함, 일곱 단위 모두 동일건으로 회귀 아님을 재확인.
- 관련 문서: `docs/collab/TODO.md`(백엔드 API 연동 체크리스트 절), `docs/collab/CHANGELOG.md` 2026-08-19 항목들, `backend/FRONTEND_API_REQUIREMENTS.md` §3(이메일 회원가입 정책 원본), `docs/api/auth.md`(auth API 전체 최신 계약).
