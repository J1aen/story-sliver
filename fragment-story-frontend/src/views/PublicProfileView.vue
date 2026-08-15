<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserProfile } from '../api/auth'
import { likeFragment, unlikeFragment } from '../api/fragments'
import { userStore } from '../stores/user'
import CommentModal from '../components/CommentModal.vue'
import FragmentCard from '../components/FragmentCard.vue'
import RoleBadge from '../components/RoleBadge.vue'
import UserAvatar from '../components/UserAvatar.vue'

/**
 * 他人公开主页（v2.0 Task 20）。
 * 干什么用：显示某用户公开信息（昵称/头像/铭牌）+ 他「非匿名且已发布」的碎片瀑布流，
 * 支持点赞、看评论、加载更多；游客也能看。
 */
const route = useRoute()
const router = useRouter()
const userId = ref(Number(route.params.userId))// 路由里的用户 id
const profile = ref(null)// 用户公开信息（第一页返回）
const fragments = ref([])// 碎片列表
const pageNum = ref(0)// 已加载页码（后端 PageBean 不返回 pageNum，自己累计）
const hasMore = ref(false)// 是否还有下一页
const loading = ref(false)// 加载中
const message = ref('')// 错误提示（用户不存在等）
const commentFragment = ref(null)// 正在查看评论的碎片
const avatarSize = ref(window.innerWidth < 560 ? 60 : 72)// 头像大小：手机 60，PC 72（按批准的可视化）

/** 加载下一页（首次进入 / 加载更多共用） */
async function loadMore() {
  if (loading.value) return
  loading.value = true
  try {
    const next = pageNum.value + 1
    const d = await getUserProfile(userId.value, next, 9)// 每页 9 条，配 3 列瀑布流
    if (next === 1) {
      profile.value = d// 第一页顺带返回用户信息
      fragments.value = []
    }
    fragments.value.push(...(d.fragments?.list || []))// 追加本页碎片
    pageNum.value = next
    hasMore.value = fragments.value.length < (d.fragments?.total || 0)// 还有更多才显示按钮
  } catch (e) {
    message.value = e.message// 用户不存在/网络错误统一提示
  } finally {
    loading.value = false
  }
}

/** 点赞/取消（和首页一致：未登录先提示） */
async function toggleLike(f) {
  if (!userStore.token) {
    message.value = '请先登录'
    return
  }
  try {
    if (f.likedByMe) {
      await unlikeFragment(f.id)
      f.likedByMe = false
      f.likeCount--
    } else {
      await likeFragment(f.id)
      f.likedByMe = true
      f.likeCount++
    }
  } catch (e) { message.value = e.message }
}

onMounted(loadMore)
</script>

<template>
  <div class="page">
    <button class="btn" style="width:auto;margin-bottom:14px;padding:8px 16px;" @click="router.back()">← 返回</button>

    <!-- 一个大版块：上面是用户信息（居中），下面是碎片内容（按批准的可视化） -->
    <div class="pp-block">
      <div v-if="profile" class="pp-head">
        <div class="pp-ava-wrap">
          <UserAvatar :avatar="profile.avatar" :name="profile.nickname" :size="avatarSize" />
        </div>
        <h2>{{ profile.nickname }} <RoleBadge :role="profile.role" /></h2>
        <p class="pp-at">@{{ profile.nickname }}</p>
      </div>

      <p v-if="message" class="error">{{ message }}</p>
      <p v-if="!loading && profile && fragments.length === 0" class="empty">TA 还没有公开碎片</p>

      <!-- 只展示「非匿名且已发布」的碎片（后端已过滤） -->
      <div class="fragment-masonry pp-masonry">
        <FragmentCard
          v-for="f in fragments"
          :key="f.id"
          :fragment="f"
          @like="toggleLike"
          @comments="commentFragment = f"
          @profile="(x) => x.authorUserId && router.replace(`/profile/${x.authorUserId}`)"
        />
      </div>

      <button v-if="hasMore" class="btn pp-more" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中…' : '加载更多碎片' }}
      </button>
    </div>

    <CommentModal v-if="commentFragment" :fragment="commentFragment" @like="toggleLike" @close="commentFragment = null" />
  </div>
</template>

<style scoped>
/* 一个大版块：米白底 + 边框 + 圆角，头像信息在顶、碎片在下（按批准的可视化） */
.pp-block {
  background: #fbf8f1;
  border: 1px solid #eadfc8;
  border-radius: 12px;
  padding: 28px 20px 20px;
  box-shadow: 0 2px 8px rgba(74, 66, 56, 0.06);
}
.pp-head { text-align: center; padding: 0 0 18px; border-bottom: 1px dashed #e5dcc8; }
.pp-ava-wrap { display: flex; justify-content: center; margin-bottom: 10px; }
.pp-head h2 { margin: 0 0 2px; font-size: 20px; }
.pp-at { color: #a39476; font-size: 14px; margin: 0; }
.pp-masonry { padding-top: 18px; }
.pp-more { display: block; margin: 18px auto 0; width: auto; padding: 9px 22px; letter-spacing: 2px; }

/* 移动端：版块收窄居中、内边距/字号缩小（区别于 PC，按批准的可视化） */
@media (max-width: 560px) {
  .pp-block { max-width: 400px; margin: 0 auto; padding: 20px 14px 16px; }
  .pp-head { padding-bottom: 14px; }
  .pp-head h2 { font-size: 18px; }
  .pp-at { font-size: 13px; }
  .pp-masonry { padding-top: 14px; }
}
</style>
