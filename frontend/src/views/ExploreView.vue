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
      <!-- '#' 입력 시 태그 제안 — 탭하면 해당 태그(인기순)로 -->
      <div v-if="tagSuggestions.length" class="tag-suggest">
        <button
          v-for="t in tagSuggestions"
          :key="t.key"
          class="tag-suggest-item"
          @click="applyTagSuggest(t.key)"
        >#{{ t.label }}</button>
      </div>
    </div>

    <!-- ── Category chips ────────────────────────────────────────────────── -->
    <div class="category-scroll">
      <!-- 태그(인기순) 모드 표시 + 해제 칩 -->
      <button v-if="activeTag" class="cat-pill tag-pill active" @click="clearTag">
        #{{ TAG_LABELS[activeTag] }} ✕
      </button>
      <button
        v-for="cat in CATEGORIES"
        :key="cat.key"
        class="cat-pill"
        :class="{ active: !activeTag && selectedCategory === cat.key }"
        :style="!activeTag && selectedCategory === cat.key ? { background: CATEGORY_COLORS[cat.key], borderColor: CATEGORY_COLORS[cat.key] } : {}"
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
        :category-colors="pinColorMap"
        :fit="mapFit"
        @select="selectPlace"
        @detail="goDetail"
        @move="onMapMove"
      />
      <button v-if="showMapSearchBtn" class="map-search-btn" @click="searchByMapCenter">
        현 지도에서 검색
      </button>
    </div>

    <!-- ── Bottom sheet ──────────────────────────────────────────────────── -->
    <div ref="bottomSheet" class="bottom-sheet" :class="{ dragging: sheetDragging }" :style="{ height: sheetH + 'vh' }">
      <div class="sheet-handle-wrap" @pointerdown="onSheetPointerDown">
        <div class="sheet-handle" />
      </div>
      <div class="sheet-header">
        <h2 class="sheet-title">
          {{ sheetTitle }}
          <span class="count">{{ displayedPlaces.length }}</span>
        </h2>
        <button class="sort-btn" :class="{ active: sortMode === 'default' || sortMode === 'distance' }" @click="openSortSheet">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="7" y1="12" x2="17" y2="12" />
            <line x1="10" y1="18" x2="14" y2="18" />
          </svg>
          {{ sortLabel }}
          <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>
      </div>

      <!-- 지도가 넓어 조회 반경(최대 20km)이 화면보다 작을 때 — 정직하게 안내 -->
      <div v-if="areaCapped" class="area-cap-hint">
        지도가 넓어 중심 반경 20km만 조회됐어요. 확대하면 보이는 영역과 정확히 맞춰져요.
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
          :ref="(el) => setRowRef(place.contentId, el)"
          class="place-row"
          :class="{ selected: selectedPlace && String(place.contentId) === String(selectedPlace.contentId) }"
          @click="onRowClick(place)"
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
              <!-- 태그(인기순) 모드: 좋아요 수 표시 -->
              <div v-if="Number.isFinite(place.likeCount)" class="like-badge">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="var(--color-peach)" stroke="none">
                  <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 10-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 000-7.78z"/>
                </svg>
                <span>{{ place.likeCount }}</span>
              </div>
              <div v-else-if="place.rating" class="rating">
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

        <!-- 무한스크롤 센티넬 — 화면에 들어오면 다음 페이지 로드 (검색 중에도 동작) -->
        <div
          v-if="displayedPlaces.length && store.hasMore"
          ref="loadMoreSentinel"
          class="load-more-sentinel"
        >
          <div v-if="store.loadingMore" class="load-more-spinner">
            <span class="spinner-dot" /><span class="spinner-dot" /><span class="spinner-dot" />
          </div>
        </div>
        <div v-else-if="displayedPlaces.length && !store.hasMore" class="list-end">
          마지막 결과입니다
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

    <!-- ── 정렬 기준 바텀시트 ──────────────────────────────────────────────── -->
    <transition name="sort-fade">
      <div v-if="sortSheetOpen" class="sort-overlay" @click.self="closeSortSheet">
        <div class="sort-sheet">
          <div class="sort-sheet-handle" />
          <h3 class="sort-sheet-title">정렬 기준</h3>
          <button
            v-for="opt in SORT_OPTIONS"
            :key="opt.key"
            class="sort-opt"
            :class="{ active: sortMode === opt.key, disabled: opt.requiresLoc && !userLoc }"
            @click="selectSort(opt.key)"
          >
            <span class="sort-opt-label">
              {{ opt.label }}
              <span v-if="opt.requiresLoc && !userLoc" class="sort-opt-hint">위치 권한 필요</span>
            </span>
            <svg
              v-if="sortMode === opt.key"
              width="16" height="16" viewBox="0 0 24 24"
              fill="none" stroke="var(--color-peach)" stroke-width="2.5"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAttractionStore } from '@/stores/attraction.js'
import { useFestivalStore } from '@/stores/festival.js'
import { useLocationStore } from '@/stores/location.js'
import { attractionApi } from '@/api/index.js'
import TripMap from '@/components/common/TripMap.vue'

const router = useRouter()
const route = useRoute()
const store = useAttractionStore()
const festivalStore = useFestivalStore()
const locationStore = useLocationStore()

// ── State ─────────────────────────────────────────────────────────────────────
const searchQuery = ref('')
const selectedPlace = ref(null)

// ── 태그 큐레이션 모드 (홈 칩/‘#태그’ 검색 → 좋아요 인기순) ──────────────────────
const TAG_LABELS = { food: '맛집', culture: '문화·역사', activity: '액티비티', night: '야경' }
// 검색어 → 태그 매핑(‘#맛집’ 또는 ‘맛집’ 입력 시)
const KEYWORD_TO_TAG = {
  맛집: 'food', 음식: 'food', 맛: 'food',
  문화: 'culture', 역사: 'culture', 박물관: 'culture', 고궁: 'culture',
  액티비티: 'activity', 레저: 'activity', 체험: 'activity',
  야경: 'night', 전망: 'night', 야간: 'night',
}
const activeTag = ref(null)        // 'food'|'culture'|'activity'|'night' | null
const curatedItems = ref([])       // 큐레이트 결과(좋아요순) — mapAttraction 형태 + likeCount
const curatedLoading = ref(false)

// ── 드래그 바텀시트 (높이 vh, 스냅 3단) ───────────────────────────────────────
const SHEET_SNAPS = [26, 52, 82]   // 접힘 / 중간 / 펼침 (viewport height %)
const sheetH = ref(48)             // 현재 시트 높이(vh) — 인라인 style로 바인딩
const sheetDragging = ref(false)
let dragStartY = 0
let dragStartH = 0
let dragMoved = 0

function expandSheet() {
  sheetH.value = SHEET_SNAPS[2]
}
function nearestSnap(v) {
  return SHEET_SNAPS.reduce((best, s) => (Math.abs(s - v) < Math.abs(best - v) ? s : best), SHEET_SNAPS[0])
}
function onSheetPointerDown(e) {
  e.preventDefault()
  sheetDragging.value = true
  dragStartY = e.clientY
  dragStartH = sheetH.value
  dragMoved = 0
  try { e.currentTarget.setPointerCapture(e.pointerId) } catch { /* noop */ }
  window.addEventListener('pointermove', onSheetPointerMove, { passive: false })
  window.addEventListener('pointerup', onSheetPointerUp, { once: true })
  window.addEventListener('pointercancel', onSheetPointerUp, { once: true })
}
function onSheetPointerMove(e) {
  if (!sheetDragging.value) return
  e.preventDefault()
  dragMoved = Math.max(dragMoved, Math.abs(e.clientY - dragStartY))
  const dyVh = ((dragStartY - e.clientY) / window.innerHeight) * 100 // 위로 끌면 +
  sheetH.value = Math.min(88, Math.max(18, dragStartH + dyVh))
}
function onSheetPointerUp() {
  window.removeEventListener('pointermove', onSheetPointerMove)
  window.removeEventListener('pointerup', onSheetPointerUp)
  window.removeEventListener('pointercancel', onSheetPointerUp)
  sheetDragging.value = false
  if (dragMoved < 8) {
    // 거의 안 움직였으면 탭으로 간주 → 다음 스냅으로 순환
    const idx = SHEET_SNAPS.indexOf(nearestSnap(sheetH.value))
    sheetH.value = SHEET_SNAPS[(idx + 1) % SHEET_SNAPS.length]
  } else {
    sheetH.value = nearestSnap(sheetH.value)
  }
}
const selectedCategory = ref('all')

// 무한스크롤 — 바닥 센티넬이 보이면 다음 페이지를 누적 로드
const bottomSheet = ref(null)          // 스크롤 컨테이너(IntersectionObserver root)
const loadMoreSentinel = ref(null)     // 목록 끝 감지 대상
let io = null                          // IntersectionObserver 인스턴스

// ── Category chip definitions ─────────────────────────────────────────────────
// contentTypeId mapping: 관광지=12, 축제행사=15, 음식점=39, 숙박=32, 전체=all
const CATEGORIES = [
  { key: 'all',  label: '전체',   contentTypeId: null },
  { key: '12',   label: '관광지', contentTypeId: 12   },
  { key: '15',   label: '축제',   contentTypeId: 15   },
  { key: '39',   label: '음식점', contentTypeId: 39   },
  { key: '32',   label: '숙박',   contentTypeId: 32   },
]

// 카테고리별 핀·칩 색상 (contentTypeId 기준)
const CATEGORY_COLORS = {
  all:  '#F78F57', // 브랜드 피치
  '12': '#5B9BD5', // 관광지 — 스틸 블루
  '15': '#D45B6A', // 축제 — 코랄 레드
  '39': '#E8A020', // 음식점 — 웜 앰버
  '32': '#5BA888', // 숙박 — 세이지 그린
}

// TripMap에 전달할 contentTypeId → color 맵
const pinColorMap = {
  '12': CATEGORY_COLORS['12'],
  '15': CATEGORY_COLORS['15'],
  '39': CATEGORY_COLORS['39'],
  '32': CATEGORY_COLORS['32'],
}

const PAGE_SIZE = 30   // 거리순 정렬이 의미있도록 후보를 넉넉히 받음

// ── 현재 위치 / 정렬 ─────────────────────────────────────────────────────────
const userLoc = ref(null)              // { lat, lng }
const sortMode = ref('name')           // 'default' | 'distance' | 'name'
const sortSheetOpen = ref(false)       // 정렬 기준 바텀시트 표시 여부

// 정렬 기준 정의 — key/label/설명. requiresLoc 인 항목은 위치 없으면 비활성.
// NOTE: TourAPI 는 평점 데이터를 제공하지 않아(rating 항상 null) 평점순은 제외.
const SORT_OPTIONS = [
  { key: 'default',  label: '추천순',        requiresLoc: false },
  { key: 'distance', label: '거리순',        requiresLoc: true  },
  { key: 'name',     label: '이름순(가나다)', requiresLoc: false },
]

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

const RECOMMEND_SORT_FIELDS = [
  'recommendationScore',
  'recommendScore',
  'score',
  'popularityScore',
  'likeCount',
  'rating',
  'reviewCount',
]

function finiteNumber(value) {
  if (value == null || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function comparePlaceName(a, b) {
  const aName = String(a.name ?? '').trim()
  const bName = String(b.name ?? '').trim()
  if (aName && !bName) return -1
  if (!aName && bName) return 1
  const byName = aName.localeCompare(bName, 'ko')
  if (byName !== 0) return byName
  return String(a.contentId ?? a.id ?? '').localeCompare(
    String(b.contentId ?? b.id ?? ''),
    'ko',
    { numeric: true },
  )
}

function compareRecommendation(a, b) {
  for (const field of RECOMMEND_SORT_FIELDS) {
    const aValue = finiteNumber(a[field])
    const bValue = finiteNumber(b[field])
    if (aValue == null && bValue == null) continue
    if (aValue == null) return 1
    if (bValue == null) return -1
    if (aValue !== bValue) return bValue - aValue
  }
  return comparePlaceName(a, b)
}

function compareDistance(a, b) {
  const aDist = finiteNumber(a._dist) ?? Infinity
  const bDist = finiteNumber(b._dist) ?? Infinity
  if (aDist !== bDist) return aDist < bDist ? -1 : 1
  return comparePlaceName(a, b)
}

// ── Filtered + sorted display list ────────────────────────────────────────────
const displayedPlaces = computed(() => {
  // 태그모드면 큐레이트(좋아요 인기순) 결과, 아니면 일반 탐색 목록.
  let list = activeTag.value ? curatedItems.value : store.attractions
  const q = searchQuery.value.trim()
  // '#태그' 검색어는 모드 트리거일 뿐 필터가 아니므로 일반 텍스트만 필터.
  const textQ = q.startsWith('#') ? '' : q
  if (textQ) {
    list = list.filter(
      (p) =>
        p.name.includes(textQ) ||
        (p.address && p.address.includes(textQ))
    )
  }
  // 위치가 있으면 항상 _dist 를 계산해 row 에 거리 배지를 노출(정렬 여부와 무관).
  if (userLoc.value) {
    list = list.map((p) => ({
      ...p,
      _dist:
        Number.isFinite(p.lat) && Number.isFinite(p.lng)
          ? distanceKm(userLoc.value, p)
          : Infinity,
    }))
  }
  // 정렬 적용 — 원본을 변형하지 않도록 복사 후 정렬.
  if (sortMode.value === 'distance' && userLoc.value) {
    // 거리순 — 가까운 순. 좌표 없는 항목(_dist=Infinity)은 뒤로.
    list = [...list].sort(compareDistance)
  } else if (sortMode.value === 'name') {
    // 이름순 — title 로캘 비교(가나다). 한글·영문·숫자 자연스러운 정렬.
    list = [...list].sort(comparePlaceName)
  } else {
    list = [...list].sort(compareRecommendation)
  }
  return list
})

// 지도 중심 — 위치 권한 허용 시 현재 위치, 아니면 서울
const mapCenter = computed(() =>
  userLoc.value ? [userLoc.value.lat, userLoc.value.lng] : [37.5665, 126.978],
)

// 지도 드래그/줌 → 현 지도에서 검색 버튼
const mapDragCenter = ref(null)
const showMapSearchBtn = ref(false)
const mapFit = ref(true) // 현 지도에서 검색 시 false → 카메라 고정
// 지도가 너무 넓어(내접원 > 20km) 조회 원이 화면보다 작아진 상태 — 안내 노출용
const areaCapped = ref(false)

// 카카오맵 level(클수록 광역) → 검색 반경(m)
const ZOOM_RADIUS = { 1:500, 2:1000, 3:2000, 4:3000, 5:5000, 6:8000, 7:12000, 8:16000, 9:20000, 10:35000, 11:60000, 12:100000, 13:200000, 14:400000 }

function onMapMove(center) {
  mapDragCenter.value = center
  showMapSearchBtn.value = true
}

// 현재 보고 있는 지도 영역 기준 조회 파라미터 — 중심 + '크기'(반경 m).
// 반경은 보이는 사각형의 '내접원'(중심~가장 가까운 변까지)으로 잡아, 조회 원이 항상 화면 영역
// 안쪽에 들어오게 한다(이전엔 중심~모서리=반대각선이라 화면보다 크게 조회됐음).
// bounds 가 없으면(구형 SDK) 줌레벨로 반경을 근사한다.
function mapAreaParams(extra = {}) {
  const c = mapDragCenter.value
  let radius
  if (c.neLat != null && c.neLng != null) {
    // 중심~북변(N-S 반높이), 중심~동변(E-W 반너비) 중 더 작은 값 = 내접원 반경.
    const vertM = distanceKm({ lat: c.lat, lng: c.lng }, { lat: c.neLat, lng: c.lng }) * 1000
    const horizM = distanceKm({ lat: c.lat, lng: c.lng }, { lat: c.lat, lng: c.neLng }) * 1000
    const inscribed = Math.round(Math.min(vertM, horizM))
    radius = Math.min(Math.max(inscribed, 500), 20000) // TourAPI radius 상한 20km
    // 화면 내접원이 상한(20km)을 넘으면 조회 원이 화면보다 작아져 '화면=조회영역'이 깨진다.
    // → 사용자에게 "확대하면 정확해진다"고 안내하기 위한 플래그.
    areaCapped.value = inscribed > 20000
  } else {
    radius = ZOOM_RADIUS[c.level ?? 9] ?? 20000
  }
  return {
    page: 1,
    size: PAGE_SIZE,
    mapX: String(c.lng),
    mapY: String(c.lat),
    radius,
    ...extra,
  }
}

function searchByMapCenter() {
  if (!mapDragCenter.value) return
  showMapSearchBtn.value = false
  mapFit.value = false
  const cat = CATEGORIES.find((c) => c.key === selectedCategory.value)
  const ct = cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}
  store.list(mapAreaParams(ct), currentUi(), { forceLoading: true })
}

// 지도 마커는 항상 시트와 동일한 집합을 표시 — 클릭(목록 row)과 지도 핀 불일치 방지.
// (거리순일 때도 8개로 자르지 않고 전체를 핀으로 노출)
const mapPlaces = computed(() => displayedPlaces.value)

// 현재 활성 정렬 기준의 라벨 (정렬 버튼에 표시)
const sortLabel = computed(
  () => SORT_OPTIONS.find((o) => o.key === sortMode.value)?.label ?? '추천순',
)

const CATEGORY_LABEL = { all: '플레이스', '12': '관광지', '15': '축제', '39': '음식점', '32': '숙박' }

const sheetTitle = computed(() => {
  if (activeTag.value) return `${TAG_LABELS[activeTag.value]} 인기순`
  const label = CATEGORY_LABEL[selectedCategory.value] ?? '플레이스'
  return sortMode.value === 'distance' ? `내 주변 ${label}` : label
})

// ── 태그 큐레이션 조회/모드 전환 ───────────────────────────────────────────────
// 큐레이트 응답(content) → 목록/지도 행 형태로 매핑(+likeCount).
function mapCurated(c) {
  return {
    contentId: c.contentId,
    contentTypeId: c.contentType,
    name: c.title,
    address: c.addr,
    lat: c.latitude,
    lng: c.longitude,
    imageUrl: c.imageUrl,
    likeCount: c.likeCount ?? 0,
  }
}
// 늦게 도착한 큐레이트 응답이 최신 태그 결과를 덮어쓰지 못하도록 요청 토큰을 둔다.
let curatedReqId = 0
async function fetchCurated(tag) {
  if (!tag) return
  const myId = ++curatedReqId
  curatedLoading.value = true
  store.loading = true // 시트 스켈레톤 재사용
  try {
    const { data } = await attractionApi.curated({ tag, sort: 'like', page: 0, size: 50 })
    if (myId !== curatedReqId) return // 더 최신 요청이 진행 중 — 무시
    const content = Array.isArray(data) ? data : (data?.content ?? [])
    curatedItems.value = content.map(mapCurated)
  } catch {
    if (myId === curatedReqId) curatedItems.value = []
  } finally {
    if (myId === curatedReqId) {
      curatedLoading.value = false
      store.loading = false
    }
  }
}
// 태그모드 진입 — 큐레이트 조회 + URL(?tag) 동기화. route query를 단일 진실로 유지.
function setTag(tag) {
  if (!TAG_LABELS[tag]) return
  activeTag.value = tag
  sortMode.value = 'default' // 태그모드 기본은 좋아요(인기)순 — 이전 거리/이름순 잔재 제거
  showMapSearchBtn.value = false
  mapFit.value = true
  if (searchQuery.value.trim() && !searchQuery.value.trim().startsWith('#')) searchQuery.value = ''
  if (route.query.tag !== tag) router.replace({ path: '/explore', query: { tag } })
  fetchCurated(tag)
}
// 태그모드 해제 — URL의 ?tag 제거 + 일반 탐색 복귀.
function clearTag() {
  if (!activeTag.value) return
  activeTag.value = null
  curatedItems.value = []
  curatedReqId++ // 진행 중 큐레이트 응답 무효화
  searchQuery.value = ''
  if (route.query.tag) router.replace({ path: '/explore', query: {} })
  loadAttractions()
}

// '#' 입력 시 태그 제안 목록(입력 텍스트로 필터).
const tagSuggestions = computed(() => {
  const q = searchQuery.value.trim()
  if (!q.startsWith('#')) return []
  const term = q.slice(1).trim().toLowerCase()
  return Object.entries(TAG_LABELS)
    .filter(([key, label]) => !term || label.includes(term) || key.includes(term))
    .map(([key, label]) => ({ key, label }))
})
function applyTagSuggest(key) {
  searchQuery.value = ''
  setTag(key)
}

// 정렬 바텀시트 토글
function openSortSheet() {
  sortSheetOpen.value = true
}
function closeSortSheet() {
  sortSheetOpen.value = false
}

// 정렬 기준 선택 — 거리순은 위치가 필요하므로 없으면 권한 요청 후 적용.
function selectSort(key) {
  const opt = SORT_OPTIONS.find((o) => o.key === key)
  if (!opt) return
  if (opt.requiresLoc && !userLoc.value) {
    // 위치 미확보 — 권한 확보를 시도하고, 성공하면 거리순으로 전환(locateUser 내부 처리).
    closeSortSheet()
    locateUser(true)
    return
  }
  sortMode.value = key
  closeSortSheet()
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

// 현재 위치 확보 — 공유 location store 사용(앱 시작 시 프리페치된 좌표를 즉시 재사용).
// 성공 시 거리순 + 근처 목록 조회(캐시 적중 시 네트워크 없이 즉시 표시).
// 실패/미지원 시 전체 목록 폴백.
function locateUser(forceSort = false) {
  locationStore.ensureLocation().then((coords) => {
    if (coords) {
      userLoc.value = { lat: coords.lat, lng: coords.lng }
      if (forceSort || sortMode.value === 'default') sortMode.value = 'distance'
      // 위치 확보 → BE 좌표 검색으로 근처 목록 조회 (검색 중이 아닐 때).
      // 프리페치 캐시가 있으면 store.list 가 즉시 캐시를 표시한다.
      if (!searchQuery.value.trim()) loadAttractions()
    } else {
      // 거부/실패/미지원 → 위치 없이 전체 목록(아직 안 불러왔으면)
      if (!store.attractions.length) loadAttractions()
    }
  })
}

// 마지막 탐색 보존용 — 현재 화면의 검색어/필터/정렬 상태 스냅샷
function currentUi() {
  return {
    searchQuery: searchQuery.value,
    selectedCategory: selectedCategory.value,
    sortMode: sortMode.value,
    userLoc: userLoc.value,
  }
}

// ── Actions ───────────────────────────────────────────────────────────────────
function selectCategory(key) {
  if (activeTag.value) { // 카테고리 선택 시 태그(인기순) 모드 해제 + URL ?tag 제거
    activeTag.value = null
    curatedItems.value = []
    curatedReqId++
    if (route.query.tag) router.replace({ path: '/explore', query: {} })
  }
  showMapSearchBtn.value = false
  areaCapped.value = false // 분류 조회는 지도범위 기반이 아니므로 안내 해제
  selectedCategory.value = key
  const cat = CATEGORIES.find((c) => c.key === key)
  const ct = cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}

  // 사용자가 정한 지도 영역이 우선 — 분류를 바꿔도 카메라를 결과에 맞춰 재조정하지 않고
  // 현재 보고 있는 지도 영역(크기) 안에서만 다시 조회한다.
  if (mapDragCenter.value) {
    mapFit.value = false // 결과에 맞춰 줌/이동하지 않음(지도 크기 유지)
    store.list(mapAreaParams(ct), currentUi(), { forceLoading: true })
    return
  }

  // 아직 지도를 움직인 적이 없으면(영역 미확정) 위치 기준 + 결과에 맞춰 카메라 fit.
  mapFit.value = true
  const params = { page: 1, size: PAGE_SIZE, ...locParams(), ...ct }
  // 카테고리 전환은 결과가 반드시 바뀌어야 하므로 즉시 로딩 표시(forceLoading)
  store.list(params, currentUi(), { forceLoading: true })
}

// contentId -> 목록 row DOM 매핑(선택 시 스크롤 인투 뷰용)
const rowRefs = new Map()
function setRowRef(contentId, el) {
  if (el) rowRefs.set(String(contentId), el)
  else rowRefs.delete(String(contentId))
}

// 지도 마커 클릭 → 선택 상태 반영 + 하단 시트에서 해당 항목으로 스크롤/강조.
// (지도 패닝/마커 강조는 TripMap 이 selectedId watch 로 처리)
function selectPlace(place) {
  selectedPlace.value = place
  // 시트를 펼쳐 항목이 보이도록 한 뒤, 해당 row 를 스크롤로 가시화
  expandSheet()
  nextTick(() => {
    const el = rowRefs.get(String(place.contentId))
    if (el && typeof el.scrollIntoView === 'function') {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  })
}

// 상세 페이지로 이동(인포윈도우 '상세보기' 탭 / 목록 row 탭 공통).
function goDetail(place) {
  if (place && place.contentId != null) {
    router.push(`/place/${place.contentId}`)
  }
}

// 목록 row 탭 — 기존 동작(상세 네비) 보존.
function onRowClick(place) {
  goDetail(place)
}

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const q = searchQuery.value.trim()
    // '#맛집'/'#food' 형태 → 태그(인기순) 모드 진입
    if (q.startsWith('#')) {
      const key = q.slice(1).trim()
      const tag = TAG_LABELS[key] ? key : KEYWORD_TO_TAG[key]
      if (tag) { setTag(tag); return }
    }
    // 태그모드 중 일반 입력은 큐레이트 목록 내 필터(displayedPlaces)로만 — 전국 검색 호출 안 함.
    if (activeTag.value) return
    if (q.length >= 2) {
      const cat = CATEGORIES.find((c) => c.key === selectedCategory.value)
      const params = {
        keyword: q,
        page: 1,
        size: PAGE_SIZE,
        ...(cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}),
      }
      // 검색어 변경도 결과 전환이므로 즉시 로딩 표시 + page 리셋
      store.list(params, currentUi(), { forceLoading: true })
    } else {
      // Query cleared below threshold — reload the unfiltered list for current category
      loadAttractions()
    }
  }, 600)
}

function clearSearch() {
  searchQuery.value = ''
  showMapSearchBtn.value = false
  loadAttractions() // 현 지도 영역 기준으로 재조회(카메라 유지 여부는 loadAttractions가 결정)
}

function loadAttractions() {
  const cat = CATEGORIES.find((c) => c.key === selectedCategory.value)
  const ct = cat?.contentTypeId ? { contentTypeId: cat.contentTypeId } : {}

  // 현재 보고 있는 지도 영역이 있으면 그 '크기'(반경) 기준으로 조회하고 카메라를 고정한다.
  // (확대했는데도 고정 20km로 검색되던 문제 해결 — 검색은 항상 현 지도 영역 기준)
  if (mapDragCenter.value) {
    mapFit.value = false
    store.list(mapAreaParams(ct), currentUi())
    return
  }

  // 지도 영역 미확정(최초 진입) — 위치 기준 + 결과에 맞춰 카메라 fit.
  mapFit.value = true
  const params = { page: 1, size: PAGE_SIZE, ...locParams(), ...ct }
  store.list(params, currentUi())
}

function formatDist(km) {
  if (!Number.isFinite(km)) return ''
  return km < 1 ? `${Math.round(km * 1000)}m` : `${km.toFixed(1)}km`
}

// 썸네일 — 실제 이미지가 없으면 로컬 기본 썸네일로 채움(외부 더미 미사용)
const THUMB_PLACEHOLDER = '/images/placeholder-thumb.png'
function placePlaceholder() {
  return THUMB_PLACEHOLDER
}
function thumbSrc(place) {
  return place.imageUrl || THUMB_PLACEHOLDER
}
function onThumbError(e) {
  if (e.target.src !== location.origin + THUMB_PLACEHOLDER) e.target.src = THUMB_PLACEHOLDER
}
function festPlaceholder() {
  return THUMB_PLACEHOLDER
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

// 마지막 탐색(검색어/지역/카테고리 필터 + 결과)을 즉시 복원한 뒤 백그라운드 갱신.
// 사용자가 명시적으로 한 탐색이 우선이며, 네트워크 없이 바로 보여준다.
function restoreFromLastExplore(snap) {
  // UI 필터 상태 복원
  const ui = snap.ui ?? {}
  if (typeof ui.searchQuery === 'string') searchQuery.value = ui.searchQuery
  if (ui.selectedCategory) selectedCategory.value = ui.selectedCategory
  if (ui.userLoc && Number.isFinite(ui.userLoc.lat) && Number.isFinite(ui.userLoc.lng)) {
    userLoc.value = { lat: ui.userLoc.lat, lng: ui.userLoc.lng }
  }
  if (ui.sortMode) sortMode.value = ui.sortMode
  // 결과 즉시 표시 (store 상태에 raw 반영, 스피너 없음)
  store.restoreLastExplore()
  // 백그라운드 SWR 갱신 — 같은 params·ui 로 다시 list (캐시/네트워크 최신화)
  store.list(snap.params ?? {}, currentUi())
}

// ── 무한스크롤 ─────────────────────────────────────────────────────────────────
// 바닥 센티넬이 스크롤 컨테이너(bottom-sheet) 안에서 보이면 다음 페이지를 누적 로드.
// 페이지 누적 직후 센티넬이 여전히 보이면 IntersectionObserver 가 다시 발화하므로
// 짧은 목록에서도 자연스럽게 이어진다(중복은 store.loadMore 가 contentId로 제거).
function onSentinelIntersect(entries) {
  if (!entries.some((e) => e.isIntersecting)) return
  if (!store.hasMore || store.loading || store.loadingMore) return
  store.loadMore()
}

function setupObserver() {
  if (typeof IntersectionObserver === 'undefined') return
  teardownObserver()
  if (!loadMoreSentinel.value) return
  io = new IntersectionObserver(onSentinelIntersect, {
    root: bottomSheet.value ?? null,   // 시트 내부 스크롤 기준(없으면 뷰포트)
    rootMargin: '120px',               // 바닥 도달 전 미리 로드
    threshold: 0,
  })
  io.observe(loadMoreSentinel.value)
}

function teardownObserver() {
  if (io) {
    io.disconnect()
    io = null
  }
}

// 센티넬은 v-if 로 붙었다 떨어지므로(검색어 입력/마지막 페이지) DOM 변동마다 재관찰.
watch(
  () => loadMoreSentinel.value,
  async () => {
    await nextTick()
    if (loadMoreSentinel.value) setupObserver()
    else teardownObserver()
  },
)

onBeforeUnmount(() => {
  teardownObserver()
  // 드래그 도중 이탈 시 window 리스너 누수 방지
  window.removeEventListener('pointermove', onSheetPointerMove)
  window.removeEventListener('pointerup', onSheetPointerUp)
  window.removeEventListener('pointercancel', onSheetPointerUp)
})

// 홈 칩/딥링크의 ?tag 변화 → 태그(인기순) 모드 진입/해제
watch(
  () => route.query.tag,
  (tag) => {
    if (tag && TAG_LABELS[tag]) {
      if (tag !== activeTag.value) setTag(tag)
    } else if (!tag && activeTag.value) {
      clearTag()
    }
  },
)

// ── Bootstrap ─────────────────────────────────────────────────────────────────
onMounted(() => {
  festivalStore.loadFestivals()              // festival section (non-blocking)
  store.loadAreas()                          // area list for potential future use

  // (a) 홈 칩/딥링크로 태그가 지정돼 들어오면 큐레이션(좋아요 인기순)부터 표시.
  if (route.query.tag && TAG_LABELS[route.query.tag]) {
    setTag(route.query.tag)
    return
  }

  // (b) 마지막 탐색이 있으면 그대로 복원 — 뒤로가기→재진입 시 초기화하지 않음.
  const snap = store.lastExplore
  if (snap && Array.isArray(snap.results) && snap.results.length) {
    restoreFromLastExplore(snap)
    return
  }

  // (c) 없으면 기존 동작 — 위치 우선(내 주변 → 거부/실패 시 전체/제주 폴백).
  store.loading = true
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
  padding: 52px 16px 0;
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

/* '#' 태그 제안 */
.tag-suggest {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 4px 2px;
}
.tag-suggest-item {
  padding: 7px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
  background: var(--color-peach-light);
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
  color: white;
}

/* ── Map ──────────────────────────────────────────────────────────────────── */
.map-container {
  flex: 1;
  min-height: 0;
  position: relative;
}
.map-search-btn {
  position: absolute;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  background: #fff;
  border: 1.5px solid var(--color-peach, #f78f57);
  border-radius: 20px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach-pressed, #e0743a);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.18);
  cursor: pointer;
  white-space: nowrap;
}

/* ── Bottom sheet ─────────────────────────────────────────────────────────── */
.bottom-sheet {
  background: var(--color-white);
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.08);
  height: 48vh;              /* JS가 인라인 style로 덮어씀(드래그) */
  overflow-y: auto;
  transition: height 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}
/* 드래그 중엔 트랜지션을 꺼서 손가락을 즉시 따라오게 */
.bottom-sheet.dragging { transition: none; }

/* 핸들 — 히트영역 넓힌 래퍼로 감싸 드래그 쉽게 */
.sheet-handle-wrap {
  display: flex;
  justify-content: center;
  padding: 8px 0 6px;
  cursor: grab;
  touch-action: none;       /* 핸들에서 시작한 터치는 스크롤이 아닌 드래그로 */
}
.sheet-handle-wrap:active { cursor: grabbing; }
.sheet-handle {
  width: 44px;
  height: 5px;
  background: var(--color-line);
  border-radius: 3px;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}

.area-cap-hint {
  margin: -4px 20px 8px;
  padding: 8px 12px;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: -0.2px;
  border-radius: var(--radius-md);
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

/* ── 정렬 바텀시트 ────────────────────────────────────────────────────────── */
.sort-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(0, 0, 0, 0.32);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.sort-sheet {
  width: 100%;
  max-width: 480px;
  background: var(--color-white);
  border-radius: 20px 20px 0 0;
  padding: 0 16px calc(20px + env(safe-area-inset-bottom));
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.16);
}

.sort-sheet-handle {
  width: 36px;
  height: 4px;
  background: var(--color-line);
  border-radius: 2px;
  margin: 10px auto 6px;
}

.sort-sheet-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink-muted);
  letter-spacing: -0.3px;
  padding: 8px 4px 6px;
}

.sort-opt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  border-top: 1px solid var(--color-line-light);
  cursor: pointer;
  text-align: left;
}

.sort-opt.active {
  color: var(--color-peach);
}

.sort-opt.disabled {
  color: var(--color-ink-muted);
}

.sort-opt-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-opt-hint {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 2px 8px;
}

/* 오버레이 페이드 트랜지션 */
.sort-fade-enter-active,
.sort-fade-leave-active {
  transition: opacity 0.18s ease;
}
.sort-fade-enter-from,
.sort-fade-leave-to {
  opacity: 0;
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

/* 지도 마커로 선택된 항목 강조 */
.place-row.selected {
  background: var(--color-peach-light);
  box-shadow: inset 3px 0 0 var(--color-peach);
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

/* 좋아요(하트) 수 배지 — 태그 인기순 모드 */
.like-badge {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach);
  flex-shrink: 0;
}

/* 태그 모드 표시/해제 칩 */
.tag-pill {
  background: var(--color-peach) !important;
  border-color: var(--color-peach) !important;
  color: #fff !important;
}

.place-addr {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

/* ── 무한스크롤 (센티넬 / more 로딩 / 끝) ─────────────────────────────────── */
.load-more-sentinel {
  height: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px 0 4px;
}

.load-more-spinner {
  display: flex;
  gap: 5px;
  align-items: center;
}

.spinner-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-peach);
  opacity: 0.4;
  animation: dot-pulse 1s infinite ease-in-out;
}

.spinner-dot:nth-child(2) { animation-delay: 0.15s; }
.spinner-dot:nth-child(3) { animation-delay: 0.3s; }

@keyframes dot-pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.85); }
  50%      { opacity: 1;   transform: scale(1); }
}

.list-end {
  text-align: center;
  font-size: 11.5px;
  color: var(--color-ink-muted);
  padding: 14px 0 6px;
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
