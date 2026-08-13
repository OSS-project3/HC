# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-13
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **Review 도메인 CRUD 5개 API 구현 완료** (등록/목록/단건조회/삭제/수정). 설계 문서(`docs/specs/review/{data-model,api}.md`)는 이미 2026-08-09에 프론트(카드종류 단일선택·사진 0~1장) 기준으로 재작성되어 있었고, 이번에 그 설계를 실제 코드로 구현했다. TDD로 진행(테스트 먼저 → 실패 확인 → 구현 → 통과)했고 신규 테스트 76개 전부 통과.
- 구현 도중 발견해 사람에게 확인 후 새로 확정한 정책 2건(둘 다 문서에 반영 완료):
  1. **후기 작성 개수 제한** — 사용자 1명이 자격 있는 (신청유형, 카드종류) 조합마다 후기를 1개만 쓸 수 있다("한 신청당 한 개"). `Review→Application` FK를 두지 않는 기존 설계를 유지하기 위해 판단 기준은 실제 Application row가 아니라 `(user_id, application_type, card_type_id)` 조합의 유일성이다. 위반 시 `REVIEW_ALREADY_EXISTS`(409, 신규 ErrorCode).
  2. **탈퇴 계정 작성 차단** — 탈퇴(`WITHDRAWN`) 처리된 계정은 새 후기를 작성할 수 없다(`ALREADY_WITHDRAWN`, 기존 ErrorCode 재사용). 등록에만 적용하고 수정(API 5)에는 적용하지 않는다 — 원작성자가 나중에 탈퇴해도 관리자가 기존 후기를 계속 관리(수정)할 수 있어야 하므로.
- WEBP 이미지 검증을 위해 `com.twelvemonkeys.imageio:imageio-webp:3.10.1` 의존성을 신규 추가했다(Java 표준 `ImageIO`는 WEBP 디코딩을 지원하지 않음). 사람이 "WEBP 디코딩 라이브러리 추가"로 직접 결정.
- `SecurityConfig`에 `GET /api/reviews`·`GET /api/reviews/{id}`만 `permitAll()` 추가(등록/수정/삭제는 기존 `hasAnyRole("USER","ADMIN")` 그대로).
- 이 프로젝트 첫 페이징 API라 공용 `PageResponse<T>`(`common/response/`)를 신설했다.
- 테스트 작성 관례(서비스 계층은 `@SpringBootTest`+실 H2, 컨트롤러 계층은 `@AutoConfigureMockMvc`+`MockMvc`로 실제 JWT까지 통과시켜 검증)를 처음으로 `docs/collab/RULES.md` §8에 문서화했다 — 기존 코드에 이미 있던 관례를 관찰해서 명문화만 한 것.
- 구현 중 실제로 겪은 함정 2가지(다음에 비슷한 작업할 때 참고):
  - 이 프로젝트의 Spring Data JPA 버전은 `Specification.where(null)`/`.and(null)`을 더 이상 허용하지 않고 `IllegalArgumentException`을 던진다(과거 버전과 다름). 조건이 없을 때는 `null` 대신 `cb.conjunction()`(항상 참)을 반환해야 한다 — `ReviewSpecifications.java` 참고.
  - 목록 정렬을 `createdAt DESC`만으로 하면 동시 등록 시 밀리초 단위로 값이 같아져(H2 등) 순서가 불안정해질 수 있다(실제로 플레이키 발생 후 수정). `id DESC`를 2차 정렬키로 추가해서 해결했다.
- 전체 테스트 스위트 216개 중 기존과 동일하게 `UserControllerTest` 2건, `UserApplicationFlowTest` 1건만 실패 — 전부 로컬 Redis 미기동이 원인(Review 작업과 무관, 새로 발생한 회귀 아님).

## 다음에 할 일

- **"내가 후기 쓸 수 있는 (신청유형, 카드종류) 목록" 조회 API** — 없으면 프론트가 라디오 옵션을 모른 채 제출했다가 `REVIEW_NOT_ELIGIBLE`로 사후 거절만 가능. 이번 5개 API 범위 밖으로 남겨뒀다(`docs/specs/review/api.md` 하단 "이번 범위 밖" 참고). 필요 여부·설계는 미정.
- **마이페이지 "내 후기" 목록** — 마찬가지로 이번 범위 밖. `Review.user_id` 기준 조회라 `ReviewRepository.findByUserId(...)` 신규 필요, 페이징은 기존 `PageResponse<T>` 재사용 가능.
- **프론트 반영** — `ReviewEditorPage.tsx`가 아직 신청유형/카드종류 단일선택·사진 1장·작성자 직접입력 폼으로 안 바뀌어 있을 수 있다(프론트 담당자 영역, 이번 세션에서 건드리지 않음). 실제 백엔드 계약과 일치하는지 프론트 담당자 확인 필요.
- **Redis 의존 테스트 3건**은 여전히 미해결 상태로 남아있다(이번 세션과 무관, 이전부터 있던 이슈) — 로컬에 Redis를 띄우거나 test profile/mock 전략 결정 필요.

## ❓ 확인 필요

- 없음 (이번 Review 구현 범위에서 발견한 정책 공백 2건은 위에 적힌 대로 이미 사람 확인 받아 확정·문서 반영·구현까지 완료됨).

## 참고

- 관련 테스트 실행: `./gradlew.bat test --tests "com.example.honorcitizen.domain.review.*" --tests "com.example.honorcitizen.api.ReviewControllerTest"` (Review 도메인만), 또는 `./gradlew.bat test`(전체 스위트).
- 결과: Review 도메인 76개 전부 통과. 전체 216개 중 Redis 미기동 관련 3개만 실패(회귀 아님).
- 관련 문서: `docs/specs/review/{data-model,api}.md`, `docs/collab/CHANGELOG.md`(2026-08-13 항목), `docs/collab/RULES.md` §8(신규 — 테스트 작성 규칙), `docs/collab/TODO.md`("Review 도메인 구현" 행 완료로 갱신)
