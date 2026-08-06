# Application Data Model

> Application 도메인의 엔티티, 컬럼, 관계 및 제약조건에 대한 Source of Truth입니다.
> 업무 규칙은 [requirements.md](requirements.md), 외부 계약은 [api.md](api.md)를 기준으로 합니다.

## 2. Application 도메인

### 2.1 Application

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 신청 ID |
| application_number | VARCHAR(20) | NOT NULL, UNIQUE | ✅ 2026-07-25 확인: 서버 생성 신청번호(예: `APP-2026-XXXXXX`). 사용자가 신청 조회·고객센터 문의 시 사용하는 식별자 |
| user_id | BIGINT | FK → User | 신청한 회원 |
| card_type_id | BIGINT | FK → CardType, NOT NULL | 카드 종류. ✅ 2026-07-31 명시: **신청 생성 API에서 사용자가 직접 선택해서 보내는 값**(4종 중 1개) — `card_design_id`가 사용자 입력에서 빠지면서, 이 컬럼이 신청 생성 요청에 실리는 유일한 "카드 관련" 식별자가 됨 |
| application_type | ENUM | NOT NULL | INDIVIDUAL, GROUP |
| status | ENUM | NOT NULL | ✅ 2026-07-31 재정정: `PAYMENT_PENDING, RECEIVED, REVIEWING, PHOTO_REJECTED, NAME_EDITING, PRODUCING, COMPLETED, CANCELLED` — 사진 승인과 작명 완료는 별개 문제라 `NAME_EDITING` 상태 신규 추가 (Admin 도메인 설계 중 확정, 아래 참고) |
| payment_status | ENUM | NOT NULL | WAITING, CONFIRMED |
| receiver_same_as_applicant | BOOLEAN | NOT NULL DEFAULT TRUE | 신청자와 수령인이 동일한지 여부 |
| total_quantity | INT | NOT NULL | 신청 인원 수 |
| issue_type | ENUM | NOT NULL | MOBILE, MOBILE_AND_PHYSICAL — ✅ 2026-07-25 확인: Application 테이블에 반드시 있어야 함 (누락이었음) |
| card_design_id | BIGINT | FK → CardDesign, NULL | ⚠️ 2026-07-31 재정정: **"신청 1건당 디자인 1개 선택"은 취소 — 사용자가 고르지 않음.** 신청 생성 시점엔 항상 `NULL`, **관리자가 신청 검토 과정에서 배정**(정확한 배정 시점은 Admin API 설계 시 확정, [TBD]). `시안.zip` 확인 결과 디자인이 발행 지자체별로 나뉘는 행정적 값이라 사용자가 미학적으로 고를 성격이 아님(`docs/specs/application/requirements.md` 6절) |
| logo_file_id | BIGINT | FK → UploadFile, NULL | 업로드한 로고. 학생증은 개인/단체 모두 학교 로고로 사용하고, 그 외 카드종류는 GROUP에서 사용 |
| seal_file_id | BIGINT | FK → UploadFile, NULL | 업로드한 직인. 일반 단체 신청에서는 필수이고 학생증 개인·단체 신청에서는 선택 |
| submit_file_id | BIGINT | FK → UploadFile, NULL | ✅ 2026-07-25 확인: 제출한 ZIP(엑셀+사진) |
| photo_reject_reason | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 관리자가 사진 반려 시 입력하는 사유. `status=PHOTO_REJECTED`일 때 사용자에게 노출(`/lookup` 조회 결과) |

> ✅ 2026-07-31 확정(Admin 도메인 — 신청 상태 흐름, 사진검토/작명 분리 재정정):
> ```
> PAYMENT_PENDING(결제전)
>    │  ※ 신청일로부터 3일 이내 미입금 시 스케줄러가 자동 CANCELLED 처리
>    ▼
> RECEIVED(접수완료)   ← 관리자가 입금 확인(payment_status: WAITING→CONFIRMED)
>    ▼
> REVIEWING(검토중 — 사진/내용 검토)
>    ├─ 반려 → PHOTO_REJECTED(사진반려) → 사용자 재업로드 → REVIEWING(복귀)
>    └─ 승인 → NAME_EDITING(작명중)   ← 사진 승인과 작명은 별개 문제라 상태 분리
>                  │  관리자가 ApplicationMember별 이름/한자/뜻/풀이 입력
>                  ▼  (전원 작명 완료)
>              PRODUCING(카드발급중) → COMPLETED(발급완료)
> (모든 단계에서 → CANCELLED 가능)
> ```
> - **이번 범위는 `MOBILE`(웹/디지털 발급)만 다룸.** `MOBILE_AND_PHYSICAL`의 실물 배송(SHIPPING/DELIVERED, 운송장 처리)은 이번 Admin 설계 범위 밖 — `issue_type` enum 값 자체는 유지, 배송 처리 흐름은 추후 별도 설계.
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
| email | VARCHAR(255) | NOT NULL | 신청 당시 이메일. ✅ 2026-07-25 정정: **신청 시 이메일은 가입(User.email) 이메일과 같아야 함** — 자유 입력 값이 아니라 로그인 계정 이메일과 일치해야 하는 제약 |
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

> ✅ 2026-07-25 확인: **`is_same_as_applicant` 컬럼 제거.** 동일 여부 플래그는 `Application.receiver_same_as_applicant` 하나만 유지. 사용자가 "신청인과 동일" 체크 시, 그 시점에 `Applicant`의 정보를 `Receiver`로 복사해서 저장하는 구조로 설계(값 자체를 이중 저장하지 않음).

### 2.4 ApplicationMember (카드 1장 단위)

> ✅ 2026-07-25 확정 원칙: **"카드 1장 = ApplicationMember 1명".** 개인/단체 구분 없이 통일.
> - 개인 신청: `Application` 1건 + `ApplicationMember` **1건**
> - 단체(ZIP) 신청: `Application` 1건 + `ApplicationMember` **N건** (엑셀+사진 인원 수만큼)
> - 개인/단체의 차이는 `ApplicationMember` 구조가 아니라 `CardDesign`·`Application` 데이터에서 처리 (예: 법인 로고·직인은 `Application.logo_file_id`/`seal_file_id`를 쓰고, 개인 카드는 해당 요소를 렌더링하지 않음)

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
| address | VARCHAR(255) | NULL | 카드에 인쇄되는 주소 |
| birth_date | DATE | NOT NULL | 십이간지(캐릭터) 계산용. ✅ 2026-07-25 확인: 개인 신청도 필수 — 개인 신청 폼(`StepInfo.tsx`)에 생년월일 입력란 추가 필요 (프론트 미구현, 별도 작업 필요) |
| nationality | VARCHAR(10) | NOT NULL | ✅ 2026-07-31 신규 확정: 국적(ISO 3166-1 alpha-2). 사주(만세력) 작명 도구 입력값 — 개인 신청 폼에 입력란 추가 필요(프론트 미구현) |
| birth_time | TIME | NULL | ⚠️ 2026-07-31 재정정: NOT NULL이었던 걸 **NULL로 변경.** "출생시간을 모릅니다" 체크 시 미입력 가능(사용자 명세 확정) |
| birth_region | VARCHAR(200) | NULL | ⚠️ 2026-07-31 재정정: NOT NULL이었던 걸 **NULL로 변경(선택 입력)** — `docs/specs/application/requirements.md` 2-1절 확정 |
| gender | ENUM | NOT NULL | ✅ 2026-07-31 신규 확정: 성별(MALE, FEMALE). 사주 작명 도구 입력값 — 개인 신청 폼에 입력란 추가 필요(프론트 미구현) |
| entry_date | DATE | NULL | ✅ 2026-07-31 신규 확정: 한국 입국날짜. 선택 입력. 단체 신청 시 엑셀의 "공통 입국날짜"(상단 셀) + "개별입국날짜"(행별, 예외자만) 2단 해석을 거친 **최종값만 저장** — "공통값" 자체는 별도 컬럼으로 안 둠(`docs/specs/application/requirements.md` 2-3절) |
| email | VARCHAR(255) | NULL | ✅ 2026-07-31 신규 확정: 신청자 개인 이메일. **개인 신청은 항상 NULL**(로그인 계정=`Applicant.email`로 대체, 중복 저장 안 함) — **단체 신청에서만 엑셀 행별로 채워짐** |
| phone | VARCHAR(20) | NULL | ✅ 2026-07-31 신규 확정: 신청자 개인 연락처. email과 동일 원칙 — 개인 신청은 NULL, 단체 신청만 엑셀 행별로 채움 |
| student_id | VARCHAR(50) | NULL | ✅ 2026-07-31 신규 확정: 학번. **카드종류=학생증일 때만 사용**(그 외 카드종류는 NULL) |
| department | VARCHAR(100) | NULL | ✅ 2026-07-31 신규 확정: 학과. 카드종류=학생증 전용. ⚠️ `Applicant`/`Receiver`의 `department`(부서명, 법인용)와는 다른 테이블의 다른 개념 — 이름만 같음, 혼동 주의 |
| issue_date | DATE | NULL | 카드 발급일자 — 발급 시점에 채워짐 |
| card_number | VARCHAR(30) | NULL, UNIQUE | 카드 발급 후 채워짐 (Application이 아니라 여기로 확정). ✅ 2026-07-31 확인: 형식 `ROK-XXXXX-XXXX`(5자리-4자리) — `시안.zip` 실물 카드번호 확인. 채번 로직(순차/무작위)은 미확정 |
| card_front_path | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 카드 발급(`PRODUCING`) 시 생성되는 **앞면 합성 결과 이미지** 경로. 사용자 다운로드용 |
| card_back_path | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 카드 발급 시 생성되는 **"이름풀이" 뒷면 합성 결과 이미지** 경로 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

> ✅ 2026-07-25 확인: `Applicant`/`Receiver`는 `ApplicationMember`와 직접 연결하지 않음 — 신청인/수령인은 Application에 속하는 정보이고, 서로 다른 사람일 수 있음(2.2/2.3절 참고).
> ✅ 2026-07-25 확인: **개인 신청 폼(`StepInfo.tsx`/`StepFiles.tsx`)에 사진 업로드 입력란 추가 확정.** 개인 신청도 카드용 사진이 필요함 — `birth_date`와 마찬가지로 프론트 미구현, 별도 작업 필요 (이번 문서 정리 범위에서는 코드 수정 안 함, 항목만 기록).
> ✅ 2026-07-31 확정: **사주(만세력) 작명 도구는 URL 링크아웃일 뿐, 실제 이름은 관리자가 우리 시스템에 직접 입력.** `nationality`/`birth_time`/`birth_region`/`gender`는 그 도구에 참고용으로 넣는 입력값이고, 실제 작명 결과(`name`/`chinese_name`/`name_meaning`/`name_interpretation`)는 관리자가 수동 입력 폼으로 저장 — 백엔드가 그 도구를 API로 호출하지 않음.
> ✅ 2026-07-31 확정: **신청조회(lookup) 시 카드번호로 조회하는 경우, 본인인증은 `Applicant`가 아니라 그 카드의 실제 소유자인 `ApplicationMember.email`/`phone`과 대조한다.** 개인 신청은 이 두 컬럼이 NULL이므로 자연히 `Applicant` 쪽을 참조하게 됨. 전화번호 인증/이메일 인증 조합(둘 다 필수 vs 하나만)은 API 설계 시 확정([TBD], `docs/api/README.md` 참고).
