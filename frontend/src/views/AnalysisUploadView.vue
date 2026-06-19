# Created: 2026-06-19
<template>
  <div class="page">
    <!-- Header -->
    <header class="an-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="header-title">대화 분석 업로드</span>
      <span class="header-spacer" />
    </header>

    <div class="scroll-content">
      <!-- 안내 -->
      <p class="intro">
        음성 통화 녹음이나 카카오톡 대화 내보내기 파일을 올리면, 내용을 텍스트로 추출해 여행 취향 분석에 활용해요.
      </p>

      <!-- 모드 탭 -->
      <div class="mode-tabs" role="tablist">
        <button
          v-for="m in modes"
          :key="m.key"
          role="tab"
          :class="['mode-tab', { active: mode === m.key }]"
          :aria-selected="mode === m.key"
          :disabled="busy"
          @click="selectMode(m.key)"
        >
          <span class="mode-icon" v-html="m.icon" />
          <span class="mode-label">{{ m.label }}</span>
          <span class="mode-sub">{{ m.sub }}</span>
        </button>
      </div>

      <!-- 업로드 카드 -->
      <section class="upload-card">
        <input
          ref="fileInput"
          type="file"
          :accept="activeMode.accept"
          class="file-input-hidden"
          @change="onFileSelected"
        />

        <!-- 드롭/선택 영역 -->
        <button
          v-if="!selectedFile"
          class="dropzone"
          :disabled="busy"
          @click="triggerFilePicker"
        >
          <span class="dropzone-icon" v-html="activeMode.icon" />
          <span class="dropzone-title">{{ activeMode.pickLabel }}</span>
          <span class="dropzone-hint">{{ activeMode.hint }}</span>
        </button>

        <!-- 선택된 파일 -->
        <div v-else class="file-row">
          <span class="file-icon" v-html="activeMode.icon" />
          <div class="file-info">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">{{ prettySize(selectedFile.size) }}</span>
          </div>
          <button v-if="!busy" class="file-clear" aria-label="파일 제거" @click="clearFile">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <!-- 진행 바 -->
        <div v-if="busy" class="progress-wrap">
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: progressWidth }" />
          </div>
          <span class="progress-label">{{ phaseLabel }}</span>
        </div>

        <!-- 액션 버튼 -->
        <button
          class="submit-btn"
          :disabled="!selectedFile || busy"
          @click="onUpload"
        >
          <span v-if="busy" class="spinner" />
          {{ busy ? '처리 중…' : '업로드하고 분석하기' }}
        </button>

        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      </section>

      <!-- 결과 -->
      <section v-if="result" class="result-card">
        <div class="result-head">
          <span class="result-badge">완료</span>
          <span class="result-title">{{ result.modeLabel }} 분석이 등록됐어요</span>
        </div>

        <dl class="result-meta">
          <div class="meta-row">
            <dt>파일</dt>
            <dd class="meta-file">{{ result.fileName }}</dd>
          </div>
          <div class="meta-row">
            <dt>분석 ID</dt>
            <dd>#{{ result.dataId }}</dd>
          </div>
        </dl>

        <!-- 전사/원문 미리보기 (카카오 .txt 는 클라이언트에서 읽어 표시) -->
        <div v-if="result.preview" class="transcript">
          <div class="transcript-head">
            <span class="transcript-title">{{ result.previewTitle }}</span>
            <span class="transcript-count">{{ result.previewChars.toLocaleString() }}자</span>
          </div>
          <pre class="transcript-body">{{ result.preview }}</pre>
          <p v-if="result.truncated" class="transcript-more">… 일부만 표시했어요 (전체 내용은 분석에 사용돼요)</p>
        </div>
        <p v-else class="transcript-note">
          업로드한 파일의 텍스트 추출과 분석은 서버에서 처리됐어요. 추출된 전사 내용은 취향 분석에 자동으로 반영돼요.
        </p>

        <button class="again-btn" @click="resetForAnother">다른 파일 업로드</button>
      </section>
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { analysisApi } from '@/api/index.js'

// ── 모드 정의 ─────────────────────────────────────────────────────────────────
const VOICE_ICON = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z"/><path d="M19 10v2a7 7 0 01-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>`
const KAKAO_ICON = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>`

const modes = [
  {
    key: 'voice',
    label: '음성 통화',
    sub: 'm4a · mp3 · wav',
    accept: '.m4a,.mp3,.wav,audio/*',
    pickLabel: '음성 파일 선택',
    hint: 'm4a, mp3, wav 형식을 지원해요',
    icon: VOICE_ICON,
  },
  {
    key: 'kakao',
    label: '카카오톡',
    sub: '대화 내보내기 .txt',
    accept: '.txt,text/plain',
    pickLabel: '대화 파일 선택',
    hint: '카카오톡 "대화 내보내기"로 받은 .txt 파일',
    icon: KAKAO_ICON,
  },
]

const mode = ref('voice')
const activeMode = computed(() => modes.find((m) => m.key === mode.value))

// ── 상태 ──────────────────────────────────────────────────────────────────────
const fileInput = ref(null)
const selectedFile = ref(null)
const uploadPct = ref(0)
const phase = ref('idle') // idle | uploading | processing
const errorMsg = ref('')
const result = ref(null)

const busy = computed(() => phase.value === 'uploading' || phase.value === 'processing')
const progressWidth = computed(() =>
  phase.value === 'processing' ? '100%' : `${Math.max(4, uploadPct.value)}%`,
)
const phaseLabel = computed(() => {
  if (phase.value === 'uploading') return `업로드 중… ${uploadPct.value}%`
  if (phase.value === 'processing') return mode.value === 'voice' ? '음성을 텍스트로 변환 중…' : '대화 내용을 분석 중…'
  return ''
})

// ── 토스트 ────────────────────────────────────────────────────────────────────
const toast = ref('')
let toastTimer = null
function showToast(message) {
  toast.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2400)
}

// ── 모드/파일 선택 ────────────────────────────────────────────────────────────
function selectMode(key) {
  if (busy.value || mode.value === key) return
  mode.value = key
  clearFile()
  errorMsg.value = ''
}

function triggerFilePicker() {
  if (busy.value) return
  fileInput.value?.click()
}

function onFileSelected(e) {
  const file = e.target.files?.[0]
  // 동일 파일 재선택 시에도 change 가 발생하도록 초기화
  e.target.value = ''
  if (!file) return
  errorMsg.value = ''
  if (!isAccepted(file)) {
    errorMsg.value = mode.value === 'voice'
      ? '음성 파일(m4a, mp3, wav)만 올릴 수 있어요.'
      : '카카오톡 대화 .txt 파일만 올릴 수 있어요.'
    return
  }
  selectedFile.value = file
}

function isAccepted(file) {
  const name = (file.name || '').toLowerCase()
  if (mode.value === 'voice') {
    return /\.(m4a|mp3|wav)$/.test(name) || (file.type || '').startsWith('audio/')
  }
  return name.endsWith('.txt') || file.type === 'text/plain'
}

function clearFile() {
  selectedFile.value = null
  uploadPct.value = 0
}

// ── 업로드 ────────────────────────────────────────────────────────────────────
async function onUpload() {
  if (!selectedFile.value || busy.value) return
  const file = selectedFile.value
  const currentMode = mode.value
  errorMsg.value = ''
  result.value = null
  uploadPct.value = 0
  phase.value = 'uploading'

  // 카카오 .txt 는 브라우저에서 원문을 미리 읽어 결과 미리보기로 쓴다.
  let previewPayload = null
  if (currentMode === 'kakao') {
    previewPayload = await readTextPreview(file).catch(() => null)
  }

  // analysisApi.uploadXxx 래퍼는 file 인자만 받아 진행률 콜백을 전달할 수 없으므로,
  // 업로드 단계 동안 진행 바를 부드럽게 채우는 의사(疑似) 진행을 사용한다.
  // 실제 응답이 오면 즉시 processing → 완료로 전환된다.
  const tick = setInterval(() => {
    if (phase.value !== 'uploading') return
    // 90% 까지 점근적으로 증가 (남은 거리의 일부씩)
    uploadPct.value = Math.min(90, uploadPct.value + Math.max(1, Math.round((90 - uploadPct.value) * 0.18)))
  }, 200)

  try {
    const fn = currentMode === 'voice' ? analysisApi.uploadVoice : analysisApi.uploadKakao
    phase.value = 'uploading'
    const res = await fn(file)
    clearInterval(tick)
    phase.value = 'processing'
    uploadPct.value = 100

    const dataId = extractDataId(res)
    result.value = buildResult(currentMode, file, dataId, previewPayload)
    clearFile()
    showToast('분석을 등록했어요.')
  } catch (err) {
    errorMsg.value = readableError(err)
  } finally {
    clearInterval(tick)
    phase.value = 'idle'
    uploadPct.value = 0
  }
}

function extractDataId(res) {
  // BE 응답 본문은 Long(dataId) 자체. axios → res.data 가 숫자.
  const d = res?.data
  if (d == null) return null
  if (typeof d === 'number') return d
  if (typeof d === 'object') return d.dataId ?? d.id ?? null
  const n = Number(d)
  return Number.isNaN(n) ? d : n
}

function buildResult(currentMode, file, dataId, previewPayload) {
  const modeLabel = currentMode === 'voice' ? '음성 통화' : '카카오톡 대화'
  const base = {
    modeLabel,
    fileName: file.name,
    dataId: dataId ?? '—',
    preview: null,
    previewTitle: '',
    previewChars: 0,
    truncated: false,
  }
  if (previewPayload) {
    return {
      ...base,
      preview: previewPayload.text,
      previewTitle: '대화 원문 미리보기',
      previewChars: previewPayload.totalChars,
      truncated: previewPayload.truncated,
    }
  }
  return base
}

// 카카오 .txt 미리보기 (최대 4000자)
function readTextPreview(file) {
  const LIMIT = 4000
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const full = String(reader.result ?? '')
      resolve({
        text: full.length > LIMIT ? full.slice(0, LIMIT) : full,
        totalChars: full.length,
        truncated: full.length > LIMIT,
      })
    }
    reader.onerror = () => reject(reader.error)
    reader.readAsText(file, 'utf-8')
  })
}

function readableError(err) {
  if (err?.response) {
    const status = err.response.status
    const msg = err.response.data?.message
    if (msg) return msg
    if (status === 401) return '로그인이 필요해요. 다시 로그인해 주세요.'
    if (status === 413) return '파일 용량이 너무 커요.'
    if (status >= 500) return '서버에서 처리 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.'
    return `업로드에 실패했어요. (${status})`
  }
  if (err?.code === 'ECONNABORTED') return '시간이 초과됐어요. 파일이 크다면 잠시 후 다시 시도해 주세요.'
  return '업로드에 실패했어요. 네트워크 상태를 확인해 주세요.'
}

function resetForAnother() {
  result.value = null
  errorMsg.value = ''
  clearFile()
}

// ── 표시 헬퍼 ─────────────────────────────────────────────────────────────────
function prettySize(bytes) {
  if (!bytes && bytes !== 0) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface-alt);
}

/* Header */
.an-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}
.header-title { font-size: 16px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.header-spacer { width: 40px; }

.scroll-content { flex: 1; overflow-y: auto; padding: 18px 20px calc(40px + var(--safe-bottom)); }

.intro {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
  line-height: 1.65;
  letter-spacing: -0.2px;
  margin-bottom: 18px;
}

/* Mode tabs */
.mode-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 18px;
}
.mode-tab {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 14px 14px 13px;
  background: var(--color-white);
  border: 1.5px solid var(--color-line-light);
  border-radius: var(--radius-lg);
  text-align: left;
  transition: border-color var(--duration-fast, 160ms), background var(--duration-fast, 160ms);
}
.mode-tab .mode-icon { color: var(--color-ink-muted); display: inline-flex; }
.mode-tab.active {
  border-color: var(--color-peach);
  background: var(--color-peach-light);
}
.mode-tab.active .mode-icon { color: var(--color-peach); }
.mode-tab:disabled { opacity: 0.6; }
.mode-label { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.mode-tab.active .mode-label { color: var(--color-peach-pressed); }
.mode-sub { font-size: 11.5px; font-weight: 600; color: var(--color-ink-muted); }

/* Upload card */
.upload-card {
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-lg);
  padding: 16px;
}
.file-input-hidden { display: none; }

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 28px 16px;
  border: 1.5px dashed var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}
.dropzone:disabled { opacity: 0.6; }
.dropzone-icon {
  color: var(--color-peach);
  display: inline-flex;
  margin-bottom: 2px;
}
.dropzone-title { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }
.dropzone-hint { font-size: 12px; color: var(--color-ink-muted); letter-spacing: -0.2px; }

.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 4px;
}
.file-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  color: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.file-info { flex: 1; min-width: 0; }
.file-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-size { font-size: 11.5px; color: var(--color-ink-muted); margin-top: 2px; }
.file-clear {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}

/* Progress */
.progress-wrap { margin: 14px 0 4px; }
.progress-track {
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--color-line-light);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--color-peach);
  border-radius: var(--radius-full);
  transition: width 0.25s ease;
}
.progress-label {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-ink-secondary);
  letter-spacing: -0.2px;
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 13px 0;
  margin-top: 14px;
  background: var(--color-peach);
  color: white;
  font-size: 14.5px;
  font-weight: 700;
  border-radius: var(--radius-md);
  letter-spacing: -0.3px;
}
.submit-btn:disabled { opacity: 0.5; }

.error-text {
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--color-error);
  letter-spacing: -0.2px;
  line-height: 1.5;
}

/* Result */
.result-card {
  margin-top: 16px;
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-lg);
  padding: 16px;
}
.result-head { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.result-badge {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-peach-pressed);
  background: var(--color-peach-light);
  border-radius: var(--radius-full);
  padding: 3px 10px;
}
.result-title { font-size: 14.5px; font-weight: 700; color: var(--color-ink); letter-spacing: -0.3px; }

.result-meta { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }
.meta-row { display: flex; align-items: baseline; gap: 10px; }
.meta-row dt {
  flex-shrink: 0;
  width: 56px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-muted);
}
.meta-row dd { font-size: 13px; color: var(--color-ink); letter-spacing: -0.2px; min-width: 0; }
.meta-file {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.transcript {
  border-top: 1px solid var(--color-line-light);
  padding-top: 14px;
}
.transcript-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.transcript-title { font-size: 13px; font-weight: 700; color: var(--color-ink); }
.transcript-count { font-size: 11.5px; color: var(--color-ink-muted); }
.transcript-body {
  max-height: 240px;
  overflow-y: auto;
  background: var(--color-surface-alt);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-md);
  padding: 12px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--color-ink-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}
.transcript-more { margin-top: 8px; font-size: 11.5px; color: var(--color-ink-muted); }
.transcript-note {
  border-top: 1px solid var(--color-line-light);
  padding-top: 14px;
  font-size: 13px;
  color: var(--color-ink-secondary);
  line-height: 1.6;
  letter-spacing: -0.2px;
}

.again-btn {
  width: 100%;
  margin-top: 16px;
  padding: 11px 0;
  border: 1.5px solid var(--color-line);
  border-radius: var(--radius-md);
  background: var(--color-white);
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

/* Spinner */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.5);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Toast */
.toast {
  position: fixed;
  bottom: calc(40px + var(--safe-bottom));
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
