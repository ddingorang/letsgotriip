// Created: 2026-06-05 16:00:07
import { ref } from 'vue';
import { defineStore } from 'pinia';
import { fetchFestivals } from '../api/festival.js';

export const useFestivalStore = defineStore('festival', () => {
  const festivals = ref([]);
  const loading = ref(false);
  const error = ref('');
  const loadedArea = ref(null);

  async function loadFestivals(areaCode = '') {
    if (loading.value) return;
    loading.value = true;
    error.value = '';
    try {
      festivals.value = await fetchFestivals({ areaCode });
      loadedArea.value = areaCode;
    } catch (e) {
      error.value = e instanceof Error ? e.message : '행사 정보를 불러오지 못했습니다.';
    } finally {
      loading.value = false;
    }
  }

  return { festivals, loading, error, loadedArea, loadFestivals };
});
