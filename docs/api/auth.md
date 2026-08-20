### API 1 / 6 — OAuth 로그인 (시작 + 콜백)

#### ④ Request/Response 설계

**로그인 시작**
```
GET /oauth2/authorization/{provider}
```
| 경로 변수 | 값 | 설명 |
|---|---|---|
| provider | `google` \| `naver` | `.md` User.provider(GOOGLE, NAVER)와 매칭 |

Spring Security OAuth2 표준 라우트라 커스텀 request body 없음 → provider의 로그인 페이지로 리다이렉트.

**콜백 (провider 인증 성공 후 서버가 처리)**

```
Response 302 Found
Set-Cookie: accessToken={JWT}; HttpOnly; Path=/; Max-Age=900
Set-Cookie: refreshToken={JWT}; HttpOnly; Path=/; Max-Age=1209600
Location: {프론트 URL}
```

| 케이스 | 리다이렉트 위치 |
|---|---|
| 기존 회원(재로그인) | `/` |
| 신규 회원(최초 로그인 → 계정 자동 생성) | `/terms` — 약관 동의 전용 화면 |

#### 신규 OAuth 사용자 약관 화면 계약

- 프론트는 `/terms` 라우트를 제공해야 한다.
- `/terms`는 신규 OAuth 사용자의 약관 동의를 위한 별도 화면으로 유지한다.
- 현재 개인정보 처리방침과 이용약관의 최종 내용은 확정되지 않았으므로 본문은 `[TBD]`로 관리한다.
- 확정되지 않은 약관 문구나 버전을 프론트에서 최종 정책으로 간주해 하드코딩하지 않는다.
- 약관 동의 제출은 기존 `POST /api/auth/terms` 계약을 사용하며 Request/Response는 변경하지 않는다.
- 동의 완료 후 프론트는 홈(`/`)으로 이동하고 `GET /api/users/me`를 통해 로그인 사용자 상태를 갱신한다.

#### ⑤ Validation

- `provider`는 `google`/`naver`만 허용, 그 외 값은 404
- OAuth 프로바이더가 반환하는 `email`이 없으면(권한 미동의 등) 로그인 실패 처리

#### ⑥ DB 컬럼과 매핑 검증

최초 로그인 시 `User` row 생성에 쓰이는 값:

| User 컬럼 | 출처 |
|---|---|
| email | OAuth 프로바이더 응답 |
| provider | 요청 경로의 `{provider}` |
| provider_id | OAuth 프로바이더 응답(고유 ID) |
| name | OAuth 프로바이더 응답 |
| role | 기본값 `USER` |
| phone | NULL로 생성 (아래 참고) |
| address | NULL 허용이라 문제 없음 |

#### ⑦ 누락된 필드 확인

**해결됨 (2026-07-29):** `User.phone`을 NOT NULL → NULL 허용으로 정정했습니다. 실제 연락처는 신청 시점마다 `Applicant.phone`에서 받으므로 계정 가입 단계에서 강제로 받을 필요가 없습니다. 다만 신규 OAuth 사용자는 별도 정보 입력 온보딩 없이 약관 동의 전용 화면인 `/terms`로 리다이렉트됩니다.

**API 1 완료.**

---

### API 3 / 6 — 로그아웃 ⚠️ 확인필요 — 버튼은 있으나 mock 세션 clear만 함, 실제 호출 없음

#### ④ Request/Response 설계

```
POST /api/auth/logout
Cookie: accessToken={JWT}; refreshToken={JWT}
```

**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

- 비로그인 상태에서 호출해도 그냥 200(멱등 처리) — 굳이 401 안 줘도 됨

#### ⑥ DB 컬럼과 매핑 검증

`User` 테이블 쓰기 없음 — `accessToken`/`refreshToken` 쿠키를 만료시키고(Max-Age=0), refresh 세션 저장소가 생기면 그쪽도 무효화(세부 구현은 위 TODO 참고).

#### ⑦ 누락된 필드 확인

없음.

**API 3 완료.**

---

### API 4 / 6 — 일반 이메일 회원가입: 인증 코드 요청

#### ④ Request/Response 설계

```
POST /api/auth/signup/email-verification/request
Content-Type: application/json

{ "email": "user@example.com" }
```

**Response `200 OK`**
```json
{ "success": true, "data": { "expiresInSeconds": 600, "resendAfterSeconds": 60 } }
```

인증 코드 자체는 응답에 포함하지 않는다 — 이메일로만 전달된다.

#### ⑤ Validation

- 이메일 형식(RFC 이메일)·길이(255자 이하) 검증, 정규화(trim+소문자) 후 처리
- 이미 가입된 이메일이면 `EMAIL_ALREADY_EXISTS`(409)
- 재전송 대기 60초 이내 재요청 시 `TOO_MANY_REQUESTS`(429)
- 이메일당 1시간 5회, IP당 1시간 20회 초과 시 `TOO_MANY_REQUESTS`(429) — 발송 성공 여부와 무관하게 요청 자체가 횟수에 포함됨
- 이메일 발송(SMTP) 자체가 실패하면 `EMAIL_DELIVERY_FAILED`(503), 이 경우 재전송 대기시간(쿠폴다운)은 시작되지 않음

#### ⑥ DB 컬럼과 매핑 검증

이 API는 `User` row를 생성하지 않는다 — 인증 상태 전부 Redis(`auth:signup:code:*`)에만 저장하고, 실제 계정 생성은 API 6(회원가입)에서만 일어난다.

#### ⑦ 누락된 필드 확인

없음.

**API 4 완료.** 상세 정책·Redis 키 설계는 `docs/collab/TODO.md`의 `SIGNUP-1` 항목 참고.

---

### API 5 / 6 — 일반 이메일 회원가입: 인증 코드 확인

#### ④ Request/Response 설계

```
POST /api/auth/signup/email-verification/confirm
Content-Type: application/json

{ "email": "user@example.com", "code": "482193" }
```

**Response `200 OK`**
```json
{ "success": true, "data": { "signupToken": "32바이트-URL-safe-토큰", "expiresInSeconds": 1800 } }
```

`signupToken`은 30분간 유효한 1회성 토큰이며, 이어지는 API 6(회원가입) 요청에 그대로 실어 보낸다.

#### ⑤ Validation

- 이메일은 API 4와 동일하게 정규화 후 비교
- 코드 검증은 최대 5회까지 허용, 5회 실패 시 해당 인증 challenge 자체가 폐기됨
- 코드 불일치·만료·이미 사용됨·5회 초과 — 이 4가지 경우를 전부 동일한 `INVALID_VERIFICATION_CODE`(400)로 응답한다(공격자가 원인을 구분하거나 남은 시도 횟수를 알 수 없도록)

#### ⑥ DB 컬럼과 매핑 검증

이 API 역시 `User` row를 생성하지 않는다 — 성공 시 Redis(`auth:signup:token:*`)에 발급 토큰의 SHA-256 해시만 저장한다(원본 토큰은 저장하지 않음).

#### ⑦ 누락된 필드 확인

없음.

**API 5 완료.** 상세 정책·Redis 키 설계는 `docs/collab/TODO.md`의 `SIGNUP-2` 항목 참고.

---

### API 6 / 6 — 일반 이메일 회원가입 완료

#### ④ Request/Response 설계

```
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "signupToken": "API 5 응답의 signupToken",
  "password": "8~72자, 복잡도 규칙 없음",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

**Response `201 Created`**
```
Set-Cookie: accessToken={JWT}; HttpOnly; Path=/; Max-Age=900
Set-Cookie: refreshToken={JWT}; HttpOnly; Path=/; Max-Age=1209600
```
```json
{
  "success": true,
  "data": { "id": 1, "name": "홍길동", "email": "user@example.com", "role": "USER", "phone": "010-1234-5678", "address": null }
}
```

OAuth 콜백과 동일하게 응답 본문이 아니라 HttpOnly 쿠키로 토큰을 발급한다. 프론트는 성공 후 `/terms`로 이동한다(약관 동의는 이 API에 포함되지 않음 — 기존 `POST /api/auth/terms` 그대로 재사용, API 1 "신규 OAuth 사용자 약관 화면 계약"과 동일한 흐름).

#### ⑤ Validation

- `email`/`signupToken`/`password`/`name`/`phone` 5개 모두 필수
- `password`: 최소 8자·최대 72자(BCrypt 해시 입력 상한), 복잡도(대소문자/숫자/특수문자) 조합 규칙 없음(2026-08-19 확정)
- `phone`: `^[0-9\-]{9,20}$`(숫자·하이픈, 9~20자) — 프론트 `SignupPage.tsx`가 이미 필수 입력값으로 받고 있어 가입 시점에 함께 저장한다(2026-08-19 확인)
- `signupToken`을 SHA-256 해시해 Redis 조회 → 없거나(만료·오타) 저장된 이메일과 요청 이메일의 정규화 값이 다르면 **이메일 존재 여부를 노출하지 않는 동일한** `INVALID_SIGNUP_TOKEN`(400)으로 응답
- 이메일 중복은 이 시점에 한 번 더 재조회(API 4 시점과 시간차가 있어 그 사이 다른 경로로 가입됐을 수 있음) → `EMAIL_ALREADY_EXISTS`(409), 동시요청은 DB `email UNIQUE` 제약이 최종 방어선
- 비밀번호는 `PasswordEncoder`(BCrypt, `{bcrypt}$2a$10$...` 형식)로 해시 후 저장, 평문은 요청/로그/응답 어디에도 남기지 않음

#### ⑥ DB 컬럼과 매핑 검증

| User 컬럼 | 출처 |
|---|---|
| email | 요청 `email`(정규화) |
| passwordHash | 요청 `password`를 BCrypt로 해시 |
| name | 요청 `name` |
| phone | 요청 `phone` |
| oauthId / oauthProvider | 둘 다 NULL(일반 계정) |
| role | 기본값 `USER` |
| address | NULL(가입 시점엔 받지 않음, 이후 `PATCH /api/users/me`로만 수정 가능) |

#### ⑦ 누락된 필드 확인

없음(`phone`이 회원가입 필수 필드에 포함되는지가 미결이었으나 2026-08-19 프론트 `SignupPage.tsx` 확인 후 포함으로 확정).

**API 6 완료.** 상세 정책·트랜잭션/Redis 삭제 순서 설계는 `docs/collab/TODO.md`의 `AUTH-4` 항목 참고.

---

### API 7 — 계정 복구: 아이디(이메일) 찾기 (2026-08-20 정책 확정, 미구현 — RECOVERY-1)

> 배경: 로그인 아이디 = 이메일이라, "아이디 찾기"는 "가입 시 쓴 이메일을 잊었을 때 다시 확인하는 기능"이다. `phone`은 회원가입 때 형식 검증만 하고 SMS 인증을 한 적이 없어서, 이름+전화번호만으로 마스킹 이메일을 바로 공개하면 그 조합을 아는 제3자가 계정 존재/이메일을 추측할 수 있다. **2026-08-20 결정: 이름+전화번호가 일치해도 마스킹 이메일을 즉시 공개하지 않고, 그 계정의 실제 가입 이메일로 확인 코드를 보낸 뒤 코드를 맞춰야 공개한다** — 기존 `EmailVerificationService`의 HMAC 챌린지/Redis TTL/재전송 쿨다운/횟수 제한 패턴을 그대로 재사용한다.

#### ④ Request/Response 설계 — 2단계(요청 → 확인), API 4/5와 동일한 모양

**7-1. 요청**
```
POST /api/auth/recovery/id/request
Content-Type: application/json

{ "name": "홍길동", "phone": "010-1234-5678" }
```
**Response `200 OK`** (이름·전화번호 일치 여부와 무관하게 항상 같은 모양)
```json
{ "success": true, "data": { "requestId": "UUID", "expiresInSeconds": 600, "resendAfterSeconds": 60 } }
```

**7-2. 확인**
```
POST /api/auth/recovery/id/confirm
Content-Type: application/json

{ "requestId": "7-1 응답의 requestId", "code": "482193" }
```
**Response `200 OK`**
```json
{ "success": true, "data": { "maskedEmail": "ho***@example.com" } }
```

#### ⑤ Validation

- 7-1: `name`/`phone` 형식 검증만 통과하면 항상 200 — **일치하는 계정이 없어도, OAuth 전용 계정이어도 이메일을 보내지 않고 동일한 성공 응답**(계정 존재 비노출). 매칭 대상은 `passwordHash != null`인 로컬 계정만(OAuth 계정은 이메일로 로그인하지 않으므로 이 기능의 대상이 아님).
- `requestId`는 매 요청마다 새로 발급(매칭 성공/실패 무관) — Redis에 `{requestId → HMAC(code)}`로 저장, 매칭 실패 시엔 코드 자체를 만들지도 이메일을 보내지도 않아 이후 7-2는 항상 실패한다(성공 케이스와 동일한 에러로).
- 재전송 대기 60초, 이름당(또는 전화번호당) 1시간 5회 / IP당 1시간 20회 — API 4와 동일한 한도 재사용.
- 7-2: 코드 불일치·만료·이미 사용됨·5회 초과·애초에 매칭 실패(코드 자체가 없음) — **전부 동일한 `INVALID_VERIFICATION_CODE`(400)**로 응답(API 5와 동일 원칙).
- 성공 시 `maskedEmail`은 로컬 앞부분을 일부만 노출(예: `hong@example.com` → `ho***@example.com`, 로컬파트 앞 2글자 + `***` + `@도메인`).

#### ⑥ DB 컬럼과 매핑 검증

- `User` row는 조회만 하고 쓰지 않는다. 매칭 조건: `name`(정확히 일치) + `phone`(정규화 후 일치) + `passwordHash IS NOT NULL`.
- Redis 키 네임스페이스: `auth:recovery:id:code:{requestId}`(챌린지), `auth:recovery:id:cooldown:{phone}`, `auth:recovery:id:count:phone:{phone}`, `auth:recovery:id:count:ip:{ip}` — API 4의 `auth:signup:code:*`와 동일 구조, prefix만 분리.

#### ⑦ 누락된 필드 확인

없음. 새 에러코드 불필요(기존 `TOO_MANY_REQUESTS`/`INVALID_VERIFICATION_CODE` 재사용).

**API 7 정책 확정, 구현 대기.**

---

### API 8 — 계정 복구: 비밀번호 재설정 (2026-08-20 정책 확정, 미구현 — RECOVERY-2)

> 배경: `PATCH /api/users/me/password`(현재 비밀번호 변경)는 로그인 상태 전용이라, 비밀번호를 잊어버린 비로그인 사용자를 위한 별도 흐름이 필요하다. **UX 결정(2026-08-19, 유지)**: 코드 확인과 새 비밀번호 입력을 한 화면에서 한 번에 제출 — 그래서 "코드 검증"과 "비밀번호 저장"을 API 2개로 쪼개지 않고 확인 단계 하나(`{email, code, newPassword}`)로 합친다. **2026-08-20 결정: 요청 이메일이 OAuth 전용 계정(비밀번호 자체가 없음)이거나 존재하지 않는 이메일이어도 메일을 보내지 않고 조용히 같은 성공 응답을 준다** — `UserService.changePassword`가 `PASSWORD_CHANGE_NOT_ALLOWED`로 명시적으로 거절하는 것과 달리, 이 흐름은 비로그인 상태라 계정 존재/유형을 노출하면 안 된다.

#### ④ Request/Response 설계

**8-1. 재설정 코드 요청**
```
POST /api/auth/recovery/password/request
Content-Type: application/json

{ "email": "user@example.com" }
```
**Response `200 OK`** (계정 존재/유형과 무관하게 항상 같은 모양)
```json
{ "success": true, "data": { "expiresInSeconds": 600, "resendAfterSeconds": 60 } }
```

**8-2. 코드 확인 + 새 비밀번호 저장(1회 호출)**
```
POST /api/auth/recovery/password/confirm
Content-Type: application/json

{ "email": "user@example.com", "code": "482193", "newPassword": "새비밀번호8~72자" }
```
**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

- 8-1: 이메일 형식만 검증하면 항상 200. 실제로 코드를 만들고 메일을 보내는 건 `passwordHash != null`인 로컬 계정이 그 이메일로 존재할 때뿐 — 나머지(미가입, OAuth 전용)는 아무 것도 안 하고 동일 응답만 반환.
- 재전송 대기 60초, 이메일당 1시간 5회 / IP당 1시간 20회 — API 4와 동일 한도.
- 8-2: `newPassword`는 API 6과 동일 규칙(8~72자, 복잡도 규칙 없음, BCrypt 해시). 코드 불일치·만료·이미 사용됨·5회 초과·애초에 대상 계정이 아니었음 — **전부 동일한 `INVALID_VERIFICATION_CODE`(400)**.
- 8-2 성공 시: `User.changePasswordHash()` + **해당 유저의 전체 세션 무효화**(`tokenSessionStore.invalidateUserSessions(userId)`, `changePassword`와 동일한 보안 이벤트 로그 패턴) — 재설정 직후 자동 로그인은 시키지 않는다(비로그인 상태에서 시작된 흐름이라 accessToken 자체가 없음). 프론트는 성공 후 로그인 화면으로 안내한다.

#### ⑥ DB 컬럼과 매핑 검증

| 필드 | 처리 |
|---|---|
| passwordHash | `newPassword`를 BCrypt로 재해시 후 갱신 |
| refreshToken / 전체 세션 | `changePassword`와 동일하게 무효화 |

Redis 키 네임스페이스: `auth:recovery:password:code:{normalizedEmail}`, `auth:recovery:password:cooldown:{email}`, `auth:recovery:password:count:email:{email}`, `auth:recovery:password:count:ip:{ip}` — API 4와 동일 구조.

#### ⑦ 누락된 필드 확인

없음. 새 에러코드 불필요(기존 `INVALID_VERIFICATION_CODE`/`TOO_MANY_REQUESTS` 재사용).

**API 8 정책 확정, 구현 대기.** 구현 시 `EmailVerificationService`의 Lua 스크립트(`compare-and-delete-challenge.lua`/`verify-and-increment-code.lua`)를 그대로 재사용 가능(다른 키 prefix로).

---
