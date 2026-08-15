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
  updateUserRole,
  getAnnouncements,
  createAnnouncement,
  updateAnnouncement,
  updateAnnouncementStatus,
  deleteAnnouncement,
  uploadAnnouncementImage,
  getSensitiveWords,
  addSensitiveWord,
  deleteSensitiveWord
} from '../api/admin'
import { getCommentReports, handleCommentReport } from '../api/comment'// v2.0 Task 21：评论举报
import { userStore } from '../stores/user'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const tab = ref('review')
const fragments = ref([])
const avatars = ref([])
const users = ref([])
const newCode = ref('')
const rejectReason = ref('')
const rejectingId = ref(null)// 当前正在填拒绝原因的用户 id（null=没有展开）
const banTarget = ref(null)// 当前封禁弹窗的目标：{ type: 'fragment'|'user'|'report', id, userId, name }
const banPermanent = ref(false)
const banDays = ref('')
const banReason = ref('')
const banError = ref('')// 封禁弹窗内的错误提示（避免被遮罩挡住看不到）
const message = ref('')
const confirmTarget = ref(null)
const confirmAnnTarget = ref(null)// 待删除的公告（null=没弹确认框）
const userStatusFilter = ref('all')// 用户筛选：all 全部 / normal 正常 / temp 暂时封禁 / perm 永久封禁
// —— 公告管理（v1.2，仅站长）——
const annList = ref([])
const annForm = ref({ title: '', content: '', imageUrl: '' })
const editingAnnId = ref(null)
// —— 敏感词管理（v1.2，仅站长）——
const words = ref([])
const newWord = ref('')
// —— 评论举报（v2.0 Task 21，管理员/站长）——
const reports = ref([])

async function loadAnn() {
  try { annList.value = await getAnnouncements() } catch (e) { message.value = e.message }
}
function editAnn(a) {
  editingAnnId.value = a.id
  annForm.value = { title: a.title, content: a.content, imageUrl: a.imageUrl || '' }
}
async function saveAnn() {
  try {
    if (editingAnnId.value) await updateAnnouncement(editingAnnId.value, annForm.value)
    else await createAnnouncement(annForm.value)
    editingAnnId.value = null
    annForm.value = { title: '', content: '', imageUrl: '' }
    message.value = '公告已保存（新建默认下架，需手动上架）'
    await loadAnn()
  } catch (e) { message.value = e.message }
}
async function toggleAnnStatus(a) {
  try { await updateAnnouncementStatus(a.id, a.status === 1 ? 0 : 1); await loadAnn() }
  catch (e) { message.value = e.message }
}
// 删除公告：硬删除（记录 + 图片文件一起删，不可恢复），先弹窗确认
async function confirmAnnDelete() {
  try {
    await deleteAnnouncement(confirmAnnTarget.value.id)
    confirmAnnTarget.value = null
    await loadAnn()
  } catch (e) { message.value = e.message }
}
async function onAnnImage(e) {
  const file = e.target.files[0]
  e.target.value = ''
  if (!file) return
  try {
    const d = await uploadAnnouncementImage(file)
    annForm.value.imageUrl = d.url// 回填图片地址到表单，保存公告时一起提交
  } catch (err) { message.value = err.message }
}

async function loadWords() {
  try { words.value = await getSensitiveWords() } catch (e) { message.value = e.message }
}
async function addWord() {
  try { await addSensitiveWord(newWord.value); newWord.value = ''; await loadWords() }
  catch (e) { message.value = e.message }
}
async function delWord(w) {
  try { await deleteSensitiveWord(w.id); await loadWords() } catch (e) { message.value = e.message }
}

// —— 评论举报（v2.0 Task 21）——
async function loadReports() {
  try { reports.value = await getCommentReports() } catch (e) { message.value = e.message }
}
// 处理举报：dismiss 不下架 / delete 下架评论 / ban 下架并封禁评论用户
async function handleReport(r, action, banDays, banReason) {
  try {
    await handleCommentReport(r.id, action, banDays ?? null, banReason ?? null)
    await loadReports()
  } catch (e) { message.value = e.message }
}
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
  banError.value = ''
}

function cancelBan() {
  banTarget.value = null
}

async function submitBan() {
  const t = banTarget.value
  const reason = banReason.value.trim()
  if (!reason) {
    banError.value = '封禁理由不能为空'
    return
  }
  let days = null
  if (!banPermanent.value) {
    days = parseInt(banDays.value, 10)
    if (!days || days <= 0) {
      banError.value = '请填写封禁天数（至少 1 天），或勾选永久封禁'
      return
    }
  }
  banError.value = ''
  try {
    if (t.type === 'fragment') {
      await adminDeleteFragment(t.id)
      await banUser(t.userId, days, reason)
      message.value = `已删除碎片并封禁 ${t.name || '该用户'}` + (banPermanent.value ? '（永久）' : `（${days} 天）`) + `，理由：${reason}`
      await loadFragments(tab.value === 'review' ? 0 : 1)
    } else if (t.type === 'report') {
      // 评论举报的「下架并封禁」：后端一次完成 下架评论 + 封禁评论用户
      await handleCommentReport(t.reportId, 'ban', days, reason)
      message.value = `已下架评论并封禁 ${t.name || '该用户'}` + (banPermanent.value ? '（永久）' : `（${days} 天）`) + `，理由：${reason}`
      await loadReports()
    } else {
      await banUser(t.id, days, reason)
      message.value = `已封禁 ${t.name || '该用户'}` + (banPermanent.value ? '（永久）' : `（${days} 天）`) + `，理由：${reason}`
      await loadUsers()
    }
    cancelBan()
  } catch (e) { banError.value = e.message }
}

function switchTab(t) {
  tab.value = t
  if (t === 'review') loadFragments(0)
  else if (t === 'published') loadFragments(1)
  else if (t === 'avatars') loadAvatars()
  else if (t === 'users') loadUsers()
  else if (t === 'ann') loadAnn()
  else if (t === 'words') loadWords()
  else if (t === 'reports') loadReports()
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
      <!-- v1.2 Task 30：公告与敏感词管理，仅站长可见 -->
      <button v-if="userStore.isOwner" :class="{ active: tab === 'ann' }" @click="switchTab('ann')">公告</button>
      <button v-if="userStore.isOwner" :class="{ active: tab === 'words' }" @click="switchTab('words')">敏感词</button>
      <!-- v2.0 Task 21：评论举报（管理员和站长都能看） -->
      <button v-if="userStore.isAdmin" :class="{ active: tab === 'reports' }" @click="switchTab('reports')">评论举报</button>
    </nav>
    <p v-if="message" class="toast">{{ message }}</p>

    <!-- 公告管理（v1.2，仅站长） -->
    <template v-if="tab === 'ann'">
      <div class="card">
        <h3>{{ editingAnnId ? '编辑公告' : '新建公告' }}</h3>
        <input v-model="annForm.title" class="input" maxlength="50" placeholder="公告标题（≤50字）" />
        <textarea v-model="annForm.content" class="input" rows="4" maxlength="2000" placeholder="公告正文（≤2000字）"></textarea>
        <div class="ann-image-row">
          <label class="btn small">
            {{ annForm.imageUrl ? '更换图片' : '上传图片' }}
            <input type="file" accept="image/*" style="display:none" @change="onAnnImage" />
          </label>
          <img v-if="annForm.imageUrl" :src="annForm.imageUrl" class="ann-preview" alt="预览" />
          <button v-if="annForm.imageUrl" class="btn small" @click="annForm.imageUrl = ''">移除图片</button>
        </div>
        <div class="actions">
          <button v-if="editingAnnId" class="btn small" @click="editingAnnId = null; annForm = { title: '', content: '', imageUrl: '' }">取消编辑</button>
          <button class="btn small primary" @click="saveAnn">{{ editingAnnId ? '保存修改' : '创建（默认下架）' }}</button>
        </div>
      </div>
      <p v-if="annList.length === 0" class="empty">还没有公告</p>
      <article v-for="a in annList" :key="a.id" class="card">
        <div class="meta">
          <span class="author">{{ a.title }}</span>
          <span class="tag">{{ a.status === 1 ? '已上架' : '已下架' }}</span>
          <span class="time">{{ a.createdAt }}</span>
        </div>
        <p class="content">{{ a.content }}</p>
        <img v-if="a.imageUrl" :src="a.imageUrl" class="ann-preview" alt="公告图片" />
        <div class="actions">
          <button class="btn small" @click="editAnn(a)">编辑</button>
          <button class="btn small primary" @click="toggleAnnStatus(a)">{{ a.status === 1 ? '下架' : '上架' }}</button>
          <button class="btn small danger" @click="confirmAnnTarget = a">删除</button>
        </div>
      </article>
    </template>

    <!-- 敏感词管理（v1.2，仅站长） -->
    <template v-if="tab === 'words'">
      <div class="card">
        <input v-model="newWord" class="input" maxlength="50" placeholder="输入新敏感词" style="max-width:240px" />
        <button class="btn small primary" @click="addWord">添加</button>
      </div>
      <p v-if="words.length === 0" class="empty">还没有敏感词</p>
      <div class="word-list">
        <span v-for="w in words" :key="w.id" class="word-chip">
          {{ w.word }}
          <button class="word-x" @click="delWord(w)">✕</button>
        </span>
      </div>
    </template>

    <!-- 评论举报（v2.0 Task 21） -->
    <template v-if="tab === 'reports'">
      <p v-if="reports.length === 0" class="empty">没有待处理举报</p>
      <article v-for="r in reports" :key="r.id" class="card">
        <div class="meta">
          <span class="author">{{ r.commenterName }}</span>
          <span class="tag pending">待处理</span>
          <span class="time">举报 #{{ r.id }}</span>
        </div>
        <p class="content">评论：{{ r.commentContent }}</p>
        <p class="ban-reason">举报理由：{{ r.reason }}</p>
        <div class="actions">
          <button class="btn small" @click="handleReport(r, 'dismiss')">不下架</button>
          <button class="btn small danger" @click="handleReport(r, 'delete')">下架评论</button>
          <button class="btn small danger" @click="openBan({ type: 'report', reportId: r.id, userId: r.commenterId, name: r.commenterName })">下架并封禁</button>
        </div>
      </article>
    </template>

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
          <!-- 管理员不能封禁管理员：仅站长可对管理员碎片执行删除并封禁 -->
          <button
            v-if="!(userStore.user?.role === 1 && f.authorRole === 1)"
            class="btn small danger"
            @click="openBan({ type: 'fragment', id: f.id, userId: f.userId, name: f.authorName })"
          >删除并封禁</button>
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
    <!-- 删除公告：硬删除（含图片文件），不可恢复，先确认 -->
    <ConfirmDialog
      :show="!!confirmAnnTarget"
      title="删除公告"
      message="删除后无法恢复，公告图片也会一起删除，确定删除吗？"
      confirm-text="删除"
      @cancel="confirmAnnTarget = null"
      @confirm="confirmAnnDelete"
    />

    <!-- 封禁弹窗：自定义天数或永久 -->
    <div v-if="banTarget" class="modal-mask" @click.self="cancelBan">
      <div class="modal">
        <h3>封禁{{ banTarget.type === 'fragment' ? '并删除该碎片' : banTarget.type === 'report' ? '并下架评论' : '账号' }}</h3>
        <p class="subtitle">封禁 {{ banTarget.name }}{{ banTarget.type === 'fragment' ? '，并删除该碎片' : banTarget.type === 'report' ? '，并下架其评论' : '' }}</p>
        <input v-model="banReason" class="input" placeholder="封禁理由（必填，被封禁用户可见）" />
        <label class="checkbox"><input type="checkbox" v-model="banPermanent" /> 永久封禁</label>
        <input v-if="!banPermanent" v-model="banDays" type="number" min="1" class="input" placeholder="封禁天数（至少 1 天）" />
        <p v-if="banError" class="error">{{ banError }}</p>
        <div class="modal-actions">
          <button class="btn" @click="cancelBan">取消</button>
          <button class="btn danger" @click="submitBan">确认封禁</button>
        </div>
      </div>
    </div>
  </div>
</template>
