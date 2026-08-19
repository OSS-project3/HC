# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ 회원탈퇴 정책 변경 확정(2026-08-19) — 문서만, 코드 미착수**: 사용자가 소프트 삭제(7일 유예+익명화)를 폐지하고 즉시 하드 삭제로 바꾸자고 제안 → 먼저 스코프 분석(코드베이스 전체에서 `User.id`를 참조하는 7개 테이블 전수 확인, `arch.md` §5.1의 FK-없는-Long-참조 원칙 덕분에 하드 삭제해도 DB/화면 레벨 문제가 없음을 확인) → 사용자가 최종 정책표로 확정.
  - **확정 정책 요약**: 탈퇴 즉시 확정(유예기간·자동복구 폐지), `User`/`RefreshTokenSession`/`ApplicationDailyLimit`만 하드 삭제, `Application`(+`Applicant`/`Member`/`Receiver`)·결제이력·`Inquiry`·`Review`·`Board.created_by_user_id`·`AdminActivityLog.admin_id`는 삭제하지 않고 각자 보존정책 유지, 동일 이메일 재가입 가능하나 과거 데이터 자동 승계 안 됨, 익명화 스케줄러 제거(거래·상담 파기는 별도 후속 스케줄러).
  - **`docs/collab/user.md`가 source of truth다** — 사용자가 별도로 작성해둔 상세 정책 원본("회원정보·개인정보 보유·탈퇴·파기 정책", 18개 절). 작업 중 미커밋 상태로 발견해 사용자에게 처리 방식을 확인했고("이 문서를 상위 소스로 삼아 arch.md 등을 재조정"), 그 기준으로 아래 문서들을 재보강했다. **다음에 이 주제를 다시 볼 때는 이 파일을 먼저 읽을 것** — `arch.md` 등의 요약보다 상세하다(상품 수령일 기산점, 법정대리인 조항, 파기 감사로그 형식, 개인정보처리방침 문안 자체의 미해결 사항 §17 등).
  - **문서 반영 완료**: `arch.md` §4.1(탈퇴 정책 표)·§4.7(Review 유지 규칙)·§11(스케줄러 인벤토리), `backend/FRONTEND_API_REQUIREMENTS.md` §3, `docs/api/user.md`(API 4 + 로직 변경 절, 전부 "폐지" 표시), `docs/specs/inquiry/requirements.md` §⑥(근거 정정), `docs/specs/review/data-model.md`(user_id 컬럼), `docs/collab/TODO.md`(신규 대기 항목).
  - **❗ 코드는 아직 하나도 안 건드렸다.** 다음 세션에서 실제 구현 필요 — 상세 체크리스트는 아래 "다음에 할 일" 참고.
  - **⚠️ 미해결(법무 확인 필요, 임의 해석 금지)**: 개인정보처리방침 문안의 "회원가입 정보 보유기간: 상품 수령 후 6개월" 문구가 상품을 신청/수령한 적 없는 회원에게는 기준일이 없다는 문제 — 이번 즉시 하드 삭제 정책과 어떻게 정리할지 미확정(`docs/collab/user.md` §17.1). 비밀번호 제3자 제공 문구 오류 의심(§17.2), 제3자 제공/처리위탁 조항 혼재(§17.3)도 별도 확인 필요.
- **✅ Inquiry(1:1 문의) 도메인 신규 구현 완료(2026-08-19, 회원탈퇴 정책 작업보다 먼저 진행)**: 정책 정의부터 구현까지 이번 세션에서 처음부터 끝까지 진행. 6개 API 전부 구현·테스트·커밋·푸시 완료.
  - `POST /api/inquiries`(`1abab25`) → `GET /api/my/inquiries`·`/{id}`(`0b08b41`) → `GET /api/admin/inquiries`·`/{id}`(`3cb647f`) → `PATCH /api/admin/inquiries/{id}/answer`(`f877d2d`) → `PATCH /api/admin/inquiries/{id}/status`(`a9abff7`).
  - 정책 문서: `docs/specs/inquiry/requirements.md`가 source of truth(①~⑨ 전부 확정, §⑨ 체크리스트 전 항목 완료 표시). §⑥ 탈퇴 관련 근거는 위 회원탈퇴 정책 변경에 맞춰 오늘 한 번 더 정정했다(결론은 그대로, 근거만 수정).
  - 핵심 설계: `userId`는 JWT에서만 추출, `GET /api/my/inquiries/{id}`는 미존재 404/타인소유 403 분리(`MyApplicationController` 선례), `category`는 `InquiryCategory` enum(`@JsonValue`/`@JsonCreator`, 프론트 수정 불필요), 답변 등록 시 최초 1회만 이메일 발송(수정 시 재발송 안 함, best-effort), `COMPLETED`+`answer=null` 허용.
  - 전체 스위트 472개 중 `UserApplicationFlowTest.fullUserApplicationFlow`(아래 "기존 결함" 참고, 무관) 1건만 실패, 회귀 없음.
- **❗ 오픈 아이템 — 프론트 `privacyConsent` 미반영**: `POST /api/inquiries`가 `privacyConsent: true`를 요구하는데(`@AssertTrue`) `InquiryPage.tsx`가 아직 이 필드를 보내지 않는다(체크박스가 제출 버튼 비활성화에만 쓰임). 프론트 반영 전까지 실 연동 시 항상 400 — `docs/FRONTEND_API_GAPS.md` §1.3에 기록.
- **⚠️ 절차 관련 — 이번 세션에서 사용자 피드백으로 두 가지 교정함**:
  1. 체크리스트 없이 코드부터 작성하기 시작했다가 사용자 지적으로 중단 → `requirements.md` §⑨에 체크리스트를 먼저 작성하고 그 순서대로 진행.
  2. 단위마다 전체 테스트 스위트를 반복 실행하던 것을 비효율적이라는 피드백을 받아 `RULES.md` §8에 새 규칙 추가 — 단위별로는 영향 범위만 `--tests`로 좁혀 실행, 전체 스위트는 기능 묶음 완료·공통 인프라 변경·push 직전에만.
- **✅ 일반 이메일 인증·로그인·계정관리 그룹(AUTH-1~6·PW-1·MAIL-1·SIGNUP-1/2·RATE-1) — 이전 세션에 완료**: 구현 자체는 완료 상태였으나, 이번 세션의 회원탈퇴 정책 변경으로 `withdraw()`/`OAuth2SuccessHandler`/`UserWithdrawalScheduler` 부분은 재구현이 필요해졌다(위 참고).
- **❗ 문서 후속작업 여전히 미완료(AUTH 그룹, 이번 세션에서도 안 건드림)**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13)·`docs/FRONTEND_API_GAPS.md`(§1.1)가 AUTH-5(로그인)·AUTH-3(중복확인)·AUTH-6(비밀번호변경) 완료를 아직 반영 못 한 상태 — 여러 세션째 이월.
- **⚠️ 로컬 테스트 환경 참고**: Docker 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**). `REDIS_PORT=6400 ./gradlew.bat test`로 실행, `build.gradle`엔 커밋 안 함(로컬 전용). Docker Desktop이 꺼져 있으면 `docker start honor-citizen-redis-test`.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함, 여러 세션째 유지)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패 — 약관동의 단계를 안 거치는 기존 결함(회귀 아님), User/Application 도메인 작업자가 처리할 범위.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 코드는 건드리지 않았다(Review는 문서만 갱신).

## 다음에 할 일

- **회원탈퇴 하드 삭제 구현(최우선, 신규)**: `arch.md` §4.1 "탈퇴 정책" 표대로 —
  1. `User.anonymize()`/`isRestorable()`/`restore()` 제거
  2. `OAuth2SuccessHandler`의 유예기간 자동복구 분기 제거
  3. `UserWithdrawalScheduler`/`UserService.anonymizeExpiredWithdrawnUsers()` 제거
  4. `UserService.withdraw()`를 `User`+`RefreshTokenSession`+`ApplicationDailyLimit` 하드 삭제로 교체
  5. 관련 테스트 7개 파일 재작성(`UserServiceTest`/`UserServiceLoginTest`/`UserTest`/`UserControllerTest`/`ReviewEligibilityServiceTest`/`ApplicationServiceTest`/`ApplicationServiceBulkTest` — grep 결과 기준, 실제 재작성 필요 범위는 착수 시 재확인)
  6. `ErrorCode`의 "탈퇴유예기간경과" 관련 주석·`INVALID_CREDENTIALS` 케이스 정리
  체크리스트 방식(TDD, 단위별 커밋)으로 진행 권장 — Inquiry 도메인에서 쓴 절차와 동일하게.
- **프론트 `privacyConsent` 반영 확인**: `InquiryPage.tsx`에 이 필드가 추가됐는지 확인 후 실 연동 테스트.
- **거래·상담 데이터 파기 스케줄러(별도, 회원탈퇴 정책과 무관)**: Inquiry 6개월(§⑧), 결제·거래 이력 법정 보존기간 경과분 — 담당자 미정.
- **AUTH 그룹 문서 갱신(이전부터 이월)**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`/`docs/FRONTEND_API_GAPS.md`에 AUTH-5/AUTH-3/AUTH-6 완료 반영.
- **UserApplicationFlowTest 403 수정**: 담당자 미정.
- 그 외 미착수 항목(관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고.

## ❓ 확인 필요

- 없음 — 회원탈퇴 정책 3개 질문(관리기간-유예기간 관계/삭제 범위/AdminActivityLog)은 사용자가 전부 확정해줬다.

## 참고

- 회원탈퇴 정책 스코프 분석 방법: `Grep`으로 `Long userId`/`createdByUserId`/`adminId` 패턴 검색해 7개 테이블 확인 → 각 도메인이 스냅샷 저장인지 재확인(`Applicant`/`authorDisplayName`/`Inquiry` 요청바디 저장) → `JwtAuthFilter`/`TokenSessionStore`/`RefreshTokenSession` 등 인증 인프라 코드 직접 읽어서 실제 동작 확인. 코드 추측 없이 전부 실제 파일 확인 기반.
- Inquiry는 5개 단위 신규 테스트(집중 범위) 통과 확인 후 마지막 단위에서 전체 스위트(472개) 회귀 테스트로 마무리.
- 관련 문서: **`docs/collab/user.md`(회원탈퇴/개인정보 정책 source of truth)**, `arch.md` §4.1/§4.7/§11, `backend/FRONTEND_API_REQUIREMENTS.md` §3, `docs/api/user.md`, `docs/specs/inquiry/requirements.md`, `docs/specs/review/data-model.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md` 2026-08-19 항목 전부, `docs/collab/RULES.md` §8.
