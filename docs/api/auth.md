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

### API 7 — 계정 복구: 아이디(이메일) 찾기 (2026-08-21 정책 확정, 구현 진행 — RECOVERY-1)

> 로그인 아이디는 이메일이다. 전화번호는 가입 시 형식만 검증하고 SMS 소유 인증을 하지 않으므로, 이름+전화번호가 일치해도 이메일을 즉시 공개하지 않는다. 일반 이메일 계정이 정확히 하나 일치할 때만 가입 이메일로 코드를 보내고 검증 성공 후 마스킹 이메일을 반환한다. HMAC/TTL/Lua 규칙은 가입 인증과 공유하되 공용 `VerificationChallengeStore`로 분리한다.

#### ④ Request/Response 설계 — 2단계(요청 → 확인), API 4/5와 동일한 모양

**7-1. 요청**
```
POST /api/auth/recovery/id/request
Content-Type: application/json

{ "name": "홍길동", "phone": "010-1234-5678" }
```
**Response `200 OK`** (이름·전화번호 일치 여부와 무관하게 항상 같은 모양)
```json
{ "success": true, "data": { "requestId": "32-byte URL-safe random value", "expiresInSeconds": 600, "resendAfterSeconds": 60 } }
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

- 7-1: `name`/`phone` 형식 검증만 통과하면 항상 200 — **일치하는 계정이 없어도, OAuth 전용 계정이어도 이메일을 보내지 않고 동일한 성공 응답**(계정 존재 비노출). 매칭 대상은 `passwordHash != null`인 로컬 계정만이며 OAuth 계정은 아이디 찾기에서 제외한다.
- `requestId`는 매 요청마다 새로 발급(매칭 성공/실패 무관) — Redis에 `{requestId → HMAC(code)}`로 저장, 매칭 실패 시엔 코드 자체를 만들지도 이메일을 보내지도 않아 이후 7-2는 항상 실패한다(성공 케이스와 동일한 에러로).
- 재전송 대기 60초, 정규화 전화번호당 1시간 5회 / IP당 1시간 20회 — API 4와 동일한 한도 재사용.
- 7-2: 코드 불일치·만료·이미 사용됨·5회 초과·애초에 매칭 실패(코드 자체가 없음) — **전부 동일한 `INVALID_VERIFICATION_CODE`(400)**로 응답(API 5와 동일 원칙).
- 성공 시 `maskedEmail`만 반환하고 로그인 방식은 반환하지 않는다. 로컬파트가 3자 이상이면 앞 2자, 2자이면 앞 1자만 남기고, 1자이면 전부 가린다. 도메인은 유지한다.

#### ⑤-1 조회·정규화 정책

- `name`은 앞뒤 공백을 제거한 뒤 정확히 일치시킨다.
- `phone` 입력은 선택적 선행 `+`와 숫자·공백·하이픈을 허용하며 raw 최대 25자다. 공백과 하이픈을 제거하고 선행 `+`는 보존한 뒤 숫자 부분이 9~15자리인지 검증한다.
- 회원가입·회원정보 수정·계정 복구 DTO가 같은 전화번호 규칙을 사용한다. 기존 DB 값은 일괄 변환하지 않고 비교할 때 입력값과 저장값 양쪽을 정규화한다.
- 전화번호에는 UNIQUE 제약을 추가하지 않는다. 같은 전화번호를 사용하는 계정이 있을 수 있으므로 조회 결과가 정확히 1개일 때만 코드를 보낸다.
- 조회 결과가 0개 또는 2개 이상이면 임의 계정을 선택하거나 여러 계정에 발송하지 않는다. 가짜 `requestId`만 반환하고 확인 단계는 일반 코드 오류와 동일하게 실패한다.
- Repository는 `TRIM(name)`과 `passwordHash IS NOT NULL`로 후보 목록을 조회하고 Service가 각 후보의 저장 전화번호를 정규화해 입력과 비교한다. 중복 가능성을 표현하도록 `Optional<User>`를 사용하지 않는다.
- 복수 계정 때문에 찾을 수 없는 경우 프론트는 계정 존재 여부를 단정하지 않고 고객지원 문의를 안내한다.

#### ⑤-2 인증 코드·보안 정책

- 코드는 `SecureRandom`으로 만든 6자리 숫자이며 유효시간은 10분이다.
- cooldown과 횟수 카운터는 계정 조회 전에 적용한다. 실제 계정·미가입·OAuth·복수 계정 모두 동일하게 카운트해 두 번째 요청의 429 여부로 계정 존재를 추측할 수 없게 한다.
- 임계값 확인·증가와 cooldown 선점은 Lua 또는 동등한 Redis 원자 연산으로 처리해 동시 요청의 제한 우회와 중복 메일 발송을 막는다.
- 확인 실패는 요청당 최대 5회다. 오류·만료·재사용·가짜 `requestId`는 모두 `INVALID_VERIFICATION_CODE`로 처리하고 남은 횟수는 노출하지 않는다.
- 코드는 평문이 아니라 서버 비밀키로 HMAC 처리해 저장한다. 검증과 소비는 Redis Lua 스크립트로 원자 처리한다.
- 메일 발송 실패 시 해당 요청의 챌린지만 compare-and-delete로 제거한다. 응답은 계정 미존재와 같은 일반 성공 응답을 유지한다.
- 메일 발송 실패 시에도 이미 증가한 횟수와 cooldown은 유지한다.
- 실제 챌린지는 `userId + codeHmac + failedAttempts`에 결속한다. 이메일·전화번호 원문은 Redis value에도 넣지 않으며 가짜 요청은 챌린지를 저장하지 않는다.
- 확인 시 `userId`로 User를 다시 조회한다. 계정 삭제 또는 일반 계정 조건 불충족은 다른 코드 오류와 같은 `INVALID_VERIFICATION_CODE`로 처리한다.
- 로그에는 원문 이름·전화번호·이메일·코드·`requestId`를 남기지 않는다.

클라이언트 IP는 공용 `ClientIpResolver`로 구한다. 설정된 신뢰 프록시에서 온 요청에만 `X-Forwarded-For`를 인정하고 직접 요청이나 비신뢰 프록시 요청은 `remoteAddr`를 사용한다. Nginx는 전달 헤더를 설정하고 신뢰 프록시 목록은 `app.security.trusted-proxies`로 관리한다.

#### ⑥ DB 컬럼과 매핑 검증

- `User` row는 조회만 하고 쓰지 않는다. 매칭 조건: `name`(trim 후 정확히 일치) + `phone`(정규화 후 일치) + `passwordHash IS NOT NULL`.
- Redis 키에는 원문 개인정보를 넣지 않는다.

```text
auth:recovery:id:challenge:{sha256(requestId)}
auth:recovery:id:cooldown:{sha256(normalizedPhone)}
auth:recovery:id:count:phone:{sha256(normalizedPhone)}
auth:recovery:count:ip:{sha256(clientIp)}
```

#### ⑦ 누락된 필드 확인

없음. 새 에러코드 불필요(기존 `TOO_MANY_REQUESTS`/`INVALID_VERIFICATION_CODE` 재사용). `requestId`는 UUID 노출에 의존하지 않고 32-byte URL-safe 난수로 발급한다.

**API 7 정책 확정.** `AccountRecoveryService`가 흐름을 조정하고 Redis 챌린지는 공용 `VerificationChallengeStore`가 담당한다. 성공해도 로그인이나 토큰 발급은 하지 않는다.

---

### API 8 — 계정 복구: 비밀번호 재설정 (2026-08-21 정책 확정, 구현 진행 — RECOVERY-2)

> `PATCH /api/users/me/password`는 로그인 사용자용이다. 비밀번호를 잊은 사용자는 이메일로 재설정 코드를 받고, **10분 안에 코드와 사용자가 정한 새 비밀번호를 한 번에 제출**한다. 임시 비밀번호는 발급하지 않는다. 요청 이메일이 OAuth 전용 계정이거나 존재하지 않더라도 계정 존재·유형을 노출하지 않도록 같은 성공 응답을 반환한다.

#### ④ Request/Response 설계

**8-1. 재설정 코드 요청**
```
POST /api/auth/recovery/password/request
Content-Type: application/json

{ "email": "user@example.com" }
```
**Response `200 OK`** (계정 존재/유형과 무관하게 항상 같은 모양)
```json
{ "success": true, "data": { "requestId": "32-byte URL-safe random value", "expiresInSeconds": 600, "resendAfterSeconds": 60 } }
```

**8-2. 코드 확인 + 새 비밀번호 저장(1회 호출)**
```
POST /api/auth/recovery/password/confirm
Content-Type: application/json

{ "requestId": "8-1 응답의 requestId", "code": "482193", "newPassword": "새비밀번호8~72자" }
```
**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

- 8-1: 이메일 형식만 검증하면 항상 200. 실제로 코드를 만들고 메일을 보내는 건 `passwordHash != null`인 로컬 계정이 그 이메일로 존재할 때뿐 — 나머지(미가입, OAuth 전용)는 아무 것도 안 하고 동일 응답만 반환.
- `requestId`는 성공·가짜 요청 모두 매번 새 32-byte URL-safe 난수로 반환한다. Redis 챌린지는 성공 대상에만 저장하고 confirm API는 이메일을 다시 받지 않는다.
- 재전송 대기 60초, 이메일당 1시간 5회 / IP당 1시간 20회 — API 4와 동일 한도. cooldown과 횟수 카운터는 계정 조회 전에 적용해 일반 계정·미가입·OAuth 모두 같은 제한 결과를 낸다.
- 8-2: `newPassword`는 API 6과 동일 규칙(8~72자, 복잡도 규칙 없음, BCrypt 해시). `newPasswordConfirm`은 프론트에서만 비교하며 API에는 보내지 않는다.
- 코드 불일치·만료·이미 사용됨·5회 초과·가짜 `requestId`는 **전부 동일한 `INVALID_VERIFICATION_CODE`(400)**로 응답하고 남은 횟수를 노출하지 않는다.
- challenge는 `userId + codeHmac + failedAttempts`에 결속한다. confirm 시 User를 재조회하며 삭제되었거나 `passwordHash == null`이면 같은 `INVALID_VERIFICATION_CODE`로 처리한다.
- 새 비밀번호가 현재 비밀번호와 같아도 허용한다. 기존 로그인 상태 비밀번호 변경과 동일하며 비밀번호 이력 기능은 이번 범위에 도입하지 않는다.
- 성공 시 새 비밀번호 저장, 코드 즉시 폐기, 해당 사용자의 refresh token과 이미 발급된 access token 전체 무효화를 수행한다.
- 재설정 직후 자동 로그인하거나 새 토큰을 발급하지 않는다. 프론트는 성공 안내 후 로그인 화면으로 이동한다.

#### ⑤-1 확정 처리 순서

```text
재설정 코드 발송
→ 10분 안에 requestId + 코드 + 새 비밀번호 입력
→ Redis에서 코드 검증 및 즉시 소비
→ BCrypt 새 비밀번호 저장
→ 전체 refresh/access 세션 무효화
→ DB 트랜잭션 commit
→ 비밀번호 변경 알림 메일 발송(best effort)
→ 로그인 화면으로 이동
```

- 임시 비밀번호는 이메일로 보내지 않는다. 이메일 탈취 시 바로 사용할 수 있는 인증정보를 만들지 않고, 별도 `mustChangePassword` 상태도 추가하지 않는다.
- 코드 소비와 DB 저장은 Redis/DB 간 분산 트랜잭션으로 묶지 않는다. 코드 검증 성공 후 DB 저장이 실패하면 코드를 복구하지 않고 새 코드를 다시 요청하게 한다.
- 비밀번호 저장과 세션 무효화는 `UserService.resetPassword`의 동일 업무 단위로 처리한다. 세션 무효화에 실패하면 비밀번호 변경을 성공 처리하지 않는다.
- Redis 세션 무효화가 성공한 뒤 DB commit이 실패하면 종료된 세션을 복구하지 않는다. 비밀번호는 기존 값으로 rollback되지만 사용자는 새 코드를 요청해야 하며, 보안을 위해 로그아웃 상태를 유지한다.
- 비밀번호 변경 알림 메일은 DB commit 이후 발송한다. 발송 실패는 경고 로그로 남기되 이미 완료된 비밀번호 변경을 rollback하지 않는다.
- IP 제한은 API 7과 같은 `ClientIpResolver`를 사용한다. 신뢰 프록시가 아닌 요청의 `X-Forwarded-For`는 무시한다.

#### ⑥ DB 컬럼과 매핑 검증

| 필드 | 처리 |
|---|---|
| passwordHash | `newPassword`를 BCrypt로 재해시 후 갱신 |
| refreshToken | 사용자 소유 refresh 세션 전체 삭제 |
| accessToken | 사용자별 `revoked-after` 시각을 기록해 기존 토큰 거절 |

Redis 키에는 원문 이메일과 IP를 넣지 않는다.

```text
auth:recovery:password:challenge:{sha256(requestId)}
auth:recovery:password:cooldown:{sha256(normalizedEmail)}
auth:recovery:password:count:email:{sha256(normalizedEmail)}
auth:recovery:count:ip:{sha256(clientIp)}
auth:access:user-revoked-after:{userId}
```

access token 무효화 정책:

- Access JWT 발급 시 표준 `iat`와 별도로 millisecond 정밀도의 `authIssuedAtMillis` 커스텀 claim을 넣는다. 표준 JWT `iat`는 초 단위라 재설정과 새 로그인이 같은 초에 일어나면 새 토큰까지 거절할 수 있으므로 무효화 비교에 사용하지 않는다.
- 비밀번호 변경 시 서버 millisecond 시각을 `auth:access:user-revoked-after:{userId}`에 `revokedAfterMillis`로 기록한다.
- 키 TTL은 access token 최대 수명과 clock skew를 합한 값으로 한다. 현재 access token 15분 기준 기본 TTL은 16분이다.
- JWT 인증 필터는 `authIssuedAtMillis <= revokedAfterMillis`인 기존 토큰을 거절한다.
- revoked-after 키가 없으면 배포 전에 발급되어 커스텀 claim이 없는 토큰도 만료까지 허용한다. 키가 존재하는 사용자의 토큰에 claim이 없으면 재설정 이전 구버전 토큰으로 보고 거절한다.
- 재설정 완료 후 발급된 새 로그인 토큰은 millisecond 시각이 이후이므로 같은 초에 발급돼도 정상 사용한다.
- Access blacklist와 revoked-after Redis 확인은 하나의 세션 검증 책임으로 통합한다. Redis 조회 장애 시 기존 토큰을 허용하지 않는 fail-closed를 적용하고 `AUTH_SESSION_VALIDATION_UNAVAILABLE(503)`을 반환한다.
- 같은 사용자 단위 revoke primitive를 로그인 상태 비밀번호 변경에도 적용한다. 현재 요청 토큰 1개만 blacklist하지 않고 다른 기기의 기존 access token도 모두 거절해야 "전체 세션 무효화" 계약을 만족한다.
- Filter 단계에서 발생한 장애도 공통 API 오류 JSON으로 반환한다. HTML 오류나 빈 503 응답을 반환하지 않는다.

#### ⑦ 누락된 필드 확인

확인 요청 자체는 기존 `INVALID_VERIFICATION_CODE`/`TOO_MANY_REQUESTS`를 재사용한다. 세션 검증 인프라 장애에는 `AUTH_SESSION_VALIDATION_UNAVAILABLE(503)`을 추가하고 인증 실패 코드로 뭉뚱그리지 않는다.

**API 8 정책 확정.** 가입·아이디 찾기·비밀번호 재설정 세 흐름에서 Redis 검증 규칙을 공유하므로 `EmailVerificationService`를 계속 비대하게 만들지 않고 공용 `VerificationChallengeStore`로 책임을 분리한다.

---
