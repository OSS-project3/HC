# 백엔드 남은 작업 (전 도메인) — 2026-08-25

이 문서는 **백엔드가 아직 해야 할 것만** 모은 단일 목록이다. 2026-08-25 코드 전수 대조(프론트↔백엔드 65개 엔드포인트, 5개 도메인 라이브 테스트, 문서 4그룹 감사)로 확정한 **실제 미구현**만 담는다.

> 판정 기준: "백엔드에 엔드포인트/필드/도메인이 실제로 없는 것"만 포함. 이미 구현·연동된 것(관리자 상태전이 8종·엑셀 export·작명·문의·후기 다중이미지·입금자명 등)은 제외한다. 현재 연동 상태의 단일 소스는 `docs/FRONTEND_API_GAPS.md`·`docs/API_TEST_REPORT.md`.

---

## 0. 우선순위 한눈에

| 순위 | 항목 | 현재 대체 수단 | 난이도 | 관련 |
|---|---|---|---|---|
| P1 | 관리자 통계 집계 `GET /api/admin/stats` | 프론트가 목록 받아 클라이언트 집계 | 중 | §1 |
| P1 | 신청 건별 동의 이력 저장 | 저장 안 됨(약관은 user 레벨만) | 하 | §2 |
| P2 | 공지(Board) 서버 검색·페이지네이션 | 프론트가 `size=100` 받아 클라이언트 검색 | 하 | §3 |
| P2 | 이벤트 "프로그램" 콘텐츠 저장 | 프론트 localStorage(`managed-content:events`) | 하 | §4 |
| P2 | 카드 이미지 합성 API | 합성 엔진(`CardImageCompositor`)만, HTTP 없음 | 중 | §4.5 |
| P3 | 한국이름 조회·추천 API | 프론트 정적 번들(`nameResults.json` 215KB)/자체 mock | 중 | §5 |
| P3 | CardType 관리 API | 코드/시드로만 관리(편집 불가) | 중 | §6 |
| P3 | CardDesign 관리·배정 API | `Application.cardDesignId` 항상 null | 중 | §6 |
| 🔒 배포전 | 임시 관리자 시드·mock 폴백 제거 | 데모용으로 남아있음(보안) | 하 | §7 |
| 인프라 | SMTP 메일 발송 설정 | 로컬 미설정 → 회원가입 인증 503 | 하 | §8 |
| 인프라 | OAuth 운영 리다이렉트/시크릿 | 미확정 | 하 | §8 |

---

## 1. 관리자 통계 집계 — `GET /api/admin/stats` (P1)
- **현황**: 컨트롤러 자체가 없다. 존재하는 통계성 API는 `GET /api/admin/name-selection-stats`(이름 선택이력)뿐이고, 대시보드 통계 카드는 프론트가 `GET /api/admin/applications` 목록을 받아 클라이언트에서 집계 중.
- **필요**: 신청 상태별 건수, 결제 대기/확인 건수, 기간별 신규 신청 수 등 집계 응답. 대량 데이터 시 클라이언트 집계는 부정확·비효율.
- **제안**: `GET /api/admin/stats`(관리자, 기간 파라미터 옵션). 응답 예: `{ byStatus: {SUBMITTED: n, ...}, byPayment: {WAITING, CONFIRMED}, todayCount, ... }`.
- **DB**: 기존 `applications` 집계 쿼리. 신규 테이블 불필요.

## 2. 신청 건별 동의 이력 저장 (P1)
- **현황**: 프론트 `StepType`에서 "상담확인·유의사항 동의"를 받지만 신청 요청 DTO·엔티티에 대응 필드가 없어 **서버에 남지 않는다**. (회원가입 시 약관 동의는 `User` 레벨에만 저장되고, 신청 단위 동의 이력은 없음.)
- **필요**: 신청 시점의 동의 스냅샷(동의 항목·시각)을 신청 건별로 보존(분쟁·법무 대비).
- **제안**: `ApplicationCreateRequest`/`BulkApplicationCreateRequest`에 동의 필드 추가 + `Application`(또는 별도 `ApplicationConsent`)에 저장. 최소한 boolean 스냅샷 + `consentedAt`.
- **참고**: `docs/FRONTEND_API_GAPS.md` §1.11(프론트 UI는 유지, 백엔드 보강 시 연결).

## 3. 공지(Board) 서버 검색·페이지네이션 (P2)
- **현황**: `GET /api/boards`에 `keyword`/`searchType` 파라미터가 없다(프론트 `NoticesPage`가 `size=100`으로 받아 클라이언트 검색). ※ 후기(`GET /api/reviews`)는 이미 `keyword`/`searchType` 지원 — 공지만 없음.
- **필요**: 게시글 증가 시 제목/작성일 서버 검색 + 페이지네이션.
- **제안**: `GET /api/boards`에 `keyword`·`searchType`·`page`/`size` 파라미터 추가(Review 검색과 동일 패턴 재사용).

## 4. 이벤트 "프로그램" 콘텐츠 저장 (P2)
- **현황**: 행사 **부스/협업 피드**는 이미 실 API(`/api/events`, `/api/admin/events`)로 저장·편집됨. 그러나 `EventsPage`의 "이벤트 **프로그램**" 블럽만 아직 프론트 localStorage(`managed-content:events`, `ContentAdminPanel`)에 저장돼 다른 기기/사용자에 반영 안 됨.
- **선택지**: (a) 이 프로그램 콘텐츠를 boards(공지형) 또는 events로 흡수하는 백엔드 저장, 또는 (b) 레거시 프론트 경로(`ContentAdminPanel`/`managed-content`)를 제거. **백엔드 필수는 아니며** 프론트 정리로도 해소 가능.
- **참고**: `docs/LOCALSTORAGE_TO_BACKEND.md` §3.1.

## 4.5 카드 이미지 합성 API (P2)
- **현황**: 좌표 기반 합성 엔진(`CardImageCompositor`)만 구현 — 신청 정보를 카드 템플릿(명예한국인증/명예시민증/방문증 3종·디자인 6개씩)에 합성해 PNG를 만드는 로직은 있으나 **이걸 부르는 HTTP 엔드포인트가 없다.** 카드번호 생성 정책·`ApplicationMember`에 결과 저장하는 로직도 미구현.
- **필요**: 관리자 심사 단계에서 카드 이미지를 생성·미리보기·확정하는 API. `Application.cardDesignId` 배정(§6)과 함께 묶임.
- **제안**: `POST /api/admin/applications/{id}/card-image`(생성/미리보기) + 결과 저장. 프론트는 이 API가 나온 뒤 디자인 선택·미리보기 화면 신규 구현(현재 프론트에 할 일 없음).
- **참고**: `docs/FRONTEND_API_GAPS.md` §1.16.

## 5. 한국이름 조회·추천 API (P3)
- **현황**: 이름 추천 데이터가 프론트 정적 번들(`frontend/src/data/nameResults.json`, ~215KB). 백엔드 조회 API 없음.
- **필요 시**: 데이터가 커지거나 서버 관리가 필요하면 조회 API로 전환(오행/성별 등 필터). 현 규모에선 정적 유지도 무방.

## 6. 카드 종류·디자인 관리 API (P3)
- **현황**: `CardTypeController`·`CardDesignController` **둘 다 존재하지 않는다**(코드 대조). 카드 종류는 코드/시드로만 관리되고, 관리자가 심사 후 배정하는 카드 디자인(`Application.cardDesignId`)은 **항상 null**(배정 흐름 미구현).
- **필요 시**:
  - `CardType` 관리: `POST/GET/PATCH /api/admin/card-types`(관리자 카드종류 편집이 실제로 필요할 때).
  - `CardDesign` 관리·배정: `/api/admin/card-designs` + 신청에 디자인 배정 엔드포인트(관리자 심사 흐름에 카드 디자인 선택을 넣을 때).
- **참고**: `docs/api/card-type.md`·`card-design.md`(설계만 있고 미구현으로 정정됨).

## 7. 🔒 배포 전 필수 — 임시 관리자 제거
- **현황**: 데모/시연용 관리자 계정이 시드돼 있다 — `DemoDataSeeder.ensureAdminUser()`(`admin@test.com`/`admin1234!`, `app.seed-demo-data=true`일 때). 로그인 실패 시 클라이언트 mock 세션을 만드는 폴백도 있다.
- **작업**: 운영 배포 전 `DemoDataSeeder.ensureAdminUser()` + `ADMIN_EMAIL`/`ADMIN_PASSWORD` 상수 삭제, 이미 생성된 DB 계정 수동 삭제/권한 회수, 프론트 로그인 mock 폴백 제거, `User.promoteToAdmin()`을 대체할 정식 관리자 승격 경로(가입·승격 API·정책) 확정.
- **참고**: `docs/TEMP_ADMIN_LOGIN.md`(계정/구성/제거 절차).
- **참고(데모 USER)**: 신청 조회 테스트용 `demo@test.com`(SQL 직접 시드)도 운영 전 삭제 대상.

## 8. 인프라·설정 (코드 아님, 운영 준비)
- **SMTP 메일 발송**: 로컬에 SMTP 미설정이라 회원가입 이메일 인증 코드 발송(`POST /api/auth/signup/email-verification/request`)이 503. 백엔드 발송 로직·단위 테스트는 정상 — 운영 SMTP(`MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD`) 설정 필요. 설정 전까지 회원가입 이메일 인증 E2E 불가.
- **OAuth 운영값**: Google/Naver 클라이언트 ID/시크릿·리다이렉트 URI 운영 도메인 확정(`docs/api/unresolved.md`).

---

## 부록 — "미구현 아님"(오해 방지)
아래는 이미 **구현·연동 완료**됐다. 다른 (낡은) 문서가 미구현으로 적었더라도 이 목록에 넣지 말 것:
- 관리자 신청 상태전이 8종(결제확인·검토시작·작명승인·사진반려·작명완료·제작시작·카드발급·배송발송), 엑셀 export, 작명결과 엑셀(naming-result), 작명 확정·선택이력.
- 1:1 문의 도메인(작성·내문의·관리자 답변/상태) 전체.
- 후기 다중 이미지(0~5장), 후기 수정 removeImage 버그 수정.
- 입금자명 저장(`PATCH /api/applications/{id}/depositor`).
- 내 신청 목록·상세, 신청 취소, 조회 `applicationType` 재제출 분기.
- 일일 신청 3회 제한(`APPLICATION_LIMIT_EXCEEDED`), 학생증 `schoolName`.
- 결제 "확인"은 정책상 무통장+관리자 수동(`confirm-payment`)이 정상 — 결제 게이트웨이 자동화는 요구사항 아님.
