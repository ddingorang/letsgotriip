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
  categoryColors: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['select', 'detail', 'move'])

const mapEl = ref(null)
const error = ref('')
let map = null
let overlays = []
let infoOverlay = null      // 선택된 핀 위의 상세보기 말풍선(인포윈도우)
let resizeObserver = null
let kakaoRef = null
// 마운트 직후/장소목록 변경 시에만 전체 bounds 로 맞춤.
// 선택(selectedId) 변경 시에는 카메라를 재설정하지 않고 panTo 만 수행한다.
let needsFit = true

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

function clearInfo() {
  if (infoOverlay) {
    infoOverlay.setMap(null)
    infoOverlay = null
  }
}

function clearOverlays() {
  clearInfo()
  overlays.forEach((o) => o.setMap(null))
  overlays = []
}

// 선택된 핀 위에 '상세보기' 말풍선(인포윈도우)을 띄운다.
// 탭하면 'detail' 이벤트로 상세 페이지 네비를 부모에 위임.
function showInfo(kakao, p, pos) {
  clearInfo()
  const box = document.createElement('div')
  box.className = 'trip-info'
  const nameEl = document.createElement('span')
  nameEl.className = 'trip-info-name'
  nameEl.textContent = p.name || '상세보기'
  const goEl = document.createElement('span')
  goEl.className = 'trip-info-go'
  goEl.textContent = '상세보기 ›'
  box.appendChild(nameEl)
  box.appendChild(goEl)
  box.addEventListener('click', (e) => {
    e.stopPropagation()
    emit('detail', p)
  })
  infoOverlay = new kakao.maps.CustomOverlay({
    position: pos,
    content: box,
    yAnchor: 2.05, // 핀 위쪽으로 띄움
    clickable: true,
    zIndex: 100,
  })
  infoOverlay.setMap(map)
}

function renderMarkers(kakao) {
  if (!map) return
  clearOverlays()
  const pts = validPoints()
  const bounds = new kakao.maps.LatLngBounds()
  let selectedPos = null
  let selectedPt = null

  pts.forEach((p, idx) => {
    const pos = new kakao.maps.LatLng(Number(p.lat), Number(p.lng))
    const selected = props.selectedId != null && keyOf(p) === String(props.selectedId)
    const label = props.numbered ? idx + 1 : '•'
    const color = props.categoryColors[String(p.contentTypeId)] ?? '#F78F57'
    const el = document.createElement('div')
    el.className = 'trip-pin' + (selected ? ' selected' : '')
    el.style.setProperty('--pin-color', color)
    el.innerHTML = `<span>${label}</span>`
    el.title = p.name || ''
    el.addEventListener('click', () => emit('select', p))

    const overlay = new kakao.maps.CustomOverlay({
      position: pos,
      content: el,
      yAnchor: 1,
      clickable: true,
      zIndex: selected ? 50 : 1,
    })
    overlay.setMap(map)
    overlays.push(overlay)
    bounds.extend(pos)

    if (selected) {
      selectedPos = pos
      selectedPt = p
    }
  })

  // 선택된 핀이 있으면 상세보기 말풍선 표시
  if (selectedPos && selectedPt) showInfo(kakao, selectedPt, selectedPos)

  // 카메라 맞춤은 마운트/장소목록 변경 시에만(needsFit). 선택 변경만으로는 재설정하지 않는다.
  if (needsFit) {
    if (pts.length === 1) {
      map.setCenter(new kakao.maps.LatLng(Number(pts[0].lat), Number(pts[0].lng)))
      map.setLevel(props.level)
    } else if (pts.length > 1) {
      map.setBounds(bounds, 40, 40, 40, 40)
    }
    needsFit = false
  }
}

// 선택된 장소로 부드럽게 이동(panTo) + 적절한 줌으로 확대.
function panToSelected(kakao) {
  if (!map || props.selectedId == null) return
  const p = validPoints().find((q) => keyOf(q) === String(props.selectedId))
  if (!p) return
  const pos = new kakao.maps.LatLng(Number(p.lat), Number(p.lng))
  // 너무 멀리 줌아웃돼 있으면 살짝 당겨준다(레벨이 작을수록 확대).
  if (map.getLevel() > 6) map.setLevel(5)
  map.panTo(pos)
}

// 컨테이너 크기가 0이거나 늦게 잡히면 지도가 회색으로 남으므로,
// relayout + 마커 재배치를 수행해 항상 정상 표시되도록 한다.
function refresh() {
  if (!map || !kakaoRef) return
  map.relayout()
  renderMarkers(kakaoRef)
}

onMounted(async () => {
  await nextTick()
  try {
    const kakao = await loadKakao()
    kakaoRef = kakao
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(props.center[0], props.center[1]),
      level: props.level,
    })
    renderMarkers(kakao)

    kakao.maps.event.addListener(map, 'dragend', () => {
      const c = map.getCenter()
      emit('move', { lat: c.getLat(), lng: c.getLng() })
    })

    // 탭 전환/폰트 로드 등으로 컨테이너 크기가 늦게 잡히는 경우 대비 — 재시도
    setTimeout(refresh, 60)
    setTimeout(refresh, 250)
    setTimeout(refresh, 600)

    // 컨테이너 크기가 바뀔 때마다(0→실측, 탭 표시 등) relayout
    if (window.ResizeObserver && mapEl.value) {
      let lastW = 0
      let lastH = 0
      resizeObserver = new ResizeObserver((entries) => {
        const cr = entries[0]?.contentRect
        if (!cr) return
        const w = Math.round(cr.width)
        const h = Math.round(cr.height)
        if (w > 0 && h > 0 && (w !== lastW || h !== lastH)) {
          lastW = w
          lastH = h
          refresh()
        }
      })
      resizeObserver.observe(mapEl.value)
    }

    // 장소 목록이 바뀌면(검색/카테고리/거리순) 전체 bounds 로 다시 맞춘다.
    watch(
      () => props.places,
      () => {
        needsFit = true
        renderMarkers(kakao)
      },
      { deep: true },
    )
    // 선택만 바뀌면 카메라를 재설정하지 않고(needsFit=false) 강조 갱신 후 부드럽게 panTo.
    watch(
      () => props.selectedId,
      () => {
        renderMarkers(kakao)
        panToSelected(kakao)
      },
    )
  } catch (e) {
    error.value = e.message || '지도를 불러올 수 없습니다.'
    // eslint-disable-next-line no-console
    console.error('[TripMap]', e)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  clearOverlays()
  map = null
  kakaoRef = null
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
  background: var(--pin-color, #F78F57);
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
  background: var(--pin-color, #F78F57);
  filter: brightness(0.85);
  width: 34px;
  height: 34px;
  font-size: 14px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.3);
}

/* 선택된 핀 위 상세보기 말풍선 */
.trip-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 200px;
  padding: 7px 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--color-peach, #f78f57);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  cursor: pointer;
  white-space: nowrap;
  transform: translateX(-50%);
  position: relative;
  left: 50%;
}
.trip-info::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid #fff;
}
.trip-info-name {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-ink, #2b2b2b);
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}
.trip-info-go {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-peach-pressed, #e0743a);
  flex-shrink: 0;
}
</style>
