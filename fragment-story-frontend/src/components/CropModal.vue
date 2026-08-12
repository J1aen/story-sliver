<script setup>
import { onMounted, ref } from 'vue'

defineProps({ show: Boolean, imageUrl: String })
const emit = defineEmits(['cropped', 'cancel'])

const stage = 280// 裁剪舞台边长（正方形）
const circle = ref({ cx: stage / 2, cy: stage / 2, r: 64 })
const imgEl = ref(null)
const natural = ref({ w: 0, h: 0 })
const mode = ref('')// '' 无操作 / 'move' 拖动圆 / 'resize' 拖角调整大小
const drag = ref({ px: 0, py: 0, start: null })

function initCrop() {
  natural.value = { w: imgEl.value.naturalWidth, h: imgEl.value.naturalHeight }
  circle.value = { cx: stage / 2, cy: stage / 2, r: 64 }
}

// 指针相对舞台的坐标
function pos(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

function clamp(v, min, max) {
  return Math.min(Math.max(v, min), max)
}

function onPointerDown(e) {
  const p = pos(e)
  const isHandle = e.target && e.target.classList.contains('crop-handle')
  if (isHandle) {
    mode.value = 'resize'
  } else {
    const dx = p.x - circle.value.cx
    const dy = p.y - circle.value.cy
    if (dx * dx + dy * dy <= circle.value.r * circle.value.r) {
      mode.value = 'move'
    } else {
      return// 点在圆外，不响应
    }
  }
  drag.value = { px: p.x, py: p.y, start: { ...circle.value } }
}

function onPointerMove(e) {
  if (!mode.value) return
  const p = pos(e)
  if (mode.value === 'move') {
    const dx = p.x - drag.value.px
    const dy = p.y - drag.value.py
    circle.value.cx = clamp(drag.value.start.cx + dx, circle.value.r, stage - circle.value.r)
    circle.value.cy = clamp(drag.value.start.cy + dy, circle.value.r, stage - circle.value.r)
  } else if (mode.value === 'resize') {
    // 半径 = 圆心到指针的距离，直观地「拖多大多大」
    const dist = Math.hypot(p.x - circle.value.cx, p.y - circle.value.cy)
    circle.value.r = clamp(dist, 36, stage / 2 - 6)
    // 保证圆心仍在舞台内
    circle.value.cx = clamp(circle.value.cx, circle.value.r, stage - circle.value.r)
    circle.value.cy = clamp(circle.value.cy, circle.value.r, stage - circle.value.r)
  }
}

function onPointerUp() {
  mode.value = ''
}

// 按当前圆形区域裁切：先按 cover 方式把图铺满舞台，再裁圆形导出透明 PNG
function confirm() {
  const out = 512
  const canvas = document.createElement('canvas')
  canvas.width = out
  canvas.height = out
  const ctx = canvas.getContext('2d')
  const k = out / stage
  // 计算 object-fit: cover 的源图裁剪区域
  const iw = natural.value.w
  const ih = natural.value.h
  const scale = Math.max(iw / stage, ih / stage)
  const sw = stage * scale
  const sh = stage * scale
  const sx = (iw - sw) / 2
  const sy = (ih - sh) / 2
  ctx.drawImage(imgEl.value, sx, sy, sw, sh, 0, 0, out, out)
  // 圆形裁剪（透明背景）
  ctx.globalCompositeOperation = 'destination-in'
  ctx.beginPath()
  ctx.arc(circle.value.cx * k, circle.value.cy * k, circle.value.r * k, 0, Math.PI * 2)
  ctx.fill()
  canvas.toBlob((blob) => {
    if (blob) emit('cropped', blob)
  }, 'image/png')
}

onMounted(() => {
  circle.value = { cx: stage / 2, cy: stage / 2, r: 64 }
})
</script>

<template>
  <div v-if="show" class="modal-mask" @click.self="emit('cancel')">
    <div class="modal">
      <h3>裁剪头像</h3>
      <p class="subtitle">拖动圆形移动位置，拖右下角圆点调整大小</p>
      <div
        class="crop-stage2"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointerleave="onPointerUp"
      >
        <img ref="imgEl" :src="imageUrl" class="crop-img2" draggable="false" @load="initCrop" alt="待裁剪图片" />
        <div
          class="crop-circle"
          :style="{
            left: circle.cx - circle.r + 'px',
            top: circle.cy - circle.r + 'px',
            width: circle.r * 2 + 'px',
            height: circle.r * 2 + 'px'
          }"
        >
          <span class="crop-handle" aria-hidden="true"></span>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn" @click="emit('cancel')">取消</button>
        <button class="btn primary" @click="confirm">确认</button>
      </div>
    </div>
  </div>
</template>
