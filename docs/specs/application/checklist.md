# Application Policy Sync and Code Audit

> `APPLICATION.md`의 확정 정책과 `POLICY_SYNC_CHECKLIST.md`의 검증 기준을 적용한 결과다. 코드는 수정하지 않았으며 실제 구현에서 확인한 내용만 기록한다.
> ⚠️ 2026-08-07: 학생증 `department`(학과) 필드 제외는 이 문서에 반영하지 않았다 — `APPLICATION.md`에 근거가 없고 사람이 미결정으로 확인했다(`PENDING_DECISIONS.md` 참고). department를 "충돌"로 지목했던 원본 Audit 항목은 아래에서 "정책과 일치"로 재분류하거나 관련 설명만 제거했다.

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

## 4. 수정 필요

| 파일 / 클래스 / 메서드(라인) | 현재 구현 | 충돌하는 APPLICATION.md 정책 / 판단 근거 |
|---|---|---|
| api/ApplicationController.java / ApplicationController.createGroup (49-58) | seal을 필수 `@RequestPart("seal")`로 받는다. | 학생증 학교 직인은 선택이다. 학생증 단체 신청도 Controller에서 직인이 없으면 거절되므로 충돌한다. |
| service/ApplicationService.java / createIndividual, createGroup (63-78, 125-168) | 두 생성 메서드 자체가 `@Transactional`이며 검증·업로드·DB 저장을 한 Service에서 수행한다. | `ApplicationService`는 비트랜잭션 오케스트레이터이고 별도 `ApplicationPersistenceService.save()`가 트랜잭션 저장을 담당해야 한다. |
| 같은 파일 / validateReceiverPresence, validateGroupReceiverPresence (325-329, 368-372) | `MOBILE_AND_PHYSICAL`의 Receiver 누락만 거절하고 `MOBILE`에 Receiver가 전달된 경우는 거절하지 않는다. | `MOBILE` + Receiver는 `INVALID_INPUT`이어야 한다. |
| 같은 파일 / saveReceiverIfNeeded, saveGroupReceiverIfNeeded (331-342, 402-413) | `sameAsApplicant=true`이면 요청 Receiver의 이름·연락처·배송지를 사용하지 않고 `copyFromApplicant` 결과를 저장한다. | 이름·연락처는 자동 복사 후 수정 가능해야 하고 배송지는 Receiver가 항상 입력해야 한다. |
| 같은 파일 / createIndividualApplicant, createGroup (107-112, 149-155) | 요청 email 필드 없이 항상 `user.getEmail()`을 저장한다. | `User.email`은 기본값이며 신청 화면에서 수정 가능해야 한다. |
| 같은 파일 / validateStudentFields (375-391) | 직인(`schoolSeal`)까지 필수로 검사하고 학번의 최대 10자·숫자 형식을 검사하지 않는다. | 직인은 선택, 학번은 필수·최대 10자·숫자만 허용한다. (학과 필수 검사는 현재 정책과 일치 — 3절 참고) |
| 같은 파일 / createGroup (131-140) | 카드종류와 무관하게 `logo`와 `seal`을 모두 필수 검사한다. | 학생증은 로고만 필수이고 직인은 선택이다. |
| 같은 파일 / storePhotoBytes, storePhotoFile, storeUploadFile, reuploadPhoto (188-209, 344-349, 415-429) | 업로드 직후 DB를 저장하며 실패 시 업로드 파일을 역순 삭제하는 호출이 없다. 재업로드 후 기존 파일도 삭제하지 않는다. | DB 실패 시 요청 업로드 파일 역순 보상 삭제, 파일 수정 시 DB 갱신 성공 후 기존 파일 삭제가 필요하다. |
| 같은 파일 / generateApplicationNumber (439-444) | 연도 prefix 건수를 조회해 `count + 1`로 신청번호를 만든다. | `count+1` 금지, `application_seq.nextval` 기반 DB Sequence 사용 정책과 충돌한다. |
| service/BulkExcelParser.java / parse (46-76) | 첫 `.xlsx`만 읽고 추가 Excel을 무시하며 하위 경로 Excel도 허용한다. 사진은 `photos/` 하위에서만 수집한다. | Excel은 ZIP 루트에 정확히 1개, 2개 이상 전체 실패, 사진은 ZIP 루트에서 매칭한다. |
| 같은 파일 / parseExcel (79-108) | 첫 ID 공백 행에서 break하여 이후 행을 읽지 않는다. | 중간 빈 행과 마지막 빈 행을 무시해야 한다. |
| 같은 파일 / parseRow (111-135) | 학번 길이·숫자 형식을 검사하지 않으며 첫 오류에 `EXCEL_PARSE_ERROR`를 던진다. | 학번 최대 10자·숫자, 전체 오류를 `BULK_APPLICATION_VALIDATION_FAILED` + `errors[]`로 반환해야 한다. (학과를 필수로 읽는 것은 현재 정책과 일치 — 3절 참고) |
| dto/ApplicationCreateRequest.java / ApplicantRequest, ReceiverRequest, MemberRequest (39-85) | Applicant email이 없고 Receiver 주소 필수 검증과 studentId 형식 검증이 없다. | 수정 가능한 Applicant email, Receiver 우편번호·기본주소 필수, 학생증 학번 제약과 충돌한다. |
| dto/BulkApplicationCreateRequest.java / ApplicantRequest, ReceiverRequest (27-52) | Applicant email이 없고 Receiver 우편번호·기본주소 필수 검증이 없다. | Applicant email 수정 가능 및 실물 배송 Receiver 주소 필수 정책과 충돌한다. |
| entity/ApplicationMember.java / createIndividual, createGroupRow (76-80, 93-120) | studentId 길이는 50이다. | 학번 최대 10자·숫자 정책과 충돌한다. (department 저장은 현재 정책과 일치 — 3절 참고) |
| repository/ApplicationRepository.java / countByApplicationNumberStartingWith (8-12) | `count+1` 채번에 사용되는 count 쿼리를 제공한다. | DB Sequence 신청번호 정책과 충돌한다. |
| common/exception/ErrorCode.java / ErrorCode (23-36) | `BULK_APPLICATION_VALIDATION_FAILED`, `APPLICATION_LIMIT_EXCEEDED`가 없고 현재 무제한인 ZIP에 500MB `ZIP_TOO_LARGE`가 남아 있다. | 단체 오류 코드와 일일 제한 오류 코드를 추가해야 하며 ZIP 최대 크기는 현재 제한 없음이다. `ZIP_TOO_LARGE` 호출은 확인되지 않아 실행 중인 제한으로는 분류하지 않는다. |
| common/response/ApiResponse.java / fail (6-30), GlobalExceptionHandler.java / handleCustomException (19-26) | 실패 응답은 단일 errorCode, errorMessage만 직렬화한다. | 단체 오류 응답은 `errors[]`를 함께 제공해야 한다. |
| ApplicationServiceBulkTest.java / createGroupRequiresStudentIdAndDepartmentForStudentCardType, createGroupSucceedsForStudentCardWithStudentIdAndDepartment (216-244) | 단일 `EXCEL_PARSE_ERROR`를 정상 계약으로 고정한다. | `BULK_APPLICATION_VALIDATION_FAILED` + `errors[]` 정책과 충돌한다. (학과 필수·저장 검증은 현재 정책과 일치 — 3절 참고) |

## 5. 미구현

| 정책 기능 | 확인 근거 |
|---|---|
| ApplicationPersistenceService 분리 | src/main에 클래스와 호출이 없고 생성 DB 저장은 ApplicationService가 직접 수행한다. |
| BULK_APPLICATION_VALIDATION_FAILED + errors[] | ErrorCode, 응답 DTO, 예외 처리기에 해당 코드·배열 구조가 없다. |
| 일일 KST 3회 제한의 DB 원자 처리 | APPLICATION_LIMIT_EXCEEDED와 제한 조회·원자 처리 구현이 없다. 현재 리팩터링 범위 미구현이라는 정책과 일치하는 상태다. |
| application_seq.nextval 채번 | Sequence 정의·호출이 없고 실제로 count+1 경로를 사용한다. |
| 업로드 추적 및 DB 실패 보상 삭제 | 생성 경로에 storageService.delete 호출과 업로드 목록 추적이 없다. |

## 6. Legacy

- 실제 구현에서 확인된 Legacy 항목은 없다.
- `APPLICATION.md`가 과거 정책으로 언급한 30% 부분 성공 로직은 `src/main`과 `src/test`에서 확인되지 않았다. 현재 단일 오류 즉시 실패 구현은 "수정 필요"로만 분류했다.
