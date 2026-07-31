# User/Auth/OAuth/JWT 도메인 테스트 로그

## 목적

User/Auth 도메인(OAuth2 로그인, JWT 발급/재발급/로그아웃, 약관동의)이 실제로 동작하는지
로컬 환경에서 수동으로 검증한 기록. 자동화 테스트 코드는 아직 없으므로, 이 문서가
현재까지 검증된 범위와 그 과정에서 발견/수정한 버그에 대한 유일한 근거 자료다.

- 검증 일자: 2026-07-31
- 실행 환경: 로컬 (`--spring.profiles.active=local`), H2, Redis(로컬 프로세스), 프론트 `localhost:5173`
- 방법: 브라우저(OAuth 리다이렉트 플로우) + `curl.exe`(쿠키 jar 기반, API 직접 호출)

---

## 1. OAuth2 로그인 (E2E)

| 시나리오 | 기대 결과 | 실제 결과 |
|---|---|---|
| Google OAuth 로그인 | 로그인 성공 → 홈으로 리다이렉트, 세션 생성 | ✅ 서버 로그 `보안 이벤트: 로그인 성공 userId=1` 확인 |
| Naver OAuth 로그인 | 로그인 성공 → 홈으로 리다이렉트, 세션 생성 | ✅ 서버 로그 `보안 이벤트: 로그인 성공 userId=2` 확인 |

---

## 2. JWT — Refresh / Reuse Detection / Logout

`POST /api/auth/refresh`, `POST /api/auth/logout` 대상. 쿠키 jar(`cookies.txt`, Netscape 포맷)로
`accessToken`/`refreshToken`을 관리하며 순차 실행.

| 시나리오 | 기대 결과 | 실제 결과 |
|---|---|---|
| ✅ Refresh Token Rotation | `200`, 새 accessToken/refreshToken 쌍 발급 | ✅ |
| ✅ Refresh Token Reuse Detection | 이미 rotate된 옛 refreshToken 재사용 시 `401 REFRESH_TOKEN_REUSE_DETECTED` | ✅ |
| ✅ Cascade Revoke | reuse 감지 직후, 방금 발급된 "최신" refreshToken도 함께 무효화되는지 | ✅ `401 INVALID_REFRESH_TOKEN` (세션 자체가 삭제되어 "재사용 감지"가 아닌 "세션 없음" 분기로 응답 — 코드상 의도된 동작, [TokenSessionStore.java:46-68](../../src/main/java/com/example/honorcitizen/infra/security/TokenSessionStore.java#L46-L68) 참고) |
| ✅ Logout | `200` | ✅ (최초 시도 시 Redis 설정 문제로 `500` — 3장 참고. 원인 해결 후 재시도 통과) |
| ✅ Logout 후 Access Token 무효화 | 로그아웃한 accessToken으로 보호 API 호출 시 `401` | ✅ (블랙리스트 정상 동작) |

---

## 3. 약관동의 (`POST /api/auth/terms`)

Request body: `{ privacyAgreed, imageUploadAgreed, shippingAgreed }` (전부 필수, `@NotNull`)

| 시나리오 | 기대 결과 | 실제 결과 |
|---|---|---|
| ✅ 최초 요청 | `200` + 응답에 4개 필드 모두 반영, `agreedAt` 채워짐 | ✅ |
| ✅ 중복 요청 (이미 동의한 계정으로 재호출) | `409 TERMS_ALREADY_AGREED` | ✅ |

---

## 4. 쿠키 속성

| 항목 | 기대 결과 | 실제 결과 |
|---|---|---|
| ✅ HttpOnly | accessToken/refreshToken 모두 JS로 접근 불가 | ✅ |
| ✅ SameSite=Strict | 설정값대로 적용 | ✅ |
| ✅ Secure (로컬 환경 기준) | 로컬 HTTP 개발에서는 `app.cookie.secure=false`로 분기 적용 확인 | ✅ |

---

## 5. 인증 필요 API 정상 흐름

| 시나리오 | 기대 결과 | 실제 결과 |
|---|---|---|
| `GET /api/my/applications` (유효 토큰) | `200` + `{ applications: [] }` (신청 내역 없는 신규 유저) | ✅ |

> 최초 테스트 시 이 엔드포인트에서 `500 INTERNAL_ERROR`가 한 차례 발생했으나, 로그 버퍼링 문제로
> 당시 원인을 특정하지 못한 채 재현되지 않았다. 이후 여러 차례 재시도에서 모두 `200`으로 정상
> 응답해 재발 없음을 확인했다 — 특정 원인이 밝혀지지 않은 채 "재현 불가"로 종결.

---

## 발견 및 수정한 버그 (오늘 테스트 과정에서)

| # | 증상 | 원인 | 수정 |
|---|---|---|---|
| 1 | 백엔드 컴파일 실패 | `ApplicationService`/`BulkApplicationService`가 `Application.createSingle/createBulk`의 새 인자(`entryDate`/`address`/`cardType`)를 안 넘김 | 임시로 `null` 채워 컴파일만 통과 (TODO 주석, 실제 로직은 Application 도메인 재설계 시 처리) — [ApplicationService.java:47-60](../../src/main/java/com/example/honorcitizen/domain/application/service/ApplicationService.java#L47-L60) |
| 2 | 앱 부팅 실패 | `JWT_SECRET`/Redis 설정값 없음 | `application-local.properties`에 로컬 전용 값 추가 |
| 3 | 앱 부팅 실패 | `S3Config`의 `s3Client` 빈이 즉시(eager) 생성되는데 AWS 관련 환경변수 자체가 없음 | 부팅만 통과시킬 더미 값 추가 (`AWS_ACCESS_KEY` 등, 실제 S3 호출은 불가) |
| 4 | OAuth 콜백 URL에 `${GOOGLE_CLIENT_ID}` 문자열이 그대로 노출 | `spring-dotenv`가 이 Spring Boot 4.x 구성에서 `.env`를 실제로 로드하지 않음 | `.env` 값을 `application-local.properties`로 직접 복사 |
| 5 | 로그인 완료 직전 `500` (`DataIntegrityViolationException: Value too long for column REFRESH_TOKEN VARCHAR(255)`) | `User.refreshToken`에 `@Column(length=...)` 누락 → 기본 255자로 JWT 문자열(300자+)이 안 들어감 | `@Column(length = 2048)` 추가 — [User.java:54-55](../../src/main/java/com/example/honorcitizen/domain/user/entity/User.java#L54-L55) |
| 6 | 로그인 성공 후 "연결이 거부되었습니다" | `app.frontend-url=http://localhost:3000` (실제 Vite는 5173) + 프론트 서버 미실행 | `application-local.properties`에서 포트 5173으로 override + 프론트 서버 기동 |
| 7 | `POST /api/auth/logout` 호출 시 `500` (`RedisSystemException: MISCONF ...`) | 로컬 Redis 프로세스의 `dir`(RDB 저장 경로)이 `C:\WINDOWS\system32`로 잡혀 BGSAVE가 계속 실패 → `stop-writes-on-bgsave-error`로 모든 쓰기 명령 차단 | `redis-cli config set dir`로 쓰기 가능한 경로로 변경 후 `bgsave` 재시도 — 애플리케이션 코드 문제 아님, 로컬 Redis 실행 방식 문제. Docker로 전환 시 `-v redis_data:/data` 볼륨 마운트로 구조적으로 재발 안 함 |

---

## 결론

User/OAuth/JWT 도메인은 이번 세션에서 정의한 시나리오(로그인, 재발급, 재사용 감지, 로그아웃+블랙리스트,
약관동의, 쿠키 속성) 전부 통과. 정식 자동화 테스트(통합 테스트 코드)는 아직 없음 — 추후 회귀 방지를
위해 이 시나리오들을 `@SpringBootTest` 기반 통합 테스트로 옮기는 것을 권장.
