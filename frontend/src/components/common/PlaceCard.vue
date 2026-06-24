# Created: 2026-06-16 13:25:15
<template>
  <div class="place-card" @click="$emit('click')">
    <div class="card-image">
      <div v-if="rank" class="rank-badge">인기 {{ rank }}위</div>
      <button class="bookmark-btn" @click.stop="$emit('bookmark')">
        <svg width="18" height="18" viewBox="0 0 24 24" :fill="bookmarked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" />
        </svg>
      </button>
      <div class="img-wrapper">
        <img :src="thumb" :alt="place.name" @error="onImgError" />
      </div>
      <div v-if="place.likeCount != null" class="like-badge">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" stroke="none">
          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
        </svg>
        <span>{{ likeLabel }}</span>
      </div>
    </div>
    <div class="card-body">
      <div class="card-top">
        <div class="place-name">{{ place.name }}</div>
        <div v-if="place.rating != null" class="place-rating">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="var(--color-gold)" stroke="none">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
          <span>{{ place.rating }}</span>
          <span v-if="place.reviewCount != null" class="review-count">({{ place.reviewCount?.toLocaleString() }})</span>
        </div>
      </div>
      <div class="place-address">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
          <circle cx="12" cy="10" r="3" />
        </svg>
        {{ place.address }}
        <span v-for="tag in place.tags" :key="tag" class="place-tag">{{ tag }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  place: { type: Object, required: true },
  rank: { type: Number, default: null },
  bookmarked: { type: Boolean, default: false },
})

defineEmits(['click', 'bookmark'])

// 썸네일 — 업로드/관광 이미지가 없거나 로딩 실패 시 로컬 기본 썸네일로 채움(외부 더미 미사용)
const imgFailed = ref(false)
const placeholder = computed(() => '/images/placeholder-thumb.png')
const thumb = computed(() => (!imgFailed.value && props.place.imageUrl) ? props.place.imageUrl : placeholder.value)
function onImgError() {
  imgFailed.value = true
}

// 좋아요 수 — 1,000 이상은 1.2k 형태로 축약(배지 폭 고정).
const likeLabel = computed(() => {
  const n = props.place.likeCount
  if (n == null) return ''
  return n >= 1000 ? (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : String(n)
})
</script>

<style scoped>
.place-card {
  background: var(--color-white);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  cursor: pointer;
  flex-shrink: 0;
  width: 160px;
}

.card-image {
  position: relative;
  height: 120px;
}

.img-wrapper img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #efe6e4, #e7e0d8);
  display: flex;
  align-items: flex-end;
  padding: 8px;
}

.place-caption {
  font-family: var(--font-mono);
  font-size: 10px;
  color: #a99f93;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(1px);
  padding: 3px 6px;
  border-radius: 4px;
}

.rank-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--color-peach);
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  z-index: 1;
}

.like-badge {
  position: absolute;
  bottom: 8px;
  left: 8px;
  display: flex;
  align-items: center;
  gap: 3px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 7px 3px 6px;
  border-radius: var(--radius-full);
  z-index: 1;
  backdrop-filter: blur(2px);
}

.like-badge svg {
  color: var(--color-peach);
}

.bookmark-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-ink-muted);
  z-index: 1;
}

.card-body {
  padding: 10px 12px 12px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 4px;
  margin-bottom: 4px;
}

.place-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-ink);
  letter-spacing: -0.3px;
}

.place-rating {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-ink);
  flex-shrink: 0;
}

.review-count {
  font-weight: 400;
  color: var(--color-ink-muted);
  font-size: 11px;
}

.place-address {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: var(--color-ink-muted);
  flex-wrap: wrap;
}

.place-tag {
  color: var(--color-peach-pressed);
  font-size: 11px;
}
</style>
