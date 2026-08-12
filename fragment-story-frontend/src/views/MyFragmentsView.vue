<script setup>
import { onMounted, ref } from 'vue'
import { deleteFragment, getMyFragments, hideFragment, unhideFragment } from '../api/fragments'
import FragmentCard from '../components/FragmentCard.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'

const fragments = ref([])
const loading = ref(true)
const confirmTarget = ref(null)
const message = ref('')

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
    />
    <ConfirmDialog
      :show="!!confirmTarget"
      title="删除碎片"
      message="删除后无法撤回，确定删除吗？"
      @cancel="confirmTarget = null"
      @confirm="onConfirmDelete"
    />
  </div>
</template>
