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
  // ── 동행 목록 커서 페이지네이션 상태 ───────────────────────────────────────────
  // BE GET /companion/posts 는 CursorPageResponse { content, nextCursor, hasNext } 를 반환한다.
  // nextCursor = 다음 페이지 요청에 넘길 마지막 글 id(없으면 null), hasMore = 더 불러올 수 있는지.
  const nextCursor = ref(null)
  const hasMore = ref(true)
  // 추가 로딩(무한스크롤) 전용 상태 — 초기 loading 과 분리해 스피너를 따로 제어한다.
  const loadingMore = ref(false)
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
      thumbnail: item.thumbnail ?? item.imageUrl ?? null,
      imageUrl: item.imageUrl ?? item.thumbnail ?? null,
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

  // 상세 조회 전용 정규화.
  // normalizeItem(목록/카드용)은 화이트리스트라 BE CompanionPostResponse 의 상세 필드
  // (linkedPlan, chatRoomId, myApplicationStatus, myApplicationId, pendingCount,
  //  approvedCount, isOwner 등)를 strip 한다. 그러면 CompanionDetailView 가 store 경유로
  // 받을 때 일정 지도 마커(linkedPlan 좌표)·승인 배너(myApplicationStatus)가 안 뜬다.
  // → 상세는 raw 응답을 먼저 펼쳐 보존한 뒤, 카드 정규화의 파생값(상태 라벨/비용 포맷/
  //   author 등)을 그 위에 덮어 일관성을 유지한다(목록은 기존대로 가벼운 카드용으로 둔다).
  function normalizeDetail(item) {
    const base = normalizeItem(item)
    return {
      // 1) BE 상세 응답 원본을 먼저 펼쳐 풀 필드를 보존(미래 추가 필드 포함)
      ...item,
      // 2) 카드 정규화 파생값(상태 라벨/비용 포맷/author/location/dateRange 등)으로 덮어쓰기
      ...base,
      // 3) 상세 전용 필드는 raw 우선으로 명시 보존(카드 정규화 기본값에 덮이지 않게)
      linkedPlan: item.linkedPlan ?? base.linkedPlan ?? null,
      chatRoomId: item.chatRoomId ?? base.chatRoomId ?? null,
      myApplicationStatus: item.myApplicationStatus ?? base.myApplicationStatus ?? null,
      myApplicationId: item.myApplicationId ?? base.myApplicationId ?? null,
      pendingCount: item.pendingCount ?? base.pendingCount ?? 0,
      approvedCount: item.approvedCount ?? base.approvedCount ?? 0,
      // isOwner 는 authorId↔내 userId 비교 파생값(base)을 우선. 둘 다 없으면 raw 사용.
      isOwner: base.isOwner ?? item.isOwner ?? false,
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

  // 동행 목록 조회. reset=true 면 처음부터(커서 초기화) 다시 불러오고, false 면 다음 페이지를 이어 붙인다.
  // BE 응답이 CursorPageResponse({content,nextCursor,hasNext}) 인 경우 커서/더보기 상태를 갱신하고,
  // (구버전 호환) 배열로 내려오면 페이지네이션 없이 전체 교체한다.
  async function getList(params = {}, { reset = true } = {}) {
    // 추가 로딩은 loadingMore, 초기/리셋 로딩은 loading 으로 분리해 UI 스피너를 따로 제어한다.
    if (reset) loading.value = true
    else loadingMore.value = true
    error.value = null
    try {
      const query = { ...params }
      // 다음 페이지 요청 시에만 커서를 실어 보낸다(리셋이면 커서 없이 첫 페이지).
      if (!reset && nextCursor.value != null) query.cursor = nextCursor.value
      const { data } = await companionApi.getList(query)
      const isCursorPage = data && !Array.isArray(data) && Array.isArray(data.content)
      const items = isCursorPage ? data.content : (Array.isArray(data) ? data : [])
      const normalized = items.map(normalizeItem)
      if (reset) companions.value = normalized
      else companions.value.push(...normalized)
      if (isCursorPage) {
        nextCursor.value = data.nextCursor ?? null
        hasMore.value = data.hasNext === true
      } else {
        // 배열 응답(구버전)은 페이지네이션을 지원하지 않으므로 더보기 종료로 간주.
        nextCursor.value = null
        hasMore.value = false
      }
    } catch (e) {
      error.value = e.response?.data?.message ?? e.message ?? '목록을 불러오지 못했어요.'
      // 추가 로딩 실패 시 무한 재시도 루프를 막기 위해 더보기를 멈춘다(리셋 실패는 상태 유지).
      if (!reset) hasMore.value = false
    } finally {
      if (reset) loading.value = false
      else loadingMore.value = false
    }
  }

  // 다음 페이지 로드(무한스크롤). 더 없거나 이미 로딩 중이면 무시한다.
  async function fetchMoreCompanions() {
    if (!hasMore.value || loading.value || loadingMore.value) return
    await getList({}, { reset: false })
  }

  async function getDetail(id) {
    loading.value = true
    detailLoading.value = true
    error.value = null
    detailError.value = null
    detailNotFound.value = false
    try {
      const { data } = await companionApi.getDetail(id)
      // 상세는 풀 필드 보존 정규화(normalizeDetail)를 사용한다.
      // 목록 정규화(normalizeItem)는 카드용이라 linkedPlan/myApplicationStatus 등을 strip 한다.
      const normalized = normalizeDetail(data)
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

  // 동행 목록 초기 로드/새로고침. reset=true(기본) 면 커서를 초기화하고 첫 페이지부터 불러온다.
  // 호출 측이 인자를 안 넘기거나 true 를 넘기면 동일하게 처음부터 재조회한다(S1 호출 호환).
  function fetchCompanions(reset = true) {
    if (reset) {
      nextCursor.value = null
      hasMore.value = true
    }
    return getList({}, { reset: true })
  }

  return {
    myRooms, companions, applicants, messages,
    loading, error,
    nextCursor, hasMore, loadingMore,
    detailLoading, detailError, detailNotFound,
    applicantsLoading, applicantsError, applicantsForbidden,
    getList, getDetail, create, join, cancel,
    fetchMyRooms, fetchCompanions, fetchMoreCompanions,
    getApplications, approveApplicant, rejectApplicant, getById,
    getMyApplication, cancelApplication,
  }
})
