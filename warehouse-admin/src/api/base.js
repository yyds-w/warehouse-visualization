import request from '../utils/request'

// 仓库
export const pageWarehouses = (params) => request.get('/warehouse/page', { params })
export const listWarehouses = () => request.get('/warehouse/all')
export const getWarehouse = (id) => request.get(`/warehouse/${id}`)
export const createWarehouse = (data) => request.post('/warehouse', data)
export const updateWarehouse = (data) => request.put('/warehouse', data)
export const deleteWarehouse = (id) => request.delete(`/warehouse/${id}`)

// 子仓库
export const pageSubWarehouses = (params) => request.get('/sub-warehouse/page', { params })
export const listSubWarehouses = (warehouseId) => request.get(`/sub-warehouse/by-warehouse/${warehouseId}`)
export const createSubWarehouse = (data) => request.post('/sub-warehouse', data)
export const updateSubWarehouse = (data) => request.put('/sub-warehouse', data)
export const deleteSubWarehouse = (id) => request.delete(`/sub-warehouse/${id}`)

// 库管用户
export const pageUsers = (params) => request.get('/user/page', { params })
export const listUsers = () => request.get('/user/all')
export const createUser = (data) => request.post('/user', data)
export const updateUser = (data) => request.put('/user', data)
export const deleteUser = (id) => request.delete(`/user/${id}`)
