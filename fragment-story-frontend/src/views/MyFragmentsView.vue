<script setup>
import { onMounted, ref } from 'vue'
import { deleteFragment, getMyFragments, hideFragment, likeFragment, unhideFragment, unlikeFragment } from '../api/fragments'
import FragmentCard from '../components/FragmentCard.vue'
import CommentModal from '../components/CommentModal.vue'// v2.0 Task 21：评论详情弹窗
import ConfirmDialog from '../components/ConfirmDialog.vue'

const fragments = ref([])
const loading = ref(true)
const confirmTarget = ref(null)
const message = ref('')
const commentFragment = ref(null)// 正在查看评论的碎片（null=弹窗关闭）

// 加载我的碎片（全部状态：待审核/已发布/已隐藏，含匿名的）
async function load() {
  loading.value = true
  try {
    fragments.value = await getMyFragments()
  } catch (e) {
    message.value = e.message
  } finally {
    loading.value = false
  }
}

// 隐藏 / 取消隐藏：只更新本地状态，不用重新拉列表
async function onHide(f) {
  try { await hideFragment(f.id); f.status = 2 } catch (e) { message.value = e.message }
}
async function onUnhide(f) {
  try { await unhideFragment(f.id); f.status = 1 } catch (e) { message.value = e.message }
}

// 删除：弹窗确认后硬删除，前端不可恢复
async function onConfirmDelete() {
  try {
    await deleteFragment(confirmTarget.value.id)
    fragments.value = fragments.value.filter((x) => x.id !== confirmTarget.value.id)
  } catch (e) {
    message.value = e.message
  } finally {
    confirmTarget.value = null
  }
}

onMounted(load)

// v2.0 Task 21：详情弹窗里点赞/取消（fragment 对象就地更新）
async function toggleLike(f) {
  try {
    if (f.likedByMe) {
      await unlikeFragment(f.id)
      f.likedByMe = false
      f.likeCount--
    } else {
      await likeFragment(f.id)
      f.likedByMe = true
      f.likeCount++
    }
  } catch (e) { message.value = e.message }
}
</script>

<template>
  <div class="page">
    <h1>我的碎片</h1>
    <p v-if="message" class="error">{{ message }}</p>
    <p v-if="!loading && fragments.length === 0" class="empty">你还没有发过碎片</p>
    <FragmentCard
      v-for="f in fragments"
      :key="f.id"
      :fragment="f"
      show-actions
      @hide="onHide"
      @unhide="onUnhide"
      @delete="confirmTarget = f"
      @like="toggleLike"
      @comments="commentFragment = f"
    />
    <ConfirmDialog
      :show="!!confirmTarget"
      title="删除碎片"
      message="删除后无法撤回，确定删除吗？"
      @cancel="confirmTarget = null"
      @confirm="onConfirmDelete"
    />
    <!-- v2.0 Task 21：评论详情弹窗（我的碎片里也能看评论/点赞） -->
    <CommentModal v-if="commentFragment" :fragment="commentFragment" @like="toggleLike" @close="commentFragment = null" />
  </div>
</template>
