// Created: 2026-06-16 13:36:03
//
// ── Prefix-proxy 구조 (dev only) ────────────────────────────────────────────────
// 개발 서버(5173)에서만 동작하는 프록시 규칙. axios baseURL은 ''(http.js) 이라
// FE 코드는 모든 백엔드 호출을 /api·/auth·/users 등 상대경로로 던지고, 여기서
// 대상 origin(BACKEND :9090, TourAPI)으로 라우팅한다.
//
// 규칙 순서 주의(더 구체적인 규칙이 먼저 와야 함):
//   /api/community  → BACKEND, '/api' prefix만 제거 (BE는 /community 로 서빙)
//   /api/companion  → BACKEND, '/api' prefix만 제거 (BE는 /companion 로 서빙)
//   /api            → BACKEND (prefix 유지; /api/attractions·/api/festivals·/api/plans·/api/recommendations·/api/chat)
//   /auth /users /uploads /oauth2 /login/oauth2 → BACKEND
// community·companion 을 /api/* 로 감싸 호출하는 이유: SPA 라우트(/community,
// /companion)와 경로가 겹쳐, 프록시가 SPA 네비게이션을 가로채는 것을 막기 위함.
// (축제는 더 이상 TourAPI를 직접 호출하지 않고 BE /api/festivals 를 경유한다.)
//
// ── 프로덕션 요구사항 (중요) ─────────────────────────────────────────────────────
// 빌드 결과물에는 이 dev 프록시가 포함되지 않는다. 운영에서는:
//   1) frontend/.env.production 의 VITE_API_BASE_URL 을 BE(또는 리버스 프록시)
//      origin 으로 설정한다.
//   2) 그 리버스 프록시(Nginx 등)가 위 prefix rewrite 를 그대로 미러링해야 한다.
//      특히 /api/community → /community, /api/companion → /companion rewrite 를
//      동일하게 구성하지 않으면 운영에서 404 발생.
//   3) WebSocket(/ws, STOMP)은 dev에서 클라이언트가 BE(:9090)로 직접 연결한다.
//      운영에서는 same-origin 또는 리버스 프록시의 WebSocket 업그레이드 설정이 필요하다.
// ────────────────────────────────────────────────────────────────────────────────
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const BACKEND = 'http://localhost:9090'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Order matters: more specific rules first.
      // BE serves community/companion at /community·/companion (no /api prefix),
      // but the SPA uses those same paths as page routes. So the FE calls them
      // under /api/* and we strip /api here — this prevents the proxy from
      // hijacking SPA navigations to /community or /companion.
      '/api/community': {
        target: BACKEND,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/api/companion': {
        target: BACKEND,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/api': { target: BACKEND, changeOrigin: true },
      '/auth': { target: BACKEND, changeOrigin: true },
      '/users': { target: BACKEND, changeOrigin: true },
      '/uploads': { target: BACKEND, changeOrigin: true },
      '/oauth2': { target: BACKEND, changeOrigin: true },
      '/login/oauth2': { target: BACKEND, changeOrigin: true },
    },
  },
})
