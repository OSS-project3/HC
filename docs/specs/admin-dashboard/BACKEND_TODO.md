# 관리자 페이지 — 필요한 API · 백엔드 이전 데이터 정리 (2026-08-25)

관리 대시보드에서 **아직 백엔드가 없어 프론트/localStorage/mock로 동작하는 부분**을 실제 백엔드로
옮기기 위한 정리 문서. 현황은 [`STATUS.md`](./STATUS.md), 상세 계약/아키텍처는 [`DESIGN.md`](./DESIGN.md),
임시 로그인 제거는 [`../../TEMP_ADMIN_LOGIN.md`](../../TEMP_ADMIN_LOGIN.md) 참고.

> 범례: 🟢 쉬움 · 🟡 중간 · 🔴 어려움 / ✅ 이미 있음 · ⬜ 신규 필요

---

## 0. 현재 이미 연결된 API (참고 — 추가 작업 불필요)

| 영역 | 엔드포인트 |
|---|---|
| 공지/FAQ | `GET /api/boards`, `POST·PATCH·DELETE /api/admin/boards[/{id}]` ✅ |
| 행사사업 | `GET·POST·PATCH·DELETE /api/admin/events[/{id}]` ✅ |
| 후기 | `GET /api/reviews`, `DELETE /api/reviews/{id}`(ADMIN 허용) ✅ |
| 1:1 문의 | `GET /api/admin/inquiries`, `GET .../{id}`, `PATCH .../answer`, `PATCH .../status` ✅ |
| 제작신청 | `GET /api/admin/applications`, `GET .../{id}`, `GET .../{id}/members` ✅ |

---

## 1. 신규로 필요한 API

| # | 엔드포인트 | 용도 | 난이도 | 선행 |
|---|---|---|---|---|
| API-1 | `GET /api/admin/applications/{id}/members/{mid}/name-recommendations?limit=8` | 오행 결핍 기반 추천 이름 N개 | 🟡 | DATA-1, (DATA-4) |
| API-2 | `POST /api/admin/applications/{id}/members/{mid}/name` | 이름 확정 저장(멤버 반영) + 선택이력 +1 | 🟢 | DATA-2, DATA-3 |
| API-3 | `POST /api/admin/applications/export` | 확정 이름 포함 엑셀(xlsx) 내보내기 | 🟡 | API-2 |
| API-4 | (택1) 만세력 계산 경로 — 사이드카 or 프론트 유지 | 4주·오행 분포 산출 | 🔴/⬜ | — |
| API-5 | `PATCH /api/admin/applications/{id}/status` | 신청 상태 전이(발급/반려/제작/배송) | 🟡 | — |
| API-6 | `GET /api/admin/reviews` | 후기 모더레이션 목록(숨김 포함) | 🟢 | — |
| API-7 | 관리자 승격 경로(정식) | 임시 로그인/시드 대체 | 🟡 | — |

### API-1 이름 추천
```
GET /api/admin/applications/{id}/members/{mid}/name-recommendations?limit=8
→ 200 { need:[{element,weight}], recommendations:[
     { nameId, name, hanja, roman, reading, meaning, jawon:[…], eum:[…], score }
   ] }
```
- 입력: 멤버 사주 오행 counts(만세력 결과, API-4). 내부에서 `recommend.py` 점수화 로직 이식 사용.
- 현재: 프론트 `adminNamingMock.ts`가 브라우저에서 계산 중 → 백엔드로 이관.

### API-2 이름 확정 저장 (+선택이력)
```
POST /api/admin/applications/{id}/members/{mid}/name
  body { nameId, name, hanja, reading, meaning }
→ 200 { memberId, assignedName:{ name, hanja } }
```
- `ApplicationMember.name/chineseName/nameMeaning/nameInterpretation` 반영(엔티티에 세터 추가 필요 — 현재 없음).
- 성공 시 해당 이름 **선택 카운트 +1**(DATA-3).
- 상태 정책: 확정 시 `NAME_EDITING → PRODUCTION_READY` 전이 여부 결정 필요.
- 현재: 프론트 localStorage(`admin:member-chosen-names`)로만 시연.

### API-3 엑셀 내보내기
```
POST /api/admin/applications/export
  body { applicationIds:[…], type:"INDIVIDUAL"|"GROUP" }
→ 200 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (xlsx)
```
- 개인=선택된 여러 건을 한 시트로 / 단체=원본 엑셀 컬럼 + 확정 이름(한글·한자·뜻) 컬럼 append.
- 백엔드 **xlsx 라이터 필요**(Apache POI 등). 현재 `BulkExcelParser`는 읽기 전용.

### API-4 만세력 계산 (택1)
- (a) **Node 사이드카**: npm `manseryeok` 감싼 초경량 서비스 → Spring이 호출(정확도 안전, 권장).
- (b) Java 포팅: 음력/절기/진태양시 재구현 — 🔴 위험.
- (c) **프론트 계산 유지**(현 상태, `frontend/src/lib/saju.ts`) — 백엔드 불필요. 급하지 않으면 권장.
- 해외 출생 진태양시 보정 시 출생지 경도/시차(`frontend/src/data/birthCities.ts`) 입력 필요.

---

## 2. 백엔드로 이전 필요한 데이터

| # | 데이터 | 현재 위치 | 이전 대상 | 난이도 |
|---|---|---|---|---|
| DATA-1 | 미리 지어진 이름 700개(한자·자원/발음오행·뜻) | 프론트 번들 `frontend/src/data/sajuNames.json` | DB 테이블 `saju_names` + 시드 | 🟢 |
| DATA-2 | 확정(선택)된 이름 | 브라우저 localStorage `admin:member-chosen-names` | `application_members.name/chinese_name/…` (API-2) | 🟢 |
| DATA-3 | 이름 선택 이력(+1 카운트) | 브라우저 localStorage `admin:name-selection-counts` | DB 테이블 `name_selection_*` | 🟢 |
| DATA-4 | 추천 점수화 로직 | 프론트 `adminNamingMock.ts` | 백엔드 `@Service`(recommend.py 이식) | 🟡 |
| DATA-5 | 만세력 계산 로직 | 프론트 `lib/saju.ts`(manseryeok) | (API-4 참조) | 🔴/유지 |

> 현재 DB 확인 결과: 이름 사전 테이블 **없음**, `application_members`의 확정 이름 컬럼 **전부 NULL**
> (선택해도 DB에 안 써지고 localStorage에만 저장됨).

---

## 3. 제안 데이터 모델(신규 테이블)

```sql
-- DATA-1: 이름 사전 (sajuNames.json 이관)
CREATE TABLE saju_names (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(10)  NOT NULL,   -- 한글 이름
  hanja        VARCHAR(20)  NOT NULL,
  roman        VARCHAR(50),
  reading      VARCHAR(200),            -- 훈음
  meaning      TEXT,
  jawon_1      VARCHAR(2), jawon_2 VARCHAR(2),  -- 자원오행(글자별)
  eum_1        VARCHAR(2), eum_2   VARCHAR(2),  -- 발음오행(초성별)
  UNIQUE (name, hanja)
);

-- DATA-3: 선택 이력(감사 로그 방식 — 카운트는 집계로 산출)
CREATE TABLE name_selection_log (
  id             BIGSERIAL PRIMARY KEY,
  saju_name_id   BIGINT NOT NULL REFERENCES saju_names(id),
  application_id BIGINT NOT NULL,
  member_id      BIGINT NOT NULL,
  admin_id       BIGINT NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT now()
);
-- 또는 단순 집계: name_selection_stats(saju_name_id PK, selected_count INT, updated_at)
```
- DATA-2는 **기존 `application_members`** 컬럼(`name`, `chinese_name`, `name_meaning`, `name_interpretation`)에 반영 —
  엔티티에 세터 추가만 필요.

---

## 4. 권장 진행 순서

1. **DATA-1**(이름 사전 DB) → **API-2 + DATA-2/3**(확정 저장 + 선택이력) — DB에 실제로 데이터가 남는 가장 큰 체감.
2. **DATA-4 + API-1**(추천 점수화 백엔드화). 만세력은 우선 **API-4 (c) 프론트 유지**.
3. **API-3**(엑셀 내보내기) → **API-5**(상태 전이).
4. 운영 직전 **API-7 / C(임시 로그인·시드 제거)**.
5. (선택) **API-4 (a) Node 사이드카**로 만세력까지 서버화.

---

## 5. 프론트 연동 지점(이관 시 수정 대상)

- `frontend/src/services/api.ts` — 신규 엔드포인트 client 함수 추가.
- `frontend/src/components/admin/sections/ApplicationsSection.tsx` — 추천/확정/선택이력 호출을 API로 교체.
- `frontend/src/data/adminNamingMock.ts` · `frontend/src/data/sajuNames.json` — 백엔드 이관 후 제거/축소.
- `frontend/src/lib/saju.ts` — 만세력을 서버화하면 제거(유지 시 존치).
