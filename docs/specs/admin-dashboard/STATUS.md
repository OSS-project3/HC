# 관리자 대시보드 · 작명 플로우 — 작업 현황 (2026-08-24)

이 문서는 "어디까지 했는지"를 한눈에 정리한 진행 현황이다.
설계/미구현 API 계약은 [`DESIGN.md`](./DESIGN.md), 임시 로그인 제거 가이드는 [`../../TEMP_ADMIN_LOGIN.md`](../../TEMP_ADMIN_LOGIN.md) 참고.

관련 커밋: `5e9d554`(대시보드 신규) → `295e72b`(신청 시드) → `0992c3f`·`b86f35d`·`54eabf4`(표·오행 스타일)
→ `d6148f6`(실제 이름 데이터) → `70c74be`(실제 만세력+구성원) → `fef3605`(추천 새로고침 버튼).

---

## 1. 한 줄 요약

관리자 페이지를 **사이드바 대시보드로 신규 구축**하고, **제작신청 작명 플로우**(구성원 → 실제 만세력 →
실제 이름 추천 → 이름 확정)를 만들었다. **API가 있는 영역은 실제 연결**, 없는 영역은 **UI + 문서**로 처리했다.
`admin@test.com / admin1234!` 로 로그인해 `/admin`에서 확인 가능(임시 계정, 운영 전 제거).

---

## 2. 기능별 현황

### 2.1 대시보드 셸 ✅
- `frontend/src/pages/AdminPage/AdminPage.tsx` — 좌측 사이드바 + 섹션 전환(해시 딥링크).
- 섹션: 개요 / 제작신청 관리 / 공지사항 / FAQ / 행사사업 / 후기 / 고객지원(문의).
- 디자인: 기존 사이트 토큰(`tokens.css`) 재사용, 대시보드 형식.

### 2.2 콘텐츠 관리 (실제 API 연결) ✅
| 영역 | API | 상태 |
|---|---|---|
| 공지사항 / FAQ | `/api/admin/boards` (CRUD) | ✅ 연결 (`BoardsSection` + 기존 `BoardAdminPanel`) |
| 행사사업(부스/협업) | `/api/admin/events` (CRUD+이미지) | ✅ 연결 (기존 `EventAdminPanel`) |
| 후기 | `/api/reviews` (목록·삭제, 삭제는 ADMIN 허용) | ✅ 연결 (`ReviewsSection`) |
| 1:1 문의 | `/api/admin/inquiries` (목록·상세·답변·상태) | ✅ 연결 (`InquiriesSection`, client 함수 신규) |

### 2.3 제작신청 관리 ✅ (핵심)
- 목록/상세: **실제 API** `/api/admin/applications`, `/api/admin/applications/{id}`.
- 구성원: **실제 API 신규** `GET /api/admin/applications/{id}/members` — 이름·출신국가·성별·생년월일 등.
- 개인/단체 탭 분리. 개인은 여러 건 선택 → (엑셀 버튼).
- **만세력(四柱): 실제 계산** — npm `manseryeok`으로 구성원 생년월일/시간에서 4주·오행 분포 산출
  (`frontend/src/lib/saju.ts`). 계산 불가 시 mock 폴백.
- **이름 추천: 실제 데이터** — `사주 이름 결과.xlsx` + saju 레포 `names.json` 병합 700개
  (`frontend/src/data/sajuNames.json`). recommend.py 점수화 이식(자원오행×2+발음오행×1+상생/상극).
  한 번에 **8개**, **"↻ 다른 이름 추천"** 버튼 또는 새로고침으로 새 조합.
- **구성원 정보 표시**: 개인=신청정보에 출신국가·성별·생년월일 / 단체=멤버별 이름·국가·성별·만세력·추천.
- **상태 전환**: 이름 선택 전 **접수** → 선택 후 **작명 완료**, 카드가 compact로 접힘.
- **선택 이력(+1)**: 이름별 선택 카운트 표시(굵게).

### 2.4 로그인 ✅ (임시)
- `admin@test.com / admin1234!` → 실제 `/api/auth/login`(백엔드 시드 ADMIN 계정)으로 세션 확보,
  실패 시 클라이언트 admin 폴백. **운영 전 제거 대상** — `TEMP_ADMIN_LOGIN.md`.

### 2.5 데모 시드 ✅
- 관리자 계정(`DemoDataSeeder.ensureAdminUser`).
- 제작신청 6건: 개인 4(2건 작명중) + 단체 2(멤버 3·4명), 신청자/멤버/수령인 포함.
  번호대 `APP-2026-900001~`(실제 채번 시퀀스와 충돌 회피). `count()==0`일 때만 시드.

---

## 3. 실제 vs 임시(mock/localStorage) 구분 — 중요

| 항목 | 상태 |
|---|---|
| 콘텐츠 CRUD(공지/FAQ/행사/후기/문의) | ✅ **실제 백엔드** |
| 신청 목록/상세/구성원 | ✅ **실제 백엔드** |
| 만세력(四柱) 계산 | ✅ **실제**(manseryeok, 프론트) — 해외 진태양시 보정만 미적용 |
| 추천 이름 데이터/점수화 | ✅ **실제 데이터**(700개) + 점수화 — 단, 프론트에서 계산 |
| 이름 확정 저장 · 선택이력 집계 | 🟡 대시보드 인앱 선택은 **localStorage**. 단, **엑셀 업로드(`POST .../naming-result`)로는 DB 반영 가능**(원격 2026-08-25) — 프론트 미연동 |
| 엑셀 내보내기(이름 포함) | 🟡 **버튼만**(export 엔드포인트 미구현) |
| 신청 상태 전이(반려/제작/발급/배송/작명완료) | ✅ **백엔드 완료**(원격 2026-08-25) — 프론트 버튼 미연동 |
| 관리자 로그인/권한 승격 | 🟡 **임시 시드/하드코딩** |

---

## 4. 신규/변경 산출물

**백엔드**
- `api/AdminApplicationController.java` — `GET /{id}/members` 추가.
- `domain/application/service/ApplicationService.java` — `getApplicationMembersForAdmin`.
- `domain/application/dto/AdminApplicationMemberResponse.java` — 신규.
- `domain/user/entity/User.java` — `promoteToAdmin()`.
- `infra/seed/DemoDataSeeder.java` — 관리자·제작신청 시드.

**프론트엔드**
- `pages/AdminPage/AdminPage.tsx`(재작성) + `AdminPage.css`.
- `components/admin/sections/{Applications,Boards,Reviews,Inquiries}Section.tsx` — 신규.
- `lib/saju.ts` — 실제 만세력(manseryeok).
- `data/sajuNames.json`(700개) · `data/adminNamingMock.ts`(추천 점수화·선택/확정 helper).
- `services/api.ts` — admin applications/inquiries/members, `loginWithPassword`.
- `features/auth/AuthContext.tsx`(role 유지 버그 수정) · `pages/LoginPage/LoginPage.tsx`(임시 admin).
- 의존성: `manseryeok@^2.0.0`.

**문서**
- `docs/specs/admin-dashboard/DESIGN.md`(미구현 API 계약·saju 통합·라이선스).
- `docs/TEMP_ADMIN_LOGIN.md`(임시 로그인 제거 가이드).
- 본 문서 `STATUS.md`.

---

## 5. 남은 작업 (TODO)

> 상세·결정사항은 [`BACKEND_TODO.md`](./BACKEND_TODO.md) 참고. 요약:
1. **작명 방식 결정** — (A) 엑셀 왕복(`naming-result` 이미 있음) vs (B) 인앱 추천 저장. → BACKEND_TODO §"결정 필요".
2. **엑셀 내보내기 API**(`POST .../export`) — A·B 공통 필요. 현재 프론트 버튼만.
3. **프론트 연동**: 상태 전이 버튼(백엔드 ✅ 완료), 결과 엑셀 업로드 UI(`naming-result` ✅), 를 대시보드에 붙이기.
4. **만세력 진태양시 보정** — 해외 출생 시차/경도(`birthCities`) 반영(정확도).
5. **관리자 승격 정식 경로** — 임시 로그인/시드 제거 후 대체.
6. (B 선택 시) 이름사전 DB 적재 + 추천 API + 확정 저장/선택이력.

---

## 6. 실행·확인 방법

```bash
docker compose up -d --build           # db·redis·minio·backend·frontend
# http://localhost:3000 → 로그인 admin@test.com / admin1234! → 상단 '관리'
```
- 제작신청 관리 → 개인/단체 탭 → 행 펼치기 → 만세력·추천 확인 → 이름 선택(작명 완료).
- 데모 신청이 없으면 예시 카드로 플로우 미리보기가 뜬다.
- 로컬 이미지 서빙은 MinIO가 필요(`docker-compose`에 포함). 상세는 `../../TEMP_ADMIN_LOGIN.md`/`DOCKER.md`.
