<template>
  <div>
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ===================== 仓库 ===================== -->
        <el-tab-pane label="仓库信息" name="warehouse">
          <div class="filter-bar">
            <el-input v-model="whQuery.keyword" placeholder="名称 / 编码" clearable style="width: 200px"
                      @keyup.enter="loadWarehouses" />
            <el-select v-model="whQuery.status" placeholder="状态" clearable style="width: 110px">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadWarehouses">查询</el-button>
            <el-button @click="resetWh">重置</el-button>
            <el-button type="primary" plain @click="openWhDialog()">新增仓库</el-button>
          </div>
          <el-table :data="whList" v-loading="whLoading" border stripe>
            <el-table-column prop="code" label="编码" width="90" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
            <el-table-column prop="area" label="总面积(㎡)" width="110" align="right" />
            <el-table-column prop="usableArea" label="可用面积(㎡)" width="120" align="right" />
            <el-table-column label="形状" width="90" align="center">
              <template #default="{ row }">{{ shapeName(row.shapeType) }}</template>
            </el-table-column>
            <el-table-column label="尺寸(长×宽×高m)" width="140" align="center">
              <template #default="{ row }">
                {{ row.lengthM != null ? `${row.lengthM}×${row.widthM}×${row.heightM}` : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="capacity" label="容量" width="90" align="right" />
            <el-table-column prop="maxWeight" label="最大重量" width="100" align="right" />
            <el-table-column prop="volume" label="立方数(m³)" width="100" align="right" />
            <el-table-column label="负责人/部门" min-width="140">
              <template #default="{ row }">
                {{ row.managerName ? `${row.managerName}${row.department ? ' / ' + row.department : ''}` : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="goLayout(row)">平面图</el-button>
                <el-button size="small" type="primary" link @click="openWhDialog(row)">编辑</el-button>
                <el-popconfirm title="确认删除该仓库？" @confirm="removeWarehouse(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无仓库数据" /></template>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next" :total="whTotal"
                         :page-size="whQuery.pageSize" :current-page="whQuery.pageNum"
                         @current-change="(p) => { whQuery.pageNum = p; loadWarehouses() }" />
        </el-tab-pane>

        <!-- ===================== 子仓库 ===================== -->
        <el-tab-pane label="子仓库" name="sub">
          <div class="filter-bar">
            <el-select v-model="subQuery.warehouseId" placeholder="所属仓库" style="width: 200px" @change="loadSubs">
              <el-option v-for="w in allWarehouses" :key="w.id" :label="w.name" :value="w.id" />
            </el-select>
            <el-input v-model="subQuery.keyword" placeholder="名称 / 编码" clearable style="width: 180px"
                      @keyup.enter="loadSubs" />
            <el-button type="primary" @click="loadSubs">查询</el-button>
            <el-button type="primary" plain @click="openSubDialog()">新增子仓库</el-button>
          </div>
          <el-table :data="subList" v-loading="subLoading" border stripe>
            <el-table-column prop="code" label="编码" width="90" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="area" label="面积(㎡)" width="110" align="right" />
            <el-table-column label="平面图位置" min-width="200">
              <template #default="{ row }">
                ({{ row.positionX }}, {{ row.positionY }})　{{ row.width }} × {{ row.height }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="openSubDialog(row)">编辑</el-button>
                <el-popconfirm title="确认删除该子仓库？" @confirm="removeSub(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty><el-empty description="请选择仓库或暂无子仓库" /></template>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next" :total="subTotal"
                         :page-size="subQuery.pageSize" :current-page="subQuery.pageNum"
                         @current-change="(p) => { subQuery.pageNum = p; loadSubs() }" />
        </el-tab-pane>

        <!-- ===================== 库管 ===================== -->
        <el-tab-pane label="库管人员" name="user">
          <div class="filter-bar">
            <el-input v-model="userQuery.keyword" placeholder="用户名 / 姓名" clearable style="width: 180px"
                      @keyup.enter="loadUsers" />
            <el-select v-model="userQuery.role" placeholder="角色" clearable style="width: 120px">
              <el-option label="管理员" :value="1" />
              <el-option label="库管" :value="2" />
              <el-option label="拣货员" :value="3" />
            </el-select>
            <el-button type="primary" @click="loadUsers">查询</el-button>
            <el-button type="primary" plain @click="openUserDialog()">新增人员</el-button>
          </div>
          <el-table :data="userList" v-loading="userLoading" border stripe>
            <el-table-column prop="username" label="用户名" width="130" />
            <el-table-column prop="realName" label="姓名" width="110" />
            <el-table-column prop="phone" label="电话" width="150" />
            <el-table-column label="角色" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="roleTag(row.role)">{{ roleName(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="openUserDialog(row)">编辑</el-button>
                <el-popconfirm title="确认删除该人员？" @confirm="removeUser(row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无人员数据" /></template>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next" :total="userTotal"
                         :page-size="userQuery.pageSize" :current-page="userQuery.pageNum"
                         @current-change="(p) => { userQuery.pageNum = p; loadUsers() }" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 仓库弹窗 -->
    <el-dialog v-model="whDialog.visible" :title="whDialog.form.id ? '编辑仓库' : '新增仓库'" width="560px">
      <el-form ref="whFormRef" :model="whDialog.form" :rules="whRules" label-width="100px">
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="whDialog.form.code" placeholder="如 WH03" />
        </el-form-item>
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="whDialog.form.name" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="whDialog.form.address" />
        </el-form-item>
        <el-form-item label="总面积(㎡)">
          <el-input-number v-model="whDialog.form.area" :min="0" :precision="2" style="width: 180px" />
        </el-form-item>
        <el-form-item label="可用面积(㎡)">
          <el-input-number v-model="whDialog.form.usableArea" :min="0" :precision="2" style="width: 180px" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="whDialog.form.managerId" clearable style="width: 180px">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="whDialog.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="whDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="whDialog.saving" @click="saveWarehouse">保存</el-button>
      </template>
    </el-dialog>

    <!-- 子仓库弹窗 -->
    <el-dialog v-model="subDialog.visible" :title="subDialog.form.id ? '编辑子仓库' : '新增子仓库'" width="560px">
      <el-form ref="subFormRef" :model="subDialog.form" :rules="subRules" label-width="110px">
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select v-model="subDialog.form.warehouseId" style="width: 220px">
            <el-option v-for="w in allWarehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="子仓库编码" prop="code">
          <el-input v-model="subDialog.form.code" placeholder="如 D01" />
        </el-form-item>
        <el-form-item label="子仓库名称" prop="name">
          <el-input v-model="subDialog.form.name" />
        </el-form-item>
        <el-form-item label="面积(㎡)">
          <el-input-number v-model="subDialog.form.area" :min="0" :precision="2" style="width: 160px" />
        </el-form-item>
        <el-form-item label="平面图坐标">
          <el-input-number v-model="subDialog.form.positionX" :min="0" placeholder="X" style="width: 110px" />
          <el-input-number v-model="subDialog.form.positionY" :min="0" placeholder="Y" style="width: 110px; margin-left: 8px" />
        </el-form-item>
        <el-form-item label="尺寸(宽×高)">
          <el-input-number v-model="subDialog.form.width" :min="0" placeholder="宽" style="width: 110px" />
          <el-input-number v-model="subDialog.form.height" :min="0" placeholder="高" style="width: 110px; margin-left: 8px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="subDialog.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="subDialog.saving" @click="saveSub">保存</el-button>
      </template>
    </el-dialog>

    <!-- 人员弹窗 -->
    <el-dialog v-model="userDialog.visible" :title="userDialog.form.id ? '编辑人员' : '新增人员'" width="480px">
      <el-form ref="userFormRef" :model="userDialog.form" :rules="userRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userDialog.form.username" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="userDialog.form.realName" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="userDialog.form.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userDialog.form.role" style="width: 160px">
            <el-option label="管理员" :value="1" />
            <el-option label="库管" :value="2" />
            <el-option label="拣货员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userDialog.form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="userDialog.saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  pageWarehouses, listWarehouses, createWarehouse, updateWarehouse, deleteWarehouse,
  pageSubWarehouses, createSubWarehouse, updateSubWarehouse, deleteSubWarehouse,
  pageUsers, listUsers, createUser, updateUser, deleteUser
} from '../api/base'

const router = useRouter()
const activeTab = ref('warehouse')

// ---------- 仓库 ----------
const whQuery = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })
const whList = ref([])
const whTotal = ref(0)
const whLoading = ref(false)
const allWarehouses = ref([])
const allUsers = ref([])
const whFormRef = ref(null)
const whDialog = reactive({ visible: false, saving: false, form: {} })
const whRules = {
  code: [{ required: true, message: '仓库编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }]
}

const loadWarehouses = async () => {
  whLoading.value = true
  try {
    const res = await pageWarehouses(whQuery)
    whList.value = res.data.list
    whTotal.value = res.data.total
  } finally {
    whLoading.value = false
  }
}
const resetWh = () => { whQuery.keyword = ''; whQuery.status = null; whQuery.pageNum = 1; loadWarehouses() }
const openWhDialog = (row) => {
  whDialog.form = row ? { ...row } : { code: '', name: '', address: '', area: null, usableArea: null, managerId: null, status: 1 }
  whDialog.visible = true
}
const saveWarehouse = async () => {
  await whFormRef.value.validate()
  whDialog.saving = true
  try {
    if (whDialog.form.id) {
      await updateWarehouse(whDialog.form)
    } else {
      await createWarehouse(whDialog.form)
    }
    ElMessage.success('保存成功')
    whDialog.visible = false
    loadWarehouses()
    refreshAllWarehouses()
  } finally {
    whDialog.saving = false
  }
}
const removeWarehouse = async (id) => {
  await deleteWarehouse(id)
  ElMessage.success('删除成功')
  loadWarehouses()
  refreshAllWarehouses()
}
const goLayout = (row) => router.push(`/layout/${row.id}`)
const shapeName = (t) => ({ square: '正方形', rect: '长方形', parallelogram: '平行四边形', trapezoid: '梯形' }[t] || '—')

// ---------- 子仓库 ----------
const subQuery = reactive({ warehouseId: null, keyword: '', pageNum: 1, pageSize: 10 })
const subList = ref([])
const subTotal = ref(0)
const subLoading = ref(false)
const subFormRef = ref(null)
const subDialog = reactive({ visible: false, saving: false, form: {} })
const subRules = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  code: [{ required: true, message: '子仓库编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '子仓库名称不能为空', trigger: 'blur' }]
}

const loadSubs = async () => {
  subLoading.value = true
  try {
    const res = await pageSubWarehouses(subQuery)
    subList.value = res.data.list
    subTotal.value = res.data.total
  } finally {
    subLoading.value = false
  }
}
const openSubDialog = (row) => {
  subDialog.form = row ? { ...row } : {
    warehouseId: subQuery.warehouseId, code: '', name: '', area: null,
    positionX: 50, positionY: 50, width: 200, height: 150, status: 1
  }
  subDialog.visible = true
}
const saveSub = async () => {
  await subFormRef.value.validate()
  subDialog.saving = true
  try {
    if (subDialog.form.id) {
      await updateSubWarehouse(subDialog.form)
    } else {
      await createSubWarehouse(subDialog.form)
    }
    ElMessage.success('保存成功')
    subDialog.visible = false
    loadSubs()
  } finally {
    subDialog.saving = false
  }
}
const removeSub = async (id) => {
  await deleteSubWarehouse(id)
  ElMessage.success('删除成功')
  loadSubs()
}

// ---------- 库管 ----------
const userQuery = reactive({ keyword: '', role: null, pageNum: 1, pageSize: 10 })
const userList = ref([])
const userTotal = ref(0)
const userLoading = ref(false)
const userFormRef = ref(null)
const userDialog = reactive({ visible: false, saving: false, form: {} })
const userRules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  phone: [{ pattern: /^(\d{3,4}-?\d{7,8}|1[3-9]\d{9})?$/, message: '电话格式不正确', trigger: 'blur' }]
}
const roleName = (r) => ({ 1: '管理员', 2: '库管', 3: '拣货员' }[r] || '-')
const roleTag = (r) => ({ 1: 'danger', 2: 'primary', 3: 'warning' }[r] || 'info')

const loadUsers = async () => {
  userLoading.value = true
  try {
    const res = await pageUsers(userQuery)
    userList.value = res.data.list
    userTotal.value = res.data.total
  } finally {
    userLoading.value = false
  }
}
const openUserDialog = (row) => {
  userDialog.form = row ? { ...row } : { username: '', realName: '', phone: '', role: 2, status: 1 }
  userDialog.visible = true
}
const saveUser = async () => {
  await userFormRef.value.validate()
  userDialog.saving = true
  try {
    if (userDialog.form.id) {
      await updateUser(userDialog.form)
    } else {
      await createUser(userDialog.form)
    }
    ElMessage.success('保存成功')
    userDialog.visible = false
    loadUsers()
    refreshAllUsers()
  } finally {
    userDialog.saving = false
  }
}
const removeUser = async (id) => {
  await deleteUser(id)
  ElMessage.success('删除成功')
  loadUsers()
  refreshAllUsers()
}

const refreshAllWarehouses = async () => {
  const res = await listWarehouses()
  allWarehouses.value = res.data || []
  if (!subQuery.warehouseId && allWarehouses.value.length) {
    subQuery.warehouseId = allWarehouses.value[0].id
  }
}
const refreshAllUsers = async () => {
  const res = await listUsers()
  allUsers.value = res.data || []
}

onMounted(async () => {
  await refreshAllWarehouses()
  await refreshAllUsers()
  loadWarehouses()
  loadSubs()
  loadUsers()
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
