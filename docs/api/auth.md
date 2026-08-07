### API 1 / 3 — OAuth 로그인 (시작 + 콜백)

#### ④ Request/Response 설계

**로그인 시작**
```
GET /oauth2/authorization/{provider}
```
| 경로 변수 | 값 | 설명 |
|---|---|---|
| provider | `google` \| `naver` | `.md` User.provider(GOOGLE, NAVER)와 매칭 |

Spring Security OAuth2 표준 라우트라 커스텀 request body 없음 → provider의 로그인 페이지로 리다이렉트.

**콜백 (провider 인증 성공 후 서버가 처리)**

```
Response 302 Found
Set-Cookie: accessToken={JWT}; HttpOnly; Path=/; Max-Age=900
Set-Cookie: refreshToken={JWT}; HttpOnly; Path=/; Max-Age=1209600
Location: {프론트 URL}
```

| 케이스 | 리다이렉트 위치 |
|---|---|
| 기존 회원(재로그인) | `/` |
| 신규 회원(최초 로그인 → 계정 자동 생성) | `/terms` — 약관 동의 전용 화면 |

#### 신규 OAuth 사용자 약관 화면 계약

- 프론트는 `/terms` 라우트를 제공해야 한다.
- `/terms`는 신규 OAuth 사용자의 약관 동의를 위한 별도 화면으로 유지한다.
- 현재 개인정보 처리방침과 이용약관의 최종 내용은 확정되지 않았으므로 본문은 `[TBD]`로 관리한다.
- 확정되지 않은 약관 문구나 버전을 프론트에서 최종 정책으로 간주해 하드코딩하지 않는다.
- 약관 동의 제출은 기존 `POST /api/auth/terms` 계약을 사용하며 Request/Response는 변경하지 않는다.
- 동의 완료 후 프론트는 홈(`/`)으로 이동하고 `GET /api/users/me`를 통해 로그인 사용자 상태를 갱신한다.

#### ⑤ Validation

- `provider`는 `google`/`naver`만 허용, 그 외 값은 404
- OAuth 프로바이더가 반환하는 `email`이 없으면(권한 미동의 등) 로그인 실패 처리

#### ⑥ DB 컬럼과 매핑 검증

최초 로그인 시 `User` row 생성에 쓰이는 값:

| User 컬럼 | 출처 |
|---|---|
| email | OAuth 프로바이더 응답 |
| provider | 요청 경로의 `{provider}` |
| provider_id | OAuth 프로바이더 응답(고유 ID) |
| name | OAuth 프로바이더 응답 |
| role | 기본값 `USER` |
| phone | NULL로 생성 (아래 참고) |
| address | NULL 허용이라 문제 없음 |

#### ⑦ 누락된 필드 확인

**해결됨 (2026-07-29):** `User.phone`을 NOT NULL → NULL 허용으로 정정했습니다. 실제 연락처는 신청 시점마다 `Applicant.phone`에서 받으므로 계정 가입 단계에서 강제로 받을 필요가 없습니다. 다만 신규 OAuth 사용자는 별도 정보 입력 온보딩 없이 약관 동의 전용 화면인 `/terms`로 리다이렉트됩니다.

**API 1 완료.**

---

### API 3 / 3 — 로그아웃 ⚠️ 확인필요 — 버튼은 있으나 mock 세션 clear만 함, 실제 호출 없음

#### ④ Request/Response 설계

```
POST /api/auth/logout
Cookie: accessToken={JWT}; refreshToken={JWT}
```

**Response `200 OK`**
```json
{ "success": true }
```

#### ⑤ Validation

- 비로그인 상태에서 호출해도 그냥 200(멱등 처리) — 굳이 401 안 줘도 됨

#### ⑥ DB 컬럼과 매핑 검증

`User` 테이블 쓰기 없음 — `accessToken`/`refreshToken` 쿠키를 만료시키고(Max-Age=0), refresh 세션 저장소가 생기면 그쪽도 무효화(세부 구현은 위 TODO 참고).

#### ⑦ 누락된 필드 확인

없음.

**API 3 완료.**

---
