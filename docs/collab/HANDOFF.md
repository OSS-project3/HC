# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-09-01
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ "4-D. 학생증 템플릿 업로드 API" 구현+테스트+실제 통합 검증+push 완료(Claude, 2026-09-01)**:
  - `GET/POST /api/admin/schools/{schoolId}/card-template` 신규 — 관리자가 배포 없이 학교별 학생증 카드 템플릿(앞/뒤)을 등록·교체한다. `SchoolCardTemplateService`(비-transactional, S3 업로드+정리)+`SchoolCardTemplatePersistenceService`(`@Transactional`, UploadFile/CardDesign 반영) 2-서비스 분리 — `CardGenerationService`/`CardGenerationPersistenceService`와 같은 패턴(4-D 정책 문서는 원래 `ApplicationService.registerS3CleanupAfterTransaction` 재사용을 지시했는데, 구현 시작 시 Card 모듈 안에 더 가까운 선례가 있다는 걸 발견해 그쪽을 따름 — 결과는 동일한 보장).
  - `SchoolCardTemplateValidator`(PNG 시그니처+카드 비율 235:156±5%+최소 해상도 800px), `CardDesign.replaceTemplates()` mutator 신규, `SchoolService.getSchoolNameOrThrow()`(Card→School 참조를 Repository 직접 주입 없이), `student_card_design_seq` DB 시퀀스+`card_designs_school_orientation_idx` unique 인덱스(`schema.sql`) 신규, `ErrorCode.SCHOOL_NOT_FOUND`/`CARD_TEMPLATE_INVALID_RESOLUTION` 추가.
  - **착수 전 검토 단계에서 사용자와 함께 정정한 것**(구현 전에 이미 결정 완료 상태로 시작): `isDefault`는 `true`→`false`로 정정(GET /api/admin/card-designs 자체를 프론트가 안 부른다는 것까지 확인), MIME 에러코드는 신규 대신 기존 `UNSUPPORTED_FILE_TYPE` 재사용, `designNumber` 채번은 DB Sequence, 학교 불변조건은 DB Unique Index.
  - **구현 중 추가로 발견해 정정한 것**: 불변조건 인덱스를 원래 partial unique index(`WHERE active=true`)로 설계했으나, 테스트용 H2 2.4.240이 `CREATE INDEX ... WHERE` 구문 자체를 지원하지 않아(실측 확인) 조건 없는 일반 unique index로 변경 — STUDENT `CardDesign`을 비활성화하는 코드 경로가 현재 없어 실질적 영향 없음(향후 그런 경로가 생기면 재검토 필요, `schema.sql` 주석 참고).
  - **테스트**: `SchoolCardTemplateServiceTest`(9, 신규 생성/교체/조회/각종 검증 실패), `AdminSchoolCardTemplateControllerTest`(6, 인증/권한/multipart 바인딩), **`SchoolCardTemplateEndToEndTest`(1, 사용자가 명시적으로 요구한 통합 테스트)** — 실제 4-D 업로드 API로 실제 디자이너 템플릿을 등록하고, 그 `CardDesign`으로 실제 `CardPreviewService.preview()`(프로덕션 API)까지 실행해 렌더링 결과를 파일로 남기고 육안 확인함(이름·영문명·학번·학과·발급일자·띠 이미지 전부 정상). 전체 스위트 801개(Redis 포함) 재실행 통과.
  - `arch.md` 갱신(RULES.md §5 요구): School 모듈(§4.10)이 문서에 아예 없던 걸 뒤늦게 채움(구현 자체는 4-A에서 이미 완료돼 있었음), 모듈 의존 매트릭스에 `Card→School`/`Application→School` 추가, Card 모듈의 낡은 서술("디자인 배정 시점 미결정", STUDENT가 "학교 로고/직인"을 쓴다는 서술 — 실제로는 4-C에서 STUDENT는 로고·직인을 아예 안 그리는 걸로 확정됨) 정정.
  - 상세 근거는 `TODO.md` "4-D" 절, `docs/api/school-card-template.md`(신규), 커밋 diff 참고.
- **✅ "4-E. 카드 렌더링 CJK 폰트 fallback" 구현 완료(Claude, 2026-09-01)**: 4-C 검증 중 KoPub 폰트가 못 그리는 한자(예: 실제 이름 한자 昡, U+6621)가 네모(□)로 깨지는 걸 발견 → 주 폰트는 유지하고 `Font.canDisplayUpTo()`로 필요한 글자만 CJK fallback 폰트(`NotoSansKR-Regular.otf`, SIL OFL, 애플리케이션 리소스로 직접 번들)로 그리도록 STUDENT 전용이 아니라 `CardImageCompositor`의 4개 공용 텍스트 primitive(`drawTextGeneric`/`leftEdgeXGeneric`/`drawTextAtPixelXGeneric`/`drawBackText`)에 공통 구현 — 주 폰트가 문자열 전체를 지원하는 기존 렌더링(4종 카드 전부)은 코드 경로 자체가 안 바뀌어 회귀 없음. 실 렌더링으로 "金緑洙" 스타일 혼합 문자열이 진짜 글리프로(네모 아님) 그려지는 것 확인. **완전한 유니코드 커버리지는 목표가 아니었고 실측으로도 아님**(fallback 폰트도 일부 CJK 확장A 문자는 여전히 못 그림) — KoPub 단독보다 넓힌 것.
- **⚠️ 4-C 완료 후 발견해 그 자리에서 고친 추가 버그(2026-09-01, 4-D 착수 전)**: `STUDENT_BACK` PORTRAIT 좌표 중 영문명 y좌표가 한자 있는 학생 렌더링 시 한자 줄과 겹치는 걸 발견(세로형+한자 조합을 이전엔 실제로 렌더링 안 해봤음) — 영문명 좌표만 보정(`f907500`). 이 과정에서 세로형 뒷면 "이름풀이" 텍스트 위치 자체는 원래도 정상이었다는 것도 확인(사용자 질문 계기로 재확인).
- **⚠️ 로컬 테스트 환경 참고 — Redis**: 전체 스위트를 실행하려면 `honor-citizen-redis-test` 컨테이너(호스트 포트 **6400**)가 필요하다 — Docker Desktop이 꺼져 있으면 실행 후 `docker start honor-citizen-redis-test`, `REDIS_PORT=6400 ./gradlew.bat test`. 이번 세션엔 컨테이너가 존재했지만(이전 세션이 만들어둠) 꺼져 있었다 — 시작만 하면 됨. 아예 없다면 `docker run -d --name honor-citizen-redis-test -p 6400:6379 redis:7-alpine`로 새로 만들 것.
- **이전 세션들(2026-08-19~2026-08-31) 요약** — 전부 완료·커밋·push됨, 상세는 `docs/collab/CHANGELOG.md` 해당 날짜 항목 참고:
  - "3. 카드 생성·저장 — 최소 버전", "4-B. CardDesign 학교 매칭", "4-C. 학생증 카드 앞/뒷면 렌더링" 전부 완료.
  - 프론트-백엔드 정책 갭 감사(`docs/FRONTEND_API_GAPS.md`) — 만세력/진태양시 보정, 성씨(surname) 필드 프론트 미처리 발견·문서화.
  - School 마스터 엔티티 + 검색select, 계정 복구, Event 협업 로고/갤러리 편집, 관리자 신청 목록·상세 조회, 회원탈퇴 하드삭제, Inquiry 도메인, 일반 이메일 인증·로그인, 런타임 콘텐츠 영어 번역(i18n).
  - 배포 인프라(Postgres 전환, `.env` 시크릿 3단 분리, EC2 배포 시도) — EC2 쪽 마무리 여부는 확인 안 됨(아래 "다음에 할 일" 참고).

## 다음에 할 일

- **학생증(STUDENT) 정책 4-A~4-E는 이번 세션으로 전부 백엔드 구현 완료.** 남은 건 프론트(관리자 업로드 화면 등, `docs/FRONTEND_API_GAPS.md`에 계약 전달 예정 — 아직 안 함, 다음 작업자가 이어서 정리할 것) + 실제 디자이너가 4-D API로 진짜 학교 템플릿을 올리는 운영 작업(현재는 테스트/임시 이미지로만 검증됨).
- **뒷면 텍스트 색상 고정(BLACK) 가독성 이슈 — 미해결로 남음**: TODO.md 4-D "6번" 항목 참고 — 진한 색 배경 위에서 검정 타이틀 글씨 가독성이 떨어지는 게 실측으로 재확인됨(오늘 통합 테스트의 뒷면 렌더링에서도 "학생증" 타이틀이 진한 초록 밴드 위에서 다소 흐릿함). 지금은 디자인과 무관하게 고정 색상으로 그리는데, 실제 디자이너 템플릿이 다양하게 올라오면 이 문제가 반복될 수 있다 — 색상을 디자인별로 지정하게 할지, 아니면 텍스트에 외곽선/그림자를 넣을지 등 정책 결정 필요.
- **로컬 테스트 환경 Redis 컨테이너 시작 습관화**: 위 "지금 어디까지 됐는가" 참고.
- **§3 미커버 테스트**: "DB 반영 실패 시 두 key 모두 보상삭제"·"준비 단계 이후 상태가 바뀐 경우 최종 재검증 실패" 2개 케이스는 진짜 동시 트랜잭션 경합 재현이 필요해 못 씀 — `TODO.md` 3-E에 명시.
- **TODO.md 3-F 착수 여부 결정**: 작명 API `NAME_EDITING` 제한, `startProducing`/`markCardReady` 집계 검증, 카드 생성 동시성 방어 재설계, `GlobalExceptionHandler`의 `ObjectOptimisticLockingFailureException` 전역 처리 부재 — 4개 항목 전부 코드 없음, 문서(근거 포함)만 있음. 필요성 판단 후 착수.
- **§3/4-A~4-D 프론트 연동 전혀 없음(프론트 담당)**: 카드 생성 버튼, 재생성 확인 다이얼로그, STUDENT 학교구분/가로세로 선택 UI, 관리자 템플릿 업로드 화면(미리보기+파일변경) — `TODO.md` 3-D/4-A/4-D, `docs/FRONTEND_API_GAPS.md` 참고(4-D 계약은 아직 그 문서에 전달 안 함 — 다음 작업자가 `docs/api/school-card-template.md` 내용을 옮길 것).
- 아래는 이전 HANDOFF에서 이어져 온 항목으로, 이번 세션에서 상태를 재확인하지 않았다 — 여전히 열려있다고 가정하되 다음 작업자가 먼저 실제 상태를 확인할 것:
  - **Event 협업 로고·갤러리 프론트 연동(프론트 담당)**.
  - **계정 복구 남은 테스트 보강(담당 미정)** — `TODO.md` RECOVERY-1/2.
  - **계정 복구 프론트 연동(프론트 담당)** — `pages/AccountRecoveryPage`.
  - **마이페이지 "제작 내역" 연동(프론트 담당)** — `docs/FRONTEND_API_GAPS.md` §1.2.
  - **만세력 진태양시 보정 프론트 반영, 성씨(surname) 필드 프론트 반영(프론트 담당)** — `docs/FRONTEND_API_GAPS.md` §1.15/1.16, 백엔드는 이미 완전히 구현·검증됨.
  - ~~**EC2 배포 마무리**: `.env` 값 확인, HTTPS(certbot) 미착수.~~ — ✅ 2026-09-05 확인: 실제로는 Let's Encrypt HTTPS가 이미 적용돼 있었음(이 문구가 낡은 것이었음). MinIO presigned URL host 문제(별도 이슈)는 진행 중 — 아래 "❓ 확인 필요" 참고.
  - ~~**`UserApplicationFlowTest.fullUserApplicationFlow()` 403 기존 결함**: 여러 세션째 미수정, 담당자 미정.~~ — ✅ 2026-09-05 확인: 이미 해결돼 있었음(`TODO.md` 1114행 — 개인 신청에 `member.address` 필드가 추가되면서 함께 그린으로 전환된 것으로 기록돼 있었으나 이 문서엔 반영 안 됨). 방금 다시 실행해서 1/1 통과 재확인. 이 문구는 낡은 기록이었을 뿐 실제 결함 아님.
  - ~~다른 세션(Codex로 추정)이 동시에 `SchoolSeeder`/`universities.csv`(전국대학 CSV 시딩) 작업 중~~ — ✅ 2026-09-05 커밋 완료(`60437f5`), 운영 배포 반영 및 라이브 검색 검증(서울대/연세대/고려대/카이스트 등) 완료.
  - 그 외 `docs/collab/TODO.md` 진행 보드·`docs/BACKEND_API_GAPS.md` 참고.

## ❓ 확인 필요

- 없음 — 이번 세션 질문(4-D 남은 정책 결정, isDefault 값, MIME 에러코드)은 전부 사용자가 확정해줬다.

## 참고

- **로컬 테스트 환경(전체 스위트)**: `honor-citizen-redis-test` 컨테이너(호스트 포트 **6400**) — 꺼져 있으면 `docker start honor-citizen-redis-test`, 아예 없으면 `docker run -d --name honor-citizen-redis-test -p 6400:6379 redis:7-alpine`로 생성. `REDIS_PORT=6400 ./gradlew.bat test`.
- **MinIO 버킷명 함정**: docker-compose 기본값은 `honor-citizen-local`이지만 실제 `.env`가 `honorcard-storage-2026`로 오버라이드해뒀다 — `mc`로 직접 접근할 땐 `docker exec main-preview-backend-1 env | grep AWS_S3`로 실제 값부터 확인할 것.
- **브랜치 규칙**: `RULES.md` §1에 따라 `main` 하나만 쓴다 — 별도 `feat/*` 브랜치를 만들지 말 것.
- **학생증 디자이너 원본 에셋**: 앞면은 `D:\HC-worktrees\saju\시안\시안\학생증\`에 4종(고등학교/대학교×가로/세로) 실물 PNG가 있다. **뒷면 실물 배경은 아직 없다** — 지금까지의 뒷면 검증은 전부 다른 학교 앞면 시안을 임시로 재활용하거나(`아트보드 8 사본 10/15.png`) 흰 캔버스였다. 4-D API가 완성됐으니 이제 진짜 관리자가(또는 시뮬레이션으로) 진짜 뒷면 디자인을 올려서 재검증할 수 있다.
- **H2가 지원 안 하는 PostgreSQL 전용 SQL 문법 주의**: `schema.sql`은 H2(테스트)와 PostgreSQL(운영)이 공유한다 — partial/filtered index(`CREATE INDEX ... WHERE`)처럼 PostgreSQL 전용 기능을 쓰기 전에 반드시 H2 2.4.240에서도 실제로 실행해볼 것(이번 세션에 이걸로 한 번 막혔음, `schema.sql` 주석 참고).
- 관련 문서: `docs/collab/TODO.md`("4. 학생증" 절 4-A~4-E 전부 완료, "3. 카드 생성·저장" + 3-F 후속강화 + "5. 보류"), `docs/collab/CHANGELOG.md`, `docs/collab/RULES.md`, `docs/api/school-card-template.md`, `docs/FRONTEND_API_GAPS.md`, `docs/specs/application/admin-saju.md`.
