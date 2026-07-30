# DOMAIN_APPLICATION.md — 신청 도메인

## 1. 책임

사용자가 명예 시민증 발급을 신청할 때 입력한 정보를 관리한다.
전체 서비스 흐름의 핵심 중심 엔티티로, 모든 도메인이 Application을 기준으로 연결된다.
단건 신청(폼 입력)과 대량 신청(엑셀+ZIP)을 전략 패턴으로 처리한다.

---

## 2. 패키지 위치

```
domain/application/
├── entity/
│   └── Application.java
├── repository/
│   └── ApplicationRepository.java
├── service/
│   ├── ApplicationService.java
│   └── strategy/
│       ├── ApplicationCreateStrategy.java      ← 인터페이스
│       ├── SingleApplicationStrategy.java      ← 단건 구현체
│       ├── BulkApplicationStrategy.java        ← 대량 구현체
│       └── ApplicationStrategyFactory.java     ← Map 기반 팩토리
└── dto/
    ├── ApplicationCreateRequest.java
    ├── ApplicationBulkResponse.java
    ├── ApplicationBulkFailure.java
    ├── ApplicationResponse.java
    ├── ApplicationSummaryResponse.java
    └── ApplicationDetailResponse.java
```

---

## 3. 엔티티

```java
@Entity
@Table(name = "applications")
public class Application extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private String nationality;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private LocalTime birthTime;

    @Column(nullable = false)
    private String birthRegion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private String photoPath;

    private String photoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    // 상태 변경 메서드 (setter 금지, 전이 규칙 엔티티 내부에서 검증)
    public void startReview() {
        validateTransition(ApplicationStatus.REVIEWING);
        this.status = ApplicationStatus.REVIEWING;
    }

    public void rejectPhoto() {
        validateTransition(ApplicationStatus.PHOTO_REJECTED);
        this.status = ApplicationStatus.PHOTO_REJECTED;
    }

    public void resubmitPhoto(String newPhotoPath, String newPhotoId) {
        validateTransition(ApplicationStatus.PENDING);
        this.status = ApplicationStatus.PENDING;
        this.photoPath = newPhotoPath;
        this.photoId = newPhotoId;
    }

    public void markCardReady() {
        validateTransition(ApplicationStatus.CARD_READY);
        this.status = ApplicationStatus.CARD_READY;
    }

    public void startShipping() {
        validateTransition(ApplicationStatus.SHIPPING);
        this.status = ApplicationStatus.SHIPPING;
    }

    public void completeDelivery() {
        validateTransition(ApplicationStatus.DELIVERED);
        this.status = ApplicationStatus.DELIVERED;
    }

    public void cancel() {
        if (this.status == ApplicationStatus.SHIPPING ||
            this.status == ApplicationStatus.DELIVERED) {
            throw new InvalidStatusTransitionException(
                this.status, ApplicationStatus.CANCELLED
            );
        }
        this.status = ApplicationStatus.CANCELLED;
    }

    private void validateTransition(ApplicationStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(this.status, next);
        }
    }
}
```

---

## 4. 상태 흐름

```
단건 신청
PENDING        → 결제 완료, 관리자 검토 대기
    ↓
REVIEWING      → 관리자 검토 중
    ↓
PHOTO_REJECTED → 사진 재요청 (PENDING으로 복귀 가능)
    ↓
CARD_READY     → 디지털 시민증 생성 완료
    ↓
SHIPPING       → 실물 카드 배송 중
    ↓
DELIVERED      → 배송 완료

대량 신청
DRAFT          → 업체 업로드 완료 (결제 전)
    ↓
PENDING        → 결제 완료 후 (이후 단건과 동일)

모든 상태 → CANCELLED (SHIPPING 이전까지만 가능)
```

**허용 전이 규칙**

| 현재 상태 | 전이 가능 상태 |
|-----------|---------------|
| `DRAFT` | `PENDING`, `CANCELLED` |
| `PENDING` | `REVIEWING`, `CANCELLED` |
| `REVIEWING` | `PHOTO_REJECTED`, `CARD_READY`, `CANCELLED` |
| `PHOTO_REJECTED` | `PENDING` |
| `CARD_READY` | `SHIPPING` |
| `SHIPPING` | `DELIVERED` |
| `DELIVERED` | 없음 |
| `CANCELLED` | 없음 |

---

## 5. 유저 흐름

### 단건 신청 흐름

```
1. 사진 사전 업로드
   POST /api/uploads/photo
   → photoId 발급

2. 신청 정보 입력
   POST /api/applications
   → nameEn, nationality, birthDate, birthTime
   → birthRegion, gender, photoId

3. 배송지 입력
   POST /api/applications/{id}/shipping

4. 결제 금액 확인
   GET /api/applications/{id}/payment-info

5. 가상계좌 발급
   POST /api/payments/virtual-account
   → 입금 기한 내 입금

6. 웹훅 수신 후 PENDING 상태로 전환
   → 관리자 처리 대기
```

### 대량 신청 흐름

```
1. 업체가 ZIP 파일 준비
   upload.zip
   ├── data.xlsx
   └── photos/
       ├── john.jpg
       └── jane.jpg

2. ZIP 업로드
   POST /api/applications/bulk
   → 성공 N건 / 실패 N건 응답

3. 실패 건 재업로드 (선택)

4. 결제 후 DRAFT → PENDING 전환
```

### 사진 재요청 흐름

```
관리자가 사진 재요청
POST /admin/applications/{id}/photo-reject
→ 상태: PHOTO_REJECTED
→ 사용자 이메일 발송

사용자가 사진 재업로드
PATCH /api/my/applications/{id}/photo
→ 상태: PENDING 복귀
→ 관리자 검토 재시작

※ 5-2 신청 상세 조회 응답의 배송 상태 필드명: orderStatus (API_SPEC 기준)
   PREPARING / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED
```

---

## 6. 주요 비즈니스 규칙

### 중복 신청
- PENDING / REVIEWING / PHOTO_REJECTED 상태인 신청이 있으면 중복 신청 불가
- CARD_READY / DELIVERED 상태는 완료된 건이므로 재신청 허용
- 중복 시 `DUPLICATE_APPLICATION` 에러 반환

### 사진 검증
- 허용 형식: JPG, PNG, WEBP
- 최대 크기: 10MB
- photoId는 String 타입
- 다른 유저의 photoId 사용 불가 → `PHOTO_NOT_FOUND` 에러
- 사전 업로드 만료 시 → `PHOTO_EXPIRED` 에러

### 약관 동의 검증
- 신청 생성 전 반드시 약관 동의 여부 확인
- 미동의 시 → `TERMS_NOT_AGREED` 에러
- AOP로 처리하여 Service 진입 전 검증

### 대량 업로드
- 부분 성공 허용 (성공 N건 / 실패 N건)
- 건별 독립 트랜잭션 처리
  `@Transactional(propagation = Propagation.REQUIRES_NEW)`
- 전체 실패 시 → `ALL_FAILED` 에러

### ZIP 파일 보안
- 압축 해제 후 크기 제한 (500MB)
- 파일 수 제한 (최대 200개)
- 허용 파일: .xlsx 1개 + photos/ 하위 JPG/PNG만
- Zip Slip 공격 방지 (경로 탐색 차단)
- `.exe`, `.sh` 등 실행 파일 포함 시 즉시 거부

### 상태 변경 자동 로그
- 모든 상태 변경 시 ApplicationStatusLog 자동 기록
- Spring Event로 처리 (도메인 간 직접 의존 금지)
- `changed_by`: SYSTEM / ADMIN / USER 구분하여 기록

### 취소 정책
- SHIPPING 이후 취소 불가
- 취소 시 결제 환불 처리 연동 필요 (PaymentService 호출)

---

## 7. 전략 패턴 구조

```java
// 인터페이스
public interface ApplicationCreateStrategy {
    List<Application> create(ApplicationCreateContext context);
}

// 단건 구현체
@Component
public class SingleApplicationStrategy
    implements ApplicationCreateStrategy {

    @Override
    public List<Application> create(ApplicationCreateContext context) {
        // 폼 데이터 → Application 1개 생성
    }
}

// 대량 구현체
@Component
public class BulkApplicationStrategy
    implements ApplicationCreateStrategy {

    @Override
    public List<Application> create(ApplicationCreateContext context) {
        // ZIP 압축 해제 → 엑셀 파싱 → 사진 매핑 → Application N개 생성
    }
}

// 팩토리 (if/switch 금지 → Map으로 분기)
@Component
public class ApplicationStrategyFactory {

    private final Map<ApplicationCreateType, ApplicationCreateStrategy> strategies;

    public ApplicationStrategyFactory(
        SingleApplicationStrategy single,
        BulkApplicationStrategy bulk) {

        this.strategies = Map.of(
            ApplicationCreateType.SINGLE, single,
            ApplicationCreateType.BULK, bulk
        );
    }

    public ApplicationCreateStrategy getStrategy(ApplicationCreateType type) {
        return strategies.get(type);
    }
}
```

---

## 8. Service 메서드

| 메서드 | 설명 | 트랜잭션 |
|--------|------|----------|
| `createSingle(userId, request)` | 단건 신청 생성 | `@Transactional` |
| `createBulk(userId, file)` | 대량 신청 생성 (ZIP) | 건별 독립 트랜잭션 |
| `getMyApplications(userId)` | 내 신청 목록 조회 | `@Transactional(readOnly = true)` |
| `getMyApplicationDetail(userId, applicationId)` | 내 신청 상세 조회 | `@Transactional(readOnly = true)` |
| `reuploadPhoto(userId, applicationId, photo)` | 사진 재업로드 | `@Transactional` |
| `getAdminApplications(pageable, filter)` | 어드민 목록 조회 | `@Transactional(readOnly = true)` |
| `getAdminApplicationDetail(applicationId)` | 어드민 상세 조회 | `@Transactional(readOnly = true)` |
| `startReview(applicationId, adminId)` | 검토 시작 | `@Transactional` |
| `rejectPhoto(applicationId, adminId, reason)` | 사진 재요청 | `@Transactional` |
| `validateTermsAgreed(userId)` | 약관 동의 검증 | `@Transactional(readOnly = true)` |
| `validateDuplicate(userId)` | 중복 신청 검증 | `@Transactional(readOnly = true)` |

---

## 9. Repository

```java
public interface ApplicationRepository
    extends JpaRepository<Application, Long> {

    // 내 신청 목록 (fetch join으로 N+1 방지)
    @Query("""
        SELECT a FROM Application a
        LEFT JOIN FETCH a.user
        WHERE a.user.id = :userId
        ORDER BY a.createdAt DESC
        """)
    List<Application> findAllByUserId(@Param("userId") Long userId);

    // 신청 상세 (연관관계 전부 fetch join)
    @Query("""
        SELECT a FROM Application a
        LEFT JOIN FETCH a.user
        WHERE a.id = :id
        """)
    Optional<Application> findByIdWithUser(@Param("id") Long id);

    // 중복 신청 검증
    boolean existsByUserIdAndStatusIn(
        Long userId,
        List<ApplicationStatus> statuses
    );

    // 어드민 목록 (동적 쿼리 → QueryDSL 사용)
    Page<Application> findAll(Specification<Application> spec, Pageable pageable);
}
```

---

## 10. DTO

### ApplicationCreateRequest

```java
public class ApplicationCreateRequest {

    @NotBlank
    private String nameEn;

    @NotBlank
    private String nationality;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotNull
    private LocalTime birthTime;

    @NotBlank
    private String birthRegion;

    @NotNull
    private Gender gender;

    @NotBlank
    private String photoId;        // String 타입
}
```

### ApplicationResponse (생성 응답)

```java
// API_SPEC 2-2 Response 기준
public class ApplicationResponse {
    private Long applicationId;
    private String initialStatus;   // 생성 직후 상태 (PENDING)
    private LocalDateTime createdAt;
}
```

### ApplicationSummaryResponse (목록 응답)

```java
// API_SPEC 5-1 Response 기준
public class ApplicationSummaryResponse {
    private Long applicationId;
    private String status;
    private String koreanName;      // 한국 이름 전체 (미부여 시 null)
    private LocalDateTime createdAt;
    private String cardNumber;      // 카드 번호 (미발급 시 null)
}
```

---

### ApplicationBulkResponse (대량 생성 응답)

```java
public class ApplicationBulkResponse {
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<ApplicationBulkFailure> failures;
}

public class ApplicationBulkFailure {
    private int row;
    private String name;
    private String reason;
}
```

### ApplicationDetailResponse (상세 응답)

```java
// API_SPEC 5-2 Response 기준
public class ApplicationDetailResponse {
    private Long applicationId;
    private String status;
    private String nameEn;
    private String nationality;
    private KoreanNameDetail koreanName;     // 미부여 시 null
    private CitizenCardDetail citizenCard;  // 미발급 시 null
    private ShippingDetail shipping;        // 미등록 시 null
    private PaymentDetail payment;          // 미결제 시 null

    public static class KoreanNameDetail {
        private String fullNameKo;
        private String fullNameEn;
        private String meaning;
        private String nameOrigin;
    }

    public static class CitizenCardDetail {
        private String cardNumber;
        private String downloadUrl;
        private LocalDateTime issuedAt;
    }

    public static class ShippingDetail {
        private String trackingNumber;
        private String carrier;
        private String orderStatus;         // PREPARING / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED
        private LocalDate estimatedDelivery;
        private String trackingUrl;
    }

    public static class PaymentDetail {
        private String orderId;
        private Long paidAmount;
        private LocalDateTime paidAt;
    }
}
```

---

## 11. 에러 케이스

| 에러 코드 | HTTP | 상황 |
|-----------|------|------|
| `TERMS_NOT_AGREED` | 403 | 약관 미동의 상태에서 신청 |
| `DUPLICATE_APPLICATION` | 409 | 진행 중인 신청 존재 |
| `INVALID_INPUT` | 400 | 필수 항목 누락 / 미래 날짜 입력 |
| `PHOTO_NOT_FOUND` | 404 | 유효하지 않은 photoId |
| `PHOTO_EXPIRED` | 400 | 사전 업로드 만료된 사진 |
| `PHOTO_OWNERSHIP_MISMATCH` | 403 | 다른 유저 사진 사용 시도 |
| `INVALID_STATUS_TRANSITION` | 400 | 허용되지 않는 상태 전이 |
| `APPLICATION_NOT_FOUND` | 404 | 존재하지 않는 신청 |
| `ZIP_TOO_LARGE` | 413 | ZIP 파일 500MB 초과 |
| `INVALID_ZIP` | 400 | ZIP 형식 오류 |
| `EXCEL_NOT_FOUND` | 400 | ZIP 안에 엑셀 없음 |
| `EXCEL_PARSE_ERROR` | 400 | 엑셀 형식 오류 |
| `ALL_FAILED` | 400 | 대량 업로드 전체 실패 |
| `ZIP_SLIP_DETECTED` | 400 | 경로 탐색 공격 감지 |
| `CANNOT_CANCEL` | 400 | 취소 불가 상태 (SHIPPING 이후) |