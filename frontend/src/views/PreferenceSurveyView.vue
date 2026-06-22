# Created: 2026-06-16 14:08:00
<template>
  <div class="page">
    <!-- Top nav -->
    <header class="survey-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="step-label">2 / 3</span>
      <button class="skip-btn" @click="skip">건너뛰기</button>
    </header>

    <!-- Progress bar -->
    <div class="progress-bar">
      <div class="progress-fill" style="width: 66.6%" />
    </div>

    <div class="scroll-content">
      <div class="survey-tag">취향 설문</div>
      <h1 class="survey-title">어떤 여행을<br />좋아하세요?</h1>
      <p class="survey-sub">관심사를 골라주시면 AI가 더 잘 맞는 일정을 추천해드려요. (복수 선택)</p>
      <span v-if="selectedInterests.length" class="select-count">{{ selectedInterests.length }}개 선택됨</span>

      <!-- Interest grid -->
      <div class="interests-grid">
        <button
          v-for="item in interests"
          :key="item.key"
          :class="['interest-btn', { selected: selectedInterests.includes(item.key) }]"
          @click="toggleInterest(item.key)"
        >
          <div class="interest-icon" v-html="item.icon" />
          <span class="interest-label">{{ item.label }}</span>
          <div v-if="selectedInterests.includes(item.key)" class="check-mark">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
          </div>
        </button>
      </div>

      <!-- Companion type -->
      <h2 class="companion-title">누구와 떠나세요?</h2>
      <div class="companion-chips">
        <button
          v-for="type in companionTypes"
          :key="type"
          :class="['companion-chip', { active: selectedCompanion === type }]"
          @click="selectedCompanion = type"
        >
          {{ type }}
        </button>
      </div>

      <div style="height: 100px" />
    </div>

    <!-- Next button -->
    <div class="cta-bar">
      <button class="next-btn" :disabled="saving" @click="next">{{ saving ? '저장 중...' : '다음' }}</button>
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

const interests = [
  {
    key: 'nature', label: '자연·힐링',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`,
  },
  {
    key: 'food', label: '미식 여행',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8h1a4 4 0 010 8h-1"/><path d="M2 8h16v9a4 4 0 01-4 4H6a4 4 0 01-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>`,
  },
  {
    key: 'history', label: '역사·문화',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
  },
  {
    key: 'activity', label: '액티비티',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="2"/><path d="M10 22v-6M14 22v-6M8 11l-1 5h10l-1-5"/><path d="M10 9l-1 2M14 9l1 2"/></svg>`,
  },
  {
    key: 'cafe', label: '카페·디저트',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M23 3a10.9 10.9 0 01-3.14 1.53A4.48 4.48 0 0016 2a4.48 4.48 0 00-4 6.27C7 8.28 4 6 2 3c0 0-4 9 5 13a11.64 11.64 0 01-7 2c9 5 20 0 20-11.5a4.5 4.5 0 00-.08-.83A7.72 7.72 0 0023 3z"/></svg>`,
  },
  {
    key: 'night', label: '야경·사진',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>`,
  },
  {
    key: 'shopping', label: '쇼핑',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>`,
  },
  {
    key: 'resort', label: '휴양·호캉스',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
  },
  {
    key: 'festival', label: '축제·공연',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 010 7.07"/><path d="M19.07 4.93a10 10 0 010 14.14"/></svg>`,
  },
]

const companionTypes = ['혼자', '연인', '친구', '가족']
// 기존 저장값이 있으면 프리필, 없으면 빈 선택으로 시작(하드코딩 프리셋 제거)
const selectedInterests = ref([...(authStore.user?.preferredInterests ?? [])])
const selectedCompanion = ref(authStore.user?.preferredCompanion ?? '')
const saving = ref(false)

function toggleInterest(key) {
  const idx = selectedInterests.value.indexOf(key)
  if (idx === -1) selectedInterests.value.push(key)
  else selectedInterests.value.splice(idx, 1)
}

/** 취향설문을 서버에 저장한다. 실패해도 온보딩 흐름은 막지 않는다. */
async function savePreferences() {
  try {
    await http.patch('/users/me/preferences', {
      interests: selectedInterests.value,
      companion: selectedCompanion.value || null,
    })
    // 저장된 취향을 스토어에 반영
    await authStore.fetchMe()
  } catch {
    // 비로그인/네트워크 오류 시 조용히 무시하고 다음 단계로 진행
  }
}

async function skip() {
  router.push('/home')
}
async function next() {
  if (saving.value) return
  saving.value = true
  try {
    await savePreferences()
  } finally {
    saving.value = false
  }
  router.push('/home')
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

.survey-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
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
.step-label { font-size: 14px; font-weight: 600; color: var(--color-ink-secondary); }
.skip-btn { font-size: 14px; font-weight: 600; color: var(--color-peach); padding: 8px; }

.progress-bar {
  height: 4px;
  background: var(--color-line-light);
  flex-shrink: 0;
}
.progress-fill {
  height: 100%;
  background: var(--color-peach);
  border-radius: 2px;
  transition: width 0.3s;
}

.scroll-content { flex: 1; overflow-y: auto; padding: 28px 20px 20px; }

.survey-tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-peach);
  letter-spacing: 0.2px;
  margin-bottom: 8px;
}
.survey-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.7px;
  line-height: 1.3;
  margin-bottom: 10px;
}
.survey-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.55;
  margin-bottom: 24px;
}
.select-count {
  display: inline-block;
  margin-bottom: 16px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  border-radius: var(--radius-full);
}

.interests-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 28px;
}
.interest-btn {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 16px 8px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  color: var(--color-ink-secondary);
  transition: all 0.15s;
}
.interest-btn.selected {
  border-color: var(--color-peach);
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}
.interest-icon { display: flex; align-items: center; justify-content: center; }
.interest-label { font-size: 12.5px; font-weight: 600; letter-spacing: -0.2px; }
.check-mark {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1.5px solid var(--color-peach);
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.companion-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 12px;
}
.companion-chips { display: flex; gap: 8px; }
.companion-chip {
  flex: 1;
  padding: 11px 0;
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-line);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-ink-muted);
  text-align: center;
  transition: all 0.15s;
}
.companion-chip.active {
  background: var(--color-ink);
  color: white;
  border-color: var(--color-ink);
}

.cta-bar {
  padding: 12px 20px calc(12px + var(--safe-bottom));
  border-top: 1px solid var(--color-line-light);
  background: white;
  flex-shrink: 0;
}
.next-btn {
  width: 100%;
  padding: 16px;
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}
.next-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
