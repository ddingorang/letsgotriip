# Created: 2026-06-19
<template>
  <div class="page">
    <!-- Header -->
    <header class="story-header">
      <h1 class="header-title">여행 스토리</h1>
      <button class="add-btn" title="스토리 작성" @click="openCreate">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </header>

    <div class="scroll-content">
      <p class="page-hint">여행 전 기대와 다녀온 뒤 회고를 한 장의 카드에 담아보세요.</p>

      <!-- 로딩 -->
      <div v-if="loading" class="state-text">불러오는 중…</div>

      <!-- 에러 -->
      <div v-else-if="loadError" class="state-box">
        <p class="state-title">스토리를 불러오지 못했어요</p>
        <button class="retry-btn" @click="fetchStories">다시 시도</button>
      </div>

      <!-- 목록 -->
      <div v-else-if="stories.length > 0" class="story-list">
        <article v-for="story in stories" :key="story.storyId ?? story.id" class="story-card">
          <div class="card-top">
            <h3 class="story-title">{{ story.title }}</h3>
            <div class="rating-stars" aria-label="별점">
              <svg
                v-for="n in 5"
                :key="n"
                width="15"
                height="15"
                viewBox="0 0 24 24"
                :fill="n <= (story.rating ?? 0) ? 'var(--color-peach)' : 'none'"
                stroke="var(--color-peach)"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
            </div>
          </div>

          <div v-if="story.beforeNote" class="note-block">
            <span class="note-label note-before">여행 전 기대</span>
            <p class="note-text">{{ story.beforeNote }}</p>
          </div>
          <div v-if="story.afterNote" class="note-block">
            <span class="note-label note-after">여행 후 회고</span>
            <p class="note-text">{{ story.afterNote }}</p>
          </div>

          <div class="card-actions">
            <button class="text-btn" @click="openEdit(story)">수정</button>
            <button class="text-btn danger" :disabled="deletingId === storyKey(story)" @click="onDelete(story)">
              <span v-if="deletingId === storyKey(story)" class="spinner small" />
              <span v-else>삭제</span>
            </button>
          </div>
        </article>
      </div>

      <!-- 빈 상태 -->
      <div v-else class="empty-box">
        <div class="empty-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
          </svg>
        </div>
        <h3 class="empty-title">아직 작성한 스토리가 없어요</h3>
        <p class="empty-sub">여행 전 기대와 다녀온 뒤 회고를<br />기록으로 남겨보세요.</p>
        <button class="empty-cta" @click="openCreate">스토리 작성하기</button>
      </div>

      <div class="bottom-spacer" />
    </div>

    <!-- ── 작성/수정 모달 ──────────────────────────────────────────────── -->
    <div v-if="modalOpen" class="modal-overlay" @click.self="closeModal">
      <div class="modal-sheet">
        <div class="sheet-handle" />
        <div class="sheet-header">
          <span class="sheet-title">{{ editing ? '스토리 수정' : '스토리 작성' }}</span>
          <button class="sheet-close" @click="closeModal">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="sheet-body">
          <label class="field">
            <span class="field-label">제목</span>
            <input v-model.trim="form.title" type="text" class="field-input" placeholder="예) 제주도 3박 4일" maxlength="60" />
          </label>

          <label class="field">
            <span class="field-label">여행 전 기대</span>
            <textarea v-model.trim="form.beforeNote" class="field-textarea" rows="3" placeholder="이번 여행에서 기대하는 점을 적어보세요." maxlength="1000" />
          </label>

          <label class="field">
            <span class="field-label">여행 후 회고</span>
            <textarea v-model.trim="form.afterNote" class="field-textarea" rows="3" placeholder="다녀온 뒤 느낀 점을 적어보세요." maxlength="1000" />
          </label>

          <div class="field">
            <span class="field-label">별점</span>
            <div class="star-picker">
              <button
                v-for="n in 5"
                :key="n"
                type="button"
                class="star-btn"
                :aria-label="`${n}점`"
                @click="form.rating = (form.rating === n ? 0 : n)"
              >
                <svg width="30" height="30" viewBox="0 0 24 24" :fill="n <= form.rating ? 'var(--color-peach)' : 'none'" stroke="var(--color-peach)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                </svg>
              </button>
            </div>
          </div>

          <p v-if="formError" class="error-text">{{ formError }}</p>
        </div>

        <div class="sheet-footer">
          <button class="submit-btn" :disabled="saving" @click="onSubmit">
            <span v-if="saving" class="spinner" />
            <span v-else>{{ editing ? '수정 완료' : '작성 완료' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 토스트 -->
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { storyApi } from '@/api/index.js'

// ── 상태 ──────────────────────────────────────────────────────────────────────
const stories = ref([])
const loading = ref(false)
const loadError = ref(false)
const deletingId = ref(null)

const modalOpen = ref(false)
const editing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formError = ref('')

const form = reactive({
  title: '',
  beforeNote: '',
  afterNote: '',
  rating: 0,
})

// ── 토스트 ────────────────────────────────────────────────────────────────────
const toast = ref('')
let toastTimer = null
function showToast(message) {
  toast.value = message
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2200)
}

// ── 헬퍼 ──────────────────────────────────────────────────────────────────────
function storyKey(story) {
  return story.storyId ?? story.id
}

// ── 목록 조회 ─────────────────────────────────────────────────────────────────
async function fetchStories() {
  loading.value = true
  loadError.value = false
  try {
    const { data } = await storyApi.list()
    stories.value = Array.isArray(data) ? data : (data?.content ?? data?.stories ?? [])
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// ── 모달 ──────────────────────────────────────────────────────────────────────
function resetForm() {
  form.title = ''
  form.beforeNote = ''
  form.afterNote = ''
  form.rating = 0
  formError.value = ''
}

function openCreate() {
  editing.value = false
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(story) {
  editing.value = true
  editingId.value = storyKey(story)
  form.title = story.title ?? ''
  form.beforeNote = story.beforeNote ?? ''
  form.afterNote = story.afterNote ?? ''
  form.rating = story.rating ?? 0
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  if (saving.value) return
  modalOpen.value = false
}

// ── 작성 / 수정 ───────────────────────────────────────────────────────────────
async function onSubmit() {
  if (saving.value) return
  if (!form.title) {
    formError.value = '제목을 입력해주세요.'
    return
  }
  formError.value = ''
  saving.value = true

  const payload = {
    title: form.title,
    beforeNote: form.beforeNote || null,
    afterNote: form.afterNote || null,
    rating: form.rating || null,
  }

  try {
    if (editing.value) {
      await storyApi.update(editingId.value, payload)
      showToast('스토리를 수정했어요.')
    } else {
      await storyApi.create(payload)
      showToast('스토리를 작성했어요.')
    }
    modalOpen.value = false
    await fetchStories()
  } catch (e) {
    formError.value = e.response?.data?.message ?? '저장하지 못했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    saving.value = false
  }
}

// ── 삭제 ──────────────────────────────────────────────────────────────────────
async function onDelete(story) {
  if (deletingId.value) return
  if (!window.confirm(`'${story.title}' 스토리를 삭제할까요?`)) return
  deletingId.value = storyKey(story)
  try {
    await storyApi.remove(storyKey(story))
    showToast('삭제했어요.')
    await fetchStories()
  } catch (e) {
    showToast(e.response?.data?.message ?? '삭제하지 못했어요.')
  } finally {
    deletingId.value = null
  }
}

onMounted(fetchStories)
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
.story-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 20px 12px;
  background: var(--color-white);
  border-bottom: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.header-title {
  font-size: 19px;
  font-weight: 800;
  color: var(--color-ink);
  letter-spacing: -0.4px;
}
.add-btn {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full);
  background: var(--color-peach-light);
  color: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
}
.page-hint {
  font-size: 13px;
  color: var(--color-ink-secondary);
  line-height: 1.6;
  letter-spacing: -0.2px;
  padding: 16px 0 4px;
}

.state-text {
  padding: 40px 0;
  font-size: 13.5px;
  color: var(--color-ink-muted);
  text-align: center;
}
.state-box {
  padding: 40px 0;
  text-align: center;
}
.state-title {
  font-size: 14px;
  color: var(--color-ink-secondary);
  margin-bottom: 14px;
}
.retry-btn {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-peach);
  border: 1px solid var(--color-peach);
  border-radius: var(--radius-full);
  padding: 7px 18px;
}

/* Story list */
.story-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 12px;
}
.story-card {
  background: var(--color-white);
  border: 1px solid var(--color-line-light);
  border-radius: var(--radius-lg);
  padding: 16px 16px 12px;
}
.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
.story-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
}
.rating-stars {
  display: flex;
  gap: 1px;
  flex-shrink: 0;
  padding-top: 2px;
}

.note-block { margin-bottom: 12px; }
.note-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  border-radius: var(--radius-full);
  padding: 2px 9px;
  margin-bottom: 6px;
}
.note-before {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
}
.note-after {
  background: var(--color-surface);
  color: var(--color-ink-secondary);
}
.note-text {
  font-size: 13.5px;
  color: var(--color-ink-secondary);
  line-height: 1.65;
  letter-spacing: -0.2px;
  white-space: pre-wrap;
  word-break: break-word;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  border-top: 1px solid var(--color-line-light);
  margin-top: 4px;
  padding-top: 8px;
}
.text-btn {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-ink-secondary);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
}
.text-btn.danger { color: var(--color-error); }
.text-btn:disabled { opacity: 0.5; }

/* Empty */
.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 24px 24px;
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
  margin-bottom: 20px;
}
.empty-cta {
  font-size: 14px;
  font-weight: 700;
  color: white;
  background: var(--color-peach);
  border-radius: var(--radius-md);
  padding: 12px 24px;
  letter-spacing: -0.3px;
}

.bottom-spacer { height: 32px; }

/* ── Modal ─────────────────────────────────────────────────────────────────── */
.modal-overlay {
  position: fixed;
  inset: 0;
  max-width: 430px;
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 80;
}
.modal-sheet {
  width: 100%;
  max-height: 88%;
  background: var(--color-white);
  border-radius: 20px 20px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: sheet-up 0.24s ease;
}
@keyframes sheet-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.sheet-handle {
  width: 40px;
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--color-line);
  margin: 10px auto 0;
  flex-shrink: 0;
}
.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 12px 20px;
  flex-shrink: 0;
}
.sheet-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.sheet-close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
}

.sheet-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.field-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}
.field-input,
.field-textarea {
  width: 100%;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  padding: 11px 13px;
  font-size: 14px;
  color: var(--color-ink);
  background: var(--color-surface-alt);
  letter-spacing: -0.2px;
}
.field-input:focus,
.field-textarea:focus {
  outline: none;
  border-color: var(--color-peach);
  background: var(--color-white);
}
.field-textarea {
  resize: none;
  line-height: 1.6;
}

.star-picker {
  display: flex;
  gap: 6px;
}
.star-btn {
  padding: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-text {
  font-size: 12.5px;
  color: var(--color-error);
  letter-spacing: -0.2px;
}

.sheet-footer {
  padding: 12px 20px calc(16px + var(--safe-bottom));
  border-top: 1px solid var(--color-line-light);
  flex-shrink: 0;
}
.submit-btn {
  width: 100%;
  padding: 14px 0;
  background: var(--color-peach);
  color: white;
  font-size: 15px;
  font-weight: 700;
  border-radius: var(--radius-md);
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.submit-btn:disabled { opacity: 0.6; }

/* Spinner */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
.spinner.small {
  width: 14px;
  height: 14px;
  border: 2px solid var(--color-line);
  border-top-color: var(--color-error);
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Toast */
.toast {
  position: fixed;
  bottom: calc(var(--bottom-nav-height) + 16px + var(--safe-bottom));
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.82);
  color: #fff;
  font-size: 13px;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  z-index: 90;
  max-width: 80%;
  text-align: center;
}
</style>
