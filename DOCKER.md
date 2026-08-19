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
- DB(Postgres)·Redis: Compose 내부 네트워크에서만 접근

프론트 Nginx가 `/api`, `/oauth2`, `/login/oauth2` 요청을 백엔드로 프록시하므로 브라우저에서는 동일 출처 쿠키로 API를 호출한다.

## DB(Postgres)

- ✅ 2026-08-19 추가: 이전엔 datasource 설정이 없어 백엔드가 H2 인메모리로 기동됐다(컨테이너 재시작마다 데이터 소실). 이제 `db` 서비스(Postgres 16)가 함께 뜨고, `db-data` named volume에 데이터가 영속된다.
- 계정/DB명은 `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD` 환경변수로 바꿀 수 있고, 기본값(`honor_citizen`/`honor_citizen`/`honor_citizen_local`)은 로컬 전용이다 — **실서비스 배포 시 반드시 실제 값으로 교체**한다.
- `SPRING_JPA_HIBERNATE_DDL_AUTO`는 기본 `update`(누락된 테이블/컬럼만 추가, 기존 데이터 유지)다. 스키마가 안정화된 뒤에는 `validate`로 낮추는 것을 검토한다.

## 실서비스(도메인) 배포 시 추가로 필요한 것

이 Compose 구성은 로컬/단일 서버 기동까지만 다룬다. 실제 도메인으로 배포하려면 최소한 아래가 더 필요하다:

- `APP_FRONTEND_URL`을 실제 도메인(`https://...`)으로 설정 — OAuth 로그인 성공 후 리다이렉트 주소로 쓰인다(설정 안 하면 `localhost:3000`으로 리다이렉트됨).
- HTTPS(Let's Encrypt/certbot 또는 ALB+ACM) — `frontend/nginx.conf`는 현재 80번 포트만 처리하며 인증서 설정이 없다. `COOKIE_SECURE=true`(운영 기본값)는 HTTPS 없이는 쿠키가 브라우저에 안 걸릴 수 있다.
- 구글/네이버 개발자 콘솔에 프로덕션 리다이렉트 URI(`https://실도메인/login/oauth2/code/{google|naver}`) 등록.
- `MAIL_USERNAME`/`MAIL_PASSWORD`(이메일 인증 발송), `EMAIL_CODE_SECRET`(기본값 없음 — 없으면 기동 자체가 실패) — `.env.example`에 아직 반영 안 됨, 실배포 전 `.env`에 직접 채워야 한다.
- `POSTGRES_*`/`JWT_SECRET`/`AWS_*` 전부 로컬 placeholder가 아닌 실제 값으로 교체.

## 종료 및 로그

```bash
docker compose logs -f
docker compose down
```

기본 placeholder 환경 변수로 컨테이너 부팅과 비인증 신청 조회 화면까지 확인할 수 있다. `.env`의 `JWT_SECRET`은 디코딩했을 때 32바이트 이상인 Base64 문자열이어야 한다. Google/Naver 로그인과 S3 업로드는 각 서비스의 실제 자격 증명 및 OAuth redirect URI 설정이 필요하다.
