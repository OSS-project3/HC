# 프론트 ↔ 백엔드 API 갭 · 목데이터 전환 목록

> **갱신: 2026-08-19.** 초판(2026-08-14)은 이후 Review·Board·Event·My-Review 구현/연동을 반영하지 못해 전면 갱신했다(2026-08-18). 이번 갱신은 §1.2(내 신청 목록·상세)·§1.5(신청 취소)가 `main`에 커밋·푸시(`b5f6140`)된 것만 반영한다. 프론트 연동 계약 종합은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`, 백엔드 미구현 상세는 `docs/BACKEND_API_GAPS.md`와 함께 본다. 프론트의 목데이터/localStorage 사용 자체는 결함이 아니라, 백엔드가 준비된 화면부터 순차 교체하는 방식이다.

> 대상: `frontend/src` 전체 · 근거: `services/api.ts`(실제 호출) ↔ `backend/honor-citizen/.../api/*Controller.java`(실구현) 상호 대조(현재 워킹 트리·`main` 기준).

---

## 0. 한눈에 보기

| 기능 영역 | 프론트 | 백엔드 | 상태 |
|---|---|---|---|
| OAuth 로그인·약관·세션·회원정보 | ✅ 실 API | ✅ 구현 | **연동 완료** (데모 로컬 로그인만 운영빌드에서 제거 필요) |
| 신청 생성(개인/단체) | ✅ 실 API | ✅ 구현 | **연동 완료** — 학생증 `orientation`/`schoolType`·대학교만 학번/학과·서버 `totalQuantity`·파일 파트(logo/seal/submitFile) 계약 반영, enum 공통 매퍼(`features/apply/mappers.ts`) |
| 신청 조회/카드다운로드 | ✅ 실 API | ✅ 준비 | 조회 응답에 `applicationType`이 포함돼 개인 `photo`/단체 `submitFile` 재제출 분기 가능. 단체 재제출 UI 연결만 남음. 카드다운로드는 소유자 로그인 전용(비로그인 조회는 데모 폴백) |
| 후기(Review) CRUD + 내 후기 | ✅ 실 API | ✅ 구현 | **연동 완료** |
| 공지/FAQ(Board) | ✅ 실 API | ✅ 구현 | **연동 완료** |
| 행사(Event) | ✅ 실 API | ✅ 구현 | **연동 완료** (`company`/`logoUrl` 필드 갭 §1.6) |
| **일반 이메일 회원가입·로그인·복구** | ❌ 목/로컬 | ❌ 없음 | **백엔드 신규 구현 필요** (§1.1) |
| **내 신청 목록·상세(마이페이지)** | ❌ 목(localStorage) | ✅ 구현·`main` 반영 완료(`b5f6140`) | **연동 가능** (§1.2) |
| **1:1 문의(Inquiry)** | ❌ 목(localStorage) | ❌ 없음 | **도메인 신규 구현** (§1.3) |
| **관리자 신청관리·통계** | ❌ 목(localStorage) | ❌ 없음 | **도메인 신규 구현** (§1.4) |
| **신청 취소** | ⚠️ 진입점만 | ✅ 구현·`main` 반영 완료(`b5f6140`) | **연동 가능** (§1.5) |
| 행사 회사/로고·관리자 전체목록 | ⚠️ 부분 | ⚠️ 필드/엔드포인트 없음 | **백엔드 필드·API 보강** (§1.6) |
| 공지 서버 검색 | ⚠️ 클라 검색 | ❌ keyword 파라미터 없음 | 필요 시 검색 파라미터 추가 (§1.7) |
| 후기 다중 이미지 | ⚠️ 단일로 축소 | 단일만 지원 | 정책 확정(단일 유지 vs 확장) (§1.8) |
| 회원정보 address 수정·학생증 schoolName | ⚠️ 화면엔 있음 | 저장 필드 없음 | 요구 확정 시 DTO/Entity 보강 (§1.9) |
| 카드 종류·디자인 카탈로그 | 정적(`cards.ts`) | 🟡 내부만 존재 | **STATIC 확정**(공개 API 신설 안 함) (§2.1) |
| 한국이름 조회(`nameResults.json`) | 정적 215KB 번들 | ❌ 없음 | 조회 API 필요 (§2.2) |
| 정적 마케팅(협력사/SNS/기념품/약관문/회사정보) | 정적 | ❌ 없음 | 선택 — CMS/설정 API (§3) |

범례: ✅ 완료 · ⚠️ 부분/혼재 · 🟡 내부·미커밋 존재 · ❌ 미구현

---

## 1. 프론트가 필요로 하나 백엔드에 없는 부분

### 1.1 일반 이메일 회원가입·로그인·계정 복구 — 없음 (BLOCKED)
- **프론트 사용처**: `pages/LoginPage`(이메일/비밀번호 + 데모 로그인), `pages/SignupPage`, `pages/AccountRecoveryPage`. 현재 이메일 로그인/가입은 `AuthContext`의 로컬 mock 세션으로만 동작.
- **백엔드 현황**: OAuth2(구글/네이버) + `POST /api/auth/terms`·`refresh`·`logout`만 존재. 이메일/비밀번호 계정 모델과 인증 API 자체가 없음.
- **필요 API**
  | 메서드/경로 | 용도 | 인증 |
  |---|---|---|
  | `POST /api/auth/signup` | 이메일 회원가입, 성공 시 토큰 쿠키 후 `/terms` | 없음 |
  | `POST /api/auth/login` | 이메일/비밀번호 로그인 | 없음 |
  | `POST /api/auth/email/check` | 이메일 중복 확인 | 없음 |
  | `POST /api/auth/recovery/id` | 이름·전화로 가입 이메일(마스킹) 안내 | 없음 |
  | `POST /api/auth/recovery/password/request`·`/confirm` | 재설정 토큰 발송·저장(만료·1회성) | 없음 |
  | `PATCH /api/users/me/password` | 비밀번호 변경 | USER |
- **정책**: 로그인 아이디=이메일(정규화 trim+소문자, DB UNIQUE), 비밀번호 단방향 해시, role은 서버 결정, 복구 응답은 계정 존재 비노출, **운영 빌드에서 데모 로그인 제거**.

### 1.2 내 신청 목록·상세(마이페이지) — ✅ `main` 반영 완료, 연동 가능
- **프론트 사용처**: `pages/MyPage` 제작 내역 — 현재 `data/adminMock.ts` localStorage(`applicantEmail === user.email` 필터).
- **백엔드 현황**: `MyApplicationController`(`GET /api/my/applications`, `GET /api/my/applications/{id}`) 구현 완료, `main`에 커밋·푸시됨(`b5f6140`, 2026-08-19). `FRONTEND_API_INTEGRATION_SPEC.md` §3.6 계약과 동일.
- **조치**: 이제 `MyPage` 제작 내역을 `data/adminMock.ts` localStorage 대신 이 API로 연동 가능. 응답 필드: 목록 `applicationId, applicationNumber, applicationType, cardTypeId, cardTypeName, totalQuantity, status, paymentStatus, createdAt` / 상세 `issueType, paymentGuidedAt, paymentDueAt, cancelled*, refundedAt, cardReadyAt, physicalDispatchedAt, photoRejectReason, applicant, receiver, memberCount`.

### 1.3 1:1 문의(Inquiry) — 도메인 전체 없음 (BLOCKED)
- **프론트 사용처**: `pages/InquiryPage`(작성), `pages/InquiryDetailPage`(상세), `pages/MyPage`(내 문의), `pages/AdminPage`(관리자 목록+답변). 저장소 `data/inquiries.ts`(`localStorage["customer-inquiries"]`).
- **데이터 계약**(`InquiryRecord`): `id, category, name, email, phone, title, content, createdAt, status("PENDING"|"COMPLETED"), answer?, answeredAt?`
- **필요 API**
  | 메서드/경로 | 용도 | 인증 |
  |---|---|---|
  | `POST /api/inquiries` | 문의 등록 | 선택(비회원 허용 여부 확정) |
  | `GET /api/my/inquiries`, `/{id}` | 내 문의 목록·상세 | 회원/소유자 |
  | `GET /api/admin/inquiries`, `/{id}` | 관리자 목록/상세 | 관리자 |
  | `POST /api/admin/inquiries/{id}/answer` | 답변 등록 → `COMPLETED` | 관리자 |

### 1.4 관리자 신청관리·통계 — 없음 (BLOCKED)
- **프론트 사용처**: `pages/AdminPage`(신청 목록·상태 변경, 통계 카드, 문의 답변). 저장소 `data/adminMock.ts`.
- **필요 API**: `GET /api/admin/applications`(+`/{id}`), 상태 전이 명령(`payment-guide/payment-confirm/start-review/photo-reject/approve-review/production-start/complete/refund-complete`), `GET /api/admin/stats`, 문의 관리(§1.3).
- **⚠️ status enum 불일치**: 프론트 `adminMock.ts`(옛 값 `SUBMITTED/CONSULTING/PAYMENT_PENDING/IN_PRODUCTION/COMPLETED/CANCELLED`)와 백엔드 실제 enum(`SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`, 결제상태 별도 `WAITING/CONFIRMED`)이 다름 → 연동 전 매핑 확정 필요.
- **인가**: 현재 프론트 `loginAsAdmin` 데모 버튼 의존 → 실제 `role=ADMIN` 서버 검증 필요.

### 1.5 신청 취소 — ✅ `main` 반영 완료, 연동 가능
- **프론트 사용처**: 마이페이지/모바일 카드에서 취소 진입점 필요.
- **백엔드 현황**: `POST /api/applications/{id}/cancel`이 `ApplicationController`에 구현 완료, `main`에 커밋·푸시됨(`b5f6140`, 2026-08-19). `FRONTEND_API_INTEGRATION_SPEC.md` §3.7 계약과 동일.
- **조치**: 이제 연동 가능. 가능 상태 `SUBMITTED/REVIEWING/PHOTO_REJECTED`, 이미 취소면 멱등 성공, 응답 `applicationId,status,paymentStatus,refundRequired,cancelledAt`.

### 1.6 행사(Event) — 회사/로고 필드·관리자 전체목록 없음 (PARTIAL)
- **프론트 사용처**: `pages/EventsPage` 협업 카드가 `company`/`logoUrl`로 로고 표시. 관리자 패널이 숨긴(`visible=false`) 글 재편집 필요.
- **백엔드 현황**: `Event`에 `company`/`logoUrl` 대응 필드 없음 → 협업 로고는 현재 카드라벨 텍스트로 대체 표시. 숨김 포함 관리자 목록(`GET /api/admin/events`)도 없음.
- **필요**: Event에 회사명·로고 필드(또는 로고 업로드) 추가, `GET /api/admin/events`(visible 무관) 추가, `PATCH`의 갤러리(`images`) 편집 지원.

### 1.7 공지 서버 검색 — 없음 (PARTIAL)
- **프론트 사용처**: `pages/NoticesPage` 제목/작성일 검색.
- **백엔드 현황**: `GET /api/boards`에 `keyword`/`searchType` 파라미터 없음 → 프론트가 `size=100`으로 받아 **클라이언트 검색** 중.
- **필요**: 데이터 증가 시 게시판 서버 검색·페이지네이션 파라미터.

### 1.8 후기 다중 이미지 — 정책 공백 (PARTIAL)
- **백엔드 현황**: 후기 1건당 이미지 1장만 저장(`imagePath`). 프론트 갤러리 UI는 현재 **단일 이미지로 축소**해 연동.
- **필요**: 다중 이미지 유지 여부 확정 → 유지 시 백엔드 스키마 확장.

### 1.9 회원정보 address 수정 · 학생증 schoolName — 저장 필드 없음 (PARTIAL)
- **회원정보**: `GET /api/users/me`는 `address`를 반환하지만 `PATCH /api/users/me`는 `name`,`phone`만 처리(주소 수정 경로 없음). → 프론트 `MyPage` 편집폼에서 **주소 입력을 제거**(오해 방지)해 둠. 주소 수정이 확정 요구면 Request/Entity 보강 필요.
- **학생증**: 화면의 `schoolName`을 저장·조회할 백엔드 필드가 없음(`FRONTEND_API_INTEGRATION_SPEC.md` §3.3). 프론트는 입력받되 신청 request에는 보내지 않음. 실제 발급 정보라면 DTO/Entity 추가 필요.

### 1.10 신청 조회 응답 `applicationType` — ✅ 백엔드 해결, 프론트 연동 가능
- **배경**: 스펙 §3.7은 사진 재업로드 시 **프론트가 ApplicationType에 따라 part를 분리**(개인 `photo` / 단체 `submitFile`)하라고 요구한다.
- **백엔드 현황**: `ApplicationLookupResponse.applicationType`(`INDIVIDUAL`/`GROUP`) 구현 완료.
- **남은 작업**: 프론트 `MobileCardPage`가 `applicationType`에 따라 개인 `photo`/단체 `submitFile` 파트와 안내문구를 분기한다.

### 1.11 신청 폼이 수집하나 백엔드가 저장하지 않는 입력 (프론트 유지 · 백엔드 보강)
프론트 화면에는 입력/표시가 있으나 백엔드 request DTO·도메인에 대응이 없어 값이 서버에 남지 않는 항목. **프론트 UI는 그대로 유지**하고 백엔드 보강 시 연결한다. 상세·조치는 `BACKEND_API_GAPS.md P1-4`.

| 프론트 입력 | 위치 | 백엔드 현황 |
|---|---|---|
| 입금자명 + 입금 확인/취소 | `StepComplete` | 결제·입금(Payment) 도메인 없음(입금 안내는 정적 계좌) |
| 학교명(schoolName) | `StepInfo`·`StepReview`(학생증) | member 필드 없음(§1.9와 동일) |
| 상담확인·유의사항 동의 | `StepType` | 신청 건별 동의 이력 저장 없음 |

> 단체 "신청 수량"은 백엔드가 엑셀 인원 수로 산정하는 정상 계약이라 프론트 입력을 제거함(응답 `totalQuantity` 사용) — 위 목록과 성격이 다름.

---

## 2. 정책상 정적 유지 또는 별도 조회 API

### 2.1 카드 종류·디자인 카탈로그 — STATIC 확정 (공개 API 신설 안 함)
- **프론트**: `pages/ApplyPage`·`pages/DesignPage`·`components/gallery`·`components/brand`가 `data/cards.ts` 정적 사용.
- **결정**: 공개 catalog API를 신설하지 않는다(`FRONTEND_API_INTEGRATION_SPEC.md` §1.2 `STATIC`). `CardType`은 백엔드 내부에만 존재하고, 프론트 문자열 enum ↔ `cardTypeId`(1~4) 매핑은 `cards.ts`의 공통 매퍼로 처리. 관리자가 카드종류/설명을 편집해야 하는 CMS 요구가 생기면 그때 재검토.

### 2.2 한국이름 조회(`nameResults.json`) — 조회 API 없음
- **프론트**: `components/home/ServiceCoreSection`이 `data/nameResults.json`(약 215KB)을 번들에 그대로 포함.
- **필요**: `GET /api/names/search?...`(조건 기반 조회)로 서버 이전, 또는 외부 작명 도구 링크아웃 정책과 정합화.

---

## 3. 정적 마케팅 데이터 (우선순위 낮음 · 선택 CMS)

배포 없이 운영자가 수정해야 할 때만 CMS/설정 API로 이관:

| 파일 | 소비 화면 | 성격 |
|---|---|---|
| `data/zodiac.ts` | Hero/MainDesigns/ZodiacIcon | 12간지 정적 |
| `data/partners.ts` | PartnersSection | 협력기관 로고 |
| `data/social.ts` | footer/SocialLinks | SNS 링크 |
| `data/merchandise.ts` | MerchandiseSection | 기념품 |
| `data/policies.ts` | footer/Footer | 약관/정책 문서(버전 관리 필요) |
| `config/company.ts` | 다수 | 회사 정보·계좌(공개범위 제한 필요) |
| `pages/EventsPage` PROGRAM 카드 | 행사 프로그램 소개 3종 | `managed-content:events` localStorage |

---

## 4. 목데이터 인벤토리 (`frontend/src/data/*`)

| 파일 | 저장 | 대체 방향 | 상태 |
|---|---|---|---|
| `reviews.ts` | (API 매퍼) | Review API | ✅ 연동 완료 |
| `adminMock.ts` | localStorage | §1.2 내 신청 + §1.4 관리자 | 동적·미구현 |
| `inquiries.ts` | localStorage | §1.3 Inquiry | 동적·미구현 |
| `eventFeedPosts.ts` | (API 매퍼) | Event API | ✅ 연동 완료(회사/로고 갭 §1.6) |
| `nameResults.json` | 정적 번들 215KB | §2.2 이름 조회 | 동적·미구현 |
| `cards.ts` | 정적 | §2.1 STATIC 확정 | 정적 유지 |
| `zodiac/partners/social/merchandise/policies.ts` | 정적 | §3 (선택) | 정적 |

`components/admin/ContentAdminPanel.tsx`의 `managed-content:events`(PROGRAM 카드)만 localStorage 잔존(§3). 공지/FAQ는 `BoardAdminPanel`, 행사는 `EventAdminPanel`로 실 API 연동됨.

---

## 5. 실 API + 목 병행(하이브리드) 정리 대상

실제 API를 호출하지만 목 저장소를 함께 써서, 관련 백엔드 완성 후 제거해야 하는 코드:

| 위치 | 현상 | 정리 조건 |
|---|---|---|
| `pages/ApplyPage` `saveLocalApplication` | `api.createApplication` 성공 후 `saveApplications`로 localStorage 미러 | §1.2/§1.4 완성 시 제거 |
| `pages/LookupPage` | `api.lookupApplication` + `loadApplications()` 교차 폴백 | 서버 조회 단일화 시 제거 |
| `features/auth/AuthContext` | `api.getMe`(`source:"api"`) + `loginAsUser/loginAsAdmin` 데모 세션 | §1.1 완성 시 데모 로그인 제거 |

---

## 6. 권장 진행 순서

1. **일반 이메일 인증(§1.1)** — 로그인/가입 mock 제거의 전제.
2. **내 신청 목록·상세(§1.2)** + **신청 취소(§1.5)** — ✅ 백엔드 커밋·푸시 완료, 바로 연동 가능.
3. **관리자 신청관리(§1.4)** — status enum 프론트/백 일치 확정 후.
4. **문의 도메인(§1.3)** — 신규 CRUD.
5. **행사 필드·공지 검색(§1.6/§1.7)**, **후기 다중이미지(§1.8)**, **회원 address·학생증 schoolName(§1.9)** — 정책 확정 후 보강.
6. **이름 조회(§2.2)** / 정적 마케팅(§3) 및 하이브리드 목 미러링(§5) 정리는 마지막.
