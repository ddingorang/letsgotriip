# Created: 2026-06-16 14:03:38
<template>
  <div class="page">
    <!-- Header -->
    <header class="comm-header">
      <h1 class="header-title">커뮤니티</h1>
      <div class="header-right">
        <button class="icon-btn" @click="$router.push('/search')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
          </svg>
        </button>
        <button class="icon-btn bell-wrap" @click="$router.push('/notifications')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <span v-if="notifStore.hasUnread" class="notif-dot" />
        </button>
      </div>
    </header>

    <!-- Main tabs -->
    <div class="main-tab-bar">
      <button
        v-for="(tab, i) in ['공유게시판', '핫플', '동행']"
        :key="tab"
        :class="['main-tab', { active: activeMain === i }]"
        @click="activeMain = i"
      >
        {{ tab }}
      </button>
    </div>

    <!-- ① 공유게시판 -->
    <div v-show="activeMain === 0" class="tab-pane">
      <div class="filter-row">
        <button
          v-for="f in filterTabs"
          :key="f"
          :class="['chip-btn', { active: activeFilter === f }]"
          @click="selectFilter(f)"
        >
          {{ f }}
        </button>
      </div>
      <div class="scroll-content" @scroll="onScroll">
        <div class="post-list">
          <PostCard
            v-for="post in postsStore.posts"
            :key="post.id"
            :post="post"
            @click="$router.push(`/community/${post.id}`)"
            @like="postsStore.likePost($event)"
            @bookmark="postsStore.bookmarkPost($event)"
          />
        </div>
        <div v-if="!postsStore.loading && postsStore.posts.length === 0" class="hp-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" />
          </svg>
          <span>아직 게시글이 없어요</span>
          <button class="hp-empty-btn" @click="$router.push('/community/write')">글쓰기</button>
        </div>
        <div v-if="postsStore.loading" class="loading-row">
          <div class="spinner" />
        </div>
        <div v-if="!postsStore.hasMore && postsStore.posts.length > 0" class="end-msg">모든 게시글을 불러왔어요</div>
        <div class="bottom-spacer" />
      </div>

      <!-- 글쓰기 FAB — 게시글 유무와 무관하게 항상 노출(글 0개일 때만 보이던 진입점 보완) -->
      <button class="fab" @click="$router.push('/community/write')" aria-label="글쓰기">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
      </button>
    </div>

    <!-- ② 핫플 -->
    <div v-show="activeMain === 1" class="tab-pane hp-pane">
      <div class="hp-controls">
        <div class="view-toggle">
          <button :class="['toggle-btn', { active: hpView === 'map' }]" @click="hpView = 'map'">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
              <line x1="8" y1="2" x2="8" y2="18" /><line x1="16" y1="6" x2="16" y2="22" />
            </svg>
            지도
          </button>
          <button :class="['toggle-btn', { active: hpView === 'list' }]" @click="hpView = 'list'">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" />
              <line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
            </svg>
            목록
          </button>
        </div>
        <div class="sort-toggle" aria-label="핫플 정렬 기준">
          <button
            v-for="option in hpSortOptions"
            :key="option.value"
            type="button"
            :class="['sort-btn', { active: hpSort === option.value }]"
            :aria-pressed="hpSort === option.value"
            @click="selectHpSort(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
        <div class="cat-scroll">
          <button
            v-for="cat in hpCategories"
            :key="cat"
            :class="['chip-btn', { active: hpCat === cat }]"
            @click="hpCat = cat"
          >
            {{ cat }}
          </button>
        </div>
      </div>

      <!-- Map view (실제 카카오 지도) -->
      <div v-if="hpView === 'map'" class="map-wrap">
        <TripMap
          :places="mappableHotplaces"
          :selected-id="selectedHp?.id"
          @select="onHpSelect"
          @detail="goHotplaceDetail"
        />
        <div v-if="!mappableHotplaces.length" class="map-empty">
          지도에 표시할 핫플이 아직 없어요
        </div>

        <Transition name="slide-up">
          <div
            v-if="mapCardHotplace"
            class="map-card"
            role="button"
            tabindex="0"
            @click="goHotplaceDetail(mapCardHotplace)"
            @keydown.enter.prevent="goHotplaceDetail(mapCardHotplace)"
            @keydown.space.prevent="goHotplaceDetail(mapCardHotplace)"
          >
            <div class="map-thumb">
              <img :src="mapCardHotplace.imageUrl || seedImg('hp-' + mapCardHotplace.id)" :alt="mapCardHotplace.name" @error="onThumbError($event, 'hp-' + mapCardHotplace.id)" />
            </div>
            <div class="map-card-body">
              <div class="map-card-row1">
                <span class="cat-tag">{{ mapCardHotplace.category }}</span>
              </div>
              <div class="map-card-name">{{ mapCardHotplace.name }}</div>
              <div class="map-card-sub">{{ mapCardHotplace.location }} · {{ mapCardHotplace.description }}</div>
              <div class="map-card-rating">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="#f78f57"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                {{ mapCardHotplace.rating }} ({{ mapCardHotplace.ratingCount }})
              </div>
            </div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </Transition>
      </div>

      <!-- List view -->
      <div v-else class="hp-list">
        <div v-if="!filteredHotplaces.length" class="hp-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /><circle cx="12" cy="10" r="3" />
          </svg>
          <span>아직 등록된 핫플이 없어요</span>
          <button class="hp-empty-btn" @click="$router.push('/hotplace/register')">핫플 등록하기</button>
        </div>
        <div
          v-for="hp in filteredHotplaces"
          :key="hp.id"
          class="hp-item"
          role="button"
          tabindex="0"
          @click="goHotplaceDetail(hp)"
          @keydown.enter.prevent="goHotplaceDetail(hp)"
          @keydown.space.prevent="goHotplaceDetail(hp)"
        >
          <div class="hp-thumb">
            <img :src="hp.imageUrl || seedImg('hp-' + hp.id)" :alt="hp.name" @error="onThumbError($event, 'hp-' + hp.id)" />
            <span class="cat-tag">{{ hp.category }}</span>
          </div>
          <div class="hp-info">
            <div class="hp-name">{{ hp.name }}</div>
            <div class="hp-loc">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
              {{ hp.location }}
            </div>
            <div class="hp-stats">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="#f78f57"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
              {{ hp.rating }} ({{ hp.ratingCount }}) · 좋아요 {{ hp.likeCount ?? 0 }}
            </div>
          </div>
        </div>
      </div>

      <button
        :class="['fab', 'hp-fab', { 'with-map-card': hpView === 'map' && mapCardHotplace }]"
        @click="$router.push('/hotplace/register')"
        aria-label="핫플 등록하기"
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
      </button>
    </div>

    <!-- ③ 동행 -->
    <div v-show="activeMain === 2" class="tab-pane companion-pane">
      <div class="scroll-content">
        <div class="companion-section">
          <div v-if="companions.length" class="companion-list">
            <div
              v-for="comp in companions"
              :key="comp.id"
              class="companion-card"
              @click="$router.push(`/companion/${comp.id}`)"
            >
              <div class="comp-thumb" :class="{ ph: !comp.imageUrl }">
                <img
                  v-if="comp.imageUrl"
                  :src="comp.imageUrl"
                  alt=""
                  loading="lazy"
                  @error="(e) => { e.target.style.display = 'none'; e.target.parentElement.classList.add('ph') }"
                />
              </div>
              <div class="comp-header">
                <span class="comp-badge" :class="{ urgent: comp.status === '마감임박' }">{{ comp.status }}</span>
                <span
                  v-if="companionDday(comp.dateRange) != null"
                  class="comp-dday"
                  :class="{ urgent: companionDday(comp.dateRange) <= 3 }"
                >
                  D-{{ companionDday(comp.dateRange) }}
                </span>
              </div>
              <h4 class="comp-title">{{ comp.title }}</h4>
              <p class="comp-sub">{{ comp.location }}<span v-if="comp.dateRange"> · {{ comp.dateRange }}</span></p>
              <div class="comp-footer">
                <div class="comp-members">
                  <div v-for="i in Math.min(comp.currentCount, 6)" :key="i" class="member-dot" />
                  <span class="member-text">{{ comp.currentCount }}/{{ comp.maxCount }}명</span>
                </div>
                <button class="join-btn" @click.stop="$router.push(`/companion/${comp.id}`)">참여하기</button>
              </div>
            </div>
          </div>
          <div v-else class="companion-empty">
            <p class="companion-empty-text">아직 모집 중인 동행이 없어요</p>
            <button class="companion-empty-btn" @click="$router.push('/companion/write')">
              동행 모집하기
            </button>
          </div>
          <div class="bottom-spacer" />
        </div>
      </div>

      <button class="fab" @click="$router.push('/companion/write')" aria-label="동행 모집하기">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
      </button>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import PostCard from '@/components/community/PostCard.vue'
import TripMap from '@/components/common/TripMap.vue'
import { usePostsStore } from '@/stores/posts.js'
import { useHotplaceStore } from '@/stores/hotplace.js'
import { useCompanionStore } from '@/stores/companion.js'
import { useNotificationStore } from '@/stores/notification.js'

const postsStore = usePostsStore()
const hotplaceStore = useHotplaceStore()
const companionStore = useCompanionStore()
const notifStore = useNotificationStore()
const route = useRoute()
const router = useRouter()

// 진입 쿼리(?tab=hotplace|companion)로 초기 탭 선택 — 0:공유게시판 1:핫플 2:동행
const TAB_INDEX = { board: 0, hotplace: 1, companion: 2 }
const activeMain = ref(TAB_INDEX[route.query.tab] ?? 0)

// 동행 — 모집 중인 동행 목록(계획 페이지에서 커뮤니티 탭으로 이전)
const { companions } = storeToRefs(companionStore)
// dateRange('YYYY-MM-DD')에서 남은 일수. 파싱 불가/과거면 null → 배지 미표시.
function companionDday(dateStr) {
  if (!dateStr) return null
  const target = new Date(dateStr)
  if (Number.isNaN(target.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  target.setHours(0, 0, 0, 0)
  const diff = Math.round((target - today) / 86400000)
  return diff >= 0 ? diff : null
}

// 공유게시판
const filterTabs = ['전체', '후기', '질문', '꿀팁', '맛집', '동행']
const activeFilter = ref('전체')
// 라벨 → BE PostCategory enum. '전체'는 무필터(undefined).
const FILTER_ENUM = {
  후기: 'REVIEW',
  질문: 'QUESTION',
  꿀팁: 'TIP',
  맛집: 'RESTAURANT',
  동행: 'COMPANION',
}
// 카테고리 필터 변경 시 서버에서 재조회(클라이언트 필터 제거 — 페이지네이션과 정합)
function selectFilter(f) {
  if (activeFilter.value === f) return
  activeFilter.value = f
  postsStore.fetchPosts(true, FILTER_ENUM[f])
}
function onScroll(e) {
  const el = e.target
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 80) postsStore.fetchPosts()
}

// 핫플
const hpView = ref('list')
const hpSortOptions = [
  { value: 'latest', label: '최신순' },
  { value: 'popular', label: '인기순' },
  { value: 'name', label: '이름순' },
]
const hpSort = ref('latest')
const hpCategories = ['전체', '카페', '맛집', '명소', '포토존']
const hpCat = ref('전체')
const selectedHp = ref(null)
const filteredHotplaces = computed(() => {
  if (hpCat.value === '전체') return hotplaceStore.hotplaces
  return hotplaceStore.hotplaces.filter((h) => h.category === hpCat.value)
})
// 지도용 — 좌표(lat/lng)가 있는 핫플만
const mappableHotplaces = computed(() =>
  filteredHotplaces.value.filter(
    (h) => Number.isFinite(Number(h.lat)) && Number.isFinite(Number(h.lng)),
  ),
)
const mapCardHotplace = computed(() => {
  if (!selectedHp.value) return null
  return mappableHotplaces.value.find((h) => h.id === selectedHp.value.id) ?? null
})
function onHpSelect(place) {
  if (selectedHp.value?.id === place.id) {
    goHotplaceDetail(place)
    return
  }
  selectedHp.value = place
}
async function loadHotplaces() {
  const items = await hotplaceStore.getList(undefined, { sort: hpSort.value })
  if (selectedHp.value && !items.some((h) => h.id === selectedHp.value.id)) {
    selectedHp.value = null
  }
}
function selectHpSort(sort) {
  if (hpSort.value === sort) return
  hpSort.value = sort
  selectedHp.value = null
  loadHotplaces()
}
function goHotplaceDetail(place) {
  const id = place?.id
  if (id == null) return
  router.push({ name: 'hotplace-detail', params: { id } })
}

// 썸네일 — 업로드 이미지가 없거나 로딩 실패 시 로컬 기본 썸네일로 채움(외부 더미 미사용)
const THUMB_PLACEHOLDER = '/images/placeholder-thumb.png'
function seedImg() {
  return THUMB_PLACEHOLDER
}
function onThumbError(e) {
  if (!e.target.src.endsWith(THUMB_PLACEHOLDER)) e.target.src = THUMB_PLACEHOLDER
}

onMounted(() => {
  postsStore.fetchPosts(true)
  notifStore.load()
  loadHotplaces()
  companionStore.fetchCompanions()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}

/* Header */
.comm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 20px 12px;
  flex-shrink: 0;
}
.header-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
.icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.bell-wrap {
  position: relative;
}
.notif-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-peach);
  border: 1.5px solid white;
}

/* Main tabs */
.main-tab-bar {
  display: flex;
  border-bottom: 1px solid var(--color-line-light);
  padding: 0 16px;
  flex-shrink: 0;
}
.main-tab {
  flex: 1;
  padding: 11px 0;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  position: relative;
  letter-spacing: -0.3px;
}
.main-tab.active {
  color: var(--color-ink);
  font-weight: 700;
}
.main-tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-peach);
  border-radius: 2px 2px 0 0;
}

/* Tab panes */
.tab-pane {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* Shared scroll */
.scroll-content {
  flex: 1;
  overflow-y: auto;
}

/* Chips */
.filter-row {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  overflow-x: auto;
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-line-light);
}
.chip-btn {
  flex-shrink: 0;
  padding: 6px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  letter-spacing: -0.2px;
  transition: all 0.15s;
}
.chip-btn.active {
  background: var(--color-ink);
  color: white;
}

/* Post list */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
}
.loading-row {
  display: flex;
  justify-content: center;
  padding: 20px;
}
.spinner {
  width: 24px;
  height: 24px;
  border: 2.5px solid var(--color-line);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.end-msg {
  text-align: center;
  padding: 20px;
  font-size: 13px;
  color: var(--color-ink-muted);
}
.bottom-spacer { height: calc(28px + var(--safe-bottom)); }

/* ====== 핫플 ====== */
.hp-pane {
  background: var(--color-white);
  min-height: 0;
}
.hp-controls {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
  position: relative;
  z-index: 30;
  background: var(--color-white);
}
.view-toggle {
  display: flex;
  gap: 6px;
  align-self: flex-start;
}
.toggle-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  letter-spacing: -0.2px;
  transition: all 0.15s;
}
.toggle-btn.active {
  background: var(--color-ink);
  color: white;
  border-color: var(--color-ink);
}
.sort-toggle {
  display: flex;
  gap: 6px;
  overflow-x: auto;
}
.sort-btn {
  flex-shrink: 0;
  min-width: 64px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  letter-spacing: 0;
  transition: all 0.15s;
}
.sort-btn.active {
  background: var(--color-peach);
  color: white;
  border-color: var(--color-peach);
}
.cat-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}
.cat-scroll .chip-btn.active {
  background: var(--color-peach);
  color: white;
}

/* Map */
.map-wrap {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

.map-wrap :deep(.trip-map) {
  min-height: 0;
}

.map-empty {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 35;
  max-width: calc(100% - 32px);
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  box-shadow: var(--shadow-card);
  border-radius: var(--radius-full);
  padding: 8px 16px;
  font-size: 12.5px;
  color: var(--color-ink-muted);
  text-align: center;
}

.hp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 56px 20px;
  color: var(--color-ink-muted);
  font-size: 14px;
}

.hp-empty-btn {
  margin-top: 4px;
  padding: 11px 22px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: #fff;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
}
.map-bg {
  width: 100%;
  height: 100%;
  background: #edf2e8;
  position: relative;
  overflow: hidden;
}
.map-road {
  position: absolute;
  background: #ffffff;
}
.map-road.h-road { left: 0; right: 0; height: 5px; }
.map-road.v-road { top: 0; bottom: 0; width: 4px; }
.map-road.thin { height: 3px; width: 2px; }
.map-block {
  position: absolute;
  border-radius: 4px;
  opacity: 0.7;
}
.map-block.water { background: #b8d4e8; border-radius: 0; }

.pin-btn {
  position: absolute;
  transform: translate(-50%, -100%);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.pin-btn.selected { z-index: 2; }
.pin-label {
  margin-top: 2px;
  background: var(--color-ink);
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  white-space: nowrap;
}
.map-card {
  position: absolute;
  bottom: calc(24px + var(--safe-bottom));
  left: 12px;
  right: 12px;
  z-index: 40;
  min-height: 88px;
  margin: 0;
  background: white;
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.map-thumb {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  flex-shrink: 0;
  overflow: hidden;
}
.map-thumb img,
.hp-thumb img,
.comp-thumb img,
.room-avatar-area img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.map-card-body { flex: 1; min-width: 0; }
.map-card-row1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}
.map-card-name { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.map-card-sub {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-top: 2px;
}
.map-card-rating { display: flex; align-items: center; gap: 3px; font-size: 12px; color: var(--color-ink-secondary); margin-top: 4px; }
.map-card > svg { flex-shrink: 0; }

.cat-tag {
  display: inline-block;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 11px;
  font-weight: 600;
}

/* HP list */
.hp-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0 calc(88px + var(--safe-bottom));
}
.hp-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}
.hp-item:focus-visible,
.map-card:focus-visible {
  outline: 2px solid var(--color-peach);
  outline-offset: -2px;
}
.hp-thumb {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  flex-shrink: 0;
  overflow: hidden;
}
.hp-thumb .cat-tag {
  position: absolute;
  left: 5px;
  bottom: 5px;
  padding: 2px 7px;
  font-size: 10px;
}
.hp-info { flex: 1; min-width: 0; }
.hp-name { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; margin-bottom: 4px; }
.hp-loc {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 4px;
}
.hp-stats {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--color-ink-secondary);
}
/* ====== 동행 ====== */
.companion-pane { background: var(--color-white); }
.companion-section { padding: 16px; }
.companion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.companion-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 16px;
  cursor: pointer;
  overflow: hidden;
}
/* 동행 카드 대표 이미지 — 카드 패딩을 상쇄해 상단 전체 폭 배너로 */
.comp-thumb {
  margin: -16px -16px 12px;
  height: 96px;
  position: relative;
  overflow: hidden;
  background: var(--color-line-light);
}
.comp-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.comp-thumb.ph {
  background: linear-gradient(135deg, #f7b690 0%, #e89a6c 100%);
}
.comp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.comp-badge {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 11.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-full);
}
.comp-badge.urgent {
  background: #fff0e8;
  color: #d04010;
}
.comp-dday {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-ink-muted);
}
.comp-dday.urgent {
  color: var(--color-error);
}
.comp-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 4px;
}
.comp-sub {
  font-size: 12.5px;
  color: var(--color-ink-muted);
  margin-bottom: 12px;
}
.comp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.comp-members {
  display: flex;
  align-items: center;
  gap: 4px;
}
.member-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-peach);
}
.member-text {
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-left: 4px;
}
.join-btn {
  background: var(--color-peach);
  color: white;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 16px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}
.companion-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 16px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}
.companion-empty-text {
  font-size: 13px;
  color: var(--color-ink-muted);
}
.companion-empty-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach-pressed);
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  padding: 8px 18px;
  border-radius: var(--radius-full);
  cursor: pointer;
}

/* FAB */
.fab {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-peach);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(247, 143, 87, 0.45);
  z-index: 10;
  transition: background 0.15s, transform 0.15s;
}
.hp-fab {
  bottom: calc(20px + var(--safe-bottom));
  z-index: 50;
}
.hp-fab.with-map-card {
  bottom: calc(128px + var(--safe-bottom));
}
.fab:active { background: var(--color-peach-pressed); transform: scale(0.95); }

/* Transitions */
.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.25s ease, opacity 0.2s; }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); opacity: 0; }
</style>
