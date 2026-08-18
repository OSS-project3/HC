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
| ✅ | 단체 신청 Excel 사진 번호 고정 및 파서 정합성 | Codex | `main` | `docs/specs/application/{APPLICATION,requirements,api,service-flow}.md`, `docs/collab/BULK_EXCEL_TEMPLATE_POLICY.md` | v1.1 양식 3종의 A열을 사진 번호 001~100 텍스트로 사전 입력·잠금·색상·메모 처리. 파서는 사진 번호만 있는 행을 무시하고 실제 입력 행 사진만 매칭. 집중 테스트 19개 통과 |
| ✅ | User CRUD (조회/수정/탈퇴/자동복구/익명화 스케줄러) 구현+테스트 | Claude | `backend-api` (병합됨) | `backend/honor-citizen/docs/test/user-test-result.md` | - |
| ✅ | API-명세.md → `docs/api/*.md` 도메인별 분리 | Codex | `feature/application-domain-docs` | `docs/api/README.md` | 원본과 대조하여 내용 유실 없음 확인 완료 |
| ✅ | Application 문서 도메인 패키지 이전 | Codex | `feature/application-domain-docs` | `docs/specs/application/` | requirements/data-model/api/checklist 구성 및 구 경로 참조 수정 완료 |
| ✅ | `arch.md` 구조를 실제 코드 규모에 맞게 단순화 | Claude | `feature/application-domain-docs` | `arch.md` | 비즈니스 규칙 절은 유지, 계층/패키지 구조만 축소 |
| ✅ | 협업 규칙 체계(`docs/collab/`) 도입 | Claude | `backend-api` | `docs/collab/RULES.md` | - |
| ✅ | Application 도메인 엔티티/API 구현 | Claude | `feature/application-domain-impl` | `docs/specs/application/*.md` | API 1~5 전부 완료, 신규 테스트 46개 전부 통과. checklist.md 6개 섹션 검증 완료(결과는 HANDOFF.md 참고) |
| ⚪ | Codex: HANDOFF.md "확인 필요" 3건을 `docs/specs/application/*`에 반영 | Codex | `feature/application-domain-docs` | `docs/specs/application/{requirements,data-model,api}.md` | englishName 추가, total_price 보류 각주, 엑셀 부분실패=전체거부 확정 — 사람 확인 끝난 결정사항, 문서 반영만 필요 |
| ✅ | 조회(`lookup`) 인증 정책 method별 분리 구현 | Claude | `feature/application-domain-impl` | `docs/specs/application/api.md` | `application`=phone+email 둘 다 필수, `card`=인증값 없음. 상세는 CHANGELOG 2026-08-06 참고 |
| ✅ | `CardTypeSeeder` 추가 — CardType ID 1~4 고정 시딩 | Claude | `feature/application-domain-impl` | `docs/BACKEND_API_GAPS.md`(참고: 카드 종류 ID) | 프론트 `cardTypeId` 하드코딩(1~4)을 그대로 쓰기로 결정, `GET /api/card-types` 신규 API는 만들지 않음 |
| ⚪ | 단체 재제출 UI(`MobileCardPage.tsx`) 추가 | 프론트 담당자 | `main` | `docs/BACKEND_API_GAPS.md` P0-2 | 백엔드는 이미 구현됨(`PATCH .../photo`의 `submitFile` 파트, `PHOTO_REJECTED` 상태에서만 허용). 프론트에 단체용 업로드 UI만 없음 |
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
| ✅ | 마이페이지 신청 목록/상세 조회 API 6·7 구현 (2026-08-18) | Claude | `main` | `docs/specs/application/api.md` API 6/7 | `GET /api/my/applications`(목록, `status` 선택 필터·`createdAt DESC` 고정 정렬)·`GET /api/my/applications/{id}`(상세) 구현+테스트 완료. `ApplicationRepository.findByUserId`/`findByUserIdAndStatus`, `ApplicationMemberRepository.countByApplicationId` 신규. 신규 DTO `MyApplicationListItemResponse`/`MyApplicationDetailResponse`(중첩 `ApplicantSummary`/`ReceiverSummary`), 기존 `PageResponse<T>`(Review에서 처음 도입) 재사용. `receiver`는 `issueType=MOBILE_AND_PHYSICAL`일 때만 채워지고 그 외엔 `null`. `MyApplicationController` 신규(`SecurityConfig` 변경 없음 — 기존 `/api/**` → `hasAnyRole("USER","ADMIN")` 규칙에 자동 편입). 신규 테스트 13개(`ApplicationServiceMyApplicationsTest` 7개, `MyApplicationControllerTest` 6개) 전부 통과. 전체 스위트 361개 중 기존과 동일하게 UserControllerTest 2건+Redis 미기동 1건만 실패(회귀 없음). ⚠️ **커밋 보류**: 응답 DTO(`MyApplicationDetailResponse`)가 `Application.paymentGuidedAt/cancelledAt/cancellationType/cancellationReason/refundedAt/cardReadyAt/physicalDispatchedAt`(Codex의 미커밋 상태 리팩터링 신규 필드) 게터를 그대로 쓰고 있어, 지금 HEAD엔 없는 필드라 이 작업만 단독 커밋하면 컴파일이 깨짐 — Codex의 "신청 상태 리팩터링" 커밋이 먼저 들어간 뒤에 이 작업을 커밋한다 |
| ✅ | 신청 상태·취소·환불 정책 문서 정합성 반영 | Codex | `main` | `docs/specs/application/APPLICATION.md` §16, `docs/collab/TODO.md` 상세 체크리스트 | requirements/data-model/api/service-flow/admin/payment/PENDING 문서를 확정 정책으로 동기화. 초기 `SUBMITTED+WAITING`, 최초 결제 안내+72시간, 10분 설정형 자동취소, 취소 commit 직후 S3 삭제 반영 |
| ⚪ | 신청 상태 리팩터링 및 사용자 취소 API 구현 | 미정 | `main` | `docs/specs/application/APPLICATION.md` §16, `docs/collab/TODO.md` 상세 체크리스트 | `PAYMENT_PENDING/RECEIVED` 제거, `SUBMITTED/PRODUCTION_READY` 추가, 결제 확인과 상태 전이 분리, 취소 이력·3일 자동 취소·일일 슬롯 반환·최소 환불 모델·다운로드/실물 발송 기준을 체크리스트 순서대로 구현하고 통합 테스트 |
| ✅ | 학생증 신청 항목 추가(학교구분·가로형/세로형, 2026-08-14) | Claude | `main` | `docs/specs/application/{data-model,api}.md`, 계획: `C:\Users\gpdnj\.claude\plans\application-api-async-knuth.md` | 구현+테스트 완료. `Application`에 `orientation`(LANDSCAPE/PORTRAIT)·`school_type`(UNIVERSITY/HIGH_SCHOOL) 컬럼 신규 추가(개인·단체 공통, 학생증 전용, 신청서 전체에 1개). `ApplicationMember.student_id`/`department` 필수 조건을 "학생증이면 무조건"→"학생증+`school_type=UNIVERSITY`일 때만"으로 변경(HIGH_SCHOOL이면 있으면 오히려 거절). 단체는 학번·학과를 여전히 엑셀로만 받음(`BulkExcelParser` 변경 없음), orientation/schoolType만 신청 폼 필드로 추가. 카드종류별 config 추상화 없이 기존 `isStudent` boolean 게이트 재사용. `Application.createIndividual`/`createGroup` 팩토리 메서드는 기존 시그니처를 하위호환 오버로드로 유지해 무관한 기존 테스트 ~20개는 손대지 않음. 신규 ErrorCode 없음(`INVALID_INPUT` 재사용). 신규 테스트 12개(`ApplicationServiceTest` 7개, `ApplicationServiceBulkTest` 5개) 전부 통과, 기존 테스트 3개(`ApplicationServiceUploadCompensationTest`) 픽스처 보정 후 통과. 전체 스위트 224개 중 기존과 동일하게 Redis 미기동 3건만 실패(회귀 없음). 프론트(`StepInfo.tsx` 등)는 프론트 담당자 영역이라 미착수 |
| ✅ | Board 도메인(공지사항/FAQ) 구현 (2026-08-14) | Claude | `main` | `docs/specs/board/{data-model,api}.md`, `arch.md` §4.8 | CRUD 5개 API(목록/단건/생성/수정/삭제) 전부 구현+테스트 완료. `Board`+`BoardType{NOTICE,FAQ}` enum 통합 관리, `BoardAttachment` join 엔티티(`Board:UploadFile`=1:N, NOTICE 전용). 신규 `BoardAttachmentValidator`(최대 10개, 1개당 10MB, 문서+이미지 확장자/MIME 허용목록, 이미지만 시그니처 검증). `BoardService`/`BoardController`(공개 GET, `/api/boards`)/`BoardAdminController`(관리자 CRUD, `/api/admin/boards`) 신규. `SecurityConfig`에 `arch.md` §4.6이 이미 명시했으나 코드로는 없었던 `/api/admin/**` → `hasRole("ADMIN")` 신규 추가(이 프로젝트 첫 관리자 전용 쓰기 API), `GET /api/boards`·`GET /api/boards/**` `permitAll()` 추가. 서비스 레벨 권한 분기 없음 — 라우트 레벨 강제로 충분(리소스 소유권 판단이 필요없는 "관리자냐 아니냐"뿐이라 Review의 `canEdit`/`canDelete`와 다름). `ErrorCode.BOARD_NOT_FOUND` 신규. FAQ+첨부파일 요청은 `INVALID_INPUT`으로 거절(2026-08-14 사용자 확인). API 4(수정)는 첨부파일 편집을 이번 패스 범위 밖으로 명시적으로 미루고 `application/json`으로 단순화(당초 `multipart/form-data` 초안에 실체 없는 `attachments` 파트가 남아있던 문서 불일치를 구현 중 발견해 함께 정리). 신규 테스트 34개(`BoardTest` 2개, `BoardAttachmentTest` 1개, `BoardAttachmentValidatorTest` 6개, `BoardServiceTest` 12개, `BoardControllerTest` 5개, `BoardAdminControllerTest` 8개) 전부 통과. 전체 스위트 258개 중 기존과 동일하게 Redis 미기동 관련 3건만 실패(회귀 없음). NOTICE 첨부파일 교체/추가/삭제 흐름과 프론트 업로드 UI는 다음 패스로 이월 |
| ⚪ | Inquiry(1:1 문의) 도메인 신규 구현 | 미정 | `main` | `docs/FRONTEND_API_GAPS.md` §1.1, `docs/FRONTEND_USER_FLOW_AUDIT.md`(Codex, 2026-08-14) "공지·FAQ·문의·행사" | 백엔드에 도메인 자체가 없음(`Inquiry` 엔티티/Controller 전무) — Codex 감사에서도 동일 지적, `InquiryPage`/`InquiryDetailPage`/`MyPage`/`AdminPage`가 전부 `localStorage["customer-inquiries"]` 사용 확인. 데이터 계약(`InquiryRecord`): `category, name, email, phone, title, content, status(PENDING/COMPLETED), answer, answeredAt`. 착수 전 결정 필요: 비회원 문의 등록·조회 허용 여부(Codex 감사도 동일하게 미확정으로 지적) |
| ✅ | Event(행사) 도메인 구현 (2026-08-16) | Claude | `main` | `docs/specs/events/{data-model,api}.md` | CRUD 5개 API(목록/단건/생성/수정/삭제) 전부 구현+테스트 완료. `EventPost`(`EventType{BOOTH,COLLABORATION}`, `visible`/`display_order` 포함)+`EventImage`(상세 갤러리, `UploadFile` 미경유·S3 key 직접 저장 — Review `image_path`와 동일 패턴, Board `BoardAttachment`의 UploadFile join과는 다름). `EventController`(공개 GET, `/api/events`)/`EventAdminController`(관리자 CRUD, `/api/admin/events`) 신규 — `/api/admin/events/**`는 Board 때 추가한 `/api/admin/**` → `hasRole("ADMIN")` 규칙에 코드 변경 없이 자동 편입, `SecurityConfig`엔 공개 GET `permitAll()`만 추가. 서비스 로직: 생성(썸네일+갤러리 S3 업로드 후 DB 트랜잭션, `uploadedKeys` 역순 보상삭제 — Board `create()`와 동일 골격) / 목록(`EventPostRepository.findVisibleByEventType`가 JPQL `ORDER BY display_order ASC NULLS LAST, event_date DESC NULLS LAST, created_at DESC` 고정 정렬 전담, `visible=true`만) / 상세(`visible=false`면 `EVENT_NOT_FOUND`로 존재 자체를 숨김, `next` 없음 — 프론트에 상세 페이지 라우트 자체가 없어 이전/다음 이동 UI가 없음) / 수정(텍스트 필드+`visible`+`displayOrder` 전체 재제출, 썸네일은 새 파일 있을 때만 교체(Review `applyImageChange`와 동일 패턴), 갤러리 편집은 이번 패스 제외) / 삭제(`EventImage`+`EventPost` 한 트랜잭션 삭제 후 커밋 이후 S3 정리, 썸네일도 함께). 신규 `EventImageValidator`(`ReviewImageValidator`와 규칙 동일: 2MB, jpg/jpeg/png/webp — package-private라 재사용 불가 + 도메인별 검증기 독립이 기존 관례라 신규 제작), 신규 ErrorCode `EVENT_NOT_FOUND`. 설계 단계에서 확정한 2가지: (1) `EventImage.representative` 플래그 제거 — `EventPost.thumbnail_image_path`가 대표 이미지 유일 소스 (2) 관리자 전용 전체 목록 API(`GET /api/admin/events`, `visible` 무관)는 이번 패스 제외, 이후 별도 구현. 신규 테스트 39개(`EventPostTest` 3개, `EventImageTest` 1개, `EventImageValidatorTest` 7개, `EventServiceTest` 14개, `EventControllerTest` 6개, `EventAdminControllerTest` 8개) 전부 통과. 전체 스위트 297개 중 기존과 동일하게 Redis 미기동 관련 3건만 실패(회귀 없음) |
| ⚪ | 관리자(Admin) 신청 목록·상태변경·통계 API | 미정 | `main` | `docs/FRONTEND_API_GAPS.md` §1.2, `docs/FRONTEND_USER_FLOW_AUDIT.md` "관리자 의존성" | `GET /api/admin/applications`(목록, 상태·유형 필터), `GET /api/admin/applications/{id}`, 상태별 명령 API, `GET /api/admin/stats`가 아직 없음. 백엔드 확정 상태는 `SUBMITTED→REVIEWING↔PHOTO_REJECTED→NAME_EDITING→PRODUCTION_READY→PRODUCING→COMPLETED/CANCELLED`이며 PaymentStatus는 별도 관리 |
| ⚪ | "내 후기" 목록 조회 API | 미정 | `main` | `docs/FRONTEND_USER_FLOW_AUDIT.md` "후기" | `ReviewController`/`ReviewService.list()`에 로그인 사용자 범위로 좁히는 파라미터가 없음(`cardTypeId`/`hasPhoto`/`searchType`/`keyword`/`page`/`size`뿐, 작성자 필터 없음) — 코드로 확인 완료. 마이페이지 "내 후기" 목록에 필요. `GET /api/reviews/me` 신설 또는 기존 목록에 `mine=true`(로그인 필요) 파라미터 추가 중 택1 |
| ⚪ | 단체신청 Excel 양식 다운로드 API 필요 여부 확정 | 미정 | `main` | `docs/FRONTEND_USER_FLOW_AUDIT.md` "신청" | 백엔드에 template 관련 엔드포인트가 확인되지 않음(코드 검색 결과 없음) — 프론트에도 양식 다운로드 버튼 자체가 없음. 신규 API가 필요한 건지, 정적 파일 제공으로 충분한지 정책 확인부터 필요 |

> 아래 "Task 1~4" 4행 요약은 이 로드맵이 Task 1~6(5-A/5-B 포함)으로 세분화되기 전의 옛 버전이라 삭제함 — 최신 진행 상태는 바로 아래 "Application 개인 신청 리팩터링 로드맵" 절 참고.

---

## 백엔드 API 연동 체크리스트 (2026-08-19 감사 기준)

> 근거: `docs/FRONTEND_API_INTEGRATION_SPEC.md`/`backend/FRONTEND_API_REQUIREMENTS.md`/`docs/FRONTEND_API_GAPS.md`/`docs/BACKEND_API_GAPS.md` 4개 문서를 실제 백엔드 코드와 전수 대조한 감사(`C:\Users\gpdnj\.claude\plans\application-api-async-knuth.md`) 결과를 작업 단위로 분해한 것. **관리자 신청관리 페이지는 이번 범위 밖**이라 관련 항목은 전부 제외했고(아래 "현재 범위에서 제외된 관리자 기능" 참고), 정책이 확정 안 된 항목은 구현에 넣지 않고 "정책 결정이 필요한 항목"으로 뺐다.

### 작업그룹 A — 일반 이메일 회원가입·로그인·계정 복구

> 화면 근거: `pages/LoginPage`·`SignupPage`·`AccountRecoveryPage`가 이미 존재하고 현재 mock/데모 로그인으로만 동작 중(`FRONTEND_API_GAPS.md` §1.1). 핵심 정책(로그인 식별자=이메일, 정규화 규칙, DB UNIQUE, 서버가 role 결정, 소프트탈퇴 7일 자동복구)은 `backend/FRONTEND_API_REQUIREMENTS.md` §3에 이미 확정돼 있어 구현 대상에 포함한다. 단, 비밀번호 재설정/아이디찾기(이메일 발송이 필요한 두 개)는 이메일 발송 인프라 자체가 전무해(`spring-boot-starter-mail`/`JavaMailSender` 없음, `EmailLog`도 실사용처 없는 미사용 엔티티) 별도 인프라 결정이 선행돼야 하므로 이번 체크리스트에서 제외하고 "정책 결정이 필요한 항목"으로 뺐다.

> 설계 확정(2026-08-19): OAuth 컬럼(`oauthId`/`oauthProvider`)은 sentinel 값이 아니라 **nullable로 전환**(A안 — 일반 이메일 계정은 도메인상 OAuth 정보가 없는 게 정확한 상태). `password` 필드명은 `passwordHash`로 확정(평문이 저장되지 않는다는 의미를 코드에 드러냄), 길이는 해시 알고리즘 교체 여지를 감안해 255자. 계정 생성 경로는 `createNewUser()`(범용) 대신 `createLocalUser(...)`/`createOAuthUser(...)`로 분리. 상세 근거·SQL·단계별 순서는 이 TODO 항목들과 `docs/api/user.md`(구현 시 반영)를 기준으로 한다.

> **정책 확정(2026-08-19, 2차)**: 아래 3가지를 최종 확정해 AUTH-4/5의 세부 작업으로 반영한다.
> - **로컬 회원가입 이메일 인증**: 운영에서는 인증 필수(인증 완료 후에만 계정 생성 — OAuth 이메일은 공급자가 이미 검증했으므로 인증 완료로 간주하고 기존 흐름 유지). 근거: 인증 없이 가입을 허용하면 타인의 이메일을 먼저 등록해 실제 소유자의 가입·비밀번호 재설정·안내 메일 수신을 막을 수 있고, `email` UNIQUE 정책 때문에 이메일 선점 문제로 직결된다. `UserStatus`나 인증 토큰 테이블은 추가하지 않고, 계정 생성 **전에** Redis 기반 코드 검증으로 처리한다(미인증 User가 DB에 남지 않음) — 아래 SIGNUP-1/2 참고.
> - **비밀번호 인코더**: `PasswordEncoderFactories.createDelegatingPasswordEncoder()`(Spring Security 기본, 저장 형식 `{bcrypt}$2a$10$...`) — 알고리즘 교체 여지를 남기면서 지금은 BCrypt strength 10(기본값) 사용. 아래 PW-1 참고.
> - **로그인 실패 제한**: 정규화 이메일 기준 15분 내 5회 실패 → 15분 잠금, Redis에만 저장(DB/`User` 엔티티 변경 없음). 계정 기준 제한만 우선 구현하고 IP 기준 제한은 운영 중 공격이 관찰되면 후속 검토. 아래 RATE-1 참고.

- [x] **AUTH-1** User 계정 모델 기반(스키마+정규화+UNIQUE) — ✅ 구현+테스트 완료(Claude, 2026-08-19, 미커밋)
  - 변경 내용: `User`에 `passwordHash`(`@Column(length = 255)`, nullable) 추가. `oauthId`/`oauthProvider`를 `nullable = false` → nullable로 전환. `email`에 `unique = true` 추가. `User.normalizeEmail(String)`(trim+소문자) 신설, `createOAuthUser`(구 `createNewUser` 개명, 24개 호출부 전부 갱신)와 `createLocalUser`(신규) 둘 다 저장 시 이 유틸을 거치도록 구현. `anonymize()`에 `passwordHash = null` 추가.
  - 대상 파일: `domain/user/entity/User.java`, `infra/security/OAuth2SuccessHandler.java`(호출부 개명), 테스트 24개 파일(호출부 개명), `UserTest.java`(신규 케이스 5개)
  - 완료됨: `passwordHash`/nullable OAuth 컬럼/이메일 UNIQUE 전부 적용, `createLocalUser`(passwordHash 필수·oauth null)/`createOAuthUser`(oauth 필수·passwordHash null) 불변조건 보장, 정규화 유틸 존재. 신규 테스트 5개(정규화 trim/null, 두 팩토리 불변조건, anonymize 시 passwordHash null화) 전부 통과. 전체 스위트 386개(381+5) 중 기존과 동일하게 `UserControllerTest` 2건+Redis 미기동 1건만 실패(회귀 없음).
  - ⚠️ **미완료로 남긴 부분**: 운영 DB 기존 데이터의 이메일 중복 점검(`SELECT LOWER(TRIM(email)), COUNT(*) ... HAVING COUNT(*) > 1`)은 이 프로젝트에 마이그레이션 도구(Flyway/Liquibase)나 운영 DB 접근이 없어 실행하지 못함 — 운영 배포 담당자가 배포 전 별도로 확인 필요.
  - 우선순위: P0(이 그룹 나머지 전부의 선행) — 완료

- [x] **AUTH-2** 기존 OAuth 로그인 안전화 — ✅ 구현+테스트 완료(Claude, 2026-08-19, 미커밋)
  - 변경 내용: `OAuth2SuccessHandler`가 `(oauthId, provider)`로 기존 계정을 찾고, 없으면 `UserService.createOAuthUserIfAbsent(...)`를 호출하도록 재작성. 이 신규 메서드는 normalized email로 충돌(다른 provider·일반 계정) 여부를 먼저 확인해 있으면 `CustomException(EMAIL_ALREADY_EXISTS)`를 던지고, 동시요청으로 인한 DB UNIQUE 위반도 동일 예외로 변환한다. `OAuth2SuccessHandler`는 이 예외를 잡아 `frontendUrl + "/login?error=oauth"`로 리다이렉트한다.
  - **트랜잭션 격리 설계**: `onAuthenticationSuccess`가 이미 `@Transactional`이라, `createOAuthUserIfAbsent`를 그냥 REQUIRED로 두면 이메일 충돌 실패가 호출자의 트랜잭션 전체를 rollback-only로 오염시킨다. `Propagation.REQUIRES_NEW`로 분리해 독립시켰다. 단, REQUIRES_NEW는 별도 트랜잭션이라 반환된 엔티티가 detached 상태이므로, 이후 `restore()`/`updateRefreshToken()` 변경이 유실되지 않도록 호출자에서 `userRepository.findById(created.getId())`로 다시 조회해 managed 상태로 만든 뒤 사용한다.
  - 신규 `ErrorCode.EMAIL_ALREADY_EXISTS(409)` 추가(AUTH-3/4에서도 재사용 예정).
  - 대상 파일: `infra/security/OAuth2SuccessHandler.java`, `domain/user/service/UserService.java`(`createOAuthUserIfAbsent` 신규), `common/exception/ErrorCode.java`
  - 완료됨: 같은 이메일의 타 provider/일반 계정 존재 시 거절 확인, 정규화(trim+대소문자) 무관하게 충돌 감지 확인. 신규 테스트 4개(`UserServiceOAuthTest` — 신규 생성 성공, 타 provider 충돌, 일반계정 충돌, 대소문자/공백 무관 충돌 감지) 전부 통과. 전체 스위트 390개(386+4) 중 기존과 동일하게 `UserControllerTest` 2건+Redis 미기동 1건만 실패(회귀 없음).
  - ⚠️ **테스트 설계 메모**: 이 테스트들은 `UserServiceTest`(클래스 레벨 `@Transactional`, 테스트 종료 시 자동 롤백)가 아니라 별도 클래스 `UserServiceOAuthTest`(비-트랜잭셔널, `@BeforeEach deleteAll()`로 수동 정리)에 뒀다 — `createOAuthUserIfAbsent`가 REQUIRES_NEW(별도 커넥션)라서, `@Transactional` 테스트 안에서 `saveAndFlush`로 만든 선행 데이터는 커밋되지 않아 별도 커넥션에서 안 보이고, 그 결과 충돌 감지 자체가 성립하지 않는 문제를 실제로 겪고 나서 분리했다.
  - 우선순위: P0(AUTH-1 직후, 이메일 관련 API보다 먼저) — 완료

- [ ] **AUTH-3** 이메일 중복 확인 API (`POST /api/auth/email/check`)
  - 변경 내용: 정규화된 이메일로 기존 계정(OAuth 계정 포함) 존재 여부만 반환, 계정 상세는 노출하지 않음.
  - 대상 파일: `api/AuthController.java`, `domain/user/service/UserService.java`, `domain/user/repository/UserRepository.java`(정규화 이메일 조회 메서드)
  - 선행 작업: AUTH-1, AUTH-2
  - 완료 조건: 중복/미중복 각각 정상 응답, OAuth로 이미 가입된 이메일도 중복으로 판정
  - 검증할 테스트: 신규 컨트롤러 테스트(중복/미중복/OAuth 계정과 충돌)
  - 우선순위: P1

- [x] **PW-1** PasswordEncoder Bean 등록 — ✅ 구현+테스트 완료(Claude, 2026-08-19, 미커밋)
  - 변경 내용: `PasswordEncoderFactories.createDelegatingPasswordEncoder()`를 `PasswordEncoder` Bean으로 등록(저장 형식 `{bcrypt}$2a$10$...`).
  - 대상 파일: `infra/security/SecurityConfig.java`, 신규 `PasswordEncoderConfigTest.java`
  - 완료됨: Bean 등록 확인, 인코딩 결과가 `{bcrypt}` 접두사로 시작함을 확인, `matches()` 정상/불일치 케이스 확인. 신규 테스트 2개 전부 통과. 전체 스위트 392개(390+2) 중 기존과 동일하게 `UserControllerTest` 2건+Redis 미기동 1건만 실패(회귀 없음).
  - 우선순위: P0(AUTH-4·AUTH-6의 선행) — 완료

- [x] **MAIL-1** 이메일 발송 인프라 구축 — ✅ 구현+테스트 완료(Claude, 2026-08-19, 미커밋)
  - 변경 내용: `spring-boot-starter-mail` 의존성 추가. `spring.mail.*` 설정 — `host`/`port`/`app.mail.from`은 로컬 기본값(`localhost`/`587`/`no-reply@honor-citizen.local`)을 둬서 설정 없이도 기동되게 하고, `username`/`password`는 AWS/OAuth 시크릿과 동일하게 기본값 없이 env var로만 받음(`MAIL_USERNAME`/`MAIL_PASSWORD`, 하드코딩 없음). SMTP connection/read/write timeout 각 5초. `EmailSender` 인터페이스(`send(to, emailType, subject, htmlBody, textBody)`) + `SmtpEmailSender` 구현체(`JavaMailSender`+`MimeMessageHelper` 사용, text/html 동시 제공, 첨부 없음). `EmailType{SIGNUP_VERIFICATION, PASSWORD_RESET, PASSWORD_CHANGED}` enum 신규(이번 구현은 `SIGNUP_VERIFICATION`만 사용). 신규 `ErrorCode.EMAIL_DELIVERY_FAILED(503)`. 발송 실패는 이 예외로 변환, 자동 재시도 없음. 로그에는 이메일 전문·전체 주소를 남기지 않고 `앞1글자***@도메인` 형태로 마스킹.
  - 대상 파일: `build.gradle`(의존성+테스트 env var), `application.properties`, `common/enums/EmailType.java`(신규), `common/exception/ErrorCode.java`, `infra/mail/EmailSender.java`(신규), `infra/mail/SmtpEmailSender.java`(신규), `SmtpEmailSenderTest.java`(신규)
  - ⚠️ **중요 발견**: `build.gradle`의 `test` 태스크가 `${VAR}`(기본값 없는) 프로퍼티마다 더미 env var를 명시적으로 주입하고 있었다(주석: "없으면 컨텍스트 로딩 자체가 실패함") — `MAIL_USERNAME`/`MAIL_PASSWORD`도 이 패턴을 따라 `test` 태스크에 추가하지 않았다면 전체 스위트가 컨텍스트 로딩 실패로 전부 깨졌을 것. 발견 후 반영함.
  - ⚠️ **버그 1건 발견·수정**: 처음 구현 시 `MimeMessageHelper(message, false, ...)`(비-멀티파트)로 만들어서 `setText(text, html)` 호출이 `IllegalStateException`을 던짐 — `multipart=true`로 수정.
  - 완료됨: 메일 설정 없이도(로컬 기본값) 애플리케이션 기동 확인, 발송 성공 시 `JavaMailSender.send()` 호출+제목/수신자 헤더 확인, 발송 실패 시 `EMAIL_DELIVERY_FAILED`로 변환 확인. 신규 테스트 2개 전부 통과. 전체 스위트 394개(392+2) 중 기존과 동일하게 `UserControllerTest` 2건+Redis 미기동 1건만 실패(회귀 없음).
  - 우선순위: P0(SIGNUP-1의 선행) — 완료

- [ ] **SIGNUP-1** 이메일 인증 코드 요청 API (`POST /api/auth/signup/email-verification`)
  - 변경 내용: 이메일 정규화 → 이미 가입된 이메일이면 `EMAIL_ALREADY_EXISTS`(AUTH-3과 동일 사전 방어) → 6자리 랜덤 코드 생성 → Redis `auth:signup:code:{normalizedEmail}`에 TTL 10분으로 저장 → `EmailSender`로 발송. 코드 자체는 응답에 포함하지 않음.
  - 대상 파일: `api/AuthController.java`, 신규 `domain/user/service/EmailVerificationService.java`(또는 `UserService` 확장)
  - 선행 작업: AUTH-1, MAIL-1
  - 완료 조건: 정상 요청 시 Redis에 코드 저장+발송 호출 확인, 이미 가입된 이메일은 거절
  - 검증할 테스트: 신규 테스트(정상 요청, 중복 이메일 거절) — `EmailSender`는 Mock
  - 우선순위: P0

- [ ] **SIGNUP-2** 이메일 인증 코드 확인 API (`POST /api/auth/signup/email-verification/confirm`)
  - 변경 내용: 이메일+코드 검증 → 일치하면 Redis 코드 삭제 후 단기 가입 토큰(UUID) 발급, `auth:signup:token:{token}` → normalizedEmail로 TTL 30분 저장 → 응답에 토큰 반환. 불일치·만료·이미 사용된 코드는 동일한 오류로 응답(코드 존재 여부 노출 안 함).
  - 대상 파일: `EmailVerificationService.java`에 메서드 추가, `api/AuthController.java`
  - 선행 작업: SIGNUP-1
  - 완료 조건: 정상 확인 시 토큰 발급+코드 재사용 불가, 코드 불일치/만료 거절
  - 검증할 테스트: 신규 테스트(정상확인/코드불일치/만료/재사용 거절)
  - 우선순위: P0

- [ ] **AUTH-4** 이메일 회원가입 API (`POST /api/auth/signup`)
  - 변경 내용: 요청에 포함된 가입 토큰으로 Redis에서 정규화 이메일 조회(없거나 만료면 "인증이 필요합니다" 거절) → `findByEmail(normalized)` 사전 중복확인(동시요청 사이 다른 경로로 먼저 가입됐을 가능성 대비) → `PasswordEncoder`로 해시 → `User.createLocalUser(email, passwordHash, name)` → 저장(DB `UNIQUE` 위반 시 동시요청도 `EMAIL_ALREADY_EXISTS`로 변환) → 가입 토큰 삭제(1회성) → 기존 OAuth와 동일한 HttpOnly access/refresh 쿠키 발급 → `/terms`로 이동(약관 동의는 이 API에 포함하지 않고 기존 `POST /api/auth/terms` 재사용).
  - 대상 파일: `api/AuthController.java`, `domain/user/service/UserService.java`, `domain/user/entity/User.java`(`createLocalUser`), 신규 `SignupRequest` DTO
  - 선행 작업: AUTH-1, AUTH-2, PW-1, SIGNUP-2 (AUTH-3은 병렬 가능 — DB UNIQUE가 최종 방어선이라 강한 의존 아님)
  - 완료 조건: 정상 가입 시 쿠키 발급+DB 저장 확인, 가입 토큰 없이/만료된 토큰으로는 거절, 중복 이메일 거절(사전조회·동시요청 UNIQUE 충돌 둘 다), 비밀번호 평문 미저장 확인
  - 검증할 테스트: 신규 `AuthControllerTest`(정상가입/토큰없음거절/중복거절/동시요청 충돌)
  - 우선순위: P0

- [ ] **RATE-1** 로그인 실패 횟수 제한(Redis)
  - 변경 내용: 정규화 이메일 기준(원문 이메일 대신 SHA-256 해시로 키 구성) 15분 내 5회 실패 시 15분 잠금. `auth:login:fail:{sha256}`(카운터, INCR+TTL 원자 처리)과 `auth:login:lock:{sha256}`(5회째 실패 시 별도 생성, TTL 15분) 두 키 사용. 로그인 성공 시 두 키 모두 삭제. 존재하지 않는 이메일로 시도해도 동일하게 카운트 증가(이메일 존재 여부가 잠금 발생 여부로 새어나가지 않도록). 비밀번호 재설정(향후 계정복구 구현 시)은 잠금 중에도 허용.
  - 대상 파일: 신규 `infra/security/LoginAttemptLimiter.java`
  - 선행 작업: 없음(독립 구현 가능, AUTH-5에서 사용)
  - 완료 조건: 5회 미만 실패는 통과, 5회째부터 잠금, 잠금 중엔 올바른 비밀번호로도 거절, 성공 시 카운터·잠금 모두 리셋, 원문 이메일이 Redis 키/로그에 남지 않음
  - 검증할 테스트: 신규 테스트(5회 미만 통과, 5회째 잠금, 잠금 중 거절, 성공 시 리셋)
  - 우선순위: P0(AUTH-5의 선행)

- [ ] **AUTH-5** 이메일 로그인 API (`POST /api/auth/login`) + 소프트탈퇴 자동복구
  - 변경 내용: 이메일 정규화 → `LoginAttemptLimiter`로 잠금 여부 우선 확인(잠겨있으면 비밀번호 검증 없이 즉시 거절) → `findByEmail` → 계정 없음/`passwordHash == null`(OAuth 전용 계정)/비밀번호 불일치를 **전부 동일한 `INVALID_CREDENTIALS`로 응답**(이메일 존재 여부를 로그인 응답으로 노출하지 않음 — 회원가입 중복확인과는 다른 정책) 및 실패 카운터 증가. 소프트탈퇴 7일 유예기간 내 계정이면 자동 복구하고 응답에 `restored:true` 포함, 유예기간 경과 계정은 동일하게 거절. 성공 시 실패 카운터·잠금 리셋.
  - 대상 파일: `api/AuthController.java`, `domain/user/service/UserService.java`, 신규 `LoginRequest`/`LoginResponse` DTO
  - 선행 작업: AUTH-1, AUTH-2, AUTH-4, PW-1, RATE-1(계정이 있어야 실제 로그인 통합테스트 가능 — 로직 자체는 독립 개발 가능)
  - 완료 조건: 정상 로그인 성공, 계정없음/비밀번호불일치/OAuth전용계정 모두 동일한 `INVALID_CREDENTIALS`, 5회 실패 후 잠금, 유예기간 내 자동복구+`restored:true`, 유예기간 경과 후 거절
  - 검증할 테스트: 신규 로그인 테스트(성공/3가지 실패 케이스 응답 동일성/잠금/자동복구/영구탈퇴 후 거절)
  - 우선순위: P0

- [ ] **AUTH-6** 비밀번호 변경 API (`PATCH /api/users/me/password`)
  - 변경 내용: 로그인 사용자의 현재 비밀번호 확인 후 새 비밀번호로 교체. OAuth 전용 계정(`passwordHash=null`)은 이 API 자체를 차단.
  - 대상 파일: `api/UserController.java`, `domain/user/service/UserService.java`, 신규 `PasswordUpdateRequest` DTO
  - 선행 작업: AUTH-1, AUTH-5, PW-1
  - 완료 조건: 정상 변경, 현재 비밀번호 불일치 거절, OAuth 전용 계정 거절
  - 검증할 테스트: 신규 테스트(성공/현재비번불일치/OAuth계정거절)
  - 우선순위: P1

> **탈퇴 정책과의 정합성 확인 완료**: `User.anonymize()`가 이미 이메일을 `withdrawn-{id}@anonymized.local`로 치환하므로 영구 익명화 후엔 원래 이메일의 UNIQUE 슬롯이 자연히 해제되어 재가입 가능 — 일반 계정은 여기에 `this.passwordHash = null;` 한 줄만 추가하면 됨. 소프트탈퇴 7일 이내는 이메일·해시 유지(AUTH-5의 자동복구), 7일 후 익명화되면 기존 비밀번호로 로그인 불가 — 현재 정책과 그대로 맞음.

### 작업그룹 B — 신청 조회 응답 보강

- [x] **LOOKUP-1** `ApplicationLookupResponse`에 `applicationType` 필드 추가 — ✅ 구현+커밋 완료(Codex, `8d178cc`, 2026-08-19)
  - 변경 내용: 응답 DTO에 `applicationType`(INDIVIDUAL/GROUP) 추가. `Application` 엔티티엔 이미 있던 값을 `ApplicationService.lookup()`이 DTO 생성 시 넘기도록 수정.
  - 대상 파일: `domain/application/dto/ApplicationLookupResponse.java`, `domain/application/service/ApplicationService.java`(`lookup()`), `ApplicationServiceLookupTest.java`
  - 완료됨: 신청번호 조회·카드번호 조회 응답 둘 다 `applicationType` 포함. 위 진행 보드 "단체 재제출 UI(`MobileCardPage.tsx`) 추가"(프론트 담당자 행)가 이제 실제로 착수 가능함.

### 작업 순서 (의존관계 기준)

```
AUTH-1 ─→ AUTH-2 ─┐
PW-1 ──────────────┤
MAIL-1 ─→ SIGNUP-1 ─→ SIGNUP-2 ─┴─→ AUTH-4 ─┐
RATE-1 ──────────────────────────────────────┴─→ AUTH-5 ─→ AUTH-6

LOOKUP-1 — 완료(Codex, 8d178cc)
```

### 정책 결정이 필요한 항목

- **계정 복구**(아이디 찾기, 비밀번호 재설정): 이메일 발송 수단(MAIL-1)은 AUTH-4 인증코드 발송용으로 구축하면 그대로 재사용 가능해진다 — 다만 "찾은 아이디를 어떻게 마스킹해서 보여줄지", "재설정 토큰 만료 시간" 등 계정복구 자체의 세부 계약은 아직 미정이라 AUTH-1~6과는 별도 작업으로 남긴다.
- ~~이메일 인증 여부·비밀번호 해시 알고리즘·로그인 시도 제한 구체값~~ — ✅ 2026-08-19 확정(위 "정책 확정(2차)" 참고, PW-1/MAIL-1/SIGNUP-1/SIGNUP-2/RATE-1로 반영 완료).
- **단체 신청 구성원별 상세·카드 ZIP 다운로드 API**(`/api/my/bulk-applications/{id}/members`, `/cards/download`): 프론트 `MyPage`/`MobileCardPage` 어디에도 이 데이터를 쓰는 화면이 없음을 확인함 — 실제로 필요한 화면인지부터 확인 필요. 필요 시 `MyApplicationController`에 신규 엔드포인트+`ApplicationMemberRepository` 조회 추가.
- **카드 다운로드 가능 기준(`COMPLETED` vs `cardReadyAt`)**: 문서(`FRONTEND_API_INTEGRATION_SPEC.md`)는 `cardReadyAt` 기준이 확정 정책이라 하지만 실제 코드(`ApplicationService.getCardDownload()`)는 `COMPLETED` 단독 검사 — 이미 위 "신청 상태·취소·환불 구조 변경 체크리스트" §5(라인 166)에 동일 항목이 있음, 중복 추가하지 않고 그 항목을 그대로 참조. 관리자 API 미구현으로 `PRODUCING`/`markPhysicalDispatched` 상태에 실제로 도달시킬 방법이 없어 실질적으로 검증도 안 되는 상태.
- **Event `company`/`logoUrl` 필드**: `host`(주최자 텍스트)로 대체 가능한지 먼저 결정. 결정 시 `EventPost`+DTO 4종에 필드 추가.
- **Payment 도메인(입금자명 저장·확인)**: 도메인 신설 여부 자체가 미정.
- **학생증 `schoolName` 저장 필드**: 실제 카드 발급에 필요한 정보인지 확인 필요.
- **신청 건별 동의 이력(상담확인·유의사항) 저장**: 단순 UX 게이트로 충분한지, 이력 저장이 필요한지 결정 필요.
- **후기 다중 이미지**: 현재 API·DB 둘 다 1장으로 확정 동작 중 — 다중 유지 여부 결정 필요.
- **게시판 서버 검색(keyword/searchType)**: 현재 데이터량에서 클라이언트 검색으로 충분한지, Review 검색 패턴을 재사용해 지금 만들지 결정 필요.
- **한국이름 조회 API**: 서버 API로 옮길지, 외부 링크아웃으로 대체할지 결정 필요.

### 프론트엔드 담당 작업

- 위 LOOKUP-1 완료 후 착수 가능한 "단체 재제출 UI(`MobileCardPage.tsx`) 추가"는 이미 진행 보드에 있음(라인 43) — 별도 행 신설 안 함.
- 그 외 프론트 전담 항목은 기존 진행 보드(라인 43·44·60)에 이미 등록돼 있어 중복 추가하지 않음.

### 문서만 수정할 항목 (백엔드 작업 아님)

- `FRONTEND_API_INTEGRATION_SPEC.md`/`BACKEND_API_GAPS.md`/`backend/FRONTEND_API_REQUIREMENTS.md`의 마이페이지 신청 목록·상세, 신청 취소 "미커밋" 표기 → `main` 커밋 완료(`b5f6140`, 2026-08-19)로 갱신 필요(`docs/FRONTEND_API_GAPS.md`는 이미 갱신함).
- 신청 조회 phone+email 서술 정정: 문서가 "phone+email만으로 조회 가능"처럼 서술하지만 실제 코드는 `keyValue`(신청번호)가 항상 `@NotBlank` 필수이고 phone/email은 부가 일치 검증일 뿐 — 서술을 "신청번호+phone+email 조합" 기준으로 명확화.
- 회원정보 `address` 수정 불가: 문서에 여전히 열린 갭처럼 적혀 있지만 `UserUpdateRequest.java` 코드 주석에 "확정 정책(2026-08-08): address는 이 API로 수정하지 않는다"고 명시돼 있음 — 갭이 아니라 확정 정책이라는 사실을 문서에 반영.

### 이미 구현되어 제외된 항목

- 마이페이지 신청 목록·상세(`GET /api/my/applications`, `/{id}`) — 구현+커밋 완료(`b5f6140`), 위 진행 보드 라인 63 참고.
- 사용자 신청 취소(`POST /api/applications/{id}/cancel`) — 구현+커밋 완료(`b5f6140`).
- 카드 종류·디자인 카탈로그 조회 API — 신설 안 함으로 이미 확정(2026-08-06), 코드도 그에 맞게 구현됨(`CardTypeSeeder`).

### 현재 범위에서 제외된 관리자 기능 (이번 체크리스트 대상 아님)

- **관리자 신청 관리 전체**(`AdminApplicationController` — 목록/상세/상태전이/결제안내/입금확인/사진반려/한국이름등록/카드발급/배송추적/상태이력/통계): 관리자 신청 관리 페이지 자체가 이번 구현 범위 밖이라 제외. 위 진행 보드 라인 70에 이미 등록돼 있어 중복 추가 안 함. (참고: `ApplicationService.guidePayment`/`confirmPayment` Service 메서드와 `Application` 엔티티의 상태전이 메서드 대부분은 이미 존재하지만 호출하는 Controller가 없는 상태 — 나중에 착수 시 이 Service 계층을 그대로 재사용하면 됨.)
- **1:1 문의(Inquiry) 도메인**: 사용자 접수 화면도 포함되지만 이번 범위에서 함께 제외(사용자 지시). 위 진행 보드 라인 68에 이미 등록돼 있어 중복 추가 안 함.
- **관리자 이벤트 전체 목록·상세 API**(`GET /api/admin/events`, `/{id}`): `EventAdminController`에 GET 자체가 없음 확인됨. `components/admin/EventAdminPanel.tsx`를 확인한 결과 숨김(`visible=false`) 이벤트를 다시 불러오는 UI 자체가 없어(현재 편집은 공개 목록만 대상) 소비 화면이 없음 — 관리자 화면 없으므로 제외.
- **이벤트 갤러리(`images`) PATCH 편집 지원**: `EventAdminPanel.tsx`가 이미 "갤러리는 생성 시에만 설정 가능"으로 프론트에서 우회 구현해뒀음(코드 주석 확인) — 현재 이걸 요구하는 소비 UI가 없어 제외.

---

## 신청 상태·취소·환불 구조 변경 체크리스트

> 기준: 2026-08-17 확정 정책. `origin/main` `26ac036`에는 일일 KST 3회 제한만 구현되어 있으며 아래 항목은 미구현 상태다.

### 1. 정책 문서 정합성

- [x] `requirements.md`의 `PAYMENT_PENDING → RECEIVED → REVIEWING` 선형 흐름을 새 상태 구조로 교체
- [x] `data-model.md`에 새 상태 enum과 취소·환불·결제기한·카드준비·실물발송 필드 반영
- [x] `api.md`의 생성·조회·재업로드·다운로드 상태 예시에서 `PAYMENT_PENDING`, `RECEIVED` 제거
- [x] `docs/api/admin.md`, `docs/api/payment.md`의 `PAYMENT_PENDING → RECEIVED` 계약과 관리자 액션을 새 결제·상태 분리 정책으로 교체
- [x] `service-flow.md`의 일일 신청 제한 미구현 문구와 상태 관련 주석을 현재 구현·정책 기준으로 갱신
- [x] 해결된 신청·결제 선후 관계와 3일 자동 취소 항목을 `PENDING_DECISIONS.md`에서 정리
- [x] 취소 파일 30일 보관/Cleanup Scheduler TBD를 “취소 commit 직후 S3 삭제” 확정 정책으로 `PENDING_DECISIONS.md`에서 정리
- [ ] 프론트 상태 라벨·관리자 필터 및 `backend/FRONTEND_API_REQUIREMENTS.md` 동기화는 프론트 작업 범위로 별도 전달

### 2. Enum 및 Application 필드

- [x] `ApplicationStatus`를 `SUBMITTED, REVIEWING, PHOTO_REJECTED, NAME_EDITING, PRODUCTION_READY, PRODUCING, COMPLETED, CANCELLED`로 변경
- [x] `PaymentStatus`는 입금 확인 이력인 `WAITING, CONFIRMED`만 유지
- [x] `CancellationType`에 `USER, SYSTEM, ADMIN` 정의
- [x] `CancellationReason`에 `USER_REQUEST, PAYMENT_TIMEOUT, ADMIN_DECISION` 정의
- [x] `Application`에 nullable `cancelledAt`, `cancellationType`, `cancellationReason`, `refundedAt` 추가
- [x] `Application`에 nullable `paymentGuidedAt`, `paymentDueAt` 추가
- [x] 모바일 다운로드 기준 `cardReadyAt`, 실물 발송 여부 `physicalDispatchedAt` 반영
- [ ] `Application`에 동시 상태 변경 감지를 위한 `@Version` 필드 추가하고 DB 컬럼·기존 row 초기화 방식 반영 — Entity 필드는 추가 완료, 운영 DB 기존 row 초기화·배포 절차는 미완료
- [ ] 기존 데이터 변환 정의: `PAYMENT_PENDING → SUBMITTED`, `RECEIVED → SUBMITTED + CONFIRMED`, 나머지 상태 유지
- [ ] nullable 신규 컬럼과 enum 변경의 운영 배포 순서 검증

### 3. Entity 상태 전이와 불변조건

- [x] 생성 초기값을 `SUBMITTED + WAITING`으로 변경
- [x] `confirmPayment()`는 ApplicationStatus를 변경하지 않고 `CONFIRMED`만 기록하며, 자동 취소 후 늦은 입금(`CANCELLED + WAITING`)도 재활성화 없이 허용
- [x] 이미 `CONFIRMED`인 입금 확인 재호출은 값을 변경하지 않는 멱등 성공으로 처리
- [x] 결제 안내 최초 처리만 `paymentGuidedAt`, `paymentDueAt`을 기록하고 재안내는 기한을 초기화·연장하지 않도록 보장
- [x] `startReview()`는 `SUBMITTED + CONFIRMED`에서만 허용
- [x] `REVIEWING → PHOTO_REJECTED → REVIEWING` 반려·재업로드 전이 유지
- [x] 관리자 검토 승인 시 `REVIEWING → NAME_EDITING` 전이
- [x] 작명 완료 시 `NAME_EDITING → PRODUCTION_READY` 전이 추가
- [x] 제작 시작은 `PRODUCTION_READY + CONFIRMED`에서 관리자 승인으로만 허용
- [ ] 카드 파일 생성 완료를 `markCardReady()` 같은 명시적 전이로 분리하고, 단체는 모든 Member의 앞·뒷면 파일 준비 완료 후 한 번만 `cardReadyAt` 기록
- [x] `MOBILE`은 카드 준비 완료와 함께 `COMPLETED`, `MOBILE_AND_PHYSICAL`은 카드 준비 후 `PRODUCING`을 유지하다 택배사 인계 시 `physicalDispatchedAt` 기록 후 `COMPLETED`로 전이
- [x] `physicalDispatchedAt`은 `MOBILE_AND_PHYSICAL + cardReadyAt!=null`에서만 기록하고 배송사·운송장·배송완료 상태는 Application에 추가하지 않음
- [x] 사용자 취소 가능 상태를 `SUBMITTED, REVIEWING, PHOTO_REJECTED`로 제한
- [x] `cancelByUser()`는 최초 1회만 `CANCELLED`, `cancelledAt`, `USER_REQUEST` 기록; PaymentStatus 유지
- [x] 이미 `CANCELLED`이면 값을 다시 변경하지 않고 멱등 성공
- [x] `cancelForPaymentTimeout()`은 `SUBMITTED + WAITING + paymentDueAt 경과`에서만 허용하고 `PAYMENT_TIMEOUT` 기록
- [x] 현재 모든 비취소 상태를 취소할 수 있는 범용 `cancel()`을 제거/비공개화하고 이번 범위에서는 `cancelByUser()`, `cancelForPaymentTimeout()`만 각각 허용 상태를 검증하도록 분리
- [x] 관리자 직접 취소 API·Service 메서드는 구현하지 않고 `ADMIN`, `ADMIN_DECISION` 값만 향후 확장용으로 예약
- [x] `cancellationType`과 `cancellationReason`의 허용 조합(`USER/USER_REQUEST`, `SYSTEM/PAYMENT_TIMEOUT`, `ADMIN/ADMIN_DECISION`) 불변조건 보장
- [x] `markRefunded()`는 `CANCELLED + CONFIRMED`에서만 `refundedAt`을 한 번 기록
- [x] `refundedAt != null`이면 반드시 `CANCELLED + CONFIRMED` 불변조건 보장
- [x] 자동 취소 후 늦은 입금은 재활성화하지 않고 `CANCELLED + CONFIRMED + refundedAt=null` 유지

### 4. Service 및 트랜잭션

- [x] 사용자 취소를 `조회 → 소유권 → 멱등 확인 → 상태 검증 → 취소 → 일일 슬롯 반환` 순서로 처리
- [x] 취소 상태 저장과 `releaseSlot()`을 동일 트랜잭션에서 commit/rollback
- [x] 최초 `CANCELLED` 전이에만 신청 생성일 KST 슬롯 반환; 중복 요청에서는 반환 금지
- [x] 사용자 취소에 `@Version` 낙관적 락 적용 — 입금 확인·자동 취소 Service 연결은 후속 단위
- [x] 결제 안내 시 `paymentGuidedAt`, `paymentDueAt=paymentGuidedAt+3일` 기록
- [x] 관리자 결제 안내와 입금 확인을 호출할 Application Service 명령을 추가하고 관리자 권한을 검증; 최초 입금 확인에만 `PAYMENT_CONFIRMED` 감사로그 1건 기록
- [ ] 결제 안내/입금 확인/검토 시작/검토 승인/편집 완료/제작 승인/제작 완료가 새 Entity 메서드만 통해 전이되도록 관리자 Application 처리 흐름 연결
- [x] 자동 취소 스케줄러는 `SUBMITTED + WAITING + paymentDueAt<=now` 조회 후 처리 직전 Entity에서 재검증
- [x] 자동 취소 스케줄러 기본 주기를 10분(`0 */10 * * * *`)으로 두고 `application.payment-timeout-scheduler.cron` 설정으로 변경 가능하게 구현
- [ ] `ApplicationRepository`에 자동 취소 대상과 환불 대기 대상 조회를 추가하고, 상태 변경용 조회에는 선택한 낙관적/비관적 잠금 정책을 일관되게 적용 — 자동 취소 대상 조회·`@Version` 적용 완료, 환불 대기 조회는 후속
- [x] 스케줄러와 관리자 입금 확인이 같은 신청을 처리할 때 낙관적 락 충돌을 감지하고 스케줄러가 해당 stale 후보를 건너뛰도록 처리
- [x] 늦은 입금 확인은 환불 대상 조합으로만 바꾸고 Application 상태를 복구하지 않음
- [x] 최초 사용자/자동 취소 commit 직후 `afterCommit`에서 얼굴사진·로고·직인·제출 ZIP 등 Application 전용 S3 객체를 즉시 삭제; 중복 취소에서는 재삭제하지 않음
- [x] 사용자 취소 DB 트랜잭션에서 `logoFileId`, `sealFileId`, `submitFileId`, `ApplicationMember.photoPath` 등 삭제 대상 참조와 해당 UploadFile metadata row를 함께 정리하고 Application/Applicant/Receiver/Member 이력 row는 유지
- [x] 사용자 취소 DB rollback/commit 실패 시 S3 삭제를 실행하지 않고 기존 파일을 보존
- [x] 사용자 취소 after-commit S3 삭제 실패는 취소 결과를 되돌리거나 원 예외로 바꾸지 않고 실패 key를 오류 로그로 남겨 운영자가 수동 재삭제
- [ ] 환불 완료 처리 시 기존 `AdminActivityLog`에 관리자·신청·처리 시각 기록
- [ ] `AdminActivityLog`에 환불 완료 action type을 추가하고 최초 완료 때만 로그가 한 건 생성되도록 보장
- [x] `ApplicationDailyLimitService`의 “향후 취소 구현” 주석을 실제 취소 트랜잭션 연결 방식으로 갱신

### 5. API 및 조회 계약

- [x] `POST /api/applications/{applicationId}/cancel` 추가; 로그인 본인 신청만 허용
- [x] 요청 본문 없이 ApplicationStatus, PaymentStatus, 환불 필요 여부 응답
- [x] 신청 없음, 타인 신청, 취소 불가 상태, 중복 취소의 HTTP/ErrorCode 계약 구현·검증
- [ ] 관리자 결제 안내 API/명령과 입금 확인 API 계약을 분리하고, 안내 시각·기한 및 실제 입금 이력을 각각 응답에 반영
- [ ] 입금 확인 최초 호출과 중복 호출 모두 `200 OK`; 중복 호출은 `CONFIRMED` 유지와 “이미 입금 확인 완료” 안내를 기존 `ApiResponse` 형식으로 반환
- [ ] 관리자 상태 변경 API가 임의 status 문자열 덮어쓰기가 아니라 검토 시작·승인·편집 완료·제작 승인·제작 완료 명령을 호출하도록 계약 정리
- [ ] 카드 파일 준비 완료와 `MOBILE_AND_PHYSICAL` 택배사 인계를 기록하는 관리자 API/명령을 분리하고, 인계 API는 배송사·운송장 정보를 받지 않도록 계약
- [ ] 낙관적 락 충돌을 409 등 일관된 ErrorCode/HTTP 응답으로 매핑
- [ ] `CANCELLED + CONFIRMED + refundedAt=null`은 환불 대기, `refundedAt!=null`은 환불 완료로 조회
- [ ] 관리자 환불 대상 조건을 `CANCELLED + CONFIRMED + refundedAt IS NULL`로 정의
- [ ] 관리자 환불 완료 API 또는 내부 관리 명령에서만 `markRefunded()` 호출
- [ ] 카드 다운로드 조건을 `COMPLETED` 단독 검사에서 `cardReadyAt` 기준으로 변경
- [ ] `MOBILE`은 카드 파일 준비 시 완료, `MOBILE_AND_PHYSICAL`은 `physicalDispatchedAt` 기록 시 완료
- [ ] 배송사·운송장·배송 중·배송 완료 상태는 저장하거나 제공하지 않음

### 6. 테스트

- [x] 새 ApplicationStatus 정상 전이와 역방향·건너뛰기 거절 Entity 테스트
- [x] 기존 테스트 픽스처의 `PAYMENT_PENDING → confirmPayment() → startReview()` 전제를 `SUBMITTED + CONFIRMED → startReview()` 정책에 맞게 일괄 수정
- [ ] 생성 API 응답의 초기값이 개인·단체 모두 `SUBMITTED + WAITING`인지 검증
- [x] 결제 안내 최초 시각+72시간 기한, 재안내 시 기한 불변, 입금 확인 시 ApplicationStatus 불변을 검증
- [ ] `CONFIRMED` 입금 확인 재호출이 200 멱등 성공하고 상태·시각·이력을 중복 변경하지 않는지 검증
- [ ] 카드 준비 시 IssueType별 상태 전이와 `cardReadyAt`, `physicalDispatchedAt`, 조기 다운로드 허용 조건 검증
- [x] `WAITING` 취소는 `CANCELLED + WAITING`, `CONFIRMED` 취소는 `CANCELLED + CONFIRMED + refundedAt=null` 검증
- [ ] `SUBMITTED, REVIEWING, PHOTO_REJECTED` 취소 성공과 `NAME_EDITING` 이후 거절 검증
- [x] 중복 취소가 멱등 성공하고 일일 슬롯을 한 번만 반환하는지 검증
- [x] 타인 신청 취소 403, 신청 없음 404 검증
- [ ] 환불 완료 선행조건·멱등성·환불 대기 목록 제외 검증
- [ ] 결제 안내 전, 3일 미경과는 자동 취소하지 않고 기한 경과 건만 취소하는지 검증
- [ ] 자동 취소와 입금 확인 동시 실행 통합 테스트
- [x] 자동 취소 후 늦은 입금이 신청을 재활성화하지 않는지 검증
- [x] 취소 실패 시 Application 상태와 일일 카운터가 함께 rollback되는 실제 DB 통합 테스트
- [x] 사용자 취소 commit 전에는 S3를 삭제하지 않고 commit 성공 후 한 번만 삭제하며, rollback/commit 실패에서는 삭제하지 않는 트랜잭션 생명주기 테스트
- [x] S3 삭제 실패에도 취소 상태와 일일 슬롯 반환이 유지되는지 검증하고 실패 key 오류 로그 유지
- [ ] 조회·사진 재업로드·카드 다운로드·Review 작성 자격 테스트 전체 회귀 검증

### 7. 최소 환불 모델 운영 한계

- [ ] `refundedAt`은 환불 누락과 중복 완료 기록은 방지하지만 외부 계좌이체 중복 송금까지 보장하지 못함을 운영 문서에 명시
- [ ] 초기 운영은 환불 대기 목록 + 관리자 완료 처리 + `AdminActivityLog`로 관리
- [ ] 다중 관리자 중복 송금 문제가 실제 발생할 때만 `refundProcessingAt`, `refundedBy` 또는 Refund 엔티티 재검토

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
- [x] 생성 후 `CANCELLED`된 신청은 해당 날짜의 신청 횟수에서 제외(취소 시 자리가 다시 빔) — 사용자 취소 API가 최초 취소 시 `ApplicationDailyLimitService.releaseSlot`을 같은 트랜잭션에서 호출하도록 연결 완료
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
