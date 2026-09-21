# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-09-21
- 작성자: Claude
- 브랜치: main
- 커밋·push: 아래 "완료" 항목 전부 로컬 커밋 완료. **push는 하지 않음**(`origin/main` 대비 47 commits ahead).

## 현재 워킹 트리

깨끗함(2026-09-21까지 세션 작업 전부 커밋 완료, push만 안 함).

## 이번 세션(2026-09-17~09-21) 완료 — 상세는 `docs/collab/CHANGELOG.md` 해당 날짜 항목 참고

1. `/api/users/me` 응답에 `role` 추가 (2026-09-17)
2. 작명 결과 Excel "뜻" 컬럼 파싱 + 관리자 명단 엑셀 내보내기 뜻 컬럼 추가, `NAME_EDITING` 상태 편집 잠금 (2026-09-17)
3. 단체/개인 신청 사진 **내용** 검증 누락 버그 수정, 학생증 앞·뒤 텍스트 색상 기능 **백엔드 전체 구현** (2026-09-19)
4. QA 체크리스트(상태 전이·중간 저장 검증) 14건 전부 완료 — 실제 버그 6건 수정(Review 이미지 UNIQUE 위반, 카드발급/배송 감사로그 중복, 학교템플릿·카드생성 커밋경계 오삭제 2건, CardType/CardDesign 시더 부분복구), 확정정책 충돌 구현공백 2건(단체 ZIP 업로드 한도, Excel 헤더 계약), 나머지 6건은 버그 없이 테스트만 보강. 상세: `docs/collab/qa_state_persistence_checklist.md` (2026-09-20)
5. 운영용 임시 관리자 자동 시드(`DemoDataSeeder.ensureAdminUser()`, `admin@test.com`) 제거 — 확정 정책: 운영 관리자는 일반 가입 후 운영자가 DB에서 `role` 수동 변경, 승격 API/UI는 미구현. 상세: `docs/TEMP_ADMIN_LOGIN.md` (2026-09-20)
6. 학생증 텍스트 색상 프론트엔드 갭 문서화(`FRONTEND_API_GAPS.md` P2 신규, `FRONTEND_API_INTEGRATION_SPEC.md` 계약 기록) + 실제 UI 구현(카드 제작 패널에 STUDENT 전용 앞/뒤 글씨색 select, 확정 후 잠금) (2026-09-20)
7. 십이간지 디자인 세트 선택 UI 구현 — 기존 P0 하드 블로커(모든 카드종류의 카드 생성을 막던 미착수 갭)를 신청 상세 레벨(select 1~5, 텍스트만·미리보기 이미지 없음)에 추가해 해소. `FRONTEND_API_GAPS.md`/`docs/collab/TODO.md`의 완료 반영 누락(6번 항목의 학생증 색상 포함)도 같이 정리 (2026-09-20)
8. 결제 안내·72시간 미입금 자동취소 정책 폐기를 문서에 반영(코드는 그대로) — `guidePayment()` Service는 있는데 이걸 호출하는 Controller가 없다는 지적에서 시작, 확인해보니 이미 폐기하기로 한 정책인데 requirements.md/data-model.md/api.md(2곳)/admin.md/TODO.md §5-A 전부 여전히 "구현 예정"으로 남아있었음. 확정 정책: 결제 안내는 시스템 밖에서 처리, 관리자는 confirm-payment만 호출, 자동 취소 없음. `guidePayment()`/`paymentGuidedAt`/`paymentDueAt`/`ApplicationPaymentTimeoutScheduler`는 2026-09-13 `refundedAt` 처리와 동일하게 코드는 그대로 두고 문서만 정정(사용자 확인 완료) (2026-09-20)
9. 카드 제작 설정 복원 — 백엔드가 이미 내려주던 확정 `cardDesignId`/`cardIssueDate`를 프론트 타입이 무시해 화면 재진입 시 항상 기본 디자인·오늘 날짜로 리셋되던 문제 수정. 학생증 텍스트 색상과 동일한 confirmed-value 잠금·복원 패턴 적용(정책 질문 없이 바로 구현, 사전 갭 문서 등록도 없었음). 커밋 `b611676` (2026-09-20)
10. 신청 건별 동의 이력 — **완료(백엔드+프론트)**. 1단계로 백엔드 저장 계약만 먼저 추가(사용자 요청: "백엔드 먼저") — `Application.consultationConfirmed`/`disclaimerConfirmed`/`consentPolicyVersion`, 개인·단체 생성 DTO에 같은 필드(커밋 `6cbeafb`, 부수적으로 Jackson이 `ApplicationCreateRequest`의 미사용 `@AllArgsConstructor`를 암묵적 creator로 채택해 새 boolean 필드 때문에 기존 테스트 4건이 깨지는 걸 발견해 제거). 2단계로 `ApplyPage.tsx`가 체크박스 값을 실제로 전송하도록 연결하고(커밋 `b86c5a7`), 백엔드에 `@AssertTrue` 검증을 활성화해 둘 다 true가 아니면 거절하도록 마무리(커밋 `bfa776e` — 성공 경로를 검증하던 기존 테스트 7건이 동의 필드 누락으로 새로 실패해 픽스처에 추가). `FRONTEND_API_GAPS.md` P1에서 제거 (2026-09-20~21)
11. 공개 카드 조회 → 다운로드 — 단기 토큰 기반 재설계 완료(백엔드+프론트). 비로그인 조회 화면이 로그인 전용 다운로드 API를 호출해 항상 401이었고, 프론트가 그 실패를 데모 카드로 조용히 대체해 실제로 작동한 적이 없던 기능이었음(외부 감사표에서 P0로 지적). 단순 `permitAll()`은 `applicationId`만 바꿔 타인의 카드를 내려받는 IDOR을 만들어 대신 `CardLookupTokenService`(신규, Redis 기반 1회용 단기 토큰, `VerificationChallengeStore`의 signupToken 패턴 재사용·새 시크릿 없음)로 조회 성공 사실 자체를 인가 근거로 삼는 신규 공개 엔드포인트(`GET .../cards/download/public`)를 추가했다. 기존 로그인 기반 엔드포인트는 그대로 두고 공유 로직만 추출. 프론트는 데모 카드 fallback을 제거하고 실제 실패 메시지로 교체(사용자 확인 완료). 커밋 `4ea6801`(백엔드) / `9b8da0e`(프론트) / `688c3fa`(체크리스트) (2026-09-20)
12. **실제 dev 컨테이너(기존 데이터가 쌓인 populated DB) 재빌드 검증 중 배포 위험 버그 발견·수정**. 사용자가 "실제 브라우저 클릭 테스트 해보면 안 됨?"이라고 물어 dev 스택을 오늘 코드로 재빌드했더니, 이미 행이 있는 `applications` 테이블에 DEFAULT 없는 `NOT NULL` boolean 컬럼(`consultation_confirmed`/`disclaimer_confirmed`)을 추가하려다 Postgres가 거부했고, Hibernate `ddl-auto=update`는 이 실패를 **조용히 무시**하고 앱을 계속 띄웠다 — 컬럼 자체가 안 생겨서 이후 모든 Application 읽기/쓰기가 "column does not exist"로 깨지는 상태였다(순수 유닛 테스트는 항상 빈 스키마에서 시작하므로 이 클래스의 버그를 원천적으로 못 잡는다). `@ColumnDefault("false")` 추가로 수정하고 같은 populated DB에 재적용해 컬럼이 정상 생성됨을 확인(커밋 `c68cc2f`). 이어서 실제 chromium-cli/Playwright는 이 환경에 없어 브라우저 클릭 자체는 못 했지만, 살아있는 컨테이너에 `Invoke-RestMethod`로 프론트와 동일한 실제 요청을 직접 보내 조회→토큰발급→다운로드(CARD_NOT_READY)→토큰 재사용 거절(INVALID_LOOKUP_TOKEN)까지 전 구간을 실제 스택으로 확인 (2026-09-21)
13. **소셜 로그인 시 관리자 메뉴가 안 뜨는 버그 발견·수정**. 사용자가 로컬 dev DB에 만든 관리자 계정, 이어서 운영 DB에서 본인 계정을 `role='ADMIN'`으로 승격 후 로그인했는데도 관리자 메뉴가 안 뜬다고 지적. 원인: `AuthContext.refreshProfile()`이 백엔드가 2026-09-17부터 내려주던 `/api/users/me`의 실제 `role`을 무시하고, 로그인 시점에만 기록되는 localStorage 힌트(`auth-role`)로 role을 복원해왔음 — 비밀번호 로그인은 로그인 성공 시 명시적으로 `login()`을 호출해 문제가 안 드러났지만, **소셜 로그인(OAuth)은 백엔드가 쿠키만 심고 바로 리다이렉트할 뿐 프론트 어디서도 `login()`을 안 불러서** `refreshProfile()`의 힌트 폴백만 타고, 이 계정으로 첫 로그인이면 힌트가 없어 관리자도 항상 "user"로 떨어짐. `FRONTEND_API_GAPS.md` P1 "`/api/users/me` 역할 복원" 갭이 실제로 이 버그였음(문서엔 "백엔드에 role이 없다"고 적혀 있었는데 이미 2026-09-17에 추가돼 있었고, 프론트가 안 쓰고 있었을 뿐). `refreshProfile()`이 `profile.role`을 그대로 쓰도록 고치고 이제 불필요한 힌트 메커니즘(`ROLE_HINT_KEY`/`readRoleHint`/`writeRoleHint`) 제거. 커밋 `22a8a30` (2026-09-21)

## 검증

- 백엔드: 항목별 개별 회귀 + 세션 마지막 전체 회귀 실행, 마지막 실행은 976개 전부 통과(무관 플레이키 `HighSchoolSeederIntegrationTest`는 세션 중 간헐적으로 실패했었지만 이번 마지막 실행에는 안 걸림) — 스위트 전체가 공유하는 `schools` 테이블을 여러 테스트가 각자 `deleteAll()`해서 생기는 기존 문제, 이 세션이 유발한 게 아님(재현·원인 확인은 `qa_state_persistence_checklist.md` 10번 항목 참고).
- 테스트 실행 시 `hc-test-redis` 컨테이너(포트 6379) 필요 — 개발 `docker-compose.yml`은 Redis를 호스트에 노출하지 않는다. `docker run -d --name hc-test-redis -p 6379:6379 redis:7-alpine`(최초) 또는 `docker start hc-test-redis`(이후). **주의**: 이 컨테이너는 docker-compose 관리 밖이라 Docker Desktop 재시작 시 자동으로 안 살아난다 — 재시작 직후 테스트가 전부 `RedisConnectionException`으로 실패하면 이것부터 의심할 것(이번 세션에 실제로 겪음, 코드 문제 아니었음).
- 프론트: `tsc --noEmit` strict, `npm run build` 둘 다 통과(학생증 색상·십이간지 선택 UI·공개 카드 다운로드 포함).
- **실제 dev 컨테이너 검증**: 2026-09-21에 처음으로 `docker compose up -d --build`로 이 세션의 실제 코드를 기존 populated DB에 반영해 검증(위 12번 참고) — 이 과정에서 순수 유닛 테스트로는 못 잡는 배포급 버그(populated 테이블에 DEFAULT 없는 NOT NULL 컬럼 추가 실패)를 발견·수정했다. 실제 브라우저 클릭 자동화(chromium-cli/Playwright)는 이 환경에 없어 여전히 못 하지만, 살아있는 컨테이너에 직접 HTTP 요청을 보내는 방식으로 공개 조회→다운로드 토큰 흐름 전 구간을 확인했다. **다음에 이런 요청이 오면**: `docker compose up -d --build`로 재빌드 후 `Invoke-RestMethod`(PowerShell)로 실제 엔드포인트를 직접 호출하는 방식이 이 환경에서 유효했다 — Bash 도구의 셸이 이번 세션 중간에 일시적으로 깨진 적이 있었는데(`git`/`head`/`tail` 등 기본 명령이 안 잡힘) PowerShell 도구는 계속 정상이었다.

## 현재 미완료·확인 필요

1. `FRONTEND_API_GAPS.md`의 남은 갭 — 직접입력 학생증 School 연결 UI(P1). `zodiacDesignSet`은 이번 세션에서 완료(위 7번).
2. `LoginPage.tsx`의 "로그인 실패 시 클라이언트 admin mock 폴백" 제거 — 백엔드 담당 범위 밖, 프론트 수정이라 별도 확인 필요.
3. `docs/api/admin.md` "8. 환불 완료 기록"과 `docs/specs/application/requirements.md`의 `refundedAt` 서술이 2026-09-13 확정 정책(시스템은 환불 완료 여부를 관리하지 않음)보다 낡음 — 코드는 문제 없어 이번엔 안 건드림, 문서 정리만 남음.
4. (기존, 미해결 이월) `docs/collab/TODO.md` 상단의 "단체 Excel에서 성씨를 받지 않는다" 정책과 현재 구현(선택적 성씨 열)이 충돌 — 정책 문서와 구현 중 하나를 정합화해야 함.
5. **작업 완료 직후 `FRONTEND_API_GAPS.md`/`docs/collab/TODO.md` 갱신을 매번 습관화할 것** — 이번 세션 중 학생증 색상 프론트 구현(커밋 `4ae03ec`)이 `FRONTEND_API_GAPS.md`에 반영 안 된 채로 남아있던 걸 십이간지 작업 도중에야 발견해 뒤늦게 정리함(커밋 `a68f713`).

현재 상태의 단일 소스는 `docs/FRONTEND_API_GAPS.md`(프론트 갭), `docs/FRONTEND_API_INTEGRATION_SPEC.md`(프론트 연동 방식), `docs/collab/CHANGELOG.md`(전체 변경 이력), `docs/collab/qa_state_persistence_checklist.md`(이번 세션 QA 상세)다.
