# Created: 2026-06-16 14:04:14
<template>
  <div class="page">
    <!-- Photo hero -->
    <div class="hero">
      <div class="hero-img" :style="hp.imageUrl ? `background-image: url('${hp.imageUrl}'); background-size: cover; background-position: center;` : ''" />
      <div class="hero-overlay" />
      <div class="hero-top">
        <button class="ghost-btn" @click="$router.back()">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
        </button>
        <button class="ghost-btn" @click="share">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
            <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Content -->
    <div class="content-scroll">
      <div class="meta-row">
        <span class="cat-tag">{{ hp.category }}</span>
        <div class="rating">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="#f78f57"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
          <span class="rating-val">{{ hp.rating }}</span>
          <span class="rating-count">({{ hp.ratingCount }})</span>
        </div>
      </div>

      <h1 class="hp-name">{{ hp.name }}</h1>

      <div class="address-row">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
        <span class="address-text">{{ hp.address }}</span>
      </div>

      <!-- Mini map -->
      <div class="mini-map">
        <!-- 실좌표 보유 시 카카오 지도, 미보유/로드 실패 시 데코레이션 핀으로 폴백 -->
        <div v-show="hasCoords && !mapError" ref="mapEl" class="mini-map-el" />
        <div v-if="!hasCoords || mapError" class="mini-map-bg">
          <div class="mini-map-pin">
            <svg width="24" height="30" viewBox="0 0 30 38" fill="#f78f57">
              <path d="M15 0C6.716 0 0 6.716 0 15c0 10.5 15 23 15 23S30 25.5 30 15C30 6.716 23.284 0 15 0z" />
              <circle cx="15" cy="15" r="6" fill="white" />
            </svg>
          </div>
        </div>
        <button v-if="hasCoords" class="route-btn" @click="openRoute">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12h18M12 5l7 7-7 7" /></svg>
          길찾기
        </button>
      </div>

      <!-- 핫플 소개 -->
      <div class="section">
        <h3 class="section-title">핫플 소개</h3>
        <p class="intro-text">{{ hp.intro }}</p>
      </div>

      <!-- Registrant -->
      <div class="registrant-row">
        <div class="registrant-avatar" />
        <div class="registrant-info">
          <span class="registrant-name">{{ hp.registrant }}</span>
          <span class="registrant-date">등록 · {{ hp.registeredAt }}</span>
        </div>
      </div>

      <div style="height: 120px" />
    </div>

    <!-- Bottom CTA -->
    <div class="cta-bar">
      <button class="cta-main" @click="addToItinerary">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
          <line x1="12" y1="14" x2="12" y2="18" /><line x1="10" y1="16" x2="14" y2="16" />
        </svg>
        내 일정에 추가
      </button>
      <button class="cta-bookmark" :class="{ bookmarked }" :disabled="bookmarkLoading" @click="toggleBookmark">
        <svg width="20" height="20" viewBox="0 0 24 24" :fill="bookmarked ? 'var(--color-peach)' : 'none'" :stroke="bookmarked ? 'var(--color-peach)' : 'var(--color-ink)'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHotplaceStore } from '@/stores/hotplace.js'
import { useAuthStore } from '@/stores/auth.js'
import { favoriteApi } from '@/api/index.js'

const route = useRoute()
const router = useRouter()
const hotplaceStore = useHotplaceStore()
const authStore = useAuthStore()

const DEFAULT_HP = {
  id: null, name: '핫플레이스', category: '명소', location: '제주',
  address: '주소 정보 없음', rating: 0, ratingCount: 0,
  intro: '소개 정보가 없습니다.', registrant: '익명', registeredAt: '-',
  imageUrl: null, lat: null, lng: null,
}

const hp = ref({ ...DEFAULT_HP, id: route.params.id })
const loading = ref(true)
const bookmarked = ref(false)
const bookmarkLoading = ref(false)

// 미니맵용 실좌표 여부
const mapEl = ref(null)
const mapError = ref('')
const hasCoords = computed(() =>
  Number.isFinite(Number(hp.value.lat)) && Number.isFinite(Number(hp.value.lng)),
)

const KAKAO_KEY = import.meta.env.VITE_KAKAO_MAP_KEY
let miniMap = null

function loadKakao() {
  if (window.kakao?.maps?.services) return Promise.resolve(window.kakao)
  if (window.__kakaoMapLoading && !window.kakao?.maps?.services) {
    window.__kakaoMapLoading = null
  }
  if (window.__kakaoMapLoading) return window.__kakaoMapLoading
  window.__kakaoMapLoading = new Promise((resolve, reject) => {
    if (!KAKAO_KEY) { reject(new Error('VITE_KAKAO_MAP_KEY 누락')); return }
    const s = document.createElement('script')
    s.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_KEY}&autoload=false&libraries=services`
    s.onload = () => window.kakao.maps.load(() => resolve(window.kakao))
    s.onerror = () => reject(new Error('Kakao 지도 SDK 로드 실패'))
    document.head.appendChild(s)
  })
  return window.__kakaoMapLoading
}

// 실좌표로 미니맵 렌더 (마커 표시, 상호작용 최소화)
async function renderMiniMap() {
  if (!hasCoords.value) return
  try {
    const kakao = await loadKakao()
    await nextTick()
    if (!mapEl.value) return
    const pos = new kakao.maps.LatLng(Number(hp.value.lat), Number(hp.value.lng))
    miniMap = new kakao.maps.Map(mapEl.value, { center: pos, level: 4 })
    new kakao.maps.Marker({ position: pos, map: miniMap })
    miniMap.setZoomable(false)
    miniMap.setDraggable(false)
    setTimeout(() => {
      miniMap?.relayout()
      miniMap?.setCenter(pos)
    }, 150)
  } catch (e) {
    mapError.value = e.message || '지도를 불러올 수 없습니다.'
  }
}

onMounted(async () => {
  try {
    const detail = await hotplaceStore.getDetail(route.params.id)
    if (detail) hp.value = detail
    else hp.value = hotplaceStore.getById(route.params.id) ?? { ...DEFAULT_HP, id: route.params.id }
  } finally {
    loading.value = false
  }
  await renderMiniMap()
  // 상세 로드 후 hp.id가 확정되면 초기 찜 상태 수화 (로그인 시)
  loadBookmark()
})

onBeforeUnmount(() => {
  miniMap = null
})

function share() {
  if (navigator.share) navigator.share({ title: hp.value.name, url: location.href })
}
// 카카오맵 길찾기 — 실좌표 보유 시 새 탭으로 카카오 지도 길찾기 페이지를 연다.
function openRoute() {
  if (!hasCoords.value) return
  const name = encodeURIComponent(hp.value.name || '목적지')
  const url = `https://map.kakao.com/link/to/${name},${Number(hp.value.lat)},${Number(hp.value.lng)}`
  window.open(url, '_blank', 'noopener')
}
function addToItinerary() {
  router.push('/plan')
}

// ── 찜(즐겨찾기) 토글 — 응답 favorited로 갱신, 실패 시 롤백 ────────────────────
async function toggleBookmark() {
  if (!authStore.isAuthenticated) {
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  const targetId = hp.value.id
  if (targetId == null) return
  const prev = bookmarked.value
  bookmarked.value = !prev          // 낙관적 갱신
  bookmarkLoading.value = true
  try {
    const { data } = await favoriteApi.toggle('HOTPLACE', targetId, hp.value?.name)
    bookmarked.value = !!data?.favorited
  } catch {
    bookmarked.value = prev         // 실패 롤백
  } finally {
    bookmarkLoading.value = false
  }
}

// 초기 찜 상태 수화 — 내 즐겨찾기 목록에 이 핫플이 있으면 표시
async function loadBookmark() {
  if (!authStore.isAuthenticated) return
  const targetId = String(hp.value.id)
  try {
    const { data } = await favoriteApi.list('HOTPLACE')
    bookmarked.value = Array.isArray(data)
      && data.some((f) => String(f.targetId) === targetId)
  } catch {
    bookmarked.value = false
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}

.hero {
  position: relative;
  height: 300px;
  flex-shrink: 0;
}
.hero-img {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #d8c8b8 0%, #c0b0a0 100%);
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.28) 0%, transparent 40%, rgba(0,0,0,0.1) 100%);
}
.hero-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  z-index: 1;
}
.ghost-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.25);
  border-radius: 50%;
  backdrop-filter: blur(4px);
}
.content-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 20px 20px 0;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.cat-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 12px;
  font-weight: 600;
}
.rating {
  display: flex;
  align-items: center;
  gap: 4px;
}
.rating-val { font-size: 15px; font-weight: 700; color: var(--color-ink); }
.rating-count { font-size: 13px; color: var(--color-ink-muted); }

.hp-name {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.6px;
  margin-bottom: 10px;
}
.address-row {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 16px;
}
.address-text { font-size: 13.5px; color: var(--color-ink-muted); }

.mini-map {
  height: 130px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 24px;
  position: relative;
  background: #edf2e8;
}
.mini-map-el {
  width: 100%;
  height: 100%;
}
.mini-map-bg {
  width: 100%;
  height: 100%;
  background: #edf2e8;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mini-map-pin {
  margin-bottom: 12px;
}
.route-btn {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  background: white;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink);
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
}

.section { margin-bottom: 24px; }
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 10px;
}
.intro-text {
  font-size: 14.5px;
  color: var(--color-ink-secondary);
  line-height: 1.65;
  letter-spacing: -0.2px;
}

.registrant-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 0;
  border-top: 1px solid var(--color-line-light);
}
.registrant-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
}
.registrant-info { display: flex; flex-direction: column; gap: 2px; }
.registrant-name { font-size: 14px; font-weight: 600; color: var(--color-ink); }
.registrant-date { font-size: 12px; color: var(--color-ink-muted); }

.cta-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 8px;
  padding: 12px 20px calc(12px + var(--safe-bottom));
  background: white;
  border-top: 1px solid var(--color-line-light);
}
.cta-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 15px;
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}
.cta-bookmark {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: border-color 0.15s;
}
.cta-bookmark.bookmarked { border-color: var(--color-peach); }
</style>
