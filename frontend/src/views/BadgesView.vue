<template>
  <div class="page">
    <!-- 로드 실패: 가짜 Lv1·0XP 를 그리지 않고 에러 + 재시도 -->
    <div v-if="error" class="state-box">
      <p class="state-text">{{ error }}</p>
      <button class="retry-btn" @click="reload">다시 시도</button>
    </div>

    <!-- 로딩: 스켈레톤 -->
    <div v-else-if="loading" class="state-box">
      <div class="skeleton skel-hero" />
      <div class="skeleton skel-line" />
      <div class="skeleton skel-line short" />
    </div>

    <!-- 정상: 로딩 아님 + 에러 없음 + 실제 summary 있을 때만 -->
    <div v-else-if="ready" class="scroll-content">
      <!-- Level hero card -->
      <div class="level-card">
        <div class="level-glow" />
        <div class="level-row">
          <div class="level-badge-icon">
            <span class="level-badge-text">LV<br /><span class="level-num">{{ level.level }}</span></span>
          </div>
          <div class="level-info">
            <h2 class="level-title">{{ level.title }}</h2>
            <p class="level-exp">다음 레벨까지 {{ level.expToNext }} XP 남음</p>
          </div>
        </div>
        <div class="level-progress-wrap">
          <div class="level-progress-labels">
            <span class="level-progress-text">LV {{ level.level }} — {{ level.expLabel }} XP</span>
            <span class="level-progress-val">{{ level.percent }}% · LV {{ level.level + 1 }}까지 {{ level.expToNext }} XP</span>
          </div>
          <div class="level-bar">
            <div class="level-bar-fill" :style="{ width: level.percent + '%' }" />
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
        <!-- 퀘스트 섹션 (탭별 목록 분기: 진행중 / 완료) -->
        <template v-if="showQuests">
          <div class="section-header">
            <span class="section-title">{{ questSectionTitle }}</span>
          </div>
          <div v-if="shownQuests.length" class="quest-list">
            <div v-for="quest in shownQuests" :key="quest.code" class="quest-card">
              <div class="quest-icon" :style="{ background: questIconBg(quest.iconType) }">
                <svg v-if="quest.iconType === 'map'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="questIconStroke(quest.iconType)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
                <svg v-else-if="quest.iconType === 'thumb'" width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="questIconStroke(quest.iconType)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z" />
                </svg>
                <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="questIconStroke(quest.iconType)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
                    <span class="quest-progress-pct">{{ quest.percent }}%</span>
                  </div>
                  <div class="quest-bar">
                    <div class="quest-bar-fill" :style="{ width: quest.percent + '%', background: questIconStroke(quest.iconType) }" />
                  </div>
                </div>
                <div class="quest-reward">
                  보상:
                  <div class="reward-chip">⚡ {{ quest.rewardExp }} XP</div>
                  <div v-if="quest.completed" class="reward-chip done">완료</div>
                </div>
              </div>
            </div>
          </div>
          <p v-else class="empty-text">{{ questEmptyText }}</p>
        </template>

        <!-- 뱃지 섹션 (탭별 목록 분기: 전체 / 획득) -->
        <template v-if="showBadges">
          <div class="section-header" :style="showQuests ? 'margin-top:24px' : ''">
            <span class="section-title">{{ badgeSectionTitle }}</span>
          </div>
          <div v-if="shownBadges.length" class="badge-grid">
            <div v-for="badge in shownBadges" :key="badge.id" class="badge-item">
              <div
                class="badge-icon"
                :class="badge.earned ? 'earned' : 'locked'"
                :style="{ background: badge.bg }"
              >
                <svg v-if="badge.iconType === 'star'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" /></svg>
                <svg v-else-if="badge.iconType === 'calendar'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
                <svg v-else-if="badge.iconType === 'location'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><circle cx="12" cy="11" r="3" /></svg>
                <svg v-else-if="badge.iconType === 'map'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" /></svg>
                <svg v-else-if="badge.iconType === 'people'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
                <svg v-else-if="badge.iconType === 'check'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" /></svg>
                <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><circle cx="12" cy="12" r="10" /></svg>
                <div v-if="!badge.earned" class="badge-lock">
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                    <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </div>
              </div>
              <span class="badge-name" :class="{ earned: badge.earned }">{{ badge.name }}</span>
              <span v-if="!badge.earned && badge.progressText" class="badge-progress">{{ badge.progressText }}</span>
            </div>
          </div>
          <p v-else class="empty-text">{{ badgeEmptyText }}</p>
        </template>
      </div>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useGamificationStore } from '@/stores/gamification.js'
import { gamificationApi } from '@/api/index.js'

const gamiStore = useGamificationStore()
// error/loading 을 구독해 로드 실패를 'Lv1·0XP·빈 챌린지'의 가짜 성공으로 위장하지 않는다.
const { loading, error } = storeToRefs(gamiStore)
// 정상 렌더는 로딩 아님 + 에러 없음 + 실제 summary 가 있을 때만.
const ready = computed(() => !loading.value && !error.value && !!gamiStore.summary)
// 전체 퀘스트(완료 포함)는 summary 가 아니라 quests 엔드포인트로 받는다.
// (summary.quests 는 미완료만 담는다)
const allQuests = ref([])

async function reload() {
  await gamiStore.refresh()
  // summary 로드 실패 시에는 퀘스트 목록을 덮어쓰지 않는다(에러 화면으로 분기됨).
  if (gamiStore.error) return
  try {
    const { data } = await gamificationApi.quests()
    allQuests.value = Array.isArray(data) ? data : []
  } catch {
    allQuests.value = []
  }
}

onMounted(reload)

const activeTab = ref(0)
const tabs = ['진행중 퀘스트', '뱃지 컬렉션', '완료 기록']

// ── 레벨 (summary.level → 표시 모델) ─────────────────────────────
// 레벨 구간(EXP_PER_LEVEL=100) 기준 단순 칭호. BE 는 칭호를 주지 않으므로 FE 에서 파생.
const LEVEL_TITLES = ['새내기 여행자', '초보 여행가', '열정 여행가', '베테랑 여행가', '전설의 여행가']
function titleFor(lvl) {
  return LEVEL_TITLES[Math.min(LEVEL_TITLES.length - 1, Math.floor((lvl - 1) / 3))]
}
const level = computed(() => {
  const l = gamiStore.summary?.level
  const lvl = l?.level ?? 1
  const exp = l?.exp ?? 0
  const into = l?.expIntoLevel ?? 0
  const span = l?.expForNextLevel ?? 100
  return {
    level: lvl,
    title: titleFor(lvl),
    percent: l?.percent ?? 0,
    expLabel: exp.toLocaleString(),
    expToNext: Math.max(0, span - into),
  }
})

// ── 통계 (summary.stats) ────────────────────────────────────────
const levelStats = computed(() => {
  const s = gamiStore.summary?.stats
  return [
    { val: String(s?.plans ?? 0), label: '여행 계획' },
    { val: String(s?.badges ?? 0), label: '획득 뱃지' },
    { val: (gamiStore.summary?.level?.exp ?? 0).toLocaleString(), label: '총 XP' },
    { val: String(activeQuests.value.length), label: '진행 퀘스트' },
  ]
})

// ── 뱃지 (summary.badges → 표시 shape) ──────────────────────────
const UNLOCKED_BG = 'linear-gradient(135deg,#FFD700,#FFA500)'
const LOCKED_BG = 'linear-gradient(135deg,#cfc8c2,#a8a09a)'
const badges = computed(() =>
  (gamiStore.summary?.badges ?? []).map((b) => ({
    id: b.key,
    earned: b.unlocked,
    name: b.name,
    iconType: b.iconType, // star/calendar/location/map/check/people (BadgeItem 가 직접 분기)
    progressText: b.progressText,
    bg: b.unlocked ? UNLOCKED_BG : LOCKED_BG,
  })),
)
const earnedBadges = computed(() => badges.value.filter((b) => b.earned))
const earnedCount = computed(() => earnedBadges.value.length)

// ── 퀘스트 (quests 엔드포인트 → QuestDto 그대로 사용) ───────────
const activeQuests = computed(() => allQuests.value.filter((q) => !q.completed))
const completedQuests = computed(() => allQuests.value.filter((q) => q.completed))

// 퀘스트 아이콘 톤 (BE iconType: map/thumb/people)
const QUEST_ICON = {
  map: { bg: 'var(--color-peach-light)', stroke: 'var(--color-peach-pressed)' },
  thumb: { bg: '#E8F5E9', stroke: '#2E7D32' },
  people: { bg: '#FFF3E0', stroke: '#E65100' },
}
function questIconBg(t) {
  return (QUEST_ICON[t] ?? QUEST_ICON.map).bg
}
function questIconStroke(t) {
  return (QUEST_ICON[t] ?? QUEST_ICON.map).stroke
}

// ── 탭별 콘텐츠 분기 ────────────────────────────────────────────
// 0: 진행중 퀘스트 / 1: 뱃지 컬렉션(전체) / 2: 완료 기록(획득 뱃지 + 완료 퀘스트)
const showQuests = computed(() => activeTab.value === 0 || activeTab.value === 2)
const showBadges = computed(() => activeTab.value === 1 || activeTab.value === 2)
const shownQuests = computed(() =>
  activeTab.value === 0 ? activeQuests.value : completedQuests.value,
)
const shownBadges = computed(() =>
  activeTab.value === 1 ? badges.value : earnedBadges.value,
)
const questSectionTitle = computed(() =>
  activeTab.value === 0 ? '진행중 퀘스트' : `완료한 퀘스트 (${completedQuests.value.length})`,
)
const questEmptyText = computed(() =>
  activeTab.value === 0 ? '진행 중인 퀘스트가 없어요.' : '아직 완료한 퀘스트가 없어요.',
)
const badgeSectionTitle = computed(() =>
  activeTab.value === 1
    ? `뱃지 컬렉션 (${earnedCount.value}/${badges.value.length})`
    : `획득한 뱃지 (${earnedBadges.value.length})`,
)
const badgeEmptyText = computed(() =>
  activeTab.value === 1 ? '아직 뱃지가 없어요.' : '아직 획득한 뱃지가 없어요.',
)
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
  width: 0;
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
.reward-chip.done {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}

.empty-text {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-ink-muted);
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
.badge-progress {
  font-size: 9.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

.bottom-spacer {
  height: 32px;
}

/* 로드 실패 / 로딩 상태 */
.state-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 56px 24px;
  text-align: center;
}
.state-text {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
  line-height: 1.5;
}
.retry-btn {
  padding: 10px 22px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.skeleton {
  background: linear-gradient(90deg, var(--color-line-light) 25%, var(--color-surface) 50%, var(--color-line-light) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-md);
  animation: skel-shine 1.2s ease-in-out infinite;
}
.skel-hero { width: 100%; height: 120px; border-radius: var(--radius-lg); }
.skel-line { width: 80%; height: 14px; }
.skel-line.short { width: 50%; }
@keyframes skel-shine {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
