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
  // 로드 실패 여부. summary가 null인 두 경우(① 아직 미로딩/진짜 빈 데이터, ② API 실패)를
  // 화면이 구분할 수 있게 노출한다. 실패를 '레벨1·0개'의 가짜 성공으로 위장하지 않기 위함.
  const error = ref(null)

  async function load(force = false) {
    if (loaded.value && !force) return
    loading.value = true
    error.value = null
    try {
      const { data } = await gamificationApi.summary()
      summary.value = data
      loaded.value = true
    } catch (e) {
      // 미로그인은 정상 빈 상태로 둔다(401/403). 그 외 오류는 실패로 노출해
      // 화면이 가짜 레벨1을 그리지 않고 에러/재시도를 표시할 수 있게 한다.
      const status = e?.response?.status
      if (status === 401 || status === 403) {
        summary.value = null
      } else {
        error.value =
          e?.response?.data?.message ?? e?.message ?? '진행 정보를 불러오지 못했어요.'
      }
    } finally {
      loading.value = false
    }
  }

  // 화면 진입 시 캐시 무시하고 최신화. load(true) 와 동치(가독성용 별칭).
  function refresh() {
    return load(true)
  }

  return { summary, loading, loaded, error, load, refresh }
})
