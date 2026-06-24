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
      <h1 class="header-title">{{ editId ? '게시글 수정' : '게시글 작성' }}</h1>
      <button class="submit-btn" :disabled="!isValid" @click="submitPost">{{ editId ? '수정' : '등록' }}</button>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { communityApi } from '@/api/index.js'
import { usePostsStore } from '@/stores/posts.js'
import { useAlert } from '@/composables/useAlert.js'

const $alert = useAlert()

const route = useRoute()
const router = useRouter()
const postsStore = usePostsStore()
const fileInput = ref(null)
const previewUrl = ref(null)
// URL.createObjectURL로 만든 미리보기 blob URL — 교체/언마운트 시 해제(revoke)한다.
const objectUrl = ref(null)
const uploading = ref(false)
const tagInput = ref('')

// edit mode: present when route has ?id=<postId>
const editId = computed(() => route.query.id || null)

const form = ref({
  category: '후기',
  title: '',
  content: '',
  location: '',
  tags: [],
  imageUrl: null,
})

const categories = ['후기', '꿀팁', '맛집', '질문']
// Korean label → BE PostCategory enum (REVIEW/QUESTION/TIP/RESTAURANT)
const categoryEnum = {
  후기: 'REVIEW',
  꿀팁: 'TIP',
  맛집: 'RESTAURANT',
  질문: 'QUESTION',
}
// BE enum → Korean label (for prefilling category selector)
const enumCategory = {
  REVIEW: '후기',
  TIP: '꿀팁',
  RESTAURANT: '맛집',
  QUESTION: '질문',
}

// 업로드 진행 중에는 등록/수정 비활성(미완 이미지로 제출 방지)
const isValid = computed(() => form.value.title.trim() && form.value.content.trim() && !uploading.value)

function triggerImageUpload() {
  fileInput.value?.click()
}

// 이전 미리보기 blob URL 해제
function revokeObjectUrl() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = null
  }
}

async function onImageSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  // 미리보기는 base64가 아니라 로컬 blob URL로(메모리 효율 + 짧은 본문)
  revokeObjectUrl()
  objectUrl.value = URL.createObjectURL(file)
  previewUrl.value = objectUrl.value
  // 실제 파일은 즉시 서버에 업로드해 짧은 경로(imageUrl)만 폼에 저장한다.
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await communityApi.uploadImage(fd)
    form.value.imageUrl = res.data?.imageUrl ?? null
    if (!form.value.imageUrl) throw new Error('이미지 응답에 imageUrl이 없습니다')
  } catch {
    // 업로드 실패를 가짜 성공으로 숨기지 않는다 — 미리보기/폼 이미지를 비우고 알린다.
    form.value.imageUrl = null
    revokeObjectUrl()
    previewUrl.value = null
    $alert.error('이미지 업로드에 실패했어요. 다시 시도해주세요.')
  } finally {
    uploading.value = false
    // 같은 파일 재선택 시에도 change가 발화하도록 input 값 초기화
    if (fileInput.value) fileInput.value.value = ''
  }
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
  // BE PostCreateRequest / PostUpdateRequest: { title, content, category(enum), imageUrls(list) }
  const payload = {
    title: form.value.title,
    content: form.value.content,
    category: categoryEnum[form.value.category] ?? 'REVIEW',
    imageUrls: form.value.imageUrl ? [form.value.imageUrl] : [],
  }
  if (editId.value) {
    try {
      await postsStore.updatePost(editId.value, payload)
      router.push(`/community/${editId.value}`)
    } catch {
      $alert.error('수정에 실패했어요. 다시 시도해주세요.')
    }
  } else {
    try {
      await communityApi.createPost(payload)
      router.push('/community')
    } catch {
      // 등록 실패를 성공처럼 이동시키지 않고 실패로 노출한다.
      $alert.error('등록에 실패했어요. 다시 시도해주세요.')
    }
  }
}

onMounted(async () => {
  if (!editId.value) return
  try {
    await postsStore.fetchPost(editId.value)
    const post = postsStore.currentPost
    if (!post) return
    form.value.title = post.title ?? ''
    form.value.content = post.content ?? ''
    form.value.category = enumCategory[post.category] ?? '후기'
    form.value.location = post.location ?? ''
    form.value.tags = post.tags ?? []
    // prefill image preview with the first existing image URL (not a local blob)
    const existingImage = post.imageUrls?.[0] ?? post.imageUrl ?? null
    if (existingImage) {
      previewUrl.value = existingImage
      form.value.imageUrl = existingImage
    }
  } catch {
    // silently fall through; form stays blank and user can re-enter
  }
})

// 미리보기 blob URL 누수 방지 — 언마운트 시 해제
onBeforeUnmount(() => {
  revokeObjectUrl()
})
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
