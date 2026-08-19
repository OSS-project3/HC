## User 도메인

### ① 도메인의 책임

회원 계정 정보를 관리한다. OAuth(Google/Naver) 로그인 기반으로 계정을 생성·인증하고, USER/ADMIN 권한을 구분한다. (`.md` 1절 기준)

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `LoginPage.tsx` | 이메일+비밀번호 폼 + 데모 로그인 버튼(일반/관리자) — 전부 mock, 서버 호출 없음 |
| `SignupPage.tsx` | 이름/이메일/비밀번호/비밀번호확인 폼 — mock |
| `AuthContext.tsx` | `user{name,email,role}`를 localStorage에 저장하는 mock 세션. `login`/`loginAsUser`/`loginAsAdmin`/`logout` 제공 |
| `Header.tsx` | 로그인 시 `user.name` + 로그아웃 버튼 표시. `isAdmin`이면 관리자 메뉴 노출 |
| `AdminPage.tsx` | `isAdmin`이 아니면 `/login`으로 리다이렉트 (별도 관리자 로그인 폼 없음, 같은 계정의 role로 구분) |

⚠️ **현재 로그인/회원가입 화면은 이메일+비밀번호 방식인데, `.md` 7절에 "OAuth(Google/Naver) 정책이 맞음"으로 이미 확정되어 있음.** 따라서 이 화면들은 API 설계의 입력 기준으로 쓰지 않고, 확정된 OAuth 정책 기준으로 설계함. (화면을 OAuth 버튼으로 교체하는 건 별도 프론트 작업 — 기존에 이미 TODO로 기록됨)

### ③ 필요한 API 목록

프론트가 실제로 필요로 하는 기능 기준:
1. **OAuth 로그인 시작/콜백** — 로그인 자체
2. **내 정보 조회** — 헤더의 `user.name` 표시, `isAdmin` 판별(관리자 메뉴 노출·`/admin` 접근 제어)
3. **로그아웃** — 헤더/드로어의 로그아웃 버튼

✅ 2026-07-29 확정:
- **세션/토큰 전략**: JWT(accessToken 15분 + refreshToken 14일) + HttpOnly 쿠키. 기존 백엔드(`backend/honor-citizen`)의 `JwtTokenProvider`/`JwtAuthFilter`/`OAuth2SuccessHandler`/`AuthCookieManager`/`TokenSessionStore`를 그대로 재사용(인증은 도메인 독립적이라 사주 도메인용으로 짜여 있어도 그대로 씀).
- **관리자 role 부여**: 별도 API 없음. 일반 유저와 동일하게 OAuth로 가입하고, 필요 시 운영자가 DB에서 `role`을 `ADMIN`으로 직접 변경.

⚠️ 아직 TODO: refresh 토큰 rotation/재사용 감지를 위한 세션 저장소(기존 백엔드의 `refresh_token_sessions` 테이블 같은 것)를 이번 `.md`에도 추가할지는 별도 논의 필요 — 지금은 "JWT+HttpOnly 쿠키를 쓴다"는 방침만 확정, 세부 구현(DB 테이블 vs Redis 등)은 보류.

### API 2 / 3 — 내 정보 조회 (구현 완료)

#### ④ Request/Response 설계

```
GET /api/users/me
Cookie: accessToken={JWT}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "USER",
    "phone": "010-1234-5678",
    "address": "서울특별시 강남구 ..."
  }
}
```

**Response `401 Unauthorized`**
```json
{ "success": false, "data": null, "errorCode": "UNAUTHORIZED", "errorMessage": "인증이 필요합니다." }
```

#### ⑤ Validation

- 인증 안 된 요청은 `UNAUTHORIZED`(401) — 공통 에러코드 재사용, 별도 body validation 없음(GET이라 입력 없음)

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | User 컬럼 |
|---|---|
| id | id |
| name | name |
| email | email |
| role | role |
| phone | phone (NULL 가능) |
| address | address (NULL 가능) |

`provider`/`provider_id`는 응답에 안 넣음(로그인 식별용 내부 값, 프론트에서 쓸 일 없음). `phone`/`address`는 ⚠️ 2026-07-31 추가 — 내 정보 수정(API 5) 화면에서 기존 값을 미리 채워 보여줘야 해서 Read 응답에도 포함시킴(수정 API 설계하며 정합성 점검 중 반영).

#### ⑦ 누락된 필드 확인

없음 — 프론트가 필요로 하는 필드(`name`, `email`, `role`)와, 신규로 필요해진 `phone`/`address`가 전부 `User` 테이블에 이미 있어서 막히는 게 없습니다.

**API 2 완료.**

---

### API 4 / 4 — 회원탈퇴 (2026-07-31 추가 → 2026-08-19 소프트 삭제 폐지, 즉시 하드 삭제로 정책 변경. 로그인 필수) ⚠️ 확인필요 — 프론트에 탈퇴 버튼/화면 자체가 없음(신규)

> ⚠️ 2026-08-19 정책 변경: 아래 ⑤~⑥은 "소프트 탈퇴(7일 유예+익명화)" 시절 내용이 섞여 있었다. **확정된 새 정책의 source of truth는 `docs/collab/user.md`("회원정보·개인정보 보유·탈퇴·파기 정책")** — `arch.md` §4.1 "탈퇴 정책" 표와 `backend/FRONTEND_API_REQUIREMENTS.md` §3 "회원탈퇴 정책"은 그 요약이다. 요약: **탈퇴 즉시(배치로 미루지 않고 탈퇴 처리 과정에서 바로) `User` row 하드 삭제, 유예기간·자동복구 없음, `RefreshTokenSession`도 revoke가 아니라 즉시 하드 삭제, `ApplicationDailyLimit`도 삭제, `Application`/`Inquiry`/`Review`/결제 이력/`Board`/감사로그는 삭제하지 않고 각자의 보존정책(상품 수령일+6개월, 접수일+6개월 등)을 따름.** 구현 시 아래 ⑤~⑥을 이 정책에 맞춰 다시 작성해야 한다(현재는 이전 정책 그대로 남아있는 참고용 초안).

#### ④ Request/Response 설계

```
POST /api/users/me/withdraw
Cookie: accessToken={JWT}
```
(body 없음)

**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| 이미 `status=WITHDRAWN`인 계정이 다시 호출 | `ALREADY_WITHDRAWN`(신규 코드) | 409 |

- 진행 중인 신청(`Application`)이 있어도 탈퇴 자체는 항상 허용 — `.md` User 정책 참고(이력은 `user_id` FK로 보존, 완전탈퇴 때도 삭제 안 됨)

#### ⑥ DB 컬럼과 매핑 검증

| — | 변경되는 컬럼 |
|---|---|
| — | User.status: `ACTIVE` → `WITHDRAWN` |
| — | User.withdrawal_requested_at = 현재 시각 |
| — | 세션 무효화: `TokenSessionStore.invalidateUserSessions()` + 현재 accessToken 블랙리스트 등록 (로그아웃과 동일 처리) |

#### ⑦ 누락된 필드 확인

없음.

**API 4 완료.**

---

### API 5 / 5 — 내 정보 수정 (2026-07-31 추가, 로그인 필수) ⚠️ 확인필요 — 프론트에 정보수정 화면 자체가 없음(신규)

> ✅ 2026-08-08 정정: **`address`는 이 API의 수정 대상에서 제외한다.** OAuth 기반 계정이라 `email` 수정이 애초에 불가능한 것과 별개로, 사람이 "수정 가능한 필드는 이름·전화번호뿐"으로 범위를 확정했다. 아래 Request 예시·Validation 표·DB 컬럼 매핑의 `address` 관련 부분은 이 정정으로 대체됨(취소선 대신 이 노트로 표시, 과거 기록은 남겨둠).

#### ④ Request/Response 설계

```
PATCH /api/users/me
Cookie: accessToken={JWT}
Content-Type: application/json
```
```json
{
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```
(두 필드 다 선택 — 보낸 필드만 갱신하는 partial update. 단, 최소 1개는 있어야 함. `address`를 보내도 무시된다)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "role": "USER",
    "phone": "010-1234-5678",
    "address": "서울특별시 강남구 ..."
  }
}
```
(API 2와 동일한 응답 형태 — 수정 직후 최신 상태를 그대로 돌려줌. `address`는 응답에는 계속 포함되지만 이 API로 값을 바꿀 수는 없다)

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `name`/`phone` 전부 없음(빈 요청) | `INVALID_INPUT` | 400 |
| `name`이 빈 문자열로 옴 | `INVALID_INPUT` | 400 |
| `phone` 형식 오류(숫자/하이픈 외 문자 등) | `INVALID_INPUT` | 400 |

- `email`은 이 API로 수정 불가 — OAuth 계정 식별값이자 `Applicant.email`과 일치해야 하는 제약(`.md` 2.2절)이라, 바꾸려면 OAuth 재연동이 필요한 별개 문제. 이번 범위에서 다루지 않음.
- `address`도 이 API로 수정 불가(2026-08-08 확정). ~~`phone`/`address`는 `NULL` 허용 컬럼이라, 빈 문자열이 아니라 값을 아예 지우고 싶은 경우(예: `null` 전송)까지 지원할지는 구현 단계 확인 필요~~ → `address`가 수정 대상에서 빠지면서 이 TODO는 소멸(더 이상 지울 대상 자체가 없음). `phone`은 여전히 `@Pattern` 형식 검증이 빈 문자열을 항상 거부하므로 "지우기"는 지원하지 않는다.

#### ⑥ DB 컬럼과 매핑 검증

| Request | User 컬럼 |
|---|---|
| name | name |
| phone | phone |
| — | updated_at 자동 갱신 |

(`address` 컬럼은 계속 존재하고 API 2 응답에도 나오지만, API 5 Request로는 매핑되지 않는다)

#### ⑦ 누락된 필드 확인

없음.

**API 5 완료.**

---

### ~~회원탈퇴 관련 로직 변경~~ (2026-08-19 정책 변경으로 전면 대체, API 아님 — 기존 인프라 수정)

> ⚠️ 아래 1)·2)는 실제로 구현까지 됐던 "소프트 탈퇴" 정책(자동복구+7일 후 익명화 스케줄러)의 설계 기록이다. 2026-08-19에 이 정책 자체를 폐지하고 즉시 하드 삭제로 바꾸기로 확정했다 — 아래는 더 이상 목표 상태가 아니라 "이전엔 이렇게 설계·구현했었다"는 이력으로만 남긴다. 실제 구현 변경 시 체크리스트: `User.anonymize()`/`isRestorable()`/`restore()` 제거(또는 재설계), `OAuth2SuccessHandler`의 자동복구 분기 제거, `UserWithdrawalScheduler`/`UserService.anonymizeExpiredWithdrawnUsers()`를 "하드 삭제"로 교체, `withdraw()`에서 `RefreshTokenSession`/`ApplicationDailyLimit` 하드 삭제 추가.

**1) (폐지) 재로그인 시 자동 복구 (`OAuth2SuccessHandler` 수정)**
기존 회원 재로그인 시(`existsByOauthIdAndOauthProvider`로 판별), 해당 User의 `status=WITHDRAWN`이고 `anonymized_at IS NULL`이면(= 완전탈퇴 전, 유예기간 내) 로그인 처리 직전에 자동 복구:
```
status: WITHDRAWN → ACTIVE
withdrawal_requested_at = NULL
```
그 다음은 기존 로그인 흐름 그대로 진행(신규회원 아님, `/`로 리다이렉트).

**2) (폐지) 완전탈퇴 배치 (신규 스케줄러, API 아님)**
- 대상: `status=WITHDRAWN` AND `anonymized_at IS NULL` AND `withdrawal_requested_at` ≤ NOW - 7일
- 처리: `email`/`name`/`oauth_id`/`oauth_provider`(+`phone`/`address`가 있다면) 스크램블 값으로 치환, `anonymized_at = NOW`
- 실행 주기: 일 1회 배치로 충분(신청일로부터 3일 이내 미입금 자동취소 스케줄러와 같은 성격 — 둘 다 아직 구현 안 됨, 별도 작업)

---

## User 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `GET /oauth2/authorization/{provider}` (+콜백) | 설계 완료 |
| 2 | `GET /api/users/me` | 구현 완료 |
| 3 | `POST /api/auth/logout` | 설계 완료 |
| 4 | `POST /api/users/me/withdraw` (회원탈퇴) | 구현 완료(소프트 삭제 방식) — ⚠️ 2026-08-19 정책 변경으로 하드 삭제 재구현 필요(위 경고 박스 참고) |
| 5 | `PATCH /api/users/me` (내 정보 수정) | 설계 완료 |
| — | ~~재로그인 자동복구(`OAuth2SuccessHandler`) + 완전탈퇴 스케줄러~~ | 구현은 됐었으나 2026-08-19 정책 폐지로 제거 대상(위 경고 박스 참고) |

**남은 TODO (User 도메인 공통):**
- refresh 토큰 rotation/재사용 감지용 세션 저장소를 `.md`에 추가할지 여부 (구현 단계에서 결정해도 되는 사항)
- ~~`ALREADY_WITHDRAWN` 에러코드는 기존 `ErrorCode.java`에 없음~~ (이미 추가·사용 중, 이 TODO는 stale)
- **회원탈퇴 하드 삭제 재구현 필요(2026-08-19 신규, 최우선)**: `arch.md` §4.1 "탈퇴 정책" 표대로 `User.anonymize()`/`isRestorable()`/`restore()` 제거, `OAuth2SuccessHandler` 자동복구 분기 제거, `UserWithdrawalScheduler` 제거, `withdraw()`에서 `User`/`RefreshTokenSession`/`ApplicationDailyLimit` 하드 삭제로 교체. 관련 테스트(`UserServiceTest`/`UserServiceLoginTest`/`UserTest`/`UserControllerTest`/`ReviewEligibilityServiceTest` 등) 재작성 필요.
- ~~정보수정(API 5)에서 `phone`/`address`를 `null`로 지우는 것까지 지원할지~~ (2026-08-08 해결 — `address`는 수정 대상에서 완전히 제외됨, `CHANGELOG.md` 2026-08-08 "UserUpdateRequest — address를 수정 대상에서 제외" 항목 참고)
- ⚪ 2026-08-09 추가(나중 정책사항, 미착수) — **개인정보처리방침 버전 관리**: 회원가입(OAuth) 시 동의받는 개인정보처리방침 문구(사단법인 한글과 세종, 10개 조항 — 처리 목적/수집 항목/제3자 제공/위탁/파기/정보주체 권리/안전성 확보조치/자동수집장치/보호책임자/구제방법/변경 등)를 지금 확정할지 여부. 동의 체크 자체는 기존 `TermsAgreeRequest.privacyAgreed`(`User.privacyAgreed`)로 이미 충분하며 이번 방침 문구 추가만으로는 백엔드 로직/DB 구조 변경이 필요 없음(방침 본문은 프론트 정적 콘텐츠로 처리 가능). 단, **방침 내용을 나중에 개정할 계획이 있다면** 사용자가 "몇 번째 버전에 동의했는지" 추적할 방법이 현재 없음 — `User.termsAgreedAt`은 전체 동의 항목 공통 타임스탬프 1개뿐이고 버전 컬럼이 없음. 개정 계획이 생기면 `User`에 정책 버전 컬럼(또는 별도 동의 이력 테이블) 추가 필요 — 지금은 미결정.

✅ 2026-07-31 확인/추가:
- **`POST /api/auth/terms`(약관동의) 유지 확정.** 기존 코드에 이미 구현되어 있고(신규가입 시 `/terms`로 리다이렉트), 새 설계에서도 계속 씀 — `DB.md` User 엔티티에 `terms_agreed` 등 필드 반영함
- **`GET /api/users/me`(API 2)는 구현 완료.** 실제 엔드포인트는 `UserController#getMe`에서 제공한다.

---
User 도메인 완료.

---
