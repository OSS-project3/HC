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

### 0.1 최근 백엔드 추가 (2026-08-25, 원격 병합) ✅
협업자가 아래를 추가했다. **본 문서의 일부 TODO가 이미 구현됨.**
| 엔드포인트 | 내용 |
|---|---|
| `POST /api/admin/applications/{id}/naming-result` | **saju 프로그램이 돌려준 "사주이름" 엑셀 업로드 → 구성원 한글/한자 이름 반영**(DB `application_members.name/chinese_name`). `NamingResultExcelParser`가 이메일/전화번호로 행 매칭, "사주이름" 열 파싱, all-or-nothing 검증 |
| `POST /api/admin/applications/{id}/reject-photo` | 사진 반려(상태 전이) |
| `POST /api/admin/applications/{id}/start-producing` | 제작 시작 |
| `POST /api/admin/applications/{id}/card-ready` | 카드 발급 완료 |
| `POST /api/admin/applications/{id}/dispatch` | 실물 배송(운송장) |
| `POST /api/admin/applications/{id}/complete-naming` | 작명 완료 → 제작대기 |

> **결과: 아래 §1의 API-5(상태 전이) 완료. 이름의 DB 저장(§2 DATA-2)은 "엑셀 왕복" 방식으로 이미 해결됨.**

---

## 작명 방식 — 두 경로 공존 (둘 다 DB 저장)

작명 결과를 DB에 넣는 경로가 두 가지이며, **둘 다 `application_members`에 저장**되어 공존 가능하다.

| | (A) 엑셀 왕복 | (B) 인앱 추천·선택 (현 대시보드) |
|---|---|---|
| 추천/작명 주체 | **외부 saju 프로그램**(05solar/saju web) | **브라우저**(manseryeok + 번들 700개) |
| DB 저장 | ✅ `naming-result` 엑셀 업로드 | ✅ `POST .../members/{mid}/name`(API-2) |
| localStorage | 미사용 | **미사용**(제거 완료) |
| 남은 것 | 엑셀 **export** 엔드포인트 + 프론트 업로드 UI | (핵심 완료) 추천/만세력까지 백엔드로 옮길지는 선택 |

- 두 경로 모두 최종적으로 구성원 한글/한자 이름을 DB에 반영하므로, 운영에서 병행하거나 하나를 선택해도 된다.
- 공통으로 필요한 것은 **엑셀 내보내기(export)** 하나 — (A)는 saju 입력·결과 왕복에, (B)는 완료 명단 export에 쓴다.

---

## 1. 신규로 필요한 API

| # | 엔드포인트 | 용도 | 난이도 | 상태/선행 |
|---|---|---|---|---|
| API-0 | `POST .../{id}/naming-result` (엑셀 업로드) | saju 결과 엑셀로 이름 DB 반영 | — | ✅ **완료**(원격) |
| API-2 | `POST .../{id}/members/{mid}/name` | 인앱 이름 확정 저장 → `application_members.name/chinese_name` + `name_selection_stats` +1 | 🟢 | ✅ **완료** |
| API-2b | `GET /api/admin/name-selection-stats` | 이름별 선택 이력 카운트 조회 | 🟢 | ✅ **완료** |
| API-3 | `POST /api/admin/applications/export` | 신청/구성원 명단 엑셀 내보내기 | 🟡 | ⬜ **필요**(공통) |
| API-1 | `GET .../{id}/members/{mid}/name-recommendations?limit=8` | 추천을 백엔드로(현재 프론트 계산) | 🟡 | ⬜ 선택 · DATA-1,4 |
| API-4 | 만세력 계산 경로 — 사이드카 or 프론트 유지 | 4주·오행 분포 | 🔴/⬜ | ⬜ 선택 |
| API-5 | 상태 전이(반려/제작/발급/배송/작명완료) | 신청 상태 관리 | — | ✅ **완료**(원격, §0.1) |
| API-6 | `GET /api/admin/reviews` | 후기 모더레이션 목록(숨김 포함) | 🟢 | ⬜ 선택 |
| API-7 | 관리자 승격 경로(정식) | 임시 로그인/시드 대체 | 🟡 | ⬜ 운영 전 필수 |

> **✅ 확정 저장/선택이력은 백엔드로 완료** — 대시보드에서 이름을 선택하면 서버(DB)에 저장되고
> 프론트 localStorage를 전혀 쓰지 않는다(데이터 유출 방지). 확정 여부는 서버 `member.assignedName`로 판정.
> **남은 공통 과제: API-3(엑셀 내보내기)** — 프론트 "엑셀 내보내기" 버튼이 이것만 붙이면 동작한다.

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
| DATA-2 | 확정(선택)된 이름 | ~~localStorage~~ → **DB** | ✅ **완료** — `application_members.name/chinese_name`(API-2). localStorage 제거됨 |
| DATA-3 | 이름 선택 이력(+1 카운트) | ~~localStorage~~ → **DB** | ✅ **완료** — 신규 테이블 `name_selection_stats`(API-2/2b) |
| DATA-1 | 미리 지어진 이름 700개(한자·오행·뜻) | 프론트 번들 `sajuNames.json` | ⬜ 선택 — DB 테이블 `saju_names`로 옮겨 추천을 백엔드화할 때 |
| DATA-4 | 추천 점수화 로직 | 프론트 `adminNamingMock.ts` | ⬜ 선택 — 백엔드 `@Service`(recommend.py 이식) |
| DATA-5 | 만세력 계산 로직 | 프론트 `lib/saju.ts`(manseryeok) | ⬜ 선택 — (API-4 참조) |

> ✅ 확정 이름·선택이력은 **DB 저장으로 이전 완료**(프론트 localStorage 미사용).
> DATA-1/4/5(추천 후보·점수화·만세력)는 여전히 프론트에서 처리한다. 이것까지 백엔드로 옮길지는 선택 사항.

---

## 3. 데이터 모델

> ✅ **구현됨**: `name_selection_stats(id, name, hanja, selected_count, ...)` — 이름별 선택 카운트(ddl-auto로 자동 생성).
> 확정 이름은 기존 `application_members.name/chinese_name/name_meaning/name_interpretation`에 저장(엔티티 `assignKoreanName`).
> 아래 `saju_names`는 추천을 백엔드로 옮길 때만 필요(현재는 프론트 번들).

```sql
-- (선택) DATA-1: 이름 사전 (sajuNames.json 이관) — 추천을 백엔드화할 때
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

1. **API-3 엑셀 내보내기** — 남은 공통 최우선. 프론트 "엑셀 내보내기" 버튼이 이것만 붙이면 동작.
2. **프론트 연동**: 상태 전이 버튼(백엔드 ✅ 완료) 대시보드에 붙이기. (엑셀 왕복도 쓰려면 결과 업로드 UI 추가.)
3. 운영 직전 **API-7**(관리자 승격) + 임시 로그인·시드 제거.
4. (선택) 추천 후보·점수화·만세력까지 백엔드로 — DATA-1/4/5 + API-1/4.

> ✅ 이미 완료: 상태 전이(API-5), 이름 DB 반영(API-0 엑셀 / API-2 인앱), 선택이력(API-2/2b).
> 확정 이름·선택이력은 **프론트 localStorage를 쓰지 않고 전부 DB에 저장**된다.

---

## 5. 프론트 연동 지점(이관 시 수정 대상)

- `frontend/src/services/api.ts` — 신규 엔드포인트 client 함수 추가.
- `frontend/src/components/admin/sections/ApplicationsSection.tsx` — 추천/확정/선택이력 호출을 API로 교체.
- `frontend/src/data/adminNamingMock.ts` · `frontend/src/data/sajuNames.json` — 백엔드 이관 후 제거/축소.
- `frontend/src/lib/saju.ts` — 만세력을 서버화하면 제거(유지 시 존치).
