<template>
  <div class="page">
    <!-- Hero photo area -->
    <div class="hero-section">
      <img class="hero-img" src="/loginpic.jpg" alt="" />
      <div class="hero-grain" />

      <!-- Logo card overlay -->
      <div class="logo-card">
        <div class="logo-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="white" stroke="none">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
          </svg>
        </div>
      </div>
    </div>

    <!-- Form section -->
    <div class="form-section">
      <div v-if="isRedirected" class="auth-notice">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        로그인이 필요한 서비스예요.
      </div>
      <div v-if="isResetDone" class="auth-notice success">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 6L9 17l-5-5" />
        </svg>
        비밀번호가 변경되었어요. 새 비밀번호로 로그인해 주세요.
      </div>
      <h2 class="form-title">다시 만나서 반가워요</h2>
      <p class="form-sub">로그인하고 나만의 여행을 계획해보세요.</p>

      <div class="fields">
        <!-- Email -->
        <div class="field-wrap">
          <label class="field-label">이메일</label>
          <div class="input-wrap">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" />
            </svg>
            <input
              v-model="email"
              type="email"
              class="field-input"
              placeholder="user@email.com"
              autocomplete="email"
              @keydown.enter="focusPassword"
            />
          </div>
        </div>

        <!-- Password -->
        <div class="field-wrap">
          <label class="field-label">비밀번호</label>
          <div class="input-wrap">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" />
            </svg>
            <input
              ref="passwordRef"
              v-model="password"
              type="password"
              class="field-input"
              placeholder="••••••••"
              autocomplete="current-password"
              @keydown.enter="submitLogin"
            />
          </div>
        </div>
      </div>

      <div v-if="error" class="error-msg">{{ error }}</div>

      <!-- Login button -->
      <button class="login-btn" :disabled="loading" @click="submitLogin">
        <span v-if="!loading">로그인</span>
        <div v-else class="btn-spinner" />
      </button>

      <!-- Forgot password -->
      <button class="forgot-link" @click="goPasswordReset">비밀번호를 잊으셨나요?</button>

      <!-- Divider -->
      <div class="divider-row">
        <div class="divider" />
        <span class="divider-text">또는</span>
        <div class="divider" />
      </div>

      <!-- Social buttons -->
      <div class="social-col">
        <!-- Kakao -->
        <button class="social-btn kakao" @click="handleKakao">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="#3C1E1E" stroke="none">
            <path d="M12 3C6.477 3 2 6.477 2 10.917c0 2.817 1.728 5.291 4.337 6.757l-1.1 4.073c-.097.361.296.65.612.44L10.5 19.5c.492.058.993.083 1.5.083 5.523 0 10-3.478 10-7.666C22 6.477 17.523 3 12 3z" />
          </svg>
          카카오로 계속하기
        </button>

        <!-- Google -->
        <button class="social-btn google" @click="handleGoogle">
          <svg width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
          </svg>
          Google로 계속하기
        </button>
      </div>

      <!-- Sign up link -->
      <p class="signup-link">
        아직 회원이 아니신가요?
        <button class="signup-text" @click="goSignup">회원가입</button>
      </p>

      <!-- Guest link -->
      <button class="guest-link" @click="$router.push('/home')">비로그인 상태로 둘러보기</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 데모 편의: VITE_DEMO_EMAIL/PASSWORD 가 비어있지 않으면 로그인 폼을 미리 채움(운영은 빈칸)
const email = ref(import.meta.env.VITE_DEMO_EMAIL || '')
const password = ref(import.meta.env.VITE_DEMO_PASSWORD || '')
const loading = ref(false)
const error = ref('')
const passwordRef = ref(null)
const isRedirected = !!route.query.redirect
const isResetDone = route.query.reset === 'done'

function focusPassword() {
  passwordRef.value?.focus()
}

async function submitLogin() {
  if (!email.value || !password.value) {
    error.value = '이메일과 비밀번호를 입력해주세요.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await authStore.login(email.value, password.value)
    const redirect = route.query.redirect
    router.push(redirect && typeof redirect === 'string' ? redirect : '/home')
  } catch (err) {
    const code = err.response?.data?.code
    if (code === 'USER401' || code === 'USER404') {
      error.value = '이메일 또는 비밀번호가 올바르지 않아요.'
    } else {
      error.value = '로그인 중 오류가 발생했어요. 다시 시도해 주세요.'
    }
  } finally {
    loading.value = false
  }
}

function handleKakao() {
  const redirect = route.query.redirect
  if (redirect && typeof redirect === 'string') {
    sessionStorage.setItem('oauth_redirect', redirect)
  }
  window.location.href = '/oauth2/authorization/kakao'
}

function handleGoogle() {
  const redirect = route.query.redirect
  if (redirect && typeof redirect === 'string') {
    sessionStorage.setItem('oauth_redirect', redirect)
  }
  window.location.href = '/oauth2/authorization/google'
}

function goSignup() {
  router.push('/signup')
}

function goPasswordReset() {
  router.push('/password-reset')
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}

/* Hero */
.hero-section {
  position: relative;
  height: 220px;
  flex-shrink: 0;
}
.hero-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hero-grain {
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.12'/%3E%3C/svg%3E");
  opacity: 0.4;
}
.logo-card {
  position: absolute;
  bottom: -24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 10;
}
.logo-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(247, 143, 87, 0.4);
}

.auth-notice {
  display: flex;
  align-items: center;
  gap: 7px;
  background: var(--color-peach-light, #fff0e8);
  color: var(--color-peach-pressed, #d45f1e);
  font-size: 13px;
  font-weight: 500;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  letter-spacing: -0.2px;
}
.auth-notice.success {
  background: var(--color-success-light, #e7f6ec);
  color: var(--color-success, #1f9d55);
}

/* Form */
.form-section {
  flex: 1;
  overflow-y: auto;
  padding: 48px 24px 32px;
}
.form-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.6px;
  margin-bottom: 6px;
}
.form-sub {
  font-size: 14px;
  color: var(--color-ink-muted);
  margin-bottom: 28px;
  letter-spacing: -0.2px;
}

.fields { display: flex; flex-direction: column; gap: 16px; margin-bottom: 4px; }
.field-wrap { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 600; color: var(--color-ink-secondary); }

.input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 13px 16px;
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.input-wrap:focus-within { border-color: var(--color-peach); }
.field-input {
  flex: 1;
  font-size: 14.5px;
  color: var(--color-ink);
  background: transparent;
}
.field-input::placeholder { color: var(--color-ink-muted); }

.error-msg {
  font-size: 13px;
  color: var(--color-error);
  margin: 8px 0 12px;
}

.login-btn {
  width: 100%;
  padding: 16px;
  background: var(--color-peach);
  color: white;
  font-size: 15.5px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  letter-spacing: -0.3px;
  transition: background 0.15s;
}
.login-btn:hover:not(:disabled) { background: var(--color-peach-pressed); }
.login-btn:disabled { opacity: 0.6; }

.forgot-link {
  display: block;
  width: 100%;
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-muted);
  text-decoration: underline;
  text-underline-offset: 3px;
}
.btn-spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255,255,255,0.4);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.divider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 22px 0 16px;
}
.divider { flex: 1; height: 1px; background: var(--color-line-light); }
.divider-text { font-size: 12.5px; color: var(--color-ink-muted); white-space: nowrap; }

.social-col { display: flex; flex-direction: column; gap: 10px; }
.social-btn {
  width: 100%;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: opacity 0.15s;
}
.social-btn:active { opacity: 0.8; }
.social-btn.kakao { background: #FEE500; color: #3C1E1E; }
.social-btn.google { background: var(--color-white); color: var(--color-ink); border: 1.5px solid var(--color-line); }

.signup-link {
  text-align: center;
  margin-top: 20px;
  font-size: 13.5px;
  color: var(--color-ink-muted);
}
.signup-text {
  color: var(--color-peach-pressed);
  font-weight: 700;
  margin-left: 4px;
}

.guest-link {
  display: block;
  width: 100%;
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--color-ink-muted);
  text-decoration: underline;
  text-underline-offset: 3px;
}
</style>
