import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const backend = {
  target: process.env.AGENT_ENGINE_BACKEND_URL ?? "https://localhost:8443",
  secure: false,
};

export default defineConfig({
  plugins: [vue()],
  build: { outDir: "../../../target/frontend", emptyOutDir: true },
  server: {
    proxy: {
      "/web": backend,
      "/api": backend,
      "/login": backend,
      "/logout": backend,
    },
  },
});
