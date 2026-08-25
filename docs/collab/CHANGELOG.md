# CHANGELOG

작업을 종료할 때마다 **맨 위에** 새 항목을 추가한다 (최신이 위). 과거 항목은 수정·삭제하지 않는다.

## 템플릿

```md
## {YYYY-MM-DD} — {Claude|Codex} — `{브랜치명}`

- 변경: {무엇을 바꿨는지 한두 줄}
- 파일: {변경한 파일 목록}
- 사유: {왜 바꿨는지 — 요구사항 변경 / 버그 수정 / 설계 정리 등}
- 관련: TODO #{번호} (있다면)
```

---

## 2026-08-25 — Claude — `main` (관리자 신청 엑셀 내보내기 API)

- 변경: `POST /api/admin/applications/export`(DESIGN.md §2.4, 프론트가 이미 버튼까지 만들어둔 계약) 구현. INDIVIDUAL은 DB 값으로 새 워크북(단체신청 템플릿과 동일 컬럼)을 만들어 여러 건을 한 시트에, GROUP은 신청 시 업로드된 원본 ZIP의 xlsx를 그대로 열어 이름·한자 컬럼만 append(원본 서식·병합셀 보존). GROUP은 신청마다 원본 서식이 달라 여러 건을 하나의 워크북으로 안전하게 합칠 수 없어 정확히 1건만 허용(2건 이상은 `INVALID_INPUT`) — 프론트가 GROUP 탭에서 전체 행을 한 번에 보내는 지금 동작과 안 맞아 lotus05f님께 별도 전달 필요.
- 파일: `ApplicationExportRequest.java`(신규 DTO), `ApplicationExportExcelBuilder.java`(신규, POI 쓰기 유틸), `ApplicationService.java`(`exportExcel`), `AdminApplicationController.java`(`POST /export`), 테스트 3개 파일(신규 `ApplicationExportExcelBuilderTest`/`ApplicationServiceExportTest` 포함)
- 사유: 어느 작명 경로(엑셀 왕복/인앱)를 택하든 공통으로 필요한 마지막 조각이라 다른 정책 결정과 무관하게 우선 진행(BACKEND_TODO.md 자체 우선순위 1번).
- 검증: `./gradlew.bat test`(REDIS_PORT=6400) 전체 608개 통과(실패 0).
- 관련: TODO "관리자 신청 엑셀 내보내기 API"

## 2026-08-25 — Claude — `main` (관리자 신청 상태 전이 API)

- 변경: 사진반려/제작시작/카드발급/실물배송/작명완료 5개 상태 전이를 관리자 API로 노출했다. 전이 규칙 자체는 이미 있던 `Application` 엔티티 메서드를 그대로 호출만 하고 재구현하지 않았다. 실물배송 처리 시 운송장번호를 `AdminActivityLog`가 아니라 `Application.trackingNumber`(신규 nullable 컬럼)에 별도 저장하도록 정책 확정. 어제 만든 `applyNamingResult`(naming-result API)가 로그를 전혀 안 남기고 있던 걸 발견해, 멤버별 `KOREAN_NAME_REGISTER`/`KOREAN_NAME_UPDATE`(신규/덮어쓰기 구분)를 추가했다. `completeNaming()`(상태 전이 자체)은 이름 저장과 의미가 달라 별도 신규 상수 `NAMING_COMPLETE`로 기록해 중복 로그를 피했다. `startProducing()`용 `PRODUCTION_START` 상수도 신규 추가.
- 파일: `Application.java`(trackingNumber, markPhysicalDispatched 시그니처), `AdminActivityLog.java`(상수 2개), `ApplicationStatusResponse.java`/`RejectPhotoRequest.java`/`DispatchRequest.java`(신규 DTO), `ApplicationService.java`(전이 메서드 5개 + applyNamingResult 로깅), `AdminApplicationController.java`(엔드포인트 5개), 테스트 4개 파일(신규 `ApplicationServiceAdminTransitionTest` 포함)
- 사유: 관리자 대시보드(프론트, lotus05f)가 제작신청 파이프라인을 실제로 운영하려면 이 5개 전이를 관리자가 트리거할 방법이 필요했음(엔티티엔 규칙이 있었지만 그동안 시드·테스트만 직접 호출, 노출 API가 전혀 없었음).
- 검증: `./gradlew.bat test` 전체 596개 통과(실패 0). 로컬 환경 메모: `honor-citizen-redis-test` 컨테이너가 host 6400↔container 6379로 매핑돼 있어 `REDIS_PORT=6400`을 명시해야 정상 통과함(기본값 6379로 돌리면 기존 테스트까지 전부 503 — 코드 문제 아님).
- 관련: TODO "관리자 신청 상태 전이 API"

## 2026-08-24 — Codex — `main` (출생지역 필수화 및 단체 Excel 안내 갱신)

- 변경: 개인 신청 `member.birthRegion`을 `@NotBlank` 필수값으로 전환하고, 단체 Excel 파서도 출생지역 누락을 `birthRegion/REQUIRED` 오류로 수집하도록 변경했다. 출생지역은 태어난 도시/지역명으로 정의하고 `Chicago`, `London`, `Tokyo`, `Beijing`, `Los Angeles` 예시를 정책·API·양식 작성안내에 반영했다.
- 파일: `ApplicationCreateRequest.java`, `BulkExcelParser.java`, `BulkMemberRow.java`, `ApplicationMember.java`, 관련 테스트 6개, `docs/specs/application/{requirements,data-model,api}.md`, `docs/collab/BULK_EXCEL_TEMPLATE_POLICY.md`, `outputs/bulk-excel-templates-20260818/*_v1.1.xlsx`
- 검증: 신규 DTO/Parser 테스트 2개가 구현 전 의도대로 실패한 뒤 통과. Application 도메인 182개 통과. Excel 3종 각각 24개 구조 검증 통과, 전 시트 렌더 montage 육안 확인.
- 호환성: DB `birth_region`은 기존 데이터·스키마 호환을 위해 nullable을 유지하고 신규 개인 API/단체 Excel 입력 계층에서만 필수로 강제한다.
- 병합: 동시 작업에서 확정된 사용자 노출 명칭 `출생국가`를 Excel D열·작성안내·검증 제목에 반영했다. 내부 호환성을 위해 API/Java/DB 키 `nationality`는 변경하지 않았다.

## 2026-08-24 — Claude — `main` (공지·FAQ·후기 데모 시드 추가 — 프론트 목데이터 백엔드 이관)

- 변경: 사용자 요청으로 프론트 목데이터를 백엔드에 시드하는 `DemoDataSeeder`(CommandLineRunner)를 추가했다.
  - **공지사항 4건·FAQ 8건**: `frontend SupportPage`의 `notices`/`faqs` 배열을 그대로 `Board`(NOTICE/FAQ)로 이관. FAQ는 title=질문, content=답변.
  - **후기**: 프론트에 목데이터가 없어(완전 API 기반) 카드종류별 대표 **샘플 6건**을 생성(개인/단체, 4개 카드종류). 실제 사용자 후기가 쌓이기 전까지 후기 페이지가 비지 않도록 함.
  - 작성자/소유자 audit용 데모 유저(`seed-demo@hangeul-sejong.local`, OAuth 계정이라 비번 로그인 불가)를 1회 생성해 `Board.createdByUserId`(NOT NULL)·`Review.userId`에 사용.
  - **가드**: `@ConditionalOnProperty("app.seed-demo-data")` — 프로퍼티가 없으면 빈 자체가 안 만들어져 테스트(@SpringBootTest)에 무영향. docker-compose backend에 `APP_SEED_DEMO_DATA=true` 추가(기본 켜짐, 각 도메인 비어 있을 때만 1회 채움 → idempotent).
  - `CardTypeSeeder`에 `@Order(1)`, `DemoDataSeeder`에 `@Order(2)`를 주어 후기 시드 시 카드종류가 반드시 먼저 존재하도록 함.
- 파일: `infra/seed/DemoDataSeeder.java`(신규), `domain/card/CardTypeSeeder.java`(@Order 추가), `docker-compose.yml`
- 사유: reviews/공지/FAQ 페이지가 백엔드 데이터가 없어 비어 보이던 문제를, 프론트 목데이터를 백엔드로 이관해 실제 API로 채워지도록 해결.
- 관련: 아래 2026-08-24 "API 미연동 원인 진단" 항목의 후속(데이터 없음 → 시드로 채움)

---

## 2026-08-24 — Claude — `main` (관리자 행사 게시글 실제 API+이미지 연동 / 계정 찾기 실제 API / dev 프록시 + API 미연동 원인 진단)

### 원인 진단(중요)
사용자가 "행사 페이지에 내용 없음, 후기/공지/FAQ 불러오기 안 됨, 계정 찾기 안 됨"을 보고. 조사 결과:
- **공개 EventsPage/NoticesPage/FaqPage/ReviewsPage는 실제 API를 호출**(정상 코드)하지만, **SupportPage의 공지사항은 하드코딩 정적 배열**이라 "지원 페이지엔 보이는데 공지 페이지는 안 뜨는" 착시가 생겼다. 즉 이들 "안 뜸"의 공통 원인은 **연결된 백엔드에 데이터가 없거나(관리자/사용자가 생성해야 채워짐) 백엔드 미연결**이지 프론트 코드 버그가 아니다.
- **진짜 코드 문제 3가지**를 수정: (1) 관리자 행사 게시글이 localStorage mock이라 공개 API와 안 이어짐, (2) 계정 찾기가 데모 스텁(API 미호출), (3) dev 환경에 API 프록시 없음.

### 수정 1 — 관리자 행사 게시글 실제 API + 이미지
- `AdminPage`가 쓰던 localStorage 기반 `EventFeedAdminPanel`을 제거하고, 실제 이벤트 API 기반 `EventAdminPanel`로 교체(자체적으로 `GET /api/admin/events`로 목록 로드).
- 썸네일(1) + **행사 이미지 갤러리(다중, 최대 10장, 수정 시 keepImageIds로 유지/삭제/추가)** + **협업 로고(COLLABORATION 전용)** 업로드 지원. 회사·단체명·공개여부·정렬순서 필드 추가, 목록에 제목/장소/주최/비공개 표시.
- 이제 관리자가 만든 행사가 실제 백엔드에 저장돼 공개 EventsPage에 그대로 노출된다(mock↔API 불일치 해소).

### 수정 2 — 공개 행사페이지 로고/회사명
- `eventToFeedPost`가 백엔드의 `company`/`logoUrl`을 매핑하도록 수정 → 협업 카드에 로고·회사명 표시(EventsPage 렌더는 이미 지원).

### 수정 3 — 계정 찾기(아이디/비밀번호) 실제 API
- `AccountRecoveryPage` 데모 스텁을 **2단계 실제 API 플로우**로 재작성: 아이디 찾기(이름+전화 → 코드 확인 → 마스킹 이메일), 비밀번호 찾기(이메일 → 코드+새 비밀번호 확인 → 재설정). `/api/auth/recovery/id|password/request·confirm` 사용.

### 수정 4 — api.ts 계약 정합 + dev 프록시
- `api.ts` 이벤트 계약을 강화된 백엔드에 맞춤: `company`/`logoUrl`/관리자 목록·상세(`listAdminEvents`/`getAdminEvent`) + `createEvent`/`updateEvent`가 thumbnail/logo/images 멀티파트와 `keepImageIds`/`removeThumbnail`/`removeLogo` 지원. 계정 복구 4개 메서드 추가.
- `vite.config.ts`에 dev 프록시(`/api`·`/oauth2`·`/login/oauth2` → `VITE_DEV_BACKEND`(기본 localhost:8080)) 추가 — dev에서 API가 vite 서버로 404 나던 문제 해소(운영은 nginx가 담당).

### 파일
- 프론트: `services/api.ts`, `components/admin/EventAdminPanel.tsx`(전면 개편)·`EventFeedAdminPanel.css`, `components/admin/EventFeedAdminPanel.tsx`(삭제), `pages/AdminPage/AdminPage.tsx`, `pages/EventsPage/EventsPage.tsx`, `data/eventFeedPosts.ts`, `pages/AccountRecoveryPage/AccountRecoveryPage.tsx`, `pages/LoginPage/LoginPage.css`, `vite.config.ts`

### 참고(백엔드 미연결 시)
- reviews/notices/FAQ가 비어 보이면 백엔드가 떠 있고 데이터가 있는지 확인 필요(공지/FAQ는 관리자 생성, 후기는 사용자 작성). dev 단독 실행 시엔 별도 백엔드(localhost:8080) 기동 + 이번 vite 프록시로 연결.
- SupportPage의 공지사항은 여전히 하드코딩(정적) — 실제 API로 바꾸려면 별도 작업 필요(이번 범위 밖).

---

## 2026-08-24 — Claude — `main` (후기 다중 사진 풀스택 + 관리자 신청상세/문의답변 + 후기 저장 버그·기존 플로우테스트 결함 수정)

### 요약
사용자 요청 3건(관리자 신청 세부내역 열람, 후기 다중 사진 첨부, 관리자 문의 상세/답변)을 구현하고, 그 과정에서 발견한 버그 1건과 이전부터 방치돼 있던 무관 테스트 결함 1건(`UserApplicationFlowTest`)을 실제로 수정했다. 백엔드 전체 테스트 **555개 전부 통과**(BUILD SUCCESSFUL)로 검증 완료.

### 기능 1 — 후기 다중 사진 (풀스택, 사용자 요청)
- 백엔드: `ReviewImage` 엔티티(`review_images`, `UNIQUE(review_id, display_order)`)와 `ReviewImageRepository` 신설. `Review.imagePath`는 목록 썸네일·`hasPhoto` 필터용 대표(첫) 이미지로 비정규화 유지. `ReviewService` 전면 개편: 생성 시 다중 업로드(최대 5장), 상세는 `images:[{id,imageUrl}]` 반환, 수정은 `keepImageIds`(유지할 기존 이미지)+새 파일 append 방식(EventService 갤러리 패턴 동일 — 임시 오프셋 재정렬·업로드 롤백·커밋 후 S3 삭제), 레거시 단일 `imagePath`는 상세/수정 시 `review_images` 행으로 지연 마이그레이션.
- DTO: `ReviewUpdateRequest.keepImageIds`, `ReviewDetailResponse.images`, `ReviewImageResponse`(신규). 컨트롤러는 기존 `List<MultipartFile>`(part명 `image`) 그대로 다중 수용.
- 프론트: `api.ts`(`createReview(body, File[])`·`updateReview(id, {..,keepImageIds}, File[])`·`ReviewDetail.images`), `ReviewEditorPage`(다중 선택·미리보기·개별삭제·기존이미지 유지/삭제, objectURL 누수 방지), `ReviewDetailPage`(갤러리 렌더), `reviews.ts` 매퍼.
- `review_images` 테이블은 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`로 자동 생성(마이그레이션 스크립트 불필요).

### 기능 2 — 관리자 신청 세부 내역 (프론트, 사용자 요청)
- `AdminApplication.detail`(`AdminApplicationDetail`) 추가 — 신청 생성 시(`ApplyPage`) 발급방식·개인/단체/학생 정보·수령인·배송지를 함께 저장. `AdminPage` 신청 행의 신청번호 클릭 시 세부내역 펼침(값 있는 항목만 표시, 데모 데이터는 요약만 + 안내).

### 기능 3 — 관리자 문의 상세/답변 개선 (프론트, 사용자 요청)
- 문의 상세/답변 UI는 존재했으나 피드백이 없어 "안 된다"고 느낄 수 있어, 저장 확인 토스트·빈 답변 방지·답변완료 일시 표시·버튼 라벨(저장/수정) 전환을 추가.

### 버그 수정 1 (내 변경에서 발견) — 후기 이미지 교체 시 UNIQUE 제약 위반
- 증상: `ReviewServiceUpdateTest.replacesImageAndDeletesOldOneAfterCommit`에서 H2 `JdbcSQLIntegrityConstraintViolationException`.
- 원인: Hibernate 기본 flush 순서(INSERT→DELETE)상, 삭제될 이미지의 `display_order 0`을 새 이미지가 재사용할 때 삭제가 반영되기 전 INSERT가 실행돼 충돌.
- 수정: `reconcileImages`에서 `deleteAllById` 직후 `reviewImageRepository.flush()`로 삭제를 먼저 DB에 반영.

### 버그 수정 2 (기존 방치 결함, 사용자 요청으로 근본 수정) — `UserApplicationFlowTest.fullUserApplicationFlow`
- 배경: 2026-08-23 CHANGELOG에 "기존 무관 결함"으로 남겨져 있던 실패. 내 변경을 stash하고 원본 HEAD에서도 동일 실패함을 확인해 사전 존재 결함임을 규명한 뒤, 이번 pull에서 강화된 API 계약에 맞춰 테스트를 실제로 수정했다(3중 원인).
  1. `POST /api/applications`가 **약관 동의 선행**을 요구(`TERMS_NOT_AGREED`, 403) → 로그인 후 `POST /api/auth/terms` 단계를 추가.
  2. 신청 사진이 **실제 이미지 시그니처 + 해상도 하한** 검증을 통과해야 함(가짜 바이트 → 400) → 300x400 유효 JPEG 생성 헬퍼로 교체(생성·재업로드 2곳).
  3. `APPLICATION` 방식 본인확인 lookup이 **전화 + 이메일 모두** 요구(`INVALID_INPUT`, 400) → lookup 요청에 이메일 추가.

### 테스트 결과 (Java 21 + Redis 컨테이너, `gradle test`)
- Redis 미기동 시 144개 실패 → 전부 `RedisConnectionFailureException`(스프링 컨텍스트 로드 실패, CI엔 Redis 존재)로 환경 문제임을 확인. Redis 컨테이너 연결 후 재실행.
- 최종: **555개 전부 통과, BUILD SUCCESSFUL**. 프론트는 `npm run build`(tsc+vite) 통과.

### 파일
- 백엔드(main): `domain/review/entity/{Review,ReviewImage}.java`, `domain/review/repository/ReviewImageRepository.java`, `domain/review/service/ReviewService.java`, `domain/review/dto/{ReviewUpdateRequest,ReviewDetailResponse,ReviewImageResponse}.java`
- 백엔드(test): `domain/review/service/{ReviewServiceCreateTest,ReviewServiceUpdateTest}.java`, `api/ReviewControllerTest.java`, `flow/UserApplicationFlowTest.java`
- 프론트: `services/api.ts`, `data/reviews.ts`, `pages/ReviewEditorPage/*`, `pages/ReviewDetailPage/*`, `pages/AdminPage/*`, `data/adminMock.ts`, `pages/ApplyPage/ApplyPage.tsx`, `components/apply/FileUploadBox.tsx`
- (같은 세션 UI 조정: 고객지원 FAQ 추가·순서변경, 로그인 데모계정 제거, 헤더 드롭다운 반응성·최상단 클릭 버그, 제작신청 필수입력 검증·자동스크롤·형식검증, 국적 검색 드롭다운)

### 사유
사용자 요청 기능 구현 + 그 과정에서 드러난 실제 버그 및 이전 세션이 방치한 테스트 결함을 "제대로 된 방향"으로(테스트를 현재 API 계약에 맞춰) 수정하고 전체 테스트로 검증.

### 문서
- 후기 API 계약 변경(다중 이미지 0~5장, `keepImageIds`, `images[]`, 대표 썸네일 비정규화)에 맞춰 `docs/specs/review/api.md`·`data-model.md`를 갱신(2026-08-24 개정 배너 + `ReviewImage` 재도입 §1-1 + API 1·3·4·5 반영).

---

## 2026-08-23 — Claude — `main` (계정 복구 RECOVERY-3 마무리 — 미검증 경계 케이스 6건 테스트 보강)

- 변경: 사용자가 "정책을 임의로 넣은 거 아니냐"고 재확인을 요청해 `docs/api/auth.md` API 7·8과 코드를 다시 줄 단위로 대조했고, 그 과정에서 2026-08-21 `e4c3287` 커밋이 RECOVERY-3 섹션을 "완료"라는 메시지로 추가했지만 체크박스는 전부 미체크 상태로 남아있던 것을 발견했다(전체 테스트 실행 자체는 그때 CHANGELOG에 남긴 대로 실질적으로 했었음). TODO.md에 ⚠️로 남아있던 6개 미검증 항목(코드 만료, confirm 시점 계정삭제/OAuth전환, 재설정 직후 같은 초 재로그인, DB 실패 후 세션 미복구, 동시요청 cooldown 우회, Redis 장애 시 HTTP 필터체인)을 전부 테스트로 보강했다. (d)는 실제 DB 장애를 재현할 인프라가 없어 `TokenSessionStore`를 `@MockitoSpyBean`으로 감싸 Redis 기록은 실제로 성공시키고 그 직후 `TransactionSynchronization.beforeCommit`을 등록해 Hibernate flush 직전에 결정론적으로 실패를 유발하는 방식을 새로 썼다 — 비밀번호는 롤백되고 Redis revoked-after 키는 실제로 남아있음을 확인했다. (f)는 MockMvc로 `JwtAuthFilter`→`GlobalExceptionHandler`까지 실제 필터체인을 관통시켜 503 JSON을 확인했다.
- 테스트: `AccountRecoveryServiceTest`(17→25, +8), `UserServiceResetPasswordRollbackTest`(신규, 1), `JwtAuthFilterSessionValidationIntegrationTest`(신규, 1) — 신규/보강 테스트 10개 전부 통과. 전체 스위트 553개 중 552개 통과(실패 1개는 이전과 동일한 `UserApplicationFlowTest:143` 기존 무관 결함).
- 파일: `AccountRecoveryServiceTest.java`, `UserServiceResetPasswordRollbackTest.java`(신규), `JwtAuthFilterSessionValidationIntegrationTest.java`(신규), `docs/collab/TODO.md`
- 사유: 계정 복구 정책이 임의로 만들어진 게 아니라 사전에 확정된 `docs/api/auth.md`를 따른 것임을 코드-문서 재대조로 확인했고, 그 과정에서 드러난 RECOVERY-3 체크리스트 미완료·6개 경계 케이스 미검증을 사용자 요청으로 실제 테스트로 매듭지었다.
- 관련: `RECOVERY-3`

---

## 2026-08-21 — Claude — `main` (Event 협업 로고·회사명·관리자 전체목록·갤러리 편집 구현 완료)

- 변경: Codex가 확정한 정책(`docs/specs/events/api.md`·`data-model.md`, `docs/collab/TODO.md` EVENT-EXT-0~6)에 맞춰 `EventPost.companyName`/`logoImagePath`(`COLLABORATION` 전용, `BOOTH` 전송 시 `INVALID_INPUT`)를 추가했다. 생성 API에 `logo` multipart part, 수정 API에 `removeLogo`/`removeThumbnail`(유지·교체·삭제)와 `keepImageIds`(생략=전체유지, `[]`=전체삭제, 그 외=재정렬+신규추가, 타 Event 이미지 ID 거절) 갤러리 편집을 추가했다. `GET /api/admin/events`·`/{id}`(visible 무관 전체) 신규. 공개 응답에도 `companyName`/`logoImageUrl` 추가(기존 필드 불변, 하위 호환). 갤러리 재정렬은 `UNIQUE(event_post_id, display_order)` 제약 충돌을 피하려고 임시 오프셋(+1000)으로 먼저 밀고 flush한 뒤 최종 값을 배정하는 2단계 방식을 썼다.
- 테스트: `EventPostTest`(10)·`EventServiceTest`(33)·`EventAdminControllerTest`(13)·`EventControllerTest`(6) 신규/보강, Event 도메인 전체 70개 통과. 전체 스위트 543개 중 542개 통과(실패 1개는 이번 작업과 무관한 기존 결함 `UserApplicationFlowTest`).
- 파일: `EventPost`, `EventImage`, `EventService`, `EventAdminController`, `EventPostRepository`, `EventImageRepository`, DTO 6종(`EventAdminDetailResponse`/`EventAdminListItemResponse` 신규), 테스트 4개 파일, `docs/specs/events/{api,data-model}.md`, `docs/BACKEND_API_GAPS.md`, `docs/FRONTEND_API_GAPS.md`, `docs/FRONTEND_API_INTEGRATION_SPEC.md`
- 사유: `docs/collab/TODO.md` "Event 협업 로고·이미지 및 관리자 편집 보강 체크리스트" EVENT-EXT-0~6 수행. 구현 후 프론트 코드와 정적 대조하다가 프론트 `FeedPost`가 `company`/`logoUrl`을 쓰는데 백엔드는 `companyName`/`logoImageUrl`을 쓰는 불일치를 발견 — 필드명은 기존 `thumbnailImageUrl` 명명 규칙에 맞춰 유지하고, 이미 있는 `eventToFeedPost()` 어댑터에 매핑을 추가하는 쪽으로 정리했다(코드 변경 없음, 문서만 기록).
- 관련: `EVENT-EXT-0`~`EVENT-EXT-6`, 커밋 `ea1a41f`(구현), `433993b`(테스트)

---

## 2026-08-21 — Claude — `main` (관리자 신청 목록·상세 조회 API 구현 완료)

- 변경: `GET /api/admin/applications`, `/{id}` 신규 — 소유자 무관 전체 신청 조회. `ApplicationService.listApplicationsForAdmin`/`getApplicationDetailForAdmin` 추가, 기존 `validateAdmin`과 마이페이지용 응답 DTO(`MyApplicationListItemResponse`/`MyApplicationDetailResponse`)를 그대로 재사용해 신규 DTO를 만들지 않았다. 상태 전이(사진반려/카드발급/배송추적 등)·통계는 범위 밖 — 사용자가 순수 조회 2개만 명시적으로 요청.
- 테스트: `AdminApplicationControllerTest`(7) 신규, 신청 도메인 회귀 테스트 통과 확인.
- 파일: `AdminApplicationController`(신규), `ApplicationService`, `ApplicationRepository`, 테스트, `docs/BACKEND_API_GAPS.md`, `docs/FRONTEND_API_INTEGRATION_SPEC.md`
- 사유: 사용자 확인 후 즉시 구현 — 기존 `MyApplicationController` 패턴을 거의 그대로 재사용할 수 있어 리스크가 낮다고 판단.
- 관련: 커밋 `6575d09`(구현), `c99873c`(문서)

---

## 2026-08-21 — Claude — `main` (계정 복구 아이디 찾기·비밀번호 재설정 구현 완료)

- 변경: Codex가 확정한 정책(`docs/api/auth.md` API 7·8, `arch.md` §4.1)에 맞춰 `POST /api/auth/recovery/{id,password}/{request,confirm}` 4개 엔드포인트를 구현했다. `AccountRecoveryService`가 흐름을 조정하고, 가입 인증과 공유하는 HMAC challenge/원자 rate-limit은 신규 `VerificationChallengeStore`로 분리했다(`EmailVerificationService`도 여기로 리팩터링, 외부 동작 불변 확인). access token 세션 무효화를 위해 JWT에 millisecond 정밀도 `authIssuedAtMillis` 클레임을 추가하고 `auth:access:user-revoked-after:{userId}` Redis 키와 비교하도록 `TokenSessionStore`/`JwtAuthFilter`를 고쳤다 — Redis 장애 시 기존 fail-open을 버리고 `AUTH_SESSION_VALIDATION_UNAVAILABLE(503)`로 fail-closed 응답한다. `UserService.changePassword`도 같은 revoke primitive를 적용해 다른 기기의 access token까지 실제로 무효화하도록 함께 고쳤다.
- 테스트: `AccountRecoveryServiceTest`(17) · `ClientIpResolverTest`(7) · `TokenSessionStoreSessionValidationTest`(5) 신규 작성, 전부 통과. 전체 스위트 506개 중 505개 통과(실패 1개는 이번 작업과 무관한 기존 결함 `UserApplicationFlowTest`).
- 파일: `AccountRecoveryService`, `VerificationChallengeStore`, `VerificationChallenge`, `ClientIpResolver`(신규), `EmailVerificationService`, `UserService`, `UserRepository`, `TokenSessionStore`, `JwtTokenProvider`, `JwtAuthFilter`, `SecurityConfig`, `AuthController`, `User`, `EmailType`, `ErrorCode`, DTO 7종, `application.properties`, Lua 스크립트 2종(1개 신규)
- 사유: `docs/collab/TODO.md` "계정 복구 구현 체크리스트" RECOVERY-0~2 수행. TDD 순서(실패 테스트 먼저)는 엄격히 지키지 않았고, 코드 만료·confirm 시점 계정삭제·DB실패후 세션미복구·동시요청·Redis장애 시 필터체인 HTTP 통합테스트 5가지는 전용 테스트 없이 코드만 구현했다 — TODO.md에 항목별로 명시.
- 관련: `RECOVERY-1`, `RECOVERY-2`, 커밋 `db002a7`(구현), `2d49acd`(테스트)

---

## 2026-08-21 — Codex — `main` (계정 복구 상세 정책·책임 구조 확정)

- 변경: 아이디 찾기를 일반 이메일 계정 전용으로 확정하고, 이름+정규화 전화번호가 정확히 1건일 때만 이메일 코드를 발송하도록 상세화했다. 비밀번호 재설정은 이메일만 입력받아 `requestId + code + newPassword`를 10분 안에 제출하며, 임시 비밀번호 없이 BCrypt 저장 → 코드 즉시 폐기 → refresh/access 세션 전체 무효화 → 로그인 화면 이동 순서로 확정했다. 중복 계정, 가짜 요청, Redis HMAC/Lua 원자 처리, access `revoked-after`, DB 실패·메일 실패 경계까지 문서화했다.
- 추가 확정: 가짜 요청에도 조회 전 동일 rate limit 적용, challenge의 `userId` 결속, 국제번호 정규화와 기존 DB 값 비교, 신뢰 프록시 기반 `ClientIpResolver`, millisecond `authIssuedAtMillis` claim, `AUTH_SESSION_VALIDATION_UNAVAILABLE(503)` fail-closed, 구버전 JWT 호환 경계까지 확정했다. RECOVERY-1/2는 더 이상 정책 질문 없이 구현 가능한 체크리스트로 보강했다.
- 체크리스트 재구성: 긴 외부 프롬프트 없이 작업자가 검증할 수 있도록 `TODO.md`를 RECOVERY-0(기준선·현재 충돌) → 1(아이디 찾기) → 2(비밀번호 재설정·세션) → 3(전체 검증·문서·커밋) 순서의 자체 완결형 체크리스트로 재구성했다.
- 파일: `docs/api/auth.md`, `backend/FRONTEND_API_REQUIREMENTS.md`, `docs/FRONTEND_API_GAPS.md`, `docs/BACKEND_API_GAPS.md`, `arch.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`
- 사유: 계정 복구 구현 전에 API 계약, 프론트 사용자 흐름, Service/Redis 책임과 전체 세션 무효화의 실패 경계를 하나의 기준으로 고정하기 위함. Java 구현은 수정하지 않았다.
- 관련: `RECOVERY-1`, `RECOVERY-2`

---

## 2026-08-20 — Claude — `main` (회원정보 address 수정 정책 재확정 원복 + 조회 응답에서 role 제거 + 마이페이지 "내 정보" 표시 스펙 확정)

- 변경: 같은 날 앞서 "address도 수정 가능"으로 뒤집었던 정책을 사용자가 다시 확인해 **"수정 가능 필드는 이름·전화번호뿐"으로 최종 원복**. `UserUpdateRequest`에서 `address` 필드 제거, `User.updateProfile`을 2-인자로 복원, `UserService.updateMe`의 address 검증·전달 코드 삭제. 테스트도 "address 무시됨" 검증으로 되돌림(`updateMeIgnoresAddressEvenWhenProvidedInRequestBody`). 이어서 사용자가 마이페이지 "내 정보" 조회 화면에 뜨던 "회원 유형"(일반회원/관리자) 표시가 등급제처럼 잘못 보인다고 지적 — `UserMeResponse`(`GET`/`PATCH /api/users/me` 공통 응답)에서 `role` 필드 자체를 제거(단순 화면 숨김이 아니라 DTO 스키마 변경). 관련 테스트 3곳(`UserControllerTest`/`AuthControllerSignupTest`/`UserApplicationFlowTest`)의 `$.data.role` 값 검증을 `.doesNotExist()`로 수정. 마이페이지 "내 정보" 표시 스펙 확정 — 조회는 이름·전화번호·이메일만(회원유형 표시 불가), 수정은 이름·전화번호만, role은 관리자/유저 어느 쪽이든 이 API로 수정 불가(원래도 `UserUpdateRequest`에 필드 자체가 없었음). 이 모든 내용을 `FRONTEND_API_GAPS.md` §1.9-a에 반영, 프론트 `AuthContext.tsx`가 이 응답의 `role`로 `isAdmin`을 판별하던 로직이 더는 동작하지 않는다는 점도 경고로 남김. 별건으로 마이페이지 "제작 내역"이 실 API 미연동(localStorage mock만 읽음)이라 실제 신청 후에도 빈 목록만 뜨는 문제를 코드로 재확인해 `FRONTEND_API_GAPS.md` §1.2에 상세 기록(상태 enum 불일치·날짜 포맷 문제까지 포함, 백엔드 작업은 없음).
- 파일: `UserUpdateRequest.java`, `User.java`, `UserService.java`, `UserMeResponse.java`, `UserControllerTest.java`, `AuthControllerSignupTest.java`, `UserApplicationFlowTest.java`, `docs/api/user.md`, `docs/FRONTEND_API_GAPS.md`
- 사유: 사용자가 "수정에는 이름과 전화번호만 가능해야 한다", "회원유형(등급제처럼 보이는 표시)은 조회 화면에 아예 없어야 한다"고 재확인 — 같은 날 두 번 바뀐 address 정책 + role 제거까지 겹쳐 혼동 방지를 위해 코드·문서·테스트 전부 명시적으로 정리.
- 관련: `docs/collab/HANDOFF.md` 2026-08-20 항목

---

## 2026-08-20 — Claude — `main` (EC2 배포 준비 중 발견한 버그 일괄 수정)

- 변경: 사용자가 실제 EC2 배포를 진행하면서 실사용 중 발견한 문제 7건을 그때그때 수정. ① 단체신청 엑셀 템플릿 유효성검사 수식의 잘못된 선행 `=`(한셀에서 정상 입력도 거부되던 버그) ② 고등학교 단체신청 학번·학과 정책을 `BulkExcelParser`가 `schoolType`을 몰라 개인 신청과 어긋나 있던 것 통일(추가했다가 사용자가 프론트 근거로 재정정해 원복) ③ 회원정보 `address` 수정 정책 재정정(2026-08-08 확정을 뒤집음) ④ `User.createLocalUser`가 phone을 생성 시점에 받도록 리팩터링 ⑤ `docker-compose.yml`에 Postgres 추가(H2 인메모리로 데이터 소실되던 문제) + `MAIL_HOST`/`PORT`/`FROM` 누락 수정 ⑥ 루트 `.gitignore`에 `.env` 누락 발견해 추가(시크릿 커밋 위험 차단) ⑦ 프론트-백엔드 갭 재대조 중 `schoolName` 미연동으로 학생증 신청이 전부 깨진 회귀 발견.
- 파일: `BulkExcelParser.java`, `ApplicationService.java`, `User.java`, `UserService.java`, `UserUpdateRequest.java`, `docker-compose.yml`, `.gitignore`, `.env.example`, `DOCKER.md`, `docs/api/user.md`, `docs/FRONTEND_API_GAPS.md`, `outputs/bulk-excel-templates-20260818/*.xlsx`, 관련 테스트 다수
- 사유: 문서·설계 검토가 아니라 **실제 배포·실사용 과정에서 발견된 버그**라는 점이 이전 항목들과 다름 — 엑셀 템플릿은 실제 사용자(사용자님)가 한셀로 채우다 발견, 고등학교 정책은 사용자가 실제 신청 흐름을 검토하다 발견, `.env`/docker-compose 문제는 로컬에서 SMTP·S3·OAuth를 실제로 테스트하다 발견.
- 관련: `docs/collab/HANDOFF.md` 2026-08-20 항목(전체 상세), `docs/collab/TODO.md` "배포 준비 + 버그 수정 다발" 행, 커밋 `20ee58e`~`30a1a52` 구간 전부(총 11개 커밋)

---

## 2026-08-19 — Claude — `main` (학생증 신청 schoolName 필드 추가, SCHOOLNAME-1 완료)

- 변경: 프론트-백엔드 갭 재점검(`docs/BACKEND_API_GAPS.md` P1-4) 중 발견한 "학교명 저장 필드 없음" 항목을 정책 확정 후 구현. `Application`에 `school_name` 컬럼 신규 추가(개인·단체 공통, 학생증 전용, `orientation`/`schoolType`과 동일하게 신청서 전체에 1개). `UNIVERSITY`/`HIGH_SCHOOL` 둘 다 필수(학번/학과와 달리 대학교 전용 조건 없음), 트림 후 5~20자·한글/영문/숫자/공백만 허용, 비학생증이면 있으면 거절.
- 파일: `Application.java`, `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `ApplicationFactory.java`, `ApplicationPersistenceService.java`, `ApplicationService.java`, `ApplicationFactoryTest.java`, `ApplicationServiceTest.java`, `ApplicationServiceBulkTest.java`, `ApplicationServiceUploadCompensationTest.java`, `docs/specs/application/{requirements,data-model,api}.md`
- 사유: 사용자 요청으로 프론트-백엔드 API 갭을 다시 확인하던 중 Application 도메인의 학교명 필드 미저장 갭을 짚어 정책 확정(대학교/고등학교 둘 다 필수, DB nullable+서비스 레벨 강제, 5~20자, 한글/영문/숫자/공백만) 후 체크리스트 기반으로 구현
- 관련: `docs/collab/TODO.md` "학생증 신청에 학교명(schoolName) 필드 추가" 행. 커밋 `575f6c0`(코드), `6653fd2`(문서)

---

## 2026-08-19 — Claude — `main` (회원탈퇴 WITHDRAW-4 구현 + 정책 변경 전체 완료 마무리)

- 변경: `docs/collab/user.md` §19 체크리스트의 마지막 단위 WITHDRAW-4 구현 — `RefreshTokenSessionRepository.deleteByUserId`/`ApplicationDailyLimitRepository.deleteByUserId` 신규, `TokenSessionStore.deleteUserSessions()`(하드 삭제 전용, 기존 `invalidateUserSessions`의 revoke와 구분), `ApplicationDailyLimitService.deleteAllForUser()`(arch.md §5.1 "다른 모듈 Repository 직접 호출 금지" 원칙에 따라 `UserService`가 이 공개 메서드를 거치도록 함), `UserService.withdraw()`를 실제 하드 삭제(세션 무효화→액세스 토큰 블랙리스트→RefreshTokenSession 삭제→ApplicationDailyLimit 삭제→User 삭제)로 전면 재작성. 이제 어디서도 안 쓰이는 `ErrorCode.ALREADY_WITHDRAWN`과 빈 placeholder였던 `User.withdraw()` 엔티티 메서드도 함께 삭제. 이걸로 확정 정책(WITHDRAW-1~4) 전부 구현 완료 — §19 체크리스트 전 항목 `[x]` 처리, `docs/collab/TODO.md` 행을 🔵→✅로 변경, `docs/api/user.md` API 4를 실제 구현 기준으로 최종 정리, `arch.md`의 "구현 전" 표시를 "구현 완료"로 갱신.
- 파일: `domain/user/repository/RefreshTokenSessionRepository.java`, `domain/application/repository/ApplicationDailyLimitRepository.java`, `domain/application/service/ApplicationDailyLimitService.java`, `infra/security/TokenSessionStore.java`, `domain/user/service/UserService.java`, `domain/user/entity/User.java`, `common/exception/ErrorCode.java`, 테스트(`UserServiceWithdrawTest` 신규, `UserControllerTest` 갱신), 문서(`docs/collab/user.md`/`TODO.md`, `docs/api/user.md`, `arch.md`)
- 테스트 결과: 이번 단위 신규 4개(`UserServiceWithdrawTest`) + `UserControllerTest` 갱신분 통과. 마지막 단위라 RULES.md §8대로 전체 스위트 실행 — 462개 중 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing) 1건만 실패, 회귀 없음.
- 사유: 체크리스트(§19) 마지막 단위 구현 + 회원탈퇴 정책 변경 작업 전체 마무리.
- 관련: TODO "회원탈퇴 정책 변경" — 완료. 남은 항목은 §17.2/§17.3(법무 확인 대상, 코드 무관)뿐.

---

## 2026-08-19 — Claude — `main` (회원탈퇴 WITHDRAW-3B 구현 — UserStatus 완전 제거)

- 변경: `docs/collab/user.md` §19 체크리스트의 WITHDRAW-3B 단위 구현(WITHDRAW-3에서 분리된 단위). `common/enums/UserStatus.java` 삭제, `User`의 `status`/`withdrawalRequestedAt`/`anonymizedAt` 필드와 `isWithdrawn()` 삭제(`withdraw()`는 WITHDRAW-4에서 실제 하드 삭제가 채워질 빈 자리로 유지), `UserRepository`의 익명화 스케줄러 전용 쿼리 메서드 삭제. 이 필드 제거가 걸쳐있던 3개 도메인 호출부도 함께 정리: `UserService.findEligibleApplicationUser()`·`UserService.login()`·`UserService.withdraw()`의 상태 체크 제거, `ApplicationService.validateAdmin()`의 `ACTIVE` 체크 제거(role 체크는 유지), `ReviewEligibilityService.validateForCreate()`의 `isWithdrawn()` 체크 제거(원래 목적이었던 "탈퇴 시 현재 토큰만 블랙리스트되는 허점 방어"는 하드 삭제로 `existsById` 자체가 실패하게 되어 근본적으로 해소됨).
- 파일: `common/enums/UserStatus.java`(삭제), `domain/user/{entity/User,repository/UserRepository,service/UserService}.java`, `domain/application/service/ApplicationService.java`, `domain/review/service/ReviewEligibilityService.java`, 테스트 6개(`UserTest`/`UserServiceLoginTest`/`UserControllerTest`/`ReviewEligibilityServiceTest`/`ApplicationServiceTest`/`ApplicationServiceBulkTest` — "탈퇴한 사용자" 시나리오를 재현하던 테스트들을 제거, 이제 이 계층에서는 재현 불가능해졌기 때문)
- 테스트 결과: RULES.md §8대로 영향 범위(User/Application/Review 도메인 전체+`UserApplicationFlowTest`) 180개 실행 — pre-existing 실패 1건(무관)만, 나머지 전부 통과.
- 사유: 체크리스트(§19) 순서대로 구현. 착수 전 전체 참조 검색으로 확인한 대로 3개 도메인에 걸친 작업이었음(예상대로).
- 관련: TODO "회원탈퇴 정책 변경" — 다음 단위 WITHDRAW-4(핵심, 실제 하드 삭제 구현)

---

## 2026-08-19 — Claude — `main` (회원탈퇴 WITHDRAW-3 구현 — 익명화 스케줄러 제거)

- 변경: `docs/collab/user.md` §19 체크리스트의 WITHDRAW-3 단위 구현. `UserWithdrawalScheduler` 삭제, `UserService.anonymizeExpiredWithdrawnUsers()`+`WITHDRAWAL_GRACE_PERIOD_DAYS` 상수 삭제, `User.anonymize()`/`isRestorable()`/`restore()` 삭제(WITHDRAW-1·2에서 호출부를 이미 정리해둬서 컴파일 안전). 이 스케줄러만을 위해 존재하던 `UserServiceTest.java`는 테스트 대상 자체가 사라져 파일째 삭제. `UserTest.java`도 `isRestorable`/`restore`/`anonymize` 관련 테스트 3개 제거.
- 파일: `domain/user/entity/User.java`, `domain/user/service/UserService.java`, `domain/user/scheduler/UserWithdrawalScheduler.java`(삭제), `UserServiceTest.java`(삭제), `UserTest.java`
- 테스트 결과: RULES.md §8대로 영향 범위(`domain.user.*`, `UserController*`, `AuthController*`, `UserApplicationFlowTest`) 58개 실행 — pre-existing 실패 1건(무관)만, 나머지 전부 통과.
- 사유: 체크리스트(§19) 순서대로 구현.
- 관련: TODO "회원탈퇴 정책 변경" — 다음 단위 WITHDRAW-3B

---

## 2026-08-19 — Claude — `main` (회원탈퇴 WITHDRAW-2 구현 — 일반 로그인 자동복구 제거)

- 변경: `docs/collab/user.md` §19 체크리스트의 WITHDRAW-2 단위 구현. `UserService.login()`에서 탈퇴 유예기간 판정+`isRestorable()`/`restore()` 자동 복구 분기를 제거 — 탈퇴한(`WITHDRAWN`) 계정은 비밀번호가 맞아도 예외 없이 `INVALID_CREDENTIALS`로 거절한다. 이에 딸려있던 `restored` 응답 필드(`LoginResult`/`LoginResponse`/`AuthController`)도 전부 제거 — 항상 `false`만 나오는 죽은 필드가 되므로 체크리스트에 명시된 "`restored: true` 응답 필드 폐지"를 이 단위에서 함께 처리했다.
- 파일: `domain/user/service/{UserService,LoginResult}.java`, `domain/user/dto/LoginResponse.java`, `api/AuthController.java`, 테스트 2개(`UserServiceLoginTest`: grace-period 테스트 2개를 "탈퇴 계정 항상 거절" 테스트 1개로 교체, `AuthControllerLoginTest`: `restored` assertion 제거)
- 테스트 결과: RULES.md §8대로 영향 범위(`domain.user.*`, `AuthController*`, `UserController*`, `UserApplicationFlowTest`) 64개 실행 — pre-existing 실패 1건(무관)만, 나머지 전부 통과.
- 사유: 체크리스트(§19) 순서대로 구현.
- 관련: TODO "회원탈퇴 정책 변경" — 다음 단위 WITHDRAW-3

---

## 2026-08-19 — Claude — `main` (회원탈퇴 WITHDRAW-1 구현 — OAuth 자동복구 제거)

- 변경: `docs/collab/user.md` §19 체크리스트의 WITHDRAW-1 단위 구현. `OAuth2SuccessHandler.onAuthenticationSuccess()`에서 기존 회원 재로그인 시 `user.isRestorable()`/`user.restore()`를 호출해 탈퇴 유예기간 내 자동 복구하던 분기를 제거. `User` 엔티티의 `isRestorable()`/`restore()` 메서드 자체는 아직 남아있음(WITHDRAW-3에서 제거 예정, 이번 단위는 호출부만 정리 — 컴파일 의존성 순서).
- 파일: `infra/security/OAuth2SuccessHandler.java`
- 테스트 결과: 이 클래스를 직접 테스트하는 전용 테스트가 없음(OAuth2AuthenticationToken 목킹 인프라 자체가 프로젝트에 없음, `UserApplicationFlowTest`도 "OAuth2SuccessHandler와 동일한 코드 경로"라는 주석과 달리 이 클래스를 직접 호출하지 않고 `User.createOAuthUser`+`issueLoginTokens`만 재현 — 이번 변경이 어떤 기존 테스트에도 걸리지 않음을 확인). RULES.md §8대로 영향 범위(`domain.user.*`, `UserController*`, `AuthController*`, `UserApplicationFlowTest`) 65개 실행 — pre-existing 실패 1건(`UserApplicationFlowTest.fullUserApplicationFlow`, 무관)만, 나머지 전부 통과.
- 사유: 체크리스트(§19) 순서대로 구현 착수.
- 관련: TODO "회원탈퇴 정책 변경" — 다음 단위 WITHDRAW-2

---

## 2026-08-19 — Claude — `main` (회원가입 정보 보유기간 정합성 해소 — "상품 수령 후 6개월" vs 즉시 탈퇴 하드 삭제)

- 변경: 사용자가 확인해준 개인정보처리방침 고지 보유기간 5개 항목(본인확인정보=즉시파기, 신청정보/결제정보=상품수령후6개월, 상담정보=상담일로부터6개월, 회원가입정보=상품수령후6개월)을 `docs/collab/user.md` §1.1에 표로 정리. 이어서 남아있던 미해결 항목("회원가입 정보 6개월" 문구와 즉시 하드 삭제 정책의 정합성)을 사용자가 확정: **6개월은 탈퇴하지 않고 활동하는 회원의 기본 보유기간이고, 회원탈퇴 시에는 그 기간을 기다리지 않고 지체 없이 파기한다** — 탈퇴/보유기간만료 중 먼저 도래하는 사유로 파기되는 구조.
- 파일: `docs/collab/user.md`(§1.1 표 추가, §17.1 해소 표시), `arch.md` §4.1(동일 해소 반영), `docs/collab/TODO.md`·`docs/collab/HANDOFF.md`(미해결 표시를 해소로 갱신)
- 사유: 사용자가 실제 개인정보처리방침 문구를 확인해주고, 뒤이어 두 정책(보유기간 상한 vs 탈퇴 즉시삭제)의 관계를 명시적으로 확정.
- 관련: TODO "회원탈퇴 정책 변경"

---

## 2026-08-19 — Claude — `main` (회원탈퇴 정책 변경 — 소프트 삭제 폐지, 즉시 하드 삭제로 확정. 문서만, 코드 미착수)

- 변경: 사용자가 소프트 삭제(7일 유예+익명화) 정책을 폐지하고 즉시 하드 삭제로 바꾸려는 요청에 따라, 먼저 코드베이스 전체를 대상으로 스코프 분석을 수행했다 — `User.id`를 참조하는 7개 테이블(Application/ApplicationDailyLimit/Board/RefreshTokenSession/Review/Inquiry/AdminActivityLog) 전수 확인, `arch.md` §5.1(FK 없는 Long 참조 원칙)이 전역 적용돼 있어 `User` row 하드 삭제가 DB 레벨 문제를 일으키지 않음을 확인, Application/Review/Board/Inquiry 전부 이름·이메일·전화 스냅샷 저장 패턴이라 화면 표시도 깨지지 않음을 확인. 이 분석 결과를 바탕으로 사용자가 1차 정책표를 확정했다. 이후 `git status`에서 사용자가 별도로 작성해둔 상세 정책 원본 `docs/collab/user.md`("회원정보·개인정보 보유·탈퇴·파기 정책", 미커밋 상태로 발견)를 확인 — 사용자에게 처리 방식을 물어 "이 문서를 상위 소스로 삼아 arch.md 등을 재조정"으로 확정받고, 그 기준으로 이미 작성한 문서들을 다시 보강했다: 상품 수령일 기산점(신청/결제 개인정보는 회원탈퇴와 무관하게 "상품 수령 후 6개월" 별도 기산), 법정대리인(만 14세 미만) 개인정보 조건부 조항, `RefreshTokenSession`은 revoke가 아니라 하드 삭제, Review 작성자 표시명 익명화는 "권장"일 뿐 미확정임을 명시, 파기 배치의 감사 로그 형식(파기 유형/건수/일시/결과만 남기고 원문 재저장 금지), 개인정보처리방침 문안 자체의 미해결 사항(§17: "회원가입 정보 상품수령후 6개월" 문구와 즉시 하드삭제 정책의 정합성 미해결, 비밀번호 제3자제공 문구 오류 의심 등).
- 파일(전부 문서, 코드 변경 없음): `docs/collab/user.md`(신규, source of truth로 커밋), `arch.md` §4.1(User 모듈, "탈퇴 정책" 표 신설+보강)·§4.7(Review 모듈)·§11(스케줄러 인벤토리), `backend/FRONTEND_API_REQUIREMENTS.md` §3(회원탈퇴 정책 절 재작성+보강), `docs/api/user.md`(API 4·"회원탈퇴 관련 로직 변경" 절 폐지 표시+source of truth 링크, User 도메인 정리 표·TODO 갱신), `docs/specs/inquiry/requirements.md`(§⑥ 근거 정정), `docs/specs/review/data-model.md`(user_id 컬럼 설명 보강), `docs/collab/TODO.md`(신규 대기 항목 추가+보강).
- 사유: 사용자가 회원정보 관리기간 정책 도입을 함께 고려해 소프트 삭제 정책을 재검토 요청. 스코프 분석 → 1차 정책 확정 → 상세 원본 문서 발견·확인 → 문서 재조정까지 이번 세션에서 완료. **실제 코드 구현(하드 삭제 로직, 관련 테스트 재작성)은 아직 착수하지 않았다** — 다음 세션 작업 대상.
- 관련: TODO "회원탈퇴 정책 변경" 신규 항목

---

## 2026-08-19 — Claude — `main` (Inquiry INQUIRY-5 구현 + 도메인 전체 완료 마무리)

- 변경: `docs/specs/inquiry/requirements.md` §⑨ 체크리스트의 마지막 단위 INQUIRY-5 구현 — `InquiryStatusUpdateRequest`, `InquiryService.changeStatus(inquiryId, status)`(답변 유무와 무관하게 상태만 변경, `answer=null`인 채 `COMPLETED` 허용), `InquiryAdminController`에 `PATCH /{inquiryId}/status` 추가. 이걸로 확정 API 6개 전부 구현 완료 — §⑨ 체크리스트 전 항목 `[x]` 처리, `docs/collab/TODO.md` 행을 🔵→✅로 변경, `docs/FRONTEND_API_GAPS.md` §1.3을 "BLOCKED"에서 "PARTIAL(연동 가능, privacyConsent 프론트 반영 필요)"로 갱신.
- 파일: `domain/inquiry/{dto/InquiryStatusUpdateRequest,service/InquiryService}.java`, `api/InquiryAdminController.java`, 테스트 2개(`InquiryServiceTest`/`InquiryAdminControllerTest` 확장), `docs/specs/inquiry/requirements.md`, `docs/collab/TODO.md`, `docs/FRONTEND_API_GAPS.md`
- 테스트 결과: 이번 단위 신규 5개 전부 통과. RULES.md §8 정책대로 체크리스트 전 단위(기능 묶음) 완료 시점이라 전체 스위트 실행 — 472개 중 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing) 1건만 실패, 회귀 없음.
- 사유: 체크리스트(§⑨) 마지막 단위 구현 + 도메인 완료 마무리 문서화.
- 관련: TODO "Inquiry(1:1 문의) 도메인" — 완료. 남은 오픈 아이템은 프론트 `privacyConsent` 반영(§1.3)과 §⑧ 파기 배치(별도 인프라 작업)뿐.

---

## 2026-08-19 — Claude — `main` (Inquiry INQUIRY-4 구현 — 답변 등록 API + 이메일 알림)

- 변경: `docs/specs/inquiry/requirements.md` §⑨ 체크리스트의 INQUIRY-4 단위 구현. `EmailType.INQUIRY_ANSWERED` 추가, `InquiryAnswerRequest`(§⑦ `answer` 최대 5000자), `InquiryService.answer(inquiryId, answerText)` — 갱신 직전 `inquiry.getAnswer() == null`로 최초 등록 여부를 판정해 최초 등록일 때만 `TransactionSynchronizationManager.registerSynchronization`으로 커밋 이후 이메일 발송을 등록(Board의 `deleteFilesAfterCommit`과 동일한 after-commit 패턴), 답변 "수정"(이미 answer가 있던 상태에서 재저장)이면 이메일 스킵, `EmailSender`가 예외를 던져도 로깅만 하고 삼켜 답변 저장 자체는 유지(best-effort). `InquiryAdminController`에 `PATCH /{inquiryId}/answer` 추가.
- 파일: `common/enums/EmailType.java`, `domain/inquiry/{dto/InquiryAnswerRequest,service/InquiryService}.java`, `api/InquiryAdminController.java`, 테스트 2개(`InquiryServiceTest` 확장, `InquiryAdminControllerTest` 확장)
- 테스트 결과: 신규 10개(서비스 5 — 답변 저장+상태전이, 최초 등록 시 이메일 발송, 수정 시 이메일 미발송, 이메일 실패해도 답변 유지, 미존재 404 / 컨트롤러 3 — 성공, 공백 답변 400, USER 토큰 403) 전부 통과. RULES.md §8 신규 정책에 따라 이번 단위는 Inquiry 범위 테스트만 실행(누적 31개 전부 통과) — 공통 인프라(SecurityConfig 등) 변경이 없어 전체 스위트는 생략.
- 사유: 체크리스트(§⑨) 순서대로 구현.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현" — 다음 단위 INQUIRY-5(마지막)

---

## 2026-08-19 — Claude — `main` (RULES.md §8 테스트 범위 정책 추가)

- 변경: 작업 단위별 테스트 범위 규칙 신설 — 단위마다 변경 기능의 직접 테스트+영향받는 공통 영역 테스트만 우선 실행(`--tests`로 범위 좁힘), 단순 도메인 내부 변경마다 전체 스위트 반복 실행 금지, 커밋 전 `git diff`로 예상 밖 변경 파일 점검, 전체 스위트는 기능 묶음 완료·공통 인프라 변경·push 직전 최종 회귀 검증 시에만 실행, 실행 시 §9대로 로그 파일 리다이렉트 후 종료 코드·전체 수·신규 실패만 확인.
- 파일: `docs/collab/RULES.md` §8
- 사유: INQUIRY-1~3 진행 중 매 단위마다 전체 스위트(450+ 테스트)를 반복 실행해온 것이 비효율적이라는 사용자 피드백 반영 — 앞으로 INQUIRY-4/5부터 적용.
- 관련: 없음(프로세스 규칙)

---

## 2026-08-19 — Claude — `main` (Inquiry INQUIRY-3 구현 — 관리자 문의 목록/상세 API)

- 변경: `docs/specs/inquiry/requirements.md` §⑨ 체크리스트의 INQUIRY-3 단위 구현. `InquiryRepository.findAllByOrderByCreatedAtDesc` 추가, `InquiryService.listAdmin`/`getAdminDetail`(소유권 개념 없이 404만), `InquiryAdminController`(`GET /api/admin/inquiries`, `/{id}`) 신규 — `SecurityConfig`의 `/api/admin/**` → `hasRole("ADMIN")`가 이미 라우트 레벨로 강제하므로 서비스·컨트롤러에서 별도 권한 재확인 없음(Board 관리자 컨트롤러와 동일 원칙). TDD로 진행 — 컴파일 실패(red) 확인 후 최소 구현.
- 파일: `domain/inquiry/{repository/InquiryRepository,service/InquiryService}.java`, `api/InquiryAdminController.java`, 테스트 2개(`InquiryServiceTest` 확장, `InquiryAdminControllerTest` 신규)
- 테스트 결과: 신규 7개(서비스 3, 컨트롤러 4 — 전체 목록, USER 토큰 403, 상세, 미존재 404) 전부 통과. 누적 신규 테스트 23개, 전체 스위트 459개 중 기존과 동일하게 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing) 1건만 실패(회귀 없음).
- 사유: 체크리스트(§⑨) 순서대로 구현.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현" — 다음 단위 INQUIRY-4

---

## 2026-08-19 — Claude — `main` (Inquiry INQUIRY-2 구현 — 내 문의 목록/상세 API)

- 변경: `docs/specs/inquiry/requirements.md` §⑨ 체크리스트의 INQUIRY-2 단위 구현. `ErrorCode.INQUIRY_NOT_FOUND` 추가, `InquiryRepository.findAllByUserIdOrderByCreatedAtDesc` 추가, `InquiryListItemResponse`/`InquiryDetailResponse`(내 목록·관리자 목록/상세 공용으로 설계), `InquiryService.listMine`/`getMineDetail`(존재 확인 404 → 소유권 확인 403 순서로 분리, `MyApplicationController` 선례와 동일), `MyInquiryController`(`GET /api/my/inquiries`, `GET /api/my/inquiries/{id}`) 신규. TDD로 진행 — 컴파일 실패(red) 확인 후 최소 구현.
- 파일: `common/exception/ErrorCode.java`, `domain/inquiry/{repository/InquiryRepository,dto/InquiryListItemResponse,dto/InquiryDetailResponse,service/InquiryService}.java`, `api/MyInquiryController.java`, 테스트 2개(`InquiryServiceTest` 확장, `MyInquiryControllerTest` 신규)
- 테스트 결과: 신규 9개(서비스 4, 컨트롤러 5) 전부 통과. 누적 신규 테스트 16개, 전체 스위트 452개 중 기존과 동일하게 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing) 1건만 실패(회귀 없음).
- 사유: 체크리스트(§⑨) 순서대로 구현.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현" — 다음 단위 INQUIRY-3

---

## 2026-08-19 — Claude — `main` (Inquiry INQUIRY-1 구현 — 문의 등록 API)

- 변경: `docs/specs/inquiry/requirements.md` §⑨ 체크리스트의 INQUIRY-1 단위 구현. `Inquiry` 엔티티(`create`/`isOwnedBy`/`answer`/`changeStatus`, `answer`/`changeStatus`는 이후 단위에서 사용), `InquiryRepository`(현재는 `JpaRepository` 그대로, 목록 조회 메서드는 INQUIRY-2/3에서 추가 예정), `InquiryCreateRequest`(§⑦ Validation 전부 + `privacyConsent` `@AssertTrue`), `InquiryCreateResponse`, `InquiryService.create`, `InquiryController`(`POST /api/inquiries`, USER 권한) 신규. TDD로 진행 — 실패하는 테스트(`InquiryService`/`InquiryController` 미존재로 컴파일 실패) 먼저 확인 후 최소 구현.
- 파일: `common/enums/InquiryStatus.java`, `domain/inquiry/{entity/Inquiry,repository/InquiryRepository,dto/InquiryCreateRequest,dto/InquiryCreateResponse,service/InquiryService}.java`, `api/InquiryController.java`, 테스트 3개(`InquiryTest`, `InquiryServiceTest`, `InquiryControllerTest`)
- 테스트 결과: 신규 7개 전부 통과, 전체 스위트 443개 중 기존과 동일하게 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing, TERMS_NOT_AGREED) 1건만 실패(회귀 없음).
- 사유: 체크리스트(§⑨) 순서대로 구현 착수.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현" — 다음 단위 INQUIRY-2

---

## 2026-08-19 — Claude — `main` (Inquiry 구현 착수 체크리스트 + 6개 잔여 정책 확정)

- 변경: 구현 착수 전 5단계 체크리스트(INQUIRY-1~5, 각 단위 독립 커밋 대상) 신설. 그와 별개로 사용자가 제시한 6개 정책을 반영: (1) 탈퇴 회원 데이터는 `Inquiry.userId`가 FK 없는 순수 참조라 cascade 없이 자동 보존됨을 확인·문서화, (2) 6개월 파기는 `status`와 무관하게 `createdAt` 단일 기준임을 명확화, (3) 답변 "수정" 시에는 이메일 재발송하지 않음(최초 등록 여부는 갱신 직전 `answer == null` 판정으로 구분), (4) 탈퇴→재가입 시 신규 `userId`라 과거 문의 자동 연결 안 됨(설계상 자연히 보장), (5) 전화상담 등으로 `status=COMPLETED`이면서 `answer=null`인 상태도 유효 — 두 필드 간 불변식을 걸지 않음, (6) 개인정보 동의 체크박스를 서버 미검증 UI 전용 게이트로 둔 기존 설계가 API 직접 호출로 우회 가능하다는 문제 제기를 받아 `privacyConsent: true` 서버 검증(`@AssertTrue`)을 신규 추가 — 단, 이건 `InquiryPage.tsx`가 현재 이 필드를 전혀 전송하지 않아 프론트 수정이 별도로 필요한 새 의존성이라 `FRONTEND_API_GAPS.md` §1.3에 명시적으로 기록.
- 파일: `docs/specs/inquiry/requirements.md`(§⑥ 정책 4건 추가, §⑤/§⑦/§⑨ 갱신, §⑨ 구현 체크리스트 신설), `docs/FRONTEND_API_GAPS.md` §1.3(privacyConsent 프론트 의존성 기록)
- 사유: 구현 착수 직전 마지막 정책 라운드 + 체크리스트 없이 코드부터 작성했던 절차 실수를 바로잡음(사용자 지적).
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현"

---

## 2026-08-19 — Claude — `main` (Inquiry Validation 규칙 + 개인정보 보유기간 6개월 정책 확정)

- 변경: Inquiry 필드별 Bean Validation 규칙(category/name/email/phone/title/content/answer) 확정, `name`은 `ApplicationCreateRequest.Applicant.name` 관례(max 100)를 따름. `phone`은 사용자가 언급한 공용 `ValidPhone` 어노테이션이 실제로는 존재하지 않음을 코드로 확인해 정정 — 기존 `SignupRequest`/`UserUpdateRequest`가 쓰는 인라인 `@Pattern(regexp = "^[0-9\\-]{9,20}$")`을 그대로 재사용하는 걸로 확정. 개인정보 보유기간은 `createdAt + 6개월 → 파기 대상`으로 확정(파기 배치 구현은 `docs/api/user.md`의 "완전탈퇴 배치 스케줄러"와 묶어 이후 진행, 이번엔 정책만). 저장소의 실제 개인정보처리방침 텍스트(`frontend/src/data/policies.ts`)엔 "6개월" 구체 문구가 없고 placeholder임을 확인해 문서에 남김.
- 파일: `docs/specs/inquiry/requirements.md`(§⑦ Validation 규칙, §⑧ 개인정보 보유기간 신설)
- 사유: Inquiry 도메인 착수 전 정책을 계속 정하는 running 작업의 일부.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현"

---

## 2026-08-19 — Claude — `main` (Inquiry 답변 이메일 알림 정책 확정)

- 변경: 관리자 답변 등록(`PATCH /api/admin/inquiries/{id}/answer`)이 인앱 저장(`AdminPage.tsx`의 답변 textarea·저장 버튼)뿐 아니라 문의자에게 이메일 알림도 발송하도록 확정. 기존 `infra/mail/EmailSender`+`common/enums/EmailType` 인프라(`EmailVerificationService`가 이미 사용 중인 패턴)를 재사용해 `EmailType.INQUIRY_ANSWERED`를 추가하는 방향으로 정했고, 신청 도메인 전용이라 재사용 부적합한 `EmailLog`(applicationId 필수, 미사용 스캐폴딩)는 채택하지 않았다. 이메일 발송은 답변 저장 트랜잭션과 분리된 best-effort로 처리(발송 실패가 답변 저장을 되돌리지 않음)하기로 확정.
- 파일: `docs/specs/inquiry/requirements.md`(§④ 답변 API 행, §⑤ `PATCH .../answer` 처리 흐름 신설, §⑥ 정책 항목 추가)
- 사유: "관리자 답변은 메일이니 API 불필요"라는 초기 제안이 실제 프론트 코드(인앱 답변 저장 UI)와 맞지 않아 정정하는 과정에서, 별도 이메일 알림 자체는 필요하다는 사용자 확인을 반영.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현"

---

## 2026-08-19 — Claude — `main` (Inquiry 남은 정책 4건 해소 + InquiryCategory enum 선반영)

- 변경: `docs/specs/inquiry/requirements.md`의 미결정 4건을 프론트 코드 확인 후 전부 확정 — (1) `category`는 enum으로 강제(신규 `InquiryCategory.java`, `@JsonValue`/`@JsonCreator`로 프론트의 한글 값을 그대로 매핑해 프론트 수정 불필요 — 기존 `LookupMethod`와 동일 패턴), (2) 일일 등록 횟수 제한 없음, (3) 재문의(추가 질문) 흐름 없음(`InquiryDetailPage.tsx`에 대응 UI 없음 확인), (4) 첨부파일 미지원(`InquiryPage.tsx` 폼에 파일 입력 없음 확인). 이로써 Inquiry 도메인의 남은 정책 결정 사항이 없어졌다.
- 파일: `common/enums/InquiryCategory.java`(신규, 도메인 나머지는 아직 없음), `docs/specs/inquiry/requirements.md`, `docs/collab/TODO.md`
- 사유: Inquiry 도메인 착수 전 정책을 계속 정하는 running 작업의 일부.
- 관련: TODO "Inquiry(1:1 문의) 도메인 신규 구현"

---

## 2026-08-19 — Claude — `main` (프론트 변경분 되돌림 — 백엔드 전용 원칙 확정)

- 변경: 사용자가 "백엔드만 수정, 프론트엔드는 절대 수정하지 않는다"는 원칙을 명확히 함에 따라, `f19830e`에서 함께 완화했던 `frontend/src/pages/SignupPage/SignupPage.tsx`의 비밀번호 검증(8~72자만)을 원래 규칙(8~64자+영문/숫자/특수문자 조합)으로 되돌렸다. 백엔드 정책(AUTH-4, 8~72자·복잡도 규칙 없음)은 그대로 유지 — 프론트 자체 검증 규칙만 되돌림.
- 파일: `frontend/src/pages/SignupPage/SignupPage.tsx`
- 사유: 프론트/백엔드 수정 범위를 명확히 분리하는 사용자 지시. 앞으로 프론트 쪽 필요사항은 코드 수정 대신 `docs/FRONTEND_API_GAPS.md` 등 문서로만 전달한다.
- 관련: TODO `AUTH-4`

---

## 2026-08-19 — Claude — `main` (AUTH-6: 비밀번호 변경 API — 일반 이메일 인증·로그인 그룹 전체 완료)

- 변경: `PATCH /api/users/me/password` 구현. 현재 비밀번호 확인 후 새 비밀번호로 교체하고, OAuth 전용 계정은 API 자체를 차단한다. **정책에 없던 "세션 처리" 확인 요청 → 사용자가 전체 세션 무효화로 확정**(withdraw()와 동일 패턴 — 다른 기기 세션까지 강제 로그아웃, 이 요청의 accessToken도 블랙리스트). 이로써 AUTH-1~6·PW-1·MAIL-1·SIGNUP-1/2·RATE-1 전부 완료 — 일반 이메일 회원가입·인증·로그인·계정관리 그룹이 통째로 끝났다.
- 파일: `api/UserController.java`, `domain/user/service/UserService.java`, `domain/user/entity/User.java`(`changePasswordHash` 추가), `domain/user/dto/PasswordUpdateRequest.java`(신규), `common/exception/ErrorCode.java`(`CURRENT_PASSWORD_MISMATCH`/`PASSWORD_CHANGE_NOT_ALLOWED` 추가), `UserControllerChangePasswordTest.java`(신규)
- 테스트: 신규 5개 전부 통과. 전체 스위트 436개 중 `UserApplicationFlowTest` 1건만 실패(기존 결함, 회귀 아님).
- 사유: AUTH-3에 이어 이 그룹의 마지막 단위 진행.
- 관련: TODO `AUTH-6`

---

## 2026-08-19 — Claude — `main` (AUTH-3: 이메일 중복 확인 API)

- 변경: `POST /api/auth/email/check` 구현. 정규화된 이메일로 `existsByEmail` 조회 후 boolean만 반환(계정 상세 비노출). OAuth 계정도 같은 UNIQUE 제약을 공유해 provider 구분 없이 중복 판정된다.
- 파일: `api/AuthController.java`, `infra/security/SecurityConfig.java`, `domain/user/service/UserService.java`, `domain/user/repository/UserRepository.java`(`existsByEmail` 추가), `domain/user/dto/EmailCheckRequest.java`/`EmailCheckResponse.java`(신규), `AuthControllerEmailCheckTest.java`(신규)
- 테스트: 신규 4개 전부 통과. 전체 스위트 431개 중 `UserApplicationFlowTest` 1건만 실패(기존 결함, 회귀 아님).
- 사유: AUTH-5 완료 후 남은 두 단위(AUTH-3/AUTH-6) 중 독립적으로 진행 가능한 AUTH-3 먼저 진행.
- 관련: TODO `AUTH-3`

---

## 2026-08-19 — Claude — `main` (AUTH-5: 이메일 로그인 API + 소프트탈퇴 자동복구)

- 변경: `POST /api/auth/login` 구현. `LoginAttemptLimiter`로 잠금 확인 → 계정없음/OAuth전용계정/비밀번호불일치를 전부 `INVALID_CREDENTIALS`로 동일 응답 → 탈퇴 계정은 `withdrawalRequestedAt`을 직접 날짜 비교해 7일 이내면 자동복구(`restored:true`), 지났으면 동일하게 거절 → 성공 시 실패 카운터 리셋.
- 파일: `api/AuthController.java`, `infra/security/SecurityConfig.java`, `domain/user/service/UserService.java`, `domain/user/service/LoginResult.java`(신규), `domain/user/dto/LoginRequest.java`/`LoginResponse.java`(신규), `common/exception/ErrorCode.java`(`INVALID_CREDENTIALS` 추가), `UserServiceLoginTest.java`/`AuthControllerLoginTest.java`(신규)
- 테스트: 신규 11개(서비스 8, 컨트롤러 3) 전부 통과. 전체 스위트 427개 중 `UserApplicationFlowTest` 1건만 실패(기존 결함, 회귀 아님).
- 사유: RATE-1 다음 critical path 단위.
- 관련: TODO `AUTH-5`

---

## 2026-08-19 — Claude — `main` (RATE-1: 로그인 실패 횟수 제한)

- 변경: `LoginAttemptLimiter` 신규 구현 — 정규화 이메일을 SHA-256 해시해 Redis 키로 쓰고, 15분 내 5회 실패 시 15분 잠금(`checkNotLocked`/`recordFailure`/`reset` 3개 메서드). AUTH-5(로그인 API)가 아직 없어 이 클래스 자체는 독립적으로 구현·테스트했다.
- 파일: `infra/security/LoginAttemptLimiter.java`(신규), `common/exception/ErrorCode.java`(`ACCOUNT_LOCKED` 추가), `LoginAttemptLimiterTest.java`(신규)
- 테스트: 신규 5개 전부 실제 로컬 Redis(포트 6400)로 통과. 전체 스위트 416개 중 `UserApplicationFlowTest` 1건만 실패(기존 결함, 회귀 아님).
- 사유: AUTH-3/RATE-1/AUTH-5/AUTH-6 중 AUTH-5(로그인)의 선행 작업이라 critical path상 먼저 진행.
- 관련: TODO `RATE-1`

---

## 2026-08-19 — Claude — `main` (FRONTEND_API_GAPS.md §1.1 갱신 + 프론트 UX 결정 반영)

- 변경: `docs/FRONTEND_API_GAPS.md` §1.1을 (a) 회원가입(이메일 인증 포함 — 백엔드 완료, 프론트 미연동)과 (b) 로그인·계정복구(여전히 미구현)로 분리. (a)에는 "인증코드를 SignupPage에 인라인으로 넣는다"는 UX 결정을, (b)에는 "비밀번호 재설정 화면에서 코드 확인+새 비밀번호 입력을 한 화면으로 통합한다"는 UX 결정과 그에 따른 백엔드 API 설계 영향(코드검증+비밀번호저장을 단일 요청으로)을 명시했다. §0 표·§6 진행순서도 함께 갱신.
- 파일: `docs/FRONTEND_API_GAPS.md`
- 사유: 사용자가 프론트에 전달할 UX 결정 2건을 이 문서에 반영해달라고 요청.
- 관련: TODO `AUTH-4`(연장)

---

## 2026-08-19 — Claude — `main` (FRONTEND_API_INTEGRATION_SPEC.md 정합성 갱신)

- 변경: AUTH-4 커밋 이후 `docs/FRONTEND_API_INTEGRATION_SPEC.md`가 "일반 이메일 회원가입"을 여전히 `BLOCKED`로 표기하고 있어 최신화. 1.2 표에서 회원가입(§3.13, `READY`)과 로그인/중복확인/계정복구(여전히 `BLOCKED`)를 별도 행으로 분리하고, §3.12 차단 목록에서 `POST /api/auth/signup`을 제거, 신규 §3.13(요청/응답 예시·제약·프론트 미착수 사실 포함)을 추가했다. §5 체크리스트에 "일반 회원가입" 소절도 신설.
- 파일: `docs/FRONTEND_API_INTEGRATION_SPEC.md`
- 사유: 사용자가 이 문서의 프론트 계약이 실제 구현과 일치하는지 확인 요청 → 불일치 확인 후 최신화.
- 관련: TODO `AUTH-4`

---

## 2026-08-19 — Claude — `main` (AUTH-4 확인 필요 항목 확정 반영: phone 포함·비밀번호 정책)

- 변경: AUTH-4 커밋 시 남겨뒀던 확인 필요 2건이 사용자 확정됨에 따라 반영. (1) 프론트 `SignupPage.tsx` 재확인 결과 회원가입 화면이 `phone`을 필수 입력값으로 받고 있어 `SignupRequest`/`registerLocalUser`에 `phone`을 추가(엔티티 팩토리는 안 바꾸고 기존 `updateProfile`로 채움). (2) 비밀번호 정책을 최소 8자·최대 72자·복잡도 규칙 없음으로 확정(이미 구현된 값과 일치, 프론트도 동일하게 완화).
- 파일: `domain/user/dto/SignupRequest.java`, `domain/user/service/UserService.java`, `api/AuthController.java`, `AuthControllerSignupTest.java`, `backend/FRONTEND_API_REQUIREMENTS.md`, `docs/api/auth.md`(SIGNUP-1/2·AUTH-4 API 4~6 신규 문서화), `frontend/src/pages/SignupPage/SignupPage.tsx`(비밀번호 검증 8~64자+복잡도 → 8~72자만)
- 테스트: 신규 phone 형식 거절 테스트 1개 추가, 기존 4개 전부 통과 유지(총 5개). 전체 스위트 411개 중 `UserApplicationFlowTest` 1건만 실패(기존 결함, 회귀 아님).
- 사유: AUTH-4 완료 보고 후 사용자가 phone 필드 재확인·비밀번호 정책 확정을 요청.
- 관련: TODO `AUTH-4`

---

## 2026-08-19 — Claude — `main` (AUTH-4: 이메일 회원가입 API, 4단계 작업 완료)

- 변경: `POST /api/auth/signup` 구현 — signupToken을 SHA-256 해시로 Redis 조회해 이메일 일치를 검증하고(불일치/만료 전부 `INVALID_SIGNUP_TOKEN`으로 동일 응답), 중복 재조회 → BCrypt 해시 → `User.createLocalUser` 저장 → 로그인 토큰 발급을 하나의 트랜잭션으로 처리한다. 가입 토큰은 이 트랜잭션이 실제로 commit된 뒤(컨트롤러에서 `registerLocalUser` 호출이 반환된 다음)에만 삭제한다. 이로써 MAIL-1→SIGNUP-1→SIGNUP-2→AUTH-4 4단계 이메일 회원가입 인증 작업이 전부 완료됐다.
- 파일: `api/AuthController.java`, `domain/user/service/UserService.java`, `domain/user/service/EmailVerificationService.java`, `domain/user/service/LocalSignupResult.java`(신규), `domain/user/dto/SignupRequest.java`(신규), `infra/security/SecurityConfig.java`, `infra/security/PasswordEncoderConfig.java`(신규), `common/exception/ErrorCode.java`(`INVALID_SIGNUP_TOKEN` 추가), `AuthControllerSignupTest.java`(신규)
- 버그 수정: `UserService`가 `PasswordEncoder`를 직접 주입받기 시작하며 `SecurityConfig`↔`UserService` 순환 의존이 발생(`BeanCurrentlyInCreationException`)해 `PasswordEncoder` Bean을 `PasswordEncoderConfig`로 분리했다.
- 테스트: 신규 4개 전부 통과(permitAll 라우트 검증 포함). 전체 스위트 410개 중 `UserApplicationFlowTest.fullUserApplicationFlow()` 1건만 실패(기존 결함, 회귀 아님).
- 사유: 이메일 회원가입 인증 작업(MAIL-1→SIGNUP-1→SIGNUP-2→AUTH-4) 4단계 중 마지막 단위.
- 관련: TODO `AUTH-4`

---

## 2026-08-19 — Claude — `main` (SIGNUP-2: 이메일 인증 코드 확인 API)

- 변경: `POST /api/auth/signup/email-verification/confirm` 구현. 코드 확인과 실패 횟수 증가를 Redis Lua 스크립트로 원자 처리하고, 불일치/만료/이미사용/5회초과를 전부 동일한 오류(`INVALID_VERIFICATION_CODE`)로 응답한다(남은 시도 횟수 비노출). 성공 시 32바이트 URL-safe 가입 토큰을 발급하고 Redis엔 SHA-256 해시만 저장한다.
- 파일: `domain/user/service/EmailVerificationService.java`, `domain/user/dto/SignupEmailVerificationConfirmRequest.java`/`SignupEmailVerificationConfirmResponse.java`(신규), `resources/redis/verify-and-increment-code.lua`(신규), `api/AuthController.java`, `common/exception/ErrorCode.java`(`INVALID_VERIFICATION_CODE` 추가), `EmailVerificationServiceConfirmTest.java`(신규)
- 테스트: 신규 6개 전부 실제 로컬 Redis(포트 6400)로 통과. 전체 스위트 406개 중 `UserApplicationFlowTest.fullUserApplicationFlow()` 1건만 실패(SIGNUP-1 때 기록한 기존 결함과 동일건, 회귀 아님).
- 사유: 이메일 회원가입 인증 작업(MAIL-1→SIGNUP-1→SIGNUP-2→AUTH-4) 4단계 중 3번째 단위.
- 관련: TODO `SIGNUP-2`

---

## 2026-08-19 — Claude — `main` (SIGNUP-1: 이메일 인증 코드 요청 API)

- 변경: `POST /api/auth/signup/email-verification/request` 구현. 정책 확정 9단계(정규화 → 형식검증 → 중복이메일 조회 → 재전송/발송 횟수 제한 → 코드생성 → HMAC 저장 → TTL 10분 → SMTP 동기발송 → 응답)를 그대로 따랐다. 재전송 쿠폴다운 60초, 이메일별 1시간 5회, IP별 1시간 20회 제한을 Redis로 처리하고, 메일 발송 실패 시 challengeId가 일치할 때만 compare-and-delete Lua 스크립트로 안전하게 정리한다.
- 파일: `api/AuthController.java`, `infra/security/SecurityConfig.java`, `domain/user/service/EmailVerificationService.java`(신규)/`SignupCodeChallenge.java`(신규), `domain/user/dto/SignupEmailVerificationRequest.java`/`SignupEmailVerificationResponse.java`(신규), `resources/redis/compare-and-delete-challenge.lua`(신규), `common/exception/ErrorCode.java`(`TOO_MANY_REQUESTS` 추가), `application.properties`, `build.gradle`, `EmailVerificationServiceTest.java`(신규)
- 테스트: 신규 6개 전부 실제 로컬 Redis(Docker `honor-citizen-redis-test`, 포트 6400)로 통과. 전체 스위트 400개 중 `UserApplicationFlowTest.fullUserApplicationFlow()` 1건만 실패했으나 SIGNUP-1과 무관한 기존 결함으로 확인(약관동의 안 거친 테스트 픽스처 문제, 클린 HEAD에서도 재현) — 상세는 `TODO.md` "발견된 기존 결함" 절 참고.
- 사유: 사용자가 지시한 이메일 회원가입 인증 작업(MAIL-1→SIGNUP-1→SIGNUP-2→AUTH-4) 4단계 중 2번째 단위.
- 관련: TODO `SIGNUP-1`

---

## 2026-08-18 — Claude — `main` (마이페이지 신청 목록/상세 조회 API 6·7 구현)

- 변경: `docs/specs/application/api.md`에 설계만 있던 API 6(`GET /api/my/applications`, 목록)·API 7(`GET /api/my/applications/{id}`, 상세)을 구현했다. 목록은 로그인 사용자 본인 신청만 `createdAt DESC` 고정 정렬로 페이지네이션하며, `status`(`ApplicationStatus`) 선택 필터를 지원한다. 상세는 `application.isOwnedBy(userId)`로 소유권을 검증하고(타인이면 `FORBIDDEN`), `receiver`는 `issueType=MOBILE_AND_PHYSICAL`일 때만 채우고 그 외엔 `null`을 반환한다. 단체 신청은 구성원 개별 목록 대신 `memberCount`만 노출한다.
- 파일: `ApplicationRepository.java`(+`findByUserId`/`findByUserIdAndStatus`), `ApplicationMemberRepository.java`(+`countByApplicationId`), `MyApplicationListItemResponse.java`/`MyApplicationDetailResponse.java`(신규 DTO), `ApplicationService.java`(+`listMyApplications`/`getMyApplicationDetail`, `ReceiverRepository` 신규 주입), `MyApplicationController.java`(신규), `ApplicationServiceMyApplicationsTest.java`/`MyApplicationControllerTest.java`(신규), `docs/specs/application/api.md`
- 테스트: 신규 테스트 13개(서비스 7개, 컨트롤러 6개) 전부 통과. 전체 스위트 361개 중 기존과 동일하게 `UserControllerTest` 2건+Redis 미기동 1건만 실패(회귀 없음).
- 사유: 마이페이지 신청 목록/상세는 프론트가 이미 mock으로 화면을 그려둔 상태라 데이터 소스만 교체하면 되는 상태였고, 백엔드 설계가 이미 완료돼 있어 사용자 확인("응 그럼 구현") 후 바로 착수했다.
- ⚠️ 커밋 보류: `MyApplicationDetailResponse`가 `Application.paymentGuidedAt/cancelledAt/cancellationType/cancellationReason/refundedAt/cardReadyAt/physicalDispatchedAt`을 그대로 읽는데, 이 필드들은 Codex가 진행 중인 "신청 상태 리팩터링"(TODO #64)의 미커밋 `Application` 엔티티 변경분에만 존재한다. 지금 HEAD의 `Application`엔 없어 이 작업만 단독 커밋하면 컴파일이 깨진다 — Codex의 리팩터링 커밋이 먼저 들어간 뒤 이 작업을 커밋하기로 결정(사용자 확인 완료).

## 2026-08-18 — Codex — `main` (단체 신청 Excel 사진 번호 고정)

- 변경: Excel `ID` 열을 사용자 입력이 아닌 `사진 번호`로 변경하고, v1.1 양식 3종의 A4:A103에 텍스트 `001`~`100`을 수식 없이 사전 입력했다. 사진 번호 셀은 잠금·구분 색상·메모를 적용했다. `BulkExcelParser`는 사진 번호만 있는 행을 빈 행으로 무시하고 B열 이후 신청자 정보가 있는 행만 처리하며, 실제 처리 행의 사진만 ZIP과 매칭한다.
- 파일: `outputs/bulk-excel-templates-20260818/*_v1.1.xlsx`, `BulkExcelParser.java`, `BulkMemberRow.java`, `BulkExcelParserTest.java`, Application 정책 문서
- 테스트: 신규 테스트 3개를 기존 구현에서 실패 확인 후 `BulkExcelParserTest` 19개 전체 통과. 워크북 3종 자동 검증 및 전 시트 렌더링 통과.
- 사유: 사용자가 사진 매칭용 번호를 직접 입력하면서 발생할 수 있는 선행 0·중복·오입력 문제를 제거하고, 빈 템플릿 행을 신청자로 오인하지 않도록 하기 위함.

## 2026-08-17 — Codex — `main` (결제 안내·입금 확인 Service 및 자동취소 스케줄러)

- 변경: 관리자 결제 안내·입금 확인 Service 명령을 추가했다. 최초 안내만 `paymentGuidedAt`과 72시간 기한을 기록하고 재호출은 값을 유지한다. 입금 확인은 ApplicationStatus를 바꾸지 않으며 최초 `WAITING → CONFIRMED`에만 `AdminActivityLog.PAYMENT_CONFIRMED` 한 건을 기록한다.
- 자동취소: `SUBMITTED + WAITING + paymentDueAt<=now` 후보 ID를 조회해 신청별 별도 트랜잭션에서 재검증·취소한다. 기본 cron은 10분(`0 */10 * * * *`)이며 `application.payment-timeout-scheduler.cron`으로 변경할 수 있다. 자동취소도 슬롯 반환, DB 파일 참조 정리, commit 후 S3 삭제를 재사용한다.
- 동시성: `Application.@Version` 충돌 시 스케줄러가 stale 후보를 건너뛴다. 자동취소 후 늦은 입금 확인은 ApplicationStatus를 복구하지 않고 `CANCELLED + CONFIRMED` 환불 대기 조합으로 유지한다.
- 문서 정리: `docs/api/admin.md`의 구 `Payment row 필수` 조건을 제거했다. Source of Truth에 따라 별도 Payment row 없이 Application의 입금 확인 이력만 사용한다. 관리자 HTTP API는 아직 구현 전이다.
- 테스트: `ApplicationPaymentWorkflowTest` 5개 통과. 안내 기한 멱등성, 최초 입금 확인 로그 1건, 비관리자 거절, 자동취소·슬롯·S3 정리, 늦은 입금 비재활성화를 검증했다. 로그: `backend/honor-citizen/build/logs/application-payment-workflow.log`.
- 관련: TODO “신청 상태·취소·환불 구조 변경 체크리스트” §4·§6

---

## 2026-08-17 — Codex — `main` (사용자 취소 S3 정리·HTTP API 구현)

- 변경: 최초 사용자 취소 트랜잭션에서 로고·직인·제출 ZIP의 `UploadFile` 행과 Application 파일 ID, 멤버 `photoPath`를 정리하고 실제 S3 객체는 commit 성공 후 `afterCommit()`에서만 삭제하도록 연결했다. rollback·중복 취소에서는 삭제하지 않으며 S3 삭제 실패는 key와 예외를 경고 로그로 남기고 취소 결과를 유지한다. `POST /api/applications/{applicationId}/cancel`과 `ApplicationCancelResponse`를 추가했다.
- API: 요청 본문 없음. `applicationId`, `status`, `paymentStatus`, `refundRequired`, `cancelledAt` 반환. 비로그인 401, 타인 403, 없음 404, `NAME_EDITING` 이후 400, 중복 취소 200 멱등 성공.
- 파일: `Application.java`, `ApplicationMember.java`, `ApplicationService.java`, `ApplicationCancelResponse.java`, `ApplicationController.java`, `ApplicationServiceDailyLimitTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/api.md`, `docs/collab/{TODO,HANDOFF}.md`
- 테스트: 취소 S3/DB 통합 테스트 8개 통과. 취소 API Controller 테스트 11개 통과. 단체 취소 시 UploadFile 3행과 S3 4개 key 정리, rollback 시 파일 보존, S3 삭제 실패 격리, WAITING/CONFIRMED 환불 필요 응답을 검증했다. 로그: `backend/honor-citizen/build/logs/application-cancel-{s3-cleanup,api}.log`.
- 사유: 확정된 사용자 취소 정책의 파일 생명주기와 외부 HTTP 계약을 완성하기 위함.
- 관련: TODO “신청 상태·취소·환불 구조 변경 체크리스트” §4~6

---

## 2026-08-17 — Codex — `main` (사용자 취소 DB 전이·일일 슬롯 원자 처리)

- 변경: `ApplicationService.cancelByUser()`를 추가해 신청 조회, 소유권 확인, Entity 멱등 취소, 신청 생성일 KST 슬롯 반환을 하나의 `@Transactional` 경계에서 처리했다. 최초 취소에만 슬롯을 반환하며 중복 취소는 카운터를 다시 감소시키지 않는다. `ApplicationDailyLimitService.releaseSlot()`은 생성 실패 경로에서는 독립 트랜잭션, 취소 경로에서는 외부 트랜잭션에 합류한다는 실제 REQUIRED 전파 의미로 주석을 정리했다.
- 파일: `domain/application/service/{ApplicationService,ApplicationDailyLimitService}.java`, `domain/application/entity/ApplicationDailyLimit.java`, `ApplicationServiceDailyLimitTest.java`, `docs/collab/{TODO,HANDOFF}.md`
- 테스트: `ApplicationServiceDailyLimitTest` 6개 통과. 강제 outer rollback 시 `ApplicationStatus.SUBMITTED`와 일일 카운터 1이 함께 복구되는 실제 Spring/Test DB 검증 포함. Entity·일일 제한 직접 회귀 21개 통과. 로그: `backend/honor-citizen/build/logs/application-user-cancel-{transaction,regression}.log`.
- 범위: S3 파일 after-commit 삭제와 HTTP 취소 API는 포함하지 않았으며 각각 후속 논리 단위로 연결한다.
- 사유: 취소 상태와 일일 신청 슬롯이 서로 다른 트랜잭션에서 부분 반영되는 것을 방지하기 위함.
- 관련: TODO “신청 상태·취소·환불 구조 변경 체크리스트” §4·§6

---

## 2026-08-17 — Codex — `main` (Application 상태 enum·Entity 전이 구현)

- 변경: `ApplicationStatus`에서 `PAYMENT_PENDING`/`RECEIVED`를 제거하고 `SUBMITTED`/`PRODUCTION_READY`를 추가했다. `Application`에 결제안내·취소·환불·카드준비·실물인계 시각과 취소 분류, 낙관적 락 버전을 추가하고, 초기 `SUBMITTED + WAITING`부터 입금 확인·검토·작명·제작·완료·사용자/자동취소·환불완료까지 확정된 Entity 전이를 구현했다.
- 파일: `ApplicationStatus.java`, `CancellationType.java`, `CancellationReason.java`, `Application.java`, `ApplicationStateTransitionTest.java` 및 새 전이를 직접 사용하는 기존 Application/Review 테스트 픽스처, `docs/collab/TODO.md`
- 테스트: `ApplicationStateTransitionTest` 6개 통과. 상태 전이를 직접 사용하는 Application/Review 회귀 테스트 71개 통과. 로그: `backend/honor-citizen/build/logs/application-state-transition.log`, `application-state-dependent-regression.log`.
- 주의: 운영 DB 마이그레이션 도구가 없고 `schema.sql`은 시퀀스만 관리한다. `@Version` 및 신규 nullable 컬럼의 기존 운영 DB 반영·구 상태 데이터 변환은 별도 배포 단위로 남겼다.
- 사유: enum과 Entity 전이를 분리하면 중간 상태가 컴파일되지 않으므로, 독립 빌드·검증 가능한 하나의 논리 단위로 구현했다.
- 관련: TODO “신청 상태·취소·환불 구조 변경 체크리스트” §2~3

---

## 2026-08-17 — Codex — `main` (신청 상태·결제·취소 정책 문서 정합성)

- 변경: `APPLICATION.md` §16 확정 정책을 Application 요구사항·데이터 모델·API·서비스 흐름과 Admin/Payment 계약에 동기화했다. 초기 상태를 `SUBMITTED + WAITING`으로 변경하고 ApplicationStatus와 PaymentStatus를 분리했다. 최초 결제 안내부터 72시간, 기본 10분·설정형 자동 취소 스케줄러, 입금 확인 멱등 성공, 사용자/자동 취소 commit 직후 S3 삭제, 최소 `refundedAt` 환불 모델을 문서에 반영했다.
- 파일: `docs/specs/application/{APPLICATION,requirements,data-model,api,service-flow}.md`, `docs/api/{admin,payment}.md`, `docs/collab/{TODO,PENDING_DECISIONS}.md`
- 검증: 대상 문서에서 `PAYMENT_PENDING`, `RECEIVED`, 신청일 기준 3일, `COMPLETED` 단독 다운로드 조건 잔존 여부를 검색해 0건 확인. `git diff --check` 통과. 문서 전용 변경이라 Gradle 테스트는 실행하지 않았다.
- 사유: 코드 구현 전에 상충하는 구 상태·결제 계약을 제거하고 확정 정책을 Source of Truth 문서 체계에 전파하기 위함.
- 관련: TODO “신청 상태·취소·환불 정책 문서 정합성 반영” 완료

---

## 2026-08-16 — Claude — `main` (일일 신청 3회 제한 구현 — Application 도메인 마지막 미구현 항목)

- 변경: `checklist.md` §4·§5에서 유일하게 남아있던 미구현 항목("일일 KST 3회 제한")을 구현했다. 정책 자체는 `APPLICATION.md` §7에 이미 있었지만 "취소·반려 신청을 카운트에 포함할지"가 TBD로 막혀 있었는데, 이번에 사용자와 함께 확정(취소는 카운트 제외 — 취소하면 자리가 다시 빔, 반려는 재시도가 update성 사진 재업로드라 새 `create()` 자체가 없어 별도 결정 불필요)한 뒤 바로 구현까지 진행했다.
  - **왜 단순 `COUNT` 쿼리가 아닌지**: "카운트 확인 → 저장" 사이에 동시 요청이 끼어들면 두 요청 모두 "아직 2건이니 통과"로 오판할 수 있는 경쟁 상태가 생긴다 — 신청번호 채번을 `count+1` 방식에서 `application_seq` DB 시퀀스로 바꾼 것과 같은 이유(`ApplicationService.java` 주석 참고)이지만, 이번엔 "상한이 있는" 카운터라 시퀀스 하나로는 안 되고 사용자별·일자별 카운터 행을 비관적 락으로 잠그는 방식이 필요했다.
  - **신규 `ApplicationDailyLimit` 엔티티**(`user_id`+`count_date` UNIQUE): 별도 카운터 테이블을 둔 이유는 "취소하면 자리가 빈다"를 라이브 `COUNT(*) FROM applications` 집계로 구현하면 아직 DB에 저장되지 않은(파일 업로드 중인) 진행 중 요청을 반영할 수 없기 때문이다 — 파일 업로드 이전에 원자적으로 "자리"를 먼저 확정해야 동시 요청이 그 확정을 즉시 볼 수 있다.
  - **신규 `ApplicationDailyLimitService.reserveSlot/releaseSlot`**: 각각 독립된 `@Transactional`이라 호출할 때마다 새 트랜잭션이 열리고 즉시 커밋된다(파일 업로드처럼 느린 작업 동안 락을 들고 있지 않기 위해). `reserveSlot`은 기존 row가 있으면 `PESSIMISTIC_WRITE` 락으로 잠그고 증가시키고, 오늘 첫 신청이면 `saveAndFlush`로 INSERT를 시도한다 — 두 요청이 동시에 "오늘 첫 신청"이면 `UNIQUE(user_id, count_date)` 위반(`DataIntegrityViolationException`)이 나는데, 이 예외는 `ApplicationService`가 새 트랜잭션으로 **한 번만 재시도**해서 해소한다(같은 트랜잭션 안에서 재시도하면 이미 rollback-only로 표시된 트랜잭션을 계속 쓰게 돼 불안정하다 — Spring AOP self-invocation 문제와 별개로, 실패한 트랜잭션 재사용 자체가 문제).
  - **`ApplicationService.createIndividual`/`createGroup`**: 모든 검증(수령인/사진/학생필드/ZIP파싱 등) 이후, 파일 업로드 이전에 `reserveDailyLimitSlot()` 호출 — 한도 초과면 `APPLICATION_LIMIT_EXCEEDED`(429)로 파일 업로드 자체를 막는다. 기존 `uploadedKeys` 역순 삭제 catch 블록에 `releaseSlot()` 호출을 추가해, 파일 업로드나 DB 저장이 실패해도 슬롯이 낭비되지 않도록 했다.
  - **"취소 시 자리 반환"은 이번 범위에서 절반만 구현됨**: `releaseSlot()`은 재사용 가능한 공개 메서드로 만들어뒀지만, 실제 "신청 취소" API 자체가 아직 없어(별도 TODO, 정책도 미확정) 지금은 실패 보상 경로에서만 호출된다. `Application.cancel()`은 Entity라 Service를 호출할 수 없으므로(arch.md 계층 규칙), 취소 API가 생기면 그 Service 계층에서 `releaseSlot()`을 호출하는 방식으로 연결해야 한다 — Entity 자체에서 미리 연결해둘 수 없는 구조적인 이유다.
- 파일: `common/exception/ErrorCode.java`(`APPLICATION_LIMIT_EXCEEDED`), `domain/application/entity/ApplicationDailyLimit.java`(신규), `domain/application/repository/ApplicationDailyLimitRepository.java`(신규), `domain/application/service/{ApplicationDailyLimitService,ApplicationService}.java`, 테스트 4개 파일(아래), `docs/specs/application/{APPLICATION,api}.md`, `docs/collab/{TODO,PENDING_DECISIONS}.md`
- 테스트: 신규 19개 전부 통과 — `ApplicationDailyLimitTest`(엔티티, 5개), `ApplicationDailyLimitServiceTest`(9개 — `ExecutorService`+`CountDownLatch`로 동시 요청을 재현하는 동시성 시나리오 2개 포함: 기존 카운터 row를 여러 요청이 동시에 다투는 경우, 오늘 첫 신청 row 자체를 동시에 만들려는 경우), `ApplicationServiceDailyLimitTest`(3개 — 4번째 신청 거절, 개인·단체 합산, 타 사용자 무관), `ApplicationServiceUploadCompensationTest`에 2개 추가(실패 시 슬롯 반환 검증 — 이 파일은 이미 모든 테스트가 DB 저장 실패를 스텁하고 있어 딱 맞는 픽스처였다). 전체 스위트 316개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음. 기존 create 관련 테스트 중 같은 사용자로 4회 이상 생성하는 테스트가 있는지 사전에 grep으로 전수 확인(최대 2회)해 회귀 가능성을 미리 배제했다.
- 사유: Application 도메인 리팩터링 로드맵의 마지막 미구현 항목. 사용자가 TODO를 보고 "이거 메서드 쿼리만 만들면 되는거 아님?"이라고 질문한 것을 계기로 동시성 문제를 설명하고, 남은 정책 TBD(취소분 포함 여부)를 대화로 확정한 뒤 바로 구현까지 이어갔다.
- 관련: TODO "일일 KST 3회 제한 DB 원자 처리" 항목(완료로 갱신), `PENDING_DECISIONS.md` 관련 항목 2건 해결

---

## 2026-08-16 — Claude — `main` (Event 도메인 신규 구현 — 행사사업 부스 운영/법인·단체 협업)

- 변경: 프론트 `/events` 페이지의 부스 운영·법인단체 협업 기록(정적 목데이터+URL 없는 모달)을 `EventPost`+`EventType{BOOTH,COLLABORATION}` 모델로 신규 구현했다. 공개 조회 2개(목록/단건) + 관리자 전용 CRUD 3개(생성/수정/삭제) 총 5개 API. Board 구현 때 만든 패턴(관리자 CRUD 라우트 레벨 인가, S3 업로드 보상삭제, 전체 재제출 PATCH)을 그대로 재사용해서 설계·구현 모두 빠르게 진행했다.
  - **`EventPost` 엔티티**: `eventType`/`title`/`eventDate`/`eventDateText`/`place`/`host`/`cardLabel`/`content`/`thumbnailImagePath`/`visible`/`displayOrder`. Board와 달리 작성자 추적(`created_by_user_id`)이 요구사항에 없어 두지 않았다. `visible=false`는 상세·목록 양쪽에서 `EVENT_NOT_FOUND`로 존재 자체를 숨긴다 — Board에는 없던 "비공개 게시" 개념을 이번에 신규로 정했다.
  - **`EventImage`**: 상세 갤러리 이미지. Board의 `BoardAttachment`(`UploadFile` join 엔티티)와 달리 **`UploadFile`을 경유하지 않고 S3 key를 엔티티에 직접 저장**한다 — Review의 `image_path` 직접 저장 패턴과 동일. 설계 단계에서 `representative`(대표 이미지) 플래그를 넣을지 고민했으나, `EventPost.thumbnail_image_path`가 이미 대표 이미지의 유일한 소스이고 실제 프론트(`EventsPage.tsx` 상세 모달)도 `[썸네일, ...갤러리]`를 클라이언트에서 직접 이어붙이는 구조라 서버가 대표 여부를 별도 추적할 필요가 없다고 판단해 **최종적으로 빼기로 확정**(사용자 확인 완료, `data-model.md` §2 갱신).
  - **`EventPostRepository.findVisibleByEventType`**: 정렬 정책(`display_order ASC(NULL 맨 뒤) → event_date DESC(NULL 맨 뒤) → created_at DESC`)을 JPQL의 `ORDER BY ... NULLS LAST`로 고정 구현 — Pageable에는 정렬을 싣지 않고 페이지 범위만 넘긴다.
  - **`EventService`**: `create()`는 Board와 동일하게 썸네일+갤러리 S3 업로드와 DB 저장을 한 트랜잭션에서 처리하고 실패 시 `uploadedKeys` 역순 보상삭제. `update()`는 텍스트 필드+`visible`+`displayOrder` 전체 재제출이며 갤러리 편집은 이번 패스에서 다루지 않고, 썸네일은 새 파일이 있을 때만 Review `applyImageChange`와 동일한 패턴(새 파일 업로드→교체→기존 파일은 커밋 이후 삭제)으로 교체한다. `delete()`는 `EventImage`+`EventPost`를 한 트랜잭션에서 지우고 썸네일+갤러리 전체를 커밋 이후 S3에서 정리한다.
  - **신규 `EventImageValidator`**(package-private, `domain.event.service`): `ReviewImageValidator`와 검증 규칙이 완전히 동일(2MB, jpg/jpeg/png/webp, 크기→확장자/MIME→시그니처→디코딩 순)하지만 재사용하지 않고 새로 만들었다 — `ReviewImageValidator`가 package-private라 다른 패키지에서 애초에 주입이 불가능하고, 이 프로젝트는 이미 "검증기는 도메인마다 독립"이 관례(Board의 `BoardAttachmentValidator`도 동일 원칙으로 신규 제작).
  - **`SecurityConfig`**: `/api/admin/events/**`는 Board 구현 때 이미 추가한 `/api/admin/**` → `hasRole("ADMIN")` 규칙에 코드 변경 없이 자동으로 편입된다. 이번에 추가한 건 공개 GET(`/api/events`, `/api/events/**`) `permitAll()` 하나뿐.
  - 세션 중 사용자와 함께 확정한 2가지: (1) 위에서 설명한 `EventImage.representative` 제거 (2) 관리자 전용 전체 목록 API(`GET /api/admin/events`, `visible` 무관 — 관리자가 숨긴 글을 다시 찾으려면 필요)는 있어야 하는 건 맞지만 이번 패스에서는 제외하고 이후 별도 구현하기로 결정. v1에서 관리자는 생성 응답의 `id`로만 수정·삭제 가능.
  - `EventDetailResponse`에는 Board/Review의 `next`(다음글)를 넣지 않았다 — 프론트에 애초에 상세 페이지 라우트가 없어(모달뿐) 이전/다음 이동 UI 자체가 없다(`data-model.md` §0에서 이미 범위 밖으로 명시).
- 파일: `common/enums/EventType.java`(신규), `common/exception/ErrorCode.java`(`EVENT_NOT_FOUND`), `domain/event/entity/{EventPost,EventImage}.java`, `domain/event/repository/{EventPostRepository,EventImageRepository}.java`, `domain/event/service/{EventImageValidator,EventService}.java`, `domain/event/dto/*`(6개), `api/{EventController,EventAdminController}.java`, `infra/security/SecurityConfig.java`, 테스트 6개 파일(아래), `docs/specs/events/{data-model,api}.md`, `arch.md` §4.9(신규), `docs/collab/TODO.md`
- 테스트: 설계를 먼저 사용자와 텍스트로 합의한 뒤 구현하는 방식으로 진행(Board와 동일한 협업 순서). 신규 39개 전부 통과 — `EventPostTest`(3), `EventImageTest`(1), `EventImageValidatorTest`(7), `EventServiceTest`(14, 생성/목록 정렬·필터/상세/수정/삭제+검증 실패 케이스), `EventControllerTest`(6, 공개 조회+비공개 글 숨김), `EventAdminControllerTest`(8, 관리자 CRUD+`ADMIN`/`USER`/비로그인 권한 3분기). 전체 스위트 297개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: `docs/specs/events/data-model.md`(이전 세션에 작성만 되고 커밋 안 된 채 방치)를 발견한 뒤, 사용자에게 서비스 로직 제안을 먼저 드리고 두 가지 설계 결정(대표 이미지 플래그 제거, 관리자 목록 API 이월)을 확인받은 뒤 구현했다 — Board 때 확립한 "설계 문서 먼저 → 사용자 확인 → 코드" 순서를 그대로 재사용.
- 관련: TODO "Event(행사) 도메인 구현" 행(완료로 갱신)

---

## 2026-08-14 — Claude — `main` (Board 도메인 신규 구현 — 공지사항/FAQ)

- 변경: 프론트에 존재하지만 서버 API가 없던 공지사항/FAQ 게시판을 `Board`+`BoardType{NOTICE,FAQ}` enum 통합 모델로 신규 구현했다. 공개 조회 2개(목록/단건) + 관리자 전용 CRUD 3개(생성/수정/삭제) 총 5개 API.
  - **`Board` 엔티티**: `boardType`/`title`/`content`/`createdByUserId`. FAQ는 title/content에 질문/답변을 저장(별도 Q/A 필드 없음, data-model.md §0에서 이미 확정한 방향). `arch.md` §5.1 원칙대로 `createdByUserId`는 `Long`만 참조(`User`와 JPA 연관관계 없음).
  - **`BoardAttachment`** join 엔티티(`Board:UploadFile`=1:N, NOTICE 전용) — `UploadFile`이 "아무것도 참조하지 않는 공용 메타데이터 테이블"이라는 기존 원칙(`docs/api/upload-file.md`) 때문에 직접 1:N을 걸지 않는다. `(board_id,upload_file_id)`·`(board_id,display_order)` 유니크 제약.
  - **`BoardAttachmentValidator`** 신규(패키지 프라이빗, `ApplicationPhotoValidator`와 분리) — 문서 위주 첨부(pdf/hwp/docx/xlsx 등)라 이미지 전용 검증기를 재사용할 수 없어 별도 컴포넌트로 만들었다. 최대 10개, 1개당 10MB, 확장자+MIME 허용목록, 이미지 확장자(jpg/png)에 한해서만 바이너리 시그니처 검증.
  - **`BoardService`**: `create()`는 Review처럼 S3 업로드+DB 저장을 한 `@Transactional` 안에서 처리(파일 수가 적어 커넥션 점유가 문제되지 않음, Application처럼 별도 영속 서비스로 분리하지 않음), 실패 시 `uploadedKeys` 역순 보상 삭제(data-model.md §4.1). `delete()`는 `BoardAttachment`+`UploadFile`+`Board`를 한 트랜잭션에서 지우고 S3는 커밋 이후 삭제(§4.4, 순서를 바꾸면 롤백 시 DB·S3 불일치 발생). `update()`는 Review PATCH와 동일하게 전체 재제출이며 이번 패스에서는 첨부파일을 건드리지 않는다.
  - **`SecurityConfig`**: `arch.md` §4.6에 이미 "`/api/admin/**`는 `ADMIN`만"이라는 원칙이 문서로는 있었으나 실제 코드는 `/admin/**`(API 프리픽스 없는 경로)만 막고 있던 공백을 이번에 메웠다 — `.requestMatchers("/api/admin/**").hasRole("ADMIN")` 신규 추가(이 프로젝트의 첫 관리자 전용 쓰기 API). `GET /api/boards`·`GET /api/boards/**`는 `permitAll()`. 라우트 레벨 강제만으로 충분해 컨트롤러/서비스에는 별도 권한 분기 코드가 없다(Review의 `canEdit`/`canDelete`처럼 리소스 소유권 판단이 필요한 경우와 다름).
  - 세션 중 사용자와 함께 비즈니스 로직을 먼저 확정한 뒤 코드로 넘어갔다: (1) QnA(FAQ)는 관리자만 CRUD 가능 (2) `boardType=FAQ`인데 첨부파일이 오면 조용히 무시하지 않고 `INVALID_INPUT`으로 거절(FAQ는 첨부파일 개념 자체가 없음) (3) 관리자 CRUD 인가는 서비스 레벨이 아니라 라우트 레벨(`SecurityConfig`)로 강제.
  - 구현 중 `docs/specs/board/api.md` 문서 불일치 발견 후 정리: API 4(수정)가 `multipart/form-data`로 초안 작성돼 있었는데 실제로는 `attachments` 파트 자체를 받지 않아(첨부파일 편집은 다음 패스로 명시적으로 미룸) Validation 표에 도달 불가능한 행이 남아있었다 — `application/json`으로 단순화하고 관련 행을 정리했다.
- 파일: `common/enums/{BoardType,UploadFileType}.java`, `common/exception/ErrorCode.java`(`BOARD_NOT_FOUND`), `domain/board/entity/{Board,BoardAttachment}.java`, `domain/board/repository/{BoardRepository,BoardAttachmentRepository}.java`, `domain/board/service/{BoardAttachmentValidator,BoardService}.java`, `domain/board/dto/*`(6개), `api/{BoardController,BoardAdminController}.java`, `infra/security/SecurityConfig.java`, 테스트 6개 파일(아래), `docs/specs/board/api.md`, `docs/collab/TODO.md`
- 테스트: TDD로 진행. 신규 34개 전부 통과 — `BoardTest`(2), `BoardAttachmentTest`(1), `BoardAttachmentValidatorTest`(6), `BoardServiceTest`(12, 생성/목록/단건/수정/삭제+검증 실패 케이스), `BoardControllerTest`(5, 공개 조회+검증), `BoardAdminControllerTest`(8, 관리자 CRUD+`ADMIN`/`USER`/비로그인 권한 3분기). 전체 스위트 258개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: `docs/specs/board/data-model.md`(이전 세션에 작성만 되고 커밋 안 된 채 방치)와 신규 작성한 `api.md`로 설계를 먼저 확정한 뒤, "미완료 작업 조사" 요청으로 이 공백이 드러나 이번 세션에서 실제 동작하는 API로 완성했다.
- 관련: TODO "Board 도메인(공지사항/FAQ) 구현" 행(완료로 갱신)

---

## 2026-08-14 — Claude — `main` (학생증 신청 항목 추가 — 학교구분·가로형/세로형)

- 변경: 학생증(STUDENT) 카드 신청 방식을 변경했다. 사용자 요청: 개인 신청은 대학교/고등학교 선택 + 가로형/세로형 선택을 새로 받고, 대학교를 선택했을 때만 학번·학과를 입력받는다(고등학교는 추가 입력 없음). 법인·단체 신청은 가로형/세로형 + 학교구분 선택만 추가하고, 학번·학과는 여전히 첨부 엑셀로만 받는다.
  - **신규 enum**(`common/enums/`): `Orientation{LANDSCAPE,PORTRAIT}`, `SchoolType{UNIVERSITY,HIGH_SCHOOL}`. JSON 값은 대문자(이 프로젝트에 enum 케이스 변환 설정이 없어 Jackson 기본 동작 그대로 — `gender` 필드와 동일하게 프론트가 전송 직전 `.toUpperCase()` 필요).
  - **`Application` 엔티티**에 `orientation`/`school_type` 컬럼 신규 추가 — 개인·단체 공통, 학생증 전용, 신청서 전체에 1개(단체도 엑셀 컬럼이 아니라 신청 폼 필드). `createIndividual`/`createGroup` 정적 팩토리는 새 2개 파라미터를 받는 버전을 추가하면서, 기존 시그니처(로고/직인까지만 받던 버전)를 **하위 호환 오버로드**로 남겨 `null, null`로 위임하게 했다 — 그 시그니처를 직접 호출하는 기존 테스트가 약 20개(Review 도메인 테스트 포함) 있어, 전부 고치는 대신 오버로드로 격리해 무관한 파일을 건드리지 않았다.
  - **`ApplicationService.validateStudentFields`(개인)**: 기존 "학생증이면 학번·학과 무조건 필수" 규칙을 "학생증+`schoolType=UNIVERSITY`일 때만 필수, `HIGH_SCHOOL`이면 있으면 오히려 거절"로 변경. orientation·schoolType·로고는 학교구분과 무관하게 학생증이면 항상 필수. 신규 ErrorCode 없이 기존 `INVALID_INPUT` 재사용.
  - **`ApplicationService.createGroup`(단체)**: 학생증이면 orientation·schoolType 둘 다 필수, 아니면 둘 다 없어야 함(`INVALID_INPUT`) — 학번·학과 검증은 그대로 `BulkExcelParser`(엑셀) 책임으로 남겨두고 이번 변경에서 건드리지 않았다.
  - 세션 중 두 가지를 사용자에게 확인 후 확정: (1) 단체 신청도 처음엔 "학교구분 필드 없이 첨부 엑셀 자유기재"로 논의했으나, 프론트에 애초에 그런 UI가 없다는 걸 같이 확인한 뒤 "단체도 체크박스 추가"로 최종 확정 — 그 결과 schoolType을 `ApplicationMember`가 아니라 `Application` 레벨로 옮겨(orientation과 동일 위치) 개인·단체 모델을 통일했다. (2) orientation 값의 JSON 대소문자 계약을 명확히 확인 — 대문자, `gender` 필드와 동일 관례.
  - 카드종류별 config/전략 추상화는 도입하지 않고 기존 `CardType.isStudentCard()`(`isStudent` boolean) 게이트를 그대로 재사용 — 재사용처가 1곳(학생증)뿐이라 새 추상화는 과설계라고 판단(이 저장소의 "재사용 2곳 이상 아니면 새 클래스 안 만든다" 원칙과 일치).
- 파일: `Orientation.java`, `SchoolType.java`(신규), `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `Application.java`, `ApplicationFactory.java`, `ApplicationPersistenceService.java`, `ApplicationService.java`, `ApplicationServiceTest.java`(+7 신규 케이스), `ApplicationServiceBulkTest.java`(+5 신규 케이스), `ApplicationServiceUploadCompensationTest.java`(기존 학생증 픽스처에 orientation/schoolType 보정), `ApplicationFactoryTest.java`(시그니처 보정), `docs/specs/application/{data-model,api}.md`, `docs/collab/TODO.md`
- 테스트: TDD로 진행(테스트 먼저 작성 → 의도대로 실패 확인 → 구현 → 통과). `ApplicationServiceTest`/`ApplicationServiceBulkTest` 신규 12개 케이스 전부 통과. 전체 스위트 224개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: 학생증 신청 방식이 대학교/고등학교로 갈리도록 정책이 바뀌었고, 구현 전 `/plan`으로 변경범위(엔티티·DTO·검증 로직·기존 테스트 파급범위)를 먼저 확정한 뒤 착수했다.
- 관련: TODO "학생증 신청 항목 추가" 행(완료로 갱신), 계획 파일 `C:\Users\gpdnj\.claude\plans\application-api-async-knuth.md`

---

## 2026-08-14 — Codex — `main` (User 조회 문서 정합성 정리)

- 변경: `docs/api/user.md`의 `GET /api/users/me` 과거 구현 전 문구를 현재 백엔드 구현 상태에 맞게 정리하고, API 상태를 구현 완료로 갱신했다.
- 파일: `docs/api/user.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`
- 테스트: 코드 변경 없음. `rg`로 stale 문구 제거와 구현 근거 문구를 정적 검증했다.
- 사유: `GET /api/users/me`는 현재 `UserController#getMe`로 구현되어 있으나 문서에는 과거 구현 전 문구가 남아 있어 정합성을 맞춤.
- 관련: TODO "User 조회 문서 정합성 정리" 행(완료로 갱신)

## 2026-08-13 — Claude — `main` (Review 도메인 CRUD 5개 API 구현)

- 변경: `docs/specs/review/{data-model,api}.md`에 설계된 Review 도메인을 실제로 구현했다. TDD로 진행(테스트 먼저 작성 → 실패 확인 → 구현 → 통과 확인)했으며, 신규 테스트 76개 전부 통과.
  - **엔티티/리포지토리**: `Review`(`card_type_id`/`image_path`를 컬럼으로 직접 저장, join 엔티티 없음), `ReviewRepository extends JpaSpecificationExecutor<Review>`(이 프로젝트 첫 Specification 사용).
  - **이미지 검증**: `ReviewImageValidator`(2MB, jpg/jpeg/png/webp, 해상도 하한 없음 — `ApplicationPhotoValidator`와 기준이 달라 재사용하지 않고 신규 작성). WEBP는 Java 표준 `ImageIO`가 디코딩을 지원하지 않아 `com.twelvemonkeys.imageio:imageio-webp:3.10.1` 의존성을 신규 추가(`build.gradle`) — 사람이 "WEBP 디코딩 라이브러리 추가"로 결정.
  - **자격검증**: `ReviewEligibilityService` — (1) 로그인 사용자의 이메일이 `Applicant.email`(대표 제출자) 또는 `ApplicationMember.email`(단체 구성원 개인)과 매칭되는 `Application.status=COMPLETED` 건 중 `(application_type, card_type_id)` 조합이 일치해야 통과(`REVIEW_NOT_ELIGIBLE`), (2) ✅ 세션 중 신규 확정: 같은 사용자가 같은 조합으로 이미 작성한 후기가 있으면 거절(`REVIEW_ALREADY_EXISTS`, "조합당 1개" 정책 — `Review→Application` FK를 두지 않는 기존 설계를 유지하기 위해 판단 기준은 `(user_id, application_type, card_type_id)` 조합의 유일성), (3) ✅ 세션 중 신규 확정: 탈퇴(`WITHDRAWN`) 계정은 새 후기를 등록할 수 없음(`ALREADY_WITHDRAWN`) — 단 이 체크는 **등록에만** 적용하고 수정에는 적용하지 않음(원작성자가 나중에 탈퇴해도 관리자가 기존 후기를 계속 관리할 수 있어야 하므로).
  - **API 1 등록** `POST /api/reviews`: multipart(`request` JSON + `image` 0~1개). `image` 파트 2개 이상 전송 시 `INVALID_INPUT`.
  - **API 2 목록** `GET /api/reviews`: `cardTypeId`/`hasPhoto`/`searchType`+`keyword` 필터, 페이징(기본 size=9, 상한 100). `ReviewSpecifications`로 동적 쿼리 구현. **주의**: 이 프로젝트가 쓰는 Spring Data JPA 버전은 `Specification.where(null)`/`.and(null)`을 더 이상 허용하지 않고(과거 버전과 동작이 다름) `IllegalArgumentException`을 던지므로, 각 조건 메서드가 null 대신 `cb.conjunction()`(항상 참)을 반환하도록 작성해야 한다. 정렬은 `createdAt DESC`만으로는 동시 등록 시 밀리초 단위로 값이 같아질 수 있어(H2 등) `id DESC`를 2차 정렬키로 추가했다(초기 구현에서 실제로 플레이키 발생 후 수정).
  - **API 3 단건조회** `GET /api/reviews/{id}`: 비로그인 공개 조회, `canEdit`/`canDelete`는 관리자 또는 작성자 본인만 `true`. `next`(다음 오래된 글)만 제공.
  - **API 4 삭제** `DELETE /api/reviews/{id}`: 작성자 또는 관리자만 가능(`FORBIDDEN`). Review row 삭제 후 트랜잭션 commit 시점에 S3 이미지 객체 삭제(`TransactionSynchronizationManager.registerSynchronization`, `ApplicationService`의 기존 after-commit 패턴 재사용).
  - **API 5 수정** `PATCH /api/reviews/{id}`: 등록과 동일 5개 필드 전체 재제출 + `removeImage`. 사진 처리 3가지 경우(교체/삭제/유지) 구현. `applicationType`/`cardTypeId` 변경 시 원작성자(`Review.userId`, 수정자 아님) 기준으로 자격 재검증.
  - **공통**: `PageResponse<T>`(이 프로젝트 첫 페이징 응답 포맷) 신규. `ErrorCode`에 `REVIEW_NOT_FOUND`/`REVIEW_NOT_ELIGIBLE`/`REVIEW_ALREADY_EXISTS`/`INVALID_IMAGE_FILE` 추가(`INVALID_IMAGE_FILE`은 기존 `INVALID_IMAGE`가 "얼굴을 식별할 수 없습니다"라는 얼굴사진 전용 메시지라 Review에 그대로 재사용하면 오해의 소지가 있어 신규로 분리). `GlobalExceptionHandler`에 `MethodArgumentTypeMismatchException` 핸들러 추가(`?searchType=WRONG` 같은 잘못된 enum 쿼리 파라미터를 `INVALID_INPUT`으로 응답). `SecurityConfig`에 `GET /api/reviews`·`GET /api/reviews/{id}`만 `permitAll()` 추가(등록/수정/삭제는 기존 `hasAnyRole("USER","ADMIN")` 그대로 적용).
  - **테스트 관례 명문화**: 서비스 계층(`@SpringBootTest`+실 H2)·컨트롤러 계층(`@AutoConfigureMockMvc`+`MockMvc`, 실제 JWT로 Security 필터체인까지 통과) 2계층 테스트 패턴을 처음으로 `docs/collab/RULES.md` §8에 문서화 — 기존 코드에 이미 있던 관례를 관찰해 따른 것이라 명문화만 함.
- 파일: `Review.java`, `ReviewRepository.java`, `ReviewImageValidator.java`, `ReviewSpecifications.java`, `ReviewEligibilityService.java`, `ReviewService.java`, `ReviewController.java`, `ReviewCreateRequest/Response.java`, `ReviewUpdateRequest.java`, `ReviewListItemResponse.java`, `ReviewDetailResponse.java`, `CardTypeSummaryResponse.java`, `ReviewSearchType.java`, `PageResponse.java`, `ErrorCode.java`, `GlobalExceptionHandler.java`, `SecurityConfig.java`, `ApplicantRepository.java`/`ApplicationMemberRepository.java`(`findByEmail` 신규 추가), `build.gradle`(twelvemonkeys 의존성), 관련 테스트 8개 파일(신규), `docs/specs/review/{data-model,api}.md`, `docs/collab/RULES.md` §8(신규)
- 테스트: Review 도메인 신규 76개 전부 통과(`ReviewTest`, `ReviewEligibilityServiceTest`, `ReviewImageValidatorTest`, `ReviewServiceCreateTest`, `ReviewServiceListTest`, `ReviewServiceDetailTest`, `ReviewServiceDeleteTest`, `ReviewServiceUpdateTest`, `ReviewControllerTest`, `PageResponseTest`). 전체 스위트 216개 중 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: `docs/specs/review/{data-model,api}.md`에 설계만 있고 구현이 없던 상태를 실제 동작하는 API로 완성. 구현 도중 발견한 정책 공백(후기 작성 개수 제한, 탈퇴 계정 처리) 2건은 임의로 결정하지 않고 사람에게 확인 후 문서(`api.md`/`data-model.md`)에 먼저 반영한 뒤 구현했다.
- 관련: TODO "Review 도메인 구현" 행(완료로 갱신)

## 2026-08-09 — Codex — `main` (Redis 기동 후 전체 테스트 실패 재분류)

- 변경: Redis 기동 후 기존 전체 테스트 실패 3건을 재실행해 실패 원인을 재분류했다. `UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken`, `UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall`은 통과했고, 이전 실패 원인은 Redis 미기동에 따른 `RedisConnectionFailureException`으로 확인했다. `UserApplicationFlowTest.fullUserApplicationFlow`는 Redis 연결 실패가 해소됐지만 신청 생성 단계에서 `TERMS_NOT_AGREED` 403으로 실패했다.
- 파일: `docs/specs/application/checklist.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`
- 테스트: `./gradlew.bat test --tests com.example.honorcitizen.api.UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken --tests com.example.honorcitizen.api.UserControllerTest.withdrawReturnsAlreadyWithdrawnOnSecondCall --tests com.example.honorcitizen.flow.UserApplicationFlowTest.fullUserApplicationFlow` 실행 — 3개 중 2개 통과, 1개 실패(`TERMS_NOT_AGREED`).
- 사유: Redis 기동 후 실패가 환경 문제인지, 현재 정책/테스트 불일치인지 정확히 분리하기 위함.
- 관련: Application 검증 작업 후속 확인

## 2026-08-08 — Codex — `main` (재업로드 S3 정리 after-commit 전환)

- 변경: 사진 재업로드에서 기존 S3 파일 삭제를 DB 트랜잭션 commit 이후에만 수행하도록 `TransactionSynchronizationManager` 기반 after-commit 정리로 변경. 신규 업로드 S3 key는 메서드 내부 실패뿐 아니라 transaction rollback/commit 실패 경로에서도 보상 삭제되도록 after-completion 보상 경로를 추가했다. `PHOTO_REJECTED → PENDING` stale 주석은 SoT 기준인 `PHOTO_REJECTED → REVIEWING`으로 수정했다.
- 파일: `ApplicationService.java`, `ApplicationServicePhotoReuploadTest.java`, `docs/specs/application/service-flow.md`
- 테스트: 재업로드 rollback 시 신규 S3 삭제·기존 S3 유지, after-commit 기존 S3 삭제 실패 시 성공 응답 유지, 단체 재업로드 멤버 사진 업로드 실패 시 신규 ZIP 보상 삭제를 최소 보강.
- 사유: `@Transactional` 메서드 내부 마지막에 기존 S3 파일을 삭제하면 실제 DB commit 이후 삭제가 보장되지 않아, rollback 시 DB는 복구됐지만 기존 S3 파일은 삭제되는 정합성 위험이 있었음.
- 관련: Application 검증 작업 커밋 분리 계획 — 2. 재업로드 transaction-safe cleanup

## 2026-08-08 — Codex — `main` (생성 경로 S3 업로드 실패 보상)

- 변경: `createIndividual`/`createGroup`의 S3 업로드 구간까지 보상 처리 범위를 확장. 로고·직인·ZIP·멤버 사진 업로드 중간 실패와 DB 저장 실패 모두에서 이미 업로드된 신규 S3 key를 역순 삭제한다. 보상 삭제 실패는 원 예외를 덮어쓰지 않고 로그만 남기도록 정리했다.
- 파일: `ApplicationService.java`, `ApplicationServiceUploadCompensationTest.java`, `docs/specs/application/service-flow.md`
- 테스트: `ApplicationServiceUploadCompensationTest`에 S3 업로드 중간 실패와 보상 삭제 실패 케이스를 최소 보강. `./gradlew.bat test --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceUploadCompensationTest"` 통과.
- 사유: 기존 try/catch가 DB 저장 호출만 감싸 S3 업로드 중간 실패 시 앞서 업로드된 파일이 고아로 남을 수 있던 failure-path 정리.
- 관련: Application 검증 작업 커밋 분리 계획 — 1. 생성 경로 S3 failure compensation

## 2026-08-08 — Claude — `main` (UserUpdateRequest — address를 수정 대상에서 제외, name 길이 제한 추가)

- 변경: `PATCH /api/users/me`의 수정 가능 필드를 `name`/`phone`으로 확정(사람 확인) — `email`은 기존처럼 OAuth 식별값이라 수정 불가, `address`도 이번에 수정 대상에서 제외됨. `UserUpdateRequest`에서 `address` 필드를 제거(요청 본문에 보내도 무시됨)하고 `name`에 `@Size(max=255)`(User 컬럼 길이 기본값 기준)를 추가했다. `User.updateProfile(name, phone, address)` → `updateProfile(name, phone)`으로 시그니처 축소, `UserService.updateMe()`의 "최소 1개 필드 필요" 체크에서도 `address`를 뺐다. `GET /api/users/me` 응답에는 `address` 컬럼이 계속 노출됨(조회는 그대로, 수정만 막힘).
- 파일: `UserUpdateRequest.java`, `User.java`, `UserService.java`, `UserControllerTest.java`(`updateMeUpdatesPhoneAndAddress` → `updateMeUpdatesNameAndPhone`로 교체, `updateMeIgnoresAddressEvenWhenProvidedInRequestBody` 신규), `docs/api/user.md`(API 5 정정 노트)
- 테스트: 신규/수정 테스트를 구현 전 실패 확인 후 통과. User 도메인 18개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패. 전체 스위트 147개 중 동일하게 3건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: Request DTO 검증 점검 중 `address`만 blank 방어가 없어 조용히 지워질 수 있던 문제를 보고했고, 사람이 "address는 애초에 수정 대상이 아니어야 한다"로 범위를 확정하면서 문제가 자연히 해소됨.
- 관련: 없음

## 2026-08-08 — Claude — `main` (createGroup — User 자격 검증을 파일 업로드 이전으로 이동)

- 변경: `ApplicationService.createGroup()`이 존재 여부만 확인하는 `userService.findById(userId)`를 로고·직인·ZIP·멤버 사진 업로드가 모두 끝난 뒤(그것도 `try` 블록 밖)에서 호출하던 문제를 수정. 개인 신청(`createIndividual`)과 동일하게 메서드 최상단에서 `findUser(userId)`(=`findEligibleApplicationUser`, 탈퇴/권한/약관 동의까지 검증)를 호출하도록 이동했다. 이로써 ①탈퇴·비-USER role·약관 미동의 사용자의 단체 신청이 개인 신청과 동일하게 차단되고, ②User 검증 실패가 더 이상 `try` 블록 밖에서 발생하지 않아 이미 업로드된 S3 파일이 고아로 남는 문제도 함께 해소됨.
- 파일: `ApplicationService.java`, `ApplicationServiceBulkTest.java`(신규 3건 + 픽스처 `agreeTerms` 보강), `ApplicationBulkControllerTest.java`(픽스처 `agreeTerms` 보강)
- 테스트: 신규 3건(탈퇴/비-USER role/약관 미동의, 탈퇴 케이스는 `storageService` 미호출까지 검증)을 구현 전 실패 확인 후 통과. Application/API 도메인 133개 전체 통과, 전체 스위트는 기존과 동일하게 `UserControllerTest` 2건·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 회귀 없음.
- 사유: 개인/단체 신청 간 신청 자격 검증 정책 불일치 및 리소스 누수 버그 발견 후 수정.
- 관련: 없음

## 2026-08-08 — Claude — `main` (Application Request DTO 입력값 검증 보강)

- 변경: `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 필드 검증을 DB 컬럼 길이·표준 Bean Validation 기준으로 보강. `@Size`(엔티티 컬럼 길이와 일치: name/zipCode/address/detailAddress/deliveryRequest/englishName/birthRegion/organizationName/department), `@Email`(applicant.email), `@Past`(member.birthDate)를 추가했다. 국적은 `data-model.md`(ISO 3166-1 alpha-2 확정 명시)와 `APPLICATION.md`(언급 없음) 간 문서 충돌을 발견해 보고한 뒤, 사람이 "ISO 코드 기준 관리"로 확정해 커스텀 `@ValidNationality`(`Locale.getISOCountries()` 기반)를 추가했다. 이 판정 로직(`ApplicationFieldFormats`)은 개인 신청 DTO와 `BulkExcelParser`(단체 신청 엑셀 행 파싱) 양쪽에서 재사용해 개인/단체 검증 정책이 갈라지지 않게 했다. 전화번호 형식(`@Pattern`)과 생년월일 최소연도 제한은 각각 국제 전화번호 정책 미확정, 비즈니스 근거 부재로 이번 범위에서 제외하고 `@NotBlank`/`@Past`만 유지했다.
- 파일: `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `BulkExcelParser.java`, `domain/application/dto/validation/`(신규 — `ApplicationFieldFormats`, `ValidNationality`, `NationalityValidator`), `ApplicationCreateRequestValidationTest.java`(신규), `BulkExcelParserTest.java`
- 테스트: 신규 테스트를 구현 전 실패 확인 후 통과. 기존 테스트 픽스처(`nationality: "US"`, `birthDate: "1990-05-15"` 등)가 이미 새 규칙과 호환돼 회귀 없음 확인.
- 사유: `Application.photoRejectReason` 관련 논의 중 발견한, 신청 단계 입력값 검증이 충분한지에 대한 점검 요청에 따른 보강.
- 관련: `docs/collab/PENDING_DECISIONS.md` "국제 전화번호 형식 정책" 항목(후속 확정 필요)

## 2026-08-08 — Codex — `main` (UploadFile DB 저장 트랜잭션 이동)

- 변경: 신청 생성 경로에서 S3 업로드와 `UploadFile` DB 저장 책임을 분리. `ApplicationService`는 로고·직인·제출 ZIP을 S3에 먼저 업로드하고 `UploadedFileMetadata`만 전달하며, `ApplicationPersistenceService`가 동일 `@Transactional` 안에서 `UploadFile` row를 저장한 뒤 `Application`/`Applicant`/`Receiver`/`ApplicationMember`를 저장하도록 변경. 얼굴사진/멤버사진은 기존처럼 S3 key(`photoPath`)만 저장하고 `UploadFile` row를 만들지 않는다. 재업로드 경로는 기존 동작을 유지하되 `uploadFileToStorage` + `saveUploadFileMetadata` primitive 조합으로 책임 이름을 분리했다.
- 파일: `ApplicationService.java`, `ApplicationPersistenceService.java`, `UploadedFileMetadata.java`(신규), `ApplicationPersistenceServiceTest.java`
- 테스트: `./gradlew.bat test --tests "com.example.honorcitizen.domain.application.service.ApplicationPersistenceServiceTest" --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceUploadCompensationTest"` 통과. `./gradlew.bat test --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceTest" --tests "com.example.honorcitizen.domain.application.service.ApplicationServiceBulkTest" --tests "com.example.honorcitizen.domain.application.service.ApplicationServicePhotoReuploadTest"` 통과. 전체 `./gradlew.bat test`는 146개 중 `UserControllerTest` 2건, `UserApplicationFlowTest` 1건 실패 — 기존 사용자/Redis 환경 이슈로 이번 Application 변경과 무관.
- 사유: S3 객체는 DB 트랜잭션 밖에서 먼저 업로드하되, `UploadFile` row와 신청 관련 DB row는 하나의 트랜잭션으로 원자성을 보장하기 위함. DB 저장 실패 시에는 수동 DB 보상 삭제 없이 트랜잭션 rollback으로 정리하고, 바깥 서비스는 S3 key 역순 보상 삭제만 유지한다.
- 관련: UploadFile DB 저장 트랜잭션 이동 계획

## 2026-08-08 — Claude — `main` (GlobalExceptionHandler — Bean Validation 다중 필드 오류 응답)

- 변경: `MethodArgumentNotValidException` 처리 시 첫 번째 `FieldError`만 반환하던 것을 개선 — 위반된 모든 필드를 `ApiResponse.errors`(기존에 Bulk가 쓰던 `List<ValidationErrorDetail>` 필드를 그대로 재사용, 새 필드 추가 없음)에 담아 반환한다. `errors[]`는 `field` 기준으로 정렬해 `BindingResult` 내부 순서(스펙상 미보장)에 우연히 의존하지 않게 했다. 최상위 `errorMessage`는 하위 호환을 위해 기존과 동일하게 (정렬 전) 첫 번째 오류 메시지를 그대로 사용 — `errors[]` 정렬과 무관하게 유지. 중첩 DTO(`ApplicationCreateRequest.applicant.phone` 등) 경로는 Spring이 이미 `FieldError.getField()`에 점(.) 표기로 채워주므로 별도 처리 없이 그대로 노출됨. `ValidationErrorDetail.row`는 Bulk 전용 개념이라 이 경로에서는 항상 `null` — `PENDING_DECISIONS.md`에 후속 공통 오류 모델 정리 대상으로 기록.
- 파일: `GlobalExceptionHandler.java`, `GlobalExceptionHandlerTest.java`(신규 — 다중 필드 오류 시 `errors[]` 2건·중첩 경로·기존 `errorMessage` 호환성, 단일 필드 오류 시 기존과 동일한 단일-메시지 계약을 각각 검증)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. 전체 스위트 141개 중 `UserControllerTest` 2건(Redis 미기동)·`UserApplicationFlowTest` 1건(Redis 미기동)만 실패 — 전부 이번 변경 이전부터 있던 환경 문제이며 무관. 회귀 없음.
- 사유: Application 도메인 입력값 검증 작업 중 발견한 별도 이슈(클라이언트가 한 번의 요청으로 모든 필드 오류를 확인할 수 없음)를 앱 전체 공통 컴포넌트 변경으로 분리해 처리.
- 관련: `docs/collab/PENDING_DECISIONS.md` "GlobalExceptionHandler의 Bean Validation 다중 필드 오류 응답" 항목 해결

## 2026-08-07 — Claude — `main` (checklist.md §5 진행 상황 정리)

- 변경: `checklist.md` §5(미구현) 5개 항목을 실제 코드와 대조 — 4개는 §4 작업 과정에서 이미 구현 완료된 상태였음을 확인하고 `TODO.md`만 체크(코드 변경 없음, `checklist.md`는 수정하지 않음): `ApplicationPersistenceService` 신규(§4 "ApplicationPersistenceService 분리"), `BULK_APPLICATION_VALIDATION_FAILED`+`errors[]`(§4 "BulkExcelParser 학번 검증·errors[] 계약"), `application_seq.nextval` 채번(§4 "신청번호 DB Sequence 전환"), 업로드 추적 및 DB 실패 보상 삭제(§4 "업로드 보상 삭제" — `uploadedKeys` 추적 + 역순 `storageService.delete`가 §5 항목의 "확인 근거"였던 두 조건을 모두 충족). 남은 §5 항목은 "일일 KST 3회 제한 DB 원자 처리" 1건뿐 — 정책 문서에 "현재 리팩터링 범위 미구현"으로 명시된 저우선순위 항목.
- 파일: `docs/collab/TODO.md`
- 사유: `checklist.md` §4 작업이 §5의 상당 부분을 자연스럽게 해소했는지 실제로 확인하고 진행 상황을 정확히 반영.
- 관련: TODO "checklist.md §5 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — student_id 컬럼 길이 정리, §4 전체 완료)

- 변경: `checklist.md` §4 마지막 항목 구현 — `ApplicationMember.student_id` 컬럼을 `@Column(length = 50)` → `@Column(length = 10)`으로 변경. 개인 신청(`ApplicationService.isValidStudentId`)·단체 신청(`BulkExcelParser`) 양쪽 다 이미 10자·숫자만 통과시키므로 이 값을 넘는 값이 저장 경로에 도달할 수 없어 순수 스키마 정합성 정리. 이로써 `checklist.md` §4(수정 필요) 14개 행 전부 완료.
- 파일: `ApplicationMember.java`
- 테스트: 스키마 정의만 바뀌는 변경이라 신규 테스트 없이 Application/API 도메인 124개 전체 재실행으로 검증(`UserControllerTest` 2건, Redis 미기동, 무관만 실패) — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "학번 최대 10자·숫자 정책과 충돌한다."
- 관련: TODO "checklist.md §4 구현 진행" (§4 전체 완료, 다음은 §5 미구현 항목)

## 2026-08-07 — Claude — `main` (checklist.md §4 — Receiver 우편번호·기본주소 필수 검증)

- 변경: `checklist.md` §4 항목 구현 — `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `ReceiverRequest.zipCode`/`address`에 `@NotBlank` 추가(receiver 자체가 없으면 `@Valid`가 건너뛰므로 `MOBILE`엔 영향 없음). studentId 형식 검증은 개인 신청(`ApplicationService.isValidStudentId`, item6)과 단체 신청(`BulkExcelParser`, 직전 항목)에 이미 있어 DTO에 중복 추가하지 않음.
- 파일: `ApplicationCreateRequest.java`, `BulkApplicationCreateRequest.java`, `ApplicationControllerTest.java`(`createIndividualReturnsInvalidInputWhenReceiverZipCodeMissing` 신규), `ApplicationBulkControllerTest.java`(`createGroupReturnsInvalidInputWhenReceiverZipCodeMissing` 신규)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 124개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "Receiver 우편번호·기본주소 필수".
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — BulkExcelParser 학번 검증·errors[] 계약)

- 변경: `checklist.md` §4 나머지 항목(학번 검증·`ErrorCode`·`ApiResponse`·`BulkExcelParser` errors[])을 한 번에 구현 — 서로 강하게 얽혀 있어 하나로 묶음. 신규 `ValidationErrorDetail`(row·field·code·message) 레코드와 `BulkValidationException`(`CustomException` 상속) 추가. `ErrorCode`에 `BULK_APPLICATION_VALIDATION_FAILED` 추가, 미사용이 된 `ZIP_TOO_LARGE`/`EXCEL_NOT_FOUND`/`EXCEL_PARSE_ERROR` 제거(`APPLICATION_LIMIT_EXCEEDED`는 별도 §5 항목 몫이라 이번엔 추가 안 함). `ApiResponse`에 `errors` 필드(`@JsonInclude(NON_NULL)`) 추가. `GlobalExceptionHandler`에 `BulkValidationException` 전용 핸들러 추가. `BulkExcelParser.parseRow`는 필드별로 즉시 던지던 것을 `errors` 리스트에 수집하는 방식으로 바꿔 한 행이 잘못돼도 나머지 행을 계속 검사하고, 학번 형식(`\d{1,10}`) 검증도 추가. 엑셀 없음/2개 이상/데이터 없음도 동일한 `BulkValidationException` 계약으로 통일.
- 파일: `ValidationErrorDetail.java`(신규), `BulkValidationException.java`(신규), `ErrorCode.java`, `ApiResponse.java`, `GlobalExceptionHandler.java`, `BulkExcelParser.java`(전면 개편), `BulkExcelParserTest.java`(신규 3건 + 기존 3건 갱신), `ApplicationServiceBulkTest.java`(4건 갱신), `ApplicationBulkControllerTest.java`(1건 갱신)
- 테스트: 신규 테스트를 구현 전 컴파일 실패(신규 타입 부재) 확인 후 구현, 통과. Application/API 도메인 122개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "오류 하나라도 발생하면 부분 성공 없이 신청 전체를 실패 처리하고, 상세 오류를 errors[](행 번호·필드·코드·메시지)로 함께 반환한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — BulkExcelParser ZIP 루트·빈 행 정책)

- 변경: `checklist.md` §4 아홉 번째 항목 구현 — `BulkExcelParser.parse`가 ZIP 루트(경로에 `/`가 없는 항목)만 스캔하도록 변경: `.xlsx`는 후보로 모아 0개면 `EXCEL_NOT_FOUND`, 2개 이상이면 전체 실패(`EXCEL_PARSE_ERROR`)로 처리(기존엔 첫 `.xlsx`만 조용히 사용하고 나머지·하위 폴더 엑셀을 허용했음). 사진은 `photos/` 하위 대신 ZIP 루트에서 파일명으로 매칭(`__MACOSX/...`는 루트가 아니라 자동 제외, `.DS_Store`는 파일명으로 명시 무시). `parseExcel`은 ID가 빈 행에서 `break`해서 이후 행을 통째로 버리던 것을, 시트 마지막 행까지 순회하며 빈 ID 행만 `continue`로 건너뛰도록 변경 — 중간 빈 행 뒤의 유효한 데이터도 이제 정상적으로 읽힘.
- 파일: `BulkExcelParser.java`(parse/parseExcel, isRootEntry/isIgnoredEntry 신규), `BulkExcelParserTest.java`(신규 — 6개 테스트: ZIP 루트 사진 매칭, 하위 폴더 사진 무시, 엑셀 2개 이상 거부, 하위 폴더 엑셀 무시, `__MACOSX`/`.DS_Store` 무시, 중간·마지막 빈 행 무시), `ApplicationServiceBulkTest.java`/`ApplicationServicePhotoReuploadTest.java`/`ApplicationServiceUploadCompensationTest.java`/`ApplicationBulkControllerTest.java`(기존 zip 픽스처의 `photos/` 접두사를 ZIP 루트로 이동)
- 테스트: 신규 `BulkExcelParserTest` 6건을 구현 전 실패 확인(1건은 우연히 다른 이유로 이미 실패 상태였던 것 확인) 후 전부 통과. Application/API 도메인 120개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "Excel은 ZIP 루트에 정확히 1개, 2개 이상 전체 실패, 사진은 ZIP 루트에서 매칭한다", "중간 빈 행과 마지막 빈 행을 무시해야 한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 신청번호 DB Sequence 전환)

- 변경: `checklist.md` §4 여덟 번째 항목 구현 — 신청번호 채번을 `count+1`에서 진짜 DB Sequence(`application_seq`)로 교체. Hibernate `@SequenceGenerator`를 엔티티 ID 생성에 실제로 연결하지 않으면 ddl-auto가 시퀀스를 만들어주지 않는 것을 테스트로 확인해서, `schema.sql`에 `CREATE SEQUENCE IF NOT EXISTS application_seq`를 직접 선언하고 `spring.jpa.defer-datasource-initialization=true`+`spring.sql.init.mode=always`로 Hibernate DDL 이후 실행되게 설정. `ApplicationService.generateApplicationNumber`는 `EntityManager` native query(`SELECT nextval('application_seq')`)로 채번. 더 이상 쓰이지 않는 `ApplicationRepository.countByApplicationNumberStartingWith`는 제거(§4의 별도 "count+1 정리" 항목도 함께 해소).
- 파일: `application.properties`(schema init 설정 추가), `schema.sql`(신규), `Application.java`(미사용 `@SequenceGenerator` 시도 후 제거), `ApplicationService.java`(generateApplicationNumber, nextApplicationSequence 신규, EntityManager 필드 추가), `ApplicationRepository.java`(countByApplicationNumberStartingWith 제거), `ApplicationServiceTest.java`(`generateApplicationNumberNeverReusesSequenceEvenAfterExistingApplicationsAreDeleted` 신규)
- 테스트: 신규 테스트를 구현 전 실패 확인(처음엔 시퀀스 미생성으로 다른 이유로도 실패해서 원인 재확인 후 schema.sql 방식으로 수정) 후 통과. 전체 테스트 123개 중 `UserControllerTest` 2건 + `UserApplicationFlowTest` 1건(모두 Redis 미기동, 무관)만 실패 — 회귀 없음. `application.properties`를 건드린 변경이라 이번엔 도메인 범위가 아닌 전체 테스트 스위트로 재확인함.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "`count+1` 금지, `application_seq.nextval` 기반 DB Sequence 사용".
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 업로드 보상 삭제)

- 변경: `checklist.md` §4 일곱 번째 항목 구현 — (1) 생성 경로: `createIndividual`/`createGroup`이 업로드한 storage key를 순서대로 추적하고, `applicationPersistenceService.saveIndividual`/`saveGroup`이 실패하면 역순으로 `storageService.delete`를 호출한 뒤 원래 예외를 재던짐(고아 파일 방지). (2) 수정 경로: `reuploadPhoto`가 DB 갱신에 성공한 뒤 개인은 기존 사진, 단체는 기존 회원 사진 전체와 기존 제출 ZIP(`UploadFileRepository` 조회)을 삭제.
- 파일: `ApplicationService.java`(createIndividual/createGroup/reuploadPhoto, `storeUploadFile`/`storePhotoFile`/`storePhotoBytes`에 key-tracking 오버로드 추가, `deleteUploadedFilesReversed`/`deleteIfPresent` 신규), `ApplicationServiceUploadCompensationTest.java`(신규 — `createIndividualDeletesUploadedFilesInReverseOrderWhenPersistenceFails`, `createGroupDeletesUploadedFilesInReverseOrderWhenPersistenceFails`), `ApplicationServicePhotoReuploadTest.java`(`reuploadPhotoForIndividualDeletesOldPhotoFile`, `reuploadPhotoForGroupDeletesOldMemberPhotosAndOldSubmitFile` 신규)
- 테스트: 신규 테스트 4건을 구현 전 실패 확인 후 통과. Application/API 도메인 113개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "DB 실패 시 요청 업로드 파일 역순 보상 삭제, 파일 수정 시 DB 갱신 성공 후 기존 파일 삭제가 필요하다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 학번 형식 검증)

- 변경: `checklist.md` §4 여섯 번째 항목 구현 — 개인 신청 경로(`ApplicationService.validateStudentFields`)에서 학생증 학번이 최대 10자·숫자만 허용하도록 형식 검증 추가(`\d{1,10}` 정규식). 단체 신청 경로(`BulkExcelParser`)는 이번 항목 범위 밖(별도 TODO 항목에서 처리 예정).
- 파일: `ApplicationService.java`(validateStudentFields, isValidStudentId 신규), `ApplicationServiceTest.java`(`createIndividualRejectsStudentIdWithNonDigitCharacters`, `createIndividualRejectsStudentIdLongerThanTenDigits` 신규)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 109개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "학번은 필수·최대 10자·숫자만 허용한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — Applicant.email 요청값 반영)

- 변경: `checklist.md` §4 다섯 번째 항목 구현 — `Applicant.email`을 항상 `User.email`로 고정 저장하던 것을, 요청(`applicant.email`)이 있으면 그 값을, 없으면 `User.email`을 기본값으로 저장하도록 변경. `ApplicationCreateRequest`/`BulkApplicationCreateRequest`의 `ApplicantRequest`에 `email` 필드 신규 추가(검증 annotation 없음 — 신청 화면에서 자유롭게 수정 가능해야 하므로).
- 파일: `ApplicationCreateRequest.java`(`ApplicantRequest.email`), `BulkApplicationCreateRequest.java`(`ApplicantRequest.email`), `ApplicationService.java`(createIndividual/createGroup), `ApplicationServiceTest.java`(`createIndividualSavesApplicantEmailFromRequestWhenProvided`, `createIndividualFallsBackToUserEmailWhenApplicantEmailBlank` 신규), `ApplicationServiceBulkTest.java`(`createGroupSavesApplicantEmailFromRequestWhenProvided` 신규)
- 테스트: 신규 테스트 3건을 구현 전 실패 확인 후 통과. Application/API 도메인 107개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "User.email은 기본값이며 신청 화면에서 수정 가능해야 한다."
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — sameAsApplicant 복사 범위 제한)

- 변경: `checklist.md` §4 네 번째 항목 구현 — `sameAsApplicant=true`여도 배송지(우편번호·주소·상세주소·배송메모)는 항상 요청의 `receiver` 값을 저장하도록 변경(기존엔 `copyFromApplicant`/`copyIndividualReceiver`가 이 필드들을 전부 `null`로 덮어씀). 이름·연락처는 요청값이 비어 있을 때만 Applicant 값으로 대체(fallback) — `ReceiverRequest`에 검증 annotation이 없어 빈 값 제출이 가능하기 때문.
- 파일: `ApplicationPersistenceService.java`(saveReceiverIfNeeded/saveGroupReceiverIfNeeded), `ApplicationServiceTest.java`(`createIndividualCopiesReceiverFromApplicantWhenSameAsApplicantTrue`→`createIndividualUsesSubmittedReceiverAddressEvenWhenSameAsApplicantTrue`로 갱신 + `createIndividualFallsBackToApplicantNameAndPhoneWhenReceiverFieldsBlank` 신규), `ApplicationServiceBulkTest.java`(`createGroupUsesSubmittedReceiverAddressEvenWhenSameAsApplicantTrue` 신규 + `requestWithPhysicalReceiverSameAsApplicant` 헬퍼)
- 테스트: 갱신/신규 테스트 3건을 구현 전 실패 확인 후 통과. Application/API 도메인 104개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영 — "이름·연락처는 자동 복사 후 수정 가능, 배송지는 Receiver가 항상 입력".
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — MOBILE+Receiver 거절)

- 변경: `checklist.md` §4 세 번째 항목 구현 — `issueType=MOBILE`인데 `receiver`를 전달하면 `INVALID_INPUT`으로 거절(기존엔 `MOBILE_AND_PHYSICAL`인데 `receiver` 없는 경우만 검증했음). 개인(`validateReceiverPresence`)·단체(`validateGroupReceiverPresence`) 둘 다 반대 방향 검증 추가.
- 파일: `ApplicationService.java`, `ApplicationServiceTest.java`(`createIndividualRejectsReceiverWhenMobile` 신규), `ApplicationServiceBulkTest.java`(`createGroupRejectsReceiverWhenMobile` 신규 + `requestWithMobileAndReceiver` 헬퍼)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 102개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영.
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — ApplicationPersistenceService 분리)

- 변경: `checklist.md` §4 두 번째 항목 구현 — `ApplicationService`를 비트랜잭션 오케스트레이터로, DB 저장을 신규 `ApplicationPersistenceService`(`@Transactional`)로 분리. self-invocation으로 인해 `@Transactional`이 무력화되는 문제를 막기 위해 별도 Bean으로 도입(`APPLICATION.md` §5). `saveIndividual()`/`saveGroup()`이 Application→Applicant→Receiver(조건부)→ApplicationMember 순서로 한 트랜잭션에 저장. 단체 신청은 파일 업로드(트랜잭션 밖)와 DB 저장(트랜잭션 안) 사이를 넘기기 위해 `GroupMemberUpload`(row+photoPath) record 신규 도입.
- 파일: `ApplicationPersistenceService.java`(신규), `GroupMemberUpload.java`(신규), `ApplicationService.java`(createIndividual/createGroup 및 관련 private 메서드 이동), `ApplicationPersistenceServiceTest.java`(신규 3테스트)
- 테스트: 신규 테스트를 클래스 부재로 컴파일 실패 확인 후 구현, 통과. Application/API 도메인 100개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영.
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Claude — `main` (checklist.md §4 — 학생증 직인 선택 구현)

- 변경: `checklist.md` §4 첫 항목 구현 — 학생증 학교 직인(seal)을 필수 → 선택으로 변경. `ApplicationController.createGroup`의 `seal` 파트를 optional로 변경(개인 신청 `schoolSeal`은 이미 optional이었음). `ApplicationService.validateStudentFields`는 학번·학과·로고만 필수로 검사하고 직인은 있을 때만 형식 검증. `createIndividualApplication`/`createGroup`은 직인이 없으면 업로드를 건너뛰고 `sealFileId=null`로 저장.
- 파일: `ApplicationController.java`, `ApplicationService.java`(validateStudentFields/createIndividualApplication/createGroup), `ApplicationServiceTest.java`(`createIndividualForStudentCardSucceedsWithoutSchoolSeal` 신규), `ApplicationServiceBulkTest.java`(`createGroupSucceedsForStudentCardWithoutSeal` 신규)
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application/API 도메인 97개 중 `UserControllerTest` 2건(Redis 미기동, 무관)만 실패 — 회귀 없음.
- 사유: `APPLICATION.md`/`checklist.md` 기준 구현 반영(사용자 승인된 작업 방식 — TDD, 최소 범위, 완료마다 TODO/CHANGELOG 갱신).
- 관련: TODO "checklist.md §4 구현 진행"

## 2026-08-07 — Codex — Application 정책 문서 동기화 및 Code Audit

- APPLICATION.md와 POLICY_SYNC_CHECKLIST.md에 맞춰 requirements/data-model/api/checklist 및 운영 문서를 동기화했다.
- 실제 Controller, Service, Validator, Factory, Entity, DTO, Repository, ErrorCode, Test 구현을 파일·클래스·메서드·라인 단위로 재검증해 수정 필요·미구현 항목을 기록했다.
- 코드 파일은 수정하지 않았다.
- (Claude 추가 2026-08-07: 학생증 `department`(학과) 필드 삭제는 이번 동기화에서 제외 — 사람이 미결정 상태로 확인, `PENDING_DECISIONS.md` 참고)

## 2026-08-06 — Claude — `main` (마이페이지 신청 목록/상세 조회 API 설계)

- 변경: 로그인 사용자가 자기 신청 내역을 목록(`GET /api/my/applications`, 페이징+status 필터)/상세(`GET /api/my/applications/{id}`, 소유권 검증)로 조회하는 API 2개 설계. 기존 `POST /api/applications/lookup`(API 3)은 비로그인 공개 조회용이라 이 용도로 못 씀 — 사용자가 "로그인한 경우 다건조회 → 클릭 시 단건조회" 흐름을 요청해서 추가.
- 파일: `docs/specs/application/api.md`(API 6/7 신규), `docs/collab/TODO.md`
- 사유: Application 도메인 현재 구현 상태(생성만 완성, 조회/수정/취소는 부분적/미구현)를 사용자가 점검하다가 발견한 갭.
- 참고: 설계까지만 진행, 구현은 다음 확인 후 — `ApplicationRepository.findByUserId(...)` 신규 필요, `PageResponse<T>`는 Review 목록조회와 공유하는 첫 실제 사용처가 될 예정.
- 관련: TODO "마이페이지 신청 목록/상세 조회 API 6·7 설계"

## 2026-08-06 — Claude — `main` (Review 자격 검증 정책 확정)

- 변경: 후기 작성 자격을 `Application.user_id`(제출 계정) 대신 **이메일 매칭**(`Applicant.email` 또는 `ApplicationMember.email`이 로그인 계정 이메일과 일치)으로 검증하도록 확정. 단체 신청은 대표 제출자뿐 아니라 실제 카드를 받은 구성원 개인(같은 이메일로 별도 가입된 경우)도 자격을 인정 — `lookup` API의 본인확인 방식과 같은 사고방식. 신규 `REVIEW_NOT_ELIGIBLE`(403) 에러코드 추가.
- 파일: `docs/specs/review/data-model.md`(§2.1 신규), `docs/specs/review/api.md`, `docs/collab/TODO.md`
- 사유: 사용자 지적 — "단체신청도 개인에서는 카드 정보를 조회할 수 있는 사람만" — 기존 설계(신청유형/카드종류를 자유 입력받는 자기신고 방식)로는 신청 경험이 없는 사람도 후기를 쓸 수 있어서 수정.
- 관련: TODO "Review 자격 검증 정책 반영"

## 2026-08-06 — Claude — `main` (Review 도메인 설계)

- 변경: 후기(Review) 작성 요구사항 변경에 따라 Entity/API 설계. `Review`(작성자 실계정 `user_id`와 화면표시 `author_display_name`을 분리, `application_type` 재사용, 신청 실체와 FK 연결 없이 자기신고 값), `ReviewCardType`(`@ElementCollection`, `CardTypeCode` 다중 선택), `ReviewImage`(`UploadFile` 재사용 + `review_id`/`upload_file_id`/`display_order` join). API 3개(등록/목록조회/단건조회) 설계, 목록 응답 최소 4필드로 제한, 프로젝트 첫 페이징 응답 포맷(`PageResponse<T>`) 제안. `docs/api/upload-file.md`에 있던 옛 "`Review.thumbnail_file_id`(단일)" 가정을 대체.
- 파일(신규): `docs/specs/review/{data-model,api}.md`. 파일(수정): `docs/api/{README,upload-file,board}.md`, `arch.md`(§4.7 Review 모듈 신설, 기존 Board는 §4.8로 이동, §5.3/§5.5 갱신), `docs/collab/TODO.md`
- 사유: 사용자 요청 — "후기 기능 요구사항 변경, Entity/API 설계까지만(구현 금지)"
- 참고: [TBD] 3건(카드종류 0개 허용 여부/본문 최대 글자수/조회수 노출 여부)과 사진 첨부 최대 개수는 확인 후 반영 필요 — TODO.md에 기록. 구현·프론트 반영은 이번 범위 밖.
- 관련: TODO "Review 도메인 설계"

## 2026-08-06 — Claude — feature/application-domain-impl (조회 인증 정책 + CardType ID 고정)

- 변경: (1) `ApplicationService.lookup()`을 method별로 분리 검증하도록 수정 — `method=application`은 phone·email 둘 다 필수+둘 다 일치해야 함(기존엔 OR), `method=card`는 phone·email 검증을 아예 제거(카드번호 단독 조회). (2) 신규 `CardTypeSeeder`(`CommandLineRunner`)를 추가해 최초 기동 시 `HONOR_KOREAN=1, HONOR_CITIZEN=2, VISITOR=3, STUDENT=4` 순서로 시딩 — 프론트가 `cardTypeId`를 1~4로 하드코딩해서 쓰는 것을 그대로 허용하기 위함(신규 `GET /api/card-types` API는 만들지 않기로 결정).
- 파일: `ApplicationService.java`(lookup/lookupByCard/matches), `ApplicationServiceLookupTest.java`(신규 케이스로 재작성), `domain/card/CardTypeSeeder.java`(신규)
- 사유: 프론트 `LookupPage.tsx`/`ApplyPage.tsx` 실제 구현 대비 UI-API 갭 분석 결과를 사람이 확인하고 확정한 정책. `backend/FRONTEND_API_REQUIREMENTS.md`(main 브랜치)에 결정 배경 상세 기록.
- 테스트: `ApplicationServiceLookupTest` 전체 재작성 후 통과. Application/API 도메인 테스트 95개 중 기존 `UserControllerTest` 2건(Redis 미기동 환경 의존, 무관)만 실패.
- 참고: 같은 갭 분석에서 나온 다른 결정 3건(단체 파일은 `logo`/`seal`/`submitFile` 3파트 유지, 단체 재제출은 이미 백엔드 구현 완료·프론트 UI만 남음, `englishName`은 언어 무관 필드로 확정)은 코드 변경이 필요 없어 `FRONTEND_API_REQUIREMENTS.md`에만 기록. 이 커밋엔 Task 4~6 로드맵의 다른 진행 중 변경(ApplicationFactory 등)은 포함하지 않음 — 파일 단위로 분리해서 커밋함.
- 관련: 2026-08-06 UI/API 갭 분석

## 2026-08-01 — Claude — `backend-api` (User/Application End-to-End 플로우 테스트)

- 변경: 실사용자 시나리오 기준 통합 테스트(`UserApplicationFlowTest`) 추가 — Google 로그인(OAuth2SuccessHandler와 동일 코드 경로로 재현) → 인증 유지(Cookie/JWT, 이후 전 구간 동일 쿠키 재사용) → 개인 신청 생성 → 신청 조회(lookup) → 신청 상태 전이(관리자 검토·반려를 엔티티 레벨로 재현, HTTP API 없음) → Lookup 재조회(반려 사유 노출 확인) → 사진 재업로드 → DB 최종 상태 검증까지 8단계를 한 테스트로 연결. 각 단계 HTTP 상태코드/Response/DB 상태/(모킹된) 파일 업로드 호출을 전부 검증.
- 로컬에 Redis가 없어 처음엔 기존 `UserControllerTest`의 탈퇴 테스트 2개와 이 신규 테스트가 전부 막혀 있었음 — Docker로 Redis 컨테이너를 띄워서 실제로 통과하는 것까지 확인(이 컴퓨터엔 다른 프로젝트("zerotime")의 Redis가 이미 6379를 점유 중이라 별도 컨테이너를 다른 포트로 띄움 — `REDIS_PORT` 오버라이드는 로컬 전용이라 커밋 안 함).
- 테스트 작성 중 버그 발견: `@Transactional` 테스트 메서드에서 `deleteAll()` 직후 `save()`를 호출하면 Hibernate의 flush 순서(Insert가 Delete보다 먼저 실행됨) 때문에 유니크 제약 위반 발생 — 이 프로젝트의 나머지 테스트들이 전부 비-`@Transactional`(각 리포지토리 호출이 즉시 커밋)인 이유와 일치, 새 테스트도 동일 컨벤션으로 맞춤.
- 파일: `backend/honor-citizen/src/test/java/com/example/honorcitizen/flow/UserApplicationFlowTest.java`
- 사유: 사용자 요청 — "API 단위 테스트는 충분, 이제 실제 사용자 시나리오 기준 User Flow Integration Test 작성"
- 관련: 전체 테스트 83/83 통과(Redis 가용 시)

## 2026-08-01 — Claude — `backend-api` (전체 코드베이스 감사 + 정리)

- 변경: 사용자 요청으로 전체 백엔드 감사(① 문서에 없는 코드 ② 코드에 없는 문서 ③ 호출자 없는 클래스 ④ 호출자 없는 API ⑤ 아키텍처 위반 ⑥ 실행 안 되는 코드) 수행 후 삭제 가능 항목 정리. `infra/card/*`(5개 파일, CitizenCard 삭제 후 orphan) 삭제. `domain/photo/*` + `api/UploadController.java` 삭제(프론트 `src/` 검색 결과 호출 없음 확인, `docs/api/upload-file.md`도 "독립 API 불필요"로 이미 결론). `domain/user/dto/{TokenRefreshRequest,TokenRefreshResponse}`(정의만 되고 미사용) 삭제. `ErrorCode`에서 `DUPLICATE_APPLICATION`(미사용)과 domain/photo 삭제로 연쇄 orphan된 `UNSUPPORTED_FILE_TYPE`/`INVALID_IMAGE`/`INAPPROPRIATE_IMAGE`/`PHOTO_NOT_FOUND`/`PHOTO_EXPIRED`/`PHOTO_OWNER_MISMATCH` 제거. **아키텍처 위반 수정**: `ApplicationService`가 `UserRepository`를 직접 주입하던 것 — arch.md "다른 도메인의 Repository를 생성자 주입하지 않는다" 위반 — `UserService.findById()` 경유로 교체.
- 파일: `infra/card/*`(삭제), `domain/photo/*`(삭제), `api/UploadController.java`(삭제), `domain/user/dto/TokenRefresh{Request,Response}.java`(삭제), `common/exception/ErrorCode.java`, `domain/application/service/ApplicationService.java`, `docs/collab/TODO.md`
- 사유: "다 갈아엎고 지금 쓰레기 클래스 없음?" 질문에서 시작된 전체 감사 요청, 결과 승인 후 정리 실행
- 관련: TODO "전체 코드베이스 감사 + 죽은 코드/아키텍처 위반 정리"

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 5 완료)

- 변경: IDENTITY와 Root 선저장 순서를 유지하는 package-private ApplicationFactory를 추가하고 개인 신청의 Application, Applicant, Receiver, ApplicationMember 생성 책임을 Service에서 이동. Service는 검증, 파일 준비, 생성 호출, 저장 순서를 조정.
- 파일: ApplicationFactory.java, ApplicationService.java, ApplicationFactoryTest.java, docs/specs/application/requirements.md, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: CreatedApplication, CreatedChildren, Context 없이 최소 구조로 Entity 생성 책임을 독립시키기 위함.
- 테스트: Factory 테스트를 구현 전 클래스 부재로 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 101개 중 기존 UserControllerTest 2건만 실패.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 5

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 4 완료)

- 변경: Application 생성 시 서버 값의 책임을 확정. 수령인 동일 여부 계산을 ApplicationCreateRequest의 파생 메서드로 이동하고 Service는 이를 사용하도록 정리. prepareServerValues, Context, Factory는 추가하지 않음.
- 파일: ApplicationCreateRequest.java, ApplicationService.java, ApplicationCreateRequestTest.java, docs/specs/application/requirements.md, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: 신청번호는 Service, 초기 상태는 Entity, 수령인 동일 여부는 Request의 독립된 책임이므로 별도 준비 객체로 묶을 필요가 없음.
- 테스트: 신규 DTO 테스트 2건을 구현 전 메서드 부재로 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 99개 중 기존 UserControllerTest 2건만 실패.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 4

## 2026-08-01 — Codex — feature/application-domain-impl (Application Task 4 정책 확정)

- 변경: 상담 후 신청하고 신청 이후 계좌이체하는 흐름을 확정하여 Application 생성 시 total_price를 계산·저장하지 않도록 문서를 정정. 단체 신청은 엑셀 ID와 ZIP 사진 파일명을 매칭하되 ID를 저장하거나 구성원별 사진 파일 ID를 생성하지 않는 것으로 확정.
- 파일: docs/specs/application 문서, docs/collab/TODO.md, docs/collab/CHANGELOG.md
- 사유: Task 4 구현 전에 가격과 파일 식별 정책을 확정하여 불필요한 서버 생성값과 중간 구조 도입을 방지.
- 관련: Application 개인 신청 리팩터링 로드맵 Task 4

## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 3)

- 변경: `ApplicationPhotoValidator`를 추가해 얼굴사진과 학생증 학교 로고·직인의 5MiB, 확장자, MIME, signature, 디코딩을 검증. 얼굴사진은 EXIF Orientation 적용 후 300×400 최소 해상도를 검증하고 학교 파일은 해상도에서 제외. 학생증 필수값은 기존 Service private 메서드에서 공백까지 거절하도록 보강.
- 파일: `ApplicationPhotoValidator.java`, `ApplicationService.java`, `ApplicationPhotoValidatorTest.java`, `ApplicationServiceTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/{requirements,api}.md`, `docs/collab/{TODO,CHANGELOG}.md`
- 사유: Application Task 3의 확정 사진·학생증 정책을 업로드와 DB 저장 전에 적용. StudentCardValidator 별도 클래스는 구조 변경 원칙상 추가하지 않음.
- 테스트: Validator 정책 테스트 8건과 Service 통합 테스트 2건 추가. Application 관련 전체 테스트 통과. 전체 97개 중 기존 `UserControllerTest` 2건만 실패.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 3"
## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 2)

- 변경: 기존 `UserService.findEligibleApplicationUser()`에서 회원 존재·ACTIVE·USER 권한·필수 약관을 검증하고 User를 반환하도록 구현. ApplicationService의 UserRepository 직접 의존을 제거하고 UserService를 사용. 하루 3회 제한은 정책 보류에 따라 미구현.
- 파일: `domain/user/service/UserService.java`, `domain/application/service/ApplicationService.java`, `ApplicationServiceTest.java`, `ApplicationControllerTest.java`, `docs/specs/application/api.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`
- 사유: Application Task 2의 확정 정책을 기존 ErrorCode로 구현하고 도메인 간 Repository 직접 참조를 제거.
- 테스트: 신규 상태·권한·약관 테스트 3건을 구현 전 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 87개 중 기존 `UserControllerTest` 2건만 실패.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 2"
## 2026-08-01 — Codex — `feature/application-domain-impl` (Application Task 1)

- 변경: `createIndividual()`을 private 메서드 중심으로 분리하고 User 조회를 첫 단계로 이동. User 미존재 시 CardType 조회·신청번호 생성·파일 업로드·DB 저장 전에 `USER_NOT_FOUND`로 중단하도록 테스트 우선으로 보장. Factory/Validator/Context는 추가하지 않음.
- 파일: `domain/application/service/ApplicationService.java`, `domain/application/service/ApplicationServiceTest.java`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`
- 사유: Application 개인 신청 리팩터링 Task 1. 이후 로직의 전제조건인 User 존재를 부수효과보다 먼저 검증하고, 최소 구조 변경으로 Service 가독성을 개선.
- 테스트: 신규 테스트 2건을 구현 전 실패 확인 후 통과. Application 관련 전체 테스트 통과. 전체 84개 중 기존 `UserControllerTest` 2건만 실패하며 Task 전후 동일.
- 관련: TODO "Application 개인 신청 리팩터링 로드맵 — Task 1"

## 2026-08-01 — Claude — `feature/application-domain-impl` (API 4/5 + merge)

- 변경: API 4(`PATCH /api/applications/{id}/photo`, 사진 재업로드)·API 5(`GET /api/applications/{id}/cards/download`, 카드 다운로드) 구현으로 Application 도메인 5개 API 전부 완료. `backend-api`(Codex의 `docs/specs/application/*` 재구성, `ecd72b3`)를 이 브랜치로 머지(`docs/collab/*` 4개 파일만 충돌, RULES.md 7절 방식대로 수동 재작성). `ErrorCode`에서 삭제된 레거시 도메인 전용 코드 정리, `CARD_NOT_READY` 재사용. 컴파일이 깨진 orphan `infra/toss/*`(Payment 도메인 삭제 후 미사용) 삭제. `checklist.md` 6개 섹션 자체 검증(결과는 HANDOFF.md).
- 파일: `domain/application/service/ApplicationService.java`, `domain/application/dto/{ApplicationPhotoReuploadResponse,ApplicationCardDownloadResponse}.java`, `api/ApplicationController.java`, `common/exception/ErrorCode.java`, `infra/toss/*`(삭제), 테스트 4개 클래스(신규 21테스트)
- 사유: 사용자 승인("응") 후 API 4/5 이어서 구현, 그 직전 "변경사항을 받아오고" 요청으로 backend-api 병합 선행
- 관련: TODO "Application 도메인 엔티티/API 구현"(완료로 갱신)

## 2026-08-01 — Claude — `feature/application-domain-impl`

- 변경: `docs/specs/application/*` 기준으로 Application 도메인 재구현. 옛 BulkOrder/CitizenCard/KoreanName/Payment/Shipping 도메인 및 옛 Application/CardType/ApplicationStatus 삭제. 신규 CardType/CardDesign/UploadFile/Applicant/Receiver/ApplicationMember 엔티티 + 재작성된 Application(8단계 상태머신). API 1(개인 신청)/API 2(단체 ZIP 신청, 신규 BulkExcelParser)/API 3(신청 조회 lookup) 구현. GlobalExceptionHandler에 MissingServletRequestPartException 핸들러 추가.
- 파일: `domain/application/*`, `domain/card/*`, `domain/uploadfile/*`, `api/ApplicationController.java`, `common/enums/*`, `common/exception/GlobalExceptionHandler.java`, `infra/security/SecurityConfig.java`, 테스트 다수
- 사유: 사용자 요청("Application 구현 전 docs/specs/application 읽고 시작") + 5개 확정 문서(requirements/data-model/api/checklist + arch.md) 기준 구현
- 관련: TODO "Application 도메인 엔티티/API 구현 착수", HANDOFF.md의 "확인 필요" 항목(englishName/total_price/엑셀실패정책 — 사람 확인 완료, docs/specs/application 반영은 Codex 몫)

## 2026-08-01 — Codex — `feature/application-domain-docs`

- 변경: Application 문서를 `docs/specs/application/` 아래의 `requirements.md`/`data-model.md`/`api.md`/`checklist.md`로 패키지화하고 기존 경로 참조를 갱신.
- 파일: `docs/specs/application/*`, `DB.md`, `docs/api/README.md`, Application 경로를 참조하는 협업·테스트 문서
- 사유: Application 업무 규칙, 데이터 모델, API 계약과 검증 영역을 한 도메인 폴더에서 찾을 수 있도록 Source of Truth 구조를 정리.
- 관련: TODO "Application 문서 도메인 패키지 이전"

## 2026-08-01 — Claude — `backend-api`

- 변경: 협업 규칙 체계(`docs/collab/`) 신설 — `RULES.md`/`TODO.md`/`CHANGELOG.md`/`HANDOFF.md` 추가. 기존 `guide.md`는 `RULES.md`로 가리키는 안내문으로 축소.
- 파일: `docs/collab/RULES.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md`, `guide.md`
- 사유: Claude(구현)와 Codex(문서/아키텍처)가 각자 다른 워크트리에서 작업하면서 서로의 변경사항을 사람이 매번 전달해야 하는 문제를 해결하기 위해, 작업 시작/종료 시 반드시 확인·갱신하는 공용 문서 체계를 만듦.
- 관련: TODO #1

## 2026-08-01 — Claude — `feature/application-domain-docs` (codex-docs 워크트리)

- 변경: `arch.md`의 계층 구조를 4계층(API/Application/Domain/Infrastructure) + Port/Adapter에서, 실제 코드 규모에 맞는 3단 구조(Controller/Service/Repository·Entity, Infra)로 단순화. 패키지 구조 예시를 실제 코드(`api/`, `domain/{도메인}/{entity,repository,service,dto}`, `infra/`)와 일치시킴. Command/Query 강제 분리 규칙 완화, 테스트 섹션 네이밍을 실제 컨벤션(`{Class}Test`/`{Class}ServiceTest`/`{Class}ControllerTest`)에 맞춤.
- 파일: `arch.md`
- 사유: 사용자가 "현재 구조에서는 과한 설계이니 규모에 맞게 축소해달라"고 요청. 비즈니스 규칙(4~18절 대부분)은 그대로 유지.
- 관련: -
