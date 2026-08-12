<script setup>
import { onMounted, reactive, watch } from 'vue'
import { getCaptcha } from '../api/auth'

const emit = defineEmits(['update'])
const state = reactive({ key: '', answer: '', img: '' })

async function refresh() {
  try {
    const data = await getCaptcha()
    state.key = data.captchaKey
    state.img = data.imageBase64
    state.answer = ''
  } catch (e) {
    // 验证码加载失败不阻塞页面，点图片可重试
  }
}

// 答案一变化就把 key + 答案告诉父组件（注册时提交）
watch(() => state.answer, (v) => emit('update', { key: state.key, answer: v }))

onMounted(refresh)

// 暴露刷新方法：注册失败时父组件调用，自动换一张新验证码（旧的一次性 key 已失效）
defineExpose({ refresh })
</script>

<template>
  <div class="captcha-row">
    <img v-if="state.img" :src="state.img" class="captcha-img" alt="验证码" title="点我刷新" @click="refresh" />
    <input v-model="state.answer" class="input" placeholder="验证码答案" required />
  </div>
</template>
