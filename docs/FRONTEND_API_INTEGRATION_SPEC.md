# 프론트엔드 API 연동 준비도 및 구현 명세

작성 기준: 2026-08-18

검증 대상: `frontend/src`, `backend/honor-citizen` 현재 워킹 트리

요구사항 출발점: `backend/FRONTEND_API_REQUIREMENTS.md`

## 0. 문서 목적과 개발 경위

이 프로젝트는 다음 순서로 개발됐다.

1. 프론트엔드가 목데이터를 사용해 사용자 화면과 업무 흐름을 먼저 구성했다.
2. 해당 화면에서 필요한 데이터, DTO, Validation, API를 도출해 백엔드를 구현했다.
3. 프론트엔드는 확정된 백엔드 계약을 기준으로 목데이터를 실제 API 호출로 교체한다.

따라서 `localStorage`, 하드코딩, 목데이터 사용 또는 아직 API를 호출하지 않는다는 사실 자체는 오류로 판정하지 않는다. 이 문서가 판정하는 것은 다음 두 가지다.

- 기존 화면의 데이터와 기능을 실제로 제공할 만큼 백엔드가 준비됐는가.
- 준비된 백엔드 API를 프론트가 어떤 요청·응답 계약으로 연결해야 하는가.

세부 도메인 계약이 이 문서와 충돌하면 각 도메인 Source of Truth가 우선한다.

- 공통 응답·인증: `docs/api/common.md`
- Application: `docs/specs/application/api.md`
- Review: `docs/specs/review/api.md`
- Board: `docs/specs/board/api.md`
- Event: `docs/specs/events/api.md`
- User/Auth: `docs/api/user.md`, `docs/api/auth.md`

> 현재 워킹 트리에는 `MyApplicationController`와 관련 DTO가 신규 파일로 존재하지만 아직 커밋되지 않았다. 이 문서의 `READY (미커밋)` 표시는 현재 파일 기준으로는 연동 가능하지만 원격 `main`을 받는 프론트 개발자는 아직 사용할 수 없다는 뜻이다.

### 프론트엔드 담당자 전달사항

이 문서를 프론트 API 연동 작업의 기준으로 사용한다. 목데이터와 localStorage는 API 연결이 완료된 화면부터 제거하며, 작성 중 신청 draft·언어·모달 등 UI 전용 상태는 유지할 수 있다.

우선 작업 범위:

1. OAuth 로그인, 약관, 회원정보 조회
2. 개인·단체 신청 생성과 Validation `errors[]` 처리
3. 신청 취소와 개인·단체 사진 재업로드
4. 후기 CRUD — 이미지 1개 기준
5. FAQ
6. 공지사항 — 검색 제외
7. 행사 — `company`, `logoUrl` 제외

마이페이지 신청 목록·상세는 관련 백엔드 변경이 커밋·푸시된 것을 확인한 다음 연결한다.

현재 작업 보류 범위:

- 일반 이메일 회원가입·로그인·계정 복구
- 전화번호+이메일만 사용하는 신청 조회
- 1:1 문의
- 관리자 신청관리·통계
- address 수정
- 학생증 `schoolName`
- 다중 이미지 후기

공통 구현 원칙:

- 인증 요청에 `credentials: include` 적용
- 401 발생 시 refresh 후 원 요청 재시도는 최대 1회
- refresh 실패 시 사용자 상태 초기화
- `ApiEnvelope.errors[]`를 버리지 않고 필드·Excel 행 오류로 표시
- 페이지 번호는 API의 0-based 규칙 적용
- 화면 문자열과 API enum 변환은 공통 mapper에서 처리
- API 실패를 목데이터 성공으로 대체하는 fallback은 해당 화면 연동 완료 시 제거

---

## 1. 백엔드 구현 완결성 검증

### 1.1 판정 기준

| 판정 | 의미 |
|---|---|
| `READY` | 현재 화면 요구사항을 실제 API로 교체할 수 있다. 프론트 매핑 작업만 남았다. |
| `PARTIAL` | API는 있으나 화면 필드, 조회 조건, 파일 조합 또는 상태 처리 일부가 빠져 있다. |
| `BLOCKED` | 화면에 필요한 백엔드 API 또는 도메인 자체가 없다. |
| `STATIC` | 서버 데이터로 전환하지 않기로 정한 정적 UI 데이터다. |

### 1.2 화면별 준비도

| 기능/화면 | 화면이 요구하는 데이터·동작 | 백엔드 검증 결과 | 판정 | 백엔드에서 남은 작업 |
|---|---|---|---|---|
| Google/Naver 로그인 | OAuth 진입, 최초 사용자 생성, JWT 쿠키, 기존 사용자 로그인 | OAuth2 성공 처리, User 생성, access/refresh 쿠키 발급 구현 | `READY` | 없음 |
| 신규 OAuth 약관 | 개인정보·이미지·배송 약관 동의 | `POST /api/auth/terms` 구현 | `READY` | 없음. `/terms` 라우트는 프론트 작업 |
| 일반 이메일 회원가입(이메일 인증 포함) | 이메일 인증 코드 요청·확인, 회원가입(이메일/비밀번호/이름/전화번호) | `POST /api/auth/signup/email-verification/{request,confirm}`, `POST /api/auth/signup` 전부 구현(2026-08-19) | `READY` | 없음. 상세 계약은 §3.13, 최신 소스는 `docs/api/auth.md` API 4~6 |
| 일반 이메일 로그인·중복 확인·계정 복구 | 로그인, 이메일 중복 확인, 아이디/비밀번호 찾기 | `login`/`email/check`는 이전부터 구현됨(AUTH-3/5). `recovery/*`(아이디 찾기·비밀번호 재설정) 4개 API도 2026-08-21 구현·테스트 완료(커밋 `db002a7`/`2d49acd`) | `READY` | 백엔드 없음. 프론트 미연동 — `AccountRecoveryPage`에 확인 화면(코드 입력·마스킹이메일 표시, 코드+새비밀번호 입력) 신규 필요. 최신 계약은 `docs/api/auth.md` API 7·8 |
| 회원정보 조회 | id, name, email, role, phone, address | `GET /api/users/me` 응답에 전부 존재 | `READY` | 없음 |
| 회원정보 수정 | name, phone, address | `PATCH /api/users/me`는 name, phone만 처리 | `PARTIAL` | 화면에서 address 수정이 확정 요구라면 Request/Entity 수정 경로 추가 |
| 회원 탈퇴 | 소프트 탈퇴, 세션 무효화, 유예기간 후 처리 | 탈퇴 API와 7일 후 익명화 스케줄러 구현 | `PARTIAL` | “실제 삭제”와 “익명화 후 row 보존” 중 최종 정책 일치 필요 |
| 개인 일반카드 신청 | 신청자, 수령인, 얼굴사진, 카드 종류·발급 유형 | 생성·검증·저장·응답 구현 | `READY` | 사용자가 입력한 Applicant email을 프론트가 요청에 포함해야 함 |
| 개인 학생증 신청 | 학교 구분, 방향, 학교명, 학번·학과, 로고, 선택 직인 | `orientation`, `schoolType`, 대학교 학번·학과, 로고·직인 검증 구현 | `PARTIAL` | 화면의 `schoolName`을 저장·조회할 필드가 없음. 실제 발급 정보라면 DTO/Entity 추가 |
| 단체 신청 | 단체/담당자, 방향·학교 구분, Excel+사진 ZIP, 로고·직인 | 생성, ZIP 파싱, 사진 번호 매칭, 전체 실패 오류 구현 | `PARTIAL` | 250 MiB ZIP 정책과 전역 multipart 10MB 설정 충돌. 인원·해제 크기·파일 수 상한 검증도 완료 필요 |
| 신청 수량 | 개인 1명, 단체 실제 유효 행 수 | 개인 1, 단체 parser 결과 행 수로 서버 계산 | `READY` | 프론트의 임의 `quantity` 입력은 API 필드로 사용하지 않음 |
| 신청 조회 | 신청번호+phone+email 또는 카드번호 | 공개 lookup API 구현 | `PARTIAL` | 화면의 “phone+email 조회”를 지원하지 않음. 현재 application 방식은 `keyValue=applicationNumber` 필수 |
| 내 신청 목록 | 로그인 사용자 신청 목록·상태·수량 | 현재 워킹 트리에 `GET /api/my/applications` 구현 | `READY (미커밋)` | 커밋·푸시 후 사용 가능 |
| 내 신청 상세 | 결제, 취소, 반려, 수령인, 카드 준비, 멤버 수 | 현재 워킹 트리에 `GET /api/my/applications/{id}` 구현 | `READY (미커밋)` | 단체 구성원별 상세 화면이 필요하면 별도 members API 필요 |
| 사용자 신청 취소 | 취소 가능 상태, 환불 필요 여부 | 소유권·멱등성·상태 검증 포함 취소 API 구현 | `READY` | 없음 |
| 사진 재업로드 | 개인 얼굴사진 또는 단체 수정 ZIP | 개인 `photo`, 단체 `submitFile` 분기 구현 | `READY` | 없음. 프론트가 ApplicationType에 따라 part를 다르게 전송해야 함 |
| 모바일 카드 다운로드 | 개인 앞/뒷면, 단체 ZIP | 로그인 사용자 본인 다운로드 구현 | `PARTIAL` | 현재는 `COMPLETED`만 허용. 확정 정책인 `cardReadyAt` 기준 다운로드와 불일치 |
| 후기 목록·상세·CRUD | 필터, 검색, 작성·수정·삭제, 이미지, 권한 | 공개 조회·사용자 CRUD·내 후기 목록 구현 | `PARTIAL` | 내 후기 목록은 준비됨. 화면 다중 이미지와 서버 단일 이미지 정책은 여전히 불일치 |
| 공지사항 | 목록·상세·검색·첨부 다운로드, 관리자 CRUD | Board NOTICE 조회/CRUD와 실제 첨부 구현 | `PARTIAL` | 화면 검색을 서버에서 수행할 keyword/searchType 계약 없음 |
| FAQ | 질문·답변 목록, 관리자 CRUD | Board FAQ 조회/CRUD 구현 | `READY` | 없음 |
| 1:1 문의 | 문의 작성, 내 목록·상세, 관리자 답변 | Inquiry 도메인 없음 | `BLOCKED` | Inquiry Entity/Repository/Service/API 전체 구현 |
| 행사 목록·상세 | BOOTH/COLLABORATION, 날짜, 장소, 주최, 회사·로고·이미지 | Event 공개 조회·관리자 CRUD 구현. 2026-08-21: `companyName`/`logoImageUrl`(COLLABORATION 전용) 응답 필드 추가, 관리자 전체목록·상세(`GET /api/admin/events`, `/{id}`) 신규, 수정 API에 로고 유지·교체·삭제 + 갤러리(`keepImageIds`) 편집 추가 | `READY` | 백엔드 없음. 프론트 미연동 — `EventAdminPanel`에 회사명·로고 업로드 UI, 갤러리 유지/추가/삭제 UI 연결 필요. 최신 계약은 `docs/specs/events/api.md` API 3·4·6·7 |
| 관리자 신청관리 | 목록·상세·검색, 결제 안내·확인, 반려, 상태 전이, 통계 | 목록·상세 조회(`GET /api/admin/applications`, `/{id}`)는 2026-08-21 구현 완료(커밋 `6575d09`, 상태 단일 필터만·복합검색 없음). 결제 안내·확인은 내부 Service만 있고 HTTP 미연결. 반려·상태전이·카드발급·배송추적·통계는 여전히 없음 | `PARTIAL` | `/api/admin/applications/{id}/status` 등 명령 API, `/api/admin/dashboard/stats` 구현 필요. 상세는 `docs/BACKEND_API_GAPS.md` P0-3 |
| 카드 종류·디자인 화면 | 카드 설명과 정적 미리보기 | 정적 프론트 데이터 유지로 정책 확정 | `STATIC` | 공개 catalog API를 새로 만들 필요 없음 |
| 회사 소개·파트너·SNS 등 | 배포 콘텐츠 | 정적 유지 가능 | `STATIC` | 운영자가 배포 없이 수정해야 할 때만 CMS 도입 |

### 1.3 프론트 연동 전에 반드시 해결할 백엔드 차단 사항

다음 항목은 프론트 매핑만으로 해결되지 않는다.

1. 일반 이메일 로그인·이메일 중복 확인·비밀번호 복구 API — 회원가입(이메일 인증 포함)은 2026-08-19 구현 완료(§3.13), 로그인/중복확인/복구만 남음
2. 전화번호+이메일 방식 신청 조회를 유지할 경우 조회 Repository/Service 계약
3. 학생증 `schoolName`이 실제 카드·신청 상세에 필요한지 확정 후 저장 모델
4. 단체 ZIP 허용 크기와 Spring multipart 설정 및 parser resource limit
5. 후기 다중 이미지를 유지할지, 화면을 단일 이미지로 제한할지 결정
6. Inquiry 도메인
7. 관리자 Application HTTP API
8. Event의 회사명·로고 표시 계약
9. 카드 다운로드 가능 기준을 `COMPLETED`가 아니라 `cardReadyAt`으로 볼지 현재 정책과 구현 동기화

---

## 2. 프론트 공통 연동 규칙

### 2.1 API Base URL

현재 `api.ts`는 `import.meta.env.VITE_API_BASE_URL ?? "`을 사용한다.

- Docker/Nginx에서 동일 origin을 사용하면 빈 문자열로 둔다.
- Vite 단독 개발은 현재 proxy가 없으므로 `VITE_API_BASE_URL`에 백엔드 주소를 지정한다.
- 인증 요청은 `credentials: include`를 사용한다.

### 2.2 인증 방식

- OAuth 시작: `GET /oauth2/authorization/google`, `GET /oauth2/authorization/naver`
- access/refresh token은 HttpOnly cookie로 전달되며 localStorage에 저장하지 않는다.
- 401이면 `POST /api/auth/refresh` 후 원 요청을 한 번만 재시도한다.
- refresh도 실패하면 사용자 상태를 비우고 로그인 화면으로 이동한다.
- 공개 API: 신청 lookup, 후기 GET, Board GET, Event GET
- ADMIN API: `/api/admin/**`
- 나머지 `/api/**`: USER 또는 ADMIN

### 2.3 공통 응답 타입

```ts
interface ValidationErrorDetail {
  row: number | null;
  field: string;
  code: string;
  message: string;
}

interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  errorCode: string | null;
  errorMessage: string | null;
  errors?: ValidationErrorDetail[];
}
```

일반 실패는 최상위 `errorCode`, `errorMessage`를 사용한다. Bean Validation과 단체 파일 검증은 `errors`를 추가한다. 프론트는 이를 버리지 않고 다음처럼 사용한다.

- `row`: Excel 행
- `field`: 입력 영역
- `code`: UI 분기
- `message`: 사용자 표시

Spring Security의 401/403은 JSON body가 없을 수 있으므로 HTTP status fallback도 유지한다.

### 2.4 페이지 응답

```ts
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

페이지는 0부터 시작한다. 화면의 1페이지는 요청 `page=0`으로 변환한다.

### 2.5 enum·날짜 매핑

| 프론트 기존 값 | API 값 |
|---|---|
| `personal` / `organization` | `INDIVIDUAL` / `GROUP` |
| `mobile` / `mobile_and_physical` | `MOBILE` / `MOBILE_AND_PHYSICAL` |
| `male` / `female` | `MALE` / `FEMALE` |
| `university` / `highschool` | `UNIVERSITY` / `HIGH_SCHOOL` |
| `landscape` / `portrait` | `LANDSCAPE` / `PORTRAIT` |
| `booth` / `collaboration` | `BOOTH` / `COLLABORATION` |

신청 상태는 `SUBMITTED`, `REVIEWING`, `PHOTO_REJECTED`, `NAME_EDITING`, `PRODUCTION_READY`, `PRODUCING`, `COMPLETED`, `CANCELLED`이다. 결제 상태는 별도의 `WAITING`, `CONFIRMED`다.

- `LocalDate`: `YYYY-MM-DD`
- `LocalTime`: ISO local time
- `LocalDateTime`: timezone suffix 없는 ISO 문자열

---

## 3. 화면별 실제 API 연동 계약

### 3.1 OAuth·약관

| 동작 | Method/URL | 인증 |
|---|---|---|
| Google 시작 | `GET /oauth2/authorization/google` | 없음 |
| Naver 시작 | `GET /oauth2/authorization/naver` | 없음 |
| 현재 사용자 | `GET /api/users/me` | USER/ADMIN |
| 갱신 | `POST /api/auth/refresh` | refresh cookie |
| 로그아웃 | `POST /api/auth/logout` | USER/ADMIN |

OAuth 성공 후 신규 사용자는 `/terms`, 기존 사용자는 `/`, 실패는 `/login?error=oauth`로 이동한다.

약관 요청:

```http
POST /api/auth/terms
```

```json
{
  privacyAgreed: true,
  imageUploadAgreed: true,
  shippingAgreed: true
}
```

세 값 모두 필수다. 성공 후 `GET /api/users/me`로 사용자를 다시 읽는다.

### 3.2 회원정보

| 동작 | Method/URL | 계약 |
|---|---|---|
| 조회 | `GET /api/users/me` | `id,name,email,role,phone,address` |
| 수정 | `PATCH /api/users/me` | 요청 `name?,phone?`, 수정된 User 응답 |
| 탈퇴 | `POST /api/users/me/withdraw` | body 없음 |

수정 시 두 필드를 모두 생략하거나 name을 blank로 보내면 `INVALID_INPUT`이다. address는 조회에는 포함되지만 현재 수정 요청에는 없다. 탈퇴 성공 시 클라이언트 사용자 상태를 즉시 제거한다.

### 3.3 개인 신청 생성

`POST /api/applications`, `multipart/form-data`

| part | 필수 |
|---|---|
| `request` JSON | 항상 |
| `photo` | 항상 |
| `schoolLogo` | STUDENT |
| `schoolSeal` | STUDENT 선택 |

`request` 필드:

```text
cardTypeId, issueType, orientation?, schoolType?
applicant { name, phone, email? }
receiver? { sameAsApplicant, name?, phone?, zipCode, address, detailAddress?, deliveryRequest? }
member { englishName, birthDate, nationality, birthTime?, birthRegion?, gender, entryDate?, studentId?, department? }
```

`MOBILE`은 receiver를 보내지 않고 `MOBILE_AND_PHYSICAL`은 receiver가 필수다. 일반 카드는 학생증 필드·파일을 보내면 안 된다.

학생증 조건:

- STUDENT: `orientation`, `schoolType`, `schoolLogo` 필수
- UNIVERSITY: `studentId`, `department` 필수
- HIGH_SCHOOL: `studentId`, `department` 미전송
- `nationality`: ISO 3166-1 alpha-2
- `birthDate`: 과거 날짜
- 얼굴사진: 5 MiB 이하, jpg/jpeg/png, signature·decode, EXIF 반영 300×400 이상
- 로고·직인: 같은 파일 검증, 최소 해상도 제외

성공 data:

```ts
interface ApplicationCreateResponse {
  applicationId: number;
  applicationNumber: string;
  status: SUBMITTED;
  paymentStatus: WAITING;
  createdAt: string;
}
```

현재 프론트는 `orientation`, `schoolType`, Applicant email을 요청에 포함해야 한다. 화면의 `schoolName`은 백엔드 저장 필드가 없다.

### 3.4 단체 신청 생성

`POST /api/applications/bulk`, `multipart/form-data`

| part | 필수 |
|---|---|
| `request` JSON | 항상 |
| `logo` | 항상 |
| `seal` | 일반 카드 필수, 학생증 선택 |
| `submitFile` ZIP | 항상 |

`request` 필드:

```text
cardTypeId, issueType, orientation?, schoolType?
applicant { organizationName?, department?, name, phone, email? }
receiver? { sameAsApplicant, organizationName?, department?, name?, phone?, zipCode, address, detailAddress?, deliveryRequest? }
```

단체 수량은 요청하지 않는다. 서버가 실제 처리한 Excel 행 수를 `totalQuantity`로 반환한다. 프론트는 사용자 입력 quantity 대신 이 값을 표시한다.

### 3.5 신청 조회

`POST /api/applications/lookup`, 공개 API

신청번호 방식:

```json
{
  method: application,
  keyValue: ROK-20260818-000001,
  phone: +82-10-1234-5678,
  email: user@example.com
}
```

카드번호 방식:

```json
{ method: card, keyValue: CARD-000001 }
```

`method` 값은 소문자다. 응답은 `applicationId, applicationNumber, applicantNameMasked, cardType, status, photoRejectReason, submittedAt`을 제공한다.

현재 프론트의 phone+email만 입력하는 탭은 이 계약으로 연결할 수 없다. 신청번호 입력을 추가하거나 백엔드 조회 계약을 변경해야 한다.

### 3.6 마이페이지 신청 목록·상세

> 현재 워킹 트리 구현 기준이며 아직 커밋되지 않았다.

목록:

```http
GET /api/my/applications?status=&page=0&size=20
```

목록 항목:

```text
applicationId, applicationNumber, applicationType
cardTypeId, cardTypeName, totalQuantity
status, paymentStatus, createdAt
```

상세:

```http
GET /api/my/applications/{applicationId}
```

상세 추가 필드:

```text
issueType, paymentGuidedAt, paymentDueAt
cancelledAt, cancellationType, cancellationReason, refundedAt
cardReadyAt, physicalDispatchedAt, photoRejectReason
applicant, receiver, memberCount
```

`issueType=MOBILE`이면 `receiver=null`이다. 단체 구성원별 데이터가 아니라 전체 `memberCount`만 제공한다.

### 3.7 취소·사진 재업로드

취소:

```http
POST /api/applications/{applicationId}/cancel
```

- body 없음
- 가능 상태: `SUBMITTED`, `REVIEWING`, `PHOTO_REJECTED`
- 이미 CANCELLED면 200 멱등 성공
- 응답: `applicationId, status, paymentStatus, refundRequired, cancelledAt`

사진 재업로드:

```http
PATCH /api/applications/{applicationId}/photo
Content-Type: multipart/form-data
```

- 개인: `photo` part
- 단체: `submitFile` part
- `PHOTO_REJECTED`에서만 가능
- 성공: `applicationId`, `status=REVIEWING`

프론트는 ApplicationType에 따라 part와 안내 문구를 분리한다.

### 3.8 카드 다운로드

```http
GET /api/applications/{applicationId}/cards/download
```

- 로그인한 신청 소유자만 가능
- 현재 구현은 `COMPLETED`에서만 허용
- presigned URL 만료는 7일
- 개인: `cardFrontUrl`, `cardBackUrl`
- 단체: `downloadUrl` ZIP
- 공통: `applicationId, applicationType, expiresAt`

공개 lookup 결과의 `applicationId`만으로 비로그인 다운로드할 수 없다. 조회 화면에서 즉시 카드를 보여주려면 인증 사용자 흐름으로 제한하거나 별도 공개 다운로드 정책이 필요하다.

또한 확정 정책의 `cardReadyAt`과 현재 `COMPLETED` 조건이 다르므로 프론트 버튼 노출 기준을 고정하기 전에 백엔드가 동기화돼야 한다.

### 3.9 후기

| Method/URL | 역할 | 인증 |
|---|---|---|
| `GET /api/reviews` | 목록·검색 | 공개 |
| `GET /api/reviews/{id}` | 상세 | 공개 |
| `POST /api/reviews` | 작성 | USER |
| `PATCH /api/reviews/{id}` | 수정 | 소유자/ADMIN |
| `DELETE /api/reviews/{id}` | 삭제 | 소유자/ADMIN |

목록 query는 `cardTypeId, hasPhoto, searchType, keyword, page, size`다. 작성·수정은 multipart이며 `request` JSON과 선택 `image` 0~1개를 보낸다.

`request` 필드:

```text
title, applicationType, cardTypeId, authorName, content
수정만 removeImage 추가
```

상세 응답의 `canEdit`, `canDelete`를 버튼 기준으로 사용한다. 프론트가 email을 비교해 권한을 판단하면 안 된다. 현재 서버는 이미지 1개만 지원한다.

마이페이지 내 후기:

```http
GET /api/my/reviews?page=0&size=9
```

인증 사용자의 후기만 `createdAt DESC, id DESC`로 반환하며 응답 구조는 공개 목록과 같다.

### 3.10 공지사항·FAQ

| Method/URL | 역할 | 인증 |
|---|---|---|
| `GET /api/boards?type=NOTICE|FAQ&page=0&size=9` | 목록 | 공개 |
| `GET /api/boards/{id}` | 상세 | 공개 |
| `POST /api/admin/boards` | 생성 | ADMIN |
| `PATCH /api/admin/boards/{id}` | 수정 | ADMIN |
| `DELETE /api/admin/boards/{id}` | 삭제 | ADMIN |

관리자 생성·수정은 multipart다. `request`는 `boardType,title,content`, `attachments`는 NOTICE에서만 최대 10개다. 수정 시 `keepAttachmentIds`로 기존 첨부 유지 목록을 보낸다.

상세 첨부는 `{id, originalFileName, url}[]`이다. 프론트는 파일을 합성하지 않고 `url`을 다운로드 링크로 사용한다. FAQ에 첨부를 보내면 `INVALID_INPUT`이다. 공지 검색 query는 아직 없다.

### 3.11 행사

| Method/URL | 역할 | 인증 |
|---|---|---|
| `GET /api/events?type=BOOTH|COLLABORATION&page=0&size=10` | 목록 | 공개 |
| `GET /api/events/{id}` | 상세 | 공개 |
| `POST /api/admin/events` | 생성 | ADMIN |
| `PATCH /api/admin/events/{id}` | 수정 | ADMIN |
| `DELETE /api/admin/events/{id}` | 삭제 | ADMIN |

목록 필드:

```text
id, eventType, title, eventDate, eventDateText
place, host, cardLabel, content
thumbnailImageUrl, displayOrder
```

상세는 `images {id,originalFileName,url}[]`를 추가한다. 프론트 갤러리는 `[thumbnailImageUrl, ...images]`로 구성한다.

현재 화면의 `company`, `logoUrl`은 서버 응답에 없다. `host`, `thumbnailImageUrl`로 대체 가능한지 결정하기 전에는 해당 카드 UI를 완전히 연결할 수 없다. 비공개 행사를 다시 찾는 관리자 전체 목록 API도 없다.

### 3.12 아직 연동할 수 없는 화면

다음은 프론트 문제가 아니라 백엔드 계약이 없어서 API 교체를 시작하면 안 되는 영역이다.

일반 인증(회원가입은 2026-08-19, 계정 복구는 2026-08-21 구현 완료 — §3.13·`docs/api/auth.md` API 7·8 참고. `login`/`email/check`/`users/me/password`도 이미 구현돼 있음 — 아래는 프론트 미연동 상태만 남은 목록이지 백엔드 미구현이 아니다):

```text
POST /api/auth/login
POST /api/auth/email/check
POST /api/auth/recovery/id/request
POST /api/auth/recovery/id/confirm
POST /api/auth/recovery/password/request
POST /api/auth/recovery/password/confirm
PATCH /api/users/me/password
```

Inquiry:

```text
POST /api/inquiries
GET /api/my/inquiries
GET /api/my/inquiries/{id}
GET /api/admin/inquiries
GET /api/admin/inquiries/{id}
POST /api/admin/inquiries/{id}/answer
```

위 경로는 요구 동작 목록이며 실제 구현 전 도메인 API 문서에서 확정한다.

관리자 신청관리(목록·상세 조회 2건은 2026-08-21 구현 완료, 커밋 `6575d09` — 나머지는 여전히 없음):

```text
GET /api/admin/applications          # ✅ 구현 완료
GET /api/admin/applications/{id}     # ✅ 구현 완료
POST /api/admin/applications/{id}/payment-guide
POST /api/admin/applications/{id}/payment-confirm
POST /api/admin/applications/{id}/start-review
POST /api/admin/applications/{id}/photo-reject
POST /api/admin/applications/{id}/approve-review
POST /api/admin/applications/{id}/production-start
POST /api/admin/applications/{id}/complete
POST /api/admin/applications/{id}/refund-complete
GET /api/admin/stats
```

숨김 행사를 포함하는 `GET /api/admin/events`도 아직 없다.

---

### 3.13 일반 이메일 회원가입(이메일 인증 포함) — 신규 READY(2026-08-19)

3단계를 순서대로 호출해야 한다. 최신 소스는 `docs/api/auth.md` API 4~6(요청/응답 예시, 에러코드까지 상세).

| 단계 | Method/URL | 인증 |
|---|---|---|
| ① 인증 코드 요청 | `POST /api/auth/signup/email-verification/request` | 없음 |
| ② 인증 코드 확인 | `POST /api/auth/signup/email-verification/confirm` | 없음 |
| ③ 회원가입 완료 | `POST /api/auth/signup` | 없음 |

```json
// ① 요청 { "email": "user@example.com" }
// ① 응답 { "expiresInSeconds": 600, "resendAfterSeconds": 60 }

// ② 요청 { "email": "user@example.com", "code": "482193" }
// ② 응답 { "signupToken": "...", "expiresInSeconds": 1800 }

// ③ 요청
{
  "email": "user@example.com",
  "signupToken": "②의 signupToken",
  "password": "8~72자, 복잡도 규칙 없음",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
// ③ 성공: 201 + Set-Cookie(accessToken/refreshToken, OAuth와 동일) + 사용자 정보(GET /api/users/me와 동일 필드)
```

핵심 제약:

- `email`/`signupToken`/`password`/`name`/`phone` 5개 모두 필수(현재 `SignupPage.tsx` 폼이 이미 name/email/password/phone을 받고 있음 — `signupToken`만 새로 필요, ①·②를 거쳐야 얻을 수 있다).
- `signupToken`은 ②에서 발급 후 30분 유효한 1회성 토큰 — 화면 흐름상 ②와 ③ 사이에 시간이 오래 걸리면 만료될 수 있다.
- 재전송 대기 60초, 이메일당 1시간 5회, IP당 1시간 20회 제한이 있어 ①에서 `TOO_MANY_REQUESTS`(429)가 날 수 있다.
- ②의 코드 검증은 최대 5회, 실패해도 남은 횟수는 알려주지 않는다(`INVALID_VERIFICATION_CODE`, 400) — 5회 실패 시 ①부터 다시 해야 한다.
- ③ 성공 후 프론트는 `/terms`로 이동(약관 동의는 기존 `POST /api/auth/terms` 그대로).

**프론트 미착수 항목**: `SignupPage.tsx`가 아직 이 3단계 흐름을 호출하지 않는다(현재는 폼 제출 시 로컬 mock 로그인 처리만 함) — 인증 코드 입력 UI 자체가 없어 화면 설계가 먼저 필요하다.

---

## 4. 프론트 목데이터 교체 순서

1. OAuth·약관·회원정보 조회
2. 개인/단체 신청 생성과 `errors[]` 표시
3. 내 신청 목록·상세를 커밋한 뒤 마이페이지 연결
4. 신청 취소·사진 재업로드·카드 다운로드
5. Review 단일 이미지 정책에 맞춰 후기 연결
6. FAQ, 공지, Event 순서로 콘텐츠 연결
7. 일반 인증·Inquiry·관리자 신청관리 구현 후 해당 화면 연결

각 화면은 API 연결이 완료된 뒤 해당 localStorage 원본 사용을 제거한다. 작성 중 신청 draft, 언어 선택, 모달 상태 등 UI 전용 저장은 유지할 수 있다.

---

## 5. 프론트 연동 완료 검증 체크리스트

### 공통

- [ ] 인증 요청에 `credentials: include` 적용
- [ ] 401 refresh 재시도 최대 1회
- [ ] refresh 실패 시 사용자 상태 초기화
- [ ] `ApiEnvelope.errors[]`의 필드·행 오류 표시
- [ ] 0-based 페이지 변환
- [ ] enum 변환을 공통 mapper로 관리
- [ ] API 실패를 목데이터 성공으로 바꾸는 fallback 제거

### 신청

- [ ] 학생증에 `orientation`, `schoolType` 전송
- [ ] 대학교만 학번·학과 전송
- [ ] MOBILE에는 receiver 미전송
- [ ] MOBILE_AND_PHYSICAL에는 receiver 전송
- [ ] Applicant email 전송
- [ ] 단체 수량은 응답 `totalQuantity` 사용
- [ ] 단체 오류의 row/field/code/message 표시
- [ ] 개인과 단체 재업로드 part 구분

### 일반 회원가입

- [ ] ①인증코드 요청 → ②코드 확인 → ③회원가입 순서 강제(③에 ②의 `signupToken` 사용)
- [ ] ①의 429(재전송 대기/횟수 초과), ②의 `INVALID_VERIFICATION_CODE`(남은 시도 횟수 비노출) 메시지 처리
- [ ] ③ 성공 후 `/terms`로 이동(약관 동의 API는 기존 것 재사용)
- [ ] 인증 코드 입력 UI 신규 설계(현재 화면엔 없음)

### 조회·콘텐츠

- [ ] lookup의 실제 `keyValue` 계약 준수
- [ ] 공개 lookup과 로그인 전용 다운로드 분리
- [ ] 상세 `receiver=null` 처리
- [ ] 후기 권한은 `canEdit/canDelete` 사용
- [ ] 공지 첨부는 서버 URL 사용
- [ ] FAQ에는 첨부 UI 미노출
- [ ] 행사 썸네일과 상세 images 조합

---

## 6. 최종 판단

현재 백엔드는 신청 생성, 사용자 신청 조회·취소, 사진 재업로드, 후기, 공지/FAQ, 행사 공개 흐름, 일반 이메일 회원가입(이메일 인증 포함, §3.13)의 상당 부분을 제공한다. 이 영역은 위 계약대로 프론트 연동을 시작할 수 있다.

하지만 기존 화면 전체 요구사항이 빠짐없이 구현된 상태는 아니다. 일반 로그인·이메일 중복확인·계정복구, Inquiry, 관리자 신청관리 API는 미구현이고, 신청 조회 방식, 학교명, 단체 업로드 한도, 후기 다중 이미지, 공지 검색, 행사 회사·로고, 카드 다운로드 기준에는 계약 공백이 남아 있다.

프론트는 `READY` 영역부터 API로 교체하고, `PARTIAL`과 `BLOCKED` 영역은 백엔드 계약이 확정·구현될 때까지 목데이터를 제거하지 않는다.
