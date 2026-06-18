# Created: 2026-06-16 13:26:10
<template>
  <div class="page">
    <header class="home-header">
      <div class="location-row">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2.2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
          <circle cx="12" cy="10" r="3" />
        </svg>
        <span class="location-text">현재 위치 · 제주특별자치도</span>
      </div>
      <h1 class="home-title">오늘은 어디로<br />떠나볼까요?</h1>
      <div class="header-right">
        <button class="icon-btn" @click="$router.push('/mypage')">
          <div class="profile-avatar" />
        </button>
      </div>
    </header>

    <div class="scroll-content">
      <div class="search-bar" @click="$router.push('/explore')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>
        <span class="search-placeholder">여행지, 맛집, 테마를 검색하세요</span>
      </div>

      <div class="category-row">
        <button v-for="cat in categories" :key="cat.label" class="category-chip" @click="$router.push('/explore')">
          <span class="cat-icon" v-html="cat.icon" />
          <span class="cat-label">{{ cat.label }}</span>
        </button>
      </div>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">지금 뜨는 여행지</h2>
          <button class="see-all" @click="$router.push('/explore')">전체보기</button>
        </div>
        <div class="horizontal-scroll">
          <PlaceCard v-for="place in places" :key="place.id" :place="place" :rank="place.rank" @click="$router.push(`/place/${place.id}`)" @bookmark="toggleBookmark(place.id)" />
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

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import PlaceCard from '@/components/common/PlaceCard.vue'
import PostCard from '@/components/community/PostCard.vue'
import { usePlacesStore } from '@/stores/places.js'
import { usePostsStore } from '@/stores/posts.js'

const placesStore = usePlacesStore()
const postsStore = usePostsStore()

const places = computed(() => placesStore.places.slice(0, 5))
const posts = computed(() => postsStore.posts)

const categories = [
  {
    label: '맛집',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8h1a4 4 0 010 8h-1"/><path d="M2 8h16v9a4 4 0 01-4 4H6a4 4 0 01-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>`,
  },
  {
    label: '문화·역사',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18"/><path d="M5 21V7l7-4 7 4v14"/><path d="M9 9h1v3H9zM14 9h1v3h-1zM9 15h1v6H9zM14 15h1v6h-1z"/></svg>`,
  },
  {
    label: '액티비티',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg>`,
  },
  {
    label: '야경',
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>`,
  },
]

function toggleBookmark(id) {
  const place = placesStore.places.find((p) => p.id === id)
  if (place) place.bookmarked = !place.bookmarked
}

onMounted(async () => {
  await Promise.all([placesStore.fetchPlaces(), postsStore.fetchPosts(true)])
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
  padding: 0 20px;
  overflow-x: auto;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--color-line-light);
}

.bottom-spacer {
  height: 24px;
}
</style>
