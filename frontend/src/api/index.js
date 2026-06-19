// Created: 2026-06-16 13:22:42 (rewritten for real Spring backend at :9090)
import { http } from '@/api/http.js'

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

// ── Attractions ───────────────────────────────────────────────────────────────
// GET /api/attractions?areaCode=&sigunguCode=&contentTypeId=&keyword=&page=1&size=10
// GET /api/attractions/areas
// GET /api/attractions/{contentId}
export const attractionApi = {
  list: (params) => http.get('/api/attractions', { params }),
  areas: () => http.get('/api/attractions/areas'),
  detail: (contentId) => http.get(`/api/attractions/${contentId}`),
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
}

// ── Notifications (내 알림, 인증 필요) ────────────────────────────────────────
// GET /api/notifications, GET /unread-count, PATCH /read-all, PATCH /{id}/read
export const notificationApi = {
  list: () => http.get('/api/notifications'),
  unreadCount: () => http.get('/api/notifications/unread-count'),
  markAllRead: () => http.patch('/api/notifications/read-all'),
  markRead: (id) => http.patch(`/api/notifications/${id}/read`),
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
  // 두 계획 비교 (소유자) → PlanCompareResponseDto
  compare: (aId, bId) => http.get('/api/plans/compare', { params: { aId, bId } }),
  // 예산 추정 (소유자) → 일자별/총 예산
  getBudget: (planId) => http.get(`/api/plans/${planId}/budget`),
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
  getPost: (id) => http.get(`/api/community/posts/${id}`),
  createPost: (data) => http.post('/api/community/posts', data),
  updatePost: (id, data) => http.patch(`/api/community/posts/${id}`, data),
  deletePost: (id) => http.delete(`/api/community/posts/${id}`),
  likePost: (id) => http.post(`/api/community/posts/${id}/likes`),
  getComments: (postId) => http.get(`/api/community/posts/${postId}/comments`),
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
  create: (data) => http.post('/api/companion/posts', data),
  join: (id) => http.post(`/api/companion/posts/${id}/applications`),
}

// ── Chat (BE: /api/chat/rooms) ────────────────────────────────────────────────
// 히스토리는 chat store(loadHistory)가 직접 호출. 여기에는 부가 액션만 둔다.
export const chatApi = {
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
}

// ── Assistant (RAG 챗봇, BE: /api/assistant) ──────────────────────────────────
// POST /api/assistant/chat { conversationId, message } → { conversationId, reply }
export const assistantApi = {
  chat: ({ conversationId, message }) =>
    http.post('/api/assistant/chat', { conversationId, message }, { timeout: 60_000 }),
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

export default http
