# API 검증·테스트 리포트 (2026-08-25)

프론트-백엔드 **전체 API의 연결 검증 + 라이브 동작 테스트**. 정적 매핑(코드 대조)과 라이브 호출(실기동 스택 curl)을 병행했다.

- **검증 방식**: ① 정적 — 백엔드 컨트롤러 엔드포인트 ↔ 프론트 `services/api.ts` 호출 대조. ② 라이브 — 도메인별 5개 테스팅 서브에이전트가 실기동 스택(backend·db·redis·minio·frontend, docker compose)에 `curl`로 인증/미인증 호출, HTTP 코드·응답·DB 반영 확인.
- **계정**: USER `demo@test.com`/`demo1234!` (DB 직접 시드, id=3), ADMIN `admin@test.com`/`admin1234!` (DemoDataSeeder).
- **대상**: 백엔드 컨트롤러 엔드포인트 **64개** 전부.

---

## 0. 한눈에 보기

| 구간 | 결과 |
|---|---|
| **정적 연결(프론트↔백엔드)** | ✅ **64 / 64 연결** (모든 백엔드 엔드포인트가 `api.ts`에서 호출됨. `bulk`는 `createApplication`이 동적 생성) |
| **라이브 테스트(71개 케이스/64 엔드포인트)** | ✅ **61** · ⚠️ **9** · ❌ **1** |
| **실제 결함(수정 필요)** | 🔴 **1건** — `PATCH /api/reviews/{id}` 후기 수정이 실사용 payload에서 항상 400 |
| 인가 경계(미인증 401 / 소유권·권한 403) | 전 도메인 정상 |
| 상태 가드·검증(400 / 404 / 409) | 전 도메인 정상 |

> ⚠️ 9건은 대부분 **결함이 아님**: 로컬 SMTP 미설정(503)·`type` 파라미터 표기·유효 테스트 자산(얼굴사진/ZIP/정상 엑셀) 부재로 2xx 성공경로만 미검증·후기 작성 자격정책(403). 상세는 §3.

---

## 1. 정적 연결 검증 (64/64)

백엔드 `*Controller.java`의 모든 `@{Get,Post,Patch,Delete}Mapping`을 추출해 프론트 `services/api.ts`의 호출 경로와 대조했다.

- 백엔드 엔드포인트 **64개** (직전 61개 + 2026-08-25 추가한 상태전이 3개: `confirm-payment`·`start-review`·`approve-naming`).
- **64개 전부** `api.ts`에 대응 함수가 있고, 그 함수를 실제 화면(pages/components)이 호출함.
- 유일한 표기 예외: `POST /api/applications/bulk`은 `createApplication(form, true)`가 `` `/api/applications${bulk ? "/bulk" : ""}` ``로 동적 생성(연결됨).
- `api.refresh` 헬퍼는 미사용이나, `refresh` 엔드포인트 자체는 `request()`의 401 자동재시도(raw fetch)가 호출 → 엔드포인트는 연결됨(헬퍼만 데드코드).

---

## 2. 도메인별 라이브 테스트 결과

### 2.1 인증(Auth) + 회원(User) — ✅15 / ⚠️1 / ❌0

| 엔드포인트 | 메서드 | 프론트연결 | 라이브(HTTP) | 판정 |
|---|---|---|---|---|
| /api/auth/login | POST | LoginPage | 200 / 틀린비번 401 `INVALID_CREDENTIALS` | ✅ |
| /api/auth/email/check | POST | SignupPage | 200 `{exists}` | ✅ |
| /api/auth/signup/email-verification/request | POST | SignupPage | **503 `EMAIL_DELIVERY_FAILED`** (로컬 SMTP 없음) | ⚠️ |
| /api/auth/signup/email-verification/confirm | POST | SignupPage | 400 `INVALID_VERIFICATION_CODE` | ✅ |
| /api/auth/signup | POST | SignupPage | 400 `INVALID_SIGNUP_TOKEN` | ✅ |
| /api/auth/recovery/id/request · /id/confirm | POST | AccountRecoveryPage | 200(requestId)·429 레이트리밋 / 코드오류 400 | ✅ |
| /api/auth/recovery/password/request · /confirm | POST | AccountRecoveryPage | 200 / 코드오류 400 | ✅ |
| /api/auth/refresh | POST | `request()` 인터셉터 | 200(쿠키有) / 401(쿠키無) | ✅ |
| /api/auth/logout | POST | AuthContext | 200 / 미인증 401 | ✅ |
| /api/auth/terms | POST | TermsPage | 200 / 재동의 409 / 미인증 401 | ✅ |
| /api/users/me | GET | AuthContext | 200 / 401 | ✅ |
| /api/users/me | PATCH | MyPage | 200(name·phone 반영) | ✅ |
| /api/users/me/password | PATCH | MyPage | 200 / 8자미만 400 / 현재비번틀림 400 | ✅ |
| /api/users/me/withdraw | POST | MyPage | 200(임시계정 하드삭제 확인) / 401 | ✅ |

임시계정(`throwaway1@test.com`)으로 비번변경→재로그인→탈퇴(DB row 0건) 전 과정 검증. demo/admin 무손상.

### 2.2 신청(Application) — ✅18 / ⚠️5 / ❌0

| 엔드포인트 | 메서드 | 라이브(HTTP) | 판정 |
|---|---|---|---|
| /api/applications (개인) | POST | 400 `INVALID_IMAGE`(1×1 png 얼굴 미검출 — 검증 파이프라인 정상 도달) | ⚠️ |
| /api/applications/bulk (단체) | POST | 400(직인/ZIP 무효) | ⚠️ |
| /api/applications/lookup | POST | 200(이름 마스킹) / 불일치 404 / phone누락 400 | ✅ |
| /api/my/applications (+/{id}) | GET | 200(본인) / 타인 403 / 미인증 401 | ✅ |
| /api/applications/{id}/cancel | POST | 200(본인→CANCELLED) / 타인 403 | ✅ |
| /api/applications/{id}/photo | PATCH | 타인 403 / 비반려상태 400 (200성공 미검증) | ⚠️ |
| /api/applications/{id}/cards/download | GET | 타인 403 / 미완료 400 (200성공 미검증) | ⚠️ |
| /api/admin/applications (+/{id}·/members) | GET | 200 / USER 403 / 미인증 401 | ✅ |
| .../confirm-payment · start-review · approve-naming | POST | 200 (SUBMITTED→…→NAME_EDITING) | ✅ |
| .../reject-photo · complete-naming · start-producing · card-ready · dispatch | POST | 200 (전 전이) | ✅ |
| .../members/{memberId}/name | POST | 200(선택이력 +1) / USER 403 | ✅ |
| /api/admin/name-selection-stats | GET | 200(assign 반영) | ✅ |
| /api/admin/applications/export | POST | 200 INDIVIDUAL(xlsx PK매직바이트 `50 4b 03 04`) / GROUP 원본없음 400 | ✅ |
| .../naming-result | POST | 400(비엑셀) (200성공 미검증) | ⚠️ |

**상태 전이 시퀀스**(app3, SUBMITTED→): 결제확인→검토시작→작명승인→작명완료→제작시작→카드발급 **6단계 전부 200**, DB `COMPLETED/CONFIRMED/card_ready_at` 반영. 규칙위반 재호출은 전부 400 `INVALID_STATUS_TRANSITION`. 실물동반(app6) card-ready→PRODUCING 유지→dispatch→COMPLETED 설계대로.

⚠️5는 전부 "유효 얼굴사진/ZIP/정상 엑셀 등 테스트 자산 부재로 2xx 성공경로만 미검증"(실패 아님).

### 2.3 후기(Review) — ✅4 / ⚠️1 / ❌1

| 엔드포인트 | 메서드 | 라이브(HTTP) | 판정 |
|---|---|---|---|
| /api/reviews | GET | 공개 200(page/size/cardTypeId/hasPhoto/keyword) / cardTypeId=999 404 | ✅ |
| /api/reviews/{id} | GET | 비로그인 200(canEdit=false) / ADMIN 200(canEdit=true) / 없는id 404 | ✅ |
| /api/reviews | POST | 미인증 401 / demo 403 `REVIEW_NOT_ELIGIBLE`(자격정책) | ⚠️ |
| **/api/reviews/{id}** | **PATCH** | **removeImage 생략 시 400 (프론트 실제 payload) — 수정 실질 불능** | ❌ |
| /api/reviews/{id} | DELETE | 미인증 401 / 타인 403 / 본인 200 | ✅ |
| /api/my/reviews | GET | 미인증 401 / 200 | ✅ |

### 2.4 게시판(Board) + 행사(Event) — ✅17 / ⚠️2 / ❌0

| 엔드포인트 | 메서드 | 라이브(HTTP) | 판정 |
|---|---|---|---|
| /api/boards?type=NOTICE·FAQ | GET | 200(4·8건) | ✅ |
| /api/boards (type 생략) | GET | 400 `INVALID_INPUT` | ⚠️ |
| /api/boards/{id} | GET | 200 / 없는id 404 | ✅ |
| /api/admin/boards (POST·PATCH·DELETE) | POST/PATCH/DELETE | 201·200·200(삭제후 404) / 미인증 401 / USER 403 / FAQ+첨부 400 | ✅ |
| /api/events?type=BOOTH·COLLABORATION | GET | 200 | ✅ |
| /api/events (type 생략) | GET | 400 `INVALID_INPUT` | ⚠️ |
| /api/events/{id} | GET | 200 / 없는id 404 | ✅ |
| /api/admin/events (GET·POST·PATCH·DELETE +/{id}) | 전체 | 200·201·200·200(삭제후 404) / 미인증401 / USER403 / BOOTH+company·필드누락 400 | ✅ |

⚠️2 = `GET /api/boards`·`/api/events`의 `type`이 `@RequestParam(required=false)`인데 서비스에선 필수(null→400) — **계약 표기 불일치**(프론트는 항상 type 전달, 실사용 무영향).

### 2.5 문의(Inquiry) — ✅7 / ⚠️0 / ❌0

| 엔드포인트 | 메서드 | 라이브(HTTP) | 판정 |
|---|---|---|---|
| /api/inquiries | POST | 201 / 미인증 401 / privacyConsent 누락·false 400 | ✅ |
| /api/my/inquiries (+/{id}) | GET | 200 / 타인 403 / 없는id 404 / 미인증 401 | ✅ |
| /api/admin/inquiries (+/{id}) | GET | 200 / USER 403 | ✅ |
| /api/admin/inquiries/{id}/answer | PATCH | 200(답변저장) / USER 403 | ✅ |
| /api/admin/inquiries/{id}/status | PATCH | 200(COMPLETED) / 잘못된 enum 400 | ✅ |

E2E: USER 문의생성→내문의 노출→ADMIN 답변·상태변경→USER 재조회 반영 확인. category는 DB enum(`PRODUCTION`)↔한글(`제작 신청`) `@JsonValue` 매핑 정상.

---

## 3. 발견 문제

### 🔴 P1 — `PATCH /api/reviews/{id}` 후기 수정이 실사용에서 항상 400 (CONFIRMED)
- **증상**: 후기 수정 화면 "수정 완료" 저장 시 400 `INVALID_INPUT`. 백엔드 로그: `HttpMessageNotReadableException: Cannot map 'null' into type 'boolean'`(FAIL_ON_NULL_FOR_PRIMITIVES).
- **근본원인**: `domain/review/dto/ReviewUpdateRequest.java`의 `private boolean removeImage;`(원시타입) + `@AllArgsConstructor`. 프론트 `ReviewUpdateBody`(api.ts)는 title/applicationType/cardTypeId/authorName/content/**keepImageIds**만 보내고 **`removeImage`를 생략**한다. Jackson이 all-args 생성자를 creator로 써서, 생략된 원시 boolean을 null로 바인딩하다 실패.
- **확정 근거(에이전트 A/B)**: PATCH에 `"removeImage":false`를 넣으면 파싱 통과(403 소유권검사 도달), 생략하면 400. 순수 ASCII payload에서도 재현(셸 인코딩 아티팩트 아님).
- **왜 기존 테스트가 못 잡았나**: `ReviewControllerTest`가 `removeImage`를 **항상 명시**해 프론트의 생략 케이스를 재현 못 함.
- **권장 수정(택1)**: ① `removeImage`를 `Boolean`(래퍼)로 변경 ② `@AllArgsConstructor` 제거(no-args + 필드/세터 바인딩) ③ 프론트 `updateReview`가 `removeImage:false`를 항상 전송. + 프론트 생략 경로 테스트 추가.
- **영향**: 후기 **작성**은 별개(자격정책 403)지만, 자격 있는 사용자의 후기 **수정** 저장은 현재 전부 실패.

### 🟡 경미 (결함 아님 / 표기·환경·테스트자산)
1. **이메일 인증코드 발송 503** (Auth): 로컬 SMTP 미설정 환경 제약. 코드 정상, SMTP 구성 환경에서만 발송 E2E 가능.
2. **`GET /api/boards`·`/api/events`의 `type` 표기 불일치**: 어노테이션 optional인데 서비스 필수(null→400). 프론트 항상 전달로 무영향 — 어노테이션을 `required=true`로 맞추면 표기 정합.
3. **신청 2xx 성공경로 5건 미검증**: 유효 얼굴사진/ZIP+직인/정상 saju 엑셀, PHOTO_REJECTED·COMPLETED 상태의 본인 신청 등 테스트 자산 부재. 인가·상태가드·검증 파이프라인은 전부 정상 확인.
4. **후기 작성 403 `REVIEW_NOT_ELIGIBLE`**: demo 계정에 (COMPLETED 신청 + 카드타입 일치) 이력이 없어 자격정책상 차단 — 설계대로.
5. **`api.refresh` 데드코드**: 미사용 헬퍼(엔드포인트는 인터셉터로 동작). 정리 대상.

---

## 4. 환경·재현·주의

- **재현**: 스택 기동 후 §2 도메인별 curl. 관리자 전이 시퀀스는 SUBMITTED/WAITING 신청에 confirm-payment→start-review→approve-naming→complete-naming→start-producing→card-ready 순.
- **⚠️ 테스트 하네스 주의(제품 무관)**: Git Bash에서 `curl -d`/`-F 'request={...}'`에 **한글을 인라인**으로 넣으면 UTF-8이 깨져 백엔드가 `요청 본문이 올바르지 않습니다`(400)로 오판정된다. **UTF-8 JSON 파일**(`--data-binary @file` / `-F request=@file.json`)로 보내야 정상. 프론트는 `JSON.stringify`/`Blob`으로 전송하므로 실제 영향 없음. (단, §3의 후기 PATCH 400은 이 아티팩트가 아니라 ASCII에서도 재현되는 **실제 서버 결함**임.)
- **데이터**: 각 에이전트는 자기 도메인 테이블에만 테스트 데이터 생성·정리. demo/admin 계정 무손상. 신청 상태 일부 전이됨(app3·6 COMPLETED, app5 PHOTO_REJECTED, app7 CANCELLED — 데모 데이터).

---

## 5. 커버리지 집계

| 도메인 | 케이스 | ✅ | ⚠️ | ❌ |
|---|---|---|---|---|
| 인증 + 회원 | 16 | 15 | 1 | 0 |
| 신청(공개/USER/ADMIN) | 23 | 18 | 5 | 0 |
| 후기 | 6 | 4 | 1 | 1 |
| 게시판 + 행사 | 19 | 17 | 2 | 0 |
| 문의 | 7 | 7 | 0 | 0 |
| **합계** | **71** | **61** | **9** | **1** |

**결론**: 백엔드 엔드포인트 64개 전부 프론트에 연결되어 있고, 라이브 동작·인가·검증·상태전이가 대부분 정상. **수정이 필요한 실제 결함은 후기 수정(PATCH /api/reviews/{id}) 1건**뿐이며 원인·수정안이 확정돼 있다.
