// Created: 2026-06-19
//
// AI 어시스턴트(RAG 챗봇) 상태 store.
// - 기본 송신: POST /api/assistant/chat/stream (SSE) 로 토큰을 점진 수신해 타이핑 효과로 렌더.
//   · 첫 event:conversationId 로 대화ID 보존(이후 요청 재사용 → 대화 맥락 유지).
//   · event:token 마다 마지막 어시스턴트 버블에 누적.
// - 스트리밍이 실패하면(연결/HTTP 오류 등) 비스트리밍 POST /api/assistant/chat 로 1회 폴백.
// - conversationId 는 첫 응답에서 받아 이후 요청에 재사용.
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assistantApi } from '@/api/index.js'

export const useAssistantStore = defineStore('assistant', () => {
  // { id, role: 'user' | 'assistant', content, time } 형태의 메시지 배열
  const messages = ref([])
  const conversationId = ref(null)
  const loading = ref(false) // 요청 시작 ~ 첫 토큰 도착 전(typing 인디케이터)
  const streaming = ref(false) // 첫 토큰 ~ 응답 종료(취소 가능 구간)
  const error = ref(null)

  // 진행 중인 스트리밍 취소용 컨트롤러
  let abortController = null

  function nowTime() {
    return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  }

  function makeId() {
    return crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  function pushMessage(role, content) {
    const msg = { id: makeId(), role, content, time: nowTime() }
    messages.value.push(msg)
    return msg
  }

  async function send(message) {
    const text = (message ?? '').trim()
    if (!text || loading.value) return

    // 1) 사용자 메시지를 먼저 화면에 표시
    pushMessage('user', text)

    loading.value = true
    streaming.value = true
    error.value = null
    abortController = new AbortController()

    // 스트리밍으로 받은 토큰을 누적할 어시스턴트 버블. 첫 토큰 도착 시 생성한다
    // (도착 전에는 기존 typing 인디케이터를 노출하기 위해 미리 만들지 않음).
    let assistantMsg = null
    const ensureAssistant = () => {
      if (!assistantMsg) {
        assistantMsg = pushMessage('assistant', '')
        loading.value = false // 첫 토큰부터는 typing 인디케이터 대신 버블 표시
      }
      return assistantMsg
    }

    try {
      const result = await assistantApi.chatStream(
        { conversationId: conversationId.value, message: text },
        {
          onConversationId: (id) => {
            if (id) conversationId.value = id
          },
          onToken: (token) => {
            ensureAssistant().content += token
          },
          signal: abortController.signal,
        },
      )
      // 서버가 발급/유지하는 conversationId 보존(이벤트를 놓친 경우 대비)
      if (result?.conversationId) conversationId.value = result.conversationId

      // 토큰이 하나도 안 온 경우(빈 응답) — 버블이 없으면 누적 결과로 채운다.
      if (!assistantMsg) {
        const reply = result?.reply ?? ''
        if (reply) pushMessage('assistant', reply)
      } else if (!assistantMsg.content && result?.reply) {
        assistantMsg.content = result.reply
      }
    } catch (e) {
      // 사용자가 직접 취소한 경우: 조용히 종료(부분 응답은 그대로 유지).
      if (e?.name === 'AbortError') {
        return
      }
      // 스트리밍 실패 → 비스트리밍으로 1회 폴백 시도.
      // 단, 이미 일부 토큰을 받았다면(부분 버블 존재) 중복 응답을 막기 위해 폴백하지 않고 에러만 노출.
      if (!assistantMsg) {
        try {
          const { data } = await assistantApi.chat({
            conversationId: conversationId.value,
            message: text,
          })
          if (data?.conversationId) conversationId.value = data.conversationId
          const reply = data?.reply ?? ''
          pushMessage('assistant', reply)
          return
        } catch (fallbackErr) {
          error.value =
            fallbackErr.response?.data?.message ??
            fallbackErr.message ??
            '답변을 받지 못했어요.'
          return
        }
      }
      // 실패를 성공으로 위장하지 않는다: 에러를 노출.
      error.value = e?.message ?? '답변을 받지 못했어요.'
    } finally {
      loading.value = false
      streaming.value = false
      abortController = null
    }
  }

  /** 진행 중인 응답 스트리밍을 중단한다(부분 응답은 화면에 유지). */
  function cancel() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    loading.value = false
    streaming.value = false
  }

  function reset() {
    cancel()
    messages.value = []
    conversationId.value = null
    error.value = null
    loading.value = false
    streaming.value = false
  }

  return {
    messages,
    conversationId,
    loading,
    streaming,
    error,
    send,
    cancel,
    reset,
  }
})
