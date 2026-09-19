import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 全局唯一的 axios 实例。
 *
 * - baseURL 默认 /api：开发环境交给 Vite 代理转发到后端 8123，生产环境由 Nginx 同源代理
 * - AI 接口返回 { code, msg, data } 包装结构，这里自动解包成 data
 * - 视频 / 评论接口直接返回数组或对象，原样透传
 * - 失败时统一弹提示，单个请求可用 { silent: true } 关掉
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
  silent: false
})

/** 只解包 { code, msg, data } 结构，避免误伤业务对象里同名字段 */
const unwrap = (body) => {
  const isResultWrapper =
    body && typeof body === 'object' && !Array.isArray(body) && 'code' in body && 'data' in body
  if (!isResultWrapper) return body
  if (body.code === 200) return body.data
  throw new Error(body.msg || '请求失败')
}

const resolveErrorMessage = (error) => {
  if (error.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  const data = error.response?.data
  if (data?.msg) return data.msg
  if (data?.message && data.message !== 'No message available') return data.message
  if (error.response) return `请求失败（${error.response.status}）`
  return '网络连接失败，请检查网络'
}

http.interceptors.response.use(
  (response) => unwrap(response.data),
  (error) => {
    const message = resolveErrorMessage(error)
    if (!error.config?.silent) {
      ElMessage.error(message)
    }
    const wrapped = new Error(message)
    wrapped.status = error.response?.status
    wrapped.config = error.config
    return Promise.reject(wrapped)
  }
)

export default http
