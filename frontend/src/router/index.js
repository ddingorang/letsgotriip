import { createRouter, createWebHistory } from 'vue-router';

// meta.tabBar: false → 하단 탭바 숨김 (풀스크린 플로우 화면)
// meta.requiresAuth: true → 비로그인 시 /login?redirect=<path> 으로 이동
const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/search', name: 'search', component: () => import('../views/SearchView.vue') },
  { path: '/detail/:id?', name: 'detail', component: () => import('../views/DetailView.vue'), meta: { tabBar: false } },
  { path: '/payment', name: 'payment', component: () => import('../views/PaymentView.vue'), meta: { tabBar: false } },
  { path: '/confirmation', name: 'confirmation', component: () => import('../views/ConfirmationView.vue'), meta: { tabBar: false } },
  { path: '/mypage', name: 'mypage', component: () => import('../views/MyPageView.vue'), meta: { requiresAuth: true } },
  { path: '/review', name: 'review', component: () => import('../views/ReviewView.vue') },
  { path: '/ai', name: 'ai-input', component: () => import('../views/AiInputView.vue'), meta: { requiresAuth: true } },
  { path: '/ai/result', name: 'ai-result', component: () => import('../views/AiResultView.vue'), meta: { tabBar: false, requiresAuth: true } },
  { path: '/plan', name: 'plan-edit', component: () => import('../views/PlanEditView.vue'), meta: { tabBar: false, requiresAuth: true } },
  { path: '/community', name: 'community', component: () => import('../views/CommunityView.vue') },
  { path: '/companion', name: 'companion', component: () => import('../views/CompanionView.vue') },
  { path: '/badges', name: 'badges', component: () => import('../views/BadgesView.vue'), meta: { requiresAuth: true } },
  { path: '/checklist', name: 'checklist', component: () => import('../views/ChecklistView.vue'), meta: { requiresAuth: true } },
  // Auth screens — no tab bar
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { tabBar: false } },
  { path: '/signup', name: 'signup', component: () => import('../views/SignupView.vue'), meta: { tabBar: false } },
  { path: '/oauth/callback', name: 'oauth-callback', component: () => import('../views/OAuthCallbackView.vue'), meta: { tabBar: false } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

// Global auth guard
router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true;

  // Lazy import to avoid circular deps at module load time
  const { useAuthStore } = await import('../stores/auth.js');
  const auth = useAuthStore();

  if (!auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  return true;
});
