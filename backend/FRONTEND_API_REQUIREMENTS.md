# 프론트엔드 기준 백엔드 API 요구사항 전수 조사

> **후속 문서:** 이 파일은 최초 목 화면에서 백엔드 요구사항을 도출한 조사본이다. 현재 구현 완결성 판정과 프론트 개발용 실제 연동 계약은 `docs/FRONTEND_API_INTEGRATION_SPEC.md`를 따른다. 목데이터/localStorage 사용 자체는 오류가 아니며, 후속 문서는 화면 요구사항을 지원할 백엔드 준비 여부를 기준으로 판정한다.

작성 기준: 2026-08-05 현재 `frontend/src` 전체 라우트, 데이터 모듈, 폼 제출, `localStorage`/`sessionStorage` 사용처를 조사했다. 이 문서는 운영 빌드에서 브라우저 목데이터를 서버 데이터로 교체하기 위해 필요한 API를 정리한다. 경로는 제안이며 실제 구현 시 백엔드 규칙에 맞게 확정해야 한다.

> ✅ 2026-08-17 갱신: 작성 이후 실제로 **구현 완료된 도메인 3개(후기 §8, 공지사항/FAQ §9, 이벤트 §10)** 를 실제 백엔드 코드 기준으로 다시 썼다. 나머지 섹션(마이페이지, 관리자 신청관리, 문의, 카드 카탈로그, CMS)은 여전히 이 문서 작성 시점의 제안 상태 그대로다 — 상세는 각 섹션 상단의 갱신 표시 참고. 실제 최신 계약은 항상 `docs/specs/{도메인}/api.md`(구현된 도메인) 또는 `docs/api/{도메인}.md`(설계만 있는 도메인)를 우선한다 — 이 문서는 "무엇이 왜 필요한가"를 정리한 요구사항 조사본이라 세부 계약의 최신 소스가 아니다.

## 1. 결론 및 우선순위

### P0 — 운영 전에 반드시 서버화

1. 일반 이메일 회원가입·로그인·계정 복구
2. 사용자별 신청 목록/상세
3. 관리자 신청 목록/상세/상태 변경
4. 1:1 문의 등록·사용자 조회·관리자 처리
5. ~~후기 목록/상세/작성/수정/삭제~~ — ✅ 2026-08-13 구현 완료(§8 참고)
6. 카드 종류·디자인 카탈로그 조회 — §4 결정대로 조회 API는 신설하지 않기로 확정(변경 없음)

### P1 — 관리자 콘텐츠 기능을 여러 사용자/기기에서 공유하려면 필수

1. ~~공지사항 CRUD~~ — ✅ 2026-08-14 구현 완료(Board 도메인, §9 참고. 원래 제안했던 `/api/notices`와 다른 경로로 구현됨)
2. ~~FAQ CRUD~~ — ✅ 2026-08-14 구현 완료(공지사항과 동일 Board 도메인, `BoardType=FAQ`)
3. ~~이벤트 CRUD~~ — ✅ 2026-08-16 구현 완료(§10 참고)
4. 콘텐츠 첨부파일/이미지 업로드 — 공통 API로 만들지 않고 Board/Event가 각자 own 첨부 흐름을 가짐(§9·§10 참고, §11는 여전히 미구현)

### P2 — 운영 정책에 따라 CMS화

회사 정보, 대표 인사말, 연혁, 로드맵, 파트너, 상품, 약관, 소셜 링크, 제작 이야기 등의 정적 콘텐츠다. 배포 없이 관리자가 수정해야 한다면 API가 필요하고, 코드 배포로만 수정할 정책이면 정적으로 유지할 수 있다.

## 2. 현재 브라우저 저장소 사용처

| 저장 키 | 현재 데이터 | 운영 시 처리 |
|---|---|---|
| `auth-user` | 이름, 이메일, 역할, 로그인 출처 | 권한 판단에 사용하면 안 됨. `/api/users/me`를 진실의 원천으로 사용하고 로컬 값은 표시 캐시로만 제한 |
| `admin-applications` | 신청 목록과 관리자 변경 상태 | 신청/관리자 API로 완전 교체 |
| `customer-inquiries` | 1:1 문의와 처리 상태 | 문의 API로 완전 교체 |
| `review-posts` | 후기 게시물 | 후기 API로 완전 교체 |
| `managed-content:notices` | 공지사항 CRUD 결과 | 공지 API로 완전 교체 |
| `managed-content:faqs` | FAQ CRUD 결과 | FAQ API로 완전 교체 |
| `managed-content:events` | 이벤트 CRUD 결과 | 이벤트 API로 완전 교체 |
| `application-draft` (`sessionStorage`) | 작성 중 신청 임시 데이터 | 브라우저 탭 임시 저장이므로 유지 가능. 장기 임시저장이 필요하면 draft API 추가 |
| `last-application-lookup` (`sessionStorage`) | 마지막 신청 조회 결과 | 화면 간 전달용이므로 유지 가능. 서버 데이터 원본으로 사용하지 않음 |
| `site-language` | 사용자가 선택한 언어 | 기기별 UI 설정이므로 로컬 유지 가능. 계정 동기화가 필요할 때만 사용자 설정 API 추가 |

## 3. 인증·회원 API

### 현재 실제 구현

- `POST /api/auth/terms`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET/PATCH /api/users/me`
- `POST /api/users/me/withdraw`
- Google/Naver OAuth2 진입점

### OAuth 신규 사용자 약관 라우트 — 확정

백엔드 `OAuth2SuccessHandler`는 기존 회원을 `/`, 신규 OAuth 사용자를 `/terms`로 리다이렉트한다. 프론트에는 현재 `/terms` 라우트가 없어 신규 사용자가 페이지 없음 화면으로 이동하므로 다음 라우트를 추가해야 한다.

| Route | 대상 | 역할 |
|---|---|---|
| `/terms` | 신규 OAuth 사용자 | 개인정보 처리방침 및 이용약관 확인·동의 |

프론트 구현 요구사항:

- `App.tsx`에 `/terms` 라우트와 전용 약관 동의 페이지를 추가한다.
- 이메일 회원가입 화면인 `/signup`으로 대체하거나 합치지 않고 OAuth 신규 사용자용 화면으로 별도 유지한다.
- 개인정보 처리방침과 이용약관의 최종 내용은 아직 확정되지 않았으므로 `[TBD]`로 표시하고 임의의 문구를 최종 정책으로 확정하지 않는다.
- 약관 제출 시 기존 `POST /api/auth/terms`를 사용한다. API URL과 Request/Response 계약은 변경하지 않는다.
- 동의 성공 후 `GET /api/users/me`로 인증 사용자 정보를 갱신하고 홈(`/`)으로 이동한다.
- 기존 OAuth 사용자는 지금처럼 홈(`/`)으로 이동한다.

약관 본문, 정책 버전 및 최종 표시 문구는 정책 확정 후 별도 반영한다.

### 추가 필요

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| POST | `/api/auth/signup` | 일반 이메일 회원가입 | 없음 |
| POST | `/api/auth/login` | 일반 이메일 로그인 및 HttpOnly 토큰 쿠키 발급 | 없음 |
| POST | `/api/auth/email/check` | 이메일 중복 확인 | 없음 |
| POST | `/api/auth/recovery/id` | 이름·전화번호 기반 계정 이메일 안내 | 없음 |
| POST | `/api/auth/recovery/password/request` | 이메일·전화번호 확인 후 재설정 토큰 발송 | 없음 |
| POST | `/api/auth/recovery/password/confirm` | 재설정 토큰으로 새 비밀번호 저장 | 없음 |
| PATCH | `/api/users/me/password` | 로그인 사용자의 비밀번호 변경 | USER |

### 일반 이메일 계정 정책 — 확정

#### 로그인 식별자와 이메일 중복

- 일반 계정의 로그인 아이디는 별도 `username`이 아니라 이메일이다.
- 일반 회원가입, 이메일 중복 확인, 로그인, 아이디 찾기, 비밀번호 재설정, OAuth 로그인에서 동일한 이메일 정규화 규칙을 사용한다.
- 정규화는 앞뒤 공백 제거 후 전체 소문자 변환으로 정의한다.
- 현재 이메일 변경 API는 없지만 이후 추가할 경우에도 같은 정규화 함수와 중복 검증을 반드시 사용한다.
- 회원가입 시 정규화된 이메일을 먼저 DB에서 조회하고, 이미 존재하면 가입을 거절한다.
- DB에도 정규화된 이메일 기준 `UNIQUE` 제약을 적용한다. 애플리케이션의 사전 조회만으로 중복을 보장하지 않으며, 동시 요청의 충돌도 DB 제약으로 최종 차단한다.
- OAuth가 반환한 이메일도 동일하게 정규화한다. 동일한 이메일의 일반 계정 또는 다른 OAuth 계정이 이미 있으면 계정을 자동 연결하거나 병합하지 않고 로그인을 거절한다.
- 중복 시 프론트는 `EMAIL_ALREADY_EXISTS` 오류를 받아 `이미 가입된 이메일입니다.`라고 안내한다. OAuth 로그인 중 발생한 경우에도 로그인 화면에서 같은 내용을 명확히 알린다.

#### 회원가입과 약관 동의

- 일반 이메일 회원가입 요청에는 약관 동의 값을 포함하지 않는다.
- 회원가입 성공 시 백엔드는 기존 OAuth 로그인과 같은 HttpOnly access/refresh token 쿠키를 발급하고, 프론트는 `/terms`로 이동한다.
- `/terms`에서 사용자가 동의 내용을 직접 확인하고 기존 `POST /api/auth/terms`로 제출한다.
- 약관 동의가 완료되기 전 계정은 로그인 상태이더라도 신청 등 필수 약관 동의가 필요한 기능을 사용할 수 없다.
- 약관 동의 성공 후 `GET /api/users/me`로 사용자 정보를 갱신하고 홈(`/`)으로 이동한다.

#### 탈퇴 계정 로그인과 영구 탈퇴

- 소프트 탈퇴 후 7일 유예기간 안에 일반 이메일로 로그인하면 OAuth와 동일하게 계정을 자동 복구한다.
- 자동 복구된 로그인 응답은 프론트가 구분할 수 있는 `restored: true` 값을 제공하고, 프론트는 `탈퇴한 계정이 복구되었습니다.`라고 명확히 안내한다.
- 소프트 탈퇴 후 7일이 지나면 계정을 복구할 수 없는 영구 탈퇴로 처리한다.
- 영구 탈퇴는 현재 구현된 개인정보 익명화와 다르게 실제 계정 삭제를 의미한다. 구현 전 신청·후기·감사 로그 등 기존 데이터의 참조 및 보존 의무에 미치는 영향을 먼저 검증해야 한다.

#### 아이디 찾기와 비밀번호 재설정

- 아이디 찾기는 이름과 전화번호로 계정을 확인하고, 로그인 아이디인 이메일을 마스킹하여 안내한다.
- 비밀번호 재설정 안내는 SMS가 아니라 가입 이메일로 발송한다.
- 비밀번호 재설정 요청은 현재 프론트 입력과 같이 이메일과 전화번호를 받되, 계정 존재 여부를 노출하지 않도록 성공·실패 여부와 관계없이 동일한 접수 응답을 반환한다.
- 재설정 메일에는 만료시간과 일회성 사용을 적용한 토큰을 포함하고, 토큰 확인 후 새 비밀번호를 저장한다.

회원가입 요청 예시:

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "phone": "01012345678",
  "password": "server-never-logs-this"
}
```

보안 요구사항:

- 비밀번호는 단방향 강한 해시로만 저장하고 요청/로그/응답에 남기지 않는다.
- 로그인 성공 후 역할은 클라이언트가 보낸 값을 신뢰하지 않고 서버 사용자 정보에서 결정한다.
- 프론트의 데모 관리자 로그인은 운영 빌드에서 제거해야 한다.
- 계정 존재 여부가 복구 API 응답으로 노출되지 않도록 동일한 성공 메시지를 사용한다.
- 로그인 시도 제한, 재설정 토큰 만료·일회성 사용, 이메일 발송 기록이 필요하다.

## 4. 카드 종류·디자인 API

프론트의 `data/cards.ts`에는 문자열 `designId`, 카드 이미지, 방향, 샘플 표시 정보가 하드코딩되어 있고, `ApplyPage.tsx`는 `cardTypeId`를 `{honorary-korean:1, honorary-citizen:2, visitor:3, student:4}`로 하드코딩해서 신청 API에 그대로 전송한다.

### 결정 — 조회 API 신설 안 함 (2026-08-06)

카드 목록/디자인 표시는 이미 프론트 정적 페이지(`data/cards.ts`)로 처리되고 있어 `GET /api/card-types` 같은 조회 API를 신설할 필요가 없다. 실제 문제는 "프론트가 카드종류를 몰라서"가 아니라 **DB에 자동 생성되는 `CardType.id`가 프론트가 하드코딩한 1~4와 어긋날 수 있다는 것**이었다. 이를 백엔드에서 부팅 시 `CardTypeSeeder`(`domain/card/CardTypeSeeder.java`)가 `HONOR_KOREAN=1, HONOR_CITIZEN=2, VISITOR=3, STUDENT=4` 순서로 최초 1회 시딩하도록 고정했다(이미 데이터가 있으면 아무 것도 하지 않음). 따라서 프론트의 하드코딩된 `cardTypeId` 매핑은 그대로 유지해도 된다.

한국어→영어 등 다국어 표시는 순수 프론트 관심사(코드값 `code`는 이미 안정적인 enum이므로 프론트가 언어별 라벨 사전을 갖고 있으면 됨)이고 백엔드 API가 필요하지 않다. 다만 향후 관리자가 카드 종류명/설명을 언어별로 직접 편집해야 하는 CMS성 요구가 생기면, 그때는 `/admin/card-types` 계열의 콘텐츠 관리 API가 별도로 필요해질 수 있다 — 지금은 범위 밖.

가격은 `CardTypeSeeder`가 `0`(placeholder)으로 시딩하며, 관리자가 실제 가격을 설정할 관리자 API는 아직 없다(Admin 도메인 전체가 [TBD]).

## 5. 신청·마이페이지 API

### 현재 실제 구현

- `POST /api/applications` — 개인 신청
- `POST /api/applications/bulk` — 단체 신청
- `POST /api/applications/lookup` — 신청번호/카드번호 조회
- `POST /api/applications/{id}/cancel` — ✅ 2026-08-17 구현 완료(사용자 취소, 아래 새 섹션 참고)
- `PATCH /api/applications/{id}/photo` — 사진/제출파일 재업로드
- `GET /api/applications/{id}/cards/download` — 카드 다운로드 정보

### 신청 상태·취소·환불 — ✅ 2026-08-17 구현 완료 (관리자 API는 아직 별개, §6 참고)

> `ApplicationStatus` enum이 이 문서 작성 시점(`PAYMENT_PENDING`/`RECEIVED` 포함)과 완전히 달라졌다. 최신 정책은 `docs/specs/application/APPLICATION.md` §16, 최신 계약은 `docs/specs/application/api.md`.

- **`ApplicationStatus` 실제 값**: `SUBMITTED → REVIEWING ↔ PHOTO_REJECTED → NAME_EDITING → PRODUCTION_READY → PRODUCING → COMPLETED`, 각 단계에서 `CANCELLED`로 분기 가능(단 `NAME_EDITING` 이후는 취소 불가). `PAYMENT_PENDING`/`RECEIVED`는 더 이상 없다 — 입금 확인은 `PaymentStatus`(`WAITING`/`CONFIRMED`)만 바꾸고 `ApplicationStatus`는 그대로 둔다(입금 확인 자체로는 상태가 안 바뀜).
- **`POST /api/applications/{id}/cancel`**(요청 본문 없음, 소유자 인증): `SUBMITTED`/`REVIEWING`/`PHOTO_REJECTED`에서만 가능. 이미 `CANCELLED`면 멱등 성공(재취소 시 값 변경 없음). 응답(`ApplicationCancelResponse`): `applicationId`,`status`,`paymentStatus`,`refundRequired`(입금 확인됐고 아직 환불 안 됐으면 `true`),`cancelledAt`.
- **미입금 자동 취소**: 결제 안내 후 `paymentDueAt`(안내 시각+72시간) 지나도 미입금이면 스케줄러(`ApplicationPaymentTimeoutScheduler`, 기본 10분 간격 cron)가 자동 취소한다. 사용자 취소와 구분해 취소 이력에 `cancellationType=SYSTEM`으로 남는다.
- **취소 이력**: `Application`에 `cancelledAt`,`cancellationType`(`USER`/`SYSTEM`/`ADMIN` — `ADMIN`은 아직 API 없음),`cancellationReason`(`USER_REQUEST`/`PAYMENT_TIMEOUT`/`ADMIN_DECISION`) 저장. 자유 형식 취소 사유는 사용자에게 받지 않는다.
- **최소 환불 모델**: 신청당 1회 입금·전액 환불만 지원 — 별도 Refund 엔티티 없이 `Application.refundedAt`(nullable)만 사용. 환불 계좌는 API로 받지 않고 관리자가 별도(운영 절차로) 확인한다.
- **일일 신청 3회 제한과의 연동**: 취소가 최초로 성공하면 그 신청이 차지했던 생성일(KST) 슬롯을 한 번 반환한다(§1 P0 항목과 연동, `ApplicationDailyLimitService.releaseSlot`).

### 마이페이지 — 여전히 미구현

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/my/applications?page=&size=&status=` | 마이페이지 신청 목록 | USER |
| GET | `/api/my/applications/{id}` | 신청 상세, 상태 이력, 결제·배송·카드 정보 | 소유자 |
| GET | `/api/my/bulk-applications/{id}/members` | 단체 신청 구성원/검증 결과 | 소유자 |
| GET | `/api/my/bulk-applications/{id}/cards/download` | 단체 카드 ZIP 다운로드 | 소유자 |

마이페이지 목록 최소 필드:

```json
{
  "content": [
    {
      "applicationId": 1,
      "applicationNumber": "APP-2026-000001",
      "applicationType": "INDIVIDUAL",
      "cardTypeId": 1,
      "cardTypeName": "명예한국인증",
      "quantity": 1,
      "status": "PENDING",
      "paymentStatus": "PENDING",
      "submittedAt": "2026-08-05T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

현재 프론트와 백엔드 계약에서 추가로 확정할 항목:

- 프론트는 신청자 이메일과 입금자명을 수집하지만 신청 DTO에는 없다.
- 프론트는 디자인을 선택하지만 신청 DTO에는 `cardDesignId`가 없다.
- 프론트 관리자 상태와 백엔드 `ApplicationStatus` 값이 다르다. 서버 enum을 기준으로 UI 매핑표를 확정해야 한다 — 2026-08-17부로 서버 enum 자체가 `SUBMITTED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCTION_READY/PRODUCING/COMPLETED/CANCELLED`로 다시 바뀌었으니(위 새 섹션 참고) 기존에 봤던 매핑표가 있다면 다시 확인해야 한다.
- 신청 완료 후 로컬 `admin-applications`에 복사하지 말고 서버 응답과 목록 조회 API를 사용해야 한다.
- 다운로드 URL은 만료 시간이 있으므로 저장하지 말고 클릭 시마다 다시 발급받아야 한다.

### `POST /api/applications/lookup` 인증 정책 — 확정·구현 완료 (2026-08-06)

`LookupPage.tsx` 실제 구현을 기준으로 백엔드 정책을 다음과 같이 확정했고, `ApplicationService.lookup()`에 반영 완료했다. (기존 문서에 "phone/email 중 최소 1개 필수"로 되어 있던 TBD를 대체한다.)

- **`method: "application"`(신청번호 조회, 프론트 탭 "전화번호·이메일")**: 전화번호와 이메일을 **모두** 입력받고, `Applicant.phone`/`Applicant.email`과 **모두** 일치해야 조회된다. 하나만 맞으면 실패로 처리한다.
- **`method: "card"`(카드번호 조회, 프론트 탭 "카드번호")**: 전화번호·이메일 입력란이 아예 없다. **카드번호만으로 조회**하며 별도 본인 인증 값을 요구하지 않는다.
- 카드번호만으로 신원 확인 없이 조회를 허용하는 것이므로, 카드번호 자체의 추측 난이도(형식 `ROK-XXXXX-XXXX`)와 응답에 포함되는 정보(마스킹된 이름, 신청 상태 등 개인정보 성격)를 감안해 보안상 허용 가능한 수준인지는 별도 검토가 필요하다는 점은 여전히 남아있다 — 프론트팀과 같이 재검토 대상.

### UI/API 갭 분석 결정사항 (2026-08-06)

아래 4건은 2026-08-06 UI/API 갭 분석에 대한 최종 결정이다. 프론트를 이어받는 팀은 이 문서 기준으로 작업해야 한다.

1. **단체 신청 파일 파트**: `logo`/`seal`/`submitFile` 3개 별도 파트 방식을 유지한다(`excelFile`+`photoZip` 2파트로 바꾸는 안은 채택하지 않음). 현재 프론트 UI(`ApplyPage.tsx`의 `submit()`)를 그대로 유지하면 된다. 백엔드 `POST /api/applications/bulk`, `PATCH .../photo`(단체 재제출) 모두 3파트 방식으로 구현되어 있다.
2. **단체 재제출**: `PATCH /api/applications/{id}/photo`가 `submitFile` 파트로 단체 전체(엑셀+ZIP) 재제출을 이미 지원하며, `Application.status`가 `PHOTO_REJECTED`(수정 가능한 상태)일 때만 허용한다(그 외 상태는 `INVALID_STATUS_TRANSITION`). 백엔드 구현은 완료 상태 — **프론트에 단체용 재업로드 UI가 없는 것만 남은 문제**(`MobileCardPage.tsx`는 개인 `photo` 파트만 사용). 프론트팀 작업 필요.
3. **사주정보 필수 항목**: 영문명/국적/생년월일/성별 등은 카드종류 4종 전부에 필수(방문증에 한정되지 않음) — 기존 방침 그대로 유지, 백엔드 변경 없음. 지금 방문증 외 3종에서 검증 실패가 안 드러나는 건 프론트가 아직 목데이터로 동작 중이기 때문이며, 실제 API 연동 시 바로 드러난다. **프론트팀이 `StepInfo.tsx`에 나머지 3종도 사주정보 입력폼을 추가해야 한다**(requirements.md §7-3에 이미 기록됨).
4. **`ApplicationMember.englishName`**: 언어 무관하게 신원 표기용 이름 필드로 사용한다(반드시 영문이어야 하는 것은 아님) — 별도 입력란이 없는 카드종류에서 "이름" 필드값을 그대로 전송하는 현재 프론트 동작은 의도된 동작이며 수정 불필요.

### UI/API 갭 분석 추가 발견 — 프론트 수정 필요 (2026-08-06, 2차)

기존 6건과 별개로 코드 레벨로 다시 대조하다가 추가로 발견한 4건. **전부 프론트 수정 필요**(백엔드/ERD는 이미 올바름). 프론트를 이어받는 팀은 이 내용 기준으로 작업.

1. **[차단] 법인·단체 신청에 `organizationName` 입력 UI가 아예 없음.** ERD(`Applicant.organization_name`, `Receiver.organization_name`, `data-model.md` §2.2/§2.3)와 백엔드 `BulkApplicationCreateRequest.ApplicantRequest.organizationName`은 이 값을 요구하는데, `StepInfo.tsx`는 `isVisitor` 여부로만 분기하고 `applicantType`(개인/법인)은 전혀 안 본다. "법인·단체 신청"을 선택해도 Step2(정보입력)에 회사명/단체명 입력란 자체가 없고, 개인용 폼(이름/학번·학과/연락처/이메일)이 그대로 뜬다. 결과: 지금 이 UI로 제출되는 모든 단체 신청은 `organizationName`이 항상 빈 값. 수령인 쪽 `organizationName`/`department`도 동일 문제. **수정 방향: `StepInfo.tsx`가 `applicantType`으로 분기해서 법인 신청 전용 폼(단체명/부서/담당자명/연락처, 수령인도 동일)을 별도로 렌더링해야 한다.**
2. **[차단] 개인 신청 + 명예한국인증/명예시민증 조합은 현재 제출이 100% 불가능.** 원인은 `StepFiles.tsx`가 분기 기준을 잘못 잡은 것 — `applicantType`이 아니라 `cardType`(`isVisitor`/`isStudent`)으로만 분기한다: 방문증→얼굴사진만, 학생증→얼굴사진+학교로고+학교직인, **그 외(명예한국인증/명예시민증)→로고+직인+제출ZIP(얼굴사진 없음)**. 이 "그 외" 분기는 원래 법인용 UI인데 `applicantType`을 안 보기 때문에 "개인 신청"을 선택해도 이 카드종류 2개에서는 그대로 나온다. 반면 `ApplyPage.tsx`의 `submit()`은 `applicantType`으로 분기해서 개인 신청이면 `draft.faceFile`을 요구하는데, 방금 그 UI에서는 `faceFile`을 받은 적이 없어서 API 호출 전에 클라이언트에서 즉시 "본인 사진을 다시 선택해 주세요" 에러가 난다. 개인+방문증, 개인+학생증, 법인+아무 카드종류는 각각 맞는 분기를 타서 정상 동작한다 — 딱 이 조합(개인+명예한국인증/명예시민증)만 막혀 있다. **수정 방향: `StepFiles.tsx`의 분기 기준을 `cardType`이 아니라 `applicantType`으로 바꿔야 한다 — "개인이면 얼굴사진(학생증이면 학교로고/직인 추가), 법인이면 로고/직인/제출ZIP"이 맞는 기준.**
3. **[중간] `member.entryDate`(한국입국날짜) 입력 UI가 개인 신청 흐름에 없음.** 백엔드/ERD는 개인·단체 둘 다 선택 필드로 지원하고(`data-model.md` §2.4 `entry_date`), 단체는 엑셀 템플릿에 "공통 입국날짜"/"개별입국날짜" 컬럼이 있어 동작하지만, 개인 신청은 `ApplicantInfo` 타입(`features/apply/types.ts`) 자체에 해당 필드가 없어서 입력할 방법이 없다. 선택값이라 에러는 안 나지만 기능이 죽어 있다. **수정 방향: `ApplicantInfo`에 `entryDate` 필드 추가하고 `StepInfo.tsx`(비방문증 포함 전체 카드종류)에 입력란 추가, `ApplyPage.tsx` submit()의 `member` 객체에도 포함.**
4. **[낮음] `ApplicantInfo.department` 필드가 법인 "부서"와 학생증 "학과"를 하나로 공유.** `data-model.md` 115행이 "이름만 같음, 혼동 주의"라고 별도로 경고해둔 서로 다른 개념인데 프론트 타입에서는 필드 하나로 합쳐져 있다. `applicantType`/`cardType`을 중간에 바꾸면 이전 값이 남을 여지가 있다. **수정 방향: `ApplicantInfo`에서 `department`(법인 부서용)와 `studentDepartment`(학과용) 등으로 필드를 분리.**

## 6. 관리자 신청 관리 API

현재 `/admin` 화면은 모든 신청 목록, 상태별 통계, 상태 변경을 브라우저 `localStorage`로 처리한다. 서버 역할 검증이 포함된 API가 필요하다.

| Method | 제안 경로 | 목적 |
|---|---|---|
| GET | `/admin/applications` | 페이지, 상태, 유형, 카드 종류, 기간, 이름/번호 검색 |
| GET | `/admin/applications/{id}` | 신청자, 구성원, 파일, 결제, 배송, 상태 이력 상세 |
| PATCH | `/admin/applications/{id}/status` | 허용된 상태 전이 수행 |
| POST | `/admin/applications/{id}/photo-reject` | 사진 반려 사유 등록 |
| POST/PATCH | `/admin/applications/{id}/korean-name` | 한국 이름 등록/수정 |
| POST | `/admin/applications/{id}/issue-card` | 카드 발급 |
| PATCH | `/admin/applications/{id}/tracking` | 배송사·송장번호 등록 |
| GET | `/admin/dashboard/stats` | 전체/상태별 신청 수와 기간 통계 |
| GET | `/admin/applications/{id}/status-history` | 상태 변경 감사 이력 |

상태 변경 요청에는 `targetStatus`, `reason`이 필요하며 서버가 전이 가능 여부와 관리자 권한을 검증해야 한다. 관리자 ID, 변경 전후 상태, 시각을 감사 로그에 남겨야 한다.

## 7. 1:1 문의 API

현재 `InquiryPage`, `MyPage`, `AdminPage`가 `customer-inquiries`를 공유한다.

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| POST | `/api/inquiries` | 문의 접수 | 선택: USER 또는 비회원 |
| GET | `/api/my/inquiries` | 내 문의 목록 | USER |
| GET | `/api/my/inquiries/{id}` | 내 문의와 답변 상세 | 소유자 |
| GET | `/admin/inquiries` | 관리자 문의 목록/검색 | ADMIN |
| GET | `/admin/inquiries/{id}` | 문의 상세 | ADMIN |
| POST | `/admin/inquiries/{id}/answer` | 관리자 답변 등록 및 알림 | ADMIN |
| PATCH | `/admin/inquiries/{id}/status` | `PENDING`, `IN_PROGRESS`, `COMPLETED` 변경 | ADMIN |

문의 필드: `id`, `category`, `requesterUserId`, `name`, `email`, `phone`, `title`, `content`, `status`, `answer`, `answeredBy`, `answeredAt`, `createdAt`, `updatedAt`.

개인정보 동의 시각과 동의한 정책 버전도 서버에 기록해야 한다. 비회원 문의를 허용하면 조회용 인증 절차 또는 이메일 안내만 제공하고 다른 사용자의 문의가 노출되지 않게 해야 한다.

## 8. 후기 API — ✅ 2026-08-13 구현 완료

> 아래는 실제 `ReviewController`/`ReviewService`/DTO 기준(2026-08-17 확인). 최신 소스는 `docs/specs/review/api.md`.

| Method | 실제 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/reviews?cardTypeId=&hasPhoto=&searchType=&keyword=&page=(기본0)&size=(기본9)` | 공개 후기 목록/검색 | 없음(`permitAll`) |
| GET | `/api/reviews/{id}` | 공개 후기 상세 | 없음(`permitAll`, 로그인 시 `canEdit`/`canDelete` 값만 달라짐) |
| POST | `/api/reviews` | 후기 작성(멀티파트: `request` JSON + 선택 `image` 파트 0~1개) | USER |
| PATCH | `/api/reviews/{id}` | 후기 전체 재수정(멀티파트: `request` JSON + 선택 `image`, `removeImage` 플래그로 사진 삭제) | 소유자 또는 ADMIN |
| DELETE | `/api/reviews/{id}` | 후기 삭제(이미지·업로드 파일까지 완전 삭제) | 소유자 또는 ADMIN |

- `GET /api/my/reviews`(마이페이지 내 후기 전용 목록)는 **아직 없음** — 위 목록 API에 로그인 사용자로 좁히는 파라미터가 없어 마이페이지에서 쓰려면 별도 결정 필요(`docs/collab/TODO.md` "내 후기 목록 조회 API" 참고).
- `ReviewCreateRequest`/`ReviewUpdateRequest` 필드: `title`(≤100자), `applicationType`, `cardTypeId`, `authorName`(≤50자, 로그인 이름 자동 아님 — 직접 입력), `content`. `Update`는 여기에 `removeImage`(boolean) 추가.
- 응답(`ReviewDetailResponse`) 필드: `id`,`title`,`content`,`authorName`,`applicationType`,`cardType`(`{id,name}`),`imageUrl`,`createdAt`,`next`(`{id,title}`, 이전/다음 글),`canEdit`,`canDelete`. 목록(`ReviewListItemResponse`)은 `next`/`canEdit`/`canDelete` 없이 나머지 동일.
- 이미지: 최대 2 MiB, `jpg/jpeg/png/webp`만 허용, 시그니처(매직바이트) 검증.
- **본인 후기 여부에 따른 수정/삭제 노출**(2026-08-09 확정): `canEdit`/`canDelete`는 로그인 사용자가 `Review.user_id`와 일치할 때만 `true`. 단건 조회는 비로그인도 가능한 공개 API지만 로그인 여부에 따라 이 값이 달라지므로 백엔드가 `Authorization` 헤더를 선택적으로 파싱한다(없어도 401 아님).
- 작성 자격 제한: 자격 있는 (신청유형, 카드종류) 조합당 후기 1개만 허용(`REVIEW_ALREADY_EXISTS`), 탈퇴 계정은 신규 작성 불가(`ALREADY_WITHDRAWN`, 수정에는 미적용).

## 9. 공지사항/FAQ API — ✅ 2026-08-14 구현 완료 (Board 도메인)

> ⚠️ 원래 이 문서가 제안했던 `/api/notices`·`/api/faqs`(도메인 분리) 경로는 채택되지 않았다. 실제로는 **`Board`+`BoardType{NOTICE,FAQ}` enum 하나로 통합** 구현됐다 — 신규 게시판 종류가 생겨도 테이블 추가 없이 enum 값만 늘리는 구조. 최신 소스는 `docs/specs/board/api.md`.

| Method | 실제 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/boards?type=NOTICE\|FAQ&page=(기본0)&size=(기본9)` | 공개 목록(타입 필수) | 없음(`permitAll`) |
| GET | `/api/boards/{id}` | 공개 상세(첨부파일·다음글 포함) | 없음(`permitAll`) |
| POST | `/api/admin/boards` | 작성(멀티파트: `request` JSON + 선택 `attachments` 0~10개, NOTICE 전용) | ADMIN |
| PATCH | `/api/admin/boards/{id}` | 전체 재수정(멀티파트: `request` JSON + 선택 `attachments`) | ADMIN |
| DELETE | `/api/admin/boards/{id}` | 삭제(첨부파일까지 완전 삭제) | ADMIN |

- FAQ는 첨부파일 개념 자체가 없다 — `boardType=FAQ`인데 첨부파일을 보내면 `INVALID_INPUT`으로 거절(무시 안 함).
- `BoardCreateRequest`/`BoardUpdateRequest` 필드: `boardType`,`title`(FAQ는 질문),`content`(FAQ는 답변). `Update`에는 `keepAttachmentIds`(List<Long>) 추가 — 유지할 기존 첨부파일 id 목록이며, 생략/빈 배열이면 기존 첨부파일 전부 삭제(전체 재제출 원칙).
- 응답(`BoardDetailResponse`) 필드: `id`,`boardType`,`title`,`content`,`createdAt`,`attachments`(`{id,originalFileName,url}[]`),`next`(`{id,title}`, 같은 `boardType` 안에서만). 목록(`BoardListItemResponse`)은 `attachments`/`next` 없이 나머지 동일하며 본문(`content`)도 절삭 없이 그대로 내려준다(FAQ 아코디언이 목록에서 바로 펼쳐지는 화면 구조 때문).
- 첨부파일: 최대 10개, 개당 10 MiB, 확장자 `pdf/hwp/hwpx/doc/docx/xls/xlsx/ppt/pptx/jpg/jpeg/png`(이미지 확장자만 매직바이트 시그니처 검증).
- 관리자 인가는 라우트 레벨(`/api/admin/**` → `ADMIN`)에서만 강제하고 서비스 레벨 중복 검증은 없다 — 리소스 소유권 판단이 필요 없는 "관리자냐 아니냐"뿐이라서(Review의 `canEdit`/`canDelete`와 다른 이유).
- `authorId`/`published`/`pinned` 같은 이 문서가 원래 제안했던 필드는 실제로 없다 — 작성 관리자(`created_by_user_id`)는 내부 감사용으로만 저장하고 공개 응답에 노출하지 않는다. "게시 여부"/"고정" 개념 자체가 요구사항에 없어 구현되지 않았다.

## 10. 이벤트(행사사업) API — ✅ 2026-08-16 구현 완료

> 실제 경로·필드는 이 문서가 원래 제안했던 것과 상당히 다르다 — 부스 운영/법인단체 협업 두 화면을 `EventType{BOOTH,COLLABORATION}` enum 하나로 통합했고, `category`/`status`/`startAt`/`endAt`/`applicationUrl` 같은 필드는 실제 요구사항에 없어 구현되지 않았다. 최신 소스는 `docs/specs/events/api.md`.

| Method | 실제 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/events?type=BOOTH\|COLLABORATION&page=(기본0)&size=(기본10)` | 공개 목록(타입 필수, `visible=true`만) | 없음(`permitAll`) |
| GET | `/api/events/{id}` | 공개 상세(`visible=false`면 `EVENT_NOT_FOUND`로 존재 자체를 숨김) | 없음(`permitAll`) |
| POST | `/api/admin/events` | 작성(멀티파트: `request` JSON + 선택 `thumbnail` 1개 + 선택 `images` 0~10개) | ADMIN |
| PATCH | `/api/admin/events/{id}` | 전체 재수정(멀티파트: `request` JSON + 선택 `thumbnail` 교체) | ADMIN |
| DELETE | `/api/admin/events/{id}` | 삭제(썸네일·갤러리 이미지까지 완전 삭제) | ADMIN |

- `EventCreateRequest`/`EventUpdateRequest` 필드: `eventType`,`title`,`eventDate`(선택, 정렬용),`eventDateText`(화면 표시용, 예 "2026. 12"),`place`,`host`,`cardLabel`(발급 카드 표시값, 예 "명예한국인증 · 방문증"),`content`,`visible`(생략 시 서버가 `true`로 채움),`displayOrder`(선택, 수동 정렬).
- **PATCH는 갤러리(`images`) 편집을 다루지 않는다** — 이번 패스 범위 밖으로 명시적으로 미뤘다. 썸네일만 새 파일 제공 시 교체 가능.
- 응답(`EventDetailResponse`) 필드: `id`,`eventType`,`title`,`eventDate`,`eventDateText`,`place`,`host`,`cardLabel`,`content`,`thumbnailImageUrl`,`images`(`{id,originalFileName,url}[]`, 갤러리, 썸네일은 미포함 — 프론트가 상세 화면에서 `[썸네일, ...갤러리]`로 직접 이어붙이는 구조). 목록(`EventListItemResponse`)은 `images` 대신 `displayOrder`를 내려주고 갤러리는 포함하지 않는다.
- 정렬: `display_order ASC(NULL 맨 뒤) → event_date DESC(NULL 맨 뒤) → created_at DESC` 고정.
- 이미지(썸네일·갤러리 공통): 최대 2 MiB, `jpg/jpeg/png/webp`, 매직바이트 시그니처 검증(Review와 동일 규칙, 별도 컴포넌트).
- **관리자 전용 전체 목록 API(`GET /api/admin/events`, `visible` 무관)는 아직 없음** — 관리자가 숨긴(`visible=false`) 글을 다시 찾을 방법이 현재 없다. 필요성은 확인됐으나 이번 패스에서 제외, 별도 구현 예정.

## 11. 공통 미디어 API

공지 첨부, 이벤트 썸네일, 카드 디자인, 파트너 로고, 상품 이미지를 CMS에서 관리하려면 공통 파일 API가 필요하다.

| Method | 제안 경로 | 목적 |
|---|---|---|
| POST | `/admin/uploads` | 관리자 파일 업로드 |
| GET | `/admin/uploads/{id}` | 파일 메타데이터 |
| DELETE | `/admin/uploads/{id}` | 참조되지 않는 파일 삭제 |

응답 필드: `fileId`, `originalName`, `contentType`, `size`, `url`, `createdAt`. 서버에서 MIME, 확장자, 크기, 이미지 디코딩 여부를 검증하고 공개/비공개 저장소를 구분해야 한다.

> ⚠️ 2026-08-17 확인: 이 범용 API는 만들어지지 않았고, 대신 Board(§9)·Event(§10)가 각자 자기 첨부파일 업로드 흐름을 독립적으로 구현했다(둘 다 파일 검증 규칙은 별도 컴포넌트, `UploadFile` 테이블 재사용 방식은 도메인마다 다름 — Board는 join 엔티티 경유, Event는 S3 key 직접 저장). 앞으로 새 도메인이 파일 업로드가 필요할 때도 이 패턴(도메인별 독립 구현)을 따를 가능성이 높다 — 공통 API로 통합할 계획은 없다.

## 12. 선택적 CMS/사이트 설정 API

다음 프론트 파일은 현재 정적 배열/객체다.

| 프론트 소스 | 콘텐츠 | 제안 API |
|---|---|---|
| `config/company.ts` | 회사명, 연락처, 주소, 사업자/특허, 운영시간, 계좌정보 | `GET /api/site/company`, `PATCH /admin/site/company` |
| `data/partners.ts` | 제휴기관명과 로고 | `GET /api/partners`, 관리자 CRUD/순서 API |
| `data/merchandise.ts` | 상품/문화상품 목록과 이미지 | `GET /api/merchandise`, 관리자 CRUD |
| `data/social.ts` | SNS 링크와 활성 여부 | `GET /api/site/social-links`, 관리자 CRUD/순서 API |
| `data/policies.ts` | 개인정보처리방침, 이용약관, 이메일 수집 거부 | `GET /api/policies/{type}`, 관리자 버전 발행 API |
| `SupportPage.tsx` | 제작 이야기/상담 안내 | `GET /api/site/stories`, 관리자 CRUD |
| `CompanyPage.tsx` | 회사 소개, 약속, 프로세스, 연혁, 로드맵 | `GET /api/site/company-page`, 관리자 수정 API |
| `GreetingsPage.tsx` | 대표 인사말 | `GET /api/site/greeting`, 관리자 수정 API |
| `data/zodiac.ts` | 띠 표시 데이터 | 사업 규칙이면 서버 기준 데이터, 단순 설명이면 정적 유지 |

약관은 일반 CMS 콘텐츠와 달리 `version`, `effectiveAt`, `publishedAt`, `required`를 관리하고 사용자 동의 기록이 어떤 버전을 대상으로 했는지 저장해야 한다. 계좌정보는 공개 범위와 수정 권한을 특히 엄격하게 제한해야 한다.

## 13. 서버 API가 필요하지 않은 프론트 상태

다음은 서버 데이터로 옮기지 않아도 된다.

- 모달 열림/닫힘, 카드 뒤집기, 현재 스텝, 검색창 입력값
- 페이지 스크롤 위치와 캐러셀 순서
- 작성 중 신청 draft의 탭 단위 임시 저장
- 마지막 조회 결과의 화면 간 임시 전달
- 언어 선택의 기기별 저장
- 다음 주소 검색 스크립트 호출 자체
- 버튼 토스트와 로딩/오류 상태

단, 여러 기기에서 작성 중 신청을 이어가는 요구가 생기면 아래 API를 별도 도입한다.

- `POST /api/application-drafts`
- `GET/PATCH/DELETE /api/application-drafts/{id}`

draft에는 파일 원본을 브라우저 저장소에 넣지 말고 서버 임시 업로드 ID만 참조하며 만료 정책을 둔다.

## 14. 공통 API 규칙

- 목록 API는 `page`, `size`, `sort`와 필터를 지원하고 일관된 페이지 응답을 사용한다.
- 시간은 ISO 8601, 서버 저장은 UTC, 표시만 사용자 시간대로 변환한다.
- 사용자/관리자 쓰기 API는 서버에서 인증·소유권·역할을 다시 검증한다.
- 삭제 데이터의 감사/복구가 필요한 콘텐츠는 soft delete 또는 게시 상태를 사용한다.
- 모든 변경 API는 생성자/수정자와 생성·수정 시각을 기록한다.
- 에러는 현재 `ApiResponse`의 `success`, `data`, `errorCode`, `errorMessage` 형식을 유지한다.
- 개인정보, 비밀번호, 토큰, 업로드 원본 URL은 로그에 남기지 않는다.
- HttpOnly 쿠키 인증을 사용할 때 운영 환경은 HTTPS, `Secure`, 적절한 `SameSite` 및 CSRF 정책을 적용한다.
- 관리자 통계는 프론트에서 전체 목록을 내려받아 계산하지 말고 집계 API에서 계산한다.

## 15. 권장 구현 순서

> ✅ 2026-08-17 갱신: 5·6번은 완료됐고, 나머지는 작성 시점 순서 그대로 남아있다.

1. 일반 회원가입/로그인/복구와 서버 권한 검증
2. 카드 종류·디자인 조회 및 신청 요청의 ID 계약 확정
3. 내 신청 목록/상세와 관리자 신청 목록/상태 변경
4. 문의 사용자/관리자 흐름
5. ~~후기 API~~ — ✅ 완료(§8)
6. ~~공지·FAQ·이벤트 API~~ — ✅ 완료(§9·§10). 공통 미디어 API(§11)는 도메인별 독립 구현으로 대체돼 더 이상 필요하지 않음.
7. 회사 정보·약관·파트너·상품 등 선택적 CMS
8. 프론트에서 목데이터 fallback과 데모 관리자 로그인 제거
