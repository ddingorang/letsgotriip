# Created: 2026-06-16 14:06:20
<template>
  <div class="page">
    <header class="nav-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 5l-7 7 7 7" />
        </svg>
      </button>
      <span class="nav-title">동행 모집</span>
      <button class="submit-top-btn" :disabled="!isValid" @click="submit">등록</button>
    </header>

    <div class="scroll-content">
      <div class="form-body">
        <!-- 제목 -->
        <div class="field">
          <label class="field-label">제목 <span class="req">*</span></label>
          <input v-model="form.title" class="field-input" placeholder="예) 제주 3박 4일 동행 구해요" />
        </div>

        <!-- 여행 지역 + 날짜 -->
        <div class="row-two">
          <div class="field">
            <label class="field-label">여행 지역 <span class="req">*</span></label>
            <div class="input-icon-wrap">
              <input v-model="form.location" class="field-input" placeholder="제주" />
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" /></svg>
            </div>
          </div>
          <div class="field">
            <label class="field-label">날짜 <span class="req">*</span></label>
            <div class="input-icon-wrap">
              <input v-model="form.dateRange" class="field-input" placeholder="6.12-6.15" />
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="2" stroke-linecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
            </div>
          </div>
        </div>

        <!-- 최대 인원 -->
        <div class="field">
          <label class="field-label">최대 인원 <span class="req">*</span></label>
          <div class="chips-row">
            <button
              v-for="n in personOptions"
              :key="n"
              :class="['chip-btn', { active: form.maxCount === n }]"
              @click="form.maxCount = n"
            >
              {{ n }}
            </button>
          </div>
        </div>

        <!-- 설명 -->
        <div class="field">
          <label class="field-label">설명</label>
          <textarea
            v-model="form.description"
            class="field-textarea"
            placeholder="여행 스타일, 원하는 동행, 일정 등을 자유롭게 적어주세요"
            rows="5"
          />
        </div>

        <!-- 태그 -->
        <div class="field">
          <label class="field-label">태그</label>
          <div class="chips-row">
            <button
              v-for="tag in availableTags"
              :key="tag"
              :class="['chip-btn tag-chip', { active: form.tags.includes(tag) }]"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <!-- Info banner -->
        <div class="info-banner">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-secondary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
          <p class="info-text">모집글을 등록하면 그룹 채팅방이 자동 생성돼요. 승인한 멤버만 입장할 수 있어요.</p>
        </div>

        <div style="height: 32px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCompanionStore } from '@/stores/companion.js'

const router = useRouter()
const companionStore = useCompanionStore()

const personOptions = ['2명', '3명', '4명', '5명', '6명+']
const availableTags = ['#20대', '#30대', '#뚜벅이', '#드라이브', '#맛집투어', '#혼행환영', '#가족여행']

const form = ref({
  title: '',
  location: '',
  dateRange: '',
  maxCount: null,
  description: '',
  tags: [],
})

const isValid = computed(() => form.value.title && form.value.location && form.value.dateRange && form.value.maxCount)

function toggleTag(tag) {
  const idx = form.value.tags.indexOf(tag)
  if (idx === -1) form.value.tags.push(tag)
  else form.value.tags.splice(idx, 1)
}

function submit() {
  if (!isValid.value) return
  companionStore.companions.unshift({
    id: Date.now(),
    title: form.value.title,
    location: form.value.location,
    dateRange: form.value.dateRange,
    status: '모집중',
    currentCount: 1,
    maxCount: parseInt(form.value.maxCount) || 4,
    author: { nickname: '나', tripCount: 1 },
    period: '-',
    estimatedCost: '-',
    tags: form.value.tags,
    intro: form.value.description,
    isOwner: true,
    pendingCount: 0,
    approvedCount: 0,
  })
  router.back()
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-white);
}
.nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
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
.nav-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}
.submit-top-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: white;
  font-size: 14px;
  font-weight: 700;
  transition: opacity 0.15s;
}
.submit-top-btn:disabled { opacity: 0.4; }

.scroll-content { flex: 1; overflow-y: auto; }
.form-body { padding: 20px 16px; display: flex; flex-direction: column; gap: 24px; }

.field { display: flex; flex-direction: column; gap: 8px; }
.field-label { font-size: 13.5px; font-weight: 600; color: var(--color-ink); }
.req { color: var(--color-peach); }

.row-two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.input-icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.input-icon-wrap .field-input { padding-right: 36px; }
.input-icon-wrap svg {
  position: absolute;
  right: 12px;
  pointer-events: none;
}

.field-input {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: var(--color-peach); }
.field-input::placeholder { color: var(--color-ink-muted); }

.field-textarea {
  width: 100%;
  padding: 13px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-ink);
  resize: none;
  line-height: 1.6;
  border: 1.5px solid transparent;
  transition: border-color 0.15s;
}
.field-textarea:focus { border-color: var(--color-peach); }
.field-textarea::placeholder { color: var(--color-ink-muted); }

.chips-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  border: 1.5px solid var(--color-line);
  background: var(--color-white);
  transition: all 0.15s;
}
.chip-btn.active {
  background: var(--color-peach);
  color: white;
  border-color: var(--color-peach);
}
.tag-chip { font-size: 13px; }

.info-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px;
  background: #f0f7f0;
  border-radius: var(--radius-md);
  border: 1px solid #c8e0c8;
}
.info-text { font-size: 13px; color: var(--color-ink-secondary); line-height: 1.55; letter-spacing: -0.2px; }
</style>
