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
  - 4단계 전부 신규 테스트가 실제 로컬 Redis로 통과했고, 전체 스위트 410개 중 아래 "발견된 기존 결함" 1건만 실패(회귀 없음). 상세는 `TODO.md` 백엔드 API 연동 체크리스트 절, `CHANGELOG.md` 2026-08-19 항목들 참고.
- **⚠️ AUTH-4 진행 중 발견·수정한 버그(범위 내)**: `UserService`가 `PasswordEncoder`를 직접 주입받기 시작하자 `SecurityConfig`(PW-1에서 `PasswordEncoder` Bean을 여기 정의했었음)↔`UserService` 순환 의존이 생겨 `BeanCurrentlyInCreationException`으로 컨텍스트 로딩이 실패했다. `PasswordEncoder` Bean을 신규 `infra/security/PasswordEncoderConfig.java`(다른 의존 없는 최소 Configuration)로 분리해 해결 — Bean 타입/동작은 그대로.
- **❓ AUTH-4 관련 확인 필요(사용자 확인 대기, 아래 절 참고)**: `SignupRequest`에 `password`/`name` 필드를 추가했는데, 정책 원문("요청에는 email과 signupToken이 포함됩니다")엔 명시가 없었지만 구조상 반드시 필요해서 보충했다. 비밀번호 최소 길이(8자)도 임의로 정한 값이다.
- **⚠️ 로컬 테스트 환경 참고**: Redis 의존 테스트를 실제 Redis로 검증 중(사용자 선택). Docker Desktop에 전용 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**, 다른 프로젝트("ZeroTime")의 `zerotime-redis-local`이 이미 기본 포트 6379를 쓰고 있어 분리)를 띄워뒀다. 테스트 실행 시 `REDIS_PORT=6400 ./gradlew.bat test`로 지정해야 하며, `build.gradle`엔 커밋하지 않았다(CI/다른 환경엔 이 컨테이너가 없음 — 로컬 전용). Docker Desktop이 세션 사이 꺼져 있을 수 있으니, 다음 작업자는 `docker ps`로 컨테이너 상태부터 확인하고 필요하면 `docker start honor-citizen-redis-test`로 재기동한다.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패한다. 원인은 `POST /api/applications`가 `UserService.findEligibleApplicationUser()`에서 `TERMS_NOT_AGREED`(403)를 던지는 것 — 이 테스트가 로그인을 `User.createOAuthUser(...)`를 리포지토리에 직접 save하는 방식으로 재현하면서 실제 약관동의 단계(`POST /api/auth/terms`)를 거치지 않아 발생. 클린 `main` HEAD 기준으로도 동일하게 재현되어 이번 4단계 작업과 무관한 기존 결함임을 확인했다(테스트 픽스처가 약관동의 필수화 이후 갱신 안 된 것으로 추정). User/Application 도메인 테스트 파일이라 이번 작업 범위 밖 — 상세 근거는 `TODO.md` "발견된 기존 결함" 절 참고. 다음 작업자가 고칠 때는 로그인 재현 단계 뒤에 약관동의 단계를 추가하면 될 것으로 보인다.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 건드리지 않았다.

## 다음에 할 일

- **AUTH-4 확인 필요 항목 사용자 확인**: `SignupRequest.password`/`name` 필드 추가, 비밀번호 최소 길이(8자) — 정책에 없던 값이라 사용자 확인 후 필요시 조정.
- **UserApplicationFlowTest 403 수정**: 위 "발견된 기존 결함" 참고 — 담당자 미정, User/Application 도메인 작업자가 처리.
- **AUTH-3, RATE-1, AUTH-5, AUTH-6**: `TODO.md`에 설계까지 확정돼 있음. 이제 4단계(MAIL-1~AUTH-4)가 끝났으니 다음 순서로 착수 가능(`TODO.md` "작업 순서(의존관계 기준)" 참고 — RATE-1과 AUTH-3은 병렬 가능).
- 그 외 미착수 항목(Inquiry 도메인, 관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고 — 이번 세션과 무관.

## ❓ 확인 필요

- **`SignupRequest`에 `password`/`name` 필드 추가**: AUTH-4 정책 원문은 "요청에는 email과 signupToken이 포함됩니다"로만 서술했지만, 비밀번호 해시(4단계)·User 생성(5단계, `name`은 `NOT NULL` 컬럼)을 수행하려면 원문 비밀번호와 이름이 요청에 있어야 해서 추가했다. `phone`은 `FRONTEND_API_REQUIREMENTS.md` §3 예시엔 있지만 `User.createLocalUser`가 받지 않고 기존 `PATCH /api/users/me`로 채울 수 있어 넣지 않았다 — 이 판단이 맞는지 확인 필요.
- **비밀번호 최소 길이 8자**: 정책에 명시된 값이 아니라 임의로 정한 보수적 기본값. 실제 정책 확정 필요.

## 참고

- MAIL-1/SIGNUP-1/SIGNUP-2/AUTH-4 전부 신규 테스트(집중) 실행 후 전체 스위트(각각 394/400/406/410개, 회귀 테스트)를 돌려 확인했다 — 실패 1건(`UserApplicationFlowTest`)은 위에 기록한 대로 기존 결함, 네 단위 모두 동일건으로 회귀 아님을 재확인.
- 관련 문서: `docs/collab/TODO.md`(백엔드 API 연동 체크리스트 절), `docs/collab/CHANGELOG.md` 2026-08-19 항목들, `backend/FRONTEND_API_REQUIREMENTS.md` §3(이메일 회원가입 정책 원본).
