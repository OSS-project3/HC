# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **이메일 회원가입 인증 작업 완료 (MAIL-1 → SIGNUP-1 → SIGNUP-2 → AUTH-4, 4/4 완료)** — 사용자가 지시한 4단계 작업, 전부 완료해 커밋(이번 커밋으로 AUTH-4 반영).
  - **MAIL-1**(`a79c08c`): `spring-boot-starter-mail`+`EmailSender`/`SmtpEmailSender` 인프라.
  - **SIGNUP-1**(`4c0a534`): `POST /api/auth/signup/email-verification/request` — 정책 9단계 그대로 구현.
  - **SIGNUP-2**(`bc37d8a`): `POST /api/auth/signup/email-verification/confirm` — Redis Lua로 코드 확인+실패횟수 원자 처리, 32바이트 가입 토큰 발급.
  - **AUTH-4**(이번 커밋): `POST /api/auth/signup` — signupToken 검증(`EmailVerificationService.consumeSignupToken`) → `UserService.registerLocalUser`(중복재조회+BCrypt 해시+저장+로그인토큰 발급, 하나의 트랜잭션) → 그 트랜잭션이 실제 commit된 뒤에만 Redis 가입 토큰 삭제 → HttpOnly 쿠키 발급.
  - AUTH-1/AUTH-2/PW-1도 완료·커밋됨(각각 `dba810d`/`c5b1b14`/`e167c19`).
  - 4단계 전부 신규 테스트가 실제 로컬 Redis로 통과했고, 전체 스위트 411개 중 아래 "발견된 기존 결함" 1건만 실패(회귀 없음). 상세는 `TODO.md` 백엔드 API 연동 체크리스트 절, `CHANGELOG.md` 2026-08-19 항목들 참고.
- **⚠️ AUTH-4 진행 중 발견·수정한 버그(범위 내, 유지 확정)**: `UserService`가 `PasswordEncoder`를 직접 주입받기 시작하자 `SecurityConfig`(PW-1에서 `PasswordEncoder` Bean을 여기 정의했었음)↔`UserService` 순환 의존이 생겨 `BeanCurrentlyInCreationException`으로 컨텍스트 로딩이 실패했다. `PasswordEncoder` Bean을 신규 `infra/security/PasswordEncoderConfig.java`(다른 의존 없는 최소 Configuration)로 분리해 해결 — Bean 타입/동작은 그대로. 사용자가 이 분리를 그대로 유지하도록 확인함(2026-08-19).
- **✅ AUTH-4 확인 필요 항목 전부 확정 반영(2026-08-19)**: `SignupRequest.password`/`name` 필드 추가는 방향이 맞다고 확인받음. `phone`은 프론트 `SignupPage.tsx`를 재확인한 결과 회원가입 화면이 이미 필수 입력값으로 받고 있어 **포함으로 변경**(`SignupRequest.phone` 추가, `UserService.registerLocalUser`가 `updateProfile(null, phone)`으로 채움 — 엔티티 팩토리 시그니처는 안 바꿈). 비밀번호는 **최소 8자·최대 72자, 복잡도 규칙 없음으로 확정**(기존 구현값과 일치). 확정 내용을 `backend/FRONTEND_API_REQUIREMENTS.md`·`docs/api/auth.md`(API 4/5/6 신규)·`SignupRequest.java`·프론트 `SignupPage.tsx`(비밀번호 검증 완화)에 전부 반영했다. 상세는 `TODO.md` AUTH-4 항목, `CHANGELOG.md` 2026-08-19 두 번째 항목 참고.
- **문서 정합성 후속 작업 완료(2026-08-19)**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13 신설)·`docs/FRONTEND_API_GAPS.md`(§1.1을 회원가입=완료/로그인·복구=미구현으로 분리)가 AUTH-4 완료를 반영 못 하고 있던 걸 발견해 최신화. 프론트 UX 결정 2건도 함께 기록: 회원가입 인증코드는 `SignupPage.tsx`에 인라인으로, 비밀번호 재설정은 코드확인+새비번입력을 한 화면으로 통합(→ 향후 `recovery/password/confirm` API도 단일요청으로 설계해야 함).
- **RATE-1 완료, 이번 커밋으로 반영**: `infra/security/LoginAttemptLimiter.java` — 정규화 이메일을 SHA-256 해시해 Redis 키로 씀, 15분 내 5회 실패 시 15분 잠금(`checkNotLocked`/`recordFailure`/`reset`). AUTH-3/RATE-1/AUTH-5/AUTH-6 중 AUTH-5(로그인)의 유일한 선행 작업이라 critical path상 먼저 진행함(AUTH-3은 어느 쪽도 안 막는 병렬 작업). 신규 테스트 5개 전부 실제 로컬 Redis로 통과.
- **⚠️ 로컬 테스트 환경 참고**: Redis 의존 테스트를 실제 Redis로 검증 중(사용자 선택). Docker Desktop에 전용 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**, 다른 프로젝트("ZeroTime")의 `zerotime-redis-local`이 이미 기본 포트 6379를 쓰고 있어 분리)를 띄워뒀다. 테스트 실행 시 `REDIS_PORT=6400 ./gradlew.bat test`로 지정해야 하며, `build.gradle`엔 커밋하지 않았다(CI/다른 환경엔 이 컨테이너가 없음 — 로컬 전용). Docker Desktop이 세션 사이 꺼져 있을 수 있으니, 다음 작업자는 `docker ps`로 컨테이너 상태부터 확인하고 필요하면 `docker start honor-citizen-redis-test`로 재기동한다.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패한다. 원인은 `POST /api/applications`가 `UserService.findEligibleApplicationUser()`에서 `TERMS_NOT_AGREED`(403)를 던지는 것 — 이 테스트가 로그인을 `User.createOAuthUser(...)`를 리포지토리에 직접 save하는 방식으로 재현하면서 실제 약관동의 단계(`POST /api/auth/terms`)를 거치지 않아 발생. 클린 `main` HEAD 기준으로도 동일하게 재현되어 이번 4단계 작업과 무관한 기존 결함임을 확인했다(테스트 픽스처가 약관동의 필수화 이후 갱신 안 된 것으로 추정). User/Application 도메인 테스트 파일이라 이번 작업 범위 밖 — 상세 근거는 `TODO.md` "발견된 기존 결함" 절 참고. 다음 작업자가 고칠 때는 로그인 재현 단계 뒤에 약관동의 단계를 추가하면 될 것으로 보인다.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 건드리지 않았다.

## 다음에 할 일

- **AUTH-5 착수**: `POST /api/auth/login` — `LoginAttemptLimiter.checkNotLocked` 우선 확인(잠겨있으면 비밀번호 검증 없이 즉시 거절) → `findByEmail` → 계정없음/`passwordHash==null`(OAuth전용)/비밀번호불일치를 전부 동일한 `INVALID_CREDENTIALS`(신규 코드 필요)로 응답 및 `recordFailure` 호출 → 소프트탈퇴 7일 유예 자동복구+`restored:true` → 성공 시 `LoginAttemptLimiter.reset` 호출. 상세는 `TODO.md` AUTH-5 항목 참고.
- **AUTH-3 (병렬 가능)**: `POST /api/auth/email/check` — AUTH-5와 의존관계 없어 언제 해도 됨.
- **UserApplicationFlowTest 403 수정**: 위 "발견된 기존 결함" 참고 — 담당자 미정, User/Application 도메인 작업자가 처리.
- 그 외 미착수 항목(Inquiry 도메인, 관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고 — 이번 세션과 무관.

## ❓ 확인 필요

- **RATE-1의 `ACCOUNT_LOCKED` 노출 여부**: "잠금 중엔 올바른 비밀번호로도 거절"이 AUTH-5에서 `INVALID_CREDENTIALS`로 뭉뚱그려질지, `ACCOUNT_LOCKED`를 그대로 노출할지 정책에 명시가 없었다. 계정 존재 여부와 무관하게 카운트가 증가해 잠금 사유를 알려줘도 정보가 새지 않는다고 판단해 **`ACCOUNT_LOCKED`를 그대로 노출하는 방향으로 설계**했음 — AUTH-5 구현 시 재확인 필요.

## 참고

- MAIL-1/SIGNUP-1/SIGNUP-2/AUTH-4/RATE-1 전부 신규 테스트(집중) 실행 후 전체 스위트(394/400/406/411/416개, 회귀 테스트)를 돌려 확인했다 — 실패 1건(`UserApplicationFlowTest`)은 위에 기록한 대로 기존 결함, 다섯 단위 모두 동일건으로 회귀 아님을 재확인.
- 관련 문서: `docs/collab/TODO.md`(백엔드 API 연동 체크리스트 절), `docs/collab/CHANGELOG.md` 2026-08-19 항목들, `backend/FRONTEND_API_REQUIREMENTS.md` §3(이메일 회원가입 정책 원본).
