<script setup>
import { onMounted, ref } from 'vue'

defineProps({ show: Boolean, imageUrl: String })
const emit = defineEmits(['cropped', 'cancel'])

const viewport = 220// 圆形裁剪框显示尺寸
const scale = ref(1)
const offset = ref({ x: 0, y: 0 })
const imgSize = ref({ w: 0, h: 0 })
const imgEl = ref(null)
const dragging = ref(false)
const start = ref({ x: 0, y: 0 })

// 图片加载完成后计算初始尺寸：按比例适配裁剪框并居中
function initCrop() {
  const iw = imgEl.value.naturalWidth
  const ih = imgEl.value.naturalHeight
  const fit = Math.min(viewport / iw, viewport / ih)
  imgSize.value = { w: iw * fit, h: ih * fit }
  offset.value = { x: (viewport - imgSize.value.w) / 2, y: (viewport - imgSize.value.h) / 2 }
  scale.value = 1
}

// 拖动时保证图片始终盖住圆形区域（不露出空白）
function clampOffset() {
  const w = imgSize.value.w * scale.value
  const h = imgSize.value.h * scale.value
  const maxX = Math.max(0, (w - viewport) / 2)
  const maxY = Math.max(0, (h - viewport) / 2)
  offset.value.x = Math.min(Math.max(offset.value.x, -maxX), maxX)
  offset.value.y = Math.min(Math.max(offset.value.y, -maxY), maxY)
}

function onPointerDown(e) {
  dragging.value = true
  start.value = { x: e.clientX - offset.value.x, y: e.clientY - offset.value.y }
}
function onPointerMove(e) {
  if (!dragging.value) return
  offset.value.x = e.clientX - start.value.x
  offset.value.y = e.clientY - start.value.y
  clampOffset()
}
function onPointerUp() {
  dragging.value = false
}

function zoom(delta) {
  scale.value = Math.min(Math.max(scale.value + delta, 1), 3)
  clampOffset()
}

// 按当前显示位置裁成圆形并导出 PNG Blob（背景透明）
function confirm() {
  const out = 256
  const canvas = document.createElement('canvas')
  canvas.width = out
  canvas.height = out
  const ctx = canvas.getContext('2d')
  const k = out / viewport
  ctx.beginPath()
  ctx.arc(out / 2, out / 2, out / 2, 0, Math.PI * 2)
  ctx.clip()
  ctx.drawImage(
    imgEl.value,
    offset.value.x * k,
    offset.value.y * k,
    imgSize.value.w * scale.value * k,
    imgSize.value.h * scale.value * k
  )
  canvas.toBlob((blob) => {
    if (blob) emit('cropped', blob)
  }, 'image/png')
}

// 每次打开弹窗重置
onMounted(() => {
  scale.value = 1
  offset.value = { x: 0, y: 0 }
})
</script>

<template>
  <div v-if="show" class="modal-mask" @click.self="emit('cancel')">
    <div class="modal">
      <h3>裁剪头像</h3>
      <p class="subtitle">拖动图片调整位置，按钮缩放；圆形区域就是头像</p>
      <div class="crop-stage">
        <img
          ref="imgEl"
          :src="imageUrl"
          class="crop-img"
          draggable="false"
          :style="{
            width: imgSize.w * scale + 'px',
            height: imgSize.h * scale + 'px',
            transform: 'translate(' + offset.x + 'px,' + offset.y + 'px)'
          }"
          @load="initCrop"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @pointerup="onPointerUp"
          @pointerleave="onPointerUp"
        />
      </div>
      <div class="crop-controls">
        <button class="btn small" @click="zoom(-0.25)">缩小</button>
        <button class="btn small" @click="zoom(0.25)">放大</button>
      </div>
      <div class="modal-actions">
        <button class="btn" @click="emit('cancel')">取消</button>
        <button class="btn primary" @click="confirm">确认</button>
      </div>
    </div>
  </div>
</template>
