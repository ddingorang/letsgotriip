# Created: 2026-06-16 14:05:43
<template>
  <div class="page">
    <!-- Hero photo -->
    <div class="hero">
      <div class="hero-img" />
      <div class="hero-overlay" />
      <div class="hero-top">
        <button class="ghost-btn" @click="$router.back()">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
        </button>
        <div class="hero-top-right">
          <!-- 수정 화면(/companion/:id/edit) 라우트가 없어 깨진 이동을 방지하기 위해 수정 버튼 제거 -->
          <button class="ghost-btn" @click="share">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="content-scroll">
      <!-- Status & title -->
      <div class="title-area">
        <div class="badges-row">
          <span v-if="comp.isOwner" class="badge owner-badge">내 모집글 · 방장</span>
          <span v-else :class="['badge', comp.status === '마감임박' ? 'badge-urgent' : 'badge-open']">{{ comp.status }}</span>
          <span class="date-range">{{ comp.dateRange }}</span>
        </div>
        <h1 class="comp-title">{{ comp.title }}</h1>

        <!-- Author -->
        <div class="author-row">
          <div class="author-avatar" />
          <div class="author-info">
            <span class="author-name">{{ comp.author?.nickname }}</span>
            <span class="author-sub">방장 · 동행 {{ comp.author?.tripCount }}회</span>
          </div>
          <span v-if="!comp.isOwner" class="seat-count">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
            {{ comp.currentCount }}/{{ comp.maxCount }} 모집 인원
          </span>
        </div>

        <!-- Owner: applicant status -->
        <div v-if="comp.isOwner" class="owner-status-row">
          <span class="owner-status-label">신청 현황</span>
          <span class="owner-status-val">대기 {{ comp.pendingCount }} · 승인 {{ comp.approvedCount }} / 정원 {{ comp.maxCount }}</span>
          <div class="avatar-stack">
            <div v-for="n in Math.min(comp.approvedCount + comp.pendingCount, 3)" :key="n" class="stack-avatar" />
          </div>
        </div>
      </div>

      <!-- Pending state for applied user -->
      <div v-if="isApplied && !comp.isOwner" class="pending-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
        </svg>
        <div>
          <div class="pending-title">승인 대기 중이에요</div>
          <div class="pending-sub">방장이 신청을 확인하면 채팅방에 입장할 수 있어요.</div>
        </div>
      </div>

      <!-- 모집 조건 -->
      <div class="cond-head">
        <h3 class="section-title">모집 조건</h3>
        <span v-if="seatsLeft > 0" class="cond-seats">남은 자리 {{ seatsLeft }}명</span>
        <span v-else class="cond-seats cond-seats-full">모집 마감</span>
      </div>

      <!-- Info grid -->
      <div class="info-grid">
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
            <span class="info-label">일정</span>
          </div>
          <span class="info-val">{{ comp.dateRange || '-' }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            <span class="info-label">기간</span>
          </div>
          <span class="info-val">{{ comp.period || '-' }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
            <span class="info-label">지역</span>
          </div>
          <span class="info-val">{{ comp.location || '-' }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
            <span class="info-label">모집 인원</span>
          </div>
          <span class="info-val">{{ comp.currentCount }}/{{ comp.maxCount }}명</span>
        </div>
        <div class="info-cell info-cell-wide">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" /></svg>
            <span class="info-label">예상 비용</span>
          </div>
          <span class="info-val">{{ comp.estimatedCost || '-' }}</span>
        </div>
      </div>

      <!-- Tags -->
      <div v-if="comp.tags?.length" class="tags-row">
        <span v-for="tag in comp.tags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <!-- Intro -->
      <div class="section">
        <h3 class="section-title">소개</h3>
        <p class="intro-text">{{ comp.intro }}</p>
      </div>

      <!-- Linked plan: map + day-by-day route (연결된 계획이 있을 때만) -->
      <div v-if="linkedPlan" class="section plan-section">
        <div class="cond-head">
          <h3 class="section-title">정해진 일정</h3>
          <span class="plan-fixed-pill">계획 연동</span>
        </div>
        <p v-if="linkedPlan.title" class="plan-meta">
          {{ linkedPlan.title }}
          <span v-if="planDateRange" class="plan-date">· {{ planDateRange }}</span>
        </p>

        <!-- Kakao map -->
        <div class="plan-map-wrap">
          <div ref="mapEl" class="plan-map" />
          <div v-if="mapError" class="plan-map-error">{{ mapError }}</div>
          <div v-else-if="!hasPlaces" class="plan-map-error">표시할 장소 좌표가 없어요.</div>
        </div>

        <!-- Day-by-day place list -->
        <div v-for="day in placesByDay" :key="day.dayNo" class="plan-day">
          <div class="plan-day-head">
            <span class="plan-day-pill">{{ day.dayNo }}일차</span>
          </div>
          <div class="plan-route">
            <div
              v-for="(place, idx) in day.places"
              :key="`${day.dayNo}-${idx}`"
              class="plan-stop"
            >
              <div class="plan-stop-left">
                <div class="plan-stop-dot">{{ idx + 1 }}</div>
                <div v-if="idx < day.places.length - 1" class="plan-stop-line" />
              </div>
              <div class="plan-stop-name">{{ place.title }}</div>
            </div>
          </div>
        </div>
      </div>

      <div style="height: 100px" />
    </div>

    <!-- Bottom CTA -->
    <div class="cta-bar">
      <div v-if="applyError" class="apply-error">{{ applyError }}</div>
      <!-- Visitor: not applied -->
      <template v-if="!comp.isOwner && !isApplied">
        <div class="seats-left">남은 자리 {{ seatsLeft }}명</div>
        <button class="cta-main" :disabled="companionStore.loading" @click="apply">참여 신청하기</button>
      </template>

      <!-- Visitor: approved — 취소 불가, 채팅방 입장 -->
      <template v-else-if="!comp.isOwner && isApplied && isApproved">
        <button class="cta-main" :disabled="comp.chatRoomId == null" @click="openChat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
          채팅방 입장
        </button>
      </template>

      <!-- Visitor: applied (pending) — 취소 가능 -->
      <template v-else-if="!comp.isOwner && isApplied">
        <button class="cta-cancel" @click="cancelApply">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          신청 취소
        </button>
      </template>

      <!-- Owner -->
      <template v-else>
        <button class="cta-chat" :disabled="comp.chatRoomId == null" @click="openChat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
        </button>
        <button class="cta-main" @click="$router.push(`/companion/${comp.id}/applicants`)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
          신청자 관리 {{ comp.pendingCount }}
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'

const route = useRoute()
const router = useRouter()
const companionStore = useCompanionStore()

const comp = computed(() => companionStore.getById(route.params.id) ?? {
  id: route.params.id, title: '동행 모집', location: '-', dateRange: '-',
  status: '모집중', currentCount: 0, maxCount: 4, author: { nickname: '-', tripCount: 0 },
  period: '-', estimatedCost: '-', tags: [], intro: '',
  isOwner: false, pendingCount: 0, approvedCount: 0,
  isApplied: false, myApplicationId: null, myApplicationStatus: null, chatRoomId: null,
})

// 신청 여부는 서버 응답(comp.isApplied)을 기준으로 하되,
// 신청/취소 직후에는 재조회 전까지 낙관적 오버라이드를 적용한다.
const appliedOverride = ref(null)
const isApplied = computed(() =>
  appliedOverride.value !== null ? appliedOverride.value : !!comp.value.isApplied,
)
// 승인된 신청은 채팅방 멤버십이 생성되어 취소가 불가하다(BE에서 409 반환).
// → 취소 버튼 대신 채팅방 입장 안내를 노출한다.
const isApproved = computed(() => comp.value.myApplicationStatus === 'APPROVED')
const applyError = ref('')

// 남은 모집 자리 — 모집 조건 카드와 하단 CTA에서 공유(중복 계산 방지)
const seatsLeft = computed(() =>
  Math.max(0, (comp.value.maxCount ?? 0) - (comp.value.currentCount ?? 0)),
)

// ── 연결된 계획(지도/동선) ──────────────────────────────────────────────────
// 상세 응답의 linkedPlan = { planId, title, startDate, endDate, places:[{ dayNo, title, lat, lng }] }
const linkedPlan = computed(() => comp.value.linkedPlan ?? null)
// 좌표(lat,lng)가 모두 있는 장소만 (BE가 이미 걸러주지만 방어적으로 한 번 더)
const mapPlaces = computed(() =>
  (linkedPlan.value?.places ?? []).filter(
    p => Number.isFinite(Number(p.lat)) && Number.isFinite(Number(p.lng)),
  ),
)
const hasPlaces = computed(() => mapPlaces.value.length > 0)
// 일자(dayNo)별 그룹핑 — 순서 보존
const placesByDay = computed(() => {
  const groups = []
  const byDay = new Map()
  for (const p of mapPlaces.value) {
    const dayNo = p.dayNo ?? 1
    if (!byDay.has(dayNo)) {
      const entry = { dayNo, places: [] }
      byDay.set(dayNo, entry)
      groups.push(entry)
    }
    byDay.get(dayNo).places.push(p)
  }
  return groups
})
const planDateRange = computed(() => {
  const lp = linkedPlan.value
  if (!lp?.startDate) return ''
  return lp.endDate && lp.endDate !== lp.startDate
    ? `${lp.startDate} ~ ${lp.endDate}`
    : lp.startDate
})

// ── Kakao 지도 ──────────────────────────────────────────────────────────────
const mapEl = ref(null)
const mapError = ref('')
const KAKAO_KEY = import.meta.env.VITE_KAKAO_MAP_KEY
let map = null
let markers = []
let mapReady = false

// HotplaceRegisterView 와 동일한 SDK 로딩 패턴(전역 캐시 + autoload=false)
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

function clearMarkers() {
  markers.forEach(m => m.setMap(null))
  markers = []
}

// 장소 좌표로 마커를 찍고 bounds 에 맞춰 화면을 맞춘다
function renderMarkers() {
  if (!map || !window.kakao?.maps) return
  clearMarkers()
  const places = mapPlaces.value
  if (!places.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  places.forEach((p, i) => {
    const pos = new window.kakao.maps.LatLng(Number(p.lat), Number(p.lng))
    const marker = new window.kakao.maps.Marker({ position: pos, map })
    markers.push(marker)
    bounds.extend(pos)
    // 방문 순서/장소명 인포 라벨
    const overlay = new window.kakao.maps.CustomOverlay({
      position: pos,
      yAnchor: 2.1,
      content: `<div class="map-label">${i + 1}. ${escapeHtml(p.title ?? '')}</div>`,
    })
    overlay.setMap(map)
    markers.push(overlay)
  })
  if (places.length === 1) {
    map.setCenter(new window.kakao.maps.LatLng(Number(places[0].lat), Number(places[0].lng)))
    map.setLevel(5)
  } else {
    map.setBounds(bounds)
  }
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
  ))
}

// linkedPlan 이 준비되면(상세 비동기 로드 후) 지도를 초기화/갱신한다
async function ensureMap() {
  if (!hasPlaces.value) return
  await nextTick()
  if (!mapEl.value) return
  try {
    if (!map) {
      const kakao = await loadKakao()
      if (!mapEl.value) return
      map = new kakao.maps.Map(mapEl.value, {
        center: new kakao.maps.LatLng(Number(mapPlaces.value[0].lat), Number(mapPlaces.value[0].lng)),
        level: 7,
      })
      mapReady = true
      setTimeout(() => map?.relayout(), 200)
    }
    renderMarkers()
  } catch (e) {
    mapError.value = e.message || '지도를 불러올 수 없습니다.'
  }
}

watch(hasPlaces, (ready) => {
  if (ready) ensureMap()
})

onMounted(async () => {
  await companionStore.getDetail(route.params.id)
  if (hasPlaces.value) ensureMap()
})

onBeforeUnmount(() => {
  clearMarkers()
  map = null
  mapReady = false
})

async function apply() {
  applyError.value = ''
  try {
    await companionStore.join(comp.value.id)
    appliedOverride.value = true
    // 서버 기준 isApplied/myApplicationId 동기화
    await companionStore.getDetail(route.params.id)
    appliedOverride.value = null
  } catch {
    applyError.value = companionStore.error || '신청에 실패했어요.'
  }
}

async function cancelApply() {
  applyError.value = ''
  const applicationId = comp.value.myApplicationId
  if (!applicationId) {
    // 신청 ID를 모르면 최신 상세를 다시 받아 확인
    await companionStore.getDetail(route.params.id)
  }
  const id = comp.value.myApplicationId
  if (!id) {
    applyError.value = '신청 정보를 찾을 수 없어요.'
    return
  }
  try {
    await companionStore.cancel(comp.value.id, id)
    appliedOverride.value = false
    await companionStore.getDetail(route.params.id)
    appliedOverride.value = null
  } catch {
    applyError.value = companionStore.error || '신청 취소에 실패했어요.'
  }
}

function openChat() {
  const chatRoomId = comp.value.chatRoomId
  if (chatRoomId != null) {
    router.push(`/chat/${chatRoomId}`)
  }
}

function share() {
  if (navigator.share) navigator.share({ title: comp.value.title, url: location.href })
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
  height: 260px;
  position: relative;
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
  background: linear-gradient(to bottom, rgba(0,0,0,0.28) 0%, transparent 50%);
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
.hero-top-right { display: flex; gap: 8px; }
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

.content-scroll { flex: 1; overflow-y: auto; }

.title-area {
  padding: 20px 20px 0;
  border-bottom: 1px solid var(--color-line-light);
  padding-bottom: 16px;
}
.badges-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.badge {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11.5px;
  font-weight: 600;
}
.badge-open { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.badge-urgent { background: #fff0e8; color: #d04010; }
.owner-badge { background: var(--color-peach); color: white; }
.date-range { font-size: 13px; color: var(--color-ink-muted); }

.comp-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
  margin-bottom: 14px;
}
.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
}
.author-info { display: flex; flex-direction: column; gap: 1px; flex: 1; }
.author-name { font-size: 14px; font-weight: 600; color: var(--color-ink); }
.author-sub { font-size: 12px; color: var(--color-ink-muted); }
.seat-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}

.owner-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}
.owner-status-label { font-size: 12px; color: var(--color-ink-muted); white-space: nowrap; }
.owner-status-val { font-size: 13.5px; font-weight: 600; color: var(--color-ink); flex: 1; }
.avatar-stack { display: flex; }
.stack-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-line);
  border: 2px solid white;
  margin-left: -6px;
}
.stack-avatar:first-child { margin-left: 0; }

.pending-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 16px 20px;
  padding: 14px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
}
.pending-title { font-size: 13.5px; font-weight: 700; color: var(--color-peach-pressed); margin-bottom: 2px; }
.pending-sub { font-size: 12.5px; color: var(--color-ink-secondary); line-height: 1.5; }

.cond-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 20px 0;
}
.cond-head .section-title { margin-bottom: 0; flex: 1; }
.cond-seats {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}
.cond-seats-full {
  color: var(--color-ink-muted);
  background: var(--color-surface);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin: 12px 20px 16px;
  background: var(--color-line-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-line-light);
}
.info-cell-wide { grid-column: 1 / -1; }
.info-cell {
  padding: 14px 16px;
  background: var(--color-white);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.info-icon-row { display: flex; align-items: center; gap: 4px; }
.info-label { font-size: 12px; color: var(--color-ink-muted); }
.info-val { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 20px 20px;
}
.tag-chip {
  padding: 5px 12px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-ink-secondary);
  font-weight: 500;
}

.section { padding: 0 20px 20px; }
.section-title { font-size: 16px; font-weight: 700; color: var(--color-ink); margin-bottom: 10px; letter-spacing: -0.3px; }
.intro-text { font-size: 14.5px; color: var(--color-ink-secondary); line-height: 1.7; letter-spacing: -0.2px; }

.cta-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 12px 20px calc(12px + var(--safe-bottom));
  background: white;
  border-top: 1px solid var(--color-line-light);
}
.seats-left {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  white-space: nowrap;
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
.cta-cancel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 15px;
  border: 1.5px solid var(--color-line);
  color: var(--color-ink-secondary);
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-xl);
}
.cta-chat {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-ink);
}
.apply-error {
  width: 100%;
  font-size: 13px;
  color: var(--color-error);
  font-weight: 500;
  text-align: center;
  padding-bottom: 4px;
}

/* ── Linked plan: map + route ─────────────────────────────────────────────── */
.plan-section { padding-top: 4px; }
.plan-section .cond-head { padding: 0; margin-bottom: 8px; }
.plan-fixed-pill {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  padding: 3px 9px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}
.plan-meta {
  font-size: 13px;
  color: var(--color-ink-secondary);
  margin: -4px 0 12px;
  font-weight: 600;
}
.plan-date { color: var(--color-ink-muted); font-weight: 500; }

.plan-map-wrap {
  position: relative;
  width: 100%;
  height: 220px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-line-light);
  background: var(--color-surface);
  margin-bottom: 16px;
}
.plan-map { width: 100%; height: 100%; }
.plan-map-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--color-ink-muted);
  background: var(--color-surface);
}

.plan-day { margin-bottom: 14px; }
.plan-day-head { margin-bottom: 8px; }
.plan-day-pill {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  padding: 4px 11px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 700;
}
.plan-route { display: flex; flex-direction: column; }
.plan-stop { display: flex; gap: 10px; align-items: stretch; }
.plan-stop-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 22px;
  flex-shrink: 0;
}
.plan-stop-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--color-peach);
  background: var(--color-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 800;
  color: var(--color-peach);
  flex-shrink: 0;
  z-index: 1;
}
.plan-stop-line {
  width: 2px;
  flex: 1;
  background: var(--color-line-light);
  margin: 2px 0;
  min-height: 14px;
}
.plan-stop-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  padding: 1px 0 12px;
}

/* Kakao CustomOverlay 라벨 — scoped 밖에서 렌더되므로 :deep 사용 */
:deep(.map-label) {
  background: rgba(30, 30, 30, 0.86);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  white-space: nowrap;
  transform: translateX(-50%);
  pointer-events: none;
}
</style>
