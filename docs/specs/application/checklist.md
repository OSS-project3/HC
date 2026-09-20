# Application Policy Sync and Code Audit

> ⚠️ **정정(2026-08-25):** "일일 KST 3회 제한 / `APPLICATION_LIMIT_EXCEEDED` 미구현(범위 밖)"은 이후 **구현됐다**(2026-08-16) — `ApplicationDailyLimit`(엔티티/Service/Repository) + `ErrorCode.APPLICATION_LIMIT_EXCEEDED`(429). `POLICY_SYNC_CHECKLIST.md`의 동일 "미구현" 표기도 낡음.

## 2026-08-08 Failure-path and Contract Verification Summary

- 생성 경로: `createIndividual()`/`createGroup()`의 S3 업로드 중간 실패와 DB 저장 실패 모두에서 신규 S3 key를 역순 보상 삭제하도록 검증했다.
- 재업로드 경로: 신규 S3 파일은 transaction rollback/unknown 상태에서 보상 삭제하고, 기존 S3 파일은 DB commit 이후에만 삭제하도록 검증했다.
- API/validation 계약: multipart part 이름, Receiver 정책, 학생증/일반카드 로고·직인 정책, Bean Validation `errors[]`, Bulk `row/field/code/message` 응답 계약을 기존 테스트로 확인했다.
- Bulk Excel 계약: Excel ID는 텍스트 기반 식별자로 고정했고, `1`과 `001`은 다른 ID로 처리하며 leading zero 자동 보정은 하지 않는다.
- Bulk parser edge-case: 중복 ID, 중복 사진, 누락/여분 사진, 대소문자 확장자, 빈 Excel/빈 행, macOS 부산물, 하위 디렉터리, 잘못된 ZIP/Excel 계약을 확인했다.
- 소비 경로: 현재 구현된 조회, 카드 다운로드, 사진 반려 후 재업로드 경로를 확인했다. 별도 Admin Application API와 카드 미리보기/생성 API는 현재 구현되어 있지 않아 후속 구현 시 별도 계약 테스트가 필요하다.

## 2026-08-09 Redis Retry Verification

- Redis 기동 후 기존 전체 테스트 실패 3건을 재실행했다.
- `UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken`, `UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall`은 통과했다. 이전 실패 원인은 Redis 미기동에 따른 `RedisConnectionFailureException`이었다.
- `UserApplicationFlowTest.fullUserApplicationFlow`는 Redis 연결 실패가 해소됐지만, 신청 생성 단계에서 `TERMS_NOT_AGREED` 403으로 실패했다.
- 남은 실패는 Redis 환경 문제가 아니라 테스트 플로우가 현재 신청 정책(신청 전 필수 약관 동의)을 반영하지 못한 상태로 분류한다.

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

## 6. 학생증 앞·뒷면 텍스트 색상 선택

> ✅ 2026-09-19 구현 완료(Claude) — 아래 체크박스를 실제 코드·테스트 기준으로 갱신했다. 프론트 전달 문서화는 2026-09-20에 완료(`FRONTEND_API_INTEGRATION_SPEC.md`/`FRONTEND_API_GAPS.md`); 실제 프론트 UI·API 연결 코드만 이번 범위 밖으로 남아있다.

### 확정 정책

- [x] 적용 카드 종류는 `STUDENT`로 한정한다. 다른 카드 종류의 렌더링 색상은 변경하지 않는다.
- [x] 관리자는 학생증 앞면과 뒷면의 텍스트 색상을 각각 독립적으로 선택할 수 있어야 한다.
- [x] 각 면에서 허용하는 값은 `DARK_GRAY`, `WHITE` 두 가지뿐이다.

- [x] 앞면 선택값은 앞면에 서버가 동적으로 그리는 모든 텍스트에 일괄 적용한다.
- [x] 뒷면 선택값은 뒷면에 서버가 동적으로 그리는 모든 텍스트에 일괄 적용한다.
- [x] 배경 템플릿 이미지에 이미 포함된 글자, 사진, 로고, 직인 및 십이간지 이미지는 색상 변경 대상이 아니다.
- [x] 개인·단체 신청 모두 신청(`Application`) 단위로 앞면 색상 1개와 뒷면 색상 1개를 사용한다. 단체 구성원별로 서로 다른 색상을 허용하지 않는다.
- [x] 기존 신청과 값이 누락된 요청은 하위 호환을 위해 앞면·뒷면 모두 `DARK_GRAY`로 해석한다.
- [x] 카드 미리보기와 실제 카드 생성은 반드시 동일한 색상 해석 로직을 사용한다(`CardRenderPreparation`/`CardGenerationPersistenceService` 둘 다 `resolveStudentTextColor` 동일 로직, 후자는 커밋 직전 재검증).

### 구현 체크리스트

- [x] `StudentTextColor` enum을 추가하고 허용값을 `DARK_GRAY`, `WHITE`로 제한한다.
- [x] `Application`에 `studentFrontTextColor`, `studentBackTextColor` nullable enum 필드를 추가한다. 기존 row의 `null`은 `DARK_GRAY`로 해석하고 기존 데이터를 일괄 갱신하지 않는다.
- [ ] 개발 DB의 `ddl-auto=update` 및 운영 DB migration에서 컬럼이 안전하게 추가되는지 확인한다 — ⚠️ 별도 확인 안 함(zodiacDesignSet 등 기존 필드와 동일하게 별도 migration 파일 없이 엔티티 필드만 추가하는 이 프로젝트 관행을 그대로 따랐을 뿐, 운영 배포 시 실제 컬럼 추가 여부는 미검증).
- [x] 기존 카드 미리보기·생성 요청(`CardPreviewRequest`)에 `studentFrontTextColor`, `studentBackTextColor`를 추가한다. 별도 색상 저장 전용 API는 만들지 않는다.
- [x] 기존 클라이언트 호환을 위해 두 요청 필드는 optional로 받고, 둘 중 하나만 누락되면 누락된 면만 `DARK_GRAY`로 해석한다.
- [x] 비학생증 신청이 두 색상 필드 중 하나라도 명시적으로 전송하면 잘못된 요청 조합으로 거절한다(기존 `INVALID_INPUT` 재사용, 신규 코드 안 만듦).
- [x] `CardRenderPreparation`에서 카드 종류, 요청값, 기존 확정값을 검증하고 앞·뒤 유효 색상을 한 번만 결정한다.
- [x] `CardMemberData`에 앞면·뒷면 색상을 전달하되 기존 비학생증 생성자와 테스트 fixture가 불필요하게 깨지지 않도록 기본값 경로를 유지한다(기존 21-인자 생성자를 compat 오버로드로 보존, 새 23-인자가 canonical).

- [x] `CardImageCompositor.composeStudentFront()`는 앞면 선택값을 이름·영문명·학번·학과·생년월일·발급일자 등 모든 동적 텍스트에 적용한다.
- [x] `CardImageCompositor.composeStudentBack()`는 뒷면 선택값을 제목·이름·영문명·한자·뜻·풀이 및 줄바꿈 텍스트 전체에 적용한다.
- [x] `CardGenerationPersistenceService`의 카드 생성 성공 트랜잭션에서 앞·뒤 색상을 `Application`에 함께 저장한다. 렌더링 또는 S3 업로드·DB 저장이 실패하면 색상만 먼저 확정되어서는 안 된다(기존 트랜잭션 경계를 그대로 재사용 — 별도 롤백 로직 추가 안 함).
- [x] 같은 신청의 다른 구성원을 생성할 때 이미 저장된 앞·뒤 색상과 다른 요청은 거절하여 단체 카드의 색상을 통일한다.
- [x] 카드가 이미 생성된 신청에서 다른 색상으로 바꾸는 기능은 기존 이미지 전체 재생성·교체 정책 없이는 허용하지 않는다. 같은 색상으로 기존 재생성하는 동작은 유지한다.
- [x] 관리자 신청 상세 응답(`MyApplicationDetailResponse`, 관리자·사용자 공용)에 `studentFrontTextColor`, `studentBackTextColor`를 포함해 새로고침 후 선택값을 복원할 수 있게 한다.
- [x] `MyApplicationDetailResponse.withTranslated()` 등 상세 응답을 재조립하는 경로에서 두 필드가 누락되지 않게 전달한다.
- [x] 확정 색상과 다른 생성 요청을 프론트가 구분할 필요가 있으므로 전용 오류 코드(`STUDENT_TEXT_COLOR_MISMATCH`)를 신설했다(비학생증 거절은 기존 `INVALID_INPUT` 재사용 — "재사용 가능하면 재사용" 원칙, 이 경우는 기존 코드 재사용 시 메시지가 실제 문제와 달라 오해를 유발해 신설).
- [x] 프론트 전달 문서에 신청 단위 선택 UI·학생증 전용 노출·생성 후 잠금 조건을 기록한다 — `docs/FRONTEND_API_INTEGRATION_SPEC.md`("카드 제작" §, 2026-09-20 추가)와 `docs/FRONTEND_API_GAPS.md`(P2 신규 갭)에 기록 완료. 실제 UI·API 연결 코드는 여전히 이번 범위 밖(프론트 작업).
- [x] 기존 `zodiacDesignSet` 선택 UI와 같은 영역 배치 — 배치 조건을 위 문서에 명시함. 실제 UI 코드는 이번 범위 밖(프론트 작업).

### 테스트 및 검증 체크리스트

> `StudentTextColorTest.java`(신규, 7개), `CardImageCompositorTest.java`(신규 3개) 기준. 렌더링 픽셀 자체의 정확한 색상 검증은 이 문서 상단 파일들의 기존 관행(텍스트 렌더링은 자동 픽셀 검증이 어려워 구조 검증만 자동화하고 가독성은 육안 확인)을 따라 자동화하지 않았다.

- [x] 요청에서 두 색상을 모두 생략하면 학생증 앞·뒷면이 `DARK_GRAY`로 해석된다(`previewDefaultsToDarkGrayWhenColorsOmitted` — 해석 로직 검증, 픽셀 확인 아님).
- [~] `DARK_GRAY/DARK_GRAY`, `DARK_GRAY/WHITE`, `WHITE/DARK_GRAY`, `WHITE/WHITE` 네 조합 — `WHITE/DARK_GRAY` 조합만 명시 테스트(`previewAcceptsExplicitWhiteFrontAndDarkGrayBackIndependently`), 나머지 3개는 별도 테스트 없음(로직상 대칭이라 위험은 낮다고 판단).
- [~] 앞면 색상을 바꿔도 뒷면 결과는 바뀌지 않고, 뒷면도 마찬가지 — 코드 구조상 성립(각 면이 독립된 `resolveStudentTextColor` 호출과 별도 `textColor` 변수 사용), 별도 회귀 테스트는 없음.
- [ ] 각 면의 모든 동적 텍스트가 선택 색상을 쓰는지, 배경·사진·로고·십이간지 이미지 픽셀이 불변인지 — 자동 검증 안 함(위 사유). 코드 리뷰로 각 `drawXxx` 호출부가 `textColor` 변수를 쓰도록 전부 교체했음은 확인.
- [x] 학생증이 아닌 카드의 기존 렌더링·API 계약 회귀 없음(`nonStudentCardIgnoresTextColorFieldsAndRendersUnchanged`, 기존 전체 회귀 929/930 통과).
- [x] 비학생증 요청에 색상 필드를 보내면 거절(`previewRejectsColorFieldsForNonStudentCard`).
- [x] 최초 카드 생성 성공 후 `Application`에 앞·뒤 색상 저장(`generateConfirmsAndPersistsColorsOnApplication`) — 상세 응답 반환 자체는 필드 존재로 자명, 별도 HTTP 레벨 테스트는 없음.
- [x] 단체 신청 다른 구성원 다른 색상 거절/동일 색상 성공(`generateAllowsSecondGroupMemberOnlyWithMatchingColor`).
- [x] 같은 색상으로 재생성 허용(`generateAllowsRegenerationWithSameConfirmedColor`) — S3 교체·DB 갱신 자체는 기존 `CardGenerationServiceTest`의 재생성 테스트가 이미 커버(색상 로직이 그 경로를 변경하지 않음).
- [ ] 렌더링/S3 업로드/DB 실패 시 색상 확정과 카드 경로 저장이 함께 롤백 — 별도 테스트 없음(기존 트랜잭션 경계·기존 보상삭제 로직을 그대로 재사용했고 기존 `CardGenerationServiceTest` 실패 테스트들이 이미 그 경로를 검증).
- [ ] 동시에 서로 다른 색상으로 단체 구성원 생성 요청 시 트랜잭션 경계 검증 — 별도 트랜잭션 두 개짜리 동시성 테스트 없음(이 프로젝트의 다른 동시성 항목들과 동일하게 후속 강화로 남김).
- [~] 관리자 상세 응답 일반/영어 재조립 경로 모두 보존 — 코드는 두 경로 다 전달하지만(`withTranslated()`), 직접 HTTP 레벨 assertion 테스트는 없음.
- [x] 관련 집중 테스트(929/930, 무관 플레이키 1건 제외), `compileJava`/`compileTestJava` 통과.

## 7. Legacy

- 실제 구현에서 확인된 Legacy 항목은 없다.
- `APPLICATION.md`가 과거 정책으로 언급한 30% 부분 성공 로직은 `src/main`과 `src/test`에서 확인되지 않았다.
- **Legacy 후보(미사용 메서드, 2026-08-08 신규 발견):** `Receiver.copyFromApplicant(Long, Applicant)`와 이를 감싸는 `ApplicationFactory.copyIndividualReceiver`는 정의만 있고 운영 저장 경로(`ApplicationPersistenceService.saveReceiverIfNeeded`/`saveGroupReceiverIfNeeded`)에서는 호출되지 않는다(grep 기준 호출부는 `ReceiverTest.java` 단위 테스트뿐). `copyFromApplicant`는 `zipCode`/`address`를 `null`로 채우므로, 다시 호출되게 되면 "배송지는 항상 Receiver 입력" 정책과 충돌한다. 삭제하거나 실제로 쓰이지 않는 이유를 문서화할지 판단이 필요하다.
