// Created: 2026-06-19
//
// AI 어시스턴트(RAG 챗봇) 상태 store.
// - 송신/수신: POST /api/assistant/chat { conversationId, message } → { conversationId, reply }
// - conversationId 는 첫 응답에서 받아 이후 요청에 재사용(대화 맥락 유지).
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assistantApi } from '@/api/index.js'

export const useAssistantStore = defineStore('assistant', () => {
  // { id, role: 'user' | 'assistant', content, time } 형태의 메시지 배열
  const messages = ref([])
  const conversationId = ref(null)
  const loading = ref(false)
  const error = ref(null)

  function nowTime() {
    return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }

  function makeId() {
    return crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  function pushMessage(role, content) {
    messages.value.push({ id: makeId(), role, content, time: nowTime() })
  }

  async function send(message) {
    const text = (message ?? '').trim()
    if (!text || loading.value) return

    // 1) 사용자 메시지를 먼저 화면에 표시
    pushMessage('user', text)

    loading.value = true
    error.value = null
    try {
      const { data } = await assistantApi.chat({
        conversationId: conversationId.value,
        message: text,
      })
      // 서버가 발급/유지하는 conversationId 보존
      if (data?.conversationId) conversationId.value = data.conversationId
      const reply = data?.reply ?? ''
      pushMessage('assistant', reply)
    } catch (e) {
      // 실패를 성공으로 위장하지 않는다: 에러를 노출하고 어시스턴트 버블은 추가하지 않음.
      error.value = e.response?.data?.message ?? e.message ?? '답변을 받지 못했어요.'
    } finally {
      loading.value = false
    }
  }

  function reset() {
    messages.value = []
    conversationId.value = null
    error.value = null
    loading.value = false
  }

  return {
    messages,
    conversationId,
    loading,
    error,
    send,
    reset,
  }
})
