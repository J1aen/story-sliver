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
  // Task 19：自动贴合——把「圆形里的可见内容」紧贴到画布边缘再放大铺满。
  // 为什么：以前用户不调整圆形大小时，导出图会有大片透明边距，显示出来就是「特别小的头像」；
  // 现在按透明像素的边界裁掉空边并放大，无论圆形画多大，头像都会占满 512x512。
  const imgData = ctx.getImageData(0, 0, out, out).data
  let minX = out, minY = out, maxX = -1, maxY = -1
  for (let y = 0; y < out; y++) {
    for (let x = 0; x < out; x++) {
      // alpha 通道 > 8 视为可见内容（抗锯齿边缘的浅透明像素忽略）
      if (imgData[(y * out + x) * 4 + 3] > 8) {
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
      }
    }
  }
  // 全透明（理论上不会发生）：直接导出原图
  if (maxX < 0) {
    canvas.toBlob((blob) => blob && emit('cropped', blob), 'image/png')
    return
  }
  // 留 8px 边距，避免圆形边缘被切得太紧
  const pad = 8
  minX = Math.max(0, minX - pad)
  minY = Math.max(0, minY - pad)
  maxX = Math.min(out - 1, maxX + pad)
  maxY = Math.min(out - 1, maxY + pad)
  // 以内容中心取正方形区域，再放大铺满 512x512
  const side = Math.max(maxX - minX + 1, maxY - minY + 1)
  const cx = Math.round((minX + maxX) / 2)
  const cy = Math.round((minY + maxY) / 2)
  const bx = Math.max(0, Math.min(cx - side / 2, out - side))
  const by = Math.max(0, Math.min(cy - side / 2, out - side))
  const finalCanvas = document.createElement('canvas')
  finalCanvas.width = out
  finalCanvas.height = out
  const fctx = finalCanvas.getContext('2d')
  fctx.drawImage(canvas, bx, by, side, side, 0, 0, out, out)
  finalCanvas.toBlob((blob) => {
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
