# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-09-15
- 작성자: Codex
- 브랜치: main
- 커밋·push: 수행하지 않음

## 현재 워킹 트리

사용자가 시작 전에 보유하던 백엔드/프론트/API 연동 미커밋 변경을 그대로 보존한 상태에서 프론트 구조 정리를 추가했다. 특히 다음 선행 변경은 이번 작업에서 되돌리지 않았다.

- 관리자 신청 만세력 결과 일괄 조회 API와 프론트 복원
- 단체 작명 Excel 선택적 성씨 열
- 인증 서버 세션 단일화
- 관리자·마이페이지·공지 페이지 이동
- 행사 PROGRAM 정적화와 `ContentAdminPanel` 삭제

## 이번 작업 완료

- `StepInfo.tsx`: 944줄 → 124줄. 개인·단체·학교·수령인 섹션과 검증·학교 검색 hook으로 분리
- `ApplicationsSection.tsx`: 924줄 → 190줄. 상세·상태, 이름 추천, 만세력, 카드 제작, 카드번호와 복원 hook으로 분리
- `adminNamingMock.ts` → `lib/namingRecommendations.ts`, `MockSaju` → `SajuSnapshot`
- 호출되지 않던 `lib/shuffle.ts`와 단건 `getActiveManseryeokResult` 프론트 래퍼 제거
- UI theme의 직접 색상을 `styles/tokens.css` semantic token으로 이전. 브랜드 SVG와 홈 장식 효과는 예외 유지
- `frontend/README.md`, `docs/FRONTEND_API_GAPS.md`, `docs/FRONTEND_API_INTEGRATION_SPEC.md`를 현재 코드 기준으로 재작성
- 초기 조사·누적 이력 문서에 Historical Reference 또는 현재 상태 우선순위 안내 추가

## 검증

- TypeScript strict + `noUnusedLocals` + `noUnusedParameters`: PASS
- `npm run build`: PASS, 225 modules
- `naming-determinism.spec.ts`: PASS 7/7
- `playwright.ui.config.ts` 격리 Edge 브라우저: PASS 5/5
  - 공개 주요 라우트·공지·마이페이지·언어 전환
  - 개인 신청 검증·수령인·draft 복원
  - 학생증 학교 검색·직접 입력
  - 단체 신청·실물 수령인
  - 관리자 로그인·신청 목록/상세·이름 저장·만세력·카드·페이지 이동
- 구조 정리 전후 API 호출 집합과 호출 횟수 동일. 미사용 단건 래퍼 1개만 제거
- CSS 34개 파일을 토큰 값으로 역치환해 기존 selector/value와 동일함 확인
- 관리자 상세와 모바일 홈 캡처를 육안 확인

Docker 엔진이 실행 중이지 않아 실제 백엔드 통합 E2E는 이번에 재실행하지 않았다. 백엔드 코드는 이번 구조 정리에서 수정하지 않았다.

### 백엔드 단위 테스트 (2026-09-15 재검증)

- `gradlew compileJava compileTestJava`: PASS
- `gradlew test`: Redis(6379, `docker run redis:7-alpine`) 기동 후 **882개 전부 PASS (실패 0, 스킵 5)**. Redis 없이 실행하면 174개가 `RedisConnectionFailureException`/503으로 실패하므로 테스트 전 Redis가 필요하다.
- 학생증 카드 렌더링 테스트 3개(`BulkExcelToCardRenderingEndToEndTest`, `CardBackInterpretationWrapTest`의 학생증 케이스, `SchoolCardTemplateEndToEndTest`)는 별도 saju 리포의 디자이너 원본 PNG(`D:\HC-worktrees\saju\시안\...`)를 절대 경로로 읽는다. 자산이 없는 머신에서 실패하지 않도록 JUnit `Assumptions`로 skip 처리했다(자산이 있는 머신에서는 기존과 동일하게 전부 실행된다).
- 워킹 트리 백엔드 변경과 직접 관련된 스위트는 전부 통과: `ApplicationServiceNamingResultTest`(12), `NamingResultExcelParserTest`(9), `ManseryeokServiceTest`(16), `BirthTimeZoneResolverTest`(12), `BulkExcelParserTest`(22), `ApplicationExportExcelBuilderTest`(4)
- 이 머신 주의사항: 사용자 홈 경로에 한글이 포함되어 Gradle 테스트 워커 JVM이 classpath를 읽지 못한다(`ClassNotFoundException: GradleWorkerMain`). 우회: `GRADLE_USER_HOME=C:\Users\Public\hc-gradle`(기존 캐시 복사본) + `subst X: C:\Users\이솔하\Desktop\HC` 후 `X:` 경로에서 실행.

## 현재 미완료·확인 필요

1. `/api/users/me`가 role을 반환하지 않아 프론트가 로그인 응답 role을 `auth-role` UI 힌트로 유지한다. 서버 인가는 영향 없다.
2. 신청 전 상담확인·유의사항은 UI 게이트이며 신청 건별 이력이 저장되지 않는다.
3. 공개 FAQ·Support FAQ·행사 목록은 `size: 100` 단일 조회다. 100건 초과 운영 정책이 필요할 때 페이지 이동을 추가한다.
4. `docs/collab/TODO.md`의 “Excel에서 성씨를 받지 않는다” 정책과 현재 워킹 트리의 선택적 성씨 열 구현이 충돌한다. 기존 작업을 보존했으며 병합 전 정합화가 필요하다.
5. 이름 조회 API와 정적 마케팅 CMS는 제품 선택 사항이다.

현재 상태의 단일 소스는 `docs/FRONTEND_API_GAPS.md`, 프론트 API 사용법은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`다.
