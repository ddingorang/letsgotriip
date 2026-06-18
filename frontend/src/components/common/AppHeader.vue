# Created: 2026-06-16 13:24:56
<template>
  <header class="app-header" :class="{ transparent: transparent }">
    <button v-if="showBack" class="icon-btn" @click="goBack">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M19 12H5M12 5l-7 7 7 7" />
      </svg>
    </button>
    <span v-else class="header-spacer" />
    <h1 v-if="title" class="header-title">{{ title }}</h1>
    <div class="header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: false },
  transparent: { type: Boolean, default: false },
})

const router = useRouter()

function goBack() {
  router.back()
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
  z-index: 10;
}

.app-header.transparent {
  background: transparent;
  border-bottom: none;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: var(--color-ink);
  transition: background 0.15s;
}

.icon-btn:hover {
  background: var(--color-surface);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-spacer {
  width: 40px;
}
</style>
