// Created: 2026-06-16 13:23:36
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { hotplaceApi } from '@/api/index.js'

export const usePlacesStore = defineStore('places', () => {
  const places = ref([])
  const currentPlace = ref(null)
  const loading = ref(false)
  const selectedCategory = ref('전체')

  const categories = ['전체', '관광지', '자연', '맛집', '카페', '숙박']

  async function fetchPlaces(params = {}) {
    loading.value = true
    try {
      const res = await hotplaceApi.getList({ category: selectedCategory.value !== '전체' ? selectedCategory.value : undefined, ...params })
      places.value = res.data?.content || res.data || []
    } catch {
      places.value = mockPlaces
    } finally {
      loading.value = false
    }
  }

  async function fetchPlace(id) {
    try {
      const res = await hotplaceApi.getDetail(id)
      currentPlace.value = res.data
    } catch {
      currentPlace.value = mockPlaces.find((p) => p.id === Number(id)) || mockPlaces[0]
    }
  }

  function setCategory(category) {
    selectedCategory.value = category
    fetchPlaces()
  }

  return { places, currentPlace, loading, selectedCategory, categories, fetchPlaces, fetchPlace, setCategory }
})

export const mockPlaces = [
  {
    id: 1,
    name: '성산일출봉',
    category: '관광지',
    address: '제주 서귀포시',
    tags: ['유네스코 자연유산'],
    rating: 4.92,
    reviewCount: 2418,
    distance: '차 15분',
    rank: 1,
    lat: 33.4581,
    lng: 126.9425,
    imageUrl: null,
  },
  {
    id: 2,
    name: '협재해변',
    category: '자연',
    address: '제주 한림읍',
    tags: ['에메랄드 바다'],
    rating: 4.79,
    reviewCount: 1832,
    distance: '차 20분',
    rank: 2,
    lat: 33.3942,
    lng: 126.2394,
    imageUrl: null,
  },
  {
    id: 3,
    name: '우도',
    category: '관광지',
    address: '제주 우도면',
    tags: ['섬 속의 섬'],
    rating: 4.88,
    reviewCount: 3241,
    distance: '차 30분 + 배',
    rank: 3,
    lat: 33.5,
    lng: 126.951,
    imageUrl: null,
  },
  {
    id: 4,
    name: '감천문화마을',
    category: '문화',
    address: '부산 사하구',
    tags: ['벽화마을', '포토스팟'],
    rating: 4.75,
    reviewCount: 4120,
    distance: '차 25분',
    rank: 4,
    lat: 35.0976,
    lng: 129.0109,
    imageUrl: null,
  },
  {
    id: 5,
    name: '불국사',
    category: '문화',
    address: '경주 불국로',
    tags: ['세계문화유산', '단풍명소'],
    rating: 4.85,
    reviewCount: 5670,
    distance: '차 10분',
    rank: 5,
    lat: 35.7896,
    lng: 129.3315,
    imageUrl: null,
  },
  {
    id: 6,
    name: '제주 애월 카페거리',
    category: '카페',
    address: '제주 애월읍',
    tags: ['오션뷰', '감성카페'],
    rating: 4.71,
    reviewCount: 2890,
    distance: '차 35분',
    rank: 6,
    lat: 33.4607,
    lng: 126.3227,
    imageUrl: null,
  },
]
