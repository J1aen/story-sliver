<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  adminDeleteFragment,
  approveAvatar,
  approveFragment,
  getAdminFragments,
  getPendingAvatars,
  getUsers,
  rejectAvatar,
  banUser,
  unbanUser,
  updateAdminCode,
  updateUserRole
} from '../api/admin'
import { userStore } from '../stores/user'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const tab = ref('review')
const fragments = ref([])
const avatars = ref([])
const users = ref([])
const newCode = ref('')
const rejectReason = ref('')
const rejectingId = ref(null)// 当前正在填拒绝原因的用户 id（null=没有展开）
const banTarget = ref(null)// 当前封禁弹窗的目标：{ type: 'fragment'|'user', id, userId, name }
const banPermanent = ref(false)
const banDays = ref('')
const banReason = ref('')
const message = ref('')
const confirmTarget = ref(null)
const userStatusFilter = ref('all')// 用户筛选：all 全部 / normal 正常 / temp 暂时封禁 / perm 永久封禁

// 按状态筛选用户（保持服务端分页数据，客户端过滤）
const filteredUsers = computed(() => {
  if (userStatusFilter.value === 'all') return users.value
  if (userStatusFilter.value === 'normal') return users.value.filter((u) => u.status === 0)
  if (userStatusFilter.value === 'temp') return users.value.filter((u) => u.status === 1 && u.banExpiresAt)
  return users.value.filter((u) => u.status === 1 && !u.banExpiresAt)// 永久封禁
})

async function loadFragments(status) {
  try { const d = await getAdminFragments(status, 1, 50); fragments.value = d.list }
  catch (e) { message.value = e.message }
}
async function loadAvatars() {
  try { const d = await getPendingAvatars(1, 50); avatars.value = d.list }
  catch (e) { message.value = e.message }
}
async function loadUsers() {
  try { const d = await getUsers(1, 100); users.value = d.list }
  catch (e) { message.value = e.message }
}

async function onApprove(f) {
  try { await approveFragment(f.id); await loadFragments(tab.value === 'review' ? 0 : 1) }
  catch (e) { message.value = e.message }
}
async function onAvatarApprove(u) {
  try { await approveAvatar(u.id); await loadAvatars() } catch (e) { message.value = e.message }
}
async function onAvatarReject(u) {
  try {
    await rejectAvatar(u.id, rejectReason.value.trim() || null)
    const reason = rejectReason.value.trim() || '头像不符合要求'
    message.value = `已拒绝 ${u.nickname || u.username}（原因：${reason}）`
    rejectingId.value = null
    rejectReason.value = ''
    await loadAvatars()
  } catch (e) { message.value = e.message }
}

// 点击「拒绝」：展开原因输入框
function startReject(u) {
  rejectingId.value = u.id
  rejectReason.value = ''
}

// 点击「取消」：收起输入框，回到通过/拒绝
function cancelReject() {
  rejectingId.value = null
  rejectReason.value = ''
}
async function onRole(u, role) {
  try { await updateUserRole(u.id, role); await loadUsers() } catch (e) { message.value = e.message }
}
async function onSaveCode() {
  try { await updateAdminCode(newCode.value); message.value = '已更新'; newCode.value = '' }
  catch (e) { message.value = e.message }
}
async function onConfirmDelete() {
  try {
    await adminDeleteFragment(confirmTarget.value.id)
    confirmTarget.value = null
    await loadFragments(tab.value === 'review' ? 0 : 1)
  } catch (e) { message.value = e.message }
}

async function onUnbanUser(u) {
  try { await unbanUser(u.id); message.value = `已解封 ${u.nickname || u.username}`; await loadUsers() }
  catch (e) { message.value = e.message }
}

// 打开封禁弹窗（删除并封禁 / 用户管理封禁共用）
function openBan(target) {
  banTarget.value = target
  banPermanent.value = false
  banDays.value = ''
  banReason.value = ''
}

function cancelBan() {
  banTarget.value = null
}

async function submitBan() {
  const t = banTarget.value
  const reason = banReason.value.trim()
  if (!reason) {
    message.value = '封禁理由不能为空'
    return
  }
  let days = null
  if (!banPermanent.value) {
    days = parseInt(banDays.value, 10)
    if (!days || days <= 0) {
      message.value = '请填写封禁天数（至少 1 天），或勾选永久封禁'
      return
    }
  }
  try {
    if (t.type === 'fragment') {
      await adminDeleteFragment(t.id)
      await banUser(t.userId, days, reason)
      message.value = `已删除碎片并封禁 ${t.name || '该用户'}` + (banPermanent.value ? '（永久）' : `（${days} 天）`) + `，理由：${reason}`
      await loadFragments(tab.value === 'review' ? 0 : 1)
    } else {
      await banUser(t.id, days, reason)
      message.value = `已封禁 ${t.name || '该用户'}` + (banPermanent.value ? '（永久）' : `（${days} 天）`) + `，理由：${reason}`
      await loadUsers()
    }
    cancelBan()
  } catch (e) { message.value = e.message }
}

function switchTab(t) {
  tab.value = t
  if (t === 'review') loadFragments(0)
  else if (t === 'published') loadFragments(1)
  else if (t === 'avatars') loadAvatars()
  else if (t === 'users') loadUsers()
}

onMounted(() => loadFragments(0))
</script>

<template>
  <div class="page">
    <h1>管理后台</h1>
    <nav class="tabs">
      <button :class="{ active: tab === 'review' }" @click="switchTab('review')">待审核</button>
      <button :class="{ active: tab === 'published' }" @click="switchTab('published')">已发布</button>
      <button :class="{ active: tab === 'avatars' }" @click="switchTab('avatars')">头像审核</button>
      <button v-if="userStore.isOwner" :class="{ active: tab === 'users' }" @click="switchTab('users')">用户</button>
      <button v-if="userStore.isOwner" :class="{ active: tab === 'config' }" @click="tab = 'config'">配置</button>
    </nav>
    <p v-if="message" class="toast">{{ message }}</p>

    <!-- 碎片审核 / 已发布列表 -->
    <template v-if="tab === 'review' || tab === 'published'">
      <p v-if="fragments.length === 0" class="empty">没有碎片</p>
      <article v-for="f in fragments" :key="f.id" class="card">
        <div class="meta">
          <img v-if="f.authorAvatar" :src="f.authorAvatar" class="author-avatar" alt="" />
          <span class="author">{{ f.authorName }}</span>
          <span class="time">{{ f.createdAt }}</span>
          <span class="tag">{{ f.status === 0 ? '待审核' : '已发布' }}</span>
        </div>
        <p class="content">{{ f.content }}</p>
        <div class="actions">
          <button v-if="f.status === 0" class="btn small primary" @click="onApprove(f)">通过</button>
          <button class="btn small danger" @click="confirmTarget = { id: f.id }">删除</button>
          <button class="btn small danger" @click="openBan({ type: 'fragment', id: f.id, userId: f.userId, name: f.authorName })">删除并封禁</button>
        </div>
      </article>
    </template>

    <!-- 头像审核队列 -->
    <template v-if="tab === 'avatars'">
      <p v-if="avatars.length === 0" class="empty">没有待审核头像</p>
      <article v-for="u in avatars" :key="u.id" class="card">
        <div class="meta">
          <img :src="u.avatarPending" class="author-avatar large" alt="待审核头像" />
          <span class="author">{{ u.nickname || u.username }}</span>
          <span class="tag pending">待审核</span>
        </div>
        <div class="actions">
          <template v-if="rejectingId !== u.id">
            <button class="btn small primary" @click="onAvatarApprove(u)">通过</button>
            <button class="btn small danger" @click="startReject(u)">拒绝</button>
          </template>
        </div>
        <!-- 点击拒绝后展开的输入区（带展开动画） -->
        <div v-if="rejectingId === u.id" class="reject-panel">
          <input v-model="rejectReason" class="input" placeholder="拒绝原因（不填默认：头像不符合要求）" />
          <div class="actions">
            <button class="btn small" @click="cancelReject">取消</button>
            <button class="btn small danger" @click="onAvatarReject(u)">提交</button>
          </div>
        </div>
      </article>
    </template>

    <!-- 用户管理（仅站长） -->
    <template v-if="tab === 'users'">
      <nav class="tabs user-filters">
        <button :class="{ active: userStatusFilter === 'all' }" @click="userStatusFilter = 'all'">全部</button>
        <button :class="{ active: userStatusFilter === 'normal' }" @click="userStatusFilter = 'normal'">正常</button>
        <button :class="{ active: userStatusFilter === 'temp' }" @click="userStatusFilter = 'temp'">暂时封禁</button>
        <button :class="{ active: userStatusFilter === 'perm' }" @click="userStatusFilter = 'perm'">永久封禁</button>
      </nav>
      <table class="table">
        <tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>状态</th><th>操作</th></tr>
        <tr v-for="u in filteredUsers" :key="u.id">
          <td>{{ u.id }}</td><td>{{ u.username }}</td><td>{{ u.nickname }}</td>
          <td>{{ u.role === 2 ? '站长' : u.role === 1 ? '管理员' : '普通' }}</td>
          <td>
            <span v-if="u.status === 0">正常</span>
            <span v-else-if="u.status === 1 && u.banExpiresAt" class="tag temp-ban">暂时封禁（至 {{ (u.banExpiresAt || '').slice(0, 10) }}）</span>
            <span v-else-if="u.status === 1" class="tag perm-ban">永久封禁</span>
            <div v-if="u.status === 1 && u.banReason" class="ban-reason">理由：{{ u.banReason }}</div>
          </td>
          <td>
            <div class="row-actions">
              <button v-if="u.role === 0" class="btn xs" @click="onRole(u, 1)">设为管理员</button>
              <button v-else-if="u.role === 1" class="btn xs danger" @click="onRole(u, 0)">撤销管理员</button>
              <span v-else class="row-dash">—</span>
              <button v-if="u.status === 1" class="btn xs primary" @click="onUnbanUser(u)">解封</button>
              <button v-else-if="u.role !== 2" class="btn xs danger" @click="openBan({ type: 'user', id: u.id, name: u.nickname || u.username })">封禁</button>
            </div>
          </td>
        </tr>
      </table>
    </template>

    <!-- 系统配置（仅站长） -->
    <template v-if="tab === 'config'">
      <div class="card">
        <h3>管理员注册特殊密码</h3>
        <input v-model="newCode" class="input" type="password" placeholder="新密码（至少 6 位）" />
        <button class="btn primary" @click="onSaveCode">保存</button>
      </div>
    </template>

    <ConfirmDialog
      :show="!!confirmTarget"
      title="删除碎片"
      message="删除后无法撤回，确定删除吗？"
      @cancel="confirmTarget = null"
      @confirm="onConfirmDelete"
    />

    <!-- 封禁弹窗：自定义天数或永久 -->
    <div v-if="banTarget" class="modal-mask" @click.self="cancelBan">
      <div class="modal">
        <h3>封禁{{ banTarget.type === 'fragment' ? '并删除该碎片' : '账号' }}</h3>
        <p class="subtitle">封禁 {{ banTarget.name }}{{ banTarget.type === 'fragment' ? '，并删除该碎片' : '' }}</p>
        <input v-model="banReason" class="input" placeholder="封禁理由（必填，被封禁用户可见）" />
        <label class="checkbox"><input type="checkbox" v-model="banPermanent" /> 永久封禁</label>
        <input v-if="!banPermanent" v-model="banDays" type="number" min="1" class="input" placeholder="封禁天数（至少 1 天）" />
        <div class="modal-actions">
          <button class="btn" @click="cancelBan">取消</button>
          <button class="btn danger" @click="submitBan">确认封禁</button>
        </div>
      </div>
    </div>
  </div>
</template>
