# 프론트 ↔ 백엔드 API 갭 · 목데이터 전환 목록

> **갱신: 2026-08-20(5차).** 회원정보 `address` 수정 정책이 같은 날 두 번 뒤집혔다 — (4차) "이름·전화번호만" → "address도 수정 가능"으로 바뀌었다가, (5차, 이번 갱신) **다시 "이름·전화번호만"으로 최종 확정**됐다(백엔드 코드도 원복 완료). §1.9(a)는 다시 "갭 아님"이며, 추가로 마이페이지 "내 정보" 표시 스펙도 확정됨 — 조회는 이름·전화번호·이메일만(회원 유형 표시 제거), 수정은 이름·전화번호만. `docs/api/user.md` API 5도 함께 원복 반영. 마이페이지 "제작 내역"이 실 API 미연동으로 빈 목록만 뜨는 문제(§1.2)도 이번에 코드 근거와 함께 상세화됨. 3차 갱신 내용(코드 재대조로 `schoolName` 미연동 발견, §1.9 전면 정정)은 그대로 유지. 로그인/이메일중복확인/비밀번호변경(§1.1-b) 오탈 정정은 2026-08-19(2차)에 이미 반영됨. 프론트 연동 계약 종합은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13), 백엔드 API 상세는 `docs/api/auth.md`(API 4~6), 백엔드 미구현 상세는 `docs/BACKEND_API_GAPS.md`와 함께 본다. 프론트의 목데이터/localStorage 사용 자체는 결함이 아니라, 백엔드가 준비된 화면부터 순차 교체하는 방식이다.

> 대상: `frontend/src` 전체 · 근거: `services/api.ts`(실제 호출) ↔ `backend/honor-citizen/.../api/*Controller.java`(실구현) 상호 대조(현재 워킹 트리·`main` 기준).

---

## 0. 한눈에 보기

| 기능 영역 | 프론트 | 백엔드 | 상태 |
|---|---|---|---|
| OAuth 로그인·약관·세션·회원정보 | ✅ 실 API | ✅ 구현 | **연동 완료** (데모 로컬 로그인만 운영빌드에서 제거 필요) |
| 신청 생성(개인/단체) | ✅ 실 API | ✅ 구현 | **🔴 두 가지 문제로 연동 깨짐**: ① 학생증만 — `schoolName`을 프론트가 요청에 안 보내서 학생증 신청이 400으로 실패 (§1.9). ② **개인 신청 전체** — 국적 입력란 placeholder("대한민국")가 백엔드가 요구하는 ISO 코드(`KR` 등)와 안 맞아서 placeholder대로 입력하면 무조건 400 (§1.12) |
| 신청 조회/카드다운로드 | ✅ 실 API | ✅ 준비 | 조회 응답에 `applicationType`이 포함돼 개인 `photo`/단체 `submitFile` 재제출 분기 가능. 단체 재제출 UI 연결만 남음. 카드다운로드는 소유자 로그인 전용(비로그인 조회는 데모 폴백) |
| 후기(Review) CRUD + 내 후기 | ✅ 실 API | ✅ 구현 | **연동 완료** |
| 공지/FAQ(Board) | ✅ 실 API | ✅ 구현 | **연동 완료** |
| 행사(Event) | ✅ 실 API | ✅ 구현 | **연동 완료** (`company`/`logoUrl` 필드 갭 §1.6) |
| **일반 이메일 회원가입(인증 포함)** | ❌ 목/로컬 | ✅ 구현·`main` 반영 완료(`bc7d7ce`) | **프론트 신규 구현 필요**(인증코드 인라인 입력 UI) (§1.1) |
| **일반 이메일 로그인·이메일 중복확인·비밀번호 변경** | ❌ 목/로컬 | ✅ 구현·`main` 반영 완료 | **프론트 신규 구현 필요**(연동만 하면 됨, 백엔드 작업 없음) (§1.1) |
| **계정 복구(아이디/비밀번호 찾기)** | ⚠️ 요청 단계만 | ❌ 없음 | **백엔드 신규 구현 필요**(비번재설정 화면 UX는 결정됨) (§1.1) |
| **내 신청 목록·상세(마이페이지)** | ❌ 목(localStorage) | ✅ 구현·`main` 반영 완료(`b5f6140`) | **🔴 연동 안 돼 있어 실사용 불가**("제작 신청 내역이 없습니다" 표시) — API 실 호출로 교체 필요, 상태 라벨·날짜 포맷도 같이 손봐야 함 (§1.2) |
| **1:1 문의(Inquiry)** | ❌ 목(localStorage) | ✅ 구현·`main` 반영 완료 | **연동 가능(단, `privacyConsent` 필드 프론트 추가 전송 필요)** (§1.3) |
| **관리자 신청관리·통계** | ❌ 목(localStorage) | ❌ 없음 | **도메인 신규 구현** (§1.4) |
| **신청 취소** | ⚠️ 진입점만 | ✅ 구현·`main` 반영 완료(`b5f6140`) | **연동 가능** (§1.5) |
| 행사 회사/로고·관리자 전체목록 | ⚠️ 부분 | ⚠️ 필드/엔드포인트 없음 | **백엔드 필드·API 보강** (§1.6) |
| 공지 서버 검색 | ⚠️ 클라 검색 | ❌ keyword 파라미터 없음 | 필요 시 검색 파라미터 추가 (§1.7) |
| 후기 다중 이미지 | ⚠️ 단일로 축소 | 단일만 지원 | 정책 확정(단일 유지 vs 확장) (§1.8) |
| 회원정보 address 수정 | ⚠️ 화면엔 없음(정책상 제거된 상태) | ❌ 미지원(확정 정책) | **갭 아님** — 조회는 이름·전화번호·이메일만, 수정도 이름·전화번호만(§1.9-a) |
| **학생증 schoolName** | 🔴 화면엔 있으나 요청에 미포함 | ✅ 필수 필드로 구현 완료(2026-08-19) | **프론트 연동 시급** — 안 고치면 학생증 신청 전부 400 (§1.9) |
| 카드 종류·디자인 카탈로그 | 정적(`cards.ts`) | 🟡 내부만 존재 | **STATIC 확정**(공개 API 신설 안 함) (§2.1) |
| 한국이름 조회(`nameResults.json`) | 정적 215KB 번들 | ❌ 없음 | 조회 API 필요 (§2.2) |
| 정적 마케팅(협력사/SNS/기념품/약관문/회사정보) | 정적 | ❌ 없음 | 선택 — CMS/설정 API (§3) |

범례: ✅ 완료 · ⚠️ 부분/혼재 · 🟡 내부·미커밋 존재 · ❌ 미구현

---

## 1. 프론트가 필요로 하나 백엔드에 없는 부분

### 1.1 일반 이메일 회원가입·로그인·계정 복구

#### (a) 회원가입(이메일 인증 포함) — ✅ 백엔드 구현 완료(2026-08-19), **프론트 신규 구현 필요**
- **백엔드 현황**: 3개 API 전부 구현·`main` 커밋·푸시 완료 — `POST /api/auth/signup/email-verification/request`(인증코드 발송), `POST /api/auth/signup/email-verification/confirm`(코드 확인 → `signupToken` 발급), `POST /api/auth/signup`(가입 완료, `signupToken`+`email`+`password`+`name`+`phone` 필수). 상세 계약·요청/응답 예시는 `docs/api/auth.md` API 4~6, `docs/FRONTEND_API_INTEGRATION_SPEC.md` §3.13.
- **프론트 사용처**: `pages/SignupPage` — 현재 이름/이메일/비밀번호/전화번호를 받는 단일 폼이며, 제출 시 실제 API를 호출하지 않고 `AuthContext.login()`으로 로컬 mock 세션만 만든다.
- **프론트가 새로 만들어야 하는 것**: 이메일 인증 코드 입력 UI가 화면에 전혀 없다. **UX 결정(2026-08-19)**: 별도 페이지/스텝으로 분리하지 않고 `SignupPage.tsx` 폼 안에 **인라인**으로 넣는다 — 이메일 입력 후 코드 요청 트리거(버튼) → 같은 화면에 코드 입력 필드 노출 → 확인 성공 시 `signupToken` 확보 → 이어서 비밀번호·이름·전화번호까지 채운 뒤 최종 회원가입 제출.
- **조치**: `services/api.ts`에 3개 API 바인딩 추가(타입 포함) + `SignupPage.tsx`에 인라인 인증코드 입력 섹션 신규 구현 + 실제 제출 로직을 mock에서 실 API 호출로 교체. 재전송 대기(60초)/횟수제한(429)/코드불일치(`INVALID_VERIFICATION_CODE`, 남은 시도 횟수 비노출)/`signupToken` 만료(`INVALID_SIGNUP_TOKEN`) 에러 메시지 처리 필요.
- **⚠️ 비밀번호 검증 규칙 불일치(2026-08-19)**: 백엔드 확정 정책은 최소 8자·최대 72자·복잡도 규칙 없음이지만, 현재 `SignupPage.tsx`는 여전히 "8~64자 + 영문/숫자/특수문자 조합 필수"로 더 엄격하게 검증한다(한 번 완화했다가 "프론트는 백엔드 세션에서 수정하지 않는다"는 방침에 따라 되돌림). 서버가 최종 검증을 하므로 저장 자체엔 문제없지만, 72자 이상 비밀번호나 복잡도 조합을 안 채운 8자 이상 비밀번호는 프론트 자체 검증에서 먼저 막힌다. 프론트 담당자가 실 연동 시 이 규칙도 8~72자·복잡도 규칙 없음으로 맞출지 판단 필요.

#### (b) 로그인·이메일 중복확인·비밀번호 변경 — ✅ 백엔드 구현 완료, **프론트 신규 구현 필요** (2026-08-19 정정 — 이전 버전은 이 3개 API를 "미구현"으로 잘못 기재했었음, 실제로는 AUTH-1~6에서 이미 구현·커밋 완료된 상태였다)
- **백엔드 현황**: 3개 API 전부 구현·`main` 커밋·푸시 완료 — `POST /api/auth/login`(이메일/비밀번호, `AuthController.java:87`), `POST /api/auth/email/check`(이메일 중복 확인, `AuthController.java:99`), `PATCH /api/users/me/password`(로그인 사용자 비밀번호 변경, `UserController.java:43`).
- **프론트 사용처**: `pages/LoginPage`가 실 API 대신 데모 로그인만 쓰고, 이메일/비밀번호 로그인 폼 제출 시 아직 이 API를 호출하지 않는다.
- **조치**: `services/api.ts`에 3개 API 바인딩 추가 + `LoginPage.tsx` 제출 로직을 mock에서 실 API 호출로 교체. 백엔드 신규 작업은 필요 없음.
- **정책**: 로그인 아이디=이메일(정규화 trim+소문자, DB UNIQUE — AUTH-1), 비밀번호 단방향 해시(BCrypt, 8~72자, 복잡도 규칙 없음 — AUTH-4), role은 서버 결정, **운영 빌드에서 데모 로그인 제거**.

#### (c) 계정 복구(아이디/비밀번호 찾기) — 여전히 없음 (BLOCKED)
- **프론트 사용처**: `pages/AccountRecoveryPage`(아이디 찾기/비밀번호 찾기 탭 — **요청 단계까지만** 구현돼 있고, 코드·토큰 검증 후 실제로 새 비밀번호를 저장하는 화면은 없음).
- **백엔드 현황**: 아래 API는 전부 미구현. `POST /api/auth/{login,email/check}`·`PATCH /api/users/me/password`(위 (b))와는 별개다 — 혼동해서 함께 "구현 완료"로 보지 말 것.
- **필요 API**
  | 메서드/경로 | 용도 | 인증 |
  |---|---|---|
  | `POST /api/auth/recovery/id` | 이름·전화로 가입 이메일(마스킹) 안내 | 없음 |
  | `POST /api/auth/recovery/password/request` | 이메일·전화로 재설정 코드/토큰 발송 | 없음 |
  | `POST /api/auth/recovery/password/confirm` | 코드/토큰 검증 + 새 비밀번호 저장(1회 호출) | 없음 |
- **프론트가 새로 만들어야 하는 것**: 비밀번호 재설정 "확인" 화면 자체가 없다(`AccountRecoveryPage`는 요청 단계로 끝남). **UX 결정(2026-08-19)**: 코드 확인과 새 비밀번호 입력을 별도 스텝으로 나누지 않고 **한 화면에 통합**한다(스텝 최소화) — 코드/토큰 입력란과 새 비밀번호(+확인) 입력란을 함께 보여주고 "재설정" 버튼 한 번으로 제출.
- **⚠️ 이 UX 결정이 백엔드 API 설계에 미치는 영향**: 화면이 코드와 새 비밀번호를 한 번에 제출하므로, `POST /api/auth/recovery/password/confirm`도 "코드 검증"과 "비밀번호 저장"을 별도 API 2개로 쪼개지 말고 **하나의 요청**(`{ email, code, newPassword }`)으로 설계해야 한다 — AUTH-4의 `signupToken`+`password`를 한 번에 받는 패턴과 동일한 방향. 백엔드 구현 착수 전에 이 계약으로 맞춰야 화면과 어긋나지 않는다.
- **정책**: 복구 응답은 계정 존재 비노출.

### 1.2 내 신청 목록·상세(마이페이지) — ✅ `main` 반영 완료, 연동 가능
- **🔴 현재 증상(2026-08-20 재확인)**: 실제로 신청을 제출해도 마이페이지 "제작 내역"에 "제작 신청 내역이 없습니다"만 뜬다. 원인은 이 절에 이미 적힌 그대로 — `MyPage.tsx`가 실 API를 아예 호출하지 않고 `data/adminMock.ts`의 `loadApplications()`(localStorage)만 읽기 때문(`MyPage.tsx:39` `loadApplications().filter((a) => a.applicantEmail === user.email)`). 이 localStorage 항목은 `ApplyPage.tsx`의 `saveLocalApplication()`(제출 성공 시 클라이언트에서만 mirror, `ApplyPage.tsx:60-62,108,121`)이 채우므로 (a) 제출과 조회가 같은 브라우저/오리진이어야 하고 (b) 신청서에 입력한 `applicant.email`이 로그인 계정 `user.email`과 정확히 일치해야 하고 (c) localStorage가 지워지지 않아야 보인다 — 서버에 실제로 저장된 신청과는 무관하게 뜨거나 안 뜬다. `api.ts`엔 이 엔드포인트를 부르는 함수 자체가 없다(`listMyReviews`류 패턴 없음).
- **프론트 사용처**: `pages/MyPage` 제작 내역 — 현재 `data/adminMock.ts` localStorage(`applicantEmail === user.email` 필터).
- **백엔드 현황**: `MyApplicationController`(`GET /api/my/applications`, `GET /api/my/applications/{id}`) 구현 완료, `main`에 커밋·푸시됨(`b5f6140`, 2026-08-19). `FRONTEND_API_INTEGRATION_SPEC.md` §3.6 계약과 동일. **백엔드 쪽엔 추가 작업 없음.**
- **조치(프론트 전용, 변경 범위)**:
  1. `services/api.ts`에 `listMyApplications({ status?, page?, size? })`(`GET /api/my/applications`) 추가 — `listMyReviews`(`api.ts:106`)와 동일 패턴. 상세가 필요하면 `getMyApplicationDetail(applicationId)`(`GET /api/my/applications/{id}`)도 추가.
  2. `MyPage.tsx`: `loadApplications().filter(...)`(39행) 제거, `myReviews`와 동일한 `useEffect(user.source === "api"일 때만 호출)` 패턴으로 교체.
  3. **⚠️ 상태 라벨 매핑 재작성 필요**: 현재 렌더링(`MyPage.tsx:71`)이 쓰는 `adminStatusLabels`(`adminMock.ts:22-29`)는 옛 mock enum(`SUBMITTED/CONSULTING/PAYMENT_PENDING/IN_PRODUCTION/COMPLETED/CANCELLED`) 기준이라, 실 API가 주는 백엔드 enum(`SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`)과 `SUBMITTED`/`COMPLETED`/`CANCELLED` 3개만 겹친다. 그대로 연결하면 나머지 상태에서 라벨이 빈 값으로 나온다 — §1.4에 이미 지적된 것과 동일한 enum 불일치가 여기도 적용됨. 새 상태 라벨 맵이 필요하다.
  4. **날짜 필드 교체**: mock은 `submittedAt`(`YYYY-MM-DD` 문자열, `.replace(/-/g,".")`로 표시), 실 API는 `createdAt`(`LocalDateTime`, 예: `2026-08-20T14:32:00`) — 그대로 `.replace()`하면 시분초까지 붙어 나오므로 `.slice(0,10)` 등으로 날짜만 잘라야 함.
  5. `cardType` 표시는 mock처럼 별도 라벨 테이블 조회가 필요 없음 — 응답에 이미 `cardTypeName`이 문자열로 온다.
  - 응답 필드: 목록 `applicationId, applicationNumber, applicationType, cardTypeId, cardTypeName, totalQuantity, status, paymentStatus, createdAt` / 상세 `issueType, paymentGuidedAt, paymentDueAt, cancelled*, refundedAt, cardReadyAt, physicalDispatchedAt, photoRejectReason, applicant, receiver, memberCount`.
  - **스코프 요약**: 순수 프론트 파일 2개(`api.ts` +1함수, `MyPage.tsx` 데이터소스 교체) 변경이지만, 상태 라벨 매핑과 날짜 포맷 두 가지를 놓치면 "목록은 뜨는데 상태/날짜가 깨져 보이는" 2차 버그로 이어지므로 같이 처리해야 함.

### 1.3 1:1 문의(Inquiry) — ✅ 백엔드 구현 완료, 프론트 연동 전 조치 필요 (PARTIAL)
- **프론트 사용처**: `pages/InquiryPage`(작성), `pages/InquiryDetailPage`(상세), `pages/MyPage`(내 문의), `pages/AdminPage`(관리자 목록+답변). 현재는 `data/inquiries.ts`(`localStorage["customer-inquiries"]`) mock 그대로 — 아직 API 연동 안 됨.
- **✅ 백엔드 6개 API 전부 구현·테스트·커밋 완료(2026-08-19)**: `POST /api/inquiries`, `GET /api/my/inquiries`(+`/{id}`), `GET /api/admin/inquiries`(+`/{id}`), `PATCH /api/admin/inquiries/{id}/answer`, `PATCH /api/admin/inquiries/{id}/status`. 상세 계약은 **`docs/specs/inquiry/requirements.md`가 source of truth**(§④ API 목록, §⑤ 처리 흐름, §⑦ Validation).
- **⚠️ 연동 전 프론트가 반드시 반영해야 하는 것 — `privacyConsent` 필드(2026-08-19)**: 개인정보 수집·이용 동의를 서버에서도 강제하기로 정책 확정했다(`requirements.md` §⑤·§⑥·§⑦) — `POST /api/inquiries` 요청 바디에 `privacyConsent: true`가 **필수**다(Bean Validation `@AssertTrue`로 서버가 거절함). 현재 `InquiryPage.tsx`의 동의 체크박스 상태(`agreed`)는 제출 버튼 비활성화에만 쓰이고 `FormData`에는 포함되지 않는다. **이 필드를 프론트가 요청 바디에 추가로 실어 보내지 않으면 문의 등록이 매번 400(`INVALID_INPUT`)으로 거절된다.**
- **연동 시 참고**: `category`는 프론트가 이미 보내는 한글 문자열(제작 신청/결제 및 배송/카드 발급/행사·단체 협업/기타) 그대로 받는다(백엔드가 `@JsonValue`/`@JsonCreator`로 매핑, 프론트 값 변경 불필요). `name`/`email`/`phone`은 계정 값이 아니라 폼에 입력한 값 그대로 저장된다. 목록·상세 API는 페이지네이션이 없다(프론트에 검색/페이지 UI 자체가 없어 전체 나열).

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

### 1.9 회원정보 address 수정(⚠️ 정책 재정정, 프론트 신규 구현 필요) · 학생증 schoolName(🔴 프론트 연동 시급, 신청 깨짐)

#### (a) 회원정보 조회/수정 — 갭 아님, 확정 정책(2026-08-08, 2026-08-20 재확인 2회)
`PATCH /api/users/me`는 `name`/`phone`만 처리한다. **`address`는 이 API로 수정하지 않는다** — 한때(2026-08-20 세션 초반) 지원하도록 뒤집혔다가, 다시 원래 정책(이름·전화번호만 수정 가능)으로 재확정됐다. `UserUpdateRequest`엔 `address` 필드 자체가 없어 요청 바디에 보내도 무시된다.
**추가로 `GET /api/users/me`(조회) 응답에서도 `role`(회원등급) 필드를 완전히 제거했다** — 마이페이지 "내 정보"에 회원등급 개념 자체가 없어야 한다는 요구를 반영, `UserMeResponse` DTO 자체에서 `role`을 뺐다(단순히 화면에서 안 보이게 하는 게 아니라 백엔드 응답 스키마 변경).
- **⚠️ 프론트가 반드시 확인해야 하는 것 — `AuthContext.tsx`의 `isAdmin`**: `refreshProfile()`이 `api.getMe()`(=`GET /api/users/me`) 응답의 `profile.role`을 읽어 `user.role`/`isAdmin`을 세팅하고 있는데(`AuthContext.tsx:51`), 이제 이 필드가 응답에서 사라진다. 지금은 이 `isAdmin`이 데모 로그인(`loginAsAdmin`) mock 상태에만 실질적으로 쓰이고 실 서버 인가와는 무관한 프론트 전용 값이라 당장 보안 문제는 아니지만, `Header.tsx` 관리자 메뉴 노출·`InquiryDetailPage.tsx`의 `user.role === "admin"` 열람권한 체크가 이 값에 의존하므로 **API 응답으로 `isAdmin`을 판별하던 로직은 더 이상 동작하지 않는다.** 관리자 화면이 실제로 필요하다면 별도 신호(예: 관리자 전용 엔드포인트 접근 성패, 혹은 새 전용 API)로 바꿔야 한다 — 이번 변경 범위엔 대체 수단이 포함돼 있지 않다.
- **마이페이지 "내 정보" 표시/수정 화면 스펙(확정)**:
  - **조회 시 노출 필드**: 이름, 전화번호, 이메일 3개만. `UserMeResponse`는 `id, name, email, phone, address`를 반환한다 — 이름·이메일·전화번호 다 있다. **"회원 유형" 자체가 응답에 없으므로 표시할 수도 없다.**
  - **수정 가능 필드**: 이름, 전화번호 **둘 뿐**. `UserUpdateRequest`엔 `name`, `phone` 두 필드뿐이다. `address`는 조회·수정 어느 화면에도 넣지 않는다.
- 계약 상세는 `docs/api/user.md` API 2·API 5 참고.

#### (b) 학생증 schoolName — 🔴 백엔드는 구현 완료, **프론트가 안 보내서 학생증 신청이 지금 전부 실패**
- **백엔드 현황(2026-08-19 구현, `docs/specs/application/requirements.md` §5-0)**: `Application.schoolName` 신규 필드. `cardTypeId`가 학생증이면 `schoolType`(대학교/고등학교) 무관하게 **항상 필수**(개인·단체 공통, 신청 폼 최상위 필드 — `orientation`/`schoolType`과 같은 위치). 트림 후 5~20자, 한글·영문·숫자·공백만 허용, 비학생증이면 값이 있으면 거절. 개인 API·단체 API 둘 다 동일하게 적용.
- **프론트 현황 — 화면엔 있는데 요청엔 안 실림**: `StepInfo.tsx`가 `draft.applicant.schoolName` 입력을 이미 받고(대학교/고등학교 공통 "학교명"/"대학교명" 라벨), `StepReview.tsx`도 확인 화면에 보여준다. 하지만 실제 제출 코드인 `ApplyPage.tsx`의 `submit()`을 직접 확인한 결과, 개인·단체 요청 바디 어디에도 `schoolName`이 없다 — `orientation`/`schoolType`만 실려 있고 `schoolName`은 빠져 있다.
- **🔴 실제 영향**: 지금 상태로 실 연동하면 **학생증(개인/단체 모두) 신청은 100% 400(`INVALID_INPUT`)으로 거절된다.** 목데이터 모드에서는 서버 호출 자체를 안 하므로 드러나지 않았을 뿐이다.
- **조치(작아서 빠르게 가능)**: `ApplyPage.tsx`의 `submit()`에서 `orientation`/`schoolType` 옆에 한 줄 추가하면 된다 — `schoolName: isStudent ? draft.applicant.schoolName : undefined`(개인·단체 요청 객체 둘 다). 이미 `orientation`/`schoolType`을 학생증일 때만 조건부로 넣는 동일한 패턴이 코드에 있어서 그대로 따라 하면 된다.

### 1.10 신청 조회 응답 `applicationType` — ✅ 백엔드 해결, 프론트 연동 가능
- **배경**: 스펙 §3.7은 사진 재업로드 시 **프론트가 ApplicationType에 따라 part를 분리**(개인 `photo` / 단체 `submitFile`)하라고 요구한다.
- **백엔드 현황**: `ApplicationLookupResponse.applicationType`(`INDIVIDUAL`/`GROUP`) 구현 완료.
- **남은 작업**: 프론트 `MobileCardPage`가 `applicationType`에 따라 개인 `photo`/단체 `submitFile` 파트와 안내문구를 분기한다.

### 1.12 개인 신청 국적(nationality) — 🔴 placeholder가 백엔드 검증 규칙과 어긋남

- **백엔드 현황**: `ApplicationFieldFormats.isValidNationality()`가 `Set.of(Locale.getISOCountries())`로 검증한다 — **ISO 3166-1 alpha-2 2자리 대문자 코드**(`KR`, `US`, `JP` 등)만 통과하고, 그 외 문자열은 전부 `INVALID_INPUT`으로 거절된다(`ApplicationCreateRequest.MemberRequest.nationality`의 `@ValidNationality`).
- **프론트 현황 — 잘못된 예시로 사용자를 오도함**: `StepInfo.tsx`의 국적 입력란이 자유 텍스트고 `placeholder="대한민국"`이다. `ApplyPage.tsx`의 `submit()`은 이 값을 아무 가공 없이 그대로 요청에 싣는다(`nationality: draft.applicant.nationality`) — 대소문자 변환도, 국가명→코드 매핑도 없음.
- **🔴 실제 영향**: 사용자가 placeholder 그대로 "대한민국"이라고 입력하면(또는 "Korea"/"korea"/"kr" 등 ISO 코드가 아닌 어떤 값이든) **100% `INVALID_INPUT`으로 신청이 거절된다.** 외국인 관광객 대상 서비스라 국적 입력 자체가 핵심 흐름인데, 지금 UI가 정답이 아닌 값을 예시로 보여주고 있다.
- **참고 — 단체(엑셀) 경로는 이미 정상**: 단체신청 엑셀 템플릿의 국적 열은 ISO alpha-2 코드 드롭다운(`CountryCodes` 이름정의, 249개)으로 이미 제한돼 있어 문제없다. 이번 건은 **개인 신청 화면에만 해당**.
- **조치**: 아래 중 하나로 정리 필요(정책 판단 필요, 프론트 담당자 결정 사항) —
  1. 국가명을 입력받아 프론트에서 ISO 코드로 매핑해 전송(국가 선택 드롭다운/자동완성 UI로 교체, 엑셀 템플릿과 동일한 코드 목록 재사용 가능)
  2. 개인 신청도 엑셀처럼 "국가 선택" 드롭다운으로 바꿔 애초에 잘못된 값 입력 자체를 차단
  - 최소한 **placeholder를 실제 유효한 값 예시("KR" 등)로 바꾸는 것**만이라도 임시 조치로 필요.

### 1.11 신청 폼이 수집하나 백엔드가 저장하지 않는 입력 (프론트 유지 · 백엔드 보강)
프론트 화면에는 입력/표시가 있으나 백엔드 request DTO·도메인에 대응이 없어 값이 서버에 남지 않는 항목. **프론트 UI는 그대로 유지**하고 백엔드 보강 시 연결한다. 상세·조치는 `BACKEND_API_GAPS.md P1-4`.

| 프론트 입력 | 위치 | 백엔드 현황 |
|---|---|---|
| 입금자명 + 입금 확인/취소 | `StepComplete` | 결제·입금(Payment) 도메인 없음(입금 안내는 정적 계좌) |
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

## 6. 남은 프론트엔드 작업 전체 목록 (우선순위 순)

| 순위 | 항목 | 상태 | 관련 절 |
|---|---|---|---|
| 🔴 0 | **학생증 schoolName 요청 추가** | 백엔드 완료. `ApplyPage.tsx` `submit()`에 한 줄만 추가하면 됨 — **지금 학생증 개인·단체 신청이 전부 400으로 깨져 있음** | §1.9-b |
| 🔴 0 | **개인 신청 국적(nationality) 입력 방식 변경** | placeholder("대한민국")가 백엔드 요구값(ISO 코드)과 어긋남 — 국가 선택 드롭다운으로 교체하거나 최소한 placeholder만이라도 수정 | §1.12 |
| 1 | 일반 이메일 회원가입(인증코드 인라인 UI) | 백엔드 완료, 프론트 신규 구현 필요 | §1.1-a |
| 1 | 로그인·이메일 중복확인·비밀번호 변경 연동 | 백엔드 완료, 프론트 연동만 하면 됨(백엔드 작업 없음) | §1.1-b |
| 2 | 마이페이지 "내 신청 목록·상세" | 백엔드 완료, `adminMock.ts` localStorage → 실 API 교체 | §1.2 |
| 2 | 신청 취소 진입점 | 백엔드 완료, 연동 가능 | §1.5 |
| 3 | 1:1 문의(Inquiry) 전체 연동 | 백엔드 완료, `privacyConsent` 필드만 요청에 추가하면 됨 | §1.3 |
| 4 | 신청 조회 응답 `applicationType`으로 재제출 UI 분기 | 백엔드 완료, 프론트가 개인 `photo`/단체 `submitFile` 파트 분기만 하면 됨 | §1.10 |
| 4 | "내 정보" 표시 정리 — 회원 유형 제거, 전화번호 노출 추가 | 백엔드 변경 없음(확정 정책 §1.9-a 반영). 조회: 이름·전화번호·이메일만 표시(현재 `MyPage.tsx`엔 전화번호 미표시 + 회원유형 표시 중 — 둘 다 수정 필요). 수정: 이름·전화번호만(현행 유지) | §1.9-a |
| 5 | 관리자 신청관리·통계 UI | **백엔드도 아직 없음**(공동 대기) — status enum 프론트/백 매핑도 먼저 확정 필요 | §1.4 |
| 6 | 행사 회사/로고 필드, 공지 서버검색, 후기 다중이미지 | **정책 결정 대기** — 결정 후 백엔드·프론트 함께 진행 | §1.6~§1.8 |
| 7 | 계정복구(아이디/비밀번호 찾기) | **백엔드도 아직 없음**(공동 대기, UX는 이미 결정됨) | §1.1-c |
| 8 | 한국이름 조회 API 전환, 정적 마케팅 CMS화, 하이브리드 목데이터(§5) 정리 | 우선순위 낮음, 필요 시에만 | §2.2, §3, §5 |

**진행 원칙**: 0번은 기존 기능을 되살리는 회귀 수정이라 다른 무엇보다 먼저. 1~4번은 백엔드가 이미 준비돼 있어 프론트 작업만으로 끝나는 항목(가장 빠르게 갭을 줄일 수 있음). 5번 이후는 백엔드 작업이나 정책 결정이 먼저 필요해 프론트 혼자 진행할 수 없는 항목.
