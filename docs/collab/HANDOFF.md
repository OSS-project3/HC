# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-30
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ "3. 카드 생성·저장 — 최소 버전" 구현+테스트+실검증 완료, 2-C Preview 계약 변경 포함(Claude, 2026-08-30, 로컬 `main`에 커밋 완료 — `ac981e7`/`3ab32f4`, ⚠️ 아직 origin에 push 안 함)**:
  - `POST /api/admin/applications/{applicationId}/members/{memberId}/card-generate` 신규 — Member 1명 단위 동기 처리로 카드 앞/뒤를 렌더링해 S3에 저장하고 `ApplicationMember.cardFrontPath/cardBackPath/issueDate`, `Application.cardDesignId/cardIssueDate`에 연결한다. 비동기 `CardGenerationJob`/Worker/진행률/재시도는 이번 스코프가 아니고 `TODO.md` "5. [보류 — 향후 확장]"에 원문 그대로 보존돼 있다.
  - 검증·S3 다운로드·렌더링을 `CardPreviewService.preview()`에서 `CardRenderPreparation`으로 추출해 Preview·Generate가 공유(신규 컴포넌트). `Application.cardDesignId`/`cardIssueDate`가 이미 확정된 뒤에는 Preview도 다른 값을 거절한다(`CARD_DESIGN_MISMATCH`/신규 `CARD_ISSUE_DATE_MISMATCH`).
  - `CardGenerationService`(오케스트레이션, 비-transactional)+`CardGenerationPersistenceService`(`@Transactional`, 최종 재검증 후 반영) 분리. FRONT/BACK 중 하나라도 실패하면 신규 S3 key 보상삭제. 재생성은 신규 파일 선업로드→DB commit→기존 파일 후삭제. 상태 게이트는 `PRODUCTION_READY` 또는 (`PRODUCING && cardReadyAt == null`)만 허용.
  - **최초 구현엔 더 있었다가 재검토 후 되돌린 것(=`TODO.md` "3-F. 후속 강화"로 이동, 코드는 없음)**: `applyNamingResult`/`assignMemberName`의 `NAME_EDITING` 상태 제한, `startProducing`/`markCardReady`의 카드 생성 완료 집계 검증, `ApplicationMember`에 대한 낙관적 락(`@Version`) 기반 동시성 방어. 셋 다 §3 핵심 동작과 독립적이고 §3 밖 기존 API·엔티티에 부수효과가 있어 제외했다 — 근거는 `TODO.md` 3-F에 항목별로 남겨뒀다. `ApplicationService.java`는 이 되돌리기 결과 커밋된 원본(이번 세션 이전)과 diff 0.
  - **테스트**: `CardGenerationServiceTest`(신규 11개), `CardPreviewServiceTest`(+3, 16→19), `ApplicationMemberTest`/`ApplicationStateTransitionTest`(+3, 신규 mutator 단위테스트). 테스트 작성 중 실제 버그 하나 발견해 수정: `CardGenerationService`가 `CustomException`이 아닌 일반 `RuntimeException`(S3 장애 등) 실패는 감사로그를 안 남기고 있었음 — 전부 남기도록 수정.
  - **전체 스위트**: `REDIS_PORT=6400 ./gradlew.bat test` 753개 중 751 통과, skip 2(기존부터 있던 것, 무관), 실패/에러 0.
  - **실제 Docker+MinIO+curl 검증(2026-08-30)**: `docker compose build backend` 재빌드 → 실제 fixture(application 7/member 12, 기존 PRODUCTION_READY 데이터)로 실제 curl 호출 → 응답 JSON, DB(`applications.card_design_id/card_issue_date`, `application_members.card_front_path/card_back_path/issue_date`), MinIO 실물 PNG(버킷명 `honorcard-storage-2026` — `.env`로 오버라이드된 실제값, docker-compose 기본값 `honor-citizen-local`이 아님) 전부 확인. 재생성 curl로 새 파일 생성+기존 파일이 MinIO에서 실제로 사라지는 것까지 실물로 확인. `AdminActivityLog.CARD_IMAGE_GENERATED` 기록도 확인. 없는 신청 ID curl로 `404 APPLICATION_NOT_FOUND` JSON 포맷 확인.
  - **⚠️ 이번 세션에서 브랜치 실수 발견·정정**: 중간에 `feat/card-generation-minimal`이라는 별도 브랜치를 만들어 커밋했다가, `RULES.md` §1("2026-08-06부터 `main` 브랜치 하나만 계속 개발한다")과 어긋난다는 걸 뒤늦게 알아채고 로컬에서 `main`으로 fast-forward 병합 후 브랜치 삭제했다. **origin에는 아직 push 안 됨** — 다음 작업자가 이어받으려면 먼저 `git push`부터 해야 한다(또는 사용자가 직접 push).
  - **⚠️ 미커버 항목**: `CardGenerationServiceTest`에 "DB 반영 실패 시 두 key 모두 보상삭제"·"준비 단계 이후 상태가 바뀐 경우 최종 재검증 실패" 2개 케이스는 진짜 동시 트랜잭션 경합 재현이 필요해 못 씀 — `TODO.md` 3-E에 명시.
- **이전 세션들(2026-08-19~2026-08-27) 요약** — 전부 완료·커밋됨, 상세는 `docs/collab/CHANGELOG.md` 해당 날짜 항목 참고:
  - 2-C 카드 미리보기 API(FRONT/BACK 계약 변경 포함) — 이번 세션 커밋에 같이 실려있음(위 참고).
  - School 마스터 엔티티 + 검색select(schoolId 연동), 학생증 카드 정책 확정(TODO.md "4. 학생증(STUDENT)" 절, 4-B/4-C 미착수).
  - 계정 복구(아이디 찾기·비밀번호 재설정), Event 협업 로고/갤러리 편집, 관리자 신청 목록·상세 조회, 회원탈퇴 하드삭제, Inquiry 도메인, 일반 이메일 인증·로그인.
  - 배포 인프라(Postgres 전환, `.env` 시크릿 3단 분리, EC2 배포 시도) — EC2 쪽 마무리 여부는 확인 안 됨(아래 "다음에 할 일" 참고).

## 다음에 할 일

- **🔴 이번 세션 커밋 push**: `git push`로 origin/main에 반영. 아직 아무도 안 함.
- **§3 미커버 테스트**: 위 "⚠️ 미커버 항목" 2개 — 필요하면 진짜 동시 트랜잭션 테스트로 채울 것.
- **TODO.md 3-F 착수 여부 결정**: 작명 API `NAME_EDITING` 제한, `startProducing`/`markCardReady` 집계 검증, 카드 생성 동시성 방어 재설계, `GlobalExceptionHandler`의 `ObjectOptimisticLockingFailureException` 전역 처리 부재 — 4개 항목 전부 코드 없음, 문서(근거 포함)만 있음. 필요성 판단 후 착수.
- **STUDENT 카드 4-B/4-C 미착수**: `CardDesign` 학교 매칭, STUDENT `CardLayouts` 좌표 등록 — `TODO.md` "4. 학생증(STUDENT)" 절 참고. 이게 끝나야 §3 Generate API가 STUDENT도 지원한다(코드 변경 없이 자동 지원되도록 이미 설계됨).
- **§3 프론트 연동 전혀 없음(프론트 담당)**: 카드 생성 버튼, 재생성 확인 다이얼로그, pending 중 버튼 비활성화(현재 백엔드에 동시성 방어가 없어 이게 사실상 유일한 중복 요청 방지선) — `TODO.md` 3-D, `docs/FRONTEND_API_GAPS.md` 참고.
- 아래는 2026-08-24 이전 HANDOFF에서 이어져 온 항목으로, 이번 세션에서 상태를 재확인하지 않았다 — 여전히 열려있다고 가정하되 다음 작업자가 먼저 실제 상태를 확인할 것:
  - **Event 협업 로고·갤러리 프론트 연동(프론트 담당)**.
  - **계정 복구 남은 테스트 보강(담당 미정)** — `TODO.md` RECOVERY-1/2.
  - **계정 복구 프론트 연동(프론트 담당)** — `pages/AccountRecoveryPage`.
  - **schoolName 미전송 회귀(프론트 담당)** — `docs/FRONTEND_API_GAPS.md` §1.9-b. School 검색select 기능(2026-08-27, 커밋 `28e2141`)이 이미 들어갔으므로 이 항목은 해소됐을 가능성이 있다 — 다음 작업자가 프론트 코드로 직접 확인.
  - **마이페이지 "제작 내역" 연동(프론트 담당)** — `docs/FRONTEND_API_GAPS.md` §1.2.
  - **EC2 배포 마무리**: `.env` 값 확인, HTTPS(certbot) 미착수.
  - **`UserApplicationFlowTest.fullUserApplicationFlow()` 403 기존 결함**: 여러 세션째 미수정, 담당자 미정, 이번 세션 변경과 무관함 재확인 안 함(이전에는 무관 확인됨).
  - 그 외 `docs/collab/TODO.md` 진행 보드·`docs/BACKEND_API_GAPS.md` 참고.

## ❓ 확인 필요

- 없음 — 이번 세션 질문(카드 생성 최소 스코프 vs 비동기 전체 설계, 정책 충돌 5개 재검토, 브랜치 전략)은 사용자가 전부 확정해줬다.

## 참고

- **로컬 테스트 환경**: `honor-citizen-redis-test` 컨테이너(호스트 포트 **6400**) — `REDIS_PORT=6400 ./gradlew.bat test`. 별도 `docker compose`(저장소 루트)는 전체 스택(Postgres+Redis+MinIO+backend) 실동작 검증용 — 이번 세션에 이걸로 §3을 검증했다.
- **MinIO 버킷명 함정**: docker-compose 기본값은 `honor-citizen-local`이지만 실제 `.env`가 `honorcard-storage-2026`로 오버라이드해뒀다 — `mc`로 직접 접근할 땐 `docker exec main-preview-backend-1 env | grep AWS_S3`로 실제 값부터 확인할 것. MinIO root 자격증명도 docker-compose 기본값(`minioadmin`)이 아니라 `.env`의 실제 값을 써야 한다(`docker exec main-preview-minio-1 env | grep MINIO_ROOT`).
- **브랜치 규칙**: `RULES.md` §1에 따라 `main` 하나만 쓴다 — 별도 `feat/*` 브랜치를 만들지 말 것(이번 세션에 실수로 만들었다가 되돌림).
- 관련 문서: `docs/collab/TODO.md`("3. 카드 생성·저장" 최소버전 절 + 3-F 후속강화 + "4. 학생증" + "5. 보류"), `docs/collab/CHANGELOG.md`, `docs/collab/RULES.md`, `docs/FRONTEND_API_GAPS.md`, `docs/specs/application/admin-saju.md`.
