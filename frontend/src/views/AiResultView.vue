<template>
  <div class="page">
    <!-- Loading skeleton -->
    <div v-if="pageLoading" class="skeleton-wrap">
      <div class="skeleton-nav" />
      <div class="skeleton-banner" />
      <div class="skeleton-body">
        <div class="skeleton-line w60" />
        <div class="skeleton-line w90" />
        <div class="skeleton-line w75" />
      </div>
    </div>

    <!-- No result — redirect should have fired, show fallback -->
    <div v-else-if="!rec" class="empty-wrap">
      <div class="nav-bar">
        <button class="nav-btn" @click="goBack">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
        </button>
        <span class="nav-title">AI 추천 결과</span>
        <span style="width:36px" />
      </div>
      <div class="empty-page">
      <div class="empty-icon">
        <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <path d="M8 15s1.5 2 4 2 4-2 4-2" />
          <line x1="9" y1="9" x2="9.01" y2="9" />
          <line x1="15" y1="9" x2="15.01" y2="9" />
        </svg>
      </div>
      <p class="empty-title">추천 결과가 없어요</p>
      <p class="empty-sub">AI 추천을 먼저 실행해 주세요</p>
      <button class="goto-ai-btn" @click="router.replace('/ai/plan')">AI 추천 받기</button>
      </div>
    </div>

    <template v-else>
      <!-- Custom top nav -->
      <div class="nav-bar">
        <button class="nav-btn" @click="router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
        </button>
        <span class="nav-title">AI 추천 결과</span>
        <button class="nav-regen-btn" @click="router.push('/ai/plan')">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          재생성
        </button>
      </div>

      <!-- Summary banner -->
      <div class="summary-banner">
        <div class="banner-glow" />
        <div class="banner-info">
          <div class="banner-label">
            AI가 생성한 최적 일정
            <span v-if="isPartial" class="partial-badge">일부 일정만 생성됐어요</span>
          </div>
          <div class="banner-title">{{ draft?.totalSummary ?? '나만의 여행 일정' }}</div>
          <div class="banner-tags">
            <span class="banner-tag">{{ days.length }}일 일정</span>
            <span v-if="rec.status && rec.status !== 'DONE'" class="banner-tag">{{ rec.status }}</span>
          </div>
        </div>
        <div class="score-badge">
          <span class="score-label-top">AI</span>
          <span class="score-label-bot">추천</span>
        </div>
      </div>

      <!-- Day tabs -->
      <div class="day-tabs">
        <button
          v-for="(tab, i) in tabs"
          :key="tab"
          class="day-tab"
          :class="{ active: activeTab === i }"
          @click="activeTab = i"
        >{{ tab }}</button>
      </div>

      <!-- Scrollable day content -->
      <div class="scroll-content">
        <div v-if="activeDay">
          <div class="day-header">
            <span class="day-pill">{{ activeDay.dayNo }}일차</span>
            <span class="day-summary">{{ activeDay.summary }}</span>
          </div>

          <div v-if="activeDay.places && activeDay.places.length" class="timeline">
            <div
              v-for="(place, idx) in activeDay.places"
              :key="place.contentId ?? idx"
              class="timeline-item"
            >
              <div class="tl-left">
                <div class="tl-dot">{{ idx + 1 }}</div>
                <div v-if="idx < activeDay.places.length - 1" class="tl-line" />
              </div>
              <div class="place-card">
                <div class="place-thumb-wrap">
                  <div class="place-thumb">
                    <img
                      v-if="placeImage(place)"
                      :src="placeImage(place)"
                      :alt="place.title"
                      class="place-thumb-img"
                      @error="onThumbError"
                    />
                    <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
                      <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </div>
                </div>
                <div class="place-info">
                  <div class="place-meta">
                    <span v-if="place.visitTime" class="place-time">{{ place.visitTime }}</span>
                  </div>
                  <div class="place-name">{{ place.title }}</div>
                  <div v-if="place.reason" class="place-reason">{{ place.reason }}</div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-day">
            이 날의 일정이 없어요.
          </div>
        </div>

        <!-- Store error -->
        <div v-if="recommendStore.error" class="error-msg">
          {{ recommendStore.error }}
        </div>

        <!-- Save done notice -->
        <Transition name="slide-down">
          <div v-if="saveDone" class="save-notice">
            <span>계획에 저장했어요!</span>
            <button class="save-notice-link" @click="router.push('/plan')">계획 보러 가기 →</button>
          </div>
        </Transition>

        <!-- 저장된 계획으로 동행 모집하기 -->
        <Transition name="slide-down">
          <button v-if="savedPlanId" class="companion-cta" @click="goCompanion">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" /></svg>
            이 일정으로 동행 모집하기
          </button>
        </Transition>

        <div class="bottom-spacer" />
      </div>

      <!-- Bottom bar -->
      <div class="bottom-bar">
        <button class="btn-retry" @click="router.push('/ai/plan')">재생성</button>
        <button
          class="btn-save"
          :disabled="saveLoading || saveDone"
          @click="handleSavePlan"
        >
          <template v-if="saveLoading">
            <span class="spinner" />
            저장 중...
          </template>
          <template v-else-if="saveDone">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12" /></svg>
            저장됨
          </template>
          <template v-else>
            계획으로 저장
          </template>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRecommendStore } from '@/stores/recommend.js'

const router = useRouter()
const recommendStore = useRecommendStore()

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/home')
}

// ── State ─────────────────────────────────────────────────────────────────────
const pageLoading = ref(false)
const saveLoading = ref(false)
const saveDone = ref(false)
// 저장 후 반환된 계획 ID — 동행 모집 연결용
const savedPlanId = ref(null)

// ── Bootstrap: if no current recommendation, try loading latest from history ──
onMounted(async () => {
  if (recommendStore.current) return

  pageLoading.value = true
  try {
    await recommendStore.loadHistory()
    if (recommendStore.history.length === 0) {
      router.replace('/ai')
      return
    }
    const latest = recommendStore.history[0]
    await recommendStore.load(latest.id)
  } catch {
    router.replace('/ai')
  } finally {
    pageLoading.value = false
  }
})

// ── Computed ──────────────────────────────────────────────────────────────────
const rec = computed(() => recommendStore.current)
const draft = computed(() => rec.value?.draft ?? null)
const days = computed(() => draft.value?.days ?? [])
const isPartial = computed(() => rec.value?.status === 'PARTIAL')

// ── Tabs ──────────────────────────────────────────────────────────────────────
const activeTab = ref(0)
const tabs = computed(() => days.value.map(d => `${d.dayNo}일차`))
const activeDay = computed(() => days.value[activeTab.value] ?? null)

// ── Save plan ─────────────────────────────────────────────────────────────────
async function handleSavePlan() {
  if (!rec.value) return
  saveLoading.value = true
  recommendStore.error = null
  try {
    // savePlan 은 저장된 PlanDetail({ id, ... })을 반환 — id 를 동행 모집 연결에 사용
    const plan = await recommendStore.savePlan(rec.value.id)
    saveDone.value = true
    savedPlanId.value = plan?.id ?? null
    // 저장 후 계획 보러 가기 / 동행 모집하기 선택지를 노출하기 위해
    // 자동 이동을 하지 않는다(savedPlanId 없으면 기존처럼 계획 허브로 이동).
    if (!savedPlanId.value) {
      setTimeout(() => router.replace('/plan'), 800)
    }
  } catch {
    // error shown via store.error
  } finally {
    saveLoading.value = false
  }
}

// 로드 실패한 이미지 URL — 폴백(핀 SVG) 노출용
const failedThumbs = ref(new Set())
// 장소 썸네일 — 이미지 URL이 있으면 사진을, 없으면(또는 로드 실패 시) 핀 SVG로 폴백
function placeImage(place) {
  const url = place?.imageUrl || place?.firstimage || place?.firstImage || place?.image || ''
  return url && !failedThumbs.value.has(url) ? url : ''
}
// 이미지 로드 실패 시 해당 URL을 기록 → 다음 렌더에서 핀 SVG로 전환
function onThumbError(e) {
  const url = e.target.getAttribute('src')
  if (url) failedThumbs.value = new Set(failedThumbs.value).add(url)
}

// 저장된 계획 ID 를 동행 작성 화면에 query 로 전달(라우트 변경 없음)
function goCompanion() {
  if (!savedPlanId.value) return
  router.push({ name: 'companion-write', query: { planId: String(savedPlanId.value) } })
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

/* ── Skeleton ─────────────────────────────────────────────────────────────── */
.skeleton-wrap {
  flex: 1;
  overflow: hidden;
}

.skeleton-nav {
  height: 52px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
}

.skeleton-banner {
  height: 110px;
  background: linear-gradient(135deg, #1c1c3a 0%, #0f2f5a 100%);
}

.skeleton-body {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 14px;
  background: var(--color-line-light);
  border-radius: var(--radius-full);
  animation: shimmer 1.2s infinite;
}

.w60 { width: 60%; }
.w90 { width: 90%; }
.w75 { width: 75%; }

@keyframes shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ── Empty page ──────────────────────────────────────────────────────────── */
.empty-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.empty-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
}

.empty-icon {
  margin-bottom: 8px;
}

.empty-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.empty-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  margin-bottom: 12px;
}

.goto-ai-btn {
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
  padding: 13px 28px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}

/* ── Top nav ──────────────────────────────────────────────────────────────── */
.nav-bar {
  height: 52px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 8px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
  z-index: 10;
}

.nav-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
  flex-shrink: 0;
}

.nav-title {
  flex: 1;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.nav-regen-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-peach-pressed);
  padding: 7px 12px;
  background: var(--color-peach-light);
  border-radius: var(--radius-full);
  white-space: nowrap;
  letter-spacing: -0.2px;
}

/* ── Summary banner ──────────────────────────────────────────────────────── */
.summary-banner {
  background: linear-gradient(135deg, #1c1c3a 0%, #0f2f5a 100%);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.banner-glow {
  position: absolute;
  top: -30px;
  right: -20px;
  width: 130px;
  height: 130px;
  background: radial-gradient(circle, rgba(247, 143, 87, 0.2) 0%, transparent 70%);
  pointer-events: none;
}

.banner-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.partial-badge {
  background: rgba(255, 200, 100, 0.2);
  border: 1px solid rgba(255, 200, 100, 0.45);
  color: #ffd280;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 10px;
}

.banner-title {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.4px;
  line-height: 1.3;
}

.banner-tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.banner-tag {
  font-size: 10px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.12);
  padding: 3px 8px;
  border-radius: var(--radius-full);
}

.score-badge {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(247, 143, 87, 0.18);
  border: 2px solid rgba(247, 143, 87, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.score-label-top {
  font-size: 15px;
  font-weight: 800;
  color: var(--color-peach);
  line-height: 1;
}

.score-label-bot {
  font-size: 9px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

/* ── Day tabs ─────────────────────────────────────────────────────────────── */
.day-tabs {
  display: flex;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  overflow-x: auto;
  padding: 0 16px;
  flex-shrink: 0;
}

.day-tab {
  flex-shrink: 0;
  padding: 13px 16px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  border-bottom: 2.5px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  letter-spacing: -0.2px;
  transition: all 0.15s;
}

.day-tab.active {
  color: var(--color-peach);
  border-bottom-color: var(--color-peach);
  font-weight: 700;
}

/* ── Scroll content ──────────────────────────────────────────────────────── */
.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px 0;
}

.bottom-spacer {
  height: 120px;
}

/* ── Day header ──────────────────────────────────────────────────────────── */
.day-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.day-pill {
  background: var(--color-peach);
  color: #fff;
  padding: 5px 12px;
  border-radius: var(--radius-full);
  font-size: 12.5px;
  font-weight: 700;
  flex-shrink: 0;
}

.day-summary {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-secondary);
  line-height: 1.45;
  padding-top: 3px;
}

/* ── Timeline ─────────────────────────────────────────────────────────────── */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.tl-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 28px;
  flex-shrink: 0;
}

.tl-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--color-peach);
  background: var(--color-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 800;
  color: var(--color-peach);
  flex-shrink: 0;
  z-index: 1;
}

.tl-line {
  width: 2px;
  flex: 1;
  background: var(--color-line-light);
  margin: 4px 0;
  min-height: 16px;
}

.place-card {
  flex: 1;
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: 14px;
  box-shadow: var(--shadow-card);
  margin-bottom: 12px;
  display: flex;
  gap: 12px;
}

.place-thumb-wrap {
  flex-shrink: 0;
}

.place-thumb {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-peach);
  overflow: hidden;
}

.place-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.place-info {
  flex: 1;
}

.place-meta {
  margin-bottom: 4px;
}

.place-time {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-peach);
  background: var(--color-peach-light);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.place-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 5px;
}

.place-reason {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
  line-height: 1.45;
}

/* ── Empty day ────────────────────────────────────────────────────────────── */
.empty-day {
  text-align: center;
  font-size: 13.5px;
  color: var(--color-ink-muted);
  padding: 28px 0;
}

/* ── Error / notice ──────────────────────────────────────────────────────── */
.error-msg {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-error);
  text-align: center;
  margin-top: 12px;
  padding: 0 4px;
}

.save-notice {
  background: var(--color-peach-light);
  border: 1px solid rgba(247, 143, 87, 0.3);
  border-radius: var(--radius-lg);
  padding: 13px 16px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-peach-pressed);
}

.save-notice-link {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-peach);
  white-space: nowrap;
}

.companion-cta {
  width: 100%;
  margin-top: 12px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--color-white);
  color: var(--color-peach-pressed);
  border: 1.5px solid var(--color-peach);
  border-radius: var(--radius-xl);
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: -0.3px;
  cursor: pointer;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ── Bottom bar ───────────────────────────────────────────────────────────── */
.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  padding: 12px 16px calc(env(safe-area-inset-bottom, 0px) + 16px);
  display: flex;
  gap: 10px;
  z-index: 10;
}

.btn-retry {
  height: 50px;
  padding: 0 18px;
  background: transparent;
  color: var(--color-ink-secondary);
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-xl);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: -0.2px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-save {
  flex: 1;
  height: 50px;
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: #fff;
  border: none;
  border-radius: var(--radius-xl);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.3px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 14px rgba(247, 143, 87, 0.3);
  transition: opacity 0.2s;
}

.btn-save:disabled {
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
