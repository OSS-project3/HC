import { expect, test, type Page } from "@playwright/test";
import { createEmptyDraft } from "../src/features/apply/types";

// Test-only HTTP fixtures verify UI composition and request wiring, not backend behavior.
const application = { applicationId: 101, applicationNumber: "REGRESSION-101", applicationType: "INDIVIDUAL", cardTypeId: 2, cardTypeName: "명예시민증", status: "NAME_EDITING", paymentStatus: "CONFIRMED", issueType: "MOBILE", memberCount: 1, quantity: 1, createdAt: "2026-09-15T10:00:00", applicant: { name: "Regression User", phone: "010-1234-5678", email: "regression@example.test" } };
const member = { memberId: 201, englishName: "Regression User", birthDate: "1995-06-15", birthTime: "14:30", birthRegion: "Seoul", gender: "MALE", nationality: "KR", surname: "김" };
const result = { confirmedPillars: { year: { stem: "을", branch: "해" }, month: { stem: "임", branch: "오" }, day: { stem: "정", branch: "축" }, hour: { stem: "정", branch: "미" } }, uncertainPillars: [], elementCounts: { 목: 1, 화: 3, 토: 2, 금: 0, 수: 2 }, timeAccuracy: "EXACT" };
const paged = (content: unknown[], page = 0) => ({ content, totalPages: 2, totalElements: 2, number: page, size: 1 });
const pixel = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=";

async function fixtureApi(page: Page) {
  const calls: string[] = [];
  await page.route("**/api/**", async route => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    calls.push(`${route.request().method()} ${path}${url.search}`);
    let data: unknown = undefined;
    if (path === "/api/users/me" || path === "/api/auth/login") data = { id: 1, name: "Regression User", email: "regression@example.test", role: "ADMIN" };
    else if (path === "/api/admin/stats") data = { applicationCount: 1, reviewCount: 0, inquiryCount: 0 };
    else if (path === "/api/admin/applications" || path === "/api/my/applications") data = paged([application], Number(url.searchParams.get("page")));
    else if (path === "/api/admin/applications/101") data = application;
    else if (path.endsWith("/manseryeok-results")) data = [{ memberId: 201, result }];
    else if (path.endsWith("/members")) data = [member];
    else if (path.includes("name-selection-stats")) data = [];
    else if (path.includes("birth-region")) data = [{ displayName: "Seoul", latitude: 37.56, longitude: 126.97 }];
    else if (path.endsWith("/resolve")) data = { status: "EXACT", timezoneId: "Asia/Seoul", longitude: 126.97, utcInstant: "1995-06-15T05:30:00Z" };
    else if (path.includes("card-designs")) data = [{ id: 1, name: "Regression design", designNumber: 1, isDefault: true }];
    else if (path.endsWith("/card-preview")) data = { front: pixel, back: pixel };
    else if (path.endsWith("/download")) data = { cardFrontUrl: `data:image/png;base64,${pixel}`, cardBackUrl: `data:image/png;base64,${pixel}` };
    else if (path === "/api/schools/search") data = [{ id: 1, name: "회귀대학교", schoolType: "UNIVERSITY" }];
    else if (path.includes("inquiries")) data = [];
    else if (/boards|reviews|events/.test(path)) data = paged([]);
    else if (route.request().method() !== "GET") data = { status: "PRODUCTION_READY" };
    else throw new Error(`Unhandled fixture request: ${path}`);
    await route.fulfill({ json: { success: true, data, errorCode: null, errorMessage: null } });
  });
  return calls;
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => { localStorage.setItem("site-language", "ko"); });
});

test("public routes, language, notices and my-page mount without runtime errors", async ({ page }, testInfo) => {
  const errors: string[] = [];
  page.on("pageerror", e => errors.push(e.message));
  const calls = await fixtureApi(page);
  for (const path of ["/", "/company", "/reviews", "/events", "/notices", "/mypage"]) {
    await page.goto(path);
    await expect(page.locator("main")).toBeVisible();
    await expect(page.locator("main")).not.toBeEmpty();
  }
  await page.locator(".lang__toggle").click();
  await page.getByRole("button", { name: "English", exact: true }).click();
  await expect(page.locator(".lang__toggle")).toContainText("English");
  expect(calls.some(c => c.includes("/api/boards"))).toBeTruthy();
  expect(calls.some(c => c.includes("/api/my/applications"))).toBeTruthy();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await page.screenshot({ path: testInfo.outputPath("home-mobile.png"), fullPage: true });
  expect(errors).toEqual([]);
});

test("individual form preserves validation, recipient identity and draft across steps", async ({ page }) => {
  await fixtureApi(page);
  const draft = createEmptyDraft();
  draft.consultationConfirmed = true; draft.disclaimerConfirmed = true;
  await page.addInitScript(d => sessionStorage.setItem("application-draft", JSON.stringify(d)), draft);
  await page.goto("/apply/honorary-citizen");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.getByRole("heading", { name: "정보 입력", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.locator(".field__input--invalid").first()).toBeVisible();
  await page.getByPlaceholder("010-1234-5678").fill("010-1234-5678");
  await page.getByRole("radio", { name: "모바일 + 실물 발급", exact: true }).check();
  await page.getByRole("checkbox", { name: "신청인과 동일합니다" }).check();
  const recipient = page.locator(".info-col").last();
  await expect(recipient.getByPlaceholder("010-1234-5678")).toHaveValue("010-1234-5678");
  await page.getByRole("radio", { name: "모바일 발급", exact: true }).check();
  await expect(page.getByRole("heading", { name: "수령인 정보" })).toHaveCount(0);
  await page.getByRole("button", { name: "이전", exact: true }).click();
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.getByPlaceholder("010-1234-5678")).toHaveValue("010-1234-5678");
});

test("student school search and manual entry preserve required university fields", async ({ page }) => {
  const calls = await fixtureApi(page);
  const draft = createEmptyDraft(); draft.consultationConfirmed = true; draft.disclaimerConfirmed = true;
  await page.addInitScript(d => sessionStorage.setItem("application-draft", JSON.stringify(d)), draft);
  await page.goto("/apply/student");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await page.getByRole("button", { name: "학교 선택", exact: true }).click();
  await page.locator(".select-field.is-open input").fill("회귀");
  await page.getByRole("option", { name: "회귀대학교" }).click();
  await expect(page.getByPlaceholder("20260001")).toBeVisible();
  await page.getByRole("button", { name: "찾는 학교가 없나요? 직접 입력" }).click();
  await page.getByRole("radio", { name: "고등학교", exact: true }).check();
  await expect(page.getByPlaceholder("20260001")).toHaveCount(0);
  expect(calls.some(c => c.includes("/api/schools/search?query="))).toBeTruthy();
});

test("group applicant and physical recipient remain valid when advancing to files", async ({ page }) => {
  await fixtureApi(page);
  const draft = createEmptyDraft();
  draft.applicantType = "organization";
  draft.issuanceMethod = "mobile_and_physical";
  draft.consultationConfirmed = true; draft.disclaimerConfirmed = true;
  Object.assign(draft.applicant, { name: "단체 담당자", organizationName: "회귀 단체", phone: "010-1234-5678", email: "group@example.test" });
  Object.assign(draft.recipient, { name: "수령인", phone: "010-1234-5678", postalCode: "03000", address: "서울시", addressDetail: "101호" });
  await page.addInitScript(d => sessionStorage.setItem("application-draft", JSON.stringify(d)), draft);
  await page.goto("/apply/honorary-citizen");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.getByPlaceholder("담당자 이름")).toHaveValue("단체 담당자");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page.locator(".step__heading")).not.toHaveText("정보 입력");
  await page.getByRole("button", { name: "이전", exact: true }).click();
  await expect(page.getByPlaceholder("담당자 이름")).toHaveValue("단체 담당자");
});

test("admin login, detail restoration, naming, card panels and pagination stay connected", async ({ page }, testInfo) => {
  const errors: string[] = []; page.on("pageerror", e => errors.push(e.message));
  const calls = await fixtureApi(page);
  await page.goto("/login");
  await page.getByPlaceholder("you@example.com").fill("regression@example.test");
  await page.getByPlaceholder("비밀번호", { exact: true }).fill("test-only-password");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(page).not.toHaveURL(/\/login$/);
  await page.goto("/admin");
  await page.getByRole("button", { name: "제작신청 관리", exact: true }).click();
  await page.getByRole("button", { name: /REGRESSION-101/ }).click();
  await expect(page.locator(".admin-naming__info")).toBeVisible();
  await expect(page.locator(".admin-naming__rec")).toHaveCount(5);
  expect(calls.filter(c => c.includes("/manseryeok-results"))).toHaveLength(1);
  await page.locator(".admin-naming__rec").first().getByRole("button", { name: "이 이름 선택" }).click();
  await expect.poll(() => calls.some(c => c.startsWith("POST ") && c.endsWith("/name"))).toBeTruthy();
  await page.getByRole("button", { name: "디자인 불러오기", exact: true }).click();
  await page.getByRole("button", { name: "미리보기", exact: true }).click();
  await expect(page.getByAltText("카드 앞면 미리보기")).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("admin-detail.png"), fullPage: true });
  await page.getByRole("button", { name: "카드 생성", exact: true }).click();
  await expect.poll(() => calls.some(c => c.includes("/card-generate"))).toBeTruthy();
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await page.getByRole("button", { name: "만세력 확정", exact: true }).click();
  await expect.poll(() => calls.filter(c => c.includes("/manseryeok-results")).length).toBe(2);
  await page.locator(".admin-pager").getByRole("button", { name: /다음/ }).click();
  await expect.poll(() => calls.some(c => c.includes("page=1"))).toBeTruthy();
  expect(errors).toEqual([]);
});
