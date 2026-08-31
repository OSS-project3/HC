# 프론트 ↔ 백엔드 API 갭 · 목데이터 전환 목록

> **갱신: 2026-08-31(9차, Claude 세션).** §1.15(c)를 정밀 재조사 — `computeMemberSaju()`가 `manseryeok` 라이브러리에 `trueSolarTime`을 안 넘겨서 "입력 시각=KST"로 가정하고 계산한다는 걸 라이브러리 타입 문서 원문으로 확인했고(`true-solar-time.d.ts`), 이건 미완성이 아니라 **한국 외 출생 신청자에게 틀린 사주가 나올 수 있는 정확도 버그**임을 확정했다. 또한 이 보정에 필요한 `longitude`가 `BirthRegionCandidateResponse`/`ManseryeokResolveResponse`/`ManseryeokResult`/`ManseryeokActiveResultResponse` 4곳 모두에 **이미 존재·API로 이미 반환 중**이라는 것도 확인 — 즉 백엔드 작업 없이 프론트만 고치면 되는 상태다. 파일 단위 필요 변경사항을 §1.15(c)에 표로 정리했다. 코드는 수정하지 않았다(백엔드 세션 스코프 — `docs/collab/RULES.md`).
>
> **갱신: 2026-08-27(8차, Claude 세션).** §1.9(b) 학생증 `schoolName` 갭을 완전히 해결 — 원래 `submit()` 한 줄 추가면 끝나는 작업이었으나, 자유텍스트 학교명이 카드 디자인 매칭 오타·예외처리 복잡도와 관리자 수작업 정정 부담을 유발한다는 이유로 School 마스터 엔티티+검색select(+직접입력 폴백) 구조로 스코프를 넓혀 구현했다. 이 프론트 작업은 원래 사용자(백엔드 담당) 범위가 아니었으나 위 이유로 승인받아 함께 진행했다 — 수정한 클래스 전체 목록은 §1.9(b) 참고. 실 브라우저(Playwright) E2E로 개인·단체 양쪽 다 검증했고, 단체 경로는 실제 "신청 제출" 클릭 → 실 API 응답 → DB 반영까지 확인했다. 이 과정에서 단체+학생증 화면의 기존 "학교명" 필드가 실제로는 `Applicant.organizationName`(다른 필드)에 저장되고 있어 단체 학생증 신청이 애초에 화면상으로 불가능했던 버그도 함께 발견·수정했다.
>
> **갱신: 2026-08-27(7차, Claude 세션).** §1.4를 실제 코드 대조로 대폭 정정 — 관리자 신청 조회·상태전이(결제확인/검토시작/작명승인/작명완료/제작시작/카드준비/사진반려/배송)·인앱작명·엑셀작명반영·카드번호 단건·일괄은 **이미 `services/api.ts`에 바인딩되어 `ApplicationsSection.tsx`가 실제로 호출 중**이었다(과거 버전의 "❌ 아직 없음"은 낡은 정보). 반면 실제 관리자 전체 플로우를 처음부터 끝까지 태워보는 통합 검증(2026-08-26~27) 중, 만세력(출생지역검색·timezone판정·확정저장·조회)·카드디자인목록·카드미리보기 6개 API는 백엔드가 실 curl 검증까지 끝났는데도 프론트에 래퍼 함수조차 없다는 걸 신규로 확인해 **§1.15**로 상세 기록했다(요청/응답 필드, 검증 순서, 에러코드까지). 이 감사 과정에서 백엔드의 이름 뜻풀이/훈음 필드 스왑 버그(카드 렌더링 결과로 실제 발견)도 별도로 수정·커밋됨(이 문서 범위 밖, `docs/collab/CHANGELOG.md` 참고).
>
> **갱신: 2026-08-24(6차).** Codex 세션이 오늘 커밋한 변경사항을 실제 코드 대조로 반영: 계정 복구(§1.1-c)·행사 관리자 연동(§1.6)·후기 다중 이미지(§1.8) **연동 완료**로 전환. 공지/FAQ(§1.14)는 데모 시드 추가로 `FaqPage.tsx` 쪽 빈 목록 문제는 해소됐지만 `SupportPage.tsx`가 별도 소스인 근본 문제는 안 고쳐짐. 관리자 신청관리(§1.4)·1:1 문의 관리자 답변(§1.3)은 조회/답변 백엔드 API가 이미 있는데도 프론트가 여전히 `services/api.ts`를 아예 안 부르고 mock만 쓴다는 걸 재확인(오늘 UI만 보강되고 연동은 안 됨). 상세 근거는 각 절의 파일:라인 인용 참고.
>
> **갱신: 2026-08-20(5차).** 회원정보 `address` 수정 정책이 같은 날 두 번 뒤집혔다 — (4차) "이름·전화번호만" → "address도 수정 가능"으로 바뀌었다가, (5차, 이번 갱신) **다시 "이름·전화번호만"으로 최종 확정**됐다(백엔드 코드도 원복 완료). §1.9(a)는 다시 "갭 아님"이며, 추가로 마이페이지 "내 정보" 표시 스펙도 확정됨 — 조회는 이름·전화번호·이메일만(회원 유형 표시 제거), 수정은 이름·전화번호만. `docs/api/user.md` API 5도 함께 원복 반영. 마이페이지 "제작 내역"이 실 API 미연동으로 빈 목록만 뜨는 문제(§1.2)도 이번에 코드 근거와 함께 상세화됨. 3차 갱신 내용(코드 재대조로 `schoolName` 미연동 발견, §1.9 전면 정정)은 그대로 유지. 로그인/이메일중복확인/비밀번호변경(§1.1-b) 오탈 정정은 2026-08-19(2차)에 이미 반영됨. 프론트 연동 계약 종합은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13), 백엔드 API 상세는 `docs/api/auth.md`(API 4~6), 백엔드 미구현 상세는 `docs/BACKEND_API_GAPS.md`와 함께 본다. 프론트의 목데이터/localStorage 사용 자체는 결함이 아니라, 백엔드가 준비된 화면부터 순차 교체하는 방식이다.

> 대상: `frontend/src` 전체 · 근거: `services/api.ts`(실제 호출) ↔ `backend/honor-citizen/.../api/*Controller.java`(실구현) 상호 대조(현재 워킹 트리·`main` 기준).

---

## 0. 한눈에 보기

| 기능 영역 | 프론트 | 백엔드 | 상태 |
|---|---|---|---|
| OAuth 로그인·약관·세션·회원정보 | ✅ 실 API | ✅ 구현 | **🔴 회원정보 타입 갱신 필요** — 백엔드가 `role` 응답 제거·`address` 수정 미지원으로 바뀌었는데 `api.ts`의 `ApiUser`/`updateMe` 타입이 아직 옛날 그대로라 실 관리자 로그인도 `isAdmin=false`로 떨어짐 (§1.9-a). 그 외엔 연동 완료(데모 로컬 로그인만 운영빌드에서 제거 필요) |
| 신청 생성(개인/단체) | ✅ 실 API | ✅ 구현 | **🔴 세 가지 문제로 연동 깨짐**: ① 학생증만 — `schoolName`을 프론트가 요청에 안 보내서 학생증 신청이 400으로 실패 (§1.9). ② **개인 신청 전체** — 국적 입력란 placeholder("대한민국")가 백엔드가 요구하는 ISO 코드(`KR` 등)와 안 맞아서 placeholder대로 입력하면 무조건 400 (§1.12). ③ **법인·단체 신청** — 새로고침/재마운트 후 드래프트 복원 시 첨부파일이 화면엔 그대로 보이는데 실제로는 사라져 있어 제출이 프론트 단에서 막힘, API 호출 자체가 안 나감 (§1.13) |
| 신청 조회/카드다운로드 | ✅ 실 API | ✅ 준비 | 조회 응답에 `applicationType`이 포함돼 개인 `photo`/단체 `submitFile` 재제출 분기 가능. 단체 재제출 UI 연결만 남음. 카드다운로드는 소유자 로그인 전용(비로그인 조회는 데모 폴백) |
| 후기(Review) CRUD + 내 후기 + 다중 이미지 | ✅ 실 API | ✅ 구현(2026-08-24, 0~5장) | **연동 완료** — §1.8 "정책 공백"은 다중 허용으로 확정·구현 완료돼 해소됨 |
| 공지/FAQ(Board) | ⚠️ 화면별로 다름 | ✅ 구현 + 데모 시드 추가(2026-08-24) | **🟡 부분 해소** — `FaqPage.tsx`는 실 API 그대로라 이제 시드 데이터(공지 4·FAQ 8건, `app.seed-demo-data` 켜져 있고 DB가 비어있을 때만 적재)가 있으면 정상 표시됨. `SupportPage.tsx`는 여전히 자체 하드코딩 배열이라 두 화면이 다른 소스인 문제 자체는 안 고쳐짐 (§1.14) |
| 행사(Event) | ✅ 실 API | ✅ 구현(2026-08-21) | **연동 완료(2026-08-24)** — `EventAdminPanel.tsx`가 관리자 전체목록·생성·수정(갤러리·로고 유지/교체/삭제)·삭제까지 실 API로 전환 완료, `company`/`logoUrl` 필드 매핑도 그대로 대입 (§1.6) |
| **일반 이메일 회원가입(인증 포함)** | ❌ 목/로컬 | ✅ 구현·`main` 반영 완료(`bc7d7ce`) | **프론트 신규 구현 필요**(인증코드 인라인 입력 UI) (§1.1) |
| **일반 이메일 로그인·이메일 중복확인·비밀번호 변경** | ❌ 목/로컬 | ✅ 구현·`main` 반영 완료 | **프론트 신규 구현 필요**(연동만 하면 됨, 백엔드 작업 없음) (§1.1) |
| **계정 복구(아이디/비밀번호 찾기)** | ✅ 실 API(2026-08-24) | ✅ 구현 | **연동 완료** — `AccountRecoveryPage.tsx`가 요청→확인(마스킹 이메일/비밀번호 재설정)까지 4개 API 전부 호출 (§1.1) |
| **내 신청 목록·상세(마이페이지)** | ❌ 목(localStorage) | ✅ 구현·`main` 반영 완료(`b5f6140`) | **🔴 연동 안 돼 있어 실사용 불가**("제작 신청 내역이 없습니다" 표시) — API 실 호출로 교체 필요, 상태 라벨·날짜 포맷도 같이 손봐야 함 (§1.2) |
| **1:1 문의(Inquiry)** | ❌ 목(localStorage), 2026-08-24 관리자 화면 UI만 손봄(여전히 mock) | ✅ 구현·`main` 반영 완료(사용자 작성 API·관리자 답변 API 둘 다) | **연동 가능(단, `privacyConsent` 필드 프론트 추가 전송 필요)** — 관리자 답변 화면(`AdminPage.tsx`)도 `api.ts` import 자체가 없어 여전히 mock (§1.3, §1.4) |
| 관리자 신청관리(조회·상태전이·작명·카드번호) | ✅ 실 API | ✅ 구현 | **연동 완료(2026-08-27 재확인)** — 통계(`GET /api/admin/stats`)만 백엔드 자체가 미구현 (§1.4) |
| **관리자 작명 확정·카드 제작(만세력·카드디자인·카드미리보기)** | ❌ 래퍼 자체 없음 | ✅ 구현·실 API 검증 완료 | **🔴 프론트 미착수 + 정확도 버그**(2026-08-31 재확인) — 6개 API 진입점 UI 전부 없음. 그냥 안 뜨는 게 아니라 `computeMemberSaju()`가 진태양시 보정 없이 "입력 시각=KST"로 가정한 **틀릴 수 있는 결과를 정상처럼** 관리자 화면에 보여주고 있음(한국 외 출생자 다수 상정하는 서비스라 실사용 영향 큼). 필요한 `longitude` 값은 백엔드가 이미 API로 내려주고 있어 백엔드 작업 없이 프론트만 고치면 됨 (§1.15(c)) |
| **신청 취소** | ⚠️ 진입점만 | ✅ 구현·`main` 반영 완료(`b5f6140`) | **연동 가능** (§1.5) |
| 공지 서버 검색 | ⚠️ 클라 검색 | ❌ keyword 파라미터 없음 | 필요 시 검색 파라미터 추가 (§1.7) |
| 회원정보 address 수정 | ⚠️ 화면엔 없음(정책상 제거된 상태) | ❌ 미지원(확정 정책) | **갭 아님** — 조회는 이름·전화번호·이메일만, 수정도 이름·전화번호만(§1.9-a) |
| 학생증 schoolName/schoolId | ✅ 검색select+직접입력 | ✅ School 마스터+schoolId 위변조 차단 | **연동 완료(2026-08-27)** — 실 브라우저 E2E(단체는 실제 제출·DB 반영까지) 검증 완료 (§1.9) |
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
- **⚠️ 2026-08-24 재확인**: `AdminPage.tsx`에 "신청 세부내역" 펼침 행, 답변 토스트 등 UI가 추가됐지만 `services/api.ts` import 자체가 없다 — `answerInquiry()`가 여전히 `data/inquiries.ts`의 `saveInquiries()`(localStorage)에만 쓴다. 백엔드 `PATCH /api/admin/inquiries/{id}/answer`는 이미 구현돼 있는데 그대로 안 씀 — UI만 보강되고 실제 연동은 안 된 상태.
- **✅ 백엔드 6개 API 전부 구현·테스트·커밋 완료(2026-08-19)**: `POST /api/inquiries`, `GET /api/my/inquiries`(+`/{id}`), `GET /api/admin/inquiries`(+`/{id}`), `PATCH /api/admin/inquiries/{id}/answer`, `PATCH /api/admin/inquiries/{id}/status`. 상세 계약은 **`docs/specs/inquiry/requirements.md`가 source of truth**(§④ API 목록, §⑤ 처리 흐름, §⑦ Validation).
- **⚠️ 연동 전 프론트가 반드시 반영해야 하는 것 — `privacyConsent` 필드(2026-08-19)**: 개인정보 수집·이용 동의를 서버에서도 강제하기로 정책 확정했다(`requirements.md` §⑤·§⑥·§⑦) — `POST /api/inquiries` 요청 바디에 `privacyConsent: true`가 **필수**다(Bean Validation `@AssertTrue`로 서버가 거절함). 현재 `InquiryPage.tsx`의 동의 체크박스 상태(`agreed`)는 제출 버튼 비활성화에만 쓰이고 `FormData`에는 포함되지 않는다. **이 필드를 프론트가 요청 바디에 추가로 실어 보내지 않으면 문의 등록이 매번 400(`INVALID_INPUT`)으로 거절된다.**
- **연동 시 참고**: `category`는 프론트가 이미 보내는 한글 문자열(제작 신청/결제 및 배송/카드 발급/행사·단체 협업/기타) 그대로 받는다(백엔드가 `@JsonValue`/`@JsonCreator`로 매핑, 프론트 값 변경 불필요). `name`/`email`/`phone`은 계정 값이 아니라 폼에 입력한 값 그대로 저장된다. 목록·상세 API는 페이지네이션이 없다(프론트에 검색/페이지 UI 자체가 없어 전체 나열).

### 1.4 관리자 신청관리·통계 — 조회·상태전이·작명·카드번호는 이미 연동됨, 만세력·카드디자인·카드미리보기만 미연동 (2026-08-27 재대조로 대폭 정정)
- **프론트 사용처**: `pages/AdminPage`, `components/admin/sections/ApplicationsSection.tsx`.
- **✅ 실제로는 이미 연동돼 있음 — 위 "❌ 아직 없음" 기재는 낡은 정보였다.** `services/api.ts`(206~232행)에 아래 API들이 전부 바인딩돼 있고 `ApplicationsSection.tsx`/`AdminPage.tsx`가 실제로 호출한다: 목록/상세(`listAdminApplications`/`getAdminApplicationDetail`), 구성원 목록(`getAdminApplicationMembers`), 상태전이 7종(`confirmApplicationPayment`/`startApplicationReview`/`approveApplicationNaming`/`completeNaming`/`startProducing`/`markCardReady`/`rejectApplicationPhoto`/`dispatchApplication`), 인앱 작명(`saveMemberName`), 엑셀 작명 반영(`applyNamingResult`), 카드번호 단건/일괄(`assignCardNumber`/`assignCardNumbersBatch`), 명단 엑셀 내보내기(`exportApplications`). **§1.15에 정리한 4종(출생지역 검색·만세력 resolve/confirm/조회·카드디자인 조회·카드미리보기)만 여전히 미연동이다.**
- **❌ 여전히 없음**: `GET /api/admin/stats`(통계 대시보드) — 백엔드 자체가 미구현.
- **⚠️ status enum 재확인 필요**: 프론트가 이제 실 API를 쓰므로 옛 `adminMock.ts` enum(`SUBMITTED/CONSULTING/PAYMENT_PENDING/IN_PRODUCTION/COMPLETED/CANCELLED`) 라벨 테이블이 실제로 백엔드 enum(`SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`, 결제상태 별도 `WAITING/CONFIRMED`)으로 완전히 교체됐는지는 `ApplicationsSection.tsx`의 라벨 매핑 코드를 다시 대조해서 확인 필요(이번 대조 범위 밖 — §1.2와 동일한 종류의 리스크).
- **인가**: `/api/admin/**`는 SecurityConfig에서 `hasRole("ADMIN")`으로 라우트 레벨 강제, 각 API도 `validateAdmin()`으로 이중 검증.

### 1.5 신청 취소 — ✅ `main` 반영 완료, 연동 가능
- **프론트 사용처**: 마이페이지/모바일 카드에서 취소 진입점 필요.
- **백엔드 현황**: `POST /api/applications/{id}/cancel`이 `ApplicationController`에 구현 완료, `main`에 커밋·푸시됨(`b5f6140`, 2026-08-19). `FRONTEND_API_INTEGRATION_SPEC.md` §3.7 계약과 동일.
- **조치**: 이제 연동 가능. 가능 상태 `SUBMITTED/REVIEWING/PHOTO_REJECTED`, 이미 취소면 멱등 성공, 응답 `applicationId,status,paymentStatus,refundRequired,cancelledAt`.

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

### 1.9 회원정보 address 수정(⚠️ 정책 재정정, 프론트 신규 구현 필요) · 학생증 schoolName/schoolId(✅ 2026-08-27 해결)

#### (a) 회원정보 조회/수정 — 갭 아님, 확정 정책(2026-08-08, 2026-08-20 재확인 2회)
`PATCH /api/users/me`는 `name`/`phone`만 처리한다. **`address`는 이 API로 수정하지 않는다** — 한때(2026-08-20 세션 초반) 지원하도록 뒤집혔다가, 다시 원래 정책(이름·전화번호만 수정 가능)으로 재확정됐다. `UserUpdateRequest`엔 `address` 필드 자체가 없어 요청 바디에 보내도 무시된다.
**추가로 `GET /api/users/me`(조회) 응답에서도 `role`(회원등급) 필드를 완전히 제거했다** — 마이페이지 "내 정보"에 회원등급 개념 자체가 없어야 한다는 요구를 반영, `UserMeResponse` DTO 자체에서 `role`을 뺐다(단순히 화면에서 안 보이게 하는 게 아니라 백엔드 응답 스키마 변경).
- **⚠️ 프론트가 반드시 확인해야 하는 것 — `AuthContext.tsx`의 `isAdmin`**: `refreshProfile()`이 `api.getMe()`(=`GET /api/users/me`) 응답의 `profile.role`을 읽어 `user.role`/`isAdmin`을 세팅하고 있는데(`AuthContext.tsx:51`), 이제 이 필드가 응답에서 사라진다. 지금은 이 `isAdmin`이 데모 로그인(`loginAsAdmin`) mock 상태에만 실질적으로 쓰이고 실 서버 인가와는 무관한 프론트 전용 값이라 당장 보안 문제는 아니지만, `Header.tsx` 관리자 메뉴 노출·`InquiryDetailPage.tsx`의 `user.role === "admin"` 열람권한 체크가 이 값에 의존하므로 **API 응답으로 `isAdmin`을 판별하던 로직은 더 이상 동작하지 않는다.** 관리자 화면이 실제로 필요하다면 별도 신호(예: 관리자 전용 엔드포인트 접근 성패, 혹은 새 전용 API)로 바꿔야 한다 — 이번 변경 범위엔 대체 수단이 포함돼 있지 않다.
- **마이페이지 "내 정보" 표시/수정 화면 스펙(확정)**:
  - **조회 시 노출 필드**: 이름, 전화번호, 이메일 3개만. `UserMeResponse`는 `id, name, email, phone, address`를 반환한다 — 이름·이메일·전화번호 다 있다. **"회원 유형" 자체가 응답에 없으므로 표시할 수도 없다.**
  - **수정 가능 필드**: 이름, 전화번호 **둘 뿐**. `UserUpdateRequest`엔 `name`, `phone` 두 필드뿐이다. `address`는 조회·수정 어느 화면에도 넣지 않는다.
- 계약 상세는 `docs/api/user.md` API 2·API 5 참고.
- **🔴 신규 갭(2026-08-20, `git pull` 후 재대조로 확인) — `services/api.ts`의 타입이 이 백엔드 변경을 아직 반영 못 함**: `ApiUser` 인터페이스(`api.ts:42`)가 여전히 `role: "USER" | "ADMIN"`을 필수 필드로 선언하고, `updateMe` 파라미터 타입(`api.ts:93`)도 여전히 `address?: string`을 받는다. 백엔드 응답엔 이제 `role`이 없으므로 런타임엔 `profile.role`이 `undefined`로 들어오는데, TS 타입은 항상 존재한다고 우기는 상태 — 컴파일 에러는 안 나지만 **실제 관리자 계정으로 로그인해도 `AuthContext.tsx:51`의 `profile.role === "ADMIN"` 비교가 항상 거짓이 되어 `isAdmin`이 항상 `false`로 떨어진다**(데모 로그인 전용이 아니라 실 API 로그인 시에도 이제 발생). `updateMe`에 `address`를 실어 보내는 건 여전히 컴파일은 되지만 백엔드가 조용히 무시한다. `api.ts`에서 `ApiUser`의 `role` 제거(또는 optional 처리), `updateMe` 파라미터에서 `address` 제거가 필요.

#### (b) 학생증 schoolName/schoolId — ✅ 백엔드·프론트 구현 완료, 실 브라우저 E2E까지 검증 완료(2026-08-27, Claude 세션)

**⚠️ 이 절의 프론트 부분은 원래 사용자(백엔드 담당) 작업 범위가 아니다.** 원래는 `schoolName`을 `submit()`에 한 줄 추가하면 끝나는 작은 수정이었는데, **자유텍스트로 학교명을 받으면 안 되는 이유**(아래 "왜 자유입력 대신 검색select로 갔는가")가 드러나면서 School 마스터 엔티티+검색select UI까지 스코프가 커졌고, 사용자가 "프론트 작업은 원래 내 범위가 아니지만 이 이유 때문에 진행해달라"고 명시적으로 승인해 프론트까지 구현했다. 이후 프론트 담당자가 이 영역을 유지보수할 때 참고할 수 있도록 남긴다.

**왜 자유입력 대신 검색select로 갔는가**: `Application.schoolName`은 학생증 카드 디자인(`CardDesign`) 매칭 키로 그대로 쓰인다(§1.15 이후 신설된 학생증 카드 제작 계획 참고). 자유텍스트 입력을 그대로 받으면:
1. 오타·표기 차이("서울고" vs "서울고등학교")로 실제 등록된 학교와 매칭이 안 되는 케이스가 늘어나 예외 처리(카드 디자인 미발견 등) 분기가 계속 복잡해진다.
2. 오타가 접수된 뒤에는 관리자가 나중에 신청 건을 열어 "이 학교가 실제로 어떤 등록 학교를 말하는지" 수작업으로 다시 확인·정정해야 하는 추가 업무가 별도로 발생한다.

그래서 등록된 학교 목록(`School` 마스터)에서 검색select로 고르게 하고, 목록에 없는 학교만 기존처럼 자유입력으로 받는 하이브리드 구조로 정리했다. 등록 학교를 고르면 서버가 `schoolName`/`schoolType`을 School 값으로 강제 확정해(클라이언트 값 무시) 위변조도 차단한다.

**백엔드 — 신규/수정 클래스**
- 신규: `domain/school/entity/School.java`, `domain/school/repository/SchoolRepository.java`, `domain/school/service/SchoolService.java`, `domain/school/dto/SchoolSearchResponse.java`, `api/SchoolController.java`(`GET /api/schools/search?query=`, 공개 API — 로그인 없이도 학생증 신청서 작성 화면에서 호출), `domain/application/service/ResolvedSchool.java`
- 수정: `domain/application/dto/ApplicationCreateRequest.java`/`BulkApplicationCreateRequest.java`(`schoolId` nullable 필드 추가), `domain/application/entity/Application.java`(`schoolId` 컬럼 + 신규 팩토리 오버로드), `domain/application/service/ApplicationFactory.java`/`ApplicationPersistenceService.java`/`ApplicationService.java`(`resolveSchool()` — schoolId 있으면 School 값 강제, 없으면 기존 직접입력 검증 유지), `infra/security/SecurityConfig.java`(`/api/schools/search` permitAll 추가)

**프론트 — 신규/수정 클래스**
- `services/api.ts`: `searchSchools()`, `SchoolOption` 타입 추가
- `features/apply/types.ts`: `ApplicantInfo.schoolId?: number` 추가
- `components/apply/steps/StepInfo.tsx`: 개인·단체 신청 둘 다 학교명 입력을 검색select(`SearchableSelectField` 재사용)로 교체 + "찾는 학교가 없나요? 직접 입력" 토글. **단체 신청 쪽은 기존 "학교명" 필드가 사실 `Applicant.organizationName`(전혀 다른 백엔드 필드)에 저장되고 있던 걸 실 브라우저 테스트로 발견**해서, 학교 선택 시 `organizationName`과 `schoolName`(+`schoolId`)에 동일한 값이 함께 저장되도록 맞췄다(그 전엔 단체+학생증 신청이 화면상으로는 애초에 성공할 수 없는 상태였음).
- `pages/ApplyPage/ApplyPage.tsx`: `submit()` 요청 바디에 `schoolId` 추가(개인·단체 공통)
- `pages/ApplyPage/ApplyPage.css`: 검색select ↔ 직접입력 토글 링크 스타일(`.field__toggle`) 추가

**검증**: TypeScript 컴파일 통과 + Playwright로 실제 브라우저에서 개인/단체 양쪽 다 (a)등록 학교 검색select 선택 (b)직접입력 두 경로 확인, 단체 경로는 실제 "신청 제출" 버튼 클릭 → 실제 `POST /api/applications/bulk` 201 응답 → DB에 `school_id`/`school_name`/`school_type`/`organization_name`이 정확히 저장되는 것까지 확인 완료(개인 경로는 얼굴 사진 실제 검증 때문에 최종 제출까지는 못 돌려봤고, UI 동작까지만 확인).

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

### 1.13 신청서 드래프트 복원 시 첨부파일이 사라지는데 화면엔 첨부된 것처럼 보임 — 🔴 실사용 중 발견(2026-08-20)

- **증상**: 법인·단체 신청에서 로고·직인 이미지·엑셀 zip을 전부 첨부하고 "최종 확인" 화면까지 파일명이 정상 표시된 상태로 "신청 제출"을 눌러도 `Error("로고와 제출 ZIP 파일을 다시 선택해 주세요.")`가 뜨며 API 호출 자체가 나가지 않는다(`ApplyPage.tsx:95`, 개인 신청의 사진/학교 로고도 동일 패턴 — `:100-101`).
- **원인**: `useApplicationDraft.ts`가 신청 폼 상태를 `sessionStorage`에 저장하는데, `File` 객체는 JSON 직렬화가 안 돼 저장 시 의도적으로 이름·크기만 남기고 실제 파일(`.file`)은 버린다(`:28-37`, 주석에도 "File objects... intentionally NOT persisted"라고 명시돼 있음). 페이지 새로고침이나 컴포넌트 재마운트가 한 번이라도 발생하면 `sessionStorage`에서 드래프트를 복원하면서 `logoFile`/`sealFile`/`archiveFile`/`faceFile`이 `{name, size}`만 있고 `.file`(실제 바이너리)이 없는 상태가 된다. **화면(최종 확인 등)은 `name`만 보고 정상 첨부된 것처럼 그대로 표시**하므로 사용자는 문제를 전혀 눈치챌 수 없고, 실제 제출 시점에야 이 예외로 걸린다.
- **조치(사용자 확정, 2026-08-20)**: 드래프트 복원 시 파일 메타데이터(`name`/`size`)만 남고 실제 `File` 객체가 없는 경우, 화면에 "첨부된 것처럼" 표시하지 말고 **해당 파일 상태를 초기화해서 재첨부를 요구**하도록 수정한다. `useApplicationDraft.ts`의 `sessionStorage` 복원 로직도 텍스트 입력값만 복구하고, **실제 파일 필드(`logoFile`/`sealFile`/`archiveFile`/`faceFile`)는 복구 대상에서 아예 제외**하는 방향으로 처리한다(지금처럼 `{name, size}`만 남겨 화면에 표시하지 않음).

### 1.14 "고객지원"과 "자주묻는 질문"이 서로 다른 FAQ를 보여줌 — 🔴 실사용 중 발견(2026-08-23)

- **증상**: "고객지원"(`/support`) 메뉴로 들어가면 FAQ 항목이 보이는데, 거기서 "자주묻는 질문"으로 들어가면(또는 "더보기"로 `/faq`로 이동하면) 항목이 하나도 안 보인다.
- **원인 ①(프론트)**: `SupportPage.tsx`의 FAQ 섹션(`:16-24`, `:86-100`)은 `faqs`라는 **하드코딩 목업 배열(7개 항목)**을 그대로 렌더링한다 — 백엔드를 전혀 호출하지 않아 DB 상태와 무관하게 항상 보인다. 같은 페이지의 "고객지원" 드롭다운 안 "자주 묻는 질문" 서브메뉴(`config/navigation.ts:39`)도 별도 페이지가 아니라 이 하드코딩 섹션으로 스크롤만 하는 앵커일 뿐이다(`navigation.ts:29-30` 주석에 명시).
- **원인 ②(데이터)**: 실제 `/faq` 라우트(`FaqPage.tsx`)는 `api.listBoards({ type: "FAQ", size: 100 })`(`:18`)로 `GET /api/boards?type=FAQ`를 정상 호출한다. `BoardController`/`BoardService`(`findByBoardType(BoardType.FAQ, ...)`)도 정상 동작.
- **✅ 2026-08-24 데이터 문제는 해소됨**: `DemoDataSeeder.java`가 `Board(NOTICE)` 4건·`Board(FAQ)` 8건을 시딩하도록 추가됐다(`app.seed-demo-data` 프로퍼티, `docker-compose.yml` 기본값 `true`, DB가 비어있을 때만 적재 — idempotent). 이 설정으로 배포되면 `/faq`는 이제 정상 표시된다. **다만 원인 ①(프론트, `SupportPage.tsx`가 여전히 자체 하드코딩 배열)은 그대로 남아있어, "고객지원"과 "자주묻는 질문" 두 화면이 서로 다른 소스를 보여주는 근본 문제 자체는 안 고쳐졌다.**
- **조치(남은 것)**: `SupportPage.tsx`의 하드코딩 `faqs` 배열을 `FaqPage.tsx`와 동일하게 실 API 호출로 교체해야 두 화면이 같은 소스를 보여준다.

### 1.15 관리자 작명 확정·카드 제작(만세력·카드 디자인·카드 미리보기) — 백엔드 전부 구현 완료, 프론트 연동 0% (2026-08-27 신규, 실제 통합 플로우 감사로 발견)

- **배경**: `docs/collab/TODO.md` "관리자 작명 확정·카드 제작 구현 계획" 1-D~2-C가 전부 백엔드 구현·실 API 검증(docker+curl+실제 카드 렌더링 육안 확인)까지 완료됐다. 그런데 실제 관리자 전체 플로우(엑셀 업로드 → 상태전이 → 만세력 확정 → 작명 → 카드번호 → 카드 미리보기)를 처음부터 끝까지 실제로 태워본 통합 검증(2026-08-26~27)에서, §1.4에 있는 조회·상태전이·작명·카드번호는 이미 프론트가 실제로 호출하고 있는 반면 **아래 4개 기능군은 `services/api.ts`에 래퍼 함수 자체가 없다** — 관리자 화면에 진입점(디자인 선택 UI, 발급일자 입력, 미리보기 버튼, 출생지역 검색창)도 없다. 13단계 검증 중 6단계가 이 이유로 막혔다.
- **공통 인가**: 4개 전부 `/api/admin/**` → `hasRole("ADMIN")`(SecurityConfig) + 각 API 자체의 `validateAdmin()` 이중 검증. 인증 실패는 `401`, 권한 없음(ADMIN 아님)은 `403 FORBIDDEN`.

#### (a) 출생지역 검색 — `GET /api/admin/birth-region/search?query={도시명}`
- **용도**: 만세력 계산에 필요한 위경도를 얻기 위해 관리자가 입력한 출생지 도시명(단체 엑셀의 "출생지역" 열 원문, 예: "Seoul"/"Sao Paulo")을 Google Geocoding API로 실시간 조회한다.
- **응답**: `ApiResponse<List<BirthRegionCandidateResponse>>` — 각 항목 `{ displayName: string, latitude: number, longitude: number }`. 후보가 여러 개일 수 있어 관리자가 화면에서 선택해야 한다(예: "Sao Paulo" 검색 시 브라질 상파울루 외 동명 지역이 여러 개 나올 수 있음).
- **실패 시**: `GOOGLE_MAPS_API_KEY` 미설정/Google 쪽 오류는 `GEOCODING_PROVIDER_ERROR`로 변환(1-D에서 명시적으로 처리). 결과 없음은 빈 배열.
- **프론트가 필요한 것**: `api.ts`에 `searchBirthRegion(query: string)` 래퍼 + 관리자 작명 화면에 도시명 검색창·후보 리스트 UI. 아직 전혀 없음.

#### (b) 만세력 timezone/DST 판정(미리보기) — `POST /api/admin/applications/{applicationId}/members/{memberId}/manseryeok/resolve`
- **용도**: (a)에서 얻은 위경도로 해당 생년월일시의 실제 timezone/DST 상태를 판정한다. **DB에 아무것도 저장하지 않는 순수 계산**이라 몇 번을 호출해도 안전하다.
- **요청 바디** (`ManseryeokResolveRequest`): `{ latitude: number(필수), longitude: number(필수), timezoneId?: string, selectedOffset?: string }`. `timezoneId`를 안 보내면 서버가 위경도로 Google Time Zone API를 호출해 자동 판정한다. DST 중복 시각이라 여러 후보가 나온 뒤 관리자가 하나를 고르면 `selectedOffset`을 실어 재호출해 확정한다.
- **응답** (`ManseryeokResolveResponse`): `{ status: "EXACT"|"NONEXISTENT_LOCAL_TIME"|"AMBIGUOUS_LOCAL_TIME"|"UNKNOWN_TIME", timezoneId, longitude, selectedOffset, utcInstant, candidates: [{offset, utcInstant}, ...] }`. `status`가 `AMBIGUOUS_LOCAL_TIME`이면 `candidates`에 2개 이상이 오고, 관리자가 그중 하나를 골라야 (c)로 넘어갈 수 있다. `UNKNOWN_TIME`은 출생시간 자체가 미입력일 때(시주는 계산에서 항상 제외).
- **프론트가 필요한 것**: `resolveManseryeokBirthTime(applicationId, memberId, body)` 래퍼 + DST 중복 시 후보 선택 UI. 전혀 없음.

#### (c) 만세력 확정 결과 저장 — `POST /api/admin/applications/{applicationId}/members/{memberId}/manseryeok`
- **⚠️ 핵심 정책 — 사주(四柱) 계산 자체는 백엔드가 하지 않는다(admin-saju.md 원칙)**: 실제 만세력/사주팔자 계산 로직은 `manseryeok` npm 패키지(프론트 전용)에만 있다. 이 API는 프론트가 **자체 계산한 결과를 그대로** 받아 이력 보존 방식으로 저장하고, 백엔드가 검증 가능한 부분(`timezoneId`+생년월일시로 재계산했을 때 `selectedOffset`/`utcInstant`가 요청값과 일치하는지, `TimeAccuracy.EXACT`일 때만)만 무결성 검증한다. `confirmedPillars`(사주 8자) 자체는 재계산·검증하지 않고 그대로 저장한다.
- **요청 바디** (`ManseryeokConfirmRequest`): `{ timezoneId(필수), longitude(필수), selectedOffset?, utcInstant?(EXACT일 때 필수), timeAccuracy: "EXACT"|"PARTIAL"|"UNKNOWN"(필수), confirmedPillars: {year:{stem,branch}, month:{...}, day:{...}, hour:{...}}(필수, 확정된 주만), uncertainPillars?: string[](확정 못한 주 이름, 예 ["hour"]), elementCounts?: {목,화,토,금,수}, calculationEngineVersion(필수, 예 "manseryeok@2.0.0"), inputHash(필수, 재계산 입력 동일성 추적용) }`.
- **정책**: 재확정 시 기존 활성 결과는 `active=false`로 비활성화되고 새 row가 활성으로 저장된다(이력 보존, 덮어쓰지 않음). `EXACT`가 아니면 무결성 재검증을 하지 않고 그대로 신뢰해 저장한다.
- **프론트가 필요한 것**: 이 흐름 전체를 태우려면 프론트가 **`manseryeok` 패키지로 실제 사주를 계산하는 코드부터 새로 있어야 한다** — 현재 유일하게 있는 `frontend/src/lib/saju.ts`의 `computeMemberSaju()`는 (b)의 `utcInstant`/경도를 전혀 받지 않고 로컬 시각만으로 계산하는 **작명 추천 화면 전용 mock 미리보기 함수**라 이 API에 그대로 이어붙일 수 없다(진태양시 보정 없음, timezone 확정 결과 미반영). 즉 단순 API 바인딩 문제가 아니라 **계산 로직 자체를 새로 작성**해야 하는 항목.

- **⚠️ 갱신(2026-08-31) — (c)가 왜 "나중에 정교화" 항목이 아니라 지금 당장 고쳐야 하는 정확도 버그인지, 정확한 근거와 함께 정리**:

  **버그 자체(확인된 사실, 추정 아님)**: `computeMemberSaju()`(`frontend/src/lib/saju.ts:9-44`)가 `calculateFourPillars()`를 호출할 때 `trueSolarTime` 옵션을 아예 안 넘긴다. `manseryeok` 라이브러리 타입 문서(`frontend/node_modules/manseryeok/dist/time/true-solar-time.d.ts:10-11`)에 원문 그대로 이렇게 적혀 있다: **"진태양시 옵션을 주지 않으면(기본) longitude=135, 균시차=0, 서머타임 미적용이 되어 결과적으로 '입력 벽시계 = KST 표준시'로 동작한다."** 즉 지금 `ApplicationsSection.tsx:400`이 관리자 화면에 보여주는 `realSaju`는 **신청자가 어느 나라에서 태어났든 그 시각을 한국 표준시로 취급해서 계산한 결과**다. 게다가 백엔드가 이미 구현해둔 (a)~(d) 흐름(출생지역검색→timezone/DST 판정→확정저장) 자체도 이 화면에서 호출되지 않는다(§1.15 상단 배경 문단 참고) — 즉 타임존 판정도, 진태양시 보정도 둘 다 안 거친 값이다.

  **왜 "언젠가 정교화"가 아니라 지금 문제인지**: 이 서비스의 핵심 기능이 "출생지역 검색"이라는 것 자체가, 신청자 다수가 한국이 아닌 다른 나라·타임존에서 태어났다는 것을 전제한다. 그런 신청자에게 이 계산은 단순히 "덜 정밀한" 수준이 아니라 **연주(년) 경계·일주(日) 경계·시주(時) 경계가 실제와 다르게 나올 수 있는 오답**이다 — 절기(節氣)가 걸치는 시점 근처의 출생이면 월주까지도 바뀔 수 있다. 이 값이 그대로 오행 결핍 판정(→이름 추천 점수)과 카드 뒷면 "이름풀이"·띠 이미지까지 흘러간다. "만세력 mock" 배지(`ApplicationsSection.tsx:452`)가 화면에 뜨긴 하지만, `computeMemberSaju()`가 값을 반환하는 한(계산 자체는 "성공"하므로) 이 배지는 절대 안 뜬다 — 관리자 입장에선 **틀린 값이 "정상 계산됨"으로 보인다.**

  **왜 진태양시 보정을 백엔드가 아니라 프론트가 맡아야 하는지, 그리고 왜 지금 그게 어렵지 않은지**:
  - 사주 산출(간지 계산) 자체가 이미 100% 프론트 책임으로 확정된 아키텍처다(`admin-saju.md`) — 진태양시는 별도 계산 단계가 아니라 이미 프론트가 호출 중인 `calculateFourPillars()`에 옵션 하나(`trueSolarTime: {longitude}`)를 더 넘기는 것뿐이다. 백엔드가 떠맡으려면 `manseryeok`이 내장한 절기·균시차·음력변환 계산을 Java로 통째로 재구현/포팅해야 해서, 이미 존재하는 로직을 언어만 바꿔 이중 유지보수하는 데다 두 구현이 같은 입력에 다른 결과를 낼 위험까지 생긴다.
  - **필요한 데이터는 이미 백엔드에 전부 있다** — 신규 백엔드 작업이 필요 없다: `BirthRegionCandidateResponse.longitude`((a) 응답), `ManseryeokResolveResponse.longitude`((b) 응답), `ManseryeokResult.longitude`(DB 컬럼), `ManseryeokActiveResultResponse.longitude`((d) 응답) — 넷 다 이미 존재하고 API로 이미 내려주고 있다. `ManseryeokResult.inputHash`의 주석조차 "계산 입력(생년월일시+timezoneId+**longitude**+엔진 버전)의 해시"라고 되어 있어,애초에 이 값이 계산에 쓰일 것으로 설계돼 있었다. 즉 프론트가 (b) 또는 (d) 응답의 `longitude`를 그대로 `trueSolarTime`에 넣기만 하면 된다 — 새 API도, 새 필드도 필요 없다.

  **지금 당장 필요한 변경(파일 단위, 이게 "diff"에 해당)**:
  | 파일 | 지금 | 바뀌어야 하는 것 |
  |---|---|---|
  | `services/api.ts` | (a)~(d) 4개 API 래퍼 자체가 없음 | `searchBirthRegion`/`resolveManseryeokBirthTime`/`confirmManseryeokResult`/`getActiveManseryeokResult` 4개 추가(§1.15 (a)~(d) 계약 그대로) |
  | `lib/saju.ts` | `computeMemberSaju(birthDate, birthTime)` — `trueSolarTime` 미사용, 로컬 시각을 KST로 취급 | (b)/(d) 응답의 `longitude`(+`utcInstant`)를 받아 `calculateFourPillars(..., trueSolarTime: { longitude })`로 계산하는 새 함수로 교체(또는 병행) — 지금처럼 `birthDate`/`birthTime`만으로 즉석 계산하는 경로는 백엔드 확정 흐름 없이는 호출되지 않게 정리 |
  | `components/admin/sections/ApplicationsSection.tsx` | `NamingCard`가 `computeMemberSaju()`(mock 폴백 포함)만 사용, (a)~(d) API 호출·UI 진입점 전혀 없음 | 출생지역 검색창 → timezone 판정(DST 후보 선택) → 확정 저장 → 활성 결과 조회까지 이어지는 실제 흐름 UI 추가, `realSaju`/`mockSaju` 폴백 로직 제거(또는 "미확정" 상태로 명확히 구분 표시) |

  **결론**: 이건 "미연동"이 아니라 "**연동 안 된 채로 틀린 값을 정상처럼 보여주고 있다**"는 차이가 있다 — 값이 아예 안 뜨는 게 아니라 그럴듯한 오답이 뜬다는 점에서 우선순위를 다시 볼 필요가 있다.

#### (d) 활성 만세력 결과 조회 — `GET /api/admin/applications/{applicationId}/members/{memberId}/manseryeok`
- **용도**: 현재 활성(active=true)인 확정 결과를 다시 읽어온다. 화면에 "이미 확정된 만세력" 표시, 또는 카드 미리보기 전 확정 여부 확인 용도.
- **응답** (`ManseryeokActiveResultResponse`): `{ timezoneId, longitude, selectedOffset, utcInstant, timeAccuracy, confirmedPillars, uncertainPillars, elementCounts, tzdbVersion, calculationEngineVersion, calculatedAt }`. 활성 결과가 없으면 `404 NOT_FOUND`.
- **프론트가 필요한 것**: `getActiveManseryeokResult(applicationId, memberId)` 래퍼. 전혀 없음.

#### (e) 카드 디자인 목록 조회 — `GET /api/admin/card-designs?cardTypeId={id}&active={true|false}`
- **용도**: 카드 미리보기·최종 발급 전에 관리자가 카드 디자인(같은 카드종류 내 여러 후보 중 1개)을 선택한다.
- **응답**: `ApiResponse<List<CardDesignResponse>>` — 각 항목 `{ id, designNumber: number, name: string, orientation: "LANDSCAPE"|"PORTRAIT", isDefault: boolean, active: boolean }`. `active` 쿼리파라미터 생략 시 전체(비활성 포함), `true`면 활성만.
- **정책**: `cardTypeId`가 STUDENT(학생증)면 `UNSUPPORTED_CARD_TYPE`으로 거절 — 학생증은 학교별 커스텀 업로드 디자인 체계라 이 카탈로그 방식 자체가 적용되지 않는다(별도 설계, 아직 미착수).
- **프론트가 필요한 것**: `listCardDesigns(cardTypeId, active?)` 래퍼 + 디자인 선택 UI(썸네일/이름/가로세로 표시). 전혀 없음.

#### (f) 저장 없는 카드 미리보기 — `POST /api/admin/applications/{applicationId}/members/{memberId}/card-preview`
- **용도**: DB row·S3 object를 전혀 만들지 않고, 실제 저장된 신청 데이터(이름·한자·주소·카드번호·발급일자·만세력 확정 연주→띠 캐릭터)로 카드 앞/뒷면 PNG를 즉시 렌더링해 반환한다. 관리자가 최종 발급 전 눈으로 확인하는 용도.
- **요청 바디** (`CardPreviewRequest`): `{ cardDesignId(필수, (e)에서 고른 id), issueDate: "YYYY-MM-DD"(필수) }`.
- **⚠️ 계약 변경(2026-08-27)**: 원래 `side: "FRONT"|"BACK"`를 받아 한쪽만 반환했으나(호출 2번 필요), 공통 DB조회·검증·만세력조회·S3다운로드(사진/로고/직인)가 side와 무관하게 매번 중복 실행되는 낭비가 있어 **한 번의 호출로 앞/뒤를 함께 반환**하도록 바꿨다. 요청에서 `side` 필드 삭제, `CardSide` enum도 삭제(다른 어디서도 안 쓰였음).
- **응답** (`ApiResponse<CardPreviewResponse>`, JSON): `{ front: string(base64 PNG), back: string(base64 PNG) }`. 예전엔 `image/png` raw 바이너리(`ApiResponse` 미포장)였으나, 이미지 2개를 한 응답에 담아야 해서 이 API 전체가 원래 쓰는 JSON envelope 패턴으로 통일했다(`export`처럼 다운로드가 목적이 아니라 화면에 바로 보여주는 미리보기라 base64+`<img src="data:image/png;base64,...">`가 자연스럽다).
- **검증 순서(실패하면 여기서 막힘, 프론트가 각 단계별 안내 문구를 준비해야 함, side 제거와 무관하게 그대로)**: 관리자 권한 → Application 존재 → **상태가 정확히 `PRODUCTION_READY`가 아니면 거절**(`INVALID_STATUS_TRANSITION` — §1.4의 `completeNaming` 이후 상태) → Member가 해당 Application 소속인지 → 카드 디자인 존재+카드종류 일치+`active=true`(`CARD_DESIGN_NOT_FOUND`/`CARD_DESIGN_MISMATCH`) → **발급일자가 신청일(`createdAt`) 이상·신청일+3개월 이하**(`CARD_ISSUE_DATE_OUT_OF_RANGE`) → 작명 완료(surname/name/nameMeaning 전부 non-blank, 아니면 `NAMING_INCOMPLETE`) → 카드번호 존재(`CARD_NOT_READY`) → **단체 신청은 로고+직인 둘 다 필수**(`CARD_ISSUER_ASSETS_MISSING`, `Application.isIndividual()`로 판정 — 개인은 이 검사 자체를 건너뜀) → **만세력 확정 결과 존재 + 연주(year)가 `uncertainPillars`에 없어야 함**(`MANSERYEOK_NOT_CONFIRMED` — (c)가 선행돼야 한다는 뜻).
- **띠 캐릭터 자동 삽입**: 이 API가 (c)에서 저장된 연주 지지(예: "사")를 읽어 12간지 동물 PNG를 카드에 자동으로 그린다(생년월일에서 직접 계산하는 게 아니라 **확정된 만세력 결과 경유** — 그래서 (c)가 먼저 끝나 있어야 함).
- **프론트가 필요한 것**: `getCardPreview(applicationId, memberId, body)` 래퍼(JSON 응답이라 다른 API와 동일한 `request()` 헬퍼 그대로 사용 가능 — base64 문자열을 `<img src="data:image/png;base64,${front}">`에 바로 꽂으면 됨, blob/Object URL 처리 불필요) + 미리보기 버튼·이미지 표시 UI. 전혀 없음.
- **검증(2026-08-27)**: `CardPreviewServiceTest` 18개(계약 변경으로 앞/뒤 성공 테스트 2개→1개로 병합, 나머지 검증/거절 테스트 전부 유지) 전부 통과. 실제 docker 재빌드 후 curl로 JSON 에러 응답 경로(`APPLICATION_NOT_FOUND`)까지 확인.

**정리하면 이 순서가 실제 관리자 사용 흐름이다**: (a) 도시명 검색 → (b) timezone 판정(DST 중복이면 후보 선택 후 재호출) → 프론트가 `manseryeok` 패키지로 진태양시 보정 사주 계산 → (c) 결과 저장 → (e) 디자인 선택 → (f) 발급일자 입력 후 미리보기. **6개 API 모두 백엔드는 실제 curl 검증까지 끝났지만 프론트는 어느 하나도 시작 전이다.**

### 1.11 신청 폼이 수집하나 백엔드가 저장하지 않는 입력 (프론트 유지 · 백엔드 보강)
프론트 화면에는 입력/표시가 있으나 백엔드 request DTO·도메인에 대응이 없어 값이 서버에 남지 않는 항목. **프론트 UI는 그대로 유지**하고 백엔드 보강 시 연결한다. 상세·조치는 `BACKEND_API_GAPS.md P1-4`.

| 프론트 입력 | 위치 | 백엔드 현황 |
|---|---|---|
| 입금자명 + 입금 확인/취소 | `StepComplete` | 결제·입금(Payment) 도메인 없음(입금 안내는 정적 계좌) |
| 상담확인·유의사항 동의 | `StepType` | 신청 건별 동의 이력 저장 없음 |

> 단체 "신청 수량"은 백엔드가 엑셀 인원 수로 산정하는 정상 계약이라 프론트 입력을 제거함(응답 `totalQuantity` 사용) — 위 목록과 성격이 다름.

---

## 2. 정책상 정적 유지 또는 별도 조회 API

### 공통 원칙 — 고정 config·정적 UI 문구는 프론트 i18n

- 메뉴, 버튼, placeholder, 안내문, 고정 config 문구와 `ApplicationStatus`·`EventType` 등의 화면 표시 label은 백엔드 API 갭으로 분류하지 않는다.
- 백엔드는 안정적인 enum/code 값을 반환하고, 프론트가 `ko`/`en` 리소스 파일(`react-i18next` 등)에서 표시 문구를 선택한다.
- 번역만을 목적으로 고정 config 조회 API나 Gemini 실시간 번역 API를 신설하지 않는다.
- 예외적으로 관리자가 작성하는 공식 콘텐츠인 공지사항·FAQ·행사는 `Accept-Language`에 따른 백엔드 lazy translation + DB cache 정책을 적용한다. 

### 현재 확인된 미번역 정적 UI

| 화면/요소 | 대표 코드 위치 | 필요한 조치 |
|---|---|---|
| Support 페이지 | `pages/SupportPage/SupportPage.tsx` | 페이지 제목·안내·FAQ 등 고정 문구를 `ko/en` 리소스로 이동 |
| 신청 유형 선택 | `components/apply/steps/StepType.tsx` | heading 번역 키 적용 |
| 개인 신청 | `StepType.tsx`, `StepInfo.tsx`, `StepFiles.tsx`, `StepReview.tsx` | 신청 유형 label 번역 키 적용 |
| 법인·단체 신청 | `StepInfo.tsx`, `StepFiles.tsx`, `StepReview.tsx` | 신청 유형 label 번역 키 적용 |
| 안내사항 | `StepType.tsx`, `StepComplete.tsx` | 제목과 본문을 함께 번역 |
| 다음 버튼 | `StepType.tsx`, `StepInfo.tsx`, `StepFiles.tsx` | 공통 button key로 통합 |
| 메인 페이지 소개 문구 | `components/home/HeroSection.tsx` 및 메인 섹션 | 제목·설명 문구를 `ko/en` 리소스로 이동 |
| 서비스 핵심 소개 문구 | `components/home/ServiceCoreSection.tsx` | eyebrow·제목·설명 전체에 번역 키 적용 |

위 항목은 모두 고정 UI 문구이므로 백엔드 변경 없이 프론트 i18n으로 처리한다.

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
| — | ~~학생증 schoolName 요청 추가~~ | **완료(2026-08-27)** — School 마스터+검색select까지 포함해 해결됨 | §1.9-b |
| 🔴 0 | **개인 신청 국적(nationality) 입력 방식 변경** | placeholder("대한민국")가 백엔드 요구값(ISO 코드)과 어긋남 — 국가 선택 드롭다운으로 교체하거나 최소한 placeholder만이라도 수정 | §1.12 |
| 🔴 0 | **드래프트 복원 시 사라진 첨부파일을 재첨부 요구로 초기화** | `useApplicationDraft.ts`가 `sessionStorage`에 파일을 복구 못 하는데도 화면은 첨부된 것처럼 표시 — 실제 `File` 없는 필드는 복원하지 말고 상태 초기화, `sessionStorage`엔 텍스트 입력값만 복구 | §1.13 |
| 1 | 일반 이메일 회원가입(인증코드 인라인 UI) | 백엔드 완료, 프론트 신규 구현 필요 | §1.1-a |
| 1 | 로그인·이메일 중복확인·비밀번호 변경 연동 | 백엔드 완료, 프론트 연동만 하면 됨(백엔드 작업 없음) | §1.1-b |
| 2 | 마이페이지 "내 신청 목록·상세" | 백엔드 완료, `adminMock.ts` localStorage → 실 API 교체 | §1.2 |
| 2 | 신청 취소 진입점 | 백엔드 완료, 연동 가능 | §1.5 |
| 3 | 1:1 문의(Inquiry) 전체 연동 | 백엔드 완료, `privacyConsent` 필드만 요청에 추가하면 됨 | §1.3 |
| 4 | 신청 조회 응답 `applicationType`으로 재제출 UI 분기 | 백엔드 완료, 프론트가 개인 `photo`/단체 `submitFile` 파트 분기만 하면 됨 | §1.10 |
| 4 | "내 정보" 표시 정리 — 회원 유형 제거, 전화번호 노출 추가 | 백엔드 변경 없음(확정 정책 §1.9-a 반영). 조회: 이름·전화번호·이메일만 표시(현재 `MyPage.tsx`엔 전화번호 미표시 + 회원유형 표시 중 — 둘 다 수정 필요). 수정: 이름·전화번호만(현행 유지) | §1.9-a |
| 5 | 관리자 통계 대시보드(`GET /api/admin/stats`) | **백엔드도 아직 없음**(공동 대기) | §1.4 |
| 5 | 관리자 작명 확정·카드 제작(만세력·카드디자인·카드미리보기) | 백엔드 완료. 프론트는 API 바인딩 6개 + UI 신규 + **사주 계산 로직(진태양시 보정) 신규 작성**까지 필요해 다른 항목보다 작업량 큼 | §1.15 |
| 6 | 행사 회사/로고 필드, 공지 서버검색, 후기 다중이미지 | **정책 결정 대기** — 결정 후 백엔드·프론트 함께 진행 | §1.6~§1.8 |
| 7 | 계정복구(아이디/비밀번호 찾기) | **백엔드 구현 진행 중**(정책·API 계약 확정 완료, `docs/api/auth.md` API 7·8) — 커밋·검증 후 프론트 3단계 화면(아이디 찾기)·2단계 화면(비번 재설정) 신규 필요 | §1.1-c |
| 8 | 한국이름 조회 API 전환, 정적 마케팅 CMS화, 하이브리드 목데이터(§5) 정리 | 우선순위 낮음, 필요 시에만 | §2.2, §3, §5 |

**진행 원칙**: 0번은 기존 기능을 되살리는 회귀 수정이라 다른 무엇보다 먼저. 1~4번은 백엔드가 이미 준비돼 있어 프론트 작업만으로 끝나는 항목(가장 빠르게 갭을 줄일 수 있음). 5번 이후는 백엔드 작업이나 정책 결정이 먼저 필요해 프론트 혼자 진행할 수 없는 항목.
