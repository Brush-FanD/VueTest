<template>
  <div class="comment" :class="{ nested: depth > 0 }">
    <div class="avatar" :style="{ background: avatarBg }">{{ avatarText }}</div>
    <div class="main">
      <div class="head">
        <span class="name">{{ comment.commenter || '匿名用户' }}</span>
        <span v-if="mine" class="me">我</span>
        <span class="time">{{ timeAgo(comment.createTime) }}</span>
      </div>

      <p class="text">{{ comment.comment }}</p>

      <div class="ops">
        <button class="op" :class="{ on: liked }" type="button" @click="toggleLike">
          {{ liked ? '❤️ 已赞' : '🤍 点赞' }}
        </button>
        <button v-if="depth === 0" class="op" type="button" @click="emit('reply', comment)">回复</button>
        <button v-if="mine" class="op danger" type="button" @click="emit('delete', comment)">删除</button>
      </div>

      <div v-if="comment.showReply" class="reply-box">
        <input
          v-model="comment.replyContent"
          class="reply-input"
          :placeholder="`回复 @${comment.commenter || '匿名用户'}`"
          @keyup.enter="submitReply"
        />
        <button class="reply-send" type="button" :disabled="!comment.replyContent?.trim()" @click="submitReply">发送</button>
      </div>

      <div v-if="comment.replies?.length" class="replies">
        <CommentItem
          v-for="child in comment.replies"
          :key="child.id"
          :comment="child"
          :depth="depth + 1"
          @reply="emit('reply', $event)"
          @delete="emit('delete', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { timeAgo } from '../utils/format'
import { likeStore } from '../utils/storage'
import { localNickname } from '../utils/nickname'

const props = defineProps({
  comment: { type: Object, required: true },
  depth: { type: Number, default: 0 }
})

const emit = defineEmits(['reply', 'delete'])

/** 点赞只保存在本机，不做无后端支撑的假计数 */
const liked = ref(likeStore.has(props.comment.id))
const toggleLike = () => {
  liked.value = likeStore.toggle(props.comment.id)
}

const nickname = localNickname()
const mine = computed(() => (props.comment.commenter || '') === nickname)

const palette = ['#F97316', '#0EA5E9', '#8B5CF6', '#10B981', '#EF4444', '#F59E0B', '#3B82F6']
const avatarText = computed(() => (props.comment.commenter || '匿').slice(0, 1))
const avatarBg = computed(() => {
  const source = props.comment.commenter || '匿名用户'
  let hash = 0
  for (const ch of source) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  return palette[hash % palette.length]
})

const submitReply = () => {
  if (!props.comment.replyContent?.trim()) return
  emit('reply', props.comment)
}
</script>

<style scoped>
.comment { display: flex; gap: 10px; }
.comment.nested { margin-top: 12px; }

.avatar {
  flex-shrink: 0;
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 13px; font-weight: 600;
}
.comment.nested .avatar { width: 26px; height: 26px; font-size: 11px; }

.main { flex: 1; min-width: 0; }
.head { display: flex; align-items: center; gap: 6px; }
.name { font-size: 13px; font-weight: 600; color: var(--c-text); }
.me {
  font-size: 10px; color: var(--c-brand);
  background: var(--c-brand-soft); border-radius: 4px; padding: 1px 5px;
}
.time { margin-left: auto; font-size: 11px; color: var(--c-text-3); }

.text {
  margin-top: 4px;
  font-size: 14px; line-height: 1.6; color: var(--c-text);
  word-break: break-word; white-space: pre-wrap;
}

.ops { display: flex; gap: 14px; margin-top: 6px; }
.op {
  border: none; background: none; padding: 0;
  font-size: 12px; color: var(--c-text-3);
  font-family: inherit; cursor: pointer;
}
.op.on { color: var(--c-brand); }
.op.danger:hover { color: var(--c-brand); }

.reply-box { display: flex; gap: 8px; margin-top: 8px; }
.reply-input {
  flex: 1; min-width: 0;
  border: none; outline: none;
  background: #F4F5F7; border-radius: 10px;
  padding: 8px 12px; font-size: 13px; font-family: inherit;
  color: var(--c-text);
}
.reply-send {
  flex-shrink: 0;
  border: none; border-radius: 10px; padding: 8px 14px;
  font-size: 13px; font-family: inherit; color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
}
.reply-send:disabled { opacity: .45; cursor: not-allowed; }

.replies {
  margin-top: 10px; padding-left: 10px;
  border-left: 2px solid var(--c-line);
}
</style>