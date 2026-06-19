# Created: 2026-06-16 14:27:18
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="nav-title-col">
        <span class="nav-title">이달의 챌린지</span>
        <span class="nav-sub">{{ ch?.month ?? '' }} · 여행자 뱃지 챌린지</span>
      </div>
      <div style="width: 40px" />
    </header>

    <div class="scroll-content">
      <!-- Icon -->
      <div class="challenge-icon-wrap">
        <div class="challenge-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="white" stroke="none">
            <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" />
          </svg>
        </div>
      </div>

      <!-- Title -->
      <div class="challenge-body">
        <h1 class="challenge-title">{{ ch?.title ?? '이달의 챌린지' }}</h1>
        <p class="challenge-desc">이번 달 계획에 새로운 장소를 {{ ch?.goal ?? 10 }}곳 담으면 보상 뱃지를 드려요</p>

        <!-- Progress display -->
        <div class="progress-display">
          <span class="progress-current">{{ ch?.current ?? 0 }}</span>
          <span class="progress-sep"> / </span>
          <span class="progress-total">{{ ch?.goal ?? 10 }}곳</span>
        </div>

        <div class="progress-bar-wrap">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: (ch?.percent ?? 0) + '%' }" />
          </div>
        </div>
        <p class="progress-hint">{{ ch?.hint ?? '' }}</p>

        <!-- 달성 조건 -->
        <div class="section">
          <h3 class="section-title">달성 조건</h3>
          <div class="condition-list">
            <div class="condition-item">
              <div class="condition-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
              </div>
              <div>
                <div class="condition-label">새로운 장소 방문</div>
                <div class="condition-sub">다녀온 곳에 체크인 시 1곳 인정</div>
              </div>
            </div>
            <div class="condition-item">
              <div class="condition-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
              </div>
              <div>
                <div class="condition-label">기간</div>
                <div class="condition-sub">6월 1일 ~ 6월 30일</div>
              </div>
            </div>
            <div class="condition-item">
              <div class="condition-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
              </div>
              <div>
                <div class="condition-label">대상</div>
                <div class="condition-sub">전체 회원</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 달성 보상 -->
        <div class="section">
          <h3 class="section-title">달성 보상</h3>
          <div class="reward-card">
            <div class="reward-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 12 20 22 4 22 4 12" /><rect x="2" y="7" width="20" height="5" /><line x1="12" y1="22" x2="12" y2="7" /><path d="M12 7H7.5a2.5 2.5 0 010-5C11 2 12 7 12 7z" /><path d="M12 7h4.5a2.5 2.5 0 000-5C13 2 12 7 12 7z" />
              </svg>
            </div>
            <div>
              <div class="reward-title">여행자 뱃지</div>
              <div class="reward-sub">달성 시 자동 지급돼요. 프로필 뱃지 탭에서 확인할 수 있어요.</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useGamificationStore } from '@/stores/gamification.js'

const gamiStore = useGamificationStore()
const ch = computed(() => gamiStore.summary?.challenge)

onMounted(() => gamiStore.load())
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.nav-title-col { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.nav-title { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.nav-sub { font-size: 11.5px; color: var(--color-ink-muted); }

.scroll-content { flex: 1; overflow-y: auto; }

.challenge-icon-wrap {
  display: flex;
  justify-content: center;
  padding: 32px 0 20px;
}
.challenge-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(247, 143, 87, 0.35);
}

.challenge-body { padding: 0 24px 32px; text-align: center; }
.challenge-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.7px;
  margin-bottom: 8px;
}
.challenge-desc {
  font-size: 14px;
  color: var(--color-ink-muted);
  line-height: 1.55;
  margin-bottom: 28px;
}

.progress-display {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
  margin-bottom: 12px;
}
.progress-current {
  font-size: 48px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -2px;
}
.progress-sep { font-size: 28px; color: var(--color-ink-muted); }
.progress-total { font-size: 22px; font-weight: 700; color: var(--color-ink-muted); }

.progress-bar-wrap { padding: 0 0 8px; }
.progress-bar {
  height: 8px;
  background: var(--color-line-light);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--color-peach);
  border-radius: 4px;
}
.progress-hint {
  font-size: 13px;
  color: var(--color-ink-muted);
  margin-bottom: 32px;
}

.section { text-align: left; margin-bottom: 24px; }
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 12px;
}

.condition-list { display: flex; flex-direction: column; gap: 14px; }
.condition-item { display: flex; align-items: flex-start; gap: 12px; }
.condition-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.condition-label { font-size: 14px; font-weight: 600; color: var(--color-ink); margin-bottom: 2px; }
.condition-sub { font-size: 12.5px; color: var(--color-ink-muted); }

.reward-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--color-peach-light);
  border-radius: var(--radius-lg);
  border: 1px solid #fde8d4;
}
.reward-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.reward-title { font-size: 14px; font-weight: 700; color: var(--color-ink); margin-bottom: 4px; }
.reward-sub { font-size: 12.5px; color: var(--color-ink-secondary); line-height: 1.5; }
</style>
