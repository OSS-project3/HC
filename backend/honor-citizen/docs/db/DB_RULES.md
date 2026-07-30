# DB_RULES.md — 엔티티 관계와 제약조건

## 1. 전체 관계 요약

```
USERS (1) ──── (N) APPLICATIONS                ← 핵심 중심 엔티티
APPLICATIONS (1) ──── (1) KOREAN_NAMES         ← 이름 부여
APPLICATIONS (1) ──── (1) CITIZEN_CARDS        ← 시민증 발급
APPLICATIONS (1) ──── (1) SHIPPING_ADDRESSES   ← 배송지
APPLICATIONS (1) ──── (1) PAYMENTS             ← 결제
APPLICATIONS (1) ──── (1) PHYSICAL_ORDERS      ← 배송
APPLICATIONS (1) ──── (N) APPLICATION_STATUS_LOGS ← 상태 이력
APPLICATIONS (1) ──── (N) EMAIL_LOGS           ← 이메일 발송 이력
PAYMENTS     (1) ──── (N) PAYMENT_LOGS         ← 결제 이벤트 이력
USERS        (1) ──── (N) ADMIN_ACTIVITY_LOGS  ← 관리자 행동 이력
USERS        (1) ──── (N) PHOTO_UPLOADS         ← 사진 업로드 임시 보관
```

---

## 2. 테이블 상세

### USERS (사용자)

> Google OAuth 로그인 사용자 정보 및 약관 동의 이력 관리
> API: POST /api/auth/google, POST /api/auth/terms

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | - |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 | email |
| google_id | VARCHAR(100) | NOT NULL, UNIQUE | 구글계정 ID | - |
| name | VARCHAR(100) | NOT NULL | 이름 | - |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | 권한 (USER, ADMIN) | role |
| terms_agreed | TINYINT(1) | NOT NULL, DEFAULT 0 | 이용약관 동의 | termsAgreed |
| privacy_agreed | TINYINT(1) | NOT NULL, DEFAULT 0 | 개인정보 동의 | privacyAgreed |
| image_upload_agreed | TINYINT(1) | NOT NULL, DEFAULT 0 | 이미지 업로드 동의 | imageUploadAgreed |
| shipping_agreed | TINYINT(1) | NOT NULL, DEFAULT 0 | 배송 안내 동의 | shippingAgreed |
| terms_agreed_at | DATETIME | NULL | 약관 동의 일시 | agreedAt |
| created_at | DATETIME | NOT NULL | 생성 일시 | createdAt |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

```sql
CREATE TABLE users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    email                VARCHAR(100) NOT NULL UNIQUE,
    google_id            VARCHAR(100) NOT NULL UNIQUE,
    name                 VARCHAR(100) NOT NULL,
    role                 VARCHAR(20)  NOT NULL DEFAULT 'USER',
    terms_agreed         TINYINT(1)   NOT NULL DEFAULT 0,
    privacy_agreed       TINYINT(1)   NOT NULL DEFAULT 0,
    image_upload_agreed  TINYINT(1)   NOT NULL DEFAULT 0,
    shipping_agreed      TINYINT(1)   NOT NULL DEFAULT 0,
    terms_agreed_at      DATETIME,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    PRIMARY KEY (id)
);
```

---

### PHOTO_UPLOADS (사진 업로드 임시 보관)

> 사진 사전 업로드 후 신청 생성 전까지 임시 보관
> API: POST /api/uploads/photo
> 신청 생성 시 소유자 확인 · 만료 여부 검증 후 applications.photo_path에 경로 복사

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | - |
| user_id | BIGINT | NOT NULL, FK | 업로드한 사용자 ID | - |
| photo_id | VARCHAR(100) | NOT NULL, UNIQUE | 사진 고유 식별자 | photoId |
| photo_path | VARCHAR(500) | NOT NULL | S3 저장 경로 | - |
| photo_url | VARCHAR(1000) | NULL | 미리보기 URL | photoUrl |
| is_used | TINYINT(1) | NOT NULL, DEFAULT 0 | 신청에 사용 여부 | - |
| expires_at | DATETIME | NOT NULL | 유효 기간 만료 일시 (업로드 후 1시간) | expiresAt |
| created_at | DATETIME | NOT NULL | 생성 일시 | - |

**처리 흐름**
```
사진 업로드 시  → photo_uploads INSERT → photoId, expiresAt 반환
신청 생성 시    → photo_uploads에서 photoId 조회
               → user_id 일치 여부 검증 (소유자 확인)
               → expires_at 만료 여부 확인
               → applications.photo_path에 경로 복사
               → is_used = true 업데이트
만료 정리       → 스케줄러가 주기적으로
               is_used = false + expires_at 지난 것 S3 삭제
```

```sql
CREATE TABLE photo_uploads (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    photo_id    VARCHAR(100)  NOT NULL UNIQUE,
    photo_path  VARCHAR(500)  NOT NULL,
    photo_url   VARCHAR(1000),
    is_used     TINYINT(1)    NOT NULL DEFAULT 0,
    expires_at  DATETIME      NOT NULL,
    created_at  DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_photo_uploads_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

### APPLICATIONS (신청 — 핵심 중심 엔티티)

> 모든 도메인이 이 테이블을 기준으로 연결됨
> API: POST /api/applications, POST /api/applications/bulk

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | applicationId |
| user_id | BIGINT | NOT NULL, FK | 사용자 ID | - |
| name_en | VARCHAR(100) | NOT NULL | 영문 이름 | nameEn |
| nationality | VARCHAR(10) | NOT NULL | 국적 (ISO 3166-1 alpha-2) | nationality |
| birth_date | DATE | NOT NULL | 생년월일 | birthDate |
| birth_time | TIME | NOT NULL | 출생 시각 | birthTime |
| birth_region | VARCHAR(200) | NOT NULL | 출생 지역 | birthRegion |
| gender | VARCHAR(10) | NOT NULL | 성별 (MALE, FEMALE) | gender |
| photo_path | VARCHAR(500) | NULL | 사진 S3 경로 | photoUrl |
| photo_id | VARCHAR(100) | NULL | 사진 ID (String) | photoId |
| status | VARCHAR(30) | NOT NULL, DEFAULT 'PENDING' | 처리 상태 | status |
| created_at | DATETIME | NOT NULL | 생성 일시 | createdAt |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

**status 값 (API 응답 status 값과 동일)**
| DB 값 | API 값 | 설명 | 진입 시점 |
|------|------|------|----------|
| `DRAFT` | `DRAFT` | 대량 업로드 완료 (결제 전) | POST /api/applications/bulk |
| `PENDING` | `PENDING` | 결제 완료, 검토 대기 | 웹훅 수신 후 |
| `REVIEWING` | `REVIEWING` | 관리자 검토 중 | 관리자 액션 |
| `PHOTO_REJECTED` | `PHOTO_REJECTED` | 사진 재요청 | POST /admin/.../photo-reject |
| `CARD_READY` | `CARD_READY` | 디지털 시민증 생성 완료 | POST /admin/.../issue-card |
| `SHIPPING` | `SHIPPING` | 실물 카드 배송 중 | PATCH /admin/.../tracking |
| `DELIVERED` | `DELIVERED` | 배송 완료 | 스위트트래커 웹훅 |
| `CANCELLED` | `CANCELLED` | 취소 | 발송 전까지 가능 |

**상태 전이 규칙**
```
DRAFT          → PENDING        (웹훅 입금 완료 시)
PENDING        → REVIEWING      (관리자 검토 시작)
REVIEWING      → PHOTO_REJECTED (사진 재요청)
REVIEWING      → CARD_READY     (시민증 발급 완료)
PHOTO_REJECTED → PENDING        (사진 재업로드 완료)
CARD_READY     → SHIPPING       (운송장 번호 등록)
SHIPPING       → DELIVERED      (배송 완료)
모든 상태      → CANCELLED      (SHIPPING 이전까지만)
```

```sql
CREATE TABLE applications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    name_en      VARCHAR(100) NOT NULL,
    nationality  VARCHAR(10)  NOT NULL,
    birth_date   DATE         NOT NULL,
    birth_time   TIME         NOT NULL,
    birth_region VARCHAR(200) NOT NULL,
    gender       VARCHAR(10)  NOT NULL,
    photo_path   VARCHAR(500),
    photo_id     VARCHAR(100),
    status       VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_applications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

### KOREAN_NAMES (한국 이름)

> 관리자가 직접 작명한 한국 이름 정보
> API: POST /admin/applications/{id}/korean-name
> 등록 후 applicationStatus = REVIEWING 반환

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | koreanNameId |
| application_id | BIGINT | NOT NULL, UNIQUE, FK | 신청 ID | - |
| family_name | VARCHAR(10) | NOT NULL | 성 (한글, 1자) | familyName |
| given_name | VARCHAR(20) | NOT NULL | 이름 (한글, 1~2자) | givenName |
| full_name_ko | VARCHAR(30) | NOT NULL | 전체 이름 (한글) | fullNameKo |
| full_name_en | VARCHAR(100) | NOT NULL | 전체 이름 (로마자) | fullNameEn |
| meaning | TEXT | NOT NULL | 이름 의미 | meaning |
| name_origin | VARCHAR(100) | NULL | 이름 유래 (한자 등) | nameOrigin |
| created_at | DATETIME | NOT NULL | 생성 일시 | createdAt |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

```sql
CREATE TABLE korean_names (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    application_id  BIGINT       NOT NULL UNIQUE,
    family_name     VARCHAR(10)  NOT NULL,
    given_name      VARCHAR(20)  NOT NULL,
    full_name_ko    VARCHAR(30)  NOT NULL,
    full_name_en    VARCHAR(100) NOT NULL,
    meaning         TEXT         NOT NULL,
    name_origin     VARCHAR(100),
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_korean_names_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

---

### CITIZEN_CARDS (시민증)

> 발급된 시민증 이미지 및 ZIP 다운로드 정보 관리
> API: POST /admin/applications/{id}/issue-card
>      GET /api/my/applications/{id}/card/download
> 발급 시: 시민증 PNG + 이름 의미 카드 PNG + ZIP 생성 후 S3 저장
> 다운로드: ZIP Presigned URL 발급 (유효 7일)

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | - |
| application_id | BIGINT | NOT NULL, UNIQUE, FK | 신청 ID | - |
| card_number | VARCHAR(20) | NOT NULL, UNIQUE | 카드 번호 (HN-KR-YYMM-NNNN) | cardNumber |
| image_path | VARCHAR(500) | NOT NULL | 시민증 이미지 S3 경로 | - |
| name_meaning_path | VARCHAR(500) | NULL | 이름 의미 카드 S3 경로 | - |
| zip_path | VARCHAR(500) | NULL | ZIP 파일 S3 경로 | - |
| download_url | VARCHAR(1000) | NULL | Presigned ZIP 다운로드 URL | downloadUrl |
| url_expires_at | DATETIME | NULL | URL 만료 일시 | expiresAt |
| issued_at | DATETIME | NOT NULL | 발급 일시 | issuedAt |
| created_at | DATETIME | NOT NULL | 생성 일시 | createdAt |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

**카드 번호 생성 규칙**
```
형식: HN-KR-{YYMM}-{4자리 순번}
예시: HN-KR-2609-0001
순번: 전체 누적 (초기화 없음, MAX(RIGHT(card_number,4)) + 1)
```

**ZIP 파일 구성 (5-4 API 응답의 includes 필드와 일치)**
```
HN-KR-2609-0001.zip
├── HN-KR-2609-0001.png         ← 시민증 이미지 (image_path)
└── meaning_HN-KR-2609-0001.png ← 이름 의미 카드 (name_meaning_path)
```

```sql
CREATE TABLE citizen_cards (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    application_id     BIGINT        NOT NULL UNIQUE,
    card_number        VARCHAR(30)   NOT NULL UNIQUE,
    image_path         VARCHAR(500)  NOT NULL,
    name_meaning_path  VARCHAR(500),
    zip_path           VARCHAR(500),
    download_url       VARCHAR(1000),
    url_expires_at     DATETIME,
    issued_at          DATETIME      NOT NULL,
    created_at         DATETIME      NOT NULL,
    updated_at         DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_citizen_cards_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

---

### SHIPPING_ADDRESSES (배송지)

> 실물 카드 배송을 위한 주소 정보
> API: POST /api/applications/{id}/shipping
>      PATCH /api/applications/{id}/shipping
> is_locked = true 이면 수정 불가 (SHIPPING 이후)

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | shippingAddressId |
| application_id | BIGINT | NOT NULL, UNIQUE, FK | 신청 ID | - |
| recipient_name | VARCHAR(100) | NOT NULL | 수령인 이름 | recipientName |
| country | VARCHAR(10) | NOT NULL | 국가 코드 | country |
| zip_code | VARCHAR(20) | NOT NULL | 우편번호 | zipCode |
| address | VARCHAR(500) | NOT NULL | 기본 주소 | address |
| address_detail | VARCHAR(200) | NULL | 상세 주소 | addressDetail |
| email | VARCHAR(100) | NOT NULL | 이메일 | email |
| phone | VARCHAR(30) | NOT NULL | 전화번호 | phone |
| is_locked | TINYINT(1) | NOT NULL, DEFAULT 0 | 수정 잠금 여부 (SHIPPING 이후 true로 변경) | isLocked |
| created_at | DATETIME | NOT NULL | 생성 일시 | createdAt |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

```sql
CREATE TABLE shipping_addresses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    application_id  BIGINT       NOT NULL UNIQUE,
    recipient_name  VARCHAR(100) NOT NULL,
    country         VARCHAR(10)  NOT NULL,
    zip_code        VARCHAR(20)  NOT NULL,
    address         VARCHAR(500) NOT NULL,
    address_detail  VARCHAR(200),
    email           VARCHAR(100) NOT NULL,
    phone           VARCHAR(30)  NOT NULL,
    is_locked       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shipping_addresses_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

---

### PAYMENTS (결제)

> 결제 정보 관리
> API: GET /api/applications/{id}/payment-info
>      POST /api/payments/virtual-account
>      POST /api/payments/webhook (토스페이먼츠 → 우리 서버)

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | - |
| application_id | BIGINT | NOT NULL, UNIQUE, FK | 신청 ID | - |
| order_id | VARCHAR(100) | NOT NULL, UNIQUE | 주문 번호 | orderId |
| payment_method | VARCHAR(20) | NOT NULL | 결제 수단 | paymentMethod |
| payment_key | VARCHAR(200) | NULL | PG사 결제 키 | paymentKey |
| depositor_name | VARCHAR(100) | NULL | 입금자명 (가상계좌용) | depositorName |
| service_amount | BIGINT | NOT NULL | 서비스 금액 | serviceAmount |
| shipping_fee | BIGINT | NOT NULL | 배송비 | shippingFee |
| total_amount | BIGINT | NOT NULL | 총 결제 금액 | totalAmount |
| currency | VARCHAR(10) | NOT NULL, DEFAULT 'KRW' | 통화 단위 | currency |
| payment_status | VARCHAR(20) | NOT NULL | 결제 상태 | paymentStatus |
| receipt_url | VARCHAR(500) | NULL | 영수증 URL | receiptUrl |
| paid_at | DATETIME | NULL | 결제 일시 | paidAt |
| expired_at | DATETIME | NULL | 가상계좌 입금 기한 | expiredAt |
| created_at | DATETIME | NOT NULL | 생성 일시 | createdAt |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

**payment_method 값**
| 값 | 설명 |
|----|------|
| `VIRTUAL_ACCOUNT` | 가상계좌 입금 (토스페이먼츠) |
| `PAYPAL` | 페이팔 |

**payment_status 값 (API 응답 paymentStatus와 동일)**
| DB 값 | API 값 | 설명 | 진입 시점 |
|------|------|------|----------|
| `PENDING` | `PENDING` | 결제 대기 | 가상계좌 발급 직후 |
| `WAITING_DEPOSIT` | `WAITING_DEPOSIT` | 가상계좌 입금 대기 | 가상계좌 발급 완료 |
| `COMPLETED` | `COMPLETED` | 결제 완료 | 웹훅 수신 후 |
| `FAILED` | `FAILED` | 결제 실패 | 입금 기한 초과 등 |
| `CANCELLED` | `CANCELLED` | 결제 취소 | 신청 취소 시 |
| `REFUNDED` | `REFUNDED` | 환불 완료 | 환불 처리 후 |

```sql
CREATE TABLE payments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    application_id  BIGINT       NOT NULL UNIQUE,
    order_id        VARCHAR(100) NOT NULL UNIQUE,
    payment_method  VARCHAR(20)  NOT NULL,
    payment_key     VARCHAR(200),
    depositor_name  VARCHAR(100),
    service_amount  BIGINT       NOT NULL,
    shipping_fee    BIGINT       NOT NULL,
    total_amount    BIGINT       NOT NULL,
    currency        VARCHAR(10)  NOT NULL DEFAULT 'KRW',
    payment_status  VARCHAR(20)  NOT NULL,
    receipt_url     VARCHAR(500),
    paid_at         DATETIME,
    expired_at      DATETIME,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

---

### PHYSICAL_ORDERS (배송)

> 실물 카드 배송 정보 및 운송장 번호 관리
> API: PATCH /admin/applications/{id}/tracking
> 관리자가 운송장 번호 입력 시 생성 → status SHIPPING 변경

| 필드명             | 타입           | 제약                   | 설명       | API 필드명        |
| --------------- | ------------ | -------------------- | -------- | -------------- |
| id              | BIGINT       | PK, AUTO_INCREMENT   | 고유번호     | -              |
| application_id  | BIGINT       | NOT NULL, UNIQUE, FK | 신청 ID    | -              |
| tracking_number | VARCHAR(100) | NOT NULL             | 운송장 번호   | trackingNumber |
| order_status    | VARCHAR(20)  | NOT NULL             | 배송 상태    | orderStatus    |
| shipped_at      | DATETIME     | NULL                 | 발송 일시    | shippedAt      |
| delivered_at    | DATETIME     | NULL                 | 배송 완료 일시 | deliveredAt    |
| created_at      | DATETIME     | NOT NULL             | 생성 일시    | createdAt      |
| updated_at      | DATETIME     | NOT NULL             | 수정 일시    | updatedAt      |

**order_status 값 (API 응답 orderStatus와 동일)**

| DB 값               | API 값              | 설명      |
| ------------------ | ------------------ | ------- |
| `PREPARING`        | `PREPARING`        | 배송 준비 중 |
| `IN_TRANSIT`       | `IN_TRANSIT`       | 배송 중    |
| `OUT_FOR_DELIVERY` | `OUT_FOR_DELIVERY` | 배달 중    |
| `DELIVERED`        | `DELIVERED`        | 배송 완료   |

```sql
CREATE TABLE physical_orders (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    application_id  BIGINT       NOT NULL UNIQUE,
    tracking_number VARCHAR(100) NOT NULL,
    order_status    VARCHAR(20)  NOT NULL,
    shipped_at      DATETIME,
    delivered_at    DATETIME,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_physical_orders_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

```

---

### APPLICATION_STATUS_LOGS (신청 상태 이력)

> 신청 상태 변경 이력 전체 기록
> 분쟁 발생 시 법적 증거 용도
> 모든 상태 전이 시 자동 기록

| 필드명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 |
| application_id | BIGINT | NOT NULL, FK | 신청 ID |
| from_status | VARCHAR(30) | NULL | 변경 전 상태 |
| to_status | VARCHAR(30) | NOT NULL | 변경 후 상태 |
| changed_by | VARCHAR(50) | NOT NULL | 변경 주체 (SYSTEM/ADMIN/USER) |
| reason | VARCHAR(200) | NULL | 변경 사유 |
| changed_at | DATETIME | NOT NULL | 변경 일시 |

```sql
CREATE TABLE application_status_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    application_id  BIGINT       NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30)  NOT NULL,
    changed_by      VARCHAR(50)  NOT NULL,
    reason          VARCHAR(200),
    changed_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_status_logs_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

---

### ADMIN_ACTIVITY_LOGS (관리자 행동 이력)

> 관리자가 수행한 모든 행동 기록
> 누가 언제 뭘 바꿨는지 추적 용도

| 필드명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 |
| admin_user_id | BIGINT | NOT NULL, FK | 관리자 ID |
| action_type | VARCHAR(50) | NOT NULL | 행동 유형 |
| target_table | VARCHAR(50) | NOT NULL | 대상 테이블 |
| target_id | BIGINT | NOT NULL | 대상 ID |
| description | VARCHAR(500) | NULL | 행동 설명 |
| ip_address | VARCHAR(45) | NULL | IP 주소 |
| created_at | DATETIME | NOT NULL | 생성 일시 |

**action_type 값 (API 6번 플로우와 매핑)**
| 값 | 설명 | 연관 API |
|----|------|----------|
| `PHOTO_REJECT` | 사진 재요청 | POST /admin/.../photo-reject |
| `KOREAN_NAME_ASSIGN` | 한국 이름 등록 | POST /admin/.../korean-name |
| `CARD_ISSUE` | 시민증 발급 | POST /admin/.../issue-card |
| `TRACKING_REGISTER` | 운송장 번호 등록 | PATCH /admin/.../tracking |
| `APPLICATION_CANCEL` | 신청 취소 | - |

```sql
CREATE TABLE admin_activity_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    admin_user_id   BIGINT       NOT NULL,
    action_type     VARCHAR(50)  NOT NULL,
    target_table    VARCHAR(50)  NOT NULL,
    target_id       BIGINT       NOT NULL,
    description     VARCHAR(500),
    ip_address      VARCHAR(45),
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_logs_user
        FOREIGN KEY (admin_user_id) REFERENCES users(id)
);
```

---

### PAYMENT_LOGS (결제 이벤트 이력)

> 결제 관련 모든 이벤트 기록
> API: POST /api/payments/webhook 수신 시 기록
> PG사 정산 대조 및 환불 처리 근거

| 필드명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 |
| payment_id | BIGINT | NOT NULL, FK | 결제 ID |
| event_type | VARCHAR(50) | NOT NULL | 이벤트 유형 |
| payment_method | VARCHAR(20) | NULL | 결제 수단 |
| amount | BIGINT | NULL | 금액 |
| status | VARCHAR(20) | NOT NULL | 처리 상태 |
| failure_reason | VARCHAR(200) | NULL | 실패 사유 |
| pg_response | TEXT | NULL | PG사 응답 원문 (웹훅 전문) |
| created_at | DATETIME | NOT NULL | 생성 일시 |

**event_type 값 (웹훅 eventType과 매핑)**
| 값 | 설명 |
|----|------|
| `VIRTUAL_ACCOUNT_ISSUED` | 가상계좌 발급 |
| `VIRTUAL_ACCOUNT_COMPLETED` | 입금 완료 (웹훅 eventType과 동일) |
| `PAYMENT_FAILED` | 결제 실패 |
| `REFUND_REQUEST` | 환불 요청 |
| `REFUND_COMPLETED` | 환불 완료 |

```sql
CREATE TABLE payment_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    payment_id      BIGINT       NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payment_method  VARCHAR(20),
    amount          BIGINT,
    status          VARCHAR(20)  NOT NULL,
    failure_reason  VARCHAR(200),
    pg_response     TEXT,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_logs_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
);
```

---

### EMAIL_LOGS (이메일 발송 이력)

> 이메일 발송 이력 전체 기록
> 이메일 못 받았어요 민원 대응 용도
> API 7번 이메일 알림 발송 시점과 1:1 매핑

| 필드명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 |
| application_id | BIGINT | NOT NULL, FK | 신청 ID |
| email_type | VARCHAR(50) | NOT NULL | 이메일 유형 |
| recipient_email | VARCHAR(100) | NOT NULL | 수신 이메일 |
| status | VARCHAR(20) | NOT NULL | 발송 상태 |
| failure_reason | VARCHAR(200) | NULL | 실패 사유 |
| retry_count | INT | NOT NULL, DEFAULT 0 | 재시도 횟수 |
| sent_at | DATETIME | NULL | 발송 일시 |
| created_at | DATETIME | NOT NULL | 생성 일시 |

**email_type 값 (API 7번 이메일 발송 시점과 동일)**
| 값 | 설명 | 발송 시점 |
|----|------|-----------|
| `PAYMENT_COMPLETE` | 결제 완료 안내 | 웹훅 수신 후 |
| `PHOTO_REJECTED` | 사진 재요청 | POST /admin/.../photo-reject |
| `CARD_READY` | 시민증 제작 완료 | POST /admin/.../issue-card |
| `SHIPPING_STARTED` | 배송 출발 안내 | PATCH /admin/.../tracking |
| `DELIVERED` | 배송 완료 안내 | 배송 완료 감지 후 |

```sql
CREATE TABLE email_logs (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    application_id   BIGINT       NOT NULL,
    email_type       VARCHAR(50)  NOT NULL,
    recipient_email  VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    failure_reason   VARCHAR(200),
    retry_count      INT          NOT NULL DEFAULT 0,
    sent_at          DATETIME,
    created_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_email_logs_application
        FOREIGN KEY (application_id) REFERENCES applications(id)
);
```

---

## 3. 인덱스 전략

```sql
-- APPLICATIONS: 상태별 목록 조회 (GET /admin/applications?status=)
CREATE INDEX idx_applications_status ON applications(status);

-- APPLICATIONS: 유저별 신청 조회 (GET /api/my/applications)
CREATE INDEX idx_applications_user_id ON applications(user_id);

-- APPLICATIONS: 국적별 필터링 (GET /admin/applications?nationality=)
CREATE INDEX idx_applications_nationality ON applications(nationality);

-- CITIZEN_CARDS: 카드 번호 조회
CREATE INDEX idx_citizen_cards_number ON citizen_cards(card_number);

-- APPLICATION_STATUS_LOGS: 신청별 이력 조회
CREATE INDEX idx_status_logs_application_id ON application_status_logs(application_id);

-- PAYMENT_LOGS: 결제별 이벤트 조회
CREATE INDEX idx_payment_logs_payment_id ON payment_logs(payment_id);

-- EMAIL_LOGS: 신청별 이메일 이력 조회
CREATE INDEX idx_email_logs_application_id ON email_logs(application_id);

-- ADMIN_ACTIVITY_LOGS: 관리자별 행동 조회
CREATE INDEX idx_admin_logs_admin_user_id ON admin_activity_logs(admin_user_id);

-- PHOTO_UPLOADS: 사용자별 업로드 조회
CREATE INDEX idx_photo_uploads_user_id ON photo_uploads(user_id);

-- PHOTO_UPLOADS: 스케줄러 만료 정리용
CREATE INDEX idx_photo_uploads_expires_at ON photo_uploads(expires_at);
```

---

## 4. 제약조건 요약

| 규칙 | 대상 | 방식 |
|------|------|------|
| users : applications = 1:N | user_id FK | 일반 FK |
| applications : korean_names = 1:1 | application_id | UNIQUE 제약 |
| applications : citizen_cards = 1:1 | application_id | UNIQUE 제약 |
| applications : shipping_addresses = 1:1 | application_id | UNIQUE 제약 |
| applications : payments = 1:1 | application_id | UNIQUE 제약 |
| applications : physical_orders = 1:1 | application_id | UNIQUE 제약 |
| users : photo_uploads = 1:N | user_id FK | 일반 FK |
| 부모 삭제 시 자식 보호 | 모든 FK | RESTRICT (기본값) |

---

## 5. API ↔ DB 매핑 요약

| API | 영향 받는 테이블 |
|-----|----------------|
| POST /api/auth/google | users |
| POST /api/auth/terms | users |
| POST /api/uploads/photo | photo_uploads |
| POST /api/applications | applications |
| POST /api/applications/bulk | applications (N개) |
| POST /api/applications/{id}/shipping | shipping_addresses |
| PATCH /api/applications/{id}/shipping | shipping_addresses |
| GET /api/applications/{id}/payment-info | payments (조회) |
| POST /api/payments/virtual-account | payments |
| POST /api/payments/webhook | payments, application_status_logs, email_logs, payment_logs |
| GET /api/my/applications | applications (조회) |
| GET /api/my/applications/{id} | applications, korean_names, citizen_cards, shipping_addresses, payments (조회) |
| PATCH /api/my/applications/{id}/photo | applications |
| GET /api/my/applications/{id}/card/download | citizen_cards (조회, URL 갱신) |
| POST /admin/applications/{id}/photo-reject | applications, admin_activity_logs, email_logs |
| POST /admin/applications/{id}/korean-name | korean_names, application_status_logs, admin_activity_logs |
| POST /admin/applications/{id}/issue-card | citizen_cards, applications, application_status_logs, admin_activity_logs, email_logs |
| PATCH /admin/applications/{id}/tracking | physical_orders, applications, application_status_logs, admin_activity_logs, email_logs |

---

## 6. Flyway 마이그레이션 순서

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
V13__create_photo_uploads.sql
```

- 마이그레이션 파일은 한 번 적용 후 절대 수정하지 않는다
- 변경이 필요하면 새 버전 파일 추가 (V13__ ...)
- FK 순서 반드시 준수 (부모 테이블 먼저 생성)
