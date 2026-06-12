<script setup>
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const tabs = [
  { key: 'home', label: '홈', to: '/' },
  { key: 'search', label: '탐색', to: '/search' },
  { key: 'community', label: '커뮤니티', to: '/community' },
  { key: 'mypage', label: '마이', to: '/mypage' }
];

function isActive(to) {
  if (to === '/') return route.path === '/';
  return route.path.startsWith(to);
}
</script>

<template>
  <nav class="tab-bar">
    <button class="tab-item" :class="{ active: isActive(tabs[0].to) }" @click="router.push(tabs[0].to)">
      <span class="tab-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>
      </span>
      <span class="tab-label">홈</span>
    </button>
    <button class="tab-item" :class="{ active: isActive(tabs[1].to) }" @click="router.push(tabs[1].to)">
      <span class="tab-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
      </span>
      <span class="tab-label">탐색</span>
    </button>
    <button class="tab-center" @click="router.push('/ai')">
      <span class="tab-center-btn">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
      </span>
      <span class="tab-center-label">AI 추천</span>
    </button>
    <button class="tab-item" :class="{ active: isActive(tabs[2].to) }" @click="router.push(tabs[2].to)">
      <span class="tab-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      </span>
      <span class="tab-label">커뮤니티</span>
    </button>
    <button class="tab-item" :class="{ active: isActive(tabs[3].to) }" @click="router.push(tabs[3].to)">
      <span class="tab-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
      </span>
      <span class="tab-label">마이</span>
    </button>
  </nav>
</template>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  display: flex;
  align-items: flex-start;
  background: var(--surface-bg);
  border-top: 1px solid var(--border-subtle);
  padding: 10px 0 max(10px, env(safe-area-inset-bottom));
  height: var(--tab-bar-height);
  z-index: var(--z-raised);
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-tertiary);
  transition: color var(--duration-fast);
}
.tab-item.active {
  color: var(--color-primary-500);
}
.tab-icon {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tab-label {
  font: var(--weight-medium) 10px/1 var(--font-sans);
}
.tab-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-top: -18px;
}
.tab-center-btn {
  width: 52px;
  height: 52px;
  background: var(--color-primary-500);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 98, 0, 0.35);
}
.tab-center-label {
  font: var(--weight-medium) 10px/1 var(--font-sans);
  color: var(--text-tertiary);
  margin-top: 4px;
}
</style>
