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

// ── 마지막 탐색 상태 보존 (sessionStorage) ────────────────────────────────────
// 사용자가 마지막으로 한 탐색(파라미터 + 결과 + 검색어/필터 UI값)을 저장해
// Explore 재진입 시 초기화 없이 그대로 복원한다. 위치 프리페치와 별개로,
// "사용자가 명시적으로 한 탐색"이 있을 때 우선 복원하기 위한 용도.
const LAST_EXPLORE_KEY = 'triip.attraction.lastExplore'

function readLastExplore() {
  try {
    const raw = sessionStorage.getItem(LAST_EXPLORE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || !Array.isArray(parsed.results)) return null
    return parsed
  } catch {
    return null
  }
}

function writeLastExplore(snapshot) {
  try {
    sessionStorage.setItem(LAST_EXPLORE_KEY, JSON.stringify(snapshot))
  } catch {
    // 용량 초과/사용 불가 — 보존 없이 정상 동작
  }
}

// ── Store ─────────────────────────────────────────────────────────────────────
export const useAttractionStore = defineStore('attraction', () => {
  const attractions = ref([])        // mapped list for ExploreView
  const searchResults = ref([])      // raw list (kept for backwards compat)
  const currentAttraction = ref(null)
  const loading = ref(false)
  const loadingMore = ref(false)     // 무한스크롤 다음 페이지 로딩 중
  const hasMore = ref(true)          // 더 불러올 페이지가 남았는지(빈 결과 시 false)
  const error = ref(null)
  const searchParams = ref({})
  const areas = ref([])

  // 동시/연속 list() 호출에서 늦게 도착한 응답이 최신 결과를 덮어쓰지 못하도록
  // 단조 증가 토큰을 둔다. list()/loadMore() 진입 시 토큰을 올리고, 비동기 응답
  // 반영 직전 토큰이 그대로면(= 더 새 요청이 없으면) 적용한다.
  let requestSeq = 0

  // 마지막 탐색 스냅샷 (params + raw results + UI 필터값 + 갱신시각).
  // sessionStorage 와 동기화되어 재진입 시 복원에 쓰인다.
  const lastExplore = ref(readLastExplore())

  /**
   * 현재 탐색 상태를 마지막 탐색으로 저장한다.
   * @param {object} params  list() 에 넘긴 조회 파라미터
   * @param {Array}  raw     조회 결과 raw 배열
   * @param {object} ui      검색어/필터 등 화면 상태 ({ searchQuery, selectedCategory, sortMode } 등)
   */
  function saveLastExplore(params, raw, ui = {}) {
    const snapshot = {
      params: params ?? {},
      results: Array.isArray(raw) ? raw : [],
      ui,
      updatedAt: Date.now(),
    }
    lastExplore.value = snapshot
    writeLastExplore(snapshot)
  }

  /**
   * 마지막 탐색 스냅샷을 반환한다(없으면 null). 결과를 즉시 화면에 반영해
   * 네트워크 없이 복원하기 위한 용도. 호출 측에서 UI 필터값도 함께 복원한다.
   */
  function restoreLastExplore() {
    const snap = lastExplore.value ?? readLastExplore()
    if (!snap || !Array.isArray(snap.results)) return null
    // 결과를 즉시 상태에 반영 (스피너 없이 바로 표시)
    applyRaw(snap.results, snap.params ?? {})
    loading.value = false
    // 재진입 시 무한스크롤을 다시 사용할 수 있도록 페이지 상태 리셋
    hasMore.value = snap.results.length > 0
    return snap
  }

  // 주어진 raw 배열을 상태에 반영.
  // append=true 면 기존 목록 뒤에 누적하되 contentId 기준 중복을 제거한다.
  function applyRaw(raw, params, append = false) {
    if (append) {
      const seen = new Set(searchResults.value.map((it) => String(it.contentId ?? it.id ?? '')))
      const fresh = raw.filter((it) => {
        const id = String(it.contentId ?? it.id ?? '')
        if (!id || seen.has(id)) return false
        seen.add(id)
        return true
      })
      const merged = searchResults.value.concat(fresh)
      searchResults.value = merged
      attractions.value = merged.map((item, i) => mapAttraction(item, i + 1))
    } else {
      searchResults.value = raw
      attractions.value = raw.map((item, i) => mapAttraction(item, i + 1))
    }
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
  async function list(params = {}, ui = undefined, opts = {}) {
    // forceLoading: 카테고리/검색어 전환처럼 "결과가 바뀌어야 하는" 호출에서
    // 기존 목록이 남아 있어도 스켈레톤을 띄워 즉시 전환을 알린다.
    const { forceLoading = false } = opts
    error.value = null
    searchParams.value = params
    // 새 1페이지 조회 — 페이지네이션 상태 리셋(다음 페이지 다시 탐색 가능)
    hasMore.value = true

    // 이번 호출의 토큰 — 늦게 도착한 이전 요청이 최신 결과를 덮어쓰는 것을 막는다
    const seq = ++requestSeq
    const isCurrent = () => seq === requestSeq

    const cached = readListCache(params)
    if (cached) {
      // 캐시 즉시 반영 — 로딩 스피너 없이 바로 표시
      applyRaw(cached, params)
      loading.value = false
      hasMore.value = cached.length > 0
      // 사용자가 명시적으로 탐색한 경우(ui 전달) 즉시 마지막 탐색으로 보존
      if (ui) saveLastExplore(params, cached, ui)
      // 백그라운드 갱신 (조용히, 실패해도 캐시 유지)
      fetchAndStore(params)
        .then((raw) => {
          if (raw && isCurrent()) {
            applyRaw(raw, params)
            hasMore.value = raw.length > 0
            if (ui) saveLastExplore(params, raw, ui)
          }
        })
        .catch(() => {})
      return
    }

    // 이미 표시 중인 결과가 있으면(예: 마지막 탐색 복원 후 백그라운드 갱신)
    // 스켈레톤으로 덮지 않고 조용히 갱신 — "네트워크 없이 즉시 복원" 보장.
    // 단, 카테고리/검색어 전환(forceLoading)이면 즉시 스켈레톤을 띄운다.
    if (forceLoading || !attractions.value.length) loading.value = true
    try {
      const raw = await fetchAndStore(params)
      // 더 새 요청이 시작됐으면 이 응답은 버린다(stale overwrite 방지)
      if (!isCurrent()) return
      applyRaw(raw, params)
      hasMore.value = raw.length > 0
      if (ui) saveLastExplore(params, raw, ui)
    } catch (e) {
      if (!isCurrent()) return
      error.value = e.response?.data?.message ?? e.message ?? '검색 중 오류가 발생했습니다.'
      searchResults.value = MOCK_ATTRACTIONS
      attractions.value = MOCK_ATTRACTIONS.map((item, i) => mapAttraction(item, i + 1))
      hasMore.value = false
    } finally {
      if (isCurrent()) loading.value = false
    }
  }

  /**
   * 무한스크롤 다음 페이지 로드 — 현재 searchParams 기준 page+1 을 조회해
   * 기존 목록 뒤에 누적(append)한다. contentId 기준 중복은 제거한다.
   *
   * 빈 결과(마지막 페이지)면 hasMore=false 로 더 이상 호출하지 않는다.
   * 기존 캐시/lastExplore 동작은 그대로 보존한다(append 결과는 캐시에 기록하지 않음).
   * @returns {Promise<boolean>} 새 항목을 추가했으면 true
   */
  async function loadMore() {
    // 더 없거나 이미 로딩 중/초기 로딩 중이면 무시
    if (!hasMore.value || loadingMore.value || loading.value) return false
    const base = searchParams.value ?? {}
    const currentPage = Number(base.page) > 0 ? Number(base.page) : 1
    const nextParams = { ...base, page: currentPage + 1 }

    const seq = ++requestSeq
    const isCurrent = () => seq === requestSeq

    loadingMore.value = true
    try {
      const { data } = await attractionApi.list(nextParams)
      const raw = Array.isArray(data) ? data : (data?.content ?? data?.items ?? [])
      if (!isCurrent()) return false
      if (!raw.length) {
        hasMore.value = false
        return false
      }
      const before = searchResults.value.length
      applyRaw(raw, nextParams, true)   // append + dedup
      const added = searchResults.value.length - before
      // 모두 중복이었으면(서버가 같은 페이지 반복) 더 진행하지 않음
      if (added === 0) hasMore.value = false
      return added > 0
    } catch {
      // 다음 페이지 실패는 조용히 무시 — 현재 목록 유지, 재시도 가능하도록 page 유지
      return false
    } finally {
      if (isCurrent()) loadingMore.value = false
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
    loadingMore,
    hasMore,
    error,
    searchParams,
    areas,
    lastExplore,
    // actions
    list,
    loadMore,
    prefetch,
    search,
    fetchDetail,
    loadAreas,
    saveLastExplore,
    restoreLastExplore,
  }
})
