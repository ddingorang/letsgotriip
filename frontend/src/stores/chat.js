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
    // correlationId 또는 messageTSID 중복 방지(에코 수신 대비)
    const dup = list.some(
      (m) =>
        (normalized.correlationId && m.correlationId === normalized.correlationId) ||
        (normalized.id && m.id === normalized.id),
    )
    if (!dup) list.push(normalized)
  }

  // ── 송신 ────────────────────────────────────────────────────────────────────
  function sendMessage(roomId, content) {
    const text = (content ?? '').trim()
    if (!text || !client || !client.connected) return false
    const correlationId = crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`

    client.send(`/pub/chat.message.${roomId}`, {
      chatRoomId: Number(roomId),
      correlationId,
      messageType: 'TEXT',
      content: text,
    })
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
  }
})
