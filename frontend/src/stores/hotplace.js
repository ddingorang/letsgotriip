// Created: 2026-06-16 14:01:08
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { hotplaceApi } from '@/api/index.js'
import { http } from '@/api/http.js'

// Korean UI label → BE enum value
const CATEGORY_ENUM_MAP = {
  '맛집': 'RESTAURANT',
  '카페': 'CAFE',
  '명소': 'ATTRACTION',
  '관광지': 'ATTRACTION',
  '자연': 'NATURE',
  '쇼핑': 'SHOPPING',
  '숙박': 'ACCOMMODATION',
  '숙소': 'ACCOMMODATION',
  '액티비티': 'ACTIVITY',
  '포토존': 'ETC',
}

// BE enum value → Korean display label
export const CATEGORY_LABEL_MAP = {
  RESTAURANT: '맛집',
  CAFE: '카페',
  ATTRACTION: '명소',
  NATURE: '자연',
  SHOPPING: '쇼핑',
  ACCOMMODATION: '숙박',
  ACTIVITY: '액티비티',
  ETC: '기타',
}

/**
 * Normalize a BE HotPlaceResponse or HotPlaceSummaryResponse to the shape
 * the views consume (lat/lng, imageUrl, category label, etc.).
 */
function normalize(item) {
  if (!item) return item
  return {
    ...item,
    // coordinate aliases expected by views / map
    lat: item.latitude ?? item.lat,
    lng: item.longitude ?? item.lng,
    // single imageUrl alias (views use imageUrl; BE returns imageUrls array or thumbnailUrl)
    imageUrl: item.imageUrl
      ?? (item.imageUrls && item.imageUrls.length > 0 ? item.imageUrls[0] : null)
      ?? item.thumbnailUrl
      ?? null,
    // Korean category label for display; keep raw enum too
    categoryEnum: item.category,
    category: CATEGORY_LABEL_MAP[item.category] ?? item.category ?? '',
    // registrant alias
    registrant: item.registrant ?? item.submitterNickname ?? '익명',
    // registeredAt alias
    registeredAt: item.registeredAt
      ?? (item.createdAt ? new Date(item.createdAt).toLocaleDateString('ko-KR') : '-'),
    // intro alias
    intro: item.intro ?? item.description ?? '',
    // rating defaults (BE does not return rating yet)
    rating: item.rating ?? 0,
    ratingCount: item.ratingCount ?? 0,
    saveCount: item.saveCount ?? 0,
    // location display alias
    location: item.location ?? item.address ?? '',
  }
}

export const useHotplaceStore = defineStore('hotplace', () => {
  // ── Mock seed data (fallback when BE is unavailable) ──────────────────────
  const hotplaces = ref([
    { id: 1, name: '오조포구 카페', category: '카페', location: '제주 서귀포시 성산읍', address: '제주 서귀포시 성산읍 일주동로 1234', rating: 4.7, ratingCount: 128, saveCount: 179, description: '오선뷰 카페', x: 62, y: 42, registrant: '제주러버', registeredAt: '2주 전', intro: '오조리 포구 바로 앞 오선뷰 카페에요. 통창으로 우도와 성산일출봉이 한눈에 보입니다. 노을 시간대 방문 추천 🌅' },
    { id: 2, name: '함덕 해변 포차', category: '맛집', location: '제주 제주시 조천읍', address: '제주 제주시 조천읍 함덕로 45', rating: 4.5, ratingCount: 86, saveCount: 120, description: '해변 포차', x: 28, y: 22, registrant: '바다러버', registeredAt: '1주 전', intro: '함덕 해변 바로 앞에 위치한 포차예요. 신선한 해산물과 시원한 뷰가 일품입니다.' },
    { id: 3, name: '사려니 숲길 입구', category: '명소', location: '제주 제주시 조천읍', address: '제주 제주시 조천읍 교래리 산 137-1', rating: 4.9, ratingCount: 212, saveCount: 297, description: '숲길 입구', x: 45, y: 65, registrant: '숲속산책', registeredAt: '3일 전', intro: '신비로운 사려니 숲길의 시작점이에요. 이른 아침 안개가 낀 숲길이 특히 아름답습니다.' },
  ])

  const myHotplaces = ref([
    { id: 4, name: '오조포구 카페', category: '카페', location: '제주 서귀포시 성산읍', status: '승인 대기', note: '나에게만 보임 · 빙금 전 등록' },
    { id: 5, name: '사려니 숲길 입구', category: '명소', location: '제주 제주시 조천읍', status: '공개 중', rating: 4.9, ratingCount: 212 },
  ])

  const registrationSuccess = ref(false)

  // ── Mock fallback ─────────────────────────────────────────────────────────
  function getById(id) {
    return hotplaces.value.find(h => h.id === Number(id))
  }

  // ── Real API actions ──────────────────────────────────────────────────────

  /**
   * Fetch paginated list from BE.
   * Spring Page response shape: { content: [...], totalElements, ... }
   * Returns normalized items array; on error falls back to mock hotplaces.value.
   */
  async function getList(params) {
    try {
      const res = await hotplaceApi.getList(params)
      const items = (res.data?.content ?? res.data ?? []).map(normalize)
      hotplaces.value = items
      return items
    } catch (e) {
      console.warn('[hotplace] getList failed, using mock data', e)
      return hotplaces.value
    }
  }

  /**
   * Fetch single hotplace detail from BE.
   * Returns a normalized item; on error falls back to mock getById.
   */
  async function getDetail(id) {
    try {
      const res = await hotplaceApi.getDetail(id)
      return normalize(res.data)
    } catch (e) {
      console.warn('[hotplace] getDetail failed, using mock data', e)
      return getById(id) ?? null
    }
  }

  /**
   * POST /api/community/hotplaces (HotPlaceCreateRequest).
   * payload: { name, address, description, category (enum string), imageUrls, latitude, longitude }
   * Returns the created HotPlaceResponse (normalized).
   */
  async function create(payload) {
    const res = await http.post('/api/community/hotplaces', payload)
    return normalize(res.data)
  }

  /**
   * Map a Korean UI category label to the BE enum value.
   * Defaults to 'RESTAURANT' if no match found.
   */
  function toCategoryEnum(koreanLabel) {
    return CATEGORY_ENUM_MAP[koreanLabel] ?? 'RESTAURANT'
  }

  return {
    hotplaces,
    myHotplaces,
    registrationSuccess,
    getById,
    getList,
    getDetail,
    create,
    toCategoryEnum,
  }
})
