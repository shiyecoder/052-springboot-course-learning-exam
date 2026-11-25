import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一 axios 实例：自动带 token、统一解包 {code,message,data}、401 回登录页
const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('ce_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 200) return body.data
      if (body.code === 401) {
        localStorage.removeItem('ce_token')
        if (location.hash !== '#/login') location.hash = '#/login'
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '网络异常'
    if (status === 401 || status === 403) {
      ElMessage.error(status === 401 ? '登录已过期，请重新登录' : '没有该操作的权限')
      if (status === 401) {
        localStorage.removeItem('ce_token')
        location.hash = '#/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default http
