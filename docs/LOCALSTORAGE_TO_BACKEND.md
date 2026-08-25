# localStorage → 백엔드 이전 필요 목록 (2026-08-25)

원칙: **사용자/업무 데이터는 브라우저 localStorage에 저장하지 않는다. 반드시 백엔드 DB에 저장하고 API로 주고받는다.**
이 문서는 프론트엔드 전체(`frontend/src`)를 순회해 localStorage/sessionStorage에 저장 중인 모든 항목을 찾아,
**백엔드로 옮겨야 하는 것**과 **그대로 둬도 되는 것**을 구분하고, 각 항목에 필요한 API·DB·프론트 변경을 정리한다.

- 관련: 유출 위험 관점 정리는 [`FRONTEND_STORAGE_AUDIT.md`](./FRONTEND_STORAGE_AUDIT.md), 관리자 영역은 [`specs/admin-dashboard/BACKEND_TODO.md`](./specs/admin-dashboard/BACKEND_TODO.md).
- 인증 토큰(access/refresh)은 이미 **HttpOnly 쿠키(서버)** 라 JS 저장소 대상 아님.

---

## 1. 전체 인벤토리

| 키 | 저장소 | 파일 | 데이터 | 판정 |
|---|---|---|---|---|
| `admin-applications` | localStorage | `data/adminMock.ts`, `pages/ApplyPage`, `pages/MyPage`, `pages/LookupPage` | 제출 신청 전체 PII(+ownerEmail·depositorName) | 🔴 **백엔드 필수** |
| `customer-inquiries` | localStorage | `data/inquiries.ts`, `pages/InquiryPage`, `pages/MyPage`, `pages/InquiryDetailPage` | 문의자 이름·이메일·전화·내용·답변 | 🔴 **백엔드 필수** |
| `managed-content:*` (`events` 등) | localStorage | `data/eventFeedPosts.ts`, `components/admin/ContentAdminPanel.tsx`, `pages/EventsPage` | 관리자 편집 공개 콘텐츠(이벤트/피드) | 🟠 **백엔드 이전 권장** |
| `auth-user` | localStorage | `features/auth/AuthContext.tsx` | 로그인 사용자 {name,email,role,phone,address} | 🟠 **실제 로그인 전환 필요** |
| `application-draft` | sessionStorage | `features/apply/useApplicationDraft.ts` | 작성 중 신청서(PII, 파일 제외) | 🟢 유지 가능(세션·임시) |
| `last-application-lookup` | sessionStorage | `pages/MobileCardPage.tsx` | 조회 결과(마스킹 이름) | 🟢 유지 가능(세션·임시) |
| `site-language` | localStorage | `features/i18n/LanguageContext.tsx` | 언어 설정 ko/en | 🟢 유지 가능(UI 설정) |

> 참고: 관리자 작명 확정·선택이력(`admin:*`)은 이미 백엔드로 이전 완료(커밋 `b763cc9`).

---

## 2. 🔴 백엔드 필수 — localStorage 금지

### 2.1 신청 내역 `admin-applications`
- **현재(잘못)**: 제출 시 `pages/ApplyPage/ApplyPage.tsx`의 `saveLocalApplication`이 신청 전체(PII)를 localStorage에 저장.
  마이페이지(`pages/MyPage`)·조회(`pages/LookupPage`)가 localStorage에서 읽음.
  - 문제: 실제 신청은 `POST /api/applications`로도 보내지만(로그인 API 세션일 때만), **localStorage에 중복·영구 저장**되어 유출.
    또한 일반(mock 로그인) 사용자는 서버에 안 가고 **localStorage에만** 남음.
- **백엔드 현황(이미 존재)**:
  - `POST /api/applications`(개인), `POST /api/applications/bulk`(단체) — 생성
  - `GET /api/my/applications`, `GET /api/my/applications/{id}` — 내 신청 조회
  - `GET /api/admin/applications`, `.../{id}`, `.../{id}/members` — 관리자
- **필요 작업**:
  1. (신규 필드) `POST /api/applications` 요청/엔티티에 **`depositorName`(입금자명)** 추가 — 현재 백엔드에 필드 없음.
  2. 프론트 `saveLocalApplication`/`loadApplications`(및 `data/adminMock.ts`) **제거**.
  3. 마이페이지 "제작 내역"을 **`GET /api/my/applications`** 로 교체(현재 localStorage 필터).
  4. 조회(`LookupPage`)의 localStorage 매칭 제거, `POST /api/applications/lookup`만 사용.
- **DB**: 기존 `applications`/`applicants`/`application_members`/`receivers` 재사용. depositorName 컬럼만 추가.
- **선행**: §4(실제 로그인 세션) — `/api/my/*`는 인증 세션 필요.

### 2.2 1:1 문의 `customer-inquiries`
- **현재(잘못)**: `pages/InquiryPage`가 제출을 **localStorage에만** 저장(`data/inquiries.ts`). 백엔드로 전송 안 함.
  마이페이지·상세가 localStorage에서 읽음. → 문의가 실제 관리자에게 전달되지 않고 브라우저에만 영구 잔존.
- **백엔드 현황(이미 존재)**:
  - `POST /api/inquiries` — 문의 생성(공개)
  - `GET /api/my/inquiries`, `GET /api/my/inquiries/{id}` — 내 문의
  - `GET /api/admin/inquiries`, 답변/상태 — 관리자(대시보드 이미 연결됨)
- **필요 작업**:
  1. 프론트 `api.ts`에 **공개 `createInquiry`**, **`listMyInquiries`/`getMyInquiry`** 클라이언트 함수 추가(백엔드는 이미 있음).
  2. `InquiryPage` 제출을 `POST /api/inquiries`로 전환, `saveInquiries` **제거**.
  3. 마이페이지 "문의 내역"을 `GET /api/my/inquiries`로 교체.
  4. `data/inquiries.ts` **제거**.
- **DB**: 기존 `inquiries` 테이블 재사용(신규 불필요).
- **선행**: §4(실제 로그인 세션) — `/api/my/inquiries` 및 문의 소유자 매칭.

---

## 3. 🟠 백엔드 이전 권장

### 3.1 관리자 편집 콘텐츠 `managed-content:*`
- **현재**: `pages/EventsPage`가 이벤트 프로그램/피드 글을 localStorage(`managed-content:events` 등)에 저장/편집(`ContentAdminPanel`, `data/eventFeedPosts.ts`).
- **문제**: PII는 아니지만 **관리자 편집 결과가 그 브라우저에만** 남아 다른 사용자/기기엔 반영 안 됨(실데이터 아님).
- **백엔드 현황**: 실제 행사·게시판 CRUD API 존재(`/api/admin/events`, `/api/admin/boards`). 이 레거시 로컬 편집은 그와 **별개**.
- **필요 작업**: 실제 이벤트/게시판 API로 이관하거나, 레거시 `ContentAdminPanel`/`managed-content` 경로 **제거**. (우선순위 중)

### 3.2 로그인 상태 `auth-user`
- **현재**: mock 로그인이라 사용자 PII를 localStorage에 저장(`AuthContext`).
- **필요 작업**: §4 실제 로그인 전환 시, 클라이언트엔 표시용 최소 정보만 두고 세션은 서버(HttpOnly 쿠키) 기준. `phone/address`는 저장 제외.

---

## 4. 공통 선행 과제 — 실제 로그인 세션

위 2.1·2.2의 `/api/my/*`(내 신청·내 문의)는 **서버 인증 세션**이 있어야 동작한다.
현재 일반 사용자는 **mock 로그인**(`LoginPage`가 아무 이메일로 클라이언트 상태만 세팅)이라 서버 세션이 없어 `/api/my/*`가 401이 난다.
- 백엔드는 `POST /api/auth/login`(이메일+비밀번호) 존재.
- 프론트 일반 로그인을 실제 API 로그인으로 전환해야 §2 이전이 실효를 가진다(임시 admin 하드코딩과 별개 — `TEMP_ADMIN_LOGIN.md`).

---

## 5. 🟢 유지 가능 (백엔드 불필요)

- `application-draft` (**sessionStorage**): 작성 중 신청서 임시 상태. 세션 한정 + 제출 시 clear + 파일 미저장. 서버 저장 불필요.
- `last-application-lookup` (**sessionStorage**): 조회 결과 화면 전달용. 이름 마스킹, 세션 한정. (조회 자체는 이미 `POST /api/applications/lookup`.)
- `site-language` (localStorage): 언어 설정. 순수 UI 환경설정이라 로컬 보관 적절.

---

## 6. 우선순위 · 체크리스트

- [ ] **P0** 실제 로그인 세션 전환(§4) — `/api/my/*` 전제.
- [ ] **P0** 문의(§2.2): `api.ts` createInquiry/listMyInquiries 추가 → InquiryPage/MyPage 전환 → `data/inquiries.ts` 제거.
- [ ] **P0** 신청(§2.1): `depositorName` 백엔드 필드 추가 → MyPage를 `/api/my/applications`로 → `saveLocalApplication`/`data/adminMock.ts` 제거 → LookupPage localStorage 매칭 제거.
- [ ] **P1** 관리자 콘텐츠(§3.1): `managed-content:*` 실제 API 이관 또는 제거.
- [ ] **P1** `auth-user`(§3.2): 실제 로그인 후 클라이언트 저장 최소화.
- [ ] 유지: `application-draft`, `last-application-lookup`, `site-language`.

> 완료 기준: 프론트 코드에서 **사용자/업무 데이터를 쓰는 `localStorage.setItem` 호출이 0개**가 되고(§5의 UI 설정 제외),
> 해당 데이터가 전부 백엔드 API를 통해 DB에 저장·조회된다.
