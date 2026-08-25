# 테스트 리포트 (2026-08-25)

프론트-백엔드 연동 갭(`FRONTEND_API_GAPS.md` 우선순위 0~4) 구현을 마친 뒤, **백엔드 전체 테스트 스위트 + 프론트 타입체크·빌드 + 라이브 엔드포인트 스모크**를 실행한 결과.

- 대상 커밋 기준: `main` 워킹 트리(2026-08-25).
- 이 리포트는 구버전 `INTEGRATION_TEST_REPORT.md`(2026-08-18)를 대체한다.

---

## 0. 한눈에 보기

| 구간 | 방법 | 결과 |
|---|---|---|
| 백엔드 단위·통합 테스트 | Gradle `test` (JUnit 5, 84 스위트) | ✅ **612 / 612 통과** (실패 0, 오류 0, 스킵 0) |
| 프론트 타입체크 | `tsc --noEmit` | ✅ 통과 |
| 프론트 프로덕션 빌드 | `vite build` | ✅ 성공 (1.11s) |
| 라이브 엔드포인트 스모크 | 실기동 스택에 `curl` | ✅ 계약 정상 (메일 발송만 로컬 SMTP 미설정으로 503) |

---

## 1. 백엔드 테스트 스위트

### 1.1 실행 환경

| 항목 | 값 |
|---|---|
| 실행 방식 | `gradle:9.4.1-jdk21-alpine` 컨테이너에서 `gradle test --no-daemon` |
| 네트워크 | 실기동 `hc_default`(compose)에 연결 — 테스트 컨텍스트가 `redis:6379` 세션 저장소에 실제 접속 |
| DB | H2 인메모리(런타임 `com.h2database:h2`, `@SpringBootTest` 자동 구성) |
| Redis | `REDIS_HOST=redis` 주입 — `@SpringBootTest`가 실제 세션 스토어 검증 |
| 소요 | 테스트 태스크 2m 07s (테스트 합산 ~116s) |

> ⚠️ 참고: 로컬에 JDK가 없어 도커 이미지로 실행했다. Redis를 못 붙인 격리 실행(1차)에서는 세션 의존 `@SpringBootTest` 153건이 연결 실패로 떨어졌으나, compose 네트워크에 붙여 `REDIS_HOST=redis`를 준 재실행(2차)에서 **전건 통과**했다. 즉 1차 실패는 인프라 부재이지 코드 결함이 아니다.

### 1.2 결과

```
612 tests completed, 0 failed
BUILD SUCCESSFUL
```

| 계층 | 테스트 | 스위트 |
|---|---:|---:|
| `domain.application` (신청 도메인) | 214 | 26 |
| `api` (컨트롤러·MockMvc 통합) | 143 | 19 |
| `domain.review` (후기) | 67 | 8 |
| `domain.user` (회원·인증·복구) | 57 | 8 |
| `domain.event` (행사) | 51 | 4 |
| `infra` (security·mail) | 28 | 7 |
| `domain.board` (공지·FAQ) | 26 | 4 |
| `domain.inquiry` (문의) | 17 | 2 |
| `domain.sajuname` (작명 시드) | 4 | 2 |
| `common` (예외·페이지응답) | 3 | 2 |
| `flow` (사용자 신청 E2E) | 1 | 1 |
| 컨텍스트 로드(`HonorCitizenApplicationTests`) | 1 | 1 |
| **합계** | **612** | **84** |

---

## 2. 이번 프론트 변경(§0~4)을 뒷받침하는 백엔드 테스트

이번에 프론트가 연결한 엔드포인트는 모두 기존 백엔드 테스트가 계약을 고정하고 있다. 아래 스위트가 전부 통과했다.

| 프론트 변경(§) | 엔드포인트 | 커버하는 백엔드 테스트 (통과) |
|---|---|---|
| §1.1-a 회원가입 | `POST /api/auth/signup`, `.../email-verification/*` | `AuthControllerSignupTest`(5), `EmailVerificationServiceTest`(6), `EmailVerificationServiceConfirmTest`(6) |
| §1.1-b 이메일 중복확인 | `POST /api/auth/email/check` | `AuthControllerEmailCheckTest`(4) |
| §1.1-b 비밀번호 변경 | `PATCH /api/users/me/password` | `UserControllerChangePasswordTest`(5) |
| §1.9-b 학생증 schoolName / §1.12 국적 | `POST /api/applications` | `ApplicationCreateRequestValidationTest`(5), `ApplicationServiceTest` 외 |
| §1.10 재제출 파트 분기 | `PATCH /api/applications/{id}/photo` | `ApplicationPhotoControllerTest`(2), `ApplicationServicePhotoReuploadTest`(11) |
| §1.10 조회 `applicationType` | `POST /api/applications/lookup` | `ApplicationServiceLookupTest`(10) |
| §1.5 신청 취소 | `POST /api/applications/{id}/cancel` | `ApplicationStateTransitionTest`(6) |
| §1.2 내 신청 | `GET /api/my/applications` | `MyApplicationControllerTest`(6), `ApplicationServiceMyApplicationsTest`(7) |
| §1.3 문의 | `POST /api/inquiries` | `InquiryControllerTest`(4), `InquiryServiceTest`(15) |
| §1.14 FAQ | `GET /api/boards?type=FAQ` | `BoardControllerTest`(5) |

---

## 3. 프론트엔드

### 3.1 타입체크 · 빌드

- `npx tsc --noEmit` — 오류 0.
- `npx vite build` — 성공(1.11s). 이번에 손댄 청크: `SignupPage`(5.45 kB), `MyPage`(7.17 kB), `SupportPage`(6.44 kB), `ApplyPage`(55.90 kB), `MobileCardPage`(index 포함).

### 3.2 프론트 자동화 테스트 부재 (한계)

프론트엔드에는 단위 테스트 러너(vitest 등)가 구성돼 있지 않다(`package.json` scripts = dev/build/preview). 따라서 프론트 검증은 **타입체크 + 프로덕션 빌드 + 아래 라이브 스모크**로 갈음했다. UI 상호작용(인증코드 입력 흐름, 비밀번호 변경 폼 등)의 자동 회귀 테스트는 후속 과제다.

---

## 4. 라이브 엔드포인트 스모크

실기동 스택(backend·db·redis·minio·frontend, docker compose)에 직접 호출.

| 대상 | 기대 | 결과 |
|---|---|---|
| `POST /api/auth/email/check` | 200 + `{exists}` | ✅ 200 |
| `PATCH /api/users/me/password` (미인증) | 401 | ✅ 401 (엔드포인트 존재·보안 동작) |
| `GET /`(프론트, :3000) | 200 | ✅ 200 |
| `POST /api/auth/signup/email-verification/request` | 200 | ⚠️ **503** — 로컬 dev에 SMTP(메일 발송) 미설정. 프론트 연동 자체는 정상이며, 메일 서버가 구성된 환경에서만 실제 발송/E2E 가능 |

> 회원가입 전체 E2E(코드 수신→확인→가입)는 SMTP 구성 환경에서만 검증 가능하다. 백엔드 발송 로직은 `EmailVerificationServiceTest`/`SmtpEmailSenderTest`로 단위 검증돼 있다.

---

## 5. 재현 방법

```bash
# 백엔드 전체 테스트 (로컬 JDK 없을 때 — compose 네트워크의 redis 사용)
docker compose up -d redis db                      # 세션·DB 의존성 기동
cd backend/honor-citizen
docker run --rm --network hc_default \
  -e REDIS_HOST=redis -e REDIS_PORT=6379 \
  -v "$PWD:/app" -v hc-gradle-cache:/home/gradle/.gradle \
  -w /app gradle:9.4.1-jdk21-alpine gradle test --no-daemon
# 리포트: backend/honor-citizen/build/reports/tests/test/index.html

# 프론트
cd frontend && npx tsc --noEmit && npx vite build
```

> 로컬에 JDK 21이 설치돼 있으면 `./gradlew test`로 바로 실행 가능(단, `localhost:6379`에 Redis가 떠 있어야 세션 의존 통합 테스트가 통과한다).
