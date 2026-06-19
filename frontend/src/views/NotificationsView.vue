<template>
  <div class="page">
    <!-- Header -->
    <header class="nav-header">
      <button class="back-btn" aria-label="뒤로가기" @click="goBack">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </button>
      <span class="nav-title">알림</span>
      <button
        class="read-all-btn"
        :disabled="!store.hasUnread || tab !== 'alerts'"
        @click="store.markAllRead()"
      >모두 읽음</button>
    </header>

    <!-- Tabs -->
    <div class="tab-bar">
      <button :class="['tab', { active: tab === 'alerts' }]" @click="tab = 'alerts'">
        알림
        <span v-if="store.unreadCount" class="tab-badge">{{ store.unreadCount }}</span>
      </button>
      <button :class="['tab', { active: tab === 'notices' }]" @click="tab = 'notices'">공지사항</button>
    </div>

    <div class="scroll-content">
      <!-- ── 알림 ──────────────────────────────────────────────────────────── -->
      <template v-if="tab === 'alerts'">
        <div v-if="store.notifications.length" class="list">
          <div
            v-for="n in store.notifications"
            :key="n.id"
            :class="['notif-row', { unread: !n.read }]"
          >
            <span class="notif-icon" :class="`icon-${n.type}`" v-html="iconFor(n.type)" />
            <div class="notif-body">
              <div class="notif-top">
                <span class="notif-title">{{ n.title }}</span>
                <span class="notif-time">{{ n.time }}</span>
              </div>
              <p class="notif-text">{{ n.body }}</p>
            </div>
            <span v-if="!n.read" class="unread-dot" />
          </div>
        </div>
        <div v-else class="empty">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 01-3.46 0" />
          </svg>
          <span>{{ store.alertError ? '알림을 불러오지 못했어요' : '새로운 알림이 없어요' }}</span>
        </div>
      </template>

      <!-- ── 공지사항 ──────────────────────────────────────────────────────── -->
      <template v-else>
        <div v-if="store.notices.length" class="list">
          <div v-for="notice in store.notices" :key="notice.id" class="notice-row">
            <button class="notice-head" @click="toggle(notice.id)">
              <div class="notice-head-left">
                <span class="notice-cat" :class="catClass(notice.category)">{{ notice.category }}</span>
                <span class="notice-title">{{ notice.title }}</span>
              </div>
              <svg
                class="chevron"
                :class="{ open: openId === notice.id }"
                width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"
              ><path d="M6 9l6 6 6-6" /></svg>
            </button>
            <div class="notice-date">{{ notice.date }}</div>
            <p v-if="openId === notice.id" class="notice-body">{{ notice.body }}</p>
          </div>
        </div>
        <div v-else class="empty">
          <span>등록된 공지가 없어요</span>
        </div>
      </template>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification.js'

const router = useRouter()
const store = useNotificationStore()

const tab = ref('alerts')
const openId = ref(null)

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/mypage')
}

function toggle(id) {
  openId.value = openId.value === id ? null : id
}

function catClass(category) {
  if (category === '필독') return 'cat-must'
  if (category === '업데이트') return 'cat-update'
  return 'cat-info'
}

function iconFor(type) {
  const map = {
    companion: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/></svg>',
    community: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>',
    badge: '<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 17 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z"/></svg>',
  }
  return map[type] ?? map.community
}

onMounted(async () => {
  await store.refresh()
  store.subscribe() // 진입 후 실시간 알림 구독
})

onUnmounted(() => {
  store.unsubscribe() // 이탈 시 구독 해제
})
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

.read-all-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
  cursor: pointer;
  padding: 6px 4px;
}

.read-all-btn:disabled {
  color: var(--color-ink-muted);
  opacity: 0.5;
  cursor: default;
}

/* ── Tabs ────────────────────────────────────────────────────────────────── */
.tab-bar {
  display: flex;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}

.tab {
  flex: 1;
  padding: 13px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink-muted);
  position: relative;
  cursor: pointer;
}

.tab.active {
  color: var(--color-ink);
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 40%;
  height: 2.5px;
  background: var(--color-peach);
  border-radius: 2px;
}

.tab-badge {
  display: inline-block;
  min-width: 17px;
  padding: 1px 5px;
  margin-left: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--color-peach);
  border-radius: var(--radius-full);
  vertical-align: middle;
}

/* ── Scroll content ──────────────────────────────────────────────────────── */
.scroll-content {
  flex: 1;
  overflow-y: auto;
}

.list {
  display: flex;
  flex-direction: column;
}

/* ── 알림 row ─────────────────────────────────────────────────────────────── */
.notif-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  position: relative;
}

.notif-row.unread {
  background: var(--color-peach-light);
}

.notif-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
}

.icon-companion { background: #e8f0ff; color: #3a78d6; }
.icon-community { background: #fff0e8; color: var(--color-peach-pressed); }
.icon-badge { background: #fff6e0; color: #d6a93a; }

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 3px;
}

.notif-title {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.notif-time {
  font-size: 11px;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}

.notif-text {
  font-size: 12.5px;
  color: var(--color-ink-secondary);
  line-height: 1.4;
}

.unread-dot {
  position: absolute;
  top: 16px;
  right: 12px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-peach);
}

/* ── 공지 row ─────────────────────────────────────────────────────────────── */
.notice-row {
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  padding: 14px 18px;
}

.notice-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  cursor: pointer;
  text-align: left;
}

.notice-head-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.notice-cat {
  flex-shrink: 0;
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: var(--radius-full);
}

.cat-must { background: #ffe4e0; color: #d6433a; }
.cat-update { background: #e8f0ff; color: #3a78d6; }
.cat-info { background: var(--color-surface); color: var(--color-ink-secondary); }

.notice-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chevron {
  flex-shrink: 0;
  transition: transform 0.2s;
}

.chevron.open {
  transform: rotate(180deg);
}

.notice-date {
  font-size: 11.5px;
  color: var(--color-ink-muted);
  margin-top: 5px;
}

.notice-body {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-ink-secondary);
  white-space: pre-wrap;
}

/* ── Empty ───────────────────────────────────────────────────────────────── */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 64px 0;
  color: var(--color-ink-muted);
  font-size: 13.5px;
}

.bottom-spacer {
  height: 24px;
}
</style>
