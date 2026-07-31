# 협업 규칙 (Claude / Codex 공용)

> 이 문서는 `guide.md`(구 "Application 도메인 작업 가이드")를 대체·확장한다.
> Claude와 Codex가 서로 다른 git worktree/브랜치에서 동시에 작업할 때,
> **사람이 매번 상대방의 변경사항을 프롬프트로 전달하지 않아도** 스스로 이어받을 수 있게 하는 것이 목적이다.

핵심 메커니즘: 작업을 시작할 때 `docs/collab/HANDOFF.md`를 (내 브랜치 + 상대 브랜치 양쪽에서) 읽는 것을
**작업 절차의 0단계로 강제**한다. 사람의 개입 없이도 "상대가 뭘 바꿨는지"를 여기서 알 수 있다.

---

## 1. 작업 공간

| 워크트리 | 브랜치 | 담당 | 역할 |
|---|---|---|---|
| `D:\HC-worktrees\claude-impl` | `feature/application-domain-impl` | Claude | 구현(코드) |
| `D:\HC-worktrees\codex-docs` | `feature/application-domain-docs` | Codex | 문서·아키텍처·로직 결정 |
| `D:\HC` | `backend-api` | 공용 | 두 브랜치의 병합 대상 |

`docs/collab/` 디렉터리(이 폴더)는 **세 워크트리 모두에 동일 구조로 존재**해야 하며, 각자의 브랜치에 커밋한다.
서로 다른 브랜치에 있는 사본이므로 실시간으로 자동 동기화되지는 않는다 — 그래서 아래 2단계 확인이 필요하다.

```
git fetch origin
git show origin/feature/application-domain-docs:docs/collab/HANDOFF.md   # 상대 브랜치 최신 스냅샷
git show origin/feature/application-domain-impl:docs/collab/HANDOFF.md
```

---

## 2. 문서 우선순위

### 0순위 — 작업 상태 (가장 먼저 확인)

- `docs/collab/HANDOFF.md` — 마지막 작업자가 남긴 "지금 상태" 스냅샷. 항상 최신 1개만 존재(누적 아님).
- `docs/collab/TODO.md` — 현재 진행중/대기/블로킹 작업 목록과 담당자.

### 1순위 — 기준 문서 (Source of Truth)

- `APPLICATION-사용자명세.md`
  - Application 도메인의 요구사항·비즈니스 규칙을 정의한 기준 문서. 구현·API 설계는 이 문서를 최우선으로 따른다.
  - ⚠️ 파일명은 대문자 `APPLICATION`으로 시작한다 (Windows는 대소문자를 구분하지 않아 오탈자가 숨을 수 있으니 주의).

### 2순위 — 도메인 API

- `docs/api/{도메인}.md` (예: `docs/api/application.md`, `docs/api/user.md`)
  - 도메인별 API 명세. 해당 도메인 API는 이 파일에서만 관리한다.

### 3순위 — 공통 문서

- `docs/api/README.md` — 전체 API 문서 목차
- `docs/api/common.md` — 공통 응답 형식·인증 방식·에러 코드

### 4순위 — 참고 문서

- `arch.md` — 아키텍처·개발 규칙
- `DB.md` — 테이블·컬럼 구조
- `docs/api/unresolved.md` — 미결정 사항 목록

### 문서 갱신 전파 방향

요구사항이 바뀌면 반드시 이 순서로 전파한다:
`APPLICATION-사용자명세.md` → `DB.md` → `docs/api/{도메인}.md` → (구조에 영향 있으면) `arch.md`

---

## 3. 작업 원칙 (기존 guide.md 승계)

- `APPLICATION-사용자명세.md`를 Source of Truth로 사용한다.
- 공통 규칙(응답 형식, 인증, 에러 코드)은 `docs/api/common.md`를 따르며 중복 작성하지 않는다.
- 문서 간 내용이 충돌하면 임의로 구현·수정하지 말고, 충돌 내용을 `HANDOFF.md`의 "❓ 확인 필요"에 기록한 뒤 사람에게 질문한다.
- 미결정 사항(Unresolved)이 있거나 요구사항이 모호하면 구현하지 말고 먼저 질문한다.
- 작업에 필요한 문서만 참고하며, 관련 없는 문서는 임의로 고치지 않는다.
- 한 번에 전체 도메인을 작업하지 않는다. 기능 단위(예: 신청 생성 API / 신청 조회 API)로 잘게 나눈다.

---

## 4. 작업 시작 절차 (체크리스트)

- [ ] `git fetch origin`
- [ ] 내 워크트리에서 `git pull` (내 브랜치 최신화)
- [ ] `docs/collab/HANDOFF.md`(내 브랜치) 읽기 — 내가 어디까지 했는지 확인
- [ ] `git show origin/<상대 브랜치>:docs/collab/HANDOFF.md` 로 상대방 최신 스냅샷 확인 — 아직 안 머지된 변경까지 파악
- [ ] `docs/collab/TODO.md` 읽고, 맡을 작업 행에 담당자·상태를 `🔵 진행중`으로 표시 후 커밋+푸시 (다른 작업자와 중복 방지)
- [ ] `docs/collab/CHANGELOG.md` 최근 5~10개 항목 훑어보기
- [ ] 이번 작업 범위에 맞는 1~4순위 문서 확인
- [ ] 문서 간 충돌이나 모호한 요구사항 발견 시 → 진행하지 말고 `HANDOFF.md`에 질문 기록 후 사람에게 확인

## 5. 작업 종료 절차 (체크리스트)

- [ ] 변경 내용이 아래 4가지 기준과 일치하는지 검증
  - [ ] `APPLICATION-사용자명세.md`와 일치하는가
  - [ ] `DB.md` 구조와 일치하는가
  - [ ] `docs/api/common.md` 공통 규칙을 준수하는가
  - [ ] 기존 API/문서와 충돌하지 않는가
- [ ] `docs/collab/TODO.md` 갱신 (완료 체크, 새로 발견한 작업 추가, 블로킹이면 `🔴`로 표시)
- [ ] `docs/collab/CHANGELOG.md` 맨 위에 새 항목 추가 (템플릿 참고)
- [ ] `docs/collab/HANDOFF.md` 전체를 지금 상태로 덮어쓰기 (이어붙이지 않는다 — 항상 최신 스냅샷 1개)
- [ ] `docs/collab/*` 포함하여 커밋, 내 feature 브랜치에 push
- [ ] 기능 단위 작업이 검증까지 끝났다면 `backend-api` 병합 여부를 사람에게 확인 후 병합

---

## 6. 문서별 갱신 책임

| 상황 | 반드시 수정해야 하는 문서 |
|---|---|
| 요구사항/정책 변경 | `APPLICATION-사용자명세.md` → `DB.md` → `docs/api/{도메인}.md` |
| API 엔드포인트 추가/변경 | `docs/api/{도메인}.md` |
| DB 스키마 변경 | `DB.md` |
| 패키지 구조 등 아키텍처 변경 | `arch.md` |
| 코드 구현/리팩터링 | 코드 (+ 구조가 바뀌면 `arch.md`) |
| 결정 보류 항목 발생 | `docs/api/unresolved.md` |
| **모든 작업 종료 시 (예외 없음)** | `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/HANDOFF.md` |

---

## 7. `docs/collab/*` 병합 규칙

- `TODO.md`, `CHANGELOG.md`는 append형이라 자동 병합(merge)이 대체로 안전하다. 표 순서가 꼬이면 병합 후 한 번 정리한다.
- `HANDOFF.md`는 "스냅샷" 문서라 자동 병합하지 않는다. `backend-api`로 병합할 때 충돌이 나면
  두 쪽 내용을 참고해 **새로 하나로 다시 작성**한다 (둘 중 하나를 임의로 버리지 않는다).
- 협업 문서(`docs/collab/*`) 변경은 가능하면 코드/기능 변경과 **별도 커밋**으로 분리한다.
  예: `chore(collab): update handoff`
