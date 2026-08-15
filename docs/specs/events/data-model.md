# Events Data Model

> 행사사업 페이지의 `부스 운영`과 `법인·단체 협업` 카드형 기록을 백엔드로 관리하기 위한 데이터 모델 기준 문서입니다.
> 현재 프론트는 `/events` 단일 라우트에서 카드 목록과 모달 상세를 표시하며, `/events/{id}` 같은 상세 URL은 없습니다.
> 따라서 v1은 공개 카드 목록/상세 모달에 필요한 데이터와 관리자 CRUD를 위한 저장 구조만 먼저 정의합니다.

## 0. 범위

- **포함**
  - 부스 운영 행사 기록
  - 법인·단체 협업 행사 기록
  - 카드 대표 이미지
  - 상세 모달 갤러리 이미지
- **제외**
  - 상단 `PROGRAM` 소개 카드
  - `PROCESS` 행사 진행 과정
  - 하단 상담 배너
  - `/events/{id}` 상세 페이지 라우트

`PROGRAM`, `PROCESS`, 상담 배너는 현재 정적 소개 콘텐츠 성격이 강하므로 v1 행사 게시글 모델에 포함하지 않는다. 운영 중 CMS화가 필요해지면 별도 엔티티로 분리한다.

## 1. EventPost

`EventPost`는 `/events` 화면의 카드 1개이자 상세 모달 1개를 의미한다.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | 행사 기록 ID |
| event_type | ENUM | NOT NULL | `EventType { BOOTH, COLLABORATION }` |
| title | VARCHAR(255) | NOT NULL | 행사명 |
| event_date | DATE | NULL | 정렬·관리 기준 날짜. 월 단위 행사면 해당 월 1일로 저장 가능 |
| event_date_text | VARCHAR(50) | NOT NULL | 화면 표시용 날짜. 예: `2026. 12` |
| place | VARCHAR(255) | NOT NULL | 장소 |
| host | VARCHAR(255) | NOT NULL | 주최 |
| card_label | VARCHAR(255) | NOT NULL | 발급 카드 표시값. 예: `명예한국인증 · 방문증` |
| content | TEXT | NOT NULL | 카드 본문 및 상세 모달 본문 |
| thumbnail_image_path | VARCHAR(500) | NULL | 카드 대표 이미지 경로. 없으면 프론트가 placeholder 표시 |
| visible | BOOLEAN | NOT NULL | 공개 여부. 기본값 `true` |
| display_order | INT | NULL | 수동 정렬 순서. 값이 있으면 우선 적용 |
| created_at | — | — | `BaseTimeEntity` 공통 규칙 |
| updated_at | — | — | `BaseTimeEntity` 공통 규칙 |

`event_type` 구분:

```java
public enum EventType {
    BOOTH,          // 부스 운영
    COLLABORATION   // 법인·단체 협업
}
```

정렬 기본값:

1. `display_order ASC`
2. `event_date DESC`
3. `created_at DESC`

현재 프론트 주석은 "가장 최근 행사가 맨 위"를 전제로 하므로, 수동 정렬값이 없으면 행사 기준일 최신순으로 정렬한다.

## 2. EventImage

`EventImage`는 상세 모달 갤러리에 표시되는 이미지다.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | 행사 이미지 ID |
| event_post_id | BIGINT | NOT NULL | `EventPost.id` 참조 |
| image_path | VARCHAR(500) | NOT NULL | S3 key 또는 저장 경로 |
| original_filename | VARCHAR(255) | NULL | 원본 파일명 |
| display_order | INT | NOT NULL | 갤러리 노출 순서. 0부터 시작 |
| created_at | — | — | 생성일 |

제약:

- `UNIQUE(event_post_id, display_order)` — 동일 행사 내 이미지 순서 중복 방지

`thumbnail_image_path`와 `EventImage`의 관계 — ✅ 2026-08-16 확정: `representative` 플래그를 두지 않는다. `EventPost.thumbnail_image_path`가 카드·목록용 대표 이미지의 유일한 소스이고, `EventImage`는 상세 모달의 추가 갤러리 이미지만 담당한다. 실제 프론트(`EventsPage.tsx` `EventDetail`)도 상세 모달에서 `[post.image, ...gallery]`처럼 썸네일을 갤러리 맨 앞에 클라이언트에서 붙이는 방식이라, 서버가 "갤러리 중 몇 번째가 대표냐"를 별도로 추적할 필요가 없다 — `representative` row와 `thumbnail_image_path` 컬럼이 어긋나는 동기화 문제 자체를 없앤다.

## 3. 파일 정책

- 행사 이미지는 일반 게시판 첨부파일이 아니라 화면에 직접 노출되는 이미지다.
- 허용 확장자와 MIME은 후기 이미지 정책을 우선 참고하되, 실제 제한값은 Events API 설계 시 확정한다.
- 이미지 업로드 실패 또는 DB 저장 실패 시 S3 보상 삭제 정책을 적용한다.
- 삭제 시에는 DB 삭제 성공 후 S3 객체를 삭제한다.

## 4. API 설계 메모

✅ 2026-08-16 확정 — 정확한 Request/Response JSON, 이미지 검증값, 삭제 방식은 [api.md](api.md) 참고.

```http
GET /api/events?type=BOOTH&page=0&size=10      (공개, visible=true만)
GET /api/events/{id}                            (공개, visible=false면 EVENT_NOT_FOUND)
POST /api/admin/events                           (관리자)
PATCH /api/admin/events/{id}                     (관리자)
DELETE /api/admin/events/{id}                    (관리자)
```

⚪ `GET /api/admin/events`(관리자용 전체 목록, `visible` 무관) — 관리자가 숨긴 글을 다시 찾으려면 필요하지만, 이번 구현 패스 범위에서는 제외하고 이후 별도로 구현한다(2026-08-16 사용자 확인). v1에서 관리자는 생성 직후 응답의 `id`로만 수정·삭제 가능.

관리자 작성/수정은 이미지 업로드가 필요하므로 multipart를 기본으로 둔다.
