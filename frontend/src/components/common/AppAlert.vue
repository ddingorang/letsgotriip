<template>
  <Teleport to="body">
    <div class="alert-stack">
      <TransitionGroup name="alert">
        <div
          v-for="a in alerts"
          :key="a.id"
          class="alert-item"
          :class="`alert-${a.type}`"
          @click="dismiss(a.id)"
        >
          <span class="alert-icon">{{ icons[a.type] }}</span>
          <span class="alert-msg">{{ a.message }}</span>
          <button class="alert-close" @click.stop="dismiss(a.id)">✕</button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { useAlert } from '@/composables/useAlert.js'

const { alerts, dismiss } = useAlert()

const icons = { error: '✕', info: 'ℹ', success: '✓', warning: '⚠' }
</script>

<style scoped>
.alert-stack {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(calc(100vw - 32px), 400px);
  pointer-events: none;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
  font-size: 14px;
  font-weight: 500;
  pointer-events: auto;
  cursor: pointer;
  line-height: 1.4;
}

.alert-error   { background: #fff0f0; color: #c0392b; border-left: 4px solid var(--color-error); }
.alert-info    { background: #eff6ff; color: #1d4ed8; border-left: 4px solid var(--color-info); }
.alert-success { background: #f0fdf4; color: #15803d; border-left: 4px solid var(--color-success); }
.alert-warning { background: #fff7ed; color: #b45309; border-left: 4px solid var(--color-warning); }

.alert-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.alert-msg {
  flex: 1;
}

.alert-close {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  opacity: 0.5;
  padding: 0 2px;
  color: inherit;
  flex-shrink: 0;
}
.alert-close:hover { opacity: 1; }

.alert-enter-active { transition: all 0.25s ease; }
.alert-leave-active { transition: all 0.2s ease; }
.alert-enter-from   { opacity: 0; transform: translateY(-12px); }
.alert-leave-to     { opacity: 0; transform: translateY(-8px); }
</style>
