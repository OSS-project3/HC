# Inquiry(1:1 문의) 요구사항 · 정책

> 백엔드에 도메인 자체가 아직 없다(`Inquiry` 엔티티/Controller 전무, `docs/collab/TODO.md` 진행 보드 참고). 이 문서는 착수 전 정책을 계속 확정해나가는 running 문서다 — 실제 API 요청/응답 JSON, DB 컬럼 상세는 구현 착수 시 `api.md`/`data-model.md`로 분리한다(Board/Review 도메인과 동일 패턴).

## ① 도메인의 책임

- 로그인 사용자가 1:1 문의를 접수한다.
- 관리자가 접수된 문의를 조회하고 답변을 등록한다.
- 사용자는 본인이 접수한 문의의 목록·상세와 답변을 확인한다.

## ② 프론트 실제 구조 (기준, 2026-08-19 코드 확인)

| 파일 | 구조 |
|---|---|
| `pages/InquiryPage.tsx` | 문의 작성 폼. **로그인 필수**(`if (!user)`면 폼 대신 로그인 유도 화면만 노출). 입력 필드: 문의 유형(`category`, 고정 5개 값 중 선택), 이름(`name`, 기본값 로그인 사용자 이름), 이메일(`email`, 기본값 로그인 사용자 이메일), 연락처(`phone`), 제목(`title`, 최대 80자), 문의 내용(`content`, 최대 2000자), 개인정보 수집 동의 체크박스(필수, 서버에 전송하지 않는 UI 전용 게이트). 제출 시 `status: "PENDING"`으로 로컬 저장. |
| `pages/InquiryDetailPage.tsx` | 문의 상세. 접근 제어는 현재 `user.email === inquiry.email \|\| user.role === "admin"`(이메일 문자열 비교) — 백엔드는 이 방식을 재현하지 않고 `userId` 기준으로 소유권을 검증한다(§⑤ 참고). 상태 배지(답변 대기/문의 완료), 문의 내용, 답변(있으면) 표시. |
| `pages/MyPage.tsx` | "문의 내역" 섹션. `inquiry.email === user.email`로 필터링(→ 백엔드는 `userId`로 대체)해 제목+상태+접수일만 목록 표시, 클릭 시 상세로 이동. |
| `pages/AdminPage.tsx` | 관리자 문의 테이블. 컬럼: 문의유형/제목(클릭 시 펼쳐서 내용+답변 textarea+저장 버튼 노출)/문의자/이메일/연락처/접수일/처리상태(드롭다운). **검색·페이지네이션 UI 없음**(전체 나열). 답변 textarea는 `value={draft ?? inquiry.answer ?? ""}`로 신규 작성·기존 답변 수정을 같은 UI로 처리한다. 처리상태 드롭다운은 답변 저장 버튼과 **별개**로 존재해 `PENDING`/`COMPLETED`를 답변 내용과 무관하게 직접 전환할 수 있다. |
| `data/inquiries.ts` | `localStorage["customer-inquiries"]`. `InquiryRecord`: `id, category, name, email, phone, title, content, createdAt, status("PENDING"\|"COMPLETED"), answer?, answeredAt?` |

## ③ 확정된 서비스 흐름 (2026-08-19)

```
[사용자 로그인]
      ↓
1:1 문의 작성
      ↓
JWT에서 userId 추출
      ↓
문의 내용 검증
      ↓
Inquiry 저장 (status = PENDING)
      ↓
마이페이지에서 본인 문의 목록/상세 조회
      ↓
관리자가 관리자 페이지에서 문의 조회
      ↓
관리자 답변 등록 → status = COMPLETED, answer/answeredAt 저장
      ↓
사용자가 답변 확인
```

프론트 코드(§②) 전수 대조로 이 흐름이 실제 화면 동작과 일치함을 확인했다.

## ④ 확정된 API 목록 (2026-08-19)

| 메서드/경로 | 용도 | 인증 |
|---|---|---|
| `POST /api/inquiries` | 문의 등록. `userId`는 요청 바디에 없음 — JWT(`@AuthenticationPrincipal`)에서 추출(`RULES.md` §3 원칙). `privacyConsent: true` 필수(⚠️ 프론트 추가 전송 필요, §⑤ 근거 참고) | USER |
| `GET /api/my/inquiries` | 내 문의 목록(호출자 `userId` 기준) | USER |
| `GET /api/my/inquiries/{id}` | 내 문의 상세(소유자만) | USER |
| `GET /api/admin/inquiries` | 관리자 전체 목록. **검색 파라미터 없음** — 프론트에 검색 UI 자체가 없어(§②) 이번 범위 밖. 필요해지면 Board/Review의 `keyword`/`searchType` 패턴 재사용 | 관리자 |
| `GET /api/admin/inquiries/{id}` | 관리자 상세 | 관리자 |
| `PATCH /api/admin/inquiries/{id}/answer` | 답변 등록/수정(신규·수정 동일 API, `AdminPage.tsx`와 동일하게). 성공 시 `status=COMPLETED`로 함께 전이 + 문의자에게 답변 등록 이메일 알림 발송 | 관리자 |
| `PATCH /api/admin/inquiries/{id}/status` | 답변과 무관하게 상태만 독립 변경(`PENDING`↔`COMPLETED`) — `AdminPage.tsx`의 별도 상태 드롭다운 동작을 그대로 유지하기 위해 신설 | 관리자 |

`POST /api/admin/inquiries/{id}/answer`가 아니라 `PATCH`인 이유: 기존 리소스(답변 필드)를 갱신하는 동작이라 `PATCH`가 더 정확하고, 프론트도 신규/수정을 같은 UI·같은 호출로 처리한다.

## ⑤ API별 상세 처리 흐름 (2026-08-19 확정)

### `POST /api/inquiries` — 문의 등록

1. JWT에서 `userId` 추출(로그인 필수 — 미인증이면 401)
2. 요청 검증(Bean Validation): `category`/`name`/`email`/`phone`/`title`/`content` 전부 필수, `email` 형식, `title` 최대 80자, `content` 최대 2000자, `phone` 형식(기존 `UserUpdateRequest.phone`과 동일 정규식 재사용 검토)
3. `privacyConsent`가 `true`가 아니면 거절(`INVALID_INPUT`, 400) — 아래 "개인정보 동의 서버 검증" 근거 참고
4. `Inquiry` 생성
   - `userId` = JWT `userId`
   - `category`/`name`/`email`/`phone`/`title`/`content` = **요청 바디 값을 그대로 저장**(아래 근거 참고 — `User` 엔티티에서 가져오지 않음)
   - `status` = `PENDING`
   - `answer` = `null`
   - `answeredAt` = `null`
   - `privacyConsent` 값 자체는 컬럼으로 저장하지 않는다(검증 게이트일 뿐, 감사 기록 목적이 아님 — 아래 근거 참고)
5. 저장
6. 생성 결과 반환

**개인정보 동의 서버 검증이 필요한 근거(2026-08-19 확정)**: 기존 버전은 개인정보 수집·이용 동의 체크박스를 "서버에 전송하지 않는 UI 전용 게이트"로 규정했었다. 그러나 브라우저 UI에서만 막으면 API를 직접 호출해 체크박스를 우회하고 동의 없이 `name`/`email`/`phone`을 저장할 수 있다 — 개인정보 보호법상 필수 동의 항목(수집·이용 목적, 항목, 보유기간, 거부권 고지)을 서비스 정책으로 요구한다면 이 우회 가능성은 정책적으로 약하다. 따라서 `privacyConsent: boolean`을 요청 바디에 추가하고 서버에서 `true`인지 검증한다(`@AssertTrue`). ⚠️ **프론트 의존성**: 현재 `InquiryPage.tsx`의 체크박스 상태(`agreed`)는 제출 버튼 비활성화에만 쓰이고 `FormData`에는 전혀 포함되지 않는다 — 이 필드가 백엔드에 추가되면 프론트가 `privacyConsent` 값을 요청 바디에 실어 보내도록 **같이 고쳐야 API가 동작한다**(안 고치면 매 요청이 400으로 거절됨). 프론트는 내 담당 범위가 아니므로 코드는 건드리지 않고, `docs/FRONTEND_API_GAPS.md` §1.3에 이 의존성을 명시적으로 기록해 프론트 담당자에게 전달한다.

**`name`/`email`/`phone`을 `User`에서 가져오지 않고 요청으로 받기로 확정한 근거**(2026-08-19, 프론트 코드 확인): `InquiryPage.tsx`에서 `name`은 `defaultValue={user?.name}`, `email`은 `defaultValue={user?.email}`로 계정 값이 미리 채워지지만 **평범한 편집 가능 input**이라 사용자가 자유롭게 고쳐 제출할 수 있다. 특히 `email` input의 placeholder는 "답변받을 이메일"이라 로그인 계정 이메일과 의도적으로 달라질 수 있는 값이다. `phone`은 `defaultValue`조차 없이 항상 빈 값에서 시작한다(계정에 전화번호가 없는 사용자도 있을 수 있음). 즉 이 셋은 계정 스냅샷이 아니라 "이 문의 건에 한정된 연락처"라 요청 바디로 받는 것이 프론트 요구사항과 맞다.

### `PATCH /api/admin/inquiries/{id}/answer` — 답변 등록/수정

1. 관리자 권한 확인
2. 요청 검증: `answer` 필수(공백만 있으면 거절)
3. 갱신 직전 `inquiry.getAnswer() == null`을 판정해 "최초 등록"인지 "수정"인지 구분해둔다(이메일 발송 여부 결정용)
4. `Inquiry.answer`/`answeredAt` 갱신, `status = COMPLETED`로 전이, DB에 커밋
5. **커밋이 끝난 뒤, 3단계에서 최초 등록으로 판정된 경우에만** 문의자에게 답변 등록 이메일 알림 발송 시도 — 수신 대상은 `Inquiry.email`(문의 등록 시 요청 바디로 저장된 값, 계정 이메일과 다를 수 있음. §⑤ `POST /api/inquiries` 근거와 동일). **수정인 경우 이메일 발송 자체를 스킵한다**(§⑥ "답변 수정 시 이메일 재발송 안 함" 정책).
6. 이메일 발송은 best-effort로 취급한다: 발송 실패가 답변 저장 자체(3~4단계)를 롤백하거나 이 API의 응답을 실패로 만들지 않는다 — 관리자 입장에서 "답변은 저장됐는데 메일만 실패"를 "답변 저장 자체가 실패"로 오인하게 하지 않기 위함. 실패는 로그로만 남긴다(예외를 삼키되 로깅은 필수).

### `GET /api/my/inquiries` — 내 문의 목록

1. JWT에서 `userId` 추출
2. `InquiryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)` 호출 — 이메일 매칭을 서버에서도 재현하지 않고 처음부터 `userId` 컬럼으로 조회
3. 조회 결과를 그대로 반환 — 프론트는 추가 필터링 없이 받은 목록을 그대로 표시(현재 `inquiries.filter(inquiry => inquiry.email === user.email)`처럼 클라이언트 사이드로 다시 거르지 않는다)

```
JWT
 ↓
userId = 15
 ↓
SELECT * FROM inquiry WHERE user_id = 15 ORDER BY created_at DESC
```

### `GET /api/my/inquiries/{id}` — 내 문의 상세

1. JWT에서 `userId` 추출
2. `InquiryRepository.findById(inquiryId)` — 존재 여부 확인, 없으면 `INQUIRY_NOT_FOUND`(404)
3. `inquiry.userId != userId`면 `FORBIDDEN`(403)
4. 결과 있으면 상세 반환

⚠️ **정정(2026-08-19, 구현 착수 직전 코드 재확인으로 발견)**: 이전 버전은 "타인 소유는 404로 통일 — `ApplicationService`가 이미 쓰는 패턴"이라고 적었으나 사실이 아니었다. 실제 `ApplicationService.getMyApplicationDetail()`(`findById` → `APPLICATION_NOT_FOUND`, 그 다음 `isOwnedBy` 실패 시 별도로 `FORBIDDEN`)은 존재/소유권 판정을 분리해 403을 쓴다 — 404로 통일하는 패턴이 아니다. `arch.md` §8.4는 "정책에 따라 404로 응답**할 수 있다**"는 선택지일 뿐 의무가 아니다. 이 프로젝트의 유일한 실제 "마이페이지 소유권" 선례(`MyApplicationController`)와의 일관성을 위해 Inquiry도 존재 확인(404)과 소유권 확인(403)을 분리하는 쪽으로 정정한다.

## ⑥ 확정 정책

- **비회원 문의 불가(2026-08-19, 코드 확인으로 해소)**: `InquiryPage.tsx`가 로그인하지 않은 사용자에게 폼을 보여주지 않는다 — `POST /api/inquiries`는 `USER` 권한 전용으로 설계.
- **소유권은 `userId`로 판별(2026-08-19)**: 프론트는 이메일 문자열 비교로 "내 문의"를 가리지만, 로그인이 필수인 이상 백엔드는 이 방식을 재현하지 않는다. `Inquiry.userId`를 `@AuthenticationPrincipal`로 채우고 그 값으로 소유권을 검증한다.
- **검색 API는 이번 범위 아님(2026-08-19)**: 프론트 구조와 동일하게 유지 — 관리자 목록은 전체 나열만 지원, 검색·필터·페이지네이션 파라미터를 추가하지 않는다.
- **답변 등록/수정은 `PATCH .../answer` 하나로 유지(2026-08-19)**: 별도의 "수정" API를 새로 만들지 않는다.
- **문의 제출 후 사용자의 수정·삭제 불가(2026-08-19 확정)**: 사용자가 접수한 문의는 이후 내용을 수정하거나 삭제할 수 없다. 프론트에도 대응하는 UI가 없다(작성 폼만 있고, 상세 페이지에는 수정·삭제 버튼이 없음). `PATCH /api/my/inquiries/{id}`나 `DELETE /api/my/inquiries/{id}` 같은 API는 만들지 않는다.
- **`name`/`email`/`phone`은 `User`가 아니라 요청 바디에서 받는다(2026-08-19 확정)**: §⑤ `POST /api/inquiries` 근거 참고 — 프론트가 계정 값과 다르게 편집해서 보낼 수 있는 필드라 `User` 엔티티 값으로 서버가 덮어쓰지 않는다.
- **자원 미존재는 404, 타인 소유는 403(2026-08-19 정정)**: `GET /api/my/inquiries/{id}`는 `INQUIRY_NOT_FOUND`(404, 존재 자체가 없음)와 `FORBIDDEN`(403, 존재하지만 내 것이 아님)을 분리한다. `MyApplicationController`의 실제 구현(`ApplicationService.getMyApplicationDetail`)과 동일한 패턴 — 앞서 "동일하게 404"로 적었던 건 확인 없이 잘못 기록한 것이었다(§⑤ `GET /api/my/inquiries/{id}` 정정 내용 참고).
- **`category`는 enum으로 강제(2026-08-19 확정)**: `InquiryPage.tsx`에 `SelectField`로 문의 유형 선택란이 실제로 존재하고 고정 5개 값(제작 신청/결제 및 배송/카드 발급/행사·단체 협업/기타)만 보낸다 — 자유 문자열이 아니라 닫힌 집합이므로 enum이 맞다. 신규 `common/enums/InquiryCategory.java` 작성 완료(값: `PRODUCTION`/`PAYMENT_AND_SHIPPING`/`CARD_ISSUANCE`/`EVENT_COLLABORATION`/`OTHER`). 프론트는 영문 키가 아니라 한글 문자열을 그대로 보내므로, `@JsonValue`/`@JsonCreator`로 한글 값을 매핑해 **프론트 수정 없이** 백엔드만 enum으로 검증한다(기존 `LookupMethod` enum과 동일 패턴 재사용). 목록/상세 응답도 같은 한글 문자열로 직렬화되어 프론트가 받는 형태는 지금과 동일하다.
- **문의 등록 스팸/남용 방지 없음(2026-08-19 확정)**: 일일 등록 횟수 제한 등은 이번 범위에 넣지 않는다. 필요해지면 신청 도메인의 `ApplicationDailyLimitService` 패턴을 참고해 별도로 추가한다.
- **재문의(추가 질문) 흐름 없음(2026-08-19 확정, 코드 확인)**: `InquiryDetailPage.tsx`에 답변에 대한 재질문·댓글·스레드 UI가 전혀 없다(하단엔 "목록"/"문의하기"만 있고 문의하기는 완전히 새 문의를 여는 링크). 문의 1건당 답변 1회의 단발성 구조로 확정 — 별도 API 불필요.
- **첨부파일 미지원(2026-08-19 확정, 코드 확인)**: `InquiryPage.tsx` 폼에 파일 입력 자체가 없다(`category`/`name`/`email`/`phone`/`title`/`content`/동의 체크박스뿐). 이번 범위에서 제외 — 나중에 필요해지면 Board의 `BoardAttachment`(`UploadFile` join) 패턴을 재사용하면 된다.
- **답변 등록 시 이메일 알림 발송(2026-08-19 확정)**: 관리자가 `PATCH .../answer`로 답변을 저장하면 문의자에게 답변 등록 안내 이메일을 보낸다(`AdminPage.tsx`의 답변 textarea·저장 버튼은 인앱 답변 저장 UI이며 메일 발송 자체가 아니므로 — 별도 알림으로 확정). 기존 `infra/mail/EmailSender`+`common/enums/EmailType`(현재 `SIGNUP_VERIFICATION`/`PASSWORD_RESET`/`PASSWORD_CHANGED`만 존재) 인프라를 재사용해 `EmailType.INQUIRY_ANSWERED`를 추가한다 — `EmailVerificationService`가 이미 쓰고 있는 것과 동일한 패턴. `domain/log/entity/EmailLog`(applicationId 필수 컬럼, PAYMENT_COMPLETE 등 신청 도메인 전용 상수)는 재사용하지 않는다(Inquiry에는 applicationId가 없고, 이 엔티티는 현재 실제로 어디서도 호출되지 않는 미사용 스캐폴딩이라 신뢰할 선례가 아님). 이메일 발송은 답변 저장 트랜잭션과 분리된 best-effort로 처리한다(§⑤ `PATCH .../answer` 처리 흐름 4~5단계 참고) — 발송 실패가 답변 저장 자체를 실패로 되돌리지 않는다.
- **답변 수정 시에는 이메일 재발송하지 않는다(2026-08-19 확정)**: `PATCH .../answer`는 신규 등록·기존 수정에 같은 API를 쓰지만(§④), 이메일 발송은 **최초 등록에만** 트리거한다. 판정 기준은 갱신 직전 `Inquiry.answer`가 `null`이었는지 여부 — 이미 값이 있던 상태에서 다시 저장(수정)하면 상태는 그대로 `COMPLETED`로 유지·저장은 되지만 이메일은 보내지 않는다. 문의자가 사소한 오탈자 수정마다 메일을 반복 수신하지 않게 하기 위함.
- **`COMPLETED`인데 `answer`가 없는 상태도 유효하다(2026-08-19 확정)**: 전화 상담 등으로 답변이 이뤄질 수 있어(§② `AdminPage.tsx`의 상태 드롭다운이 답변 저장 버튼과 완전히 독립적으로 존재하는 이유) `PATCH .../status`만으로 `COMPLETED` 전이가 가능해야 한다. 서비스·Validation 어디에도 "`COMPLETED`면 `answer != null`이어야 한다"는 불변식을 걸지 않는다 — `answer`(내용)와 `status`(처리 여부)는 완전히 독립된 두 필드로 유지한다.
- **탈퇴 회원의 Inquiry 보존 및 재가입 시 비연결(2026-08-19 확정)**: `Inquiry.userId`는 `User`와 JPA 연관관계·FK가 없는 순수 `Long` 참조(arch.md §5.1 원칙, `Application.userId`와 동일 방식)이므로 회원 탈퇴가 `Inquiry`에 어떤 cascade도 일으키지 않는다 — 별도 구현 불필요, 설계상 자동 보장. 실제 탈퇴 흐름(`User.withdraw()`→`UserStatus.WITHDRAWN`, 유예기간 후 `User.anonymize()`)도 로우를 삭제하지 않고 PII만 익명화하므로(`User.java` 확인 완료) `Inquiry.userId`가 가리키는 id 자체는 계속 유효하게 남는다. 파생 정책: (1) 탈퇴 후에도 문의는 §⑧ 6개월 보유기간까지 그대로 보존, (2) 관리자는 탈퇴 여부와 무관하게 계속 조회·답변 가능, (3) 답변 알림은 `Inquiry.email`(요청 바디 스냅샷)로 발송되므로 탈퇴 후에도 발송 가능, (4) 탈퇴 회원은 로그인 자체가 막히므로(`withdraw()` 시 액세스 토큰 블랙리스트 처리 — `UserControllerTest.withdrawMarksUserWithdrawnAndBlacklistsAccessToken`로 기존에 이미 검증됨) `GET /api/my/inquiries*`로 본인 문의를 더 이상 조회할 수 없다, (5) 재가입 시 신규 `User` row가 새 `id`로 생성되므로(기존 익명화 row는 남아있고 재사용되지 않음) 과거 `Inquiry.userId`와 새 계정의 `userId`가 다르다 — 자동 연결 로직을 별도로 만들지 않는다(설계상 자연히 비연결).

## ⑦ Validation 규칙 (2026-08-19 확정)

| 필드 | 규칙 | 근거/재사용 |
|---|---|---|
| `category` | 필수 (`InquiryCategory` enum) | §⑥ category enum 정책 |
| `name` | 필수, 최대 100자 | `ApplicationCreateRequest.Applicant.name`(`@Size(max=100)`)과 동일 — 계정용 `User.name`(255)이 아니라 신청 건별 연락처 이름 성격이 더 가까움 |
| `email` | 필수, 이메일 형식, 최대 255자 | `SignupRequest`/`UserUpdateRequest` 등 기존 이메일 필드 전부 `@Email @Size(max=255)` 패턴과 동일 |
| `phone` | 필수, 형식 검증 | ⚠️ 재사용 가능한 공용 `ValidPhone` 어노테이션은 실제로 존재하지 않는다(코드 확인 완료) — `SignupRequest`/`UserUpdateRequest`가 각자 `@Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")`를 필드마다 직접 붙이는 방식. Inquiry도 동일 정규식을 그대로 복붙해 재사용한다(신규 공용 어노테이션 추출은 이번 범위 아님 — 재사용처가 3곳째라 향후 별도로 추출을 검토할 만하지만, 지금 당장은 기존 관례를 따른다) |
| `title` | 필수, 1~80자 | `@NotBlank @Size(max=80)` — 프론트 `maxLength={80}`과 일치(`InquiryPage.tsx`) |
| `content` | 필수, 1~2000자 | `@NotBlank @Size(max=2000)` — 프론트 `maxLength={2000}`과 일치 |
| `answer`(관리자 `PATCH .../answer` 요청) | 필수, 최대 5000자 | `@NotBlank @Size(max=5000)` |
| `privacyConsent` | 필수, `true`만 허용 | `@AssertTrue` — §⑤ "개인정보 동의 서버 검증" 근거 참고. ⚠️ 프론트가 이 필드를 아직 전송하지 않음(추가 전송 필요) |

## ⑧ 개인정보 보유기간 (2026-08-19 확정)

- **Inquiry는 `createdAt` 기준 6개월 보관 후 파기 대상**이다. 문의 접수(`createdAt`) 시점으로부터 6개월이 지나면 파기 대상으로 분류한다(`createdAt + 6개월 → 파기 대상`).
- **`status`와 무관하게 `createdAt` 하나만 기준(2026-08-19 명확화)**: 아직 `PENDING`(답변 대기 중)인 문의라도 `createdAt` 기준 6개월이 지나면 동일하게 파기 대상이다 — "답변 완료 후 6개월"이 아니라 "접수 후 6개월"이며, 미답변 상태라고 보존 기간이 늘어나거나 예외 처리되지 않는다.
- ⚠️ 확인 결과, 저장소의 현재 개인정보처리방침 텍스트(`frontend/src/data/policies.ts` "제3조")는 "수집·이용 목적 달성 후 지체 없이 파기, 법령상 필요시 해당 기간 보관"이라는 범용 placeholder 문구이며("본 방침은 예시 문구이며 실제 배포 전 최종 검토가 필요합니다"라고 자체 명시), "상담일로부터 6개월"이라는 구체적 문구는 이 저장소 안에 없다. 이 정책은 그 placeholder 문구와 모순되지 않으므로 백엔드 확정 정책으로 그대로 채택한다 — 다만 실제 배포 전에는 프론트 정책 텍스트도 이 기간을 명시하도록 정리가 필요하다(프론트 담당자 영역이라 이 문서에 기록만 해둔다).
- **파기 방식은 이번 범위에서 구현하지 않는다** — `docs/api/user.md`에 이미 있는 "완전탈퇴 배치 스케줄러 구현 필요" 항목과 같은 인프라 작업으로 묶어 나중에 배치 스케줄러로 처리하는 걸 전제로 정책만 먼저 확정해둔다. 착수 시점엔 `createdAt < now - 6개월`인 `Inquiry` 로우를 주기적으로 하드 삭제(또는 별도 파기 로그를 남기고 삭제)하는 배치 잡을 신설한다.

## ⑨ 구현 순서 체크리스트 (2026-08-19 작성 → 2026-08-19 전 단위 완료)

> ✅ **INQUIRY-1~5 전부 구현·테스트·커밋·푸시 완료**(2026-08-19). 6개 API 전부 동작한다. 커밋: INQUIRY-1 `1abab25`, INQUIRY-2 `0b08b41`, INQUIRY-3 `3cb647f`, INQUIRY-4 `f877d2d`, INQUIRY-5 `a9abff7`. 전체 스위트 472개 중 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing, 이 도메인과 무관) 1건만 실패, 회귀 없음.

단위 내부 순서는 이 세션 전체에서 지켜온 절차를 그대로 따랐다: **정책 재확인 → 실패하는 테스트 먼저 작성 → 최소 구현 → 해당 단위 테스트 통과 확인 → (기능 묶음 완료 시점에) 전체 스위트 회귀 테스트**.

### INQUIRY-1. 도메인 기반 + 문의 등록 — `POST /api/inquiries` (`1abab25`)

- [x] `domain/inquiry/entity/Inquiry.java`(엔티티, `create`/`isOwnedBy`/`answer`/`changeStatus`), `domain/inquiry/repository/InquiryRepository.java`
- [x] `domain/inquiry/dto/InquiryCreateRequest.java`(§⑦ Validation 전체 적용, `privacyConsent` 포함), `InquiryCreateResponse.java`
- [x] `domain/inquiry/service/InquiryService.java`의 `create(userId, request)`
- [x] `api/InquiryController.java`(`POST /api/inquiries`만)
- [x] 테스트: `InquiryTest`, `InquiryServiceTest.create*`, `InquiryControllerTest`
- [x] `docs/FRONTEND_API_GAPS.md` §1.3에 "`privacyConsent` 프론트 전송 필요" 의존성 기록 — **아직 프론트가 반영하지 않음(오픈 상태), 아래 "남은 오픈 아이템" 참고**

### INQUIRY-2. 내 문의 목록/상세 — `GET /api/my/inquiries`, `GET /api/my/inquiries/{id}` (`0b08b41`)

- [x] `ErrorCode.INQUIRY_NOT_FOUND` 추가
- [x] `InquiryListItemResponse.java`, `InquiryDetailResponse.java`
- [x] `InquiryService`에 `listMine(userId)`, `getMineDetail(userId, inquiryId)`(404→403 순서 분리)
- [x] `api/MyInquiryController.java`
- [x] 테스트: 본인 목록만 반환, 정렬 확인, 상세 성공, 미존재 404, 타인 소유 403, 비로그인 401

### INQUIRY-3. 관리자 목록/상세 — `GET /api/admin/inquiries`, `GET /api/admin/inquiries/{id}` (`3cb647f`)

- [x] `InquiryService`에 `listAdmin()`, `getAdminDetail(inquiryId)`
- [x] `api/InquiryAdminController.java`(GET 2개)
- [x] 테스트: 전체 목록 반환, 미존재 404, USER 토큰 403

### INQUIRY-4. 답변 등록 — `PATCH /api/admin/inquiries/{id}/answer` (`f877d2d`)

- [x] `common/enums/EmailType`에 `INQUIRY_ANSWERED` 추가
- [x] `InquiryAnswerRequest.java`
- [x] `InquiryService.answer(inquiryId, answer)` — 최초 등록만 커밋 후 best-effort 이메일, 수정 시 스킵
- [x] `InquiryAdminController`에 PATCH 추가
- [x] 테스트: 상태전이, 최초 등록 시 발송, 수정 시 미발송, 발송 실패해도 저장 유지, 공백 거절 400, 미존재 404

### INQUIRY-5. 상태 독립 변경 — `PATCH /api/admin/inquiries/{id}/status` (`a9abff7`)

- [x] `InquiryStatusUpdateRequest.java`
- [x] `InquiryService.changeStatus(inquiryId, status)`
- [x] `InquiryAdminController`에 PATCH 추가
- [x] 테스트: 답변 없이 `COMPLETED` 전환 가능(`answer=null` 유지 확인), 미존재 404

### 남은 오픈 아이템(체크리스트 범위 밖, 완료 아님)

- **프론트 `privacyConsent` 전송 미반영**: `POST /api/inquiries`가 `privacyConsent: true`를 요구하는데 `InquiryPage.tsx`는 아직 이 필드를 보내지 않는다 — 프론트 담당자가 반영하기 전까지는 실제 연동 시 매 요청이 400으로 거절된다(`docs/FRONTEND_API_GAPS.md` §1.3에 기록됨).
- §⑧ 개인정보 6개월 파기 배치 — `docs/api/user.md`의 "완전탈퇴 배치 스케줄러"와 묶어 별도 인프라 작업으로, 정책만 확정된 상태.
- `docs/specs/inquiry/api.md`/`data-model.md` 분리 — Board/Review 관례상 구현 완료 시점에 이 문서(`requirements.md`)에서 분리하는 절차가 남아있음(선택, 다음 세션에 진행 가능).

## 관련 문서

- `docs/FRONTEND_API_GAPS.md` §1.3 — 이 문서와 동일한 내용을 프론트-백엔드 갭 관점에서 요약(향후 이 문서가 source of truth가 되면 §1.3은 이 문서를 가리키도록 정리)
- `docs/collab/TODO.md` 진행 보드 "Inquiry(1:1 문의) 도메인 신규 구현" 행
- `docs/collab/RULES.md` §3 — `userId`는 JWT에서만 추출한다는 공용 원칙
