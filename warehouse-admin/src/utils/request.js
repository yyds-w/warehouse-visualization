import axios from 'axios'
import { ElMessage } from 'element-plus'

// Vite dev server（5174 端口）走同源代理：/api → 8081
// 双击 html（file://）或本地静态预览时，直连后端绝对地址
const isDevServer = window.location.protocol.startsWith('http') && window.location.port === '5174'
const baseURL = isDevServer ? '/api' : 'http://localhost:8081/api'

const request = axios.create({
  baseURL,
  timeout: 15000
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 200) {
      return res
    }
    ElMessage.error((res && res.message) || '请求失败')
    return Promise.reject(new Error((res && res.message) || '请求失败'))
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误，请检查后端服务'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
