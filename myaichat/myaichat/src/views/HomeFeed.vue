<template>
  <div class="home">
    <header class="hero">
      <div class="hero-top">
        <div class="brand">
          <span class="brand-logo">懂车帝</span>
          <span class="brand-sub">AI 汽车助手</span>
        </div>
        <button class="ai-pill" type="button" @click="openAi()">问 AI</button>
      </div>

      <div class="search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="7" /><path d="m20 20-3.6-3.6" />
        </svg>
        <input v-model="keyword" placeholder="搜索视频名称" />
        <button v-if="keyword" class="clear" type="button" @click="keyword = ''">✕</button>
      </div>

      <div class="quick">
        <button v-for="item in quickEntries" :key="item.label" class="quick-item" type="button" @click="item.action">
          <span class="quick-icon" :style="{ background: item.bg }">{{ item.icon }}</span>
          <span class="quick-label">{{ item.label }}</span>
        </button>
      </div>
    </header>

    <div class="page">
      <div v-if="history.length" class="history">
        <span class="history-label">最近搜索</span>
        <button v-for="word in history" :key="word" class="chip" type="button" @click="applyKeyword(word)">{{ word }}</button>
        <button class="history-clear" type="button" @click="clearHistory">清空</button>
      </div>

      <div class="section-title">
        <span class="bar"></span>
        <span>最新视频</span>
        <span class="more">{{ loading ? '加载中…' : `共 ${filtered.length} 个` }}</span>
      </div>

      <div v-if="loading" class="grid">
        <div v-for="i in 4" :key="i" class="vcard">
          <div class="vthumb skeleton"></div>
          <div class="vbody">
            <div class="skeleton line"></div>
            <div class="skeleton line short"></div>
          </div>
        </div>
      </div>

      <div v-else-if="filtered.length" class="grid">
        <article
          v-for="(item, index) in filtered"
          :key="item.id"
          class="vcard rise"
          :style="{ animationDelay: `${Math.min(index, 8) * 40}ms` }"
          @click="openVideo(item)"
        >
          <div class="vthumb">
            <img v-if="item.image" :src="mediaUrl(item.image)" :alt="item.videoName" loading="lazy" @error="hideImage" />
            <div v-else class="vthumb-fallback">🎬</div>
            <span class="vplay">▶</span>
          </div>
          <div class="vbody">
            <h3 class="vtitle clamp-2">{{ item.videoName }}</h3>
            <span class="vtime">{{ timeAgo(item.createTime) }}</span>
          </div>
        </article>
      </div>

      <div v-else-if="keyword" class="empty">
        <span class="emoji">🔍</span>
        <p class="tip">没有找到「{{ keyword }}」相关的视频</p>
        <button class="btn primary sm" type="button" @click="askAi(keyword)">让懂车AI回答这个关键词</button>
      </div>

      <div v-else class="empty">
        <span class="emoji">🎬</span>
        <p class="tip">还没有视频，上传第一个吧</p>
        <button class="btn primary sm" type="button" @click="router.push('/videos')">去上传</button>
      </div>
    </div>

    <!-- 首页 AI 弹层：与 AI 页面共用 useChat -->
    <div v-if="showAi" class="ai-mask" @click.self="closeAi">
      <section class="ai-sheet">
        <header class="sheet-head">
          <span class="sheet-title">懂车AI</span>
          <div class="sheet-actions">
            <button type="button" @click="router.push('/ai')">全屏</button>
            <button type="button" @click="closeAi">✕</button>
          </div>
        </header>

        <div class="sheet-body" ref="aiBody">
          <div v-if="!messages.length" class="sheet-empty">
            <p class="tip">想了解什么车型？问我任何汽车问题</p>
          </div>
          <div v-for="msg in messages" :key="msg.id" class="sheet-msg" :class="msg.role">
            <div class="sheet-bubble">
              <span>{{ msg.content }}</span>
              <span v-if="msg.streaming" class="cursor"></span>
            </div>
          </div>
        </div>

        <footer class="sheet-foot">
          <input
            v-model="input"
            class="sheet-input"
            placeholder="输入汽车问题…"
            @keyup.enter="send"
          />
          <button v-if="aiLoading" class="btn sm" type="button" @click="stop">停止</button>
          <button v-else class="btn primary sm" type="button" :disabled="!input.trim()" @click="send">发送</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listVideos } from '../api/video'
import { mediaUrl } from '../utils/media'
import { timeAgo } from '../utils/format'
import { historyStore } from '../utils/storage'
import { useChat } from '../composables/useChat'

const router = useRouter()
const keyword = ref('')
const loading = ref(true)
const videos = ref([])
const history = ref(historyStore.all())

const showAi = ref(false)
const aiBody = ref(null)
const { messages, input, loading: aiLoading, send, sendText, stop } = useChat(aiBody)

const quickEntries = [
  { label: '选车', icon: '🚗', bg: 'linear-gradient(135deg,#FFE7E8,#FFD3D5)', action: () => router.push('/cars') },
  { label: '视频库', icon: '🎬', bg: 'linear-gradient(135deg,#E6F0FF,#D6E6FF)', action: () => router.push('/videos') },
  { label: '上传', icon: '⬆️', bg: 'linear-gradient(135deg,#E8F7EF,#D6F0E3)', action: () => router.push('/videos') },
  { label: '懂车AI', icon: '🤖', bg: 'linear-gradient(135deg,#F0E9FF,#E2D8FF)', action: () => router.push('/ai') }
]

const filtered = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  if (!word) return videos.value
  return videos.value.filter((item) => (item.videoName || '').toLowerCase().includes(word))
})

const fetchVideos = async () => {
  loading.value = true
  try {
    const data = await listVideos()
    videos.value = Array.isArray(data) ? data : (data?.list || [])
  } catch {
    videos.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchVideos)

/** 封面加载失败时退回占位图，避免出现浏览器默认的碎图 */
const hideImage = (event) => {
  event.target.style.display = 'none'
}

const openVideo = (item) => {
  router.push({ name: 'VideoView', params: { videoId: item.id } })
}

const applyKeyword = (word) => {
  keyword.value = word
}

const clearHistory = () => {
  historyStore.clear()
  history.value = []
}

const askAi = (question) => {
  const text = String(question || '').trim()
  if (!text) return
  const word = text
  historyStore.push(word)
  history.value = historyStore.all()
  keyword.value = ''
  showAi.value = true
  sendText(word)
}

const openAi = () => {
  showAi.value = true
}

const closeAi = () => {
  showAi.value = false
}
</script>

<style scoped>
.home { padding-bottom: 16px; }

/* 顶部 */
.hero {
  background: linear-gradient(135deg, #1F2329 0%, #2C3138 58%, #3A2226 100%);
  color: #fff;
  padding: 16px 14px 18px;
  border-radius: 0 0 20px 20px;
  position: relative;
  overflow: hidden;
}
.hero::after {
  content: ''; position: absolute; right: -50px; top: -60px;
  width: 180px; height: 180px; border-radius: 50%;
  background: radial-gradient(circle, rgba(224, 30, 38, .5), transparent 70%);
}
.hero-top { display: flex; align-items: center; gap: 10px; position: relative; z-index: 1; }
.brand { display: flex; align-items: baseline; gap: 8px; }
.brand-logo { font-size: 19px; font-weight: 800; letter-spacing: .5px; }
.brand-sub { font-size: 11px; color: rgba(255, 255, 255, .58); }
.ai-pill {
  margin-left: auto;
  border: 1px solid rgba(255, 255, 255, .24);
  background: rgba(255, 255, 255, .1);
  color: #fff; font-family: inherit; font-size: 12px;
  padding: 6px 14px; border-radius: 999px; cursor: pointer;
}
.ai-pill:active { background: rgba(255, 255, 255, .2); }

.search {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 8px;
  margin-top: 14px;
  background: rgba(255, 255, 255, .13);
  border: 1px solid rgba(255, 255, 255, .16);
  border-radius: 999px;
  padding: 9px 14px;
  backdrop-filter: blur(6px);
}
.search svg { width: 16px; height: 16px; color: rgba(255, 255, 255, .7); flex-shrink: 0; }
.search input {
  flex: 1; min-width: 0;
  border: none; outline: none; background: transparent;
  color: #fff; font-size: 14px; font-family: inherit;
}
.search input::placeholder { color: rgba(255, 255, 255, .5); }
.clear { border: none; background: none; color: rgba(255, 255, 255, .7); cursor: pointer; font-size: 12px; }

.quick {
  position: relative; z-index: 1;
  display: flex; gap: 8px; margin-top: 16px;
}
.quick-item {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  border: none; background: none; padding: 0; cursor: pointer;
  color: #fff; font-family: inherit;
}
.quick-icon {
  width: 42px; height: 42px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
  box-shadow: 0 6px 14px rgba(0, 0, 0, .18);
  transition: transform .15s ease;
}
.quick-item:active .quick-icon { transform: scale(.93); }
.quick-label { font-size: 11px; color: rgba(255, 255, 255, .82); }

/* 搜索历史 */
.history {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  margin-bottom: 4px;
}
.history-label { font-size: 12px; color: var(--c-text-3); margin-right: 2px; }
.history-clear {
  margin-left: auto; border: none; background: none;
  font-size: 12px; color: var(--c-text-3); font-family: inherit; cursor: pointer;
}

/* 视频网格 */
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.vcard {
  background: #fff; border-radius: var(--r-lg); overflow: hidden;
  box-shadow: var(--sh-1); cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}
.vcard:active { transform: scale(.975); box-shadow: var(--sh-2); }
.vthumb {
  position: relative; aspect-ratio: 16 / 10; background: #E8EAEE;
  overflow: hidden;
}
.vthumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.vthumb-fallback {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; background: linear-gradient(135deg, #E8EAEE, #DDE1E7);
}
.vplay {
  position: absolute; right: 8px; bottom: 8px;
  width: 24px; height: 24px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; color: #fff;
  background: rgba(0, 0, 0, .45);
  backdrop-filter: blur(4px);
}
.vbody { padding: 9px 10px 11px; }
.vtitle { font-size: 13px; font-weight: 600; line-height: 1.42; min-height: 37px; }
.vtime { display: block; margin-top: 5px; font-size: 11px; color: var(--c-text-3); }
.skeleton { width: 100%; height: 100%; }
.line { height: 12px; margin: 0; border-radius: 6px; }
.vbody .line { height: 11px; }
.vbody .line.short { width: 55%; margin-top: 8px; }

/* AI 弹层 */
.ai-mask {
  position: fixed; inset: 0; z-index: 60;
  background: rgba(16, 24, 40, .45);
  display: flex; align-items: flex-end; justify-content: center;
  backdrop-filter: blur(2px);
}
.ai-sheet {
  width: 100%; max-width: 480px;
  height: 78vh;
  background: var(--c-bg);
  border-radius: 20px 20px 0 0;
  display: flex; flex-direction: column;
  overflow: hidden;
  animation: sheet-up .26s ease;
}
@keyframes sheet-up { from { transform: translateY(24px); opacity: .6; } to { transform: none; opacity: 1; } }
.sheet-head {
  display: flex; align-items: center;
  padding: 13px 16px;
  background: #fff; border-bottom: 1px solid var(--c-line);
}
.sheet-title { font-size: 15px; font-weight: 700; }
.sheet-actions { margin-left: auto; display: flex; gap: 14px; }
.sheet-actions button {
  border: none; background: none; font-family: inherit;
  font-size: 13px; color: var(--c-text-2); cursor: pointer;
}
.sheet-body { flex: 1; min-height: 0; overflow-y: auto; padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.sheet-empty { padding: 30px 0; text-align: center; }
.sheet-msg { display: flex; }
.sheet-msg.user { justify-content: flex-end; }
.sheet-bubble {
  max-width: 84%;
  padding: 9px 12px; border-radius: 13px;
  font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.sheet-msg.assistant .sheet-bubble { background: #fff; border: 1px solid var(--c-line); border-top-left-radius: 4px; }
.sheet-msg.user .sheet-bubble { background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2)); color: #fff; border-top-right-radius: 4px; }
.cursor {
  display: inline-block; width: 6px; height: 14px; margin-left: 2px;
  background: var(--c-brand); border-radius: 1px; vertical-align: -2px;
  animation: blink 1s steps(2, start) infinite;
}
@keyframes blink { 50% { opacity: 0; } }
.sheet-foot {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px calc(12px + env(safe-area-inset-bottom, 0px));
  background: #fff; border-top: 1px solid var(--c-line);
}
.sheet-input {
  flex: 1; min-width: 0;
  border: none; outline: none; background: #F4F5F7;
  border-radius: 12px; padding: 10px 13px;
  font-size: 14px; font-family: inherit; color: var(--c-text);
}
</style>