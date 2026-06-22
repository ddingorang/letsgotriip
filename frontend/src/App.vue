<template>
  <div class="app-frame">
    <main class="app-main" :class="{ 'no-tab': !showTabBar }">
      <RouterView />
    </main>
    <CartFab />
    <BottomNav v-if="showTabBar" />
    <AppAlert />
    <AppConfirm />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'
import CartFab from '@/components/common/CartFab.vue'
import AppAlert from '@/components/common/AppAlert.vue'
import AppConfirm from '@/components/common/AppConfirm.vue'
import { useLocationStore } from '@/stores/location.js'
import { useAttractionStore } from '@/stores/attraction.js'
import { contextApi } from '@/api/index.js'

const route = useRoute()
const showTabBar = computed(() => route.meta.tabBar !== false)

const locationStore = useLocationStore()
const attractionStore = useAttractionStore()

// 날씨 프리페치 캐시 키 — 좌표를 소수 2자리로 라운딩(근처면 동일 캐시 재사용)
const WEATHER_CACHE_PREFIX = 'triip.weather.'
function prefetchWeather(lat, lng) {
  const key = `${WEATHER_CACHE_PREFIX}${lat.toFixed(2)},${lng.toFixed(2)}`
  try {
    if (sessionStorage.getItem(key)) return // 이미 프리페치됨
  } catch {
    // sessionStorage 사용 불가 — 그래도 호출은 시도하지 않음(캐시 없으면 의미 없음)
    return
  }
  contextApi
    .weather(lat, lng)
    .then(({ data }) => {
      try {
        sessionStorage.setItem(key, JSON.stringify({ data, savedAt: Date.now() }))
      } catch {
        /* 저장 실패 무시 */
      }
    })
    .catch(() => {
      /* 날씨 프리페치 실패는 조용히 무시 */
    })
}

// 앱 시작 시 위치 확보 → 좌표 있으면 근처 관광지/날씨를 백그라운드 프리페치.
// 앱 로드를 막지 않도록 비동기로 띄우고, 거부/실패는 조용히 무시(폴백 유지).
onMounted(() => {
  locationStore
    .ensureLocation()
    .then((coords) => {
      if (!coords) return // 거부/타임아웃/미지원 — 각 뷰의 기존 폴백 사용
      const { lat, lng } = coords
      // 뷰가 실제로 요청하는 파라미터와 동일하게 프리페치해야 캐시가 적중한다.
      // ExploreView: 기본 카테고리 근처 목록 (size 30, radius 20km)
      attractionStore.prefetch({ mapX: lng, mapY: lat, radius: 20000, size: 30 })
      // HomeView: 관광지(12) 근처 10개
      attractionStore.prefetch({ mapX: lng, mapY: lat, radius: 20000, contentTypeId: 12, size: 10 })
      // 날씨
      prefetchWeather(lat, lng)
    })
    .catch(() => {
      /* ensureLocation 은 reject 하지 않지만 방어적으로 무시 */
    })
})
</script>

<style>
.app-frame {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-main.no-tab {
  /* Full-screen flows: tab bar hidden, main gets all vertical space */
  flex: 1;
}
</style>
