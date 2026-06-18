<template>
  <div class="app-frame">
    <main class="app-main" :class="{ 'no-tab': !showTabBar }">
      <RouterView />
    </main>
    <BottomNav v-if="showTabBar" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import BottomNav from '@/components/common/BottomNav.vue'

const route = useRoute()
const showTabBar = computed(() => route.meta.tabBar !== false)
</script>

<style>
.app-frame {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.app-main.no-tab {
  /* Full-screen flows: tab bar hidden, main gets all vertical space */
  flex: 1;
}
</style>
