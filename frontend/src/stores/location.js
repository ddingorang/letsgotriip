/**
 * Location store — 앱 시작 시 내 위치를 한 번 확보해 세션 내 공유한다.
 *
 * - coords: { lat, lng } | null
 * - status: 'idle' | 'granted' | 'denied'
 * - ensureLocation(): navigator.geolocation 으로 1회 조회.
 *   성공하면 sessionStorage 에 캐시하고 다음 조회부터는 즉시 복원한다.
 *   거부/타임아웃/미지원이면 status='denied' 로 두고 좌표는 null 을 유지한다.
 *
 * 앱 로드를 막지 않도록 모든 동작은 비동기이며, 실패는 조용히 무시한다.
 * (위치가 없으면 각 뷰가 기존 폴백 — 제주 인기 등 — 으로 동작한다.)
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'triip.location'
const CACHE_TTL = 30 * 60 * 1000 // 30분 — 위치는 잘 안 바뀌므로 넉넉히

function readCache() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed.lat !== 'number' || typeof parsed.lng !== 'number') return null
    if (!parsed.savedAt || Date.now() - parsed.savedAt > CACHE_TTL) return null
    return { lat: parsed.lat, lng: parsed.lng }
  } catch {
    return null
  }
}

function writeCache(coords) {
  try {
    sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ lat: coords.lat, lng: coords.lng, savedAt: Date.now() }),
    )
  } catch {
    // sessionStorage 사용 불가(프라이빗 모드 등) — 메모리 상태만 사용
  }
}

export const useLocationStore = defineStore('location', () => {
  const coords = ref(null) // { lat, lng } | null
  const status = ref('idle') // 'idle' | 'granted' | 'denied'

  // 동시 호출이 여러 getCurrentPosition 을 띄우지 않도록 in-flight Promise 를 공유
  let pending = null

  /**
   * 위치를 1회 확보한다. 항상 Promise<coords|null> 를 resolve 하며 reject 하지 않는다
   * (호출부가 await 후 좌표 유무만 보고 분기할 수 있게).
   */
  function ensureLocation() {
    // 이미 좌표가 있으면 즉시 반환
    if (coords.value) return Promise.resolve(coords.value)

    // 세션 캐시 복원
    const cached = readCache()
    if (cached) {
      coords.value = cached
      status.value = 'granted'
      return Promise.resolve(cached)
    }

    // 미지원 → 조용히 denied
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      status.value = 'denied'
      return Promise.resolve(null)
    }

    // 진행 중인 요청이 있으면 그 결과를 공유
    if (pending) return pending

    pending = new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const next = { lat: pos.coords.latitude, lng: pos.coords.longitude }
          coords.value = next
          status.value = 'granted'
          writeCache(next)
          pending = null
          resolve(next)
        },
        () => {
          // 거부/타임아웃/실패 — 폴백 유지
          status.value = 'denied'
          pending = null
          resolve(null)
        },
        { enableHighAccuracy: false, timeout: 7000, maximumAge: 300000 },
      )
    })
    return pending
  }

  return {
    coords,
    status,
    ensureLocation,
  }
})
