# Created: 2026-06-16 13:30:39
<template>
  <div class="page">
    <div class="scroll-area">
      <!-- ── Hero ─────────────────────────────────────────────────────────── -->
      <div class="hero">
        <div class="hero-img">
          <!-- Loading skeleton -->
          <div v-if="loading" class="img-placeholder skeleton-bg" />
          <!-- Real image -->
          <div v-else-if="place?.imageUrl" class="img-wrapper">
            <img :src="place.imageUrl" :alt="place?.name" @error="(e) => e.target.parentElement.style.display='none'" />
          </div>
          <!-- Fallback placeholder -->
          <div v-else class="img-placeholder">
            <span class="img-caption">{{ place?.address }}</span>
          </div>
          <div class="hero-gradient" />
        </div>
        <div class="top-bar">
          <button class="ghost-btn" @click="$router.back()">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 5l-7 7 7 7" />
            </svg>
          </button>
          <button class="ghost-btn" @click="bookmarked = !bookmarked">
            <svg width="22" height="22" viewBox="0 0 24 24" :fill="bookmarked ? 'white' : 'none'" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" />
            </svg>
          </button>
        </div>
        <div class="hero-info">
          <div v-if="place?.category" class="rank-badge">{{ place.category }}</div>
          <h1 class="place-name">{{ loading ? '불러오는 중…' : (place?.name ?? '관광지') }}</h1>
          <div v-if="place?.rating" class="rating-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="var(--color-gold)" stroke="none">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
            <span class="rating-val">{{ place.rating }}</span>
            <span v-if="place.reviewCount" class="review-count">(리뷰 {{ place.reviewCount.toLocaleString() }}개)</span>
          </div>
        </div>
      </div>

      <!-- ── Fetch error notice (non-fatal: mock data shown) ──────────────── -->
      <div v-if="fetchError" class="fetch-error-notice">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        {{ fetchError }}
      </div>

      <!-- ── Meta ──────────────────────────────────────────────────────────── -->
      <div class="meta-row">
        <div class="meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <span>{{ place?.address || '주소 정보 없음' }}</span>
        </div>
        <div v-if="place?.tel" class="meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2">
            <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07A19.5 19.5 0 013.07 9.8 19.79 19.79 0 01.1 1.18 2 2 0 012.09 0h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L6.09 7.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 14.92z" />
          </svg>
          <span>{{ place.tel }}</span>
        </div>
      </div>

      <!-- ── Tags ──────────────────────────────────────────────────────────── -->
      <div v-if="place?.tags?.length" class="tags-row">
        <span v-for="tag in place.tags" :key="tag" class="place-tag">{{ tag }}</span>
      </div>

      <!-- ── Overview / description ────────────────────────────────────────── -->
      <div v-if="place?.overview" class="overview-section">
        <h2 class="section-title">소개</h2>
        <p class="overview-text">{{ place.overview }}</p>
      </div>

      <!-- ── Actions ───────────────────────────────────────────────────────── -->
      <div class="action-row">
        <button class="action-btn primary" :disabled="addLoading" @click="addToPlan">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18M8 14h.01M12 14h.01M16 14h.01" />
          </svg>
          <span v-if="addLoading">추가 중…</span>
          <span v-else-if="addMsg === 'added'">담기 완료!</span>
          <span v-else-if="addMsg === 'duplicate'">이미 담겨 있어요</span>
          <span v-else-if="addMsg === 'error'">오류가 발생했어요</span>
          <span v-else>일정에 담기</span>
        </button>
        <button class="action-btn secondary" @click="$router.push('/explore')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <path d="M21 21l-4.35-4.35" />
          </svg>
          지도 보기
        </button>
        <button class="action-btn secondary" @click="getRecommendation">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="1 4 1 10 7 10" />
            <path d="M3.51 15a9 9 0 102.13-9.36L1 10" />
          </svg>
          다시 추천
        </button>
      </div>

      <!-- ── Mini map ──────────────────────────────────────────────────────── -->
      <div class="map-section">
        <h2 class="section-title">위치</h2>
        <div class="mini-map">
          <TripMap
            v-if="place?.lat && place?.lng"
            :places="[place]"
            :numbered="false"
          />
          <div v-else class="mini-map-bg" />
        </div>
        <div v-if="place?.lat && place?.lng" class="coords-hint">
          {{ place.lat.toFixed(4) }}° N, {{ place.lng.toFixed(4) }}° E
        </div>
      </div>

      <!-- ── 여행 맥락 정보 (날씨/일출일몰/충전소) ──────────────────────────── -->
      <div v-if="hasCoords" class="context-section">
        <!-- 날씨 + 일출·일몰 + 3일 예보 -->
        <div v-if="weatherLoading" class="ctx-card ctx-loading">
          <span class="ctx-loading-text">날씨 정보를 불러오는 중…</span>
        </div>
        <div v-else-if="weather" class="ctx-card weather-card">
          <div class="weather-now">
            <div class="weather-now-main">
              <span class="weather-temp">{{ currentTempLabel }}</span>
              <span class="weather-desc">{{ weather.currentDescription || '정보 없음' }}</span>
            </div>
            <div v-if="todaySun" class="weather-sun">
              <span class="sun-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-gold)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
                </svg>
                일출 {{ todaySun.rise }}
              </span>
              <span class="sun-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 18a5 5 0 00-10 0M12 2v7M4.22 10.22l1.42 1.42M1 18h2M21 18h2M18.36 11.64l1.42-1.42M23 22H1M16 5l-4 4-4-4" />
                </svg>
                일몰 {{ todaySun.set }}
              </span>
            </div>
          </div>
          <div v-if="forecast.length" class="forecast-row">
            <div v-for="d in forecast" :key="d.date" class="forecast-cell">
              <span class="fc-day">{{ d.dayLabel }}</span>
              <span class="fc-desc">{{ d.description }}</span>
              <span class="fc-temp">
                <span class="fc-max">{{ d.maxLabel }}</span>
                <span class="fc-min">{{ d.minLabel }}</span>
              </span>
            </div>
          </div>
        </div>
        <div v-else class="ctx-card ctx-empty">
          <span class="ctx-empty-text">날씨 정보를 불러올 수 없어요.</span>
        </div>

        <!-- 근처 전기차 충전소 -->
        <div v-if="evStations.length" class="ev-section">
          <div class="ev-header">
            <h3 class="ev-title">
              근처 전기차 충전소
              <span class="demo-badge">데모</span>
            </h3>
          </div>
          <div class="ev-list">
            <div v-for="(ev, i) in evStations" :key="i" class="ev-item">
              <div class="ev-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M13 2L3 14h7l-1 8 10-12h-7l1-8z" />
                </svg>
              </div>
              <div class="ev-info">
                <p class="ev-name">{{ ev.name }}</p>
                <p class="ev-addr">{{ ev.address }}</p>
              </div>
              <div class="ev-meta">
                <span class="ev-type">{{ ev.type }}</span>
                <span class="ev-count">{{ ev.chargerCount }}대</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Reviews (static seed — no reviews API) ────────────────────────── -->
      <div class="reviews-section">
        <div class="section-header">
          <h2 class="section-title">여행 후기</h2>
          <button class="see-all" @click="$router.push('/community')">전체보기</button>
        </div>
        <div class="review-list">
          <div v-for="review in STATIC_REVIEWS" :key="review.id" class="review-card">
            <div class="review-header">
              <div class="reviewer-info">
                <div class="rv-avatar" />
                <div>
                  <p class="rv-name">{{ review.author }}</p>
                  <p class="rv-date">{{ review.date }}</p>
                </div>
              </div>
              <div class="rv-rating">
                <svg
                  v-for="i in 5"
                  :key="i"
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  :fill="i <= review.rating ? 'var(--color-gold)' : 'var(--color-line)'"
                  stroke="none"
                >
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                </svg>
              </div>
            </div>
            <p class="review-text">{{ review.content }}</p>
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAttractionStore } from '@/stores/attraction.js'
import { usePlanStore } from '@/stores/plan.js'
import { useAuthStore } from '@/stores/auth.js'
import { contextApi } from '@/api/index.js'
import TripMap from '@/components/common/TripMap.vue'

const route = useRoute()
const router = useRouter()
const store = useAttractionStore()
const planStore = usePlanStore()
const authStore = useAuthStore()

const bookmarked = ref(false)
const place = ref(null)
const loading = ref(false)
const fetchError = ref(null)
const addMsg = ref('')      // 'added' | 'duplicate' | 'error' | 'no-auth'
const addLoading = ref(false)

// ── 여행 맥락 정보 (날씨/일출일몰/충전소) ────────────────────────────────────
const weather = ref(null)
const weatherLoading = ref(false)
const evStations = ref([])

const hasCoords = computed(() => !!(place.value?.lat && place.value?.lng))

// "06-19" → "19일", 오늘은 "오늘"
function formatDayLabel(dateStr) {
  if (!dateStr) return ''
  const d = new Date(`${dateStr}T00:00:00`)
  if (Number.isNaN(d.getTime())) return dateStr
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (d.getTime() === today.getTime()) return '오늘'
  const days = ['일', '월', '화', '수', '목', '금', '토']
  return `${days[d.getDay()]}요일`
}

// "2026-06-19T05:11" → "05:11"
function formatTime(iso) {
  if (!iso) return '--:--'
  const m = String(iso).match(/T(\d{2}:\d{2})/)
  return m ? m[1] : '--:--'
}

function tempLabel(v) {
  return v == null ? '--°' : `${Math.round(v)}°`
}

const currentTempLabel = computed(() => tempLabel(weather.value?.currentTempC))

const todaySun = computed(() => {
  const d = weather.value?.daily?.[0]
  if (!d) return null
  return { rise: formatTime(d.sunrise), set: formatTime(d.sunset) }
})

const forecast = computed(() => {
  const list = weather.value?.daily ?? []
  return list.slice(0, 3).map((d) => ({
    date: d.date,
    dayLabel: formatDayLabel(d.date),
    description: d.description || '—',
    minLabel: tempLabel(d.minTempC),
    maxLabel: tempLabel(d.maxTempC),
  }))
})

async function loadContext() {
  if (!hasCoords.value) return
  const lat = place.value.lat
  const lng = place.value.lng

  weatherLoading.value = true
  try {
    const { data } = await contextApi.weather(lat, lng)
    weather.value = data
  } catch {
    weather.value = null
  } finally {
    weatherLoading.value = false
  }

  try {
    const { data } = await contextApi.evStations(lat, lng)
    evStations.value = Array.isArray(data) ? data.slice(0, 3) : []
  } catch {
    evStations.value = []
  }
}

// Static review seed (no reviews endpoint in API)
const STATIC_REVIEWS = [
  { id: 1, author: '여행가_지민', date: '2시간 전', rating: 5, content: '정말 아름다운 곳이었어요. 꼭 다시 오고 싶어요!' },
  { id: 2, author: '한국여행러버', date: '1일 전', rating: 5, content: '가족과 함께 방문했는데 아이들도 너무 좋아했어요.' },
  { id: 3, author: '힐링여행자', date: '3일 전', rating: 4, content: '풍경이 정말 멋있었습니다. 날씨가 좋은 날에 방문하시길 추천해요.' },
]

async function addToPlan() {
  if (!authStore.isAuthenticated) {
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  addLoading.value = true
  addMsg.value = ''
  planStore.error = null

  try {
    await planStore.loadPlans()

    let planId
    if (planStore.plans.length === 0) {
      // Auto-create a default plan
      const today = new Date()
      const endD = new Date()
      endD.setDate(today.getDate() + 2)
      const pad = (n) => String(n).padStart(2, '0')
      const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
      const created = await planStore.createPlan({
        title: '나의 여행',
        startDate: fmt(today),
        endDate: fmt(endD),
      })
      planId = created.id
    } else {
      planId = planStore.plans[0].id
      await planStore.loadPlan(planId)
    }

    const contentId = route.params.id
    const contentType = place.value?.contentTypeId ?? 12

    await planStore.addPlace(planId, 1, { contentId, contentType })
    addMsg.value = 'added'
  } catch (e) {
    const code = e?.response?.data?.code
    if (code === 'PLAN4093') {
      addMsg.value = 'duplicate'
    } else {
      addMsg.value = 'error'
    }
  } finally {
    addLoading.value = false
  }
}

function getRecommendation() {
  alert('비슷한 여행지를 추천해드릴게요!')
}

onMounted(async () => {
  const contentId = route.params.id
  if (!contentId) return
  loading.value = true
  fetchError.value = null
  try {
    place.value = await store.fetchDetail(contentId)
    // fetchDetail sets store.error on failure and returns mock — check if it errored
    if (store.error) {
      fetchError.value = store.error
    }
  } catch (e) {
    fetchError.value = e?.message ?? '정보를 불러올 수 없습니다.'
    place.value = store.currentAttraction
  } finally {
    loading.value = false
  }
  // 좌표가 있으면 날씨/충전소 맥락 정보 로드 (실패 시 위젯 숨김)
  if (hasCoords.value) loadContext()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}

.scroll-area {
  flex: 1;
  overflow-y: auto;
}

/* ── Hero ─────────────────────────────────────────────────────────────────── */
.hero {
  position: relative;
  height: 280px;
}

.hero-img {
  width: 100%;
  height: 100%;
}

.img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.skeleton-bg {
  animation: shimmer 1.4s infinite;
  background: linear-gradient(90deg, #efe6e4 25%, #e7e0d8 50%, #efe6e4 75%);
  background-size: 200% 100%;
}

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.img-caption {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: #a99f93;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(1px);
  padding: 4px 8px;
  border-radius: 6px;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.3) 0%,
    rgba(0, 0, 0, 0) 30%,
    rgba(0, 0, 0, 0) 50%,
    rgba(0, 0, 0, 0.5) 100%
  );
}

.top-bar {
  position: absolute;
  top: 56px;
  left: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ghost-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
}

.hero-info {
  position: absolute;
  bottom: 16px;
  left: 16px;
  right: 16px;
}

.rank-badge {
  display: inline-block;
  background: var(--color-peach);
  color: white;
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  margin-bottom: 8px;
}

.place-name {
  font-size: 26px;
  font-weight: 800;
  color: white;
  letter-spacing: -0.7px;
  line-height: 1.15;
  margin-bottom: 6px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-val {
  font-size: 14px;
  font-weight: 700;
  color: white;
}

.review-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

/* ── Error notice ──────────────────────────────────────────────────────────── */
.fetch-error-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 20px 0;
  padding: 8px 12px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-peach-pressed);
}

/* ── Meta ─────────────────────────────────────────────────────────────────── */
.meta-row {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13.5px;
  color: var(--color-ink-secondary);
}

/* ── Tags ─────────────────────────────────────────────────────────────────── */
.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 20px 16px;
}

.place-tag {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

/* ── Overview ─────────────────────────────────────────────────────────────── */
.overview-section {
  padding: 0 20px 16px;
  border-bottom: 1px solid var(--color-line-light);
}

.section-title {
  font-size: 17px;
  font-weight: 750;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 8px;
}

.overview-text {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
  line-height: 1.7;
  letter-spacing: -0.2px;
}

/* ── Actions ──────────────────────────────────────────────────────────────── */
.action-row {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-line-light);
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 11px 8px;
  border-radius: var(--radius-xl);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.action-btn.primary {
  background: var(--color-peach);
  color: white;
  flex: 1.3;
}

.action-btn.secondary {
  background: var(--color-surface);
  color: var(--color-ink-secondary);
}

/* ── Map section ──────────────────────────────────────────────────────────── */
.map-section {
  padding: 20px 20px 0;
}

.mini-map {
  height: 140px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
  margin-bottom: 8px;
}

.mini-map-bg {
  width: 100%;
  height: 100%;
  background: #e8f0e8;
  background-image: radial-gradient(circle at 50% 50%, #d4e8d4 0%, transparent 50%),
    linear-gradient(135deg, #e0ead8, #d8e8d8);
}

.map-pin-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.center-pin {
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.coords-hint {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  text-align: center;
  margin-bottom: 20px;
  font-family: var(--font-mono);
}

/* ── Reviews ──────────────────────────────────────────────────────────────── */
.reviews-section {
  padding: 0 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-top: 20px;
}

.see-all {
  font-size: 13px;
  color: var(--color-ink-muted);
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  padding: 14px 16px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rv-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
}

.rv-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
}

.rv-date {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}

.rv-rating {
  display: flex;
  gap: 2px;
}

.review-text {
  font-size: 13px;
  color: var(--color-dark-text);
  line-height: 1.6;
  letter-spacing: -0.2px;
}

/* ── 여행 맥락 정보 ───────────────────────────────────────────────────────── */
.context-section {
  padding: 20px 20px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ctx-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.ctx-loading,
.ctx-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ctx-loading-text,
.ctx-empty-text {
  font-size: 12.5px;
  color: var(--color-ink-muted);
}

.weather-now {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.weather-now-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.weather-temp {
  font-size: 30px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -1px;
  line-height: 1;
}

.weather-desc {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}

.weather-sun {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.sun-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: var(--color-ink-secondary);
  font-family: var(--font-mono);
}

.forecast-row {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--color-line-light);
}

.forecast-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.fc-day {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink);
}

.fc-desc {
  font-size: 11px;
  color: var(--color-ink-muted);
  text-align: center;
  min-height: 14px;
}

.fc-temp {
  display: flex;
  gap: 5px;
}

.fc-max {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
}

.fc-min {
  font-size: 12.5px;
  color: var(--color-ink-muted);
}

/* ── 충전소 ──────────────────────────────────────────────────────────────── */
.ev-header {
  margin-bottom: 10px;
}

.ev-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.demo-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-full);
  padding: 1px 7px;
}

.ev-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ev-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}

.ev-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ev-info {
  flex: 1;
  min-width: 0;
}

.ev-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-addr {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ev-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.ev-type {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-peach-pressed);
}

.ev-count {
  font-size: 11px;
  color: var(--color-ink-muted);
}

.bottom-spacer {
  height: 32px;
}
</style>
