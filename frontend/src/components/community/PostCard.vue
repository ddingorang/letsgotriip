# Created: 2026-06-16 13:25:41
<template>
  <article class="post-card" @click="$emit('click')">
    <div class="post-image">
      <div class="img-area">
        <div v-if="!imgFailed" class="img-wrapper">
          <img :src="displayImage" :alt="post.title" @error="imgFailed = true" />
        </div>
        <div v-else class="img-placeholder">
          <span class="img-caption">{{ post.location }}</span>
        </div>
        <div class="img-gradient" />
        <span class="category-badge">{{ post.categoryLabel ?? post.category }}</span>
        <h2 class="post-title">{{ post.title }}</h2>
      </div>
    </div>

    <div class="post-body">
      <div class="author-row">
        <div class="author-info">
          <div class="avatar">
            <img v-if="post.author?.avatarUrl" :src="post.author.avatarUrl" :alt="post.author.nickname" />
            <div v-else class="avatar-placeholder" />
          </div>
          <div class="author-meta">
            <span class="author-name">{{ post.author?.nickname }}</span>
            <span class="author-sub">{{ post.location }} · {{ timeAgo(post.createdAt) }}</span>
          </div>
        </div>
        <button class="follow-btn" @click.stop>팔로우</button>
      </div>

      <p v-if="post.content" class="post-excerpt">{{ post.content.slice(0, 80) }}{{ post.content.length > 80 ? '...' : '' }}</p>

      <div class="tags-row">
        <span v-for="tag in post.tags" :key="tag" class="tag">{{ tag }}</span>
      </div>

      <div class="stats-row">
        <div class="stat-group">
          <button class="stat-btn" @click.stop="$emit('like', post.id)">
            <svg width="18" height="18" viewBox="0 0 24 24" :fill="post.likedByMe ? 'var(--color-peach)' : 'none'" :stroke="post.likedByMe ? 'none' : 'var(--color-peach)'" stroke-width="1.8">
              <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z" />
            </svg>
            <span class="stat-num peach">{{ post.likeCount }}</span>
          </button>
          <button class="stat-btn" @click.stop>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
            </svg>
            <span class="stat-num">{{ post.commentCount }}</span>
          </button>
        </div>
        <button class="bookmark-icon" @click.stop="$emit('bookmark', post.id)">
          <svg width="18" height="18" viewBox="0 0 24 24" :fill="post.bookmarked ? 'var(--color-ink)' : 'none'" stroke="var(--color-ink-muted)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" />
          </svg>
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  post: { type: Object, required: true },
})

defineEmits(['click', 'like', 'bookmark'])

// 게시물 사진 — 업로드 이미지가 없으면 id 기반 결정적 여행 사진(picsum)을 채움
const imgFailed = ref(false)
const displayImage = computed(() => {
  const u = props.post.imageUrl
  if (u) return u
  const seed = props.post.id ?? props.post.title ?? 'triip'
  return `https://picsum.photos/seed/triip-${seed}/640/440`
})

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}분 전`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}시간 전`
  const days = Math.floor(hours / 24)
  return `${days}일 전`
}
</script>

<style scoped>
.post-card {
  background: var(--color-white);
  cursor: pointer;
}

.post-image {
  position: relative;
  overflow: hidden;
}

.img-area {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.img-wrapper {
  position: absolute;
  inset: 0;
}

.img-wrapper img,
.img-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.img-placeholder {
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
  display: flex;
  align-items: flex-end;
  padding: 12px 16px;
}

.img-caption {
  font-family: var(--font-mono);
  font-size: 10px;
  color: #a99f93;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(1px);
  padding: 3px 7px;
  border-radius: 5px;
}

.img-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.34) 0%, rgba(0, 0, 0, 0) 26%, rgba(0, 0, 0, 0) 55%, rgba(0, 0, 0, 0.5) 100%);
}

.category-badge {
  position: absolute;
  bottom: 42px;
  left: 16px;
  background: var(--color-peach);
  color: white;
  font-size: 11.5px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  backdrop-filter: blur(3px);
}

.post-title {
  position: absolute;
  bottom: 12px;
  left: 16px;
  right: 16px;
  font-size: 20px;
  font-weight: 800;
  color: white;
  letter-spacing: -0.7px;
  line-height: 1.15;
}

.post-body {
  padding: 16px 20px;
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.author-info {
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

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.post-excerpt {
  font-size: 13.4px;
  color: var(--color-dark-text);
  line-height: 2.02;
  letter-spacing: -0.2px;
  margin-bottom: 10px;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.tag {
  font-size: 13px;
  color: var(--color-peach-pressed);
  letter-spacing: -0.2px;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-top: 1px solid var(--color-line-light);
  border-bottom: 1px solid var(--color-line-light);
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

.bookmark-icon {
  display: flex;
  align-items: center;
}
</style>
