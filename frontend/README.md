# 한글과 세종 프론트엔드

외국인을 위한 한국 이름 추천과 명예한국인증·명예시민증·방문증·학생증의 신청, 조회, 발급 관리를 제공하는 React 웹 애플리케이션이다. 이 문서는 현재 `frontend/src`의 구조와 실행 방식을 설명한다. 작업 이력과 미완료 API는 [`docs/FRONTEND_API_GAPS.md`](../docs/FRONTEND_API_GAPS.md)에서 관리한다.

## 실행과 검증

요구 환경은 Node.js와 npm이다. 개발 서버는 `/api`, `/oauth2`, `/login/oauth2` 요청을 `VITE_DEV_BACKEND`(기본값 `http://localhost:8080`)로 프록시한다.

```bash
cd frontend
npm install
npm run dev
npm run build
npm run preview
```

| 스크립트 | 역할 |
|---|---|
| `npm run dev` | Vite 개발 서버 실행 |
| `npm run build` | TypeScript 검사 후 프로덕션 번들 생성 |
| `npm run preview` | 생성된 `dist` 번들 미리보기 |

별도 검증 명령:

```bash
npx tsc --noEmit
npx playwright test naming-determinism.spec.ts
# 빌드 후 격리 UI 회귀 테스트. Windows에서는 PLAYWRIGHT_CHANNEL=msedge 사용 가능
npx playwright test --config playwright.ui.config.ts
```

현재 `package.json`에는 lint와 범용 test 스크립트가 없다. Playwright 전체 E2E는 Docker 격리 스택과 테스트 데이터베이스가 필요한 별도 통합 검증이다.

## 기술 스택

- React 18, TypeScript, React Router 6
- Vite 5
- Playwright
- `manseryeok` 기반 관리자 사주 계산
- Pretendard와 은평사가독서체 웹폰트
- CSS Modules 대신 전역 클래스와 페이지별 CSS 사용

## 실제 디렉터리 구조

```text
src/
├─ App.tsx                         라우트 테이블과 lazy loading
├─ main.tsx                        Router·인증·다국어 Provider 조립
├─ pages/                          URL 단위 화면
│  ├─ ApplyPage/                   5단계 제작 신청 오케스트레이션
│  ├─ AdminPage/                   관리자 대시보드와 관리자 스타일
│  └─ *Page/                       회사·후기·행사·조회·지원·계정 화면
├─ components/
│  ├─ apply/
│  │  ├─ steps/                    신청 단계 조립 컴포넌트
│  │  └─ info/                     개인·단체·학교·수령인 정보 섹션
│  ├─ admin/
│  │  ├─ applications/             신청 상세·작명·만세력·카드 제작
│  │  └─ sections/                 신청·후기·문의·게시판·학교 템플릿
│  ├─ header/ footer/ layout/      공통 레이아웃
│  ├─ home/ gallery/ brand/        홈·카드·브랜드 표시
│  └─ ui/                          Button·Modal·Toast·입력 UI
├─ features/
│  ├─ apply/                       draft·검증·학교 검색·API 매핑
│  ├─ auth/                        서버 세션 기반 인증 상태
│  ├─ i18n/                        언어 상태·번역·서버 오류 메시지
│  └─ admin/                       관리자 기능별 상태 hook
├─ services/api.ts                 공통 HTTP 클라이언트·API 타입·래퍼
├─ config/                         회사 정보와 내비게이션
├─ data/                           정적 콘텐츠·카드·국가·도시 데이터
├─ lib/                            우편번호·사주·이름 추천·다운로드 유틸
└─ styles/                         토큰·reset·공통 폼·콘텐츠 스타일
```

## 화면과 기능

- 공개 화면: 홈, 회사 소개, 인사말, 카드 디자인, 제작 신청, 신청 조회, 모바일 카드, 공지, FAQ, 후기, 행사, 고객지원
- 계정: 이메일 인증 회원가입, 이메일 로그인, Google/Naver OAuth, 약관 동의, 계정 복구, 회원정보와 비밀번호 변경, 탈퇴
- 신청: 개인/단체 선택 → 정보 입력 → 사진/파일 → 최종 확인 → 완료 및 입금자명 저장
- 마이페이지: 신청 목록·상세·취소·사진 재업로드, 후기, 문의, 회원정보
- 관리자: 통계, 신청 목록·상세·상태 전이, 엑셀 입출력, 카드번호, 이름 추천, 만세력 확정·복원, 카드 미리보기·생성·다운로드, 학생증 템플릿, 게시판·후기·행사·문의 관리

회사 소개·후기·행사·관리자 화면과 주요 업무 API는 구현되어 있다. 마케팅 콘텐츠 중 회사 정보·파트너·상품·정책 문안과 행사 PROGRAM 카드는 의도적으로 정적 데이터로 관리한다.

## 인증 구조

`AuthProvider`는 HttpOnly access/refresh 쿠키와 `GET /api/users/me`를 인증의 기준으로 사용한다. 앱 시작 시 서버에서 세션을 확인하고 `loading`, `authenticated`, `unauthenticated`, `error`를 구분한다. API 권한은 항상 백엔드가 최종 판단한다.

현재 백엔드 `/api/users/me` 응답에는 역할이 없어서 로그인 응답의 role을 `auth-role` UI 힌트로만 저장한다. 이 값은 관리자 API 인가 근거가 아니다. 이 계약 차이는 갭 문서의 현재 미완료 항목으로 관리한다.

## API 호출 구조

모든 프론트 API 타입과 래퍼는 `src/services/api.ts`에 있다.

- 기본 URL은 `VITE_API_BASE_URL`; 비어 있으면 same-origin이다.
- 요청마다 `credentials: include`와 현재 언어의 `Accept-Language`를 전송한다.
- JSON 요청에만 `Content-Type: application/json`을 자동 적용한다.
- 401이면 refresh를 한 번 호출한 뒤 원 요청을 최대 한 번 재시도한다.
- `ApiEnvelope<T>`의 `errorCode`, `errorMessage`, `errors[]`를 `ApiError`로 보존한다.
- 파일 응답은 동일한 인증·refresh 규칙을 적용하는 별도 binary 경로를 사용한다.
- 화면에서는 기존 `api.*` 래퍼를 재사용하고 endpoint 문자열을 중복 작성하지 않는다.

세부 도메인 계약은 `docs/api/`와 `docs/specs/*/api.md`가 원본이며, 프론트 사용 방식은 [`docs/FRONTEND_API_INTEGRATION_SPEC.md`](../docs/FRONTEND_API_INTEGRATION_SPEC.md)를 따른다.

## 다국어 구조

`LanguageProvider`가 `ko | en` 상태를 제공하고 UI 선택은 `site-language`에 저장한다. 컴포넌트는 `useLanguage().t()`를 사용하며, React 밖의 오류 처리에는 `getLanguage()`와 `translateText()`를 사용한다. API는 `Accept-Language`를 보내 서버가 게시판·후기·행사 등 런타임 콘텐츠를 번역할 수 있게 한다.

## 신청 플로우 구조

`ApplyPage`가 단계 이동과 최종 multipart 요청을 담당한다. `useApplicationDraft`는 탭 생명주기 동안 `sessionStorage`에 직렬화 가능한 입력만 저장하며 파일 객체는 복원하지 않는다.

`StepInfo`는 발급 방식과 다음 단계 이동만 조정한다. 입력 책임은 `IndividualFields`, `OrganizationFields`, `SchoolFields`, `RecipientSection`으로 분리했다. 필수값·형식 검증은 `useInfoValidation`, 학교 API 검색은 `useSchoolSearch`가 담당한다.

## 관리자 신청 구조

`ApplicationsSection`은 목록·탭·선택·페이지 이동을 담당한다. 펼친 행 아래의 기능은 다음 경계로 분리되어 있다.

- `ApplicationDetail`: 상세 조회, 멤버 조회, 상태 전이, 단체 엑셀·ZIP 작업
- `NamingCard`: 확정 만세력 표시와 결정적 이름 추천·저장
- `ManseryeokPanel`: 출생지역 검색, 시간 해석, 결과 확정
- `CardProductionPanel`: 디자인 조회, 미리보기, 생성, 다운로드
- `CardNumberField`: 멤버 카드번호 저장
- `useManseryeokResults`: 신청 단위 활성 결과 일괄 복원

이름 추천은 `lib/namingRecommendations.ts`가 담당한다. 같은 확정 만세력과 이름 사전에는 항상 같은 상위 5개를 반환한다.

## 스타일과 디자인 토큰

UI 테마 색상은 `src/styles/tokens.css`의 의미 기반 토큰을 사용한다. 컴포넌트 CSS는 primary, danger, warning, success, surface, border, text와 관리자 상태 토큰을 참조한다. 새 UI 색상은 실제 의미를 먼저 정한 뒤 토큰에 추가한다.

다음 값은 토큰 통합 대상에서 제외한다.

- Google/Naver/SNS 로고와 Instagram SVG 등 브랜드 고유 색상
- 오행 아이콘처럼 도메인 의미가 있는 색상은 전용 semantic token
- 홈 장식의 광원·그림자처럼 이미지 표현에 가까운 시각 효과

## 개발 규칙

- 페이지는 화면 조립과 라우트 단위 상태를 맡고 재사용 가능한 업무 UI는 `components`로 둔다.
- 기능 상태와 긴 로직은 책임이 분명할 때 `features`의 hook으로 추출한다.
- 서버 호출은 `services/api.ts`에만 정의하고 화면에서 fetch를 직접 중복하지 않는다.
- 인증 사용자와 업무 데이터는 localStorage에 저장하지 않는다. 언어와 비권한 UI 힌트만 허용한다.
- API enum 변환은 `features/*/mappers.ts`처럼 경계에서 처리한다.
- 이름 추천에 임의 fallback이나 무작위 처리를 추가하지 않는다.
- UI 변경 후 최소 `npx tsc --noEmit`과 `npm run build`를 실행한다. 해당 기능 테스트가 있으면 함께 실행한다.
