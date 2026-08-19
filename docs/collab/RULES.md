# 협업 규칙 (Claude / Codex 공용)

> 이 문서는 `guide.md`(구 "Application 도메인 작업 가이드")를 대체·확장한다.
> Claude와 Codex가 동시에 작업할 때,
> **사람이 매번 상대방의 변경사항을 프롬프트로 전달하지 않아도** 스스로 이어받을 수 있게 하는 것이 목적이다.

핵심 메커니즘: 작업을 시작할 때 `docs/collab/HANDOFF.md`를 읽는 것을
**작업 절차의 0단계로 강제**한다. 사람의 개입 없이도 "상대가 뭘 바꿨는지"를 여기서 알 수 있다.

---

## 1. 작업 공간

> ⚠️ 2026-08-06 갱신: `backend-api`/`feature/application-domain-impl`/`feature/application-domain-docs` 세 브랜치로 나눠 작업하던 구조를 정리하고, git merge로 전부 `main`에 합쳤다. **이후로는 `main` 브랜치 하나만 계속 개발한다.** 브랜치 3개짜리 표는 히스토리 참고용으로만 아래에 남겨둔다.

| 브랜치 | 상태 |
|---|---|
| `main` | ✅ 현재 유일한 개발 브랜치. Claude/Codex 전부 여기서 작업 |
| `backend-api` | 폐기(2026-08-06 `main`에 병합 완료) — 더 이상 커밋하지 않음, 삭제 여부는 보류 중 |
| `feature/application-domain-impl` | 폐기(2026-08-06 `main`에 병합 완료) — 더 이상 커밋하지 않음, 삭제 여부는 보류 중 |
| `feature/application-domain-docs` | 폐기(이미 `feature/application-domain-impl` 경유로 병합 완료) |

`docs/collab/` 디렉터리(이 폴더)는 `main` 하나에만 존재하면 된다. 여러 브랜치를 오가며 `HANDOFF.md`를 대조할 필요는 이제 없고, 대신 **작업 시작 전 반드시 `git pull`로 `main`을 최신화**한다(동시 작업자가 그사이 커밋했을 수 있음).

---

## 2. 문서 우선순위

### 0순위 — 작업 상태 (가장 먼저 확인)

- `docs/collab/HANDOFF.md` — 마지막 작업자가 남긴 "지금 상태" 스냅샷. 항상 최신 1개만 존재(누적 아님).
- `docs/collab/TODO.md` — 현재 진행중/대기/블로킹 작업 목록과 담당자.

### 1순위 — 기준 문서 (Source of Truth)

- `docs/specs/{도메인}/requirements.md` (있는 도메인만 — 도메인 규모가 작으면 requirements.md 없이 api.md 상단에 요구사항을 함께 적어둔다. 예: `docs/specs/review/`는 requirements.md 없이 api.md/data-model.md 두 개만 있음)
  - 해당 도메인의 요구사항·비즈니스 규칙을 정의한 기준 문서. 구현·API 설계는 이 문서를 최우선으로 따른다.
  - 현재 존재: `docs/specs/application/requirements.md`

### 2순위 — 도메인 API

- 이전 완료된 도메인: `docs/specs/{도메인}/api.md` (예: `docs/specs/application/api.md`, `docs/specs/review/api.md`)
- 아직 이전하지 않은 도메인: `docs/api/{도메인}.md` (도메인 이전 작업 완료 시 `docs/specs/{도메인}/api.md`로 변경)
  - 도메인별 API 명세. 해당 도메인 API는 이 파일에서만 관리한다.
  - 전체 목차는 `docs/api/README.md` 참고.

### 3순위 — 공통 문서

- `docs/api/README.md` — 전체 API 문서 목차
- `docs/api/common.md` — 공통 응답 형식·인증 방식·에러 코드

### 4순위 — 참고 문서

- `arch.md` — 아키텍처·개발 규칙
- Application: `docs/specs/application/data-model.md` — Application 데이터 모델 Source of Truth
- 아직 이전하지 않은 도메인: `DB.md` — 이전 완료 전까지의 테이블·컬럼 구조
- `docs/api/unresolved.md` — 미결정 사항 목록

### 문서 갱신 전파 방향

요구사항이 바뀌면 반드시 이 순서로 전파한다:
`docs/specs/application/requirements.md` → `docs/specs/application/data-model.md` → `docs/specs/application/api.md` → (구조에 영향 있으면) `arch.md`

---

## 3. 작업 원칙 (기존 guide.md 승계)

- `docs/specs/application/requirements.md`를 Source of Truth로 사용한다.
- 공통 규칙(응답 형식, 인증, 에러 코드)은 `docs/api/common.md`를 따르며 중복 작성하지 않는다.
- 문서 간 내용이 충돌하면 임의로 구현·수정하지 말고, 충돌 내용을 `HANDOFF.md`의 "❓ 확인 필요"에 기록한 뒤 사람에게 질문한다.
- 미결정 사항(Unresolved)이 있거나 요구사항이 모호하면 구현하지 말고 먼저 질문한다.
- 작업에 필요한 문서만 참고하며, 관련 없는 문서는 임의로 고치지 않는다.
- 한 번에 전체 도메인을 작업하지 않는다. 기능 단위(예: 신청 생성 API / 신청 조회 API)로 잘게 나눈다.
- **`frontend/` 디렉터리는 수정하지 않는다(2026-08-19 확정)** — 백엔드 작업자는 백엔드(`backend/`)와 문서(`docs/`)만 다룬다. 프론트 쪽에 필요한 변경사항(계약 불일치, 새로 만들어야 하는 화면, UX 결정 등)은 코드로 직접 고치지 말고 `docs/FRONTEND_API_GAPS.md`/`docs/FRONTEND_API_INTEGRATION_SPEC.md`에 기록해 전달한다. 예외적으로 프론트 수정이 꼭 필요하다고 판단되면, 고치기 전에 반드시 사람에게 먼저 확인한다.

---

## 4. 작업 시작 절차 (체크리스트)

- [ ] `git fetch origin && git pull` — `main` 최신화(동시 작업자가 그사이 커밋했을 수 있음)
- [ ] `docs/collab/HANDOFF.md` 읽기 — 마지막 작업자가 어디까지 했는지 확인
- [ ] `docs/collab/TODO.md` 읽고, 맡을 작업 행에 담당자·상태를 `🔵 진행중`으로 표시 후 커밋+푸시 (다른 작업자와 중복 방지)
- [ ] `docs/collab/CHANGELOG.md` 최근 5~10개 항목 훑어보기
- [ ] 이번 작업 범위에 맞는 1~4순위 문서 확인
- [ ] 문서 간 충돌이나 모호한 요구사항 발견 시 → 진행하지 말고 `HANDOFF.md`에 질문 기록 후 사람에게 확인

## 5. 작업 종료 절차 (체크리스트)

- [ ] 변경 내용이 아래 4가지 기준과 일치하는지 검증
  - [ ] 해당 도메인 `requirements.md`(있으면)와 일치하는가
  - [ ] 해당 도메인 `data-model.md` 구조와 일치하는가
  - [ ] `docs/api/common.md` 공통 규칙을 준수하는가
  - [ ] 기존 API/문서와 충돌하지 않는가
- [ ] `docs/collab/TODO.md` 갱신 (완료 체크, 새로 발견한 작업 추가, 블로킹이면 `🔴`로 표시)
- [ ] `docs/collab/CHANGELOG.md` 맨 위에 새 항목 추가 (템플릿 참고)
- [ ] `docs/collab/HANDOFF.md` 전체를 지금 상태로 덮어쓰기 (이어붙이지 않는다 — 항상 최신 스냅샷 1개)
- [ ] `docs/collab/*` 포함하여 커밋
- [ ] `git pull --rebase` 후 `main`에 push (그 사이 다른 작업자가 push했을 수 있음 — 충돌 시 §7 참고)

---

## 6. 문서별 갱신 책임

| 상황 | 반드시 수정해야 하는 문서 |
|---|---|
| Application 요구사항/정책 변경 | `docs/specs/application/requirements.md` → `data-model.md` → `api.md` |
| Application API 엔드포인트 추가/변경 | `docs/specs/application/api.md` |
| Application DB 스키마 변경 | `docs/specs/application/data-model.md` |
| 패키지 구조 등 아키텍처 변경 | `arch.md` |
| 코드 구현/리팩터링 | 코드 (+ 구조가 바뀌면 `arch.md`) |
| 결정 보류 항목 발생 | `docs/api/unresolved.md` |
| **모든 작업 종료 시 (예외 없음)** | `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md` |

---

## 7. `docs/collab/*` 병합 규칙

모두 `main` 한 브랜치에서 작업하므로, `git pull` 시점에 다른 작업자의 커밋과 충돌할 수 있다.

- `TODO.md`, `CHANGELOG.md`는 append형이라 자동 병합(merge)이 대체로 안전하다. 표 순서가 꼬이면 병합 후 한 번 정리한다.
- `HANDOFF.md`는 "스냅샷" 문서라 자동 병합하지 않는다. `git pull` 중 충돌이 나면
  두 쪽 내용을 참고해 **새로 하나로 다시 작성**한다 (둘 중 하나를 임의로 버리지 않는다).
- 코드 파일이 충돌하면(예: 같은 Service 파일을 동시에 건드림) 상대방이 무엇을 하려 했는지 `HANDOFF.md`/`CHANGELOG.md` 최근 항목으로 먼저 파악한 뒤 수동으로 합친다 — 한쪽을 임의로 버리지 않는다(2026-08-06 `main` 통합 병합 때 실제로 이 방식으로 처리함, `CHANGELOG.md` 해당 날짜 항목 참고).
- 협업 문서(`docs/collab/*`) 변경은 가능하면 코드/기능 변경과 **별도 커밋**으로 분리한다.
  예: `chore(collab): update handoff`

---

## 8. 테스트 작성 규칙 (2026-08-13 신규 — Review 도메인 구현 중 명문화)

> 이전까지 이 문서에 테스트 방법론이 없어서, 기존 코드(`ApplicationServiceTest`, `ApplicationServiceLookupTest`, `ApplicationControllerTest` 등)를 관찰해서 따라온 관례를 여기서 처음 문서화한다. 아래 패턴과 다르게 작성된 기존 테스트를 발견해도 임의로 통일하지 않는다 — 새로 작성하는 테스트만 이 규칙을 따른다.

**두 계층으로 나눠 작성한다:**

1. **서비스 계층 통합테스트** (`domain/{도메인}/service/*Test.java`)
   - `@SpringBootTest` + Spring Boot가 자동 구성하는 실제 H2 인메모리 DB 사용. `Repository`를 `@Autowired`로 주입받아 실제로 저장·조회하며 검증한다(Mock으로 대체하지 않는다).
   - S3 등 외부 연동만 `@MockitoBean`으로 목킹한다(예: `StorageService`). 네트워크 호출이 실제로 나가면 안 되는 것만 목킹 대상이다.
   - `@BeforeEach`에서 관련 Repository 전부 `deleteAll()`로 초기화해 테스트 간 데이터 격리를 보장한다.

2. **컨트롤러 계층 API 테스트** (`api/*ControllerTest.java`)
   - `@SpringBootTest` + `@AutoConfigureMockMvc`로 `MockMvc`를 통해 실제 HTTP 요청처럼 호출한다.
   - 인증이 필요한 API는 `JwtTokenProvider`로 실제 accessToken을 발급해 `Authorization` 헤더에 실어 보낸다 — `JwtAuthFilter`/`SecurityConfig`까지 실제로 통과시켜 검증한다(인증 로직 자체를 목킹하지 않는다).
   - multipart 바인딩, HTTP 상태 코드, `permitAll()` 대상 여부 등 서비스 단위 테스트로는 확인할 수 없는 계층을 검증하는 것이 목적이다. 서비스 로직 자체(비즈니스 규칙 분기 전부)를 컨트롤러 테스트에서 중복 검증하지 않는다 — 그건 1번(서비스 계층)의 역할이다.

**작성 순서(TDD)**: 가능하면 테스트를 먼저 작성해 의도한 대로 실패하는지 확인한 뒤 최소 구현을 추가하고, 관련 테스트 스위트를 재실행해 통과를 확인한다. 구현 후에는 영향 범위의 전체 테스트(`./gradlew test` 또는 관련 `--tests` 범위)를 반드시 돌려 회귀를 확인한다.

---

## 9. 대량 출력 명령 실행 및 로그 출력 규칙 (2026-08-13 신규, 2026-08-16 범위 확장)

Spring Boot/Gradle 테스트는 로그가 매우 길어질 수 있으므로, 전체 테스트 실행 시 stdout/stderr 전체를 대화에 출력하지 않는다.

✅ 2026-08-16 확장: 이 규칙은 테스트에만 국한되지 않는다. **빌드, 애플리케이션 실행, Docker, 의존성 분석, 정적 분석, 크롤링, 데이터베이스 조회 등 대량 출력이 예상되는 명령**은 전부 동일하게 취급한다.

- stdout/stderr는 로그 파일로 저장한다.
- 대화에는 **종료 코드, 핵심 집계, 실패 대상, 최초 원인, 결과물 또는 리포트 경로**만 보고한다.
- 상세 분석이 필요하면 대상 서비스·파일·테스트·요청으로 **범위를 좁혀 다시 실행**하고, 문제 판단에 직접 필요한 로그 구간만 확인한다.

아래 "전체 테스트 실행"·"실패 원인 분석"·"금지 사항" 절은 테스트 실행이라는 가장 흔한 사례를 구체적으로 풀어놓은 것이고, 위 원칙은 테스트 외의 모든 대량 출력 명령에도 동일하게 적용한다.

### 전체 테스트 실행

- 전체 테스트는 stdout/stderr를 로컬 로그 파일로 리다이렉트한다.
- 로그 파일 전체를 대화나 컨텍스트에 출력하지 않는다.
- 테스트 완료 후 먼저 아래 항목만 추출해 보고한다.
  - 종료 코드
  - 실행된 전체 테스트 수
  - 실패한 테스트 수
  - 실패한 테스트 이름
  - 테스트 리포트 경로

### 실패 원인 분석

- 실패 원인 분석이 필요하면 실패한 테스트만 단독 실행한다.
- 단독 실행 로그도 전체를 출력하지 않고, 예외 원인과 직접 관련된 부분만 확인한다.
- 허용되는 출력 범위는 다음 정도로 제한한다.
  - failure message
  - exception type
  - caused by
  - 실패 line
  - 실제 HTTP status / response body 중 실패 판단에 필요한 부분

### 금지 사항

- 전체 Gradle stdout/stderr를 대화에 그대로 출력하지 않는다.
- 전체 XML/HTML 테스트 리포트를 통째로 읽거나 출력하지 않는다.
- Hibernate SQL, Spring Boot context 로그, stack trace 전체를 대량 출력하지 않는다.
- 실패와 무관한 로그를 근거 없이 컨텍스트에 올리지 않는다.
