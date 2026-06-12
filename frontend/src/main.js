import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import { router } from './router/index.js';
import './assets/design.css';
import { useAuthStore } from './stores/auth.js';

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);

// 주의: vue-router는 app.use(router) 시점에 초기 내비게이션(가드 포함)을 시작한다.
// bootstrap(세션 복원)이 끝나기 전에 가드가 돌면 로그인 상태인데도 /login으로 튕긴다.
// 반드시 bootstrap → use(router) → mount 순서를 지킬 것.
const auth = useAuthStore();
auth.bootstrap().then(() => {
  app.use(router);
  app.mount('#app');
});
