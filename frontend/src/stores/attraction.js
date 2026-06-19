import { defineStore } from 'pinia'
import { ref } from 'vue'
import { attractionApi } from '@/api/index.js'

// ── Mock seed (shown when BE is unreachable) ──────────────────────────────────
export const MOCK_ATTRACTIONS = [
  {
    contentId: '126508',
    title: '성산일출봉',
    addr1: '제주특별자치도 서귀포시 성산읍 성산리',
    firstimage: '',
    contentTypeId: 12,
    mapy: 33.4581,
    mapx: 126.9425,
    tel: '',
  },
  {
    contentId: '806048',
    title: '협재해변',
    addr1: '제주특별자치도 제주시 한림읍 협재리',
    firstimage: '',
    contentTypeId: 12,
    mapy: 33.3942,
    mapx: 126.2394,
    tel: '',
  },
  {
    contentId: '732777',
    title: '우도',
    addr1: '제주특별자치도 제주시 우도면',
    firstimage: '',
    contentTypeId: 12,
    mapy: 33.5,
    mapx: 126.951,
    tel: '',
  },
  {
    contentId: '125440',
    title: '감천문화마을',
    addr1: '부산광역시 사하구 감내2로 203',
    firstimage: '',
    contentTypeId: 14,
    mapy: 35.0976,
    mapx: 129.0109,
    tel: '',
  },
  {
    contentId: '264570',
    title: '불국사',
    addr1: '경상북도 경주시 불국로 385',
    firstimage: '',
    contentTypeId: 14,
    mapy: 35.7896,
    mapx: 129.3315,
    tel: '',
  },
  {
    contentId: '2734597',
    title: '제주 애월 카페거리',
    addr1: '제주특별자치도 제주시 애월읍',
    firstimage: '',
    contentTypeId: 39,
    mapy: 33.4607,
    mapx: 126.3227,
    tel: '',
  },
]

// ── contentTypeId → 한국어 category label ────────────────────────────────────
export const CONTENT_TYPE_MAP = {
  12: '관광지',
  14: '문화시설',
  15: '축제행사',
  25: '여행코스',
  28: '레포츠',
  32: '숙박',
  38: '쇼핑',
  39: '음식점',
}

/**
 * Map a raw TourAPI / BE attraction item → shape PlaceCard + detail view expects.
 *   place.name, place.address, place.imageUrl, place.rating,
 *   place.reviewCount, place.tags, place.contentId, place.lat, place.lng,
 *   place.tel, place.category, place.overview
 */
export function mapAttraction(item, rank = null) {
  return {
    // identity
    id: String(item.contentId ?? item.id ?? ''),
    contentId: String(item.contentId ?? item.id ?? ''),
    // display
    name: String(item.title ?? item.name ?? '').trim(),
    address: [item.addr1, item.addr2].filter(Boolean).join(' ').trim() || String(item.address ?? ''),
    imageUrl: item.firstimage || item.firstimage2 || item.imageUrl || '',
    // rating / review — TourAPI has no ratings; use null so UI can hide gracefully
    rating: item.rating ?? null,
    reviewCount: item.reviewCount ?? null,
    // tags from contentTypeId
    tags: item.tags ?? (item.contentTypeId ? [CONTENT_TYPE_MAP[item.contentTypeId] ?? '관광'].filter(Boolean) : []),
    // geo
    lat: Number(item.mapy ?? item.lat) || null,
    lng: Number(item.mapx ?? item.lng) || null,
    // extra detail fields
    tel: item.tel ?? '',
    overview: item.overview ?? item.overview2 ?? '',
    category: CONTENT_TYPE_MAP[item.contentTypeId] ?? item.category ?? '관광지',
    contentTypeId: item.contentTypeId ?? null,
    // rank badge
    rank,
  }
}

// ── 경량 리스트 캐시 (sessionStorage + TTL) ───────────────────────────────────
// 동일 파라미터 조회를 세션 내 재사용해 첫 탐색을 즉시 표시한다.
const LIST_CACHE_PREFIX = 'triip.attraction.list.'
const LIST_CACHE_TTL = 10 * 60 * 1000 // 10분

// 파라미터 객체 → 안정적인 캐시 키 (키 정렬로 순서 무관)
function cacheKey(params = {}) {
  const norm = {}
  Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null && params[k] !== '')
    .sort()
    .forEach((k) => {
      norm[k] = String(params[k])
    })
  return LIST_CACHE_PREFIX + JSON.stringify(norm)
}

function readListCache(params) {
  try {
    const raw = sessionStorage.getItem(cacheKey(params))
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || !Array.isArray(parsed.raw)) return null
    if (!parsed.savedAt || Date.now() - parsed.savedAt > LIST_CACHE_TTL) return null
    return parsed.raw
  } catch {
    return null
  }
}

function writeListCache(params, raw) {
  try {
    sessionStorage.setItem(
      cacheKey(params),
      JSON.stringify({ raw, savedAt: Date.now() }),
    )
  } catch {
    // 용량 초과/사용 불가 — 캐시 없이 정상 동작
  }
}

// ── Store ─────────────────────────────────────────────────────────────────────
export const useAttractionStore = defineStore('attraction', () => {
  const attractions = ref([])        // mapped list for ExploreView
  const searchResults = ref([])      // raw list (kept for backwards compat)
  const currentAttraction = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const searchParams = ref({})
  const areas = ref([])

  // 주어진 raw 배열을 상태에 반영
  function applyRaw(raw, params) {
    searchResults.value = raw
    attractions.value = raw.map((item, i) => mapAttraction(item, i + 1))
    searchParams.value = params
  }

  // 네트워크에서 받아 상태/캐시에 저장. 성공 시 raw 반환, 실패 시 null.
  async function fetchAndStore(params) {
    const { data } = await attractionApi.list(params)
    const raw = Array.isArray(data) ? data : (data?.content ?? data?.items ?? [])
    writeListCache(params, raw)
    return raw
  }

  /**
   * Load a filtered/searched list of attractions.
   * params: { areaCode, sigunguCode, contentTypeId, keyword, page, size }
   *
   * 캐시 우선(stale-while-revalidate): 동일 파라미터 캐시가 있으면 즉시 표시한 뒤
   * 백그라운드로 최신화한다. 캐시가 없으면 기존처럼 로딩 후 조회한다.
   * Falls back to MOCK_ATTRACTIONS on network failure.
   */
  async function list(params = {}) {
    error.value = null
    searchParams.value = params

    const cached = readListCache(params)
    if (cached) {
      // 캐시 즉시 반영 — 로딩 스피너 없이 바로 표시
      applyRaw(cached, params)
      loading.value = false
      // 백그라운드 갱신 (조용히, 실패해도 캐시 유지)
      fetchAndStore(params)
        .then((raw) => {
          if (raw) applyRaw(raw, params)
        })
        .catch(() => {})
      return
    }

    loading.value = true
    try {
      const raw = await fetchAndStore(params)
      applyRaw(raw, params)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '검색 중 오류가 발생했습니다.'
      searchResults.value = MOCK_ATTRACTIONS
      attractions.value = MOCK_ATTRACTIONS.map((item, i) => mapAttraction(item, i + 1))
    } finally {
      loading.value = false
    }
  }

  /**
   * 백그라운드 프리페치 — 현재 화면 상태(attractions/loading)를 건드리지 않고
   * 결과를 캐시에만 저장한다. 앱 시작 시 호출되어 첫 탐색을 즉시 표시하기 위함.
   * 실패는 조용히 무시한다.
   */
  async function prefetch(params = {}) {
    if (readListCache(params)) return // 이미 신선한 캐시가 있으면 생략
    try {
      await fetchAndStore(params)
    } catch {
      // 프리페치 실패는 무시 — 실제 진입 시 정상 경로로 재시도
    }
  }

  /**
   * Alias kept for components that call store.search() (e.g. legacy/springaitrip compat).
   */
  async function search(params = {}) {
    return list(params)
  }

  /**
   * Fetch single attraction detail by contentId.
   * Stores result in currentAttraction. Falls back to mock on error.
   */
  async function fetchDetail(contentId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await attractionApi.detail(contentId)
      currentAttraction.value = mapAttraction(data)
      return currentAttraction.value
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '정보를 불러올 수 없습니다.'
      // fallback: find in mock seeds or build a stub
      const seed = MOCK_ATTRACTIONS.find((m) => String(m.contentId) === String(contentId))
      currentAttraction.value = seed ? mapAttraction(seed) : null
      return currentAttraction.value
    } finally {
      loading.value = false
    }
  }

  /** Load area code list from /api/attractions/areas */
  async function loadAreas() {
    try {
      const { data } = await attractionApi.areas()
      areas.value = Array.isArray(data) ? data : []
    } catch {
      areas.value = []
    }
  }

  return {
    // state
    attractions,
    searchResults,
    currentAttraction,
    loading,
    error,
    searchParams,
    areas,
    // actions
    list,
    prefetch,
    search,
    fetchDetail,
    loadAreas,
  }
})
