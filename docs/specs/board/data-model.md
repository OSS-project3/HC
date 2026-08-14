# Board Data Model

> Board(공지사항/FAQ 등 텍스트형 게시판) 도메인의 엔티티, 컬럼, 관계, 파일 생명주기·검증 정책에 대한 Source of Truth입니다.
> 업무 요구사항은 도메인 규모가 작아 requirements.md를 별도로 두지 않고 이 문서에 함께 정리합니다(`docs/specs/review/` 선례와 동일 방식).
> API 엔드포인트의 정확한 요청·응답 JSON 형식은 이번 패스에 포함하지 않습니다 — 데이터 모델·파일 생명주기·첨부파일 검증 정책까지 확정.
>
> `arch.md` §4.8은 "Post(일반 게시판)는 요구사항과 화면이 확정되지 않아 구현 대상에서 제외"라고 적혀 있는데, 이 문서가 그 요구사항 확정 작업입니다 — 이 문서가 완성되면 `arch.md` §4.8도 갱신이 필요합니다(§1 문서 갱신 전파 참고).

## 0. 범위

- **포함**: 공지사항(NOTICE), FAQ — 필드 구조가 동일한 단순 텍스트형 게시판을 `Board` + `BoardType` enum으로 통합 관리.
- **제외**:
  - **후기(Review)** — 이미 확정된 전용 설계(`docs/specs/review/data-model.md`)가 있고, 카드종류 다중선택·자격검증 등 `Board`와 근본적으로 다른 필드/로직이라 통합하지 않는다.
  - **1:1 문의(Inquiry)** — 공개 목록/상세가 없고(작성자 본인도 재조회 불가), 답변 텍스트 필드 없이 상태 토글만 있어 "게시판"과 성격이 다르다. 별도 도메인으로 남겨둔다.
  - **이벤트(Events)** — 프론트상 정적 소개 페이지일 뿐 게시글 목록/상세 구조가 없어 게시판이 아니다.

## 1. Board

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| board_type | ENUM | NOT NULL | `BoardType { NOTICE, FAQ }` — 신규 텍스트형 게시판이 생기면 이 enum 값만 추가(테이블 추가 없음) |
| title | VARCHAR | NOT NULL | FAQ는 질문(Q)을 이 필드에 저장 |
| content | TEXT | NOT NULL | FAQ는 답변(A)을 이 필드에 저장 |
| created_by_user_id | BIGINT | NOT NULL | 작성 관리자 `User.id`. **`arch.md` §5.1 원칙(모듈 간 영속 참조는 `Long ...Id`, `@ManyToOne`/양방향 JPA 연관관계 금지)에 따라 JPA 연관관계로 두지 않는다** — `Application.userId`와 동일한 방식. DB 레벨 FK 제약도 걸지 않는다(이 프로젝트는 `schema.sql`에 테이블 DDL이 없고 Hibernate가 엔티티 기준으로 생성하며, 다른 모든 `...Id` 참조도 실제 FOREIGN KEY 제약이 없는 순수 컬럼이라 동일하게 맞춘다) |
| created_at | — | — | `BaseTimeEntity` 공통 규칙 |
| updated_at | — | — | `BaseTimeEntity` 공통 규칙 |

`Board extends BaseTimeEntity`.

## 2. BoardAttachment (게시글 첨부파일 — 0개 이상 다중)

`Board : UploadFile` = 1 : N. `Review`의 `ReviewImage`와 동일한 join 성격 Entity.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| board_id | BIGINT | NOT NULL, FK → Board | |
| upload_file_id | BIGINT | NOT NULL, FK → UploadFile | |
| display_order | INT | NOT NULL | 0부터 시작하는 노출 순서 |

제약:
- `UNIQUE(board_id, upload_file_id)` — 동일 파일 중복 첨부 방지
- `UNIQUE(board_id, display_order)` — 동일 게시글 내 순서 중복 방지

`BoardAttachment`가 왜 `UploadFile`에 직접 1:N을 걸지 않고 join Entity를 두는지: `docs/api/upload-file.md`에 이미 확정된 원칙("`UploadFile`은 아무것도 참조하지 않는 공용 메타데이터 테이블")을 그대로 따른 것 — `ReviewImage`와 이유가 완전히 같다.

## 3. UploadFileType 추가

기존 `UploadFileType`(`PHOTO`/`EXCEL`/`ZIP`/`CARD_IMAGE`)에 `BOARD_ATTACHMENT` 1개 값을 추가한다.

```java
public enum UploadFileType {
    PHOTO,
    EXCEL,
    ZIP,
    CARD_IMAGE,
    BOARD_ATTACHMENT
}
```

- 확인 결과 이 enum은 파일의 실제 포맷이 아니라 **업로드 파일의 용도(role)** 를 나타낸다 — 실제 포맷 구분은 별도 `mimeType` 필드가 담당하고, 코드 전체에서 `fileType`으로 분기하는 로직은 없다(순수 저장용 라벨).
- `BoardType`별로(`NOTICE_ATTACHMENT`/`FAQ_ATTACHMENT` 등) 세분화하지 않고 `BOARD_ATTACHMENT` 하나로 통일한다 — 어느 게시글에 붙었는지는 `BoardAttachment.board_id`가 이미 추적하므로 `UploadFileType`에서 게시판 종류별로 쪼갤 실익이 없다.

## 4. 파일 생명주기 정책

### 4.1 게시글 생성 (첨부 N개 포함)

```
요청/파일 검증
   ↓
S3 파일 N개 업로드
   ↓
DB Transaction
 ├─ Board 저장
 ├─ UploadFile N개 저장
 └─ BoardAttachment N개 저장
   ↓
COMMIT
```

- S3 업로드 도중 실패(예: 3개 중 3번째 실패) → 그때까지 성공한 파일들을 역순으로 보상 삭제 후 원래 예외 반환. `ApplicationService.createGroup()`의 `uploadedKeys` 역순 삭제 패턴과 동일.
- DB 저장 실패 → DB ROLLBACK + 이번 요청에서 새로 올린 S3 파일 전부 보상 삭제.
- 보상 삭제 자체가 실패해도 원래 예외를 덮어쓰지 않는다(로그만 남김) — 기존 정책과 동일.

### 4.2 게시글 수정 + 신규 첨부파일 추가

```
신규 파일 S3 업로드
   ↓
UploadFile 생성
   ↓
BoardAttachment 생성
   ↓
COMMIT
```

- 실패 시 이번에 새로 올린 파일만 보상 삭제. 기존 첨부파일(A/B 등)은 건드리지 않는다.

### 4.3 기존 첨부파일 삭제

```
DB Transaction
 ├─ BoardAttachment 삭제
 └─ UploadFile 삭제
   ↓
COMMIT
   ↓
S3 객체 삭제 (commit 이후)
```

- **기존 파일의 S3 삭제는 DB commit 이후에 수행한다.** 반대 순서(S3 먼저 삭제 후 DB 삭제/ROLLBACK)로 하면 DB에는 파일이 있다고 나오는데 실제로는 사라지는 정합성 문제가 생긴다. Commit 이후 S3 삭제가 실패해도 DB 정합성은 유지되고 S3에 미사용 객체 하나가 남는 정도라 운영적으로 재정리 가능 — Application 도메인의 재업로드에서 이미 적용한 "기존 S3 파일은 transaction commit 이후 삭제" 원칙과 동일.

### 4.4 게시글 자체 삭제

```
DB Transaction
 └─ Board 삭제
     ├─ BoardAttachment 전체 삭제(cascade)
     └─ 연결된 UploadFile 전체 삭제
   ↓
COMMIT
   ↓
연결됐던 S3 객체 전체 삭제
```

- `BoardAttachment`만 지우고 `UploadFile`을 남길 이유가 없다 — 해당 `UploadFile`은 `BOARD_ATTACHMENT` 용도로 그 게시글 전용으로 생성된 것이라, `Board`가 사라지면 참조하는 곳이 없는 고아 데이터가 된다. `UploadFile`/S3까지 함께 완전히 삭제하는 정책으로 확정.

## 5. 첨부파일 검증 정책

### 5.1 `BoardAttachmentValidator` 신규 — `ApplicationPhotoValidator`와 분리

✅ 확정: 공지사항 첨부파일은 이미지가 아니라 **일반 문서(pdf/hwp/docx/xlsx 등) 위주가 실제 요구사항**이다(프론트 mock의 `notices[].attachment`가 `.txt` 같은 문서 파일인 것이 근거). `ApplicationPhotoValidator`는 얼굴사진 전용으로 EXIF 파싱·이미지 디코딩·해상도 검증까지 포함된 특수 목적 컴포넌트라 그대로 재사용할 수 없다.

**책임을 분리한다**: `ApplicationPhotoValidator`는 얼굴사진이라는 특수 목적(EXIF, 이미지 디코딩, 해상도)을 계속 전담하고, 신규 `BoardAttachmentValidator`가 일반 첨부파일이라는 별개의 보안 경계를 전담한다.

- 검증 단계: 파일 크기 → 확장자/MIME 허용목록 → (이미지인 경우만) 바이너리 시그니처. 문서 포맷(pdf/hwp/docx 등)은 이미지처럼 표준화된 magic number 판별이 마땅치 않아 시그니처 검증은 이미지 확장자에 한해서만 적용하고, 문서는 확장자/MIME/크기까지만 검증한다.
- 허용 확장자/MIME 목록, 최대 용량은 **DB로 관리하지 않는다** — 운영 중 임의로 바뀌는 동적 설정까지는 불필요하고, `application.yml` 또는 코드 상수(Properties)로 충분하다. 변경 이력도 Git으로 추적된다.

### 5.2 확정된 상한값

| 항목 | 값 | 근거 |
|---|---|---|
| 첨부파일 개수 | 게시글 1건당 최대 10개 | Review 사진 첨부 제안값(10장)과 통일 — 앱 전체에서 "다중 첨부" 기본값 일관성 유지 |
| 첨부파일 1개 용량 | 최대 10MB | 기존 `ErrorCode.FILE_TOO_LARGE` 공용 메시지("파일 크기는 10MB를 초과할 수 없습니다")와 이미 일치하는 값. `ApplicationPhotoValidator`의 5MB는 사진 전용 기준이라 문서 첨부에는 그대로 적용하지 않음 |

### 5.3 FAQ 목록 페이징

✅ 확정: 별도 엔드포인트/응답 형태로 분기하지 않고 **NOTICE와 동일한 페이징 API(`GET /api/boards?type=...&page=&size=`)를 그대로 재사용**한다. FAQ는 항목 수가 적어 프론트가 충분히 큰 `size`로 한 번에 전부 가져오면 되므로, API 계약을 게시판 종류별로 나눌 이유가 없다(§0에서 이미 정한 "이넘 기반 공용 게시판" 방향과 일관).

### 5.4 `created_by_user_id` 노출 여부

✅ 확정: 공개 API 응답에 포함하지 않는다. `NoticeDetailPage`/`FaqPage` 등 프론트 어디에도 작성자명을 노출하는 화면이 없음을 확인했다(제목/날짜/본문/첨부파일뿐 — Review와 달리 작성자 표시 UI 자체가 없음). 따라서 "작성자가 탈퇴하면 어떻게 표시하나" 문제 자체가 발생하지 않는다 — `created_by_user_id`는 순수 내부 감사(추후 관리자 화면 등)용으로만 남겨둔다.

## 6. 확정하지 않은 것 (다음 단계)

- ✅ API 엔드포인트 목록/정확한 요청·응답 JSON 형식 — [api.md](api.md)로 확정 및 구현 완료(2026-08-14).
- NOTICE 첨부파일 교체/추가/삭제 흐름(수정 API에서 기존 첨부 유지/개별 삭제/신규 추가) — 이번 패스는 QnA(FAQ) 기준 CRUD 골격만 확정, 다음 패스에서 별도 설계.
- 프론트 첨부파일 업로드 UI(`ContentAdminPanel.tsx`에 파일 입력 자체가 없음) — 프론트 담당자 영역.
