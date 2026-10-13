<template>
  <div class="account">
    <header class="hero">
      <div class="brand">
        <span class="brand-logo">我的</span>
        <span class="brand-sub">{{ isLoggedIn ? '已登录' : '登录 / 注册' }}</span>
      </div>
    </header>

    <div class="page">
      <!-- 已登录 -->
      <div v-if="isLoggedIn" class="card center">
        <div class="avatar">{{ avatarText }}</div>
        <h2 class="name">{{ user.displayName }}</h2>
        <p class="sub">@{{ user.username }}</p>
        <p class="hint">上传的视频和评论都归属这个账号，换设备登录后同样能管理。</p>
        <button class="btn primary block" type="button" :disabled="pending" @click="handleLogout">
          {{ pending ? '处理中…' : '退出登录' }}
        </button>
      </div>

      <!-- 未登录 -->
      <div v-else class="card">
        <div class="tabs">
          <button type="button" :class="{ active: tab === 'login' }" @click="switchTab('login')">登录</button>
          <button type="button" :class="{ active: tab === 'register' }" @click="switchTab('register')">注册</button>
        </div>

        <form class="form" @submit.prevent="submit">
          <label class="field">
            <span>用户名</span>
            <input
              v-model.trim="form.username"
              placeholder="3-20 位字母、数字或下划线"
              autocomplete="username"
            />
          </label>

          <label v-if="tab === 'register'" class="field">
            <span>昵称（可选）</span>
            <input v-model.trim="form.displayName" placeholder="不填默认用用户名" />
          </label>

          <label class="field">
            <span>密码</span>
            <input
              v-model="form.password"
              type="password"
              placeholder="至少 6 位"
              :autocomplete="tab === 'register' ? 'new-password' : 'current-password'"
            />
          </label>

          <button class="btn primary block" type="submit" :disabled="pending">
            {{ pending ? '处理中…' : tab === 'login' ? '登录' : '注册并登录' }}
          </button>
        </form>

        <p class="hint">未登录也能浏览视频、问 AI；上传视频和发表评论需要登录。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../composables/useAuth'

const route = useRoute()
const router = useRouter()
const { user, isLoggedIn, pending, login, register, logout } = useAuth()

const tab = ref('login')
const form = reactive({ username: '', password: '', displayName: '' })

const avatarText = computed(() => (user.value?.displayName || '车').slice(0, 1))

const switchTab = (next) => {
  tab.value = next
}

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  try {
    if (tab.value === 'login') {
      await login(form.username, form.password)
      ElMessage.success('登录成功')
    } else {
      await register(form.username, form.password, form.displayName)
      ElMessage.success('注册成功，已自动登录')
    }
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/home')
  } catch {
    // 错误提示已由请求拦截器统一处理
  }
}

const handleLogout = async () => {
  await logout()
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.account { min-height: 100%; background: var(--c-bg); }

.hero { padding: 18px 16px 16px; background: linear-gradient(135deg, #1F2329, #2C3138 60%, #3A2226); color: #fff; }
.brand { display: flex; align-items: baseline; gap: 8px; }
.brand-logo { font-size: 20px; font-weight: 800; }
.brand-sub { font-size: 12px; opacity: .7; }

.page { padding: 16px; }
.card { background: #fff; border-radius: var(--r-lg); box-shadow: var(--sh-1); padding: 18px; }
.card.center { display: flex; flex-direction: column; align-items: center; text-align: center; }

.avatar {
  width: 56px; height: 56px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
}
.name { margin: 12px 0 2px; font-size: 18px; }
.sub { margin: 0; font-size: 13px; color: var(--c-text-3); }

.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tabs button {
  flex: 1; padding: 9px 0; border: none; border-radius: 10px;
  background: #F4F5F7; color: var(--c-text-2);
  font-size: 14px; font-family: inherit; cursor: pointer;
}
.tabs button.active { background: var(--c-brand-soft); color: var(--c-brand); font-weight: 600; }

.form { display: flex; flex-direction: column; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field > span { font-size: 12px; color: var(--c-text-2); }
.field input {
  border: 1px solid var(--c-line); border-radius: 10px;
  padding: 10px 12px; font-size: 14px; font-family: inherit;
  outline: none; color: var(--c-text); background: #fff;
}
.field input:focus { border-color: var(--c-brand); }

.btn {
  margin-top: 6px; border: none; border-radius: 12px;
  padding: 11px 16px; font-size: 14px; font-family: inherit; cursor: pointer;
}
.btn.block { width: 100%; }
.btn.primary {
  color: #fff;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 6px 16px rgba(224, 30, 38, .25);
}
.btn.primary:disabled { opacity: .5; box-shadow: none; cursor: not-allowed; }

.hint { margin: 14px 0 0; font-size: 12px; line-height: 1.6; color: var(--c-text-3); }
</style>