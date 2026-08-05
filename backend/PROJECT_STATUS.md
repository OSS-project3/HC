# 프로젝트 현황

기준일: 2026-08-05

기준 브랜치: `main`

저장소 구조: 프론트엔드 `frontend/`, 백엔드 `backend/honor-citizen/`

이 문서는 현재 소스 코드를 기준으로 프로젝트의 구현 범위와 남은 작업을 요약한다. API의 상세 요청·응답 및 프론트 요구사항은 `API_ANALYSIS.md`, `FRONTEND_API_REQUIREMENTS.md`를 함께 참고한다.

## 1. 현재 요약

- React 프론트엔드와 Spring Boot 백엔드를 하나의 저장소로 통합했다.
- 프론트엔드는 백엔드에 실제 구현된 API만 호출하도록 공통 API 모듈을 갖추고 있다.
- 백엔드에 없는 기능은 임의의 API를 호출하지 않으며, 기존 목데이터와 브라우저 저장소 동작을 유지한다.
- Google/Naver 소셜 로그인 진입점과 쿠키 기반 토큰 갱신·로그아웃을 연결했다.
- 일반 이메일 회원가입·로그인 UI는 있으나 이를 처리할 백엔드 API는 아직 없다.
- 관리자 로그인 상태에서 후기, 공지, FAQ, 이벤트의 작성·수정·삭제 UI가 동작한다. 현재 데이터는 `localStorage`에 저장된다.
- Docker Compose로 프론트엔드, 백엔드, Redis를 한 번에 빌드하고 실행할 수 있다.

## 2. 기술 구성

| 영역 | 구성 |
|---|---|
| 프론트엔드 | React 18, TypeScript, Vite, React Router |
| 백엔드 | Java 21, Spring Boot 4.0.6, Spring Security, OAuth2 Client, JPA |
| 인증 | Google/Naver OAuth2, JWT HttpOnly 쿠키, Redis 세션 |
| 데이터베이스 | 로컬 H2, 운영 PostgreSQL 구성 가능 |
| 파일 | AWS S3 연동 코드 |
| 실행 환경 | Docker Compose, Nginx, Redis 7 |

## 3. 백엔드 실제 구현 API

Controller 매핑을 기준으로 명시적인 REST API는 총 11개다. 문서에만 있고 Controller가 없는 API는 구현된 것으로 계산하지 않는다.

### 인증 및 사용자

| Method | Path | 용도 |
|---|---|---|
| POST | `/api/auth/terms` | 약관 동의 저장 |
| POST | `/api/auth/refresh` | 쿠키 기반 토큰 갱신 |
| POST | `/api/auth/logout` | 로그아웃 및 토큰 폐기 |
| GET | `/api/users/me` | 내 프로필 조회 |
| PATCH | `/api/users/me` | 내 프로필 수정 |
| POST | `/api/users/me/withdraw` | 회원 탈퇴 |

Spring Security가 제공하는 OAuth2 진입점도 사용한다.

- `/oauth2/authorization/google`
- `/oauth2/authorization/naver`

### 신청

| Method | Path | 용도 |
|---|---|---|
| POST | `/api/applications` | 개인 신청 생성 |
| POST | `/api/applications/bulk` | 단체 신청 생성 |
| POST | `/api/applications/lookup` | 신청번호 또는 카드번호 조회 |
| PATCH | `/api/applications/{applicationId}/photo` | 사진·제출 파일 재업로드 |
| GET | `/api/applications/{applicationId}/cards/download` | 발급 카드 다운로드 정보 조회 |

## 4. 프론트엔드 연동 상태

`frontend/src/services/api.ts`에서 `credentials: "include"` 방식의 공통 요청, 401 응답 시 토큰 갱신 후 1회 재요청, 공통 응답 형식 처리를 제공한다.

| 기능 | 현재 상태 |
|---|---|
| Google/Naver 로그인 | 백엔드 OAuth2 진입점 연결 완료 |
| 로그인 사용자 프로필 | `/api/users/me` 조회 연결 완료 |
| 프로필 수정·탈퇴 | API 함수 준비 완료 |
| 토큰 갱신·로그아웃 | API 연결 완료 |
| 개인·단체 신청 | 실제 multipart API 호출 코드 준비 완료 |
| 신청 조회 | 실제 API를 우선 호출하고 실패 시 기존 목데이터 유지 |
| 사진 재업로드 | API 함수 준비 완료, 전용 화면 흐름은 추가 작업 필요 |
| 카드 다운로드 | API 함수 준비 완료, 화면 연결은 추가 작업 필요 |

백엔드 API가 없는 일반 회원가입·이메일 로그인·콘텐츠 관리 기능은 호출을 생략하고 현재 프론트 동작을 유지한다.

## 5. 프론트엔드 기능 상태

### 인증 화면

- 이메일, 비밀번호, 전화번호 등 필수 입력 항목에 `*`를 표시한다.
- 이메일 형식, 전화번호 숫자 형식, 비밀번호 길이·조합, 비밀번호 확인 일치 여부를 프론트에서 검증한다.
- 잘못된 입력에는 항목별 오류 메시지를 표시한다.
- 비밀번호 확인은 비밀번호 입력 바로 아래에 배치했다.
- Google/Naver 로고와 브랜드 스타일을 적용한 소셜 로그인 버튼을 제공한다.
- 브라우저의 "정보가 유출된 것 같다"는 경고는 브라우저 비밀번호 관리자가 유출 이력이 있는 비밀번호나 데모 계정을 감지할 때 표시할 수 있으며, 서버에서 프론트 입력값을 유출했다는 의미는 아니다.

### 관리자 및 콘텐츠

관리자 데모 로그인 상태에서 다음 항목의 작성·수정·삭제 UI가 제공된다.

- 후기
- 공지사항
- FAQ
- 이벤트
- 1:1 문의 상태 관리
- 신청 목록 및 상태 관리

현재 이 데이터는 브라우저별 `localStorage`에 저장되므로 다른 기기나 관리자와 공유되지 않는다. 운영 전 해당 백엔드 API가 반드시 필요하다.

## 6. 현재 목데이터·브라우저 저장소 사용 영역

| 저장 키/소스 | 용도 | 운영 전 처리 |
|---|---|---|
| `auth-user` | 데모 사용자·관리자 로그인 상태 | 실제 인증/권한 API로 교체 |
| `admin-applications` | 신청·관리자 처리 목데이터 | 관리자 신청 API로 교체 |
| `customer-inquiries` | 1:1 문의 | 문의 API로 교체 |
| `review-posts` | 후기 | 후기 API로 교체 |
| `managed-content:notices` | 공지사항 | 공지 API로 교체 |
| `managed-content:faqs` | FAQ | FAQ API로 교체 |
| `managed-content:events` | 이벤트 | 이벤트 API로 교체 |
| `application-draft` | 작성 중 신청 임시 저장 | `sessionStorage` 유지 가능 |
| `last-application-lookup` | 화면 간 최근 조회 결과 전달 | `sessionStorage` 유지 가능 |
| `site-language` | 언어 선택 | 로컬 UI 설정으로 유지 가능 |

사용자 요구에 따라 목데이터는 현재 삭제하지 않고 보류한다. 백엔드 API가 구현된 기능부터 순차적으로 교체한 뒤 운영 전 제거한다.

## 7. 백엔드에 추가로 필요한 핵심 API

### P0: 운영 필수

- 이메일 회원가입, 로그인, 이메일 중복 확인
- 아이디/비밀번호 찾기 및 비밀번호 재설정
- 내 신청 목록·상세
- 카드 종류·디자인 조회
- 관리자 신청 목록·상세·상태 변경·통계
- 1:1 문의 등록, 사용자 조회, 관리자 답변

### P1: 콘텐츠 운영

- 후기 목록·상세·작성·수정·삭제
- 공지사항 공개 조회 및 관리자 CRUD
- FAQ 공개 조회 및 관리자 CRUD·정렬
- 이벤트 공개 조회 및 관리자 CRUD
- 콘텐츠 첨부파일 업로드·삭제

### P2: 확장 기능

- 결제 정보, 가상계좌, 입금 확인, 웹훅
- 배송지, 송장, 배송 상태
- 사이트 회사 정보·정책·파트너·상품·SNS 링크 CMS
- 여러 기기에서 신청 초안을 이어 쓰기 위한 draft API

구체적인 권장 경로와 필드는 `FRONTEND_API_REQUIREMENTS.md`에 정리되어 있다. 이는 요구사항 문서이며 현재 구현된 API 목록과 구분해야 한다.

## 8. Docker 실행 상태

루트에서 다음 명령으로 전체 서비스를 실행한다.

```powershell
docker compose up -d --build
```

| 서비스 | 주소/역할 |
|---|---|
| frontend | `http://localhost:3000` |
| backend | `http://localhost:8080` |
| redis | Compose 내부 `redis:6379` |

프론트 Nginx는 `/api`, `/oauth2`, `/login/oauth2` 요청을 백엔드로 프록시한다. Gradle Wrapper 다운로드 과정의 인증서 오류를 피하기 위해 백엔드 Docker 빌드는 Gradle JDK 21 이미지를 사용하도록 수정했으며, 프론트 빌드와 컨테이너 기동 및 HTTP 200 응답을 확인했다.

실제 소셜 로그인과 S3 기능은 유효한 환경변수 설정 후 별도 검증해야 한다. 저장소에는 실제 비밀값을 커밋하지 않고 `.env.example`만 유지한다.

## 9. 검증 상태와 주의점

- 프론트엔드 `npm run build` 성공
- Docker Compose 이미지 빌드 및 서비스 기동 성공
- 프론트엔드 HTTP 200 응답 확인
- 실제 Google/Naver OAuth 로그인은 운영 Client ID, Secret, Redirect URI 설정 필요
- S3 업로드는 실제 AWS 자격증명 및 버킷 설정 필요
- 이메일 로그인과 회원가입은 백엔드 API 미구현으로 현재 프론트 데모 흐름만 동작
- 관리자 메뉴 노출만으로 권한을 보장할 수 없으므로 운영 API에는 서버 측 `ADMIN` 권한 검증이 필수
- 목데이터 fallback은 개발 편의를 위한 것으로 운영 빌드 전 제거해야 함

## 10. 다음 권장 작업

1. 이메일 회원가입·로그인 API를 구현하고 데모 인증을 제거한다.
2. 카드 종류·디자인 조회 계약을 확정하고 신청 화면의 문자열 ID를 서버 ID로 교체한다.
3. 내 신청 목록·상세 및 관리자 신청 관리 API를 구현한다.
4. 문의, 후기, 공지, FAQ, 이벤트 API를 구현해 `localStorage`를 제거한다.
5. 실제 OAuth·S3 환경에서 통합 테스트를 수행한다.
6. 프론트/백엔드 자동 테스트와 CI 빌드를 추가한다.
