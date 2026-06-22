<template>
  <Teleport to="body">
    <Transition name="confirm">
      <div v-if="state.visible" class="confirm-backdrop" @click.self="answer(false)">
        <div class="confirm-dialog" role="dialog" aria-modal="true">
          <p class="confirm-msg">{{ state.message }}</p>
          <div class="confirm-actions">
            <button class="btn-cancel" @click="answer(false)">취소</button>
            <button class="btn-ok" @click="answer(true)">확인</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useConfirm } from '@/composables/useConfirm.js'

const { state, answer } = useConfirm()
</script>

<style scoped>
.confirm-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.confirm-dialog {
  background: var(--surface-card, #fff);
  border-radius: 16px;
  padding: 24px 20px 20px;
  width: 100%;
  max-width: 320px;
  box-shadow: var(--shadow-lg);
}

.confirm-msg {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.5;
  margin: 0 0 20px;
  word-break: keep-all;
}

.confirm-actions {
  display: flex;
  gap: 8px;
}

.btn-cancel,
.btn-ok {
  flex: 1;
  padding: 12px 0;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-cancel:hover,
.btn-ok:hover { opacity: 0.85; }

.btn-cancel {
  background: var(--surface-subtle, #f5f5f5);
  color: var(--text-secondary);
}

.btn-ok {
  background: var(--color-primary-500, #f78f57);
  color: #fff;
}

.confirm-enter-active,
.confirm-leave-active { transition: opacity 0.2s ease; }
.confirm-enter-active .confirm-dialog,
.confirm-leave-active .confirm-dialog { transition: transform 0.2s ease; }
.confirm-enter-from,
.confirm-leave-to { opacity: 0; }
.confirm-enter-from .confirm-dialog { transform: scale(0.93) translateY(8px); }
.confirm-leave-to .confirm-dialog  { transform: scale(0.95); }
</style>
