# JWT 인증/인가 보안 명세서 (OWASP 기반)

## 목적

본 프로젝트의 JWT 인증 시스템은 OWASP JWT Cheat Sheet 및 OWASP Session Management Guide를 기준으로 구현한다.

JWT 사용 시 발생 가능한 다음 취약점을 방지해야 한다.

* alg=none 공격
* JWT 위조
* 토큰 탈취
* XSS 기반 토큰 유출
* CSRF
* Refresh Token 재사용 공격
* 장기 세션 악용
* 민감정보 노출

---

# 1. 인증 구조

## 로그인 흐름

```text
Google OAuth2 Login

→ 사용자 인증 성공

→ Access Token 발급
→ Refresh Token 발급

→ Access Token 저장
→ Refresh Token 저장

→ 인증 완료
```

---

# 2. Token 정책

## Access Token

### 목적

API 인증

### 만료시간

```text
15분
```

### 저장 위치

```text
HttpOnly Cookie
```

### Cookie 옵션

```text
HttpOnly = true
Secure = true
SameSite = Strict
```

### 포함 Claim

```json
{
  "sub": "userId",
  "role": "USER",
  "iat": 1710000000,
  "exp": 1710000900,
  "iss": "application-name"
}
```

### 금지 Claim

절대 저장 금지

```json
{
  "password": "...",
  "email": "...",
  "phone": "...",
  "address": "...",
  "refreshToken": "..."
}
```

JWT Payload는 암호화가 아니며 누구나 디코딩 가능하므로 민감정보 저장 금지

---

## Refresh Token

### 목적

Access Token 재발급

### 만료시간

```text
14일
```

### 저장 위치

```text
HttpOnly Cookie
```

### DB 저장

필수

```text
Refresh Token
User ID
만료시간
생성시간
상태
```

저장

---

# 3. JWT 생성 규칙

## 허용 알고리즘

```text
HS256
```

또는

```text
RS256
```

중 하나만 사용

---

## 금지

JWT Header의 alg 값을 신뢰하여 검증 로직 결정

예시

```json
{
  "alg": "none"
}
```

수신 시 즉시 거부

---

## 서버 검증 방식

반드시 서버가 허용 알고리즘을 고정 지정

예시

```java
JWT.require(
    Algorithm.HMAC256(secret)
)
```

---

# 4. Secret Key 정책

## 길이

최소

```text
256bit 이상
```

랜덤 키 사용

---

## 저장 위치

금지

```java
String SECRET = "my-secret";
```

허용

```env
JWT_SECRET=...
```

---

## 환경변수 사용

```yaml
jwt:
  secret: ${JWT_SECRET}
```

---

# 5. JWT 검증 규칙

모든 요청마다 수행

## 검증 순서

### 1

Signature 검증

실패 시

```text
401 Unauthorized
```

---

### 2

alg 검증

허용 알고리즘 외 거부

---

### 3

exp 검증

만료 시 거부

---

### 4

iss 검증

발급자 검증

---

### 5

sub 검증

사용자 존재 여부 검증

---

### 6

권한 검증

```text
USER
ADMIN
```

등

Role 확인

---

# 6. Token Storage 정책

## 금지

```javascript
localStorage.setItem(...)
```

```javascript
sessionStorage.setItem(...)
```

---

## 이유

XSS 발생 시

```javascript
localStorage.getItem(...)
```

으로 토큰 탈취 가능

---

## 허용

```text
HttpOnly Cookie
```

---

# 7. XSS 대응

JWT 보안은 XSS 방어와 함께 구현

## 적용

Content Security Policy

```http
Content-Security-Policy
```

설정

---

입력값 검증

출력 인코딩

적용

---

# 8. CSRF 대응

Cookie 기반 인증 사용 시 적용

## Cookie 옵션

```text
SameSite=Strict
```

우선 사용

---

## 필요 시

CSRF Token 적용

예시

```text
X-CSRF-TOKEN
```

Header 검증

---

# 9. Logout 정책

로그아웃 시

## 수행

Refresh Token 삭제

DB 상태 변경

Cookie 삭제

---

## Access Token

만료 전까지 유효

따라서

```text
짧은 만료시간
```

사용

---

# 10. Refresh Token Rotation

재발급 시

기존 Refresh Token 폐기

새 Refresh Token 발급

---

예시

```text
RT-1 사용

↓

AT-2
RT-2 발급

↓

RT-1 폐기
```

---

이전 Refresh Token 재사용 시

```text
강제 로그아웃
보안 이벤트 기록
```

수행

---

# 11. 보안 로그

다음 이벤트 기록

* 로그인 성공
* 로그인 실패
* 토큰 재발급
* 토큰 검증 실패
* 만료 토큰 사용
* Refresh Token 재사용
* 로그아웃

---

# 12. Spring Security 구현 요구사항

## 필수 구성

* Spring Security
* OAuth2 Login
* JWT Filter
* AuthenticationEntryPoint
* AccessDeniedHandler

---

## JWT Filter 역할

```text
Cookie 추출

↓

JWT 검증

↓

SecurityContext 등록

↓

Controller 진입
```

---

# 13. 금지사항

## 금지 1

JWT Payload에 민감정보 저장

---

## 금지 2

localStorage 저장

---

## 금지 3

alg Header 신뢰

---

## 금지 4

하드코딩 Secret

---

## 금지 5

만료시간 없는 JWT

---

## 금지 6

Refresh Token 미관리

---

## 금지 7

Signature 검증 생략

---

# 14. 완료 조건

다음 조건을 모두 만족해야 한다.

* alg=none 공격 차단
* Signature 검증 구현
* exp 검증 구현
* iss 검증 구현
* HttpOnly Cookie 사용
* Secure Cookie 사용
* SameSite 적용
* Refresh Token DB 저장
* Refresh Token Rotation 구현
* Secret 환경변수 관리
* 민감정보 Payload 저장 금지
* Spring Security 연동 완료
* OAuth2 Login 연동 완료

```
```
