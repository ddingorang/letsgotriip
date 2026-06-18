// Created: 2026-06-16 13:22:42 (rewritten for real Spring backend at :9090)
import { http } from '@/api/http.js'

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  signup: (data) => http.post('/auth/signup', data),
  login: (data) => http.post('/auth/login', data),
  refresh: () => http.post('/auth/refresh'),
  logout: () => http.post('/auth/logout'),
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

// ── Hotplace (BE: /community/hotplaces — no /api prefix) ───────────────────────
// GET /community/hotplaces            (Page)
// GET /community/hotplaces/{id}
export const hotplaceApi = {
  getList: (params) => http.get('/community/hotplaces', { params }),
  getDetail: (id) => http.get(`/community/hotplaces/${id}`),
  // BE has no /area endpoint; fall back to the list endpoint with params
  getByArea: (params) => http.get('/community/hotplaces', { params }),
}

// ── Community (BE: /community — no /api prefix) ────────────────────────────────
export const communityApi = {
  getPosts: (params) => http.get('/community/posts', { params }),
  getPost: (id) => http.get(`/community/posts/${id}`),
  createPost: (data) => http.post('/community/posts', data),
  updatePost: (id, data) => http.patch(`/community/posts/${id}`, data),
  deletePost: (id) => http.delete(`/community/posts/${id}`),
  likePost: (id) => http.post(`/community/posts/${id}/likes`),
  getComments: (postId) => http.get(`/community/posts/${postId}/comments`),
  createComment: (postId, data) => http.post(`/community/posts/${postId}/comments`, data),
  likeComment: (postId, commentId) =>
    http.post(`/community/posts/${postId}/comments/${commentId}/likes`),
}

// ── Companion (BE: /companion/posts — no /api prefix) ──────────────────────────
export const companionApi = {
  getList: (params) => http.get('/companion/posts', { params }),
  getDetail: (id) => http.get(`/companion/posts/${id}`),
  create: (data) => http.post('/companion/posts', data),
  join: (id) => http.post(`/companion/posts/${id}/applications`),
}

export default http
