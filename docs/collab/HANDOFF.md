# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ 일반 이메일 인증·로그인·계정관리 그룹 전체 완료**: AUTH-1~6·PW-1·MAIL-1·SIGNUP-1/2·RATE-1 10개 단위 전부 구현·테스트·커밋·푸시 완료.
  - **회원가입**: MAIL-1(`a79c08c`) → SIGNUP-1(`4c0a534`, 인증코드 요청) → SIGNUP-2(`bc37d8a`, 코드확인+signupToken) → AUTH-4(`bc7d7ce`+`f19830e`, 가입완료·`phone` 포함·비밀번호 8~72자 정책).
  - **로그인**: RATE-1(`2cfcb74`, 로그인 실패 15분5회→15분잠금) → AUTH-5(`20157ea`, 로그인·계정없음/OAuth전용/비번불일치 전부 `INVALID_CREDENTIALS` 동일응답·소프트탈퇴 7일 자동복구).
  - **계정관리**: AUTH-3(`277a71d`, 이메일 중복확인) → AUTH-6(이번 커밋, 비밀번호 변경·성공 시 전체 세션 무효화).
  - AUTH-1(`dba810d`)/AUTH-2(`c5b1b14`)/PW-1(`e167c19`)도 이미 완료.
  - 전체 스위트 436개 중 아래 "발견된 기존 결함" 1건만 실패(회귀 없음, 일곱 단위 연속 확인).
- **⚠️ 발견·수정한 버그(범위 내, 유지 확정)**: AUTH-4 때 `PasswordEncoder` Bean을 `SecurityConfig`→`infra/security/PasswordEncoderConfig.java`로 분리(순환 의존 해결). 사용자 확인 완료.
- **⚠️ 설계 메모**: `User.isRestorable()`(OAuth 로그인도 사용)은 `anonymizedAt==null`만 확인해 7일 경과 여부가 익명화 스케줄러 실행 시점에 좌우된다 — AUTH-5는 이를 피하려 `withdrawalRequestedAt` 날짜를 직접 비교하는 로직을 `login()`에 별도로 추가했다(User/OAuth2SuccessHandler는 안 건드림, 그쪽엔 같은 잠재적 오차가 여전히 남아있음 — 필요시 별도 단위로 통일 검토).
- **✅ 정책에 없던 결정, 사용자 확인 완료**: AUTH-6 "비밀번호 변경 시 다른 기기 세션 처리"를 **전체 세션 무효화**로 확정(`withdraw()`와 동일 패턴). RATE-1의 `ACCOUNT_LOCKED` 노출 여부도 AUTH-5 구현으로 확정 반영됨.
- **❗ 문서 후속작업 미완료**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13)·`docs/FRONTEND_API_GAPS.md`(§1.1)가 AUTH-4(회원가입) 완료까지는 반영했지만, **AUTH-5(로그인)·AUTH-3(중복확인)·AUTH-6(비밀번호변경) 완료는 아직 반영 못 했다** — 두 문서의 "로그인·복구 BLOCKED" 표기가 이제 로그인 부분만큼은 stale하다. 다음 세션에서 갱신 필요(§1.1(b)를 로그인=완료/계정복구(아이디·비번찾기)=미구현으로 다시 쪼개야 함).
- **⚠️ 로컬 테스트 환경 참고**: Redis 의존 테스트를 실제 Redis로 검증 중. Docker 전용 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**)를 띄워뒀다. 테스트 실행 시 `REDIS_PORT=6400 ./gradlew.bat test`로 지정, `build.gradle`엔 커밋 안 함(로컬 전용). Docker Desktop이 꺼져 있으면 `docker start honor-citizen-redis-test`로 재기동.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패한다 — `POST /api/applications`가 `TERMS_NOT_AGREED`를 던지는데, 이 테스트가 약관동의 단계를 안 거치고 로그인만 재현해서 발생. 클린 `main` HEAD에서도 재현되는 기존 결함(회귀 아님), User/Application 도메인 테스트라 이번 작업 범위 밖.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 건드리지 않았다.

## 다음에 할 일

- **문서 갱신(우선)**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`/`docs/FRONTEND_API_GAPS.md`에 AUTH-5/AUTH-3/AUTH-6 완료 반영 — 로그인 BLOCKED 해제, §1.1(b)를 로그인(완료)/계정복구(아이디·비번찾기, 여전히 미구현)로 재분리.
- **계정복구(아이디 찾기·비밀번호 재설정)**: 정책은 확정돼 있음(`FRONTEND_API_REQUIREMENTS.md` §116-121) — 이름/전화로 아이디 찾기, 이메일/전화로 비번 재설정 요청. UX 결정 2건도 이미 남겨둠(회원가입 인증코드 인라인, 비번재설정 코드+새비번 통합화면 → API도 단일요청 `{email,code,newPassword}`로 설계). 다음 백엔드 그룹으로 자연스러움.
- **프론트 실 연동**: 백엔드가 준비된 회원가입/로그인/이메일중복확인/비밀번호변경 API를 `SignupPage.tsx`/`LoginPage.tsx` 등이 아직 mock으로만 처리 중 — 실 API 연동은 이번 세션 범위 밖(프론트 작업).
- **UserApplicationFlowTest 403 수정**: 위 "발견된 기존 결함" 참고 — 담당자 미정, User/Application 도메인 작업자가 처리.
- 그 외 미착수 항목(Inquiry 도메인, 관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고 — 이번 세션과 무관.

## ❓ 확인 필요

- 없음(이번 세션 기준 — 이 그룹의 확인 필요 항목은 전부 사용자 확정으로 해소됨).

## 참고

- MAIL-1/SIGNUP-1/SIGNUP-2/AUTH-4/RATE-1/AUTH-5/AUTH-3/AUTH-6 전부 신규 테스트(집중) 실행 후 전체 스위트(394/400/406/411/416/427/431/436개, 회귀 테스트)를 돌려 확인했다 — 실패 1건(`UserApplicationFlowTest`)은 위에 기록한 대로 기존 결함, 여덟 단위 모두 동일건으로 회귀 아님을 재확인.
- 관련 문서: `docs/collab/TODO.md`(백엔드 API 연동 체크리스트 절), `docs/collab/CHANGELOG.md` 2026-08-19 항목들, `backend/FRONTEND_API_REQUIREMENTS.md` §3(이메일 회원가입 정책 원본), `docs/api/auth.md`(auth API 전체 최신 계약 — AUTH-3/5/6은 아직 이 문서에 반영 안 됨, 필요 시 API 7~9로 추가).
