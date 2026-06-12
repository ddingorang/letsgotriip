import { createRouter, createWebHistory } from 'vue-router';
import { navItems } from '../data.js';

const pagePaths = {
  home: '/',
  explore: '/explore',
  plan: '/plan',
  recommend: '/recommend',
  hotplace: '/hotplace',
  community: '/community',
  mypage: '/mypage'
};

const routes = navItems.map((item) => ({
  path: pagePaths[item.key] ?? `/${item.key}`,
  name: item.key,
  component: { template: '<span />' },
  meta: { page: item.key, label: item.label }
}));

routes.push({ path: '/:pathMatch(.*)*', redirect: '/explore' });

export const routeByPage = Object.fromEntries(routes.filter((route) => route.name).map((route) => [route.name, route.path]));

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});
