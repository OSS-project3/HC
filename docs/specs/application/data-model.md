# Application Data Model

> ⚠️ **정정(2026-08-25):** Application 테이블에 **`depositor_name VARCHAR(60)`(nullable)** 컬럼이 추가됐다(입금자명, 완료 화면에서 등록, 아래 §2.1 표에 미반영). 등록은 `registerDepositorName()`(결제 확인 전 SUBMITTED·WAITING만) + `PATCH /api/applications/{id}/depositor`. 그 외 필드·상태전이는 코드와 일치.

> Application 도메인의 엔티티, 컬럼, 관계 및 제약조건은 [APPLICATION.md](APPLICATION.md)의 최종 정책을 반영합니다.
> 업무 규칙은 [requirements.md](requirements.md), 외부 계약은 [api.md](api.md)를 기준으로 합니다.
> ⚠️ 2026-08-07: 단, 학생증 `ApplicationMember.department`(학과) 필드는 예외 — `APPLICATION.md`가 "제외"로 적었으나 근거가 없어 사람이 미결정으로 확인, 기존대로 유지(`PENDING_DECISIONS.md` 참고).
> ✅ 2026-08-25: 관리자 작명 확정·카드 제작 관련 정책(성씨 분리, 사진 번호, 카드번호 입력·유일성, 카드 표기 주소, 발행처/로고·직인, 띠 이미지 등)은 [admin-saju.md](admin-saju.md)를 기준으로 하며, `docs/collab/TODO.md`의 "관리자 작명 확정·카드 제작 구현 계획"에 따라 1-A~3-C 순서로 반영한다. 이 문서의 관련 항목은 admin-saju.md 확정 정책에 맞춰 갱신했다.

## 2. Application 도메인

### 2.1 Application

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 신청 ID |
| application_number | VARCHAR(20) | NOT NULL, UNIQUE | ✅ 2026-07-25 확인: 서버 생성 신청번호(예: `APP-2026-XXXXXX`). 사용자가 신청 조회·고객센터 문의 시 사용하는 식별자 |
| user_id | BIGINT | FK → User | 신청한 회원 |
| card_type_id | BIGINT | FK → CardType, NOT NULL | 카드 종류. ✅ 2026-07-31 명시: **신청 생성 API에서 사용자가 직접 선택해서 보내는 값**(4종 중 1개) — `card_design_id`가 사용자 입력에서 빠지면서, 이 컬럼이 신청 생성 요청에 실리는 유일한 "카드 관련" 식별자가 됨 |
| application_type | ENUM | NOT NULL | INDIVIDUAL, GROUP |
| status | ENUM | NOT NULL | `SUBMITTED, REVIEWING, PHOTO_REJECTED, NAME_EDITING, PRODUCTION_READY, PRODUCING, COMPLETED, CANCELLED` |
| payment_status | ENUM | NOT NULL | WAITING, CONFIRMED |
| payment_guided_at | DATETIME | NULL | 관리자가 최초 결제를 안내한 시각. 재안내 시 변경하지 않음 |
| payment_due_at | DATETIME | NULL | 최초 결제 안내 시각 + 72시간. 10분 주기 자동 취소 조회 기준 |
| cancelled_at | DATETIME | NULL | 최초 취소 완료 시각 |
| cancellation_type | ENUM | NULL | `USER, SYSTEM, ADMIN`. 이번 구현은 USER/SYSTEM만 사용하고 ADMIN은 예약값 |
| cancellation_reason | ENUM | NULL | `USER_REQUEST, PAYMENT_TIMEOUT, ADMIN_DECISION`. 이번 구현은 앞의 두 값만 사용 |
| refunded_at | DATETIME | NULL | 관리자가 외부 전액 환불을 완료한 시각. `CANCELLED + CONFIRMED`에서만 기록 |
| card_ready_at | DATETIME | NULL | 신청의 모든 카드 파일 생성 완료 시각. 모바일 다운로드 허용 기준 |
| physical_dispatched_at | DATETIME | NULL | `MOBILE_AND_PHYSICAL` 신청을 택배사에 인계한 시각 |
| version | BIGINT | NOT NULL DEFAULT 0 | 주요 상태 전이 동시성 충돌 감지용 낙관적 락 버전 |
| receiver_same_as_applicant | BOOLEAN | NOT NULL DEFAULT TRUE | 신청자와 수령인이 동일한지 여부 |
| total_quantity | INT | NOT NULL | 신청 인원 수 |
| issue_type | ENUM | NOT NULL | MOBILE, MOBILE_AND_PHYSICAL — ✅ 2026-07-25 확인: Application 테이블에 반드시 있어야 함 (누락이었음) |
| card_design_id | BIGINT | FK → CardDesign, NULL | ⚠️ 2026-07-31 재정정: **"신청 1건당 디자인 1개 선택"은 취소 — 사용자가 고르지 않음.** 신청 생성 시점엔 항상 `NULL`, **관리자가 신청 검토 과정에서 배정**(정확한 배정 시점은 Admin API 설계 시 확정, [TBD]). `시안.zip` 확인 결과 디자인이 발행 지자체별로 나뉘는 행정적 값이라 사용자가 미학적으로 고를 성격이 아님(`docs/specs/application/requirements.md` 6절) |
| logo_file_id | BIGINT | FK → UploadFile, NULL | 업로드한 로고. 학생증은 개인/단체 모두 학교 로고로 사용하고, 그 외 카드종류는 GROUP에서 사용. ✅ 2026-08-25 확인(`admin-saju.md` 기준): **"발행처"는 텍스트 문구가 아니라 이 이미지 그 자체다.** 카드에는 이 로고 이미지만 매핑하고 별도 발행처 텍스트 필드는 추가하지 않는다(학생증의 학교 로고도 동일 — `schoolName`은 이미 별도 목적으로 존재하는 텍스트 필드이며 카드 렌더링에 발행처 문구로 쓰이지 않는다) |
| seal_file_id | BIGINT | FK → UploadFile, NULL | 업로드한 직인. 일반 단체 신청에서는 필수이고 학생증 개인·단체 신청에서는 선택 |
| submit_file_id | BIGINT | FK → UploadFile, NULL | ✅ 2026-07-25 확인: 제출한 ZIP(엑셀+사진) |
| photo_reject_reason | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 관리자가 사진 반려 시 입력하는 사유. `status=PHOTO_REJECTED`일 때 사용자에게 노출(`/lookup` 조회 결과) |
| orientation | ENUM | NULL | ✅ 2026-08-14 신규 확정: 카드 가로형/세로형(`LANDSCAPE`, `PORTRAIT`). **카드종류=학생증일 때만 사용**(그 외 카드종류는 NULL), 신청서 전체에 1개(개인·단체 공통 — 단체도 신청 폼 필드이며 엑셀 컬럼 아님) |
| school_type | ENUM | NULL | ✅ 2026-08-14 신규 확정: 학교구분(`UNIVERSITY`, `HIGH_SCHOOL`). **카드종류=학생증일 때만 사용**, orientation과 동일하게 신청서 전체에 1개(개인·단체 공통). `UNIVERSITY`일 때만 `ApplicationMember.student_id`/`department`가 필수가 된다 — `HIGH_SCHOOL`이면 오히려 둘 다 NULL이어야 한다(있으면 거절). ⚠️ 2026-08-20 재정정: 이 조건은 개인·단체 공통이다 — 단체(엑셀) 신청도 `BulkExcelParser`가 `schoolType`을 받아 동일하게 검증한다(`HIGH_SCHOOL`이면 학번·학과 열에 값이 있으면 행 오류). "단체는 개인과 무관하게 항상 필수"였던 이전 서술은 `BulkExcelParser`가 `schoolType`을 몰랐던 시절의 오류였다 |
| school_name | VARCHAR(20) | NULL | ✅ 2026-08-19 신규 확정: 학교명. **카드종류=학생증일 때만 사용**, orientation/school_type과 동일하게 신청서 전체에 1개(개인·단체 공통 — 단체는 항상 한 학교 단위로 접수된다는 전제). `UNIVERSITY`/`HIGH_SCHOOL` 둘 다 필수(학번/학과와 달리 대학교 전용 조건 없음). DB는 nullable, 학생증 여부에 따른 필수 검증은 서비스 레벨에서만 강제(비학생증이면 있으면 거절). 트림 후 5~20자, 한글·영문·숫자·공백만 허용 |

> ✅ 2026-08-17 확정(Application 상태와 입금 상태 분리):
> ```
> SUBMITTED + WAITING
>    │  결제 안내: paymentGuidedAt, paymentDueAt(+72시간)
>    │  입금 확인: status 유지, payment_status만 CONFIRMED
>    ▼
> SUBMITTED + CONFIRMED
>    │  관리자 검토 시작
>    ▼
> REVIEWING + CONFIRMED
>    ├─ 반려 → PHOTO_REJECTED(사진반려) → 사용자 재업로드 → REVIEWING(복귀)
>    └─ 승인 → NAME_EDITING(작명·편집중)
>                  ▼  편집 완료
>              PRODUCTION_READY(제작 승인 대기)
>                  ▼  관리자 제작 승인
>              PRODUCING(카드 제작중)
>                  ▼  카드 파일 생성 완료: cardReadyAt 기록
>              MOBILE: COMPLETED
>              MOBILE_AND_PHYSICAL: 택배사 인계 후 physicalDispatchedAt 기록, COMPLETED
> ```
> - 사용자 취소는 `SUBMITTED, REVIEWING, PHOTO_REJECTED`에서만 가능하다. `NAME_EDITING` 이후에는 불가능하며 `CANCELLED` 재호출은 멱등 성공이다.
> - 미입금 자동 취소는 `SUBMITTED + WAITING + payment_due_at<=now`만 대상으로 하며 스케줄러 기본 주기는 10분(설정 가능)이다.
> - 배송사·운송장·배송 중·배송 완료 상태는 저장하지 않고 택배사 인계 시각만 기록한다.
> - **단체(GROUP) 신청은 구성원별이 아니라 Application 단위로 검토/발급/작명 진행.** 예: 125명 중 25명 사진 반려 → 나머지 100명 먼저 발급 안 하고 `Application` 전체가 `PHOTO_REJECTED`로 전환. 사용자가 수정 ZIP(또는 반려 사진)을 재제출 → 관리자 재검토 완료 시 `REVIEWING`으로 복귀. **전원 통과해야만 `NAME_EDITING`, 전원 작명 완료해야만 `PRODUCING`으로 진행.** → `ApplicationMember`별 개별 status는 불필요.
> 십이간지 캐릭터는 card_design_id와 별개 — 저장하지 않고 렌더링 시 `ApplicationMember.birth_date`로 계산 (6절 참고)
> ✅ 2026-07-29 확인(API 명세 작성 중): `application_type=INDIVIDUAL`이면 `total_quantity`는 항상 `1`로 고정. 프론트 `StepInfo.tsx`의 "신청 수량" 입력은 법인/단체 신청에서만 의미가 있음(docs/api/README.md Application 도메인 참고).
> ⚠️ 2026-07-25 정정: `card_number`는 Application 컬럼이 아님 — "카드 1장 = ApplicationMember 1명" 원칙으로 통일되면서 **개인/단체 관계없이 항상 `ApplicationMember.card_number`에 저장.** (아래 2.4절 참고)

### 2.2 Applicant (신청인)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 신청자 정보 ID |
| application_id | BIGINT | FK, UNIQUE | Application(1:1) |
| name | VARCHAR(100) | NOT NULL | 신청 당시 이름 |
| email | VARCHAR(255) | NOT NULL | 신청 당시 이메일. ✅ 2026-08-07 정정(`APPLICATION.md` 기준): 로그인 `User.email`을 기본값으로 사용하며 **신청 화면에서 수정 가능**(계정 `User.email` 자체를 바꾸는 것은 아님, 이 신청 1건의 값만 저장) |
| phone | VARCHAR(20) | NOT NULL | 신청 당시 연락처 |
| postal_code | VARCHAR(10) | NULL | 우편번호 |
| address1 | VARCHAR(255) | NULL | 기본주소 |
| address2 | VARCHAR(255) | NULL | 상세주소 |
| organization_name | VARCHAR(200) | NULL | ✅ 2026-07-25 확인: 법인/단체명. 개인 신청(INDIVIDUAL)은 NULL, 법인/단체 신청(GROUP)에서만 사용 |
| department | VARCHAR(100) | NULL | ✅ 2026-07-25 확인: 부서명. 개인 신청은 NULL, 법인/단체 신청에서만 사용 |

> ✅ 2026-07-25 확인: 신청인 주소 컬럼은 DB에 있어야 함. 단, **UI 입력란은 수령인(Receiver) 쪽에만 만든다** —
> 신청인 주소는 별도 폼 필드 없이 저장(예: 동일인일 때 수령인 주소를 복사하는 등)하는 정책. 프론트에 신청인 주소 입력 필드를 새로 추가하지 말 것.
> ✅ 2026-07-25 확인: **Applicant(신청인)와 Receiver(수령인)는 서로 다른 사람일 수 있음** — 신청 시 사용자가 직접 입력해서 결정. 둘 다 Application에 속하는 정보이며, `ApplicationMember`와는 직접 연결하지 않음(2.4절 참고).

### 2.3 Receiver (수령인)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 수령인 정보 ID |
| application_id | BIGINT | FK, UNIQUE | Application(1:1) |
| receiver_name | VARCHAR(100) | | 수령인 이름 |
| receiver_phone | VARCHAR(20) | | 연락처 |
| country | VARCHAR(100) | NULL | ⚠️ 2026-07-25 확인: **보류.** 해외 배송 지원 여부가 아직 안 정해짐 — 지원 안 하기로 결정되면 이 컬럼 자체를 제거하는 방향으로 재검토 예정. 프론트 `StepInfo.tsx`에도 입력란 없음 |
| zip_code | VARCHAR(10) | | 우편번호 |
| address | VARCHAR(255) | | 기본주소 |
| detail_address | VARCHAR(255) | | 상세주소 |
| delivery_request | VARCHAR(255) | | 배송 요청사항 |
| organization_name | VARCHAR(200) | NULL | ✅ 2026-07-25 확인: 법인/단체명. 개인 신청은 NULL, 법인/단체 신청에서만 사용 |
| department | VARCHAR(100) | NULL | ✅ 2026-07-25 확인: 부서명. 개인 신청은 NULL, 법인/단체 신청에서만 사용 |
| created_at | DATETIME | | 생성일 |
| updated_at | DATETIME | | 수정일 |

> ✅ 2026-07-25 확인: **`is_same_as_applicant` 컬럼 제거.** 동일 여부 플래그는 `Application.receiver_same_as_applicant` 하나만 유지.
> ✅ 2026-08-07 정정(`APPLICATION.md` 기준): `IssueType=MOBILE`이면 Receiver row를 생성하지 않는다. `IssueType=MOBILE_AND_PHYSICAL`이면 Receiver가 필수이며 `sameAsApplicant=true`일 때 **이름과 연락처만** 복사하고 사용자가 수정할 수 있다(배송지는 복사 대상 아님 — 항상 Receiver 입력값 저장). 우편번호와 기본주소는 필수, 상세주소와 배송 요청사항은 선택이다.

### 2.4 ApplicationMember (카드 1장 단위)

> ✅ 2026-07-25 확정 원칙: **"카드 1장 = ApplicationMember 1명".** 개인/단체 구분 없이 통일.
> - 개인 신청: `Application` 1건 + `ApplicationMember` **1건**
> - 단체(ZIP) 신청: `Application` 1건 + `ApplicationMember` **N건** (엑셀+사진 인원 수만큼)
> - 개인/단체의 차이는 `ApplicationMember` 구조가 아니라 `CardDesign`·`Application` 데이터에서 처리 (예: 법인 로고·직인은 `Application.logo_file_id`/`seal_file_id`를 쓰고, 개인 카드는 해당 요소를 렌더링하지 않음)
> ✅ 2026-08-25 확정: `application_id`+`photo_number` 조합에 `UNIQUE` 제약을 둔다. 개인은 `photo_number`가 항상 NULL이라 여러 건이 저장돼도 충돌하지 않는다(NULL은 서로 다른 값으로 취급).

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| application_id | BIGINT | FK → Application, NOT NULL | 개인은 항상 1건, 단체는 N건 |
| name | VARCHAR(100) | NULL | 카드에 들어가는 한국이름 — 신청 시점엔 비어있고, 관리자 작명(KoreanName 절차, 7절) 이후 채워지는 값으로 추정 |
| english_name | VARCHAR(100) | NULL | 영문명 |
| chinese_name | VARCHAR(50) | NULL | 한자 이름 — 마찬가지로 작명 이후 채워짐 |
| name_meaning | TEXT | NULL | ✅ 2026-07-25 확인: 이름의 **뜻(짧은 의미)**. "풀이"와는 별개 필드로 확정 |
| name_interpretation | TEXT | NULL | ✅ 2026-07-25 신규 확정: 이름의 **상세 풀이(긴 설명)**. `name_meaning`을 재사용하지 않고 별도 컬럼으로 분리 (명칭은 가안, 최종 컬럼명은 추후 조정 가능) |
| photo_path | VARCHAR(500) | NULL | ✅ 2026-07-25 확인: 카드에 들어가는 사진. **`UploadFile` FK가 아니라 경로 텍스트 컬럼으로 확정** — 신청(Application)이 삭제되면 사진도 함께 삭제되는 생명주기와 자연스럽게 맞음 |
| address | VARCHAR(255) | NULL | 카드에 인쇄되는 주소. ✅ 2026-08-25 확정(`admin-saju.md` 기준): **학생증을 제외한 카드종류는 개인 신청도 이 컬럼에 저장한다.** 단체는 기존처럼 엑셀 행에서, 개인은 신규 신청 입력값(`MemberRequest.address`)에서 채워진다 — 학생증은 카드에 주소를 표시하지 않으므로 값이 있으면 거절한다. 배송용 `Receiver.address`와는 별도 값 |
| surname | VARCHAR(10) | NULL | ✅ 2026-08-25 신규 확정(`admin-saju.md` "성씨 분리 정책"): 관리자가 작명 단계에서 확정하는 한글 성씨(1~2글자). `NAME_EDITING` 중에는 NULL 허용, `completeNaming()` 실행 시 필수 — Application 소속 전 Member를 Service(`ApplicationService.completeNaming`)에서 집계 검증하며, 하나라도 성씨·이름·의미가 없으면 `NAMING_INCOMPLETE`로 거절하고 상태를 바꾸지 않는다(1-B 구현 완료). 형식(한글 1~2글자) 검증은 `ApplicationMember.assignKoreanName`에서 강제한다. 카드의 한글 이름은 `surname + name`으로 조합. 이름 추천·이름 사전에는 성씨를 저장하지 않는다 |
| photo_number | VARCHAR(10) | NULL | ✅ 2026-08-25 신규 확정: 단체 신청 Excel의 고정 사진 번호(`BulkMemberRow.photoNumber`, 예: "001"). 단체는 필수, 개인은 항상 NULL. `(application_id, photo_number)` 조합이 유일해야 관리자 카드번호 일괄 입력(사진 번호+카드번호 붙여넣기)에서 행을 정확히 매칭할 수 있다 |
| birth_date | DATE | NOT NULL | 십이간지(캐릭터) 계산용. ✅ 2026-07-25 확인: 개인 신청도 필수 — 개인 신청 폼(`StepInfo.tsx`)에 생년월일 입력란 추가 필요 (프론트 미구현, 별도 작업 필요) |
| nationality | VARCHAR(10) | NOT NULL | ✅ 2026-07-31 신규 확정: 국적(ISO 3166-1 alpha-2). 사주(만세력) 작명 도구 입력값 — 개인 신청 폼에 입력란 추가 필요(프론트 미구현) |
| birth_time | TIME | NULL | ⚠️ 2026-07-31 재정정: NOT NULL이었던 걸 **NULL로 변경.** "출생시간을 모릅니다" 체크 시 미입력 가능(사용자 명세 확정) |
| birth_region | VARCHAR(200) | NULL | ✅ 2026-08-24 최신 정책: 개인 DTO·단체 Excel에서는 **필수**인 태어난 도시/지역명. 기존 데이터 호환을 위해 DB 컬럼만 Nullable 유지 |
| gender | ENUM | NOT NULL | ✅ 2026-07-31 신규 확정: 성별(MALE, FEMALE). 사주 작명 도구 입력값 — 개인 신청 폼에 입력란 추가 필요(프론트 미구현) |
| entry_date | DATE | NULL | ✅ 2026-07-31 신규 확정: 한국 입국날짜. 선택 입력. 단체 신청 시 엑셀의 "공통 입국날짜"(상단 셀) + "개별입국날짜"(행별, 예외자만) 2단 해석을 거친 **최종값만 저장** — "공통값" 자체는 별도 컬럼으로 안 둠(`docs/specs/application/requirements.md` 2-3절) |
| email | VARCHAR(255) | NULL | ✅ 2026-07-31 신규 확정: 신청자 개인 이메일. **개인 신청은 항상 NULL**(로그인 계정=`Applicant.email`로 대체, 중복 저장 안 함) — **단체 신청에서만 엑셀 행별로 채워짐** |
| phone | VARCHAR(20) | NULL | ✅ 2026-07-31 신규 확정: 신청자 개인 연락처. email과 동일 원칙 — 개인 신청은 NULL, 단체 신청만 엑셀 행별로 채움 |
| student_id | VARCHAR(10) | NULL | ✅ 2026-08-07 정정(`APPLICATION.md` 기준): 학번. **카드종류=학생증일 때만 사용**(그 외 카드종류는 NULL), 최대 10자이며 숫자만 허용. ⚠️ 2026-08-14 조건 변경: 개인 신청은 "학생증이면 무조건 필수"가 아니라 `Application.school_type=UNIVERSITY`일 때만 필수 — `HIGH_SCHOOL`이면 오히려 NULL이어야 함(있으면 거절). 단체 신청은 이 조건과 무관하게 기존처럼 엑셀에서만 채워짐(`BulkExcelParser`, 변경 없음) |
| department | VARCHAR(100) | NULL | ✅ 2026-07-31 신규 확정: 학과. 카드종류=학생증 전용. ⚠️ `Applicant`/`Receiver`의 `department`(부서명, 법인용)와는 다른 테이블의 다른 개념 — 이름만 같음, 혼동 주의. ⚠️ 2026-08-14: student_id와 동일하게 개인 신청은 `school_type=UNIVERSITY`일 때만 필수 |
| issue_date | DATE | NULL | 카드 발급일자 — 발급 시점에 채워짐 |
| card_number | VARCHAR(30) | NULL, UNIQUE | 카드 발급 후 채워짐 (Application이 아니라 여기로 확정). ✅ 2026-07-31 확인: 형식 `ROK-XXXXX-XXXX`(5자리-4자리) — `시안.zip` 실물 카드번호 확인. ⚠️ 2026-08-25 재정정(`admin-saju.md` 기준, 이전 "채번 로직 미확정"·"서버 무작위 생성" 계획을 대체): **서버가 채번하지 않는다.** 관리자가 Member별로 `ROK-XXXXX-XXXX` 형식 값을 직접 입력·확정하고(개인은 단건 API, 단체는 사진 번호 기준 일괄 붙여넣기), DB `UNIQUE` 제약이 최종 유일성 방어선이다. 최초 카드 생성 성공 전에는 변경 가능하나 성공 후에는 재생성에서도 기존 번호를 유지한다. ✅ 2026-08-26 구현 완료(1-C): `PUT /api/admin/applications/{id}/members/{memberId}/card-number`(개인/단일)·`PUT /api/admin/applications/{id}/card-numbers`(단체 일괄, `(applicationId, photoNumber)` 매칭). "카드 생성 성공"은 `card_front_path IS NOT NULL`로 판정(`ApplicationMember.isCardGenerated()`) — 그 뒤 값이 다른 번호로 바꾸려 하면 `CARD_NUMBER_LOCKED`, 같은 값 재저장은 멱등 허용 |
| card_front_path | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 카드 발급(`PRODUCING`) 시 생성되는 **앞면 합성 결과 이미지** 경로. 사용자 다운로드용 |
| card_back_path | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 카드 발급 시 생성되는 **"이름풀이" 뒷면 합성 결과 이미지** 경로 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

> ✅ 2026-07-25 확인: `Applicant`/`Receiver`는 `ApplicationMember`와 직접 연결하지 않음 — 신청인/수령인은 Application에 속하는 정보이고, 서로 다른 사람일 수 있음(2.2/2.3절 참고).
> ✅ 2026-07-25 확인: **개인 신청 폼(`StepInfo.tsx`/`StepFiles.tsx`)에 사진 업로드 입력란 추가 확정.** 개인 신청도 카드용 사진이 필요함 — `birth_date`와 마찬가지로 프론트 미구현, 별도 작업 필요 (이번 문서 정리 범위에서는 코드 수정 안 함, 항목만 기록).
> ✅ 2026-07-31 확정: **사주(만세력) 작명 도구는 URL 링크아웃일 뿐, 실제 이름은 관리자가 우리 시스템에 직접 입력.** `nationality`/`birth_time`/`birth_region`/`gender`는 그 도구에 참고용으로 넣는 입력값이고, 실제 작명 결과(`name`/`chinese_name`/`name_meaning`/`name_interpretation`)는 관리자가 수동 입력 폼으로 저장 — 백엔드가 그 도구를 API로 호출하지 않음.
> ✅ 2026-07-31 확정: **신청조회(lookup) 시 카드번호로 조회하는 경우, 본인인증은 `Applicant`가 아니라 그 카드의 실제 소유자인 `ApplicationMember.email`/`phone`과 대조한다.** 개인 신청은 이 두 컬럼이 NULL이므로 자연히 `Applicant` 쪽을 참조하게 됨. 전화번호 인증/이메일 인증 조합(둘 다 필수 vs 하나만)은 API 설계 시 확정([TBD], `docs/api/README.md` 참고).

### 2.5 파일 및 Payment 데이터 정책 (2026-08-07, `APPLICATION.md` 기준)

- 학생증은 학교 로고가 필수이고 학교 직인은 선택이다. 그 외 단체 카드종류는 로고와 직인이 모두 필수다.
- 모든 검증 후 최종 경로에 업로드하고 `UploadFile`을 저장한다. DB 저장 실패 시 해당 요청이 업로드한 파일을 역순 삭제하며 삭제 실패는 Error 로그로 남긴다.
- 얼굴사진·학교 로고·직인 수정은 새 파일 업로드와 DB 갱신 성공 후 기존 파일을 삭제한다.
- 신청 생성 시 Payment를 생성하거나 totalPrice를 계산하지 않는다. 향후 온라인 결제를 위해 Payment Entity와 관련 도메인은 유지한다.
