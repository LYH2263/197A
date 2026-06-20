import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const user = useUserStore()
  if (user.token) config.headers.Authorization = `Bearer ${user.token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    let msg = err.message || '网络错误'
    if (err.response?.data) {
      if (err.response.data instanceof Blob) {
        try {
          const text = await err.response.data.text()
          const json = JSON.parse(text)
          msg = json.message || msg
        } catch {
          msg = err.response.statusText || msg
        }
      } else if (err.response.data.message) {
        msg = err.response.data.message
      }
    }
    ElMessage.error(msg)
    if (err.response?.status === 401) {
      useUserStore().logout()
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(err)
  }
)

export default api
