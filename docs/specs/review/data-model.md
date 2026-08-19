# Review Data Model

> Review(후기) 도메인의 엔티티, 컬럼, 관계 및 제약조건에 대한 Source of Truth입니다.
> 업무 요구사항은 [api.md](api.md)에 함께 정리되어 있습니다(도메인 규모가 작아 requirements.md를 별도로 두지 않음).
>
> ✅ 2026-08-09 전면 개정: `frontend`(모노레포에 새로 동기화된 실제 화면)를 기준으로 재작성. 이전 버전(2026-08-06)은 카드종류 다중선택(체크박스)·사진 다중첨부를 전제로 `ReviewCardType`/`ReviewImage` join 엔티티를 뒀으나, 실제 화면(`ReviewEditorPage.tsx`)은 **카드종류 단일선택(라디오)·사진 1장(선택)** 구조다. 이번 개정에서 두 join 엔티티를 전부 제거하고 `Review`에 컬럼 2개(`card_type_id`, `image_path`)를 추가하는 것으로 단순화했다.
>
> ✅ 2026-08-06 확정 사항(유지): **실제 카드 발급 이력이 있는 사람만 후기를 쓸 수 있다.** 신청유형·카드종류는 자유 입력이 아니라 로그인 계정의 실제 신청 이력을 서버가 조회해서 검증한다. 자세한 규칙은 §2 참고(다중선택 전제였던 부분만 단일값 기준으로 갱신).

## 1. Review (후기)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, IDENTITY | 후기 ID |
| user_id | BIGINT | NOT NULL | 실제 작성 계정(`User.id`). `arch.md` §5.1 원칙에 따라 JPA 연관관계로 두지 않는다(`Application.userId`와 동일 방식) — 스푸핑 방지·수정/삭제 권한 판단·향후 "내 후기" 조회에 사용할 서버 판단 기준. 로그인 세션에서 서버가 채움, 요청 값으로 받지 않음. **회원탈퇴(하드 삭제) 후에도 이 값은 그대로 남고 Review 본문은 삭제하지 않는다**(2026-08-19 확정, `arch.md` §4.1 "탈퇴 정책"·§4.7, 원본 `docs/collab/user.md` §10 참고) — `author_display_name`이 `User.name`과 독립된 스냅샷이라 표시에 영향 없음. 탈퇴한 작성자 본인은 이후 수정·삭제 불가(로그인 자체가 불가하므로). `author_display_name` 익명화는 권장 사항일 뿐 아직 확정 아님(콘텐츠 운영정책으로 별도 결정 필요) |
| author_display_name | VARCHAR(50) | NOT NULL | 로그인 사용자 이름을 자동 사용하지 않고 작성자가 직접 입력하는 표시용 이름. `User.name`과 다를 수 있음 |
| title | VARCHAR(100) | NOT NULL | 제목 |
| application_type | ENUM | NOT NULL | `INDIVIDUAL`, `GROUP` — Application 도메인의 `ApplicationType` 재사용(신규 enum 안 만듦). 저장 전 실제 신청 이력과 대조 검증(§2) |
| card_type_id | BIGINT | NOT NULL | ✅ 2026-08-09 변경(구 `ReviewCardType`, 다중선택 → 단일선택). `CardType.id`를 **직접 저장**한다 — `Application.cardTypeId`와 동일한 패턴(코드가 아니라 ID). 관계 없이 ID만 저장(`arch.md` §5.1) |
| image_path | VARCHAR(500) | NULL | ✅ 2026-08-09 신규(구 `ReviewImage`, 다중첨부 → 0~1장). S3 key만 저장 — `ApplicationMember.photo_path`와 동일한 패턴(파일 1장을 표현하는 로우이므로 별도 join 엔티티 불필요, `UploadFile`도 거치지 않음). 사진 없으면 `NULL` |
| content | TEXT | NOT NULL | 본문 |
| view_count | — | — | ❌ 포함하지 않음(§3) |

`Review extends BaseTimeEntity` — `created_at`/`updated_at`은 공통 규칙대로 자동 관리.

**왜 `card_type_id`는 코드가 아니라 ID로 바뀌었는가**: 이전 버전(다중선택)은 "리뷰 1건에 여러 태그가 딸린 분류값"이라 안정적인 enum 코드로 저장하는 게 맞았다. 지금은 "리뷰 1건이 카드종류 1개를 가리키는 일반적인 FK 관계"에 더 가까워서, `Application.cardTypeId`와 똑같이 ID로 저장하는 게 이 프로젝트 전체 컨벤션과 일치한다. 등록 시 입력도 조회 시 표시도 모두 ID 기준이라 코드↔ID 변환 로직 자체가 필요 없어졌다(이전 버전에 있던 `CardTypeRepository.findByCode()` 신규 추가 요구사항은 철회 — 기존 `findById()`로 충분).

## 2. 자격 검증 — "실제 카드 이력이 있는 사람만" (2026-08-06 확정, 단일값 기준으로 갱신)

`Application.user_id`(신청서를 제출한 계정) 기준으로만 검증하면 **단체 신청의 실제 카드 수령자(개별 구성원)** 가 제외된다 — 단체 신청은 회사 담당자 1명(대표 신청인, `Applicant`)이 제출하고, 실제 카드를 받는 사람들은 `ApplicationMember`(엑셀 행별 개인, 자체 계정 연결 없음)로만 존재하기 때문이다. 그래서 "신청서를 낸 계정"이 아니라 **"개인정보(이메일)로 자기 카드를 조회할 수 있는 사람"** 기준으로 검증한다 — `POST /api/applications/lookup`의 본인확인 방식과 같은 사고방식이다.

**자격 있는 (신청유형, 카드종류) 조합 계산:**

로그인한 `User.email`과 아래 둘 중 하나가 일치하는 `Application`을 전부 찾는다.
- `Applicant.email`(개인 신청 대표자, 또는 단체 신청을 직접 제출한 담당자 본인)
- `ApplicationMember.email`(단체 신청의 구성원 개인 — 본인이 그 이메일로 이 서비스에 별도 가입되어 있어야 매칭됨)

매칭된 각 `Application`에서 `(application_type, card_type_id)` 쌍을 뽑아 합친 것이 "이 사용자가 후기에서 주장할 수 있는 자격 집합"이다. `Application.cardTypeId`와 `Review.card_type_id`가 이제 같은 표현(ID)이라 코드 변환 없이 바로 비교 가능하다.

**등록 시 검증**: `request.applicationType`+`request.cardTypeId` 조합이 이 자격 집합에 있어야 한다. 아니면 거절(`REVIEW_NOT_ELIGIBLE`, api.md 참고).

- 매칭 대상 `Application.status` 최소 조건: `COMPLETED`(카드 실제 발급 완료)만 인정 — ✅ 확정.

**✅ 2026-08-13 확정 — 작성 개수 제한**: 자격 있는 (신청유형, 카드종류) 조합마다 후기를 하나씩 쓸 수 있다("한 신청당 한 개"). 위 §3에서 `Review`가 특정 `Application` row를 가리키지 않기로 한 결정을 유지하기 위해, 판단 기준은 실제 `Application` row 개수가 아니라 **`(user_id, application_type, card_type_id)` 조합의 유일성**이다 — 같은 조합의 신청을 실제로 여러 번 했더라도 후기는 1개만 허용한다. 위반 시 `REVIEW_ALREADY_EXISTS`(409). 상세는 api.md §API 1/§API 5 참고.

## 3. 검토했지만 이번 설계에 넣지 않은 것

- **조회수(`view_count`)**: 프론트 화면에 노출이 없어 이번 범위에 포함하지 않는다. 나중에 필요해지면 컬럼 추가만으로 확장 가능.
- **`Review → Application` FK**: 특정 신청 1건에 못박지 않고, 등록 시점에 이메일 매칭으로 자격만 검증하고 결과값(신청유형/카드종류ID)만 저장하는 방식 유지.

## 4. 파일 검증 규칙 — Review 전용 (2026-08-09 확정, 프론트 기준)

`ApplicationPhotoValidator`(5 MiB, jpg/jpeg/png)를 그대로 재사용하지 않는다 — 프론트 업로드 폼(`ReviewEditorPage.tsx`)이 이미 다른 기준으로 클라이언트 검증을 하고 있어, 백엔드도 동일 기준으로 맞춘다:

| 항목 | Review 사진 | (참고) `ApplicationPhotoValidator` |
|---|---|---|
| 최대 용량 | **2 MiB** | 5 MiB |
| 허용 확장자/MIME | **jpg, jpeg, png, webp** | jpg, jpeg, png(webp 제외) |
| 개수 | **0~1장** | — |
| 해상도 하한 | 없음 | 300×400(얼굴사진 전용) |

서버 검증은 프론트의 용량 체크(`file.size > 2 * 1024 * 1024`)에만 의존하지 않고 동일 기준으로 서버에서도 다시 검증한다(클라이언트 검증은 우회 가능). `ApplicationPhotoValidator`와 기준이 달라 그대로 재사용할 수 없으므로, Review 전용 검증기(또는 파라미터화된 공용 로직)가 필요하다 — 상세 구현은 api.md 참고.

## 5. 목록 필터 — `hasPhoto` (2026-08-09 신규 발견)

프론트 목록 화면(`ReviewsPage.tsx`)에 "사진 모아보기"(사진 있는 후기만) 필터가 이미 있다. `image_path IS NOT NULL` 조건으로 필터링 — api.md §API 2 참고.
