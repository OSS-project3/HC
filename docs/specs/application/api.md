## Application 도메인

> ⚠️ **정정(2026-08-25):** "입금자명 등록 — 이번 패스 설계 보류"는 이후 **구현됐다.** `PATCH /api/applications/{applicationId}/depositor`(요청 `DepositorNameUpdateRequest{depositorName}`, 응답 Void, 본인·결제 확인 전만). `Application.depositorName` 필드로 저장하고 `MyApplicationDetailResponse`에 노출. (별도 Payment 도메인 아님.)

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
  "schoolName": "전북대학교",
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
    "department": "컴퓨터공학과",
    "address": "서울특별시 종로구 세종대로 1"
  }
}
```

- ✅ 2026-08-07 정정(`APPLICATION.md` 기준): `applicant.email`은 요청에 **포함한다** — `Applicant.email`은 로그인 `User.email`을 기본값으로 프리필하되 신청 화면에서 수정할 수 있다(계정 `User.email` 자체는 바뀌지 않음, 이 신청 1건의 값만 저장).
- ✅ 2026-08-07 정정: `receiver`는 `issueType=MOBILE`이면 **전달하면 안 되며**(전달 시 `INVALID_INPUT`), `issueType=MOBILE_AND_PHYSICAL`이면 필수다.
- ⚠️ 2026-07-31 정정: **`cardDesignId` → `cardTypeId`로 교체.** 사용자는 카드 "종류"만 선택하고, 구체적 디자인은 관리자가 신청 검토 중 배정(`.md` 2.1절, `docs/specs/application/requirements.md` 6절) — `Application.card_design_id`는 생성 시 NULL
- `logo`/`seal`/제출ZIP(회사용)은 이 API에 없음 — 개인 신청은 법인 전용 요소라 불필요. 단, **학생증(`CardType.code=STUDENT`)은 예외로 `schoolLogo`가 필수이고 `schoolSeal`은 선택**이다.
- ✅ 2026-08-24 최신 정책: `member.birthTime`은 선택이지만 `member.birthRegion`은 **필수**다. 태어난 도시/지역명을 최대 200자로 입력한다(예: `Chicago`, `London`, `Tokyo`, `Beijing`, `Los Angeles`). DB 컬럼은 기존 데이터 호환을 위해 Nullable 유지
- ✅ 2026-07-31 신규: `member.entryDate`(한국입국날짜, 선택) 추가
- ✅ 2026-08-14 확정(값은 대문자 문자열, `gender`와 동일 관례 — 프론트가 내부적으로 소문자를 쓰더라도 전송 직전 `.toUpperCase()` 필요): `orientation`(`LANDSCAPE`/`PORTRAIT`, 가로형/세로형)과 `schoolType`(`UNIVERSITY`/`HIGH_SCHOOL`, 대학교/고등학교)을 최상위 필드로 신규 추가. 둘 다 `cardTypeId`가 학생증일 때만 필수이고 그 외 카드종류는 반드시 생략해야 한다.
- ✅ 2026-08-14 조건 변경: `member.studentId`/`department`는 더 이상 "학생증이면 무조건 필수"가 아니라 **`schoolType=UNIVERSITY`일 때만** 필수다(최대 10자·숫자만 허용은 기존과 동일). `schoolType=HIGH_SCHOOL`이면 오히려 `studentId`/`department`를 보내면 안 된다(보내면 `INVALID_INPUT`). 이전 문서의 "학생증이면 무조건 필수" 서술(2026-08-07 정정분)은 이 조건으로 대체됨.
- ✅ 2026-08-19 신규: `schoolName`(학교명)을 최상위 필드로 추가. `cardTypeId`가 학생증이면 `schoolType`(`UNIVERSITY`/`HIGH_SCHOOL`) 무관하게 **항상 필수**이고(학번/학과와 달리 대학교 전용 조건 없음), 그 외 카드종류는 반드시 생략해야 한다. 트림 후 5~20자, 한글·영문·숫자·공백만 허용(그 외 문자는 `INVALID_INPUT`).
- ✅ 2026-08-25 신규(`admin-saju.md` 기준): `member.address`(카드에 인쇄되는 주소) 추가. `cardTypeId`가 학생증이 아니면 **필수**이고, 학생증이면 카드에 주소를 표시하지 않으므로 **보내면 안 된다**(보내면 `INVALID_INPUT`). 배송지 `receiver.address`와는 별개 값.

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
| `member.birthDate`/`nationality`/`birthRegion`/`gender` 중 하나라도 누락, `photo` 파일 누락 | `INVALID_INPUT` | 400 |
| `cardTypeId`가 학생증인데 `orientation`/`schoolType`/`schoolLogo` 중 하나라도 누락 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-19 신규: `cardTypeId`가 학생증인데 `schoolName`이 없거나, 트림 후 5~20자를 벗어나거나, 한글·영문·숫자·공백 외 문자를 포함 | `INVALID_INPUT` | 400 |
| `cardTypeId`가 학생증 + `schoolType=UNIVERSITY`인데 `studentId`/`department` 중 하나라도 누락, 또는 학번이 10자 초과·숫자 외 문자 포함 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-14 신규: `cardTypeId`가 학생증 + `schoolType=HIGH_SCHOOL`인데 `studentId`/`department` 중 하나라도 있음 | `INVALID_INPUT` | 400 |
| `cardTypeId`가 학생증이 아닌데 `orientation`/`schoolType`/`schoolName`/`studentId`/`department`/`schoolLogo`/`schoolSeal`을 보냄 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-25 신규: `cardTypeId`가 학생증이 아닌데 `member.address`가 없음(공백 포함) | `INVALID_INPUT` | 400 |
| ✅ 2026-08-25 신규: `cardTypeId`가 학생증인데 `member.address`를 보냄 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-16 신규: 같은 사용자가 오늘(KST) 이미 3건 신청(개인·단체 합산, 취소분 제외) | `APPLICATION_LIMIT_EXCEEDED` | 429 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- ✅ 2026-08-07 정정: `receiver.sameAsApplicant=true`여도 우편번호와 기본주소는 필수이며, 이름과 연락처만 복사된 기본값을 서버가 채우고 사용자가 수정할 수 있다.
- `member.birthTime`/`entryDate`는 선택이다. `member.birthRegion`은 필수이며 공백이면 Bean Validation에서 거절한다(2026-08-24 최신 정책)
- ✅ 2026-07-29 확인: `quantity`는 요청에 없음 — 개인 신청은 `total_quantity=1` 서버 고정, 클라이언트가 보낼 필요 없음

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| cardTypeId | Application.card_type_id (⚠️ 2026-07-31 정정 — 기존 `cardDesignId` 대체) |
| — | Application.card_design_id = `NULL`(관리자가 이후 배정, ⚠️ 2026-07-31 정정) |
| issueType | Application.issue_type |
| orientation | Application.orientation (✅ 2026-08-14 신규, 학생증 전용) |
| schoolType | Application.school_type (✅ 2026-08-14 신규, 학생증 전용) |
| schoolName | Application.school_name (✅ 2026-08-19 신규, 학생증 전용 — `UNIVERSITY`/`HIGH_SCHOOL` 둘 다 필수) |
| applicant.name/phone | Applicant.name/phone |
| applicant.email | Applicant.email (✅ 2026-08-07 정정 — 요청값 저장, `User.email`을 기본값으로 프리필하되 수정 가능) |
| receiver.* | Receiver.* (receiver.name/phone → `Receiver.receiver_name`/`receiver_phone` 컬럼명 매핑 주의) |
| receiver.sameAsApplicant | Application.receiver_same_as_applicant |
| member.birthDate | ApplicationMember.birth_date |
| member.nationality | ApplicationMember.nationality |
| member.birthTime | ApplicationMember.birth_time (⚠️ 2026-07-31 Nullable로 정정) |
| member.birthRegion | ApplicationMember.birth_region (요청 필수, 최대 200자. DB는 기존 데이터 호환을 위해 Nullable 유지) |
| member.gender | ApplicationMember.gender |
| member.entryDate | ApplicationMember.entry_date (✅ 2026-07-31 신규) |
| member.studentId | ApplicationMember.student_id (✅ 2026-08-07 정정, 최대 10자·숫자만. ⚠️ 2026-08-14 조건 변경: 학생증+`schoolType=UNIVERSITY`일 때만 필수) |
| member.department | ApplicationMember.department (✅ 2026-07-31 신규. ⚠️ 2026-08-14 조건 변경: 학생증+`schoolType=UNIVERSITY`일 때만 필수) |
| member.address | ApplicationMember.address (✅ 2026-08-25 신규 — 학생증이 아니면 필수, 학생증이면 반드시 생략) |
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

> 과거 이력(2026-07-31, 2026-08-24 최신 정책으로 대체됨): `cardDesignId`(사용자 선택) → `cardTypeId`(카드종류만 선택, 디자인은 관리자 배정)로 교체, 당시 `birthTime`/`birthRegion` 필수→선택 전환, `entryDate`/학생증 전용 필드(`studentId`/`department`/`schoolLogo`/`schoolSeal`) 신규 추가.

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
  "schoolName": "전북대학교",
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
- ✅ 2026-08-14 확정: `orientation`(`LANDSCAPE`/`PORTRAIT`)은 API 1과 동일하게 `cardTypeId`가 학생증일 때만 필수이고 신청서 전체에 1개다(엑셀 컬럼이 아니라 이 요청의 최상위 필드). `schoolType`(`UNIVERSITY`/`HIGH_SCHOOL`)도 동일하게 신청서 전체에 1개로 신규 추가된다.
- ⚠️ 2026-08-20 재정정: `schoolType`은 엑셀의 학번·학과 필수 여부에도 개인 신청과 동일하게 영향을 준다("영향을 주지 않는다"던 이전 서술은 오류였다). `UNIVERSITY`면 엑셀 11·12열(학번·학과)이 필수, `HIGH_SCHOOL`이면 그 열에 값이 있으면 오히려 행 오류(`INVALID_INPUT`)다. `BulkExcelParser.parse(zipFile, isStudent, schoolType)`가 이 조건을 검증한다.
- ✅ 2026-08-19 신규: `schoolName`(학교명)도 API 1과 동일하게 최상위 필드로 추가. 단체 신청은 항상 한 학교 단위로 접수된다는 전제로 신청서 전체에 1개이며(엑셀 컬럼 아님), `cardTypeId`가 학생증이면 `schoolType` 무관하게 항상 필수, 그 외 카드종류는 생략해야 한다. 트림 후 5~20자, 한글·영문·숫자·공백만 허용.

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
| ✅ 2026-08-19 신규: `cardTypeId`가 학생증인데 `schoolName`이 없거나, 트림 후 5~20자를 벗어나거나, 한글·영문·숫자·공백 외 문자를 포함 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-14 신규: `cardTypeId`가 학생증이 아닌데 `orientation`/`schoolType`을 보냄 | `INVALID_INPUT` | 400 |
| ✅ 2026-08-19 신규: `cardTypeId`가 학생증이 아닌데 `schoolName`을 보냄 | `INVALID_INPUT` | 400 |
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
| schoolType | Application.school_type (✅ 2026-08-14 신규, 학생증 전용. ⚠️ 2026-08-20 재정정: 엑셀 학번/department 필수 여부에 개인과 동일하게 영향을 준다 — `UNIVERSITY`만 필수, `HIGH_SCHOOL`이면 있으면 거절) |
| schoolName | Application.school_name (✅ 2026-08-19 신규, 학생증 전용 — `UNIVERSITY`/`HIGH_SCHOOL` 둘 다 필수) |
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
| 출생지역 | birth_region | **필수** — 태어난 도시/지역명, 최대 200자. 예: `Chicago`, `London`, `Tokyo`, `Beijing`, `Los Angeles` |
| 성별 | gender | 필수 |
| 개별입국날짜 | entry_date(상단 공통값과 조합해 해석) | 선택 |
| 이메일 | email(✅ 2026-07-31 신규 — 행마다 다른 신청자 본인 이메일) | 필수 |
| 전화번호 | phone(✅ 2026-07-31 신규 — 행마다 다른 신청자 본인 연락처) | 필수 |
| 주소 | address | 선택 |
| 학번 | student_id(✅ 2026-08-07 정정, 최대 10자·숫자만) | ⚠️ 2026-08-20 재정정: 학생증+`schoolType=UNIVERSITY`만 필수. `HIGH_SCHOOL`이면 값이 있으면 행 오류(개인 신청과 동일) |
| 학과 | department(✅ 2026-07-31 신규) | ⚠️ 2026-08-20 재정정: 학생증+`schoolType=UNIVERSITY`만 필수. `HIGH_SCHOOL`이면 값이 있으면 행 오류(개인 신청과 동일) |
| (사진 파일명, ZIP 루트) | photo_path | 필수 |

✅ 2026-08-18 정정(`APPLICATION.md` 기준): ZIP 루트에는 자유로운 파일명의 `.xlsx` Excel 1개만 허용한다(2개 이상이면 전체 실패). 공식 양식의 A열은 `사진 번호`이며 4~103행에 문자열 `001`~`100`이 미리 입력되고 잠금 처리된다. 사용자는 이 열을 입력·수정하지 않는다. 사진 번호만 있는 행은 빈 행으로 무시하고 B열 이후 신청자 정보가 하나라도 입력된 행만 검증·처리한다. ZIP 루트 사진은 실제 처리 행의 사진 번호와 정확히 매칭하며, 빈 행 번호의 사진은 `PHOTO_UNMATCHED` 여분 사진으로 전체 실패한다. 사진 확장자는 대소문자를 구분하지 않는다(예: `001` ↔ `001.jpg`/`001.JPG`). 구성원별 UploadFile ID는 생성하지 않고 매칭된 이미지의 저장 경로만 `ApplicationMember.photo_path`에 저장한다. ZIP 원본의 `Application.submit_file_id`는 신청 단위 제출 파일을 가리키며 구성원 사진 ID가 아니다.

#### ⑦ 누락된 필드 확인

⚠️ **재정정 (2026-07-31, DB.md와 정합성 점검 중 발견):** 원래 "없음"으로 완료 처리했던 07-29 시점 이후 `.md` 2.4절에서 `nationality`/`birth_time`/`birth_region`/`gender`가 NOT NULL로 신규 확정되어, 엑셀 템플릿에 4개 컬럼을 추가로 반영했었음.

> 과거 이력(2026-07-31, 2026-08-24 최신 정책으로 대체됨): `cardDesignId`→`cardTypeId` 교체, 당시 `birthTime`/`birthRegion` 필수→선택 전환, 개별입국날짜(+상단 공통값)/이메일/전화번호/학번/학과 컬럼 신규 추가.

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

### 관리자 인앱 작명 확정 — `POST /api/admin/applications/{applicationId}/members/{memberId}/name`

✅ 2026-08-25 갱신(`admin-saju.md`/1-B 기준): 요청 바디에 `surname` 필드를 신규 추가했다.

```json
{
  "surname": "홍",
  "name": "길동",
  "hanja": "吉童",
  "reading": "길할 길, 아이 동",
  "meaning": "복을 비는 이름"
}
```

- `surname`은 선택 입력이다. `NAME_EDITING` 중에는 비워둘 수 있으나(값을 안 보내면 기존 성씨를 그대로 두지 않고 `NULL`로 저장), `completeNaming()` 집계 검증 시점에는 모든 Member에 값이 있어야 한다.
- `name`/`meaning`은 `@NotBlank` — 없으면 `INVALID_INPUT`.
- 저장 시점 형식 검증(`ApplicationMember.assignKoreanName`, 엑셀 왕복 경로와 공유): `name`은 성씨를 제외한 한글 2~3글자, `surname`은 한글 1~2글자, `hanja`가 있으면 `name`과 Unicode 글자 수가 같아야 한다. 하나라도 위반하면 `INVALID_INPUT`.

### 관리자 작명 완료 — `POST /api/admin/applications/{applicationId}/complete-naming`

✅ 2026-08-25 갱신(1-B): 상태 전이 전에 Application 소속 모든 `ApplicationMember`의 성씨·이름·의미가 채워져 있는지 집계 검증한다.

- 하나라도 누락되면 `errorCode=NAMING_INCOMPLETE`(400)로 거절하고 `Application.status`는 변경하지 않는다. 응답 `errors[]`에 누락 Member별 상세(`row`=memberId, `field`=`surname`/`name`/`nameMeaning`, `code`=`REQUIRED`)를 담는다(`BulkValidationException` 재사용).
- 전원 완료 상태면 기존과 동일하게 `NAME_EDITING → PRODUCTION_READY` 전이 후 `AdminActivityLog.NAMING_COMPLETE`를 기록한다.
- Member가 0명인 Application(정상 플로우에서는 발생하지 않음)은 검증 대상이 없어 전이가 허용된다.

### 관리자 카드번호 확정(개인/단일) — `PUT /api/admin/applications/{applicationId}/members/{memberId}/card-number`

✅ 2026-08-26 신규(1-C, `admin-saju.md` "관리자 카드번호 입력 정책"): 서버가 채번하지 않고 관리자가 직접 입력한다.

```json
{ "cardNumber": "ROK-12345-6789" }
```

- 형식은 `ROK-XXXXX-XXXX`(5자리-4자리 숫자) — 위반 시 `INVALID_INPUT`.
- 카드가 이미 생성된 Member(`cardFrontPath` 확정)의 번호를 **다른 값으로** 바꾸려 하면 `CARD_NUMBER_LOCKED`(400). 같은 값 재저장은 멱등 성공.
- 다른 Member(다른 신청 포함)가 이미 쓰는 번호면 DB UNIQUE 제약에 걸려 `CARD_NUMBER_ALREADY_USED`(409).
- `memberId`가 `applicationId` 소속이 아니면 `INVALID_INPUT`.

### 관리자 카드번호 일괄 확정(단체) — `PUT /api/admin/applications/{applicationId}/card-numbers`

✅ 2026-08-26 신규(1-C): 관리자 화면에서 Excel의 "사진 번호"·"카드번호" 두 열을 탭 구분으로 붙여넣으면, 프론트가 아래 JSON으로 변환해 보낸다.

```json
{
  "applicationVersion": 12,
  "items": [
    { "photoNumber": "001", "cardNumber": "ROK-12345-0001" },
    { "photoNumber": "002", "cardNumber": "ROK-12345-0002" }
  ]
}
```

- **매칭 키는 `(applicationId, photoNumber)`다.** 화면 순서나 `memberId`로 매칭하지 않는다 — `photoNumber`는 `ApplicationMember.photoNumber`(1-A)를 그대로 쓴다.
- `applicationVersion`이 현재 `Application.version`과 다르면 저장 없이 `APPLICATION_VERSION_CONFLICT`(409) — Application row는 `PESSIMISTIC_WRITE`로 잠근 채 비교한다.
- 요청 내부에서 검증하는 항목(하나라도 걸리면 **전체 거절, 부분 저장 없음** — `errorCode=CARD_NUMBER_VALIDATION_FAILED`(400), `errors[]`에 행별 상세):
  - `photoNumber` 요청 내부 중복
  - `cardNumber` 요청 내부 중복
  - `cardNumber` 형식 오류
  - 이 신청에 존재하지 않는 `photoNumber`
  - 이미 카드가 생성된 Member의 번호를 다른 값으로 바꾸려는 시도
- `items`는 전체 Member를 다 채우지 않아도 된다(일부만 먼저 저장 가능) — 단, 이번 범위(1-C)는 저장 계약까지이며 "최종 카드 생성 전 전원 필수" 강제는 카드 생성 단계(2~3단계)에서 적용한다.
- DB UNIQUE 위반(요청 통과 후에도 동시성으로 충돌 발생)은 `CARD_NUMBER_ALREADY_USED`(409)로 응답하고 전체 rollback.
- 같은 `items`로 재호출하면 멱등 성공(카드번호가 이미 같은 값이면 변경 없이 `updatedCount`만 반환).

### 출생지역 검색 — `GET /api/admin/birth-region/search?query={query}`

✅ 2026-08-26 신규(1-D): Google Geocoding API로 지명 → 좌표 후보를 조회한다. 특정 신청/구성원과 무관한 순수 조회.

```json
{ "data": [ { "displayName": "Chicago, IL, USA", "latitude": 41.8781, "longitude": -87.6298 } ] }
```

- Google Maps API 키가 설정 안 되어 있으면 `GEOCODING_NOT_CONFIGURED`(503).
- 검색 결과가 없으면 빈 배열(에러 아님) — 관리자가 timezoneId를 직접 입력하는 경로로 유도.

### 만세력 timezone/DST 판정(미리보기) — `POST /api/admin/applications/{applicationId}/members/{memberId}/manseryeok/resolve`

✅ 2026-08-26 신규(1-D): **DB에 아무것도 저장하지 않는다.** `ApplicationMember`의 생년월일시 + 요청의 좌표/timezoneId로 절대 시각을 확정 시도한다.

```json
{ "latitude": 41.8781, "longitude": -87.6298, "timezoneId": "America/Chicago", "selectedOffset": null }
```

- `timezoneId`를 안 보내면 서버가 `latitude`/`longitude`로 Google Time Zone API를 호출해 조회한다.
- `selectedOffset`을 보내면(AMBIGUOUS 후보 중 관리자가 하나를 골랐을 때) 그 값이 실제 유효 후보인지 재검증한 뒤 EXACT로 확정한다.
- 응답은 `status`에 따라 형태가 다르다(admin-saju.md "DST 경계 정책" 그대로):

```json
// EXACT
{ "status": "EXACT", "timezoneId": "America/Chicago", "longitude": -87.6298, "selectedOffset": "-05:00", "utcInstant": "...", "candidates": [] }
// NONEXISTENT_LOCAL_TIME — DST 시작으로 그 현지시각 자체가 존재하지 않음. 관리자가 출생기록 재확인 필요.
{ "status": "NONEXISTENT_LOCAL_TIME", "timezoneId": "...", "selectedOffset": null, "utcInstant": null, "candidates": [] }
// AMBIGUOUS_LOCAL_TIME — DST 종료로 같은 현지시각이 두 번 존재.
{ "status": "AMBIGUOUS_LOCAL_TIME", "timezoneId": "America/New_York", "candidates": [
  { "offset": "-04:00", "utcInstant": "2000-10-29T05:30:00Z" },
  { "offset": "-05:00", "utcInstant": "2000-10-29T06:30:00Z" }
] }
```

### 만세력 확정 결과 저장 — `POST /api/admin/applications/{applicationId}/members/{memberId}/manseryeok`

✅ 2026-08-26 신규(1-D): 프론트(`saju.ts`)가 위 resolve 응답의 확정값으로 진태양시 보정+만세력 계산을 마친 뒤, 최종 결과를 저장 요청한다.

```json
{
  "timezoneId": "America/New_York",
  "longitude": -74.0060,
  "selectedOffset": "-04:00",
  "utcInstant": "2000-10-29T05:30:00Z",
  "timeAccuracy": "EXACT",
  "confirmedPillars": { "year": {"stem": "경", "branch": "진"}, "month": {...}, "day": {...}, "hour": {...} },
  "uncertainPillars": [],
  "elementCounts": { "목": 2, "화": 1, "토": 1, "금": 2, "수": 2 },
  "calculationEngineVersion": "manseryeok@1.0.0",
  "inputHash": "..."
}
```

- `timeAccuracy=EXACT`이면 Spring이 `timezoneId`+`ApplicationMember`의 생년월일시로 자체 재계산해 `selectedOffset`/`utcInstant`가 일치하는지 검증한다 — 불일치하면 `INVALID_INPUT`(프론트가 보낸 timezone 확정값을 신뢰하지 않고 무결성만 재검증, 실제 사주 계산 자체는 재현하지 않음).
- 저장은 이력 보존 방식이다 — 기존 활성 결과가 있으면 비활성화하고 새 행을 활성으로 추가한다. 덮어쓰지 않는다.
- `AdminActivityLog.MANSERYEOK_CONFIRMED`로 기록한다.

### 활성 만세력 결과 조회 — `GET /api/admin/applications/{applicationId}/members/{memberId}/manseryeok`

✅ 2026-08-26 신규(1-D): 현재 활성(`active=true`) 결과를 반환한다. 없으면 `NOT_FOUND`(404). 카드 띠 이미지 결정 등에 쓴다.

### 카드 디자인 조회 — `GET /api/admin/card-designs?cardTypeId={id}&active={true|false}`

✅ 2026-08-26 신규(2-A): 관리자가 신청의 카드 종류에 맞는 검수 완료 디자인을 조회한다. 특정 신청·구성원과 무관한 순수 카탈로그 조회.

```json
{ "data": [
  { "id": 1, "designNumber": 1, "name": "명예한국인증 디자인1", "orientation": "LANDSCAPE", "isDefault": true, "active": true }
] }
```

- `active` 생략 시 활성+비활성 전체 반환. `true`/`false`로 필터링 가능.
- `cardTypeId`가 존재하지 않으면 `CARD_TYPE_NOT_FOUND`(404).
- `cardTypeId`가 학생증이면 `UNSUPPORTED_CARD_TYPE`(400) — 학생증 카드 렌더링은 이번 카드 제작 계획 범위 밖(TODO.md 참고).
- `designNumber`는 `card-templates/{cardType}/{designNumber}/` classpath 리소스와 1:1 대응하며, `CardImageCompositor` 호출 시 그대로 넘긴다.

---
Application 도메인 완료.

---


## 관리자 카드 제작 워크플로우 구현 조사 (2026-08-24)

> 현재 main 코드 기준의 구현 현황과 예상 작업 범위다. 아직 신규 관리자 API, DB 필드, 렌더러를 구현한 것은 아니다.

### 목표 흐름

    단체 신청 Excel+사진 ZIP 업로드
    → 구성원별 관리자 검토
    → 수정 요청과 사용자 재제출
    → 재검토
    → 구성원별 한국 이름 확정
    → 디자인 6개 중 선택
    → 신청 정보를 고정 좌표에 합성
    → 앞·뒷면 PNG/인쇄용 파일 생성
    → 다운로드·브라우저 인쇄 또는 카드 프린터 출력

### 현재 구현과 재사용 범위

| 기능 | 현재 코드 상태 | 재사용 판단 |
|---|---|---|
| 단체 Excel+사진 ZIP 신청 | 파싱, 검증, 사진 번호 매칭, S3 업로드, ApplicationMember 생성 구현 | 높음 |
| 신청 상태 | SUBMITTED, REVIEWING, PHOTO_REJECTED, NAME_EDITING, PRODUCTION_READY, PRODUCING, COMPLETED 구현 | 높음 |
| 관리자 인가 | /api/admin/**에 ADMIN 권한 적용 | 그대로 재사용 |
| 관리자 신청 조회 | 목록·상세 API 구현 | 부분 재사용 |
| 상세의 구성원 데이터 | memberCount만 제공하며 구성원 목록·사진은 없음 | 신규 API 필요 |
| 결제 안내·입금 확인 | Service 구현, HTTP Controller 미연결 | Service 재사용 |
| 사진 반려 | Application 상태 메서드만 존재 | 관리자 Service/API 필요 |
| 사용자 재제출 | 개인 사진 또는 단체 ZIP 전체 교체와 PHOTO_REJECTED → REVIEWING 구현 | 사진 반려에는 재사용 |
| 구성원별 검토·일반 정보 수정 요청 | 구성원별 상태·수정 사유·이력 없음 | 신규 |
| 한국 이름 편집 | name, chineseName, nameMeaning, nameInterpretation 컬럼만 존재 | 필드 재사용, API/UI 신규 |
| 디자인 배정 | Application.cardDesignId 존재 | 필드 재사용, 배정 API 신규 |
| CardDesign | Entity/Repository와 앞·뒷면 템플릿 ID 존재, CRUD/시드 없음 | 부분 재사용 |
| 디자인 6개 | 프론트 정적 샘플 이미지이며 백엔드 템플릿과 연결되지 않음 | 인쇄 원본 별도 검증 |
| 좌표 기반 합성 | 코드 없음 | 신규 |
| 카드 결과 | issueDate, cardNumber, cardFrontPath, cardBackPath 존재 | 재사용 |
| 사용자 다운로드 | 개인 presigned URL과 단체 ZIP 다운로드 구현 | 결과 소비 경로 재사용 |
| 관리자 UI | adminMock/localStorage 기반 | 실제 API 기반으로 교체 |

### 현재 구조와 충돌하는 부분

1. 현재 단체 정책은 Application 전체 단위 검토이며 ApplicationMember별 상태가 없다. 한 명씩 검토하고 진행률과 수정 대상을 저장하려면 멤버별 검토 모델이 필요하다.
2. ✅ 2026-08-25 해소: Excel parser의 photoNumber(001~100)를 `ApplicationMember.photo_number`에 저장하도록 구현했다(`docs/specs/application/admin-saju.md` "관리자 작명 확정·카드 제작 구현 계획" 1-A). `(application_id, photo_number)` 조합이 유일하다.
3. PHOTO_REJECTED는 영문명·생년월일·국적 같은 일반 정보 수정 요청을 표현하기 어렵다. CORRECTION_REQUESTED 같은 별도 상태 추가를 권장한다.
4. 단체 재제출은 기존 멤버 전체를 삭제하고 새 ZIP으로 다시 만든다. 최소 구현에서는 재사용 가능하지만 구성원 한 명만 웹에서 수정하려면 별도 사용자 API가 필요하다.
5. MOBILE_AND_PHYSICAL은 파일 생성 후에도 발송 전까지 PRODUCING인데 다운로드 API는 COMPLETED만 허용한다. 파일 생성 완료 시 다운로드 정책을 적용하려면 cardReadyAt != null 기준으로 정리해야 한다.
6. Application.cardDesignId는 신청 전체 디자인 하나를 뜻한다. 구성원마다 다른 디자인이면 ApplicationMember.cardDesignId가 필요하다.

### 신규 DB 구조

최소 필드:

    ApplicationMember.photoNumber
    ApplicationMember.reviewStatus       # PENDING / APPROVED / CORRECTION_REQUESTED
    ApplicationMember.reviewedAt
    ApplicationMember.reviewedBy
    ApplicationMember.version
    UNIQUE(application_id, photo_number)

여러 필드의 수정 사유와 해결 여부를 전달하려면 다음 이력 테이블을 권장한다.

    ApplicationMemberReviewIssue
    - id
    - applicationId
    - photoNumber
    - fieldName
    - reason
    - status: OPEN / RESOLVED
    - requestedBy
    - requestedAt
    - resolvedAt

단체 재제출 시 ApplicationMember row가 교체되므로 이력은 memberId만 쓰지 않고 applicationId + photoNumber를 함께 보존한다.

Application.cardDesignId, 기존 작명 필드, issueDate/cardNumber/cardFrontPath/cardBackPath, Application.version, CardDesign의 템플릿·방향 필드는 재사용한다.

PDF를 저장한다면 Application.printFileId와 printGeneratedAt을 선택적으로 추가한다. 최대 100명의 앞·뒷면을 비동기로 만들고 부분 실패를 재시도하려면 CardProductionJob 엔티티를 권장한다.

### 신규 또는 연결이 필요한 API

    POST /api/admin/applications/{applicationId}/payment-guide
    POST /api/admin/applications/{applicationId}/payment-confirm
    POST /api/admin/applications/{applicationId}/start-review

    GET  /api/admin/applications/{applicationId}/members
    GET  /api/admin/applications/{applicationId}/members/{memberId}
    POST /api/admin/applications/{applicationId}/members/{memberId}/approve
    POST /api/admin/applications/{applicationId}/members/{memberId}/correction-request
    POST /api/admin/applications/{applicationId}/complete-review

    PATCH /api/admin/application-members/{memberId}/name
    POST  /api/admin/applications/{applicationId}/complete-naming
    GET   /api/admin/card-designs?cardTypeId={cardTypeId}
    POST  /api/admin/card-designs
    PATCH /api/admin/card-designs/{cardDesignId}
    PUT   /api/admin/applications/{applicationId}/card-design

    POST /api/admin/applications/{applicationId}/members/{memberId}/card-preview
    POST /api/admin/applications/{applicationId}/issue-cards
    GET  /api/admin/card-production-jobs/{jobId}
    GET  /api/admin/applications/{applicationId}/production-files

complete-review는 모든 구성원이 승인된 경우에만 NAME_EDITING으로 전이한다.

### 관리자 UI

/admin/applications/{applicationId} 상세 라우트를 권장한다. 신청·결제·상태 요약, 구성원 검토 큐, 사진 확대, 이전/다음 이동, 필드별 수정 요청, 상태 필터, 작명 저장, 미완료 인원, 디자인 선택, 앞·뒷면 미리보기, 생성 진행률·재시도, 다운로드·출력 버튼이 필요하다. 현재 mock 상태 드롭다운은 허용된 상태 전이 명령 버튼으로 교체한다.

### 카드 렌더링

제작 원본은 서버 측 Java2D 렌더링을 권장한다.

    빈 앞·뒷면 템플릿
    + 얼굴사진·로고·직인
    + 이름·영문명·한자·뜻·풀이
    + 카드번호·주소·발급일
    + 디자인별 좌표·폰트 설정
    → PNG 생성 → S3 저장
    → ApplicationMember.cardFrontPath/cardBackPath 기록

인쇄 가능한 빈 템플릿, 픽셀 크기와 DPI, 한글·한자 폰트와 사용 권한, 텍스트 초과 규칙, 사진 crop, bleed/crop mark를 확정해야 한다. Alpine Docker에도 동일 폰트를 설치한다. 프론트 Canvas 유틸은 생성 결과 다운로드에는 재사용할 수 있지만 제작 원본 렌더러로 사용하지 않는다.

### 예상 소요일

개발자 1명, 테스트 포함, 빈 템플릿 6개와 좌표가 준비된 경우다.

| 기능 | 예상 |
|---|---:|
| 정책·상태·DB 모델 | 1~2일 |
| 관리자 상세·구성원 조회 | 3~4일 |
| 구성원 검토·수정 요청·재검토 | 5~7일 |
| 작명 저장·완료 검증·UI | 3~4일 |
| CardDesign 등록·조회·배정 | 3~5일 |
| 6개 디자인 렌더러 | 6~9일 |
| 카드번호·이미지 일괄 생성·S3 | 3~5일 |
| 미리보기·진행률·다운로드 UI | 3~5일 |
| 회귀·동시성·부분 실패 테스트 | 3~5일 |
| **PNG/ZIP 전체** | **30~46일** |

| 출력 방식 | 추가 | 전체 |
|---|---:|---:|
| 앞·뒷면 PNG + 단체 ZIP | 기본 | 30~46일 |
| 정확한 카드 크기 PDF | +3~5일 | 33~51일 |
| 브라우저 인쇄 | +1~3일 | 31~49일 |
| Linux 호환 네트워크 프린터 SDK | +8~15일 | 38~61일 |
| 관리자 PC USB 프린터 + Windows 출력 에이전트 | +15~30일 | 45~76일 |

EC2의 Linux/Alpine 백엔드는 관리자 PC의 USB 프린터에 직접 접근할 수 없다. 직접 연동은 프린터 모델, 연결 방식, 운영체제, 제조사 SDK를 확인한 뒤 재산정한다.

위 추정은 전체 디자인 6개 기준이다. 카드 종류마다 6개여서 총 24개를 보정·테스트한다면 8~15일이 추가될 수 있다.

---
결론
현재 코드는 “신청 접수와 상태 모델”까지는 상당 부분 재사용할 수 있지만, 관리자 제작 워크플로우의 핵심인 구성원별 검토·작명 API·디자인 배정·카드 렌더링·출력은 대부분 새로 구현해야 합니다.
요청한 전체 흐름을 운영 가능한 수준으로 구현하면, 개발자 1명 기준:
- PNG/ZIP 다운로드: 약 30~46인일
- 인쇄용 PDF 포함: 약 33~51인일
- 브라우저 인쇄 포함: 약 31~49인일
- 특정 카드 프린터 직접 연동: 약 38~76인일
코드는 수정하지 않았습니다.
현재 구현과 재사용 범위
기능	현재 구현	재사용 판단
단체 Excel+사진 ZIP 신청	파싱·검증·사진 매핑·ApplicationMember 생성까지 구현	높음
신청 상태 머신	SUBMITTED → REVIEWING → PHOTO_REJECTED → NAME_EDITING → PRODUCTION_READY → PRODUCING → COMPLETED 구현	높음
관리자 인증	/api/admin/**에 ADMIN 권한 적용	그대로 재사용
관리자 신청 목록·상세	API 2개 구현	부분 재사용
결제 안내·입금 확인	Service 구현, HTTP API 없음	Service 재사용
사진 반려	엔티티 상태 메서드만 존재	API/Service 신규 필요
사용자 사진 재제출	개인 사진 또는 단체 ZIP 전체 교체 구현	사진 반려에는 재사용 가능
구성원별 정보 검토	없음	신규
일반 정보 수정 요청	없음. 현재는 PHOTO_REJECTED만 존재	신규 정책·상태 필요
한국 이름 저장	DB 필드는 존재	Entity/API/UI 신규
디자인 선택	Application.cardDesignId 필드 존재	필드 재사용
디자인 관리	CardDesign Entity/Repository만 존재	CRUD·시드·UI 신규
카드 템플릿 6개	프론트 정적 샘플 이미지로 존재	인쇄 템플릿으로 바로 사용 불가
좌표 기반 합성	문서 설계만 있고 코드 없음	신규
카드 결과 저장	앞·뒷면 경로, 카드번호, 발급일 필드 존재	재사용
카드 다운로드	사용자 PNG/단체 ZIP 다운로드 구현	결과 소비 부분 재사용
관리자 화면	localStorage/mock 기반 목록·상태 변경	대부분 교체


근거가 되는 주요 코드:
- 관리자 API는 조회 2개뿐입니다: [AdminApplicationController.java (line 18)](/D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/java/com/example/honorcitizen/api/AdminApplicationController.java:18)
- 상태와 cardDesignId는 이미 있습니다: [Application.java (line 107)](/D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/java/com/example/honorcitizen/domain/application/entity/Application.java:107)
- 작명·결과 이미지 필드는 이미 있습니다: [ApplicationMember.java (line 38)](/D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/java/com/example/honorcitizen/domain/application/entity/ApplicationMember.java:38)
- 결제 Service는 구현됐습니다: [ApplicationService.java (line 436)](/D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/java/com/example/honorcitizen/domain/application/service/ApplicationService.java:436)
- 카드 다운로드는 생성된 파일을 소비할 뿐, 이미지를 생성하지는 않습니다: [ApplicationService.java (line 648)](/D:/HC-worktrees/main-preview/backend/honor-citizen/src/main/java/com/example/honorcitizen/domain/application/service/ApplicationService.java:648)
- 관리자 프론트는 현재 mock입니다: [AdminPage.tsx (line 20)](/D:/HC-worktrees/main-preview/frontend/src/pages/AdminPage/AdminPage.tsx:20)
현재 워크플로우와 충돌하는 부분
1. 구성원별 검토 상태가 없음
현재 정책은 단체 신청 전체를 한 번에 검토하는 구조입니다. ApplicationMember에는 검토 상태나 수정 요청 사유가 없습니다.
“100명 중 37명까지 검토했다”, “002번은 승인, 003번은 수정 요청” 같은 진행 상황을 저장할 수 없습니다.
2. 사진 번호가 DB에 저장되지 않음
Excel parser는 001, 002 같은 photoNumber를 가지고 있지만, ApplicationMember 저장 시 버립니다. 구성원별 수정 요청을 사용자에게 전달하려면 안정적인 식별자로 반드시 저장해야 합니다.
3. PHOTO_REJECTED는 일반 정보 오류를 표현하기 어려움
영문명·생년월일·국적 오류까지 PHOTO_REJECTED로 표현하는 것은 의미가 맞지 않습니다.
전체 요구를 구현하려면 다음 중 하나를 정해야 합니다.
- 권장: CORRECTION_REQUESTED 상태 추가
- 최소 변경: PHOTO_REJECTED를 모든 수정 요청에 재사용하지만 명칭이 부자연스러움
4. 관리자 상세 응답에 구성원 목록이 없음
현재 관리자 상세는 memberCount만 반환합니다. 구성원 데이터·사진 URL·검토 상태를 가져오는 API가 추가로 필요합니다.
5. 실물 포함 신청의 모바일 다운로드 조건
MOBILE_AND_PHYSICAL은 카드 파일이 생성돼도 배송 전까지 PRODUCING입니다. 그러나 기존 다운로드 API는 COMPLETED만 허용하므로 배송 전 모바일 카드 다운로드가 불가능합니다.
정책대로라면 status == COMPLETED가 아니라 cardReadyAt != null을 다운로드 기준으로 보는 것이 자연스럽습니다.
새로 필요한 DB 구조
필수에 가까운 필드
ApplicationMember:
photoNumber          # 001~100, Excel/사진/수정 요청 연결
reviewStatus         # PENDING, APPROVED, CORRECTION_REQUESTED
reviewedAt
reviewedBy
version              # 구성원별 관리자 동시 수정 방지
권장 제약:
UNIQUE(application_id, photo_number)
수정 요청 상세
필드별 수정 사유를 제공하려면 별도 테이블이 더 적절합니다.
ApplicationMemberReviewIssue
- id
- applicationId
- photoNumber
- fieldName
- reason
- status: OPEN / RESOLVED
- requestedBy
- requestedAt
- resolvedAt
재업로드 시 기존 ApplicationMember가 삭제·재생성되므로, 이력 테이블은 memberId만 참조하기보다 applicationId + photoNumber를 함께 보존해야 합니다.
이미 있어 추가하지 않아도 되는 필드
- Application.cardDesignId
- ApplicationMember.name
- chineseName
- nameMeaning
- nameInterpretation
- issueDate
- cardNumber
- cardFrontPath
- cardBackPath
- Application.version
출력 방식에 따라 선택
PDF를 저장할 경우:
Application.printFileId
Application.printGeneratedAt
대량 생성을 비동기로 처리한다면 별도 CardProductionJob 엔티티가 권장됩니다. 최대 100명의 앞·뒷면 200장을 S3에 저장하는 작업을 단일 HTTP 요청으로 처리하면 타임아웃과 부분 실패 관리가 어려워집니다.
필요한 API
기존 API 확장/연결
GET  /api/admin/applications
GET  /api/admin/applications/{id}
POST /api/admin/applications/{id}/payment-guide
POST /api/admin/applications/{id}/payment-confirm
POST /api/admin/applications/{id}/start-review
결제 안내·확인은 기존 Service를 Controller에 연결하면 됩니다.
구성원 검토
GET  /api/admin/applications/{id}/members
GET  /api/admin/applications/{id}/members/{memberId}
POST /api/admin/applications/{id}/members/{memberId}/approve
POST /api/admin/applications/{id}/members/{memberId}/correction-request
POST /api/admin/applications/{id}/complete-review
complete-review는 모든 구성원이 승인된 경우에만 NAME_EDITING으로 전이합니다.
사용자 수정은 MVP에서 기존 단체 ZIP 전체 재업로드를 재사용할 수 있습니다. 웹에서 구성원 한 명만 수정하게 만들면 별도 사용자 수정 API와 UI가 필요해 3~5인일 정도 증가합니다.
작명
PATCH /api/admin/application-members/{memberId}/name
POST  /api/admin/applications/{id}/complete-naming
complete-naming은 모든 구성원의 필수 작명 필드가 입력됐는지 확인한 후 PRODUCTION_READY로 전이합니다.
디자인
GET  /api/admin/card-designs?cardTypeId={id}
POST /api/admin/card-designs
PATCH /api/admin/card-designs/{designId}
PUT  /api/admin/applications/{id}/card-design
현재 모델은 신청 전체에 디자인 하나를 배정합니다. 구성원마다 다른 디자인을 고르게 하려면 ApplicationMember.cardDesignId가 추가로 필요하고 1~2인일이 증가합니다.
렌더링·제작
POST /api/admin/applications/{id}/members/{memberId}/card-preview
POST /api/admin/applications/{id}/issue-cards
GET  /api/admin/card-production-jobs/{jobId}
GET  /api/admin/applications/{id}/production-files
프린터 SDK 직접 연동 시에는 별도의 출력 명령 API 또는 로컬 출력 에이전트 통신 API가 필요합니다.
관리자 UI
기존 확장 행 형태보다 별도 상세 라우트가 적합합니다.
/admin/applications
/admin/applications/{applicationId}
상세 화면에 필요한 영역:
- 신청·결제·상태 요약
- 구성원 검토 큐
- 사진 확대
- 이전/다음 구성원 이동
- 필드별 수정 요청
- 승인/반려 상태 필터
- 작명 입력 및 자동 저장
- 미완료 인원 표시
- 6개 디자인 썸네일 선택
- 앞·뒷면 카드 미리보기
- 전체 생성 진행률
- 실패 구성원 재시도
- PNG ZIP/PDF 다운로드
- 브라우저 인쇄 또는 프린터 출력
현재 AdminPage의 localStorage 상태 메뉴는 백엔드 상태 전이 규칙을 우회하므로 제거하고, 허용된 명령 버튼 방식으로 교체해야 합니다.
권장 카드 렌더링 방식
서버 측 Java2D 렌더링을 권장합니다.
빈 카드 템플릿
+ 증명사진
+ 이름·영문명·한자
+ 카드번호·주소·발급일
+ 로고·직인
+ 디자인별 좌표·폰트 설정
→ PNG 생성
→ S3 저장
→ cardFrontPath/cardBackPath 기록
장점:
- 관리자·사용자·PDF·프린터에서 같은 결과 사용
- 브라우저/OS 차이 없음
- 기존 S3와 cardFrontPath/cardBackPath 재사용 가능
- golden-image 테스트 가능
추가로 필요한 것:
- 인쇄 가능한 빈 앞·뒷면 템플릿
- 정확한 픽셀 크기와 DPI
- 한글·한자 폰트 파일 및 사용 권한
- 각 디자인의 필드 좌표
- 텍스트 길이 초과 시 축소/줄바꿈 규칙
- 사진 crop 규칙
- bleed/crop mark 여부
- Docker Alpine 이미지에 폰트 설치
현재 프론트의 Canvas 유틸은 이미 생성된 앞·뒷면을 한 장으로 묶는 용도라 다운로드 UX에는 재사용할 수 있지만, 제작 원본 렌더러로 쓰기에는 적합하지 않습니다: [cardDownload.ts (line 51)](/D:/HC-worktrees/main-preview/frontend/src/lib/cardDownload.ts:51)
출력 방식별 난이도
출력 방식	추가 작업	추가 소요일	난이도
PNG 앞·뒤 + 단체 ZIP	기본 범위	포함	중
정확한 카드 크기 PDF	PDFBox 도입, DPI·물리 크기·여백·양면 배치	+3~5일	중
브라우저 window.print()	인쇄 전용 화면·CSS·프린터 설정 안내	+1~3일	중
네트워크 프린터 + Linux 호환 SDK	SDK 어댑터·큐·재시도·상태 조회	+8~15일	상
관리자 PC USB 프린터 + Windows SDK	로컬 출력 에이전트 설치·보안·업데이트·브라우저 통신	+15~30일	매우 상


현재 백엔드는 EC2의 Linux/Alpine Docker에서 실행되므로 관리자 PC에 USB로 연결된 카드 프린터에는 직접 접근할 수 없습니다. 이 경우 브라우저 인쇄를 사용하거나, 관리자 PC에 별도 Windows 출력 에이전트를 설치해야 합니다.
기능별 예상 소요일
개발자 1명, 템플릿 6개 전체, 테스트 포함 기준입니다.
기능	예상
정책·상태·DB 모델 확정	1~2일
관리자 상세 및 구성원 페이지 조회	3~4일
구성원별 검토·수정 요청·재검토	5~7일
작명 저장·완료 검증·UI	3~4일
CardDesign 등록·조회·배정	3~5일
6개 디자인 좌표 렌더러	6~9일
카드번호·앞뒤 이미지 일괄 생성·S3 저장	3~5일
관리자 미리보기·진행률·다운로드 UI	3~5일
회귀·동시성·부분 실패·이미지 기준 테스트	3~5일
합계—PNG/ZIP	약 30~46일


간소화한 MVP로 구성원별 DB 검토 상태를 두지 않고, “전체 반려 → ZIP 전체 재업로드”, 디자인 6개를 코드에 고정하면 약 20~28인일까지 줄일 수 있습니다. 다만 요청하신 “한 명씩 검토하고 잘못된 사람만 추적” 기능은 약해집니다.
이 산정은 “총 6개 디자인” 기준입니다. 문서에 적힌 것처럼 카드 종류마다 6개라서 실제로 24개 디자인을 보정해야 한다면 좌표 조정·이미지 기준 테스트에 약 8~15일이 추가될 수 있습니다.


오후 4:47








너가 확인한 정보는 docs/api/ applciaton.md에 같이 적어줘.