/**
 * 게임화(챌린지/뱃지) 스토어 — GET /api/gamification/summary 연동.
 * BE가 기존 계획/장소 수에서 진행도를 파생하므로 별도 이벤트 추적 없음.
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { gamificationApi } from '@/api/index.js'

export const useGamificationStore = defineStore('gamification', () => {
  const summary = ref(null)
  const loading = ref(false)
  const loaded = ref(false)

  async function load(force = false) {
    if (loaded.value && !force) return
    loading.value = true
    try {
      const { data } = await gamificationApi.summary()
      summary.value = data
      loaded.value = true
    } catch {
      // 미로그인/오류 — summary는 null 유지(화면은 폴백 처리)
    } finally {
      loading.value = false
    }
  }

  // 화면 진입 시 캐시 무시하고 최신화. load(true) 와 동치(가독성용 별칭).
  function refresh() {
    return load(true)
  }

  return { summary, loading, loaded, load, refresh }
})
