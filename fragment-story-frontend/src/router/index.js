import { createRouter, createWebHashHistory } from 'vue-router'
import { userStore } from '../stores/user'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'

// 目前只有首页（骗你的大页面）和登录页；碎片墙/我的碎片/管理后台后续加
const routes = [
  // requiresAuth：没登录访问首页会跳去登录页（登录成功后才能看到「骗你的」页面）
  { path: '/', component: HomeView, meta: { requiresAuth: true } },
  { path: '/login', component: LoginView }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 全局守卫：需要登录的页面，没 token 就送去登录页
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !userStore.token) {
    return '/login'
  }
})

export default router
