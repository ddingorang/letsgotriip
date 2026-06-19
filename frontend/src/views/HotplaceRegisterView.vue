# Created: 2026-06-18 17:59:13
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="nav-title">핫플 등록</span>
      <button class="submit-top-btn" :disabled="!isValid || submitting" @click="submit">{{ submitting ? '등록 중…' : '등록' }}</button>
    </header>

    <div class="scroll-content">
      <!-- Map + Search overlay -->
      <div class="map-area">
        <div ref="mapEl" class="map-el" />

        <!-- Search overlaid on map -->
        <div class="map-search-overlay">
          <div class="search-input-wrap">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
            </svg>
            <input
              v-model="searchQuery"
              class="search-input"
              placeholder="장소 또는 주소 검색"
              @input="onSearchInput"
              @keydown.enter.prevent="runSearch"
            />
            <button v-if="searchQuery" class="clear-btn" @click="clearSearch">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <!-- Search results dropdown -->
          <ul v-if="searchResults.length" class="search-dropdown">
            <li
              v-for="r in searchResults"
              :key="r.id"
              class="search-item"
              @click="selectSearchResult(r)"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
              <div class="search-item-text">
                <span class="search-item-name">{{ r.place_name }}</span>
                <span class="search-item-addr">{{ r.road_address_name || r.address_name }}</span>
              </div>
            </li>
          </ul>
        </div>

        <div v-if="mapError" class="map-error">{{ mapError }}</div>
        <div v-if="!selectedAddress" class="map-hint-card">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
          검색하거나 지도를 클릭해 위치를 지정하세요
        </div>
      </div>

      <!-- Form -->
      <div class="form-section">
        <div class="loc-label">선택한 위치</div>
        <div class="loc-input-wrap">
          <span class="loc-text" :class="{ placeholder: !selectedAddress }">
            {{ selectedAddress || '지도에서 위치를 선택해주세요' }}
          </span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /><circle cx="12" cy="10" r="3" /></svg>
        </div>

        <div class="field">
          <label class="field-label">핫플 이름 <span class="req">*</span></label>
          <input v-model="form.name" class="field-input" placeholder="예) 오조포구 카페" />
        </div>

        <div class="field">
          <label class="field-label">카테고리 <span class="req">*</span></label>
          <div class="chips-row">
            <button
              v-for="cat in categories"
              :key="cat"
              :class="['chip-btn', { active: form.category === cat }]"
              @click="form.category = cat"
            >{{ cat }}</button>
          </div>
        </div>

        <div class="field">
          <label class="field-label">설명</label>
          <textarea
            v-model="form.description"
            class="field-textarea"
            placeholder="이 장소의 매력을 알려주세요 (오션뷰, 주차, 분위기 등)"
            rows="4"
          />
        </div>

        <div class="field">
          <label class="field-label">사진</label>
          <div class="photo-grid">
            <button class="photo-add-btn">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span class="photo-count">0/5</span>
            </button>
          </div>
        </div>
        <div style="height: 32px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHotplaceStore } from '@/stores/hotplace.js'

const router = useRouter()
const hotplaceStore = useHotplaceStore()

const mapEl = ref(null)
const mapError = ref('')
const searchQuery = ref('')
const searchResults = ref([])
const selectedAddress = ref('')
const selectedLat = ref(33.450701)
const selectedLng = ref(126.570667)

const categories = ['카페', '맛집', '명소', '포토존', '숙소']
const form = ref({ name: '', category: '', description: '' })
const submitting = ref(false)

const isValid = computed(() => form.value.name && form.value.category)

const KAKAO_KEY = import.meta.env.VITE_KAKAO_MAP_KEY

let map = null
let marker = null
let geocoder = null
let ps = null
let searchTimer = null

function loadKakao() {
  // services가 이미 로드된 경우 바로 반환
  if (window.kakao?.maps?.services) return Promise.resolve(window.kakao)
  // services 없이 로드된 경우 재로드
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

function placeMarker(kakao, lat, lng) {
  const pos = new kakao.maps.LatLng(lat, lng)
  if (marker) {
    marker.setPosition(pos)
  } else {
    marker = new kakao.maps.Marker({ position: pos, map })
  }
  selectedLat.value = lat
  selectedLng.value = lng
  map.setCenter(pos)
}

function reverseGeocode(lat, lng) {
  if (!geocoder) return
  geocoder.coord2Address(lng, lat, (result, status) => {
    if (status === window.kakao.maps.services.Status.OK) {
      const r = result[0]
      selectedAddress.value = r.road_address?.address_name || r.address?.address_name || ''
    }
  })
}

function onSearchInput() {
  clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) { searchResults.value = []; return }
  searchTimer = setTimeout(runSearch, 350)
}

function runSearch() {
  const query = searchQuery.value.trim()
  if (!query) return

  // 주소 검색 우선, 결과 없으면 키워드 검색
  geocoder.addressSearch(query, (addrResult, addrStatus) => {
    if (addrStatus === window.kakao.maps.services.Status.OK && addrResult.length) {
      searchResults.value = addrResult.slice(0, 5).map(r => ({
        id: r.address_name,
        place_name: r.address_name,
        road_address_name: r.road_address?.address_name || '',
        address_name: r.address_name,
        x: r.x,
        y: r.y,
      }))
    } else {
      // 주소 검색 실패 시 키워드 검색
      ps.keywordSearch(query, (data, status) => {
        if (status === window.kakao.maps.services.Status.OK) {
          searchResults.value = data.slice(0, 5)
        } else {
          searchResults.value = []
        }
      })
    }
  })
}

function selectSearchResult(r) {
  const lat = Number(r.y)
  const lng = Number(r.x)
  placeMarker(window.kakao, lat, lng)
  selectedAddress.value = r.road_address_name || r.address_name
  if (form.value.name === '' && r.place_name !== r.address_name) {
    form.value.name = r.place_name
  }
  map.setLevel(3)
  searchResults.value = []
  searchQuery.value = ''
}

function clearSearch() {
  searchQuery.value = ''
  searchResults.value = []
}

onMounted(async () => {
  try {
    const kakao = await loadKakao()
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(selectedLat.value, selectedLng.value),
      level: 7,
    })
    geocoder = new kakao.maps.services.Geocoder()
    ps = new kakao.maps.services.Places()

    // 지도 클릭으로 핀 이동
    kakao.maps.event.addListener(map, 'click', (mouseEvent) => {
      const lat = mouseEvent.latLng.getLat()
      const lng = mouseEvent.latLng.getLng()
      placeMarker(kakao, lat, lng)
      reverseGeocode(lat, lng)
    })

    setTimeout(() => map?.relayout(), 200)
  } catch (e) {
    mapError.value = e.message || '지도를 불러올 수 없습니다.'
  }
})

onBeforeUnmount(() => {
  clearTimeout(searchTimer)
  map = null
  marker = null
})

async function submit() {
  if (!isValid.value || submitting.value) return
  const payload = {
    name: form.value.name,
    address: selectedAddress.value || '',
    description: form.value.description || '',
    category: hotplaceStore.toCategoryEnum(form.value.category),
    imageUrls: [],
    latitude: selectedLat.value,
    longitude: selectedLng.value,
  }
  submitting.value = true
  try {
    await hotplaceStore.create(payload)
    hotplaceStore.registrationSuccess = true
    router.back()
  } catch (e) {
    hotplaceStore.hotplaces.push({
      id: Date.now(),
      name: form.value.name,
      category: form.value.category,
      address: selectedAddress.value,
      location: selectedAddress.value,
      description: form.value.description,
      lat: selectedLat.value,
      lng: selectedLng.value,
      rating: 0, ratingCount: 0, saveCount: 0,
      registrant: '나', registeredAt: '방금',
      intro: form.value.description,
    })
    hotplaceStore.registrationSuccess = true
    router.back()
  } finally {
    submitting.value = false
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

.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.back-btn {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-ink);
}
.nav-title { font-size: 16px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.submit-top-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 14px; font-weight: 700;
  transition: opacity 0.15s;
}
.submit-top-btn:disabled { opacity: 0.4; }

.scroll-content { flex: 1; overflow-y: auto; }

/* Search overlay on map */
.map-search-overlay {
  position: absolute;
  top: 12px; left: 12px; right: 12px;
  z-index: 20;
}
.search-input-wrap {
  display: flex; align-items: center; gap: 8px;
  background: var(--color-white);
  border-radius: var(--radius-full);
  padding: 10px 14px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.15);
}
.search-input {
  flex: 1; font-size: 14px; color: var(--color-ink); background: transparent;
}
.search-input::placeholder { color: var(--color-ink-muted); }
.clear-btn { display: flex; align-items: center; color: var(--color-ink-muted); }

.search-dropdown {
  margin-top: 6px;
  background: var(--color-white);
  border-radius: var(--radius-md);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  overflow: hidden;
}
.search-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--color-line-light);
}
.search-item:last-child { border-bottom: none; }
.search-item:active { background: var(--color-surface); }
.search-item-text { display: flex; flex-direction: column; gap: 2px; }
.search-item-name { font-size: 14px; font-weight: 600; color: var(--color-ink); letter-spacing: -0.2px; }
.search-item-addr { font-size: 12px; color: var(--color-ink-muted); letter-spacing: -0.2px; }

/* Map */
.map-area {
  height: 300px;
  position: relative;
  flex-shrink: 0;
}
.map-el { width: 100%; height: 100%; }

.map-error {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: var(--color-ink-muted);
  background: #e8f0e8;
}

.map-hint-card {
  position: absolute;
  bottom: 12px; left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  background: rgba(30,30,30,0.82);
  color: white;
  font-size: 12.5px; font-weight: 500;
  padding: 7px 14px;
  border-radius: var(--radius-full);
  display: flex; align-items: center; gap: 6px;
  pointer-events: none;
  z-index: 10;
}

/* Form */
.form-section { padding: 20px 16px; }

.loc-label {
  font-size: 11.5px; font-weight: 600;
  color: var(--color-ink-muted);
  letter-spacing: 0.2px;
  margin-bottom: 6px;
}
.loc-input-wrap {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}
.loc-text { font-size: 13.5px; color: var(--color-ink); flex: 1; }
.loc-text.placeholder { color: var(--color-ink-muted); }

.field { margin-bottom: 20px; }
.field-label { display: block; font-size: 13.5px; font-weight: 600; color: var(--color-ink); margin-bottom: 8px; }
.req { color: var(--color-peach); }
.field-input {
  width: 100%; padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px; color: var(--color-ink);
}
.field-input::placeholder { color: var(--color-ink-muted); }
.field-textarea {
  width: 100%; padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px; color: var(--color-ink);
  resize: none; line-height: 1.6;
}
.field-textarea::placeholder { color: var(--color-ink-muted); }

.chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip-btn {
  padding: 7px 16px; border-radius: var(--radius-full);
  font-size: 13.5px; font-weight: 500;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  transition: all 0.15s;
}
.chip-btn.active { background: var(--color-peach); color: white; border-color: var(--color-peach); }

.photo-grid { display: flex; gap: 8px; }
.photo-add-btn {
  width: 80px; height: 80px;
  border-radius: var(--radius-md);
  border: 1.5px dashed var(--color-line);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  background: var(--color-surface);
}
.photo-count { font-size: 11px; color: var(--color-ink-muted); }
</style>
