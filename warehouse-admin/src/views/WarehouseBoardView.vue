<template>
  <div class="board-page">
    <!-- 头部统计 -->
    <div class="bd-header">
      <button class="bd-back" @click="goBack">← 返回</button>
      <div class="bd-title-wrap">
        <div class="bd-title">{{ board.warehouse?.name }} - 平面图与数据看板</div>
        <div class="bd-code">编号: {{ board.warehouse?.code }}</div>
      </div>
      <div class="bd-stats">
        <div class="bd-stat"><span class="bd-stat-label">大小</span><b>{{ board.warehouse?.area ?? '-' }}m²</b></div>
        <div class="bd-stat"><span class="bd-stat-label">总库存</span><b>{{ board.totalStock ?? 0 }}</b></div>
        <div class="bd-stat"><span class="bd-stat-label">可用库存</span><b>{{ board.availableStock ?? 0 }}</b></div>
        <div class="bd-stat"><span class="bd-stat-label">总重量</span><b>{{ fmt(board.totalWeight) }}kg</b></div>
        <div class="bd-stat"><span class="bd-stat-label">总体积</span><b>{{ fmt(board.totalVolume) }}m³</b></div>
        <div class="bd-stat"><span class="bd-stat-label">使用率</span><b>{{ board.usageRate ?? 0 }}%</b></div>
      </div>
    </div>

    <!-- 模式切换 + 搜索 -->
    <div class="bd-toolbar">
      <span class="bd-toolbar-label">信息显示:</span>
      <button
        v-for="m in modes"
        :key="m.key"
        class="bd-mode-btn"
        :class="{ active: mode === m.key }"
        @click="mode = m.key"
      >{{ m.label }}</button>
      <div class="bd-search">
        <span class="bd-search-ic">🔍</span>
        <input v-model="keyword" :placeholder="currentMode.placeholder" />
      </div>
    </div>

    <div class="bd-body">
      <!-- 货架网格区 -->
      <div class="bd-grid-area" @wheel.ctrl.prevent="onZoom">
        <div class="bd-grid-scale" :style="{ transform: `scale(${scale})` }">
          <div v-for="shelf in board.shelves || []" :key="shelf.id" class="shelf-block">
            <div class="shelf-title">
              货架 {{ shelf.name }} - {{ shelf.layerCount }}层{{ colsPerLayer(shelf) }}列
              <span class="shelf-type">{{ shelf.type === 2 ? '双排' : '单排' }}</span>
            </div>
            <div class="cell-grid" :style="{ gridTemplateColumns: `repeat(${colsPerLayer(shelf)}, 46px)` }">
              <template v-for="(row, li) in matrixOf(shelf)" :key="li">
                <div
                  v-for="(cell, ci) in row"
                  :key="ci"
                  class="cell"
                  :style="cellStyle(cell, shelf, li, ci)"
                  @mouseenter="showTip($event, cell, shelf, li, ci)"
                  @mouseleave="tip = null"
                >{{ cellLabel(cell, li, ci) }}</div>
              </template>
            </div>
          </div>
        </div>

        <!-- 悬停货位详情 -->
        <div v-if="tip" class="cell-tip" :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
          <template v-if="tip.cell && tip.cell.goodsName">
            <div class="cell-tip-title">{{ tip.cell.goodsName }} - {{ tip.cell.category || '未分类' }}</div>
            <div class="cell-tip-row">数量: {{ tip.cell.quantity }}{{ tip.cell.unit || '件' }}</div>
            <div class="cell-tip-row">体积: {{ tip.cell.volume ?? '-' }}m³</div>
            <div class="cell-tip-row">重量: {{ tip.cell.weight ?? '-' }}kg</div>
            <div class="cell-tip-row">位置: {{ tip.layer + 1 }}-{{ tip.col + 1 }}</div>
          </template>
          <template v-else>
            <div class="cell-tip-title">空闲货位</div>
            <div class="cell-tip-row">位置: {{ tip.layer + 1 }}-{{ tip.col + 1 }}</div>
          </template>
        </div>
      </div>

      <!-- 右侧数据看板 -->
      <div class="bd-side">
        <div class="side-title">数据看板</div>

        <div class="side-card">
          <div class="side-card-title">存货种类占比</div>
          <div class="donut-wrap">
            <svg viewBox="0 0 42 42" class="donut">
              <circle cx="21" cy="21" r="15.9155" fill="none" stroke="#f3f4f6" stroke-width="6"></circle>
              <circle
                v-for="(seg, i) in donutSegments"
                :key="i"
                cx="21" cy="21" r="15.9155" fill="none"
                :stroke="seg.color" stroke-width="6"
                :stroke-dasharray="`${seg.pct} ${100 - seg.pct}`"
                :stroke-dashoffset="25 - seg.offset"
                class="donut-seg"
                @mouseenter="donutHover = seg"
                @mouseleave="donutHover = null"
              ></circle>
              <text x="21" y="21" text-anchor="middle" dominant-baseline="central" class="donut-text">
                {{ donutHover ? `占比: ${donutHover.pct}%` : '' }}
              </text>
            </svg>
            <div class="donut-legend">
              <div v-for="(seg, i) in donutSegments" :key="i" class="donut-legend-row">
                <i :style="{ background: seg.color }"></i>
                <span class="dl-name">{{ seg.category }}</span>
                <span class="dl-pct">{{ seg.pct }}%</span>
              </div>
              <div v-if="!donutSegments.length" class="dl-empty">暂无库存数据</div>
            </div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">每日进出库流量</div>
          <svg :viewBox="`0 0 340 180`" class="bars">
            <line x1="34" y1="10" x2="34" y2="140" stroke="#e5e7eb" />
            <line x1="34" y1="140" x2="334" y2="140" stroke="#e5e7eb" />
            <g v-for="(tick, i) in barTicks" :key="'t' + i">
              <line x1="34" :y1="tick.y" x2="334" :y2="tick.y" stroke="#f3f4f6" />
              <text x="30" :y="tick.y + 3" text-anchor="end" class="bar-tick">{{ tick.v }}</text>
            </g>
            <g v-for="(d, i) in board.dailyFlow || []" :key="i">
              <rect :x="44 + i * 42" :y="140 - barH(d.inQty)" width="12" :height="barH(d.inQty)" rx="1.5" fill="#34d399" />
              <rect :x="58 + i * 42" :y="140 - barH(d.outQty)" width="12" :height="barH(d.outQty)" rx="1.5" fill="#fbbf24" />
              <text :x="58 + i * 42" y="156" text-anchor="middle" class="bar-label">{{ d.weekday }}</text>
            </g>
          </svg>
          <div class="bars-legend">
            <span><i style="background:#34d399"></i>入库量</span>
            <span><i style="background:#fbbf24"></i>出库量</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：图例 + 缩放 -->
    <div class="bd-footer">
      <div class="bd-legend">
        <span class="lg-label">图例</span>
        <span v-for="item in legendItems" :key="item.label" class="lg-item">
          <i :style="{ background: item.color }"></i>{{ item.label }}
        </span>
      </div>
      <div class="bd-zoom">
        <span>缩放: {{ Math.round(scale * 100) }}%</span>
        <span class="zoom-hint">Ctrl+滚轮缩放</span>
        <button class="zoom-reset" @click="scale = 1">重置</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { panoramaBoard } from '../api/panorama'

const route = useRoute()
const router = useRouter()

const board = ref({})
const mode = ref('code')
const keyword = ref('')
const scale = ref(1)
const tip = ref(null)
const donutHover = ref(null)

const modes = [
  { key: 'code', label: '编号', placeholder: '搜索编号...' },
  { key: 'qty', label: '当前货量', placeholder: '搜索当前货量...' },
  { key: 'category', label: '存货种类', placeholder: '搜索存货种类...' },
  { key: 'volume', label: '体积', placeholder: '搜索编号...' },
  { key: 'weight', label: '重量', placeholder: '搜索编号...' },
  { key: 'expiry', label: '保质期', placeholder: '搜索编号...' }
]

const CATEGORY_PALETTE = ['#3b82f6', '#22c55e', '#ef4444', '#f59e0b', '#8b5cf6', '#06b6d4', '#ec4899']
const STATUS_COLORS = { empty: '#22c55e', normal: '#3b82f6', expired: '#ef4444', shipping: '#f59e0b' }
const HEAT_COLORS = ['#22c55e', '#facc15', '#fb923c', '#ef4444']

const currentMode = computed(() => modes.find((m) => m.key === mode.value))

const categoryColorMap = computed(() => {
  const map = {}
  ;(board.value.categoryStats || []).forEach((c, i) => {
    map[c.category] = CATEGORY_PALETTE[i % CATEGORY_PALETTE.length]
  })
  return map
})

const donutSegments = computed(() => {
  const list = board.value.categoryStats || []
  const total = list.reduce((s, c) => s + Number(c.qty || 0), 0)
  if (!total) return []
  let offset = 0
  return list.map((c) => {
    const pct = Math.round((Number(c.qty) * 100) / total)
    const seg = { category: c.category, pct, offset, color: categoryColorMap.value[c.category] }
    offset += pct
    return seg
  })
})

const barMax = computed(() => {
  let max = 0
  for (const d of board.value.dailyFlow || []) {
    max = Math.max(max, Number(d.inQty || 0), Number(d.outQty || 0))
  }
  return max || 1
})

const barTicks = computed(() => {
  const step = Math.ceil(barMax.value / 4 / 5) * 5 || 1
  return [0, 1, 2, 3, 4].map((i) => ({ v: step * i, y: 140 - (step * i * 120) / (step * 4) }))
})

const barH = (v) => (Number(v || 0) * 120) / (barTicks.value[4].v || 1)

const legendItems = computed(() => {
  switch (mode.value) {
    case 'code':
      return [
        { label: '空闲', color: STATUS_COLORS.empty },
        { label: '有货', color: STATUS_COLORS.normal },
        { label: '过期', color: STATUS_COLORS.expired },
        { label: '出货中', color: STATUS_COLORS.shipping }
      ]
    case 'category':
      return Object.entries(categoryColorMap.value).map(([label, color]) => ({ label, color }))
    case 'expiry':
      return [
        { label: '已过期', color: '#ef4444' },
        { label: '≤30天', color: '#fb923c' },
        { label: '≤90天', color: '#facc15' },
        { label: '>90天', color: '#22c55e' }
      ]
    default:
      return [
        { label: '低', color: HEAT_COLORS[0] },
        { label: '中', color: HEAT_COLORS[1] },
        { label: '较高', color: HEAT_COLORS[2] },
        { label: '高', color: HEAT_COLORS[3] }
      ]
  }
})

onMounted(async () => {
  const res = await panoramaBoard(route.params.id)
  board.value = res.data || {}
})

const goBack = () => router.push('/panorama')

const colsPerLayer = (shelf) =>
  Math.max(1, (shelf.rowCount || 1) * (shelf.bayCount || 1) * (shelf.colCount || 1))

const matrixOf = (shelf) => {
  const layers = Math.max(1, shelf.layerCount || 1)
  const cols = colsPerLayer(shelf)
  const matrix = Array.from({ length: layers }, () => Array(cols).fill(null))
  for (const cell of shelf.cells || []) {
    const flat =
      ((cell.rowIndex - 1) * (shelf.bayCount || 1) + (cell.bayIndex - 1)) * (shelf.colCount || 1) +
      (cell.colIndex - 1)
    const li = (cell.layerIndex || 1) - 1
    if (li >= 0 && li < layers && flat >= 0 && flat < cols) matrix[li][flat] = cell
  }
  return matrix
}

const heatLevel = (value, max) => {
  if (!max) return 0
  const r = value / max
  return r <= 0.25 ? 0 : r <= 0.5 ? 1 : r <= 0.75 ? 2 : 3
}

const maxOf = (field) => {
  let max = 0
  for (const shelf of board.value.shelves || []) {
    for (const cell of shelf.cells || []) {
      max = Math.max(max, Number(cell[field] || 0))
    }
  }
  return max
}

const expiryDays = (cell) => {
  if (!cell.expiryDate) return null
  const days = Math.ceil((new Date(cell.expiryDate) - Date.now()) / 86400000)
  return days
}

const matchKeyword = (cell) => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return null
  if (!cell) return false
  const text = `${cell.locationCode || ''} ${cell.goodsName || ''} ${cell.category || ''}`.toLowerCase()
  return text.includes(kw)
}

const cellStyle = (cell, shelf, li, ci) => {
  const matched = matchKeyword(cell)
  if (matched === true) return { background: '#3b82f6', color: '#fff' }
  if (matched === false) return { background: '#f9fafb', color: '#d1d5db', borderColor: '#f3f4f6' }

  if (!cell || !cell.goodsName) {
    return mode.value === 'code'
      ? { background: STATUS_COLORS.empty, color: '#fff' }
      : { background: '#f9fafb', color: '#9ca3af', borderColor: '#f3f4f6' }
  }
  switch (mode.value) {
    case 'code':
      return { background: STATUS_COLORS[cell.cellStatus] || STATUS_COLORS.normal, color: '#fff' }
    case 'category':
      return { background: categoryColorMap.value[cell.category] || '#6b7280', color: '#fff' }
    case 'qty':
      return { background: HEAT_COLORS[heatLevel(Number(cell.quantity || 0), maxOf('quantity'))], color: '#fff' }
    case 'volume':
      return { background: HEAT_COLORS[heatLevel(Number(cell.volume || 0), maxOf('volume'))], color: '#fff' }
    case 'weight':
      return { background: HEAT_COLORS[heatLevel(Number(cell.weight || 0), maxOf('weight'))], color: '#fff' }
    case 'expiry': {
      const d = expiryDays(cell)
      if (d === null) return { background: '#e5e7eb', color: '#6b7280' }
      const color = d < 0 ? '#ef4444' : d <= 30 ? '#fb923c' : d <= 90 ? '#facc15' : '#22c55e'
      return { background: color, color: '#fff' }
    }
    default:
      return {}
  }
}

const cellLabel = (cell, li, ci) => {
  if (mode.value === 'code') return `${li + 1}-${ci + 1}`
  if (!cell || !cell.goodsName) return '—'
  switch (mode.value) {
    case 'qty':
      return cell.quantity
    case 'category':
      return (cell.category || '').length > 2 ? cell.category.slice(0, 2) : cell.category || '—'
    case 'volume':
      return cell.volume ?? '—'
    case 'weight':
      return cell.weight ?? '—'
    case 'expiry': {
      const d = expiryDays(cell)
      return d === null ? '—' : d < 0 ? '逾' : d
    }
    default:
      return ''
  }
}

const showTip = (e, cell, shelf, li, ci) => {
  const rect = e.currentTarget.getBoundingClientRect()
  tip.value = {
    x: Math.min(rect.right + 8, window.innerWidth - 200),
    y: Math.max(rect.top - 10, 8),
    cell,
    layer: li,
    col: ci
  }
}

const onZoom = (e) => {
  const next = e.deltaY < 0 ? scale.value * 1.1 : scale.value / 1.1
  scale.value = Math.min(2.5, Math.max(0.4, next))
}

const fmt = (v) => (v === null || v === undefined ? '0' : Number(v).toString())
</script>

<style scoped>
.board-page {
  min-height: 100vh;
  background: #fff;
  padding: 20px 28px 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* 头部 */
.bd-header { display: flex; align-items: flex-start; gap: 20px; flex-wrap: wrap; }
.bd-back {
  border: none;
  background: none;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.bd-back:hover { background: #f3f4f6; color: #1f2937; }
.bd-title { font-size: 17px; font-weight: 700; color: #1f2937; }
.bd-code { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.bd-stats { display: flex; gap: 28px; margin-left: auto; }
.bd-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.bd-stat-label { font-size: 11px; color: #9ca3af; }
.bd-stat b { font-size: 15px; color: #1f2937; font-weight: 700; }

/* 工具条 */
.bd-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 12px;
  flex-wrap: wrap;
}
.bd-toolbar-label { font-size: 12px; color: #6b7280; margin-right: 4px; }
.bd-mode-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #4b5563;
  font-size: 12px;
  padding: 5px 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.bd-mode-btn:hover { border-color: #93c5fd; color: #2563eb; }
.bd-mode-btn.active { background: #3b82f6; border-color: #3b82f6; color: #fff; }
.bd-search {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 5px 10px;
  background: #fff;
}
.bd-search-ic { font-size: 12px; color: #9ca3af; }
.bd-search input { border: none; outline: none; font-size: 12px; width: 160px; color: #1f2937; }

/* 主体 */
.bd-body { display: flex; gap: 16px; flex: 1; min-height: 0; }
.bd-grid-area {
  flex: 1;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: auto;
  position: relative;
  background: #fff;
  padding: 16px;
}
.bd-grid-scale { transform-origin: 0 0; transition: transform 0.1s; width: max-content; }

.shelf-block { margin-bottom: 24px; }
.shelf-title { font-size: 13px; font-weight: 600; color: #4b5563; margin-bottom: 8px; }
.shelf-type { font-size: 11px; color: #9ca3af; font-weight: 400; margin-left: 6px; }
.cell-grid { display: grid; gap: 3px; }
.cell {
  height: 24px;
  border-radius: 3px;
  border: 1px solid transparent;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: default;
  white-space: nowrap;
  overflow: hidden;
}

.cell-tip {
  position: fixed;
  z-index: 50;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  padding: 10px 12px;
  font-size: 12px;
  color: #4b5563;
  pointer-events: none;
  min-width: 150px;
}
.cell-tip-title { font-weight: 700; color: #1f2937; margin-bottom: 4px; }
.cell-tip-row { line-height: 1.9; }

/* 右侧看板 */
.bd-side {
  width: 380px;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  overflow-y: auto;
}
.side-title { font-size: 14px; font-weight: 700; color: #1f2937; margin-bottom: 12px; }
.side-card { border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.side-card-title { font-size: 12px; font-weight: 600; color: #6b7280; margin-bottom: 10px; }

.donut-wrap { display: flex; align-items: center; gap: 12px; }
.donut { width: 150px; height: 150px; flex-shrink: 0; }
.donut-seg { cursor: pointer; transition: stroke-width 0.15s; }
.donut-seg:hover { stroke-width: 8; }
.donut-text { font-size: 4.5px; fill: #4b5563; font-weight: 600; }
.donut-legend { flex: 1; }
.donut-legend-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
  line-height: 2;
}
.donut-legend-row i { width: 9px; height: 9px; border-radius: 2px; }
.dl-name { flex: 1; }
.dl-pct { color: #9ca3af; }
.dl-empty { font-size: 12px; color: #9ca3af; }

.bars { width: 100%; }
.bar-tick { font-size: 9px; fill: #9ca3af; }
.bar-label { font-size: 9px; fill: #6b7280; }
.bars-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  font-size: 11px;
  color: #6b7280;
  margin-top: 4px;
}
.bars-legend i { display: inline-block; width: 9px; height: 9px; border-radius: 2px; margin-right: 4px; }

/* 底部 */
.bd-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px 0;
  font-size: 12px;
  color: #6b7280;
}
.bd-legend { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.lg-label { color: #9ca3af; }
.lg-item { display: flex; align-items: center; gap: 5px; }
.lg-item i { width: 10px; height: 10px; border-radius: 2px; display: inline-block; }
.bd-zoom { display: flex; align-items: center; gap: 10px; }
.zoom-hint { color: #9ca3af; }
.zoom-reset {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
  color: #4b5563;
  padding: 2px 10px;
  cursor: pointer;
}
.zoom-reset:hover { border-color: #93c5fd; color: #2563eb; }
</style>
