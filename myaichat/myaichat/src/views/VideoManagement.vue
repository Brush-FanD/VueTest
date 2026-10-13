<template>
  <div class="videos">
    <header class="hero">
      <div class="hero-top">
        <div class="brand">
          <span class="brand-logo">视频库</span>
          <span class="brand-sub">{{ videos.length }} 个视频</span>
        </div>
        <button class="upload-btn" type="button" @click="openAdd">＋ 上传</button>
      </div>
      <div class="search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="7" /><path d="m20 20-3.6-3.6" />
        </svg>
        <input v-model="keyword" placeholder="搜索视频名称" />
        <button v-if="keyword" class="clear" type="button" @click="keyword = ''">✕</button>
      </div>
    </header>

    <div class="page">
      <div v-if="loading" class="list">
        <div v-for="i in 3" :key="i" class="item">
          <div class="thumb skeleton"></div>
          <div class="info">
            <div class="skeleton line"></div>
            <div class="skeleton line short"></div>
          </div>
        </div>
      </div>

      <div v-else-if="filtered.length" class="list">
        <article
          v-for="(item, index) in filtered"
          :key="item.id"
          class="item rise"
          :style="{ animationDelay: `${Math.min(index, 8) * 40}ms` }"
          @click="play(item)"
        >
          <div class="thumb">
            <img v-if="item.image" :src="mediaUrl(item.image)" :alt="item.videoName" loading="lazy" @error="hideImage" />
            <div v-else class="thumb-fallback">🎬</div>
            <span class="play">▶</span>
          </div>
          <div class="info">
            <h3 class="title clamp-2">{{ item.videoName }}</h3>
            <span class="time">{{ timeAgo(item.createTime) }}</span>
          </div>
          <div v-if="item.mine" class="ops" @click.stop>
            <button class="op" type="button" title="编辑" @click="openEdit(item)">编辑</button>
            <button class="op danger" type="button" title="删除" @click="handleDelete(item)">删除</button>
          </div>
        </article>
      </div>

      <div v-else-if="keyword" class="empty">
        <span class="emoji">🔍</span>
        <p class="tip">没有找到「{{ keyword }}」相关的视频</p>
      </div>

      <div v-else class="empty">
        <span class="emoji">🎬</span>
        <p class="tip">视频库还是空的</p>
        <button class="btn primary sm" type="button" @click="openAdd">上传第一个视频</button>
      </div>
    </div>

    <!-- 上传 / 编辑共用一个表单，避免两份几乎一样的代码 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑视频' : '上传视频'" width="92%" top="6vh">
      <el-form label-width="62px" label-position="left">
        <el-form-item label="名称">
          <el-input v-model="form.videoName" maxlength="100" placeholder="给视频起个名字" />
        </el-form-item>

        <el-form-item label="视频">
          <div class="picker" :class="{ filled: !!form.videoFile }">
            <input ref="videoInput" type="file" accept="video/*" class="hidden-input" @change="onVideoPicked" />
            <div v-if="form.videoFile" class="picked">
              <span class="picked-name ellipsis">{{ form.videoFile.name }}</span>
              <span class="picked-meta">{{ formatSize(form.videoFile.size) }}<template v-if="pickedDuration"> · {{ pickedDuration }}</template></span>
              <button class="btn sm" type="button" @click="pickVideo">重选</button>
            </div>
            <div v-else class="picker-empty" @click="pickVideo">
              <span class="picker-icon">⬆️</span>
              <span class="picker-tip">{{ editingId ? '不选则保留原视频' : '点击选择视频（mp4 / mov / webm 等）' }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="封面">
          <div class="picker" :class="{ filled: !!form.imageFile }">
            <input ref="imageInput" type="file" accept="image/*" class="hidden-input" @change="onImagePicked" />
            <div v-if="coverPreview" class="picked">
              <img :src="coverPreview" class="cover-preview" alt="封面预览" />
              <span class="picked-name ellipsis">{{ form.imageFile ? form.imageFile.name : '当前封面' }}</span>
              <button class="btn sm" type="button" @click="pickImage">换一张</button>
            </div>
            <div v-else class="picker-empty" @click="pickImage">
              <span class="picker-icon">🖼️</span>
              <span class="picker-tip">点击选择封面图（可选，建议 16:10）</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item v-if="submitLoading" label="进度">
          <el-progress :percentage="uploadPercent" :stroke-width="10" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitLoading" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listVideos, removeVideo, updateVideo, uploadVideo } from '../api/video'
import { useAuth } from '../composables/useAuth'
import { mediaUrl } from '../utils/media'
import { formatDuration, formatSize, timeAgo } from '../utils/format'

const router = useRouter()
const { isLoggedIn } = useAuth()

/** 未登录时提示并跳去登录页，返回 false 让调用方短路 */
const requireLogin = (redirect = '/videos') => {
  if (isLoggedIn.value) return true
  ElMessage.warning('请先登录')
  router.push({ name: 'Account', query: { redirect } })
  return false
}

const videos = ref([])
const loading = ref(true)
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref(null)
const submitLoading = ref(false)
const uploadPercent = ref(0)
const coverPreview = ref('')
const pickedDuration = ref('')

const videoInput = ref(null)
const imageInput = ref(null)

const form = reactive({ videoName: '', videoFile: null, imageFile: null })

const filtered = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  if (!word) return videos.value
  return videos.value.filter((item) => (item.videoName || '').toLowerCase().includes(word))
})

const hideImage = (event) => {
  event.target.style.display = 'none'
}

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

/** 点击卡片进播放页：这是之前缺掉的入口 */
const play = (item) => {
  router.push({ name: 'VideoView', params: { videoId: item.id } })
}

const resetForm = () => {
  form.videoName = ''
  form.videoFile = null
  form.imageFile = null
  coverPreview.value = ''
  pickedDuration.value = ''
  uploadPercent.value = 0
}

const openAdd = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (item) => {
  editingId.value = item.id
  resetForm()
  form.videoName = item.videoName
  coverPreview.value = item.image ? mediaUrl(item.image) : ''
  dialogVisible.value = true
}

const pickVideo = () => videoInput.value?.click()
const pickImage = () => imageInput.value?.click()

/** 本地读一下视频时长，纯粹为了让用户确认选对了文件，不发给后端 */
const readDuration = (file) => {
  pickedDuration.value = ''
  const url = URL.createObjectURL(file)
  const probe = document.createElement('video')
  probe.preload = 'metadata'
  probe.onloadedmetadata = () => {
    pickedDuration.value = formatDuration(probe.duration)
    URL.revokeObjectURL(url)
  }
  probe.onerror = () => URL.revokeObjectURL(url)
  probe.src = url
}

const onVideoPicked = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  form.videoFile = file
  readDuration(file)
  if (!form.videoName) form.videoName = file.name.replace(/\.[^.]+$/, '')
  event.target.value = ''
}

const onImagePicked = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  form.imageFile = file
  coverPreview.value = URL.createObjectURL(file)
  event.target.value = ''
}

const submit = async () => {
  if (!form.videoName.trim()) {
    ElMessage.warning('请输入视频名称')
    return
  }
  if (!editingId.value && !form.videoFile) {
    ElMessage.warning('请选择要上传的视频文件')
    return
  }
  submitLoading.value = true
  uploadPercent.value = 0
  const onProgress = (percent) => { uploadPercent.value = percent }
  try {
    if (editingId.value) {
      await updateVideo(editingId.value, { ...form }, onProgress)
      ElMessage.success('修改成功')
    } else {
      await uploadVideo({ ...form }, onProgress)
      ElMessage.success('上传成功')
    }
    dialogVisible.value = false
    await fetchVideos()
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (item) => {
  ElMessageBox.confirm(`确定删除「${item.videoName}」？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await removeVideo(item.id)
        ElMessage.success('删除成功')
        await fetchVideos()
      } catch {
        // 错误提示已由请求拦截器统一处理
      }
    })
    .catch(() => {})
}
</script>

<style scoped>
.videos { padding-bottom: 16px; }

.hero { border-radius: 0 0 20px 20px; }
.hero-top { display: flex; align-items: center; position: relative; z-index: 1; }
.brand { display: flex; align-items: baseline; gap: 8px; }
.brand-logo { font-size: 19px; font-weight: 800; }
.brand-sub { font-size: 11px; color: rgba(255, 255, 255, .58); }
.upload-btn {
  margin-left: auto;
  border: none; cursor: pointer; font-family: inherit;
  font-size: 13px; font-weight: 600; color: #fff;
  padding: 8px 16px; border-radius: 999px;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 6px 16px rgba(224, 30, 38, .38);
}
.upload-btn:active { transform: scale(.96); }

.search {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 8px;
  margin-top: 14px;
  background: rgba(255, 255, 255, .13);
  border: 1px solid rgba(255, 255, 255, .16);
  border-radius: 999px; padding: 9px 14px;
}
.search svg { width: 16px; height: 16px; color: rgba(255, 255, 255, .7); flex-shrink: 0; }
.search input {
  flex: 1; min-width: 0; border: none; outline: none;
  background: transparent; color: #fff; font-size: 14px; font-family: inherit;
}
.search input::placeholder { color: rgba(255, 255, 255, .5); }
.clear { border: none; background: none; color: rgba(255, 255, 255, .7); cursor: pointer; font-size: 12px; }

.list { display: flex; flex-direction: column; gap: 10px; }
.item {
  display: flex; align-items: center; gap: 11px;
  background: #fff; border-radius: var(--r-lg);
  padding: 10px; box-shadow: var(--sh-1);
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}
.item:active { transform: scale(.985); box-shadow: var(--sh-2); }
.thumb {
  position: relative; flex-shrink: 0;
  width: 104px; height: 66px; border-radius: 10px;
  overflow: hidden; background: #E8EAEE;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb-fallback {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; background: linear-gradient(135deg, #E8EAEE, #DDE1E7);
}
.play {
  position: absolute; inset: 0; margin: auto;
  width: 26px; height: 26px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; color: #fff;
  background: rgba(0, 0, 0, .42);
  backdrop-filter: blur(3px);
}
.info { flex: 1; min-width: 0; }
.title { font-size: 14px; font-weight: 600; line-height: 1.42; }
.time { display: block; margin-top: 5px; font-size: 11px; color: var(--c-text-3); }
.ops { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
.op {
  border: none; background: #F4F5F7; color: var(--c-text-2);
  font-size: 12px; font-family: inherit;
  padding: 5px 10px; border-radius: 999px; cursor: pointer;
}
.op.danger { color: var(--c-brand); background: var(--c-brand-soft); }
.op:active { transform: scale(.95); }

.line { height: 11px; }
.line.short { width: 50%; margin-top: 8px; }

/* 上传表单 */
.picker {
  width: 100%;
  border: 1px dashed #D6DAE1;
  border-radius: var(--r-md);
  padding: 12px;
  background: #FAFBFC;
}
.picker.filled { border-style: solid; border-color: var(--c-line); background: #fff; }
.hidden-input { display: none; }
.picker-empty {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 8px 0; cursor: pointer; color: var(--c-text-3);
}
.picker-icon { font-size: 22px; }
.picker-tip { font-size: 12px; text-align: center; }
.picked { display: flex; align-items: center; gap: 10px; }
.picked-name { flex: 1; min-width: 0; font-size: 13px; }
.picked-meta { font-size: 11px; color: var(--c-text-3); flex-shrink: 0; }
.cover-preview {
  width: 62px; height: 40px; border-radius: 6px; object-fit: cover; flex-shrink: 0;
}
</style>