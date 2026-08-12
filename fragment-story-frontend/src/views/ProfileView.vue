<script setup>
import { onMounted, ref } from 'vue'
import { getMyFragments } from '../api/fragments'
import { userStore } from '../stores/user'
import RoleBadge from '../components/RoleBadge.vue'

// 个人主页统计：全部 / 已发布 / 待审核 / 已隐藏
const stats = ref({ total: 0, published: 0, pending: 0, hidden: 0 })

onMounted(async () => {
  try {
    const list = await getMyFragments()
    stats.value = {
      total: list.length,
      published: list.filter((f) => f.status === 1).length,
      pending: list.filter((f) => f.status === 0).length,
      hidden: list.filter((f) => f.status === 2).length
    }
  } catch (e) {
    // 统计加载失败不阻塞页面
  }
})
</script>

<template>
  <div class="profile-page">
    <div class="profile-card">
      <div class="avatar">{{ (userStore.user?.nickname || userStore.user?.username || '?').charAt(0) }}</div>
      <h2>{{ userStore.user?.nickname || userStore.user?.username }}</h2>
      <p class="subtitle">@{{ userStore.user?.username }}</p>
      <!-- 个人主页身份铭牌：站长金色 / 管理员蓝色 -->
      <RoleBadge :role="userStore.user?.role" />
      <div class="profile-stats">
        <div><b>{{ stats.total }}</b><span>全部碎片</span></div>
        <div><b>{{ stats.published }}</b><span>已发布</span></div>
        <div><b>{{ stats.pending }}</b><span>待审核</span></div>
        <div><b>{{ stats.hidden }}</b><span>已隐藏</span></div>
      </div>
    </div>
  </div>
</template>
