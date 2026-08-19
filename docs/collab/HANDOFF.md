# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **이메일 회원가입 인증 작업 진행 중 (MAIL-1 → SIGNUP-1 → SIGNUP-2 → AUTH-4 중 3/4 완료)** — 사용자가 지시한 4단계 작업.
  - **MAIL-1 완료·커밋됨**(`a79c08c`): `spring-boot-starter-mail`+`EmailSender`/`SmtpEmailSender` 인프라. 상세는 `TODO.md` MAIL-1 항목 참고.
  - **SIGNUP-1 완료·커밋됨**(`4c0a534`): `POST /api/auth/signup/email-verification/request` — 정책 9단계(정규화→형식검증→중복확인→재전송/횟수제한→코드생성→HMAC저장→TTL10분→SMTP발송→응답) 그대로 구현. 신규 테스트 6개 전부 **실제 로컬 Redis**(Docker `honor-citizen-redis-test`, 호스트 포트 6400)로 통과.
  - **SIGNUP-2 완료, 이번 커밋으로 반영**: `POST /api/auth/signup/email-verification/confirm` — 코드 확인+실패횟수 증가를 Redis Lua(`verify-and-increment-code.lua`)로 원자 처리, 불일치/만료/이미사용/5회초과 전부 `INVALID_VERIFICATION_CODE`(신규) 하나로 동일 응답, 성공 시 32바이트 URL-safe 가입 토큰 발급(Redis엔 SHA-256 해시만 저장, TTL 30분). 신규 테스트 6개 전부 실제 로컬 Redis로 통과. 상세는 `TODO.md` SIGNUP-2 항목, `CHANGELOG.md` 2026-08-19 항목 참고.
  - **다음 단위: AUTH-4**(회원가입 완료, `POST /api/auth/signup`) — 아직 미착수. signupToken+email 필수, DB commit 성공 후에만 토큰 삭제해야 함(순서 중요).
  - AUTH-1/AUTH-2/PW-1도 이미 완료·커밋됨(각각 `dba810d`/`c5b1b14`/`e167c19`).
- **⚠️ 로컬 테스트 환경 참고**: 이 작업부터 Redis 의존 테스트를 실제 Redis로 검증하기로 했다(사용자 선택). Docker Desktop에 전용 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**, 다른 프로젝트("ZeroTime")의 `zerotime-redis-local`이 이미 기본 포트 6379를 쓰고 있어 충돌 방지 목적으로 분리)를 띄워뒀다. 테스트 실행 시 `REDIS_PORT=6400 ./gradlew.bat test`로 지정해야 하며, `build.gradle`엔 커밋하지 않았다(CI/다른 환경엔 이 컨테이너가 없음 — 로컬 전용). Docker Desktop이 세션 사이 꺼져 있을 수 있으니, 다음 작업자는 `docker ps`로 컨테이너 상태부터 확인하고 필요하면 `docker start honor-citizen-redis-test`로 재기동한다.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패한다. 원인은 `POST /api/applications`가 `UserService.findEligibleApplicationUser()`에서 `TERMS_NOT_AGREED`(403)를 던지는 것 — 이 테스트가 로그인을 `User.createOAuthUser(...)`를 리포지토리에 직접 save하는 방식으로 재현하면서 실제 약관동의 단계(`POST /api/auth/terms`)를 거치지 않아 발생. 클린 `main` HEAD 기준으로도 동일하게 재현되어 SIGNUP-1과 무관한 기존 결함임을 확인했다(테스트 픽스처가 약관동의 필수화 이후 갱신 안 된 것으로 추정). User/Application 도메인 테스트 파일이라 이번 작업 범위 밖 — 상세 근거는 `TODO.md` "발견된 기존 결함" 절 참고. 다음 작업자가 고칠 때는 로그인 재현 단계 뒤에 약관동의 단계를 추가하면 될 것으로 보인다.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 건드리지 않았다.

## 다음에 할 일

- **AUTH-4 착수**: `POST /api/auth/signup` — signupToken+email 필수, 토큰 해시(SHA-256)로 Redis(`auth:signup:token:{hash}`) 조회 후 정규화 이메일 일치 검증(불일치면 이메일 존재 여부를 노출하지 않는 동일 오류), DB 중복 재조회, `PasswordEncoder`(BCrypt)로 해시, `User.createLocalUser` 저장(DB UNIQUE가 동시요청 최종 방어선), **DB commit 성공 후에만** 가입 토큰 삭제(먼저 삭제 금지), 기존 OAuth와 동일한 HttpOnly 쿠키 발급, `/terms`는 기존 `POST /api/auth/terms` 그대로 재사용. 상세 정책은 `TODO.md` AUTH-4 항목 참고.
- **UserApplicationFlowTest 403 수정**: 위 "발견된 기존 결함" 참고 — 담당자 미정, User/Application 도메인 작업자가 처리.
- **AUTH-3, RATE-1, AUTH-5, AUTH-6**: `TODO.md`에 설계까지 확정돼 있으나 이번 4단계(MAIL-1~AUTH-4) 완료 후 순서.
- 그 외 미착수 항목(Inquiry 도메인, 관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고 — 이번 세션과 무관.

## ❓ 확인 필요

- 없음(이번 세션 기준).

## 참고

- SIGNUP-1/SIGNUP-2 둘 다 신규 테스트(집중) 실행 후 전체 스위트(각각 400개/406개, 회귀 테스트)를 돌려 확인했다 — 실패 1건(`UserApplicationFlowTest`)은 위에 기록한 대로 기존 결함, 두 단위 모두 동일건으로 회귀 아님을 재확인.
- 관련 문서: `docs/collab/TODO.md`(백엔드 API 연동 체크리스트 절), `docs/collab/CHANGELOG.md` 2026-08-19 항목, `backend/FRONTEND_API_REQUIREMENTS.md` §3(이메일 회원가입 정책 원본).
