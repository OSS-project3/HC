# HANDOFF — 현재 작업 상태

> ⚠️ 이 문서는 누적 기록이 아니라 **"지금 시점" 스냅샷 1개**다. 작업을 종료할 때 아래 내용을 전부 덮어쓴다.
> 과거 기록이 필요하면 `CHANGELOG.md`를 본다.

- 마지막 갱신: 2026-08-01
- 작성자: Claude (Codex의 `backend-api` 스냅샷과 병합해 재작성)
- 작성 브랜치: `feature/application-domain-impl` (`backend-api`의 Codex 문서 재구성 커밋 `ecd72b3`을 머지함)

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

**문서 (Codex, `feature/application-domain-docs` → `backend-api`에 병합됨):**
- Application 문서를 `docs/specs/application/`으로 이전 완료 — `requirements.md`(구 `APPLICATION-사용자명세.md`), `data-model.md`(구 `DB.md` 2.1~2.4), `api.md`(구 `docs/api/application.md`), `checklist.md`(신규).
- `DB.md`의 Application 영역은 `data-model.md` 링크로 교체. Payment(2.5) 이후는 아직 미이전.
- `docs/api/README.md` 등 기존 경로 참조를 새 경로로 수정.
- 다른 도메인(User/Auth/Payment/Card/Common/Admin/File/Board) 문서는 아직 기존 위치.

**구현 (Claude, `feature/application-domain-impl`):**
- 위 `docs/specs/application/*` 기준으로 Application 도메인을 처음부터 재구현, 커밋 완료(`0dd1067`, `a6f013a`), 방금 `backend-api`(Codex 문서 병합본)를 머지해 두 작업이 한 브랜치에 합쳐짐.
- **삭제**: 옛 모델을 참조하던 `BulkOrder`/`CitizenCard`/`KoreanName`/`Payment`/`Shipping` 도메인 전체, 옛 `Application`/`CardType`(enum)/`ApplicationStatus` — 새 명세에 없고 카드번호/사진/카드이미지가 `ApplicationMember`로 흡수되면서 대체됨(사람 확인 후 진행).
- **신규 엔티티**: `CardType`(entity), `CardDesign`, `UploadFile`, `Applicant`, `Receiver`, `ApplicationMember`, 재작성된 `Application`(8단계 상태머신). 엔티티 단위테스트 19개.
- **API 1** `POST /api/applications`(개인 신청) — 서비스 9테스트 + 컨트롤러 4테스트.
- **API 2** `POST /api/applications/bulk`(단체 ZIP 신청) — 신규 `BulkExcelParser`(POI 기반), 서비스 7테스트 + 컨트롤러 3테스트.
- **API 3** `POST /api/applications/lookup`(신청 조회, 비로그인) — 서비스 9테스트. `SecurityConfig`에 permitAll 추가.
- 부수 수정: `GlobalExceptionHandler`에 `MissingServletRequestPartException` 핸들러 추가(500→400).
- API 4(사진 재업로드), API 5(카드 다운로드) 아직 미착수.
- User/Auth 도메인은 이전 세션에 구현·테스트·문서화 완료, `backend-api`에 이미 병합됨.

## 다음에 할 일

1. API 4(`PATCH /api/applications/{id}/photo`), API 5(`GET /api/applications/{id}/cards/download`) 구현 + 테스트.
2. `checklist.md`(Requirements/Data Model/State Transitions/API Contract/Tests/Documentation Consistency) 최종 검증.
3. 아래 "확인 필요" 스펙 반영을 Codex 쪽에 요청(Claude는 `docs/specs/application/*` read-only).
4. (Codex) User/Auth 문서 `docs/specs/user/`로 이전 → Payment → Card → Common/Admin/File/Board 순.
5. API 4/5까지 끝난 뒤 `backend-api` 재병합 여부 사람에게 확인.

## ❓ 확인 필요 (사람에게 질문 대기 중 / Codex 문서 반영 필요)

구현 중 사람에게 직접 확인해서 "결정"까지 끝난 항목(재질문 불필요, **docs/specs/application/\* 문서에만 아직 반영 안 됨**):

- `member.englishName`이 API 1(개인 신청) request/검증/매핑 표에 빠져 있었음(API 2엔 있음, requirements/data-model엔 필수로 명시) → **추가하기로 확정**, 구현엔 반영됨. `api.md`의 API 1 섹션에 반영 필요.
- `Application.total_price`(NOT NULL로 명시되어 있었음) → 상담 확정 금액을 등록하는 API 자체가 아직 없어서 **이번 구현 범위에서 컬럼 자체를 만들지 않음**. `data-model.md` 2.1절의 `total_price` 행에 "결제/상담 도메인 설계 시 추가 예정"이라는 각주 필요, `requirements.md` 10절 TBD에도 반영 필요.
- 단체 신청 엑셀 일부 행 파싱 실패 시 처리 → 옛 "30% 룰"은 가져오지 않고 **한 행이라도 실패하면 전체 거부(`EXCEL_PARSE_ERROR`)**로 확정. `requirements.md` 9절의 해당 TBD, `docs/specs/application/api.md` API 2 Validation 표에 명시 필요.

아직 사람 확인 없이 구현하며 정한 세부사항(정책 결정은 아니고 구현 디테일이지만, 명세에 없어서 임의로 채운 부분이라 검토 필요):

- **단체신청 ZIP 내부 레이아웃을 구체적으로 정의함**: 루트에 `.xlsx` 1개 + `photos/{ID}.{ext}` 사진들(대소문자·확장자 무시 매칭), 엑셀은 1행에 "공통 입국날짜" 라벨/값, 3행 헤더, 4행부터 데이터, 컬럼 순서 고정(ID·영문명·생년월일·국적·출생시간·출생지역·성별·개별입국날짜·이메일·전화번호·주소·[학번·학과]). `requirements.md`는 ASCII 목업만 제공하고 정확한 셀 좌표가 없었음 — **향후 `GET /api/applications/bulk/template` 구현 시 이 레이아웃과 반드시 일치해야 함**. `api.md`나 별도 템플릿 문서에 이 레이아웃을 명문화 추천.
- **단체 신청의 `logo`/`seal`은 학생증 여부와 무관하게 항상 필수**로 구현(일반 카드=회사로고/직인, 학생증=학교로고/직인 — `api.md` 본문 설명은 있었으나 Validation 표엔 학생증 케이스만 명시돼 있었음). `api.md` API 2 Validation 표에 "logo/seal 자체 누락" 케이스 추가 추천.
- `Applicant`의 `postal_code`/`address1`/`address2`는 API 1/2 어느 요청에도 입력 필드가 없어서 **항상 NULL로 둠** — `data-model.md` 2.2절의 "수령인 주소를 복사하는 등" 문구는 구현하지 않음(정책이 아니라 예시로만 적혀 있었음).

Codex가 이전에 남긴 질문(아직 답변 대기, Application 구현 범위와는 직접 관련 없음):

- 상담 확정 금액을 시스템에 등록하는 주체와 API (위 total_price 항목과 동일 맥락)
- 입금 기한의 신청일 포함 여부와 마감 시각
- `NAME_EDITING` 이후 환불 정책

## 참고

- 관련 TODO 항목: "Application 도메인 엔티티/API 구현", "Codex: HANDOFF.md 확인 필요 3건을 docs/specs/application/*에 반영"
- 관련 CHANGELOG 항목: 2026-08-01 Claude — `feature/application-domain-impl`, 2026-08-01 Codex — `feature/application-domain-docs`
