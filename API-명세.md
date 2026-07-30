# API 명세서

> `.md`(DB 엔티티 정리)를 기준으로 도메인별로 하나씩 작성합니다.
> 확정된 DB 구조·프론트 구현 기준으로 진행하며, 확정 안 된 사항은 임의로 만들지 않고 TODO로 남깁니다. 이 문서가 가장 최근에 만들어진 API명세입니다.
> **⚠️ 확인필요** 표시 = 이 API가 실제 프론트 화면/호출로 검증된 게 아니라는 뜻(화면이 아예 없거나, mock이라 서버 호출을 안 하는 상태). DB 구조·정책 기준으로 설계는 됐지만, 프론트 구현 후 실제 요청 형태와 다를 수 있으니 그때 다시 대조 필요.

---

## 공통 규칙 (2026-07-29 확정)

**기존 백엔드(`backend/honor-citizen`)의 `ApiResponse<T>`/`GlobalExceptionHandler`/`ErrorCode`를 그대로 재사용합니다.** 인증(JWT)과 마찬가지로 도메인 독립적인 인프라라 새로 안 만들고 가져다 씁니다.

### 성공 응답
```json
{
  "success": true,
  "data": { ... }
}
```
(데이터 없는 성공은 `data: null`)

### 실패 응답

⚠️ **주의 — `error: {code, message}`처럼 중첩된 게 아니라, `errorCode`/`errorMessage`가 최상위에 나란히 있습니다.**
```json
{
  "success": false,
  "data": null,
  "errorCode": "INVALID_INPUT",
  "errorMessage": "입력값 검증에 실패했습니다."
}
```

### 공통 에러 코드 (기존 `ErrorCode.java`에서 그대로 재사용 — 도메인 무관)

| 코드 | HTTP | 메시지 |
|---|---|---|
| `INVALID_INPUT` | 400 | 입력값 검증에 실패했습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 권한이 없습니다. |
| `NOT_FOUND` | 404 | 데이터를 찾을 수 없습니다. |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |

도메인별 에러 코드(예: `DUPLICATE_APPLICATION`, `APPLICATION_NOT_FOUND` 등)는 각 도메인 API 설계(⑤ Validation) 때 그때그때 추가합니다. 기존 `ErrorCode.java`에 이미 있는 이름은 재사용하고, 새로 필요한 것만 신규로 만듭니다.

---

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

### API 1 / 3 — OAuth 로그인 (시작 + 콜백) ⚠️ 확인필요 — 프론트는 이메일/비밀번호 mock 폼, OAuth 미구현

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
| 신규 회원(최초 로그인 → 계정 자동 생성) | `/` — ✅ 별도 온보딩 스텝 불필요(아래 참고) |

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

**해결됨 (2026-07-29):** `User.phone`을 NOT NULL → NULL 허용으로 정정했습니다. 실제 연락처는 신청 시점마다 `Applicant.phone`에서 받으므로, 계정 가입 단계에서 강제로 받을 필요가 없다고 확정됐습니다(`.md` 1절 반영). 그래서 최초 로그인 시 별도 온보딩 스텝 없이 바로 계정이 생성되고 `/`로 리다이렉트됩니다.

**API 1 완료.**

---

### API 2 / 3 — 내 정보 조회 ⚠️ 확인필요 — `AuthContext.tsx`가 mock 세션이라 실제 호출 없음

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
    "role": "USER"
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

`phone`/`address`/`provider`/`provider_id`는 응답에 안 넣음 — 프론트가 실제로 쓰는 건 `user.name`(헤더 표시)과 `role`(`isAdmin` 판별)뿐이라서, `AuthContext.tsx`의 `AuthUser` 타입(`name`, `email`, `role`)과도 맞음.

#### ⑦ 누락된 필드 확인

없음 — 프론트가 필요로 하는 필드(`name`, `email`, `role`)가 전부 `User` 테이블에 이미 있어서 막히는 게 없습니다.

**API 2 완료.**

---

### API 3 / 3 — 로그아웃 ⚠️ 확인필요 — 버튼은 있으나 mock 세션 clear만 함, 실제 호출 없음

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

## User 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `GET /oauth2/authorization/{provider}` (+콜백) | 설계 완료 |
| 2 | `GET /api/users/me` | 설계 완료 |
| 3 | `POST /api/auth/logout` | 설계 완료 |

**남은 TODO (User 도메인 공통):**
- refresh 토큰 rotation/재사용 감지용 세션 저장소를 `.md`에 추가할지 여부 (구현 단계에서 결정해도 되는 사항)

---
User 도메인 완료.

---

## Application 도메인

### ① 도메인의 책임

카드 제작 신청의 생성·조회를 담당한다. 신청 유형(개인/법인단체), 카드 종류/디자인, 발급 방식(모바일/실물), 신청인·수령인 정보, 카드 1장 단위(`ApplicationMember`)를 포괄한다. (`.md` 2절 기준)

> 스코프 참고: 이번 패스는 **사용자가 신청을 만들고 조회하는 흐름**만 다룹니다. 관리자가 상태를 바꾸거나 카드를 발급하는 쪽(`AdminPage.tsx`)은 별도 Admin 도메인으로 분리해서 나중에 다룹니다 — 한 번에 여러 도메인 안 만든다는 규칙 때문입니다.

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `ApplyPage.tsx` | 5단계(유형→정보→파일→확인→완료) 오케스트레이션. 서버 호출 없음 — 전 단계를 `sessionStorage`(`useApplicationDraft.ts`)에 모아뒀다가 마지막에 한 번에 제출하는 구조(중간 저장 API 불필요) |
| `StepType.tsx` | 개인/법인단체 선택 + 사전상담 확인 체크(게이트용, 저장 안 함) |
| `StepInfo.tsx` | 발급유형, 수량, 신청인 정보, (실물일 때만) 수령인 정보 — **`applicantType`에 따른 분기가 이 필드들엔 없음(신청인/수령인 정보는 개인·법인 공통 폼)** |
| `StepFiles.tsx` | 로고/직인/제출ZIP 3개 업로드 — **`applicantType` 상관없이 항상 3개 다 보여줌** |
| `StepComplete.tsx` | 입금자명 입력 + 신청번호 표시 |
| `LookupPage.tsx` | 신청번호/카드번호 + 연락처로 조회 (비로그인도 가능, README에 `POST /api/applications/lookup`으로 이미 언급됨) |

### ③ 필요한 API 목록

1. **개인 신청 생성** — `StepReview`→제출
2. **단체(ZIP) 신청 생성** — 동일 흐름, `applicantType=GROUP`
3. **신청 조회** — `LookupPage.tsx`
4. ⚠️ TODO: **입금자명 등록** — `StepComplete.tsx`가 입금자명을 받는데, 이 값을 저장할 엔티티가 `.md`에 없음(Payment 엔티티 자체가 아직 없음, 이전에 "결제 필요함"으로만 확정되고 실제 테이블은 안 만들어짐). 이번 패스에선 설계 보류, 별도 확인 필요.

### API 1 / 3 — 개인 신청 생성 ⚠️ 확인필요 — `StepInfo.tsx`에 생년월일·국적·출생시각·출생지역·성별·사진 입력란 없음, 서버 호출 자체도 없음

#### ④ Request/Response 설계

```
POST /api/applications
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `photo` | file | `ApplicationMember.photo_path`용 — ⚠️ 프론트에 아직 업로드 입력란 없음(기존 확정 TODO) |

```json
{
  "cardDesignId": 1,
  "issueType": "MOBILE_AND_PHYSICAL",
  "applicant": {
    "name": "홍길동",
    "phone": "010-1234-5678"
  },
  "receiver": {
    "sameAsApplicant": false,
    "name": "김수령",
    "phone": "010-9999-8888",
    "zipCode": "06236",
    "address": "서울특별시 강남구 ...",
    "detailAddress": "101동 202호",
    "deliveryRequest": "부재 시 경비실"
  },
  "member": {
    "birthDate": "1990-05-15"
  }
}
```

- `applicant.email`은 요청에 **포함하지 않음** — `Applicant.email`은 로그인 세션의 `User.email`을 서버가 그대로 채움(신청 이메일=가입 이메일 확정 정책)
- `receiver`는 `issueType=MOBILE`이면 생략
- `logo`/`seal`/제출ZIP은 이 API에 없음 — 개인 신청은 법인 전용 요소라 불필요(2.4절 근거)

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationNumber": "APP-2026-000123",
    "status": "PAYMENT_PENDING",
    "paymentStatus": "WAITING",
    "createdAt": "2026-07-29T10:00:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `cardDesignId`가 없거나 `is_active=false` | `NOT_FOUND` | 404 |
| `issueType=MOBILE_AND_PHYSICAL`인데 `receiver` 없음 | `INVALID_INPUT` | 400 |
| `member.birthDate` 누락, `photo` 파일 누락 | `INVALID_INPUT` | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- `receiver.sameAsApplicant=true`면 나머지 receiver 필드 생략 가능(서버가 `applicant` 값을 복사)
- ✅ 2026-07-29 확인: `quantity`는 요청에 없음 — 개인 신청은 `total_quantity=1` 서버 고정, 클라이언트가 보낼 필요 없음

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| cardDesignId | Application.card_design_id |
| issueType | Application.issue_type |
| applicant.name/phone | Applicant.name/phone |
| (세션) | Applicant.email ← User.email |
| receiver.* | Receiver.* |
| member.birthDate | ApplicationMember.birth_date |
| photo(file) | ApplicationMember.photo_path |
| — | Application.application_type = `INDIVIDUAL` (고정) |
| — | Application.total_quantity = `1` (✅ 확정, `.md` 2.1절 반영) |
| — | Application.total_price = `CardType.price × 1` |
| — | Application.status = `PAYMENT_PENDING`, payment_status = `WAITING` (기본값, ✅ 2026-07-31 정정 — Admin 도메인 상태 흐름 확정 반영) |
| — | Application.application_number = 서버 생성 |
| — | ApplicationMember 1건 자동 생성(개인 원칙) |

#### ⑦ 누락된 필드 확인 / 질문

**해결됨 (2026-07-29):**
1. 개인 신청은 `quantity=1` 고정으로 확정 (위 반영).
2. `StepFiles.tsx`가 신청 유형과 무관하게 로고/직인/제출ZIP을 항상 보여주는 건 **프론트가 아직 이 설계를 못 따라간 상태로 확인** — 개인 신청 시엔 이 3개를 숨기고 생년월일·사진 입력란을 보여주는 쪽으로 프론트를 고쳐야 함. 프론트 미구현 TODO로 기록(`birth_date`/사진 업로드 TODO와 같은 묶음).

**API 1 완료.**

---

### API 2 / 3 — 단체(ZIP) 신청 생성 ⚠️ 확인필요 — 필드 구성은 프론트와 대체로 맞으나 실제 서버 호출 없음(전부 sessionStorage에만 보관)

#### ④ Request/Response 설계

```
POST /api/applications/bulk
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `logo` | file | Application.logo_file_id |
| `seal` | file | Application.seal_file_id |
| `submitFile` | file (ZIP) | Application.submit_file_id — 엑셀(인원별 이름/생년월일/사진 파일명 등) + 사진 묶음 |

```json
{
  "cardDesignId": 1,
  "issueType": "MOBILE_AND_PHYSICAL",
  "applicant": {
    "organizationName": "OO기업",
    "department": "인사팀",
    "name": "홍길동",
    "phone": "010-1234-5678"
  },
  "receiver": {
    "sameAsApplicant": false,
    "organizationName": "OO기업",
    "department": "총무팀",
    "name": "김수령",
    "phone": "010-9999-8888",
    "zipCode": "06236",
    "address": "서울특별시 강남구 ...",
    "detailAddress": "101동 202호",
    "deliveryRequest": "부재 시 경비실"
  }
}
```

- `applicant.email`은 API 1과 동일하게 세션에서 채움
- `member`(개인 신청의 `birthDate` 등)는 이 요청에 없음 — 인원별 정보는 ZIP 안 엑셀에서 옴

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "applicationId": 2,
    "applicationNumber": "APP-2026-000124",
    "status": "PAYMENT_PENDING",
    "paymentStatus": "WAITING",
    "totalQuantity": 42,
    "createdAt": "2026-07-29T10:05:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `submitFile`이 ZIP이 아니거나 손상됨 | `INVALID_ZIP` | 400 |
| ZIP 안에 엑셀이 없음 | `EXCEL_NOT_FOUND` | 400 |
| 엑셀 형식이 안 맞음 | `EXCEL_PARSE_ERROR` | 400 |
| ZIP 파일 크기 초과 | `ZIP_TOO_LARGE` | 413 |
| `cardDesignId` 없음/비활성 | `NOT_FOUND` | 404 |

(위 4개 코드는 기존 `ErrorCode.java`에 이미 있는 걸 그대로 재사용 — Bulk 신청 도메인은 사주 도메인이었을 때도 ZIP+엑셀 처리 방식이 똑같아서 그대로 맞음)

- 엑셀 행 수만큼 `ApplicationMember`가 생성됨 → `total_quantity`는 서버가 엑셀 행 수를 세서 채움(클라이언트가 안 보냄)
- ✅ 2026-07-29 확인: 엑셀 컬럼 = `영문명(english_name)`, `생년월일(birth_date)`, `주소(address)` + 사진 파일명(→ `photo_path` 매칭). 나머지(`name`/`chinese_name`/`name_meaning`/`name_interpretation`/`card_number`/`issue_date`)는 관리자가 신청 이후 별도로 채움

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| cardDesignId | Application.card_design_id |
| issueType | Application.issue_type |
| applicant.* | Applicant.* (organizationName/department 포함) |
| receiver.* | Receiver.* (organizationName/department 포함) |
| logo(file) | UploadFile 생성 → Application.logo_file_id |
| seal(file) | UploadFile 생성 → Application.seal_file_id |
| submitFile(file) | UploadFile 생성 → Application.submit_file_id |
| 엑셀 각 행 | ApplicationMember N건 — 생성 시 채움: `english_name`/`birth_date`/`address`/`photo_path`. `name`/`chinese_name`/`name_meaning`/`name_interpretation`/`card_number`/`issue_date`는 NULL로 시작(관리자가 나중에 채움) |
| — | Application.application_type = `GROUP` (고정) |
| — | Application.total_quantity = 엑셀 행 수 |
| — | Application.total_price = `CardType.price × total_quantity` |

**엑셀 템플릿 컬럼 (확정)**

| 컬럼 | ApplicationMember 필드 |
|---|---|
| ID | (사진 파일명 매칭용 식별자, 저장 안 함) |
| 영문명 | english_name |
| 생년월일 | birth_date |
| 주소 | address |
| (사진 파일명, ZIP 내부 photos/ 하위) | photo_path |

#### ⑦ 누락된 필드 확인

없음 — 위 표로 해결됨.

**API 2 완료.**

---

### API 3 / 3 — 신청 조회 ⚠️ 확인필요 — `LookupPage.tsx`는 mock 데이터 표시뿐, `statusLabels`도 옛 enum 사용 중

#### ④ Request/Response 설계

```
POST /api/applications/lookup
Content-Type: application/json
```
(로그인 불필요 — `LookupPage.tsx`는 비로그인 상태에서도 조회 가능한 공개 페이지)

```json
{
  "method": "application",
  "keyValue": "APP-2026-000123",
  "phone": "010-1234-5678"
}
```
| 필드 | 설명 |
|---|---|
| method | `"application"`(신청번호) \| `"card"`(카드번호) |
| keyValue | 신청번호 또는 카드번호 |
| phone | 신청인 연락처(`Applicant.phone`) — 필수, 단독 조회 불가 |

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationNumber": "APP-2026-000123",
    "applicantNameMasked": "이*하",
    "cardType": "명예한국인증",
    "status": "PHOTO_REJECTED",
    "photoRejectReason": "사진이 흐려서 식별이 어렵습니다. 선명한 사진으로 다시 올려주세요.",
    "submittedAt": "2026-07-15"
  }
}
```
✅ 2026-07-31 추가: `status=PHOTO_REJECTED`일 때 `photoRejectReason` 노출 — 사진 재업로드 흐름(API 4)에서 사용. `applicationId`도 추가(재업로드 API 호출 시 필요).

**Response `404 Not Found`** — 번호+연락처 조합이 안 맞음 (존재 여부를 굳이 구분해서 알려주지 않음 — 개인정보 보호)
```json
{ "success": false, "data": null, "errorCode": "NOT_FOUND", "errorMessage": "데이터를 찾을 수 없습니다." }
```

#### ⑤ Validation

- `method=card`일 때 `keyValue`(`ApplicationMember.card_number`)로 검색 → 그 카드가 속한 `Application`을 찾음 → ✅ 2026-07-29 확인: **`Application` 전체(단체 신청 전체)의 진행상태를 반환.** 그 카드 1장만의 상태가 아님
- `phone`이 `Applicant.phone`과 일치해야 함(9자리 미만이면 `INVALID_INPUT` 400, 기존 프론트 검증과 동일)
- 번호+연락처 조합이 안 맞으면 `NOT_FOUND`(404) — 신청번호는 맞는데 연락처만 틀렸어도 동일하게 404(존재 여부 구분 안 함)

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| applicationId | Application.id |
| applicationNumber | Application.application_number |
| applicantNameMasked | Applicant.name (마스킹 처리). ⚠️ 단체 신청은 이게 **신청 대표자**(예: 인사담당자) 이름이지, 카드번호로 조회한 그 개인(직원 등)의 이름이 아님 — 결과 화면에서 헷갈릴 수 있어 참고로 남김 |
| cardType | Application.card_type_id → CardType.name |
| status | Application.status — ✅ 2026-07-31 정정된 enum(PAYMENT_PENDING/RECEIVED/REVIEWING/PHOTO_REJECTED/PRODUCING/COMPLETED/CANCELLED) 기준. 프론트 `statusLabels`도 이걸로 교체 필요(이미 알려진 프론트 갭) |
| photoRejectReason | Application.photo_reject_reason (status=PHOTO_REJECTED일 때만) |
| submittedAt | Application.created_at |

#### ⑦ 누락된 필드 확인

없음.

**API 3 완료.**

---

### API 4 / 4 — 사진 재업로드 (2026-07-31 추가, 로그인 필수) ⚠️ 확인필요 — 프론트에 해당 화면 자체가 없음(신규)

#### ④ Request/Response 설계

```
PATCH /api/applications/{applicationId}/photo
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `photo` | file | `application_type=INDIVIDUAL`일 때 — 새 사진 1장 |
| `submitFile` | file (ZIP) | `application_type=GROUP`일 때 — 수정된 엑셀+사진 ZIP 전체 재제출 (API 2와 동일한 형식) |

(둘 중 신청 유형에 맞는 파트 1개만 보냄)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "REVIEWING"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.user_id != 로그인한 유저` | `FORBIDDEN` | 403 |
| `Application.status != PHOTO_REJECTED` | `INVALID_STATUS_TRANSITION` | 400 |
| `application_type=INDIVIDUAL`인데 `submitFile`을 보냄(또는 반대) | `INVALID_INPUT` | 400 |
| `submitFile`이 ZIP 형식 오류 | `INVALID_ZIP` | 400 |

✅ 2026-07-31 확정: **신청 본인만 수정 가능** — 단체 신청도 대표 신청인(`Applicant`, `Application.user_id`) 본인만, 구성원 개인이 각자 수정하는 게 아님.

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| photo(file) | ApplicationMember.photo_path 갱신 (INDIVIDUAL) |
| submitFile(file) | UploadFile 재생성 → Application.submit_file_id 갱신, 엑셀 재파싱 → ApplicationMember 재생성/갱신 (GROUP) |
| — | Application.status: `PHOTO_REJECTED` → `REVIEWING` |
| — | Application.photo_reject_reason = NULL (초기화) |

#### ⑦ 누락된 필드 확인

없음.

**API 4 완료.**

---

### API 5 / 5 — 완성된 카드 다운로드 (2026-07-31 추가, 로그인 필수) ⚠️ 확인필요 — 프론트에 해당 화면 자체가 없음(신규)

#### ④ Request/Response 설계

```
GET /api/applications/{applicationId}/cards/download
Cookie: accessToken={JWT}
```

**Response `200 OK` — 개인(INDIVIDUAL)**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationType": "INDIVIDUAL",
    "cardFrontUrl": "https://.../APP-2026-000123-front.png",
    "cardBackUrl": "https://.../APP-2026-000123-back.png",
    "expiresAt": "2026-08-07T10:00:00"
  }
}
```

**Response `200 OK` — 단체(GROUP)**
```json
{
  "success": true,
  "data": {
    "applicationId": 2,
    "applicationType": "GROUP",
    "downloadUrl": "https://.../APP-2026-000124-cards.zip",
    "expiresAt": "2026-08-07T10:00:00"
  }
}
```

✅ 2026-07-31 정정: **개인은 이미지 URL 2장(앞/뒤)을 바로 반환 — ZIP으로 묶을 이유 없음.** 단체(N명분)만 ZIP으로 묶어서 반환. (기존 백엔드의 `CitizenCard` 다운로드 presigned URL 패턴은 유지)

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.user_id != 로그인한 유저` | `FORBIDDEN` | 403 |
| `Application.status != COMPLETED` | `CARD_NOT_READY`(신규 코드) | 400 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| cardFrontUrl/cardBackUrl (개인) | `ApplicationMember.card_front_path`/`card_back_path` (1건뿐이라 바로 매핑) |
| downloadUrl (단체) | `ApplicationMember[].card_front_path`/`card_back_path` 전체를 묶어 ZIP 생성 후 presigned URL 발급 (매번 새로 묶는지, 발급 시 미리 만들어 캐싱하는지는 구현 세부사항) |
| expiresAt | presigned URL 만료 시각(예: 7일) |

#### ⑦ 누락된 필드 확인

없음.

**API 5 완료.**

---

## Application 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `POST /api/applications` (개인 신청 생성) | 설계 완료 |
| 2 | `POST /api/applications/bulk` (단체 신청 생성) | 설계 완료 |
| 3 | `POST /api/applications/lookup` (신청 조회) | 설계 완료 |
| 4 | `PATCH /api/applications/{applicationId}/photo` (사진 재업로드) | 설계 완료 |
| 5 | `GET /api/applications/{applicationId}/cards/download` (카드 다운로드) | 설계 완료 |

**프론트 반영 필요 항목(이번 도메인에서 새로 확인/누적된 것):**
- `StepInfo.tsx`/`StepFiles.tsx`가 `applicantType`에 따라 분기 안 되어 있음 — 개인은 생년월일·국적·출생시각·출생지역·성별·사진 입력, 로고/직인/제출ZIP 숨김 / 법인은 반대
- `LookupPage.tsx`의 `statusLabels`가 옛 status 값(SUBMITTED 등) 사용 중 — 새 enum(`PAYMENT_PENDING/RECEIVED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCING/COMPLETED/CANCELLED`)으로 교체 필요
- 카드 다운로드 버튼/화면 신규 필요 (지금 프론트 어디에도 없음)
- `/lookup`에 `PHOTO_REJECTED` 상태일 때 반려사유 + "로그인 후 재업로드" 버튼 신규 추가 필요
- `LookupPage.tsx`의 카드번호 placeholder(`HN-KR-2609-1188`)가 틀린 형식 — `ROK-XXXXX-XXXX`로 교체 필요

---
Application 도메인 완료.

---

## Payment 도메인

### ① 도메인의 책임

입금 확인을 관리한다. PG/가상계좌 자동화가 아니라 **고정 회사 계좌 무통장입금 + 관리자 수동 확인** 방식 — 사용자가 입금자명을 등록하면, 관리자가 그 이름을 기준으로 통장 내역과 대조해서 확인 처리한다. (`.md` 2.5절 기준)

> 스코프 참고: 관리자가 입금을 "확인 처리"하는 쪽은 Admin 도메인에서 다룹니다. 이번 패스는 **사용자가 입금자명을 등록하는 흐름**만 다룹니다.

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `StepComplete.tsx` | 고정 계좌 정보 표시(은행/계좌번호/예금주, `bankInfo` 하드코딩) + 입금자명 입력란. ⚠️ 입력란이 `value`/`onChange` 없는 비활성 상태 — 프론트 미구현(기존 확정 TODO) |

### ③ 필요한 API 목록

1. **입금자명 등록** — `StepComplete.tsx` 진입 직후(신청 생성 API 완료 후 별도 호출)

### API 1 / 1 — 입금자명 등록/수정 ⚠️ 확인필요 — `StepComplete.tsx`에 입력란은 있으나 `value`/`onChange` 없는 비활성 상태

#### ④ Request/Response 설계

```
PATCH /api/applications/{applicationId}/payment
Cookie: accessToken={JWT}
Content-Type: application/json
```
```json
{ "depositorName": "홍길동" }
```

✅ 2026-07-29 확정: **멱등(Upsert) — `Payment` row가 없으면 생성, 있으면 `depositor_name` 덮어쓰기.** 오타 등으로 확인 전까지는 자유롭게 재호출 가능.

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "applicationId": 1,
    "depositorName": "홍길동",
    "updatedAt": "2026-07-29T10:10:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `depositorName` 누락/공백 | `INVALID_INPUT` | 400 |
| `applicationId`가 존재하지 않음 | `NOT_FOUND` | 404 |
| `applicationId`가 로그인한 유저의 신청이 아님 | `FORBIDDEN` | 403 |
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.payment_status = CONFIRMED`(이미 입금 확인됨) 이후 호출 | `PAYMENT_ALREADY_CONFIRMED`(신규 코드) | 409 |

✅ 2026-07-29 확정: **관리자가 입금 확인(`payment_status=CONFIRMED`) 이후엔 잠금** — 기존 `ShippingAddress.is_locked`(배송 시작 후 배송지 잠금)와 같은 철학. 확인 전까지는 계속 수정 가능.

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| (path) applicationId | Payment.application_id |
| depositorName | Payment.depositor_name |
| — | Payment.confirmed_at = NULL (관리자 확인 전) |
| — | Application.payment_status는 그대로 `WAITING` 유지(등록만으론 안 바뀜). 관리자가 입금 확인하면 `payment_status: WAITING→CONFIRMED`와 `status: PAYMENT_PENDING→RECEIVED`가 **동시에** 전이(Admin 도메인에서 설계) |

#### ⑦ 누락된 필드 확인

없음.

**API 1 완료.**

---

## Payment 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `PATCH /api/applications/{applicationId}/payment` (입금자명 등록/수정) | 설계 완료 |

**프론트 반영 필요 항목:**
- `StepComplete.tsx`의 입금자명 입력란을 `value`/`onChange` 연결해서 실제로 이 API를 호출하도록 구현 필요

**남은 TODO:**
- `PAYMENT_ALREADY_CONFIRMED` 에러코드는 기존 `ErrorCode.java`에 없음 — 신규 추가 필요(구현 단계에서 처리)
- 관리자가 입금을 확인 처리하는 API는 Admin 도메인에서 다룸

---
Payment 도메인 완료.

---

## UploadFile 도메인

> 진행 순서: 남은 도메인 중 의존관계가 제일 상위인 것부터 갑니다. `Application`(logo/seal/submit_file_id), `CardDesign`(preview/template), `Review`(thumbnail_file_id)가 전부 `UploadFile`을 참조하고, `UploadFile`은 아무것도 참조하지 않으므로 이게 다음입니다.

### ① 도메인의 책임

업로드된 파일(사진/엑셀/ZIP/카드이미지)의 메타데이터와 저장 경로를 관리한다. (`.md` 3절 기준)

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `FileUploadBox.tsx` | 파일 선택 시 `URL.createObjectURL()`로 미리보기만 생성. **서버 업로드 호출이 전혀 없음** |
| `types.ts`의 `UploadFileInfo` | `{ name, size, previewUrl }` — ⚠️ **실제 `File` 객체(바이트)를 어디에도 안 들고 있음.** 파일 선택 즉시 메타데이터만 남고 원본 파일 참조는 버려짐 |
| `useApplicationDraft.ts` | sessionStorage 저장 시 파일은 `{name, size}`만 남기고 의도적으로 제외(주석에 명시) |

⚠️ **발견한 것**: 지금 프론트는 파일을 "나중에 한 번에 진짜로 전송"할 수 있는 구조가 아예 아닙니다 — 선택 즉시 미리보기만 만들고 실제 `File` 객체를 버립니다. Application 도메인에서 제가 "로고/직인/제출ZIP을 신청 생성 API에 멀티파트로 같이 보낸다"고 설계했는데, 이게 되려면 **`FileUploadBox`/`ApplicationDraft`가 실제 `File` 객체를 최종 제출 시점까지 들고 있도록 먼저 고쳐야 합니다.** 이것도 `birth_date`/사진입력란과 같은 종류의 프론트 미구현 갭입니다.

### ③ 필요한 API 목록

지금 프론트 화면들을 다 훑어봤는데, **독립적인 "파일 업로드 API"가 필요한 지점이 없습니다.**
- 로고/직인/제출ZIP/개인사진 → 전부 Application 생성 API(개인/단체)에 멀티파트로 임베드되어 처리 (이미 설계 완료)
- `CardDesign`의 preview/template 이미지 → 관리자가 카드 디자인을 등록하는 화면 자체가 프론트에 없음(`DesignPage.tsx`는 읽기 전용, `cards.ts` 정적 데이터를 그대로 보여줄 뿐)
- `Review.thumbnail_file_id` → `/reviews`가 아직 미구현(`StubPage`)

즉 "먼저 업로드해서 fileId를 받고, 그 fileId를 다른 API에 넣는" 방식의 **사전 업로드 전용 엔드포인트가 필요한 화면이 하나도 없습니다.** 파일은 전부 그 파일을 쓰는 도메인의 생성 API에 같이 실려서 그때 `UploadFile` row가 만들어지는 구조입니다(이미 Application API 1/2 설계에 반영됨).

### UploadFile 도메인 정리

| # | API | 상태 |
|---|---|---|
| — | (없음) | 독립 API 불필요 — 각 도메인 생성 API에 임베드 |

**프론트 반영 필요 항목(신규 발견):**
- `FileUploadBox.tsx`/`ApplicationDraft`가 실제 `File` 객체를 최종 제출 시점까지 보관하도록 구조 변경 필요 — 지금은 메타데이터만 남기고 파일 자체를 버림. 이게 안 고쳐지면 Application 생성 API(로고/직인/제출ZIP/사진 멀티파트 전송)가 애초에 동작할 수 없음.

✅ 2026-07-29 확인: 위 결론(독립 API 불필요, `FileUploadBox` 구조 변경 필요) 확정.

---
UploadFile 도메인 완료.

---

## 카드 도메인 (CardType / CardDesign / CardFieldDefinition)

> ✅ 2026-07-29 방향: 사용자 API와 관리자 API를 나눠서 설계.

### ① 도메인의 책임

카드 종류(`CardType`)·디자인(`CardDesign`)·디자인 내 출력 항목 좌표(`CardFieldDefinition`)를 관리한다. 사용자에게는 선택 가능한 디자인 목록/상세를 보여주고, 관리자에게는 이를 등록·수정하는 기능을 제공한다. (`.md` 4절 기준)

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `DesignPage.tsx` | 카테고리별(명예한국인증/명예시민증/학생증/방문증) 카드 디자인 전체 목록 — `cards.ts` **정적 데이터**를 그대로 렌더링, API 호출 없음 |
| `MainDesignsSection.tsx`(홈) | 주요 디자인 미리보기 이미지 + `/apply?designId=xxx` 링크 — 마찬가지로 정적 |
| `ApplyPage.tsx` | URL의 `designId`로 `findCardDesign()`(cards.ts 함수) 호출해 디자인 정보를 가져옴 |
| 관리자 화면 | ⚠️ **없음** — `AdminPage.tsx`는 신청 관리만 하고, 카드 종류/디자인을 등록·수정하는 화면 자체가 없음 |
| `CardFieldDefinition`을 쓰는 화면 | ⚠️ **없음** — 카드 이미지에 이름/사진을 좌표 기반으로 합성하는 기능 자체가 미구현 (8절에 이미 기록된 사항) |

### ③ 필요한 API 목록

✅ 2026-07-29 확정: **사용자용 API 없음.** `DesignPage.tsx`/`MainDesignsSection.tsx`/`ApplyPage.tsx`의 디자인 정보 표시는 계속 프론트 정적 자산(`cards.ts`류)으로 처리.

✅ 2026-07-29 확정(관리자 API 범위):
- **`CardType` → 관리자 CRUD 필요** (가격 등 운영 중 계속 바뀜)
- **`CardDesign` → 관리자 CRUD 필요** (카드 종류마다 여러 디자인, 예: 5개 정도를 운영)
- **`CardFieldDefinition` → API 없음.** DB 테이블이 아니라 config/코드 상수로 관리 (`.md` 4.3절 반영) — 운영 중 안 바뀌는 렌더링 고정값이라 CRUD 자체가 불필요

**관리자 API 목록**
1. 카드 종류 등록/목록조회/수정
2. 카드 디자인 등록/목록조회/수정

### API 1 / 2 — 카드 종류 관리 (관리자) ⚠️ 확인필요 — 관리자 화면 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

**등록**
```
POST /api/admin/card-types
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: application/json
```
```json
{ "name": "명예한국인증", "description": "...", "price": 30000 }
```
**Response `201 Created`**
```json
{ "success": true, "data": { "cardTypeId": 1, "name": "명예한국인증", "price": 30000, "isActive": true } }
```

**목록 조회**
```
GET /api/admin/card-types
```
**Response `200 OK`**
```json
{ "success": true, "data": [ { "cardTypeId": 1, "name": "명예한국인증", "price": 30000, "isActive": true } ] }
```

**수정**
```
PATCH /api/admin/card-types/{cardTypeId}
```
```json
{ "price": 35000, "isActive": true }
```
**Response `200 OK`** — 등록 응답과 동일 형태

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `name` 누락, 또는 이미 존재하는 `name`(UNIQUE) | `INVALID_INPUT` | 400 |
| `cardTypeId` 없음(수정 시) | `NOT_FOUND` | 404 |
| 비로그인 | `UNAUTHORIZED` | 401 |

⚠️ `price` 변경은 **이미 신청된 건에 영향 안 줌** — `Application.total_price`는 신청 시점 스냅샷이라 이 API로 가격을 바꿔도 기존 신청 금액은 그대로 (2.5절 가격 정책과 일치)

#### ⑥ DB 컬럼과 매핑 검증

| Request/Response | CardType 컬럼 |
|---|---|
| name | name |
| description | description |
| price | price |
| isActive | is_active |

#### ⑦ 누락된 필드 확인

없음 — `.md` `CardType` 컬럼과 1:1.

**API 1 완료.**

---

### API 2 / 2 — 카드 디자인 관리 (관리자) ⚠️ 확인필요 — 관리자 화면 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

**등록**
```
POST /api/admin/card-designs
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | `{ "cardTypeId": 1, "name": "명예한국인증 01", "orientation": "LANDSCAPE", "isDefault": false }` |
| `templateFront` | file | 앞면 빈 템플릿 → `CardDesign.template_front_id` (UploadFile 생성) |
| `templateBack` | file | 뒷면("이름풀이") 빈 템플릿 → `CardDesign.template_back_id` (UploadFile 생성) |

✅ 2026-07-31 확정(`시안.zip` 실물 확인): 템플릿 2장(앞/뒤) + `orientation` 필수로 정정. 명예시민증/명예한국인증=LANDSCAPE(83×55mm), 방문증=PORTRAIT(55×83mm) — 시안 자료로 실제 확인됨.

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "cardDesignId": 1,
    "cardTypeId": 1,
    "name": "명예한국인증 01",
    "orientation": "LANDSCAPE",
    "isDefault": false,
    "isActive": true
  }
}
```

**목록 조회**
```
GET /api/admin/card-designs?cardTypeId={optional}
```

**수정**
```
PATCH /api/admin/card-designs/{cardDesignId}
```
(`templateFront`/`templateBack` 파일 교체는 각각 선택 — 안 보내면 기존 템플릿 유지)

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `cardTypeId` 없음 | `NOT_FOUND` | 404 |
| `orientation` 누락/잘못된 값 | `INVALID_INPUT` | 400 |
| `templateFront`/`templateBack` 파일 형식 오류 | `UNSUPPORTED_FILE_TYPE` | 415 |
| 비로그인 | `UNAUTHORIZED` | 401 |

#### ⑥ DB 컬럼과 매핑 검증

| Request | CardDesign 컬럼 |
|---|---|
| cardTypeId | card_type_id |
| name | name |
| orientation | orientation |
| isDefault | is_default |
| templateFront(file) | UploadFile 생성 → template_front_id |
| templateBack(file) | UploadFile 생성 → template_back_id |

#### ⑦ 누락된 필드 확인

없음 — `시안.zip` 확인으로 전부 해결됨.

**API 2 완료.**

---

## 카드 도메인 정리

| # | API | 상태 |
|---|---|---|
| — | 사용자 API | 불필요(정적 프론트 자산 유지) |
| 1 | `POST/GET/PATCH /api/admin/card-types` (카드 종류 관리) | 설계 완료 |
| 2 | `POST/GET/PATCH /api/admin/card-designs` (카드 디자인 관리) | 설계 완료 |
| — | `CardFieldDefinition` | API 없음, config/코드 상수 관리 (필드 구성 `.md` 4.3절에 확정 반영) |

**참고 (시안.zip 확인 결과, 2026-07-31):**
- 카드종류당 디자인 6개 (명예시민증/명예한국인증/방문증 각각 폴더 1~6)
- 학생증 디자인은 추후 전달 예정 — 받으면 동일 구조로 추가
- 뒷면은 한자 유무에 따라 좌표 2벌(`CardFieldDefinition` config에서 처리)

---
카드 도메인 완료.

---

## 게시판(Review/Post) 도메인 — 보류

✅ 2026-07-31 확인: 프론트 화면 자체가 없고(`/reviews`, `/events` 둘 다 `StubPage`), 연관관계도 `User`(작성자) 하나뿐이라 다른 도메인 설계에 영향 없음. 디자인/요구사항이 나오기 전까진 API 설계 보류 — `.md` 5절에 이미 "존재만 인지" 상태로 기록되어 있음.

---

## Admin 도메인 — 흐름 확정 (2026-07-31)

> 참고 화면(대학교 학생증 관리자 UI 스크린샷)은 **확정 UI 아님 — 구조 참고용 목업.** 실제 화면은 아래 흐름을 기준으로 새로 설계.

### 신청 상태 흐름 (`.md` 2.1절에 반영 완료, 2026-07-31 사진검토/작명 분리 재정정)

```
PAYMENT_PENDING(결제전)
   │  ※ 3영업일 내 미입금 시 스케줄러가 자동 CANCELLED
   ▼
RECEIVED(접수완료)   ← 관리자가 입금 확인 (payment_status도 동시에 CONFIRMED)
   ▼
REVIEWING(검토중 — 사진/내용 검토)
   ├─ 반려 → PHOTO_REJECTED(사진반려) → 사용자 재업로드 → REVIEWING(복귀)
   └─ 승인 → NAME_EDITING(작명중)   ← 사진 승인과 작명 완료는 별개 문제라 상태 분리
                │  관리자가 ApplicationMember별 이름/한자/뜻/풀이 입력
                ▼ (전원 작명 완료)
            PRODUCING(카드발급중) → COMPLETED(발급완료)
(모든 단계에서 → CANCELLED 가능)
```

### 확정된 스코프 / 설계 원칙

- **이번 범위는 `MOBILE`(웹/디지털 발급)만.** `MOBILE_AND_PHYSICAL`의 실물 배송(SHIPPING/DELIVERED)은 범위 밖 — 나중에 별도 설계.
- **단체(GROUP) 신청은 구성원별이 아니라 Application 전체 단위로 검토/발급/작명 진행.** 일부만 반려돼도 Application 전체가 `PHOTO_REJECTED`. 전원 통과해야 `NAME_EDITING`, 전원 작명 완료해야 `PRODUCING` 진행. → `ApplicationMember`별 개별 status 불필요.
- **"승인" 액션은 이제 `NAME_EDITING`으로 감 (`PRODUCING`으로 바로 안 감).** 예전에 "승인 시 작명 여부를 검사해서 막을지" 질문드렸던 건 이 상태 분리로 해결됨 — 별도 검사 불필요.
- **3영업일 미입금 자동취소** — 스케줄러/배치 필요 (`StepComplete.tsx` 기존 안내문구와 일치)
- **사진 재업로드** — ✅ 확정, Application 도메인 API 4로 설계 완료(로그인 필수, 본인 확인)

### ✅ 이름 작명 방식 확정 (2026-07-31)

**사주(만세력) 프로그램은 URL 링크아웃일 뿐, 실제 작명은 전부 수동.** 백엔드가 그 서비스를 API로 호출하지 않음.

```
관리자가 화면에서 대상자의 nationality/birth_time/birth_region/gender/birth_date 확인
   ↓
관리자가 사주 사이트(고정 URL) 새 탭에서 열어서 참고
   ↓
(그 사이트에서 직접 확인한 이름 후보를 보고)
   ↓
관리자가 우리 시스템에 이름/한자/뜻/풀이를 직접 타이핑 → 저장 API
```

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `AdminPage.tsx` | `adminMock.ts` 정적 데이터로 목록 테이블만 표시(신청번호/구분/카드종류/신청인/연락처/수량/상태/접수일) + 통계 4개. 서버 호출 없음 |
| 상세/처리 화면 | 코드에 없음 — 이전 스크린샷은 참고 목업, 실제 액션 버튼도 없음 |
| `adminMock.ts`의 `AdminStatus` | 옛 enum이라 교체 필요 |

### ③ 필요한 API 목록

1. 신청 목록 조회
2. 신청 상세 조회
3. 입금 확인 (`PAYMENT_PENDING→RECEIVED`)
4. 사진 검토 — 승인/반려
5. 이름 작명 저장
6. 카드 발급 (`PRODUCING→COMPLETED`)

### API 1 / 6 — 신청 목록 조회 ⚠️ 확인필요 — `adminMock.ts`와 필드 구성은 유사하나 실제 서버 호출 없음, `status`/검색 필터는 신규

#### ④ Request/Response 설계

```
GET /api/admin/applications?status={optional}&cardTypeId={optional}&keyword={optional}&page=0&size=20
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "applicationId": 1,
        "applicationNumber": "APP-2026-000131",
        "applicationType": "GROUP",
        "cardType": "명예한국인증",
        "applicantName": "홍길동",
        "applicantPhone": "010-1234-5678",
        "totalQuantity": 100,
        "status": "PRODUCING",
        "createdAt": "2026-07-18"
      }
    ],
    "totalElements": 6,
    "totalPages": 1,
    "page": 0
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- `keyword`는 신청번호/신청인 이름/연락처 통합 검색으로 추정(기존 `AdminPage.tsx`엔 검색 UI가 없어서 새로 추가하는 개념 — 스크린샷의 검색창 참고)

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| applicationId/applicationNumber/applicationType/status/totalQuantity/createdAt | Application.* |
| cardType | Application.card_type_id → CardType.name |
| applicantName/applicantPhone | Applicant.name/phone |

#### ⑦ 누락된 필드 확인

없음 — 기존 `adminMock.ts` 필드 구성과 거의 그대로 대응됩니다(`status` enum만 교체).

**API 1 완료.**

---

### API 2 / 6 — 신청 상세 조회 ⚠️ 확인필요 — 상세/처리 화면 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

```
GET /api/admin/applications/{applicationId}?memberPage=0&memberSize=20
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationNumber": "APP-2026-000131",
    "applicationType": "GROUP",
    "status": "REVIEWING",
    "paymentStatus": "CONFIRMED",
    "issueType": "MOBILE",
    "cardType": "명예한국인증",
    "cardDesignId": 3,
    "totalQuantity": 100,
    "totalPrice": 3000000,
    "photoRejectReason": null,
    "createdAt": "2026-07-18T10:00:00",
    "applicant": {
      "name": "홍길동",
      "email": "hong@example.com",
      "phone": "010-1234-5678",
      "organizationName": "OO기업",
      "department": "인사팀"
    },
    "receiver": {
      "name": "김수령",
      "phone": "010-9999-8888",
      "address": "서울특별시 강남구 ...",
      "detailAddress": "101동 202호"
    },
    "files": {
      "logoUrl": "https://.../logo.png",
      "sealUrl": "https://.../seal.png",
      "submitFileUrl": "https://.../submit.zip"
    },
    "payment": {
      "depositorName": "홍길동",
      "confirmedAt": null
    },
    "members": {
      "content": [
        {
          "applicationMemberId": 101,
          "name": null,
          "englishName": "Kim Minjun",
          "chineseName": null,
          "nameMeaning": null,
          "nameInterpretation": null,
          "photoUrl": "https://.../photos/1.jpg",
          "nationality": "US",
          "birthDate": "1995-03-12",
          "birthTime": "14:30",
          "birthRegion": "New York",
          "gender": "MALE",
          "address": "...",
          "cardNumber": null,
          "issueDate": null
        }
      ],
      "totalElements": 100,
      "totalPages": 5,
      "page": 0
    }
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- `members`는 단체 신청이 최대 수백 명일 수 있어 페이지네이션(`memberPage`/`memberSize`) — 개인 신청은 항상 1건이라 `totalElements=1`

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| Application 최상위 필드들 | `Application.*` |
| applicant.* | `Applicant.*` |
| receiver.* | `Receiver.*` |
| files.* | `Application.logo_file_id/seal_file_id/submit_file_id` → `UploadFile.file_path` |
| payment.* | `Payment.depositor_name/confirmed_at` |
| members[].* | `ApplicationMember.*` (photo_path → photoUrl로 변환) |

#### ⑦ 누락된 필드 확인

없음 — 지금까지 확정된 컬럼으로 다 채워집니다.

**API 2 완료.**

---

### API 3 / 6 — 입금 확인 ⚠️ 확인필요 — 액션 버튼 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

```
POST /api/admin/applications/{applicationId}/confirm-payment
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음 — `Payment.depositor_name`을 관리자가 이미 상세화면에서 보고 통장 대조 후 확인 버튼만 누르는 액션)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "RECEIVED",
    "paymentStatus": "CONFIRMED"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| `Application.status != PAYMENT_PENDING` | `INVALID_STATUS_TRANSITION` | 400 |
| `Payment` row가 아예 없음(사용자가 입금자명도 안 넣은 상태) | `NOT_FOUND` | 404 |
| 비로그인 | `UNAUTHORIZED` | 401 |

#### ⑥ DB 컬럼과 매핑 검증

| — | 변경되는 컬럼 |
|---|---|
| — | `Application.status`: `PAYMENT_PENDING` → `RECEIVED` |
| — | `Application.payment_status`: `WAITING` → `CONFIRMED` |
| — | `Payment.confirmed_at` = 현재 시각 |

#### ⑦ 누락된 필드 확인

없음.

**API 3 완료.**

---

### API 4 / 6 — 사진 검토 (승인/반려) ⚠️ 확인필요 — 액션 버튼 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

**승인**
```
POST /api/admin/applications/{applicationId}/approve-photo
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음)

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationId": 1, "status": "NAME_EDITING" } }
```

**반려**
```
POST /api/admin/applications/{applicationId}/reject-photo
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: application/json
```
```json
{ "reason": "사진이 흐려서 식별이 어렵습니다. 선명한 사진으로 다시 올려주세요." }
```

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationId": 1, "status": "PHOTO_REJECTED" } }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| `Application.status != REVIEWING` | `INVALID_STATUS_TRANSITION` | 400 |
| 반려 시 `reason` 누락/공백 | `INVALID_INPUT` | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

✅ 승인은 `PRODUCING`이 아니라 `NAME_EDITING`으로 전이(사진검토/작명 분리 확정 반영).

#### ⑥ DB 컬럼과 매핑 검증

| — | 변경되는 컬럼 |
|---|---|
| 승인 | `Application.status`: `REVIEWING` → `NAME_EDITING` |
| 반려 | `Application.status`: `REVIEWING` → `PHOTO_REJECTED`, `Application.photo_reject_reason` = 입력한 사유 |

#### ⑦ 누락된 필드 확인

없음.

**API 4 완료.**

---

### API 5 / 6 — 이름 작명 저장 + 작명 완료 처리 ⚠️ 확인필요 — 작명 화면 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

**구성원별 작명 저장** (인원마다 반복 호출)
```
PATCH /api/admin/application-members/{applicationMemberId}/name
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: application/json
```
```json
{
  "name": "임별하",
  "chineseName": null,
  "nameMeaning": "별처럼 높은 곳에서 세상을 밝게 비추고...",
  "nameInterpretation": "선한 영향력을 널리 행사하며, 맑고 순수한 성품을 유지한 채 꿈을 향해 꿋꿋하게 나아가는 이름."
}
```
(`chineseName`은 선택 — 한자이름 없는 사람은 `null`)

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationMemberId": 101, "name": "임별하" } }
```

**작명 완료 처리** (Application 전체, 전원 작명 확인 후)
```
POST /api/admin/applications/{applicationId}/complete-naming
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음)

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationId": 1, "status": "PRODUCING" } }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationMemberId`/`applicationId` 없음 | `NOT_FOUND` | 404 |
| 부모 `Application.status != NAME_EDITING` | `INVALID_STATUS_TRANSITION` | 400 |
| 작명 저장 시 `name`/`nameMeaning`/`nameInterpretation` 누락 | `INVALID_INPUT` | 400 |
| 작명 완료 처리 시, 구성원 중 `name`이 NULL인 사람이 1명이라도 있음 | `NAMING_NOT_COMPLETE`(신규 코드) | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

✅ 2026-07-31 설계 방향: 저장(PATCH)과 완료 처리(POST)를 분리 — 관리자가 인원별로 저장하면서 검토하다가, **전원 다 채운 걸 확인한 뒤 명시적으로 "작명 완료"를 눌러야** `PRODUCING`으로 넘어감(자동 전이 아님). 입금확인/사진승인과 같은 패턴.

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| name/chineseName/nameMeaning/nameInterpretation | `ApplicationMember.name/chinese_name/name_meaning/name_interpretation` |
| — | 작명완료 처리 시 `Application.status`: `NAME_EDITING` → `PRODUCING` |

#### ⑦ 누락된 필드 확인

없음.

**API 5 완료.**

---

### API 6 / 6 — 카드 발급 ⚠️ 확인필요 — 액션 버튼 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

```
POST /api/admin/applications/{applicationId}/issue-cards
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음 — `PRODUCING` 상태의 신청을 대상으로 전 구성원 카드 이미지를 일괄 생성)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "COMPLETED",
    "issuedCount": 100
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| `Application.status != PRODUCING` | `INVALID_STATUS_TRANSITION` | 400 |
| 구성원 중 `name`이 NULL(작명 안 됨) — 이론상 API 5에서 이미 막혔어야 함 | `NAMING_NOT_COMPLETE` | 400 |
| `CardDesign.template_front_id`/`template_back_id`가 없음(디자인 등록이 안 된 카드종류) | `NOT_FOUND` | 404 |

#### ⑥ DB 컬럼과 매핑 검증

구성원(`ApplicationMember`)별로:

| 처리 | 컬럼 |
|---|---|
| `CardDesign.template_front_id`/`template_back_id` + `CardFieldDefinition`(config) 좌표에 `name`/`english_name`/`photo_path`/`card_number`/`address`/`issue_date`/캐릭터(계산값)/`chinese_name`/`name_meaning`/`name_interpretation` 합성 | 결과 이미지 생성 |
| — | `ApplicationMember.card_number` = 신규 채번, ✅ 2026-07-31 정정: `ROK-XXXXX-XXXX`(5자리-4자리) 형식 — `시안.zip` 실물 카드번호(`ROK-35777-2105` 등) 확인, `HN-KR-YYMM-NNNN`은 틀린 정보였음(취소) |
| — | `ApplicationMember.issue_date` = 오늘 날짜 |
| — | `ApplicationMember.card_front_path`/`card_back_path` = 생성된 이미지 경로 |

전 구성원 처리 끝나면 `Application.status`: `PRODUCING` → `COMPLETED`

#### ⑦ 누락된 필드 확인

형식은 `ROK-XXXXX-XXXX`로 확정. **채번 로직(순차 발급 시퀀스인지, 무작위인지)은 미결정 사항으로 분류** — 시안 이미지의 숫자만 봐서는 날짜 인코딩 패턴이 안 보임(35777/13575/63153/35115/64889/85165 — 규칙성 없어 보임). 순차 발급(예: `MAX(RIGHT(card_number,4))+1`) vs 무작위 생성 중 어느 쪽인지 결정 필요 → 아래 "미결정 사항" 참고.

**API 6 완료.**

---

## Admin 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `GET /api/admin/applications` (신청 목록 조회) | 설계 완료 |
| 2 | `GET /api/admin/applications/{id}` (신청 상세 조회) | 설계 완료 |
| 3 | `POST /api/admin/applications/{id}/confirm-payment` (입금 확인) | 설계 완료 |
| 4 | `POST /api/admin/applications/{id}/approve-photo` \| `reject-photo` (사진 검토) | 설계 완료 |
| 5 | `PATCH /api/admin/application-members/{id}/name` + `POST .../complete-naming` (작명) | 설계 완료 |
| 6 | `POST /api/admin/applications/{id}/issue-cards` (카드 발급) | 설계 완료 |

Admin 도메인 완료 — User/Application/Payment/카드/Admin 5개 도메인, 총 21개 API 설계 끝났습니다.

---

## 미결정 사항 (전체 종합)

| 항목 | 내용 | 관련 위치 |
|---|---|---|
| **카드번호 채번 로직** | `ROK-XXXXX-XXXX` 형식은 확정, 순차 발급 vs 무작위 생성은 미정 | Admin API 6 |
| 게시판(Review/Post) 필드 | 프론트 요구사항 나오기 전까지 설계 보류 | 게시판 도메인 |
| `Receiver.country` | 해외 배송 지원 여부 미정 — 지원 안 하면 컬럼 제거 검토 | `.md` 2.3절 |
| `CardFieldDefinition.font_color` 이후 필드 | 원본 자료 자체가 없음(잘림) | `.md` 4.3절 |
| refresh 토큰 rotation용 세션 저장소 | DB 테이블로 만들지 Redis로 갈지 — 구현 단계 결정 가능 | User 도메인 |
| `MOBILE_AND_PHYSICAL` 배송 흐름(SHIPPING/DELIVERED) | 이번 Admin 설계에서 스코프 아웃, 추후 별도 설계 | Admin 도메인 |
