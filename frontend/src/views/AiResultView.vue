<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useRecommendStore } from '../stores/recommend.js';

const router = useRouter();
const recommendStore = useRecommendStore();

// ── 초기 로드 ────────────────────────────────────────────────────────────────
const loading = ref(false);
const saveLoading = ref(false);
const saveDone = ref(false);
const savedPlanId = ref(null);

onMounted(async () => {
  if (recommendStore.current) return; // 스토어에 결과 있으면 그대로 사용

  loading.value = true;
  try {
    await recommendStore.loadHistory();
    if (recommendStore.history.length === 0) {
      router.replace('/ai');
      return;
    }
    const latest = recommendStore.history[0];
    await recommendStore.load(latest.id);
  } catch {
    router.replace('/ai');
  } finally {
    loading.value = false;
  }
});

// ── 편의 computed ─────────────────────────────────────────────────────────────
const rec = computed(() => recommendStore.current);
const draft = computed(() => rec.value?.draft ?? null);
const days = computed(() => draft.value?.days ?? []);
const isPartial = computed(() => rec.value?.status === 'PARTIAL');

// ── 탭 ──────────────────────────────────────────────────────────────────────
const activeTab = ref(0);
const tabs = computed(() => days.value.map(d => `${d.dayNo}일차`));
const activeDay = computed(() => days.value[activeTab.value] ?? null);

// ── 내 계획에 담기 ─────────────────────────────────────────────────────────
async function handleSavePlan() {
  if (!rec.value) return;
  saveLoading.value = true;
  recommendStore.error = null;
  try {
    const plan = await recommendStore.savePlan(rec.value.id);
    savedPlanId.value = plan?.id ?? null;
    saveDone.value = true;
  } catch {
    // error displayed via store.error
  } finally {
    saveLoading.value = false;
  }
}
</script>

<template>
  <div class="ai-result-view">

    <!-- Loading skeleton -->
    <div v-if="loading" class="skeleton-wrap">
      <div class="skeleton-nav"></div>
      <div class="skeleton-banner"></div>
      <div class="skeleton-body"></div>
    </div>

    <template v-else-if="rec">
      <!-- Custom nav -->
      <div class="nav-bar">
        <div class="nav-back" @click="router.back()">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </div>
        <div class="nav-title">AI 추천 결과</div>
        <div class="nav-regen" @click="router.push('/ai')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          재생성
        </div>
        <div class="nav-share">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
        </div>
      </div>

      <!-- Summary Banner -->
      <div class="summary-banner">
        <div class="summary-info">
          <div class="summary-label">
            AI가 생성한 최적 일정
            <span v-if="isPartial" class="partial-badge">일부 일정만 생성됐어요</span>
          </div>
          <div class="summary-title">{{ draft?.totalSummary ?? '추천 일정' }}</div>
          <div class="summary-tags">
            <span class="summary-tag">{{ days.length }}일 일정</span>
            <span v-if="rec.status" class="summary-tag">{{ rec.status }}</span>
          </div>
        </div>
        <div class="summary-score">
          <div class="score-circle">
            <div class="score-num">AI</div>
            <div class="score-label">추천</div>
          </div>
        </div>
      </div>

      <!-- Day Tabs -->
      <div class="day-tabs">
        <div
          v-for="(tab, i) in tabs"
          :key="tab"
          class="day-tab"
          :class="{ active: activeTab === i }"
          @click="activeTab = i"
        >{{ tab }}</div>
      </div>

      <div class="content">
        <!-- Day Header -->
        <div v-if="activeDay" class="day-header">
          <div class="day-pill">{{ activeDay.dayNo }}일차</div>
          <div class="day-summary">{{ activeDay.summary }}</div>
        </div>

        <!-- Timeline -->
        <div v-if="activeDay && activeDay.places.length" class="timeline">
          <div
            v-for="(place, idx) in activeDay.places"
            :key="place.contentId"
            class="timeline-item"
          >
            <div class="timeline-dot">
              <div class="dot-circle">
                <div class="dot-num">{{ idx + 1 }}</div>
              </div>
              <div class="dot-time">{{ place.visitTime }}</div>
            </div>
            <div class="place-card">
              <div class="place-top">
                <div class="place-thumb">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                </div>
                <div class="place-info">
                  <div class="place-name">{{ place.title }}</div>
                  <div v-if="place.reason" class="place-reason">{{ place.reason }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="activeDay" class="empty-day">
          이 날의 일정이 없어요.
        </div>

        <!-- Error -->
        <div v-if="recommendStore.error" class="error-msg">{{ recommendStore.error }}</div>

        <!-- Save done notice -->
        <div v-if="saveDone" class="save-notice">
          계획에 저장했어요!
          <span class="save-notice-link" @click="router.push('/plan')">계획 보러 가기 →</span>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div class="bottom-bar">
        <button class="btn-modify" @click="router.push('/ai')">재생성</button>
        <button
          class="btn-save"
          :disabled="saveLoading"
          @click="handleSavePlan"
        >
          <template v-if="saveLoading">
            <span class="spinner"></span>
            저장 중...
          </template>
          <template v-else-if="saveDone">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            저장됨
          </template>
          <template v-else>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            내 계획에 담기
          </template>
        </button>
      </div>
    </template>

  </div>
</template>

<style scoped>
.ai-result-view { background: var(--surface-subtle); min-height: 100%; }

/* skeleton */
.skeleton-wrap { padding: 0; }
.skeleton-nav { height: 52px; background: #fff; border-bottom: 1px solid var(--border-subtle); }
.skeleton-banner { height: 110px; background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%); }
.skeleton-body { padding: 20px; display: flex; flex-direction: column; gap: 12px; }

.nav-bar { height: 52px; display: flex; align-items: center; padding: 0 16px; gap: 8px; background: #fff; border-bottom: 1px solid var(--border-subtle); position: sticky; top: 0; z-index: var(--z-raised); }
.nav-back { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-primary); cursor: pointer; flex-shrink: 0; }
.nav-title { flex: 1; font: var(--weight-semibold) var(--text-lg)/1 var(--font-sans); color: var(--text-primary); }
.nav-share { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: var(--text-secondary); cursor: pointer; flex-shrink: 0; }
.nav-regen { display: flex; align-items: center; gap: 5px; font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-500); padding: 7px 12px; background: var(--color-primary-50); border-radius: var(--radius-full); cursor: pointer; white-space: nowrap; }

.summary-banner { background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%); padding: 18px 20px; display: flex; align-items: center; justify-content: space-between; }
.summary-label { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: rgba(255,255,255,0.6); margin-bottom: 5px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.partial-badge { background: rgba(255,200,100,0.25); border: 1px solid rgba(255,200,100,0.5); color: #ffd280; padding: 2px 8px; border-radius: var(--radius-full); font-size: 10px; }
.summary-title { font: var(--weight-bold) var(--text-xl)/var(--leading-snug) var(--font-sans); color: #fff; letter-spacing: -0.02em; }
.summary-tags { display: flex; gap: 6px; margin-top: 8px; }
.summary-tag { font: var(--weight-medium) 10px/1 var(--font-sans); color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.12); padding: 3px 8px; border-radius: var(--radius-full); }
.score-circle { width: 60px; height: 60px; border-radius: var(--radius-full); background: rgba(247,143,87,0.2); border: 2px solid rgba(247,143,87,0.5); display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; }
.score-num { font: var(--weight-extrabold) 16px/1 var(--font-sans); color: var(--color-primary-400); }
.score-label { font: var(--weight-medium) 9px/1 var(--font-sans); color: rgba(255,255,255,0.5); margin-top: 2px; }

.day-tabs { display: flex; background: #fff; border-bottom: 1px solid var(--border-subtle); overflow-x: auto; scrollbar-width: none; padding: 0 20px; }
.day-tabs::-webkit-scrollbar { display: none; }
.day-tab { flex-shrink: 0; padding: 14px 16px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); border-bottom: 2.5px solid transparent; cursor: pointer; white-space: nowrap; }
.day-tab.active { color: var(--color-primary-500); border-bottom-color: var(--color-primary-500); font-weight: var(--weight-semibold); }

.content { padding: 16px 20px 140px; }

.day-header { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 14px; }
.day-pill { background: var(--color-primary-500); color: #fff; padding: 5px 12px; border-radius: var(--radius-full); font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); flex-shrink: 0; }
.day-summary { font: var(--weight-medium) var(--text-sm)/1.4 var(--font-sans); color: var(--text-secondary); padding-top: 3px; }

.empty-day { font: var(--type-body-sm); color: var(--text-tertiary); padding: 20px 0; text-align: center; }
.error-msg { font: var(--weight-medium) var(--text-sm)/1.4 var(--font-sans); color: #e53e3e; margin-top: 12px; text-align: center; }
.save-notice { background: var(--color-primary-50); border: 1px solid var(--color-primary-200); border-radius: var(--radius-md); padding: 12px 16px; margin-top: 16px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-700); display: flex; align-items: center; justify-content: space-between; }
.save-notice-link { font-weight: var(--weight-semibold); color: var(--color-primary-500); cursor: pointer; }

.timeline { position: relative; }
.timeline::before { content: ''; position: absolute; left: 19px; top: 0; bottom: 0; width: 2px; background: var(--border-subtle); }
.timeline-item { display: flex; gap: 14px; margin-bottom: 16px; position: relative; }
.timeline-dot { width: 40px; flex-shrink: 0; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.dot-circle { width: 22px; height: 22px; border-radius: var(--radius-full); border: 2.5px solid var(--color-primary-500); background: #fff; display: flex; align-items: center; justify-content: center; z-index: 1; position: relative; }
.dot-num { font: var(--weight-bold) 9px/1 var(--font-sans); color: var(--color-primary-500); }
.dot-time { font: var(--weight-medium) 9px/1 var(--font-sans); color: var(--text-tertiary); margin-top: 2px; white-space: nowrap; }

.place-card { flex: 1; background: var(--surface-bg); border-radius: var(--radius-lg); padding: 14px; box-shadow: var(--shadow-xs); }
.place-top { display: flex; align-items: flex-start; gap: 10px; }
.place-thumb { width: 48px; height: 48px; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-neutral-200), var(--color-neutral-100)); display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: var(--text-tertiary); }
.place-info { flex: 1; }
.place-name { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 5px; }
.place-reason { font: var(--type-body-sm); color: var(--text-secondary); line-height: 1.4; }

.bottom-bar { position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); width: 100%; max-width: 430px; background: var(--surface-bg); border-top: 1px solid var(--border-subtle); padding: 14px 20px 34px; display: flex; gap: 10px; z-index: var(--z-raised); }
.btn-save { flex: 1; height: 52px; background: var(--color-primary-500); color: #fff; border: none; border-radius: var(--radius-md); font: var(--weight-bold) var(--text-base)/1 var(--font-sans); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; transition: opacity 0.2s; }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-modify { height: 52px; padding: 0 18px; background: transparent; color: var(--text-secondary); border: 1.5px solid var(--border-default); border-radius: var(--radius-md); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); cursor: pointer; white-space: nowrap; }

@keyframes spin { to { transform: rotate(360deg); } }
.spinner { width: 18px; height: 18px; border: 2.5px solid rgba(255,255,255,0.4); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; flex-shrink: 0; }
</style>
