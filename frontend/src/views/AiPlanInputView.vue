<template>
  <div class="page">
    <!-- Header -->
    <header class="ai-header">
      <div class="ai-hero-glow" />
      <div class="ai-hero-glow2" />
      <div class="ai-badge">
        <span class="ai-badge-dot" />
        AI 분석 준비 완료
      </div>
      <h1 class="ai-title">어떤 여행을<br />원하시나요?</h1>
      <p class="ai-sub">조건을 입력하면 AI가 최적의 일정을 만들어 드려요</p>
    </header>

    <div class="scroll-content">
      <!-- 지역 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="form-card-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <circle cx="12" cy="11" r="3" />
            </svg>
          </span>
          어디로 가고 싶으세요?
        </div>
        <div v-if="areasLoading" class="region-loading">지역 목록을 불러오는 중...</div>
        <div v-else class="region-grid">
          <button
            v-for="area in areas"
            :key="area.code"
            class="region-btn"
            :class="{ selected: selectedAreaCode === area.code }"
            @click="selectedAreaCode = area.code"
          >{{ area.name }}</button>
        </div>
      </div>

      <!-- 날짜 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="form-card-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="4" width="18" height="18" rx="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          </span>
          언제 떠나세요?
        </div>
        <div class="date-row">
          <div class="date-picker">
            <div class="date-picker-label">출발일</div>
            <input
              v-model="startDate"
              type="date"
              class="date-input"
              :min="todayStr"
            />
            <div class="date-picker-value">{{ formatDisplay(startDate) }}</div>
          </div>
          <div class="date-arrow">→</div>
          <div class="date-picker">
            <div class="date-picker-label">도착일</div>
            <div class="date-picker-value">{{ formatDisplay(endDate) }}</div>
          </div>
        </div>
        <div class="night-row">
          <span class="night-label">숙박 일수</span>
          <div class="night-ctrl">
            <button class="night-btn" :disabled="nights <= 1" @click="nights > 1 && nights--">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12" /></svg>
            </button>
            <span class="night-val">{{ nights }}박</span>
            <button class="night-btn" :disabled="nights >= 7" @click="nights < 7 && nights++">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 동행인 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="form-card-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          누구와 함께 가나요?
        </div>
        <div class="people-grid">
          <button
            v-for="c in companionTypes"
            :key="c.key"
            class="people-btn"
            :class="{ selected: selectedCompanion === c.key }"
            @click="selectedCompanion = c.key"
          >
            <span class="people-icon" v-html="c.icon" />
            <span class="people-text">
              <span class="people-name">{{ c.label }}</span>
              <span class="people-sub">{{ c.sub }}</span>
            </span>
          </button>
        </div>
      </div>

      <!-- 예산 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="form-card-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="1" x2="12" y2="23" />
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
            </svg>
          </span>
          예산이 얼마나 되세요?
        </div>
        <div class="budget-options">
          <button
            v-for="opt in budgetOptions"
            :key="opt.label"
            class="budget-chip"
            :class="{ selected: selectedBudget === opt.label }"
            @click="selectedBudget = opt.label"
          >{{ opt.label }}</button>
        </div>
      </div>

      <!-- 테마 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="form-card-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
          </span>
          어떤 여행을 원하세요?
          <span class="title-hint">(복수 선택)</span>
        </div>
        <div class="theme-grid">
          <button
            v-for="t in themes"
            :key="t.key"
            class="theme-btn"
            :class="{ selected: selectedThemes.includes(t.key) }"
            @click="toggleTheme(t.key)"
          >
            <span class="theme-icon">{{ t.icon }}</span>
            <span class="theme-label">{{ t.label }}</span>
          </button>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- Bottom CTA -->
    <div class="bottom-bar">
      <div v-if="localError" class="error-msg">{{ localError }}</div>
      <button class="ai-btn" @click="handleNext">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M5 12h14M12 5l7 7-7 7" />
        </svg>
        다음 단계
      </button>
      <p class="ai-btn-sub">조건 확인 후 AI 일정을 생성할 수 있어요</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/http.js'

const router = useRouter()

// ── 지역 ──────────────────────────────────────────────────────────────────────
const areas = ref([])
const areasLoading = ref(true)
const selectedAreaCode = ref('')

onMounted(async () => {
  try {
    const { data } = await http.get('/api/attractions/areas')
    areas.value = (Array.isArray(data) ? data : []).slice(0, 8)
    if (areas.value.length) selectedAreaCode.value = areas.value[0].code
  } catch {
    areas.value = [
      { code: '1', name: '서울' },
      { code: '6', name: '부산' },
      { code: '4', name: '대구' },
      { code: '2', name: '인천' },
      { code: '5', name: '광주' },
      { code: '3', name: '대전' },
      { code: '7', name: '울산' },
      { code: '39', name: '제주도' },
    ]
    selectedAreaCode.value = areas.value[0].code
  } finally {
    areasLoading.value = false
  }
})

// ── 날짜 ──────────────────────────────────────────────────────────────────────
const fmtDate = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

const todayStr = fmtDate(new Date())

function defaultStart() {
  const d = new Date()
  d.setDate(d.getDate() + 7)
  return fmtDate(d)
}

const startDate = ref(defaultStart())
const nights = ref(2)

const endDate = computed(() => {
  const d = new Date(startDate.value)
  d.setDate(d.getDate() + nights.value)
  return fmtDate(d)
})

function formatDisplay(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const weekdays = ['일', '월', '화', '수', '목', '금', '토']
  const wd = weekdays[d.getDay()]
  return `${String(month).padStart(2, '0')}/${String(day).padStart(2, '0')} (${wd})`
}

// ── 동행인 ────────────────────────────────────────────────────────────────────
const companionTypes = [
  {
    key: 'solo',
    apiVal: 'SOLO',
    label: '혼자',
    sub: '솔로 여행',
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
  },
  {
    key: 'couple',
    apiVal: 'COUPLE',
    label: '커플',
    sub: '2명',
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
  },
  {
    key: 'family',
    apiVal: 'FAMILY',
    label: '가족',
    sub: '아이 포함',
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>`,
  },
  {
    key: 'friend',
    apiVal: 'FRIENDS',
    label: '친구',
    sub: '3명 이상',
    icon: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="9" cy="7" r="4"/><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/></svg>`,
  },
]
const selectedCompanion = ref('couple')

// ── 예산 ──────────────────────────────────────────────────────────────────────
const budgetOptions = [
  { label: '10만원 이하',  value: 100_000   },
  { label: '10~30만원',   value: 300_000   },
  { label: '30~50만원',   value: 500_000   },
  { label: '50만원 이상', value: 1_000_000 },
  { label: '상관없음',    value: null       },
]
const selectedBudget = ref('10~30만원')

// ── 테마 ──────────────────────────────────────────────────────────────────────
// key 값(sea/mountain/food/...)이 BE로 전송되는 테마 코드다.
// BE는 RecommendService.THEME_LABELS에서 이 키를 한글 의미로 매핑해 AI 프롬프트에 반영한다.
// 키를 추가/변경하면 BE THEME_LABELS도 함께 갱신해야 한다(매핑 테이블 단일 소스 = BE).
const themes = [
  { key: 'sea',      icon: '🌊', label: '바다/해변' },
  { key: 'mountain', icon: '🏔️', label: '산/자연'   },
  { key: 'food',     icon: '🍽️', label: '맛집 투어' },
  { key: 'history',  icon: '🏛️', label: '역사/문화' },
  { key: 'activity', icon: '🎢', label: '액티비티'  },
  { key: 'shopping', icon: '🛍️', label: '쇼핑'      },
]
const selectedThemes = ref(['sea', 'food'])

function toggleTheme(key) {
  const idx = selectedThemes.value.indexOf(key)
  if (idx === -1) selectedThemes.value.push(key)
  else selectedThemes.value.splice(idx, 1)
}

// ── 에러 ──────────────────────────────────────────────────────────────────────
const localError = ref('')

// ── 다음 단계 ─────────────────────────────────────────────────────────────────
function handleNext() {
  if (!selectedAreaCode.value) {
    localError.value = '지역을 선택해 주세요.'
    return
  }
  localError.value = ''

  const companion = companionTypes.find(c => c.key === selectedCompanion.value)
  const budgetOpt = budgetOptions.find(o => o.label === selectedBudget.value)

  const conditions = {
    areaCode: selectedAreaCode.value,
    startDate: startDate.value,
    endDate: endDate.value,
    companions: companion?.apiVal,
    themes: selectedThemes.value.length ? [...selectedThemes.value] : undefined,
    periodValid: true,
  }
  if (budgetOpt?.value != null) conditions.budget = budgetOpt.value

  // Pass conditions to /ai via History state so AiInputView can read them
  router.push({ name: 'ai-input', state: { conditions } })
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

/* ── Form cards ──────────────────────────────────────────────────────────── */
.form-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
}

.form-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 14px;
}

.form-card-icon {
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

.title-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-ink-muted);
}

/* ── Region grid ─────────────────────────────────────────────────────────── */
.region-loading {
  font-size: 13px;
  color: var(--color-ink-muted);
}

.region-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.region-btn {
  padding: 10px 4px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  border: 1.5px solid transparent;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
}

.region-btn.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
  color: var(--color-peach-pressed);
  font-weight: 700;
}

/* ── Date ────────────────────────────────────────────────────────────────── */
.date-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.date-picker {
  flex: 1;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 12px;
  border: 1.5px solid var(--color-line-light);
  position: relative;
}

.date-picker-label {
  font-size: 11px;
  color: var(--color-ink-muted);
  margin-bottom: 4px;
  font-weight: 500;
}

.date-picker-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.date-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  width: 100%;
  cursor: pointer;
}

.date-arrow {
  color: var(--color-ink-muted);
  font-size: 14px;
  flex-shrink: 0;
}

.night-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.night-label {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
}

.night-ctrl {
  display: flex;
  align-items: center;
  gap: 14px;
}

.night-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-ink);
  transition: all 0.15s;
}

.night-btn:disabled {
  opacity: 0.35;
  cursor: default;
}

.night-val {
  font-size: 17px;
  font-weight: 800;
  color: var(--color-ink);
  min-width: 36px;
  text-align: center;
}

/* ── People grid ─────────────────────────────────────────────────────────── */
.people-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.people-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1.5px solid transparent;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s;
}

.people-btn.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
}

.people-icon {
  width: 32px;
  height: 32px;
  background: var(--color-white);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-secondary);
  flex-shrink: 0;
}

.people-btn.selected .people-icon {
  background: var(--color-peach-light);
  color: var(--color-peach);
}

.people-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.people-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.people-sub {
  font-size: 11px;
  color: var(--color-ink-muted);
}

/* ── Budget chips ────────────────────────────────────────────────────────── */
.budget-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.budget-chip {
  padding: 8px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}

.budget-chip.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
  color: var(--color-peach-pressed);
  font-weight: 700;
}

/* ── Theme grid ──────────────────────────────────────────────────────────── */
.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.theme-btn {
  padding: 12px 8px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1.5px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.theme-btn.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
}

.theme-icon {
  font-size: 22px;
  line-height: 1;
}

.theme-label {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
  text-align: center;
}

.theme-btn.selected .theme-label {
  color: var(--color-peach-pressed);
  font-weight: 700;
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

.ai-btn-sub {
  text-align: center;
  font-size: 12px;
  color: var(--color-ink-muted);
}
</style>
