# 프론트 ↔ 백엔드 API 갭 · 목데이터 전환 목록

> **상태: 이력 참고용.** 이 문서는 2026-08-14 당시 구현 상태를 기록한 자료로, 이후 My Application·Board·Event 등 구현 내용이 반영되지 않았다. 현재 백엔드 준비도와 프론트 개발용 실제 API 계약은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`를 사용한다. 프론트의 목데이터/localStorage 사용 자체는 결함으로 분류하지 않는다.

> 작성일: 2026-08-14 · 대상: `frontend/src` 전체 코드 기준
> 목적: **프론트가 실제로 필요로 하지만 백엔드에 API로 구현되어 있지 않은 부분**과, **프론트에 남아 있는 목(mock) 데이터 → 실제 API 전환 대상**을 한 문서로 정리한다.
> 근거: `frontend/src/services/api.ts`(실제 호출), `frontend/src/data/*`(목데이터), `backend/honor-citizen/.../api/*Controller.java`(실구현 엔드포인트) 상호 대조.

---

## 0. 한눈에 보기

| 기능 영역 | 프론트 현황 | 백엔드 현황 | 조치 |
|---|---|---|---|
| 인증(OAuth/세션/약관) | ✅ 실 API 연동 | ✅ 구현됨 | 데모 로컬 로그인만 제거 |
| 신청 생성/조회/사진재업로드/카드다운로드 | ⚠️ 실 API + 목 병행 | ✅ 구현됨 | 목 미러링 제거 |
| **후기(Review)** | ❌ 목 전용(localStorage) | ✅ **구현됨** | **프론트 연동만 하면 됨** (§2.1) |
| **내 신청 목록(마이페이지)** | ❌ 목(localStorage) | ❌ 없음 | **신규 API 필요** (§1.3) |
| **1:1 문의(Inquiry)** | ❌ 목(localStorage) | ❌ 없음 | **도메인 신규 구현** (§1.1) |
| **관리자(신청/문의 관리·통계)** | ❌ 목(localStorage) | ❌ 없음 | **도메인 신규 구현** (§1.2) |
| **공지/FAQ/이벤트(관리형 콘텐츠)** | ❌ 정적+localStorage | ❌ 없음 | **신규 API 필요** (§1.4) |
| **카드 종류·디자인 카탈로그** | ❌ 정적(`cards.ts`) | 🟡 도메인만 존재(공개 API 없음) | **공개 카탈로그 API 필요** (§1.5) |
| **한국이름 조회(`nameResults.json`)** | ❌ 215KB 정적 번들 | ❌ 없음 | **조회 API 필요** (§1.6) |
| 정적 마케팅(띠/협력사/SNS/기념품/약관문) | ❌ 정적 | ❌ 없음 | 선택 — CMS/설정 API (§3) |

범례: ✅ 완료 · ⚠️ 부분/혼재 · 🟡 내부만 존재 · ❌ 미구현/미연동

---

## 1. 백엔드 미구현 — 신규 API가 필요한 부분

### 1.1 1:1 문의(Inquiry) — 도메인 전체 없음
- **프론트 사용처**: `pages/InquiryPage`(작성), `pages/InquiryDetailPage`(상세), `pages/MyPage`(내 문의 목록), `pages/AdminPage`(관리자 목록+답변)
- **현재 저장소**: `data/inquiries.ts` — `localStorage["customer-inquiries"]`
- **데이터 계약**(`InquiryRecord`):
  `id, category, name, email, phone, title, content, createdAt, status("PENDING"|"COMPLETED"), answer?, answeredAt?`
- **필요 API**
  | 메서드/경로 | 용도 | 인증 |
  |---|---|---|
  | `POST /api/inquiries` | 문의 등록 | 선택(비회원 허용 여부 확정 필요) |
  | `GET /api/inquiries/{id}` | 문의 상세 | 작성자/관리자 |
  | `GET /api/inquiries/me` | 내 문의 목록 | 회원 |
  | `GET /api/admin/inquiries` | 관리자 목록(상태 필터·페이지네이션) | 관리자 |
  | `PATCH /api/admin/inquiries/{id}/answer` | 답변 등록 → `status=COMPLETED`, `answeredAt` 세팅 | 관리자 |

### 1.2 관리자(Admin) — 신청/문의 관리·통계 없음
- **프론트 사용처**: `pages/AdminPage` (신청 목록·상태 변경, 문의 목록·답변, 상단 통계 카드)
- **현재 저장소**: `data/adminMock.ts`(`localStorage["admin-applications"]`) + `data/inquiries.ts`
- **데이터 계약**(`AdminApplication`):
  `applicationNumber, applicantType("개인"|"법인·단체"), cardType, applicantName, phone, applicantEmail?, quantity, status, submittedAt`
- **필요 API**
  | 메서드/경로 | 용도 | 비고 |
  |---|---|---|
  | `GET /api/admin/applications` | 신청 목록(상태·유형 필터, 페이지네이션) | `PageResponse` 재사용 가능 |
  | `GET /api/admin/applications/{id}` | 신청 상세 | 구성원/파일 포함 |
  | `PATCH /api/admin/applications/{id}/status` | 상태 전이 | enum 확정 필요(아래) |
  | `GET /api/admin/stats` | 통계(전체/제작중/입금대기/발급완료) | `adminStats` 대체 |
  | (문의 관리) | §1.1 참조 | |
- **⚠️ status enum 불일치**: 프론트 `adminMock.ts`는 옛 값
  `SUBMITTED / CONSULTING / PAYMENT_PENDING / IN_PRODUCTION / COMPLETED / CANCELLED`.
  백엔드 확정 흐름(`PAYMENT_PENDING → RECEIVED → REVIEWING ↔ PHOTO_REJECTED → NAME_EDITING → PRODUCING → COMPLETED / CANCELLED`)과 다름 → **프론트/백엔드 enum 일치 필요.**
- **인가**: 관리자 라우팅 가드는 현재 프론트 `loginAsAdmin` 데모 버튼에만 의존 → 실제 `role=ADMIN` 서버 검증 필요.

### 1.3 내 신청 목록 — `GET /api/applications/me` 없음
- **프론트 사용처**: `pages/MyPage` — `loadApplications().filter(applicantEmail === user.email)`
- **백엔드 현황**: `ApplicationController`에는 생성/조회(lookup)/사진재업로드/카드다운로드만 있고 **"내 신청 내역 목록"이 없음.**
- **필요 API**: `GET /api/applications/me` (로그인 사용자의 신청 목록; 상태·신청번호·카드종류·수량·일자)

### 1.4 공지/FAQ/이벤트(관리형 콘텐츠) — 없음
- **프론트 사용처**: `pages/NoticesPage`, `pages/NoticeDetailPage`, `pages/FaqPage`, `pages/EventsPage` + `components/admin/ContentAdminPanel`
- **현재 저장소**: 기본값은 `pages/SupportPage`의 정적 배열(`notices`, `faqs`), 편집분은 `localStorage["managed-content:{notices|faqs|events}"]`
- **데이터 계약**(`ManagedContent`): `id, title, content, meta?`
- **필요 API**(관리자 CRUD + 공개 조회)
  | 경로 | 용도 |
  |---|---|
  | `GET /api/notices`, `GET /api/notices/{id}` | 공지 목록/상세(공개) |
  | `GET /api/faqs` | FAQ 목록(공개) |
  | `GET /api/events`, `GET /api/events/{id}` | 이벤트 목록/상세(공개) |
  | `POST|PATCH|DELETE /api/admin/{notices|faqs|events}[/{id}]` | 관리자 관리 |
  - 세 리소스를 일반화한 `content-type` 파라미터형 단일 API로 묶어도 무방.

### 1.5 카드 종류·디자인 카탈로그 — 공개 API 없음
- **프론트 사용처**: `pages/ApplyPage`(신청 시 디자인 선택), `pages/DesignPage`(갤러리), `components/gallery/CardCarousel`, `components/brand/SampleCard` — 모두 `data/cards.ts` 정적 사용
- **백엔드 현황**: Review 도메인이 `cardTypeId`(Long) + `CardTypeSummaryResponse`를 참조 → **CardType 도메인은 내부에 존재하나 프론트용 공개 카탈로그 엔드포인트가 없음.**
- **필요 API**
  | 경로 | 용도 |
  |---|---|
  | `GET /api/card-types` | 카드 종류 목록(명예한국인/명예시민/학생/방문) |
  | `GET /api/card-designs` | 디자인 목록(종류별 앞/뒷면 이미지) |
  | `POST|PATCH /api/admin/card-types`, `.../card-designs` | 관리자 CRUD |
- **부수 효과**: 이게 생기면 후기(§2.1)의 `cardTypeId ↔ 프론트 문자열 enum` 매핑도 서버 소스로 일원화 가능.

### 1.6 한국이름 조회(`nameResults.json`) — API 없음
- **프론트 사용처**: `components/home/ServiceCoreSection` — `data/nameResults.json`(약 215KB) 번들 임포트
- **문제**: 대용량 데이터가 클라이언트 번들에 그대로 포함됨. 작명/이름 검색은 서버 조회여야 함.
- **필요 API**: `GET /api/names/search?...`(생년/조건 기반 조회) 또는 확정된 외부 작명 도구 링크아웃 정책과의 정합화.

---

## 2. 백엔드는 있으나 프론트가 목을 쓰는 부분 (연동만 하면 됨)

### 2.1 후기(Review) — 백엔드 완비, 프론트 미연동
- **백엔드 현황**: `ReviewController` 완비
  `POST /api/reviews` · `GET /api/reviews`(목록, `PageResponse`) · `GET /api/reviews/{id}` · `PATCH /api/reviews/{id}` · `DELETE /api/reviews/{id}` + 이미지 검증(`ReviewImageValidator`).
- **프론트 현황**: `services/api.ts`에 **review 메서드가 하나도 없음.** 4개 화면이 전부 `data/reviews.ts`의 `loadReviews/saveReviews/findReview` = `localStorage["review-posts"]` 사용:
  `pages/ReviewsPage`, `pages/ReviewDetailPage`, `pages/ReviewEditorPage`, `pages/MyPage`.
- **⚠️ 계약 매핑 필요** (프론트 `ReviewPost` ↔ 백엔드 DTO)
  | 프론트(`ReviewPost`) | 백엔드 | 매핑 |
  |---|---|---|
  | `author` | `authorName` | 이름만 상이 |
  | `content`, `title` | `content`, `title` | 동일 |
  | `cardType`(문자열 `"honorary-korean"` 등) | `cardTypeId`(Long) + `CardTypeSummaryResponse` | **문자열 enum ↔ id 변환 필요**(§1.5 카탈로그 연계) |
  | `applicantType`(`"personal"|"organization"`) | `applicationType`(enum) | 값 매핑 필요 |
  | `imageUrl` | `imageUrl`(+ 업데이트 시 `removeImage`) | 생성은 파일 업로드 방식 확인 필요 |
  | — | `canEdit`, `canDelete`, `next`(이전/다음 글) | 프론트 신규 반영 |
- **조치**
  1. `api.ts`에 `listReviews / getReview / createReview / updateReview / deleteReview` 추가(생성/수정은 이미지 포함 → `FormData`).
  2. 4개 화면을 `loadReviews/saveReviews` → API 호출로 교체.
  3. 카드종류 매핑 유틸(문자열 ↔ `cardTypeId`) 도입, `getReviewImageUrl`의 레거시 이미지 매핑 정리.

---

## 3. 정적 마케팅 데이터 (우선순위 낮음 · 선택)

DB/운영 변경 빈도가 낮아 정적으로 둬도 무방하나, "모든 목데이터를 API로" 기준이면 설정/CMS API로 이관 대상:

| 파일 | 소비 화면 | 성격 |
|---|---|---|
| `data/zodiac.ts` | `HeroSection`, `MainDesignsSection`, `ZodiacIcon` | 12간지 정적 |
| `data/partners.ts` | `PartnersSection` | 협력기관 로고 |
| `data/social.ts` | `footer/SocialLinks` | SNS 링크 |
| `data/merchandise.ts` | `MerchandiseSection` | 기념품 소개 |
| `data/policies.ts` | `footer/Footer` | 약관/정책 문서 |
| `config/company.ts` | 다수 | 회사 정보 상수 |

---

## 4. 목데이터 인벤토리 (`frontend/src/data/*`)

| 파일 | 저장 방식 | 대체 방향 | 분류 |
|---|---|---|---|
| `adminMock.ts` | localStorage | §1.2 Admin API + §1.3 내 신청 목록 | 동적 |
| `inquiries.ts` | localStorage | §1.1 Inquiry API | 동적 |
| `reviews.ts` | localStorage | §2.1 Review API(백엔드 이미 존재) | 동적 |
| `nameResults.json` | 정적 번들(215KB) | §1.6 이름 조회 API | 동적 |
| `cards.ts` | 정적 | §1.5 카드 카탈로그 API | 준정적 |
| `zodiac/partners/social/merchandise/policies.ts` | 정적 | §3 (선택) | 정적 |

추가로 `components/admin/ContentAdminPanel.tsx`의 `managed-content:*` localStorage → §1.4.

---

## 5. 실 API + 목 병행(하이브리드) 정리 대상

실제 API를 이미 호출하지만 **목 저장소를 함께 쓰고 있어** 백엔드 API 완성 후 제거해야 하는 코드:

| 위치 | 현상 | 정리 방향 |
|---|---|---|
| `pages/ApplyPage` L67–68, `saveLocalApplication` | `api.createApplication` 성공 후 `saveApplications`로 localStorage에도 미러 | §1.2/§1.3 완성 시 미러 제거 |
| `pages/LookupPage` L123 | `api.lookupApplication` + `loadApplications()` 교차 확인 | 서버 조회 단일화 |
| `features/auth/AuthContext` | `api.getMe`(`source:"api"`) + `loginAsUser/loginAsAdmin` 데모 로컬 세션(`source:"local"`) | 데모 로그인 제거, 서버 세션 단일화 |

---

## 6. 권장 진행 순서

1. **후기 연동(§2.1)** — 백엔드가 이미 있어 즉시 착수 가능. `api.ts` 메서드 추가 + 화면 4개 교체.
2. **카드 카탈로그 공개 API(§1.5)** — 후기 `cardTypeId` 매핑과 신청/디자인 화면이 동시에 의존.
3. **내 신청 목록(§1.3)** + **관리자 신청관리(§1.2)** — status enum 프론트/백 일치부터.
4. **문의 도메인(§1.1)** — 신규 CRUD.
5. **관리형 콘텐츠(§1.4)** / **이름 조회(§1.6)**.
6. 하이브리드 목 미러링(§5) 일괄 제거 → 정적 마케팅(§3)은 마지막.
