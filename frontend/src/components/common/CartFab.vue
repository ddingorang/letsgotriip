<template>
  <!-- 장바구니(담는 중인 여행) 플로팅 버튼 — 탐색/홈 등에서 노출, 탭하면 내 여행으로 -->
  <Transition name="fab">
    <button
      v-if="visible"
      class="cart-fab"
      :class="{ raised: hasTabBar }"
      @click="goToPlan"
      aria-label="담는 중인 여행 보기"
    >
      <span class="fab-icon">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1" /><circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
        </svg>
        <span v-if="count > 0" class="fab-badge">{{ count > 99 ? '99+' : count }}</span>
      </span>
      <span class="fab-text">{{ title }}</span>
    </button>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlanStore } from '@/stores/plan.js'
import { useAuthStore } from '@/stores/auth.js'

const route = useRoute()
const router = useRouter()
const planStore = usePlanStore()
const authStore = useAuthStore()

// 장바구니 FAB를 띄울 둘러보기 라우트
const SHOW_ON = new Set(['home', 'explore', 'search', 'place-detail'])

const hasTabBar = computed(() => route.meta.tabBar !== false)
const count = computed(() => planStore.cartCount)
const title = computed(() => planStore.activePlanTitle || '내 여행')

const visible = computed(() =>
  authStore.isAuthenticated
  && planStore.activePlanId != null
  && SHOW_ON.has(route.name),
)

function goToPlan() {
  router.push('/plan')
}
</script>

<style scoped>
.cart-fab {
  position: fixed;
  right: max(16px, calc((100vw - 430px) / 2 + 16px));
  bottom: 24px;
  z-index: 900;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px 11px 13px;
  border-radius: var(--radius-full);
  background: var(--color-peach);
  color: #fff;
  box-shadow: 0 6px 20px rgba(247, 143, 87, 0.42);
}
.cart-fab.raised { bottom: calc(64px + env(safe-area-inset-bottom, 0px)); }
.cart-fab:active { background: var(--color-peach-pressed); }

.fab-icon { position: relative; display: flex; }
.fab-badge {
  position: absolute;
  top: -7px;
  right: -8px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: var(--radius-full);
  background: var(--color-ink);
  color: #fff;
  font-size: 10.5px;
  font-weight: 800;
  line-height: 17px;
  text-align: center;
}
.fab-text {
  font-size: 13.5px;
  font-weight: 700;
  letter-spacing: -0.2px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fab-enter-active, .fab-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.fab-enter-from, .fab-leave-to { opacity: 0; transform: translateY(12px) scale(0.92); }
</style>
