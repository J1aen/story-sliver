<script setup>
import RoleBadge from './RoleBadge.vue'
// Task 28：统一头像组件——有头像显示图片，没头像显示昵称首字占位
import UserAvatar from './UserAvatar.vue'

defineProps({ fragment: Object, showActions: Boolean })
// v2.0 Task 21：新增 comments 事件——点卡片上的「💬 评论」打开详情弹窗
defineEmits(['like', 'delete', 'hide', 'unhide', 'comments'])

// 长碎片阈值：超过这个字数就独占一行（瀑布流里避免又细又长）
const LONG_THRESHOLD = 200
</script>

<template>
  <article class="card fragment-card" :class="{ wide: fragment.content.length >= LONG_THRESHOLD }">
    <div class="meta">
      <!-- Task 28：非匿名才显示头像；有已审核头像显示图片，没有则显示昵称首字占位（匿名不显示头像） -->
      <UserAvatar
        v-if="fragment.isAnonymous !== 1"
        :avatar="fragment.authorAvatar"
        :name="fragment.authorName"
        :size="20"
      />
      <span class="author">{{ fragment.authorName }}</span>
      <!-- 非匿名且作者是站长/管理员时，显示身份铭牌（匿名时 authorRole 为 null，不显示） -->
      <RoleBadge v-if="fragment.authorRole" :role="fragment.authorRole" />
      <span class="time">{{ fragment.createdAt }}</span>
      <span v-if="fragment.status === 0" class="tag pending">待审核</span>
      <span v-else-if="fragment.status === 2" class="tag hidden">已隐藏</span>
    </div>
    <p class="content">{{ fragment.content }}</p>
    <div class="actions">
      <!-- Task 22：点赞后心形变纯红色实心（♥），未点赞是空心（♡）；数量照常变化 -->
      <button class="like-btn" :class="{ liked: fragment.likedByMe }" @click="$emit('like', fragment)">
        {{ fragment.likedByMe ? '♥' : '♡' }} {{ fragment.likeCount }}
      </button>
      <!-- v2.0 Task 21：评论入口（图标+评论数），点它弹详情窗（卡片本身不跳转） -->
      <button class="cmt-btn" @click="$emit('comments', fragment)">
        <svg class="cmt-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
        </svg>
        <span class="cmt-count">{{ fragment.commentCount ?? 0 }}</span>
      </button>
      <template v-if="showActions">
        <button v-if="fragment.status === 1" class="btn small" @click="$emit('hide', fragment)">隐藏</button>
        <button v-else-if="fragment.status === 2" class="btn small" @click="$emit('unhide', fragment)">取消隐藏</button>
        <button class="btn small danger" @click="$emit('delete', fragment)">删除</button>
      </template>
    </div>
  </article>
</template>
