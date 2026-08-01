# HANDOFF — 현재 작업 상태

> ⚠️ 이 문서는 누적 기록이 아니라 **"지금 시점" 스냅샷 1개**다. 작업을 종료할 때 아래 내용을 전부 덮어쓴다.
> 과거 기록이 필요하면 `CHANGELOG.md`를 본다.

- 마지막 갱신: 2026-08-01
- 작성자: Claude
- 작성 브랜치: `feature/application-domain-impl` (`backend-api`의 Codex 문서 재구성 커밋 `ecd72b3`을 이미 머지함)

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

**Application 도메인 API 1~5 전부 구현 완료** (`docs/specs/application/api.md` 기준 5개 API 모두):

- **API 1** `POST /api/applications`(개인 신청) — 서비스 9테스트 + 컨트롤러 4테스트.
- **API 2** `POST /api/applications/bulk`(단체 ZIP 신청) — 신규 `BulkExcelParser`(POI 기반 ZIP+XLSX 파서), 서비스 7테스트 + 컨트롤러 3테스트.
- **API 3** `POST /api/applications/lookup`(신청 조회, 비로그인) — 서비스 9테스트. `SecurityConfig`에 permitAll 추가.
- **API 4** `PATCH /api/applications/{id}/photo`(사진 재업로드) — 개인은 `ApplicationMember.photo_path` 갱신, 단체는 ZIP 재파싱 후 `ApplicationMember` 전체 재생성 + `total_quantity` 갱신. 서비스 6테스트 + 컨트롤러 2테스트.
- **API 5** `GET /api/applications/{id}/cards/download`(카드 다운로드) — 개인은 presigned URL 2장, 단체는 앞/뒤 이미지를 묶어 ZIP 생성 후 presigned URL 1개. 서비스 5테스트 + 컨트롤러 2테스트.
- 엔티티(`Application`/`Applicant`/`Receiver`/`ApplicationMember`/`CardType`/`CardDesign`/`UploadFile`) 단위테스트 19개 포함, **총 46개 신규 테스트 전부 통과**.
- 부수 정리: `GlobalExceptionHandler`에 `MissingServletRequestPartException` 핸들러 추가(500→400). `ErrorCode`에서 삭제된 레거시 도메인(Shipping/KoreanName/CitizenCard/BulkOrder/Payment) 전용 코드 정리, `CARD_NOT_READY`는 Application 범용으로 재사용. 그 결과 컴파일이 깨진 orphan `infra/toss/*`(TossPayments 클라이언트, Payment 도메인 삭제 후 아무도 안 씀)도 함께 삭제.
- `backend-api`(Codex의 `docs/specs/application/*` 문서 재구성, 커밋 `ecd72b3`)를 이 브랜치로 머지 완료 — `docs/collab/*` 4개 파일만 충돌, RULES.md 7절 방식대로 수동 재작성해서 해결.
- `checklist.md` 6개 섹션 자체 검증 결과는 아래 참고.
- User/Auth 도메인은 이전 세션에 구현·테스트·문서화 완료, `backend-api`에 이미 병합됨.

**`checklist.md` 검증 결과 요약:**
- 1(Requirements): 대부분 충족. 미충족 3개는 전부 **Payment/스케줄러 도메인 자체가 이번 구현 범위 밖**이라 발생 — 상담 확정 금액 저장, 3일 미입금 자동취소, RECEIVED/REVIEWING/PHOTO_REJECTED 전액환불. (api.md 스코프 노트에도 "이번 패스는 사용자가 신청을 만들고 조회하는 흐름만" 이라고 명시돼 있어 예상된 범위 밖임.)
- 2(Data Model): 8개 항목 전부 충족.
- 3(State Transitions): 8개 항목 전부 충족(`ApplicationTest`로 검증).
- 4(API Contract): 6개 항목 전부 충족(엔티티 직접 노출 없음, 공통 응답/에러 형식 준수 확인).
- 5(Tests): 5개 항목 전부 충족.
- 6(Documentation Consistency): 필드명/enum 불일치 1건(englishName, 아래 참고)은 코드엔 반영, 문서 미반영. 새 미결정사항은 이 문서에 기록(→ Codex에게 보고 완료, `docs/api/unresolved.md`는 Claude가 직접 못 씀).

## 다음에 할 일

1. (Codex) 아래 "확인 필요" 3건을 `docs/specs/application/*`에 반영.
2. (Codex) User/Auth 문서 `docs/specs/user/`로 이전 → Payment → Card → Common/Admin/File/Board 순.
3. Payment/상담금액/자동취소/환불 도메인 설계 및 구현 — 다음 단위 작업으로 분리 필요(사람 확인 후 시작).
4. Admin 도메인(사진검토/작명/카드발급/CardDesign 배정) — 아직 전혀 손 안 댐, `docs/specs/application/api.md`도 "이번 범위 아님"으로 명시.
5. `backend-api` 재병합 여부 사람에게 확인.

## ❓ 확인 필요 (사람에게 질문 대기 중 / Codex 문서 반영 필요)

구현 중 사람에게 직접 확인해서 "결정"까지 끝난 항목(재질문 불필요, **docs/specs/application/\* 문서에만 아직 반영 안 됨**):

- `member.englishName`이 API 1(개인 신청) request/검증/매핑 표에 빠져 있었음(API 2엔 있음, requirements/data-model엔 필수로 명시) → **추가하기로 확정**, 구현엔 반영됨. `api.md`의 API 1 섹션에 반영 필요.
- `Application.total_price`(NOT NULL로 명시되어 있었음) → 상담 확정 금액을 등록하는 API 자체가 아직 없어서 **이번 구현 범위에서 컬럼 자체를 만들지 않음**. `data-model.md` 2.1절의 `total_price` 행에 "결제/상담 도메인 설계 시 추가 예정"이라는 각주 필요, `requirements.md` 10절 TBD에도 반영 필요.
- 단체 신청 엑셀 일부 행 파싱 실패 시 처리 → 옛 "30% 룰"은 가져오지 않고 **한 행이라도 실패하면 전체 거부(`EXCEL_PARSE_ERROR`)**로 확정. `requirements.md` 9절의 해당 TBD, `docs/specs/application/api.md` API 2 Validation 표에 명시 필요.

아직 사람 확인 없이 구현하며 정한 세부사항(정책 결정은 아니고 구현 디테일이지만, 명세에 없어서 임의로 채운 부분이라 검토 필요):

- **단체신청 ZIP 내부 레이아웃을 구체적으로 정의함**: 루트에 `.xlsx` 1개 + `photos/{ID}.{ext}` 사진들(대소문자·확장자 무시 매칭), 엑셀은 1행에 "공통 입국날짜" 라벨/값, 3행 헤더, 4행부터 데이터, 컬럼 순서 고정(ID·영문명·생년월일·국적·출생시간·출생지역·성별·개별입국날짜·이메일·전화번호·주소·[학번·학과]). `requirements.md`는 ASCII 목업만 제공하고 정확한 셀 좌표가 없었음 — **향후 `GET /api/applications/bulk/template` 구현 시 이 레이아웃과 반드시 일치해야 함**. `api.md`나 별도 템플릿 문서에 이 레이아웃을 명문화 추천.
- **단체 신청의 `logo`/`seal`은 학생증 여부와 무관하게 항상 필수**로 구현(일반 카드=회사로고/직인, 학생증=학교로고/직인 — `api.md` 본문 설명은 있었으나 Validation 표엔 학생증 케이스만 명시돼 있었음). `api.md` API 2 Validation 표에 "logo/seal 자체 누락" 케이스 추가 추천.
- `Applicant`의 `postal_code`/`address1`/`address2`는 API 1/2 어느 요청에도 입력 필드가 없어서 **항상 NULL로 둠** — `data-model.md` 2.2절의 "수령인 주소를 복사하는 등" 문구는 구현하지 않음(정책이 아니라 예시로만 적혀 있었음).
- **API 5 카드 다운로드(단체) ZIP은 요청마다 새로 생성**(캐싱 안 함) — api.md가 "매번 새로 묶는지, 발급 시 미리 만들어 캐싱하는지는 구현 세부사항"이라고 명시적으로 열어뒀던 부분.

Codex가 이전에 남긴 질문(아직 답변 대기, Application 구현 범위와는 직접 관련 없음):

- 상담 확정 금액을 시스템에 등록하는 주체와 API (위 total_price 항목과 동일 맥락)
- 입금 기한의 신청일 포함 여부와 마감 시각
- `NAME_EDITING` 이후 환불 정책

## 참고

- 관련 TODO 항목: "Application 도메인 엔티티/API 구현", "Codex: HANDOFF.md 확인 필요 3건을 docs/specs/application/*에 반영"
- 관련 CHANGELOG 항목: 2026-08-01 Claude — `feature/application-domain-impl`
