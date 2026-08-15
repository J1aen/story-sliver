<script setup>
import { onMounted, ref } from 'vue'
import { getUserProfile } from '../api/auth'
import FragmentCard from './FragmentCard.vue'
import RoleBadge from './RoleBadge.vue'
import UserAvatar from './UserAvatar.vue'

/**
 * 他人主页预览抽屉（v2.0 Task 20，PC 端）。
 * 干什么用：PC 点头像/昵称 → 右侧滑出抽屉，预览该用户最新几条公开碎片 + 「查看完整主页」。
 * 为什么只在 PC 用：移动端直接整页跳转（Q4 已确认），不做抽屉。
 */
const props = defineProps({
  userId: { type: Number, required: true }// 要预览的用户 id
})
// close 关闭抽屉 / go-full 跳完整主页 / like·comments·profile 交给父组件处理（复用首页逻辑）
const emit = defineEmits(['close', 'go-full', 'like', 'comments', 'profile'])
const profile = ref(null)// 用户信息 + 第一页碎片（pageSize=4）
const loading = ref(true)
const message = ref('')

onMounted(async () => {
  try {
    profile.value = await getUserProfile(props.userId, 1, 4)// 只拉最新 4 条做预览
  } catch (e) {
    message.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- 遮罩：点空白处关闭 -->
  <div class="drawer-mask" @click.self="emit('close')">
    <aside class="profile-drawer">
      <div class="drawer-head">
        <span class="drawer-title">TA 的主页</span>
        <button type="button" class="drawer-x" aria-label="关闭" @click="emit('close')">✕</button>
      </div>

      <template v-if="profile">
        <div class="drawer-prof">
          <UserAvatar :avatar="profile.avatar" :name="profile.nickname" :size="48" />
          <div class="drawer-name">{{ profile.nickname }} <RoleBadge :role="profile.role" /></div>
        </div>

        <p v-if="profile.fragments?.list?.length" class="drawer-sub">最新碎片</p>
        <FragmentCard
          v-for="f in profile.fragments?.list || []"
          :key="f.id"
          :fragment="f"
          @like="(x) => emit('like', x)"
          @comments="(x) => emit('comments', x)"
          @profile="(x) => emit('profile', x)"
        />
        <p v-if="!profile.fragments?.list?.length" class="empty">TA 还没有公开碎片</p>

        <button type="button" class="btn primary drawer-go" @click="emit('go-full', props.userId)">查看完整主页</button>
      </template>
      <p v-else-if="message" class="error">{{ message }}</p>
      <p v-else class="empty">加载中…</p>
    </aside>
  </div>
</template>

<style scoped>
/* 遮罩 + 右侧抽屉：固定定位，不影响页面布局 */
.drawer-mask { position: fixed; inset: 0; background: rgba(74, 66, 56, 0.35); z-index: 20; }
.profile-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  width: min(340px, 88vw);
  background: #fbf8f1; border-left: 1px solid #eadfc8;
  box-shadow: -8px 0 24px rgba(74, 66, 56, 0.18);
  overflow-y: auto; padding: 18px 16px;
  animation: drawer-in 0.25s ease;
}
@keyframes drawer-in { from { transform: translateX(30px); opacity: 0; } to { transform: none; opacity: 1; } }
.drawer-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.drawer-title { font-size: 15px; font-weight: bold; }
.drawer-x { border: none; background: none; color: #8d8577; font-size: 15px; cursor: pointer; font-family: inherit; }
.drawer-prof { text-align: center; margin-bottom: 14px; }
.drawer-prof .ua-char, .drawer-prof .ua-img { margin: 0 auto 8px; }
.drawer-name { font-size: 15px; font-weight: bold; }
.drawer-sub { font-size: 13px; color: #a39476; margin: 0 0 8px; }
.drawer-go { width: 100%; margin-top: 14px; }
</style>
