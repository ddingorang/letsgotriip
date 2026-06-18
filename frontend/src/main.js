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

// Bootstrap must complete before router starts navigation guards,
// otherwise a logged-in user can get bounced to /login on hard reload.
const auth = useAuthStore()
auth.bootstrap().then(() => {
  app.use(router)
  app.mount('#app')
})
