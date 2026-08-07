# CHANGELOG

작업을 종료할 때마다 **맨 위에** 새 항목을 추가한다 (최신이 위). 과거 항목은 수정·삭제하지 않는다.

## 템플릿

```md
## {YYYY-MM-DD} — {Claude|Codex} — `{브랜치명}`

- 변경: {무엇을 바꿨는지 한두 줄}
- 파일: {변경한 파일 목록}
- 사유: {왜 바꿨는지 — 요구사항 변경 / 버그 수정 / 설계 정리 등}
- 관련: TODO #{번호} (있다면)
```

---

## 2026-08-07 — Claude — `main` (checklist.md §4 — 업로드 보상 삭제)

- 변경: `checklist.md` §4 일곱 번째 항목 구현 — (1) 생성 경로: `createIndividual`/`createGroup`이 업로드한 storage key를 순서대로 추적하고, `applicationPersistenceService.saveIndividual`/`saveGroup`이 실패하면 역순으로 `storageService.delete`를 호출한 뒤 원래 예외를 재던짐(고아 파일 방지). (2) 수정 경로: `reuploadPhoto`가 DB 갱신에 성공한 뒤 개인은 기존 사진, 단체는 기존 회원 사진 전체와 기존 제출 ZIP(`UploadFileRepository` 조회)을 삭제.
- 파일: `ApplicationService.java`(createIndividual/createGroup/reuploadPhoto, `storeUploadFile`/`storePhotoFile`/`storePhotoBytes`에 key-tracking 오버로드 추가, `deleteUploadedFilesReversed`/`deleteIfPresent` 신규), `ApplicationServiceUploadCompensationTest.java`(신규 — `createIndividualDeletesUploadedFilesInReverseOrderWhenPersistenceFails`, `createGroupDeletesUploadedFilesInReverseOrderWhenPersistenceFails`), `ApplicationServicePhotoReuploadTest.java`(`reuploadPhotoForIndividualDeletesOldPhotoFile`, `reuploadPhotoForGroupDeletesOldMemberPhotosAndOldSubmitFile` 신규)
- 테스트: 신규 테스트 4건을 구현 전 실패 확인 후 통과. Application/API 도메인 113개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "DB 실패 시 요청 업로드 파일 역순 보상 삭제, 파일 수정 시 DB 갱신 성공 후 기존 파일 삭제가 필요하다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 학번 형식 검증)

- 변경: `checklist.md` §4 여섯 번째 항목 구현 — 개인 신청 경로(`ApplicationService.validateStudentFields`)에서 학생증 학번이 최대 10자·숫자만 허용하도록 형식 검증 추가(`\d{1,10}` 정규식). 단체 신청 경로(`BulkExcelParser`)는 이번 항목 범위 밖(별도 TODO 항목에서 처리 예정).
- 파일: `ApplicationService.java`(validateStudentFields, isValidStudentId 신규), `ApplicationServiceTest.java`(`createIndividualRejectsStudentIdWithNonDigitCharacters`, `createIndividualRejectsStudentIdLongerThanTenDigits` 신규)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 109개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "학번은 필수·최대 10자·숫자만 허용한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — Applicant.email 요청값 반영)

- 변경: `checklist.md` §4 다섯 번째 항목 구현 — `Applicant.email`을 항상 `User.email`로 고정 저장하던 것을, 요청(`applicant.email`)이 있으면 그 값을, 없으면 `User.email`을 기본값으로 저장하도록 변경. `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `ApplicantRequest`에 `email` 필드 신규 추가(검증 annotation 없음 — 신청 화면에서 자유롭게 수정 가능해야 하므로).
- 파일: `ApplicationCreateRequest.java`(`ApplicantRequest.email`), `BulkApplicationCreateRequest.java`(`ApplicantRequest.email`), `ApplicationService.java`(createIndividual/createGroup), `ApplicationServiceTest.java`(`createIndividualSavesApplicantEmailFromRequestWhenProvided`, `createIndividualFallsBackToUserEmailWhenApplicantEmailBlank` 신규), `ApplicationServiceBulkTest.java`(`createGroupSavesApplicantEmailFromRequestWhenProvided` 신규)
- 테스트: 신규 테스트 3건을 구현 전 실패 확인 후 통과. Application/API 도메인 107개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "User.email은 기본값이며 신청 화면에서 수정 가능해야 한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — sameAsApplicant 복사 범위 제한)

- 변경: `checklist.md` §4 네 번째 항목 구현 — `sameAsApplicant=true`여도 배송지(우편번호·주소·상세주소·배송메모)는 항상 요청의 `receiver` 값을 저장하도록 변경(기존엔 `copyFromApplicant`/`copyIndividualReceiver`가 이 필드들을 전부 `null`로 덮어씀). 이름·연락처는 요청값이 비어 있을 때만 Applicant 값으로 대체(fallback) — `ReceiverRequest`에 검증 annotation이 없어 빈 값 제출이 가능하기 때문.
- 파일: `ApplicationPersistenceService.java`(saveReceiverIfNeeded/saveGroupReceiverIfNeeded), `ApplicationServiceTest.java`(`createIndividualCopiesReceiverFromApplicantWhenSameAsApplicantTrue`→`createIndividualUsesSubmittedReceiverAddressEvenWhenSameAsApplicantTrue`로 갱신 + `createIndividualFallsBackToApplicantNameAndPhoneWhenReceiverFieldsBlank` 신규), `ApplicationServiceBulkTest.java`(`createGroupUsesSubmittedReceiverAddressEvenWhenSameAsApplicantTrue` 신규 + `requestWithPhysicalReceiverSameAsApplicant` 헬퍼)
- 테스트: 갱신/신규 테스트 3건을 구현 전 실패 확인 후 통과. Application/API 도메인 104개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "이름·연락처는 자동 복사 후 수정 가능, 배송지는 Receiver가 항상 입력".
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — MOBILE+Receiver 거절)

- 변경: `checklist.md` §4 세 번째 항목 구현 — `issueType=MOBILE`인데 `receiver`를 전달하면 `INVALID_INPUT`으로 거절(기존엔 `MOBILE_AND_PHYSICAL`인데 `receiver` 없는 경우만 검증했음). 개인(`validateReceiverPresence`)·단체(`validateGroupReceiverPresence`) 둘 다 반대 방향 검증 추가.
- 파일: `ApplicationService.java`, `ApplicationServiceTest.java`(`createIndividualRejectsReceiverWhenMobile` 신규), `ApplicationServiceBulkTest.java`(`createGroupRejectsReceiverWhenMobile` 신규 + `requestWithMobileAndReceiver` 헬퍼)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 102개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영.
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — ApplicationPersistenceService 분리)

- 변경: `checklist.md` §4 두 번째 항목 구현 — `ApplicationService`를 비트랜잭션 오케스트레이터로, DB 저장을 신규 `ApplicationPersistenceService`(`@Transactional`)로 분리. self-invocation으로 인해 `@Transactional`이 무력화되는 문제를 막기 위해 별도 Bean으로 도입(`APPLICATION.md` §5). `saveIndividual()`/`saveGroup()`이 Application→Applicant→Receiver(조건부)→ApplicationMember 순서로 한 트랜잭션에 저장. 단체 신청은 파일 업로드(트랜잭션 밖)와 DB 저장(트랜잭션 안) 사이를 넘기기 위해 `GroupMemberUpload`(row+photoPath) record 신규 도입.
- 파일: `ApplicationPersistenceService.java`(신규), `GroupMemberUpload.java`(신규), `ApplicationService.java`(createIndividual/createGroup 및 관련 private 메서드 이동), `ApplicationPersistenceServiceTest.java`(신규 3테스트)
- 테스트: 신규 테스트를 클래스 부재로 컴파일 실패 확인 후 구현, 통과. Application/API 도메인 100개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영.
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 학생증 직인 선택 구현)

- 변경: `checklist.md` §4 첫 항목 구현 — 학생증 학교 직인(seal)을 필수 → 선택으로 변경. `ApplicationController.createGroup`의 `seal` 파트를 optional로 변경(개인 신청 `schoolSeal`은 이미 optional이었음). `ApplicationService.validateStudentFields`는 학번·학과·로고만 필수로 검사하고 직인은 있을 때만 형식 검증. `createIndividualApplication`/`createGroup`은 직인이 없으면 업로드를 건너뛰고 `sealFileId=null`로 저장.
- 파일: `ApplicationController.java`, `ApplicationService.java`(validateStudentFields/createIndividualApplication/createGroup), `ApplicationServiceTest.java`(`createIndividualForStudentCardSucceedsWithoutSchoolSeal` 신규), `ApplicationServiceBulkTest.java`(`createGroupSucceedsForStudentCardWithoutSeal` 신규)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 97개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영(사용자 승인된 작업 방식 — TDD, 최소 범위, 완료마다 TODO/CHANGELOG 갱신).
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Codex — Application 정책 문서 동기화 및 Code Audit

- APPLICATION.md와 POLICY_SYNC_CHECKLIST.md에 맞춰 requirements/data-model/api/checklist 및 운영 문서를 동기화했다.
- 실제 Controller, Service, Validator, Factory, Entity, DTO, Repository, ErrorCode, Test 구현을 파일·클래스·메서드·라인 단위로 재검증해 수정 필요·미구현 항목을 기록했다.
- 코드 파일은 수정하지 않았다.
- (Claude 추가 2026-08-07: 학생증 `department`(학과) 필드 삭제는 이번 동기화에서 제외 — 사람이 미결정 상태로 확인, `PENDING_DECISIONS.md` 참고)

## 2026-08-06 — Claude — `main` (마이페이지 신청 목록/상세 조회 API 설계)

- 변경: 로그인 사용자가 자기 신청 내역을 목록(`GET /api/my/applications`, 페이징+status 필터)/상세(`GET /api/my/applications/{id}`, 소유권 검증)로 조회하는 API 2개 설계. 기존 `POST /api/applications/lookup`(API 3)은 비로그인 공개 조회용이라 이 용도로 못 씀 — 사용자가 "로그인한 경우 다건조회 → 클릭 시 단건조회" 흐름을 요청해서 추가.
- 파일: `docs/specs/application/api.md`(API 6/7 신규), `docs/collab/TODO.md`
- 사유: Application 도메인 현재 구현 상태(생성만 완성, 조회/수정/취소는 부분적/미구현)를 사용자가 점검하다가 발견한 갭.
- 참고: 설계까지만 진행, 구현은 다음 확인 후 — `ApplicationRepository.findByUserId(...)` 신규 필요, `PageResponse<T>`는 Review 목록조회와 공유하는 첫 실제 사용처가 될 예정.
- 관련: TODO "마이페이지 신청 목록/상세 조회 API 6·7 설계"

## 2026-08-06 — Claude — `main` (Review 자격 검증 정책 확정)

- 변경: 후기 작성 자격을 `Application.user_id`(제출 계정) 대신 **이메일 매칭**(`Applicant.email` 또는 `ApplicationMember.email`이 로그인 계정 이메일과 일치)으로 검증하도록 확정. 단체 신청은 대표 제출자뿐 아니라 실제 카드를 받은 구성원 개인(같은 이메일로 별도 가입된 경우)도 자격을 인정 — `lookup` API의 본인확인 방식과 같은 사고방식. 신규 `REVIEW_NOT_ELIGIBLE`(403) 에러코드 추가.
- 파일: `docs/specs/review/data-model.md`(§2.1 신규), `docs/specs/review/api.md`, `docs/collab/TODO.md`
- 사유: 사용자 지적 — "단체신청도 개인에서는 카드 정보를 조회할 수 있는 사람만" — 기존 설계(신청유형/카드종류를 자유 입력받는 자기신고 방식)로는 신청 경험이 없는 사람도 후기를 쓸 수 있어서 수정.
- 관련: TODO "Review 자격 검증 정책 반영"

## 2026-08-06 — Claude — `main` (Review 도메인 설계)

- 변경: 후기(Review) 작성 요구사항 변경에 따라 Entity/API 설계. `Review`(작성자 실계정 `user_id`와 화면표시 `author_display_name`을 분리, `application_type` 재사용, 신청 실체와 FK 연결 없이 자기신고 값), `ReviewCardType`(`@ElementCollection`, `CardTypeCode` 다중 선택), `ReviewImage`(`UploadFile` 재사용 + `review_id`/`upload_file_id`/`display_order` join). API 3개(등록/목록조회/단건조회) 설계, 목록 응답 최소 4필드로 제한, 프로젝트 첫 페이징 응답 포맷(`PageResponse<T>`) 제안. `docs/api/upload-file.md`에 있던 옛 "`Review.thumbnail_file_id`(단일)" 가정을 대체.
- 파일(신규): `docs/specs/review/{data-model,api}.md`. 파일(수정): `docs/api/{README,upload-file,board}.md`, `arch.md`(§4.7 Review 모듈 신설, 기존 Board는 §4.8로 이동, §5.3/§5.5 갱신), `docs/collab/TODO.md`
- 사유: 사용자 요청 — "후기 기능 요구사항 변경, Entity/API 설계까지만(구현 금지)"
- 참고: [TBD] 3건(카드종류 0개 허용 여부/본문 최대 글자수/조회수 노출 여부)과 사진 첨부 최대 개수는 확인 후 반영 필요 — TODO.md에 기록. 구현·프론트 반영은 이번 범위 밖.
- 관련: TODO "Review 도메인 설계"

## 2026-08-06 — Claude — feature/application-domain-impl (조회 인증 정책 + CardType ID 고정)

- 변경: (1) `ApplicationService.lookup()`을 method별로 분리 검증하도록 수정 — `method=application`은 phone·email 둘 다 필수+둘 다 일치해야 함(기존엔 OR), `method=card`는 phone·email 검증을 아예 제거(카드번호 단독 조회). (2) 신규 `CardTypeSeeder`(`CommandLineRunner`)를 추가해 최초 기동 시 `HONOR_KOREAN=1, HONOR_CITIZEN=2, VISITOR=3, STUDENT=4` 순서로 시딩 — 프론트가 `cardTypeId`를 1~4로 하드코딩해서 쓰는 것을 그대로 허용하기 위함(신규 `GET /api/card-types` API는 만들지 않기로 결정).
- 파일: `ApplicationService.java`(lookup/lookupByCard/matches), `ApplicationServiceLookupTest.java`(신규 케이스로 재작성), `domain/card/CardTypeSeeder.java`(신규)
- 사유: 프론트 `LookupPage.tsx`/`ApplyPage.tsx` 실제 구현 대비 UI-API 갭 분석 결과를 사람이 확인하고 확정한 정책. `backend/FRONTEND_API_REQUIREMENTS.md`(main 브랜치)에 결정 배경 상세 기록.
- 테스트: `ApplicationServiceLookupTest` 전체 재작성 후 통과. Application/API 도메인 테스트 95개 중 기존 `UserControllerTest` 2건(Redis 미기동 환경 의존, 무관)만 실패.
- 참고: 같은 갭 분석에서 나온 다른 결정 3건(단체 파일은 `logo`/`seal`/`submitFile` 3파트 유지, 단체 재제출은 이미 백엔드 구현 완료·프론트 UI만 남음, `englishName`은 언어 무관 필드로 확정)은 코드 변경이 필요 없어 `FRONTEND_API_REQUIREMENTS.md`에만 기록. 이 커밋엔 Task 4~6 로드맵의 다른 진행 중 변경(ApplicationFactory 등)은 포함하지 않음 — 파일 단위로 분리해서 커밋함.
- 관련: 2026-08-06 UI/API 갭 분석

## 2026-08-01 — Claude — `backend-api` (User/Application End-to-End 플로우 테스트)

- 변경: 실사용자 시나리오 기준 통합 테스트(`UserApplicationFlowTest`) 추가 — Google 로그인(OAuth2SuccessHandler와 동일 코드 경로로 재현) → 인증 유지(Cookie/JWT, 이후 전 구간 동일 쿠키 재사용) → 개인 신청 생성 → 신청 조회(lookup) → 신청 상태 전이(관리자 검토·반려를 엔티티 레벨로 재현, HTTP API 없음) → Lookup 재조회(반려 사유 노출 확인) → 사진 재업로드 → DB 최종 상태 검증까지 8단계를 한 테스트로 연결. 각 단계 HTTP 상태코드/Response/DB 상태/(모킹된) 파일 업로드 호출을 전부 검증.
- 로컬에 Redis가 없어 처음엔 기존 `UserControllerTest`의 탈퇴 테스트 2개와 이 신규 테스트가 전부 막혀 있었음 — Docker로 Redis 컨테이너를 띄워서 실제로 통과하는 것까지 확인(이 컴퓨터엔 다른 프로젝트("zerotime")의 Redis가 이미 6379를 점유 중이라 별도 컨테이너를 다른 포트로 띄움 — `REDIS_PORT` 오버라이드는 로컬 전용이라 커밋 안 함).
- 테스트 작성 중 버그 발견: `@Transactional` 테스트 메서드에서 `deleteAll()` 직후 `save()`를 호출하면 Hibernate의 flush 순서(Insert가 Delete보다 먼저 실행됨) 때문에 유니크 제약 위반 발생 — 이 프로젝트의 나머지 테스트들이 전부 비-`@Transactional`(각 리포지토리 호출이 즉시 커밋)인 이유와 일치, 새 테스트도 동일 컨벤션으로 맞춤.
- 파일: `backend/honor-citizen/src/test/java/com/example/honorcitizen/flow/UserApplicationFlowTest.java`
- 사유: 사용자 요청 — "API 단위 테스트는 충분, 이제 실제 사용자 시나리오 기준 User Flow Integration Test 작성"
- 관련: 전체 테스트 83/83 통과(Redis 가용 시)

## 2026-08-01 — Claude — `backend-api` (전체 코드베이스 감사 + 정리)

- 변경: 사용자 요청으로 전체 백엔드 감사(① 문서에 없는 코드 ② 코드에 없는 문서 ③ 호출자 없는 클래스 ④ 호출자 없는 API ⑤ 아키텍처 위반 ⑥ 실행 안 되는 코드) 수행 후 삭제 가능 항목 정리. `infra/card/*`(5개 파일, CitizenCard 삭제 후 orphan) 삭제. `domain/photo/*` + `api/UploadController.java` 삭제(프론트 `src/` 검색 결과 호출 없음 확인, `docs/api/upload-file.md`도 "독립 API 불필요"로 이미 결론). `domain/user/dto/{TokenRefreshRequest,TokenRefreshResponse}`(정의만 되고 미사용) 삭제. `ErrorCode`에서 `DUPLICATE_APPLICATION`(미사용)과 domain/photo 삭제로 연쇄 orphan된 `UNSUPPORTED_FILE_TYPE`/`INVALID_IMAGE`/`INAPPROPRIATE_IMAGE`/`PHOTO_NOT_FOUND`/`PHOTO_EXPIRED`/`PHOTO_OWNER_MISMATCH` 제거. **아키텍처 위반 수정**: `ApplicationService`가 `UserRepository`를 직접 주입하던 것 — arch.md "다른 도메인의 Repository를 생성자 주입하지 않는다" 위반 — `UserService.findById()` 경유로 교체.
- 파일: `infra/card/*`(삭제), `domain/photo/*`(삭제), `api/UploadController.java`(삭제), `domain/user/dto/TokenRefresh{Request,Response}.java`(삭제), `common/exception/ErrorCode.java`, `domain/application/service/ApplicationService.java`, `docs/collab/TODO.md`
- 사유: "다 갈아엎고 지금 쓰레기 클래스 없음?" 질문에서 시작된 전체 감사 요청, 결과 승인 후 정리 실행
- 관련: TODO "전체 코드베이스 감사 + 죽은 코드/아키텍처 위반 정리"

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 5 완료)

- 변경: IDENTITY와 Root 선저장 순서를 유지하는 package-private ApplicationFactory를 추가하고 개인 신청의 Application, Applicant, Receiver, ApplicationMember 생성 책임을 Service에서 이동. Service는 검증, 파일 준비, 생성 호출, 저장 순서를 조정.
- 파일: ApplicationFactory.java, ApplicationService.java, ApplicationFactoryTest.java, docs/specs/application/requirements.md, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: CreatedApplication, CreatedChildren, Context 없이 최소 구조로 Entity 생성 책임을 독립시키기 위함.
- 테스트: Factory 테스트를 구현 전 클래스 부재로 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 101개 중 기존 UserControllerTest 2건만 실패.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 5

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 4 완료)

- 변경: Application 생성 시 서버 값의 책임을 확정. 수령인 동일 여부 계산을 ApplicationCreateRequest의 파생 메서드로 이동하고 Service는 이를 사용하도록 정리. prepareServerValues, Context, Factory는 추가하지 않음.
- 파일: ApplicationCreateRequest.java, ApplicationService.java, ApplicationCreateRequestTest.java, docs/specs/application/requirements.md, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: 신청번호는 Service, 초기 상태는 Entity, 수령인 동일 여부는 Request의 독립된 책임이므로 별도 준비 객체로 묶을 필요가 없음.
- 테스트: 신규 DTO 테스트 2건을 구현 전 메서드 부재로 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 99개 중 기존 UserControllerTest 2건만 실패.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 4

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 4 정책 확정)

- 변경: 상담 후 신청하고 신청 이후 계좌이체하는 흐름을 확정하여 Application 생성 시 total_price를 계산·저장하지 않도록 문서를 정정. 단체 신청은 엑셀 ID와 ZIP 사진 파일명을 매칭하되 ID를 저장하거나 구성원별 사진 파일 ID를 생성하지 않는 것으로 확정.
- 파일: docs/specs/application 문서, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: Task 4 구현 전에 가격과 파일 식별 정책을 확정하여 불필요한 서버 생성값과 중간 구조 도입을 방지.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 4

## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 3)

- 변경: `ApplicationPhotoValidator`를 추가해 얼굴사진과 학생증 학교 로고·직인의 5MiB, 확장자, MIME, signature, 디코딩을 검증. 얼굴사진은 EXIF Orientation 적용 후 300×400 최소 해상도를 검증하고 학교 파일은 해상도에서 제외. 학생증 필수값은 기존 Service private 메서드에서 공백까지 거절하도록 보강.
- 파일: `ApplicationPhotoValidator.java`, `ApplicationService.java`, `ApplicationPhotoValidatorTest.java`, `ApplicationServiceTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/{requirements,api}.md`, `docs/collab/{TODO,CHANGELOG}.md`
- 사유: Application Task 3의 확정 사진·학생증 정책을 업로드와 DB 저장 전에 적용. StudentCardValidator 별도 클래스는 구조 변경 원칙상 추가하지 않음.
- 테스트: Validator 정책 테스트 8건과 Service 통합 테스트 2건 추가. Application 관련 전체 테스트 통과. 전체 97개 중 기존 `UserControllerTest` 2건만 실패.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 3"
## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 2)

- 변경: 기존 `UserService.findEligibleApplicationUser()`에서 회원 존재·ACTIVE·USER 권한·필수 약관을 검증하고 User를 반환하도록 구현. ApplicationService의 UserRepository 직접 의존을 제거하고 UserService를 사용. 하루 3회 제한은 정책 보류에 따라 미구현.
- 파일: `domain/user/service/UserService.java`, `domain/application/service/ApplicationService.java`, `ApplicationServiceTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/api.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`
- 사유: Application Task 2의 확정 정책을 기존 ErrorCode로 구현하고 도메인 간 Repository 직접 참조를 제거.
- 테스트: 신규 상태·권한·약관 테스트 3건을 구현 전 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 87개 중 기존 `UserControllerTest` 2건만 실패.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 2"
## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 1)

- 변경: `createIndividual()`을 private 메서드 중심으로 분리하고 User 조회를 첫 단계로 이동. User 미존재 시 CardType 조회·신청번호 생성·파일 업로드·DB 저장 전에 `USER_NOT_FOUND`로 중단하도록 테스트 우선으로 보장. Factory/Validator/Context는 추가하지 않음.
- 파일: `domain/application/service/ApplicationService.java`, `domain/application/service/ApplicationServiceTest.java`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`
- 사유: Application 개인 신청 리팩터링 Task 1. 이후 로직의 전제조건인 User 존재를 부수효과보다 먼저 검증하고, 최소 구조 변경으로 Service 가독성을 개선.
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 84개 중 기존 `UserControllerTest` 2건만 실패하며 Task 전후 동일.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 1"

## 2026-08-01 — Claude — `feature/application-domain-impl` (API 4/5 + merge)

- 변경: API 4(`PATCH /api/applications/{id}/photo`, 사진 재업로드)·API 5(`GET /api/applications/{id}/cards/download`, 카드 다운로드) 구현으로 Application 도메인 5개 API 전부 완료. `backend-api`(Codex의 `docs/specs/application/*` 재구성, `ecd72b3`)를 이 브랜치로 머지(`docs/collab/*` 4개 파일만 충돌, RULES.md 7절 방식대로 수동 재작성). `ErrorCode`에서 삭제된 레거시 도메인 전용 코드 정리, `CARD_NOT_READY` 재사용. 컴파일이 깨진 orphan `infra/toss/*`(Payment 도메인 삭제 후 미사용) 삭제. `checklist.md` 6개 섹션 자체 검증(결과는 HANDOFF.md).
- 파일: `domain/application/service/ApplicationService.java`, `domain/application/dto/{ApplicationPhotoReuploadResponse,ApplicationCardDownloadResponse}.java`, `api/ApplicationController.java`, `common/exception/ErrorCode.java`, `infra/toss/*`(삭제), 테스트 4개 클래스(신규 21테스트)
- 사유: 사용자 승인("응") 후 API 4/5 이어서 구현, 그 직전 "변경사항을 받아오고" 요청으로 backend-api 병합 선행
- 관련: TODO "Application 도메인 엔티티/API 구현"(완료로 갱신)

## 2026-08-01 — Claude — `feature/application-domain-impl`

- 변경: `docs/specs/application/*` 기준으로 Application 도메인 재구현. 옛 BulkOrder/CitizenCard/KoreanName/Payment/Shipping 도메인 및 옛 Application/CardType/ApplicationStatus 삭제. 신규 CardType/CardDesign/UploadFile/Applicant/Receiver/ApplicationMember 엔티티 + 재작성된 Application(8단계 상태머신). API 1(개인 신청)/API 2(단체 ZIP 신청, 신규 BulkExcelParser)/API 3(신청 조회 lookup) 구현. GlobalExceptionHandler에 MissingServletRequestPartException 핸들러 추가.
- 파일: `domain/application/*`, `domain/card/*`, `domain/uploadfile/*`, `api/ApplicationController.java`, `common/enums/*`, `common/exception/GlobalExceptionHandler.java`, `infra/security/SecurityConfig.java`, 테스트 다수
- 사유: 사용자 요청("Application 구현 전 docs/specs/application 읽고 시작") + 5개 확정 문서(requirements/data-model/api/checklist + arch.md) 기준 구현
- 관련: TODO "Application 도메인 엔티티/API 구현 착수", HANDOFF.md의 "확인 필요" 항목(englishName/total_price/엑셀실패정책 — 사람 확인 완료, docs/specs/application 반영은 Codex 몫)

## 2026-08-01 — Codex — `feature/application-domain-docs`

- 변경: Application 문서를 `docs/specs/application/` 아래의 `requirements.md`/`data-model.md`/`api.md`/`checklist.md`로 패키지화하고 기존 경로 참조를 갱신.
- 파일: `docs/specs/application/*`, `DB.md`, `docs/api/README.md`, Application 경로를 참조하는 협업·테스트 문서
- 사유: Application 업무 규칙, 데이터 모델, API 계약과 검증 영역을 한 도메인 폴더에서 찾을 수 있도록 Source of Truth 구조를 정리.
- 관련: TODO "Application 문서 도메인 패키지 이전"

## 2026-08-01 — Claude — `backend-api`

- 변경: 협업 규칙 체계(`docs/collab/`) 신설 — `RULES.md`/`TODO.md`/`CHANGELOG.md`/`HANDOFF.md` 추가. 기존 `guide.md`는 `RULES.md`로 가리키는 안내문으로 축소.
- 파일: `docs/collab/RULES.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`, `guide.md`
- 사유: Claude(구현)와 Codex(문서/아키텍처)가 각자 다른 워크트리에서 작업하면서 서로의 변경사항을 사람이 매번 전달해야 하는 문제를 해결하기 위해, 작업 시작/종료 시 반드시 확인·갱신하는 공용 문서 체계를 만듦.
- 관련: TODO #1

## 2026-08-01 — Claude — `feature/application-domain-docs` (codex-docs 워크트리)

- 변경: `arch.md`의 계층 구조를 4계층(API/Application/Domain/Infrastructure) + Port/Adapter에서, 실제 코드 규모에 맞는 3단 구조(Controller/Service/Repository·Entity, Infra)로 단순화. 패키지 구조 예시를 실제 코드(`api/`, `domain/{도메인}/{entity,repository,service,dto}`, `infra/`)와 일치시킴. Command/Query 강제 분리 규칙 완화, 테스트 섹션 네이밍을 실제 컨벤션(`{Class}Test`/`{Class}ServiceTest`/`{Class}ControllerTest`)에 맞춤.
- 파일: `arch.md`
- 사유: 사용자가 "현재 구조에서는 과한 설계이니 규모에 맞게 축소해달라"고 요청. 비즈니스 규칙(4~18절 대부분)은 그대로 유지.
- 관련: -
