# Created: 2026-06-19
<template>
  <div class="page">
    <!-- Header -->
    <header class="nav-header">
      <button class="back-btn" aria-label="뒤로가기" @click="goBack">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <span class="nav-title">운영 관리</span>
      <button class="refresh-btn" :disabled="loading" @click="loadPending">새로고침</button>
    </header>

    <div class="scroll-content">
      <section class="section">
        <h2 class="section-title">
          핫플 승인 대기<span class="section-count">{{ pending.length }}</span>
        </h2>

        <!-- Loading -->
        <div v-if="loading" class="status-block">
          <div class="spinner" />
        </div>

        <!-- Error -->
        <div v-else-if="error" class="status-block">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span class="status-text">{{ error }}</span>
          <button class="retry-btn" @click="loadPending">재시도</button>
        </div>

        <!-- Empty -->
        <div v-else-if="!pending.length" class="status-block">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 11-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
          </svg>
          <span class="status-text">승인 대기 중인 핫플이 없어요.</span>
        </div>

        <!-- List -->
        <div v-else class="list">
          <div v-for="item in pending" :key="item.id" class="card">
            <div class="card-info">
              <div class="card-title">{{ item.title || item.name || `핫플 #${item.id}` }}</div>
              <div v-if="item.address || item.region" class="card-sub">{{ item.address || item.region }}</div>
            </div>
            <div class="card-actions">
              <button class="btn btn-approve" :disabled="busyId === item.id" @click="onApprove(item)">승인</button>
              <button class="btn btn-reject" :disabled="busyId === item.id" @click="onReject(item)">반려</button>
            </div>
          </div>
        </div>
      </section>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { hotplaceApi } from '@/api/index.js'

const router = useRouter()

const pending = ref([])
const loading = ref(false)
const error = ref('')
const busyId = ref(null)

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/mypage')
}

async function loadPending() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await hotplaceApi.pending()
    pending.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch (e) {
    error.value = e.response?.data?.message ?? e.message ?? '목록을 불러오지 못했어요.'
    pending.value = []
  } finally {
    loading.value = false
  }
}

async function onApprove(item) {
  busyId.value = item.id
  try {
    await hotplaceApi.approve(item.id)
    pending.value = pending.value.filter((p) => p.id !== item.id)
  } catch (e) {
    error.value = e.response?.data?.message ?? e.message ?? '승인에 실패했어요.'
  } finally {
    busyId.value = null
  }
}

async function onReject(item) {
  busyId.value = item.id
  try {
    await hotplaceApi.reject(item.id)
    pending.value = pending.value.filter((p) => p.id !== item.id)
  } catch (e) {
    error.value = e.response?.data?.message ?? e.message ?? '반려에 실패했어요.'
  } finally {
    busyId.value = null
  }
}

onMounted(loadPending)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

/* ── Header ──────────────────────────────────────────────────────────────── */
.nav-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 52px 12px 12px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
  cursor: pointer;
}

.nav-title {
  flex: 1;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.refresh-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
  cursor: pointer;
  padding: 6px 4px;
}

.refresh-btn:disabled {
  color: var(--color-ink-muted);
  opacity: 0.5;
  cursor: default;
}

/* ── Scroll content ──────────────────────────────────────────────────────── */
.scroll-content {
  flex: 1;
  overflow-y: auto;
}

.section {
  padding-top: 4px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px 18px 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.section-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
}

/* ── Status (loading / error / empty) ────────────────────────────────────── */
.status-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 56px 24px;
  color: var(--color-ink-muted);
}

.status-text {
  font-size: 13.5px;
  letter-spacing: -0.2px;
  text-align: center;
}

.spinner {
  width: 26px;
  height: 26px;
  border: 2.5px solid var(--color-line);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.retry-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
  text-decoration: underline;
}

/* ── List ────────────────────────────────────────────────────────────────── */
.list {
  display: flex;
  flex-direction: column;
}

.card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-sub {
  font-size: 12px;
  color: var(--color-ink-muted);
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn {
  padding: 7px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.2px;
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.btn-approve {
  background: var(--color-peach);
  color: white;
}

.btn-reject {
  background: var(--color-surface);
  color: var(--color-ink-secondary);
}

.bottom-spacer {
  height: 24px;
}
</style>
