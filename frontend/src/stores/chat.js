// Created: 2026-06-19
//
// 실시간 채팅 상태 + STOMP 연결 관리 store.
// - 히스토리: GET /api/chat/rooms/{id}/messages (REST)
// - 실시간 수신: SUBSCRIBE /topic/chat.room.{id}
// - 송신: SEND /pub/chat.message.{id}
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/api/http.js'
import { createStompClient } from '@/api/stomp.js'
import { useAuthStore } from '@/stores/auth.js'

export const useChatStore = defineStore('chat', () => {
  // roomId(String) → 메시지 배열
  const messages = ref({})
  const connected = ref(false)
  const error = ref(null)

  let client = null
  let currentSub = null
  let currentRoomId = null

  // SYSTEM 이벤트(ROOM_UPDATED/PARTICIPANT_KICKED/HOST_CHANGED/PARTICIPANT_JOINED) 구독자.
  // 채팅 버블이 아니라 방 상태/참여자 목록 갱신용이므로 뷰가 핸들러를 등록해 받아간다.
  const systemHandlers = new Set()
  function onSystemEvent(handler) {
    systemHandlers.add(handler)
    return () => systemHandlers.delete(handler)
  }
  function emitSystemEvent(roomId, frame) {
    systemHandlers.forEach((h) => {
      try { h(String(roomId), frame) } catch { /* 핸들러 오류 격리 */ }
    })
  }

  function ensureRoom(roomId) {
    const key = String(roomId)
    if (!messages.value[key]) messages.value[key] = []
    return key
  }

  // BE MessageResponseDto → 화면 표시용 메시지 형태로 정규화
  function normalizeMessage(dto) {
    return {
      id: dto.messageTSID ?? dto.id ?? `${Date.now()}-${Math.random()}`,
      correlationId: dto.correlationId ?? null,
      senderId: dto.senderId ?? null,
      senderNickname: dto.senderNickname ?? null,
      content: dto.content ?? dto.text ?? '',
      type: dto.messageType ?? 'TEXT',
      timestamp: dto.timestamp ?? null,
      time: formatTime(dto.timestamp),
    }
  }

  function formatTime(ts) {
    if (!ts) {
      return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
    }
    const d = new Date(ts)
    if (isNaN(d.getTime())) return ''
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }

  // ── 히스토리 ──────────────────────────────────────────────────────────────────
  async function loadHistory(roomId) {
    const key = ensureRoom(roomId)
    error.value = null
    try {
      const { data } = await http.get(`/api/chat/rooms/${roomId}/messages`)
      const list = Array.isArray(data) ? data : (data?.content ?? [])
      messages.value[key] = list.map(normalizeMessage)
    } catch (e) {
      // 히스토리 없음/권한 등은 조용히 무시 (실시간만으로도 동작)
      error.value = e.response?.data?.message ?? null
    }
  }

  // ── 실시간 연결 ────────────────────────────────────────────────────────────────
  async function connect(roomId) {
    const key = ensureRoom(roomId)
    const authStore = useAuthStore()

    // 동일 방에 이미 연결돼 있으면 재사용
    if (client && client.connected && currentRoomId === key) return

    // 다른 방이거나 끊긴 상태면 정리 후 재연결
    disconnect()

    currentRoomId = key
    client = createStompClient({
      token: authStore.accessToken,
      baseUrl: import.meta.env.VITE_API_BASE_URL || '',
      onClose: () => { connected.value = false },
      onError: () => { error.value = 'WebSocket 오류' },
    })

    try {
      await client.connect()
      connected.value = true
      currentSub = client.subscribe(`/topic/chat.room.${roomId}`, (frame) => {
        try {
          const dto = JSON.parse(frame.body)
          // SYSTEM 이벤트는 채팅 버블로 렌더하지 않고 뷰 상태 갱신용으로만 전달한다.
          if (dto?.type === 'SYSTEM') {
            emitSystemEvent(roomId, dto)
            return
          }
          appendMessage(roomId, dto)
        } catch {
          // 비 JSON 프레임 무시
        }
      })
    } catch (e) {
      connected.value = false
      error.value = '채팅 서버에 연결하지 못했어요.'
    }
  }

  function appendMessage(roomId, dto) {
    const key = ensureRoom(roomId)
    const normalized = normalizeMessage(dto)
    const list = messages.value[key]
    // 낙관적 버블 병합: 동일 correlationId의 pending 버블이 있으면 실제 메시지로 교체
    // (sendMessage가 먼저 push한 임시 버블을 에코 수신 시 확정한다)
    if (normalized.correlationId) {
      const pendingIdx = list.findIndex(
        (m) => m.correlationId === normalized.correlationId && m.pending,
      )
      if (pendingIdx !== -1) {
        const pending = list[pendingIdx]
        if (pending.failTimer) clearTimeout(pending.failTimer)
        // 실제 서버 데이터로 교체하되 pending/failed 플래그는 제거
        list[pendingIdx] = normalized
        return
      }
    }
    // correlationId 또는 messageTSID 중복 방지(에코 수신 대비)
    const dup = list.some(
      (m) =>
        (normalized.correlationId && m.correlationId === normalized.correlationId) ||
        (normalized.id && m.id === normalized.id),
    )
    if (!dup) list.push(normalized)
  }

  // ── 송신 ────────────────────────────────────────────────────────────────────
  // 낙관적 버블이 일정 시간 내 에코로 확정되지 않으면 '전송 실패'로 표시할 대기시간(ms).
  const ECHO_TIMEOUT_MS = 8000

  // messageType: 'TEXT'(기본) | 'IMAGE' 등. IMAGE면 content는 업로드된 이미지 URL.
  function sendMessage(roomId, content, messageType = 'TEXT') {
    // TEXT는 공백 트림 후 빈 값 차단, 그 외(IMAGE 등)는 URL 보존을 위해 원본 사용
    const body = messageType === 'TEXT' ? (content ?? '').trim() : (content ?? '')
    if (!body || !client || !client.connected) return false
    const correlationId = crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`

    client.send(`/pub/chat.message.${roomId}`, {
      chatRoomId: Number(roomId),
      correlationId,
      messageType,
      content: body,
    })

    // 낙관적 표시: 전송 직후 내 메시지 버블을 즉시 목록에 추가한다.
    // 동일 correlationId로 에코가 도착하면 appendMessage가 실제 메시지로 교체한다.
    const key = ensureRoom(roomId)
    const authStore = useAuthStore()
    const optimistic = {
      id: `pending-${correlationId}`,
      correlationId,
      senderId: authStore.user?.userId ?? null,
      senderNickname: authStore.user?.nickname ?? null,
      content: body,
      type: messageType,
      timestamp: null,
      time: formatTime(null),
      pending: true,  // 전송 중(에코 대기) 표시
      failed: false,  // 에코 타임아웃 시 true
      failTimer: null,
    }
    // 8초 내 에코 미수신 시 '전송 실패' 상태로 전환
    optimistic.failTimer = setTimeout(() => {
      const list = messages.value[key]
      const target = list.find((m) => m.correlationId === correlationId && m.pending)
      if (target) {
        target.pending = false
        target.failed = true
      }
    }, ECHO_TIMEOUT_MS)
    messages.value[key].push(optimistic)

    return true
  }

  function disconnect() {
    if (currentSub) {
      try { currentSub.unsubscribe() } catch { /* ignore */ }
      currentSub = null
    }
    if (client) {
      try { client.disconnect() } catch { /* ignore */ }
      client = null
    }
    // 대기 중인 낙관적 버블의 실패 타이머 정리(메모리/오발화 방지)
    if (currentRoomId && messages.value[currentRoomId]) {
      messages.value[currentRoomId].forEach((m) => {
        if (m.failTimer) { clearTimeout(m.failTimer); m.failTimer = null }
      })
    }
    connected.value = false
    currentRoomId = null
  }

  return {
    messages,
    connected,
    error,
    loadHistory,
    connect,
    sendMessage,
    disconnect,
    onSystemEvent,
  }
})
