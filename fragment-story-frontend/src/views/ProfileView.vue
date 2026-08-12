<script setup>
import { onMounted, ref } from 'vue'
import { getMe, uploadAvatar } from '../api/auth'
import { getMyFragments } from '../api/fragments'
import { userStore } from '../stores/user'
import RoleBadge from '../components/RoleBadge.vue'

// 个人主页统计：全部 / 已发布 / 待审核 / 已隐藏
const stats = ref({ total: 0, published: 0, pending: 0, hidden: 0 })
const uploading = ref(false)
const message = ref('')

// 选择图片后上传：进入待审核，管理员通过后才替换头像
async function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  message.value = ''
  try {
    const data = await uploadAvatar(file)
    message.value = data.text || '头像已提交，等待管理员审核'
    // 刷新用户信息：avatarPending 会让页面显示「审核中」
    const me = await getMe()
    userStore.setUser(me)
  } catch (err) {
    message.value = err.message
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

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
      <!-- 已审核头像优先显示；没有就用昵称首字占位 -->
      <div class="avatar-wrap">
        <img v-if="userStore.user?.avatar" :src="userStore.user.avatar" class="avatar-img" alt="头像" />
        <div v-else class="avatar">{{ (userStore.user?.nickname || userStore.user?.username || '?').charAt(0) }}</div>
      </div>
      <h2>{{ userStore.user?.nickname || userStore.user?.username }}</h2>
      <p class="subtitle">@{{ userStore.user?.username }}</p>
      <!-- 个人主页身份铭牌：站长金色 / 管理员蓝色 -->
      <RoleBadge :role="userStore.user?.role" />
      <!-- 有待审核头像时提示「审核中」 -->
      <p v-if="userStore.user?.avatarPending" class="pending-tip">头像审核中…</p>
      <p v-if="message" class="error">{{ message }}</p>
      <div class="avatar-upload">
        <label class="btn small">
          {{ uploading ? '上传中…' : userStore.user?.avatar ? '更换头像' : '上传头像' }}
          <input type="file" accept="image/jpeg,image/png" style="display:none" @change="onFileChange" />
        </label>
      </div>
      <div class="profile-stats">
        <div><b>{{ stats.total }}</b><span>全部碎片</span></div>
        <div><b>{{ stats.published }}</b><span>已发布</span></div>
        <div><b>{{ stats.pending }}</b><span>待审核</span></div>
        <div><b>{{ stats.hidden }}</b><span>已隐藏</span></div>
      </div>
    </div>
  </div>
</template>
