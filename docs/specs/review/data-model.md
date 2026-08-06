# Review Data Model

> Review(후기) 도메인의 엔티티, 컬럼, 관계 및 제약조건에 대한 Source of Truth입니다.
> 업무 요구사항은 [api.md](api.md)에 함께 정리되어 있습니다(도메인 규모가 작아 requirements.md를 별도로 두지 않음).
>
> ✅ 2026-08-06: 후기 작성 요구사항 변경(제목/신청유형/카드종류/작성자명 수동입력/사진 다중첨부/내용)에 따라 신규 설계. 기존 `docs/api/upload-file.md`에 남아있던 "`Review.thumbnail_file_id`(단일 썸네일)" 가정은 이 문서로 대체됨 — 사진이 0개 이상 다중 첨부로 바뀌면서 단일 FK 컬럼 방식은 더 이상 맞지 않음.
>
> ✅ 2026-08-06 추가 확정: **실제 카드 발급 이력이 있는 사람만 후기를 쓸 수 있다.** 특정 `Application` 1건에 FK로 못박지는 않되(체크박스로 여러 카드종류 경험을 한 후기에 묶을 수 있어야 하므로), 신청유형·카드종류는 자유 입력이 아니라 **로그인 계정의 실제 신청 이력을 서버가 조회해서 검증**한다. 자세한 규칙은 §2.1 참고.

## 1. Review (후기)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | 후기 ID |
| user_id | BIGINT | FK → User, NOT NULL | 실제 작성 계정. **화면에 노출되는 이름과는 별개** — 스푸핑 방지·수정/삭제 권한 판단·향후 "내 후기" 조회 등에 사용할 서버 판단 기준. 로그인 세션에서 서버가 채움, 요청 값으로 받지 않음 |
| author_display_name | VARCHAR(50) | NOT NULL | ✅ 확정: **로그인 사용자 이름을 자동 사용하지 않고, 작성자가 직접 입력**하는 표시용 이름. `User.name`과 다를 수 있음(예: 단체 후기 작성 시 담당자 개인명이 아닌 조직명을 적고 싶을 수 있음) |
| title | VARCHAR(100) | NOT NULL | 제목 |
| application_type | ENUM | NOT NULL | `INDIVIDUAL`, `GROUP` — Application 도메인의 `ApplicationType`과 동일한 enum 재사용(신규 enum 만들지 않음). `Application` 레코드에 FK로 연결하지는 않지만, 저장 전에 로그인 사용자의 실제 신청 이력과 대조해서 검증한다 — §2.1 참고 |
| content | TEXT | NOT NULL | 본문. 최대 글자수는 [TBD] — 기존 프론트 mock(`ReviewEditorPage.tsx`)은 `maxLength=3000`을 쓰고 있었으나 이번 요구사항 변경에서 재확인된 값은 아님 |
| view_count | — | — | ❌ 이번 설계에 포함하지 않음(아래 §4 참고) |

`Review extends BaseTimeEntity` — `created_at`/`updated_at`은 공통 규칙대로 자동 관리.

## 2. ReviewCardType (신청한 카드 종류 — 체크박스 다중 선택)

`Review : CardTypeCode` = 1 : N. 별도 Entity 클래스 대신 `@ElementCollection`으로 구현(아래 §5 참고).

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| review_id | BIGINT | PK(복합) / FK → Review | |
| card_type_code | ENUM | PK(복합), NOT NULL | `CardTypeCode`(`HONOR_KOREAN`/`HONOR_CITIZEN`/`VISITOR`/`STUDENT`) 재사용. `CardType.id`가 아니라 **코드로 저장** — 카드 종류 자체가 관리자 CRUD 대상(가격 등)인 것과 달리, 후기 태그는 순수 분류값이라 안정적인 enum 코드를 직접 쓰는 게 맞음(코드로 분기한다는 기존 원칙과 동일선상) |

- ✅ 확정(추론): 요구사항에서 사진은 "0개 이상"이라고 명시했지만 카드 종류는 그런 표현이 없어 **최소 1개 이상 선택 필수**로 해석. [TBD — 확인 필요: 0개(미선택) 허용 여부]
- 테이블명: `review_card_types`

### 2.1 자격 검증 — "실제 카드 이력이 있는 사람만" (2026-08-06 확정)

`Application.user_id`(신청서를 제출한 계정) 기준으로만 검증하면 **단체 신청의 실제 카드 수령자(개별 구성원)** 가 제외된다 — 단체 신청은 회사 담당자 1명(대표 신청인, `Applicant`)이 제출하고, 실제 카드를 받는 사람들은 `ApplicationMember`(엑셀 행별 개인, 자체 계정 연결 없음)로만 존재하기 때문이다. 그래서 "신청서를 낸 계정"이 아니라 **"개인정보(이메일)로 자기 카드를 조회할 수 있는 사람"** 기준으로 검증한다 — `POST /api/applications/lookup`의 본인확인 방식과 같은 사고방식이다.

**자격 있는 (신청유형, 카드종류) 조합 계산:**

로그인한 `User.email`과 아래 둘 중 하나가 일치하는 `Application`을 전부 찾는다.
- `Applicant.email`(개인 신청 대표자, 또는 단체 신청을 직접 제출한 담당자 본인)
- `ApplicationMember.email`(단체 신청의 구성원 개인 — 본인이 그 이메일로 이 서비스에 별도 가입되어 있어야 매칭됨. 개인 신청의 `ApplicationMember.email`은 항상 NULL이라 매칭 대상 아님, `Applicant.email`이 이미 커버함)

매칭된 각 `Application`에서 `(application_type, card_type.code)` 쌍을 뽑아 합친 것이 "이 사용자가 후기에서 주장할 수 있는 자격 집합"이다.

**등록 시 검증**: `request.applicationType`과 `request.cardTypeCodes`의 각 코드 조합이 이 자격 집합의 부분집합이어야 한다. 아니면 거절(`REVIEW_NOT_ELIGIBLE`, api.md 참고).

- 매칭 대상 `Application.status` 최소 조건: [TBD — 확인 필요]. `COMPLETED`(카드 실제 발급 완료)만 인정하는 걸 제안 — "카드 정보를 조회할 수 있다"는 표현 자체는 `lookup` API처럼 상태 무관하게도 해석 가능하지만, 후기는 실제로 카드를 받은 경험을 전제하는 게 자연스러움.
- 체크박스 UI가 이 자격 집합만 옵션으로 보여줄 수 있도록, 프론트가 미리 조회할 수 있는 "내가 후기 쓸 수 있는 카드종류 목록" API가 있으면 UX상 좋지만 이번 3개 API 범위 밖 — TODO.md에 후속 과제로 기록. 지금은 프론트가 이걸 모른 채 제출했다가 `REVIEW_NOT_ELIGIBLE`로 거절당하는 흐름만 가능.

## 3. ReviewImage (후기 사진, 0장 이상 다중 첨부)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | |
| review_id | BIGINT | FK → Review, NOT NULL | |
| upload_file_id | BIGINT | FK → UploadFile, NOT NULL | 실제 파일 메타데이터(경로/MIME/크기)는 `UploadFile`에 저장 — 아래 §5 참고 |
| display_order | INT | NOT NULL | 0부터 시작하는 노출 순서(사용자가 첨부한 순서) |

`ReviewImage extends BaseTimeEntity`. 테이블명: `review_images`.

- 최대 첨부 개수: [TBD] — 우선 10장으로 제안(운영 중 조정 가능하도록 상수화 권장, DB 제약으로 강제하지 않음)
- 파일 검증 규칙: `ApplicationPhotoValidator`가 이미 구현한 규칙(5 MiB 이하, jpg/jpeg/png, MIME+signature+디코딩 검증)을 재사용 권장 — 새 Validator를 중복 구현하지 않음(아래 §6 구현 메모)

## 4. 검토했지만 이번 설계에 넣지 않은 것

- **조회수(`view_count`)**: 요구사항 어디에도 "노출한다"는 언급이 없고, 넣으면 상세 조회 API가 GET인데도 매번 쓰기가 발생(동시성/캐싱 고려 필요)해서 이번 범위에서는 넣지 않는 것을 제안. 나중에 필요해지면 `Review.view_count` 컬럼 추가만으로 확장 가능(API 응답 형태를 미리 바꿔둘 필요 없음). **[TBD — 확인 필요]**
- **`Review → Application` FK**: (2026-08-06 결정 반영) 특정 신청 1건에 못박는 FK 대신, §2.1처럼 등록 시점에 이메일 매칭으로 자격만 검증하고 결과값(신청유형/카드종류)만 저장하는 방식으로 확정 — 한 후기가 여러 카드종류 경험을 체크박스로 묶을 수 있어야 해서 FK 1개로는 표현이 안 됨.

## 5. Entity 설계 판단 — 사진을 UploadFile 재사용 vs 별도 Entity

**결론: `UploadFile`은 그대로 재사용하고, `Review`와의 N:1 다중 연결을 위해 별도 `ReviewImage`(join 성격) Entity를 둔다.**

근거:
- `UploadFile`은 현재 구조상 **완전히 소유자 없는 공용 메타데이터 테이블**이다 — 어떤 컬럼도 자신을 참조하는 도메인을 향해 역참조하지 않고, 항상 소유 도메인 쪽에서 `..._file_id`로 참조한다(`docs/api/upload-file.md`: "UploadFile은 아무것도 참조하지 않는다"). `Application.logo_file_id`/`seal_file_id`/`submit_file_id`가 이 패턴의 선례다.
- 다만 그 선례들은 전부 **1:1**(신청 1건당 로고 1개)이라 컬럼 하나로 충분했다. Review는 **1:N**(후기 1건당 사진 0~N개)이라 컬럼 하나로 표현이 안 되고, 중간에 순서(`display_order`)를 들고 있을 곳도 필요하다.
- `ApplicationMember.photo_path`처럼 파일 정보를 문자열로 직접 들고 있는 선례도 있지만, 이건 "그 Entity 자체가 사진 1장을 표현하는 사람 단위 로우"라 가능한 패턴이다(카드 1장 = 사람 1명 = 사진 1장). Review는 그런 구조가 아니라 그대로 가져올 수 없다.
- 따라서 `UploadFile`(파일 자체의 메타데이터)은 그대로 재사용하고, `ReviewImage`(`review_id` + `upload_file_id` + `display_order`)가 "이 후기에 이 파일들이 이 순서로 첨부되어 있다"는 관계만 담당하는 얇은 join 성격 Entity로 둔다. `ReviewImage`는 파일 관련 필드(경로/MIME/크기 등)를 중복 저장하지 않는다.

## 6. 구현 메모(설계 참고용, 이번 범위는 설계까지)

- `ApplicationPhotoValidator`를 Review 사진 검증에도 재사용할 수 있도록 일반화(예: 파일 하나를 검증하는 공용 메서드로 추출)하는 것을 권장 — 최소 해상도 검증(300×400)은 얼굴사진 전용 규칙이라 Review 사진에는 적용하지 않는 편이 자연스러움(학교 로고/직인 검증 때와 동일하게 해상도 검증만 제외).
- 페이징 응답은 프로젝트에 아직 없는 첫 사례라 공용 `PageResponse<T>`(content/page/size/totalElements/totalPages)를 `common/response/`에 신설하는 것을 제안 — [api.md](api.md) §공통 참고.
