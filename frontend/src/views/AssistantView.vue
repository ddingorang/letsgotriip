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
    </header>

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
        <!-- 어시스턴트 -->
        <div v-else class="msg-row incoming">
          <div class="msg-avatar">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
            </svg>
          </div>
          <div class="msg-col">
            <div class="bubble incoming-bubble">{{ msg.content }}</div>
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

      <!-- 에러 -->
      <div v-if="error" class="error-banner">{{ error }}</div>
    </div>

    <!-- 입력 바 -->
    <div class="input-bar">
      <input
        v-model="inputText"
        class="msg-input"
        placeholder="메시지 입력"
        :disabled="loading"
        @keydown.enter.prevent="(e) => !e.isComposing && onSend()"
      />
      <button class="send-btn" :class="{ active: inputText.trim() && !loading }" :disabled="loading" @click="onSend">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useAssistantStore } from '@/stores/assistant.js'

const assistantStore = useAssistantStore()
const msgScroll = ref(null)
const inputText = ref('')

const messages = computed(() => assistantStore.messages)
const loading = computed(() => assistantStore.loading)
const error = computed(() => assistantStore.error)

function scrollToBottom() {
  nextTick(() => {
    if (msgScroll.value) msgScroll.value.scrollTop = msgScroll.value.scrollHeight
  })
}

async function onSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  await assistantStore.send(text)
}

// 새 메시지/로딩 변화 시 자동 스크롤
watch(() => messages.value.length, () => scrollToBottom())
watch(loading, () => scrollToBottom())
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
  background: #fdecea;
  color: var(--color-error);
  font-size: 12.5px;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: var(--radius-full);
  margin: 4px 0;
  text-align: center;
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
</style>
