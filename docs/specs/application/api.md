## Application 도메인

### ① 도메인의 책임

카드 제작 신청의 생성·조회를 담당한다. 신청 유형(개인/법인단체), 카드 종류/디자인, 발급 방식(모바일/실물), 신청인·수령인 정보, 카드 1장 단위(`ApplicationMember`)를 포괄한다. (`.md` 2절 기준)

> 스코프 참고: 이번 패스는 **사용자가 신청을 만들고 조회하는 흐름**만 다룹니다. 관리자가 상태를 바꾸거나 카드를 발급하는 쪽(`AdminPage.tsx`)은 별도 Admin 도메인으로 분리해서 나중에 다룹니다 — 한 번에 여러 도메인 안 만든다는 규칙 때문입니다.

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `ApplyPage.tsx` | 5단계(유형→정보→파일→확인→완료) 오케스트레이션. 서버 호출 없음 — 전 단계를 `sessionStorage`(`useApplicationDraft.ts`)에 모아뒀다가 마지막에 한 번에 제출하는 구조(중간 저장 API 불필요) |
| `StepType.tsx` | 개인/법인단체 선택 + 사전상담 확인 체크(게이트용, 저장 안 함) |
| `StepInfo.tsx` | 발급유형, 수량, 신청인 정보, (실물일 때만) 수령인 정보 — **`applicantType`에 따른 분기가 이 필드들엔 없음(신청인/수령인 정보는 개인·법인 공통 폼)** |
| `StepFiles.tsx` | 로고/직인/제출ZIP 3개 업로드 — **`applicantType` 상관없이 항상 3개 다 보여줌** |
| `StepComplete.tsx` | 입금자명 입력 + 신청번호 표시 |
| `LookupPage.tsx` | 신청번호/카드번호 + 연락처로 조회 (비로그인도 가능, README에 `POST /api/applications/lookup`으로 이미 언급됨) |

### ③ 필요한 API 목록

1. **개인 신청 생성** — `StepReview`→제출
2. **단체(ZIP) 신청 생성** — 동일 흐름, `applicantType=GROUP`
3. **신청 조회** — `LookupPage.tsx`
4. ⚠️ TODO: **입금자명 등록** — `StepComplete.tsx`가 입금자명을 받는데, 이 값을 저장할 엔티티가 `.md`에 없음(Payment 엔티티 자체가 아직 없음, 이전에 "결제 필요함"으로만 확정되고 실제 테이블은 안 만들어짐). 이번 패스에선 설계 보류, 별도 확인 필요.

### API 1 / 3 — 개인 신청 생성 ⚠️ 확인필요 — `StepInfo.tsx`에 생년월일·국적·출생시각·출생지역·성별·사진 입력란 없음, 서버 호출 자체도 없음

#### ④ Request/Response 설계

```
POST /api/applications
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `photo` | file | `ApplicationMember.photo_path`용 — ⚠️ 프론트에 아직 업로드 입력란 없음(기존 확정 TODO) |
| `schoolLogo` | file | ⚠️ 2026-07-31 신규 — `cardTypeId`가 학생증일 때만 필수. `Application.logo_file_id` |
| `schoolSeal` | file (선택) | 학생증 전용. 전달된 경우 `Application.seal_file_id`로 저장 |

```json
{
  "cardTypeId": 1,
  "issueType": "MOBILE_AND_PHYSICAL",
  "orientation": "LANDSCAPE",
  "schoolType": "UNIVERSITY",
  "applicant": {
    "name": "홍길동",
    "email": "hong@example.com",
    "phone": "010-1234-5678"
  },
  "receiver": {
    "sameAsApplicant": false,
    "name": "김수령",
    "phone": "010-9999-8888",
    "zipCode": "06236",
    "address": "서울특별시 강남구 ...",
    "detailAddress": "101동 202호",
    "deliveryRequest": "부재 시 경비실"
  },
  "member": {
    "birthDate": "1990-05-15",
    "nationality": "US",
    "birthTime": "14:30",
    "birthRegion": "New York",
    "gender": "MALE",
    "entryDate": "2026-08-15",
    "studentId": "20261234",
    "department": "컴퓨터공학과"
  }
}
```

- ✅ 2026-08-07 정정(`APPLICATION.md` 기준): `applicant.email`은 요청에 **포함한다** — `Applicant.email`은 로그인 `User.email`을 기본값으로 프리필하되 신청 화면에서 수정할 수 있다(계정 `User.email` 자체는 바뀌지 않음, 이 신청 1건의 값만 저장).
- ✅ 2026-08-07 정정: `receiver`는 `issueType=MOBILE`이면 **전달하면 안 되며**(전달 시 `INVALID_INPUT`), `issueType=MOBILE_AND_PHYSICAL`이면 필수다.
- ⚠️ 2026-07-31 정정: **`cardDesignId` → `cardTypeId`로 교체.** 사용자는 카드 "종류"만 선택하고, 구체적 디자인은 관리자가 신청 검토 중 배정(`.md` 2.1절, `docs/specs/application/requirements.md` 6절) — `Application.card_design_id`는 생성 시 NULL
- `logo`/`seal`/제출ZIP(회사용)은 이 API에 없음 — 개인 신청은 법인 전용 요소라 불필요. 단, **학생증(`CardType.code=STUDENT`)은 예외로 `schoolLogo`가 필수이고 `schoolSeal`은 선택**이다.
- ⚠️ 2026-07-31 재정정: `member.birthTime`/`birthRegion`은 **선택 입력으로 정정**(NOT NULL이었던 걸 Nullable로 변경, "출생시간 모름" 체크 지원). `nationality`/`gender`/`birthDate`는 계속 필수
- ✅ 2026-07-31 신규: `member.entryDate`(한국입국날짜, 선택) 추가
- ✅ 2026-08-14 확정(값은 대문자 문자열, `gender`와 동일 관례 — 프론트가 내부적으로 소문자를 쓰더라도 전송 직전 `.toUpperCase()` 필요): `orientation`(`LANDSCAPE`/`PORTRAIT`, 가로형/세로형)과 `schoolType`(`UNIVERSITY`/`HIGH_SCHOOL`, 대학교/고등학교)을 최상위 필드로 신규 추가. 둘 다 `cardTypeId`가 학생증일 때만 필수이고 그 외 카드종류는 반드시 생략해야 한다.
- ✅ 2026-08-14 조건 변경: `member.studentId`/`department`는 더 이상 "학생증이면 무조건 필수"가 아니라 **`schoolType=UNIVERSITY`일 때만** 필수다(최대 10자·숫자만 허용은 기존과 동일). `schoolType=HIGH_SCHOOL`이면 오히려 `studentId`/`department`를 보내면 안 된다(보내면 `INVALID_INPUT`). 이전 문서의 "학생증이면 무조건 필수" 서술(2026-08-07 정정분)은 이 조건으로 대체됨.

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationNumber": "APP-2026-000123",
    "status": "SUBMITTED",
    "paymentStatus": "WAITING",
    "createdAt": "2026-07-29T10:00:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 로그인 User가 존재하지 않음 | `USER_NOT_FOUND` | 404 |
| User 상태가 `ACTIVE`가 아님 | `ALREADY_WITHDRAWN` | 409 |
| User 권한이 `USER`가 아님 | `FORBIDDEN` | 403 |
| 필수 약관에 모두 동의하지 않음 | `TERMS_NOT_AGREED` | 403 |
| `cardTypeId`가 없거나 `is_active=false` | `NOT_FOUND` | 404 |
| 얼굴사진 또는 학교 로고·직인이 5 MiB를 초과함 | `FILE_TOO_LARGE` | 413 |
| 얼굴사진 또는 학교 로고·직인의 확장자/MIME가 허용되지 않음 | `UNSUPPORTED_FILE_TYPE` | 415 |
| 파일 signature 불일치 또는 이미지 디코딩 실패 | `INVALID_IMAGE` | 400 |
| EXIF Orientation 적용 후 얼굴사진 해상도가 300×400 미만 | `INVALID_IMAGE` | 400 |
| `issueType=MOBILE_AND_PHYSICAL`인데 `receiver` 없음 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-07 신규: `issueType=MOBILE`인데 `receiver` 전달 | `INVALID_INPUT` | 400 |
| `member.birthDate`/`nationality`/`gender` 중 하나라도 누락, `photo` 파일 누락 | `INVALID_INPUT` | 400 |
| `cardTypeId`가 학생증인데 `orientation`/`schoolType`/`schoolLogo` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| `cardTypeId`가 학생증 + `schoolType=UNIVERSITY`인데 `studentId`/`department` 중 하나라도 누락, 또는 학번이 10자 초과·숫자 외 문자 포함 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-14 신규: `cardTypeId`가 학생증 + `schoolType=HIGH_SCHOOL`인데 `studentId`/`department` 중 하나라도 있음 | `INVALID_INPUT` | 400 |
| `cardTypeId`가 학생증이 아닌데 `orientation`/`schoolType`/`studentId`/`department`/`schoolLogo`/`schoolSeal`을 보냄 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-16 신규: 같은 사용자가 오늘(KST) 이미 3건 신청(개인·단체 합산, 취소분 제외) | `APPLICATION_LIMIT_EXCEEDED` | 429 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- ✅ 2026-08-07 정정: `receiver.sameAsApplicant=true`여도 우편번호와 기본주소는 필수이며, 이름과 연락처만 복사된 기본값을 서버가 채우고 사용자가 수정할 수 있다.
- `member.birthTime`/`birthRegion`/`entryDate`는 선택이라 누락돼도 통과(2026-07-31 정정)
- ✅ 2026-07-29 확인: `quantity`는 요청에 없음 — 개인 신청은 `total_quantity=1` 서버 고정, 클라이언트가 보낼 필요 없음

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| cardTypeId | Application.card_type_id (⚠️ 2026-07-31 정정 — 기존 `cardDesignId` 대체) |
| — | Application.card_design_id = `NULL`(관리자가 이후 배정, ⚠️ 2026-07-31 정정) |
| issueType | Application.issue_type |
| orientation | Application.orientation (✅ 2026-08-14 신규, 학생증 전용) |
| schoolType | Application.school_type (✅ 2026-08-14 신규, 학생증 전용) |
| applicant.name/phone | Applicant.name/phone |
| applicant.email | Applicant.email (✅ 2026-08-07 정정 — 요청값 저장, `User.email`을 기본값으로 프리필하되 수정 가능) |
| receiver.* | Receiver.* (receiver.name/phone → `Receiver.receiver_name`/`receiver_phone` 컬럼명 매핑 주의) |
| receiver.sameAsApplicant | Application.receiver_same_as_applicant |
| member.birthDate | ApplicationMember.birth_date |
| member.nationality | ApplicationMember.nationality |
| member.birthTime | ApplicationMember.birth_time (⚠️ 2026-07-31 Nullable로 정정) |
| member.birthRegion | ApplicationMember.birth_region (⚠️ 2026-07-31 Nullable로 정정) |
| member.gender | ApplicationMember.gender |
| member.entryDate | ApplicationMember.entry_date (✅ 2026-07-31 신규) |
| member.studentId | ApplicationMember.student_id (✅ 2026-08-07 정정, 최대 10자·숫자만. ⚠️ 2026-08-14 조건 변경: 학생증+`schoolType=UNIVERSITY`일 때만 필수) |
| member.department | ApplicationMember.department (✅ 2026-07-31 신규. ⚠️ 2026-08-14 조건 변경: 학생증+`schoolType=UNIVERSITY`일 때만 필수) |
| photo(file) | ApplicationMember.photo_path |
| schoolLogo(file) | UploadFile 생성 → Application.logo_file_id (✅ 2026-07-31 신규, 학생증 전용) |
| schoolSeal(file) | 선택. 전달된 경우 UploadFile 생성 → Application.seal_file_id |
| — | ApplicationMember.email/phone = `NULL`(개인 신청은 항상 비움 — `Applicant`가 대신함, ✅ 2026-07-31 확정) |
| — | Application.application_type = `INDIVIDUAL` (고정) |
| — | Application.total_quantity = `1` (✅ 확정, `.md` 2.1절 반영) |
| — | 결제 금액을 계산하거나 저장하지 않음. 상담 후 신청하고 신청 이후 별도 계좌이체 |
| — | Application.status = `SUBMITTED`, payment_status = `WAITING` (모든 신청 생성 경로의 기본값) |
| — | Application.application_number = 서버 생성 |
| — | ApplicationMember 1건 자동 생성(개인 원칙) |

#### ⑦ 누락된 필드 확인 / 질문

**해결됨 (2026-07-29):**
1. 개인 신청은 `quantity=1` 고정으로 확정 (위 반영).
2. `StepFiles.tsx`가 신청 유형과 무관하게 로고/직인/제출ZIP을 항상 보여주는 건 **프론트가 아직 이 설계를 못 따라간 상태로 확인** — 개인 신청 시엔 이 3개를 숨기고 생년월일·사진 입력란을 보여주는 쪽으로 프론트를 고쳐야 함. 프론트 미구현 TODO로 기록(`birth_date`/사진 업로드 TODO와 같은 묶음).

⚠️ **재정정 (2026-07-31, DB.md와 정합성 점검 중 발견):** 위 "해결됨" 시점(07-29) 이후, `.md` 2.4절에서 `ApplicationMember.nationality`/`birth_time`/`birth_region`/`gender`가 **NOT NULL로 신규 확정**됐는데 이 API 설계엔 반영이 안 되어 있었음 — 그대로 두면 `ApplicationMember` 저장 시 NOT NULL 위반. `member` 요청 필드 4개 추가로 정정함(위 반영).

⚠️ **재정정 2 (2026-07-31, `docs/specs/application/requirements.md` 기준 반영):** `cardDesignId`(사용자 선택) → `cardTypeId`(카드종류만 선택, 디자인은 관리자 배정)로 교체, `birthTime`/`birthRegion` 필수→선택 전환, `entryDate`/학생증 전용 필드(`studentId`/`department`/`schoolLogo`/`schoolSeal`) 신규 추가.

**API 1 완료.**

---

### API 2 / 3 — 단체(ZIP) 신청 생성 ⚠️ 확인필요 — 필드 구성은 프론트와 대체로 맞으나 실제 서버 호출 없음(전부 sessionStorage에만 보관)

#### ④ Request/Response 설계

```
POST /api/applications/bulk
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `request` | JSON | 아래 |
| `logo` | file | `Application.logo_file_id` — 일반 카드종류는 회사 로고, 학생증은 학교 로고 |
| `seal` | file (조건부) | 일반 카드종류는 기관 직인으로 필수, 학생증은 학교 직인으로 선택 |
| `submitFile` | file (ZIP) | `Application.submit_file_id` — 엑셀과 구성원 사진 묶음 |

```json
{
  "cardTypeId": 1,
  "issueType": "MOBILE_AND_PHYSICAL",
  "orientation": "LANDSCAPE",
  "schoolType": "UNIVERSITY",
  "applicant": {
    "organizationName": "OO기업",
    "department": "인사팀",
    "name": "홍길동",
    "email": "hong@example.com",
    "phone": "010-1234-5678"
  },
  "receiver": {
    "sameAsApplicant": false,
    "organizationName": "OO기업",
    "department": "총무팀",
    "name": "김수령",
    "phone": "010-9999-8888",
    "zipCode": "06236",
    "address": "서울특별시 강남구 ...",
    "detailAddress": "101동 202호",
    "deliveryRequest": "부재 시 경비실"
  }
}
```

- ✅ 2026-08-07 정정: `applicant.email`은 API 1과 동일하게 요청값으로 받는다(`User.email`을 기본값으로 프리필하되 수정 가능).
- ✅ 2026-08-07 정정: Receiver 규칙은 API 1과 같다 — `MOBILE`에서는 전달 금지(`INVALID_INPUT`), `MOBILE_AND_PHYSICAL`에서는 필수이며 `sameAsApplicant=true`면 이름·연락처만 복사한다.
- `member`(개인 신청의 `birthDate` 등)는 이 요청에 없음 — 인원별 정보는 ZIP 안 엑셀에서 옴
- ⚠️ 2026-07-31 정정: `cardDesignId` → `cardTypeId`로 교체(API 1과 동일 이유 — 디자인은 관리자 배정)
- ✅ 2026-08-14 확정: `orientation`(`LANDSCAPE`/`PORTRAIT`)은 API 1과 동일하게 `cardTypeId`가 학생증일 때만 필수이고 신청서 전체에 1개다(엑셀 컬럼이 아니라 이 요청의 최상위 필드). **`schoolType`(`UNIVERSITY`/`HIGH_SCHOOL`)도 동일하게 신청서 전체에 1개로 신규 추가되지만, `studentId`/`department` 필수 여부에는 영향을 주지 않는다** — 단체는 여전히 학번·학과를 엑셀에서만 받고(§엑셀 템플릿 컬럼 표 참고, 변경 없음), 이 값은 두 카드종류(학생증/비학생증) 구분에만 쓰인다.

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "applicationId": 2,
    "applicationNumber": "APP-2026-000124",
    "status": "SUBMITTED",
    "paymentStatus": "WAITING",
    "totalQuantity": 42,
    "createdAt": "2026-07-29T10:05:00"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `submitFile`이 ZIP이 아니거나 손상됨 | `INVALID_ZIP` | 400 |
| ✅ 2026-08-07 정정: ZIP 검증 오류(엑셀 없음/형식 오류/ID·사진 중복·누락·매핑불가 등)가 하나 이상 발생 | `BULK_APPLICATION_VALIDATION_FAILED` + `errors[]` | 400 |
| `cardTypeId` 없음/비활성 | `NOT_FOUND` | 404 |
| 일반 단체 신청에서 `logo` 또는 `seal` 누락 | `INVALID_INPUT` | 400 |
| 학생증 단체 신청에서 `logo` 누락 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-14 신규: `cardTypeId`가 학생증인데 `orientation`/`schoolType` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-14 신규: `cardTypeId`가 학생증이 아닌데 `orientation`/`schoolType`을 보냄 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-16 신규: 같은 사용자가 오늘(KST) 이미 3건 신청(개인·단체 합산, 취소분 제외) | `APPLICATION_LIMIT_EXCEEDED` | 429 |

✅ 2026-08-07 확정(`APPLICATION.md` 기준): 오류 하나라도 발생하면 **부분 성공 없이 신청 전체를 실패 처리**하고, 상세 오류를 `errors[]`(행 번호·필드·코드·메시지)로 함께 반환한다. 옛 "실패율 30% 룰"은 폐기(Legacy). 기존 `EXCEL_NOT_FOUND`/`EXCEL_PARSE_ERROR`/`ZIP_TOO_LARGE`는 `BULK_APPLICATION_VALIDATION_FAILED`로 흡수되며 개별 errorCode로는 더 쓰지 않는다. ZIP 최대 크기·Excel 최대 행 수·최대 신청 인원은 현재 제한하지 않는다([TBD], `PENDING_DECISIONS.md`).

사진 매칭 세부 오류 code는 다음을 사용한다: 엑셀 ID 중복은 `DUPLICATE_ID`, 동일 ID 사진 중복은 `PHOTO_DUPLICATE`, 엑셀 ID와 매칭되지 않는 여분 사진은 `PHOTO_UNMATCHED`, 엑셀 ID에 대응하는 사진 누락은 `PHOTO_NOT_FOUND`.

- 엑셀 행 수만큼 `ApplicationMember`가 생성됨 → `total_quantity`는 서버가 엑셀 행 수를 세서 채움(클라이언트가 안 보냄)
- ⚠️ 2026-07-31 정정: `cardDesignId` → `cardTypeId`로 교체(API 1과 동일 이유)
- ✅ 2026-07-31 신규: 엑셀에 개별입국날짜/이메일/전화번호/(학생증이면)학번·학과 컬럼 추가 — 아래 엑셀 템플릿 표 참고

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| cardTypeId | Application.card_type_id (⚠️ 2026-07-31 정정 — 기존 `cardDesignId` 대체) |
| — | Application.card_design_id = `NULL`(관리자가 이후 배정, ⚠️ 2026-07-31 정정) |
| issueType | Application.issue_type |
| orientation | Application.orientation (✅ 2026-08-14 신규, 학생증 전용) |
| schoolType | Application.school_type (✅ 2026-08-14 신규, 학생증 전용 — studentId/department 필수 여부와는 무관) |
| applicant.* | Applicant.* (organizationName/department 포함) |
| receiver.* | Receiver.* (organizationName/department 포함, receiver.name/phone → `receiver_name`/`receiver_phone` 컬럼명 매핑 주의) |
| receiver.sameAsApplicant | Application.receiver_same_as_applicant |
| logo(file) | UploadFile 생성 → Application.logo_file_id |
| seal(file) | UploadFile 생성 → Application.seal_file_id |
| submitFile(file) | UploadFile 생성 → Application.submit_file_id |
| 엑셀 각 행 | ApplicationMember N건 — 생성 시 채움: `english_name`/`birth_date`/`nationality`/`birth_time`/`birth_region`/`gender`/`entry_date`/`email`/`phone`/`address`/`photo_path`, (학생증이면)`student_id`/`department`. `name`/`chinese_name`/`name_meaning`/`name_interpretation`/`card_number`/`issue_date`는 NULL로 시작(관리자가 나중에 채움) |
| — | Application.application_type = `GROUP` (고정) |
| — | Application.total_quantity = 엑셀 행 수 |
| — | 결제 금액을 계산하거나 저장하지 않음. 상담 후 신청하고 신청 이후 별도 계좌이체 |

**엑셀 템플릿 컬럼 (⚠️ 2026-07-31 재정정 — `docs/specs/application/requirements.md` 기준)**

표 위 별도 셀에 **"공통 입국날짜"** 1개 값(선택 입력)을 두고, 표 안 "개별입국날짜"는 예외자만 입력 — 비어있으면 공통값 적용, 채워져 있으면 개별값 우선(해석된 최종값만 `entry_date`에 저장, 상세 로직은 `.md` 2.4절/`docs/specs/application/requirements.md` 2-3절).

```
공통 입국날짜 : 2026-08-15   (선택 입력)
```

| 컬럼 | ApplicationMember 필드 | 필수 여부 |
|---|---|---|
| ID | 사진 파일명 매칭용 임시 식별자. 별도 사진 파일 ID를 생성하거나 DB에 저장하지 않음 | 필수 |
| 영문명 | english_name | 필수 |
| 생년월일 | birth_date | 필수 |
| 국적 | nationality | 필수 |
| 출생시간 | birth_time | 선택(⚠️ 2026-07-31 Nullable로 정정) |
| 출생지역 | birth_region | 선택(⚠️ 2026-07-31 Nullable로 정정) |
| 성별 | gender | 필수 |
| 개별입국날짜 | entry_date(상단 공통값과 조합해 해석) | 선택 |
| 이메일 | email(✅ 2026-07-31 신규 — 행마다 다른 신청자 본인 이메일) | 필수 |
| 전화번호 | phone(✅ 2026-07-31 신규 — 행마다 다른 신청자 본인 연락처) | 필수 |
| 주소 | address | 선택 |
| 학번 | student_id(✅ 2026-08-07 정정, `cardTypeId`가 학생증일 때만 존재, 최대 10자·숫자만) | 학생증만 필수 |
| 학과 | department(✅ 2026-07-31 신규, 학생증 전용) | 학생증만 필수 |
| (사진 파일명, ZIP 루트) | photo_path | 필수 |

✅ 2026-08-18 정정(`APPLICATION.md` 기준): ZIP 루트에는 자유로운 파일명의 `.xlsx` Excel 1개만 허용한다(2개 이상이면 전체 실패). 공식 양식의 A열은 `사진 번호`이며 4~103행에 문자열 `001`~`100`이 미리 입력되고 잠금 처리된다. 사용자는 이 열을 입력·수정하지 않는다. 사진 번호만 있는 행은 빈 행으로 무시하고 B열 이후 신청자 정보가 하나라도 입력된 행만 검증·처리한다. ZIP 루트 사진은 실제 처리 행의 사진 번호와 정확히 매칭하며, 빈 행 번호의 사진은 `PHOTO_UNMATCHED` 여분 사진으로 전체 실패한다. 사진 확장자는 대소문자를 구분하지 않는다(예: `001` ↔ `001.jpg`/`001.JPG`). 구성원별 UploadFile ID는 생성하지 않고 매칭된 이미지의 저장 경로만 `ApplicationMember.photo_path`에 저장한다. ZIP 원본의 `Application.submit_file_id`는 신청 단위 제출 파일을 가리키며 구성원 사진 ID가 아니다.

#### ⑦ 누락된 필드 확인

⚠️ **재정정 (2026-07-31, DB.md와 정합성 점검 중 발견):** 원래 "없음"으로 완료 처리했던 07-29 시점 이후 `.md` 2.4절에서 `nationality`/`birth_time`/`birth_region`/`gender`가 NOT NULL로 신규 확정되어, 엑셀 템플릿에 4개 컬럼을 추가로 반영했었음.

⚠️ **재정정 2 (2026-07-31, `docs/specs/application/requirements.md` 기준):** `cardDesignId`→`cardTypeId` 교체, `birthTime`/`birthRegion` 필수→선택 전환, 개별입국날짜(+상단 공통값)/이메일/전화번호/학번/학과 컬럼 신규 추가.

**API 2 완료.**

---

### API 3 / 3 — 신청 조회 ⚠️ 확인필요 — `LookupPage.tsx`는 mock 데이터 표시뿐, `statusLabels`도 옛 enum 사용 중

#### ④ Request/Response 설계

```
POST /api/applications/lookup
Content-Type: application/json
```
(로그인 불필요 — `LookupPage.tsx`는 비로그인 상태에서도 조회 가능한 공개 페이지)

```json
{
  "method": "card",
  "keyValue": "ROK-35777-2105",
  "phone": "010-1234-5678",
  "email": "hong@example.com"
}
```
| 필드 | 설명 |
|---|---|
| method | `"application"`(신청번호) \| `"card"`(카드번호) |
| keyValue | 신청번호 또는 카드번호 |
| phone / email | ⚠️ 2026-07-31 재정정: 본인 인증 채널 — `phone`/`email` 중 **최소 1개 필수**(정확히 둘 다 필수인지, 하나만 있어도 되는지는 [TBD], 우선 "하나 이상"으로 설계). 대조 대상 엔티티는 아래 ⑤ 참고 |

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationType": "INDIVIDUAL",
    "applicationNumber": "APP-2026-000123",
    "applicantNameMasked": "이*하",
    "cardType": "명예한국인증",
    "status": "PHOTO_REJECTED",
    "photoRejectReason": "사진이 흐려서 식별이 어렵습니다. 선명한 사진으로 다시 올려주세요.",
    "submittedAt": "2026-07-15"
  }
}
```
✅ 2026-07-31 추가: `status=PHOTO_REJECTED`일 때 `photoRejectReason` 노출 — 사진 재업로드 흐름(API 4)에서 사용. `applicationId`도 추가(재업로드 API 호출 시 필요).

**Response `404 Not Found`** — 번호+연락처 조합이 안 맞음 (존재 여부를 굳이 구분해서 알려주지 않음 — 개인정보 보호)
```json
{ "success": false, "data": null, "errorCode": "NOT_FOUND", "errorMessage": "데이터를 찾을 수 없습니다." }
```

#### ⑤ Validation

- `method=card`일 때 `keyValue`(`ApplicationMember.card_number`)로 검색 → 그 카드가 속한 `Application`을 찾음 → ✅ 2026-07-29 확인: **`Application` 전체(단체 신청 전체)의 진행상태를 반환.** 그 카드 1장만의 상태가 아님
- ⚠️ 2026-07-31 재정정 — 본인 인증 대조 대상:
  - `method=card`(카드번호 조회): 그 카드의 실제 소유자, 즉 **`ApplicationMember.phone`/`email`**과 대조(개인 신청은 이 두 컬럼이 NULL이므로 자연히 `Applicant`를 대신 참조)
  - `method=application`(신청번호 조회): 기존대로 **`Applicant.phone`/`email`**과 대조(신청 대표자 기준)
- `phone`/`email` 둘 다 없으면 `INVALID_INPUT`(400)
- 번호+연락처(또는 이메일) 조합이 안 맞으면 `NOT_FOUND`(404) — 신청번호는 맞는데 인증 정보만 틀렸어도 동일하게 404(존재 여부 구분 안 함)

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| applicationId | Application.id |
| applicationType | Application.application_type (`INDIVIDUAL`, `GROUP`) |
| applicationNumber | Application.application_number |
| applicantNameMasked | Applicant.name (마스킹 처리). ⚠️ 단체 신청은 이게 **신청 대표자**(예: 인사담당자) 이름이지, 카드번호로 조회한 그 개인(직원 등)의 이름이 아님 — 결과 화면에서 헷갈릴 수 있어 참고로 남김 |
| cardType | Application.card_type_id → CardType.name |
| status | Application.status — `SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED` 기준 |
| photoRejectReason | Application.photo_reject_reason (status=PHOTO_REJECTED일 때만) |
| submittedAt | Application.created_at |

#### ⑦ 누락된 필드 확인

⚠️ **재정정 (2026-07-31, `docs/specs/application/requirements.md` 기준):** 단체 신청에서 `ApplicationMember`가 자기 phone/email을 갖게 되면서, 카드번호 조회는 `Applicant`가 아니라 `ApplicationMember` 기준으로 인증하도록 대조 대상을 변경. 이메일 인증 채널도 추가.

**API 3 완료.**

---

### API 4 / 4 — 사진 재업로드 (2026-07-31 추가, 로그인 필수) ⚠️ 확인필요 — 프론트에 해당 화면 자체가 없음(신규)

#### ④ Request/Response 설계

```
PATCH /api/applications/{applicationId}/photo
Cookie: accessToken={JWT}
Content-Type: multipart/form-data
```

| part | 타입 | 설명 |
|---|---|---|
| `photo` | file | `application_type=INDIVIDUAL`일 때 — 새 사진 1장 |
| `submitFile` | file (ZIP) | `application_type=GROUP`일 때 — 수정된 엑셀+사진 ZIP 전체 재제출 |

(둘 중 신청 유형에 맞는 파트 1개만 보냄)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "REVIEWING"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.user_id != 로그인한 유저` | `FORBIDDEN` | 403 |
| `Application.status != PHOTO_REJECTED` | `INVALID_STATUS_TRANSITION` | 400 |
| `application_type=INDIVIDUAL`인데 `submitFile`을 보냄(또는 반대) | `INVALID_INPUT` | 400 |
| `submitFile`이 ZIP 형식 오류 | `INVALID_ZIP` | 400 |

✅ 2026-07-31 확정: **신청 본인만 수정 가능** — 단체 신청도 대표 신청인(`Applicant`, `Application.user_id`) 본인만, 구성원 개인이 각자 수정하는 게 아님.

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| photo(file) | ApplicationMember.photo_path 갱신 (INDIVIDUAL) |
| submitFile(file) | UploadFile 재생성 → Application.submit_file_id 갱신, 엑셀 재파싱 → ApplicationMember 재생성/갱신 (GROUP) |
| — | Application.status: `PHOTO_REJECTED` → `REVIEWING` |
| — | Application.photo_reject_reason = NULL (초기화) |

#### ⑦ 누락된 필드 확인

없음.

**API 4 완료.**

---

### API 5 / 5 — 완성된 카드 다운로드 (2026-07-31 추가, 로그인 필수) ⚠️ 확인필요 — 프론트에 해당 화면 자체가 없음(신규)

#### ④ Request/Response 설계

```
GET /api/applications/{applicationId}/cards/download
Cookie: accessToken={JWT}
```

**Response `200 OK` — 개인(INDIVIDUAL)**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationType": "INDIVIDUAL",
    "cardFrontUrl": "https://.../APP-2026-000123-front.png",
    "cardBackUrl": "https://.../APP-2026-000123-back.png",
    "expiresAt": "2026-08-07T10:00:00"
  }
}
```

**Response `200 OK` — 단체(GROUP)**
```json
{
  "success": true,
  "data": {
    "applicationId": 2,
    "applicationType": "GROUP",
    "downloadUrl": "https://.../APP-2026-000124-cards.zip",
    "expiresAt": "2026-08-07T10:00:00"
  }
}
```

✅ 2026-07-31 정정: **개인은 이미지 URL 2장(앞/뒤)을 바로 반환 — ZIP으로 묶을 이유 없음.** 단체(N명분)만 ZIP으로 묶어서 반환. (기존 백엔드의 `CitizenCard` 다운로드 presigned URL 패턴은 유지)

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.user_id != 로그인한 유저` | `FORBIDDEN` | 403 |
| `Application.card_ready_at == null` | `CARD_NOT_READY` | 400 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| cardFrontUrl/cardBackUrl (개인) | `ApplicationMember.card_front_path`/`card_back_path` (1건뿐이라 바로 매핑) |
| downloadUrl (단체) | `ApplicationMember[].card_front_path`/`card_back_path` 전체를 묶어 ZIP 생성 후 presigned URL 발급 (매번 새로 묶는지, 발급 시 미리 만들어 캐싱하는지는 구현 세부사항) |
| expiresAt | presigned URL 만료 시각(예: 7일) |

#### ⑦ 누락된 필드 확인

없음.

**API 5 완료.**

---

### API 6 / 7 — 마이페이지 신청 목록 조회 (2026-08-06 추가, 로그인 필수) ✅ 2026-08-18 구현 완료

> 지금까지 있던 `POST /api/applications/lookup`(API 3)은 **비로그인 공개 조회**로, 신청번호/카드번호+연락처 조합으로 딱 1건만 제한된 필드로 보여준다. 로그인한 사용자가 "내가 지금까지 신청한 것들"을 목록으로 훑어보는 기능은 별개로 없었음 — 이번에 신규 추가.

#### ④ Request/Response 설계

```
GET /api/my/applications?page=0&size=20&status=
Cookie: accessToken={JWT}
```

| 쿼리 파라미터 | 설명 |
|---|---|
| page | 0부터 시작, 기본값 0 |
| size | 기본값 20, 상한 100 |
| status | 선택. `ApplicationStatus` 값 1개로 필터(예: `?status=COMPLETED`). 생략하면 전체 |

로그인한 사용자 본인의 신청만 반환한다(`Application.user_id = 로그인 userId`). 정렬은 `createdAt DESC` 고정.

**Response `200 OK`** — Review 도메인 설계에서 제안한 공용 `PageResponse<T>` 포맷 재사용(`docs/specs/review/api.md` §공통 참고, 이번에 실제로 만들면 첫 사용 사례가 됨).
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "applicationId": 1,
        "applicationNumber": "APP-2026-000123",
        "applicationType": "INDIVIDUAL",
        "cardTypeId": 1,
        "cardTypeName": "명예한국인증",
        "totalQuantity": 1,
        "status": "SUBMITTED",
        "paymentStatus": "WAITING",
        "createdAt": "2026-08-06T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

(마이페이지 신청 조회 API 제안 필드 구성과 동일 — `backend/FRONTEND_API_REQUIREMENTS.md` §5의 제안을 그대로 채택. 갭 요약은 `docs/BACKEND_API_GAPS.md` P0-2)

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `page`/`size` 음수 | `INVALID_INPUT` | 400 |
| `size` 100 초과 | `INVALID_INPUT` | 400 |
| `status` 값이 `ApplicationStatus` enum에 없음 | `INVALID_INPUT` | 400 |

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| applicationId | `Application.id` |
| applicationNumber | `Application.application_number` |
| applicationType | `Application.application_type` |
| cardTypeId/cardTypeName | `Application.card_type_id` → `CardType.id`/`name` |
| totalQuantity | `Application.total_quantity` |
| status/paymentStatus | `Application.status`/`payment_status` |
| createdAt | `Application.created_at` |

#### ⑦ 누락된 필드 확인

없음 — `ApplicationRepository.findByUserId(Long, Pageable)`/`findByUserIdAndStatus(Long, ApplicationStatus, Pageable)` 신규 추가로 해결.

**API 6 완료(구현, 커밋 보류 — `docs/collab/HANDOFF.md` 참고).**

---

### API 7 / 7 — 마이페이지 신청 상세 조회 (2026-08-06 추가, 로그인 필수) ✅ 2026-08-18 구현 완료

#### ④ Request/Response 설계

```
GET /api/my/applications/{applicationId}
Cookie: accessToken={JWT}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationNumber": "APP-2026-000123",
    "applicationType": "INDIVIDUAL",
    "cardTypeId": 1,
    "cardTypeName": "명예한국인증",
    "issueType": "MOBILE",
    "totalQuantity": 1,
    "status": "SUBMITTED",
    "paymentStatus": "WAITING",
    "paymentGuidedAt": null,
    "paymentDueAt": null,
    "cancelledAt": null,
    "cancellationType": null,
    "cancellationReason": null,
    "refundedAt": null,
    "cardReadyAt": null,
    "physicalDispatchedAt": null,
    "photoRejectReason": null,
    "applicant": {
      "name": "홍길동",
      "email": "hong@example.com",
      "phone": "010-1234-5678",
      "organizationName": null,
      "department": null
    },
    "receiver": null,
    "memberCount": 1,
    "createdAt": "2026-08-06T10:00:00"
  }
}
```

- 단체(GROUP) 신청은 `ApplicationMember`가 N건이라 이 응답에 전부 담지 않는다 — `memberCount`(총원수)만 포함하고, 구성원 개별 목록은 `GET /api/my/bulk-applications/{id}/members`(이번 범위 밖, 별도 TODO — `docs/BACKEND_API_GAPS.md` P0-2 참고)로 분리.
- `receiver`는 `issueType=MOBILE`이면 `null`. `MOBILE_AND_PHYSICAL`이면 아래 형태로 채워진다(✅ 2026-08-18 구현 시 확정 — 원 설계엔 `null` 예시만 있었음):
  ```json
  "receiver": {
    "name": "홍길동",
    "phone": "010-1234-5678",
    "zipCode": "12345",
    "address": "서울시 ...",
    "detailAddress": "101호",
    "deliveryRequest": null,
    "organizationName": null,
    "department": null
  }
  ```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| 비로그인 | `UNAUTHORIZED` | 401 |
| `Application.user_id != 로그인한 유저` | `FORBIDDEN` | 403 (API 4/5와 동일 패턴 — `application.isOwnedBy(userId)`) |
| `applicationId` 없음 | `APPLICATION_NOT_FOUND` | 404 |

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| applicationId ~ createdAt | `Application.*` (API 6과 동일 매핑 + `issue_type`/`photo_reject_reason`) |
| applicant.* | `Applicant.*`(name/email/phone/organization_name/department) |
| receiver | `Receiver.*`(있으면), `issueType=MOBILE`이면 조회 자체를 안 함 |
| memberCount | `ApplicationMember` 개수(`COUNT(*) WHERE application_id = ?`) |

#### ⑦ 누락된 필드 확인

없음(신청 목록 조회 API 6과 동일한 조회 조건 재사용).

**API 7 완료(구현, 커밋 보류 — `docs/collab/HANDOFF.md` 참고).**

---

### API 8 / 8 — 사용자 신청 취소 (로그인 필수) ✅ 구현 완료

```http
POST /api/applications/{applicationId}/cancel
Cookie: accessToken={JWT}
```

요청 본문은 없다. 로그인한 신청 소유자만 호출할 수 있다.

**Response `200 OK`**

```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "CANCELLED",
    "paymentStatus": "CONFIRMED",
    "refundRequired": true,
    "cancelledAt": "2026-08-17T15:00:00"
  }
}
```

- `SUBMITTED`, `REVIEWING`, `PHOTO_REJECTED`에서만 최초 취소할 수 있다.
- `CANCELLED` 재호출은 기존 취소·결제·환불 값을 변경하지 않는 멱등 성공이다.
- `WAITING`이면 `refundRequired=false`, `CONFIRMED + refundedAt=null`이면 `refundRequired=true`다.
- 최초 취소 commit 직후 얼굴사진·로고·직인·제출 ZIP 등 신청 전용 S3 객체를 즉시 삭제한다. rollback 시에는 삭제하지 않는다.

| 상황 | errorCode | HTTP |
|---|---|---:|
| 비로그인 | `UNAUTHORIZED` | 401 |
| 신청 없음 | `APPLICATION_NOT_FOUND` | 404 |
| 타인 신청 | `FORBIDDEN` | 403 |
| `NAME_EDITING` 이후 취소 시도 | `INVALID_STATUS_TRANSITION` | 400 |

**API 8 구현 완료.**

---

## Application 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `POST /api/applications` (개인 신청 생성) | 설계 완료 |
| 2 | `POST /api/applications/bulk` (단체 신청 생성) | 설계 완료 |
| 3 | `POST /api/applications/lookup` (신청 조회) | 설계 완료 |
| 4 | `PATCH /api/applications/{applicationId}/photo` (사진 재업로드) | 설계 완료 |
| 5 | `GET /api/applications/{applicationId}/cards/download` (카드 다운로드) | 설계 완료 |
| 6 | `GET /api/my/applications` (마이페이지 목록 조회, 페이징) | 설계 완료 (2026-08-06, 구현 전) |
| 7 | `GET /api/my/applications/{applicationId}` (마이페이지 상세 조회) | 설계 완료 (2026-08-06, 구현 전) |
| 8 | `POST /api/applications/{applicationId}/cancel` (사용자 신청 취소, 멱등) | 구현 완료 (2026-08-17) |

**프론트 반영 필요 항목(이번 도메인에서 새로 확인/누적된 것):**
- `StepInfo.tsx`/`StepFiles.tsx`가 `applicantType`에 따라 분기 안 되어 있음 — 개인은 생년월일·국적·출생시각·출생지역·성별·사진 입력, 로고/직인/제출ZIP 숨김 / 법인은 반대
- 상태 라벨은 `SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED` 기준으로 맞춰야 한다.
- 카드 다운로드 버튼/화면 신규 필요 (지금 프론트 어디에도 없음)
- `/lookup`에 `PHOTO_REJECTED` 상태일 때 반려사유 + "로그인 후 재업로드" 버튼 신규 추가 필요
- `LookupPage.tsx`의 카드번호 placeholder(`HN-KR-2609-1188`)가 틀린 형식 — `ROK-XXXXX-XXXX`로 교체 필요

---
Application 도메인 완료.

---
