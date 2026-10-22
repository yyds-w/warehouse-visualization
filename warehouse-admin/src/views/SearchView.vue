<template>
  <div>
    <el-card shadow="never" class="search-card">
      <div class="search-box">
        <el-input v-model="keyword" size="large" clearable class="search-input"
                  placeholder="输入货位编号（如 WH01-A01-S01-L1-R1-B1-C1）、商品名称或 SKU 编码"
                  @keyup.enter="doSearch">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" :loading="loading" @click="doSearch">秒级找货</el-button>
      </div>
      <div class="search-hint">支持按货位编号精准查找，或按商品品名 / SKU 模糊搜索；命中后可一键定位到区域货架视图。</div>
    </el-card>

    <el-card v-if="searched" shadow="never">
      <template #header>
        <span>搜索结果（{{ results.length }} 条）</span>
      </template>
      <el-table v-if="results.length" :data="results" border stripe v-loading="loading">
        <el-table-column prop="locationCode" label="货位编号" min-width="220">
          <template #default="{ row }"><b>{{ row.locationCode }}</b></template>
        </el-table-column>
        <el-table-column label="商品" min-width="140">
          <template #default="{ row }">
            <span v-if="row.goodsName">{{ row.goodsName }}（{{ row.skuCode }}）</span>
            <el-tag v-else type="info" size="small">空货位</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="90" align="right">
          <template #default="{ row }">{{ row.quantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次" width="130">
          <template #default="{ row }">{{ row.batchNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="subWarehouseName" label="子仓库" width="130" />
        <el-table-column prop="shelfName" label="货架" min-width="130" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.locationStatus === 2 ? 'danger' : row.locationStatus === 1 ? 'success' : 'info'" size="small">
              {{ statusName(row.locationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="locate(row)">定位</el-button>
            <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="未找到匹配的货位，试试更换关键字" />
    </el-card>

    <location-detail-dialog v-model:visible="detailVisible" :location-id="detailLocationId" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { searchLocations } from '../api/dashboard'
import LocationDetailDialog from '../components/LocationDetailDialog.vue'

const router = useRouter()
const keyword = ref('')
const results = ref([])
const searched = ref(false)
const loading = ref(false)
const detailVisible = ref(false)
const detailLocationId = ref(null)

const statusName = (s) => ({ 0: '禁用', 1: '空闲', 2: '占用', 3: '锁定' }[s] ?? '-')

const doSearch = async () => {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键字')
    return
  }
  loading.value = true
  searched.value = true
  try {
    const res = await searchLocations(keyword.value.trim())
    results.value = res.data || []
  } finally {
    loading.value = false
  }
}

const locate = (row) => {
  router.push(`/region/${row.subWarehouseId}?highlight=${row.locationId}`)
}

const openDetail = (row) => {
  detailLocationId.value = row.locationId
  detailVisible.value = true
}
</script>

<style scoped>
.search-card {
  margin-bottom: 12px;
}
.search-box {
  display: flex;
  gap: 10px;
  max-width: 860px;
  margin: 10px auto 0;
}
.search-input {
  flex: 1;
}
.search-hint {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}
</style>
