<template>
  <div class="page">
    <!-- Header -->
    <header class="ai-header">
      <div class="ai-hero-glow" />
      <div class="ai-hero-glow2" />
      <button class="back-btn" @click="$router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.8)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="ai-badge">
        <span class="ai-badge-dot" />
        조건 확인
      </div>
      <h1 class="ai-title">이렇게 일정을<br />만들까요?</h1>
      <p class="ai-sub">조건을 확인하고 AI 일정 생성을 시작하세요</p>
    </header>

    <div class="scroll-content">
      <!-- 선택 조건 요약 -->
      <div class="summary-card">
        <div class="summary-row">
          <span class="summary-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <circle cx="12" cy="11" r="3" />
            </svg>
          </span>
          <span class="summary-label">지역</span>
          <span class="summary-val">{{ areaName }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          </span>
          <span class="summary-label">일정</span>
          <span class="summary-val">{{ conditions?.startDate }} ~ {{ conditions?.endDate }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
            </svg>
          </span>
          <span class="summary-label">동행</span>
          <span class="summary-val">{{ companionLabel }}</span>
        </div>
        <div v-if="conditions?.budgetLabel" class="summary-row">
          <span class="summary-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="1" x2="12" y2="23" />
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
            </svg>
          </span>
          <span class="summary-label">예산</span>
          <span class="summary-val">{{ conditions.budgetLabel ?? conditions.budget.toLocaleString() + '원 이하' }}</span>
        </div>
        <div v-if="conditions?.themes?.length" class="summary-row themes-row">
          <span class="summary-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
          </span>
          <span class="summary-label">테마</span>
          <div class="theme-chips">
            <span v-for="t in conditions.themes" :key="t" class="theme-chip">{{ themeLabel(t) }}</span>
          </div>
        </div>
      </div>

      <!-- 조건 수정 안내 -->
      <button class="edit-btn" @click="$router.back()">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
        </svg>
        조건 수정하기
      </button>

      <div class="bottom-spacer" />
    </div>

    <!-- Bottom CTA -->
    <div class="bottom-bar">
      <div v-if="localError || recommendStore.error" class="error-msg">
        {{ localError || recommendStore.error }}
      </div>
      <button
        class="ai-btn"
        :disabled="recommendStore.generating"
        @click="handleGenerate"
      >
        <template v-if="!recommendStore.generating">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
          </svg>
          AI 일정 생성하기
        </template>
        <template v-else>
          <span class="spinner" />
          일정 생성 중...
        </template>
      </button>
      <p class="ai-btn-sub">
        <template v-if="recommendStore.generating">AI가 일정을 만들고 있어요… 최대 30초</template>
        <template v-else>약 10–30초 소요 · TourAPI + LLM 기반 생성</template>
      </p>
    </div>

    <!-- Fullscreen loading overlay -->
    <Transition name="fade">
      <div v-if="recommendStore.generating" class="loading-overlay">
        <div class="loading-box">
          <div class="loading-spinner" />
          <div class="loading-title">AI가 일정을 만들고 있어요</div>
          <div class="loading-sub">최대 30초 소요돼요. 잠시만 기다려 주세요.</div>
          <button class="cancel-btn" @click="handleCancel">취소</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRecommendStore } from '@/stores/recommend.js'

const router = useRouter()
const recommendStore = useRecommendStore()

// ── 조건 읽기: AiPlanInputView가 history.state.conditions로 전달 ──────────────
const conditions = ref(null)

onMounted(() => {
  const state = history.state?.conditions
  if (state) {
    conditions.value = state
  }
})

// ── 표시용 헬퍼 ───────────────────────────────────────────────────────────────
const areaCodeNames = {
  '1': '서울', '6': '부산', '4': '대구', '2': '인천',
  '5': '광주', '3': '대전', '7': '울산', '39': '제주도',
}
const areaName = computed(() => areaCodeNames[conditions.value?.areaCode] ?? conditions.value?.areaCode ?? '-')

const companionApiToLabel = {
  SOLO: '혼자', COUPLE: '커플', FAMILY: '가족', FRIENDS: '친구',
}
const companionLabel = computed(() => companionApiToLabel[conditions.value?.companions] ?? conditions.value?.companions ?? '-')

const themeLabelMap = {
  sea: '바다/해변', mountain: '산/자연', food: '맛집 투어',
  history: '역사/문화', activity: '액티비티', shopping: '쇼핑',
}
function themeLabel(key) { return themeLabelMap[key] ?? key }

// ── 에러 ──────────────────────────────────────────────────────────────────────
const localError = ref('')

// ── 생성 ──────────────────────────────────────────────────────────────────────
async function handleGenerate() {
  if (!conditions.value?.areaCode) {
    localError.value = '조건을 먼저 설정해 주세요. 이전 화면으로 돌아가 지역을 선택해 주세요.'
    return
  }
  localError.value = ''
  recommendStore.error = null

  const payload = {
    areaCode: conditions.value.areaCode,
    startDate: conditions.value.startDate,
    endDate: conditions.value.endDate,
    companions: conditions.value.companions,
    themes: conditions.value.themes?.length ? conditions.value.themes : undefined,
    periodValid: conditions.value.periodValid ?? true,
  }
  if (conditions.value.budget != null) payload.budget = conditions.value.budget
  if (conditions.value.title) payload.title = conditions.value.title

  try {
    await recommendStore.generate(payload)
    router.push('/ai/result')
  } catch {
    // 취소/실패 — 취소면 store.error=null, 실패면 store.error에 메시지 (둘 다 화면 유지)
  }
}

function handleCancel() {
  recommendStore.cancelGenerate()
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

/* ── Hero header ─────────────────────────────────────────────────────────── */
.ai-header {
  background: linear-gradient(135deg, #1c1c3a 0%, #0f2f5a 100%);
  padding: 52px 20px 24px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.back-btn {
  position: absolute;
  top: 52px;
  left: 16px;
  z-index: 2;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-hero-glow {
  position: absolute;
  top: -40px;
  right: -30px;
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(247, 143, 87, 0.25) 0%, transparent 70%);
  pointer-events: none;
}

.ai-hero-glow2 {
  position: absolute;
  bottom: -20px;
  left: 10px;
  width: 110px;
  height: 110px;
  background: radial-gradient(circle, rgba(100, 180, 240, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(247, 143, 87, 0.18);
  border: 1px solid rgba(247, 143, 87, 0.4);
  color: var(--color-peach);
  padding: 5px 12px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

.ai-badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-peach);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

.ai-title {
  font-size: 22px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  line-height: 1.3;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
  padding-left: 4px;
}

.ai-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: -0.2px;
  position: relative;
  z-index: 1;
}

/* ── Scroll content ──────────────────────────────────────────────────────── */
.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bottom-spacer {
  height: 140px;
  flex-shrink: 0;
}

/* ── Summary card ────────────────────────────────────────────────────────── */
.summary-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.themes-row {
  align-items: flex-start;
}

.summary-icon {
  width: 26px;
  height: 26px;
  background: var(--color-peach-light);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-peach);
  flex-shrink: 0;
}

.summary-label {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  font-weight: 500;
  width: 44px;
  flex-shrink: 0;
}

.summary-val {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  flex: 1;
}

.theme-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.theme-chip {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

/* ── Edit button ─────────────────────────────────────────────────────────── */
.edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: var(--color-white);
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  cursor: pointer;
  box-shadow: var(--shadow-card);
  transition: all 0.15s;
}

/* ── Bottom CTA ──────────────────────────────────────────────────────────── */
.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  padding: 12px 16px calc(var(--bottom-nav-height) + var(--safe-bottom) + 12px);
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 10;
}

.error-msg {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-error);
  text-align: center;
}

.ai-btn {
  width: 100%;
  height: 52px;
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: #fff;
  border: none;
  border-radius: var(--radius-xl);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.3px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 4px 16px rgba(247, 143, 87, 0.35);
  transition: opacity 0.2s;
}

.ai-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-btn-sub {
  text-align: center;
  font-size: 12px;
  color: var(--color-ink-muted);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

/* ── Loading overlay ─────────────────────────────────────────────────────── */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 20, 40, 0.85);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-box {
  text-align: center;
  padding: 32px 28px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-xl);
  max-width: 280px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 3.5px solid rgba(247, 143, 87, 0.3);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

.loading-title {
  font-size: 17px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.4px;
  margin-bottom: 8px;
}

.loading-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.cancel-btn {
  margin-top: 20px;
  padding: 9px 24px;
  font-size: 13.5px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: background 0.15s;
}

.cancel-btn:active {
  background: rgba(255, 255, 255, 0.2);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
