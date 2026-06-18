// Created: 2026-06-16 13:36:03
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
      // '/api/tour' → 한국관광공사(TourAPI)
      '/api/tour': {
        target: 'https://apis.data.go.kr',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/tour/, ''),
      },
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
