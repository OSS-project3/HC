# TODO (작업 보드)

상태 아이콘: ✅ 완료 · 🔵 진행중 · ⚪ 대기 · 🔴 블로킹(질문 대기)

작업을 시작할 때 상태를 `🔵 진행중`으로, 담당자를 채우고 커밋한다.
작업을 마칠 때 `✅ 완료`로 바꾸고 `CHANGELOG.md`에 항목을 추가한다.

---

## 📌 개발 원칙 (고정 — 모든 Task 공통, 예외 없음)

이 TODO에 정의한 개발 순서는 **반드시** 따른다. (아래는 "Application 개인 신청 리팩터링 로드맵"의 "공통 진입·완료 원칙"과 같은 내용을 더 짧게 고정해둔 버전 — 세부 절차는 그쪽 참고)

1. 정책에 맞는 테스트를 먼저 작성한다.
2. 신규 테스트가 기존 구현에서 **의도대로 실패**하는 것을 확인한다.
3. 테스트를 통과시키는 **최소한의** 구현만 추가한다.
4. 기존 동작은 변경하지 않는다.
5. Task 범위를 벗어나는 Factory/Context/Generator 등의 리팩터링은 진행하지 않는다.
6. 완료 후 **테스트 결과와 변경 사항을 보고**하고 `TODO.md`/`CHANGELOG.md`를 업데이트한다.

## 템플릿 (새 작업 추가 시)

```md
| ⚪ | {작업명} | {담당: Claude/Codex/미정} | {브랜치} | {관련 문서} | {비고} |
```

---

## 진행 보드

| 상태 | 작업 | 담당 | 브랜치 | 관련 문서 | 비고 |
|---|---|---|---|---|---|
| ✅ | User CRUD (조회/수정/탈퇴/자동복구/익명화 스케줄러) 구현+테스트 | Claude | `backend-api` (병합됨) | `backend/honor-citizen/docs/test/user-test-result.md` | - |
| ✅ | API-명세.md → `docs/api/*.md` 도메인별 분리 | Codex | `feature/application-domain-docs` | `docs/api/README.md` | 원본과 대조하여 내용 유실 없음 확인 완료 |
| ✅ | Application 문서 도메인 패키지 이전 | Codex | `feature/application-domain-docs` | `docs/specs/application/` | requirements/data-model/api/checklist 구성 및 구 경로 참조 수정 완료 |
| ✅ | `arch.md` 구조를 실제 코드 규모에 맞게 단순화 | Claude | `feature/application-domain-docs` | `arch.md` | 비즈니스 규칙 절은 유지, 계층/패키지 구조만 축소 |
| ✅ | 협업 규칙 체계(`docs/collab/`) 도입 | Claude | `backend-api` | `docs/collab/RULES.md` | - |
| ✅ | Application 도메인 엔티티/API 구현 | Claude | `feature/application-domain-impl` | `docs/specs/application/*.md` | API 1~5 전부 완료, 신규 테스트 46개 전부 통과. checklist.md 6개 섹션 검증 완료(결과는 HANDOFF.md 참고) |
| ⚪ | Codex: HANDOFF.md "확인 필요" 3건을 `docs/specs/application/*`에 반영 | Codex | `feature/application-domain-docs` | `docs/specs/application/{requirements,data-model,api}.md` | englishName 추가, total_price 보류 각주, 엑셀 부분실패=전체거부 확정 — 사람 확인 끝난 결정사항, 문서 반영만 필요 |
| ✅ | 조회(`lookup`) 인증 정책 method별 분리 구현 | Claude | `feature/application-domain-impl` | `docs/specs/application/api.md`, `backend/FRONTEND_API_REQUIREMENTS.md`(main) | `application`=phone+email 둘 다 필수, `card`=인증값 없음. 상세는 CHANGELOG 2026-08-06 참고 |
| ✅ | `CardTypeSeeder` 추가 — CardType ID 1~4 고정 시딩 | Claude | `feature/application-domain-impl` | `backend/FRONTEND_API_REQUIREMENTS.md`(main) §4 | 프론트 `cardTypeId` 하드코딩(1~4)을 그대로 쓰기로 결정, `GET /api/card-types` 신규 API는 만들지 않음 |
| ⚪ | 단체 재제출 UI(`MobileCardPage.tsx`) 추가 | 프론트 담당자 | `main` | `backend/FRONTEND_API_REQUIREMENTS.md`(main) | 백엔드는 이미 구현됨(`PATCH .../photo`의 `submitFile` 파트, `PHOTO_REJECTED` 상태에서만 허용). 프론트에 단체용 업로드 UI만 없음 |
| ⚪ | `StepInfo.tsx`에 4개 카드종류 전부 사주정보 입력폼 추가 | 프론트 담당자 | `main` | `docs/specs/application/requirements.md` §7-3 | 현재 방문증에만 있음. 나머지 3종은 목데이터라 안 드러날 뿐 실제 API 연동 시 400 발생 |
| ⚪ | Payment/상담금액/자동취소/환불 도메인 설계·구현 | 미정 | - | `docs/specs/application/requirements.md` | 이번 Application 구현 범위 밖(api.md 스코프 노트 참고). checklist.md 1절 미충족 3건이 여기 해당 |
| ⚪ | Admin 도메인(사진검토/작명/카드발급/CardDesign 배정) 설계·구현 | 미정 | - | `docs/specs/application/api.md` | 아직 미착수, api.md에 "이번 범위 아님"으로 명시돼 있던 부분 |
| ⚪ | CardDesign 관리자 배정 API/화면 흐름 확정 | 미정 | - | `docs/specs/application/requirements.md` | "CardDesign 배정 시점" TBD 선결 필요 |
| ✅ | 학생증 학번 형식 정책 문서 반영(학과는 계속 보류) | Codex | codexdocs/application-policy-sync | `docs/specs/application/requirements.md` | 학번 최대 10자·숫자만 반영. 학과는 `APPLICATION.md`가 "제외"로 적었으나 근거 없어 미결정 유지, 기존 필수 정책 그대로 둠(`PENDING_DECISIONS.md` 참고) |
| ⚪ | 학생증 디자인 시안 반영 | 미정 | - | `DB.md`, `docs/api/card-design.md` | 시안 미도착 |
| ⚪ | 단체신청 엑셀 ZIP 레이아웃(공통입국날짜 셀 위치, 컬럼 순서) 명문화 | 미정 | - | `docs/specs/application/api.md` | 구현 시 임의로 확정한 레이아웃(HANDOFF.md 참고) — bulk/template API 설계 시 반드시 일치 필요 |
| ⚪ | 신청내용 수정 API 필요 여부 결정 | 미정 | - | `docs/specs/application/api.md` | TBD |
| ⚪ | MOBILE_AND_PHYSICAL 실물배송 흐름 정의 | 미정 | - | `docs/specs/application/api.md` | TBD |
| ⚪ | 영업일 계산 기준 확정 | 미정 | - | `docs/specs/application/api.md` | TBD |
| ⚪ | 입금 기한 계산 기준(신청일 포함 여부·마감 시각) 확정 | 미정 | - | `docs/specs/application/requirements.md` | TBD |
| ✅ | 전체 코드베이스 감사 + 죽은 코드/아키텍처 위반 정리 | Claude | `backend-api` | HANDOFF.md | infra/card·domain/photo·UploadController·TokenRefreshRequest/Response·ErrorCode.DUPLICATE_APPLICATION 삭제, ApplicationService의 UserRepository 직접 주입(arch.md 위반) → UserService 경유로 수정 |
| ✅ | User 조회 문서 정합성 정리 | Codex | `main` | `docs/api/user.md` | `GET /api/users/me`는 `UserController#getMe`로 구현 완료. 코드 수정 없이 문서 stale 문구만 정리 |
| ✅ | Review 도메인 재설계(2026-08-09, 프론트 정렬) | Claude | `main` | `docs/specs/review/{data-model,api}.md`, `arch.md` §4.7 | 모노레포에 동기화된 실제 프론트(카드종류 단일선택·사진 0~1장) 기준으로 전면 재작성. `ReviewCardType`/`ReviewImage` join 엔티티 제거(`card_type_id`/`image_path` 컬럼으로 대체). CRUD 5개 API(등록/목록/단건/삭제/수정) 전부 설계 완료. 이전 "Review [TBD] 확인 필요 4건"·"사진 첨부 최대 개수" 항목은 이 재설계로 전부 해소(카드종류 0개 불허 확정, 본문 글자수 제한 없음, 조회수 미포함 확정, `Application.status=COMPLETED`만 인정 확정, 사진은 0~1장이라 "최대 개수" 자체가 무의미해짐) |
| ✅ | Review 도메인 구현 (2026-08-13) | Claude | `main` | `docs/specs/review/{data-model,api}.md` | CRUD 5개 API(등록/목록/단건/삭제/수정) 전부 구현+테스트 완료(신규 테스트 76개, 전부 통과). `Review` 엔티티/`ReviewRepository`(`JpaSpecificationExecutor`), `ReviewImageValidator`(2MB, jpg/jpeg/png/webp — webp 디코딩을 위해 `com.twelvemonkeys.imageio:imageio-webp` 신규 의존성 추가), `ReviewEligibilityService`(실 신청이력 검증+조합당 1개 제한+탈퇴계정 등록차단), `ReviewService`/`ReviewController` 신규. `ErrorCode`에 `REVIEW_NOT_FOUND`/`REVIEW_NOT_ELIGIBLE`/`REVIEW_ALREADY_EXISTS`/`INVALID_IMAGE_FILE` 추가. 공용 `PageResponse<T>` 신설(프로젝트 첫 페이징 API). `SecurityConfig`에 `GET /api/reviews`·`GET /api/reviews/{id}` `permitAll()` 추가. 세션 중 사용자 확인 거쳐 2가지 신규 정책 확정: (1) 후기 작성은 자격 있는 (신청유형,카드종류) 조합당 1개로 제한(2026-08-13) (2) 탈퇴 계정은 신규 후기 등록 불가, 단 기존 후기 수정에는 미적용(2026-08-13). 상세는 CHANGELOG 참고 |
| ⚪ | "내가 후기 쓸 수 있는 카드종류" 조회 API 추가 여부 결정 | 미정 | - | `docs/specs/review/api.md` | 없으면 프론트가 체크박스 옵션을 모른 채 제출 → `REVIEW_NOT_ELIGIBLE`로 사후 거절만 가능. UX상 필요해 보이나 이번 3개 API 범위 밖이라 별도 확정 필요 |
| ⚪ | 프론트: `ReviewEditorPage.tsx` 등 Review 요구사항 변경 반영 | 프론트 담당자 | `main` | `docs/specs/review/api.md` §② | 현재 `author: user.name`으로 로그인 이름 자동 사용 중 — "작성자 직접 입력"으로 변경 필요. 신청유형/카드종류/사진 다중첨부 입력 UI 전체 신규 필요 |
| ⚪ | Codex: `arch.md` 3절 패키지 구조 예시 최신화 | Codex | `feature/application-domain-docs` | `arch.md` | `api/admin`·`infra/toss`는 삭제됨, `domain/uploadfile`·`domain/log`는 예시에 없음, `ApplicationResponse.java`는 이제 `ApplicationCreateResponse` 등으로 분리됨 |
| ✅ | 마이페이지 신청 목록/상세 조회 API 6·7 설계 | Claude | `main` | `docs/specs/application/api.md` API 6/7 | `GET /api/my/applications`(페이징 목록)·`GET /api/my/applications/{id}`(상세, 소유권 검증) 설계만 완료, 구현 안 됨. 기존 `lookup`(API 3)은 비로그인 공개 조회라 별개 |
| ⚪ | 마이페이지 신청 목록/상세 조회 API 6·7 구현 | 미정 | - | `docs/specs/application/api.md` API 6/7 | `ApplicationRepository.findByUserId(...)` 신규 필요, 공용 `PageResponse<T>` 신설 필요(Review 목록조회와 공유) |
| ⚪ | 사용자 신청 취소 정책 문서 확정 | 미정 | `main` | `docs/specs/application/{requirements,api}.md` | ✅ 허용 상태 확정: 신청 소유자는 `PAYMENT_PENDING`, `PHOTO_REJECTED`에서만 취소 가능하며 `RECEIVED` 및 그 이후 상태에서는 취소 불가. 구현 전 취소 후 상태, 소유권 검증, 중복 취소의 멱등성, 허용되지 않은 상태의 ErrorCode, 환불·3일 자동취소 정책과의 관계를 문서로 확정 |
| ⚪ | 사용자 신청 취소 API 구현 | 미정 | `main` | `docs/specs/application/{requirements,api}.md` | 위 정책 문서 확정 후 Controller/Service/Repository와 테스트 구현. 기존 `Application.cancel()`의 상태 전이와 허용 상태를 재검증하고, 본인 신청만 취소 가능하도록 소유권 검증. 정상 취소·권한 없음·신청 없음·허용되지 않은 상태·중복 요청을 테스트 |
| ✅ | 학생증 신청 항목 추가(학교구분·가로형/세로형, 2026-08-14) | Claude | `main` | `docs/specs/application/{data-model,api}.md`, 계획: `C:\Users\gpdnj\.claude\plans\application-api-async-knuth.md` | 구현+테스트 완료. `Application`에 `orientation`(LANDSCAPE/PORTRAIT)·`school_type`(UNIVERSITY/HIGH_SCHOOL) 컬럼 신규 추가(개인·단체 공통, 학생증 전용, 신청서 전체에 1개). `ApplicationMember.student_id`/`department` 필수 조건을 "학생증이면 무조건"→"학생증+`school_type=UNIVERSITY`일 때만"으로 변경(HIGH_SCHOOL이면 있으면 오히려 거절). 단체는 학번·학과를 여전히 엑셀로만 받음(`BulkExcelParser` 변경 없음), orientation/schoolType만 신청 폼 필드로 추가. 카드종류별 config 추상화 없이 기존 `isStudent` boolean 게이트 재사용. `Application.createIndividual`/`createGroup` 팩토리 메서드는 기존 시그니처를 하위호환 오버로드로 유지해 무관한 기존 테스트 ~20개는 손대지 않음. 신규 ErrorCode 없음(`INVALID_INPUT` 재사용). 신규 테스트 12개(`ApplicationServiceTest` 7개, `ApplicationServiceBulkTest` 5개) 전부 통과, 기존 테스트 3개(`ApplicationServiceUploadCompensationTest`) 픽스처 보정 후 통과. 전체 스위트 224개 중 기존과 동일하게 Redis 미기동 3건만 실패(회귀 없음). 프론트(`StepInfo.tsx` 등)는 프론트 담당자 영역이라 미착수 |
| ✅ | Board 도메인(공지사항/FAQ) 구현 (2026-08-14) | Claude | `main` | `docs/specs/board/{data-model,api}.md`, `arch.md` §4.8 | CRUD 5개 API(목록/단건/생성/수정/삭제) 전부 구현+테스트 완료. `Board`+`BoardType{NOTICE,FAQ}` enum 통합 관리, `BoardAttachment` join 엔티티(`Board:UploadFile`=1:N, NOTICE 전용). 신규 `BoardAttachmentValidator`(최대 10개, 1개당 10MB, 문서+이미지 확장자/MIME 허용목록, 이미지만 시그니처 검증). `BoardService`/`BoardController`(공개 GET, `/api/boards`)/`BoardAdminController`(관리자 CRUD, `/api/admin/boards`) 신규. `SecurityConfig`에 `arch.md` §4.6이 이미 명시했으나 코드로는 없었던 `/api/admin/**` → `hasRole("ADMIN")` 신규 추가(이 프로젝트 첫 관리자 전용 쓰기 API), `GET /api/boards`·`GET /api/boards/**` `permitAll()` 추가. 서비스 레벨 권한 분기 없음 — 라우트 레벨 강제로 충분(리소스 소유권 판단이 필요없는 "관리자냐 아니냐"뿐이라 Review의 `canEdit`/`canDelete`와 다름). `ErrorCode.BOARD_NOT_FOUND` 신규. FAQ+첨부파일 요청은 `INVALID_INPUT`으로 거절(2026-08-14 사용자 확인). API 4(수정)는 첨부파일 편집을 이번 패스 범위 밖으로 명시적으로 미루고 `application/json`으로 단순화(당초 `multipart/form-data` 초안에 실체 없는 `attachments` 파트가 남아있던 문서 불일치를 구현 중 발견해 함께 정리). 신규 테스트 34개(`BoardTest` 2개, `BoardAttachmentTest` 1개, `BoardAttachmentValidatorTest` 6개, `BoardServiceTest` 12개, `BoardControllerTest` 5개, `BoardAdminControllerTest` 8개) 전부 통과. 전체 스위트 258개 중 기존과 동일하게 Redis 미기동 관련 3건만 실패(회귀 없음). NOTICE 첨부파일 교체/추가/삭제 흐름과 프론트 업로드 UI는 다음 패스로 이월 |
| ⚪ | Inquiry(1:1 문의) 도메인 신규 구현 | 미정 | `main` | `docs/FRONTEND_API_GAPS.md` §1.1, `docs/FRONTEND_USER_FLOW_AUDIT.md`(Codex, 2026-08-14) "공지·FAQ·문의·행사" | 백엔드에 도메인 자체가 없음(`Inquiry` 엔티티/Controller 전무) — Codex 감사에서도 동일 지적, `InquiryPage`/`InquiryDetailPage`/`MyPage`/`AdminPage`가 전부 `localStorage["customer-inquiries"]` 사용 확인. 데이터 계약(`InquiryRecord`): `category, name, email, phone, title, content, status(PENDING/COMPLETED), answer, answeredAt`. 착수 전 결정 필요: 비회원 문의 등록·조회 허용 여부(Codex 감사도 동일하게 미확정으로 지적) |
| ✅ | Event(행사) 도메인 구현 (2026-08-16) | Claude | `main` | `docs/specs/events/{data-model,api}.md` | CRUD 5개 API(목록/단건/생성/수정/삭제) 전부 구현+테스트 완료. `EventPost`(`EventType{BOOTH,COLLABORATION}`, `visible`/`display_order` 포함)+`EventImage`(상세 갤러리, `UploadFile` 미경유·S3 key 직접 저장 — Review `image_path`와 동일 패턴, Board `BoardAttachment`의 UploadFile join과는 다름). `EventController`(공개 GET, `/api/events`)/`EventAdminController`(관리자 CRUD, `/api/admin/events`) 신규 — `/api/admin/events/**`는 Board 때 추가한 `/api/admin/**` → `hasRole("ADMIN")` 규칙에 코드 변경 없이 자동 편입, `SecurityConfig`엔 공개 GET `permitAll()`만 추가. 서비스 로직: 생성(썸네일+갤러리 S3 업로드 후 DB 트랜잭션, `uploadedKeys` 역순 보상삭제 — Board `create()`와 동일 골격) / 목록(`EventPostRepository.findVisibleByEventType`가 JPQL `ORDER BY display_order ASC NULLS LAST, event_date DESC NULLS LAST, created_at DESC` 고정 정렬 전담, `visible=true`만) / 상세(`visible=false`면 `EVENT_NOT_FOUND`로 존재 자체를 숨김, `next` 없음 — 프론트에 상세 페이지 라우트 자체가 없어 이전/다음 이동 UI가 없음) / 수정(텍스트 필드+`visible`+`displayOrder` 전체 재제출, 썸네일은 새 파일 있을 때만 교체(Review `applyImageChange`와 동일 패턴), 갤러리 편집은 이번 패스 제외) / 삭제(`EventImage`+`EventPost` 한 트랜잭션 삭제 후 커밋 이후 S3 정리, 썸네일도 함께). 신규 `EventImageValidator`(`ReviewImageValidator`와 규칙 동일: 2MB, jpg/jpeg/png/webp — package-private라 재사용 불가 + 도메인별 검증기 독립이 기존 관례라 신규 제작), 신규 ErrorCode `EVENT_NOT_FOUND`. 설계 단계에서 확정한 2가지: (1) `EventImage.representative` 플래그 제거 — `EventPost.thumbnail_image_path`가 대표 이미지 유일 소스 (2) 관리자 전용 전체 목록 API(`GET /api/admin/events`, `visible` 무관)는 이번 패스 제외, 이후 별도 구현. 신규 테스트 39개(`EventPostTest` 3개, `EventImageTest` 1개, `EventImageValidatorTest` 7개, `EventServiceTest` 14개, `EventControllerTest` 6개, `EventAdminControllerTest` 8개) 전부 통과. 전체 스위트 297개 중 기존과 동일하게 Redis 미기동 관련 3건만 실패(회귀 없음) |
| ⚪ | 관리자(Admin) 신청 목록·상태변경·통계 API | 미정 | `main` | `docs/FRONTEND_API_GAPS.md` §1.2, `docs/FRONTEND_USER_FLOW_AUDIT.md` "관리자 의존성" | 기존 "Admin 도메인(사진검토/작명/카드발급/CardDesign 배정)" 행과 별개 — `GET /api/admin/applications`(목록, 상태·유형 필터), `GET /api/admin/applications/{id}`, `PATCH .../status`, `GET /api/admin/stats` 전부 없어 관리자 페이지가 `localStorage["admin-applications"]`로만 동작. 착수 전 확인 필요: 프론트 status enum(`SUBMITTED/CONSULTING/PAYMENT_PENDING/IN_PRODUCTION/COMPLETED/CANCELLED`)이 백엔드 실제 흐름(`PAYMENT_PENDING→RECEIVED→REVIEWING↔PHOTO_REJECTED→NAME_EDITING→PRODUCING→COMPLETED/CANCELLED`)과 달라 매핑 정리 먼저 필요 |
| ⚪ | "내 후기" 목록 조회 API | 미정 | `main` | `docs/FRONTEND_USER_FLOW_AUDIT.md` "후기" | `ReviewController`/`ReviewService.list()`에 로그인 사용자 범위로 좁히는 파라미터가 없음(`cardTypeId`/`hasPhoto`/`searchType`/`keyword`/`page`/`size`뿐, 작성자 필터 없음) — 코드로 확인 완료. 마이페이지 "내 후기" 목록에 필요. `GET /api/reviews/me` 신설 또는 기존 목록에 `mine=true`(로그인 필요) 파라미터 추가 중 택1 |
| ⚪ | 단체신청 Excel 양식 다운로드 API 필요 여부 확정 | 미정 | `main` | `docs/FRONTEND_USER_FLOW_AUDIT.md` "신청" | 백엔드에 template 관련 엔드포인트가 확인되지 않음(코드 검색 결과 없음) — 프론트에도 양식 다운로드 버튼 자체가 없음. 신규 API가 필요한 건지, 정적 파일 제공으로 충분한지 정책 확인부터 필요 |

> 아래 "Task 1~4" 4행 요약은 이 로드맵이 Task 1~6(5-A/5-B 포함)으로 세분화되기 전의 옛 버전이라 삭제함 — 최신 진행 상태는 바로 아래 "Application 개인 신청 리팩터링 로드맵" 절 참고.

---

## Application 개인 신청 리팩터링 로드맵

> 대상 브랜치: `feature/application-domain-impl`
>
> 기준 문서: `arch.md`, `docs/specs/application/requirements.md`, `docs/specs/application/checklist.md`
>
> 구현 순서: Task 1 → Task 2(User Validation) → Task 3(Photo/Student Validation) → Task 4(서버 생성값) → Task 5(Factory) → Task 6(Generator/보상 처리)
>
> 각 Task는 범위를 섞지 않는다. 특히 구조 리팩터링 Task에서는 확정되지 않은 검증 정책이나 운영 개선을 함께 구현하지 않는다.

### 모든 Task의 공통 진입·완료 원칙

각 Task는 반드시 다음 순서로 진행한다.

1. 해당 Task에서 구현하거나 변경할 정책을 Source of Truth인 Requirements/API/DB/Architecture 문서에 먼저 반영한다.
2. 정책이 미결정이거나 문서끼리 충돌하면 구현을 시작하지 않고 `[TBD]` 또는 blocking 항목으로 등록한다.
3. 확정된 정책을 검증하는 테스트를 먼저 작성한다.
4. 신규 테스트가 기존 구현에서 의도한 이유로 실패하는지 확인한다.
5. 테스트를 통과시키는 최소 범위로 구현 또는 리팩터링한다.
6. 관련 도메인 테스트와 전체 테스트를 실행해 회귀 여부를 확인한다.
7. 완료 결과와 남은 실패를 본 체크리스트 및 `CHANGELOG.md`에 반영한다.

추가 원칙:

- Factory, Validator, Generator 같은 구조 자체를 먼저 만들지 않는다. 확정된 정책과 테스트가 요구할 때만 도입한다.
- 리팩터링 Task에서는 별도로 명시하지 않은 Request/Response, 예외, 저장 순서 및 외부 동작을 변경하지 않는다.
- 전체 테스트에 기존 실패가 있으면 기준 브랜치에서도 재현되는지 분리하여 기록한다.

### 구현 중 구조 변경 원칙

Factory, Validator, Context 등 새로운 클래스를 추가하기 전에 반드시 다음을 확인한다.

- 현재 클래스의 private 메서드 분리만으로 해결 가능한가?
- 새로운 클래스가 두 곳 이상에서 재사용되는가?
- 책임이 명확하게 독립되는가?

위 조건을 만족하지 않으면 기존 클래스 내부에 유지하고, 필요성이 생겼을 때만 분리한다.

### Task 1. createIndividual() 메서드 분리

상태: ✅ 완료 — Application 회귀 테스트 통과, 전체 테스트의 기존 User 실패 2건 분리 기록

- [x] `createIndividual()`의 기존 실행 순서를 유지한 채 private 메서드로 분리
- [x] `validateCreateIndividual()` 추출
- [x] `createIndividualApplication()` 추출
- [x] `createIndividualApplicant()` 추출
- [x] 기존 `saveReceiverIfNeeded()` 재사용
- [x] `createIndividualMember()` 추출
- [x] Factory, Validator, Context를 만들지 않음
- [x] 정상 Request/Response와 저장 순서 유지. User 미존재 시 `USER_NOT_FOUND`를 가장 먼저 반환하도록 오류 우선순위만 확정 정책대로 변경
- [x] Application 관련 기존 테스트 전체 통과
- [x] User 존재 여부를 이후 로직의 전제조건으로 정의하고 가장 먼저 조회
- [x] 존재하지 않는 User 테스트를 구현 전에 추가
- [x] User가 없으면 파일 업로드가 호출되지 않는지 검증
- [x] User가 없으면 Application과 하위 Entity가 저장되지 않는지 검증
- [x] 신규 테스트가 기존 구현에서 의도한 이유로 실패하는지 확인
- [x] User 조회를 CardType 조회·신청번호 생성·파일 업로드·DB 저장보다 앞으로 이동
- [x] 새로운 Factory, Validator, Context를 추가하지 않음
- [x] 최소 구현 후 Application 회귀 테스트 실행
- [x] Task 완료 결과를 TODO와 CHANGELOG에 기록
- [ ] 전체 테스트 통과
  - 현재 84개 중 `UserControllerTest` 2개가 독립 실행에서도 실패하며 Task 1 전후 동일
  - 실패 위치: `withdrawMarksUserWithdrawnAndBlacklistsAccessToken()`, `withdrawReturnsAlreadyWithdrawnOnSecondCall()`
  - Application 리팩터링과 무관한 기존 실패인지 기준 브랜치에서 최종 확인 필요

### Task 2. User Validation과 신청 제한

상태: ✅ 완료

#### User 및 신청 자격

- [x] User 조회를 신청번호 생성, 파일 업로드, DB 저장보다 먼저 수행
- [x] 회원 존재 검증 — `USER_NOT_FOUND` 재사용
- [x] 회원 상태가 `ACTIVE`인지 검증 — `ALREADY_WITHDRAWN` 재사용
- [x] 탈퇴 회원 거절 — 신규 ErrorCode 추가 없음
- [x] `USER` 권한 검증 — `FORBIDDEN` 재사용
- [x] 필수 약관 전체 동의 검증 — `TERMS_NOT_AGREED` 재사용
- [x] 하루 3번째 신청 허용, 4번째부터 거절 — 2026-08-16 구현 완료(하단 "일일 KST 3회 제한 DB 원자 처리" 항목 참고)
- [x] 생성 후 `CANCELLED`된 신청은 해당 날짜의 신청 횟수에서 제외(취소 시 자리가 다시 빔) — `ApplicationDailyLimitService.releaseSlot`로 반환 가능하도록 구현. 다만 실제 "신청 취소" API 자체가 아직 없어(별도 TODO 항목) 이 release는 현재 실패 보상(파일 업로드·DB 저장 실패)에서만 호출되고, 취소 흐름에는 아직 연결되지 않음 — 취소 API 구현 시 그 Service 계층에서 이 메서드를 호출하면 됨
- [x] 동일 사용자·동일 카드 종류의 중복 신청 허용 — 별도 차단 로직 추가하지 않음
- [x] 진행 중인 같은 카드 신청이 있다는 이유만으로 차단하지 않음
- [x] Application의 `UserRepository` 직접 의존 제거 및 `UserService` 공개 메서드 사용

- [x] 상태·권한·약관 테스트를 구현 전에 추가하고 기존 구현에서 3건 실패 확인
- [x] 검증 실패 시 파일 업로드와 Application 하위 Entity 저장 미호출 검증
- [x] Application 관련 전체 테스트 통과
- [ ] 전체 테스트 통과 — 87개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음

### Task 3. PhotoValidator와 StudentCardValidator

상태: 🔵 진행중

#### PhotoValidator

- [x] `ApplicationPhotoValidator` 구현 및 적용
- [x] 얼굴사진 필수 및 빈 파일 검증
- [x] 최대 `5 * 1024 * 1024` bytes(5 MiB) 검증
- [x] 허용 확장자 `jpg`, `jpeg`, `png` 검증
- [x] 허용 MIME `image/jpeg`, `image/png` 검증
- [x] 파일 signature 검증
- [x] 실제 이미지 디코딩 가능 여부 검증
- [x] 확장자·MIME·실제 파일 형식 일치 검증
- [x] EXIF Orientation 적용 후 최소 해상도 가로 300px, 세로 400px 검증
- [x] 2MB 및 600×800~1200×1600은 권장 기준으로만 사용하고 거절 조건으로 사용하지 않음
- [x] Validator가 파일 저장이나 DB 변경을 수행하지 않음

#### StudentCardValidator

- [x] 학생증 검증은 재사용·독립 클래스 조건을 충족하지 않아 기존 Service private 메서드로 유지
- [x] 학생증의 학번 필수 검증
- [ ] 학생증의 학번 최대 10자·숫자만 형식 검증 (✅ 2026-08-07 신규, `APPLICATION.md` 기준)
- [x] 학생증의 학과 필수 검증 (2026-08-07: `APPLICATION.md`가 "제외"로 적었으나 근거 없어 미결정 유지, 그대로 둠 — `PENDING_DECISIONS.md` 참고)
- [x] 학생증의 학교 로고 필수 및 얼굴사진과 동일한 파일 검증(최소 해상도 제외)
- [x] 학생증의 학교 직인 필수 및 얼굴사진과 동일한 파일 검증(최소 해상도 제외)
- [x] 학생증이 아닌 신청에서 학생증 전용 입력 거절
- [x] Validator가 파일 저장이나 DB 변경을 수행하지 않음

#### 검증 순서 및 테스트

- [ ] 모든 사용자·신청·파일·학생증 검증을 신청번호 생성 전에 완료
- [ ] 모든 검증을 object storage 업로드 전에 완료
- [ ] 모든 검증을 Application 및 하위 Entity 저장 전에 완료
- [ ] 검증별 ErrorCode와 HTTP 응답 계약 확인
- [ ] 회원 없음·탈퇴·권한 불일치·약관 미동의 테스트
- [ ] 일일 2건 후 3번째 허용, 4번째 거절 테스트
- [ ] 취소 신청의 일일 횟수 포함 테스트
- [ ] 동일 카드 종류 중복 신청 허용 테스트
- [ ] 사진 크기·확장자·MIME·signature·디코딩·해상도 경계값 테스트
- [ ] 학생증 및 비학생증 필드 테스트
- [ ] 검증 실패 시 파일 업로드와 DB 저장이 호출되지 않는지 테스트
- [ ] Application 관련 테스트 전체 통과
- [ ] 전체 테스트 통과

- [x] Validator 테스트를 구현 전에 추가하고 클래스 부재로 컴파일 실패 확인
- [x] 해상도·학생증 공백 실패 시 파일 업로드와 DB 저장 미호출 통합 테스트
- [x] Application 관련 전체 테스트 통과
- [ ] 전체 테스트 통과 — 97개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음

### Task 4. prepareServerValues()와 서버 생성값 정리

상태: ⚪ 대기

- [x] 결제 정책 확정 — 상담 후 신청, 신청 이후 계좌이체, Application 생성 시 `total_price` 미사용
- [x] 단체 사진 식별 정책 확정 — 엑셀 ID와 ZIP 사진 파일명을 매칭하되 ID는 저장하지 않고 별도 사진 파일 ID도 생성하지 않음
- [x] 신청번호·초기 상태·수령인 동일 여부의 결정 주체와 시점을 문서에 확정
- [x] 확정 정책에 맞는 테스트를 구현 전에 작성하고 `isReceiverSameAsApplicant()` 부재로 실패 확인
- [x] 서버 준비값이 서로 다른 책임에 있어 `prepareServerValues()`를 도입하지 않고 기존 지역변수 유지
- [x] 새로운 Context/DTO/Plan 불필요 확인 — 추가하지 않음
- [x] 기존 Request/Response와 저장 순서 유지
- [x] Application 관련 테스트 통과
- [ ] 전체 회귀 테스트 통과 — 99개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음
- [x] TODO와 CHANGELOG 갱신
### Task 5-A. Factory 도입 전 설계 확정

상태: ✅ 완료

- [x] `CreatedApplication`, `CreatedChildren`, Context, Plan 객체를 도입하지 않음
- [x] `Application.id=IDENTITY` 유지, Root 저장 후 발급된 ID로 하위 Entity 생성
- [x] 저장 전 하위 Entity에 null FK를 넣지 않음
- [x] 파일 업로드는 Service가 담당하고 Factory에는 저장된 경로·파일 ID만 전달
- [x] Factory는 Entity 생성만 담당하고 Repository·Storage를 호출하지 않음
- [x] 기존 검증·예외·파일 업로드·저장 순서를 변경하지 않음
- [x] Factory 메서드는 Application Service와 테스트에서만 접근할 수 있도록 package-private로 제한

### Task 5-B. ApplicationFactory 구현

상태: ✅ 완료

- [x] `ApplicationFactory` 생성
- [x] `IndividualCreationContext`를 만들지 않음 — 현재 값 규모와 저장 순서에서는 불필요
- [x] `CreatedApplication` 결과 타입을 만들지 않음 — Root 선저장 구조와 맞지 않음
- [x] 별도 Context를 만들지 않음
- [ ] Request DTO에는 클라이언트 입력과 단순 파생값만 유지
- [ ] Context가 꼭 필요한 경우 검증된 입력, User/CardType 조회 결과, 신청번호 등 실제 서버 준비값만 포함 (`total_price`와 구성원별 사진 파일 ID 제외)
- [x] `createApplication()`, `createApplicant()`, `createReceiver()`, `createMember()`를 package-private로 제한
- [x] 개인 신청 Entity 생성 로직을 Factory로 이동
- [x] Factory가 Repository, object storage, 외부 API를 호출하지 않음
- [x] Entity public Builder를 추가하지 않음
- [x] `ApplicationService`는 검증·파일 준비·Factory 호출·저장 흐름만 조정
- [x] Factory를 package-private로 제한해 Application Service 패키지 밖의 부분 생성을 차단
- [x] 기존 실행 순서, 예외 발생 시점, 파일 업로드 및 저장 순서 유지
- [x] Application 관련 테스트 전체 통과
- [ ] 전체 테스트 통과 — 101개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음
- [x] 패키지 의존 방향 검증 — Factory는 Entity에만 의존

### Task 6-A. ApplicationNumberGenerator와 동시성

상태: ⚪ 대기

- [ ] `ApplicationNumberGenerator` 분리
- [ ] 현재 `count + 1` 방식 제거
- [ ] 신청번호 생성 규칙과 연도 전환 규칙 문서화
- [ ] DB UNIQUE 제약 확인
- [ ] DB 잠금, 번호 발급 테이블 등 동시성 보장 방식 결정
- [ ] 동시 요청에서 신청번호 중복이 발생하지 않는지 테스트
- [ ] 번호 생성 충돌 시 재시도 가능 여부와 범위 정의
- [ ] 번호 생성 실패가 중복 신청 생성으로 이어지지 않는지 확인

### Task 6-B. 파일 보상 처리와 트랜잭션

상태: ⚪ 대기

- [ ] 파일 검증과 실제 업로드 단계 분리
- [ ] 임시 object storage 경로 업로드 적용 여부 결정
- [ ] DB 저장 성공 시 파일 확정
- [ ] DB 저장 실패 시 업로드 파일 보상 삭제
- [ ] 보상 삭제 실패 로그 기록
- [ ] 고아 파일 cleanup job 연계
- [ ] Application과 Applicant/Receiver/ApplicationMember 저장 원자성 확인
- [ ] 외부 파일 작업이 DB 트랜잭션을 불필요하게 오래 점유하지 않는지 점검
- [ ] 장애 단계별 롤백·보상 처리 통합 테스트

### Task 6-C. Retry·멱등성·운영 검증

상태: ⚪ 대기

- [ ] 재시도 가능한 오류와 재시도하면 안 되는 오류 구분
- [ ] 최대 재시도 횟수와 backoff 정의
- [ ] 신청 생성 API의 멱등성 필요 여부 결정
- [ ] 재시도로 Application 또는 파일이 중복 생성되지 않는지 검증
- [ ] 장애 단계별 로그와 추적 ID 적용
- [ ] 운영 지표 및 알림 대상 정의
- [ ] 동시성·롤백·보상·재시도 운영 테스트
- [ ] Application 관련 테스트 전체 통과
- [ ] 전체 테스트 통과

## Application 정책 동기화 Audit 후속 작업 (2026-08-07)

- 확정 정책의 구현 충돌과 미구현 항목은 `docs/specs/application/checklist.md`의 실제 파일·클래스·메서드 근거를 기준으로 후속 구현한다.
- 미결정 사항은 TODO로 구현하지 않고 `docs/collab/PENDING_DECISIONS.md`에서 관리한다.
- 학생증 `department`(학과) 필드는 이번 동기화에서 제외 — 유지/제외 여부가 결정되면 `checklist.md`/`requirements.md`/`data-model.md`/`api.md`를 다시 동기화해야 한다.

### checklist.md §4 구현 진행

- [x] 학생증 직인(seal) 필수 → 선택 — `ApplicationController.createGroup`(`seal` optional), `ApplicationService.validateStudentFields`/`createIndividualApplication`/`createGroup`(로고만 필수, 직인은 있으면만 검증·업로드). 개인 신청 Controller의 `schoolSeal`은 이미 optional이었음.
- [x] `ApplicationService`를 비트랜잭션 오케스트레이터 + `ApplicationPersistenceService`(`@Transactional`)로 분리 — 신규 `ApplicationPersistenceService`(`saveIndividual`/`saveGroup`)와 `GroupMemberUpload` record 추가. `ApplicationService.createIndividual`/`createGroup`에서 `@Transactional` 제거, Application/Applicant/Receiver/ApplicationMember 저장 로직 전부 이동. 파일 업로드(`storeUploadFile`/`storePhotoFile`/`storePhotoBytes`)는 트랜잭션 밖 `ApplicationService`에 유지.
- [x] `MOBILE`에 `receiver` 전달 시 `INVALID_INPUT` 거절 — `validateReceiverPresence`(개인)/`validateGroupReceiverPresence`(단체) 둘 다에 반대 방향 검증 추가
- [x] `sameAsApplicant=true` 복사 범위를 이름·연락처로 제한(배송지는 항상 Receiver 입력값) — `ApplicationPersistenceService.saveReceiverIfNeeded`/`saveGroupReceiverIfNeeded`에서 `copyFromApplicant`/`copyIndividualReceiver` 분기 제거, 항상 `receiverRequest`의 우편번호·주소·상세주소·배송메모를 그대로 저장. 이름·연락처만 요청값이 비어 있을 때 Applicant 값으로 대체(fallback)
- [x] `Applicant.email`을 요청값으로 받아 저장(현재는 `user.getEmail()` 고정) — `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `ApplicantRequest`에 `email` 필드 추가(검증 없음). `ApplicationService.createIndividual`/`createGroup`에서 요청 email이 있으면 그 값을, 없으면 `User.email`을 저장하도록 변경
- [x] 학번 최대 10자·숫자만 형식 검증 — `ApplicationService.validateStudentFields`(개인 신청 경로)에 `isValidStudentId`(정규식 `\d{1,10}`) 추가. `BulkExcelParser` 쪽(단체 신청 경로)은 별도 항목에서 처리
- [x] 업로드 실패 시 역순 보상 삭제, 파일 수정 시 갱신 성공 후 기존 파일 삭제 — 생성 경로(`createIndividual`/`createGroup`)는 업로드한 storage key를 `List<String>`으로 추적, `applicationPersistenceService.save*` 실패 시 역순으로 `storageService.delete` 호출 후 원래 예외 재던짐. `reuploadPhoto`는 개인/단체 모두 DB 갱신 성공 후 기존 사진·기존 ZIP(UploadFile 조회)을 삭제
- [x] 신청번호 `application_seq.nextval` 기반 DB Sequence로 교체(`count+1` 제거) — `schema.sql`에 `CREATE SEQUENCE IF NOT EXISTS application_seq` 추가(`spring.jpa.defer-datasource-initialization=true`+`spring.sql.init.mode=always`로 Hibernate ddl-auto 이후 실행), `ApplicationService.generateApplicationNumber`가 `EntityManager.createNativeQuery("SELECT nextval('application_seq')")`로 채번. `ApplicationRepository.countByApplicationNumberStartingWith` 제거(§4 "ApplicationRepository count+1 정리" 항목도 함께 해소됨)
- [x] `BulkExcelParser`: Excel 1개·ZIP 루트 제한, 사진 ZIP 루트에서 매칭, 중간·마지막 빈 행 무시 — `parse`가 ZIP 루트(하위 경로 `/` 없는 항목)만 스캔, `.xlsx`는 후보로 모았다가 0개면 `EXCEL_NOT_FOUND`·2개 이상이면 `EXCEL_PARSE_ERROR`, 나머지 루트 파일은 사진으로 매칭(`photos/` 하위 매칭 제거). `.DS_Store` 무시(`__MACOSX/...`는 하위 경로라 루트 필터로 자동 제외). `parseExcel`은 ID 빈 행에서 `break`하던 것을 `continue`로 바꿔 중간·마지막 빈 행 모두 무시하도록 변경
- [x] `BulkExcelParser`: 학번 형식 검증, `BULK_APPLICATION_VALIDATION_FAILED`+`errors[]`로 전체 오류 수집 — `parseRow`가 필드별로 즉시 던지던 것을 `List<ValidationErrorDetail>`에 수집하는 방식으로 전환, 행 하나가 실패해도 나머지 행을 계속 검사. 학번은 `\d{1,10}` 형식 검증 추가. 엑셀 없음/2개 이상/데이터 없음도 동일하게 `BulkValidationException`(row·field·code·message)으로 통일
- [x] `ApplicationCreateRequest`/`BulkApplicationCreateRequest`: Receiver 우편번호·기본주소 필수 검증, studentId 형식 검증 추가 (email 필드는 위 항목에서 이미 추가됨) — `ReceiverRequest.zipCode`/`address`에 `@NotBlank` 추가. studentId 형식은 개인은 `ApplicationService.isValidStudentId`(item6), 단체는 `BulkExcelParser`에서 이미 검증하고 있어 DTO에 중복 추가하지 않음
- [x] `ApplicationMember.student_id` 컬럼 길이 50→10 — `@Column(length = 50)` → `@Column(length = 10)`. Service/BulkExcelParser에서 이미 10자·숫자만 통과시키므로 이 값을 넘는 값이 저장될 경로는 없음(스키마 정합성 정리)
- [x] `ApplicationRepository`의 count+1용 조회 메서드 정리(Sequence 전환과 함께) — 위 Sequence 전환 항목에서 `countByApplicationNumberStartingWith` 함께 제거됨
- [x] `ErrorCode`에 `BULK_APPLICATION_VALIDATION_FAILED` 추가, 미사용 `ZIP_TOO_LARGE`/`EXCEL_NOT_FOUND`/`EXCEL_PARSE_ERROR` 정리 — `APPLICATION_LIMIT_EXCEEDED`는 별도 §5 "일일 3회 제한" 항목(미착수) 몫이라 이번엔 추가하지 않음
- [x] `ApiResponse`/`GlobalExceptionHandler`에 단체 오류용 `errors[]` 필드 추가 — `ApiResponse`에 `List<ValidationErrorDetail> errors` 필드(`@JsonInclude(NON_NULL)`로 평소엔 응답에서 생략) + `fail(errorCode, message, errors)` 오버로드 추가. `GlobalExceptionHandler`에 `BulkValidationException` 전용 핸들러 추가(일반 `CustomException` 핸들러보다 먼저 매칭)
- [x] `ApplicationServiceBulkTest`의 단일 `EXCEL_PARSE_ERROR` 고정 테스트를 `errors[]` 계약에 맞게 갱신 — 4개 테스트를 `BULK_APPLICATION_VALIDATION_FAILED` 기대값으로 갱신, `ApplicationBulkControllerTest`도 `errors[0].code` 검증 추가

### checklist.md §5 구현 진행

- [x] `ApplicationPersistenceService` 신규 — §4 "ApplicationPersistenceService 분리" 항목에서 이미 구현됨(2026-08-07)
- [x] `BULK_APPLICATION_VALIDATION_FAILED` + `errors[]` 응답 구조 — §4 "BulkExcelParser 학번 검증·errors[] 계약" 항목에서 이미 구현됨(2026-08-07)
- [x] 일일 KST 3회 제한 DB 원자 처리 (2026-08-16 구현 완료) — §4/§5 통틀어 유일하게 남았던 항목. 신규 `ApplicationDailyLimit` 엔티티(사용자별·일자별 카운터, `UNIQUE(user_id, count_date)`) + `ApplicationDailyLimitService.reserveSlot/releaseSlot`. `reserveSlot`은 `findByUserIdAndCountDateForUpdate`(비관적 락 `PESSIMISTIC_WRITE`)로 기존 row를 잠그고 증가시키거나, 오늘 첫 신청이면 `saveAndFlush`로 INSERT 시도 — 두 요청이 동시에 "오늘 첫 신청"이면 `UNIQUE` 제약 충돌(`DataIntegrityViolationException`)이 나는데, 이건 `ApplicationService`가 새 트랜잭션으로 한 번 재시도해서 해소(같은 트랜잭션 안에서 재시도하면 이미 실패로 표시된 트랜잭션을 계속 쓰게 돼 불안정). `ApplicationService.createIndividual`/`createGroup`에 파일 업로드 이전(모든 검증 이후) 지점에서 호출, 실패(파일 업로드·DB 저장 실패) 시 `releaseSlot`으로 자리 반환. 신규 테스트 19개(`ApplicationDailyLimitTest` 5개, `ApplicationDailyLimitServiceTest` 9개 — 동시성 시나리오 2개 포함, `ApplicationServiceDailyLimitTest` 3개, `ApplicationServiceUploadCompensationTest`에 2개 추가) 전부 통과. 전체 스위트 316개 중 기존과 동일하게 Redis 미기동 3건만 실패(회귀 없음)
- [x] `application_seq.nextval` 채번 — §4 "신청번호 DB Sequence 전환" 항목에서 이미 구현됨(2026-08-07)
- [x] 업로드 추적 및 DB 실패 보상 삭제 — §4 "업로드 보상 삭제" 항목에서 이미 구현됨(2026-08-07, `uploadedKeys` 추적 + `storageService.delete` 역순 호출)
