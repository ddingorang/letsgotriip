# Created: 2026-06-16 13:26:52
<template>
  <div class="page">
    <!-- ── Search bar ─────────────────────────────────────────────────────── -->
    <div class="top-bar">
      <div class="search-bar">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>
        <input
          ref="searchInput"
          v-model="searchQuery"
          class="search-input"
          placeholder="지역·관광지 검색"
          @input="onSearchInput"
        />
        <button v-if="searchQuery" class="clear-btn" @click="clearSearch">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>

    <!-- ── Category chips ────────────────────────────────────────────────── -->
    <div class="category-scroll">
      <button
        v-for="cat in CATEGORIES"
        :key="cat.key"
        class="cat-pill"
        :class="{ active: selectedCategory === cat.key }"
        @click="selectCategory(cat.key)"
      >
        {{ cat.label }}
      </button>
    </div>

    <!-- ── Map (Leaflet + OpenStreetMap) ─────────────────────────────────── -->
    <div class="map-container">
      <TripMap
        :places="mapPlaces"
        :selected-id="selectedPlace?.contentId"
        :center="mapCenter"
        @select="selectPlace"
      />
    </div>

    <!-- ── Bottom sheet ──────────────────────────────────────────────────── -->
    <div class="bottom-sheet" :class="{ expanded: sheetExpanded }">
      <div class="sheet-handle" @click="sheetExpanded = !sheetExpanded" />
      <div class="sheet-header">
        <h2 class="sheet-title">
          {{ sortMode === 'distance' ? '내 주변 관광지' : '관광지' }}
          <span class="count">{{ displayedPlaces.length }}</span>
        </h2>
        <button class="sort-btn" :class="{ active: sortMode === 'distance' }" @click="toggleSort">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="7" y1="12" x2="17" y2="12" />
            <line x1="10" y1="18" x2="14" y2="18" />
          </svg>
          {{ sortLabel }}
        </button>
      </div>

      <!-- Loading skeleton -->
      <div v-if="store.loading" class="place-grid">
        <div v-for="n in 4" :key="n" class="place-row skeleton-row">
          <div class="img-placeholder small skeleton-box" />
          <div class="place-info">
            <div class="skeleton-line short" />
            <div class="skeleton-line" />
          </div>
        </div>
      </div>

      <!-- Error notice (fallback data still shown below) -->
      <div v-if="store.error && !store.loading" class="error-notice">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>연결 오류 — 저장된 데이터를 표시합니다.</span>
        <button class="retry-btn" @click="loadAttractions">재시도</button>
      </div>

      <!-- Place list -->
      <div v-if="!store.loading" class="place-grid">
        <div
          v-if="displayedPlaces.length === 0"
          class="empty-state"
        >
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5">
            <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
          </svg>
          <span>검색 결과가 없습니다.</span>
        </div>

        <div
          v-for="(place, idx) in displayedPlaces"
          :key="place.contentId"
          class="place-row"
          @click="$router.push(`/place/${place.contentId}`)"
        >
          <div class="place-img">
            <div class="img-placeholder small">
              <img
                :src="thumbSrc(place)"
                :alt="place.name"
                class="thumb-img"
                @error="(e) => onThumbError(e, place)"
              />
            </div>
          </div>
          <div class="place-info">
            <div class="place-name-row">
              <span class="place-name">{{ place.name }}</span>
              <div v-if="place.rating" class="rating">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="var(--color-gold)" stroke="none">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                </svg>
                <span>{{ place.rating }}</span>
              </div>
              <span v-else class="cat-badge">{{ place.category }}</span>
            </div>
            <span class="place-addr">
              <span v-if="Number.isFinite(place._dist)" class="place-dist">{{ formatDist(place._dist) }}</span>
              {{ place.address }}
            </span>
          </div>
        </div>
      </div>

      <!-- Festival section (if loaded and fits cleanly) -->
      <template v-if="festivalStore.festivals.length && !store.loading">
        <div class="sheet-header festival-header">
          <h2 class="sheet-title">
            진행중인 축제
            <span class="count">{{ festivalStore.festivals.slice(0, 6).length }}</span>
          </h2>
        </div>
        <div class="festival-grid">
          <div
            v-for="fest in festivalStore.festivals.slice(0, 6)"
            :key="fest.id"
            class="festival-row"
          >
            <div class="festival-thumb">
              <img :src="festThumb(fest)" :alt="fest.title" @error="(e) => onFestError(e, fest)" />
            </div>
            <div class="festival-info">
              <span class="festival-title">{{ fest.title }}</span>
              <span class="festival-addr">{{ fest.address }}</span>
              <span class="festival-date">{{ formatFestDate(fest.startDate) }} ~ {{ formatFestDate(fest.endDate) }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAttractionStore } from '@/stores/attraction.js'
import { useFestivalStore } from '@/stores/festival.js'
import TripMap from '@/components/common/TripMap.vue'

const store = useAttractionStore()
const festivalStore = useFestivalStore()

// ── State ─────────────────────────────────────────────────────────────────────
const searchQuery = ref('')
const sheetExpanded = ref(false)
const selectedPlace = ref(null)
const selectedCategory = ref('all')

// ── Category chip definitions ─────────────────────────────────────────────────
// contentTypeId mapping: 관광지=12, 축제행사=15, 음식점=39, 숙박=32, 전체=all
const CATEGORIES = [
  { key: 'all',  label: '전체',   contentTypeId: null },
  { key: '12',   label: '관광지', contentTypeId: 12   },
  { key: '15',   label: '축제',   contentTypeId: 15   },
  { key: '39',   label: '음식점', contentTypeId: 39   },
  { key: '32',   label: '숙박',   contentTypeId: 32   },
]

const PAGE_SIZE = 30   // 거리순 정렬이 의미있도록 후보를 넉넉히 받음

// ── 현재 위치 / 정렬 ─────────────────────────────────────────────────────────
const userLoc = ref(null)              // { lat, lng }
const sortMode = ref('default')        // 'default' | 'distance'

// 두 좌표 사이 거리(km) — Haversine
function distanceKm(a, b) {
  const R = 6371
  const dLat = ((b.lat - a.lat) * Math.PI) / 180
  const dLng = ((b.lng - a.lng) * Math.PI) / 180
  const lat1 = (a.lat * Math.PI) / 180
  const lat2 = (b.lat * Math.PI) / 180
  const x =
    Math.sin(dLat / 2) ** 2 +
    Math.sin(dLng / 2) ** 2 * Math.cos(lat1) * Math.cos(lat2)
  return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
}

// ── Filtered + sorted display list ────────────────────────────────────────────
const displayedPlaces = computed(() => {
  let list = store.attractions
  const q = searchQuery.value.trim()
  if (q) {
    list = list.filter(
      (p) =>
        p.name.includes(q) ||
        (p.address && p.address.includes(q))
    )
  }
  if (sortMode.value === 'distance' && userLoc.value) {
    list = [...list]
      .map((p) => ({
        ...p,
        _dist:
          Number.isFinite(p.lat) && Number.isFinite(p.lng)
            ? distanceKm(userLoc.value, p)
            : Infinity,
      }))
      .sort((a, b) => a._dist - b._dist)
  }
  return list
})

// 지도 중심 — 위치 권한 허용 시 현재 위치, 아니면 서울
const mapCenter = computed(() =>
  userLoc.value ? [userLoc.value.lat, userLoc.value.lng] : [37.5665, 126.978],
)

// 거리순일 땐 지도가 내 주변으로 줌되도록 가까운 곳만 핀 표시
const mapPlaces = computed(() =>
  sortMode.value === 'distance' && userLoc.value
    ? displayedPlaces.value.slice(0, 8)
    : displayedPlaces.value,
)

const sortLabel = computed(() =>
  sortMode.value === 'distance' ? '거리순' : '기본순',
)

function toggleSort() {
  if (sortMode.value === 'distance') {
    sortMode.value = 'default'
  } else if (userLoc.value) {
    sortMode.value = 'distance'
  } else {
    locateUser(true)   // 위치 먼저 확보 후 거리순
  }
}

// 위치가 있으면 BE 좌표(반경 20km) 검색 파라미터 — 검색어 없을 때만 사용
function locParams() {
  if (!userLoc.value) return {}
  return {
    mapX: String(userLoc.value.lng),
    mapY: String(userLoc.value.lat),
    radius: 20000,
  }
}

// 현재 위치 확보 — 성공 시 거리순 + 근처 목록 조회. 실패/미지원 시 전체 목록 폴백.
function locateUser(forceSort = false) {
  if (!navigator.geolocation) {
    if (!store.attractions.length) loadAttractions()   // 위치 미지원 → 전체
    return
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      userLoc.value = { lat: pos.coords.latitude, lng: pos.coords.longitude }
      if (forceSort || sortMode.value === 'default') sortMode.value = 'distance'
      // 위치 확보 → BE 좌표 검색으로 근처 목록 조회 (검색 중이 아닐 때)
      if (!searchQuery.value.trim()) loadAttractions()
    },
    () => {
      // 거부/실패 → 위치 없이 전체 목록(아직 안 불러왔으면)
      if (!store.attractions.length) loadAttractions()
    },
    { enableHighAccuracy: false, timeout: 7000, maximumAge: 300000 },
  )
}

// ── Actions ───────────────────────────────────────────────────────────────────
function selectCategory(key) {
  selectedCategory.value = key
  const cat = CATEGORIES.find((c) => c.key === key)
  const params = {
    size: PAGE_SIZE,
    ...locParams(),
    ...(cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}),
  }
  store.list(params)
}

function selectPlace(place) {
  selectedPlace.value = place
}

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const q = searchQuery.value.trim()
    if (q.length >= 2) {
      const cat = CATEGORIES.find((c) => c.key === selectedCategory.value)
      const params = {
        keyword: q,
        size: PAGE_SIZE,
        ...(cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}),
      }
      store.list(params)
    } else {
      // Query cleared below threshold — reload the unfiltered list for current category
      loadAttractions()
    }
  }, 600)
}

function clearSearch() {
  searchQuery.value = ''
  loadAttractions()
}

function loadAttractions() {
  const cat = CATEGORIES.find((c) => c.key === selectedCategory.value)
  const params = {
    size: PAGE_SIZE,
    ...locParams(),
    ...(cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}),
  }
  store.list(params)
}

function formatDist(km) {
  if (!Number.isFinite(km)) return ''
  return km < 1 ? `${Math.round(km * 1000)}m` : `${km.toFixed(1)}km`
}

// 썸네일 — 실제 이미지가 없으면 id 기반 결정적 사진(picsum)으로 채움
function placePlaceholder(place) {
  return `https://picsum.photos/seed/triip-${place.contentId ?? place.name}/240/240`
}
function thumbSrc(place) {
  return place.imageUrl || placePlaceholder(place)
}
function onThumbError(e, place) {
  const fb = placePlaceholder(place)
  if (e.target.src !== fb) e.target.src = fb
}
function festPlaceholder(fest) {
  return `https://picsum.photos/seed/fest-${fest.id ?? fest.title}/240/240`
}
function festThumb(fest) {
  return fest.image || festPlaceholder(fest)
}
function onFestError(e, fest) {
  const fb = festPlaceholder(fest)
  if (e.target.src !== fb) e.target.src = fb
}

function formatFestDate(raw) {
  if (!raw || raw.length < 8) return raw
  return `${raw.slice(0, 4)}.${raw.slice(4, 6)}.${raw.slice(6, 8)}`
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────
onMounted(() => {
  // 위치 우선 — 진입 즉시 내 주변부터. 위치 확보까지 스켈레톤(빈결과 깜빡임 방지),
  // 성공 시 근처 목록, 거부/실패/미지원 시 전체 목록으로 폴백(locateUser 내부에서 처리).
  store.loading = true
  festivalStore.loadFestivals()              // festival section (non-blocking)
  store.loadAreas()                          // area list for potential future use
  locateUser()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

/* ── Top bar ──────────────────────────────────────────────────────────────── */
.top-bar {
  padding: 12px 16px 0;
  background: var(--color-white);
  z-index: 10;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 11px 16px;
}

.search-input {
  flex: 1;
  font-size: 14px;
  color: var(--color-ink);
  background: transparent;
  letter-spacing: -0.2px;
}

.search-input::placeholder {
  color: var(--color-ink-muted);
}

.clear-btn {
  color: var(--color-ink-muted);
}

/* ── Category chips ───────────────────────────────────────────────────────── */
.category-scroll {
  display: flex;
  gap: 8px;
  padding: 10px 16px;
  overflow-x: auto;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
}

.cat-pill {
  flex-shrink: 0;
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  letter-spacing: -0.2px;
  transition: all 0.15s;
}

.cat-pill.active {
  background: var(--color-peach);
  color: white;
}

/* ── Map ──────────────────────────────────────────────────────────────────── */
.map-container {
  flex: 1;
  min-height: 0;
  position: relative;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.map-bg {
  width: 100%;
  height: 100%;
  background: #e8f0e8;
  background-image: radial-gradient(circle at 30% 40%, #d4e8d4 0%, transparent 40%),
    radial-gradient(circle at 70% 60%, #c8ddc8 0%, transparent 35%),
    linear-gradient(135deg, #e0ead8 0%, #d8e8d8 50%, #cce0cc 100%);
}

.map-pin {
  position: absolute;
  transform: translate(-50%, -100%);
  cursor: pointer;
}

.pin-bubble {
  width: 32px;
  height: 32px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  background: var(--color-white);
  border: 2px solid var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-ink);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.15s;
}

.pin-bubble > * {
  transform: rotate(45deg);
}

.pin-bubble span {
  transform: rotate(45deg);
}

.pin-bubble.selected {
  background: var(--color-peach);
  color: white;
  border-color: var(--color-peach-pressed);
  transform: rotate(-45deg) scale(1.15);
}

.location-btn {
  position: absolute;
  bottom: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-white);
  box-shadow: var(--shadow-card);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
}

/* ── Bottom sheet ─────────────────────────────────────────────────────────── */
.bottom-sheet {
  background: var(--color-white);
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.08);
  max-height: 40%;
  overflow-y: auto;
  transition: max-height 0.3s ease;
  flex-shrink: 0;
}

.bottom-sheet.expanded {
  max-height: 65%;
}

.sheet-handle {
  width: 36px;
  height: 4px;
  background: var(--color-line);
  border-radius: 2px;
  margin: 10px auto 0;
  cursor: pointer;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}

.festival-header {
  padding-top: 4px;
  border-top: 1px solid var(--color-line-light);
  margin-top: 4px;
}

.sheet-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.count {
  color: var(--color-peach);
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-ink-muted);
  cursor: pointer;
  transition: color 0.15s;
}

.sort-btn.active {
  color: var(--color-peach);
  font-weight: 600;
}

.place-dist {
  color: var(--color-peach);
  font-weight: 600;
  margin-right: 2px;
}

/* ── Place grid / rows ────────────────────────────────────────────────────── */
.place-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 8px 16px;
}

.place-row {
  display: flex;
  gap: 12px;
  cursor: pointer;
  align-items: center;
  padding: 8px;
  border-radius: var(--radius-md);
  transition: background 0.12s;
}

.place-row:active {
  background: var(--color-surface);
}

.place-img {
  flex-shrink: 0;
}

.img-placeholder.small {
  width: 66px;
  height: 66px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rank-num {
  font-size: 16px;
  font-weight: 800;
  color: var(--color-peach);
}

.place-info {
  flex: 1;
  min-width: 0;
}

.place-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-bottom: 2px;
}

.place-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rating {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink);
  flex-shrink: 0;
}

.cat-badge {
  font-size: 11px;
  color: var(--color-peach);
  font-weight: 600;
  flex-shrink: 0;
}

.place-addr {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

/* ── Loading skeleton ─────────────────────────────────────────────────────── */
.skeleton-row {
  pointer-events: none;
}

.skeleton-box {
  background: linear-gradient(90deg, #efe6e4 25%, #e7e0d8 50%, #efe6e4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line {
  height: 11px;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, #efe6e4 25%, #e7e0d8 50%, #efe6e4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  margin-bottom: 6px;
  width: 80%;
}

.skeleton-line.short {
  width: 50%;
}

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Error / empty ────────────────────────────────────────────────────────── */
.error-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 16px 10px;
  padding: 8px 12px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--color-peach-pressed);
}

.retry-btn {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-peach);
  text-decoration: underline;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 0;
  color: var(--color-ink-muted);
  font-size: 13px;
}

/* ── Festival section ─────────────────────────────────────────────────────── */
.festival-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 8px 16px;
}

.festival-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: var(--radius-md);
}

.festival-thumb {
  width: 66px;
  height: 66px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
}

.festival-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.festival-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.festival-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.festival-addr {
  display: block;
  font-size: 12px;
  color: var(--color-ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.festival-date {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-peach);
}
</style>
