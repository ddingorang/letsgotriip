<template>
  <div class="page">
    <!-- Header -->
    <div class="topbar">
      <button class="back-btn" @click="goBack" aria-label="뒤로">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <span class="topbar-title">비밀번호 재설정</span>
      <span class="topbar-spacer" />
    </div>

    <!-- Form section -->
    <div class="form-section">
      <!-- Step indicator -->
      <div class="steps">
        <div class="step" :class="{ active: step >= 1 }">
          <span class="step-dot">1</span>
          <span class="step-label">이메일 확인</span>
        </div>
        <div class="step-line" :class="{ active: step >= 2 }" />
        <div class="step" :class="{ active: step >= 2 }">
          <span class="step-dot">2</span>
          <span class="step-label">새 비밀번호</span>
        </div>
      </div>

      <!-- ── Step 1: 이메일 입력 ── -->
      <template v-if="step === 1">
        <h2 class="form-title">비밀번호를 잊으셨나요?</h2>
        <p class="form-sub">가입하신 이메일을 입력하시면 재설정 토큰을 보내드려요.</p>

        <div class="fields">
          <div class="field-wrap">
            <label class="field-label">이메일</label>
            <div class="input-wrap">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="4" width="20" height="16" rx="2" /><path d="M22 6l-10 7L2 6" />
              </svg>
              <input
                v-model="email"
                type="email"
                class="field-input"
                placeholder="user@email.com"
                autocomplete="email"
                @keydown.enter="submitRequest"
              />
            </div>
          </div>
        </div>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <button class="primary-btn" :disabled="loading" @click="submitRequest">
          <span v-if="!loading">재설정 토큰 받기</span>
          <div v-else class="btn-spinner" />
        </button>
      </template>

      <!-- ── Step 2: 토큰 + 새 비밀번호 ── -->
      <template v-else>
        <h2 class="form-title">새 비밀번호 설정</h2>
        <p class="form-sub">발급된 토큰과 사용하실 새 비밀번호를 입력해주세요.</p>

        <!-- 데모 안내: 발급된 토큰 노출 -->
        <div v-if="resetToken" class="demo-notice">
          <div class="demo-notice-head">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            데모 환경 안내
          </div>
          <p class="demo-notice-text">
            실제 서비스에서는 이메일로 토큰이 전송돼요. 데모이므로 아래 토큰을 그대로 사용하세요.
          </p>
          <button class="token-chip" @click="copyToken">
            <code class="token-code">{{ resetToken }}</code>
            <span class="token-copy">{{ copied ? '복사됨' : '복사' }}</span>
          </button>
        </div>

        <div class="fields">
          <div class="field-wrap">
            <label class="field-label">재설정 토큰</label>
            <div class="input-wrap">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.778 7.778 5.5 5.5 0 017.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4" />
              </svg>
              <input
                v-model="token"
                type="text"
                class="field-input"
                placeholder="발급받은 토큰"
                autocomplete="off"
              />
            </div>
          </div>

          <div class="field-wrap">
            <label class="field-label">새 비밀번호</label>
            <div class="input-wrap">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" />
              </svg>
              <input
                v-model="newPassword"
                type="password"
                class="field-input"
                placeholder="••••••••"
                autocomplete="new-password"
                @keydown.enter="submitReset"
              />
            </div>
          </div>

          <div class="field-wrap">
            <label class="field-label">새 비밀번호 확인</label>
            <div class="input-wrap">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" />
              </svg>
              <input
                v-model="confirmPassword"
                type="password"
                class="field-input"
                placeholder="••••••••"
                autocomplete="new-password"
                @keydown.enter="submitReset"
              />
            </div>
          </div>
        </div>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <button class="primary-btn" :disabled="loading" @click="submitReset">
          <span v-if="!loading">비밀번호 변경하기</span>
          <div v-else class="btn-spinner" />
        </button>

        <button class="ghost-btn" @click="backToStep1">이메일 다시 입력</button>
      </template>

      <!-- 로그인으로 돌아가기 -->
      <p class="login-link">
        비밀번호가 기억나셨나요?
        <button class="login-text" @click="goLogin">로그인</button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/index.js'

const router = useRouter()

const step = ref(1)
const email = ref('')
const token = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const resetToken = ref('')
const loading = ref(false)
const error = ref('')
const copied = ref(false)

async function submitRequest() {
  if (!email.value) {
    error.value = '이메일을 입력해주세요.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await authApi.requestPasswordReset({ email: email.value })
    // 데모: 응답으로 받은 토큰을 화면에 노출하고 2단계로 이동
    const data = res?.data ?? res
    resetToken.value = data?.token || ''
    token.value = resetToken.value
    step.value = 2
  } catch (err) {
    const code = err.response?.data?.code
    if (code === 'USER404') {
      error.value = '가입되지 않은 이메일이에요.'
    } else {
      error.value = '요청 중 오류가 발생했어요. 다시 시도해 주세요.'
    }
  } finally {
    loading.value = false
  }
}

async function submitReset() {
  if (!token.value) {
    error.value = '재설정 토큰을 입력해주세요.'
    return
  }
  if (!newPassword.value || newPassword.value.length < 8) {
    error.value = '비밀번호는 8자 이상으로 입력해주세요.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = '비밀번호가 일치하지 않아요.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await authApi.resetPassword({ token: token.value, newPassword: newPassword.value })
    router.push({ path: '/login', query: { reset: 'done' } })
  } catch (err) {
    const code = err.response?.data?.code
    if (code === 'USER404' || code === 'USER401') {
      error.value = '토큰이 유효하지 않거나 만료되었어요. 처음부터 다시 시도해 주세요.'
    } else {
      error.value = '비밀번호 변경 중 오류가 발생했어요. 다시 시도해 주세요.'
    }
  } finally {
    loading.value = false
  }
}

function copyToken() {
  if (!resetToken.value) return
  navigator.clipboard?.writeText(resetToken.value).then(() => {
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  }).catch(() => {})
}

function backToStep1() {
  step.value = 1
  error.value = ''
}

function goBack() {
  if (step.value === 2) {
    backToStep1()
    return
  }
  router.back()
}

function goLogin() {
  router.push('/login')
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

/* Topbar */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  flex-shrink: 0;
}
.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
}
.back-btn:active { background: var(--color-surface); }
.topbar-title {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.topbar-spacer { width: 40px; }

/* Form */
.form-section {
  flex: 1;
  overflow-y: auto;
  padding: 12px 24px 32px;
}

/* Steps */
.steps {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}
.step {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.45;
  transition: opacity 0.2s;
}
.step.active { opacity: 1; }
.step-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-ink-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  font-weight: 700;
  flex-shrink: 0;
}
.step.active .step-dot {
  background: var(--color-peach);
  color: white;
}
.step-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  white-space: nowrap;
}
.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-line-light);
  border-radius: 1px;
  transition: background 0.2s;
}
.step-line.active { background: var(--color-peach); }

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
  margin-bottom: 24px;
  letter-spacing: -0.2px;
  line-height: 1.5;
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

/* 데모 안내 */
.demo-notice {
  background: var(--color-peach-light, #fff0e8);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  margin-bottom: 24px;
}
.demo-notice-head {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-peach-pressed, #d45f1e);
  margin-bottom: 6px;
}
.demo-notice-text {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
  line-height: 1.5;
  letter-spacing: -0.2px;
  margin-bottom: 12px;
}
.token-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  background: var(--color-white);
  border: 1.5px solid var(--color-peach);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  text-align: left;
}
.token-code {
  flex: 1;
  font-size: 12.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--color-ink);
  word-break: break-all;
  line-height: 1.4;
}
.token-copy {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach-pressed, #d45f1e);
}

.error-msg {
  font-size: 13px;
  color: var(--color-error);
  margin: 8px 0 12px;
}

.primary-btn {
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
.primary-btn:hover:not(:disabled) { background: var(--color-peach-pressed); }
.primary-btn:disabled { opacity: 0.6; }
.btn-spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255,255,255,0.4);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.ghost-btn {
  display: block;
  width: 100%;
  text-align: center;
  margin-top: 14px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

.login-link {
  text-align: center;
  margin-top: 24px;
  font-size: 13.5px;
  color: var(--color-ink-muted);
}
.login-text {
  color: var(--color-peach-pressed);
  font-weight: 700;
  margin-left: 4px;
}
</style>
