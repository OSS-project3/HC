# QA 체크리스트 — 상태 전이·중간 저장 검증 (2026-09-20)

> 보안 취약점이 아니라 **기능적 정확성**(사용자 관점에서 상태 변화가 실제로 맞게 되는지, 부분/중간 저장이 맞게 되는지) 기준 조사다. `backend/honor-citizen` 전체 도메인의 상태 전이·부분 업데이트를 실제 테스트가 "전이 메서드를 직접 호출하고 결과 상태를 검증"하는지 기준으로 훑었다(단순 `ReflectionTestUtils`로 상태를 미리 박아두고 스킵하는 건 검증으로 안 침).
>
> 결론: 대부분(신청 상태 전이, 결제, 이름/카드번호/카드생성, 회원가입·로그인·계정복구, 게시판·행사 부분수정 등)은 이미 실제 테스트로 잘 덮여 있다. 아래는 **실제로 빈 곳만** 추린 목록이다.

## 발견된 미검증 항목 (6건)

- [x] **1. `Application.markRefunded()` — ✅ 확인 완료(2026-09-20, Claude), 버그 아님·조치 불필요.** `docs/collab/TODO.md` "신청 상태·취소·환불 구조 변경 체크리스트"의 "최신 환불 운영 정책(2026-09-13 확정)"과 그 CHANGELOG 항목을 확인한 결과, 이미 공식적으로 확정된 정책이었다: 시스템은 환불 완료 여부를 아예 관리하지 않고(관리자가 `CANCELLED + CONFIRMED` 조합만 보고 은행 내역 등 외부 절차로 처리), `refundedAt` 필드·`markRefunded()`는 "이전 정책의 구현 흔적"으로 의도적으로 남겨두되 신규 흐름에 연결하지 않기로 결정됨(TODO.md 777~783행 전부 `[x]`). 즉 죽은 코드가 맞지만 **버그가 아니라 확정 정책의 결과**이므로 삭제·구현 둘 다 필요 없음.
  - ⚠️ 부수 발견(별도 트랙, 이번 QA 체크리스트 범위 아님): `docs/api/admin.md` 73행 "8. 환불 완료 기록"과 `docs/specs/application/requirements.md` 306~309행의 `refundedAt` 기반 설명이 이 2026-09-13 정책보다 오래돼 최신 상태를 반영 못 하고 있음(상세 API 섹션 없이 목록에만 남아있음/구모델 그대로 서술). 코드는 문제 없으니 이번 체크리스트에서 손대지 않았고, 문서 정리가 필요하면 별도로 알려달라.
- [x] **2. `Application.markCardReady()` 두 번째 호출(멱등성) — ✅ 완료(2026-09-20, Claude), 실제 버그였음.** 엔티티 `markCardReady()` 자체는 이미 멱등하게 구현돼 있었다(두 번째 호출은 `false` 반환, 상태 변경 없음). 그런데 `ApplicationService.markCardReady()`가 이 반환값을 무시하고 매번 무조건 `AdminActivityLog`(`CARD_ISSUE`)를 저장하고 있었다 — 관리자가 실수로 두 번 누르면 아무 상태 변화도 없는데 "카드 발급 완료" 로그가 중복 기록되는 버그. `confirmPaymentByAdmin()`처럼 반환값을 확인해 최초 반영 때만 로그를 남기도록 수정. 신규 테스트 `markCardReadyIsIdempotentAndDoesNotDuplicateLog` 추가.
- [x] **3. `Application.markPhysicalDispatched()` 두 번째 호출(멱등성) — ✅ 완료(2026-09-20, Claude), 실제 버그였음.** 2번과 동일한 패턴의 버그. 엔티티는 두 번째 호출 시 운송장 번호를 덮어쓰지 않고 `false`를 반환하도록 이미 구현돼 있었지만, `ApplicationService.dispatchPhysical()`이 이를 무시하고 매번 `AdminActivityLog`(`TRACKING_REGISTER`)를 새 운송장 번호로 저장 — 실제로는 무시된 두 번째 운송장 번호가 로그에는 마치 등록된 것처럼 남는 버그. 동일하게 반환값 확인 후 최초 반영 때만 로그 저장하도록 수정. 신규 테스트 `dispatchPhysicalIsIdempotentAndDoesNotOverwriteTrackingNumberOrDuplicateLog` 추가.
  - 2·3번 공통: 전체 회귀 933개 중 1개 실패(`HighSchoolSeederIntegrationTest.reseedingTheSameCsvDoesNotDuplicateRows`, 무관 플레이키 — 단독 재실행 시 통과, 6번 항목 처리 때도 동일 테스트가 무관 플레이키로 기록됨) 제외 전부 통과.
- [ ] **4. `POST /api/auth/terms`(약관 동의) 실제 엔드포인트가 테스트 0건.** 관련 테스트 15개 파일 전부가 `user.agreeTerms(...)`를 엔티티 메서드로 직접 호출해서 상태만 세팅하고, 실제 HTTP 엔드포인트(`AuthController.agreeTerms`)나 `UserService.agreeTerms()` 자체를 통과하는 테스트가 없다. 신규 OAuth 가입자가 실제로 약관 동의 화면에서 제출했을 때 저장이 맞게 되는지, 일부만 동의하고 제출하면 막히는지 실제 코드 경로로 확인 필요.
- [ ] **5. 리프레시 토큰 재발급·로그아웃 엔드포인트 자체의 왕복 테스트 없음.** 로그인 → 리프레시 호출 → 새 토큰 정상 작동/헌 토큰 무효화(1회용이라면) 확인, 로그아웃 → 세션 재사용 안 되는지 확인 필요.
- [x] **6. 후기(Review) 수정 시 기존 이미지 2장 이상 중 일부만 유지(`keepImageIds`)하는 시나리오 — ✅ 완료(2026-09-20, Claude), 실제 버그였음.** 테스트 작성 즉시 `DataIntegrityViolationException`(UNIQUE(review_id, display_order) 위반)으로 재현됨. 원인: `ReviewService.reconcileImages()`가 "kept 이미지 최종 순번 재배정"과 "미포함 이미지 삭제"를 같은 flush에 같이 두고 있었는데, Hibernate가 그 flush 안에서 UPDATE를 DELETE보다 먼저 실행해 삭제 예정 행이 아직 남아있는 순간 kept 행이 같은 순번으로 내려가 제약을 위반했다(3장 중 2번째만 빼고 1·3번 유지 + 신규 추가 시나리오). 수정: 순서를 "임시 오프셋 이동→flush → 삭제→flush → 최종 재배정→flush"로 변경. 신규 테스트 `keepsSelectedSubsetOfMultipleExistingImagesWhileAddingNew` 추가, 전체 회귀 930/931(무관 플레이키 1건 제외) 통과.

## 확인 완료 — 문제 없음 (기록용, 재작업 불필요)

- 백엔드에 별도 "임시저장/자동저장" 메커니즘 자체가 없음을 확인(프론트 sessionStorage 드래프트만 존재, 서버 쪽엔 검증 대상 자체가 없음).
- 신청 상태 전이 전체(SUBMITTED~COMPLETED, CANCELLED 분기), 결제·자동취소, 이름/카드번호(단건+배치)/카드생성·재생성/미리보기, 만세력 확정, 학생증 텍스트 색상 잠금, 회원가입·로그인·OAuth·계정복구 전체 플로우, 게시판·행사 부분수정은 전부 실제 전이 호출 기준으로 테스트돼 있음.

## 진행 방식

이 문서의 항목을 하나씩: **정책/재현 확인 → (버그면) 실패 테스트 먼저 → 최소 구현 → 회귀** 순서로 처리한다. 항목 처리 후 이 파일의 체크박스를 갱신하고 `docs/collab/CHANGELOG.md`에 기록한다.
