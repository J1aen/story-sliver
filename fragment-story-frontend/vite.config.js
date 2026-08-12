import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true, // 允许局域网访问：手机连同一 WiFi 也能打开
    proxy: {
      // 开发时把 /api 开头的请求转发给后端 8080（上线后由后端托管，不需要这个代理）
      '/api': 'http://localhost:8080',
      // 头像图片也在后端（/uploads/** 映射到本地 D:/HeadImage）
      '/uploads': 'http://localhost:8080'
    }
  }
})
