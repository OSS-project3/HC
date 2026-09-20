# 임시 관리자 로그인/시드 — 해결됨(2026-09-20)

관리자 대시보드 개발/시연을 위해 넣어뒀던 **임시 관리자 계정**은 제거됐다. 이 문서는 경위와 해결 내용, 새 정책을 기록한다.

## 확정 정책(2026-09-20)

- 운영에서는 `DemoDataSeeder` 기반 임시 관리자 생성을 쓰지 않는다.
- 관리자는 **일반 회원가입 후 운영자가 DB에서 해당 사용자의 `role`을 `ADMIN`으로 1회 변경**한다.
  ```sql
  UPDATE users SET role = 'ADMIN' WHERE email = '운영자가_지정한_이메일';
  ```
- 별도의 관리자 승격 API/UI는 현재 범위에서 구현하지 않는다.
- 향후 관리자 권한 관리 수요(여러 명 승격·감사 기록 필요 등)가 늘어나면 별도 기능으로 도입한다.

## 해결 내용(백엔드)

- `backend/.../infra/seed/DemoDataSeeder.java`에서 `ensureAdminUser()` 메서드와 `ADMIN_EMAIL`/`ADMIN_PASSWORD` 상수, 관련 `PasswordEncoder` 의존성을 삭제했다. `app.seed-demo-data=true`여도 더 이상 관리자 계정을 시드하지 않는다(공지·FAQ·후기·행사·데모 신청 시드만 남음).
- `User.promoteToAdmin()`은 삭제하지 않고 남겨뒀다 — 테스트 픽스처에서 ADMIN 상태를 만들 때만 쓰는 헬퍼로 주석을 정리했다.
- 이미 이전에 시드됐던 `admin@test.com` DB 계정이 있다면(로컬/데모 환경) 필요 시 수동으로 삭제하거나 권한을 회수한다 — 이 문서의 자격증명(`admin@test.com`/`admin1234!`)은 더 이상 유효한 시드 경로가 없으므로 참고용으로만 남긴다.

## 남은 항목(프론트, 백엔드 담당자 범위 밖)

- `frontend/src/pages/LoginPage/LoginPage.tsx`의 "로그인 실패 시 클라이언트 상태만 `role:"admin"`으로 세팅하는 폴백 블록"은 아직 남아 있다. 운영에서는 실패를 실패로 처리해야 하므로 제거가 필요하지만, 이 저장소 정책상 `frontend/` 수정은 별도 확인이 필요해 이번 정리 범위에 포함하지 않았다.

## 관련 문서

- `docs/BACKEND_TODO.md` §7 — 이번 정리의 백엔드 작업 항목.
- 관리자 대시보드에서 API가 없는 기능(이름추천·만세력·엑셀출력 등)의 설계: [`specs/admin-dashboard/DESIGN.md`](./specs/admin-dashboard/DESIGN.md)
