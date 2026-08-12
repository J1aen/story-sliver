import axios from 'axios'
import { userStore } from '../stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 封禁提示只弹一次：封禁瞬间可能多个请求同时返回 403，防止 alert 连弹好几遍
let banAlertShown = false

// 请求拦截器：有 token 就自动带上
request.interceptors.request.use((config) => {
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：统一处理 Result 包装和 401
request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code !== 200) {
      return Promise.reject(new Error(body.text || '请求失败'))
    }
    return body.data
  },
  (err) => {
    const status = err.response?.status
    const data = err.response?.data || {}
    // 账号被封禁：弹出提示并退出登录（登录态中的封禁立即生效）
    if (status === 403 && data.text && data.text.includes('封禁')) {
      userStore.logout()
      if (!banAlertShown) {
        banAlertShown = true
        alert(data.text)
        setTimeout(() => { banAlertShown = false }, 2000)
      }
      if (!location.hash.startsWith('#/login')) location.hash = '#/login'
      return Promise.reject(new Error(data.text))
    }
    if (status === 401) {
      userStore.logout()
      if (!location.hash.startsWith('#/login')) location.hash = '#/login'
    }
    // 后端 Result 的提示字段叫 text（照 sims 风格），不是 message
    const msg = data.text || data.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default request
