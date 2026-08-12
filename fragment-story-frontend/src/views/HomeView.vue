<script setup>
import { onMounted, ref } from 'vue'
import { getFragments, likeFragment, submitFragment, unlikeFragment } from '../api/fragments'
import { userStore } from '../stores/user'
import FragmentCard from '../components/FragmentCard.vue'

const fragments = ref([])
const pageNum = ref(0)
const pageSize = 10
const hasMore = ref(true)
const loading = ref(false)
const showSubmit = ref(false)
const content = ref('')
const isAnonymous = ref(false)
const submitting = ref(false)
const message = ref('')
let timer

function notice(text) {
  message.value = text
  clearTimeout(timer)
  timer = setTimeout(() => (message.value = ''), 3000)
}

// 加载下一页（「加载更多」按钮触发）
async function loadMore() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const data = await getFragments(pageNum.value + 1, pageSize)
    fragments.value.push(...data.list)
    pageNum.value = data.pageNum
    hasMore.value = fragments.value.length < data.total
  } catch (e) {
    notice(e.message)
  } finally {
    loading.value = false
  }
}

// 发布：进入待审核，管理员通过后才上墙
async function publish() {
  if (!userStore.token) {
    notice('请先登录')
    return
  }
  if (!content.value.trim()) return
  submitting.value = true
  try {
    await submitFragment(content.value, isAnonymous.value)
    notice('已提交，等待管理员审核')
    content.value = ''
    isAnonymous.value = false
    showSubmit.value = false
  } catch (e) {
    notice(e.message)
  } finally {
    submitting.value = false
  }
}

// 点赞 / 取消点赞：局部更新，不刷新整页
async function toggleLike(fragment) {
  if (!userStore.token) {
    notice('请先登录')
    return
  }
  try {
    if (fragment.likedByMe) {
      await unlikeFragment(fragment.id)
      fragment.likedByMe = false
      fragment.likeCount--
    } else {
      await likeFragment(fragment.id)
      fragment.likedByMe = true
      fragment.likeCount++
    }
  } catch (e) {
    notice(e.message)
  }
}

onMounted(loadMore)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h1>故事碎片墙</h1>
      <button class="btn primary" @click="showSubmit = !showSubmit">写一块碎片</button>
    </div>

    <p v-if="message" class="toast">{{ message }}</p>

    <!-- 发布框（点击「写一块碎片」展开） -->
    <div v-if="showSubmit" class="card submit-card">
      <textarea v-model="content" class="textarea" maxlength="1000" rows="4" placeholder="一小段故事，或今天的一点生活…"></textarea>
      <div class="count">{{ content.length }}/1000</div>
      <label class="checkbox"><input type="checkbox" v-model="isAnonymous" /> 匿名发布</label>
      <div class="actions">
        <button class="btn" @click="showSubmit = false">取消</button>
        <button class="btn primary" :disabled="submitting || !content.trim()" @click="publish">发布</button>
      </div>
    </div>

    <p v-if="!loading && fragments.length === 0" class="empty">还没有碎片，来写第一块吧</p>

    <FragmentCard v-for="f in fragments" :key="f.id" :fragment="f" @like="toggleLike" />

    <div v-if="hasMore" class="more-wrap">
      <button class="btn" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
    </div>
  </div>
</template>
