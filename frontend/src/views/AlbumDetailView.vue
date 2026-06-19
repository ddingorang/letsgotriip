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
        <span class="nav-title">{{ album?.name ?? '앨범' }}</span>
        <span class="nav-sub">사진 {{ photos.length }}장</span>
      </div>
      <!-- 우측 공유 버튼 — 실제 토큰 발급 후 공개 링크를 클립보드에 복사한다 -->
      <button
        class="share-btn"
        :class="{ disabled: sharing }"
        :disabled="sharing || !album"
        @click="onShare"
      >
        <svg v-if="!sharing" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
          <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
        </svg>
        <span v-else class="share-spinner-label">…</span>
      </button>
    </header>

    <!-- 로딩 -->
    <div v-if="loading" class="state-wrap">
      <p class="state-hint">불러오는 중…</p>
    </div>

    <!-- 에러 / 없음 -->
    <div v-else-if="error" class="state-wrap">
      <div class="state-icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
      <h3 class="state-title">앨범을 불러올 수 없어요</h3>
      <p class="state-sub">{{ error }}</p>
    </div>

    <template v-else>
      <div class="photo-grid-scroll">
        <div class="photo-grid">
          <!-- Add button -->
          <div class="photo-add-cell" :class="{ disabled: uploading }" @click="onAddClick">
            <svg v-if="!uploading" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span class="add-label">{{ uploading ? '업로드 중…' : '추가' }}</span>
          </div>

          <!-- Photos (실데이터: album.photos 의 imageUrl) -->
          <div
            v-for="photo in photos"
            :key="photo.id"
            class="photo-cell"
          >
            <img :src="photo.imageUrl" :alt="album?.name" class="photo-img" />
          </div>

          <!-- 사진이 한 장도 없을 때 안내 (추가 셀만 남음) -->
          <div v-if="photos.length === 0" class="photo-empty-cell">
            <span class="empty-hint">아직 사진이 없어요</span>
          </div>
        </div>
      </div>

      <!-- 멀티파트 업로드용 숨김 input -->
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display:none"
        @change="onFileSelected"
      />

      <!-- 공유 토큰 발급/복사 결과 피드백 (성공/실패 모두 정직하게 노출) -->
      <p v-if="shareMsg" class="share-msg" :class="{ err: shareErr }">{{ shareMsg }}</p>

      <!-- Bottom actions -->
      <div class="action-bar">
        <button class="action-link" @click="copyLink">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" />
          </svg>
          {{ copied ? '복사됨!' : '링크 복사' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/api/http.js'
import { albumApi as centralAlbumApi } from '@/api/index.js'

const route = useRoute()

// S3 가 api/index.js 에 albumApi 를 추가하면 그것을 소비하고,
// 아직 없으면 BE 경로(/users/me/albums/*)로 http 직접 호출한다.
// (가짜 성공 위장 금지 — 실패는 그대로 error 분기로 노출)
const albumApi = {
  get: (id) => http.get(`/users/me/albums/${id}`),
  uploadImage: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/users/me/albums/images', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  // 업로드한 imageUrl 을 앨범에 연결 (PATCH addImageUrls)
  addPhoto: (id, name, imageUrl) =>
    http.patch(`/users/me/albums/${id}`, { name, addImageUrls: [imageUrl] }),
}

const album = ref(null)
const photos = ref([])
const loading = ref(true)
const error = ref('')

const fileInput = ref(null)
const uploading = ref(false)
const copied = ref(false)

// 공유 상태
const sharing = ref(false)
const shareMsg = ref('')
const shareErr = ref(false)
let shareMsgTimer = null

async function loadAlbum() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await albumApi.get(route.params.id)
    album.value = data
    photos.value = data?.photos ?? []
  } catch (e) {
    // 404(없음) / 소유자 불일치 등 — 빈 상태/에러 분기
    const status = e.response?.status
    error.value =
      status === 404
        ? '앨범을 찾을 수 없거나 접근 권한이 없어요.'
        : e.response?.data?.message ?? e.message ?? '앨범을 불러오지 못했어요.'
    album.value = null
    photos.value = []
  } finally {
    loading.value = false
  }
}

function onAddClick() {
  if (uploading.value) return
  fileInput.value?.click()
}

async function onFileSelected(e) {
  const file = e.target.files?.[0]
  // input 재선택을 위해 값 초기화
  e.target.value = ''
  if (!file) return

  uploading.value = true
  try {
    // 1) 파일 업로드 → { imageUrl }
    const { data: up } = await albumApi.uploadImage(file)
    const imageUrl = up?.imageUrl
    if (!imageUrl) throw new Error('업로드 응답에 imageUrl 이 없어요.')
    // 2) 앨범에 사진 연결 후 최신 상태 반영
    const { data: updated } = await albumApi.addPhoto(route.params.id, album.value?.name, imageUrl)
    album.value = updated
    photos.value = updated?.photos ?? []
  } catch (err) {
    error.value = err.response?.data?.message ?? err.message ?? '사진 추가에 실패했어요.'
  } finally {
    uploading.value = false
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  } catch {
    // 클립보드 API 미지원/거부 — 조용히 무시(가짜 성공 표시 안 함)
  }
}

function flashShareMsg(text, isErr) {
  shareMsg.value = text
  shareErr.value = isErr
  if (shareMsgTimer) clearTimeout(shareMsgTimer)
  shareMsgTimer = setTimeout(() => { shareMsg.value = '' }, 2500)
}

// 공유: 실제 토큰을 발급받아 공개 링크를 클립보드에 복사한다.
// 공개 뷰 라우트(/album/shared/:token)는 아직 없으므로 토큰 포함 링크 복사까지만 수행한다
// (가짜 동작 금지 — 토큰 발급/복사 실패는 그대로 노출).
async function onShare() {
  if (sharing.value || !album.value) return
  sharing.value = true
  try {
    const { data } = await centralAlbumApi.share(album.value.id)
    // 응답 필드명이 스트림별로 다를 수 있어 방어적으로 토큰을 추출한다.
    const token = data?.token ?? data?.shareToken
    const shareUrl = data?.shareUrl
      ? (data.shareUrl.startsWith('http') ? data.shareUrl : `${location.origin}${data.shareUrl}`)
      : (token ? `${location.origin}/album/shared/${token}` : null)
    if (!shareUrl) throw new Error('공유 응답에 토큰이 없어요.')

    try {
      await navigator.clipboard.writeText(shareUrl)
      flashShareMsg('링크 복사됨', false)
    } catch {
      // 클립보드 거부/미지원 — 토큰은 발급됐으나 자동 복사는 실패. 정직하게 안내.
      flashShareMsg('링크가 생성됐지만 복사에 실패했어요. 직접 복사해 주세요.', true)
    }
  } catch (err) {
    flashShareMsg(err.response?.data?.message ?? err.message ?? '공유 링크 생성에 실패했어요.', true)
  } finally {
    sharing.value = false
  }
}

onMounted(loadAlbum)
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
.nav-spacer { width: 40px; height: 40px; flex-shrink: 0; }
.share-btn {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
  background: none;
  border: none;
  cursor: pointer;
}
.share-btn.disabled { opacity: 0.5; cursor: default; }
.share-btn:disabled { opacity: 0.5; cursor: default; }
.share-spinner-label { font-size: 18px; color: var(--color-ink-muted); line-height: 1; }
.nav-title-col { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.nav-title { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.nav-sub { font-size: 11.5px; color: var(--color-ink-muted); }

/* 로딩/에러 상태 */
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
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
}
.state-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}
.state-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
}
.state-hint {
  font-size: 13.5px;
  color: var(--color-ink-muted);
}

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
.photo-add-cell.disabled { opacity: 0.6; cursor: default; }
.add-label { font-size: 11px; color: var(--color-ink-muted); }

.photo-cell {
  aspect-ratio: 1;
  background: linear-gradient(145deg, #e8ddd4, #d8cdc4);
  overflow: hidden;
}
.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.photo-empty-cell {
  grid-column: span 2;
  aspect-ratio: 2 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-hint { font-size: 12px; color: var(--color-ink-muted); }

.share-msg {
  flex-shrink: 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--color-ink-muted);
  padding: 6px 20px 0;
}
.share-msg.err { color: var(--color-error); }

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
</style>
