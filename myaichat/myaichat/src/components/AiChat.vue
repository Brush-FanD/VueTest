<template>
  <div class="chat-page">
    <header class="chat-head">
      <div class="head-left">
        <div class="head-avatar">AI</div>
        <div class="head-meta">
          <strong>懂车AI</strong>
          <span>{{ loading ? '正在思考…' : '汽车问题随时问' }}</span>
        </div>
      </div>
      <div class="head-right">
        <button class="ghost-btn" type="button" @click="toggleMode" :title="mode === 'stream' ? '已开启流式输出' : '已切换为整段返回'">
          {{ mode === 'stream' ? '流式' : '整段' }}
        </button>
        <button class="ghost-btn" type="button" @click="router.push('/knowledge')">知识库</button>
        <button class="ghost-btn" type="button" @click="reset">新会话</button>
      </div>
    </header>

    <div class="chat-body" ref="messageContainer">
      <div v-if="!messages.length" class="welcome">
        <div class="welcome-logo">懂车AI</div>
        <p class="welcome-title">买车看车用车，都可以问我</p>
        <p class="welcome-sub">支持多轮记忆，回答会像打字一样逐字出现</p>
        <div class="suggest-list">
          <button
            v-for="item in suggestions"
            :key="item"
            class="suggest"
            type="button"
            @click="ask(item)"
          >{{ item }}</button>
        </div>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        class="msg-row"
        :class="msg.role === 'user' ? 'right' : 'left'"
      >
        <div class="msg-avatar" :class="msg.role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="msg-main">
          <div class="bubble" :class="[msg.role, { failed: msg.failed }]">
            <span class="bubble-text">{{ msg.content }}</span>
            <span v-if="msg.streaming" class="cursor"></span>
          </div>
          <div class="msg-foot">
            <span class="msg-time">{{ clock(msg.time) }}</span>
            <button
              v-if="msg.role === 'assistant' && !msg.streaming"
              class="foot-btn"
              type="button"
              @click="copy(msg.content)"
            >复制</button>
            <button
              v-if="msg.role === 'assistant' && msg.failed"
              class="foot-btn danger"
              type="button"
              @click="retry"
            >重新生成</button>
          </div>
        </div>
      </div>
    </div>

    <footer class="chat-foot">
      <div class="foot-row">
        <textarea
          v-model="input"
          class="input"
          rows="1"
          placeholder="输入汽车问题，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="send"
        ></textarea>
        <button v-if="loading" class="send-btn stop" type="button" @click="stop">停止</button>
        <button v-else class="send-btn" type="button" :disabled="!input.trim()" @click="send">发送</button>
      </div>
      <p class="foot-tip">回答由大模型生成，仅供参考</p>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useChat } from '../composables/useChat'

const route = useRoute()
const router = useRouter()
const messageContainer = ref(null)
const { messages, input, loading, mode, send, sendText, stop, retry, reset } = useChat(messageContainer)

const suggestions = [
  '20 万预算买什么 SUV 好？',
  '增程和插混到底怎么选？',
  '小米 SU7 值得买吗？',
  '纯电车冬天续航打折严重吗？'
]

const clock = (time) => {
  const date = new Date(time)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const toggleMode = () => {
  mode.value = mode.value === 'stream' ? 'sync' : 'stream'
}

const ask = (question) => sendText(question)

const copy = async (text) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('当前浏览器不允许读取剪贴板')
  }
}

onMounted(() => {
  // 支持从车型详情页带着问题跳过来，例如 /ai?q=理想L9值不值得买
  const question = route.query.q
  if (question) sendText(String(question))
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--c-bg);
}

/* 顶部 */
.chat-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #1F2329, #2C3138 60%, #3A2226);
  color: #fff;
}
.head-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.head-avatar {
  width: 34px; height: 34px; border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; letter-spacing: .5px;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 4px 12px rgba(224, 30, 38, .35);
}
.head-meta { display: flex; flex-direction: column; line-height: 1.25; }
.head-meta strong { font-size: 15px; }
.head-meta span { font-size: 11px; color: rgba(255, 255, 255, .62); }
.head-right { margin-left: auto; display: flex; gap: 6px; }
.ghost-btn {
  border: 1px solid rgba(255, 255, 255, .22);
  background: rgba(255, 255, 255, .08);
  color: #fff; font-size: 12px; font-family: inherit;
  padding: 5px 10px; border-radius: 999px; cursor: pointer;
}
.ghost-btn:active { background: rgba(255, 255, 255, .18); }

/* 消息区 */
.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 14px 4px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.welcome { text-align: center; padding: 26px 10px 10px; }
.welcome-logo {
  display: inline-block;
  font-weight: 800; font-size: 18px; letter-spacing: 1px;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.welcome-title { margin-top: 10px; font-size: 15px; font-weight: 600; }
.welcome-sub { margin-top: 4px; font-size: 12px; color: var(--c-text-3); }
.suggest-list {
  margin-top: 18px;
  display: flex; flex-direction: column; gap: 8px; align-items: stretch;
}
.suggest {
  text-align: left;
  border: 1px solid var(--c-line);
  background: #fff;
  border-radius: var(--r-md);
  padding: 11px 14px;
  font-size: 13px; color: var(--c-text-2); font-family: inherit;
  cursor: pointer;
  transition: border-color .15s ease, transform .12s ease;
}
.suggest:active { transform: scale(.98); border-color: var(--c-brand-line); }

.msg-row { display: flex; gap: 8px; align-items: flex-start; }
.msg-row.right { flex-direction: row-reverse; }
.msg-avatar {
  flex-shrink: 0;
  width: 30px; height: 30px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600; color: #fff;
}
.msg-avatar.assistant { background: linear-gradient(135deg, #2C3138, #4A5158); }
.msg-avatar.user { background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2)); }

.msg-main { max-width: 78%; display: flex; flex-direction: column; gap: 3px; }
.msg-row.right .msg-main { align-items: flex-end; }

.bubble {
  padding: 10px 13px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.62;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble.assistant {
  background: #fff;
  border: 1px solid var(--c-line);
  border-top-left-radius: 4px;
  box-shadow: var(--sh-1);
}
.bubble.user {
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  color: #fff;
  border-top-right-radius: 4px;
}
.bubble.failed { border-color: var(--c-brand-line); background: #FFF7F7; }

.cursor {
  display: inline-block;
  width: 6px; height: 15px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--c-brand);
  border-radius: 1px;
  animation: blink 1s steps(2, start) infinite;
}
@keyframes blink { 50% { opacity: 0; } }

.msg-foot { display: flex; align-items: center; gap: 8px; padding: 0 2px; }
.msg-time { font-size: 11px; color: var(--c-text-3); }
.foot-btn {
  border: none; background: none; padding: 0;
  font-size: 11px; color: var(--c-text-3);
  font-family: inherit; cursor: pointer;
}
.foot-btn:hover { color: var(--c-brand); }
.foot-btn.danger { color: var(--c-brand); }

/* 底部输入 */
.chat-foot {
  flex-shrink: 0;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom, 0px));
  background: #fff;
  border-top: 1px solid var(--c-line);
}
.foot-row { display: flex; align-items: flex-end; gap: 8px; }
.input {
  flex: 1;
  resize: none;
  border: none;
  outline: none;
  background: #F4F5F7;
  border-radius: 14px;
  padding: 11px 14px;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
  max-height: 108px;
  color: var(--c-text);
}
.input::placeholder { color: var(--c-text-3); }
.send-btn {
  flex-shrink: 0;
  border: none;
  border-radius: 14px;
  padding: 11px 18px;
  font-size: 14px; font-family: inherit;
  color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 6px 16px rgba(224, 30, 38, .28);
}
.send-btn:disabled { opacity: .45; box-shadow: none; cursor: not-allowed; }
.send-btn.stop { background: #2C3138; box-shadow: none; }
.foot-tip {
  margin: 7px 0 0;
  text-align: center;
  font-size: 11px;
  color: var(--c-text-3);
}
</style>