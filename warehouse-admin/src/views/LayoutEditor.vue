<template>
  <div>
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-button :icon="Back" @click="$router.push('/warehouse')">返回</el-button>
        <span class="title">平面图绘制 - {{ warehouse.name || '' }}</span>
        <el-radio-group v-model="mode" size="small">
          <el-radio-button value="select">选择/移动</el-radio-button>
          <el-radio-button value="draw">绘制区域</el-radio-button>
        </el-radio-group>
        <el-button size="small" type="danger" plain :disabled="selectedIndex < 0" @click="removeSelected">
          删除选中
        </el-button>
        <el-upload :show-file-list="false" accept="image/*" :http-request="doImport" style="display:inline-block">
          <el-button size="small" type="warning" plain>导入图纸(图片)</el-button>
        </el-upload>
        <el-button size="small" type="primary" :loading="saving" @click="save">保存平面图</el-button>
      </div>
    </el-card>

    <div class="editor-body">
      <el-card shadow="never" class="canvas-card">
        <svg ref="svgRef" :viewBox="`0 0 ${layout.width} ${layout.height}`" class="layout-svg"
             :class="{ drawing: mode === 'draw' }"
             @mousedown="onMouseDown" @mousemove="onMouseMove" @mouseup="onMouseUp" @mouseleave="onMouseUp">
          <rect x="0" y="0" :width="layout.width" :height="layout.height" fill="#fff" stroke="#d9d9d9" />
          <image v-if="layout.bgImage" :href="layout.bgImage" x="0" y="0"
                 :width="layout.width" :height="layout.height" preserveAspectRatio="none" opacity="0.6" />
          <!-- 子仓库参考层 -->
          <g v-for="sub in subWarehouses" :key="'sub-' + sub.id" opacity="0.5">
            <rect :x="num(sub.positionX)" :y="num(sub.positionY)" :width="num(sub.width)" :height="num(sub.height)"
                  fill="none" stroke="#1890ff" stroke-width="1.5" stroke-dasharray="6 4" />
            <text :x="num(sub.positionX) + 4" :y="num(sub.positionY) + 14" fill="#1890ff" font-size="12">{{ sub.name }}</text>
          </g>
          <!-- 已绘制元素 -->
          <g v-for="(el, i) in elements" :key="el.id" @mousedown.stop="onElementMouseDown(i, $event)">
            <rect :x="el.x" :y="el.y" :width="el.width" :height="el.height"
                  :fill="i === selectedIndex ? 'rgba(24,144,255,0.35)' : 'rgba(82,196,26,0.25)'"
                  :stroke="i === selectedIndex ? '#1890ff' : '#52c41a'" stroke-width="2" />
            <text :x="el.x + 5" :y="el.y + 18" font-size="14" fill="#333">{{ el.label }}</text>
          </g>
          <!-- 绘制预览 -->
          <rect v-if="drawing" :x="previewRect.x" :y="previewRect.y" :width="previewRect.width" :height="previewRect.height"
                fill="rgba(24,144,255,0.15)" stroke="#1890ff" stroke-dasharray="5 3" />
        </svg>
        <div class="canvas-tip">左侧面板切换「绘制区域」后，在画布上拖拽鼠标框选区域；「选择/移动」模式可拖动调整。</div>
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header><span>属性面板</span></template>
        <el-form label-width="90px" size="small">
          <el-form-item label="图纸名称">
            <el-input v-model="layout.name" />
          </el-form-item>
          <el-form-item label="画布尺寸">
            <el-input-number v-model="layout.width" :min="400" :max="4000" style="width: 100px" />
            ×
            <el-input-number v-model="layout.height" :min="300" :max="3000" style="width: 100px" />
          </el-form-item>
        </el-form>
        <el-divider />
        <template v-if="selectedIndex >= 0 && elements[selectedIndex]">
          <el-form label-width="90px" size="small">
            <el-form-item label="区域名称">
              <el-input v-model="elements[selectedIndex].label" />
            </el-form-item>
            <el-form-item label="坐标 X/Y">
              <el-input-number v-model="elements[selectedIndex].x" :min="0" style="width: 100px" />
              <el-input-number v-model="elements[selectedIndex].y" :min="0" style="width: 100px; margin-left: 6px" />
            </el-form-item>
            <el-form-item label="宽 / 高">
              <el-input-number v-model="elements[selectedIndex].width" :min="10" style="width: 100px" />
              <el-input-number v-model="elements[selectedIndex].height" :min="10" style="width: 100px; margin-left: 6px" />
            </el-form-item>
          </el-form>
          <el-divider />
        </template>
        <div class="element-list">
          <div class="element-list-title">区域列表（{{ elements.length }}）</div>
          <div v-for="(el, i) in elements" :key="el.id" class="element-item"
               :class="{ active: i === selectedIndex }" @click="selectedIndex = i">
            {{ el.label || '未命名区域' }}
            <span class="element-pos">{{ el.width }}×{{ el.height }}</span>
          </div>
          <el-empty v-if="!elements.length" description="尚未绘制区域" :image-size="60" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getWarehouse, listSubWarehouses } from '../api/base'
import { getLayoutByWarehouse, saveLayout, importLayout } from '../api/shelf'

const route = useRoute()
const warehouseId = Number(route.params.warehouseId)
const warehouse = ref({})
const subWarehouses = ref([])
const svgRef = ref(null)
const mode = ref('select')
const saving = ref(false)

const layout = reactive({ id: null, warehouseId, name: '', width: 1200, height: 800, bgImage: null })
const elements = ref([])
const selectedIndex = ref(-1)

const drawing = ref(false)
const drawStart = reactive({ x: 0, y: 0 })
const drawCurrent = reactive({ x: 0, y: 0 })

const dragging = ref(false)
const dragOffset = reactive({ x: 0, y: 0 })

const num = (v) => Number(v) || 0

const previewRect = computed(() => ({
  x: Math.min(drawStart.x, drawCurrent.x),
  y: Math.min(drawStart.y, drawCurrent.y),
  width: Math.abs(drawCurrent.x - drawStart.x),
  height: Math.abs(drawCurrent.y - drawStart.y)
}))

const toSvgPoint = (e) => {
  const rect = svgRef.value.getBoundingClientRect()
  return {
    x: Math.round((e.clientX - rect.left) * (layout.width / rect.width)),
    y: Math.round((e.clientY - rect.top) * (layout.height / rect.height))
  }
}

const onMouseDown = (e) => {
  if (mode.value !== 'draw') return
  const p = toSvgPoint(e)
  drawStart.x = p.x
  drawStart.y = p.y
  drawCurrent.x = p.x
  drawCurrent.y = p.y
  drawing.value = true
}

const onElementMouseDown = (i, e) => {
  if (mode.value !== 'select') return
  selectedIndex.value = i
  const p = toSvgPoint(e)
  dragOffset.x = p.x - elements.value[i].x
  dragOffset.y = p.y - elements.value[i].y
  dragging.value = true
}

const onMouseMove = (e) => {
  const p = toSvgPoint(e)
  if (drawing.value) {
    drawCurrent.x = p.x
    drawCurrent.y = p.y
  } else if (dragging.value && selectedIndex.value >= 0) {
    const el = elements.value[selectedIndex.value]
    el.x = Math.max(0, p.x - dragOffset.x)
    el.y = Math.max(0, p.y - dragOffset.y)
  }
}

const onMouseUp = () => {
  if (drawing.value) {
    drawing.value = false
    const r = previewRect.value
    if (r.width >= 20 && r.height >= 20) {
      elements.value.push({
        id: 'el-' + Date.now(),
        type: 'rect',
        x: r.x,
        y: r.y,
        width: r.width,
        height: r.height,
        label: `区域 ${elements.value.length + 1}`
      })
      selectedIndex.value = elements.value.length - 1
    }
  }
  dragging.value = false
}

const removeSelected = () => {
  if (selectedIndex.value >= 0) {
    elements.value.splice(selectedIndex.value, 1)
    selectedIndex.value = -1
  }
}

const save = async () => {
  if (!layout.name) {
    ElMessage.warning('请填写图纸名称')
    return
  }
  saving.value = true
  try {
    const res = await saveLayout({
      id: layout.id,
      warehouseId,
      name: layout.name,
      width: layout.width,
      height: layout.height,
      bgImage: layout.bgImage,
      sourceType: 1,
      layoutData: JSON.stringify({ elements: elements.value })
    })
    layout.id = res.data.id
    ElMessage.success('平面图已保存')
  } finally {
    saving.value = false
  }
}

const doImport = async ({ file }) => {
  const formData = new FormData()
  formData.append('warehouseId', warehouseId)
  formData.append('name', layout.name || file.name)
  formData.append('sourceType', 2)
  formData.append('file', file)
  const res = await importLayout(formData)
  ElMessage.success('图纸已导入为背景图，可在其上继续绘制')
  applyLayout(res.data)
}

const applyLayout = (data) => {
  layout.id = data.id
  layout.name = data.name
  layout.width = Number(data.width) || 1200
  layout.height = Number(data.height) || 800
  layout.bgImage = data.bgImage || null
  try {
    const parsed = JSON.parse(data.layoutData || '{"elements":[]}')
    elements.value = parsed.elements || []
  } catch {
    elements.value = []
  }
  selectedIndex.value = -1
}

onMounted(async () => {
  const [wRes, subRes, lRes] = await Promise.all([
    getWarehouse(warehouseId),
    listSubWarehouses(warehouseId),
    getLayoutByWarehouse(warehouseId)
  ])
  warehouse.value = wRes.data || {}
  subWarehouses.value = subRes.data || []
  if (lRes.data) {
    applyLayout(lRes.data)
  } else {
    layout.name = `${warehouse.value.name || ''}平面图`
  }
})
</script>

<style scoped>
.toolbar-card {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.title {
  font-size: 15px;
  font-weight: 600;
  margin-right: 8px;
}
.editor-body {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.canvas-card {
  flex: 1;
  min-width: 0;
}
.panel-card {
  width: 300px;
  flex-shrink: 0;
}
.layout-svg {
  width: 100%;
  height: auto;
  display: block;
  background: #fff;
  user-select: none;
}
.layout-svg.drawing {
  cursor: crosshair;
}
.canvas-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
.element-list-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.element-item {
  padding: 6px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
}
.element-item.active {
  border-color: #1890ff;
  background: #e6f7ff;
}
.element-pos {
  color: #999;
  font-size: 12px;
}
</style>
