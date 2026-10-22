import request from '../utils/request'

// 在线绘制仓库（方案）
export const getPlan = () => request.get('/plan')
export const savePlan = (data) => request.post('/plan/save', data)
export const importPlanBackground = (formData) =>
  request.post('/plan/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
