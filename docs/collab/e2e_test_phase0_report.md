# 개인 명예시민증 E2E 검증 — Phase 0 (선행 조사 + 계약표)

검증 기준: origin/main HEAD `10339ea` (사용자 확인: 764c4ed가 아니라 최신 HEAD 기준으로 진행)
문서 우선순위: 1) 실제 코드 2) api.md 3) TODO.md(✅만 인정) 4) admin-saju.md 5) APPLICATION.md/service-flow.md
CHECK.md/result.md는 명세로 사용하지 않음.
조사 방식: 읽기 전용(Read/Grep/Glob, `git status`/`git log` 등) — 코드 변경 없음.

---

## A. 선행 조사 결과표

| 항목 | Backend | Frontend | E2E | 원인 | 필요한 작업 |
|---|---|---|---|---|---|
| 1. Playwright + config | N/A | 없음 | BLOCKED | `frontend/package.json` devDependencies에 playwright 없음, `playwright.config.*` 없음. `node_modules/playwright`(1.62.1)는 물리적으로 존재하나 lockfile/package.json 미기록 + `.gitignore`로 무시되는 stray 설치 | `@playwright/test` 추가 + config 신규 작성 |
| 2. 개인 비학생증 `ApplicationMember.address` | PASS | PASS | PASS | FE `StepInfo.tsx:767-779`(입력)+`:184-189`(검증), `ApplyPage.tsx:95`(전송) ↔ BE `ApplicationCreateRequest.java:141-144` → `validateCardAddress`(`ApplicationService.java:257-271`) → 저장(`ApplicationMember.java`) | 관리자 프론트 타입 `AdminApplicationMember`(`services/api.ts:158-162`)에 `address` 누락 — 백엔드는 내려주는데 화면에 안 보임 |
| 3. `zodiacDesignSet` 1~5 선택 UI | PASS(API) | **없음(UI 공백)** | HYBRID 필요 | `frontend/src` 전체에 `zodiacDesignSet`/`zodiac-design` grep 0건. API는 실존(`AdminApplicationController.java:135-142`) | HYBRID PASS로 진행 — 같은 로그인 세션 APIRequestContext로 직접 PUT 호출 |
| 4. 추천 후보 수 = 5? | 해당 API 없음 | **실제 8** | 불일치 | 코드 `adminNamingMock.ts:91`(`limit=8`), 문서 `DESIGN.md`/`STATUS.md`도 8. "5"는 e2e_test.md에만 등장 | **사용자 확정 필요**(아래 C-1) |
| 5. 추천 정렬 재현 가능? | — | **비결정적(FAIL)** | BLOCKED | `adminNamingMock.ts:95-99` `Math.random()` 셔플 — `admin-saju.md:282`("무작위 사용 안 함")와 정면 충돌 | 재현성 assertion 불가, 정책 위반 자체가 이슈(아래 C-2) |
| 6. Google Maps 테스트 설정 | 실키 존재, mock 없음 | N/A | 실호출만 가능 | `.env`에 실제 `GOOGLE_MAPS_API_KEY` 존재, 별도 test profile/스텁 없음 | 실호출 기반 검증(플래키 위험) 또는 로컬 스텁 별도 구축 |
| 7. 카드 미리보기/생성 버튼 | PASS | PASS | PASS(단 3번 때문에 실패 예상) | `ApplicationsSection.tsx:729-756`이 실제 `getCardPreview`/`generateCard` 호출, 중복클릭 방지(`disabled={busy}`) 있음 | 없음(zodiacDesignSet 미지정이면 어차피 실패) |
| 8. 테스트 USER/ADMIN 계정 | ADMIN PASS, USER 공백 | N/A | USER는 fixture 필요 | ADMIN: `DemoDataSeeder`(`admin@test.com`/`admin1234!`). USER: 시드 계정이 OAuth 전용(비밀번호 로그인 불가), 정식 가입은 이메일 인증 필요 | E2E fixture로 DB에 USER row 직접 insert(신청 데이터는 직접 삽입 금지, 계정만) |
| 9. HONOR_CITIZEN + 활성 CardDesign 1~6 | PASS | N/A | PASS(격리 DB 전제) | `CardDesignSeeder`가 6개 전부 active 시딩 확인 | 시더가 `count()>0`이면 스킵 → 반드시 격리 DB 사용. CardDesign id는 하드코딩 말고 API로 조회 |
| 10. 얼굴사진 fixture | 검증규칙 확인됨 | N/A | 없음 → 생성 필요 | `ApplicationPhotoValidator` 규칙(≤5MB, jpg/png, magic number 3중 일치, 최소 300×400) 확인. repo에 조건 만족하는 fixture 파일 없음 | ≥300×400 PNG/JPEG fixture 신규 생성 필요 |

---

## B. 계약표 (화면 → 프론트 API → 백엔드 Controller → Service → DB/S3)

27개 단계 전체 추적 완료(신청 제출 → 관리자 검토 → 만세력 확정 → 작명 → 카드번호 → zodiacDesignSet → 작명완료 → 디자인선택 → 미리보기 → 생성 → 다운로드). 상세는 이 세션의 조사 결과 원문 참고 — 핵심만 발췌:

- 신청 제출(`POST /api/applications`) ~ 카드 생성(`POST .../card-generate`) ~ 다운로드(`GET .../cards/download`)까지 전 구간 실제 코드로 연결 확인.
- **UI 공백 2곳** (백엔드 실패 아님): ① zodiacDesignSet 선택(21단계), ② 개인 신청 화면의 전체 ZIP 다운로드 버튼(단체 전용, 27-b단계 — 원래 개인엔 불필요).
- **API 존재하지만 프론트 호출부 없음**: `GET .../manseryeok`(active 조회, 17단계) — 래퍼만 있고 실사용처 없음. E2E에서 APIRequestContext로 직접 검증 가능.

---

## C. 발견된 문서-코드 충돌 (판단 없이 그대로 보고)

**C-1. 추천 후보 개수: 5 vs 8**
- e2e_test.md: "정책상 추천 후보는 5개"
- 코드+DESIGN.md+STATUS.md: 8개
- admin-saju.md: 개수 규정 자체가 없음
→ **사용자 확정 필요**

**C-2. 추천 순서 비결정성 — 정책 위반**
- admin-saju.md:282: "무작위 후보 선택은 사용하지 않습니다... 같은 입력이면 결과 순서가 같아야 합니다"
- 실제 코드(`adminNamingMock.ts`): `Math.random()` 셔플 + 동점 tiebreaker 없음 + "↻ 다른 이름 추천" 버튼으로 의도적 재추첨
→ 명백한 정책 위반. 수정 여부 판단 안 함(지시대로 코드 변경 안 함)

**C-3. api.md가 zodiac-design을 전혀 다루지 않음** (문서 stale, 코드/TODO.md ✅ 기준으로 "구현됨"이 맞음)

**C-4. zodiacDesignSet 프론트 UI 부재가 TODO.md 어디에도 기록 안 됨** (완료 체크리스트가 전부 백엔드/자산/테스트 항목뿐, UI 갭 자체를 언급 안 함)

**C-5. DemoDataSeeder 주석과 실제 @Order 불일치** (경미, 동작엔 영향 없음)

**C-6. `reading` 필드 검증 불일치** — 저장 시점(`NameAssignRequest`)엔 `reading`이 선택인데, 사용 시점(카드 렌더링·작명완료)엔 `nameMeaning`(=reading이 매핑된 값)이 필수 → `reading` 빈 값으로 저장은 성공하고 나중에 `NAMING_INCOMPLETE`로 실패하는 경로 존재

**C-7. 일부 항목 미확인** — birth-region-search/name-selection-stats의 정확한 Controller 클래스, 700개 이름 사전의 reading/meaning 전수 non-blank 여부 등

---

## D. 코드 변경 여부

**없음.** 읽기 전용 조사만 수행(`git status` 변동 없음, HEAD 그대로 `10339ea`).
