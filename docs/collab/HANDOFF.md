# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-31
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ "4-C. 학생증 카드 앞/뒷면 렌더링" 구현+테스트+실 렌더링 육안 검증+push 완료(Claude, 2026-08-31, `58443c9`)**:
  - `CardStudentFrontLayout` 신규 레코드 + `CardLayouts.STUDENT_FRONT`/`STUDENT_BACK`(LANDSCAPE/PORTRAIT 2세트) — 좌표는 이전 세션의 탐색 렌더링(실제 렌더링 후 육안 조정)으로 확정된 값을 그대로 이식(이번 세션에 좌표를 새로 정하지 않음). `CardImageCompositor.composeStudentFront/composeStudentBack` 추가, 기존 `CardLayout` 전용 그리기 메서드 6종을 `(baseWidth,baseHeight)` 기반 `*Generic`으로 추출해 위임(로직 중복 없음). `CardMemberData`에 STUDENT 전용 필드 7개 추가(기존 13-인자 생성자로 하위 호환, 다른 3종 호출부 무변경). `CardRenderPreparation.studentMemberData()`가 STUDENT일 때 `CardDesign.templateFrontId`/`templateBackId`를 기존 `downloadUploadFile()`로 S3에서 내려받아 전달.
  - **구현 중 발견·수정한 버그 2건**: (1) `validateIssuerAssets`가 STUDENT 그룹 신청에도 로고·직인 업로드를 잘못 요구하던 것 — STUDENT는 로고·직인을 렌더링하지 않는데 검증만 요구하고 있었음. (2) `Map.of(...).get(null)`은 다른 Map 구현과 달리 null을 반환하지 않고 `NullPointerException`을 던진다(`ImmutableCollections$MapN`) — `studentOrientation` 누락 시 `INVALID_INPUT` 대신 raw NPE가 새고 있었음, `.get()` 전에 null 체크 추가로 수정.
  - **테스트**: `CardImageCompositorTest`에 STUDENT 성공 경로 6건(대학교/고등학교×가로/세로 앞면, 한자 유무별 뒷면) + 템플릿 누락 실패 케이스 추가. 기존 STUDENT 실패 테스트 2건("레이아웃 없음")은 실제로는 이제 레이아웃이 있으므로 이름·주석을 실제 실패 사유(studentOrientation/템플릿 누락)로 정정. `domain.card`/`domain.application` 패키지 전체 재실행 통과 확인.
  - **실 렌더링 육안 검증(2026-08-31)**: 프로덕션 `composeStudentFront/Back`을 직접 호출하는 임시 테스트로 4개 조합(고등학교/대학교×가로/세로)의 앞+뒷면을 실제 렌더링해 화면에 띄워 확인(검증 후 임시 파일 삭제). 앞면은 이전 세션 확정 좌표와 동일(재확인), 뒷면은 이번이 처음 렌더링 — 이름/한자/영문명/풀이 4줄이 겹침 없이 배치됨을 확인. **단, 뒷면 배경은 디자이너 원본 에셋이 아직 없어(4-D 업로드 전) 흰 캔버스로 대체 렌더링했다 — 배치 좌표만 검증된 상태이고 실제 디자인 위 육안 확인은 4-D 완료 후 다시 필요.**
  - 상세 근거는 `TODO.md` "4-C" 절, 커밋 diff 참고.
- **⚠️ 로컬 테스트 환경 참고**: 이번 세션 전체 스위트(`./gradlew.bat test`, Redis 포트 오버라이드 없이)는 154개 Redis 연결 실패로 막힘 — **내가 만든 회귀가 아니라 로컬 Docker의 redis 컨테이너가 호스트 포트를 노출하지 않는 환경 설정 문제**(`docker-compose.yml`이 컨테이너 내부망 전용으로 구성돼 있음, 아래 "참고" 섹션 확인). `domain.card`/`domain.application` 패키지만 필터링해서 돌리면(Redis 의존 없음) 전부 통과한다 — 이번 세션은 이 방식으로 검증했다. 다음 작업자가 전체 스위트를 실행하려면 먼저 로컬 redis에 호스트 포트를 열어야 한다.
- **이전 세션들(2026-08-19~2026-08-30) 요약** — 전부 완료·커밋·push됨, 상세는 `docs/collab/CHANGELOG.md` 해당 날짜 항목 참고:
  - "3. 카드 생성·저장 — 최소 버전"(`POST .../card-generate`) + 2-C Preview 계약 변경, 실 Docker+MinIO+curl 검증까지 완료.
  - "4-B. CardDesign 학교 매칭"(schoolId+orientation 기반 STUDENT 디자인 조회, `applicationId` 파라미터로 관리자 목록 API 자동 해석) 완료.
  - 프론트-백엔드 정책 갭 감사(`docs/FRONTEND_API_GAPS.md`) — 만세력/진태양시 보정 프론트 미적용, 성씨(surname) 필드 프론트 미처리 발견·문서화.
  - School 마스터 엔티티 + 검색select, 계정 복구, Event 협업 로고/갤러리 편집, 관리자 신청 목록·상세 조회, 회원탈퇴 하드삭제, Inquiry 도메인, 일반 이메일 인증·로그인, 런타임 콘텐츠 영어 번역(i18n).
  - 배포 인프라(Postgres 전환, `.env` 시크릿 3단 분리, EC2 배포 시도) — EC2 쪽 마무리 여부는 확인 안 됨(아래 "다음에 할 일" 참고).

## 다음에 할 일

- **🟡 4-D. 학생증 템플릿 업로드 API(관리자, S3 기반) — 설계만 완료, 코드 미착수**: `TODO.md` "4-D" 절에 API 계약(`GET`/`POST /api/admin/schools/{schoolId}/card-template`)·5단계 처리 순서·`UploadFile` 행 정리(`registerS3CleanupAfterTransaction` 패턴 재사용)·`CARD_IMAGE` enum 용도 구분까지 전부 확정돼 있다. 이게 끝나야 STUDENT `CardDesign`을 실제로 등록할 방법이 생기고(현재는 관리자가 직접 만들 경로가 없음), 4-C가 실제 디자이너 배경 위에서 육안 검증될 수 있다. 백엔드 API+문서까지만(관리자 UI는 프론트 담당, `docs/FRONTEND_API_GAPS.md`에 전달 예정).
- **로컬 테스트 환경 Redis 포트 노출**: 위 "지금 어디까지 됐는가" 참고 — 전체 스위트를 실행하려면 필요.
- **§3 미커버 테스트**: "DB 반영 실패 시 두 key 모두 보상삭제"·"준비 단계 이후 상태가 바뀐 경우 최종 재검증 실패" 2개 케이스는 진짜 동시 트랜잭션 경합 재현이 필요해 못 씀 — `TODO.md` 3-E에 명시.
- **TODO.md 3-F 착수 여부 결정**: 작명 API `NAME_EDITING` 제한, `startProducing`/`markCardReady` 집계 검증, 카드 생성 동시성 방어 재설계, `GlobalExceptionHandler`의 `ObjectOptimisticLockingFailureException` 전역 처리 부재 — 4개 항목 전부 코드 없음, 문서(근거 포함)만 있음. 필요성 판단 후 착수.
- **§3/4-C 프론트 연동 전혀 없음(프론트 담당)**: 카드 생성 버튼, 재생성 확인 다이얼로그, STUDENT 학교구분/가로세로 선택 UI — `TODO.md` 3-D/4-A, `docs/FRONTEND_API_GAPS.md` 참고.
- 아래는 이전 HANDOFF에서 이어져 온 항목으로, 이번 세션에서 상태를 재확인하지 않았다 — 여전히 열려있다고 가정하되 다음 작업자가 먼저 실제 상태를 확인할 것:
  - **Event 협업 로고·갤러리 프론트 연동(프론트 담당)**.
  - **계정 복구 남은 테스트 보강(담당 미정)** — `TODO.md` RECOVERY-1/2.
  - **계정 복구 프론트 연동(프론트 담당)** — `pages/AccountRecoveryPage`.
  - **마이페이지 "제작 내역" 연동(프론트 담당)** — `docs/FRONTEND_API_GAPS.md` §1.2.
  - **만세력 진태양시 보정 프론트 반영, 성씨(surname) 필드 프론트 반영(프론트 담당)** — `docs/FRONTEND_API_GAPS.md` §1.15/1.16, 백엔드는 이미 완전히 구현·검증됨.
  - **EC2 배포 마무리**: `.env` 값 확인, HTTPS(certbot) 미착수.
  - **`UserApplicationFlowTest.fullUserApplicationFlow()` 403 기존 결함**: 여러 세션째 미수정, 담당자 미정.
  - 다른 세션(Codex로 추정)이 동시에 `SchoolSeeder`/`universities.csv`(전국대학 CSV 시딩) 작업 중인 것으로 보임(이 세션 작업 디렉터리에 커밋 안 된 관련 파일 존재) — 이번 세션은 건드리지 않았다. 다음 작업자는 해당 작업자의 커밋 여부를 `git log`/`git status`로 먼저 확인할 것.
  - 그 외 `docs/collab/TODO.md` 진행 보드·`docs/BACKEND_API_GAPS.md` 참고.

## ❓ 확인 필요

- 없음 — 이번 세션은 이미 확정된 4-C 정책·좌표를 그대로 구현했다.

## 참고

- **로컬 테스트 환경(전체 스위트)**: `honor-citizen-redis-test` 컨테이너(호스트 포트 **6400**)가 있다면 `REDIS_PORT=6400 ./gradlew.bat test`. 없으면(이번 세션 확인 결과 없었음) `docker-compose.yml`의 `redis` 서비스가 호스트 포트를 노출하지 않아(`main` 컨테이너 내부망 전용) 전체 스위트가 Redis 연결 실패로 대량 실패한다 — 카드/신청 도메인만 검증할 땐 `./gradlew.bat test --tests "com.example.honorcitizen.domain.card.*" --tests "com.example.honorcitizen.domain.application.*"`로 우회 가능(이번 세션 방식).
- **MinIO 버킷명 함정**: docker-compose 기본값은 `honor-citizen-local`이지만 실제 `.env`가 `honorcard-storage-2026`로 오버라이드해뒀다 — `mc`로 직접 접근할 땐 `docker exec main-preview-backend-1 env | grep AWS_S3`로 실제 값부터 확인할 것. MinIO root 자격증명도 docker-compose 기본값(`minioadmin`)이 아니라 `.env`의 실제 값을 써야 한다(`docker exec main-preview-minio-1 env | grep MINIO_ROOT`).
- **브랜치 규칙**: `RULES.md` §1에 따라 `main` 하나만 쓴다 — 별도 `feat/*` 브랜치를 만들지 말 것.
- **학생증 디자이너 원본 에셋**: 앞면은 `D:\HC-worktrees\saju\시안\시안\학생증\`에 4종(고등학교/대학교×가로/세로) 실물 PNG가 있다(이번 세션 육안 검증에 사용). **뒷면 실물 배경은 아직 없다** — 4-D 업로드 API가 준비되면 디자이너가 실제로 올려야 함.
- 관련 문서: `docs/collab/TODO.md`("4. 학생증" 절 4-A~4-D, "3. 카드 생성·저장" + 3-F 후속강화 + "5. 보류"), `docs/collab/CHANGELOG.md`, `docs/collab/RULES.md`, `docs/FRONTEND_API_GAPS.md`, `docs/specs/application/admin-saju.md`.
