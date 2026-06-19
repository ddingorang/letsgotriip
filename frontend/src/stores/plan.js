import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/http.js'

export const usePlanStore = defineStore('plan', () => {
  const plans = ref([])
  const current = ref(null)
  const routeReport = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // ── helpers ──────────────────────────────────────────────────────────────────

  function handleError(e) {
    const code = e.response?.data?.code
    const msg = e.response?.data?.message ?? e.message ?? '오류가 발생했습니다.'
    return { code, msg }
  }

  // ── actions ──────────────────────────────────────────────────────────────────

  async function loadPlans() {
    loading.value = true
    error.value = null
    try {
      // Backend returns Spring Page({ content: [...] })
      const { data } = await http.get('/api/plans', { params: { page: 0, size: 20 } })
      plans.value = Array.isArray(data) ? data : (data?.content ?? [])
    } catch (e) {
      const { msg } = handleError(e)
      error.value = msg
      plans.value = []
    } finally {
      loading.value = false
    }
  }

  async function loadPlan(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await http.get(`/api/plans/${id}`)
      current.value = data
    } catch (e) {
      const { msg } = handleError(e)
      error.value = msg
    } finally {
      loading.value = false
    }
  }

  /** 동선 리포트 로드 — 거리·소요시간·추천순서 (GET /api/plans/{id}/route-report) */
  async function loadRouteReport(id) {
    try {
      const { data } = await http.get(`/api/plans/${id}/route-report`)
      routeReport.value = data
      return data
    } catch (e) {
      routeReport.value = null
      throw e
    }
  }

  async function createPlan(payload) {
    loading.value = true
    error.value = null
    try {
      const { data } = await http.post('/api/plans', payload)
      await loadPlans()
      await loadPlan(data.id)
      return data
    } catch (e) {
      const { msg } = handleError(e)
      error.value = msg
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updatePlan(payload) {
    loading.value = true
    error.value = null
    try {
      const { id, ...body } = payload
      // BE PlanUpdateRequestDto requires expectedVersion (optimistic locking)
      if (body.expectedVersion == null) body.expectedVersion = current.value?.version
      await http.patch(`/api/plans/${id}`, body)
      await loadPlan(id)
    } catch (e) {
      const { code, msg } = handleError(e)
      if (code === 'PLAN4092') {
        await loadPlan(payload.id).catch(() => {})
        error.value = '다른 곳에서 수정됐어요. 최신 상태로 새로고침했어요.'
      } else {
        error.value = msg
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deletePlan(id) {
    loading.value = true
    error.value = null
    try {
      await http.delete(`/api/plans/${id}`)
      if (current.value?.id === id) current.value = null
      await loadPlans()
    } catch (e) {
      const { msg } = handleError(e)
      error.value = msg
      throw e
    } finally {
      loading.value = false
    }
  }

  async function addPlace(planId, dayNo, payload) {
    loading.value = true
    error.value = null
    try {
      await http.post(`/api/plans/${planId}/days/${dayNo}/places`, payload)
      await loadPlan(planId)
    } catch (e) {
      const { code, msg } = handleError(e)
      if (code === 'PLAN4093') {
        error.value = '이미 계획에 있어요.'
      } else if (code === 'PLAN4092') {
        await loadPlan(planId).catch(() => {})
        error.value = '다른 곳에서 수정됐어요. 최신 상태로 새로고침했어요.'
      } else {
        error.value = msg
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  async function replacePlaces(planId, dayNo, places) {
    loading.value = true
    error.value = null
    try {
      const body = {
        expectedVersion: current.value?.version,
        places: places.map((p, idx) => ({
          contentId: p.attraction.contentId,
          contentType: p.attraction.contentType,
          seq: idx + 1,
          visitTime: p.visitTime ?? null,
          memo: p.memo ?? null,
        })),
      }
      await http.put(`/api/plans/${planId}/days/${dayNo}/places`, body)
      await loadPlan(planId)
    } catch (e) {
      const { code, msg } = handleError(e)
      if (code === 'PLAN4092') {
        await loadPlan(planId).catch(() => {})
        error.value = '다른 곳에서 수정됐어요. 최신 상태로 새로고침했어요.'
      } else {
        error.value = msg
      }
      throw e
    } finally {
      loading.value = false
    }
  }

  async function removePlace(planId, dayNo, placeId) {
    loading.value = true
    error.value = null
    try {
      await http.delete(`/api/plans/${planId}/days/${dayNo}/places/${placeId}`)
      await loadPlan(planId)
    } catch (e) {
      const { msg } = handleError(e)
      error.value = msg
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    plans,
    current,
    routeReport,
    loading,
    error,
    loadPlans,
    loadPlan,
    loadRouteReport,
    createPlan,
    updatePlan,
    deletePlan,
    addPlace,
    replacePlaces,
    removePlace,
  }
})
