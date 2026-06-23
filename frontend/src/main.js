// Created: 2026-06-16 13:24:11
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
import './assets/base.css'
import { useAuthStore } from './stores/auth.js'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// ── 스마트 뒤로가기 ──────────────────────────────────────────────────────────
// 모든 화면이 router.back()/$router.back() 으로 뒤로 간다. 앱 내부 이전 기록이 있으면
// 평소처럼 뒤로 가지만, 딥링크·새로고침 등으로 '앱 내 이전 화면'이 없으면(history.state.back 부재)
// 앱 밖으로 나가거나 이상한 곳으로 가지 않도록 안전한 기본 화면(/home)으로 보낸다.
// router 인스턴스의 back 을 한 번만 패치 → 모든 호출부($router.back/useRouter().back)에 일괄 적용.
const _origBack = router.back.bind(router)
router.back = (fallback = '/home') => {
  if (typeof window !== 'undefined' && window.history.state && window.history.state.back) {
    _origBack()
  } else {
    router.replace(typeof fallback === 'string' ? fallback : '/home')
  }
}

// Bootstrap must complete before router starts navigation guards,
// otherwise a logged-in user can get bounced to /login on hard reload.
const auth = useAuthStore()
auth.bootstrap().then(() => {
  app.use(router)
  app.mount('#app')
})
