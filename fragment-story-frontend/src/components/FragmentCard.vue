<script setup>
import RoleBadge from './RoleBadge.vue'

defineProps({ fragment: Object, showActions: Boolean })
defineEmits(['like', 'delete', 'hide', 'unhide'])
</script>

<template>
  <article class="card fragment-card">
    <div class="meta">
      <span class="author">{{ fragment.authorName }}</span>
      <!-- 非匿名且作者是站长/管理员时，显示身份铭牌（匿名时 authorRole 为 null，不显示） -->
      <RoleBadge v-if="fragment.authorRole" :role="fragment.authorRole" />
      <span class="time">{{ fragment.createdAt }}</span>
      <span v-if="fragment.status === 0" class="tag pending">待审核</span>
      <span v-else-if="fragment.status === 2" class="tag hidden">已隐藏</span>
    </div>
    <p class="content">{{ fragment.content }}</p>
    <div class="actions">
      <button class="like-btn" :class="{ liked: fragment.likedByMe }" @click="$emit('like', fragment)">
        ♡ {{ fragment.likeCount }}
      </button>
      <template v-if="showActions">
        <button v-if="fragment.status === 1" class="btn small" @click="$emit('hide', fragment)">隐藏</button>
        <button v-else-if="fragment.status === 2" class="btn small" @click="$emit('unhide', fragment)">取消隐藏</button>
        <button class="btn small danger" @click="$emit('delete', fragment)">删除</button>
      </template>
    </div>
  </article>
</template>
