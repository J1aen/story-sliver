import { reactive } from 'vue'

// 从 localStorage 恢复登录态：刷新页面不掉登录
const saved = JSON.parse(localStorage.getItem('story-user') || 'null')

export const userStore = reactive({
  token: saved?.token || '',
  user: saved?.user || null,

  setLogin(token, user) {
    this.token = token
    this.user = user
    this.persist()
  },
  setUser(user) {
    this.user = user
    this.persist()
  },
  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem('story-user')
  },
  persist() {
    localStorage.setItem('story-user', JSON.stringify({ token: this.token, user: this.user }))
  },

  get isAdmin() {
    return !!this.user && this.user.role >= 1
  },
  get isOwner() {
    return !!this.user && this.user.role === 2
  }
})
