<template>
  <div class="drawing-page">
    <!-- 顶部步骤条：1 在线绘制仓库（当前） → 2 配置货架信息 -->
    <div class="step-bar">
      <div class="step step-active">
        <span class="step-num">1</span>
        <span class="step-text">在线绘制仓库</span>
      </div>
      <span class="step-arrow">→</span>
      <div class="step step-next" title="绘制完成后进入货架配置" @click="goStep2">
        <span class="step-num">2</span>
        <span class="step-text">配置货架信息</span>
      </div>
      <div class="step-bar-right">
        <span class="saved-tip">{{ savedTip }}</span>
      </div>
    </div>

    <div class="board-body">
      <!-- 左侧工具栏 -->
      <aside class="tool-panel">
        <button class="upload-btn" @click="uploadVisible = true">
          <svg viewBox="0 0 1024 1024" class="ico" aria-hidden="true">
            <path d="M542.72 186.181a32 32 0 0 0-45.248 0L331.392 352.32a32 32 0 0 0 45.184 45.248l131.2-131.264v364.8a32 32 0 0 0 64 0V266.24l131.264 131.328a32 32 0 0 0 45.184-45.248L542.72 186.24z" />
            <path d="M864 608a32 32 0 0 0-32 32v128a32 32 0 0 1-32 32H224a32 32 0 0 1-32-32v-128a32 32 0 0 0-64 0v128a96 96 0 0 0 96 96h576a96 96 0 0 0 96-96v-128a32 32 0 0 0-32-32z" />
          </svg>
          上传平面图
        </button>

        <div class="field-label">方格大小</div>
        <el-select v-model="plan.gridSize" size="default" @change="onGridSizeChange">
          <el-option v-for="g in gridOptions" :key="g.value" :label="g.label" :value="g.value" />
        </el-select>

        <div class="field-label">基础形状</div>
        <div class="shape-grid">
          <div v-for="s in shapeDefs" :key="s.type" class="shape-btn"
               :class="{ active: drawShape === s.type }" @click="selectDrawShape(s.type)">
            <svg :viewBox="s.viewBox" class="shape-ico" aria-hidden="true">
              <polygon :points="s.points" />
            </svg>
            <span>{{ s.label }}</span>
          </div>
        </div>

        <div class="field-label">工具</div>
        <div class="tool-btn" :class="{ active: !drawShape && tool === 'select' }" @click="setTool('select')">
          <el-icon><Pointer /></el-icon>
          <span>选择工具</span>
        </div>
        <div class="tool-btn" :class="{ active: !drawShape && tool === 'grid' }" @click="setTool('grid')">
          <el-icon><Grid /></el-icon>
          <span>网格选择</span>
        </div>

        <div class="panel-spacer" />

        <button class="save-btn" :disabled="saving" @click="save">
          <svg viewBox="0 0 1024 1024" class="ico" aria-hidden="true">
            <path d="M893.44 338.24a32 32 0 0 0-10.88-7.04 30.72 30.72 0 0 0-26.24 0 32 32 0 0 0-10.88 7.04L529.28 647.68a32 32 0 1 0 45.248 45.248l250.24-250.368V864a32 32 0 0 1-32 32H224a32 32 0 0 1-32-32V160a32 32 0 0 1 32-32h393.6a32 32 0 0 0 0-64H224a96 96 0 0 0-96 96v704a96 96 0 0 0 96 96h568.768a96 96 0 0 0 96-96V360.64a32 32 0 0 0-4.608-16.32 32 32 0 0 0-1.92-3.2 32 32 0 0 0 10.944-22.08z" />
            <path d="M704 320H352a32 32 0 0 0-32 32v128a32 32 0 0 0 64 0V384h288a64 64 0 0 1 64 64v128a32 32 0 0 0 64 0V448a128 128 0 0 0-96-128z" />
          </svg>
          {{ saving ? '保存中…' : '保存平面图' }}
        </button>
        <button class="delete-btn" @click="removeSelected">
          <svg viewBox="0 0 1024 1024" class="ico" aria-hidden="true">
            <path d="M864 256h-160v-64a64 64 0 0 0-64-64H384a64 64 0 0 0-64 64v64H160a32 32 0 0 0 0 64h32v512a96 96 0 0 0 96 96h448a96 96 0 0 0 96-96V320h32a32 32 0 0 0 0-64z m-448-64h224v64H416z m320 640a32 32 0 0 1-32 32H256a32 32 0 0 1-32-32V320h512z" />
            <path d="M416 448m32 0l0 0q32 0 32 32l0 224q0 32-32 32l0 0q-32 0-32-32l0-224q0-32 32-32Z" />
            <path d="M608 448m32 0l0 0q32 0 32 32l0 224q0 32-32 32l0 0q-32 0-32-32l0-224q0-32 32-32Z" />
          </svg>
          删除仓库
        </button>
      </aside>

      <!-- 中部画布 -->
      <div class="canvas-wrap">
        <svg ref="svgRef" class="board-svg" :viewBox="`0 0 ${plan.width} ${plan.height}`"
             :class="canvasCursorClass"
             @mousedown="onCanvasMouseDown" @mousemove="onCanvasMouseMove"
             @mouseup="onCanvasMouseUp" @mouseleave="onCanvasMouseUp">
          <defs>
            <pattern id="small-grid" :width="plan.gridSize" :height="plan.gridSize" patternUnits="userSpaceOnUse">
              <path :d="`M ${plan.gridSize} 0 L 0 0 0 ${plan.gridSize}`" fill="none" stroke="#ececec" stroke-width="0.04" />
            </pattern>
            <pattern id="big-grid" :width="bigCell" :height="bigCell" patternUnits="userSpaceOnUse">
              <rect :width="bigCell" :height="bigCell" fill="url(#small-grid)" />
              <path :d="`M ${bigCell} 0 L 0 0 0 ${bigCell}`" fill="none" stroke="#dcdcdc" stroke-width="0.06" />
            </pattern>
          </defs>
          <rect x="0" y="0" :width="plan.width" :height="plan.height" fill="#fff" />
          <rect x="0" y="0" :width="plan.width" :height="plan.height" fill="url(#big-grid)" />
          <image v-if="plan.bgImage" :href="plan.bgImage" x="0" y="0"
                 :width="plan.width" :height="plan.height" preserveAspectRatio="none" opacity="0.45" />

          <!-- 已绘制图形 -->
          <g v-for="s in shapes" :key="s.id" class="shape-g"
             @mousedown.stop="onShapeMouseDown(s, $event)">
            <polygon :points="pointsOf(s)"
                     :fill="isSelected(s) ? '#e6f7ff' : '#fafafa'"
                     :stroke="isSelected(s) ? '#1890ff' : '#bfbfbf'"
                     :stroke-width="isSelected(s) ? 0.25 : 0.12" />
            <text :x="s.x + s.w / 2" :y="s.y + s.h / 2" text-anchor="middle" dominant-baseline="middle"
                  class="shape-label" :font-size="labelSize(s)">{{ s.name }}</text>
            <!-- 选中手柄（仅单选） -->
            <g v-if="selected && selected.id === s.id">
              <rect v-for="hd in handlesOf(s)" :key="hd.key" class="handle"
                    :x="hd.cx - handleR" :y="hd.cy - handleR"
                    :width="handleR * 2" :height="handleR * 2"
                    @mousedown.stop="onHandleMouseDown(s, hd.key, $event)" />
            </g>
          </g>

          <!-- 绘制预览 -->
          <polygon v-if="drag && drag.kind === 'draw' && previewShape"
                   :points="pointsOf(previewShape)" fill="rgba(24,144,255,0.12)"
                   stroke="#1890ff" stroke-width="0.15" stroke-dasharray="0.8 0.5" />
          <!-- 网格选择框 -->
          <rect v-if="drag && drag.kind === 'marquee'"
                :x="marquee.x" :y="marquee.y" :width="marquee.w" :height="marquee.h"
                fill="rgba(24,144,255,0.08)" stroke="#1890ff" stroke-width="0.15" stroke-dasharray="0.8 0.5" />
        </svg>
        <div class="canvas-scale">{{ plan.width }}m × {{ plan.height }}m　方格 {{ gridSizeText }}</div>
      </div>

      <!-- 右侧属性面板 -->
      <aside class="props-panel" :class="{ collapsed: panelCollapsed }">
        <div class="panel-header" @click="panelCollapsed = !panelCollapsed">
          <span>属性面板</span>
          <el-icon class="panel-arrow" :class="{ flipped: panelCollapsed }"><ArrowDown /></el-icon>
        </div>
        <div v-show="!panelCollapsed" class="panel-body">
          <template v-if="selected">
            <div class="section-title">仓库信息</div>
            <div class="grid-2">
              <div class="field">
                <label>仓库名称</label>
                <el-input v-model="selected.name" placeholder="输入仓库名称" />
              </div>
              <div class="field">
                <label>仓库编号</label>
                <el-input v-model="selected.code" placeholder="输入仓库编号" />
              </div>
              <div class="field">
                <label>容量</label>
                <el-input-number v-model="selected.capacity" :min="0" placeholder="容量" controls-position="right" />
              </div>
              <div class="field">
                <label>最大重量</label>
                <el-input-number v-model="selected.maxWeight" :min="0" placeholder="最大重量" controls-position="right" />
              </div>
              <div class="field">
                <label>仓库立方数</label>
                <el-input-number v-model="selected.volume" :min="0" placeholder="立方数" controls-position="right" />
              </div>
              <div class="field">
                <label>仓库状态</label>
                <el-select v-model="selected.status">
                  <el-option label="启用" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </div>
            </div>

            <div class="section-title">形状信息</div>
            <div class="grid-2">
              <div class="field">
                <label>长度(m)</label>
                <el-input-number v-model="selected.w" :min="plan.gridSize" :step="plan.gridSize"
                                 controls-position="right" @change="onLengthChange" />
              </div>
              <div class="field">
                <label>宽度(m)</label>
                <el-input-number v-model="selected.h" :min="plan.gridSize" :step="plan.gridSize"
                                 controls-position="right" @change="onWidthChange" />
              </div>
              <div class="field">
                <label>高度(m)</label>
                <el-input-number v-model="selected.heightM" :min="0.5" :step="0.5" controls-position="right" />
              </div>
              <div class="field">
                <label>面积(m²)</label>
                <el-input :model-value="areaOf(selected)" disabled />
              </div>
              <div class="field field-full">
                <label>形状类型</label>
                <el-select v-model="selected.shape" @change="onShapeTypeChange">
                  <el-option v-for="s in shapeDefs" :key="s.type" :label="s.label" :value="s.type" />
                </el-select>
              </div>
            </div>

            <div class="section-title">库管信息</div>
            <div class="grid-2">
              <div class="field">
                <label>负责人</label>
                <el-input v-model="selected.manager" placeholder="负责人" />
              </div>
              <div class="field">
                <label>归属部门</label>
                <el-input v-model="selected.department" placeholder="归属部门" />
              </div>
              <div class="field field-full">
                <label>联系电话</label>
                <el-input v-model="selected.phone" placeholder="联系电话" />
              </div>
            </div>

            <el-collapse class="more-collapse">
              <el-collapse-item title="更多信息" name="more">
                <div class="field">
                  <label>仓库地址</label>
                  <el-input v-model="selected.address" placeholder="输入仓库地址" />
                </div>
                <div class="field">
                  <label>备注</label>
                  <el-input v-model="selected.remark" type="textarea" :rows="2" placeholder="输入备注" />
                </div>
              </el-collapse-item>
            </el-collapse>
          </template>
          <el-empty v-else-if="selectIds.length > 1" :description="`已选中 ${selectIds.length} 个仓库，可拖动或删除`" :image-size="70" />
          <el-empty v-else description="在画布上绘制或点击仓库图形查看属性" :image-size="70" />
        </div>
      </aside>
    </div>

    <!-- 上传 CAD / 平面图弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传CAD或平面图" width="430px" align-center>
      <div class="drop-zone" :class="{ dragging: dragOver }"
           @dragover.prevent="dragOver = true" @dragleave.prevent="dragOver = false"
           @drop.prevent="onDrop" @click="fileRef && fileRef.click()">
        <svg viewBox="0 0 1024 1024" class="drop-ico" aria-hidden="true">
          <path d="M845.76 759.04a32 32 0 0 1-45.248 0L543.232 501.76a44.8 44.8 0 0 1 0-63.36L800.512 181.12a32 32 0 0 1 45.248 45.248L610.56 461.888a11.52 11.52 0 0 0 0 16.32l235.264 235.52a32 32 0 0 1 0 45.248z" />
          <path d="M880 288a32 32 0 0 0-32 32v448a32 32 0 0 1-32 32H208a32 32 0 0 1-32-32V320a32 32 0 0 1 32-32h288a32 32 0 0 0 0-64H208a96 96 0 0 0-96 96v448a96 96 0 0 0 96 96h608a96 96 0 0 0 96-96V320a32 32 0 0 0-32-32z" />
        </svg>
        <p>拖拽文件到此处或点击选择文件</p>
        <el-button type="primary" size="default" :loading="importing" @click.stop="fileRef && fileRef.click()">选择文件</el-button>
        <input ref="fileRef" type="file" accept="image/*" hidden @change="onFileChange" />
      </div>
      <div class="drop-tip">支持导入图片格式图纸（PNG / JPG 等）作为背景底图，导入后可继续在其上绘制仓库</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pointer, Grid, ArrowDown } from '@element-plus/icons-vue'
import { getPlan, savePlan, importPlanBackground } from '../api/plan'

const router = useRouter()

const gridOptions = [
  { label: '0.5m × 0.5m', value: 0.5 },
  { label: '1m × 1m', value: 1 },
  { label: '2m × 2m', value: 2 },
  { label: '5m × 5m', value: 5 }
]

const shapeDefs = [
  { type: 'square', label: '正方形', viewBox: '0 0 40 40', points: '6,6 34,6 34,34 6,34' },
  { type: 'rect', label: '长方形', viewBox: '0 0 40 40', points: '4,12 36,12 36,28 4,28' },
  { type: 'parallelogram', label: '平行四边形', viewBox: '0 0 40 40', points: '10,10 36,10 30,30 4,30' },
  { type: 'trapezoid', label: '梯形', viewBox: '0 0 40 40', points: '12,10 28,10 36,30 4,30' }
]

const plan = reactive({ id: null, name: '仓库平面图', width: 60, height: 40, gridSize: 1, bgImage: null })
const shapes = ref([])
const selectIds = ref([])
const drawShape = ref(null)
const tool = ref('select')
const drag = ref(null)
const saving = ref(false)
const savedTip = ref('')

const svgRef = ref(null)
const fileRef = ref(null)
const uploadVisible = ref(false)
const importing = ref(false)
const dragOver = ref(false)
const panelCollapsed = ref(false)

const bigCell = computed(() => plan.gridSize * 5)
const gridSizeText = computed(() => {
  const g = gridOptions.find(o => o.value === plan.gridSize)
  return g ? g.label : `${plan.gridSize}m`
})
const handleR = computed(() => Math.max(0.25, plan.gridSize * 0.22))

const selected = computed(() => {
  if (selectIds.value.length !== 1) return null
  return shapes.value.find(s => s.id === selectIds.value[0]) || null
})

const canvasCursorClass = computed(() => ({
  'cursor-draw': !!drawShape.value,
  'cursor-grid': !drawShape.value && tool.value === 'grid'
}))

const previewShape = computed(() => {
  if (!drag.value || drag.value.kind !== 'draw') return null
  const r = rectOf(drag.value.start, drag.value.cur)
  if (drawShape.value === 'square') {
    const side = Math.max(r.w, r.h)
    return makeShape(drawShape.value, drag.value.start.x, drag.value.start.y, side, side)
  }
  return makeShape(drawShape.value, r.x, r.y, r.w, r.h)
})

const marquee = computed(() => {
  if (!drag.value || drag.value.kind !== 'marquee') return { x: 0, y: 0, w: 0, h: 0 }
  return rectOf(drag.value.start, drag.value.cur)
})

// ---------- 几何 ----------
const rectOf = (a, b) => ({
  x: Math.min(a.x, b.x), y: Math.min(a.y, b.y),
  w: Math.abs(b.x - a.x), h: Math.abs(b.y - a.y)
})

const pointsOf = (s) => {
  const { x, y, w, h } = s
  if (s.shape === 'parallelogram') {
    const k = clampNum(s.skew, 0, w / 2)
    return `${x + k},${y} ${x + w},${y} ${x + w - k},${y + h} ${x},${y + h}`
  }
  if (s.shape === 'trapezoid') {
    const tw = clampNum(s.topW, 0.5, w)
    const off = (w - tw) / 2
    return `${x + off},${y} ${x + off + tw},${y} ${x + w},${y + h} ${x},${y + h}`
  }
  return `${x},${y} ${x + w},${y} ${x + w},${y + h} ${x},${y + h}`
}

const handlesOf = (s) => [
  { key: 'nw', cx: s.x, cy: s.y }, { key: 'n', cx: s.x + s.w / 2, cy: s.y },
  { key: 'ne', cx: s.x + s.w, cy: s.y }, { key: 'e', cx: s.x + s.w, cy: s.y + s.h / 2 },
  { key: 'se', cx: s.x + s.w, cy: s.y + s.h }, { key: 's', cx: s.x + s.w / 2, cy: s.y + s.h },
  { key: 'sw', cx: s.x, cy: s.y + s.h }, { key: 'w', cx: s.x, cy: s.y + s.h / 2 }
]

const clampNum = (v, min, max) => Math.min(Math.max(v, min), max)
const snap = (v) => Math.round(v / plan.gridSize) * plan.gridSize
const labelSize = (s) => Math.max(0.9, Math.min(1.6, s.w / 6, s.h / 3))
const areaOf = (s) => (Number(s.w) * Number(s.h)).toFixed(1)
const isSelected = (s) => selectIds.value.includes(s.id)

const makeShape = (shape, x, y, w, h, patch = {}) => ({
  id: 's' + Date.now() + '' + Math.floor(Math.random() * 1000),
  warehouseId: null,
  shape, x, y, w, h,
  skew: shape === 'parallelogram' ? w / 4 : undefined,
  topW: shape === 'trapezoid' ? w / 2 : undefined,
  name: '', code: '', capacity: null, maxWeight: null, volume: null,
  status: 1, heightM: 15, manager: '', department: '', phone: '',
  address: '', remark: '', ...patch
})

const nextName = () => {
  let max = 0
  shapes.value.forEach(s => {
    const m = /^仓库(\d+)$/.exec(s.name || '')
    if (m) max = Math.max(max, Number(m[1]))
  })
  return `仓库${max + 1}`
}

// ---------- 坐标换算 ----------
const toPoint = (e) => {
  const rect = svgRef.value.getBoundingClientRect()
  const scale = Math.min(rect.width / plan.width, rect.height / plan.height)
  const ox = (rect.width - plan.width * scale) / 2
  const oy = (rect.height - plan.height * scale) / 2
  return {
    x: (e.clientX - rect.left - ox) / scale,
    y: (e.clientY - rect.top - oy) / scale
  }
}

// ---------- 交互 ----------
const selectDrawShape = (type) => {
  drawShape.value = drawShape.value === type ? null : type
  if (drawShape.value) tool.value = 'select'
}

const setTool = (t) => {
  tool.value = t
  drawShape.value = null
}

const onCanvasMouseDown = (e) => {
  const p = toPoint(e)
  if (drawShape.value) {
    drag.value = { kind: 'draw', start: p, cur: p }
    return
  }
  if (tool.value === 'grid') {
    drag.value = { kind: 'marquee', start: p, cur: p }
  } else {
    selectIds.value = []
    drag.value = null
  }
}

const onShapeMouseDown = (s, e) => {
  if (drawShape.value) return
  if (!selectIds.value.includes(s.id)) {
    selectIds.value = [s.id]
  }
  const p = toPoint(e)
  drag.value = {
    kind: 'move', start: p,
    origins: new Map(shapes.value.filter(x => selectIds.value.includes(x.id))
      .map(x => [x.id, { x: x.x, y: x.y }]))
  }
}

const onHandleMouseDown = (s, key, e) => {
  const p = toPoint(e)
  drag.value = { kind: 'resize', id: s.id, handle: key, orig: { ...s } }
}

const onCanvasMouseMove = (e) => {
  if (!drag.value) return
  const p = toPoint(e)
  drag.value.cur = p
  if (drag.value.kind === 'move') {
    const dx = snap(p.x - drag.value.start.x)
    const dy = snap(p.y - drag.value.start.y)
    drag.value.origins.forEach((o, id) => {
      const s = shapes.value.find(x => x.id === id)
      if (s) {
        s.x = clampNum(o.x + dx, 0, plan.width - s.w)
        s.y = clampNum(o.y + dy, 0, plan.height - s.h)
      }
    })
  } else if (drag.value.kind === 'resize') {
    const s = shapes.value.find(x => x.id === drag.value.id)
    if (!s) return
    const o = drag.value.orig
    const h = drag.value.handle
    let x1 = o.x, y1 = o.y, x2 = o.x + o.w, y2 = o.y + o.h
    const px = snap(p.x), py = snap(p.y)
    if (h.includes('w')) x1 = Math.min(px, x2 - plan.gridSize)
    if (h.includes('e')) x2 = Math.max(px, x1 + plan.gridSize)
    if (h.includes('n')) y1 = Math.min(py, y2 - plan.gridSize)
    if (h.includes('s')) y2 = Math.max(py, y1 + plan.gridSize)
    let nx = Math.max(0, x1), nw = Math.min(x2, plan.width) - nx
    let ny = Math.max(0, y1), nh = Math.min(y2, plan.height) - ny
    if (o.shape === 'square') {
      const side = Math.max(nw, nh)
      nw = Math.min(side, plan.width - nx)
      nh = Math.min(side, plan.height - ny)
      if (h.includes('n')) ny = y2 - nh
      if (h.includes('w')) nx = x2 - nw
    }
    if (nw < plan.gridSize || nh < plan.gridSize) return
    applyBbox(s, nx, ny, nw, nh, o)
  }
}

const applyBbox = (s, x, y, w, h, orig) => {
  s.x = x; s.y = y
  if (s.shape === 'parallelogram' && orig) {
    s.w = w; s.h = h
    s.skew = clampNum((orig.skew ?? w / 4) * (w / (orig.w || w)), 0, w / 2)
  } else if (s.shape === 'trapezoid' && orig) {
    s.w = w; s.h = h
    s.topW = clampNum((orig.topW ?? w / 2) * (w / (orig.w || w)), 0.5, w)
  } else {
    s.w = w; s.h = h
  }
}

const onCanvasMouseUp = () => {
  if (!drag.value) return
  const d = drag.value
  drag.value = null
  if (d.kind === 'draw' && drawShape.value) {
    const r = rectOf(d.start, d.cur)
    const sx = snap(d.start.x), sy = snap(d.start.y)
    let w = Math.max(snap(r.w), plan.gridSize)
    let h = Math.max(snap(r.h), plan.gridSize)
    if (drawShape.value === 'square') {
      const side = Math.max(w, h)
      w = Math.min(side, plan.width - sx)
      h = Math.min(side, plan.height - sy)
      w = Math.min(w, h)
      h = w
    }
    if (w >= plan.gridSize && h >= plan.gridSize && sx + w <= plan.width + 1e-6 && sy + h <= plan.height + 1e-6) {
      const s = makeShape(drawShape.value, Math.min(sx, plan.width - w), Math.min(sy, plan.height - h), w, h)
      s.name = nextName()
      shapes.value.push(s)
      selectIds.value = [s.id]
      savedTip.value = ''
    }
  } else if (d.kind === 'marquee') {
    const m = rectOf(d.start, d.cur)
    if (m.w > plan.gridSize / 2 && m.h > plan.gridSize / 2) {
      selectIds.value = shapes.value.filter(s =>
        s.x < m.x + m.w && s.x + s.w > m.x && s.y < m.y + m.h && s.y + s.h > m.y
      ).map(s => s.id)
    }
  }
}

// ---------- 属性面板 ----------
const onLengthChange = (val) => {
  const s = selected.value
  if (!s || val == null) return
  const w = clampNum(val, plan.gridSize, plan.width - s.x)
  s.w = w
  if (s.shape === 'square') s.h = w
  if (s.shape === 'trapezoid') s.topW = clampNum(s.topW, 0.5, w)
  if (s.shape === 'parallelogram') s.skew = clampNum(s.skew, 0, w / 2)
}

const onWidthChange = (val) => {
  const s = selected.value
  if (!s || val == null) return
  const h = clampNum(val, plan.gridSize, plan.height - s.y)
  s.h = h
  if (s.shape === 'square') s.w = h
}

const onShapeTypeChange = (type) => {
  const s = selected.value
  if (!s) return
  if (type === 'square') {
    const side = Math.max(s.w, s.h)
    s.w = Math.min(side, plan.width - s.x)
    s.h = Math.min(side, plan.height - s.y)
    s.w = Math.min(s.w, s.h)
    s.h = s.w
  } else if (type === 'parallelogram') {
    s.skew = s.w / 4
  } else if (type === 'trapezoid') {
    s.topW = s.w / 2
  }
}

const onGridSizeChange = () => {
  // 方格变化后不强制移动已有图形，仅影响后续绘制与吸附
}

// ---------- 保存 / 删除 ----------
const sanitize = (s) => {
  const g = plan.gridSize
  s.w = clampNum(Number.isFinite(Number(s.w)) ? Number(s.w) : g, g, plan.width)
  s.h = clampNum(Number.isFinite(Number(s.h)) ? Number(s.h) : g, g, plan.height)
  s.x = clampNum(Number.isFinite(Number(s.x)) ? Number(s.x) : 0, 0, Math.max(0, plan.width - s.w))
  s.y = clampNum(Number.isFinite(Number(s.y)) ? Number(s.y) : 0, 0, Math.max(0, plan.height - s.h))
  if (s.shape === 'parallelogram') s.skew = clampNum(Number(s.skew) || s.w / 4, 0, s.w / 2)
  if (s.shape === 'trapezoid') s.topW = clampNum(Number(s.topW) || s.w / 2, g, s.w)
  return s
}

const save = async () => {
  saving.value = true
  try {
    const res = await savePlan({
      id: plan.id,
      name: plan.name,
      width: plan.width,
      height: plan.height,
      gridSize: plan.gridSize,
      bgImage: plan.bgImage,
      shapes: shapes.value.map(s => sanitize({ ...s }))
    })
    applyPlan(res.data.plan)
    savedTip.value = '已保存'
    if (res.data.skippedDelete && res.data.skippedDelete.length) {
      ElMessage.warning(`以下仓库已配置子仓库，未从系统删除：${res.data.skippedDelete.join('、')}`)
    } else {
      ElMessage.success('平面图已保存')
    }
  } finally {
    saving.value = false
  }
}

const removeSelected = async () => {
  if (!selectIds.value.length) {
    ElMessage.warning('请先选择要删除的仓库图形')
    return
  }
  const names = shapes.value.filter(s => selectIds.value.includes(s.id)).map(s => s.name || s.id)
  try {
    await ElMessageBox.confirm(`确认删除选中的仓库（${names.join('、')}）？`, '删除仓库', { type: 'warning' })
  } catch {
    return
  }
  shapes.value = shapes.value.filter(s => !selectIds.value.includes(s.id))
  selectIds.value = []
  await save()
}

const goStep2 = () => {
  router.push('/shelf')
}

// ---------- 上传 ----------
const doImport = async (file) => {
  if (!file) return
  if (!file.type || !file.type.startsWith('image/')) {
    ElMessage.warning('当前版本支持图片格式图纸（CAD 矢量解析需接入识别服务，可先截图/导出为图片）')
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await importPlanBackground(fd)
    plan.bgImage = res.data.bgImage
    uploadVisible.value = false
    savedTip.value = ''
    ElMessage.success('图纸已导入为背景图，可继续在其上绘制')
  } finally {
    importing.value = false
  }
}

const onDrop = (e) => {
  dragOver.value = false
  doImport(e.dataTransfer?.files?.[0])
}

const onFileChange = (e) => {
  doImport(e.target.files?.[0])
  e.target.value = ''
}

// ---------- 加载 ----------
const applyPlan = (data) => {
  if (!data) return
  plan.id = data.id
  plan.name = data.name || '仓库平面图'
  plan.width = Number(data.width) || 60
  plan.height = Number(data.height) || 40
  plan.gridSize = Number(data.gridSize) || 1
  plan.bgImage = data.bgImage || null
  let list = []
  try {
    const parsed = JSON.parse(data.planData || '{"shapes":[]}')
    list = parsed.shapes || []
  } catch {
    list = []
  }
  shapes.value = list.map(s => makeShape(
    s.shape || 'rect', s.x || 0, s.y || 0, s.w || 5, s.h || 5, {
      id: s.id, warehouseId: s.warehouseId ?? null,
      skew: s.skew, topW: s.topW,
      name: s.name || '', code: s.code || '',
      capacity: s.capacity ?? null, maxWeight: s.maxWeight ?? null, volume: s.volume ?? null,
      status: s.status ?? 1, heightM: s.heightM ?? 15,
      manager: s.manager || '', department: s.department || '', phone: s.phone || '',
      address: s.address || '', remark: s.remark || ''
    }
  )).map(s => {
    if (s.shape === 'parallelogram' && s.skew == null) s.skew = s.w / 4
    if (s.shape === 'trapezoid' && s.topW == null) s.topW = s.w / 2
    return s
  })
}

onMounted(async () => {
  const res = await getPlan()
  applyPlan(res.data)
})
</script>

<style scoped>
.drawing-page {
  --draw-primary: #1890ff;
  height: calc(100vh - 32px);
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

/* ===== 顶部步骤条 ===== */
.step-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 6px;
  padding: 10px 18px;
  margin-bottom: 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}
.step {
  display: flex;
  align-items: center;
  gap: 8px;
}
.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.step-active .step-num {
  background: var(--draw-primary);
  color: #fff;
}
.step-active .step-text {
  color: var(--draw-primary);
  font-weight: 600;
  font-size: 15px;
}
.step-next {
  cursor: pointer;
}
.step-next .step-num {
  background: #f0f0f0;
  color: #999;
}
.step-next .step-text {
  color: #999;
  font-size: 15px;
}
.step-next:hover .step-num {
  background: var(--draw-primary);
  color: #fff;
}
.step-next:hover .step-text {
  color: var(--draw-primary);
}
.step-arrow {
  color: #d9d9d9;
  font-size: 16px;
}
.step-bar-right {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

/* ===== 主体 ===== */
.board-body {
  flex: 1;
  display: flex;
  gap: 10px;
  min-height: 0;
}

/* ===== 左侧工具栏 ===== */
.tool-panel {
  width: 225px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 6px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  height: 38px;
  background: #fff;
  border: 1px solid var(--draw-primary);
  border-radius: 4px;
  color: var(--draw-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.upload-btn:hover {
  background: #e6f7ff;
}
.upload-btn .ico {
  width: 15px;
  height: 15px;
  fill: var(--draw-primary);
}
.field-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.shape-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.shape-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px 6px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #606266;
  transition: all 0.15s;
}
.shape-btn:hover {
  border-color: var(--draw-primary);
}
.shape-btn.active {
  border-color: var(--draw-primary);
  background: #e6f7ff;
  color: var(--draw-primary);
}
.shape-ico {
  width: 34px;
  height: 34px;
}
.shape-ico polygon {
  fill: currentColor;
  opacity: 0.75;
}
.tool-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.15s;
}
.tool-btn:hover {
  border-color: var(--draw-primary);
}
.tool-btn.active {
  border-color: var(--draw-primary);
  background: #e6f7ff;
  color: var(--draw-primary);
}
.panel-spacer {
  flex: 1;
}
.save-btn, .delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  height: 38px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  color: #fff;
  transition: opacity 0.15s;
}
.save-btn {
  background: var(--draw-primary);
}
.save-btn:hover {
  opacity: 0.85;
}
.save-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.delete-btn {
  background: #ff4d4f;
}
.delete-btn:hover {
  opacity: 0.85;
}
.save-btn .ico, .delete-btn .ico {
  width: 15px;
  height: 15px;
  fill: #fff;
}

/* ===== 画布 ===== */
.canvas-wrap {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  position: relative;
}
.board-svg {
  flex: 1;
  min-height: 0;
  width: 100%;
  user-select: none;
  border: 1px solid #e8e8e8;
  background: #fff;
}
.board-svg.cursor-draw {
  cursor: crosshair;
}
.board-svg.cursor-grid {
  cursor: cell;
}
.shape-g {
  cursor: move;
}
.shape-label {
  fill: #333;
  pointer-events: none;
  font-weight: 500;
}
.handle {
  fill: #fff;
  stroke: var(--draw-primary);
  stroke-width: 0.12;
  cursor: nwse-resize;
}
.canvas-scale {
  font-size: 12px;
  color: #999;
  text-align: right;
  padding-top: 6px;
  flex-shrink: 0;
}

/* ===== 右侧属性面板 ===== */
.props-panel {
  width: 305px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  flex-shrink: 0;
}
.panel-arrow {
  transition: transform 0.2s;
  color: #999;
}
.panel-arrow.flipped {
  transform: rotate(-90deg);
}
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
}
.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  padding: 6px 0 8px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 10px;
  margin-top: 2px;
}
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 8px;
}
.field {
  min-width: 0;
}
.field label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}
.field :deep(.el-input-number),
.field :deep(.el-select),
.field :deep(.el-input) {
  width: 100%;
}
.field-full {
  grid-column: 1 / -1;
}
.more-collapse {
  margin-top: 14px;
  border: none;
}
.more-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: #333;
  font-weight: 600;
}
.more-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}
.more-collapse .field {
  margin-bottom: 8px;
}

/* ===== 上传弹窗 ===== */
.drop-zone {
  border: 1.5px dashed #d9d9d9;
  border-radius: 6px;
  padding: 34px 20px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.drop-zone:hover, .drop-zone.dragging {
  border-color: #1890ff;
  background: #f0f9ff;
}
.drop-ico {
  width: 46px;
  height: 46px;
  fill: #1890ff;
}
.drop-zone p {
  margin: 0;
  font-size: 13px;
  color: #666;
}
.drop-tip {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
  text-align: center;
}
</style>
