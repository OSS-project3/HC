# 문서 인덱스 (docs/)

한글과 세종 — 한글 오행 기반 한국 이름 발급 서비스의 문서 모음. 이 파일이 **문서 전체의 진입점**이다.

> 최종 정리: 2026-08-25. 구버전·중복 문서 3건을 삭제·통합하고(아래 "정리 이력" 참고) 인덱스를 신설했다.

---

## 1. 현황·추적 (최상위)

| 문서 | 역할 | 상태 |
|---|---|---|
| [`FRONTEND_API_GAPS.md`](./FRONTEND_API_GAPS.md) | 프론트-백엔드 연동 갭 추적(우선순위·완료 표시). **프론트 작업의 단일 소스** | 우선순위 0~4 완료(2026-08-25) |
| [`BACKEND_TODO.md`](./BACKEND_TODO.md) | **백엔드가 아직 해야 할 것만** 모은 단일 목록(전 도메인, 2026-08-25 코드 검증 기준) | 신설 |
| [`BACKEND_API_GAPS.md`](./BACKEND_API_GAPS.md) | (구버전, 2026-08-18) 프론트 실동작에 필요하나 백엔드에 없는 API — 대부분 이후 구현됨(상단 정정 배너 참고). 최신은 `BACKEND_TODO.md` | 정정됨 |
| [`FRONTEND_API_INTEGRATION_SPEC.md`](./FRONTEND_API_INTEGRATION_SPEC.md) | 프론트 API 연동 준비도·요청/응답 계약 상세(레퍼런스) | 유지 |
| [`LOCALSTORAGE_TO_BACKEND.md`](./LOCALSTORAGE_TO_BACKEND.md) | 브라우저 저장소(localStorage/sessionStorage) 인벤토리·유출점검·백엔드 이전. **저장소 단일 소스** | P0 완료, P1 일부 잔여 |
| [`TEST_REPORT.md`](./TEST_REPORT.md) | 백엔드 테스트 스위트 실행 결과 + 프론트 타입체크/빌드 + 엔드포인트 스모크 | 2026-08-25 실행 |
| [`TEMP_ADMIN_LOGIN.md`](./TEMP_ADMIN_LOGIN.md) | ⚠️ 임시 관리자 계정(운영 배포 전 제거 필수) | 활성 경고 |

## 2. API 레퍼런스 ([`api/`](./api/))

`docs/api/README.md`가 목차. 도메인별 REST 계약: `auth`, `user`, `admin`, `board`, `card`, `card-design`, `card-type`, `payment`, `upload-file`, `common`, `unresolved`.

## 3. 도메인 스펙 ([`specs/`](./specs/))

- [`application/`](./specs/application/) — 신청 도메인(요구사항·데이터모델·서비스흐름·API·체크리스트). 신청 정책의 source of truth.
- [`inquiry/requirements.md`](./specs/inquiry/requirements.md) — 1:1 문의 요구사항·API·Validation.
- [`board/`](./specs/board/) · [`review/`](./specs/review/) · [`events/`](./specs/events/) — 게시판·후기·행사 데이터모델·API.
- [`admin-dashboard/`](./specs/admin-dashboard/) — 관리자 대시보드 설계·상태·백엔드 TODO.
- [`i18n/`](./specs/i18n/) — 다국어 번역 정책.

## 4. 협업·운영 ([`collab/`](./collab/))

`RULES.md`(작업 규칙) · `TODO.md`(작업 로드맵) · `HANDOFF.md`(인수인계) · `CHANGELOG.md` · `PENDING_DECISIONS.md` · `user.md`(회원/탈퇴 정책 source of truth) · `BULK_EXCEL_TEMPLATE_POLICY.md`.

## 5. 아키텍처·배포 (루트)

- [`../arch.md`](../arch.md) — 시스템 아키텍처.
- [`../DOCKER.md`](../DOCKER.md) — Docker Compose 기동·EC2 배포 절차.

---

## 정리 이력 (2026-08-25)

구버전·중복 문서를 정리하고 최신 문서로 통합했다.

| 삭제된 문서 | 사유 | 대체 |
|---|---|---|
| `INTEGRATION_TEST_REPORT.md` (2026-08-18) | 구버전 테스트 리포트 | [`TEST_REPORT.md`](./TEST_REPORT.md) 신설 |
| `FRONTEND_STORAGE_AUDIT.md` (2026-08-25) | `LOCALSTORAGE_TO_BACKEND.md`와 내용 중복(위험도 점검) | [`LOCALSTORAGE_TO_BACKEND.md`](./LOCALSTORAGE_TO_BACKEND.md)로 통합 |
| `FRONTEND_USER_FLOW_AUDIT.md` (2026-08-14) | "진행 중" 시점 스냅샷, 발견 사항 대부분 해소됨 | [`FRONTEND_API_GAPS.md`](./FRONTEND_API_GAPS.md)가 최신 갭을 추적 |

> 삭제 문서는 모두 git 이력에 남아 있어 필요 시 복원 가능(`git log --diff-filter=D -- docs/`).
