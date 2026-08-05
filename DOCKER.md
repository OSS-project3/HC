# Docker 통합 실행

## 실행

1. Docker Desktop을 실행한다.
2. 실제 소셜 로그인/S3 테스트가 필요하면 `.env.example`을 `.env`로 복사하고 값을 채운다.
3. 저장소 루트에서 실행한다.

```bash
docker compose up -d --build
```

- 프론트엔드: http://localhost:3000
- 백엔드: http://localhost:8080
- Redis: Compose 내부 네트워크에서만 접근

프론트 Nginx가 `/api`, `/oauth2`, `/login/oauth2` 요청을 백엔드로 프록시하므로 브라우저에서는 동일 출처 쿠키로 API를 호출한다.

## 종료 및 로그

```bash
docker compose logs -f
docker compose down
```

기본 placeholder 환경 변수로 컨테이너 부팅과 비인증 신청 조회 화면까지 확인할 수 있다. `.env`의 `JWT_SECRET`은 디코딩했을 때 32바이트 이상인 Base64 문자열이어야 한다. Google/Naver 로그인과 S3 업로드는 각 서비스의 실제 자격 증명 및 OAuth redirect URI 설정이 필요하다.
