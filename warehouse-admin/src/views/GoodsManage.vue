<template>
  <div>
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="商品名 / SKU" clearable style="width: 200px"
                  @keyup.enter="load" />
        <el-input v-model="query.category" placeholder="分类" clearable style="width: 140px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="primary" plain @click="openDialog()">新增商品</el-button>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="skuCode" label="SKU 编码" width="120" />
        <el-table-column prop="name" label="商品名称" min-width="160" />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column prop="spec" label="规格" min-width="150" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除该商品？" @confirm="remove(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无商品数据" /></template>
      </el-table>
      <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                     :page-size="query.pageSize" :current-page="query.pageNum"
                     @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑商品' : '新增商品'" width="480px">
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-width="90px">
        <el-form-item label="SKU 编码" prop="skuCode">
          <el-input v-model="dialog.form.skuCode" placeholder="如 SKU-1003" />
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="dialog.form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="dialog.form.category" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="dialog.form.spec" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="dialog.form.unit" style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dialog.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageGoods, createGoods, updateGoods, deleteGoods } from '../api/stock'

const query = reactive({ keyword: '', category: '', pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const formRef = ref(null)
const dialog = reactive({ visible: false, saving: false, form: {} })
const rules = {
  skuCode: [{ required: true, message: 'SKU 编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await pageGoods(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const reset = () => { query.keyword = ''; query.category = ''; query.pageNum = 1; load() }

const openDialog = (row) => {
  dialog.form = row ? { ...row } : { skuCode: '', name: '', category: '', spec: '', unit: '件', status: 1 }
  dialog.visible = true
}

const save = async () => {
  await formRef.value.validate()
  dialog.saving = true
  try {
    if (dialog.form.id) {
      await updateGoods(dialog.form)
    } else {
      await createGoods(dialog.form)
    }
    ElMessage.success('保存成功')
    dialog.visible = false
    load()
  } finally {
    dialog.saving = false
  }
}

const remove = async (id) => {
  await deleteGoods(id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
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
