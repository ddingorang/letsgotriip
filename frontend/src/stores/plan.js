import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/http.js'

// 장바구니(활성 계획) 로컬 저장 키
const LS_ACTIVE_ID = 'triip.activePlanId'
const LS_ACTIVE_TITLE = 'triip.activePlanTitle'
const LS_CART_COUNT = 'triip.cartCount'

function lsGet(k) {
  try { return localStorage.getItem(k) } catch { return null }
}
function lsSet(k, v) {
  try { if (v == null) localStorage.removeItem(k); else localStorage.setItem(k, String(v)) } catch { /* noop */ }
}

export const usePlanStore = defineStore('plan', () => {
  const plans = ref([])
  const current = ref(null)
  const routeReport = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // ── 장바구니(활성 계획) 상태 — 마지막에 담은/선택한 "내 여행" ───────────────────
  // 한 번 탭으로 담을 수 있도록, 어느 계획에 담을지를 기억한다(localStorage 영속).
  const activePlanId = ref(Number(lsGet(LS_ACTIVE_ID)) || null)
  const activePlanTitle = ref(lsGet(LS_ACTIVE_TITLE) || '')
  const cartCount = ref(Number(lsGet(LS_CART_COUNT)) || 0)

  // ── helpers ──────────────────────────────────────────────────────────────────

  function handleError(e) {
    const code = e.response?.data?.code
    const msg = e.response?.data?.message ?? e.message ?? '오류가 발생했습니다.'
    return { code, msg }
  }

  function setActivePlan(id, title) {
    activePlanId.value = id ?? null
    if (title != null) activePlanTitle.value = title
    lsSet(LS_ACTIVE_ID, id ?? null)
    if (title != null) lsSet(LS_ACTIVE_TITLE, title || null)
  }

  function setCartCount(n) {
    cartCount.value = n ?? 0
    lsSet(LS_CART_COUNT, cartCount.value)
  }

  function countPlaces(plan) {
    return (plan?.days ?? []).reduce((sum, d) => sum + (d.places?.length ?? 0), 0)
  }

  // ── actions ──────────────────────────────────────────────────────────────────

  async function loadPlans(params = {}) {
    loading.value = true
    error.value = null
    try {
      // Backend returns Spring Page({ content: [...] })
      const { data } = await http.get('/api/plans', { params: { page: 0, size: 50, ...params } })
      plans.value = Array.isArray(data) ? data : (data?.content ?? [])
      // 캐시된 활성 계획이 실제 목록에 없으면 localStorage 초기화
      if (activePlanId.value && !plans.value.find((p) => p.id === activePlanId.value)) {
        setActivePlan(null, '')
        setCartCount(0)
      }
      if (plans.value.length === 0) {
        setActivePlan(null, '')
        setCartCount(0)
      }
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

  /**
   * 장소를 다른 일차로 이동 — 단일 move 엔드포인트가 없어 (대상에 추가 → 원본에서 제거) 2단계.
   * 대상에 이미 있으면(PLAN4093) 추가는 건너뛰고 원본만 제거(중복 정리 효과).
   * 두 호출 모두 expectedVersion이 필요 없는 add/delete라 버전 충돌 없이 동작한다.
   */
  async function movePlaceToDay(planId, fromDay, toDay, place) {
    error.value = null
    const contentId = String(place.attraction?.contentId ?? place.contentId ?? '')
    const contentType = place.attraction?.contentType ?? place.contentType
    loading.value = true
    try {
      try {
        await http.post(`/api/plans/${planId}/days/${toDay}/places`, { contentId, contentType })
      } catch (e) {
        if (e.response?.data?.code !== 'PLAN4093') throw e
        // 대상 일차에 이미 있음 → 추가 생략, 원본만 제거
      }
      await http.delete(`/api/plans/${planId}/days/${fromDay}/places/${place.id}`)
      await loadPlan(planId)
    } catch (e) {
      await loadPlan(planId).catch(() => {})
      const { msg } = handleError(e)
      error.value = msg
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 한 장소의 방문 시간 설정/해제 — 같은 일차 places를 그대로 PUT(낙관적).
   * visitTime: 'HH:mm' 문자열 또는 null(해제). reorder와 동일한 본문 규격.
   */
  async function setPlaceVisitTime(planId, dayNo, placeId, visitTime) {
    if (!current.value) return
    const dayObj = (current.value.days ?? []).find((d) => d.dayNo === dayNo)
    if (!dayObj) return
    const newPlaces = (dayObj.places ?? []).map((p) =>
      p.id === placeId ? { ...p, visitTime } : p,
    )
    const snapshot = current.value
    current.value = {
      ...current.value,
      days: (current.value.days ?? []).map((d) =>
        d.dayNo === dayNo ? { ...d, places: newPlaces } : d,
      ),
    }
    error.value = null
    try {
      const body = {
        expectedVersion: snapshot.version,
        places: newPlaces.map((p, idx) => ({
          contentId: p.attraction?.contentId ?? p.contentId,
          contentType: p.attraction?.contentType ?? p.contentType,
          seq: idx + 1,
          visitTime: p.visitTime ?? null,
          memo: p.memo ?? null,
        })),
      }
      await http.put(`/api/plans/${planId}/days/${dayNo}/places`, body)
      if (current.value) current.value = { ...current.value, version: (current.value.version ?? 0) + 1 }
    } catch (e) {
      await loadPlan(planId).catch(() => {})
      const { msg } = handleError(e)
      error.value = msg
      throw e
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

  /**
   * 같은 일차 내 순서 변경 — 낙관적 업데이트.
   * 화면(current)을 즉시 새 순서로 바꾸고, 저장은 백그라운드로 한다(전역 loading 토글 X →
   * 목록이 "불러오는 중…"으로 사라지지 않음). 실패 시에만 서버 상태로 되돌린다.
   * 서버가 version을 force-increment 하므로 성공 시 로컬 version을 +1 해 다음 저장 충돌을 막는다.
   */
  async function reorderDayPlaces(planId, dayNo, newPlaces) {
    if (!current.value) return
    const snapshot = current.value
    // 1) 낙관적 반영 — 즉시 새 순서로
    current.value = {
      ...current.value,
      days: (current.value.days ?? []).map((d) =>
        d.dayNo === dayNo ? { ...d, places: newPlaces } : d,
      ),
    }
    error.value = null
    // 2) 백그라운드 저장 (loading 토글 없이)
    try {
      const body = {
        expectedVersion: snapshot.version,
        places: newPlaces.map((p, idx) => ({
          contentId: p.attraction?.contentId ?? p.contentId,
          contentType: p.attraction?.contentType ?? p.contentType,
          seq: idx + 1,
          visitTime: p.visitTime ?? null,
          memo: p.memo ?? null,
        })),
      }
      await http.put(`/api/plans/${planId}/days/${dayNo}/places`, body)
      // 성공: 서버 force-increment에 맞춰 로컬 version 동기화(재조회 없이)
      if (current.value) {
        current.value = { ...current.value, version: (current.value.version ?? 0) + 1 }
      }
    } catch (e) {
      // 실패: 서버 최신 상태로 되돌린다
      await loadPlan(planId).catch(() => {})
      const { msg } = handleError(e)
      error.value = msg ?? '순서 저장에 실패했어요.'
      throw e
    }
  }

  /**
   * 장바구니에 담을 "활성 계획"을 보장한다.
   * 1) 저장된 활성 계획이 아직 존재하면 그대로 사용
   * 2) 없으면 가장 최근(=id 큰) 계획을 활성으로
   * 3) 계획이 하나도 없으면 기본 "나의 여행"(오늘~+2일)을 자동 생성
   * @returns {Promise<number>} 활성 계획 id
   */
  async function ensureActivePlan() {
    if (!plans.value.length) await loadPlans()
    // 1) 기존 활성 계획 유효성 확인
    const found = activePlanId.value && plans.value.find((p) => p.id === activePlanId.value)
    if (found) {
      setActivePlan(found.id, found.title)
      return found.id
    }
    // 2) 가장 최근 계획
    if (plans.value.length) {
      const recent = [...plans.value].sort((a, b) => (b.id ?? 0) - (a.id ?? 0))[0]
      setActivePlan(recent.id, recent.title)
      return recent.id
    }
    // 3) 기본 계획 자동 생성
    const today = new Date()
    const end = new Date(); end.setDate(today.getDate() + 2)
    const pad = (n) => String(n).padStart(2, '0')
    const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    const created = await createPlan({ title: '나의 여행', startDate: fmt(today), endDate: fmt(end) })
    setActivePlan(created.id, '나의 여행')
    return created.id
  }

  /**
   * 장바구니 담기 — 활성 계획의 첫 일차에 한 번에 담는다(피커 없이).
   * 계획이 없으면 기본 계획을 만들어 담는다. 전역 loading 토글은 최소화한다.
   * @returns {Promise<{planId:number, planTitle:string, dayNo:number, duplicate:boolean}>}
   */
  async function quickAddPlace(payload) {
    const planId = await ensureActivePlan()
    if (current.value?.id !== planId) await loadPlan(planId)
    const dayNo = current.value?.days?.[0]?.dayNo ?? 1
    const title = current.value?.title ?? activePlanTitle.value ?? '내 여행'
    try {
      await http.post(`/api/plans/${planId}/days/${dayNo}/places`, payload)
      await loadPlan(planId)
      setActivePlan(planId, current.value?.title ?? title)
      setCartCount(countPlaces(current.value))
      return { planId, planTitle: current.value?.title ?? title, dayNo, duplicate: false }
    } catch (e) {
      const code = e.response?.data?.code
      if (code === 'PLAN4093') {
        // 이미 담긴 장소 — 카운트만 동기화하고 중복으로 보고
        return { planId, planTitle: title, dayNo, duplicate: true }
      }
      if (code === 'PLAN4092') await loadPlan(planId).catch(() => {})
      const { msg } = handleError(e)
      error.value = msg
      throw e
    }
  }

  return {
    plans,
    current,
    routeReport,
    loading,
    error,
    activePlanId,
    activePlanTitle,
    cartCount,
    setActivePlan,
    setCartCount,
    ensureActivePlan,
    quickAddPlace,
    loadPlans,
    loadPlan,
    loadRouteReport,
    createPlan,
    updatePlan,
    deletePlan,
    addPlace,
    replacePlaces,
    reorderDayPlaces,
    movePlaceToDay,
    setPlaceVisitTime,
    removePlace,
  }
})
