<script setup>
defineProps({
  currentPage: { type: String, required: true },
  navItems: { type: Array, required: true },
  detailOpen: { type: Boolean, required: true }
});

const emit = defineEmits(['change-page', 'toggle-detail']);
</script>

<template>
  <header class="et-rail-head et-header header">
    <a class="logo et-brand" href="#" aria-label="EnjoyTrip 홈" @click.prevent="emit('change-page', 'home')">
      <span class="logo-kicker">국내 여행 노트</span>
      <strong class="logo-word">ENJOY<span class="logo-accent">TRIP</span></strong>
    </a>
    <button class="mobile-menu-btn" type="button" aria-expanded="false" aria-controls="site-nav">메뉴</button>
    <div class="rail-menu-row">
      <nav id="site-nav" class="nav et-float-nav" aria-label="주요 메뉴">
        <a
          v-for="item in navItems"
          :key="item.key"
          href="#"
          :class="{ active: currentPage === item.key }"
          @click.prevent="emit('change-page', item.key)"
        >
          {{ item.label }}
        </a>
      </nav>
      <button
        class="detail-toggle"
        type="button"
        :aria-pressed="String(detailOpen)"
        @click="emit('toggle-detail')"
      >
        {{ detailOpen ? '간단히' : '자세히' }}
      </button>
    </div>
  </header>
</template>
