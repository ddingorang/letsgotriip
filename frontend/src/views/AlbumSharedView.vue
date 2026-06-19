<template>
  <div class="page">
    <!-- 헤더 — 공유 앨범명 + 뒤로가기(홈) -->
    <header class="nav-header">
      <button class="back-btn" @click="goHome">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="nav-title-col">
        <span class="nav-title">{{ album?.name ?? '공유 앨범' }}</span>
        <span v-if="album" class="nav-sub">사진 {{ photos.length }}장</span>
      </div>
      <div class="nav-spacer" />
    </header>

    <!-- 공개(읽기전용) 안내 -->
    <div v-if="album && !loading && !error" class="ro-note">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z" /><circle cx="12" cy="12" r="3" />
      </svg>
      공유된 앨범을 보고 있어요
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="state-wrap">
      <p class="state-hint">불러오는 중…</p>
    </div>

    <!-- 에러 / 없음·만료 -->
    <div v-else-if="error" class="state-wrap">
      <div class="state-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <h3 class="state-title">앨범을 불러올 수 없어요</h3>
      <p class="state-sub">{{ error }}</p>
      <button class="state-btn" @click="goHome">홈으로</button>
    </div>

    <!-- 사진 그리드 (실데이터: photos[].imageUrl) -->
    <template v-else>
      <div v-if="photos.length" class="photo-grid-scroll">
        <div class="photo-grid">
          <div
            v-for="photo in photos"
            :key="photo.id"
            class="photo-cell"
          >
            <img :src="photo.imageUrl" :alt="album?.name" class="photo-img" loading="lazy" />
          </div>
        </div>
      </div>

      <!-- 빈 상태 — 앨범은 있으나 사진이 없음 -->
      <div v-else class="state-wrap">
        <div class="state-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
          </svg>
        </div>
        <h3 class="state-title">아직 사진이 없어요</h3>
        <p class="state-sub">이 앨범에는 공유된 사진이 없어요.</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { albumApi } from '@/api/index.js'

const router = useRouter()
const route = useRoute()

const album = ref(null)
const photos = ref([])
const loading = ref(true)
const error = ref('')

async function loadShared() {
  loading.value = true
  error.value = ''
  try {
    // 공개 조회 — 비로그인에서도 동작 (http는 토큰이 있을 때만 첨부)
    const { data } = await albumApi.getShared(route.params.token)
    album.value = data
    photos.value = data?.photos ?? []
  } catch (e) {
    // 404 = 없거나 만료된 공유 링크 / 그 외는 메시지 그대로 노출 (가짜 성공 위장 금지)
    const status = e.response?.status
    error.value =
      status === 404
        ? '없거나 만료된 공유 링크예요.'
        : e.response?.data?.message ?? e.message ?? '공유된 앨범을 불러오지 못했어요.'
    album.value = null
    photos.value = []
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/')
}

onMounted(loadShared)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--surface-bg);
}

/* ── 헤더 ──────────────────────────────────────────────────────────────────── */
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
}
.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  background: none;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
}
.nav-spacer { width: 40px; height: 40px; flex-shrink: 0; }
.nav-title-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 0;
}
.nav-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-sub { font-size: 11.5px; color: var(--text-tertiary); }

/* ── 읽기전용 안내 ────────────────────────────────────────────────────────── */
.ro-note {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--color-primary-50);
  border-bottom: 1px solid var(--color-primary-100);
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-primary-700);
  flex-shrink: 0;
}

/* ── 로딩/에러/빈 상태 ─────────────────────────────────────────────────────── */
.state-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
  text-align: center;
}
.state-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  background: var(--surface-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
}
.state-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.4px;
}
.state-sub {
  font-size: 13.5px;
  color: var(--text-tertiary);
  line-height: 1.6;
}
.state-hint {
  font-size: 13.5px;
  color: var(--text-tertiary);
}
.state-btn {
  margin-top: 4px;
  background: var(--color-primary-500);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 12px 28px;
  border: none;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
  cursor: pointer;
}

/* ── 사진 그리드 (AlbumDetailView 재사용) ─────────────────────────────────── */
.photo-grid-scroll { flex: 1; overflow-y: auto; padding: 2px; }
.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2px;
}
.photo-cell {
  aspect-ratio: 1;
  background: var(--surface-subtle);
  overflow: hidden;
}
.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>
