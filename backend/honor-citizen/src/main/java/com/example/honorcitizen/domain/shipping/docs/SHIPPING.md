# DOMAIN_SHIPPING.md — 배송지 도메인

---

## 1. 책임

실물 시민증 카드 배송을 위한 배송지 정보를 관리한다.
Application과 1:1, User와 N:1로 연결되며, 운송장 등록 이후에는 수정이 불가능하다.
국내 단일 배송 정책으로 운영하며, 배송비와 예상 배송일은 서버 상수로 반환한다.

---

## 2. 패키지 위치

```
domain/shipping/
├── entity/
│   └── ShippingAddress.java
├── repository/
│   └── ShippingAddressRepository.java
├── service/
│   └── ShippingService.java
└── dto/
    ├── ShippingAddressRequest.java
    ├── ShippingAddressRegisterResponse.java
    └── ShippingAddressUpdateResponse.java
```

---

## 3. 엔티티

DB 컬럼 기준으로 정의. `country`는 국내 전용으로 제거.
`Application`, `User`는 `Long` ID가 아닌 객체 직접 참조(`@ManyToOne`)로 연결.

```java
@Entity
@Table(name = "shipping_addresses")
public class ShippingAddress extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;    // Application 직접 참조 (1:1)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                  // User 직접 참조 (본인 검증용)

    @Column(nullable = false)
    private String recipientName;       // 수령인 이름

    @Column(nullable = false)
    private String zipCode;             // 우편번호

    @Column(nullable = false)
    private String address;             // 기본 주소

    private String addressDetail;       // 상세 주소 (nullable)

    @Column(nullable = false)
    private String email;               // 이메일

    @Column(nullable = false)
    private String phone;               // 전화번호

    @Column(nullable = false)
    private boolean isLocked;           // 수정 잠금 여부 (DEFAULT false)

    // 수정 잠금 (운송장 등록 시 호출)
    public void lock() {
        this.isLocked = true;
    }

    // 본인 여부 검증
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    // Application 상태 조회 (서비스에서 직접 사용)
    public ApplicationStatus getApplicationStatus() {
        return this.application.getStatus();
    }

    // 배송지 수정 (잠금 상태 검증 포함)
    public void update(String recipientName, String zipCode,
                       String address, String addressDetail,
                       String email, String phone) {
        if (this.isLocked) {
            throw new ShippingLockedException();
        }
        this.recipientName = recipientName;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.email = email;
        this.phone = phone;
    }
}
```

---

## 4. 유저 흐름

```
신청 생성 완료 (application 존재)
        ↓
배송지 등록
POST /api/applications/{applicationId}/shipping
→ ShippingAddress 생성 (isLocked = false)
→ 고정 배송비/소요일 반환
→ shippingAddressId, estimatedDeliveryDays,
  shippingFee, isLocked 반환

(필요 시) 배송지 수정
PATCH /api/applications/{applicationId}/shipping
→ 상태가 PENDING / REVIEWING / PHOTO_REJECTED 이고
  isLocked = false 인 경우만 수정 가능
→ shippingAddressId, updatedAt, isLocked 반환

관리자 운송장 번호 등록 시
PATCH /admin/applications/{id}/tracking
→ ShippingAddress.lock() 호출
→ isLocked = true 로 변경
→ 이후 수정 불가
```

---

## 5. 주요 비즈니스 규칙

### 배송지 등록

- Application이 존재해야 등록 가능
- Application 1개당 ShippingAddress는 1개만 허용
- 이미 등록된 경우 `SHIPPING_ALREADY_EXISTS` (409) 에러 반환
- 등록 시 `isLocked = false` 로 초기화

### 배송지 수정

- `isLocked = true` 이면 수정 불가 → `SHIPPING_LOCKED` (400) 에러
- 신청 상태가 `PENDING`, `REVIEWING`, `PHOTO_REJECTED` 이 아니면 수정 불가 → `EDIT_PERIOD_EXPIRED` (400) 에러
- 잠금 검증은 Entity 내부 `update()` 메서드에서 처리
- 상태 검증은 ShippingService에서 처리

### 배송비 / 예상 배송일

- 국내 단일 고정값으로 서비스 상수로 관리
- DB에 저장하지 않고 응답 시 반환
- `shippingFee` 는 Payment 생성 시 반영

| 구분    | 배송비    | 예상 소요일 |
| ----- | ------ | ------- |
| 한국 전국 | 3,000원 | 1~3일    |

### 수정 잠금

- 관리자가 운송장 번호 등록 시 자동으로 `isLocked = true`
- 잠금 이후 어떠한 수정도 불가

---

## 6. Service 메서드

| 메서드 | 설명 | 트랜잭션 |
| --- | --- | --- |
| `register(applicationId, userId, request)` | 배송지 등록 + 고정 배송비 반환 | `@Transactional` |
| `update(applicationId, userId, request)` | 배송지 수정 | `@Transactional` |
| `lock(application)` | 배송지 잠금 (운송장 등록 시) | `@Transactional` |
| `findByApplication(application)` | 배송지 조회 | `@Transactional(readOnly = true)` |

> 내부적으로 `applicationId`, `userId`로 각각 `Application`, `User` 엔티티를 조회한 뒤 사용.
> `calculateShippingFee()`는 고정 상수 반환으로 단순화하여 별도 메서드 불필요.

---

## 7. Repository

```java
public interface ShippingAddressRepository
    extends JpaRepository<ShippingAddress, Long> {

    Optional<ShippingAddress> findByApplication(Application application);

    boolean existsByApplication(Application application);
}
```

---

## 8. DTO

### ShippingAddressRequest

```java
public class ShippingAddressRequest {

    @NotBlank
    private String recipientName;

    @NotBlank
    private String zipCode;

    @NotBlank
    private String address;

    private String addressDetail;       // 선택

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;
}
```

> `country` 필드 제거. 국내 전용으로 서버에서 `"KR"` 고정 처리.

### ShippingAddressRegisterResponse

```java
// API_SPEC 3-1 Response 기준
public class ShippingAddressRegisterResponse {
    private Long shippingAddressId;
    private String estimatedDeliveryDays;   // 고정값 "1~3일"
    private int shippingFee;                // 고정값 3000
    private boolean isLocked;              // 항상 false
}
```

### ShippingAddressUpdateResponse

```java
// API_SPEC 3-2 Response 기준
public class ShippingAddressUpdateResponse {
    private Long shippingAddressId;
    private LocalDateTime updatedAt;
    private boolean isLocked;              // 항상 false
}
```

---

## 9. 연관 API

| 메서드   | 경로                                   | 설명      | 인증 |
| ----- | ------------------------------------ | ------- | -- |
| POST  | `/api/applications/{id}/shipping`    | 배송지 등록  | ✅  |
| PATCH | `/api/applications/{id}/shipping`    | 배송지 수정  | ✅  |

---

## 10. 에러 케이스

| 에러 코드                     | HTTP | 상황                                          |
| ------------------------- | ---- | --------------------------------------------- |
| `SHIPPING_ALREADY_EXISTS` | 409  | 이미 배송지 등록됨                                   |
| `SHIPPING_NOT_FOUND`      | 404  | 배송지 정보 없음                                    |
| `SHIPPING_LOCKED`         | 400  | 운송장 등록 이후 수정 불가 (`isLocked = true`)          |
| `EDIT_PERIOD_EXPIRED`     | 400  | 수정 가능 상태(`PENDING`/`REVIEWING`/`PHOTO_REJECTED`)가 아님 |
| `APPLICATION_NOT_FOUND`   | 404  | 존재하지 않는 신청                                   |
| `UNAUTHORIZED_ACCESS`     | 403  | 본인 신청이 아님                                    |

---

## 11. 도메인 간 의존 관계

```
ShippingService
    → ApplicationRepository  (Application 엔티티 조회 — 존재 여부 + 상태 검증)
    → UserRepository         (User 엔티티 조회 — 본인 검증)

PhysicalOrderService (운송장 등록 시)
    → ShippingService.lock(application) 호출
```

---

## 12. DB 스키마 (SHIPPING_ADDRESSES)

> `application_id` UNIQUE 제약으로 신청 1건당 배송지 1개 보장
> `user_id` FK로 본인 검증 직접 가능
> `is_locked = 1` 이면 수정 불가 (운송장 등록 이후)

| 필드명              | 타입           | 제약                   | 설명                          | 엔티티 필드              |
| ---------------- | ------------ | -------------------- | --------------------------- | ------------------- |
| `id`             | BIGINT       | PK, AUTO_INCREMENT   | 고유번호                        | `id`                |
| `application_id` | BIGINT       | NOT NULL, UNIQUE, FK | Application (1:1)           | `application`       |
| `user_id`        | BIGINT       | NOT NULL, FK         | User (N:1, 본인 검증용)          | `user`              |
| `recipient_name` | VARCHAR(100) | NOT NULL             | 수령인 이름                      | `recipientName`     |
| `zip_code`       | VARCHAR(20)  | NOT NULL             | 우편번호                        | `zipCode`           |
| `address`        | VARCHAR(500) | NOT NULL             | 기본 주소                       | `address`           |
| `address_detail` | VARCHAR(200) | NULL                 | 상세 주소 (선택)                  | `addressDetail`     |
| `email`          | VARCHAR(100) | NOT NULL             | 이메일                         | `email`             |
| `phone`          | VARCHAR(30)  | NOT NULL             | 전화번호                        | `phone`             |
| `is_locked`      | TINYINT(1)   | NOT NULL, DEFAULT 0  | 수정 잠금. 운송장 등록 시 `1`로 변경    | `isLocked`          |
| `created_at`     | DATETIME     | NOT NULL             | 생성 일시                       | (BaseTimeEntity)    |
| `updated_at`     | DATETIME     | NOT NULL             | 수정 일시                       | (BaseTimeEntity)    |

```sql
CREATE TABLE shipping_addresses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    application_id  BIGINT       NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL,
    recipient_name  VARCHAR(100) NOT NULL,
    zip_code        VARCHAR(20)  NOT NULL,
    address         VARCHAR(500) NOT NULL,
    address_detail  VARCHAR(200),
    email           VARCHAR(100) NOT NULL,
    phone           VARCHAR(30)  NOT NULL,
    is_locked       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shipping_application
        FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_shipping_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 13. Claude Code 구현 지시

### 구현 순서

1. `domain/shipping/entity/ShippingAddress.java`
2. `domain/shipping/repository/ShippingAddressRepository.java`
3. `domain/shipping/dto/ShippingAddressRequest.java`
4. `domain/shipping/dto/ShippingAddressRegisterResponse.java`
5. `domain/shipping/dto/ShippingAddressUpdateResponse.java`
6. `domain/shipping/service/ShippingService.java`
7. `api/ShippingController.java`

### 구현 규칙

- `isLocked` 검증은 Entity 내부 `update()` 메서드에서 처리
- Application 상태 검증은 `ShippingService`에서 처리
- 배송비 / 예상 배송일은 서비스 내 상수로 관리 (`SHIPPING_FEE = 3000`, `ESTIMATED_DELIVERY_DAYS = "1~3일"`)
- `country` 필드는 엔티티 및 DTO에서 제거
- `if` / `switch` / `for` 금지
- Setter 공개 금지