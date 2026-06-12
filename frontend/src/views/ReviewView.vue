<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import AppNavBar from '../components/layout/AppNavBar.vue';

const router = useRouter();

const ratingBars = [
  { star: '5★', width: '88%', count: '1,091' },
  { star: '4★', width: '8%',  count: '99'    },
  { star: '3★', width: '2%',  count: '25'    },
  { star: '2★', width: '1%',  count: '12'    },
  { star: '1★', width: '1%',  count: '13'    }
];

const tags = [
  { label: '청결 최고 (324)', hot: true  },
  { label: '위치 편리 (298)', hot: true  },
  { label: '조식 맛있음 (201)', hot: false },
  { label: '친절한 직원 (187)', hot: false },
  { label: '뷰 훌륭 (155)', hot: false   }
];

const reviews = ref([
  {
    id: 1,
    initial: '김',
    name: '김여행',
    meta: '2026.06.10 · 2박',
    stars: '★★★★★',
    starsColor: '',
    avatarStyle: '',
    text: '뷰가 정말 대박이에요! 객실 창문 바로 앞에 해운대 해수욕장이 펼쳐져 있고, 일출이 너무 아름다웠습니다. 조식도 종류가 다양하고 맛있었어요. 다음에 부산 방문할 때도 꼭 여기서 숙박하고 싶습니다.',
    photos: [1, 2, 3],
    helpful: 42,
    helpfulActive: true
  },
  {
    id: 2,
    initial: '이',
    name: '이관통',
    meta: '2026.05.28 · 3박',
    stars: '★★★★☆',
    starsColor: 'var(--color-warning)',
    avatarStyle: 'background:var(--color-neutral-100);color:var(--text-secondary)',
    text: '전반적으로 만족스러운 숙박이었습니다. 다만 성수기라 그런지 체크인이 조금 늦었고, 주차 공간이 협소했던 점은 아쉬웠어요. 수영장과 피트니스 센터는 정말 훌륭했습니다.',
    photos: [],
    helpful: 18,
    helpfulActive: false
  }
]);

function toggleHelpful(review) {
  review.helpfulActive = !review.helpfulActive;
  review.helpful += review.helpfulActive ? 1 : -1;
}
</script>

<template>
  <div class="review-view">
    <AppNavBar title="리뷰 1,240개">
      <template #action>
        <span class="nav-write">리뷰 쓰기</span>
      </template>
    </AppNavBar>

    <div class="summary">
      <div class="score-big">
        <div class="score-num">4.9</div>
        <div class="stars-big">★★★★★</div>
        <div class="score-count">1,240개</div>
      </div>
      <div class="score-bars">
        <div v-for="bar in ratingBars" :key="bar.star" class="bar-row">
          <span class="bar-star">{{ bar.star }}</span>
          <div class="bar-track"><div class="bar-fill" :style="{ width: bar.width }"></div></div>
          <span class="bar-count">{{ bar.count }}</span>
        </div>
      </div>
    </div>

    <div class="tag-section">
      <div v-for="tag in tags" :key="tag.label" class="review-tag" :class="{ hot: tag.hot }">
        {{ tag.label }}
      </div>
    </div>

    <div class="reviews">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-header">
          <div class="reviewer-avatar" :style="review.avatarStyle">{{ review.initial }}</div>
          <div class="reviewer-info">
            <div class="reviewer-name">{{ review.name }}</div>
            <div class="reviewer-meta">{{ review.meta }}</div>
          </div>
          <div class="review-stars" :style="review.starsColor ? `color:${review.starsColor}` : ''">{{ review.stars }}</div>
        </div>
        <div class="review-text">{{ review.text }}</div>
        <div v-if="review.photos.length" class="review-photos">
          <div v-for="p in review.photos" :key="p" class="review-photo">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          </div>
        </div>
        <div class="review-helpful">
          <div class="helpful-btn" :class="{ active: review.helpfulActive }" @click="toggleHelpful(review)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/><path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/></svg>
            도움됨 {{ review.helpful }}
          </div>
        </div>
      </div>
    </div>

    <div class="write-fab" role="button" @click="router.push('/review/write')">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
      리뷰 작성
    </div>
  </div>
</template>

<style scoped>
.review-view { background: var(--surface-subtle); min-height: 100%; }

.nav-write { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--color-primary-500); cursor: pointer; }

.summary { background: #fff; padding: 20px; display: flex; gap: 20px; border-bottom: 8px solid var(--surface-subtle); }
.score-big { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.score-num { font: var(--weight-extrabold) 52px/1 var(--font-sans); color: var(--text-primary); letter-spacing: -0.04em; }
.stars-big { display: flex; gap: 3px; }
.score-count { font: var(--type-caption); color: var(--text-tertiary); }
.score-bars { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.bar-row { display: flex; align-items: center; gap: 8px; }
.bar-star { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-secondary); width: 22px; text-align: right; flex-shrink: 0; }
.bar-track { flex: 1; height: 6px; background: var(--surface-subtle); border-radius: var(--radius-full); overflow: hidden; }
.bar-fill { height: 100%; background: var(--color-warning); border-radius: var(--radius-full); }
.bar-count { font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); width: 28px; text-align: right; flex-shrink: 0; }

.tag-section { background: #fff; padding: 14px 20px; border-bottom: 8px solid var(--surface-subtle); display: flex; gap: 8px; flex-wrap: wrap; }
.review-tag { display: inline-flex; align-items: center; gap: 5px; padding: 7px 13px; border-radius: var(--radius-full); font: var(--weight-medium) var(--text-sm)/1 var(--font-sans); background: var(--surface-subtle); color: var(--text-secondary); border: 1.5px solid var(--border-default); }
.review-tag.hot { background: var(--color-primary-50); color: var(--color-primary-500); border-color: var(--color-primary-300); }

.reviews { padding: 16px 20px 100px; display: flex; flex-direction: column; gap: 1px; background: var(--surface-subtle); }
.review-card { background: #fff; padding: 18px 20px; }
.review-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.reviewer-avatar { width: 38px; height: 38px; border-radius: var(--radius-full); background: var(--color-primary-100); display: flex; align-items: center; justify-content: center; color: var(--color-primary-600); font: var(--weight-bold) var(--text-sm)/1 var(--font-sans); flex-shrink: 0; }
.reviewer-info { flex: 1; }
.reviewer-name { font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); color: var(--text-primary); }
.reviewer-meta { font: var(--type-caption); color: var(--text-tertiary); margin-top: 3px; }
.review-stars { display: flex; gap: 2px; color: var(--color-warning); font-size: 14px; }
.review-text { font: var(--type-body); color: var(--text-primary); line-height: var(--leading-loose); margin-bottom: 12px; }
.review-photos { display: flex; gap: 6px; margin-bottom: 12px; }
.review-photo { width: 72px; height: 72px; border-radius: var(--radius-sm); background: var(--surface-subtle); display: flex; align-items: center; justify-content: center; color: var(--text-tertiary); }
.review-helpful { display: flex; align-items: center; gap: 6px; }
.helpful-btn { display: flex; align-items: center; gap: 5px; font: var(--weight-medium) var(--text-xs)/1 var(--font-sans); color: var(--text-tertiary); padding: 5px 10px; border: 1px solid var(--border-default); border-radius: var(--radius-full); cursor: pointer; }
.helpful-btn.active { color: var(--color-primary-500); border-color: var(--color-primary-300); background: var(--color-primary-50); }

.write-fab { position: fixed; bottom: 24px; right: 20px; display: flex; align-items: center; gap: 8px; background: var(--color-primary-500); color: #fff; padding: 14px 20px; border-radius: var(--radius-full); font: var(--weight-semibold) var(--text-sm)/1 var(--font-sans); box-shadow: 0 4px 16px rgba(255,98,0,.4); cursor: pointer; z-index: var(--z-raised); }
</style>
