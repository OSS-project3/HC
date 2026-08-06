# TODO (작업 보드)

상태 아이콘: ✅ 완료 · 🔵 진행중 · ⚪ 대기 · 🔴 블로킹(질문 대기)

작업을 시작할 때 상태를 `🔵 진행중`으로, 담당자를 채우고 커밋한다.
작업을 마칠 때 `✅ 완료`로 바꾸고 `CHANGELOG.md`에 항목을 추가한다.

---

## 📌 개발 원칙 (고정 — 모든 Task 공통, 예외 없음)

이 TODO에 정의한 개발 순서는 **반드시** 따른다. (아래는 "Application 개인 신청 리팩터링 로드맵"의 "공통 진입·완료 원칙"과 같은 내용을 더 짧게 고정해둔 버전 — 세부 절차는 그쪽 참고)

1. 정책에 맞는 테스트를 먼저 작성한다.
2. 신규 테스트가 기존 구현에서 **의도대로 실패**하는 것을 확인한다.
3. 테스트를 통과시키는 **최소한의** 구현만 추가한다.
4. 기존 동작은 변경하지 않는다.
5. Task 범위를 벗어나는 Factory/Context/Generator 등의 리팩터링은 진행하지 않는다.
6. 완료 후 **테스트 결과와 변경 사항을 보고**하고 `TODO.md`/`CHANGELOG.md`를 업데이트한다.

## 템플릿 (새 작업 추가 시)

```md
| ⚪ | {작업명} | {담당: Claude/Codex/미정} | {브랜치} | {관련 문서} | {비고} |
```

---

## 진행 보드

| 상태 | 작업 | 담당 | 브랜치 | 관련 문서 | 비고 |
|---|---|---|---|---|---|
| ✅ | User CRUD (조회/수정/탈퇴/자동복구/익명화 스케줄러) 구현+테스트 | Claude | `backend-api` (병합됨) | `backend/honor-citizen/docs/test/user-test-result.md` | - |
| ✅ | API-명세.md → `docs/api/*.md` 도메인별 분리 | Codex | `feature/application-domain-docs` | `docs/api/README.md` | 원본과 대조하여 내용 유실 없음 확인 완료 |
| ✅ | Application 문서 도메인 패키지 이전 | Codex | `feature/application-domain-docs` | `docs/specs/application/` | requirements/data-model/api/checklist 구성 및 구 경로 참조 수정 완료 |
| ✅ | `arch.md` 구조를 실제 코드 규모에 맞게 단순화 | Claude | `feature/application-domain-docs` | `arch.md` | 비즈니스 규칙 절은 유지, 계층/패키지 구조만 축소 |
| ✅ | 협업 규칙 체계(`docs/collab/`) 도입 | Claude | `backend-api` | `docs/collab/RULES.md` | - |
| ✅ | Application 도메인 엔티티/API 구현 | Claude | `feature/application-domain-impl` | `docs/specs/application/*.md` | API 1~5 전부 완료, 신규 테스트 46개 전부 통과. checklist.md 6개 섹션 검증 완료(결과는 HANDOFF.md 참고) |
| ⚪ | Codex: HANDOFF.md "확인 필요" 3건을 `docs/specs/application/*`에 반영 | Codex | `feature/application-domain-docs` | `docs/specs/application/{requirements,data-model,api}.md` | englishName 추가, total_price 보류 각주, 엑셀 부분실패=전체거부 확정 — 사람 확인 끝난 결정사항, 문서 반영만 필요 |
| ✅ | 조회(`lookup`) 인증 정책 method별 분리 구현 | Claude | `feature/application-domain-impl` | `docs/specs/application/api.md`, `backend/FRONTEND_API_REQUIREMENTS.md`(main) | `application`=phone+email 둘 다 필수, `card`=인증값 없음. 상세는 CHANGELOG 2026-08-06 참고 |
| ✅ | `CardTypeSeeder` 추가 — CardType ID 1~4 고정 시딩 | Claude | `feature/application-domain-impl` | `backend/FRONTEND_API_REQUIREMENTS.md`(main) §4 | 프론트 `cardTypeId` 하드코딩(1~4)을 그대로 쓰기로 결정, `GET /api/card-types` 신규 API는 만들지 않음 |
| ⚪ | 단체 재제출 UI(`MobileCardPage.tsx`) 추가 | 프론트 담당자 | `main` | `backend/FRONTEND_API_REQUIREMENTS.md`(main) | 백엔드는 이미 구현됨(`PATCH .../photo`의 `submitFile` 파트, `PHOTO_REJECTED` 상태에서만 허용). 프론트에 단체용 업로드 UI만 없음 |
| ⚪ | `StepInfo.tsx`에 4개 카드종류 전부 사주정보 입력폼 추가 | 프론트 담당자 | `main` | `docs/specs/application/requirements.md` §7-3 | 현재 방문증에만 있음. 나머지 3종은 목데이터라 안 드러날 뿐 실제 API 연동 시 400 발생 |
| ⚪ | Payment/상담금액/자동취소/환불 도메인 설계·구현 | 미정 | - | `docs/specs/application/requirements.md` | 이번 Application 구현 범위 밖(api.md 스코프 노트 참고). checklist.md 1절 미충족 3건이 여기 해당 |
| ⚪ | Admin 도메인(사진검토/작명/카드발급/CardDesign 배정) 설계·구현 | 미정 | - | `docs/specs/application/api.md` | 아직 미착수, api.md에 "이번 범위 아님"으로 명시돼 있던 부분 |
| ⚪ | CardDesign 관리자 배정 API/화면 흐름 확정 | 미정 | - | `docs/specs/application/requirements.md` | "CardDesign 배정 시점" TBD 선결 필요 |
| ⚪ | 학번/학과 형식 제약 확정 | 미정 | - | `docs/specs/application/requirements.md` | TBD |
| ⚪ | 학생증 디자인 시안 반영 | 미정 | - | `DB.md`, `docs/api/card-design.md` | 시안 미도착 |
| ⚪ | 단체신청 엑셀 ZIP 레이아웃(공통입국날짜 셀 위치, 컬럼 순서) 명문화 | 미정 | - | `docs/specs/application/api.md` | 구현 시 임의로 확정한 레이아웃(HANDOFF.md 참고) — bulk/template API 설계 시 반드시 일치 필요 |
| ⚪ | 신청내용 수정 API 필요 여부 결정 | 미정 | - | `docs/specs/application/api.md` | TBD |
| ⚪ | refresh 토큰 세션 저장소(DB vs Redis) 결정 | 미정 | - | `arch.md` | TBD |
| ⚪ | MOBILE_AND_PHYSICAL 실물배송 흐름 정의 | 미정 | - | `docs/specs/application/api.md` | TBD |
| ⚪ | 영업일 계산 기준 확정 | 미정 | - | `docs/specs/application/api.md` | TBD |
| ⚪ | 입금 기한 계산 기준(신청일 포함 여부·마감 시각) 확정 | 미정 | - | `docs/specs/application/requirements.md` | TBD |
---

## Application 개인 신청 리팩터링 로드맵

> 대상 브랜치: `feature/application-domain-impl`
>
> 기준 문서: `arch.md`, `docs/specs/application/requirements.md`, `docs/specs/application/checklist.md`
>
> 구현 순서: Task 1 → Task 2(User Validation) → Task 3(Photo/Student Validation) → Task 4(서버 생성값) → Task 5(Factory) → Task 6(Generator/보상 처리)
>
> 각 Task는 범위를 섞지 않는다. 특히 구조 리팩터링 Task에서는 확정되지 않은 검증 정책이나 운영 개선을 함께 구현하지 않는다.

### 모든 Task의 공통 진입·완료 원칙

각 Task는 반드시 다음 순서로 진행한다.

1. 해당 Task에서 구현하거나 변경할 정책을 Source of Truth인 Requirements/API/DB/Architecture 문서에 먼저 반영한다.
2. 정책이 미결정이거나 문서끼리 충돌하면 구현을 시작하지 않고 `[TBD]` 또는 blocking 항목으로 등록한다.
3. 확정된 정책을 검증하는 테스트를 먼저 작성한다.
4. 신규 테스트가 기존 구현에서 의도한 이유로 실패하는지 확인한다.
5. 테스트를 통과시키는 최소 범위로 구현 또는 리팩터링한다.
6. 관련 도메인 테스트와 전체 테스트를 실행해 회귀 여부를 확인한다.
7. 완료 결과와 남은 실패를 본 체크리스트 및 `CHANGELOG.md`에 반영한다.

추가 원칙:

- Factory, Validator, Generator 같은 구조 자체를 먼저 만들지 않는다. 확정된 정책과 테스트가 요구할 때만 도입한다.
- 리팩터링 Task에서는 별도로 명시하지 않은 Request/Response, 예외, 저장 순서 및 외부 동작을 변경하지 않는다.
- 전체 테스트에 기존 실패가 있으면 기준 브랜치에서도 재현되는지 분리하여 기록한다.

### 구현 중 구조 변경 원칙

Factory, Validator, Context 등 새로운 클래스를 추가하기 전에 반드시 다음을 확인한다.

- 현재 클래스의 private 메서드 분리만으로 해결 가능한가?
- 새로운 클래스가 두 곳 이상에서 재사용되는가?
- 책임이 명확하게 독립되는가?

위 조건을 만족하지 않으면 기존 클래스 내부에 유지하고, 필요성이 생겼을 때만 분리한다.

### Task 1. createIndividual() 메서드 분리

상태: ✅ 완료 — Application 회귀 테스트 통과, 전체 테스트의 기존 User 실패 2건 분리 기록

- [x] `createIndividual()`의 기존 실행 순서를 유지한 채 private 메서드로 분리
- [x] `validateCreateIndividual()` 추출
- [x] `createIndividualApplication()` 추출
- [x] `createIndividualApplicant()` 추출
- [x] 기존 `saveReceiverIfNeeded()` 재사용
- [x] `createIndividualMember()` 추출
- [x] Factory, Validator, Context를 만들지 않음
- [x] 정상 Request/Response와 저장 순서 유지. User 미존재 시 `USER_NOT_FOUND`를 가장 먼저 반환하도록 오류 우선순위만 확정 정책대로 변경
- [x] Application 관련 기존 테스트 전체 통과
- [x] User 존재 여부를 이후 로직의 전제조건으로 정의하고 가장 먼저 조회
- [x] 존재하지 않는 User 테스트를 구현 전에 추가
- [x] User가 없으면 파일 업로드가 호출되지 않는지 검증
- [x] User가 없으면 Application과 하위 Entity가 저장되지 않는지 검증
- [x] 신규 테스트가 기존 구현에서 의도한 이유로 실패하는지 확인
- [x] User 조회를 CardType 조회·신청번호 생성·파일 업로드·DB 저장보다 앞으로 이동
- [x] 새로운 Factory, Validator, Context를 추가하지 않음
- [x] 최소 구현 후 Application 회귀 테스트 실행
- [x] Task 완료 결과를 TODO와 CHANGELOG에 기록
- [ ] 전체 테스트 통과
  - 현재 84개 중 `UserControllerTest` 2개가 독립 실행에서도 실패하며 Task 1 전후 동일
  - 실패 위치: `withdrawMarksUserWithdrawnAndBlacklistsAccessToken()`, `withdrawReturnsAlreadyWithdrawnOnSecondCall()`
  - Application 리팩터링과 무관한 기존 실패인지 기준 브랜치에서 최종 확인 필요

### Task 2. User Validation과 신청 제한

상태: ✅ 완료

#### User 및 신청 자격

- [x] User 조회를 신청번호 생성, 파일 업로드, DB 저장보다 먼저 수행
- [x] 회원 존재 검증 — `USER_NOT_FOUND` 재사용
- [x] 회원 상태가 `ACTIVE`인지 검증 — `ALREADY_WITHDRAWN` 재사용
- [x] 탈퇴 회원 거절 — 신규 ErrorCode 추가 없음
- [x] `USER` 권한 검증 — `FORBIDDEN` 재사용
- [x] 필수 약관 전체 동의 검증 — `TERMS_NOT_AGREED` 재사용
- [ ] [TBD] 하루 3회 제한 정책 확정 전 구현하지 않음
- [ ] [TBD] 하루 3번째 신청 허용
- [ ] [TBD] 하루 4번째 신청 거절
- [ ] 생성 후 `CANCELLED`된 신청도 해당 날짜의 신청 횟수에 포함
- [x] 동일 사용자·동일 카드 종류의 중복 신청 허용 — 별도 차단 로직 추가하지 않음
- [x] 진행 중인 같은 카드 신청이 있다는 이유만으로 차단하지 않음
- [x] Application의 `UserRepository` 직접 의존 제거 및 `UserService` 공개 메서드 사용

- [x] 상태·권한·약관 테스트를 구현 전에 추가하고 기존 구현에서 3건 실패 확인
- [x] 검증 실패 시 파일 업로드와 Application 하위 Entity 저장 미호출 검증
- [x] Application 관련 전체 테스트 통과
- [ ] 전체 테스트 통과 — 87개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음

### Task 3. PhotoValidator와 StudentCardValidator

상태: 🔵 진행중

#### PhotoValidator

- [x] `ApplicationPhotoValidator` 구현 및 적용
- [x] 얼굴사진 필수 및 빈 파일 검증
- [x] 최대 `5 * 1024 * 1024` bytes(5 MiB) 검증
- [x] 허용 확장자 `jpg`, `jpeg`, `png` 검증
- [x] 허용 MIME `image/jpeg`, `image/png` 검증
- [x] 파일 signature 검증
- [x] 실제 이미지 디코딩 가능 여부 검증
- [x] 확장자·MIME·실제 파일 형식 일치 검증
- [x] EXIF Orientation 적용 후 최소 해상도 가로 300px, 세로 400px 검증
- [x] 2MB 및 600×800~1200×1600은 권장 기준으로만 사용하고 거절 조건으로 사용하지 않음
- [x] Validator가 파일 저장이나 DB 변경을 수행하지 않음

#### StudentCardValidator

- [x] 학생증 검증은 재사용·독립 클래스 조건을 충족하지 않아 기존 Service private 메서드로 유지
- [x] 학생증의 학번 필수 검증
- [x] 학생증의 학과 필수 검증
- [x] 학생증의 학교 로고 필수 및 얼굴사진과 동일한 파일 검증(최소 해상도 제외)
- [x] 학생증의 학교 직인 필수 및 얼굴사진과 동일한 파일 검증(최소 해상도 제외)
- [x] 학생증이 아닌 신청에서 학생증 전용 입력 거절
- [x] Validator가 파일 저장이나 DB 변경을 수행하지 않음

#### 검증 순서 및 테스트

- [ ] 모든 사용자·신청·파일·학생증 검증을 신청번호 생성 전에 완료
- [ ] 모든 검증을 object storage 업로드 전에 완료
- [ ] 모든 검증을 Application 및 하위 Entity 저장 전에 완료
- [ ] 검증별 ErrorCode와 HTTP 응답 계약 확인
- [ ] 회원 없음·탈퇴·권한 불일치·약관 미동의 테스트
- [ ] 일일 2건 후 3번째 허용, 4번째 거절 테스트
- [ ] 취소 신청의 일일 횟수 포함 테스트
- [ ] 동일 카드 종류 중복 신청 허용 테스트
- [ ] 사진 크기·확장자·MIME·signature·디코딩·해상도 경계값 테스트
- [ ] 학생증 및 비학생증 필드 테스트
- [ ] 검증 실패 시 파일 업로드와 DB 저장이 호출되지 않는지 테스트
- [ ] Application 관련 테스트 전체 통과
- [ ] 전체 테스트 통과

- [x] Validator 테스트를 구현 전에 추가하고 클래스 부재로 컴파일 실패 확인
- [x] 해상도·학생증 공백 실패 시 파일 업로드와 DB 저장 미호출 통합 테스트
- [x] Application 관련 전체 테스트 통과
- [ ] 전체 테스트 통과 — 97개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음

### Task 4. prepareServerValues()와 서버 생성값 정리

상태: ⚪ 대기

- [x] 결제 정책 확정 — 상담 후 신청, 신청 이후 계좌이체, Application 생성 시 `total_price` 미사용
- [x] 단체 사진 식별 정책 확정 — 엑셀 ID와 ZIP 사진 파일명을 매칭하되 ID는 저장하지 않고 별도 사진 파일 ID도 생성하지 않음
- [x] 신청번호·초기 상태·수령인 동일 여부의 결정 주체와 시점을 문서에 확정
- [x] 확정 정책에 맞는 테스트를 구현 전에 작성하고 `isReceiverSameAsApplicant()` 부재로 실패 확인
- [x] 서버 준비값이 서로 다른 책임에 있어 `prepareServerValues()`를 도입하지 않고 기존 지역변수 유지
- [x] 새로운 Context/DTO/Plan 불필요 확인 — 추가하지 않음
- [x] 기존 Request/Response와 저장 순서 유지
- [x] Application 관련 테스트 통과
- [ ] 전체 회귀 테스트 통과 — 99개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음
- [x] TODO와 CHANGELOG 갱신
### Task 5-A. Factory 도입 전 설계 확정

상태: ✅ 완료

- [x] `CreatedApplication`, `CreatedChildren`, Context, Plan 객체를 도입하지 않음
- [x] `Application.id=IDENTITY` 유지, Root 저장 후 발급된 ID로 하위 Entity 생성
- [x] 저장 전 하위 Entity에 null FK를 넣지 않음
- [x] 파일 업로드는 Service가 담당하고 Factory에는 저장된 경로·파일 ID만 전달
- [x] Factory는 Entity 생성만 담당하고 Repository·Storage를 호출하지 않음
- [x] 기존 검증·예외·파일 업로드·저장 순서를 변경하지 않음
- [x] Factory 메서드는 Application Service와 테스트에서만 접근할 수 있도록 package-private로 제한

### Task 5-B. ApplicationFactory 구현

상태: ✅ 완료

- [x] `ApplicationFactory` 생성
- [x] `IndividualCreationContext`를 만들지 않음 — 현재 값 규모와 저장 순서에서는 불필요
- [x] `CreatedApplication` 결과 타입을 만들지 않음 — Root 선저장 구조와 맞지 않음
- [x] 별도 Context를 만들지 않음
- [ ] Request DTO에는 클라이언트 입력과 단순 파생값만 유지
- [ ] Context가 꼭 필요한 경우 검증된 입력, User/CardType 조회 결과, 신청번호 등 실제 서버 준비값만 포함 (`total_price`와 구성원별 사진 파일 ID 제외)
- [x] `createApplication()`, `createApplicant()`, `createReceiver()`, `createMember()`를 package-private로 제한
- [x] 개인 신청 Entity 생성 로직을 Factory로 이동
- [x] Factory가 Repository, object storage, 외부 API를 호출하지 않음
- [x] Entity public Builder를 추가하지 않음
- [x] `ApplicationService`는 검증·파일 준비·Factory 호출·저장 흐름만 조정
- [x] Factory를 package-private로 제한해 Application Service 패키지 밖의 부분 생성을 차단
- [x] 기존 실행 순서, 예외 발생 시점, 파일 업로드 및 저장 순서 유지
- [x] Application 관련 테스트 전체 통과
- [ ] 전체 테스트 통과 — 101개 중 기존 `UserControllerTest` 2건 실패, 신규 회귀 없음
- [x] 패키지 의존 방향 검증 — Factory는 Entity에만 의존

### Task 6-A. ApplicationNumberGenerator와 동시성

상태: ⚪ 대기

- [ ] `ApplicationNumberGenerator` 분리
- [ ] 현재 `count + 1` 방식 제거
- [ ] 신청번호 생성 규칙과 연도 전환 규칙 문서화
- [ ] DB UNIQUE 제약 확인
- [ ] DB 잠금, 번호 발급 테이블 등 동시성 보장 방식 결정
- [ ] 동시 요청에서 신청번호 중복이 발생하지 않는지 테스트
- [ ] 번호 생성 충돌 시 재시도 가능 여부와 범위 정의
- [ ] 번호 생성 실패가 중복 신청 생성으로 이어지지 않는지 확인

### Task 6-B. 파일 보상 처리와 트랜잭션

상태: ⚪ 대기

- [ ] 파일 검증과 실제 업로드 단계 분리
- [ ] 임시 object storage 경로 업로드 적용 여부 결정
- [ ] DB 저장 성공 시 파일 확정
- [ ] DB 저장 실패 시 업로드 파일 보상 삭제
- [ ] 보상 삭제 실패 로그 기록
- [ ] 고아 파일 cleanup job 연계
- [ ] Application과 Applicant/Receiver/ApplicationMember 저장 원자성 확인
- [ ] 외부 파일 작업이 DB 트랜잭션을 불필요하게 오래 점유하지 않는지 점검
- [ ] 장애 단계별 롤백·보상 처리 통합 테스트

### Task 6-C. Retry·멱등성·운영 검증

상태: ⚪ 대기

- [ ] 재시도 가능한 오류와 재시도하면 안 되는 오류 구분
- [ ] 최대 재시도 횟수와 backoff 정의
- [ ] 신청 생성 API의 멱등성 필요 여부 결정
- [ ] 재시도로 Application 또는 파일이 중복 생성되지 않는지 검증
- [ ] 장애 단계별 로그와 추적 ID 적용
- [ ] 운영 지표 및 알림 대상 정의
- [ ] 동시성·롤백·보상·재시도 운영 테스트
- [ ] Application 관련 테스트 전체 통과
- [ ] 전체 테스트 통과
