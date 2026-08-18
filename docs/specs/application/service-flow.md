# Application Service Flow

> 이 문서는 Application 생성 유스케이스의 Service 처리 순서와 책임 경계를 정의한다.
> 업무 정책은 [requirements.md](requirements.md), 데이터 구조는 [data-model.md](data-model.md), 외부 계약은 [api.md](api.md)를 우선한다.

## 1. 확정 원칙

Application 생성에는 서로 독립적인 세 가지 분기 축이 있다.

| 분기 축 | 책임 |
|---|---|
| `ApplicationType` | 개인·단체 입력 구조, 구성원 수, 생성 전략 |
| `IssueType` | Receiver 검증·생성 여부와 배송 정보 |
| `CardType` | 카드별 필드와 추가 파일 요구사항 |

핵심 규칙:

- 공개 생성 유스케이스는 `createIndividual()`, `createGroup()`으로 유지한다.
- `IssueType`은 Receiver와 배송 정책만 결정한다.
- 필요한 파일 조합은 `ApplicationType × CardType`으로 결정한다.
- `IssueType`에 따라 파일 요구사항, 파일 검증 또는 파일 준비 방식을 변경하지 않는다.
- 모든 입력과 파일을 검증한 후 신청번호 생성, object storage 업로드, DB 저장을 수행한다.
- Application은 IDENTITY 전략에 따라 먼저 저장하고, 발급된 `applicationId`로 하위 Entity를 생성한다.

## 2. 분기별 책임

### 2.1 ApplicationType

#### INDIVIDUAL

- Request의 `member`로 구성원 1명을 입력한다.
- 얼굴사진은 `photo` multipart part로 받는다.
- `ApplicationMember`를 정확히 1건 생성한다.
- 제출 ZIP은 사용하지 않는다.

#### GROUP

- 구성원 정보는 `submitFile`의 Excel에서 입력한다.
- 구성원 얼굴사진은 `submitFile` ZIP 내부에서 실제 입력된 행의 고정 사진 번호(`001`~`100`)와 매칭한다.
- 사진 번호만 미리 채워진 행은 무시하고, 영문명 등 신청자 정보가 입력된 유효 행 수만큼 `ApplicationMember`를 생성한다.
- 단체 공통 로고·직인은 Application 단위 파일로 처리한다.

### 2.2 IssueType

#### MOBILE

- Receiver를 요구하지 않는다.
- Receiver Entity를 생성하지 않는다.
- 배송지 검증을 수행하지 않는다.
- Request에 Receiver가 전달되면 `INVALID_INPUT`으로 거절한다.

#### MOBILE_AND_PHYSICAL

- Receiver가 반드시 필요하다.
- `sameAsApplicant=true`이면 Applicant의 공통 정보를 복사해 Receiver를 생성한다.
- `sameAsApplicant=false`이면 수령인 이름, 연락처, 우편번호, 주소 등 확정된 배송 필수값을 검증한다.
- 검증된 Receiver를 Application 저장 후 1건 생성한다.

### 2.3 CardType

#### 일반 카드

대상:

- `HONOR_KOREAN`
- `HONOR_CITIZEN`
- `VISITOR`

개인 신청에서는 카드별 추가 필드가 없고 얼굴사진만 필요하다. 학생증 전용 필드와 학교 파일이 들어오면 `INVALID_INPUT`으로 거절한다.

#### STUDENT

- 학번 필수
- 학과 필수
- 학교 로고 필수
- 학교 직인 선택
- 학교 직인이 전달된 경우에만 이미지 검증과 파일 준비를 수행한다.

## 3. 파일 요구사항

| ApplicationType | CardType | 파일 조합 |
|---|---|---|
| 개인 | 일반 카드 | 얼굴사진 |
| 개인 | 학생증 | 얼굴사진 + 학교 로고 + 선택적 학교 직인 |
| 단체 | 일반 카드 | 기관 로고 + 기관 직인 + Excel·사진 ZIP |
| 단체 | 학생증 | 학교 로고 + 선택적 학교 직인 + Excel·사진 ZIP |

파일 처리는 다음 세 책임으로 구분한다.

### File Requirement

`ApplicationType × CardType`에 따라 필요한 파일과 선택 파일을 결정한다.

### File Validation

- 필수 파일 존재 여부를 확인한다.
- 전달된 이미지의 크기, 확장자, MIME, signature와 디코딩 결과를 검증한다.
- 얼굴사진은 EXIF Orientation 적용 후 최소 해상도를 검증한다.
- 단체 ZIP 구조, Excel 행, ID와 사진 매칭, 중복·누락·불필요 파일을 검증한다.
- 선택 파일은 전달된 경우에만 검증한다.

### File Preparation

- 모든 검증이 끝난 파일만 object storage에 업로드한다.
- Application 단위 파일은 object storage 업로드 후 `UploadedFileMetadata`로 보관하고, `UploadFile` DB row는 `ApplicationPersistenceService`의 동일 트랜잭션 안에서 생성해 `logoFileId`, `sealFileId`, `submitFileId`를 준비한다.
- 구성원 얼굴사진은 저장 경로를 준비해 `ApplicationMember.photoPath`에 전달한다.
- object storage 업로드 중간 실패 또는 DB 저장 실패 시 이미 업로드한 신규 파일을 역순 보상 삭제한다.
- 보상 삭제 자체가 실패해도 원래 실패 원인을 덮어쓰지 않고 로그만 남긴다.

## 4. 책임 배치

### ApplicationService

- 생성 흐름 오케스트레이션
- User와 CardType 조회
- Validator 호출과 오류 우선순위 관리
- 신청번호 생성 호출
- 파일 준비 호출
- Factory 호출
- Repository 저장 순서와 트랜잭션 경계 관리
- 실패 시 파일 보상 처리
- Response 생성

### UserService

- 회원 존재 검증
- ACTIVE 상태 검증
- USER 권한 검증
- 필수 약관 동의 검증

일일 신청 제한은 사용자별 KST 하루 3회로 구현되어 있다. 개인·단체 신청을 합산하며 생성 전에 `ApplicationDailyLimitService.reserveSlot()`으로 원자적으로 예약하고, 생성 실패 또는 최초 취소 성공 시 생성일 KST 슬롯을 한 번 반환한다.

신청 생성 직후 상태는 개인·단체 모두 `SUBMITTED + WAITING`이다. 결제 안내·입금 확인·검토·취소·환불·자동 취소 흐름은 [APPLICATION.md](APPLICATION.md) §16을 따르며, 자동 취소 스케줄러는 기본 10분 주기를 설정값으로 제공한다.

결제 안내와 입금 확인 Application Service 명령은 구현되어 있다. 결제 안내 재호출은 최초 안내 시각과 72시간 기한을 유지하고, 입금 확인 재호출은 `CONFIRMED`를 유지한다. 최초 입금 확인에만 `AdminActivityLog.PAYMENT_CONFIRMED`를 한 건 남긴다. 자동 취소 스케줄러는 대상 ID를 조회한 뒤 각 신청을 별도 트랜잭션으로 재검증하며, 최초 자동 취소에 사용자 취소와 동일한 슬롯 반환·DB 파일 참조 정리·after-commit S3 삭제를 적용한다.

### Validator

- Request 형식 검증은 DTO와 Spring Validation이 담당한다.
- Receiver 정책은 개인·단체에서 동일하게 재사용한다.
- 카드별 필드 검증은 CardType 기준으로 수행한다.
- 이미지 내용 검증은 `ApplicationPhotoValidator`가 담당한다.
- 단체 ZIP·Excel·사진 매핑 검증은 DB 저장과 object storage 업로드 없이 완료한다.

### ApplicationFactory

- 유효성이 확인된 값으로 Entity만 생성한다.
- Repository, object storage, 외부 API를 호출하지 않는다.
- 입력 검증, 파일 검증, 신청번호 생성, 트랜잭션을 담당하지 않는다.
- Application 저장 전 null FK를 가진 하위 Entity 묶음을 만들지 않는다.

## 5. 개인 신청 처리 순서

`POST /api/applications`

~~~text
1. Spring MVC / DTO 형식 검증

2. User 조회 및 신청 자격 검증
   ├─ 회원 존재
   ├─ ACTIVE
   ├─ USER
   └─ 필수 약관 동의

3. CardType 조회 및 active 검증

4. IssueType / Receiver 검증
   ├─ MOBILE: Receiver 금지
   └─ MOBILE_AND_PHYSICAL: Receiver와 배송 필수값 검증

5. 개인 공통 입력 검증
   ├─ 공통 Member 필수값
   └─ 얼굴사진 검증

6. CardType별 검증
   ├─ 일반 카드: 학생증 전용 입력 금지
   └─ STUDENT
      ├─ 학번 필수
      ├─ 학과 필수
      ├─ 학교 로고 필수 및 검증
      └─ 학교 직인은 전달된 경우에만 검증

7. 모든 검증 완료

8. 신청번호 및 서버 생성값 준비

9. 카드 공통 파일 준비
   ├─ 학생증 학교 로고 업로드 및 UploadFile 저장
   └─ 학교 직인이 있으면 업로드 및 UploadFile 저장

10. Factory → Application 생성

11. Application 저장 및 ID 발급

12. Factory → Applicant 생성 및 저장

13. IssueType별 Receiver 처리
   ├─ MOBILE: 생성하지 않음
   └─ MOBILE_AND_PHYSICAL: Factory 생성 후 저장

14. 얼굴사진 업로드

15. Factory → ApplicationMember 생성 및 저장

16. 응답 생성

17. 실패 시 업로드 파일 보상 삭제
~~~

## 6. 단체 신청 처리 순서

`POST /api/applications/bulk`

~~~text
1. Spring MVC / DTO 형식 검증

2. User 조회 및 신청 자격 검증
   ├─ 회원 존재
   ├─ ACTIVE
   ├─ USER
   └─ 필수 약관 동의

3. CardType 조회 및 active 검증

4. IssueType / Receiver 검증
   ├─ MOBILE: Receiver 금지
   └─ MOBILE_AND_PHYSICAL: Receiver와 배송 필수값 검증

5. 단체 신청인 검증
   ├─ 기관명
   ├─ 담당자명
   ├─ 부서
   └─ 연락처

6. ApplicationType × CardType 파일 요구사항 검증
   ├─ 일반 단체: 기관 로고·기관 직인·submitFile 필수
   └─ 학생증 단체: 학교 로고·submitFile 필수, 학교 직인 선택

7. 공통 이미지와 submitFile 전체 검증
   ├─ 로고와 전달된 직인 이미지 검증
   ├─ ZIP 구조와 Excel 형식 검증
   ├─ ID 중복 검증
   ├─ 사진 누락·중복·불필요 파일 검증
   ├─ 구성원 필수값 검증
   ├─ 구성원 얼굴사진 검증
   └─ 학생증 구성원 학번·학과 검증

8. 모든 검증 완료
   └─ 이 시점까지 DB 저장과 object storage 업로드 없음

9. 신청번호 및 서버 생성값 준비

10. 공통 파일 준비
    ├─ logo 업로드 및 UploadFile 저장
    ├─ 필수 또는 선택 seal 업로드 및 UploadFile 저장
    └─ submitFile 업로드 및 UploadFile 저장

11. Factory → Application 생성

12. Application 저장 및 ID 발급

13. Factory → Group Applicant 생성 및 저장

14. IssueType별 Receiver 처리
    ├─ MOBILE: 생성하지 않음
    └─ MOBILE_AND_PHYSICAL: Factory 생성 후 저장

15. 구성원별 처리
    ├─ 얼굴사진 업로드
    ├─ Factory → ApplicationMember 생성
    └─ ApplicationMember 저장

16. 응답 생성

17. 실패 시 업로드한 모든 파일 보상 삭제
~~~

## 7. 오류 우선순위

사용자 입력 오류와 외부 부수효과가 섞이지 않도록 다음 순서를 유지한다.

~~~text
DTO 형식 오류
→ User 자격 오류
→ CardType 오류
→ IssueType / Receiver 오류
→ 공통 입력 오류
→ 카드별 입력 오류
→ 파일 내용 오류
→ 신청번호 생성 오류
→ object storage 오류
→ DB 저장 오류
~~~

## 8. 트랜잭션과 저장 순서

- 파일 파싱과 내용 검증은 DB 트랜잭션 밖에서 완료한다.
- IDENTITY 전략을 유지하므로 저장 순서는 `Application → Applicant → Receiver → ApplicationMember`다.
- Receiver는 `MOBILE_AND_PHYSICAL`일 때만 저장한다.
- 하나의 신청에서 발생하는 DB 변경은 하나의 트랜잭션으로 처리한다.
- object storage 작업은 DB 롤백 대상이 아니므로 업로드 목록을 추적하고 실패 시 보상 삭제한다.
- 파일 검증 실패에는 업로드와 DB 저장이 없어야 한다.

### 사진 재업로드 S3 lifecycle

- 재업로드에서 새로 업로드한 S3 파일은 DB 트랜잭션 rollback 또는 commit 실패 시 보상 삭제한다.
- 재업로드에서 기존 S3 파일은 DB commit 성공 이후에만 삭제한다.
- commit 이후 기존 S3 파일 삭제가 실패해도 재업로드 성공 응답을 실패로 뒤집지 않고 로그만 남긴다.
- 재업로드 성공 후 Application 상태는 `PHOTO_REJECTED`에서 `REVIEWING`으로 복귀한다.
