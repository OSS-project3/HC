현재까지 Application 도메인의 정책을 다음과 같이 최종 확정했습니다.

이미 확정된 내용은 다시 제안하지 말고, 이 정책을 기준으로 문서와 구현 방향을 맞춰주세요.

# 1. Receiver 정책

sameAsApplicant=true인 경우

- 신청자 이름 → 수령인 이름 자동 복사
- 신청자 연락처 → 수령인 연락처 자동 복사
- 단, 자동 복사 후 사용자가 수정은 가능해야 한다.

배송지는 항상 Receiver에서 입력한다.

필수
- 우편번호
- 기본주소

선택
- 상세주소
- 배송 요청사항(deliveryRequest)

IssueType=MOBILE

- Receiver를 보내면 INVALID_INPUT
- Receiver Entity 생성하지 않음

IssueType=MOBILE_AND_PHYSICAL

- Receiver 필수
- sameAsApplicant=true면 이름/연락처만 자동 복사
- 배송지는 항상 Receiver 입력

---

# 2. 단체 ZIP 정책

ZIP 내부

members.xlsx
001.jpg
002.jpg
003.jpg
logo.png
seal.png

정책

- Excel은 ZIP 내부 1개만 허용
- 파일명은 자유
- 확장자는 .xlsx만 허용
- ZIP 루트에 위치
- Excel 2개 이상이면 전체 실패

사진 번호 정책

- 공식 양식의 A열 헤더는 `사진 번호`다.
- 4~103행에 `001`~`100`을 텍스트 값으로 미리 입력하고 셀을 잠근다.
- 사용자는 사진 번호를 직접 입력하거나 수정하지 않는다.
- 사진 번호만 있는 행은 빈 행이며, 영문명 등 B열 이후 신청자 정보가 입력된 행만 처리한다.
- ZIP 사진은 실제 처리되는 행의 사진 번호와 정확히 매칭하고, 빈 행 번호의 사진은 여분 사진으로 전체 실패한다.

사진 정책

001.JPG
001.jpg
001.Jpg

모두 허용

확장자는 대소문자 구분하지 않는다.

무시 대상

__MACOSX
.DS_Store

빈 행

- 중간 빈 행 무시
- 마지막 빈 행 무시

최대 인원

신청 1건당 100명

ZIP 최대 크기

현재 제한 없음
(운영 정책으로 TBD)

Excel 최대 행 수도
현재 제한 없음
(TBD로 문서화)

---

# 3. 단체 오류 정책

오류 하나라도 발생하면

신청 전체 실패

부분 성공 없음

응답은

BULK_APPLICATION_VALIDATION_FAILED

+

errors[]

형태로 반환한다.

---

# 4. 파일 업로드 정책

1.
모든 검증 완료

↓

2.
최종 경로에 바로 업로드

↓

3.
ApplicationService가 업로드 파일 목록 추적

↓

4.
UploadFile DB 저장

↓

5.
Application/ApplicationMember 저장

↓

6.
DB 실패

↓

업로드한 실제 파일 역순 삭제

↓

삭제 실패

↓

Error 로그 기록

향후 Cleanup Scheduler 도입 검토

신청 취소 시

파일은

30일 보관 후 삭제

정책으로 두고

현재는 TBD

---

# 5. 단체 트랜잭션 구조

ApplicationService
(비트랜잭션 오케스트레이터)

├── 검증
├── ZIP 파싱
├── 파일 준비
├── 파일 업로드
└── persistence.save()

↓

ApplicationPersistenceService
(@Transactional)

├── Application
├── Applicant
├── Receiver
└── ApplicationMember

Spring 자기호출 방식은 사용하지 않는다.

---

# 6. 상담 정책

상담 완료는

프론트 UX 안내용

백엔드는 검증하지 않는다.

---

# 7. 일일 신청 제한

정책

- 하루 3회까지 허용
- 4번째부터 거절
- KST 기준
00:00 ~ 23:59
- 개인/단체 합산
- ✅ 2026-08-16 확정: 취소(`CANCELLED`)된 신청은 그날 카운트에서 제외한다 — 취소하면 그 자리가 다시 빈다. 반려(`PHOTO_REJECTED`)는 재시도가 사진 재업로드(`PATCH .../photo`)로 처리되어 새 `create()` 호출 자체가 없으므로 별도 포함/제외 결정이 필요 없다(생성 시점에 이미 카운트됨).
- 카운트 대상은 `create()`(`createIndividual`/`createGroup`)로 새 `Application` row가 생성되는 시점뿐이다 — update성 재시도(사진 재업로드 등)는 새 row를 만들지 않으므로 카운트에 포함되지 않는다.
- 동시 요청은 DB 수준에서 원자적으로 처리
- APPLICATION_LIMIT_EXCEEDED ErrorCode 추가

✅ 2026-08-16 구현 완료. 취소가 최초로 성공하면 해당 신청이 차지한 생성일 KST 슬롯을 한 번만 반환한다.

---

# 8. 신청번호 생성

count+1 사용하지 않는다.

DB Sequence 사용

application_seq.nextval

기반으로 생성한다.

날짜별 번호 정책은 추후 결정.

---

# 9. 요청 멱등성

현재는 구현하지 않는다.

프론트에서

신청 버튼 비활성화

정도로 처리한다.

운영 전 Idempotency-Key 도입 여부 검토.

---

# 10. 파일 수정 정책

얼굴사진

새 파일 업로드 성공

↓

DB 갱신 성공

↓

기존 파일 삭제

학교 로고/직인도 동일 정책

수정 가능

기존 파일 삭제

---

# 11. 미결 정책(TBD)

아래는 구현하지 않고

PENDING_DECISIONS.md

또는

TBD

로 남긴다.

- 신청 취소 후 파일 보존 정책
- 반려 후 파일 보존 정책
- 회원 탈퇴 시 파일 처리
- 출력 완료 후 파일 보존 기간
- ZIP 최대 크기
- Excel 최대 행 수
- 최대 신청 인원
- 학생증 공백 문자열 정책
- 학교명 필드 추가 여부
- quantity 정책(개인 신청 프론트 입력 유지 여부)

---

# 12. 학생증 정책

학생증

학번

- 최대 10자
- 숫자만 허용

학과

현재 제외

학교명

필요 여부 TBD

학교 로고

필수

학교 직인

선택

공백 문자열 정책

TBD

---

# 13. 로고/직인 정책

개인

학생증
- 로고 필수
- 직인 선택

명예시민증
- 로고 없음
- 직인 없음

명예한국인증
- 로고 없음
- 직인 없음

방문증
- 로고 없음
- 직인 없음

단체

학생증
- 로고 필수
- 직인 선택

명예시민증
- 로고 필수
- 직인 필수

명예한국인증
- 로고 필수
- 직인 필수

방문증
- 로고 필수
- 직인 필수

---

# 14. 기존 정책 수정

반드시 수정

- 학생증 직인을 필수 검사하는 코드 → 선택으로 변경
- MOBILE에서 Receiver 전달 시 INVALID_INPUT을 API 문서에 명시
- 오류 하나라도 발생하면 전체 실패 정책으로 통일
- 과거 "30% 부분 성공" 정책 삭제 또는 Legacy 표시

---

# 15. Applicant 이메일

Applicant.email은

로그인 User.email을 기본으로 사용한다.

단,

신청 화면에서 수정 가능하도록 한다.

---

# 16. 신청·결제·취소·환불 정책

## 16-1. 신청·결제 업무 흐름

상담은 신청 전에 완료한다. 사진·내용 검토와 작명은 입금 확인 후에만 시작한다.

```text
사전 상담
→ 신청서 제출: SUBMITTED + WAITING
→ 결제 안내: paymentGuidedAt 기록, paymentDueAt = 안내 시각 + 72시간
→ 입금 확인: SUBMITTED + CONFIRMED
→ 검토 시작: REVIEWING + CONFIRMED
→ 사진 반려: PHOTO_REJECTED + CONFIRMED
→ 사진 재업로드: REVIEWING + CONFIRMED
→ 검토 승인: NAME_EDITING + CONFIRMED
→ 편집 완료: PRODUCTION_READY + CONFIRMED
→ 관리자 제작 승인: PRODUCING + CONFIRMED
→ 제작 완료: COMPLETED + CONFIRMED
```

ApplicationStatus는 신청 업무 진행 단계를, PaymentStatus는 실제 입금 확인 이력을 나타낸다. 입금 확인만으로 ApplicationStatus를 변경하지 않는다.

입금 확인은 멱등하게 처리한다. 이미 `PaymentStatus.CONFIRMED`인 신청에 입금 확인을 다시 요청하면 상태와 입금 이력을 변경하지 않고 `200 OK` 성공 응답을 반환한다. 중복 호출을 오류로 처리하거나 `PaymentStatus`를 다른 값으로 바꾸지 않는다. 성공 응답에는 필요하면 `이미 입금 확인이 완료된 신청입니다.`라는 안내를 포함하되, 기존 공통 `ApiResponse` 형식 안에서 제공한다.

```java
status == ApplicationStatus.SUBMITTED
        && paymentStatus == PaymentStatus.CONFIRMED
```

`startReview()`는 위 조건에서만 실행할 수 있다.

신청 생성 시 Payment 엔티티를 생성하거나 `totalPrice`를 계산하지 않는다. 상담으로 결정한 금액과 계좌이체 확인은 현재 운영 절차로 처리하며, 향후 온라인 결제가 필요할 때 Payment 도메인을 확장한다.

## 16-2. ApplicationStatus

| 상태 | 운영 의미 |
|---|---|
| `SUBMITTED` | 신청서 제출 완료, 입금·검토 시작 전 |
| `REVIEWING` | 입금 확인 후 사진·내용 검토 중 |
| `PHOTO_REJECTED` | 사진 보완 대기 |
| `NAME_EDITING` | 검토 승인 후 실제 작명·편집 작업 중 |
| `PRODUCTION_READY` | 검토·작명·편집 완료, 관리자 제작 승인 대기 |
| `PRODUCING` | 카드 제작 중 |
| `COMPLETED` | 발급 완료 |
| `CANCELLED` | 취소 완료 |

`PaymentStatus`는 `WAITING`, `CONFIRMED`만 사용하며 취소 또는 환불 후에도 기존 입금 확인 이력을 변경하지 않는다.

## 16-3. 사용자 취소

| 신청 상태 | 사용자 취소 | 가능한 결제 상태 |
|---|---:|---|
| `SUBMITTED` | 가능 | `WAITING`, `CONFIRMED` |
| `REVIEWING` | 가능 | 정책상 `CONFIRMED` |
| `PHOTO_REJECTED` | 가능 | 정책상 `CONFIRMED` |
| `NAME_EDITING` 이후 | 불가 | 관계없음 |
| `CANCELLED` | 멱등 성공 | 기존 값 유지 |

취소 가능 여부는 ApplicationStatus만으로 판단한다.

```java
public boolean canCancelByUser() {
    return status == ApplicationStatus.SUBMITTED
            || status == ApplicationStatus.REVIEWING
            || status == ApplicationStatus.PHOTO_REJECTED;
}
```

```text
SUBMITTED + WAITING
→ CANCELLED + WAITING
→ 환불 불필요
```

```text
SUBMITTED/REVIEWING/PHOTO_REJECTED + CONFIRMED
→ CANCELLED + CONFIRMED + refundedAt=null
→ 전액 환불 필요
```

```text
관리자가 외부에서 전액 환불 완료
→ CANCELLED + CONFIRMED + refundedAt!=null
```

이미 `CANCELLED`인 신청에 취소 API가 다시 호출되면 PaymentStatus, 취소 이력, `refundedAt`을 변경하지 않고 멱등 성공으로 처리한다. 최초 취소 성공 시에만 생성일 KST 기준 일일 신청 슬롯을 한 번 반환한다.

사용자 취소 API는 요청 본문 없이 제공한다.

```http
POST /api/applications/{applicationId}/cancel
```

## 16-4. 취소 이력

취소 시각, 실행 주체, 사유를 저장한다.

```java
private LocalDateTime cancelledAt;

@Enumerated(EnumType.STRING)
private CancellationType cancellationType;
// USER, SYSTEM, ADMIN

@Enumerated(EnumType.STRING)
private CancellationReason cancellationReason;
// USER_REQUEST, PAYMENT_TIMEOUT, ADMIN_DECISION
```

허용 조합은 `USER + USER_REQUEST`, `SYSTEM + PAYMENT_TIMEOUT`, `ADMIN + ADMIN_DECISION`이다. 이번 구현 범위는 사용자 취소와 미입금 자동 취소만이다. `ADMIN`과 `ADMIN_DECISION`은 향후 확장을 위해 값만 예약하며, 관리자 직접 취소 API·Service 메서드·허용 상태는 이번에 구현하지 않는다.

## 16-5. 최소 환불 모델

신청당 한 번 입금하고 전액 환불만 지원하므로 별도 Refund 엔티티와 환불 상태 enum을 만들지 않는다. Application에 nullable `refundedAt`만 저장한다.

```java
@Column
private LocalDateTime refundedAt;
```

| 조건 | 의미 |
|---|---|
| `CANCELLED + WAITING` | 미입금 취소, 환불 불필요 |
| `CANCELLED + CONFIRMED + refundedAt=null` | 전액 환불 대기 |
| `CANCELLED + CONFIRMED + refundedAt!=null` | 전액 환불 완료 |

환불 대상은 다음 조건으로 조회한다.

```sql
status = 'CANCELLED'
AND payment_status = 'CONFIRMED'
AND refunded_at IS NULL
```

환불 완료는 `CANCELLED + CONFIRMED`에서만 허용하고, `refundedAt`이 이미 있으면 값을 바꾸지 않는 멱등 성공으로 처리한다. 환불 시각은 요청값이 아니라 서버 시각을 기록한다.

환불 완료 API는 계좌이체를 실행하지 않는다. 관리자가 외부 운영 절차로 전액 환불한 사실만 기록한다.

```http
POST /api/admin/applications/{applicationId}/refund-complete
```

최초 환불 완료 시에만 기존 `AdminActivityLog`에 처리 관리자, 대상 신청, 처리 시각을 기록한다. `refundedAt`은 환불 누락과 중복 완료 기록을 관리하지만 외부 계좌이체의 동시 중복 송금까지 보장하지는 않는다. 초기 운영에서는 별도 Refund 엔티티, `refundProcessingAt`, `refundedBy`를 추가하지 않는다.

## 16-6. 결제 안내와 3일 미입금 자동 취소

결제 기한은 신청일이 아니라 최초 결제 안내 시각부터 정확히 72시간이다.

```java
paymentGuidedAt = now;
paymentDueAt = paymentGuidedAt.plusDays(3);
```

결제 안내를 재발송해도 기존 `paymentGuidedAt`, `paymentDueAt`을 초기화하거나 연장하지 않는다. 별도 기한 연장 기능은 현재 구현하지 않는다.

자동 취소 대상은 다음 조건이다.

```sql
status = 'SUBMITTED'
AND payment_status = 'WAITING'
AND payment_due_at IS NOT NULL
AND payment_due_at <= :now
```

자동 취소 스케줄러는 기본 10분 주기로 실행한다. 실행 주기는 코드에 하드코딩하지 않고 애플리케이션 설정값으로 관리하여 환경별로 변경할 수 있게 한다.

```properties
application.payment-timeout-scheduler.cron=0 */10 * * * *
```

스케줄러가 10분마다 실행되므로 실제 자동 취소 처리는 `paymentDueAt` 직후부터 최대 약 10분 늦어질 수 있다. 기한 판정 자체는 스케줄 실행 시각이 아니라 저장된 `paymentDueAt <= now` 조건을 사용한다.

자동 취소 결과는 다음과 같다.

```text
ApplicationStatus = CANCELLED
PaymentStatus = WAITING 유지
cancelledAt = 서버 현재 시각
cancellationType = SYSTEM
cancellationReason = PAYMENT_TIMEOUT
```

자동 취소 후 늦은 입금이 확인되면 ApplicationStatus를 복구하지 않는다. PaymentStatus만 실제 입금 이력에 맞게 `CONFIRMED`로 변경하여 `CANCELLED + CONFIRMED + refundedAt=null` 환불 대상으로 관리한다.

계좌이체 확인이 수동인 동안에는 실제 기한 내 입금이 관리자 확인 지연으로 자동 취소되지 않도록 스케줄러 실행 전에 입금 대조 운영 절차를 완료해야 한다.

## 16-7. 동시성 및 트랜잭션

다음 작업은 동시에 실행될 수 있다.

- 입금 확인과 미입금 자동 취소
- 사용자 취소와 관리자 검토 시작
- 사용자 취소와 다른 상태 전이
- 환불 완료 API 중복 호출

Application 주요 상태 전이에 `@Version` 낙관적 락 또는 동일 수준의 잠금 정책을 적용한다. 환불 완료는 트랜잭션 안에서 신청을 잠그고 처리한다.

사용자 취소의 Application 상태 변경과 일일 신청 슬롯 반환은 동일 트랜잭션에서 처리한다. 취소·환불 메서드는 실제 값이 처음 변경됐는지를 반환하고, 최초 변경일 때만 슬롯 반환과 감사 로그를 수행한다.

## 16-8. 모바일 카드와 실물 발송 완료

- 모바일 카드 파일 생성 완료 시 `cardReadyAt`을 기록하고 다운로드를 허용한다.
- `MOBILE`은 카드 파일 생성 완료 시 `COMPLETED`로 전이한다.
- `MOBILE_AND_PHYSICAL`은 카드 파일 생성 후에도 모바일 다운로드를 허용하며, 택배사 인계 시 `physicalDispatchedAt`을 기록하고 `COMPLETED`로 전이한다.
- 배송사, 운송장, 배송 중, 배송 완료 상태는 이 서비스에서 저장하지 않는다.

## 16-9. 취소 신청 파일 정리

사용자 취소와 미입금 자동 취소가 최초로 성공하면 해당 신청이 소유한 S3 파일을 별도 보관 기간 없이 바로 삭제한다. 중복 취소 요청에서는 삭제를 다시 수행하지 않는다.

삭제 대상은 다음과 같다.

- 개인·단체 구성원의 얼굴사진
- 학교·기관 로고와 직인
- 단체 신청 원본 제출 ZIP
- 그 밖에 해당 Application 전용으로 생성된 S3 객체

S3 삭제는 취소 DB 트랜잭션 안에서 먼저 실행하지 않는다. 트랜잭션이 rollback되면 신청은 유효한데 파일만 사라질 수 있으므로, 취소 상태·취소 이력·일일 슬롯 반환·파일 참조 정리를 같은 DB 트랜잭션에서 완료하고 실제 commit이 성공한 직후 `afterCommit`에서 S3 객체를 삭제한다. 이 정책에서 말하는 “바로 삭제”는 보관 기간이나 지연 삭제 배치를 두지 않고 commit 직후 삭제한다는 뜻이다.

DB에서는 `Application`의 `logoFileId`, `sealFileId`, `submitFileId`와 `ApplicationMember.photoPath`처럼 삭제된 S3 객체를 가리키는 참조를 함께 정리한다. Application, Applicant, Receiver, ApplicationMember 및 취소·결제 이력 자체는 운영·환불 확인을 위해 삭제하지 않는다.

S3 삭제 실패가 취소 commit을 되돌리지는 않는다. 원래 취소 결과를 유지하고 실패 key를 오류 로그로 남긴다. 초기 운영에서는 별도 파일 정리 스케줄러나 재시도 테이블을 추가하지 않으며, 실패 건은 운영 로그를 통해 수동 재삭제한다.

---

위 정책을 기준으로

1. 문서(requirements, data-model, api, checklist, TODO, changelog 등)를 모두 정합성 있게 수정하고,

2. 현재 구현과 충돌하는 부분을 체크리스트 형태로 정리해 주세요.

이미 확정된 정책을 다시 제안하지 말고, 충돌되는 구현과 수정이 필요한 부분만 알려주세요.
