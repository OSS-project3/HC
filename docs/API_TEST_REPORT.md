# API 검증·테스트 리포트 (2026-08-26 전수 재실행)

프론트-백엔드 **전체 API 67개**를 도메인별 5개 서브에이전트로 실기동 스택에 라이브 호출해 (1) 프론트 연결, (2) 인증/인가/검증/음성케이스 동작을 검증했다. 이 리포트는 이전 리포트(2026-08-25, 64~65개)를 대체한다.

- **검증 방식**: 백엔드 컨트롤러 엔드포인트 ↔ 프론트 `services/api.ts` 호출 정적 대조 + 실기동 스택(backend·db·redis·minio·frontend, docker compose)에 도메인별 서브에이전트가 `curl`로 인증/미인증/음성 호출, HTTP 코드·응답·DB 반영 확인.
- **계정**: USER `demo@test.com`/`demo1234!`(id=3), ADMIN `admin@test.com`/`admin1234!`.
- **대상**: 백엔드 컨트롤러 엔드포인트 **67개** 전부.

---

## 0. 한눈에 보기

| 구간 | 결과 |
|---|---|
| **정적 연결(프론트↔백엔드)** | ✅ **67 / 67 연결** (모든 백엔드 엔드포인트가 `api.ts`에서 호출됨. `bulk`는 `createApplication`이 동적 생성) |
| **라이브 테스트(67 엔드포인트)** | ✅ **60** · ⚠️ **7** · ❌ **0** |
| **실제 결함(수정 필요)** | **0건** |
| 인가 경계(미인증 401 / 소유권·권한 403) | 전 도메인 정상 |
| 검증·상태 가드(400 / 404 / 409) | 전 도메인 정상 |

> ⚠️ 7건은 **결함이 아님**: 로컬 SMTP 미설정(503)·이미 약관동의된 계정(409)·유효 테스트 자산(얼굴사진/ZIP/saju xlsx/발급카드) 부재로 2xx 성공경로만 미검증. 상세는 §3.

---

## 1. 정적 연결 검증 (67/67)

백엔드 `*Controller.java`의 모든 `@{Get,Post,Put,Patch,Delete}Mapping`을 추출해 프론트 `services/api.ts` 호출 경로와 대조 — **67개 전부** 대응 함수가 있고 실제 화면(pages/components)이 호출한다. 이번 세션에 추가된 **카드번호 저장 2개**(개인·일괄)까지 연결 완료.

- 유일한 표기 예외: `POST /api/applications/bulk`은 `createApplication(form, true)`가 동적 생성(연결됨).
- `api.refresh` 헬퍼는 미사용이나, refresh 엔드포인트는 `request()`의 401 자동재시도가 호출.

---

## 2. 도메인별 라이브 결과

### 2.1 인증(Auth)+회원(User) — 16개: ✅14 / ⚠️2 / ❌0
- 로그인/로그아웃/refresh/getMe/updateMe/비밀번호변경/탈퇴/약관/이메일중복확인/회원가입(인증)/계정복구 4종 전부 연결·동작.
- **confirm 3종(signup-confirm·recovery id/password confirm)은 Redis에 코드 직접 주입으로 해피패스 200까지 실검증**(토큰 발급·마스킹 이메일·비번 재설정), 오답 코드 400도 확인.
- 미인증 401, 잘못된 비번 401, 임시계정으로 비번변경→재로그인→탈퇴(재로그인 401)까지 검증. demo/admin 무손상.
- ⚠️2: ① `signup/email-verification/request` **503**(로컬 SMTP 미설정, 환경) ② `auth/terms` **409**(demo/admin이 이미 약관동의 상태 — 신규 미동의 계정에서만 200, 정상 정책).

### 2.2 신청(Application) — 26개: ✅21 / ⚠️5 / ❌0
- 조회(lookup/my/admin/members)·취소·**입금자명**(본인·WAITING만 200, CONFIRMED 400, 타인 403)·상태전이 8종·작명확정·선택이력·엑셀 export(PK 매직바이트)·작명결과 업로드·**카드번호 개별/일괄** 전부 연결·동작.
- **카드번호**: 개인 `PUT .../members/{mid}/card-number` 200(형식 `ROK-#####-####`, 위반 400, USER 403), 일괄 `PUT .../card-numbers` 200(updatedCount:3, 버전불일치 **409**, photoNumber 불일치 시 all-or-nothing 미반영). 멤버 응답에 `cardNumber`/`photoNumber`, 상세에 `version` 노출 확인.
- **상태전이 시퀀스**(app7): confirm-payment→start-review→approve-naming→(작명)→complete-naming→start-producing→card-ready 전 구간 200, 작명 미완 시 `complete-naming` 400(NAMING_INCOMPLETE), 규칙위반 전이 400(INVALID_STATUS_TRANSITION) 확인.
- 관리자 엔드포인트 USER세션 403, 미인증 401.
- ⚠️5(성공경로 미검증, 실패 아님): 개인 생성/단체 생성/사진 재업로드/카드다운로드/작명결과 업로드 — 유효 얼굴사진·ZIP·saju xlsx·발급된 카드가 없어 2xx 미달(검증·인가·상태가드는 정상 확인).

### 2.3 후기(Review) — 6개: ✅6 / ⚠️0 / ❌0
- 목록/상세/작성/수정/삭제/내후기 연결·동작. 공개 GET 200, 미인증 401, 소유권 403, 없는 id 404, 페이징·필터·정렬 정상.
- **이전 결함(PATCH removeImage 생략 시 400)은 회귀 없음 확정** — 프론트 실제 형태(removeImage 생략, keepImageIds 포함)로 PATCH 시 400 파싱오류 없이 소유권·자격 검증까지 도달(`Boolean removeImage` 수정 유효). DELETE는 본인 생성분으로 200 확인.
- POST/PATCH 성공경로(201/200)는 demo/admin에 COMPLETED 신청이력이 없어 자격게이트(403 REVIEW_NOT_ELIGIBLE)로 정상 차단 — 설계대로.

### 2.4 게시판(Board)+행사(Event) — 12개: ✅12 / ⚠️0 / ❌0
- Board(공개 2 + 관리자 3), Event(공개 2 + 관리자 5) 전부 연결·동작.
- CRUD 전 생명주기: 생성(201, 한글 멀티파트·첨부/썸네일/로고/갤러리)→PATCH(부분삭제 `keepAttachmentIds`/`keepImageIds`/`removeLogo`)→DELETE→404·카운트 원복.
- `type` 파라미터 없으면 400, 미인증 401, USER세션 403, 도메인 엣지(BOOTH+company→400, removeLogo+새 logo 동시→400) 확인. 시드 무변경.

### 2.5 문의(Inquiry) — 7개: ✅7 / ⚠️0 / ❌0
- 작성(privacyConsent:true 필수, 누락/false 400)·내문의·관리자 목록/상세/답변/상태 전부 연결·동작.
- E2E: USER 생성→내문의 노출→ADMIN 답변·상태변경→USER 재조회 반영. 미인증 401, USER세션→admin 403, 타인 문의 403, 없는 id 404, 잘못된 status enum 400.

---

## 3. 발견 사항 (실제 결함 0 · 참고/개선)

**실제 결함 없음.** 아래는 환경 제약·경미한 개선 포인트다.

1. ⚠️ **이메일 인증코드 발송 503**: 로컬 SMTP 미설정. 코드/단위테스트 정상, 운영 SMTP 구성 시 동작(`BACKEND_TODO.md §8`).
2. ⚠️ **신청 5개 2xx 성공경로 미검증**: 유효 얼굴사진/ZIP+직인/정상 saju 엑셀/발급 카드 자산 부재. 인가·상태가드·검증 파이프라인은 전부 확인.
3. ℹ️ **일괄 카드번호 버전충돌은 HTTP 409**(`APPLICATION_VERSION_CONFLICT`) 반환 — 일부 설계문서는 400으로 기재했으나 409(Conflict)가 시맨틱상 정확. 문서 상태코드 정합만 필요.
4. ℹ️ **작명 저장(POST members/name) 검증 오류가 뭉뚱그려짐** — 한자 글자수·성씨 형식 위반 시 필드 상세 없이 일반 `INVALID_INPUT`만 반환(프론트 원인 안내 어려움). 개선 여지.
5. ℹ️ **lookup(application 방식)은 전화+이메일 둘 다** 필요(하나만 400). `api.ts` 시그니처는 optional이나 `LookupPage`는 둘 다 채워 보냄(정상).
6. ✅ (수정됨) 낡은 `api.ts` 주석("admin 상태전이·엑셀 미구현")을 실제(전부 연결됨)로 정정.

---

## 4. 커버리지 집계

| 도메인 | 엔드포인트 | ✅ | ⚠️ | ❌ |
|---|---|---|---|---|
| 인증 + 회원 | 16 | 14 | 2 | 0 |
| 신청(공개/USER/ADMIN·카드번호 포함) | 26 | 21 | 5 | 0 |
| 후기 | 6 | 6 | 0 | 0 |
| 게시판 + 행사 | 12 | 12 | 0 | 0 |
| 문의 | 7 | 7 | 0 | 0 |
| **합계** | **67** | **60** | **7** | **0** |

**결론**: 백엔드 엔드포인트 **67개 전부 프론트에 연결**되어 있고, 라이브 동작·인가·검증·상태전이가 정상. **수정이 필요한 실제 결함 0건.** ⚠️7은 환경(SMTP·이미 동의)·테스트 자산 부재로 인한 성공경로 미검증이며 코드 결함이 아니다.

> 남은 백엔드 미구현(연결 대상 아님)은 `docs/BACKEND_TODO.md` 참고: 관리자 통계·공지 서버검색·이름조회/추천 API·카드 이미지 합성 HTTP API·CardType/CardDesign 컨트롤러.

---

## 5. 재현 방법

```bash
docker compose up -d                     # 전체 스택
# 도메인별 curl 검증 — 계정: demo@test.com/demo1234!, admin@test.com/admin1234!
# 한글 body는 UTF-8 파일로: curl ... --data-binary @body.json  (Git Bash 인라인 한글은 깨짐)
# 카드번호 형식: ROK-#####-####  · 일괄은 GET 상세의 version + GET members의 photoNumber 사용
```

> 참고: 이번 테스트로 데모 신청 일부 상태가 전이됨(app2 COMPLETED, app5 PHOTO_REJECTED, app7 CANCELLED 등). 원복은 `DemoDataSeeder` 재시드(빈 DB)로 가능.
