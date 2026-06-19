<template>
  <div class="page">
    <!-- Top nav -->
    <div class="nav-bar">
      <button class="nav-btn" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
      </button>
      <span class="nav-title">AI 동선 리포트</span>
      <div class="nav-spacer" />
    </div>

    <!-- Loading -->
    <div v-if="planStore.loading" class="state-wrap">
      <div class="skeleton-body">
        <div class="skeleton-line w80" />
        <div class="skeleton-line w60" />
        <div class="skeleton-line w90" />
        <div class="skeleton-line w50" />
      </div>
    </div>

    <!-- Error / not found -->
    <div v-else-if="planStore.error || !plan" class="state-wrap">
      <div class="empty-icon">
        <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <p class="state-title">리포트를 불러올 수 없어요</p>
      <p class="state-sub">{{ planStore.error ?? '계획 정보를 찾을 수 없어요.' }}</p>
      <button class="state-btn" @click="router.back()">돌아가기</button>
    </div>

    <!-- Report content -->
    <template v-else>
      <!-- Summary banner -->
      <div class="summary-banner">
        <div class="banner-glow" />
        <div class="banner-info">
          <div class="banner-label">AI 동선 최적화 리포트</div>
          <div class="banner-title">{{ plan.title }}</div>
          <div class="banner-tags">
            <span class="banner-tag">총 {{ totalPlaces }}개 장소</span>
            <span class="banner-tag">{{ totalDays }}일 일정</span>
            <span v-if="report?.totalDistanceKm" class="banner-tag">이동 {{ report.totalDistanceKm }}km</span>
          </div>
        </div>
        <div class="route-badge">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
        </div>
      </div>

      <!-- Optimization note -->
      <div class="opt-note">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        {{ optNote }}
      </div>

      <!-- Scroll area -->
      <div class="scroll-content">
        <!-- Per-day optimized route -->
        <div
          v-for="day in days"
          :key="day.dayNo"
          class="day-section"
        >
          <div class="day-header">
            <span class="day-pill">{{ day.dayNo }}일차</span>
            <span v-if="day.summary" class="day-summary">{{ day.summary }}</span>
          </div>

          <!-- Route stats row -->
          <div class="route-stats">
            <div class="stat-item">
              <span class="stat-label">장소 수</span>
              <span class="stat-value">{{ day.places?.length ?? 0 }}곳</span>
            </div>
            <div class="stat-dot" />
            <div class="stat-item">
              <span class="stat-label">예상 이동</span>
              <span class="stat-value">{{ estimatedDistance(day.dayNo) }}</span>
            </div>
            <div class="stat-dot" />
            <div class="stat-item">
              <span class="stat-label">소요 시간</span>
              <span class="stat-value">{{ estimatedDuration(day.dayNo) }}</span>
            </div>
          </div>

          <!-- Optimized place list -->
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
                  <div class="place-name">{{ place.attraction?.title ?? place.title ?? '장소' }}</div>
                  <div v-if="place.visitTime" class="place-time-row">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    {{ place.visitTime }}
                  </div>
                  <div v-if="idx < day.places.length - 1 && travelTime(idx, day.dayNo)" class="travel-hint">
                    다음 장소까지 {{ travelTime(idx, day.dayNo) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-day">이 날의 일정이 없어요</div>
        </div>

        <div v-if="!days.length" class="no-days">
          <p>등록된 일정이 없어요.</p>
          <p class="no-days-sub">AI 플래너로 일정을 먼저 생성해보세요!</p>
        </div>

        <div class="bottom-spacer" />
      </div>

      <!-- Bottom action bar -->
      <div class="bottom-bar">
        <button class="btn-apply" :disabled="applying || planStore.loading || !canApply" @click="applyRoute">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12" /></svg>
          {{ applyLabel }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePlanStore } from '@/stores/plan.js'

const router = useRouter()
const route = useRoute()
const planStore = usePlanStore()

const planId = route.params.id

const applying = ref(false)

onMounted(async () => {
  try {
    await planStore.loadPlan(planId)
    await planStore.loadRouteReport(planId)   // 좌표 기반 거리·추천순서 (BE Haversine)
  } catch {
    // error shown via planStore.error
  }
})

const plan = computed(() => planStore.current?.id == planId ? planStore.current : null)
const days = computed(() => plan.value?.days ?? [])
const totalDays = computed(() => days.value.length)
const totalPlaces = computed(() => days.value.reduce((acc, d) => acc + (d.places?.length ?? 0), 0))

// ── 동선 리포트: BE 실데이터를 주 데이터원으로, 좌표 기반 Haversine을 폴백으로 ──
// 우선순위 1) 서버 동선 리포트(planStore.routeReport, BE RouteCalculator 계산값)
//          2) 서버 리포트가 없거나 해당 일자가 비면 FE Haversine 추정(좌표 보유 시)
// 이동시간/거리는 외부 라우팅 API가 아닌 직선거리 기반 추정값이다.

const report = computed(() => planStore.routeReport)
function dayReport(dayNo) {
  return report.value?.days?.find((d) => d.dayNo === dayNo) ?? null
}
// 서버가 한 곳이라도 추천 순서가 현재와 다르다고 하면 "대체 동선 적용" 가능
const hasSuggestion = computed(() =>
  (report.value?.days ?? []).some((d) => d.reorderSuggested),
)

// ── FE 폴백용 Haversine 계산 ────────────────────────────────────────────────
// 장소의 위경도(attraction.latitude / attraction.longitude, TourAPI mapy/mapx)를
// 사용해 직선거리를 합산한다. 도보 ≤2km 4.5km/h, 그 이상 차량 30km/h를 가정.
const WALK_THRESHOLD_KM = 2   // 이 거리 이하면 도보로 가정
const WALK_SPEED_KMH = 4.5
const DRIVE_SPEED_KMH = 30

function coordOf(place) {
  const a = place?.attraction
  const lat = a?.latitude
  const lng = a?.longitude
  if (lat == null || lng == null) return null
  return { lat: Number(lat), lng: Number(lng) }
}

/** 두 좌표 사이 Haversine 거리(km) */
function haversineKm(a, b) {
  const R = 6371
  const dLat = ((b.lat - a.lat) * Math.PI) / 180
  const dLng = ((b.lng - a.lng) * Math.PI) / 180
  const lat1 = (a.lat * Math.PI) / 180
  const lat2 = (b.lat * Math.PI) / 180
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)))
}

/** 한 구간(직선)의 이동시간(분) — 거리에 따라 도보/차량 속도 가정 */
function legMinutes(km) {
  const speed = km <= WALK_THRESHOLD_KM ? WALK_SPEED_KMH : DRIVE_SPEED_KMH
  return (km / speed) * 60
}

/** 해당 일자의 place 배열을 dayNo로 조회 */
function placesOf(dayNo) {
  return days.value.find((d) => d.dayNo === dayNo)?.places ?? []
}

/**
 * 하루 일정의 총 직선 이동거리(km)와 좌표 보유 여부를 계산.
 * coords: 좌표가 있는 장소 목록 수, missing: 좌표가 없는 장소 수
 */
function dayDistance(places) {
  const list = places ?? []
  const coords = []
  let missing = 0
  for (const p of list) {
    const c = coordOf(p)
    if (c) coords.push(c)
    else missing++
  }
  let km = 0
  for (let i = 0; i < coords.length - 1; i++) {
    km += haversineKm(coords[i], coords[i + 1])
  }
  return { km, usable: coords.length, missing }
}

/** 하루 전체 구간 이동시간 합(분, FE 추정) */
function legSumMinutes(places) {
  const list = places ?? []
  let mins = 0
  for (let i = 0; i < list.length - 1; i++) {
    const a = coordOf(list[i])
    const b = coordOf(list[i + 1])
    if (a && b) mins += legMinutes(haversineKm(a, b))
  }
  return mins
}

// ── 화면용 텍스트: 서버 리포트 우선, 없으면 FE 폴백 ───────────────────────────

/** 예상 이동 거리 텍스트 */
function estimatedDistance(dayNo) {
  const r = dayReport(dayNo)
  if (r) {
    if (r.placeCount <= 1) return '–'
    return `약 ${r.totalDistanceKm ?? r.distanceKm}km`
  }
  // 폴백: FE Haversine
  const places = placesOf(dayNo)
  if ((places.length ?? 0) <= 1) return '–'
  const { km, usable, missing } = dayDistance(places)
  if (usable < 2) return '좌표 없음'
  const txt = km < 1 ? `약 ${Math.round(km * 1000)}m` : `약 ${km.toFixed(1)}km`
  return missing > 0 ? `${txt}~` : txt
}

/** 예상 소요 시간 텍스트 */
function estimatedDuration(dayNo) {
  const r = dayReport(dayNo)
  if (r) {
    if (r.placeCount === 0) return '–'
    return formatMinutes(r.estimatedMinutes)
  }
  // 폴백: FE Haversine (장소당 체류 60분 가정)
  const places = placesOf(dayNo)
  const n = places.length ?? 0
  if (n === 0) return '–'
  const { usable } = dayDistance(places)
  if (usable < 2) return '추정 불가'
  const STAY_MIN = 60
  const mins = Math.round(n * STAY_MIN + legSumMinutes(places))
  return formatMinutes(mins)
}

/** 분(minutes) → "약 N시간 M분" 텍스트 */
function formatMinutes(mins) {
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h > 0 ? `약 ${h}시간${m > 0 ? ' ' + m + '분' : ''}` : `약 ${m}분`
}

/** 구간별 이동시간 텍스트 — 서버 leg 우선, 없으면 FE 추정 */
function travelTime(idx, dayNo) {
  const r = dayReport(dayNo)
  const leg = r?.legs?.[idx]
  if (leg) {
    const mins = Math.max(1, Math.round((leg.distanceKm / DRIVE_SPEED_KMH) * 60)) // 30km/h 가정
    return `${leg.distanceKm}km · 약 ${mins}분`
  }
  // 폴백: FE Haversine
  const places = placesOf(dayNo)
  if (!places.length || idx >= places.length - 1) return ''
  const a = coordOf(places[idx])
  const b = coordOf(places[idx + 1])
  if (!a || !b) return '거리 추정 불가'
  const km = haversineKm(a, b)
  const mins = Math.round(legMinutes(km))
  const mode = km <= WALK_THRESHOLD_KM ? '도보' : '차량'
  const dist = km < 1 ? `${Math.round(km * 1000)}m` : `${km.toFixed(1)}km`
  return `${dist} · ${mode} ${mins}분`
}

// ── 대체 동선 적용 (재정렬) ──────────────────────────────────────────────────

/** 하루 일정 중 좌표를 가진 장소가 2곳 이상이면 FE 거리기반 재정렬 가능 */
function canOptimize(places) {
  const list = places ?? []
  let usable = 0
  for (const p of list) if (coordOf(p)) usable++
  return usable >= 2
}

/** FE 폴백으로라도 재정렬 가능한 날이 하나라도 있는지 */
const hasOptimizable = computed(() => days.value.some((d) => canOptimize(d.places)))

// 서버 추천이 있거나, 서버 리포트가 없더라도 FE로 재정렬할 수 있으면 적용 버튼 활성
const canApply = computed(() => {
  if (report.value) return hasSuggestion.value
  return hasOptimizable.value
})

const optNote = computed(() => {
  if (report.value) {
    return hasSuggestion.value
      ? '더 짧은 동선을 찾았어요. 아래에서 적용해 보세요.'
      : '장소 간 좌표 거리로 계산한 동선이에요'
  }
  return `거리·시간은 장소 좌표 기준 직선거리 추정값이에요 (도보 ${WALK_THRESHOLD_KM}km 이하·차량 가정)`
})

const applyLabel = computed(() => {
  if (applying.value) return '적용 중...'
  if (report.value) return hasSuggestion.value ? '대체 동선 적용' : '확인'
  return hasOptimizable.value ? '거리 기반 동선 적용' : '좌표가 없어 적용 불가'
})

/**
 * 최근접 이웃(greedy nearest-neighbor)으로 장소 순서를 재정렬 (FE 폴백).
 * 첫 장소를 출발점으로 고정하고, 가장 가까운 장소를 차례로 잇는다.
 * 좌표가 없는 장소는 원래 상대순서를 유지하며 뒤에 붙인다.
 */
function reorderByDistance(places) {
  const list = [...(places ?? [])]
  const withCoord = list.filter((p) => coordOf(p))
  const without = list.filter((p) => !coordOf(p))
  if (withCoord.length < 2) return list

  const remaining = [...withCoord]
  const ordered = [remaining.shift()] // 첫 장소 고정
  while (remaining.length) {
    const last = coordOf(ordered[ordered.length - 1])
    let bestIdx = 0
    let bestKm = Infinity
    remaining.forEach((p, i) => {
      const km = haversineKm(last, coordOf(p))
      if (km < bestKm) {
        bestKm = km
        bestIdx = i
      }
    })
    ordered.push(remaining.splice(bestIdx, 1)[0])
  }
  // 좌표 없는 장소는 끝에 추가(거리 계산 불가하므로 후순위)
  return [...ordered, ...without]
}

/**
 * "대체 동선 적용" — 서버 추천 순서(suggestedOrder)가 있으면 그것으로,
 * 서버 리포트가 없으면 FE 거리기반(nearest-neighbor)으로 각 일자를 재배치.
 * 기존 PUT /api/plans/{id}/days/{dayNo}/places(replacePlaces)를 재사용한다.
 */
async function applyRoute() {
  if (applying.value) return

  // 서버 리포트가 있는 경우
  if (report.value) {
    if (!hasSuggestion.value) {
      router.replace('/plan')
      return
    }
    applying.value = true
    try {
      for (const dr of report.value.days) {
        if (!dr.reorderSuggested) continue
        const day = days.value.find((d) => d.dayNo === dr.dayNo)
        if (!day?.places?.length) continue
        // suggestedOrder(placeId 목록) → 실제 place 객체 순서로 매핑
        const byId = new Map(day.places.map((p) => [p.id, p]))
        const reordered = dr.suggestedOrder.map((id) => byId.get(id)).filter(Boolean)
        if (reordered.length === day.places.length) {
          await planStore.replacePlaces(planId, dr.dayNo, reordered)
        }
      }
      await planStore.loadRouteReport(planId).catch(() => {})
      router.replace('/plan')
    } catch {
      // error in planStore.error
    } finally {
      applying.value = false
    }
    return
  }

  // 폴백: 서버 리포트가 없을 때 FE 거리기반 재정렬
  const targets = days.value.filter((d) => canOptimize(d.places))
  if (!targets.length) return
  applying.value = true
  try {
    for (const day of targets) {
      const reordered = reorderByDistance(day.places)
      await planStore.replacePlaces(planId, day.dayNo, reordered)
    }
    router.replace('/plan')
  } catch {
    // 오류는 planStore.error에 반영됨 (상단 에러 상태로 표시)
  } finally {
    applying.value = false
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

/* ── Optimization note ────────────────────────────────────────────────────── */
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
  height: 100px;
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

/* ── Route stats ──────────────────────────────────────────────────────────── */
.route-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  margin-bottom: 12px;
  box-shadow: var(--shadow-card);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.stat-label {
  font-size: 10.5px;
  color: var(--color-ink-muted);
  font-weight: 500;
}

.stat-value {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.stat-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-line);
  flex-shrink: 0;
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

.place-time-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-peach);
  margin-bottom: 4px;
}

.travel-hint {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  font-style: italic;
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

.no-days-sub {
  font-size: 13px !important;
  font-weight: 400 !important;
  margin-top: 8px;
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
  z-index: 10;
}

.btn-apply {
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
}

.btn-apply:disabled {
  background: var(--color-line);
  color: var(--color-ink-muted);
  box-shadow: none;
  cursor: not-allowed;
}
</style>
