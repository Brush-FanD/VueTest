<template>
  <div class="kb">
    <header class="hero">
      <div class="brand">
        <span class="brand-logo">知识库</span>
        <span class="brand-sub">{{ documents.length }} 份资料 · {{ chunkTotal }} 个片段</span>
      </div>
      <button class="ghost" type="button" @click="toggleAdd">{{ showAdd ? '收起' : '＋ 添加资料' }}</button>
    </header>

    <div class="page">
      <section v-if="showAdd" class="card">
        <h3 class="card-title">添加资料</h3>
        <input v-model.trim="form.title" class="input" placeholder="标题，例如：小米 SU7 参数" />
        <input v-model.trim="form.source" class="input" placeholder="来源（可选），例如：su7-manual.md" />
        <textarea v-model="form.content" class="textarea" rows="6" placeholder="把资料正文粘贴到这里…"></textarea>
        <div class="row">
          <span class="tip">入库会调用 embedding 服务；纯文本，建议一段一段粘</span>
          <button class="btn primary" type="button" :disabled="adding" @click="submitDoc">
            {{ adding ? '入库中…' : '入库' }}
          </button>
        </div>
      </section>

      <section class="card">
        <h3 class="card-title">知识库问答</h3>
        <div class="ask-row">
          <input
            v-model.trim="question"
            class="input"
            placeholder="问资料里有的内容，例如：SU7 的续航是多少？"
            @keyup.enter="ask"
          />
          <button class="btn primary" type="button" :disabled="asking || !question" @click="ask">
            {{ asking ? '检索中…' : '提问' }}
          </button>
        </div>

        <div v-if="answer" class="answer">
          <p class="answer-text">{{ answer.answer }}</p>
          <div v-if="answer.sources && answer.sources.length" class="sources">
            <span class="sources-label">引用来源（Top-{{ answer.sources.length }}）</span>
            <div v-for="s in answer.sources" :key="s.index" class="source">
              <span class="badge">[{{ s.index }}]</span>
              <div class="source-main">
                <span class="source-name">{{ s.title }} · {{ s.source }}</span>
                <span class="source-snippet">{{ s.snippet }}</span>
              </div>
              <span class="score">{{ s.score.toFixed(3) }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="row">
          <h3 class="card-title">已入库资料（{{ documents.length }}）</h3>
          <button v-if="documents.length" class="link danger" type="button" @click="clearAll">清空</button>
        </div>
        <div v-if="documents.length" class="docs">
          <div v-for="d in documents" :key="d.docId" class="doc">
            <div class="doc-main">
              <span class="doc-title">{{ d.title }}</span>
              <span class="doc-meta">{{ d.source }} · {{ d.chunks }} 块</span>
            </div>
            <button class="link danger" type="button" @click="removeOne(d)">删除</button>
          </div>
        </div>
        <p v-else class="empty">还没有资料，点右上角「添加资料」</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addDocument, askKnowledge, clearDocuments, listDocuments, removeDocument } from '../api/knowledge'

const documents = ref([])
const showAdd = ref(false)
const adding = ref(false)
const asking = ref(false)
const question = ref('')
const answer = ref(null)
const form = reactive({ title: '', source: '', content: '' })

const chunkTotal = computed(() => documents.value.reduce((sum, d) => sum + (d.chunks || 0), 0))

const loadDocuments = async () => {
  try {
    documents.value = (await listDocuments()) || []
  } catch {
    documents.value = []
  }
}

onMounted(loadDocuments)

const toggleAdd = () => {
  showAdd.value = !showAdd.value
}

const submitDoc = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请粘贴资料内容')
    return
  }
  adding.value = true
  try {
    const result = await addDocument({ title: form.title, source: form.source, content: form.content })
    ElMessage.success(`已入库，切成 ${result.chunks} 块`)
    form.title = ''
    form.source = ''
    form.content = ''
    showAdd.value = false
    await loadDocuments()
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    adding.value = false
  }
}

const ask = async () => {
  if (!question.value) return
  asking.value = true
  try {
    answer.value = await askKnowledge(question.value)
  } catch {
    answer.value = null
  } finally {
    asking.value = false
  }
}


const removeOne = async (doc) => {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.title}」？`, '提示', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
  } catch {
    return
  }
  try {
    await removeDocument(doc.docId)
    await loadDocuments()
    answer.value = null
    ElMessage.success('已删除')
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

const clearAll = () => {
  ElMessageBox.confirm('确定清空整个知识库？', '提示', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await clearDocuments()
      answer.value = null
      await loadDocuments()
      ElMessage.success('已清空')
    })
    .catch(() => {})
}
</script>

<style scoped>
.kb { min-height: 100%; background: var(--c-bg); }

.hero {
  display: flex; align-items: center; gap: 10px;
  padding: 16px; color: #fff;
  background: linear-gradient(135deg, #1F2329, #2C3138 60%, #20343A);
}
.brand { display: flex; flex-direction: column; }
.brand-logo { font-size: 18px; font-weight: 800; }
.brand-sub { font-size: 12px; opacity: .7; }
.ghost {
  margin-left: auto; border: 1px solid rgba(255, 255, 255, .4);
  background: none; color: #fff; border-radius: 999px;
  padding: 6px 14px; font-size: 13px; font-family: inherit; cursor: pointer;
}

.page { padding: 14px; display: flex; flex-direction: column; gap: 14px; }
.card { background: #fff; border-radius: var(--r-lg); box-shadow: var(--sh-1); padding: 15px; }
.card-title { margin: 0; font-size: 15px; font-weight: 700; }

.input, .textarea {
  width: 100%; box-sizing: border-box; margin-top: 10px;
  border: 1px solid var(--c-line); border-radius: 10px;
  padding: 10px 12px; font-size: 14px; font-family: inherit;
  color: var(--c-text); outline: none; background: #fff;
}
.textarea { resize: vertical; line-height: 1.6; }
.input:focus, .textarea:focus { border-color: var(--c-brand); }

.row { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.row .card-title { flex: 1; }
.tip { flex: 1; font-size: 12px; color: var(--c-text-3); line-height: 1.5; }

.btn { border: none; border-radius: 11px; padding: 10px 18px; font-size: 14px; font-family: inherit; cursor: pointer; }
.btn.primary { color: #fff; background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2)); box-shadow: 0 6px 16px rgba(224, 30, 38, .22); }
.btn.primary:disabled { opacity: .5; box-shadow: none; cursor: not-allowed; }

.link { border: none; background: none; font-size: 13px; font-family: inherit; cursor: pointer; }
.link.danger { color: var(--c-brand); }

.ask-row { display: flex; gap: 8px; margin-top: 10px; }
.ask-row .input { margin-top: 0; flex: 1; }
.ask-row .btn { flex-shrink: 0; }

.answer { margin-top: 14px; padding: 13px; background: #F8F9FB; border-radius: 12px; }
.answer-text { margin: 0; font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }

.sources { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.sources-label { font-size: 12px; color: var(--c-text-3); }
.source { display: flex; gap: 8px; align-items: flex-start; padding: 8px 10px; background: #fff; border: 1px solid var(--c-line); border-radius: 10px; }
.badge { flex-shrink: 0; font-size: 12px; color: var(--c-brand); font-weight: 700; }
.source-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.source-name { font-size: 12px; font-weight: 600; }
.source-snippet { font-size: 12px; color: var(--c-text-3); line-height: 1.5; }
.score { flex-shrink: 0; font-size: 11px; color: var(--c-text-3); }

.docs { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }
.doc { display: flex; align-items: center; gap: 10px; padding: 9px 11px; background: #F8F9FB; border-radius: 10px; }
.doc-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.doc-title { font-size: 14px; font-weight: 600; }
.doc-meta { font-size: 12px; color: var(--c-text-3); }

.empty { margin: 12px 0 0; font-size: 13px; color: var(--c-text-3); }
</style>