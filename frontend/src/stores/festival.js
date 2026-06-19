/**
 * Festival store — Spring 백엔드(GET /api/festivals)를 단일 데이터 소스로 사용한다.
 * TourAPI 직접 호출/하드코딩 키/mock 폴백은 모두 제거됐다.
 * 실제 데이터는 BE 의 festival sync 배치 실행 후 채워진다.
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchFestivals } from '@/api/festival.js'

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
      festivals.value = await fetchFestivals({ areaCode })
      loadedArea.value = areaCode
    } catch (e) {
      error.value = e instanceof Error ? e.message : '행사 정보를 불러오지 못했습니다.'
      festivals.value = []
    } finally {
      loading.value = false
    }
  }

  return { festivals, loading, error, loadedArea, loadFestivals }
})
