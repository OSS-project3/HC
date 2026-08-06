# 프로젝트 현재 상태 (2026-07-31 기준)

> 이 문서 하나만 읽으면 프로젝트를 한동안 안 봤어도 "지금 뭐가 되어있고, 뭐가 안 되어있고, 다음에 뭘 해야 하는지" 파악할 수 있도록 작성했습니다.
> 상세 설계는 `DB.md`(엔티티), `docs/api/README.md`(API)를 참고하세요. 이 문서는 그 둘의 요약 + 백엔드 실제 코드 상태 + 프론트 상태를 종합한 것입니다.

---

# 1. 프로젝트 개요

## 목적
한국을 방문/체류하는 외국인에게 **한글 오행(사주) 기반 한국 이름**을 지어주고, **명예한국인증·명예시민증·학생증·방문증** 4종 카드를 제작·발급하는 서비스형 홈페이지. 개인 신청과 법인/단체(엑셀+ZIP 일괄) 신청을 모두 지원하고, 무통장입금 결제와 관리자 검토·작명·카드 발급 워크플로를 거친다.

## 현재 구현 범위 (한 줄 요약)
- **프론트**: 화면/디자인은 상당히 완성도 높게 만들어져 있으나, **서버 연동이 0%** (fetch/axios 호출이 코드 전체에 단 하나도 없음). 전부 정적 mock 데이터.
- **백엔드**: Spring Boot 프로젝트가 이미 상당 부분 구현되어 있으나, **이것과 완전히 다른 옛 도메인("사주 기반 외국인 등록증") 기준**으로 짜여 있고, **현재 컴파일조차 안 되는 상태**(4곳 미완성 리팩터링). 새로 확정한 설계(`DB.md`/`docs/api/README.md`)와 재사용 가능한 건 인증(OAuth/JWT) 인프라 정도뿐.
- **DB/API 설계 문서**: `DB.md`, `docs/api/README.md`에 새 도메인 기준 엔티티·API 21개가 상세 설계되어 있음(코드는 아직 없음).

## 주요 기술 스택
| 영역 | 스택 |
|---|---|
| 프론트 | React 18 + TypeScript, Vite, React Router v6, 폰트 CDN(Pretendard 등) |
| 백엔드 | Spring Boot 4.x (Java 21), H2(로컬)/PostgreSQL(운영), Spring Security OAuth2, JWT(jjwt), Redis(Lettuce), AWS S3, TossPayments(옛 도메인 전용, 새 설계에선 미사용) |
| 저장소 구조 | 모노레포 (`/` = 프론트, `/backend/honor-citizen` = 백엔드) — 2026-07-31 편입, `main`엔 아직 반영 안 됨(`feature/db-api-docs` 브랜치에만 있음) |

## 전체 아키텍처
```
프론트(React SPA, 정적 mock)  ⟷ [연동 코드 없음] ⟷  백엔드(Spring Boot, 옛 도메인, 컴파일 깨짐)
        │                                                    │
   src/data/*.ts 정적 데이터                          domain/{user,application,payment,...}
   sessionStorage에 신청 draft 보관                    (사주 기반 — 새 설계와 불일치)
        │
   DB.md / docs/api/README.md ── 새 도메인 설계 (User/Application/Payment/카드/Admin, 21개 API) — 구현 전
```
프론트와 백엔드가 **지금은 완전히 분리되어 있고 연결된 적이 없습니다.** 새 설계 문서 기준으로 백엔드를 다시 짜고, 프론트에 API 연동 코드를 새로 붙이는 작업이 남아있습니다.

---

# 2. 현재 구현 상태 (기능별)

## 인증/인가
- **OAuth(Google/Naver) + JWT**: 백엔드에 이미 잘 구현되어 있음(`SecurityConfig`, `OAuth2SuccessHandler`, `JwtTokenProvider`, `JwtAuthFilter`, `AuthCookieManager`, `TokenSessionStore`). HttpOnly 쿠키, accessToken 15분/refreshToken 14일, rotation, 재사용 감지, 블랙리스트까지 구현됨. **User 도메인은 새 설계와 거의 그대로 재사용 가능**(단, `User.phone`을 NOT NULL→NULL 허용으로 정정 필요).
- **프론트**: `LoginPage.tsx`/`SignupPage.tsx`가 이메일+비밀번호 mock 폼. 실제 정책(OAuth)과 다름 — 교체 필요.
- **실행 가능 여부**: 미확인. `.env`에 Google/Naver 키는 있으나 `JWT_SECRET`/`AWS_*` 등 다른 필수 값이 없고, `spring-dotenv`가 Spring Boot 4.x에서 정상 동작하는지도 불확실(자체 문서에 "안 된다"고 적혀 있었음). Redis도 로컬에 미기동.

## 신청(Application)
- **백엔드**: `ApplicationController`/`ApplicationService`/`BulkApplicationService`가 있으나 **옛 "사주" 도메인**(nameEn/nationality/birthDate/birthTime/birthRegion/gender/photoId) 기준. **컴파일 에러로 깨진 상태**(4절 참고).
- **새 설계**: `card_design_id`, `ApplicationMember`(카드 1장=1명), `Applicant`/`Receiver` 분리, `issue_type`(MOBILE/MOBILE_AND_PHYSICAL) 등 완전히 다른 구조. **사실상 전면 재작성 대상.**
- **프론트**: `ApplyPage.tsx` 5단계 UI는 있으나 서버 제출 없음(sessionStorage만). 개인 신청 폼에 생년월일·국적·출생시각·출생지역·성별·사진 입력란이 아예 없음(새로 확정된 필수 항목인데 미구현).

## 결제(Payment)
- **백엔드**: TossPayments 가상계좌 발급/웹훅 방식으로 이미 구현됨(옛 설계).
- **새 설계**: **고정 회사 계좌 무통장입금 + 관리자 수동 확인** 방식으로 완전히 다름. TossPayments 인프라는 새 설계에서 불필요.
- **프론트**: `StepComplete.tsx`에 계좌 정보 표시는 있으나, 입금자명 입력란이 `value`/`onChange` 없는 비활성 상태.

## 카드(Card)
- **백엔드**: `CitizenCard` 엔티티로 카드번호(`HN-KR-YYMM-NNNN`, 새 설계와 형식 다름) 및 이미지 생성 구현됨. 카드 레이아웃은 `DefaultCardImageGenerator.java`에 좌표 하드코딩(파라미터 값 미결정 — 백엔드 자체 문서에 "높음 우선순위 미결정"으로 기록됨).
- **새 설계**: `CardType`/`CardDesign`(관리자 CRUD)+`CardFieldDefinition`(DB 아님, config/코드 상수). 카드번호 형식도 `ROK-XXXXX-XXXX`로 확정(시안 실물 확인).
- **디자인 자산**: `시안.zip`에 명예한국인증/명예시민증/방문증 3종 실물 시안(카드당 6개 디자인, 좌표값 포함) 확보됨. **학생증 시안은 아직 없음.**

## 관리자(Admin)
- **백엔드**: `AdminApplicationController`에 한국이름 등록/수정, 시민증 발급 API 일부 구현(옛 도메인 기준).
- **새 설계**: `PAYMENT_PENDING→RECEIVED→REVIEWING→(PHOTO_REJECTED↔REVIEWING)→NAME_EDITING→PRODUCING→COMPLETED` 상태 흐름 + 6개 API(목록/상세/입금확인/사진검토/작명/카드발급) 설계 완료, 코드는 없음.
- **프론트**: `AdminPage.tsx`는 **읽기 전용 정적 테이블 하나뿐**. 상세/처리 화면, 액션 버튼 전부 없음.

## 프론트 UI/디자인 (공통)
홈/디자인/제작신청/조회/로그인/회원가입/고객지원 페이지가 시각적으로는 상당히 완성되어 있음(반응형, 폰트, 색상 토큰 등). 다만 **회사소개/후기/행사사업은 `StubPage`(준비중) 상태**이고, 전체적으로 **모든 데이터가 정적**이라 실제 서비스로 쓰려면 API 연동이 전면적으로 필요.

---

# 4. 현재 발견된 문제점

### 컴파일 오류 (즉시 조치 필요)
- `ApplicationService.java`, `BulkApplicationService.java`가 `Application.createSingle()`/`createBulk()`, `BulkOrder.create()`를 호출하는데, **엔티티 쪽 메서드 시그니처엔 `entryDate`/`address`/`cardType` 파라미터가 추가되어 있고 호출부는 안 고쳐진 상태.** 프로젝트 전체가 빌드 안 됨(`./gradlew bootRun` 실행 시 `compileJava` 단계에서 실패 확인함). 누군가 엔티티만 고치고 호출부를 못 고친 미완성 리팩터링으로 보임.
- **왜 문제인가**: 백엔드를 전혀 실행할 수 없어서, OAuth 테스트든 뭐든 아무것도 검증할 수 없음. 가장 먼저 처리해야 함.

### 설계상 문제 — 도메인 자체가 다름
- 백엔드는 "외국인 방문자 개인 등록증(사주 작명)" 도메인으로 짜여 있고, 실제 요구사항은 "카드 디자인 선택 + 개인/법인 신청 + 무통장입금 + 관리자 검토" 도메인. **필드 구조가 근본적으로 다름**(예: `nameEn`/`nationality` 단일 필드 vs `ApplicationMember` N명 구조).
- **왜 문제인가**: 기존 백엔드 코드를 "고쳐 쓰는" 접근이 거의 불가능하고, User/JWT 인프라 정도만 재사용하고 나머지(Application/Payment/Card/Admin)는 **사실상 새로 작성**해야 함. 이걸 모르고 기존 코드 위에 패치를 얹으려 하면 계속 충돌이 남.

### 로컬 실행 환경 문제
- `.env`에 Google/Naver OAuth 키만 있고, `JWT_SECRET`/`AWS_ACCESS_KEY`/`AWS_SECRET_KEY`/`AWS_S3_BUCKET` 등 다른 필수 환경변수는 **`.env`에도 없음**(기본값 없는 필수 프로퍼티라 없으면 부팅 자체가 실패할 가능성).
- `spring-dotenv` 라이브러리는 의존성에 있지만, 자체 문서(`CLAUDE.md`)에 "Spring Boot 4.x에서 동작 안 함"이라고 적혀 있어 `.env`가 실제로 로딩되는지 불확실.
- Redis가 로컬에 안 떠있음(`redis-cli` 없음, Docker Desktop도 꺼져있음). `TokenSessionStore`가 Redis 없으면 로그인 세션 관리가 안 됨.
- **왜 문제인가**: 컴파일 문제를 고쳐도, 이 3가지가 안 풀리면 서버 자체를 못 띄움.

### 프론트-백엔드 연동 0%
- 프론트 코드 전체에 `fetch`/`axios` 호출이 **단 한 줄도 없음.** API 클라이언트 레이어 자체가 없음.
- **왜 문제인가**: 화면은 다 만들어져 있어 보이지만, 실제로는 진짜 아무 기능도 동작하지 않는 정적 프로토타입 상태. API가 완성돼도 프론트에 붙이는 작업이 별도로 필요.

### 파일 업로드 아키텍처 결함
- `FileUploadBox.tsx`가 파일 선택 시 `URL.createObjectURL()`로 미리보기만 만들고 **실제 `File` 객체를 어디에도 안 들고 있음**(`UploadFileInfo` 타입에 `File` 필드 자체가 없음).
- **왜 문제인가**: 이 상태로는 신청 제출 시 로고/직인/제출ZIP/사진을 실제로 서버에 전송할 방법이 없음. API 연동보다 먼저 고쳐야 하는 구조적 문제.

### Mock 코드 / 실제 미구현 화면
- 로그인/회원가입: 이메일+비밀번호 mock (실제 정책은 OAuth)
- 신청 제출: `sessionStorage`에만 저장, 제출 시 프론트가 임의로 신청번호(`APP-2026-XXXXXX`) 생성
- `AdminPage.tsx`: 정적 배열(`adminMock.ts`) 표시만, 상세/처리 화면 없음
- `LookupPage.tsx`: 정적 mock 결과 표시, 카드번호 placeholder도 틀린 형식(`HN-KR-...`, 실제는 `ROK-XXXXX-XXXX`)
- `/company`, `/reviews`, `/events`: `StubPage`(준비중)
- **왜 문제인가**: 겉보기엔 완성된 것처럼 보여서, 실제 구현 범위를 착각하기 쉬움.

### 미완성/누락된 프론트 필드
- 개인 신청 폼에 생년월일·국적·출생시각·출생지역·성별·사진 입력란 없음(사주 작명 도구 연동에 필수인데 신규 확정된 항목)
- `StepFiles.tsx`/`StepInfo.tsx`가 `applicantType`(개인/법인)에 따른 분기가 없음 — 개인 신청에도 로고/직인/제출ZIP이 다 보임
- `/lookup`에 반려 상태·재업로드 기능 없음
- 카드 다운로드 화면 자체가 없음

### 구버전 값 잔존
- `adminMock.ts`/`LookupPage.tsx`의 status enum이 옛날 값(`SUBMITTED`/`CONSULTING`/`PAYMENT_PENDING`/`IN_PRODUCTION` 등) — 새로 확정된 enum(`PAYMENT_PENDING/RECEIVED/REVIEWING/PHOTO_REJECTED/NAME_EDITING/PRODUCING/COMPLETED/CANCELLED`)과 다름

### 백엔드 내부 문서 불일치 (발견 당시 이력)
- `backend/.../domain/shipping/docs/db.md`(로컬)와 `docs/db/DB_RULES.md`(중앙) 간 `shipping_addresses.country`/`user_id` 필드 존재 여부가 서로 달랐음(어느 쪽이 최신인지 미확인 상태로 남아있음)

### 테스트 커버리지 사실상 0
- 백엔드 테스트 파일 **2개뿐**(`HonorCitizenApplicationTests`(스모크), `ApplicationControllerTest`) — 그마저도 컴파일이 안 돼서 지금은 실행 자체가 안 됨. OAuth/JWT/User 관련 테스트는 **0개**.
- **왜 문제인가**: 새로 구현할 때 회귀를 잡아줄 안전망이 없음.

### 기술 부채 / 미결정 사항 (`docs/api/README.md` 말미에 종합됨)
- 카드번호 채번 로직(순차 발급 vs 무작위) 미정
- 게시판(Review/Post) 필드 미정 — 프론트 요구사항 대기
- `Receiver.country`(해외배송 지원 여부) 미정
- `CardFieldDefinition.font_color` 이후 필드 — 원본 자료 자체가 없어 확인 불가
- refresh 토큰 rotation용 세션 저장소를 DB로 할지 Redis만 쓸지 미정(구현 단계 결정 가능)
- `MOBILE_AND_PHYSICAL`(실물배송) SHIPPING/DELIVERED 흐름 — 이번 설계 범위 밖, 추후 별도 설계
- 사주 작명 프로그램(TypeScript로 존재)은 URL 링크아웃만 하기로 확정, 별도 통합 없음
- 학생증 카드 시안 미도착

---

# 5. 앞으로 구현해야 할 기능

## 반드시 먼저 해야 하는 것
1. **백엔드 컴파일 에러 수정** — 옛 `Application`/`BulkOrder` 서비스 호출부를 고치거나, 새 설계로 갈아엎을 거면 아예 폐기 여부 결정
2. **로컬 실행 환경 정비** — `JWT_SECRET`/`AWS_*` 등 필수 env 채우기, `.env` 로딩 방식 재검증(안 되면 `application-local.properties`로 이전), Redis 로컬 기동
3. **OAuth 로그인 E2E 검증** — Google/Naver 실제 로그인 → JWT 쿠키 발급까지 브라우저로 한 번 확인 (User 도메인 재사용 전제가 맞는지 확인하는 리스크 제거 단계)
4. **`FileUploadBox`가 실제 `File` 객체를 보관하도록 구조 변경** — 이게 안 되면 이후 어떤 업로드 API도 프론트에서 못 씀
5. **프론트-백엔드 연동 골격 구축** — API 클라이언트, 공통 응답(`{success,data,errorCode,errorMessage}`) 파싱 유틸

## 이후 구현
6. `DB.md`/`docs/api/README.md` 기준 **Application/Payment/카드/Admin 도메인 백엔드 신규 구현** (엔티티 + API 21개)
7. 프론트: 개인 신청 폼에 생년월일·국적·출생시각·출생지역·성별·사진 필드 추가, `StepFiles`/`StepInfo` 신청유형별 분기
8. 로그인/회원가입 화면을 OAuth 버튼 방식으로 교체
9. 입금자명 입력 활성화(`value`/`onChange` 연결), `/lookup`에 반려사유+재업로드 버튼, 카드 다운로드 화면 신규
10. 관리자 화면 전체 신규 구축(목록/상세/입금확인/사진검토/작명/카드발급 6개 액션)
11. `CardType`/`CardDesign` 관리자 CRUD 화면 (또는 화면 없이 DB 직접 시드로 시작)

## 나중에 개선
12. 실물배송(`MOBILE_AND_PHYSICAL`) SHIPPING/DELIVERED 흐름 설계·구현
13. 게시판(후기/행사) 도메인 — 프론트 요구사항 확정 후
14. 카드번호 채번 로직 확정(순차/무작위)
15. refresh 토큰 세션 저장소 구조 확정
16. 학생증 시안 반영
17. 테스트 커버리지 확충 (특히 OAuth/JWT부터)
18. 이메일 알림, 통계 대시보드 등 부가 기능

---

# 6. API 구현 현황

## 현재 구현된 API (백엔드 코드에 실존, ⚠️ 전부 옛 "사주" 도메인 기준 — 새 설계와 필드가 다름)

| 엔드포인트 | 컨트롤러 | 비고 |
|---|---|---|
| `POST /api/auth/terms` | AuthController | 약관 동의 — 새 설계엔 없는 개념(User 정책이 달라짐) |
| `POST /api/auth/refresh` | AuthController | ✅ 새 설계에서도 그대로 재사용 가능 |
| `POST /api/auth/logout` | AuthController | ✅ 새 설계 User API 3과 거의 동일, 재사용 가능 |
| OAuth 로그인(Google/Naver) | SecurityConfig+OAuth2SuccessHandler | ✅ 새 설계 User API 1과 사실상 동일, 재사용 가능 |
| `POST /api/applications` (단건) | ApplicationController | ⚠️ 옛 필드(nameEn 등), 컴파일 안 됨 |
| `POST /api/applications/bulk` | BulkOrderController | ⚠️ 옛 필드, 컴파일 안 됨 |
| `POST /api/uploads/photo` | UploadController | 옛 PhotoUpload 방식(사전업로드+photoId) — 새 설계는 신청 생성 API에 파일 임베드 방식이라 구조가 다름 |
| `POST/PATCH /api/applications/{id}/shipping` | ShippingController | 새 설계엔 배송지가 `Receiver`로 흡수됨, 별도 API 불필요해짐 |
| `GET /api/applications/{id}/payment-info`, `POST /api/payments/virtual-account`, `POST /api/payments/webhook` | PaymentController | TossPayments 가상계좌 방식 — 새 설계(무통장입금 수동확인)와 완전히 다름, 재사용 불가 |
| `GET /api/my/applications`, `GET /api/my/applications/{id}`, `PATCH /api/my/applications/{id}/photo`, `GET /api/my/applications/{id}/card/download` | ApplicationController | 옛 도메인 기준, 개념은 유사(목록/상세/재업로드/다운로드)하나 필드 재작성 필요 |
| `POST/PATCH /admin/applications/{id}/korean-name`, `POST /admin/applications/{id}/issue-card` | AdminApplicationController | 개념(작명/발급)은 새 설계와 유사하나 상태 흐름·필드 다름, 재작성 필요 |

## 미구현 API (백엔드 자체 문서 기준, 옛 도메인의 나머지 14개 — 참고용, 새 설계에선 대부분 불필요해짐)
`POST /admin/auth/login`, `GET /admin/applications` 등 어드민 관리 API 일부, Bulk 검증/확인 분리 API(`bulk/validate`, `bulk/confirm`, `bulk/template`) — TossPayments 웹훅 서명검증 TODO 포함.

## 새로 추가해야 하는 API (`docs/api/README.md` 기준, 21개 — 전부 코드 없음, ⚠️ 표시는 프론트 미확인)

| 도메인 | API | 비고 |
|---|---|---|
| User | OAuth 로그인/콜백 | 기존 인프라 재사용 가능 |
| User | `GET /api/users/me` | 신규 |
| User | `POST /api/auth/logout` | 기존 재사용 가능 |
| Application | `POST /api/applications` (개인) | 신규 필드 다수(국적/출생시각 등) |
| Application | `POST /api/applications/bulk` (단체) | 엑셀 컬럼 재정의됨 |
| Application | `POST /api/applications/lookup` | 신규(반려사유 포함) |
| Application | `PATCH /api/applications/{id}/photo` (재업로드) | 신규, 로그인 필수 |
| Application | `GET /api/applications/{id}/cards/download` | 신규 |
| Payment | `PATCH /api/applications/{id}/payment` (입금자명) | 신규 |
| 카드 | `POST/GET/PATCH /api/admin/card-types` | 신규 |
| 카드 | `POST/GET/PATCH /api/admin/card-designs` | 신규 |
| Admin | `GET /api/admin/applications` (목록) | 신규 |
| Admin | `GET /api/admin/applications/{id}` (상세) | 신규 |
| Admin | `POST /api/admin/applications/{id}/confirm-payment` | 신규 |
| Admin | `POST .../approve-photo` \| `reject-photo` | 신규 |
| Admin | `PATCH /api/admin/application-members/{id}/name` + `.../complete-naming` | 신규 |
| Admin | `POST /api/admin/applications/{id}/issue-cards` | 신규 |

전부 `docs/api/README.md`에 Request/Response/Validation/DB매핑까지 상세 설계되어 있음.

---

# 7. DB 구현 현황

## 현재 엔티티 (백엔드 코드에 실존, 옛 도메인 기준)
`User`, `RefreshTokenSession`, `Application`(사주 필드: nameEn/nationality/birthDate/birthTime/birthRegion/gender/photoPath/photoId/entryDate/address/cardType), `PhotoUpload`, `ShippingAddress`, `KoreanName`, `CitizenCard`(카드번호 `HN-KR-YYMM-NNNN`), `BulkOrder`, `Payment`+`PaymentLog`(TossPayments), `ApplicationStatusLog`/`AdminActivityLog`/`EmailLog`

## 새로 설계된 엔티티 (`DB.md` 기준, 코드 없음)
`User`(일부 수정), `Application`(전면 재설계), `Applicant`, `Receiver`, `ApplicationMember`(신규 — 카드 1장=1명), `Payment`(경량 재설계), `UploadFile`, `CardType`, `CardDesign`, `CardFieldDefinition`(DB 아님, config/코드 상수로 전환), `Review`/`Post`(보류)

## 매핑 관계 — 재사용 가능한 것 vs 재작성 필요한 것

| 새 엔티티 | 기존 엔티티와의 관계 | 조치 |
|---|---|---|
| `User` | 기존 `User`와 거의 동일 구조 | `phone` NOT NULL→NULL 정정만 하면 재사용 가능 |
| `Application` | 기존 `Application`과 이름만 같고 필드 전혀 다름(사주 정보 vs 카드종류/신청유형) | **전면 재작성** |
| `Applicant`/`Receiver` | 기존엔 없음(주소가 `ShippingAddress`에만 있었음) | **신규 생성** |
| `ApplicationMember` | 기존 `KoreanName`+`CitizenCard`가 하던 역할을 통합 | **신규 생성**, 개념 유사하지만 구조 다름 |
| `Payment` | 기존 `Payment`는 TossPayments 전용 필드(orderId/paymentKey 등) 다수 | **경량 재설계** (depositor_name 위주로 축소) |
| `CardType`/`CardDesign` | 기존엔 이런 마스터 테이블 없음(카드 종류가 Java enum `CardType`으로만 존재) | **신규 생성** |
| `UploadFile` | 기존 `PhotoUpload`(사진 전용)와 부분 유사 | 결론적으로 **독립 API 불필요**로 확정(각 도메인에 임베드) |

**결론**: 새 DB 설계 기준으로 볼 때 **`User` 정도만 기존 스키마를 거의 그대로 쓸 수 있고, 나머지는 전부 신규 마이그레이션이 필요합니다.**

---

# 8. 프론트 구현 현황

## 현재 구현된 화면

| 경로 | 화면 | 완성도 |
|---|---|---|
| `/` | 홈(히어로/주요디자인/서비스핵심/기념품/상담문의/협력기관) | 시각적으로 완성, 전부 정적 |
| `/design` | 카드 디자인 갤러리 | 시각적으로 완성, 정적 이미지(`cards.ts`) |
| `/apply` | 제작신청 5단계 | UI는 있으나 필드 일부 누락(생년월일 등), 서버 제출 없음 |
| `/lookup` | 신청 조회 | mock 결과만 표시, 반려/재업로드 기능 없음 |
| `/support` | 고객지원(공지/FAQ/제작이야기/상담문의) | 정적 콘텐츠 |
| `/login`, `/signup` | 로그인/회원가입 | 이메일+비밀번호 mock (정책은 OAuth라 교체 대상) |
| `/admin` | 관리자 | **읽기 전용 테이블뿐**, 상세/액션 없음 |

## Mock 상태인 부분
- `src/data/*.ts` 전체: `cards.ts`, `zodiac.ts`, `partners.ts`, `merchandise.ts`, `social.ts`, `policies.ts`, `adminMock.ts` — 전부 정적 배열
- `AuthContext.tsx`: localStorage 기반 mock 세션(`loginAsUser`/`loginAsAdmin` 데모 버튼 존재)
- `useApplicationDraft.ts`: `sessionStorage`에만 신청 draft 저장
- `ApplyPage.tsx`의 신청번호: 제출 시 프론트가 그 자리에서 `APP-2026-XXXXXX` 임의 생성

## 실제 API 연결 여부
**0%.** `fetch`/`axios` 호출이 프론트 코드 전체에 하나도 없음. `package.json`에도 HTTP 클라이언트 라이브러리 없음.

## 추가 구현해야 하는 화면/기능
- 개인 신청 폼: 생년월일·국적·출생시각·출생지역·성별·사진 입력란 (신규)
- `StepFiles`/`StepInfo`: `applicantType`(개인/법인)에 따른 필드 분기
- 로그인/회원가입: OAuth 버튼 방식으로 전면 교체
- `StepComplete`: 입금자명 입력 활성화(`value`/`onChange`)
- `/lookup`: `PHOTO_REJECTED` 상태 시 반려사유 표시 + "로그인 후 재업로드" 버튼 (신규)
- 카드 다운로드 화면 (신규, 지금 어디에도 없음)
- 관리자 상세/처리 화면 전체 (신규) — 입금확인/사진검토(승인·반려)/작명(구성원별)/카드발급 액션
- 관리자 카드종류·카드디자인 관리 화면 (신규, 없어도 DB 시드로 대체 가능)
- `FileUploadBox`: 실제 `File` 객체 보관하도록 구조 변경 (선행 조건)
- `statusLabels`류 전부 새 status enum으로 교체 (`adminMock.ts`, `LookupPage.tsx`)
