// Created: 2026-06-16 13:22:42 (rewritten for real Spring backend at :9090)
import { http } from '@/api/http.js'
import { useAuthStore } from '@/stores/auth.js'

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  signup: (data) => http.post('/auth/signup', data),
  login: (data) => http.post('/auth/login', data),
  refresh: () => http.post('/auth/refresh'),
  logout: () => http.post('/auth/logout'),
  // 비밀번호 재설정 (공개)
  // POST /auth/password/reset-request { email } → { token, expiresAt, demoNote }
  // POST /auth/password/reset { token, newPassword }
  requestPasswordReset: (data) => http.post('/auth/password/reset-request', data),
  resetPassword: (data) => http.post('/auth/password/reset', data),
}

// ── Users (BE: /users, 인증 필요) ─────────────────────────────────────────────
// 프로필/선호도 일반 갱신은 각 뷰가 http 로 직접 호출(/users/me 등). 여기에는
// 멀티파트 등 부가 액션만 둔다.
export const userApi = {
  // 프로필 이미지 업로드: multipart/form-data, field명 'file' → { imageUrl }
  uploadProfileImage: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/users/me/profile-image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// ── Albums (내 여행 앨범, BE: /users/me/albums, 인증 필요) ─────────────────────
// GET    /users/me/albums            → 내 앨범 목록
// GET    /users/me/albums/{albumId}  → 앨범 단건
// POST   /users/me/albums            → 앨범 생성
// POST   /users/me/albums/images     → 앨범 이미지 업로드(multipart 'file') → { imageUrl }
export const albumApi = {
  list: () => http.get('/users/me/albums'),
  get: (albumId) => http.get(`/users/me/albums/${albumId}`),
  create: (body) => http.post('/users/me/albums', body),
  uploadImage: (formData) =>
    http.post('/users/me/albums/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  // 공유 토큰 발급 (소유자) → { token, ... } (앨범 공개 링크용)
  share: (albumId) => http.post(`/users/me/albums/${albumId}/share`),
  // 공유 토큰으로 공개 조회 (인증 불필요) → GET /api/albums/shared/{token}
  getShared: (token) => http.get(`/api/albums/shared/${token}`),
}

// ── Attractions ───────────────────────────────────────────────────────────────
// GET /api/attractions?areaCode=&sigunguCode=&contentTypeId=&keyword=&page=1&size=10
// GET /api/attractions/areas
// GET /api/attractions/{contentId}
export const attractionApi = {
  list: (params) => http.get('/api/attractions', { params }),
  areas: () => http.get('/api/attractions/areas'),
  detail: (contentId) => http.get(`/api/attractions/${contentId}`),
  images: (contentId) => http.get(`/api/attractions/${contentId}/images`),
}

// ── Reviews (관광지 리뷰, BE: /api/attractions/{contentId}/reviews) ────────────
// GET    /api/attractions/{contentId}/reviews
// POST   /api/attractions/{contentId}/reviews { rating, content }
// PATCH  /api/attractions/{contentId}/reviews/{reviewId}
// DELETE /api/attractions/{contentId}/reviews/{reviewId}
export const reviewApi = {
  list: (contentId) => http.get(`/api/attractions/${contentId}/reviews`),
  create: (contentId, data) => http.post(`/api/attractions/${contentId}/reviews`, data),
  update: (contentId, reviewId, data) =>
    http.patch(`/api/attractions/${contentId}/reviews/${reviewId}`, data),
  remove: (contentId, reviewId) =>
    http.delete(`/api/attractions/${contentId}/reviews/${reviewId}`),
  // 내가 쓴 리뷰 전체 (인증 필요) → GET /api/reviews/me
  myReviews: () => http.get('/api/reviews/me'),
}

// ── Favorites (즐겨찾기/찜, BE: /api/favorites, 인증 필요) ─────────────────────
// POST   /api/favorites { targetType, targetId }   → 토글
// GET    /api/favorites?type=                        → 목록(type 선택 필터)
// DELETE /api/favorites/{type}/{id}
export const favoriteApi = {
  toggle: (targetType, targetId, targetName) => http.post('/api/favorites', { targetType, targetId, targetName }),
  list: (type) => http.get('/api/favorites', { params: type ? { type } : undefined }),
  remove: (type, id) => http.delete(`/api/favorites/${type}/${id}`),
}

// ── Search (통합 검색, BE: /api/search) ───────────────────────────────────────
// GET /api/search?q=&type=   (type 기본 'all')
export const searchApi = {
  search: (q, type = 'all') => http.get('/api/search', { params: { q, type } }),
}

// ── Festivals ─────────────────────────────────────────────────────────────────
// GET /api/festivals?areaCode=&status=
export const festivalApi = {
  list: (params) => http.get('/api/festivals', { params }),
}

// ── Notices ───────────────────────────────────────────────────────────────────
// GET /api/notices, GET /api/notices/{id}
// POST /api/notices, PUT /api/notices/{id}, DELETE /api/notices/{id} (관리자 전용)
export const noticeApi = {
  list: () => http.get('/api/notices'),
  detail: (id) => http.get(`/api/notices/${id}`),
  // body: { category?, title, content, pinned }
  create: (data) => http.post('/api/notices', data),
  update: (id, data) => http.put(`/api/notices/${id}`, data),
  remove: (id) => http.delete(`/api/notices/${id}`),
}

// ── Gamification (챌린지/뱃지, 인증 필요) ─────────────────────────────────────
export const gamificationApi = {
  summary: () => http.get('/api/gamification/summary'),
  quests: () => http.get('/api/gamification/quests'),
}

// ── Notifications (내 알림, 인증 필요) ────────────────────────────────────────
// GET /api/notifications, GET /unread-count, PATCH /read-all, PATCH /{id}/read
export const notificationApi = {
  list: () => http.get('/api/notifications'),
  unreadCount: () => http.get('/api/notifications/unread-count'),
  markAllRead: () => http.patch('/api/notifications/read-all'),
  markRead: (id) => http.patch(`/api/notifications/${id}/read`),
  streamUrl: () => '/api/notifications/stream',

  /**
   * 실시간 알림 SSE 구독. EventSource 는 Authorization 헤더를 못 쓰므로
   * fetch + ReadableStream 으로 SSE 를 수동 파싱한다(assistantApi.chatStream 패턴).
   * @param {(data: string) => void} onMessage  SSE data 프레임 수신마다 호출
   * @param {{ signal?: AbortSignal }} [opts]    구독 취소용
   * @returns {Promise<void>} 스트림 종료 시 resolve
   */
  async connectStream(onMessage, { signal } = {}) {
    let token = null
    try {
      token = useAuthStore().accessToken
    } catch {
      // Pinia 미활성(테스트 등) — 토큰 없이 진행
    }

    const headers = { Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    const res = await fetch(`${API_BASE}/api/notifications/stream`, {
      method: 'GET',
      headers,
      credentials: 'include',
      signal,
    })

    if (!res.ok || !res.body) {
      const err = new Error(`알림 스트림 구독 실패 (HTTP ${res.status})`)
      err.status = res.status
      throw err
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const parseFrame = (frame) => {
      const dataLines = []
      for (const rawLine of frame.split('\n')) {
        const line = rawLine.replace(/\r$/, '')
        if (!line || line.startsWith(':')) continue
        const idx = line.indexOf(':')
        const field = idx === -1 ? line : line.slice(0, idx)
        let value = idx === -1 ? '' : line.slice(idx + 1)
        if (value.startsWith(' ')) value = value.slice(1)
        if (field === 'data') dataLines.push(value)
      }
      if (dataLines.length) onMessage?.(dataLines.join('\n'))
    }

    try {
      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        let sep
        while ((sep = buffer.indexOf('\n\n')) !== -1) {
          const frame = buffer.slice(0, sep)
          buffer = buffer.slice(sep + 2)
          if (frame.trim()) parseFrame(frame)
        }
      }
      buffer += decoder.decode()
      if (buffer.trim()) parseFrame(buffer)
    } finally {
      try {
        reader.releaseLock()
      } catch {
        // 무시
      }
    }
  },
}

// ── Plans ─────────────────────────────────────────────────────────────────────
// GET  /api/plans
// GET  /api/plans/{id}
// POST /api/plans
// PATCH /api/plans/{id}
// DELETE /api/plans/{id}
// POST   /api/plans/{id}/days/{dayNo}/places
// PUT    /api/plans/{id}/days/{dayNo}/places
// DELETE /api/plans/{id}/days/{dayNo}/places/{placeId}
export const planApi = {
  getMyPlans: (params) => http.get('/api/plans', { params }),
  getPlan: (id) => http.get(`/api/plans/${id}`),
  createPlan: (data) => http.post('/api/plans', data),
  updatePlan: (id, data) => http.patch(`/api/plans/${id}`, data),
  deletePlan: (id) => http.delete(`/api/plans/${id}`),
  addPlace: (id, dayNo, data) => http.post(`/api/plans/${id}/days/${dayNo}/places`, data),
  replacePlaces: (id, dayNo, data) => http.put(`/api/plans/${id}/days/${dayNo}/places`, data),
  removePlace: (id, dayNo, placeId) =>
    http.delete(`/api/plans/${id}/days/${dayNo}/places/${placeId}`),
  // 공유 토큰 발급 (소유자) → { shareToken, shareUrl }
  share: (planId) => http.post(`/api/plans/${planId}/share`),
  // 공유 토큰으로 공개 조회 (인증 불필요) → PlanDetailResponseDto
  getShared: (token) => http.get(`/api/plans/shared/${token}`),
  // 두 계획 비교 (소유자) → PlanCompareResponseDto (레거시 2개 호환)
  compare: (aId, bId) => http.get('/api/plans/compare', { params: { aId, bId } }),
  // N개 계획 비교 (소유자) → { plans: PlanStat[] }. ids는 배열 → ids=1,2,3 쿼리로 직렬화
  compareMany: (ids) =>
    http.get('/api/plans/compare-many', {
      params: { ids: (ids ?? []).join(',') },
    }),
  // 예산 추정 (소유자) → 일자별/총 예산
  getBudget: (planId) => http.get(`/api/plans/${planId}/budget`),
  // 일자별 자동차 도로 경로 (카카오 길찾기) → { planId, enabled, days:[{dayNo, distanceMeters, durationSeconds, taxiFare, tollFare, path:[[lat,lng],...]}] }
  getRoutePath: (planId) => http.get(`/api/plans/${planId}/route-path`),
}

// ── Recommendations ───────────────────────────────────────────────────────────
// POST /api/recommendations
// GET  /api/recommendations?page=&size=
// GET  /api/recommendations/{id}
// POST /api/recommendations/{id}/save-plan
export const recommendApi = {
  create: (data) => http.post('/api/recommendations', data, { timeout: 40_000 }),
  list: (params) => http.get('/api/recommendations', { params }),
  get: (id) => http.get(`/api/recommendations/${id}`),
  savePlan: (id) => http.post(`/api/recommendations/${id}/save-plan`),
}

// NOTE: BE serves these at /community/* and /companion/* (no /api prefix), but the
// SPA also uses /community and /companion as page routes. To avoid the dev proxy
// hijacking page navigations, the FE calls them under /api/* and the vite proxy
// rewrites /api away before forwarding to the BE (see vite.config.js).

// ── Hotplace (BE: /community/hotplaces) ───────────────────────────────────────
export const hotplaceApi = {
  getList: (params) => http.get('/api/community/hotplaces', { params }),
  getDetail: (id) => http.get(`/api/community/hotplaces/${id}`),
  // BE has no /area endpoint; fall back to the list endpoint with params
  getByArea: (params) => http.get('/api/community/hotplaces', { params }),
  // 관리자 승인 대기 목록/승인/반려
  pending: () => http.get('/api/community/hotplaces/pending'),
  approve: (id) => http.post(`/api/community/hotplaces/${id}/approve`),
  reject: (id) => http.post(`/api/community/hotplaces/${id}/reject`),
}

// ── Community (BE: /community) ────────────────────────────────────────────────
export const communityApi = {
  // 이미지 업로드: multipart/form-data, field명 'file' → { imageUrl }
  // (FE는 /api/community/images 로 호출하고 dev 프록시/nginx가 /api 를 제거)
  uploadImage: (formData) =>
    http.post('/api/community/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  getPosts: (params) => http.get('/api/community/posts', { params }),
  // 내가 좋아요한 게시글(커서 페이지, 인증 필요). cursor=직전 페이지 마지막 PostLike.id
  // → CursorPageResponse<PostSummaryResponse>{ content, nextCursor, hasNext }
  getLikedPosts: (params) => http.get('/api/community/posts/liked', { params }),
  getPost: (id) => http.get(`/api/community/posts/${id}`),
  createPost: (data) => http.post('/api/community/posts', data),
  updatePost: (id, data) => http.patch(`/api/community/posts/${id}`, data),
  deletePost: (id) => http.delete(`/api/community/posts/${id}`),
  likePost: (id) => http.post(`/api/community/posts/${id}/likes`),
  // params(page,size) 선택 — BE는 Page<CommentResponse> 반환. 무인자 호출과 호환.
  getComments: (postId, params) => http.get(`/api/community/posts/${postId}/comments`, { params }),
  createComment: (postId, data) => http.post(`/api/community/posts/${postId}/comments`, data),
  deleteComment: (postId, commentId) => http.delete(`/api/community/posts/${postId}/comments/${commentId}`),
  likeComment: (postId, commentId) =>
    http.post(`/api/community/posts/${postId}/comments/${commentId}/likes`),
}

// ── Companion (BE: /companion/posts) ──────────────────────────────────────────
export const companionApi = {
  getList: (params) => http.get('/api/companion/posts', { params }),
  getMyRooms: () => http.get('/api/companion/posts/my'),
  getDetail: (id) => http.get(`/api/companion/posts/${id}`),
  // data: { title, region, travelDate, duration, maxMembers, estimatedCost, description, tags, planId? }
  // planId(optional)가 있으면 작성자 소유 계획과 연결된다(BE 검증).
  create: (data) => http.post('/api/companion/posts', data),
  join: (id) => http.post(`/api/companion/posts/${id}/applications`),
  // 내 신청 현황 조회 (신청자 본인) — GET .../applications/me
  getMyApplication: (id) => http.get(`/api/companion/posts/${id}/applications/me`),
  // 내 신청 취소 (신청자 본인) — DELETE .../applications/{applicationId}
  cancelApplication: (postId, applicationId) =>
    http.delete(`/api/companion/posts/${postId}/applications/${applicationId}`),
  // 신청자 목록/승인/반려 (방장만)
  getApplications: (postId) => http.get(`/api/companion/posts/${postId}/applications`),
  approve: (postId, applicationId) =>
    http.patch(`/api/companion/posts/${postId}/applications/${applicationId}/approve`),
  reject: (postId, applicationId) =>
    http.patch(`/api/companion/posts/${postId}/applications/${applicationId}/reject`),
  // 수정 / 모집 마감 / 글 삭제 (방장만)
  update: (postId, data) => http.patch(`/api/companion/posts/${postId}`, data),
  close: (postId) => http.patch(`/api/companion/posts/${postId}/close`),
  remove: (postId) => http.delete(`/api/companion/posts/${postId}`),
}

// ── Follow (팔로우, BE: /api/follows, 인증 필요) ──────────────────────────────
// POST /api/follows { targetUserId }                       → 토글 FollowToggleResponse
//   { targetUserId, following(bool), followerCount }
// GET  /api/follows/users/{userId}/follow-status           → FollowStatusResponse
//   { userId, following(bool), followerCount, followingCount }
// GET  /api/follows/me/following                           → List<FollowUserResponse>
//   [{ userId, nickname, profileImageUrl, bio }]
export const followApi = {
  toggle: (targetUserId) => http.post('/api/follows', { targetUserId }),
  status: (userId) => http.get(`/api/follows/users/${userId}/follow-status`),
  following: () => http.get('/api/follows/me/following'),
}

// ── Chat (BE: /api/chat/rooms) ────────────────────────────────────────────────
// 실시간 전송은 STOMP/WebSocket 경로(chat store)를 쓰고, 히스토리/부가 액션은 REST.
export const chatApi = {
  // 메시지 히스토리(Mongo 영속) — GET /api/chat/rooms/{roomId}/messages
  getMessages: (roomId) => http.get(`/api/chat/rooms/${roomId}/messages`),
  getParticipants: (roomId) => http.get(`/api/chat/rooms/${roomId}/participants`),
  leaveRoom: (roomId) => http.delete(`/api/chat/rooms/${roomId}/membership`),
  // 방 정보(제목/소개) 수정 — 방장만. body { title(<=18), description?(<=200) } → 204
  updateRoom: (roomId, data) => http.patch(`/api/chat/rooms/${roomId}`, data),
  // 본인 멤버십 음소거 토글. body { muted:boolean } → 204
  muteMembership: (roomId, data) => http.patch(`/api/chat/rooms/${roomId}/membership/mute`, data),
  // 참여자 강퇴 — 방장만 → 204
  kickParticipant: (roomId, userId) => http.delete(`/api/chat/rooms/${roomId}/participants/${userId}`),
  // 참여자 초대 — 방장만. body { nickname? | email? } → 204
  inviteParticipant: (roomId, data) => http.post(`/api/chat/rooms/${roomId}/participants`, data),
  // 방장 위임 — 현 방장만. body { newHostUserId } → 204
  transferHost: (roomId, data) => http.patch(`/api/chat/rooms/${roomId}/host`, data),
  // 채팅방 이미지 업로드: multipart/form-data, field명 'file' → { imageUrl }
  uploadRoomImage: (roomId, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post(`/api/chat/rooms/${roomId}/image`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// ── Assistant (RAG 챗봇, BE: /api/assistant) ──────────────────────────────────
// POST /api/assistant/chat        { conversationId, message } → { conversationId, reply }  (비스트리밍 폴백)
// POST /api/assistant/chat/stream { conversationId, message } → text/event-stream
//   이벤트: event:conversationId(data=대화ID) → event:token(조각)* → event:done
//
// EventSource 는 커스텀 Authorization 헤더/POST 를 못 쓰므로 fetch()+ReadableStream 으로 SSE 를 수동 파싱한다.
// Authorization Bearer 토큰은 axios 와 동일하게 auth store 의 accessToken 에서 가져온다(http.js 의 토큰 소스).
// baseURL 도 axios 와 동일하게 VITE_API_BASE_URL(미설정 시 '' → dev 프록시) 을 사용하고, refresh 쿠키 전파를 위해
// credentials:'include' 로 호출한다.
const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export const assistantApi = {
  chat: ({ conversationId, message, memory }) =>
    http.post('/api/assistant/chat', { conversationId, message, memory }, { timeout: 60_000 }),

  /**
   * SSE 스트리밍 채팅. fetch + ReadableStream 으로 토큰을 점진 수신한다.
   * @param {{ conversationId?: string|null, message: string }} body
   * @param {{
   *   onToken?: (token: string) => void,          // 응답 조각 수신마다 호출
   *   onConversationId?: (id: string) => void,    // 첫 conversationId 이벤트 수신 시 호출
   *   onError?: (message: string) => void,        // 'error' 이벤트 수신 시 호출(스트림 중단 신호)
   *   signal?: AbortSignal,                       // 중간 취소용
   * }} [handlers]
   * @returns {Promise<{ conversationId: string|null, reply: string, errored: boolean, errorMessage: string|null }>} 누적 결과
   */
  async chatStream({ conversationId, message, memory }, { onToken, onConversationId, onError, signal } = {}) {
    let token = null
    try {
      token = useAuthStore().accessToken
    } catch {
      // Pinia 미활성(테스트 등) — 토큰 없이 진행
    }

    const headers = { 'Content-Type': 'application/json', Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    const res = await fetch(`${API_BASE}/api/assistant/chat/stream`, {
      method: 'POST',
      headers,
      credentials: 'include',
      body: JSON.stringify({ conversationId: conversationId ?? null, message, memory }),
      signal,
    })

    if (!res.ok || !res.body) {
      const err = new Error(`스트리밍 요청 실패 (HTTP ${res.status})`)
      err.status = res.status
      throw err
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let reply = ''
    let convId = conversationId ?? null
    let errored = false
    let errorMessage = null

    // SSE 프레임: "event: <name>\n" + "data: <payload>\n" ... 빈 줄로 구분.
    // data 가 여러 줄이면 \n 으로 합친다(SSE 규약).
    const dispatch = (eventName, dataLines) => {
      const data = dataLines.join('\n')
      if (eventName === 'conversationId') {
        if (data) {
          convId = data
          onConversationId?.(data)
        }
      } else if (eventName === 'token') {
        if (data) {
          reply += data
          onToken?.(data)
        }
      } else if (eventName === 'error') {
        // BE가 스트림 도중 오류를 알리는 'error' 이벤트. 부분 토큰이 이미 왔더라도
        // 호출자에게 실패를 노출한다(가짜 성공 위장 금지).
        errored = true
        errorMessage = data || '응답 생성 중 오류가 발생했습니다.'
        onError?.(errorMessage)
      }
      // 'done' 및 기타 이벤트는 별도 처리 없음(루프가 스트림 종료로 마무리).
    }

    const parseFrame = (frame) => {
      let eventName = 'message'
      const dataLines = []
      for (const rawLine of frame.split('\n')) {
        const line = rawLine.replace(/\r$/, '')
        if (!line || line.startsWith(':')) continue
        const idx = line.indexOf(':')
        const field = idx === -1 ? line : line.slice(0, idx)
        // data: 뒤 선행 공백을 제거하지 않는다 — BE(Spring)가 data:<token> 형태로 토큰을
        // 그대로 보내므로, 토큰의 의미있는 선행 공백(단어 사이 띄어쓰기)이 제거되면
        // 응답에서 띄어쓰기가 모두 사라진다.
        const value = idx === -1 ? '' : line.slice(idx + 1)
        if (field === 'event') eventName = value
        else if (field === 'data') dataLines.push(value)
      }
      if (dataLines.length || eventName !== 'message') dispatch(eventName, dataLines)
    }

    try {
      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // 완성된 프레임(빈 줄 구분)만 처리하고 나머지는 버퍼에 보관
        let sep
        while ((sep = buffer.indexOf('\n\n')) !== -1) {
          const frame = buffer.slice(0, sep)
          buffer = buffer.slice(sep + 2)
          if (frame.trim()) parseFrame(frame)
        }
      }
      // 스트림 종료 후 잔여 프레임 처리
      buffer += decoder.decode()
      if (buffer.trim()) parseFrame(buffer)
    } finally {
      try {
        reader.releaseLock()
      } catch {
        // 무시
      }
    }

    return { conversationId: convId, reply, errored, errorMessage }
  },
}

// ── Documents (문서 업로드/RAG 인덱싱, BE: /api/documents) ─────────────────────
// POST   /api/documents (multipart 'file') → { id, filename, type, status, ... }
// GET    /api/documents → [ ... ]
// DELETE /api/documents/{id}
export const documentApi = {
  list: () => http.get('/api/documents'),
  upload: (formData) =>
    http.post('/api/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60_000,
    }),
  remove: (id) => http.delete(`/api/documents/${id}`),
}

// ── Checklist (여행 준비물 체크리스트, 인증 필요) ─────────────────────────────
// GET    /api/checklists?planId=            → 내 항목 목록(sortOrder/id 정렬)
// POST   /api/checklists { title, ... }     → 201 + Location
// PATCH  /api/checklists/{id}                → 부분 수정(null 필드 유지)
// PATCH  /api/checklists/{id}/toggle         → 완료/미완료 토글
// DELETE /api/checklists/{id}                → 204
// GET    /api/checklists/templates           → 내장 템플릿 3종
// POST   /api/checklists/apply?templateKey=&planId=  → 201 + 생성된 항목 목록
export const checklistApi = {
  list: (params) => http.get('/api/checklists', { params }),
  create: (data) => http.post('/api/checklists', data),
  update: (id, data) => http.patch(`/api/checklists/${id}`, data),
  toggle: (id) => http.patch(`/api/checklists/${id}/toggle`),
  remove: (id) => http.delete(`/api/checklists/${id}`),
  templates: () => http.get('/api/checklists/templates'),
  // templateKey: 'domestic' | 'overseas' | 'camping', planId 선택
  applyTemplate: (params) => http.post('/api/checklists/apply', null, { params }),
}

// ── Context (맥락 정보: 날씨/충전소/뉴스, 공개) ───────────────────────────────
// GET /api/context/weather?lat=&lng=        → 현재 날씨 + 3일 예보
// GET /api/context/ev-stations?lat=&lng=    → 주변 데모 전기차 충전소(~5)
// GET /api/context/news                     → 데모 한국어 여행 뉴스(~6)
export const contextApi = {
  weather: (lat, lng) => http.get('/api/context/weather', { params: { lat, lng } }),
  evStations: (lat, lng) => http.get('/api/context/ev-stations', { params: { lat, lng } }),
  news: () => http.get('/api/context/news'),
}

// ── Stories (내 여행 스토리, BE: /api/stories, 인증 필요) ──────────────────────
// GET    /api/stories               → 내 스토리 목록 (createdAt desc)
// GET    /api/stories/{storyId}     → 스토리 상세 (소유자만)
// POST   /api/stories               → 스토리 생성 (201 Created + Location)
// PATCH  /api/stories/{storyId}     → 부분 수정 (소유자만, null 아닌 필드만)
// DELETE /api/stories/{storyId}     → 삭제 (소유자만, 204)
export const storyApi = {
  list: () => http.get('/api/stories'),
  get: (storyId) => http.get(`/api/stories/${storyId}`),
  create: (data) => http.post('/api/stories', data),
  update: (storyId, data) => http.patch(`/api/stories/${storyId}`, data),
  remove: (storyId) => http.delete(`/api/stories/${storyId}`),
}

// ── Groups (여행 그룹/단체할인, BE: /api/groups) ──────────────────────────────
// POST   /api/groups                → 그룹 생성 (소유자 OWNER 자동 추가, 201, auth)
// GET    /api/groups                → 내가 속한/소유한 그룹 목록 (auth)
// GET    /api/groups/{id}           → 그룹 단건 (public, 없으면 400)
// GET    /api/groups/{id}/members   → 멤버 목록 (public)
// POST   /api/groups/{id}/join      → 가입 (MEMBER, auth, 204)
// DELETE /api/groups/{id}/leave     → 탈퇴 (auth, 204)
// GET    /api/groups/discounts      → 단체할인 데모 목록 (public)
export const groupApi = {
  list: () => http.get('/api/groups'),
  get: (id) => http.get(`/api/groups/${id}`),
  create: (data) => http.post('/api/groups', data),
  join: (id) => http.post(`/api/groups/${id}/join`),
  leave: (id) => http.delete(`/api/groups/${id}/leave`),
  members: (id) => http.get(`/api/groups/${id}/members`),
  discounts: () => http.get('/api/groups/discounts'),
}

// ── Analysis (STT/카톡 전처리, BE: PreprocessingController @ "analysis") ───────
// BE는 /api 프리픽스 없이 analysis/upload/kakao·analysis/upload/voice 로 서빙하며,
// 멀티파트 필드명은 'file'(@RequestParam("file"))이다. community/companion 과 동일
// 하게 FE는 /api/analysis/* 로 호출하고 dev 프록시/리버스 프록시가 /api 를 제거한다
// (프록시 rewrite 추가는 Wire 단계가 담당).
// POST /api/analysis/upload/kakao  (multipart 'file') → Long dataId
// POST /api/analysis/upload/voice  (multipart 'file') → Long dataId
export const analysisApi = {
  uploadKakao: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/api/analysis/upload/kakao', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60_000,
    })
  },
  uploadVoice: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/api/analysis/upload/voice', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120_000,
    })
  },
}

// ── 여행별 체크리스트 헬퍼 (planId 스코프) ───────────────────────────────────
// 기존 checklistApi는 그대로 두고, 여행(계획) 단위 조작용 편의 함수만 새로 추가한다.
// planId가 null/undefined면 "여행 미지정(전체)" 으로 동작한다.
// 보안: 서버가 planId 소유권을 검증하므로(남의 계획 거부) 프론트는 planId만 전달하면 된다.
export const planChecklistApi = {
  // 특정 여행의 체크리스트만 조회. planId 없으면 전체(미지정 포함) 조회.
  list: (planId) =>
    http.get('/api/checklists', { params: planId != null ? { planId } : {} }),
  // 항목 생성 — planId가 있으면 그 여행에 묶어서 생성.
  create: (title, { category = null, planId = null } = {}) =>
    http.post('/api/checklists', { title, category, planId }),
  // 완료/미완료 토글 (소유자만, 서버 검증).
  toggle: (id) => http.patch(`/api/checklists/${id}/toggle`),
  // 항목 삭제 (소유자만, 서버 검증).
  remove: (id) => http.delete(`/api/checklists/${id}`),
  // 템플릿을 특정 여행에 일괄 적용 — planId 선택.
  applyTemplate: (templateKey, planId = null) =>
    http.post('/api/checklists/apply', null, {
      params: planId != null ? { templateKey, planId } : { templateKey },
    }),
}

export default http
