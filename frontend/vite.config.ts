import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// https://vite.dev/config/
// Dev server proxies API/OAuth calls to the local backend (default localhost:8080).
// Prod uses nginx (frontend/nginx.conf) for the same routes; dev has no nginx so
// without this every `/api` call hits the vite server and 404s.
const BACKEND = process.env.VITE_DEV_BACKEND ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    proxy: {
      "/api": { target: BACKEND, changeOrigin: true },
      "/oauth2": { target: BACKEND, changeOrigin: true },
      "/login/oauth2": { target: BACKEND, changeOrigin: true },
    },
  },
});
