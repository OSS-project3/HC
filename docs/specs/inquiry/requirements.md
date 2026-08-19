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
| `POST /api/inquiries` | 문의 등록. `userId`는 요청 바디에 없음 — JWT(`@AuthenticationPrincipal`)에서 추출(`RULES.md` §3 원칙) | USER |
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
3. `Inquiry` 생성
   - `userId` = JWT `userId`
   - `category`/`name`/`email`/`phone`/`title`/`content` = **요청 바디 값을 그대로 저장**(아래 근거 참고 — `User` 엔티티에서 가져오지 않음)
   - `status` = `PENDING`
   - `answer` = `null`
   - `answeredAt` = `null`
4. 저장
5. 생성 결과 반환

**`name`/`email`/`phone`을 `User`에서 가져오지 않고 요청으로 받기로 확정한 근거**(2026-08-19, 프론트 코드 확인): `InquiryPage.tsx`에서 `name`은 `defaultValue={user?.name}`, `email`은 `defaultValue={user?.email}`로 계정 값이 미리 채워지지만 **평범한 편집 가능 input**이라 사용자가 자유롭게 고쳐 제출할 수 있다. 특히 `email` input의 placeholder는 "답변받을 이메일"이라 로그인 계정 이메일과 의도적으로 달라질 수 있는 값이다. `phone`은 `defaultValue`조차 없이 항상 빈 값에서 시작한다(계정에 전화번호가 없는 사용자도 있을 수 있음). 즉 이 셋은 계정 스냅샷이 아니라 "이 문의 건에 한정된 연락처"라 요청 바디로 받는 것이 프론트 요구사항과 맞다.

### `PATCH /api/admin/inquiries/{id}/answer` — 답변 등록/수정

1. 관리자 권한 확인
2. 요청 검증: `answer` 필수(공백만 있으면 거절)
3. `Inquiry.answer`/`answeredAt` 갱신, `status = COMPLETED`로 전이, DB에 커밋
4. **커밋이 끝난 뒤** 문의자에게 답변 등록 이메일 알림 발송 시도 — 수신 대상은 `Inquiry.email`(문의 등록 시 요청 바디로 저장된 값, 계정 이메일과 다를 수 있음. §⑤ `POST /api/inquiries` 근거와 동일)
5. 이메일 발송은 best-effort로 취급한다: 발송 실패가 답변 저장 자체(2~3단계)를 롤백하거나 이 API의 응답을 실패로 만들지 않는다 — 관리자 입장에서 "답변은 저장됐는데 메일만 실패"를 "답변 저장 자체가 실패"로 오인하게 하지 않기 위함. 실패는 로그로만 남긴다(예외를 삼키되 로깅은 필수).

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
2. `InquiryRepository.findByIdAndUserId(inquiryId, userId)` — 존재 여부와 소유권을 한 쿼리로 동시에 확인
3. 결과 없음 → `INQUIRY_NOT_FOUND`(404)로 응답. **다른 사용자의 문의이든 애초에 존재하지 않는 id든 동일하게 404로 처리한다** — `arch.md` §8.4("존재하지만 권한이 없는 사용자 자원은 정보 노출 방지를 위해 404로 응답할 수 있다") 및 신청 조회(`ApplicationService`)가 이미 쓰고 있는 것과 동일한 패턴. 소유권 불일치를 `403 FORBIDDEN`으로 응답하면 "이 id가 존재하긴 한다"는 사실이 새어나가므로 쓰지 않는다.
4. 결과 있으면 상세 반환

## ⑥ 확정 정책

- **비회원 문의 불가(2026-08-19, 코드 확인으로 해소)**: `InquiryPage.tsx`가 로그인하지 않은 사용자에게 폼을 보여주지 않는다 — `POST /api/inquiries`는 `USER` 권한 전용으로 설계.
- **소유권은 `userId`로 판별(2026-08-19)**: 프론트는 이메일 문자열 비교로 "내 문의"를 가리지만, 로그인이 필수인 이상 백엔드는 이 방식을 재현하지 않는다. `Inquiry.userId`를 `@AuthenticationPrincipal`로 채우고 그 값으로 소유권을 검증한다.
- **검색 API는 이번 범위 아님(2026-08-19)**: 프론트 구조와 동일하게 유지 — 관리자 목록은 전체 나열만 지원, 검색·필터·페이지네이션 파라미터를 추가하지 않는다.
- **답변 등록/수정은 `PATCH .../answer` 하나로 유지(2026-08-19)**: 별도의 "수정" API를 새로 만들지 않는다.
- **문의 제출 후 사용자의 수정·삭제 불가(2026-08-19 확정)**: 사용자가 접수한 문의는 이후 내용을 수정하거나 삭제할 수 없다. 프론트에도 대응하는 UI가 없다(작성 폼만 있고, 상세 페이지에는 수정·삭제 버튼이 없음). `PATCH /api/my/inquiries/{id}`나 `DELETE /api/my/inquiries/{id}` 같은 API는 만들지 않는다.
- **`name`/`email`/`phone`은 `User`가 아니라 요청 바디에서 받는다(2026-08-19 확정)**: §⑤ `POST /api/inquiries` 근거 참고 — 프론트가 계정 값과 다르게 편집해서 보낼 수 있는 필드라 `User` 엔티티 값으로 서버가 덮어쓰지 않는다.
- **소유자 자원 미존재/타인 소유는 동일하게 404(2026-08-19 확정)**: `GET /api/my/inquiries/{id}`에서 `findByIdAndUserId`가 빈 결과면 항상 `INQUIRY_NOT_FOUND`(404) — 403을 쓰지 않아 id 존재 여부 자체가 노출되지 않는다(`arch.md` §8.4, 신청 조회와 동일 패턴).
- **`category`는 enum으로 강제(2026-08-19 확정)**: `InquiryPage.tsx`에 `SelectField`로 문의 유형 선택란이 실제로 존재하고 고정 5개 값(제작 신청/결제 및 배송/카드 발급/행사·단체 협업/기타)만 보낸다 — 자유 문자열이 아니라 닫힌 집합이므로 enum이 맞다. 신규 `common/enums/InquiryCategory.java` 작성 완료(값: `PRODUCTION`/`PAYMENT_AND_SHIPPING`/`CARD_ISSUANCE`/`EVENT_COLLABORATION`/`OTHER`). 프론트는 영문 키가 아니라 한글 문자열을 그대로 보내므로, `@JsonValue`/`@JsonCreator`로 한글 값을 매핑해 **프론트 수정 없이** 백엔드만 enum으로 검증한다(기존 `LookupMethod` enum과 동일 패턴 재사용). 목록/상세 응답도 같은 한글 문자열로 직렬화되어 프론트가 받는 형태는 지금과 동일하다.
- **문의 등록 스팸/남용 방지 없음(2026-08-19 확정)**: 일일 등록 횟수 제한 등은 이번 범위에 넣지 않는다. 필요해지면 신청 도메인의 `ApplicationDailyLimitService` 패턴을 참고해 별도로 추가한다.
- **재문의(추가 질문) 흐름 없음(2026-08-19 확정, 코드 확인)**: `InquiryDetailPage.tsx`에 답변에 대한 재질문·댓글·스레드 UI가 전혀 없다(하단엔 "목록"/"문의하기"만 있고 문의하기는 완전히 새 문의를 여는 링크). 문의 1건당 답변 1회의 단발성 구조로 확정 — 별도 API 불필요.
- **첨부파일 미지원(2026-08-19 확정, 코드 확인)**: `InquiryPage.tsx` 폼에 파일 입력 자체가 없다(`category`/`name`/`email`/`phone`/`title`/`content`/동의 체크박스뿐). 이번 범위에서 제외 — 나중에 필요해지면 Board의 `BoardAttachment`(`UploadFile` join) 패턴을 재사용하면 된다.
- **답변 등록 시 이메일 알림 발송(2026-08-19 확정)**: 관리자가 `PATCH .../answer`로 답변을 저장하면 문의자에게 답변 등록 안내 이메일을 보낸다(`AdminPage.tsx`의 답변 textarea·저장 버튼은 인앱 답변 저장 UI이며 메일 발송 자체가 아니므로 — 별도 알림으로 확정). 기존 `infra/mail/EmailSender`+`common/enums/EmailType`(현재 `SIGNUP_VERIFICATION`/`PASSWORD_RESET`/`PASSWORD_CHANGED`만 존재) 인프라를 재사용해 `EmailType.INQUIRY_ANSWERED`를 추가한다 — `EmailVerificationService`가 이미 쓰고 있는 것과 동일한 패턴. `domain/log/entity/EmailLog`(applicationId 필수 컬럼, PAYMENT_COMPLETE 등 신청 도메인 전용 상수)는 재사용하지 않는다(Inquiry에는 applicationId가 없고, 이 엔티티는 현재 실제로 어디서도 호출되지 않는 미사용 스캐폴딩이라 신뢰할 선례가 아님). 이메일 발송은 답변 저장 트랜잭션과 분리된 best-effort로 처리한다(§⑤ `PATCH .../answer` 처리 흐름 4~5단계 참고) — 발송 실패가 답변 저장 자체를 실패로 되돌리지 않는다.

## 관련 문서

- `docs/FRONTEND_API_GAPS.md` §1.3 — 이 문서와 동일한 내용을 프론트-백엔드 갭 관점에서 요약(향후 이 문서가 source of truth가 되면 §1.3은 이 문서를 가리키도록 정리)
- `docs/collab/TODO.md` 진행 보드 "Inquiry(1:1 문의) 도메인 신규 구현" 행
- `docs/collab/RULES.md` §3 — `userId`는 JWT에서만 추출한다는 공용 원칙
