<script setup>
import { onMounted, ref } from 'vue'
import { getMe, uploadAvatar } from '../api/auth'
import { getMyFragments } from '../api/fragments'
import { userStore } from '../stores/user'
import RoleBadge from '../components/RoleBadge.vue'
import CropModal from '../components/CropModal.vue'

// 个人主页统计：全部 / 已发布 / 待审核 / 已隐藏
const stats = ref({ total: 0, published: 0, pending: 0, hidden: 0 })
const uploading = ref(false)
const message = ref('')
const showCrop = ref(false)
const cropUrl = ref('')
// 已关闭的拒绝原因（持久化）：关闭后切页不再出现；原因变化或重新上传后会重新提醒
const dismissedReason = ref(localStorage.getItem('dismissed-avatar-reject') || '')

function dismissRejectNotice() {
  const reason = userStore.user?.avatarRejectReason || ''
  dismissedReason.value = reason
  localStorage.setItem('dismissed-avatar-reject', reason)
}

// 选择图片 → 打开圆形裁剪框
async function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  // 简单客户端校验
  if (!/image\/(jpeg|png)/.test(file.type)) {
    message.value = '头像只支持 jpg/png 格式'
    e.target.value = ''
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    message.value = '头像大小不能超过 2MB'
    e.target.value = ''
    return
  }
  cropUrl.value = URL.createObjectURL(file)
  showCrop.value = true
  e.target.value = ''
}

// 裁剪完成 → 上传（进入待审核）
async function onCropped(blob) {
  showCrop.value = false
  uploading.value = true
  message.value = ''
  try {
    const data = await uploadAvatar(blob)
    message.value = data.text || '头像已提交，等待管理员审核'
    // 重新上传后清除已关闭记录：如果这次又被拒，会重新提醒
    dismissedReason.value = ''
    localStorage.removeItem('dismissed-avatar-reject')
    const me = await getMe()
    userStore.setUser(me)
  } catch (err) {
    message.value = err.message
  } finally {
    uploading.value = false
    URL.revokeObjectURL(cropUrl.value)
  }
}

onMounted(async () => {
  try {
    // 进入个人主页就重新同步用户信息：头像被拒/通过/角色变化立即反映（SPA 切页不会自动刷新）
    const me = await getMe()
    userStore.setUser(me)
  } catch (e) {
    // token 失效交给拦截器处理
  }
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
      <!-- 头像被拒绝时显示原因（重新上传或通过后自动消失） -->
      <div
        v-if="userStore.user?.avatarRejectReason && !userStore.user?.avatarPending && dismissedReason !== userStore.user.avatarRejectReason"
        class="reject-tip"
      >
        <span>头像被拒绝：{{ userStore.user.avatarRejectReason }}</span>
        <button class="reject-dismiss" @click="dismissRejectNotice">知道了</button>
      </div>
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

    <CropModal
      :show="showCrop"
      :image-url="cropUrl"
      @cropped="onCropped"
      @cancel="showCrop = false; URL.revokeObjectURL(cropUrl)"
    />
  </div>
</template>
