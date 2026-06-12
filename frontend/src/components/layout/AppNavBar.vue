<script setup>
import { useRouter } from 'vue-router';

defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true }
});

const router = useRouter();
</script>

<template>
  <header class="nav-bar">
    <button v-if="back" class="icon-btn" aria-label="뒤로가기" @click="router.back()">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
    </button>
    <span v-else class="nav-spacer" />
    <h1 class="nav-title">{{ title }}</h1>
    <span class="nav-right"><slot name="action" /></span>
  </header>
</template>

<style scoped>
.nav-bar {
  height: var(--nav-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: var(--z-raised);
}
.nav-title {
  font: var(--weight-semibold) var(--text-lg) / 1 var(--font-sans);
  color: var(--text-primary);
  letter-spacing: -0.02em;
}
.icon-btn,
.nav-spacer,
.nav-right {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  border-radius: var(--radius-sm);
}
</style>
