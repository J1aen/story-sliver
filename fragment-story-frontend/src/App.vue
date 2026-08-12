<script setup>
import { useRouter } from 'vue-router'
import { userStore } from './stores/user'

const router = useRouter()
function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app">
    <nav class="nav">
      <span class="brand">故事碎片墙</span>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link v-if="userStore.token" to="/profile">个人主页</router-link>
        <button v-if="userStore.token" class="link" @click="logout">退出</button>
        <router-link v-else to="/login">登录</router-link>
      </div>
    </nav>
    <router-view />
  </div>
</template>
