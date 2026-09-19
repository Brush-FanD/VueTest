import http from './http'

const CONVERSATION_KEY = 'aichat:conversationId'

/** 流结束标记：后端在 Flux 末尾追加，前端据此关闭 EventSource */
export const STREAM_DONE = '[COMPLETE]'

/**
 * 取当前会话 ID：存在 sessionStorage 里，同一次浏览器会话保持同一个值，
 * AI 才能记住上文（原来的实现每次请求都新建 ID，等于没有上下文）。
 */
export const getConversationId = () => {
  let id = sessionStorage.getItem(CONVERSATION_KEY)
  if (!id) {
    id = `web-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
    sessionStorage.setItem(CONVERSATION_KEY, id)
  }
  return id
}

/** 清空后端记忆，失败也不影响用户继续对话 */
export const clearMemory = (conversationId) =>
  http.delete('/web/aichat/memory', { params: { conversationId }, silent: true })

/** 开启新会话：后端记忆清空 + 本地会话 ID 重置 */
export const newConversation = async () => {
  const previous = sessionStorage.getItem(CONVERSATION_KEY)
  sessionStorage.removeItem(CONVERSATION_KEY)
  if (previous) {
    await clearMemory(previous)
  }
  return getConversationId()
}

/** 同步对话，返回 AI 回复文本 */
export const chatSync = (message, conversationId = getConversationId()) =>
  http.post('/web/aichat', { message, conversationId })

/** EventSource 只能发 GET，所以流式对话的参数走查询串 */
export const chatStreamUrl = (message, conversationId = getConversationId()) => {
  const base = http.defaults.baseURL || ''
  return `${base}/web/chatstream?message=${encodeURIComponent(message)}&conversationId=${encodeURIComponent(conversationId)}`
}
