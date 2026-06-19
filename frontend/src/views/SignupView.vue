<template>
  <div class="page">
    <!-- Header bar -->
    <div class="header">
      <button class="back-btn" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <span class="header-title">회원가입</span>
      <div class="header-spacer" />
    </div>

    <!-- Form section -->
    <div class="form-section">
      <!-- Logo row -->
      <div class="logo-row">
        <div class="logo-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="white" stroke="none">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
          </svg>
        </div>
        <span class="logo-text">여행ON</span>
      </div>

      <h2 class="form-title">새 계정 만들기</h2>
      <p class="form-sub">정보를 입력하고 여행을 시작해보세요.</p>

      <div class="fields">
        <!-- Email -->
        <div class="field-wrap">
          <label class="field-label">이메일</label>
          <div class="input-wrap">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" /><polyline points="22,6 12,13 2,6" />
            </svg>
            <input
              v-model="email"
              type="email"
              class="field-input"
              placeholder="이메일을 입력해 주세요"
              autocomplete="email"
            />
          </div>
        </div>

        <!-- Nickname -->
        <div class="field-wrap">
          <label class="field-label">닉네임</label>
          <div class="input-wrap" :class="{ 'input-error-border': nicknameError }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" /><circle cx="12" cy="7" r="4" />
            </svg>
            <input
              v-model="nickname"
              type="text"
              class="field-input"
              placeholder="2~20자로 입력해 주세요"
              autocomplete="username"
              maxlength="20"
            />
          </div>
          <span v-if="nicknameError" class="inline-error">{{ nicknameError }}</span>
        </div>

        <!-- Password -->
        <div class="field-wrap">
          <label class="field-label">비밀번호</label>
          <div class="input-wrap" :class="{ 'input-error-border': passwordError }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" />
            </svg>
            <input
              v-model="password"
              type="password"
              class="field-input"
              placeholder="8자 이상 입력해 주세요"
              autocomplete="new-password"
            />
          </div>
          <span v-if="passwordError" class="inline-error">{{ passwordError }}</span>
        </div>

        <!-- Password Confirm -->
        <div class="field-wrap">
          <label class="field-label">비밀번호 확인</label>
          <div class="input-wrap" :class="{ 'input-error-border': passwordConfirmError }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
            </svg>
            <input
              v-model="passwordConfirm"
              type="password"
              class="field-input"
              placeholder="비밀번호를 다시 입력해 주세요"
              autocomplete="new-password"
              @keydown.enter="handleSignup"
            />
          </div>
          <span v-if="passwordConfirmError" class="inline-error">{{ passwordConfirmError }}</span>
        </div>
      </div>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

      <button class="signup-btn" :disabled="loading" @click="handleSignup">
        <span v-if="!loading">가입하기</span>
        <div v-else class="btn-spinner" />
      </button>

      <p class="login-link">
        이미 계정이 있으신가요?
        <button class="login-text" @click="router.push('/login')">로그인</button>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { authApi } from '@/api/index.js'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const nickname = ref('')
const password = ref('')
const passwordConfirm = ref('')
const errorMsg = ref('')
const loading = ref(false)

const nicknameError = computed(() => {
  if (!nickname.value) return ''
  if (nickname.value.length < 2 || nickname.value.length > 20)
    return '닉네임은 2~20자로 입력해 주세요.'
  return ''
})

const passwordError = computed(() => {
  if (!password.value) return ''
  if (password.value.length < 8) return '비밀번호는 8자 이상이어야 해요.'
  return ''
})

const passwordConfirmError = computed(() => {
  if (!passwordConfirm.value) return ''
  if (password.value !== passwordConfirm.value) return '비밀번호가 일치하지 않아요.'
  return ''
})

async function handleSignup() {
  errorMsg.value = ''

  if (!email.value || !nickname.value || !password.value || !passwordConfirm.value) {
    errorMsg.value = '모든 항목을 입력해 주세요.'
    return
  }
  if (nicknameError.value || passwordError.value || passwordConfirmError.value) {
    errorMsg.value = nicknameError.value || passwordError.value || passwordConfirmError.value
    return
  }

  loading.value = true
  try {
    // profileImageUrl 미전송 → BE가 기본 아바타(/images/default-profile.png)로 채움(외부 dicebear 제거)
    await authApi.signup({
      email: email.value,
      password: password.value,
      nickname: nickname.value,
    })
    // Auto-login after signup
    await auth.login(email.value, password.value)
    router.push('/home')
  } catch (err) {
    const code = err.response?.data?.code
    if (code === 'USER409') {
      errorMsg.value = '이미 사용 중인 이메일이에요.'
    } else {
      errorMsg.value = '가입 중 오류가 발생했어요. 다시 시도해 주세요.'
    }
  } finally {
    loading.value = false
  }
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

/* Header */
.header {
  display: flex;
  align-items: center;
  padding: 12px 8px 12px 16px;
  border-bottom: 1px solid var(--color-line-light);
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
.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.header-spacer { width: 40px; }

/* Form */
.form-section {
  flex: 1;
  overflow-y: auto;
  padding: 28px 24px 40px;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 8px rgba(247, 143, 87, 0.35);
}
.logo-text {
  font-size: 17px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
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

.fields { display: flex; flex-direction: column; gap: 14px; margin-bottom: 4px; }
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
.input-wrap.input-error-border { border-color: var(--color-error); }
.field-input {
  flex: 1;
  font-size: 14.5px;
  color: var(--color-ink);
  background: transparent;
}
.field-input::placeholder { color: var(--color-ink-muted); }

.inline-error {
  font-size: 12px;
  color: var(--color-error);
  padding-left: 2px;
}

.error-msg {
  font-size: 13px;
  color: var(--color-error);
  margin: 10px 0 4px;
}

.signup-btn {
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
.signup-btn:hover:not(:disabled) { background: var(--color-peach-pressed); }
.signup-btn:disabled { opacity: 0.6; }
.btn-spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255,255,255,0.4);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 13.5px;
  color: var(--color-ink-muted);
}
.login-text {
  color: var(--color-peach-pressed);
  font-weight: 700;
  margin-left: 4px;
}
</style>
