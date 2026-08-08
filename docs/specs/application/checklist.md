# Application Policy Sync and Code Audit

> ✅ 2026-08-08 검증: API/validation 계약은 기존 테스트로 충분히 보장됨을 확인했다. `ApplicationControllerTest`, `ApplicationBulkControllerTest`, `GlobalExceptionHandlerTest`, `ApplicationServiceTest`, `ApplicationServiceBulkTest`가 multipart part, Receiver 양방향 정책, 학생증/일반카드 로고·직인 정책, Bean Validation `errors[]`, Bulk `row/field/code/message` 계약을 검증한다. 중복 신규 테스트는 추가하지 않았다.
> ✅ 2026-08-08 검증: Bulk parser edge-case는 `BulkExcelParserTest`로 검증한다. 텍스트 ID/숫자 ID 매칭, 중복 사진, 여분 사진, 중복 ID, 대소문자 확장자, 사진 누락, 빈 Excel, 중간·마지막 빈 행, `__MACOSX`, `.DS_Store`, 하위 디렉터리, Excel 중복/누락, 읽을 수 없는 Excel 계약을 포함한다.
> ✅ 2026-08-08 검증: Application 이후 소비 경로는 현재 구현된 `lookup`, `cards/download`, `photo reupload` 범위에서 확인했다. `ApplicationServiceLookupTest`, `ApplicationServiceCardDownloadTest`, `ApplicationCardDownloadControllerTest`, `ApplicationPhotoControllerTest`, `ApplicationServicePhotoReuploadTest`가 조회, 카드 다운로드, `PHOTO_REJECTED → REVIEWING` 재제출 상태 복귀를 검증한다. 별도 Admin Application API와 카드 미리보기/생성 API는 현재 `src/main`에 구현되어 있지 않아 테스트 고정 대상에서 제외했다.

> `APPLICATION.md`의 확정 정책과 `POLICY_SYNC_CHECKLIST.md`의 검증 기준을 적용한 결과다. 코드는 수정하지 않았으며 실제 구현에서 확인한 내용만 기록한다.
> ⚠️ 2026-08-07: 학생증 `department`(학과) 필드 제외는 이 문서에 반영하지 않았다 — `APPLICATION.md`에 근거가 없고 사람이 미결정으로 확인했다(`PENDING_DECISIONS.md` 참고). department를 "충돌"로 지목했던 원본 Audit 항목은 아래에서 "정책과 일치"로 재분류하거나 관련 설명만 제거했다.
> ✅ 2026-08-08 재감사: 아래 07-07 시점 Audit의 "수정 필요" 12건, "미구현" 5건을 실제 코드(`ApplicationService`, `ApplicationPersistenceService`, `BulkExcelParser`, DTO, Entity, `ErrorCode`, 테스트 전체)와 한 줄씩 다시 대조했다. 그 사이 구현이 진행되어 11건은 이미 정책과 일치하는 상태였고, "미구현" 5건 중 4건도 이미 구현되어 있었다. 아래 3~6절을 이 재감사 결과로 갱신한다.
> ✅ 2026-08-08 추가: `ApplicationCreateRequest`/`BulkApplicationCreateRequest`에 표준 Bean Validation(`@Size`는 DB 컬럼 길이 기준, `@Email`, `@Past`)을 보강했다. 국적은 `data-model.md`(ISO 3166-1 alpha-2 확정 명시)와 이 문서(언급 없음) 간 충돌이 있었는데, 자유 문자열 저장 대신 ISO 코드 기준 관리로 확정해 `@ValidNationality`(커스텀, `Locale.getISOCountries()` 기반) 검증을 추가했다 — 개인 신청 DTO와 `BulkExcelParser` 행 파싱 양쪽에 동일 로직(`ApplicationFieldFormats`)을 적용해 정책이 갈라지지 않게 했다. `birthDate`는 `@NotNull` + `@Past`(표준)만 적용하고 별도 최소연도 제한은 근거가 없어 추가하지 않았다. `phone` 형식 검증은 국제번호 정책 미확정으로 보류(`PENDING_DECISIONS.md` 참고), `@NotBlank`(필수 여부)만 유지한다.

## 1. 문서 동기화

- [x] requirements.md: Receiver, ZIP, 단체 오류, 파일, 트랜잭션, 상담, 일일 제한, 신청번호, 멱등성, 학생증(학번), Applicant, Payment 정책 반영
- [x] data-model.md: Receiver, Applicant 이메일, 학생증(학번), UploadFile, Payment 데이터 정책 반영
- [x] api.md: Receiver 오류, BULK_APPLICATION_VALIDATION_FAILED + errors[], ZIP 및 학생증 계약 반영
- [x] TBD는 PENDING_DECISIONS.md에만 집약
- [x] 부분 성공 등 현재 정책과 충돌하는 문서 표현 제거(학과 필수는 미결정 상태로 유지)

## 2. Code Audit 기준

- 정책과 일치: 파일 책임 범위의 실제 구현이 정책과 일치한다.
- 수정 필요: 실제 구현이 확정 정책과 충돌한다.
- 미구현: 저장소 검색과 호출 경로 확인 결과 확정 정책 구현이 없다.
- Legacy: `APPLICATION.md`가 과거 정책으로 특정한 구현이 실제 남아 있다.

## 3. 정책과 일치

| 파일 | 클래스 / 메서드 | 실제 구현 근거 |
|---|---|---|
| ApplicationPhotoValidator.java | ApplicationPhotoValidator / validateFacePhoto, validateSchoolAsset | 얼굴사진과 학교 파일의 형식·내용 검증을 업로드 전에 수행한다. |
| ApplicationFactory.java | ApplicationFactory / 생성 메서드 | Application, Applicant, Receiver, ApplicationMember 생성 책임을 분리한다. |
| Applicant.java | Applicant / createIndividual, createGroup | 신청 시점 Applicant 값을 별도 Entity로 저장한다. |
| Receiver.java | Receiver / copyFromApplicant, create | 신청인 복사 메서드는 이름·연락처를 복사하고 주소를 복사하지 않는다. |
| ApplicantRepository.java, ApplicationMemberRepository.java, ReceiverRepository.java | 각 Repository | 각 Entity의 저장·조회 책임만 제공하며 확인된 정책 충돌이 없다. |
| 조회·다운로드 DTO와 Service/Controller, 관련 테스트 | 각 클래스의 lookup, getCardDownload 경로 | 이번 Source of Truth의 생성 정책과 충돌하는 실제 동작이 확인되지 않았다. |
| ApplicationMember.java | ApplicationMember / department 필드·인자 | ✅ 2026-08-07 재분류: 학과 필드 저장은 현재 정책(유지, 미결정)과 일치한다 — 원래 "수정 필요"로 분류됐던 항목을 여기로 옮김. |
| ApplicationMemberTest.java | studentCardCarriesStudentIdAndDepartment (59-66) | ✅ 2026-08-07 재분류: 학생증 Entity가 학과를 저장하는 것을 검증하는 테스트는 현재 정책과 일치한다. |
| api/ApplicationController.java, service/ApplicationService.java | createGroup의 `seal` `@RequestPart(required=false)` + 학생증 분기 | ✅ 2026-08-08 재분류: 학생증은 `seal` 생략을 허용하고, 비학생증은 여전히 필수로 검사한다 — "학교 직인은 선택" 정책과 일치. |
| service/ApplicationService.java, service/ApplicationPersistenceService.java | ApplicationService(비트랜잭션) → ApplicationPersistenceService.saveIndividual/saveGroup(`@Transactional`) | ✅ 2026-08-08 재분류: 별도 `@Service`로 분리되어 있고 Spring 자기호출 없이 Bean 간 호출로 구성된다 — 트랜잭션 구조 정책과 일치. |
| service/ApplicationService.java | validateReceiverPresence, validateGroupReceiverPresence | ✅ 2026-08-08 재분류: `MOBILE_AND_PHYSICAL` + Receiver 없음, `MOBILE` + Receiver 있음 두 방향 모두 `INVALID_INPUT`으로 거절한다. |
| service/ApplicationPersistenceService.java | saveReceiverIfNeeded, saveGroupReceiverIfNeeded | ✅ 2026-08-08 재분류: 요청값이 있으면 그대로, 없을 때만 신청인 이름·연락처로 fallback한다(수정 가능). 우편번호·기본주소는 항상 요청값을 그대로 저장해 배송지 필수 정책과 일치한다. `Receiver.copyFromApplicant`는 이 경로에서 더 이상 호출되지 않는다(6절 Legacy 참고). |
| service/ApplicationService.java | createIndividual, createGroup의 applicantEmail 계산 | ✅ 2026-08-08 재분류: `hasText(request.getApplicant().getEmail())`이면 요청값, 아니면 `user.getEmail()`을 사용한다 — `User.email` 기본값 + 수정 가능 정책과 일치. |
| service/ApplicationService.java | validateStudentFields, isValidStudentId | ✅ 2026-08-08 재분류: 직인은 `isPresent(schoolSeal)`일 때만 검증(선택), 학번은 `\d{1,10}` 정규식으로 최대 10자·숫자만 허용한다. |
| service/ApplicationService.java | createGroup의 logo/seal 필수 조건 (`!isPresent(logo) \|\| (!isStudent && !isPresent(seal))`) | ✅ 2026-08-08 재분류: 학생증은 logo만 필수, 그 외 카드종류는 logo·seal 모두 필수 — §13 정책과 일치. |
| service/ApplicationService.java | uploadedKeys 추적 + deleteUploadedFilesReversed | ✅ 2026-08-08 재분류: 생성 흐름에서 저장 실패 시 그 요청에서 업로드한 파일을 역순으로 삭제하는 보상 로직이 구현되어 있다. |
| service/ApplicationService.java | generateApplicationNumber, nextApplicationSequence | ✅ 2026-08-08 재분류: `entityManager.createNativeQuery("SELECT nextval('application_seq')")`로 DB Sequence 기반 채번을 사용한다. `count+1` 방식과 `ApplicationRepository.countByApplicationNumberStartingWith`는 코드에서 완전히 제거됐다. |
| service/BulkExcelParser.java | parse, isRootEntry | ✅ 2026-08-08 재분류: ZIP 루트(`!name.contains("/")`)의 `.xlsx`만 후보로 모으고, 하위 폴더 Excel/사진은 자동으로 제외된다. 루트에 `.xlsx`가 2개 이상이면 `EXCEL_DUPLICATE`로 전체 실패한다. |
| service/BulkExcelParser.java | parseExcel | ✅ 2026-08-08 재분류: ID가 빈 행은 `continue`로 건너뛴다 — 중간 빈 행과 마지막 빈 행 모두 무시하는 정책과 일치한다. |
| service/BulkExcelParser.java | parseRow, isValidStudentId | ✅ 2026-08-08 재분류: 학번 형식(`\d{1,10}`)을 검증하고, 행별 오류를 모두 모아 `BulkValidationException`으로 한 번에 던진다(첫 오류에서 즉시 중단하지 않음). |
| common/exception/ErrorCode.java | ErrorCode | ✅ 2026-08-08 재분류: `BULK_APPLICATION_VALIDATION_FAILED`가 존재하며, `EXCEL_NOT_FOUND`/`EXCEL_PARSE_ERROR`/`ZIP_TOO_LARGE`는 더 이상 정의되어 있지 않다. |
| common/response/ApiResponse.java, common/exception/GlobalExceptionHandler.java | ApiResponse.fail(errorCode, message, errors), handleBulkValidationException | ✅ 2026-08-08 재분류: `ApiResponse`에 `errors` 필드가 있고, `BulkValidationException` 전용 핸들러가 `errors[]`를 포함해 응답한다. |
| entity/ApplicationMember.java | studentId 컬럼 (`@Column(length = 10)`) | ✅ 2026-08-08 재분류: 학번 컬럼 길이가 10으로 정책과 일치한다. |
| ApplicationServiceBulkTest.java, BulkExcelParserTest.java, ApplicationBulkControllerTest.java | 각 테스트의 `BULK_APPLICATION_VALIDATION_FAILED` 단언 | ✅ 2026-08-08 재분류: 세 테스트 파일 모두 `BULK_APPLICATION_VALIDATION_FAILED` 기준으로 검증하며 `EXCEL_PARSE_ERROR` 참조는 남아있지 않다. |

## 4. 수정 필요

(2026-08-08 재감사 결과 없음 — 07-07 시점에 기록됐던 12건은 모두 3절 "정책과 일치"로 재분류됐다.)

## 5. 미구현

| 정책 기능 | 확인 근거 |
|---|---|
| 일일 KST 3회 제한의 DB 원자 처리 | `APPLICATION_LIMIT_EXCEEDED`와 제한 조회·원자 처리 구현이 없다. `APPLICATION.md` §7이 현재 범위에서 구현하지 않는다고 명시했으므로 정책과 일치하는 상태다. |

(2026-08-08 재감사: 07-07 시점 5건 중 `ApplicationPersistenceService` 분리, `BULK_APPLICATION_VALIDATION_FAILED` + errors[], `application_seq.nextval` 채번, 업로드 추적/보상 삭제 4건은 이미 구현되어 3절로 이동했다.)

## 6. Legacy

- 실제 구현에서 확인된 Legacy 항목은 없다.
- `APPLICATION.md`가 과거 정책으로 언급한 30% 부분 성공 로직은 `src/main`과 `src/test`에서 확인되지 않았다.
- **Legacy 후보(미사용 메서드, 2026-08-08 신규 발견):** `Receiver.copyFromApplicant(Long, Applicant)`와 이를 감싸는 `ApplicationFactory.copyIndividualReceiver`는 정의만 있고 운영 저장 경로(`ApplicationPersistenceService.saveReceiverIfNeeded`/`saveGroupReceiverIfNeeded`)에서는 호출되지 않는다(grep 기준 호출부는 `ReceiverTest.java` 단위 테스트뿐). `copyFromApplicant`는 `zipCode`/`address`를 `null`로 채우므로, 다시 호출되게 되면 "배송지는 항상 Receiver 입력" 정책과 충돌한다. 삭제하거나 실제로 쓰이지 않는 이유를 문서화할지 판단이 필요하다.
