# Created: 2026-06-16 14:06:39
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="nav-title-col">
        <span class="nav-title">신청자 관리</span>
        <span class="nav-sub">{{ comp?.title }}</span>
      </div>
      <div style="width: 40px" />
    </header>

    <div class="summary-bar">
      <span class="summary-text">
        신청 <strong>{{ pending.length + approved.length }}</strong> · 승인 <strong>{{ approved.length }}</strong> / 정원 <strong>{{ comp?.maxCount }}</strong>
      </span>
    </div>

    <div class="scroll-content">
      <div
        v-for="applicant in companionStore.applicants"
        :key="applicant.id"
        class="applicant-card"
      >
        <div class="applicant-top">
          <div class="avatar" />
          <div class="applicant-info">
            <div class="applicant-name">{{ applicant.nickname }}</div>
            <div class="applicant-meta">
              {{ applicant.ageGroup }} · 동행 {{ applicant.tripCount }}회
              <span v-if="applicant.mannerScore"> · 매너 {{ applicant.mannerScore }}</span>
            </div>
          </div>
          <div v-if="applicant.status === 'approved'" class="approved-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            승인됨
          </div>
          <div v-else-if="applicant.status === 'rejected'" class="rejected-badge">거절됨</div>
        </div>

        <p class="applicant-msg">{{ applicant.message }}</p>

        <div v-if="applicant.status === 'pending'" class="action-row">
          <button class="reject-btn" @click="handleReject(applicant.id)">거절</button>
          <button class="approve-btn" @click="handleApprove(applicant.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            승인
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'

const route = useRoute()
const companionStore = useCompanionStore()

const postId = computed(() => route.params.id)
const comp = computed(() => companionStore.getById(postId.value))
const pending = computed(() => companionStore.applicants.filter((a) => a.status === 'pending'))
const approved = computed(() => companionStore.applicants.filter((a) => a.status === 'approved'))

onMounted(async () => {
  await companionStore.getApplications(postId.value)
})

async function handleApprove(applicationId) {
  await companionStore.approveApplicant(postId.value, applicationId)
  // refresh list so counts and statuses are up-to-date
  await companionStore.getApplications(postId.value)
}

async function handleReject(applicationId) {
  await companionStore.rejectApplicant(postId.value, applicationId)
  await companionStore.getApplications(postId.value)
}
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
.nav-title { font-size: 16px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.nav-sub { font-size: 11.5px; color: var(--color-ink-muted); }

.summary-bar {
  padding: 10px 20px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.summary-text { font-size: 13.5px; color: var(--color-ink-secondary); }
.summary-text strong { color: var(--color-ink); font-weight: 700; }

.scroll-content { flex: 1; overflow-y: auto; padding: 8px 0; }

.applicant-card {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-line-light);
}
.applicant-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
}
.applicant-info { flex: 1; }
.applicant-name { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.applicant-meta { font-size: 12px; color: var(--color-ink-muted); margin-top: 2px; }

.approved-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}
.rejected-badge {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-muted);
}

.applicant-msg {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
  line-height: 1.55;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
}

.action-row { display: flex; gap: 8px; }
.reject-btn {
  flex: 1;
  padding: 11px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}
.approve-btn {
  flex: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 11px;
  border-radius: var(--radius-lg);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
}
</style>
