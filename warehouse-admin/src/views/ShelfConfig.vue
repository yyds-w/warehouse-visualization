<template>
  <div class="shelf-config-page">
    <!-- 顶部：标题 + 仓库选择 + 批量导入 -->
    <div class="page-header">
      <div class="page-title">配置货架信息</div>
      <div class="header-controls">
        <span class="sel-label">仓库</span>
        <el-select v-model="warehouseId" style="width: 160px" placeholder="选择仓库" @change="onWarehouseChange">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
        <span class="sel-label">子仓库</span>
        <el-select v-model="subWarehouseId" style="width: 160px" placeholder="选择子仓库" @change="onSubChange">
          <el-option v-for="s in subWarehouses" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" plain :icon="Upload" @click="openImport">批量导入</el-button>
      </div>
    </div>

    <div class="main-body">
      <!-- 左侧：货架画布 -->
      <div v-loading="loading" class="canvas-wrap">
        <svg
          ref="svgRef"
          class="canvas-svg"
          :class="{ panning: isPanning }"
          @wheel.prevent="onWheel"
          @mousedown="onSvgMouseDown"
          @click="onSvgClick"
        >
          <g :transform="viewTransform">
            <g
              v-for="g in groups"
              :key="g.key"
              class="grp"
              :transform="groupTransform(g)"
              @click.stop="onGroupClick(g)"
            >
              <rect
                class="grp-box"
                :class="{ selected: g.key === selectedKey }"
                :width="groupW(g)"
                :height="groupH(g)"
                rx="2"
              />
              <g v-for="(u, ui) in g.units" :key="ui + '-' + u.code" :transform="unitTransform(g, ui)">
                <text class="unit-code" :x="GEO.pad" :y="unitH(u) / 2 + 4">{{ u.code }}</text>
                <g :transform="`translate(${GEO.pad + GEO.labelW} 0)`">
                  <rect class="unit-box" :width="unitW(u)" :height="unitH(u)" />
                  <path class="unit-grid" :d="gridPath(u)" />
                  <template v-if="showCells">
                    <g v-for="li in u.layerCount" :key="'L' + li">
                      <g v-for="ci in u.colCount" :key="'C' + ci">
                        <rect
                          v-if="cellTint(u, li, ci)"
                          class="cell-tint"
                          :x="(ci - 1) * GEO.cellW"
                          :y="(li - 1) * GEO.cellH"
                          :width="GEO.cellW"
                          :height="GEO.cellH"
                        />
                        <text
                          v-if="showCellText"
                          class="cell-text"
                          :x="(ci - 0.5) * GEO.cellW"
                          :y="(li - 0.5) * GEO.cellH + 3.5"
                        >{{ cellText(u, li, ci) }}</text>
                      </g>
                    </g>
                  </template>
                </g>
                <text
                  class="unit-spec"
                  :x="GEO.pad + GEO.labelW + maxUnitW(g) + 8"
                  :y="unitH(u) / 2 + 4"
                >{{ u.layerCount }}层{{ u.colCount }}列</text>
              </g>
              <line
                v-if="g.units.length === 2"
                class="unit-divider"
                :x1="GEO.pad + GEO.labelW"
                :y1="GEO.pad + unitH(g.units[0]) + GEO.unitGap / 2"
                :x2="GEO.pad + GEO.labelW + maxUnitW(g)"
                :y2="GEO.pad + unitH(g.units[0]) + GEO.unitGap / 2"
              />
              <text class="grp-type" :x="groupW(g) / 2" :y="groupH(g) - 6">
                {{ g.units.length === 2 ? '双排' : '单排' }}
              </text>
            </g>
          </g>
        </svg>

        <div v-if="!groups.length && !loading" class="canvas-empty">
          <el-empty description="画布为空：在右侧配置货架排布与属性，点击「添加」生成货架" :image-size="88" />
        </div>

        <div class="canvas-toolbar">
          <span class="zoom-hint">鼠标滚轮 缩放视图 · 按住拖动平移 · 点击选择货架组</span>
          <div class="zoom-ctrl">
            <el-button size="small" text :icon="ZoomOut" @click="zoomStep(-1)" />
            <span class="zoom-val">{{ Math.round(zoom * 100) }}%</span>
            <el-button size="small" text :icon="ZoomIn" @click="zoomStep(1)" />
            <el-button size="small" @click="fitView">重置视角</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：配置面板 -->
      <aside class="config-panel">
        <el-scrollbar class="panel-scroll">
          <section class="panel-section">
            <h3 class="section-title">货架排布</h3>
            <el-radio-group v-model="panel.mode" class="mode-radio">
              <el-radio :value="1">单排货架</el-radio>
              <el-radio :value="2">双排货架</el-radio>
            </el-radio-group>
            <p class="section-tip">
              {{ panel.mode === 2 ? '每个双排货架组由2个货架单元组成（上排 01 + 下排 02）' : '每个货架组包含1个货架单元' }}
            </p>
            <div class="two-col">
              <div class="field">
                <label>货架行数</label>
                <el-input-number v-model="panel.rowCount" :min="1" :max="50" controls-position="right" />
              </div>
              <div class="field">
                <label>货架排数</label>
                <el-input-number v-model="panel.bayCount" :min="1" :max="50" controls-position="right" />
              </div>
            </div>
            <p class="section-tip calc">{{ calcText }}</p>
          </section>

          <section class="panel-section">
            <h3 class="section-title">货架属性</h3>
            <div class="two-col">
              <div class="field">
                <label>货架层数</label>
                <el-input-number v-model="panel.layerCount" :min="1" :max="50" controls-position="right" />
              </div>
              <div class="field">
                <label>货架列数</label>
                <el-input-number v-model="panel.colCount" :min="1" :max="50" controls-position="right" />
              </div>
            </div>
            <div class="field">
              <label>{{ panel.mode === 2 ? '上排货架 (01)' : '货架名称' }}</label>
              <el-input v-model="panel.nameTop" :placeholder="panel.mode === 2 ? '如 A01' : '请输入唯一的货架编码名称'" />
            </div>
            <div v-if="panel.mode === 2" class="field">
              <label>下排货架 (02)</label>
              <el-input v-model="panel.nameBottom" placeholder="如 A02" />
            </div>
            <p class="section-tip">{{ selectionTip }}</p>
            <div class="btn-grid">
              <el-button @click="resetPanel">重置</el-button>
              <el-button type="primary" @click="addGroups">添加</el-button>
              <el-button :disabled="!selectedGroup" @click="applyModify">修改组</el-button>
              <el-button type="danger" plain :disabled="!selectedGroup" @click="removeSelectedGroup">删除组</el-button>
            </div>
          </section>

          <section class="panel-section">
            <h3 class="section-title">货架信息</h3>
            <p class="section-tip">选择要在货位网格中显示的信息类型</p>
            <div class="chip-group">
              <button
                v-for="m in displayModes"
                :key="m.value"
                type="button"
                class="chip"
                :class="{ active: displayMode === m.value }"
                @click="displayMode = m.value"
              >{{ m.label }}</button>
            </div>
          </section>
        </el-scrollbar>

        <div class="panel-footer">
          <el-button @click="cancelChanges">取消</el-button>
          <el-button type="primary" :loading="saving" :disabled="!subWarehouseId" @click="saveAll">保存</el-button>
        </div>
      </aside>
    </div>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入货架" width="720px" :close-on-click-modal="false">
      <div class="import-head">
        <el-button :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
        <el-upload
          accept=".xlsx,.xls,.csv"
          :show-file-list="false"
          :auto-upload="false"
          :on-change="onImportFile"
        >
          <el-button type="primary" :icon="Upload">选择 Excel / CSV 文件</el-button>
        </el-upload>
        <span v-if="importFileName" class="import-file">{{ importFileName }}</span>
      </div>
      <p class="import-tip">
        模板列：货架编码、层数、列数、行、排、位置（上/下）。同一「行 + 排」的上下两条记录组成一个双排货架组，仅一条则为单排货架。
      </p>
      <el-alert v-if="importErrors.length" type="error" :closable="false" class="import-alert">
        <div v-for="(e, i) in importErrors.slice(0, 5)" :key="i">{{ e }}</div>
        <div v-if="importErrors.length > 5">…… 共 {{ importErrors.length }} 个问题</div>
      </el-alert>
      <el-table v-if="importRows.length" :data="importRows" border size="small" max-height="280">
        <el-table-column prop="code" label="货架编码" width="120" />
        <el-table-column prop="layerCount" label="层数" width="70" align="center" />
        <el-table-column prop="colCount" label="列数" width="70" align="center" />
        <el-table-column prop="row" label="行" width="60" align="center" />
        <el-table-column prop="col" label="排" width="60" align="center" />
        <el-table-column prop="posLabel" label="位置" width="70" align="center" />
        <el-table-column label="所属组" min-width="120">
          <template #default="{ row }">第 {{ row.row }} 行 · 第 {{ row.col }} 排（{{ row.groupType }}）</template>
        </el-table-column>
      </el-table>
      <div v-else class="import-empty">尚未解析到有效数据，请先下载模板并按格式填写后上传</div>
      <div v-if="importRows.length" class="import-mode">
        <el-radio-group v-model="importMode">
          <el-radio value="replace">替换现有画布</el-radio>
          <el-radio value="append">追加到现有画布下方</el-radio>
        </el-radio-group>
      </div>
      <p class="import-tip warn">注意：替换模式下已有库存的货架会阻止保存，请先清空相关库存。</p>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!importRows.length" @click="confirmImport">
          确认导入（{{ importGroupsCount }} 组）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Download, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { listWarehouses, listSubWarehouses } from '../api/base'
import { listShelvesBySub, saveAllShelves, listLocationsBySub } from '../api/shelf'

/* ---------- 基础状态 ---------- */
const warehouses = ref([])
const subWarehouses = ref([])
const warehouseId = ref(null)
const subWarehouseId = ref(null)
const loading = ref(false)
const saving = ref(false)

// 画布工作副本：groups = [{ key, type(1单排/2双排), row, col, units:[{id,code,name,layerCount,colCount,status}] }]
const groups = ref([])
const removedIds = ref([])
const selectedKey = ref(null)
const locMap = ref({})

const panel = reactive({
  mode: 1,
  rowCount: 1,
  bayCount: 1,
  layerCount: 1,
  colCount: 1,
  nameTop: '',
  nameBottom: ''
})

const displayMode = ref('default')
const displayModes = [
  { value: 'default', label: '默认' },
  { value: 'code', label: '编号' },
  { value: 'qty', label: '当前货量' },
  { value: 'kinds', label: '存货种类' },
  { value: 'volume', label: '体积' },
  { value: 'weight', label: '重量' }
]

/* ---------- 画布几何 ---------- */
const GEO = { cellW: 24, cellH: 16, pad: 8, labelW: 34, specW: 64, unitGap: 6, bottomH: 20, rowGap: 34, colGap: 54 }

const unitW = (u) => u.colCount * GEO.cellW
const unitH = (u) => u.layerCount * GEO.cellH
const maxUnitW = (g) => Math.max(...g.units.map(unitW))
const groupW = (g) => GEO.pad * 2 + GEO.labelW + maxUnitW(g) + GEO.specW
const groupH = (g) =>
  GEO.pad * 2 + g.units.reduce((s, u) => s + unitH(u), 0) + (g.units.length - 1) * GEO.unitGap + GEO.bottomH

const layout = computed(() => {
  const rowH = []
  const colW = []
  for (const g of groups.value) {
    rowH[g.row] = Math.max(rowH[g.row] || 0, groupH(g))
    colW[g.col] = Math.max(colW[g.col] || 0, groupW(g))
  }
  const rowY = []
  let y = 0
  for (let r = 0; r < rowH.length; r++) {
    rowY[r] = y
    y += (rowH[r] || 0) + GEO.rowGap
  }
  const colX = []
  let x = 0
  for (let c = 0; c < colW.length; c++) {
    colX[c] = x
    x += (colW[c] || 0) + GEO.colGap
  }
  return { rowY, colX, width: Math.max(0, x - GEO.colGap), height: Math.max(0, y - GEO.rowGap) }
})

const groupTransform = (g) => `translate(${layout.value.colX[g.col] || 0} ${layout.value.rowY[g.row] || 0})`

const unitOffset = (g, ui) => {
  let y = GEO.pad
  for (let i = 0; i < ui; i++) y += unitH(g.units[i]) + GEO.unitGap
  return y
}
const unitTransform = (g, ui) => `translate(0 ${unitOffset(g, ui)})`

const gridPath = (u) => {
  const w = unitW(u)
  const h = unitH(u)
  let d = ''
  for (let c = 1; c < u.colCount; c++) d += `M${c * GEO.cellW} 0V${h}`
  for (let l = 1; l < u.layerCount; l++) d += `M0 ${l * GEO.cellH}H${w}`
  return d || 'M0 0'
}

/* ---------- 缩放 / 平移 ---------- */
const svgRef = ref(null)
const zoom = ref(1)
const panX = ref(0)
const panY = ref(0)
const isPanning = ref(false)
let dragState = null
let wasDragged = false

const viewTransform = computed(() => `translate(${panX.value} ${panY.value}) scale(${zoom.value})`)

const clampZoom = (z) => Math.min(4, Math.max(0.15, z))

const onWheel = (e) => {
  const svg = svgRef.value
  if (!svg) return
  const rect = svg.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const factor = e.deltaY < 0 ? 1.15 : 1 / 1.15
  const nz = clampZoom(zoom.value * factor)
  panX.value = mx - (mx - panX.value) * (nz / zoom.value)
  panY.value = my - (my - panY.value) * (nz / zoom.value)
  zoom.value = nz
}

const zoomStep = (dir) => {
  const svg = svgRef.value
  const nz = clampZoom(zoom.value * (dir > 0 ? 1.25 : 0.8))
  if (!svg) {
    zoom.value = nz
    return
  }
  const rect = svg.getBoundingClientRect()
  const mx = rect.width / 2
  const my = rect.height / 2
  panX.value = mx - (mx - panX.value) * (nz / zoom.value)
  panY.value = my - (my - panY.value) * (nz / zoom.value)
  zoom.value = nz
}

const fitView = () => {
  const svg = svgRef.value
  if (!svg) {
    zoom.value = 1
    panX.value = 0
    panY.value = 0
    return
  }
  const W = svg.clientWidth || 800
  const H = svg.clientHeight || 600
  const { width, height } = layout.value
  if (!width || !height) {
    zoom.value = 1
    panX.value = 0
    panY.value = 0
    return
  }
  const z = clampZoom(Math.min((W - 48) / width, (H - 72) / height))
  zoom.value = z
  panX.value = (W - width * z) / 2
  panY.value = (H - height * z) / 2 + 12
}

const onSvgMouseDown = (e) => {
  dragState = { x: e.clientX, y: e.clientY, px: panX.value, py: panY.value, moved: false }
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragUp)
}

const onDragMove = (e) => {
  if (!dragState) return
  const dx = e.clientX - dragState.x
  const dy = e.clientY - dragState.y
  if (Math.abs(dx) + Math.abs(dy) > 3) {
    dragState.moved = true
    isPanning.value = true
  }
  if (dragState.moved) {
    panX.value = dragState.px + dx
    panY.value = dragState.py + dy
  }
}

const onDragUp = () => {
  wasDragged = !!(dragState && dragState.moved)
  dragState = null
  isPanning.value = false
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragUp)
  setTimeout(() => { wasDragged = false }, 0)
}

const cleanupDrag = () => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragUp)
}

onUnmounted(cleanupDrag)

const onSvgClick = () => {
  if (!wasDragged) selectedKey.value = null
}

const onGroupClick = (g) => {
  if (wasDragged) return
  selectGroup(g.key)
}

/* ---------- 选中与面板 ---------- */
const selectedGroup = computed(() => groups.value.find(g => g.key === selectedKey.value) || null)

const calcText = computed(() => {
  const n = panel.rowCount * panel.bayCount
  return panel.mode === 2
    ? `共计双排货架组：${n} 个，货架单元：${n * 2} 个`
    : `共计单排货架：${n} 个`
})

const selectionTip = computed(() => {
  const g = selectedGroup.value
  if (!g) return '未选中货架组：点击画布中的货架组后可修改或删除'
  return g.units.length === 2
    ? '当前选中双排货架组，包含2个货架单元'
    : '当前选中单排货架组，包含1个货架单元'
})

const selectGroup = (key) => {
  selectedKey.value = key
  const g = groups.value.find(x => x.key === key)
  if (!g) return
  panel.mode = g.type
  panel.layerCount = g.units[0].layerCount
  panel.colCount = g.units[0].colCount
  panel.nameTop = g.units[0].code
  panel.nameBottom = g.units[1] ? g.units[1].code : ''
}

const resetPanel = () => {
  selectedKey.value = null
  panel.mode = 1
  panel.rowCount = 1
  panel.bayCount = 1
  panel.layerCount = 1
  panel.colCount = 1
  panel.nameTop = ''
  panel.nameBottom = ''
}

/* ---------- 货位信息显示 ---------- */
const totalCells = computed(() =>
  groups.value.reduce((s, g) => s + g.units.reduce((t, u) => t + u.layerCount * u.colCount, 0), 0)
)
// 库存色块便宜，低缩放也画；文字节点重，仅在单元足够大时渲染
const showCells = computed(() => displayMode.value !== 'default' && totalCells.value <= 20000)
const showCellText = computed(() => showCells.value && totalCells.value <= 5000 && zoom.value * GEO.cellW >= 18)

const fmtNum = (v) => {
  const n = Number(v)
  if (!Number.isFinite(n)) return '—'
  return n >= 100 ? String(Math.round(n)) : String(Math.round(n * 10) / 10)
}

const cellText = (u, li, ci) => {
  const loc = locMap.value[u.id]?.[`${li}:${ci}`]
  if (displayMode.value === 'code') {
    if (!loc) return `L${li}C${ci}`
    const parts = [`L${loc.layerIndex ?? li}`]
    if ((loc.rowIndex ?? 1) > 1) parts.push(`R${loc.rowIndex}`)
    parts.push(`C${loc.colIndex ?? ci}`)
    return parts.join('')
  }
  if (!loc) return '—'
  switch (displayMode.value) {
    case 'qty': return String(loc.quantity ?? 0)
    case 'kinds': return String(loc.goodsCount ?? 0)
    case 'volume': return fmtNum(loc.volume)
    case 'weight': return fmtNum(loc.weight)
    default: return ''
  }
}

const cellTint = (u, li, ci) => {
  const loc = locMap.value[u.id]?.[`${li}:${ci}`]
  return !!loc && Number(loc.quantity) > 0
}

const buildLocMap = (list) => {
  const m = {}
  for (const loc of list || []) {
    const sid = loc.shelfId
    if (sid == null) continue
    if (!m[sid]) m[sid] = {}
    m[sid][`${loc.layerIndex}:${loc.colIndex}`] = loc
  }
  locMap.value = m
}

/* ---------- 从后端构建画布 ---------- */
const unitNo = (code) => {
  const m = String(code || '').match(/(\d+)$/)
  return m ? Number(m[1]) : 0
}

const makeGroup = (shelfArr, row, col) => {
  const arr = [...shelfArr].sort((a, b) => unitNo(a.code) - unitNo(b.code))
  return {
    key: 'srv-' + row + '-' + col + '-' + Math.random().toString(36).slice(2, 8),
    type: arr.length === 2 ? 2 : 1,
    row,
    col,
    layerCount: arr[0].layerCount,
    colCount: arr[0].colCount,
    units: arr.map(s => ({
      id: s.id,
      code: s.code,
      name: s.name || s.code,
      layerCount: s.layerCount,
      colCount: s.colCount,
      status: s.status
    }))
  }
}

const tryGridGroups = (list) => {
  if (!list.length) return []
  const map = new Map()
  for (const s of list) {
    const px = Number(s.positionX)
    const py = Number(s.positionY)
    if (!Number.isInteger(px) || !Number.isInteger(py) || px < 0 || px > 49 || py < 0 || py > 49) return null
    const key = py + ':' + px
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(s)
  }
  const out = []
  for (const arr of map.values()) {
    if (arr.length > 2) return null
    out.push(makeGroup(arr, Number(arr[0].positionY), Number(arr[0].positionX)))
  }
  return out
}

const buildFromServer = (shelves) => {
  const list = [...(shelves || [])].sort((a, b) => String(a.code).localeCompare(String(b.code)))
  const grid = tryGridGroups(list)
  if (grid) {
    groups.value = grid
    return
  }
  // 旧数据（无网格坐标）：每个货架单独成组，两列排布
  groups.value = list.map((s, i) => makeGroup([s], Math.floor(i / 2), i % 2))
}

const syncPanelFromGrid = () => {
  const rows = groups.value.reduce((m, g) => Math.max(m, g.row), -1) + 1
  const cols = groups.value.reduce((m, g) => Math.max(m, g.col), -1) + 1
  panel.rowCount = rows || 1
  panel.bayCount = cols || 1
}

/* ---------- 数据加载 ---------- */
const reload = async () => {
  if (!subWarehouseId.value) {
    groups.value = []
    removedIds.value = []
    selectedKey.value = null
    locMap.value = {}
    return
  }
  loading.value = true
  try {
    const [shelfRes, locRes] = await Promise.all([
      listShelvesBySub(subWarehouseId.value),
      listLocationsBySub(subWarehouseId.value)
    ])
    buildFromServer(shelfRes.data || [])
    buildLocMap(locRes.data || [])
    removedIds.value = []
    selectedKey.value = null
    syncPanelFromGrid()
    await nextTick()
    fitView()
  } finally {
    loading.value = false
  }
}

const onWarehouseChange = async () => {
  const res = await listSubWarehouses(warehouseId.value)
  subWarehouses.value = res.data || []
  subWarehouseId.value = subWarehouses.value.length ? subWarehouses.value[0].id : null
  await reload()
}

const onSubChange = () => reload()

onMounted(async () => {
  const res = await listWarehouses()
  warehouses.value = res.data || []
  if (warehouses.value.length) {
    warehouseId.value = warehouses.value[0].id
    await onWarehouseChange()
  }
})

/* ---------- 添加 / 修改 / 删除 ---------- */
const idxToLetter = (n) => {
  let s = ''
  n++
  while (n > 0) {
    n--
    s = String.fromCharCode(65 + (n % 26)) + s
    n = Math.floor(n / 26)
  }
  return s
}

const addGroups = () => {
  if (!subWarehouseId.value) {
    ElMessage.warning('请先选择子仓库')
    return
  }
  const rows = panel.rowCount
  const cols = panel.bayCount
  const startRow = groups.value.reduce((m, g) => Math.max(m, g.row), -1) + 1
  const existing = new Set(groups.value.flatMap(g => g.units.map(u => u.code)))
  const firstTop = (panel.nameTop || '').trim()
  const firstBottom = (panel.nameBottom || '').trim()

  // 先整体校验再生成
  const plan = []
  let letterIdx = groups.value.length
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const letter = idxToLetter(letterIdx++)
      const isCorner = r === 0 && c === 0
      const top = isCorner && firstTop ? firstTop : letter + '01'
      const bottom = isCorner && firstBottom ? firstBottom : letter + '02'
      const codes = panel.mode === 2 ? [top, bottom] : [top]
      for (const code of codes) {
        if (existing.has(code)) {
          ElMessage.warning(`货架编码已存在：${code}`)
          return
        }
      }
      plan.push({ codes })
    }
  }
  const stamp = Date.now()
  const newGroups = plan.map((p, i) => ({
    key: 'new-' + stamp + '-' + i,
    type: panel.mode,
    row: startRow + Math.floor(i / cols),
    col: i % cols,
    layerCount: panel.layerCount,
    colCount: panel.colCount,
    units: p.codes.map(code => ({
      code,
      name: code,
      layerCount: panel.layerCount,
      colCount: panel.colCount,
      status: 1
    }))
  }))
  groups.value.push(...newGroups)
  selectGroup(newGroups[0].key)
  ElMessage.success(
    `已添加 ${newGroups.length} 个货架组（${newGroups.reduce((s, g) => s + g.units.length, 0)} 个货架单元），点击「保存」同步到数据库`
  )
  nextTick(fitView)
}

const applyModify = () => {
  const g = selectedGroup.value
  if (!g) return
  const top = (panel.nameTop || '').trim()
  const bottom = (panel.nameBottom || '').trim()
  if (!top) {
    ElMessage.warning('请填写上排货架编码')
    return
  }
  if (g.type === 2 && !bottom) {
    ElMessage.warning('请填写下排货架编码')
    return
  }
  const others = new Set(
    groups.value.filter(x => x.key !== g.key).flatMap(x => x.units.map(u => u.code))
  )
  for (const code of g.type === 2 ? [top, bottom] : [top]) {
    if (others.has(code)) {
      ElMessage.warning(`货架编码已存在：${code}`)
      return
    }
  }
  g.units[0].code = top
  g.units[0].name = top
  if (g.units[1]) {
    g.units[1].code = bottom
    g.units[1].name = bottom
  }
  g.units.forEach(u => {
    u.layerCount = panel.layerCount
    u.colCount = panel.colCount
  })
  g.layerCount = panel.layerCount
  g.colCount = panel.colCount
  ElMessage.success('修改已应用到选中货架组，点击「保存」同步到数据库')
}

const removeSelectedGroup = async () => {
  const g = selectedGroup.value
  if (!g) return
  try {
    await ElMessageBox.confirm(
      `确认删除货架组 ${g.units.map(u => u.code).join(' / ')}？该组货位将一并删除，有库存的货位会阻止保存。`,
      '删除货架组',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  g.units.forEach(u => {
    if (u.id) removedIds.value.push(u.id)
  })
  groups.value = groups.value.filter(x => x.key !== g.key)
  selectedKey.value = null
  ElMessage.success('已在画布中删除该货架组，点击「保存」后生效')
}

/* ---------- 取消 / 保存 ---------- */
const cancelChanges = async () => {
  await reload()
  ElMessage.info('已放弃未保存的修改，画布已恢复为数据库中的内容')
}

const saveAll = async () => {
  if (!subWarehouseId.value) return
  const seen = new Set()
  for (const g of groups.value) {
    for (const u of g.units) {
      const code = String(u.code || '').trim()
      if (!code) {
        ElMessage.warning('存在未填写编码的货架单元，请先在画布中选中并修改')
        return
      }
      if (seen.has(code)) {
        ElMessage.warning(`货架编码重复：${code}`)
        return
      }
      seen.add(code)
    }
  }
  saving.value = true
  try {
    const shelves = groups.value.flatMap(g =>
      g.units.map(u => ({
        id: u.id,
        warehouseId: warehouseId.value,
        subWarehouseId: subWarehouseId.value,
        name: String(u.code).trim(),
        code: String(u.code).trim(),
        type: g.type,
        rowCount: 1,
        bayCount: 1,
        colCount: u.colCount,
        layerCount: u.layerCount,
        positionX: g.col,
        positionY: g.row,
        status: u.status ?? 1
      }))
    )
    const res = await saveAllShelves({
      warehouseId: warehouseId.value,
      subWarehouseId: subWarehouseId.value,
      shelves,
      removedIds: removedIds.value
    })
    ElMessage.success(res.message || '保存成功')
    await reload()
  } finally {
    saving.value = false
  }
}

/* ---------- 批量导入 ---------- */
const importVisible = ref(false)
const importRows = ref([])
const importErrors = ref([])
const importMode = ref('replace')
const importFileName = ref('')

const importGroupsCount = computed(
  () => new Set(importRows.value.map(p => `${p.row}:${p.col}`)).size
)

const openImport = () => {
  importRows.value = []
  importErrors.value = []
  importMode.value = groups.value.length ? 'replace' : 'append'
  importFileName.value = ''
  importVisible.value = true
}

const downloadTemplate = () => {
  const aoa = [
    ['货架编码', '层数', '列数', '行', '排', '位置'],
    ['A01', 3, 20, 1, 1, '上'],
    ['A02', 3, 20, 1, 1, '下'],
    ['B01', 3, 20, 1, 2, '上'],
    ['B02', 3, 20, 1, 2, '下'],
    ['C01', 3, 20, 2, 1, '上'],
    ['C02', 3, 20, 2, 1, '下']
  ]
  const ws = XLSX.utils.aoa_to_sheet(aoa)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '货架')
  XLSX.writeFile(wb, '货架批量导入模板.xlsx')
}

const onImportFile = async (file) => {
  if (!file?.raw) return
  importFileName.value = file.name || ''
  try {
    const buf = await file.raw.arrayBuffer()
    const wb = XLSX.read(buf, { type: 'array' })
    const ws = wb.Sheets[wb.SheetNames[0]]
    const rows = XLSX.utils.sheet_to_json(ws, { defval: '' })
    const errors = []
    const parsed = []
    const get = (r, ...names) => {
      for (const n of names) {
        const k = Object.keys(r).find(key => String(key).replace(/\s+/g, '') === n)
        if (k !== undefined && r[k] !== '' && r[k] !== null && r[k] !== undefined) return r[k]
      }
      return undefined
    }
    ;(rows || []).forEach((r, idx) => {
      const line = idx + 2
      const code = String(get(r, '货架编码', '编码', '货架名称', 'code') ?? '').trim()
      if (!code) {
        errors.push(`第 ${line} 行：货架编码为空`)
        return
      }
      const layer = Number(get(r, '层数', '层', 'layerCount'))
      const col = Number(get(r, '列数', '列', 'colCount'))
      const row = Number(get(r, '行', '行数', 'row'))
      const bay = Number(get(r, '排', '排数', '排号', 'bay'))
      const posRaw = String(get(r, '位置', '上下排', '组内位置', 'pos') ?? '上').trim()
      const pos = ['下', '下排', '2', 'bottom'].includes(posRaw) ? 2 : 1
      if ([layer, col, row, bay].some(v => !Number.isInteger(v) || v < 1 || v > 50)) {
        errors.push(`第 ${line} 行：层数/列数/行/排 需为 1~50 的整数`)
        return
      }
      parsed.push({ code, layerCount: layer, colCount: col, row, col: bay, pos, posLabel: pos === 2 ? '下' : '上' })
    })
    const posSeen = new Set()
    const codeSeen = new Set()
    for (const p of parsed) {
      const pk = `${p.row}:${p.col}:${p.pos}`
      if (posSeen.has(pk)) errors.push(`行 ${p.row} 排 ${p.col} 位置「${p.posLabel}」重复`)
      posSeen.add(pk)
      if (codeSeen.has(p.code)) errors.push(`货架编码重复：${p.code}`)
      codeSeen.add(p.code)
    }
    // 标注每条记录所属组的类型
    const typeMap = new Map()
    for (const p of parsed) {
      const k = `${p.row}:${p.col}`
      typeMap.set(k, (typeMap.get(k) || 0) + 1)
    }
    for (const p of parsed) {
      p.groupType = typeMap.get(`${p.row}:${p.col}`) === 2 ? '双排组' : '单排'
    }
    importRows.value = parsed
    importErrors.value = [...new Set(errors)]
    if (parsed.length && !errors.length) {
      ElMessage.success(`解析成功：${parsed.length} 条货架记录，${importGroupsCount.value} 个货架组`)
    }
  } catch (err) {
    importRows.value = []
    importErrors.value = []
    ElMessage.error('文件解析失败：' + (err?.message || '请使用模板规定的 Excel / CSV 格式'))
  }
}

const confirmImport = () => {
  const map = new Map()
  for (const p of importRows.value) {
    const k = `${p.row}:${p.col}`
    if (!map.has(k)) map.set(k, [])
    map.get(k).push(p)
  }
  const append = importMode.value === 'append' && groups.value.length > 0
  if (append) {
    const existing = new Set(groups.value.flatMap(g => g.units.map(u => u.code)))
    for (const p of importRows.value) {
      if (existing.has(p.code)) {
        ElMessage.warning(`追加模式下编码与现有货架冲突：${p.code}`)
        return
      }
    }
  }
  const baseRow = append ? groups.value.reduce((m, g) => Math.max(m, g.row), -1) + 1 : 0
  const minRow = append ? Math.min(...importRows.value.map(p => p.row)) : 1
  const stamp = Date.now()
  const newGroups = [...map.values()].map((arr, i) => {
    const sorted = [...arr].sort((a, b) => a.pos - b.pos)
    return {
      key: 'imp-' + stamp + '-' + i,
      type: sorted.length === 2 ? 2 : 1,
      row: baseRow + sorted[0].row - minRow,
      col: sorted[0].col - 1,
      layerCount: sorted[0].layerCount,
      colCount: sorted[0].colCount,
      units: sorted.map(p => ({
        code: p.code,
        name: p.code,
        layerCount: p.layerCount,
        colCount: p.colCount,
        status: 1
      }))
    }
  })
  if (importMode.value === 'replace') {
    groups.value.forEach(g => g.units.forEach(u => {
      if (u.id) removedIds.value.push(u.id)
    }))
    groups.value = newGroups
  } else {
    groups.value.push(...newGroups)
  }
  importVisible.value = false
  selectedKey.value = null
  syncPanelFromGrid()
  ElMessage.success(
    `已导入 ${newGroups.length} 个货架组（${newGroups.reduce((s, g) => s + g.units.length, 0)} 个货架单元），点击「保存」同步到数据库`
  )
  nextTick(fitView)
}
</script>

<style scoped>
.shelf-config-page {
  --el-color-primary: #1890ff;
  --el-color-primary-light-3: #69b1ff;
  --el-color-primary-light-5: #91caff;
  --el-color-primary-light-7: #bae0ff;
  --el-color-primary-light-8: #d6f0ff;
  --el-color-primary-light-9: #e6f4ff;
  --el-color-primary-dark-2: #0958d9;
  --el-color-danger: #ff4d4f;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 32px);
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

/* 顶部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  gap: 12px;
}
.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #262626;
}
.header-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sel-label {
  font-size: 13px;
  color: #595959;
  margin-left: 8px;
}

/* 主体 */
.main-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 画布 */
.canvas-wrap {
  flex: 1;
  position: relative;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
  overflow: hidden;
  min-width: 0;
}
.canvas-svg {
  width: 100%;
  height: 100%;
  display: block;
  cursor: grab;
  user-select: none;
}
.canvas-svg.panning {
  cursor: grabbing;
}
.grp {
  cursor: pointer;
}
.grp-box {
  fill: #fff;
  stroke: #d9d9d9;
  stroke-width: 1;
}
.grp-box.selected {
  fill: #f0f7ff;
  stroke: #1890ff;
  stroke-width: 2;
  stroke-dasharray: 6 4;
}
.unit-box {
  fill: #fff;
  stroke: #bfbfbf;
  stroke-width: 1;
}
.unit-grid {
  stroke: #e8e8e8;
  stroke-width: 1;
  fill: none;
}
.unit-divider {
  stroke: #595959;
  stroke-width: 2;
}
.unit-code {
  font-size: 12px;
  font-weight: 600;
  fill: #0958d9;
}
.unit-spec {
  font-size: 11px;
  fill: #8c8c8c;
}
.grp-type {
  font-size: 11px;
  fill: #8c8c8c;
  text-anchor: middle;
}
.cell-tint {
  fill: #bae0ff;
}
.cell-text {
  font-size: 9px;
  fill: #333;
  text-anchor: middle;
}
.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.canvas-toolbar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.88);
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}
.zoom-ctrl {
  display: flex;
  align-items: center;
  gap: 4px;
}
.zoom-val {
  min-width: 44px;
  text-align: center;
  color: #595959;
  font-variant-numeric: tabular-nums;
}

/* 右侧面板 */
.config-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.panel-scroll {
  flex: 1;
  min-height: 0;
}
.panel-section {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.section-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
.section-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}
.section-tip.calc {
  color: #595959;
}
.mode-radio {
  display: flex;
  gap: 16px;
}
.two-col {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}
.field {
  flex: 1;
  min-width: 0;
  margin-top: 10px;
}
.field label {
  display: block;
  font-size: 12px;
  color: #595959;
  margin-bottom: 4px;
}
.field :deep(.el-input-number) {
  width: 100%;
}
.btn-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 14px;
}
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.chip {
  padding: 5px 14px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: all 0.15s;
}
.chip:hover {
  border-color: #1890ff;
  color: #1890ff;
}
.chip.active {
  background: #262626;
  border-color: #262626;
  color: #fff;
}
.panel-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

/* 批量导入弹窗 */
.import-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.import-file {
  font-size: 12px;
  color: #595959;
}
.import-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}
.import-tip.warn {
  color: #d46b08;
}
.import-alert {
  margin-top: 10px;
}
.import-empty {
  margin-top: 12px;
  padding: 28px 0;
  text-align: center;
  color: #bfbfbf;
  font-size: 13px;
  background: #fafafa;
  border-radius: 4px;
}
.import-mode {
  margin-top: 12px;
}
</style>
