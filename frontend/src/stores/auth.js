import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { http } from '@/api/http.js'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null)
  const user = ref(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  /** Keep backward compat: isLoggedIn alias */
  const isLoggedIn = isAuthenticated

  function setAuth({ accessToken: token, userId }) {
    accessToken.value = token
    if (!user.value || user.value.userId !== userId) {
      user.value = userId ? { userId } : null
    }
  }

  function clear() {
    accessToken.value = null
    user.value = null
  }

  /** POST /auth/refresh — single source of truth for token renewal */
  async function refresh() {
    const { data } = await axios.post('/auth/refresh', null, {
      withCredentials: true,
    })
    // Expected: { accessToken, userId }
    setAuth(data)
  }

  /** Fetch /users/me and populate user object (requires accessToken set) */
  async function fetchMe() {
    const { data } = await http.get('/users/me')
    user.value = data
  }

  /** Merge partial fields into the current user object (e.g. after profile image upload). */
  function setUser(patch) {
    if (!patch) return
    user.value = { ...(user.value ?? {}), ...patch }
  }

  /**
   * POST /auth/login → store token → GET /users/me to populate user.
   * Accepts either (email, password) or ({ email, password }) for backward compat.
   */
  async function login(emailOrCredentials, passwordArg) {
    let email, password
    if (typeof emailOrCredentials === 'object' && emailOrCredentials !== null) {
      email = emailOrCredentials.email
      password = emailOrCredentials.password
    } else {
      email = emailOrCredentials
      password = passwordArg
    }
    const { data } = await http.post('/auth/login', { email, password })
    // data: { accessToken, userId }
    accessToken.value = data.accessToken
    user.value = data.userId ? { userId: data.userId } : null
    await fetchMe()
  }

  /** POST /auth/logout → clear local state. */
  async function logout() {
    try {
      await http.post('/auth/logout')
    } finally {
      clear()
    }
  }

  /**
   * Called once at app startup. Attempts a silent token refresh.
   * Swallows all errors so the app always mounts.
   */
  async function bootstrap() {
    try {
      await refresh()
      await fetchMe()
    } catch {
      // No valid session — stay logged out, proceed silently
    }
  }

  return {
    accessToken,
    user,
    isAuthenticated,
    isLoggedIn,
    setAuth,
    clear,
    refresh,
    fetchMe,
    setUser,
    login,
    logout,
    bootstrap,
  }
})
