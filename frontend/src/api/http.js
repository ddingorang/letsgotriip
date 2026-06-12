/**
 * Shared axios instance for all backend calls.
 * baseURL '' → Vite dev proxy routes /api, /auth, etc. to :9090
 * withCredentials: true → refresh-token cookie is sent
 *
 * Auth store is accessed lazily (inside interceptors) so this module
 * can be imported before Pinia is installed without throwing.
 */
import axios from 'axios';
import { useAuthStore } from '../stores/auth.js';

// Cached store reference – populated on first interceptor call (after app.use(pinia))
let _store = null;
function store() {
  if (!_store) _store = useAuthStore();
  return _store;
}

export const http = axios.create({
  baseURL: '',
  withCredentials: true,
});

// ── Request: attach Bearer token ──────────────────────────────────────────────
http.interceptors.request.use((config) => {
  try {
    const token = store().accessToken;
    if (token) config.headers['Authorization'] = `Bearer ${token}`;
  } catch {
    // Pinia not yet active (SSR / tests) – skip
  }
  return config;
});

// ── Response: single-flight 401 → refresh → retry ────────────────────────────
let refreshPromise = null;

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const orig = error.config;

    if (error.response?.status !== 401 || orig._retry) {
      return Promise.reject(error);
    }

    orig._retry = true;

    // Share one in-flight refresh across concurrent 401 failures
    if (!refreshPromise) {
      refreshPromise = store()
        .refresh()
        .finally(() => { refreshPromise = null; });
    }

    try {
      await refreshPromise;
      const token = store().accessToken;
      if (token) orig.headers['Authorization'] = `Bearer ${token}`;
      return http(orig);
    } catch (e) {
      try { store().clear(); } catch {}
      return Promise.reject(e);
    }
  },
);
