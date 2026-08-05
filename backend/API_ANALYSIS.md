# 백엔드 API 및 프론트엔드 연동 차이 분석

분석 기준: `backend-api` 브랜치에서 가져온 실제 Controller 소스와 현재 `frontend/src` 코드. 백엔드 문서의 예정/구현 표시는 실제 코드와 차이가 있어, 아래의 "구현됨"은 Controller 매핑이 존재하는 경우만 뜻한다.

프론트 전체의 목데이터·정적 데이터·브라우저 저장소를 운영 API로 전환하기 위한 상세 요구사항과 데이터 계약은 [`FRONTEND_API_REQUIREMENTS.md`](./FRONTEND_API_REQUIREMENTS.md)를 기준으로 한다.

## 1. 실제 구현된 API

### 인증

| Method | Path | 용도 | 인증 |
|---|---|---|---|
| POST | `/api/auth/terms` | 필수/마케팅 약관 동의 저장 | 필요 |
| POST | `/api/auth/refresh` | refreshToken 쿠키로 토큰 재발급 | refreshToken 쿠키 |
| POST | `/api/auth/logout` | 세션·토큰 폐기 및 쿠키 만료 | 필요 |

별도 REST 로그인 API는 없다. Spring Security OAuth2 진입점인 `/oauth2/authorization/google`, `/oauth2/authorization/naver`를 사용하며, 성공 시 JWT를 HttpOnly 쿠키로 발급하는 구조다.

### 사용자

| Method | Path | 용도 | 인증 |
|---|---|---|---|
| GET | `/api/users/me` | 로그인 사용자 프로필 조회 | 필요 |
| PATCH | `/api/users/me` | 이름·전화번호·주소 수정 | 필요 |
| POST | `/api/users/me/withdraw` | 회원 탈퇴 | 필요 |

### 신청

| Method | Path | 용도 | 주요 요청 형식 | 인증 |
|---|---|---|---|---|
| POST | `/api/applications` | 개인 신청 생성 | multipart: `request`, `photo`, 선택 `schoolLogo`, `schoolSeal` | 필요 |
| POST | `/api/applications/bulk` | 단체 신청 생성 | multipart: `request`, `logo`, `seal`, `submitFile` | 필요 |
| POST | `/api/applications/lookup` | 연락처 또는 카드 번호로 신청 조회 | JSON: `method`, `keyValue`, 선택 `phone`, `email` | 불필요 |
| PATCH | `/api/applications/{applicationId}/photo` | 반려된 사진/제출 파일 재업로드 | multipart: 선택 `photo`, `submitFile` | 필요 |
| GET | `/api/applications/{applicationId}/cards/download` | 발급 카드 다운로드 정보 조회 | 없음 | 필요 |

총 11개의 명시적 REST API가 실제 Controller에 구현되어 있다(인증 3, 사용자 3, 신청 5). 이와 별도로 OAuth2 로그인 진입점 2개는 Spring Security가 제공한다.

## 2. 프론트에서 바로 연결 가능한 기능

| 프론트 기능 | 대응 백엔드 | 상태/주의점 |
|---|---|---|
| 개인 신청 | `POST /api/applications` | API는 있으나 프론트는 현재 localStorage에 저장한다. `cardTypeId` 매핑과 실제 File 객체 전달이 필요하다. |
| 단체 신청 | `POST /api/applications/bulk` | API는 있으나 multipart 필수 파일(`logo`, `seal`, `submitFile`)과 화면 입력 검증을 맞춰야 한다. |
| 신청 조회 | `POST /api/applications/lookup` | API는 있으나 프론트는 데모 값/localStorage만 검사한다. 성공 후 응답을 모바일 카드 화면으로 전달해야 한다. |
| 사진 재업로드 | `PATCH /api/applications/{id}/photo` | 백엔드만 구현되어 있고 프론트 전용 재업로드 흐름은 아직 없다. |
| 카드 다운로드 | `GET /api/applications/{id}/cards/download` | 백엔드만 구현되어 있고 모바일 카드/마이페이지에서 호출하지 않는다. |
| 내 프로필 | `GET/PATCH /api/users/me` | 백엔드 API는 있으나 마이페이지는 mock 로그인 정보만 표시한다. |
| 로그아웃/토큰 갱신 | `/api/auth/logout`, `/api/auth/refresh` | 백엔드는 쿠키 기반인데 프론트는 localStorage mock 인증이다. `credentials: "include"` 설정이 필요하다. |

중요한 계약 차이:

- 프론트 주석의 `POST /api/applications/{draftId}/submit`은 백엔드에 없다. 현재 백엔드는 `POST /api/applications` 한 번으로 신청을 생성한다.
- 프론트의 `designId`는 문자열이지만 백엔드는 숫자 `cardTypeId`를 요구한다. 카드 디자인/종류 조회 API가 없으므로 매핑 기준이 필요하다.
- 프론트 신청 데이터에는 이메일과 입금자명이 있지만 개인/단체 신청 DTO에는 이 필드가 없다. 이메일은 로그인 사용자 정보 사용 여부, 입금자명은 결제 API 도입 여부를 결정해야 한다.
- 프론트는 업로드 파일의 이름·크기·미리보기 URL만 draft에 저장하므로 제출 시 사용할 실제 `File` 객체 보관 방식이 필요하다.
- 백엔드 신청 상태 enum과 프론트 관리자 상태(`SUBMITTED`, `CONSULTING`, `PAYMENT_PENDING`, `IN_PRODUCTION`, `COMPLETED`, `CANCELLED`)가 다르므로 변환 규칙이 필요하다.

## 3. 프론트에 필요하지만 백엔드에 실제 구현되지 않은 API

### 우선순위 높음: 핵심 사용자 흐름

| 필요한 기능 | 권장 API 예시 | 근거 |
|---|---|---|
| 이메일/비밀번호 로그인 | `POST /api/auth/login` | 현재 로그인 화면은 일반 계정/관리자 mock 로그인을 제공하지만 백엔드는 OAuth2 로그인만 지원한다. OAuth2 전용으로 갈 경우 화면을 변경해야 한다. |
| 회원가입 | `POST /api/auth/signup` | 회원가입 화면의 이름·이메일·비밀번호·전화번호을 처리할 API가 없다. OAuth2 전용이면 가입 화면 대신 최초 로그인 후 약관/프로필 흐름으로 바꿔야 한다. |
| 아이디/비밀번호 찾기 | `POST /api/auth/recovery/id`, `POST /api/auth/recovery/password` | 계정 찾기 화면은 결과 문구만 출력한다. 백엔드에는 메일/SMS 인증 및 재설정 API가 없다. |
| 내 신청 목록 | `GET /api/my/applications` | 마이페이지가 사용자의 전체 신청 내역을 표시하지만 백엔드는 단건 공개 조회만 제공한다. |
| 내 신청 상세 | `GET /api/my/applications/{id}` | 마이페이지/모바일 카드에서 상태, 카드, 배송 정보를 확인할 상세 API가 없다. |
| 카드 종류·디자인 조회 | `GET /api/card-types`, `GET /api/card-designs` | 신청 요청은 `cardTypeId`를 요구하지만 프론트의 문자열 디자인 ID를 서버 ID로 변환할 API가 없다. |

### 우선순위 높음: 관리자 화면

현재 백엔드에는 `/admin/**` Controller가 하나도 없다. 프론트 관리자 화면을 실제 데이터로 전환하려면 최소 다음이 필요하다.

| 필요한 기능 | 권장 API 예시 |
|---|---|
| 신청 목록/검색 | `GET /admin/applications` |
| 신청 상세 | `GET /admin/applications/{id}` |
| 신청 상태 변경 | `PATCH /admin/applications/{id}/status` |
| 관리자 대시보드 통계 | `GET /admin/dashboard/stats` |
| 문의 목록/상세 | `GET /admin/inquiries`, `GET /admin/inquiries/{id}` |
| 문의 처리 상태/답변 | `PATCH /admin/inquiries/{id}` 또는 `POST /admin/inquiries/{id}/answer` |

백엔드 문서에는 한국 이름 등록, 카드 발급, 사진 반려, 배송 추적 등의 관리자 API가 설계돼 있지만 실제 Controller 구현은 없다.

### 우선순위 중간: 커뮤니티·고객지원

| 필요한 기능 | 권장 API 예시 | 현재 프론트 상태 |
|---|---|---|
| 후기 목록/상세/등록/수정/삭제 | `GET/POST /api/reviews`, `GET/PATCH/DELETE /api/reviews/{id}` | localStorage 및 초기 mock 데이터 사용 |
| 1:1 문의 등록/내 목록 | `POST /api/inquiries`, `GET /api/my/inquiries` | localStorage 사용 |
| 문의 상세 조회 | `GET /api/my/inquiries/{id}` | 마이페이지는 제목·상태만 표시 |

### 정책에 따라 선택: 콘텐츠 관리

공지사항, FAQ, 이벤트는 현재 프론트 정적 데이터/화면으로 동작한다. 관리자가 운영 중 내용을 수정해야 한다면 목록·상세 및 관리자 CRUD API가 추가로 필요하다. 정적 배포 콘텐츠로 유지한다면 필수 API는 아니다.

현재 프론트는 관리자 로그인 시 후기·공지사항·FAQ·이벤트를 localStorage에서 작성·수정·삭제할 수 있게 구현되어 있다. 여러 관리자와 기기에서 데이터를 공유하려면 다음 백엔드 API가 필요하다.

- 후기: `GET/POST /api/reviews`, `GET/PATCH/DELETE /api/reviews/{id}`
- 공지사항: `GET /api/notices`, `GET /api/notices/{id}`, `POST/PATCH/DELETE /admin/notices[/{id}]`
- FAQ: `GET /api/faqs`, `POST/PATCH/DELETE /admin/faqs[/{id}]`
- 이벤트: `GET /api/events`, `GET /api/events/{id}`, `POST/PATCH/DELETE /admin/events[/{id}]`

위 경로는 필요한 계약을 문서화한 제안이며 현재 백엔드에는 구현되어 있지 않으므로 프론트에서 호출하지 않는다.

## 4. 백엔드 문서에는 있으나 실제 구현되지 않은 주요 API

`docs/api/API_SPEC.md`와 프로젝트 상태 문서는 아래 API를 기술하지만 현재 소스에는 Controller가 없다.

- 사진 사전 업로드: `POST /api/uploads/photo`
- 배송지 등록/수정: `POST/PATCH /api/applications/{id}/shipping`
- 결제 정보/가상계좌/웹훅: `GET /api/applications/{id}/payment-info`, `POST /api/payments/virtual-account`, `POST /api/payments/webhook`
- 내 신청 목록/상세: `GET /api/my/applications`, `GET /api/my/applications/{id}`
- 단체 신청 템플릿·검증·확정 및 ZIP 다운로드 API
- 모든 관리자 API: 로그인, 신청 조회, 사진 반려, 한국 이름 등록/수정, 카드 발급, 배송 추적, 통계

따라서 연동 기준은 당분간 문서의 전체 API 표가 아니라 실제 Controller 11개로 잡아야 한다.

## 5. 권장 구현 순서

1. 인증 방식을 OAuth2 전용 또는 이메일/비밀번호 병행 중 하나로 확정하고 프론트 mock 인증을 제거한다.
2. 카드 종류/디자인 ID 조회 기준을 만든 뒤 개인·단체 신청과 신청 조회를 실제 API에 연결한다.
3. 내 신청 목록/상세 API를 추가해 마이페이지와 모바일 카드 화면을 연결한다.
4. 관리자 신청 목록·상태 변경 및 문의 관리 API를 구현한다.
5. 후기·문의 API를 구현해 localStorage 데이터를 제거한다.
6. 결제·배송·콘텐츠 관리 API는 실제 운영 범위에 맞춰 추가한다.
