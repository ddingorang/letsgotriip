# Created: 2026-06-19
<template>
  <div class="page">
    <!-- ── Top bar: 검색바 ───────────────────────────────────────────────── -->
    <div class="top-bar">
      <button class="back-btn" @click="$router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <div class="search-bar">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>
        <input
          ref="searchInput"
          v-model="query"
          class="search-input"
          placeholder="관광지·게시글·동행·축제 검색"
          @input="onInput"
        />
        <button v-if="query" class="clear-btn" @click="clearQuery">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>

    <!-- ── Type chips ────────────────────────────────────────────────────── -->
    <div class="type-scroll">
      <button
        v-for="t in TYPES"
        :key="t.key"
        class="type-pill"
        :class="{ active: activeType === t.key }"
        @click="selectType(t.key)"
      >
        {{ t.label }}
      </button>
    </div>

    <!-- ── Results ───────────────────────────────────────────────────────── -->
    <div class="results">
      <!-- Loading -->
      <div v-if="loading" class="status-block">
        <div class="spinner" />
      </div>

      <!-- Error -->
      <div v-else-if="error" class="status-block">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span class="status-text">검색 중 오류가 발생했어요.</span>
        <button class="retry-btn" @click="runSearch">재시도</button>
      </div>

      <!-- Idle (no query yet) -->
      <div v-else-if="!hasQuery" class="status-block">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5">
          <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
        </svg>
        <span class="status-text">검색어를 입력해 주세요.</span>
      </div>

      <!-- Empty -->
      <div v-else-if="isEmpty" class="status-block">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5">
          <circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" />
        </svg>
        <span class="status-text">'{{ lastQuery }}'에 대한 결과가 없습니다.</span>
      </div>

      <!-- Result groups -->
      <template v-else>
        <!-- 관광지 -->
        <section v-if="attractions.length" class="group">
          <h2 class="group-title">
            관광지<span class="group-count">{{ attractions.length }}</span>
          </h2>
          <div
            v-for="item in attractions"
            :key="`attr-${item.contentId}`"
            class="result-row"
            @click="$router.push(`/place/${item.contentId}`)"
          >
            <div class="thumb">
              <img
                v-if="item.firstimage"
                :src="item.firstimage"
                :alt="item.title"
                class="thumb-img"
                @error="(e) => (e.target.style.display = 'none')"
              />
            </div>
            <div class="row-info">
              <div class="row-title">{{ item.title }}</div>
              <div v-if="item.addr1" class="row-sub">{{ item.addr1 }}</div>
            </div>
            <svg class="chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </section>

        <!-- 게시글 -->
        <section v-if="posts.length" class="group">
          <h2 class="group-title">
            게시글<span class="group-count">{{ posts.length }}</span>
          </h2>
          <div
            v-for="item in posts"
            :key="`post-${item.type}-${item.id}`"
            class="result-row"
            @click="goPost(item)"
          >
            <div class="thumb thumb-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" />
              </svg>
            </div>
            <div class="row-info">
              <div class="row-title">{{ item.title }}</div>
              <div v-if="item.category" class="row-sub">{{ item.category }}</div>
            </div>
            <svg class="chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </section>

        <!-- 동행 -->
        <section v-if="companions.length" class="group">
          <h2 class="group-title">
            동행<span class="group-count">{{ companions.length }}</span>
          </h2>
          <div
            v-for="item in companions"
            :key="`comp-${item.id}`"
            class="result-row"
            @click="$router.push(`/companion/${item.id}`)"
          >
            <div class="thumb thumb-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
              </svg>
            </div>
            <div class="row-info">
              <div class="row-title">{{ item.title }}</div>
              <div class="row-sub">
                <span v-if="item.region">{{ item.region }}</span>
                <span v-if="item.region && item.travelDate"> · </span>
                <span v-if="item.travelDate">{{ formatDate(item.travelDate) }}</span>
              </div>
            </div>
            <svg class="chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6" /></svg>
          </div>
        </section>

        <!-- 축제 -->
        <section v-if="festivals.length" class="group">
          <h2 class="group-title">
            축제<span class="group-count">{{ festivals.length }}</span>
          </h2>
          <div
            v-for="item in festivals"
            :key="`fest-${item.contentId}`"
            class="result-row result-row-static"
          >
            <div class="thumb">
              <img
                v-if="item.imageUrl"
                :src="item.imageUrl"
                :alt="item.title"
                class="thumb-img"
                @error="(e) => (e.target.style.display = 'none')"
              />
            </div>
            <div class="row-info">
              <div class="row-title">{{ item.title }}</div>
              <div class="row-sub">
                <span v-if="item.address">{{ item.address }}</span>
              </div>
              <div v-if="item.startDate" class="row-date">
                {{ formatDate(item.startDate) }}<span v-if="item.endDate"> ~ {{ formatDate(item.endDate) }}</span>
              </div>
            </div>
          </div>
        </section>
      </template>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { searchApi } from '@/api/index.js'

const router = useRouter()

// ── Search type tabs ───────────────────────────────────────────────────────
const TYPES = [
  { key: 'all', label: '전체' },
  { key: 'attraction', label: '관광지' },
  { key: 'post', label: '게시글' },
  { key: 'companion', label: '동행' },
  { key: 'festival', label: '축제' },
]

// ── State ──────────────────────────────────────────────────────────────────
const searchInput = ref(null)
const query = ref('')
const activeType = ref('all')
const loading = ref(false)
const error = ref(false)
const lastQuery = ref('')

const attractions = ref([])
const posts = ref([])
const companions = ref([])
const festivals = ref([])

// ── Derived ────────────────────────────────────────────────────────────────
const hasQuery = computed(() => query.value.trim().length >= 2)
const isEmpty = computed(
  () =>
    !loading.value &&
    !error.value &&
    hasQuery.value &&
    !attractions.value.length &&
    !posts.value.length &&
    !companions.value.length &&
    !festivals.value.length,
)

// ── Actions ────────────────────────────────────────────────────────────────
let debounceTimer = null
// 응답 레이스 가드: 매 요청마다 증가시키고, 응답 적용 시 최신 요청만 반영한다.
let requestSeq = 0

function onInput() {
  clearTimeout(debounceTimer)
  if (!hasQuery.value) {
    resetResults()
    return
  }
  debounceTimer = setTimeout(runSearch, 400)
}

function selectType(key) {
  activeType.value = key
  if (hasQuery.value) runSearch()
}

function clearQuery() {
  query.value = ''
  clearTimeout(debounceTimer)
  resetResults()
  searchInput.value?.focus()
}

function resetResults() {
  // 진행 중인 요청의 늦은 응답이 초기화된 상태를 덮어쓰지 못하도록 일련번호를 무효화한다.
  requestSeq++
  attractions.value = []
  posts.value = []
  companions.value = []
  festivals.value = []
  error.value = false
  loading.value = false
  lastQuery.value = ''
}

async function runSearch() {
  const q = query.value.trim()
  if (q.length < 2) {
    resetResults()
    return
  }
  // 이 요청의 일련번호. 응답이 도착했을 때 여전히 최신 요청인 경우에만 결과를 반영한다.
  const seq = ++requestSeq
  loading.value = true
  error.value = false
  lastQuery.value = q
  try {
    const { data } = await searchApi.search(q, activeType.value)
    if (seq !== requestSeq) return // 더 최신 요청이 시작됨 — 오래된 응답은 폐기
    attractions.value = data?.attractions ?? []
    posts.value = data?.posts ?? []
    companions.value = data?.companions ?? []
    festivals.value = data?.festivals ?? []
  } catch {
    if (seq !== requestSeq) return // 오래된 응답의 에러는 무시
    error.value = true
    attractions.value = []
    posts.value = []
    companions.value = []
    festivals.value = []
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

function goPost(item) {
  if (item.type === 'HOTPLACE') {
    return router.push(`/hotplace/${item.id}`)
  }
  return router.push(`/community/${item.id}`)
}

function formatDate(raw) {
  if (!raw) return ''
  const s = String(raw)
  // ISO yyyy-MM-dd
  if (s.includes('-')) return s.slice(0, 10).replace(/-/g, '.')
  // compact yyyyMMdd
  if (s.length >= 8) return `${s.slice(0, 4)}.${s.slice(4, 6)}.${s.slice(6, 8)}`
  return s
}

// ── Bootstrap ──────────────────────────────────────────────────────────────
onMounted(() => {
  searchInput.value?.focus()
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
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

/* ── Top bar ──────────────────────────────────────────────────────────────── */
.top-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 52px 16px 12px;
  background: var(--color-white);
  flex-shrink: 0;
}

.back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
  flex-shrink: 0;
}

.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 11px 16px;
  min-width: 0;
}

.search-input {
  flex: 1;
  font-size: 14px;
  color: var(--color-ink);
  background: transparent;
  letter-spacing: -0.2px;
  min-width: 0;
}

.search-input::placeholder {
  color: var(--color-ink-muted);
}

.clear-btn {
  color: var(--color-ink-muted);
  flex-shrink: 0;
}

/* ── Type chips ───────────────────────────────────────────────────────────── */
.type-scroll {
  display: flex;
  gap: 8px;
  padding: 0 16px 10px;
  overflow-x: auto;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}

.type-pill {
  flex-shrink: 0;
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  letter-spacing: -0.2px;
  transition: all 0.15s;
}

.type-pill.active {
  background: var(--color-peach);
  color: white;
}

/* ── Results ──────────────────────────────────────────────────────────────── */
.results {
  flex: 1;
  overflow-y: auto;
}

/* ── Status (loading / error / idle / empty) ──────────────────────────────── */
.status-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 64px 24px;
  color: var(--color-ink-muted);
}

.status-text {
  font-size: 13.5px;
  letter-spacing: -0.2px;
  text-align: center;
}

.spinner {
  width: 26px;
  height: 26px;
  border: 2.5px solid var(--color-line);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.retry-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
  text-decoration: underline;
}

/* ── Result groups ────────────────────────────────────────────────────────── */
.group {
  padding-top: 4px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 16px 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.group-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
}

.result-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
}

.result-row-static {
  cursor: default;
}

.thumb {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-icon {
  background: var(--color-peach-light);
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.row-info {
  flex: 1;
  min-width: 0;
}

.row-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-sub {
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-date {
  font-size: 12px;
  color: var(--color-ink-secondary);
  margin-top: 3px;
}

.chevron {
  flex-shrink: 0;
}

.bottom-spacer {
  height: 24px;
}
</style>
