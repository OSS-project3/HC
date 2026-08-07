# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-07
- 작성자: Codex
- 작성 브랜치: codexdocs/application-policy-sync

## 지금 어디까지 됐는가

- APPLICATION.md와 POLICY_SYNC_CHECKLIST.md 기준 Application 문서 동기화를 완료했다.
- Code Audit을 실제 구현의 파일·클래스·메서드·라인 근거로 보강했다.
- 코드 파일은 수정하지 않았다.
- (Claude 추가 2026-08-07: 위 동기화 중 학생증 `department`(학과) 필드 삭제만 제외 — `APPLICATION.md`/patch 어디에도 근거가 없고 사람이 "아직 미결정"으로 확인해서, `department` 필드·필수 검증은 기존대로 유지했다. `PENDING_DECISIONS.md` 참고.)

## 다음에 할 일

- `docs/specs/application/checklist.md`의 수정 필요와 미구현 항목을 기준으로 별도 승인 후 구현한다.
- TBD는 `docs/collab/PENDING_DECISIONS.md`에서 결정 전까지 유지한다.
- 학생증 `department`(학과) 필드를 실제로 제외할지는 별도 결정 필요 — 결정되면 `checklist.md`/`requirements.md`/`data-model.md`/`api.md`/`TODO.md`를 다시 동기화해야 한다.

## ❓ 확인 필요

- `docs/collab/PENDING_DECISIONS.md`의 Application TBD 항목
- 학생증 `department`(학과) 필드를 계속 유지할지, 제외할지

## 참고

- 관련 TODO: Application 정책 동기화 Audit 후속 작업 (2026-08-07)
- 관련 CHANGELOG: 2026-08-07 — Application 정책 문서 동기화 및 Code Audit
