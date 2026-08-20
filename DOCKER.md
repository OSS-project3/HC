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

## EC2 배포 — 시크릿 3단 분리 원칙

이 저장소는 시크릿을 3곳으로 분리해서 다룬다. **이 순서를 벗어나면 안 된다**(특히 Dockerfile에 키를 절대 넣지 않는다):

1. **`Dockerfile`(`backend/honor-citizen/Dockerfile`, `frontend/Dockerfile`)** — 시크릿을 전혀 참조하지 않는다. `docker build` 결과 이미지 자체엔 AWS 키·JWT 시크릿 등 어떤 값도 남지 않는다(이미지가 유출돼도 안전).
2. **`docker-compose.yml`** — 값이 아니라 **환경변수 이름만** 참조한다(`${AWS_ACCESS_KEY}` 같은 형태). 로컬 개발 편의를 위한 placeholder 기본값(`local-access-key` 등)만 코드에 있고, 실제 값은 절대 여기 넣지 않는다.
3. **EC2의 `.env`(저장소에 커밋되지 않음)** — 실제 자격증명이 담기는 유일한 곳. `docker compose` 명령을 실행하는 디렉터리(=저장소 루트)에 이 파일이 있으면 Compose가 자동으로 읽어서 2번의 환경변수 이름에 값을 채운다.

### EC2에서 하는 절차

```bash
# 1) 저장소를 EC2로 가져온다(최초 1회는 clone, 이후는 pull)
git clone https://github.com/OSS-project3/HC.git
cd HC

# 2) .env.example을 .env로 복사하고 실제 값을 채운다
cp .env.example .env
nano .env   # 또는 vim — AWS_ACCESS_KEY, AWS_SECRET_KEY, JWT_SECRET, EMAIL_CODE_SECRET,
            # MAIL_USERNAME/PASSWORD, GOOGLE/NAVER 클라이언트 자격증명, POSTGRES_PASSWORD,
            # APP_FRONTEND_URL(실제 도메인)까지 전부 실제 값으로 채운다

# 3) 소유자만 읽을 수 있게 권한을 좁힌다
chmod 600 .env

# 4) 빌드 후 기동
docker compose up -d --build
```

- `.gitignore`에 `.env`/`.env.*`가 이미 등록돼 있고 `.env.example`만 예외로 커밋 대상이다 — `.env`를 실수로 `git add`해도 무시된다(직접 `git add -f`로 강제하지 않는 한 안전).
- `AWS_ACCESS_KEY`/`AWS_SECRET_KEY`는 **IAM 루트 키가 아니라, S3 버킷 하나에만 쓰기 권한을 준 전용 IAM 사용자**를 새로 만들어 그 키를 쓰는 걸 권장한다(루트 키 유출 시 계정 전체가 위험해짐 — 이건 코드가 아니라 AWS 콘솔에서 직접 처리해야 하는 부분).
- 값을 바꾼 뒤에는 `docker compose up -d --build`를 다시 실행해야 컨테이너가 새 값을 읽는다(`.env`만 고치고 재시작 안 하면 기존 컨테이너는 옛 값을 계속 씀).

## 실서비스(도메인) 배포 시 추가로 필요한 것

위 시크릿 설정 외에 실제 도메인으로 배포하려면 최소한 아래가 더 필요하다:

- `APP_FRONTEND_URL`을 실제 도메인(`https://...`)으로 설정 — OAuth 로그인 성공 후 리다이렉트 주소로 쓰인다(설정 안 하면 `localhost:3000`으로 리다이렉트됨).
- HTTPS(Let's Encrypt/certbot 또는 ALB+ACM) — `frontend/nginx.conf`는 현재 80번 포트만 처리하며 인증서 설정이 없다.
- `.env`에 `COOKIE_SECURE=true` 설정 — `docker-compose.yml`은 로컬 개발 편의를 위해 기본값이 `false`다. HTTPS 도메인으로 배포하면서 이 값을 안 바꾸면 로그인 쿠키가 `Secure` 속성 없이 발급돼 브라우저가 저장을 거부할 수 있다.
- 구글/네이버 개발자 콘솔에 프로덕션 리다이렉트 URI(`https://실도메인/login/oauth2/code/{google|naver}`) 등록.
- EC2 보안그룹 인바운드에 80·443 포트가 열려 있는지 확인(기본은 SSH 22번만 열려있는 경우가 많음).
- `POSTGRES_*`/`JWT_SECRET`/`EMAIL_CODE_SECRET`/`AWS_*` 전부 로컬 placeholder가 아닌 실제 값으로 교체(위 절차의 2번 단계에서 함께 처리).

## 종료 및 로그

```bash
docker compose logs -f
docker compose down
```

기본 placeholder 환경 변수로 컨테이너 부팅과 비인증 신청 조회 화면까지 확인할 수 있다. `.env`의 `JWT_SECRET`은 디코딩했을 때 32바이트 이상인 Base64 문자열이어야 한다. Google/Naver 로그인과 S3 업로드는 각 서비스의 실제 자격 증명 및 OAuth redirect URI 설정이 필요하다.
