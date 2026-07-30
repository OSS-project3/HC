# HARNESS.md — Claude Code 구현 지시서

## 시작 전 필수 준비

아래 파일을 전부 읽어라. 읽기 완료 전까지 코드 작성 금지.

```
AGENT.md          ← 전체 코드 작성 규칙
ARCHITECTURE.md   ← 패키지 구조 규칙
DB_RULES.md       ← 엔티티 관계 + DDL
API_SPEC.md       ← 구현할 API 명세
DOMAIN_APPLICATION.md
DOMAIN_KOREANNAME.md
DOMAIN_CITIZENCARD.md
DOMAIN_SHARELOG.md
```

읽기 완료 후 아래 내용을 확인하고 보고해라.

```
1. 전체 테이블 수
2. 전체 API 수 (사용자 / 관리자 구분)
3. 도메인 수
4. 구현 시 판단이 필요한 애매한 부분
```

보고 완료 후 구현 시작.

---

## 구현 규칙

- 각 단계 완료 후 다음 단계로 넘어가라
- 판단이 필요한 부분은 구현 멈추고 질문해라
- 절대 혼자 판단해서 구조 바꾸지 마라
- AGENT.md 규칙 위반 시 즉시 수정해라

---

## 구현 순서

### 1단계 — 프로젝트 설정

```
build.gradle 의존성 추가
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- mysql-connector-java
- flyway-core
- lombok
- springdoc-openapi-starter-webmvc-ui
- aws-java-sdk-s3
- jjwt (JWT)

application.yml 설정
- DB 연결 (MySQL)
- JPA 설정 (ddl-auto: validate)
- Flyway 설정
- S3 설정
- JWT 설정
```

완료 확인 후 2단계로.

---

### 2단계 — 공통 모듈

아래 순서대로 구현해라.

```
common/entity/BaseTimeEntity.java
common/response/ApiResponse.java
common/enums/ApplicationStatus.java
common/enums/Gender.java
common/enums/PaymentMethod.java
common/enums/PaymentStatus.java
common/enums/CarrierType.java
common/enums/EmailType.java
common/enums/AdminActionType.java
common/exception/GlobalExceptionHandler.java
common/exception/ApplicationNotFoundException.java
common/exception/CardAlreadyIssuedException.java
common/exception/InvalidStatusTransitionException.java
common/exception/ShippingLockedException.java
common/exception/PaymentAlreadyExistsException.java
common/util/CardNumberGenerator.java
```

**ApiResponse 형식**
```java
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "data": null,
  "errorCode": "ERROR_CODE",
  "errorMessage": "메시지" }
```

완료 확인 후 3단계로.

---

### 3단계 — Flyway 마이그레이션

DB_RULES.md의 DDL을 그대로 사용해라.
아래 순서 반드시 준수 (FK 순서).

```
V1__create_users.sql
V2__create_applications.sql
V3__create_korean_names.sql
V4__create_citizen_cards.sql
V5__create_shipping_addresses.sql
V6__create_payments.sql
V7__create_physical_orders.sql
V8__create_application_status_logs.sql
V9__create_admin_activity_logs.sql
V10__create_payment_logs.sql
V11__create_email_logs.sql
V12__create_indexes.sql
```

완료 확인 후 4단계로.

---

### 4단계 — 도메인 레이어

아래 순서대로 구현해라.
각 도메인마다 entity → repository → service → dto 순서로.

#### 4-1. user 도메인

```
domain/user/entity/User.java
domain/user/repository/UserRepository.java
domain/user/service/UserService.java
domain/user/dto/UserResponse.java
```

#### 4-2. application 도메인

```
domain/application/entity/Application.java
domain/application/repository/ApplicationRepository.java
domain/application/service/ApplicationService.java
domain/application/service/strategy/ApplicationCreateStrategy.java
domain/application/service/strategy/SingleApplicationStrategy.java
domain/application/service/strategy/BulkApplicationStrategy.java
domain/application/service/strategy/ApplicationStrategyFactory.java
domain/application/dto/ApplicationCreateRequest.java
domain/application/dto/ApplicationBulkResponse.java
domain/application/dto/ApplicationResponse.java
domain/application/dto/ApplicationDetailResponse.java
```

**전략 패턴 규칙**
```
if/switch 절대 금지
Map<ApplicationCreateType, Strategy> 로 분기
```

#### 4-3. koreanname 도메인

```
domain/koreanname/entity/KoreanName.java
domain/koreanname/repository/KoreanNameRepository.java
domain/koreanname/service/KoreanNameService.java
domain/koreanname/dto/KoreanNameAssignRequest.java
domain/koreanname/dto/KoreanNameResponse.java
```

#### 4-4. citizencard 도메인

```
domain/citizencard/entity/CitizenCard.java
domain/citizencard/repository/CitizenCardRepository.java
domain/citizencard/service/CitizenCardService.java
domain/citizencard/dto/CitizenCardResponse.java
domain/citizencard/dto/CitizenCardDownloadResponse.java
```

#### 4-5. shipping 도메인

```
domain/shipping/entity/ShippingAddress.java
domain/shipping/repository/ShippingAddressRepository.java
domain/shipping/service/ShippingService.java
domain/shipping/dto/ShippingAddressRequest.java
domain/shipping/dto/ShippingAddressResponse.java
```

#### 4-6. payment 도메인

```
domain/payment/entity/Payment.java
domain/payment/entity/PaymentLog.java
domain/payment/repository/PaymentRepository.java
domain/payment/repository/PaymentLogRepository.java
domain/payment/service/PaymentService.java
domain/payment/service/WebhookService.java
domain/payment/dto/VirtualAccountRequest.java
domain/payment/dto/VirtualAccountResponse.java
domain/payment/dto/WebhookRequest.java
domain/payment/dto/PaymentInfoResponse.java
```

#### 4-7. physicalorder 도메인

```
domain/physicalorder/entity/PhysicalOrder.java
domain/physicalorder/repository/PhysicalOrderRepository.java
domain/physicalorder/service/PhysicalOrderService.java
domain/physicalorder/dto/TrackingRegisterRequest.java
domain/physicalorder/dto/TrackingRegisterResponse.java
```

#### 4-8. log 도메인

```
domain/log/entity/ApplicationStatusLog.java
domain/log/entity/AdminActivityLog.java
domain/log/entity/EmailLog.java
domain/log/repository/ApplicationStatusLogRepository.java
domain/log/repository/AdminActivityLogRepository.java
domain/log/repository/EmailLogRepository.java
domain/log/service/ApplicationStatusLogService.java
domain/log/service/AdminActivityLogService.java
domain/log/service/EmailLogService.java
```

완료 확인 후 5단계로.

---

### 5단계 — 인프라 레이어

```
infra/storage/StorageService.java        ← 인터페이스
infra/storage/S3StorageService.java      ← S3 구현체
infra/security/SecurityConfig.java       ← Spring Security
infra/security/JwtTokenProvider.java     ← JWT 발급/검증
infra/security/JwtAuthFilter.java        ← JWT 필터
infra/security/GoogleOAuthService.java   ← 구글 토큰 검증
infra/email/EmailService.java            ← AWS SES 이메일 발송
```

**Security 설정 규칙**
```
/api/auth/google  → 인증 없음
/api/payments/webhook → 인증 없음 (토스 서명 검증)
/api/**           → USER 인증 필요
/admin/**         → ADMIN 인증 필요
```

완료 확인 후 6단계로.

---

### 6단계 — API 컨트롤러

API_SPEC.md 명세를 그대로 구현해라.
경로, 메서드, 요청/응답 형식 임의 변경 금지.

#### 6-1. 사용자 API

```
api/AuthController.java
- POST /api/auth/google
- POST /api/auth/terms
- POST /api/auth/refresh
- POST /api/auth/logout

api/UploadController.java
- POST /api/uploads/photo

api/ApplicationController.java
- POST /api/applications
- POST /api/applications/bulk
- POST /api/applications/{id}/shipping
- PATCH /api/applications/{id}/shipping
- GET  /api/applications/{id}/payment-info

api/PaymentController.java
- POST /api/payments/virtual-account
- POST /api/payments/webhook

api/MyPageController.java
- GET  /api/my/applications
- GET  /api/my/applications/{id}
- PATCH /api/my/applications/{id}/photo
- GET  /api/my/applications/{id}/card/download
```

#### 6-2. 관리자 API

```
api/admin/AdminAuthController.java
- POST /admin/auth/login

api/admin/AdminApplicationController.java
- GET  /admin/applications
- GET  /admin/applications/{id}
- POST /admin/applications/{id}/photo-reject
- POST /admin/applications/{id}/korean-name
- POST /admin/applications/{id}/issue-card
- PATCH /admin/applications/{id}/tracking

api/admin/AdminDashboardController.java
- GET /admin/dashboard/stats
```

완료 확인 후 7단계로.

---

### 7단계 — 설정 클래스

```
config/JpaConfig.java       ← Auditing 설정
config/SwaggerConfig.java   ← API 문서
config/WebConfig.java       ← CORS 설정
config/S3Config.java        ← S3 빈 등록
```

---

### 8단계 — 최종 검증

아래 항목을 전부 확인하고 보고해라.

```
체크리스트
─────────────────────────────
□ API_SPEC.md 전체 API 구현 여부 대조
□ DB_RULES.md 테이블 전부 Entity로 구현됐는지
□ AGENT.md 금지 사항 위반 없는지
  - Set 사용 금지
  - for/switch 문 금지
  - Setter 공개 금지
  - System.out.println 금지
  - 필드 주입 금지
□ 모든 연관관계 fetch join 처리됐는지
□ 트랜잭션 경계 Service에만 있는지
□ 공통 응답 ApiResponse 래퍼 전체 적용됐는지
□ 에러 응답 errorCode + errorMessage 형식인지
□ ApplicationStatus 전이 규칙 지켜졌는지
```

이상 없으면 구현 완료 보고.
이상 있으면 수정 후 재보고.

---

## 구현 중 판단 필요 시

아래 상황에서는 반드시 멈추고 질문해라.

```
- API_SPEC.md에 없는 API가 필요할 때
- DB_RULES.md에 없는 컬럼이 필요할 때
- 도메인 간 의존 방향이 애매할 때
- 상태 전이 규칙이 불명확할 때
- 보안 관련 판단이 필요할 때
```

혼자 판단해서 진행하지 마라.
