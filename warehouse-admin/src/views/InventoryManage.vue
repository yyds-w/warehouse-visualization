<template>
  <div>
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="商品名 / SKU / 货位编号" clearable style="width: 240px"
                  @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" plain @click="openInbound">入库</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="locationCode" label="货位编号" min-width="210" />
        <el-table-column prop="goodsName" label="商品" min-width="130" />
        <el-table-column prop="skuCode" label="SKU" width="110" />
        <el-table-column label="数量" width="100" align="right">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="130" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="{ 1: 'success', 2: 'info', 3: 'warning' }[row.status] || 'info'" size="small">
              {{ { 1: '正常', 2: '冻结', 3: '待出库' }[row.status] || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openAdjust(row)">调整</el-button>
            <el-popconfirm title="确认删除该库存记录？货位将恢复空闲" @confirm="remove(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无库存记录" /></template>
      </el-table>
      <el-pagination class="pager" background layout="total, sizes, prev, pager, next" :total="total"
                     :page-sizes="[10, 20, 50]" :page-size="query.pageSize" :current-page="query.pageNum"
                     @current-change="(p) => { query.pageNum = p; load() }"
                     @size-change="(s) => { query.pageSize = s; query.pageNum = 1; load() }" />
    </el-card>

    <!-- 入库弹窗 -->
    <el-dialog v-model="inboundDialog.visible" title="商品入库" width="520px">
      <el-form label-width="90px">
        <el-form-item label="货位" required>
          <el-select v-model="inboundDialog.form.locationId" filterable remote clearable
                     :remote-method="searchLocationOptions" :loading="inboundDialog.locLoading"
                     placeholder="输入货位编号搜索" style="width: 100%">
            <el-option v-for="l in inboundDialog.locationOptions" :key="l.locationId"
                       :label="l.locationCode" :value="l.locationId" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品" required>
          <el-select v-model="inboundDialog.form.goodsId" filterable style="width: 100%">
            <el-option v-for="g in goodsList" :key="g.id" :label="`${g.name}（${g.skuCode}）`" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="inboundDialog.form.quantity" :min="1" style="width: 160px" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="inboundDialog.form.batchNo" placeholder="留空则并入无批次库存" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="inboundDialog.saving" @click="doInbound">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 调整弹窗 -->
    <el-dialog v-model="adjustDialog.visible" title="库存调整" width="400px">
      <el-form label-width="90px">
        <el-form-item label="货位">
          <span>{{ adjustDialog.row?.locationCode }}</span>
        </el-form-item>
        <el-form-item label="商品">
          <span>{{ adjustDialog.row?.goodsName }}</span>
        </el-form-item>
        <el-form-item label="新数量" required>
          <el-input-number v-model="adjustDialog.quantity" :min="1" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="adjustDialog.saving" @click="doAdjust">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageInventory, inbound, adjustInventory, deleteInventory, listGoods } from '../api/stock'
import { searchLocations } from '../api/dashboard'

const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const goodsList = ref([])

const inboundDialog = reactive({
  visible: false, saving: false, locLoading: false,
  locationOptions: [],
  form: { locationId: null, goodsId: null, quantity: 1, batchNo: '' }
})
const adjustDialog = reactive({ visible: false, saving: false, row: null, quantity: 1 })

const load = async () => {
  loading.value = true
  try {
    const res = await pageInventory(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const reset = () => { query.keyword = ''; query.pageNum = 1; load() }

const openInbound = () => {
  inboundDialog.form = { locationId: null, goodsId: null, quantity: 1, batchNo: '' }
  inboundDialog.locationOptions = []
  inboundDialog.visible = true
}

const searchLocationOptions = async (kw) => {
  if (!kw) {
    inboundDialog.locationOptions = []
    return
  }
  inboundDialog.locLoading = true
  try {
    const res = await searchLocations(kw)
    inboundDialog.locationOptions = res.data || []
  } finally {
    inboundDialog.locLoading = false
  }
}

const doInbound = async () => {
  if (!inboundDialog.form.locationId || !inboundDialog.form.goodsId) {
    ElMessage.warning('请选择货位与商品')
    return
  }
  inboundDialog.saving = true
  try {
    await inbound(inboundDialog.form)
    ElMessage.success('入库成功')
    inboundDialog.visible = false
    load()
  } finally {
    inboundDialog.saving = false
  }
}

const openAdjust = (row) => {
  adjustDialog.row = row
  adjustDialog.quantity = row.quantity
  adjustDialog.visible = true
}

const doAdjust = async () => {
  adjustDialog.saving = true
  try {
    await adjustInventory(adjustDialog.row.id, adjustDialog.quantity)
    ElMessage.success('调整成功')
    adjustDialog.visible = false
    load()
  } finally {
    adjustDialog.saving = false
  }
}

const remove = async (id) => {
  await deleteInventory(id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  const res = await listGoods()
  goodsList.value = res.data || []
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
