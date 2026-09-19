import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    server: {
      port: 5000,
      proxy: {
        // 后端 context-path 就是 /api，所以这里不做 rewrite
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8123',
          changeOrigin: true
        }
      }
    }
  }
})
