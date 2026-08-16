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

ID 정책

- trim() 후 비교
- String으로 처리
- 001 같은 선행 0 유지

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

현재 제한 없음

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

현재 리팩터링 범위에서는 구현하지 않는다.

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
- 취소/반려 신청의 일일 횟수 포함 여부

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

# 16. Payment 정책

현재 신청 흐름은

상담

↓

금액 결정

↓

계좌이체

↓

관리자 확인

이다.

신청 생성 시

Payment 생성

totalPrice 계산

은 수행하지 않는다.

다만

향후 온라인 결제 기능을 구현할 예정이므로

Payment Entity와 관련 도메인은 삭제하지 않는다.

---

위 정책을 기준으로

1. 문서(requirements, data-model, api, checklist, TODO, changelog 등)를 모두 정합성 있게 수정하고,

2. 현재 구현과 충돌하는 부분을 체크리스트 형태로 정리해 주세요.

이미 확정된 정책을 다시 제안하지 말고, 충돌되는 구현과 수정이 필요한 부분만 알려주세요.