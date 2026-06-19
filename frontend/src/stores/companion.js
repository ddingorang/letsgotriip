// Created: 2026-06-18 16:04:27
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { companionApi } from '@/api/index.js'
import { http } from '@/api/http.js'
import { useAuthStore } from '@/stores/auth.js'

export const useCompanionStore = defineStore('companion', () => {
  const companions = ref([])
  const myRooms = ref([])
  const applicants = ref([])
  const messages = ref({})
  const loading = ref(false)
  const error = ref(null)

  // ── helpers ───────────────────────────────────────────────────────────────────

  function normalizeItem(item) {
    const authStore = useAuthStore()
    const myUserId = authStore.user?.userId
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
      isOwner: myUserId != null && item.authorId != null
        ? Number(item.authorId) === Number(myUserId)
        : (item.isOwner ?? false),
      chatRoomId: item.chatRoomId ?? null,
      pendingCount: item.pendingCount ?? item.pending_count ?? 0,
      approvedCount: item.approvedCount ?? item.approved_count ?? 0,
    }
  }

  // ── actions ───────────────────────────────────────────────────────────────────

  async function fetchMyRooms() {
    try {
      const { data } = await companionApi.getMyRooms()
      myRooms.value = (Array.isArray(data) ? data : []).map(r => ({
        id: r.chatRoomId,
        title: r.title,
        region: r.region,
        daysLeft: r.daysLeft ?? null,
        isHost: r.isHost,
        unreadCount: 0,
      }))
    } catch (e) {
      // 미로그인 등 조용히 실패
    }
  }

  async function getList(params) {
    loading.value = true
    error.value = null
    try {
      const { data } = await companionApi.getList(params)
      const items = Array.isArray(data) ? data : (data?.content ?? [])
      companions.value = items.map(normalizeItem)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '목록을 불러오지 못했어요.'
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
      const { data } = await companionApi.join(id)
      return data
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function getMyApplication(postId) {
    try {
      const { data, status } = await companionApi.getMyApplication(postId)
      if (status === 204) return null
      return data
    } catch {
      return null
    }
  }

  async function cancelApplication(postId, applicationId) {
    try {
      await companionApi.cancelApplication(postId, applicationId)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '취소에 실패했어요.'
      throw e
    }
  }

  // ── applicant normalization ───────────────────────────────────────────────────

  function normalizeApplicant(item) {
    return {
      id: item.id,
      nickname: item.applicantNickname ?? item.nickname ?? '-',
      ageGroup: item.ageGroup ?? '-',
      tripCount: item.tripCount ?? 0,
      mannerScore: item.mannerScore ?? null,
      message: item.message ?? '',
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
      const a = applicants.value.find(a => a.id === applicationId)
      if (a) a.status = 'rejected'
      error.value = e.response?.data?.message ?? e.message ?? '거절에 실패했어요.'
    }
  }

  function getById(id) {
    return companions.value.find(c => c.id === Number(id))
  }

  function fetchCompanions() {
    return getList({})
  }

  return {
    myRooms, companions, applicants, messages,
    loading, error,
    getList, getDetail, create, join,
    fetchMyRooms, fetchCompanions,
    getApplications, approveApplicant, rejectApplicant, getById,
    getMyApplication, cancelApplication,
  }
})
