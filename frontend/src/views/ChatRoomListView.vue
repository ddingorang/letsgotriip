# Created: 2026-06-16 14:06:54
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="nav-title-col">
        <span class="nav-title">참여 중인 방</span>
        <span class="nav-sub">동행 채팅방 {{ companionStore.myRooms.length }}</span>
      </div>
      <div style="width: 40px" />
    </header>

    <div class="scroll-content">
      <div
        v-for="room in companionStore.myRooms"
        :key="room.id"
        class="room-item"
        :class="{ ended: room.ended }"
        @click="$router.push(`/chat/${room.id}`)"
      >
        <div class="room-avatar">
          <div class="avatar-placeholder" />
        </div>
        <div class="room-info">
          <div class="room-title-row">
            <span class="room-title">{{ room.title }}</span>
            <span v-if="!room.ended" :class="['room-d-badge', { urgent: room.daysLeft !== null && room.daysLeft <= 3 }]">
              D-{{ room.daysLeft }}
            </span>
            <span v-else class="room-ended-badge">종료</span>
          </div>
          <div class="room-last-msg">{{ room.lastMsg }}</div>
        </div>
        <div class="room-right">
          <span class="room-time">{{ room.time }}</span>
          <span v-if="room.unreadCount > 0" class="unread-badge">{{ room.unreadCount }}</span>
          <span v-else class="read-label">최 {{ room.unreadCount || '0' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useCompanionStore } from '@/stores/companion.js'

const companionStore = useCompanionStore()
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

.scroll-content { flex: 1; overflow-y: auto; }

.room-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-line-light);
  cursor: pointer;
  transition: background 0.1s;
}
.room-item:active { background: var(--color-surface); }
.room-item.ended { opacity: 0.6; }

.room-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e8ddd4, #d8c8b8);
}

.room-info { flex: 1; min-width: 0; }
.room-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.room-title {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.room-d-badge {
  padding: 2px 7px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.room-d-badge.urgent {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}
.room-ended-badge {
  padding: 2px 7px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.room-last-msg {
  font-size: 13px;
  color: var(--color-ink-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.room-time { font-size: 11.5px; color: var(--color-ink-muted); }
.unread-badge {
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: var(--color-peach);
  color: white;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}
.read-label { font-size: 11px; color: var(--color-ink-muted); }
</style>
