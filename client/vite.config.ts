import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig, loadEnv } from "vite"

import { tanstackRouter } from "@tanstack/router-plugin/vite"

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "")
  const backendUrl =
    env.VITE_BACKEND_URL || env.BACKEND_URL || "http://localhost:8080"
  const port = Number(env.PORT || env.VITE_PORT || 3000)

  return {
    plugins: [react(), tailwindcss(), tanstackRouter()],
    resolve: {
      alias: {
        "@": path.resolve(import.meta.dirname, "./src"),
      },
    },
    server: {
      port,
      proxy: {
        "/api": {
          target: backendUrl,
          changeOrigin: true,
        },
      },
    },
  }
})
