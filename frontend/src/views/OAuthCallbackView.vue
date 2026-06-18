<template>
  <div class="page">
    <div class="spinner-wrap">
      <div class="logo-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="white" stroke="none">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
        </svg>
      </div>
      <div class="spinner" />
      <p class="msg">{{ statusMsg }}</p>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const statusMsg = ref('로그인 처리 중이에요…')
const errorMsg = ref('')

onMounted(async () => {
  try {
    // Spring OAuth2 sets the refresh cookie on redirect — call refresh to get accessToken
    await auth.refresh()
    await auth.fetchMe()
    // Honor stored redirect (sessionStorage from OAuth login) or query param, else go home
    const stored = sessionStorage.getItem('oauth_redirect')
    sessionStorage.removeItem('oauth_redirect')
    const redirect = stored || route.query.redirect
    router.replace(redirect && typeof redirect === 'string' ? redirect : '/home')
  } catch {
    errorMsg.value = '로그인에 실패했어요. 다시 시도해 주세요.'
    statusMsg.value = '오류가 발생했어요'
    setTimeout(() => {
      router.replace('/login')
    }, 2000)
  }
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: var(--color-white);
}

.spinner-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(247, 143, 87, 0.35);
  margin-bottom: 4px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-line);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.msg {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.error-msg {
  font-size: 13px;
  color: var(--color-error);
  text-align: center;
  padding: 0 24px;
}
</style>
