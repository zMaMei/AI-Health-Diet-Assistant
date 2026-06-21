/* Vite构建配置 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  /* 注册Vue插件 */
  plugins: [vue()],
  server: {
    port: 5173,
    /* 开发服务器API代理（转发到Spring Boot后端） */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
