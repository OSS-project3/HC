# ⚠️ 임시 관리자 로그인/시드 — 반드시 제거할 것

관리자 대시보드 개발/시연을 위해 **임시 관리자 계정**을 넣어두었다. **운영 배포 전 반드시 제거**한다.

## 자격증명

- 이메일: `admin@test.com`
- 비밀번호: `admin1234!`

## 구성 (2곳)

### 1) 백엔드 — 실제 ADMIN 계정 시드
- 파일: `backend/.../infra/seed/DemoDataSeeder.java` (`ensureAdminUser`)
- 동작: `app.seed-demo-data=true`(로컬 docker-compose 기본)일 때, `admin@test.com` 계정이 없으면
  비밀번호 `admin1234!`(bcrypt 해시)로 생성하고 `User.promoteToAdmin()`으로 **ADMIN 권한** 부여. 존재하면 건너뜀(idempotent).
- 관련: `User.promoteToAdmin()` 메서드도 이 용도로 추가됨(실제 승격 정책/엔드포인트는 여전히 미구현).

### 2) 프론트엔드 — 로그인 분기
- 파일: `frontend/src/pages/LoginPage/LoginPage.tsx` (`submit`)
- 동작: 위 자격증명 입력 시 **실제 `POST /api/auth/login`** 을 호출해 ADMIN 세션(HttpOnly 쿠키)을 받고
  `refreshProfile()`로 사용자 상태를 서버 기준으로 갱신한다. → `/api/admin/**` 호출이 실제로 동작한다.
- 폴백: 백엔드가 오프라인이거나 admin이 시드되지 않아 로그인 실패 시, **클라이언트 상태만** `role:"admin"`으로 세팅해
  대시보드 UI만이라도 열리게 한다(이 경우 admin API 호출은 401이 난다).

## 왜 위험한가 / 왜 임시인가

- 실제 관리자 승격 경로(가입·승격 API·정책)가 아직 없어서, "데모 관리자"를 시드로 심어 두는 편법이다.
- 자격증명이 소스/문서에 노출되어 있다. 운영에 남으면 **누구나 관리자 로그인 가능** → 심각한 보안 사고.

## 제거 방법

1. 프론트: `LoginPage.tsx`의 `admin@test.com` 분기 블록 삭제. 일반 로그인도 실제 `api.loginWithPassword`로 전환(현재 일반 로그인은 여전히 mock).
2. 백엔드: `DemoDataSeeder.ensureAdminUser()` 및 `ADMIN_EMAIL/ADMIN_PASSWORD` 상수 삭제. 이미 생성된 DB 계정은 수동 삭제/권한 회수.
3. 관리자 승격은 정식 경로(예: 운영자 전용 승격 API 또는 DB 마이그레이션)로 대체한다.

## 관련 문서

- 관리자 대시보드에서 API가 없는 기능(이름추천·만세력·엑셀출력 등)의 설계: [`specs/admin-dashboard/DESIGN.md`](./specs/admin-dashboard/DESIGN.md)
