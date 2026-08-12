<script setup>
import { onMounted, ref } from 'vue'
import {
  adminDeleteFragment,
  approveAvatar,
  approveFragment,
  getAdminFragments,
  getPendingAvatars,
  getUsers,
  rejectAvatar,
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
const message = ref('')
const confirmTarget = ref(null)

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
  try { await rejectAvatar(u.id); await loadAvatars() } catch (e) { message.value = e.message }
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
          <button class="btn small primary" @click="onAvatarApprove(u)">通过</button>
          <button class="btn small danger" @click="onAvatarReject(u)">拒绝</button>
        </div>
      </article>
    </template>

    <!-- 用户管理（仅站长） -->
    <template v-if="tab === 'users'">
      <table class="table">
        <tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>操作</th></tr>
        <tr v-for="u in users" :key="u.id">
          <td>{{ u.id }}</td><td>{{ u.username }}</td><td>{{ u.nickname }}</td>
          <td>{{ u.role === 2 ? '站长' : u.role === 1 ? '管理员' : '普通' }}</td>
          <td>
            <button v-if="u.role === 0" class="btn small" @click="onRole(u, 1)">设为管理员</button>
            <button v-else-if="u.role === 1" class="btn small danger" @click="onRole(u, 0)">撤销管理员</button>
            <span v-else>—</span>
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
  </div>
</template>
