import { defineConfig } from "@playwright/test";

// E2E(계약 8-1 E01부터 E06). 백엔드와 DB가 떠 있을 때만 돈다(계약 9절).
// 프론트는 E2E_BASE_URL(기본 http://localhost:3000)에 떠 있어야 한다. 백엔드는 그 프론트의 rewrites가 가리키는 곳이고
// D-4와 D-7대로 o2o_web_test에 붙고 hold-ttl이 짧아야 한다(E04는 hold-ttl 20초를 전제한다).
//
// 실행 예시
//   npm run test:e2e
// 실패 시 출력 예시
//   Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:3000/
//     프론트가 안 떠 있다. next build 뒤 next start -p 3000
export default defineConfig({
  testDir: "./e2e",
  timeout: 90_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["html", { open: "never", outputFolder: "playwright-report" }]],
  outputDir: "test-results",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    viewport: { width: 1300, height: 900 },
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { browserName: "chromium" } }],
});
