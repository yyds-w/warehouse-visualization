import { createRouter, createWebHashHistory } from 'vue-router'

// 静态导入（而非 () => import(...)）：避免构建产物里出现 import.meta.url，
// 这样单文件产物可以用普通 <script> 内联，在 file:// 协议下双击直接打开。
import Dashboard from '../views/Dashboard.vue'
import WarehouseManage from '../views/WarehouseManage.vue'
import DrawingBoard from '../views/DrawingBoard.vue'
import LayoutEditor from '../views/LayoutEditor.vue'
import ShelfConfig from '../views/ShelfConfig.vue'
import RegionView from '../views/RegionView.vue'
import SearchView from '../views/SearchView.vue'
import InventoryManage from '../views/InventoryManage.vue'
import GoodsManage from '../views/GoodsManage.vue'
import PanoramaView from '../views/PanoramaView.vue'
import WarehouseBoardView from '../views/WarehouseBoardView.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'Dashboard', component: Dashboard, meta: { title: '全景可视化看板' } },
  { path: '/drawing', name: 'DrawingBoard', component: DrawingBoard, meta: { title: '在线绘制仓库' } },
  { path: '/warehouse', name: 'WarehouseManage', component: WarehouseManage, meta: { title: '仓库基础信息管理' } },
  { path: '/layout/:warehouseId', name: 'LayoutEditor', component: LayoutEditor, meta: { title: '仓库平面图绘制' } },
  { path: '/shelf', name: 'ShelfConfig', component: ShelfConfig, meta: { title: '货架配置' } },
  { path: '/region/:subId', name: 'RegionView', component: RegionView, meta: { title: '区域货架视图' } },
  { path: '/search', name: 'SearchView', component: SearchView, meta: { title: '智能找货' } },
  { path: '/inventory', name: 'InventoryManage', component: InventoryManage, meta: { title: '库存管理' } },
  { path: '/goods', name: 'GoodsManage', component: GoodsManage, meta: { title: '商品管理' } },
  { path: '/panorama', name: 'PanoramaView', component: PanoramaView, meta: { title: '仓库全景可视图', bare: true } },
  { path: '/panorama/warehouse/:id', name: 'WarehouseBoardView', component: WarehouseBoardView, meta: { title: '平面图与数据看板', bare: true } },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

// hash 模式：兼容 Vite dev server，也支持 file:// 直接双击打开
const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '简简仓库可视化系统'
})

export default router
