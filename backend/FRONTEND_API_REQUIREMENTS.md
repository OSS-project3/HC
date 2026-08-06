# 프론트엔드 기준 백엔드 API 요구사항 전수 조사

작성 기준: 2026-08-05 현재 `frontend/src` 전체 라우트, 데이터 모듈, 폼 제출, `localStorage`/`sessionStorage` 사용처를 조사했다. 이 문서는 운영 빌드에서 브라우저 목데이터를 서버 데이터로 교체하기 위해 필요한 API를 정리한다. 경로는 제안이며 실제 구현 시 백엔드 규칙에 맞게 확정해야 한다.

## 1. 결론 및 우선순위

### P0 — 운영 전에 반드시 서버화

1. 일반 이메일 회원가입·로그인·계정 복구
2. 사용자별 신청 목록/상세
3. 관리자 신청 목록/상세/상태 변경
4. 1:1 문의 등록·사용자 조회·관리자 처리
5. 후기 목록/상세/작성/수정/삭제
6. 카드 종류·디자인 카탈로그 조회

### P1 — 관리자 콘텐츠 기능을 여러 사용자/기기에서 공유하려면 필수

1. 공지사항 CRUD
2. FAQ CRUD
3. 이벤트 CRUD
4. 콘텐츠 첨부파일/이미지 업로드

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

회원가입 요청 예시:

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "phone": "01012345678",
  "password": "server-never-logs-this",
  "privacyAgreed": true,
  "imageUploadAgreed": true,
  "shippingAgreed": true
}
```

보안 요구사항:

- 비밀번호는 단방향 강한 해시로만 저장하고 요청/로그/응답에 남기지 않는다.
- 로그인 성공 후 역할은 클라이언트가 보낸 값을 신뢰하지 않고 서버 사용자 정보에서 결정한다.
- 프론트의 데모 관리자 로그인은 운영 빌드에서 제거해야 한다.
- 계정 존재 여부가 복구 API 응답으로 노출되지 않도록 동일한 성공 메시지를 사용한다.
- 로그인 시도 제한, 재설정 토큰 만료·일회성 사용, 이메일/SMS 발송 기록이 필요하다.

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
- `PATCH /api/applications/{id}/photo` — 사진/제출파일 재업로드
- `GET /api/applications/{id}/cards/download` — 카드 다운로드 정보

### 추가 필요

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/my/applications?page=&size=&status=` | 마이페이지 신청 목록 | USER |
| GET | `/api/my/applications/{id}` | 신청 상세, 상태 이력, 결제·배송·카드 정보 | 소유자 |
| POST | `/api/applications/{id}/cancel` | 허용 상태의 신청 취소 | 소유자 |
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
- 프론트 관리자 상태와 백엔드 `ApplicationStatus` 값이 다르다. 서버 enum을 기준으로 UI 매핑표를 확정해야 한다.
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

## 8. 후기 API

현재 `review-posts`에 제목, 내용, 작성자 이름/이메일을 저장한다. 작성자 정보는 요청 값이 아니라 인증 사용자로부터 서버가 설정해야 한다.

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/reviews?page=&size=&searchBy=&keyword=` | 공개 후기 목록/검색 | 없음 |
| GET | `/api/reviews/{id}` | 공개 후기 상세 | 없음 |
| POST | `/api/reviews` | 후기 작성 | USER |
| PATCH | `/api/reviews/{id}` | 본인 후기 수정 | 소유자 또는 ADMIN |
| DELETE | `/api/reviews/{id}` | 본인 후기 삭제 | 소유자 또는 ADMIN |
| GET | `/api/my/reviews` | 마이페이지 내 후기 | USER |

최소 필드: `id`, `title`, `content`, `authorId`, `authorDisplayName`, `createdAt`, `updatedAt`. 이메일은 공개 응답에서 제외한다. 현재 프론트는 수정 권한을 관리자에게만 허용하지만 운영 정책에 따라 작성자 본인 수정·삭제도 지원하는 것이 자연스럽다.

## 9. 공지사항 API

| Method | 제안 경로 | 목적 | 인증 |
|---|---|---|---|
| GET | `/api/notices?page=&size=&keyword=` | 공개 목록 | 없음 |
| GET | `/api/notices/{id}` | 상세 및 첨부파일 | 없음 |
| POST | `/admin/notices` | 작성 | ADMIN |
| PATCH | `/admin/notices/{id}` | 수정 | ADMIN |
| DELETE | `/admin/notices/{id}` | 삭제 또는 비공개 처리 | ADMIN |

필드: `id`, `title`, `content`, `pinned`, `published`, `publishedAt`, `authorId`, `attachments[]`, `createdAt`, `updatedAt`. 현재 data URI로 만드는 TXT 첨부는 실제 업로드 파일 메타데이터와 다운로드 URL로 교체해야 한다.

## 10. FAQ API

| Method | 제안 경로 | 목적 |
|---|---|---|
| GET | `/api/faqs?category=&active=` | 공개 FAQ 목록 |
| POST | `/admin/faqs` | 작성 |
| PATCH | `/admin/faqs/{id}` | 질문·답변·순서·노출 여부 수정 |
| DELETE | `/admin/faqs/{id}` | 삭제 |
| PATCH | `/admin/faqs/reorder` | 표시 순서 일괄 변경 |

필드: `id`, `category`, `question`, `answer`, `displayOrder`, `active`, `createdAt`, `updatedAt`.

## 11. 이벤트 API

현재 이벤트 화면은 `tag`, `title`, `text`만 저장하고 일정·상세·이미지·게시 상태가 없다.

| Method | 제안 경로 | 목적 |
|---|---|---|
| GET | `/api/events?page=&size=&status=` | 공개 이벤트 목록 |
| GET | `/api/events/{id}` | 이벤트 상세 |
| POST | `/admin/events` | 작성 |
| PATCH | `/admin/events/{id}` | 수정 |
| DELETE | `/admin/events/{id}` | 삭제/비공개 |

권장 필드: `id`, `category`, `title`, `summary`, `content`, `thumbnailUrl`, `startAt`, `endAt`, `location`, `applicationUrl`, `status`, `publishedAt`, `createdAt`, `updatedAt`.

## 12. 공통 미디어 API

공지 첨부, 이벤트 썸네일, 카드 디자인, 파트너 로고, 상품 이미지를 CMS에서 관리하려면 공통 파일 API가 필요하다.

| Method | 제안 경로 | 목적 |
|---|---|---|
| POST | `/admin/uploads` | 관리자 파일 업로드 |
| GET | `/admin/uploads/{id}` | 파일 메타데이터 |
| DELETE | `/admin/uploads/{id}` | 참조되지 않는 파일 삭제 |

응답 필드: `fileId`, `originalName`, `contentType`, `size`, `url`, `createdAt`. 서버에서 MIME, 확장자, 크기, 이미지 디코딩 여부를 검증하고 공개/비공개 저장소를 구분해야 한다.

## 13. 선택적 CMS/사이트 설정 API

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

## 14. 서버 API가 필요하지 않은 프론트 상태

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

## 15. 공통 API 규칙

- 목록 API는 `page`, `size`, `sort`와 필터를 지원하고 일관된 페이지 응답을 사용한다.
- 시간은 ISO 8601, 서버 저장은 UTC, 표시만 사용자 시간대로 변환한다.
- 사용자/관리자 쓰기 API는 서버에서 인증·소유권·역할을 다시 검증한다.
- 삭제 데이터의 감사/복구가 필요한 콘텐츠는 soft delete 또는 게시 상태를 사용한다.
- 모든 변경 API는 생성자/수정자와 생성·수정 시각을 기록한다.
- 에러는 현재 `ApiResponse`의 `success`, `data`, `errorCode`, `errorMessage` 형식을 유지한다.
- 개인정보, 비밀번호, 토큰, 업로드 원본 URL은 로그에 남기지 않는다.
- HttpOnly 쿠키 인증을 사용할 때 운영 환경은 HTTPS, `Secure`, 적절한 `SameSite` 및 CSRF 정책을 적용한다.
- 관리자 통계는 프론트에서 전체 목록을 내려받아 계산하지 말고 집계 API에서 계산한다.

## 16. 권장 구현 순서

1. 일반 회원가입/로그인/복구와 서버 권한 검증
2. 카드 종류·디자인 조회 및 신청 요청의 ID 계약 확정
3. 내 신청 목록/상세와 관리자 신청 목록/상태 변경
4. 문의 사용자/관리자 흐름
5. 후기 API
6. 공지·FAQ·이벤트 및 공통 미디어 API
7. 회사 정보·약관·파트너·상품 등 선택적 CMS
8. 프론트에서 목데이터 fallback과 데모 관리자 로그인 제거
