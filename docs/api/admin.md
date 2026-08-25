## Admin 도메인 — 신청·결제 흐름 확정 (2026-08-17)

> ⚠️ **정정(2026-08-25) — 아래 상태전이 API의 경로명이 실제 구현과 다르다.** 실제 `AdminApplicationController` 엔드포인트(전부 구현·프론트 연동됨):
> - 결제 확인: `POST /api/admin/applications/{id}/confirm-payment` · 검토 시작: `.../start-review` · 작명 승인: `.../approve-naming`
> - 사진 반려: `.../reject-photo` · 작명 완료: `.../complete-naming` · 제작 시작: `.../start-producing` · 카드 발급: `.../card-ready` · 배송 발송: `.../dispatch`(body `{trackingNumber}`)
> - 작명 저장: **`POST /api/admin/applications/{applicationId}/members/{memberId}/name`**, body `NameAssignRequest { name, hanja, reading, meaning }` (문서의 `application-members/{id}/name` + `{chineseName,...}` 아님)
> - 신규: `POST .../{id}/naming-result`(작명결과 엑셀 업로드), `POST /api/admin/applications/export`(xlsx 다운로드), `GET .../{id}/members`
> - 목록 응답은 `applicantName/applicantPhone` 없이 `cardTypeId`+`cardTypeName`+`paymentStatus` 포함.
> - 미구현: `GET /api/admin/stats`(통계 집계)만. 상세는 `docs/FRONTEND_API_GAPS.md` §1.4. 아래는 낡은 설계 기록.

> 참고 화면(대학교 학생증 관리자 UI 스크린샷)은 **확정 UI 아님 — 구조 참고용 목업.** 실제 화면은 아래 흐름을 기준으로 새로 설계.

### 신청 상태 흐름

```
SUBMITTED + WAITING
   │  결제 안내: paymentGuidedAt 기록, paymentDueAt=안내 시각+72시간
   │  입금 확인: ApplicationStatus 유지, PaymentStatus만 CONFIRMED
   ▼
SUBMITTED + CONFIRMED
   │  관리자 검토 시작
   ▼
REVIEWING + CONFIRMED
   ├─ 반려 → PHOTO_REJECTED(사진반려) → 사용자 재업로드 → REVIEWING(복귀)
   └─ 승인 → NAME_EDITING(작명·편집중)
                │  관리자가 ApplicationMember별 이름/한자/뜻/풀이 입력
                ▼ (전원 작명 완료)
            PRODUCTION_READY(제작 승인 대기)
                ▼ 관리자 제작 승인
            PRODUCING(카드 제작중) → 카드 준비/실물 인계 기준에 따라 COMPLETED
```

### 확정된 스코프 / 설계 원칙

- `MOBILE`은 카드 파일 생성 완료 시 `COMPLETED`, `MOBILE_AND_PHYSICAL`은 택배사 인계 시 `COMPLETED`로 처리한다. 배송사·운송장·배송 중·배송 완료 상태는 저장하지 않는다.
- **단체(GROUP) 신청은 구성원별이 아니라 Application 전체 단위로 검토/발급/작명 진행.** 일부만 반려돼도 Application 전체가 `PHOTO_REJECTED`. 전원 통과해야 `NAME_EDITING`, 전원 작명 완료해야 `PRODUCING` 진행. → `ApplicationMember`별 개별 status 불필요.
- **"승인" 액션은 이제 `NAME_EDITING`으로 감 (`PRODUCING`으로 바로 안 감).** 예전에 "승인 시 작명 여부를 검사해서 막을지" 질문드렸던 건 이 상태 분리로 해결됨 — 별도 검사 불필요.
- **최초 결제 안내 후 72시간 미입금 시 자동취소** — 기본 10분 주기이며 설정으로 변경 가능하다.
- 관리자 직접 취소는 이번 구현 범위에서 제외한다.
- **사진 재업로드** — ✅ 확정, Application 도메인 API 4로 설계 완료(로그인 필수, 본인 확인)

### ✅ 이름 작명 방식 확정 (2026-07-31)

**사주(만세력) 프로그램은 URL 링크아웃일 뿐, 실제 작명은 전부 수동.** 백엔드가 그 서비스를 API로 호출하지 않음.

```
관리자가 화면에서 대상자의 nationality/birth_time/birth_region/gender/birth_date 확인
   ↓
관리자가 사주 사이트(고정 URL) 새 탭에서 열어서 참고
   ↓
(그 사이트에서 직접 확인한 이름 후보를 보고)
   ↓
관리자가 우리 시스템에 이름/한자/뜻/풀이를 직접 타이핑 → 저장 API
```

### ② 프론트 화면 분석

| 파일 | 현재 동작 |
|---|---|
| `AdminPage.tsx` | `adminMock.ts` 정적 데이터로 목록 테이블만 표시(신청번호/구분/카드종류/신청인/연락처/수량/상태/접수일) + 통계 4개. 서버 호출 없음 |
| 상세/처리 화면 | 코드에 없음 — 이전 스크린샷은 참고 목업, 실제 액션 버튼도 없음 |
| `adminMock.ts`의 `AdminStatus` | 옛 enum이라 교체 필요 |

### ③ 필요한 API 목록

1. 신청 목록 조회
2. 신청 상세 조회
3. 결제 안내(`paymentGuidedAt`, `paymentDueAt` 기록)
4. 입금 확인(PaymentStatus만 `CONFIRMED`, 멱등)
5. 사진·내용 검토 시작 및 승인/반려
6. 이름 작명·편집 저장 및 편집 완료
7. 제작 승인·카드 준비·실물 인계
8. 환불 완료 기록

⚠️ **[TBD] 7번째 액션 — 카드 디자인 배정 API 필요 (2026-07-31 신규 이슈).** `docs/specs/application/requirements.md` 6절 확정으로 `Application.card_design_id`는 이제 사용자가 아니라 관리자가 채우는데, 위 6개 API 어디에도 이 값을 채우는 액션이 없음. 신청 접수 직후/사진검토 통과 후/작명 단계 중 **언제** 배정하는지부터 정해야 API로 설계 가능 — 아직 미설계.

### API 1 / 6 — 신청 목록 조회 ⚠️ 확인필요 — `adminMock.ts`와 필드 구성은 유사하나 실제 서버 호출 없음, `status`/검색 필터는 신규

#### ④ Request/Response 설계

```
GET /api/admin/applications?status={optional}&cardTypeId={optional}&keyword={optional}&page=0&size=20
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "applicationId": 1,
        "applicationNumber": "APP-2026-000131",
        "applicationType": "GROUP",
        "cardType": "명예한국인증",
        "applicantName": "홍길동",
        "applicantPhone": "010-1234-5678",
        "totalQuantity": 100,
        "status": "PRODUCING",
        "createdAt": "2026-07-18"
      }
    ],
    "totalElements": 6,
    "totalPages": 1,
    "page": 0
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- `keyword`는 신청번호/신청인 이름/연락처 통합 검색으로 추정(기존 `AdminPage.tsx`엔 검색 UI가 없어서 새로 추가하는 개념 — 스크린샷의 검색창 참고)

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| applicationId/applicationNumber/applicationType/status/totalQuantity/createdAt | Application.* |
| cardType | Application.card_type_id → CardType.name |
| applicantName/applicantPhone | Applicant.name/phone |

#### ⑦ 누락된 필드 확인

없음 — 기존 `adminMock.ts` 필드 구성과 거의 그대로 대응됩니다(`status` enum만 교체).

**API 1 완료.**

---

### API 2 / 6 — 신청 상세 조회 ⚠️ 확인필요 — 상세/처리 화면 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

```
GET /api/admin/applications/{applicationId}?memberPage=0&memberSize=20
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicationNumber": "APP-2026-000131",
    "applicationType": "GROUP",
    "status": "REVIEWING",
    "paymentStatus": "CONFIRMED",
    "issueType": "MOBILE",
    "cardType": "명예한국인증",
    "cardDesignId": 3,
    "totalQuantity": 100,
    "totalPrice": 3000000,
    "photoRejectReason": null,
    "createdAt": "2026-07-18T10:00:00",
    "applicant": {
      "name": "홍길동",
      "email": "hong@example.com",
      "phone": "010-1234-5678",
      "organizationName": "OO기업",
      "department": "인사팀"
    },
    "receiver": {
      "name": "김수령",
      "phone": "010-9999-8888",
      "address": "서울특별시 강남구 ...",
      "detailAddress": "101동 202호"
    },
    "files": {
      "logoUrl": "https://.../logo.png",
      "sealUrl": "https://.../seal.png",
      "submitFileUrl": "https://.../submit.zip"
    },
    "payment": {
      "depositorName": "홍길동",
      "confirmedAt": null
    },
    "members": {
      "content": [
        {
          "applicationMemberId": 101,
          "name": null,
          "englishName": "Kim Minjun",
          "chineseName": null,
          "nameMeaning": null,
          "nameInterpretation": null,
          "photoUrl": "https://.../photos/1.jpg",
          "nationality": "US",
          "birthDate": "1995-03-12",
          "birthTime": "14:30",
          "birthRegion": "New York",
          "gender": "MALE",
          "entryDate": "2026-08-15",
          "email": "kim@example.com",
          "phone": "010-1111-2222",
          "studentId": null,
          "department": null,
          "address": "...",
          "cardNumber": null,
          "issueDate": null
        }
      ],
      "totalElements": 100,
      "totalPages": 5,
      "page": 0
    }
  }
}
```
⚠️ 2026-07-31 갱신: `members[]`에 `entryDate`/`email`/`phone`(✅ 신규) + `studentId`/`department`(✅ 신규, `cardType`이 학생증이 아니면 항상 `null`) 추가. `cardDesignId`는 관리자가 아직 배정 안 했으면 `null`(위 예시는 이미 배정된 상태를 보여줌).

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| 비로그인 | `UNAUTHORIZED` | 401 |

- `members`는 단체 신청이 최대 수백 명일 수 있어 페이지네이션(`memberPage`/`memberSize`) — 개인 신청은 항상 1건이라 `totalElements=1`

#### ⑥ DB 컬럼과 매핑 검증

| Response 필드 | 출처 |
|---|---|
| Application 최상위 필드들 | `Application.*` |
| applicant.* | `Applicant.*` |
| receiver.* | `Receiver.*` |
| files.* | `Application.logo_file_id/seal_file_id/submit_file_id` → `UploadFile.file_path` |
| payment.* | `Payment.depositor_name/confirmed_at` |
| members[].* | `ApplicationMember.*` (photo_path → photoUrl로 변환) |

#### ⑦ 누락된 필드 확인

⚠️ 2026-07-31 갱신: `members[].*`가 `ApplicationMember.*` 전체를 가리키는 와일드카드 매핑이라 `entry_date`/`email`/`phone`/`student_id`/`department` 신규 컬럼도 별도 수정 없이 자동 반영됨.

**API 2 완료.**

---

### API 3-A — 결제 안내 ⚠️ Service 구현 완료, HTTP API 구현 전

```http
POST /api/admin/applications/{applicationId}/payment-guide
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```

최초 호출 시 서버 시각을 `paymentGuidedAt`에 기록하고 `paymentDueAt=paymentGuidedAt+72시간`으로 설정한다. 재호출은 기존 시각과 기한을 변경하지 않는 멱등 성공이다.

자동 취소 스케줄러는 기본 10분 주기로 `SUBMITTED + WAITING + paymentDueAt<=now`를 조회하며 실행 주기는 설정값으로 변경할 수 있다.

백엔드 Service는 구현 완료됐다. 최초 안내만 시각과 기한을 기록하며 재호출은 값을 변경하지 않는다. 결제 안내는 `AdminActivityLog`에 별도 기록하지 않는다.

---

### API 3-B — 입금 확인 ⚠️ Service 구현 완료, HTTP API 구현 전

#### ④ Request/Response 설계

```
POST /api/admin/applications/{applicationId}/confirm-payment
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음 — `Payment.depositor_name`을 관리자가 이미 상세화면에서 보고 통장 대조 후 확인 버튼만 누르는 액션)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "SUBMITTED",
    "paymentStatus": "CONFIRMED"
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| `Application.status`가 `SUBMITTED`, `CANCELLED` 중 하나가 아님 | `INVALID_STATUS_TRANSITION` | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

별도 `Payment` row 존재 여부를 입금 확인의 선행조건으로 사용하지 않는다. Source of Truth인 `APPLICATION.md` §16에 따라 `Application.paymentStatus`만 실제 입금 확인 이력으로 관리한다. 최초 `WAITING → CONFIRMED` 변경에만 `AdminActivityLog.PAYMENT_CONFIRMED`를 한 건 기록하고, 중복 확인은 추가 로그 없이 멱등 성공한다.

#### ⑥ DB 컬럼과 매핑 검증

| — | 변경되는 컬럼 |
|---|---|
| — | `Application.status`: 변경하지 않음 (`SUBMITTED` 또는 자동 취소 후 `CANCELLED` 유지) |
| — | `Application.payment_status`: `WAITING` → `CONFIRMED` |
| — | `Payment.confirmed_at` = 현재 시각 |

#### ⑦ 누락된 필드 확인

이미 `CONFIRMED`이면 값을 변경하지 않고 `200 OK` 멱등 성공으로 응답한다. 자동 취소 후 늦은 입금이면 `CANCELLED + CONFIRMED + refundedAt=null` 환불 대상으로 남긴다.

**API 3 완료.**

---

### API 4 / 6 — 사진 검토 (승인/반려) ⚠️ 확인필요 — 액션 버튼 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

**승인**
```
POST /api/admin/applications/{applicationId}/approve-photo
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음)

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationId": 1, "status": "NAME_EDITING" } }
```

**반려**
```
POST /api/admin/applications/{applicationId}/reject-photo
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: application/json
```
```json
{ "reason": "사진이 흐려서 식별이 어렵습니다. 선명한 사진으로 다시 올려주세요." }
```

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationId": 1, "status": "PHOTO_REJECTED" } }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| `Application.status != REVIEWING` | `INVALID_STATUS_TRANSITION` | 400 |
| 반려 시 `reason` 누락/공백 | `INVALID_INPUT` | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

✅ 승인은 `PRODUCING`이 아니라 `NAME_EDITING`으로 전이(사진검토/작명 분리 확정 반영).

#### ⑥ DB 컬럼과 매핑 검증

| — | 변경되는 컬럼 |
|---|---|
| 승인 | `Application.status`: `REVIEWING` → `NAME_EDITING` |
| 반려 | `Application.status`: `REVIEWING` → `PHOTO_REJECTED`, `Application.photo_reject_reason` = 입력한 사유 |

#### ⑦ 누락된 필드 확인

없음.

**API 4 완료.**

---

### API 5 / 6 — 이름 작명 저장 + 작명 완료 처리 ⚠️ 확인필요 — 작명 화면 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

**구성원별 작명 저장** (인원마다 반복 호출)
```
PATCH /api/admin/application-members/{applicationMemberId}/name
Cookie: accessToken={JWT}  (role=ADMIN 필요)
Content-Type: application/json
```
```json
{
  "name": "임별하",
  "chineseName": null,
  "nameMeaning": "별처럼 높은 곳에서 세상을 밝게 비추고...",
  "nameInterpretation": "선한 영향력을 널리 행사하며, 맑고 순수한 성품을 유지한 채 꿈을 향해 꿋꿋하게 나아가는 이름."
}
```
(`chineseName`은 선택 — 한자이름 없는 사람은 `null`)

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationMemberId": 101, "name": "임별하" } }
```

**작명 완료 처리** (Application 전체, 전원 작명 확인 후)
```
POST /api/admin/applications/{applicationId}/complete-naming
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음)

**Response `200 OK`**
```json
{ "success": true, "data": { "applicationId": 1, "status": "PRODUCING" } }
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationMemberId`/`applicationId` 없음 | `NOT_FOUND` | 404 |
| 부모 `Application.status != NAME_EDITING` | `INVALID_STATUS_TRANSITION` | 400 |
| 작명 저장 시 `name`/`nameMeaning`/`nameInterpretation` 누락 | `INVALID_INPUT` | 400 |
| 작명 완료 처리 시, 구성원 중 `name`이 NULL인 사람이 1명이라도 있음 | `NAMING_NOT_COMPLETE`(신규 코드) | 400 |
| 비로그인 | `UNAUTHORIZED` | 401 |

✅ 2026-07-31 설계 방향: 저장(PATCH)과 완료 처리(POST)를 분리 — 관리자가 인원별로 저장하면서 검토하다가, **전원 다 채운 걸 확인한 뒤 명시적으로 "작명 완료"를 눌러야** `PRODUCING`으로 넘어감(자동 전이 아님). 입금확인/사진승인과 같은 패턴.

#### ⑥ DB 컬럼과 매핑 검증

| Request | 엔티티.컬럼 |
|---|---|
| name/chineseName/nameMeaning/nameInterpretation | `ApplicationMember.name/chinese_name/name_meaning/name_interpretation` |
| — | 작명완료 처리 시 `Application.status`: `NAME_EDITING` → `PRODUCING` |

#### ⑦ 누락된 필드 확인

없음.

**API 5 완료.**

---

### API 6 / 6 — 카드 발급 ⚠️ 확인필요 — 액션 버튼 자체가 프론트에 없음(신규)

#### ④ Request/Response 설계

```
POST /api/admin/applications/{applicationId}/issue-cards
Cookie: accessToken={JWT}  (role=ADMIN 필요)
```
(body 없음 — `PRODUCING` 상태의 신청을 대상으로 전 구성원 카드 이미지를 일괄 생성)

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "status": "COMPLETED",
    "issuedCount": 100
  }
}
```

#### ⑤ Validation

| 상황 | errorCode | HTTP |
|---|---|---|
| `role != ADMIN` | `FORBIDDEN` | 403 |
| `applicationId` 없음 | `NOT_FOUND` | 404 |
| `Application.status != PRODUCING` | `INVALID_STATUS_TRANSITION` | 400 |
| 구성원 중 `name`이 NULL(작명 안 됨) — 이론상 API 5에서 이미 막혔어야 함 | `NAMING_NOT_COMPLETE` | 400 |
| `CardDesign.template_front_id`/`template_back_id`가 없음(디자인 등록이 안 된 카드종류) | `NOT_FOUND` | 404 |

#### ⑥ DB 컬럼과 매핑 검증

구성원(`ApplicationMember`)별로:

| 처리 | 컬럼 |
|---|---|
| `CardDesign.template_front_id`/`template_back_id` + `CardFieldDefinition`(config) 좌표에 `name`/`english_name`/`photo_path`/`card_number`/`address`/`issue_date`/캐릭터(계산값)/`chinese_name`/`name_meaning`/`name_interpretation` 합성 | 결과 이미지 생성 |
| — | `ApplicationMember.card_number` = 신규 채번, ✅ 2026-07-31 정정: `ROK-XXXXX-XXXX`(5자리-4자리) 형식 — `시안.zip` 실물 카드번호(`ROK-35777-2105` 등) 확인, `HN-KR-YYMM-NNNN`은 틀린 정보였음(취소) |
| — | `ApplicationMember.issue_date` = 오늘 날짜 |
| — | `ApplicationMember.card_front_path`/`card_back_path` = 생성된 이미지 경로 |

전 구성원 처리 끝나면 `Application.status`: `PRODUCING` → `COMPLETED`

#### ⑦ 누락된 필드 확인

형식은 `ROK-XXXXX-XXXX`로 확정. **채번 로직(순차 발급 시퀀스인지, 무작위인지)은 미결정 사항으로 분류** — 시안 이미지의 숫자만 봐서는 날짜 인코딩 패턴이 안 보임(35777/13575/63153/35115/64889/85165 — 규칙성 없어 보임). 순차 발급(예: `MAX(RIGHT(card_number,4))+1`) vs 무작위 생성 중 어느 쪽인지 결정 필요 → 아래 "미결정 사항" 참고.

**API 6 완료.**

---

## Admin 도메인 정리

| # | API | 상태 |
|---|---|---|
| 1 | `GET /api/admin/applications` (신청 목록 조회) | 설계 완료 |
| 2 | `GET /api/admin/applications/{id}` (신청 상세 조회) | 설계 완료 |
| 3 | `POST /api/admin/applications/{id}/confirm-payment` (입금 확인) | 설계 완료 |
| 4 | `POST /api/admin/applications/{id}/approve-photo` \| `reject-photo` (사진 검토) | 설계 완료 |
| 5 | `PATCH /api/admin/application-members/{id}/name` + `POST .../complete-naming` (작명) | 설계 완료 |
| 6 | `POST /api/admin/applications/{id}/issue-cards` (카드 발급) | 설계 완료 |

Admin 도메인 완료 — User/Application/Payment/카드/Admin 5개 도메인, 총 21개 API 설계 끝났습니다.

---
