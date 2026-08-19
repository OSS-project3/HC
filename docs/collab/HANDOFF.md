# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ 학생증 신청에 학교명(schoolName) 필드 추가 완료(2026-08-19, SCHOOLNAME-1)**: 프론트-백엔드 API 갭을 사용자 요청으로 재점검하다가 `docs/BACKEND_API_GAPS.md` P1-4("학교명 저장 필드 없음")를 다시 짚어 정책 확정 후 구현까지 완료.
  - **확정 정책**: `Application` 레벨 단일 필드(개인·단체 공통, `orientation`/`schoolType`과 동일 위치) — 단체 신청은 항상 한 학교 단위로 접수된다는 기존 전제(`schoolType`이 이미 단일값인 이유)를 그대로 이어받음. `UNIVERSITY`/`HIGH_SCHOOL` **둘 다 필수**(학번/학과와 달리 대학교 전용 조건 없음). DB 컬럼은 nullable, 학생증 여부에 따른 필수 검증은 서비스 레벨(`validateStudentFields`/`createGroup`)에서만 강제(비학생증이면 있으면 거절, 기존 orientation/schoolType과 동일 패턴). 트림 후 5~20자, 한글·영문·숫자·공백만 허용.
  - 정책 문서: `docs/specs/application/requirements.md` §5-0(정책)/§5-2(체크리스트, SCHOOLNAME-1 완료 표시)에 기록. `data-model.md`/`api.md`(개인·단체 API 둘 다) 갱신 완료.
  - 신규 테스트 9개(`ApplicationServiceTest` 6개, `ApplicationServiceBulkTest` 3개) 전부 통과, 기존 학생증 픽스처가 있던 3개 파일(`ApplicationServiceTest`/`ApplicationServiceBulkTest`/`ApplicationServiceUploadCompensationTest`) 보정 후 통과. 전체 스위트 471개(462+9) 중 `UserApplicationFlowTest.fullUserApplicationFlow`(아래 "기존 결함" 참고, 무관) 1건만 실패, 회귀 없음.
  - 커밋: `575f6c0`(코드+테스트), `6653fd2`(문서).
- **✅ 프론트-백엔드 갭 문서 오류 정정 완료(2026-08-19)**: `docs/FRONTEND_API_GAPS.md` §1.1(b)/`docs/BACKEND_API_GAPS.md` P0-1이 "`POST /api/auth/login`·`POST /api/auth/email/check`·`PATCH /api/users/me/password`가 여전히 없음"이라고 잘못 적어뒀던 것(이전 세션 AUTH-1~6에서 이미 구현·커밋된 게 갭 재점검 문서에 반영이 안 됐던 것)을 정정 — §1.1(b)를 "로그인·이메일 중복확인·비밀번호 변경(✅ 완료)"과 "(c) 계정복구(❌ 미구현)"로 분리, `BACKEND_API_GAPS.md` P0-1도 완료/미구현 표로 분리하고 이제는 유효하지 않은 "소프트탈퇴 7일 유예 자동복구" 서술도 함께 제거(WITHDRAW 정책으로 폐지됨). 코드 변경 없음, 문서만 수정.
- **✅ 회원탈퇴 정책 변경 확정 및 구현 완료(2026-08-19)**: 사용자가 소프트 삭제(7일 유예+익명화)를 폐지하고 즉시 하드 삭제로 바꾸자고 제안 → 스코프 분석(코드베이스 전체에서 `User.id`를 참조하는 7개 테이블 전수 확인, `arch.md` §5.1의 FK-없는-Long-참조 원칙 덕분에 하드 삭제해도 DB/화면 레벨 문제가 없음을 확인) → 사용자가 최종 정책표로 확정 → **5단위 체크리스트로 구현까지 전부 완료**.
  - **`docs/collab/user.md`가 source of truth다** — 사용자가 별도로 작성해둔 상세 정책 원본("회원정보·개인정보 보유·탈퇴·파기 정책", 19개 절, §19가 구현 체크리스트). 작업 중 미커밋 상태로 발견해 사용자 확인 후 상위 소스로 채택. **다음에 이 주제를 다시 볼 때는 이 파일을 먼저 읽을 것.**
  - **확정 정책 요약**: 탈퇴 즉시 확정(유예기간·자동복구 폐지), `User`/`RefreshTokenSession`/`ApplicationDailyLimit`만 하드 삭제, `Application`(+`Applicant`/`Member`/`Receiver`)·결제이력·`Inquiry`·`Review`·`Board.created_by_user_id`·`AdminActivityLog.admin_id`는 삭제하지 않고 각자 보존정책 유지, 동일 이메일 재가입 가능하나 과거 데이터 자동 승계 안 됨. "회원가입 정보 상품수령후6개월" 문구는 미탈퇴 회원의 기본 보유기간일 뿐, 탈퇴 시엔 지체 없이 파기(§17.1 해소).
  - **구현 5단위(전부 완료)**: WITHDRAW-1(OAuth 자동복구 제거, `a3bc798`) → WITHDRAW-2(로그인 자동복구+`restored` 응답 필드 제거, `b83bd65`) → WITHDRAW-3(익명화 스케줄러+`anonymize()`/`isRestorable()`/`restore()` 제거, `f956120`) → WITHDRAW-3B(`UserStatus`/`status`/`isWithdrawn()` 완전 제거 — 착수 전 전체 참조 검색으로 `ApplicationService.validateAdmin()`·`ReviewEligibilityService.validateForCreate()` 2개 도메인까지 걸치는 걸 확인해 별도 단위로 분리, `861b92e`) → WITHDRAW-4(`UserService.withdraw()`를 실제 하드 삭제로 교체 — `RefreshTokenSession`/`ApplicationDailyLimit`도 함께 삭제, `ApplicationDailyLimitService.deleteAllForUser()`를 신설해 arch.md §5.1 "다른 모듈 Repository 직접 호출 금지" 원칙 준수, `7e131f5`).
  - `ErrorCode.ALREADY_WITHDRAWN`은 완전히 안 쓰이게 돼 삭제. `docs/api/user.md` API 4를 실제 구현 기준으로 최종 정리 완료. `arch.md` §4.1/§4.7/§11도 "구현 완료" 상태로 갱신 완료.
  - 전체 스위트 462개 중 `UserApplicationFlowTest.fullUserApplicationFlow`(아래 "기존 결함" 참고, 무관) 1건만 실패, 회귀 없음. 각 단위 통과 후 마지막 단위(WITHDRAW-4)에서 전체 회귀까지 확인.
  - **남은 미해결(구현과 무관, 법무 확인 대상)**: `docs/collab/user.md` §17.2(비밀번호 제3자 제공 문구 오류 의심), §17.3(제3자 제공/처리위탁 조항 혼재) — 임의 해석 금지, 담당자 확인 필요.
- **✅ Inquiry(1:1 문의) 도메인 신규 구현 완료(2026-08-19)**: 정책 정의부터 구현까지 처음부터 끝까지 진행. 6개 API 전부 구현·테스트·커밋·푸시 완료.
  - `POST /api/inquiries`(`1abab25`) → `GET /api/my/inquiries`·`/{id}`(`0b08b41`) → `GET /api/admin/inquiries`·`/{id}`(`3cb647f`) → `PATCH /api/admin/inquiries/{id}/answer`(`f877d2d`) → `PATCH /api/admin/inquiries/{id}/status`(`a9abff7`).
  - 정책 문서: `docs/specs/inquiry/requirements.md`가 source of truth(①~⑨ 전부 확정).
  - 핵심 설계: `userId`는 JWT에서만 추출, `GET /api/my/inquiries/{id}`는 미존재 404/타인소유 403 분리, `category`는 `InquiryCategory` enum(프론트 수정 불필요), 답변 등록 시 최초 1회만 이메일 발송, `COMPLETED`+`answer=null` 허용.
- **❗ 오픈 아이템 — 프론트 `privacyConsent` 미반영**: `POST /api/inquiries`가 `privacyConsent: true`를 요구하는데(`@AssertTrue`) `InquiryPage.tsx`가 아직 이 필드를 보내지 않는다. 프론트 반영 전까지 실 연동 시 항상 400 — `docs/FRONTEND_API_GAPS.md` §1.3에 기록.
- **⚠️ 절차 관련 — 이 세션에서 사용자 피드백으로 정착된 작업 방식(계속 적용 중)**:
  1. 착수 전 반드시 정책 문서에 체크리스트를 먼저 작성하고 그 순서대로 진행(코드부터 쓰지 않는다).
  2. `RULES.md` §8 — 단위별로는 영향 범위만 `--tests`로 좁혀 실행, 전체 스위트는 기능 묶음 완료·공통 인프라 변경·push 직전에만. 착수 전 전체 참조를 grep으로 검색해서 예상보다 범위가 크면 단위를 분리(WITHDRAW-3B 사례).
- **✅ 일반 이메일 인증·로그인·계정관리 그룹(AUTH-1~6·PW-1·MAIL-1·SIGNUP-1/2·RATE-1)** — 이전 세션에 완료. `POST /api/auth/login`·`/email/check`, `PATCH /api/users/me/password` 전부 구현돼 있음(위 "갭 문서 오류" 항목 참고 — 문서만 안 따라감).
- **⚠️ 로컬 테스트 환경 참고**: Docker 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**). `REDIS_PORT=6400 ./gradlew.bat test`로 실행, `build.gradle`엔 커밋 안 함(로컬 전용). Docker Desktop이 꺼져 있으면 `docker start honor-citizen-redis-test`.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함, 여러 세션째 유지)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패 — 약관동의 단계를 안 거치는 기존 결함(회귀 아님), User/Application 도메인 작업자가 처리할 범위.
- 그 외 도메인(Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태.

## 다음에 할 일

- **프론트 `privacyConsent` 반영 확인**: `InquiryPage.tsx`에 이 필드가 추가됐는지 확인 후 실 연동 테스트.
- **거래·상담 데이터 파기 스케줄러(별도, 회원탈퇴 정책과 무관)**: Inquiry 6개월(`docs/specs/inquiry/requirements.md` §⑧), 결제·거래 이력 법정 보존기간 경과분 — 담당자 미정. `docs/collab/user.md` §15 참고.
- **개인정보처리방침 문안 자체 확인(법무 대상)**: §17.2(비밀번호 제3자 제공 문구), §17.3(제3자 제공/처리위탁 조항 혼재) — 코드 작업 아님.
- **UserApplicationFlowTest 403 수정**: 담당자 미정.
- 그 외 미착수 항목(관리자 신청관리, Payment 도메인, 단체 신청 구성원별 상세/카드 ZIP 다운로드 등)은 `TODO.md` 진행 보드 및 `docs/BACKEND_API_GAPS.md`(P0~P2 우선순위) 참고.

## ❓ 확인 필요

- 없음 — 이번 세션에서 나온 모든 질문(Inquiry 정책, 회원탈퇴 정책, schoolName 정책)은 사용자가 전부 확정해줬다.

## 참고

- 회원탈퇴 정책 스코프 분석 방법(재사용 가능): `Grep`으로 `Long userId`/`createdByUserId`/`adminId` 패턴 검색해 참조 테이블 확인 → 각 도메인이 스냅샷 저장인지 재확인 → 관련 인프라 코드(`JwtAuthFilter`/`TokenSessionStore` 등) 직접 읽어서 실제 동작 확인. 코드 추측 없이 전부 실제 파일 확인 기반으로 진행.
- 프론트-백엔드 갭 재점검 방법(재사용 가능): 갭 문서(`docs/FRONTEND_API_GAPS.md`/`docs/BACKEND_API_GAPS.md`)의 서술을 그대로 믿지 않고 실제 Controller/DTO/Entity를 grep+Read로 대조 — 이번에 그렇게 해서 login/email-check/password-change가 이미 구현돼 있는데 문서만 안 따라간 것과 schoolName이 실제로 없는 것(진짜 갭) 둘 다 정확히 구분해냄.
- Inquiry/회원탈퇴/schoolName 전부 "착수 전 체크리스트 작성 → 단위별 TDD+독립 커밋 → 마지막 단위에서 전체 회귀"로 진행. 착수 전 grep으로 실제 참조 범위를 먼저 확인하고, 예상보다 크면(회원탈퇴 WITHDRAW-3B처럼) 단위를 쪼개는 방식이 정착됨.
- 관련 문서: **`docs/collab/user.md`(회원탈퇴/개인정보 정책 source of truth, §19가 체크리스트)**, `docs/specs/application/requirements.md` §5-0/§5-2(schoolName 정책+체크리스트), `arch.md` §4.1/§4.7/§11, `docs/api/user.md`, `docs/specs/inquiry/requirements.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md` 2026-08-19 항목 전부, `docs/collab/RULES.md` §8.
