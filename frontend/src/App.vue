<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppTabBar from './components/layout/AppTabBar.vue';

const route = useRoute();
const showTabBar = computed(() => route.meta.tabBar !== false);
</script>

<template>
  <div class="app-frame">
    <main class="app-main" :class="{ 'no-tab': !showTabBar }">
      <RouterView />
    </main>
    <AppTabBar v-if="showTabBar" />
  </div>
</template>
