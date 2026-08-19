# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-19
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ Inquiry(1:1 문의) 도메인 신규 구현 완료(2026-08-19)**: 정책 정의부터 구현까지 이번 세션에서 처음부터 끝까지 진행. 6개 API 전부 구현·테스트·커밋·푸시 완료.
  - `POST /api/inquiries`(`1abab25`) → `GET /api/my/inquiries`·`/{id}`(`0b08b41`) → `GET /api/admin/inquiries`·`/{id}`(`3cb647f`) → `PATCH /api/admin/inquiries/{id}/answer`(`f877d2d`) → `PATCH /api/admin/inquiries/{id}/status`(`a9abff7`).
  - 정책 문서: `docs/specs/inquiry/requirements.md`가 이 도메인의 source of truth(①~⑨ 전부 확정, §⑨ 체크리스트 전 항목 완료 표시함).
  - 핵심 설계: `userId`는 JWT에서만 추출(요청 바디·쿼리 파라미터로 안 받음), `GET /api/my/inquiries/{id}`는 미존재 404/타인소유 403 분리(`MyApplicationController` 선례와 일관성 맞춤 — 이전엔 "404로 통일"이라고 잘못 적었다가 구현 직전 코드 재확인으로 정정), `category`는 `InquiryCategory` enum(`@JsonValue`/`@JsonCreator`로 프론트의 한글 문자열을 그대로 매핑, 프론트 수정 불필요), 답변 등록 시 최초 1회만 이메일 발송(수정 시 재발송 안 함, 커밋 이후 best-effort — 발송 실패해도 답변 저장은 유지), `COMPLETED`+`answer=null` 조합 허용(전화상담 대응).
  - 전체 스위트 472개 중 `UserApplicationFlowTest.fullUserApplicationFlow`(아래 "기존 결함" 참고, 무관) 1건만 실패, 회귀 없음.
- **❗ 오픈 아이템 — 프론트 `privacyConsent` 미반영**: 개인정보 동의를 서버에서도 검증하기로 확정해 `POST /api/inquiries`가 `privacyConsent: true`를 요구하는데(`@AssertTrue`), `InquiryPage.tsx`는 이 필드를 아직 요청 바디에 담지 않는다(체크박스 상태가 제출 버튼 비활성화에만 쓰임). **프론트가 반영하기 전까지 실제 연동 시 문의 등록이 항상 400으로 거절된다** — `docs/FRONTEND_API_GAPS.md` §1.3에 기록해뒀다(프론트 담당자 영역, 백엔드에서 코드로 고치지 않음).
- **⚠️ 범위 밖으로 명시적으로 미룬 것**: §⑧ 개인정보 6개월 파기 배치(정책만 확정, 구현은 `docs/api/user.md`의 "완전탈퇴 배치 스케줄러"와 묶어 나중에), `docs/specs/inquiry/api.md`/`data-model.md` 분리(Board/Review 관례, 선택적 후속 정리).
- **⚠️ 절차 관련 — 이번 세션에서 사용자 피드백으로 두 가지 교정함**:
  1. 체크리스트 없이 코드부터 작성하기 시작했다가 사용자 지적으로 중단 → `requirements.md` §⑨에 5단위 체크리스트를 먼저 작성하고 그 순서대로 진행.
  2. 단위마다 전체 테스트 스위트(450개 이상)를 반복 실행하던 것을 비효율적이라는 피드백을 받아 `RULES.md` §8에 새 규칙 추가 — 단위별로는 영향 범위만 `--tests`로 좁혀 실행하고, 전체 스위트는 기능 묶음 완료·공통 인프라 변경·push 직전에만 실행.
- **✅ 일반 이메일 인증·로그인·계정관리 그룹(AUTH-1~6·PW-1·MAIL-1·SIGNUP-1/2·RATE-1) — 이전 세션에 완료, 이번 세션엔 무관**: 전부 구현·테스트·커밋·푸시 완료 상태 그대로 유지. 상세는 `CHANGELOG.md`의 해당 날짜 항목 참고.
- **❗ 문서 후속작업 여전히 미완료(AUTH 그룹, 이번 세션에서도 안 건드림)**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`(§3.13)·`docs/FRONTEND_API_GAPS.md`(§1.1)가 AUTH-5(로그인)·AUTH-3(중복확인)·AUTH-6(비밀번호변경) 완료를 아직 반영 못 한 상태— 이전 HANDOFF부터 이어진 미해결 항목.
- **⚠️ 로컬 테스트 환경 참고**: Docker 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**). `REDIS_PORT=6400 ./gradlew.bat test`로 실행, `build.gradle`엔 커밋 안 함(로컬 전용). Docker Desktop이 꺼져 있으면 `docker start honor-citizen-redis-test`.
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함, 여러 세션째 유지)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403(기대 201)으로 실패 — `POST /api/applications`가 `TERMS_NOT_AGREED`를 던지는데 이 테스트가 약관동의 단계를 안 거침. 클린 `main` HEAD에서도 재현되는 기존 결함(회귀 아님), User/Application 도메인 테스트라 범위 밖.
- 그 외 도메인(Application/Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태이며, 이번 세션에서 건드리지 않았다.

## 다음에 할 일

- **프론트 `privacyConsent` 반영 확인**: 프론트 담당자가 `InquiryPage.tsx`에 이 필드를 추가했는지 확인 후 실 연동 테스트.
- **Inquiry 6개월 파기 배치**: `docs/api/user.md`의 "완전탈퇴 배치 스케줄러"와 함께 인프라 작업으로 묶어 착수(담당자 미정).
- **Inquiry 문서 분리(선택)**: `docs/specs/inquiry/requirements.md`를 Board/Review 관례대로 `api.md`/`data-model.md`로 분리할지 판단.
- **AUTH 그룹 문서 갱신(이전부터 이월)**: `docs/FRONTEND_API_INTEGRATION_SPEC.md`/`docs/FRONTEND_API_GAPS.md`에 AUTH-5/AUTH-3/AUTH-6 완료 반영.
- **UserApplicationFlowTest 403 수정**: 담당자 미정, User/Application 도메인 작업자가 처리.
- 그 외 미착수 항목(관리자 신청관리, Payment 도메인 등)은 `TODO.md` 진행 보드 참고.

## ❓ 확인 필요

- 없음 — 이번 세션에서 나온 질문(정책 4~6건, 절차 2건)은 전부 사용자 확인으로 해소됨.

## 참고

- Inquiry 5개 단위 전부 신규 테스트(집중 범위) 실행 후 통과 확인, 마지막 단위(INQUIRY-5, 기능 묶음 완료 시점)에서 전체 스위트(472개) 회귀 테스트로 마무리. 실패 1건은 위에 기록한 기존 결함, 무관.
- 관련 문서: `docs/specs/inquiry/requirements.md`(source of truth, §⑨ 체크리스트), `docs/FRONTEND_API_GAPS.md` §1.3, `docs/collab/TODO.md` Inquiry 행, `docs/collab/CHANGELOG.md` 2026-08-19 Inquiry 관련 항목 전부, `docs/collab/RULES.md` §8(테스트 범위 정책 갱신).
