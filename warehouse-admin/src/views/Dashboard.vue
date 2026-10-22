<template>
  <div>
    <div class="page-toolbar">
      <span class="toolbar-label">当前仓库</span>
      <el-select v-model="currentWarehouseId" style="width: 220px" @change="loadDashboard">
        <el-option v-for="w in warehouses" :key="w.id" :label="`${w.name}（${w.code}）`" :value="w.id" />
      </el-select>
      <el-button :icon="Refresh" circle @click="loadDashboard" />
    </div>

    <el-row :gutter="16" class="stat-row" v-loading="loading">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num">{{ dashboard.totalLocations ?? '-' }}</div>
          <div class="stat-label">总货位数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num" style="color: #f5222d">{{ dashboard.occupiedLocations ?? '-' }}</div>
          <div class="stat-label">已占用货位</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num" style="color: #1890ff">{{ dashboard.usageRate ?? '-' }}%</div>
          <div class="stat-label">空间利用率</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num" style="color: #52c41a">{{ dashboard.totalInventory ?? '-' }}</div>
          <div class="stat-label">库存总量</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>仓库平面图（双击子仓库进入区域货架视图）</span>
          </template>
          <div class="canvas-wrap">
            <svg viewBox="0 0 1200 800" class="layout-svg">
              <rect x="0" y="0" width="1200" height="800" fill="#fafafa" stroke="#d9d9d9" />
              <g v-for="sub in dashboard.subWarehouses || []" :key="sub.id"
                 style="cursor: pointer" @dblclick="goRegion(sub)">
                <rect :x="num(sub.positionX)" :y="num(sub.positionY)"
                      :width="num(sub.width)" :height="num(sub.height)"
                      :fill="blockColor(sub)" stroke="rgba(0,0,0,0.25)" stroke-width="2" rx="6" opacity="0.92" />
                <text :x="num(sub.positionX) + 12" :y="num(sub.positionY) + 28" fill="#fff" font-size="19" font-weight="600">
                  {{ sub.name }}
                </text>
                <text :x="num(sub.positionX) + 12" :y="num(sub.positionY) + 52" fill="rgba(255,255,255,0.95)" font-size="14">
                  利用率 {{ sub.usageRate }}%（{{ sub.occupiedLocations }}/{{ sub.totalLocations }}）
                </text>
                <text :x="num(sub.positionX) + 12" :y="num(sub.positionY) + 72" fill="rgba(255,255,255,0.95)" font-size="14">
                  库存 {{ sub.totalInventory }}
                </text>
              </g>
              <text v-if="!(dashboard.subWarehouses || []).length" x="600" y="400" text-anchor="middle" fill="#999" font-size="16">
                暂无子仓库数据，请先到「仓库管理」录入
              </text>
            </svg>
          </div>
          <div class="legend">
            <span><i class="dot" style="background:#1890ff"></i>利用率 &lt; 40%</span>
            <span><i class="dot" style="background:#faad14"></i>40% - 70%</span>
            <span><i class="dot" style="background:#f5222d"></i>&ge; 70%</span>
            <span><i class="dot" style="background:#bfbfbf"></i>无货位</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <template #header><span>子仓库空间利用率</span></template>
          <div ref="barRef" class="chart-box"></div>
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header><span>子仓库库存占比</span></template>
          <div ref="pieRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { listWarehouses } from '../api/base'
import { warehouseDashboard } from '../api/dashboard'

const router = useRouter()
const warehouses = ref([])
const currentWarehouseId = ref(null)
const dashboard = reactive({})
const loading = ref(false)
const barRef = ref(null)
const pieRef = ref(null)
let barChart = null
let pieChart = null

const num = (v) => Number(v) || 0

const blockColor = (sub) => {
  if (!sub.totalLocations) return '#bfbfbf'
  if (sub.usageRate >= 70) return '#f5222d'
  if (sub.usageRate >= 40) return '#faad14'
  return '#1890ff'
}

const goRegion = (sub) => {
  router.push(`/region/${sub.id}`)
}

const renderCharts = () => {
  const subs = dashboard.subWarehouses || []
  if (barChart) {
    barChart.setOption({
      grid: { left: 40, right: 20, top: 30, bottom: 40 },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: subs.map(s => s.name), axisLabel: { interval: 0, rotate: 20 } },
      yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
      series: [{ type: 'bar', data: subs.map(s => s.usageRate), itemStyle: { color: '#1890ff' }, barMaxWidth: 40 }]
    }, true)
  }
  if (pieChart) {
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}（{d}%）' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['50%', '45%'],
        data: subs.map(s => ({ name: s.name, value: s.totalInventory || 0 }))
      }]
    }, true)
  }
}

const loadDashboard = async () => {
  if (!currentWarehouseId.value) return
  loading.value = true
  try {
    const res = await warehouseDashboard(currentWarehouseId.value)
    Object.keys(dashboard).forEach(k => delete dashboard[k])
    Object.assign(dashboard, res.data)
    await nextTick()
    if (!barChart && barRef.value) barChart = echarts.init(barRef.value)
    if (!pieChart && pieRef.value) pieChart = echarts.init(pieRef.value)
    renderCharts()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await listWarehouses()
  warehouses.value = res.data || []
  if (warehouses.value.length) {
    currentWarehouseId.value = warehouses.value[0].id
    await loadDashboard()
  }
  window.addEventListener('resize', () => {
    barChart && barChart.resize()
    pieChart && pieChart.resize()
  })
})
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.toolbar-label {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 6px;
}
.canvas-wrap {
  width: 100%;
  overflow: hidden;
}
.layout-svg {
  width: 100%;
  height: auto;
  display: block;
}
.legend {
  display: flex;
  gap: 18px;
  font-size: 12px;
  color: #666;
  margin-top: 8px;
}
.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 4px;
}
.chart-card {
  margin-bottom: 16px;
}
.chart-box {
  height: 240px;
}
</style>
