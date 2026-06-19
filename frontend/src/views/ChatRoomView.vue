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
        <span class="header-sub">여행 D-{{ room?.daysLeft }}</span>
      </div>
      <div class="more-wrap" ref="moreWrap">
        <button class="more-btn" @click="toggleMenu">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="5" r="1" fill="currentColor" />
            <circle cx="12" cy="12" r="1" fill="currentColor" />
            <circle cx="12" cy="19" r="1" fill="currentColor" />
          </svg>
        </button>
        <div v-if="menuOpen" class="dropdown">
          <button class="dropdown-item" @click="openParticipants">
            참여자
            <span v-if="participantCount != null" class="dropdown-badge">{{ participantCount }}</span>
          </button>
          <button
            v-if="!isHost"
            class="dropdown-item danger"
            :disabled="leaving"
            @click="leaveRoom"
          >
            {{ leaving ? '나가는 중…' : '채팅방 나가기' }}
          </button>
        </div>
      </div>
    </header>

    <div class="msg-scroll" ref="msgScroll">
      <div class="date-separator">
        <span>6월 10일 화요일</span>
      </div>

      <template v-for="msg in messages" :key="msg.id">
        <!-- Outgoing (내 메시지) -->
        <div v-if="isMyMessage(msg)" class="msg-row outgoing">
          <div class="msg-col-out">
            <span class="msg-time">{{ msg.time }}</span>
            <img
              v-if="msg.type === 'IMAGE'"
              :src="msg.content"
              class="msg-image"
              alt="이미지"
              @click="openImage(msg.content)"
              @error="onImageError"
            />
            <div v-else class="bubble outgoing-bubble">{{ msg.text ?? msg.content }}</div>
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

        <!-- Incoming (상대방 메시지) -->
        <div v-else class="msg-row incoming">
          <div class="msg-avatar" />
          <div class="msg-col">
            <span class="msg-sender">{{ msg.sender ?? msg.senderNickname }}</span>
            <img
              v-if="msg.type === 'IMAGE'"
              :src="msg.content"
              class="msg-image"
              alt="이미지"
              @click="openImage(msg.content)"
              @error="onImageError"
            />
            <div v-else class="bubble incoming-bubble">{{ msg.text ?? msg.content }}</div>
            <span class="msg-time">{{ msg.time }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- Input bar -->
    <div class="input-bar">
      <button class="attach-btn" :disabled="uploading" @click="triggerFilePicker">
        <svg v-if="!uploading" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        <span v-else class="spinner" />
      </button>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        class="file-input-hidden"
        @change="onFileSelected"
      />
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

    <!-- 참여자 시트 -->
    <div v-if="participantsOpen" class="overlay" @click.self="participantsOpen = false">
      <div class="sheet">
        <div class="sheet-header">
          <span>참여자 {{ participants.length }}명</span>
          <button class="sheet-close" @click="participantsOpen = false">닫기</button>
        </div>
        <ul class="participant-list">
          <li v-for="p in participants" :key="p.userId" class="participant-row">
            <span class="participant-avatar" />
            <span class="participant-name">{{ p.nickname }}</span>
            <span v-if="p.isHost" class="participant-host">방장</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 이미지 확대 -->
    <div v-if="previewImage" class="image-lightbox" @click="previewImage = null">
      <img :src="previewImage" alt="이미지 미리보기" />
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'
import { useChatStore } from '@/stores/chat.js'
import { useAuthStore } from '@/stores/auth.js'
import { chatApi, communityApi } from '@/api/index.js'

const route = useRoute()
const router = useRouter()
const companionStore = useCompanionStore()
const chatStore = useChatStore()
const authStore = useAuthStore()
const msgScroll = ref(null)
const inputText = ref('')

const roomId = computed(() => route.params.id)

function isMyMessage(msg) {
  const myId = authStore.user?.userId
  return myId != null ? Number(msg.senderId) === Number(myId) : msg.senderId === 'me'
}

const room = computed(() => companionStore.myRooms.find((r) => r.id === Number(route.params.id)))
const messages = computed(() => chatStore.messages[String(route.params.id)] ?? [])

// 방장 여부: myRooms 의 isHost(있으면) 우선, 없으면 false
const isHost = computed(() => room.value?.isHost === true)

function scrollToBottom() {
  nextTick(() => {
    if (msgScroll.value) msgScroll.value.scrollTop = msgScroll.value.scrollHeight
  })
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  const sent = chatStore.sendMessage(roomId.value, text)
  if (sent) {
    inputText.value = ''
    // 실제 메시지는 STOMP 브로드캐스트(에코)로 수신되어 목록에 추가된다.
  }
}

// ── 토스트 ────────────────────────────────────────────────────────────────────
const toast = ref('')
let toastTimer = null
function showToast(message) {
  toast.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2200)
}

// ── ⋮ 드롭다운 메뉴 ──────────────────────────────────────────────────────────
const menuOpen = ref(false)
const moreWrap = ref(null)
function toggleMenu() {
  menuOpen.value = !menuOpen.value
}
function closeMenuOnOutside(e) {
  if (menuOpen.value && moreWrap.value && !moreWrap.value.contains(e.target)) {
    menuOpen.value = false
  }
}

// ── 참여자 ────────────────────────────────────────────────────────────────────
const participants = ref([])
const participantCount = ref(null)
const participantsOpen = ref(false)
async function fetchParticipants() {
  try {
    const { data } = await chatApi.getParticipants(roomId.value)
    participants.value = Array.isArray(data?.participants) ? data.participants : []
    participantCount.value = data?.count ?? participants.value.length
  } catch (e) {
    // 권한/네트워크 실패는 조용히 무시 (배지 미표시)
  }
}
async function openParticipants() {
  menuOpen.value = false
  await fetchParticipants()
  participantsOpen.value = true
}

// ── 채팅방 나가기 ────────────────────────────────────────────────────────────
const leaving = ref(false)
async function leaveRoom() {
  if (leaving.value) return
  if (!window.confirm('채팅방에서 나가시겠어요? 대화 내용을 더 이상 볼 수 없어요.')) return
  leaving.value = true
  try {
    await chatApi.leaveRoom(roomId.value)
    menuOpen.value = false
    chatStore.disconnect()
    await companionStore.fetchMyRooms()
    router.back()
  } catch (e) {
    showToast(e.response?.data?.message ?? '채팅방을 나가지 못했어요.')
  } finally {
    leaving.value = false
  }
}

// ── 이미지 첨부 ──────────────────────────────────────────────────────────────
const fileInput = ref(null)
const uploading = ref(false)
const previewImage = ref(null)

function triggerFilePicker() {
  if (uploading.value) return
  fileInput.value?.click()
}

async function onFileSelected(e) {
  const file = e.target.files?.[0]
  // 동일 파일 재선택 시에도 change 가 발생하도록 입력값 초기화
  e.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showToast('이미지 파일만 첨부할 수 있어요.')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await communityApi.uploadImage(formData)
    const imageUrl = data?.imageUrl
    if (!imageUrl) {
      showToast('업로드 응답이 올바르지 않아요.')
      return
    }
    const sent = chatStore.sendMessage(roomId.value, imageUrl, 'IMAGE')
    if (!sent) showToast('연결이 끊겨 전송하지 못했어요.')
  } catch (err) {
    showToast(err.response?.data?.message ?? '이미지 업로드에 실패했어요.')
  } finally {
    uploading.value = false
  }
}

function openImage(url) {
  if (url) previewImage.value = url
}
function onImageError(e) {
  // 깨진 이미지 표시 대체
  e.target.classList.add('img-broken')
  e.target.alt = '이미지를 불러올 수 없어요'
}

// 새 메시지가 들어오면 자동 스크롤
watch(() => messages.value.length, () => scrollToBottom())

onMounted(async () => {
  // 참여 중인 방 목록이 비어 있으면 헤더 정보를 위해 로드
  if (companionStore.myRooms.length === 0) {
    await companionStore.fetchMyRooms()
  }
  await chatStore.loadHistory(roomId.value)
  await chatStore.connect(roomId.value)
  // 헤더 배지용 인원수 미리 조회(실패해도 무시)
  fetchParticipants()
  scrollToBottom()
  document.addEventListener('click', closeMenuOnOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenuOnOutside)
  if (toastTimer) clearTimeout(toastTimer)
  chatStore.disconnect()
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

/* ── ⋮ 드롭다운 메뉴 ───────────────────────────────────────────── */
.more-wrap { position: relative; }
.dropdown {
  position: absolute;
  top: 44px;
  right: 0;
  min-width: 160px;
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-md);
  box-shadow: 0 6px 20px rgba(0,0,0,0.12);
  padding: 6px;
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  font-size: 14px;
  color: var(--color-ink);
  border-radius: var(--radius-sm);
  text-align: left;
  background: transparent;
}
.dropdown-item:hover { background: var(--color-surface); }
.dropdown-item:disabled { opacity: 0.5; }
.dropdown-item.danger { color: #e2483d; }
.dropdown-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 1px 8px;
}

/* ── 첨부/이미지 메시지 ───────────────────────────────────────── */
.file-input-hidden { display: none; }
.attach-btn:disabled { opacity: 0.5; }
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-line);
  border-top-color: var(--color-peach);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.msg-image {
  max-width: 220px;
  max-height: 280px;
  border-radius: 14px;
  object-fit: cover;
  cursor: pointer;
  background: var(--color-surface);
}
.msg-image.img-broken {
  width: 160px;
  height: 90px;
  object-fit: contain;
  border: 1px dashed var(--color-line);
}

/* ── 참여자 시트 ──────────────────────────────────────────────── */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 50;
}
.sheet {
  width: 100%;
  max-width: 480px;
  background: var(--color-white);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  padding: 16px 16px calc(16px + var(--safe-bottom));
  max-height: 60%;
  overflow-y: auto;
}
.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  margin-bottom: 12px;
}
.sheet-close { font-size: 13px; color: var(--color-ink-muted); }
.participant-list { display: flex; flex-direction: column; gap: 4px; }
.participant-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
}
.participant-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-surface);
  flex-shrink: 0;
}
.participant-name { font-size: 14px; color: var(--color-ink); flex: 1; }
.participant-host {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-peach);
  background: rgba(0,0,0,0.04);
  border-radius: var(--radius-full);
  padding: 2px 8px;
}

/* ── 이미지 확대 ──────────────────────────────────────────────── */
.image-lightbox {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 60;
  padding: 24px;
}
.image-lightbox img {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
}

/* ── 토스트 ───────────────────────────────────────────────────── */
.toast {
  position: fixed;
  bottom: calc(90px + var(--safe-bottom));
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.82);
  color: #fff;
  font-size: 13px;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  z-index: 70;
  max-width: 80%;
  text-align: center;
}
</style>
