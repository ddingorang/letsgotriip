# Created: 2026-06-16 13:26:10
<template>
  <div class="page">
    <div class="scroll-content">
      <header class="home-header">
      <div class="location-row">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2.2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
          <circle cx="12" cy="10" r="3" />
        </svg>
        <span class="location-text">{{ locationLabel }}</span>
      </div>
      <h1 class="home-title">오늘은 어디로<br />떠나볼까요?</h1>
      <div class="header-right">
        <button class="icon-btn" @click="$router.push('/mypage')">
          <img
            v-if="avatarUrl"
            :src="avatarUrl"
            :alt="authStore.user?.nickname ?? '프로필'"
            class="profile-avatar avatar-img"
            @error="onAvatarError"
          />
          <div v-else class="profile-avatar" />
        </button>
      </div>
    </header>

      <div class="search-bar" @click="$router.push('/explore')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>
        <span class="search-placeholder">여행지, 맛집, 테마를 검색하세요</span>
      </div>

      <div class="category-row">
        <button v-for="cat in categories" :key="cat.label" class="category-chip" @click="$router.push({ path: '/explore', query: { tag: cat.tag } })">
          <span class="cat-icon" v-html="cat.icon" />
          <span class="cat-label">{{ cat.label }}</span>
        </button>
      </div>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">지금 뜨는 여행지</h2>
          <button class="see-all" @click="$router.push('/explore')">전체보기</button>
        </div>
        <div class="horizontal-scroll" ref="hScroll" @wheel.prevent="onHScroll">
          <!-- 로딩 중(데이터 도착 전)엔 스켈레톤 — 빈 줄로 "고장난 것처럼" 보이지 않게 -->
          <template v-if="trendingLoading && !places.length">
            <div v-for="n in 3" :key="'sk' + n" class="place-skeleton" />
          </template>
          <template v-else>
            <PlaceCard v-for="place in places" :key="place.id" :place="place" :rank="place.rank" @click="$router.push(`/hotplace/${place.id}`)" />
          </template>
          <p v-if="!trendingLoading && !places.length" class="trending-empty">표시할 여행지를 불러오지 못했어요.</p>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">여행 후기</h2>
          <button class="see-all" @click="$router.push('/community')">전체보기</button>
        </div>
        <div class="post-list">
          <PostCard v-for="post in posts.slice(0, 2)" :key="post.id" :post="post" @click="$router.push(`/community/${post.id}`)" @like="postsStore.likePost($event)" />
        </div>
      </section>

      <!-- ── 여행 뉴스 (데모) ──────────────────────────────────────────────── -->
      <section v-if="newsLoading || news.length" class="section">
        <div class="section-header">
          <h2 class="section-title">
            여행 뉴스
            <span class="demo-badge">데모</span>
          </h2>
        </div>
        <div v-if="newsLoading" class="news-loading">여행 뉴스를 불러오는 중…</div>
        <div v-else class="news-list">
          <a
            v-for="(item, i) in news"
            :key="i"
            class="news-card"
            :href="item.url || undefined"
            :target="item.url ? '_blank' : undefined"
            rel="noopener noreferrer"
          >
            <div class="news-body">
              <p class="news-title">{{ item.title }}</p>
              <p v-if="item.summary" class="news-summary">{{ item.summary }}</p>
              <div class="news-meta">
                <span v-if="item.source" class="news-source">{{ item.source }}</span>
                <span v-if="formatNewsDate(item.publishedAt)" class="news-date">{{ formatNewsDate(item.publishedAt) }}</span>
              </div>
            </div>
            <svg v-if="item.url" class="news-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </a>
        </div>
      </section>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed, ref } from 'vue'
import PlaceCard from '@/components/common/PlaceCard.vue'
import PostCard from '@/components/community/PostCard.vue'
import { useAttractionStore } from '@/stores/attraction.js'
import { usePostsStore } from '@/stores/posts.js'
import { useAuthStore } from '@/stores/auth.js'
import { useLocationStore } from '@/stores/location.js'
import { contextApi, hotplaceApi } from '@/api/index.js'

const attractionStore = useAttractionStore()
const postsStore = usePostsStore()
const authStore = useAuthStore()
const locationStore = useLocationStore()

// 지금 뜨는 여행지 — 핫플(게시글) 중 좋아요 높은 순(진짜 추천순).
const trending = ref([])
const trendingLoading = ref(false)
const places = computed(() => trending.value)

async function loadTrending() {
  trendingLoading.value = true
  try {
    const { data } = await hotplaceApi.popular({ size: 10 })
    const content = Array.isArray(data) ? data : (data?.content ?? [])
    trending.value = content.map((h, i) => ({
      id: h.id,
      name: h.name,
      address: h.address,
      imageUrl: h.thumbnailUrl,
      category: h.category,
      likeCount: h.likeCount,
      rank: i + 1,
    }))
  } catch {
    trending.value = []
  } finally {
    trendingLoading.value = false
  }
}
const posts = computed(() => postsStore.posts)
const locationLabel = ref('현재 위치 · 제주특별자치도')

// ── 여행 뉴스 (데모) ─────────────────────────────────────────────────────────
const news = ref([])
const newsLoading = ref(false)

// "2026-06-19..." → "6월 19일"
function formatNewsDate(raw) {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

async function loadNews() {
  newsLoading.value = true
  try {
    const { data } = await contextApi.news()
    news.value = Array.isArray(data) ? data.slice(0, 6) : []
  } catch {
    news.value = []
  } finally {
    newsLoading.value = false
  }
}

// 업로드한 프로필만 표시 — BE 기본값(/images/default-profile.png)은 실제 파일이 없어 깨지므로 placeholder 처리
const hScroll = ref(null)
const onHScroll = (e) => { hScroll.value.scrollLeft += e.deltaY }

const avatarBroken = ref(false)
const avatarUrl = computed(() => {
  if (avatarBroken.value) return null
  const u = authStore.user?.profileImageUrl
  return u && !u.includes('default-profile') ? u : null
})
function onAvatarError() {
  avatarBroken.value = true
}

const categories = [
  {
    label: '맛집',
    tag: 'food',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8h1a4 4 0 010 8h-1"/><path d="M2 8h16v9a4 4 0 01-4 4H6a4 4 0 01-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>`,
  },
  {
    label: '문화·역사',
    tag: 'culture',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18"/><path d="M5 21V7l7-4 7 4v14"/><path d="M9 9h1v3H9zM14 9h1v3h-1zM9 15h1v6H9zM14 15h1v6h-1z"/></svg>`,
  },
  {
    label: '액티비티',
    tag: 'activity',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg>`,
  },
  {
    label: '야경',
    tag: 'night',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>`,
  },
]

function toggleBookmark(id) {
  const place = attractionStore.attractions.find((p) => p.id === id)
  if (place) place.bookmarked = !place.bookmarked
}

// 헤더 위치 라벨만 갱신(트렌딩 데이터는 핫플 인기순 loadTrending 이 담당).
function loadJejuPopular() {
  locationLabel.value = '추천 · 제주특별자치도'
}
function loadNearbyAttractions() {
  locationStore.ensureLocation().then((coords) => {
    locationLabel.value = coords ? '현재 위치 · 내 주변' : '추천 · 제주특별자치도'
  })
}

onMounted(() => {
  postsStore.fetchPosts(true)
  loadTrending()          // 지금 뜨는 여행지 = 인기 핫플(좋아요순)
  loadNearbyAttractions() // 위치 라벨용(현재 위치/제주)
  loadNews()
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

.scroll-content {
  flex: 1;
  overflow-y: auto;
}

.home-header {
  padding: 52px 20px 20px;
  background: var(--color-white);
  position: relative;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 8px;
}

.location-text {
  font-size: 12px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.home-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.7px;
  line-height: 1.2;
}

.header-right {
  position: absolute;
  top: 52px;
  right: 20px;
}

.profile-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
  overflow: hidden;
}

.avatar-img {
  object-fit: cover;
  display: block;
}

.search-bar {
  margin: 0 20px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 13px 18px;
  cursor: pointer;
}

.search-placeholder {
  font-size: 14px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.category-row {
  display: flex;
  gap: 10px;
  padding: 0 20px 24px;
  overflow-x: auto;
}

.category-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  width: 64px;
}

.cat-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cat-label {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
  letter-spacing: -0.2px;
}

.section {
  margin-bottom: 28px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 14px;
}

.section-title {
  font-size: 17px;
  font-weight: 750;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}

.see-all {
  font-size: 13px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.horizontal-scroll {
  display: flex;
  gap: 12px;
  padding: 0 20px 8px;
  overflow-x: auto;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--color-line-light);
}

/* ── 여행 뉴스 (데모) ─────────────────────────────────────────────────────── */
.demo-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-line);
  border-radius: var(--radius-full);
  padding: 1px 7px;
  margin-left: 6px;
  vertical-align: middle;
}

.place-skeleton {
  flex-shrink: 0;
  width: 150px;
  height: 190px;
  border-radius: var(--radius-lg, 16px);
  background: var(--color-surface);
  animation: home-shimmer 1.2s infinite ease-in-out;
}
@keyframes home-shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}
.trending-empty {
  font-size: 13px;
  color: var(--color-ink-muted);
  padding: 24px 4px;
}

.news-loading {
  padding: 0 20px;
  font-size: 12.5px;
  color: var(--color-ink-muted);
}

.news-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 20px;
}

.news-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}

.news-body {
  flex: 1;
  min-width: 0;
}

.news-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  line-height: 1.4;
  margin-bottom: 4px;
}

.news-summary {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
  line-height: 1.5;
  letter-spacing: -0.2px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}

.news-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.news-source {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-peach-pressed);
}

.news-date {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}

.news-arrow {
  flex-shrink: 0;
}

.bottom-spacer {
  height: 24px;
}
</style>
