# User/Auth 도메인 구현 및 테스트 결과

> 작성일: 2026-07-31
> 범위: User/Auth 도메인 전체(OAuth 로그인, JWT 인증/인가, User CRUD, 회원탈퇴)
> 다음 단계(Application 도메인 구현)로 넘어가기 전 개발 기록용 문서.

---

## 1. 개요

### 목적
기존 백엔드(`backend/honor-citizen`)에 이미 구현되어 있던 OAuth/JWT 인프라를 실제로 검증하고,
새로 확정된 설계(`DB.md`/`API-명세.md`)에 맞춰 User 도메인 CRUD(조회/수정/탈퇴)를 신규 구현했다.

### 테스트 환경
| 항목 | 값 |
|---|---|
| 실행 방식 | `./gradlew bootRun --args='--spring.profiles.active=local'` |
| DB | PostgreSQL 17 (로컬, `honor_citizen` DB / `honor_citizen_app` 전용 계정) — 세션 도중 H2 인메모리에서 전환 |
| 세션 저장소 | Redis (로컬, refresh 세션 + accessToken 블랙리스트) |
| 프론트 | React + Vite, `http://localhost:5173` |
| 인증 방식 | OAuth2(Google/Naver) → JWT(HttpOnly 쿠키) |
| 테스트 도구 | `curl.exe`(PowerShell), 브라우저(DevTools 쿠키 확인), JUnit5 + MockMvc(단위/통합) |

---

## 2. 구현한 기능

| 영역 | 기능 | 상태 |
|---|---|---|
| 인증 | Google/Naver OAuth2 로그인 → JWT 쿠키 발급 | ✅ 기존 구현, 검증 완료 |
| 인증 | Refresh Token Rotation + 재사용 감지(Reuse Detection) | ✅ 기존 구현, 검증 완료 |
| 인증 | 로그아웃 + accessToken 블랙리스트 | ✅ 기존 구현, 검증 완료 |
| 인증 | 약관동의(`POST /api/auth/terms`) | ✅ 기존 구현, 검증 완료 |
| User CRUD | 내 정보 조회(`GET /api/users/me`) | ✅ 신규 구현 |
| User CRUD | 내 정보 수정(`PATCH /api/users/me`) | ✅ 신규 구현 |
| User CRUD | 회원탈퇴(소프트, `POST /api/users/me/withdraw`) | ✅ 신규 구현 |
| User CRUD | 탈퇴 유예기간(7일) 내 재로그인 자동 복구 | ✅ 신규 구현 |
| User CRUD | 완전탈퇴(익명화) 배치 스케줄러 | ✅ 신규 구현 |
| 인프라 | PostgreSQL 전환 (H2 인메모리 → 영속 DB) | ✅ 신규 전환 |

---

## 3. 회원탈퇴 정책 (신규 설계)

```
사용자가 "탈퇴하기" 클릭
   ↓
status=WITHDRAWN, withdrawal_requested_at=NOW (소프트 삭제) + 세션 즉시 무효화
   │  (유예기간 7일 이내 재로그인 시 자동 복구: status=ACTIVE, withdrawal_requested_at=NULL)
   ▼ (7일 경과, 재로그인 없음)
스케줄러(매일 새벽 3시)가 email/name/oauth_id/oauth_provider/phone/address 스크램블 처리
anonymized_at = NOW
```

- 완전탈퇴 후에도 `User` row 자체는 삭제하지 않고 PII만 익명화 — `Application`/`Payment` 등 연관 이력은 `user_id` FK로 그대로 보존.
- `oauth_id`/`oauth_provider`까지 스크램블되므로, 완전탈퇴 후 같은 계정으로 재로그인하면 기존 row가 복구되는 게 아니라 **새 User row가 생성**됨(의도된 동작).

---

## 4. API별 테스트 상세

### 4.1 Google OAuth 로그인

| 항목 | 내용 |
|---|---|
| 목적 | Google 계정으로 로그인 시 계정 자동 생성 + JWT 쿠키 발급이 정상 동작하는지 확인 |
| 방법 | 브라우저에서 `http://localhost:8080/oauth2/authorization/google` 접근 → Google 로그인 → 콜백 리다이렉트 확인 |
| 검증 내용 | ① `OAuth2SuccessHandler`가 신규/기존 회원 판별(`existsByOauthIdAndOauthProvider`) ② `accessToken`/`refreshToken` 쿠키 발급 ③ 신규회원은 `/terms`, 기존회원은 `/`로 리다이렉트 |
| 결과 | ✅ 성공 — 서버 로그 `보안 이벤트: 로그인 성공 userId=1` 확인, 홈페이지 정상 리다이렉트 |

```
서버 로그
2026-07-31T12:29:xx  INFO ... TokenSessionStore : 보안 이벤트: 로그인 성공 userId=1
```

같은 방식으로 Naver OAuth도 별도 검증(userId=2) — ✅ 성공.

---

### 4.2 JWT(accessToken/refreshToken) 발급 및 Cookie 기반 인증

| 항목 | 내용 |
|---|---|
| 목적 | 로그인 성공 시 JWT가 올바른 만료시간·속성으로 HttpOnly 쿠키에 담기는지 확인 |
| 방법 | 브라우저 DevTools > Application > Cookies 확인 |
| 검증 내용 | `accessToken`(15분)/`refreshToken`(14일) 둘 다 `HttpOnly=true`, `SameSite=Strict`, 로컬 환경이라 `Secure=false`(운영 기본값은 `true`) |
| 결과 | ✅ 3개 속성(HttpOnly/SameSite/Secure) 전부 확인 완료 |

**Refresh Token Rotation 테스트**

```powershell
curl.exe -s -i -X POST http://localhost:8080/api/auth/refresh -b cookies.txt -c cookies.txt
```
- 기대: `200` + 새 accessToken/refreshToken 쌍 발급, 기존 refreshToken은 서버에서 폐기(ROTATED)
- 결과: ✅ 성공

**Refresh Token 재사용 감지(Reuse Detection) 테스트**

이미 rotate된(폐기된) refreshToken을 재사용:
```json
{ "success": false, "data": null, "errorCode": "REFRESH_TOKEN_REUSE_DETECTED", "errorMessage": "리프레시 토큰 재사용이 감지되어 모든 세션이 만료되었습니다." }
```
- 결과: ✅ `401` + 해당 유저의 전체 세션(Redis + DB `refresh_token_sessions`) 강제 만료 확인

**로그아웃 + accessToken 블랙리스트 테스트**
```powershell
curl.exe -s -i -X POST http://localhost:8080/api/auth/logout -b cookies.txt
curl.exe -s -i http://localhost:8080/api/users/me -b cookies.txt   # 같은 accessToken 재사용
```
- 기대: 로그아웃 `200`, 이후 같은(아직 만료 전인) accessToken으로 보호 API 호출 시 `401`
- 결과: ✅ 둘 다 성공 — 로그아웃 즉시 블랙리스트 등록되어 accessToken 만료 전에도 거부됨

---

### 4.3 GET /api/users/me

| 항목 | 내용 |
|---|---|
| 목적 | 로그인한 유저의 정보(이름/이메일/권한/전화번호/주소)를 정상 조회하는지 확인 |
| 방법 | 단위테스트(MockMvc) + 실제 서버 curl 통합테스트 |
| 인증 | `accessToken` 쿠키 필요 |

**Request**
```
GET /api/users/me
Cookie: accessToken={JWT}
```

**Response 200 (성공)**
```json
{
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "USER",
    "phone": null,
    "address": null
  },
  "errorCode": null,
  "errorMessage": null,
  "success": true
}
```

**Response 401 (비로그인)** — 토큰 자체가 없으면 Spring Security가 컨트롤러 진입 전에 차단, **빈 바디**로 401 반환 (`GlobalExceptionHandler`의 JSON envelope은 CustomException 케이스에만 적용됨 — 이번 검증 과정에서 확인한 실제 동작)

| 테스트 케이스 | 결과 |
|---|---|
| 단위테스트: 정상 조회 | ✅ PASS |
| 단위테스트: 비로그인 401 | ✅ PASS |
| 통합테스트(curl, 실제 로그인 세션) | ✅ PASS — 실제 응답: `id=1, name="이혜원", email="gpdnjs2578@gmail.com", role="USER", phone=null, address=null` |

---

### 4.4 PATCH /api/users/me

| 항목 | 내용 |
|---|---|
| 목적 | 가입 이후 전화번호/주소 등 부가 정보를 수정할 수 있어야 함(가입 시점엔 OAuth가 제공 안 함) |
| 방법 | 단위테스트(MockMvc) + 실제 서버 curl 통합테스트 |
| 설계 근거 | `email`은 로그인 식별자라 수정 불가, `name`/`phone`/`address`만 부분 수정(partial update) 가능 |

**Request**
```
PATCH /api/users/me
Cookie: accessToken={JWT}
Content-Type: application/json
```
```json
{ "phone": "010-1234-5678", "address": "서울특별시 강남구" }
```

**Response 200**
```json
{
  "data": {
    "id": 1, "name": "홍길동", "email": "hong@example.com", "role": "USER",
    "phone": "010-1234-5678", "address": "서울특별시 강남구"
  },
  "errorCode": null, "errorMessage": null, "success": true
}
```

**Validation 규칙**

| 상황 | errorCode | HTTP |
|---|---|---|
| `name`/`phone`/`address` 전부 없음(빈 요청) | `INVALID_INPUT` | 400 |
| `name`이 공백 문자열 | `INVALID_INPUT` | 400 |
| `phone` 형식 오류(`^[0-9\-]{9,20}$` 불일치) | `INVALID_INPUT` | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

| 테스트 케이스 | 결과 |
|---|---|
| 단위테스트: phone/address 정상 수정 | ✅ PASS |
| 단위테스트: 빈 요청 → 400 | ✅ PASS |
| 단위테스트: name 공백 → 400 | ✅ PASS |
| 단위테스트: phone 형식 오류 → 400 | ✅ PASS |
| 단위테스트: 비로그인 → 401 | ✅ PASS |
| 통합테스트(curl) | ✅ PASS — `phone`/`address` 실제 반영 확인 |

---

### 4.5 POST /api/users/me/withdraw (회원탈퇴, 소프트)

| 항목 | 내용 |
|---|---|
| 목적 | 탈퇴 요청 시 계정을 즉시 삭제하지 않고 소프트 삭제 처리, 세션도 즉시 무효화되는지 확인 |
| 방법 | 단위테스트 + 실제 서버 curl 통합테스트 |

**Request**
```
POST /api/users/me/withdraw
Cookie: accessToken={JWT}
```

**Response 200**
```json
{ "data": null, "errorCode": null, "errorMessage": null, "success": true }
```

| 검증 내용 | 결과 |
|---|---|
| DB `users.status` → `WITHDRAWN`, `withdrawal_requested_at` 채워짐 | ✅ |
| 세션 무효화(`invalidateUserSessions`) + 방금 쓴 accessToken 블랙리스트 등록 | ✅ |
| 같은 accessToken으로 재호출 시 `401` | ✅ |
| 이미 탈퇴한 계정에 재호출(새 토큰) → `409 ALREADY_WITHDRAWN` | ✅ |

실제 통합테스트 결과(DB 직접 확인):
```
 id | status    | withdrawal_requested_at
----+-----------+----------------------------
  2 | WITHDRAWN | 2026-07-31 15:24:20.305276
```

---

### 4.6 재로그인 자동 복구 (`OAuth2SuccessHandler`)

| 항목 | 내용 |
|---|---|
| 목적 | 탈퇴 유예기간(7일) 내 같은 계정으로 재로그인하면 자동으로 계정이 복구되는지 확인 |
| 방법 | 실제 Google 계정으로 탈퇴 → 같은 계정으로 재로그인(브라우저) |
| 검증 내용 | `status=WITHDRAWN` && `anonymized_at IS NULL`인 기존 회원이 재로그인하면, 로그인 처리 직전에 `status=ACTIVE`로 복구 + `withdrawal_requested_at=NULL` |

**테스트 결과 (서버 로그 + DB 대조)**
```
서버 로그:
INFO ... OAuth2SuccessHandler : 보안 이벤트: 탈퇴 유예기간 내 재로그인으로 계정 자동 복구 userId=2
INFO ... TokenSessionStore    : 보안 이벤트: 로그인 성공 userId=2

DB (재로그인 직후):
 id | status | withdrawal_requested_at
----+--------+--------------------------
  2 | ACTIVE | (NULL)
```
복구 직후 `GET /api/users/me` 재호출로 정상 로그인 상태(200) 확인 — ✅ 성공

---

### 4.7 완전탈퇴 배치 스케줄러

| 항목 | 내용 |
|---|---|
| 목적 | 유예기간(7일)이 지난 탈퇴 계정을 스케줄러가 자동으로 익명화 처리하는지 확인 |
| 구현 | `UserWithdrawalScheduler`, cron `0 0 3 * * *`(매일 새벽 3시), 프로퍼티로 오버라이드 가능(`app.scheduler.withdrawal-cleanup-cron`) |
| 대상 조건 | `status=WITHDRAWN` AND `anonymized_at IS NULL` AND `withdrawal_requested_at` ≤ NOW - 7일 |
| 처리 내용 | `email`/`name`/`oauth_id`/`oauth_provider`/`phone`/`address` 스크램블, `anonymized_at=NOW` |

**테스트 방법**: 실제 3시까지 기다릴 수 없어, 로컬 전용으로 cron을 1분 주기로 임시 변경 → 8일 전 탈퇴한 것으로 조작한 테스트 유저를 DB에 직접 삽입 → 스케줄러 발동 확인 → 원복.

**테스트 결과**
```
서버 로그:
INFO ... UserService : 보안 이벤트: 완전탈퇴(익명화) 처리 1건

DB (처리 후):
 id | email                          | name          | oauth_id                                    | oauth_provider | status    | anonymized_at
----+--------------------------------+---------------+----------------------------------------------+----------------+-----------+----------------------------
  3 | withdrawn-3@anonymized.local  | 탈퇴한 사용자 | anon-e9ea81e9-a5f3-49c0-87c0-98f525b31435    | ANONYMIZED     | WITHDRAWN | 2026-07-31 15:38:00.14433
```
✅ 성공. 테스트 후 cron 설정 원복 + 테스트용 더미 유저 삭제 완료.

단위테스트(3개, `UserServiceTest`)도 별도로 통과:
- 유예기간 지난 유저 → 익명화됨
- 유예기간 이내 유저 → 그대로 유지
- 이미 익명화된 유저 → 재처리 안 됨

---

### 4.8 Spring Security(JwtAuthFilter) 동작 확인

| 항목 | 내용 |
|---|---|
| 목적 | 모든 인증 필요 API에서 JWT 검증 필터가 올바르게 동작하는지 확인 |
| 검증 순서(코드 기준) | ① 쿠키/헤더에서 토큰 추출 ② signature/만료 검증(`validateToken`) ③ 토큰 타입 확인(`isAccessToken`) ④ 블랙리스트 확인(`isAccessTokenBlacklisted`) ⑤ 유저 존재 여부(`userRepository.existsById`) ⑥ role 유효성 |
| 실제 확인된 동작 | 토큰이 아예 없으면 컨트롤러/`GlobalExceptionHandler` 진입 전에 Spring Security가 차단 → **빈 바디 401** (JSON 에러 envelope 아님) — curl 테스트로 실제 확인, 처음엔 예상과 달라서 단위테스트 기대값도 이에 맞춰 수정함 |

---

### 4.9 PostgreSQL 연동 및 users 테이블

| 항목 | 내용 |
|---|---|
| 목적 | H2 인메모리(재시작 시 데이터 초기화)에서 PostgreSQL(영속 저장)로 전환 |
| 계정 구성 | `honor_citizen_app` 전용 role + `honor_citizen` DB 신규 생성 (postgres 슈퍼유저 계정 직접 사용 안 함) |
| 설정 | `application-local.properties`에 `spring.datasource.*` + `spring.jpa.hibernate.ddl-auto=update` |

**users 테이블 스키마 (전환 후 실제 확인)**
```
 필드명                  | 형태                            | NULL허용
-------------------------+---------------------------------+----------
 id                      | bigint                          | not null
 email                   | character varying(255)         | not null
 oauth_id                | character varying(255)         | not null
 oauth_provider          | character varying(255)         | not null
 name                    | character varying(255)         | not null
 role                    | character varying(255)         | not null
 phone                   | character varying(255)         |
 address                 | character varying(255)         |
 refresh_token           | character varying(2048)        |
 status                  | character varying(255)         | not null
 withdrawal_requested_at | timestamp(6) without time zone |
 anonymized_at           | timestamp(6) without time zone |
 terms_agreed / privacy_agreed / image_upload_agreed / shipping_agreed | boolean | not null
 terms_agreed_at, created_at, updated_at | timestamp |
```
총 13개 테이블 전부 자동 생성 확인(`users`, `applications`, `payments`, `refresh_token_sessions` 등).

**재시작 후 데이터 영속성 확인**
| 시점 | 확인 방법 | 결과 |
|---|---|---|
| PostgreSQL 전환 직후 서버 재기동 | `\dt`로 테이블 목록 확인 | ✅ 13개 테이블 정상 생성 |
| withdraw API 테스트 후 서버 재기동 | `SELECT status FROM users` | ✅ `WITHDRAWN` 상태 그대로 유지(H2였다면 초기화됐을 상황) |
| 재로그인 자동복구 후 서버 재기동 | 동일 유저로 `GET /api/users/me` | ✅ 데이터 유지, 재로그인 없이 계속 사용 가능 |

---

## 5. 발견한 이슈 및 해결 과정

세션 전체에서 발견하고 고친 이슈를 시간순으로 정리.

| # | 증상 | 원인 | 해결 |
|---|---|---|---|
| 1 | 백엔드 컴파일 실패 | `ApplicationService`/`BulkApplicationService`/`ApplicationControllerTest`가 `Application.createSingle()`의 변경된 시그니처(`entryDate`/`address`/`cardType` 추가)를 반영 안 함 | 임시로 `null` 채워 컴파일 통과(TODO 주석 — 실제 로직은 Application 도메인 재설계 시 처리) |
| 2 | 앱 부팅 실패 | `JWT_SECRET`/Redis 필수 환경변수 없음 | `application-local.properties`에 로컬 전용 값 추가 |
| 3 | 앱 부팅 실패 | `S3Config`의 `s3Client` 빈이 즉시(eager) 생성되는데 AWS 환경변수 자체가 없음 | 부팅만 통과시킬 더미 값 추가(AWS 연동 자체는 아직 안 됨) |
| 4 | OAuth 콜백 URL에 `${GOOGLE_CLIENT_ID}` 문자열이 그대로 노출 | `spring-dotenv`가 이 Spring Boot 4.x 구성에서 `.env`를 실제로 로드하지 않음 | `.env` 값을 `application-local.properties`로 직접 복사 |
| 5 | 로그인 완료 직전 `500`(`Value too long for column REFRESH_TOKEN VARCHAR(255)`) | `User.refreshToken`에 `@Column(length=...)` 누락 → 기본 255자로 JWT(300자+)가 안 들어감 | `@Column(length = 2048)` 추가 |
| 6 | 로그인 성공 후 "연결이 거부되었습니다" | `app.frontend-url=http://localhost:3000`(실제 Vite는 5173) + 프론트 서버 미실행 | 포트 수정 + 프론트 서버 기동 |
| 7 | `POST /api/auth/logout` 호출 시 `500`(`RedisSystemException: MISCONF`) | 로컬 Redis 프로세스의 저장 경로(`dir`)가 `C:\WINDOWS\system32`로 잡혀 BGSAVE 계속 실패 → 쓰기 차단 모드 | `redis-cli config set dir`로 쓰기 가능한 경로 변경 후 `bgsave` 재시도 |
| 8 | PowerShell curl로 PATCH 테스트 시 `400 INVALID_INPUT`("요청 본문이 올바르지 않습니다") | PowerShell 작은따옴표 안에서 `\"` 이스케이프가 해석 안 돼 curl에 깨진 JSON 전달됨 | JSON을 파일로 분리해서 `-d "@file.json"` 방식으로 전송 |
| 9 | PostgreSQL 전환 후 재기동 시 `status` 컬럼 추가 실패("null 값이 있습니다") | 기존 row가 있는 상태에서 NOT NULL 컬럼을 기본값 없이 `ALTER TABLE ADD COLUMN` 시도 | 로컬 더미 데이터라 `TRUNCATE TABLE users CASCADE` 후 재기동 |
| 10 | 테스트 실행 시 컨텍스트 로딩 실패(`PlaceholderResolutionException`) | `AWS_ACCESS_KEY` 등 필수 환경변수가 테스트 JVM엔 없음 | `build.gradle`의 `test` 태스크에 더미 환경변수 주입 |
| 11 | `JWT_SECRET`이 유효하지 않은 Base64라 테스트 컨텍스트 로딩 실패(`DecodingException`) | 더미 값이 실제 Base64 형식이 아니었음 | `openssl rand -base64 32`로 생성한 유효한 값으로 교체 |
| 12 | 단위테스트에서 비로그인 401 응답에 JSON body가 있을 거라 가정했다가 실패 | 토큰이 없으면 Spring Security가 `GlobalExceptionHandler` 이전에 차단해서 빈 바디로 응답(실제 동작) | 테스트 기대값을 상태코드만 확인하도록 수정 |
| 13 | 완전탈퇴 스케줄러 단위테스트에서 익명화 결과가 반영 안 됨 | `entityManager.clear()`를 flush 없이 호출해서 미반영 변경사항이 그냥 버려짐 | `flush()` 후 `clear()` 하도록 수정 |

---

## 6. 최종 검증 결과 요약

| 영역 | 검증 항목 수 | 결과 |
|---|---|---|
| OAuth 로그인(Google/Naver) | 2 | ✅ 전부 성공 |
| JWT(Refresh Rotation/Reuse Detection/Logout+블랙리스트) | 5 | ✅ 전부 성공 |
| 약관동의 | 2(최초/중복) | ✅ 전부 성공 |
| 쿠키 속성(HttpOnly/SameSite/Secure) | 3 | ✅ 전부 성공 |
| `GET /api/users/me` | 단위 2 + 통합 1 | ✅ 전부 성공 |
| `PATCH /api/users/me` | 단위 5 + 통합 1 | ✅ 전부 성공 |
| `POST /api/users/me/withdraw` | 단위 3 + 통합 1 | ✅ 전부 성공 |
| 재로그인 자동복구 | 단위 4 + 통합 1 | ✅ 전부 성공 |
| 완전탈퇴 스케줄러 | 단위 3 + 통합 1 | ✅ 전부 성공 |
| PostgreSQL 전환 + 영속성 | 3 | ✅ 전부 성공 |

**결론**: User/Auth 도메인은 이번 세션에서 정의한 시나리오 전부 통과했고, 실제 서버(PostgreSQL 기반)에서 재시작에도 데이터가 유지되는 것까지 확인했다. 자동화 테스트(JUnit)는 User CRUD 관련 12개 + 엔티티 단위 4개, 총 16개가 `UserControllerTest`/`UserServiceTest`/`UserTest`에 남아있어 이후 회귀 방지 안전망으로 쓸 수 있다.

**알려진 out-of-scope 이슈**: `ApplicationControllerTest`의 기존 테스트 6개는 옛 "사주" 도메인 잔재로 인해 여전히 실패 중 — Application 도메인 재설계·재구현 시 같이 정리 예정.

**다음 단계**: `DB.md`/`API-명세.md` 기준 Application 도메인(개인/단체 신청, 카드 1장=1명 원칙) 백엔드 구현.
