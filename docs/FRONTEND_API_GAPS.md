# 프론트 ↔ 백엔드 API 갭 · 목데이터 전환 목록

> **갱신: 2026-08-25(9차, 실 API 검증).** 관리자 작명 확정·카드 제작 계획 1-A/1-B(백엔드 병합 완료) 이후 로컬 docker-compose로 백엔드를 실제 기동해 curl로 신규 API를 검증하는 과정에서 **blocking 회귀 2건 신규 발견**: ① `surname`(성씨) 입력 UI가 프론트에 전혀 없어 관리자가 어떤 신청도 "작명 완료 처리"를 할 수 없음(§1.19). ② 개인 신청(학생증 제외)에 카드 표기 주소 입력란이 없어 제출이 전부 400으로 실패(§1.20). 둘 다 백엔드 신규 필수 필드가 추가됐는데 프론트가 아직 못 따라간 경우 — §0 표·§1.4도 함께 정정.
>
> **갱신: 2026-08-25(8차, 원격 병합).** 이후 관리자 상태전이 3종(`confirm-payment`·`start-review`·`approve-naming`)과 입금자명(`PATCH /api/applications/{id}/depositor`)이 추가되어 **백엔드 엔드포인트 총 65개**가 되었고 전부 프론트 `api.ts`에서 실호출됨(65/65). 관리자 상태전이는 총 8종(결제확인·검토시작·작명승인·사진반려·작명완료·제작시작·카드발급·배송발송). 후기 수정 removeImage 버그도 수정 완료. **아래 원격 7차 노트가 미구현/미연동이라 한 것 중: §1.4a(입금확인→심사→작명 진입)·§1.15(엑셀 export 프론트)·§1.18(saju 재업로드)은 이후 구현·연동 완료됨. 여전히 미구현은 §1.16(카드 이미지 합성 API)·§1.17(이름 추천/조회 API)·관리자 통계뿐** — `docs/BACKEND_TODO.md` 참고.
>
> **갱신: 2026-08-25(7차, 원격).** 관리자 대시보드 코드 재대조로 4건 추가 확인(당시 기준). §1.4a — 신규 신청을 입금확인→심사시작→작명으로 못 넘김(`confirmPayment`/`startReview`/`approveToNaming` Controller 부재). §1.15 엑셀 내보내기 백엔드 완료·프론트 미연동. §1.16 카드 이미지 합성(좌표 기반)은 백엔드 엔진만·API/프론트 없음. §1.17 이름 추천 데이터는 DB 이관됐으나 조회 API 없음. §1.18 엑셀 왕복(saju 재업로드) 백엔드 완료·업로드 UI 없음. → **§1.4a·§1.15·§1.18은 8차에서 해소, §1.16·§1.17은 잔존**(위 8차 참고).
>
> **갱신: 2026-08-25(7차) — 전면 재대조.** 백엔드 컨트롤러 엔드포인트가 프론트 `api.ts`에서 실호출됨을 코드로 검증(경로 매칭 전건, 정의만 하고 미호출 함수 0건). 이번에 발견·수정한 **미연동 3건**: ① `POST /api/admin/applications/export`(엑셀 내보내기) — 프론트가 "백엔드 미구현" 토스트만 띄우던 걸 실제 xlsx 다운로드로 연결(개인 export E2E 200+유효 xlsx 확인) ② `POST /api/admin/applications/{id}/naming-result`(작명결과 엑셀 반영) — 업로드 UI 연결 ③ `GET /api/my/applications/{id}`(내 신청 상세) — 마이페이지 행 펼침 연결. **6차 노트의 "§1.3·§1.4 관리자가 여전히 api.ts를 안 부르고 mock만 쓴다"는 서술은 사실이 아님(현재 `InquiriesSection`/`ApplicationsSection`이 실 API 다수 호출) — 아래 §0 표·§1.4로 정정됨.** 관리자 로그인 `isAdmin`도 정상(`LoginPage`가 `ADMIN→admin` 매핑, `refreshProfile`가 role 보존). 남은 백엔드 부재: 통계·공지검색·이름조회 API 3건.
>
> **갱신: 2026-08-24(6차).** Codex 세션이 오늘 커밋한 변경사항을 실제 코드 대조로 반영: 계정 복구(§1.1-c)·행사 관리자 연동(§1.6)·후기 다중 이미지(§1.8) **연동 완료**로 전환. 공지/FAQ(§1.14)는 데모 시드 추가로 `FaqPage.tsx` 쪽 빈 목록 문제는 해소. ~~관리자 신청관리(§1.4)·1:1 문의 관리자 답변(§1.3)이 여전히 mock~~ **[7차 정정: 이 서술은 오류였음 — 관리자 문의·신청 모두 `InquiriesSection`/`ApplicationsSection`에서 실 API 연동됨. §1.3·§1.4 본문 참고]**. 상세 근거는 각 절의 파일:라인 인용 참고.
>
> **갱신: 2026-08-20(5차).** 회원정보 `address` 수정 정책이 같은 날 두 번 뒤집혔다 — (4차) "이름·전화번호만" → "address도 수정 가능"으로 바뀌었다가, (5차, 이번 갱신) **다시 "이름·전화번호만"으로 최종 확정**됐다(백엔드 코드도 원복 완료). §1.9(a)는 다시 "갭 아님"이며, 추가로 마이페이지 "내 정보" 표시 스펙도 확정됨 — 조회는 이름·전화번호·이메일만(회원 유형 표시 제거), 수정은 이름·전화번호만. `docs/api/user.md` API 5도 함께 원복 반영. ~~마이페이지 "제작 내역"이 실 API 미연동으로 빈 목록만 뜨는 문제(§1.2)~~ **[7차 정정: 현재 `MyPage`가 `listMyApplications`/`getMyApplication` 실호출로 연동 완료 — §1.2 본문 참고]**. 3차 갱신 내용(코드 재대조로 `schoolName` 미연동 발견, §1.9 전면 정정)은 그대로 유지. 로그인/이메일중복확인/비밀번호변경(§1.1-b) 오탈 정정은 2026-08-19(2차)에 이미 반영됨. 프론트 연동 계약 종합은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13), 백엔드 API 상세는 `docs/api/auth.md`(API 4~6), 백엔드 미구현 상세는 `docs/BACKEND_API_GAPS.md`와 함께 본다. 프론트의 목데이터/localStorage 사용 자체는 결함이 아니라, 백엔드가 준비된 화면부터 순차 교체하는 방식이다.

> 대상: `frontend/src` 전체 · 근거: `services/api.ts`(실제 호출) ↔ `backend/honor-citizen/.../api/*Controller.java`(실구현) 상호 대조(현재 워킹 트리·`main` 기준).

---

## 0. 한눈에 보기

> **2026-08-25 전면 갱신**: 아래 표는 이전 버전에서 이미 해소된 항목이 🔴로 남아 본문(§1.x)과 모순됐다. 코드 실호출을 전수 재대조해 현재 상태로 정정했다. **백엔드 컨트롤러 엔드포인트 65개 전부 프론트 `api.ts`에서 실호출됨**(정의만 있고 미호출 0건, 유일한 예외 `api.refresh`는 `request()`의 401 자동재시도가 직접 호출). 남은 것은 백엔드에 엔드포인트 자체가 없는 3건(통계·공지검색·이름조회)과 정적 유지 확정 항목뿐.

| 기능 영역 | 프론트 연동 | 근거(실호출 위치) | 상태 |
|---|---|---|---|
| OAuth·이메일 로그인·약관·세션·회원정보 | ✅ 완료 | `LoginPage`(`loginWithPassword`, `ADMIN→admin` 매핑), `AuthContext.refreshProfile`(role 보존), `TermsPage`(`agreeTerms`) | 관리자 로그인 `isAdmin=true` 정상(실 admin API 호출 E2E 확인). 데모 로컬 로그인만 운영빌드 제거 대상(`TEMP_ADMIN_LOGIN.md`) |
| 회원정보 조회·수정·비밀번호 변경·탈퇴 | ✅ 완료 | `MyPage`(`getMe`/`updateMe`/`changePassword`/`withdraw`) | 조회=이름·전화·이메일, 수정=이름·전화, 비밀번호 변경 폼(`PATCH /me/password`) (§1.9-a·§1.1-b) |
| 신청 생성(개인/단체) | 🔴 **회귀** | `ApplyPage`(`createApplication`, 단체=`/bulk`) | 개인(학생증 제외) 제출 시 `member.address` 누락으로 **항상 400 실패**(§1.20, 2026-08-25 신규). 기존 세 회귀(schoolName/국적ISO/유령첨부)는 수정됨(§1.9-b·§1.12·§1.13) |
| 신청 조회·카드다운로드·재제출 | ✅ 완료 | `LookupPage`(`lookupApplication`), `MobileCardPage`(`getCardDownload`, `reuploadPhoto` — 개인 `photo`/단체 `submitFile` 분기) | 재제출 분기까지 연결(§1.10) |
| 신청 취소 | ✅ 완료 | `MyPage`(`cancelApplication`, 취소가능 상태 한정) | (§1.5) |
| 내 신청 목록·상세 | ✅ 완료 | `MyPage`(`listMyApplications` 목록 + `getMyApplication` 행 펼침 상세) | 2026-08-25 상세 연결(§1.2) |
| 후기 CRUD·내 후기·다중이미지 | ✅ 완료 | `ReviewsPage`/`ReviewEditor`/`ReviewDetail`/`MyPage`(`listReviews`/`createReview`/`updateReview`/`deleteReview`/`listMyReviews`/`getReview`) | (§1.8) |
| 공지/FAQ(Board) | ✅ 완료 | `NoticesPage`/`FaqPage`/`SupportPage`(`listBoards`/`getBoard`), 관리자 `BoardsSection`/`BoardAdminPanel`(`create/update/deleteBoard`) | `SupportPage`도 `listBoards({type:"FAQ"})` 실호출(폴백 有) — 두 화면 소스 통일(§1.14) |
| 행사(Event) | ✅ 완료 | `EventsPage`(`listEvents`/`getEvent`), `EventAdminPanel`(`listAdminEvents`/`getAdminEvent`/`create/update/deleteEvent`) | (§1.6) |
| 일반 이메일 회원가입(인증 인라인) | ✅ 완료 | `SignupPage`(`checkEmail`/`requestSignupEmailCode`/`confirmSignupEmailCode`/`signup`) | 2026-08-24 구현(§1.1-a). ※ 로컬 dev는 SMTP 미설정으로 코드발송만 503 |
| 계정 복구(아이디/비밀번호 찾기) | ✅ 완료 | `AccountRecoveryPage`(`request/confirm Id/Password Recovery` 4종) | (§1.1-c) |
| 1:1 문의(Inquiry) | ✅ 완료 | `InquiryPage`(`createInquiry`, `privacyConsent:true`), `MyPage`/`InquiryDetailPage`(`listMyInquiries`/`getMyInquiry`), 관리자 `InquiriesSection`(`listAdminInquiries`/`getAdminInquiry`/`answerInquiry`/`updateInquiryStatus`) | 관리자 답변까지 실 API(§1.3) |
| 관리자 신청관리 | ⚠️ 부분 회귀 | `ApplicationsSection`: 조회 3종·작명확정·선택이력·상태전이 8종·**엑셀 export**·**작명결과 업로드** 전부 실호출 | 호출 자체는 되나 작명확정 바디에 `surname` 누락 — **작명 완료 처리가 항상 실패**(§1.19, 2026-08-25 신규) |
| 관리자 통계(`GET /api/admin/stats`) | — | (백엔드 엔드포인트 없음) | 🔴 **백엔드 미구현** — 대시보드 통계는 목록 프론트 집계로 대체(§1.4) |
| 공지 서버 검색 | — | (백엔드 keyword 파라미터 없음) | ⚠️ 클라 검색만. 필요 시 백엔드 추가(§1.7) |
| 한국이름 조회·추천(`nameResults.json`) | 정적 번들/자체 mock | (백엔드 조회·추천 API 없음) | ⚠️ 조회/추천 API 미정(§2.2·§1.17) |
| 카드 이미지 합성(신청정보→카드 자동매핑) | — | 🟡 합성 엔진(`CardImageCompositor`)만, HTTP API 없음 | 🔴 **백엔드 API 미구현** — 엔드포인트 신설 후 프론트 신규(§1.16) |
| 카드 종류·디자인 카탈로그 | 정적(`cards.ts`) | — | STATIC 확정, 공개 API 신설 안 함(§2.1) |
| 정적 마케팅(협력사/SNS/약관문/회사정보) | 정적 | — | 선택 CMS화(우선순위 낮음, §3) |

범례: ✅ 프론트-백엔드 실 연동 완료 · ⚠️ 부분/대체 · 🔴 백엔드 엔드포인트 자체가 없음 · — 해당없음

---

## 1. 프론트가 필요로 하나 백엔드에 없는 부분

### 1.1 일반 이메일 회원가입·로그인·계정 복구 — ✅ a(회원가입 인라인 인증)·b(이메일 중복확인·비밀번호 변경) 프론트 연동 완료(2026-08-24)

#### (a) 회원가입(이메일 인증 포함) — ✅ 프론트 연동 완료(2026-08-24)
- **백엔드**: `POST /api/auth/signup/email-verification/request`(코드 발송), `.../confirm`(코드 확인→`signupToken`), `POST /api/auth/signup`(`signupToken`+`email`+`password`+`name`+`phone`). 계약 `docs/api/auth.md` API 4~6.
- **✅ 구현됨**: `SignupPage.tsx`를 인라인 인증 플로우로 재작성 — 이메일 입력 → "인증코드 받기"(발송 전 `checkEmail` 중복확인) → 같은 화면에 코드 입력·"확인" → `signupToken` 확보 → 이름·비밀번호·전화 입력 → `api.signup` 제출 → `/login` 이동. `api.ts`에 `checkEmail`/`requestSignupEmailCode`/`confirmSignupEmailCode`/`signup` 4개 바인딩 추가. 비밀번호 규칙도 백엔드와 동일(8~72자, 복잡도 없음)로 맞춤.
- **⚠️ 로컬 dev 한계**: SMTP 미설정 환경에서는 `.../request`가 503(메일 발송 불가) — 연동 자체는 정상, 실제 코드 수신 E2E는 메일 서버 구성 후 가능. 백엔드 발송 로직은 `EmailVerificationServiceTest`로 단위 검증됨.

#### (b) 로그인·이메일 중복확인·비밀번호 변경 — ✅ 프론트 연동 완료
- **백엔드**: `POST /api/auth/login`, `POST /api/auth/email/check`, `PATCH /api/users/me/password`.
- **✅ 구현됨**: `LoginPage`가 `api.loginWithPassword` 실호출(실패 시에만 데모 폴백, `ADMIN→admin` 매핑). 이메일 중복확인은 회원가입 플로우(§(a))의 `checkEmail`으로 연동. 비밀번호 변경은 `MyPage`에 폼 추가 — `api.changePassword(current, new)` → `PATCH /me/password`(8~72자 검증). 테스트 `AuthControllerLoginTest`/`AuthControllerEmailCheckTest`(4)/`UserControllerChangePasswordTest`(5) 통과.
- **정책**: 로그인 아이디=이메일(trim+소문자 UNIQUE), 비밀번호 BCrypt 8~72자, role 서버 결정, **운영 빌드에서 데모 로그인 제거 필요**(`TEMP_ADMIN_LOGIN.md`).

#### (c) 계정 복구(아이디/비밀번호 찾기) — ✅ 백엔드·프론트 연동 완료(2026-08-24)
- **프론트 사용처**: `pages/AccountRecoveryPage` — 아이디 찾기(이름·전화 요청→코드 확인→마스킹 이메일 표시)와 비밀번호 찾기(이메일 요청→코드+새 비밀번호 확인) 둘 다 요청·확인 단계 전부 실 API로 구현 완료(2026-08-24).
- **백엔드 현황**: 4개 API(`docs/api/auth.md` API 7·8) 구현·테스트 보강까지 완료(`docs/collab/TODO.md` RECOVERY-0~3 전부 완료 처리됨).
- **필요 API(계약 확정)**
  | 메서드/경로 | 용도 | 인증 |
  |---|---|---|
  | `POST /api/auth/recovery/id/request` | 이름·전화 일치 시 가입 이메일로 확인 코드 발송(불일치해도 동일 응답) | 없음 |
  | `POST /api/auth/recovery/id/confirm` | 코드 확인 → 마스킹 이메일(`ho***@example.com`) 공개 | 없음 |
  | `POST /api/auth/recovery/password/request` | 이메일로 재설정 코드 발송(OAuth 전용/미가입도 동일 응답) | 없음 |
  | `POST /api/auth/recovery/password/confirm` | `{requestId, code, newPassword}` 한 번에 — 코드 검증+비밀번호 저장+전체 세션 무효화 | 없음 |
- **⚠️ 2026-08-20 정책 결정 2건(사용자 확인 완료)**:
  1. **아이디 찾기 인증 강도**: 일반 이메일 계정만 대상이다. 전화번호가 SMS 인증된 적이 없어서 이름+전화번호만으로 이메일을 즉시 공개하지 않는다. 정확히 한 계정이 일치할 때만 가입 이메일로 코드를 보내고, 중복 일치 시 임의 선택 없이 고객지원을 안내한다.
  2. **비밀번호 재설정 대상이 OAuth 전용 계정(비밀번호 없음)이거나 미가입 이메일인 경우**: 에러를 주지 않고 **메일 발송 없이 조용히 동일한 성공 응답**만 준다(계정 존재/유형 비노출).
- **프론트가 새로 만들어야 하는 것**: 아이디 찾기는 "이름·전화 입력 → 확인 코드 입력 → 마스킹 이메일 표시" 3단계다. 비밀번호 찾기는 기존 전화번호 입력을 제거하고 "이메일 입력 → `requestId` 보관 → 코드+새 비밀번호를 한 화면에서 제출" 2단계로 만든다. 임시 비밀번호 표시 화면은 만들지 않는다. 성공 시 토큰을 받거나 자동 로그인하지 않고 로그인 화면으로 이동한다.
- **세션 계약**: 재설정 성공 시 기존 refresh token과 access token이 모두 무효화된다. 다른 브라우저·기기의 로그인도 종료될 수 있음을 성공 안내에 표시한다.
- **입력·오류 계약**: 아이디 찾기 전화번호는 국제번호 `+`·공백·하이픈 입력을 허용한다. `TOO_MANY_REQUESTS`는 실제 계정과 가짜 요청에 동일하게 적용되므로 계정 존재 여부를 의미하지 않는다. `AUTH_SESSION_VALIDATION_UNAVAILABLE`(503)은 비밀번호 오류가 아니라 인증 인프라 일시 장애로 표시하고 재시도를 안내한다.
- **정책**: 복구 응답은 계정 존재 비노출(위 2건 결정이 이 원칙의 구체화).

### 1.2 내 신청 목록·상세(마이페이지) — ✅ 프론트 연동 완료
- **연동 상태(2026-08-25 코드 검증)**: `MyPage.tsx`가 실 API로 완전히 전환됨.
  - 목록: `api.listMyApplications({ size: 100 })` 실호출(`MyPage.tsx:38,81`) — `user.source === "api"`(서버 세션)일 때만.
  - 상세: 제작내역 행을 펼치면 `api.getMyApplication(id)`(`GET /api/my/applications/{id}`) 호출로 발급방식·결제·반려사유·발송시각 표시(`MyPage.tsx:67`, 2026-08-25 연결).
  - 상태 라벨: 백엔드 enum(`SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`) 기준 `APP_STATUS_LABELS` 맵 사용, 날짜는 `createdAt`을 `toLocaleDateString`으로 표시.
- **localStorage 제거 완료**: 옛 `data/adminMock.ts`(`loadApplications`/`saveLocalApplication`)는 **파일째 삭제**됨 — 코드베이스에 호출 0건. 신청은 `POST /api/applications`에만 저장되고 조회는 서버에서만 온다.
- **백엔드**: `MyApplicationController`(`GET /api/my/applications`(+`/{id}`)) — 추가 작업 없음. 테스트 `MyApplicationControllerTest`(6)·`ApplicationServiceMyApplicationsTest`(7) 통과.

### 1.3 1:1 문의(Inquiry) — ✅ 프론트 연동 완료(작성·내문의·관리자답변 전부 실 API)
- **연동 상태(2026-08-25 코드 검증)**:
  - 작성: `InquiryPage`가 `api.createInquiry`로 `POST /api/inquiries` 호출, **`privacyConsent: true` 포함**(`InquiryPage.tsx:38`) — 서버 `@AssertTrue` 통과.
  - 내 문의: `MyPage`/`InquiryDetailPage`가 `api.listMyInquiries`/`getMyInquiry`.
  - 관리자: `components/admin/sections/InquiriesSection.tsx`가 `api.listAdminInquiries`/`getAdminInquiry`/`answerInquiry`/`updateInquiryStatus` **전부 실호출**(옛 "AdminPage가 api.ts를 안 부른다"는 서술은 사실 아님 — 관리자 문의는 `InquiriesSection`으로 분리·실연동됨).
- **localStorage 제거**: 옛 `data/inquiries.ts`(`customer-inquiries`) 삭제됨.
- **백엔드**: 6개 API(`POST /api/inquiries`, `GET /api/my/inquiries`(+`/{id}`), `GET /api/admin/inquiries`(+`/{id}`), `PATCH .../answer`, `PATCH .../status`) — source of truth `docs/specs/inquiry/requirements.md`. `category`는 한글 문자열 그대로(백엔드 `@JsonValue`/`@JsonCreator` 매핑). 테스트 `InquiryControllerTest`(4)·`InquiryServiceTest`(15) 통과.

### 1.4 관리자 신청관리 — ✅ 조회·작명·상태전이·엑셀 전부 프론트 연동 완료(2026-08-25). 통계 API만 백엔드 미구현. ⚠️ 단, 작명 확정 요청에 `surname` 누락(§1.19, 2026-08-25 실 API 검증으로 발견) — "완료"는 연동 자체 얘기고, 계약 필드 하나가 새로 추가돼 다시 깨졌다는 뜻
- **프론트 사용처**: `components/admin/sections/ApplicationsSection.tsx`(제작신청 관리). **실제 API 연동 완료** — `data/adminMock.ts` 미사용, `services/api.ts` 실호출.
- **✅ 프론트 연동 완료된 관리자 신청 API(전부 실호출)**:
  - 조회: `GET /api/admin/applications`(`listAdminApplications`), `.../{id}`(`getAdminApplication`), `.../{id}/members`(`getAdminApplicationMembers`).
  - 작명: `POST /api/admin/applications/{id}/members/{memberId}/name`(`saveMemberName`, 인앱 확정), `GET /api/admin/name-selection-stats`(`getNameSelectionStats`, 선택이력).
  - **작명 결과 엑셀 반영: `POST /api/admin/applications/{id}/naming-result`(`applyNamingResult`) — 2026-08-25 연동**(단체 상세의 "작명 결과 엑셀 업로드").
  - **엑셀 내보내기: `POST /api/admin/applications/export`(`exportApplications`) — 2026-08-25 연동**. 개인은 다중선택 일괄, 단체는 상세에서 1건씩(원본 서식 보존). 이전 코드가 "백엔드 미구현"이라 잘못 표기하고 토스트만 띄우던 것을 실제 xlsx 다운로드로 교체(INDIVIDUAL export E2E 200+유효 xlsx 확인).
  - 상태 전이(8종): **앞단 3종 `confirm-payment`(`confirmApplicationPayment`)·`start-review`(`startApplicationReview`)·`approve-naming`(`approveApplicationNaming`)** + `reject-photo`(`rejectApplicationPhoto`)·`start-producing`(`startProducing`)·`card-ready`(`markCardReady`)·`dispatch`(`dispatchApplication`)·`complete-naming`(`completeNaming`) — 상세의 "상태 관리" 드롭다운에서 현재 상태에 맞는 것만 실호출.
- **status enum**: 프론트가 백엔드 실제 enum(`SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`, 결제 `WAITING/CONFIRMED`)을 그대로 사용 — 불일치 해소됨.
- **❌ 백엔드 미구현(프론트 연동 불가)**: `GET /api/admin/stats`(통계 집계) — 백엔드에 엔드포인트 자체가 없음. 대시보드 통계 카드는 목록에서 프론트 집계로 대체 중.
- **인가**: `/api/admin/**` → `hasRole("ADMIN")` 서버 검증. 데모 admin 계정은 `TEMP_ADMIN_LOGIN.md`(운영 전 제거).

### 1.5 신청 취소 — ✅ 프론트 연동 완료
- **연동 상태(2026-08-25 코드 검증)**: `MyPage` 제작내역 행에 "신청 취소" 버튼 — 취소 가능 상태(`SUBMITTED/REVIEWING/PHOTO_REJECTED`)에서만 노출(`CANCELLABLE` set), 확인 후 `api.cancelApplication(id)` 호출→목록 재조회(`MyPage.tsx`).
- **백엔드**: `POST /api/applications/{id}/cancel`(`ApplicationController`). 이미 취소면 멱등 성공, 응답 `applicationId,status,refundRequired`. 테스트 `ApplicationStateTransitionTest`(6) 통과.

### 1.6 행사(Event) — 회사/로고 필드·관리자 전체목록·갤러리 편집 — ✅ 백엔드·프론트 연동 완료(백엔드 2026-08-21, 프론트 2026-08-24)
- **프론트 사용처**: `pages/EventsPage`의 `FeedPost`(`data/eventFeedPosts.ts`)가 협업 카드에 `company`/`logoUrl`로 로고 표시. 관리자 패널(`EventAdminPanel.tsx`)이 실 API로 전체목록(숨긴 글 포함)·생성·수정(갤러리 편집·로고 유지/교체/삭제)·삭제까지 전부 구현 완료.
- **백엔드 현황(2026-08-21 구현 완료·push 완료)**: `EventPost`에 `companyName`/`logoImagePath`(`COLLABORATION` 전용, 내부 엔티티/컬럼명) 추가. 응답 DTO(`EventListItemResponse`/`EventDetailResponse`/`EventAdminListItemResponse`/`EventAdminDetailResponse`)와 요청 DTO(`EventCreateRequest`/`EventUpdateRequest`)는 프론트 `FeedPost`(`data/eventFeedPosts.ts`)와 동일하게 `company`/`logoUrl` 필드명을 쓴다(엔티티 내부명과 API 계약명을 분리, 커밋 `1e5a7b3`). `GET /api/admin/events`(`visible` 무관 전체, `type`/`visible` 선택 필터)·`GET /api/admin/events/{id}` 신규. `PATCH /api/admin/events/{id}`에 갤러리 편집(`keepImageIds`) + 로고 유지·교체·삭제(`removeLogo`) 추가. 계약 상세는 `docs/specs/events/api.md` API 3·4·6·7.
- **프론트 현황(2026-08-24 연동 완료)**: `EventAdminPanel.tsx`가 `api.listAdminEvents`/`getAdminEvent`/`createEvent`/`updateEvent`(`keepImageIds`·`removeThumbnail`·`removeLogo` 포함)/`deleteEvent`를 전부 호출. `company`/`logoUrl` 필드명이 백엔드와 동일해 별도 매핑 어댑터 없이 그대로 대입한다(`eventToFeedPost()`, `data/eventFeedPosts.ts:28-29`). 기존 별도 컴포넌트였던 `EventFeedAdminPanel.tsx`는 이 작업으로 삭제됨(`EventAdminPanel.tsx`로 통합).

### 1.7 공지 서버 검색 — 없음 (PARTIAL)
- **프론트 사용처**: `pages/NoticesPage` 제목/작성일 검색.
- **백엔드 현황**: `GET /api/boards`에 `keyword`/`searchType` 파라미터 없음 → 프론트가 `size=100`으로 받아 **클라이언트 검색** 중.
- **필요**: 데이터 증가 시 게시판 서버 검색·페이지네이션 파라미터.

### 1.8 후기 다중 이미지 — ✅ 정책 확정(다중 허용) + 백엔드·프론트 구현 완료(2026-08-24)
- **백엔드 현황**: 후기 1건당 0~5장(`MAX_IMAGE_COUNT`, `ReviewService.java:54`) — `ReviewImage` 엔티티로 정식 다중 이미지 지원, 생성(`:74`)·수정(`:218`, 유지+신규 합쳐 5장 제한) 둘 다 적용. `docs/specs/review` API 계약도 갱신 완료(커밋 `ff4d27d`).
- **프론트 현황**: `ReviewEditorPage.tsx`가 다중 파일 선택(`multiple`, 최대 5장)·기존/신규 이미지 갤러리·개별 삭제를 구현, `keepImageIds`를 포함해 `updateReview`/`createReview` 호출.

### 1.9 회원정보 address 수정(⚠️ 정책 재정정) · 학생증 schoolName(✅ 프론트 연동 완료 2026-08-24) · 내정보 표시(✅ 완료)

#### (a) 회원정보 조회/수정 — 갭 아님, 확정 정책(2026-08-08, 2026-08-20 재확인 2회)
`PATCH /api/users/me`는 `name`/`phone`만 처리한다. **`address`는 이 API로 수정하지 않는다** — 한때(2026-08-20 세션 초반) 지원하도록 뒤집혔다가, 다시 원래 정책(이름·전화번호만 수정 가능)으로 재확정됐다. `UserUpdateRequest`엔 `address` 필드 자체가 없어 요청 바디에 보내도 무시된다.
**추가로 `GET /api/users/me`(조회) 응답에서도 `role`(회원등급) 필드를 완전히 제거했다** — 마이페이지 "내 정보"에 회원등급 개념 자체가 없어야 한다는 요구를 반영, `UserMeResponse` DTO 자체에서 `role`을 뺐다(단순히 화면에서 안 보이게 하는 게 아니라 백엔드 응답 스키마 변경).
- **✅ `isAdmin`은 정상 동작(2026-08-25 코드 검증) — 옛 우려는 해소됨**: `refreshProfile()`은 더 이상 `profile.role`을 읽지 않고 **로그인 시 확정된 기존 role을 보존**한다(`AuthContext.tsx` `role: prev?.role ?? "user"`, 주석에 "role을 profile에서 파생하면 admin이 user로 강등된다" 명시). role은 `LoginPage`가 `POST /api/auth/login` 응답의 `me.role`(ADMIN/USER, 로그인 응답엔 role 포함)을 `admin`/`user`로 매핑해 세팅한다(`LoginPage.tsx:30`). 즉 `GET /api/users/me`가 role을 빼도 실 관리자 로그인의 `isAdmin=true`는 유지된다(이번 세션 admin API E2E로 확인).
- **마이페이지 "내 정보" 표시/수정 화면 스펙(확정)**:
  - **조회 시 노출 필드**: 이름, 전화번호, 이메일 3개만. `UserMeResponse`는 `id, name, email, phone, address`를 반환한다 — 이름·이메일·전화번호 다 있다. **"회원 유형" 자체가 응답에 없으므로 표시할 수도 없다.**
  - **수정 가능 필드**: 이름, 전화번호 **둘 뿐**. `UserUpdateRequest`엔 `name`, `phone` 두 필드뿐이다. `address`는 조회·수정 어느 화면에도 넣지 않는다.
- 계약 상세는 `docs/api/user.md` API 2·API 5 참고.
- **🟡 사소한 타입 정리(비차단) — 기능 영향 없음**: `ApiUser`(`api.ts:62`)가 `role: "USER"|"ADMIN"`을 필수로 선언하는데, 이는 `POST /api/auth/login` 응답 기준으로는 맞다(로그인 응답엔 role 있음). `GET /api/users/me`만 role을 안 주지만 `getMe()` 결과의 `role`을 아무도 읽지 않으므로(refreshProfile은 prev role 보존) 런타임 문제 없음. `updateMe` 파라미터의 `address?`도 남아 있으나 `MyPage`는 `{name, phone}`만 전송해 무해. 원하면 `getMe` 전용 타입 분리·`updateMe`에서 `address` 제거로 정리 가능(선택).

#### (b) 학생증 schoolName — ✅ 프론트 연동 완료(2026-08-24)
- **백엔드**: `Application.schoolName` — 학생증이면 `schoolType` 무관 **항상 필수**(개인·단체 공통), 트림 후 5~20자, 비학생증이면 값 있으면 거절.
- **✅ 수정됨**: `ApplyPage.tsx`의 `submit()`이 개인·단체 요청 객체 둘 다 `schoolName: isStudent ? draft.applicant.schoolName : undefined`를 전송한다(`orientation`/`schoolType`과 동일 조건부 패턴). 이전엔 화면(`StepInfo`/`StepReview`)에만 입력이 있고 요청엔 빠져 학생증 신청이 400으로 전부 실패하던 회귀 — 해소됨.

### 1.10 신청 조회 응답 `applicationType` 재제출 분기 — ✅ 프론트 연동 완료(2026-08-25)
- **배경**: 스펙 §3.7 — 사진/파일 재업로드 시 개인은 `photo`, 단체는 `submitFile` 파트로 분리.
- **✅ 수정됨**: `LookupResult`에 `applicationType` 추가, `MobileCardPage`가 `PHOTO_REJECTED` 상태일 때 `applicationType==="GROUP"`이면 `submitFile`(ZIP)·아니면 `photo`(이미지) 파트와 안내문구·accept를 분기해 `api.reuploadPhoto` 호출. 반려 사유도 함께 표시.

### 1.12 개인 신청 국적(nationality) — ✅ 프론트 연동 완료(2026-08-24)
- **백엔드**: `@ValidNationality` → `Set.of(Locale.getISOCountries())` — **ISO 3166-1 alpha-2**(`KR`/`US`/`JP`)만 통과, 그 외 `INVALID_INPUT`.
- **✅ 수정됨**: `data/countries.ts` 158개국에 `iso` 코드 추가 + `nationalityToIso(ko)` 헬퍼 신설. `ApplyPage.tsx`의 `submit()`이 `nationality: nationalityToIso(draft.applicant.nationality)`로 변환 전송. 이전엔 자유텍스트 "대한민국"을 그대로 보내 개인 신청이 400으로 거절되던 회귀 — 해소됨. (단체 엑셀 경로는 원래부터 ISO 코드 드롭다운이라 무관.)

### 1.13 신청서 드래프트 복원 시 첨부파일이 사라지는데 화면엔 첨부된 것처럼 보임 — ✅ 프론트 수정 완료(2026-08-24)

- **증상**: 법인·단체 신청에서 로고·직인 이미지·엑셀 zip을 전부 첨부하고 "최종 확인" 화면까지 파일명이 정상 표시된 상태로 "신청 제출"을 눌러도 `Error("로고와 제출 ZIP 파일을 다시 선택해 주세요.")`가 뜨며 API 호출 자체가 나가지 않는다(`ApplyPage.tsx:95`, 개인 신청의 사진/학교 로고도 동일 패턴 — `:100-101`).
- **원인**: `useApplicationDraft.ts`가 신청 폼 상태를 `sessionStorage`에 저장하는데, `File` 객체는 JSON 직렬화가 안 돼 저장 시 의도적으로 이름·크기만 남기고 실제 파일(`.file`)은 버린다(`:28-37`, 주석에도 "File objects... intentionally NOT persisted"라고 명시돼 있음). 페이지 새로고침이나 컴포넌트 재마운트가 한 번이라도 발생하면 `sessionStorage`에서 드래프트를 복원하면서 `logoFile`/`sealFile`/`archiveFile`/`faceFile`이 `{name, size}`만 있고 `.file`(실제 바이너리)이 없는 상태가 된다. **화면(최종 확인 등)은 `name`만 보고 정상 첨부된 것처럼 그대로 표시**하므로 사용자는 문제를 전혀 눈치챌 수 없고, 실제 제출 시점에야 이 예외로 걸린다.
- **✅ 수정됨(2026-08-24)**: `useApplicationDraft.ts`가 `sessionStorage` 복원 시 파일 필드(`logoFile`/`sealFile`/`archiveFile`/`faceFile`)를 `delete`로 제거한 뒤 `createEmptyDraft()`와 병합한다. 텍스트 입력값만 복원되고, 실제 `File`이 없는 유령 첨부는 화면에 표시되지 않아 재첨부를 유도한다.

### 1.14 "고객지원"과 "자주묻는 질문"이 서로 다른 FAQ를 보여줌 — ✅ 프론트 수정 완료(2026-08-24)

- **증상**: "고객지원"(`/support`) 메뉴로 들어가면 FAQ 항목이 보이는데, 거기서 "자주묻는 질문"으로 들어가면(또는 "더보기"로 `/faq`로 이동하면) 항목이 하나도 안 보인다.
- **원인 ①(프론트)**: `SupportPage.tsx`의 FAQ 섹션(`:16-24`, `:86-100`)은 `faqs`라는 **하드코딩 목업 배열(7개 항목)**을 그대로 렌더링한다 — 백엔드를 전혀 호출하지 않아 DB 상태와 무관하게 항상 보인다. 같은 페이지의 "고객지원" 드롭다운 안 "자주 묻는 질문" 서브메뉴(`config/navigation.ts:39`)도 별도 페이지가 아니라 이 하드코딩 섹션으로 스크롤만 하는 앵커일 뿐이다(`navigation.ts:29-30` 주석에 명시).
- **원인 ②(데이터)**: 실제 `/faq` 라우트(`FaqPage.tsx`)는 `api.listBoards({ type: "FAQ", size: 100 })`(`:18`)로 `GET /api/boards?type=FAQ`를 정상 호출한다. `BoardController`/`BoardService`(`findByBoardType(BoardType.FAQ, ...)`)도 정상 동작.
- **✅ 완전 해소(2026-08-24)**: (데이터) `DemoDataSeeder.java`가 `Board(NOTICE)` 4·`Board(FAQ)` 8건 시딩(`app.seed-demo-data`, DB 비었을 때만). (프론트) `SupportPage.tsx`의 하드코딩 배열을 `FALLBACK_FAQS`로 강등하고 `useEffect`에서 `api.listBoards({ type: "FAQ", size: 100 })`를 호출해 응답이 있으면 그 값으로 대체(비었거나 오류면 폴백 유지). 이제 `/support`와 `/faq`가 **같은 소스(`GET /api/boards?type=FAQ`)** 를 본다.

### 1.15 관리자 신청 엑셀 내보내기 — ✅ 백엔드·프론트 연동 완료(2026-08-25)
> **정정**: 이제 프론트도 연동됨. `ApplicationsSection`이 `api.exportApplications`로 실제 xlsx 다운로드(개인 다중선택 일괄, 단체 상세 1건씩). 아래는 연동 전 기록.
- **백엔드 현황**: `POST /api/admin/applications/export`(`{applicationIds, type}` → xlsx 바이너리) 구현·테스트·`main` 반영 완료. INDIVIDUAL은 여러 건을 한 시트로, GROUP은 원본 서식을 보존한 채 이름·한자만 append(단, GROUP은 서식 보존 문제로 **한 번에 1건만** 허용 — 2건 이상 보내면 `INVALID_INPUT`).
- **프론트 현황**: `ApplicationsSection.tsx:65-70`의 `exportExcel()`이 여전히 `POST /export`를 호출하지 않고 "백엔드 미구현입니다" 안내 토스트만 띄운다. `services/api.ts`에 이 엔드포인트를 부르는 함수 자체가 없다.
- **⚠️ 프론트 작업 시 주의**: 지금 GROUP 탭 버튼(`전체 엑셀 내보내기`)은 화면에 보이는 **모든 GROUP 신청 id를 한 번에** 보내도록 만들어져 있는데(`exportExcel()`의 `rows.map(...)`), 백엔드는 GROUP은 1건 초과 시 거절한다. GROUP은 "전체 내보내기" 버튼이 아니라 **행(신청 건)별 개별 내보내기 버튼**으로 바꿔야 한다.
- **조치**: `api.ts`에 `exportApplications({applicationIds, type})`(응답은 blob) 추가, `exportExcel()`을 실 호출로 교체, GROUP UI를 건별 버튼으로 변경.

### 1.16 카드 이미지 합성(신청자 정보 → 카드 디자인 자동 매핑) — 🟡 백엔드 엔진만 존재, API·프론트 전부 없음
- **배경(확정 요구사항)**: 관리자가 **카드 디자인을 직접 골라, 실제 카드 이미지 위에 이름·한자·영문명·주소·카드번호 등이 어떻게 인쇄되는지 눈으로 보면서 그 자리에서 값을 확인·수정**할 수 있어야 한다 — 입금 확인 후 카드를 제작하는 화면에서, 신청자 데이터가 선택한 카드 디자인에 자동 매핑된 미리보기가 뜨고 관리자가 편집 가능해야 한다는 뜻. §1.19(성씨)·§1.20(주소)가 최근 신설된 것도 전부 이 화면이 최종적으로 카드에 찍어야 하는 필드를 채우기 위한 선행 작업이다(두 절 사이의 "왜 신설됐는가" 참고).
- **백엔드 현황(2026-08-25)**: 좌표 기반 합성 엔진(`CardImageCompositor`)만 구현 완료 — 신청 정보를 실제 카드 템플릿(명예한국인증/명예시민증/방문증 3종, 디자인 6개씩)에 합성해 PNG를 만드는 로직은 검증됐지만, **이걸 부르는 HTTP 엔드포인트가 없다.** 카드번호 생성 정책, `ApplicationMember`에 결과 저장하는 로직도 미구현.
- **프론트 현황**: 관련 UI 전혀 없음(디자인 선택 화면, 미리보기, 필드 수동 수정 등 전부 신규 구현 필요).
- **⚠️ 정책 확정 필요(선행)**: 직인(관인)·발행처 필드는 시안이 실제 지자체장 공식 직인을 무단 사용하고 있어 **이번 범위에서 제외**하기로 확정(대체안 미정 — 회사 자체 로고? 공란?). 학생증은 가로/세로+고등학교/대학교 조합이 필요해 별도 작업으로 분리.
- **조치**: 백엔드가 API 엔드포인트(카드 미리보기 조회/생성)까지 만든 뒤, 프론트가 관리자 대시보드에 카드 제작 화면(디자인 선택 → 자동매핑 미리보기 → 필드 수정 → 저장) 신규 구현. 현재는 백엔드 엔진 검증 단계라 프론트가 당장 할 일은 없음 — API 계약이 나오면 후속 갱신 예정.

### 1.18 관리자 엑셀 왕복(saju 결과 재업로드) — ✅ 백엔드·프론트 연동 완료(2026-08-25)
> **정정**: 이제 업로드 UI 있음. 단체 신청 상세에 "작명 결과 엑셀 업로드"(`api.applyNamingResult` → `POST .../naming-result`) 연결. 아래는 연동 전 기록.
- **배경**: 두 작명 경로가 공존한다 — (A) 엑셀 왕복(saju 프로그램이 정밀 계산, 해외 출생 시간대·진태양시 보정 포함) vs (B) 인앱 즉석 추천(프론트 `manseryeok` 계산, 시간대 보정 없음). (B)만 연동돼 있어 **더 정확한 (A) 경로가 UI에서 아예 접근 불가능**하다.
- **백엔드 현황**: `POST /api/admin/applications/{id}/naming-result`(saju가 돌려준 "사주이름 포함" 엑셀 업로드 → `ApplicationMember.name/chineseName` 반영) 구현·테스트·`main` 반영 완료.
- **프론트 현황**: `services/api.ts`에 이 엔드포인트를 부르는 함수 자체가 없다(파일 업로드 input도 없음) — `grep` 결과 `naming-result` 문자열이 프론트 어디에도 없음.
- **조치**: `api.ts`에 엑셀 업로드 함수 추가(`multipart/form-data`) + `ApplicationsSection.tsx`에 파일 업로드 버튼/입력 UI 신규 구현(해외 출생 신청자는 이 경로를 쓰도록 안내 문구도 필요해 보임).

### 1.17 이름 추천 — 데이터는 DB로 이관됐으나 조회 API 없음, 프론트는 여전히 mock
- **백엔드 현황(2026-08-25)**: 이름 사전 700개를 `saju_names` 테이블로 이관 완료(`SajuNameSeeder`, 기동 시 자동 적재). 단, 이 데이터를 오행 결핍 기반으로 점수화해 추천하는 API(추천 엔드포인트)는 아직 없음.
- **프론트 현황**: `ApplicationsSection.tsx`의 `mockRecommendations`(`data/adminNamingMock.ts`)가 여전히 프론트 자체 번들 데이터로 추천을 계산한다. 백엔드 DB와는 무관하게 동작 중.
- **조치**: 필요성이 확인되면(추천 로직을 서버로 이전할지) 백엔드가 추천 API를 먼저 만들어야 프론트 연동이 의미가 있음 — 지금은 백엔드 후속 작업 대기.

### 왜 `surname`·`member.address`가 신설됐는가 — §1.16(카드 제작 화면)과 같은 문제

아래 §1.19·§1.20은 별개의 두 버그가 아니라 **같은 배경에서 나온 한 쌍**이다. 확정 요구사항: **관리자가 카드 디자인을 직접 골라, 실제 카드 이미지 위에 이름·한자·영문명·주소·카드번호 등이 어떻게 인쇄되는지 눈으로 보면서 값을 확인·수정할 수 있는 화면**이 필요하다 — 즉 §1.16(카드 이미지 합성) 화면 자체가 요구사항의 핵심이고, `surname`/`address`는 그 화면이 카드에 실제로 찍어야 하는 필드 중 이번에 새로 채워 넣은 두 개일 뿐이다. `ApplicationMember.surname`(성씨, 1-B)과 `member.address`(카드 표기 주소, 1-A)는 전부 이 최종 목표(카드 미리보기·수정 화면)를 만들기 위한 선행 데이터 정비 단계다. 그래서 지금 당장은:
- 관리자 작명 화면(`NamingCard`)에 성씨 입력이 없고,
- 개인 신청서에 카드 표기 주소 입력이 없어서

작명·신청 단계에서부터 이미 데이터가 비어 있고, §1.16 화면이 생기더라도 애초에 채울 값이 DB에 없다. **§1.19·§1.20을 먼저 메워야 §1.16(디자인 선택 → 실제 값이 반영된 카드 미리보기 → 그 자리에서 필드 수정)이 의미가 있다.**

### 1.19 관리자 작명 확정 — `surname`(성씨) 입력 UI가 프론트에 아예 없음 (🔴 blocking, 2026-08-25 실 API 검증으로 발견)

- **배경**: 관리자 작명 확정·카드 제작 계획 1-B(`docs/collab/TODO.md`)에서 `ApplicationMember.surname`을 신설하고, `completeNaming()`이 Application 소속 전 Member의 성씨·이름·의미를 집계 검증하도록 바꿨다(`NAMING_COMPLETE` → 하나라도 누락되면 `NAMING_INCOMPLETE` 400). 실제 서버 기동 후 curl로 확인 완료(정상/실패 케이스 전부 계약대로 동작).
- **프론트 현황**: `services/api.ts:206-207`의 `saveMemberName` 요청 바디 타입이 `{ name, hanja?, reading?, meaning? }`로 **`surname` 필드 자체가 없다.** `ApplicationsSection.tsx`의 `NamingCard`(`choose()`, `:373`)도 `mockRecommendations()`가 만든 이름 객체(성씨 없음)를 그대로 보낸다. 프론트 전체에 성씨 입력 UI가 한 군데도 없다(`grep surname` 0건).
- **영향**: 지금 프론트로 저장한 이름은 `surname`이 영원히 `NULL`이라, "작명 완료 처리" 버튼을 눌러도 **모든 신청에서 항상 `NAMING_INCOMPLETE`로 거절된다** — 관리자가 실제로 작명을 끝낼 방법이 없음.
- **부가①**: `GET /api/admin/applications/{id}/members` 응답(`AdminApplicationMemberResponse`)에도 `surname`/`nameMeaning`/`nameInterpretation`이 노출되지 않아, UI를 만들어도 저장된 값을 다시 읽어올 수 없다.
- **부가②(미확인)**: `completeNaming` 실패 시 응답 `errors[]`엔 Member별 상세(누락 필드)가 담겨 오는데, 프론트 `runStatus`는 `ApiError.message`(요약 문구 "모든 구성원의 작명이 완료되지 않았습니다.")만 토스트로 띄우는 구조라 이 상세를 화면에 보여주는지는 코드 정적 분석으로만 확인했고 실제 브라우저 렌더링으로는 확인하지 못했다(브라우저 자동화 도구 미보유). 관리자가 "어느 멤버가 왜 막혔는지" 알려면 상세 표시가 필요해 보인다.
- **조치**: (백엔드) `AdminApplicationMemberResponse`에 `surname`/`nameMeaning`/`nameInterpretation` 추가. (프론트) `NamingCard`에 성씨 입력란 신설 → `saveMemberName` 바디에 `surname` 포함, `services/api.ts`의 타입도 갱신. 위 "왜 신설됐는가" 참고 — 이 입력란은 결국 §1.16 카드 미리보기 화면의 일부가 되어야 한다.

### 1.20 개인 신청(학생증 제외) — 카드 표기 주소 입력란 자체가 없음 (🔴 blocking 회귀, 2026-08-25 실 API 검증으로 발견)

- **배경**: 1-A에서 `member.address`(카드에 인쇄되는 주소)를 신설 — 학생증이 아니면 **필수**, 학생증이면 있으면 거절(`docs/specs/application/api.md` "API 1" §⑤). 배송지 `receiver.address`와는 별개 값.
- **프론트 현황**: `ApplyPage.tsx:88`의 `member` 요청 객체 빌더에 `address` 필드가 없다. `useApplicationDraft.ts`에도 `applicant.address` 상태 자체가 없고, `StepInfo.tsx`엔 수령인(배송지) 주소 입력란만 있다 — 카드 표기용 주소 입력란이 화면에 아예 없다.
- **영향**: 명예한국인증·명예시민증·방문증(학생증 제외) 개인 신청을 실제 웹사이트로 제출하면 **전부 400 INVALID_INPUT으로 실패한다.** curl로 재현 확인(주소 없이 보내면 400, 넣으면 201).
- **조치**: `useApplicationDraft.ts`에 `applicant.address` 상태 추가, `StepInfo.tsx`에 입력란 신설(학생증이면 숨김), `ApplyPage.tsx` `submit()`의 `member` 객체에 `address` 포함. 위 "왜 신설됐는가" 참고 — 이 값도 결국 §1.16 카드에 인쇄되는 필드다.

> **검증 방법 메모**: §1.19·§1.20 둘 다 로컬 docker-compose 백엔드를 최신 코드로 재기동해 실제 curl 호출(정상+실패 케이스)로 백엔드 계약은 확인했다. 프론트 쪽은 **정적 코드 대조**(요청 바디 타입·컴포넌트 구현에 해당 필드가 존재하는지)로 확인한 것이며, 브라우저 자동화 도구가 없어 실제 화면 클릭까지는 하지 못했다 — 다만 필드 자체가 프론트 코드에 없는 것은 브라우저 확인 없이도 100% 확정적인 결함이다.

### 1.11 신청 폼이 수집하나 백엔드가 저장하지 않는 입력 (프론트 유지 · 백엔드 보강)
프론트 화면에는 입력/표시가 있으나 백엔드 request DTO·도메인에 대응이 없어 값이 서버에 남지 않는 항목. **프론트 UI는 그대로 유지**하고 백엔드 보강 시 연결한다. 상세·조치는 `BACKEND_API_GAPS.md P1-4`.

| 프론트 입력 | 위치 | 백엔드 현황 |
|---|---|---|
| ✅ 입금자명 (저장 완료 2026-08-25) | `StepComplete` | `Application.depositorName` + `PATCH /api/applications/{id}/depositor`로 저장·조회. 입금 확인/취소 **자동화**(결제 게이트웨이)는 별도 미구현 — 관리자 수동 확인(§1.4 confirm-payment)·정적 계좌 안내 유지 |
| 상담확인·유의사항 동의 | `StepType` | 신청 건별 동의 이력 저장 없음 |

> 단체 "신청 수량"은 백엔드가 엑셀 인원 수로 산정하는 정상 계약이라 프론트 입력을 제거함(응답 `totalQuantity` 사용) — 위 목록과 성격이 다름.

---

## 2. 정책상 정적 유지 또는 별도 조회 API

### 공통 원칙 — 고정 config·정적 UI 문구는 프론트 i18n

- 메뉴, 버튼, placeholder, 안내문, 고정 config 문구와 `ApplicationStatus`·`EventType` 등의 화면 표시 label은 백엔드 API 갭으로 분류하지 않는다.
- 백엔드는 안정적인 enum/code 값을 반환하고, 프론트가 `ko`/`en` 리소스 파일(`react-i18next` 등)에서 표시 문구를 선택한다.
- 번역만을 목적으로 고정 config 조회 API나 Gemini 실시간 번역 API를 신설하지 않는다.

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
| ✅ 0 | **학생증 schoolName 요청 추가** | **완료(2026-08-24)** — `ApplyPage.tsx` `submit()`이 학생증일 때 `schoolName`을 개인·단체 모두 전송 | §1.9-b |
| ✅ 0 | **개인 신청 국적(nationality) 입력 방식 변경** | **완료(2026-08-24)** — `data/countries.ts`에 ISO alpha-2 매핑 추가, `nationalityToIso()`로 변환해 전송 | §1.12 |
| ✅ 0 | **드래프트 복원 시 사라진 첨부파일을 재첨부 요구로 초기화** | **완료(2026-08-24)** — `useApplicationDraft.ts`가 복원 시 파일 필드(`logoFile`/`sealFile`/`archiveFile`/`faceFile`)를 제거해 유령 첨부 제거 | §1.13 |
| ✅ 1 | 일반 이메일 회원가입(인증코드 인라인 UI) | **완료(2026-08-24)** — `SignupPage.tsx` 인라인 인증코드 발송→확인(signupToken)→가입 플로우 | §1.1-a |
| ✅ 1 | 로그인·이메일 중복확인·비밀번호 변경 연동 | **완료(2026-08-24)** — 회원가입 시 `checkEmail` 중복확인, `MyPage`에 비밀번호 변경 폼(`PATCH /me/password`) | §1.1-b |
| ✅ 2 | 마이페이지 "내 신청 목록·상세" | **완료** — `listMyApplications`/`listMyInquiries` 실 API 연동, localStorage 제거 | §1.2 |
| ✅ 2 | 신청 취소 진입점 | **완료(2026-08-24)** — `MyPage` 제작내역에 취소 가능 상태 한정 "신청 취소" 버튼 | §1.5 |
| ✅ 3 | 1:1 문의(Inquiry) 전체 연동 | **완료** — `InquiryPage`가 `createInquiry`로 `privacyConsent: true` 전송 | §1.3 |
| ✅ 4 | 신청 조회 응답 `applicationType`으로 재제출 UI 분기 | **완료(2026-08-24)** — `MobileCardPage`가 개인 `photo`/단체 `submitFile` 파트·안내문구 분기 | §1.10 |
| ✅ 4 | "내 정보" 표시 정리 — 회원 유형 제거, 전화번호 노출 추가 | **완료(2026-08-24)** — `MyPage` 조회에서 회원유형 제거·전화번호 노출 | §1.9-a |
| ✅ 5 | 관리자 신청관리 UI | **완료(2026-08-25)** — 목록·상세·구성원·작명확정·선택이력·상태전이 8종(결제확인·검토시작·작명승인 포함)·**엑셀 내보내기**·**작명결과 엑셀 업로드** 전부 실 API 연동. `GET /api/admin/stats`(통계)만 백엔드 미구현 | §1.4 |
| ✅ - | 내 신청 상세(`getMyApplication`) | **완료(2026-08-25)** — `GET /api/my/applications/{id}` → `MyPage` 제작내역 행 펼침 상세(발급방식·결제·반려사유·발송시각) | §1.2 |
| 6 | 행사 회사/로고 필드, 공지 서버검색, 후기 다중이미지 | 행사·후기 백엔드와 프론트 연동 완료. 공지 서버검색만 필요 시 백엔드 후속 구현 | §1.6~§1.8 |
| 7 | 계정복구(아이디/비밀번호 찾기) | **백엔드 구현 및 프론트 연동 완료** — 요청·확인 4개 API와 아이디 찾기/비밀번호 재설정 화면 동작 확인 | §1.1-c |
| 8 | 한국이름 조회 API 전환, 정적 마케팅 CMS화, 하이브리드 목데이터(§5) 정리 | 우선순위 낮음, 필요 시에만 | §2.2, §3, §5 |
| 9 | 카드 이미지 자동 매핑(디자인 선택·미리보기 화면) | 🔴 **백엔드 API 신설 대기** — 합성 엔진(`CardImageCompositor`)만 있고 HTTP 엔드포인트 없음, 프론트는 그 후 신규 | §1.16 |
| 10 | 이름 추천 API 연동 | 데이터는 DB 이관됐으나 추천/조회 API 없음 — 백엔드 선행 | §1.17 |

**진행 원칙**: 0번은 기존 기능을 되살리는 회귀 수정이라 다른 무엇보다 먼저. 1~4번은 백엔드가 이미 준비돼 있어 프론트 작업만으로 끝나는 항목이다. 5번은 구현 완료된 조회·작명 결과 반영부터 연동할 수 있고 상태전이·통계만 백엔드 후속 작업이 필요하다. 6번의 공지 서버검색과 8번 항목은 필요성과 우선순위를 확인한 뒤 진행한다.
