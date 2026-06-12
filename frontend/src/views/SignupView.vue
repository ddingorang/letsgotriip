<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.js';
import { http } from '../api/http.js';

const router = useRouter();
const auth = useAuthStore();

const email = ref('');
const nickname = ref('');
const password = ref('');
const passwordConfirm = ref('');
const errorMsg = ref('');
const loading = ref(false);

// Client-side validation
const nicknameError = computed(() => {
  if (!nickname.value) return '';
  if (nickname.value.length < 2 || nickname.value.length > 20)
    return '닉네임은 2~20자로 입력해 주세요.';
  return '';
});

const passwordError = computed(() => {
  if (!password.value) return '';
  if (password.value.length < 8) return '비밀번호는 8자 이상이어야 해요.';
  return '';
});

const passwordConfirmError = computed(() => {
  if (!passwordConfirm.value) return '';
  if (password.value !== passwordConfirm.value) return '비밀번호가 일치하지 않아요.';
  return '';
});

function profileImageUrl(nick) {
  return `https://api.dicebear.com/9.x/thumbs/svg?seed=${encodeURIComponent(nick)}`;
}

async function handleSignup() {
  errorMsg.value = '';

  // Basic presence check
  if (!email.value || !nickname.value || !password.value || !passwordConfirm.value) {
    errorMsg.value = '모든 항목을 입력해 주세요.';
    return;
  }
  if (nicknameError.value || passwordError.value || passwordConfirmError.value) {
    errorMsg.value = nicknameError.value || passwordError.value || passwordConfirmError.value;
    return;
  }

  loading.value = true;
  try {
    await http.post('/auth/signup', {
      email: email.value,
      password: password.value,
      nickname: nickname.value,
      profileImageUrl: profileImageUrl(nickname.value),
    });
    // Auto-login after signup
    await auth.login(email.value, password.value);
    router.push('/');
  } catch (err) {
    const code = err.response?.data?.code;
    if (code === 'USER409') {
      errorMsg.value = '이미 사용 중인 이메일이에요.';
    } else {
      errorMsg.value = '가입 중 오류가 발생했어요. 다시 시도해 주세요.';
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="signup-view">
    <!-- Logo area -->
    <div class="logo-area">
      <div class="logo-text">관통여행</div>
      <div class="logo-sub">새 계정을 만들어 보세요</div>
    </div>

    <div class="form-card">
      <div class="form-title">회원가입</div>

      <div class="field">
        <label class="field-label">이메일</label>
        <input
          v-model="email"
          type="email"
          class="field-input"
          placeholder="이메일을 입력해 주세요"
          autocomplete="email"
        />
      </div>

      <div class="field">
        <label class="field-label">닉네임</label>
        <input
          v-model="nickname"
          type="text"
          class="field-input"
          :class="{ 'input-error': nicknameError }"
          placeholder="2~20자로 입력해 주세요"
          autocomplete="username"
          maxlength="20"
        />
        <span v-if="nicknameError" class="inline-error">{{ nicknameError }}</span>
      </div>

      <div class="field">
        <label class="field-label">비밀번호</label>
        <input
          v-model="password"
          type="password"
          class="field-input"
          :class="{ 'input-error': passwordError }"
          placeholder="8자 이상 입력해 주세요"
          autocomplete="new-password"
        />
        <span v-if="passwordError" class="inline-error">{{ passwordError }}</span>
      </div>

      <div class="field">
        <label class="field-label">비밀번호 확인</label>
        <input
          v-model="passwordConfirm"
          type="password"
          class="field-input"
          :class="{ 'input-error': passwordConfirmError }"
          placeholder="비밀번호를 다시 입력해 주세요"
          autocomplete="new-password"
          @keyup.enter="handleSignup"
        />
        <span v-if="passwordConfirmError" class="inline-error">{{ passwordConfirmError }}</span>
      </div>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <button class="btn-primary" :disabled="loading" @click="handleSignup">
        <span v-if="loading" class="spinner"></span>
        <span v-else>가입하기</span>
      </button>

      <p class="login-link">
        이미 계정이 있으신가요?
        <router-link to="/login">로그인</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.signup-view {
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
  margin-bottom: 14px;
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
.field-input.input-error {
  border-color: var(--color-error);
}

.inline-error {
  font: var(--weight-medium) var(--text-xs)/1.3 var(--font-sans);
  color: var(--color-error);
}

.error-msg {
  font: var(--weight-medium) var(--text-sm)/1.4 var(--font-sans);
  color: var(--color-error);
  margin: 4px 0 12px;
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
  margin-top: 8px;
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

.login-link {
  text-align: center;
  font: var(--weight-medium) var(--text-sm)/1 var(--font-sans);
  color: var(--text-secondary);
  margin-top: 20px;
  margin-bottom: 0;
}
.login-link a {
  color: var(--color-primary-500);
  font-weight: var(--weight-semibold);
  text-decoration: none;
}
.login-link a:hover {
  text-decoration: underline;
}
</style>
