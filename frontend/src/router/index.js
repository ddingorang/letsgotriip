// Created: 2026-06-16 14:08:18
import { createRouter, createWebHistory } from 'vue-router'

// meta.tabBar: false  → global shell hides BottomNav (full-screen flows)
// meta.requiresAuth   → global guard redirects to /login?redirect=<path>

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/home' },

    // ── Main tab views ────────────────────────────────────────────────────────
    { path: '/home', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/explore', name: 'explore', component: () => import('@/views/ExploreView.vue') },

    // /plan tab — requiresAuth so unauthenticated users go to login first
    { path: '/plan', name: 'plan', component: () => import('@/views/PlanView.vue'), meta: { requiresAuth: true } },
    // Plan 동선 리포트 — full-screen, no bottom nav
    { path: '/plan/:id/report', name: 'plan-report', component: () => import('@/views/PlanReportView.vue'), meta: { tabBar: false, requiresAuth: true } },

    // ── Community ─────────────────────────────────────────────────────────────
    { path: '/community', name: 'community', component: () => import('@/views/CommunityView.vue') },
    { path: '/community/write', name: 'post-write', component: () => import('@/views/PostWriteView.vue'), meta: { tabBar: false, requiresAuth: true } },
    { path: '/community/:id', name: 'post-detail', component: () => import('@/views/PostDetailView.vue'), meta: { tabBar: false } },

    // ── Hotplace ──────────────────────────────────────────────────────────────
    { path: '/hotplace/register', name: 'hotplace-register', component: () => import('@/views/HotplaceRegisterView.vue'), meta: { tabBar: false, requiresAuth: true } },
    { path: '/hotplace/:id', name: 'hotplace-detail', component: () => import('@/views/HotplaceDetailView.vue'), meta: { tabBar: false } },

    // ── Companion ─────────────────────────────────────────────────────────────
    { path: '/companion/write', name: 'companion-write', component: () => import('@/views/CompanionWriteView.vue'), meta: { tabBar: false, requiresAuth: true } },
    { path: '/companion/:id/applicants', name: 'companion-applicants', component: () => import('@/views/CompanionApplicantsView.vue'), meta: { tabBar: false, requiresAuth: true } },
    { path: '/companion/:id', name: 'companion-detail', component: () => import('@/views/CompanionDetailView.vue'), meta: { tabBar: false } },

    // ── Chat ──────────────────────────────────────────────────────────────────
    { path: '/chat', name: 'chat-list', component: () => import('@/views/ChatRoomListView.vue'), meta: { tabBar: false } },
    { path: '/chat/:id', name: 'chat-room', component: () => import('@/views/ChatRoomView.vue'), meta: { tabBar: false } },

    // ── MyPage ────────────────────────────────────────────────────────────────
    { path: '/mypage', name: 'mypage', component: () => import('@/views/MyPageView.vue'), meta: { requiresAuth: true } },
    { path: '/notifications', name: 'notifications', component: () => import('@/views/NotificationsView.vue'), meta: { tabBar: false, requiresAuth: true } },
    { path: '/mypage/edit', name: 'profile-edit', component: () => import('@/views/ProfileEditView.vue'), meta: { tabBar: false, requiresAuth: true } },
    { path: '/mypage/challenge', name: 'challenge-detail', component: () => import('@/views/ChallengeDetailView.vue'), meta: { tabBar: false } },
    { path: '/mypage/album/:id', name: 'album-detail', component: () => import('@/views/AlbumDetailView.vue'), meta: { tabBar: false } },
    { path: '/place/:id', name: 'place-detail', component: () => import('@/views/PlaceDetailView.vue'), meta: { tabBar: false } },

    // ── Auth ──────────────────────────────────────────────────────────────────
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { tabBar: false } },
    { path: '/signup', name: 'signup', component: () => import('@/views/SignupView.vue'), meta: { tabBar: false } },
    { path: '/oauth/callback', name: 'oauth-callback', component: () => import('@/views/OAuthCallbackView.vue'), meta: { tabBar: false } },

    // ── Preference survey ─────────────────────────────────────────────────────
    { path: '/survey', name: 'survey', component: () => import('@/views/PreferenceSurveyView.vue'), meta: { tabBar: false } },

    // ── AI ────────────────────────────────────────────────────────────────────
    // Step 1: condition input → /ai/plan
    { path: '/ai/plan', name: 'ai-plan-input', component: () => import('@/views/AiPlanInputView.vue'), meta: { requiresAuth: true } },
    // Step 2: confirm & generate → /ai
    { path: '/ai', name: 'ai-input', component: () => import('@/views/AiInputView.vue'), meta: { requiresAuth: true } },
    { path: '/ai/result', name: 'ai-result', component: () => import('@/views/AiResultView.vue'), meta: { tabBar: false, requiresAuth: true } },

    // ── Payment / Confirmation ────────────────────────────────────────────────
    { path: '/payment', name: 'payment', component: () => import('@/views/PaymentView.vue'), meta: { tabBar: false } },
    { path: '/confirmation', name: 'confirmation', component: () => import('@/views/ConfirmationView.vue'), meta: { tabBar: false } },

    // ── Badges / Checklist ────────────────────────────────────────────────────
    { path: '/badges', name: 'badges', component: () => import('@/views/BadgesView.vue'), meta: { requiresAuth: true } },
    { path: '/checklist', name: 'checklist', component: () => import('@/views/ChecklistView.vue'), meta: { requiresAuth: true } },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
})

// ── Global auth guard ─────────────────────────────────────────────────────────
router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true

  // Lazy import to avoid circular deps at module load time
  const { useAuthStore } = await import('@/stores/auth.js')
  const auth = useAuthStore()

  if (!auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
