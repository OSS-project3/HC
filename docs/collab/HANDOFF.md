# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-09-25
- 작성자: Claude
- 브랜치: main
- 커밋·push: `87e44c0`까지 push 완료·배포 확인됨(단체 사진 반려 재업로드 유니크 제약 버그 수정까지). 아래 "완료 — 관리자 강제 취소(백엔드)" 절은 이 문서 갱신 시점 기준 **아직 커밋 전**.

## 현재 워킹 트리

**미커밋 상태** — 아래 "완료 — 관리자 강제 취소(백엔드)" 절의 소스·테스트·`docs/collab/TODO.md`/`CHANGELOG.md` 변경이 아직 커밋되지 않았다. 전체 백엔드 회귀 1045개 중 1044개 통과(무관 플레이키 1건 제외) 확인 완료. 다음 세션은 커밋부터 시작하면 된다.

## 완료 — 관리자 강제 취소 — 백엔드 (2026-09-25, Frontend·docs/specs는 사용자가 명시적으로 범위 제외)

- Codex가 `docs/collab/TODO.md`에 미리 정리해둔 "관리자 강제 취소 구현 체크리스트"(2026-09-25 확정 정책)를 사용자가 "확인해서 백엔드만 구현해줘"로 요청 → 기존 자산(`cancelByUser`/`completeCancellation` 공유 로직, `CancellationType.ADMIN`/`CancellationReason.ADMIN_DECISION` enum, `@Version`)을 실제 코드와 대조 검증한 뒤 구현.
- **구현 중 스스로 발견한 중요한 갭**: `ApplicationStatus.canTransitionTo()`가 `NAME_EDITING`/`PRODUCTION_READY`/`PRODUCING`에서 `CANCELLED`로의 전이 자체를 막고 있었다 — 정책이 요구하는 6개 허용 상태 중 3개가 상태머신 레벨에서부터 거절당했을 것. 이 세 상태에 `CANCELLED` 이탈 경로를 추가해 해결(다른 정상 전이는 불변).
- `Application.cancelByAdmin(cancelledAt, cancellationMemo)` 신설 — 허용 상태(`canTransitionTo` 재사용)·메모 trim 후 1~500자·`CANCELLED` 재호출 멱등을 Entity가 보장. `ApplicationService.cancelByAdmin()`은 `findApplicationForUpdate`(비관적 락)로 카드생성/다른 상태전이/입금확인과의 경쟁을 차단하고, 최초 취소에만 파일정리·슬롯반환·S3 예약삭제·감사로그를 처리. 기존 사용자 취소가 쓰는 `clearCancellationFileReferences`를 확장해 Member 카드 이미지까지 정리하도록 했다(관리자 취소는 `PRODUCTION_READY`/`PRODUCING`에서도 허용돼 카드가 이미 있을 수 있음 — 사용자 취소 경로에서는 항상 no-op이라 회귀 없음).
- `POST /api/admin/applications/{id}/cancel` 신규, 전용 응답 `AdminApplicationCancelResponse`(상태·결제상태·환불필요 안내·취소유형/사유/메모·최초처리여부), `MyApplicationDetailResponse`(관리자 상세·마이페이지 상세 공용)에 `cancellationMemo` 필드 추가.
- 신규 테스트 21건(Entity 6 + Service 통합 9 + Controller 6) 전부 GREEN. 전체 회귀 1045개 중 1044개 통과(나머지 1건은 무관 플레이키 `HighSchoolSeederIntegrationTest`). `cancellationMemo` nullable 컬럼이 실제 populated dev DB에 `ddl-auto=update`로 안전하게 적용되는지 재빌드해 직접 확인(2026-09-21에 겪었던 NOT NULL 컬럼 populated-table 실패와는 다른 케이스임을 확인). 재빌드된 dev 컨테이너에 실제 HTTP 호출(Playwright `fetch`)로 최초 취소·멱등 재호출까지 라이브로 재확인.
- 상세는 `docs/collab/CHANGELOG.md` 2026-09-25 "관리자 강제 취소 — 백엔드" 항목, 체크리스트는 `docs/collab/TODO.md` "관리자 강제 취소 구현 체크리스트" 절 참고.
- **다음에 할 일**: 위 변경(소스+테스트 한 커밋 + 문서 한 커밋, 이 세션 관례대로 분리)을 커밋(아직 안 함). 사용자가 명시적으로 "백엔드만"이라고 범위를 좁혔으므로 프론트(`ApplicationDetail.tsx` 취소 버튼·확인 모달·경고 문구)와 `docs/specs/*.md`/`docs/api/*.md` 갱신은 별도 확인 없이 착수하지 말 것.

## 완료 — 추천 이름 데이터와 백엔드 이름 검증 정합화 (2026-09-24, 범위 축소판 구현 완료, 커밋·push 완료)

- Codex가 미리 `TODO.md`에 써둔 정책(기존 DB의 손상 행까지 멱등 보정하는 계획 포함)을 사용자가 "확인해주세요"로 검토 요청 → 실제 코드 대조로 문서의 모든 수치·주장(700건 중 문제 20건, 두 JSON 파일 SHA-256 동일, `SajuNameSeeder`가 `count()>0`이면 스킵 등)이 정확함을 확인해 보고. 사용자가 더 단순한 대안(검증 자체를 1~4글자로 완화)을 제안했으나, `admin-saju.md`의 "전체 한글 이름 최대 5글자"(카드 레이아웃 제약)와 `CardImageCompositor`에 오버플로우 실패 처리가 실제로는 구현돼 있지 않다는 점을 근거로 반박 → 사용자가 최종적으로 범위를 명시적으로 축소해 확정: `validateNameFormat()` 2~3자 규칙 유지, **기존 DB 보정·`SajuNameSeeder` idempotent 보정은 이번 범위에서 제외**, 추천 필터는 길이 조건만, `태산`/`현산`은 필터가 아니라 JSON 데이터 자체를 직접 수정, 카드 렌더링·이름 길이 정책은 불변.
- 구현: `frontend/src/data/sajuNames.json` + `backend/.../seed/saju-names.json`에 동일한 Node 치환으로 `태산`(`兌示산`→`兌祘`)·`현산`(`鉉示산`→`鉉祘`) hanja·reading만 수정(SHA-256 재일치 확인). `namingRecommendations.ts`의 `recommendNames()`에 길이 기반(`isRecommendable`, 2~3 코드포인트) 필터를 점수 계산 전에 추가, 사전 index는 필터 전 원본 위치로 캡처해 §1.19 결정성(동점 tie-break) 불변. 백엔드 소스 코드는 전혀 안 건드림.
- **백엔드 DB 보정(Task 1)은 이번 세션에서 의도적으로 미착수** — 사용자가 명시적으로 범위에서 뺀 것. 기존 운영 DB의 `태산`/`현산` 두 행은 이번 변경으로 자동 교정되지 않는다(신규 DB는 수정된 JSON으로 정상 시드됨). 나중에 필요해지면 `docs/collab/TODO.md`의 "Task 1 — ⛔ 이번 범위에서 제외" 목록을 그대로 재사용할 수 있다.
- 상세는 `docs/collab/CHANGELOG.md` 2026-09-24 "추천 이름 데이터 정합화..." 항목, 체크리스트는 `docs/collab/TODO.md` 동일 절 참고.
- **다음에 할 일**: 위 변경(데이터+로직+테스트+문서)을 커밋(아직 안 함). 커밋 성격이 섞여있지 않게 데이터/로직/테스트 한 커밋 + 문서 한 커밋으로 나누는 이 세션의 관례를 따를 것.

## 완료 — 관리자 상태 전이 — 드롭다운 제거, 업무 버튼 + 자동 전이 (2026-09-24 정책 확정, 백엔드+프론트 구현 완료, push 완료)

- 사용자가 "제작신청 관리"의 범용 "상태 변경" `<select>`를 없애고 업무 버튼만 남기되, 버튼을 누르면 백엔드가 알아서 다음 상태로 전이되게 해달라고 요청. 여러 차례 요구사항을 구체화한 끝에 확정된 정책: **버튼(이름 확정/카드 생성)의 위치·동작은 그대로 두고**, 그 버튼 클릭이 "마지막 멤버"의 완료였을 때만 부수효과로 자동 전이한다 — 마지막 멤버 이름 확정 시 `NAME_EDITING→PRODUCTION_READY`, 마지막 멤버 카드 생성 시 `PRODUCTION_READY→PRODUCING`. 기존 검증 로직(`completeNaming()`/`requireCardGenerationComplete()`)은 그대로 재사용, 기존 수동 엔드포인트는 삭제하지 않고 복구 경로로 유지.
- 구현 중 자체적으로 발견해 사용자 확인을 받은 설계 위험: 마지막 멤버 이름 확정 즉시 `PRODUCTION_READY`로 잠기면 역방향 전이가 없어 이름을 다시 고칠 방법이 없어지는 회귀였음 → 사용자가 "PRODUCTION_READY에서도 이름 재확정 허용(추천)"을 선택해, 해당 멤버의 카드가 아직 생성되지 않았다면 `PRODUCTION_READY`에서도 이름 재확정을 허용하도록 게이트를 relax했다(카드가 이미 생성된 뒤에는 여전히 거절).
- 백엔드 구현·테스트 전부 완료·GREEN(타겟 테스트 + 전체 회귀 1024개 중 1023 통과, 나머지 1건은 무관 플레이키) — 소스+테스트 커밋 `6fa5a5d`, 문서 커밋 `0c9374d`.
- 프론트는 사용자가 "정해야할 정책없이 바로 코드 작성가능하면 작성해줘. 모르는 부분이 있으면 구현하지말고 물어봐줘"로 착수를 승인해 이어서 구현. `<select>` 제거 → 남는 6개 업무 액션 독립 버튼화, "작명완료 처리"/"제작 시작" 버튼 목록 제거, 전체 파이프라인 토글형 읽기전용 상태 표시기 신규(주 경로 6단계 + 이탈 경로 2개, 백엔드 `ApplicationStatus.canTransitionTo` 그래프 그대로 반영) — 여기까지는 정책 질문 없이 바로 구현. 유일한 미정 사항(자동 전이 시 별도 알림 여부)만 `AskUserQuestion`으로 확인 → **"기존 저장 토스트만 유지(추가 없음)"** 확정, 추가 구현 불필요(상태 배지·파이프라인은 기존 `onSaved`→`reloadDetail()`로 이미 자동 갱신됨).
- 상세는 `docs/collab/CHANGELOG.md` 2026-09-24 "관리자 상태 전이... 백엔드"/"...프론트엔드" 두 항목, 체크리스트는 `docs/collab/TODO.md` 동일 절(Backend/Frontend 전부 ✅) 참고.
- 프론트 커밋 `de2f3f9`, 문서 커밋 `68210f6` — 전부 `main`에 push 완료, 배포 워크플로 성공 확인.
- **미확답으로 남은 사소한 결정**: 검토시작/작명승인/배송발송을 프론트에서 계속 독립 버튼으로 유지할지 사용자가 명시적으로 답한 적은 없음 — 이번 프론트 구현에서 "별다른 이견 없어 그대로 유지" 가정대로 독립 버튼으로 남겨뒀다(실제 적용됨, 문제 제기 없으면 그대로 확정으로 간주해도 됨).

## 완료 — 관리자 카드 개별 다운로드 — 새 탭 대신 파일로 저장 (2026-09-24 정책 확정·구현 완료)

- 사용자가 "다운로드 버튼 클릭 시 새 탭 대신 파일로 저장되게 해달라"고 요청 → 코드 조사만 먼저 수행(수정 없음, 현재 응답 구조 보고 — 핵심 발견: 이 엔드포인트는 presigned S3 URL만 반환하고 실제 파일 전송은 브라우저가 S3에 직접 요청해서 일어나므로 우리 컨트롤러 응답에 헤더를 붙이는 접근은 애초에 안 통함) → "조사한 최소 구현안대로 진행해줘"로 파일명 정책까지 확정받고 구현 완료(커밋 `98f3664` 백엔드, `ee2f480` 프론트). **구현 중 실제 버그 1건 발견·수정**: `<a>` 클릭 방식으로 앞·뒤 2개 파일을 연달아 다운로드하면 두 번째 네비게이션이 첫 번째를 취소해버려 앞면이 계속 누락되는 걸 실제 MinIO로 Playwright 검증하다 발견 — 숨긴 iframe 방식으로 교체해 해결. 상세는 `docs/collab/CHANGELOG.md` 2026-09-24 항목 참고.

## 완료 — 작명 업무 진행중/완료/캔슬 조회 (2026-09-24 정책 확정, 백엔드+프론트 구현 완료)

- 사용자가 관리자 화면에 작명 진행중/완료 구분 조회를 요청 → 코드 조사만 먼저 수행(수정 없음, 현재 구조·갭 보고) → 대화로 정책 확정(개인=`Application.status`, 단체=멤버 전원 카드생성완료 여부로 status와 무관하게 판정, 자동 상태전이 없음, 진행중/완료/캔슬 3분류) → "백엔드먼저 구현좀" 지시로 백엔드 완료(커밋 `981a29d`) → "더 정해야할 정책없으면 진행해도돼" 승인으로 프론트도 완료(커밋 `dda3762`). 상세는 `docs/collab/CHANGELOG.md` 2026-09-24 두 항목(백엔드/프론트엔드), API 계약은 `docs/api/admin.md` "작명 업무 진행중/완료/캔슬 조회" 절 참고.
- 프론트: `ApplicationsSection.tsx`에 서버 파라미터(`namingProgress`) 기반 진행중/완료/캔슬 필터 추가(기존 개인/단체 탭의 클라이언트 필터 방식은 이미 부정확하다고 조사에서 확인돼 있어 답습하지 않음), 단체 신청 행에 "작명 진행률"(`완료수/전체수`) 컬럼 추가. Playwright로 실제 dev 컨테이너에서 필터 전환·진행률 표시까지 확인 완료.

## 완료 — 십이간지·카드 디자인 선택 이미지 미리보기 (2026-09-22 Codex 확정 정책, 2026-09-23 구현 완료)

- **백엔드**(커밋 `3336b6f`) + **프론트엔드**(커밋 `a3d588f`→`24ce146`→`d3731be`) 모두 완료. 상세는 `docs/collab/CHANGELOG.md` 2026-09-23 두 항목(백엔드/프론트엔드) 참고 — 특히 프론트 항목에 실제 라이브 테스트로 찾아 고친 버그 3건(자기 취소 fetch, CSS overflow 클리핑, scroll이 팝업을 닫아버리는 버그+모바일 높이 오버플로)이 자세히 기록돼 있다.
- **미완료로 남은 것 1건**: 모바일 터치 자동 검증 — Playwright `.tap()`/`touchscreen.tap()` 둘 다 이 환경(headless Chromium)에서 `position:fixed` 포탈 패널과의 상호작용에 원인 불명의 한계가 있어 끝내 성공시키지 못했다. 코드는 표준 `<button onClick>`이라 터치→클릭 변환을 막는 로직이 전혀 없어 실기기에서는 동작할 것으로 판단하지만, 확인은 못 했다 — 사용자가 실기기(또는 Chrome DevTools 모바일 에뮬레이션)로 한 번 확인해볼 것을 권장.

## 이번 세션(2026-09-17~09-22) 완료 — 상세는 `docs/collab/CHANGELOG.md` 해당 날짜 항목 참고

1. `/api/users/me` 응답에 `role` 추가 (2026-09-17)
2. 작명 결과 Excel "뜻" 컬럼 파싱 + 관리자 명단 엑셀 내보내기 뜻 컬럼 추가, `NAME_EDITING` 상태 편집 잠금 (2026-09-17)
3. 단체/개인 신청 사진 **내용** 검증 누락 버그 수정, 학생증 앞·뒤 텍스트 색상 기능 **백엔드 전체 구현** (2026-09-19)
4. QA 체크리스트(상태 전이·중간 저장 검증) 14건 전부 완료 — 실제 버그 6건 수정(Review 이미지 UNIQUE 위반, 카드발급/배송 감사로그 중복, 학교템플릿·카드생성 커밋경계 오삭제 2건, CardType/CardDesign 시더 부분복구), 확정정책 충돌 구현공백 2건(단체 ZIP 업로드 한도, Excel 헤더 계약), 나머지 6건은 버그 없이 테스트만 보강. 상세: `docs/collab/qa_state_persistence_checklist.md` (2026-09-20)
5. 운영용 임시 관리자 자동 시드(`DemoDataSeeder.ensureAdminUser()`, `admin@test.com`) 제거 — 확정 정책: 운영 관리자는 일반 가입 후 운영자가 DB에서 `role` 수동 변경, 승격 API/UI는 미구현. 상세: `docs/TEMP_ADMIN_LOGIN.md` (2026-09-20)
6. 학생증 텍스트 색상 프론트엔드 갭 문서화(`FRONTEND_API_GAPS.md` P2 신규, `FRONTEND_API_INTEGRATION_SPEC.md` 계약 기록) + 실제 UI 구현(카드 제작 패널에 STUDENT 전용 앞/뒤 글씨색 select, 확정 후 잠금) (2026-09-20)
7. 십이간지 디자인 세트 선택 UI 구현 — 기존 P0 하드 블로커(모든 카드종류의 카드 생성을 막던 미착수 갭)를 신청 상세 레벨(select 1~5, 텍스트만·미리보기 이미지 없음)에 추가해 해소. `FRONTEND_API_GAPS.md`/`docs/collab/TODO.md`의 완료 반영 누락(6번 항목의 학생증 색상 포함)도 같이 정리 (2026-09-20)
8. 결제 안내·72시간 미입금 자동취소 정책 폐기를 문서에 반영(코드는 그대로) — `guidePayment()` Service는 있는데 이걸 호출하는 Controller가 없다는 지적에서 시작, 확인해보니 이미 폐기하기로 한 정책인데 requirements.md/data-model.md/api.md(2곳)/admin.md/TODO.md §5-A 전부 여전히 "구현 예정"으로 남아있었음. 확정 정책: 결제 안내는 시스템 밖에서 처리, 관리자는 confirm-payment만 호출, 자동 취소 없음. `guidePayment()`/`paymentGuidedAt`/`paymentDueAt`/`ApplicationPaymentTimeoutScheduler`는 2026-09-13 `refundedAt` 처리와 동일하게 코드는 그대로 두고 문서만 정정(사용자 확인 완료) (2026-09-20)
9. 카드 제작 설정 복원 — 백엔드가 이미 내려주던 확정 `cardDesignId`/`cardIssueDate`를 프론트 타입이 무시해 화면 재진입 시 항상 기본 디자인·오늘 날짜로 리셋되던 문제 수정. 학생증 텍스트 색상과 동일한 confirmed-value 잠금·복원 패턴 적용(정책 질문 없이 바로 구현, 사전 갭 문서 등록도 없었음). 커밋 `b611676` (2026-09-20)
10. 신청 건별 동의 이력 — **완료(백엔드+프론트)**. 1단계로 백엔드 저장 계약만 먼저 추가(사용자 요청: "백엔드 먼저") — `Application.consultationConfirmed`/`disclaimerConfirmed`/`consentPolicyVersion`, 개인·단체 생성 DTO에 같은 필드(커밋 `6cbeafb`, 부수적으로 Jackson이 `ApplicationCreateRequest`의 미사용 `@AllArgsConstructor`를 암묵적 creator로 채택해 새 boolean 필드 때문에 기존 테스트 4건이 깨지는 걸 발견해 제거). 2단계로 `ApplyPage.tsx`가 체크박스 값을 실제로 전송하도록 연결하고(커밋 `b86c5a7`), 백엔드에 `@AssertTrue` 검증을 활성화해 둘 다 true가 아니면 거절하도록 마무리(커밋 `bfa776e` — 성공 경로를 검증하던 기존 테스트 7건이 동의 필드 누락으로 새로 실패해 픽스처에 추가). `FRONTEND_API_GAPS.md` P1에서 제거 (2026-09-20~21)
11. 공개 카드 조회 → 다운로드 — 단기 토큰 기반 재설계 완료(백엔드+프론트). 비로그인 조회 화면이 로그인 전용 다운로드 API를 호출해 항상 401이었고, 프론트가 그 실패를 데모 카드로 조용히 대체해 실제로 작동한 적이 없던 기능이었음(외부 감사표에서 P0로 지적). 단순 `permitAll()`은 `applicationId`만 바꿔 타인의 카드를 내려받는 IDOR을 만들어 대신 `CardLookupTokenService`(신규, Redis 기반 1회용 단기 토큰, `VerificationChallengeStore`의 signupToken 패턴 재사용·새 시크릿 없음)로 조회 성공 사실 자체를 인가 근거로 삼는 신규 공개 엔드포인트(`GET .../cards/download/public`)를 추가했다. 기존 로그인 기반 엔드포인트는 그대로 두고 공유 로직만 추출. 프론트는 데모 카드 fallback을 제거하고 실제 실패 메시지로 교체(사용자 확인 완료). 커밋 `4ea6801`(백엔드) / `9b8da0e`(프론트) / `688c3fa`(체크리스트) (2026-09-20)
12. **실제 dev 컨테이너(기존 데이터가 쌓인 populated DB) 재빌드 검증 중 배포 위험 버그 발견·수정**. 사용자가 "실제 브라우저 클릭 테스트 해보면 안 됨?"이라고 물어 dev 스택을 오늘 코드로 재빌드했더니, 이미 행이 있는 `applications` 테이블에 DEFAULT 없는 `NOT NULL` boolean 컬럼(`consultation_confirmed`/`disclaimer_confirmed`)을 추가하려다 Postgres가 거부했고, Hibernate `ddl-auto=update`는 이 실패를 **조용히 무시**하고 앱을 계속 띄웠다 — 컬럼 자체가 안 생겨서 이후 모든 Application 읽기/쓰기가 "column does not exist"로 깨지는 상태였다(순수 유닛 테스트는 항상 빈 스키마에서 시작하므로 이 클래스의 버그를 원천적으로 못 잡는다). `@ColumnDefault("false")` 추가로 수정하고 같은 populated DB에 재적용해 컬럼이 정상 생성됨을 확인(커밋 `c68cc2f`). 이어서 실제 chromium-cli/Playwright는 이 환경에 없어 브라우저 클릭 자체는 못 했지만, 살아있는 컨테이너에 `Invoke-RestMethod`로 프론트와 동일한 실제 요청을 직접 보내 조회→토큰발급→다운로드(CARD_NOT_READY)→토큰 재사용 거절(INVALID_LOOKUP_TOKEN)까지 전 구간을 실제 스택으로 확인 (2026-09-21)
13. **소셜 로그인 시 관리자 메뉴가 안 뜨는 버그 발견·수정**. 사용자가 로컬 dev DB에 만든 관리자 계정, 이어서 운영 DB에서 본인 계정을 `role='ADMIN'`으로 승격 후 로그인했는데도 관리자 메뉴가 안 뜬다고 지적. 원인: `AuthContext.refreshProfile()`이 백엔드가 2026-09-17부터 내려주던 `/api/users/me`의 실제 `role`을 무시하고, 로그인 시점에만 기록되는 localStorage 힌트(`auth-role`)로 role을 복원해왔음 — 비밀번호 로그인은 로그인 성공 시 명시적으로 `login()`을 호출해 문제가 안 드러났지만, **소셜 로그인(OAuth)은 백엔드가 쿠키만 심고 바로 리다이렉트할 뿐 프론트 어디서도 `login()`을 안 불러서** `refreshProfile()`의 힌트 폴백만 타고, 이 계정으로 첫 로그인이면 힌트가 없어 관리자도 항상 "user"로 떨어짐. `FRONTEND_API_GAPS.md` P1 "`/api/users/me` 역할 복원" 갭이 실제로 이 버그였음(문서엔 "백엔드에 role이 없다"고 적혀 있었는데 이미 2026-09-17에 추가돼 있었고, 프론트가 안 쓰고 있었을 뿐). `refreshProfile()`이 `profile.role`을 그대로 쓰도록 고치고 이제 불필요한 힌트 메커니즘(`ROLE_HINT_KEY`/`readRoleHint`/`writeRoleHint`) 제거. 커밋 `22a8a30` (2026-09-21)
14. **카드 미리보기 자동 갱신 구현 — Codex가 정책·체크리스트 작성, Claude가 구현**. Codex가 `docs/collab/TODO.md` 최상단(그때 기준 라인 12)에 "카드 미리보기 자동 갱신" 확정 정책·Backend/Frontend/완료검증 체크리스트를 직접 워킹 트리에 커밋 없이 추가해뒀고, 사용자가 이를 진행하라고 지시. 핵심 정책: ① 저장 완료된 값만 반영, ② `NAME_EDITING`에서도 미리보기 허용(기존엔 `PRODUCTION_READY`부터만). 백엔드는 `CardPreviewService`의 상태 게이트를 확장(커밋 `39bed2e`), 프론트는 `CardProductionPanel`에 800ms debounce 자동 갱신 effect 추가 — 순번 가드로 stale 응답 무시, 마운트 시 스킵으로 전체 구성원 일괄 호출 방지, `PRODUCING` 이후엔 생성된 이미지로 전환(커밋 `1cfb666`). `docs/collab/TODO.md` 체크리스트 전 항목 완료 반영(커밋 `c01f2fd`). **다른 세션(Codex)이 같은 워킹 트리에 직접 파일을 써두고 커밋은 안 한 상태였던 사례** — 내가 그 다음 편집(진행중 상태 표시)을 하면서 그 내용까지 같이 커밋하게 됐음(정상, 공유 워킹 트리 관례상 문제없음). 커밋 `39bed2e`/`1cfb666`/`c01f2fd` (2026-09-22)
15. **카드 미리보기 자동 갱신 — 실사용(Playwright) 검증**. 사용자가 "test이번에 어떻게 진행함?"이라고 물어, 프론트 런타임 동작을 실제로 클릭해본 적이 없었다는 걸 정직하게 밝히고 라이브 브라우저 테스트를 제안·승인받음. 이 환경에 Chromium·`@playwright/test`가 이미 설치돼 있는 걸 확인하고 단일 멤버 신청(`APP-2026-000010`)으로 debounce·순번가드·실제 렌더링 결과를 스크린샷까지 확인. 이어서 사용자가 "단체 신청으로 확인도 해야해"(1명만 호출되는지, 즉 멤버 간 격리가 단체 신청에서도 성립하는지 단일 멤버 테스트로는 증명 안 됨)라고 지적해, 실제 4명 단체 신청(`APP-2026-900006`)에 3명은 API로 직접 "준비완료" 상태까지 세팅하고 1명은 의도적으로 미완료로 남긴 뒤, 한 멤버 패널을 조작하면 그 멤버 ID로만 scoped된 네트워크 호출 1건만 발생하고 다른 멤버 패널에는 전혀 영향이 없음을 직접 증명. `docs/collab/TODO.md`의 "완료 검증" 체크리스트 항목을 이 구체적 검증 결과로 갱신(커밋 `c4db930`). (2026-09-22)
16. **십이간지·카드 디자인 선택 이미지 미리보기 — 백엔드+프론트 구현 완료**. 위 15번 검증 커밋(`c4db930`)을 위해 `TODO.md`를 다시 열었다가, Codex가 같은 파일에 커밋 없이 직접 써둔 완전히 새로운 대형 기능 스펙("십이간지·카드 디자인 선택 이미지 미리보기", 2026-09-22 확정)을 발견 — 세션 초반 사용자가 "십이간지 캐릭터나 카드 디자인 셀렉시 토글 선택할 때 옆에 포인터를 갖다대면... 이미지가 떴으면 좋겠다"고 요청했던 바로 그 기능이었다. 사용자에게 보고 후 "구현 진행하되 백엔드부터 구현해줘" 지시를 받아 진행 보드를 먼저 claim(커밋 `e098d6b`), 백엔드 구현 완료(커밋 `3336b6f`), 사용자가 "정해야할 정책이 완전히 확정됐다면 그대로 진행"으로 프론트 착수를 승인해 이어서 프론트도 완료(커밋 `a3d588f`→`24ce146`→`d3731be`) — 프론트 구현 중 dev 컨테이너 라이브 테스트로 실제 버그 3건을 발견·수정한 게 이 작업에서 가장 값진 부분이었다. 상세는 `docs/collab/CHANGELOG.md` 2026-09-23 백엔드/프론트엔드 두 항목 참고. (2026-09-23)

## 검증

- 백엔드: 항목별 개별 회귀 + 세션 마지막 전체 회귀 실행, 마지막 실행(2026-09-24, 관리자 상태 전이 자동화 백엔드 포함)은 1024개 중 1023개 통과 — 실패 1건은 여전히 동일한 무관 플레이키 `HighSchoolSeederIntegrationTest.reseedingTheSameCsvDoesNotDuplicateRows`(스위트 전체가 공유하는 `schools` 테이블을 여러 테스트가 각자 `deleteAll()`해서 생기는 기존 문제, 이 세션이 유발한 게 아님 — 재현·원인 확인은 `qa_state_persistence_checklist.md` 10번 항목 참고).
- 테스트 실행 시 `hc-test-redis` 컨테이너(포트 6379) 필요 — 개발 `docker-compose.yml`은 Redis를 호스트에 노출하지 않는다. `docker run -d --name hc-test-redis -p 6379:6379 redis:7-alpine`(최초) 또는 `docker start hc-test-redis`(이후). **주의**: 이 컨테이너는 docker-compose 관리 밖이라 Docker Desktop 재시작 시 자동으로 안 살아난다 — 재시작 직후 테스트가 전부 `RedisConnectionException`으로 실패하면 이것부터 의심할 것(이번 세션에 실제로 겪음, 코드 문제 아니었음).
- 프론트: `tsc --noEmit` strict, `npm run build` 둘 다 통과(학생증 색상·십이간지 선택 UI·공개 카드 다운로드 포함).
- **실제 dev 컨테이너 검증**: 2026-09-21에 처음으로 `docker compose up -d --build`로 이 세션의 실제 코드를 기존 populated DB에 반영해 검증(위 12번 참고) — 이 과정에서 순수 유닛 테스트로는 못 잡는 배포급 버그(populated 테이블에 DEFAULT 없는 NOT NULL 컬럼 추가 실패)를 발견·수정했다. 실제 브라우저 클릭 자동화(chromium-cli/Playwright)는 이 환경에 없어 여전히 못 하지만, 살아있는 컨테이너에 직접 HTTP 요청을 보내는 방식으로 공개 조회→다운로드 토큰 흐름 전 구간을 확인했다. **다음에 이런 요청이 오면**: `docker compose up -d --build`로 재빌드 후 `Invoke-RestMethod`(PowerShell)로 실제 엔드포인트를 직접 호출하는 방식이 이 환경에서 유효했다 — Bash 도구의 셸이 이번 세션 중간에 일시적으로 깨진 적이 있었는데(`git`/`head`/`tail` 등 기본 명령이 안 잡힘) PowerShell 도구는 계속 정상이었다.

## 현재 미완료·확인 필요

1. `FRONTEND_API_GAPS.md`의 남은 갭 — 직접입력 학생증 School 연결 UI(P1). `zodiacDesignSet`은 이번 세션에서 완료(위 7번).
2. `LoginPage.tsx`의 "로그인 실패 시 클라이언트 admin mock 폴백" 제거 — 백엔드 담당 범위 밖, 프론트 수정이라 별도 확인 필요.
3. `docs/api/admin.md` "8. 환불 완료 기록"과 `docs/specs/application/requirements.md`의 `refundedAt` 서술이 2026-09-13 확정 정책(시스템은 환불 완료 여부를 관리하지 않음)보다 낡음 — 코드는 문제 없어 이번엔 안 건드림, 문서 정리만 남음.
4. (기존, 미해결 이월) `docs/collab/TODO.md` 상단의 "단체 Excel에서 성씨를 받지 않는다" 정책과 현재 구현(선택적 성씨 열)이 충돌 — 정책 문서와 구현 중 하나를 정합화해야 함.
5. **작업 완료 직후 `FRONTEND_API_GAPS.md`/`docs/collab/TODO.md` 갱신을 매번 습관화할 것** — 이번 세션 중 학생증 색상 프론트 구현(커밋 `4ae03ec`)이 `FRONTEND_API_GAPS.md`에 반영 안 된 채로 남아있던 걸 십이간지 작업 도중에야 발견해 뒤늦게 정리함(커밋 `a68f713`).
6. **십이간지·카드 디자인 선택 이미지 미리보기 — 모바일 touch 실기기 QA 필요**. 백엔드·프론트 모두 완료(위 16번)했지만, 새 custom listbox 2종(`ZodiacDesignSelector`/`CardDesignSelector`)의 모바일 터치 상호작용은 이 환경(headless Chromium + Playwright)에서 자동 검증을 끝내 성공시키지 못했다 — `position:fixed` 포탈 패널에 tap 이벤트 자체가 안 닿는 원인 불명의 한계. 코드는 표준 `<button onClick>` 시맨틱이라 실기기에서는 동작할 것으로 판단하지만 확인은 안 됐다. 다음에 시간이 나면 실기기 또는 Chrome DevTools 모바일 에뮬레이션(Playwright 헤드리스가 아닌 실제 브라우저)으로 한 번 확인할 것.

현재 상태의 단일 소스는 `docs/FRONTEND_API_GAPS.md`(프론트 갭), `docs/FRONTEND_API_INTEGRATION_SPEC.md`(프론트 연동 방식), `docs/collab/CHANGELOG.md`(전체 변경 이력), `docs/collab/qa_state_persistence_checklist.md`(이번 세션 QA 상세)다.
