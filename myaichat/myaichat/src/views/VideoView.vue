<template>
  <div class="detail">
    <header class="bar">
      <button class="back" type="button" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m15 5-7 7 7 7" />
        </svg>
      </button>
      <span class="bar-title ellipsis">{{ videoTitle || '视频详情' }}</span>
      <span class="bar-holder"></span>
    </header>

    <div class="player-wrap">
      <video
        v-if="videoUrl && !failed"
        ref="player"
        class="player"
        :src="videoUrl"
        :poster="posterUrl || undefined"
        controls
        playsinline
        preload="metadata"
        @loadedmetadata="onLoaded"
        @timeupdate="onTimeUpdate"
        @error="failed = true"
      ></video>

      <div v-else class="player-fallback">
        <div v-if="loading" class="loading-box">
          <span class="spinner"></span>
          <p>加载中…</p>
        </div>
        <div v-else class="error-box">
          <span class="emoji">🎬</span>
          <p>{{ failed ? '视频无法播放，可能是文件已丢失或格式不受支持' : '视频不存在或已被删除' }}</p>
          <button class="btn primary sm" type="button" @click="goBack">返回视频库</button>
        </div>
      </div>
    </div>

    <div v-if="videoUrl && !loading" class="meta">
      <h1 class="title">{{ videoTitle }}</h1>
      <div class="meta-row">
        <span class="chip">懂车帝用户上传</span>
        <span class="meta-time">{{ timeAgo(createTime) }}</span>
      </div>
    </div>

    <div class="actions">
      <button class="action" :class="{ on: favorited }" type="button" @click="toggleFavorite">
        <span class="action-icon">{{ favorited ? '⭐' : '☆' }}</span>
        <span>{{ favorited ? '已收藏' : '收藏' }}</span>
      </button>
      <button class="action" :class="{ on: liked }" type="button" @click="toggleLike">
        <span class="action-icon">{{ liked ? '❤️' : '🤍' }}</span>
        <span>{{ liked ? '已赞' : '点赞' }}</span>
      </button>
      <button class="action" type="button" @click="share">
        <span class="action-icon">🔗</span>
        <span>分享</span>
      </button>
      <button class="action" type="button" @click="scrollToComments">
        <span class="action-icon">💬</span>
        <span>{{ comments.length }}</span>
      </button>
    </div>

    <section class="comments" ref="commentSection">
      <div class="section-title">
        <span class="bar"></span>
        <span>评论</span>
        <span class="more">{{ comments.length }} 条</span>
      </div>

      <div class="editor">
        <input
          v-model="draft"
          class="editor-input"
          :placeholder="`以「${nickname}」的身份发表看法…`"
          :disabled="submitting"
          @keyup.enter="submitComment"
        />
        <button class="editor-send" type="button" :disabled="submitting || !draft.trim()" @click="submitComment">
          {{ submitting ? '发送中' : '发送' }}
        </button>
      </div>

      <div v-if="comments.length" class="list">
        <CommentItem
          v-for="item in comments"
          :key="item.id"
          :comment="item"
          @reply="handleReply"
          @delete="handleDelete"
        />
      </div>
      <div v-else class="empty">
        <span class="emoji">💬</span>
        <p class="tip">还没有评论，来说两句</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommentItem from '../components/CommentItem.vue'
import { addComment as createComment, listComments, removeComment } from '../api/comment'
import { getVideo } from '../api/video'
import { mediaUrl } from '../utils/media'
import { timeAgo } from '../utils/format'
import { favoriteStore, likeStore, progressStore } from '../utils/storage'
import { localNickname } from '../utils/nickname'

const route = useRoute()
const router = useRouter()

const videoId = route.params.videoId
const player = ref(null)
const commentSection = ref(null)

const videoTitle = ref('')
const videoUrl = ref('')
const posterUrl = ref('')
const createTime = ref('')
const loading = ref(true)
const failed = ref(false)

const comments = ref([])
const draft = ref('')
const submitting = ref(false)
const nickname = localNickname()

const favorited = ref(favoriteStore.has(videoId))
const liked = ref(likeStore.has(videoId))

const toggleFavorite = () => {
  favorited.value = favoriteStore.toggle(videoId)
  ElMessage.success(favorited.value ? '已加入收藏' : '已取消收藏')
}

const toggleLike = () => {
  liked.value = likeStore.toggle(videoId)
}

const share = async () => {
  const link = window.location.href
  try {
    if (navigator.share) {
      await navigator.share({ title: videoTitle.value, url: link })
      return
    }
    await navigator.clipboard.writeText(link)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.info(link)
  }
}

const scrollToComments = () => {
  commentSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/videos')
}

/** 把后端的评论树补上前端需要的交互状态 */
const initComments = (list) =>
  (list || []).map((comment) => ({
    ...comment,
    showReply: false,
    replyContent: '',
    replies: comment.childComments?.length ? initComments(comment.childComments) : []
  }))

const fetchVideo = async () => {
  loading.value = true
  try {
    const video = await getVideo(videoId)
    videoTitle.value = video.videoName || '视频详情'
    videoUrl.value = mediaUrl(video.videoPath)
    posterUrl.value = video.image ? mediaUrl(video.image) : ''
    createTime.value = video.createTime
  } catch {
    videoUrl.value = ''
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    comments.value = initComments(await listComments(videoId))
  } catch {
    // 视频不存在时评论接口同样会失败，不重复提示
  }
}

onMounted(() => {
  fetchVideo()
  fetchComments()
})

/** 记住播放进度，下次进来接着看 */
const onLoaded = () => {
  const saved = progressStore.get(videoId)
  const duration = player.value?.duration || 0
  if (saved > 3 && duration && saved < duration - 5) {
    player.value.currentTime = saved
  }
}

let lastSaved = 0
const onTimeUpdate = () => {
  const current = player.value?.currentTime || 0
  // 每 5 秒写一次，避免高频写 localStorage
  if (current - lastSaved >= 5) {
    lastSaved = current
    progressStore.set(videoId, current)
  }
}

const submitComment = async () => {
  const content = draft.value.trim()
  if (!content || submitting.value) return
  submitting.value = true
  try {
    const created = await createComment({ videoId, comment: content, commenter: nickname })
    comments.value.unshift(initComments([created])[0])
    draft.value = ''
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

const handleReply = async (comment) => {
  const content = comment.replyContent?.trim()
  if (!content) return
  try {
    const created = await createComment({
      videoId,
      parentCommentId: comment.id,
      comment: content,
      commenter: nickname
    })
    comment.replies.unshift(initComments([created])[0])
    comment.showReply = false
    comment.replyContent = ''
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

/** 递归删除：回复是嵌套的，只过滤顶层会漏掉子评论 */
const removeFromTree = (list, commentId) => {
  const index = list.findIndex((item) => item.id === commentId)
  if (index !== -1) {
    list.splice(index, 1)
    return true
  }
  return list.some((item) => item.replies?.length && removeFromTree(item.replies, commentId))
}

const handleDelete = (comment) => {
  ElMessageBox.confirm('确定删除这条评论？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await removeComment(comment.id)
        removeFromTree(comments.value, comment.id)
        ElMessage.success('已删除')
      } catch {
        // 错误提示已由请求拦截器统一处理
      }
    })
    .catch(() => {})
}
</script>

<style scoped>
.detail { padding-bottom: 16px; }

.bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px;
  background: #fff; border-bottom: 1px solid var(--c-line);
  position: sticky; top: 0; z-index: 10;
}
.back {
  width: 32px; height: 32px; border-radius: 10px;
  border: none; background: #F4F5F7; color: var(--c-text);
  display: flex; align-items: center; justify-content: center; cursor: pointer;
}
.back svg { width: 18px; height: 18px; }
.bar-title { flex: 1; text-align: center; font-size: 15px; font-weight: 600; }
.bar-holder { width: 32px; }

.player-wrap { background: #000; position: relative; }
.player { width: 100%; aspect-ratio: 16 / 9; display: block; background: #000; }
.player-fallback {
  width: 100%; aspect-ratio: 16 / 9;
  display: flex; align-items: center; justify-content: center;
  background: #14161A; color: rgba(255, 255, 255, .72);
}
.loading-box, .error-box {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 20px; text-align: center;
}
.error-box .emoji { font-size: 34px; }
.loading-box p, .error-box p { font-size: 13px; margin: 0; }
.spinner {
  width: 22px; height: 22px; border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, .25);
  border-top-color: #fff;
  animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.meta { background: #fff; padding: 13px 14px 12px; }
.title { font-size: 16px; font-weight: 700; line-height: 1.45; }
.meta-row { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.meta-time { font-size: 12px; color: var(--c-text-3); }

.actions {
  display: flex; gap: 8px;
  padding: 10px 12px;
  background: #fff; border-top: 1px solid var(--c-line);
}
.action {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: none; background: none; cursor: pointer;
  font-size: 11px; color: var(--c-text-2); font-family: inherit;
}
.action-icon { font-size: 18px; line-height: 1; }
.action.on { color: var(--c-brand); }

.comments { margin: 12px; padding: 14px; background: #fff; border-radius: var(--r-lg); box-shadow: var(--sh-1); }
.section-title { margin-top: 0; }

.editor { display: flex; gap: 8px; margin-bottom: 16px; }
.editor-input {
  flex: 1; min-width: 0;
  border: none; outline: none;
  background: #F4F5F7; border-radius: 12px;
  padding: 10px 13px; font-size: 14px; font-family: inherit;
  color: var(--c-text);
}
.editor-send {
  flex-shrink: 0;
  border: none; border-radius: 12px; padding: 10px 16px;
  font-size: 14px; font-family: inherit; color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 6px 16px rgba(224, 30, 38, .25);
}
.editor-send:disabled { opacity: .45; box-shadow: none; cursor: not-allowed; }

.list { display: flex; flex-direction: column; gap: 16px; }
</style>