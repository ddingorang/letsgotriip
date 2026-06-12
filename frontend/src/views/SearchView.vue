<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAttractionStore } from '../stores/attraction.js';

const router = useRouter();
const store = useAttractionStore();

// ── Filter chips ─────────────────────────────────────────────────────────────
// contentTypeId mapping: 숙소=32, 관광지=12, 문화시설=14, 축제=15, 음식점=39
// 교통 has no TourAPI type → skip (keep as UI-only decoration)
const FILTER_CHIPS = [
  { key: 'date',     label: '06/15 ~ 06/17', type: 'has-value', contentTypeId: null, isUi: true },
  { key: 'people',   label: '성인 2명',       type: 'has-value', contentTypeId: null, isUi: true },
  { key: 'stay',     label: '숙소',           type: 'default',   contentTypeId: 32 },
  { key: 'transport',label: '교통',           type: 'default',   contentTypeId: null, isUi: true },
  { key: 'activity', label: '액티비티',       type: 'default',   contentTypeId: 12 },
  { key: 'filter',   label: '필터',           type: 'default',   contentTypeId: null, isUi: true, hasFilterIcon: true },
];

const activeType = ref(12); // default: 관광지

function chipClass(chip) {
  if (chip.isUi || chip.hasFilterIcon) return chip.type;
  if (chip.contentTypeId === activeType.value) return 'active';
  return 'default';
}

function onChipClick(chip) {
  if (chip.isUi || chip.hasFilterIcon) return;
  const typeId = chip.contentTypeId ?? 12;
  activeType.value = typeId;
  store.search({ contentTypeId: typeId });
}

// ── Mount: default search ─────────────────────────────────────────────────────
onMounted(() => {
  store.search({ contentTypeId: 12 });
});

function retry() {
  store.search({ contentTypeId: activeType.value });
}
</script>

<template>
  <div class="search">
    <div class="top-bar">
      <div class="search-row">
        <button class="back-btn" aria-label="뒤로가기" @click="router.back()">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </button>
        <div class="search-input-wrap">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary-400)" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
          <span class="search-input-text">부산 여행<span class="cursor"></span></span>
        </div>
      </div>
      <div class="filter-scroll">
        <div
          v-for="f in FILTER_CHIPS"
          :key="f.key"
          class="filter-chip"
          :class="chipClass(f)"
          @click="onChipClick(f)"
        >
          <svg v-if="f.key === 'filter'" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="6" x2="20" y2="6"/><line x1="8" y1="12" x2="16" y2="12"/><line x1="11" y1="18" x2="13" y2="18"/></svg>
          <svg v-else-if="f.key === 'date'" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          <svg v-else-if="f.key === 'people'" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
          {{ f.label }}
        </div>
      </div>
    </div>

    <div class="content">
      <div class="result-header">
        <div class="result-count">검색 결과 <span>{{ store.searchResults.length }}개</span></div>
        <div class="sort-btn">
          추천순
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
        </div>
      </div>

      <!-- Loading skeletons -->
      <div v-if="store.loading" class="result-list">
        <div v-for="n in 4" :key="n" class="result-item skeleton-item">
          <div class="result-thumb skeleton-box"></div>
          <div class="result-body">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line mid"></div>
          </div>
        </div>
      </div>

      <!-- Error state -->
      <div v-else-if="store.error" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <div class="empty-msg">{{ store.error }}</div>
        <button class="retry-btn" @click="retry">다시 시도</button>
      </div>

      <!-- Results -->
      <div v-else class="result-list">
        <!-- Empty results -->
        <div v-if="store.searchResults.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-tertiary)" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
          <div class="empty-msg">검색 결과가 없습니다.</div>
        </div>

        <div
          v-for="item in store.searchResults"
          :key="item.contentId"
          class="result-item"
          @click="router.push(`/detail/${item.contentId}`)"
        >
          <!-- Thumbnail: image or placeholder gradient -->
          <div class="result-thumb" :style="item.firstimage ? '' : ''">
            <img
              v-if="item.firstimage"
              :src="item.firstimage"
              :alt="item.title"
              class="result-thumb-img"
            />
            <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/></svg>
          </div>
          <div class="result-body">
            <div class="result-badge">관광</div>
            <div class="result-title">{{ item.title }}</div>
            <div class="result-sub">{{ item.addr1 }}</div>
          </div>
          <div class="result-heart" @click.stop>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </div>
        </div>
      </div>
    </div>

    <div class="map-fab">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
      지도로 보기
    </div>
  </div>
</template>

<style scoped>
.search { background: var(--surface-subtle); min-height: 100%; }

.top-bar { background: #fff; padding: 12px 20px 0; border-bottom: 1px solid var(--border-subtle); position: sticky; top: 0; z-index: var(--z-raised); }
.search-row { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.back-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--text-primary); cursor: pointer; }
.search-input-wrap { flex: 1; display: flex; align-items: center; gap: 8px; background: var(--surface-subtle); border-radius: var(--radius-md); padding: 10px 14px; border: 1.5px solid var(--color-primary-400); }
.search-input-text { flex: 1; font: var(--type-body-lg); color: var(--text-primary); }
.cursor { display: inline-block; width: 2px; height: 16px; background: var(--color-primary-500); animation: blink 1s step-end infinite; vertical-align: middle; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

.filter-scroll { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 14px; scrollbar-width: none; }
.filter-scroll::-webkit-scrollbar { display: none; }
.filter-chip { display: inline-flex; align-items: center; gap: 5px; padding: 7px 13px; border-radius: var(--radius-full); font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); white-space: nowrap; cursor: pointer; border: 1.5px solid transparent; flex-shrink: 0; }
.filter-chip.active { background: var(--color-primary-50); color: var(--color-primary-500); border-color: var(--color-primary-300); }
.filter-chip.default { background: #fff; color: var(--text-secondary); border-color: var(--border-default); }
.filter-chip.has-value { background: var(--color-primary-500); color: #fff; }

.content { padding: 0 0 100px; }
.result-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px 10px; }
.result-count { font: var(--weight-semibold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); }
.result-count span { color: var(--color-primary-500); }
.sort-btn { display: flex; align-items: center; gap: 4px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); cursor: pointer; }

.result-list { display: flex; flex-direction: column; gap: 1px; background: var(--border-subtle); }
.result-item { display: flex; align-items: center; gap: 14px; padding: 16px 20px; background: var(--surface-card); cursor: pointer; }
.result-thumb { width: 80px; height: 72px; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-neutral-200), var(--color-neutral-100)); display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--text-tertiary); overflow: hidden; }
.result-thumb-img { width: 100%; height: 100%; object-fit: cover; }
.result-body { flex: 1; }
.result-badge { display: inline-block; font: var(--weight-semibold) 10px/1 var(--font-sans); background: var(--color-primary-50); color: var(--color-primary-600); padding: 2px 7px; border-radius: var(--radius-full); margin-bottom: 5px; }
.result-title { font: var(--weight-semibold) var(--text-base)/var(--leading-snug) var(--font-sans); color: var(--text-primary); margin-bottom: 3px; }
.result-sub { font: var(--type-body-sm); color: var(--text-secondary); margin-bottom: 6px; }
.result-heart { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); flex-shrink: 0; cursor: pointer; }

/* Skeleton loading */
.skeleton-item { pointer-events: none; }
.skeleton-box { background: linear-gradient(90deg, var(--color-neutral-100) 25%, var(--color-neutral-200) 50%, var(--color-neutral-100) 75%); background-size: 200% 100%; animation: shimmer 1.4s infinite; }
.skeleton-line { height: 12px; border-radius: var(--radius-sm); background: linear-gradient(90deg, var(--color-neutral-100) 25%, var(--color-neutral-200) 50%, var(--color-neutral-100) 75%); background-size: 200% 100%; animation: shimmer 1.4s infinite; margin-bottom: 8px; }
.skeleton-line.short { width: 40%; }
.skeleton-line.mid { width: 60%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Empty / error state */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 60px 20px; background: var(--surface-card); }
.empty-msg { font: var(--type-body); color: var(--text-secondary); text-align: center; }
.retry-btn { padding: 10px 24px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); cursor: pointer; }

.map-fab { position: fixed; bottom: 100px; right: 20px; display: flex; align-items: center; gap: 8px; background: var(--color-neutral-900); color: #fff; padding: 12px 18px; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); box-shadow: var(--shadow-lg); cursor: pointer; z-index: var(--z-raised); }
</style>
