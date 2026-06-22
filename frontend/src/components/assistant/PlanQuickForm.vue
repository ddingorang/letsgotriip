<!-- Created: 2026-06-19 -->
<!--
  AssistantView 전용 "여행 계획 세우기" 퀵 폼(바텀시트).
  - AiPlanInputView 의 옵션 정의(지역/동행/예산/테마)를 동일하게 재사용한다.
  - 제출 시 recommendApi.create 가 받는 조건 객체({ areaCode, startDate, endDate,
    companions, budget?, themes? })를 emit('submit', conditions) 로 부모에 넘긴다.
  - 생성 호출/로딩/에러 표시는 부모(AssistantView)가 담당한다(여기선 입력 수집만).
-->
<template>
  <div class="sheet-backdrop" @click.self="emitClose">
    <div class="sheet" role="dialog" aria-label="여행 계획 세우기">
      <div class="sheet-handle" @click="emitClose" />

      <div class="sheet-head">
        <h2 class="sheet-title">✈️ 여행 계획 세우기</h2>
        <p class="sheet-sub">조건을 고르면 AI가 일정을 만들어 드려요</p>
      </div>

      <div class="sheet-body">
        <!-- 지역 -->
        <section class="field">
          <div class="field-label">어디로 가세요?</div>
          <div v-if="areasLoading" class="field-loading">지역 목록을 불러오는 중...</div>
          <div v-else class="region-grid">
            <button
              v-for="area in areas"
              :key="area.code"
              type="button"
              class="region-btn"
              :class="{ selected: selectedAreaCode === area.code }"
              @click="selectedAreaCode = area.code"
            >{{ area.name }}</button>
          </div>
        </section>

        <!-- 날짜 -->
        <section class="field">
          <div class="field-label">언제 떠나세요?</div>
          <div class="date-row">
            <label class="date-box">
              <span class="date-cap">출발일</span>
              <input v-model="startDate" type="date" class="date-native" :min="todayStr" />
              <span class="date-val">{{ formatDisplay(startDate) }}</span>
            </label>
            <span class="date-arrow">→</span>
            <div class="date-box static">
              <span class="date-cap">도착일</span>
              <span class="date-val">{{ formatDisplay(endDate) }}</span>
            </div>
          </div>
          <div class="night-row">
            <span class="night-label">숙박 일수</span>
            <div class="night-ctrl">
              <button type="button" class="night-btn" :disabled="nights <= 1" @click="nights > 1 && nights--">−</button>
              <span class="night-val">{{ nights }}박</span>
              <button type="button" class="night-btn" :disabled="nights >= 7" @click="nights < 7 && nights++">＋</button>
            </div>
          </div>
        </section>

        <!-- 동행 -->
        <section class="field">
          <div class="field-label">누구와 함께 가나요?</div>
          <div class="chip-row">
            <button
              v-for="c in companionTypes"
              :key="c.key"
              type="button"
              class="chip"
              :class="{ selected: selectedCompanion === c.key }"
              @click="selectedCompanion = c.key"
            >{{ c.label }}</button>
          </div>
        </section>

        <!-- 예산 -->
        <section class="field">
          <div class="field-label">예산은 얼마나 되세요?</div>
          <div class="chip-row">
            <button
              v-for="opt in budgetOptions"
              :key="opt.label"
              type="button"
              class="chip"
              :class="{ selected: selectedBudget === opt.label }"
              @click="selectedBudget = opt.label"
            >{{ opt.label }}</button>
          </div>
        </section>

        <!-- 테마 -->
        <section class="field">
          <div class="field-label">
            어떤 여행을 원하세요?
            <span class="field-hint">(복수 선택)</span>
          </div>
          <div class="theme-grid">
            <button
              v-for="t in themes"
              :key="t.key"
              type="button"
              class="theme-btn"
              :class="{ selected: selectedThemes.includes(t.key) }"
              @click="toggleTheme(t.key)"
            >
              <span class="theme-icon">{{ t.icon }}</span>
              <span class="theme-label">{{ t.label }}</span>
            </button>
          </div>
        </section>
      </div>

      <div class="sheet-foot">
        <div v-if="localError" class="foot-error">{{ localError }}</div>
        <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
          <template v-if="loading">
            <span class="spinner" />
            일정 생성 중...
          </template>
          <template v-else>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
            </svg>
            AI 일정 만들기
          </template>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { attractionApi } from '@/api/index.js'

const props = defineProps({
  // 부모가 생성 호출 진행 중이면 true — 버튼 잠금/스피너 표시
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'close'])

function emitClose() {
  if (props.loading) return // 생성 중에는 닫기 방지
  emit('close')
}

// ── 지역 (AiPlanInputView 와 동일한 로드/폴백) ──────────────────────────────────
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
    const { data } = await attractionApi.areas()
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

// ── 날짜 (AiPlanInputView 와 동일한 계산식) ─────────────────────────────────────
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
  const weekdays = ['일', '월', '화', '수', '목', '금', '토']
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} (${weekdays[d.getDay()]})`
}

// ── 동행 (AiPlanInputView 와 동일한 apiVal 매핑: SOLO/COUPLE/FAMILY/FRIENDS) ─────
const companionTypes = [
  { key: 'solo', apiVal: 'SOLO', label: '혼자' },
  { key: 'couple', apiVal: 'COUPLE', label: '커플' },
  { key: 'family', apiVal: 'FAMILY', label: '가족' },
  { key: 'friend', apiVal: 'FRIENDS', label: '친구' },
]
const selectedCompanion = ref('couple')

// ── 예산 (AiPlanInputView 와 동일한 값) ─────────────────────────────────────────
const budgetOptions = [
  { label: '10만원 이하', value: 100_000 },
  { label: '10~30만원', value: 300_000 },
  { label: '30~50만원', value: 500_000 },
  { label: '50만원 이상', value: 1_000_000 },
  { label: '상관없음', value: null },
]
const selectedBudget = ref('10~30만원')

// ── 테마 (AiPlanInputView 와 동일한 key — BE THEME_LABELS 단일 소스) ────────────
const themes = [
  { key: 'sea', icon: '🌊', label: '바다/해변' },
  { key: 'mountain', icon: '🏔️', label: '산/자연' },
  { key: 'food', icon: '🍽️', label: '맛집 투어' },
  { key: 'history', icon: '🏛️', label: '역사/문화' },
  { key: 'activity', icon: '🎢', label: '액티비티' },
  { key: 'shopping', icon: '🛍️', label: '쇼핑' },
]
const selectedThemes = ref(['sea', 'food'])

function toggleTheme(key) {
  const idx = selectedThemes.value.indexOf(key)
  if (idx === -1) selectedThemes.value.push(key)
  else selectedThemes.value.splice(idx, 1)
}

// ── 제출 ────────────────────────────────────────────────────────────────────────
const localError = ref('')

function handleSubmit() {
  if (props.loading) return
  if (!selectedAreaCode.value) {
    localError.value = '지역을 선택해 주세요.'
    return
  }
  localError.value = ''

  const companion = companionTypes.find((c) => c.key === selectedCompanion.value)
  const budgetOpt = budgetOptions.find((o) => o.label === selectedBudget.value)

  // recommendApi.create 가 받는 조건 객체(AiInputView.handleGenerate 의 payload 와 동일 형태)
  const conditions = {
    areaCode: selectedAreaCode.value,
    startDate: startDate.value,
    endDate: endDate.value,
    companions: companion?.apiVal,
    themes: selectedThemes.value.length ? [...selectedThemes.value] : undefined,
    periodValid: true,
  }
  if (budgetOpt?.value != null) conditions.budget = budgetOpt.value

  // 사람이 읽을 요약(말풍선용) — 부모가 사용자 메시지로 표시
  const areaName = areas.value.find((a) => a.code === selectedAreaCode.value)?.name ?? selectedAreaCode.value
  const themeLabels = selectedThemes.value
    .map((k) => themes.find((t) => t.key === k)?.label)
    .filter(Boolean)
  const summary =
    `✈️ ${areaName} · ${startDate.value} ~ ${endDate.value} (${nights.value}박)` +
    ` · ${companion?.label ?? ''}` +
    (budgetOpt?.value != null ? ` · 예산 ${selectedBudget.value}` : '') +
    (themeLabels.length ? ` · ${themeLabels.join('/')}` : '')

  emit('submit', { conditions, summary })
}
</script>

<style scoped>
.sheet-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 20, 40, 0.45);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
  animation: fade-in 0.18s ease;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sheet {
  width: 100%;
  max-height: 88%;
  background: var(--color-white);
  border-radius: 20px 20px 0 0;
  display: flex;
  flex-direction: column;
  box-shadow: 0 -6px 24px rgba(0, 0, 0, 0.12);
  animation: slide-up 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-handle {
  width: 36px;
  height: 4px;
  background: var(--color-line);
  border-radius: var(--radius-full);
  margin: 10px auto 4px;
  cursor: pointer;
  flex-shrink: 0;
}

.sheet-head {
  padding: 8px 20px 12px;
  flex-shrink: 0;
}

.sheet-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.sheet-sub {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-top: 4px;
}

.sheet-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-label {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.field-hint {
  font-size: 11.5px;
  font-weight: 400;
  color: var(--color-ink-muted);
}

.field-loading {
  font-size: 13px;
  color: var(--color-ink-muted);
}

/* ── 지역 ─────────────────────────────────────────────────────────────────── */
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
  transition: all 0.15s;
}

.region-btn.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
  color: var(--color-peach-pressed);
  font-weight: 700;
}

/* ── 날짜 ─────────────────────────────────────────────────────────────────── */
.date-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.date-box {
  flex: 1;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  border: 1.5px solid var(--color-line-light);
  position: relative;
  display: block;
}

.date-cap {
  display: block;
  font-size: 10.5px;
  color: var(--color-ink-muted);
  margin-bottom: 4px;
  font-weight: 500;
}

.date-val {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.date-native {
  position: absolute;
  inset: 0;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.date-box.static {
  background: var(--color-surface-alt);
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
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-secondary);
}

.night-ctrl {
  display: flex;
  align-items: center;
  gap: 14px;
}

.night-btn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  line-height: 1;
}

.night-btn:disabled {
  opacity: 0.35;
}

.night-val {
  font-size: 16px;
  font-weight: 800;
  color: var(--color-ink);
  min-width: 34px;
  text-align: center;
}

/* ── 칩(동행/예산) ─────────────────────────────────────────────────────────── */
.chip-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  padding: 8px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line);
  white-space: nowrap;
  transition: all 0.15s;
}

.chip.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
  color: var(--color-peach-pressed);
  font-weight: 700;
}

/* ── 테마 ─────────────────────────────────────────────────────────────────── */
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
  transition: all 0.15s;
}

.theme-btn.selected {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
}

.theme-icon {
  font-size: 20px;
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

/* ── 풋(제출) ─────────────────────────────────────────────────────────────── */
.sheet-foot {
  padding: 12px 20px calc(14px + var(--safe-bottom));
  border-top: 1px solid var(--color-line-light);
  background: var(--color-white);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.foot-error {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-error);
  text-align: center;
}

.submit-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: #fff;
  border-radius: var(--radius-xl);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 4px 14px rgba(247, 143, 87, 0.32);
  transition: opacity 0.2s;
}

.submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinner {
  width: 17px;
  height: 17px;
  border: 2.5px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
</style>
