// 개인 명예시민증 전체 흐름 E2E — docs/collab/e2e_test.md 2~6절 매핑.
// 격리 스택(docker-compose.e2e.yml, -p hc-e2e) 대상. 앱 코드는 안 건드리고 실제 UI를 그대로 조작한다.
// zodiacDesignSet(1~5)은 프론트 UI가 없어(Phase0 확인) page.request로 직접 PUT 호출 — HYBRID PASS.
import { test, expect, type Page, type APIResponse } from "@playwright/test";
import pg from "pg";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createHash } from "node:crypto";
import { E2E_USER_EMAIL, E2E_USER_PASSWORD } from "./fixtures/seed-e2e-user.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FACE_PHOTO = path.join(__dirname, "fixtures", "face-photo.png");
const ADMIN_EMAIL = "admin@test.com";
const ADMIN_PASSWORD = "admin1234!";
const CARD_NUMBER = "ROK-00001-0001";
const ZODIAC_DESIGN_SET = 2;

const pool = new pg.Pool({
  host: "localhost",
  port: 15432,
  user: "honor_citizen_e2e",
  password: "honor_citizen_e2e_local",
  database: "honor_citizen_e2e",
});

async function login(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByPlaceholder("you@example.com").fill(email);
  await page.getByPlaceholder("비밀번호").fill(password);
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByRole("button", { name: "로그인" })).toHaveCount(0, { timeout: 15_000 });
}

async function pickSearchableOption(page: Page, ariaLabel: string, optionText: string) {
  await page.getByRole("button", { name: ariaLabel }).click();
  const panel = page.locator(".select-field.is-open");
  await panel.getByPlaceholder(/입력해 주세요/).fill(optionText);
  await panel.getByRole("option", { name: optionText, exact: false }).first().click();
}

async function pickPlainOption(page: Page, ariaLabel: string, optionText: string) {
  await page.getByRole("button", { name: ariaLabel }).click();
  await page.getByRole("option", { name: optionText, exact: true }).click();
}

test.describe.serial("개인 명예시민증 신청→관리자 처리→카드 생성", () => {
  let applicationId: number;
  let memberId: number;
  let applicationNumber: string;

  test.afterAll(async () => {
    await pool.end();
  });

  test("1. 사용자 신청 — 로그인 후 개인 명예시민증 신청서 제출", async ({ page }) => {
    await login(page, E2E_USER_EMAIL, E2E_USER_PASSWORD);

    await page.goto("/apply/honorary-citizen");

    // Step 0: 유형 선택
    await page.getByRole("button", { name: "개인 신청" }).click();
    await page.getByRole("checkbox", { name: "위 안내사항을 확인하였으며" }).check();
    await page.getByRole("checkbox", { name: "본 증서는 문화체험용" }).check();
    await page.getByRole("button", { name: "다음" }).click();

    // Step 1: 정보 입력
    await page.getByPlaceholder("HONG GIL DONG").fill("E2E TESTER");
    await pickSearchableOption(page, "국적 선택", "대한민국");
    await pickSearchableOption(page, "출생지역 선택", "서울");
    await page.locator('input[type="date"]').first().fill("1990-01-01");
    await pickPlainOption(page, "성별 선택", "남성");
    // 한국입국일: 첫 번째 date input이 생년월일이므로 두 번째가 한국입국일.
    await page.locator('input[type="date"]').nth(1).fill("2020-01-01");
    await page.getByPlaceholder("카드에 표시될 주소를 입력해 주세요").fill("서울특별시 종로구 세종로 1 (E2E 테스트 주소)");
    await page.getByPlaceholder("010-1234-5678").fill("010-1234-5678");
    await page.getByPlaceholder("hong@example.com").fill(E2E_USER_EMAIL);
    await page.getByRole("button", { name: "다음" }).click();

    // Step 2: 파일 등록
    await page.locator("#upload-faceFile input[type=file]").setInputFiles(FACE_PHOTO);
    await page.getByRole("button", { name: "다음" }).click();

    // Step 3: 최종 확인 → 제출
    const [response] = await Promise.all([
      page.waitForResponse((res: APIResponse) => res.url().includes("/api/applications") && res.request().method() === "POST"),
      page.getByRole("button", { name: "신청 제출" }).click(),
    ]);
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.success).toBe(true);
    applicationId = body.data.applicationId;
    applicationNumber = body.data.applicationNumber;
    expect(applicationId).toBeGreaterThan(0);

    await expect(page.getByText(applicationNumber)).toBeVisible({ timeout: 10_000 });
  });

  test("2. DB 검증 — 신청 데이터가 격리 DB에 저장됐는지 확인", async () => {
    const { rows } = await pool.query(
      "SELECT application_number, application_type, status, payment_status, total_quantity, issue_type FROM applications WHERE id = $1",
      [applicationId],
    );
    expect(rows).toHaveLength(1);
    expect(rows[0].application_number).toBe(applicationNumber);
    expect(rows[0].application_type).toBe("INDIVIDUAL");
    expect(rows[0].status).toBe("SUBMITTED");
    expect(rows[0].issue_type).toBe("MOBILE");

    const memberRows = await pool.query(
      "SELECT id, address, photo_path FROM application_members WHERE application_id = $1",
      [applicationId],
    );
    expect(memberRows.rows).toHaveLength(1);
    memberId = memberRows.rows[0].id;
    expect(memberRows.rows[0].address).toContain("세종로");
    expect(memberRows.rows[0].photo_path).toBeTruthy();
  });

  test("3. 관리자 처리 — 결제 확인 → 검토 시작 → 작명 승인", async ({ page }) => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");

    await page.getByRole("button", { name: applicationNumber }).click();
    const statusSelect = page.getByLabel("상태 변경");

    await statusSelect.selectOption({ label: "결제 확인" });
    await expect(page.getByText("결제 확인 완료")).toBeVisible({ timeout: 10_000 });

    await statusSelect.selectOption({ label: "검토 시작" });
    await expect(page.getByText("검토 시작 완료")).toBeVisible({ timeout: 10_000 });

    await statusSelect.selectOption({ label: "작명 승인(작명중으로)" });
    await expect(page.getByText("작명 승인(작명중으로) 완료")).toBeVisible({ timeout: 10_000 });

    const { rows } = await pool.query("SELECT status, payment_status FROM applications WHERE id = $1", [applicationId]);
    expect(rows[0].status).toBe("NAME_EDITING");
    expect(rows[0].payment_status).toBe("CONFIRMED");
  });

  test("4. 관리자 처리 — 출생지역 검색 및 만세력 확정", async ({ page }) => {
    // 이전 테스트에서 이미 로그인·상세 패널이 열려 있는 새 page 컨텍스트이므로 다시 진입한다.
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");
    await page.getByRole("button", { name: applicationNumber }).click();

    await page.getByPlaceholder("출생지역 검색").fill("서울");
    await page.getByRole("button", { name: "검색", exact: true }).click();
    await expect(page.locator(".admin-naming__resolve select")).toBeVisible({ timeout: 10_000 });

    await page.getByRole("button", { name: "만세력 확정" }).click();
    await expect(page.getByText(/만세력 결과를 확정 저장했습니다|출생시간 미상으로 만세력 결과를 저장했습니다/)).toBeVisible({ timeout: 15_000 });

    const active = await page.request.get(`/api/admin/applications/${applicationId}/members/${memberId}/manseryeok`);
    expect(active.ok()).toBeTruthy();
    const activeBody = await active.json();
    expect(activeBody.success).toBe(true);
    expect(activeBody.data.confirmedPillars).toBeTruthy();
  });

  test("5. 관리자 처리 — 이름 추천 목록에서 선택 + 성씨 입력", async ({ page }) => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");
    await page.getByRole("button", { name: applicationNumber }).click();

    // 추천은 프론트 클라이언트 mock(adminNamingMock.ts)이라 특정 이름을 강제하지 않고,
    // "목록이 뜨고 하나를 선택할 수 있다"만 확인한다(정책 위반 이슈는 별도 트랙).
    await page.locator(".admin-naming__surname input").fill("김");
    const firstRec = page.locator(".admin-naming__rec").first();
    await expect(firstRec).toBeVisible({ timeout: 10_000 });
    await firstRec.getByRole("button", { name: "이 이름 선택" }).click();
    await expect(page.getByText(/이름을 확정했습니다/)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText("작명 완료").first()).toBeVisible();

    const { rows } = await pool.query(
      "SELECT surname, name, name_meaning FROM application_members WHERE id = $1",
      [memberId],
    );
    expect(rows[0].surname).toBe("김");
    expect(rows[0].name).toBeTruthy();
  });

  test("6. 카드 준비 — 카드번호 입력 및 zodiacDesignSet 직접 API 호출 (HYBRID / UI 없음)", async ({ page }) => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");
    await page.getByRole("button", { name: applicationNumber }).click();

    await page.locator(".admin-naming__cardnum input").fill(CARD_NUMBER);
    await page.locator(".admin-naming__cardnum").getByRole("button", { name: "저장" }).click();
    await expect(page.getByText("카드번호를 저장했습니다")).toBeVisible({ timeout: 10_000 });

    // zodiacDesignSet(1~5): 프론트에 UI가 전혀 없음(Phase0 확인, grep 0건) — 같은 admin 세션 쿠키를
    // 재사용하는 page.request로 직접 PUT 호출. UI로는 도달 불가능하므로 여기만 HYBRID PASS.
    const zodiacRes = await page.request.put(`/api/admin/applications/${applicationId}/zodiac-design`, {
      data: { zodiacDesignSet: ZODIAC_DESIGN_SET },
    });
    expect(zodiacRes.ok()).toBeTruthy();

    const { rows } = await pool.query(
      "SELECT card_number, zodiac_design_set FROM applications a JOIN application_members m ON m.application_id = a.id WHERE a.id = $1",
      [applicationId],
    );
    expect(rows[0].card_number).toBe(CARD_NUMBER);
    expect(rows[0].zodiac_design_set).toBe(ZODIAC_DESIGN_SET);
  });

  test("7. 작명 완료 처리", async ({ page }) => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");
    await page.getByRole("button", { name: applicationNumber }).click();

    await page.getByLabel("상태 변경").selectOption({ label: "작명 완료 처리" });
    await expect(page.getByText("작명 완료 처리 완료")).toBeVisible({ timeout: 10_000 });

    const { rows } = await pool.query("SELECT status FROM applications WHERE id = $1", [applicationId]);
    expect(rows[0].status).toBe("PRODUCTION_READY");
  });

  test("8. 카드 미리보기 — 디자인 선택, 발급일자 입력, PNG 응답 검증", async ({ page }) => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");
    await page.getByRole("button", { name: applicationNumber }).click();

    await page.getByRole("button", { name: "디자인 불러오기" }).click();
    const designSelect = page.locator(".admin-card-tools select");
    await expect(designSelect).toBeVisible({ timeout: 10_000 });
    const optionCount = await designSelect.locator("option").count();
    expect(optionCount).toBeGreaterThan(1); // "디자인 선택" placeholder + 실제 옵션 최소 1개
    await designSelect.selectOption({ index: 1 });

    await page.locator(".admin-card-tools__date").fill("2026-09-13");

    const [previewRes] = await Promise.all([
      page.waitForResponse((res: APIResponse) => res.url().includes("/card-preview") && res.request().method() === "POST"),
      page.getByRole("button", { name: "미리보기" }).click(),
    ]);
    expect(previewRes.ok()).toBeTruthy();
    const previewBody = await previewRes.json();
    expect(previewBody.success).toBe(true);
    const frontBuf = Buffer.from(previewBody.data.front, "base64");
    const backBuf = Buffer.from(previewBody.data.back, "base64");
    // PNG magic number: 89 50 4E 47
    expect(frontBuf.subarray(0, 4).toString("hex")).toBe("89504e47");
    expect(backBuf.subarray(0, 4).toString("hex")).toBe("89504e47");
    expect(frontBuf.length).toBeGreaterThan(1000);

    await expect(page.getByAltText("카드 앞면 미리보기")).toBeVisible({ timeout: 10_000 });
    await expect(page.getByAltText("카드 뒷면 미리보기")).toBeVisible();
    await page.screenshot({ path: "e2e-results/artifacts/card-preview.png", fullPage: true });
  });

  test("9. 카드 생성·저장 — DB/S3 확인 및 미리보기와 SHA-256 비교", async ({ page }) => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.goto("/admin");
    await page.getByRole("button", { name: applicationNumber }).click();

    await page.getByRole("button", { name: "디자인 불러오기" }).click();
    const designSelect = page.locator(".admin-card-tools select");
    await expect(designSelect).toBeVisible({ timeout: 10_000 });
    await designSelect.selectOption({ index: 1 });
    await page.locator(".admin-card-tools__date").fill("2026-09-13");

    const [previewRes] = await Promise.all([
      page.waitForResponse((res: APIResponse) => res.url().includes("/card-preview") && res.request().method() === "POST"),
      page.getByRole("button", { name: "미리보기" }).click(),
    ]);
    const previewBody = await previewRes.json();
    const previewFrontHash = createHash("sha256").update(Buffer.from(previewBody.data.front, "base64")).digest("hex");

    const [generateRes] = await Promise.all([
      page.waitForResponse((res: APIResponse) => res.url().includes("/card-generate") && res.request().method() === "POST"),
      page.getByRole("button", { name: "카드 생성" }).click(),
    ]);
    expect(generateRes.ok()).toBeTruthy();
    await expect(page.getByText("카드 이미지를 생성해 저장했습니다")).toBeVisible({ timeout: 15_000 });

    const { rows } = await pool.query(
      "SELECT a.card_design_id, a.card_issue_date, m.card_front_path, m.card_back_path FROM applications a JOIN application_members m ON m.application_id = a.id WHERE a.id = $1",
      [applicationId],
    );
    expect(rows[0].card_design_id).toBeTruthy();
    expect(rows[0].card_front_path).toBeTruthy();
    expect(rows[0].card_back_path).toBeTruthy();

    const downloadRes = await page.request.get(`/api/admin/applications/${applicationId}/members/${memberId}/cards/download`);
    expect(downloadRes.ok()).toBeTruthy();
    const downloadBody = await downloadRes.json();
    expect(downloadBody.data.cardFrontUrl).toBeTruthy();

    const frontFileRes = await page.request.get(downloadBody.data.cardFrontUrl);
    expect(frontFileRes.ok()).toBeTruthy();
    const frontFileBuf = await frontFileRes.body();
    expect(frontFileBuf.subarray(0, 4).toString("hex")).toBe("89504e47");
    const generatedHash = createHash("sha256").update(frontFileBuf).digest("hex");
    // 미리보기와 생성본은 같은 designId/issueDate/이름/카드번호로 만들었으므로 동일 렌더링이어야 한다.
    expect(generatedHash).toBe(previewFrontHash);
  });
});
