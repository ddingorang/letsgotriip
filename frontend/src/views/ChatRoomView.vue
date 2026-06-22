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
          <button v-if="viewerIsHost" class="dropdown-item" @click="openSettings">
            방 설정
          </button>
          <button class="dropdown-item" @click="toggleMute" :disabled="muteLoading">
            <span>알림 {{ muted ? '켜기' : '음소거' }}</span>
            <span class="dropdown-badge">{{ muted ? '꺼짐' : '켜짐' }}</span>
          </button>
          <button
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
        <span>{{ todayLabel }}</span>
      </div>

      <!-- 빈 대화: 메시지가 하나도 없을 때 -->
      <div v-if="messages.length === 0" class="empty-thread">
        <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
        </svg>
        <span class="empty-thread-text">첫 메시지를 보내 대화를 시작하세요</span>
      </div>

      <template v-for="msg in messages" :key="msg.id">
        <!-- Outgoing -->
        <div v-if="isMyMessage(msg)" class="msg-row outgoing">
          <div class="msg-col-out">
            <span class="msg-status">
              <span v-if="msg.failed" class="msg-failed">전송 실패</span>
              <span v-else-if="msg.pending" class="msg-pending">전송 중…</span>
              <template v-else>{{ msg.time }}</template>
            </span>
            <img
              v-if="msg.type === 'IMAGE'"
              :src="msg.content"
              class="msg-image"
              :class="{ 'msg-sending': msg.pending, 'msg-send-failed': msg.failed }"
              alt="이미지"
              @click="openImage(msg.content)"
              @error="onImageError"
            />
            <div
              v-else
              class="bubble outgoing-bubble"
              :class="{ 'msg-sending': msg.pending, 'msg-send-failed': msg.failed }"
            >{{ msg.text ?? msg.content }}</div>
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

        <!-- 초대(방장 전용): 닉네임 또는 이메일 -->
        <div v-if="viewerIsHost" class="invite-box">
          <input
            v-model="inviteValue"
            class="invite-input"
            placeholder="닉네임 또는 이메일로 초대"
            @keydown.enter.prevent="(e) => !e.isComposing && submitInvite()"
          />
          <button class="invite-btn" :disabled="inviting || !inviteValue.trim()" @click="submitInvite">
            {{ inviting ? '초대 중…' : '초대' }}
          </button>
        </div>

        <ul class="participant-list">
          <li v-for="p in participants" :key="p.userId" class="participant-row">
            <span class="participant-avatar" />
            <span class="participant-name">{{ p.nickname }}</span>
            <span v-if="p.isHost" class="participant-host">방장</span>
            <template v-if="viewerIsHost && !p.isHost">
              <button
                class="participant-action transfer"
                :disabled="actionBusy"
                @click="transferHost(p)"
              >위임</button>
              <button
                class="participant-action kick"
                :disabled="actionBusy"
                @click="kickParticipant(p)"
              >강퇴</button>
            </template>
          </li>
        </ul>
      </div>
    </div>

    <!-- 방 설정 시트(방장 전용) -->
    <div v-if="settingsOpen" class="overlay" @click.self="settingsOpen = false">
      <div class="sheet">
        <div class="sheet-header">
          <span>방 설정</span>
          <button class="sheet-close" @click="settingsOpen = false">닫기</button>
        </div>
        <!-- 방 이미지 변경(방장 전용) -->
        <div class="settings-field">
          <label class="settings-label">방 이미지</label>
          <button
            type="button"
            class="room-image-picker"
            :disabled="roomImageUploading"
            @click="triggerRoomImagePicker"
          >
            <img
              v-if="roomImageUrl"
              :src="roomImageUrl"
              class="room-image-preview"
              alt="방 이미지"
              @error="onRoomImageError"
            />
            <div v-else class="room-image-placeholder">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
              </svg>
            </div>
            <span class="room-image-overlay">
              {{ roomImageUploading ? '업로드 중…' : '이미지 변경' }}
            </span>
          </button>
          <input
            ref="roomImageInput"
            type="file"
            accept="image/*"
            class="file-input-hidden"
            @change="onRoomImageSelected"
          />
        </div>
        <div class="settings-field">
          <label class="settings-label">방 이름</label>
          <input
            v-model="settingsTitle"
            class="settings-input"
            maxlength="18"
            placeholder="방 이름 (최대 18자)"
          />
        </div>
        <div class="settings-field">
          <label class="settings-label">소개</label>
          <textarea
            v-model="settingsDescription"
            class="settings-textarea"
            maxlength="200"
            rows="3"
            placeholder="방 소개 (선택, 최대 200자)"
          />
        </div>
        <button
          class="settings-save"
          :disabled="savingSettings || !settingsTitle.trim()"
          @click="saveSettings"
        >
          {{ savingSettings ? '저장 중…' : '저장' }}
        </button>
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
import { useConfirm } from '@/composables/useConfirm.js'

const $confirm = useConfirm().confirm

const route = useRoute()
const router = useRouter()
const companionStore = useCompanionStore()
const chatStore = useChatStore()
const authStore = useAuthStore()
const msgScroll = ref(null)
const inputText = ref('')

const roomId = computed(() => route.params.id)
const room = computed(() => companionStore.myRooms.find((r) => r.id === Number(route.params.id)))
// 실시간 메시지는 chatStore(STOMP)가 단일 소스로 관리한다.
const messages = computed(() => chatStore.messages[String(route.params.id)] ?? [])

// 헤더 연결 상태 라벨 — STOMP 연결 여부(chatStore.connected)를 기준으로 표시.
const connectionLabel = computed(() => {
  if (chatStore.connected) {
    return room.value?.daysLeft != null ? `여행 D-${room.value.daysLeft}` : '연결됨'
  }
  return '연결 중...'
})

const todayLabel = computed(() => {
  return new Date().toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })
})

function isMyMessage(msg) {
  const myId = authStore.user?.userId
  return myId != null ? Number(msg.senderId) === Number(myId) : false
}

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
  // STOMP 송신은 chatStore.sendMessage가 수행한다.
  // 내부에서 낙관적 버블을 1개만 push하고, 동일 correlationId 에코 수신 시
  // 실제 메시지로 교체(중복 방지)하므로 메시지는 한 번만 전송·표시된다.
  const sent = chatStore.sendMessage(roomId.value, text)
  if (sent) {
    inputText.value = ''
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
// 요청자가 방장인지 — participants 응답의 viewerIsHost 를 신뢰(myRooms.isHost 보다 최신).
// 참여자 조회 전에는 myRooms.isHost 로 선표시해 메뉴 깜빡임을 줄인다.
const viewerIsHost = ref(isHost.value)
async function fetchParticipants() {
  try {
    const { data } = await chatApi.getParticipants(roomId.value)
    participants.value = Array.isArray(data?.participants) ? data.participants : []
    participantCount.value = data?.count ?? participants.value.length
    viewerIsHost.value = data?.viewerIsHost === true
    // 서버가 내려준 음소거 상태로 초기화/동기화(토글 직후 서버값과 일치 유지)
    if (typeof data?.viewerMuted === 'boolean') muted.value = data.viewerMuted === true
  } catch (e) {
    // 권한/네트워크 실패는 조용히 무시 (배지 미표시)
  }
}
async function openParticipants() {
  menuOpen.value = false
  await fetchParticipants()
  participantsOpen.value = true
}

// ── 알림 음소거 ──────────────────────────────────────────────────────────────
const muted = ref(false)
const muteLoading = ref(false)
async function toggleMute() {
  if (muteLoading.value) return
  muteLoading.value = true
  const next = !muted.value
  try {
    await chatApi.muteMembership(roomId.value, { muted: next })
    muted.value = next
    menuOpen.value = false
    showToast(next ? '알림을 음소거했어요.' : '알림을 켰어요.')
  } catch (e) {
    showToast(e.response?.data?.message ?? '설정을 변경하지 못했어요.')
  } finally {
    muteLoading.value = false
  }
}

// ── 방 설정(방장) ────────────────────────────────────────────────────────────
const settingsOpen = ref(false)
const settingsTitle = ref('')
const settingsDescription = ref('')
const savingSettings = ref(false)
// 방 이미지 변경(방장 전용) — 현재 이미지 미리보기 + 업로드 상태
const roomImageInput = ref(null)
const roomImageUrl = ref(null)
const roomImageUploading = ref(false)
function openSettings() {
  menuOpen.value = false
  settingsTitle.value = room.value?.title ?? ''
  settingsDescription.value = room.value?.description ?? ''
  roomImageUrl.value = room.value?.imageUrl ?? null
  settingsOpen.value = true
}

// 숨김 file input 트리거(업로드 중이면 무시)
function triggerRoomImagePicker() {
  if (roomImageUploading.value) return
  roomImageInput.value?.click()
}

async function onRoomImageSelected(e) {
  const file = e.target.files?.[0]
  // 동일 파일 재선택 시에도 change 가 발생하도록 입력값 초기화
  e.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showToast('이미지 파일만 업로드할 수 있어요.')
    return
  }
  roomImageUploading.value = true
  try {
    // chatApi.uploadRoomImage 는 File 을 받아 내부에서 FormData(field 'file')로 감싸 전송한다.
    const { data } = await chatApi.uploadRoomImage(roomId.value, file)
    const imageUrl = data?.imageUrl
    if (!imageUrl) {
      showToast('업로드 응답이 올바르지 않아요.')
      return
    }
    // 미리보기 + 방 객체 갱신(헤더/목록에서도 즉시 반영)
    roomImageUrl.value = imageUrl
    if (room.value) room.value.imageUrl = imageUrl
    showToast('방 이미지를 변경했어요.')
  } catch (err) {
    showToast(err.response?.data?.message ?? '이미지 업로드에 실패했어요.')
  } finally {
    roomImageUploading.value = false
  }
}

function onRoomImageError(e) {
  // 깨진 이미지면 플레이스홀더로 되돌아가도록 미리보기 해제
  e.target.classList.add('img-broken')
}
async function saveSettings() {
  const title = settingsTitle.value.trim()
  if (!title || savingSettings.value) return
  savingSettings.value = true
  try {
    await chatApi.updateRoom(roomId.value, {
      title,
      description: settingsDescription.value.trim() || null,
    })
    // 낙관적 갱신: 헤더 제목 즉시 반영(브로드캐스트로도 다시 갱신됨)
    if (room.value) room.value.title = title
    settingsOpen.value = false
    showToast('방 설정을 저장했어요.')
  } catch (e) {
    showToast(e.response?.data?.message ?? '저장에 실패했어요.')
  } finally {
    savingSettings.value = false
  }
}

// ── 강퇴 / 초대 / 방장 위임 ──────────────────────────────────────────────────
const actionBusy = ref(false)
async function kickParticipant(p) {
  if (actionBusy.value) return
  if (!await $confirm(`${p.nickname} 님을 내보낼까요?`)) return
  actionBusy.value = true
  try {
    await chatApi.kickParticipant(roomId.value, p.userId)
    await fetchParticipants()
    showToast(`${p.nickname} 님을 내보냈어요.`)
  } catch (e) {
    showToast(e.response?.data?.message ?? '내보내지 못했어요.')
  } finally {
    actionBusy.value = false
  }
}
async function transferHost(p) {
  if (actionBusy.value) return
  if (!await $confirm(`${p.nickname} 님에게 방장을 위임할까요?`)) return
  actionBusy.value = true
  try {
    await chatApi.transferHost(roomId.value, { newHostUserId: p.userId })
    await fetchParticipants()
    showToast(`${p.nickname} 님이 방장이 되었어요.`)
  } catch (e) {
    showToast(e.response?.data?.message ?? '위임하지 못했어요.')
  } finally {
    actionBusy.value = false
  }
}

const inviteValue = ref('')
const inviting = ref(false)
async function submitInvite() {
  const raw = inviteValue.value.trim()
  if (!raw || inviting.value) return
  // '@' 포함이면 이메일, 아니면 닉네임으로 식별
  const body = raw.includes('@') ? { email: raw } : { nickname: raw }
  inviting.value = true
  try {
    await chatApi.inviteParticipant(roomId.value, body)
    inviteValue.value = ''
    await fetchParticipants()
    showToast('참여자를 초대했어요.')
  } catch (e) {
    showToast(e.response?.data?.message ?? '초대하지 못했어요.')
  } finally {
    inviting.value = false
  }
}

// ── 채팅방 나가기 ────────────────────────────────────────────────────────────
const leaving = ref(false)
async function leaveRoom() {
  if (leaving.value) return
  if (!await $confirm('채팅방에서 나가시겠어요? 대화 내용을 더 이상 볼 수 없어요.')) return
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

// ── 실시간 SYSTEM 이벤트 처리 ───────────────────────────────────────────────
// 채팅 버블이 아니라 방 상태/참여자 목록 갱신 또는 강퇴 시 방 이탈에 사용.
let unsubscribeSystem = null
function handleSystemEvent(eventRoomId, frame) {
  // 다른 방의 프레임은 무시(스토어가 한 방만 구독하지만 방어적으로 확인)
  if (String(eventRoomId) !== String(roomId.value)) return
  const myId = authStore.user?.userId
  switch (frame.event) {
    case 'ROOM_UPDATED':
      if (room.value && typeof frame.title === 'string') room.value.title = frame.title
      if (room.value && 'description' in frame) room.value.description = frame.description
      break
    case 'PARTICIPANT_JOINED':
      // 참여자 시트가 열려 있으면 목록 갱신, 닫혀 있어도 배지 카운트 갱신
      fetchParticipants()
      break
    case 'HOST_CHANGED':
      // 방장 변경 — 내가 새 방장/전 방장이면 권한 표시가 바뀌므로 재조회
      fetchParticipants()
      if (room.value && myId != null) {
        room.value.isHost = Number(frame.newHostUserId) === Number(myId)
      }
      break
    case 'PARTICIPANT_KICKED':
      if (myId != null && Number(frame.userId) === Number(myId)) {
        // 내가 강퇴됨 — 연결 해제 후 목록으로 이동
        showToast('채팅방에서 내보내졌어요.')
        chatStore.disconnect()
        companionStore.fetchMyRooms()
        router.back()
      } else {
        fetchParticipants()
      }
      break
    default:
      // 알 수 없는 SYSTEM 이벤트는 조용히 무시
      break
  }
}

onMounted(async () => {
  // 참여 중인 방 목록이 비어 있으면 헤더 정보를 위해 로드
  if (companionStore.myRooms.length === 0) {
    await companionStore.fetchMyRooms()
  }
  unsubscribeSystem = chatStore.onSystemEvent(handleSystemEvent)
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
  if (unsubscribeSystem) unsubscribeSystem()
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

/* ── 빈 대화 힌트 ─────────────────────────────────────────────── */
.empty-thread {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin: auto 0;
  padding: 40px 24px;
  text-align: center;
}
.empty-thread-text { font-size: 13px; color: var(--color-ink-muted); letter-spacing: -0.2px; }

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

/* ── 전송 상태(낙관적 전송) ─────────────────────────────────────── */
.msg-status { font-size: 11px; color: var(--color-ink-muted); flex-shrink: 0; padding-bottom: 2px; }
.msg-pending { color: var(--color-ink-muted); }
.msg-failed { color: #e2483d; font-weight: 600; }
/* 에코 대기 중인 버블은 살짝 흐리게 */
.msg-sending { opacity: 0.6; }
/* 전송 실패 버블은 붉은 테두리로 강조 */
.msg-send-failed { outline: 1px solid #e2483d; outline-offset: 1px; }

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
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}
.send-btn:active { transform: scale(0.95); }
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
.participant-action {
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--radius-full);
  padding: 4px 10px;
  flex-shrink: 0;
}
.participant-action:disabled { opacity: 0.5; }
.participant-action.transfer {
  color: var(--color-ink-secondary);
  background: var(--color-surface);
}
.participant-action.kick {
  color: #e2483d;
  background: rgba(226,72,61,0.08);
}

/* ── 초대 입력 ────────────────────────────────────────────────── */
.invite-box {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.invite-input {
  flex: 1;
  padding: 9px 12px;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-ink);
}
.invite-input::placeholder { color: var(--color-ink-muted); }
.invite-btn {
  flex-shrink: 0;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 600;
  color: white;
  background: var(--color-peach);
  border-radius: var(--radius-full);
}
.invite-btn:disabled { opacity: 0.5; }

/* ── 방 설정 시트 ─────────────────────────────────────────────── */
.settings-field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }

/* 방 이미지 변경 — 미리보기 + 오버레이 라벨 */
.room-image-picker {
  position: relative;
  width: 100%;
  height: 120px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.room-image-picker:disabled { opacity: 0.6; }
.room-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.room-image-preview.img-broken {
  width: auto;
  height: auto;
  object-fit: contain;
}
.room-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}
.room-image-overlay {
  position: absolute;
  bottom: 8px;
  right: 8px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  background: rgba(0,0,0,0.55);
  border-radius: var(--radius-full);
  padding: 4px 12px;
}
.settings-label { font-size: 12px; font-weight: 600; color: var(--color-ink-secondary); }
.settings-input,
.settings-textarea {
  padding: 10px 12px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  width: 100%;
}
.settings-textarea { resize: none; line-height: 1.5; }
.settings-input::placeholder,
.settings-textarea::placeholder { color: var(--color-ink-muted); }
.settings-save {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  font-weight: 700;
  color: white;
  background: var(--color-peach);
  border-radius: var(--radius-md);
}
.settings-save:disabled { opacity: 0.5; }

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
