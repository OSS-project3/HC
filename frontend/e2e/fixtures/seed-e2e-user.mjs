// E2E 전용 USER 계정을 격리 DB에 직접 삽입한다(애플리케이션 코드 우회 — 신청 데이터는 절대
// 여기서 넣지 않고 실제 API로만 만든다, docs/collab/e2e_test.md 52행 허용 범위).
// 비밀번호 해시는 Spring Security DelegatingPasswordEncoder와 호환되는 {bcrypt}$2a$... 포맷.
// 실행: node e2e/fixtures/seed-e2e-user.mjs
// (E2E_USER_EMAIL/PASSWORD만 필요하면 다른 모듈에서 import해도 안전 — main()은 직접 실행될 때만 돈다.)
import pg from "pg";
import bcrypt from "bcryptjs";
import { fileURLToPath } from "node:url";

export const E2E_USER_EMAIL = "e2e-user@honor-citizen-e2e.local";
export const E2E_USER_PASSWORD = "E2eTestPassword!23";

async function main() {
  const client = new pg.Client({
    host: "localhost",
    port: 15432, // docker-compose.e2e.yml db는 컨테이너 내부 전용이라, 호스트 포트로 별도 노출 필요.
    user: "honor_citizen_e2e",
    password: "honor_citizen_e2e_local",
    database: "honor_citizen_e2e",
  });
  await client.connect();

  const passwordHash = "{bcrypt}" + (await bcrypt.hash(E2E_USER_PASSWORD, 10));

  const existing = await client.query("SELECT id FROM users WHERE email = $1", [E2E_USER_EMAIL]);
  if (existing.rows.length > 0) {
    console.log("이미 존재함, 건너뜀:", E2E_USER_EMAIL, "id=" + existing.rows[0].id);
    await client.end();
    return;
  }

  const result = await client.query(
    `INSERT INTO users
       (email, name, phone, role, password_hash,
        terms_agreed, privacy_agreed, image_upload_agreed, shipping_agreed, terms_agreed_at,
        created_at, updated_at)
     VALUES ($1, $2, $3, 'USER', $4, true, true, true, true, NOW(), NOW(), NOW())
     RETURNING id`,
    [E2E_USER_EMAIL, "E2E Tester", "010-0000-0000", passwordHash],
  );

  console.log("생성 완료: id=" + result.rows[0].id, "email=" + E2E_USER_EMAIL);
  await client.end();
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch((e) => {
    console.error(e);
    process.exit(1);
  });
}
