# Application 도메인 — 사용자 명세

> 작성일: 2026-07-31
> 목적: Application(신청) 도메인 백엔드 구현 전, 사용자 흐름 기준으로 개인/단체 처리 방침과 카드종류별 차이를 확정한다.
> 기준 문서: `DB.md`(엔티티), `docs/api/README.md`(API) — 이 문서에서 확정되는 내용은 두 문서에 이어서 반영한다.
> ⚠️ 아직 DB/API 상세 설계 단계가 아니라 **요구사항 정리 단계**. 임의로 세부 스펙(글자수 제한 등)을 만들지 않고, 확인 필요한 건 [TBD]로 남긴다.

---

## 1. 카드 종류 (4종)

| 카드종류 | 영문 코드(예시) | 방향 | 비고 |
|---|---|---|---|
| 명예한국인증 | `HONOR_KOREAN` | 가로 | |
| 명예시민증 | `HONOR_CITIZEN` | 가로 | |
| 방문증 | `VISITOR` | 세로 | |
| 학생증 | `STUDENT` | 세로 | **추가 항목 있음(3절)**, 시안 이미지 아직 미도착 |

`시안.zip` 재확인 결과: 명예한국인증/명예시민증/방문증은 카드종류당 디자인 6개(발행 지자체별: 삼척시/서울시/제천시/공주시/상주시/전주시 등)가 이미 존재. 학생증 폴더는 없음(추후 전달 예정, 기존에 파악한 내용과 동일).

---

## 2. 신청인 개인정보(사주 정보) 필드 — 2026-07-31 수정

### 2-1. 개인 신청

| 필드 | 소속 단위 | 필수 여부 | 비고 |
|---|---|---|---|
| 영문이름 | `ApplicationMember`(english_name) | 필수 | |
| 국적 | `ApplicationMember`(nationality) | 필수 | |
| 생년월일 | `ApplicationMember`(birth_date) | 필수 | |
| 출생시간 | `ApplicationMember`(birth_time) | **조건부 선택** | "모름" 체크 시 미입력 가능 → **Nullable로 변경 필요**(기존 NOT NULL이었음) |
| 출생지역 | `ApplicationMember`(birth_region) | **필수** | 태어난 도시/지역명 입력. 예: `Chicago`, `London`, `Tokyo`, `Beijing`, `Los Angeles`. 기존 데이터 호환을 위해 DB 컬럼은 Nullable 유지 |
| 성별 | `ApplicationMember`(gender) | 필수 | |
| **한국 입국날짜** | `ApplicationMember`(entry_date) | **선택(Nullable)** | ✅ 신규 추가 필드 |
| 이메일 | 신청 폼에서 입력 | 필수 | 소속 단위는 3-1절 참고 |
| 전화번호 | 신청 폼에서 입력 | 필수 | 소속 단위는 3-1절 참고 |

### 2-2. 단체 신청 (엑셀)

개인 신청과 동일한 항목을 **엑셀 파일**을 통해 인원별로 입력받는다.

**엑셀 컬럼(확정)**: 사진 번호, 영문이름, 국적, 생년월일, 출생시간, 출생지역, 성별, 개별입국날짜, 이메일, 전화번호, **주소** (공통 11개)

- 출생지역: **필수 입력**. 태어난 도시/지역명을 최대 200자로 입력(예: `Chicago`, `London`, `Tokyo`, `Beijing`, `Los Angeles`)
- 이메일/전화번호: **각 신청자(행)별로 입력** — 3절 참고
- ✅ 확정(2026-07-31): **"주소" 컬럼은 유지** — `ApplicationMember.address`(카드에 인쇄되는 주소), Nullable. 아래 `Receiver.address`(카드 실물 배송지)와는 별개 개념이니 혼동 주의:

| 필드 | 의미 | 소속 |
|---|---|---|
| `ApplicationMember.address` | 엑셀로 입력받는, **카드 전면에 인쇄되는 주소** | 인원별(단체는 행마다 다름) |
| `Receiver.address` | 실물 카드를 **배송받을 주소**(`issue_type=MOBILE_AND_PHYSICAL`일 때만) | 신청 1건당 1개(대표 수령인 기준) |

### 2-3. ✅ 입국날짜 — 공통값 + 개별 예외 방식 (2026-07-31 확정)

단체 신청 엑셀은 **표 바깥(상단)의 "공통 입국날짜" 1개 값**과 **표 안의 "개별입국날짜" 컬럼(예외자만 입력)**, 두 군데로 나뉜다.

```
=====================================================
단체 신청 정보

공통 입국날짜 : 2026-08-15
=====================================================

| 사진 번호 | 영문이름 | 국적 | 개별 입국날짜 |
|-----------|----------|------|---------------|
| 001       | John     | USA  |               |
| 002       | Mike     | USA  |               |
| 003       | Anna     | USA  | 2026-08-18    |
```

**적용 규칙**: 행의 "개별입국날짜"가 비어있으면 상단 "공통 입국날짜"를 적용, 채워져 있으면 그 값이 우선(공통값을 덮어씀).

| 이름 | 개별입국날짜(엑셀) | 적용되는 최종 `entry_date` |
|---|---|---|
| John | (비어있음) | 2026-08-15 (공통) |
| Mike | (비어있음) | 2026-08-15 (공통) |
| Anna | 2026-08-18 | 2026-08-18 (개별 우선) |

- "공통 입국날짜"도 여전히 선택 입력(비워두면 개별입국날짜가 없는 행은 `entry_date=NULL`).
- DB엔 이 해석 로직을 거친 **최종값만 `ApplicationMember.entry_date`에 저장** — "공통 입국날짜" 자체를 별도 컬럼으로 저장하지 않음(엑셀 파싱 시점의 입력 편의 개념일 뿐, 저장은 항상 인원별 최종값으로 정규화).
- 엑셀 템플릿(`GET /api/applications/bulk/template`) 및 검증 로직(`bulk/validate` 등, 도입 시)에 이 2단 구조(상단 요약 셀 + 표) 반영 필요 — 기존 설계는 표만 있는 단순 구조였어서 파싱 로직 변경 필요.

---

## 3. 이메일/전화번호 — 소속 단위 변경 (중요, 구조 영향)

| 신청유형 | 입력 방식 | 소속 단위 |
|---|---|---|
| 개인 | 신청 폼에서 입력 | `Applicant`(기존과 동일 — 신청인=카드 대상자 1명이라 사실상 같은 사람) |
| 단체 | 엑셀에 신청자별로 입력 | **`ApplicationMember`(신규 컬럼 필요)** |

### 구조 변경 필요 사항
기존 설계는 `Applicant.email`/`Applicant.phone`(단체 신청의 대표 신청인 1명 것)만 있었고, `ApplicationMember`엔 연락처 정보가 없었음. 이번 요구사항대로면:

- ✅ **`ApplicationMember`에 `email`/`phone` 컬럼 신규 추가(둘 다 Nullable)** — 단체 신청 시 엑셀의 각 행(각 신청자)이 자신의 이메일/전화번호를 가짐.
- `Applicant.email`/`Applicant.phone`은 그대로 유지 — 단체 신청의 "대표 신청인"(예: 인사담당자) 연락처로, 개별 카드 수령자와는 다른 개념.
- ✅ **확정(2026-07-31): 개인 신청은 `ApplicationMember.email`/`phone`을 비워둔다(NULL).** 가입 시점에 이미 `User.email`(필수)과 `User.phone`(선택)이 있으므로, 개인 신청은 `Applicant`(=로그인 계정 정보)만 참조하고 `ApplicationMember`엔 중복 저장하지 않음. → 즉 `ApplicationMember.email`/`phone`은 **단체 신청에서만 실제로 채워지는 값**.

### ✅ 신청 조회(`lookup`) API 로직 — 확정 방향
- **카드번호(`method=card`)로 조회할 때는 그 카드의 실제 소유자(`ApplicationMember`)를 기준으로 본인 인증한다** — `Applicant.phone`이 아니라 조회 대상 `ApplicationMember.phone`/`email`과 대조.
- **전화번호 인증과 이메일 인증을 별도 채널로 둔다** — 사용자가 phone 또는 email 중 하나로 인증 가능(정확한 조합: 둘 다 필수인지, 둘 중 하나만 있으면 되는지는 [TBD] — API 상세 설계 시 확정).
- 개인 신청 조회는 기존처럼 `Applicant.phone`/`email`(=로그인 계정 정보)과 대조 — `ApplicationMember`가 비어있는 케이스이므로 자연스럽게 `Applicant` 참조로 귀결됨.

---

## 4. ZIP 파일 (단체 신청)

✅ 확정(기존과 동일): 단체 신청 시 **엑셀 + 사진을 포함한 ZIP 파일 업로드는 필수**. 선택 아님.

- ✅ 2026-08-07 확정(`APPLICATION.md` 기준): Excel은 ZIP 루트에 1개만 허용하며 파일명은 자유이고 확장자는 `.xlsx`만 허용한다. 2개 이상이면 신청 전체를 실패 처리한다.
- ✅ 2026-08-18 확정: A열 `사진 번호`는 공식 양식에 `001`~`100` 텍스트 값으로 미리 입력하며 잠금 처리한다. 사용자가 직접 입력하거나 수정하지 않는다.
- ✅ 2026-08-18 확정: 사진 번호만 있는 행은 빈 행으로 무시하고, B열 이후 신청자 정보가 하나라도 입력된 행만 검증·처리한다.
- ✅ 2026-08-07 확정: 사진 확장자는 대소문자를 구분하지 않는다(`001.JPG`, `001.jpg`, `001.Jpg` 모두 허용).
- ✅ 2026-08-18 확정: `__MACOSX`, `.DS_Store`는 무시하며 단체 신청 최대 인원은 100명이다.
- 실제 처리되는 행의 사진 번호와 ZIP 내부 사진 파일명을 정확히 매칭한다. 예: `사진 번호=001`은 `001.jpg` 또는 `001.png`와 매칭한다.
- 신청자 정보가 없는 행 번호의 사진을 넣으면 여분 사진(`PHOTO_UNMATCHED`)으로 전체 실패한다.
- 사진 번호는 ZIP 안에서 사진을 찾기 위한 임시 식별자이며 DB에 저장하지 않는다.
- 구성원 사진마다 별도의 사진 파일 ID를 생성하지 않는다. 매칭된 이미지의 저장 경로만 `ApplicationMember.photo_path`에 저장한다.
- 제출 ZIP 원본은 신청 단위 파일이므로 기존 `Application.submit_file_id`로 관리한다. 이는 구성원별 사진 파일 ID와 구분한다.

---

## 5. 학생증(STUDENT) 추가 항목

| 항목 | 소속 단위 | 이유 |
|---|---|---|
| 학번 | `ApplicationMember`(개인별) | 단체 신청 시 N명 각각 학번이 다름 |
| 학과 | `ApplicationMember`(개인별) | 동일 |
| 학교 로고 | `Application`(신청당 1회) | 개인/단체 무관하게 카드에 공통으로 들어가는 요소 |
| 학교 직인 | `Application`(신청당 1회) | 선택 입력. 제공된 경우에만 저장 |
| 가로형/세로형(`orientation`) | `Application`(신청당 1회) | ✅ 2026-08-14 확정. 카드 방향, 개인·단체 공통 |
| 학교구분(`schoolType`) | `Application`(신청당 1회) | ✅ 2026-08-14 확정. `UNIVERSITY`/`HIGH_SCHOOL`, 개인·단체 공통 |
| 학교명(`schoolName`) | `Application`(신청당 1회) | ✅ 2026-08-19 신규 확정. 단체 신청은 항상 한 학교 단위로 접수되므로 `orientation`/`schoolType`과 동일하게 신청서 전체에 1개 |

✅ 확정: **학생증은 개인신청/단체신청 둘 다 처리방침이 동일** — 학교 로고는 항상 필요하고 학교 직인은 선택이다.

✅ 2026-08-07 확정(`APPLICATION.md` 기준): **학번은 최대 10자이며 숫자만 허용한다.**

[TBD] **학과 형식 제약**(글자수 등)은 여전히 미정 — ⚠️ 2026-08-07: `APPLICATION.md`는 "학과 현재 제외"라고 되어 있으나 근거가 제시되어 있지 않고 사람이 아직 미결정으로 확인(`PENDING_DECISIONS.md` 참고). 그래서 이 문서는 학과 필드를 계속 필수로 유지한다 — 위 표의 "학과" 행 그대로.

### 5-0. 학교명(`schoolName`) — 2026-08-19 정책 확정

기존에 "학교명 필드 필요 여부는 [TBD]"였던 항목을 아래와 같이 확정한다.

- **위치**: `Application` 레벨 단일 필드(개인·단체 공통, `orientation`/`schoolType`과 동일한 위치). 단체 신청은 항상 한 학교 단위로 접수되는 것이 전제이므로 `BulkExcelParser`(엑셀 행별 필드)가 아니라 신청 폼 최상위 필드다 — 학번/학과와는 소속 단위가 다르다.
- **필수 조건**: 카드종류=학생증(`isStudent`)이면 `schoolType`이 `UNIVERSITY`/`HIGH_SCHOOL` 어느 쪽이든 **항상 필수**다(학번/학과와 달리 `UNIVERSITY` 전용 조건 없음). 비학생증 카드는 항상 값이 없어야 하며, 있으면 `INVALID_INPUT`으로 거절한다(`orientation`/`schoolType`과 동일한 대칭 패턴).
- **DB 제약**: `orientation`/`schoolType`/`studentId`/`department`와 동일하게 **DB 컬럼은 nullable**로 두고, "학생증일 때만 필수"는 서비스 레벨(`validateStudentFields`, `createGroup`)에서만 강제한다. DB에 `NOT NULL` 제약을 걸면 비학생증 신청(명예한국인증/명예시민증/방문증) 저장이 깨지므로 걸지 않는다.
- **길이**: 최소 5자, 최대 20자. 저장 전 앞뒤 공백을 트림한 뒤 그 길이로 검사한다.
- **허용 문자**: 한글 + 영문 + 숫자 + 공백만 허용(정규식으로 강제, 그 외 특수문자는 `INVALID_INPUT`).

### 5-1. 얼굴사진·학교 로고·학교 직인 파일 검증

- 얼굴사진은 필수이며 빈 파일을 허용하지 않는다.
- 최대 크기는 `5 * 1024 * 1024` bytes(5 MiB)다.
- 허용 확장자는 `jpg`, `jpeg`, `png`다.
- 허용 MIME은 `image/jpeg`, `image/png`다.
- 요청 MIME만 신뢰하지 않고 파일 signature와 실제 이미지 디코딩 결과를 검증한다.
- 확장자·MIME·signature·실제 이미지 형식은 서로 일치해야 한다.
- 얼굴사진은 EXIF Orientation을 적용한 최종 표시 방향을 기준으로 가로 300px 이상, 세로 400px 이상이어야 한다.
- 학생증 학교 로고와 제공된 학교 직인에도 동일한 용량·확장자·MIME·signature·디코딩 검증을 적용한다.
- 최소 해상도 제한은 얼굴사진에만 적용하고 학교 로고·직인에는 적용하지 않는다.
- 모든 파일 검증은 object storage 업로드와 DB 저장 전에 완료한다.
- 오류 코드는 기존 `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `INVALID_IMAGE`를 재사용하고 신규 세부 ErrorCode를 추가하지 않는다.

### 5-2. `schoolName` 구현 순서 체크리스트 (2026-08-19 작성 → 2026-08-19 완료)

> ✅ **SCHOOLNAME-1 구현·테스트·문서 반영 완료**(2026-08-19). 개인·단체 등록 API 둘 다 `schoolName`을 받고 저장한다. 전체 스위트 471개(신규 9개 포함, 이전 462개) 중 `UserApplicationFlowTest.fullUserApplicationFlow`(pre-existing, 이 변경과 무관) 1건만 실패, 회귀 없음.

단위 내부 순서: **정책 재확인(위 5-0) → 실패하는 테스트 먼저 작성 → 최소 구현 → 단위 테스트 통과 확인 → 전체 스위트 회귀**(RULES.md §8 — 이 도메인 내부 변경이라 전체 스위트는 마지막에만 실행).

### SCHOOLNAME-1. `Application.school_name` 추가 (개인 + 단체 경로 한 번에)

- [x] `Application.java`: `schoolName` 컬럼 추가(`@Column(length = 20)`, nullable), `createIndividual(...)`/`createGroup(...)`에 파라미터 추가(+ 기존 하위호환 오버로드 유지)
- [x] `ApplicationCreateRequest.java`/`BulkApplicationCreateRequest.java`: `schoolName` 최상위 필드 추가(`orientation`/`schoolType` 옆) + 커스텀 getter로 트림 적용
- [x] `ApplicationFactory.java`/`ApplicationPersistenceService.java`: 파라미터 관통
- [x] `ApplicationService.validateStudentFields(...)`: `schoolName` 필수/트림/길이(5~20)/문자셋(한글·영문·숫자·공백) 검증 추가 — `schoolType` 무관하게 학생증이면 항상 필수, 비학생증이면 있으면 거절
- [x] `ApplicationService.createGroup(...)`: 동일 조건의 `schoolName` 검증 추가
- [x] 신규 테스트 9개 (개인 6 + 단체 3): 누락/5자 미만/20자 초과/허용 외 문자/트림 후 저장값/비학생증 거절(개인), 누락/허용 외 문자/비학생증 거절(단체) — 대학교·고등학교 성공 케이스는 기존 성공 테스트에 `schoolName` 추가로 흡수
- [x] `docs/specs/application/data-model.md`: `Application.school_name` 컬럼 추가
- [x] `docs/specs/application/api.md`: 개인/단체 등록 API 두 곳(요청 예시, Validation 표, 매핑 표)에 `schoolName` 반영
- [x] 영향 범위 재확인: `git diff`로 예상 밖 변경 파일 없음 확인. 학생증 픽스처가 있던 3개 파일(`ApplicationServiceTest`, `ApplicationServiceBulkTest`, `ApplicationServiceUploadCompensationTest`) 전부 갱신 — `ApplicationControllerTest`/`ApplicationBulkControllerTest`는 학생증 픽스처 자체가 없어 영향 없음(grep으로 확인)
- [x] 전체 스위트 회귀 실행 — 결과는 위 완료 배너 참고

---

## 6. 카드 디자인 배정 방식 — 정책 변경 (중요)

### 기존 설계/프론트 동작
- 프론트: 홈/`DesignPage.tsx`에서 사용자가 구체적 디자인(예: `honorary-korean-03`)을 클릭 → `/apply?designId=xxx`로 진입 → 신청 시 그 `designId`가 그대로 제출됨.
- `docs/api/README.md` Application API 1/2 request body에 `cardDesignId`가 사용자 입력값으로 포함되어 있었음.

### 변경된 정책
✅ **사용자는 "카드 종류"만 선택하고, 구체적 디자인(`CardDesign`)은 관리자가 신청 검토 과정에서 배정한다.**
- 근거: 디자인이 발행 지자체별로 갈리는 구조라(예: 서울시/전주시 등) 사용자가 미학적으로 고를 성격이 아니라 행정적으로 배정되는 값.
- `Application.card_design_id`는 신청 생성 시점엔 `NULL`, 관리자가 이후 채움(구체적으로 Admin 흐름의 어느 시점에 배정하는지는 Admin API 설계 시 확정 필요 — [TBD]).
- 신청 생성 API(개인/단체)의 request body에서 `cardDesignId` **제거**.

### 프론트 영향 (참고, 이번 백엔드 작업 범위는 아님)
- `DesignPage.tsx`의 디자인 갤러리는 "예시 전시"로 성격이 바뀜(신청과 연결 안 됨) — `/apply?designId=xxx` 딥링크 방식 재검토 필요.
- `/apply` 진입 시 필요한 건 `cardType`(카드종류)뿐, 특정 `designId`는 더 이상 필요 없음.

---

## 7. 사용자 흐름 (프론트 5단계 기준)

현재 `ApplyPage.tsx` 5단계: **유형 선택 → 정보 입력 → 사진/파일 등록 → 최종 확인 → 신청 완료**

### 7-1. 공통 흐름
1. **유형 선택**: 카드 종류 선택(1절 4종 중 1개) + 개인/법인·단체 선택 + 사전상담 확인 체크
2. **정보 입력**: 발급방식(모바일/모바일+실물), 신청인 정보(2·3절), (실물일 때만) 수령인 정보 — **개인 신청은 카드종류 무관하게 전부 사주 정보 입력 필요** (7-3절 프론트 불일치 참고)
3. **사진/파일 등록**: 카드종류·신청유형에 따라 다름 (7-2절 매트릭스 참고)
4. **최종 확인**: 입력 내용 리뷰
5. **신청 완료**: 신청번호 발급 + 입금 안내(계좌정보 + 입금자명 입력)

### 7-2. 카드종류 × 신청유형별 필요 항목 매트릭스

| | 명예한국인증/명예시민증/방문증 — 개인 | 명예한국인증/명예시민증/방문증 — 단체 | 학생증 — 개인 | 학생증 — 단체 |
|---|---|---|---|---|
| 사주정보(영문명/국적/생년월일/출생시간·선택/**출생지역·필수**/성별/**입국날짜·선택**) | ✅ (`ApplicationMember`, 1건) | 엑셀 행별(`ApplicationMember`, N건) | ✅ | 엑셀 행별 |
| 이메일/전화번호 | ✅ (`Applicant`만, `ApplicationMember`는 NULL) | 엑셀 행별(`ApplicationMember`, 신규 컬럼) | ✅ (`Applicant`만) | 엑셀 행별 |
| 본인 얼굴사진 | ✅ | 엑셀+ZIP 내 사진 | ✅ | 엑셀+ZIP 내 사진 |
| 학번/학과 | — | — | ✅ (`ApplicationMember`) | 엑셀 행별(`ApplicationMember`) |
| 법인/단체명, 부서명 | — | ✅ (`Applicant`/`Receiver`) | — | ✅ |
| 로고(회사) | — | ✅ (`Application.logo_file_id`) | — | — |
| 직인(회사) | — | ✅ (`Application.seal_file_id`) | — | — |
| 학교 로고 | — | — | ✅ (`Application.logo_file_id`) | ✅ (`Application.logo_file_id`) |
| 학교 직인 | — | — | 선택 (`Application.seal_file_id`) | 선택 (`Application.seal_file_id`) |
| 제출 ZIP(엑셀+사진) | — | ✅ 필수 | — | ✅ 필수 |

**정리**: `Application.logo_file_id`는 학생증이면 개인·단체 모두 사용하고, 일반 카드는 단체 신청에서 사용한다. `Application.seal_file_id`는 일반 단체 신청에서는 필수이며 학생증 개인·단체 신청에서는 선택이다.

### 7-3. 발견한 프론트 코드 불일치 (수정 필요, 백엔드 설계와 별개)

`StepInfo.tsx`/`StepFiles.tsx` 실제 코드 확인 결과, 다음이 위 매트릭스와 다르게 구현되어 있음 — 프론트 작업 시 같이 고쳐야 함:

- **개인/단체 분기가 아예 없음**: 현재는 `applicantType`이 아니라 `cardType === "visitor"` 여부로만 분기 중. 로고/직인/제출ZIP이 "단체일 때"가 아니라 "방문증이 아닐 때" 무조건 노출되고 있음.
- **사주 정보 입력폼이 방문증에만 있음**: `isVisitor`일 때만 영문이름/국적/생년월일/출생시각/출생지역/성별 필드가 보이고, 나머지 3종(명예한국인증/명예시민증/학생증)은 이름/연락처/이메일만 받는 단순폼임. 매트릭스대로면 **4종 전부** 사주 정보가 필요함.
- **한국입국날짜/"출생시간 모름" 체크 시 미입력 처리**: 현재 프론트엔 입국날짜 필드 자체가 없음(신규 추가 필요), 출생시간 "모름" 체크박스는 이미 있음(`birthTimeUnknown`, `visitor` 폼에만 존재).

### 7-4. 결제 금액·입금 기한·환불 정책

#### 결제 금액

- 개인/단체 신청 모두 **상담을 먼저 진행한 후 신청**한다.
- 신청 이후 결제는 계좌이체 방식으로 진행한다.
- Application 신청 생성 단계에서는 결제 금액을 계산하거나 저장하지 않는다.
- 카드 종류별 고정 금액 하드코딩, `CardType.price × total_quantity` 자동 계산 및 `Application.total_price`는 현재 범위에서 사용하지 않는다.
- 상담 결과와 실제 입금 내역을 시스템에서 관리하는 방식은 Payment 도메인 설계 시 별도로 확정한다.

### 7-5. Application 생성 시 서버 값 책임

- 신청번호는 클라이언트가 보내지 않으며 Application Service가 생성한다. 번호 생성 전략의 분리는 Task 6에서 다룬다.
- 초기 신청 상태 `SUBMITTED`와 초기 결제 상태 `WAITING`은 모든 생성 경로에 공통인 Application 불변조건이므로 Entity가 설정한다.
- 수령인 동일 여부는 Request 자체에서 계산 가능한 단순 파생값이다. ✅ 2026-08-07 정정(`APPLICATION.md` 기준): `MOBILE_AND_PHYSICAL`에서 `sameAsApplicant=true`이면 **이름과 연락처만** 복사하며(배송지는 복사하지 않고 Receiver가 항상 입력), 복사 후 수정할 수 있다.
- 결제 금액과 구성원별 사진 파일 ID는 Application 생성 시 서버 준비값에 포함하지 않는다.
- 위 값들은 하나의 준비 객체로 함께 전달할 필요가 없으므로 현재는 Context, Plan, `prepareServerValues()`를 추가하지 않는다.

### 7-6. ApplicationFactory와 IDENTITY 저장 순서

- Application은 기존 `IDENTITY` 식별자 전략을 유지한다.
- Factory는 먼저 Application을 생성하고 Service가 이를 저장한다. DB에서 ID가 발급된 뒤 Factory가 Applicant, Receiver, ApplicationMember를 생성한다.
- 하위 Entity는 유효한 `applicationId`가 발급된 후에만 생성하며 null FK 상태의 중간 객체를 만들지 않는다.
- Factory는 Entity 생성만 담당한다. 검증, 파일 업로드, Repository 저장, 트랜잭션 흐름은 Application Service가 담당한다.
- 별도 CreatedApplication, CreatedChildren, Context, Plan 객체는 추가하지 않는다.

ApplicationType, IssueType, CardType별 Service 책임과 개인·단체 생성 순서는 [service-flow.md](service-flow.md)를 기준으로 한다.

#### 신청·결제 진행

- 상담은 신청 전에 완료한다.
- 신청서 제출 시 `SUBMITTED + WAITING`으로 생성한다.
- 관리자가 결제를 안내한 최초 시각을 `paymentGuidedAt`에 기록하고 `paymentDueAt=paymentGuidedAt+72시간`으로 설정한다.
- 결제 안내를 다시 보내도 기존 기한은 초기화하거나 연장하지 않는다.
- 입금 확인은 ApplicationStatus를 변경하지 않고 PaymentStatus만 `CONFIRMED`로 변경한다.
- 이미 `CONFIRMED`인 신청의 입금 확인 재호출은 값을 변경하지 않는 멱등 성공으로 처리한다.
- 사진·내용 검토는 `SUBMITTED + CONFIRMED`에서만 시작할 수 있다.

#### 사용자 취소·환불

| 신청 상태 | 사용자 취소 | 결제 상태 처리 |
|---|---:|---|
| `SUBMITTED` | 가능 | `WAITING` 또는 `CONFIRMED` 유지 |
| `REVIEWING` | 가능 | 정책상 `CONFIRMED` 유지 |
| `PHOTO_REJECTED` | 가능 | 정책상 `CONFIRMED` 유지 |
| `NAME_EDITING` 이후 | 불가 | 변경 없음 |
| `CANCELLED` | 멱등 성공 | 기존 값과 취소 이력 유지 |

- `CANCELLED + WAITING`은 미입금 취소이므로 환불이 필요 없다.
- `CANCELLED + CONFIRMED + refundedAt=null`은 전액 환불 대기다.
- `CANCELLED + CONFIRMED + refundedAt!=null`은 관리자가 외부에서 전액 환불한 사실을 기록한 상태다.
- PaymentStatus는 실제 입금 확인 이력이므로 취소·환불 후에도 변경하지 않는다.
- 관리자 직접 취소는 이번 구현 범위에서 제외한다.

#### 미입금 자동 취소

- 자동 취소 대상은 `SUBMITTED + WAITING + paymentDueAt<=now`인 신청이다.
- 스케줄러는 기본 10분 주기로 실행하며 설정값으로 변경 가능하게 한다.
- 자동 취소 후 늦은 입금이 확인돼도 신청을 재활성화하지 않고 `CANCELLED + CONFIRMED` 환불 대상으로 관리한다.
- 사용자 취소와 미입금 자동 취소가 최초 commit되면 신청 전용 S3 파일은 commit 직후 바로 삭제한다. rollback 시에는 삭제하지 않는다.

---

## 8. 단건/다건 CRUD 정책

| 구분 | 개인(단건) | 단체(다건) |
|---|---|---|
| **Create** | `POST /api/applications` — JSON 1명분 | `POST /api/applications/bulk` — ZIP(엑셀+사진), **필수** |
| **Read(사용자)** | `POST /api/applications/lookup`(비로그인, 신청번호+연락처) / `GET /api/my/applications`(로그인) | 동일 API — phone 대조 로직은 2절 하단 [TBD] 참고 |
| **Read(관리자)** | `GET /api/admin/applications`, `GET /api/admin/applications/{id}` | 동일, `members` 배열로 N명 페이지네이션 |
| **Update(사용자)** | 사진 재업로드(`PATCH .../photo`, `PHOTO_REJECTED` 상태에서만) / 신청 취소(`POST .../cancel`) / 입금자명 등록(`PATCH .../payment`) | 동일 API — 단체는 엑셀+ZIP 전체 재제출 |
| **Update(관리자)** | 결제안내/입금확인/사진검토/작명·편집/제작승인/카드준비/실물 인계/환불완료 (Admin API) | Application 전체 단위로 동일하게 적용(개별 멤버 단위 아님) |
| **Delete** | 명시적 삭제 API 없음 — `status=CANCELLED` 전이로 대체(소프트) | 동일 |

**신청 내용 자체(카드종류/인적사항 등) 수정 API는 없음** — 한번 제출하면 재작성 불가, 반려된 사진만 재업로드 가능. [TBD] 이 방침이 맞는지(오타 등으로 인한 수정 요청은 고객센터 문의로 처리하는 건지) 확인 필요.

---

## 9. 예외처리 정책

| 상황 | 처리 방침 |
|---|---|
| 단체 신청 중 일부 인원 사진 반려 | ✅ 기존 확정: `Application` 전체가 `PHOTO_REJECTED`로 전환 — 개별 인원 단위 상태 없음 |
| 단체 신청 검증 오류 | ✅ 2026-08-07 확정(`APPLICATION.md` 기준): 옛 "실패율 30% 룰"은 폐기(Legacy). **오류가 하나라도 있으면 부분 성공 없이 신청 전체를 실패 처리**하고 `BULK_APPLICATION_VALIDATION_FAILED` + `errors[]`로 상세 오류를 반환한다 |
| 학생증 학번 누락·형식 오류 | ✅ 2026-08-07 확정: 필수값 검증으로 막음. 최대 10자·숫자만 허용 |
| 학생증 학과 누락 | [TBD] 필수값으로 막을지, 관리자가 나중에 보완 가능하게 할지 — ⚠️ `APPLICATION.md`는 "학과 제외"로 되어 있으나 근거 없음, 사람 확인 결과 미결정 유지(`PENDING_DECISIONS.md`) |
| 개인 신청 필수 사주정보(영문명/국적/생년월일/성별) 미입력 | 필수값 검증으로 막음(기존 방침) — 단, 출생시간/입국날짜는 선택이며 출생지역은 필수 검증 |
| 단체 신청 ZIP 미첨부 | 필수값 검증으로 막음(4절) |

---

## 10. 확인 필요 사항 (TBD 종합)

- 관리자가 `CardDesign`을 정확히 **언제** 배정하는지(신청 접수 직후? 사진검토 통과 후? 작명 단계?) — Admin API 설계 시 확정 필요
- 학과 형식 제약(학번은 2026-08-07 확정됨 — 최대 10자·숫자만)
- 학과 필드를 계속 유지할지, 제외할지 자체(`APPLICATION.md`가 "제외"로 적었으나 근거 없음, 미결정)
- 학생증 공백 문자열 정책
- 학교명 필드 추가 여부
- 학생증 디자인 시안(아직 미도착)
- 신청 내용 수정 API 필요 여부
- **신청조회(`lookup`) API의 전화번호 인증 vs 이메일 인증 조합** — 둘 다 필수인지, 둘 중 하나만 있어도 되는지(3절) — API 상세 설계 시 확정

### 확정 운영 정책 (2026-08-07, `APPLICATION.md` 기준)

- `sameAsApplicant=true`이면 이름과 연락처만 Receiver에 복사하며 복사 후 수정할 수 있다. 배송지는 Receiver가 항상 입력한다. `MOBILE`은 Receiver를 금지하고 `MOBILE_AND_PHYSICAL`은 Receiver를 필수로 한다.
- `ApplicationService`는 비트랜잭션 오케스트레이터로 검증·ZIP 파싱·파일 준비·업로드를 수행하고, 별도 `ApplicationPersistenceService`의 `@Transactional` 메서드가 `Application`·`Applicant`·`Receiver`·`ApplicationMember`를 저장한다. Spring 자기호출은 사용하지 않는다.
- 상담 완료 여부는 프론트 UX 안내이며 백엔드는 검증하지 않는다.
- 일일 신청은 KST 하루 개인·단체 합산 3회까지 허용하고 4번째부터 `APPLICATION_LIMIT_EXCEEDED`로 거절한다. 동시 요청은 DB 수준에서 원자 처리하며 현재 리팩터링 범위에서는 구현하지 않는다.
- 신청번호는 `count+1`이 아니라 `application_seq.nextval` 기반 DB Sequence로 생성한다.
- 요청 멱등성은 현재 구현하지 않고 프론트 신청 버튼 비활성화로 처리한다.
- 얼굴사진·학교 로고·직인은 새 파일 업로드와 DB 갱신 성공 후 기존 파일을 삭제한다.

---

이 문서 확정 후 [data-model.md](data-model.md)(`ApplicationMember`에 `entry_date`/`email`/`phone` 추가, `birth_region`/`birth_time` Nullable로 정정, CardType 학생증 필드 반영) → [api.md](api.md)(Application API 1/2에서 `cardDesignId` 제거, 신규 필드 반영) 순으로 이어서 반영합니다.
