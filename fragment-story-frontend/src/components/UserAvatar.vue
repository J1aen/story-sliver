<script setup>
import { computed } from 'vue'

/**
 * 用户头像组件（Task 28 / V1.1）
 * 干什么用：统一展示用户头像——有已审核头像就显示图片；
 *          没有头像（或头像还没通过审核）就显示「昵称首字」的圆形占位，和一开始个人主页的默认头像一致。
 * 为什么单独抽成组件：碎片卡片、评论、个人主页都要用，避免每处都写一遍「有没有头像」的判断。
 */
const props = defineProps({
  avatar: { type: String, default: '' }, // 已审核头像 URL（可能为空）
  name: { type: String, default: '' },   // 昵称（取首字做占位）
  size: { type: Number, default: 30 }    // 展示直径（px），调用处按位置传入，如卡片 20、主页 72
})

// 温暖色板：占位头像的背景色，取色稳定、不刺眼
const PALETTE = ['#d9cfbc', '#c9d7c5', '#d5cdb0', '#cfc4d6', '#c8d4da']

// 昵称首字符；昵称为空时用 '?' 兜底
const firstChar = computed(() => (props.name || '?').charAt(0))

// 根据昵称算一个稳定下标：同一个用户每次进来颜色都一样，不会闪变
const color = computed(() => {
  let h = 0
  for (const ch of props.name || '') {
    h = (h * 31 + ch.charCodeAt(0)) >>> 0
  }
  return PALETTE[h % PALETTE.length]
})
</script>

<template>
  <!-- 有头像：显示图片，圆形 + cover 防止变形 -->
  <img
    v-if="avatar"
    :src="avatar"
    class="ua-img"
    :style="{ width: size + 'px', height: size + 'px' }"
    alt=""
  />
  <!-- 无头像：昵称首字圆形占位，颜色按昵称哈希稳定取色 -->
  <div
    v-else
    class="ua-char"
    :style="{
      width: size + 'px',
      height: size + 'px',
      background: color,
      fontSize: Math.round(size * 0.42) + 'px'
    }"
  >
    {{ firstChar }}
  </div>
</template>

<style scoped>
/* 头像图片：圆形 + 等比裁剪，图片比例不对也不会变形 */
.ua-img {
  border-radius: 50%;
  object-fit: cover;
  display: block;
  flex: none;
}
/* 首字占位：圆形居中 */
.ua-char {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b614f;
  font-weight: 500;
  flex: none;
}
</style>
