import request from '../utils/request'

// 全景可视图：仓库全景卡片
export const panoramaOverview = () => request.get('/panorama/overview')

// 仓库平面图与数据看板
export const panoramaBoard = (warehouseId) => request.get(`/panorama/warehouse/${warehouseId}`)
