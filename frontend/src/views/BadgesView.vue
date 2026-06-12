<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const activeTab = ref(0);
const tabs = ['진행중 퀘스트', '뱃지 컬렉션', '완료 기록'];

const quests = [
  {
    id: 1,
    iconBg: 'var(--color-primary-50)',
    iconStroke: 'var(--color-primary-500)',
    iconType: 'map',
    name: '여행 계획 마스터',
    desc: '여행 계획 5개 저장하기',
    progressText: '3/5 완료',
    progressVal: '60%',
    progressWidth: '60%',
    progressColor: 'var(--color-primary-500)',
    rewards: [
      { label: '⚡ 200 XP', style: '' },
      { label: '뱃지 획득', style: 'background:var(--color-primary-50);color:var(--color-primary-600)' },
    ],
  },
  {
    id: 2,
    iconBg: '#E8F5E9',
    iconStroke: '#2E7D32',
    iconType: 'thumb',
    name: '리뷰어 등극',
    desc: '여행지 리뷰 10개 작성하기',
    progressText: '8/10 완료',
    progressVal: '80%',
    progressWidth: '80%',
    progressColor: '#34C759',
    rewards: [
      { label: '⚡ 150 XP', style: '' },
    ],
  },
  {
    id: 3,
    iconBg: '#FFF3E0',
    iconStroke: '#E65100',
    iconType: 'people',
    name: '동행 매니저',
    desc: '동행 모집 3회 완료하기',
    progressText: '1/3 완료',
    progressVal: '33%',
    progressWidth: '33%',
    progressColor: '#FF9500',
    rewards: [
      { label: '⚡ 300 XP', style: '' },
      { label: '특별 뱃지', style: 'background:var(--color-primary-50);color:var(--color-primary-600)' },
    ],
  },
];

const badges = [
  { id: 1, earned: true, bg: 'linear-gradient(135deg,#FFD700,#FFA500)', iconType: 'star', name: '첫 여행' },
  { id: 2, earned: true, bg: 'linear-gradient(135deg,#29B6F6,#0277BD)', iconType: 'wave', name: '해변탐험가' },
  { id: 3, earned: true, bg: 'linear-gradient(135deg,#66BB6A,#2E7D32)', iconType: 'location', name: '지도 마스터' },
  { id: 4, earned: true, bg: 'linear-gradient(135deg,var(--color-primary-400),var(--color-primary-600))', iconType: 'food', name: '미식 여행자' },
  { id: 5, earned: false, bg: 'linear-gradient(135deg,#CE93D8,#7B1FA2)', iconType: 'people2', name: '동행 매니저' },
  { id: 6, earned: false, bg: 'linear-gradient(135deg,#FFB74D,#E65100)', iconType: 'star', name: '전국 일주' },
  { id: 7, earned: false, bg: 'linear-gradient(135deg,#80CBC4,#00695C)', iconType: 'circle', name: 'AI 파워유저' },
  { id: 8, earned: false, bg: 'linear-gradient(135deg,#EF9A9A,#C62828)', iconType: 'heart', name: '찜 컬렉터' },
];
</script>

<template>
  <div class="badges-view">
    <!-- Dark nav matching the hero gradient -->
    <div class="dark-nav">
      <button class="dark-back" aria-label="뒤로가기" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      </button>
      <h1 class="dark-title">나의 뱃지 · 퀘스트</h1>
      <div style="width:36px"></div>
    </div>

    <div class="level-card">
      <div class="level-glow"></div>
      <div class="level-row">
        <div class="level-badge">
          <div class="level-badge-text">LV<br><span style="font-size:18px">7</span></div>
        </div>
        <div class="level-info">
          <div class="level-title">열정 여행가</div>
          <div class="level-exp">다음 레벨까지 340 XP 남음</div>
        </div>
      </div>
      <div class="level-progress-wrap">
        <div class="level-progress-label">
          <span class="level-progress-text">LV 7 — 1,660 XP</span>
          <span class="level-progress-val">68% · LV 8까지 340 XP</span>
        </div>
        <div class="level-bar"><div class="level-bar-fill"></div></div>
      </div>
      <div class="level-stats">
        <div class="level-stat"><div class="level-stat-val">12</div><div class="level-stat-label">여행 횟수</div></div>
        <div class="level-stat"><div class="level-stat-val">8</div><div class="level-stat-label">획득 뱃지</div></div>
        <div class="level-stat"><div class="level-stat-val">1,660</div><div class="level-stat-label">총 XP</div></div>
        <div class="level-stat"><div class="level-stat-val">3</div><div class="level-stat-label">진행 퀘스트</div></div>
      </div>
    </div>

    <div class="section-tabs">
      <div
        v-for="(tab, i) in tabs"
        :key="i"
        class="section-tab"
        :class="{ active: activeTab === i }"
        @click="activeTab = i"
      >{{ tab }}</div>
    </div>

    <div class="content">
      <div class="section-title">
        이번 달 퀘스트
        <span class="section-more">전체보기</span>
      </div>

      <div class="quest-list">
        <div v-for="quest in quests" :key="quest.id" class="quest-card">
          <div class="quest-icon" :style="{ background: quest.iconBg }">
            <svg v-if="quest.iconType === 'map'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="quest.iconStroke" stroke-width="2"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
            <svg v-else-if="quest.iconType === 'thumb'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="quest.iconStroke" stroke-width="2"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/></svg>
            <svg v-else-if="quest.iconType === 'people'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="quest.iconStroke" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <div class="quest-body">
            <div class="quest-name">{{ quest.name }}</div>
            <div class="quest-desc">{{ quest.desc }}</div>
            <div class="quest-progress-wrap">
              <div class="quest-progress-row">
                <span class="quest-progress-text">{{ quest.progressText }}</span>
                <span class="quest-progress-val">{{ quest.progressVal }}</span>
              </div>
              <div class="quest-bar">
                <div class="quest-bar-fill" :style="{ width: quest.progressWidth, background: quest.progressColor }"></div>
              </div>
            </div>
            <div class="quest-reward">
              보상:
              <div v-for="(r, ri) in quest.rewards" :key="ri" class="reward-badge" :style="r.style">{{ r.label }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-title">획득한 뱃지 (8/24)</div>
      <div class="badge-grid">
        <div v-for="badge in badges" :key="badge.id" class="badge-item">
          <div class="badge-icon" :class="badge.earned ? 'earned' : 'locked'" :style="{ background: badge.bg }">
            <svg v-if="badge.iconType === 'star'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            <svg v-else-if="badge.iconType === 'wave'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/></svg>
            <svg v-else-if="badge.iconType === 'location'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
            <svg v-else-if="badge.iconType === 'food'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/></svg>
            <svg v-else-if="badge.iconType === 'people2'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            <svg v-else-if="badge.iconType === 'circle'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>
            <svg v-else-if="badge.iconType === 'heart'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
            <div v-if="!badge.earned" class="badge-lock">
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </div>
          </div>
          <div class="badge-name" :class="{ earned: badge.earned }">{{ badge.name }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.badges-view { background: var(--surface-subtle); }

.dark-nav {
  height: 52px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 8px;
  background: #1a1a2e;
}
.dark-back {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-radius: var(--radius-sm);
}
.dark-title {
  flex: 1;
  text-align: center;
  font: var(--weight-semibold) var(--text-lg)/1 var(--font-sans);
  color: #fff;
}

.level-card { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%); padding: 22px 20px; position: relative; overflow: hidden; }
.level-glow { position: absolute; top: -30px; right: -30px; width: 140px; height: 140px; background: radial-gradient(circle, rgba(247,143,87,0.2) 0%, transparent 70%); pointer-events: none; }
.level-row { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.level-badge { width: 56px; height: 56px; background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600)); border-radius: var(--radius-xl); display: flex; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 4px 12px rgba(247,143,87,0.4); }
.level-badge-text { font: var(--weight-extrabold) 11px/1 var(--font-sans); color: #fff; text-align: center; }
.level-info { flex: 1; }
.level-title { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: #fff; margin-bottom: 4px; }
.level-exp { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: rgba(255,255,255,0.6); }
.level-progress-label { display: flex; justify-content: space-between; margin-bottom: 6px; }
.level-progress-text { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: rgba(255,255,255,0.6); }
.level-progress-val { font: var(--weight-semibold) var(--text-xs)/1 var(--font-sans); color: var(--color-primary-400); }
.level-bar { height: 6px; background: rgba(255,255,255,0.15); border-radius: var(--radius-full); overflow: hidden; }
.level-bar-fill { height: 100%; background: linear-gradient(90deg, var(--color-primary-400), #F9A96A); border-radius: var(--radius-full); width: 68%; }
.level-stats { display: flex; margin-top: 16px; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 14px; }
.level-stat { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.level-stat + .level-stat { border-left: 1px solid rgba(255,255,255,0.1); }
.level-stat-val { font: var(--weight-bold) var(--text-xl)/1 var(--font-sans); color: #fff; }
.level-stat-label { font: var(--weight-medium) 10px/1 var(--font-sans); color: rgba(255,255,255,0.5); }

.section-tabs { display: flex; background: #fff; border-bottom: 1px solid var(--border-subtle); }
.section-tab { flex: 1; text-align: center; padding: 13px; font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-tertiary); border-bottom: 2.5px solid transparent; cursor: pointer; }
.section-tab.active { color: var(--color-primary-500); border-bottom-color: var(--color-primary-500); font-weight: var(--weight-semibold); }

.content { padding: 18px 20px 40px; }
.section-title { font: var(--weight-bold) var(--text-base)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; }
.section-more { font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); color: var(--text-secondary); }

.quest-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.quest-card { background: var(--surface-bg); border-radius: var(--radius-lg); padding: 14px; display: flex; gap: 12px; align-items: flex-start; }
.quest-icon { width: 44px; height: 44px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.quest-body { flex: 1; }
.quest-name { font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); margin-bottom: 4px; }
.quest-desc { font: var(--type-caption); color: var(--text-secondary); margin-bottom: 8px; }
.quest-progress-row { display: flex; justify-content: space-between; margin-bottom: 4px; }
.quest-progress-text { font: var(--type-caption); color: var(--text-tertiary); }
.quest-progress-val { font: var(--weight-semibold) var(--text-xs)/1 var(--font-sans); color: var(--color-primary-500); }
.quest-bar { height: 5px; background: var(--surface-subtle); border-radius: var(--radius-full); overflow: hidden; }
.quest-bar-fill { height: 100%; border-radius: var(--radius-full); }
.quest-reward { display: flex; align-items: center; gap: 4px; margin-top: 8px; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); }
.reward-badge { display: flex; align-items: center; gap: 3px; background: #FFF8E1; color: #F57F17; padding: 2px 7px; border-radius: var(--radius-full); font: var(--weight-semibold) 10px/1 var(--font-sans); }

.badge-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.badge-item { display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer; }
.badge-icon { width: 56px; height: 56px; border-radius: var(--radius-xl); display: flex; align-items: center; justify-content: center; position: relative; }
.badge-icon.earned { box-shadow: 0 2px 8px rgba(247,143,87,0.25); }
.badge-icon.locked { opacity: 0.35; filter: grayscale(1); }
.badge-lock { position: absolute; bottom: -3px; right: -3px; width: 18px; height: 18px; background: var(--color-neutral-300); border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; border: 2px solid #fff; }
.badge-name { font: var(--weight-medium) 10px/var(--leading-snug) var(--font-sans); color: var(--text-secondary); text-align: center; }
.badge-name.earned { color: var(--text-primary); font-weight: var(--weight-semibold); }
</style>
