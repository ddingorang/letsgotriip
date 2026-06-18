<template>
  <div class="page">
    <div class="scroll-content">
      <!-- Level hero card -->
      <div class="level-card">
        <div class="level-glow" />
        <div class="level-row">
          <div class="level-badge-icon">
            <span class="level-badge-text">LV<br /><span class="level-num">7</span></span>
          </div>
          <div class="level-info">
            <h2 class="level-title">열정 여행가</h2>
            <p class="level-exp">다음 레벨까지 340 XP 남음</p>
          </div>
        </div>
        <div class="level-progress-wrap">
          <div class="level-progress-labels">
            <span class="level-progress-text">LV 7 — 1,660 XP</span>
            <span class="level-progress-val">68% · LV 8까지 340 XP</span>
          </div>
          <div class="level-bar">
            <div class="level-bar-fill" />
          </div>
        </div>
        <div class="level-stats">
          <div v-for="stat in levelStats" :key="stat.label" class="level-stat">
            <span class="level-stat-val">{{ stat.val }}</span>
            <span class="level-stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tab-bar">
        <button
          v-for="(tab, i) in tabs"
          :key="tab"
          :class="['tab', { active: activeTab === i }]"
          @click="activeTab = i"
        >{{ tab }}</button>
      </div>

      <div class="content">
        <!-- Quests section -->
        <div class="section-header">
          <span class="section-title">이번 달 퀘스트</span>
          <span class="section-more">전체보기</span>
        </div>

        <div class="quest-list">
          <div v-for="quest in quests" :key="quest.id" class="quest-card">
            <div class="quest-icon" :style="{ background: quest.iconBg }">
              <svg v-if="quest.iconType === 'map'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="quest.iconStroke" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
              </svg>
              <svg v-else-if="quest.iconType === 'thumb'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="quest.iconStroke" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z" />
              </svg>
              <svg v-else-if="quest.iconType === 'people'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="quest.iconStroke" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <div class="quest-body">
              <div class="quest-name">{{ quest.name }}</div>
              <div class="quest-desc">{{ quest.desc }}</div>
              <div class="quest-progress-wrap">
                <div class="quest-progress-row">
                  <span class="quest-progress-text">{{ quest.progressText }}</span>
                  <span class="quest-progress-pct">{{ quest.progressVal }}</span>
                </div>
                <div class="quest-bar">
                  <div class="quest-bar-fill" :style="{ width: quest.progressWidth, background: quest.progressColor }" />
                </div>
              </div>
              <div class="quest-reward">
                보상:
                <div v-for="(r, ri) in quest.rewards" :key="ri" class="reward-chip" :style="r.style">{{ r.label }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Badges grid -->
        <div class="section-header" style="margin-top:8px">
          <span class="section-title">획득한 뱃지 (8/24)</span>
        </div>
        <div class="badge-grid">
          <div v-for="badge in badges" :key="badge.id" class="badge-item">
            <div
              class="badge-icon"
              :class="badge.earned ? 'earned' : 'locked'"
              :style="{ background: badge.bg }"
            >
              <svg v-if="badge.iconType === 'star'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" /></svg>
              <svg v-else-if="badge.iconType === 'wave'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" /></svg>
              <svg v-else-if="badge.iconType === 'location'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /></svg>
              <svg v-else-if="badge.iconType === 'food'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M18 8h1a4 4 0 0 1 0 8h-1" /><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z" /></svg>
              <svg v-else-if="badge.iconType === 'people2'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
              <svg v-else-if="badge.iconType === 'circle'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><circle cx="12" cy="12" r="10" /></svg>
              <svg v-else-if="badge.iconType === 'heart'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" /></svg>
              <div v-if="!badge.earned" class="badge-lock">
                <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                  <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
              </div>
            </div>
            <span class="badge-name" :class="{ earned: badge.earned }">{{ badge.name }}</span>
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activeTab = ref(0)
const tabs = ['진행중 퀘스트', '뱃지 컬렉션', '완료 기록']

const levelStats = [
  { val: '12', label: '여행 횟수' },
  { val: '8',  label: '획득 뱃지' },
  { val: '1,660', label: '총 XP' },
  { val: '3',  label: '진행 퀘스트' },
]

const quests = [
  {
    id: 1,
    iconBg: 'var(--color-peach-light)',
    iconStroke: 'var(--color-peach-pressed)',
    iconType: 'map',
    name: '여행 계획 마스터',
    desc: '여행 계획 5개 저장하기',
    progressText: '3/5 완료',
    progressVal: '60%',
    progressWidth: '60%',
    progressColor: 'var(--color-peach)',
    rewards: [
      { label: '⚡ 200 XP', style: '' },
      { label: '뱃지 획득', style: 'background:var(--color-peach-light);color:var(--color-peach-pressed)' },
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
      { label: '특별 뱃지', style: 'background:var(--color-peach-light);color:var(--color-peach-pressed)' },
    ],
  },
]

const badges = [
  { id: 1, earned: true,  bg: 'linear-gradient(135deg,#FFD700,#FFA500)', iconType: 'star',    name: '첫 여행' },
  { id: 2, earned: true,  bg: 'linear-gradient(135deg,#29B6F6,#0277BD)', iconType: 'wave',    name: '해변탐험가' },
  { id: 3, earned: true,  bg: 'linear-gradient(135deg,#66BB6A,#2E7D32)', iconType: 'location', name: '지도 마스터' },
  { id: 4, earned: true,  bg: 'linear-gradient(135deg,#f78f57,#e0743a)', iconType: 'food',    name: '미식 여행자' },
  { id: 5, earned: false, bg: 'linear-gradient(135deg,#CE93D8,#7B1FA2)', iconType: 'people2', name: '동행 매니저' },
  { id: 6, earned: false, bg: 'linear-gradient(135deg,#FFB74D,#E65100)', iconType: 'star',    name: '전국 일주' },
  { id: 7, earned: false, bg: 'linear-gradient(135deg,#80CBC4,#00695C)', iconType: 'circle',  name: 'AI 파워유저' },
  { id: 8, earned: false, bg: 'linear-gradient(135deg,#EF9A9A,#C62828)', iconType: 'heart',   name: '찜 컬렉터' },
]
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
}

/* Level hero card */
.level-card {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%);
  padding: 52px 20px 22px;
  position: relative;
  overflow: hidden;
}
.level-glow {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 140px;
  height: 140px;
  background: radial-gradient(circle, rgba(247, 143, 87, 0.2) 0%, transparent 70%);
  pointer-events: none;
}
.level-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}
.level-badge-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--color-peach), var(--color-peach-pressed));
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(247, 143, 87, 0.4);
}
.level-badge-text {
  font-size: 11px;
  font-weight: 800;
  color: white;
  text-align: center;
  line-height: 1.2;
}
.level-num {
  font-size: 18px;
}
.level-info {
  flex: 1;
}
.level-title {
  font-size: 20px;
  font-weight: 800;
  color: white;
  letter-spacing: -0.5px;
  margin-bottom: 4px;
}
.level-exp {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
}
.level-progress-wrap {
  margin-bottom: 16px;
}
.level-progress-labels {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}
.level-progress-text {
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.6);
}
.level-progress-val {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-peach);
}
.level-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.level-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-peach), #f9a96a);
  border-radius: var(--radius-full);
  width: 68%;
}
.level-stats {
  display: flex;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 14px;
}
.level-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.level-stat + .level-stat {
  border-left: 1px solid rgba(255, 255, 255, 0.1);
}
.level-stat-val {
  font-size: 18px;
  font-weight: 800;
  color: white;
  letter-spacing: -0.4px;
}
.level-stat-label {
  font-size: 10px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
}

/* Tabs */
.tab-bar {
  display: flex;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
}
.tab {
  flex: 1;
  padding: 13px 0;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  text-align: center;
  border-bottom: 2px solid transparent;
  letter-spacing: -0.2px;
  cursor: pointer;
  transition: color 0.15s;
}
.tab.active {
  color: var(--color-peach-pressed);
  font-weight: 700;
  border-bottom-color: var(--color-peach);
}

/* Content area */
.content {
  padding: 18px 20px 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.section-more {
  font-size: 13px;
  color: var(--color-ink-muted);
}

/* Quest cards */
.quest-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}
.quest-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  padding: 14px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.quest-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.quest-body {
  flex: 1;
  min-width: 0;
}
.quest-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  margin-bottom: 3px;
}
.quest-desc {
  font-size: 12px;
  color: var(--color-ink-secondary);
  margin-bottom: 8px;
}
.quest-progress-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.quest-progress-text {
  font-size: 11.5px;
  color: var(--color-ink-muted);
}
.quest-progress-pct {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--color-peach-pressed);
}
.quest-bar {
  height: 5px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.quest-bar-fill {
  height: 100%;
  border-radius: var(--radius-full);
}
.quest-reward {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--color-ink-secondary);
}
.reward-chip {
  display: flex;
  align-items: center;
  gap: 3px;
  background: #fff8e1;
  color: #f57f17;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 600;
}

/* Badge grid */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.badge-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.badge-icon.earned {
  box-shadow: 0 2px 8px rgba(247, 143, 87, 0.25);
}
.badge-icon.locked {
  opacity: 0.35;
  filter: grayscale(1);
}
.badge-lock {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 18px;
  height: 18px;
  background: var(--color-line);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
}
.badge-name {
  font-size: 10px;
  font-weight: 500;
  color: var(--color-ink-muted);
  text-align: center;
  line-height: 1.3;
}
.badge-name.earned {
  color: var(--color-ink);
  font-weight: 600;
}

.bottom-spacer {
  height: 32px;
}
</style>
