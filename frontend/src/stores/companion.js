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

  // BE CompanionStatus(enum) → 한글 라벨 매핑.
  // 마감임박은 BE에 별도 상태가 없어 정원 근접(잔여 1명 이하)일 때 FE에서 파생한다.
  function toStatusLabel(rawStatus, currentCount, maxCount) {
    switch (rawStatus) {
      case 'OPEN': {
        if (maxCount != null && currentCount != null && maxCount - currentCount <= 1 && maxCount - currentCount > 0) {
          return '마감임박'
        }
        return '모집중'
      }
      case 'CLOSED':
        return '모집완료'
      case 'CANCELLED':
        return '취소됨'
      default:
        // 이미 한글 라벨이거나 알 수 없는 값이면 그대로(없으면 모집중)
        return rawStatus ?? '모집중'
    }
  }

  function normalizeItem(item) {
    const authStore = useAuthStore()
    const myUserId = authStore.user?.userId
    const currentCount = item.currentMembers ?? item.currentCount ?? 1
    const maxCount = item.maxMembers ?? item.maxCount ?? 4
    return {
      id: item.id,
      title: item.title ?? '',
      location: item.region ?? item.location ?? '',
      dateRange: item.travelDate ?? item.dateRange ?? '',
      status: toStatusLabel(item.status, currentCount, maxCount),
      currentCount,
      maxCount,
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
      pendingCount: item.pendingCount ?? item.pending_count ?? 0,
      approvedCount: item.approvedCount ?? item.approved_count ?? 0,
      // 신청 여부/취소용 신청 ID/신청 상태/채팅방 ID (상세 응답 기준)
      isApplied: item.isApplied ?? false,
      myApplicationId: item.myApplicationId ?? null,
      myApplicationStatus: item.myApplicationStatus ?? null,
      chatRoomId: item.chatRoomId ?? null,
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
        // BE MyCompanionRoomResponse 에는 마지막 메시지/읽지 않은 수가 없어 기본값으로 정합화.
        // ended: 여행일이 지났으면(daysLeft 가 null) 종료 처리.
        ended: r.ended ?? (r.daysLeft == null),
        lastMsg: r.lastMsg ?? r.lastMessage ?? '',
        time: r.time ?? r.lastMessageAt ?? '',
        unreadCount: r.unreadCount ?? 0,
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

  async function join(id, message) {
    loading.value = true
    error.value = null
    try {
      // companionApi.join 은 메시지 인자를 받지 않으므로 메시지가 있으면 http로 직접 호출
      if (message && message.trim()) {
        await http.post(`/api/companion/posts/${id}/applications`, { message: message.trim() })
      } else {
        await companionApi.join(id)
      }
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 신청 취소 — BE DELETE /companion/posts/{postId}/applications/{applicationId}
  // companionApi 에 cancel 이 없어 http로 직접 호출한다.
  async function cancel(postId, applicationId) {
    loading.value = true
    error.value = null
    try {
      await http.delete(`/api/companion/posts/${postId}/applications/${applicationId}`)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청 취소에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // ── applicant normalization ───────────────────────────────────────────────────

  function normalizeApplicant(item) {
    return {
      id: item.id,
      nickname: item.applicantNickname ?? item.nickname ?? '-',
      // 통계 미보유 항목은 null 유지(뷰에서 조건부 표시). ageGroup은 BE가 birthDate로 파생.
      ageGroup: item.ageGroup ?? null,
      tripCount: item.tripCount ?? null,
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
    error.value = null
    try {
      const { data } = await http.patch(
        `/api/companion/posts/${postId}/applications/${applicationId}/approve`
      )
      // 성공 시에만 로컬 상태 갱신
      const normalized = normalizeApplicant(data)
      const idx = applicants.value.findIndex(a => a.id === applicationId)
      if (idx !== -1) applicants.value[idx] = normalized
    } catch (e) {
      // 실패 시 상태를 변경하지 않고 에러를 전파 (정원 초과/이미 처리됨 등)
      error.value = e.response?.data?.message ?? e.message ?? '승인에 실패했어요.'
      throw e
    }
  }

  async function rejectApplicant(postId, applicationId) {
    error.value = null
    try {
      const { data } = await http.patch(
        `/api/companion/posts/${postId}/applications/${applicationId}/reject`
      )
      // 성공 시에만 로컬 상태 갱신
      const normalized = normalizeApplicant(data)
      const idx = applicants.value.findIndex(a => a.id === applicationId)
      if (idx !== -1) applicants.value[idx] = normalized
    } catch (e) {
      // 실패 시 상태를 변경하지 않고 에러를 전파
      error.value = e.response?.data?.message ?? e.message ?? '거절에 실패했어요.'
      throw e
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
    getList, getDetail, create, join, cancel,
    fetchMyRooms, fetchCompanions,
    getApplications, approveApplicant, rejectApplicant, getById,
  }
})
