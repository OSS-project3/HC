import { defineConfig } from "@playwright/test";

// Built frontend + explicit HTTP fixtures. Does not require or mutate a backend.
export default defineConfig({
  testDir: "./e2e",
  testMatch: "structure-regression.spec.ts",
  workers: 1,
  timeout: 30_000,
  reporter: "list",
  globalTeardown: "./e2e/fixtures/stop-build-server.mjs",
  outputDir: "e2e-results/ui-regression",
  use: {
    baseURL: "http://127.0.0.1:15173",
    channel: process.env.PLAYWRIGHT_CHANNEL || undefined,
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "node e2e/fixtures/serve-build.mjs",
    url: "http://127.0.0.1:15173",
    reuseExistingServer: false,
  },
});
