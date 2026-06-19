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
  // 상세 로드 상태 — 실패/404를 가짜 "동행 모집" 객체로 위장하지 않도록 분리한다.
  const detailLoading = ref(false)
  const detailError = ref(null)
  const detailNotFound = ref(false)
  // 신청자 목록 로드 상태 — 403/실패를 빈 목록으로 위장하지 않도록 분리한다.
  const applicantsLoading = ref(false)
  const applicantsError = ref(null)
  const applicantsForbidden = ref(false)

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
      chatRoomId: item.chatRoomId ?? null,
      pendingCount: item.pendingCount ?? item.pending_count ?? 0,
      approvedCount: item.approvedCount ?? item.approved_count ?? 0,
      // 신청 여부/취소용 신청 ID/신청 상태/채팅방 ID (상세 응답 기준)
      isApplied: item.isApplied ?? false,
      myApplicationId: item.myApplicationId ?? null,
      myApplicationStatus: item.myApplicationStatus ?? null,
      chatRoomId: item.chatRoomId ?? null,
      // 연결된 여행 계획 (상세 응답 기준). 미연결 시 null.
      // linkedPlan = { planId, title, startDate, endDate, places:[{ dayNo, title, lat, lng }] }
      planId: item.planId ?? null,
      linkedPlan: item.linkedPlan ?? null,
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
    detailLoading.value = true
    error.value = null
    detailError.value = null
    detailNotFound.value = false
    try {
      const { data } = await companionApi.getDetail(id)
      const normalized = normalizeItem(data)
      const idx = companions.value.findIndex(c => c.id === normalized.id)
      if (idx !== -1) companions.value[idx] = normalized
      else companions.value.unshift(normalized)
      return normalized
    } catch (e) {
      const status = e.response?.status
      const msg = e.response?.data?.message ?? e.message ?? '상세 정보를 불러오지 못했어요.'
      error.value = msg
      // 404는 "없는 글", 그 외는 일반 오류로 구분해 뷰에서 화면을 전환한다.
      detailNotFound.value = status === 404
      detailError.value = status === 404 ? null : msg
      // 실패 시 가짜 fallback 객체를 만들지 않는다. 캐시에 있으면 그 값만 반환.
      return companions.value.find(c => c.id === Number(id)) ?? null
    } finally {
      loading.value = false
      detailLoading.value = false
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
      // companionApi.join 은 메시지 인자를 받지 않으므로 메시지가 있으면 http로 직접 호출.
      // 어느 경로든 BE 응답(data)을 반환해 호출 측이 신청 결과를 사용할 수 있게 한다.
      if (message && message.trim()) {
        const { data } = await http.post(`/api/companion/posts/${id}/applications`, { message: message.trim() })
        return data
      } else {
        const { data } = await companionApi.join(id)
        return data
      }
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 내 신청 조회 — BE GET /companion/posts/{postId}/applications/me (origin/master)
  // 204(신청 없음)는 null 로 정규화한다.
  async function getMyApplication(postId) {
    try {
      const { data, status } = await companionApi.getMyApplication(postId)
      if (status === 204) return null
      return data
    } catch {
      return null
    }
  }

  // 신청 취소 — BE DELETE /companion/posts/{postId}/applications/{applicationId}
  // cancel/cancelApplication 은 동일 기능. 뷰가 어느 이름을 부르든 깨지지 않게 둘 다 export 한다.
  async function cancel(postId, applicationId) {
    loading.value = true
    error.value = null
    try {
      await companionApi.cancelApplication(postId, applicationId)
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '신청 취소에 실패했어요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  // cancelApplication: cancel 의 별칭(동일 기능). origin/master 호환.
  const cancelApplication = cancel

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
    applicantsLoading.value = true
    error.value = null
    applicantsError.value = null
    applicantsForbidden.value = false
    try {
      const { data } = await http.get(`/api/companion/posts/${postId}/applications`)
      const list = Array.isArray(data) ? data : (data?.content ?? [])
      applicants.value = list.map(normalizeApplicant)
    } catch (e) {
      const status = e.response?.status
      const msg = e.response?.data?.message ?? e.message ?? '신청자 목록을 불러오지 못했어요.'
      error.value = msg
      // 403(방장 아님 등 권한 없음)은 별도 안내, 그 외는 일반 오류로 노출.
      // 실패를 빈 목록으로 위장하지 않도록 기존 목록을 비우고 상태를 기록한다.
      applicants.value = []
      applicantsForbidden.value = status === 403
      applicantsError.value = status === 403 ? null : msg
    } finally {
      loading.value = false
      applicantsLoading.value = false
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
    detailLoading, detailError, detailNotFound,
    applicantsLoading, applicantsError, applicantsForbidden,
    getList, getDetail, create, join, cancel,
    fetchMyRooms, fetchCompanions,
    getApplications, approveApplicant, rejectApplicant, getById,
    getMyApplication, cancelApplication,
  }
})
