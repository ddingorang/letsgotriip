<template>
  <Teleport to="body">
    <Transition name="confirm">
      <div v-if="state.visible" class="prompt-backdrop" @click.self="answer(null)">
        <div class="prompt-dialog" role="dialog" aria-modal="true">
          <p class="prompt-label">{{ state.label }}</p>
          <input
            ref="inputEl"
            v-model="inputVal"
            class="prompt-input"
            type="text"
            :placeholder="state.placeholder"
            @keydown.enter="submit"
            @keydown.esc="answer(null)"
          />
          <div class="prompt-actions">
            <button class="btn-cancel" @click="answer(null)">취소</button>
            <button class="btn-ok" @click="submit">확인</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { usePrompt } from '@/composables/usePrompt.js'

const { state, answer } = usePrompt()
const inputEl = ref(null)
const inputVal = ref('')

watch(
  () => state.visible,
  (v) => {
    if (v) {
      inputVal.value = ''
      nextTick(() => inputEl.value?.focus())
    }
  },
)

function submit() {
  answer(inputVal.value.trim() || null)
}
</script>

<style scoped>
.prompt-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.prompt-dialog {
  background: var(--surface-card, #fff);
  border-radius: 16px;
  padding: 24px 20px 20px;
  width: 100%;
  max-width: 320px;
  box-shadow: var(--shadow-lg);
}

.prompt-label {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.5;
  margin: 0 0 14px;
  word-break: keep-all;
}

.prompt-input {
  width: 100%;
  padding: 11px 14px;
  border: 1.5px solid var(--border-default, #c4c4c4);
  border-radius: 10px;
  font-size: 15px;
  color: var(--text-primary);
  background: var(--surface-subtle, #f5f5f5);
  outline: none;
  margin-bottom: 16px;
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.prompt-input:focus {
  border-color: var(--color-primary-500, #f78f57);
  background: #fff;
}

.prompt-actions {
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
.confirm-enter-active .prompt-dialog,
.confirm-leave-active .prompt-dialog { transition: transform 0.2s ease; }
.confirm-enter-from,
.confirm-leave-to { opacity: 0; }
.confirm-enter-from .prompt-dialog { transform: scale(0.93) translateY(8px); }
.confirm-leave-to .prompt-dialog  { transform: scale(0.95); }
</style>
