# 프론트엔드 API 연동 명세

> 현재 코드 기준: 2026-09-16
>
> 대상: `frontend/src/services/api.ts`와 실제 호출 화면
>
> 역할: **프론트 공통 통신 규칙과 화면별 API 사용 방식의 Source of Truth**

도메인별 필드·Validation·상태 전이의 원본은 `docs/api/*.md`와 `docs/specs/*/api.md`다. 현재 미완료 여부는 [`FRONTEND_API_GAPS.md`](./FRONTEND_API_GAPS.md)에서만 관리한다. 이 문서에는 과거 준비도나 이미 해결된 “미연동” 판정을 남기지 않는다.

## 1. 공통 클라이언트

API 타입과 래퍼는 `frontend/src/services/api.ts`에 집중한다. 화면은 `fetch`나 endpoint 문자열을 다시 작성하지 않고 `api.*`를 호출한다.

### Base URL과 개발 프록시

- `VITE_API_BASE_URL`의 끝 `/`를 제거해 사용한다.
- 값이 없으면 same-origin 요청이다.
- Vite 개발 서버는 `/api`, `/oauth2`, `/login/oauth2`를 `VITE_DEV_BACKEND`로 프록시한다.
- 운영에서는 nginx가 같은 경로를 백엔드로 전달한다.

### 요청 규칙

```ts
fetch(`${API_BASE_URL}${path}`, {
  ...init,
  credentials: "include",
  headers: {
    "Accept-Language": getLanguage(),
    // body가 FormData가 아닐 때만
    "Content-Type": "application/json",
    ...init.headers,
  },
});
```

- 인증은 HttpOnly `accessToken`/`refreshToken` 쿠키를 사용한다.
- 401이면 `POST /api/auth/refresh`를 호출하고 성공할 때 원 요청을 딱 한 번 재시도한다.
- refresh 실패 또는 재시도 401을 반복하지 않는다.
- 파일 다운로드도 동일한 cookie/refresh 규칙을 적용하며 JSON envelope 대신 `Blob`을 반환한다.

### 공통 응답과 오류

```ts
interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  errorCode: string | null;
  errorMessage: string | null;
  errors?: Array<{
    row: number | null;
    field: string;
    code: string;
    message: string;
  }>;
}
```

실패는 HTTP status, 서버 error code와 `errors[]`를 보존한 `ApiError`로 변환한다. 표시 메시지는 현재 언어와 `serverErrors.ts` 매핑을 적용한다. Bean Validation과 단체 Excel 행 오류는 `errors[]`를 버리지 않는다.

### 페이지 응답

```ts
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

페이지 번호는 0부터 시작한다. 관리자 신청·후기·게시판·행사에는 `AdminPager`, 마이페이지 신청·후기에는 더보기, 공개 공지에는 페이지 이동을 사용한다. 검색 조건을 바꾸면 첫 페이지로 되돌린다.

## 2. 인증과 계정

| 화면/기능 | 실제 API | 사용 방식 |
|---|---|---|
| 앱 시작·프로필 갱신 | `GET /api/users/me` | `AuthContext`가 서버 세션을 확정. 401/403과 네트워크·5xx를 구분 |
| 이메일 로그인 | `POST /api/auth/login` | 성공 사용자와 role을 메모리에 반영 |
| OAuth | `/oauth2/authorization/{google|naver}` | 브라우저 이동, 신규 사용자는 `/terms` |
| 약관 | `POST /api/auth/terms` | 성공 후 `/me` 갱신, 홈 이동 |
| 회원가입 | `POST /api/auth/signup/email-verification/request`, `/confirm`, `/api/auth/signup` | 인증 코드 → 1회성 signupToken → 가입 |
| 이메일 확인 | `POST /api/auth/email/check` | 가입 폼 중복 확인 |
| 계정 복구 | `/api/auth/recovery/id/*`, `/api/auth/recovery/password/*` | requestId 기반 2단계 확인 |
| 내 정보 | `GET/PATCH /api/users/me` | 이름·전화번호·주소 표시/수정 |
| 비밀번호·탈퇴 | `PATCH /api/users/me/password`, `POST /api/users/me/withdraw` | 성공 후 세션 상태 갱신 |
| 로그아웃 | `POST /api/auth/logout` | 서버 요청 후 로컬 인증 상태 제거 |

`AuthStatus`는 `loading | authenticated | unauthenticated | error`다. 관리자 화면의 클라이언트 가드는 UX 용도이며 `/api/admin/**` 권한은 서버가 검사한다.

현재 `/me`의 role 부재와 임시 UI 힌트는 갭 문서 §1에서 관리한다.

## 3. 제작 신청

### 개인 신청

`POST /api/applications`에 `FormData`를 보낸다.

| part | 조건 |
|---|---|
| `request` | 필수 JSON Blob. 카드 종류·발급 방식·신청인·member·조건부 receiver 포함 |
| `photo` | 개인 사진 |
| `schoolLogo` | 학생증일 때 필수 |
| `schoolSeal` | 학생증 선택 파일 |

학생증만 `orientation`, `schoolType`, `schoolName` 또는 선택한 `schoolId`를 전송한다. 대학이면 `studentId`와 `department`를 포함하고 고등학교이면 생략한다. 비학생증 개인 신청은 카드 표기용 `member.address`를 전송한다. 실물 발급일 때만 receiver를 전송한다.

### 단체 신청

`POST /api/applications/bulk`에 `request`, `submitFile`(Excel·사진 ZIP), `logo`, 조건부 `seal`을 보낸다. 수량은 클라이언트 입력값이 아니라 서버가 Excel 유효 행 수로 계산한다. 서버의 행 Validation은 `ApiError.errors[]`로 유지한다.

### 신청 draft

작성 중 값은 `sessionStorage.application-draft`에 보존한다. File 객체와 preview URL은 저장하지 않으며 복원 시 재첨부를 요구한다. 제출 성공 후 draft를 제거한다.

### 조회·마이페이지·후속 작업

| 기능 | 실제 API |
|---|---|
| 공개 조회 | `POST /api/applications/lookup` |
| 내 신청 목록·상세 | `GET /api/my/applications`, `GET /api/my/applications/{id}` |
| 취소 | `POST /api/applications/{id}/cancel` |
| 사진 재업로드 | `PATCH /api/applications/{id}/photo` |
| 입금자명 | `PATCH /api/applications/{id}/depositor` |
| 카드 다운로드 | `GET /api/applications/{id}/cards/download` |
| 학교 검색 | `GET /api/schools/search?query=...` |

목록과 상세의 사용자 ID는 프론트에서 보내지 않는다. 인증된 사용자 식별은 서버 principal이 담당한다.

## 4. 후기·게시판·행사·문의

### 후기

- 공개: `GET /api/reviews`, `GET /api/reviews/{id}`
- 사용자: `GET /api/my/reviews`, `POST /api/reviews`, `PATCH/DELETE /api/reviews/{id}`
- 작성·수정은 JSON `request`와 반복 `image` part를 사용하며 이미지는 0~5개다.
- 수정 시 유지할 서버 이미지 ID를 `keepImageIds`로 보낸다.

### 공지와 FAQ

- 공개: `GET /api/boards`, `GET /api/boards/{id}`
- 관리자: `POST /api/admin/boards`, `PATCH/DELETE /api/admin/boards/{id}`
- 공지 검색은 `searchType`, `keyword`, `page`, `size`를 서버로 보낸다.
- 관리자 작성·수정은 JSON `request`와 반복 `attachments` part를 사용한다.

### 행사

- 공개: `GET /api/events?type=BOOTH|COLLABORATION`
- 관리자: `GET/POST /api/admin/events`, `GET/PATCH/DELETE /api/admin/events/{id}`
- multipart part는 `request`, 단일 `thumbnail`, 단일 `logo`, 반복 `images`다.
- 행사 PROGRAM 카드 3종은 확정된 정적 콘텐츠이며 API 대상이 아니다.

### 문의

- 사용자: `POST /api/inquiries`, `GET /api/my/inquiries`, `GET /api/my/inquiries/{id}`
- 관리자: `GET /api/admin/inquiries`, 상세, 답변, 상태 변경 API
- 작성 요청에는 `privacyConsent`를 포함한다.

## 5. 관리자 신청·작명·카드

### 신청 목록과 상세

| 기능 | 실제 API |
|---|---|
| 목록 | `GET /api/admin/applications?page&size&status` |
| 상세 | `GET /api/admin/applications/{id}` |
| 멤버 | `GET /api/admin/applications/{id}/members` |
| 통계 | `GET /api/admin/stats` |
| Excel export | `POST /api/admin/applications/export` |
| 작명 결과 Excel | `POST /api/admin/applications/{id}/naming-result` |
| 전체 카드 ZIP | `GET /api/admin/applications/{id}/cards/download` |
| 이름 선택 이력 통계 | `GET /api/admin/name-selection-stats` |

상태 전이는 결제 확인, 검토 시작, 사진 반려, 작명 승인·완료, 제작 시작, 카드 준비, 배송 발송 API를 현재 상태에 맞춰 노출한다. 가능한 전이는 서버가 최종 검증한다.

### 이름 추천

1. 신청 상세와 멤버를 가져온다.
2. `GET /api/admin/applications/{id}/manseryeok-results` 한 번으로 활성 결과를 일괄 복원한다.
3. 저장 결과를 `fromActiveManseryeokResult`로 검증·변환한다.
4. `namingRecommendations.ts`가 번들 이름 사전으로 score 내림차순, 사전 index 오름차순 상위 5개를 계산한다.
5. 관리자가 고른 이름과 성씨를 `POST /api/admin/applications/{id}/members/{memberId}/name`으로 저장한다.

확정 만세력이 없거나 저장 데이터가 손상되었으면 실제 멤버 추천을 만들지 않는다. 사용되지 않던 단건 활성 결과 프론트 래퍼는 제거했고 운영 화면은 일괄 API를 사용한다.

### 만세력 확정

1. `GET /api/admin/birth-region/search?query=...`
2. `POST .../members/{memberId}/manseryeok/resolve`
3. EXACT, UNKNOWN_TIME 또는 중복 offset 선택 결과를 계산
4. `POST .../members/{memberId}/manseryeok`
5. 성공 후 일괄 GET을 다시 호출해 서버 저장 결과로 화면 갱신

프론트 계산값을 저장 성공값으로 간주하지 않는다. `AMBIGUOUS_LOCAL_TIME`은 관리자가 offset 후보를 고르게 하고 존재하지 않는 현지 시각은 저장하지 않는다.

### 카드 제작

- `GET /api/admin/card-designs?cardTypeId&active&applicationId`
- `POST .../members/{memberId}/card-preview`
- `POST .../members/{memberId}/card-generate`
- `GET .../members/{memberId}/cards/download`
- `PUT .../members/{memberId}/card-number`
- `PUT /api/admin/applications/{id}/card-numbers`로 사진번호 기반 일괄 저장

카드 생성 중 버튼을 비활성화한다. 디자인과 발급일을 선택한 뒤 미리보기와 생성을 분리한다.

#### 학생증 앞·뒤 텍스트 색상 (미구현 갭 — `FRONTEND_API_GAPS.md` P2)

`card-preview`/`card-generate` 요청 바디는 `studentFrontTextColor`, `studentBackTextColor`(`DARK_GRAY` | `WHITE`) 두 필드를 선택적으로 받는다(2026-09-19 백엔드 구현 완료, `docs/specs/application/checklist.md` §6).

- **노출 조건**: 신청의 카드종류가 `STUDENT`일 때만 선택 UI를 보여준다. 비학생증 신청에서 이 필드를 보내면 `INVALID_INPUT`으로 거절된다 — 두 필드 모두 아예 안 보내야 한다.
- **배치**: 아직 프론트에 없는 `zodiacDesignSet`(십이간지 디자인 세트) 선택 UI와 같은 영역에 두도록 설계됐다(`FRONTEND_API_GAPS.md` P0 항목).
- **단위**: 신청(Application) 전체에 앞면 1개·뒷면 1개 — 단체 신청도 구성원별로 다른 색상을 줄 수 없다.
- **기본값**: 생략하면 서버가 해당 면을 `DARK_GRAY`로 처리한다. 카드 생성 자체를 막지는 않는다.
- **새로고침 복원**: 확정된 값은 `MyApplicationDetailResponse.studentFrontTextColor`/`studentBackTextColor`로 내려온다 — 신청 상세 조회 시 이 값으로 선택 UI를 복원한다.
- **잠금 조건**: 카드가 이미 생성된 신청에서 **다른** 색상으로 재요청하면 `STUDENT_TEXT_COLOR_MISMATCH`(400)로 거절된다. **같은** 색상으로는 재생성(재발급)이 계속 허용된다. UI는 이미 확정된 색상과 다른 값을 고르지 못하게 막거나, 최소한 이 에러코드를 사용자 메시지로 안내해야 한다(`features/i18n/serverErrors.ts`에 매핑 추가 필요).

### 학생증 템플릿

- `GET /api/admin/schools/{schoolId}/card-template?orientation=...`
- `POST /api/admin/schools/{schoolId}/card-template`

업로드는 `orientation`, `front`, `back` multipart를 사용한다.

## 6. 다국어와 표시 규칙

- 모든 API 요청에 `Accept-Language: ko | en`을 보낸다.
- API enum은 `features/apply/mappers.ts` 등 경계 함수에서 화면 값과 변환한다.
- 날짜는 서버 ISO 문자열을 유지하고 화면에서 locale 표시로 변환한다.
- 서버 번역 가능 콘텐츠는 언어 변경 시 다시 조회한다.
- 알려진 error code는 `features/i18n/serverErrors.ts`, 나머지는 서버 메시지와 공통 fallback을 사용한다.

## 7. 변경 관리

- endpoint 또는 DTO 변경: 해당 도메인 API 문서를 먼저 갱신한다.
- 프론트 연결 방식 변경: 이 문서를 갱신한다.
- 완료/미완료 판정 변경: `FRONTEND_API_GAPS.md`만 갱신한다.
- 구현 이력: `docs/collab/CHANGELOG.md`에 기록한다.

## 8. 이력

| 날짜 | 변경 |
|---|---|
| 2026-09-20 | 카드 제작 절에 학생증 앞·뒤 텍스트 색상(`studentFrontTextColor`/`studentBackTextColor`) 계약 추가 — 백엔드는 2026-09-19에 구현 완료했으나 이 문서에 반영되지 않고 있던 갭(`FRONTEND_API_GAPS.md` P2로도 등록) |
| 2026-09-16 | 코드 전수 재대조. 단체 신청 ZIP part 명칭을 실제 계약(`submitFile`)으로 정정, 이름 선택 이력 통계 API 추가 |
| 2026-09-15 | 2026-08-18 준비도 스냅샷을 제거하고 현재 공통 클라이언트와 실제 화면 호출 중심으로 전면 재작성 |
| 2026-08-18~09-14 | 단계별 API 준비도 조사와 연결 수행. 상세 내역은 Git 및 `docs/collab/CHANGELOG.md` 참고 |
