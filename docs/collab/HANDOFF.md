# HANDOFF — 현재 작업 상태

> ⚠️ 이 문서는 누적 기록이 아니라 **"지금 시점" 스냅샷 1개**다. 작업을 종료할 때 아래 내용을 전부 덮어쓴다.
> 과거 기록이 필요하면 `CHANGELOG.md`를 본다.

- 마지막 갱신: 2026-08-01
- 작성자: Claude
- 작성 브랜치: `backend-api`

---

## 템플릿 (종료 시 이 구조로 전체 교체)

```md
- 마지막 갱신: {YYYY-MM-DD}
- 작성자: {Claude|Codex}
- 작성 브랜치: {브랜치명}

## 지금 어디까지 됐는가
- {완료된 것을 구체적으로}

## 다음에 할 일
- {바로 이어서 할 작업, 우선순위 순}

## ❓ 확인 필요 (사람에게 질문 대기 중)
- {문서 충돌 / 모호한 요구사항 / 판단 보류 항목}
- 없으면 "없음"이라고 명시한다

## 참고
- 관련 TODO 항목: {TODO.md의 행}
- 관련 CHANGELOG 항목: {날짜}
```

---

## 지금 어디까지 됐는가

- User/Auth 도메인: 구현·단위테스트·통합테스트·문서화(`user-test-result.md`) 완료, `backend-api`에 병합됨.
  - Refresh Token Rotation, Reuse Detection, Logout+블랙리스트, CRUD(조회/수정/탈퇴/자동복구/익명화 스케줄러) 전부 실제 서버 대상 검증 완료.
  - 로컬 개발 DB를 H2 → PostgreSQL로 전환 완료 (`application-local.properties`, gitignored).
- Application 도메인: 요구사항 정리 단계.
  - `APPLICATION-사용자명세.md` 작성 완료 — 4종 카드(명예한국인증/명예시민증/방문증/학생증), 학생증 전용 필드(학번/학과/학교로고/학교직인), CardDesign은 **관리자 배정**(사용자는 CardType만 선택)으로 정책 확정, entry_date 공통값+예외 처리 로직, 단체신청 신청자별 email/phone 구조 확정.
  - `DB.md`, `docs/api/application.md`(구 `API-명세.md`)에 위 결정사항 전파 완료.
  - Codex가 `API-명세.md`를 도메인별로 `docs/api/*.md`로 분리 — 원본과 대조해 내용 유실 없음 확인함.
  - Codex가 작성한 `arch.md`(4계층+Port/Adapter, 945줄)를 실제 코드 규모에 맞게 Claude가 단순화함 (3단 구조로 축소, 비즈니스 규칙 절은 유지). 아직 커밋 안 됨 — `codex-docs` 워크트리에 파일만 수정된 상태.
- 워크트리 구조: `claude-impl`(구현), `codex-docs`(문서), `backend-api`(병합 대상) 3개 확립, 원격에도 반영됨.
- 협업 문제 인지: Claude/Codex가 서로 다른 워크트리에서 작업하며 상대 변경을 몰라서 뒤늦게 발견하는 문제가 실제로 발생함
  (`D:\HC\arch.md`에 편집 전 원본이, `codex-docs\arch.md`에 편집본이 따로 존재 — 같은 문제의 실례).
  이를 해결하기 위해 이번 작업으로 `docs/collab/` 체계(`RULES.md`/`TODO.md`/`CHANGELOG.md`/`HANDOFF.md`)를 도입함.

## 다음에 할 일

1. `docs/collab/` 4개 파일을 `claude-impl`, `codex-docs` 워크트리에도 동일하게 배치하고 각 브랜치에 커밋.
2. 루트 `guide.md`를 `docs/collab/RULES.md`를 가리키는 안내문으로 교체 (파일명 대소문자 오탈자 `application-사용자명세.md`→`APPLICATION-사용자명세.md`도 이 참에 수정).
3. `D:\HC\arch.md`(루트에 남아있는 미편집 원본, untracked)를 `_quarantine/`으로 이동해 정리.
4. Codex: `codex-docs` 워크트리의 `arch.md` 단순화본을 확인하고 커밋.
5. Claude: `claude-impl`에서 Application 도메인 엔티티/API 구현 착수 여부를 사람에게 확인 후 시작.

## ❓ 확인 필요 (사람에게 질문 대기 중)

- 없음 (단, `TODO.md`의 TBD 항목들은 각자 해당 작업 착수 시점에 다시 확인 필요)

## 참고

- 관련 TODO 항목: "협업 규칙 체계(`docs/collab/`) 도입", "`arch.md` 구조를 실제 코드 규모에 맞게 단순화"
- 관련 CHANGELOG 항목: 2026-08-01
