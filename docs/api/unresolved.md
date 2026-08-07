## 미결정 사항 (전체 종합)

| 항목 | 내용 | 관련 위치 |
|---|---|---|
| **카드번호 채번 로직** | `ROK-XXXXX-XXXX` 형식은 확정, 순차 발급 vs 무작위 생성은 미정 | Admin API 6 |
| 게시판(Review/Post) 필드 | 프론트 요구사항 나오기 전까지 설계 보류 | 게시판 도메인 |
| `Receiver.country` | 해외 배송 지원 여부 미정 — 지원 안 하면 컬럼 제거 검토 | `.md` 2.3절 |
| `CardFieldDefinition.font_color` 이후 필드 | 원본 자료 자체가 없음(잘림) | `.md` 4.3절 |
| refresh 토큰 rotation용 세션 저장소 | DB 테이블로 만들지 Redis로 갈지 — 구현 단계 결정 가능 | User 도메인 |
| `MOBILE_AND_PHYSICAL` 배송 흐름(SHIPPING/DELIVERED) | 이번 Admin 설계에서 스코프 아웃, 추후 별도 설계 | Admin 도메인 |
| **`CardDesign` 배정 시점** (2026-07-31 신규) | 관리자가 신청 접수 직후/사진검토 통과 후/작명 단계 중 언제 배정하는지 — 정해져야 Admin API로 설계 가능 | Admin 도메인 |
| **신청조회(lookup) 인증 채널 조합** (2026-07-31 신규) | 전화번호/이메일 인증을 "둘 다 필수"로 할지 "하나만 있어도" 되는지 | Application API 3 |
| 학번/학과 형식 제약 (2026-07-31 신규) | 원본 요구사항에 글자수 등 세부 스펙 없음 | Application API 1/2 |
| 학생증 디자인 시안 (2026-07-31 신규) | 아직 미도착 — 받으면 CardDesign 시드 데이터 추가 | 카드 도메인 |
| 단체 신청 엑셀 파싱 실패율 룰 (2026-07-31 신규) | 옛 백엔드의 "30% 룰"을 새 설계에도 적용할지 미정 | Application API 2 |
| 신청 내용 수정 API 필요 여부 (2026-07-31 신규) | 현재는 반려 시 사진 재업로드만 가능, 그 외 수정 경로 없음 | Application 도메인 |

---

## OAuth 로컬 Vite 연동 방식

### 현재 상태

- Vite 개발 서버는 기본적으로 `http://localhost:5173`에서 실행한다.
- `frontend/vite.config.ts`에는 proxy 설정이 없다.
- 프론트 API 클라이언트는 `VITE_API_BASE_URL`이 없으면 `/api`, `/oauth2`를 현재 origin으로 호출한다.
- 백엔드에는 별도의 CORS 허용 설정이 없다.
- 과거 실OAuth 로컬 검증은 백엔드 OAuth 진입점(`localhost:8080`)과 `application-local.properties`의 프론트 URL override를 사용했지만, 현재 프론트의 모든 API 요청을 연결하는 표준 개발 구성을 확정한 것은 아니다.
- proxy 방식을 선택한다면 대상 경로는 실제 프론트 호출과 Spring Security 경로에 따라 `/api`, `/oauth2`, `/login/oauth2` 세 가지다. 경로 자체는 미결정 사항이 아니다.

### 결정이 필요한 이유

현재 상태에서 `npm run dev`만 실행하면 상대 경로 API 요청이 Vite 서버로 전달되어 백엔드와 연결되지 않는다. 개발자마다 임의의 CORS 또는 환경변수 설정을 사용하면 OAuth 쿠키와 Redirect URI 동작이 달라질 수 있다.

### 선택 가능한 대안

- A안: Vite에서 `/api`, `/oauth2`, `/login/oauth2`를 `http://localhost:8080`으로 proxy하여 Docker와 동일한 same-origin 방식으로 개발한다.
- B안: `VITE_API_BASE_URL=http://localhost:8080`을 사용하고 백엔드에 credential 허용 CORS를 명시적으로 구성한다.

### 결정 후 영향

`frontend/vite.config.ts`, 프론트 로컬 환경변수 문서, 백엔드 CORS 설정 여부, 로컬 OAuth 진입 URL과 쿠키 검증 방식이 변경된다.

## 환경별 OAuth 공개 URL과 Redirect URI

### 현재 상태

- Spring OAuth 콜백 경로는 `/login/oauth2/code/{registrationId}`로 구현되어 있다.
- local 실검증에서는 백엔드 직접 접근 방식의 `http://localhost:8080/login/oauth2/code/{provider}`를 사용했다.
- Docker는 프론트 `http://localhost:3000`에서 `/oauth2`와 `/login/oauth2`를 Nginx로 proxy하지만, 제공자 콘솔에 등록할 Docker Redirect URI와 reverse proxy 기준 `{baseUrl}` 생성 정책은 문서로 확정되지 않았다.
- `app.frontend-url`은 기본값이 `http://localhost:3000`이고 local 실검증에서는 `http://localhost:5173`으로 override했다.
- 운영 도메인, 운영 `app.frontend-url`, 운영 Google/Naver Redirect URI는 정해지지 않았다.

### 결정이 필요한 이유

OAuth 제공자에 등록된 Redirect URI와 애플리케이션이 생성한 콜백 URI가 정확히 일치해야 한다. reverse proxy가 외부 scheme, host, port를 백엔드에 정확히 전달하지 않으면 Docker 또는 운영 환경에서 로그인 콜백이 실패한다.

### 선택 가능한 대안

- A안: 모든 환경에서 프론트와 백엔드를 same-origin으로 노출하고 `https://{서비스 도메인}/login/oauth2/code/{provider}`를 표준 Redirect URI로 사용한다. local만 `http://localhost:5173` Vite proxy, Docker는 `http://localhost:3000` Nginx proxy를 사용한다.
- B안: 프론트와 백엔드를 별도 origin으로 노출하고 각 환경의 백엔드 URL(`https://api.{서비스 도메인}`)을 Redirect URI와 `VITE_API_BASE_URL` 기준으로 사용한다.

### 결정 후 영향

Google/Naver 개발자 콘솔의 Redirect URI, `app.frontend-url`, `VITE_API_BASE_URL`, Nginx 전달 헤더, Spring forward-header 처리 설정, 배포 환경변수와 운영 문서가 변경된다.

## 운영 OAuth Client Secret 관리 방식

### 현재 상태

- 코드의 설정 키는 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`으로 확정되어 있다.
- local은 Git에서 제외된 `application-local.properties`에 실제 값을 넣어 검증한 기록이 있다.
- Docker Compose는 루트 `.env`에서 값을 주입하도록 구현되어 있으며, 기본 placeholder는 컨테이너 부팅용이라 실제 OAuth 로그인에는 사용할 수 없다.
- 운영 환경에서 Secret을 어디에 저장하고 어떤 주기로 교체할지는 정해지지 않았다.

### 결정이 필요한 이유

운영 Client Secret을 저장소, 이미지 또는 일반 설정 파일에 포함하면 유출과 교체 비용이 커진다. 배포 플랫폼에 맞는 안전한 주입·회전·접근권한 정책이 필요하다.

### 선택 가능한 대안

- A안: 배포 플랫폼의 암호화된 환경변수/Secret 기능으로 주입한다.
- B안: AWS Secrets Manager 등 외부 Secret Manager를 사용하고 배포 또는 런타임에 값을 조회한다.

### 결정 후 영향

운영 배포 설정, CI/CD Secret 등록, 애플리케이션 설정 주입 방식, Secret 접근권한, 교체 절차와 운영 문서가 변경된다. Java 코드의 환경변수 이름은 유지할 수 있다.
