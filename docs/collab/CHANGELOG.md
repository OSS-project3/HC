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

## 2026-08-14 — Claude — `main` (학생증 신청 항목 추가 — 학교구분·가로형/세로형)

- 변경: 학생증(STUDENT) 카드 신청 방식을 변경했다. 사용자 요청: 개인 신청은 대학교/고등학교 선택 + 가로형/세로형 선택을 새로 받고, 대학교를 선택했을 때만 학번·학과를 입력받는다(고등학교는 추가 입력 없음). 법인·단체 신청은 가로형/세로형 + 학교구분 선택만 추가하고, 학번·학과는 여전히 첨부 엑셀로만 받는다.
  - **신규 enum**(`common/enums/`): `Orientation{LANDSCAPE,PORTRAIT}`, `SchoolType{UNIVERSITY,HIGH_SCHOOL}`. JSON 값은 대문자(이 프로젝트에 enum 케이스 변환 설정이 없어 Jackson 기본 동작 그대로 — `gender` 필드와 동일하게 프론트가 전송 직전 `.toUpperCase()` 필요).
  - **`Application` 엔티티**에 `orientation`/`school_type` 컬럼 신규 추가 — 개인·단체 공통, 학생증 전용, 신청서 전체에 1개(단체도 엑셀 컬럼이 아니라 신청 폼 필드). `createIndividual`/`createGroup` 정적 팩토리는 새 2개 파라미터를 받는 버전을 추가하면서, 기존 시그니처(로고/직인까지만 받던 버전)를 **하위 호환 오버로드**로 남겨 `null, null`로 위임하게 했다 — 그 시그니처를 직접 호출하는 기존 테스트가 약 20개(Review 도메인 테스트 포함) 있어, 전부 고치는 대신 오버로드로 격리해 무관한 파일을 건드리지 않았다.
  - **`ApplicationService.validateStudentFields`(개인)**: 기존 "학생증이면 학번·학과 무조건 필수" 규칙을 "학생증+`schoolType=UNIVERSITY`일 때만 필수, `HIGH_SCHOOL`이면 있으면 오히려 거절"로 변경. orientation·schoolType·로고는 학교구분과 무관하게 학생증이면 항상 필수. 신규 ErrorCode 없이 기존 `INVALID_INPUT` 재사용.
  - **`ApplicationService.createGroup`(단체)**: 학생증이면 orientation·schoolType 둘 다 필수, 아니면 둘 다 없어야 함(`INVALID_INPUT`) — 학번·학과 검증은 그대로 `BulkExcelParser`(엑셀) 책임으로 남겨두고 이번 변경에서 건드리지 않았다.
  - 세션 중 두 가지를 사용자에게 확인 후 확정: (1) 단체 신청도 처음엔 "학교구분 필드 없이 첨부 엑셀 자유기재"로 논의했으나, 프론트에 애초에 그런 UI가 없다는 걸 같이 확인한 뒤 "단체도 체크박스 추가"로 최종 확정 — 그 결과 schoolType을 `ApplicationMember`가 아니라 `Application` 레벨로 옮겨(orientation과 동일 위치) 개인·단체 모델을 통일했다. (2) orientation 값의 JSON 대소문자 계약을 명확히 확인 — 대문자, `gender` 필드와 동일 관례.
  - 카드종류별 config/전략 추상화는 도입하지 않고 기존 `CardType.isStudentCard()`(`isStudent` boolean) 게이트를 그대로 재사용 — 재사용처가 1곳(학생증)뿐이라 새 추상화는 과설계라고 판단(이 저장소의 "재사용 2곳 이상 아니면 새 클래스 안 만든다" 원칙과 일치).
- 파일: `Orientation.java`, `SchoolType.java`(신규), `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `Application.java`, `ApplicationFactory.java`, `ApplicationPersistenceService.java`, `ApplicationService.java`, `ApplicationServiceTest.java`(+7 신규 케이스), `ApplicationServiceBulkTest.java`(+5 신규 케이스), `ApplicationServiceUploadCompensationTest.java`(기존 학생증 픽스처에 orientation/schoolType 보정), `ApplicationFactoryTest.java`(시그니처 보정), `docs/specs/application/{data-model,api}.md`, `docs/collab/TODO.md`
- 테스트: TDD로 진행(테스트 먼저 작성 → 의도대로 실패 확인 → 구현 → 통과). `ApplicationServiceTest`/`ApplicationServiceBulkTest` 신규 12개 케이스 전부 통과. 전체 스위트 224개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: 학생증 신청 방식이 대학교/고등학교로 갈리도록 정책이 바뀌었고, 구현 전 `/plan`으로 변경범위(엔티티·DTO·검증 로직·기존 테스트 파급범위)를 먼저 확정한 뒤 착수했다.
- 관련: TODO "학생증 신청 항목 추가" 행(완료로 갱신), 계획 파일 `C:\Users\gpdnj\.claude\plans\application-api-async-knuth.md`

---

## 2026-08-14 — Codex — `main` (User 조회 문서 정합성 정리)

- 변경: `docs/api/user.md`의 `GET /api/users/me` 과거 구현 전 문구를 현재 백엔드 구현 상태에 맞게 정리하고, API 상태를 구현 완료로 갱신했다.
- 파일: `docs/api/user.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`
- 테스트: 코드 변경 없음. `rg`로 stale 문구 제거와 구현 근거 문구를 정적 검증했다.
- 사유: `GET /api/users/me`는 현재 `UserController#getMe`로 구현되어 있으나 문서에는 과거 구현 전 문구가 남아 있어 정합성을 맞춤.
- 관련: TODO "User 조회 문서 정합성 정리" 행(완료로 갱신)

## 2026-08-13 — Claude — `main` (Review 도메인 CRUD 5개 API 구현)

- 변경: `docs/specs/review/{data-model,api}.md`에 설계된 Review 도메인을 실제로 구현했다. TDD로 진행(테스트 먼저 작성 → 실패 확인 → 구현 → 통과 확인)했으며, 신규 테스트 76개 전부 통과.
  - **엔티티/리포지토리**: `Review`(`card_type_id`/`image_path`를 컬럼으로 직접 저장, join 엔티티 없음), `ReviewRepository extends JpaSpecificationExecutor<Review>`(이 프로젝트 첫 Specification 사용).
  - **이미지 검증**: `ReviewImageValidator`(2MB, jpg/jpeg/png/webp, 해상도 하한 없음 — `ApplicationPhotoValidator`와 기준이 달라 재사용하지 않고 신규 작성). WEBP는 Java 표준 `ImageIO`가 디코딩을 지원하지 않아 `com.twelvemonkeys.imageio:imageio-webp:3.10.1` 의존성을 신규 추가(`build.gradle`) — 사람이 "WEBP 디코딩 라이브러리 추가"로 결정.
  - **자격검증**: `ReviewEligibilityService` — (1) 로그인 사용자의 이메일이 `Applicant.email`(대표 제출자) 또는 `ApplicationMember.email`(단체 구성원 개인)과 매칭되는 `Application.status=COMPLETED` 건 중 `(application_type, card_type_id)` 조합이 일치해야 통과(`REVIEW_NOT_ELIGIBLE`), (2) ✅ 세션 중 신규 확정: 같은 사용자가 같은 조합으로 이미 작성한 후기가 있으면 거절(`REVIEW_ALREADY_EXISTS`, "조합당 1개" 정책 — `Review→Application` FK를 두지 않는 기존 설계를 유지하기 위해 판단 기준은 `(user_id, application_type, card_type_id)` 조합의 유일성), (3) ✅ 세션 중 신규 확정: 탈퇴(`WITHDRAWN`) 계정은 새 후기를 등록할 수 없음(`ALREADY_WITHDRAWN`) — 단 이 체크는 **등록에만** 적용하고 수정에는 적용하지 않음(원작성자가 나중에 탈퇴해도 관리자가 기존 후기를 계속 관리할 수 있어야 하므로).
  - **API 1 등록** `POST /api/reviews`: multipart(`request` JSON + `image` 0~1개). `image` 파트 2개 이상 전송 시 `INVALID_INPUT`.
  - **API 2 목록** `GET /api/reviews`: `cardTypeId`/`hasPhoto`/`searchType`+`keyword` 필터, 페이징(기본 size=9, 상한 100). `ReviewSpecifications`로 동적 쿼리 구현. **주의**: 이 프로젝트가 쓰는 Spring Data JPA 버전은 `Specification.where(null)`/`.and(null)`을 더 이상 허용하지 않고(과거 버전과 동작이 다름) `IllegalArgumentException`을 던지므로, 각 조건 메서드가 null 대신 `cb.conjunction()`(항상 참)을 반환하도록 작성해야 한다. 정렬은 `createdAt DESC`만으로는 동시 등록 시 밀리초 단위로 값이 같아질 수 있어(H2 등) `id DESC`를 2차 정렬키로 추가했다(초기 구현에서 실제로 플레이키 발생 후 수정).
  - **API 3 단건조회** `GET /api/reviews/{id}`: 비로그인 공개 조회, `canEdit`/`canDelete`는 관리자 또는 작성자 본인만 `true`. `next`(다음 오래된 글)만 제공.
  - **API 4 삭제** `DELETE /api/reviews/{id}`: 작성자 또는 관리자만 가능(`FORBIDDEN`). Review row 삭제 후 트랜잭션 commit 시점에 S3 이미지 객체 삭제(`TransactionSynchronizationManager.registerSynchronization`, `ApplicationService`의 기존 after-commit 패턴 재사용).
  - **API 5 수정** `PATCH /api/reviews/{id}`: 등록과 동일 5개 필드 전체 재제출 + `removeImage`. 사진 처리 3가지 경우(교체/삭제/유지) 구현. `applicationType`/`cardTypeId` 변경 시 원작성자(`Review.userId`, 수정자 아님) 기준으로 자격 재검증.
  - **공통**: `PageResponse<T>`(이 프로젝트 첫 페이징 응답 포맷) 신규. `ErrorCode`에 `REVIEW_NOT_FOUND`/`REVIEW_NOT_ELIGIBLE`/`REVIEW_ALREADY_EXISTS`/`INVALID_IMAGE_FILE` 추가(`INVALID_IMAGE_FILE`은 기존 `INVALID_IMAGE`가 "얼굴을 식별할 수 없습니다"라는 얼굴사진 전용 메시지라 Review에 그대로 재사용하면 오해의 소지가 있어 신규로 분리). `GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` 핸들러 추가(`?searchType=WRONG` 같은 잘못된 enum 쿼리 파라미터를 `INVALID_INPUT`으로 응답). `SecurityConfig`에 `GET /api/reviews`·`GET /api/reviews/{id}`만 `permitAll()` 추가(등록/수정/삭제는 기존 `hasAnyRole("USER","ADMIN")` 그대로 적용).
  - **테스트 관례 명문화**: 서비스 계층(`@SpringBootTest`+실 H2)·컨트롤러 계층(`@AutoConfigureMockMvc`+`MockMvc`, 실제 JWT로 Security 필터체인까지 통과) 2계층 테스트 패턴을 처음으로 `docs/collab/RULES.md` §8에 문서화 — 기존 코드에 이미 있던 관례를 관찰해 따른 것이라 명문화만 함.
- 파일: `Review.java`, `ReviewRepository.java`, `ReviewImageValidator.java`, `ReviewSpecifications.java`, `ReviewEligibilityService.java`, `ReviewService.java`, `ReviewController.java`, `ReviewCreateRequest/Response.java`, `ReviewUpdateRequest.java`, `ReviewListItemResponse.java`, `ReviewDetailResponse.java`, `CardTypeSummaryResponse.java`, `ReviewSearchType.java`, `PageResponse.java`, `ErrorCode.java`, `GlobalExceptionHandler.java`, `SecurityConfig.java`, `ApplicantRepository.java`/`ApplicationMemberRepository.java`(`findByEmail` 신규 추가), `build.gradle`(twelvemonkeys 의존성), 관련 테스트 8개 파일(신규), `docs/specs/review/{data-model,api}.md`, `docs/collab/RULES.md` §8(신규)
- 테스트: Review 도메인 신규 76개 전부 통과(`ReviewTest`, `ReviewEligibilityServiceTest`, `ReviewImageValidatorTest`, `ReviewServiceCreateTest`, `ReviewServiceListTest`, `ReviewServiceDetailTest`, `ReviewServiceDeleteTest`, `ReviewServiceUpdateTest`, `ReviewControllerTest`, `PageResponseTest`). 전체 스위트 216개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: `docs/specs/review/{data-model,api}.md`에 설계만 있고 구현이 없던 상태를 실제 동작하는 API로 완성. 구현 도중 발견한 정책 공백(후기 작성 개수 제한, 탈퇴 계정 처리) 2건은 임의로 결정하지 않고 사람에게 확인 후 문서(`api.md`/`data-model.md`)에 먼저 반영한 뒤 구현했다.
- 관련: TODO "Review 도메인 구현" 행(완료로 갱신)

## 2026-08-09 — Codex — `main` (Redis 기동 후 전체 테스트 실패 재분류)

- 변경: Redis 기동 후 기존 전체 테스트 실패 3건을 재실행해 실패 원인을 재분류했다. `UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken`, `UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall`은 통과했고, 이전 실패 원인은 Redis 미기동에 따른 `RedisConnectionFailureException`으로 확인했다. `UserApplicationFlowTest.fullUserApplicationFlow`는 Redis 연결 실패가 해소됐지만 신청 생성 단계에서 `TERMS_NOT_AGREED` 403으로 실패했다.
- 파일: `docs/specs/application/checklist.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`
- 테스트: `./gradlew.bat test --tests com.example.honorcitizen.api.UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken --tests com.example.honorcitizen.api.UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall --tests com.example.honorcitizen.flow.UserApplicationFlowTest.fullUserApplicationFlow` 실행 — 3개 중 2개 통과, 1개 실패(`TERMS_NOT_AGREED`).
- 사유: Redis 기동 후 실패가 환경 문제인지, 현재 정책/테스트 불일치인지 정확히 분리하기 위함.
- 관련: Application 검증 작업 후속 확인

## 2026-08-08 — Codex — `main` (재업로드 S3 정리 after-commit 전환)

- 변경: 사진 재업로드에서 기존 S3 파일 삭제를 DB 트랜잭션 commit 이후에만 수행하도록 `TransactionSynchronizationManager` 기반 after-commit 정리로 변경. 신규 업로드 S3 key는 메서드 내부 실패뿐 아니라 transaction rollback/commit 실패 경로에서도 보상 삭제되도록 after-completion 보상 경로를 추가했다. `PHOTO_REJECTED → PENDING` stale 주석은 SoT 기준인 `PHOTO_REJECTED → REVIEWING`으로 수정했다.
- 파일: `ApplicationService.java`, `ApplicationServicePhotoReuploadTest.java`, `docs/specs/application/service-flow.md`
- 테스트: 재업로드 rollback 시 신규 S3 삭제·기존 S3 유지, after-commit 기존 S3 삭제 실패 시 성공 응답 유지, 단체 재업로드 멤버 사진 업로드 실패 시 신규 ZIP 보상 삭제를 최소 보강.
- 사유: `@Transactional` 메서드 내부 마지막에 기존 S3 파일을 삭제하면 실제 DB commit 이후 삭제가 보장되지 않아, rollback 시 DB는 복구됐지만 기존 S3 파일은 삭제되는 정합성 위험이 있었음.
- 관련: Application 검증 작업 커밋 분리 계획 — 2. 재업로드 transaction-safe cleanup

## 2026-08-08 — Codex — `main` (생성 경로 S3 업로드 실패 보상)

- 변경: `createIndividual`/`createGroup`의 S3 업로드 구간까지 보상 처리 범위를 확장. 로고·직인·ZIP·멤버 사진 업로드 중간 실패와 DB 저장 실패 모두에서 이미 업로드된 신규 S3 key를 역순 삭제한다. 보상 삭제 실패는 원 예외를 덮어쓰지 않고 로그만 남기도록 정리했다.
- 파일: `ApplicationService.java`, `ApplicationServiceUploadCompensationTest.java`, `docs/specs/application/service-flow.md`
- 테스트: `ApplicationServiceUploadCompensationTest`에 S3 업로드 중간 실패와 보상 삭제 실패 케이스를 최소 보강. `./gradlew.bat test --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceUploadCompensationTest"` 통과.
- 사유: 기존 try/catch가 DB 저장 호출만 감싸 S3 업로드 중간 실패 시 앞서 업로드된 파일이 고아로 남을 수 있던 failure-path 정리.
- 관련: Application 검증 작업 커밋 분리 계획 — 1. 생성 경로 S3 failure compensation

## 2026-08-08 — Claude — `main` (UserUpdateRequest — address를 수정 대상에서 제외, name 길이 제한 추가)

- 변경: `PATCH /api/users/me`의 수정 가능 필드를 `name`/`phone`으로 확정(사람 확인) — `email`은 기존처럼 OAuth 식별값이라 수정 불가, `address`도 이번에 수정 대상에서 제외됨. `UserUpdateRequest`에서 `address` 필드를 제거(요청 본문에 보내도 무시됨)하고 `name`에 `@Size(max=255)`(User 컬럼 길이 기본값 기준)를 추가했다. `User.updateProfile(name, phone, address)` → `updateProfile(name, phone)`으로 시그니처 축소, `UserService.updateMe()`의 "최소 1개 필드 필요" 체크에서도 `address`를 뺐다. `GET /api/users/me` 응답에는 `address` 컬럼이 계속 노출됨(조회는 그대로, 수정만 막힘).
- 파일: `UserUpdateRequest.java`, `User.java`, `UserService.java`, `UserControllerTest.java`(`updateMeUpdatesPhoneAndAddress` → `updateMeUpdatesNameAndPhone`로 교체, `updateMeIgnoresAddressEvenWhenProvidedInRequestBody` 신규), `docs/api/user.md`(API 5 정정 노트)
- 테스트: 신규/수정 테스트를 구현 전 실패 확인 후 통과. User 도메인 18개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패. 전체 스위트 147개 중 동일하게 3건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: Request DTO 검증 점검 중 `address`만 blank 방어가 없어 조용히 지워질 수 있던 문제를 보고했고, 사람이 "address는 애초에 수정 대상이 아니어야 한다"로 범위를 확정하면서 문제가 자연히 해소됨.
- 관련: 없음

## 2026-08-08 — Claude — `main` (createGroup — User 자격 검증을 파일 업로드 이전으로 이동)

- 변경: `ApplicationService.createGroup()`이 존재 여부만 확인하는 `userService.findById(userId)`를 로고·직인·ZIP·멤버 사진 업로드가 모두 끝난 뒤(그것도 `try` 블록 밖)에서 호출하던 문제를 수정. 개인 신청(`createIndividual`)과 동일하게 메서드 최상단에서 `findUser(userId)`(=`findEligibleApplicationUser`, 탈퇴/권한/약관 동의까지 검증)를 호출하도록 이동했다. 이로써 ①탈퇴·비-USER role·약관 미동의 사용자의 단체 신청이 개인 신청과 동일하게 차단되고, ②User 검증 실패가 더 이상 `try` 블록 밖에서 발생하지 않아 이미 업로드된 S3 파일이 고아로 남는 문제도 함께 해소됨.
- 파일: `ApplicationService.java`, `ApplicationServiceBulkTest.java`(신규 3건 + 픽스처 `agreeTerms` 보강), `ApplicationBulkControllerTest.java`(픽스처 `agreeTerms` 보강)
- 테스트: 신규 3건(탈퇴/비-USER role/약관 미동의, 탈퇴 케이스는 `storageService` 미호출까지 검증)을 구현 전 실패 확인 후 통과. Application/API 도메인 133개 전체 통과, 전체 스위트는 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: 개인/단체 신청 간 신청 자격 검증 정책 불일치 및 리소스 누수 버그 발견 후 수정.
- 관련: 없음

## 2026-08-08 — Claude — `main` (Application Request DTO 입력값 검증 보강)

- 변경: `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 필드 검증을 DB 컬럼 길이·표준 Bean Validation 기준으로 보강. `@Size`(엔티티 컬럼 길이와 일치: name/zipCode/address/detailAddress/deliveryRequest/englishName/birthRegion/organizationName/department), `@Email`(applicant.email), `@Past`(member.birthDate)를 추가했다. 국적은 `data-model.md`(ISO 3166-1 alpha-2 확정 명시)와 `APPLICATION.md`(언급 없음) 간 문서 충돌을 발견해 보고한 뒤, 사람이 "ISO 코드 기준 관리"로 확정해 커스텀 `@ValidNationality`(`Locale.getISOCountries()` 기반)를 추가했다. 이 판정 로직(`ApplicationFieldFormats`)은 개인 신청 DTO와 `BulkExcelParser`(단체 신청 엑셀 행 파싱) 양쪽에서 재사용해 개인/단체 검증 정책이 갈라지지 않게 했다. 전화번호 형식(`@Pattern`)과 생년월일 최소연도 제한은 각각 국제 전화번호 정책 미확정, 비즈니스 근거 부재로 이번 범위에서 제외하고 `@NotBlank`/`@Past`만 유지했다.
- 파일: `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `BulkExcelParser.java`, `domain/application/dto/validation/`(신규 — `ApplicationFieldFormats`, `ValidNationality`, `NationalityValidator`), `ApplicationCreateRequestValidationTest.java`(신규), `BulkExcelParserTest.java`
- 테스트: 신규 테스트를 구현 전 실패 확인 후 통과. 기존 테스트 픽스처(`nationality: "US"`, `birthDate: "1990-05-15"` 등)가 이미 새 규칙과 호환돼 회귀 없음 확인.
- 사유: `Application.photoRejectReason` 관련 논의 중 발견한, 신청 단계 입력값 검증이 충분한지에 대한 점검 요청에 따른 보강.
- 관련: `docs/collab/PENDING_DECISIONS.md` "국제 전화번호 형식 정책" 항목(후속 확정 필요)

## 2026-08-08 — Codex — `main` (UploadFile DB 저장 트랜잭션 이동)

- 변경: 신청 생성 경로에서 S3 업로드와 `UploadFile` DB 저장 책임을 분리. `ApplicationService`는 로고·직인·제출 ZIP을 S3에 먼저 업로드하고 `UploadedFileMetadata`만 전달하며, `ApplicationPersistenceService`가 동일 `@Transactional` 안에서 `UploadFile` row를 저장한 뒤 `Application`/`Applicant`/`Receiver`/`ApplicationMember`를 저장하도록 변경. 얼굴사진/멤버사진은 기존처럼 S3 key(`photoPath`)만 저장하고 `UploadFile` row를 만들지 않는다. 재업로드 경로는 기존 동작을 유지하되 `uploadFileToStorage` + `saveUploadFileMetadata` primitive 조합으로 책임 이름을 분리했다.
- 파일: `ApplicationService.java`, `ApplicationPersistenceService.java`, `UploadedFileMetadata.java`(신규), `ApplicationPersistenceServiceTest.java`
- 테스트: `./gradlew.bat test --tests "com.example.honorcitizen.domain.application.service.ApplicationPersistenceServiceTest" --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceUploadCompensationTest"` 통과. `./gradlew.bat test --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceTest" --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceBulkTest" --tests "com.example.honorcitizen.domain.application.service.ApplicationServicePhotoReuploadTest"` 통과. 전체 `./gradlew.bat test`는 146개 중 `UserControllerTest` 2건, `UserApplicationFlowTest` 1건 실패 — 기존 사용자/Redis 환경 이슈로 이번 Application 변경과 무관.
- 사유: S3 객체는 DB 트랜잭션 밖에서 먼저 업로드하되, `UploadFile` row와 신청 관련 DB row는 하나의 트랜잭션으로 원자성을 보장하기 위함. DB 저장 실패 시에는 수동 DB 보상 삭제 없이 트랜잭션 rollback으로 정리하고, 바깥 서비스는 S3 key 역순 보상 삭제만 유지한다.
- 관련: UploadFile DB 저장 트랜잭션 이동 계획

## 2026-08-08 — Claude — `main` (GlobalExceptionHandler — Bean Validation 다중 필드 오류 응답)

- 변경: `MethodArgumentNotValidException` 처리 시 첫 번째 `FieldError`만 반환하던 것을 개선 — 위반된 모든 필드를 `ApiResponse.errors`(기존에 Bulk가 쓰던 `List<ValidationErrorDetail>` 필드를 그대로 재사용, 새 필드 추가 없음)에 담아 반환한다. `errors[]`는 `field` 기준으로 정렬해 `BindingResult` 내부 순서(스펙상 미보장)에 우연히 의존하지 않게 했다. 최상위 `errorMessage`는 하위 호환을 위해 기존과 동일하게 (정렬 전) 첫 번째 오류 메시지를 그대로 사용 — `errors[]` 정렬과 무관하게 유지. 중첩 DTO(`ApplicationCreateRequest.applicant.phone` 등) 경로는 Spring이 이미 `FieldError.getField()`에 점(.) 표기로 채워주므로 별도 처리 없이 그대로 노출됨. `ValidationErrorDetail.row`는 Bulk 전용 개념이라 이 경로에서는 항상 `null` — `PENDING_DECISIONS.md`에 후속 공통 오류 모델 정리 대상으로 기록.
- 파일: `GlobalExceptionHandler.java`, `GlobalExceptionHandlerTest.java`(신규 — 다중 필드 오류 시 `errors[]` 2건·중첩 경로·기존 `errorMessage` 호환성, 단일 필드 오류 시 기존과 동일한 단일-메시지 계약을 각각 검증)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. 전체 스위트 141개 중 `UserControllerTest` 2건(Redis 미기동)·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 전부 이번 변경 이전부터 있던 환경 문제이며 무관. 회귀 없음.
- 사유: Application 도메인 입력값 검증 작업 중 발견한 별도 이슈(클라이언트가 한 번의 요청으로 모든 필드 오류를 확인할 수 없음)를 앱 전체 공통 컴포넌트 변경으로 분리해 처리.
- 관련: `docs/collab/PENDING_DECISIONS.md` "GlobalExceptionHandler의 Bean Validation 다중 필드 오류 응답" 항목 해결

## 2026-08-07 — Claude — `main` (checklist.md §5 진행 상황 정리)

- 변경: `checklist.md` §5(미구현) 5개 항목을 실제 코드와 대조 — 4개는 §4 작업 과정에서 이미 구현 완료된 상태였음을 확인하고 `TODO.md`만 체크(코드 변경 없음, `checklist.md`는 수정하지 않음): `ApplicationPersistenceService` 신규(§4 "ApplicationPersistenceService 분리"), `BULK_APPLICATION_VALIDATION_FAILED`+`errors[]`(§4 "BulkExcelParser 학번 검증·errors[] 계약"), `application_seq.nextval` 채번(§4 "신청번호 DB Sequence 전환"), 업로드 추적 및 DB 실패 보상 삭제(§4 "업로드 보상 삭제" — `uploadedKeys` 추적 + 역순 `storageService.delete`가 §5 항목의 "확인 근거"였던 두 조건을 모두 충족). 남은 §5 항목은 "일일 KST 3회 제한 DB 원자 처리" 1건뿐 — 정책 문서에 "현재 리팩터링 범위 미구현"으로 명시된 저우선순위 항목.
- 파일: `docs/collab/TODO.md`
- 사유: `checklist.md` §4 작업이 §5의 상당 부분을 자연스럽게 해소했는지 실제로 확인하고 진행 상황을 정확히 반영.
- 관련: TODO "checklist.md §5 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — student_id 컬럼 길이 정리, §4 전체 완료)

- 변경: `checklist.md` §4 마지막 항목 구현 — `ApplicationMember.student_id` 컬럼을 `@Column(length = 50)` → `@Column(length = 10)`으로 변경. 개인 신청(`ApplicationService.isValidStudentId`)·단체 신청(`BulkExcelParser`) 양쪽 다 이미 10자·숫자만 통과시키므로 이 값을 넘는 값이 저장 경로에 도달할 수 없어 순수 스키마 정합성 정리. 이로써 `checklist.md` §4(수정 필요) 14개 행 전부 완료.
- 파일: `ApplicationMember.java`
- 테스트: 스키마 정의만 바뀌는 변경이라 신규 테스트 없이 Application/API 도메인 124개 전체 재실행으로 검증(`UserControllerTest` 2건, Redis 미기동, 무관만 실패) — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "학번 최대 10자·숫자 정책과 충돌한다."
- 관련: TODO "checklist.md §4 구현 진행" (§4 전체 완료, 다음은 §5 미구현 항목)

## 2026-08-07 — Claude — `main` (checklist.md §4 — Receiver 우편번호·기본주소 필수 검증)

- 변경: `checklist.md` §4 항목 구현 — `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `ReceiverRequest.zipCode`/`address`에 `@NotBlank` 추가(receiver 자체가 없으면 `@Valid`가 건너뛰므로 `MOBILE`엔 영향 없음). studentId 형식 검증은 개인 신청(`ApplicationService.isValidStudentId`, item6)과 단체 신청(`BulkExcelParser`, 직전 항목)에 이미 있어 DTO에 중복 추가하지 않음.
- 파일: `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `ApplicationControllerTest.java`(`createIndividualReturnsInvalidInputWhenReceiverZipCodeMissing` 신규), `ApplicationBulkControllerTest.java`(`createGroupReturnsInvalidInputWhenReceiverZipCodeMissing` 신규)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 124개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "Receiver 우편번호·기본주소 필수".
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — BulkExcelParser 학번 검증·errors[] 계약)

- 변경: `checklist.md` §4 나머지 항목(학번 검증·`ErrorCode`·`ApiResponse`·`BulkExcelParser` errors[])을 한 번에 구현 — 서로 강하게 얽혀 있어 하나로 묶음. 신규 `ValidationErrorDetail`(row·field·code·message) 레코드와 `BulkValidationException`(`CustomException` 상속) 추가. `ErrorCode`에 `BULK_APPLICATION_VALIDATION_FAILED` 추가, 미사용이 된 `ZIP_TOO_LARGE`/`EXCEL_NOT_FOUND`/`EXCEL_PARSE_ERROR` 제거(`APPLICATION_LIMIT_EXCEEDED`는 별도 §5 항목 몫이라 이번엔 추가 안 함). `ApiResponse`에 `errors` 필드(`@JsonInclude(NON_NULL)`) 추가. `GlobalExceptionHandler`에 `BulkValidationException` 전용 핸들러 추가. `BulkExcelParser.parseRow`는 필드별로 즉시 던지던 것을 `errors` 리스트에 수집하는 방식으로 바꿔 한 행이 잘못돼도 나머지 행을 계속 검사하고, 학번 형식(`\d{1,10}`) 검증도 추가. 엑셀 없음/2개 이상/데이터 없음도 동일한 `BulkValidationException` 계약으로 통일.
- 파일: `ValidationErrorDetail.java`(신규), `BulkValidationException.java`(신규), `ErrorCode.java`, `ApiResponse.java`, `GlobalExceptionHandler.java`, `BulkExcelParser.java`(전면 개편), `BulkExcelParserTest.java`(신규 3건 + 기존 3건 갱신), `ApplicationServiceBulkTest.java`(4건 갱신), `ApplicationBulkControllerTest.java`(1건 갱신)
- 테스트: 신규 테스트를 구현 전 컴파일 실패(신규 타입 부재) 확인 후 구현, 통과. Application/API 도메인 122개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "오류 하나라도 발생하면 부분 성공 없이 신청 전체를 실패 처리하고, 상세 오류를 errors[](행 번호·필드·코드·메시지)로 함께 반환한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — BulkExcelParser ZIP 루트·빈 행 정책)

- 변경: `checklist.md` §4 아홉 번째 항목 구현 — `BulkExcelParser.parse`가 ZIP 루트(경로에 `/`가 없는 항목)만 스캔하도록 변경: `.xlsx`는 후보로 모아 0개면 `EXCEL_NOT_FOUND`, 2개 이상이면 전체 실패(`EXCEL_PARSE_ERROR`)로 처리(기존엔 첫 `.xlsx`만 조용히 사용하고 나머지·하위 폴더 엑셀을 허용했음). 사진은 `photos/` 하위 대신 ZIP 루트에서 파일명으로 매칭(`__MACOSX/...`는 루트가 아니라 자동 제외, `.DS_Store`는 파일명으로 명시 무시). `parseExcel`은 ID가 빈 행에서 `break`해서 이후 행을 통째로 버리던 것을, 시트 마지막 행까지 순회하며 빈 ID 행만 `continue`로 건너뛰도록 변경 — 중간 빈 행 뒤의 유효한 데이터도 이제 정상적으로 읽힘.
- 파일: `BulkExcelParser.java`(parse/parseExcel, isRootEntry/isIgnoredEntry 신규), `BulkExcelParserTest.java`(신규 — 6개 테스트: ZIP 루트 사진 매칭, 하위 폴더 사진 무시, 엑셀 2개 이상 거부, 하위 폴더 엑셀 무시, `__MACOSX`/`.DS_Store` 무시, 중간·마지막 빈 행 무시), `ApplicationServiceBulkTest.java`/`ApplicationServicePhotoReuploadTest.java`/`ApplicationServiceUploadCompensationTest.java`/`ApplicationBulkControllerTest.java`(기존 zip 픽스처의 `photos/` 접두사를 ZIP 루트로 이동)
- 테스트: 신규 `BulkExcelParserTest` 6건을 구현 전 실패 확인(1건은 우연히 다른 이유로 이미 실패 상태였던 것 확인) 후 전부 통과. Application/API 도메인 120개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "Excel은 ZIP 루트에 정확히 1개, 2개 이상 전체 실패, 사진은 ZIP 루트에서 매칭한다", "중간 빈 행과 마지막 빈 행을 무시해야 한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 신청번호 DB Sequence 전환)

- 변경: `checklist.md` §4 여덟 번째 항목 구현 — 신청번호 채번을 `count+1`에서 진짜 DB Sequence(`application_seq`)로 교체. Hibernate `@SequenceGenerator`를 엔티티 ID 생성에 실제로 연결하지 않으면 ddl-auto가 시퀀스를 만들어주지 않는 것을 테스트로 확인해서, `schema.sql`에 `CREATE SEQUENCE IF NOT EXISTS application_seq`를 직접 선언하고 `spring.jpa.defer-datasource-initialization=true`+`spring.sql.init.mode=always`로 Hibernate DDL 이후 실행되게 설정. `ApplicationService.generateApplicationNumber`는 `EntityManager` native query(`SELECT nextval('application_seq')`)로 채번. 더 이상 쓰이지 않는 `ApplicationRepository.countByApplicationNumberStartingWith`는 제거(§4의 별도 "count+1 정리" 항목도 함께 해소).
- 파일: `application.properties`(schema init 설정 추가), `schema.sql`(신규), `Application.java`(미사용 `@SequenceGenerator` 시도 후 제거), `ApplicationService.java`(generateApplicationNumber, nextApplicationSequence 신규, EntityManager 필드 추가), `ApplicationRepository.java`(countByApplicationNumberStartingWith 제거), `ApplicationServiceTest.java`(`generateApplicationNumberNeverReusesSequenceEvenAfterExistingApplicationsAreDeleted` 신규)
- 테스트: 신규 테스트를 구현 전 실패 확인(처음엔 시퀀스 미생성으로 다른 이유로도 실패해서 원인 재확인 후 schema.sql 방식으로 수정) 후 통과. 전체 테스트 123개 중 `UserControllerTest` 2건 + `UserApplicationFlowTest` 1건(모두 Redis 미기동, 무관)만 실패 — 회귀 없음. `application.properties`를 건드린 변경이라 이번엔 도메인 범위가 아닌 전체 테스트 스위트로 재확인함.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "`count+1` 금지, `application_seq.nextval` 기반 DB Sequence 사용".
- 관련: TODO "checklist.md §4 구현 진행"

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
