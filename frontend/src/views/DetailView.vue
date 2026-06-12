<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAttractionStore } from '../stores/attraction.js';
import { useAuthStore } from '../stores/auth.js';
import { usePlanStore } from '../stores/plan.js';

const router = useRouter();
const route = useRoute();
const store = useAttractionStore();
const auth = useAuthStore();
const planStore = usePlanStore();

// ── API data ──────────────────────────────────────────────────────────────────
const detail = ref(null);
const loading = ref(false);
const fetchError = ref(null);

onMounted(async () => {
  const contentId = route.params.id;
  if (!contentId) return;
  loading.value = true;
  fetchError.value = null;
  try {
    detail.value = await store.fetchDetail(contentId);
  } catch (e) {
    fetchError.value = e.response?.data?.message ?? e.message ?? '정보를 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
});

// ── 계획에 담기 ───────────────────────────────────────────────────────────────
const addMsg = ref('');   // 성공/중복 메시지
const addLoading = ref(false);

async function addToPlan() {
  if (!auth.isAuthenticated) {
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`);
    return;
  }

  addLoading.value = true;
  addMsg.value = '';
  planStore.error = null;

  try {
    // 계획 목록 조회
    await planStore.loadPlans();

    let planId;
    if (planStore.plans.length === 0) {
      // 기본 계획 자동 생성
      const today = new Date();
      const endDate = new Date();
      endDate.setDate(today.getDate() + 2);
      const pad = (n) => String(n).padStart(2, '0');
      const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
      const created = await planStore.createPlan({
        title: '나의 여행',
        startDate: fmt(today),
        endDate: fmt(endDate),
      });
      planId = created.id;
    } else {
      planId = planStore.plans[0].id;
      await planStore.loadPlan(planId);
    }

    const contentId = route.params.id;
    const contentType = detail.value?.contentTypeId ?? 12;

    await planStore.addPlace(planId, 1, { contentId, contentType });
    addMsg.value = 'added'; // 성공 → 버튼 영역에서 처리
  } catch (e) {
    const code = e.response?.data?.code;
    if (code === 'PLAN4093') {
      addMsg.value = 'duplicate';
    } else {
      addMsg.value = 'error';
    }
  } finally {
    addLoading.value = false;
  }
}

// ── Static mock data (편의시설 · 예약정보 — API에 없음, 그대로 유지) ───────────
const amenities = [
  { label: '무료 Wi-Fi', svg: '<path d="M5 12h14M12 5l7 7-7 7"/>' },
  { label: '수영장',     svg: '<rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>' },
  { label: '주차장',     svg: '<path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>' },
  { label: '조식 포함',  svg: '<path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/>' },
  { label: '오션뷰',     svg: '<circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>' },
  { label: '공항 셔틀',  svg: '<rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/>' },
];

const infoRows = [
  {
    label: '체크인 / 체크아웃',
    value: '06월 15일(토) 15:00 ~ 06월 17일(월) 11:00',
    svg: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>'
  },
  {
    label: '인원',
    value: '성인 2명',
    svg: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>'
  },
];
</script>

<template>
  <div class="detail">
    <!-- Loading skeleton for hero area -->
    <div v-if="loading" class="hero hero-skeleton">
      <div class="hero-bg skeleton-bg"></div>
      <div class="hero-overlay"></div>
      <div class="hero-top-actions">
        <button class="hero-action-btn" aria-label="뒤로가기" @click="router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </button>
      </div>
    </div>

    <!-- Hero (data loaded or no id) -->
    <div v-else class="hero">
      <!-- background: real image or gradient placeholder -->
      <div class="hero-bg" :style="detail?.firstimage ? `background-image:url('${detail.firstimage}');background-size:cover;background-position:center` : ''">
        <svg v-if="!detail?.firstimage" width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.3)" stroke-width="1"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/></svg>
      </div>
      <div class="hero-overlay"></div>
      <div class="hero-top-actions">
        <button class="hero-action-btn" aria-label="뒤로가기" @click="router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </button>
        <div class="hero-action-right">
          <button class="hero-action-btn" aria-label="공유">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
          </button>
          <button class="hero-action-btn" aria-label="찜하기">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </button>
        </div>
      </div>
      <div class="hero-photos">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
        <span class="hero-photos-text">사진 18장</span>
      </div>
      <div class="hero-badge">★ 4.9 · 인기 No.1</div>
      <div class="hero-name">{{ detail?.title ?? '해운대 오션뷰 호텔' }}</div>
      <div class="hero-sub">{{ detail?.addr1 ?? '부산 해운대구 · 오션뷰 · 수영장' }}</div>
    </div>

    <!-- Body -->
    <div class="body">
      <!-- Fetch error notice (non-fatal: show mock data below) -->
      <div v-if="fetchError" class="fetch-error">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        {{ fetchError }}
      </div>

      <div class="rating-row">
        <div class="rating-big">4.9</div>
        <div class="rating-details">
          <div class="stars">★★★★★</div>
          <div class="rating-count">리뷰 1,240개</div>
        </div>
        <div class="rating-tag">최우수 숙소</div>
      </div>

      <div class="section-title">숙소 소개</div>
      <div class="desc">
        {{ detail?.overview || '해운대 해수욕장 바로 앞에 위치한 오션뷰 호텔입니다. 객실에서 바다를 한눈에 볼 수 있으며, 야외 수영장과 스파 시설을 갖추고 있습니다.' }}
      </div>

      <div class="section-title">편의시설</div>
      <div class="amenities">
        <div v-for="a in amenities" :key="a.label" class="amenity-item">
          <div class="amenity-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-html="a.svg"></svg>
          </div>
          <div class="amenity-label">{{ a.label }}</div>
        </div>
      </div>

      <div class="divider"></div>

      <div class="section-title">예약 정보</div>
      <div class="info-list">
        <!-- API fields: tel + addr1 -->
        <div v-if="detail?.tel" class="info-row">
          <div class="info-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07A19.5 19.5 0 013.07 9.8 19.79 19.79 0 01.1 1.18 2 2 0 012.09 0h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L6.09 7.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 14.92z"/></svg>
          </div>
          <div class="info-body">
            <div class="info-label">전화번호</div>
            <div class="info-value">{{ detail.tel }}</div>
          </div>
        </div>
        <div v-if="detail?.addr1" class="info-row">
          <div class="info-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
          </div>
          <div class="info-body">
            <div class="info-label">위치</div>
            <div class="info-value">{{ detail.addr1 }}</div>
          </div>
        </div>
        <!-- Static mock rows (체크인/인원 — API에 없으므로 유지) -->
        <div v-for="row in infoRows" :key="row.label" class="info-row">
          <div class="info-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" v-html="row.svg"></svg>
          </div>
          <div class="info-body">
            <div class="info-label">{{ row.label }}</div>
            <div class="info-value">{{ row.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Bar -->
    <div class="bottom-bar">
      <!-- 계획에 담기 결과 토스트 -->
      <div v-if="addMsg === 'added'" class="add-result add-result--ok">
        <span>계획에 담았어요!</span>
        <button class="go-plan-btn" @click="router.push('/plan')">계획 보기</button>
      </div>
      <div v-else-if="addMsg === 'duplicate'" class="add-result add-result--dup">이미 계획에 있어요.</div>
      <div v-else-if="addMsg === 'error'" class="add-result add-result--err">{{ planStore.error ?? '오류가 발생했어요.' }}</div>

      <div v-if="!addMsg" class="bottom-actions">
        <button class="plan-btn" @click="addToPlan" :disabled="addLoading">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/></svg>
          {{ addLoading ? '담는 중…' : '계획에 담기' }}
        </button>
        <button class="book-btn" @click="router.push('/payment')">예약하기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail { background: var(--surface-subtle); }

.hero { width: 100%; height: 300px; display: flex; flex-direction: column; justify-content: flex-end; padding: 20px; position: relative; overflow: hidden; }
.hero-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #b0c4de 0%, #87a8c5 50%, #5b86a8 100%); z-index: 0; display: flex; align-items: center; justify-content: center; }
.hero-overlay { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,0.15) 0%, transparent 35%, rgba(0,0,0,0.55) 100%); z-index: 1; }
.hero-top-actions { position: absolute; top: 16px; left: 20px; right: 20px; display: flex; justify-content: space-between; z-index: 2; }
.hero-action-btn { width: 38px; height: 38px; background: rgba(0,0,0,0.4); border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; color: #fff; cursor: pointer; backdrop-filter: blur(8px); border: none; }
.hero-action-right { display: flex; gap: 8px; }
.hero-photos { position: absolute; bottom: 16px; right: 16px; display: flex; align-items: center; gap: 4px; background: rgba(0,0,0,0.5); padding: 5px 10px; border-radius: var(--radius-full); backdrop-filter: blur(4px); z-index: 2; }
.hero-photos-text { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: #fff; }
.hero-badge { display: inline-flex; align-items: center; gap: 5px; background: var(--color-primary-500); color: #fff; padding: 5px 11px; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); margin-bottom: 10px; width: fit-content; z-index: 2; position: relative; }
.hero-name { font: var(--weight-bold) 24px/var(--leading-tight) var(--font-sans); color: #fff; z-index: 2; position: relative; letter-spacing: -0.02em; }
.hero-sub { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: rgba(255,255,255,0.85); z-index: 2; position: relative; margin-top: 6px; }

/* Skeleton hero */
.skeleton-bg { animation: shimmer 1.4s infinite; background: linear-gradient(90deg, #b0c4de 25%, #87a8c5 50%, #b0c4de 75%); background-size: 200% 100%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.body { background: var(--surface-bg); border-radius: var(--radius-2xl) var(--radius-2xl) 0 0; margin-top: -20px; padding: 24px 20px 120px; position: relative; }

.fetch-error { display: flex; align-items: center; gap: 8px; background: var(--surface-subtle); border-radius: var(--radius-md); padding: 10px 14px; margin-bottom: 20px; font: var(--type-body-sm); color: var(--text-secondary); }

.rating-row { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; padding-bottom: 20px; border-bottom: 1px solid var(--border-subtle); }
.rating-big { font: var(--weight-bold) 36px/1 var(--font-sans); color: var(--text-primary); }
.rating-details { flex: 1; }
.stars { display: flex; gap: 2px; margin-bottom: 5px; color: var(--color-warning); }
.rating-count { font: var(--type-caption); color: var(--text-secondary); }
.rating-tag { background: var(--color-primary-50); color: var(--color-primary-600); padding: 6px 12px; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); }

.section-title { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 12px; letter-spacing: -0.02em; }
.desc { font: var(--type-body); color: var(--text-secondary); line-height: var(--leading-loose); margin-bottom: 24px; }

.amenities { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 24px; }
.amenity-item { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 14px 8px; background: var(--surface-subtle); border-radius: var(--radius-md); }
.amenity-icon { color: var(--color-primary-500); }
.amenity-label { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); text-align: center; }

.divider { height: 8px; background: var(--surface-subtle); margin: 0 -20px 24px; }

.info-list { display: flex; flex-direction: column; gap: 14px; margin-bottom: 24px; }
.info-row { display: flex; align-items: flex-start; gap: 12px; }
.info-icon { color: var(--text-tertiary); flex-shrink: 0; margin-top: 1px; }
.info-body { flex: 1; }
.info-label { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); margin-bottom: 3px; }
.info-value { font: var(--type-body); color: var(--text-primary); }

.bottom-bar { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 430px; background: var(--surface-bg); border-top: 1px solid var(--border-subtle); padding: 14px 20px 34px; display: flex; flex-direction: column; gap: 10px; z-index: var(--z-raised); }
.bottom-actions { display: flex; align-items: center; gap: 10px; }
.price-wrap { flex: 1; }
.price-label { font: var(--type-caption); color: var(--text-tertiary); }
.price-val { font: var(--weight-bold) var(--text-2xl)/1 var(--font-sans); color: var(--text-primary); letter-spacing: -0.02em; }
.price-val span { font: var(--weight-regular) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); }
.plan-btn { flex: 1; height: 52px; background: var(--surface-subtle); color: var(--color-primary-600); border: 1.5px solid var(--color-primary-300); border-radius: var(--radius-md); font: var(--weight-semibold) var(--text-base)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; }
.plan-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.book-btn { flex: 1; height: 52px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-lg)/1 var(--font-sans); cursor: pointer; }
.add-result { width: 100%; padding: 10px 14px; border-radius: var(--radius-md); font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.add-result--ok { background: #f0fdf4; color: #2e7d32; }
.add-result--dup { background: var(--color-primary-50); color: var(--color-primary-600); }
.add-result--err { background: #fff3f3; color: #c62828; }
.go-plan-btn { background: none; border: none; color: var(--color-primary-600); font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); cursor: pointer; text-decoration: underline; padding: 0; }
</style>
