// Created: 2026-06-16 14:01:08
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useHotplaceStore = defineStore('hotplace', () => {
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

  function getById(id) {
    return hotplaces.value.find(h => h.id === Number(id))
  }

  return { hotplaces, myHotplaces, registrationSuccess, getById }
})
