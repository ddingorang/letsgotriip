<template>
  <nav class="bottom-nav">
    <RouterLink to="/home" class="nav-item" :class="{ active: isActive('/home') }">
      <span class="nav-icon" v-html="homeIcon" />
      <span class="nav-label">홈</span>
    </RouterLink>
    <RouterLink to="/explore" class="nav-item" :class="{ active: isActive('/explore') }">
      <span class="nav-icon" v-html="exploreIcon" />
      <span class="nav-label">탐색</span>
    </RouterLink>

    <!-- Center FAB → 계획 페이지(내 계획·동행 / AI로 계획 추가) -->
    <RouterLink to="/plan" class="nav-item nav-ai">
      <span class="nav-ai-btn">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2"/>
          <path d="M16 2v4M8 2v4M3 10h18"/>
        </svg>
      </span>
      <span class="nav-label">계획</span>
    </RouterLink>

    <RouterLink to="/community" class="nav-item" :class="{ active: isActive('/community') }">
      <span class="nav-icon" v-html="communityIcon" />
      <span class="nav-label">커뮤니티</span>
    </RouterLink>
    <RouterLink to="/mypage" class="nav-item" :class="{ active: isActive('/mypage') }">
      <span class="nav-icon" v-html="mypageIcon" />
      <span class="nav-label">마이</span>
    </RouterLink>
  </nav>
</template>

<script setup>
import { useRoute, RouterLink } from 'vue-router'

const route = useRoute()

function isActive(path) {
  return route.path.startsWith(path)
}

const homeIcon = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <path d="M3 9.5L12 3l9 6.5V20a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z"/>
  <path d="M9 21V12h6v9"/>
</svg>`

const exploreIcon = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="11" cy="11" r="8"/>
  <path d="M21 21l-4.35-4.35"/>
</svg>`

const communityIcon = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
</svg>`

const mypageIcon = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
  <circle cx="12" cy="8" r="4"/>
  <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
</svg>`
</script>

<style scoped>
.bottom-nav {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: var(--bottom-nav-height);
  padding-bottom: var(--safe-bottom);
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  flex-shrink: 0;
  /* 명시적 최상위 레이어 — 페이지의 positioned 요소(FAB 등)가 네비 위로 새지 않도록 */
  position: relative;
  z-index: 100;
}

/* 위로 튀어나온 중앙 AI 버튼이 콘텐츠를 덮어도 항상 깔끔하게 위에 보이도록 */
.nav-ai {
  position: relative;
  z-index: 1;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 6px 12px;
  color: var(--color-ink-muted);
  transition: color 0.15s;
  flex: 1;
}

.nav-item.active {
  color: var(--color-peach);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: -0.2px;
}

/* Center AI FAB */
.nav-ai {
  color: var(--color-ink-muted);
  margin-top: -18px;
  padding-bottom: 0;
}

.nav-ai-btn {
  width: 52px;
  height: 52px;
  background: var(--color-peach);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(247, 143, 87, 0.4);
  margin-bottom: 4px;
}

.nav-ai:hover .nav-ai-btn,
.router-link-active.nav-ai .nav-ai-btn {
  background: var(--color-peach-pressed);
}
</style>
