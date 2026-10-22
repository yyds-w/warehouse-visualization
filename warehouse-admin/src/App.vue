<template>
  <router-view v-if="route.meta.bare" />
  <el-container v-else class="app-shell">
    <el-aside width="220px" class="app-aside">
      <div class="app-logo">简简仓库可视化</div>
      <el-menu :default-active="activeMenu" router class="app-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>全景看板</span>
        </el-menu-item>
        <el-menu-item index="/panorama">
          <el-icon><MapLocation /></el-icon>
          <span>全景可视图</span>
        </el-menu-item>
        <el-menu-item index="/drawing">
          <el-icon><EditPen /></el-icon>
          <span>在线绘制</span>
        </el-menu-item>
        <el-menu-item index="/warehouse">
          <el-icon><OfficeBuilding /></el-icon>
          <span>仓库管理</span>
        </el-menu-item>
        <el-menu-item index="/shelf">
          <el-icon><Grid /></el-icon>
          <span>货架配置</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>智能找货</span>
        </el-menu-item>
        <el-menu-item index="/inventory">
          <el-icon><Box /></el-icon>
          <span>库存管理</span>
        </el-menu-item>
        <el-menu-item index="/goods">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, OfficeBuilding, Grid, Search, Box, Goods, EditPen, MapLocation } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => {
  if (route.path.startsWith('/region')) return '/dashboard'
  if (route.path.startsWith('/layout')) return '/warehouse'
  if (route.path.startsWith('/panorama')) return '/panorama'
  return route.path
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
.app-shell {
  height: 100vh;
}
.app-aside {
  background: #001529;
}
.app-logo {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  line-height: 56px;
  letter-spacing: 1px;
}
.app-menu {
  border-right: none;
  background: #001529;
}
.app-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.75);
}
.app-menu .el-menu-item.is-active {
  color: #fff;
  background: #1890ff;
}
.app-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}
.app-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
</style>
