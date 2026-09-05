# 프론트 ↔ 백엔드 API 갭 · 목데이터 전환 목록

> **갱신: 2026-09-05(13차, Claude 세션) — 관리자 통계 API(`GET /api/admin/stats`) 백엔드 구현 완료.** 12차 기록의 "§1.4(관리자 통계 API)는 이번에도 재확인 결과 그대로 백엔드 미구현" 서술은 이 갱신으로 낡았다 — 바로 이어서 이번 세션에서 구현했다. 응답은 `totalApplications`/`individualApplications`/`groupApplications`/`totalInquiries`/`pendingInquiries`/`completedInquiries` 6개(정책 확정: 상태별·카드종류별 분포, 기간 필터는 이번 범위 아님). `OverviewSection`(`AdminPage.tsx`)이 `listAdminApplications({size:100})`를 `.filter()`해 개인/단체 신청 수를 세던 부정확한 방식(100건 초과 시 틀림)을 이 API로 교체하면 된다. `services/api.ts`에 래퍼 없음 — 프론트 미착수. §0/§1.4/§6을 갱신했다.
>
> **갱신: 2026-09-05(12차, Claude 세션) — 백엔드 신규 구현 2건 반영(공지 서버 검색, 관리자 카드 다운로드). 둘 다 프론트는 아직 미착수.** ① **공지 서버 검색(§1.7)** — `GET /api/boards`에 `searchType`(`ALL`/`TITLE`/`CONTENT`)·`keyword` 파라미터가 추가됐다(`72dad09`, `Review`의 검색 패턴을 그대로 재사용). `frontend/src/services/api.ts`의 `listBoards` 타입은 여전히 `{ type?, page?, size? }`뿐이라(직접 grep 확인) 프론트는 아직 이 파라미터를 안 쓴다 — §0/§1.7/§6을 "백엔드 완료, 프론트 파라미터 추가만 남음"으로 갱신했다. ② **관리자 카드 다운로드(신규 §1.18)** — `ApplicationMember.cardFrontPath`/`cardBackPath` 존재 여부로 판단하는(=`ApplicationStatus`와 무관, `PRODUCING` 중에도 허용) 관리자 전용 카드 다운로드 API 2개(`GET /api/admin/applications/{id}/cards/download`(전체 ZIP)·`GET /api/admin/applications/{id}/members/{memberId}/cards/download`(개별, 재인쇄용))가 신규 구현됐다(`a0f2e85`). `services/api.ts`에 관련 함수가 전혀 없어(grep 0건) 프론트 진입점 자체가 없다 — §0에 신규 행 추가, §1.18 신설. §1.4(관리자 통계 API)는 이번에도 재확인 결과 그대로 백엔드 미구현이라 변경 없음.
>
> **갱신: 2026-09-02(11차, Claude 세션) — §6 표 전체 재검증.** §6("남은 프론트엔드 작업 전체 목록")이 §0/§1.x보다도 더 낡아 있었다(계정복구를 "백엔드 구현 진행 중"으로, 이미 연동 완료된 행사·후기다중이미지를 "정책 결정 대기"로 잘못 표기하는 등). §0/§1.x 서술도 그대로 믿지 않고 **`frontend/src` 실제 코드를 이번에 직접 다시 대조**했다(2026-08-31 이후 프론트 쪽에서 상당한 작업이 있었던 것으로 보임 — 이 세션이 한 작업 아님, 백엔드 세션 스코프 유지, 코드는 안 건드리고 읽기만 함). 결과: 아래 항목들이 **실제로 연동 완료된 상태였다** — ① 이메일 회원가입 인증코드 UI(`SignupPage.tsx`가 `requestSignupEmailCode`/`confirmSignupEmailCode` 실호출) ② 로그인·이메일중복확인·비밀번호변경(`loginWithPassword`/`checkEmail`/`changePassword` 전부 실호출) ③ 마이페이지 신청목록·상세(`listMyApplications`/`getMyApplication`) ④ 신청취소(`cancelApplication`, 상태별 버튼) ⑤ Inquiry 사용자 작성(`privacyConsent: true` 전송 확인) + 관리자 답변(`InquiriesSection.tsx`가 `listAdminInquiries`/`answerInquiry`/`updateInquiryStatus` 실호출 — §1.3/§1.4의 "여전히 mock" 서술은 낡음) ⑥ 개인 신청 국적 입력이 placeholder 텍스트가 아니라 `SearchableSelectField`(ISO 코드 옵션) 드롭다운으로 교체됨(§1.12 해소) ⑦ 드래프트 복원 시 `logoFile`/`sealFile`/`archiveFile`/`faceFile`을 의도적으로 복원 안 함(§1.13 해소) ⑧ FAQ·고객지원 페이지 둘 다 `api.listBoards({type:"FAQ"/"NOTICE"})` 실호출로 통합(§1.14 해소) ⑨ 마이페이지 "내 정보"에서 회원유형 표시 제거 + 전화번호 노출 확인(§1.9-a 해소). **§1.9-a의 "관리자 로그인이 role 타입 문제로 isAdmin=false로 떨어진다"는 서술도 재확인 결과 사실이 아니었다** — `LoginPage.tsx`는 `/api/users/me`(role 없음, 의도적)가 아니라 `POST /api/auth/login`의 응답(`LoginResponse`, `role` 필드 있음)에서 role을 가져오고, `AuthContext.refreshProfile()`은 그 뒤 `/me`로 프로필을 재동기화할 때 role은 덮어쓰지 않도록 이미 방어돼 있다(주석으로 이유까지 명시돼 있음) — 이 갭은 이미 해소된 상태였다. **여전히 실제로 안 된 것**(코드에 관련 함수·API 호출이 전혀 없음, 직접 확인): §1.15(관리자 만세력·카드디자인·카드미리보기 6개 API), §1.16(성씨), §1.17(학생증 템플릿 업로드, 오늘 신규), §1.7(공지 서버검색, 프론트·백엔드 둘 다 keyword 파라미터 없음), 관리자 통계 API(백엔드 자체가 없음). OAuth 로그인 경로의 role 처리는 이번에 확인 안 함(이메일/비밀번호 로그인만 확인) — 필요하면 별도 확인 필요. §0/§6을 이 재검증 결과로 갱신했다.
>
> **갱신: 2026-08-31(10차, Claude 세션).** §1.15(c)에 실제 검증 결과 추가 — 뉴욕 1995-06-15 15:00 출생 예시로 실제 백엔드 API(`manseryeok/resolve`)를 호출해 정확한 `utcInstant`/`selectedOffset`(서머타임 포함)을 확정받은 뒤, 같은 `manseryeok` 패키지로 지금 프론트 방식(버그)과 올바른 방식을 각각 계산해 비교했다 — 시주가 달라지고 오행 결핍 판정이 뒤집히면서 실제 700개 추천 데이터셋 채점 결과 상위 5개가 **0/5 겹침**으로 완전히 달라짐을 실증했다(saju 레포의 기준 구현 `computeSajuAtBirthplace()`도 함께 확인). 신규로 **§1.16 관리자 작명 성씨(surname) 필드 누락**도 추가 — §1.4에서 "연동 완료"로 분류했던 인앱 작명·엑셀 작명 반영 둘 다 성씨를 저장하지 않아, 이름을 다 골라도 `completeNaming()`에서 멤버 전원이 거절되는 걸 코드로 확인했다(§1.4도 이 내용으로 갱신). 코드는 수정하지 않았다(백엔드 세션 스코프 — `docs/collab/RULES.md`).
>
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
| OAuth 로그인·약관·세션·회원정보 | ✅ 실 API | ✅ 구현 | **연동 완료(2026-09-02 재검증)** — 예전 "role 타입 문제로 isAdmin=false" 서술은 오판이었음: `LoginPage.tsx`가 role 없는 `/api/users/me`가 아니라 `POST /api/auth/login`의 `LoginResponse`(role 포함)에서 role을 가져오고, `AuthContext.refreshProfile()`은 `/me` 재동기화 시 role을 안 덮어쓰게 이미 방어돼 있음(§1.9-a) |
| 신청 생성(개인/단체) | ✅ 실 API | ✅ 구현 | **연동 완료(2026-09-02 재검증)** — 예전에 있던 3가지 문제(schoolName 미전송·국적 placeholder·드래프트 복원 첨부파일 소실) 전부 코드로 재확인해 해소됨 확인(§1.9, §1.12, §1.13) |
| 신청 조회/카드다운로드 | ✅ 실 API | ✅ 준비 | 조회 응답에 `applicationType`이 포함돼 개인 `photo`/단체 `submitFile` 재제출 분기 가능. 단체 재제출 UI 연결만 남음. 카드다운로드는 소유자 로그인 전용(비로그인 조회는 데모 폴백) |
| 후기(Review) CRUD + 내 후기 + 다중 이미지 | ✅ 실 API | ✅ 구현(2026-08-24, 0~5장) | **연동 완료** — §1.8 "정책 공백"은 다중 허용으로 확정·구현 완료돼 해소됨 |
| 공지/FAQ(Board) | ✅ 실 API | ✅ 구현 + 데모 시드 추가(2026-08-24) | **연동 완료(2026-09-02 재검증)** — `FaqPage.tsx`·`SupportPage.tsx` 둘 다 `api.listBoards({type:"FAQ"/"NOTICE"})` 실호출로 통합됨, 예전의 "SupportPage만 하드코딩" 문제 해소 확인(§1.14) |
| 행사(Event) | ✅ 실 API | ✅ 구현(2026-08-21) | **연동 완료(2026-08-24)** — `EventAdminPanel.tsx`가 관리자 전체목록·생성·수정(갤러리·로고 유지/교체/삭제)·삭제까지 실 API로 전환 완료, `company`/`logoUrl` 필드 매핑도 그대로 대입 (§1.6) |
| **일반 이메일 회원가입(인증 포함)** | ✅ 실 API | ✅ 구현·`main` 반영 완료(`bc7d7ce`) | **연동 완료(2026-09-02 재검증)** — `SignupPage.tsx`가 `requestSignupEmailCode`/`confirmSignupEmailCode`/`signup`을 실호출하는 인라인 인증코드 UI로 구현돼 있음 확인(§1.1) |
| **일반 이메일 로그인·이메일 중복확인·비밀번호 변경** | ✅ 실 API | ✅ 구현·`main` 반영 완료 | **연동 완료(2026-09-02 재검증)** — `loginWithPassword`/`checkEmail`/`changePassword` 전부 실호출 확인(§1.1) |
| **계정 복구(아이디/비밀번호 찾기)** | ✅ 실 API(2026-08-24) | ✅ 구현 | **연동 완료** — `AccountRecoveryPage.tsx`가 요청→확인(마스킹 이메일/비밀번호 재설정)까지 4개 API 전부 호출 (§1.1) |
| **내 신청 목록·상세(마이페이지)** | ✅ 실 API | ✅ 구현·`main` 반영 완료(`b5f6140`) | **연동 완료(2026-09-02 재검증)** — `MyPage.tsx`가 `listMyApplications`/`getMyApplication` 실호출로 목록·상세·취소 버튼까지 렌더링함 확인(§1.2) |
| **1:1 문의(Inquiry)** | ✅ 실 API | ✅ 구현·`main` 반영 완료(사용자 작성 API·관리자 답변 API 둘 다) | **연동 완료(2026-09-02 재검증)** — `InquiryPage.tsx`가 `privacyConsent: true` 전송, 관리자 `InquiriesSection.tsx`가 `listAdminInquiries`/`getAdminInquiry`/`answerInquiry`/`updateInquiryStatus` 전부 실호출함 확인(예전 "관리자 화면은 여전히 mock" 서술은 낡음, §1.3·§1.4) |
| 관리자 신청관리(조회·상태전이·작명·카드번호) | ✅ 실 API | ✅ 구현 | **🔴 작명은 절반만 연동** — 조회·상태전이·카드번호는 정상, 인앱 작명·엑셀 작명 반영 둘 다 **성씨(surname) 미전송**이라 `completeNaming()`에서 멤버 전원 거절됨(2026-09-02 재확인, 여전히 미해결, §1.16). 통계(`GET /api/admin/stats`)는 **백엔드 완료(2026-09-05)**, 프론트는 여전히 자체 계산 중(§1.4) |
| **관리자 작명 확정·카드 제작(만세력·카드디자인·카드미리보기)** | ❌ 래퍼 자체 없음 | ✅ 구현·실 API 검증 완료 | **🔴 프론트 미착수 + 정확도 버그(2026-09-02 재확인, 여전히 미해결)** — `searchBirthRegion`/`resolveManseryeokBirthTime`/`confirmManseryeokResult`/`getActiveManseryeokResult`/`listCardDesigns`/`getCardPreview` 6개 전부 `services/api.ts`에 없음(grep 0건). `computeMemberSaju()`도 여전히 `trueSolarTime` 미사용 — 한국 외 출생자에게 **틀린 결과를 정상처럼** 보여주는 문제 그대로. 필요한 `longitude` 값은 백엔드가 이미 API로 내려주고 있어 백엔드 작업 없이 프론트만 고치면 됨 (§1.15(c)) |
| **학생증 카드 템플릿 업로드(관리자)** | ❌ 진입점 자체 없음 | ✅ 구현·통합 검증 완료(2026-09-01, 4-D) | **신규, 미착수(2026-09-02 확인)** — 학교별 학생증 카드 템플릿(앞/뒤) 등록·교체 API. 이게 없으면 STUDENT 신청 건은 위 카드디자인·카드미리보기가 항상 빈 값/실패로 막힌다 — §1.15와 함께 봐야 함 (§1.17) |
| **신청 취소** | ✅ 실 API | ✅ 구현·`main` 반영 완료(`b5f6140`) | **연동 완료(2026-09-02 재검증)** — `MyPage.tsx`의 `cancelApplication` 실호출, 취소 가능 상태에서만 버튼 노출 확인(§1.5) |
| 공지 서버 검색 | ⚠️ 클라 검색(미연결) | ✅ 구현 완료(2026-09-05) | **백엔드 완료, 프론트 파라미터 추가만 남음** — `GET /api/boards`에 `searchType`/`keyword` 추가됨(`72dad09`). `NoticesPage.tsx`/`listBoards()`는 여전히 전체를 받아 클라이언트에서만 `.filter()` — 서버 파라미터로 교체만 하면 됨 (§1.7) |
| **관리자 카드 다운로드(전체 ZIP/개별 재인쇄)** | ❌ 진입점 자체 없음 | ✅ 구현 완료(2026-09-05) | **신규, 미착수** — `cardFrontPath`/`cardBackPath` 존재 여부로 판단(제작 중에도 허용), 전체 ZIP 또는 멤버 개별 다운로드. `services/api.ts`에 래퍼 없음(grep 0건) (§1.18) |
| 회원정보 address 수정 | ⚠️ 화면엔 없음(정책상 제거된 상태) | ❌ 미지원(확정 정책) | **갭 아님** — 조회는 이름·전화번호·이메일만, 수정도 이름·전화번호만(§1.9-a) |
| 학생증 schoolName/schoolId | ✅ 검색select+직접입력 | ✅ School 마스터+schoolId 위변조 차단 | **연동 완료(2026-08-27)** — 실 브라우저 E2E(단체는 실제 제출·DB 반영까지) 검증 완료 (§1.9) |
| 카드 종류·디자인 카탈로그 | 정적(`cards.ts`) | 🟡 내부만 존재 | **STATIC 확정**(공개 API 신설 안 함) (§2.1) |
| 한국이름 조회(`nameResults.json`) | 정적 215KB 번들 | ❌ 없음 | 조회 API 필요 (§2.2) |
| 정적 마케팅(협력사/SNS/기념품/약관문/회사정보) | 정적 | ❌ 없음 | 선택 — CMS/설정 API (§3) |

범례: ✅ 완료 · ⚠️ 부분/혼재 · 🟡 내부·미커밋 존재 · ❌ 미구현

---

## 1. 프론트가 필요로 하나 백엔드에 없는 부분

### 1.1 일반 이메일 회원가입·로그인·계정 복구

#### (a) 회원가입(이메일 인증 포함) — ✅ 연동 완료(2026-09-02 재검증) — `SignupPage.tsx`가 인라인 인증코드 UI로 실제 구현돼 있음
- **백엔드 현황**: 3개 API 전부 구현·`main` 커밋·푸시 완료 — `POST /api/auth/signup/email-verification/request`(인증코드 발송), `POST /api/auth/signup/email-verification/confirm`(코드 확인 → `signupToken` 발급), `POST /api/auth/signup`(가입 완료, `signupToken`+`email`+`password`+`name`+`phone` 필수). 상세 계약·요청/응답 예시는 `docs/api/auth.md` API 4~6, `docs/FRONTEND_API_INTEGRATION_SPEC.md` §3.13.
- **프론트 사용처**: `pages/SignupPage` — 현재 이름/이메일/비밀번호/전화번호를 받는 단일 폼이며, 제출 시 실제 API를 호출하지 않고 `AuthContext.login()`으로 로컬 mock 세션만 만든다.
- **프론트가 새로 만들어야 하는 것**: 이메일 인증 코드 입력 UI가 화면에 전혀 없다. **UX 결정(2026-08-19)**: 별도 페이지/스텝으로 분리하지 않고 `SignupPage.tsx` 폼 안에 **인라인**으로 넣는다 — 이메일 입력 후 코드 요청 트리거(버튼) → 같은 화면에 코드 입력 필드 노출 → 확인 성공 시 `signupToken` 확보 → 이어서 비밀번호·이름·전화번호까지 채운 뒤 최종 회원가입 제출.
- **조치**: `services/api.ts`에 3개 API 바인딩 추가(타입 포함) + `SignupPage.tsx`에 인라인 인증코드 입력 섹션 신규 구현 + 실제 제출 로직을 mock에서 실 API 호출로 교체. 재전송 대기(60초)/횟수제한(429)/코드불일치(`INVALID_VERIFICATION_CODE`, 남은 시도 횟수 비노출)/`signupToken` 만료(`INVALID_SIGNUP_TOKEN`) 에러 메시지 처리 필요.
- **⚠️ 비밀번호 검증 규칙 불일치(2026-08-19)**: 백엔드 확정 정책은 최소 8자·최대 72자·복잡도 규칙 없음이지만, 현재 `SignupPage.tsx`는 여전히 "8~64자 + 영문/숫자/특수문자 조합 필수"로 더 엄격하게 검증한다(한 번 완화했다가 "프론트는 백엔드 세션에서 수정하지 않는다"는 방침에 따라 되돌림). 서버가 최종 검증을 하므로 저장 자체엔 문제없지만, 72자 이상 비밀번호나 복잡도 조합을 안 채운 8자 이상 비밀번호는 프론트 자체 검증에서 먼저 막힌다. 프론트 담당자가 실 연동 시 이 규칙도 8~72자·복잡도 규칙 없음으로 맞출지 판단 필요.

#### (b) 로그인·이메일 중복확인·비밀번호 변경 — ✅ 연동 완료(2026-09-02 재검증) — `loginWithPassword`/`checkEmail`/`changePassword` 전부 실호출 확인
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

### 1.2 내 신청 목록·상세(마이페이지) — ✅ 연동 완료(2026-09-02 재검증)
- **✅ 재검증 결과(2026-09-02)**: 아래 있던 문제(localStorage mock만 읽던 것)는 해소됐다 — `MyPage.tsx`가 `api.listMyApplications`/`api.getMyApplication`을 실호출하고, 상태별 취소 버튼(`CANCELLABLE` 상태 집합)까지 렌더링한다. 아래는 문제가 있었던 당시(2026-08-20)의 기록.
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

### 1.3 1:1 문의(Inquiry) — ✅ 연동 완료(2026-09-02 재검증)
- **프론트 사용처**: `pages/InquiryPage`(작성), `pages/InquiryDetailPage`(상세), `pages/MyPage`(내 문의), 관리자는 `components/admin/sections/InquiriesSection.tsx`(목록+답변+상태변경).
- **✅ 재검증 결과(2026-09-02)**: 아래 있던 두 문제 모두 코드로 직접 확인해 해소됐다 — `InquiriesSection.tsx`가 `listAdminInquiries`/`getAdminInquiry`/`answerInquiry`/`updateInquiryStatus`를 전부 실호출(예전엔 `services/api.ts` import 자체가 없었음)하고, `InquiryPage.tsx`도 제출 시 `privacyConsent: true`를 요청 바디에 포함해 보낸다(예전엔 체크박스 상태가 버튼 비활성화에만 쓰이고 요청엔 안 실렸음).
- **✅ 백엔드 6개 API 전부 구현·테스트·커밋 완료(2026-08-19)**: `POST /api/inquiries`, `GET /api/my/inquiries`(+`/{id}`), `GET /api/admin/inquiries`(+`/{id}`), `PATCH /api/admin/inquiries/{id}/answer`, `PATCH /api/admin/inquiries/{id}/status`. 상세 계약은 **`docs/specs/inquiry/requirements.md`가 source of truth**(§④ API 목록, §⑤ 처리 흐름, §⑦ Validation).
- **연동 시 참고**: `category`는 프론트가 이미 보내는 한글 문자열(제작 신청/결제 및 배송/카드 발급/행사·단체 협업/기타) 그대로 받는다(백엔드가 `@JsonValue`/`@JsonCreator`로 매핑, 프론트 값 변경 불필요). `name`/`email`/`phone`은 계정 값이 아니라 폼에 입력한 값 그대로 저장된다. 목록·상세 API는 페이지네이션이 없다(프론트에 검색/페이지 UI 자체가 없어 전체 나열).

### 1.4 관리자 신청관리·통계 — 조회·상태전이·작명·카드번호는 이미 연동됨, 만세력·카드디자인·카드미리보기만 미연동 (2026-08-27 재대조로 대폭 정정)
- **프론트 사용처**: `pages/AdminPage`, `components/admin/sections/ApplicationsSection.tsx`.
- **✅ 실제로는 이미 연동돼 있음 — 위 "❌ 아직 없음" 기재는 낡은 정보였다.** `services/api.ts`(206~232행)에 아래 API들이 전부 바인딩돼 있고 `ApplicationsSection.tsx`/`AdminPage.tsx`가 실제로 호출한다: 목록/상세(`listAdminApplications`/`getAdminApplicationDetail`), 구성원 목록(`getAdminApplicationMembers`), 상태전이 7종(`confirmApplicationPayment`/`startApplicationReview`/`approveApplicationNaming`/`completeNaming`/`startProducing`/`markCardReady`/`rejectApplicationPhoto`/`dispatchApplication`), 인앱 작명(`saveMemberName`), 엑셀 작명 반영(`applyNamingResult`), 카드번호 단건/일괄(`assignCardNumber`/`assignCardNumbersBatch`), 명단 엑셀 내보내기(`exportApplications`). **§1.15에 정리한 4종(출생지역 검색·만세력 resolve/confirm/조회·카드디자인 조회·카드미리보기)만 여전히 미연동이다.**
- **⚠️ 갱신(2026-08-31) — 위 "인앱 작명"/"엑셀 작명 반영"은 "연동됨"이라 부르기엔 불완전하다**: 성씨(`surname`) 필드를 이 둘 다 전송하지 않는다. `completeNaming()`(작명 완료 처리)이 멤버마다 성씨 필수로 검증하므로, 지금 상태로는 이름을 다 골라도 최종 "작명 완료 처리"에서 전원 거절된다. 상세는 §1.16.
- **✅ 백엔드 완료(2026-09-05)**: `GET /api/admin/stats` — `{ totalApplications, individualApplications, groupApplications, totalInquiries, pendingInquiries, completedInquiries }` 반환(신청 상태별·카드종류별 분포·기간 필터는 정책상 이번 범위 아님, 필요 시 추후 확장). `OverviewSection`(`AdminPage.tsx`)이 `listAdminApplications({size:100})`로 받은 첫 100건을 `.filter()`해 개인/단체 카운트를 세던 걸 이 API 호출로 교체하면 100건 초과 시의 부정확함이 해소된다. `services/api.ts`에 아직 래퍼 없음(프론트 미착수).
- **⚠️ status enum 재확인 필요**: 프론트가 이제 실 API를 쓰므로 옛 `adminMock.ts` enum(`SUBMITTED/CONSULTING/PAYMENT_PENDING/IN_PRODUCTION/COMPLETED/CANCELLED`) 라벨 테이블이 실제로 백엔드 enum(`SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`, 결제상태 별도 `WAITING/CONFIRMED`)으로 완전히 교체됐는지는 `ApplicationsSection.tsx`의 라벨 매핑 코드를 다시 대조해서 확인 필요(이번 대조 범위 밖 — §1.2와 동일한 종류의 리스크).
- **인가**: `/api/admin/**`는 SecurityConfig에서 `hasRole("ADMIN")`으로 라우트 레벨 강제, 각 API도 `validateAdmin()`으로 이중 검증.

### 1.5 신청 취소 — ✅ 연동 완료(2026-09-02 재검증)
- **프론트 사용처**: `MyPage.tsx`가 취소가능 상태(`SUBMITTED`/`REVIEWING`/`PHOTO_REJECTED`)에서만 취소 버튼을 노출하고 `api.cancelApplication(id)`를 실호출한다.
- **백엔드 현황**: `POST /api/applications/{id}/cancel`이 `ApplicationController`에 구현 완료, `main`에 커밋·푸시됨(`b5f6140`, 2026-08-19). `FRONTEND_API_INTEGRATION_SPEC.md` §3.7 계약과 동일.

### 1.6 행사(Event) — 회사/로고 필드·관리자 전체목록·갤러리 편집 — ✅ 백엔드·프론트 연동 완료(백엔드 2026-08-21, 프론트 2026-08-24)
- **프론트 사용처**: `pages/EventsPage`의 `FeedPost`(`data/eventFeedPosts.ts`)가 협업 카드에 `company`/`logoUrl`로 로고 표시. 관리자 패널(`EventAdminPanel.tsx`)이 실 API로 전체목록(숨긴 글 포함)·생성·수정(갤러리 편집·로고 유지/교체/삭제)·삭제까지 전부 구현 완료.
- **백엔드 현황(2026-08-21 구현 완료·push 완료)**: `EventPost`에 `companyName`/`logoImagePath`(`COLLABORATION` 전용, 내부 엔티티/컬럼명) 추가. 응답 DTO(`EventListItemResponse`/`EventDetailResponse`/`EventAdminListItemResponse`/`EventAdminDetailResponse`)와 요청 DTO(`EventCreateRequest`/`EventUpdateRequest`)는 프론트 `FeedPost`(`data/eventFeedPosts.ts`)와 동일하게 `company`/`logoUrl` 필드명을 쓴다(엔티티 내부명과 API 계약명을 분리, 커밋 `1e5a7b3`). `GET /api/admin/events`(`visible` 무관 전체, `type`/`visible` 선택 필터)·`GET /api/admin/events/{id}` 신규. `PATCH /api/admin/events/{id}`에 갤러리 편집(`keepImageIds`) + 로고 유지·교체·삭제(`removeLogo`) 추가. 계약 상세는 `docs/specs/events/api.md` API 3·4·6·7.
- **프론트 현황(2026-08-24 연동 완료)**: `EventAdminPanel.tsx`가 `api.listAdminEvents`/`getAdminEvent`/`createEvent`/`updateEvent`(`keepImageIds`·`removeThumbnail`·`removeLogo` 포함)/`deleteEvent`를 전부 호출. `company`/`logoUrl` 필드명이 백엔드와 동일해 별도 매핑 어댑터 없이 그대로 대입한다(`eventToFeedPost()`, `data/eventFeedPosts.ts:28-29`). 기존 별도 컴포넌트였던 `EventFeedAdminPanel.tsx`는 이 작업으로 삭제됨(`EventAdminPanel.tsx`로 통합).

### 1.7 공지 서버 검색 — 백엔드 구현 완료(2026-09-05), 프론트 미연동
- **프론트 사용처**: `pages/NoticesPage` 제목/작성일 검색.
- **✅ 백엔드 현황(2026-09-05, `72dad09`)**: `GET /api/boards`가 `type`(필수)에 더해 `searchType`(`ALL`\|`TITLE`\|`CONTENT`, 생략 시 제목+본문 동시 검색)·`keyword`(생략/공백이면 검색 없이 전체) 파라미터를 받는다. `Review`의 `searchType`/`keyword` 패턴을 그대로 재사용해 구현했다(`BoardSearchType` enum, `BoardSpecifications`). `AUTHOR` 검색은 없다 — `Board`엔 후기처럼 작성자 표시 필드가 없기 때문(정책 확정 사항, 2026-09-05).
- **정책(2026-09-05 확정)**: ① NOTICE/FAQ 통합검색 없음, `type`은 계속 필수. ② 제목+본문 검색으로 충분(FAQ 질문+답변 포함). ③ 한글 검색만 지원(Review와 동일한 한계, `LIKE` 기반). ④ 공지 상단고정 없음, 기존 정렬(작성일+id desc) 그대로 유지.
- **아직 프론트가 안 쓴다**: `services/api.ts`의 `listBoards`는 여전히 `{ type?, page?, size? }`뿐(`api.ts:210`, `keyword`/`searchType` 없음). `NoticesPage.tsx`는 여전히 전체 목록을 받아 클라이언트에서 `.filter()`.
- **필요**: `listBoards`에 `searchType?`/`keyword?` 추가 + `NoticesPage.tsx`를 서버 파라미터 호출로 교체(클라이언트 `.filter()` 제거). 백엔드 작업은 끝났으므로 프론트만 남음.

### 1.8 후기 다중 이미지 — ✅ 정책 확정(다중 허용) + 백엔드·프론트 구현 완료(2026-08-24)
- **백엔드 현황**: 후기 1건당 0~5장(`MAX_IMAGE_COUNT`, `ReviewService.java:54`) — `ReviewImage` 엔티티로 정식 다중 이미지 지원, 생성(`:74`)·수정(`:218`, 유지+신규 합쳐 5장 제한) 둘 다 적용. `docs/specs/review` API 계약도 갱신 완료(커밋 `ff4d27d`).
- **프론트 현황**: `ReviewEditorPage.tsx`가 다중 파일 선택(`multiple`, 최대 5장)·기존/신규 이미지 갤러리·개별 삭제를 구현, `keepImageIds`를 포함해 `updateReview`/`createReview` 호출.

### 1.9 회원정보 address 수정(갭 아님, 확정 정책) · 학생증 schoolName/schoolId(✅ 2026-08-27 해결)

#### (a) 회원정보 조회/수정 — 갭 아님, 확정 정책(2026-08-08, 2026-08-20 재확인 2회, 2026-09-02 재검증)
`PATCH /api/users/me`는 `name`/`phone`만 처리한다. **`address`는 이 API로 수정하지 않는다** — 한때(2026-08-20 세션 초반) 지원하도록 뒤집혔다가, 다시 원래 정책(이름·전화번호만 수정 가능)으로 재확정됐다. `UserUpdateRequest`엔 `address` 필드 자체가 없어 요청 바디에 보내도 무시된다.
**추가로 `GET /api/users/me`(조회) 응답에서도 `role`(회원등급) 필드를 완전히 제거했다** — 마이페이지 "내 정보"에 회원등급 개념 자체가 없어야 한다는 요구를 반영, `UserMeResponse` DTO 자체에서 `role`을 뺐다(단순히 화면에서 안 보이게 하는 게 아니라 백엔드 응답 스키마 변경).
- **✅ 해소 확인(2026-09-02 재검증) — `AuthContext.tsx`의 `isAdmin`은 이미 올바르게 처리돼 있었다**: 예전엔 "`refreshProfile()`이 role 없는 `/me` 응답에 의존해서 `isAdmin`이 깨진다"고 적혀 있었는데, 실제 코드는 그렇지 않았다. `LoginPage.tsx`는 `/api/users/me`가 아니라 **`POST /api/auth/login`의 응답(`LoginResponse`, `role` 필드 있음)에서 role을 가져와** 로그인 시점에 `login({..., role})`을 호출하고, `AuthContext.refreshProfile()`은 그 뒤 `/me`로 프로필(이름/이메일/전화/주소)만 재동기화하며 **role은 의도적으로 `prev?.role ?? "user"`로 유지**한다(코드 주석: "role을 profile에서 파생하면 admin이 user로 강등된다"). 즉 `/me`가 role을 안 주는 건 처음부터 이 설계의 일부였고, 실제로 admin이 강등되는 버그는 없다. (OAuth 로그인 경로의 role 처리는 이번 재검증에서 확인하지 않았다 — 이메일/비밀번호 로그인만 코드로 직접 확인함.)
- **마이페이지 "내 정보" 표시/수정 화면 스펙(확정)**:
  - **조회 시 노출 필드**: 이름, 전화번호, 이메일 3개만. `UserMeResponse`는 `id, name, email, phone, address`를 반환한다 — 이름·이메일·전화번호 다 있다. **"회원 유형" 자체가 응답에 없으므로 표시할 수도 없다.**
  - **수정 가능 필드**: 이름, 전화번호 **둘 뿐**. `UserUpdateRequest`엔 `name`, `phone` 두 필드뿐이다. `address`는 조회·수정 어느 화면에도 넣지 않는다.
- 계약 상세는 `docs/api/user.md` API 2·API 5 참고.
- **참고(더 이상 갭 아님, 위 항목 참고)**: `ApiUser` 인터페이스가 `role`/`address`를 여전히 필드로 선언하고 있긴 하지만(백엔드 `/me` 응답과 완전히 1:1은 아님), 실제 role 판별은 `/me`가 아니라 로그인 응답을 쓰므로 동작에는 영향이 없다. `updateMe`에 `address`를 실어 보내는 건 여전히 컴파일은 되지만 백엔드가 조용히 무시한다(§1.9-a 정책 그대로, 타입을 더 엄격하게 좁히는 건 선택사항).

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

### 1.10 신청 조회 응답 `applicationType` — ✅ 연동 완료(2026-09-02 재검증)
- **배경**: 스펙 §3.7은 사진 재업로드 시 **프론트가 ApplicationType에 따라 part를 분리**(개인 `photo` / 단체 `submitFile`)하라고 요구한다.
- **백엔드 현황**: `ApplicationLookupResponse.applicationType`(`INDIVIDUAL`/`GROUP`) 구현 완료.
- **✅ 재검증 결과(2026-09-02)**: `MobileCardPage.tsx`가 `lookup.applicationType === "GROUP"`으로 이미 분기해 개인은 `photo`, 단체는 `submitFile`(ZIP, `accept` 속성도 다르게) 파트로 `api.reuploadPhoto()`를 호출하고, 반려 사유(`photoRejectReason`)도 같이 표시한다.

### 1.12 개인 신청 국적(nationality) — ✅ 해소 확인(2026-09-02 재검증)

- **백엔드 현황**: `ApplicationFieldFormats.isValidNationality()`가 `Set.of(Locale.getISOCountries())`로 검증한다 — **ISO 3166-1 alpha-2 2자리 대문자 코드**(`KR`, `US`, `JP` 등)만 통과하고, 그 외 문자열은 전부 `INVALID_INPUT`으로 거절된다(`ApplicationCreateRequest.MemberRequest.nationality`의 `@ValidNationality`).
- **✅ 재검증 결과(2026-09-02)**: 아래 있던 자유 텍스트 placeholder 문제는 해소됐다 — `StepInfo.tsx`의 국적 입력란이 이제 `SearchableSelectField`(`countryOptions`, ISO 코드 옵션) 드롭다운으로 바뀌어 있어, 유효하지 않은 값을 입력할 수 있는 경로 자체가 없다. 아래는 문제가 있었던 당시(placeholder="대한민국" 자유텍스트)의 기록.
- **참고 — 단체(엑셀) 경로**: 단체신청 엑셀 템플릿의 국적 열은 ISO alpha-2 코드 드롭다운(`CountryCodes` 이름정의, 249개)으로 원래부터 제한돼 있어 문제없었다.

### 1.13 신청서 드래프트 복원 시 첨부파일이 사라지는데 화면엔 첨부된 것처럼 보임 — ✅ 해소 확인(2026-09-02 재검증)

- **✅ 재검증 결과(2026-09-02)**: `useApplicationDraft.ts`를 다시 확인한 결과, 복원 로직이 `parsed.logoFile`/`sealFile`/`archiveFile`/`faceFile`을 명시적으로 `delete`해 복구 대상에서 제외하고 있다 — 아래 "조치(사용자 확정, 2026-08-20)"가 그대로 반영된 상태. 텍스트 입력값만 `sessionStorage`에서 복구되고, 실제 `File` 객체가 없는 첨부파일 필드는 화면에 "첨부된 것처럼" 표시되지 않는다.
- **증상(해소 전 기록)**: 법인·단체 신청에서 로고·직인 이미지·엑셀 zip을 전부 첨부하고 "최종 확인" 화면까지 파일명이 정상 표시된 상태로 "신청 제출"을 눌러도 `Error("로고와 제출 ZIP 파일을 다시 선택해 주세요.")`가 뜨며 API 호출 자체가 나가지 않았다(개인 신청의 사진/학교 로고도 동일 패턴). 원인은 `File` 객체가 `sessionStorage`에 직렬화되지 않아 복원 시 이름·크기만 남고 실제 바이너리가 없었기 때문.

### 1.14 "고객지원"과 "자주묻는 질문"이 서로 다른 FAQ를 보여줌 — ✅ 해소 확인(2026-09-02 재검증)

- **증상**: "고객지원"(`/support`) 메뉴로 들어가면 FAQ 항목이 보이는데, 거기서 "자주묻는 질문"으로 들어가면(또는 "더보기"로 `/faq`로 이동하면) 항목이 하나도 안 보인다.
- **원인 ①(프론트)**: `SupportPage.tsx`의 FAQ 섹션(`:16-24`, `:86-100`)은 `faqs`라는 **하드코딩 목업 배열(7개 항목)**을 그대로 렌더링한다 — 백엔드를 전혀 호출하지 않아 DB 상태와 무관하게 항상 보인다. 같은 페이지의 "고객지원" 드롭다운 안 "자주 묻는 질문" 서브메뉴(`config/navigation.ts:39`)도 별도 페이지가 아니라 이 하드코딩 섹션으로 스크롤만 하는 앵커일 뿐이다(`navigation.ts:29-30` 주석에 명시).
- **원인 ②(데이터)**: 실제 `/faq` 라우트(`FaqPage.tsx`)는 `api.listBoards({ type: "FAQ", size: 100 })`(`:18`)로 `GET /api/boards?type=FAQ`를 정상 호출한다. `BoardController`/`BoardService`(`findByBoardType(BoardType.FAQ, ...)`)도 정상 동작.
- **✅ 2026-08-24 데이터 문제는 해소됨**: `DemoDataSeeder.java`가 `Board(NOTICE)` 4건·`Board(FAQ)` 8건을 시딩하도록 추가됐다(`app.seed-demo-data` 프로퍼티, `docker-compose.yml` 기본값 `true`, DB가 비어있을 때만 적재 — idempotent).
- **✅ 원인 ①(프론트)도 해소 확인(2026-09-02 재검증)**: `SupportPage.tsx`를 다시 확인한 결과 이제 `api.listBoards({ type: "NOTICE", size: 100 })`를 실호출한다 — 하드코딩 배열이 사라지고 `FaqPage.tsx`와 같은 소스(백엔드 `GET /api/boards`)를 보게 됐다. "고객지원"과 "자주묻는 질문"이 서로 다른 목록을 보여주던 근본 문제는 해소됐다.

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

  **참고할 기준 구현이 이미 있음**: `saju` 레포(`saju/web/src/lib/saju.ts`)의 `computeSajuAtBirthplace()`가 정확히 이 문제(해외 출생 대응)를 풀어둔 함수다 — "① 현지 벽시계+timezone → 절대 UTC, ② 그 절대 순간을 KST 벽시계로 재표현해 라이브러리에 넣고(`applyHistoricalDst:false`로 라이브러리 자체 보정은 끔), ③ longitude로 진태양시(시주)만 보정" 3단계로 처리한다. `frontend/src/lib/saju.ts`엔 이 함수의 대응물이 없다 — ①은 백엔드가 이미 대신 해주므로(`ManseryeokResolveResponse.utcInstant`), 프론트는 ②③만 이식하면 된다.

  **⚠️ 실제로 검증함(2026-08-31, 실제 백엔드 API + 같은 `manseryeok` 패키지 + 실제 700개 추천 데이터셋으로 재현, 프론트 코드는 안 건드림)**: 신청자를 뉴욕 1995-06-15 15:00 출생으로 두고 실제 `POST .../manseryeok/resolve`에 뉴욕 좌표(위 (a) 검색으로 얻은 실제 값)를 보냈더니 `{"timezoneId":"America/New_York","selectedOffset":"-04:00","utcInstant":"1995-06-15T19:00:00Z","status":"EXACT"}` — 1995년 6월 서머타임까지 정확히 반영해서 확정됨. 이 값을 기준으로 지금 프론트 방식(입력을 그냥 KST로 취급)과 올바른 방식(`utcInstant`를 KST로 재표현 + longitude 진태양시 보정)을 같은 `manseryeok` 패키지로 각각 계산해 비교:

  | | 지금 프론트 방식 | 올바른 방식(백엔드 확정값 적용) |
  |---|---|---|
  | 사주 | 을해연주 임오월주 정축일주 **무신시주** | 을해연주 임오월주 정축일주 **정미시주** |
  | 오행 결핍 | 없음 | **금(金) 결핍** |
  | 실제 700개 데이터셋 채점 상위 5개(`adminNamingMock.ts`의 `scoreName`과 동일 알고리즘) | 건중·경재·경진·석진·찬경(최고점 3.20) | 산·진성·찬·강석·재겸(최고점 9.00) |
  | 두 목록 겹침 | **0/5개 — 완전히 다른 이름이 추천됨** | |

  즉 이 갭은 "덜 정확한 정도"가 아니라 **관리자가 신청자에게 실제로 다른 이름을 추천하게 만드는 수준의 차이**다. (연·월·일주가 이 사례에서 우연히 같게 나온 것도 절기 경계 근처가 아니었기 때문 — 경계 근처 출생이면 월주까지 달라질 수 있음, `admin-saju.md` 참고.)

#### (d) 활성 만세력 결과 조회 — `GET /api/admin/applications/{applicationId}/members/{memberId}/manseryeok`
- **용도**: 현재 활성(active=true)인 확정 결과를 다시 읽어온다. 화면에 "이미 확정된 만세력" 표시, 또는 카드 미리보기 전 확정 여부 확인 용도.
- **응답** (`ManseryeokActiveResultResponse`): `{ timezoneId, longitude, selectedOffset, utcInstant, timeAccuracy, confirmedPillars, uncertainPillars, elementCounts, tzdbVersion, calculationEngineVersion, calculatedAt }`. 활성 결과가 없으면 `404 NOT_FOUND`.
- **프론트가 필요한 것**: `getActiveManseryeokResult(applicationId, memberId)` 래퍼. 전혀 없음.

#### (e) 카드 디자인 목록 조회 — `GET /api/admin/card-designs?cardTypeId={id}&active={true|false}&applicationId={id}`
- **용도**: 카드 미리보기·최종 발급 전에 관리자가 카드 디자인(같은 카드종류 내 여러 후보 중 1개)을 선택한다.
- **응답**: `ApiResponse<List<CardDesignResponse>>` — 각 항목 `{ id, designNumber: number, name: string, orientation: "LANDSCAPE"|"PORTRAIT", isDefault: boolean, active: boolean }`. `active` 쿼리파라미터 생략 시 전체(비활성 포함), `true`면 활성만.
- **⚠️ 정정(2026-09-01) — STUDENT(학생증)도 이제 이 API로 조회된다, `UNSUPPORTED_CARD_TYPE` 거절 서술은 낡음(2026-08-30 4-B에서 이미 바뀜)**: `cardTypeId`가 STUDENT면 관리자가 여러 개 중 고르는 게 아니라, **`applicationId`(그 신청의 `schoolId`+`orientation`)로 서버가 정확히 0개 또는 1개만 자동으로 좁혀서 반환**한다 — 그 신청의 학교에 등록된 템플릿이 없으면 빈 배열(에러 아님, "아직 템플릿 미등록" 정상 상태), applicationId를 안 보내면 `INVALID_INPUT`. STUDENT 신청을 다룰 때만 `applicationId`를 추가로 실어 보내면 되고, 비학생증 카드는 지금 그대로(요청·응답 계약 무변경).
- **프론트가 필요한 것**: `listCardDesigns(cardTypeId, active?, applicationId?)` 래퍼(STUDENT는 applicationId 필수) + 디자인 선택 UI(비학생증: 썸네일/이름/가로세로 표시 후 관리자가 선택 / STUDENT: 자동으로 정해진 1개를 그대로 보여주기만 하면 됨, 목록이 비어있으면 "아직 이 학교 템플릿이 등록 안 됨 — 관리자가 §1.17로 먼저 등록해야 함" 안내). 전혀 없음.

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

### 1.16 관리자 인앱 작명·엑셀 작명 반영 — 성씨(surname) 필드 누락 (2026-08-31 신규)

- **배경**: §1.4에서 "연동 완료"로 분류했던 인앱 작명(`saveMemberName`)과 엑셀 작명 반영(`applyNamingResult`) 둘 다, 실제로는 **성씨를 저장하지 않는다**. 관리자가 이름을 다 확정해도 `completeNaming()`(작명 완료 처리)에서 전원 거절되는 결과로 이어진다.
- **백엔드는 성씨를 "추천 이름 선택과 한 번에" 받도록 이미 설계돼 있다** — 성씨를 추천해주는 게 아니라(추천 데이터셋 자체에 성씨가 없음, 아래 참고), **API 계약이 이름 확정과 성씨 확정을 한 호출로 같이 받게 되어 있다**는 뜻이다. `NameAssignRequest`(`POST /api/admin/applications/{applicationId}/members/{memberId}/name`)는 `{ surname?, name(필수), hanja?, reading?, meaning(필수) }`로 다섯 필드를 한 번에 받는다. `ApplicationMember.assignKoreanName(surname, name, chineseName, nameMeaning, nameInterpretation)`도 다섯 값을 한꺼번에 저장한다.
- **근데 프론트는 이 계약을 절반만 쓴다**:
  - `services/api.ts`의 `saveMemberName` 타입에 `surname`이 아예 없다: `(applicationId, memberId, body: { name: string; hanja?: string; reading?: string; meaning?: string })`.
  - 추천 이름 데이터셋(`frontend/src/data/sajuNames.json`, 700개)에도 성씨 필드가 없다(`{"name":"가헌","hanja":"佳憲",...}` — 이름만) — 성씨는 애초에 알고리즘이 추천할 대상이 아니라 **관리자가 직접 입력해야 하는 값**이다(백엔드 검증도 `validateSurnameFormat`: 그냥 한글 1~2자 형식 확인뿐, 점수·추천 없음).
  - `frontend/src` 전체에 `surname`/`성씨`를 다루는 코드가 **0건**이다(무관한 회사소개 페이지 문구 1건 제외, 전체 grep 확인).
  - 단체 엑셀 업로드 경로(`applyNamingResult`)도 동일 — 백엔드가 이 경로에서 호출하는 `ApplicationMember.assignKoreanName(name, chineseName)`은 surname을 안 받는 **2-인자 오버로드**다. 엑셀 서식 자체에 성씨 열이 있는지도 이번에 같이 확인 필요(미확인).
- **실제로 발생하는 문제(코드로 확인)**: `ApplicationsSection.tsx`의 `NamingCard`가 "작명 완료" 배지를 `member.assignedName`(주어진 이름 존재 여부)만으로 띄운다(`:408,429`) — 성씨는 체크하지 않는다. 그래서 관리자 눈엔 멤버마다 "작명 완료"로 보이는데, 정작 최종 "작명 완료 처리" 버튼(`completeNaming`)을 누르면 `ApplicationService.java:663-666`의 집계 검증이 성씨 없는 멤버 전원을 `NAMING_INCOMPLETE`(`{field:"surname", code:"REQUIRED"}`)로 한꺼번에 거절한다 — 개별 카드 표시와 실제 서버 판정이 어긋나는 UX 결함이기도 하다.
- **프론트가 필요한 것**:
  1. `services/api.ts`의 `saveMemberName` 타입에 `surname?: string` 추가.
  2. `NamingCard`(또는 그 상위)에 **성씨 입력용 텍스트 필드 신규**(한글 1~2자) — 추천 목록엔 없으니 관리자가 직접 타이핑. "이 이름 선택" 클릭 시 이 값을 `surname`으로 같이 전송.
  3. "작명 완료" 배지 조건에 성씨 존재 여부도 포함(지금은 주어진 이름만 봄) — 그래야 화면 표시와 `completeNaming()` 실제 판정이 일치한다.
  4. 단체 엑셀 경로: 서식에 성씨 열이 있는지 확인 후, 없으면 열 추가 여부부터 정책 결정 필요(이건 프론트만의 문제가 아니라 엑셀 양식+백엔드 파서까지 걸친 별도 결정 사항).

### 1.17 학생증 카드 템플릿 업로드(관리자) — 백엔드 구현 완료(2026-09-01, 4-D), 프론트 진입점 자체가 없음(신규)

- **배경**: 학생증(STUDENT)은 다른 3종 카드와 달리 학교마다 디자이너가 완성된 앞/뒤 이미지를 통째로 다르게 제공한다(§1.15(e)에 이미 정정해뒀듯, STUDENT는 "여러 디자인 중 관리자가 선택"이 아니라 "그 학교 템플릿이 자동으로 정해짐" 구조). 그 템플릿 자체를 관리자가 배포 없이 등록·교체하는 API가 이번에 새로 생겼다 — **이 API가 있어야 STUDENT 신청 건에서 §1.15(e)/(f)(카드 디자인 조회·미리보기)가 실제로 결과를 낼 수 있다**(템플릿이 없으면 (e)는 계속 빈 배열, (f)는 `CARD_DESIGN_NOT_FOUND`).
- **공통 인가**: `/api/admin/**` → `hasRole("ADMIN")` + `validateAdmin()`.

**조회 — 현재 등록된 템플릿**: `GET /api/admin/schools/{schoolId}/card-template?orientation=LANDSCAPE|PORTRAIT`
- 응답(등록돼 있을 때): `{ cardDesignId, frontPreviewUrl, backPreviewUrl }`(presigned URL, 1시간 만료).
- 응답(미등록일 때): `{ "success": true }` — **`data` 키 자체가 없다**(에러 아님, 관리자가 새 학교를 다룰 때 항상 마주치는 정상 상태). `response.data`를 읽으면 `undefined`이므로 "존재 여부"는 `if (!response.data)`로 판단하면 되고 별도 null 체크 분기가 필요 없다.

**등록·교체**: `POST /api/admin/schools/{schoolId}/card-template`(multipart/form-data)
- part: `orientation`(폼 필드, `LANDSCAPE`\|`PORTRAIT`, 필수) + `front`(file, 필수) + `back`(file, 필수) — 앞/뒤를 항상 같이 보낸다, 한쪽만 교체하는 흐름 없음.
- 응답: GET과 동일한 DTO — 성공 시 프론트가 재조회 없이 바로 미리보기를 갱신할 수 있다.
- 에러: `FORBIDDEN`(403, ADMIN 아님) · `SCHOOL_NOT_FOUND`(404) · `INVALID_INPUT`(400, orientation 누락) · `front`/`back` 파트 자체 누락(400) · `UNSUPPORTED_FILE_TYPE`(415, PNG 아님) · `FILE_TOO_LARGE`(413, 10MB 초과) · `CARD_TEMPLATE_INVALID_RESOLUTION`(400, 카드 비율 235:156±5% 또는 최소 해상도 800px 미달).

**프론트가 필요한 것(전부 신규, 진입점 자체가 없음)**:
1. `services/api.ts`에 `getSchoolCardTemplate(schoolId, orientation)`/`uploadSchoolCardTemplate(schoolId, orientation, front, back)` 래퍼.
2. 관리자 화면에 학교+방향(가로/세로) 선택 UI → 위 GET으로 현재 앞/뒤 미리보기 표시(없으면 빈 박스, 에러 아님) → "파일 변경" 클릭 시 파일 2장 선택 → POST로 등록 → 성공 시 미리보기 갱신. (기존 사용자 목업: 학교구분+방향 선택 → 현재 앞/뒤 미리보기 → 파일 두 장 선택 후 등록.)
3. 에러 메시지 매핑(`CARD_TEMPLATE_INVALID_RESOLUTION`은 "PNG 형식은 맞지만 비율/해상도가 카드 규격과 안 맞음"으로, `UNSUPPORTED_FILE_TYPE`과 구분해서 안내).
4. 이 화면이 없으면 STUDENT 신청 건은 관리자가 카드 미리보기/발급까지 절대 진행할 수 없다(§1.15 흐름의 새 전제조건) — 우선순위 판단 시 §1.15와 묶어서 고려할 것.
- **상세 계약**: `docs/api/school-card-template.md` 참고(요청/응답 예시, 구현 메모 포함).
- 백엔드 검증: 서비스/컨트롤러 테스트 + **실제 업로드 API로 업로드한 템플릿이 실제 카드 미리보기 API까지 정상 렌더링되는 통합 테스트**까지 완료(`SchoolCardTemplateEndToEndTest`).

### 1.18 관리자 카드 다운로드(전체 ZIP/개별 재인쇄) — 백엔드 구현 완료(2026-09-05), 프론트 진입점 자체가 없음(신규)

- **배경**: 실물 제작 과정에서 관리자가 렌더링된 카드 이미지를 내려받아야 한다. 사용자용 `getCardDownload`(§1.4 참고 없음, `application.status === COMPLETED`일 때만 허용)와는 별개 정책·별개 API다 — 관리자는 `PRODUCING` 중에도 렌더링만 끝나면 다운로드할 수 있어야 한다(재인쇄·품질확인 용도).
- **공통 인가**: `/api/admin/**` → `hasRole("ADMIN")`(SecurityConfig) + `validateAdmin()` 이중 검증.
- **판단 기준(정책 확정, 2026-09-05)**: `ApplicationStatus`가 아니라 각 `ApplicationMember.cardFrontPath`/`cardBackPath`(둘 다 non-null)로 판단한다. ZIP은 S3에 영구 저장되지 않고 요청마다 저장된 이미지들을 모아 동적으로 생성해 스트리밍 응답한다.

**전체 ZIP 다운로드** — `GET /api/admin/applications/{applicationId}/cards/download`
- 응답: `Content-Type: application/zip`, `Content-Disposition: attachment; filename="application-{id}-cards.zip"` (raw 바이너리, `ApiResponse` 미포장).
- 해당 신청의 **모든 멤버**가 front/back 이미지를 둘 다 가지고 있어야 성공한다. 하나라도 없으면 전체를 거절하고(`400`, `errorCode: "CARD_NOT_READY"`), 응답 바디에 어떤 멤버가 빠졌는지 식별 정보를 포함한다(`BulkValidationException`의 `ValidationErrorDetail(row=memberId, field:"cardImage", code:"NOT_READY", message)` 배열 — 엑셀 검증 실패 응답과 동일한 형태로 재사용됨, 엑셀 전용이 아님).
- 성공 시 `AdminActivityLog.CARD_DOWNLOAD`로 감사 로그가 남는다.

**개별 멤버 다운로드(재인쇄용)** — `GET /api/admin/applications/{applicationId}/members/{memberId}/cards/download`
- 용도: 단체 신청 중 특정 1명만 재제작/재인쇄가 필요할 때. 다른 멤버가 준비 안 됐어도 이 멤버만 준비돼 있으면 성공한다.
- 응답: `ApiResponse<{ applicationId, memberId, cardFrontUrl, cardBackUrl, expiresAt }>` (presigned URL 2개, JSON — ZIP과 달리 파일을 직접 스트리밍하지 않고 URL을 내려준다).
- **만료 30일**(관리자 용도라 사용자용 7일보다 길게 잡음, 2026-09-05 확정).
- 그 멤버의 front/back이 없으면 `400 CARD_NOT_READY`, 다른 신청 소속 멤버 id를 넘기면 `404`.
- 성공 시에도 `AdminActivityLog.CARD_DOWNLOAD` 기록.

**프론트가 필요한 것(전부 신규, 진입점 자체가 없음)**:
1. `services/api.ts`에 `getAdminApplicationCardsZip(applicationId)`(blob 응답 → 다운로드 트리거)와 `getAdminMemberCardDownload(applicationId, memberId)`(URL 2개 받아 `<a>`/새 탭으로 열기) 래퍼 추가.
2. `ApplicationsSection.tsx`(또는 상세 화면)에 "전체 카드 다운로드(ZIP)" 버튼(단체) + 멤버별 "카드 다운로드/재인쇄" 버튼(개인·단체 공통) 추가.
3. 전체 ZIP 거절 시 응답의 멤버별 결측 목록을 관리자가 바로 알아볼 수 있게 표시(예: "OO님 카드 미생성 — 작명/카드번호 확인 필요" 등).
- **검증**: `ApplicationServiceAdminCardDownloadTest`(7개)·`AdminApplicationControllerTest` 신규 5개, 전부 통과. `docs/collab/TODO.md`(2026-09-05 완료 섹션) 참고.

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

> **⚠️ 2026-09-02 전체 재검증**: 이 표가 §0/§1.x보다도 더 낡아 있었다(계정복구를 "백엔드 구현 진행 중"으로, 이미 완료된 행사/후기다중이미지를 "정책 결정 대기"로 잘못 표기하는 등 — §0/§1.x가 갱신될 때 이 표는 같이 안 고쳐져 온 것으로 보임). 아래 "완료" 목록·"남은 작업" 목록 둘 다 `frontend/src` 실제 코드를 이번에 직접 대조해서 다시 정리했다(문서 상단 11차 갱신 메모 참고). 이 표 하나만 보고 판단해도 되도록, 다음 갱신 때도 여기부터 코드 재대조 후 고칠 것 — 다른 절만 고치고 이 표를 안 건드리면 다시 낡는다.

### 완료된 항목 (참고용, 작업 대상 아님)

| 항목 | 완료 근거(2026-09-02 코드 재확인) | 관련 절 |
|---|---|---|
| ~~학생증 schoolName 요청 추가~~ | School 마스터+검색select까지 포함해 해결(2026-08-27) | §1.9-b |
| ~~개인 신청 국적(nationality) 입력 방식~~ | `StepInfo.tsx`가 `SearchableSelectField`(ISO 코드 `countryOptions`)로 이미 교체됨 | §1.12 |
| ~~드래프트 복원 시 첨부파일 문제~~ | `useApplicationDraft.ts`가 `logoFile`/`sealFile`/`archiveFile`/`faceFile`을 복원 시 의도적으로 삭제 | §1.13 |
| ~~일반 이메일 회원가입(인증코드 UI)~~ | `SignupPage.tsx`가 `requestSignupEmailCode`/`confirmSignupEmailCode`/`signup` 실호출 | §1.1-a |
| ~~로그인·이메일 중복확인·비밀번호 변경~~ | `loginWithPassword`/`checkEmail`/`changePassword` 전부 실호출 | §1.1-b |
| ~~계정복구(아이디/비밀번호 찾기)~~ | `AccountRecoveryPage.tsx`가 4개 API 전부 실호출(2026-08-24) | §1.1-c |
| ~~마이페이지 "내 신청 목록·상세"~~ | `MyPage.tsx`가 `listMyApplications`/`getMyApplication` 실호출 | §1.2 |
| ~~신청 취소 진입점~~ | `MyPage.tsx`의 `cancelApplication` 실호출, 취소가능 상태에서만 버튼 노출 | §1.5 |
| ~~1:1 문의(Inquiry) 전체 연동~~ | 사용자 작성(`privacyConsent` 전송)+관리자 답변(`InquiriesSection.tsx`) 둘 다 실호출 | §1.3, §1.4 |
| ~~신청 조회 `applicationType`으로 재제출 UI 분기~~ | `MobileCardPage.tsx`가 개인=`photo`/단체=`submitFile` 파트로 이미 분기 | §1.10 |
| ~~"내 정보" 표시 정리~~ | `MyPage.tsx`에 회원유형 표시 없음 + 전화번호 노출 확인, 수정은 이름·전화번호만 | §1.9-a |
| ~~"관리자 로그인 role 타입 문제"~~ | 재조사 결과 애초에 버그가 아니었음(로그인 응답에서 role을 가져오는 구조라 무관) | §1.9-a |
| ~~공지/FAQ 소스 이원화(FaqPage·SupportPage)~~ | 둘 다 `api.listBoards()` 실호출로 통합됨 | §1.14 |
| ~~행사(Event) 관리자 전체목록·company/logoUrl~~ | `EventAdminPanel.tsx` 실 API 전환 완료(2026-08-24) | §1.6 |
| ~~후기(Review) 다중 이미지~~ | 0~5장 정책 확정·백엔드/프론트 구현 완료(2026-08-24) | §1.8 |

### 남은 작업 (우선순위 순)

| 순위 | 항목 | 상태 | 관련 절 |
|---|---|---|---|
| 1 | **관리자 작명 — 성씨(surname) 필드 추가** | 백엔드 계약은 이미 성씨를 받게 돼 있음(`NameAssignRequest.surname`) — 프론트가 안 보내서 `completeNaming()`에서 멤버 전원 거절됨. 이미 연동된 화면에 입력 필드 하나 + 타입 하나만 추가하면 되는 **가장 작고 빠른 항목** | §1.16 |
| 2 | 관리자 작명 확정·카드 제작(만세력·카드디자인·카드미리보기) | 백엔드 완료. API 바인딩 6개 + UI 신규 + **사주 계산 로직(진태양시 보정) 신규 작성**까지 필요해 작업량 큼 | §1.15 |
| 2 | 학생증 카드 템플릿 업로드(관리자) | 백엔드 완료(2026-09-01, 4-D). STUDENT 신청 건은 이게 없으면 위 항목(카드디자인·카드미리보기)이 항상 빈 값 — 같이 진행할 것 | §1.17 |
| 3 | 공지 서버 검색(`searchType`/`keyword`) | **백엔드 완료(2026-09-05)** — `listBoards`에 파라미터 추가 + `NoticesPage.tsx` 클라이언트 `.filter()` 제거만 하면 됨. 작업량 작음 | §1.7 |
| 3 | 관리자 카드 다운로드(전체 ZIP/개별 재인쇄) | **백엔드 완료(2026-09-05)** — API 래퍼 2개 + 다운로드/재인쇄 버튼 UI 신규. 실물 제작 흐름에 필요 | §1.18 |
| 4 | 관리자 통계 대시보드(`GET /api/admin/stats`) | **백엔드 완료(2026-09-05)** — API 래퍼 1개 + `OverviewSection`의 `size=100` 자체 계산을 이 호출로 교체 | §1.4 |
| 5 | 한국이름 조회 API 전환, 정적 마케팅 CMS화, 하이브리드 목데이터(§5) 정리 | 우선순위 낮음, 필요 시에만 | §2.2, §3, §5 |

**진행 원칙**: 1번은 이미 연동된 기능이 절반만 동작하는 회귀성 결함이라 가장 먼저. 2번은 작업량이 크지만 백엔드는 이미 다 준비돼 있어 프론트 작업만으로 끝난다. 3번(공지 검색·카드 다운로드)도 백엔드가 이미 끝나 있어 프론트만으로 끝나는 작은 항목들이다. 4번 이후는 백엔드 작업이나 정책 결정이 먼저 필요해 프론트 혼자 진행할 수 없는 항목.
