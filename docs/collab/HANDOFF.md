# HANDOFF — 현재 작업 상태

- 마지막 갱신: 2026-08-20
- 작성자: Claude
- 작성 브랜치: main

## 지금 어디까지 됐는가

- **✅ 회원정보 address 수정 정책 재확정 원복 + 조회 응답에서 role 제거 + 마이페이지 스펙 확정(2026-08-20, 커밋 완료)**: 이날 세션 초반에 "이름·전화번호만" → "address도 수정 가능"으로 뒤집었던 정책을, 사용자가 다시 "수정에는 이름과 전화번호만 가능해야 한다"고 확인해 **원래대로(이름·전화번호만) 최종 원복**했다. `UserUpdateRequest`에서 `address` 필드 제거, `User.updateProfile` 2-인자로 복원, `UserService.updateMe` address 검증 삭제, `UserControllerTest`도 "address 무시" 검증으로 원복, `docs/api/user.md` API 5 원복. **교훈**: 같은 날 두 번 바뀐 정책이라 다음 세션에서 다시 헷갈릴 수 있음 — `PATCH /api/users/me`는 **이름·전화번호만** 수정 가능이 최종 확정 상태다(주소는 조회 응답엔 계속 포함되지만 수정 불가).
  - **`UserMeResponse`에서 `role` 필드 완전 제거**: 사용자가 마이페이지 "내 정보"에 뜨던 "회원 유형"(일반회원/관리자) 표시를 등급제처럼 잘못 보인다고 지적 — 단순 화면 숨김이 아니라 `GET`/`PATCH /api/users/me` 응답 DTO(`UserMeResponse`) 자체에서 `role`을 뺐다. 관련 테스트 3곳(`UserControllerTest`/`AuthControllerSignupTest`/`UserApplicationFlowTest`)도 `.doesNotExist()`로 수정. **role은 애초에 `UserUpdateRequest`에 없어서 관리자든 일반유저든 이 API로 수정 불가**(변경 없음, 원래도 그랬음) — 이번 변경은 조회 응답에서 노출만 없앤 것.
  - **⚠️ 프론트 영향(대응은 프론트 몫, `FRONTEND_API_GAPS.md` §1.9-a에 기록만 함)**: `AuthContext.tsx`의 `refreshProfile()`이 이 응답의 `role`을 읽어 `isAdmin`을 세팅하고 있었는데(`Header.tsx` 관리자 메뉴, `InquiryDetailPage.tsx` 열람권한 체크가 의존), 이제 그 필드가 없어 이 로직은 더 이상 동작하지 않는다. 지금은 실 서버 인가와 무관한 프론트 전용 데모 값이라 당장 보안 문제는 아님.
  - **마이페이지 "내 정보" 표시 스펙 확정(프론트 작업, `FRONTEND_API_GAPS.md` §1.9-a에 기록만 함)**: 조회 시 이름·전화번호·이메일 3개만 노출("회원 유형"은 응답 자체에 없어 표시 불가), 수정 가능 필드는 이름·전화번호뿐. 현재 `MyPage.tsx`는 전화번호 미표시 + 회원유형 표시 중이라 둘 다 프론트에서 손봐야 함.
  - **별건 — 마이페이지 "제작 내역" 빈 목록 문제 재확인(코드 근거 확보, 아직 미수정)**: 사용자가 실제로 신청을 제출해도 "제작 신청 내역이 없습니다"만 뜨는 걸 보고했다. 원인은 `MyPage.tsx`가 실 API(`GET /api/my/applications`, 이미 `main`에 구현 완료·`b5f6140`)를 아예 호출하지 않고 `data/adminMock.ts` localStorage(`applicantEmail === user.email` 필터)만 읽기 때문 — 서버 저장 여부와 무관하게 표시된다. **백엔드 작업은 없음(순수 프론트 스코프)**. `services/api.ts`에 `listMyApplications` 함수가 아예 없는 것, 상태 라벨(`adminStatusLabels`)이 옛 mock enum 기준이라 실 enum과 3개만 겹치는 것, 날짜 필드가 `submittedAt`(날짜만)→`createdAt`(`LocalDateTime`)로 바뀌어 포맷이 깨지는 것까지 `FRONTEND_API_GAPS.md` §1.2에 상세 기록해뒀다.
- **✅ 배포 준비 + 버그 수정 다발(2026-08-20)**: 사용자가 EC2 배포를 진행하면서 발견된 문제들을 그때그때 고쳤다. 전부 `main`에 커밋·푸시 완료.
  - **단체신청 엑셀 템플릿 유효성검사 수식 버그 2건**(`artifact-work/bulk-excel-templates-20260818/build_templates.py`, git 미추적 로컬 스크립트): `type="custom"`(영문명/이메일/전화번호/학번/학과)과 `type="list"`(국적/성별, 이름정의 참조) 데이터 유효성검사의 `formula1`에 불필요한 선행 `=`가 있어 **한컴오피스 한셀에서 정상 입력값도 전부 거부**되던 버그. Excel은 관대하게 처리하지만 한셀은 안 그럼 — OOXML 스펙상 원래 `=` 없이 써야 하는 게 맞음. 3개 템플릿(`outputs/bulk-excel-templates-20260818/*.xlsx`) 재생성해 반영(`20ee58e`, `c0e61e4`).
  - **고등학교 단체신청 학번·학과 정책 왕복 수정**: 처음엔 "단체는 schoolType 무관하게 학번·학과 필수"인 기존 코드에 맞춰 고등학교 템플릿에 학번·학과 열 추가(`5d8af19`)했으나, 사용자가 프론트 코드(`StepInfo.tsx`의 "고등학교 선택 시 대학교 전용 항목은 비운다" 주석)를 근거로 "개인 신청처럼 단체도 고등학교면 학번·학과를 받으면 안 된다"고 정정 — `BulkExcelParser`가 `schoolType`을 아예 몰랐던 게 진짜 버그였음이 드러남. `BulkExcelParser.parse(zipFile, isStudent, schoolType)`로 시그니처 확장, `UNIVERSITY`만 필수·`HIGH_SCHOOL`이면 있으면 거절하도록 개인 신청과 통일(`6efd2b8`), 템플릿의 학번·학과 열도 다시 제거(`ba80a79`). **교훈**: 코드가 이미 있다고 정책이 맞다고 가정하지 말 것 — 프론트 실제 동작이 더 신뢰할 수 있는 정책 근거였음.
  - **회원정보 `address` 수정 정책 재정정**: 2026-08-08에 "이름·전화번호만 수정 가능, address 제외"로 확정했던 걸 사용자 지시로 뒤집음 — `PATCH /api/users/me`가 이제 `address`도 받는다(`UserUpdateRequest`/`User.updateProfile`/`UserService.updateMe` 전부 반영, `10a0441`). `docs/api/user.md` API 5, `docs/FRONTEND_API_GAPS.md` §1.9(a) 갱신 완료 — 프론트가 주소 입력란을 다시 만들어야 하는 항목으로 전환.
  - **`User.createLocalUser`가 phone을 생성 시점에 받도록 리팩터링**: `registerLocalUser()`가 생성 직후 `updateProfile(null, phone, null)`로 채우던 걸, `SignupRequest.phone`이 이미 `@NotBlank`라는 근거로 팩토리 4번째 파라미터로 승격(`1983021`). 기존 3인자 오버로드는 하위호환 없이 제거 — 호출부 12곳(운영 1+테스트 11) 전부 4인자로 이관.
  - **배포 인프라**: `docker-compose.yml`에 Postgres 추가(이전엔 H2 인메모리로 떠서 재시작마다 데이터 소실, `8cdafb2`), `MAIL_HOST`/`MAIL_PORT`/`MAIL_FROM`이 컨테이너에 전달 안 되던 버그 수정(`.env`에 값 넣어도 무시되고 있었음, `30a1a52`), 루트 `.gitignore`에 `.env` 누락 발견해 추가(실제 AWS 시크릿이 든 `backend/.env`가 커밋될 뻔함 — git history엔 다행히 없었음 확인 완료, `d5dc282`), EC2 배포 시크릿 3단 분리(Dockerfile 무시크릿 → compose는 변수명만 → EC2 `.env`는 실값, git 미추적) 절차를 `DOCKER.md`에 문서화(`12f28bc`).
  - **실제 배포 테스트(로컬 Docker Desktop으로 재현, EC2 아님)**: SMTP(Gmail, 앱 비밀번호)·S3(업로드/다운로드/삭제)·구글/네이버 OAuth 리다이렉트 구성을 전부 직접 호출해서 정상 동작 확인함. **EC2 자체의 `.env`는 로컬 PC의 `.env`와 별개 파일**이라는 걸 사용자가 뒤늦게 발견 — EC2 쪽엔 Google/Naver 자격증명이 비어있어서 `docker-compose.yml`의 로컬 placeholder(`docker-local-google-client` 등)로 폴백되고 있었고, 이게 `401 invalid_client`의 원인이었음. 사용자가 EC2 쪽 별도 세션에서 채우는 걸로 진행 중 — **다음에 이어서 볼 때 EC2 `.env`가 다 채워졌는지, `docker compose up -d --no-deps backend`로 반영됐는지부터 확인할 것.**
  - **프론트-백엔드 갭 재확인(2026-08-20)**: `docs/FRONTEND_API_GAPS.md`를 실제 프론트 코드(`ApplyPage.tsx`)와 재대조하다가 **`schoolName`(2026-08-19 신규 백엔드 필수 필드)을 프론트가 요청에 안 보내서 학생증 신청이 전부 400으로 깨져 있는 회귀**를 발견(`4a6db96`). §6에 우선순위 표로 정리(`e496f6a`) — 0순위로 등록돼 있음. `updateMe`의 `address` 죽은 파라미터, `PATCH.../cancel` 미바인딩 등도 같이 점검했으나 이 둘은 실제 문제 없음(주소는 이번에 반영 완료, cancel은 원래 §1.5에 미착수로 기록돼 있던 것과 일치).
- **✅ 학생증 신청에 학교명(schoolName) 필드 추가 완료(2026-08-19, SCHOOLNAME-1)**: `Application` 레벨 단일 필드(개인·단체 공통), `UNIVERSITY`/`HIGH_SCHOOL` 둘 다 필수, 트림 후 5~20자·한글/영문/숫자/공백만 허용. 커밋 `575f6c0`/`6653fd2`. (위 2026-08-20 항목에서 이 필드를 프론트가 아직 안 보내는 회귀를 발견함 — 별개 사안이니 혼동 주의.)
- **✅ 회원탈퇴 정책 변경 완료(2026-08-19)**: 소프트 삭제(7일 유예+익명화) 폐지 → 즉시 하드 삭제. `docs/collab/user.md`가 source of truth(§19 체크리스트). WITHDRAW-1~4 전부 완료(`a3bc798`/`b83bd65`/`f956120`/`861b92e`/`7e131f5`). 상세는 `docs/api/user.md` API 4, `arch.md` §4.1/§4.7/§11.
- **✅ Inquiry(1:1 문의) 도메인 신규 구현 완료(2026-08-19)**: 6개 API 전부 구현·테스트·커밋 완료(`1abab25`~`a9abff7`). `docs/specs/inquiry/requirements.md`가 source of truth.
- **✅ 일반 이메일 인증·로그인·계정관리(AUTH-1~6·PW-1·MAIL-1·SIGNUP-1/2·RATE-1)** — 이전 세션에 완료. `POST /api/auth/login`·`/email/check`, `PATCH /api/users/me/password` 전부 구현돼 있음.
- **⚠️ 로컬 테스트 환경 참고**: Docker 컨테이너 `honor-citizen-redis-test`(호스트 포트 **6400**). `REDIS_PORT=6400 ./gradlew.bat test`로 실행. 별도로 `docker compose`(저장소 루트, Postgres+Redis+backend+frontend 통합 실행)도 오늘 세션에서 실제 배포 테스트용으로 씀 — 둘은 다른 용도(전자는 단위테스트용 Redis만, 후자는 전체 스택).
- **⚠️ 발견된 기존 결함(고치지 않고 기록만 함, 여러 세션째 유지)**: `UserApplicationFlowTest.fullUserApplicationFlow()`가 403으로 실패 — 약관동의 단계를 안 거치는 기존 결함(회귀 아님).
- 그 외 도메인(Board/Event/Review 등)은 이전 세션들에서 전부 완료·커밋된 상태.

## 다음에 할 일

- **🔴 최우선(회귀 수정, 프론트 담당)**: `ApplyPage.tsx` `submit()`에 `schoolName` 필드 추가 — 안 하면 학생증 신청 전부 400. `docs/FRONTEND_API_GAPS.md` §1.9-b 참고.
- **🔴 마이페이지 "제작 내역" 연동(프론트 담당)**: `MyPage.tsx`가 실 API를 안 부르고 있어 실제 신청 후에도 빈 목록만 뜬다. 백엔드는 이미 준비됨(`GET /api/my/applications`) — `FRONTEND_API_GAPS.md` §1.2에 상태 라벨/날짜 포맷 이슈까지 포함해 상세 기록해둠.
- **EC2 배포 마무리**: EC2의 `.env`에 Google/Naver 자격증명(및 나머지 값 전체)이 다 채워졌는지 확인 → `docker compose up -d --no-deps backend`로 반영 → 브라우저로 실제 OAuth 로그인 완료까지 테스트. HTTPS(certbot)는 아직 미착수 — DNS·탄력적 IP는 사용자가 이미 설정 완료.
- **프론트 작업 우선순위 전체**는 `docs/FRONTEND_API_GAPS.md` §6 표 참고(schoolName·마이페이지 신청목록 외에 회원가입 인증코드 UI, 로그인 연동, 문의 연동, 마이페이지 "내 정보" 표시 정리(회원유형 제거·전화번호 추가) 등 백엔드는 준비됐고 프론트만 남은 항목들).
- **AWS 자격증명 보안 권장**: 지금 쓰고 있는 AWS 키가 IAM 사용자가 아니라 계정 루트 키로 확인됨(`arn:aws:iam::...:root`) — S3 버킷 하나에만 권한 준 전용 IAM 사용자로 교체 권장(급하지 않음, 사용자에게 이미 안내함).
- **거래·상담 데이터 파기 스케줄러**: Inquiry 6개월, 결제·거래 이력 법정 보존기간 — 담당자 미정.
- **개인정보처리방침 문안 확인(법무 대상)**: `docs/collab/user.md` §17.2/§17.3 — 코드 작업 아님.
- **UserApplicationFlowTest 403 수정**: 담당자 미정.
- 그 외 미착수 항목은 `TODO.md` 진행 보드 및 `docs/BACKEND_API_GAPS.md` 참고.

## ❓ 확인 필요

- 없음 — 이번 세션 질문들(고등학교 학번/학과 정책, address 정책, AWS 키 이름, SMTP 계정)은 사용자가 전부 확정해줬다.

## 참고

- **EC2/docker 배포 절차**: `DOCKER.md`의 "EC2 배포 — 시크릿 3단 분리 원칙" 절 — Dockerfile(무시크릿) → docker-compose.yml(변수명만) → EC2 `.env`(실값, git 미추적) 순서, 로컬 `.env`와 EC2 `.env`는 별개 파일이라는 점 꼭 기억할 것(오늘 실수 사례).
- **엑셀 템플릿 재생성 방법**: `artifact-work/bulk-excel-templates-20260818/build_templates.py`(Python, openpyxl) — `python3 build_templates.py`로 `outputs/bulk-excel-templates-20260818/*.xlsx` 재생성. 이 스크립트 자체는 git 미추적(Codex도 안 커밋했던 관례 유지), 산출물 xlsx만 커밋.
- 회원탈퇴 정책 스코프 분석 방법, 프론트-백엔드 갭 재점검 방법(문서 서술을 안 믿고 실제 코드 대조) 등 재사용 가능한 방법론은 이전 HANDOFF 버전 참고(git log로 조회 가능).
- 관련 문서: `docs/collab/user.md`(회원탈퇴 source of truth), `docs/specs/application/requirements.md` §5-0/§5-2(schoolName), `docs/api/user.md`, `docs/FRONTEND_API_GAPS.md`, `docs/BACKEND_API_GAPS.md`, `DOCKER.md`, `docs/collab/TODO.md`, `docs/collab/CHANGELOG.md`, `docs/collab/RULES.md` §8.
