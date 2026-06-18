# Created: 2026-06-18 12:42:02
<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchPosts } from '../api/community.js';

const router = useRouter();

const TABS = [
  { label: '전체', category: null },
  { label: '후기', category: 'REVIEW' },
  { label: '질문', category: 'QUESTION' },
  { label: '꿀팁', category: 'TIP' },
  { label: '맛집', category: 'RESTAURANT' },
  { label: '동행구함', category: 'COMPANION' },
];

const CATEGORY_LABEL = {
  REVIEW: '후기',
  QUESTION: '질문',
  TIP: '꿀팁',
  RESTAURANT: '맛집',
  COMPANION: '동행구함',
};

const activeTab = ref(0);
const posts = ref([]);
const loading = ref(false);
const hasNext = ref(false);
const nextCursor = ref(null);

async function loadPosts(reset = false) {
  if (loading.value) return;
  loading.value = true;
  if (reset) {
    posts.value = [];
    nextCursor.value = null;
  }
  try {
    const data = await fetchPosts({
      category: TABS[activeTab.value].category,
      cursor: nextCursor.value,
      size: 20,
    });
    posts.value = reset ? data.content : [...posts.value, ...data.content];
    hasNext.value = data.hasNext;
    nextCursor.value = data.nextCursor;
  } catch (e) {
    console.error('게시글 로드 실패', e);
  } finally {
    loading.value = false;
  }
}

watch(activeTab, () => loadPosts(true));
onMounted(() => loadPosts(true));

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return '방금 전';
  if (m < 60) return `${m}분 전`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}시간 전`;
  const d = Math.floor(h / 24);
  if (d < 7) return `${d}일 전`;
  return new Date(dateStr).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
}

function avatarChar(nickname) {
  return nickname ? nickname[0] : '?';
}

const hotPlaces = [
  { id: 1, badge: 'HOT', name: '오션뷰 루프탑 카페', sub: '해운대 · 조회 2,340', icon: 'location' },
  { id: 2, badge: 'NEW', name: '감성 돼지국밥 골목', sub: '서면 · 조회 1,890', icon: 'food' },
  { id: 3, badge: 'HOT', name: '광안리 야경 스팟', sub: '수영구 · 조회 1,540', icon: 'flag' },
];
</script>

<template>
  <div class="community">
    <header class="nav-bar">
      <div class="nav-logo">커뮤니티</div>
      <button class="nav-btn" aria-label="검색">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
      </button>
      <button class="nav-btn" aria-label="필터">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="6" x2="20" y2="6"/><line x1="8" y1="12" x2="16" y2="12"/><line x1="11" y1="18" x2="13" y2="18"/></svg>
      </button>
    </header>

    <div class="cat-tabs">
      <div
        v-for="(tab, i) in TABS"
        :key="i"
        class="cat-tab"
        :class="{ active: activeTab === i }"
        @click="activeTab = i"
      >
        {{ tab.label }}
      </div>
    </div>

    <div class="content-wrap">
      <div class="hot-banner">
        <div class="hot-label">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="var(--color-primary-500)" stroke="none"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
          이번 주 핫플
        </div>
        <div class="hot-scroll">
          <div v-for="place in hotPlaces" :key="place.id" class="hot-card">
            <div class="hot-card-img">
              <svg v-if="place.icon === 'location'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
              <svg v-else-if="place.icon === 'food'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/></svg>
              <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/></svg>
            </div>
            <div class="hot-card-body">
              <div class="hot-card-badge">{{ place.badge }}</div>
              <div class="hot-card-name">{{ place.name }}</div>
              <div class="hot-card-sub">{{ place.sub }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="post-section">
        <div class="section-header">
          <div class="section-title">최신 글</div>
          <div class="sort-btn">
            최신순
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
        </div>
      </div>

      <!-- 로딩 (최초) -->
      <div v-if="loading && posts.length === 0" class="empty-state">
        <div class="loading-spinner"></div>
      </div>

      <!-- 게시글 없음 -->
      <div v-else-if="!loading && posts.length === 0" class="empty-state">
        <p class="empty-text">아직 게시글이 없습니다.<br>첫 번째 글을 작성해보세요!</p>
      </div>

      <div v-else class="post-list">
        <div v-for="post in posts" :key="post.id" class="post-item">
          <div class="post-header">
            <div class="post-avatar">{{ avatarChar(post.authorNickname) }}</div>
            <div>
              <div class="post-user">{{ post.authorNickname }}</div>
              <div class="post-meta">{{ timeAgo(post.createdAt) }}</div>
            </div>
            <div class="post-region">{{ CATEGORY_LABEL[post.category] }}</div>
          </div>
          <div class="post-title">{{ post.title }}</div>
          <div v-if="post.thumbnailUrl" class="post-photos">
            <div class="post-photo">
              <img :src="post.thumbnailUrl" alt="썸네일" class="post-thumb" />
            </div>
          </div>
          <div class="post-footer">
            <div class="post-action">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              {{ post.likeCount }}
            </div>
            <div class="post-action">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              {{ post.commentCount }}
            </div>
            <div class="post-action">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              {{ post.viewCount }}
            </div>
          </div>
        </div>
      </div>

      <!-- 더 보기 -->
      <div v-if="hasNext" class="load-more-wrap">
        <button class="load-more-btn" :disabled="loading" @click="loadPosts(false)">
          {{ loading ? '불러오는 중...' : '더 보기' }}
        </button>
      </div>
    </div>

    <div class="fab" @click="router.push('/companion')">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
    </div>
  </div>
</template>

<style scoped>
.community { background: var(--surface-subtle); }

.nav-bar { height: 52px; display: flex; align-items: center; padding: 0 20px; gap: 8px; background: #fff; }
.nav-logo { font: var(--weight-extrabold) 22px/1 var(--font-sans); color: var(--color-primary-500); letter-spacing: -0.03em; flex: 1; }
.nav-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-primary); cursor: pointer; }

.cat-tabs { display: flex; background: #fff; border-bottom: 1px solid var(--border-subtle); overflow-x: auto; scrollbar-width: none; padding: 0 20px; }
.cat-tabs::-webkit-scrollbar { display: none; }
.cat-tab { flex-shrink: 0; padding: 13px 14px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); border-bottom: 2.5px solid transparent; cursor: pointer; white-space: nowrap; }
.cat-tab.active { color: var(--color-primary-500); border-bottom-color: var(--color-primary-500); font-weight: var(--weight-semibold); }

.content-wrap { padding-bottom: 100px; }

.hot-banner { margin: 14px 20px 0; }
.hot-label { display: flex; align-items: center; gap: 6px; font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 10px; }
.hot-scroll { display: flex; gap: 10px; overflow-x: auto; scrollbar-width: none; }
.hot-scroll::-webkit-scrollbar { display: none; }
.hot-card { flex-shrink: 0; width: 140px; border-radius: var(--radius-lg); overflow: hidden; background: var(--surface-bg); box-shadow: var(--shadow-sm); cursor: pointer; }
.hot-card-img { width: 100%; height: 90px; background: linear-gradient(135deg, var(--color-primary-100), var(--color-primary-50)); display: flex; align-items: center; justify-content: center; color: var(--color-primary-300); }
.hot-card-body { padding: 8px 10px; }
.hot-card-badge { display: inline-block; background: var(--color-primary-500); color: #fff; font: var(--weight-bold) 9px/1 var(--font-sans); padding: 2px 6px; border-radius: var(--radius-full); margin-bottom: 4px; }
.hot-card-name { font: var(--weight-semibold) var(--text-xs)/var(--leading-snug) var(--font-sans); color: var(--text-primary); }
.hot-card-sub { font: 10px/1 var(--font-sans); color: var(--text-tertiary); margin-top: 3px; }

.post-section { padding: 20px 20px 0; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font: var(--weight-bold) var(--text-lg)/1 var(--font-sans); color: var(--text-primary); }
.sort-btn { display: flex; align-items: center; gap: 4px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); }

.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; }
.empty-text { text-align: center; font: var(--type-body-sm); color: var(--text-tertiary); line-height: var(--leading-normal); }
.loading-spinner { width: 32px; height: 32px; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary-500); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.post-list { display: flex; flex-direction: column; gap: 1px; background: var(--border-subtle); }
.post-item { background: var(--surface-bg); padding: 16px 20px; cursor: pointer; }
.post-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.post-avatar { width: 32px; height: 32px; border-radius: var(--radius-full); background: var(--color-primary-100); display: flex; align-items: center; justify-content: center; font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-600); flex-shrink: 0; }
.post-user { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.post-meta { font: var(--type-caption); color: var(--text-tertiary); margin-top: 2px; }
.post-region { margin-left: auto; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--color-primary-500); background: var(--color-primary-50); padding: 3px 8px; border-radius: var(--radius-full); }
.post-title { font: var(--weight-bold) var(--text-base)/var(--leading-snug) var(--font-sans); color: var(--text-primary); margin-bottom: 10px; }
.post-photos { display: flex; gap: 6px; margin-bottom: 10px; }
.post-photo { width: 72px; height: 60px; border-radius: var(--radius-sm); overflow: hidden; background: var(--color-neutral-100); }
.post-thumb { width: 100%; height: 100%; object-fit: cover; }
.post-footer { display: flex; align-items: center; gap: 14px; }
.post-action { display: flex; align-items: center; gap: 4px; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); }

.load-more-wrap { display: flex; justify-content: center; padding: 20px; }
.load-more-btn { padding: 10px 28px; border: 1px solid var(--border-subtle); border-radius: var(--radius-full); font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); background: var(--surface-bg); cursor: pointer; }
.load-more-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.fab {
  position: fixed;
  bottom: 24px;
  right: max(20px, calc(50% - 195px));
  width: 52px;
  height: 52px;
  background: var(--color-primary-500);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 16px rgba(247,143,87,.4);
  cursor: pointer;
}
</style>
