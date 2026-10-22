<template>
  <div>
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-button :icon="Back" @click="$router.push('/dashboard')">返回看板</el-button>
        <span class="title">{{ data.subWarehouse?.name || '区域货架视图' }}</span>
        <el-tag v-if="data.subWarehouse" type="info">{{ data.subWarehouse.code }}</el-tag>
        <div class="spacer"></div>
        <span class="label">货架</span>
        <el-select v-model="currentShelfId" style="width: 200px" @change="onShelfChange">
          <el-option v-for="s in data.shelves || []" :key="s.id" :label="`${s.name}（${s.code}）`" :value="s.id" />
        </el-select>
        <span class="label">层</span>
        <el-select v-model="currentLayer" style="width: 100px">
          <el-option v-for="l in layerOptions" :key="l" :label="`第 ${l} 层`" :value="l" />
        </el-select>
      </div>
    </el-card>

    <el-row :gutter="12" v-if="currentShelf">
      <el-col :span="17">
        <el-card shadow="never">
          <template #header>
            <span>{{ currentShelf.name }} · 第 {{ currentLayer }} 层货位（{{ layerLocations.length }} 个）</span>
          </template>
          <div class="grid-wrap">
            <div class="loc-grid" :style="{ gridTemplateColumns: `repeat(${currentShelf.colCount}, 1fr)` }">
              <div v-for="loc in layerLocations" :key="loc.id"
                   class="loc-cell" :class="[statusClass(loc.status), { highlighted: isHighlight(loc.id) }]"
                   @click="openDetail(loc)">
                <div class="loc-code">R{{ loc.rowIndex }}-C{{ loc.colIndex }}</div>
                <div class="loc-goods">{{ loc.goodsName || '空闲' }}</div>
                <div v-if="loc.quantity" class="loc-qty">×{{ loc.quantity }}</div>
              </div>
            </div>
            <el-empty v-if="!layerLocations.length" description="该层暂无货位" :image-size="80" />
          </div>
          <div class="legend">
            <span><i class="dot" style="background:#95de64"></i>空闲</span>
            <span><i class="dot" style="background:#ff7875"></i>占用</span>
            <span><i class="dot" style="background:#bfbfbf"></i>锁定</span>
            <span><i class="dot" style="background:#595959"></i>禁用</span>
            <span class="highlight-tip"><i class="dot highlight-dot"></i>找货定位目标</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="7">
        <el-card shadow="never" class="side-card">
          <template #header><span>货架信息</span></template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="编码">{{ currentShelf.code }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ currentShelf.type === 2 ? '双排' : '单排' }}</el-descriptions-item>
            <el-descriptions-item label="规格">
              {{ currentShelf.rowCount }} 行 × {{ currentShelf.bayCount }} 排 × {{ currentShelf.colCount }} 列 × {{ currentShelf.layerCount }} 层
            </el-descriptions-item>
            <el-descriptions-item label="货位占用">
              {{ currentShelf.occupiedLocations }} / {{ currentShelf.totalLocations }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="usage-block">
            <div class="usage-label">空间利用率</div>
            <el-progress :percentage="currentShelf.usageRate || 0" :stroke-width="16"
                         :color="usageColor(currentShelf.usageRate)" />
          </div>
        </el-card>
        <el-card shadow="never" class="side-card">
          <template #header><span>本层占用概览</span></template>
          <div class="layer-stat">
            <div class="layer-stat-item">
              <div class="num" style="color:#52c41a">{{ layerLocations.filter(l => l.status === 1).length }}</div>
              <div class="txt">空闲</div>
            </div>
            <div class="layer-stat-item">
              <div class="num" style="color:#f5222d">{{ layerLocations.filter(l => l.status === 2).length }}</div>
              <div class="txt">占用</div>
            </div>
            <div class="layer-stat-item">
              <div class="num" style="color:#8c8c8c">{{ layerLocations.filter(l => l.status === 0 || l.status === 3).length }}</div>
              <div class="txt">其他</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-else shadow="never">
      <el-empty description="该子仓库暂无货架，请先到「货架配置」创建" />
    </el-card>

    <location-detail-dialog v-model:visible="detailVisible" :location-id="detailLocationId" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back } from '@element-plus/icons-vue'
import { subDashboard } from '../api/dashboard'
import LocationDetailDialog from '../components/LocationDetailDialog.vue'

const route = useRoute()
const subId = Number(route.params.subId)
const highlightId = route.query.highlight ? Number(route.query.highlight) : null

const data = ref({})
const currentShelfId = ref(null)
const currentLayer = ref(1)
const detailVisible = ref(false)
const detailLocationId = ref(null)

const currentShelf = computed(() => (data.value.shelves || []).find(s => s.id === currentShelfId.value))
const layerOptions = computed(() => {
  if (!currentShelf.value) return [1]
  return Array.from({ length: currentShelf.value.layerCount }, (_, i) => i + 1)
})
const layerLocations = computed(() => {
  if (!currentShelf.value) return []
  return (currentShelf.value.locations || []).filter(l => l.layerIndex === currentLayer.value)
})

const isHighlight = (id) => highlightId === Number(id)
const statusClass = (s) => ({ 0: 'st-disabled', 1: 'st-free', 2: 'st-occupied', 3: 'st-locked' }[s] ?? 'st-free')
const usageColor = (rate) => (rate >= 70 ? '#f5222d' : rate >= 40 ? '#faad14' : '#1890ff')

const onShelfChange = () => {
  currentLayer.value = 1
}

const openDetail = (loc) => {
  detailLocationId.value = loc.id
  detailVisible.value = true
}

onMounted(async () => {
  const res = await subDashboard(subId)
  data.value = res.data || {}
  const shelves = data.value.shelves || []
  if (shelves.length) {
    if (highlightId) {
      const target = shelves.find(s => (s.locations || []).some(l => Number(l.id) === highlightId))
      if (target) {
        currentShelfId.value = target.id
        const loc = target.locations.find(l => Number(l.id) === highlightId)
        currentLayer.value = loc ? loc.layerIndex : 1
        return
      }
    }
    currentShelfId.value = shelves[0].id
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
  gap: 10px;
  flex-wrap: wrap;
}
.spacer {
  flex: 1;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.label {
  font-size: 13px;
  color: #666;
}
.grid-wrap {
  min-height: 200px;
}
.loc-grid {
  display: grid;
  gap: 6px;
}
.loc-cell {
  border-radius: 6px;
  padding: 8px 6px;
  min-height: 58px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform 0.1s;
  position: relative;
}
.loc-cell:hover {
  transform: scale(1.04);
  border-color: #333;
}
.st-free {
  background: #d9f7be;
  color: #237804;
}
.st-occupied {
  background: #ffa39e;
  color: #a8071a;
}
.st-locked {
  background: #d9d9d9;
  color: #595959;
}
.st-disabled {
  background: #595959;
  color: #d9d9d9;
}
.loc-code {
  font-size: 12px;
  font-weight: 700;
}
.loc-goods {
  font-size: 11px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.loc-qty {
  position: absolute;
  right: 6px;
  top: 4px;
  font-size: 11px;
  font-weight: 700;
}
.highlighted {
  border-color: #faad14 !important;
  animation: blink 1s infinite alternate;
}
@keyframes blink {
  from { box-shadow: 0 0 0 2px rgba(250, 173, 20, 0.9); }
  to { box-shadow: 0 0 0 6px rgba(250, 173, 20, 0.15); }
}
.legend {
  display: flex;
  gap: 18px;
  font-size: 12px;
  color: #666;
  margin-top: 12px;
}
.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 4px;
}
.highlight-dot {
  background: #faad14;
}
.side-card {
  margin-bottom: 12px;
}
.usage-block {
  margin-top: 14px;
}
.usage-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}
.layer-stat {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.layer-stat-item .num {
  font-size: 24px;
  font-weight: 700;
}
.layer-stat-item .txt {
  font-size: 12px;
  color: #999;
}
</style>
