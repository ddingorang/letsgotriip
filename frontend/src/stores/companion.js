// Created: 2026-06-16 14:01:32
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { companionApi } from '@/api/index.js'
import { http } from '@/api/http.js'

export const useCompanionStore = defineStore('companion', () => {
  const myRooms = ref([
    { id: 1, title: '제주 3박 4일', daysLeft: 2, lastMsg: '민지: 렌터카 11시에 공항에서 픽업할게요!', time: '오후 2:10', unreadCount: 3 },
    { id: 2, title: '강릉 카페 투어', daysLeft: 11, lastMsg: '바다러버: 안목해변 쪽부터 들까요?', time: '오전 9:42', unreadCount: 0 },
    { id: 3, title: '부산 야경 원정대', daysLeft: 18, lastMsg: '사진은 공유앨범에 올려주세요 📸', time: '어제', unreadCount: 0 },
    { id: 4, title: '경주 역사 한바퀴', daysLeft: null, lastMsg: '다들 즐거웠어요! 다음에 또 가요 😄', time: '5.28', unreadCount: 0, ended: true },
  ])

  const companions = ref([
    {
      id: 1, title: '제주 3박 4일 동행 구해요', location: '제주', dateRange: '6.12-6.15',
      status: '모집중', currentCount: 3, maxCount: 4, thumbnail: null,
      author: { nickname: '민지', role: '방장', tripCount: 12 },
      period: '3박 4일', estimatedCost: '1인 35만원',
      tags: ['#20대', '#뚜벅이', '#맛집투어'],
      intro: '제주 동쪽 위주로 여유롭게 도는 일정이에요. 뚜벅이라 렌터카 같이 빌리실 분이면 더 좋아요! 맛집·카페 좋아하시는 20대 환영합니다 😊',
      isOwner: false, pendingCount: 2, approvedCount: 1,
    },
    {
      id: 2, title: '주말 강릉 카페 투어 같이 가요', location: '강릉', dateRange: '6.21 당일',
      status: '모집중', currentCount: 2, maxCount: 3, thumbnail: null,
      author: { nickname: '카페러버', role: '방장', tripCount: 5 },
      period: '당일', estimatedCost: '1인 5만원',
      tags: ['#20대', '#30대', '#카페투어'],
      intro: '강릉 안목해변부터 시작해서 유명 카페들 돌아보는 일정이에요.',
      isOwner: false, pendingCount: 0, approvedCount: 1,
    },
    {
      id: 3, title: '지리산 종주 1박 2일 (초보 환영)', location: '지리산', dateRange: '7.5-7.6',
      status: '마감임박', currentCount: 4, maxCount: 4, thumbnail: null,
      author: { nickname: '등산왕', role: '방장', tripCount: 20 },
      period: '1박 2일', estimatedCost: '1인 10만원',
      tags: ['#등산', '#초보환영', '#지리산'],
      intro: '지리산 천왕봉 종주 코스예요. 초보자도 함께 할 수 있게 천천히 진행합니다.',
      isOwner: false, pendingCount: 0, approvedCount: 3,
    },
  ])

  const applicants = ref([
    { id: 1, nickname: '여행가_지민', ageGroup: '20대', tripCount: 8, mannerScore: 4.9, message: '안녕하세요! 렌터카 같이 빌릴 수 있어요 🚗', status: 'pending' },
    { id: 2, nickname: '길따라', ageGroup: '30대', tripCount: 3, mannerScore: 4.7, message: '제주 동쪽 잘 알아요. 맛집 리스트 공유 가능합니다!', status: 'pending' },
    { id: 3, nickname: '바람돌이', ageGroup: '20대', tripCount: 1, mannerScore: null, message: '첫 동행이지만 잘 맞춰갈게요 :)', status: 'approved' },
  ])

  const messages = ref({
    1: [
      { id: 1, senderId: 'other', sender: '민지', text: '민지님 안녕하세요! 제주 동행 신청 받아주셔서 감사해요 😊', time: '오후 2:05' },
      { id: 2, senderId: 'me', text: '반가워요! 제가 짜둔 일정 공유드릴게요. 같이 보고 조율해요~', time: '오후 2:06' },
      { id: 3, senderId: 'me', type: 'plan', planTitle: '제주 3박 4일', dateRange: '6.12-6.15', spotCount: 12, time: '오후 2:07' },
      { id: 4, senderId: 'other', sender: '민지', text: '우와 동선 깔끔하네요! Day 2 우도는 저도 꼭 가고 싶었어요 🚲', time: '오후 2:08' },
      { id: 5, senderId: 'me', text: '좋아요! 그럼 숙소는 성산 쪽으로 잡을까요?', time: '오후 2:10' },
    ],
  })

  const loading = ref(false)
  const error = ref(null)

  // ── helpers ───────────────────────────────────────────────────────────────────

  function normalizeItem(item) {
    // Map BE field names → store shape used by the views
    return {
      id: item.id,
      title: item.title ?? '',
      location: item.region ?? item.location ?? '',
      dateRange: item.travelDate ?? item.dateRange ?? '',
      status: item.status ?? '모집중',
      currentCount: item.currentMembers ?? item.currentCount ?? 1,
      maxCount: item.maxMembers ?? item.maxCount ?? 4,
      thumbnail: item.thumbnail ?? null,
      author: item.author ?? { nickname: item.authorNickname ?? '-', role: '방장', tripCount: 0 },
      period: item.duration ?? item.period ?? '-',
      estimatedCost: item.estimatedCost != null
        ? `${Number(item.estimatedCost).toLocaleString()}원`
        : (item.estimatedCostStr ?? '-'),
      tags: item.tags ?? [],
      intro: item.description ?? item.intro ?? '',
      isOwner: item.isOwner ?? false,
      pendingCount: item.pendingCount ?? 0,
      approvedCount: item.approvedCount ?? 0,
    }
  }

  // ── actions ───────────────────────────────────────────────────────────────────

  async function getList(params) {
    loading.value = true
    error.value = null
    try {
      const { data } = await companionApi.getList(params)
      const items = Array.isArray(data) ? data : (data?.content ?? [])
      companions.value = items.map(normalizeItem)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '목록을 불러오지 못했어요.'
      // keep mock fallback intact
    } finally {
      loading.value = false
    }
  }

  async function getDetail(id) {
    loading.value = true
    error.value = null
    try {
      const { data } = await companionApi.getDetail(id)
      const normalized = normalizeItem(data)
      const idx = companions.value.findIndex(c => c.id === normalized.id)
      if (idx !== -1) companions.value[idx] = normalized
      else companions.value.unshift(normalized)
      return normalized
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '상세 정보를 불러오지 못했어요.'
      // fallback: return existing cached item if present
      return companions.value.find(c => c.id === Number(id)) ?? null
    } finally {
      loading.value = false
    }
  }

  async function create(payload) {
    loading.value = true
    error.value = null
    try {
      const { data } = await companionApi.create(payload)
      const normalized = normalizeItem(data)
      companions.value.unshift(normalized)
      return normalized
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '등록에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function join(id) {
    loading.value = true
    error.value = null
    try {
      await companionApi.join(id)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // ── applicant normalization ───────────────────────────────────────────────────

  // Map BE CompanionApplicationResponse fields → view shape
  function normalizeApplicant(item) {
    return {
      id: item.id,
      nickname: item.applicantNickname ?? item.nickname ?? '-',
      ageGroup: item.ageGroup ?? '-',
      tripCount: item.tripCount ?? 0,
      mannerScore: item.mannerScore ?? null,
      message: item.message ?? '',
      // BE status enum: PENDING / APPROVED / REJECTED → lowercase for views
      status: (item.status ?? 'PENDING').toLowerCase(),
      applicantId: item.applicantId ?? null,
      profileImageUrl: item.applicantProfileImageUrl ?? item.profileImageUrl ?? null,
    }
  }

  // ── applicant management (BE) ─────────────────────────────────────────────────

  async function getApplications(postId) {
    loading.value = true
    error.value = null
    try {
      const { data } = await http.get(`/api/companion/posts/${postId}/applications`)
      const list = Array.isArray(data) ? data : (data?.content ?? [])
      applicants.value = list.map(normalizeApplicant)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청자 목록을 불러오지 못했어요.'
      // keep existing mock applicants as fallback
    } finally {
      loading.value = false
    }
  }

  async function approveApplicant(postId, applicationId) {
    try {
      const { data } = await http.patch(
        `/api/companion/posts/${postId}/applications/${applicationId}/approve`
      )
      const normalized = normalizeApplicant(data)
      const idx = applicants.value.findIndex(a => a.id === applicationId)
      if (idx !== -1) applicants.value[idx] = normalized
    } catch (e) {
      // optimistic local fallback
      const a = applicants.value.find(a => a.id === applicationId)
      if (a) a.status = 'approved'
      error.value = e.response?.data?.message ?? e.message ?? '승인에 실패했어요.'
    }
  }

  async function rejectApplicant(postId, applicationId) {
    try {
      const { data } = await http.patch(
        `/api/companion/posts/${postId}/applications/${applicationId}/reject`
      )
      const normalized = normalizeApplicant(data)
      const idx = applicants.value.findIndex(a => a.id === applicationId)
      if (idx !== -1) applicants.value[idx] = normalized
    } catch (e) {
      // optimistic local fallback
      const a = applicants.value.find(a => a.id === applicationId)
      if (a) a.status = 'rejected'
      error.value = e.response?.data?.message ?? e.message ?? '거절에 실패했어요.'
    }
  }

  function getById(id) {
    return companions.value.find(c => c.id === Number(id))
  }

  return {
    myRooms, companions, applicants, messages,
    loading, error,
    getList, getDetail, create, join,
    getApplications, approveApplicant, rejectApplicant, getById,
  }
})
