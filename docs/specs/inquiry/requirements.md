# Inquiry(1:1 문의) 요구사항 · 정책

> 백엔드에 도메인 자체가 아직 없다(`Inquiry` 엔티티/Controller 전무, `docs/collab/TODO.md` 진행 보드 참고). 이 문서는 착수 전 정책을 계속 확정해나가는 running 문서다 — 실제 API 요청/응답 JSON, DB 컬럼 상세는 구현 착수 시 `api.md`/`data-model.md`로 분리한다(Board/Review 도메인과 동일 패턴).

## ① 도메인의 책임

- 로그인 사용자가 1:1 문의를 접수한다.
- 관리자가 접수된 문의를 조회하고 답변을 등록한다.
- 사용자는 본인이 접수한 문의의 목록·상세와 답변을 확인한다.

## ② 프론트 실제 구조 (기준, 2026-08-19 코드 확인)

| 파일 | 구조 |
|---|---|
| `pages/InquiryPage.tsx` | 문의 작성 폼. **로그인 필수**(`if (!user)`면 폼 대신 로그인 유도 화면만 노출). 입력 필드: 문의 유형(`category`, 고정 5개 값 중 선택), 이름(`name`, 기본값 로그인 사용자 이름), 이메일(`email`, 기본값 로그인 사용자 이메일), 연락처(`phone`), 제목(`title`, 최대 80자), 문의 내용(`content`, 최대 2000자), 개인정보 수집 동의 체크박스(필수, 서버에 전송하지 않는 UI 전용 게이트). 제출 시 `status: "PENDING"`으로 로컬 저장. |
| `pages/InquiryDetailPage.tsx` | 문의 상세. 접근 제어는 현재 `user.email === inquiry.email \|\| user.role === "admin"`(이메일 문자열 비교) — 백엔드는 이 방식을 재현하지 않고 `userId` 기준으로 소유권을 검증한다(§④ 참고). 상태 배지(답변 대기/문의 완료), 문의 내용, 답변(있으면) 표시. |
| `pages/MyPage.tsx` | "문의 내역" 섹션. `inquiry.email === user.email`로 필터링(→ 백엔드는 `userId`로 대체)해 제목+상태+접수일만 목록 표시, 클릭 시 상세로 이동. |
| `pages/AdminPage.tsx` | 관리자 문의 테이블. 컬럼: 문의유형/제목(클릭 시 펼쳐서 내용+답변 textarea+저장 버튼 노출)/문의자/이메일/연락처/접수일/처리상태(드롭다운). **검색·페이지네이션 UI 없음**(전체 나열). 답변 textarea는 `value={draft ?? inquiry.answer ?? ""}`로 신규 작성·기존 답변 수정을 같은 UI로 처리한다. 처리상태 드롭다운은 답변 저장 버튼과 **별개**로 존재해 `PENDING`/`COMPLETED`를 답변 내용과 무관하게 직접 전환할 수 있다. |
| `data/inquiries.ts` | `localStorage["customer-inquiries"]`. `InquiryRecord`: `id, category, name, email, phone, title, content, createdAt, status("PENDING"\|"COMPLETED"), answer?, answeredAt?` |

## ③ 확정된 서비스 흐름 (2026-08-19)

```
[사용자 로그인]
      ↓
1:1 문의 작성
      ↓
JWT에서 userId 추출
      ↓
문의 내용 검증
      ↓
Inquiry 저장 (status = PENDING)
      ↓
마이페이지에서 본인 문의 목록/상세 조회
      ↓
관리자가 관리자 페이지에서 문의 조회
      ↓
관리자 답변 등록 → status = COMPLETED, answer/answeredAt 저장
      ↓
사용자가 답변 확인
```

프론트 코드(§②) 전수 대조로 이 흐름이 실제 화면 동작과 일치함을 확인했다.

## ④ 확정된 API 목록 (2026-08-19)

| 메서드/경로 | 용도 | 인증 |
|---|---|---|
| `POST /api/inquiries` | 문의 등록. `userId`는 요청 바디에 없음 — JWT(`@AuthenticationPrincipal`)에서 추출(`RULES.md` §3 원칙) | USER |
| `GET /api/my/inquiries` | 내 문의 목록(호출자 `userId` 기준) | USER |
| `GET /api/my/inquiries/{id}` | 내 문의 상세(소유자만) | USER |
| `GET /api/admin/inquiries` | 관리자 전체 목록. **검색 파라미터 없음** — 프론트에 검색 UI 자체가 없어(§②) 이번 범위 밖. 필요해지면 Board/Review의 `keyword`/`searchType` 패턴 재사용 | 관리자 |
| `GET /api/admin/inquiries/{id}` | 관리자 상세 | 관리자 |
| `PATCH /api/admin/inquiries/{id}/answer` | 답변 등록/수정(신규·수정 동일 API, `AdminPage.tsx`와 동일하게). 성공 시 `status=COMPLETED`로 함께 전이 | 관리자 |
| `PATCH /api/admin/inquiries/{id}/status` | 답변과 무관하게 상태만 독립 변경(`PENDING`↔`COMPLETED`) — `AdminPage.tsx`의 별도 상태 드롭다운 동작을 그대로 유지하기 위해 신설 | 관리자 |

`POST /api/admin/inquiries/{id}/answer`가 아니라 `PATCH`인 이유: 기존 리소스(답변 필드)를 갱신하는 동작이라 `PATCH`가 더 정확하고, 프론트도 신규/수정을 같은 UI·같은 호출로 처리한다.

## ⑤ 확정 정책

- **비회원 문의 불가(2026-08-19, 코드 확인으로 해소)**: `InquiryPage.tsx`가 로그인하지 않은 사용자에게 폼을 보여주지 않는다 — `POST /api/inquiries`는 `USER` 권한 전용으로 설계.
- **소유권은 `userId`로 판별(2026-08-19)**: 프론트는 이메일 문자열 비교로 "내 문의"를 가리지만, 로그인이 필수인 이상 백엔드는 이 방식을 재현하지 않는다. `Inquiry.userId`를 `@AuthenticationPrincipal`로 채우고 그 값으로 소유권을 검증한다.
- **검색 API는 이번 범위 아님(2026-08-19)**: 프론트 구조와 동일하게 유지 — 관리자 목록은 전체 나열만 지원, 검색·필터·페이지네이션 파라미터를 추가하지 않는다.
- **답변 등록/수정은 `PATCH .../answer` 하나로 유지(2026-08-19)**: 별도의 "수정" API를 새로 만들지 않는다.
- **문의 제출 후 사용자의 수정·삭제 불가(2026-08-19 확정)**: 사용자가 접수한 문의는 이후 내용을 수정하거나 삭제할 수 없다. 프론트에도 대응하는 UI가 없다(작성 폼만 있고, 상세 페이지에는 수정·삭제 버튼이 없음). `PATCH /api/my/inquiries/{id}`나 `DELETE /api/my/inquiries/{id}` 같은 API는 만들지 않는다.

## ⑥ 아직 정하지 않은 것

- `category`를 백엔드에서 자유 문자열로 저장할지, `InquiryCategory` enum(프론트 고정 5개 값: 제작 신청/결제 및 배송/카드 발급/행사·단체 협업/기타)으로 강제할지.
- 문의 등록에 대한 스팸/남용 방지(예: 일일 등록 횟수 제한) 필요 여부.
- 관리자 답변에 대한 재문의(추가 질문) 흐름 필요 여부 — 현재 프론트는 문의 1건당 답변 1회의 단발성 구조로 보이며 스레드형 대화 UI가 없음.
- 첨부파일 지원 여부 — 현재 프론트 폼에 파일 첨부 입력이 없음.

## 관련 문서

- `docs/FRONTEND_API_GAPS.md` §1.3 — 이 문서와 동일한 내용을 프론트-백엔드 갭 관점에서 요약(향후 이 문서가 source of truth가 되면 §1.3은 이 문서를 가리키도록 정리)
- `docs/collab/TODO.md` 진행 보드 "Inquiry(1:1 문의) 도메인 신규 구현" 행
- `docs/collab/RULES.md` §3 — `userId`는 JWT에서만 추출한다는 공용 원칙
