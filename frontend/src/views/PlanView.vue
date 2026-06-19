<template>
  <div class="page">
    <header class="plan-header">
      <h1 class="header-title">내 여행 계획</h1>
      <div class="header-actions">
        <button
          v-if="plans.length >= 2"
          class="compare-toggle"
          :class="{ active: compareMode }"
          @click="toggleCompareMode"
          title="두 계획 비교"
        >
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="20" x2="18" y2="10" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="6" y1="20" x2="6" y2="14" />
          </svg>
        </button>
        <button class="add-btn" @click="goNewTrip" title="새 여행 만들기">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </button>
      </div>
    </header>

    <!-- 비교 모드 안내 배너 -->
    <div v-if="compareMode" class="compare-banner">
      <span class="compare-banner-text">
        비교할 계획 두 개를 선택하세요 ({{ compareSelection.length }}/2)
      </span>
      <button class="compare-banner-cancel" @click="toggleCompareMode">취소</button>
    </div>

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
          :class="{ expanded: selectedPlanId === plan.id, selectable: compareMode, selected: isCompareSelected(plan.id) }"
        >
          <!-- Card header — click to expand/collapse (or select in compare mode) -->
          <div class="plan-thumb" @click="onCardTap(plan)">
            <div class="thumb-gradient" />
            <div v-if="compareMode" class="compare-check" :class="{ on: isCompareSelected(plan.id) }">
              <svg v-if="isCompareSelected(plan.id)" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            </div>
            <div class="plan-dates">
              <span class="date-label">{{ formatDate(plan.startDate) }}</span>
              <span class="date-sep">–</span>
              <span class="date-label">{{ formatDate(plan.endDate) }}</span>
            </div>
          </div>
          <div class="plan-info">
            <div class="plan-info-main" @click="onCardTap(plan)">
              <h3 class="plan-name">{{ plan.title }}</h3>
              <p class="plan-sub">{{ plan.destination }} · {{ dayCount(plan.startDate, plan.endDate) }}박 {{ dayCount(plan.startDate, plan.endDate) + 1 }}일</p>
              <div class="plan-spots">
                <span v-for="spot in plan.spots?.slice(0, 3)" :key="spot" class="spot-chip">{{ spot }}</span>
              </div>
            </div>
            <button v-if="!compareMode" class="plan-delete-btn" title="여행 계획 삭제" @click.stop="confirmDeletePlan(plan)">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                <line x1="10" y1="11" x2="10" y2="17" />
                <line x1="14" y1="11" x2="14" y2="17" />
              </svg>
            </button>
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
                      <!-- 인라인 편집: 위/아래 이동, 삭제 -->
                      <div class="place-edit-actions">
                        <button
                          class="place-edit-btn"
                          title="위로 이동"
                          :disabled="idx === 0 || planStore.loading"
                          @click="movePlace(plan.id, day, idx, -1)"
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="18 15 12 9 6 15" />
                          </svg>
                        </button>
                        <button
                          class="place-edit-btn"
                          title="아래로 이동"
                          :disabled="idx === (day.places.length - 1) || planStore.loading"
                          @click="movePlace(plan.id, day, idx, 1)"
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="6 9 12 15 18 9" />
                          </svg>
                        </button>
                        <button
                          class="place-edit-btn danger"
                          title="장소 삭제"
                          :disabled="planStore.loading"
                          @click="removePlace(plan.id, day.dayNo, place)"
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div v-if="!(day.places?.length)" class="detail-empty">일정이 없어요</div>
                  </div>
                </div>
                <div v-if="!(planStore.current.days?.length)" class="detail-empty-plan">
                  아직 일정이 없어요. AI로 동선을 최적화해보세요!
                </div>
              </template>
              <div v-else class="detail-loading">상세 정보를 불러오는 중...</div>

              <!-- 예산 패널 -->
              <div v-if="budget && budget.planId === plan.id" class="budget-panel">
                <div class="budget-head">
                  <span class="budget-title">예상 예산</span>
                  <span class="budget-total">{{ formatWon(budget.totalEstimated) }}</span>
                </div>
                <div class="budget-days">
                  <div v-for="d in budget.dayBudgets" :key="d.dayNo" class="budget-day-row">
                    <span class="budget-day-label">{{ d.dayNo }}일차</span>
                    <span class="budget-day-cost">{{ formatWon(d.estimatedCost) }}</span>
                  </div>
                </div>
                <div v-if="budget.plannedBudget != null" class="budget-planned-row">
                  <span>설정 예산 {{ formatWon(budget.plannedBudget) }}</span>
                  <span
                    v-if="budget.difference != null"
                    class="budget-diff"
                    :class="{ over: budget.difference < 0 }"
                  >
                    {{ budget.difference >= 0 ? '여유 ' : '초과 ' }}{{ formatWon(Math.abs(budget.difference)) }}
                  </span>
                </div>
                <p v-if="budget.note" class="budget-note">{{ budget.note }}</p>
              </div>

              <!-- 공유 링크 -->
              <div v-if="shareInfo && shareInfo.planId === plan.id" class="share-panel">
                <span class="share-icon">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8" /><polyline points="16 6 12 2 8 6" /><line x1="12" y1="2" x2="12" y2="15" />
                  </svg>
                </span>
                <code class="share-url">{{ shareInfo.url }}</code>
                <button class="share-copy-btn" @click="copyShareUrl">{{ shareCopied ? '복사됨' : '복사' }}</button>
              </div>

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
              <div class="detail-actions secondary">
                <button class="detail-sub-btn" :disabled="budgetLoading" @click="loadBudget(plan.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" />
                  </svg>
                  {{ budgetLoading ? '계산 중...' : '예산 보기' }}
                </button>
                <button class="detail-sub-btn" :disabled="shareLoading" @click="sharePlan(plan.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                  </svg>
                  {{ shareLoading ? '생성 중...' : '공유하기' }}
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

    <!-- 비교 결과 모달 -->
    <Transition name="fade">
      <div v-if="compareResult" class="compare-overlay" @click.self="closeCompare">
        <div class="compare-sheet">
          <div class="compare-sheet-head">
            <h3 class="compare-sheet-title">계획 비교</h3>
            <button class="compare-sheet-close" @click="closeCompare">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <div class="compare-grid">
            <div class="compare-col">
              <div class="compare-plan-name">{{ compareResult.a.title }}</div>
            </div>
            <div class="compare-col-label" />
            <div class="compare-col">
              <div class="compare-plan-name">{{ compareResult.b.title }}</div>
            </div>

            <template v-for="row in compareRows" :key="row.key">
              <div class="compare-val" :class="row.aClass">{{ row.a }}</div>
              <div class="compare-metric">{{ row.label }}</div>
              <div class="compare-val" :class="row.bClass">{{ row.b }}</div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { usePlanStore } from '@/stores/plan.js'
import { planApi } from '@/api/index.js'

const router = useRouter()
const planStore = usePlanStore()
// storeToRefs로 반응성 유지 — 비반응적 destructure 시 loadPlans() 후 목록이 갱신되지 않음
const { plans } = storeToRefs(planStore)

const selectedPlanId = ref(null)

// ── 예산 보기 ────────────────────────────────────────────────────────────────
const budget = ref(null)        // { planId, dayBudgets, totalEstimated, plannedBudget, difference, note }
const budgetLoading = ref(false)

// ── 공유 ─────────────────────────────────────────────────────────────────────
const shareInfo = ref(null)     // { planId, url }
const shareLoading = ref(false)
const shareCopied = ref(false)

// ── 비교 ─────────────────────────────────────────────────────────────────────
const compareMode = ref(false)
const compareSelection = ref([])   // 선택된 planId 최대 2개
const compareResult = ref(null)    // { a: PlanStat, b: PlanStat }

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

/** 카드 탭 — 비교 모드면 선택, 아니면 펼치기/접기 */
function onCardTap(plan) {
  if (compareMode.value) {
    toggleCompareSelect(plan.id)
    return
  }
  togglePlan(plan)
}

/** Expand/collapse a plan card and load its detail */
async function togglePlan(plan) {
  if (selectedPlanId.value === plan.id) {
    selectedPlanId.value = null
    return
  }
  selectedPlanId.value = plan.id
  // 다른 계획을 펼치면 이전 계획의 예산/공유 패널은 감춘다
  if (budget.value && budget.value.planId !== plan.id) budget.value = null
  if (shareInfo.value && shareInfo.value.planId !== plan.id) shareInfo.value = null
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

/** 원(₩) 포맷 */
function formatWon(n) {
  if (n == null) return '-'
  return `${Number(n).toLocaleString('ko-KR')}원`
}

// ── 예산 보기 ────────────────────────────────────────────────────────────────
async function loadBudget(planId) {
  if (budgetLoading.value) return
  // 토글: 이미 같은 계획 예산이 열려있으면 닫는다
  if (budget.value && budget.value.planId === planId) {
    budget.value = null
    return
  }
  budgetLoading.value = true
  try {
    const { data } = await planApi.getBudget(planId)
    budget.value = data
  } catch {
    budget.value = null
  } finally {
    budgetLoading.value = false
  }
}

// ── 공유 ─────────────────────────────────────────────────────────────────────
async function sharePlan(planId) {
  if (shareLoading.value) return
  // 토글: 이미 같은 계획 공유링크가 열려있으면 닫는다
  if (shareInfo.value && shareInfo.value.planId === planId) {
    shareInfo.value = null
    return
  }
  shareLoading.value = true
  shareCopied.value = false
  try {
    const { data } = await planApi.share(planId)
    // BE는 상대경로(/plan/shared/{token})를 주므로 절대 URL로 변환
    const path = data?.shareUrl ?? `/plan/shared/${data?.shareToken}`
    const url = `${window.location.origin}${path}`
    shareInfo.value = { planId, url }
    // 생성 직후 클립보드에 자동 복사 시도
    await copyToClipboard(url)
  } catch {
    shareInfo.value = null
  } finally {
    shareLoading.value = false
  }
}

async function copyShareUrl() {
  if (!shareInfo.value) return
  await copyToClipboard(shareInfo.value.url)
}

async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    shareCopied.value = true
    setTimeout(() => { shareCopied.value = false }, 1500)
  } catch {
    // 클립보드 접근 불가 — 사용자가 링크를 직접 복사하면 됨
  }
}

// ── 비교 ─────────────────────────────────────────────────────────────────────
function toggleCompareMode() {
  compareMode.value = !compareMode.value
  compareSelection.value = []
  compareResult.value = null
  if (compareMode.value) {
    // 비교 모드 진입 시 펼친 카드/패널 정리
    selectedPlanId.value = null
    budget.value = null
    shareInfo.value = null
  }
}

function isCompareSelected(planId) {
  return compareSelection.value.includes(planId)
}

async function toggleCompareSelect(planId) {
  const idx = compareSelection.value.indexOf(planId)
  if (idx >= 0) {
    compareSelection.value.splice(idx, 1)
    return
  }
  if (compareSelection.value.length >= 2) return  // 최대 2개
  compareSelection.value.push(planId)
  if (compareSelection.value.length === 2) {
    await runCompare()
  }
}

async function runCompare() {
  const [aId, bId] = compareSelection.value
  try {
    const { data } = await planApi.compare(aId, bId)
    compareResult.value = data
  } catch {
    compareResult.value = null
  }
}

function closeCompare() {
  compareResult.value = null
  // 모달만 닫고 비교 모드는 유지 — 선택은 초기화해 다시 고를 수 있게
  compareSelection.value = []
}

/** 비교 표 행 — 값이 더 좋은 쪽을 강조 (낮을수록 좋은 항목은 작은 값 강조) */
const compareRows = computed(() => {
  const r = compareResult.value
  if (!r) return []
  const a = r.a
  const b = r.b
  const dur = (m) => {
    const h = Math.floor(m / 60)
    const min = m % 60
    return h > 0 ? `${h}시간${min > 0 ? ' ' + min + '분' : ''}` : `${min}분`
  }
  const rows = [
    { key: 'days', label: '일정', a: `${a.totalDays}일`, b: `${b.totalDays}일`, aBetter: null, bBetter: null },
    { key: 'places', label: '장소 수', a: `${a.totalPlaces}곳`, b: `${b.totalPlaces}곳`, aBetter: a.totalPlaces > b.totalPlaces, bBetter: b.totalPlaces > a.totalPlaces },
    { key: 'dist', label: '총 이동거리', a: `${a.totalDistanceKm}km`, b: `${b.totalDistanceKm}km`, aBetter: a.totalDistanceKm < b.totalDistanceKm, bBetter: b.totalDistanceKm < a.totalDistanceKm },
    { key: 'dur', label: '예상 소요', a: dur(a.totalDurationMin), b: dur(b.totalDurationMin), aBetter: a.totalDurationMin < b.totalDurationMin, bBetter: b.totalDurationMin < a.totalDurationMin },
    { key: 'budget', label: '예산', a: a.budget != null ? formatWon(a.budget) : '-', b: b.budget != null ? formatWon(b.budget) : '-', aBetter: null, bBetter: null },
  ]
  return rows.map((row) => ({
    ...row,
    aClass: row.aBetter ? 'better' : '',
    bClass: row.bBetter ? 'better' : '',
  }))
})

/** 여행 계획 삭제 — 확인 후 deletePlan 호출 */
async function confirmDeletePlan(plan) {
  if (!window.confirm(`'${plan.title}' 여행 계획을 삭제할까요?`)) return
  try {
    await planStore.deletePlan(plan.id)
    if (selectedPlanId.value === plan.id) selectedPlanId.value = null
  } catch {
    // 오류는 planStore.error에 반영됨
  }
}

/**
 * 같은 날 안에서 장소 순서를 위/아래로 한 칸 이동.
 * dir: -1(위) | +1(아래). 재정렬한 배열을 replacePlaces로 저장한다.
 */
async function movePlace(planId, day, idx, dir) {
  const places = [...(day.places ?? [])]
  const target = idx + dir
  if (target < 0 || target >= places.length) return
  // swap
  ;[places[idx], places[target]] = [places[target], places[idx]]
  try {
    await planStore.replacePlaces(planId, day.dayNo, places)
  } catch {
    // 오류는 planStore.error에 반영됨
  }
}

/** 장소 한 개 삭제 */
async function removePlace(planId, dayNo, place) {
  if (place.id == null) return
  try {
    await planStore.removePlace(planId, dayNo, place.id)
  } catch {
    // 오류는 planStore.error에 반영됨
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compare-toggle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, color 0.15s;
}

.compare-toggle.active {
  background: var(--color-peach);
  color: white;
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

/* ── Compare mode ─────────────────────────────────────────────────────────── */
.compare-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: var(--color-peach-light);
  border-bottom: 1px solid rgba(247, 143, 87, 0.15);
}

.compare-banner-text {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-peach-pressed);
}

.compare-banner-cancel {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

.plan-card.selectable {
  outline: 2px solid transparent;
  transition: outline-color 0.15s;
}

.plan-card.selected {
  outline-color: var(--color-peach);
}

.compare-check {
  position: absolute;
  top: 10px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.8);
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-peach);
  z-index: 2;
}

.compare-check.on {
  background: white;
  border-color: white;
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
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.plan-info-main {
  flex: 1;
  min-width: 0;
}

.plan-delete-btn {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  flex-shrink: 0;
  cursor: pointer;
}

.plan-delete-btn:hover {
  background: var(--color-peach-light);
  color: var(--color-error);
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

.place-edit-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.place-edit-btn {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  cursor: pointer;
}

.place-edit-btn:hover:not(:disabled) {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}

.place-edit-btn.danger:hover:not(:disabled) {
  color: var(--color-error);
}

.place-edit-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
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

.detail-actions.secondary {
  margin-top: 8px;
}

.detail-sub-btn {
  flex: 1;
  height: 38px;
  border-radius: var(--radius-xl);
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  background: var(--color-white);
  color: var(--color-ink-secondary);
  border: 1px solid var(--color-line-light);
}

.detail-sub-btn:hover:not(:disabled) {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  border-color: var(--color-peach-light);
}

.detail-sub-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── Budget panel ─────────────────────────────────────────────────────────── */
.budget-panel {
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 14px;
  box-shadow: var(--shadow-card);
}

.budget-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.budget-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
}

.budget-total {
  font-size: 16px;
  font-weight: 800;
  color: var(--color-peach-pressed);
  letter-spacing: -0.3px;
}

.budget-days {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.budget-day-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
}

.budget-day-label {
  color: var(--color-ink-muted);
}

.budget-day-cost {
  color: var(--color-ink-secondary);
  font-weight: 600;
}

.budget-planned-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--color-line-light);
  font-size: 12.5px;
  color: var(--color-ink-muted);
}

.budget-diff {
  font-weight: 700;
  color: var(--color-peach-pressed);
}

.budget-diff.over {
  color: var(--color-error);
}

.budget-note {
  font-size: 11px;
  color: var(--color-ink-muted);
  margin-top: 8px;
}

/* ── Share panel ──────────────────────────────────────────────────────────── */
.share-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  margin-top: 10px;
}

.share-icon {
  flex-shrink: 0;
  color: var(--color-peach-pressed);
  display: flex;
}

.share-url {
  flex: 1;
  min-width: 0;
  font-size: 11.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--color-ink-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-copy-btn {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  padding: 4px 8px;
}

/* ── Compare modal ────────────────────────────────────────────────────────── */
.compare-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 50;
}

.compare-sheet {
  width: 100%;
  background: var(--color-white);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: 18px 20px calc(env(safe-area-inset-bottom, 0px) + 24px);
  max-height: 80%;
  overflow-y: auto;
}

.compare-sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.compare-sheet-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.compare-sheet-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
}

.compare-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 10px 8px;
  align-items: center;
}

.compare-col {
  text-align: center;
}

.compare-plan-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
  line-height: 1.35;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--color-peach-light);
}

.compare-col-label {
  border-bottom: 2px solid transparent;
}

.compare-metric {
  font-size: 11px;
  color: var(--color-ink-muted);
  text-align: center;
  white-space: nowrap;
  padding: 0 4px;
}

.compare-val {
  text-align: center;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}

.compare-val.better {
  color: var(--color-peach-pressed);
  font-weight: 800;
}

/* fade transition for overlay */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
