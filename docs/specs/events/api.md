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

## ③ 필요한 API 목록

1. 목록 조회 — `GET /api/events`
2. 단건 조회 — `GET /api/events/{id}`
3. 생성 — `POST /api/admin/events`
4. 수정 — `PATCH /api/admin/events/{id}`
5. 삭제 — `DELETE /api/admin/events/{id}`
6. 관리자 목록 조회 — `GET /api/admin/events` (✅ 2026-08-21 구현, `visible` 무관 전체)
7. 관리자 단건 조회 — `GET /api/admin/events/{id}` (✅ 2026-08-21 구현, `visible=false`도 조회 가능)

✅ 2026-08-21 확정: `company_name`·`logo_image_path`를 `COLLABORATION` 전용 선택 필드로 추가하고, 수정 API에 갤러리(`EventImage`) 유지·교체·삭제 편집을 추가했다. `host`는 행사 주최·운영 주체이고 `company_name`은 협업 법인·단체명이라 별개 필드로 유지한다(합치지 않음).

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
        "companyName": null,
        "logoImageUrl": null,
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
- `companyName`/`logoImageUrl`은 ✅ 2026-08-21 추가 — `COLLABORATION`에서만 값이 있을 수 있고 `BOOTH`는 항상 `null`. `logoImageUrl`도 썸네일과 동일하게 presigned URL이거나 없으면 `null`.
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
| companyName | `EventPost.company_name` |
| logoImageUrl | `EventPost.logo_image_path` → presigned URL 변환, null이면 null |

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
    "companyName": null,
    "logoImageUrl": null,
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
| (API 1과 동일 필드, companyName/logoImageUrl 포함) | 동일 |
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
| `logo` | file, 0~1개(선택) | ✅ 2026-08-21 추가. 협업 로고 — `COLLABORATION`에서만 의미 있음(`BOOTH`가 보내면 `INVALID_INPUT`) |
| `images` | file, 0~10개(선택) | 상세 갤러리 이미지. 개수 상한은 Board 첨부파일과 동일하게 10개로 맞춘다(data-model.md엔 상한이 없어 이번에 새로 정함) |

```json
{
  "eventType": "COLLABORATION",
  "title": "OO기업 협업 카드 발급",
  "eventDate": "2026-12-01",
  "eventDateText": "2026. 12",
  "place": "서울 코엑스 Hall C",
  "host": "(재)한국공예·디자인문화진흥원",
  "cardLabel": "명예한국인증 · 방문증",
  "content": "협업 카드 발급 프로그램 소개...",
  "companyName": "OO기업",
  "visible": true,
  "displayOrder": null
}
```

- `eventType`/`title`/`eventDateText`/`place`/`host`/`cardLabel`/`content`는 필수(data-model.md §1 NOT NULL). `eventDate`/`displayOrder`는 선택.
- `companyName`은 ✅ 2026-08-21 추가 — `COLLABORATION`에서만 선택 입력, 최대 100자(trim 후 검증). `BOOTH`인데 값이 있으면 `INVALID_INPUT`(조용히 무시하지 않음). `host`(행사 주최·운영 주체)와는 별개 개념이라 합치지 않는다.
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
| `eventType=BOOTH`인데 `companyName` 또는 `logo`가 있음 | `INVALID_INPUT` | 400 |
| `companyName`이 100자 초과 | `INVALID_INPUT` | 400 |
| `images`가 10개 초과 | `INVALID_INPUT` | 400 |
| `thumbnail`/`logo`/`images`가 크기·확장자·MIME·시그니처·디코딩 기준 위반(`EventImageValidator`) | `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE` / `INVALID_IMAGE_FILE` | 413 / 415 / 400 |

#### DB 컬럼 매핑

| Request | 엔티티.컬럼 |
|---|---|
| eventType/title/eventDate/eventDateText/place/host/cardLabel/content/companyName/visible/displayOrder | `EventPost`의 동명 컬럼 |
| thumbnail | S3 업로드 후 경로를 `EventPost.thumbnail_image_path`에 직접 저장(`UploadFile` 미경유 — Review의 `image_path`와 동일 패턴) |
| logo | S3 업로드 후 경로를 `EventPost.logo_image_path`에 직접 저장. `thumbnail`/`images`와 별도 S3 key(`events/logos/...`) |
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
| `request` | JSON | 아래(API 3과 동일한 필드 전체 재제출 — Board PATCH와 동일 원칙, 부분수정 아님) |
| `thumbnail` | file, 0~1개(선택) | 새 파일이 있으면 교체(기존 파일은 DB 커밋 후 S3 삭제, Review `applyImageChange`와 동일 패턴). 없으면 기존 유지 |
| `logo` | file, 0~1개(선택) | ✅ 2026-08-21 추가. 새 파일이 있으면 교체. 없으면 기존 유지(`removeLogo`가 별도로 없는 한) |
| `images` | file, 0~N개(선택) | ✅ 2026-08-21 추가. `keepImageIds` 뒤에 이어붙일 신규 갤러리 이미지 |

```json
{
  "eventType": "COLLABORATION",
  "title": "OO기업 협업 카드 발급",
  "eventDate": "2026-12-01",
  "eventDateText": "2026. 12",
  "place": "서울 코엑스 Hall C",
  "host": "(재)한국공예·디자인문화진흥원",
  "cardLabel": "명예한국인증 · 방문증",
  "content": "협업 카드 발급 프로그램 소개...",
  "companyName": "OO기업",
  "removeLogo": false,
  "removeThumbnail": false,
  "keepImageIds": [10, 12],
  "visible": true,
  "displayOrder": null
}
```

- ✅ 2026-08-21 확정 — 갤러리(`EventImage`) 편집이 이번에 추가됐다(이전 패스에서 미룬 항목):
  - `keepImageIds` **필드 자체를 생략**하면 기존 갤러리 전체를 현재 순서 그대로 유지한다(신규 `images` 파일만 뒤에 추가).
  - `keepImageIds`를 **빈 배열 `[]`로 명시**하면 기존 갤러리를 전부 삭제한다(신규 `images` 파일만 새 갤러리가 됨).
  - `keepImageIds`에 담긴 ID는 반드시 이 Event 소유의 `EventImage`여야 한다 — 다른 Event의 이미지 ID나 존재하지 않는 ID가 섞이면 `INVALID_INPUT`.
  - 최종 갤러리 순서는 `keepImageIds` 나열 순서 그대로 앞에 오고, 그 뒤에 이번 요청의 `images` 파일이 전송 순서대로 이어붙는다. 최종 개수가 10장을 넘으면 `INVALID_INPUT`.
- `removeLogo=true`/`removeThumbnail=true`이면 해당 기존 파일 연결을 제거한다. 같은 요청에 새 파일과 remove flag를 동시에 보내면 `INVALID_INPUT`(의도가 모호해서 거절 — 조용히 우선순위를 정하지 않음).
- `eventType`을 `COLLABORATION → BOOTH`로 바꾸는 경우, `companyName=null`과 `removeLogo=true`를 함께 보내야 한다 — 남은 협업 데이터(`companyName` 값이 남아있거나 로고를 지우라고 하지 않음)가 있으면 `INVALID_INPUT`.

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
| `eventType=BOOTH`인데 `companyName` 또는(새 `logo`나 기존 로고를 `removeLogo=true`로 안 지운) 로고가 남음 | `INVALID_INPUT` | 400 |
| `companyName`이 100자 초과 | `INVALID_INPUT` | 400 |
| `removeLogo=true`와 새 `logo` 파일을 동시 전송(`removeThumbnail`도 동일) | `INVALID_INPUT` | 400 |
| `keepImageIds`에 타 Event 소유이거나 존재하지 않는 ID 포함 | `INVALID_INPUT` | 400 |
| 최종 갤러리 개수(`keepImageIds` + 신규 `images`)가 10장 초과 | `INVALID_INPUT` | 400 |
| `thumbnail`/`logo`/`images`가 크기·확장자·MIME·시그니처·디코딩 기준 위반 | `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE` / `INVALID_IMAGE_FILE` | 413 / 415 / 400 |

#### DB 컬럼 매핑

API 3(생성)과 동일 — 텍스트 필드·`companyName`·`visible`·`displayOrder`가 각각 대응 컬럼을 덮어쓴다. `thumbnail_image_path`/`logo_image_path`는 새 파일 제공 또는 remove flag가 있을 때만 변경. `EventImage`는 `keepImageIds`+신규 `images`로 재구성(유지되는 row는 `id` 보존, `display_order`만 재배정 — 삭제되는 row는 물리 삭제).

기존 파일 교체·삭제는 Review `applyImageChange`와 동일하게 **DB 트랜잭션 commit 이후에만** 옛 S3 객체를 지운다. commit 실패 시 기존 파일은 그대로 두고 신규 업로드분만 역순 보상 삭제한다. after-commit 기존 파일 삭제가 실패해도 이미 완료된 수정 결과는 되돌리지 않고 경고 로그만 남긴다(운영자 수동 재삭제 대상).

---

### API 6 — 관리자 게시글 목록 조회 (✅ 2026-08-21 신규)

```
GET /api/admin/events?type=&visible=&page=0&size=10
Cookie: accessToken={JWT}
```

- `type`/`visible` 둘 다 선택 — 생략하면 해당 조건은 전체(유형 무관/공개여부 무관)로 조회한다. 공개 API(API 1)와 달리 `type`이 필수가 아니다.
- 정렬은 공개 목록과 동일(`display_order ASC` NULL 맨 뒤 → `event_date DESC` → `created_at DESC`).
- `visible=false`인 게시글도 포함된다(관리자가 숨긴 글을 다시 찾는 용도).

**Response `200 OK`** — API 1과 동일한 필드 + `visible` 추가:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "eventType": "COLLABORATION",
        "title": "OO기업 협업 카드 발급",
        "eventDate": "2026-12-01",
        "eventDateText": "2026. 12",
        "place": "서울 코엑스 Hall C",
        "host": "(재)한국공예·디자인문화진흥원",
        "cardLabel": "명예한국인증 · 방문증",
        "content": "협업 카드 발급 프로그램 소개...",
        "thumbnailImageUrl": "https://.../events/thumb/....webp",
        "companyName": "OO기업",
        "logoImageUrl": "https://.../events/logos/....webp",
        "visible": false,
        "displayOrder": null
      }
    ],
    "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
  }
}
```

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `type`이 `EventType` 값이 아님 | `INVALID_INPUT` | 400 |
| `page`/`size`가 음수, 또는 `size`가 상한(100) 초과 | `INVALID_INPUT` | 400 |

---

### API 7 — 관리자 게시글 단건 조회 (✅ 2026-08-21 신규)

```
GET /api/admin/events/{id}
Cookie: accessToken={JWT}
```

- `visible=false`인 게시글도 조회 가능(공개 API 2와의 유일한 차이 — 존재를 숨기지 않는다).

**Response `200 OK`** — API 2와 동일한 필드 + `visible` 추가.

#### Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `id` 없음(`visible` 무관) | `EVENT_NOT_FOUND` | 404 |

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
- `SecurityConfig` 추가 필요분: `GET /api/events`, `GET /api/events/**` → `permitAll()` 뿐. `/api/admin/events/**`(GET 포함)는 Board 때 추가한 `/api/admin/**` → `hasRole("ADMIN")` 규칙에 자동 포함 — 관리자 목록/상세(API 6·7) 추가에 별도 `SecurityConfig` 변경 불필요.
- 여전히 범위 밖: 상세 페이지 URL 라우트(`/events/{id}`, data-model.md §0).
