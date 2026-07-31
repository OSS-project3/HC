# TODO (작업 보드)

상태 아이콘: ✅ 완료 · 🔵 진행중 · ⚪ 대기 · 🔴 블로킹(질문 대기)

작업을 시작할 때 상태를 `🔵 진행중`으로, 담당자를 채우고 커밋한다.
작업을 마칠 때 `✅ 완료`로 바꾸고 `CHANGELOG.md`에 항목을 추가한다.

## 템플릿 (새 작업 추가 시)

```md
| ⚪ | {작업명} | {담당: Claude/Codex/미정} | {브랜치} | {관련 문서} | {비고} |
```

---

## 진행 보드

| 상태 | 작업 | 담당 | 브랜치 | 관련 문서 | 비고 |
|---|---|---|---|---|---|
| ✅ | User CRUD (조회/수정/탈퇴/자동복구/익명화 스케줄러) 구현+테스트 | Claude | `backend-api` (병합됨) | `backend/honor-citizen/docs/test/user-test-result.md` | - |
| ✅ | API-명세.md → `docs/api/*.md` 도메인별 분리 | Codex | `feature/application-domain-docs` | `docs/api/README.md` | 원본과 대조하여 내용 유실 없음 확인 완료 |
| ✅ | `arch.md` 구조를 실제 코드 규모에 맞게 단순화 | Claude | `feature/application-domain-docs` | `arch.md` | 비즈니스 규칙 절은 유지, 계층/패키지 구조만 축소 |
| 🔵 | 협업 규칙 체계(`docs/collab/`) 도입 | Claude | `backend-api` | `docs/collab/RULES.md` | 이 작업 자체 |
| ⚪ | Application 도메인 엔티티/API 구현 착수 | Claude | `feature/application-domain-impl` | `docs/api/application.md`, `APPLICATION-사용자명세.md` | Codex 문서 작업 안정화 후 시작 여부 확인 필요 |
| ⚪ | CardDesign 관리자 배정 API/화면 흐름 확정 | 미정 | - | `docs/api/card-design.md` | unresolved.md의 "CardDesign 배정 시점" TBD 선결 필요 |
| ⚪ | 학번/학과 형식 제약 확정 | 미정 | - | `APPLICATION-사용자명세.md` | TBD |
| ⚪ | 학생증 디자인 시안 반영 | 미정 | - | `DB.md`, `docs/api/card-design.md` | 시안 미도착 |
| ⚪ | lookup API 전화/이메일 인증 조합 확정 | 미정 | - | `docs/api/application.md` | TBD |
| ⚪ | 단체신청 엑셀 실패율 처리 규칙 확정 | 미정 | - | `docs/api/application.md` | TBD |
| ⚪ | 신청내용 수정 API 필요 여부 결정 | 미정 | - | `docs/api/application.md` | TBD |
| ⚪ | refresh 토큰 세션 저장소(DB vs Redis) 결정 | 미정 | - | `arch.md` | TBD |
| ⚪ | MOBILE_AND_PHYSICAL 실물배송 흐름 정의 | 미정 | - | `docs/api/application.md` | TBD |
| ⚪ | 영업일 계산 기준 확정 | 미정 | - | `docs/api/application.md` | TBD |
