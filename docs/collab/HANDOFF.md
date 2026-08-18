# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-18
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **단체 신청 Excel 사진 번호 고정 완료** — v1.1 양식 3종의 A열을 `사진 번호`로 바꾸고 001~100을 텍스트로 사전 입력·보호했다. `BulkExcelParser`는 번호만 있는 행을 빈 행으로 무시하고 B열 이후 입력이 있는 행만 신청자로 처리하며 실제 처리 행 사진만 매칭한다. 집중 테스트 19개와 워크북 자동·시각 검증을 통과했다.
- **Application 상태 enum·Entity 전이 구현 완료** — `ApplicationStatus`를 `SUBMITTED → REVIEWING → NAME_EDITING → PRODUCTION_READY → PRODUCING → COMPLETED` 중심으로 재정의하고 결제 상태를 분리했다. 결제 안내/입금 확인, 사진 반려·재제출, IssueType별 카드 준비·실물 인계, 사용자/미입금 자동 취소, 최소 환불 완료 전이와 관련 필드 및 `@Version`을 `Application`에 구현했다. 집중 Entity 테스트 6개와 직접 영향 회귀 테스트 71개가 통과했다. Service/API/스케줄러 연결과 운영 DB 마이그레이션은 다음 단위다.
- **사용자 취소 API·S3 정리 완료** — `POST /api/applications/{applicationId}/cancel`이 조회→소유권→Entity 멱등 취소→신청 생성일 KST 슬롯 반환을 하나의 트랜잭션으로 처리한다. 최초 취소 DB commit 후에만 얼굴사진·로고·직인·제출 ZIP을 S3에서 삭제하고 DB 파일 참조와 UploadFile 행을 정리한다. rollback·중복 취소에서는 S3를 삭제하지 않으며 삭제 실패는 경고 로그만 남기고 취소 결과를 유지한다. WAITING/CONFIRMED 환불 필요 응답, 401/403/404/상태 오류 계약까지 Controller 통합 테스트로 확인했다.
- **결제 Service·미입금 자동취소 스케줄러 완료** — 관리자 결제 안내와 입금 확인 Service를 구현했다. 최초 입금 확인에만 `PAYMENT_CONFIRMED` 감사로그 1건을 남긴다. 결제 안내 후 72시간 경과한 `SUBMITTED + WAITING` 신청을 기본 10분 cron으로 조회해 신청별 트랜잭션에서 자동취소하며, 슬롯 반환과 취소 파일 정리를 재사용한다. 늦은 입금은 취소 상태를 복구하지 않는다. 관리자 HTTP API는 아직 구현 전이다.
- **Board 도메인(공지사항/FAQ) CRUD 5개 API 구현 완료** — 공개 조회 2개(목록/단건) + 관리자 전용 3개(생성/수정/삭제). `Board`+`BoardType{NOTICE,FAQ}` enum 통합 관리, 첨부파일은 `BoardAttachment`(`UploadFile` join 엔티티, NOTICE 전용). 이 프로젝트 첫 관리자 전용 쓰기 API라 `SecurityConfig`에 `/api/admin/**` → `hasRole("ADMIN")` 라우트 규칙을 신규 추가(`arch.md` §4.6엔 이미 명시돼 있었지만 코드엔 없던 공백을 메움). 신규 테스트 34개 전부 통과. 상세: `docs/specs/board/{data-model,api}.md`, `CHANGELOG.md` 2026-08-14 항목.
- **Event 도메인(행사사업 부스/협업) CRUD 5개 API 구현 완료** — Board와 동일 패턴 재사용(관리자 라우트는 `/api/admin/**` 규칙에 자동 편입, 신규 `SecurityConfig` 변경은 공개 GET `permitAll()` 뿐). `EventPost`(썸네일 직접 보유, `visible`/`displayOrder`)+`EventImage`(갤러리, `UploadFile` 미경유 — Review의 `image_path` 직접 저장 패턴). 설계 단계에서 사용자와 확정한 2가지: (1) `EventImage.representative` 플래그 없음(썸네일 컬럼이 유일한 대표 이미지 소스) (2) 관리자 전용 전체 목록 API(`GET /api/admin/events`)는 이번 패스 제외, 이후 별도 구현. 신규 테스트 39개 전부 통과. 상세: `docs/specs/events/{data-model,api}.md`, `CHANGELOG.md` 2026-08-16 항목("Event 도메인 신규 구현").
- **일일 신청 3회 제한(`APPLICATION.md` §7) 구현 완료** — 사용자별·일자별 카운터를 비관적 락으로 원자 처리하고 생성 실패 시 슬롯을 반환한다. 사용자 취소 API에도 연결되어 최초 취소 시 신청 생성일 KST 슬롯을 같은 트랜잭션에서 반환한다. 동시성 검증 포함 상세는 `CHANGELOG.md` 2026-08-16 및 2026-08-17 항목 참고.
- **마이페이지 신청 목록/상세 조회 API 6·7 구현 완료, 커밋은 보류 (Claude, 2026-08-18)** — 아래 "TODO #62"였던 항목. `GET /api/my/applications`(목록, `status` 선택 필터+`createdAt DESC` 고정 정렬)·`GET /api/my/applications/{id}`(상세, 소유권 검증) 둘 다 구현+테스트 완료. `receiver`는 `issueType=MOBILE_AND_PHYSICAL`일 때만 채워진다. 신규 테스트 13개 전부 통과, 전체 스위트 361개 중 기존과 동일하게 `UserControllerTest` 2건+Redis 미기동 1건만 실패(회귀 없음). 상세: `docs/specs/application/api.md` API 6/7, `CHANGELOG.md` 2026-08-18 항목("마이페이지 신청 목록/상세 조회 API 6·7 구현").
  - ⚠️ **커밋 보류 이유**: 응답 DTO `MyApplicationDetailResponse`가 api.md에 이미 문서화된 새 엔티티 구조를 그대로 따라 `Application.paymentGuidedAt/paymentDueAt/cancelledAt/cancellationType/cancellationReason/refundedAt/cardReadyAt/physicalDispatchedAt` 게터를 직접 읽는다. 이 필드들은 전부 Codex가 진행 중인 "신청 상태 리팩터링"(TODO #64, 아래 항목)의 **미커밋** `Application` 엔티티 변경분에만 존재하고, 지금 커밋된 HEAD의 `Application`엔 없다(구 `PAYMENT_PENDING/RECEIVED` enum 그대로). 그래서 이 작업만 단독으로 커밋하면 `ApplicationService`/DTO가 `cannot find symbol` 컴파일 오류로 깨진다.
  - HEAD(`8c99394`) 기준으로 `ApplicationRepository`/`ApplicationService`/`MyApplication*` 관련 파일을 재구성해 blob-stage 커밋을 시도해봤으나, 위 이유로 "내 작업만 골라 커밋"이 원천적으로 불가능함을 확인하고 사용자에게 보고 → **사용자가 "커밋 보류, 작업은 유지"를 선택**(2026-08-18). 코드는 working tree에 그대로 남아 있고 전체 스위트 기준으로는 정상 동작·통과한다. Codex의 "신청 상태 리팩터링" 커밋이 먼저 들어간 뒤에 이 작업을 커밋한다.
- 현재 working tree에는 이번 문서 변경 외에도 Codex의 진행 중 변경(신청 상태 리팩터링·취소/환불·스케줄러 관련 `ApplicationService.java`/`Application.java`/`ApplicationStatus.java`/`ApplicationDailyLimitService.java` 등, 테스트 파일 삭제, User/프론트 문서 변경)이 남아 있다. 다음 세션은 이를 건드리거나 임의로 커밋하지 않는다 — Codex가 먼저 커밋해야 할 몫이다.

## 다음에 할 일

- **마이페이지 API 6·7 커밋 (Codex 리팩터링 커밋 이후)**: 구현·테스트는 끝났고 working tree에 그대로 있다. Codex의 "신청 상태 리팩터링"(`Application`/`ApplicationStatus`/`ApplicationService` 등)이 먼저 커밋되면, 그 위에 `ApplicationRepository.findByUserId`/`findByUserIdAndStatus`, `ApplicationMemberRepository.countByApplicationId`, `MyApplication*` 신규 파일, `ApplicationService.listMyApplications`/`getMyApplicationDetail`만 골라 커밋한다.
- **신청 상태 리팩터링 후속 Service/API 구현 (Codex 다음 작업)**: enum·Entity 전이는 완료됐다.
  - 사용자 취소 API와 S3 정리, 결제 Service, 미입금 자동취소 스케줄러는 완료.
  - 다음은 관리자 결제 안내·입금 확인 HTTP API 또는 검토·제작 상태 명령 Service 연결이다.
  - 관리자 결제 안내/입금 확인/검토·제작 명령과 10분 설정형 자동 취소 스케줄러를 연결한다.
  - 취소 commit 직후 S3 정리, 환불 완료 기록, 카드 다운로드 `cardReadyAt` 기준 변경을 후속 검증 단위로 나눈다.
  - 취소 성공 시 일일 신청 슬롯 반환은 이미 구현된 `ApplicationDailyLimitService.releaseSlot()`을 그대로 재사용하면 됨(위 항목 참고).
  - 자동 취소 스케줄러는 기본 10분 cron이며 설정으로 변경 가능하게 구현한다.
  - enum에서 `PAYMENT_PENDING/RECEIVED`를 제거하면 현재 Entity와 테스트가 즉시 컴파일되지 않으므로 Enum/필드 단계와 Entity 전이 단계를 하나의 build 가능한 논리 단위로 묶을지 사용자 확인 후 착수한다.
- **관리자 전용 전체 목록 API**: Board(`GET /api/admin/boards`는 이미 있음— Board는 애초에 `visible` 개념이 없음 참고)와 달리 Event는 `GET /api/admin/events`(visible 무관 전체 조회)가 설계엔 있지만 구현 안 됨 — 관리자가 숨긴 글을 다시 찾을 방법이 없음.
- **Inquiry(1:1 문의)/관리자(Admin) 신청관리·통계/카드 카탈로그 공개 API**: `docs/FRONTEND_API_GAPS.md`·`docs/FRONTEND_USER_FLOW_AUDIT.md`(Codex 작성) 기준으로 TODO에 행만 추가해뒀고 전부 미착수.
- **Redis 의존 테스트 3건**: 이전부터 있던 이슈, 이번 세션들과 무관.

## ❓ 확인 필요

- 다음 코드 단위에서 `ApplicationStatus` enum 변경과 `Application` 상태 전이 변경을 분리하면 중간 커밋이 컴파일되지 않는다. 두 단계를 하나의 build 가능한 논리 단위로 묶는 방향을 사용자에게 확인한다.

## 참고

- 마이페이지 API 6·7 작업은 전체 Gradle 테스트 스위트(361개)를 실행해 회귀 없음을 확인했다.
- 관련 문서: `docs/specs/application/{APPLICATION,requirements,data-model,api,service-flow}.md`, `docs/api/{admin,payment}.md`, `docs/collab/{TODO,PENDING_DECISIONS,CHANGELOG}.md`.
