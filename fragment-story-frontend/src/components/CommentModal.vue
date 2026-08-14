<script setup>
import { ref, watch } from 'vue'
import { addComment, deleteComment, getComments, reportComment } from '../api/comment'
import { userStore } from '../stores/user'
import ConfirmDialog from './ConfirmDialog.vue'
import RoleBadge from './RoleBadge.vue'
import UserAvatar from './UserAvatar.vue'

/**
 * 评论弹窗组件（v2.0 Task 21）。
 * 干什么用：碎片详情弹窗——顶部显示碎片原文，下面是评论区（一页 5 条 + 查看更多），
 * 底部输入框发评论；自己的评论可删除（弹窗确认硬删除），别人的可举报（理由必填）。
 * 为什么用遮罩弹窗：和现有公告弹窗一致，固定全屏遮罩 + 居中弹窗，页面本身不滚动不跳动。
 */
const props = defineProps({
  fragment: { type: Object, required: true }// 点击 💬 的碎片（详情弹窗里展示原文）
})
const emit = defineEmits(['close', 'like'])// 关闭弹窗 / 点赞碎片（由父组件调接口并更新数据）

const comments = ref([])// 评论列表（时间正序，一页 5 条）
const pageNum = ref(0)// 当前已加载到第几页（0=还没加载）
const hasMore = ref(false)// 是否还有下一页（「查看更多」显示依据）
const loading = ref(false)// 加载中标记（防止重复请求）
const content = ref('')// 输入框内容（≤100 字）
const submitting = ref(false)// 发表中标记（按钮防连点）
const message = ref('')// 操作提示（登录提示/错误）
const deleteTarget = ref(null)// 待删除的评论（null=没有弹确认框）
const reportTarget = ref(null)// 正在填举报理由的评论 { id, reason }（null=没展开）
const reportError = ref('')// 举报输入区内的错误提示

/** 加载下一页评论（首次进入 / 点「查看更多」共用） */
async function loadMore() {
  if (loading.value) return// 正在加载就不重复发请求
  loading.value = true
  try {
    const next = pageNum.value + 1// 要请求的下一页
    const d = await getComments(props.fragment.id, next, 5)// 每页 5 条（Q1）
    comments.value.push(...d.list)// 追加到列表尾部（服务端时间正序）
    pageNum.value = next// 记录当前页（后端 PageBean 不返回 pageNum，前端自己累计，避免 undefined+1=NaN）
    hasMore.value = comments.value.length < d.total// 还有更多才显示「查看更多」
  } catch (e) {
    message.value = e.message// 拉取失败不阻塞页面，只提示
  } finally {
    loading.value = false
  }
}

// 切换碎片时重置评论列表再加载第一页（弹窗复用，避免残留上一条的评论）
watch(
  () => props.fragment.id,
  () => {
    comments.value = []
    pageNum.value = 0
    hasMore.value = false
    message.value = ''
    loadMore()
  },
  { immediate: true }
)

/** 发表评论：未登录先提示；发表成功后重拉第一页让新评论显示出来 */
async function submit() {
  if (!userStore.token) {
    message.value = '请先登录'
    return
  }
  if (!content.value.trim()) return// 空内容不发请求
  submitting.value = true
  try {
    await addComment(props.fragment.id, content.value)
    content.value = ''
    comments.value = []
    pageNum.value = 0
    await loadMore()// 重拉第一页（含刚发的）
  } catch (e) {
    message.value = e.message
  } finally {
    submitting.value = false
  }
}

/** 确认删除自己的评论（Q7 硬删除）：成功后从本地列表移除 */
async function confirmDelete() {
  try {
    await deleteComment(deleteTarget.value.id)
    comments.value = comments.value.filter((c) => c.id !== deleteTarget.value.id)
  } catch (e) {
    message.value = e.message
  }
  deleteTarget.value = null
}

/** 提交举报：理由必填（Q6），成功后收起输入区 */
async function submitReport() {
  const t = reportTarget.value
  if (!t || !t.reason.trim()) {
    reportError.value = '举报理由必填'
    return
  }
  try {
    await reportComment(t.id, t.reason)
    reportTarget.value = null
    reportError.value = ''
    message.value = '已举报，管理员会尽快处理'
  } catch (e) {
    reportError.value = e.message
  }
}
</script>

<template>
  <!-- 遮罩：点击空白处关闭（同现有公告弹窗） -->
  <div class="modal-mask" @click.self="emit('close')">
    <div class="modal comment-modal">
      <div class="comment-modal-head">
        <span class="comment-modal-title">碎片详情</span>
        <button type="button" class="comment-x" aria-label="关闭" @click="emit('close')">✕</button>
      </div>

      <!-- 碎片原文（匿名不显示头像/铭牌，规则同首页卡片） -->
      <article class="card comment-frag">
        <div class="meta">
          <UserAvatar
            v-if="fragment.isAnonymous !== 1"
            :avatar="fragment.authorAvatar"
            :name="fragment.authorName"
            :size="20"
          />
          <span class="author">{{ fragment.authorName }}</span>
          <RoleBadge v-if="fragment.authorRole" :role="fragment.authorRole" />
          <span class="time">{{ fragment.createdAt }}</span>
        </div>
        <p class="content">{{ fragment.content }}</p>
        <!-- 详情里也能点赞/取消（父组件处理接口，fragment 对象会就地更新） -->
        <div class="actions">
          <button class="like-btn" :class="{ liked: fragment.likedByMe }" @click="$emit('like', fragment)">
            {{ fragment.likedByMe ? '♥' : '♡' }} {{ fragment.likeCount }}
          </button>
        </div>
      </article>

      <p class="comment-title">评论 · {{ comments.length }}{{ hasMore ? '+' : '' }}</p>
      <p v-if="message" class="error">{{ message }}</p>

      <!-- 评论列表 -->
      <p v-if="comments.length === 0 && !loading" class="empty" style="padding: 18px 0;">
        还没有评论，来抢沙发
      </p>
      <div v-for="c in comments" :key="c.id" class="comment-row">
        <UserAvatar :avatar="c.authorAvatar" :name="c.authorName" :size="24" />
        <div class="comment-main">
          <div class="comment-top">
            <span class="comment-name">{{ c.authorName }}</span>
            <RoleBadge v-if="c.authorRole" :role="c.authorRole" />
            <span class="comment-time">{{ c.createdAt }}</span>
          </div>
          <p class="comment-text">{{ c.content }}</p>
          <!-- 举报输入区：点「举报」展开，理由必填 -->
          <div v-if="reportTarget && reportTarget.id === c.id" class="comment-report-box">
            <input v-model="reportTarget.reason" class="input" placeholder="举报理由（必填）" maxlength="255" />
            <div class="actions">
              <button class="btn small" @click="reportTarget = null; reportError = ''">取消</button>
              <button class="btn small danger" @click="submitReport">提交举报</button>
            </div>
            <p v-if="reportError" class="error">{{ reportError }}</p>
          </div>
        </div>
        <div class="comment-ops">
          <!-- 自己的评论：删除（弹窗确认，硬删除）；别人的：举报 -->
          <button v-if="c.mine" class="comment-op" @click="deleteTarget = c">删除</button>
          <button v-else class="comment-op" @click="reportTarget = { id: c.id, reason: '' }; reportError = ''">举报</button>
        </div>
      </div>

      <!-- 查看更多：还有下一页才显示 -->
      <button v-if="hasMore" class="btn comment-more" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中…' : '查看更多评论' }}
      </button>

      <!-- 发表评论：输入框和「发表」横排一行 -->
      <div class="comment-input-row">
        <input
          v-model="content"
          class="input"
          maxlength="100"
          placeholder="说点什么…（100 字以内）"
          @keyup.enter="submit"
        />
        <button class="btn primary comment-send" :disabled="submitting || !content.trim()" @click="submit">
          {{ submitting ? '发表中…' : '发表' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 删除确认（Q7：硬删除不可恢复，必须先弹窗确认） -->
  <ConfirmDialog
    :show="!!deleteTarget"
    title="删除评论"
    message="删除后无法恢复，确定删除这条评论吗？"
    confirm-text="删除"
    @confirm="confirmDelete"
    @cancel="deleteTarget = null"
  />
</template>

<style scoped>
/* 弹窗尺寸：560px 桌面详情窗（比小确认窗大，装得下评论区）；移动端自动缩到 92vw */
.comment-modal {
  width: 560px;
  max-width: 92vw;
  max-height: 86vh;
  overflow-y: auto;
}
.comment-modal-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.comment-modal-title { font-size: 15px; font-weight: bold; }
.comment-x { border: none; background: none; color: #8d8577; font-size: 15px; cursor: pointer; font-family: inherit; }
.comment-frag { margin-bottom: 12px; }
.comment-title { font-size: 13px; color: #a39476; margin: 0 0 6px; }
.comment-row { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f1ead9; }
.comment-row:last-of-type { border-bottom: none; }
.comment-main { flex: 1; min-width: 0; }
.comment-top { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }
.comment-name { font-size: 13px; font-weight: bold; color: #6d5f47; }
.comment-time { font-size: 12px; color: #b8a98b; }
.comment-text { font-size: 14px; margin: 2px 0 0; word-break: break-word; }
.comment-ops { flex: none; display: flex; flex-direction: column; gap: 4px; }
.comment-op {
  background: none; border: none; color: #a39476; font-size: 12px; cursor: pointer;
  text-decoration: underline; padding: 0; font-family: inherit;
}
.comment-report-box { margin-top: 6px; }
.comment-more { width: 100%; margin-top: 10px; }
.comment-input-row { display: flex; gap: 8px; margin-top: 12px; }
/* 覆盖全局 .input 的 width:100%：横排时输入框弹性占满剩余宽度，按钮不会被挤到下一行 */
.comment-input-row .input { flex: 1; min-width: 0; width: auto; margin-bottom: 0; }
.comment-send { width: auto; padding: 0 18px; letter-spacing: 2px; white-space: nowrap; }
</style>
