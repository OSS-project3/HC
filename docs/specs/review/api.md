## Review 도메인

> ✅ 2026-08-06: 후기 작성 요구사항이 변경되어 신규 설계. 기존 `docs/api/board.md`의 "보류" 상태는 이 문서로 대체된다(게시판/공지 등 다른 Board 하위 기능은 계속 보류, 아래 참고). Entity/컬럼 정의는 [data-model.md](data-model.md) 참고.
>
> 이번 패스는 **후기 등록 / 목록 조회 / 단건 조회 3개 API 설계까지만** 다룬다. 수정·삭제·마이페이지 목록은 범위 밖(§5 TODO 참고), 구현 코드는 작성하지 않는다.

### ① 도메인의 책임

로그인한 사용자가 카드 발급 경험에 대한 후기를 작성하고, 누구나(비로그인 포함) 목록/상세를 조회할 수 있게 한다.

### ② 현재 프론트 상태 (참고 — 이번 요구사항 변경 전 mock)

| 파일 | 현재 동작 |
|---|---|
| `data/reviews.ts` | `localStorage` 기반 mock. 필드는 `{id, title, content, author, authorEmail, createdAt}`뿐 — 신청유형/카드종류/사진 없음 |
| `ReviewEditorPage.tsx` | 로그인 사용자만 작성 가능하지만 **`author: user.name`으로 로그인 이름을 자동 사용** — 이번 요구사항("직접 입력")과 반대 방향이라 프론트도 같이 고쳐야 함 |
| `ReviewsPage.tsx`/`ReviewDetailPage.tsx` | mock 데이터 렌더링만, API 호출 없음 |

프론트는 이번 설계 범위 밖(별도 담당자 진행) — 백엔드 계약만 여기서 확정한다.

### ③ 필요한 API 목록

1. 후기 등록 — `POST /api/reviews`
2. 후기 목록 조회 — `GET /api/reviews`
3. 후기 단건 조회 — `GET /api/reviews/{reviewId}`

### 공통 — 페이지 응답 포맷 (신규)

프로젝트에 페이징 API가 이번이 처음이라 공용 포맷을 아래로 제안한다(`common/response/PageResponse.java` 신설 제안).

```json
{
  "content": [ /* T[] */ ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

`ApiResponse<PageResponse<T>>`로 감싸 반환한다(기존 공통 응답 규칙 그대로 유지).

---

### API 1 — 후기 등록

#### ④ Request/Response

```
POST /api/reviews
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `photos` | file, 0~N개 (같은 파트명 반복) | 첨부 사진. 없어도 됨(요구사항: "0개 이상") |

```json
{
  "title": "한국에서의 추억이 이름과 카드로 남았어요",
  "applicationType": "INDIVIDUAL",
  "cardTypeCodes": ["HONOR_KOREAN"],
  "authorDisplayName": "홍길동",
  "content": "..."
}
```

- `authorDisplayName`은 로그인 사용자의 `User.name`을 서버가 자동으로 채우지 않는다 — 요청 값을 그대로 저장(요구사항 확정 사항).
- `applicant`/`applicationId` 같은 실제 신청 연결 필드는 없다 — `applicationType`/`cardTypeCodes`는 작성자가 스스로 고르는 분류값이다(data-model.md §1/§3 참고, FK 검증 없음).
- Application 도메인의 개인/단체 신청 생성 API와 동일하게, 파일은 사전 업로드 없이 이 등록 API에 함께 실어서 그 자리에서 `UploadFile`(+`ReviewImage`) row가 만들어진다(`docs/api/upload-file.md`에 이미 확정된 공통 원칙).

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "reviewId": 1,
    "createdAt": "2026-08-06T10:00:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `title`/`content`/`authorDisplayName` 중 하나라도 공백 | `INVALID_INPUT` | 400 |
| `applicationType` 누락 | `INVALID_INPUT` | 400 |
| `cardTypeCodes` 비어있음 | `INVALID_INPUT` | 400 (§data-model.md 2절 — 최소 1개 필수로 해석, [TBD]) |
| 첨부 사진 개수가 상한(제안: 10장) 초과 | `INVALID_INPUT` | 400 |
| 사진 5 MiB 초과 | `FILE_TOO_LARGE` | 413 |
| 사진 확장자/MIME 미허용 | `UNSUPPORTED_FILE_TYPE` | 415 |
| 사진 signature 불일치/디코딩 실패 | `INVALID_IMAGE` | 400 |

`title` 최대 100자, `authorDisplayName` 최대 50자(초과 시 `INVALID_INPUT`, data-model.md 컬럼 길이와 일치).

#### ⑥ DB 컬럼 매핑

| Request | 엔티티.컬럼 |
|---|---|
| (세션) | `Review.user_id` ← 로그인 principal |
| authorDisplayName | `Review.author_display_name` |
| title | `Review.title` |
| applicationType | `Review.application_type` |
| content | `Review.content` |
| cardTypeCodes | `ReviewCardType`(`review_card_types`) N건 |
| photos(files) | `UploadFile` N건 생성 → `ReviewImage`(`review_id`+`upload_file_id`+`display_order`) N건 |

---

### API 2 — 후기 목록 조회

#### ④ Request/Response

```
GET /api/reviews?page=0&size=20
```
(로그인 불필요 — 공개 조회)

목록 아이템은 요구사항대로 **4개 필드만** 반환한다(상세 필드는 단건 조회에서만).

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reviewId": 1,
        "title": "한국에서의 추억이 이름과 카드로 남았어요",
        "authorDisplayName": "홍길동",
        "createdAt": "2026-08-06T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

정렬 기준: `createdAt DESC`(고정, 정렬 옵션 없음 — 필요해지면 추후 확장).

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `page`/`size`가 음수 | `INVALID_INPUT` | 400 |
| `size`가 상한(제안: 100) 초과 | `INVALID_INPUT` | 400 |

#### ⑥ DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| reviewId | `Review.id` |
| title | `Review.title` |
| authorDisplayName | `Review.author_display_name` |
| createdAt | `Review.created_at` |

---

### API 3 — 후기 단건 조회

#### ④ Request/Response

```
GET /api/reviews/{reviewId}
```
(로그인 불필요 — 공개 조회)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "reviewId": 1,
    "title": "한국에서의 추억이 이름과 카드로 남았어요",
    "applicationType": "INDIVIDUAL",
    "cardTypeCodes": ["HONOR_KOREAN"],
    "authorDisplayName": "홍길동",
    "images": [
      { "imageId": 10, "url": "https://.../review-1-1.jpg", "displayOrder": 0 }
    ],
    "content": "...",
    "createdAt": "2026-08-06T10:00:00"
  }
}
```

- `images[].url`은 presigned URL(만료 있음) — Application 카드 다운로드 API와 동일한 패턴, 저장하지 말고 조회 시마다 새로 발급.
- `cardTypeCodes`는 코드 배열만 반환(예: `HONOR_KOREAN`) — 한국어 표시명 매핑은 프론트 책임(`CardTypeSeeder`/`GET /api/card-types` 미신설 결정과 동일한 이유: code는 안정적 enum, 언어별 라벨은 프론트 관심사).

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `reviewId` 없음 | `REVIEW_NOT_FOUND`(신규) | 404 |

#### ⑥ DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| reviewId | `Review.id` |
| title | `Review.title` |
| applicationType | `Review.application_type` |
| cardTypeCodes | `ReviewCardType` 전체 |
| authorDisplayName | `Review.author_display_name` |
| images | `ReviewImage`(`display_order` 오름차순) → `UploadFile` presigned URL |
| content | `Review.content` |
| createdAt | `Review.created_at` |

---

## Review 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `POST /api/reviews` (후기 등록) | 설계 완료 |
| 2 | `GET /api/reviews` (목록 조회, 페이징) | 설계 완료 |
| 3 | `GET /api/reviews/{reviewId}` (단건 조회) | 설계 완료 |

**신규 ErrorCode**: `REVIEW_NOT_FOUND(404)` — `common/exception/ErrorCode.java`에 추가 필요(구현 시).

**이번 범위 밖 (TODO.md에 별도 기록):**
- 후기 수정/삭제 API — 소유자 판단 기준(`Review.user_id`)은 이미 설계에 포함해뒀으므로 나중에 추가해도 스키마 변경 불필요
- 마이페이지 "내 후기" 목록 (`GET /api/my/reviews`)
- 조회수 — data-model.md §4
- 카드 종류 0개 허용 여부, 본문 최대 길이, 사진 최대 개수 — [TBD], 확인 후 이 문서에 반영

---
Review 도메인 완료(설계).

---
