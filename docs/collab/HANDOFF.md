# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-09-20
- 작성자: Claude
- 브랜치: main
- 커밋·push: 아래 "완료" 항목은 전부 로컬 커밋 완료. **push는 하지 않음**(`origin/main` 대비 25 commits ahead). 프론트엔드 UI 구현(아래 참고)은 **아직 커밋도 안 함** — 사용자 확인 대기 중.

## 현재 워킹 트리

- 커밋 안 된 변경: `frontend/src/services/api.ts`, `frontend/src/features/i18n/serverErrors.ts`, `frontend/src/components/admin/applications/{CardProductionPanel,NamingCard,ApplicationDetail}.tsx` — 학생증 텍스트 색상 선택 UI. `tsc --noEmit`/`npm run build` 통과 확인, 실제 브라우저 클릭 테스트는 못 함(개발 DB에 STUDENT 타입 데모 신청 없음). 사용자에게 커밋 여부 확인 중.
- 그 외 워킹 트리는 깨끗함(2026-09-20 세션 작업 전부 커밋 완료).

## 이번 세션(2026-09-17~09-20) 완료 — 상세는 `docs/collab/CHANGELOG.md` 해당 날짜 항목 참고

1. `/api/users/me` 응답에 `role` 추가 (2026-09-17)
2. 작명 결과 Excel "뜻" 컬럼 파싱 + 관리자 명단 엑셀 내보내기 뜻 컬럼 추가, `NAME_EDITING` 상태 편집 잠금 (2026-09-17)
3. 단체/개인 신청 사진 **내용** 검증 누락 버그 수정, 학생증 앞·뒤 텍스트 색상 기능 **백엔드 전체 구현** (2026-09-19)
4. QA 체크리스트(상태 전이·중간 저장 검증) 14건 전부 완료 — 실제 버그 6건 수정(Review 이미지 UNIQUE 위반, 카드발급/배송 감사로그 중복, 학교템플릿·카드생성 커밋경계 오삭제 2건, CardType/CardDesign 시더 부분복구), 확정정책 충돌 구현공백 2건(단체 ZIP 업로드 한도, Excel 헤더 계약), 나머지 6건은 버그 없이 테스트만 보강. 상세: `docs/collab/qa_state_persistence_checklist.md` (2026-09-20)
5. 운영용 임시 관리자 자동 시드(`DemoDataSeeder.ensureAdminUser()`, `admin@test.com`) 제거 — 확정 정책: 운영 관리자는 일반 가입 후 운영자가 DB에서 `role` 수동 변경, 승격 API/UI는 미구현. 상세: `docs/TEMP_ADMIN_LOGIN.md` (2026-09-20)
6. 학생증 텍스트 색상 프론트엔드 갭 문서화(`FRONTEND_API_GAPS.md` P2 신규, `FRONTEND_API_INTEGRATION_SPEC.md` 계약 기록) + 실제 UI 구현(위 "현재 워킹 트리" 참고, 미커밋) (2026-09-20)

## 검증

- 백엔드: 항목별 개별 회귀 + 세션 마지막 전체 회귀 실행, 실패 0건(무관 플레이키 `HighSchoolSeederIntegrationTest` 1건만 간헐 — 스위트 전체가 공유하는 `schools` 테이블을 여러 테스트가 각자 `deleteAll()`해서 생기는 기존 문제, 이 세션이 유발한 게 아님. 재현·원인 확인은 `qa_state_persistence_checklist.md` 10번 항목 참고).
- 테스트 실행 시 `hc-test-redis` 컨테이너(포트 6379) 필요 — 개발 `docker-compose.yml`은 Redis를 호스트에 노출하지 않는다. `docker run -d --name hc-test-redis -p 6379:6379 redis:7-alpine`(최초) 또는 `docker start hc-test-redis`(이후).
- 프론트: `tsc --noEmit` strict, `npm run build` 둘 다 통과(위 미커밋 변경 포함).

## 현재 미완료·확인 필요

1. **프론트엔드 UI 구현(위 "현재 워킹 트리")을 커밋할지** — 사용자 확인 대기.
2. `FRONTEND_API_GAPS.md`의 기존 P0 갭 — `zodiacDesignSet`(십이간지 디자인 세트) 선택 UI, 직접입력 학생증 School 연결 UI. 이번 세션에서 안 건드림, 여전히 미착수.
3. `LoginPage.tsx`의 "로그인 실패 시 클라이언트 admin mock 폴백" 제거 — 백엔드 담당 범위 밖, 프론트 수정이라 별도 확인 필요.
4. `docs/api/admin.md` "8. 환불 완료 기록"과 `docs/specs/application/requirements.md`의 `refundedAt` 서술이 2026-09-13 확정 정책(시스템은 환불 완료 여부를 관리하지 않음)보다 낡음 — 코드는 문제 없어 이번엔 안 건드림, 문서 정리만 남음.
5. (기존, 미해결 이월) `docs/collab/TODO.md` 상단의 "단체 Excel에서 성씨를 받지 않는다" 정책과 현재 구현(선택적 성씨 열)이 충돌 — 정책 문서와 구현 중 하나를 정합화해야 함.
6. (기존, 미해결 이월) 신청 전 상담확인·유의사항 체크는 UI 게이트일 뿐 신청 건별 동의 이력이 저장되지 않음(`FRONTEND_API_GAPS.md` P1).

현재 상태의 단일 소스는 `docs/FRONTEND_API_GAPS.md`(프론트 갭), `docs/FRONTEND_API_INTEGRATION_SPEC.md`(프론트 연동 방식), `docs/collab/CHANGELOG.md`(전체 변경 이력), `docs/collab/qa_state_persistence_checklist.md`(이번 세션 QA 상세)다.
