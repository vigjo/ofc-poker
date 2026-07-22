import { defineConfig, devices } from "@playwright/test";

const PORT = 4173;

/**
 * End-to-end coverage against a real backend + DynamoDB Local, exercising
 * the full stack the way a player actually would: create/join a room over
 * HTTP, then drive the game entirely through the rendered UI. See
 * e2e/README.md for the services this expects to already be running.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "npm run preview -- --port 4173",
    url: `http://localhost:${PORT}`,
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
});
