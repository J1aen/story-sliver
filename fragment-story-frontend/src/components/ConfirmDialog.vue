<script setup>
defineProps({ show: Boolean, title: String, message: String })
defineEmits(['confirm', 'cancel'])
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
