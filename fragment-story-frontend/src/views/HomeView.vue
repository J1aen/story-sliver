<script setup>
import { useRouter } from 'vue-router'
import { userStore } from '../stores/user'

const router = useRouter()
function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="coming-page">
    <h1 class="coming-title">骗你的，还没做完！</h1>
    <p class="coming-sub">「碎片墙」还在开发中，但<strong>登录已经打通了</strong> ✓</p>
    <p class="coming-user" v-if="userStore.user">
      当前登录：{{ userStore.user.nickname || userStore.user.username }}
    </p>
    <button class="btn" @click="logout">退出登录</button>
  </div>
</template>
