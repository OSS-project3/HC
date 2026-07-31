# 엔티티 설계 정리 (혜원 공유 내용 정리 · 2026-07-25)

> 원본은 혜원 님이 채팅으로 순서 없이 공유한 내용입니다. 아래는 엔티티별로 재배치만 한 것이며,
> 원본에서 잘려서 못 받은 필드는 **[TBD]** 로 표시했습니다. 임의로 필드를 추가/추측하지 않았습니다.

---

## 목차

1. [User](#1-user)
2. [Application 도메인](#2-application-도메인)
   - 2.1 [Application](#21-application)
   - 2.2 [Applicant (신청인)](#22-applicant-신청인)
   - 2.3 [Receiver (수령인)](#23-receiver-수령인)
   - 2.4 [ApplicationMember (카드 1장 단위)](#24-applicationmember-카드-1장-단위)
   - 2.5 [Payment (결제)](#25-payment-결제)
3. [UploadFile](#3-uploadfile)
4. [카드 도메인](#4-카드-도메인)
   - 4.1 [CardType](#41-cardtype)
   - 4.2 [CardDesign](#42-carddesign)
   - 4.3 [CardFieldDefinition](#43-cardfielddefinition) ⚠️ 원본 잘림
5. [게시판](#5-게시판)
   - 5.1 [Review (후기)](#51-review-후기)
   - 5.2 [Post (행사·사업)](#52-post-행사사업) ⚠️ 원본 대부분 잘림
6. [카드 출력 매핑](#6-카드-출력-매핑)
7. [확정된 정책 (2026-07-25)](#7-확정된-정책-2026-07-25)
8. [미해결 / 확인 필요 사항 (남은 것)](#8-미해결--확인-필요-사항-남은-것)
9. [부록 — 초기 요약 초안 (참고용)](#9-부록--초기-요약-초안-참고용)

---

## 1. User

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 사용자 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 이메일 |
| provider | ENUM | NOT NULL | GOOGLE, NAVER |
| provider_id | VARCHAR(255) | UNIQUE, NOT NULL | OAuth ID |
| role | ENUM | NOT NULL | USER, ADMIN |
| name | VARCHAR(100) | NOT NULL | 이름 |
| phone | VARCHAR(20) | NULL | ✅ 2026-07-31 정정: 기존 코드(`User.java`)엔 이 컬럼 자체가 없었음 — "NOT NULL 제약을 완화"한 게 아니라 **새로 추가하는 컬럼**임(표현 정정). OAuth(Google/Naver)는 전화번호를 제공하지 않고, 실제 연락처는 신청 시점마다 `Applicant.phone`에서 받으므로 계정 가입 단계에서 강제할 필요 없음 |
| address | VARCHAR(255) | NULL | 주소 |
| terms_agreed | BOOLEAN | NOT NULL DEFAULT FALSE | ✅ 2026-07-31 신규 확정: 약관동의 여부. **기존 코드에 이미 있던 개념 — 유지하기로 확정.** 신규 가입 시 `/terms`로 리다이렉트되어 동의받는 흐름 그대로 유지 |
| privacy_agreed | BOOLEAN | NOT NULL DEFAULT FALSE | 개인정보 처리 동의 |
| image_upload_agreed | BOOLEAN | NOT NULL DEFAULT FALSE | 이미지 업로드 동의 |
| shipping_agreed | BOOLEAN | NOT NULL DEFAULT FALSE | 배송 안내 동의 |
| terms_agreed_at | DATETIME | NULL | 약관 동의 일시 |
| created_at | DATETIME | NOT NULL | 가입일 |
| updated_at | DATETIME | NOT NULL | 수정일 |
| last_login_at | DATETIME | NULL | 마지막 로그인 |
| status | ENUM | NOT NULL DEFAULT ACTIVE | ✅ 2026-07-31 신규 확정: `ACTIVE`, `WITHDRAWN`. 회원탈퇴(소프트) 처리 시 `WITHDRAWN`으로 전환 |
| withdrawal_requested_at | DATETIME | NULL | ✅ 2026-07-31 신규 확정: 탈퇴(소프트 삭제) 요청 시각 — 유예기간(7일) 계산 기준 |
| anonymized_at | DATETIME | NULL | ✅ 2026-07-31 신규 확정: 완전탈퇴(익명화) 처리 시각. NULL이면 아직 유예기간 내(복구 가능), 값이 있으면 되돌릴 수 없음 |

> ✅ 2026-07-31 확정(회원탈퇴 정책): **소프트 삭제 → 완전탈퇴 2단계 유예기간 방식.** 사용자 액션은 "탈퇴하기" 하나뿐.
> ```
> 사용자가 탈퇴 요청
>    ↓
> status=WITHDRAWN, withdrawal_requested_at=NOW (소프트 삭제) + 세션 즉시 무효화
>    │  (유예기간 7일 이내 재로그인 시 자동 복구: status=ACTIVE, withdrawal_requested_at=NULL)
>    ▼ (7일 경과, 재로그인 없음)
> 스케줄러가 email/name/oauth_id/oauth_provider(+phone/address) 스크램블 처리, anonymized_at=NOW
> ```
> - **완전탈퇴 시 `User` row 자체는 삭제하지 않고 PII만 익명화** — `Application`/`Payment` 등 연관 이력은 `user_id` FK 그대로 보존(운영/법적 목적). 신청 이력이 있어도 탈퇴 자체는 항상 허용.
> - `oauth_id`/`oauth_provider`도 스크램블 대상이라, 완전탈퇴 이후 같은 구글/네이버 계정으로 재로그인하면 이 row가 복구되는 게 아니라 **새 User row가 생성**됨(의도된 동작 — 계정 연결고리를 끊는 게 "완전"탈퇴의 의미).

---

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
| total_price | DECIMAL(10,2) | NOT NULL | 총 결제 금액 |
| issue_type | ENUM | NOT NULL | MOBILE, MOBILE_AND_PHYSICAL — ✅ 2026-07-25 확인: Application 테이블에 반드시 있어야 함 (누락이었음) |
| card_design_id | BIGINT | FK → CardDesign, NULL | ⚠️ 2026-07-31 재정정: **"신청 1건당 디자인 1개 선택"은 취소 — 사용자가 고르지 않음.** 신청 생성 시점엔 항상 `NULL`, **관리자가 신청 검토 과정에서 배정**(정확한 배정 시점은 Admin API 설계 시 확정, [TBD]). `시안.zip` 확인 결과 디자인이 발행 지자체별로 나뉘는 행정적 값이라 사용자가 미학적으로 고를 성격이 아님(`APPLICATION-사용자명세.md` 6절) |
| logo_file_id | BIGINT | FK → UploadFile, NULL | ✅ 2026-07-25 확인: 업로드한 로고. ⚠️ 2026-07-31 조건 추가: 기존엔 "GROUP 전용"이었으나, **카드종류=학생증(학교로고)이면 개인/단체 무관하게 사용** — 그 외 카드종류는 기존대로 GROUP만 |
| seal_file_id | BIGINT | FK → UploadFile, NULL | ✅ 2026-07-25 확인: 업로드한 직인. 조건은 `logo_file_id`와 동일(학생증=학교직인, 개인/단체 무관) |
| submit_file_id | BIGINT | FK → UploadFile, NULL | ✅ 2026-07-25 확인: 제출한 ZIP(엑셀+사진) |
| photo_reject_reason | VARCHAR(500) | NULL | ✅ 2026-07-31 신규 확정: 관리자가 사진 반려 시 입력하는 사유. `status=PHOTO_REJECTED`일 때 사용자에게 노출(`/lookup` 조회 결과) |

> ✅ 2026-07-31 확정(Admin 도메인 — 신청 상태 흐름, 사진검토/작명 분리 재정정):
> ```
> PAYMENT_PENDING(결제전)
>    │  ※ 3영업일 내 미입금 시 스케줄러가 자동 CANCELLED 처리
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
> ✅ 2026-07-29 확인(API 명세 작성 중): `application_type=INDIVIDUAL`이면 `total_quantity`는 항상 `1`로 고정. 프론트 `StepInfo.tsx`의 "신청 수량" 입력은 법인/단체 신청에서만 의미가 있음(API-명세.md Application 도메인 참고).
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
| birth_region | VARCHAR(200) | NULL | ⚠️ 2026-07-31 재정정: NOT NULL이었던 걸 **NULL로 변경(선택 입력)** — `APPLICATION-사용자명세.md` 2-1절 확정 |
| gender | ENUM | NOT NULL | ✅ 2026-07-31 신규 확정: 성별(MALE, FEMALE). 사주 작명 도구 입력값 — 개인 신청 폼에 입력란 추가 필요(프론트 미구현) |
| entry_date | DATE | NULL | ✅ 2026-07-31 신규 확정: 한국 입국날짜. 선택 입력. 단체 신청 시 엑셀의 "공통 입국날짜"(상단 셀) + "개별입국날짜"(행별, 예외자만) 2단 해석을 거친 **최종값만 저장** — "공통값" 자체는 별도 컬럼으로 안 둠(`APPLICATION-사용자명세.md` 2-3절) |
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
> ✅ 2026-07-31 확정: **신청조회(lookup) 시 카드번호로 조회하는 경우, 본인인증은 `Applicant`가 아니라 그 카드의 실제 소유자인 `ApplicationMember.email`/`phone`과 대조한다.** 개인 신청은 이 두 컬럼이 NULL이므로 자연히 `Applicant` 쪽을 참조하게 됨. 전화번호 인증/이메일 인증 조합(둘 다 필수 vs 하나만)은 API 설계 시 확정([TBD], `API-명세.md` 참고).

### 2.5 Payment (결제)

> ✅ 2026-07-29 확정(API 명세 작성 중): 결제는 PG/가상계좌 자동화가 아니라 **고정 회사 계좌 무통장입금 + 관리자 수동 확인** 방식. `StepComplete.tsx` 근거(계좌 하나 고정, 가상계좌 없음, 금액도 "사전 상담 시 확정"이라 화면에 표시 안 함). 기존 백엔드의 무거운 `Payment`/`PaymentLog`(주문번호·PG키·영수증URL 등)는 재사용 안 하고, 이 방식에 맞게 가볍게 새로 설계.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | |
| application_id | BIGINT | FK → Application, UNIQUE(1:1) | |
| depositor_name | VARCHAR(100) | NOT NULL | ✅ 사용자가 입력하는 실제 입금자명(신청인 이름과 달라도 됨 — 법인 신청은 회사명으로 입금하는 게 정상 케이스). 관리자가 이 값 기준으로 통장 내역과 대조 |
| confirmed_at | DATETIME | NULL | 관리자가 입금 확인한 일시. NULL이면 미확인 — 확인 여부는 이 컬럼의 NULL 여부로 판단(별도 boolean 플래그 없음) |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

> ✅ 2026-07-29 확정: **입금 확인 여부는 `Application.payment_status`(WAITING/CONFIRMED) 하나로만 관리.** 처음엔 `Payment.is_confirmed`를 따로 뒀는데 `Application.payment_status`와 의미가 중복되어 제거 — `payment_status`만 유지.
> ✅ 2026-07-29 확정: **`StepComplete.tsx`의 입금자명 입력란은 필수값으로 만들고 실제로 저장되게 고쳐야 함** — 지금은 `value`/`onChange`가 없는 비활성 입력란이라 아무것도 안 저장됨(프론트 미구현, 별도 작업 필요). "신청자명과 입금자명이 다르면 지연될 수 있다"는 안내 문구는 유지하되, 무조건 일치를 강제하지 않음 — 관리자는 신청자명이 아니라 `depositor_name`으로 대조.

**가격 정책 (2026-07-29 확정)**
- `CardType.price` = 현재 카드 기본 가격(관리자가 수정 가능, 시점에 따라 바뀔 수 있음)
- `Application.total_price` = **신청 당시 확정된 결제 금액**(가격 스냅샷). 신청 생성 시 서버가 `CardType.price`를 조회해서 `price × total_quantity`를 계산해 저장 — **이후 `CardType.price`가 바뀌어도 기존 신청의 `total_price`는 안 바뀜**
- ✅ `total_price`는 **NOT NULL 유지** (Application API 1/2에서 이미 이렇게 설계했던 것 그대로 맞음, 정정 취소)
- ✅ 결제 확인 상태는 `Application.payment_status` 하나로만 관리(위 2.5절 참고, `Payment.is_confirmed`는 제거)

---

## 3. UploadFile

사진, 엑셀, ZIP 등 업로드되는 모든 파일을 관리하는 **공용(폴리모픽) 엔티티**.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 파일 ID |
| original_name | VARCHAR(255) | NOT NULL | 원본 파일명 |
| stored_name | VARCHAR(255) | NOT NULL | 서버 저장 파일명(UUID 등) |
| file_path | VARCHAR(500) | NOT NULL | 저장 경로 |
| file_type | ENUM | NOT NULL | PHOTO, EXCEL, ZIP, CARD_IMAGE |
| mime_type | VARCHAR(100) | NOT NULL | MIME 타입 |
| file_size | BIGINT | NOT NULL | 파일 크기(Byte) |
| uploaded_at | DATETIME | NOT NULL | 업로드 시각 |

> ✅ 2026-07-25 확인: 로고·직인·제출ZIP은 역할이 고정된 3개 파일이므로, 조인 테이블이 아니라 **Application에 `logo_file_id`/`seal_file_id`/`submit_file_id` 역할별 FK 3개**로 연결 (2.1절 참고)

---

## 4. 카드 도메인

### 4.1 CardType

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 카드 종류 ID |
| code | ENUM | NOT NULL, UNIQUE | ✅ 2026-07-31 신규 추가: `HONOR_KOREAN`, `HONOR_CITIZEN`, `VISITOR`, `STUDENT`. `name`은 관리자가 자유롭게 수정 가능한 표시용 문자열이라, 학생증 전용 필드(학번/학과/학교로고/학교직인) 노출 여부 같은 **비즈니스 로직이 문자열 이름 매칭에 기대면 안 됨** — 코드값으로 판별 |
| name | VARCHAR(100) | NOT NULL, UNIQUE | 카드 종류명(표시용) |
| description | TEXT | NULL | 카드 설명 |
| price | DECIMAL(10,2) | NOT NULL | 기본 발급 가격 |
| is_active | BOOLEAN | NOT NULL DEFAULT TRUE | 신청 가능 여부 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

> ✅ 2026-07-31 확정: 학생증(`code=STUDENT`) 전용 처리(학번/학과/학교로고/학교직인 필드 노출, `Application.logo_file_id`/`seal_file_id`를 개인 신청에도 적용 등)는 전부 이 `code` 값으로 분기.

### 4.2 CardDesign

> ✅ 2026-07-29 확정: **`DesignPage.tsx` 등 전시용 화면은 계속 프론트 정적 자산 사용 — 백엔드가 미리보기 이미지를 관리할 필요 없음** (홈페이지 정적정보와 같은 결). `CardDesign`은 **실제 카드 생성 시 `CardFieldDefinition`이 참조하는 빈 템플릿 저장소**로만 존재 — 순수 내부/관리자용, 공개 조회 API 없음.
> ✅ 2026-07-31 정정(`시안.zip` 실물 확인 후): 아래 두 가지는 이전에 잘못 판단했던 부분 — 시안 자료로 확인되어 정정함.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 디자인 ID |
| card_type_id | BIGINT | FK → CardType | 카드 종류 |
| name | VARCHAR(100) | NOT NULL | 디자인 이름 |
| orientation | ENUM | NOT NULL | **정정(부활): LANDSCAPE, PORTRAIT.** `시안.zip` 확인 결과 명예시민증/명예한국인증=83×55mm(가로), 방문증=55×83mm(세로)로 카드 실물 크기 자체가 다름 — 프론트 표시용이 아니라 실제 합성 캔버스 크기를 결정하는 값이라 반드시 필요 |
| template_front_id | BIGINT | FK → UploadFile | **정정: 템플릿 1장→2장으로 분리.** 앞면 빈 템플릿(사람 정보 없는 배경) |
| template_back_id | BIGINT | FK → UploadFile | 뒷면("이름풀이") 빈 템플릿 |
| is_default | BOOLEAN | NOT NULL DEFAULT FALSE | 기본 디자인 여부 |
| is_active | BOOLEAN | NOT NULL DEFAULT TRUE | 사용 여부 |
| created_at | DATETIME | NOT NULL | 생성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

> ✅ 2026-07-31 확인: `시안.zip` 기준 카드종류당 디자인이 **정확히 6개**(명예시민증/명예한국인증/방문증 전부 폴더 1~6). 시드 데이터 참고용.
> ✅ 2026-07-31 확인: 발행처/발행처로고/직인은 **CardDesign에 고정 — 정정 불필요했음.** 디자인 6개가 각각 다른 발행 지자체(삼척시/서울시/제천시/공주시/상주시/전주시)를 담고 있어서, 애초 문서의 "❌ 발행처 = CardDesign 고정 이미지"가 맞았음(이전에 재검토가 필요하다고 잘못 flag했던 것 취소).
> ⚠️ 학생증(STUDENT) 디자인은 아직 시안 미전달 — 나중에 받기로 확정, 지금 구조는 그대로 두고 자료 오면 반영.

### 4.3 CardFieldDefinition

> ✅ 2026-07-29 확정: **DB 테이블 아님 — 설정값(config) 또는 코드 상수로 관리.** 운영 중 관리자가 수정하는 데이터가 아니라 렌더링용 고정 설정값이라, DB CRUD보다 config 파일/코드 상수가 더 단순하고 적합.
> ✅ 2026-07-31 확정(`시안.zip`의 "카드사이즈 및 위치값" 실물 확인): 필드 구성이 아래처럼 확정됨. 타이틀/발행처/발행처로고/직인은 **디자이너가 템플릿 PNG에 이미 박아넣는 고정 요소라 CardFieldDefinition에 안 들어감** — 좌표표엔 참고로 있었지만 실제로 매 신청마다 동적으로 합성해야 하는 값이 아님.

**앞면(front) — 동적 합성 필드**

| field_key | 값 출처(ApplicationMember) |
|---|---|
| NAME | name(한국이름) |
| ENGLISH_NAME | english_name |
| PHOTO | photo_path |
| CARD_NUMBER | card_number |
| ADDRESS | address |
| ISSUE_DATE | issue_date |
| CHARACTER | birth_date → 십이간지 계산(6절 참고) |
| STUDENT_ID | student_id (✅ 2026-07-31 신규 — `CardType.code=STUDENT`일 때만 사용) |
| DEPARTMENT | department (✅ 2026-07-31 신규 — `CardType.code=STUDENT`일 때만 사용) |
| SCHOOL_LOGO | 값 아님 — `Application.logo_file_id` (✅ 2026-07-31 신규, `CardType.code=STUDENT` 전용. 개인/단체 학생증 모두 적용) |
| SCHOOL_SEAL | 값 아님 — `Application.seal_file_id` (동일 조건) |

> ✅ 2026-07-31 확정: 위 4개(`STUDENT_ID`/`DEPARTMENT`/`SCHOOL_LOGO`/`SCHOOL_SEAL`)는 **학생증 전용 필드셋** — `CardType.code=STUDENT`일 때만 config에서 활성화, 나머지 3종 카드에서는 이 4개 필드 자체가 없음. 좌표 config도 학생증만 별도 버전 필요.

**뒷면(back, "이름풀이") — 동적 합성 필드**

| field_key | 값 출처(ApplicationMember) |
|---|---|
| NAME | name |
| CHINESE_NAME | chinese_name (NULL이면 이 필드 생략) |
| ENGLISH_NAME | english_name |
| NAME_MEANING | name_meaning(한자뜻음) |
| NAME_INTERPRETATION | name_interpretation(풀이) |

> ⚠️ **한자 유무에 따라 좌표셋이 2벌.** `chinese_name`이 있는지 없는지에 따라 나머지 필드(이름/영문명/한자뜻음/풀이)의 y좌표가 달라짐(한자 줄이 없으면 전체가 위로 당겨짐) — config에 "한자 있음 버전"/"한글만 버전" 좌표를 각각 준비해야 함.

각 필드는 `card_design_id`별로 x/y/width/height/font_family/font_size/font_color를 가짐 (여전히 config 스키마, DB 아님).

---

## 5. 게시판

> ✅ 2026-07-25 확인(고객지원 페이지 검토): **공지사항(`/support` #notice)도 이 게시판 도메인으로 같이 관리되어야 함.** 지금은 `SupportPage.tsx`에 배열로 하드코딩되어 있는데, 나중에 관리자가 추가/수정 가능해야 함 — 아마 5.2 `Post`와 같은 성격이거나 비슷한 구조가 필요할 것으로 보임(전용 `Notice` 엔티티인지 `Post` 재사용인지는 미정, 추후 확인).
> FAQ는 **보류** — 지금은 정적으로 두고 나중에 다시 논의.
> "1:1 문의"(홈페이지 버튼이 연결하는 기능)는 **아직 계획 없음** — `Inquiry` 같은 엔티티는 추가하지 않음. 홈페이지 버튼이 실제 기능 없는 섹션으로 연결되는 프론트 쪽 불일치는 있는 상태로 둠(이번 문서 정리 범위 아님).

### 5.1 Review (후기)

사용자가 작성하는 후기 게시판.

> ✅ 2026-07-25 확인: 프론트는 아직 미구현(`/reviews`는 `StubPage`뿐). **"상품 리뷰" 도메인과 비슷한 형태로 갈 예정** — 지금 단계에서는 이 엔티티가 존재할 거라는 것만 인지하고, 화면 요구사항이 나오면 그때 다시 대조.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK | 후기 ID |
| user_id | BIGINT | FK → User | 작성자 |
| application_id | BIGINT | FK → Application, NULL | 관련 신청(선택) |
| title | VARCHAR(200) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| thumbnail_file_id | BIGINT | FK → UploadFile, NULL | 대표 이미지 |
| view_count | INT | DEFAULT 0 | 조회수 |
| is_deleted | BOOLEAN | DEFAULT FALSE | 삭제 여부 |
| created_at | DATETIME | NOT NULL | 작성일 |
| updated_at | DATETIME | NOT NULL | 수정일 |

### 5.2 Post (행사·사업)

관리자가 작성하는 게시판. ⚠️ 원본이 도입부만 있고 필드 테이블 전체가 잘림 — [TBD]

> ✅ 2026-07-25 확인: 프론트는 아직 미구현(`/events`는 `StubPage`뿐). `Review`와 동일하게 지금은 "존재할 것"만 인지하고, 화면 요구사항이 나오면 그때 필드 확정.

---

## 6. 카드 출력 매핑

카드 이미지에 실제로 합성되는 값이 어느 엔티티/필드에서 오는지 정리한 표. (`ApplicationMember`는 2.4절에서 정식 엔티티로 확정됨 — "카드 1장 = ApplicationMember 1명", 개인/단체 공통)

| 카드 필드 | 출처 (추정: ApplicationMember 필드) |
|---|---|
| 이름 | `name` |
| 영문명 | `english_name` |
| 사진 | `ApplicationMember.photo_path` (경로 텍스트, `UploadFile` FK 아님) |
| 카드번호 | `ApplicationMember.card_number` — 개인/단체 공통 (2.4절) |
| 주소 | `address` |
| 발급일자 | `issue_date` |
| 캐릭터(띠) | 저장값 아님 — `ApplicationMember.birth_date` → 12간지 계산 → 캐릭터 이미지 선택 후 `CardFieldDefinition`(field_key=`CHARACTER`)의 좌표(x, y, width, height)에 합성 |
| 한자 | `chinese_name` |
| 한자 뜻/음 | `name_meaning` (짧은 의미) |
| 풀이 | `name_interpretation` — ✅ 2026-07-25 확인: `name_meaning`과 별개 컬럼으로 확정(긴 설명) |
| ❌ 발행처 | `CardDesign`의 고정 이미지 (Application 쪽 데이터 아님) |
| ❌ 발행처 로고 | `CardDesign`의 고정 이미지 |
| ❌ 직인 | `Application.logo/seal` 또는 `CardDesign`에서 렌더링 시 합성 |
| 학번 (학생증 전용) | `ApplicationMember.student_id` — ✅ 2026-07-31 신규 |
| 학과 (학생증 전용) | `ApplicationMember.department` — ✅ 2026-07-31 신규 |
| 학교 로고/직인 (학생증 전용) | `Application.logo_file_id`/`seal_file_id` — ✅ 2026-07-31 신규, 개인/단체 무관 |

> ✅ 2026-07-25 확인: 캐릭터(띠)는 **저장 컬럼을 두지 않음** — `birth_date`에서 렌더링 시점에 계산되는 파생값. (`character_image_file_id` 같은 FK 컬럼 제거)
> 참고로 백엔드에 이미 있는 `infra/card/ZodiacCalculator.java`(생년도 → 띠 키 계산)가 이 계산 로직과 같은 역할 — 재사용 가능해 보임.

---

## 7. 확정된 정책 (2026-07-25)

| 항목 | 결정 |
|---|---|
| 회원가입/로그인 방식 | **OAuth(Google/Naver) 정책이 맞음.** 프론트 `SignupPage.tsx`의 이메일/비밀번호 폼은 정책과 다르므로 추후 OAuth 방식으로 교체 필요 |
| `status` / `payment_status` | **분리가 맞음.** `adminMock.ts`의 `PAYMENT_PENDING`처럼 결제 상태를 `status`에 우겨넣는 방식은 폐기, `payment_status`(WAITING/CONFIRMED) 별도 컬럼 사용 |
| 결제 금액 | **필요함.** `Application.total_price`, `CardType.price` 둘 다 실제로 써야 하는 필드 — 프론트에 지금 전혀 없는 것이 문제이니 추가 필요 |
| 신청인 주소 | Applicant 엔티티엔 필요하지만, **입력 폼은 수령인 쪽에만** 만든다 (위 2.2절 참고) |
| `issue_type` | **Application 테이블에 있어야 함.** 정규화 과정에서 빠졌던 것으로 확인, 위 2.1절에 반영함 |
| 사주 기반 작명(한자이름/이름풀이/캐릭터) | `KoreanName`과 유사한 개념으로, **관리자가 신청 후 별도로 채워주는 절차.** 사용자가 신청 폼에서 직접 입력하지 않음. **다만 이 관리자 작명 화면은 `AdminPage.tsx`에 아직 전혀 구현되어 있지 않음** — 확인된 미구현 상태, 추후 신규 작업 필요 |
| 사주 작명 프로그램 (2026-07-25, 솔하 공유) | 솔하 님이 별도 웹앱 제작 완료 (trycloudflare 임시 터널). `saju-input-data.xlsx`(영문이름/국적/생년월일/출생시각/출생지역/성별 — `APPLICATION.md`의 사주 입력 필드와 동일) 업로드 → 이름 후보 목록 표시 → 선택. **이미지(카드) 기능은 카드 데이터 나오면 추가 예정.** 자동화 아님 — 관리자가 후보 중 직접 선택하는 방식. **`AdminPage.tsx`와의 연동 방식(수동 참고 / API 연동)은 2026-07-25 기준 보류 — 추후 사장님/솔하 님과 논의 후 결정** |
| `card_design_id` 소속 | **Application 컬럼으로 확정.** ⚠️ 2026-07-31 재정정: "신청 1건당 디자인 1개 선택"은 취소 — **사용자가 아니라 관리자가 배정**(생성 시 NULL, 아래 신규 행 참고) |
| 십이간지 캐릭터 | **저장하지 않음.** `card_design_id`와 별개 개념 — 렌더링 시 `ApplicationMember.birth_date` 기반으로 계산해 `CardFieldDefinition`(CHARACTER)에 합성 |
| Applicant/Receiver 법인정보 | **둘 다 `organization_name`, `department` 추가 확정.** 개인 신청(INDIVIDUAL)은 NULL, 법인/단체 신청(GROUP)에서만 사용 |
| 로고/직인/제출ZIP 연결 방식 | **Application에 `logo_file_id`/`seal_file_id`/`submit_file_id` 역할별 FK 3개로 확정.** 조인 테이블 방식 아님 |
| `application_number` | **Application에 컬럼 추가 확정.** 서버 생성, UNIQUE. 신청 조회·고객센터 문의 시 사용하는 사용자 대면 식별자. **신청 조회는 기본적으로 `application_number` + 연락처 조합으로 진행하는 방향** — `/lookup` 페이지 검토 시 이 전제로 확인 |
| `ApplicationMember` 원칙 | **"카드 1장 = ApplicationMember 1명"으로 확정, 개인/단체 공통.** 개인 신청: Application 1 + ApplicationMember 1. 단체 신청: Application 1 + ApplicationMember N. `card_number`도 개인/단체 관계없이 항상 여기 저장(Application이 아님). 컬럼 상세는 2.4절 |
| 공지사항 관리 방식 | **게시판(Post류)으로 같이 관리 확정.** 정적 하드코딩(`SupportPage.tsx`)에서 벗어나 관리자가 추가/수정 가능해야 함. 전용 `Notice` 엔티티인지 `Post` 재사용인지는 미정(8절) |
| FAQ 관리 방식 | **보류.** 지금은 정적 유지, 추후 재논의 |
| "1:1 문의" 기능 | **계획 없음.** `Inquiry` 등 별도 엔티티 추가 안 함 |
| 개인 신청 폼의 `birth_date` | **추가 확정.** `ApplicationMember.birth_date`가 개인/단체 공통 필수라서, 지금 입력란이 없는 개인 신청 폼(`StepInfo.tsx`)에 생년월일 입력란을 추가해야 함 — **프론트 미구현, 별도 작업 필요** (이번 문서 정리 범위에서는 코드 수정 안 함, 항목만 기록) |
| Applicant/Receiver ↔ ApplicationMember 관계 | **직접 연결 안 함으로 확정.** 신청인/수령인은 Application에 속하는 정보이고, 서로 다른 사람일 수 있음(사용자가 신청 시 직접 입력해서 결정) |
| 이름 뜻 vs 풀이 | **별도 컬럼으로 확정.** `name_meaning`(짧은 의미) / `name_interpretation`(긴 설명, 가안 명칭) — 재사용 아님 |
| 신청 이메일 vs 가입 이메일 | **정정: 같아야 함.** `Applicant.email`은 자유 입력이 아니라 로그인 계정(`User.email`)과 일치해야 하는 제약 |
| `photo_file_id` 연결 방식 | **`ApplicationMember.photo_path`(경로 텍스트)로 확정.** `UploadFile` FK 아님 — 신청 삭제 시 사진도 함께 삭제되는 생명주기와 자연스럽게 맞음 |
| 개인 신청 폼의 사진 업로드 | **추가 확정.** 개인 신청도 카드용 사진이 필요 — `StepInfo.tsx`/`StepFiles.tsx`에 입력란 추가 필요 (프론트 미구현, 별도 작업 필요) |
| `receiver_same_as_applicant` 중복 | **`Receiver.is_same_as_applicant` 제거.** `Application.receiver_same_as_applicant`만 유지, 체크 시 `Applicant` 정보를 `Receiver`로 복사하는 구조 |
| `Receiver.country` | **보류.** 해외 배송 지원 여부 미정. 지원 안 하기로 결정되면 컬럼 자체를 제거하는 방향으로 재검토 |
| `CardFieldDefinition.font_color` 이후 필드 | **보류.** 원본 자료 자체가 없음, 추가 정보 오면 이어서 정리 |
| 회원탈퇴 방식 | **소프트 삭제(7일 유예) → 완전탈퇴(익명화) 2단계 확정.** 사용자 액션은 "탈퇴하기" 하나, 유예기간 내 재로그인 시 자동 복구. `User` 컬럼(`status`/`withdrawal_requested_at`/`anonymized_at`) 추가 확정 |
| 카드 디자인 배정 주체 (2026-07-31) | **사용자가 아니라 관리자가 배정.** `Application.card_design_id`는 신청 생성 시 항상 NULL, `card_type_id`(카드종류)만 사용자가 선택. 배정 시점은 [TBD] |
| 학생증(STUDENT) 추가 항목 (2026-07-31) | **학번/학과(`ApplicationMember`, 인당) + 학교로고/직인(`Application.logo_file_id`/`seal_file_id` 재사용, 신청당 1회) 확정.** 개인/단체 신청 무관하게 항상 적용 — `CardType.code=STUDENT`로 판별 |
| 한국입국날짜(`entry_date`) (2026-07-31) | **`ApplicationMember`에 신규 추가, Nullable.** 단체 신청은 엑셀 상단 "공통값" + 행별 "개별 예외값" 2단 구조, 개별값이 있으면 우선 적용 — DB엔 해석된 최종값만 저장 |
| `birth_time`/`birth_region` 필수 여부 (2026-07-31) | **재정정: NOT NULL → NULL(선택 입력)로 변경.** 출생시간은 "모릅니다" 체크 시 미입력 허용 |
| 신청자 이메일/전화번호 소속 (2026-07-31) | **개인 신청은 `Applicant`만 사용(`ApplicationMember.email`/`phone`은 NULL), 단체 신청은 `ApplicationMember`에 엑셀 행별로 저장.** `Applicant.email`/`phone`은 단체의 "대표 신청인" 연락처로 별개 유지 |

## 8. 미해결 / 확인 필요 사항 (남은 것)

- **`Post` 필드 전체** — 도입부만 있고 테이블 없음. 원본 자료 없어 보류.
- **공지사항이 전용 `Notice` 엔티티인지 `Post`(행사·사업)를 재사용하는지** — 게시판으로 관리하는 것까지는 확정(5절), 구체적 엔티티 형태는 미정
- **`Receiver.country`** — 해외 배송 지원 여부 결정 대기 중 (보류)
- **CardDesign/CardFieldDefinition의 좌표 기반 합성 로직** — 프론트 `cards.ts`/`CardPreviewPanel`은 정적 이미지만 보여줄 뿐 아직 구현 안 됨 (별도 작업 필요, 정책 확인 대상은 아님).
- **카드번호(`ApplicationMember.card_number`) 채번 로직** — 형식(`ROK-XXXXX-XXXX`)은 확정, 순차 발급 vs 무작위 생성은 미정 (2.4절 본문에 inline으로 있었는데 이 목록에 누락되어 있었음 — 2026-07-31 정합성 점검 중 추가)
- **refresh 토큰 rotation용 세션 저장소** — DB 테이블(`refresh_token_sessions`)로 유지할지 Redis만 쓸지 미정, 구현 단계에서 결정 가능 (`API-명세.md` User 도메인 TODO와 동일 항목)
- **`issue_type=MOBILE_AND_PHYSICAL`의 실물배송(SHIPPING/DELIVERED) 흐름** — 이번 Admin 도메인 설계 범위 밖, 추후 별도 설계 (`API-명세.md` Admin 도메인과 동일 항목)
- **`CardDesign` 배정 시점** — 관리자가 신청 접수 직후/사진검토 통과 후/작명 단계 중 언제 배정하는지 (`APPLICATION-사용자명세.md` 6절)
- **학번/학과 형식 제약** — 원본 요구사항에 글자수 등 세부 스펙 없음
- **학생증 디자인 시안** — 아직 미도착
- **신청조회(lookup) 전화번호 인증 vs 이메일 인증 조합** — 둘 다 필수인지 하나만이면 되는지 (`APPLICATION-사용자명세.md` 3절)
- **단체 신청 엑셀 파싱 실패율 룰** — 옛 백엔드의 "30% 룰"을 새 설계에도 적용할지 미정
- **신청 내용(카드종류/인적사항 등) 수정 API 필요 여부** — 현재는 반려 시 사진 재업로드만 가능, 그 외 수정 경로 없음

---

## 9. 부록 — 초기 요약 초안 (참고용)

혜원 님이 위 정규화된 엔티티들과 별도로, 더 단순화된 형태로 공유했던 목록입니다.
본문(Application/Applicant/Receiver/UploadFile)과 **필드명·구조가 다르므로 그대로 쓰지 말고 참고만** 하세요.
(2026-07-25 기준: `issue_type` 관련 불일치는 해결됨 — 본문 2.1절에 반영. `payment_status`/`total_price`/`receiver_same_as_applicant`가 이 초안에 없는 것은 이 초안이 더 단순화된 요약이었기 때문으로 확인.)

| 필드 | 설명 |
|---|---|
| application_type | 개인 신청 / 법인·단체 신청 |
| card_type_id | 명예한국인증, 학생증, 방문증 등 |
| card_design_id | 선택한 디자인 |
| issue_type | 모바일 / 모바일+실물 |
| quantity | 신청 수량 |
| applicant_name | 신청인 이름 |
| applicant_phone | 신청인 연락처 |
| applicant_email | 신청인 이메일 |
| logo_file_id | 업로드한 로고 |
| seal_file_id | 업로드한 직인 |
| submit_file_id | 제출한 ZIP(엑셀+사진) |
| status | 신청 상태 |


