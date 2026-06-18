// Created: 2026-06-18 12:40:46
import { http } from './http.js';

export function fetchPosts({ category = null, cursor = null, size = 20 } = {}) {
  const params = { size };
  if (category) params.category = category;
  if (cursor) params.cursor = cursor;
  return http.get('/community/posts', { params }).then((r) => r.data);
}

export function fetchPost(postId) {
  return http.get(`/community/posts/${postId}`).then((r) => r.data);
}

export function createPost(payload) {
  return http.post('/community/posts', payload).then((r) => r.data);
}

export function updatePost(postId, payload) {
  return http.put(`/community/posts/${postId}`, payload).then((r) => r.data);
}

export function deletePost(postId) {
  return http.delete(`/community/posts/${postId}`);
}

export function togglePostLike(postId) {
  return http.post(`/community/posts/${postId}/likes`).then((r) => r.data);
}

export function fetchComments(postId, { page = 0, size = 20 } = {}) {
  return http.get(`/community/posts/${postId}/comments`, { params: { page, size } }).then((r) => r.data);
}

export function createComment(postId, content) {
  return http.post(`/community/posts/${postId}/comments`, { content }).then((r) => r.data);
}
