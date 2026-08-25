# Policy Sync Checklist

> ✅ 2026-08-08 재감사: `checklist.md`의 Code Audit(3~6절)을 실제 코드와 다시 대조해 아래 "## 1"과 "# 구현 Audit", "# 최종 Audit 결과", "# Summary"를 갱신했다. 이번 재감사는 **코드가 정책과 일치하는지**를 중심으로 확인했고, `requirements.md`/`data-model.md`/`api.md` 본문의 개별 문장(아래 "# 1. 문서 동기화" 절)까지 한 줄씩 다시 읽지는 않았다 — 그 절은 이전 상태 그대로 두었으니 별도로 재검토가 필요하면 그때 갱신한다.

## 1. APPLICATION_POLICY.md 반영 여부

### requirements.md
- [x] Receiver 정책 반영
- [x] ZIP 정책 반영
- [x] 학생증 정책 반영
- [x] Payment 정책 반영
- [x] 부분 성공 정책 제거

상태
- ✅ 완료
- ⚠ 수정 필요
- ❌ 미반영

---

### data-model.md
- [x] Receiver 정책
- [x] Applicant 이메일 정책
- [x] 학생증 로고/직인 정책
- [x] Payment 정책
- [ ] Quantity 정책 (이번 재감사에서 data-model.md 본문을 직접 재확인하지 않음 — 코드 동작은 일치하나 문서 문장 확인은 TODO)

---

### api.md
- [x] MOBILE + Receiver → INVALID_INPUT
- [x] BULK_APPLICATION_VALIDATION_FAILED
- [x] errors[] 응답
- [x] ZIP 정책
- [x] 학생증 API 수정

---

### checklist.md
- [x] 신규 정책 검증 항목 추가 (2026-08-08 재감사로 갱신 완료)
- [x] Legacy 제거 (30% 부분 성공 로직 등 실제 Legacy 코드 없음 확인, `Receiver.copyFromApplicant` 미사용 메서드는 Legacy 후보로 별도 기록)

---

### TODO.md
- [ ] TBD 이전 (이번 재감사 범위 아님 — TODO.md 본문을 다시 읽지 않음)
- [ ] 구현 예정 항목 정리 (이번 재감사 범위 아님)

---

### CHANGELOG.md
- [ ] 정책 변경 이력 반영 (이번 재감사 범위 아님 — CHANGELOG.md 본문을 다시 읽지 않음)

---

## 문서와 구현 충돌

### Controller
- [x] 충돌 없음
- [ ] 충돌 있음

내용

`ApplicationController`의 `createGroup`이 `seal`을 `required=false`로 받고, `ApplicationService.createGroup`이 학생증일 때만 이를 선택으로 처리한다. Receiver 관련 검증(`MOBILE`/`MOBILE_AND_PHYSICAL`)도 Controller가 그대로 위임하는 `ApplicationService`에서 양방향으로 처리되어 정책과 충돌하지 않는다.

### Service
- [x] 충돌 없음
- [ ] 충돌 있음

내용

`ApplicationService`는 검증·업로드만 담당하는 비트랜잭션 오케스트레이터이고, 실제 저장은 `ApplicationPersistenceService`(`@Transactional`)로 분리되어 있다. Receiver 자동복사(수정 가능), Applicant 이메일 기본값(수정 가능), 학생증 검증, 업로드 보상 삭제, Sequence 기반 채번이 모두 정책과 일치한다.

### Validator

`ApplicationPhotoValidator`와 `BulkExcelParser`(학번 형식·행별 오류 수집)가 정책과 일치한다.

### DTO

`ApplicationCreateRequest`/`BulkApplicationCreateRequest`에 `applicant.email`이 존재하고, `Receiver`는 `zipCode`/`address`가 필수, `quantity` 필드는 없다(서버가 계산).

### Entity

`Application`/`Applicant`/`Receiver`/`ApplicationMember`가 정책과 일치한다. 다만 `Receiver.copyFromApplicant`는 실제 저장 경로에서 호출되지 않는 미사용 메서드다(Legacy 후보, 6절 참고).

### ErrorCode

`BULK_APPLICATION_VALIDATION_FAILED`가 존재하고 `EXCEL_NOT_FOUND`/`EXCEL_PARSE_ERROR`/`ZIP_TOO_LARGE`는 제거됐다. ~~`APPLICATION_LIMIT_EXCEEDED`는 아직 구현 범위 아님~~ → **2026-08-16 구현됨**(`ApplicationDailyLimit` + `ErrorCode.APPLICATION_LIMIT_EXCEEDED` 429, 일일 KST 3회 제한).

### Test

`ApplicationServiceBulkTest`/`BulkExcelParserTest`/`ApplicationBulkControllerTest` 모두 `BULK_APPLICATION_VALIDATION_FAILED` 기준으로 검증하며 옛 `EXCEL_PARSE_ERROR` 참조가 없다.

# POLICY_SYNC_CHECKLIST.md

APPLICATION_POLICY.md를 Source of Truth로 사용하여 문서와 구현이 정책과 일치하는지 검증하기 위한 체크리스트입니다.

---

# 1. 문서 동기화

## requirements.md

### Receiver 정책

- [ ] sameAsApplicant=true 정책 반영
- [ ] 이름 자동 복사
- [ ] 연락처 자동 복사
- [ ] 복사 후 수정 가능 명시
- [ ] 배송지는 항상 Receiver 입력
- [ ] MOBILE에서는 Receiver 금지
- [ ] MOBILE_AND_PHYSICAL에서는 Receiver 필수

---

### ZIP 정책

- [ ] Excel 1개만 허용
- [ ] .xlsx만 허용
- [ ] ZIP 루트 배치
- [ ] Excel 2개 이상 전체 실패
- [ ] String 기반 ID 처리
- [ ] trim() 정책
- [ ] 선행 0 유지
- [ ] 사진 확장자 대소문자 무시
- [ ] __MACOSX 무시
- [ ] .DS_Store 무시
- [ ] 중간 빈 행 무시
- [ ] 마지막 빈 행 무시
- [ ] ZIP 최대 크기 TBD
- [ ] Excel 최대 행 수 TBD
- [ ] 최대 인원 TBD

---

### 단체 오류 정책

- [ ] 오류 하나라도 전체 실패
- [ ] 부분 성공 제거
- [ ] BULK_APPLICATION_VALIDATION_FAILED 명시
- [ ] errors[] 응답 명시

---

### 파일 정책

- [ ] 모든 검증 후 업로드
- [ ] 최종 경로 업로드
- [ ] 업로드 파일 추적
- [ ] UploadFile 저장 정책
- [ ] 보상 삭제 정책
- [ ] Cleanup Scheduler TBD
- [ ] 파일 보존 정책 TBD

---

### 트랜잭션 정책

- [ ] ApplicationService 오케스트레이터
- [ ] PersistenceService 분리
- [ ] 자기호출 금지

---

### 상담 정책

- [ ] 프론트 UX 안내
- [ ] 백엔드 미검증

---

### 일일 신청 제한

- [ ] 하루 3회
- [ ] 4번째 거절
- [ ] KST 기준
- [ ] 개인/단체 합산
- [ ] 취소/반려 TBD
- [ ] ErrorCode 명시
- [ ] 현재 미구현 명시

---

### 신청번호 정책

- [ ] count+1 제거
- [ ] DB Sequence 사용
- [ ] 날짜 정책 TBD

---

### 멱등성

- [ ] 현재 미구현
- [ ] 프론트 버튼 비활성화
- [ ] 운영 전 검토

---

### 학생증 정책

- [ ] 학번 최대 10자
- [ ] 숫자만 허용
- [ ] 학과 제외
- [ ] 학교명 TBD
- [ ] 로고 필수
- [ ] 직인 선택
- [ ] 공백 정책 TBD

---

### Applicant 정책

- [ ] User.email 기본 사용
- [ ] 수정 가능

---

### Payment 정책

- [ ] 상담
- [ ] 금액 결정
- [ ] 계좌이체
- [ ] 관리자 확인
- [ ] Payment 생성 안 함
- [ ] totalPrice 계산 안 함
- [ ] Payment Entity 유지

---

## data-model.md

### Receiver

- [ ] Receiver 정책 일치
- [ ] 배송지 정책 일치
- [ ] MOBILE Receiver 없음

---

### Applicant

- [ ] Applicant 이메일 정책
- [ ] User.email 기본값

---

### Application

- [ ] quantity 정책
- [ ] IssueType 정책
- [ ] ApplicationType 정책

---

### 학생증

- [ ] 로고 필수
- [ ] 직인 선택
- [ ] 학번 정책

---

### UploadFile

- [ ] 파일 소유 정책
- [ ] 공유 금지 정책
- [ ] 삭제 정책

---

### Payment

- [ ] Payment 정책 일치

---

## api.md / API 명세

### Receiver

- [ ] MOBILE에서 Receiver 금지
- [ ] INVALID_INPUT 명시

---

### 학생증

- [ ] 로고 필수
- [ ] 직인 선택

---

### 단체 ZIP

- [ ] Excel 정책
- [ ] 사진 정책
- [ ] ZIP 정책

---

### Error Response

- [ ] BULK_APPLICATION_VALIDATION_FAILED
- [ ] errors[] 구조
- [ ] APPLICATION_LIMIT_EXCEEDED

---

### Request

- [ ] Applicant 이메일 정책
- [ ] quantity 정책
- [ ] Receiver 정책

## checklist.md

### 정책 검증 항목

#### Receiver

- [ ] sameAsApplicant=true 복사 정책 검증
- [ ] 이름 자동 복사
- [ ] 연락처 자동 복사
- [ ] 배송지 직접 입력
- [ ] MOBILE Receiver 거부
- [ ] MOBILE_AND_PHYSICAL Receiver 필수

---

#### 학생증

- [ ] 학교 로고 필수
- [ ] 학교 직인 선택
- [ ] 학번 최대 10자
- [ ] 숫자만 허용
- [ ] 학교명 TBD

---

#### 단체 신청

- [ ] ZIP 구조 검증
- [ ] Excel 1개
- [ ] 사진 매핑
- [ ] 사진 누락
- [ ] 중복 ID
- [ ] 중복 사진
- [ ] 불필요한 사진
- [ ] 전체 실패

---

#### 파일

- [ ] 보상 삭제
- [ ] 업로드 순서
- [ ] UploadFile 저장
- [ ] 삭제 실패 로그

---

#### 신청번호

- [ ] Sequence 사용

---

#### Payment

- [ ] Payment 생성 안 함
- [ ] totalPrice 계산 안 함

---

## TODO.md

### 구현 예정

#### Application

- [ ] ApplicationPersistenceService 구현
- [ ] BULK_APPLICATION_VALIDATION_FAILED 구현
- [ ] errors[] 응답 구현
- [ ] Sequence 기반 신청번호 생성
- [ ] 업로드 보상 삭제
- [ ] 학생증 직인 선택 반영
- [ ] MOBILE Receiver INVALID_INPUT

---

### 운영 이전

- [ ] 일일 신청 제한
- [ ] 멱등성(Idempotency)
- [ ] Cleanup Scheduler
- [ ] 파일 보존 정책

---

### 학생증

- [ ] 학교명 정책 확정
- [ ] 공백 문자열 정책

---

## CHANGELOG.md

### Policy

- [ ] Receiver 정책 변경
- [ ] ZIP 정책 변경
- [ ] 학생증 직인 선택
- [ ] 전체 실패 정책
- [ ] Payment 정책 변경
- [ ] Applicant 이메일 정책
- [ ] Sequence 채번 정책
- [ ] 파일 보상 정책

---

## PENDING_DECISIONS.md

### 파일

- [ ] 신청 취소 후 파일 보존 기간
- [ ] 반려 후 파일 보존 기간
- [ ] 회원 탈퇴 시 파일 처리
- [ ] 출력 완료 후 파일 보존 기간

---

### ZIP

- [ ] ZIP 최대 크기
- [ ] Excel 최대 행 수
- [ ] 최대 신청 인원

---

### 학생증

- [ ] 학교명 필드
- [ ] 공백 문자열 정책

---

### 신청

- [ ] quantity 정책
- [ ] 취소/반려 일일 횟수 포함 여부

---

# 구현 Audit

## Controller

### ApplicationController

- [x] Receiver 정책 일치
- [x] MOBILE INVALID_INPUT
- [x] 학생증 직인 선택
- [x] Applicant 이메일 정책
- [x] quantity 정책 (요청 DTO에 quantity 필드 없음 — 서버가 1 또는 엑셀 행 수로 계산)
- [ ] ErrorCode 변경 (Controller는 예외를 직접 다루지 않음 — Service/GlobalExceptionHandler에서 확인, 별도 재검토 불필요 판단)
- [ ] API 예시 수정 (api.md의 예시 문구 하나하나까지 이번 재감사에서 재확인하지 않음)

---

## Service

### ApplicationService

- [x] Receiver 정책
- [x] 학생증 정책
- [x] ZIP 정책
- [x] 파일 업로드 순서 (검증 완료 후에만 storeUploadFile/storePhotoFile 호출)
- [x] 보상 삭제
- [x] Payment 제거 (Payment 생성/totalPrice 계산 코드 없음)
- [x] totalPrice 제거

---

### ApplicationPersistenceService

- [x] 신규 구현 여부 (`domain/application/service/ApplicationPersistenceService.java`로 존재)
- [x] @Transactional
- [x] 저장 책임 분리 (Application/Applicant/Receiver/ApplicationMember 저장을 전담)

---

## Validator

- [x] 학생증 직인 선택
- [x] Receiver 정책
- [x] ZIP 정책
- [x] 사진 정책 (`ApplicationPhotoValidator`, 기존 감사에서 이미 정책과 일치로 분류되어 있었음)
- [x] Excel 정책

---

## Factory

- [x] Entity 생성만 수행
- [x] Repository 의존 없음
- [x] UploadFile 의존 없음
- [x] 학생증 정책 반영

---

## Entity

### Application

- [x] quantity 정책 (개인 1 고정, 단체는 `saveGroup(totalQuantity=rows.size())`로 전달)
- [x] Payment 정책 (Payment 관련 필드/로직 없음)
- [x] logo/seal 정책 (`logoFileId`/`sealFileId` 그대로 저장)

---

### Applicant

- [x] 이메일 정책

---

### Receiver

- [x] 배송지 정책 (`zipCode`/`address`는 항상 요청값으로 저장, `copyFromApplicant`는 미사용)

---

### ApplicationMember

- [x] 학생증 정책 (`studentId` 컬럼 길이 10)

---

### UploadFile

- [ ] 파일 공유 금지 (이번 재감사에서 `UploadFile` 엔티티/사용처를 별도로 재확인하지 않음)

---

## DTO

- [x] quantity 정책
- [x] Receiver 정책
- [x] Applicant 이메일
- [x] 학생증 정책

---

## Repository

- [x] Sequence 적용 (`ApplicationService.nextApplicationSequence`가 네이티브 쿼리로 `application_seq.nextval` 사용)
- [x] count+1 제거 (`ApplicationRepository.countByApplicationNumberStartingWith` 자체가 코드에서 삭제됨)

---

## ErrorCode

- [x] BULK_APPLICATION_VALIDATION_FAILED
- [ ] APPLICATION_LIMIT_EXCEEDED (정책상 현재 범위에서 구현하지 않음 — 미구현이 정상 상태)

---

## Test

### Unit Test

- [ ] Receiver 정책 (이번 재감사에서 개별 단위 테스트 파일까지는 재확인하지 않음)
- [ ] 학생증 정책 (위와 동일)
- [ ] ZIP 정책 (위와 동일)
- [x] 전체 실패 정책 (`BulkExcelParserTest`/`ApplicationServiceBulkTest`가 `BULK_APPLICATION_VALIDATION_FAILED` 단언)
- [ ] 파일 보상 삭제 (이번 재감사에서 재확인하지 않음)

---

### Integration Test

- [ ] MOBILE Receiver (이번 재감사에서 재확인하지 않음)
- [ ] 학생증 직인 선택 (위와 동일)
- [ ] Sequence 채번 (위와 동일)
- [ ] Upload rollback (위와 동일)
- [x] ErrorCode (`ApplicationBulkControllerTest`가 `BULK_APPLICATION_VALIDATION_FAILED` 응답 단언)
- [ ] Payment 미생성 (이번 재감사에서 재확인하지 않음)

---

# 최종 Audit 결과

## 문서

- [x] requirements.md (checklist.md 1절 기준)
- [ ] data-model.md (Quantity 정책 문장은 미재확인)
- [x] api.md
- [x] checklist.md (2026-08-08 재작성 완료)
- [ ] TODO.md (미재확인)
- [ ] CHANGELOG.md (미재확인)
- [ ] PENDING_DECISIONS.md (미재확인)

---

## 구현

- [x] Controller
- [x] Service
- [x] Validator
- [x] Factory
- [x] Entity
- [x] DTO
- [x] Repository
- [x] ErrorCode (APPLICATION_LIMIT_EXCEEDED는 의도적 미구현)
- [x] Test (일부 세부 항목은 미재확인으로 표시)

---

## Legacy 제거

- [x] 부분 성공 정책 제거
- [x] 학생증 직인 필수 제거
- [x] count+1 제거
- [x] Payment 생성 제거
- [x] totalPrice 제거

---

# Summary

## ✅ 정책과 일치

2026-08-07 시점 `checklist.md`가 "수정 필요"로 기록했던 12건이 2026-08-08 재감사에서 전부 코드와 일치하는 것으로 확인됐다: Receiver 자동복사(수정 가능·배송지 필수), MOBILE 양방향 Receiver 검증, Applicant 이메일 기본값+수정 가능, 학생증 직인 선택·학번 형식 검증, 단체 신청 logo/seal 카드종류별 필수 조건, 업로드 보상 삭제, `application_seq` 기반 채번(count+1 완전 제거), `BulkExcelParser`의 ZIP 루트 단일 Excel·중간/마지막 빈 행 무시·행별 오류 수집, `ErrorCode`/`ApiResponse`의 `BULK_APPLICATION_VALIDATION_FAILED` + `errors[]`, `ApplicationMember.studentId` 컬럼 길이(10), 관련 테스트의 `BULK_APPLICATION_VALIDATION_FAILED` 전환까지 전부 확인됨. 근거는 `checklist.md` 3절 표 참고.

## ⚠ 수정 필요

이번 재감사에서 새로 발견된 코드-정책 불일치는 없다.

## 🟡 TBD

- 일일 신청 3회 제한(`APPLICATION_LIMIT_EXCEEDED`)은 정책(`APPLICATION.md` §7)이 명시한 대로 현재 범위에서 의도적으로 미구현 상태다.
- `data-model.md`의 Quantity 정책 문장, `TODO.md`/`CHANGELOG.md`/`PENDING_DECISIONS.md` 본문, DTO 이하 일부 단위/통합 테스트 세부 항목은 이번 재감사 범위에서 재확인하지 않았다 — 필요 시 별도 패스로 확인.

## ❌ Legacy

- `Receiver.copyFromApplicant(Long, Applicant)`와 이를 감싸는 `ApplicationFactory.copyIndividualReceiver`가 운영 저장 경로에서 더 이상 호출되지 않는 미사용 메서드로 남아 있다(`zipCode`/`address`를 `null`로 채우는 구현이라 재사용 시 정책과 충돌 위험). 삭제 여부 결정 필요 — `checklist.md` 6절 참고.