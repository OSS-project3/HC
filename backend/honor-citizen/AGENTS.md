# AGENTS.md — 프로젝트 컨텍스트 메모

## 작업 시작 전 필수 확인 순서

새 도메인/API를 구현하기 전 반드시 아래 순서로 문서를 읽는다.

1. **`docs/api/API_SPEC.md`** — 구현할 엔드포인트의 Request/Response 필드명, 에러 코드 확인 (최우선)
2. **`docs/db/DB_RULES.md`** — 관련 엔티티의 컬럼명, 상태값, 제약조건 확인
3. **`docs/domain/DOMAIN_{도메인}.md`** — 해당 도메인 흐름 명세 확인 (없으면 구현 전 먼저 작성)
4. **`docs/agent/AGENT.md`** — 코드 작성 원칙, 네이밍 컨벤션, 금지 사항 확인
5. **`docs/architecture/Architecture.md`** — 현재 파일 구조 확인 (기존 파일과 충돌 방지)
6. **이 파일 (AGENTS.md)** — 구현 현황, 주요 결정 사항, 주의사항 확인

> 도메인 명세(`DOMAIN_*.md`)가 없는 도메인은 구현 전 반드시 명세 파일부터 작성하고 사용자 확인을 받는다.

---

## 문서 기준 우선순위

코드 작성 시 문서 간 충돌이 있을 때:
1. `API_SPEC.md` — Request/Response 필드명, 에러 코드 최우선
2. `DB_RULES.md` — 엔티티 컬럼명, 상태값, 제약조건
3. `AGENT.md` — 코드 작성 원칙, 네이밍 컨벤션

---

## 프로젝트 개요

한국을 방문한 외국인에게 한국식 이름을 부여하고 명예 카드(시민증/학생증/출입증)를 발급하는 웹 서비스.
Spring Boot 4.x (Java 21) + H2(로컬) / PostgreSQL(운영) + Spring Security OAuth2.

### 카드 종류
- **명예시민증** (Honor Citizen Card)
- **학생증** (Student Card)  
- **출입증** (Access Card)

---

## 사용자 신청 프로세스

### 개별 신청 (개인 — 폼 형식)

```
① 카드 종류 선택 (시민증/학생증/출입증)
② "개인 신청" 선택
③ 정보 입력 (영문이름, 생년월일, 국적, 성별, 출생지역)
④ 사진 업로드
⑤ 최종 확인
⑥ 신청 완료 → 배송지 입력 → 결제 (가상계좌)
⑦ 입금 확인 → 자동으로 "검토 중" 상태 전환
⑧ (관리자가 이름 작명) → 카드 준비 완료 → 다운로드
```

### 단체 신청 (법인/단체 — 엑셀 + ZIP)

```
① 카드 종류 선택 (시민증/학생증/출입증)
② "법인/단체 신청" 선택
③ "엑셀 템플릿 다운로드" 
   → 빈 엑셀 파일 다운로드 (컬럼명: 영문이름, 생년월일, 국적, 성별, 출생지역)
④ 사용자가 엑셀 채우기 + 사진 파일 준비
⑤ ZIP 파일 생성 (엑셀 + 사진들)
   - 사진 파일명에 식별자 포함 (ex: ID_001_이름.jpg)
   - 엑셀의 ID 컬럼과 파일명의 ID가 매칭됨
⑥ "ZIP 업로드"
⑦ 프리뷰 & 검증 화면
   - 데이터 미리보기 (몇 명 샘플)
   - 검증 오류 표시 (형식/필수 항목/사진 파일 매칭)
   - 모두 통과하면 ✅ 표시
⑧ "최종 확인" 버튼 → 신청 완료
⑨ 배송지 입력 → 결제 (가상계좌)
⑩ 입금 확인 → 모든 신청자 자동으로 "검토 중" 상태 전환
⑪ (관리자가 한 명씩 이름 작명) → 일괄 발급 → ZIP 다운로드 (모든 카드 포함)
```

---

## 로컬 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 환경변수는 `src/main/resources/application-local.properties`에 직접 기입 (git 제외됨)
- `spring-dotenv`가 Spring Boot 4.x에서 동작하지 않으므로 `.env` 자동 로딩 불가
- H2 콘솔: http://localhost:8080/h2-console

---

## OAuth2 구조

- Google: `/oauth2/authorization/google` → `OAuth2SuccessHandler`
- Naver: `/oauth2/authorization/naver` → `OAuth2SuccessHandler`
- 성공 핸들러에서 provider 판별 (`getAuthorizedClientRegistrationId`)
- JWT를 HttpOnly 쿠키로 발급 (accessToken 15분, refreshToken 14일)
- User 식별 키: `(oauth_id, oauth_provider)` 복합 유니크

---

## JWT 보안 구조 (docs/jwt/jwt.md 기준)

현재 JWT 구조는 `docs/jwt/jwt.md`의 OWASP 기반 요구사항을 우선 기준으로 한다.

### Token 발급/저장

- OAuth2 로그인 성공 시 `OAuth2SuccessHandler`가 `accessToken`, `refreshToken`을 모두 HttpOnly 쿠키로 발급한다.
- `/api/auth/refresh`는 `refreshToken` 쿠키를 검증한 뒤 새 `accessToken`, 새 `refreshToken`을 모두 HttpOnly 쿠키로 재발급한다.
- accessToken은 API 인증 전용이며 만료시간은 15분이다.
- refreshToken은 accessToken 재발급 전용이며 만료시간은 14일이다.
- 쿠키 옵션 기본값은 `HttpOnly=true`, `Secure=true`, `SameSite=Strict`, `Path=/`이다.
- 로컬 HTTP 개발에서는 `application-local.properties`로 `app.cookie.secure=false`만 분기한다.

### JWT Claim/검증

- JWT payload에는 민감정보를 넣지 않는다. `email`, `phone`, `address`, `refreshToken`, `password` 저장 금지.
- accessToken/refreshToken 공통 claim은 `sub(userId)`, `iss`, `role`, `typ`, `jti`, `iat`, `exp`이다.
- refreshToken에는 refresh session 식별용 `sid`를 추가한다.
- `JwtTokenProvider`는 HS256을 고정해 서명/검증한다. JWT header의 `alg` 값을 신뢰해서 알고리즘을 선택하지 않는다.
- 모든 요청의 JWT 검증은 signature, issuer, expiration, subject, token type, role, 사용자 존재 여부 순서로 수행한다.

### Refresh Token 관리

- Refresh Token Rotation 적용: refresh 성공 시 기존 refresh token은 `ROTATED`, 새 refresh token은 `ACTIVE`로 저장한다.
- Refresh Token DB 저장은 `refresh_token_sessions` 테이블에서 관리한다.
- Redis에는 현재 refresh session의 유효한 `jti`와 access token blacklist를 저장한다.
- 이전 refreshToken 재사용 감지 시 해당 사용자의 ACTIVE refresh session을 모두 `REVOKED`로 바꾸고 Redis session도 삭제한다.
- 로그아웃 시 refresh session 전체 무효화, access token blacklist 등록, 쿠키 삭제를 수행한다.

---

## 문서 위치

| 문서 | 경로 | 대상 |
|------|------|------|
| 행동 규칙 | `docs/agent/AGENT.md` | 개발자 |
| 아키텍처 | `docs/architecture/Architecture.md` | 아키텍트 |
| DB 규칙 | `docs/db/DB_RULES.md` | DBA |
| API 명세 | `docs/api/API_SPEC.md` | 프론트엔드 |
| 유저 도메인 | `docs/domain/DOMAIN_USER.md` | 백엔드 |
| 신청 도메인 | `docs/domain/APPLICATION.md` | 백엔드 |
| 한국이름 도메인 | `docs/domain/DOMAIN_KOREANNAME.md` | 백엔드 |
| 시민증 도메인 | `docs/domain/DOMAIN_CITIZENCARD.md` | 백엔드 |
| **Bulk 검증 규칙 (신규)** | **`docs/bulk/BULK_VALIDATION_RULES.md`** | **사장님 확인 필요** |

---

## 구현 현황 (2026-07-01 기준)

### 완료된 패키지/파일 목록

```
api/
  AuthController.java              — 약관동의(1-1), 토큰갱신(1-3), 로그아웃(1-4)
  ApplicationController.java       — 단건신청(2-2), 목록(5-1), 상세(5-2), 사진재업로드(5-3), 카드다운로드(5-4)
  UploadController.java            — 사진업로드(2-1)
  ShippingController.java          — 배송지등록(3-1), 배송지수정(3-2)
  BulkOrderController.java         — 대량신청(2-3), 대량카드다운로드
  PaymentController.java           — 결제정보조회, 가상계좌발급, 토스웹훅
  admin/
    AdminApplicationController.java — 한국이름등록(6-5-1), 한국이름수정(6-5-2), 시민증발급(6-6)

common/
  entity/BaseTimeEntity.java
  enums/UserRole.java, ApplicationStatus.java, Gender.java
  enums/PaymentStatus.java, PaymentMethod.java
  exception/CustomException.java, ErrorCode.java, GlobalExceptionHandler.java
  response/ApiResponse.java

domain/user/
  entity/User.java, RefreshTokenSession.java, RefreshTokenStatus.java
  repository/UserRepository.java, RefreshTokenSessionRepository.java
  service/UserService.java
  dto/TermsAgreeRequest/Response.java, TokenRefreshRequest/Response.java

domain/application/
  entity/Application.java          — createSingle/createBulk(DRAFT), approvePayment(DRAFT→PENDING)
  repository/ApplicationRepository.java
  service/ApplicationService.java  — createSingle 시 ApplicationStatusLog 저장
  dto/ApplicationCreateRequest.java, ApplicationResponse.java
      ApplicationSummaryResponse.java (목록용), ApplicationDetailResponse.java (상세용)
      ApplicationListResponse.java, ApplicationPhotoReuploadResponse.java

domain/photo/
  entity/PhotoUpload.java               — 1시간 만료, 소유자 검증 포함
  repository/PhotoUploadRepository.java
  service/PhotoUploadService.java
  dto/PhotoUploadResponse.java

domain/shipping/
  entity/ShippingAddress.java           — applicationId/userId ID 참조만 저장
  repository/ShippingAddressRepository.java
  service/ShippingService.java          — 배송비 25000원, 예상 배송일 "1~3일" 상수
  dto/ShippingAddressRequest.java, ShippingAddressRegisterResponse.java, ShippingAddressUpdateResponse.java

domain/koreanname/
  entity/KoreanName.java                — application 1:1, familyName/givenName/fullNameKo/fullNameEn/meaning/nameOrigin
  repository/KoreanNameRepository.java
  service/KoreanNameService.java        — registerKoreanName/updateKoreanName 시 AdminActivityLog 저장
  dto/KoreanNameRegisterRequest/Response.java, KoreanNameUpdateRequest/Response.java

domain/citizencard/
  entity/CitizenCard.java               — cardNumber(HN-KR-YYMM-NNNN), imagePath, meaningPath, zipPath, presignedUrl
  repository/CitizenCardRepository.java
  service/CitizenCardService.java       — issueCard(adminId, applicationId), ApplicationStatusLog + AdminActivityLog 저장
  dto/CitizenCardIssueResponse.java, CitizenCardDownloadResponse.java

domain/bulk/
  entity/BulkOrder.java                 — userId, zipPath, totalCount, successCount, failCount, status
  repository/BulkOrderRepository.java
  service/BulkApplicationService.java   — ZIP+XLSX 파싱, Application.createBulk(DRAFT) 일괄 저장
  dto/BulkApplicationCreateResponse.java, BulkDownloadResponse.java

domain/payment/
  entity/Payment.java                   — applicationId(UNIQUE), orderId(UNIQUE), paymentKey, depositorName
                                          serviceAmount/shippingFee/totalAmount, paymentStatus, expiredAt
  entity/PaymentLog.java                — eventType별 결제 이벤트 감사 로그
  repository/PaymentRepository.java, PaymentLogRepository.java
  service/PaymentService.java           — getPaymentInfo, issueVirtualAccount, handleWebhook
                                          결제완료 시 DRAFT→PENDING + ApplicationStatusLog 저장
  dto/PaymentInfoResponse.java, VirtualAccountRequest/Response.java, TossWebhookRequest.java

domain/log/
  entity/ApplicationStatusLog.java      — applicationId, fromStatus, toStatus, changedBy, reason
  entity/AdminActivityLog.java          — adminId, actionType, targetId, detail
                                          상수: KOREAN_NAME_REGISTER, KOREAN_NAME_UPDATE, CARD_ISSUE, PHOTO_REJECT, TRACKING_REGISTER
  entity/EmailLog.java                  — applicationId, emailType, recipient, status(PENDING/SENT/FAILED)
                                          상수: PAYMENT_COMPLETE, PHOTO_REJECTED, CARD_READY, SHIPPING_STARTED, DELIVERED
  repository/ApplicationStatusLogRepository.java, AdminActivityLogRepository.java, EmailLogRepository.java

infra/security/
  SecurityConfig.java, JwtTokenProvider.java, JwtAuthFilter.java
  OAuth2SuccessHandler.java, OAuth2FailureHandler.java
  AuthCookieManager.java, TokenSessionStore.java, AuthTokens.java

infra/storage/
  StorageService.java (인터페이스), S3StorageService.java, S3Config.java

infra/card/
  CardImageData.java, CardImageContext.java
  CardImageGenerator.java (인터페이스), DefaultCardImageGenerator.java
  ZodiacCalculator.java                 — 생년도 → 띠 키 (2020=rat 기준)

infra/toss/
  TossPaymentsClient.java (인터페이스), TossPaymentsClientImpl.java
  TossPaymentsProperties.java           — @ConfigurationProperties(prefix="app.toss")
  TossVirtualAccountRequest.java, TossVirtualAccountResult.java
```

### API 구현률: 19 / 33 (58%)

| 번호 | 엔드포인트 | 상태 |
|------|-----------|------|
| 1-1 | POST /api/auth/terms | ✅ |
| 1-3 | POST /api/auth/refresh | ✅ |
| 1-4 | POST /api/auth/logout | ✅ |
| 2-1 | POST /api/uploads/photo | ✅ |
| 2-2 | POST /api/applications (개인 신청) | ✅ |
| 2-3 | POST /api/applications/bulk (단체 신청 업로드) | ✅ |
| 2-3-1 | GET /api/applications/bulk/template | ❌ |
| 2-3-2 | POST /api/applications/bulk/preview | ❌ |
| 3-1 | POST /api/applications/{id}/shipping | ✅ |
| 3-2 | PATCH /api/applications/{id}/shipping | ✅ |
| 4-1 | GET /api/applications/{id}/payment-info | ✅ |
| 4-2 | POST /api/payments/virtual-account | ✅ |
| 4-3 | POST /api/payments/webhook | ✅ |
| 5-1 | GET /api/my/applications | ✅ |
| 5-2 | GET /api/my/applications/{id} | ✅ |
| 5-3 | PATCH /api/my/applications/{id}/photo | ✅ |
| 5-4 | GET /api/my/applications/{id}/card/download | ✅ |
| 5-5 | GET /api/my/bulk-orders/{id}/cards/download | ✅ |
| 6-5-1 | POST /admin/applications/{id}/korean-name | ✅ |
| 6-5-2 | PATCH /admin/applications/{id}/korean-name | ✅ |
| 6-6 | POST /admin/applications/{id}/issue-card | ✅ |
| 나머지 14개 | — | ❌ |

---

## 다음 구현 순서 (우선순위 순)

### Phase 1 — 미구현 도메인 엔티티 + 레포지토리

| 도메인 | 엔티티 | 상태 | 비고 |
|--------|--------|------|------|
| korean_name | KoreanName | ✅ 완료 | |
| citizen_card | CitizenCard | ✅ 완료 | 카드번호 `HN-KR-YYMM-NNNN` |
| payment | Payment + PaymentLog | ✅ 완료 | TossPay 가상계좌 |
| bulk | BulkOrder | ✅ 완료 | ZIP+XLSX 일괄 신청 |
| log | ApplicationStatusLog, AdminActivityLog, EmailLog | ✅ 완료 | 동기 저장 |
| physical_order | PhysicalOrder | ❌ | EMS/DHL/FedEx/UPS 운송장 |

### Phase 2 — 어드민 API

| 엔드포인트 | 상태 |
|-----------|------|
| POST /admin/applications/{id}/korean-name | ✅ |
| PATCH /admin/applications/{id}/korean-name | ✅ |
| POST /admin/applications/{id}/issue-card | ✅ |
| POST /admin/auth/login | ❌ |
| GET /admin/applications (필터: status, nationality, 날짜) | ❌ |
| GET /admin/applications/{id} | ❌ |
| POST /admin/applications/{id}/photo-reject | ❌ |
| POST /admin/applications/{id}/tracking | ❌ |
| GET /admin/dashboard/stats | ❌ |
| GET /admin/bulk-orders/{id}/applications | ❌ (대량신청 목록 — 이름 배정 현황) |
| POST /admin/bulk-orders/{id}/issue-cards | ❌ (대량 일괄 시민증 발급) |

### Phase 3 — 사용자 나머지 API

**신규: 검증/신청 분리 단체 신청**

| 엔드포인트 | 상태 | 설명 |
|-----------|------|------|
| GET /api/applications/bulk/template | ❌ | 엑셀 템플릿 다운로드 |
| POST /api/applications/bulk/validate | ❌ | ZIP 검증 (읽기만, DB 수정 X) |
| POST /api/applications/bulk/confirm | ❌ | 재검증 + BulkOrder 생성 (@Transactional) |

**기존 API**

| 엔드포인트 | 상태 |
|-----------|------|
| GET /api/my/applications/{id}/card/download | ✅ |
| GET /api/applications/{id}/payment-info | ✅ |
| POST /api/payments/virtual-account (TossPay 가상계좌) | ✅ |
| POST /api/payments/webhook (입금 웹훅) | ✅ |

### Phase 4 — 이메일 알림 (5종)

결제완료 / 사진반려 / 카드준비 / 배송시작 / 배달완료
EmailLog 엔티티 완료 — 발송 서비스(JavaMailSender) 미구현

---

## Bulk 단체 신청 구조 설계 (2026-07-20 신규)

### 개요
단체 신청 프로세스를 **검증(Validation)과 신청 생성(Create) 2단계로 명확히 분리**합니다.

**최종 플로우**:
```
ZIP 업로드
   ↓
POST /api/applications/bulk/validate
   ↓
ValidationService (읽기만, DB 수정 X)
   ├─ Excel 파싱
   ├─ Row 검증
   ├─ 이미지 매칭
   └─ 오류 수집
   ↓
Error 또는 Preview JSON 반환
   ↓
사용자 확인
   ↓
POST /api/applications/bulk/confirm (같은 ZIP 재전송)
   ↓
ValidationService 재사용 (동일 로직)
   ↓
오류 없으면 @Transactional
   ├─ BulkOrder 생성
   ├─ Application N개 생성
   └─ DB Commit
   ↓
결제 진행
```

### 핵심 설계 원칙

**1️⃣ 책임 분리**
- **ValidationService**: 읽기만 (DB 수정 X, 부작용 없음)
- **BulkApplicationService**: 쓰기 (confirm 호출 시만 저장)

**2️⃣ DTO 분리**
```java
// 검증 응답
ValidationResponseDTO {
  status: "VALIDATED" | "VALIDATION_FAILED",
  validRows: Integer,
  invalidRows: Integer,
  errors: List<ErrorDetail>
}

// 신청 생성 응답
CreateResponseDTO {
  bulkOrderId: Long,
  cardType: CardType,
  totalCount: Integer,
  status: "DRAFT"
}
```

**3️⃣ 트랜잭션 분리**
```java
// validate: 트랜잭션 없음 (읽기만)
@PostMapping("/validate")
public ApiResponse<ValidationResponseDTO> validate(
    @RequestPart MultipartFile zip) {
    return bulkValidationService.validate(zip);
}

// confirm: @Transactional (쓰기)
@PostMapping("/confirm")
@Transactional
public ApiResponse<CreateResponseDTO> confirm(
    @AuthenticationPrincipal Long userId,
    @RequestPart MultipartFile zip) {
    
    ValidationResult result = bulkValidationService.validate(zip);
    
    if (result.hasErrors()) {
        throw new CustomException(ErrorCode.VALIDATION_FAILED);
    }
    
    return bulkApplicationService.createFromValidation(userId, result);
}
```

### 신규 API (2개)
| API | 메서드 | 경로 | 목적 |
|-----|--------|------|------|
| 템플릿 | GET | `/api/applications/bulk/template` | 엑셀 다운로드 |
| 검증 | POST | `/api/applications/bulk/validate` | ZIP 검증만 |
| 최종 확인 | POST | `/api/applications/bulk/confirm` | 신청 생성 |

### DB 변경 (최소)
```sql
-- bulk_orders에만 필드 추가
ALTER TABLE bulk_orders
ADD COLUMN card_type VARCHAR(50) DEFAULT 'CITIZEN_CARD',
ADD COLUMN status VARCHAR(50) DEFAULT 'DRAFT';
```

**테이블 추가 없음** ✅

### 운영 안정성
- ✅ 메모리 저장 없음
- ✅ DB 테이블 추가 없음
- ✅ 상태 관리 불필요
- ✅ 서버 재시작 안전 (상태 손실 없음)
- ✅ 로드밸런싱 안전 (validate/confirm이 다른 서버 가능)
- ✅ 메모리 누적 없음 (OOM 위험 0)

### 구현 일정
- **Week 1**: 
  - ValidationService (검증 로직)
  - 2개 API (validate, confirm)
  - 테스트 + 배포

**총 1주 예상** ✅

### 이전 문서 (참고용, 새 설계로 대체됨)
- ~~docs/design/BULK_PREVIEW_STRUCTURE.md~~ (2단계 Preview 저장 방식, 이제 불필요)
- ~~docs/domain/DOMAIN_BULK.md~~ (Preview DB 기반, 이제 불필요)
- ~~docs/db/BULK_MIGRATION_GUIDE.md~~ (BulkPreview 테이블, 이제 불필요)

---

## 알려진 설계 결정 & 주의사항

- **ApplicationStatus 전이 검증**: `Application.validateTransition()` 내부에서 `canTransitionTo()` 호출 — 상태 변경 시 엔티티 메서드(`markCardReady()` 등)만 사용
- **PhotoUpload 만료**: `expiresAt`은 업로드 시점 +1h, `ApplicationService`에서 만료 여부 체크 필요
- **Bulk vs Single**: `POST /api/applications`는 단건, `POST /api/applications/bulk`는 ZIP+XLSX 대량
- **개인 vs 단체 신청**: 폼 선택 단계에서 개인(단건)/법인(다건) 구분 — 개인은 JSON 폼, 단체는 ZIP+XLSX 처리
- **엑셀 템플릿 제공**: GET /api/applications/bulk/template에서 빈 엑셀 파일 자동 생성해 다운로드
  - 컬럼 (9개): ID | 영문이름 | 국적 | 생년월일 | 출생시간 | 출생지역 | 성별 | 한국입국일 | 주소
  - 한자이름은 사용자 입력 X → 관리자가 카드 생성 시 별도 지정
  - 발급일은 카드 발급 시 시스템에서 자동 생성 (사용자 입력 X)
- **Bulk 파일 구조**: Excel ZIP과 Photo ZIP을 별도로 업로드
  - Excel ZIP: applicants.xlsx (위 9개 컬럼 포함)
  - Photo ZIP: 1.jpg, 2.jpg, 3.jpg, ... (Excel ID와 매칭)
- **Bulk 식별자 매칭**: 엑셀의 ID 컬럼(1,2,3...) → 사진 파일명(1.jpg, 2.jpg...) 자동 매칭 (대소문자 무시, 확장자 무시)
- **검증과 신청 분리 (중요)**: 
  1️⃣ POST /api/applications/bulk/validate — ValidationService로 검증만 (DB 수정 X)
  2️⃣ POST /api/applications/bulk/confirm — 재검증 후 @Transactional로 신청 생성
- **ValidationService**: 읽기만, 부작용 없음, validate/confirm 모두에서 재사용
- **BulkApplicationService**: confirm 호출 시만 @Transactional로 DB 저장 (BulkOrder + Application)
- **DTO 분리**: ValidationResponseDTO (검증 결과) / CreateResponseDTO (신청 생성 결과)
- **Bulk DRAFT 상태**: Application은 confirm 단계에서 DRAFT로 생성 — TossPay 입금 완료(VIRTUAL_ACCOUNT_COMPLETED 웹훅) 시 DRAFT→PENDING 전이
- **Bulk 실패율 30% 룰**: XLSX 행 중 30% 이상 파싱 실패 시 validate/confirm 모두에서 에러 반환 — 30% 미만이면 유효한 행만 신청 생성
- **운영 DB**: `application-prod.properties` 미생성 — 운영 전환 시 PostgreSQL 설정 추가 필요
- **카드번호 형식**: `HN-KR-YYMM-NNNN` (연월 2+2자리 + 전체 누적 4자리 시퀀스)
- **카드번호 시퀀스**: `MAX(RIGHT(card_number, 4))` 네이티브 쿼리로 조회 (`CitizenCardRepository.findMaxSequence()`)
- **어드민 인증**: OAuth2 아님 — 별도 username/password + ADMIN role 체크 (`SecurityConfig` 수정 필요, 미구현)
- **배송지 구현**: `ShippingAddress`는 `Long applicationId`, `Long userId`만 저장 (Entity 직접 참조 없음)
- **배송지 API 경로**: `POST/PATCH /api/applications/{applicationId}/shipping`
- **배송지 수정 조건**: Application 상태가 `PENDING`, `REVIEWING`, `PHOTO_REJECTED` 중 하나이고 `isLocked=false`일 때만 가능
- **CitizenCard 이미지**: S3에서 템플릿/사진/띠 이미지 다운로드 실패 시 해당 요소 건너뜀 (카드 생성 중단 안 함)
- **presigned URL 갱신**: 다운로드 URL `urlExpiresAt`이 24시간 이내이면 `getDownloadUrl` 호출 시 자동 갱신
- **로그 저장 방식**: 동기(같은 트랜잭션) — ApplicationStatusLog는 상태 변경과 동일 tx, AdminActivityLog도 직접 저장
- **결제 금액**: 서비스비 30,000원 + 배송비 25,000원 = 55,000원 (application.properties로 분리, 환경변수 오버라이드 가능)
- **Toss 웹훅 서명 검증**: 현재 TODO 상태 — ContentCachingRequestWrapper 필터를 통한 raw body HMAC 검증 미구현
- **테스트**: AWS 환경변수 없이 전체 테스트 실행 시 컨텍스트 로딩 실패 — 더미 env 주입 필요
- **resubmitPhoto 상태 로그**: `ApplicationService.resubmitPhoto()`에서 PHOTO_REJECTED→PENDING 상태 로그 미저장 — 추후 추가 필요

---

## 미결정 사항 (사장님 확인 필요)

| 항목 | 내용 | 상태 | 우선순위 |
|------|------|------|----------|
| 카드 종류 | 명예시민증 / 학생증 / 출입증 모두 구현 확정 | ✅ | — |
| 카드 종류별 가격 | 시민증 30,000 + 배송비 25,000 / 학생증 가격 / 출입증 가격 | ❌ | 높음 |
| 카드 이미지 레이아웃 | 각 요소(사진, 한국이름, 영문이름, 카드번호, 국적, 띠, 발급기관, 발급일)의 px 좌표 및 폰트 크기 — `DefaultCardImageGenerator`에 반영 필요 | ❌ | 높음 |
| 카드 종류별 추가 항목 | 학생증 → 학교명, 학번? / 출입증 → 소속, 방문처? | ❌ | 중간 |
| 발급일 처리 방식 | 사용자 입력 X → 카드 발급 시 시스템에서 자동 생성 | ✅ | — |
| 한자 표기 | 엑셀 입력 X → 관리자가 카드 생성 시 별도 지정 | ✅ | — |
| 카드 템플릿 이미지 | 각 카드 종류별 배경 PNG 파일을 S3에 업로드 필요 | ❌ | 높음 |
| 카드 띠 이미지 12종 | S3 경로 `app.card.zodiac-key-prefix`/rat.png 등 12종 이미지 업로드 필요 | ❌ | 중간 |
| 카드 프린터 기종 | 사무실 PC에 연결할 카드 프린터 기종 선택 및 구입 | ❌ | 높음 |
| 주소 필드 | 엑셀에서 사용자 입력 (Address 컬럼, 카드 전면 출력) | ✅ | — |
| 국가 목록 | ISO 전체 국가 드롭다운 (백엔드 validation 포함 여부) | ✅ (전체 가능) | — |
| 엑셀 템플릿 컬럼 | ID / 영문이름 / 국적 / 생년월일 / 출생시간 / 출생지역 / 성별 / 한국입국일 / 주소 (9개) | ✅ | — |
| Bulk 검증 방식 | validate는 읽기만, confirm에서 재검증 | ✅ | — |
| Bulk DB 변경 | bulk_orders에만 필드 추가 (테이블 추가 X) | ✅ | — |
