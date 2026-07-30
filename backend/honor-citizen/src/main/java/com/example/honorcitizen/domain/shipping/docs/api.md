# 3. 배송지 입력 플로우

---

## 3-1. 배송지 등록

```http
POST /api/applications/{applicationId}/shipping
Authorization: Bearer {token}
```

> 신청 1건당 배송지는 1개만 등록 가능합니다. 이미 등록된 경우 `SHIPPING_ALREADY_EXISTS` 에러가 반환됩니다.

**Request**

```json
{
  "recipientName": "홍길동",
  "zipCode": "17855",
  "address": "경기도 평택시 비전9길 36-1",
  "addressDetail": "1101",
  "email": "sample@codemstory.com",
  "phone": "01012341234"
}
```

**필드 설명**

| 필드              | 타입     | 필수 | 설명    |
| --------------- | ------ | -- | ----- |
| `recipientName` | String | ✅  | 수령인 이름 |
| `zipCode`       | String | ✅  | 우편번호  |
| `address`       | String | ✅  | 기본 주소 |
| `addressDetail` | String | ❌  | 상세 주소 |
| `email`         | String | ✅  | 이메일   |
| `phone`         | String | ✅  | 전화번호  |

> 국내 배송 전용 서비스입니다. 국가 코드(`country`)는 서버에서 `KR`로 고정 처리하며 요청에 포함하지 않습니다.

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "shippingAddressId": 1,
    "estimatedDeliveryDays": "1~3일",
    "shippingFee": 3000,
    "isLocked": false
  }
}
```

**응답 필드 설명**

| 필드                      | 타입      | 설명                                                    |
| ----------------------- | ------- | ----------------------------------------------------- |
| `shippingAddressId`     | Long    | 생성된 배송지 ID                                            |
| `estimatedDeliveryDays` | String  | 예상 배송 소요일. 국내 단일 고정값 `"1~3일"` (DB에 저장되지 않음)          |
| `shippingFee`           | Integer | 배송비. 국내 단일 고정값 `3000` (DB에 저장되지 않으며, Payment 생성 시 반영) |
| `isLocked`              | Boolean | 수정 잠금 여부. 등록 직후 항상 `false`                            |

**에러 케이스**

| code                      | HTTP | 상황                             |
| ------------------------- | ---- | ------------------------------ |
| `SHIPPING_ALREADY_EXISTS` | 409  | 해당 `applicationId`에 배송지가 이미 존재 |
| `APPLICATION_NOT_FOUND`   | 404  | 존재하지 않는 신청                     |
| `UNAUTHORIZED_ACCESS`     | 403  | 본인 신청이 아님                      |

---

## 3-2. 배송지 수정

```http
PATCH /api/applications/{applicationId}/shipping
Authorization: Bearer {token}
```

> 아래 두 조건을 **모두** 만족해야 수정 가능합니다.
> 1. 신청 상태가 `PENDING`, `REVIEWING`, `PHOTO_REJECTED` 중 하나일 것
> 2. `isLocked = false` 일 것 (운송장 등록 전)

**Request**

```json
{
  "recipientName": "홍길동",
  "zipCode": "17855",
  "address": "경기도 평택시 비전9길 36-1",
  "addressDetail": "1101",
  "email": "sample@codemstory.com",
  "phone": "01012341234"
}
```

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "shippingAddressId": 1,
    "updatedAt": "2024-03-01T11:00:00",
    "isLocked": false
  }
}
```

**응답 필드 설명**

| 필드                  | 타입      | 설명                                   |
| ------------------- | ------- | ------------------------------------ |
| `shippingAddressId` | Long    | 수정된 배송지 ID                           |
| `updatedAt`         | String  | 수정 일시 (ISO 8601, DB `updated_at` 대응) |
| `isLocked`          | Boolean | 수정 잠금 여부. 수정 가능 상태이면 항상 `false`      |

**에러 케이스**

| code                    | HTTP | 상황                                                        |
| ----------------------- | ---- | --------------------------------------------------------- |
| `SHIPPING_LOCKED`       | 400  | 운송장 등록 이후 수정 불가 (`isLocked = true`)                       |
| `EDIT_PERIOD_EXPIRED`   | 400  | 수정 가능 상태(`PENDING` / `REVIEWING` / `PHOTO_REJECTED`)가 아님  |
| `SHIPPING_NOT_FOUND`    | 404  | 배송지 정보 없음                                                 |
| `APPLICATION_NOT_FOUND` | 404  | 존재하지 않는 신청                                                |
| `UNAUTHORIZED_ACCESS`   | 403  | 본인 신청이 아님                                                 |

---

## 참고: DB 스키마 (SHIPPING_ADDRESSES)

> 실물 카드 배송을 위한 주소 정보 (국내 전용)
> `application_id` UNIQUE 제약으로 신청 1건당 배송지 1개 보장
> `user_id` FK로 본인 검증 직접 가능
> `is_locked = 1` 이면 수정 불가 (운송장 등록 이후)

| 필드명              | 타입           | 제약                   | 설명                          | API 필드명            |
| ---------------- | ------------ | -------------------- | --------------------------- | ------------------- |
| `id`             | BIGINT       | PK, AUTO_INCREMENT   | 고유번호                        | `shippingAddressId` |
| `application_id` | BIGINT       | NOT NULL, UNIQUE, FK | Application (1:1)           | -                   |
| `user_id`        | BIGINT       | NOT NULL, FK         | User (N:1, 본인 검증용)          | -                   |
| `recipient_name` | VARCHAR(100) | NOT NULL             | 수령인 이름                      | `recipientName`     |
| `zip_code`       | VARCHAR(20)  | NOT NULL             | 우편번호                        | `zipCode`           |
| `address`        | VARCHAR(500) | NOT NULL             | 기본 주소                       | `address`           |
| `address_detail` | VARCHAR(200) | NULL                 | 상세 주소 (선택)                  | `addressDetail`     |
| `email`          | VARCHAR(100) | NOT NULL             | 이메일                         | `email`             |
| `phone`          | VARCHAR(30)  | NOT NULL             | 전화번호                        | `phone`             |
| `is_locked`      | TINYINT(1)   | NOT NULL, DEFAULT 0  | 수정 잠금. 운송장 등록 시 `1`로 변경    | `isLocked`          |
| `created_at`     | DATETIME     | NOT NULL             | 생성 일시                       | -                   |
| `updated_at`     | DATETIME     | NOT NULL             | 수정 일시                       | `updatedAt`         |

> `estimatedDeliveryDays`("1~3일"), `shippingFee`(3000) 는 국내 고정값으로 DB에 저장하지 않고 서버 상수로 반환합니다.

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