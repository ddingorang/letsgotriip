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

// ── Store ─────────────────────────────────────────────────────────────────────
export const useAttractionStore = defineStore('attraction', () => {
  const attractions = ref([])        // mapped list for ExploreView
  const searchResults = ref([])      // raw list (kept for backwards compat)
  const currentAttraction = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const searchParams = ref({})
  const areas = ref([])

  /**
   * Load a filtered/searched list of attractions.
   * params: { areaCode, sigunguCode, contentTypeId, keyword, page, size }
   * Falls back to MOCK_ATTRACTIONS on network failure.
   */
  async function list(params = {}) {
    loading.value = true
    error.value = null
    searchParams.value = params
    try {
      const { data } = await attractionApi.list(params)
      const raw = Array.isArray(data) ? data : (data?.content ?? data?.items ?? [])
      searchResults.value = raw
      attractions.value = raw.map((item, i) => mapAttraction(item, i + 1))
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '검색 중 오류가 발생했습니다.'
      searchResults.value = MOCK_ATTRACTIONS
      attractions.value = MOCK_ATTRACTIONS.map((item, i) => mapAttraction(item, i + 1))
    } finally {
      loading.value = false
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
    search,
    fetchDetail,
    loadAreas,
  }
})
