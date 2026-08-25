# 관리자 대시보드 — 설계 및 API 갭 문서

작성 배경: 관리자 페이지를 대시보드 형식으로 신규 구축하면서, **이미 존재하는 API는 연결**하고
**존재하지 않는 기능은 프론트엔드 UI만 먼저** 구현했다. 이 문서는 (1) 데이터 소스 현황,
(2) 아직 없는 백엔드 기능의 제안 설계, (3) 만세력/이름추천(saju) 통합 아키텍처와 **라이선스 블로커**를 정리한다.

---

## 1. 데이터 소스 현황 (2026-08-24 기준)

| 관리 영역 | 백엔드 API | 프론트 연결 | 비고 |
|---|---|---|---|
| 공지사항 / FAQ | ✅ `/api/boards`, `/api/admin/boards` (CRUD) | ✅ 연결됨 | `BoardAdminPanel` 재사용 |
| 행사사업(BOOTH/COLLAB) | ✅ `/api/admin/events` (CRUD+이미지) | ✅ 연결됨 | `EventAdminPanel` 재사용 |
| 후기 | ✅ `/api/reviews` (삭제는 ADMIN 허용) | ✅ 연결됨(목록/삭제) | 전용 admin 목록 API는 없음 → 공개 목록 재사용 |
| 1:1 문의 | ✅ `/api/admin/inquiries` (목록/상세/답변/상태) | ✅ 연결됨 | client 함수 신규 추가 |
| 제작신청 목록/상세 | ✅ `/api/admin/applications` (읽기 전용) | ✅ 연결됨 | client 함수 신규 추가 |
| 제작신청 카드 다운로드 | ✅ `/api/applications/{id}/cards/download` | (사용자용) | 관리자용 별도 없음 |
| **이름 추천 / 만세력** | ❌ 없음 | 🟡 UI만(mock) | 본 문서 §3 |
| **이름 선택 이력(+1)** | ❌ 없음 | 🟡 UI만(localStorage) | 본 문서 §2.3 |
| **신청 엑셀 내보내기(이름 포함)** | ❌ 없음 | 🟡 UI만(버튼) | 본 문서 §2.4 |
| 신청 상태 전이(발급/반려/배송) | ❌ 컨트롤러 미노출 | 🟡 UI만 | 서비스 계층엔 일부 존재, 엔드포인트 없음 |
| 관리자 권한 승격 | ❌ 없음 | — | DB 직접 수정만 가능 |

> 참고: `ApplicationMember` 엔티티에는 이미 작명 결과를 담을 자리(`name`, `chineseName`,
> `nameMeaning`, `nameInterpretation`)와 사주 입력(`birthTime`, `birthRegion`)이 있으나
> 모두 "작명 단계(범위 밖)에서 채워짐" 주석 상태이고 **세터/엔드포인트가 없다.**

---

## 2. 신규로 필요한 백엔드 엔드포인트 (제안)

프론트는 아래 계약을 가정하고 UI를 만들어 두었다. 백엔드 구현 시 이 계약에 맞추거나, 변경 시 프론트 `api.ts`를 함께 수정한다.

### 2.1 신청 만세력 조회
```
GET /api/admin/applications/{applicationId}/members/{memberId}/saju
→ 200 {
    memberId, birthDate, birthTime, birthRegion, utcOffset,
    pillars: { year:{stem,branch,hanja}, month:{...}, day:{...}, hour:{...} },
    elementCounts: { "목":n,"화":n,"토":n,"금":n,"수":n },
    missingElements: ["수", ...]
  }
```
- 개인 신청은 member가 1명, 단체 신청은 엑셀 행마다 member.
- 계산 자체는 §3의 만세력 엔진(Node 사이드카 or 포팅)에 위임한다.

### 2.2 이름 추천
```
GET  /api/admin/applications/{applicationId}/members/{memberId}/name-recommendations?limit=8
→ 200 { need:[{element,weight}], recommendations:[
      { nameId, name, hanja, roman, reading, meaning, jawon:[…], eum:[…], score, reasons:[…] }
    ] }
```
- 입력은 §2.1의 `elementCounts`. 내부적으로 saju 이름 점수화 로직(§3-B) 사용.

### 2.3 이름 선택(확정) + 선택 이력 +1
```
POST /api/admin/applications/{applicationId}/members/{memberId}/name
     body { nameId, name, hanja, reading, meaning }
→ 200 { memberId, assignedName:{…} }
```
- 이 호출이 성공하면 **해당 이름(nameId)의 선택 카운트를 +1** 한다.
  선택 이력 집계를 위해 별도 테이블이 필요하다:
  ```
  name_selection_stats(name_id PK, name, hanja, selected_count, updated_at)
  ```
  또는 이벤트 로그 테이블 `name_selection_log(id, name_id, application_id, member_id, admin_id, created_at)`를
  두고 카운트는 집계 뷰로 산출(감사 추적에 유리).
- 확정 결과는 `ApplicationMember.name/chineseName/nameMeaning/nameInterpretation`에 반영.

### 2.4 이름 포함 엑셀 내보내기
```
POST /api/admin/applications/export        // 여러 건을 하나의 엑셀로
     body { applicationIds:[…], type:"INDIVIDUAL"|"GROUP" }
→ 200 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (xlsx 바이너리)
```
- 개인: 선택된 여러 신청 건을 한 시트에 행으로.
- 단체: 업로드된 원본 엑셀 컬럼 + 확정 이름 컬럼(한글/한자/뜻)을 열 순서대로 append.
- 백엔드 xlsx 라이터 필요(현재 `BulkExcelParser`는 **읽기 전용**, 쓰기 유틸 없음 → Apache POI 등 추가).

---

## 3. 만세력 / 이름추천 통합 아키텍처

참조 레포: `https://github.com/05solar/saju.git` (분석 결과 요약).

### 구조
- **만세력(四柱) 계산**: 이 레포의 자체 코드가 아니라 npm 패키지 **`manseryeok`** 을 브라우저에서 호출한다.
  `web/src/lib/saju.ts`의 `computeSaju()` / `computeSajuAtBirthplace()`(해외 출생 → 진태양시/시차 보정)가 래퍼.
  결과에서 4주(년/월/일/시)의 천간·지지·오행을 뽑고, 오행 카운트 `{목,화,토,금,수}`와 결핍 오행을 산출.
- **이름 추천**: Python FastAPI(`backend/app/recommend.py`)가 오행 카운트를 받아 점수화.
  데이터: `backend/data/names.json`(700개, 2글자 이름 · 한자 · 자원오행 · 발음오행 · 뜻),
  `hanja-element.json`(409 한자→오행). 엔드포인트 `POST /api/recommend { elements, limit }`.
  점수식: `자원오행_avg*2 + 발음오행_avg*1` + 결핍보완 가중(결핍3/약1) + 상생/상극 보정.

### 통합 옵션(권장안)
1. **만세력 계산 = Node 사이드카.** `manseryeok`를 감싼 초경량 Express/Fastify 서비스로 4주+오행 카운트만 반환.
   - 이유: 음력/절기/진태양시/DST가 얽힌 만세력을 Java로 재구현하면 오차·버그 위험이 크다. 래퍼가 ~90줄이라 거의 그대로 재사용 가능.
   - 우리 출생지역 UTC 시차 데이터(`frontend/src/data/birthCities.ts`)를 진태양시 보정 입력으로 넘길 수 있다.
2. **이름 추천 = Java 포팅(권장) 또는 FastAPI 마이크로서비스 유지.**
   - `recommend.py`는 프레임워크 의존 없는 ~90줄 산술 → Spring `@Service`로 포팅 용이(약 1일).
   - names 데이터는 테이블 적재 또는 인메모리 캐시.

### 데이터 흐름(제안)
```
[관리자 UI]
  └─(신청 birth 정보)→ [Spring /api/admin/.../saju]
                          └─→ [Node 사이드카: manseryeok] → 4주 + 오행 카운트
  └─(오행 카운트)────→ [Spring /api/admin/.../name-recommendations]
                          └─→ [이름 점수화(Java 포팅) + names 데이터] → 추천 목록
  └─(이름 확정)─────→ [Spring POST .../name] → member 반영 + 선택 카운트 +1
  └─(엑셀 내보내기)──→ [Spring POST /export] → xlsx(이름 컬럼 포함)
```

### ⚠️ 라이선스 블로커 (중요)
- 참조 레포 `05solar/saju`에는 **LICENSE 파일이 없다** → 기본적으로 "All rights reserved".
  README는 `backend/data/*`(이름 corpus)를 "민감 데이터"로 명시하고 서버측에 둔다고 밝힌다.
- 따라서 **names.json/코드/데이터를 재배포하려면 저자(05solar)의 허락이 필요**하다(05solar가 본 프로젝트 소유자/협업자면 무방).
- ✅ 현재 상태: 소유자 제공 데이터(`사주 이름 결과.xlsx`) + saju 레포 `names.json`(자원/발음오행)을 병합한
  **실제 이름 700개**를 `frontend/src/data/sajuNames.json`으로 번들하고, `adminNamingMock.ts`가 recommend.py
  점수화 로직(자원오행×2+발음오행×1+상생/상극 보정)을 이식해 오행 결핍 기반으로 매번 무작위 8개를 추천한다.
- ✅ **만세력(四柱) 계산도 실제 적용됨**: saju 레포와 동일하게 npm `manseryeok`를 프론트에서 사용
  (`frontend/src/lib/saju.ts`). 구성원의 생년월일/출생시간으로 4주·오행 분포를 실제 계산해 추천에 반영한다.
  (해외 출생 진태양시 보정은 아직 미적용 — 필요 시 birthCities의 시차로 확장.)
- ✅ **구성원 조회 API 신설**: `GET /api/admin/applications/{id}/members` — 이름·출신국가·성별·생년월일 등.
  개인=1명/단체=엑셀 행 N명. 작명 화면이 이 데이터로 만세력을 계산하고 멤버별 정보를 표시한다.
- ✅ **이름 확정 저장·선택이력 = 백엔드 완료**: 대시보드에서 이름 선택 → `POST /api/admin/applications/{id}/members/{mid}/name`로
  `application_members`에 저장 + `name_selection_stats` +1. **프론트 localStorage 미사용**(데이터 유출 방지). 상세는 [`BACKEND_TODO.md`](./BACKEND_TODO.md).
- 🟡 남은 것: **엑셀 내보내기(export)** 만 미구현.
- `manseryeok` npm 패키지도 **별도 라이선스 확인** 후 사용한다.
- 추가 한계: 데이터가 성(姓) 없는 2글자 이름 700개뿐 → 성명학(획수) 기반 감명이나 성-이름 궁합은 추가 데이터 필요.

---

## 4. 관리자 신청 관리 플로우 (구현된 UI 기준)

### 개인 신청
1. 목록에서 **개인 신청만** 필터링해 확인.
2. 건별로 만세력 조회(§2.1) → 만세력/오행 표시.
3. 만세력 기반 추천 이름 표시(§2.2).
4. 관리자가 추천 이름 중 선택 → 해당 이름 **선택 이력 +1**(§2.3) + member에 확정.
5. 여러 건을 선택해 **하나의 엑셀**로 내보내기(§2.4).

### 단체 신청
1. 단체 신청으로 들어온 **엑셀 원본 값**을 읽음(백엔드는 이미 파싱해 member로 저장; 관리자 상세에서 member 목록 노출).
2. 엑셀 행(=member)마다 만세력 → 추천 이름(§2.1~2.2).
3. 행마다 이름 확정 시 **선택 이력 +1**(§2.3).
4. 확정 이름 컬럼이 추가된 엑셀 내보내기(§2.4).

> 상태 전이(발급/반려/배송)와 관리자 권한 승격은 이번 범위에서 UI만 두거나 제외했다. 엔드포인트 신설 필요.
