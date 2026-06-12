import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const BACKEND = 'http://localhost:9090';

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': { target: BACKEND, changeOrigin: true },
      '/auth': { target: BACKEND, changeOrigin: true },
      '/users': { target: BACKEND, changeOrigin: true },
      '/oauth2': { target: BACKEND, changeOrigin: true },
      // 주의: '/login' 전체를 프록시하면 SPA의 /login 라우트 직접 진입이 백엔드로 가버린다.
      // OAuth 인가 코드 콜백 경로만 프록시한다.
      '/login/oauth2': { target: BACKEND, changeOrigin: true },
    },
  },
});
