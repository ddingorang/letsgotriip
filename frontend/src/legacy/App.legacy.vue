<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch, watchEffect } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import BackdropScene from './components/BackdropScene.vue';
import ExploreWorkspace from './components/ExploreWorkspace.vue';
import PageWorkspace from './components/PageWorkspace.vue';
import RailHeader from './components/RailHeader.vue';
import { routeByPage } from './router/index.js';
import { useTripUiStore } from './stores/tripUi.js';
import { navItems, scenes } from './data.js';

const router = useRouter();
const route = useRoute();
const uiStore = useTripUiStore();
const { currentPage, currentScene, drawerOpen, detailOpen, detailContentOpen, scene } = storeToRefs(uiStore);
const activeLayer = ref(0);
const backdropLayers = ref([
  { id: 0, scene: scenes[0] },
  { id: 1, scene: scenes[0] }
]);
const railRef = ref(null);
const railStyle = ref({});
const lastWheelAt = ref(0);
const touchStart = ref({ x: 0, y: 0 });
let detailTimer = 0;
let compactRailTop = 0;
let railResizeObserver = null;

const heroCopyByPage = {
  home: ['오늘 여행 흐름', '저장한 장소와 이어갈 작업을 한 번에 확인합니다.'],
  explore: ['지역·유형으로 장소 찾기', '장소는 크게, 정보는 조용하게. 필터와 상세를 오른쪽에서 바로 살펴보세요.'],
  plan: ['일정 동선 관리', '비어 있는 시간과 이동 순서를 바로 정리합니다.'],
  recommend: ['취향 기반 추천', '날씨와 취향 태그를 반영한 후보를 비교합니다.'],
  hotplace: ['핫플 제보 검수', '사진과 좌표가 있는 새 장소를 빠르게 살펴봅니다.'],
  community: ['여행 이야기', '공유 일정과 여행 글 흐름을 확인합니다.'],
  mypage: ['내 여행 기록', '저장 장소와 취향 태그에서 다음 일정을 이어갑니다.']
};
const heroCopy = computed(() => {
  return heroCopyByPage[currentPage.value] ?? heroCopyByPage.home;
});

function pageFromRoute() {
  return typeof route.name === 'string' && routeByPage[route.name] ? route.name : 'explore';
}

async function changePage(page) {
  uiStore.setPage(page);
  const targetPath = routeByPage[page] ?? routeByPage.explore;
  if (route.path !== targetPath) {
    await router.push(targetPath);
  }
}

function toggleDetail() {
  window.clearTimeout(detailTimer);
  if (detailOpen.value) {
    uiStore.setDetailContentOpen(false);
    detailTimer = window.setTimeout(() => {
      uiStore.setDetailOpen(false);
    }, 180);
    return;
  }
  uiStore.setDetailOpen(true);
  detailTimer = window.setTimeout(() => {
    uiStore.setDetailContentOpen(true);
  }, 520);
}

function updateRailMetrics() {
  const rail = railRef.value;
  if (!rail) return;
  const viewportHeight = window.innerHeight;
  const maxPanelHeight = viewportHeight - Math.max(36, Math.min(viewportHeight * 0.044, 64));

  if (!detailContentOpen.value) {
    compactRailTop = rail.getBoundingClientRect().top;
    railStyle.value = {};
    return;
  }

  const header = rail.querySelector('.et-rail-head');
  const main = rail.querySelector('.et-rail-main');
  const workspace = rail.querySelector('.workspace-deck');
  const footer = rail.querySelector('.et-rail-foot');
  const compactHeight = Math.min(556, maxPanelHeight);
  const minOpenRatio = currentPage.value === 'explore' ? 0.667 : 0.52;
  const minOpenHeight = Math.min(maxPanelHeight, Math.round(viewportHeight * minOpenRatio));
  const gap = Number.parseFloat(window.getComputedStyle(rail).gap) || 0;
  const headerHeight = header?.getBoundingClientRect().height ?? 0;
  const footerHeight = footer?.getBoundingClientRect().height ?? 0;
  const mainStyle = main ? window.getComputedStyle(main) : null;
  const mainPadding = mainStyle
    ? (Number.parseFloat(mainStyle.paddingTop) || 0) + (Number.parseFloat(mainStyle.paddingBottom) || 0)
    : 0;
  const mainContentHeight = (workspace?.scrollHeight ?? main?.scrollHeight ?? 0) + mainPadding;
  const maxVisibleMainHeight = Math.max(0, maxPanelHeight - headerHeight - footerHeight - gap * 2 - 4);
  const visibleMainHeight = Math.min(mainContentHeight, maxVisibleMainHeight);
  const contentHeight = Math.ceil(headerHeight + visibleMainHeight + footerHeight + gap * 2 + 4);
  const simpleOpenHeight = Math.min(maxPanelHeight, Math.round(viewportHeight * 0.62));
  const openCeiling = currentPage.value === 'explore' ? maxPanelHeight : simpleOpenHeight;
  const targetHeight = Math.min(openCeiling, Math.max(compactHeight, minOpenHeight, contentHeight));
  const compactCenterY = compactRailTop + compactHeight / 2;
  const minTop = Math.max(18, Math.min(window.innerWidth * 0.022, 32));
  const maxTop = Math.max(minTop, viewportHeight - targetHeight);
  const targetTop = Math.min(maxTop, Math.max(minTop, compactCenterY - targetHeight / 2));

  railStyle.value = {
    '--detail-rail-height': `${targetHeight}px`,
    '--detail-rail-top': `${targetTop}px`
  };
}

function scheduleRailMetrics() {
  void nextTick(() => {
    window.requestAnimationFrame(updateRailMetrics);
  });
}

function preloadImage(src) {
  return new Promise((resolve) => {
    const image = new Image();
    image.onload = resolve;
    image.onerror = resolve;
    image.src = src;
    window.setTimeout(resolve, 900);
  });
}

async function applyScene(index) {
  const nextIndex = (index + scenes.length) % scenes.length;
  if (nextIndex === currentScene.value) return;
  uiStore.setScene(nextIndex);
  const nextLayer = activeLayer.value === 0 ? 1 : 0;
  const nextScene = scenes[nextIndex];
  await preloadImage(nextScene.image);
  backdropLayers.value = backdropLayers.value.map((layer) => (layer.id === nextLayer ? { ...layer, scene: nextScene } : layer));
  activeLayer.value = nextLayer;
}

function moveScene(delta) {
  void applyScene(currentScene.value + delta);
}

function canGestureScene(event) {
  if (drawerOpen.value) return false;
  const target = event.target;
  if (!(target instanceof Element)) return false;
  if (target.closest('.et-rail, .drawer, input, textarea, select, button, a')) return false;
  return Boolean(target.closest('.et-stage, .backdrop, .shell'));
}

function onWheel(event) {
  if (!canGestureScene(event)) return;
  const dominantDelta = Math.abs(event.deltaY) >= Math.abs(event.deltaX) ? event.deltaY : event.deltaX;
  if (Math.abs(dominantDelta) < 18) return;
  const now = Date.now();
  if (now - lastWheelAt.value < 420) return;
  lastWheelAt.value = now;
  moveScene(dominantDelta > 0 ? 1 : -1);
}

function onTouchStart(event) {
  if (!canGestureScene(event) || !event.touches[0]) return;
  touchStart.value = { x: event.touches[0].clientX, y: event.touches[0].clientY };
}

function onTouchEnd(event) {
  if (!canGestureScene(event) || !event.changedTouches[0]) return;
  const dx = touchStart.value.x - event.changedTouches[0].clientX;
  const dy = touchStart.value.y - event.changedTouches[0].clientY;
  if (Math.max(Math.abs(dx), Math.abs(dy)) < 54) return;
  moveScene(Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : -1) : (dy > 0 ? 1 : -1));
}

function onKeydown(event) {
  if (drawerOpen.value) return;
  const target = event.target;
  if (target instanceof Element && target.closest('input, textarea, select')) return;
  if (event.key === 'ArrowRight') moveScene(1);
  if (event.key === 'ArrowLeft') moveScene(-1);
  if (event.key === 'Escape') uiStore.setDrawerOpen(false);
}

onMounted(() => {
  void uiStore.loadPlaces();
  void Promise.all(scenes.map((item) => preloadImage(item.image)));
  railResizeObserver = new ResizeObserver(scheduleRailMetrics);
  if (railRef.value) railResizeObserver.observe(railRef.value);
  scheduleRailMetrics();
  window.addEventListener('resize', scheduleRailMetrics);
  window.addEventListener('wheel', onWheel, { passive: true });
  window.addEventListener('touchstart', onTouchStart, { passive: true });
  window.addEventListener('touchend', onTouchEnd, { passive: true });
  window.addEventListener('keydown', onKeydown);
});

onUnmounted(() => {
  window.clearTimeout(detailTimer);
  railResizeObserver?.disconnect();
  window.removeEventListener('resize', scheduleRailMetrics);
  window.removeEventListener('wheel', onWheel);
  window.removeEventListener('touchstart', onTouchStart);
  window.removeEventListener('touchend', onTouchEnd);
  window.removeEventListener('keydown', onKeydown);
});

watch(() => route.name, () => {
  uiStore.setPage(pageFromRoute());
}, { immediate: true });

watch([currentPage, detailContentOpen], scheduleRailMetrics, { flush: 'post' });

watchEffect(() => {
  document.body.classList.add('et-app');
  document.body.classList.toggle('drawer-open', drawerOpen.value);
  document.body.dataset.page = currentPage.value;
});
</script>

<template>
  <div class="et-app" :class="{ 'drawer-open': drawerOpen }" :data-page="currentPage">
    <BackdropScene :layers="backdropLayers" :active-layer="activeLayer" />
    <a class="corner-brand" href="#" @click.prevent="changePage('home')"><span>ENJOY</span><strong>TRIP</strong></a>
    <div class="shell et-split" :class="{ 'detail-open': detailOpen, 'detail-ready': detailContentOpen }">
      <div class="et-stage">
        <section class="et-hero hero">
          <p class="scene-label" aria-live="polite">{{ scene.label }}</p>
          <h1>{{ heroCopy[0] }}</h1>
          <p class="et-hero-desc">{{ heroCopy[1] }}</p>
          <div class="meta-row"><span>지금 이곳</span><span>{{ scene.coord }}</span></div>
          <div class="indicator" aria-label="풍경 선택">
            <button
              v-for="(item, index) in scenes"
              :key="item.label"
              type="button"
              :class="{ active: index === currentScene }"
              :aria-pressed="String(index === currentScene)"
              :aria-label="`풍경 ${index + 1}`"
              @click="applyScene(index)"
            />
          </div>
        </section>
      </div>
      <aside ref="railRef" class="et-rail" :style="railStyle" aria-label="메뉴와 여행 노트">
        <RailHeader
          :current-page="currentPage"
          :nav-items="navItems"
          :detail-open="detailOpen"
          @change-page="changePage"
          @toggle-detail="toggleDetail"
        />
        <main class="et-rail-main et-main">
          <section class="workspace-deck et-workspace" id="page-workspace" aria-live="polite">
            <ExploreWorkspace
              v-if="currentPage === 'explore'"
              :detail-content-open="detailContentOpen"
            />
            <PageWorkspace
              v-else
              :page="currentPage"
              :detail-content-open="detailContentOpen"
            />
          </section>
        </main>
        <footer class="et-rail-foot et-footer">
          <button type="button" class="et-dev-toggle" @click="uiStore.setDrawerOpen(true)">연동 정보</button>
        </footer>
      </aside>
    </div>
    <section class="drawer" :class="{ open: drawerOpen }" role="dialog" aria-modal="true" :aria-hidden="String(!drawerOpen)" aria-label="연동 정보">
      <div class="drawer-head">
        <div><h2>연동 정보</h2><p class="drawer-note">Vue 전환 후에도 mock 기반으로 먼저 동작을 확인합니다.</p></div>
        <button class="close-btn" type="button" aria-label="닫기" @click="uiStore.setDrawerOpen(false)">&times;</button>
      </div>
      <details class="contract-panel" open>
        <summary>DEV API CONTRACT</summary>
        <div class="contract-grid">
          <div class="contract-column"><span class="contract-label">STACK</span><p>Vue 3 + Vite + Router + Pinia</p></div>
          <div class="contract-column"><span class="contract-label">API</span><p>Axios mock client / 관광지 검색</p></div>
          <div class="contract-column"><span class="contract-label">CHECK</span><p>검색, 필터, 이미지 전환, drawer</p></div>
        </div>
      </details>
    </section>
  </div>
</template>
