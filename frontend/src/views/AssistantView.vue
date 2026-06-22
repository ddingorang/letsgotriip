# Created: 2026-06-19
<template>
  <div class="page">
    <!-- Header -->
    <header class="chat-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <div class="header-center">
        <span class="header-title">AI 어시스턴트</span>
        <span class="header-sub">내 문서를 바탕으로 답해드려요</span>
      </div>
      <button class="docs-btn" @click="$router.push('/documents')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" />
        </svg>
      </button>
      <button class="docs-btn" :class="{ on: mem.useRecords }" title="개인화 설정" @click="showSettings = !showSettings">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" />
        </svg>
      </button>
    </header>

    <!-- 개인화(메모리) 설정 패널 — 챗봇이 내 기록을 얼마나 참고할지 -->
    <Transition name="fade">
      <div v-if="showSettings" class="mem-overlay" @click.self="showSettings = false">
        <div class="mem-panel">
          <div class="mem-head">
            <span class="mem-title">개인화 — 내 기록 활용</span>
            <button class="mem-close" @click="showSettings = false" aria-label="닫기">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </button>
          </div>
          <p class="mem-desc">챗봇이 답할 때 참고할 내 기록을 켜고 끌 수 있어요. 모두 끄면 내 기록을 보지 않아요.</p>

          <label class="mem-row master">
            <span class="mem-label">내 기록 활용</span>
            <input type="checkbox" :checked="mem.useRecords" @change="toggle('useRecords', $event.target.checked)" />
          </label>

          <div class="mem-sub" :class="{ disabled: !mem.useRecords }">
            <label v-for="opt in memOptions" :key="opt.key" class="mem-row">
              <span class="mem-label">{{ opt.label }}</span>
              <input type="checkbox" :disabled="!mem.useRecords" :checked="mem[opt.key]" @change="toggle(opt.key, $event.target.checked)" />
            </label>
          </div>

          <label class="mem-row recall">
            <span class="mem-label">대화·문서 기억(RAG)</span>
            <input type="checkbox" :checked="mem.recall" @change="toggle('recall', $event.target.checked)" />
          </label>
        </div>
      </div>
    </Transition>

    <div class="msg-scroll" ref="msgScroll">
      <!-- 빈 상태 안내 -->
      <div v-if="messages.length === 0" class="intro">
        <div class="intro-icon">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
          </svg>
        </div>
        <h3 class="intro-title">무엇이든 물어보세요</h3>
        <p class="intro-sub">
          업로드한 문서 내용을 참고해 답변해요.<br />
          <button class="intro-link" @click="$router.push('/documents')">문서 관리로 이동 →</button>
        </p>
        <button class="quick-plan-btn" type="button" @click="openPlanForm">
          ✈️ 여행 계획 세우기
        </button>
        <div class="suggest-chips">
          <button
            v-for="s in suggestedPrompts"
            :key="s"
            class="suggest-chip"
            type="button"
            @click="onSuggest(s)"
          >{{ s }}</button>
        </div>
      </div>

      <!-- 메시지 -->
      <template v-for="msg in messages" :key="msg.id">
        <!-- 사용자 -->
        <div v-if="msg.role === 'user'" class="msg-row outgoing">
          <div class="msg-col-out">
            <span class="msg-time">{{ msg.time }}</span>
            <div class="bubble outgoing-bubble">{{ msg.content }}</div>
          </div>
        </div>
        <!-- 어시스턴트: 계획 카드 -->
        <div v-else-if="msg.type === 'plan'" class="msg-row incoming">
          <div class="msg-avatar">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
            </svg>
          </div>
          <div class="msg-col plan-col">
            <div class="plan-card">
              <div class="plan-card-head">
                <span class="plan-card-badge">AI 일정</span>
                <span v-if="msg.plan.status && msg.plan.status !== 'DONE'" class="plan-card-status">{{ msg.plan.status }}</span>
              </div>
              <div class="plan-card-title">{{ msg.plan.totalSummary }}</div>

              <div v-if="msg.plan.days.length" class="plan-days">
                <div v-for="day in msg.plan.days" :key="day.dayNo" class="plan-day">
                  <div class="plan-day-head">
                    <span class="plan-day-pill">{{ day.dayNo }}일차</span>
                    <span v-if="day.summary" class="plan-day-summary">{{ day.summary }}</span>
                  </div>
                  <ul v-if="day.places && day.places.length" class="plan-place-list">
                    <li v-for="(p, i) in day.places" :key="p.contentId ?? i" class="plan-place">
                      <span class="plan-place-no">{{ i + 1 }}</span>
                      <span class="plan-place-body">
                        <span class="plan-place-name">
                          <span v-if="p.visitTime" class="plan-place-time">{{ p.visitTime }}</span>
                          {{ p.title }}
                        </span>
                        <span v-if="p.reason" class="plan-place-reason">{{ p.reason }}</span>
                      </span>
                    </li>
                  </ul>
                  <div v-else class="plan-day-empty">이 날의 일정이 없어요.</div>
                </div>
              </div>
              <div v-else class="plan-day-empty">생성된 일정이 없어요.</div>

              <div class="plan-actions">
                <button
                  class="plan-action save"
                  type="button"
                  :disabled="!msg.plan.recommendationId || planActionId === msg.id"
                  @click="onSavePlan(msg)"
                >
                  <template v-if="planActionId === msg.id && planActionKind === 'save'">
                    <span class="mini-spinner" />저장 중...
                  </template>
                  <template v-else-if="msg.plan.savedPlanId">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12" /></svg>
                    저장됨
                  </template>
                  <template v-else>계획으로 저장</template>
                </button>
                <button
                  class="plan-action eval"
                  type="button"
                  :disabled="busy || planActionId === msg.id"
                  @click="onEvaluatePlan(msg)"
                >평가받기</button>
              </div>
              <div v-if="msg.plan.savedPlanId" class="plan-saved-note">
                계획에 저장했어요!
                <button class="plan-saved-link" type="button" @click="$router.push('/plan')">계획 보기 →</button>
              </div>
            </div>
            <span class="msg-time">{{ msg.time }}</span>
          </div>
        </div>
        <!-- 어시스턴트: 일반 텍스트 -->
        <div v-else class="msg-row incoming">
          <div class="msg-avatar">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
            </svg>
          </div>
          <div class="msg-col">
            <div class="bubble incoming-bubble" :class="{ errored: msg.errored }">{{ msg.content
              }}<span v-if="streaming && msg.id === messages[messages.length - 1]?.id" class="stream-caret" /></div>
            <span v-if="msg.errored" class="partial-note">⚠ 응답이 중단된 부분 답변이에요.</span>
            <span class="msg-time">{{ msg.time }}</span>
          </div>
        </div>
      </template>

      <!-- 로딩 인디케이터 -->
      <div v-if="loading" class="msg-row incoming">
        <div class="msg-avatar">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
          </svg>
        </div>
        <div class="msg-col">
          <div class="bubble incoming-bubble typing">
            <span class="dot" /><span class="dot" /><span class="dot" />
          </div>
        </div>
      </div>

      <!-- 업로드 진행/결과 -->
      <div v-if="uploadStatus" class="upload-banner" :class="uploadStatus.kind">
        <span v-if="uploadStatus.kind === 'progress'" class="mini-spinner dark" />
        <span>{{ uploadStatus.text }}</span>
      </div>

      <!-- 에러 -->
      <div v-if="error" class="error-banner">
        <span>{{ error }}</span>
        <button v-if="lastUserMessage && !busy" class="error-retry" type="button" @click="onRetry">다시 시도</button>
      </div>
    </div>

    <!-- 첨부(+) 메뉴 -->
    <Transition name="menu-pop">
      <div v-if="attachMenuOpen" class="attach-menu-wrap">
        <div class="attach-backdrop" @click="attachMenuOpen = false" />
        <div class="attach-menu">
          <button class="attach-item" type="button" @click="pickDocument">
            <span class="attach-ico doc">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" /></svg>
            </span>
            <span class="attach-text">
              <span class="attach-name">문서 첨부</span>
              <span class="attach-sub">PDF·TXT 자료로 질문하기</span>
            </span>
          </button>
          <button class="attach-item" type="button" @click="pickVoice">
            <span class="attach-ico voice">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z" /><path d="M19 10v2a7 7 0 01-14 0v-2" /><line x1="12" y1="19" x2="12" y2="23" /></svg>
            </span>
            <span class="attach-text">
              <span class="attach-name">음성 첨부</span>
              <span class="attach-sub">녹음 파일로 질문하기</span>
            </span>
          </button>
        </div>
      </div>
    </Transition>

    <!-- 입력 바 -->
    <div class="input-bar">
      <button
        class="attach-btn"
        :class="{ open: attachMenuOpen }"
        type="button"
        aria-label="첨부"
        :disabled="uploadStatus?.kind === 'progress'"
        @click="attachMenuOpen = !attachMenuOpen"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
      <input
        v-model="inputText"
        class="msg-input"
        placeholder="메시지 입력"
        enterkeyhint="send"
        :disabled="busy"
        @keydown.enter.prevent="(e) => !e.isComposing && onSend()"
      />
      <!-- 응답 진행 중이면 전송 버튼이 '중지' 버튼으로 바뀐다 -->
      <button
        v-if="busy"
        class="send-btn stop active"
        type="button"
        aria-label="응답 중지"
        @click="onStop"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="white" stroke="none">
          <rect x="6" y="6" width="12" height="12" rx="2" />
        </svg>
      </button>
      <button
        v-else
        class="send-btn"
        :class="{ active: inputText.trim() }"
        type="button"
        aria-label="전송"
        @click="onSend"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>

    <!-- 숨김 파일 입력(문서/음성) -->
    <input
      ref="docInput"
      type="file"
      accept=".pdf,.txt"
      class="hidden-file"
      @change="onDocumentSelected"
    />
    <input
      ref="voiceInput"
      type="file"
      accept=".m4a,.mp3,.wav,audio/*"
      class="hidden-file"
      @change="onVoiceSelected"
    />

    <!-- 여행 계획 세우기 바텀시트 -->
    <PlanQuickForm
      v-if="planFormOpen"
      :loading="planning"
      @submit="onPlanSubmit"
      @close="planFormOpen = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAssistantStore } from '@/stores/assistant.js'
import { useDocumentsStore } from '@/stores/documents.js'
import { analysisApi } from '@/api/index.js'
import PlanQuickForm from '@/components/assistant/PlanQuickForm.vue'

const route = useRoute()
const assistantStore = useAssistantStore()
const documentsStore = useDocumentsStore()
const msgScroll = ref(null)
const inputText = ref('')

// PlanView 의 "이 계획을 챗봇과 다듬기"에서 진입 시(?planId=N) 입력창에 시드 메시지를 채운다.
// 자동 전송하지 않고 사용자가 확인 후 보내도록 한다(프리필 정도).
onMounted(() => {
  const planId = route.query.planId
  if (planId != null && String(planId).trim() !== '' && !inputText.value) {
    inputText.value = `내 여행 계획(planId: ${planId})을 다듬고 싶어요. 동선·일정을 살펴보고 개선점을 제안해줘.`
  }
})

const messages = computed(() => assistantStore.messages)
const loading = computed(() => assistantStore.loading)
const streaming = computed(() => assistantStore.streaming)
const error = computed(() => assistantStore.error)
const planning = computed(() => assistantStore.planning)

// ── 개인화(메모리) 설정 ─────────────────────────────────────────────────────────
const showSettings = ref(false)
const mem = computed(() => assistantStore.memoryPrefs)
const memOptions = [
  { key: 'plans', label: '내 여행 계획' },
  { key: 'favorites', label: '찜한 곳' },
  { key: 'reviews', label: '내 리뷰' },
  { key: 'stories', label: '여행 스토리' },
]
function toggle(key, value) {
  assistantStore.setMemoryPrefs({ [key]: value })
}

// 응답 진행 중(요청~첫토큰=loading, 첫토큰~종료=streaming) — 입력 잠금/중지 버튼 노출 기준
const busy = computed(() => loading.value || streaming.value)

// 빈 상태 추천 프롬프트 — 탭하면 바로 전송
const suggestedPrompts = [
  '제주 2박3일 추천해줘',
  '서울 데이트 코스 알려줘',
  '방금 올린 문서 요약해줘',
]
function onSuggest(text) {
  if (busy.value) return
  assistantStore.send(text)
}

// 에러 복구용 — 마지막 사용자 메시지(다시 시도 대상)
const lastUserMessage = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') return messages.value[i].content
  }
  return ''
})
function onRetry() {
  const text = lastUserMessage.value
  if (!text || busy.value) return
  assistantStore.send(text)
}

// ── 계획 폼(바텀시트) ──────────────────────────────────────────────────────────
const planFormOpen = ref(false)
function openPlanForm() {
  attachMenuOpen.value = false
  planFormOpen.value = true
}

// 폼 제출 → 일정 생성. 성공/실패 모두 store.createPlan 내부에서 메시지로 처리.
async function onPlanSubmit({ conditions, summary }) {
  const rec = await assistantStore.createPlan(conditions, summary)
  planFormOpen.value = false // 결과(카드/안내)가 추가되면 시트 닫기
  if (rec) scrollToBottom()
}

// ── 계획 카드 액션(저장/평가) ──────────────────────────────────────────────────
const planActionId = ref(null) // 진행 중인 카드 메시지 id
const planActionKind = ref(null) // 'save' | 'eval'

async function onSavePlan(msg) {
  const recId = msg?.plan?.recommendationId
  if (!recId || planActionId.value) return
  planActionId.value = msg.id
  planActionKind.value = 'save'
  try {
    const plan = await assistantStore.savePlanFromCard(recId)
    // 저장된 planId 를 카드에 기록(버튼 '저장됨' + 계획 보기 노출)
    if (plan?.id != null) msg.plan.savedPlanId = plan.id
  } catch {
    // 에러는 store.error 배너로 노출
  } finally {
    planActionId.value = null
    planActionKind.value = null
  }
}

// 평가받기: 저장돼 있으면 planId 로, 아니면 먼저 저장한 뒤 LLM 에 평가 요청.
async function onEvaluatePlan(msg) {
  if (busy.value || planActionId.value) return
  let planId = msg?.plan?.savedPlanId
  if (planId == null) {
    const recId = msg?.plan?.recommendationId
    if (!recId) return
    planActionId.value = msg.id
    planActionKind.value = 'eval'
    try {
      const plan = await assistantStore.savePlanFromCard(recId)
      planId = plan?.id ?? null
      if (planId != null) msg.plan.savedPlanId = planId
    } catch {
      planActionId.value = null
      planActionKind.value = null
      return
    }
    planActionId.value = null
    planActionKind.value = null
  }
  if (planId == null) return
  // 저장된 계획을 LLM 에 평가 요청(스트리밍 send 그대로 사용)
  await assistantStore.send(`방금 저장한 여행 계획(planId: ${planId})을 평가해줘. 좋은 점과 아쉬운 점, 개선 제안을 알려줘.`)
}

// ── 첨부(+) 메뉴 ───────────────────────────────────────────────────────────────
const attachMenuOpen = ref(false)
const docInput = ref(null)
const voiceInput = ref(null)

// 업로드 상태 배너: { kind: 'progress'|'done'|'error', text }
const uploadStatus = ref(null)

function pickDocument() {
  attachMenuOpen.value = false
  docInput.value?.click()
}
function pickVoice() {
  attachMenuOpen.value = false
  voiceInput.value?.click()
}

// 문서 첨부 → documents 스토어 업로드(RAG 색인). 성공 시 안내 + 요약 제안 말풍선.
async function onDocumentSelected(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (!file) return
  uploadStatus.value = { kind: 'progress', text: `'${file.name}' 업로드 중...` }
  try {
    await documentsStore.upload(file)
    uploadStatus.value = { kind: 'done', text: '문서를 추가했어요.' }
    assistantStore.pushAssistantText(
      `문서 '${file.name}'를 추가했어요. 이제 이 자료를 바탕으로 질문할 수 있어요.`,
    )
    // 자동 요약 제안 — 사용자가 확인 후 보낼 수 있게 입력창에 채운다.
    inputText.value = '방금 올린 문서 요약해줘'
    scrollToBottom()
    setTimeout(() => { if (uploadStatus.value?.kind === 'done') uploadStatus.value = null }, 2500)
  } catch (err) {
    uploadStatus.value = {
      kind: 'error',
      text: documentsStore.error ?? err?.message ?? '문서 업로드에 실패했어요.',
    }
  }
}

// 음성 첨부 → STT 업로드(색인). BE 는 dataId(Long)만 반환하고 전사 본문은 응답에 없으므로
// 입력창에 채울 텍스트가 없다. 업로드 성공 안내만 띄우고 질문은 사용자가 직접 입력하게 한다.
async function onVoiceSelected(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  uploadStatus.value = { kind: 'progress', text: `'${file.name}' 음성 업로드 중...` }
  try {
    await analysisApi.uploadVoice(file)
    uploadStatus.value = { kind: 'done', text: '음성을 업로드했어요. 질문을 입력해 주세요.' }
    setTimeout(() => { if (uploadStatus.value?.kind === 'done') uploadStatus.value = null }, 3000)
  } catch (err) {
    uploadStatus.value = {
      kind: 'error',
      text: err?.response?.data?.message ?? err?.message ?? '음성 업로드에 실패했어요.',
    }
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgScroll.value) msgScroll.value.scrollTop = msgScroll.value.scrollHeight
  })
}

async function onSend() {
  const text = inputText.value.trim()
  if (!text || busy.value) return
  inputText.value = ''
  await assistantStore.send(text)
}

function onStop() {
  assistantStore.cancel()
}

// 새 메시지/로딩 변화 시 자동 스크롤
watch(() => messages.value.length, () => scrollToBottom())
watch(loading, () => scrollToBottom())
// 스트리밍 토큰 누적으로 마지막 버블이 길어질 때도 따라 내려간다
watch(
  () => messages.value[messages.value.length - 1]?.content,
  () => {
    if (streaming.value) scrollToBottom()
  },
)
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
.back-btn, .docs-btn {
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

/* 빈 상태 안내 */
.intro {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 48px 24px 24px;
  margin: auto 0;
}
.intro-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
.intro-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 8px;
}
.intro-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.7;
}
.intro-link {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-peach);
}

.msg-row { display: flex; gap: 8px; }
.msg-row.incoming { align-items: flex-start; }
.msg-row.outgoing { justify-content: flex-end; }

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-peach);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.msg-col { display: flex; flex-direction: column; gap: 3px; max-width: 76%; }
.msg-col-out { display: flex; flex-direction: row; align-items: flex-end; gap: 5px; max-width: 76%; }
.msg-time { font-size: 11px; color: var(--color-ink-muted); flex-shrink: 0; padding-bottom: 2px; }

.bubble {
  padding: 11px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.55;
  letter-spacing: -0.2px;
  white-space: pre-wrap;
  word-break: break-word;
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
/* 오류로 중단된 부분 답변 — 완성 답변과 구분 */
.incoming-bubble.errored {
  border: 1px solid #f3c0bb;
  background: #fff7f6;
}
.partial-note {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-error);
}

/* 스트리밍 진행 중 깜빡이는 캐럿 */
.stream-caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 1px;
  vertical-align: -2px;
  background: var(--color-peach);
  animation: caret-blink 1s step-end infinite;
}
@keyframes caret-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 타이핑 인디케이터 */
.typing { display: flex; align-items: center; gap: 4px; padding: 14px; }
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-ink-muted);
  animation: blink 1.2s infinite ease-in-out both;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}

/* 에러 배너 */
.error-banner {
  align-self: center;
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fdecea;
  color: var(--color-error);
  font-size: 12.5px;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: var(--radius-full);
  margin: 4px 0;
  text-align: center;
}
.error-retry {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-error);
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* 빈 상태 추천 칩 */
.suggest-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}
.suggest-chip {
  padding: 8px 14px;
  background: var(--color-white);
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-full);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  letter-spacing: -0.2px;
  transition: all 0.15s;
}
.suggest-chip:active {
  background: var(--color-peach-light);
  border-color: var(--color-peach);
  color: var(--color-peach-pressed);
}

/* 입력 바 */
.input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px calc(10px + var(--safe-bottom));
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
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
.msg-input:disabled { opacity: 0.7; }
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
.send-btn:disabled { opacity: 0.7; }
.send-btn.stop { background: var(--color-peach); }

/* ── 빈 상태 퀵 액션 ───────────────────────────────────────────────────────── */
.quick-plan-btn {
  margin-top: 20px;
  padding: 12px 22px;
  background: linear-gradient(90deg, var(--color-peach) 0%, #f9a96a 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.2px;
  border-radius: var(--radius-full);
  box-shadow: 0 4px 14px rgba(247, 143, 87, 0.3);
}

/* ── 첨부(+) 버튼 ──────────────────────────────────────────────────────────── */
.attach-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-ink-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.18s, background 0.15s, color 0.15s;
}
.attach-btn.open {
  background: var(--color-peach-light);
  color: var(--color-peach);
  transform: rotate(45deg);
}
.attach-btn:disabled { opacity: 0.5; }

.hidden-file {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

/* ── 첨부 메뉴 ─────────────────────────────────────────────────────────────── */
/* 뷰포트 기준 고정 — .page(overflow:hidden)에 갇히지 않고 항상 최상위(BottomNav 위) */
.attach-menu-wrap { position: fixed; inset: 0; z-index: 1000; }
.attach-backdrop { position: absolute; inset: 0; }
.attach-menu {
  position: absolute;
  left: 12px;
  bottom: calc(64px + var(--safe-bottom));
  width: 246px;
  background: var(--color-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-dropdown);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.attach-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 10px;
  border-radius: var(--radius-md);
  text-align: left;
  transition: background 0.15s;
}
.attach-item:active { background: var(--color-surface); }
.attach-ico {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.attach-ico.doc { background: var(--color-peach-light); color: var(--color-peach); }
.attach-ico.voice { background: #ede7fb; color: var(--color-ai); }
.attach-text { display: flex; flex-direction: column; gap: 2px; }
.attach-name { font-size: 14px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.2px; }
.attach-sub { font-size: 11.5px; color: var(--color-ink-muted); }

.menu-pop-enter-active, .menu-pop-leave-active { transition: opacity 0.15s; }
.menu-pop-enter-from, .menu-pop-leave-to { opacity: 0; }

/* ── 업로드 배너 ───────────────────────────────────────────────────────────── */
.upload-banner {
  align-self: center;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: var(--radius-full);
  margin: 2px 0;
  text-align: center;
}
.upload-banner.progress { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.upload-banner.done { background: #e8f6ee; color: #1f9254; }
.upload-banner.error { background: #fdecea; color: var(--color-error); }

.mini-spinner {
  width: 13px;
  height: 13px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
.mini-spinner.dark {
  border: 2px solid rgba(224, 116, 58, 0.35);
  border-top-color: var(--color-peach-pressed);
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── 계획 카드 ─────────────────────────────────────────────────────────────── */
.msg-col.plan-col { max-width: 88%; }
.plan-card {
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  border-radius: 4px 16px 16px 16px;
  box-shadow: var(--shadow-card);
  padding: 14px;
  overflow: hidden;
}
.plan-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.plan-card-badge {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  padding: 3px 9px;
  border-radius: var(--radius-full);
}
.plan-card-status {
  font-size: 10px;
  font-weight: 600;
  color: #b9851c;
  background: rgba(255, 200, 100, 0.2);
  border: 1px solid rgba(255, 200, 100, 0.45);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}
.plan-card-title {
  font-size: 14.5px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  line-height: 1.4;
  margin-bottom: 12px;
}
.plan-days { display: flex; flex-direction: column; gap: 14px; }
.plan-day-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}
.plan-day-pill {
  background: var(--color-peach);
  color: #fff;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}
.plan-day-summary {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-ink-secondary);
  line-height: 1.4;
  padding-top: 2px;
}
.plan-place-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.plan-place { display: flex; gap: 9px; }
.plan-place-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1.5px solid var(--color-peach);
  color: var(--color-peach);
  font-size: 10px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.plan-place-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.plan-place-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}
.plan-place-time {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--color-peach);
  background: var(--color-peach-light);
  padding: 1px 6px;
  border-radius: var(--radius-full);
  margin-right: 4px;
}
.plan-place-reason {
  font-size: 11.5px;
  color: var(--color-ink-secondary);
  line-height: 1.4;
}
.plan-day-empty {
  font-size: 12px;
  color: var(--color-ink-muted);
  padding: 4px 0;
}
.plan-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--color-line-light);
}
.plan-action {
  flex: 1;
  height: 38px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: opacity 0.15s;
}
.plan-action.save {
  background: var(--color-peach);
  color: #fff;
}
.plan-action.eval {
  background: var(--color-white);
  color: var(--color-peach-pressed);
  border: 1.5px solid var(--color-peach);
}
.plan-action:disabled { opacity: 0.6; cursor: not-allowed; }
.plan-saved-note {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-peach-pressed);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.plan-saved-link { font-size: 12px; font-weight: 700; color: var(--color-peach); white-space: nowrap; }

/* ── 개인화(메모리) 설정 패널 ──────────────────────────────────────────────────── */
.docs-btn.on { color: var(--color-peach); }
.mem-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0, 0, 0, 0.35);
  display: flex; align-items: flex-start; justify-content: flex-end;
  padding: 56px 12px 0;
}
.mem-panel {
  width: 100%; max-width: 320px;
  background: var(--color-white);
  border-radius: var(--radius-lg);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.22);
  padding: 14px 16px 16px;
}
.mem-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.mem-title { font-size: 15px; font-weight: 800; color: var(--color-ink); letter-spacing: -0.3px; }
.mem-close { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; color: var(--color-ink-muted); }
.mem-desc { font-size: 12px; color: var(--color-ink-muted); line-height: 1.5; margin-bottom: 10px; }
.mem-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 2px; cursor: pointer;
}
.mem-row .mem-label { font-size: 13.5px; font-weight: 600; color: var(--color-ink); }
.mem-row.master { border-top: 1px solid var(--color-line-light); }
.mem-row.master .mem-label, .mem-row.recall .mem-label { font-weight: 800; }
.mem-row input[type=checkbox] { width: 18px; height: 18px; accent-color: var(--color-peach); }
.mem-sub { padding-left: 10px; }
.mem-sub.disabled { opacity: 0.45; }
.mem-row.recall { border-top: 1px solid var(--color-line-light); margin-top: 4px; }
</style>
