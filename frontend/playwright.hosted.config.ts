import { defineConfig } from "@playwright/test";

// Run explicitly against the published build to catch origin and proxy regressions.
export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  timeout: 90_000,
  use: { baseURL: "https://games.joevig.com/ofc/", trace: "off", screenshot: "off" },
});
