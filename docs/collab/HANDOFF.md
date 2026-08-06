# HANDOFF — 현재 작업 상태

> ⚠️ 이 문서는 누적 기록이 아니라 **"지금 시점" 스냅샷 1개**다. 작업을 종료할 때 아래 내용을 전부 덮어쓴다.
> 과거 기록이 필요하면 `CHANGELOG.md`를 본다.

- 마지막 갱신: 2026-08-06
- 작성자: Claude
- 작성 브랜치: `main` (⚠️ 브랜치 구조가 바뀌었다 — 아래 참고)

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

**⚠️ 브랜치 구조 통합 완료 — 이제 `main` 하나만 개발한다.** (`docs/collab/RULES.md` §1도 갱신됨)

- 그동안 `backend-api`/`feature/application-domain-impl`/`feature/application-domain-docs` 세 브랜치로 나눠 작업해왔는데, `main`에 있던 백엔드 사본이 원래 다른 분이 `e3ab484` 근방을 **git 히스토리 연결 없이 단순 파일 복사**로 넣어둔 것이었다는 걸 발견. 사용자 지시로 `backend-api`(감사/정리/E2E테스트) → `main` 병합(충돌 없음) → `feature/application-domain-impl`(Codex Task 3/5, 오늘 작업) → `main` 병합(`ApplicationService.java`/`CHANGELOG.md`/`TODO.md` 3개 충돌, 수동 해결) 순서로 실제 `git merge` 진행. 병합 중 `ErrorCode`가 자동으로 잘못 합쳐져서(양쪽의 "삭제 vs 무변경"을 삭제로 처리) `ApplicationPhotoValidator`가 쓰는 `UNSUPPORTED_FILE_TYPE`/`INVALID_IMAGE`가 빠져 컴파일 깨졌던 것 발견·복구.
- 검증: 컴파일 통과, 테스트 104개 중 101개 통과(실패 3건은 전부 로컬 Redis 미기동 때문 — 기존부터 알려진 환경 문제, 회귀 아님).
- 옛 문서 스캐폴딩(`backend/honor-citizen/docs/{agent,api/API_SPEC.md,architecture,db,domain}/*`) 삭제 — `docs/collab/`·`docs/specs/application/`으로 이미 대체된 것들.
- `backend-api`/`feature/application-domain-impl` 브랜치 자체는 삭제하지 않고 남겨둠(사용자 지시).

**오늘 Application 도메인 변경 (2026-08-06 UI/API 갭 분석 결과 반영):**

- `ApplicationService.lookup()` 인증 정책을 method별로 분리 — `method=application`(신청번호 조회)은 phone·email **둘 다** 필수+둘 다 일치, `method=card`(카드번호 조회)는 인증값 검증 자체를 제거(카드번호 단독 조회). 기존엔 method 무관하게 "phone/email 중 1개"였음.
- `CardTypeSeeder`(`CommandLineRunner`) 신규 — 최초 기동 시 `HONOR_KOREAN=1, HONOR_CITIZEN=2, VISITOR=3, STUDENT=4` 순서로 시딩. 프론트가 `cardTypeId`를 1~4로 하드코딩해서 쓰는 걸 그대로 허용하기 위함 — `GET /api/card-types` 신규 API는 만들지 않기로 결정.
- 그 외 UI/API 갭 분석에서 나온 결정들(단체 파일은 `logo`/`seal`/`submitFile` 3파트 유지, 단체 재제출은 이미 백엔드 구현 완료, 사주정보 4종 전부 필수, `englishName` 언어무관 필드, `organizationName` 입력 UI 부재, `StepFiles.tsx` 분기 오류 등)은 코드 변경 없이 `backend/FRONTEND_API_REQUIREMENTS.md`(main 루트)에 상세 기록 — 프론트 담당자가 참고할 문서.

**Review(후기) 도메인 신규 설계 완료 — 구현은 아직 안 함:**

- 요구사항 변경(제목/신청유형/카드종류 체크박스/작성자명 수동입력/사진 0~N장 다중첨부/내용)에 따라 `docs/specs/review/{data-model,api}.md` 신규 작성.
- Entity 3개: `Review`(작성자 실계정 `user_id`와 화면표시 `author_display_name` 분리), `ReviewCardType`(`@ElementCollection`, `CardTypeCode` 다중선택), `ReviewImage`(`UploadFile` 재사용 + `review_id`/`upload_file_id`/`display_order` join — 판단 근거는 data-model.md §5).
- API 3개 설계: 등록(`POST /api/reviews`, multipart)/목록조회(`GET /api/reviews`, 페이징 — 프로젝트 첫 페이징이라 `PageResponse<T>` 공용 포맷 제안)/단건조회(`GET /api/reviews/{id}`).
- **자격 검증 정책 확정(사용자 지적으로 설계 수정)**: 처음엔 `applicationType`/`cardTypeCodes`를 자유 입력으로 설계했었는데, 사용자가 "실제 신청 이력이 있는 사람만 써야 하지 않냐"고 지적. `Application.user_id`(제출 계정) 기준으로는 단체 신청의 실제 카드 수령자(구성원 개인, 계정 연결 없음)가 제외되는 문제가 있어서, 대신 **이메일 매칭**(`Applicant.email` 또는 `ApplicationMember.email`이 로그인 계정 이메일과 일치)으로 검증하도록 수정 — `lookup` API 본인확인 방식과 동일한 사고. 신규 `REVIEW_NOT_ELIGIBLE`(403) 에러코드.
- `docs/api/upload-file.md`에 남아있던 옛 "`Review.thumbnail_file_id`(단일)" 가정 대체, `arch.md`에 Review 모듈 신설(§4.7, 기존 Board는 §4.8 Post로 분리).

## 다음에 할 일

1. Review [TBD] 4건 확인 (아래 참고) → 문서 반영 → 구현 착수 여부 결정
2. "내가 후기 쓸 수 있는 카드종류" 조회 API를 추가할지 결정 — 없으면 프론트가 체크박스 옵션을 모른 채 제출했다가 사후 거절(`REVIEW_NOT_ELIGIBLE`)만 가능
3. (예전부터 미해결, 아래 "확인 필요" 참고) `docs/specs/application/*` 문서 반영 3건 — 담당 미지정 상태로 TODO.md에 남아있음
4. Payment/상담금액/자동취소/환불, Admin 도메인(사진검토/작명/카드발급/CardDesign 배정) — 여전히 전혀 착수 안 됨
5. `backend/API_ANALYSIS.md`/`backend/arch.md`/`backend/DB.md`/`backend/PROJECT_STATUS.md`/`backend/honor-citizen/{AGENTS,CLAUDE}.md`/`backend/honor-citizen/docs/{bulk,harness,jwt,test}/*` — 브랜치 통합 중 존재를 확인했지만 내용 검토 전이라 그대로 둠. 최신/유효한 문서인지 다음에 확인 필요(`docs/test/user-test-result.md`는 TODO.md가 실제로 참조하고 있어 살아있는 문서로 확인됨)

## ❓ 확인 필요 (사람에게 질문 대기 중)

**Review 도메인 (신규, 이번 설계에서 발생):**

- 카드종류 체크박스 0개(미선택) 허용 여부 — 현재 "최소 1개 필수"로 추론 설계
- 후기 본문(`content`) 최대 글자수 — 옛 프론트 mock은 3000자였으나 이번 변경에서 재확인된 값 아님
- 조회수(`view_count`) 노출 여부 — 현재 설계엔 제외 제안(나중에 컬럼 추가만으로 확장 가능)
- 자격 검증 시 인정하는 `Application.status` 최소 조건 — `COMPLETED`(카드 실제 발급 완료)만 인정하는 걸 제안했으나 미확정
- 사진 첨부 최대 개수 — 우선 10장으로 제안만 함

**Application 도메인 (예전부터 미해결, 문서 반영 담당자 미지정 상태로 남아있음):**

- `member.englishName`이 API 1(개인 신청) `docs/specs/application/api.md` request/검증/매핑 표에 여전히 안 반영됨(구현엔 이미 있음) — 방금 재확인함, 아직도 그대로.
- `docs/api/user.md` 225행 "GET /api/users/me 미구현" — 여전히 stale(이미 구현됨, `UserController`).
- `arch.md` 3절 패키지 구조 예시 — `api/admin`·`infra/toss` 삭제 반영 안 됨, `domain/uploadfile`·`domain/log` 예시에 없음.
- (재확인 결과 정정) `Application.total_price` 미생성 건은 `requirements.md` 208행에 이미 반영되어 있어 해소된 것으로 확인. **단, 단체 엑셀 부분실패=전체거부(`EXCEL_PARSE_ERROR`) 정책은 코드엔 반영됐지만 `requirements.md` 265행이 여전히 `[TBD]`로 남아있음 — 문서 미반영 상태 그대로, 위 목록에서 빼면 안 됨(방금 실수로 뺄 뻔한 걸 재확인 중 발견)**. `docs/specs/application/requirements.md` 265행에 "전체거부로 확정" 반영 필요.

## 참고

- 관련 TODO 항목: "Review 도메인 설계", "Review 자격 검증 정책 반영", "Review 도메인 구현", "Review [TBD] 확인 필요 4건", 그 외 예전부터 있던 Codex 문서 반영 항목들
- 관련 CHANGELOG 항목: 2026-08-06 (여러 건 — 브랜치 통합, lookup 정책, CardTypeSeeder, Review 설계, Review 자격 검증)
