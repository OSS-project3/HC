# Policy Sync Checklist

## 1. APPLICATION_POLICY.md 반영 여부

### requirements.md
- [ ] Receiver 정책 반영
- [ ] ZIP 정책 반영
- [ ] 학생증 정책 반영
- [ ] Payment 정책 반영
- [ ] 부분 성공 정책 제거

상태
- ✅ 완료
- ⚠ 수정 필요
- ❌ 미반영

---

### data-model.md
- [ ] Receiver 정책
- [ ] Applicant 이메일 정책
- [ ] 학생증 로고/직인 정책
- [ ] Payment 정책
- [ ] Quantity 정책

---

### api.md
- [ ] MOBILE + Receiver → INVALID_INPUT
- [ ] BULK_APPLICATION_VALIDATION_FAILED
- [ ] errors[] 응답
- [ ] ZIP 정책
- [ ] 학생증 API 수정

---

### checklist.md
- [ ] 신규 정책 검증 항목 추가
- [ ] Legacy 제거

---

### TODO.md
- [ ] TBD 이전
- [ ] 구현 예정 항목 정리

---

### CHANGELOG.md
- [ ] 정책 변경 이력 반영

---

## 문서와 구현 충돌

### Controller
- [ ] 충돌 없음
- [ ] 충돌 있음

내용

...

### Service
- [ ] 충돌 없음
- [ ] 충돌 있음

내용

...

### Validator
...

### DTO
...

### Entity
...

### ErrorCode
...

### Test
...

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

- [ ] Receiver 정책 일치
- [ ] MOBILE INVALID_INPUT
- [ ] 학생증 직인 선택
- [ ] Applicant 이메일 정책
- [ ] quantity 정책
- [ ] ErrorCode 변경
- [ ] API 예시 수정

---

## Service

### ApplicationService

- [ ] Receiver 정책
- [ ] 학생증 정책
- [ ] ZIP 정책
- [ ] 파일 업로드 순서
- [ ] 보상 삭제
- [ ] Payment 제거
- [ ] totalPrice 제거

---

### ApplicationPersistenceService

- [ ] 신규 구현 여부
- [ ] @Transactional
- [ ] 저장 책임 분리

---

## Validator

- [ ] 학생증 직인 선택
- [ ] Receiver 정책
- [ ] ZIP 정책
- [ ] 사진 정책
- [ ] Excel 정책

---

## Factory

- [ ] Entity 생성만 수행
- [ ] Repository 의존 없음
- [ ] UploadFile 의존 없음
- [ ] 학생증 정책 반영

---

## Entity

### Application

- [ ] quantity 정책
- [ ] Payment 정책
- [ ] logo/seal 정책

---

### Applicant

- [ ] 이메일 정책

---

### Receiver

- [ ] 배송지 정책

---

### ApplicationMember

- [ ] 학생증 정책

---

### UploadFile

- [ ] 파일 공유 금지

---

## DTO

- [ ] quantity 정책
- [ ] Receiver 정책
- [ ] Applicant 이메일
- [ ] 학생증 정책

---

## Repository

- [ ] Sequence 적용
- [ ] count+1 제거

---

## ErrorCode

- [ ] BULK_APPLICATION_VALIDATION_FAILED
- [ ] APPLICATION_LIMIT_EXCEEDED

---

## Test

### Unit Test

- [ ] Receiver 정책
- [ ] 학생증 정책
- [ ] ZIP 정책
- [ ] 전체 실패 정책
- [ ] 파일 보상 삭제

---

### Integration Test

- [ ] MOBILE Receiver
- [ ] 학생증 직인 선택
- [ ] Sequence 채번
- [ ] Upload rollback
- [ ] ErrorCode
- [ ] Payment 미생성

---

# 최종 Audit 결과

## 문서

- [ ] requirements.md
- [ ] data-model.md
- [ ] api.md
- [ ] checklist.md
- [ ] TODO.md
- [ ] CHANGELOG.md
- [ ] PENDING_DECISIONS.md

---

## 구현

- [ ] Controller
- [ ] Service
- [ ] Validator
- [ ] Factory
- [ ] Entity
- [ ] DTO
- [ ] Repository
- [ ] ErrorCode
- [ ] Test

---

## Legacy 제거

- [ ] 부분 성공 정책 제거
- [ ] 학생증 직인 필수 제거
- [ ] count+1 제거
- [ ] Payment 생성 제거
- [ ] totalPrice 제거

---

# Summary

## ✅ 정책과 일치

(작성)

---

## ⚠ 수정 필요

(작성)

---

## 🟡 TBD

(작성)

---

## ❌ Legacy

(작성)