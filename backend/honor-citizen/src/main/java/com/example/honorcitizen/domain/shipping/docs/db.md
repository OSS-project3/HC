### SHIPPING_ADDRESSES (배송지)

> 실물 카드 배송을 위한 주소 정보 (국내 전용)
> API: POST /api/applications/{id}/shipping
>      PATCH /api/applications/{id}/shipping
> is_locked = true 이면 수정 불가 (운송장 등록 이후)
> application_id UNIQUE 제약으로 신청 1건당 배송지 1개 보장
> user_id FK로 본인 검증 직접 가능

| 필드명 | 타입 | 제약 | 설명 | API 필드명 |
|--------|------|------|------|----------|
| id | BIGINT | PK, AUTO_INCREMENT | 고유번호 | shippingAddressId |
| application_id | BIGINT | NOT NULL, UNIQUE, FK | 신청 ID (→ applications) | - |
| user_id | BIGINT | NOT NULL, FK | 유저 ID (→ users, 본인 검증용) | - |
| recipient_name | VARCHAR(100) | NOT NULL | 수령인 이름 | recipientName |
| zip_code | VARCHAR(20) | NOT NULL | 우편번호 | zipCode |
| address | VARCHAR(500) | NOT NULL | 기본 주소 | address |
| address_detail | VARCHAR(200) | NULL | 상세 주소 | addressDetail |
| email | VARCHAR(100) | NOT NULL | 이메일 | email |
| phone | VARCHAR(30) | NOT NULL | 전화번호 | phone |
| is_locked | TINYINT(1) | NOT NULL, DEFAULT 0 | 수정 잠금 여부 (운송장 등록 이후 true로 변경) | isLocked |
| created_at | DATETIME | NOT NULL | 생성 일시 | - |
| updated_at | DATETIME | NOT NULL | 수정 일시 | updatedAt |

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

-- SHIPPING_ADDRESSES: 유저별 배송지 조회 / 본인 검증용
CREATE INDEX idx_shipping_addresses_user_id ON shipping_addresses(user_id);
```

---

## 4. 제약조건 요약

| 규칙 | 대상 | 방식 |
|------|------|------|
| users : applications = 1:N | user_id FK | 일반 FK |
| users : shipping_addresses = 1:N | user_id FK | 일반 FK |
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
- 변경이 필요하면 새 버전 파일 추가 (V14__ ...)
- FK 순서 반드시 준수 (부모 테이블 먼저 생성)