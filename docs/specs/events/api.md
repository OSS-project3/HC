# Events API

> `EventPost`/`EventImage`(행사사업 부스 운영·법인단체 협업) 도메인의 API 계약. 엔티티·컬럼·파일 생명주기는 [data-model.md](data-model.md)를 따른다.
>
> ✅ 2026-08-16 확정: Board 도메인 구현 때 정리한 관리자 CRUD 패턴(라우트 레벨 `/api/admin/**` 강제, 파일 업로드 보상삭제)을 그대로 재사용해 설계했다.

## ① 도메인의 책임

관리자가 작성한 행사 기록(부스 운영/법인·단체 협업)을 `EventType`으로 구분해 관리하고, 목록·상세를 비로그인 포함 누구나 조회할 수 있게 한다. **게시글 생성·수정·삭제는 관리자만 할 수 있다.**

## ② 프론트 실제 구조 (기준, `EventsPage.tsx`)

- `/events` 단일 라우트. 부스/협업 두 섹션을 한 화면에 동시에 렌더링 — 프론트는 이 API를 `type` 값만 바꿔 두 번 호출하게 된다.
- 카드 목록 → "자세히 보기" → 모달 상세(같은 페이지 안, 별도 URL 없음). 상세 모달은 `[카드 썸네일, ...섹션 공용 갤러리]`를 이어 붙여 좌측 큰 이미지+썸네일 스트립으로 보여준다.
- 상단 `PROGRAM`/`PROCESS`/하단 상담 배너는 이 API 범위 밖(data-model.md §0).

## ③ 필요한 API 목록 (이번 패스)

1. 목록 조회 — `GET /api/events`
2. 단건 조회 — `GET /api/events/{id}`
3. 생성 — `POST /api/admin/events`
4. 수정 — `PATCH /api/admin/events/{id}`
5. 삭제 — `DELETE /api/admin/events/{id}`

⚪ **`GET /api/admin/events`(관리자 전용 전체 목록, `visible` 무관)는 이번 패스 범위에서 제외한다** — 관리자가 숨긴 글을 다시 찾으려면 결국 필요하지만, 2026-08-16 사용자 확인으로 "있어야 하는 건 맞지만 이번엔 미룬다"로 결정. v1에서 관리자는 생성 응답의 `id`로만 수정·삭제할 수 있다.

### 관리자 권한 강제 방식

Board 구현 때 이미 `SecurityConfig`에 `.requestMatchers("/api/admin/**").hasRole("ADMIN")`을 추가해뒀으므로 `/api/admin/events/**`는 별도 코드 변경 없이 자동으로 적용된다. 이번에 추가할 건 공개 GET 2개(`/api/events`, `/api/events/**`)의 `permitAll()` 뿐이다. 서비스·컨트롤러 레벨 권한 분기 없음 — Board와 동일 이유(리소스 소유권 판단이 필요 없는 "관리자냐 아니냐"뿐).

## 공통 — 페이지 응답 포맷

Review/Board에서 쓰는 `PageResponse<T>`(`common/response/`)를 그대로 재사용한다(신규 타입 없음).

---

### API 1 — 게시글 목록 조회

```
GET /api/events
    ?type=BOOTH            (필수, BOOTH | COLLABORATION)
    &page=0
    &size=10
```
(로그인 불필요 — 공개 조회. `visible=true`인 것만 조회 대상)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "eventType": "BOOTH",
        "title": "서울공예트렌드페어",
        "eventDate": "2026-12-01",
        "eventDateText": "2026. 12",
        "place": "서울 코엑스 Hall C",
        "host": "(재)한국공예·디자인문화진흥원",
        "cardLabel": "명예한국인증 · 방문증",
        "content": "부스를 찾은 방문객에게 한글 오행으로 지은 한국 이름과 카드를...",
        "thumbnailImageUrl": "https://.../events/thumb/....webp",
        "displayOrder": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 4,
    "totalPages": 1
  }
}
```

- `content`(본문)는 카드/모달 둘 다 같은 문구를 쓰는 현재 프론트 구조상 목록에서도 절삭 없이 그대로 반환한다(Board 목록 API와 동일한 이유).
- `thumbnailImageUrl`은 presigned URL(`StorageService.generatePresignedUrl`), 썸네일이 없으면 `null` — 프론트가 placeholder 표시.
- 갤러리(`EventImage`)는 목록 API에 포함하지 않는다(상세에서만 필요).
- 정렬: data-model.md §1 그대로 — `display_order ASC`(NULL은 맨 뒤) → `event_date DESC` → `created_at DESC`.

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `type` 누락 | `INVALID_INPUT` | 400 |
| `type`이 `EventType` 값이 아님 | `INVALID_INPUT` | 400 |
| `page`/`size`가 음수, 또는 `size`가 상한(100) 초과 | `INVALID_INPUT` | 400 |

#### DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| id/eventType/title/eventDate/eventDateText/place/host/cardLabel/content/displayOrder | `EventPost`의 동명 컬럼 |
| thumbnailImageUrl | `EventPost.thumbnail_image_path` → presigned URL 변환, null이면 null |

---

### API 2 — 게시글 단건 조회

```
GET /api/events/{id}
```
(로그인 불필요 — 공개 조회. Board와 달리 `next`(다음글)를 포함하지 않는다 — 현재 프론트에 상세 페이지 자체가 없어(모달뿐) 이전/다음 이동 UI가 없다. data-model.md §0에서 이미 "`/events/{id}` 상세 페이지 라우트"를 범위 밖으로 명시한 것과 같은 이유)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "eventType": "BOOTH",
    "title": "서울공예트렌드페어",
    "eventDate": "2026-12-01",
    "eventDateText": "2026. 12",
    "place": "서울 코엑스 Hall C",
    "host": "(재)한국공예·디자인문화진흥원",
    "cardLabel": "명예한국인증 · 방문증",
    "content": "부스를 찾은 방문객에게 한글 오행으로 지은 한국 이름과 카드를...",
    "thumbnailImageUrl": "https://.../events/thumb/....webp",
    "images": [
      { "id": 10, "originalFileName": "booth-calligraphy.webp", "url": "https://.../events/gallery/..." }
    ]
  }
}
```

- `images`: `EventImage.display_order` 순 정렬 배열. 썸네일은 여기 포함하지 않는다 — 프론트가 이미 `[thumbnailImageUrl, ...images]` 형태로 직접 이어 붙이는 구조라(`EventsPage.tsx` `EventDetail`), 서버가 중복 포함할 이유가 없다.
- `url`은 presigned URL(Board `attachments[].url`과 동일 패턴).

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `id` 없음, 또는 `visible=false` | `EVENT_NOT_FOUND`(신규) | 404 |

비공개(`visible=false`) 글은 존재 자체를 숨긴다 — Board에 없던 개념이라 신규로 정하는 규칙.

#### DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| (API 1과 동일 필드) | 동일 |
| images | `EventImage`(해당 `event_post_id`), `display_order` 순 정렬 |

---

### API 3 — 게시글 생성

```
POST /api/admin/events
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `thumbnail` | file, 0~1개(선택) | 카드 대표 이미지. 없으면 `thumbnail_image_path=null`(프론트 placeholder) |
| `images` | file, 0~10개(선택) | 상세 갤러리 이미지. 개수 상한은 Board 첨부파일과 동일하게 10개로 맞춘다(data-model.md엔 상한이 없어 이번에 새로 정함) |

```json
{
  "eventType": "BOOTH",
  "title": "서울공예트렌드페어",
  "eventDate": "2026-12-01",
  "eventDateText": "2026. 12",
  "place": "서울 코엑스 Hall C",
  "host": "(재)한국공예·디자인문화진흥원",
  "cardLabel": "명예한국인증 · 방문증",
  "content": "부스를 찾은 방문객에게...",
  "visible": true,
  "displayOrder": null
}
```

- `eventType`/`title`/`eventDateText`/`place`/`host`/`cardLabel`/`content`는 필수(data-model.md §1 NOT NULL). `eventDate`/`displayOrder`는 선택.
- `visible`을 생략하면 서버가 `true`로 채운다(data-model.md §1 기본값).
- `created_by_user_id` 같은 감사 컬럼은 이번 데이터 모델에 없다 — Board와 달리 EventPost는 작성자 추적을 요구사항에 포함하지 않았다(data-model.md 참고).

**Response `201 Created`**
```json
{ "success": true, "data": { "id": 1 } }
```

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `eventType`/`title`/`eventDateText`/`place`/`host`/`cardLabel`/`content` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| `eventType`이 `EventType` 값이 아님 | `INVALID_INPUT` | 400 |
| `images`가 10개 초과 | `INVALID_INPUT` | 400 |
| `thumbnail`/`images`가 크기·확장자·MIME·시그니처·디코딩 기준 위반(`EventImageValidator`) | `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE` / `INVALID_IMAGE_FILE` | 413 / 415 / 400 |

#### DB 컬럼 매핑

| Request | 엔티티.컬럼 |
|---|---|
| eventType/title/eventDate/eventDateText/place/host/cardLabel/content/visible/displayOrder | `EventPost`의 동명 컬럼 |
| thumbnail | S3 업로드 후 경로를 `EventPost.thumbnail_image_path`에 직접 저장(`UploadFile` 미경유 — Review의 `image_path`와 동일 패턴) |
| images(files) | S3 업로드 후 `EventImage` N개 생성(순서대로 `display_order` 0..N-1), `image_path`/`original_filename`에 직접 저장 |

---

### API 4 — 게시글 수정

```
PATCH /api/admin/events/{id}
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | API 3과 동일한 필드 전체 재제출(Board PATCH와 동일 원칙 — 부분수정 아님) |
| `thumbnail` | file, 0~1개(선택) | 새 파일이 있으면 교체(기존 파일은 DB 커밋 후 S3 삭제, Review `applyImageChange`와 동일 패턴). 없으면 기존 유지 |

- ⚪ **갤러리(`EventImage`) 편집은 이번 패스 범위 밖이다** — `images` 파트 자체를 받지 않으며, 기존 갤러리는 그대로 유지된다. Board API 4가 첨부파일 편집을 다음 패스로 미룬 것과 동일한 이유(개별 이미지 추가/삭제/순서변경까지 하려면 별도 설계 필요).

**Response `200 OK`**
```json
{ "success": true }
```

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `id` 없음 | `EVENT_NOT_FOUND` | 404 |
| `eventType`/`title`/`eventDateText`/`place`/`host`/`cardLabel`/`content` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| `eventType`이 `EventType` 값이 아님 | `INVALID_INPUT` | 400 |
| `thumbnail`이 크기·확장자·MIME·시그니처·디코딩 기준 위반 | `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE` / `INVALID_IMAGE_FILE` | 413 / 415 / 400 |

#### DB 컬럼 매핑

API 3(생성)과 동일 — 텍스트 필드·`visible`·`displayOrder`가 각각 대응 컬럼을 덮어쓴다. `thumbnail_image_path`는 새 파일 제공 시에만 교체. `EventImage`는 이번 API로 건드리지 않는다.

---

### API 5 — 게시글 삭제

```
DELETE /api/admin/events/{id}
Cookie: accessToken={JWT}
```

**Response `200 OK`**
```json
{ "success": true }
```

- 삭제 범위(data-model.md §4.4 없음 — Board §4.4와 동일 원칙 적용): `EventImage` 전체 삭제 + `EventPost` 삭제를 한 트랜잭션에서 처리하고, 커밋 이후 썸네일+갤러리 S3 객체를 전부 삭제한다. 순서를 바꾸면 롤백 시 DB엔 남아있는데 S3만 사라지는 불일치가 생긴다(Review/Board와 동일 이유).

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `id` 없음 | `EVENT_NOT_FOUND` | 404 |

---

## 정리

- 신규 ErrorCode: `EVENT_NOT_FOUND(404, "존재하지 않는 행사입니다.")`.
- 신규 `EventImageValidator`(package-private, `domain.event.service`) — `ReviewImageValidator`와 규칙이 완전히 동일(2MB, jpg/jpeg/png/webp, 크기→확장자/MIME→시그니처→디코딩 순 검증)하지만 재사용하지 않고 새로 만든다. 이유 둘: (1) `ReviewImageValidator`가 package-private라 다른 패키지에서 주입 자체가 불가능하고, (2) 이 프로젝트는 이미 "검증기는 도메인마다 독립"이 관례(`ReviewImageValidator` 자체 주석이 `ApplicationPhotoValidator` 비재사용 이유를 이렇게 설명, Board의 `BoardAttachmentValidator`도 동일 원칙으로 신규 제작).
- `EventImage`는 `UploadFile`을 경유하지 않는다 — Board의 `BoardAttachment`(join 엔티티)와 달리 Review의 `image_path` 직접 저장 패턴을 따른다(data-model.md §2, 원본 파일명도 `EventImage.original_filename`에 직접 저장).
- `SecurityConfig` 추가 필요분: `GET /api/events`, `GET /api/events/**` → `permitAll()` 뿐. `/api/admin/events/**`는 Board 때 추가한 `/api/admin/**` → `hasRole("ADMIN")` 규칙에 자동 포함.
- 이번 패스 제외(다음 패스로 이월): 관리자 전용 전체 목록 API(`GET /api/admin/events`, `visible` 무관), 수정 API의 갤러리 편집, 상세 페이지 URL 라우트.
