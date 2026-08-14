<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMe, login, register } from '../api/auth'
import { userStore } from '../stores/user'
import CaptchaField from '../components/CaptchaField.vue'

const router = useRouter()
const mode = ref('login') // login | register
const form = ref({ username: '', nickname: '', password: '', isAdmin: false, adminCode: '' })
const captcha = ref({ key: '', answer: '' })
const captchaField = ref(null)// 验证码组件实例，注册失败时刷新用
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  // Task 29：注册时用户名不允许汉字——只允许字母/数字/下划线，4-20 位；先在前端拦截，减少无效请求
  if (mode.value === 'register' && !/^[A-Za-z0-9_]{4,20}$/.test(form.value.username)) {
    error.value = '用户名只能包含字母、数字和下划线（4-20位），不能有汉字'
    return
  }
  submitting.value = true
  try {
    let token
    if (mode.value === 'login') {
      // 后端返回 { token: "..." }，要取 .token 这个字符串，不能把整个对象存进去
      token = (await login({ username: form.value.username, password: form.value.password })).token
    } else {
      token = (
        await register({
          username: form.value.username,
          nickname: form.value.nickname,
          password: form.value.password,
          isAdmin: form.value.isAdmin,
          adminCode: form.value.adminCode || null,
          captchaKey: captcha.value.key,
          captchaAnswer: captcha.value.answer
        })
      ).token
    }
    userStore.setLogin(token, null)
    const me = await getMe()
    userStore.setUser(me)
    // v1.2 公告修复：登录/注册后首次进入首页要重新弹公告，清掉本会话已读标记
    sessionStorage.removeItem('announcement-seen')
    router.push('/')
  } catch (e) {
    error.value = e.message
    // 注册失败（尤其是验证码错误）：验证码一次性，失败后自动换新
    if (mode.value === 'register') {
      captchaField.value?.refresh()
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>{{ mode === 'login' ? '登录' : '注册' }}</h2>
      <p class="subtitle">匿名故事碎片墙</p>

      <div class="tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <form @submit.prevent="submit">
        <input
          v-model="form.username"
          class="input"
          :placeholder="mode === 'register' ? '用户名（字母/数字/下划线，4-20位）' : '用户名'"
          autocomplete="username"
          maxlength="20"
          required
        />
        <input v-if="mode === 'register'" v-model="form.nickname" class="input" placeholder="昵称" required />
        <input v-model="form.password" type="password" class="input" placeholder="密码（至少 6 位）" autocomplete="current-password" required />

        <template v-if="mode === 'register'">
          <CaptchaField ref="captchaField" @update="captcha = $event" />
          <label class="checkbox">
            <input type="checkbox" v-model="form.isAdmin" /> 注册为管理员
          </label>
          <input v-if="form.isAdmin" v-model="form.adminCode" type="password" class="input" placeholder="管理员注册特殊密码" />
        </template>

        <p v-if="error" class="error">{{ error }}</p>
        <button class="btn primary" type="submit" :disabled="submitting">
          {{ submitting ? '请稍候…' : mode === 'login' ? '登 录' : '注 册' }}
        </button>
      </form>
    </div>
  </div>
</template>
