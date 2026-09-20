# 프론트엔드 API 갭 현황

> 현재 상태 기준: 2026-09-16
>
> 검증 대상: `frontend/src`, `backend/honor-citizen/src/main`, 현재 워킹 트리
>
> 역할: **프론트에서 확인되는 현재 미완료 갭의 Source of Truth**

API의 상세 요청·응답 계약은 도메인 문서(`docs/api/*.md`, `docs/specs/*/api.md`)가 원본이다. 프론트 공통 사용법과 화면별 연결은 [`FRONTEND_API_INTEGRATION_SPEC.md`](./FRONTEND_API_INTEGRATION_SPEC.md)를 따른다. 최초 목 화면 기반 요구사항은 [`backend/FRONTEND_API_REQUIREMENTS.md`](../backend/FRONTEND_API_REQUIREMENTS.md)에 역사 자료로 보존한다.

## 1. 현재 미완료 갭

| 우선순위 | 항목 | 현재 상태 | 완료 조건 | 외부 차단 |
|---|---|---|---|---|
| P1 | `/api/users/me` 역할 복원 | 백엔드 `UserMeResponse`에 role이 없다. 프론트는 로그인 응답의 role을 `auth-role` UI 힌트로만 보존한다. 서버가 관리자 API 권한을 최종 검사하므로 인가 우회는 아니다. | `/me`에 `USER | ADMIN`을 추가하고 프론트가 해당 값을 세션 복원에 사용한 뒤 role 힌트를 제거 | 백엔드 응답 계약 변경 필요 |
| P1 | 신청 건별 동의 이력 | 백엔드 저장 계약은 완료(2026-09-20, 커밋 `6cbeafb`) — `Application.consultationConfirmed`/`disclaimerConfirmed`/`consentPolicyVersion`, 생성 요청 DTO에 같은 필드 추가. 프론트 `StepType.tsx`는 아직 이 값을 전송하지 않아 항상 `false`로 저장되고, 백엔드도 아직 필수 검증(둘 다 true여야 생성 허용)을 걸지 않았다 — 프론트 미연동 상태에서 검증을 걸면 모든 신청 생성이 막히기 때문. | 프론트가 두 체크박스 값을 신청 생성 요청에 포함해 전송하고, 그 이후 백엔드가 누락 시 거절하도록 검증을 추가 | 프론트 전용 작업(백엔드 계약은 이미 있음) |
| P2 | 공개 FAQ·행사 전체 탐색 | `FaqPage`, `SupportPage`, `EventsPage`는 서버 목록을 `size: 100`으로 한 번 조회한다. 관리·공지·마이페이지 목록은 서버 페이지 이동이 연결되어 있다. | 100건 초과 운영이 필요하면 공개 화면에도 page/더보기 UI 연결 | 운영 UX 결정 필요 |
| P3 | 한국이름 조회 번들 | 홈 이름 조회가 `data/nameResults.json`을 lazy import한다. 백엔드 조회 API가 없으며 현재 동작에는 문제가 없다. | 서버 검색이 필요하다고 결정되면 검색 API 및 캐시 정책 추가 | 제품·운영 결정 필요 |
| 선택 | 정적 마케팅 콘텐츠 CMS | 회사 정보, 파트너, 상품, 정책 문안, 행사 PROGRAM은 코드의 정적 데이터다. | 배포 없이 운영자가 수정해야 할 때 CMS 계약 도입 | 제품 결정 필요 |
| P1 | 직접입력 학생증 School 연결 | 관리자 연결 API(`PUT /api/admin/applications/{applicationId}/school`)는 구현·테스트 완료(2026-09-16, 카드 생성 후 잠금·schoolType 일치 검증 포함). 프론트 래퍼·관리자 연결 UI는 아직 없다(`services/api.ts` grep 0건). `schoolId=null` 직접입력 신청은 School 연결 전까지 카드 제작이 막힌다. | 프론트 래퍼와 관리자 연결 UI 추가 | 없음(프론트 전용 작업) |

### 현재 갭이 아닌 항목

- 입금자명은 `PATCH /api/applications/{id}/depositor`로 저장된다.
- 관리자 통계는 `GET /api/admin/stats`를 `OverviewSection`에서 호출한다.
- 공지 검색과 페이지 이동은 서버 `searchType`, `keyword`, `page`, `totalPages`에 연결되어 있다.
- 단체 Excel 성씨 열은 현재 워킹 트리에서 선택 열로 구현되어 있다. 다만 `docs/collab/TODO.md` 상단의 “Excel에서 성씨를 받지 않는다” 정책과 충돌하므로, 이 저장소를 합치기 전 정책 문서와 구현 중 하나를 정합화해야 한다. 이번 구조 정리에서는 기존 작업자의 코드를 보존했다.
- 사용되지 않던 단건 `getActiveManseryeokResult` 프론트 래퍼는 제거했다. 신청 단위 `listManseryeokResults`가 운영 복원 경로다.

## 2. 현재 완료 상태
| 영역 | 프론트 연결 상태 | 대표 코드 |
|---|---|---|
| 인증·계정 | 이메일 인증 가입, 로그인, OAuth, 약관, refresh 1회 재시도, 로그아웃, 계정 복구, 내 정보·비밀번호·탈퇴 연결 | `AuthContext.tsx`, 계정 pages, `services/api.ts` |
| 신청 | 개인·단체 multipart 생성, 조회, 취소, 사진 재업로드, 입금자명, 카드 다운로드, 내 목록·상세 연결 | `ApplyPage`, `LookupPage`, `MobileCardPage`, `MyPage` |
| 후기 | 목록·상세·작성·수정·삭제와 0~5개 이미지 연결 | review pages |
| 게시판 | 공지·FAQ 목록·상세, 관리자 CRUD·첨부, 공지 검색·페이지 이동 연결 | notice/FAQ pages, `BoardsSection` |
| 행사 | 공개 부스·협업 목록/상세와 관리자 CRUD·이미지·로고 연결 | `EventsPage`, `EventAdminPanel` |
| 문의 | 사용자 작성·내 목록·상세, 관리자 답변·상태 변경 연결 | inquiry pages, `InquiriesSection` |
| 관리자 신청 | 목록·상세·멤버, 상태 전이 8종, Excel 입출력, 카드번호, ZIP/개별 다운로드 연결 | `ApplicationsSection`, `ApplicationDetail` |
| 작명·만세력 | 확정 결과 일괄 복원, 출생지역 검색·해석·확정, 결정적 상위 5개 추천, 이름 저장 연결 | `NamingCard`, `ManseryeokPanel`, `useManseryeokResults` |
| 카드·학교 | 디자인 조회, 미리보기, 생성, 학생증 템플릿 조회·업로드, 학생증 앞·뒤 텍스트 색상 선택(STUDENT 전용, `CardProductionPanel`), 십이간지 디자인 세트 선택(신청 상세 레벨, `ApplicationDetail`) 연결 | `CardProductionPanel`, `SchoolTemplateSection`, `ApplicationDetail` |
| 페이지 이동 | 관리자 신청·후기·게시판·행사, 마이페이지 신청·후기, 공개 공지 연결 | `AdminPager`, 각 목록 page |
| 다국어 | 언어 상태, 번역 사전, `Accept-Language`, 서버 오류 메시지 연결 | `features/i18n`, `services/api.ts` |

## 3. 우선순위

1. `/me` role 계약을 백엔드와 확정한다. 신청 건별 동의 이력은 백엔드 저장 계약이 이미 있으니 프론트에서 값 전송을 연결한다.
2. 공개 FAQ·행사가 실제로 100건을 넘을 때 페이지 이동을 연결한다.
3. 이름 조회 API와 정적 CMS는 운영상 서버 관리가 필요해질 때만 진행한다.

프론트 단독으로 즉시 처리해야 하는 P0 갭은 현재 없다.

## 4. 검증 상태

2026-09-15 구조 정리 후 다음을 확인했다.

- `services/api.ts` 내용과 79개 API 메서드 호출 집합·호출 횟수가 구조 정리 전과 동일하다.
- TypeScript strict 검사와 미사용 local/parameter 검사가 통과했다.
- Vite 프로덕션 빌드가 통과했다.
- 이름 추천·만세력 adapter 순수 로직 테스트 7개가 통과했다.
- 빌드 결과를 Edge에서 실행한 격리 브라우저 회귀 테스트 5개가 통과했다. 공개 주요 라우트, 공지, 마이페이지, 언어 전환, 개인·단체·학생 신청 정보, 관리자 로그인·신청 상세·이름 저장·만세력·카드·페이지 이동을 확인했다.
- 실제 Docker 백엔드 통합 E2E는 Docker 엔진이 실행 중이지 않아 이번 검증에서 재실행하지 않았다. 브라우저 회귀 테스트는 명시적인 HTTP fixture를 사용하므로 백엔드 동작 검증을 대체하지 않는다.

## 5. 변경 이력

| 날짜 | 변경 |
|---|---|
| 2026-09-20 | 십이간지 디자인 세트(P0) 완료 처리 — `ApplicationDetail.tsx`에 텍스트 선택 UI 추가(설계 노트의 "zodiacDesignSet과 같은 영역"과 달리, 신청 전체 1개·멤버별 아님이라는 실제 성격에 맞춰 `CardProductionPanel`이 아닌 신청 상세 레벨에 배치 — 의도적 이탈). 학생증 앞·뒤 텍스트 색상(P2)도 함께 완료 처리(구현 자체는 커밋 `4ae03ec`로 이미 완료됐었는데 이 문서 반영이 누락돼 있었음). 둘 다 "2. 현재 완료 상태"로 이동 |
| 2026-09-20 | 학생증 앞·뒤 텍스트 색상 선택 UI(P2) 신규 추가 — 백엔드가 2026-09-19에 구현 완료했으나 이 문서에 반영되지 않고 있던 갭 |
| 2026-09-16 | `git pull` 병합 충돌 해소. 십이간지 디자인 세트(zodiacDesignSet) P0 갭과 직접입력 학생증 School 연결 갭을 "1. 현재 미완료 갭" 표에 재기재(재구조화 과정에서 누락됐던 항목, grep으로 프론트 미착수 재확인). School 연결 항목은 백엔드 API 구현·테스트 완료 상태로 갱신 |
| 2026-09-15 | 누적 조사 기록을 현재 상태 문서로 재작성. 완료·미완료·우선순위·검증을 분리하고 구조 정리 결과 및 정책 충돌을 반영 |
| 2026-09-14 | 인증 상태 단일화, 만세력 재진입 복원, 결정적 추천, Excel 성씨 열, 목록 페이지 이동, 행사 PROGRAM 정적화 구현 |
| 2026-08-18~09-13 | 목데이터 제거와 도메인별 API 연결을 순차 검증·구현. 상세 이력은 `docs/collab/CHANGELOG.md` 참고 |
