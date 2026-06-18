<template>
  <div class="page">
    <header class="plan-header">
      <h1 class="header-title">내 여행 계획</h1>
      <button class="add-btn" @click="goNewTrip" title="새 여행 만들기">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </header>

    <div class="scroll-content">
      <!-- ── Empty state ──────────────────────────────────────────────── -->
      <div v-if="plans.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18" />
          </svg>
        </div>
        <p class="empty-title">아직 여행 계획이 없어요</p>
        <p class="empty-sub">AI가 최적의 일정을 만들어 드려요</p>
        <button class="create-ai-btn" @click="goNewTrip">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          새 여행 만들기 (AI)
        </button>
      </div>

      <!-- ── Plan list ────────────────────────────────────────────────── -->
      <div v-else class="plan-list">
        <div
          v-for="plan in plans"
          :key="plan.id"
          class="plan-card"
          :class="{ expanded: selectedPlanId === plan.id }"
        >
          <!-- Card header — always visible, click to expand/collapse -->
          <div class="plan-thumb" @click="togglePlan(plan)">
            <div class="thumb-gradient" />
            <div class="plan-dates">
              <span class="date-label">{{ formatDate(plan.startDate) }}</span>
              <span class="date-sep">–</span>
              <span class="date-label">{{ formatDate(plan.endDate) }}</span>
            </div>
          </div>
          <div class="plan-info" @click="togglePlan(plan)">
            <h3 class="plan-name">{{ plan.title }}</h3>
            <p class="plan-sub">{{ plan.destination }} · {{ dayCount(plan.startDate, plan.endDate) }}박 {{ dayCount(plan.startDate, plan.endDate) + 1 }}일</p>
            <div class="plan-spots">
              <span v-for="spot in plan.spots?.slice(0, 3)" :key="spot" class="spot-chip">{{ spot }}</span>
            </div>
          </div>

          <!-- Expanded detail panel -->
          <Transition name="expand">
            <div v-if="selectedPlanId === plan.id" class="plan-detail">
              <div class="detail-divider" />

              <!-- Days list from planStore.current if loaded -->
              <div v-if="planStore.loading" class="detail-loading">불러오는 중...</div>
              <template v-else-if="planStore.current?.id === plan.id">
                <div
                  v-for="day in planStore.current.days ?? []"
                  :key="day.dayNo"
                  class="detail-day"
                >
                  <div class="detail-day-header">
                    <span class="detail-day-pill">{{ day.dayNo }}일차</span>
                    <span v-if="day.summary" class="detail-day-summary">{{ day.summary }}</span>
                  </div>
                  <div class="detail-places">
                    <div
                      v-for="(place, idx) in day.places ?? []"
                      :key="place.id ?? idx"
                      class="detail-place-row"
                    >
                      <span class="detail-place-num">{{ idx + 1 }}</span>
                      <span class="detail-place-name">{{ place.attraction?.title ?? place.title ?? '장소' }}</span>
                      <span v-if="place.visitTime" class="detail-place-time">{{ place.visitTime }}</span>
                    </div>
                    <div v-if="!(day.places?.length)" class="detail-empty">일정이 없어요</div>
                  </div>
                </div>
                <div v-if="!(planStore.current.days?.length)" class="detail-empty-plan">
                  아직 일정이 없어요. AI로 동선을 최적화해보세요!
                </div>
              </template>
              <div v-else class="detail-loading">상세 정보를 불러오는 중...</div>

              <!-- Action buttons -->
              <div class="detail-actions">
                <button class="detail-action-btn optimize-btn" @click="goReport(plan.id)">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="3"/>
                    <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
                  </svg>
                  AI 동선 최적화
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>

      <!-- ── Companion section ────────────────────────────────────────── -->
      <div class="companion-section">
        <div class="section-header">
          <h2 class="section-title">동행 구하기</h2>
          <button class="see-all" @click="router.push({ path: '/community', query: { tab: 'companion' } })">전체보기</button>
        </div>
        <div class="companion-list">
          <div v-for="comp in companions" :key="comp.id" class="companion-card">
            <div class="comp-header">
              <span class="comp-badge">{{ comp.category }}</span>
              <span class="comp-dday" :class="{ urgent: comp.dday <= 3 }">D-{{ comp.dday }}</span>
            </div>
            <h4 class="comp-title">{{ comp.title }}</h4>
            <p class="comp-sub">{{ comp.destination }} · {{ comp.dates }}</p>
            <div class="comp-footer">
              <div class="comp-members">
                <div v-for="i in comp.currentCount" :key="i" class="member-dot" />
                <span class="member-text">{{ comp.currentCount }}/{{ comp.maxCount }}명</span>
              </div>
              <button class="join-btn" @click="router.push({ path: '/community', query: { tab: 'companion' } })">참여하기</button>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePlanStore } from '@/stores/plan.js'

const router = useRouter()
const planStore = usePlanStore()
const plans = planStore.plans

const selectedPlanId = ref(null)

const companions = ref([
  { id: 1, category: '관광', title: '제주 동부 일주 같이 해요!', destination: '제주도', dates: '12월 28-30일', currentCount: 2, maxCount: 4, dday: 5 },
  { id: 2, category: '맛집', title: '부산 로컬 맛집 투어 동행 구해요', destination: '부산', dates: '1월 4-5일', currentCount: 1, maxCount: 3, dday: 12 },
  { id: 3, category: '액티비티', title: '한라산 백록담 등반 파티 모집', destination: '제주도', dates: '1월 10일', currentCount: 3, maxCount: 6, dday: 2 },
])

onMounted(async () => {
  try {
    await planStore.loadPlans()
  } catch {
    // not logged in or BE unavailable — plans stays empty, user sees empty state
  }
})

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function dayCount(start, end) {
  if (!start || !end) return 0
  return Math.round((new Date(end) - new Date(start)) / 86400000)
}

/** Navigate to /ai/plan (new AI trip flow) */
function goNewTrip() {
  router.push('/ai/plan')
}

/** Expand/collapse a plan card and load its detail */
async function togglePlan(plan) {
  if (selectedPlanId.value === plan.id) {
    selectedPlanId.value = null
    return
  }
  selectedPlanId.value = plan.id
  // Load plan detail if not already loaded or stale
  if (planStore.current?.id !== plan.id) {
    try {
      await planStore.loadPlan(plan.id)
    } catch {
      // error shown via planStore.error
    }
  }
}

/** Navigate to the 동선 리포트 screen */
function goReport(planId) {
  router.push(`/plan/${planId}/report`)
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

.plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 20px 16px;
}

.header-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
}

.add-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-peach);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}

/* ── Empty state ─────────────────────────────────────────────────────────── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0 40px;
  gap: 8px;
}

.empty-icon {
  margin-bottom: 8px;
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.empty-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  margin-bottom: 16px;
}

.create-ai-btn {
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: white;
  font-size: 14px;
  font-weight: 700;
  padding: 13px 28px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 14px rgba(247, 143, 87, 0.3);
}

/* ── Plan list ────────────────────────────────────────────────────────────── */
.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 24px;
}

.plan-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.plan-thumb {
  height: 100px;
  background: linear-gradient(135deg, #f78f57 0%, #e0743a 100%);
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 12px 16px;
}

.thumb-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.25) 100%);
}

.plan-dates {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 1;
}

.date-label {
  font-size: 13px;
  font-weight: 600;
  color: white;
}

.date-sep {
  color: rgba(255, 255, 255, 0.7);
}

.plan-info {
  padding: 14px 16px 16px;
  background: var(--color-white);
}

.plan-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}

.plan-sub {
  font-size: 13px;
  color: var(--color-ink-muted);
  margin-bottom: 10px;
}

.plan-spots {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.spot-chip {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

/* ── Plan detail panel ────────────────────────────────────────────────────── */
.plan-detail {
  background: var(--color-surface);
  padding: 0 16px 16px;
  overflow: hidden;
}

.detail-divider {
  height: 1px;
  background: var(--color-line-light);
  margin-bottom: 14px;
}

.detail-loading {
  font-size: 13px;
  color: var(--color-ink-muted);
  text-align: center;
  padding: 12px 0;
}

.detail-day {
  margin-bottom: 14px;
}

.detail-day-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.detail-day-pill {
  background: var(--color-peach);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.detail-day-summary {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
}

.detail-places {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-place-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 9px 12px;
  box-shadow: var(--shadow-card);
}

.detail-place-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.detail-place-name {
  flex: 1;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.detail-place-time {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-peach);
  background: var(--color-peach-light);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}

.detail-empty {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  padding: 6px 0;
}

.detail-empty-plan {
  font-size: 13px;
  color: var(--color-ink-muted);
  text-align: center;
  padding: 8px 0 4px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.detail-action-btn {
  flex: 1;
  height: 42px;
  border-radius: var(--radius-xl);
  font-size: 13.5px;
  font-weight: 700;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  cursor: pointer;
}

.optimize-btn {
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: #fff;
  box-shadow: 0 3px 10px rgba(247, 143, 87, 0.3);
}

/* expand/collapse transition */
.expand-enter-active,
.expand-leave-active {
  transition: max-height 0.3s ease, opacity 0.25s ease;
  max-height: 600px;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

/* ── Companion section ────────────────────────────────────────────────────── */
.companion-section {
  padding-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 17px;
  font-weight: 750;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.see-all {
  font-size: 13px;
  color: var(--color-ink-muted);
}

.companion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.companion-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.comp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comp-badge {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}

.comp-dday {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-ink-muted);
}

.comp-dday.urgent {
  color: var(--color-error);
}

.comp-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}

.comp-sub {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 12px;
}

.comp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comp-members {
  display: flex;
  align-items: center;
  gap: 4px;
}

.member-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-peach);
}

.member-text {
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-left: 4px;
}

.join-btn {
  background: var(--color-peach);
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 16px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}

.bottom-spacer {
  height: 24px;
}
</style>
