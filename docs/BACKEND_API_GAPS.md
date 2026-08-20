# 백엔드 미구현 API 목록 (프론트 실동작에 필요)

작성일: 2026-08-18 (최종 갱신: 2026-08-18 — 프론트 연동 진행분 반영)
작성 근거: 프론트 API 연동 작업 완료 후, **프론트가 여전히 mock 데이터 / localStorage / 콘솔에 의존하는 화면**을 전수 조사했다. 아래 항목은 "실제 운영 시스템으로 구동되려면 백엔드 API 구현이 필요한데 아직 없는 것들"이다.

- 제안 경로(Method/Path)는 확정 계약이 아니라 제안이며, 상세 계약은 구현 시 확정한다.
- 이미 구현·연동된 도메인의 최신 계약은 `docs/specs/{도메인}/api.md`(예: application, review, board, events)를, 프론트 연동 계약 종합은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`를 기준으로 한다.
- 에러/응답 포맷은 기존 `ApiResponse{success,data,errorCode,errorMessage,errors[]}` 규칙을 따른다.

### ✅ 프론트 연동 완료 현황 (백엔드 API 존재 → 프론트 실연동 완료)

| 도메인 | 엔드포인트 | 프론트 |
|---|---|---|
| 인증/약관 | `POST /api/auth/{terms,refresh,logout}`, OAuth 진입점 | AuthContext, **`/terms` 라우트(TermsPage) 신규** |
| 회원 | `GET·PATCH /api/users/me`, `POST /me/withdraw` | MyPage |
| 신청 | `POST /api/applications`·`/bulk`·`/lookup`, `PATCH /{id}/photo`, `GET /{id}/cards/download` | ApplyPage·LookupPage·MobileCardPage (**학생증 orientation/schoolType·대학교만 학번/학과·서버 totalQuantity·파일 파트·applicant email·`errors[]` 표시·enum 공통 매퍼 반영**) |
| 후기 | `GET·POST /api/reviews`, `GET·PATCH·DELETE /{id}`, **`GET /api/my/reviews`** | ReviewsPage·ReviewDetail·ReviewEditor·**MyPage 내 후기** |
| 공지/FAQ | `GET /api/boards`, `GET /{id}`, admin `POST·PATCH·DELETE` | NoticesPage·NoticeDetail·FaqPage + BoardAdminPanel |
| 행사 | `GET /api/events`, `GET /{id}`, admin `POST·PATCH·DELETE` | EventsPage + EventAdminPanel |

> 공통 규칙 반영: `credentials:include`, 401→refresh 1회 재시도, `ApiError.errors[]` 보존(필드·Excel 행 오류 표시), 0-based 페이지 변환, 카드종류 enum ↔ id 공통 매퍼(`cards.ts`). 위 도메인은 이 문서의 "미구현" 범위가 아니다.

---

## 우선순위 요약

| 우선순위 | 도메인 | 핵심 이유 |
|---|---|---|
| **P0** | 계정 복구(아이디/비밀번호 찾기) | ✅ 백엔드 구현·테스트 완료(2026-08-21, 커밋 `db002a7`/`2d49acd`). 프론트 연동만 남음(P0-1) |
| **P0** | 마이페이지 신청/문의 조회 | 사용자가 자기 신청·문의 내역을 서버에서 못 봄 (내 후기는 연동 완료) |
| **P0** | 관리자 신청 관리 | `/admin` 전체가 localStorage mock. 목록·상세 조회는 백엔드 완료(2026-08-21, `6575d09`), 상태 전이·통계·프론트 연동은 여전히 없음(P0-3) |
| **P0** | 1:1 문의(Inquiry) 도메인 | 도메인 자체가 없음(사용자·관리자 양쪽 mock) |
| **P1** | 관리자 이벤트 전체목록 / 이벤트 필드 확장 / 신청조회 applicationType | 숨긴 글 관리 불가, 협업 로고·갤러리 편집 불가, 단체 재제출 UI 불가 |
| **P1** | 신청 폼 수집값 미저장(결제·학교명·동의) | 입금자명/입금확인·학생증 학교명·신청 동의 이력이 서버에 안 남음 (P1-4) |
| **P2** | PROGRAM 카드 · 정적 CMS | 배포 없이 관리자가 수정하려면 필요 |

---

## P0-1. 이메일 계정 인증 · 계정 복구

> ✅ 2026-08-21 정정: 회원가입·로그인·이메일 중복확인·비밀번호 변경은 `main` 반영 완료다. 계정 복구는 정책 확정 후 구현 진행 중이며, 현재 미커밋 코드가 있으므로 커밋·통합 테스트 전에는 구현 완료로 표시하지 않는다.

현재 상태: `LoginPage`(이메일/비밀번호 로그인, 데모 로그인), `SignupPage`, `AccountRecoveryPage`가 여전히 mock을 쓴다(백엔드는 준비됐으나 프론트 미연동). OAuth2(구글/네이버)만 프론트까지 실제 연동됨. `AuthContext`는 `localStorage["auth-user"]`에 role까지 저장하는 mock 인증.

**✅ 구현 완료(백엔드, 프론트 연동만 남음)**

| Method | 경로 | 목적 | 인증 |
|---|---|---|---|
| POST | `/api/auth/signup` (+ `/signup/email-verification/request`, `/confirm`) | 일반 이메일 회원가입(이메일 인증 포함), 성공 시 HttpOnly 토큰 쿠키 발급 | 없음 |
| POST | `/api/auth/login` | 이메일/비밀번호 로그인, HttpOnly 토큰 쿠키 발급 | 없음 |
| POST | `/api/auth/email/check` | 이메일 중복 확인 | 없음 |
| PATCH | `/api/users/me/password` | 로그인 사용자 비밀번호 변경 | USER |

**⚠️ 정책 확정 완료(2026-08-21), 구현 진행 — 계정 복구(아이디/비밀번호 찾기)**

계약 상세·정책 결정 근거는 `docs/api/auth.md` API 7·API 8, 작업 항목은 `docs/collab/TODO.md`의 RECOVERY-1/RECOVERY-2 참고.

| Method | 경로 | 목적 | 인증 |
|---|---|---|---|
| POST | `/api/auth/recovery/id/request` | 이름·전화 일치 시 가입 이메일로 확인 코드 발송(불일치해도 동일 응답) | 없음 |
| POST | `/api/auth/recovery/id/confirm` | 코드 확인 → 마스킹 이메일 공개 | 없음 |
| POST | `/api/auth/recovery/password/request` | 이메일로 재설정 코드 발송(OAuth 전용/미가입도 동일 응답, 계정 존재 비노출) | 없음 |
| POST | `/api/auth/recovery/password/confirm` | `{requestId, code, newPassword}` 한 번에 — 코드 검증+비밀번호 저장, 성공 시 refresh/access 전체 무효화 | 없음 |

아이디 찾기는 일반 이메일 계정만 대상으로 하며 OAuth 계정은 제외한다. 이름+정규화 전화번호가 정확히 한 계정과 일치할 때만 코드를 발송하고, 중복 일치 시 임의 선택 없이 고객지원을 안내한다. 비밀번호 재설정은 이메일만 받고 임시 비밀번호 없이 10분 유효 코드와 사용자가 정한 새 비밀번호를 한 번에 확인한다.

구조상 `EmailVerificationService`에 계속 기능을 추가하지 않는다. `AccountRecoveryService`가 네 개 복구 API를 조정하고, 가입·아이디 찾기·비밀번호 재설정이 공통으로 쓰는 HMAC/TTL/Lua 원자 검증은 `VerificationChallengeStore`로 분리한다. 재설정 완료 시 `UserService.resetPassword`가 BCrypt 저장과 refresh/access 세션 무효화를 처리하며, 알림 메일은 commit 이후 best effort로 발송한다.

구현 계약은 추가 결정 없이 확정됐다. 가짜 요청도 조회 전에 동일 rate limit을 적용하고, challenge는 `userId`에 결속한다. Access JWT는 초 단위 `iat`가 아니라 `authIssuedAtMillis`와 Redis `revokedAfterMillis`를 비교하며, Redis 세션 검증 장애는 `AUTH_SESSION_VALIDATION_UNAVAILABLE(503)`로 fail-closed 처리한다. 상세 체크리스트는 RECOVERY-1/2를 따른다.

필수 정책: 로그인 아이디는 이메일, 정규화(trim+소문자)+DB UNIQUE, 비밀번호 단방향 해시, 로그인 후 role은 서버 값으로 결정(클라이언트 값 불신), **운영 빌드에서 데모 로그인 제거**. ⚠️ 2026-08-19 정정: "소프트탈퇴 7일 유예 자동복구(`restored:true`)"는 더 이상 유효하지 않다 — 회원탈퇴 정책이 즉시 하드 삭제로 바뀌면서(`docs/collab/user.md`) 유예기간·자동복구 자체가 폐지됐다.
프론트 추가 필요: `/terms` 라우트(신규 OAuth/이메일 가입자 약관 동의 → 기존 `POST /api/auth/terms` 사용).

관련 프론트: `pages/LoginPage`, `pages/SignupPage`, `pages/AccountRecoveryPage`, `features/auth/AuthContext.tsx`

---

## P0-2. 마이페이지 — 내 신청/문의/후기 조회

현재 상태: `MyPage`의 제작 내역/문의 섹션은 아직 mock. "내 후기" 섹션은 **연동 완료**.

| Method | 제안 경로 | 목적 | 인증 | 상태 |
|---|---|---|---|---|
| GET | `/api/my/reviews` | 내가 쓴 후기 목록 | USER | ✅ **백엔드 구현 + 프론트 연동 완료** (`MyReviewController` / `MyPage` 내 후기) |
| GET | `/api/my/applications?page=&size=&status=` | 내 신청 목록 | USER | ⚠️ 백엔드 워킹트리에 존재하나 **미커밋** → 커밋·푸시 확인 후 연동 예정(`FRONTEND_API_INTEGRATION_SPEC.md` §3.6) |
| GET | `/api/my/applications/{id}` | 신청 상세(상태 이력·결제·배송·카드) | 소유자 | ❌ 미구현 |
| GET | `/api/my/bulk-applications/{id}/members` | 단체 신청 구성원/검증 결과 | 소유자 | ❌ 미구현 |
| GET | `/api/my/bulk-applications/{id}/cards/download` | 단체 카드 ZIP 다운로드 | 소유자 | ❌ 미구현 |
| GET | `/api/my/inquiries` | 내 문의 목록 | USER | ❌ 미구현 |
| GET | `/api/my/inquiries/{id}` | 내 문의·답변 상세 | 소유자 | ❌ 미구현 |

참고: 사진 재업로드(`PATCH /api/applications/{id}/photo`)·카드 다운로드(`GET .../cards/download`)는 백엔드에 이미 있고 조회(`lookup`) 결과를 통해 `MobileCardPage`에서 연동돼 있으나, **마이페이지용 "내 신청 목록" API가 없어** 마이페이지에서는 진입점이 끊겨 있다.

관련 프론트: `pages/MyPage`, `pages/MobileCardPage`

---

## P0-3. 관리자 — 신청 관리 / 대시보드

현재 상태: `/admin`(`AdminPage`) 전체가 여전히 localStorage mock(프론트 미연동). 목록·상세 조회는 백엔드 구현 완료(2026-08-21, 커밋 `6575d09`), 상태 전이·통계는 아직 없음. `data/adminMock.ts`가 목데이터 원본.

**✅ 구현 완료(2026-08-21) — 조회 2건**

| Method | 경로 | 목적 |
|---|---|---|
| GET | `/api/admin/applications` | 상태 필터·페이지네이션(마이페이지 목록과 동일 응답 모양, 소유자 무관 전체) |
| GET | `/api/admin/applications/{id}` | 신청자·구성원 수·결제·배송·상태(마이페이지 상세와 동일 응답, 소유권 체크 없음) |

`ApplicationService.listApplicationsForAdmin`/`getApplicationDetailForAdmin` 신규, 기존 `validateAdmin`·`MyApplicationListItemResponse`/`MyApplicationDetailResponse` 재사용(관리자 전용 필드가 아직 없어 DTO 신규 분리 안 함). 상태·유형·카드종류·기간·이름/번호 복합 검색은 이번 범위에 없음(상태 단일 필터만).

**❌ 여전히 없음 — 상태 전이·감사·통계**

| Method | 제안 경로 | 목적 |
|---|---|---|
| PATCH | `/api/admin/applications/{id}/status` | 허용된 상태 전이 수행(`targetStatus`,`reason`, 권한·전이 검증) |
| POST | `/api/admin/applications/{id}/photo-reject` | 사진 반려 사유 등록 |
| POST/PATCH | `/api/admin/applications/{id}/korean-name` | 한국 이름 등록/수정 |
| POST | `/api/admin/applications/{id}/issue-card` | 카드 발급 |
| PATCH | `/api/admin/applications/{id}/tracking` | 배송사·송장번호 등록 |
| GET | `/api/admin/applications/{id}/status-history` | 상태 변경 감사 이력 |
| GET | `/api/admin/dashboard/stats` | 전체/상태별 신청 수·기간 통계(서버 집계) |

정책: 서버에서 관리자 권한·전이 가능 여부 재검증, 관리자 ID·전후 상태·시각 감사 로그, 통계는 프론트에서 전체목록 내려받아 계산 금지. `Application` 엔티티의 상태 전이 메서드(`startReview`/`rejectPhoto`/`approveToNaming`/`completeNaming`/`startProducing` 등)는 이미 존재하나 이걸 호출하는 Controller/Service 오케스트레이션이 없다.

관련 프론트: `pages/AdminPage`, `data/adminMock.ts`

---

## P0-4. 1:1 문의(Inquiry) 도메인

현재 상태: 도메인 자체가 백엔드에 없음. `InquiryPage`(등록), `InquiryDetailPage`(상세), `MyPage`/`AdminPage` 문의 섹션이 `data/inquiries.ts` localStorage 공유.

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| POST | `/api/inquiries` | 문의 접수 | USER 또는 비회원 |
| GET | `/api/my/inquiries`, `/{id}` | 내 문의 목록·상세 | USER/소유자 |
| GET | `/admin/inquiries`, `/{id}` | 관리자 목록/검색·상세 | ADMIN |
| POST | `/admin/inquiries/{id}/answer` | 관리자 답변 등록·알림 | ADMIN |
| PATCH | `/admin/inquiries/{id}/status` | `PENDING`/`IN_PROGRESS`/`COMPLETED` 변경 | ADMIN |

필드: `id,category,requesterUserId,name,email,phone,title,content,status,answer,answeredBy,answeredAt,createdAt,updatedAt`. 개인정보 동의 시각·동의 버전 기록, 비회원 문의는 타인 문의 비노출.

관련 프론트: `pages/InquiryPage`, `pages/InquiryDetailPage`, `data/inquiries.ts`

---

## P1-1. 관리자 이벤트 전체 목록 (숨긴 글 포함)

현재 상태: 공개 목록(`GET /api/events`)은 `visible=true`만 반환. 관리자가 숨긴(`visible=false`) 이벤트를 다시 찾을 방법이 없음 → 관리자 패널에서 비공개 글 재편집 불가.

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/admin/events?type=&page=&size=` | `visible` 무관 전체 목록 | ADMIN |

관련 프론트: `components/admin/EventAdminPanel.tsx`

---

## P1-2. 이벤트 필드/편집 기능 확장

현재 상태: 아래 항목은 백엔드 Event 계약에 없어서 프론트에서 **기능이 죽어 있음**.

| 갭 | 현재 결과 | 필요한 백엔드 변경 |
|---|---|---|
| 협업 `company`/`logoUrl` 필드 없음 | 협업 카드가 로고 대신 카드라벨 텍스트로 표시 | Event에 `company`·`logoUrl`(또는 로고 업로드) 필드 추가 |
| `PATCH`가 갤러리(`images`) 편집 미지원 | 갤러리는 **등록 시에만** 설정, 이후 수정 불가 | `PATCH /api/admin/events/{id}` 갤러리 추가/삭제 지원 |
| 상세 응답에 `visible`/`displayOrder` 없음 | 수정 시 값 유실(공개=true, 순서 초기화) | 상세 응답에 두 필드 포함 또는 관리자 상세 API 제공 |

관련 프론트: `components/admin/EventAdminPanel.tsx`, `pages/EventsPage`

---

## P1-3. 신청 조회 응답에 `applicationType` 추가 (단체 재제출용)

현재 상태: 스펙 §3.7은 사진 재업로드 시 프론트가 ApplicationType으로 part를 분기(개인 `photo` / 단체 `submitFile`)하도록 요구하는데, `ApplicationLookupResponse`에 `applicationType`(INDIVIDUAL/GROUP)이 없다. 카드 다운로드 응답에는 있으나 `COMPLETED` 전용이라 재업로드 대상 상태(`PHOTO_REJECTED`)에서 얻을 수 없다.

| 대상 | 필요 변경 |
|---|---|
| `ApplicationLookupResponse` | `applicationType`(INDIVIDUAL/GROUP) 필드 추가 |

→ 추가되면 프론트 `MobileCardPage`가 개인/단체 재제출 UI를 분기할 수 있다(현재는 개인 사진 재업로드만 구현).

관련 프론트: `pages/MobileCardPage`, `pages/LookupPage`

---

## P1-4. 신청 폼이 수집하나 백엔드가 저장하지 않는 입력 (프론트 유지 · 백엔드 보강 대상)

프론트 화면에는 입력/표시가 있으나 백엔드 request DTO·도메인에 대응이 없어 값이 서버에 남지 않는 항목. **프론트 UI는 그대로 유지**하고, 아래를 백엔드에 추가하면 실제로 연결된다.

| 프론트 입력 | 위치 | 백엔드 현황 | 필요 조치 |
|---|---|---|---|
| **입금자명** + 입금 확인/취소 | `StepComplete`(입금 안내) | 결제·입금(Payment) 도메인 자체 없음. 현재 입금 안내는 정적 계좌(config)이고 입금자명 입력은 어디에도 전송되지 않음 | **Payment 도메인 신규**: 입금자명 저장, 입금 확인(수동/웹훅), 미입금 자동취소(§신청 정책의 3영업일)와 `paymentStatus`(WAITING/CONFIRMED) 연동 |
| **학교명(schoolName)** | `StepInfo`·`StepReview`(학생증) | `ApplicationCreateRequest.MemberRequest`에 학교명 필드 없음 | 발급 카드에 학교명이 필요하면 `member`(개인)·단체 파서에 `schoolName` 필드 추가. 불필요하면 정책상 "표시 전용"으로 확정 |
| **상담확인·유의사항 동의** | `StepType`(체크박스) | 신청 시점 동의 이력을 저장하는 필드/테이블 없음(약관 동의는 `/api/auth/terms`로 계정 단위만 기록) | 신청 건별 동의(항목·시각·정책버전)를 남겨야 하면 신청 생성 시 동의 기록 저장. 단순 UX 게이트면 현행 유지 |

> 참고: 단체 "신청 수량"은 이 목록에서 제외 — 백엔드가 엑셀 인원 수로 산정하는 것이 정상 계약이라 프론트 입력을 제거함(응답 `totalQuantity` 사용).

관련 프론트: `components/apply/steps/StepComplete.tsx`, `StepInfo.tsx`, `StepReview.tsx`, `StepType.tsx`

---

## P2-1. 이벤트 PROGRAM 카드

현재 상태: 이벤트 페이지 상단 "행사 프로그램 소개" 카드 3장(`managed-content:events`)은 실제 이벤트와 별개 콘텐츠이고 대응 API가 없어 localStorage로 유지.

| Method | 제안 경로 | 비고 |
|---|---|---|
| GET / admin CRUD | (예) `/api/site/event-programs` | 정적 유지 정책이면 불필요 |

관련 프론트: `pages/EventsPage`(`programs`), `components/admin/ContentAdminPanel.tsx`

---

## P2-2. 정적 CMS / 사이트 설정

현재 상태: 아래는 코드 정적값. 배포 없이 관리자가 수정해야 하면 API 필요, 코드 배포로만 관리하면 정적 유지 가능.

| 프론트 소스 | 콘텐츠 | 제안 API |
|---|---|---|
| `config/company.ts` | 회사명·연락처·주소·사업자·운영시간·계좌 | `GET /api/site/company`, `PATCH /admin/site/company` |
| `data/partners.ts` | 제휴기관·로고 | `GET /api/partners` + 관리자 CRUD/순서 |
| `data/merchandise.ts` | 상품·문화상품 | `GET /api/merchandise` + 관리자 CRUD |
| `data/social.ts` | SNS 링크·활성여부 | `GET /api/site/social-links` + 관리자 CRUD |
| `data/policies.ts` | 개인정보처리방침·이용약관 | `GET /api/policies/{type}` + 버전 발행 API |
| `SupportPage.tsx` | 제작 이야기/상담 안내 | `GET /api/site/stories` |
| `CompanyPage.tsx` | 회사소개·프로세스·연혁·로드맵 | `GET /api/site/company-page` |
| `GreetingsPage.tsx` | 대표 인사말 | `GET /api/site/greeting` |

약관은 `version`·`effectiveAt`·`publishedAt`·`required` 관리 + 사용자 동의가 어떤 버전을 대상으로 했는지 기록 필요. 계좌정보는 공개범위·수정권한 엄격 제한.

---

## 참고: 프론트 연동 시 남은 계약 확인 사항

실제 API 구현 전/후에 프론트와 함께 확정해야 하는 사항.

1. **카드 종류 ID**: 조회 API는 신설 안 함(확정). 프론트는 `honorary-korean=1, honorary-citizen=2, visitor=3, student=4` 하드코딩 → DB seed 순서와 반드시 일치해야 함.
2. **후기 다중 이미지**: 백엔드는 후기당 이미지 1장만 저장 → 프론트 갤러리를 1장으로 축소해 둠. 다중 이미지가 필요하면 백엔드 스키마 확장 필요.
3. **게시판 검색/페이지네이션**: `/api/boards`에 keyword 검색 파라미터 없음 → 프론트가 `size=100`으로 받아 클라이언트 검색 중. 데이터 증가 시 서버 검색 파라미터 필요.
4. **인증 쿠키**: 프론트는 `credentials:"include"`로 HttpOnly 쿠키 사용 → 운영 환경 HTTPS·`Secure`·`SameSite`·CSRF·CORS 설정 필요.
