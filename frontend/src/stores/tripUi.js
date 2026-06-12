import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchTourismPlaces } from '../api/client.js';
import { scenes } from '../data.js';

export const useTripUiStore = defineStore('trip-ui', () => {
  const currentPage = ref('explore');
  const currentScene = ref(0);
  const drawerOpen = ref(false);
  const detailOpen = ref(false);
  const detailContentOpen = ref(false);
  const places = ref([]);
  const placesLoaded = ref(false);

  const scene = computed(() => scenes[currentScene.value]);

  function setPage(page) {
    currentPage.value = page;
  }

  function setScene(index) {
    currentScene.value = (index + scenes.length) % scenes.length;
  }

  function setDrawerOpen(open) {
    drawerOpen.value = open;
  }

  function setDetailOpen(open) {
    detailOpen.value = open;
  }

  function setDetailContentOpen(open) {
    detailContentOpen.value = open;
  }

  async function loadPlaces() {
    if (placesLoaded.value) return;
    places.value = await fetchTourismPlaces();
    placesLoaded.value = true;
  }

  return {
    currentPage,
    currentScene,
    drawerOpen,
    detailOpen,
    detailContentOpen,
    places,
    placesLoaded,
    scene,
    setPage,
    setScene,
    setDrawerOpen,
    setDetailOpen,
    setDetailContentOpen,
    loadPlaces
  };
});
