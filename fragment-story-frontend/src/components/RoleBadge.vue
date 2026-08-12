<script setup>
// 身份铭牌：根据角色显示不同样式的标识
// role：0 普通（不显示）/ 1 管理员（蓝色铭牌）/ 2 站长（金色铭牌）
defineProps({ role: Number })
</script>

<template>
  <span v-if="role === 2" class="badge owner">站长</span>
  <span v-else-if="role === 1" class="badge admin">管理员</span>
</template>
