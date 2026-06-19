# Created: 2026-06-16 14:06:20
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="nav-title">동행 모집</span>
      <button class="submit-top-btn" :disabled="!isValid" @click="submit">등록</button>
    </header>

    <div class="scroll-content">
      <div class="form-body">
        <!-- 연결할 계획 (선택) -->
        <div class="field">
          <label class="field-label">여행 계획 연결 <span class="optional">(선택)</span></label>
          <div v-if="plansLoading" class="plan-loading">계획을 불러오는 중…</div>
          <template v-else>
            <div v-if="myPlans.length" class="plan-select-list">
              <button
                v-for="plan in myPlans"
                :key="plan.id"
                :class="['plan-option', { active: form.planId === plan.id }]"
                @click="selectPlan(plan)"
              >
                <div class="plan-option-main">
                  <span class="plan-option-title">{{ plan.title || '제목 없는 계획' }}</span>
                  <span v-if="plan.startDate" class="plan-option-date">{{ planDateLabel(plan) }}</span>
                </div>
                <svg v-if="form.planId === plan.id" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
              </button>
            </div>
            <p v-else class="plan-empty">저장된 여행 계획이 없어요. 계획 없이도 모집할 수 있어요.</p>
            <button v-if="form.planId" class="plan-clear" @click="clearPlan">연결 해제</button>
          </template>
        </div>

        <!-- 제목 -->
        <div class="field">
          <label class="field-label">제목 <span class="req">*</span></label>
          <input v-model="form.title" class="field-input" placeholder="예) 제주 3박 4일 동행 구해요" />
        </div>

        <!-- 여행 지역 -->
        <div class="field">
          <label class="field-label">여행 지역 <span class="req">*</span></label>
          <div class="input-icon-wrap">
            <input v-model="form.location" class="field-input" placeholder="제주" />
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
          </div>
        </div>

        <!-- 여행 날짜 -->
        <div class="field">
          <label class="field-label">여행 날짜 <span class="req">*</span></label>
          <div class="date-range-row">
            <input v-model="form.startDate" type="date" class="field-input date-input" :min="todayStr" :max="maxDateStr" />
            <span class="date-sep">~</span>
            <input v-model="form.endDate" type="date" class="field-input date-input" :min="form.startDate || todayStr" :max="maxDateStr" />
          </div>
          <p v-if="computedDuration" class="duration-hint">{{ computedDuration }}</p>
        </div>

        <!-- 최대 인원 -->
        <div class="field">
          <label class="field-label">최대 인원 <span class="req">*</span></label>
          <div class="chips-row">
            <button
              v-for="n in personOptions"
              :key="n"
              :class="['chip-btn', { active: form.maxCount === n }]"
              @click="form.maxCount = n"
            >
              {{ n }}
            </button>
          </div>
        </div>

        <!-- 예상 비용 -->
        <div class="field">
          <label class="field-label">예상 비용 (1인)</label>
          <div class="input-icon-wrap">
            <input
              v-model.number="form.estimatedCost"
              type="number"
              min="0"
              step="10000"
              inputmode="numeric"
              class="field-input"
              placeholder="예) 300000"
            />
            <span class="cost-unit">원</span>
          </div>
        </div>

        <!-- 설명 -->
        <div class="field">
          <label class="field-label">설명</label>
          <textarea
            v-model="form.description"
            class="field-textarea"
            placeholder="여행 스타일, 원하는 동행, 일정 등을 자유롭게 적어주세요"
            rows="5"
          />
        </div>

        <!-- 태그 -->
        <div class="field">
          <label class="field-label">태그</label>
          <div class="chips-row">
            <button
              v-for="tag in availableTags"
              :key="tag"
              :class="['chip-btn tag-chip', { active: form.tags.includes(tag) }]"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <!-- Error message -->
        <p v-if="submitError" class="submit-error">{{ submitError }}</p>

        <!-- Info banner -->
        <div class="info-banner">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-secondary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
          <p class="info-text">모집글을 등록하면 그룹 채팅방이 자동 생성돼요. 승인한 멤버만 입장할 수 있어요.</p>
        </div>

        <div style="height: 32px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'
import { useAuthStore } from '@/stores/auth.js'
import { planApi } from '@/api/index.js'

const route = useRoute()
const router = useRouter()
const companionStore = useCompanionStore()
const authStore = useAuthStore()

const personOptions = ['2명', '3명', '4명', '5명', '6명+']
const availableTags = ['#20대', '#30대', '#뚜벅이', '#드라이브', '#맛집투어', '#혼행환영', '#가족여행']

const submitting = ref(false)
const form = ref({
  title: '',
  location: '',
  startDate: '',
  endDate: '',
  maxCount: null,
  description: '',
  estimatedCost: null,
  tags: [],
  planId: null,   // 연결할 여행 계획 ID (선택). null이면 미연결
})

// ── 내 여행 계획 (연결용) ───────────────────────────────────────────────────
const myPlans = ref([])
const plansLoading = ref(false)

function planDateLabel(plan) {
  if (!plan.startDate) return ''
  return plan.endDate && plan.endDate !== plan.startDate
    ? `${plan.startDate} ~ ${plan.endDate}`
    : plan.startDate
}

// 계획 선택 시 비어 있는 폼 필드를 계획 정보로 채워준다(사용자 입력은 덮어쓰지 않음)
function selectPlan(plan) {
  form.value.planId = plan.id
  if (!form.value.title && plan.title) form.value.title = plan.title
  if (!form.value.startDate && plan.startDate) form.value.startDate = plan.startDate
  if (!form.value.endDate && plan.endDate) form.value.endDate = plan.endDate
}

function clearPlan() {
  form.value.planId = null
}

onMounted(async () => {
  plansLoading.value = true
  try {
    const { data } = await planApi.getMyPlans()
    const items = Array.isArray(data) ? data : (data?.content ?? [])
    myPlans.value = items
    // AiResultView 등에서 query.planId 로 넘어온 계획을 자동 선택
    const presetId = route.query.planId != null ? Number(route.query.planId) : null
    if (presetId != null && !Number.isNaN(presetId)) {
      const preset = items.find(p => Number(p.id) === presetId)
      if (preset) selectPlan(preset)
      else form.value.planId = presetId // 목록에 없어도 연결 의도는 보존(BE가 소유 검증)
    }
  } catch {
    myPlans.value = []
  } finally {
    plansLoading.value = false
  }
})

const computedDuration = computed(() => {
  if (!form.value.startDate || !form.value.endDate) return ''
  const start = new Date(form.value.startDate)
  const end = new Date(form.value.endDate)
  const nights = Math.round((end - start) / (1000 * 60 * 60 * 24))
  if (nights < 0) return ''
  if (nights === 0) return '당일'
  return `${nights}박 ${nights + 1}일`
})

// 종료일이 시작일보다 앞서면 자동 보정
watch(() => form.value.startDate, (newStart) => {
  if (form.value.endDate && form.value.endDate < newStart) {
    form.value.endDate = newStart
  }
})

const submitError = ref('')
const todayStr = new Date().toISOString().slice(0, 10)
const maxDateStr = new Date(Date.now() + 1000 * 60 * 60 * 24 * 730).toISOString().slice(0, 10) // 오늘+약 2년
const isValid = computed(() =>
  form.value.title &&
  form.value.location &&
  form.value.startDate &&
  form.value.endDate &&
  form.value.maxCount
)

function toggleTag(tag) {
  const idx = form.value.tags.indexOf(tag)
  if (idx === -1) form.value.tags.push(tag)
  else form.value.tags.splice(idx, 1)
}

// Parse maxCount string like '4명' → 4
function parseMax(val) {
  if (!val) return 4
  const n = parseInt(val)
  return isNaN(n) ? 4 : n
}

// 날짜 검증 — 5자리 연도 등 잘못된 값으로 BE LocalDate 파싱(500)이 깨지는 것 방지
function isSaneDate(s) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false
  const d = new Date(s + 'T00:00:00')
  return !isNaN(d.getTime()) && d.getFullYear() >= 2020 && d.getFullYear() <= 2100
}

async function submit() {
  if (!isValid.value) return
  submitError.value = ''
  if (!isSaneDate(form.value.startDate) || !isSaneDate(form.value.endDate)) {
    submitError.value = '여행 날짜를 올바르게 선택해 주세요 (예: 2026-07-01).'
    return
  }
  if (form.value.endDate < form.value.startDate) {
    submitError.value = '종료일이 시작일보다 빠를 수 없어요.'
    return
  }
  const payload = {
    title: form.value.title,
    region: form.value.location,
    travelDate: form.value.startDate,
    duration: computedDuration.value || '미정',
    maxMembers: parseMax(form.value.maxCount),
    estimatedCost: Number.isFinite(form.value.estimatedCost) && form.value.estimatedCost > 0
      ? Math.trunc(form.value.estimatedCost)
      : 0,
    description: form.value.description,
    tags: form.value.tags,
    // 연결된 계획이 있으면 함께 전송. null이면 BE는 미연결로 처리(하위호환).
    planId: form.value.planId ?? null,
  }
  try {
    const created = await companionStore.create(payload)
    if (created?.id) {
      router.push({ name: 'companion-applicants', params: { id: created.id } })
    } else {
      router.back()
    }
  } catch {
    submitError.value = companionStore.error || '등록에 실패했어요. 잠시 후 다시 시도해 주세요.'
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
.submit-top-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
  transition: opacity 0.15s;
}
.submit-top-btn:disabled { opacity: 0.4; }

.scroll-content { flex: 1; overflow-y: auto; }
.form-body { padding: 20px 16px; display: flex; flex-direction: column; gap: 24px; }

.field { display: flex; flex-direction: column; gap: 8px; }
.field-label { font-size: 13.5px; font-weight: 600; color: var(--color-ink); }
.req { color: var(--color-peach); }

.row-two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.input-icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.input-icon-wrap .field-input { padding-right: 36px; }
.input-icon-wrap svg {
  position: absolute;
  right: 12px;
  pointer-events: none;
}
.cost-unit {
  position: absolute;
  right: 14px;
  font-size: 14px;
  color: var(--color-ink-muted);
  pointer-events: none;
}

.field-input {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: var(--color-peach); }
.field-input::placeholder { color: var(--color-ink-muted); }

.field-textarea {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  resize: none;
  line-height: 1.6;
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-textarea:focus { border-color: var(--color-peach); }
.field-textarea::placeholder { color: var(--color-ink-muted); }

.date-input { color-scheme: light; }

.date-range-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.date-range-row .field-input { flex: 1; }
.date-sep {
  font-size: 16px;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.duration-hint {
  font-size: 12.5px;
  color: var(--color-peach);
  font-weight: 600;
  margin-top: 4px;
}

.chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  transition: all 0.15s;
}
.chip-btn.active {
  background: var(--color-peach);
  color: white;
  border-color: var(--color-peach);
}
.tag-chip { font-size: 13px; }

.optional { color: var(--color-ink-muted); font-weight: 500; font-size: 12px; }

.plan-loading,
.plan-empty {
  font-size: 13px;
  color: var(--color-ink-muted);
  padding: 4px 2px;
  line-height: 1.5;
}

.plan-select-list { display: flex; flex-direction: column; gap: 8px; }
.plan-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  background: var(--color-surface);
  border: 1.5px solid transparent;
  border-radius: var(--radius-md);
  text-align: left;
  transition: border-color 0.15s, background 0.15s;
}
.plan-option.active {
  border-color: var(--color-peach);
  background: var(--color-peach-light);
}
.plan-option-main { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.plan-option-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plan-option-date { font-size: 12px; color: var(--color-ink-muted); }
.plan-clear {
  align-self: flex-start;
  margin-top: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
  text-decoration: underline;
}

.info-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: #f0f7f0;
  border-radius: var(--radius-md);
  border: 1px solid #c8e0c8;
}
.info-text { font-size: 13px; color: var(--color-ink-secondary); line-height: 1.55; letter-spacing: -0.2px; }

.submit-error {
  font-size: 13px;
  color: #e53e3e;
  padding: 10px 14px;
  background: #fff5f5;
  border: 1px solid #fed7d7;
  border-radius: var(--radius-md);
}
</style>
