# Created: 2026-06-16 13:27:52
<template>
  <div class="page">
    <div class="scroll-area" ref="scrollEl">
      <div class="hero">
        <div class="hero-img">
          <img v-if="post?.imageUrl" :src="post.imageUrl" :alt="post?.title" />
          <div v-else class="hero-placeholder">
            <span class="hero-caption">{{ post?.location }}</span>
          </div>
          <div class="hero-gradient" />
        </div>

        <div class="hero-top-bar">
          <button class="ghost-btn" @click="$router.back()">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5M12 5l-7 7 7 7" />
            </svg>
          </button>
          <div class="hero-actions">
            <button class="ghost-btn" @click="sharePost">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="18" cy="5" r="3" />
                <circle cx="6" cy="12" r="3" />
                <circle cx="18" cy="19" r="3" />
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
              </svg>
            </button>
            <button class="ghost-btn" @click="menuOpen = !menuOpen">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round">
                <circle cx="12" cy="5" r="1" fill="white" />
                <circle cx="12" cy="12" r="1" fill="white" />
                <circle cx="12" cy="19" r="1" fill="white" />
              </svg>
            </button>
          </div>

          <Transition name="fade">
            <div v-if="menuOpen" class="dropdown" ref="dropdownRef">
              <button class="dropdown-item" @click="editPost">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                수정하기
              </button>
              <div class="dropdown-divider" />
              <button class="dropdown-item danger" @click="deletePost">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
                  <path d="M10 11v6M14 11v6M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2" />
                </svg>
                삭제하기
              </button>
            </div>
          </Transition>
        </div>

        <div class="hero-bottom">
          <span class="category-badge">{{ post?.categoryLabel ?? post?.category }}</span>
          <h1 class="hero-title">{{ post?.title }}</h1>
        </div>
      </div>

      <div class="content-section">
        <div class="author-row">
          <div class="author-left">
            <div class="avatar">
              <img v-if="post?.author?.avatarUrl" :src="post.author.avatarUrl" :alt="post.author.nickname" />
              <div v-else class="avatar-placeholder" />
            </div>
            <div class="author-meta">
              <span class="author-name">{{ post?.author?.nickname }}</span>
              <span class="author-sub">{{ post?.location }} · {{ timeAgo(post?.createdAt) }}</span>
            </div>
          </div>
          <button class="follow-btn">팔로우</button>
        </div>

        <p class="post-body">{{ post?.content }}</p>

        <div class="tags-row">
          <span v-for="tag in post?.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>

        <div class="stats-bar">
          <div class="stat-group">
            <button class="stat-btn" @click="likePost">
              <svg width="22" height="22" viewBox="0 0 24 24" :fill="liked ? 'var(--color-peach)' : 'none'" :stroke="liked ? 'var(--color-peach)' : 'var(--color-ink-muted)'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
              </svg>
              <span class="stat-num" :class="{ peach: liked }">{{ likeCount }}</span>
            </button>
            <button class="stat-btn" @click="focusComment">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
              </svg>
              <span class="stat-num">{{ post?.commentCount }}</span>
            </button>
          </div>
          <button class="stat-btn" @click="bookmarkPost">
            <svg width="22" height="22" viewBox="0 0 24 24" :fill="bookmarked ? 'var(--color-ink)' : 'none'" :stroke="bookmarked ? 'var(--color-ink)' : 'var(--color-ink-muted)'" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" />
            </svg>
          </button>
        </div>
      </div>

      <div class="comments-section">
        <h2 class="comments-title">댓글 {{ comments.length }}</h2>

        <div v-for="(comment, idx) in comments" :key="comment.id" class="comment-item" :class="{ bordered: idx > 0 }">
          <div class="comment-avatar">
            <div class="avatar-placeholder sm" />
          </div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-author">{{ comment.author?.nickname }}</span>
              <span class="comment-time">{{ timeAgo(comment.createdAt) }}</span>
            </div>
            <p class="comment-text">{{ comment.content }}</p>
            <div class="comment-actions">
              <button class="comment-action">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
                </svg>
                {{ comment.likeCount }}
              </button>
              <button class="comment-action">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <polyline points="9 14 4 9 9 4" />
                  <path d="M20 20v-7a4 4 0 00-4-4H4" />
                </svg>
                답글
              </button>
              <button v-if="isMyComment(comment)" class="comment-action delete-action" @click="deleteComment(comment.id)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
                </svg>
                삭제
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-spacer" />
    </div>

    <div class="comment-input-bar">
      <template v-if="authStore.isAuthenticated">
        <div class="input-wrap">
          <input ref="commentInputRef" v-model="newComment" class="comment-input" placeholder="댓글을 입력하세요" @keydown.enter.prevent="(e) => !e.isComposing && submitComment()" />
        </div>
        <button class="send-btn" :disabled="!newComment.trim() || submitting" @click="submitComment">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="22" y1="2" x2="11" y2="13" />
            <polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
        </button>
      </template>
      <button v-else class="login-prompt" @click="$router.push({ path: '/login', query: { redirect: $route.fullPath } })">
        댓글을 작성하려면 로그인하세요
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePostsStore } from '@/stores/posts.js'
import { useAuthStore } from '@/stores/auth.js'

const route = useRoute()
const router = useRouter()
const postsStore = usePostsStore()
const authStore = useAuthStore()

function isMyComment(comment) {
  const myId = authStore.user?.userId
  return myId != null && Number(comment.authorId) === Number(myId)
}

async function deleteComment(commentId) {
  if (!confirm('댓글을 삭제하시겠어요?')) return
  await postsStore.deleteComment(route.params.id, commentId)
}

const post = computed(() => postsStore.currentPost)
const comments = computed(() => postsStore.comments)

const liked = ref(false)
const bookmarked = ref(false)
const menuOpen = ref(false)
const newComment = ref('')
const submitting = ref(false)
const commentInputRef = ref(null)
const dropdownRef = ref(null)

watch(post, (p) => { if (p) liked.value = !!p.likedByMe }, { immediate: true })

const likeCount = computed(() => post.value?.likeCount ?? 0)

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}분 전`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}시간 전`
  return `${Math.floor(hours / 24)}일 전`
}

async function likePost() {
  if (!post.value?.id) return
  liked.value = await postsStore.likePost(post.value.id)
}

function bookmarkPost() {
  bookmarked.value = !bookmarked.value
}

function sharePost() {
  if (navigator.share) {
    navigator.share({ title: post.value?.title, url: window.location.href })
  }
}

function focusComment() {
  commentInputRef.value?.focus()
}

async function submitComment() {
  if (!newComment.value.trim() || submitting.value) return
  submitting.value = true
  try {
    await postsStore.addComment(route.params.id, newComment.value.trim())
    newComment.value = ''
  } finally {
    submitting.value = false
  }
}

function editPost() {
  menuOpen.value = false
  router.push(`/community/write?id=${route.params.id}`)
}

async function deletePost() {
  menuOpen.value = false
  if (!confirm('게시글을 삭제하시겠어요?')) return
  try {
    await postsStore.deletePost(route.params.id)
    router.push('/community')
  } catch {
    alert('삭제에 실패했어요. 다시 시도해주세요.')
  }
}

function onOutsideClick(e) {
  if (dropdownRef.value && !dropdownRef.value.contains(e.target)) {
    menuOpen.value = false
  }
}

onMounted(async () => {
  await Promise.all([postsStore.fetchPost(route.params.id), postsStore.fetchComments(route.params.id)])
  document.addEventListener('click', onOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onOutsideClick)
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

.scroll-area {
  flex: 1;
  overflow-y: auto;
}

.hero {
  position: relative;
  height: 340px;
}

.hero-img {
  width: 100%;
  height: 100%;
}

.hero-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-caption {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: #a99f93;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(1px);
  padding: 4px 8px;
  border-radius: 6px;
  letter-spacing: 0.2px;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.34) 0%, rgba(0, 0, 0, 0) 26%, rgba(0, 0, 0, 0) 55%, rgba(0, 0, 0, 0.5) 100%);
}

.hero-top-bar {
  position: absolute;
  top: 60px;
  left: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
}

.ghost-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: var(--color-white);
  border-radius: 14px;
  box-shadow: var(--shadow-dropdown);
  overflow: hidden;
  min-width: 148px;
  z-index: 100;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 16px;
  font-size: 14px;
  color: var(--color-ink);
  width: 100%;
  letter-spacing: -0.2px;
}

.dropdown-item.danger {
  color: var(--color-error);
}

.dropdown-divider {
  height: 1px;
  background: var(--color-line-light);
}

.hero-bottom {
  position: absolute;
  bottom: 16px;
  left: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-badge {
  display: inline-block;
  background: var(--color-peach);
  color: white;
  font-size: 11.5px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  backdrop-filter: blur(3px);
  align-self: flex-start;
}

.hero-title {
  font-size: 24px;
  font-weight: 800;
  color: white;
  letter-spacing: -0.7px;
  line-height: 1.15;
}

.content-section {
  padding: 16px 20px 0;
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.author-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.author-sub {
  font-size: 12px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.follow-btn {
  background: var(--color-peach-light);
  color: var(--color-peach-pressed);
  font-size: 13px;
  font-weight: 500;
  padding: 7px 18px;
  border-radius: 13px;
  letter-spacing: -0.2px;
}

.post-body {
  font-size: 13.4px;
  color: var(--color-dark-text);
  line-height: 2.02;
  letter-spacing: -0.2px;
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 2px;
}

.tag {
  font-size: 13px;
  color: var(--color-peach-pressed);
  letter-spacing: -0.2px;
}

.stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 0 17px;
  border-top: 1px solid var(--color-line-light);
  border-bottom: 1px solid var(--color-line-light);
  margin-top: 12px;
}

.stat-group {
  display: flex;
  align-items: center;
  gap: 22px;
}

.stat-btn {
  display: flex;
  align-items: center;
  gap: 7px;
}

.stat-num {
  font-size: 14px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.stat-num.peach {
  color: var(--color-peach);
}

.comments-section {
  padding: 4px 20px 0;
}

.comments-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.35px;
  margin-bottom: 4px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 17px 0 13px;
}

.comment-item.bordered {
  border-top: 1px solid var(--color-line-light);
}

.comment-avatar {
  flex-shrink: 0;
}

.avatar-placeholder.sm {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
}

.comment-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-author {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-ink);
  letter-spacing: -0.2px;
}

.comment-time {
  font-size: 12px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.comment-text {
  font-size: 12.1px;
  color: #333;
  line-height: 1.73;
  letter-spacing: -0.2px;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 3px;
}

.comment-action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.delete-action {
  color: var(--color-error, #e53e3e);
}

.bottom-spacer {
  height: 16px;
}

.comment-input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 16px 24px;
  background: var(--color-white);
  border-top: 1px solid var(--color-line-light);
  flex-shrink: 0;
}

.input-wrap {
  flex: 1;
  background: var(--color-surface);
  border-radius: var(--radius-full);
  padding: 12px 18px;
  display: flex;
  align-items: center;
}

.comment-input {
  flex: 1;
  font-size: 14px;
  color: var(--color-ink);
  background: transparent;
  letter-spacing: -0.2px;
}

.comment-input::placeholder {
  color: var(--color-ink-muted);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 22px;
  background: var(--color-peach);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s;
}

.send-btn:disabled {
  background: var(--color-line);
}

.login-prompt {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: var(--color-ink-muted);
  letter-spacing: -0.2px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
