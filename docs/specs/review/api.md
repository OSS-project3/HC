## Review 도메인

> ✅ 2026-08-09 전면 개정: 모노레포에 새로 동기화된 실제 프론트(`frontend/src/pages/Review*.tsx`, `data/reviews.ts`)를 기준으로 API 계약을 다시 작성했다. 이전 버전(2026-08-06)은 카드종류 다중선택·사진 다중첨부를 전제로 설계했으나, 실제 화면은 **카드종류 단일선택·사진 0~1장**이다. Entity/컬럼 정의는 [data-model.md](data-model.md) 참고(이번 개정에서 `ReviewCardType`/`ReviewImage` 제거).
>
> 등록/목록조회/단건조회/삭제/수정 5개 API를 다룬다.

### ① 도메인의 책임

로그인한 사용자가 카드 발급 경험에 대한 후기를 작성하고, 누구나(비로그인 포함) 목록/상세를 조회할 수 있게 한다. 본인이 작성한 후기는 본인(또는 관리자)이 수정·삭제할 수 있다.

### ② 프론트 실제 구조 (기준)

| 파일 | 구조 |
|---|---|
| `data/reviews.ts` | `ReviewPost { id, title, content, author, authorEmail, createdAt, applicantType: "personal"\|"organization", cardType: CardType(단일), imageUrl?: string(단일) }` |
| `ReviewEditorPage.tsx` | 제목/신청유형(라디오)/카드종류(라디오, 단일)/작성자명(직접입력)/사진(선택, 1장, 2MB 이하 png·jpeg·webp)/내용. 수정 접근 조건: `isAdmin \|\| review.authorEmail === user.email` |
| `ReviewDetailPage.tsx` | 제목/작성자/신청유형/카드종류/작성일/사진(있으면)/본문/"다음글"(다음 글만, 이전글 없음). 수정·삭제 버튼은 `isAdmin`일 때만 노출 |
| `ReviewsPage.tsx` | 필터: 신청유형은 없고 **카드종류**(단일 선택)+**사진 유무**(전체/사진 모아보기)+검색(전체/제목/내용/작성자)+키워드. 페이지 크기 9, 클라이언트 페이징 |

### ③ 필요한 API 목록

1. 후기 등록 — `POST /api/reviews`
2. 후기 목록 조회 — `GET /api/reviews`
3. 후기 단건 조회 — `GET /api/reviews/{id}`
4. 후기 삭제 — `DELETE /api/reviews/{id}`
5. 후기 수정 — `PATCH /api/reviews/{id}`
6. 내 후기 목록 조회 — `GET /api/my/reviews`

### 공통 — 페이지 응답 포맷

프로젝트에 페이징 API가 이번이 처음이라 공용 포맷을 아래로 제안한다(`common/response/PageResponse.java` 신설 제안).

```json
{
  "content": [ /* T[] */ ],
  "page": 0,
  "size": 9,
  "totalElements": 42,
  "totalPages": 5
}
```

`ApiResponse<PageResponse<T>>`로 감싸 반환한다(기존 공통 응답 규칙 그대로 유지). 기본 `size`는 프론트 `PAGE_SIZE`(9)와 맞춘다.

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
| `image` | file, 0~1개(선택) | 첨부 사진. 프론트 폼과 동일하게 **단일 파일** |

```json
{
  "title": "한국에서의 추억이 이름과 카드로 남았어요.",
  "applicationType": "INDIVIDUAL",
  "cardTypeId": 1,
  "authorName": "윤은재",
  "content": "이름의 뜻을 함께 설명해 주셔서..."
}
```

- `authorName`은 로그인 사용자의 `User.name`을 서버가 자동으로 채우지 않는다 — 요청 값을 그대로 `Review.author_display_name`에 저장.
- `cardTypeId`(Long, 단일)는 `Application.cardTypeId`와 동일한 방식으로 그대로 `Review.card_type_id`에 저장한다(코드 변환 불필요 — data-model.md §1 참고).
- **`applicationType`+`cardTypeId` 조합은 로그인 사용자의 실제 카드 발급 이력과 서버가 대조해서 검증한다** — 자격 없는 조합을 보내면 `REVIEW_NOT_ELIGIBLE`로 거절(data-model.md §2 참고). 단체 신청은 대표 제출자(`Applicant.email`)뿐 아니라 실제 카드를 받은 구성원 개인(`ApplicationMember.email`)도 자격을 인정한다.
- `image`는 파일 검증 규칙이 `ApplicationPhotoValidator`(얼굴사진 5MB)와 다르다 — **2MB 이하, jpg/jpeg/png/webp**(data-model.md §4). Review 전용 검증 로직 필요.

**Response `201 Created`**
```json
{
  "success": true,
  "data": { "id": 15 }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| 로그인 계정이 탈퇴 처리됨(2026-08-13 확정 — 등록에만 적용, 수정에는 미적용) | `ALREADY_WITHDRAWN` | 409 |
| `title`/`content`/`authorName` 중 하나라도 공백 | `INVALID_INPUT` | 400 |
| `applicationType` 누락 | `INVALID_INPUT` | 400 |
| `cardTypeId` 누락 | `INVALID_INPUT` | 400 |
| `cardTypeId`에 해당하는 `CardType`이 없음 | `NOT_FOUND` | 404 |
| `(applicationType, cardTypeId)` 조합이 로그인 사용자의 실제 카드 발급 이력에 없음 | `REVIEW_NOT_ELIGIBLE`(신규) | 403 |
| 로그인 사용자가 **같은 (applicationType, cardTypeId) 조합**으로 이미 작성한 후기가 있음(2026-08-13 확정 — §정리 참고) | `REVIEW_ALREADY_EXISTS`(신규) | 409 |
| `image` 파트가 2개 이상 전송됨 | `INVALID_INPUT` | 400 |
| 사진 2 MiB 초과 | `FILE_TOO_LARGE` | 413 |
| 사진 확장자/MIME 미허용(jpg/jpeg/png/webp 외) | `UNSUPPORTED_FILE_TYPE` | 415 |
| 사진 signature 불일치/디코딩 실패 | `INVALID_IMAGE_FILE` | 400 |

`title` 최대 100자, `authorName` 최대 50자.

#### ⑥ DB 컬럼 매핑

| Request | 엔티티.컬럼 |
|---|---|
| (세션) | `Review.user_id` ← 로그인 principal |
| authorName | `Review.author_display_name` |
| title | `Review.title` |
| applicationType | `Review.application_type` |
| cardTypeId | `Review.card_type_id` |
| content | `Review.content` |
| image(file) | S3 업로드 후 경로만 `Review.image_path`에 저장(선택) — `UploadFile` 경유하지 않음(`ApplicationMember.photo_path`와 동일 패턴) |

---

### API 2 — 후기 목록 조회

#### ④ Request/Response

```
GET /api/reviews
    ?cardTypeId=1        (선택)
    &hasPhoto=true        (선택)
    &searchType=TITLE     (선택, 기본값 ALL)
    &keyword=한국          (선택)
    &page=0
    &size=9
```
(로그인 불필요 — 공개 조회)

모든 필터 파라미터는 선택값이며, 생략하면 해당 조건 없이 전체를 대상으로 한다(AND 결합). 신청유형(`applicationType`) 필터는 프론트 목록 화면에 없어 이번 범위에서 제외한다(카드종류 필터만 있음).

```java
enum ReviewSearchType {
    ALL,      // title/content/authorDisplayName 중 하나라도 keyword 포함
    TITLE,
    CONTENT,
    AUTHOR
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "imageUrl": "https://.../review-1.jpg",
        "applicationType": "INDIVIDUAL",
        "cardType": { "id": 1, "name": "명예 한국인증" },
        "title": "한국에서의 추억이 이름과 카드로 남았어요",
        "content": "이름의 뜻을 함께 설명해 주셔서...",
        "authorName": "홍길동",
        "createdAt": "2026-08-06T10:00:00"
      }
    ],
    "page": 0,
    "size": 9,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

- `imageUrl`: `Review.image_path`가 있으면 presigned URL, 없으면 `null`. 프론트 카드 UI는 사진 없으면 텍스트 전용 카드로 렌더링.
- `content`: 절삭하지 않고 그대로 반환한다 — 프론트가 카드 안에서 CSS로 줄 수를 제한해 표시하므로(`<p>{review.content}</p>` 그대로 렌더링), 서버가 별도로 미리보기 문자열을 만들 필요가 없다.
- 정렬 기준: `createdAt DESC`(고정).

**검색 계약**:
- `keyword`가 없으면 `searchType`이 있어도 무시하고 키워드 검색 조건 자체를 걸지 않는다.
- `keyword`가 있는데 `searchType`이 없으면 기본값 `ALL`로 처리한다.

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `page`/`size`가 음수 | `INVALID_INPUT` | 400 |
| `size`가 상한(제안: 100) 초과 | `INVALID_INPUT` | 400 |
| `searchType`이 `ReviewSearchType` 값 외의 값 | `INVALID_INPUT` | 400 |
| `cardTypeId`에 해당하는 `CardType`이 없음 | `NOT_FOUND` | 404 |

#### ⑥ DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| id | `Review.id` |
| imageUrl | `Review.image_path` → presigned URL(있을 때만) |
| applicationType | `Review.application_type` |
| cardType | `Review.card_type_id` → `CardTypeRepository.findById()`로 `{id, name}` 조회 |
| title | `Review.title` |
| content | `Review.content` |
| authorName | `Review.author_display_name` |
| createdAt | `Review.created_at` |

#### 구현 메모 (조회 성능/패턴)

- **필터**: `cardTypeId`/`hasPhoto`/`searchType`+`keyword` 조합은 `JpaSpecificationExecutor<Review>`로 구현한다(이 프로젝트에 QueryDSL/`@Query`/Specification이 없어 처음 도입하지만 spring-data-jpa 내장 기능이라 새 의존성 아님).
- `hasPhoto` 필터는 단순 `WHERE image_path IS NOT NULL`(또는 `IS NULL`)이다 — 컬렉션이 아니라 단일 컬럼이라 §2026-08-06 버전에 있던 `MEMBER OF`/배치조회 로직 자체가 필요 없어졌다.
- **N+1**: `cardType` 표시를 위해 페이지 내 리뷰들의 `card_type_id` 집합을 모아 `CardTypeRepository.findAllById(...)` 1회로 배치 조회한다(카드 종류 개수 자체가 4종으로 작아 캐싱도 고려 가능하지만 이번 범위에서는 단순 배치조회로 충분). `ReviewImage`/`UploadFile` 배치조회는 더 이상 필요 없다(`image_path`가 Review 자신의 컬럼이라 별도 조회 불필요) — 2026-08-06 버전 대비 크게 단순해짐.
- presigned URL 생성은 사진이 있는 리뷰 수만큼 개별 S3 SDK 호출.

---

### API 3 — 후기 단건 조회

#### ④ Request/Response

```
GET /api/reviews/{id}
```
(로그인 불필요 — 공개 조회. 단, 로그인 여부에 따라 `canEdit`/`canDelete`가 달라지므로 `@AuthenticationPrincipal(required = false) Long userId`로 받는다)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 15,
    "title": "한국에서의 추억이 이름과 카드로 남았어요.",
    "content": "이름의 뜻을 함께 설명해 주셔서 여행이 끝난 뒤에도 특별한 기억으로 간직하고 있습니다.",
    "authorName": "윤은재",
    "applicationType": "INDIVIDUAL",
    "cardType": { "id": 1, "name": "명예 한국인증" },
    "imageUrl": "https://.../review-15.jpg",
    "createdAt": "2026-08-01T13:20:00",
    "next": { "id": 16, "title": "행사 참가자에게 색다른 경험을 선물했습니다." },
    "canEdit": true,
    "canDelete": true
  }
}
```

- `imageUrl`: 사진이 없으면 `null`.
- `next`: ✅ 프론트가 "다음글"만 지원하므로(이전글 없음) `next`만 내려준다. `id`가 현재보다 작은 것 중 가장 큰 것(더 오래된 글) — `ReviewRepository.findFirstByIdLessThanOrderByIdDesc(id)`, PK 범위 조회 1건. 마지막 글이면 `null`.
- `canEdit`/`canDelete`: ✅ 2026-08-09 확정 — **관리자이거나 작성자 본인**(`Review.user_id == 로그인 principal`)일 때 `true`. 비로그인이면 항상 `false`/`false`. 프론트 상세페이지의 버튼 노출 조건(현재는 `isAdmin`만)과 편집 폼의 실제 접근 허용 조건(관리자 또는 작성자 본인)이 서로 달랐는데, 더 완전한 쪽(관리자+본인)을 API 정책으로 확정한다 — 프론트 버튼 노출은 이 필드를 그대로 쓰면 자동으로 일치하게 된다.

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `id` 없음 | `REVIEW_NOT_FOUND`(신규) | 404 |

#### ⑥ DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| id | `Review.id` |
| title | `Review.title` |
| content | `Review.content` |
| authorName | `Review.author_display_name` |
| applicationType | `Review.application_type` |
| cardType | `Review.card_type_id` → `CardTypeRepository.findById()` |
| imageUrl | `Review.image_path` → presigned URL |
| createdAt | `Review.created_at` |
| next | `ReviewRepository.findFirstByIdLessThanOrderByIdDesc(id)` |
| canEdit/canDelete | `Review.user_id == 로그인 principal` (관리자 role이면 무조건 true) |

---

### API 4 — 후기 삭제

본인이 작성한 후기이거나 관리자면 삭제 가능(§API 3의 `canDelete`와 동일 조건).

#### ④ Request/Response

```
DELETE /api/reviews/{id}
Cookie: accessToken={JWT}
```

**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `id` 없음 | `REVIEW_NOT_FOUND` | 404 |
| 로그인 사용자가 작성자도 관리자도 아님 | `FORBIDDEN` | 403 |

#### ⑥ 삭제 범위

`ReviewCardType`/`ReviewImage` join 엔티티가 없어졌으므로 삭제가 단순하다:

```
DB Transaction
 └─ Review 삭제
   ↓
COMMIT
   ↓
image_path가 있었다면 그 S3 객체 삭제 (commit 이후)
```

`UploadFile`을 거치지 않으므로(§API 1 DB 매핑 참고) 별도로 정리할 `UploadFile` row가 없다 — S3 객체 하나만 지우면 끝난다.

---

### API 5 — 후기 수정

> ✅ 2026-08-09 확정. `ReviewEditorPage.tsx`가 등록/수정을 같은 폼·같은 필드로 처리한다(`editing` 여부와 무관하게 매번 폼 전체를 재수집) — 즉 부분수정(PATCH 의미의 partial)이 아니라 **전체 재제출**이다. 본인이 작성한 후기이거나 관리자면 수정 가능(§API 3의 `canEdit`와 동일 조건).

#### ④ Request/Response

```
PATCH /api/reviews/{id}
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `image` | file, 0~1개(선택) | 새 사진으로 교체할 때만 포함 |

```json
{
  "title": "한국에서의 추억이 이름과 카드로 남았어요.",
  "applicationType": "INDIVIDUAL",
  "cardTypeId": 1,
  "authorName": "윤은재",
  "content": "수정된 내용...",
  "removeImage": false
}
```

- 등록(API 1)과 동일한 5개 필드(`title`/`applicationType`/`cardTypeId`/`authorName`/`content`)를 **전부 다시 받는다** — 프론트가 매번 폼 전체를 보내므로 일부만 보내는 걸 허용하지 않는다(전부 필수).
- `removeImage`(boolean, 신규): 프론트에는 없는 필드지만 API에는 필요하다 — 멀티파트 요청에서 "새 파일 파트가 없다"는 것만으로는 "기존 사진 유지"와 "기존 사진 삭제"를 구분할 수 없기 때문. `true`면 기존 사진을 지운다.
- 사진 처리 3가지 경우:
  1. `image` 파트 있음 → 새 파일 검증(API 1과 동일 규칙: 2MB, jpg/jpeg/png/webp) 후 S3 업로드 → 성공하면 기존 `image_path`가 가리키던 S3 객체 삭제(새 파일 업로드 성공을 먼저 확인한 뒤 지우는 순서 — 실패 시 사진이 아예 없어지는 걸 방지) → `Review.image_path` 갱신
  2. `image` 파트 없음 + `removeImage=true` → 기존 S3 객체 삭제, `Review.image_path = NULL`
  3. `image` 파트 없음 + `removeImage=false`(기본값) → `Review.image_path` 그대로 유지, S3 작업 없음
- **`applicationType`/`cardTypeId`는 수정 화면에서도 그대로 편집 가능한 필드**(readonly 아님 — `ReviewEditorPage.tsx`에서 기존 값이 `defaultChecked`로 미리 채워질 뿐 라디오 버튼 자체는 활성 상태). 따라서 등록 때와 동일하게 **수정 시에도 자격검증을 다시 수행한다** — 자격 판정 기준은 **후기 작성자(`Review.user_id`)의 실제 신청 이력**이다(관리자가 대신 수정하는 경우에도 관리자 본인이 아니라 원 작성자 기준으로 검증).

**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `id` 없음 | `REVIEW_NOT_FOUND` | 404 |
| 로그인 사용자가 작성자도 관리자도 아님 | `FORBIDDEN` | 403 |
| `title`/`content`/`authorName` 중 하나라도 공백 | `INVALID_INPUT` | 400 |
| `applicationType`/`cardTypeId` 누락 | `INVALID_INPUT` | 400 |
| `cardTypeId`에 해당하는 `CardType`이 없음 | `NOT_FOUND` | 404 |
| `(applicationType, cardTypeId)` 조합이 **작성자**의 실제 카드 발급 이력에 없음 | `REVIEW_NOT_ELIGIBLE` | 403 |
| 바뀐 `(applicationType, cardTypeId)` 조합으로 **작성자 본인의 다른 후기**가 이미 존재함(자기 자신은 제외 — 2026-08-13 확정, §정리 참고) | `REVIEW_ALREADY_EXISTS` | 409 |
| `image` 파트가 2개 이상 전송됨 | `INVALID_INPUT` | 400 |
| `image`와 `removeImage=true`가 동시에 옴(모순된 요청) | `INVALID_INPUT` | 400 |
| 사진 2 MiB 초과 | `FILE_TOO_LARGE` | 413 |
| 사진 확장자/MIME 미허용(jpg/jpeg/png/webp 외) | `UNSUPPORTED_FILE_TYPE` | 415 |
| 사진 signature 불일치/디코딩 실패 | `INVALID_IMAGE_FILE` | 400 |

#### ⑥ DB 컬럼 매핑

API 1(등록)과 동일 — `title`/`applicationType`/`cardTypeId`/`authorName`/`content`가 각각 대응 컬럼을 덮어쓴다. `image`/`removeImage`는 위 3가지 경우에 따라 `Review.image_path`만 갱신한다.

---

### API 6 — 내 후기 목록 조회

```http
GET /api/my/reviews?page=0&size=9
```

- 인증: USER 또는 ADMIN
- 로그인 사용자의 `Review.user_id`와 일치하는 후기만 반환
- 정렬: `createdAt DESC, id DESC`
- page는 0 이상, size는 1~100
- 응답: API 2와 동일한 `ApiResponse<PageResponse<ReviewListItemResponse>>`
- 작성자 이름·이메일은 소유권 필터로 사용하지 않음

---

## Review 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `POST /api/reviews` (후기 등록) | 설계 완료 |
| 2 | `GET /api/reviews` (목록 조회, 페이징) | 설계 완료 |
| 3 | `GET /api/reviews/{id}` (단건 조회) | 설계 완료 |
| 4 | `DELETE /api/reviews/{id}` (삭제, 본인 또는 관리자) | 설계 완료 |
| 5 | `PATCH /api/reviews/{id}` (수정, 본인 또는 관리자) | 설계 완료 |
| 6 | `GET /api/my/reviews` (로그인 사용자 본인 목록) | 구현 완료 |

CRUD 4종 전부(Create/Read/Update/Delete) 설계 완료 — 실제 코드 구현 진행 중(2026-08-13~).

**신규 ErrorCode**: `REVIEW_NOT_FOUND(404)`, `REVIEW_NOT_ELIGIBLE(403, "선택한 신청유형·카드종류에 대한 신청 이력이 없습니다.")`, `REVIEW_ALREADY_EXISTS(409, "이미 해당 신청유형·카드종류로 작성한 후기가 있습니다.")`, `INVALID_IMAGE_FILE(400, "이미지 파일이 손상되었거나 형식이 올바르지 않습니다.")` — `common/exception/ErrorCode.java`.

**✅ 2026-08-13 확정 — 후기 작성 개수 제한**: 사용자 1명이 자격 있는 모든 (신청유형, 카드종류) 조합마다 후기를 하나씩 쓸 수 있다("한 신청당 한 개"). 단, `Review`가 특정 `Application` row를 가리키지 않는다는 기존 설계(data-model.md §3)를 유지하기 위해, 판단 기준은 실제 Application row가 아니라 **`(user_id, application_type, card_type_id)` 조합의 유일성**이다 — 같은 조합으로 실제 신청을 2번 했더라도 후기는 1개만 허용한다. 등록(API 1) 시 이미 같은 조합의 후기가 있으면 `REVIEW_ALREADY_EXISTS`(409). 수정(API 5)에서 조합을 바꿀 때도 동일 검사(자기 자신은 제외).

**✅ 2026-08-13 확정 — 탈퇴 계정 작성 차단**: 탈퇴(`User.status = WITHDRAWN`) 처리된 계정은 새 후기를 작성할 수 없다(`ALREADY_WITHDRAWN`, 409). 탈퇴 시 그 시점의 accessToken만 블랙리스트에 올라가므로, 다른 기기의 유효한 토큰이 남아 있으면 인증 자체는 통과할 수 있어 등록(API 1) 단계에서 한 번 더 막는다. **수정(API 5)에는 적용하지 않는다** — 이미 작성된 후기의 원작성자가 나중에 탈퇴하더라도 관리자가 해당 후기를 계속 관리(수정)할 수 있어야 하기 때문.

**⚠️ `SecurityConfig` 반영 필요**: 현재 `/api/**`는 기본 `hasAnyRole("USER","ADMIN")`로 막혀 있다. API 2(목록)·API 3(단건조회)는 비로그인 공개 조회이므로 `/api/applications/lookup`처럼 `permitAll()`에 추가해야 실제로 동작한다. API 1(등록)·API 4(삭제)·API 5(수정)는 로그인 필수라 기존 규칙 그대로 적용되면 된다.

**이번 범위 밖:**
- "내가 후기 쓸 수 있는 (신청유형, 카드종류) 목록" 조회 API — 없으면 프론트가 라디오 옵션을 모른 채 제출했다가 `REVIEW_NOT_ELIGIBLE`로 거절당하는 흐름만 가능(data-model.md §2)
- 조회수 — data-model.md §3
