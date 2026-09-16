// E2E 전용 Playwright 설정 — 격리 스택(docker-compose.e2e.yml)을 대상으로 한다.
// 앱 코드가 아니라 테스트 인프라(사용자 승인 완료, docs/collab/e2e_test.md 검증용).
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  testIgnore: "structure-regression.spec.ts", // isolated HTTP fixtures use playwright.ui.config.ts
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false, // 신청 데이터가 상태를 공유하므로 순차 실행
  retries: 0,
  workers: 1,
  reporter: [
    ["html", { outputFolder: "e2e-results/html-report", open: "never" }],
    ["list"],
  ],
  outputDir: "e2e-results/artifacts",
  use: {
    baseURL: "http://localhost:13000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],
});
