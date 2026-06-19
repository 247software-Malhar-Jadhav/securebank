/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// Vite configuration for the SecureBank frontend.
//
// Why these choices:
// - The `@` alias lets us write `import { Button } from "@/components/ui/button"`
//   instead of brittle relative paths like `../../../components/ui/button`.
// - The dev server runs on 5173 (fixed by the project spec).
// - During local development the browser talks to Vite, and any request that
//   starts with `/api` is proxied to the Spring Boot backend on :8080. This means
//   the frontend code can always call `/api/...` with no CORS headaches and no
//   environment-specific base URLs — in production nginx does the same proxying.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        // We do NOT rewrite the path: the backend already serves under /api
        // (context path /api per the spec), so /api/accounts -> :8080/api/accounts.
      },
    },
  },
  // Vitest configuration lives here so we have a single source of truth.
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    css: true,
  },
});
