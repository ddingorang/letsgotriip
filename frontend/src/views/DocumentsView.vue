# Created: 2026-06-19
<template>
  <div class="page">
    <!-- Header -->
    <header class="doc-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="header-title">문서 관리</span>
      <button class="assistant-link" @click="$router.push('/assistant')">어시스턴트</button>
    </header>

    <div class="scroll-content">
      <!-- 업로드 영역 -->
      <section class="upload-section">
        <p class="upload-hint">PDF, 텍스트, 이미지 파일을 올리면 AI 어시스턴트가 내용을 학습해 답변에 활용해요.</p>
        <input
          ref="fileInput"
          type="file"
          accept=".pdf,.txt,image/*"
          class="file-input-hidden"
          @change="onFileSelected"
        />
        <button class="upload-btn" :disabled="docStore.uploading" @click="triggerFilePicker">
          <span v-if="docStore.uploading" class="spinner" />
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" /><polyline points="17 8 12 3 7 8" /><line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          {{ docStore.uploading ? '업로드 중…' : '파일 업로드' }}
        </button>
        <p v-if="docStore.error" class="error-text">{{ docStore.error }}</p>
      </section>

      <!-- 목록 -->
      <section class="list-section">
        <div class="list-header">
          <span class="list-title">내 문서</span>
          <span class="list-count">{{ docStore.items.length }}</span>
        </div>

        <div v-if="docStore.loading" class="state-text">불러오는 중…</div>

        <ul v-else-if="docStore.items.length > 0" class="doc-list">
          <li v-for="doc in docStore.items" :key="doc.id" class="doc-row">
            <span class="doc-icon" v-html="iconFor(doc)" />
            <div class="doc-info">
              <span class="doc-name">{{ doc.filename }}</span>
              <div class="doc-meta">
                <span v-if="doc.type" class="doc-type">{{ shortType(doc.type) }}</span>
                <span :class="['doc-status', statusClass(doc)]">{{ statusLabel(doc) }}</span>
              </div>
            </div>
            <button
              class="del-btn"
              :disabled="deletingId === doc.id"
              @click="onDelete(doc)"
            >
              <span v-if="deletingId === doc.id" class="spinner small" />
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6" /><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
              </svg>
            </button>
          </li>
        </ul>

        <div v-else class="empty-box">
          <div class="empty-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><polyline points="14 2 14 8 20 8" />
            </svg>
          </div>
          <h3 class="empty-title">아직 등록한 문서가 없어요</h3>
          <p class="empty-sub">파일을 업로드하면 어시스턴트가<br />내용을 바탕으로 답변해드려요.</p>
        </div>
      </section>

      <div class="bottom-spacer" />
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useDocumentsStore } from '@/stores/documents.js'

const docStore = useDocumentsStore()
const fileInput = ref(null)
const deletingId = ref(null)

// ── 토스트 ────────────────────────────────────────────────────────────────────
const toast = ref('')
let toastTimer = null
function showToast(message) {
  toast.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2200)
}

// ── 업로드 ────────────────────────────────────────────────────────────────────
function triggerFilePicker() {
  if (docStore.uploading) return
  fileInput.value?.click()
}

async function onFileSelected(e) {
  const file = e.target.files?.[0]
  // 동일 파일 재선택 시에도 change 가 발생하도록 입력값 초기화
  e.target.value = ''
  if (!file) return
  try {
    await docStore.upload(file)
    showToast('업로드했어요.')
  } catch {
    // 에러 메시지는 docStore.error 로 표시됨
  }
}

// ── 삭제 ──────────────────────────────────────────────────────────────────────
async function onDelete(doc) {
  if (deletingId.value) return
  if (!window.confirm(`'${doc.filename}' 문서를 삭제할까요?`)) return
  deletingId.value = doc.id
  try {
    await docStore.remove(doc.id)
    showToast('삭제했어요.')
  } catch (e) {
    showToast(e.response?.data?.message ?? '삭제하지 못했어요.')
  } finally {
    deletingId.value = null
  }
}

// ── 표시 헬퍼 ─────────────────────────────────────────────────────────────────
function shortType(type) {
  if (!type) return ''
  const t = String(type).toLowerCase()
  if (t.includes('pdf')) return 'PDF'
  if (t.includes('image') || t.includes('png') || t.includes('jpg') || t.includes('jpeg')) return '이미지'
  if (t.includes('text') || t.includes('txt')) return '텍스트'
  return type
}

// doc 객체(또는 상태 문자열)를 받아 표시 라벨을 만든다.
// 색인된 내용이 없는 문서(EMPTY/추출 글자수 0)는 '완료'로 위장하지 않고
// '내용 없음'으로 구분 표기한다 — 질문에 쓸 수 없는 문서임을 알린다.
function statusLabel(doc) {
  const status = typeof doc === 'object' && doc !== null ? doc.status : doc
  const upper = String(status ?? '').toUpperCase()
  switch (upper) {
    case 'PENDING':
    case 'PROCESSING':
    case 'INDEXING':
      return '처리 중'
    case 'FAILED':
    case 'ERROR':
      return '색인 실패'
    case 'EMPTY':
    case 'NO_TEXT':
      return '내용 없음'
    case 'READY':
    case 'INDEXED':
    case 'INGESTED':
    case 'DONE':
    case 'COMPLETED':
      // INGESTED여도 추출 글자수가 0이면 색인된 내용이 없는 문서다.
      if (typeof doc === 'object' && doc !== null && Number(doc.extractedChars) === 0) {
        return '내용 없음'
      }
      return '완료'
    default:
      return '완료'
  }
}

function statusClass(doc) {
  const label = statusLabel(doc)
  if (label === '처리 중') return 'status-processing'
  if (label === '색인 실패') return 'status-failed'
  if (label === '내용 없음') return 'status-empty'
  return 'status-ready'
}

function iconFor(doc) {
  const t = shortType(doc.type)
  if (t === '이미지') {
    return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>`
  }
  // PDF / 텍스트 / 기타 = 문서 아이콘
  return `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>`
}

onMounted(() => {
  docStore.list()
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

/* Header */
.doc-header {
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
.assistant-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-peach);
  padding: 6px 8px;
}

.scroll-content { flex: 1; overflow-y: auto; }

/* Upload */
.upload-section {
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--color-line-light);
}
.upload-hint {
  font-size: 13px;
  color: var(--color-ink-secondary);
  line-height: 1.6;
  letter-spacing: -0.2px;
  margin-bottom: 14px;
}
.file-input-hidden { display: none; }
.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 13px 0;
  background: var(--color-peach);
  color: white;
  font-size: 14.5px;
  font-weight: 700;
  border-radius: var(--radius-md);
  letter-spacing: -0.3px;
}
.upload-btn:disabled { opacity: 0.6; }
.error-text {
  margin-top: 10px;
  font-size: 12.5px;
  color: var(--color-error);
  letter-spacing: -0.2px;
}

/* List */
.list-section { padding: 16px 0 0; }
.list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px 10px;
}
.list-title { font-size: 15px; font-weight: 700; color: var(--color-ink); }
.list-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 1px 9px;
}
.state-text {
  padding: 24px 20px;
  font-size: 13.5px;
  color: var(--color-ink-muted);
  text-align: center;
}

.doc-list { display: flex; flex-direction: column; }
.doc-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-line-light);
}
.doc-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-peach-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.doc-info { flex: 1; min-width: 0; }
.doc-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.doc-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}
.doc-type {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-ink-muted);
}
.doc-status {
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-full);
  padding: 2px 8px;
}
.status-ready { background: var(--color-peach-light); color: var(--color-peach-pressed); }
.status-processing { background: var(--color-surface); color: var(--color-ink-secondary); }
.status-failed { background: #fdecea; color: var(--color-error); }
.status-empty { background: #fff4e0; color: #b9851c; }

.del-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  flex-shrink: 0;
}
.del-btn:disabled { opacity: 0.5; }

/* Empty */
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 24px;
  text-align: center;
}
.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  margin-bottom: 6px;
}
.empty-sub {
  font-size: 13.5px;
  color: var(--color-ink-muted);
  line-height: 1.6;
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
.spinner.small {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-line);
  border-top-color: var(--color-peach);
}
@keyframes spin { to { transform: rotate(360deg); } }

.bottom-spacer { height: 32px; }

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
