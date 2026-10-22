import request from '../utils/request'

// 看板
export const warehouseDashboard = (warehouseId) => request.get(`/dashboard/warehouse/${warehouseId}`)
export const subDashboard = (subId) => request.get(`/dashboard/sub/${subId}`)

// 智能找货
export const searchLocations = (keyword) => request.get('/search', { params: { keyword } })
