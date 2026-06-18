# Created: 2026-06-16 14:27:37
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="nav-title-col">
        <span class="nav-title">{{ album.title }}</span>
        <span class="nav-sub">사진 {{ album.photoCount }}장</span>
      </div>
      <button class="share-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
          <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
        </svg>
      </button>
    </header>

    <div class="photo-grid-scroll">
      <div class="photo-grid">
        <!-- Add button -->
        <div class="photo-add-cell">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span class="add-label">추가</span>
        </div>

        <!-- Photos -->
        <div
          v-for="i in album.photoCount"
          :key="i"
          class="photo-cell"
        >
          <span class="photo-label">{{ album.location.replace(' 앨범', '') }} {{ i }}</span>
        </div>
      </div>
    </div>

    <!-- Bottom actions -->
    <div class="action-bar">
      <button class="action-link">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" />
        </svg>
        링크 복사
      </button>
      <button class="action-share">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8" /><polyline points="16 6 12 2 8 6" /><line x1="12" y1="2" x2="12" y2="15" />
        </svg>
        커뮤니티 공유
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const albums = [
  { id: 1, title: '제주 3박 4일', location: '제주 앨범', photoCount: 11 },
  { id: 2, title: '부산 주말 여행', location: '부산 앨범', photoCount: 8 },
  { id: 3, title: '경주 한바퀴', location: '경주 앨범', photoCount: 9 },
  { id: 4, title: '봄 벚꽃 모음', location: '벚꽃 앨범', photoCount: 6 },
]

const album = computed(() => albums.find((a) => a.id === Number(route.params.id)) ?? albums[0])
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
.back-btn, .share-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.nav-title-col { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.nav-title { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.nav-sub { font-size: 11.5px; color: var(--color-ink-muted); }

.photo-grid-scroll { flex: 1; overflow-y: auto; padding: 2px; }
.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2px;
}

.photo-add-cell {
  aspect-ratio: 1;
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  cursor: pointer;
}
.add-label { font-size: 11px; color: var(--color-ink-muted); }

.photo-cell {
  aspect-ratio: 1;
  background: linear-gradient(145deg, #e8ddd4, #d8cdc4);
  display: flex;
  align-items: flex-end;
  padding: 6px;
}
.photo-label {
  font-size: 10px;
  font-weight: 500;
  color: rgba(0,0,0,0.4);
}

.action-bar {
  display: flex;
  gap: 10px;
  padding: 12px 20px calc(12px + var(--safe-bottom));
  border-top: 1px solid var(--color-line-light);
  background: white;
  flex-shrink: 0;
}
.action-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 14px;
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-xl);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
}
.action-share {
  flex: 1.4;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 14px;
  background: var(--color-peach);
  border-radius: var(--radius-xl);
  font-size: 14px;
  font-weight: 700;
  color: white;
}
</style>
