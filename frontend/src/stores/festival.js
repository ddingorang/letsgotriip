/**
 * Festival store — uses the direct TourAPI proxy path (src/api/festival.js →
 * /api/tour/…) rather than the BE endpoint (/api/festivals).
 *
 * Rationale: fetchFestivals() in @/api/festival.js already handles the full
 * TourAPI response normalisation (date filtering, field mapping, error parsing).
 * The BE /api/festivals endpoint requires a running Spring backend and proxies
 * the same TourAPI call; when the BE is down the UI would break. The direct
 * path degrades gracefully with the empty-array fallback below and keeps the
 * Vite dev-proxy as the only dependency (/api/tour → TourAPI).
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchFestivals } from '@/api/festival.js'

// ── Mock seed (shown when TourAPI is unreachable) ─────────────────────────────
export const MOCK_FESTIVALS = [
  {
    id: 'f-1',
    title: '제주 봄꽃 축제',
    address: '제주특별자치도 제주시',
    image: '',
    startDate: '20260601',
    endDate: '20260630',
    latitude: 33.499,
    longitude: 126.531,
    homepage: '',
  },
  {
    id: 'f-2',
    title: '부산 국제 영화제',
    address: '부산광역시 해운대구',
    image: '',
    startDate: '20261001',
    endDate: '20261010',
    latitude: 35.158,
    longitude: 129.161,
    homepage: '',
  },
]

export const useFestivalStore = defineStore('festival', () => {
  const festivals = ref([])
  const loading = ref(false)
  const error = ref('')
  const loadedArea = ref(null)

  async function loadFestivals(areaCode = '') {
    if (loading.value) return
    loading.value = true
    error.value = ''
    try {
      const result = await fetchFestivals({ areaCode })
      festivals.value = result.length ? result : MOCK_FESTIVALS
      loadedArea.value = areaCode
    } catch (e) {
      error.value = e instanceof Error ? e.message : '행사 정보를 불러오지 못했습니다.'
      // fallback to mock seeds so UI never shows empty
      festivals.value = MOCK_FESTIVALS
    } finally {
      loading.value = false
    }
  }

  return { festivals, loading, error, loadedArea, loadFestivals }
})
