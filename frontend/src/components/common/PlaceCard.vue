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
      <div v-if="place.imageUrl" class="img-wrapper">
        <img :src="place.imageUrl" :alt="place.name" />
      </div>
      <div v-else class="img-placeholder">
        <span class="place-caption">{{ place.address }}</span>
      </div>
    </div>
    <div class="card-body">
      <div class="card-top">
        <div class="place-name">{{ place.name }}</div>
        <div class="place-rating">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="var(--color-gold)" stroke="none">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
          </svg>
          <span>{{ place.rating }}</span>
          <span class="review-count">({{ place.reviewCount?.toLocaleString() }})</span>
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
defineProps({
  place: { type: Object, required: true },
  rank: { type: Number, default: null },
  bookmarked: { type: Boolean, default: false },
})

defineEmits(['click', 'bookmark'])
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
