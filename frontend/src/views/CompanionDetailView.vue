# Created: 2026-06-16 14:05:43
<template>
  <div class="page">
    <!-- Hero photo -->
    <div class="hero">
      <div class="hero-img" />
      <div class="hero-overlay" />
      <div class="hero-top">
        <button class="ghost-btn" @click="$router.back()">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
        </button>
        <div class="hero-top-right">
          <button v-if="comp.isOwner" class="ghost-btn" @click="$router.push(`/companion/${comp.id}/edit`)">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
          </button>
          <button class="ghost-btn" @click="share">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" /><line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="content-scroll">
      <!-- Status & title -->
      <div class="title-area">
        <div class="badges-row">
          <span v-if="comp.isOwner" class="badge owner-badge">내 모집글 · 방장</span>
          <span v-else :class="['badge', comp.status === '마감임박' ? 'badge-urgent' : 'badge-open']">{{ comp.status }}</span>
          <span class="date-range">{{ comp.dateRange }}</span>
        </div>
        <h1 class="comp-title">{{ comp.title }}</h1>

        <!-- Author -->
        <div class="author-row">
          <div class="author-avatar" />
          <div class="author-info">
            <span class="author-name">{{ comp.author?.nickname }}</span>
            <span class="author-sub">방장 · 동행 {{ comp.author?.tripCount }}회</span>
          </div>
          <span v-if="!comp.isOwner" class="seat-count">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
            {{ comp.currentCount }}/{{ comp.maxCount }} 모집 인원
          </span>
        </div>

        <!-- Owner: applicant status -->
        <div v-if="comp.isOwner" class="owner-status-row">
          <span class="owner-status-label">신청 현황</span>
          <span class="owner-status-val">대기 {{ comp.pendingCount }} · 승인 {{ comp.approvedCount }} / 정원 {{ comp.maxCount }}</span>
          <div class="avatar-stack">
            <div v-for="n in Math.min(comp.approvedCount + comp.pendingCount, 3)" :key="n" class="stack-avatar" />
          </div>
        </div>
      </div>

      <!-- Pending state for applied user -->
      <div v-if="isApplied && !comp.isOwner" class="pending-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
        </svg>
        <div>
          <div class="pending-title">승인 대기 중이에요</div>
          <div class="pending-sub">방장이 신청을 확인하면 채팅방에 입장할 수 있어요.</div>
        </div>
      </div>

      <!-- Info grid -->
      <div class="info-grid">
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
            <span class="info-label">일정</span>
          </div>
          <span class="info-val">{{ comp.dateRange }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
            <span class="info-label">지역</span>
          </div>
          <span class="info-val">{{ comp.location }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
            <span class="info-label">기간</span>
          </div>
          <span class="info-val">{{ comp.period }}</span>
        </div>
        <div class="info-cell">
          <div class="info-icon-row">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><line x1="12" y1="1" x2="12" y2="23" /><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" /></svg>
            <span class="info-label">예상 비용</span>
          </div>
          <span class="info-val">{{ comp.estimatedCost }}</span>
        </div>
      </div>

      <!-- Tags -->
      <div class="tags-row">
        <span v-for="tag in comp.tags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>

      <!-- Intro -->
      <div class="section">
        <h3 class="section-title">소개</h3>
        <p class="intro-text">{{ comp.intro }}</p>
      </div>

      <div style="height: 100px" />
    </div>

    <!-- Bottom CTA -->
    <div class="cta-bar">
      <div v-if="applyError" class="apply-error">{{ applyError }}</div>
      <!-- Visitor: not applied -->
      <template v-if="!comp.isOwner && !isApplied">
        <div class="seats-left">남은 자리 {{ comp.maxCount - comp.currentCount }}명</div>
        <button class="cta-main" :disabled="companionStore.loading" @click="apply">참여 신청하기</button>
      </template>

      <!-- Visitor: applied (pending) -->
      <template v-else-if="!comp.isOwner && isApplied">
        <button class="cta-cancel" @click="cancelApply">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          신청 취소
        </button>
      </template>

      <!-- Owner -->
      <template v-else>
        <button class="cta-chat" @click="$router.push(`/chat/1`)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
        </button>
        <button class="cta-main" @click="$router.push(`/companion/${comp.id}/applicants`)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /></svg>
          신청자 관리 {{ comp.pendingCount }}
        </button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'

const route = useRoute()
const router = useRouter()
const companionStore = useCompanionStore()

const comp = computed(() => companionStore.getById(route.params.id) ?? {
  id: route.params.id, title: '동행 모집', location: '-', dateRange: '-',
  status: '모집중', currentCount: 0, maxCount: 4, author: { nickname: '-', tripCount: 0 },
  period: '-', estimatedCost: '-', tags: [], intro: '',
  isOwner: false, pendingCount: 0, approvedCount: 0,
})

const isApplied = ref(false)
const applyError = ref('')

onMounted(async () => {
  await companionStore.getDetail(route.params.id)
})

async function apply() {
  applyError.value = ''
  try {
    await companionStore.join(comp.value.id)
    isApplied.value = true
  } catch {
    applyError.value = companionStore.error || '신청에 실패했어요.'
  }
}
function cancelApply() {
  // Cancellation endpoint not yet in BE; optimistic local only
  isApplied.value = false
}
function share() {
  if (navigator.share) navigator.share({ title: comp.value.title, url: location.href })
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

.hero {
  height: 260px;
  position: relative;
  flex-shrink: 0;
}
.hero-img {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #d8c8b8 0%, #c0b0a0 100%);
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.28) 0%, transparent 50%);
}
.hero-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  z-index: 1;
}
.hero-top-right { display: flex; gap: 8px; }
.ghost-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.25);
  border-radius: 50%;
  backdrop-filter: blur(4px);
}

.content-scroll { flex: 1; overflow-y: auto; }

.title-area {
  padding: 20px 20px 0;
  border-bottom: 1px solid var(--color-line-light);
  padding-bottom: 16px;
}
.badges-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.badge {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11.5px;
  font-weight: 600;
}
.badge-open { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.badge-urgent { background: #fff0e8; color: #d04010; }
.owner-badge { background: var(--color-peach); color: white; }
.date-range { font-size: 13px; color: var(--color-ink-muted); }

.comp-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.5px;
  margin-bottom: 14px;
}
.author-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
}
.author-info { display: flex; flex-direction: column; gap: 1px; flex: 1; }
.author-name { font-size: 14px; font-weight: 600; color: var(--color-ink); }
.author-sub { font-size: 12px; color: var(--color-ink-muted); }
.seat-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
}

.owner-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}
.owner-status-label { font-size: 12px; color: var(--color-ink-muted); white-space: nowrap; }
.owner-status-val { font-size: 13.5px; font-weight: 600; color: var(--color-ink); flex: 1; }
.avatar-stack { display: flex; }
.stack-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-line);
  border: 2px solid white;
  margin-left: -6px;
}
.stack-avatar:first-child { margin-left: 0; }

.pending-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 16px 20px;
  padding: 14px;
  background: var(--color-peach-light);
  border-radius: var(--radius-md);
}
.pending-title { font-size: 13.5px; font-weight: 700; color: var(--color-peach-pressed); margin-bottom: 2px; }
.pending-sub { font-size: 12.5px; color: var(--color-ink-secondary); line-height: 1.5; }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin: 16px 20px;
  background: var(--color-line-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-line-light);
}
.info-cell {
  padding: 14px 16px;
  background: var(--color-white);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.info-icon-row { display: flex; align-items: center; gap: 4px; }
.info-label { font-size: 12px; color: var(--color-ink-muted); }
.info-val { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 20px 20px;
}
.tag-chip {
  padding: 5px 12px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  font-size: 13px;
  color: var(--color-ink-secondary);
  font-weight: 500;
}

.section { padding: 0 20px 20px; }
.section-title { font-size: 16px; font-weight: 700; color: var(--color-ink); margin-bottom: 10px; letter-spacing: -0.3px; }
.intro-text { font-size: 14.5px; color: var(--color-ink-secondary); line-height: 1.7; letter-spacing: -0.2px; }

.cta-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 12px 20px calc(12px + var(--safe-bottom));
  background: white;
  border-top: 1px solid var(--color-line-light);
}
.seats-left {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  white-space: nowrap;
}
.cta-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 15px;
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--radius-xl);
  letter-spacing: -0.3px;
}
.cta-cancel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 15px;
  border: 1.5px solid var(--color-line);
  color: var(--color-ink-secondary);
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-xl);
}
.cta-chat {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-ink);
}
.apply-error {
  width: 100%;
  font-size: 13px;
  color: var(--color-error);
  font-weight: 500;
  text-align: center;
  padding-bottom: 4px;
}
</style>
