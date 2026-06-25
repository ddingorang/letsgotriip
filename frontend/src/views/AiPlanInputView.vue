<template>
  <div class="page">
    <!-- Header -->
    <header class="ai-header">
      <div class="ai-hero-glow" />
      <div class="ai-hero-glow2" />
      <button class="ai-back-btn" aria-label="뒤로가기" @click="goBack">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <div class="ai-badge">
        <span class="ai-badge-dot" />
        AI 분석 준비 완료
      </div>
      <h1 class="ai-title">어떤 여행을<br />원하시나요?</h1>
      <p class="ai-sub">조건을 입력하면 AI가 최적의 일정을 만들어 드려요</p>
    </header>

    <div class="scroll-content">
      <!-- 일정 제목 -->
      <div class="form-card">
        <div class="form-card-title">
          <span class="form-card-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </span>
          일정 이름
          <span class="title-hint">(선택)</span>
        </div>
        <input
          v-model="planTitle"
          type="text"
          class="title-input"
          placeholder="예) 제주 여름 힐링 여행"
          maxlength="50"
        />
      </div>

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
          <div class="date-picker" :class="{ active: calOpen === 'start' }" @click="openCal('start')">
            <div class="date-picker-label">출발일</div>
            <div class="date-picker-value">{{ formatDisplay(startDate) }}</div>
          </div>
          <div class="date-arrow">→</div>
          <div class="date-picker" :class="{ active: calOpen === 'end' }" @click="openCal('end')">
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
        <div v-if="showCountStepper" class="companion-count-row">
          <span class="night-label">인원 수</span>
          <div class="night-ctrl">
            <button class="night-btn" :disabled="companionCount <= 2" @click="companionCount > 2 && companionCount--">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12" /></svg>
            </button>
            <span class="night-val">{{ companionCount }}명</span>
            <button class="night-btn" :disabled="companionCount >= 20" @click="companionCount < 20 && companionCount++">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
            </button>
          </div>
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
      <!-- AI 없이, 설정만으로 빈 계획 생성 -->
      <button class="empty-btn" :disabled="creatingEmpty" @click="handleCreateEmpty">
        {{ creatingEmpty ? '만드는 중...' : '빈 계획으로 만들기' }}
      </button>
      <p class="ai-btn-sub">조건 확인 후 AI 일정을 생성할 수 있어요</p>
    </div>

    <!-- ── 캘린더 팝업 ──────────────────────────────────────────────────────── -->
    <Teleport to="body">
      <div v-if="calOpen" class="cal-backdrop" @click.self="calOpen = null">
        <div class="cal-popup">
          <div class="cal-top">
            <span class="cal-picking">{{ calOpen === 'start' ? '출발일' : '도착일' }} 선택</span>
            <button class="cal-close" @click="calOpen = null">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="cal-nav">
            <button class="cal-nav-btn" @click="shiftMonth(-1)">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M15 18l-6-6 6-6"/></svg>
            </button>
            <span class="cal-month-label">{{ calYear }}년 {{ calMonth + 1 }}월</span>
            <button class="cal-nav-btn" @click="shiftMonth(1)">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M9 18l6-6-6-6"/></svg>
            </button>
          </div>
          <div class="cal-weekdays">
            <span v-for="d in ['일','월','화','수','목','금','토']" :key="d">{{ d }}</span>
          </div>
          <div class="cal-grid">
            <button
              v-for="(cell, i) in calCells"
              :key="i"
              class="cal-cell"
              :class="{
                empty: !cell,
                today: cell && fmtDate(cell) === todayStr,
                start: cell && fmtDate(cell) === startDate,
                end:   cell && fmtDate(cell) === endDate,
                inrange: cell && isInRange(cell),
                disabled: cell && isPastOrInvalid(cell),
              }"
              :disabled="!cell || isPastOrInvalid(cell)"
              @click="cell && pickDate(cell)"
            >
              <span v-if="cell">{{ cell.getDate() }}</span>
            </button>
          </div>
          <div class="cal-selected-row">
            <span class="cal-sel-item">
              <span class="cal-sel-label">출발</span>
              <span class="cal-sel-val">{{ formatDisplay(startDate) }}</span>
            </span>
            <span class="cal-sel-arrow">→</span>
            <span class="cal-sel-item">
              <span class="cal-sel-label">도착</span>
              <span class="cal-sel-val">{{ formatDisplay(endDate) }}</span>
            </span>
            <span class="cal-sel-nights">{{ nights }}박</span>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/api/http.js'
import { planApi } from '@/api/index.js'

const router = useRouter()

// 뒤로가기 — 직접 진입(history 없음) 시 홈으로 안전 폴백
function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

// ── 지역 ──────────────────────────────────────────────────────────────────────
const areas = ref([])
const areasLoading = ref(true)
const selectedAreaCode = ref('')

const AREA_SHORT = {
  '1':'서울','2':'인천','3':'대전','4':'대구','5':'광주',
  '6':'부산','7':'울산','8':'세종',
  '31':'경기','32':'강원','33':'충북','34':'충남',
  '35':'경북','36':'경남','37':'전북','38':'전남','39':'제주',
}
function shortName(area) {
  return AREA_SHORT[String(area.code)] ?? area.name
}

onMounted(async () => {
  try {
    const { data } = await http.get('/api/attractions/areas')
    areas.value = (Array.isArray(data) ? data : []).map(a => ({ ...a, name: shortName(a) }))
    if (areas.value.length) selectedAreaCode.value = areas.value[0].code
  } catch {
    areas.value = [
      { code: '1',  name: '서울' },
      { code: '2',  name: '인천' },
      { code: '3',  name: '대전' },
      { code: '4',  name: '대구' },
      { code: '5',  name: '광주' },
      { code: '6',  name: '부산' },
      { code: '7',  name: '울산' },
      { code: '8',  name: '세종' },
      { code: '31', name: '경기' },
      { code: '32', name: '강원' },
      { code: '33', name: '충북' },
      { code: '34', name: '충남' },
      { code: '35', name: '경북' },
      { code: '36', name: '경남' },
      { code: '37', name: '전북' },
      { code: '38', name: '전남' },
      { code: '39', name: '제주' },
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

// ── 캘린더 팝업 ───────────────────────────────────────────────────────────────
const calOpen = ref(null)   // 'start' | 'end' | null
const calYear = ref(0)
const calMonth = ref(0)

function openCal(which) {
  const base = which === 'start' ? new Date(startDate.value) : new Date(endDate.value)
  calYear.value  = base.getFullYear()
  calMonth.value = base.getMonth()
  calOpen.value  = which
}

function shiftMonth(delta) {
  let m = calMonth.value + delta
  let y = calYear.value
  if (m < 0)  { m = 11; y-- }
  if (m > 11) { m = 0;  y++ }
  calMonth.value = m
  calYear.value  = y
}

const calCells = computed(() => {
  const y = calYear.value
  const m = calMonth.value
  const firstDow = new Date(y, m, 1).getDay()          // 0=일
  const daysInMonth = new Date(y, m + 1, 0).getDate()
  const cells = []
  for (let i = 0; i < firstDow; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(y, m, d))
  return cells
})

function isPastOrInvalid(date) {
  const str = fmtDate(date)
  if (str < todayStr) return true
  if (calOpen.value === 'end' && str <= startDate.value) return true
  return false
}

function isInRange(date) {
  const str = fmtDate(date)
  return str > startDate.value && str < endDate.value
}

function pickDate(date) {
  const str = fmtDate(date)
  if (calOpen.value === 'start') {
    startDate.value = str
    if (endDate.value <= str) nights.value = 1
  } else {
    const diff = Math.round((date - new Date(startDate.value)) / 86400000)
    if (diff >= 1) nights.value = diff
  }
  calOpen.value = null
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
const companionCount = ref(3)
const showCountStepper = computed(() =>
  selectedCompanion.value === 'family' || selectedCompanion.value === 'friend'
)

// ── 예산 ──────────────────────────────────────────────────────────────────────
const budgetOptions = [
  { label: '10만원 이하',  value: 100_000   },
  { label: '10~50만원',   value: 500_000   },
  { label: '50~100만원',  value: 1_000_000 },
  { label: '100만원 이상', value: 2_000_000 },
  { label: '상관 없음',   value: null       },
]
const selectedBudget = ref('10~50만원')

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

// ── 일정 제목 ─────────────────────────────────────────────────────────────────
const planTitle = ref('')

// ── 에러 ──────────────────────────────────────────────────────────────────────
const localError = ref('')

// ── 빈 계획 생성 진행 상태 ────────────────────────────────────────────────────
const creatingEmpty = ref(false)

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
    ...(showCountStepper.value ? { companionCount: companionCount.value } : {}),
    themes: selectedThemes.value.length ? [...selectedThemes.value] : undefined,
    periodValid: true,
  }
  if (budgetOpt?.value != null) conditions.budget = budgetOpt.value
  if (budgetOpt) conditions.budgetLabel = budgetOpt.label
  if (planTitle.value.trim()) conditions.title = planTitle.value.trim()

  // Pass conditions to /ai via History state so AiInputView can read them
  router.push({ name: 'ai-input', state: { conditions } })
}

// ── AI 없이 빈 계획 만들기 ─────────────────────────────────────────────────────
// 폼 설정(제목/지역/날짜/동행/예산)만으로 빈 일정을 생성한다. AI 호출 없음.
// 서버는 날짜 범위만큼 빈 TripDay를 자동 생성한다.
async function handleCreateEmpty() {
  if (creatingEmpty.value) return

  // 최소 필수값 검증: 제목은 선택이라 미입력 시 기본 제목으로 보완
  if (!startDate.value || !endDate.value) {
    localError.value = '여행 날짜를 선택해 주세요.'
    return
  }
  localError.value = ''

  const companion = companionTypes.find(c => c.key === selectedCompanion.value)
  const budgetOpt = budgetOptions.find(o => o.label === selectedBudget.value)

  // 제목 미입력 시 기본값(지역명 + "여행")으로 보완 — title은 BE 필수값
  const area = areas.value.find(a => a.code === selectedAreaCode.value)
  const fallbackTitle = `${area?.name ?? ''} 여행`.trim() || '내 여행'
  const title = (planTitle.value.trim() || fallbackTitle).slice(0, 100)

  const payload = {
    title,
    startDate: startDate.value,
    endDate: endDate.value,
    companions: companion?.apiVal ?? null,           // 'SOLO'|'COUPLE'|'FAMILY'|'FRIENDS'
    budget: budgetOpt?.value ?? null,                // 정수 또는 null
    origin: 'MANUAL',
  }

  creatingEmpty.value = true
  try {
    await planApi.createPlan(payload)
    router.push('/plan')
  } catch (e) {
    localError.value = e?.response?.data?.message || '빈 계획을 만들지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    creatingEmpty.value = false
  }
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

.ai-back-btn {
  position: relative;
  z-index: 2;
  width: 36px;
  height: 36px;
  margin: 0 0 8px -8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  cursor: pointer;
  transition: background 0.15s;
}

.ai-back-btn:active {
  background: rgba(255, 255, 255, 0.22);
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

/* ── Title input ─────────────────────────────────────────────────────────── */
.title-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-line-light);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.title-input::placeholder {
  color: var(--color-ink-muted);
}

.title-input:focus {
  border-color: var(--color-peach);
}

/* ── Region grid ─────────────────────────────────────────────────────────── */
.region-loading {
  font-size: 13px;
  color: var(--color-ink-muted);
}

.region-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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
  cursor: pointer;
  transition: border-color 0.15s;
}

.date-picker.active,
.date-picker:active {
  border-color: var(--color-peach);
  background: var(--color-peach-light);
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

/* ── Companion count stepper ─────────────────────────────────────────────── */
.companion-count-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-line-light);
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

/* 빈 계획 만들기 — AI 버튼 대비 보조(secondary) 스타일 */
.empty-btn {
  width: 100%;
  height: 46px;
  background: var(--color-white);
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-xl);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.3px;
  cursor: pointer;
  transition: background 0.15s, opacity 0.2s;
}

.empty-btn:active {
  background: var(--color-surface);
}

.empty-btn:disabled {
  opacity: 0.55;
  cursor: default;
}

.ai-btn-sub {
  text-align: center;
  font-size: 12px;
  color: var(--color-ink-muted);
}

/* ── Calendar popup ──────────────────────────────────────────────────────── */
.cal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 9999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.cal-popup {
  width: 100%;
  max-width: 480px;
  background: var(--color-white);
  border-radius: 20px 20px 0 0;
  padding: 0 16px 32px;
  animation: slideUp 0.22s ease;
}

@keyframes slideUp {
  from { transform: translateY(100%); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}

.cal-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 8px;
  border-bottom: 1px solid var(--color-line-light);
  margin-bottom: 8px;
}

.cal-picking {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
}

.cal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--color-ink-secondary);
  background: var(--color-surface);
  cursor: pointer;
}

.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.cal-nav-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--color-ink-secondary);
  background: var(--color-surface);
  cursor: pointer;
  transition: background 0.15s;
}

.cal-nav-btn:active { background: var(--color-line); }

.cal-month-label {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 4px;
}

.cal-weekdays span {
  text-align: center;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
  padding: 4px 0;
}

.cal-weekdays span:first-child { color: #e55; }
.cal-weekdays span:last-child  { color: #36c; }

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.cal-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink);
  background: transparent;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.cal-cell.empty {
  pointer-events: none;
}

.cal-cell.today {
  font-weight: 800;
  color: var(--color-peach);
}

.cal-cell.start,
.cal-cell.end {
  background: var(--color-peach);
  color: #fff;
  font-weight: 700;
}

.cal-cell.inrange {
  background: var(--color-peach-light);
  border-radius: 0;
  color: var(--color-peach-pressed);
}

.cal-cell.disabled {
  opacity: 0.3;
  cursor: default;
  pointer-events: none;
}

.cal-cell:not(.empty):not(.disabled):not(.start):not(.end):hover {
  background: var(--color-surface);
}

.cal-selected-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 16px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}

.cal-sel-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.cal-sel-label {
  font-size: 11px;
  color: var(--color-ink-muted);
  font-weight: 500;
}

.cal-sel-val {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
}

.cal-sel-arrow {
  color: var(--color-ink-muted);
  font-size: 14px;
}

.cal-sel-nights {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-peach);
  background: var(--color-peach-light);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  margin-left: 4px;
}
</style>
