# Created: 2026-06-16 14:26:48
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="nav-title">프로필 수정</span>
      <button class="save-btn" :disabled="saving" @click="save">{{ saving ? '저장 중...' : '저장' }}</button>
    </header>

    <div class="scroll-content">
      <!-- Avatar -->
      <div class="avatar-section">
        <div class="avatar-wrap">
          <div class="avatar-circle">
            <img v-if="form.profileImageUrl" :src="form.profileImageUrl" alt="프로필 사진" class="avatar-img" />
            <span v-else class="avatar-text">프로필</span>
          </div>
          <button class="camera-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" /><circle cx="12" cy="13" r="4" />
            </svg>
          </button>
        </div>
        <button class="change-photo-btn">사진 변경</button>
      </div>

      <div class="form-body">
        <!-- 닉네임 -->
        <div class="field">
          <label class="field-label">닉네임 <span class="req">*</span></label>
          <input
            v-model="form.nickname"
            class="field-input"
            placeholder="닉네임"
            maxlength="12"
          />
          <span class="field-hint">2~12자, 한글·영문·숫자</span>
        </div>

        <!-- 한줄 소개 -->
        <div class="field">
          <label class="field-label">한줄 소개</label>
          <input
            v-model="form.bio"
            class="field-input"
            placeholder="나를 한 줄로 소개해보세요"
          />
        </div>

        <!-- 오류 메시지 -->
        <p v-if="error" class="error-msg">{{ error }}</p>

        <!-- 로그아웃 -->
        <button class="logout-row" @click="logout">
          <span class="logout-label">로그아웃</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { http } from '@/api/http.js'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  nickname: authStore.user?.nickname ?? '',
  profileImageUrl: authStore.user?.profileImageUrl ?? '',
  bio: '',
})

const saving = ref(false)
const error = ref(null)

async function save() {
  if (saving.value) return
  saving.value = true
  error.value = null
  try {
    await http.patch('/users/me', {
      nickname: form.value.nickname,
      profileImageUrl: form.value.profileImageUrl || undefined,
    })
    await authStore.fetchMe()
    router.back()
  } catch (e) {
    error.value = e?.response?.data?.message ?? '저장에 실패했어요. 다시 시도해주세요.'
  } finally {
    saving.value = false
  }
}

function logout() {
  authStore.logout()
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
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.nav-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.save-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
}

.scroll-content { flex: 1; overflow-y: auto; }

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 20px 20px;
  gap: 10px;
}
.avatar-wrap { position: relative; }
.avatar-circle {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e0d4cc);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text { font-size: 12px; font-weight: 500; color: var(--color-ink-muted); }
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.camera-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2.5px solid white;
}
.change-photo-btn {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-peach);
}

.form-body { padding: 8px 20px; }
.field { margin-bottom: 24px; }
.field-label {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  margin-bottom: 8px;
}
.req { color: var(--color-peach); }
.field-input {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14.5px;
  color: var(--color-ink);
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: var(--color-peach); }
.field-input::placeholder { color: var(--color-ink-muted); }
.field-hint {
  display: block;
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-top: 5px;
  padding-left: 2px;
}

.logout-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 16px 0;
  border-top: 1px solid var(--color-line-light);
  margin-top: 8px;
}
.logout-label {
  font-size: 14.5px;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error-msg {
  font-size: 13px;
  color: var(--color-error, #d0392b);
  margin-bottom: 12px;
  padding: 8px 10px;
  background: #fff0f0;
  border-radius: var(--radius-md);
}
</style>
