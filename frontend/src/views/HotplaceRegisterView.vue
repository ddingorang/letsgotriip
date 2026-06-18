# Created: 2026-06-16 14:04:50
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
      <!-- Search -->
      <div class="search-area">
        <div class="search-input-wrap">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
          </svg>
          <input v-model="searchQuery" class="search-input" placeholder="장소 또는 주소 검색" />
        </div>
      </div>

      <!-- Map placeholder -->
      <div class="map-area">
        <div class="map-bg">
          <div class="map-road h-road" style="top: 38%" />
          <div class="map-road h-road thin" style="top: 62%" />
          <div class="map-road v-road" style="left: 40%" />
          <div class="map-road v-road thin" style="left: 65%" />
          <div style="position:absolute;top:8%;left:8%;width:26%;height:20%;background:#d8e8d0;border-radius:4px;opacity:.7" />
          <div style="position:absolute;top:68%;left:0;width:35%;height:28%;background:#b8d4e8;opacity:.6" />
          <div class="map-center-pin">
            <svg width="32" height="40" viewBox="0 0 30 38" fill="#f78f57">
              <path d="M15 0C6.716 0 0 6.716 0 15c0 10.5 15 23 15 23S30 25.5 30 15C30 6.716 23.284 0 15 0z" />
              <circle cx="15" cy="15" r="6" fill="white" />
            </svg>
          </div>
        </div>
        <div class="map-hint-card">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
          지도를 움직여 위치를 지정하세요
        </div>
      </div>

      <!-- Form -->
      <div class="form-section">
        <!-- Selected location -->
        <div v-if="selectedAddress" class="selected-loc">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
          <span>{{ selectedAddress }}</span>
        </div>
        <div class="loc-label">선택한 위치</div>
        <div class="loc-input-wrap">
          <span class="loc-text">{{ selectedAddress || '지도에서 위치를 선택해주세요' }}</span>
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
            >
              {{ cat }}
            </button>
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useHotplaceStore } from '@/stores/hotplace.js'

const router = useRouter()
const hotplaceStore = useHotplaceStore()

const searchQuery = ref('')
const selectedAddress = ref('제주 서귀포시 성산읍 오조리 일주동로 1234')
// lat/lng may be set if the map integration provides coordinates
const selectedLat = ref(null)
const selectedLng = ref(null)

const categories = ['카페', '맛집', '명소', '포토존', '숙소']
const form = ref({ name: '', category: '', description: '' })
const submitting = ref(false)

const isValid = computed(() => form.value.name && form.value.category)

async function submit() {
  if (!isValid.value || submitting.value) return

  // Build HotPlaceCreateRequest payload
  const payload = {
    name: form.value.name,
    address: selectedAddress.value || '',
    description: form.value.description || '',
    category: hotplaceStore.toCategoryEnum(form.value.category),
    imageUrls: [],
  }
  if (selectedLat.value != null) payload.latitude = selectedLat.value
  if (selectedLng.value != null) payload.longitude = selectedLng.value

  submitting.value = true
  try {
    await hotplaceStore.create(payload)
    hotplaceStore.registrationSuccess = true
    router.back()
  } catch (e) {
    console.warn('[register] create failed, using optimistic fallback', e)
    // Optimistic fallback: add locally and mark success
    hotplaceStore.hotplaces.push({
      id: Date.now(),
      name: form.value.name,
      category: form.value.category,
      address: selectedAddress.value,
      description: form.value.description,
      rating: 0,
      ratingCount: 0,
      saveCount: 0,
      registrant: '나',
      registeredAt: '방금',
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
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.nav-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.submit-top-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
  transition: opacity 0.15s;
}
.submit-top-btn:disabled { opacity: 0.4; }

.scroll-content { flex: 1; overflow-y: auto; }

.search-area {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-line-light);
}
.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 10px 16px;
}
.search-input {
  flex: 1;
  font-size: 14px;
  color: var(--color-ink);
  background: transparent;
}
.search-input::placeholder { color: var(--color-ink-muted); }

.map-area {
  height: 220px;
  position: relative;
}
.map-bg {
  width: 100%;
  height: 100%;
  background: #edf2e8;
  position: relative;
  overflow: hidden;
}
.map-road { position: absolute; background: #fff; }
.map-road.h-road { left: 0; right: 0; height: 5px; }
.map-road.v-road { top: 0; bottom: 0; width: 4px; }
.map-road.thin { height: 3px; width: 2px; }
.map-center-pin {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -100%);
}
.map-hint-card {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  background: rgba(30,30,30,0.85);
  color: white;
  font-size: 12.5px;
  font-weight: 500;
  padding: 7px 14px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-section { padding: 20px 16px; }

.selected-loc {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-ink-secondary);
  margin-bottom: 4px;
}
.loc-label {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
  letter-spacing: 0.2px;
  margin-bottom: 6px;
  margin-top: 8px;
}
.loc-input-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}
.loc-text { font-size: 13.5px; color: var(--color-ink); flex: 1; }

.field { margin-bottom: 20px; }
.field-label {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  margin-bottom: 8px;
}
.req { color: var(--color-peach); }
.field-input {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
}
.field-input::placeholder { color: var(--color-ink-muted); }
.field-textarea {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  resize: none;
  line-height: 1.6;
}
.field-textarea::placeholder { color: var(--color-ink-muted); }

.chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  transition: all 0.15s;
}
.chip-btn.active {
  background: var(--color-peach);
  color: white;
  border-color: var(--color-peach);
}

.photo-grid { display: flex; gap: 8px; }
.photo-add-btn {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  border: 1.5px dashed var(--color-line);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: var(--color-surface);
}
.photo-count { font-size: 11px; color: var(--color-ink-muted); }
</style>
