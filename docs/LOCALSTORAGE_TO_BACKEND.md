# localStorage → 백엔드 이전 필요 목록 (2026-08-25)

원칙: **사용자/업무 데이터는 브라우저 localStorage에 저장하지 않는다. 반드시 백엔드 DB에 저장하고 API로 주고받는다.**
이 문서는 프론트엔드 전체(`frontend/src`)를 순회해 localStorage/sessionStorage에 저장 중인 모든 항목을 찾아,
**백엔드로 옮겨야 하는 것**과 **그대로 둬도 되는 것**을 구분하고, 각 항목에 필요한 API·DB·프론트 변경을 정리한다.

- 관련: 관리자 영역은 [`specs/admin-dashboard/BACKEND_TODO.md`](./specs/admin-dashboard/BACKEND_TODO.md). (유출 위험 관점 정리 `FRONTEND_STORAGE_AUDIT.md`는 2026-08-25 본 문서로 통합·삭제됨 — 아래 §1 위험도 컬럼 참고.)
- 인증 토큰(access/refresh)은 이미 **HttpOnly 쿠키(서버)** 라 JS 저장소 대상 아님.
- 이 문서가 저장소(localStorage/sessionStorage) 데이터에 대한 **단일 소스**다.

---

## 1. 전체 인벤토리

> ✅ **2026-08-25 완료**: 아래 🔴 두 항목(신청·문의)은 백엔드 API 연결 + localStorage 제거 완료.
> `data/adminMock.ts`·`data/inquiries.ts` 삭제, 일반 로그인도 실제 `/api/auth/login`(실패 시 mock 폴백)으로 전환.
> (managed-content, auth-user 최소화는 미완 — §3 참고. **입금자명 서버 저장은 2026-08-25 완료 — §2.1.**)

| 키 | 저장소 | 파일 | 데이터 | 판정 |
|---|---|---|---|---|
| ~~`admin-applications`~~ | ✅ 제거됨 | `ApplyPage`→`POST /api/applications`, `MyPage`→`GET /api/my/applications` | 제출 신청 | ✅ **백엔드 연결 완료** |
| ~~`customer-inquiries`~~ | ✅ 제거됨 | `InquiryPage`→`POST /api/inquiries`, `MyPage`/`InquiryDetailPage`→`GET /api/my/inquiries` | 문의 | ✅ **백엔드 연결 완료** |
| `managed-content:*` (`events` 등) | localStorage | `data/eventFeedPosts.ts`, `components/admin/ContentAdminPanel.tsx`, `pages/EventsPage` | 관리자 편집 공개 콘텐츠(이벤트/피드) | 🟠 **백엔드 이전 권장** |
| `auth-user` | localStorage | `features/auth/AuthContext.tsx` | 로그인 사용자 {name,email,role,phone,address} | 🟠 **실제 로그인 전환 필요** |
| `application-draft` | sessionStorage | `features/apply/useApplicationDraft.ts` | 작성 중 신청서(PII, 파일 제외) | 🟢 유지 가능(세션·임시) |
| `last-application-lookup` | sessionStorage | `pages/MobileCardPage.tsx` | 조회 결과(마스킹 이름) | 🟢 유지 가능(세션·임시) |
| `site-language` | localStorage | `features/i18n/LanguageContext.tsx` | 언어 설정 ko/en | 🟢 유지 가능(UI 설정) |

> 참고: 관리자 작명 확정·선택이력(`admin:*`)은 이미 백엔드로 이전 완료(커밋 `b763cc9`).

---

## 2. ✅ 완료 — 신청·문의 백엔드 연결(localStorage 제거)

> **2026-08-25 재검증(코드 대조):** 아래 두 항목은 백엔드 API 연결 + 프론트 localStorage 제거가 **완료**됐다. 남은 백엔드 미구현은 §2.1의 `depositorName`(입금자명) 저장 **1건뿐**이다.

### 2.1 신청 내역 `admin-applications` — ✅ 완료 (`depositorName` 포함, 2026-08-25)
- **✅ 완료**: 프론트 `saveLocalApplication`/`loadApplications`·`data/adminMock.ts` **완전 제거**(grep 0건). 신청은 `POST /api/applications`(개인)·`/bulk`(단체)에만 저장, 마이페이지는 `GET /api/my/applications`(+`/{id}` 상세), 조회는 `POST /api/applications/lookup`만 사용. 관리자 조회 `GET /api/admin/applications`(+`/{id}`·`/members`)도 연결됨.
- **✅ `depositorName`(입금자명) 구현 완료(2026-08-25)**: `Application.depositorName` 필드(nullable) + `registerDepositorName()`(결제 확인 전 SUBMITTED·WAITING에만 허용) + `PATCH /api/applications/{id}/depositor`(본인 소유만). 완료 화면(`StepComplete`)에서 입력→`api.updateDepositor(applicationId, name)`로 저장, 응답(`MyApplicationDetailResponse`)에 포함돼 마이페이지 상세에 노출. 엔티티 규칙 준수(팩토리 인자 아님·명명 mutation·상태가드), E2E 6케이스(200/403/401/400/가드) 통과. DB 컬럼은 ddl-auto가 자동 추가.

### 2.2 1:1 문의 `customer-inquiries` — ✅ 완전 완료
- **✅ 완료**: `data/inquiries.ts` **삭제됨**. `InquiryPage`가 `POST /api/inquiries`(privacyConsent 포함), 마이페이지/상세가 `GET /api/my/inquiries`(+`/{id}`), 관리자 답변/상태(`InquiriesSection`)까지 전부 실 API. 백엔드 추가 작업 없음. (API 테스트 7/7 통과 — `docs/API_TEST_REPORT.md` §2.5)

---

## 3. 🟠 백엔드 이전 권장

### 3.1 관리자 편집 콘텐츠 `managed-content:*` — ⚠️ 부분 (백엔드는 있음, 프론트 레거시 경로 잔존)
- **현황(2026-08-25)**: 행사 **부스/협업 피드**는 이미 실 API 연결됨 — `EventsPage.tsx`가 `api.listEvents({type})`로 조회(`:30-31`), 관리자 편집은 `EventAdminPanel`이 `/api/admin/events` CRUD로 수행(API 테스트 §2.4 통과).
- **남은 것**: "이벤트 **프로그램**" 블럽만 아직 localStorage(`managed-content:events`, `ContentAdminPanel`, `EventsPage.tsx:25-26,42`). 이 특정 프로그램 콘텐츠는 전용 백엔드 API가 없다.
- **필요 작업(프론트 정리 위주)**: 이 프로그램 블럽을 boards(공지형) 재활용 또는 events로 흡수하거나, 레거시 `ContentAdminPanel`/`managed-content` 경로 **제거**. PII 아님, 우선순위 중.

### 3.2 로그인 상태 `auth-user` — ⚠️ 백엔드 미구현 아님(프론트 저장 최소화 선택)
- **현황**: 서버 인증 세션은 **HttpOnly 쿠키(백엔드)** 로 이미 존재하고 실 로그인도 동작한다. `AuthContext`의 `auth-user` localStorage는 **화면 표시용 캐시**(name/email/role/phone/address)일 뿐 백엔드 갭이 아니다.
- **필요 작업(선택)**: 표시에 불필요한 `phone/address`는 저장에서 제외하는 프론트 최소화. 백엔드 작업 없음.

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

- [x] **P0** 실제 로그인 세션 전환(§4) — `LoginPage`가 `POST /api/auth/login` 시도(실패 시 mock 폴백).
- [x] **P0** 문의(§2.2): `createInquiry`/`listMyInquiries`/`getMyInquiry` 추가 → InquiryPage/MyPage/InquiryDetailPage 전환 → `data/inquiries.ts` 삭제. (E2E: 접수 201 → 내 문의 표시 확인)
- [x] **P0** 신청(§2.1): `listMyApplications`/`getMyApplication` 추가 → MyPage를 `/api/my/applications`로 → `saveLocalApplication`/`data/adminMock.ts` 삭제 → LookupPage localStorage 매칭 제거.
- [x] **P0** 입금자명(`depositorName`) 서버 저장 — `PATCH /api/applications/{id}/depositor` 신설(본인 소유·결제 확인 전만), 완료 화면(`StepComplete`)에서 저장. 2026-08-25 완료.
- [ ] **P1** 관리자 콘텐츠(§3.1): `managed-content:*` 실제 API 이관 또는 제거.
- [ ] **P1** `auth-user`(§3.2): 실제 로그인 후 클라이언트 저장 최소화.
- [x] 유지: `application-draft`, `last-application-lookup`, `site-language`.

> 완료 기준: 프론트 코드에서 **사용자/업무 데이터를 쓰는 `localStorage.setItem` 호출이 0개**가 되고(§5의 UI 설정 제외),
> 해당 데이터가 전부 백엔드 API를 통해 DB에 저장·조회된다.
