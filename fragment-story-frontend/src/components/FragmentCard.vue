<script setup>
import RoleBadge from './RoleBadge.vue'
// Task 28：统一头像组件——有头像显示图片，没头像显示昵称首字占位
import UserAvatar from './UserAvatar.vue'

defineProps({ fragment: Object, showActions: Boolean })
defineEmits(['like', 'delete', 'hide', 'unhide'])

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
      <template v-if="showActions">
        <button v-if="fragment.status === 1" class="btn small" @click="$emit('hide', fragment)">隐藏</button>
        <button v-else-if="fragment.status === 2" class="btn small" @click="$emit('unhide', fragment)">取消隐藏</button>
        <button class="btn small danger" @click="$emit('delete', fragment)">删除</button>
      </template>
    </div>
  </article>
</template>
