<script setup>
import { ref } from 'vue'

/**
 * 公告组件（v1.2 Task 30）
 * 交互：A 居中弹窗（有图/无图两种排版）→ 点「我知道了」→ B 顶部公告栏 → 点 B 重新展开 A → 点 ✕ 关闭。
 * 为什么做双状态：既要「进站醒目提示」（A），又不打断后续浏览（B），两者可来回切换。
 */
const props = defineProps({ announcement: { type: Object, required: true } })
const emit = defineEmits(['close'])
const mode = ref('modal')// modal=弹窗 bar=公告栏 closed=已关闭

// A → B：收起为顶部公告栏
function known() { mode.value = 'bar' }
// B → A：点公告栏重新展开弹窗
function openModal() { mode.value = 'modal' }
// 关闭并通知父组件记录「已读」（sessionStorage），刷新不再弹
function close() { mode.value = 'closed'; emit('close') }
</script>

<template>
  <div v-if="mode !== 'closed'" class="ann-root">
    <!-- 状态 A：居中弹窗 -->
    <template v-if="mode === 'modal'">
      <div class="modal-mask" @click.self="known">
        <div class="modal ann-modal">
          <img v-if="announcement.imageUrl" :src="announcement.imageUrl" class="ann-img" alt="公告图片" />
          <div class="ann-body" :class="{ noimg: !announcement.imageUrl }">
            <h3>{{ announcement.title }}</h3>
            <p class="ann-content">{{ announcement.content }}</p>
            <button class="btn primary" @click="known">我知道了</button>
          </div>
        </div>
      </div>
    </template>
    <!-- 状态 B：导航栏下方的滚动公告栏（不遮挡导航） -->
    <div v-else class="ann-bar" @click="openModal">
      <span class="ann-tag">公告</span>
      <span class="ann-track" aria-hidden="true">
        <span class="ann-scroll">{{ announcement.title }}：{{ announcement.content }}　·　{{ announcement.title }}：{{ announcement.content }}</span>
      </span>
      <span class="ann-x" @click.stop="close">✕</span>
    </div>
  </div>
</template>

<style scoped>
/* 弹窗：有图时图片铺满顶部；无图时紧凑居中排版 */
.ann-modal { max-width: 420px; overflow: hidden; }
.ann-img { width: 100%; max-height: 220px; object-fit: cover; display: block; }
.ann-body { padding: 16px 18px; }
.ann-body h3 { margin: 0 0 8px; font-size: 16px; }
.ann-content { margin: 0 0 14px; font-size: 13.5px; line-height: 1.7; color: #8d8577; white-space: pre-wrap; }
.ann-body.noimg { text-align: center; }
.ann-body.noimg .ann-content { text-align: left; }
/* 公告栏：普通文档流，放在导航栏下方，不遮挡导航；文字横向滚动 */
.ann-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 14px; background: #fff6ec; border-bottom: 1px solid #e7dfcf;
  cursor: pointer; box-shadow: 0 4px 12px rgba(60, 50, 30, 0.12);
}
.ann-tag { font-size: 10px; color: #fff; background: #e0564f; border-radius: 999px; padding: 2px 7px; flex: none; }
.ann-track { flex: 1; overflow: hidden; position: relative; }
.ann-scroll {
  display: inline-block;
  white-space: nowrap;
  padding-left: 100%;
  font-size: 12.5px;
  color: #3b352c;
  animation: ann-scroll 50s linear infinite; /* 50 秒滚完一遍：慢速滚动，不晃眼 */
}
@keyframes ann-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(-100%); }
}
.ann-x { color: #8d8577; cursor: pointer; flex: none; padding: 0 4px; }
</style>
