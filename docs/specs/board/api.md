# Board API

> `Board`(공지사항/FAQ) 도메인의 API 계약. 엔티티·컬럼·파일 생명주기는 [data-model.md](data-model.md)를 따른다(§6에서 "API 엔드포인트는 이번 패스에 포함 안 함"으로 남겨뒀던 부분을 여기서 확정).
>
> ✅ 2026-08-14 갱신: 공개 조회 2개(API 1/2) 설계 후, 관리자 CRUD 3개(API 3/4/5)도 QnA(FAQ) 기준으로 사용자와 함께 확정. NOTICE 전용 세부사항(첨부파일 업로드 흐름 등)은 다음 패스에서 이어서 다룬다 — 이번 패스는 QnA를 기준으로 공용 CRUD 골격을 확정하는 것이 목적이다.

## ① 도메인의 책임

관리자가 작성한 공지사항·FAQ를 `BoardType` 하나로 통합 관리하고, 비로그인 포함 누구나 목록·상세를 조회할 수 있게 한다. **게시글 생성·수정·삭제는 관리자만 할 수 있다.**

## ② 프론트 실제 구조 (기준, 2026-08-14 확인)

| 파일 | 구조 |
|---|---|
| `pages/NoticesPage.tsx` | 목록: 제목+작성일(`meta`)만 노출. 검색(전체/제목/작성일, 클라이언트 사이드). 페이지네이션 UI는 있으나 비활성(고정 1페이지) |
| `pages/NoticeDetailPage.tsx` | 상세: 제목/작성일/본문(줄바꿈 단락 분리)/"다음글" 링크. 첨부파일은 **현재 프론트에 실제 업로드 기능이 없어** 본문을 즉석 `.txt`로 가짜 생성해 보여주는 임시 상태 — 원본 정적 데이터(`SupportPage.tsx`)엔 `attachment` 필드가 있었으나 관리자 패널로 옮기며 유실됨 |
| `pages/FaqPage.tsx` | 질문(`q`)/답변(`a`)을 `<details>` 아코디언으로 전체 노출. 검색·페이징 없음. 첨부파일 개념 자체가 없음 |
| `components/admin/ContentAdminPanel.tsx` | `isAdmin`이면 공지/FAQ 각각에 생성·수정·삭제 UI 노출(현재 `localStorage`에만 저장, 실제 서버 인증·API 없음). 편집 가능 필드: title/content/meta(자유 텍스트, 날짜 또는 분류). **파일 업로드 입력 자체가 없음** — 관리자 CRUD API 설계 시 첨부파일 UI도 함께 새로 만들어야 함 |

## ③ 필요한 API 목록 (이번 패스)

1. 게시글 목록 조회 — `GET /api/boards`
2. 게시글 단건 조회 — `GET /api/boards/{id}`
3. 게시글 생성 — `POST /api/admin/boards`
4. 게시글 수정 — `PATCH /api/admin/boards/{id}`
5. 게시글 삭제 — `DELETE /api/admin/boards/{id}`

### 관리자 권한 강제 방식 (✅ 2026-08-14 확정)

`arch.md` §4.6에는 이미 "`/api/admin/**`는 `ADMIN` 역할만 접근할 수 있다"는 원칙이 있었지만, 실제 `SecurityConfig`는 `/admin/**`(API 프리픽스 없는 경로)만 막고 있었다 — Admin 도메인이 여태 한 번도 구현된 적이 없어 이 원칙이 코드로 옮겨지지 않은 상태였다. API 3/4/5가 이 프로젝트의 **첫 관리자 전용 쓰기 API**이므로, 이 공백을 이번에 같이 메운다.

- `SecurityConfig`에 `.requestMatchers("/api/admin/**").hasRole("ADMIN")` 추가.
- 서비스 레벨에서 별도로 role을 체크하지 않는다 — 라우트 레벨에서 이미 막히므로 컨트롤러/서비스 코드에는 권한 분기 자체가 없다(Review의 `canEdit`/`canDelete`처럼 리소스별 소유권을 판단해야 하는 경우와 다르다 — 여기는 "관리자냐 아니냐"만 판단하면 되므로 라우트 레벨 강제로 충분하고 더 안전하다).
- 비로그인 요청 → 기존 `authenticationEntryPoint`가 처리 → `UNAUTHORIZED`(401). 로그인했지만 `ADMIN`이 아님 → 기존 `accessDeniedHandler`가 처리 → `FORBIDDEN`(403). 둘 다 이미 있는 공통 처리라 신규 코드 불필요.

## 공통 — 페이지 응답 포맷

Review 도메인에서 신설한 `PageResponse<T>`(`common/response/`)를 그대로 재사용한다(신규 타입 없음).

---

### API 1 — 게시글 목록 조회

#### ④ Request/Response

```
GET /api/boards
    ?type=NOTICE          (필수)
    &page=0
    &size=9
```
(로그인 불필요 — 공개 조회)

- `type`(필수): `BoardType`(`NOTICE`, `FAQ`) 중 하나. 값이 `BoardType`에 없으면 `INVALID_INPUT`(Review API 2의 `searchType` 오류 처리와 동일하게 `MethodArgumentTypeMismatchException` → `GlobalExceptionHandler`가 처리, 신규 핸들러 불필요 — 기존 것 재사용).
- 검색/키워드 필터는 이번 패스에 포함하지 않는다 — 현재 프론트 검색이 전부 클라이언트 사이드(서버 미호출)라 서버 계약이 필요 없다. 향후 실제로 서버 검색이 필요해지면 Review의 `ReviewSearchType` 패턴을 재사용해 추가한다.

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "boardType": "NOTICE",
        "title": "단체 카드 신청 안내",
        "content": "신청 전 아래 내용을 꼭 확인해 주세요...",
        "createdAt": "2026-08-14T10:00:00"
      }
    ],
    "page": 0,
    "size": 9,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

- `content`(본문)를 목록에서도 절삭 없이 그대로 반환한다 — Review 목록 API와 동일한 이유: FAQ는 별도 상세 화면이 없이 아코디언 하나로 질문+답변을 전부 그 자리에서 보여주므로, 목록 응답에 본문이 없으면 FAQ 화면을 만들 수 없다. NOTICE 목록 화면은 이 필드를 안 쓰지만(제목만 표시), 같은 API를 공유하므로 형식은 통일한다.
- 첨부파일 목록은 목록 API에 포함하지 않는다(NOTICE 상세에서만 필요, §API 2 참고) — 목록 화면 어디에도 첨부파일 존재 여부를 표시하지 않기 때문.
- 정렬 기준: `createdAt DESC`(고정, Review와 동일 이유로 `id DESC`를 2차 정렬키로 둔다 — 동시 등록 시 밀리초 단위로 값이 같아질 수 있음).

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `type` 누락 | `INVALID_INPUT` | 400 |
| `type`이 `BoardType` 값이 아님 | `INVALID_INPUT` | 400 |
| `page`/`size`가 음수, 또는 `size`가 상한(100) 초과 | `INVALID_INPUT` | 400 |

#### ⑥ DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| id | `Board.id` |
| boardType | `Board.board_type` |
| title | `Board.title` |
| content | `Board.content` |
| createdAt | `Board.created_at` |

---

### API 2 — 게시글 단건 조회

#### ④ Request/Response

```
GET /api/boards/{id}
```
(로그인 불필요 — 공개 조회. Review와 달리 `canEdit`/`canDelete` 개념 자체가 없다 — 관리자 CRUD가 이번 범위 밖이고, 애초에 작성자 표시 UI가 없어 "본인 여부" 판단이 의미가 없다)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "boardType": "NOTICE",
    "title": "단체 카드 신청 안내",
    "content": "신청 전 아래 내용을 꼭 확인해 주세요...",
    "createdAt": "2026-08-14T10:00:00",
    "attachments": [
      { "id": 10, "originalFileName": "단체_카드_신청안내.pdf", "url": "https://.../board-attachments/..." }
    ],
    "next": { "id": 2, "title": "카드 배송 안내" }
  }
}
```

- `attachments`: `BoardAttachment.display_order` 순으로 정렬한 배열. FAQ는 항상 빈 배열(`[]`) — FAQ 자체에 첨부파일 개념이 없음(§②). 각 항목의 `url`은 presigned URL(Review의 `imageUrl`과 동일 패턴, `StorageService.generatePresignedUrl`).
- `next`: 같은 `board_type` 안에서 "다음글"(id가 더 작은 것 중 가장 큰 것 = 더 오래된 글) 1건. Review의 `next`와 동일한 목적·구현(`findFirstByIdLessThanOrderByIdDescAndBoardType` 형태) — 마지막 글이면 `null`. **주의**: 반드시 같은 `board_type`으로 스코프를 좁혀야 한다(NOTICE 상세에서 FAQ가 "다음글"로 뜨면 안 됨).
- FAQ는 프론트에 상세 화면 자체가 없지만(§②), API는 NOTICE와 동일하게 만든다 — 별도 응답 형태로 분기할 이유가 없다는 data-model.md §5.3 방향과 일관.

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `id` 없음 | `BOARD_NOT_FOUND`(신규) | 404 |

#### ⑥ DB 컬럼 매핑

| Response 필드 | 출처 |
|---|---|
| id | `Board.id` |
| boardType | `Board.board_type` |
| title | `Board.title` |
| content | `Board.content` |
| createdAt | `Board.created_at` |
| attachments | `BoardAttachment`(해당 `board_id`) → `UploadFile` 조인해 원본 파일명 조회, `display_order` 순 정렬 |
| next | `BoardRepository.findFirstByIdLessThanAndBoardTypeOrderByIdDesc(id, boardType)` |

---

### API 3 — 게시글 생성

#### ④ Request/Response

```
POST /api/admin/boards
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `attachments` | file, 0~10개(선택) | `boardType=NOTICE`일 때만 허용. `BoardAttachment.display_order`는 전송 순서대로 0부터 채운다 |

```json
{
  "boardType": "FAQ",
  "title": "카드 발급까지 얼마나 걸리나요?",
  "content": "결제 확인 후 영업일 기준 5~7일 소요됩니다."
}
```

- `boardType`이 `FAQ`인데 `attachments` 파트가 1개 이상 전송되면 **거절한다**(✅ 2026-08-14 확정, 사용자 확인) — FAQ는 첨부파일 개념 자체가 없으므로 조용히 무시하지 않고 모순된 요청으로 취급한다.
- 항상 multipart로 받는다(`NOTICE` 생성 시 파일이 필요하므로) — `FAQ` 생성 시에는 `attachments` 파트를 아예 보내지 않으면 된다.
- `created_by_user_id`는 요청 값이 아니라 `@AuthenticationPrincipal`로 받은 로그인 관리자 ID를 서버가 채운다.

**Response `201 Created`**
```json
{ "success": true, "data": { "id": 1 } }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `boardType`/`title`/`content` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| `boardType`이 `BoardType` 값이 아님 | `INVALID_INPUT` | 400 |
| `boardType=FAQ`인데 `attachments` 1개 이상 전송됨 | `INVALID_INPUT` | 400 |
| `attachments`가 10개 초과 | `INVALID_INPUT` | 400 |
| 첨부파일이 크기/확장자/MIME 허용 기준 위반(`BoardAttachmentValidator`, data-model.md §5) | `FILE_TOO_LARGE` / `UNSUPPORTED_FILE_TYPE` | 413 / 415 |

#### ⑥ DB 컬럼 매핑

| Request | 엔티티.컬럼 |
|---|---|
| boardType | `Board.board_type` |
| title | `Board.title` |
| content | `Board.content` |
| (세션) | `Board.created_by_user_id` ← 로그인 관리자 principal |
| attachments(files) | S3 업로드 후 `UploadFile` N개 생성 → `BoardAttachment` N개 생성(순서대로 `display_order` 0..N-1) — data-model.md §4.1 생명주기 그대로 |

---

### API 4 — 게시글 수정

#### ④ Request/Response

```
PATCH /api/admin/boards/{id}
Cookie: accessToken={JWT}
Content-Type: application/json
```

```json
{
  "boardType": "FAQ",
  "title": "카드 발급까지 얼마나 걸리나요?",
  "content": "결제 확인 후 영업일 기준 5~7일 소요됩니다."
}
```

- Review의 수정 API와 동일하게 **전체 재제출**이다(부분수정 아님) — 관리자 패널이 title/content를 한 폼에서 같이 편집하므로 일부만 보내는 걸 허용하지 않는다.
- `boardType`/`title`/`content` 3개 필드는 API 3과 동일한 규칙으로 다시 검증한다.
- ⚪ **첨부파일 교체/추가/삭제는 이번 패스 범위 밖이다** — 이 API는 `attachments` 파트 자체를 받지 않으며, 기존 첨부파일은 그대로 유지된다. NOTICE의 첨부파일 편집 흐름은 Review API 5의 `removeImage` 패턴(또는 개별 첨부파일 단위 삭제)을 참고해 별도 패스에서 설계한다.

**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `id` 없음 | `BOARD_NOT_FOUND` | 404 |
| `boardType`/`title`/`content` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| `boardType`이 `BoardType` 값이 아님 | `INVALID_INPUT` | 400 |

#### ⑥ DB 컬럼 매핑

`boardType`/`title`/`content`가 각각 대응 컬럼(`Board.board_type`/`title`/`content`)을 덮어쓴다. `created_by_user_id`는 수정해도 바뀌지 않는다(원작성자 감사 추적 유지). 첨부파일은 이번 패스에서 건드리지 않는다.

---

### API 5 — 게시글 삭제

#### ④ Request/Response

```
DELETE /api/admin/boards/{id}
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
| `ADMIN`이 아님 | `FORBIDDEN` | 403 |
| `id` 없음 | `BOARD_NOT_FOUND` | 404 |

#### ⑥ 삭제 범위

data-model.md §4.4 그대로: `Board` 삭제 시 연결된 `BoardAttachment` 전체 + `UploadFile` 전체를 DB에서 함께 삭제하고, commit 이후 S3 객체도 전부 삭제한다(Review API 4의 "commit 이후 S3 삭제" 원칙과 동일). QnA는 첨부파일이 없으므로 사실상 `Board` row 하나만 지우면 끝난다.

---

## 정리

| # | API | 상태 |
|---|---|---|
| 1 | `GET /api/boards` (목록 조회, 페이징) | 설계 완료 |
| 2 | `GET /api/boards/{id}` (단건 조회) | 설계 완료 |
| 3 | `POST /api/admin/boards` (생성, 관리자 전용) | 설계 완료(QnA 기준) |
| 4 | `PATCH /api/admin/boards/{id}` (수정, 관리자 전용) | 설계 완료(QnA 기준) — NOTICE 첨부파일 교체/삭제 흐름은 미확정 |
| 5 | `DELETE /api/admin/boards/{id}` (삭제, 관리자 전용) | 설계 완료 |

**신규 ErrorCode**: `BOARD_NOT_FOUND(404)` — `common/exception/ErrorCode.java`에 추가 필요(구현 시). CRUD 5개 API 전부 기존 `UNAUTHORIZED`/`FORBIDDEN`/`INVALID_INPUT`/`FILE_TOO_LARGE`/`UNSUPPORTED_FILE_TYPE`만 재사용 — 그 외 신규 ErrorCode 없음.

**✅ 2026-08-14 확정(사용자 확인)**:
- 공지사항 첨부파일(`BoardAttachment`)은 이번 구현 범위에 **포함**한다 — 원본 정적 데이터에 `attachment` 필드가 있었던 실제 요구사항이 근거. FAQ는 첨부파일 대상이 아니다(원본 데이터부터 `{q, a}`뿐, 첨부 개념 없음).
- **게시글 생성·수정·삭제는 관리자 전용**이다(QnA 논의로 확정, NOTICE도 동일 규칙 적용). 라우트를 `/api/admin/boards`로 분리하고 `SecurityConfig`에 `/api/admin/**` → `ADMIN`만 허용을 추가해 라우트 레벨에서 강제한다(`arch.md` §4.6에 이미 있던 원칙을 처음으로 실제 코드에 반영).
- FAQ 생성/수정에 첨부파일이 함께 오면 무시하지 않고 `INVALID_INPUT`으로 거절한다.

**⚪ 미결정 — NOTICE 세부사항(다음 패스)**:
- 첨부파일 교체/추가/삭제 흐름(수정 시) — Review API 5의 `removeImage` 단일 이미지 패턴을 그대로 쓸 수 없다(NOTICE는 0~10개 다중 첨부). 개별 첨부파일 단위 삭제 API를 따로 둘지, PATCH 요청에 "유지할 첨부파일 id 목록"을 받을지 결정 필요.
- `meta` 필드(현재 프론트 `ContentAdminPanel`이 자유 텍스트로 편집 중, 대부분 날짜 문자열로 씀) — `Board` 엔티티엔 이 컬럼이 없다. `created_at`으로 대체 가능해 보이지만 확정 아님. 프론트 관리자 패널도 이 필드 입력 UI를 그대로 쓸지 재검토 필요.
- 프론트 관리자 패널(`ContentAdminPanel.tsx`)에 파일 업로드 입력 자체가 없음 — NOTICE 첨부파일을 실제로 쓰려면 프론트에도 새 UI가 필요(프론트 담당자 작업).

**이번 범위 밖:**
- 관리자 CRUD (위 참고)
- 서버사이드 검색/키워드 필터 — 현재 프론트가 전부 클라이언트 사이드라 불필요, 필요해지면 Review의 `ReviewSearchType` 패턴 재사용
- `arch.md` §4.8 갱신 — 이 문서가 완성됐으므로 갱신 필요(별도 커밋)
