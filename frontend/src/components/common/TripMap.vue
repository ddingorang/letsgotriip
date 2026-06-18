<template>
  <div ref="mapEl" class="trip-map">
    <div v-if="error" class="map-error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'

const props = defineProps({
  // [{ id|contentId, name, lat, lng }]
  places: { type: Array, default: () => [] },
  selectedId: { type: [String, Number], default: null },
  center: { type: Array, default: () => [37.5665, 126.978] }, // 서울시청
  level: { type: Number, default: 9 }, // Kakao zoom level (작을수록 확대)
  numbered: { type: Boolean, default: true },
})

const emit = defineEmits(['select'])

const mapEl = ref(null)
const error = ref('')
let map = null
let overlays = []

const KAKAO_KEY = import.meta.env.VITE_KAKAO_MAP_KEY

// SDK를 한 번만 로드 (전역 공유 프라미스)
function loadKakao() {
  if (window.kakao && window.kakao.maps) return Promise.resolve(window.kakao)
  if (window.__kakaoMapLoading) return window.__kakaoMapLoading
  window.__kakaoMapLoading = new Promise((resolve, reject) => {
    if (!KAKAO_KEY) {
      reject(new Error('VITE_KAKAO_MAP_KEY 누락'))
      return
    }
    const s = document.createElement('script')
    s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_KEY}&autoload=false&libraries=services`
    s.onload = () => window.kakao.maps.load(() => resolve(window.kakao))
    s.onerror = () => reject(new Error('Kakao 지도 SDK 로드 실패(도메인 등록 확인)'))
    document.head.appendChild(s)
  })
  return window.__kakaoMapLoading
}

function keyOf(p) {
  return String(p.id ?? p.contentId ?? '')
}

function validPoints() {
  return props.places.filter(
    (p) => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng)),
  )
}

function clearOverlays() {
  overlays.forEach((o) => o.setMap(null))
  overlays = []
}

function renderMarkers(kakao) {
  if (!map) return
  clearOverlays()
  const pts = validPoints()
  const bounds = new kakao.maps.LatLngBounds()

  pts.forEach((p, idx) => {
    const pos = new kakao.maps.LatLng(Number(p.lat), Number(p.lng))
    const selected = props.selectedId != null && keyOf(p) === String(props.selectedId)
    const label = props.numbered ? idx + 1 : '•'
    const el = document.createElement('div')
    el.className = 'trip-pin' + (selected ? ' selected' : '')
    el.innerHTML = `<span>${label}</span>`
    el.title = p.name || ''
    el.addEventListener('click', () => emit('select', p))

    const overlay = new kakao.maps.CustomOverlay({
      position: pos,
      content: el,
      yAnchor: 1,
      clickable: true,
    })
    overlay.setMap(map)
    overlays.push(overlay)
    bounds.extend(pos)
  })

  if (pts.length === 1) {
    map.setCenter(new kakao.maps.LatLng(Number(pts[0].lat), Number(pts[0].lng)))
    map.setLevel(4)
  } else if (pts.length > 1) {
    map.setBounds(bounds, 40, 40, 40, 40)
  }
}

onMounted(async () => {
  await nextTick()
  try {
    const kakao = await loadKakao()
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(props.center[0], props.center[1]),
      level: props.level,
    })
    renderMarkers(kakao)
    setTimeout(() => map && map.relayout(), 200)

    watch(
      () => props.places,
      () => renderMarkers(kakao),
      { deep: true },
    )
    watch(
      () => props.selectedId,
      () => renderMarkers(kakao),
    )
  } catch (e) {
    error.value = e.message || '지도를 불러올 수 없습니다.'
    // eslint-disable-next-line no-console
    console.error('[TripMap]', e)
  }
})

onBeforeUnmount(() => {
  clearOverlays()
  map = null
})
</script>

<style scoped>
.trip-map {
  width: 100%;
  height: 100%;
  position: relative;
  background: #e8f0e8;
}
.map-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 16px;
  font-size: 13px;
  color: var(--color-ink-muted, #777);
  z-index: 1;
}
</style>

<style>
/* 비-scoped: CustomOverlay content에 적용 */
.trip-pin {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  background: var(--color-primary, #ff7043);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  cursor: pointer;
}
.trip-pin > * {
  transform: rotate(45deg);
}
.trip-pin.selected {
  background: #2e7d32;
  width: 34px;
  height: 34px;
  font-size: 14px;
}
</style>
