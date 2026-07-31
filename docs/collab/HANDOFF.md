# HANDOFF — 현재 작업 상태

> ⚠️ 이 문서는 누적 기록이 아니라 **"지금 시점" 스냅샷 1개**다. 작업을 종료할 때 아래 내용을 전부 덮어쓴다.
> 과거 기록이 필요하면 `CHANGELOG.md`를 본다.

- 마지막 갱신: 2026-08-01
- 작성자: Codex
- 작성 브랜치: `feature/application-domain-docs`

## 지금 어디까지 됐는가

- Application 문서를 `docs/specs/application/`으로 이전 완료.
  - `requirements.md`: 기존 `APPLICATION-사용자명세.md` 이동
  - `data-model.md`: 기존 `DB.md`의 Application/Applicant/Receiver/ApplicationMember(2.1~2.4) 분리
  - `api.md`: 기존 `docs/api/application.md` 이동
  - `checklist.md`: Requirements/Data Model/API/상태 전이/테스트/문서 정합성 검증 항목 생성
- `DB.md`의 기존 Application 영역은 새 `data-model.md` 링크로 교체했으며 Payment(2.5) 이후는 아직 이동하지 않음.
- 기존 Application 문서 경로 참조와 `docs/api/README.md` 목차 링크를 새 경로로 수정.
- Application 결제 정책은 상담 확정 금액, 신청일로부터 3일 이내 입금, `RECEIVED`/`REVIEWING`/`PHOTO_REJECTED` 전액 환불 기준을 유지.
- 다른 도메인 문서는 아직 기존 위치에 있음.

## 다음에 할 일

1. 사용자 확인 후 User/Auth 문서를 `docs/specs/user/`로 이전.
2. 이후 Payment → Card → Common/Admin/File/Board 순서로 도메인 문서 이전.
3. 도메인 이전 완료 후 `architecture.md`/`security.md`/`testing.md`, 프로젝트·협업·레거시 문서를 정리.
4. 마지막에 `docs/README.md`, 루트 `AGENTS.md`, `TASK.md`, 중앙 `open-questions.md`를 실제 최종 경로 기준으로 생성.

## ❓ 확인 필요 (사람에게 질문 대기 중)

- 상담 확정 금액을 시스템에 등록하는 주체와 API
- 입금 기한의 신청일 포함 여부와 마감 시각
- `NAME_EDITING` 이후 환불 정책
- 그 외 Application TBD는 `docs/specs/application/requirements.md`와 `api.md` 참고

## 참고

- 관련 TODO 항목: "Application 문서 도메인 패키지 이전"
- 관련 CHANGELOG 항목: 2026-08-01 Codex
