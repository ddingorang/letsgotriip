<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.js';

const router = useRouter();
const auth = useAuthStore();

onMounted(async () => {
  try {
    await auth.refresh();
    await auth.fetchMe();
    router.replace('/');
  } catch {
    router.replace('/login');
  }
});
</script>

<template>
  <div class="callback-view">
    <div class="spinner-wrap">
      <div class="spinner"></div>
      <p class="msg">로그인 처리 중이에요…</p>
    </div>
  </div>
</template>

<style scoped>
.callback-view {
  min-height: 100%;
  background: var(--surface-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}
.spinner-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-primary-100);
  border-top-color: var(--color-primary-500);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.msg {
  font: var(--weight-medium) var(--text-sm)/1 var(--font-sans);
  color: var(--text-secondary);
  margin: 0;
}
</style>
