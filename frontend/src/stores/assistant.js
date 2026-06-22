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
import { assistantApi, recommendApi } from '@/api/index.js'

export const useAssistantStore = defineStore('assistant', () => {
  // 메시지 배열. 일반 텍스트는 { id, role:'user'|'assistant', content, time },
  // 계획 카드는 { id, role:'assistant', type:'plan', plan:{...}, time } 형태.
  const messages = ref([])
  const conversationId = ref(null)
  const loading = ref(false) // 요청 시작 ~ 첫 토큰 도착 전(typing 인디케이터)
  const streaming = ref(false) // 첫 토큰 ~ 응답 종료(취소 가능 구간)
  const error = ref(null)

  // 계획 폼 제출 → recommendApi.create 진행 중 여부(폼/버튼 잠금용)
  const planning = ref(false)

  // ── 개인화(메모리) 설정 — 챗봇이 내 기록을 얼마나 참고할지 ──────────────────────
  // localStorage 영속. 매 요청 body.memory 로 전송. 서버는 userId 기준으로만 조회(보안).
  const MEM_KEY = 'triip.assistantMemory'
  const DEFAULT_MEM = { useRecords: true, plans: true, favorites: true, reviews: true, stories: true, recall: true }
  function loadMem() {
    try {
      const raw = localStorage.getItem(MEM_KEY)
      return raw ? { ...DEFAULT_MEM, ...JSON.parse(raw) } : { ...DEFAULT_MEM }
    } catch {
      return { ...DEFAULT_MEM }
    }
  }
  const memoryPrefs = ref(loadMem())
  function setMemoryPrefs(patch) {
    memoryPrefs.value = { ...memoryPrefs.value, ...patch }
    try { localStorage.setItem(MEM_KEY, JSON.stringify(memoryPrefs.value)) } catch { /* noop */ }
  }

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

  /** 임의 형태(계획 카드 등)의 메시지를 그대로 추가한다. */
  function pushRaw(partial) {
    const msg = { id: makeId(), time: nowTime(), ...partial }
    messages.value.push(msg)
    return msg
  }

  /** 화면에만 사용자 말풍선을 추가한다(LLM 전송 없이 폼 요약 등 표시용). */
  function pushUserText(text) {
    return pushMessage('user', text)
  }

  /** 화면에만 어시스턴트 안내 말풍선을 추가한다(LLM 전송 없이). */
  function pushAssistantText(text) {
    return pushMessage('assistant', text)
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
        { conversationId: conversationId.value, message: text, memory: memoryPrefs.value },
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

      const reply = result?.reply ?? ''
      // S3가 chatStream 결과에 { errored, errorMessage }를 실어준다(event:error 수신 시 true).
      // 스트림이 HTTP 200으로 끝났더라도 서버가 오류를 알린 경우 = 부분/빈 응답이므로
      // 완성으로 위장하지 않고 실패를 노출한다.
      if (result?.errored) {
        error.value = result.errorMessage ?? '응답 생성 중 오류가 발생했어요.'
        // 일부 토큰을 받았다면(부분 버블) 부분 답변임을 표식한다 — 완성으로 오인하지 않도록.
        if (assistantMsg) {
          assistantMsg.errored = true
        }
        return
      }
      // 스트림이 정상 종료(HTTP 200)됐지만 토큰이 하나도 없는 경우 = 서버가 event:error를
      // 보내고 done으로 마무리한 상황(BE AssistantController). 이를 빈 성공으로 위장하지 않고
      // 실패로 노출한다. (정상 응답은 항상 최소 1개 이상의 토큰을 동반한다.)
      if (!assistantMsg && !reply) {
        error.value = '답변을 받지 못했어요. 잠시 후 다시 시도해 주세요.'
        return
      }
      // 토큰이 하나도 안 온 경우(버블 미생성) — 누적 결과로 채운다.
      if (!assistantMsg) {
        pushMessage('assistant', reply)
      } else if (!assistantMsg.content && reply) {
        assistantMsg.content = reply
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
            memory: memoryPrefs.value,
          })
          if (data?.conversationId) conversationId.value = data.conversationId
          const reply = data?.reply ?? ''
          // 폴백마저 빈 응답이면 빈 말풍선으로 위장하지 않고 실패를 노출한다.
          if (!reply) {
            error.value = '답변을 받지 못했어요. 잠시 후 다시 시도해 주세요.'
            return
          }
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

  /**
   * 계획 폼 제출 → recommendApi.create(conditions) 로 일정을 생성하고
   * 결과를 'plan' 타입 어시스턴트 메시지(카드)로 추가한다.
   * @param {object} conditions  recommendApi.create payload({ areaCode, startDate, ... })
   * @param {string} [summary]   사용자 말풍선에 표시할 사람용 요약
   * @returns {Promise<object|null>} 생성된 recommendation(저장/평가에 id 사용)
   */
  async function createPlan(conditions, summary) {
    if (planning.value) return null
    // 폼 선택 내용을 사용자 말풍선으로 먼저 표시(구조화 입력의 흔적)
    if (summary) pushUserText(summary)

    planning.value = true
    loading.value = true // 카드 생성 전까지 typing 인디케이터 노출
    error.value = null
    try {
      const { data } = await recommendApi.create(conditions)
      const days = data?.draft?.days ?? []
      pushRaw({
        role: 'assistant',
        type: 'plan',
        plan: {
          recommendationId: data?.id ?? null,
          status: data?.status ?? null,
          totalSummary: data?.draft?.totalSummary ?? '나만의 여행 일정',
          days,
        },
      })
      return data
    } catch (e) {
      if (e.code === 'ERR_CANCELED' || e.name === 'CanceledError') return null
      const code = e.response?.data?.code
      let msg
      if (code === 'RECO409') msg = '이미 생성 중이에요. 잠시 후 다시 시도해 주세요.'
      else if (code === 'RECO422' || code === 'RECO502') msg = '일정 생성에 실패했어요. 다시 시도해 주세요.'
      else msg = e.response?.data?.message ?? e.message ?? '일정 생성에 실패했어요.'
      error.value = msg
      pushAssistantText(`일정을 만들지 못했어요. ${msg}`)
      return null
    } finally {
      planning.value = false
      loading.value = false
    }
  }

  /**
   * 계획 카드를 실제 계획으로 저장한다(recommendApi.savePlan).
   * @param {number|string} recommendationId
   * @returns {Promise<object|null>} 저장된 PlanDetail({ id, ... })
   */
  async function savePlanFromCard(recommendationId) {
    if (!recommendationId) return null
    error.value = null
    try {
      const { data } = await recommendApi.savePlan(recommendationId)
      return data
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '저장 중 오류가 발생했어요.'
      throw e
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
    planning.value = false
  }

  return {
    messages,
    conversationId,
    loading,
    streaming,
    planning,
    error,
    memoryPrefs,
    setMemoryPrefs,
    send,
    cancel,
    reset,
    createPlan,
    savePlanFromCard,
    pushUserText,
    pushAssistantText,
  }
})
