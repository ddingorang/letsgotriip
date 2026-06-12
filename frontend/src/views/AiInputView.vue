<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useRecommendStore } from '../stores/recommend.js';
import { http } from '../api/http.js';

const router = useRouter();
const recommendStore = useRecommendStore();

// ── 지역 ────────────────────────────────────────────────────────────────────
const areas = ref([]);
const selectedAreaCode = ref('');

onMounted(async () => {
  try {
    const { data } = await http.get('/api/attractions/areas');
    areas.value = (Array.isArray(data) ? data : []).slice(0, 8);
    if (areas.value.length) selectedAreaCode.value = areas.value[0].code;
  } catch {
    // 실패 시 빈 목록 유지
  }
});

// ── 날짜 ────────────────────────────────────────────────────────────────────
function defaultStart() {
  const d = new Date();
  d.setDate(d.getDate() + 7);
  return d.toISOString().slice(0, 10); // YYYY-MM-DD
}

const startDate = ref(defaultStart());
const nights = ref(2);

const endDate = computed(() => {
  const d = new Date(startDate.value);
  d.setDate(d.getDate() + nights.value);
  return d.toISOString().slice(0, 10);
});

function formatDisplay(dateStr) {
  const d = new Date(dateStr);
  const month = d.getMonth() + 1;
  const day = d.getDate();
  const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
  const wd = weekdays[d.getDay()];
  return `${String(month).padStart(2, '0')}월 ${String(day).padStart(2, '0')}일 (${wd})`;
}

// ── 동행인 ──────────────────────────────────────────────────────────────────
const companionTypes = [
  { key: 'solo',   apiVal: 'SOLO',    label: '혼자',   sub: '솔로 여행' },
  { key: 'couple', apiVal: 'COUPLE',  label: '커플',   sub: '2명'       },
  { key: 'family', apiVal: 'FAMILY',  label: '가족',   sub: '아이 포함' },
  { key: 'friend', apiVal: 'FRIENDS', label: '친구',   sub: '3명 이상'  },
];
const selectedCompanion = ref('couple');

// ── 예산 ────────────────────────────────────────────────────────────────────
const budgetOptions = [
  { label: '10만원 이하',  value: 100_000   },
  { label: '10~30만원',   value: 300_000   },
  { label: '30~50만원',   value: 500_000   },
  { label: '50만원 이상', value: 1_000_000 },
  { label: '상관없음',    value: null       },
];
const selectedBudget = ref('10~30만원');

// ── 테마 ────────────────────────────────────────────────────────────────────
const themes = [
  { key: 'sea',      icon: '🌊', label: '바다/해변' },
  { key: 'mountain', icon: '🏔️', label: '산/자연'   },
  { key: 'food',     icon: '🍽️', label: '맛집 투어' },
  { key: 'history',  icon: '🏛️', label: '역사/문화' },
  { key: 'activity', icon: '🎢', label: '액티비티'  },
  { key: 'shopping', icon: '🛍️', label: '쇼핑'      },
];
const selectedThemes = ref(['sea', 'food']);

function toggleTheme(key) {
  const idx = selectedThemes.value.indexOf(key);
  if (idx === -1) selectedThemes.value.push(key);
  else selectedThemes.value.splice(idx, 1);
}

// ── 에러 표시 ───────────────────────────────────────────────────────────────
const localError = ref('');

// ── CTA ─────────────────────────────────────────────────────────────────────
async function handleGenerate() {
  if (!selectedAreaCode.value) {
    localError.value = '지역을 선택해 주세요.';
    return;
  }
  localError.value = '';
  recommendStore.error = null;

  const companion = companionTypes.find(c => c.key === selectedCompanion.value);
  const budgetOpt = budgetOptions.find(o => o.label === selectedBudget.value);

  const payload = {
    areaCode: selectedAreaCode.value,
    startDate: startDate.value,
    endDate: endDate.value,
    companions: companion?.apiVal,
    themes: selectedThemes.value.length ? selectedThemes.value : undefined,
  };
  if (budgetOpt?.value != null) payload.budget = budgetOpt.value;

  try {
    await recommendStore.generate(payload);
    router.push('/ai/result');
  } catch {
    // error already set in store
  }
}
</script>

<template>
  <div class="ai-input-view">
    <!-- Custom dark nav -->
    <div class="nav-bar">
      <div class="nav-back" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      </div>
      <div class="nav-title">AI 여행 추천</div>
      <div class="nav-history" @click="router.push('/ai/result')">이전 추천</div>
    </div>

    <!-- Hero -->
    <div class="ai-hero">
      <div class="ai-hero-glow"></div>
      <div class="ai-hero-glow2"></div>
      <div class="ai-badge">
        <div class="ai-badge-dot"></div>
        AI 분석 준비 완료
      </div>
      <div class="ai-title">어떤 여행을<br>원하시나요?</div>
      <div class="ai-sub">조건을 입력하면 AI가 최적의 일정을 만들어 드려요</div>
    </div>

    <div class="content">
      <!-- 지역 -->
      <div class="form-card">
        <div class="form-card-title">
          <div class="form-card-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><circle cx="12" cy="11" r="3"/></svg>
          </div>
          어디로 가고 싶으세요?
        </div>
        <div v-if="areas.length === 0" class="region-loading">지역 목록을 불러오는 중...</div>
        <div v-else class="region-grid">
          <div
            v-for="area in areas"
            :key="area.code"
            class="region-btn"
            :class="{ selected: selectedAreaCode === area.code }"
            @click="selectedAreaCode = area.code"
          >{{ area.name }}</div>
        </div>
      </div>

      <!-- 날짜 -->
      <div class="form-card">
        <div class="form-card-title">
          <div class="form-card-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          </div>
          언제 떠나세요?
        </div>
        <div class="date-row" style="margin-bottom:14px">
          <div class="date-picker selected">
            <div class="date-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            </div>
            <div>
              <div class="date-label">출발일</div>
              <div class="date-value">{{ formatDisplay(startDate) }}</div>
            </div>
          </div>
          <div class="date-picker selected">
            <div class="date-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            </div>
            <div>
              <div class="date-label">도착일</div>
              <div class="date-value">{{ formatDisplay(endDate) }}</div>
            </div>
          </div>
        </div>
        <div class="night-row">
          <div class="night-label">숙박 일수</div>
          <div class="night-ctrl">
            <div class="night-btn" @click="nights > 1 && nights--">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </div>
            <div class="night-val">{{ nights }}</div>
            <div class="night-btn" @click="nights < 7 && nights++">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 동행인 -->
      <div class="form-card">
        <div class="form-card-title">
          <div class="form-card-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          누구와 함께 가나요?
        </div>
        <div class="people-grid">
          <div
            v-for="c in companionTypes"
            :key="c.key"
            class="people-btn"
            :class="{ selected: selectedCompanion === c.key }"
            @click="selectedCompanion = c.key"
          >
            <div class="people-icon">
              <svg v-if="c.key === 'solo'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <svg v-else-if="c.key === 'couple'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              <svg v-else-if="c.key === 'family'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="7" r="4"/><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/></svg>
            </div>
            <div class="people-text">
              <div class="people-name">{{ c.label }}</div>
              <div class="people-sub">{{ c.sub }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 예산 -->
      <div class="form-card">
        <div class="form-card-title">
          <div class="form-card-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          </div>
          예산이 얼마나 되세요?
        </div>
        <div class="budget-options">
          <div
            v-for="opt in budgetOptions"
            :key="opt.label"
            class="budget-chip"
            :class="{ selected: selectedBudget === opt.label }"
            @click="selectedBudget = opt.label"
          >{{ opt.label }}</div>
        </div>
      </div>

      <!-- 테마 -->
      <div class="form-card">
        <div class="form-card-title">
          <div class="form-card-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          </div>
          어떤 여행을 원하세요?
          <span class="title-hint">(복수 선택)</span>
        </div>
        <div class="theme-grid">
          <div
            v-for="t in themes"
            :key="t.key"
            class="theme-btn"
            :class="{ selected: selectedThemes.includes(t.key) }"
            @click="toggleTheme(t.key)"
          >
            <div class="theme-icon">{{ t.icon }}</div>
            <div class="theme-label">{{ t.label }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom CTA -->
    <div class="bottom-bar">
      <div v-if="localError || recommendStore.error" class="error-msg">
        {{ localError || recommendStore.error }}
      </div>
      <button
        class="ai-btn"
        :disabled="recommendStore.generating"
        @click="handleGenerate"
      >
        <template v-if="!recommendStore.generating">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          AI 일정 생성하기
        </template>
        <template v-else>
          <span class="spinner"></span>
          일정 생성 중...
        </template>
      </button>
      <div class="ai-btn-sub">
        <template v-if="recommendStore.generating">AI가 일정을 만들고 있어요… 최대 30초</template>
        <template v-else>약 10–30초 소요 · TourAPI + LLM 기반 생성</template>
      </div>
    </div>

    <!-- Fullscreen loading overlay -->
    <div v-if="recommendStore.generating" class="loading-overlay">
      <div class="loading-box">
        <div class="loading-spinner"></div>
        <div class="loading-title">AI가 일정을 만들고 있어요</div>
        <div class="loading-sub">최대 30초 소요돼요. 잠시만 기다려 주세요.</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-input-view { background: var(--surface-subtle); min-height: 100%; }

.nav-bar { height: 52px; display: flex; align-items: center; padding: 0 20px; gap: 8px; background: #fff; border-bottom: 1px solid var(--border-subtle); position: sticky; top: 0; z-index: var(--z-raised); }
.nav-back { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-primary); cursor: pointer; }
.nav-title { flex: 1; text-align: center; font: var(--weight-semibold) var(--text-lg)/1 var(--font-sans); color: var(--text-primary); }
.nav-history { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-500); cursor: pointer; }

.ai-hero { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); padding: 24px 20px 28px; position: relative; overflow: hidden; }
.ai-hero-glow { position: absolute; top: -40px; right: -40px; width: 180px; height: 180px; background: radial-gradient(circle, rgba(247,143,87,0.25) 0%, transparent 70%); }
.ai-hero-glow2 { position: absolute; bottom: -20px; left: 20px; width: 120px; height: 120px; background: radial-gradient(circle, rgba(99,179,237,0.15) 0%, transparent 70%); }
.ai-badge { display: inline-flex; align-items: center; gap: 6px; background: rgba(247,143,87,0.2); border: 1px solid rgba(247,143,87,0.4); color: var(--color-primary-400); padding: 5px 12px; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-xs)/1 var(--font-sans); margin-bottom: 12px; }
.ai-badge-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--color-primary-400); animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:.5;transform:scale(1.3)} }
.ai-title { font: var(--weight-bold) 22px/var(--leading-tight) var(--font-sans); color: #fff; margin-bottom: 6px; letter-spacing: -0.02em; }
.ai-sub { font: var(--type-body-sm); color: rgba(255,255,255,0.6); }

.content { padding: 20px 20px 160px; display: flex; flex-direction: column; gap: 14px; }

.form-card { background: var(--surface-bg); border-radius: var(--radius-lg); padding: 18px; }
.form-card-title { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 14px; display: flex; align-items: center; gap: 8px; }
.form-card-icon { width: 28px; height: 28px; background: var(--color-primary-50); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; color: var(--color-primary-500); flex-shrink: 0; }
.title-hint { font: var(--weight-regular) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); }

.region-loading { font: var(--type-body-sm); color: var(--text-tertiary); }
.region-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.region-btn { padding: 10px 6px; background: var(--surface-subtle); border-radius: var(--radius-sm); border: 1.5px solid transparent; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); text-align: center; cursor: pointer; }
.region-btn.selected { background: var(--color-primary-50); border-color: var(--color-primary-400); color: var(--color-primary-600); font-weight: var(--weight-semibold); }

.date-row { display: flex; gap: 10px; }
.date-picker { flex: 1; display: flex; align-items: center; gap: 8px; background: var(--surface-subtle); border-radius: var(--radius-sm); padding: 12px; cursor: pointer; border: 1.5px solid transparent; }
.date-picker.selected { border-color: var(--color-primary-300); background: var(--color-primary-50); }
.date-label { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); margin-bottom: 4px; }
.date-value { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.date-icon { color: var(--color-primary-500); flex-shrink: 0; }

.night-row { display: flex; align-items: center; justify-content: space-between; }
.night-label { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); }
.night-ctrl { display: flex; align-items: center; gap: 14px; }
.night-btn { width: 34px; height: 34px; border-radius: var(--radius-full); border: 1.5px solid var(--border-default); background: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--text-primary); }
.night-val { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: var(--text-primary); width: 24px; text-align: center; }

.people-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.people-btn { display: flex; align-items: center; gap: 10px; padding: 12px; background: var(--surface-subtle); border-radius: var(--radius-sm); border: 1.5px solid transparent; cursor: pointer; }
.people-btn.selected { background: var(--color-primary-50); border-color: var(--color-primary-400); }
.people-icon { width: 34px; height: 34px; background: #fff; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; color: var(--text-secondary); flex-shrink: 0; }
.people-btn.selected .people-icon { background: var(--color-primary-100); color: var(--color-primary-500); }
.people-text { flex: 1; }
.people-name { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.people-sub { font: var(--type-caption); color: var(--text-tertiary); margin-top: 2px; }

.budget-options { display: flex; gap: 8px; flex-wrap: wrap; }
.budget-chip { padding: 8px 14px; border-radius: var(--radius-full); font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); background: var(--surface-subtle); color: var(--text-secondary); border: 1.5px solid var(--border-default); cursor: pointer; white-space: nowrap; }
.budget-chip.selected { background: var(--color-primary-50); border-color: var(--color-primary-400); color: var(--color-primary-600); font-weight: var(--weight-semibold); }

.theme-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.theme-btn { padding: 12px 8px; background: var(--surface-subtle); border-radius: var(--radius-md); border: 1.5px solid transparent; display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer; }
.theme-btn.selected { background: var(--color-primary-50); border-color: var(--color-primary-400); }
.theme-icon { font-size: 22px; line-height: 1; }
.theme-label { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); text-align: center; }
.theme-btn.selected .theme-label { color: var(--color-primary-600); font-weight: var(--weight-semibold); }

.bottom-bar { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 430px; background: var(--surface-bg); border-top: 1px solid var(--border-subtle); padding: 14px 20px 34px; display: flex; flex-direction: column; gap: 10px; z-index: var(--z-raised); }
.error-msg { font: var(--weight-medium) var(--text-sm)/1.4 var(--font-sans); color: #e53e3e; text-align: center; }
.ai-btn { width: 100%; height: 54px; background: linear-gradient(90deg, var(--color-primary-500) 0%, #F9A96A 100%); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-lg)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 10px; box-shadow: 0 4px 16px rgba(247,143,87,0.35); transition: opacity 0.2s; }
.ai-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ai-btn-sub { text-align: center; font: var(--type-caption); color: var(--text-tertiary); }

/* inline spinner inside button */
@keyframes spin { to { transform: rotate(360deg); } }
.spinner { width: 20px; height: 20px; border: 2.5px solid rgba(255,255,255,0.4); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; flex-shrink: 0; }

/* fullscreen overlay */
.loading-overlay { position: fixed; inset: 0; background: rgba(15,20,40,0.85); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.loading-box { text-align: center; padding: 32px 28px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: var(--radius-lg); max-width: 280px; }
.loading-spinner { width: 48px; height: 48px; border: 3.5px solid rgba(247,143,87,0.3); border-top-color: var(--color-primary-400); border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
.loading-title { font: var(--weight-bold) var(--text-lg)/1.3 var(--font-sans); color: #fff; margin-bottom: 8px; }
.loading-sub { font: var(--type-body-sm); color: rgba(255,255,255,0.6); }
</style>
