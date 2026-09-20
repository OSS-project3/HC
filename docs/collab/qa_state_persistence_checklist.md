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
- [x] **4. `POST /api/auth/terms`(약관 동의) — ✅ 완료(2026-09-20, Claude), 버그 없음·테스트 공백만 있었음.** 신규 `AuthControllerTermsTest`(MockMvc + 실제 JWT)로 실제 엔드포인트를 처음 통과시켜 확인: 전체 동의 시 정상 저장·응답, 일부만 동의(`false` 포함)해도 검증 에러 없이 저장되고 `isAllTermsAgreed()`가 `false`라 재제출이 막히지 않음(의도된 동작으로 확인), 전체 동의 후 재요청은 `409 TERMS_ALREADY_AGREED`, 필드 누락은 `400 INVALID_INPUT`, 토큰 없으면 `401`. 5개 테스트 전부 통과, 전체 회귀 938개 중 1개(`HighSchoolSeederIntegrationTest`, 기존에도 반복 확인된 무관 플레이키) 제외 통과.
- [x] **5. 리프레시 토큰 재발급·로그아웃 왕복 — ✅ 완료(2026-09-20, Claude), 버그 없음·테스트 공백만 있었음.** 기존 `TokenSessionStoreSessionValidationTest`는 `isAccessTokenSessionValid()`의 fail-open/closed 분기만 Mock Redis로 검증했을 뿐, 실제 `/api/auth/refresh`·`/api/auth/logout` 컨트롤러 경로나 `rotateRefreshToken()`의 1회용/재사용 감지 로직은 통과한 적이 없었다. 신규 `AuthControllerRefreshLogoutTest`(실제 로그인→쿠키로 왕복)로 확인: 리프레시 시 access/refresh 토큰이 실제로 회전하고 새 access token이 보호된 라우트에서 동작함, 이미 회전되어 폐기된 구 refresh token 재사용 시 `REFRESH_TOKEN_REUSE_DETECTED`로 거부되고 그 사용자의 전체 세션(방금 정상 발급된 새 토큰까지)이 함께 무효화됨, 쿠키 없이 리프레시하면 `INVALID_REFRESH_TOKEN`, 로그아웃 시 access token은 블랙리스트되고 refresh token도 함께 무효화되어 재사용 불가 — 전부 이미 올바르게 구현돼 있었다. 4개 테스트 전부 통과, 전체 회귀 942개 중 동일한 무관 플레이키(`HighSchoolSeederIntegrationTest`) 1개 제외 통과.
- [x] **6. 후기(Review) 수정 시 기존 이미지 2장 이상 중 일부만 유지(`keepImageIds`)하는 시나리오 — ✅ 완료(2026-09-20, Claude), 실제 버그였음.** 테스트 작성 즉시 `DataIntegrityViolationException`(UNIQUE(review_id, display_order) 위반)으로 재현됨. 원인: `ReviewService.reconcileImages()`가 "kept 이미지 최종 순번 재배정"과 "미포함 이미지 삭제"를 같은 flush에 같이 두고 있었는데, Hibernate가 그 flush 안에서 UPDATE를 DELETE보다 먼저 실행해 삭제 예정 행이 아직 남아있는 순간 kept 행이 같은 순번으로 내려가 제약을 위반했다(3장 중 2번째만 빼고 1·3번 유지 + 신규 추가 시나리오). 수정: 순서를 "임시 오프셋 이동→flush → 삭제→flush → 최종 재배정→flush"로 변경. 신규 테스트 `keepsSelectedSubsetOfMultipleExistingImagesWhileAddingNew` 추가, 전체 회귀 930/931(무관 플레이키 1건 제외) 통과.

## 전체 완료 (2026-09-20)

6건 전부 처리 완료: 1번(정책 확인, 조치 불필요), 6번(실제 버그, 수정 완료), 2·3번(실제 버그, 수정 완료), 4·5번(버그 없음, 테스트 공백만 메움). 총 커밋: `b3a6e50`(6번), `3a4a26c`(2·3번), `6becdf9`(4번), `ff0608f`(5번).

## 확인 완료 — 문제 없음 (기록용, 재작업 불필요)

- 백엔드에 별도 "임시저장/자동저장" 메커니즘 자체가 없음을 확인(프론트 sessionStorage 드래프트만 존재, 서버 쪽엔 검증 대상 자체가 없음).
- 신청 상태 전이 전체(SUBMITTED~COMPLETED, CANCELLED 분기), 결제·자동취소, 이름/카드번호(단건+배치)/카드생성·재생성/미리보기, 만세력 확정, 학생증 텍스트 색상 잠금, 회원가입·로그인·OAuth·계정복구 전체 플로우, 게시판·행사 부분수정은 전부 실제 전이 호출 기준으로 테스트돼 있음.

## 추가 발견 항목 — Codex 조사 (2026-09-20, 7건)

> 위 6건 완료 후 Codex에게 같은 기준으로 다른 도메인(카드 템플릿·시더·단체신청 업로드·회원정보)을 훑어달라고 요청해 나온 결과. 아래 5가지는 구현 전 정책 확정이 필요해 사용자 승인을 받았고(2026-09-20), 확정된 방향을 각 항목에 그대로 기록해뒀다 — 구현 시 재질문 없이 이 결정대로 진행한다.

- [x] **7. 학교 카드 템플릿 업로드 — DB 커밋 이후 예외 시 신규 S3 파일이 잘못 삭제될 수 있음 — ✅ 완료(2026-09-20, Claude), 실제 버그였음.** `SchoolCardTemplateService.upload()`가 `persistenceService.upsert()` 커밋 이후에도 감사 로그 저장·응답 생성을 같은 try/catch 안에서 수행해, 이 단계가 실패하면 DB는 이미 신규 파일을 가리키는데 신규 S3 key를 지워버리는 버그였다(교체 요청이면 기존+신규 파일 모두 유실). 수정: `AdminActivityLog` 저장을 `SchoolCardTemplatePersistenceService.upsert()`의 같은 `@Transactional` 안으로 옮겨 DB 변경과 감사로그를 원자적으로 묶었다. `SchoolCardTemplateService.upload()`의 try/catch는 이제 "업로드+upsert" 단계만 감싸 이 단계 실패 시에만 신규 S3 key를 보상 삭제하고, upsert()가 예외 없이 반환한 뒤(=DB+로그 커밋 확정 후)의 기존파일 정리·응답 생성 단계는 try/catch 밖으로 빼 신규 파일을 절대 건드리지 않는다.
- [x] **8. 학교 카드 템플릿 — 업로드·DB 실패 경계의 보상 동작 테스트 공백 — ✅ 완료(2026-09-20, Claude), 7번과 함께 처리.** 신규 테스트 2건 추가: `SchoolCardTemplateServiceTest.doesNotDeleteNewlyUploadedFilesWhenPresignedUrlGenerationFailsAfterCommit`(커밋 이후 presigned URL 생성 실패 시 CardDesign/UploadFile/감사로그는 그대로 남고 `storageService.delete()`가 전혀 호출되지 않음을 검증), `SchoolCardTemplateServiceAuditLogFailureTest.rollsBackDesignAndDeletesNewFilesWhenAuditLogSaveFails`(별도 Spring context에서 `AdminActivityLogRepository`를 실패하는 Mock으로 교체해, 로그 저장 실패 시 CardDesign/UploadFile이 롤백되고 신규 S3 파일 2개가 보상 삭제됨을 검증). 둘 다 수정 전 RED 확인 후 GREEN. 전체 회귀 944개 중 동일한 무관 플레이키(`HighSchoolSeederIntegrationTest`) 1개 제외 통과.
  - ⚠️ 부수 발견(이번 스코프 아님, 별도 트랙 권장): `CardGenerationService.generate()`([CardGenerationService.java#L56-69](backend/honor-citizen/src/main/java/com/example/honorcitizen/domain/card/service/CardGenerationService.java#L56-L69))에도 완전히 동일한 구조의 문제가 있다 — `persistenceService.persist()` 커밋 이후 기존파일 정리+감사로그 저장이 같은 try 블록 안에 있어, 감사로그 저장이 실패하면 catch가 방금 커밋된 신규 카드 이미지 S3 파일을 잘못 삭제할 수 있다. 이번 작업 범위(학교 카드 템플릿)는 아니라 손대지 않았고, 같은 패턴이니 필요하면 별도 항목으로 다뤄달라.
- [x] **9. CardType/CardDesign 시더 — 중간 실패 시 부분 시드 상태로 고착 — ✅ 완료(2026-09-20, Claude), 실제 버그였음.** `CardTypeSeeder`/`CardDesignSeeder`는 `count() > 0`이면 전체를 건너뛰었는데, `run()` 자체엔 트랜잭션이 없어 중간 실패 시 일부 행만 남고 다음 기동에서도 `count() > 0`이라 복구되지 않았다. CardType ID 1~4는 프론트가 참조하므로 실제 신청 실패로 이어질 수 있는 버그였다.
  - **적용한 수정**: `count() > 0` 전체 건너뛰기 방식을 폐기하고, `CardTypeSeeder`는 코드(`CardTypeCode`)별로, `CardDesignSeeder`는 (cardTypeId, designNumber) 조합별로 존재 여부를 확인해 누락분만 추가 — 기존 정상 행은 절대 덮어쓰지 않는다. designNumber 중복(데이터 손상) 방어는 별도 코드가 필요 없다는 것도 확인됨 — `card_designs` 테이블에 이미 `(card_type_id, design_number)` UNIQUE 제약이 DB 스키마 수준에 있어(`CardDesign.java`) 중복 INSERT 자체가 거절되고, 그 예외를 삼키지 않으므로 애플리케이션 기동이 그대로 실패한다(임의로 하나를 골라 넘어가지 않음 — 요구했던 "충돌 시 기동 실패"가 이미 DB 제약으로 보장돼 있어 코드로 재구현할 필요가 없었음). `run()` 전체를 `@Transactional`로 처리.
  - 신규 테스트 3개(`CardTypeCardDesignSeederAtomicityTest`): 완전 시드 후 재실행해도 중복·ID변경 없음, CardType 2개만 있는 부분 상태에서 재실행하면 기존 행은 그대로 두고 누락분(VISITOR/STUDENT)만 채움, 한 카드종류(HONOR_KOREAN)의 디자인 일부(4~6번)만 지운 뒤 재실행하면 남은 행은 그대로 두고 누락분만 채우며 다른 카드종류(HONOR_CITIZEN)는 영향받지 않음 — 전부 RED→GREEN.
  - 부수 사고: 회귀 실행 중 Docker Desktop이 내려가면서 테스트용 Redis 연결이 끊겨 184개가 무관하게 연쇄 실패했다 — Docker Desktop 재시작 + `hc-test-redis` 컨테이너 재기동 후 재실행해 무관함을 확인. 최종 전체 회귀는 실패 0건(그동안 반복되던 `HighSchoolSeederIntegrationTest` 플레이키도 이번엔 통과).
- [ ] **10. 대학교 `SchoolSeeder` — 실제 DB 적재·재실행 통합 테스트 없음(테스트 공백, 구현은 정책과 일치).** [`SchoolSeederTest`](backend/honor-citizen/src/test/java/com/example/honorcitizen/domain/school/SchoolSeederTest.java#L19)는 CSV 파싱 결과만 검사하고, 실제 [`SchoolSeeder.run()`](backend/honor-citizen/src/main/java/com/example/honorcitizen/domain/school/SchoolSeeder.java#L41)을 호출해 DB 적재·재실행 후 중복 없음까지 확인하는 통합 테스트가 없음(HighSchoolSeeder엔 있음). 정책 결정 불필요 — `HighSchoolSeederIntegrationTest`와 동일한 패턴으로 테스트만 추가하면 됨.
- [x] **11. 단체 신청 ZIP/Excel 업로드 한도 — ✅ 완료(2026-09-20, Claude), 실제 구현 공백이었음.** 정책(BULK_EXCEL_TEMPLATE_POLICY.md §9)은 최대 100명·Excel 5MiB·ZIP 250MiB·해제누적 250MiB·유효파일 110개인데, 실제 Spring multipart 상한은 `application.properties` `10MB`, Nginx도 `nginx.conf` `10m`라 정책상 허용돼야 할 ZIP이 HTTP 진입 전에 거절되고 있었다. `BulkExcelParser`에도 인원수/Excel크기/해제누적크기/엔트리수 상한 검증이 전혀 없었다. `application.properties`(개별 250MiB/전체 270MiB)와 `frontend/nginx.conf`(270MiB, 사용자 승인 완료 — frontend/ 예외 확인) 조정. `BulkExcelParser`에 ZIP 순회 중 실시간 검증 추가: 유효 엔트리 110개 초과 시 즉시 거절(디렉터리/`__MACOSX`/`.DS_Store` 제외, 하위폴더 파일도 카운트에는 포함해 대량 소파일 압축폭탄 방어), 엔트리를 읽는 도중(다 읽은 뒤가 아니라) 압축 해제 누적 250MiB 초과 시 즉시 중단(`readAllBounded` — 진짜 압축폭탄 방어), Excel 5MiB 초과 시 거절, 신청자 100명 초과 시 거절. 신규 테스트 10개(경계값 100/101명, ZIP 111엔트리, Excel 5MiB+1, 누적상한 — 테스트 전용 생성자로 실제 250MiB를 할당하지 않고 작은 상한으로 재현) 전부 RED→GREEN.
  - ⚠️ 부수 사고: `BulkExcelParser`에 검증기 생성자를 2개 만들면서 `@Autowired`로 명시하지 않아 Spring이 어느 생성자를 쓸지 못 정해 전체 테스트 컨텍스트 로딩이 깨졌었다(956개 중 707개 연쇄 실패). `@Autowired`로 운영 생성자를 명시해 해결 — 최종 회귀는 정상.
- [x] **12. 단체 신청 Excel — 헤더명·열 순서 계약 미검증 — ✅ 완료(2026-09-20, Claude), 실제 구현 공백이었음.** 정책상 헤더/열 순서가 다르면 전체 거절해야 하는데 파서가 헤더 행을 "읽지 않고" 건너뛰고 있었다. `BulkExcelParser`에 `validateHeader()` 추가: 앞뒤 공백만 제거한 뒤 공식 헤더(일반·고등학교 11열 / 대학교 13열, `COMMON_HEADERS_11`/`UNIVERSITY_HEADERS_13`)와 정확히 비교, 헤더명·개수·순서 중 하나라도 다르거나 정의되지 않은 추가 열이 있으면 헤더 검증 단계에서 즉시 전체 실패(이후 열 위치 기반 데이터 파싱보다 먼저 실행). 신규 테스트 5개(헤더명 오류, 열 순서 바뀜, 정의되지 않은 추가 열, 대학교인데 학번·학과 헤더 없음, 정확한 공식 헤더는 통과) 전부 RED→GREEN.
  - 기존 테스트 다수가 헤더 행에 "ID" 같은 값만 채우거나 아예 채우지 않고 있었던 것도 이번에 확인됨(Codex가 지적한 "일부 테스트가 과거 값으로도 통과" 사례와 동일 패턴) — `BulkExcelParserTest`/`ApplicationServiceBulkTest`/`BulkExcelToCardRenderingEndToEndTest`/`ApplicationBulkControllerTest`/`ApplicationServiceDailyLimitTest`/`ApplicationServicePhotoReuploadTest`/`ApplicationServiceUploadCompensationTest` 7개 파일의 픽스처 헬퍼를 전부 공식 헤더로 고쳐 회귀 없이 통과시킴.
  - 11·12번 공통: 전체 회귀 956개 중 동일한 무관 플레이키(`HighSchoolSeederIntegrationTest`) 1개 제외 통과.
- [ ] **13. 회원정보 `PATCH /api/users/me` — HTTP 응답만 검증, 실제 DB 반영·부분수정 계약 미검증.** [`UserControllerTest.updateMeUpdatesNameAndPhone()`](backend/honor-citizen/src/test/java/com/example/honorcitizen/api/UserControllerTest.java#L82)이 응답 JSON만 확인하고 Repository 재조회/재조회 GET으로 커밋 값을 확인하지 않음 — "이름만 수정 시 전화번호 유지" 같은 부분수정 계약, dirty checking·트랜잭션 설정 문제로 인한 DB 미반영 회귀를 현재 테스트가 못 잡음.
  - **확정 방향(승인 완료)**: `name`/`phone` 생략 또는 `null`은 기존 값 유지, 빈 문자열은 오류. 두 필드 모두 생략/`null`이면 `INVALID_INPUT`. 전화번호 삭제(명시적으로 비우기)는 현재 필수 연락처 정책상 지원하지 않는다.
- [x] **14. `CardGenerationService` — DB 커밋 이후 보상 삭제 오류(P0) — ✅ 완료(2026-09-20, Claude), 실제 데이터 정합성 버그였음.** `CardGenerationService.generate()`가 `persistenceService.persist()` 커밋 이후에도 기존 파일 정리·`CARD_IMAGE_GENERATED` 감사로그 저장을 같은 try 블록 안에서 수행해, 감사로그 저장이 실패하면 방금 커밋된 신규 카드 이미지 S3 파일을 잘못 삭제할 수 있었다(재생성 시 기존·신규 이미지가 모두 사라질 수 있는 버그, 7번 학교 템플릿과 동일 패턴).
  - **적용한 수정(학교 템플릿과 동일 정책)**: `CARD_IMAGE_GENERATED`(성공) 감사로그 저장을 `CardGenerationPersistenceService.persist()`의 같은 `@Transactional` 안으로 옮겨 카드 경로 DB 반영과 원자적으로 묶었다. `CardGenerationService.generate()`의 try/catch는 "S3 업로드+persist" 단계만 감싸 이 단계 실패 시에만 신규 S3 key를 보상 삭제하고, persist()가 예외 없이 반환한 뒤(=DB+로그 커밋 확정 후) 기존 파일 정리 단계는 try/catch 밖으로 빼 신규 이미지를 절대 건드리지 않는다. 실패 시 감사로그를 남기는 기존 catch 분기(생성 실패 로그)는 그대로 유지 — 이번 수정은 성공 경로의 커밋 경계만 바꿨다.
  - 하위 작업 결과: 카드 경로 저장과 감사로그 동일 트랜잭션 처리 완료 / 최초 생성 시 감사로그 실패 → DB rollback + 신규 S3 2개 삭제 확인 / 재생성 시 감사로그 실패 → 기존 DB 경로·기존 S3 유지 + 신규 S3만 삭제 확인 / 성공 시 DB·감사로그 commit 이후에만 기존 S3 삭제(기존 동작 유지, 회귀 없음 확인) / 보상 삭제 실패가 원 예외를 덮지 않는 기존 `deleteQuietly` 동작 그대로 유지.
  - 신규 테스트 2건(`CardGenerationServiceAuditLogFailureTest.firstGenerationRollsBackAndDeletesNewFilesWhenAuditLogSaveFails`, `regenerationKeepsOldFilesAndDbPathsAndDeletesOnlyNewFilesWhenAuditLogSaveFails`) — 별도 Spring context에서 `AdminActivityLogRepository`를 "성공" 로그만 실패하는 Mock으로 교체해 재현(실패 로그까지 막으면 원 예외가 가려지므로 detail 내용으로 구분). 수정 전 코드로 잠시 되돌려 RED 확인 후 GREEN, 기존 `CardGenerationServiceTest`/`StudentTextColorTest` 전부 회귀 없음. 전체 회귀 946개 중 동일한 무관 플레이키(`HighSchoolSeederIntegrationTest`) 1개 제외 통과.
  - SchoolCardTemplateService(7·8번, 커밋 `11eb06e`)와 별도 커밋으로 처리.
  - SchoolCardTemplateService(7·8번)와 같은 커밋에 섞지 않고 별도 커밋으로 처리한다. 두 구현이 각각 안정된 뒤에만 공통 패턴 추출 필요성을 검토한다(이번 스코프 아님).

### 처리 순서(사용자 확정, 2026-09-20, 14번 추가로 갱신)

1. ~~QA 체크리스트에 미검증 항목 기록~~ (이 항목들)
2. ~~학교 템플릿 트랜잭션/S3 실패 경계(7·8번)~~ — 완료, 커밋 `11eb06e`
2-1. ~~CardGenerationService DB 커밋 이후 보상 삭제 오류(14번, P0)~~ — 완료, 커밋 `9f9a6e8`
3. ~~단체 ZIP·Excel 계약(11·12번)~~ — 완료
4. ~~CardType/CardDesign 시더 원자성(9번)~~ — 완료
5. SchoolSeeder 통합 테스트(10번)
6. 회원정보 부분수정 저장 테스트(13번)
7. 관련 회귀 테스트 실행 및 이 체크리스트·`docs/collab/CHANGELOG.md` 갱신

## 진행 방식

이 문서의 항목을 하나씩: **정책/재현 확인 → (버그면) 실패 테스트 먼저 → 최소 구현 → 회귀** 순서로 처리한다. 항목 처리 후 이 파일의 체크박스를 갱신하고 `docs/collab/CHANGELOG.md`에 기록한다. 7~13번은 정책이 이미 위에 확정돼 있으므로 재확인 없이 그대로 구현한다.
