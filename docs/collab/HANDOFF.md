# HANDOFF — 현재 작업 상태

> ⚠️ 이 문서는 누적 기록이 아니라 **"지금 시점" 스냅샷 1개**다. 작업을 종료할 때 아래 내용을 전부 덮어쓴다.
> 과거 기록이 필요하면 `CHANGELOG.md`를 본다.

- 마지막 갱신: 2026-08-01
- 작성자: Claude
- 작성 브랜치: `backend-api` (`feature/application-domain-impl`을 이미 fast-forward 병합·push 완료, 이 문서 작성 시점 작업은 `backend-api`에서 직접 진행 중)

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

**Application 도메인 API 1~5 전부 구현 완료 + `backend-api`에 병합·push 완료:**

- API 1~5(개인/단체 신청 생성, 조회, 사진 재업로드, 카드 다운로드) 전부 구현, 신규 테스트 46개 전부 통과.
- `docs/specs/application/checklist.md` 6개 섹션 자체 검증 완료 — Payment/스케줄러 관련 3개 항목만 이번 구현 범위 밖(api.md 스코프 노트에 명시된 대로)이라 미충족, 나머지 전부 충족.
- `feature/application-domain-impl` → `backend-api` fast-forward 병합(`e3ab484`) → `origin/backend-api` push 완료.

**전체 코드베이스 감사 + 정리 완료** (사용자 요청: "다 갈아엎고 지금 쓰레기 클래스 없음?" → 6개 항목 감사 → 승인 후 실행):

- **삭제**: `infra/card/*`(5개 파일, CitizenCard 삭제 후 orphan), `domain/photo/*` + `api/UploadController.java`(백엔드·프론트 `src/` 어디서도 호출 안 함, `docs/api/upload-file.md`도 "독립 API 불필요"로 이미 결론), `domain/user/dto/{TokenRefreshRequest,TokenRefreshResponse}`(정의만 되고 미사용), `ErrorCode.DUPLICATE_APPLICATION`(미사용) + domain/photo 삭제로 연쇄 orphan된 Upload 관련 에러코드 6개.
- **아키텍처 위반 수정**: `ApplicationService`가 `UserRepository`를 직접 주입하던 것(arch.md "다른 도메인의 Repository를 생성자 주입하지 않는다" 위반) → `UserService.findById()` 경유로 교체.
- **의도적으로 유지한 것**: `CardDesignRepository`/`domain/log/*`(AdminActivityLog/EmailLog) — 현재 호출자 없지만 Admin 도메인 구현 시 필요한 스캐폴딩이라 유지. `ApplicationService`의 `CardTypeRepository`/`UploadFileRepository` 직접 주입 — 전자는 Card 도메인에 Service가 아직 없어서(향후 CardService 승격 권장), 후자는 UploadFile이 애초에 "소유 도메인 없는 공용 엔티티"로 설계돼서 예외로 유지.
- 감사 방법: 코드 전체 grep으로 호출자 추적(문서 읽고 추측 아님), 프론트 `src/` 디렉터리까지 확인.

## 다음에 할 일

1. (Codex) 아래 "확인 필요" 5건을 각 문서에 반영.
2. (Codex) User/Auth 문서 `docs/specs/user/`로 이전 → Payment → Card → Common/Admin/File/Board 순.
3. Payment/상담금액/자동취소/환불 도메인 설계 및 구현 — 다음 단위 작업으로 분리 필요(사람 확인 후 시작).
4. Admin 도메인(사진검토/작명/카드발급/CardDesign 배정) — 아직 전혀 손 안 댐.

## ❓ 확인 필요 (사람에게 질문 대기 중 / Codex 문서 반영 필요)

구현 중 사람에게 직접 확인해서 "결정"까지 끝난 항목(재질문 불필요, **docs/specs/application/\* 문서에만 아직 반영 안 됨**):

- `member.englishName`이 API 1(개인 신청) request/검증/매핑 표에 빠져 있었음(API 2엔 있음) → **추가하기로 확정**, 구현엔 반영됨. `api.md` API 1 섹션에 반영 필요.
- `Application.total_price`(NOT NULL로 명시) → 상담 확정 금액 등록 API가 없어서 **이번 구현 범위에서 컬럼 자체를 만들지 않음**. `data-model.md`/`requirements.md`에 각주 필요.
- 단체 신청 엑셀 일부 행 파싱 실패 시 → **전체 거부(`EXCEL_PARSE_ERROR`)**로 확정(옛 "30% 룰" 미채택). `requirements.md` 9절, `api.md` API 2 Validation 표에 명시 필요.

문서 감사 중 새로 발견한 stale 문구(Codex 문서 소유 영역, Claude가 직접 수정 안 함):

- `docs/api/user.md` 225행 "GET /api/users/me가 아직 미구현" → **이미 구현됨**(UserController), stale 문구 수정 필요.
- `arch.md` 3절 패키지 구조 예시 → `api/admin`·`infra/toss`는 이번 세션에 삭제됨(예시에서 제거 필요), `domain/uploadfile`·`domain/log`는 예시에 아예 없음(추가 필요), `ApplicationResponse.java`는 이제 `ApplicationCreateResponse` 등으로 분리됨(예시 갱신 필요).

아직 사람 확인 없이 구현하며 정한 세부사항(정책 결정은 아니고 구현 디테일, 명세에 없어서 임의로 채운 부분이라 검토 필요):

- **단체신청 ZIP 내부 레이아웃 구체 정의**: 루트에 `.xlsx` 1개 + `photos/{ID}.{ext}`(대소문자·확장자 무시), 1행 "공통 입국날짜", 3행 헤더, 4행부터 데이터, 컬럼 순서 고정. **향후 `GET /api/applications/bulk/template` 구현 시 반드시 일치해야 함** — `api.md`에 명문화 추천.
- **단체 신청 `logo`/`seal`은 학생증 여부 무관 항상 필수**로 구현 — `api.md` Validation 표에 "logo/seal 자체 누락" 케이스 추가 추천.
- `Applicant`의 `postal_code`/`address1`/`address2`는 입력 경로가 없어서 항상 NULL.
- API 5 카드 다운로드(단체) ZIP은 요청마다 새로 생성(캐싱 안 함) — api.md가 구현 세부사항으로 열어둔 부분.

Codex가 이전에 남긴 질문(아직 답변 대기, Application 구현 범위와는 직접 관련 없음):

- 상담 확정 금액을 시스템에 등록하는 주체와 API (위 total_price 항목과 동일 맥락)
- 입금 기한의 신청일 포함 여부와 마감 시각
- `NAME_EDITING` 이후 환불 정책

## 참고

- 관련 TODO 항목: "전체 코드베이스 감사 + 죽은 코드/아키텍처 위반 정리", "Codex: docs/api/user.md 225행 stale 문구 수정", "Codex: arch.md 3절 패키지 구조 예시 최신화"
- 관련 CHANGELOG 항목: 2026-08-01 Claude — `backend-api` (전체 코드베이스 감사 + 정리)
