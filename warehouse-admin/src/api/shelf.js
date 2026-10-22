import request from '../utils/request'

// 货架
export const pageShelves = (params) => request.get('/shelf/page', { params })
export const listShelvesBySub = (subWarehouseId) => request.get(`/shelf/by-sub/${subWarehouseId}`)
export const batchCreateShelf = (data) => request.post('/shelf/batch', data)
export const saveAllShelves = (data) => request.post('/shelf/save-all', data)
export const updateShelf = (data) => request.put('/shelf', data)
export const deleteShelf = (id) => request.delete(`/shelf/${id}`)

// 货位
export const listLocationsByShelf = (shelfId) => request.get(`/location/by-shelf/${shelfId}`)
export const listLocationsBySub = (subWarehouseId) => request.get(`/location/by-sub/${subWarehouseId}`)
export const getLocationDetail = (id) => request.get(`/location/${id}`)

// 平面图
export const getLayoutByWarehouse = (warehouseId) => request.get(`/layout/by-warehouse/${warehouseId}`)
export const saveLayout = (data) => request.post('/layout/save', data)
export const importLayout = (formData) =>
  request.post('/layout/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
