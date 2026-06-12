<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http.js';

const router = useRouter();

const categories = [
  { key: 'stay',      label: '숙소',   active: true },
  { key: 'transport', label: '교통' },
  { key: 'activity',  label: '액티비티' },
  { key: 'package',   label: '패키지' },
  { key: 'guide',     label: '가이드' },
  { key: 'insurance', label: '보험' },
];

const recentSearches = [
  { main: '서울 → 부산', sub: '06/15 · 성인 2명' },
  { main: '제주도 숙소',  sub: '07/01 · 2박' },
];

// ── Recommended attractions (부산 관광지 top-6) ──────────────────────────────
const recommended = ref([]);
const recLoading = ref(false);

// ── Festivals ────────────────────────────────────────────────────────────────
const festivals = ref([]);
const festLoading = ref(false);

onMounted(async () => {
  // Fetch in parallel
  recLoading.value = true;
  festLoading.value = true;

  const [recRes, festRes] = await Promise.allSettled([
    http.get('/api/attractions', { params: { areaCode: 6, contentTypeId: 12, size: 6 } }),
    http.get('/api/festivals'),
  ]);

  recLoading.value = false;
  festLoading.value = false;

  if (recRes.status === 'fulfilled') {
    recommended.value = Array.isArray(recRes.value.data) ? recRes.value.data.slice(0, 6) : [];
  }
  if (festRes.status === 'fulfilled') {
    festivals.value = Array.isArray(festRes.value.data) ? festRes.value.data.slice(0, 6) : [];
  }
});

function festStatusLabel(f) {
  if (f.status === 'ONGOING') return '진행중';
  if (f.status === 'UPCOMING') return '예정';
  return '';
}
</script>

<template>
  <div class="home">
    <header class="nav-bar">
      <div class="nav-logo">관통여행</div>
      <div class="nav-actions">
        <button class="icon-btn" aria-label="알림">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
        </button>
        <button class="icon-btn" aria-label="마이페이지" @click="router.push('/mypage')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </button>
      </div>
    </header>

    <div class="search-wrap">
      <div class="search-bar" role="button" @click="router.push('/search')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        <span class="search-text">여행지, 숙소, 교통편 검색</span>
      </div>
    </div>

    <div class="banner" @click="router.push('/ai')">
      <div class="banner-label">이번 주 특가</div>
      <div class="banner-title">지금 떠나는<br>여름 여행</div>
      <div class="banner-btn">
        특가 보기
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </div>
      <div class="banner-dots">
        <div class="banner-dot active"></div>
        <div class="banner-dot"></div>
        <div class="banner-dot"></div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <div class="section-title">카테고리</div>
      </div>
    </div>
    <div class="cat-scroll">
      <div v-for="cat in categories" :key="cat.key" class="cat-item">
        <div class="cat-icon" :class="cat.active ? 'active' : 'default'">
          <svg v-if="cat.key === 'stay'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/></svg>
          <svg v-else-if="cat.key === 'transport'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
          <svg v-else-if="cat.key === 'activity'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg>
          <svg v-else-if="cat.key === 'package'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 7H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
          <svg v-else-if="cat.key === 'guide'" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          <svg v-else width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        </div>
        <span class="cat-label" :class="{ active: cat.active }">{{ cat.label }}</span>
      </div>
    </div>

    <!-- ── 추천 여행지 ─────────────────────────────────────────────────────── -->
    <div class="section">
      <div class="section-header">
        <div class="section-title">추천 여행지</div>
        <span class="section-more" role="button" @click="router.push('/search')">전체보기 ›</span>
      </div>
    </div>
    <div class="card-scroll">
      <!-- Loading skeletons -->
      <template v-if="recLoading">
        <div v-for="n in 3" :key="n" class="travel-card skeleton-card">
          <div class="travel-card-img skeleton-box"></div>
          <div class="travel-card-body">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line mid"></div>
          </div>
        </div>
      </template>

      <!-- Real data -->
      <template v-else-if="recommended.length">
        <div
          v-for="item in recommended"
          :key="item.contentId"
          class="travel-card"
          @click="router.push(`/detail/${item.contentId}`)"
        >
          <div class="travel-card-img">
            <img v-if="item.firstimage" :src="item.firstimage" :alt="item.title" class="card-img" />
            <svg v-else width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-neutral-400)" stroke-width="1.5"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
          </div>
          <div class="travel-card-body">
            <div class="travel-card-badge">관광</div>
            <div class="travel-card-title">{{ item.title }}</div>
            <div class="travel-card-sub">{{ item.addr1 }}</div>
          </div>
        </div>
      </template>

      <!-- Fallback (API unavailable) -->
      <template v-else>
        <div class="travel-card" @click="router.push('/search')">
          <div class="travel-card-img">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-neutral-400)" stroke-width="1.5"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
          </div>
          <div class="travel-card-body">
            <div class="travel-card-badge">인기</div>
            <div class="travel-card-title">부산 해운대</div>
            <div class="travel-card-sub">해운대구</div>
          </div>
        </div>
      </template>
    </div>

    <!-- ── 진행 중인 축제 ──────────────────────────────────────────────────── -->
    <template v-if="festLoading || festivals.length > 0">
      <div class="section">
        <div class="section-header">
          <div class="section-title">진행 중인 축제</div>
          <span class="section-more" role="button" @click="router.push('/search')">전체보기 ›</span>
        </div>
      </div>
      <div class="card-scroll">
        <!-- Loading skeletons -->
        <template v-if="festLoading">
          <div v-for="n in 3" :key="n" class="travel-card skeleton-card">
            <div class="travel-card-img skeleton-box"></div>
            <div class="travel-card-body">
              <div class="skeleton-line short"></div>
              <div class="skeleton-line"></div>
              <div class="skeleton-line mid"></div>
            </div>
          </div>
        </template>

        <template v-else>
          <div
            v-for="fest in festivals"
            :key="fest.contentId"
            class="travel-card"
            @click="router.push(`/detail/${fest.contentId}`)"
          >
            <div class="travel-card-img">
              <img v-if="fest.imageUrl" :src="fest.imageUrl" :alt="fest.title" class="card-img" />
              <svg v-else width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-neutral-400)" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            </div>
            <div class="travel-card-body">
              <div class="travel-card-badge fest-badge">{{ festStatusLabel(fest) || '축제' }}</div>
              <div class="travel-card-title">{{ fest.title }}</div>
              <div class="travel-card-sub">{{ fest.address }}</div>
            </div>
          </div>
        </template>
      </div>
    </template>

    <!-- ── 최근 검색 ───────────────────────────────────────────────────────── -->
    <div class="section">
      <div class="section-header">
        <div class="section-title">최근 검색</div>
        <span class="section-more">전체삭제</span>
      </div>
    </div>
    <div class="recent-list">
      <div v-for="item in recentSearches" :key="item.main" class="recent-item">
        <div class="recent-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        </div>
        <div class="recent-text">
          <div class="recent-main">{{ item.main }}</div>
          <div class="recent-sub">{{ item.sub }}</div>
        </div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home { background: var(--surface-subtle); }

.nav-bar { height: var(--nav-height); display: flex; align-items: center; justify-content: space-between; padding: 0 20px; background: #fff; }
.nav-logo { font: var(--weight-extrabold) 22px/1 var(--font-sans); color: var(--color-primary-500); letter-spacing: -0.03em; }
.nav-actions { display: flex; gap: 4px; }
.icon-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-primary); border-radius: var(--radius-sm); }

.search-wrap { padding: 14px 20px; background: #fff; border-bottom: 1px solid var(--border-subtle); }
.search-bar { display: flex; align-items: center; gap: 10px; background: var(--surface-subtle); border-radius: var(--radius-md); padding: 12px 14px; cursor: text; }
.search-text { font: var(--type-body); color: var(--text-tertiary); }

.banner { margin: 14px 20px; border-radius: var(--radius-xl); overflow: hidden; background: linear-gradient(135deg, #FF6200 0%, #FF9A00 100%); padding: 24px; position: relative; height: 150px; cursor: pointer; }
.banner-label { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: rgba(255,255,255,0.8); margin-bottom: 8px; }
.banner-title { font: var(--weight-bold) var(--text-2xl)/var(--leading-tight) var(--font-sans); color: #fff; margin-bottom: 14px; letter-spacing: -0.02em; }
.banner-btn { display: inline-flex; align-items: center; gap: 6px; background: #fff; color: var(--color-primary-500); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); padding: 9px 14px; border-radius: var(--radius-full); }
.banner-dots { position: absolute; bottom: 14px; right: 16px; display: flex; gap: 5px; }
.banner-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.4); }
.banner-dot.active { background: #fff; width: 18px; border-radius: 3px; }

.section { padding: 20px 20px 0; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.section-title { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: var(--text-primary); letter-spacing: -0.02em; }
.section-more { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); cursor: pointer; }

.cat-scroll { display: flex; gap: 10px; overflow-x: auto; padding: 0 20px 14px; scrollbar-width: none; }
.cat-scroll::-webkit-scrollbar { display: none; }
.cat-item { display: flex; flex-direction: column; align-items: center; gap: 8px; flex-shrink: 0; cursor: pointer; }
.cat-icon { width: 56px; height: 56px; border-radius: var(--radius-xl); display: flex; align-items: center; justify-content: center; }
.cat-icon.active { background: var(--color-primary-500); color: #fff; }
.cat-icon.default { background: var(--surface-card); color: var(--text-secondary); box-shadow: var(--shadow-xs); }
.cat-label { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); white-space: nowrap; }
.cat-label.active { color: var(--color-primary-500); font-weight: var(--weight-semibold); }

.card-scroll { display: flex; gap: 12px; overflow-x: auto; padding: 0 20px 20px; scrollbar-width: none; }
.card-scroll::-webkit-scrollbar { display: none; }
.travel-card { flex-shrink: 0; width: 180px; background: var(--surface-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; cursor: pointer; }
.travel-card-img { width: 100%; height: 118px; background: linear-gradient(135deg, var(--color-neutral-200), var(--color-neutral-100)); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.card-img { width: 100%; height: 100%; object-fit: cover; }
.travel-card-body { padding: 11px; }
.travel-card-badge { display: inline-block; font: var(--weight-semibold) 10px/1 var(--font-sans); background: var(--color-primary-50); color: var(--color-primary-600); padding: 3px 7px; border-radius: var(--radius-full); margin-bottom: 6px; }
.fest-badge { background: var(--color-primary-500); color: #fff; }
.travel-card-title { font: var(--weight-semibold) var(--text-sm)/var(--leading-snug) var(--font-sans); color: var(--text-primary); margin-bottom: 3px; }
.travel-card-sub { font: var(--type-caption); color: var(--text-secondary); }

/* Skeleton */
.skeleton-card { pointer-events: none; }
.skeleton-box { background: linear-gradient(90deg, var(--color-neutral-100) 25%, var(--color-neutral-200) 50%, var(--color-neutral-100) 75%); background-size: 200% 100%; animation: shimmer 1.4s infinite; }
.skeleton-line { height: 12px; border-radius: var(--radius-sm); background: linear-gradient(90deg, var(--color-neutral-100) 25%, var(--color-neutral-200) 50%, var(--color-neutral-100) 75%); background-size: 200% 100%; animation: shimmer 1.4s infinite; margin-bottom: 8px; }
.skeleton-line.short { width: 40%; }
.skeleton-line.mid { width: 60%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.recent-list { padding: 0 20px 20px; display: flex; flex-direction: column; gap: 2px; }
.recent-item { display: flex; align-items: center; gap: 10px; padding: 11px 0; border-bottom: 1px solid var(--border-subtle); }
.recent-item:last-child { border-bottom: none; }
.recent-icon { width: 34px; height: 34px; background: var(--surface-subtle); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; color: var(--text-secondary); flex-shrink: 0; }
.recent-text { flex: 1; }
.recent-main { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.recent-sub { font: var(--type-caption); color: var(--text-tertiary); margin-top: 3px; }
</style>
