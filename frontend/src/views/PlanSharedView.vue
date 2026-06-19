<template>
  <div class="page">
    <!-- Top nav -->
    <div class="nav-bar">
      <button class="nav-btn" @click="goHome">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
      </button>
      <span class="nav-title">공유된 여행 계획</span>
      <div class="nav-spacer" />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="state-wrap">
      <div class="skeleton-body">
        <div class="skeleton-line w80" />
        <div class="skeleton-line w60" />
        <div class="skeleton-line w90" />
        <div class="skeleton-line w50" />
      </div>
    </div>

    <!-- Error / not found -->
    <div v-else-if="error || !plan" class="state-wrap">
      <div class="empty-icon">
        <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <p class="state-title">계획을 불러올 수 없어요</p>
      <p class="state-sub">{{ error ?? '공유 링크가 만료되었거나 잘못된 링크예요.' }}</p>
      <button class="state-btn" @click="goHome">홈으로</button>
    </div>

    <!-- Shared plan content -->
    <template v-else>
      <!-- Summary banner -->
      <div class="summary-banner">
        <div class="banner-glow" />
        <div class="banner-info">
          <div class="banner-label">공유된 여행 일정</div>
          <div class="banner-title">{{ plan.title }}</div>
          <div class="banner-dates">{{ formatRange(plan.startDate, plan.endDate) }}</div>
          <div class="banner-tags">
            <span class="banner-tag">총 {{ totalPlaces }}개 장소</span>
            <span class="banner-tag">{{ totalDays }}일 일정</span>
            <span v-if="plan.budget" class="banner-tag">예산 {{ formatWon(plan.budget) }}</span>
          </div>
        </div>
        <div class="route-badge">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </div>
      </div>

      <!-- Read-only note -->
      <div class="opt-note">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 01-3.46 0" />
        </svg>
        읽기 전용으로 공유된 일정이에요
      </div>

      <!-- Scroll area -->
      <div class="scroll-content">
        <div
          v-for="day in days"
          :key="day.dayNo"
          class="day-section"
        >
          <div class="day-header">
            <span class="day-pill">{{ day.dayNo }}일차</span>
            <span v-if="day.memo" class="day-summary">{{ day.memo }}</span>
          </div>

          <!-- Place list -->
          <div v-if="day.places?.length" class="timeline">
            <div
              v-for="(place, idx) in day.places"
              :key="place.id ?? idx"
              class="timeline-item"
            >
              <div class="tl-left">
                <div class="tl-dot">{{ idx + 1 }}</div>
                <div v-if="idx < day.places.length - 1" class="tl-line" />
              </div>
              <div class="place-card">
                <div class="place-thumb">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                    <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </div>
                <div class="place-info">
                  <div class="place-name">{{ place.attraction?.title ?? '장소' }}</div>
                  <div v-if="place.attraction?.addr" class="place-addr">{{ place.attraction.addr }}</div>
                  <div v-if="place.visitTime" class="place-time-row">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    {{ formatTime(place.visitTime) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-day">이 날의 일정이 없어요</div>
        </div>

        <div v-if="!days.length" class="no-days">
          <p>등록된 일정이 없어요.</p>
        </div>

        <!-- 내 계획 만들기 유도 -->
        <button class="cta-btn" @click="goNewTrip">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          나도 AI로 여행 계획 만들기
        </button>

        <div class="bottom-spacer" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { planApi } from '@/api/index.js'

const router = useRouter()
const route = useRoute()

const token = route.params.token

const plan = ref(null)
const loading = ref(false)
const error = ref(null)

onMounted(async () => {
  loading.value = true
  error.value = null
  try {
    // 공개 조회 — 비로그인에서도 동작 (http는 토큰이 있을 때만 첨부)
    const { data } = await planApi.getShared(token)
    plan.value = data
  } catch (e) {
    error.value = e.response?.data?.message ?? '공유된 계획을 불러올 수 없어요.'
  } finally {
    loading.value = false
  }
})

const days = computed(() => plan.value?.days ?? [])
const totalDays = computed(() => days.value.length)
const totalPlaces = computed(() =>
  days.value.reduce((acc, d) => acc + (d.places?.length ?? 0), 0),
)

function formatRange(start, end) {
  if (!start || !end) return ''
  const s = new Date(start)
  const e = new Date(end)
  const f = (d) => `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
  return `${f(s)} – ${f(e)}`
}

/** "HH:mm:ss" → "HH:mm" (LocalTime 직렬화 대응) */
function formatTime(t) {
  if (!t) return ''
  return String(t).slice(0, 5)
}

function formatWon(n) {
  if (n == null) return ''
  return `${Number(n).toLocaleString('ko-KR')}원`
}

function goHome() {
  router.push('/')
}

function goNewTrip() {
  router.push('/ai/plan')
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

/* ── Nav ──────────────────────────────────────────────────────────────────── */
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

.nav-spacer {
  width: 36px;
}

/* ── Loading / error state ───────────────────────────────────────────────── */
.state-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
}

.skeleton-body {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 24px;
}

.skeleton-line {
  height: 14px;
  background: var(--color-line-light);
  border-radius: var(--radius-full);
  animation: shimmer 1.2s infinite;
}

.w80 { width: 80%; }
.w60 { width: 60%; }
.w90 { width: 90%; }
.w50 { width: 50%; }

@keyframes shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.empty-icon {
  margin-bottom: 8px;
}

.state-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.state-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  text-align: center;
  margin-bottom: 12px;
}

.state-btn {
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 600;
  padding: 12px 28px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}

/* ── Summary banner ───────────────────────────────────────────────────────── */
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

.banner-info {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.banner-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.banner-title {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.4px;
  line-height: 1.3;
}

.banner-dates {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 6px;
}

.banner-tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.banner-tag {
  font-size: 10px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.12);
  padding: 3px 8px;
  border-radius: var(--radius-full);
}

.route-badge {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: rgba(247, 143, 87, 0.18);
  border: 2px solid rgba(247, 143, 87, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

/* ── Read-only note ───────────────────────────────────────────────────────── */
.opt-note {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--color-peach-light);
  border-bottom: 1px solid rgba(247, 143, 87, 0.15);
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-peach-pressed);
  flex-shrink: 0;
}

/* ── Scroll area ──────────────────────────────────────────────────────────── */
.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px 0;
}

.bottom-spacer {
  height: 40px;
}

/* ── Day section ──────────────────────────────────────────────────────────── */
.day-section {
  margin-bottom: 24px;
}

.day-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
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
}

.timeline-item {
  display: flex;
  gap: 10px;
}

.tl-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 26px;
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
  padding: 12px;
  box-shadow: var(--shadow-card);
  margin-bottom: 10px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.place-thumb {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-peach);
  flex-shrink: 0;
}

.place-info {
  flex: 1;
  min-width: 0;
}

.place-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}

.place-addr {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  line-height: 1.4;
  margin-bottom: 4px;
}

.place-time-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-peach);
}

.empty-day {
  text-align: center;
  font-size: 13.5px;
  color: var(--color-ink-muted);
  padding: 20px 0;
}

.no-days {
  text-align: center;
  padding: 40px 0;
}

.no-days p {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

/* ── CTA ──────────────────────────────────────────────────────────────────── */
.cta-btn {
  width: 100%;
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
  margin-top: 4px;
}
</style>
