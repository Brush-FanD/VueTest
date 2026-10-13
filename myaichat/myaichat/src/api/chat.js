import http from './http'

/** 流结束标记：后端在 Flux 末尾追加，前端据此结束读取 */
export const STREAM_DONE = '[COMPLETE]'

/**
 * 会话 ID 现在由服务端签发并存在 HttpOnly Cookie 里，前端读不到也改不了，
 * 所以这里不再自己生成 / 传递 conversationId，只需通知后端清空当前会话记忆。
 */
export const newConversation = () =>
  http.delete('/web/aichat/memory', { silent: true })

/** 同步对话，返回 AI 回复文本 */
export const chatSync = (message) => http.post('/web/aichat', { message })

/** 流式对话地址：会话身份走 Cookie，不再把会话 ID 拼进查询串 */
export const chatStreamUrl = (message) => {
  const base = http.defaults.baseURL || ''
  return `${base}/web/chatstream?message=${encodeURIComponent(message)}`
}