<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMe } from './api/auth'
import { userStore } from './stores/user'

const router = useRouter()
function logout() {
  userStore.logout()
  router.push('/login')
}

// 页面加载时同步最新用户信息（头像审核通过/角色变更后刷新即生效）
onMounted(async () => {
  if (userStore.token) {
    try {
      const me = await getMe()
      userStore.setUser(me)
    } catch (e) {
      // token 失效：拦截器会处理跳登录
    }
  }
})
</script>

<template>
  <div class="app">
    <nav class="nav">
      <span class="brand">故事碎片墙</span>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link v-if="userStore.token" to="/profile">个人主页</router-link>
        <router-link v-if="userStore.token" to="/my">我的碎片</router-link>
        <router-link v-if="userStore.isAdmin" to="/admin">管理</router-link>
        <button v-if="userStore.token" class="link" @click="logout">退出</button>
        <router-link v-else to="/login">登录</router-link>
      </div>
    </nav>
    <router-view />
  </div>
</template>
