<script setup>
defineProps({ show: Boolean, title: String, message: String })
// 必须用 const emit 接收，模板里的 emit() 才能用（否则点击按钮报「emit 未定义」）
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <div v-if="show" class="modal-mask" @click.self="emit('cancel')">
    <div class="modal small">
      <h3>{{ title }}</h3>
      <p class="danger-text">{{ message }}</p>
      <div class="modal-actions">
        <button class="btn" @click="emit('cancel')">取消</button>
        <button class="btn danger" @click="emit('confirm')">确认删除</button>
      </div>
    </div>
  </div>
</template>
