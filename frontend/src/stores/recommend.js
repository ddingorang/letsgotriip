import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/http.js'

export const useRecommendStore = defineStore('recommend', () => {
  const current = ref(null)
  const history = ref([])
  const generating = ref(false)
  const error = ref(null)

  // 생성 요청 취소용 — handleGenerate 중 사용자가 취소하면 abort()
  let abortController = null

  // ── helpers ──────────────────────────────────────────────────────────────────

  function errorMessage(e) {
    const code = e.response?.data?.code
    if (code === 'RECO409') return '이미 생성 중이에요. 잠시 후 다시 시도해 주세요.'
    if (code === 'RECO422' || code === 'RECO502') return '추천 생성에 실패했어요. 다시 시도해 주세요.'
    return e.response?.data?.message ?? e.message ?? '오류가 발생했어요.'
  }

  // ── actions ──────────────────────────────────────────────────────────────────

  /**
   * POST /api/recommendations — synchronous, up to ~35 s
   * payload: { areaCode, startDate, endDate, companions?, budget?, themes? }
   */
  async function generate(payload) {
    generating.value = true
    error.value = null
    abortController = new AbortController()
    try {
      const { data } = await http.post('/api/recommendations', payload, {
        timeout: 40_000,
        signal: abortController.signal,
      })
      current.value = data
      return data
    } catch (e) {
      // 사용자가 취소한 경우 — 에러 메시지 없이 조용히 중단
      if (e.code === 'ERR_CANCELED' || e.name === 'CanceledError') {
        error.value = null
        const canceled = new Error('canceled')
        canceled.canceled = true
        throw canceled
      }
      error.value = errorMessage(e)
      throw e
    } finally {
      generating.value = false
      abortController = null
    }
  }

  /** 진행 중인 생성 요청을 취소 */
  function cancelGenerate() {
    abortController?.abort()
  }

  /** GET /api/recommendations?page=0&size=10 — Spring Page wrapper */
  async function loadHistory() {
    error.value = null
    try {
      const { data } = await http.get('/api/recommendations', { params: { page: 0, size: 10 } })
      history.value = Array.isArray(data) ? data : (data?.content ?? [])
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '오류가 발생했어요.'
      history.value = []
    }
  }

  /** GET /api/recommendations/{id} */
  async function load(id) {
    error.value = null
    try {
      const { data } = await http.get(`/api/recommendations/${id}`)
      current.value = data
      return data
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '오류가 발생했어요.'
      throw e
    }
  }

  /**
   * POST /api/recommendations/{id}/save-plan — idempotent
   * Returns PlanDetail (existing or new)
   */
  async function savePlan(id) {
    error.value = null
    try {
      const { data } = await http.post(`/api/recommendations/${id}/save-plan`)
      return data
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '저장 중 오류가 발생했어요.'
      throw e
    }
  }

  return {
    current,
    history,
    generating,
    error,
    generate,
    cancelGenerate,
    loadHistory,
    load,
    savePlan,
  }
})
