## 카드 도메인 (CardType / CardDesign / CardFieldDefinition)

> ✅ 2026-07-29 방향: 사용자 API와 관리자 API를 나눠서 설계.

### ① 도메인의 책임

카드 종류(`CardType`)·디자인(`CardDesign`)·디자인 내 출력 항목 좌표(`CardFieldDefinition`)를 관리한다. 사용자에게는 선택 가능한 디자인 목록/상세를 보여주고, 관리자에게는 이를 등록·수정하는 기능을 제공한다. (`.md` 4절 기준)

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `DesignPage.tsx` | 카테고리별(명예한국인증/명예시민증/학생증/방문증) 카드 디자인 전체 목록 — `cards.ts` **정적 데이터**를 그대로 렌더링, API 호출 없음 |
| `MainDesignsSection.tsx`(홈) | 주요 디자인 미리보기 이미지 + `/apply?designId=xxx` 링크 — 마찬가지로 정적 |
| `ApplyPage.tsx` | URL의 `designId`로 `findCardDesign()`(cards.ts 함수) 호출해 디자인 정보를 가져옴 |
| 관리자 화면 | ⚠️ **없음** — `AdminPage.tsx`는 신청 관리만 하고, 카드 종류/디자인을 등록·수정하는 화면 자체가 없음 |
| `CardFieldDefinition`을 쓰는 화면 | ⚠️ **없음** — 카드 이미지에 이름/사진을 좌표 기반으로 합성하는 기능 자체가 미구현 (8절에 이미 기록된 사항) |

### ③ 필요한 API 목록

✅ 2026-07-29 확정: **사용자용 API 없음.** `DesignPage.tsx`/`MainDesignsSection.tsx`/`ApplyPage.tsx`의 디자인 정보 표시는 계속 프론트 정적 자산(`cards.ts`류)으로 처리.

✅ 2026-07-29 확정(관리자 API 범위):
- **`CardType` → 관리자 CRUD 필요** (가격 등 운영 중 계속 바뀜)
- **`CardDesign` → 관리자 CRUD 필요** (카드 종류마다 여러 디자인, 예: 5개 정도를 운영)
- **`CardFieldDefinition` → API 없음.** DB 테이블이 아니라 config/코드 상수로 관리 (`.md` 4.3절 반영) — 운영 중 안 바뀌는 렌더링 고정값이라 CRUD 자체가 불필요

**관리자 API 목록**
1. 카드 종류 등록/목록조회/수정
2. 카드 디자인 등록/목록조회/수정

## 카드 도메인 정리

| # | API | 상태 |
|---|---|---|
| — | 사용자 API | 불필요(정적 프론트 자산 유지) |
| 1 | `POST/GET/PATCH /api/admin/card-types` (카드 종류 관리) | 설계 완료 |
| 2 | `POST/GET/PATCH /api/admin/card-designs` (카드 디자인 관리) | 설계 완료 |
| — | `CardFieldDefinition` | API 없음, config/코드 상수 관리 (필드 구성 `.md` 4.3절에 확정 반영) |

**참고 (시안.zip 확인 결과, 2026-07-31):**
- 카드종류당 디자인 6개 (명예시민증/명예한국인증/방문증 각각 폴더 1~6)
- 학생증 디자인은 추후 전달 예정 — 받으면 동일 구조로 추가
- 뒷면은 한자 유무에 따라 좌표 2벌(`CardFieldDefinition` config에서 처리)
- ✅ 2026-07-31 신규: `CardFieldDefinition`에 `STUDENT_ID`/`DEPARTMENT`/`SCHOOL_LOGO`/`SCHOOL_SEAL` 4개 필드 추가 — `CardType.code=STUDENT`일 때만 활성화(`.md` 4.3절)

---
카드 도메인 완료.

---
