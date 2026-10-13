import { nextTick, ref } from 'vue'
import { STREAM_DONE, chatStreamUrl, chatSync, newConversation } from '../api/chat'

let messageSeq = 0
const nextId = () => (messageSeq += 1)

/**
 * AI 对话逻辑：同步 / 流式两种模式，首页弹层与 AI 页面共用。
 *
 * 流式这里没有用 EventSource，而是自己 fetch + 读响应流：
 * 1. EventSource 只能在连接异常时给一个笼统的 error 事件，拿不到 HTTP 状态码和错误体，
 *    出问题时界面上只能看到一个空消息，没法提示到底发生了什么；
 * 2. EventSource 失败后会自动重连，后端已经返回过的内容可能被重复请求；
 * 3. 用 fetch + AbortController 才能做「停止生成」。
 */
export function useChat(containerRef) {
  const messages = ref([])
  const input = ref('')
  const loading = ref(false)
  const mode = ref('stream')
  const error = ref('')

  let controller = null

  const scrollToBottom = () => {
    nextTick(() => {
      const container = containerRef?.value
      if (container) container.scrollTop = container.scrollHeight
    })
  }

  const addMessage = (role, content, extra = {}) => {
    const message = { id: nextId(), role, content, streaming: false, time: Date.now(), ...extra }
    messages.value.push(message)
    return message
  }

  const lastAssistant = () => [...messages.value].reverse().find((msg) => msg.role === 'assistant')

  /** 主动停止：中断请求，并把已经收到的内容保留下来 */
  const stop = () => {
    if (controller) {
      controller.abort()
      controller = null
    }
    const target = lastAssistant()
    if (target?.streaming) {
      target.streaming = false
      target.stopped = true
      if (!target.content) target.content = '（已停止生成）'
    }
    loading.value = false
  }

  /** 把 SSE 文本流逐帧拼成消息内容 */
  const consumeSse = async (body, onDelta) => {
    const reader = body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const flushFrame = (frame) => {
      const data = frame
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).replace(/^ /, ''))
        .join('\n')
      if (!data) return false
      if (data === STREAM_DONE) return true
      onDelta(data)
      return false
    }

    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let index = buffer.indexOf('\n\n')
      while (index !== -1) {
        const frame = buffer.slice(0, index)
        buffer = buffer.slice(index + 2)
        if (flushFrame(frame)) return
        index = buffer.indexOf('\n\n')
      }
    }
    // 流结束时缓冲区里可能还有最后一帧
    if (buffer.trim()) flushFrame(buffer)
  }

  const sendStream = async (text) => {
    const reply = addMessage('assistant', '')
    reply.streaming = true
    controller = new AbortController()
    try {
      const response = await fetch(chatStreamUrl(text), {
        signal: controller.signal,
        // 会话身份在 HttpOnly Cookie 里，流式请求也必须带上凭据
        credentials: 'include',
        headers: { Accept: 'text/event-stream' }
      })
      if (!response.ok) {
        throw new Error(response.status === 404 ? '接口不存在，请确认后端已启动' : `服务返回 ${response.status}`)
      }
      if (!response.body) throw new Error('当前浏览器不支持流式读取')

      await consumeSse(response.body, (delta) => {
        reply.content += delta
        scrollToBottom()
      })
      if (!reply.content) reply.content = '（没有收到内容，请重试）'
    } catch (err) {
      if (err.name === 'AbortError') return
      error.value = err.message || '流式请求失败'
      reply.content = reply.content || `请求失败：${error.value}`
      reply.failed = true
    } finally {
      reply.streaming = false
      controller = null
      loading.value = false
      scrollToBottom()
    }
  }

  const sendSync = async (text) => {
    try {
      const reply = await chatSync(text)
      addMessage('assistant', reply || '（没有收到内容）')
    } catch (err) {
      error.value = err.message || '请求失败'
      addMessage('assistant', `请求失败：${error.value}`, { failed: true })
    } finally {
      loading.value = false
      scrollToBottom()
    }
  }

  /** 发送消息，不传参数则发送输入框内容 */
  const sendText = async (text) => {
    const content = (text ?? input.value).trim()
    if (!content || loading.value) return
    error.value = ''
    addMessage('user', content)
    input.value = ''
    loading.value = true
    scrollToBottom()
    await (mode.value === 'stream' ? sendStream(content) : sendSync(content))
  }

  const send = () => sendText()

  /** 重新生成：丢掉最后一条 AI 回复，用上一条用户提问再问一次 */
  const retry = async () => {
    if (loading.value) return
    const target = lastAssistant()
    const question = [...messages.value].reverse().find((msg) => msg.role === 'user')
    if (!question) return
    if (target) messages.value = messages.value.filter((msg) => msg.id !== target.id)
    loading.value = true
    scrollToBottom()
    await (mode.value === 'stream' ? sendStream(question.content) : sendSync(question.content))
  }

  /** 开启新会话：清空当前消息并重置后端记忆 */
  const reset = async () => {
    stop()
    messages.value = []
    error.value = ''
    loading.value = false
    await newConversation()
  }

  return { messages, input, loading, mode, error, send, sendText, stop, retry, reset, scrollToBottom }
}