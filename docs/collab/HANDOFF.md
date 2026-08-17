# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-18
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **단체 신청 Excel 사진 번호 고정 완료** — v1.1 양식 3종의 A열을 `사진 번호`로 바꾸고 001~100을 텍스트로 사전 입력·보호했다. `BulkExcelParser`는 번호만 있는 행을 빈 행으로 무시하고 B열 이후 입력이 있는 행만 신청자로 처리하며 실제 처리 행 사진만 매칭한다. 집중 테스트 19개와 워크북 자동·시각 검증을 통과했다.
- **Board 도메인(공지사항/FAQ) CRUD 5개 API 구현 완료** — 공개 조회 2개(목록/단건) + 관리자 전용 3개(생성/수정/삭제). `Board`+`BoardType{NOTICE,FAQ}` enum 통합 관리, 첨부파일은 `BoardAttachment`(`UploadFile` join 엔티티, NOTICE 전용). 이 프로젝트 첫 관리자 전용 쓰기 API라 `SecurityConfig`에 `/api/admin/**` → `hasRole("ADMIN")` 라우트 규칙을 신규 추가(`arch.md` §4.6엔 이미 명시돼 있었지만 코드엔 없던 공백을 메움). 신규 테스트 34개 전부 통과. 상세: `docs/specs/board/{data-model,api}.md`, `CHANGELOG.md` 2026-08-14 항목.
- **Event 도메인(행사사업 부스/협업) CRUD 5개 API 구현 완료** — Board와 동일 패턴 재사용(관리자 라우트는 `/api/admin/**` 규칙에 자동 편입, 신규 `SecurityConfig` 변경은 공개 GET `permitAll()` 뿐). `EventPost`(썸네일 직접 보유, `visible`/`displayOrder`)+`EventImage`(갤러리, `UploadFile` 미경유 — Review의 `image_path` 직접 저장 패턴). 설계 단계에서 사용자와 확정한 2가지: (1) `EventImage.representative` 플래그 없음(썸네일 컬럼이 유일한 대표 이미지 소스) (2) 관리자 전용 전체 목록 API(`GET /api/admin/events`)는 이번 패스 제외, 이후 별도 구현. 신규 테스트 39개 전부 통과. 상세: `docs/specs/events/{data-model,api}.md`, `CHANGELOG.md` 2026-08-16 항목("Event 도메인 신규 구현").
- **일일 신청 3회 제한(`APPLICATION.md` §7) 구현 완료** — `checklist.md` §4·§5의 마지막 미구현 항목. 사용자별·일자별 카운터(`ApplicationDailyLimit`)를 비관적 락으로 잠그고 원자적으로 증가시켜, "카운트 확인 후 저장" 사이 경쟁 상태를 차단. 오늘 첫 신청 두 건이 동시에 도착해 유니크 제약이 충돌하면 새 트랜잭션으로 한 번 재시도. `createIndividual`/`createGroup`에 파일 업로드 이전 지점에서 연결, 실패 시 슬롯 반환. **취소하면 슬롯이 반환되도록 구현**(사용자 확인 완료) — 이후 누군가 `APPLICATION.md` §16(신청 상태 리팩터링·취소 API 정책)을 작성하면서 §16-3에 동일한 결론("최초 취소 성공 시에만 슬롯 반환")을 남겨 서로 일치함을 확인함. `releaseSlot()`은 재사용 가능한 공개 메서드지만, 실제 "신청 취소" API 자체가 아직 없어(TODO #64) 지금은 실패 보상 경로에서만 호출됨 — Entity(`Application.cancel()`)는 구조상 Service를 호출할 수 없어(arch.md 계층 규칙) 취소 API 구현 시 그 Service 계층에서 연결해야 함. 동시성 검증(`ExecutorService`+`CountDownLatch`) 포함 신규 테스트 19개 전부 통과. 상세: `CHANGELOG.md` 2026-08-16 항목("일일 신청 3회 제한 구현").
- ⚠️ **위 세 작업 모두 `TODO.md`/`CHANGELOG.md`는 매번 갱신했지만, 이번까지 `HANDOFF.md`는 한 번도 갱신하지 않았다** — `RULES.md` §5·§6 위반(모든 작업 종료 시 예외 없이 갱신해야 함). 사용자가 "왜 업데이트가 안 되는 것 같냐"고 지적해서 뒤늦게 이번에 처음 갱신함. 앞으로는 작업 종료마다 반드시 함께 갱신할 것.
- (참고) 이 세션 중 Codex 또는 다른 세션이 동시에 `APPLICATION.md`에 §16(신청·결제·취소·환불 정책, `ApplicationStatus` 재정의 포함)을 새로 추가했고, `TODO.md`에 관련 행(#63/#64)도 생겼다 — 이건 내가 작성한 게 아니라 발견만 했다. 상세는 "다음에 할 일" 참고.

## 다음에 할 일

- **TODO #64 — 신청 상태 리팩터링 및 사용자 취소 API 구현 (미착수, 담당 미정)**: `APPLICATION.md` §16에 새 정책이 이미 문서화돼 있다.
  - `ApplicationStatus` 재정의: `PAYMENT_PENDING`/`RECEIVED` 제거, `SUBMITTED`/`PRODUCTION_READY` 신규 추가 — 기존 상태 전이 로직(`ApplicationStatus.canTransitionTo`)과 `confirmPayment()`/`startReview()` 등 전이 메서드 전체를 다시 짜야 하는 breaking change.
  - `ApplicationStatus`와 `PaymentStatus`의 관계 재정의: "입금 확인만으로 ApplicationStatus를 변경하지 않는다"는 새 원칙(§16-1) — 지금 코드는 `confirmPayment()`가 상태까지 같이 바꾸는 구조라 다름.
  - 신규 취소 이력 필드(`cancelledAt`/`cancellationType`/`cancellationReason`), 최소 환불 모델(`refundedAt`), `POST /api/applications/{id}/cancel` API 신규 구현.
  - 취소 성공 시 일일 신청 슬롯 반환은 이미 구현된 `ApplicationDailyLimitService.releaseSlot()`을 그대로 재사용하면 됨(위 항목 참고).
  - 이 작업에 착수하기 전에 TODO #63(문서 정합성 반영: requirements/data-model/api 동기화)이 먼저 되어 있는지 확인 필요.
- **TODO #62 — 마이페이지 신청 목록/상세 조회 API 6·7 구현**: 설계만 있고 구현 안 됨(`ApplicationRepository.findByUserId(...)` 신규 필요).
- **관리자 전용 전체 목록 API**: Board(`GET /api/admin/boards`는 이미 있음— Board는 애초에 `visible` 개념이 없음 참고)와 달리 Event는 `GET /api/admin/events`(visible 무관 전체 조회)가 설계엔 있지만 구현 안 됨 — 관리자가 숨긴 글을 다시 찾을 방법이 없음.
- **Inquiry(1:1 문의)/관리자(Admin) 신청관리·통계/카드 카탈로그 공개 API**: `docs/FRONTEND_API_GAPS.md`·`docs/FRONTEND_USER_FLOW_AUDIT.md`(Codex 작성) 기준으로 TODO에 행만 추가해뒀고 전부 미착수.
- **Redis 의존 테스트 3건**: 이전부터 있던 이슈, 이번 세션들과 무관.

## ❓ 확인 필요

- 없음 — 이번 세션에서 다룬 정책 질문(일일 3회 제한의 취소 포함 여부, `EventImage.representative` 여부, Board 관리자 라우트 방식)은 전부 사용자 확인 후 확정·구현·문서 반영까지 완료됨.

## 참고

- 관련 테스트: `./gradlew.bat test --tests "com.example.honorcitizen.domain.board.*" --tests "com.example.honorcitizen.domain.event.*" --tests "com.example.honorcitizen.domain.application.service.ApplicationDailyLimit*" --tests "com.example.honorcitizen.api.Board*" --tests "com.example.honorcitizen.api.Event*"`(이번 세션 신규분만), 또는 `./gradlew.bat test`(전체).
- 결과: 전체 스위트 316개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 관련 커밋(최신순): `26ac036`(일일 3회 제한), `2f19f53`(Event 도메인), `a5b23f3`(Board 도메인).
- 관련 문서: `docs/specs/{board,events}/{data-model,api}.md`, `docs/specs/application/APPLICATION.md` §7·§16, `docs/collab/PENDING_DECISIONS.md`(관련 TBD 항목 해결 표시됨), `docs/collab/TODO.md`.
