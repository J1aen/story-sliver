<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMe, updateNickname, updatePassword } from './api/auth'
import { getActiveAnnouncement } from './api/announcement'
import { userStore } from './stores/user'
import ConfirmDialog from './components/ConfirmDialog.vue'
import AnnouncementModal from './components/AnnouncementModal.vue'

const router = useRouter()
const route = useRoute()

// —— 退出确认（v1.2 Task 25）：防止误触，先弹窗再退出 ——
const showLogoutConfirm = ref(false)
function askLogout() { showLogoutConfirm.value = true }
function cancelLogout() { showLogoutConfirm.value = false }
function confirmLogout() {
  showLogoutConfirm.value = false
  userStore.logout()
  router.push('/login')
}

// —— 设置下拉（v1.2 Task 26）：修改昵称 / 修改密码 ——
const showNicknameDlg = ref(false)
const nicknameInput = ref('')
const showPasswordDlg = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '' })
const settingsMsg = ref('')
const settingsErr = ref('')

function openNickname() {
  nicknameInput.value = userStore.user?.nickname || ''
  settingsErr.value = ''
  showNicknameDlg.value = true
}
async function saveNickname() {
  settingsErr.value = ''
  try {
    await updateNickname(nicknameInput.value)
    // 成功后同步本地用户信息（个人主页/卡片即时显示新昵称）
    userStore.user.nickname = nicknameInput.value.trim()
    userStore.persist()
    showNicknameDlg.value = false
    settingsMsg.value = '昵称已更新'
    setTimeout(() => (settingsMsg.value = ''), 2500)
  } catch (e) { settingsErr.value = e.message }
}
function openPassword() {
  pwdForm.value = { oldPassword: '', newPassword: '' }
  settingsErr.value = ''
  showPasswordDlg.value = true
}
async function savePassword() {
  settingsErr.value = ''
  try {
    await updatePassword(pwdForm.value)
    showPasswordDlg.value = false
    // v1.2 已确认：改完密码强制重新登录（旧 token 失效）
    userStore.logout()
    router.push('/login')
  } catch (e) { settingsErr.value = e.message }
}

// —— 公告（v1.2 Task 30）：进首页检查一次；展示即记入 sessionStorage，刷新不重弹；
//    登录/注册进入首页前由 LoginView 清除标记，保证「登录/注册后首次进首页」重新弹 ——
const announcement = ref(null)
const showAnn = ref(false)
async function checkAnnouncement() {
  try {
    const d = await getActiveAnnouncement()
    if (d && d.id && sessionStorage.getItem('announcement-seen') !== String(d.id)) {
      announcement.value = d
      // 展示即记为「本会话已看过」：之前只在点 ✕ 时才记录，点「我知道了」后刷新会重复弹（bug 修复）
      sessionStorage.setItem('announcement-seen', String(d.id))
      showAnn.value = true
    }
  } catch (e) {
    // 公告拉取失败不阻塞页面
  }
}
function onAnnClose() {
  // 已读标记在展示时已写入，这里只负责隐藏
  showAnn.value = false
}

// 进入首页时检查公告（含登录/注册后跳回首页；immediate 让首次进入也生效）
watch(
  () => route.path,
  (p) => { if (p === '/') checkAnnouncement() },
  { immediate: true }
)

// 页面加载时同步最新用户信息（头像/角色变更后刷新即生效）
onMounted(async () => {
  if (userStore.token) {
    try {
      const me = await getMe()
      userStore.setUser(me)
    } catch (e) {
      // token 失效：拦截器会处理跳登录
    }
  }
})
</script>

<template>
  <div class="app">
    <nav class="nav">
      <span class="brand">故事碎片墙</span>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link v-if="userStore.token" to="/profile">个人主页</router-link>
        <router-link v-if="userStore.token" to="/my">我的碎片</router-link>
        <router-link v-if="userStore.isAdmin" to="/admin">管理</router-link>
        <!-- v1.2 Task 26：设置下拉（退出左边），鼠标悬停弹出小菜单 -->
        <div v-if="userStore.token" class="settings">
          <button class="link">设置 ▾</button>
          <div class="settings-menu">
            <button class="link" @click="openNickname">修改昵称</button>
            <button class="link" @click="openPassword">修改密码</button>
          </div>
        </div>
        <button v-if="userStore.token" class="link" @click="askLogout">退出</button>
        <router-link v-else to="/login">登录</router-link>
      </div>
    </nav>
    <!-- 公告（v1.2 Task 30）：B 状态公告栏放在导航下方，不遮挡导航 -->
    <AnnouncementModal v-if="showAnn && announcement" :announcement="announcement" @close="onAnnClose" />
    <p v-if="settingsMsg" class="toast">{{ settingsMsg }}</p>
    <router-view />

    <!-- 退出确认弹窗 -->
    <ConfirmDialog
      :show="showLogoutConfirm"
      title="退出登录"
      message="确定要退出登录吗？"
      confirm-text="退出"
      @confirm="confirmLogout"
      @cancel="cancelLogout"
    />

    <!-- 修改昵称弹窗 -->
    <div v-if="showNicknameDlg" class="modal-mask" @click.self="showNicknameDlg = false">
      <div class="modal small">
        <h3>修改昵称</h3>
        <input v-model="nicknameInput" class="input" maxlength="32" placeholder="新昵称（唯一、不能含敏感词）" />
        <p v-if="settingsErr" class="error">{{ settingsErr }}</p>
        <div class="modal-actions">
          <button class="btn" @click="showNicknameDlg = false">取消</button>
          <button class="btn primary" @click="saveNickname">保存</button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordDlg" class="modal-mask" @click.self="showPasswordDlg = false">
      <div class="modal small">
        <h3>修改密码</h3>
        <input v-model="pwdForm.oldPassword" type="password" class="input" placeholder="旧密码" autocomplete="current-password" />
        <input v-model="pwdForm.newPassword" type="password" class="input" placeholder="新密码（至少 6 位）" autocomplete="new-password" />
        <p v-if="settingsErr" class="error">{{ settingsErr }}</p>
        <div class="modal-actions">
          <button class="btn" @click="showPasswordDlg = false">取消</button>
          <button class="btn primary" @click="savePassword">保存</button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* 设置下拉：悬停展开（v1.2 Task 26） */
.settings { position: relative; }
.settings-menu {
  display: none;
  position: absolute;
  right: 0;
  top: 100%;
  background: #fffdf7;
  border: 1px solid #e7dfcf;
  border-radius: 10px;
  padding: 6px;
  min-width: 110px;
  z-index: 50;
  box-shadow: 0 8px 20px rgba(60, 50, 30, 0.12);
}
.settings:hover .settings-menu,
.settings:focus-within .settings-menu { display: block; }
.settings-menu .link {
  display: block;
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
}
.settings-menu .link:hover { background: #f3ecdd; }
</style>
