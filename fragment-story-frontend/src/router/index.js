import { createRouter, createWebHashHistory } from 'vue-router'
import { userStore } from '../stores/user'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import MyFragmentsView from '../views/MyFragmentsView.vue'
import AdminView from '../views/AdminView.vue'

// 首页是公开的碎片墙（游客可看，发布/点赞需登录）；个人主页需登录
const routes = [
  { path: '/', component: HomeView },
  { path: '/login', component: LoginView },
  { path: '/profile', component: ProfileView, meta: { requiresAuth: true } },
  { path: '/my', component: MyFragmentsView, meta: { requiresAuth: true } },
  { path: '/admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } }
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
  // 管理后台只有管理员/站长能进
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return '/'
  }
})

export default router
