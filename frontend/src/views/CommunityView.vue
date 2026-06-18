# Created: 2026-06-16 14:03:38
<template>
  <div class="page">
    <!-- Header -->
    <header class="comm-header">
      <h1 class="header-title">커뮤니티</h1>
      <div class="header-right">
        <button class="icon-btn">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
          </svg>
        </button>
        <button class="icon-btn bell-wrap">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <span class="notif-dot" />
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
          @click="activeFilter = f"
        >
          {{ f }}
        </button>
      </div>
      <div class="scroll-content" @scroll="onScroll">
        <div class="post-list">
          <PostCard
            v-for="post in filteredPosts"
            :key="post.id"
            :post="post"
            @click="$router.push(`/community/${post.id}`)"
            @like="postsStore.likePost($event)"
          />
        </div>
        <div v-if="postsStore.loading" class="loading-row">
          <div class="spinner" />
        </div>
        <div v-if="!postsStore.hasMore && filteredPosts.length > 0" class="end-msg">모든 게시글을 불러왔어요</div>
        <div class="bottom-spacer" />
      </div>
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

      <!-- Map view -->
      <div v-if="hpView === 'map'" class="map-wrap">
        <div class="map-bg">
          <div class="map-road h-road" style="top: 32%" />
          <div class="map-road h-road thin" style="top: 58%" />
          <div class="map-road v-road" style="left: 42%" />
          <div class="map-road v-road thin" style="left: 68%" />
          <div class="map-block" style="top:8%;left:10%;width:22%;height:18%;background:#d8e8d0" />
          <div class="map-block" style="top:38%;left:55%;width:28%;height:14%;background:#dde8d8" />
          <div class="map-block water" style="top:68%;left:0%;width:35%;height:28%" />

          <button
            v-for="hp in filteredHotplaces"
            :key="hp.id"
            :class="['pin-btn', { selected: selectedHp?.id === hp.id }]"
            :style="{ left: hp.x + '%', top: hp.y + '%' }"
            @click="selectedHp = selectedHp?.id === hp.id ? null : hp"
          >
            <svg width="30" height="38" viewBox="0 0 30 38" :fill="selectedHp?.id === hp.id ? '#e0743a' : '#f78f57'">
              <path d="M15 0C6.716 0 0 6.716 0 15c0 10.5 15 23 15 23S30 25.5 30 15C30 6.716 23.284 0 15 0z" />
              <circle cx="15" cy="15" r="6" fill="white" />
            </svg>
            <span v-if="selectedHp?.id === hp.id" class="pin-label">{{ hp.name }}</span>
          </button>
        </div>

        <Transition name="slide-up">
          <div v-if="selectedHp" class="map-card" @click="$router.push(`/hotplace/${selectedHp.id}`)">
            <div class="map-thumb" />
            <div class="map-card-body">
              <div class="map-card-row1">
                <span class="cat-tag">{{ selectedHp.category }}</span>
                <span class="walk-time">도보 12분</span>
              </div>
              <div class="map-card-name">{{ selectedHp.name }}</div>
              <div class="map-card-sub">{{ selectedHp.location }} · {{ selectedHp.description }}</div>
              <div class="map-card-rating">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="#f78f57"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" /></svg>
                {{ selectedHp.rating }} ({{ selectedHp.ratingCount }})
              </div>
            </div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </Transition>
      </div>

      <!-- List view -->
      <div v-else class="hp-list">
        <div
          v-for="hp in filteredHotplaces"
          :key="hp.id"
          class="hp-item"
          @click="$router.push(`/hotplace/${hp.id}`)"
        >
          <div class="hp-thumb">
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
              {{ hp.rating }} ({{ hp.ratingCount }}) · 저장 {{ hp.saveCount }}
            </div>
          </div>
          <button class="bookmark-btn" @click.stop>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" /></svg>
          </button>
        </div>
      </div>

      <button class="fab" @click="$router.push('/hotplace/register')">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
      </button>
    </div>

    <!-- ③ 동행 -->
    <div v-show="activeMain === 2" class="tab-pane companion-pane">
      <div class="scroll-content">
        <div class="section-header">
          <span class="section-title">참여 중인 방</span>
          <button class="see-all-btn" @click="$router.push('/chat')">전체보기</button>
        </div>
        <div class="rooms-row">
          <div
            v-for="room in companionStore.myRooms.slice(0, 3)"
            :key="room.id"
            class="room-card"
            @click="$router.push(`/chat/${room.id}`)"
          >
            <div class="room-avatar-area" />
            <div class="room-bottom">
              <div class="room-name">{{ room.title }}</div>
              <div class="room-d-row">
                <span v-if="room.daysLeft !== null" :class="['room-d', { urgent: room.daysLeft <= 3 }]">D-{{ room.daysLeft }}</span>
                <span v-else class="room-d ended">종료</span>
              </div>
              <div v-if="room.unreadCount > 0" class="room-unread-badge">새 메시지 {{ room.unreadCount }}</div>
              <div v-else class="room-read">읽음</div>
            </div>
          </div>
        </div>

        <div class="section-header" style="margin-top: 24px">
          <span class="section-title">동행 모집</span>
          <button class="sort-btn">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="15" y2="12" /><line x1="3" y1="18" x2="9" y2="18" /></svg>
            최신순
          </button>
        </div>
        <div class="companion-list">
          <div
            v-for="comp in companionStore.companions"
            :key="comp.id"
            class="comp-item"
            @click="$router.push(`/companion/${comp.id}`)"
          >
            <div class="comp-thumb" />
            <div class="comp-info">
              <div class="comp-header-row">
                <span :class="['status-badge', { urgent: comp.status === '마감임박' }]">{{ comp.status }}</span>
                <span class="comp-date">{{ comp.dateRange }}</span>
              </div>
              <div class="comp-title">{{ comp.title }}</div>
              <div class="comp-meta">
                <span class="comp-loc">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
                  {{ comp.location }}
                </span>
                <span class="comp-people">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" /></svg>
                  {{ comp.currentCount }}/{{ comp.maxCount }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="bottom-spacer" />
      </div>

      <button class="fab" @click="$router.push('/companion/write')">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></svg>
      </button>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import PostCard from '@/components/community/PostCard.vue'
import { usePostsStore } from '@/stores/posts.js'
import { useHotplaceStore } from '@/stores/hotplace.js'
import { useCompanionStore } from '@/stores/companion.js'

const postsStore = usePostsStore()
const hotplaceStore = useHotplaceStore()
const companionStore = useCompanionStore()

const activeMain = ref(0)

// 공유게시판
const filterTabs = ['전체', '후기', '꿀팁', '동행']
const activeFilter = ref('전체')
const filteredPosts = computed(() => {
  if (activeFilter.value === '전체') return postsStore.posts
  return postsStore.posts.filter((p) => (p.categoryLabel ?? p.category) === activeFilter.value)
})
function onScroll(e) {
  const el = e.target
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 80) postsStore.fetchPosts()
}

// 핫플
const hpView = ref('list')
const hpCategories = ['전체', '카페', '맛집', '명소', '포토존']
const hpCat = ref('전체')
const selectedHp = ref(null)
const filteredHotplaces = computed(() => {
  if (hpCat.value === '전체') return hotplaceStore.hotplaces
  return hotplaceStore.hotplaces.filter((h) => h.category === hpCat.value)
})

onMounted(() => {
  postsStore.fetchPosts(true)
  companionStore.fetchCompanions()
  companionStore.fetchMyRooms()
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
.bottom-spacer { height: 16px; }

/* ====== 핫플 ====== */
.hp-pane { background: var(--color-white); }
.hp-controls {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
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
  position: relative;
  overflow: hidden;
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
  bottom: 0;
  left: 0;
  right: 0;
  margin: 0 12px 12px;
  background: white;
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
}
.map-card-body { flex: 1; min-width: 0; }
.map-card-row1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}
.walk-time { font-size: 11.5px; color: var(--color-ink-muted); }
.map-card-name { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.map-card-sub { font-size: 12px; color: var(--color-ink-muted); margin-top: 2px; }
.map-card-rating { display: flex; align-items: center; gap: 3px; font-size: 12px; color: var(--color-ink-secondary); margin-top: 4px; }

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
  padding: 4px 0;
}
.hp-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}
.hp-thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
  padding: 6px;
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
.bookmark-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ====== 동행 ====== */
.companion-pane { background: var(--color-white); }
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 10px;
}
.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}
.see-all-btn {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-muted);
}
.sort-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-ink-secondary);
}

/* Room cards horizontal scroll */
.rooms-row {
  display: flex;
  gap: 10px;
  padding: 0 16px 4px;
  overflow-x: auto;
}
.room-card {
  flex-shrink: 0;
  width: 120px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line-light);
  overflow: hidden;
  cursor: pointer;
  background: var(--color-white);
}
.room-avatar-area {
  width: 100%;
  height: 70px;
  background: var(--color-surface);
}
.room-bottom {
  padding: 8px 10px 10px;
}
.room-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.room-d-row { margin-top: 2px; }
.room-d {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-muted);
}
.room-d.urgent { color: var(--color-peach); }
.room-d.ended { color: var(--color-ink-muted); }
.room-unread-badge {
  margin-top: 5px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-peach);
}
.room-read {
  margin-top: 5px;
  font-size: 11px;
  color: var(--color-ink-muted);
}

/* Companion list */
.companion-list { padding: 0; }
.comp-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}
.comp-thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  flex-shrink: 0;
}
.comp-info { flex: 1; min-width: 0; }
.comp-header-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}
.status-badge.urgent {
  background: #fff0e8;
  color: #d04010;
}
.comp-date { font-size: 12px; color: var(--color-ink-muted); }
.comp-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.comp-meta { display: flex; align-items: center; gap: 12px; }
.comp-loc, .comp-people {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--color-ink-muted);
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
.fab:active { background: var(--color-peach-pressed); transform: scale(0.95); }

/* Transitions */
.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.25s ease, opacity 0.2s; }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); opacity: 0; }
</style>
