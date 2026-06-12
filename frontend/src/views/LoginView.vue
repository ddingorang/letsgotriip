<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth.js';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const email = ref('');
const password = ref('');
const errorMsg = ref('');
const loading = ref(false);

async function handleLogin() {
  errorMsg.value = '';
  if (!email.value || !password.value) {
    errorMsg.value = '이메일과 비밀번호를 입력해 주세요.';
    return;
  }
  loading.value = true;
  try {
    await auth.login(email.value, password.value);
    const redirect = route.query.redirect;
    router.push(redirect && typeof redirect === 'string' ? redirect : '/');
  } catch (err) {
    const code = err.response?.data?.code;
    if (code === 'USER401' || code === 'USER404') {
      errorMsg.value = '이메일 또는 비밀번호가 올바르지 않아요.';
    } else {
      errorMsg.value = '로그인 중 오류가 발생했어요. 다시 시도해 주세요.';
    }
  } finally {
    loading.value = false;
  }
}

function handleGoogle() {
  window.location.href = '/oauth2/authorization/google';
}
</script>

<template>
  <div class="login-view">
    <!-- Logo area -->
    <div class="logo-area">
      <div class="logo-text">관통여행</div>
      <div class="logo-sub">AI가 만들어주는 나만의 여행 일정</div>
    </div>

    <!-- Form card -->
    <div class="form-card">
      <div class="form-title">로그인</div>

      <div class="field">
        <label class="field-label">이메일</label>
        <input
          v-model="email"
          type="email"
          class="field-input"
          placeholder="이메일을 입력해 주세요"
          autocomplete="email"
          @keyup.enter="handleLogin"
        />
      </div>

      <div class="field">
        <label class="field-label">비밀번호</label>
        <input
          v-model="password"
          type="password"
          class="field-input"
          placeholder="비밀번호를 입력해 주세요"
          autocomplete="current-password"
          @keyup.enter="handleLogin"
        />
      </div>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <button class="btn-primary" :disabled="loading" @click="handleLogin">
        <span v-if="loading" class="spinner"></span>
        <span v-else>로그인</span>
      </button>

      <div class="divider"><span>또는</span></div>

      <button class="btn-google" @click="handleGoogle">
        <svg width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
          <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
          <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
          <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
        </svg>
        Google로 계속하기
      </button>

      <p class="signup-link">
        아직 계정이 없으신가요?
        <router-link to="/signup">가입하기</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  min-height: 100%;
  background: var(--surface-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 20px 40px;
}

.logo-area {
  text-align: center;
  margin-bottom: 32px;
}
.logo-text {
  font: 800 32px/1 var(--font-sans);
  color: var(--color-primary-500);
  letter-spacing: -0.03em;
  margin-bottom: 8px;
}
.logo-sub {
  font: var(--type-body-sm);
  color: var(--text-secondary);
}

.form-card {
  width: 100%;
  max-width: 390px;
  background: var(--surface-bg);
  border-radius: var(--radius-xl);
  padding: 28px 24px 32px;
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 0;
}

.form-title {
  font: var(--weight-bold) var(--text-xl)/1 var(--font-sans);
  color: var(--text-primary);
  margin-bottom: 24px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}
.field-label {
  font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans);
  color: var(--text-secondary);
}
.field-input {
  height: 50px;
  border: 1.5px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: 0 14px;
  font: var(--weight-regular) var(--text-base)/1 var(--font-sans);
  color: var(--text-primary);
  background: var(--surface-bg);
  outline: none;
  transition: border-color 0.15s;
}
.field-input::placeholder {
  color: var(--text-tertiary);
}
.field-input:focus {
  border-color: var(--color-primary-400);
}

.error-msg {
  font: var(--weight-medium) var(--text-sm)/1.4 var(--font-sans);
  color: var(--color-error);
  margin: 0 0 12px;
}

.btn-primary {
  width: 100%;
  height: 52px;
  background: var(--color-primary-500);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font: var(--weight-bold) var(--text-base)/1 var(--font-sans);
  cursor: pointer;
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: background 0.15s;
}
.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-600);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255,255,255,0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 20px 0;
  color: var(--text-tertiary);
  font: var(--weight-medium) var(--text-xs)/1 var(--font-sans);
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border-subtle);
}

.btn-google {
  width: 100%;
  height: 52px;
  background: var(--surface-bg);
  border: 1.5px solid var(--border-default);
  border-radius: var(--radius-md);
  font: var(--weight-semibold) var(--text-base)/1 var(--font-sans);
  color: var(--text-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: background 0.15s, border-color 0.15s;
}
.btn-google:hover {
  background: var(--surface-subtle);
  border-color: var(--border-strong);
}

.signup-link {
  text-align: center;
  font: var(--weight-medium) var(--text-sm)/1 var(--font-sans);
  color: var(--text-secondary);
  margin-top: 20px;
  margin-bottom: 0;
}
.signup-link a {
  color: var(--color-primary-500);
  font-weight: var(--weight-semibold);
  text-decoration: none;
}
.signup-link a:hover {
  text-decoration: underline;
}
</style>
