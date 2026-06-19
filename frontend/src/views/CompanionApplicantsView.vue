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
      <!-- 로딩 -->
      <div v-if="companionStore.applicantsLoading && companionStore.applicants.length === 0" class="app-state">
        <div class="app-skeleton" />
        <div class="app-skeleton" />
        <div class="app-skeleton" />
      </div>

      <!-- 권한 없음(403) -->
      <div v-else-if="companionStore.applicantsForbidden" class="app-state app-state-msg">
        <div class="app-state-icon">
          <svg width="46" height="46" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" />
          </svg>
        </div>
        <p class="app-state-title">신청자 목록을 볼 수 없어요</p>
        <p class="app-state-sub">이 모집글의 방장만 신청자를 관리할 수 있어요.</p>
        <button class="app-state-btn" @click="$router.back()">돌아가기</button>
      </div>

      <!-- 로드 실패 -->
      <div v-else-if="companionStore.applicantsError" class="app-state app-state-msg">
        <div class="app-state-icon">
          <svg width="46" height="46" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <p class="app-state-title">신청자 목록을 불러오지 못했어요</p>
        <p class="app-state-sub">{{ companionStore.applicantsError }}</p>
        <button class="app-state-btn" :disabled="companionStore.applicantsLoading" @click="reload">
          {{ companionStore.applicantsLoading ? '불러오는 중...' : '다시 시도' }}
        </button>
      </div>

      <!-- 신청자 없음 -->
      <div v-else-if="companionStore.applicants.length === 0" class="app-state app-state-msg">
        <div class="app-state-icon">
          <svg width="46" height="46" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" />
          </svg>
        </div>
        <p class="app-state-title">아직 신청자가 없어요</p>
        <p class="app-state-sub">새 신청이 들어오면 여기에 표시돼요.</p>
      </div>

      <template v-else>
      <div
        v-for="applicant in companionStore.applicants"
        :key="applicant.id"
        class="applicant-card"
      >
        <div class="applicant-top">
          <div class="avatar" />
          <div class="applicant-info">
            <div class="applicant-name">{{ applicant.nickname }}</div>
            <div v-if="metaText(applicant)" class="applicant-meta">{{ metaText(applicant) }}</div>
          </div>
          <div v-if="applicant.status === 'approved'" class="approved-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            승인됨
          </div>
          <div v-else-if="applicant.status === 'rejected'" class="rejected-badge">거절됨</div>
        </div>

        <p v-if="applicant.message" class="applicant-msg">{{ applicant.message }}</p>
        <p v-else class="applicant-msg applicant-msg-empty">남긴 메시지가 없어요.</p>

        <div v-if="applicant.status === 'pending'" class="action-row">
          <button class="reject-btn" @click="handleReject(applicant.id)">거절</button>
          <button class="approve-btn" @click="handleApprove(applicant.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
            승인
          </button>
        </div>
      </div>
      </template>
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

// 신청자 메타(연령대·동행 횟수·매너점수)는 보유한 값만 모아 노출.
// 통계 미보유 항목(null)은 표시하지 않는다.
function metaText(applicant) {
  const parts = []
  if (applicant.ageGroup) parts.push(applicant.ageGroup)
  if (applicant.tripCount != null) parts.push(`동행 ${applicant.tripCount}회`)
  if (applicant.mannerScore != null) parts.push(`매너 ${applicant.mannerScore}`)
  return parts.join(' · ')
}

onMounted(async () => {
  await companionStore.getApplications(postId.value)
})

/** 신청자 목록 재시도 — 로드 실패 화면의 "다시 시도" 버튼용 */
async function reload() {
  await companionStore.getApplications(postId.value)
}

async function handleApprove(applicationId) {
  try {
    await companionStore.approveApplicant(postId.value, applicationId)
  } catch {
    // 실패(정원 초과/이미 처리됨 등)는 store.error 로 노출. 서버 기준으로 목록 재동기화.
  }
  // refresh list so counts and statuses are up-to-date
  await companionStore.getApplications(postId.value)
}

async function handleReject(applicationId) {
  try {
    await companionStore.rejectApplicant(postId.value, applicationId)
  } catch {
    // 실패는 store.error 로 노출. 서버 기준으로 목록 재동기화.
  }
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

/* ── Load states (로딩/권한없음/오류/없음) ────────────────────────────────── */
.app-state { padding: 16px 20px; }
.app-skeleton {
  height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  margin-bottom: 12px;
  animation: app-shimmer 1.2s infinite;
}
@keyframes app-shimmer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}
.app-state-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 64px 24px;
  gap: 6px;
}
.app-state-icon { margin-bottom: 6px; }
.app-state-title {
  font-size: 15.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.app-state-sub {
  font-size: 13px;
  color: var(--color-ink-muted);
  margin-bottom: 12px;
  line-height: 1.5;
}
.app-state-btn {
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 600;
  padding: 11px 26px;
  border-radius: var(--radius-full);
  letter-spacing: -0.2px;
}
.app-state-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

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
.applicant-msg-empty { color: var(--color-ink-muted); font-style: italic; }

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
