# 백엔드 아키텍처

> 이 문서는 `DB.md`와 `docs/api/README.md`를 기준으로 모놀리식 백엔드의 패키지 구조와 내부 모듈 규칙을 정의한다.
> 구현 코드와 본 문서가 충돌하면 API 계약은 `docs/api/README.md`, 데이터 구조와 제약은 `DB.md`를 우선 확인한다.

---

## 1. 목적

이 시스템은 외국인을 대상으로 다음 카드를 신청·검토·발급하는 Spring Boot 기반 웹 서비스다.

- 명예 한국인증 (`HONOR_KOREAN`)
- 명예 시민증 (`HONOR_CITIZEN`)
- 방문증 (`VISITOR`)
- 학생증 (`STUDENT`)

서비스는 하나의 애플리케이션과 하나의 데이터베이스로 배포하는 **모놀리식 구조**를 사용한다. 단, 코드 내부는 업무 도메인별 모듈로 나누어 다음 목표를 달성한다.

- 한 기능을 수정할 때 다른 기능에 미치는 영향을 최소화한다.
- 도메인 규칙이 Controller나 외부 연동 코드로 흩어지지 않게 한다.
- 개인 신청과 단체 신청이 동일한 핵심 모델을 공유하게 한다.
- 향후 특정 도메인을 별도 서비스로 분리하더라도 경계를 다시 설계하지 않게 한다.
- API, 데이터베이스, 파일 저장소, 인증 구현을 서로 독립적으로 변경할 수 있게 한다.

---

## 2. 전체 구조

> ⚠️ 2026-08-01 정정: 원래 API/Application/Domain/Infrastructure 4계층 + Port/Adapter 구조로 작성돼 있었으나, **지금 실제 코드 규모(도메인당 파일 5~10개 수준)에는 과한 설계**라 아래처럼 단순화했다. 도메인이 커져서 유스케이스 조정 로직과 순수 도메인 규칙을 분리할 필요가 실제로 생기면, 그때 이 문서에 Application/Domain 계층 분리를 다시 추가하는 방향으로 간다 — 지금 미리 빈 계층을 만들지 않는다.

```text
[사용자 웹 / 관리자 웹]
          │ HTTP + JSON / Multipart
          ▼
┌──────────────────────────────────────────┐
│ Controller (api 패키지)                  │
│ HTTP 매핑 · Request/Response DTO         │
├──────────────────────────────────────────┤
│ Service (domain/{도메인}/service)        │
│ 트랜잭션 · 권한/소유권 검사 · 도메인 규칙 │
├──────────────────────────────────────────┤
│ Repository / Entity (domain/{도메인})    │
│ JPA 영속성 · 상태 전이 · 불변조건        │
├──────────────────────────────────────────┤
│ Infrastructure (infra 패키지)            │
│ S3 · Redis · JWT · 이미지 렌더러 · Toss  │
└──────────────────────────────────────────┘
          │
          ├── RDBMS
          ├── Object Storage
          └── Redis
```

### 2.1 아키텍처 스타일

- 배포 단위는 하나다.
- 데이터베이스 트랜잭션도 하나의 애플리케이션 안에서 관리한다.
- 패키지는 기술이 아니라 도메인을 최상위 기준으로 나눈다(`domain/user`, `domain/application` 등).
- 도메인 내부는 Service(유스케이스 조정 + 도메인 규칙 실행을 함께 담당) / Repository / Entity / DTO로만 나눈다 — Application 계층과 Domain 계층을 별도 패키지로 분리하지 않는다.
- 도메인 간 호출은 다른 도메인의 공개 Service를 통한다. Repository 직접 주입 금지.
- 다른 도메인의 Entity를 직접 변경하지 않는다.

### 2.2 요청 처리 흐름

```text
Controller
  → Request DTO 검증(@Valid)
  → Service 호출
  → Entity 조회 및 도메인 규칙 실행(의미 있는 메서드 호출)
  → Repository 저장(JPA dirty checking 또는 명시적 save)
  → Response DTO 변환
  → 공통 응답(ApiResponse) 반환
```

Controller는 HTTP를 해석해 Service로 위임만 하고, Service가 트랜잭션 경계를 잡고 유스케이스를 조정하며, Entity는 자신의 상태와 불변조건을 지킨다.

---

## 3. 최상위 패키지 구조

> ⚠️ 2026-08-01 정정: 도메인마다 `api/application/domain/infrastructure` 4개 하위 패키지를 두는 구조로 작성돼 있었으나, 실제 코드는 아래처럼 훨씬 단순한 구조를 쓰고 있다. 이미 구현된 User 도메인 기준으로 맞춤.

```text
com.example.honorcitizen
├── HonorCitizenApplication.java
│
├── api                        ← 전체 Controller (도메인별로 나누지 않고 flat)
│   └── admin                  ← 관리자 전용 Controller
│
├── common
│   ├── entity                 ← BaseTimeEntity 등 공통 상위 클래스
│   ├── enums                  ← 여러 도메인이 공유하는 enum
│   ├── exception               ← CustomException, ErrorCode, GlobalExceptionHandler
│   └── response                ← ApiResponse
│
├── domain
│   ├── user
│   │   ├── entity
│   │   ├── repository
│   │   ├── service
│   │   ├── dto
│   │   └── scheduler
│   ├── application
│   │   ├── entity
│   │   ├── repository
│   │   ├── service
│   │   └── dto
│   ├── payment
│   ├── card
│   └── ...                    ← 새 도메인도 같은 패턴
│
└── infra                      ← 외부 기술 어댑터 (도메인 소속 아님)
    ├── security                ← JWT, OAuth2, Redis 세션
    ├── storage                 ← S3
    ├── card                    ← 카드 이미지 렌더링
    └── toss                    ← (구도메인 전용, 신규 설계에선 미사용)
```

파일이 실제로 존재할 때만 하위 패키지를 만든다(예: `scheduler`는 스케줄러가 있는 도메인에만). 빈 계층을 유지하기 위한 빈 클래스나 인터페이스는 만들지 않는다.

### 3.1 도메인 내부 기본 구조

```text
domain/application
├── entity
│   ├── Application.java
│   ├── ApplicationMember.java
│   ├── Applicant.java
│   └── Receiver.java
├── repository
│   └── ApplicationRepository.java   (JpaRepository 상속, 복잡한 조회는 @Query)
├── service
│   └── ApplicationService.java      (Command/Query 구분 없이 한 클래스, 트랜잭션 경계)
└── dto
    ├── ApplicationCreateRequest.java
    └── ApplicationResponse.java
```

Controller는 `api` 패키지에 별도로 있다(예: `api/ApplicationController.java`, `api/admin/AdminApplicationController.java`) — 도메인 패키지 안에 두지 않는다.

### 3.2 계층별 책임

| 계층 | 위치 | 책임 | 금지 사항 |
|---|---|---|---|
| Controller | `api/**` | HTTP 매핑, 인증 principal 수신(`@AuthenticationPrincipal`), `@Valid` DTO 검증, 응답 코드 결정 | 비즈니스 로직, Repository 직접 호출 |
| Service | `domain/{도메인}/service` | 유스케이스 조정 + 도메인 규칙 실행(구분 안 함), 트랜잭션(`@Transactional`), 권한·소유권 검사, Entity ↔ DTO 변환 | Controller 반환값/HTTP 객체 의존 |
| Entity | `domain/{도메인}/entity` | 상태 전이, 불변조건, 의미 있는 메서드로 캡슐화된 상태 변경 | public setter, Spring/JPA 밖 기술 의존 |
| Repository | `domain/{도메인}/repository` | `JpaRepository` 상속, 단순 조회/저장 | 비즈니스 규칙 판단 |
| Infrastructure | `infra/**` | JPA 밖 외부 기술(S3, Redis, JWT, 이미지 렌더러) | 업무 상태 전이 결정 |

---

## 4. 내부 모듈 정의

## 4.1 User 모듈

### 책임

- Google/Naver OAuth 사용자를 식별한다.
- 약관 동의 상태를 관리한다.
- 내 정보 조회·수정과 회원탈퇴를 처리한다.
- 사용자 역할(`USER`, `ADMIN`)과 계정 상태를 관리한다.
- refresh token session과 로그아웃을 관리한다.

### 소유 데이터

- `User`
- refresh token session 저장 모델 또는 Redis key

### 외부에 제공하는 기능

- 현재 사용자 조회
- 활성 사용자 여부 검증
- 약관 동의 여부 검증
- 탈퇴 요청 및 유예기간 내 복구
- 관리자 역할 검증

### 규칙

- 다른 모듈은 `User` Entity를 직접 수정하지 않는다.
- 신청 시점의 이름·이메일·전화번호는 `Applicant`에 스냅샷으로 저장한다.
- `Applicant.email`은 신청 당시 로그인한 `User.email`과 일치해야 한다.
- 회원탈퇴 후에도 신청·결제 이력을 유지하기 위해 User row를 물리 삭제하지 않는다.
- 탈퇴 요청 시 세션을 즉시 무효화한다.
- 7일 경과 후 PII 익명화는 스케줄러가 수행한다.

---

## 4.2 Application 모듈

### 책임

- 개인 및 단체 신청을 생성한다.
- 신청인, 수령인, 카드 대상 구성원을 관리한다.
- 신청 상태와 결제 확인 상태를 관리한다.
- 신청 조회, 사진 재업로드, 관리자 검토 흐름을 제공한다.
- 사전 상담으로 확정된 신청별 결제 금액과 카드 종류를 저장한다.

### Aggregate 경계

`Application`이 Aggregate Root다.

```text
Application
├── Applicant             1:1
├── Receiver              1:1
├── Payment               1:1
└── ApplicationMember     1:N
```

논리적으로 위 모델은 하나의 신청 단위를 이룬다. 다만 목록 조회 성능과 대량 구성원 처리를 위해 JPA 객체 그래프 전체를 항상 즉시 로딩하지 않는다.

### 개인·단체 통합 규칙

| 구분 | Application | ApplicationMember |
|---|---:|---:|
| 개인 신청 | 1건 | 반드시 1건 |
| 단체 신청 | 1건 | 엑셀 유효 행 수만큼 N건 |

- `application_type=INDIVIDUAL`이면 `total_quantity=1`이다.
- `application_type=GROUP`이면 `total_quantity`는 저장된 구성원 수와 같아야 한다.
- 카드 한 장의 소유자는 `ApplicationMember` 한 명이다.
- 카드번호와 발급 결과 이미지는 `Application`이 아니라 `ApplicationMember`에 저장한다.
- 단체 신청도 구성원별 별도 Application을 만들지 않는다.

### Application 상태

```text
PAYMENT_PENDING
    │ 관리자 입금 확인
    ▼
RECEIVED
    │ 검토 시작
    ▼
REVIEWING
    ├── 사진 반려 ──▶ PHOTO_REJECTED ── 재업로드 ──▶ REVIEWING
    └── 사진 승인 ──▶ NAME_EDITING
                              │ 전원 작명 완료
                              ▼
                          PRODUCING
                              │ 전원 카드 생성 완료
                              ▼
                          COMPLETED

각 진행 단계 ── 취소 정책 충족 ──▶ CANCELLED
```

허용되지 않은 상태 전이는 `INVALID_STATUS_TRANSITION`으로 거절한다. 상태를 직접 대입하거나 public setter로 변경하지 않고 Entity의 의미 있는 메서드를 사용한다.

예:

```java
application.confirmPayment();
application.startReview();
application.rejectPhoto(reason);
application.approvePhoto();
application.completeNaming();
application.completeIssuing();
application.cancel(reason);
```

### 단체 신청 일괄성

- 검토·사진 반려·작명 완료·카드 발급 상태는 `Application` 전체 단위다.
- 일부 구성원만 먼저 `COMPLETED` 처리하지 않는다.
- 한 명이라도 사진 검토를 통과하지 못하면 신청 전체를 `PHOTO_REJECTED`로 처리한다.
- 한 명이라도 필수 작명 필드가 없으면 `PRODUCING`으로 전환하지 않는다.
- `ApplicationMember`에는 별도 workflow status를 만들지 않는다.

### 불변조건

- `cardTypeId`, `applicationType`, `issueType`, `totalQuantity`, `totalPrice`는 신청 생성 시 필수다.
- `cardDesignId`는 신청 생성 시 `null`이며 관리자가 이후 배정한다.
- `totalPrice`는 신청 건별 사전 상담으로 확정된 최종 금액이다.
- 결제 금액을 `CardType.price × totalQuantity`로 자동 계산하거나 코드에 하드코딩하지 않는다.
- 상담 확정 이후 기존 신청 금액은 자동으로 변경하지 않는다.
- 학생증이 아니면 `studentId`, 학생용 `department`는 `null`이어야 한다.
- 개인 구성원의 `email`, `phone`은 중복 저장하지 않고 `Applicant` 값을 사용한다.
- 단체 구성원의 `email`, `phone`은 엑셀 행별 값을 저장할 수 있다.
- `birthTime`, `birthRegion`, `entryDate`는 선택값이다.
- `nationality`는 ISO 3166-1 alpha-2 형식을 사용한다.

---

## 4.3 Payment 모듈

### 책임

- 신청별 입금자명을 저장·수정한다.
- 관리자의 무통장입금 확인 결과와 확인 시각을 관리한다.

### 결제 방식

- PG 또는 가상계좌 자동 연동을 사용하지 않는다.
- 회사의 고정 계좌에 사용자가 무통장입금한다.
- 관리자가 입금자명을 통장 내역과 대조해 수동 확인한다.

### 데이터 규칙

- `Application`과 `Payment`는 1:1이다.
- `Payment.depositorName`은 신청인 이름과 달라도 된다.
- 결제 확인 여부의 기준값은 `Application.paymentStatus`다.
- `Payment.confirmedAt`은 확인 일시 기록이며 별도 `isConfirmed` 필드를 만들지 않는다.

### 입금 확인 트랜잭션

다음 변경은 반드시 한 트랜잭션에서 함께 수행한다.

```text
Application.status: PAYMENT_PENDING → RECEIVED
Application.paymentStatus: WAITING → CONFIRMED
Payment.confirmedAt: null → 현재 시각
감사 로그 저장
```

일부만 반영되는 상태를 허용하지 않는다.

---

## 4.4 Card 모듈

### 책임

- 카드 종류와 표시용 가격 정보를 관리한다. 표시용 가격의 유지 여부와 용도는 정책 확정 전까지 [TBD]다.
- 카드 디자인과 앞·뒤 템플릿을 관리한다.
- 카드 필드별 렌더링 규칙을 제공한다.
- 구성원별 카드번호와 앞·뒤 이미지를 생성한다.

### 소유 데이터

- `CardType`
- `CardDesign`
- 코드/config 기반 `CardFieldDefinition`

### CardType 규칙

- 비즈니스 분기는 표시명 `name`이 아니라 변경 불가능한 `code`를 사용한다.
- 관리자가 `name`, `description`, `price`, `isActive`를 수정할 수 있다.
- 비활성 카드 종류는 신규 신청에 사용할 수 없다.
- 기존 신청은 카드 종류가 비활성화돼도 조회와 발급 이력을 유지한다.

### CardDesign 규칙

- 하나의 CardType은 여러 CardDesign을 가질 수 있다.
- 사용자는 CardDesign을 직접 선택하지 않는다.
- `Application.cardDesignId`는 관리자가 배정한다.
- 앞면과 뒷면 템플릿이 모두 있어야 카드 발급이 가능하다.
- 가로형은 `LANDSCAPE`, 세로형은 `PORTRAIT`로 구분한다.
- 카드 디자인 배정 시점은 현재 미결정이므로 별도 정책 결정 전 임의 구현하지 않는다.

### 카드 발급 규칙

- 부모 Application 상태가 `PRODUCING`일 때만 발급할 수 있다.
- 모든 구성원의 작명 필수값이 채워져 있어야 한다.
- 구성원별로 카드번호, 발급일, 앞면 경로, 뒷면 경로를 저장한다.
- 카드번호 형식은 `ROK-XXXXX-XXXX`다.
- 카드번호 채번 방식은 미결정이므로 순차 또는 무작위를 임의 선택하지 않는다.
- 십이간지는 DB에 저장하지 않고 `birthDate`에서 렌더링 시 계산한다.
- 모든 구성원의 이미지 생성과 저장이 성공한 뒤 Application을 `COMPLETED`로 전환한다.

### 학생증 전용 규칙

`CardType.code=STUDENT`인 경우에만 다음 값을 사용한다.

- 학번
- 학과
- 학교 로고
- 학교 직인

학생증이 아닌 카드에 위 값이 들어오면 무시하지 말고 입력 오류로 거절한다.

---

## 4.5 File 모듈

### 책임

- 사진, Excel, ZIP, 로고, 직인, 카드 템플릿을 저장한다.
- 파일 메타데이터와 실제 object storage 경로를 관리한다.
- 업로드·다운로드·삭제·임시 URL 생성 기능을 제공한다.

### UploadFile 사용 규칙

- `UploadFile`은 파일 메타데이터를 소유한다.
- 실제 바이트 데이터는 데이터베이스가 아니라 object storage에 저장한다.
- 원본 파일명은 표시용으로만 사용한다.
- 저장 경로와 저장 파일명은 서버가 생성한다.
- MIME type은 요청 헤더만 신뢰하지 않고 파일 signature도 검증한다.
- 경로 조작 문자열과 실행 가능한 파일을 거절한다.

### Application 파일 연결

| 역할 | 연결 방식 |
|---|---|
| 단체 제출 ZIP | `Application.submitFileId` |
| 로고 | `Application.logoFileId` |
| 직인 | `Application.sealFileId` |
| 구성원 사진 | `ApplicationMember.photoPath` |
| 발급 앞면 | `ApplicationMember.cardFrontPath` |
| 발급 뒷면 | `ApplicationMember.cardBackPath` |

역할이 고정된 로고·직인·제출 ZIP을 범용 조인 테이블로 추상화하지 않는다.

### DB와 파일 저장소의 일관성

DB 트랜잭션은 object storage 업로드를 롤백할 수 없다. 따라서 다음 순서를 사용한다.

```text
1. 파일 검증
2. 임시 경로 업로드
3. DB 트랜잭션에서 메타데이터/신청 저장
4. 성공 시 파일 확정
5. DB 실패 시 업로드 파일 보상 삭제
```

보상 삭제가 실패할 경우 정리 대상 로그를 남기고 정기 cleanup job에서 재처리한다.

---

## 4.6 Admin 모듈

### 책임

- 관리자 전용 유스케이스의 진입점을 제공한다.
- 신청 목록·상세 조회를 제공한다.
- 입금 확인, 사진 승인·반려, 작명, 카드 발급을 조정한다.
- 카드 종류와 디자인 기준정보를 관리한다.

Admin은 독립된 업무 데이터 모듈이라기보다 여러 도메인의 관리자 유스케이스를 조정하는 API/Application 모듈이다.

### 규칙

- `/api/admin/**`는 `ADMIN` 역할만 접근할 수 있다.
- Admin Controller가 Application Entity를 직접 수정하지 않는다.
- 입금 확인은 Payment/Application 서비스를 통해 수행한다.
- 작명은 ApplicationMember의 공개된 작명 유스케이스를 통해 수행한다.
- 카드 발급은 Card 발급 유스케이스를 통해 수행한다.
- 모든 관리자 변경 작업은 관리자 ID, 대상 ID, 작업 종류, 변경 시각을 감사 로그에 남긴다.

---

## 4.7 Review 모듈

✅ 2026-08-09 갱신: 모노레포에 동기화된 실제 프론트(카드종류 단일선택·사진 0~1장)를 기준으로 재설계 완료(`docs/specs/review/{data-model,api}.md`). CRUD 5개 API(등록/목록조회/단건조회/삭제/수정) 전부 설계 완료 — 구현 착수 대상.

### 책임

- 로그인 사용자의 후기(제목/신청유형/카드종류 1개/작성자 표시명/사진 0~1장/본문)를 저장한다.
- 후기 목록·상세를 비로그인 포함 누구나 조회할 수 있게 한다.
- 작성자 본인 또는 관리자가 수정·삭제할 수 있다.

### 소유 데이터

- `Review` 단일 엔티티(join 엔티티 없음 — 카드종류·사진이 단일값이라 `ReviewCardType`/`ReviewImage`가 불필요해졌다)

### 규칙

- `Review.user_id`(실제 작성 계정)와 `Review.author_display_name`(화면 표시용, 사용자가 직접 입력)은 별개다 — 로그인 이름을 자동으로 채우지 않는다.
- `Review.application_type`/`Review.card_type_id`는 실제 `Application` 레코드와 FK로 연결하지 않는 자기 신고(self-report) 값이다. 다만 `Application`과 동일하게 카드종류를 코드가 아니라 `CardType.id`(Long)로 직접 저장한다(§5.1 "모듈 간 영속 참조는 식별자를 기본으로 한다" 원칙과 동일).
- 사진은 `UploadFile`을 거치지 않고 `Review.image_path`에 S3 key만 직접 저장한다 — `ApplicationMember.photo_path`와 동일한 패턴("그 Entity 자체가 사진 1장을 표현하는 로우"일 때 가능한 방식). 이번 갱신으로 Review는 File 모듈(`UploadFile`)에 더 이상 의존하지 않는다.
- 등록 시점뿐 아니라 **수정 시점에도** `(application_type, card_type_id)` 조합의 자격검증을 다시 수행한다 — 수정 화면에서 이 두 값도 편집 가능하기 때문(`docs/specs/review/api.md` API 5 참고). 검증 기준은 항상 후기 작성자(`Review.user_id`) 본인의 신청 이력이며, 관리자가 대신 수정하는 경우에도 동일하다.

## 4.8 Board 모듈

✅ 2026-08-14 갱신: 공지사항/FAQ CRUD 5개 API(목록/단건/생성/수정/삭제) 전부 구현+테스트 완료(`docs/specs/board/{data-model,api}.md`). (Review는 위 4.7절로 분리, 별도 도메인)

### 책임

- 관리자가 작성한 공지사항(NOTICE)·FAQ를 `BoardType` enum 하나로 통합 관리한다(신규 게시판 종류가 생기면 enum 값만 추가 — 테이블 추가 없음).
- 게시글 목록·상세를 비로그인 포함 누구나 조회할 수 있게 한다.
- 게시글 생성·수정·삭제는 관리자만 할 수 있다.

### 소유 데이터

- `Board`(작성 관리자 `created_by_user_id`는 `Long`으로만 참조, 공개 응답에는 노출하지 않음)
- `BoardAttachment` — `Board:UploadFile` = 1:N join 엔티티, Review의 `ReviewImage`(구 설계)와 동일 성격. NOTICE만 사용, FAQ는 첨부파일 개념이 없음.

### 규칙

- Review와 달리 작성자 개념이 공개 응답에 없다 — `canEdit`/`canDelete` 같은 소유권 판단 필드 자체가 없다(프론트에 작성자 표시 UI가 없음, `data-model.md` §5.4).
- `BoardAttachment`는 `UploadFile`을 직접 참조하지 않고 join 엔티티를 거친다 — `docs/api/upload-file.md`의 "`UploadFile`은 아무것도 참조하지 않는 공용 메타데이터 테이블" 원칙 때문(Review의 구 `ReviewImage` 설계와 동일 이유).
- 관리자 CRUD 권한은 리소스 소유권이 아니라 "관리자냐 아니냐"만으로 결정되므로, Review의 `canEdit`/`canDelete`(서비스 레벨 판단)와 달리 `/api/admin/**` → `hasRole("ADMIN")` 라우트 레벨 강제(`SecurityConfig`) 하나로 충분하다. 컨트롤러·서비스에는 별도 권한 분기 코드가 없다. `arch.md` §4.6에 이미 있던 `/api/admin/**` 원칙이 실제 `SecurityConfig`에는 반영돼 있지 않던 공백을 이번 Board 구현에서 메웠다(이 프로젝트의 첫 관리자 전용 쓰기 API).
- NOTICE 첨부파일의 교체/추가/삭제 흐름(수정 API에서)은 아직 미확정 — `docs/specs/board/data-model.md` §6 참고. 이번 패스는 QnA(FAQ) 기준 CRUD 골격만 확정했고, 수정 API는 boardType/title/content만 재제출한다(첨부파일은 생성 시에만 다룬다).

---

## 5. 도메인 간 참조 규칙

## 5.1 기본 원칙

1. 모듈은 자신이 소유한 Entity만 변경한다.
2. 다른 모듈의 JPA Entity를 필드 타입으로 직접 보유하지 않는다.
3. 모듈 간 영속 참조는 식별자(`Long ...Id`)를 기본으로 한다.
4. 다른 모듈의 Repository를 직접 호출하는 대신 공개 Service 메서드를 사용한다.
5. 조회 화면에서 여러 모듈 데이터를 조합할 때는 해당 도메인 Service의 조회 메서드를 사용한다(6절 참고).
6. 양방향 JPA 연관관계를 만들지 않는다.
7. 순환 패키지 의존을 허용하지 않는다.

### 허용 예시

```java
class Application {
    private Long userId;
    private Long cardTypeId;
    private Long cardDesignId;
}
```

### 금지 예시

```java
class Application {
    @ManyToOne
    private User user;

    @ManyToOne
    private CardType cardType;
}
```

ID 참조를 사용하면 도메인 생명주기와 로딩 전략이 분리되고, 특정 화면을 위한 거대한 Entity graph가 만들어지는 것을 막을 수 있다.

## 5.2 허용 의존 방향

```text
api → service → repository/entity
                     │
                     ▼
                  infra (필요시)
```

- Entity/Repository는 `api` 패키지를 참조하지 않는다.
- Service는 Spring MVC 타입(`HttpServletRequest` 등)을 직접 다루지 않는다 — 필요한 값은 Controller가 파라미터로 넘긴다.
- `infra`(JWT/S3/Redis 등)는 필요한 도메인 Service에서 직접 주입해서 쓴다 — 인터페이스로 반드시 추상화할 필요는 없다(구현체가 둘 이상 될 가능성이 있을 때만 인터페이스 도입, `backend/honor-citizen/CLAUDE.md` 기준).
- `common`은 어떤 업무 도메인의 Entity도 참조하지 않는다.

## 5.3 모듈 의존 매트릭스

| 호출 주체 | 참조 가능 모듈 | 목적 |
|---|---|---|
| User | common | 예외, 공통 보안 타입 |
| Application | User | 활성 사용자·약관·이메일 검증 |
| Application | Card | 카드 종류 활성 여부 조회 (`CardType.price`는 신청 금액 자동 계산에 사용하지 않음) |
| Application | File | 사진/ZIP/로고/직인 저장 |
| Payment | Application | 입금 확인과 신청 상태 전이 |
| Card | Application | 발급 대상 구성원 조회·발급 결과 반영 |
| Card | File | 템플릿 및 발급 이미지 저장 |
| Admin | Application/Payment/Card | 관리자 유스케이스 조정 |
| Review | User | 작성자 계정 검증(표시 이름은 요청 값 그대로 저장, `User`를 조회는 하되 이름을 복사하진 않음) |
| Review | Card | 카드종류 존재 확인·표시명 조회(`CardType.id` 직접 참조, `Application`과 동일 패턴) |
| Review | Application | 자격검증(등록·수정 시 `Applicant`/`ApplicationMember`의 이메일과 대조해 실제 카드 발급 이력 확인) |

역방향 참조가 필요해지면 Entity나 Repository를 직접 공유하지 말고 다음 중 하나를 선택한다.

- 호출 방향을 재설계한다.
- 읽기 전용 Query Service를 둔다.
- 도메인 이벤트를 발행한다.
- 최소 데이터만 담은 Port 인터페이스를 정의한다.

## 5.4 Repository 접근 규칙

- Repository 인터페이스는 이를 소유한 도메인에 둔다.
- Repository 구현은 해당 도메인의 infrastructure에 둔다.
- 다른 도메인의 Repository를 생성자 주입하지 않는다.
- 단순 존재 확인도 공개 서비스 또는 조회 Port를 사용한다.
- 관리자 복합 목록처럼 다중 테이블 조인이 필수인 조회는 전용 Query Repository에서 처리할 수 있다.
- Query Repository는 조회 DTO/projection을 반환하고 Entity를 외부로 노출하지 않는다.

## 5.5 Entity 생명주기

| Entity | 생성·삭제 생명주기 소유자 |
|---|---|
| Applicant | Application |
| Receiver | Application |
| ApplicationMember | Application |
| Payment | Application 신청 흐름 |
| CardType | Card 독립 기준정보 |
| CardDesign | CardType에 종속된 기준정보 |
| UploadFile | File 모듈, 연결 주체가 사용 목적 보유 |

Application 삭제가 필요한 경우 하위 신청 데이터와 사진 정리 정책을 함께 적용한다. 결제·발급 이력이 있는 신청은 원칙적으로 물리 삭제하지 않고 상태로 보존한다.

---

## 6. 조회/변경 메서드 규칙

> ⚠️ 2026-08-01 정정: 별도 CommandService/QueryService 클래스로 나누는 구조였으나, 지금 규모에서는 과함 — **하나의 도메인 Service 클래스 안에서 메서드로만 구분**한다(`UserService.getMe()`/`updateMe()`/`withdraw()`처럼 조회·변경 메서드가 같은 클래스에 공존). Service가 감당하기 버거울 만큼 커지면 그때 분리한다.

변경(신청 생성/입금확인/사진승인·반려/작명/카드발급 등)은 Entity를 조회한 뒤 의미 있는 도메인 메서드를 호출해서 처리한다. 조회(사용자 신청 조회/관리자 목록·상세/카드 다운로드 정보 등)는 화면에 필요한 값만 담은 DTO(projection)를 직접 조회해도 된다 — 조회용으로 Aggregate 전체를 메모리에 로딩할 필요는 없다.

### 조회 모델 규칙

- 관리자 신청 목록은 Application, Applicant, CardType을 조인한 projection을 사용한다.
- 관리자 상세의 구성원 목록은 반드시 페이지네이션한다.
- presigned URL은 Entity에 저장하지 않고 응답 생성 시 발급한다.
- Entity를 JSON으로 직렬화하지 않는다.

---

## 7. 트랜잭션 규칙

## 7.1 기본 규칙

- 트랜잭션 경계는 도메인 Service(`@Transactional`)에 둔다.
- Controller와 Repository에 `@Transactional`을 선언하지 않는다.
- 조회는 `@Transactional(readOnly = true)`를 사용한다.
- 하나의 사용자 액션에서 함께 성공해야 하는 DB 변경은 하나의 트랜잭션으로 묶는다.
- 외부 API나 대용량 파일 처리 중 DB 트랜잭션을 오래 유지하지 않는다.

## 7.2 신청 생성

개인 신청은 다음 데이터를 한 트랜잭션에서 저장한다.

```text
Application 1
Applicant 1
Receiver 1
ApplicationMember 1
Payment 1
```

단체 신청은 파일 파싱·검증 완료 후 다음 데이터를 한 트랜잭션에서 저장한다.

```text
Application 1
Applicant 1
Receiver 1
ApplicationMember N
Payment 1
UploadFile 참조
```

단체 파일 파싱과 이미지 검증은 트랜잭션 밖에서 수행하고, 검증을 통과한 정규화 결과만 저장 단계에 전달한다.

## 7.3 카드 발급

카드 이미지 생성은 시간이 오래 걸리고 object storage를 사용하므로 HTTP/DB 트랜잭션 하나에 모든 작업을 무조건 묶지 않는다.

권장 흐름:

```text
1. 발급 가능 상태와 입력값 검증
2. 발급 작업 시작 기록
3. 구성원별 이미지 생성 및 임시 업로드
4. DB에 카드번호·발급일·경로 일괄 반영
5. Application COMPLETED 전환
6. 실패 시 임시 파일 보상 삭제 및 재시도 가능 상태 유지
```

현재 API는 동기 응답으로 설계되어 있어도 구성원 수가 많아지면 내부 job 방식으로 전환할 수 있도록 카드 렌더링 로직을 Controller에서 분리한다.

## 7.4 낙관적 잠금과 중복 요청

- 관리자 상태 변경이 동시에 실행될 수 있으므로 Application에 낙관적 잠금(`@Version`) 적용을 권장한다.
- 입금 확인, 작명 완료, 카드 발급 API는 동일 요청 재전송을 고려한다.
- 이미 목표 상태에 도달한 요청을 성공으로 볼지 오류로 볼지는 API별로 명시하되, 카드가 중복 발급되어서는 안 된다.
- 카드번호에는 DB unique 제약을 둔다.

---

## 8. API 계층 규칙

## 8.1 URL 경계

```text
/api/auth/**                    인증
/api/users/**                   사용자
/api/applications/**            사용자 신청
/api/payments/**                입금자명
/api/admin/applications/**      관리자 신청 처리
/api/admin/application-members/** 관리자 구성원 작명
/api/admin/card-types/**        카드 종류 관리
/api/admin/card-designs/**      카드 디자인 관리
```

## 8.2 DTO 규칙

- Request와 Response DTO를 분리한다.
- Entity를 API 응답으로 반환하지 않는다.
- API 필드명과 enum 값은 `docs/api/README.md`를 그대로 따른다.
- Request validation은 형식 검증에 집중한다.
- 상태 전이, 가격 계산, 소유권 같은 업무 검증은 Service/Domain에서 수행한다.
- 목록과 상세 응답 DTO를 분리한다.
- multipart 요청의 JSON과 파일 part를 명확하게 구분한다.

## 8.3 공통 응답

성공:

```json
{
  "success": true,
  "data": {}
}
```

실패:

```json
{
  "success": false,
  "errorCode": "INVALID_INPUT",
  "errorMessage": "요청 값이 올바르지 않습니다."
}
```

예외는 공통 `ErrorCode`와 전역 예외 처리기를 사용한다. 도메인별 개별 Exception class를 무분별하게 만들지 않는다.

## 8.4 인증과 소유권

- 사용자 API는 인증된 userId를 토큰 principal에서 얻는다.
- Request body나 query parameter의 userId를 신뢰하지 않는다.
- 사용자 신청 조회·수정 시 `application.userId == currentUserId`를 검증한다.
- 관리자 API는 `ADMIN` role을 검증한다.
- 존재하지만 권한이 없는 사용자 자원은 정보 노출 방지를 위해 API 정책에 따라 404로 응답할 수 있다.

---

## 9. 단체 신청 처리 규칙

## 9.1 처리 단계

```text
파일 업로드
  → 압축 구조 검증
  → Excel 추출
  → 행별 데이터 파싱
  → 사진 파일 매칭
  → 필드 및 카드 종류별 검증
  → 오류/미리보기 반환
  → 사용자 확인
  → 재검증
  → Application + Member N건 저장
```

검증과 확정 저장을 분리한다. 미리보기 호출은 Application을 생성하지 않는다.

## 9.2 파서 구조

```text
BulkUploadParser
├── ZipStructureValidator
├── ExcelRowReader
├── PhotoMatcher
├── MemberValidator
└── BulkValidationResult
```

파서가 JPA Repository를 호출하거나 DB를 수정하면 안 된다. 파싱 결과는 저장 모델이 아니라 정규화된 command/result 객체로 반환한다.

## 9.3 보안 및 자원 제한

- 허용 확장자와 MIME type을 제한한다.
- ZIP entry의 정규화 경로가 작업 디렉터리 밖을 가리키지 못하게 한다.
- 압축 파일 크기뿐 아니라 압축 해제 후 총 크기도 제한한다.
- entry 개수, Excel 행 수, 사진 수, 개별 파일 크기를 제한한다.
- 전체 ZIP과 모든 entry를 한꺼번에 메모리에 적재하지 않는다.
- 동일한 ID나 사진 파일명이 중복되면 오류 처리한다.
- 사진이 없거나 매칭되지 않은 행은 명확한 행 번호와 오류 코드를 반환한다.

## 9.4 검증 결과

검증 실패 시 가능한 모든 행 오류를 수집해 한 번에 반환한다. 시스템 오류와 사용자 데이터 오류를 구분한다.

```text
rowNumber
field
code
message
```

엑셀 파싱 실패 허용 비율인 기존 “30% 룰”은 현재 미결정이므로 정책 확정 전 적용하지 않는다.

---

## 10. 상태 변경과 감사 로그

다음 작업은 감사 대상이다.

- 입금 확인
- 사진 승인
- 사진 반려와 사유
- 작명 데이터 생성·수정
- 작명 완료
- 카드 디자인 배정
- 카드 발급
- 신청 취소
- 카드 종류 및 디자인 변경

감사 로그에는 최소 다음 정보를 저장한다.

- actor type 및 actor ID
- action type
- target type 및 target ID
- 변경 전 상태
- 변경 후 상태
- 사유 또는 변경 상세
- 발생 시각

상태 변경과 감사 로그 저장은 같은 DB 트랜잭션에서 처리한다. 민감 개인정보 전체를 로그 detail에 복사하지 않는다.

---

## 11. 스케줄러와 비동기 작업

### 필수 스케줄러

- 신청일로부터 3일 이내 입금되지 않은 `PAYMENT_PENDING` 신청 자동 취소
- 탈퇴 요청 후 7일이 지난 User 개인정보 익명화
- 고아 임시 파일 정리
- 실패한 파일 보상 삭제 재처리

### 규칙

- 스케줄러는 여러 서버 인스턴스에서 동시에 실행돼도 중복 처리되지 않아야 한다.
- 한 번에 전체 데이터를 로드하지 않고 배치 크기와 페이지를 사용한다.
- 처리 결과와 실패 원인을 로그로 남긴다.
- 실패한 한 건 때문에 전체 배치가 중단되지 않도록 격리한다.
- “영업일” 계산 기준과 공휴일 데이터 출처는 별도 정책으로 정의한다.

---

## 12. 데이터베이스 규칙

- 테이블과 컬럼은 `snake_case`를 사용한다.
- 모든 상태 enum은 문자열로 저장한다.
- 업무 식별자인 `application_number`, `card_number`에는 unique 제약을 둔다.
- 1:1 관계인 Applicant, Receiver, Payment의 `application_id`에는 unique 제약을 둔다.
- 가격은 부동소수점이 아닌 `DECIMAL`을 사용한다.
- 생성·수정 시각을 공통 auditing으로 관리한다.
- FK의 delete cascade는 법적·운영 이력 보존 정책을 확인한 뒤 제한적으로 사용한다.
- 운영 DB 스키마는 JPA `ddl-auto`가 아니라 migration 도구로 관리한다.
- 인덱스는 관리자 목록 필터와 신청 조회 경로를 기준으로 설계한다.

### 권장 인덱스

```text
Application(application_number) UNIQUE
Application(user_id, created_at)
Application(status, created_at)
Application(card_type_id, status)
ApplicationMember(application_id, id)
ApplicationMember(card_number) UNIQUE
Applicant(application_id) UNIQUE
Applicant(phone)
Payment(application_id) UNIQUE
CardType(code) UNIQUE
CardDesign(card_type_id, is_active)
```

---

## 13. 테스트 규칙

파일명/패키지는 `domain/{도메인}/entity`, `service`, `api` 대상 테스트를 각각 `{Class}Test.java`로 둔다(예: `UserTest`, `UserServiceTest`, `UserControllerTest` — User 도메인에 이미 이 패턴으로 존재).

### Entity Test (순수 단위 테스트, Spring 컨텍스트 없음)

- Application 상태 전이 전체
- 개인/단체 수량 불변조건
- 상담 확정 결제 금액 저장 및 하드코딩 방지
- 학생증 전용 필드 검증
- 작명 완료 조건

### Service Test (`@SpringBootTest`, 실제 Repository 사용)

- 사용자 소유권과 관리자 권한
- 신청 생성 트랜잭션
- 입금 확인 원자성
- 단체 일부 오류 처리
- 카드 중복 발급 방지

### Repository Test

- 관리자 목록 필터·검색·페이지네이션
- 구성원 페이지 조회
- unique/FK 제약
- 동시 상태 변경 충돌

### Controller Test (`@SpringBootTest` + `@AutoConfigureMockMvc`, MockMvc)

- `docs/api/README.md`의 HTTP status와 응답 필드
- 인증 없음, 역할 부족, 다른 사용자 자원 접근
- multipart 형식과 validation 오류
- 공통 예외 응답 형식

### Infrastructure Test

- object storage는 fake adapter를 사용한다.
- Redis 없이도 단위·슬라이스 테스트가 실행돼야 한다.
- 실제 외부 서비스를 호출하는 테스트는 일반 테스트와 분리한다.
- 카드 렌더링 결과는 크기·포맷·필수 영역을 검증한다.

---

## 14. 설정과 운영 규칙

- 비밀키, OAuth credential, 저장소 credential을 저장소에 커밋하지 않는다.
- 로컬·테스트·운영 profile을 분리한다.
- 가격, 파일 제한, URL, 만료시간을 코드에 하드코딩하지 않는다.
- 설정값은 타입 안전한 `@ConfigurationProperties`로 묶는다.
- 운영 환경에서 `ddl-auto=create/update`를 사용하지 않는다.
- 개인정보를 일반 application log에 출력하지 않는다.
- presigned URL과 JWT를 로그에 남기지 않는다.
- health check는 DB, Redis, object storage 의존성을 구분해서 제공한다.

---

## 15. 구현 금지 사항

- Controller에서 Entity 상태 직접 변경
- Controller에서 Repository 직접 호출
- Entity를 Response로 직접 반환
- 다른 도메인의 Entity에 public setter 호출
- 패키지 간 순환 의존
- 표시 문자열로 카드 종류 판별
- 단체 구성원마다 별도의 Application 생성
- ApplicationMember별 workflow status 추가
- 결제 확인 상태를 여러 boolean/enum으로 중복 관리
- 파일 원본명을 저장 경로로 직접 사용
- ZIP 전체를 무제한으로 메모리에 적재
- DB 트랜잭션이 object storage까지 자동 롤백된다고 가정
- 미결정 정책을 개발자가 임의로 확정

---

## 16. 미결정 사항

아래 항목은 `DB.md`와 `docs/api/README.md`에서도 확정되지 않았다. 구현 전 기획 결정을 받아야 한다.

| 항목 | 현재 상태 |
|---|---|
| 카드번호 채번 방식 | 형식만 `ROK-XXXXX-XXXX`로 확정 |
| CardDesign 배정 시점/API | 관리자가 배정한다는 원칙만 확정 |
| `Receiver.country` | 해외 배송 지원 여부에 따라 유지/삭제 |
| 실물 배송 흐름 | `MOBILE_AND_PHYSICAL`은 현재 범위 밖 |
| 신청 조회 본인인증 채널 | 이메일·전화번호 조합 미정 |
| 학생증 학번·학과 세부 형식 | 길이와 패턴 미정 |
| 학생증 디자인 | 시안 미도착 |
| 단체 신청 파싱 실패율 | 기존 30% 규칙 적용 여부 미정 |
| 일반 신청 정보 수정 API | 사진 외 수정 범위 미정 |
| 게시판 | 요구사항 확정 전 보류 |
| 입금 기한 계산 기준 | 신청일 포함 여부와 마감 시각은 [TBD] |

---

## 17. 구현 순서

1. DB 모델을 Application aggregate 구조로 정리한다.
2. 개인/단체 신청을 `Application 1 + Member 1/N` 구조로 통합한다.
3. 상태 enum과 전이 메서드를 확정된 흐름으로 교체한다.
4. 고정 계좌 무통장입금 모델로 Payment를 단순화한다.
5. 카드 4종과 CardDesign 기준정보를 구현한다.
6. 단체 검증과 확정 저장을 분리한다.
7. 관리자 조회·입금확인·사진검토·작명 API를 구현한다.
8. 카드 발급과 파일 보상 처리 구조를 구현한다.
9. 자동 취소·회원 익명화·고아 파일 정리 job을 구현한다.
10. 운영 migration, 보안, 통합 테스트를 완성한다.

---

## 18. 문서 관리 규칙

- API 요청·응답·HTTP status 변경은 먼저 `docs/api/README.md`에 반영한다.
- 컬럼·제약·관계·enum 변경은 먼저 `DB.md`에 반영한다.
- 모듈 경계·의존 방향·트랜잭션 원칙 변경은 본 문서에 반영한다.
- 세 문서가 충돌하는 상태로 구현을 시작하지 않는다.
- 미결정 항목은 `[TBD]`로 표시하고 임의 기본값으로 숨기지 않는다.
