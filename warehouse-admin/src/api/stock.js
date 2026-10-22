import request from '../utils/request'

// 商品
export const pageGoods = (params) => request.get('/goods/page', { params })
export const listGoods = () => request.get('/goods/all')
export const createGoods = (data) => request.post('/goods', data)
export const updateGoods = (data) => request.put('/goods', data)
export const deleteGoods = (id) => request.delete(`/goods/${id}`)

// 库存
export const pageInventory = (params) => request.get('/inventory/page', { params })
export const inbound = (data) => request.post('/inventory/inbound', data)
export const adjustInventory = (id, quantity) => request.put(`/inventory/${id}`, null, { params: { quantity } })
export const deleteInventory = (id) => request.delete(`/inventory/${id}`)
