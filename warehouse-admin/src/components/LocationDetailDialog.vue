<template>
  <el-dialog :model-value="visible" title="货位详情" width="620px" @update:model-value="emit('update:visible', $event)">
    <div v-loading="loading">
      <el-descriptions v-if="detail.location" :column="2" border size="small">
        <el-descriptions-item label="货位编号" :span="2">
          <b>{{ detail.location.locationCode }}</b>
        </el-descriptions-item>
        <el-descriptions-item label="所属仓库">{{ detail.warehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属子仓库">{{ detail.subWarehouseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属货架">{{ detail.shelfName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(detail.location.status)">{{ statusName(detail.location.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="平面坐标">
          ({{ detail.location.positionX ?? '-' }}, {{ detail.location.positionY ?? '-' }})
        </el-descriptions-item>
        <el-descriptions-item label="容量/承重">{{ detail.location.capacity ?? '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">库存信息（{{ (detail.inventories || []).length }}）</el-divider>
      <el-table v-if="(detail.inventories || []).length" :data="detail.inventories" border size="small">
        <el-table-column prop="goodsName" label="商品" min-width="130" />
        <el-table-column prop="skuCode" label="SKU" width="110" />
        <el-table-column prop="quantity" label="数量" width="80" align="right">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="130" />
        <el-table-column prop="updatedAt" label="更新时间" width="150" />
      </el-table>
      <el-empty v-else description="该货位暂无库存" :image-size="60" />
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getLocationDetail } from '../api/shelf'

const props = defineProps({
  visible: { type: Boolean, default: false },
  locationId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:visible'])

const detail = ref({})
const loading = ref(false)

const statusName = (s) => ({ 0: '禁用', 1: '空闲', 2: '占用', 3: '锁定' }[s] ?? '-')
const statusTag = (s) => ({ 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }[s] ?? 'info')

watch(() => [props.visible, props.locationId], async ([v, id]) => {
  if (v && id) {
    loading.value = true
    detail.value = {}
    try {
      const res = await getLocationDetail(id)
      detail.value = res.data || {}
    } finally {
      loading.value = false
    }
  }
})
</script>
