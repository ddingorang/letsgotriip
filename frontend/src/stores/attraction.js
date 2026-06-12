import { defineStore } from 'pinia';
import { ref } from 'vue';
import { http } from '../api/http.js';

export const useAttractionStore = defineStore('attraction', () => {
  const searchResults = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchParams = ref({});
  const areas = ref([]);

  /**
   * Search attractions.
   * params: { areaCode, sigunguCode, contentTypeId, keyword, page, size }
   */
  async function search(params = {}) {
    loading.value = true;
    error.value = null;
    searchParams.value = params;
    try {
      const { data } = await http.get('/api/attractions', { params });
      searchResults.value = Array.isArray(data) ? data : [];
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '검색 중 오류가 발생했습니다.';
      searchResults.value = [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * Fetch a single attraction detail by contentId.
   * Returns the item (or throws) – callers own the local state.
   */
  async function fetchDetail(contentId) {
    const { data } = await http.get(`/api/attractions/${contentId}`);
    return data;
  }

  /** Load area code list from /api/attractions/areas */
  async function loadAreas() {
    try {
      const { data } = await http.get('/api/attractions/areas');
      areas.value = Array.isArray(data) ? data : [];
    } catch {
      areas.value = [];
    }
  }

  return { searchResults, loading, error, searchParams, areas, search, fetchDetail, loadAreas };
});
