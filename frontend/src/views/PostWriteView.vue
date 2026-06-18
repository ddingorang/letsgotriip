# Created: 2026-06-16 13:31:11
<template>
  <div class="page">
    <header class="write-header">
      <button class="icon-btn" @click="$router.back()">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
      <h1 class="header-title">게시글 작성</h1>
      <button class="submit-btn" :disabled="!isValid" @click="submitPost">등록</button>
    </header>

    <div class="scroll-content">
      <div class="image-section">
        <div class="image-upload" @click="triggerImageUpload">
          <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="onImageSelect" />
          <div v-if="!previewUrl" class="upload-placeholder">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--color-line)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            <span class="upload-text">사진 추가</span>
          </div>
          <img v-else :src="previewUrl" class="preview-img" />
        </div>
      </div>

      <div class="form-section">
        <div class="category-select">
          <button v-for="cat in categories" :key="cat" class="cat-btn" :class="{ active: form.category === cat }" @click="form.category = cat">
            {{ cat }}
          </button>
        </div>

        <input v-model="form.title" class="title-input" placeholder="제목을 입력하세요" maxlength="50" />
        <div class="char-count">{{ form.title.length }}/50</div>

        <div class="location-row">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-peach)" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
            <circle cx="12" cy="10" r="3" />
          </svg>
          <input v-model="form.location" class="location-input" placeholder="장소를 입력하세요" />
        </div>

        <textarea v-model="form.content" class="content-input" placeholder="여행 이야기를 들려주세요. 다른 여행자들에게 도움이 되는 정보를 공유해보세요." rows="8" />

        <div class="tags-input-section">
          <div class="tags-row">
            <span v-for="tag in form.tags" :key="tag" class="tag-chip">
              {{ tag }}
              <button class="remove-tag" @click="removeTag(tag)">×</button>
            </span>
          </div>
          <div class="tag-input-row">
            <span class="tag-prefix">#</span>
            <input v-model="tagInput" class="tag-input" placeholder="태그 추가 (엔터로 입력)" @keydown.enter.prevent="addTag" />
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '@/api/index.js'

const router = useRouter()
const fileInput = ref(null)
const previewUrl = ref(null)
const tagInput = ref('')

const form = ref({
  category: '후기',
  title: '',
  content: '',
  location: '',
  tags: [],
  imageUrl: null,
})

const categories = ['후기', '꿀팁', '동행', '질문']
// Korean label → BE PostCategory enum (REVIEW/QUESTION/TIP/RESTAURANT/COMPANION)
const categoryEnum = {
  후기: 'REVIEW',
  꿀팁: 'TIP',
  동행: 'COMPANION',
  질문: 'QUESTION',
  맛집: 'RESTAURANT',
}
const isValid = computed(() => form.value.title.trim() && form.value.content.trim())

function triggerImageUpload() {
  fileInput.value?.click()
}

function onImageSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    previewUrl.value = ev.target.result
    form.value.imageUrl = ev.target.result
  }
  reader.readAsDataURL(file)
}

function addTag() {
  const tag = tagInput.value.trim().replace(/^#/, '')
  if (tag && !form.value.tags.includes(`#${tag}`) && form.value.tags.length < 5) {
    form.value.tags.push(`#${tag}`)
    tagInput.value = ''
  }
}

function removeTag(tag) {
  form.value.tags = form.value.tags.filter((t) => t !== tag)
}

async function submitPost() {
  if (!isValid.value) return
  // BE PostCreateRequest expects { title, content, category(enum), imageUrls(list) }
  const payload = {
    title: form.value.title,
    content: form.value.content,
    category: categoryEnum[form.value.category] ?? 'REVIEW',
    imageUrls: form.value.imageUrl ? [form.value.imageUrl] : [],
  }
  try {
    await communityApi.createPost(payload)
    router.push('/community')
  } catch {
    router.push('/community')
  }
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

.write-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 52px 16px 12px;
  border-bottom: 1px solid var(--color-line-light);
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink);
}

.submit-btn {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-peach);
  padding: 8px 4px;
  letter-spacing: -0.2px;
}

.submit-btn:disabled {
  color: var(--color-line);
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
}

.image-section {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-line-light);
}

.image-upload {
  width: 120px;
  height: 120px;
  border-radius: var(--radius-lg);
  border: 2px dashed var(--color-line);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.15s;
}

.image-upload:hover {
  border-color: var(--color-peach);
}

.hidden-input {
  display: none;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-text {
  font-size: 12.5px;
  color: var(--color-ink-muted);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-section {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.category-select {
  display: flex;
  gap: 8px;
}

.cat-btn {
  padding: 7px 16px;
  border-radius: var(--radius-full);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--color-ink-muted);
  background: var(--color-surface);
  letter-spacing: -0.2px;
  transition: all 0.15s;
}

.cat-btn.active {
  background: var(--color-peach);
  color: white;
}

.title-input {
  width: 100%;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.4px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-line-light);
}

.title-input::placeholder {
  color: var(--color-line);
  font-weight: 400;
}

.char-count {
  font-size: 12px;
  color: var(--color-ink-muted);
  text-align: right;
  margin-top: -8px;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
}

.location-input {
  flex: 1;
  font-size: 14px;
  color: var(--color-ink);
  background: transparent;
  letter-spacing: -0.2px;
}

.location-input::placeholder {
  color: var(--color-ink-muted);
}

.content-input {
  width: 100%;
  font-size: 14px;
  color: var(--color-ink);
  line-height: 1.7;
  letter-spacing: -0.2px;
  resize: none;
  background: transparent;
}

.content-input::placeholder {
  color: var(--color-ink-muted);
}

.tags-input-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 13px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

.remove-tag {
  font-size: 16px;
  line-height: 1;
  color: var(--color-peach-pressed);
}

.tag-input-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tag-prefix {
  font-size: 14px;
  color: var(--color-peach-pressed);
  font-weight: 600;
}

.tag-input {
  flex: 1;
  font-size: 14px;
  color: var(--color-ink);
  background: transparent;
  letter-spacing: -0.2px;
}

.tag-input::placeholder {
  color: var(--color-ink-muted);
}

.bottom-spacer {
  height: 40px;
}
</style>
