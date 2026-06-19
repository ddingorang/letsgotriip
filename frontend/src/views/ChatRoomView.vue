# Created: 2026-06-16 14:07:23
<template>
  <div class="page">
    <header class="chat-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="header-center">
        <span class="header-title">{{ room?.title }}</span>
        <span class="header-sub">{{ connectionLabel }}</span>
      </div>
      <button class="more-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="12" cy="5" r="1" fill="currentColor" />
          <circle cx="12" cy="12" r="1" fill="currentColor" />
          <circle cx="12" cy="19" r="1" fill="currentColor" />
        </svg>
      </button>
    </header>

    <div class="msg-scroll" ref="msgScroll">
      <div class="date-separator">
        <span>{{ todayLabel }}</span>
      </div>

      <template v-for="msg in messages" :key="msg.id">
        <!-- Outgoing -->
        <div v-if="isMyMessage(msg)" class="msg-row outgoing">
          <div class="msg-col-out">
            <span class="msg-time">{{ msg.time }}</span>
            <div class="bubble outgoing-bubble">{{ msg.text ?? msg.content }}</div>
          </div>
        </div>

        <!-- Plan card (incoming) -->
        <div v-else-if="msg.type === 'plan'" class="msg-row incoming">
          <div class="msg-avatar" />
          <div class="msg-col">
            <div class="plan-card" @click="$router.push('/plan')">
              <div class="plan-card-tag">공유된 여행 계획</div>
              <div class="plan-card-title">{{ msg.planTitle }}</div>
              <div class="plan-card-meta">{{ msg.dateRange }} · {{ msg.spotCount }}곳</div>
              <div class="plan-card-link">일정 보러가기 →</div>
            </div>
            <span class="msg-time">{{ msg.time }}</span>
          </div>
        </div>

        <!-- Incoming -->
        <div v-else class="msg-row incoming">
          <div class="msg-avatar" />
          <div class="msg-col">
            <span class="msg-sender">{{ msg.sender ?? msg.senderNickname }}</span>
            <div class="bubble incoming-bubble">{{ msg.text ?? msg.content }}</div>
            <span class="msg-time">{{ msg.time }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- Input bar -->
    <div class="input-bar">
      <button class="attach-btn">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
      <input
        v-model="inputText"
        class="msg-input"
        placeholder="메시지 입력"
        @keydown.enter.prevent="(e) => !e.isComposing && sendMessage()"
      />
      <button class="send-btn" :class="{ active: inputText.trim() }" @click="sendMessage">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'
import { useAuthStore } from '@/stores/auth.js'
import { chatApi } from '@/api/index.js'
import { Client } from '@stomp/stompjs'

const route = useRoute()
const companionStore = useCompanionStore()
const authStore = useAuthStore()
const msgScroll = ref(null)
const inputText = ref('')
const connected = ref(false)

const roomId = computed(() => route.params.id)
const room = computed(() => companionStore.myRooms.find((r) => r.id === Number(roomId.value)))
const messages = computed(() => companionStore.messages[roomId.value] ?? [])

const connectionLabel = computed(() => {
  if (connected.value) return room.value?.daysLeft != null ? `여행 D-${room.value.daysLeft}` : '연결됨'
  return '연결 중...'
})

const todayLabel = computed(() => {
  return new Date().toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })
})

function isMyMessage(msg) {
  const myId = authStore.user?.userId
  return myId != null ? Number(msg.senderId) === Number(myId) : false
}

function formatTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (msgScroll.value) msgScroll.value.scrollTop = msgScroll.value.scrollHeight
  })
}

function normalizeMessage(m) {
  return {
    id: m.messageTSID ?? m.id ?? Date.now(),
    senderId: m.senderId,
    senderNickname: m.senderNickname ?? m.sender,
    text: m.content,
    content: m.content,
    time: formatTime(m.timestamp),
  }
}

async function loadHistory() {
  try {
    const { data } = await chatApi.getMessages(roomId.value)
    companionStore.messages[roomId.value] = data.map(normalizeMessage)
  } catch {
    if (!companionStore.messages[roomId.value]) {
      companionStore.messages[roomId.value] = []
    }
  }
}

// ── STOMP ────────────────────────────────────────────────────────────────────

let stompClient = null

function connectStomp() {
  const token = authStore.accessToken
  if (!token) return

  // dev: 직접 BE에 연결 (CORS 허용됨), prod: 같은 origin
  const wsUrl = import.meta.env.DEV
    ? 'ws://localhost:9090/ws'
    : `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`

  stompClient = new Client({
    brokerURL: wsUrl,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    onConnect: () => {
      connected.value = true
      stompClient.subscribe(`/topic/chat.room.${roomId.value}`, (frame) => {
        const msg = JSON.parse(frame.body)
        if (!companionStore.messages[roomId.value]) {
          companionStore.messages[roomId.value] = []
        }
        companionStore.messages[roomId.value].push(normalizeMessage(msg))
        scrollToBottom()
      })
    },
    onDisconnect: () => { connected.value = false },
    onStompError: () => { connected.value = false },
  })

  stompClient.activate()
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return

  if (stompClient?.connected) {
    stompClient.publish({
      destination: `/pub/chat.message.${roomId.value}`,
      body: JSON.stringify({
        chatRoomId: Number(roomId.value),
        correlationId: crypto.randomUUID(),
        messageType: 'TEXT',
        content: text,
      }),
    })
    inputText.value = ''
  }
}

onMounted(async () => {
  await loadHistory()
  scrollToBottom()
  connectStomp()
})

onUnmounted(() => {
  stompClient?.deactivate()
  stompClient = null
  connected.value = false
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface-alt);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.back-btn, .more-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.header-center { display: flex; flex-direction: column; align-items: center; gap: 2px; flex: 1; }
.header-title { font-size: 15px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.header-sub { font-size: 11.5px; color: var(--color-ink-muted); }

.msg-scroll { flex: 1; overflow-y: auto; padding: 16px 16px 8px; display: flex; flex-direction: column; gap: 12px; }

.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 4px 0 8px;
}
.date-separator span {
  font-size: 12px;
  color: var(--color-ink-muted);
  background: rgba(0,0,0,0.06);
  padding: 4px 12px;
  border-radius: var(--radius-full);
}

.msg-row { display: flex; gap: 8px; }
.msg-row.incoming { align-items: flex-start; }
.msg-row.outgoing { justify-content: flex-end; }

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
  margin-top: 18px;
}
.msg-col { display: flex; flex-direction: column; gap: 3px; max-width: 72%; }
.msg-col-out { display: flex; flex-direction: row; align-items: flex-end; gap: 5px; }
.msg-sender { font-size: 12px; font-weight: 600; color: var(--color-ink-secondary); }
.msg-time { font-size: 11px; color: var(--color-ink-muted); flex-shrink: 0; padding-bottom: 2px; }

.bubble {
  padding: 11px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  letter-spacing: -0.2px;
}
.incoming-bubble {
  background: var(--color-white);
  color: var(--color-ink);
  border-radius: 4px 18px 18px 18px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.outgoing-bubble {
  background: var(--color-peach);
  color: white;
  border-radius: 18px 4px 18px 18px;
}

.plan-card {
  background: var(--color-white);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  border: 1.5px solid var(--color-line-light);
  cursor: pointer;
  min-width: 180px;
}
.plan-card-tag {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-peach);
  margin-bottom: 4px;
}
.plan-card-title { font-size: 14px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.plan-card-meta { font-size: 12px; color: var(--color-ink-muted); margin-top: 3px; }
.plan-card-link {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-peach);
  margin-top: 8px;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px calc(10px + var(--safe-bottom));
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.attach-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.msg-input {
  flex: 1;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--color-ink);
}
.msg-input::placeholder { color: var(--color-ink-muted); }
.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s;
}
.send-btn.active { background: var(--color-peach); }
</style>
