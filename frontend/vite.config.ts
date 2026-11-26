/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  base: process.env.VITE_BASE_PATH || "/",
  // The Lightsail build always uses the API and socket on the game's origin.
  define: process.env.VITE_BASE_PATH === "/ofc/" ? {
    "import.meta.env.VITE_API_BASE_URL": JSON.stringify("/ofc"),
    "import.meta.env.VITE_WS_BASE_URL": JSON.stringify(""),
  } : {},
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    exclude: ["**/node_modules/**", "**/e2e/**"],
  },
});
