# API 명세서 — 사용자 플로우 기반

> Base URL: `https://api.{도메인}.com`
> 인증: Google OAuth 로그인 후 발급되는 HttpOnly `accessToken` 쿠키
> 응답 형식: 모든 응답은 `ApiResponse<T>` 래퍼로 통일

---

## 공통 응답 형식

```json
// 성공
{
  "success": true,
  "data": { ... }
}

// 실패
{
  "success": false,
  "data": null,
  "errorCode": "ERROR_CODE",
  "errorMessage": "사용자에게 보여줄 메시지"
}
```

---

## 공통 에러 코드

| code | HTTP | 설명 |
|------|------|------|
| `INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 데이터 없음 |
| `DUPLICATE_APPLICATION` | 409 | 중복 신청 |
| `ALREADY_PAID` | 409 | 이미 결제 완료 |
| `FILE_TOO_LARGE` | 413 | 파일 10MB 초과 |
| `UNSUPPORTED_FILE_TYPE` | 415 | 허용되지 않는 파일 형식 |
| `PAYMENT_FAILED` | 402 | 결제 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## Application Status 값 (전체 공통)

| 값 | 설명 |
|----|------|
| `DRAFT` | 업체 대량 업로드 완료 (결제 전) |
| `PENDING` | 결제 완료, 관리자 검토 대기 |
| `REVIEWING` | 관리자 검토 중 |
| `PHOTO_REJECTED` | 사진 재요청 |
| `CARD_READY` | 디지털 시민증 생성 완료 |
| `SHIPPING` | 실물 카드 배송 중 |
| `DELIVERED` | 배송 완료 |
| `CANCELLED` | 취소 |

---

# 1. 인증 플로우

## 1-1. Google OAuth 로그인

```
GET /oauth2/authorization/google
```

> 최초 로그인 시 회원가입 자동 처리
> 로그인 성공 시 `OAuth2SuccessHandler`가 `accessToken`, `refreshToken`을 HttpOnly 쿠키로 설정하고 프론트엔드로 리다이렉트

**Response `302 Found`**
```http
Set-Cookie: accessToken={JWT}; HttpOnly; Path=/; Max-Age=3600
Set-Cookie: refreshToken={JWT}; HttpOnly; Path=/; Max-Age=1209600
Location: {frontendUrl}/terms
```

---

## 1-2. 약관 동의 (최초 로그인 시)

```
POST /api/auth/terms
Cookie: accessToken={token}
```

**Request**
```json
{
  "privacyAgreed": true,
  "imageUploadAgreed": true,
  "shippingAgreed": true
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "termsAgreed": true,
    "privacyAgreed": true,
    "imageUploadAgreed": true,
    "shippingAgreed": true,
    "agreedAt": "2024-03-01T10:00:00"
  }
}
```

---

## 1-3. 토큰 갱신

```
POST /api/auth/refresh
Cookie: refreshToken={token}
```

**Request**
본문 없음. `refreshToken`은 HttpOnly 쿠키로 전달됩니다.

**Response `200 OK`**
```http
Set-Cookie: accessToken={newJWT}; HttpOnly; Path=/; Max-Age=3600

{
  "success": true
}
```

---

## 1-4. 로그아웃

```
POST /api/auth/logout
Cookie: accessToken={token}
```

**Response `200 OK`**
```http
Set-Cookie: accessToken=; HttpOnly; Path=/; Max-Age=0
Set-Cookie: refreshToken=; HttpOnly; Path=/; Max-Age=0

{
  "success": true
}
```

---

# 2. 정보 입력 플로우

## 단건 흐름

```
사용자가 폼 입력
        ↓
사진 사전 업로드 → photoId 발급
        ↓
POST /api/applications (JSON + photoId)
        ↓
Application 1개 생성
```

## 대량 흐름

```
업체가 ZIP 준비
📁 upload.zip
├── data.xlsx
└── photos/
    ├── john.jpg
    └── jane.jpg
        ↓
POST /api/applications/bulk (ZIP 파일 1개)
        ↓
백엔드에서 압축 해제 → 엑셀 파싱 → 사진 매핑
        ↓
Application N개 생성
        ↓
성공 N건 / 실패 N건 응답
```

---

## 2-1. 사진 사전 업로드

```
POST /api/uploads/photo
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

> 정보 입력 전 사진 먼저 업로드 후 photoId 받아서 신청 시 사용
> 업로드 즉시 photo_uploads 테이블에 저장, 유효 시간 1시간 (expiresAt)
> 신청 생성(2-2) 시 photoId → 소유자 확인 → 만료 여부 검증 → applications.photo_path 복사 → is_used = true
> 만료 미사용 항목은 스케줄러가 주기적으로 S3 삭제 처리

**Request**
```
photo: File (JPG/PNG/WEBP, max 10MB)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "photoId": "photo_abc123",
    "photoUrl": "https://s3.../photos/preview/photo_abc123.jpg",
    "expiresAt": "2024-03-01T11:00:00"
  }
}
```

**에러 케이스**
| code | 상황 |
|------|------|
| `FILE_TOO_LARGE` | 10MB 초과 |
| `UNSUPPORTED_FILE_TYPE` | JPG/PNG/WEBP 외 형식 |
| `INVALID_IMAGE` | 얼굴 식별 불가 (AWS Rekognition) |
| `INAPPROPRIATE_IMAGE` | 부적절한 이미지 감지 |

---

## 2-2. 단건 신청 생성

```
POST /api/applications
Authorization: Bearer {token}
Content-Type: application/json
```

**Request**
```json
{
  "nameEn": "John Smith",
  "nationality": "US",
  "birthDate": "1990-05-15",
  "birthTime": "14:30",
  "birthRegion": "New York, USA",
  "gender": "MALE",
  "photoId": "photo_abc123"
}
```

**필드 설명**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `nameEn` | String | ✅ | 영문 이름 |
| `nationality` | String | ✅ | 국적 코드 (ISO 3166-1 alpha-2) |
| `birthDate` | LocalDate | ✅ | 생년월일 (yyyy-MM-dd) |
| `birthTime` | LocalTime | ✅ | 출생 시각 (HH:mm) |
| `birthRegion` | String | ✅ | 출생 지역 |
| `gender` | ENUM | ✅ | 성별 (MALE/FEMALE) |
| `photoId` | String | ✅ | 사전 업로드된 사진 ID |

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "initialStatus": "PENDING",
    "createdAt": "2024-03-01T10:00:00"
  }
}
```

**에러 케이스**
| code | 상황 |
|------|------|
| `DUPLICATE_APPLICATION` | 동일 사용자 중복 신청 |
| `TERMS_NOT_AGREED` | 약관 미동의 상태 |
| `INVALID_INPUT` | 필수 항목 누락 / 미래 날짜 입력 |
| `PHOTO_NOT_FOUND` | photoId 유효하지 않음 |
| `PHOTO_EXPIRED` | 사진 업로드 만료 |
| `PHOTO_OWNER_MISMATCH` | 다른 사용자가 업로드한 photoId |

---

## 2-3. 대량 신청 생성

```
POST /api/applications/bulk
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request**
```
file: upload.zip (최대 500MB)
```

**ZIP 구조**
```
upload.zip
├── data.xlsx         ← 신청자 정보 (파일명 컬럼 포함)
└── photos/
    ├── john_smith.jpg
    └── jane_doe.jpg
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "totalCount": 50,
    "successCount": 48,
    "failCount": 2,
    "failures": [
      {
        "row": 5,
        "name": "John Smith",
        "reason": "사진 파일 없음 (john_smith.jpg)"
      },
      {
        "row": 12,
        "name": "Jane Doe",
        "reason": "생년월일 형식 오류"
      }
    ]
  }
}
```

**에러 케이스**
| code | 상황 |
|------|------|
| `ZIP_TOO_LARGE` | ZIP 파일 500MB 초과 |
| `INVALID_ZIP` | ZIP 형식 오류 |
| `EXCEL_NOT_FOUND` | ZIP 안에 엑셀 파일 없음 |
| `EXCEL_PARSE_ERROR` | 엑셀 형식 오류 |
| `ALL_FAILED` | 전체 행 처리 실패 |

---

# 3. 배송지 입력 플로우

## 3-1. 배송지 등록

```
POST /api/applications/{applicationId}/shipping
Authorization: Bearer {token}
```

**Request**
```json
{
  "recipientName": "John Smith",
  "country": "US",
  "zipCode": "17855",
  "address": "경기도 평택시 비전9길 36-1 (비전동) Aa home",
  "addressDetail": "1101",
  "email": "sample@codemstory.com",
  "phone": "01012341234"
}
```

**필드 설명**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `recipientName` | String | ✅ | 수령인 이름 |
| `country` | String | ✅ | 국가 코드 |
| `zipCode` | String | ✅ | 우편번호 |
| `address` | String | ✅ | 기본 주소 |
| `addressDetail` | String | ❌ | 상세 주소 |
| `email` | String | ✅ | 이메일 |
| `phone` | String | ✅ | 전화번호 |

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "shippingAddressId": 1,
    "estimatedDeliveryDays": "7~14일",
    "shippingFee": 25000,
    "isLocked": false
  }
}
```

---

## 3-2. 배송지 수정

```
PATCH /api/applications/{applicationId}/shipping
Authorization: Bearer {token}
```

> PENDING / REVIEWING 상태에만 수정 가능
> 발송 후 (SHIPPING / DELIVERED) 수정 불가

**Request**
```json
{
  "recipientName": "John Smith",
  "country": "US",
  "zipCode": "17855",
  "address": "경기도 평택시 비전9길 36-1 (비전동) Aa home",
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

**에러 케이스**
| code | 상황 |
|------|------|
| `SHIPPING_LOCKED` | 발송 이후 수정 불가 (isLocked = true) |
| `EDIT_PERIOD_EXPIRED` | 24시간 수정 기간 만료 (정책 확인 필요) |

---

# 4. 결제 플로우

## 4-1. 결제 금액 조회

```
GET /api/applications/{applicationId}/payment-info
Authorization: Bearer {token}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "serviceAmount": 30000,
    "shippingFee": 25000,
    "totalAmount": 55000,
    "currency": "KRW",
    "paymentStatus": "PENDING"
  }
}
```

**paymentStatus 값**
| 값 | 설명 |
|----|------|
| `PENDING` | 결제 대기 |
| `WAITING_DEPOSIT` | 가상계좌 입금 대기 |
| `COMPLETED` | 결제 완료 |
| `FAILED` | 결제 실패 |
| `CANCELLED` | 결제 취소 |
| `REFUNDED` | 환불 완료 |

---

## 4-2. 가상계좌 발급

```
POST /api/payments/virtual-account
Authorization: Bearer {token}
```

> 입금 대기 → 토스페이먼츠 웹훅 → 결제 완료 처리

**Request**
```json
{
  "applicationId": 15,
  "depositorName": "JSMITH_4821"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "bankName": "하나은행",
    "accountNumber": "1234-5678-9012",
    "amount": 55000,
    "depositorName": "JSMITH_4821",
    "expiredAt": "2024-03-02T23:59:59"
  }
}
```

**에러 케이스**
| code | 상황 |
|------|------|
| `ALREADY_PAID` | 이미 결제 완료 |
| `APPLICATION_NOT_FOUND` | 신청 없음 |
| `AMOUNT_MISMATCH` | 금액 불일치 |

---

## 4-3. 입금 웹훅 수신

```
POST /api/payments/webhook
→ 토스페이먼츠가 호출 (프론트엔드 아님)
→ 인증 없음 (토스페이먼츠 서명 검증으로 대체)
```

**Request (토스페이먼츠가 보내는 데이터)**
```json
{
  "eventType": "VIRTUAL_ACCOUNT_COMPLETED",
  "data": {
    "paymentKey": "토스 결제 키",
    "orderId": "order_20240301_0001",
    "status": "DONE",
    "totalAmount": 55000,
    "virtualAccount": {
      "accountNumber": "1234-5678-9012",
      "bankCode": "088",
      "depositorName": "JSMITH_4821",
      "dueDate": "2024-03-02T23:59:59"
    }
  }
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": null
}
```

> 웹훅은 200 반환 안 하면 토스페이먼츠가 재시도함
> 중복 처리 방지 로직 필수

---

# 5. 마이페이지 플로우

## 5-1. 내 신청 목록 조회

```
GET /api/my/applications
Authorization: Bearer {token}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applications": [
      {
        "applicationId": 1,
        "status": "CARD_READY",
        "koreanName": "김민준",
        "createdAt": "2024-03-01T10:00:00",
        "cardNumber": "honor_id_20240001"
      }
    ]
  }
}
```

---

## 5-2. 내 신청 상세 조회 (상태 폴링)

```
GET /api/my/applications/{applicationId}
Authorization: Bearer {token}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "SHIPPING",
    "nameEn": "John Smith",
    "nationality": "US",
    "koreanName": {
      "fullNameKo": "김민준",
      "fullNameEn": "Kim Min-jun",
      "meaning": "하늘처럼 넓은 마음을 가진 사람",
      "nameOrigin": "敏俊"
    },
    "citizenCard": {
      "cardNumber": "honor_id_20240001",
      "downloadUrl": "https://s3.../honor_id_20240001.zip",
      "issuedAt": "2024-03-02T11:00:00"
    },
    "shipping": {
      "trackingNumber": "EMS123456789KR",
      "carrier": "EMS",
      "orderStatus": "IN_TRANSIT",
      "estimatedDelivery": "2024-03-10",
      "trackingUrl": "https://trace.epost.go.kr/..."
    },
    "payment": {
      "orderId": "order_20240301_0001",
      "paidAmount": 55000,
      "paidAt": "2024-03-01T10:30:00"
    }
  }
}
```

**배송 상태 (orderStatus) 값**
| 값 | 설명 |
|----|------|
| `PREPARING` | 배송 준비 중 |
| `IN_TRANSIT` | 배송 중 |
| `OUT_FOR_DELIVERY` | 배달 중 |
| `DELIVERED` | 배송 완료 |

---

## 5-3. 사진 재업로드 (PHOTO_REJECTED 상태)

```
PATCH /api/my/applications/{applicationId}/photo
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request**
```
photo: File (JPG/PNG/WEBP, max 10MB)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "PENDING",
    "photoUrl": "https://s3.../photos/preview/new_photo.jpg"
  }
}
```

---

## 5-4. 시민증 다운로드 ZIP 발급

```
GET /api/my/applications/{applicationId}/card/download
Authorization: Bearer {token}
```

> 시민증 PNG + 이름 의미 카드 PNG를 ZIP으로 제공
> Presigned URL 유효 기간 7일

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "downloadUrl": "https://s3.../honor_id_20240001.zip?X-Amz-Expires=604800",
    "expiresAt": "2024-03-09T11:00:00",
    "fileName": "honor_id_20240001.zip",
    "includes": [
      "honor_id_20240001.png",
      "name_meaning_20240001.png"
    ]
  }
}
```

---

# 6. 관리자 플로우

> 모든 어드민 API는 `/admin/**` 경로, 관리자 Role 인증 필수

## 6-1. 관리자 로그인

```
POST /admin/auth/login
```

**Request**
```json
{
  "username": "admin",
  "password": "비밀번호"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "accessToken": "관리자 JWT 토큰",
    "role": "ADMIN"
  }
}
```

---

## 6-2. 신청 목록 조회

```
GET /admin/applications?page=0&size=20&status=PENDING&nationality=US&keyword=John
Authorization: Bearer {adminToken}
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `page` | ❌ | 페이지 번호 (0-based, 기본값 0) |
| `size` | ❌ | 페이지 크기 (기본값 20) |
| `status` | ❌ | 상태 필터 |
| `nationality` | ❌ | 국적 필터 |
| `keyword` | ❌ | 이름 검색 |

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "applicationId": 1,
        "nameEn": "John Smith",
        "nationality": "US",
        "birthDate": "1990-05-15",
        "status": "PENDING",
        "photoUrl": "https://s3.../photos/preview/photo_abc123.jpg",
        "createdAt": "2024-03-01T10:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "page": 0,
    "size": 20
  }
}
```

---

## 6-3. 신청 상세 조회

```
GET /admin/applications/{applicationId}
Authorization: Bearer {adminToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "nameEn": "John Smith",
    "nationality": "US",
    "birthDate": "1990-05-15",
    "birthTime": "14:30",
    "birthRegion": "New York, USA",
    "gender": "MALE",
    "photoUrl": "https://s3.../photos/photo_abc123.jpg",
    "status": "PENDING",
    "koreanName": null,
    "payment": {
      "orderId": "order_20240301_0001",
      "paidAmount": 55000,
      "paidAt": "2024-03-01T10:30:00"
    },
    "shipping": {
      "recipientName": "John Smith",
      "country": "US",
      "zipCode": "17855",
      "address": "경기도 평택시 비전9길 36-1",
      "addressDetail": "1101",
      "email": "sample@codemstory.com",
      "phone": "01012341234"
    },
    "createdAt": "2024-03-01T10:00:00"
  }
}
```

---

## 6-4. 사진 재요청

```
POST /admin/applications/{applicationId}/photo-reject
Authorization: Bearer {adminToken}
```

**Request**
```json
{
  "reason": "FACE_NOT_VISIBLE",
  "message": "얼굴이 명확하게 보이지 않습니다. 정면 사진을 다시 업로드해주세요."
}
```

**Reason 값**
| 값 | 설명 |
|----|------|
| `FACE_NOT_VISIBLE` | 얼굴 식별 불가 |
| `INAPPROPRIATE_CONTENT` | 부적절한 이미지 |
| `LOW_QUALITY` | 화질 불량 |
| `OTHER` | 기타 |

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "PHOTO_REJECTED",
    "emailSent": true
  }
}
```

---

## 6-5. 한국 이름 등록

```
POST /admin/applications/{applicationId}/korean-name
Authorization: Bearer {adminToken}
```

**Request**
```json
{
  "familyName": "김",
  "givenName": "민준",
  "fullNameEn": "Kim Min-jun",
  "meaning": "하늘처럼 넓은 마음을 가진 사람",
  "nameOrigin": "敏俊"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "koreanNameId": 1,
    "fullNameKo": "김민준",
    "fullNameEn": "Kim Min-jun",
    "meaning": "하늘처럼 넓은 마음을 가진 사람",
    "nameOrigin": "敏俊",
    "applicationStatus": "REVIEWING"
  }
}
```

---

## 6-5-2. 한국 이름 수정

```
PATCH /admin/applications/{applicationId}/korean-name
Authorization: Bearer {adminToken}
```

> REVIEWING 상태일 때만 수정 가능 (CARD_READY 이후 불가)

**Request**
```json
{
  "familyName": "박",
  "givenName": "서연",
  "fullNameEn": "Park Seo-yeon",
  "meaning": "맑고 따뜻한 빛처럼 주변을 밝히는 사람",
  "nameOrigin": "瑞姸"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "koreanNameId": 1,
    "fullNameKo": "박서연",
    "fullNameEn": "Park Seo-yeon",
    "meaning": "맑고 따뜻한 빛처럼 주변을 밝히는 사람",
    "nameOrigin": "瑞姸",
    "updatedAt": "2024-03-02T11:30:00"
  }
}
```

**에러 케이스**
| code | 상황 |
|------|------|
| `APPLICATION_NOT_FOUND` | 존재하지 않는 applicationId |
| `INVALID_APPLICATION_STATUS` | Application 상태가 REVIEWING 이 아님 |
| `KOREAN_NAME_NOT_FOUND` | 등록된 이름이 없음 (먼저 POST로 등록 필요) |

---

## 6-6. 시민증 발급

```
POST /admin/applications/{applicationId}/issue-card
Authorization: Bearer {adminToken}
```

> 이름 등록 완료 후에만 발급 가능 (REVIEWING 상태)
> 카드 이미지 생성 → 이름 의미 카드 생성 → ZIP 묶음 → S3 업로드
> 상태 CARD_READY 변경 → 사용자 이메일 발송

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "cardNumber": "honor_id_20240001",
    "imagePath": "cards/honor_id_20240001.png",
    "zipPath": "zips/honor_id_20240001.zip",
    "downloadUrl": "https://s3.../honor_id_20240001.zip",
    "issuedAt": "2024-03-02T11:00:00",
    "emailSent": true
  }
}
```

---

## 6-7. 운송장 번호 등록

```
PATCH /admin/applications/{applicationId}/tracking
Authorization: Bearer {adminToken}
```

**Request**
```json
{
  "carrier": "EMS",
  "trackingNumber": "EMS123456789KR",
  "shippedAt": "2024-03-03T09:00:00"
}
```

**Carrier 값**
| 값 | 설명 |
|----|------|
| `EMS` | 우체국 국제특급 |
| `DHL` | DHL |
| `FEDEX` | FedEx |
| `UPS` | UPS |

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "SHIPPING",
    "trackingNumber": "EMS123456789KR",
    "trackingUrl": "https://trace.epost.go.kr/...",
    "emailSent": true
  }
}
```

---

## 6-8. 대시보드 통계

```
GET /admin/dashboard/stats?from=2024-03-01&to=2024-03-31
Authorization: Bearer {adminToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "totalApplications": 120,
    "statusBreakdown": {
      "DRAFT": 2,
      "PENDING": 5,
      "REVIEWING": 3,
      "PHOTO_REJECTED": 2,
      "CARD_READY": 10,
      "SHIPPING": 30,
      "DELIVERED": 70,
      "CANCELLED": 0
    },
    "topNationalities": [
      { "nationality": "US", "count": 30 },
      { "nationality": "JP", "count": 25 },
      { "nationality": "CN", "count": 20 }
    ],
    "revenue": {
      "total": 6600000,
      "period": "2024-03-01 ~ 2024-03-31"
    }
  }
}
```

---

# 7. 이메일 알림 발송 시점

| 시점 | 발송 대상 | 내용 |
|------|-----------|------|
| 결제 완료 (웹훅 수신) | 사용자 | 주문 확인 + 주문번호 |
| 사진 재요청 | 사용자 | 재요청 사유 + 재업로드 링크 |
| 시민증 제작 완료 (CARD_READY) | 사용자 | ZIP 다운로드 링크 |
| 배송 출발 (SHIPPING) | 사용자 | 운송장 번호 + 배송 추적 링크 |
| 배송 완료 (DELIVERED) | 사용자 | 배송 완료 안내 |

---

# 8. 전체 API 목록 요약

## 사용자 API

| 메서드 | 경로 | 설명 | 인증 | 호출 주체 |
|--------|------|------|------|-----------|
| `POST` | `/api/auth/google` | 구글 로그인 | ❌ | 프론트 |
| `POST` | `/api/auth/terms` | 약관 동의 | ✅ | 프론트 |
| `POST` | `/api/auth/refresh` | 토큰 갱신 | ❌ | 프론트 |
| `POST` | `/api/auth/logout` | 로그아웃 | ✅ | 프론트 |
| `POST` | `/api/uploads/photo` | 사진 사전 업로드 | ✅ | 프론트 |
| `POST` | `/api/applications` | 단건 신청 생성 | ✅ | 프론트 |
| `POST` | `/api/applications/bulk` | 대량 신청 생성 | ✅ | 프론트 |
| `POST` | `/api/applications/{id}/shipping` | 배송지 등록 | ✅ | 프론트 |
| `PATCH` | `/api/applications/{id}/shipping` | 배송지 수정 | ✅ | 프론트 |
| `GET` | `/api/applications/{id}/payment-info` | 결제 금액 조회 | ✅ | 프론트 |
| `POST` | `/api/payments/virtual-account` | 가상계좌 발급 | ✅ | 프론트 |
| `POST` | `/api/payments/webhook` | 입금 웹훅 수신 | ❌ | 토스페이먼츠 |
| `GET` | `/api/my/applications` | 내 신청 목록 | ✅ | 프론트 |
| `GET` | `/api/my/applications/{id}` | 내 신청 상세 | ✅ | 프론트 |
| `PATCH` | `/api/my/applications/{id}/photo` | 사진 재업로드 | ✅ | 프론트 |
| `GET` | `/api/my/applications/{id}/card/download` | 카드 ZIP 다운로드 | ✅ | 프론트 |

## 관리자 API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/admin/auth/login` | 관리자 로그인 |
| `GET` | `/admin/applications` | 신청 목록 조회 |
| `GET` | `/admin/applications/{id}` | 신청 상세 조회 |
| `POST` | `/admin/applications/{id}/photo-reject` | 사진 재요청 |
| `POST` | `/admin/applications/{id}/korean-name` | 한국 이름 등록 |
| `PATCH` | `/admin/applications/{id}/korean-name` | 한국 이름 수정 |
| `POST` | `/admin/applications/{id}/issue-card` | 시민증 발급 |
| `PATCH` | `/admin/applications/{id}/tracking` | 운송장 번호 등록 |
| `GET` | `/admin/dashboard/stats` | 통계 조회 |
